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
import com.malletcrafts.interiorestimate.pano.SurveyPrep
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

/**
 * AS-IS CAPTURE, END TO END (contract of 2026-10-11, slice 1): a saved survey
 * prep goes to the web half of MCFT Interior Estimate, where staff, the
 * project's vendors and its client open project -> room -> the room as it is.
 *
 * After "Save survey prep", one unique job per capture: each of the six
 * surfaces squared to size from the kept 360 -- NO margin, the picture is the
 * surface rectangle, long side 2000 px (SurveyPrep.asisSquare) -- PUT as a
 * JPEG, then the room record. The 360 gone: the record alone, image false.
 * Saving again REPLACES the queued job, so the web always ends on the latest.
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

    /** [signIn]: a "refused" that ended for want of a sign-in, so the sync re-queues it once one
     *  is held (requeueSignInRefused) -- unlike a refusal of the room itself, which waits for the
     *  person to save the survey prep again. */
    internal fun setState(context: Context, id: String, state: String, error: String?, image: Boolean? = null,
                          signIn: Boolean = false) {
        runCatching {
            val o = JSONObject().put("state", state).put("at", System.currentTimeMillis())
            if (error != null) o.put("error", error)
            if (image != null) o.put("image", image)
            if (signIn) o.put("signin", true)
            file(context, id).writeText(o.toString())
        }
    }

    /** What survey prep shows, and whether it is good news; null when this capture was never queued. */
    fun status(context: Context, id: String): Pair<String, Boolean>? = runCatching<Pair<String, Boolean>?> {
        val f = file(context, id)
        if (!f.exists()) return@runCatching null
        val o = JSONObject(f.readText())
        when (o.optString("state")) {
            "done" -> {
                val hm = Instant.ofEpochMilli(o.optLong("at")).atZone(ZoneId.systemDefault())
                    .toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                val extra = o.optString("error", "").let { if (it.isBlank()) "" else " · $it" }
                ("On web ✓ $hm" + (if (o.optBoolean("image", true)) "" else " · no photos, the 360 is not on this phone") + extra) to true
            }
            "error", "refused" -> ("Not on web: " + o.optString("error", "upload failed")) to false
            else -> ("Waiting to upload" + o.optString("error", "").let { if (it.isBlank()) "" else " · $it" }) to false
        }
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
        val b = Request.Builder().url(url)
        when {
            auth.startsWith("token ") -> b.header("Authorization", auth)
            auth.isNotBlank() -> b.header("Cookie", auth)
        }
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

    /** The [HttpError] for a refused [a]: its status and S5's own error text, never more than 160 characters. */
    internal fun errorOf(a: Answer): HttpError {
        val text = runCatching { JSONObject(a.body).optString("error").ifBlank { null } }.getOrNull()
            ?: a.body
        val clean = text.replace(Regex("\\s+"), " ").take(160)
        return HttpError(a.code, "web answered ${a.code}" + if (clean.isNotBlank()) ": $clean" else "")
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
            if (S5Session.signedIn(ctx)) return waitWith("web session to be renewed when the web answers")
            // Nothing to send with: the job ENDS (no endless retry) and the sync re-queues it once
            // the person signs in (requeueSignInRefused).
            AsIsUpload.setState(ctx, id, "refused",
                S5Session.refusal(ctx)?.let { "sign in again (menu → Server) - $it" }
                    ?: "not signed in - open the menu and sign in", signIn = true)
            return Result.failure()
        }
        val c0 = CaptureStore(ctx).all().firstOrNull { it.deviceId == id }
        if (c0 == null) {
            // A NEW capture's row is written just after its survey prep; give it a moment.
            if (runAttemptCount < 5) return waitWith("waiting for the capture to be filed")
            AsIsUpload.setState(ctx, id, "refused", "this capture is no longer on the phone")
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
            } else return waitWith("waiting for the project to reach ERP")
        }

        val text = SurveyPrepStore(ctx).loadText(id)
        val prepJson = text?.let { runCatching { JSONObject(it) }.getOrNull() }
        val prep = prepJson?.let { SurveyPrepStore.decode(it) }
        if (prepJson == null || prep == null) {
            AsIsUpload.setState(ctx, id, "refused", "no survey prep saved for this capture")
            return Result.failure()
        }
        val l = prepJson.optDouble("L"); val w = prepJson.optDouble("W"); val h = prepJson.optDouble("H")

        val base = BuildConfig.WEB_URL.trimEnd('/') + "/api/asis/" +
            AsIsUpload.seg(c.project) + "/" + AsIsUpload.seg(c.room)

        return try {
            val pano = File(c.panoPath)
            val keptPano = c.kind == "360" && pano.exists()
            val surfaces = JSONObject()
            var anyImage = false
            if (keptPano) {
                val full = FaceWriter.fullPano(pano)
                for (s in SurveyPrep.SURFACES) {
                    // ONE FRAME AND ONE SIZE (v52 slice 12): the picture is squared, and its a x b sent, exactly as the
                    // phone squared the face -- through its reference rectangle when that is on -- so the web draws
                    // every mark (in mm) where the phone placed it
                    val ab = SurveyPrep.sizeOf(s, prep, l, w, h)
                    val f = if (prep.surface(s).done()) SurveyPrep.frameOf(s, prep, l, w, h) else null
                    var sent = false
                    if (f != null) {
                        val jpeg = FaceWriter.jpegOf(SurveyPrep.renderAsIs(full, s, f), AsIsUpload.JPEG_QUALITY)
                        AsIsUpload.put("$base/img/${AsIsUpload.seg(s)}", auth, jpeg, "image/jpeg")
                        sent = true; anyImage = true
                    }
                    surfaces.put(s, JSONObject().put("a", ab[0]).put("b", ab[1]).put("image", sent))
                }
            } else {
                for (s in SurveyPrep.SURFACES) {
                    val ab = SurveyPrep.sizeOf(s, prep, l, w, h)
                    surfaces.put(s, JSONObject().put("a", ab[0]).put("b", ab[1]).put("image", false))
                }
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
            val record = JSONObject()
                .put("v", 1)
                .put("project", c.project)
                .put("project_title", c.projectTitle)
                .put("customer", customer)
                .put("room", c.room)
                .put("capture", c.serverName ?: c.deviceId)
                .put("captured_at", Instant.ofEpochMilli(c.createdAt).atZone(ZoneId.systemDefault())
                    .toOffsetDateTime().toString())
                .put("app_version", appVersion)
                .put("L", l).put("W", w).put("H", h)
                .put("prep", prepJson)
                .put("surfaces", surfaces)
            AsIsUpload.put(base, auth, record.toString().toByteArray(Charsets.UTF_8),
                "application/json; charset=utf-8")
            // Then the site readings, per key, against what the web last said
            // (ReadingsSync). Never blocks the room: a web without the readings
            // endpoint yet answers quietly.
            val readingsNote: String? = try {
                ReadingsSync.push(ctx, id, auth, c.project, c.room, prep.actual.toMap())
            } catch (e: AsIsUpload.HttpError) {
                if (ReadingsSync.notAvailable(e)) ReadingsSync.NOT_YET
                else if (e.refused) "readings not sent: ${e.message}"
                else throw e
            }
            AsIsUpload.setState(ctx, id, "done", readingsNote, image = anyImage)
            Result.success()
        } catch (e: AsIsUpload.HttpError) {
            // An EXPIRED S5 SESSION is not a refusal of this room: sign in again with the
            // stored email and password and let WorkManager run the job again. Only a 401
            // while signed in to S5 -- a 401 on the legacy ERP key stays a refusal, as before.
            if (e.code == 401 && S5Session.signedIn(ctx)) {
                if (S5Session.relogin(ctx)) return waitWith("web session renewed")
                // Not renewed but still signed in: the web could not be reached -- try later.
                if (S5Session.signedIn(ctx)) return waitWith("web session expired - renewing when the web answers")
                // REFUSED: relogin signed the phone out of S5 rather than post that password to
                // ERP again. A legacy pair still held is used from the next run (S5Auth.current);
                // with none, the job ends and asks for a sign-in, and is re-queued after one.
                if (FrappeClient.authorization(ctx) != null)
                    return waitWith("web sign-in refused - sending with the ERP key")
                AsIsUpload.setState(ctx, id, "refused", "sign in again (menu → Server) - " +
                    (S5Session.refusal(ctx) ?: "the web refused the saved password"), signIn = true)
                return Result.failure()
            }
            // "refused" ENDS the job; "error" is a job WorkManager will run again (stateOf).
            AsIsUpload.setState(ctx, id, if (e.refused) "refused" else "error", e.message ?: "refused")
            if (e.refused) Result.failure() else Result.retry()
        } catch (e: OutOfMemoryError) {
            AsIsUpload.setState(ctx, id, "error", "not enough memory to square the 360 — will try again")
            Result.retry()
        } catch (e: Exception) {
            AsIsUpload.setState(ctx, id, "error", (e.message ?: e.javaClass.simpleName).take(200))
            Result.retry()
        }
    }
}
