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
        val cat = Catalogue(applicationContext)
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
                    // The site and job type are read back from the catalogue
                    // rather than carried on the capture row: the captures
                    // table has no migration path, and the catalogue is where
                    // the phone already remembers what was typed.
                    val siteName = cat.localSiteFor(c.customerName, c.projectTitle)
                    val resolved = client.ensureSite(
                        c.customerName, c.projectTitle,
                        siteName = siteName,
                        jobType = cat.localJobTypeFor(c.customerName, c.projectTitle),
                        siteType = cat.localSiteType(siteName),
                        // Typed on site with no signal. If it does not ride
                        // out with the capture it is only ever a note in this
                        // phone's preferences, which is where it dies.
                        siteAddress = cat.localAddress(siteName))
                    projectId = resolved.getString("project")
                    store.setProject(c.deviceId, projectId)
                    // ERP has it now, so the local copy is redundant. Dropping
                    // it is what stops the same folder appearing twice.
                    cat.forgetLocal(c.customerName,
                        cat.localSiteFor(c.customerName, c.projectTitle),
                        c.projectTitle)
                }

                // The queue stores the WORK STAGE; the phase is derived from
                // the master so a bench that has it files against the stage and
                // one that does not still gets a phase it understands.
                val masters = store.masters()
                val workStage = if (cat.isWorkStage(masters, c.stage)) c.stage else ""
                val phase = cat.phaseOfStage(masters, c.stage).ifBlank { c.stage }
                val made = client.createCapture(
                    project = projectId, room = c.room,
                    captureDate = c.captureDate, stage = phase,
                    workStage = workStage,
                    deviceCaptureId = c.deviceId,
                    appVersion = runCatching {
                        applicationContext.packageManager.getPackageInfo(
                            applicationContext.packageName, 0).versionName ?: ""
                    }.getOrDefault(""),
                    sku = c.sku,
                    captureKind = c.kind)
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
        // The gallery, read ONCE for the whole queue. Every face this app
        // wrote carries a QR in its caption bar naming its capture, so an
        // annotated copy ImageMeter published can be traced back to the
        // photograph it was drawn on — which the filename cannot do, because
        // ImageMeter renames its exports. Done here as well as on the capture
        // screen so a markup goes home whether or not anybody opens it.
        val marks = runCatching {
            StampScan.allMarks(applicationContext)
        }.getOrDefault(emptyMap())
        for (c in store.all()) {
            val serverName = c.serverName ?: continue
            marks[c.deviceId]?.let { found ->
                runCatching {
                    AnnotationPush.push(applicationContext, serverName,
                        c.deviceId, found)
                }.onFailure { failures += 1 }
            }
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

        // Faces tagged with the work expected on them, while there was no
        // signal. Sent after the captures for the same reason as everything
        // else here: a face cannot be tagged on the bench until its capture
        // exists there.
        for ((devId, face, sku) in FaceSkus.pending(applicationContext)) {
            val serverName = store.all().firstOrNull { it.deviceId == devId }?.serverName
            if (serverName.isNullOrBlank()) continue
            try {
                client.setFaceSku(serverName, face, sku)
                FaceSkus.markSynced(applicationContext, devId, face)
            } catch (e: Exception) {
                failures += 1   // the dirty marker stays; it goes next time
            }
        }

        // Work the site recorded. Sent AFTER the captures, because an SKU
        // needs the project to exist and a brand-new site only becomes real
        // when its first capture syncs.
        for (k in cat.localSkus()) {
            try {
                val projectId = k.projectId.ifBlank {
                    client.ensureSite(k.client, k.projectTitle,
                        cat.localSiteFor(k.client, k.projectTitle),
                        cat.localJobTypeFor(k.client, k.projectTitle))
                        .optString("project")
                }
                if (projectId.isBlank()) { failures += 1; continue }
                client.createSku(
                    project = projectId, room = k.room,
                    articleCode = k.articleCode,
                    // The whole reason this loop is safe to run twice.
                    deviceSkuId = k.deviceId,
                    qty = k.qty, widthMm = k.widthMm, heightMm = k.heightMm,
                    depthMm = k.depthMm, note = k.note)
                cat.forgetLocalSku(k.deviceId)
            } catch (e: Exception) {
                // Kept, not dropped: the queue is the record of what the site
                // said, and losing it silently is the one failure this whole
                // mechanism exists to prevent.
                failures += 1
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
            // The Wi-Fi-only setting IS this constraint. A 20 MB pano going
            // out over a site's mobile data is somebody's bill, so the toggle
            // has to reach WorkManager rather than just a boolean — and the
            // policy is UPDATE, not KEEP, or flipping it would change nothing
            // until the app was reinstalled.
            val wifiOnly = context
                .getSharedPreferences("capture", Context.MODE_PRIVATE)
                .getBoolean("wifi_only", false)
            val req = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(
                        if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                    .build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "mcft-sync-hourly", ExistingPeriodicWorkPolicy.UPDATE, req)
        }
    }
}
