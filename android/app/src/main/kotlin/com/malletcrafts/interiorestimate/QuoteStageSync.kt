package com.malletcrafts.interiorestimate

import android.content.Context
import android.util.Log
import com.malletcrafts.interiorestimate.pano.QuoteStage
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import org.json.JSONObject
import java.time.Instant

/**
 * THE ONE PROJECT-STATE STORE (v61c; Amit, 27 Oct 2026: "everything should be single truth"), held
 * in preferences per S5 project id: S5's quote stage, who set it and when, and since port 61c slice
 * PL each work's state at the seq S5 knows it (QuoteStage.Cache, kept in QuoteStage.stored's shape).
 * Every reader draws the ONE derived line from it ([lineFor], QuoteStage.projectLine): the project
 * screen, the project list row and the Wish list's top bar. Nothing on the phone sets it by hand;
 * S5's answers go in through [write], by S5's own order (QuoteStage.writes / merge).
 *
 * ONE GET <WEB_URL>/api/asis per read, with the same credentials ReadingsSync sends
 * (S5Auth.fresh: the S5 session when signed in, renewed near its end, else the legacy ERP key -- 22 Oct 2026). The LIST, not GET /api/asis/:project: that call also settles the project and lists
 * its photos, which is not work to cause once per project per sync. Each row carries S5-L's
 * `work_states` (`{"<id>": {state, seq}}`, the state row's own seq), so the list row and the
 * project line read the Execution counts of every project from this one call -- no album GET per
 * project. The answer is cached per ERP project id (Project.serverId) with the time this phone's
 * read went out, and a known project the list does not carry is cached as not listed -- each id
 * only where its copy is not newer by S5's quote_stage_at, nor, without two stamps to compare, from
 * something that went out LATER (QuoteStage.takes, ordered by [seq]), since reads overlap: the
 * project-open read, a sync's, the wish list's own after S5 named the stage ([named]). Each work's
 * state goes by the higher seq whichever read brought it (QuoteStage.mergeStates).
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
    // [write]'s ordered one, so a slow read of the list can never write over a stage S5 named,
    // or another read took, after that read went out (QuoteStage.writes).
    private val LOCK = Any()

    // The one sequence a read takes as its GET goes out and a [named] write takes as it writes,
    // and per ERP id the seq of the write that put its stored copy there. In memory: a read in
    // flight never outlives the process, and a fresh process has no read to order against.
    private var seq = 0L
    private val written = HashMap<String, Long>()

    // The copy in QuoteStage.stored's shape, read back by QuoteStage.storedOf (both pinned in :pano): the six fields
    // every build since 0.3.262 wrote, and the work states once read. atUnknown with the stage it qualifies: a copy
    // QuoteStage.named wrote has its who and when UNKNOWN, not "never set", and no move is built on it until a read
    // fills them in (QuoteMove.stage) -- so the mark has to outlive the process, or a restart would read the blank
    // stamp as never set. A copy an older build wrote has no `states`: read back as not read, never "no work started".
    private fun encode(c: QuoteStage.Cache): String = JSONObject(QuoteStage.stored(c) as Map<*, *>).toString()

    private fun decode(raw: String?): QuoteStage.Cache? {
        if (raw == null) return null
        return try {
            QuoteStage.storedOf(JsonRead, JSONObject(raw))
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
     * THE ONE DERIVED LINE (QuoteStage.projectLine) for the S5 id [stageKey] the stage is read under
     * (WishSync.stageKey: the project's own, or the id its moved list now has), in [tx]: the project
     * screen's line, the project list row and the Wish list's top bar all draw this. Null or blank:
     * the project is only on this phone, so S5 is not consulted and it reads "not on S5 yet". This
     * phone's queued state writes lay over the states here once the album outbox exists (K4); until
     * then there are none.
     */
    fun lineFor(context: Context, stageKey: String?, tx: Tx): QuoteStage.ProjectLine {
        val local = stageKey.isNullOrBlank()
        return QuoteStage.projectLine(if (local) null else cached(context, stageKey!!), local, emptyList(), tx, Instant.now())
    }

    /**
     * The place in the one sequence of something that reads S5, taken as its request goes OUT
     * (QuoteStage.takes): a read of the list takes one, and so will the album GET (QV).
     */
    fun ticket(): Long = synchronized(LOCK) { ++seq }

    /**
     * THE ONE WRITER: S5's [copies] by project id -- a read of the list ([refresh]), and from QV on
     * the album GET (QuoteStage.fromAlbum) and the answers to a move (QuoteMove.copyOf) -- go into
     * the store through QuoteStage.writes, each id merged by S5's own order onto the copy stored now.
     * [sent] is the [ticket] the request took as it went out; null for an answer that is S5's word as
     * it lands (a move's), which takes its place in the sequence now, as [named] does. Returns what
     * was written; never throws.
     */
    fun write(context: Context, copies: Map<String, QuoteStage.Cache>, sent: Long? = null): Map<String, QuoteStage.Cache> =
        try {
            synchronized(LOCK) {
                val at = sent ?: ++seq
                // The copies as stored now: S5's quote_stage_at orders these against them before
                // the sequence does, since a GET that went out first can reach S5 later (QuoteStage.takes).
                val stored = HashMap<String, QuoteStage.Cache>()
                for (id in copies.keys) if (id.isNotBlank()) cached(context, id)?.let { stored[id] = it }
                val out = QuoteStage.writes(copies, at, written, stored)
                if (out.isNotEmpty()) {
                    val ed = prefs(context).edit()
                    for ((id, c) in out) ed.putString(id, encode(c))
                    ed.apply()
                    // Never lowered: a copy taken by its newer stamp from a read that went out earlier
                    // is still at least as new as the later read it replaced.
                    for (id in out.keys) written[id] = maxOf(written[id] ?: Long.MIN_VALUE, at)
                }
                out
            }
        } catch (e: Exception) {
            Log.w(TAG, "quote stage from S5 not kept: " + // text:value log line
                (e.message?.take(160) ?: e.javaClass.simpleName))
            emptyMap()
        }

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
            val mine = ticket()
            val body = JSONObject(AsIsUpload.send("GET", url(), auth, null, null))
            // A 200 without the list is not "nothing listed": it would mark every project not sent.
            val arr = body.getJSONArray("projects")
            // Each row read by :pano's own reader (QuoteStage.listedOf): the stage, who and when, S5's null
            // as '', and S5-L's work_states -- absent (an S5 before S5-L) as not read, never "none started".
            val listed = (0 until arr.length()).mapNotNull { i -> QuoteStage.listedOf(JsonRead, arr.opt(i)) }
            write(context, QuoteStage.fromList(listed, ids, readAt), mine)
            true
        } catch (e: Exception) {
            // Never the header: the message is the status and S5's own error text at most.
            Log.w(TAG, "quote stage not read from S5: " + (e.message?.take(160) ?: e.javaClass.simpleName))
            false
        }
    }
}
