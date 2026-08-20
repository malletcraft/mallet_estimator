package com.malletcrafts.sitephotos

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * The ERPNext leg, whenever there is signal. ImageMeter's own sync is its own
 * business; ours is: every queued capture becomes a Site Photo 360 with the
 * pano bound, and the masters cache stays fresh.
 *
 * Order inside one capture matters and is restartable at every step because
 * the server is idempotent on the device id: create again returns the SAME
 * record, so a connection that dies after any step just resumes there next
 * run. The one thing this must never do is mark SYNCED before bind succeeds.
 */
class SyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val client = FrappeClient.load(applicationContext) ?: return Result.success()
        val store = CaptureStore(applicationContext)

        // Masters first: a phone that syncs captures but shows week-old rooms
        // files tomorrow's photo against the wrong wall.
        runCatching { store.saveMasters(client.bootstrap()) }

        var failures = 0
        for (c in store.pending()) {
            try {
                store.setState(c.deviceId, "SYNCING")

                // A capture from a NEW site carries the typed names and no
                // project id yet. Resolve first — the server matches
                // insensitively against existing masters and only creates
                // when nothing matches, so two visits to one new site end up
                // on one project however the names were typed.
                var projectId = c.project
                if (projectId.isBlank()) {
                    val site = client.ensureSite(c.customerName, c.projectTitle)
                    projectId = site.getString("project")
                    store.setProject(c.deviceId, projectId)
                }

                val made = client.createCapture(
                    project = projectId, room = c.room,
                    captureDate = c.captureDate, stage = c.stage,
                    deviceCaptureId = c.deviceId,
                    appVersion = runCatching {
                        applicationContext.packageManager.getPackageInfo(
                            applicationContext.packageName, 0).versionName ?: ""
                    }.getOrDefault(""))
                val name = made.getString("name")

                val pano = File(c.panoPath)
                if (!pano.exists()) {
                    // The queue row outlived its file (cleared storage). The
                    // capture record still exists server-side; a person can
                    // re-shoot. Never pretend this synced.
                    store.setState(c.deviceId, "ERROR",
                        serverName = name, error = "pano file missing on device")
                    continue
                }
                val fileUrl = client.uploadPano(name, pano)
                client.bindPano(name, fileUrl)
                store.setState(c.deviceId, "SYNCED", serverName = name)
                pano.delete()      // uploaded and bound; the server owns it now
            } catch (e: Exception) {
                failures += 1
                store.setState(c.deviceId, "ERROR",
                    error = e.message?.take(200) ?: e.javaClass.simpleName)
            }
        }
        // Annotations ride the same sync: any face edited since its last
        // push goes up now — measurement edits after the capture synced
        // included. Empty payloads delete server-side, mirroring the device.
        val annStore = AnnotationStore(applicationContext)
        for (c in store.all()) {
            val serverName = c.serverName ?: continue
            for (face in annStore.dirtyFaces(c.deviceId)) {
                try {
                    var ann = annStore.load(c.deviceId, face)
                    // Voice clips go up FIRST and their urls go into the JSON,
                    // so an annotation never references a recording the server
                    // hasn't got. A clip recorded with no signal simply waits
                    // here until there is some.
                    if (ann.pins.any { it.audioPending }) {
                        val pins = ann.pins.map { p ->
                            if (!p.audioPending) p else {
                                val f = VoiceNotes.file(applicationContext, p.audio)
                                if (f.exists())
                                    p.copy(audioUrl = client.uploadAudioNote(serverName, f))
                                else p.copy(audio = "")   // clip is gone; forget it
                            }
                        }
                        ann = ann.copy(pins = pins)
                        annStore.save(c.deviceId, face, ann)
                    }
                    client.saveAnnotations(serverName, face,
                        AnnotationStore.encode(ann))
                    annStore.markSynced(c.deviceId, face)
                } catch (e: Exception) {
                    failures += 1   // retry later; dirty marker stays
                }
            }

            // ...and PULL, so a capture annotated on somebody else's phone
            // opens here with their measurements on it. Amit's "sync of
            // images across devices": the photos already travel, this is
            // what makes the marks travel with them. Faces this phone has
            // unsent edits on are left alone — acceptFromServer refuses
            // them — so a pull can never eat work in progress.
            try {
                val remote = client.getAnnotations(serverName)
                val faces = remote.keys()
                while (faces.hasNext()) {
                    val face = faces.next()
                    val obj = remote.optJSONObject(face) ?: continue
                    annStore.acceptFromServer(c.deviceId, face,
                        AnnotationStore.decode(obj))
                }
            } catch (e: Exception) {
                failures += 1   // a failed pull just means stale, not lost
            }
        }

        // Update check rides the sync too: when the server holds a newer
        // camera build, remember it so the screen can offer the install.
        runCatching {
            val info = client.appUpdateInfo()
            val prefs = applicationContext.getSharedPreferences(
                "capture", Context.MODE_PRIVATE)
            val mine = applicationContext.packageManager.getPackageInfo(
                applicationContext.packageName, 0).longVersionCode
            if (info.optString("status") == "ready" &&
                info.optInt("version_code") > mine) {
                prefs.edit().putString("update_available", info.toString()).apply()
            } else {
                prefs.edit().remove("update_available").apply()
            }
        }

        // Retry lets WorkManager back off and try again with signal; the rows
        // keep their ERROR text so the screen can say why in the meantime.
        return if (failures > 0) Result.retry() else Result.success()
    }

    companion object {
        fun syncNow(context: Context) {
            val req = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("mcft-sync", ExistingWorkPolicy.REPLACE, req)
        }

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "mcft-sync-hourly", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
