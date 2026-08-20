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
                      appVersion: String = ""): JSONObject =
        post("mallet_estimator.sitephoto.create_capture", JSONObject()
            .put("project", project)
            .put("room", room)
            .put("capture_date", captureDate)
            .put("stage", stage)
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

    fun bindPano(docname: String, fileUrl: String): JSONObject =
        post("mallet_estimator.sitephoto.bind_pano", JSONObject()
            .put("name", docname)
            .put("file_url", fileUrl))

    /** Turn a client/project typed offline at a NEW site into real masters —
     *  or, far more often, match them against masters that already exist.
     *  The server matches insensitively before it ever creates. */
    fun ensureSite(customerName: String, projectTitle: String): JSONObject =
        post("mallet_estimator.sitephoto.ensure_site", JSONObject()
            .put("customer_name", customerName)
            .put("project_title", projectTitle))

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

        fun savedUrl(context: Context): String =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("url", "https://mcft-stg.frappe.cloud") ?: ""
    }
}
