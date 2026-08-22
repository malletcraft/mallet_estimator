package com.malletcrafts.sitephotos

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * The same three-call sequence the PWA performs, with the same endpoints —
 * the app is a different shell over one server contract, which is what keeps
 * web and Android functionally identical.
 *
 * Auth is an API key pair, not a session: Frappe sessions expire in hours and
 * a site visit is longer, so the phone holds `token key:secret` and sync
 * works after any number of offline days.
 */
class FrappeClient(private val baseUrl: String, private val key: String,
                   private val secret: String) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)      // the pano is tens of MB
        .build()

    private val json = "application/json; charset=utf-8".toMediaType()

    class ApiException(val code: Int, message: String) : Exception(message)

    private fun call(req: Request): JSONObject {
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                // Frappe puts the useful sentence in `exception`; the rest is
                // traceback nobody can act on from a phone.
                val hint = runCatching {
                    JSONObject(body).optString("exception").take(200)
                }.getOrNull().takeUnless { it.isNullOrBlank() } ?: "HTTP ${resp.code}"
                throw ApiException(resp.code, hint)
            }
            return JSONObject(body)
        }
    }

    /**
     * A whitelisted call, ALREADY UNWRAPPED.
     *
     * Frappe wraps every reply as {"message": ...} and this returns the
     * inside, not the envelope. Said here because forgetting it does not
     * fail loudly: a second .optJSONObject("message") yields null, every
     * caller has a graceful path for null, and the feature simply never
     * works. Five call sites had it, and the cross-device annotation pull
     * was one of them — it had been returning an empty map since the day it
     * was written.
     */
    private fun post(method: String, payload: JSONObject): JSONObject {
        val req = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/method/$method")
            .header("Authorization", "token $key:$secret")
            .post(payload.toString().toRequestBody(json))
            .build()
        return call(req).getJSONObject("message")
    }

    fun bootstrap(): JSONObject =
        post("mallet_estimator.sitephoto.bootstrap", JSONObject())

    fun createCapture(project: String, room: String, captureDate: String,
                      stage: String, deviceCaptureId: String,
                      appVersion: String = "", sku: String = "",
                      workStage: String = "", captureKind: String = "360"): JSONObject =
        post("mallet_estimator.sitephoto.create_capture", JSONObject()
            .put("project", project)
            .put("room", room)
            .put("capture_date", captureDate)
            // Two fields, on purpose. work_stage is one of the thirty-nine and
            // is what the server files against; stage is the PHASE, and is the
            // only thing a bench without the stage master can understand. Send
            // the phone's choice as work_stage or it is silently replaced by
            // whatever the project happened to be at.
            .put("work_stage", workStage)
            .put("capture_kind", captureKind)
            .put("stage", stage)
            // A SKU tagged on the phone before the capture ever left it. Sent
            // blank rather than omitted so an older bench, which ignores the
            // argument entirely, behaves the same either way.
            .put("sku", sku)
            .put("device_capture_id", deviceCaptureId)
            // The fleet's version ledger: the server records which build
            // synced this capture, so "did the phone update?" is a server
            // query instead of a hands-on-device check.
            .put("app_version", appVersion))

    fun saveAnnotations(docname: String, face: String, data: JSONObject): JSONObject =
        post("mallet_estimator.sitephoto.save_annotations", JSONObject()
            .put("name", docname)
            .put("face", face)
            .put("data", data.toString()))

    /** Every face's annotations for one capture — what somebody else's
     *  phone measured. Keyed by face name. */
    fun getAnnotations(docname: String): JSONObject =
        post("mallet_estimator.sitephoto.get_annotations", JSONObject()
            .put("name", docname))

    fun appUpdateInfo(): JSONObject =
        post("mallet_estimator.app_update.app_update_info", JSONObject())

    /** Streams a private site file to [dest] over the same token auth —
     *  the OTA download; the APK is far too big for a string body. */
    fun downloadPrivate(fileUrl: String, dest: java.io.File) {
        val req = Request.Builder().url("$baseUrl$fileUrl")
            .header("Authorization", "token $key:$secret").get().build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw ApiException(resp.code, "download: HTTP ${resp.code}")
            dest.parentFile?.mkdirs()
            dest.outputStream().use { out ->
                resp.body!!.byteStream().copyTo(out)
            }
        }
    }

    /** Returns the private file_url the server stored the pano under. */
    fun uploadPano(docname: String, pano: File): String {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", pano.name,
                pano.asRequestBody("image/jpeg".toMediaType()))
            .addFormDataPart("is_private", "1")
            .addFormDataPart("doctype", "Site Photo 360")
            .addFormDataPart("docname", docname)
            .addFormDataPart("fieldname", "pano")
            .build()
        val req = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/method/upload_file")
            .header("Authorization", "token $key:$secret")
            .post(body)
            .build()
        return call(req).getJSONObject("message").getString("file_url")
    }

    /** A voice note, attached to its capture. Multipart like the pano and
     *  for the same reason: audio is binary, and base64 in a JSON body is
     *  a third bigger for no gain. Returns the private file_url the pin
     *  will point at. */
    fun uploadAudioNote(docname: String, clip: File): String {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", clip.name,
                clip.asRequestBody("audio/mp4".toMediaType()))
            .addFormDataPart("is_private", "1")
            .addFormDataPart("doctype", "Site Photo 360")
            .addFormDataPart("docname", docname)
            .build()
        val req = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/method/upload_file")
            .header("Authorization", "token $key:$secret")
            .post(body)
            .build()
        return call(req).getJSONObject("message").getString("file_url")
    }

    /**
     * A marked-up copy home, and filed against the face it belongs to.
     *
     * Two calls because Frappe splits them: upload_file stores the bytes,
     * sitephoto.annotate appends the row that says which face they are. The
     * generated face itself is never overwritten — annotation is a LAYER, so
     * a re-split at a different FOV cannot destroy somebody's markup.
     *
     * This is the leg the Drive round trip could not do. ImageMeter renames
     * what it exports, so the bench had no way to tell which capture a
     * returned file belonged to and 88 of them queued for a human to guess.
     * The phone knows, because it read its own stamp out of the picture.
     */
    fun uploadAnnotation(docname: String, face: String, image: File,
                         note: String? = null): JSONObject {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", image.name,
                image.asRequestBody("image/jpeg".toMediaType()))
            .addFormDataPart("is_private", "1")
            .addFormDataPart("doctype", "Site Photo 360")
            .addFormDataPart("docname", docname)
            .build()
        val req = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/method/upload_file")
            .header("Authorization", "token $key:$secret")
            .post(body)
            .build()
        val fileUrl = call(req).getJSONObject("message").getString("file_url")
        return post("mallet_estimator.sitephoto.annotate", JSONObject()
            .put("photo", docname)
            .put("face", face)
            .put("file_url", fileUrl)
            .put("note", note))
    }

    /** Correct a client, site or project name. kind is "client" | "site" |
     *  "project"; name is the ERP docname. The server refuses a name another
     *  record already holds, so a clash comes back as a message rather than
     *  as two customers quietly becoming one. */
    fun renameNode(kind: String, name: String, newName: String): JSONObject =
        post("mallet_estimator.sitephoto.rename_node", JSONObject()
            .put("kind", kind).put("name", name).put("new_name", newName))

    /** What work is expected on ONE face. Blank clears it. A 360 is a whole
     *  room and cannot be one article; each of its faces is one wall, and
     *  that is what a SKU describes. */
    fun setFaceSku(docname: String, face: String, sku: String): JSONObject =
        post("mallet_estimator.sitephoto.set_face_sku", JSONObject()
            .put("name", docname).put("face", face).put("sku", sku))

    fun bindPano(docname: String, fileUrl: String): JSONObject =
        post("mallet_estimator.sitephoto.bind_pano", JSONObject()
            .put("name", docname)
            .put("file_url", fileUrl))

    /** Run the ImageMeter round trip now instead of waiting for the hourly
     *  scheduler: faces out to the Drive handover folder, annotated copies
     *  back onto the capture they came from. */
    /** QUEUED, not done inline. sync() pushes and pulls hundreds of Drive
     *  files inside the request; the read timeout here is 120 s, so the
     *  button reliably timed out while the work was in fact starting. */
    fun imagemeterSync(): JSONObject =
        post("mallet_estimator.imagemeter_sync.sync_async", JSONObject())

    /** What the last Drive round trip actually did. "Queued" says the button
     *  worked; this says whether anything came back. */
    fun imagemeterStatus(): JSONObject =
        post("mallet_estimator.imagemeter_sync.status", JSONObject())

    /** One capture in full, including the annotated images ImageMeter
     *  returned, keyed by face. */
    fun captureDetail(docname: String): JSONObject =
        post("mallet_estimator.sitephoto.detail", JSONObject().put("name", docname))

    /** Move a project to a stage. The server refuses a stage the project's
     *  job type never reaches, so a phone carrying a stale master list gets
     *  a refusal rather than a wrong stage. */
    fun setProjectStage(project: String, workStage: String): JSONObject =
        post("mallet_estimator.sitephoto.set_project_stage",
            JSONObject().put("project", project).put("work_stage", workStage))

    /** Re-file one capture: its stage, its SKU, or both.
     *
     *  Null means "leave alone", "" means "clear". Both distinctions matter:
     *  moving a photo's stage must not silently drop its SKU, and untagging
     *  a photo is a real thing to want. */
    fun setCaptureTags(name: String, workStage: String? = null,
                       sku: String? = null): JSONObject {
        val body = JSONObject().put("name", name)
        if (workStage != null) body.put("work_stage", workStage)
        if (sku != null) body.put("sku", sku)
        return post("mallet_estimator.sitephoto.set_capture_tags", body)
    }

    /** Record work the SITE says is needed.
     *
     *  deviceSkuId is what makes this safe to retry: the bench returns the
     *  SAME SKU for an id it has seen. It cannot key on the code, because two
     *  wardrobes in one room legitimately compute the same one. */
    fun createSku(
        project: String,
        room: String,
        articleCode: String,
        deviceSkuId: String,
        qty: Double? = null,
        widthMm: Int? = null,
        heightMm: Int? = null,
        depthMm: Int? = null,
        note: String = "",
    ): JSONObject {
        val body = JSONObject()
            .put("project", project)
            .put("room", room)
            .put("article_code", articleCode)
            .put("device_sku_id", deviceSkuId)
        qty?.let { body.put("qty", it) }
        widthMm?.let { body.put("width_mm", it) }
        heightMm?.let { body.put("height_mm", it) }
        depthMm?.let { body.put("depth_mm", it) }
        if (note.isNotBlank()) body.put("note", note)
        return post("mallet_estimator.sitephoto.create_sku", body)
    }

    /** Turn a client/site/project typed offline at a NEW site into real
     *  masters — or, far more often, match them against masters that already
     *  exist. The server matches insensitively before it ever creates.
     *
     *  The site and job type are optional so an older bench, which knows
     *  neither, still answers instead of erroring on an unexpected argument. */
    fun ensureSite(
        customerName: String,
        projectTitle: String,
        siteName: String = "",
        jobType: String = "",
    ): JSONObject = post("mallet_estimator.sitephoto.ensure_site", JSONObject()
        .put("customer_name", customerName)
        .put("project_title", projectTitle)
        .apply {
            if (siteName.isNotBlank()) put("site_name", siteName)
            if (jobType.isNotBlank()) put("job_type", jobType)
        })

    companion object {
        private const val PREFS = "mcft_settings"

        /** Stored app-private; the process is the trust boundary on a work
         *  profile phone, and a wrong abstraction here (half-configured
         *  encrypted prefs) fails at first sync in a way plain prefs never
         *  do. Revisit when technicians bring their own devices. */
        fun save(context: Context, url: String, key: String, secret: String) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("url", url.trim().trimEnd('/'))
                .putString("key", key.trim())
                .putString("secret", secret.trim())
                .apply()
        }

        fun load(context: Context): FrappeClient? {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val url = p.getString("url", null) ?: return null
            val key = p.getString("key", null) ?: return null
            val secret = p.getString("secret", null) ?: return null
            if (url.isBlank() || key.isBlank() || secret.isBlank()) return null
            return FrappeClient(url, key, secret)
        }

        fun configured(context: Context): Boolean = load(context) != null

        /** Sign out: drop the credentials, keep the queue. A technician
         *  handing the phone on should not take unsent captures with them. */
        fun forget(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .remove("key").remove("secret").apply()
        }

        fun savedUrl(context: Context): String =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("url", "https://mcft-stg.frappe.cloud") ?: ""
    }
}
