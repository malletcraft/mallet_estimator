package com.malletcrafts.sitephotos

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.malletcrafts.sitephotos.pano.Panorama
import com.malletcrafts.sitephotos.pano.RoomQuad
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * The room's eight corners found by Claude, through S5 QUOTE's
 * `POST /api/room/corners` -- so nobody sets the room by hand when a 360 is
 * split. Amit, 2026-10-09: "get rid of set room corner as well", and, choosing
 * where it runs, "S5 Cloudflare function".
 *
 * THE PHONE HOLDS NO ANTHROPIC KEY. It sends the pano to S5 with S5's own
 * admin token, and S5 holds the key as a Cloudflare secret. The token is typed
 * once on the split screen and kept app-private, like the bench credentials.
 *
 * Stage talks to stage: the URL is S5 STAGE, never production.
 *
 * The answer is only a starting room. When Claude's corners fail S5's checks,
 * or do not close a room round the camera, the split says so and the room is
 * set by hand in Match Photo exactly as before -- that screen stays as the
 * correction, not as a second way in.
 */
object CornerFinder {
    const val URL = "https://stage.estimate.woodugift.com/api/room/corners"
    private const val PREFS = "mcft_settings"
    private const val KEY = "s5_admin_token"

    /** About 2048 x 1024 is what S5 asks for; the split already holds it. */
    private const val W = 2048
    private const val H = 1024

    fun token(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)?.takeIf { it.isNotBlank() }

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, token.trim()).apply()
    }

    sealed class Result {
        class Found(val room: RoomQuad, val confidence: Double, val note: String, val occluded: List<String>) : Result()
        class Failed(val why: String) : Result()
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Blocking: call off the main thread. */
    fun find(token: String, pano: Panorama.Image, room: String, heightMm: Double?, lengthMm: Double?, widthMm: Double?): Result {
        val body = JSONObject()
            .put("image", jpegBase64(pano))
            .put("width", W).put("height", H)
            .put("room", room)
            .put("hint", JSONObject().apply {
                heightMm?.let { put("heightMm", it) }
                lengthMm?.let { put("lengthMm", it) }
                widthMm?.let { put("widthMm", it) }
            })
        val req = Request.Builder().url(URL)
            .header("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            http.newCall(req).execute().use { r ->
                val text = r.body?.string().orEmpty()
                val j = runCatching { JSONObject(text) }.getOrNull()
                when {
                    r.code == 401 -> Result.Failed("S5 refused the token (401) — check it")
                    r.code == 422 -> Result.Failed("Claude's corners failed the checks: " +
                        (j?.optString("error") ?: "no reason given"))
                    !r.isSuccessful || j == null -> Result.Failed("S5 answered ${r.code}" +
                        (j?.optString("error")?.let { ": $it" } ?: ""))
                    else -> {
                        val d = j.getJSONObject("dirs")
                        val q = RoomQuad.fromCorners(dirs(d.getJSONArray("ceiling")), dirs(d.getJSONArray("floor")))
                        if (q == null) Result.Failed("Claude's corners do not close a room round the camera")
                        else Result.Found(q, j.optDouble("confidence", 0.0), j.optString("note"),
                            j.optJSONArray("occluded")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList())
                    }
                }
            }
        } catch (e: Exception) {
            Result.Failed("No answer from S5: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun dirs(a: JSONArray): List<DoubleArray> = (0 until a.length()).map { i ->
        val p = a.getJSONArray(i); doubleArrayOf(p.getDouble(0), p.getDouble(1), p.getDouble(2))
    }

    /** Exactly 2:1, because S5 refuses anything else and reads the size from the JPEG. */
    private fun jpegBase64(pano: Panorama.Image): String {
        val src = Bitmap.createBitmap(pano.width, pano.height, Bitmap.Config.ARGB_8888)
        val px = IntArray(pano.pixels.size)
        for (i in px.indices) px[i] = pano.pixels[i] or (0xFF shl 24)
        src.setPixels(px, 0, pano.width, 0, 0, pano.width, pano.height)
        val sized = Bitmap.createScaledBitmap(src, W, H, true)
        val out = ByteArrayOutputStream()
        sized.compress(Bitmap.CompressFormat.JPEG, 85, out)
        if (sized !== src) sized.recycle()
        src.recycle()
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
