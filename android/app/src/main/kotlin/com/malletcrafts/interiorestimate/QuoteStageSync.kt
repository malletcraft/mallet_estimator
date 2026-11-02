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
 * ONE GET <WEB_URL>/api/asis per read, with the same credentials ReadingsSync sends
 * (S5Auth.fresh: the S5 session when signed in, renewed near its end, else the legacy ERP key -- 22 Oct 2026). The LIST, not GET /api/asis/:project: that call also settles the project and lists
 * its photos, which is not work to cause once per project per sync. The answer is cached per
 * ERP project id (Project.serverId) in preferences with the time this phone's read went out, and
 * a known project the list does not carry is cached as not listed -- each id only where nothing
 * that went out LATER has written it (QuoteStage.fromList, ordered by [seq]), since reads overlap:
 * the project-open read, a sync's, the wish list's own after S5 named the stage ([named]).
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

    // Guards [seq] and [written], and every write of the cache: [named]'s read-and-write and
    // [refresh]'s ordered write, so a slow read of the list can never write over a stage S5 named,
    // or another read took, after that read went out (QuoteStage.fromList).
    private val LOCK = Any()

    // The one sequence a read takes as its GET goes out and a [named] write takes as it writes,
    // and per ERP id the seq of the write that put its stored copy there. In memory: a read in
    // flight never outlives the process, and a fresh process has no read to order against.
    private var seq = 0L
    private val written = HashMap<String, Long>()

    private fun encode(c: QuoteStage.Cache): String = JSONObject()
        .put("listed", c.listed).put("stage", c.stage).put("by", c.by)
        .put("at", c.at).put("readAt", c.readAt).toString()

    private fun decode(raw: String?): QuoteStage.Cache? {
        if (raw == null) return null
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

    /** The cached copy for one ERP project id, or null when this phone has never read it. */
    fun cached(context: Context, serverId: String): QuoteStage.Cache? {
        if (serverId.isBlank()) return null
        return decode(prefs(context).getString(serverId, null))
    }

    /**
     * An answer of S5's that named [serverId]'s stage (the wish list PUT's refusals for the stage,
     * WishList.Kind409.forStage), written into this cache by QuoteStage.named, so the Wish list
     * and the project line take what S5 has just said rather than the copy read earlier in the
     * sync. Returns whether it wrote: then the caller re-reads the list ([refresh]) for who set
     * the stage and when, which the answer does not carry. Never throws.
     */
    fun named(context: Context, serverId: String, stage: String): Boolean {
        if (serverId.isBlank()) return false
        return try {
            synchronized(LOCK) {
                val next = QuoteStage.named(cached(context, serverId), stage, Instant.now().toString())
                    ?: return false
                prefs(context).edit().putString(serverId, encode(next)).apply()
                written[serverId] = ++seq
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "quote stage from S5's answer not kept: " + // text:value log line
                (e.message?.take(160) ?: e.javaClass.simpleName))
            false
        }
    }

    /**
     * The line the project screen and the Wish list draw for the ERP id [stageKey] the stage is read
     * under (WishSync.stageKey: the project's own, or the id its moved list now has). Null or blank:
     * the project is only on this phone, so S5 is not consulted.
     */
    fun lineFor(context: Context, stageKey: String?): QuoteStage.Line =
        if (stageKey.isNullOrBlank()) QuoteStage.phoneOnly()
        else QuoteStage.lineFor(cached(context, stageKey), Instant.now())

    /**
     * Read S5's list once and cache it for [known] ERP project ids. Blocking; call it off the
     * main thread. Returns whether the read succeeded. Never throws, never retries.
     */
    fun refresh(context: Context, known: Collection<String>): Boolean {
        val ids = known.filter { it.isNotBlank() }.distinct()
        // Only phone-only projects: nothing on this phone can be on S5, so S5 is not asked.
        if (ids.isEmpty()) return false
        val auth = S5Auth.fresh(context) ?: return false
        return try {
            // When the GET goes out, and its place in the sequence: an answer that lands after a
            // write from something that went out later ([named] from the wish list's 409, another
            // read) leaves that write alone (QuoteStage.fromList).
            val readAt = Instant.now().toString()
            val mine = synchronized(LOCK) { ++seq }
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
            synchronized(LOCK) {
                val cache = QuoteStage.fromList(listed, ids, readAt, mine, written)
                if (cache.isNotEmpty()) {
                    val ed = prefs(context).edit()
                    for ((id, c) in cache) ed.putString(id, encode(c))
                    ed.apply()
                    for (id in cache.keys) written[id] = mine
                }
            }
            true
        } catch (e: Exception) {
            // Never the header: the message is the status and S5's own error text at most.
            Log.w(TAG, "quote stage not read from S5: " + (e.message?.take(160) ?: e.javaClass.simpleName))
            false
        }
    }
}
