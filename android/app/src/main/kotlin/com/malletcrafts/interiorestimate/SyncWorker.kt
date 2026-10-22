package com.malletcrafts.interiorestimate

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

    /**
     * TWO LEGS, EITHER OR BOTH (migration slices A+C pulled forward; Amit, 22 Oct 2026:
     * "remove the erp key pair dependency and let S5 be store for apk"). The ERP leg runs
     * exactly as before whenever a pair is stored -- slices B and D still feed off its
     * bootstrap, so it stays functional DURING the migration. With no pair but an S5
     * sign-in, S5 is the store: its project list becomes the masters' projects and the
     * survey preps go up as as-is uploads. The S5 reads and writes that never cared which
     * leg ran (quote stage, wish list) run in both.
     */
    override suspend fun doWork(): Result {
        val client = FrappeClient.load(applicationContext)
        // Before anything asks S5: a session near its 12 h end is renewed here, once,
        // rather than met as a 401 by every call below.
        if (S5Session.signedIn(applicationContext)) S5Session.ensureFresh(applicationContext)
        // Read AFTER the renewal: a refused one signs the phone out of S5 (S5Session.relogin).
        val s5 = S5Session.signedIn(applicationContext)
        if (client == null && !s5) return Result.success()
        val store = CaptureStore(applicationContext)
        val cat = Catalogue(applicationContext)

        val failures = if (client != null) erpLeg(client, store, cat) else { s5Leg(store); 0 }

        // An as-is upload that ENDED for want of a sign-in goes again now there is one -- in
        // either leg, and only those: a job still in WorkManager is never replaced.
        AsIsUpload.requeueSignInRefused(applicationContext, store.all().map { it.deviceId })

        // S5's QUOTE stage for every ERP project this phone knows (v52 item 16): ONE read of
        // the caller's list, after the captures are up and the masters fresh, so a project
        // created in this run is asked about too. Read-only, and NEVER counted in `failures`:
        // QuoteStageSync swallows every refusal, 5xx and IO error with one Log.w, so S5 being
        // unreachable can never turn this sync into a retry that backs off the captures.
        // In BOTH legs (22 Oct 2026): it reads S5 with S5Auth.fresh, whichever is held.
        val projects = runCatching { cat.projects(store.masters()) }.getOrDefault(emptyList())
        QuoteStageSync.refresh(applicationContext, projects.map { it.serverId })

        // THE CLIENT'S WISH LIST (v52 item 16, slice 5b), after the quote stage and on the same
        // terms: NEVER counted in `failures`. First a list made on a phone-only project moves
        // under the ERP id that project has now -- matched on client and title against the
        // masters as refreshed above, after ensureSite and pruneLocalsCoveredBy, so a list made
        // offline is not orphaned (decision 3). Then ONE PUT per dirty list that has an ERP id,
        // with its base; a 409 is S5's copy winning (decision 2), a 404 waits on the phone, and
        // every refusal, 5xx or IO error is one Log.w inside WishSync. Both legs, as above.
        WishSync.rekey(applicationContext, projects)
        WishSync.push(applicationContext)

        // Update check rides the sync too: when the server holds a newer
        // camera build, remember it so the screen can offer the install.
        // Not in the Play build — Play is its updater, and the pref would
        // only ever feed a drawer row that pendingUpdate refuses anyway.
        // The bench serves it, so it needs the ERP leg.
        if (client != null && BuildConfig.SELF_UPDATE) runCatching {
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

    /**
     * THE S5-ONLY LEG: no ERP pair on this phone, an S5 sign-in instead. Nothing here is a
     * failure of the sync -- every step keeps what it had when S5 cannot be reached, and the
     * uploads are their own WorkManager jobs with their own retries.
     */
    private fun s5Leg(store: CaptureStore) {
        val ctx = applicationContext
        // (a) The projects this person may see on S5, folded into the masters (S5Masters
        // keeps the room and stage lists and every project already known). Null = stale kept.
        S5Masters.fetch(ctx)?.let { store.saveMasters(it) }

        // (b) A capture from a NEW site has no project id, and with no bench there is nobody
        // to mint one: it takes the project the masters hold for that client and title, else
        // the deterministic phone key (S5Auth.keyFor), so every capture of that site lands on
        // ONE S5 project, settled by the upload itself.
        for (c in store.pending()) {
            if (c.project.isBlank())
                store.setProject(c.deviceId, S5Auth.keyFor(ctx, c.customerName, c.projectTitle))
        }

        // (c) Every saved survey prep never queued (made before this sign-in) is queued. Nothing
        // else: "waiting" and "error" are jobs still in WorkManager -- enqueue REPLACEs, and
        // replacing one would restart a half-sent room from the first surface -- and a room S5
        // refused ("refused") goes again when its survey prep is saved again, not every sync.
        // Sign-in refusals are re-queued by doWork (requeueSignInRefused).
        val preps = SurveyPrepStore(ctx)
        for (c in store.all()) {
            if (preps.loadText(c.deviceId) == null) continue
            if (AsIsUpload.stateOf(ctx, c.deviceId) == null) AsIsUpload.enqueue(ctx, c.deviceId)
        }

        // (c2) S5 TAKES SURVEY PREPS ONLY. A flat photo, or a 360 whose survey prep is not saved,
        // has no way to the web: it stays counted as unsent -- it IS only on this phone -- but
        // says why instead of looking like it is waiting. The ERP leg still sends it the day a
        // pair is stored (ERROR is pending to it).
        val noPrep = "on this phone only - save its survey prep to send it to the web"
        val notPhoto = "on this phone only - the web takes a 360's survey prep, not a photo"
        for (c in store.pending()) {
            if (preps.loadText(c.deviceId) != null) {
                // Its survey prep has been saved since: the upload job carries it now.
                if (c.state == "ERROR" && c.error == noPrep) store.setState(c.deviceId, "LOCAL")
                continue
            }
            val why = if (c.kind == "360") noPrep else notPhoto
            if (c.state != "ERROR" || c.error != why) store.setState(c.deviceId, "ERROR", error = why)
        }

        // (d) SYNCED HERE MEANS ON THE WEB (S5), the ERP leg not being configured: a capture
        // whose as-is upload reads good ("On web ✓") leaves the waiting count. serverName is
        // NOT set -- it names a Site Photo 360 on the bench, and every ERP call keyed on it
        // (annotations, face SKUs, downloads) would ask the bench for a record that does not
        // exist the day a pair is stored again. The upload already names the capture by
        // serverName ?: deviceId on its own. SYNCED with no serverName is therefore exactly
        // "on S5, not on the bench", and the ERP leg picks those up (erpLeg).
        for (c in store.all()) {
            if (c.state == "SYNCED") continue
            if (AsIsUpload.status(ctx, c.deviceId)?.second == true)
                store.setState(c.deviceId, "SYNCED")
        }
    }

    /** THE ERP LEG, as it ran before 22 Oct 2026, for a phone holding a pair. Returns its failures. */
    private fun erpLeg(client: FrappeClient, store: CaptureStore, cat: Catalogue): Int {
        // Masters first: a phone that syncs captures but shows week-old rooms
        // files tomorrow's photo against the wrong wall.
        runCatching { store.saveMasters(client.bootstrap()) }

        var failures = 0
        // Plus every capture marked SYNCED by the S5-only leg (no serverName: on the web, never on
        // the bench), so a pair stored after an S5-only spell still sends them to ERP.
        val onlyOnS5 = store.all().filter { it.state == "SYNCED" && it.serverName.isNullOrBlank() }
        for (c in store.pending() + onlyOnS5) {
            try {
                store.setState(c.deviceId, "SYNCING")

                // A capture from a NEW site carries the typed names and no
                // project id yet. Resolve first — the server matches
                // insensitively against existing masters and only creates
                // when nothing matches, so two visits to one new site end up
                // on one project however the names were typed.
                // A phone-minted S5 key ("S5-...", S5Auth.localKey) is no ERP Project id: such a
                // capture resolves through ensureSite like one with no project at all.
                var projectId = c.project.takeUnless { S5Auth.isLocalKey(it) }.orEmpty()
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
                    // THE LOCAL ROW STAYS, and that is the fix. It used to be
                    // dropped here, on the reasoning that ERP had it now --
                    // but the masters in hand were fetched BEFORE this site
                    // existed, so nothing had arrived to replace it and the
                    // client vanished from the tree until a later bootstrap
                    // happened to succeed. Amit found it in a client's flat.
                    // Locals are pruned after the refresh below, against
                    // masters that can actually cover them.
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
                    captureKind = c.kind,
                    fov = c.fov,
                    roomLengthIn = c.roomLengthIn,
                    roomWidthIn = c.roomWidthIn,
                    roomHeightIn = c.roomHeightIn)
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
                // A 360 is KEPT on the phone after the upload (Amit, 2026-10-10:
                // "Keep a local copy"): survey prep reopens on a kept capture,
                // and the site readings are taken days after the shutter. It
                // goes when the capture is deleted, or by "Remove local copy"
                // on its screen. A flat photo has no survey prep: the server
                // owns it now, as before.
                if (c.kind != "360") pano.delete()
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

        // REFRESH AFTER THE LOOP, not only before it. Anything the loop just
        // created on the bench -- a new client, a new site -- exists only
        // there until the masters are read again, and the tree draws from the
        // masters. Fetching once at the start meant a site created during the
        // run was invisible for the rest of it.
        //
        // Only now are local rows pruned, and only those the refreshed
        // masters actually cover. A failed refresh therefore costs a
        // duplicate folder for one sync instead of a client nobody can find
        // while standing in their flat.
        runCatching {
            val fresh = client.bootstrap()
            store.saveMasters(fresh)
            cat.pruneLocalsCoveredBy(store.masters())
        }
        return failures
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
