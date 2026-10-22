package com.malletcrafts.interiorestimate

import android.content.Context
import android.util.Log
import com.malletcrafts.interiorestimate.pano.QuoteStage
import org.json.JSONObject
import java.time.Instant

/**
 * THE QUOTE STAGE, READ FROM S5 (v52 item 16; Amit, 14 Oct 2026, 06:53 IST: "survey, client
 * wish list, vendor quoted, client approved, estimated"). Staff move a project's quote stage
 * on MCFT Interior Estimate's /projects page; the phone only shows it.
 *
 * ONE GET <WEB_URL>/api/asis per read, with the same ERP Authorization header ReadingsSync
 * sends. The LIST, not GET /api/asis/:project: that call also settles the project and lists
 * its photos, which is not work to cause once per project per sync. The answer is cached per
 * ERP project id (Project.serverId) in preferences with the time this phone read it, and a
 * known project the list does not carry is cached as not listed.
 *
 * READ-ONLY AND NEVER A FAILURE: any refusal (401, 403, 404, 405), a 5xx, no network or an
 * answer that does not parse is one Log.w and nothing else -- the cached copy stays, and the
 * sync that called this is not told, so S5 being unreachable can never make the capture
 * upload back off. No retry of its own: the next sync or the next project open reads again.
 */
object QuoteStageSync {

    private const val TAG = "QuoteStage"
    private const val PREFS = "quote_stage"

    /** The cache's preferences; the project screen listens on them so any write redraws the line. */
    fun prefs(context: Context): android.content.SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun url() = BuildConfig.WEB_URL.trimEnd('/') + "/api/asis"

    private fun encode(c: QuoteStage.Cache): String = JSONObject()
        .put("listed", c.listed).put("stage", c.stage).put("by", c.by)
        .put("at", c.at).put("readAt", c.readAt).toString()

    /** The cached copy for one ERP project id, or null when this phone has never read it. */
    fun cached(context: Context, serverId: String): QuoteStage.Cache? {
        if (serverId.isBlank()) return null
        val raw = prefs(context).getString(serverId, null) ?: return null
        return try {
            val o = JSONObject(raw)
            QuoteStage.Cache(
                listed = o.optBoolean("listed", false),
                stage = o.optString("stage"),
                by = o.optString("by"),
                at = o.optString("at"),
                readAt = o.optString("readAt"))
        } catch (e: Exception) {
            null
        }
    }

    /** The line the project screen draws. A project with no ERP id is only on this phone, so S5 is not consulted. */
    fun lineFor(context: Context, project: Catalogue.Project): QuoteStage.Line =
        if (!project.synced) QuoteStage.phoneOnly()
        else QuoteStage.lineFor(cached(context, project.serverId), Instant.now())

    /**
     * Read S5's list once and cache it for [known] ERP project ids. Blocking; call it off the
     * main thread. Returns whether the read succeeded. Never throws, never retries.
     */
    fun refresh(context: Context, known: Collection<String>): Boolean {
        val ids = known.filter { it.isNotBlank() }.distinct()
        // Only phone-only projects: nothing on this phone can be on S5, so S5 is not asked.
        if (ids.isEmpty()) return false
        val auth = FrappeClient.authorization(context) ?: return false
        return try {
            val body = JSONObject(AsIsUpload.send("GET", url(), auth, null, null))
            // A 200 without the list is not "nothing listed": it would mark every project not sent.
            val arr = body.getJSONArray("projects")
            val listed = mutableListOf<QuoteStage.Listed>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("project")
                if (id.isBlank()) continue
                listed.add(QuoteStage.Listed(id,
                    if (o.isNull("quote_stage")) "" else o.optString("quote_stage"),
                    if (o.isNull("quote_stage_by")) "" else o.optString("quote_stage_by"),
                    if (o.isNull("quote_stage_at")) "" else o.optString("quote_stage_at")))
            }
            val cache = QuoteStage.fromList(listed, ids, Instant.now().toString())
            val ed = prefs(context).edit()
            for ((id, c) in cache) ed.putString(id, encode(c))
            ed.apply()
            true
        } catch (e: Exception) {
            // Never the header: the message is the status and S5's own error text at most.
            Log.w(TAG, "quote stage not read from S5: " + (e.message?.take(160) ?: e.javaClass.simpleName))
            false
        }
    }
}
