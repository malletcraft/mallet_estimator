package com.malletcrafts.interiorestimate

import android.content.Context
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.malletcrafts.interiorestimate.pano.AsIsPath
import com.malletcrafts.interiorestimate.pano.SurveyPrep
import com.malletcrafts.interiorestimate.pano.TourDemo
import com.malletcrafts.interiorestimate.pano.UploadNote
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * AS-IS CAPTURE, END TO END (contract of 2026-10-11, slice 1): a saved survey
 * prep goes to the web half of MCFT Interior Estimate, where staff, the
 * project's vendors and its client open project -> room -> the room as it is.
 *
 * EVERY 360 IS ITS OWN S5 CAPTURE (port 61c slice AS1, 3 Nov 2026; S5's "ONE
 * SERVER ROOM PER CAPTURE", 14 Oct 2026): after "Save survey prep", one unique
 * job per capture sends, under the capture's DEVICE ID on S5's explicit routes
 * (AsIsPath in :pano, where the addresses and the record are pinned):
 *  1. the room record, PUT …/<project>/<room>/c/<deviceId> -- with the phone's
 *     own filing (`work`), and `captured_at` byte for byte as 0.3.212 sent it,
 *     so S5's C3 renames the capture it may hold under the ERP name
 *     (MEST-PH-…) onto this id, readings and photos with it. S5 answering
 *     `created: true` (new on the web, or deleted there and back) forgets AT
 *     ONCE what this phone last saw synced of its readings, so the push sends
 *     every reading as a first write -- before anything that can fail, since a
 *     failed job runs again from the record and the second PUT is never
 *     `created` (the AS1 review, 3 Nov 2026);
 *  2. each of the six surfaces squared to size from the kept 360 -- NO margin,
 *     the picture is the surface rectangle, long side 2000 px
 *     (SurveyPrep.asisSquare) -- PUT …/c/<deviceId>/img/<surface>: S5 keeps
 *     no photo that no record owns, so a 404 here is the record not being on
 *     the web, and the job runs again from the record (RecordMissing). A photo
 *     S5 REFUSES ends the job, so the record goes once more first, naming only
 *     the surfaces that landed: the web never keeps a record that claims a
 *     photo R2 does not hold;
 *  3. the site readings, PATCH …/c/<deviceId>/readings (ReadingsSync): a 404
 *     here is the photo's 404 -- the capture gone from the web since the record
 *     went -- and runs the job again the same way, never "On web ✓".
 * The 360 gone: the record alone, every image false. Saving again REPLACES
 * the queued job, so the web always ends on the latest.
 *
 * Auth is S5Auth.current: the phone's S5 session (S5Session, ERP email and
 * password exchanged once for S5's own cookie) when it has one, else the
 * legacy ERP key (FrappeClient) -- the web asks ERP who either is. The S5
 * session wins when both exist (Amit, 22 Oct 2026: "remove the erp key pair
 * dependency and let S5 be store for apk"). Never logged or stored here.
 *
 * The state lives at filesDir/asisupload/<captureId>.json so survey prep can
 * say "On web ✓ hh:mm", "Waiting to upload" or what went wrong.
 */
object AsIsUpload {

    private const val KEY_ID = "capture"
    private const val QUALITY = 85

    fun workName(captureId: String) = "mcft-asis-$captureId"

    /** Queue (or re-queue) the upload of one capture's survey prep. */
    fun enqueue(context: Context, captureId: String) {
        // THE DEMO NEVER REACHES S5 (port 61c §31, TourDemo's one rule). SyncWorker's prep
        // sweep comes through this enqueue too, so this one line covers both.
        if (TourDemo.isDemoCapture(captureId)) return
        setState(context, captureId, "waiting", null)
        val req = OneTimeWorkRequestBuilder<AsIsUploadWorker>()
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(Data.Builder().putString(KEY_ID, captureId).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(captureId), ExistingWorkPolicy.REPLACE, req)
    }

    // ---- the state, per capture

    private fun file(context: Context, id: String) =
        File(File(context.filesDir, "asisupload").apply { mkdirs() }, "$id.json")

    /**
     * What S5 was last TOLD of this capture's filing (port 61c slice FW2): the `work` its record PUT carried,
     * or the work a re-file sent (WorkRefile). The sweep's done-mark, kept in the same file across every state
     * change (`told`, JSON null for not filed to a work), so a record that went up again or a job that waited
     * never loses it; absent for an upload from before FW2, whose record carried no `work` and which S5 filed
     * at 0. Null [work] is a filing (unfiled); a missing mark is [told] answering null. A re-file S5 REFUSED
     * moves no mark (FW2 review): the refusal is kept beside it as `refile`, S5's own words ([refileRefused]),
     * shown on survey prep's web line and cleared by the next re-file S5 takes.
     */
    class Told(val work: Int?)

    private fun toldOf(o: JSONObject): Told? =
        if (!o.has("told")) null else Told(if (o.isNull("told")) null else o.optInt("told"))

    /** The done-mark of [id], or null when S5 was never told its filing (or the capture was never queued). */
    fun told(context: Context, id: String): Told? = runCatching<Told?> {
        val f = file(context, id)
        if (!f.exists()) null else toldOf(JSONObject(f.readText()))
    }.getOrNull()

    /**
     * S5 holds [work] for this capture now (a re-file's 200, or a refusal nothing a retry changes): the mark
     * moves, the state stays. Nothing is written for a capture that was never queued or has been forgotten.
     */
    fun markTold(context: Context, id: String, work: Int?) {
        runCatching {
            val f = file(context, id)
            if (!f.exists()) return
            val o = JSONObject(f.readText()).put("told", work ?: JSONObject.NULL)
            o.remove("refile")
            f.writeText(o.toString())
        }
    }

    /**
     * S5 REFUSED a re-file of this capture with [why] (WorkRefile, nothing a retry changes): the mark stays as
     * it was, so the sweep asks again at the next sync, and the refusal is shown ([refileRefused]) instead of
     * living only in logcat. Nothing is written for a capture that was never queued or has been forgotten.
     */
    fun markRefileRefused(context: Context, id: String, why: String) {
        runCatching {
            val f = file(context, id)
            if (!f.exists()) return
            f.writeText(JSONObject(f.readText()).put("refile", why).toString())
        }
    }

    /** S5's words for the re-file it last refused of [id], or null: none refused, or one taken since. */
    fun refileRefused(context: Context, id: String): String? = runCatching<String?> {
        val f = file(context, id)
        if (!f.exists()) null else JSONObject(f.readText()).optString("refile", "").ifBlank { null }
    }.getOrNull()

    /** [signIn]: a "refused" that ended for want of a sign-in, so the sync re-queues it once one
     *  is held (requeueSignInRefused) -- unlike a refusal of the room itself, which waits for the
     *  person to save the survey prep again. [told]: the filing this state's record carried; null
     *  keeps the mark the file has. A refused re-file (`refile`) is dropped with every state change:
     *  the record going up again carries the work and writes the mark afresh. */
    internal fun setState(context: Context, id: String, state: String, error: String?, image: Boolean? = null,
                          signIn: Boolean = false, told: Told? = null) {
        runCatching {
            val f = file(context, id)
            val kept = told ?: runCatching { toldOf(JSONObject(f.readText())) }.getOrNull()
            val o = JSONObject().put("state", state).put("at", System.currentTimeMillis())
            if (error != null) o.put("error", error)
            if (image != null) o.put("image", image)
            if (signIn) o.put("signin", true)
            if (kept != null) o.put("told", kept.work ?: JSONObject.NULL)
            f.writeText(o.toString())
        }
    }

    /**
     * The capture's upload state for the screens; null when this capture was never queued. The LINE
     * is rendered at DRAW time with the live tx (UploadNote.statusLine — port 61c slice M2);
     * [UploadNote.Status.ok] is the one flag the colours and the sync read, never a string.
     */
    fun status(context: Context, id: String): UploadNote.Status? = runCatching<UploadNote.Status?> {
        val f = file(context, id)
        if (!f.exists()) return@runCatching null
        val o = JSONObject(f.readText())
        val state = o.optString("state")
        val hm = Instant.ofEpochMilli(o.optLong("at")).atZone(ZoneId.systemDefault())
            .toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.ROOT))
        UploadNote.Status(state, hm, o.optString("error", "").ifBlank { null },
            image = o.optBoolean("image", true), ok = state == "done")
    }.getOrNull()

    /**
     * The raw state, or null when never queued. "waiting" and "error" are jobs STILL IN
     * WorkManager (queued, running or backing off after a transient failure: no signal, a
     * 5xx, S5's 503 for crossed photos, memory); "done" and "refused" are jobs that ENDED.
     * So the S5-only sync re-queues only null -- re-queueing "error" REPLACEd live jobs mid
     * upload (review, 22 Oct 2026) -- plus the sign-in refusals (requeueSignInRefused).
     */
    internal fun stateOf(context: Context, id: String): String? = runCatching<String?> {
        val f = file(context, id)
        if (!f.exists()) null else JSONObject(f.readText()).optString("state").ifBlank { null }
    }.getOrNull()

    /** Re-queue every upload that ended for want of a sign-in, once a credential is held. */
    fun requeueSignInRefused(context: Context, ids: Collection<String>) {
        if (S5Auth.current(context) == null) return
        for (id in ids) {
            val signIn = runCatching {
                val f = file(context, id)
                f.exists() && JSONObject(f.readText()).let {
                    it.optString("state") == "refused" && it.optBoolean("signin")
                }
            }.getOrDefault(false)
            if (signIn) enqueue(context, id)
        }
    }

    fun forget(context: Context, id: String) { runCatching { file(context, id).delete() } }

    // ---- the request

    /** One path segment, encoded the way the web decodes it (encodeURIComponent). */
    internal fun seg(s: String): String = Uri.encode(s)

    internal fun captureId(input: Data): String? = input.getString(KEY_ID)

    internal const val JPEG_QUALITY = QUALITY

    /** The web answered with an error status; [code] says whether retrying can help. */
    class HttpError(val code: Int, message: String) : Exception(message) {
        /** 4xx is the web saying no to THIS request (not signed in, not staff, too
         *  big): retrying the same thing cannot help. 408/429 are waits. */
        val refused: Boolean get() = code in 400..499 && code != 408 && code != 429
    }

    /**
     * S5 answered 404 to a surface photo or to the readings PATCH: the capture's record is not on the web, though
     * this very job put it there moments ago (deleted there meanwhile -- S5 keeps no photo and no reading that no
     * record owns). Not done, and not a refusal of the room: the next run sends the record again, which puts the
     * capture back, and then its photos and readings. The message is S5's own text, as [HttpError] carried it.
     */
    class RecordMissing(e: HttpError) : Exception(e.message)

    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .build()
    }

    /** The web's answer, whatever its status: the status, the body, and every set-cookie
     *  header joined with \n (null when there was none) -- S5Session reads its mie_s from it. */
    internal class Answer(val code: Int, val body: String, val setCookie: String? = null) {
        val ok: Boolean get() = code in 200..299
    }

    /**
     * One request to the web, returning the answer WITHOUT judging its status -- for the
     * caller that has to read a refusal's body (the wish list's 409 carries S5's rows).
     * [method] is GET, POST, PUT or PATCH. A network failure still throws (IOException).
     *
     * [auth] is either an ERP key ("token key:secret", sent as Authorization) or an S5
     * session ("mie_s=<id>", sent as Cookie) -- S5's identify() tries the header first,
     * then the cookie (Amit, 22 Oct 2026: "remove the erp key pair dependency and let S5
     * be store for apk"). Blank sends neither: the sign-in itself has nothing to send yet.
     */
    internal fun exchange(method: String, url: String, auth: String, body: ByteArray?, contentType: String?): Answer {
        val b = authorized(Request.Builder().url(url), auth)
        val rb = body?.toRequestBody((contentType ?: "application/octet-stream").toMediaType())
        when (method) {
            "POST" -> b.post(rb ?: ByteArray(0).toRequestBody(null))
            "PUT" -> b.put(rb ?: ByteArray(0).toRequestBody(null))
            "PATCH" -> b.patch(rb ?: ByteArray(0).toRequestBody(null))
            else -> b.get()
        }
        return http.newCall(b.build()).execute().use { resp ->
            val cookies = resp.headers("Set-Cookie")
            Answer(resp.code, resp.body?.string() ?: "",
                if (cookies.isEmpty()) null else cookies.joinToString("\n"))
        }
    }

    /** [b] with [auth] as the web reads it: an ERP key as Authorization, an S5 session as Cookie, blank as neither. */
    private fun authorized(b: Request.Builder, auth: String): Request.Builder {
        when {
            auth.startsWith("token ") -> b.header("Authorization", auth)
            auth.isNotBlank() -> b.header("Cookie", auth)
        }
        return b
    }

    /** An image's answer ([fetchBytes]): the status, and the body's bytes only for a 200 within the cap. */
    internal class Bytes(val code: Int, val bytes: ByteArray?)

    /**
     * One GET of an image from the web, its body as BYTES -- [exchange] reads a body as a String, which a JPEG cannot be
     * (port 61c slice K3, the album's JPEG cache, AlbumImages). [auth] as for [exchange]. A body over [max] bytes is not
     * read on: its answer carries no bytes. CANCELLABLE (the K3 critic, finding 9): the call is enqueued on the same client
     * and cancelled when the coroutine that waits for it is -- a card scrolled away, the album left -- so a slot is never
     * held by a picture nobody is looking at. A network failure throws (IOException), as [exchange]'s does.
     */
    internal suspend fun fetchBytes(url: String, auth: String, max: Int): Bytes = suspendCancellableCoroutine { cont ->
        val call = http.newCall(authorized(Request.Builder().url(url), auth).get().build())
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val out = try {
                    response.use { r ->
                        val src = r.body?.byteStream()
                        if (r.code != 200 || src == null) Bytes(r.code, null)
                        else {
                            val buf = java.io.ByteArrayOutputStream()
                            val chunk = ByteArray(64 * 1024)
                            var total = 0
                            var over = false
                            while (true) {
                                val n = src.read(chunk)
                                if (n < 0) break
                                total += n
                                if (total > max) { over = true; break }
                                buf.write(chunk, 0, n)
                            }
                            if (over) Bytes(r.code, null) else Bytes(r.code, buf.toByteArray())
                        }
                    }
                } catch (e: java.io.IOException) {
                    if (cont.isActive) cont.resumeWithException(e)
                    return
                }
                if (cont.isActive) cont.resume(out)
            }
        })
    }

    /** The [HttpError] for a refused [a]: its status and S5's own error text, never more than 160 characters. */
    internal fun errorOf(a: Answer): HttpError {
        val text = runCatching { JSONObject(a.body).optString("error").ifBlank { null } }.getOrNull()
            ?: a.body
        val clean = text.replace(Regex("\\s+"), " ").take(160)
        return HttpError(a.code, UploadNote.WEB_ANSWERED_PREFIX + a.code + if (clean.isNotBlank()) ": $clean" else "")
    }

    /** One request to the web; the answer's body. [method] is GET, POST, PUT or PATCH. A refusal throws [HttpError]. */
    internal fun send(method: String, url: String, auth: String, body: ByteArray?, contentType: String?): String {
        val a = exchange(method, url, auth, body, contentType)
        if (a.ok) return a.body
        throw errorOf(a)
    }

    internal fun put(url: String, auth: String, body: ByteArray, contentType: String) {
        send("PUT", url, auth, body, contentType)
    }
}

class AsIsUploadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val id = AsIsUpload.captureId(inputData) ?: return Result.failure()

        fun waitWith(why: String): Result {
            AsIsUpload.setState(ctx, id, "waiting", why)
            return Result.retry()
        }

        // A session near its 12 h end is renewed BEFORE the first JPEG goes out, rather than
        // discovered by a 401 on a 2000 px upload. Blocking, like every request below.
        if (S5Session.signedIn(ctx)) S5Session.ensureFresh(ctx)
        val auth = S5Auth.current(ctx) ?: run {
            // Still signed in, the renewal just could not reach the web: a wait, not a refusal.
            if (S5Session.signedIn(ctx)) return waitWith(UploadNote.SESSION_TO_RENEW)
            // Nothing to send with: the job ENDS (no endless retry) and the sync re-queues it once
            // the person signs in (requeueSignInRefused).
            AsIsUpload.setState(ctx, id, "refused",
                S5Session.refusal(ctx)?.let { UploadNote.SIGN_AGAIN_PREFIX + it }
                    ?: UploadNote.NOT_SIGNED_IN, signIn = true)
            return Result.failure()
        }
        val c0 = CaptureStore(ctx).all().firstOrNull { it.deviceId == id }
        if (c0 == null) {
            // A NEW capture's row is written just after its survey prep; give it a moment.
            if (runAttemptCount < 5) return waitWith(UploadNote.WAITING_FILED)
            AsIsUpload.setState(ctx, id, "refused", UploadNote.CAPTURE_GONE)
            return Result.failure()
        }
        var c: CaptureStore.Capture = c0
        // A capture from a new site has no ERP project id until the sync resolves it --
        // unless there is no ERP leg at all (S5 sign-in, no stored pair: migration slices A+C,
        // 22 Oct 2026). Then S5 IS the store, and a project on S5 is settled by the upload
        // itself, so the site takes the project the masters hold for it, else its deterministic
        // phone key (S5Auth.keyFor), here, and goes now: two captures of one new site land on
        // one S5 project, and nothing waits on a bench this phone cannot reach.
        if (c.project.isBlank()) {
            if (FrappeClient.load(ctx) == null && S5Session.signedIn(ctx)) {
                val key = S5Auth.keyFor(ctx, c.customerName, c.projectTitle)
                CaptureStore(ctx).setProject(c.deviceId, key)
                c = c.copy(project = key)
            } else return waitWith(UploadNote.WAITING_PROJECT)
        }

        val text = SurveyPrepStore(ctx).loadText(id)
        val prepJson = text?.let { runCatching { JSONObject(it) }.getOrNull() }
        val prep = prepJson?.let { SurveyPrepStore.decode(it) }
        if (prepJson == null || prep == null) {
            AsIsUpload.setState(ctx, id, "refused", UploadNote.NO_PREP)
            return Result.failure()
        }
        val l = prepJson.optDouble("L"); val w = prepJson.optDouble("W"); val h = prepJson.optDouble("H")

        // Every address is the capture's own (AsIsPath): …/<project>/<room>/c/<deviceId>, then /img/<surface>
        // or /readings. Each segment is encoded ONCE, here, the way the web decodes it.
        fun url(segments: List<String>): String =
            BuildConfig.WEB_URL.trimEnd('/') + AsIsPath.path(segments) { AsIsUpload.seg(it) }

        return try {
            val pano = File(c.panoPath)
            val keptPano = c.kind == "360" && pano.exists()
            // ONE FRAME AND ONE SIZE (v52 slice 12): every surface's a x b, exactly as the phone squared the face --
            // through its reference rectangle when that is on -- so the web draws every mark (in mm) where the phone
            // placed it; and the frame of each surface that carries a photo (the kept 360 through a done surface).
            // `image` names the surfaces THIS job sends, before they go: the record goes first now, and a job that
            // dies between the record and a photo runs again from the record, so a record whose photos are not
            // coming never stays on the web -- and a photo S5 REFUSES, which ends the job, re-sends the record with
            // only the surfaces that landed (the photo loop below), so the flag never stays claiming a photo R2 does
            // not hold (the AS1 review, 3 Nov 2026).
            val frames = LinkedHashMap<String, SurveyPrep.Frame>()
            val surfaces = JSONObject()
            for (s in SurveyPrep.SURFACES) {
                val ab = SurveyPrep.sizeOf(s, prep, l, w, h)
                val f = if (keptPano && prep.surface(s).done()) SurveyPrep.frameOf(s, prep, l, w, h) else null
                if (f != null) frames[s] = f
                surfaces.put(s, JSONObject().put("a", ab[0]).put("b", ab[1]).put("image", f != null))
            }

            val customer = runCatching {
                val ps = CaptureStore(ctx).masters()?.optJSONArray("projects")
                var found = ""
                if (ps != null) for (i in 0 until ps.length()) {
                    val p = ps.optJSONObject(i) ?: continue
                    if (p.optString("project") == c.project) { found = p.optString("customer"); break }
                }
                found
            }.getOrDefault("")
            val appVersion = runCatching {
                ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
            }.getOrDefault("")
            // THE RECORD FIRST, under the capture's device id (port 61c slice AS1; port items 1 and 2). The body is
            // AsIsPath's -- S5's keys and JSON types, the phone's own filing as `work`, `captured_at` byte for byte
            // as every build sent it (S5's C3 renames an older MEST-PH-… row onto this id by that text), ERP's name
            // beside it as information -- and JSONObject(map) writes a null `work` as JSON null (AlbumQueue's rule:
            // `put(k, null)` would DROP the key, and with the key absent S5 files the capture at 0, the quote
            // journey, which the client sees -- not as "not filed", which the client does not; asis.js workOfRecord).
            // `site` (port 62 slice A2): the phone's name for this capture's site, as the tree shows its project. S5
            // keeps the site name on the project and takes a record's ONLY WHILE IT HOLDS NONE, so a new project
            // reaches the web named and an upload never renames one (that is PUT …/site, the rename dialog's).
            val body = AsIsPath.body(
                project = c.project, projectTitle = c.projectTitle, customer = customer,
                site = Catalogue(ctx).siteFor(CaptureStore(ctx).masters(), c.project, c.customerName, c.projectTitle),
                room = c.room,
                capture = c.deviceId, erpCapture = c.serverName,
                capturedAt = AsIsPath.capturedAt(c.createdAt, ZoneId.systemDefault()),
                appVersion = appVersion, l = l, w = w, h = h, prep = prepJson, surfaces = surfaces,
                work = c.work, stage = c.stage,
            )
            val recordUrl = url(AsIsPath.record(c.project, c.room, c.deviceId))
            // PUT the record as `body` stands NOW: first of all, and once more after a refused photo, with the surfaces
            // that did not land marked false (`surfaces` is the body's own object).
            // `created`: S5 had no row of this id before and it is no rename (C3) -- the capture is new on the web, or
            // was deleted there and is back. What this phone last saw synced of its readings is of a capture that is
            // gone, so it is forgotten HERE, before a photo can fail, and every reading goes as a first write (port
            // item 4). Not after the photos (the AS1 review, 3 Nov 2026): a failure there -- no signal mid-JPEG, memory,
            // the photo's own 404 -- runs the job again from the record, whose second PUT answers `created: false`, so
            // a forget left until then would never happen, the push would send only the keys that changed, and the
            // next pull would delete the rest on the phone as "gone from the web". Forgetting early costs nothing: every
            // key then goes with no base, which S5 writes where it has no row and answers as a conflict where it has
            // one (the web's value wins, as always). What stays open, and is not patched over here: a record PUT that
            // lands but whose answer is lost (a timeout) is `created: false` on the retry too; telling that case apart
            // needs a signal of its own (a GET by both names before the first PUT) -- a design choice for Amit.
            fun putRecord() {
                val record = JSONObject(body as Map<*, *>).toString().toByteArray(Charsets.UTF_8)
                val answer = JSONObject(AsIsUpload.send("PUT", recordUrl, auth, record, "application/json; charset=utf-8"))
                val created = answer.optBoolean("created", false)
                if (created) ReadingsSync.forget(ctx, id)
            }
            putRecord()

            // THEN THE PHOTOS, each under the capture. S5 keeps no photo that no record owns, so a 404 here means the
            // record is not on the web (deleted there since the PUT above): not done, not refused -- the job runs
            // again from the record (RecordMissing, below).
            val landed = LinkedHashSet<String>()
            if (frames.isNotEmpty()) {
                val full = FaceWriter.fullPano(pano)
                for ((s, f) in frames) {
                    val jpeg = FaceWriter.jpegOf(SurveyPrep.renderAsIs(full, s, f), AsIsUpload.JPEG_QUALITY)
                    try {
                        AsIsUpload.put(url(AsIsPath.image(c.project, c.room, c.deviceId, s)), auth, jpeg, "image/jpeg")
                    } catch (e: AsIsUpload.HttpError) {
                        if (e.code == 404) throw AsIsUpload.RecordMissing(e)
                        // REFUSED (413, 415, a 403: nothing retries it, the job ends): the record on the web says this
                        // surface and the rest are coming, and they are not. The record goes once more, naming only
                        // the surfaces that landed, before the refusal ends the job (the AS1 review, 3 Nov 2026). The
                        // failure stays the photo's -- unless that PUT is itself refused, waiting or without signal,
                        // which the handlers below judge as they judge any record PUT.
                        if (e.refused) {
                            for (t in frames.keys) if (t !in landed) surfaces.optJSONObject(t)?.put("image", false)
                            putRecord()
                        }
                        throw e
                    }
                    landed += s
                }
            }

            // THEN THE SITE READINGS, per key, against what the web last said of THIS capture (ReadingsSync, by the
            // device id). A refusal of the readings alone never blocks the room: the job ends done, with the note.
            // READ AT PUSH TIME (the as-is audit's finding 9): squaring and sending six 2000 px surfaces takes a
            // while, and a reading typed into survey prep meanwhile is saved to the prep file at once -- so the
            // readings sent are the file's NOW, not the copy taken for the record. A prep that cannot be read any
            // more (the capture removed from the phone meanwhile) sends the readings the record carried, which
            // are this capture's own and already on the web in `prep.actual`.
            val actual: Map<String, Double> = SurveyPrepStore(ctx).load(id)?.actual?.toMap() ?: prep.actual.toMap()
            val readingsNote: String? = try {
                ReadingsSync.push(ctx, id, auth, c.project, c.room, c.deviceId, actual)
            } catch (e: AsIsUpload.HttpError) {
                // A 404 is the photo's 404 (S5: "This capture is not on the web: deleted, or never uploaded"): the
                // capture this job put on the web is gone again, and the job runs again from the record -- never
                // "On web ✓ · web readings not available yet" for a capture that is not on the web at all (the AS1
                // review, 3 Nov 2026). Until AS1 a 404 or 405 here was a web without the readings endpoint yet; the
                // explicit routes landed on S5 together, so a web that took the record above has this one, and that
                // reading of a 404 is now the pull's alone (survey prep, through ReadingsSync's notAvailable).
                if (e.code == 404) throw AsIsUpload.RecordMissing(e)
                if (e.refused) UploadNote.READINGS_NOT_SENT_PREFIX + e.message else throw e
            }
            // DONE, and S5 was told the filing the record carried (the sweep's done-mark, FW2): the body's own
            // `work`, so the mark and the record cannot disagree. A pick moved on the phone while this job ran is
            // the sweep's to send (Album.needsRefile), not this job's to guess.
            AsIsUpload.setState(ctx, id, "done", readingsNote, image = landed.isNotEmpty(),
                told = AsIsUpload.Told(body["work"] as Int?))
            Result.success()
        } catch (e: AsIsUpload.HttpError) {
            // An EXPIRED S5 SESSION is not a refusal of this room: sign in again with the
            // stored email and password and let WorkManager run the job again. Only a 401
            // while signed in to S5 -- a 401 on the legacy ERP key stays a refusal, as before.
            if (e.code == 401 && S5Session.signedIn(ctx)) {
                if (S5Session.relogin(ctx)) return waitWith(UploadNote.SESSION_RENEWED)
                // Not renewed but still signed in: the web could not be reached -- try later.
                if (S5Session.signedIn(ctx)) return waitWith(UploadNote.SESSION_EXPIRED)
                // REFUSED: relogin signed the phone out of S5 rather than post that password to
                // ERP again. A legacy pair still held is used from the next run (S5Auth.current);
                // with none, the job ends and asks for a sign-in, and is re-queued after one.
                if (FrappeClient.authorization(ctx) != null)
                    return waitWith(UploadNote.SIGNIN_REFUSED_KEY)
                AsIsUpload.setState(ctx, id, "refused", UploadNote.SIGN_AGAIN_PREFIX +
                    (S5Session.refusal(ctx) ?: UploadNote.WEB_REFUSED_PASSWORD), signIn = true)
                return Result.failure()
            }
            // "refused" ENDS the job; "error" is a job WorkManager will run again (stateOf).
            AsIsUpload.setState(ctx, id, if (e.refused) "refused" else "error", e.message ?: "refused") // text:value a stored note; HttpError always carries a message, so unreachable in practice — if ever stored it draws as stored
            if (e.refused) Result.failure() else Result.retry()
        } catch (e: AsIsUpload.RecordMissing) {
            // A photo's or the readings PATCH's 404: the record this job sent is not on the web any more. "error" is a
            // job WorkManager runs again (stateOf), and the next run starts with the record, which puts the capture back.
            AsIsUpload.setState(ctx, id, "error", e.message)
            Result.retry()
        } catch (e: OutOfMemoryError) {
            AsIsUpload.setState(ctx, id, "error", UploadNote.NO_MEMORY)
            Result.retry()
        } catch (e: Exception) {
            AsIsUpload.setState(ctx, id, "error", (e.message ?: e.javaClass.simpleName).take(200))
            Result.retry()
        }
    }
}
