package com.malletcrafts.interiorestimate

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.malletcrafts.interiorestimate.pano.Album
import com.malletcrafts.interiorestimate.pano.AsIsPath
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * S5 LEARNS THE FILING (port 61c slice FW2): a capture the web holds is re-filed by WORK through S5-R,
 * `PUT …/<project>/<room>/c/<capture>/work {work}` (AsIsPath.work and workBody; mcft-interior-estimate
 * 9a51733, "A CAPTURE IS RE-FILED BY ITS OWN SMALL WRITE"), without re-uploading it -- S5's UPDATE moves
 * the record's `work` alone, so the capture order, the room's current capture and the project list stay.
 *
 * Two callers, ONE job per capture:
 *  - the photo's WORK line, after Client approved: CaptureStore.setWork first (the phone's own copy moves
 *    at once, with or without signal), then this job for a capture whose upload reads On web ([onWeb]);
 *    a capture not on S5 yet carries the work in its next record (AsIsPath.body);
 *  - THE SWEEP ([sweep]), in SyncWorker after BOTH legs: every capture On web whose filing
 *    (Album.workOfCapture) is not the Quote journey and that this phone has not told S5 of
 *    (Album.needsRefile over AsIsUpload.told, the done-mark the record PUT writes since FW2). S5 filed every
 *    record from before AS1 at 0, the quote journey, which the client sees (asis.js workOfRecord with no
 *    `work`), where the phone files an old 'Plaster & waterproofing' photo under Civil & masonry. Once per
 *    capture: the mark converges to the filing, and a pick that moved on the phone while a record was going
 *    up is caught by the next sync.
 *
 * The job is unique, `mcft-work-<id>`, and a later pick REPLACEs it: it reads the row's filing when it RUNS,
 * so the newest pick goes whatever was queued. It asks each name the capture has had, as S5's delete rule
 * names it: the device id, then ERP's name (MEST-PH-…, the row's serverName) on a 404 -- one capture, not a
 * fallback: until the capture's first record PUT from AS1 lands, S5 holds it under the name 0.3.212's record
 * gave it, and S5's C3 renames it onto the device id at that PUT. A 404 on every name is a capture not on
 * S5 (deleted there since its upload, or never landed): nothing to re-file, its next record carries the work,
 * and the mark is written so the sweep does not ask again every hour. 503 (a record landed meanwhile: S5's
 * own "send the re-file again"), 408, 429, every 5xx and no signal are retried by WorkManager; a 401 on the
 * S5 session renews it once. Any other refusal -- 400, 403, S5's 409 for a legacy row whose stored record is
 * not a JSON object (a direct D1 fix, S5's CLAUDE.md), a 401 on the ERP key -- ends the job: nothing a retry
 * changes. THE MARK IS NOT WRITTEN THEN (FW2 review): S5 did not take the work, so the phone's copy and S5's
 * disagree, and a mark saying they agree would have stopped the sweep for good -- for a capture whose local copy
 * was removed there is no record PUT to carry the work ever again (that is the capture S5-R exists for), and a
 * 403 on a non-staff login or a 401 on a dead ERP key is cured by a sign-in, after which the sweep's next ask
 * (one small PUT per sync, Album.needsRefile) lands. The refusal is one Log.w AND AsIsUpload.markRefileRefused:
 * S5's words beside the mark, drawn on survey prep's web line (`Not re-filed on the web: …`) until a re-file S5
 * takes clears them.
 *
 * ONE PUT PER CAPTURE AS1 UPLOADED (FW2 review, for the ship's d1-peek): the mark is written by the record PUT
 * only since FW2, so every capture 0.3.283 (AS1) uploaded has none although its record already carried the
 * work. The sweep sends each of those one S5-R PUT once; S5 answers 200 and sets the SAME `work` again, with
 * `work_by` = this login and `work_at` = the sweep's time, while `updated_at`, `updated_by` and the room's
 * current capture stay. So the stage check reads `work_by` / `work_at` on AS1 rows as this one idempotent PUT,
 * not as a changed filing. Back-filling the mark at `done` for an upload that went up with AS1 or later would
 * save it, and is more machinery than the one PUT it saves.
 *
 * The WORK line draws the phone's own filing; what S5 answered is the log line and, for a refusal, the web line.
 */
object WorkRefile {

    private const val TAG = "WorkRefile"
    private const val KEY_ID = "capture"

    fun workName(captureId: String) = "mcft-work-$captureId"

    /** Queue (or replace) the re-file of one capture: the job reads the row's filing when it runs. */
    fun enqueue(context: Context, captureId: String) {
        val req = OneTimeWorkRequestBuilder<WorkRefileWorker>()
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(Data.Builder().putString(KEY_ID, captureId).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(captureId), ExistingWorkPolicy.REPLACE, req)
    }

    internal fun captureId(input: Data): String? = input.getString(KEY_ID)

    /** Whether S5 holds this capture: its as-is upload reads On web. */
    fun onWeb(context: Context, captureId: String): Boolean =
        AsIsUpload.status(context, captureId)?.ok == true

    /**
     * THE SWEEP: every capture Album.needsRefile says so of gets the job -- On web, filed off the Quote
     * journey, and not yet told to S5 as it is filed now. Reads only; the jobs do the sending.
     */
    fun sweep(context: Context, store: CaptureStore) {
        for (c in store.all()) {
            val told = AsIsUpload.told(context, c.deviceId)
            if (Album.needsRefile(onWeb(context, c.deviceId), Album.workOfCapture(c.work, c.stage), told?.work, told != null))
                enqueue(context, c.deviceId)
        }
    }
}

class WorkRefileWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val tag = "WorkRefile"

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val id = WorkRefile.captureId(inputData) ?: return Result.failure()
        // A session near its 12 h end is renewed before the PUT, as the upload job does.
        if (S5Session.signedIn(ctx)) S5Session.ensureFresh(ctx)
        var auth = S5Auth.current(ctx)
            // Signed in, the renewal just could not reach the web: a wait. Nothing to send with: the job ends,
            // and the sweep queues it again once the mark still differs after a sign-in.
            ?: return if (S5Session.signedIn(ctx)) Result.retry() else Result.failure()
        val c = CaptureStore(ctx).all().firstOrNull { it.deviceId == id } ?: return Result.failure()
        if (c.project.isBlank()) return Result.failure()
        val work = Album.workOfCapture(c.work, c.stage)
        val body = JSONObject(AsIsPath.workBody(work) as Map<*, *>).toString().toByteArray(Charsets.UTF_8)
        fun url(capture: String): String =
            BuildConfig.WEB_URL.trimEnd('/') + AsIsPath.path(AsIsPath.work(c.project, c.room, capture)) { AsIsUpload.seg(it) }
        // Each name the capture has had: the device id, then ERP's name when it has one and it differs.
        val names = listOfNotNull(c.deviceId, c.serverName?.takeIf { it.isNotBlank() && it != c.deviceId })
        return try {
            var i = 0
            var renewed = false
            while (i < names.size) {
                val a = AsIsUpload.exchange("PUT", url(names[i]), auth, body, "application/json; charset=utf-8") // text:value the body's content type, a wire value
                when {
                    a.ok -> {
                        AsIsUpload.markTold(ctx, id, work)
                        Log.i(tag, "$id: re-filed on S5 as ${names[i]} to work $work") // text:value log line
                        return Result.success()
                    }
                    a.code == 404 -> i += 1
                    a.code == 401 && S5Session.signedIn(ctx) && !renewed -> {
                        // S5 no longer holds the session: signed in again once, then the same name asked again
                        renewed = true
                        if (!S5Session.relogin(ctx)) return Result.retry()
                        auth = S5Auth.current(ctx) ?: return Result.retry()
                    }
                    a.code == 503 || a.code == 408 || a.code == 429 || a.code >= 500 -> return Result.retry()
                    else -> {
                        // REFUSED: the mark stays, so the sweep asks again next sync; the words are shown
                        val why = AsIsUpload.errorOf(a).message ?: a.code.toString()
                        AsIsUpload.markRefileRefused(ctx, id, why)
                        Log.w(tag, "$id: re-file refused by S5 as ${names[i]}: $why") // text:value log line
                        return Result.failure()
                    }
                }
            }
            // 404 on every name: not on S5. Its next record carries the work; the mark stops the hourly ask.
            AsIsUpload.markTold(ctx, id, work)
            Log.i(tag, "$id: not on S5 under ${names.joinToString(", ")}; its next record carries work $work") // text:value log line
            Result.success()
        } catch (e: Exception) {
            // No signal, a reset: WorkManager runs it again.
            Log.w(tag, "$id: re-file not sent: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
            Result.retry()
        }
    }
}
