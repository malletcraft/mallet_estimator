package com.malletcrafts.interiorestimate

import android.content.Context
import android.util.Log
import com.malletcrafts.interiorestimate.pano.Album
import com.malletcrafts.interiorestimate.pano.QuoteMove
import com.malletcrafts.interiorestimate.pano.QuoteStage
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import org.json.JSONObject
import java.io.IOException
import java.time.Instant

/**
 * THE ONE PROJECT-STATE STORE (v61c; Amit, 27 Oct 2026: "everything should be single truth"), held
 * in preferences per S5 project id: S5's quote stage, who set it and when, and since port 61c slice
 * PL each work's state at the seq S5 knows it (QuoteStage.Cache, kept in QuoteStage.stored's shape).
 * Every reader draws the ONE derived line from it ([lineFor], QuoteStage.projectLine): the project
 * screen, the project list row and the Wish list's top bar. Nothing on the phone sets it by hand;
 * S5's answers go in through [write], by S5's own order (QuoteStage.writes / merge) -- all but ONE,
 * by design: [named], an answer that names the stage WITHOUT its stamp (the wish list's 409s), writes
 * its own copy. Through writes it would lose: against a stored copy S5 stamped, QuoteStage.takes
 * refuses a listed copy with no stamp whatever the sequence, so the `final` 409's Client approved
 * would give way to the older stamped Revisions and the Wish list would unlock under S5's "not
 * saved" (the slice W review fix). Do not route it through [write] (QuoteStageTest pins why).
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
 *
 * TWO WRITES, ONLINE AND UNQUEUED: [recordScope] (port 61c slice QV), Record the approved scope for a project
 * approved before it had one (§13.6) -- `PUT …/album/scope` built by QuoteMove.scope, answered through
 * QuoteMove.scopeAnswer -- and [move] (port 61c slice QM1), the phase strip's guarded quote move -- `PUT …/stage`
 * built by QuoteMove.stage with the stored copy's quote_stage_at as its base, answered through QuoteMove.answer.
 * Both send ONCE through [exchange] and put S5's word where it belongs through [take]: on the stage into this store
 * ([write]), and on the scope -- and on who records or approves, a 403's role and PM -- into the album mirror
 * (AlbumSync.take). Refused, offline or unanswered, nothing is kept to send later; after no answer the phone
 * re-reads the stage rather than resend with the base it had.
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
     * THE ONE WRITER of every S5 answer but one -- [named]'s, which names a stage with no stamp and so
     * writes its own copy (the class note says why it cannot come through here): S5's [copies] by
     * project id -- a read of the list ([refresh]), and from QV on
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

    // ---- Record the approved scope (port 61c slice QV; SPEC61c §4.2 "Record", §13.6) --------------------------

    /** The outcome of [recordScope]: refused on the phone (nothing sent), nothing to send, or S5's verdict on the PUT. */
    sealed interface ScopeOutcome {
        /** Refused before any call (QuoteMove.localText says why): S5 does not list the project, nothing ticked, not signed in. */
        data class Refused(val why: QuoteMove.Local) : ScopeOutcome

        /** Nothing to send: the store's copy is not at Client approved (the sheet closes, as the prototype's does). */
        data object None : ScopeOutcome

        /** S5 answered, or could not be reached (QuoteMove.Verdict.Offline). */
        data class Answered(val verdict: QuoteMove.Verdict) : ScopeOutcome
    }

    /** The status line for [o] in [tx]: null when the scope was recorded, or there was nothing to send. */
    fun scopeText(tx: Tx, o: ScopeOutcome): String? = when (o) {
        is ScopeOutcome.Refused -> QuoteMove.localText(tx, o.why)
        ScopeOutcome.None -> null
        is ScopeOutcome.Answered -> QuoteMove.text(tx, o.verdict)
    }

    /** Whether [o] is the scope recorded: the sheet closes and the pending refusal, if any, goes. */
    fun scopeRecorded(o: ScopeOutcome): Boolean = o is ScopeOutcome.Answered && o.verdict is QuoteMove.Verdict.Recorded

    /** Whether [o] is S5's stale 409: the sheet redraws its ticks from the scope as stored (the prototype's `st.ticks=scopeOf(p)`). */
    fun scopeStale(o: ScopeOutcome): Boolean = o is ScopeOutcome.Answered && o.verdict is QuoteMove.Verdict.ScopeStale

    /**
     * Record the approved scope of [project] (the S5 id the stage is read under): the ticked [works], sent ONCE
     * as `PUT …/album/scope {works, base}` with the mirror's scope_at as the base (QuoteMove.scope), with the same
     * credentials every S5 call sends (S5Auth.fresh). Blocking; call it off the main thread. Never throws.
     * - Refused on the phone, nothing sent: a project S5 does not list, an empty list (QuoteMove.scope), or no
     *   credentials at all ([QuoteMove.Local.NOT_SIGNED_IN]); nothing to send while the copy is not at Client
     *   approved ([ScopeOutcome.None]).
     * - S5's 401 (a session it no longer holds) renews the sign-in once (S5Session.relogin) and sends once more;
     *   a second refusal stands as S5's verdict ([QuoteMove.Verdict.SignIn]).
     * - Every answer that names the stage goes into this store ([write]: the 409s echo S5's stage), and every one
     *   that carries the scope as stored goes into the mirror (the 200's new baseline; a stale, final or not-yet
     *   409's "it now shows what is stored"), so the sheet and the APPROVED SCOPE row redraw from S5's word. So
     *   does the 403 that names who records it (`album_role` and `pm`: a PM named on /projects meanwhile): the
     *   mirror takes its role and PM, and the row offers Record no more (QV review: the first version read them
     *   into the verdict and dropped them, so every tap was the same 403 until the next album GET).
     * - No answer (an IOException, a body that is not S5's) is [QuoteMove.Verdict.Offline] / NoAnswer: the phone
     *   re-reads on its next sync and never resends with the base it had.
     */
    fun recordScope(context: Context, project: String, works: Collection<Int>): ScopeOutcome {
        val cache = cached(context, project)
        val scopeAt = AlbumSync.mirror(context, project)?.scopeAt.orEmpty()
        val send = when (val move = QuoteMove.scope(project, cache, works, scopeAt)) {
            is QuoteMove.Move.Refused -> return ScopeOutcome.Refused(move.why)
            QuoteMove.Move.None -> return ScopeOutcome.None
            is QuoteMove.Move.Send -> move
        }
        val auth = S5Auth.fresh(context) ?: return ScopeOutcome.Refused(QuoteMove.Local.NOT_SIGNED_IN)
        val verdict = try {
            val a = exchange(context, auth, send)
            QuoteMove.scopeAnswer(JsonRead, a.code, bodyOf(a))
        } catch (e: IOException) {
            // no signal, DNS, TLS, a cut connection: not sent, or sent and never answered -- the phone re-reads
            QuoteMove.Verdict.Offline
        } catch (e: Exception) {
            Log.w(TAG, "$project: scope record not answered: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
            QuoteMove.Verdict.NoAnswer(null)
        }
        take(context, project, verdict)
        return ScopeOutcome.Answered(verdict)
    }

    // ---- the guarded quote move (port 61c slice QM1; SPEC61c §4.1 item 3, decision 2 of 27 Oct; index61c2.html move) ----

    /** The outcome of [move]: refused on the phone (nothing sent), nothing to send, or S5's verdict on the PUT. */
    sealed interface MoveOutcome {
        /** Refused before any call (QuoteMove.localText says why): S5 does not list the project, approval is final, not signed in. */
        data class Refused(val why: QuoteMove.Local) : MoveOutcome

        /** Nothing to send: the stage asked for is the one stored, or the stored copy offers no move (unread, or named without its stamp). */
        data object None : MoveOutcome

        /** S5 answered, or could not be reached (QuoteMove.Verdict.Offline). */
        data class Answered(val verdict: QuoteMove.Verdict) : MoveOutcome
    }

    /** The strip's refusal line for [o] in [tx] (QuoteMove.text / localText): null when S5 moved the stage, or there was nothing to send. */
    fun moveText(tx: Tx, o: MoveOutcome): String? = when (o) {
        is MoveOutcome.Refused -> QuoteMove.localText(tx, o.why)
        MoveOutcome.None -> null
        is MoveOutcome.Answered -> QuoteMove.text(tx, o.verdict)
    }

    /** Whether [o] is the stage moved: the pending refusal, if any, goes (the prototype's `msg=null`). */
    fun moved(o: MoveOutcome): Boolean = o is MoveOutcome.Answered && o.verdict is QuoteMove.Verdict.Moved

    /**
     * Move the quote stage of [project] (the S5 id the stage is read under) to [to], one of the six, as a tap on the
     * phase strip asks (PhaseStrip): ONE online `PUT …/stage {stage, base, via}` built by QuoteMove.stage from the
     * stored copy -- `base` its quote_stage_at VERBATIM, so a stage somebody else moved meanwhile comes back as S5's
     * stale 409 instead of being overwritten -- with the same credentials every S5 call sends (S5Auth.fresh).
     * Blocking; call it off the main thread. Never throws.
     * - Refused on the phone, nothing sent: a project S5 does not list (v61c.2), any move from Client approved
     *   (approval is final), or no credentials at all ([QuoteMove.Local.NOT_SIGNED_IN]); nothing to send on an unread
     *   stage, the stage it already is, or a copy named without its stamp ([MoveOutcome.None]).
     * - S5's 401 renews the sign-in once and sends once more ([exchange]).
     * - Every answer that names the stage goes into this store ([take]: the 200, and every 409 echoes S5's stage as
     *   it is now, a 404 as not listed), so the line and the strip redraw from S5's word and the next tap carries
     *   S5's base; an answer that carries the scope as stored, or names who approves (QM2's Approve), goes into the
     *   mirror.
     * - No answer of S5's handler (a 5xx, a body that is not S5's): the phone RE-READS the list at once
     *   ([refresh], the status line's "showing the stage as it is now") and never resends with the base it had;
     *   offline, nothing is read or kept.
     * Nothing is queued: the move is an online act, as it is on /projects.
     */
    fun move(context: Context, project: String, to: String): MoveOutcome {
        val cache = cached(context, project)
        val send = when (val m = QuoteMove.stage(project, cache, to)) {
            is QuoteMove.Move.Refused -> return MoveOutcome.Refused(m.why)
            QuoteMove.Move.None -> return MoveOutcome.None
            is QuoteMove.Move.Send -> m
        }
        val auth = S5Auth.fresh(context) ?: return MoveOutcome.Refused(QuoteMove.Local.NOT_SIGNED_IN)
        val verdict = try {
            val a = exchange(context, auth, send)
            QuoteMove.answer(JsonRead, a.code, bodyOf(a), send)
        } catch (e: IOException) {
            QuoteMove.Verdict.Offline
        } catch (e: Exception) {
            Log.w(TAG, "$project: quote move not answered: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
            QuoteMove.Verdict.NoAnswer(null)
        }
        take(context, project, verdict)
        if (verdict is QuoteMove.Verdict.NoAnswer) refresh(context, listOf(project))
        return MoveOutcome.Answered(verdict)
    }

    // ---- shared by the two writes ----------------------------------------------------------------------------

    /**
     * ONE request of [send] (QuoteMove's `PUT …/stage` or `PUT …/album/scope`) with [auth]: S5's answer whatever its
     * status. S5's 401 (a session it no longer holds) renews the sign-in once (S5Session.relogin) and sends once
     * more; a second refusal stands as S5's verdict. A network failure throws (IOException).
     */
    private fun exchange(context: Context, auth: String, send: QuoteMove.Move.Send): AsIsUpload.Answer {
        val url = BuildConfig.WEB_URL.trimEnd('/') + send.request.path { AsIsUpload.seg(it) }
        // JSONObject(fields): S5's JSON types as QuoteMove built them (a list of numbers, a string base)
        val body = JSONObject(send.request.fields as Map<*, *>).toString().toByteArray(Charsets.UTF_8)
        val type = send.request.contentType + "; charset=utf-8" // text:value a wire value, the body's content type
        var a = AsIsUpload.exchange("PUT", url, auth, body, type)
        if (a.code == 401 && S5Session.signedIn(context) && S5Session.relogin(context)) {
            a = AsIsUpload.exchange("PUT", url, S5Auth.current(context) ?: auth, body, type)
        }
        return a
    }

    /** S5's body as a JSON tree for QuoteMove's readers, or null when it is not JSON (a proxy's page: no answer). */
    private fun bodyOf(a: AsIsUpload.Answer): Any? = runCatching { JSONObject(a.body) }.getOrNull()

    /**
     * S5's word in [verdict] goes where it belongs: on the stage into this store ([write]: a move's 200 and every 409
     * echo S5's stage as it is now, a 404 says it holds no record of the project); on the scope into the mirror
     * (AlbumSync.take, at its own place after every read in flight: a record's 200, the baseline an Approve's 200
     * carries, and every 409 that echoes the scope as stored); and on who records or approves -- the 403's
     * `album_role` and `pm`, a PM named on /projects meanwhile -- into the mirror too, so the row and the strip redraw
     * who may (QV review: the first version read them into the verdict and dropped them). An answer that names none
     * of these (a 400, a 401, no answer, offline) changes nothing.
     */
    private fun take(context: Context, project: String, verdict: QuoteMove.Verdict) {
        QuoteMove.copyOf(verdict, Instant.now().toString())?.let { write(context, mapOf(project to it)) }
        when (verdict) {
            is QuoteMove.Verdict.Recorded -> AlbumSync.take(context, project, Album.Answer(scope = verdict.scope, scopeAt = verdict.scopeAt))
            is QuoteMove.Verdict.PmScope -> verdict.role?.let { AlbumSync.take(context, project, Album.Answer(role = it, pm = verdict.pm)) }
            is QuoteMove.Verdict.PmOnly -> AlbumSync.take(context, project, Album.Answer(role = verdict.role, pm = verdict.pm))
            else -> QuoteMove.nowIn(verdict)?.let { now -> now.scopeAt?.let { AlbumSync.take(context, project, Album.Answer(scope = now.scope, scopeAt = it)) } }
        }
    }
}
