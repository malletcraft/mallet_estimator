package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.QuoteStage.APPROVED
import com.malletcrafts.interiorestimate.pano.QuoteStage.Cache
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

/**
 * THE GUARDED QUOTE MOVE (port 61c slice QM0, 3 Nov 2026): the phone's only writer of the project's one state
 * before approval (Amit, 27 Oct 2026, 06:09 IST: "everything should be single truth"). Two S5 routes, as S5 QUOTE
 * ships them on stage (mcft-interior-estimate 9a51733, functions/_lib/asis.js):
 * - `PUT /api/asis/:project/stage {stage, base, scope?, scope_base?, via}` ([stage]): any of the six until Client
 *   approved, which is FINAL (§13.5) and the Project Manager's alone (§13.2); `base` is the quote_stage_at the
 *   phone last saw, sent VERBATIM ('' when S5 never set it), so a stage somebody else moved meanwhile comes back
 *   as S5's 409 instead of being overwritten (SPEC61c R6: reformatting it would make every move a 409);
 * - `PUT /api/asis/:project/album/scope {works, base}` ([scope]): Record the approved scope, the PM's alone, for a
 *   project approved before it had one (§13.6).
 *
 * Nothing here sends anything, and no request takes a [Tx]: [stage] and [scope] say what is sent ([AlbumQueue.Request],
 * the album outbox's one request shape, :app serialising its fields), [answer] and [scopeAnswer] read S5's answer
 * through [AlbumWire.Read] into a typed [Verdict], from the body's FIELDS and never from its English, in S5's own
 * order; [copyOf] is the copy of S5's stage the answer names, for [QuoteStage.merge]; only [text] and [localText]
 * take a [Tx], to draw the status line. A move is an online act with NO outbox: refused, offline or unanswered,
 * nothing is queued or kept to send later, and after no answer the phone re-reads the stage rather than resend
 * with the base it had ([Verdict.NoAnswer]).
 *
 * v61c.2 (Amit, 28 Oct 2026, ~10:20 IST: "Read-only until S5 has it"; approved 3 Nov 2026): a project S5 does not
 * list -- no S5 id on this phone, or read as not listed -- cannot move: [stage] and [scope] refuse it before any
 * other check and build no request, as S5 would answer 404 `This project has never been uploaded`.
 */
object QuoteMove {

    /** `via` on every write that takes one (SPEC61c §9.2), the album outbox's. */
    const val VIA = AlbumQueue.VIA

    /** S5's code for an Approve refused for want of a scope (asis.js SCOPE_MISSING). */
    const val SCOPE_MISSING = "scope_missing"

    /** The quote stages at which S5 records a scope: from Estimated on (asis.js SCOPE_STAGES). */
    val SCOPE_STAGES: List<String> = QuoteStage.STAGES.drop(QuoteStage.STAGES.size - 3)

    /**
     * Why the phone sends nothing: S5 does not list the project; approval is final; no work is ticked; and (port 61c
     * slice QV) the phone holds nothing to send with -- no S5 session and no ERP key (S5Auth.fresh is null), so the
     * act is refused before any call with the phone's sign-in words rather than met as a 401 it cannot read.
     */
    enum class Local { NOT_ON_S5, APPROVED_FINAL, SCOPE_MISSING, NOT_SIGNED_IN }

    /** What a tap builds: a request to send, a refusal the phone answers itself, or nothing to do. */
    sealed interface Move {
        /** The request; [base] the quote_stage_at it carries (the scope's base for a scope record), [scopeBase] the scope_at it carries, if any. */
        data class Send(val request: AlbumQueue.Request, val base: String, val scopeBase: String?) : Move

        /** Refused on the phone, before any call ([localText] says why); nothing is sent or kept. */
        data class Refused(val why: Local) : Move

        /**
         * Nothing to send: the stage is the one asked for, not read yet, or not one of the six -- or S5 named it
         * without its stamp ([Cache.atUnknown]), so no base can be sent until a read fills the stamp in.
         */
        data object None : Move
    }

    /** Whether [project] (S5's id; blank = only on this phone) with the store's copy [cache] is a project S5 does not list. */
    fun notOnS5(project: String, cache: Cache?): Boolean = project.isBlank() || (cache != null && !cache.listed)

    /** [ticks] as S5 keeps a scope: the sixteen works only, each once, in WORKS order. */
    fun worksOf(ticks: Collection<Int>): List<Int> = Album.WORK_IDS.filter { it in ticks }

    /**
     * The move of [project] (S5's id) from the stage in [cache] (the store's copy; null = never read) to [to], in
     * the prototype's order (index61c2.html move): a project S5 does not list is refused before anything else;
     * [to] outside the six, an unread stage (no move is offered on it) or the stage it already is builds nothing;
     * any move FROM Client approved is refused (approval is final); a stage S5 named without its stamp
     * ([Cache.atUnknown], [QuoteStage.named]) builds nothing until a read of the list or the album GET fills the
     * stamp in -- its blank `at` is not "never set", and sent as the base it would be S5's stale 409 for a stage
     * nobody moved after the phone learned it; a move TO Client approved needs at least one
     * work ticked in [scope] and carries them, de-duplicated in WORKS order, with `scope_base` [scopeAt] (the
     * album mirror's scope_at, '' when none is stored). [scope] goes with Client approved only (S5 answers 400
     * to a scope with any other stage). The PM-only rule is S5's: its 403 answers anyone else ([Verdict.PmOnly]).
     */
    fun stage(project: String, cache: Cache?, to: String, scope: Collection<Int>? = null, scopeAt: String = ""): Move {
        if (notOnS5(project, cache)) return Move.Refused(Local.NOT_ON_S5)
        val target = to.trim()
        if (QuoteStage.indexOf(target) < 0) return Move.None
        val from = cache?.stage?.trim().orEmpty()
        if (cache == null || QuoteStage.indexOf(from) < 0 || from == target) return Move.None
        if (from == APPROVED) return Move.Refused(Local.APPROVED_FINAL)
        if (cache.atUnknown) return Move.None
        val base = cache.at
        val fields = linkedMapOf<String, Any?>("stage" to target, "base" to base)
        var scopeBase: String? = null
        if (target == APPROVED) {
            val ticks = worksOf(scope.orEmpty())
            if (ticks.isEmpty()) return Move.Refused(Local.SCOPE_MISSING)
            fields["scope"] = ticks
            fields["scope_base"] = scopeAt
            scopeBase = scopeAt
        }
        fields["via"] = VIA
        return Move.Send(AlbumQueue.Request("PUT", listOf(project, "stage"), fields, AlbumQueue.JSON), base, scopeBase)
    }

    /**
     * Record the approved scope of [project]: the ticked [works] with `base` [scopeAt] (the album mirror's scope_at,
     * '' when none is stored). Refused on the phone, in the prototype's order (recordScope): a project S5 does not
     * list, then an empty list; nothing to send while the stage is not Client approved -- S5 answers whether the
     * scope may still be recorded ([Verdict.ScopeFinal]) and who may record it ([Verdict.PmScope]).
     */
    fun scope(project: String, cache: Cache?, works: Collection<Int>, scopeAt: String): Move {
        if (notOnS5(project, cache)) return Move.Refused(Local.NOT_ON_S5)
        val ticks = worksOf(works)
        if (ticks.isEmpty()) return Move.Refused(Local.SCOPE_MISSING)
        if (cache?.stage?.trim() != APPROVED) return Move.None
        val fields = linkedMapOf<String, Any?>("works" to ticks, "base" to scopeAt)
        return Move.Send(AlbumQueue.Request("PUT", listOf(project, "album", "scope"), fields, AlbumQueue.JSON), scopeAt, scopeAt)
    }

    /**
     * What S5 holds now, as an answer echoes it (asis.js stageOf, and the stored scope beside it): the stage, who
     * set it and when, and the scope with its stamp -- [scopeAt] null when the answer does not carry the scope,
     * [scope] null for "no baseline".
     */
    data class Now(val stage: String, val by: String, val at: String, val scope: List<Int>?, val scopeAt: String?)

    /** S5's verdict on one move or scope record, read from the body's fields. */
    sealed interface Verdict {
        /** 200 to a move: S5 moved the stage ([now], with the scope when the move was an Approve). */
        data class Moved(val now: Now) : Verdict

        /** 200 to a scope record: S5 stored [scope] at [scopeAt]. */
        data class Recorded(val scope: List<Int>, val scopeAt: String) : Verdict

        /** 403 to an Approve from staff who are not the PM: S5's album role for the caller and the PM's login as stored. */
        data class PmOnly(val role: String, val pm: String) : Verdict

        /** 403 to a scope record: [role] and [pm] as for [PmOnly], null when S5 sent none (a caller who is not staff). */
        data class PmScope(val role: String?, val pm: String?) : Verdict

        /** 403 to a move from a caller who is not staff. */
        data object StaffOnly : Verdict

        /** 409 to a move whose base is not S5's quote_stage_at (somebody moved it meanwhile): S5's stage as it is now. */
        data class Stale(val now: Now) : Verdict

        /** 409 to a move: the project is at Client approved, and approval is final. */
        data class Final(val now: Now) : Verdict

        /** 409: the scope_base sent is not S5's scope_at (a move's Approve, or a scope record's base). */
        data class ScopeStale(val now: Now) : Verdict

        /** 409 to an Approve with no scope, none stored; [message] S5's own words, as data. */
        data class ScopeMissing(val message: String, val now: Now) : Verdict

        /** 409 to a scope record at Client approved with a scope stored: it changes only through change requests. */
        data class ScopeFinal(val now: Now) : Verdict

        /** 409 to a scope record before Estimated; [error] S5's own words, shown as data. */
        data class NotYet(val error: String, val now: Now) : Verdict

        /** 400 or 413: refused for good, [error] S5's own words, shown as data. */
        data class Refused(val code: Int, val error: String) : Verdict

        /** 404 from S5: it has no record of the project (`This project has never been uploaded`). */
        data object NotOnS5 : Verdict

        /**
         * 401 from S5: it refused the sign-in -- none, a session S5 no longer holds, or a key ERP did not accept
         * (asis.js identify); [error] S5's own words, shown as data. The phone signs in again (menu → Server), as
         * the album outbox's RELOGIN waits for; re-reading with the same credentials would be refused the same way.
         */
        data class SignIn(val error: String) : Verdict

        /**
         * No answer of S5's handler: none came back ([code] null -- the request went out, or may have, and nothing
         * came back: a timeout, a reset, [verdictOf]), or a body that is not S5's (a proxy's page, a cut body, a 200
         * without S5's fields, a 401 without S5's `error`), a 405 or 5xx. The phone re-reads the stage, never
         * resends with the old base.
         */
        data class NoAnswer(val code: Int?) : Verdict

        /** Not sent: no route to S5 -- the host unknown, the connection refused ([verdictOf]); nothing need be read. */
        data object Offline : Verdict
    }

    private fun str(v: Any?): String? = v as? String

    /** S5's echo of the project as it is now ([Now]); null when [o] carries no `quote_stage`. */
    private fun nowOf(r: AlbumWire.Read, o: Any?): Now? {
        val stage = str(r.field(o, "quote_stage")) ?: return null
        val scopeAt = str(r.field(o, "scope_at"))
        val sc = r.field(o, "scope")
        return Now(stage, str(r.field(o, "quote_stage_by")) ?: "", str(r.field(o, "quote_stage_at")) ?: "",
            if (scopeAt == null || r.items(sc) == null) null else AlbumWire.ints(r, sc), scopeAt)
    }

    /**
     * S5's answer to the move [sent] ([stage]): [code] the HTTP status (null when no answer came back), [body] the
     * parsed body. A refusal is S5's only when its body is an object with a string `error`; anything else is
     * [Verdict.NoAnswer]. The 409s are told apart in S5's own order (asis.js, the stage PUT): the stamp first --
     * S5 answers a stale base before anything else, even at Client approved -- then `final`, then a scope_at that
     * is not the scope_base sent, then the `scope_missing` code, else stale (S5's last line is the stale words).
     */
    fun answer(r: AlbumWire.Read, code: Int?, body: Any?, sent: Move.Send): Verdict {
        if (code == null) return Verdict.NoAnswer(null)
        val o = body.takeIf { r.isObject(it) } ?: return Verdict.NoAnswer(code)
        val error = str(r.field(o, "error"))
        val now = nowOf(r, o)
        return when (code) {
            200 -> if (r.field(o, "ok") == true && now != null && now.at.isNotEmpty()) Verdict.Moved(now) else Verdict.NoAnswer(code)
            403 -> when {
                error == null -> Verdict.NoAnswer(code)
                str(r.field(o, "album_role")) != null -> Verdict.PmOnly(str(r.field(o, "album_role"))!!, str(r.field(o, "pm")) ?: "")
                else -> Verdict.StaffOnly
            }
            409 -> when {
                error == null || now == null -> Verdict.NoAnswer(code)
                now.at != sent.base -> Verdict.Stale(now)
                r.field(o, "final") == true -> Verdict.Final(now)
                sent.scopeBase != null && now.scopeAt != sent.scopeBase -> Verdict.ScopeStale(now)
                error == SCOPE_MISSING -> Verdict.ScopeMissing(str(r.field(o, "message")) ?: "", now)
                else -> Verdict.Stale(now)
            }
            400, 413 -> if (error != null) Verdict.Refused(code, error) else Verdict.NoAnswer(code)
            401 -> if (error != null) Verdict.SignIn(error) else Verdict.NoAnswer(code)
            404 -> if (error != null) Verdict.NotOnS5 else Verdict.NoAnswer(code)
            else -> Verdict.NoAnswer(code)
        }
    }

    /**
     * S5's answer to a scope record ([scope]), as [answer] reads a move's. Its 409s (asis.js, PUT …/album/scope):
     * `final` at Client approved with a scope stored; before Estimated (the stage it echoes is not one of
     * [SCOPE_STAGES]) not yet; else the scope changed meanwhile. Every 403 is the PM's rule ([Verdict.PmScope]):
     * S5 answers a caller who is not staff with the same words and no role.
     */
    fun scopeAnswer(r: AlbumWire.Read, code: Int?, body: Any?): Verdict {
        if (code == null) return Verdict.NoAnswer(null)
        val o = body.takeIf { r.isObject(it) } ?: return Verdict.NoAnswer(code)
        val error = str(r.field(o, "error"))
        return when (code) {
            200 -> {
                val at = str(r.field(o, "scope_at"))
                val sc = r.field(o, "scope")
                if (r.field(o, "ok") == true && !at.isNullOrEmpty() && r.items(sc) != null) Verdict.Recorded(AlbumWire.ints(r, sc), at) else Verdict.NoAnswer(code)
            }
            403 -> if (error == null) Verdict.NoAnswer(code) else Verdict.PmScope(str(r.field(o, "album_role")), str(r.field(o, "pm")))
            409 -> {
                val now = nowOf(r, o)
                when {
                    error == null || now == null -> Verdict.NoAnswer(code)
                    r.field(o, "final") == true -> Verdict.ScopeFinal(now)
                    now.stage !in SCOPE_STAGES -> Verdict.NotYet(error, now)
                    else -> Verdict.ScopeStale(now)
                }
            }
            400, 413 -> if (error != null) Verdict.Refused(code, error) else Verdict.NoAnswer(code)
            401 -> if (error != null) Verdict.SignIn(error) else Verdict.NoAnswer(code)
            404 -> if (error != null) Verdict.NotOnS5 else Verdict.NoAnswer(code)
            else -> Verdict.NoAnswer(code)
        }
    }

    /** The [Now] a verdict carries, if any: every S5 answer that echoes the project as it is now. */
    fun nowIn(v: Verdict): Now? = when (v) {
        is Verdict.Moved -> v.now
        is Verdict.Stale -> v.now
        is Verdict.Final -> v.now
        is Verdict.ScopeStale -> v.now
        is Verdict.ScopeMissing -> v.now
        is Verdict.ScopeFinal -> v.now
        is Verdict.NotYet -> v.now
        else -> null
    }

    /**
     * The copy of S5's stage [v] names, read at [readAt], for [QuoteStage.merge] -- a move, and every 409, says what
     * S5 holds now, so the next move carries S5's base; S5's 404 says it holds NO record of the project, so its copy
     * is not listed (v61c.2: the line, the mover and Approve read "not on S5 yet" until a read lists the project
     * again); null when the answer names no stage (a 403, a 400, a 401, no answer, offline, a scope recorded). It
     * carries no work states: [QuoteStage.merge] keeps the stored ones, and a copy not listed keeps none.
     */
    fun copyOf(v: Verdict, readAt: String): Cache? =
        if (v == Verdict.NotOnS5) Cache(listed = false, readAt = readAt)
        else nowIn(v)?.let { Cache(listed = true, stage = it.stage, by = it.by, at = it.at, readAt = readAt) }

    /**
     * The verdict on a request that THREW ([IOException]) instead of answering: [Verdict.Offline] only when the
     * request NEVER REACHED S5 -- the host unknown, the connection refused or unroutable, the TLS handshake refused
     * -- and [Verdict.NoAnswer] for everything else (a read timeout, a reset, a cut stream, a connect that timed
     * out): once the request may have gone out S5 may have moved the stage, so the phone re-reads rather than say
     * "offline" over a stage that moved (QM1 review: every IOException read as offline, and a move S5 took stood
     * on the phone as not sent until the next sync, the next tap carrying the old base into a stale 409).
     */
    fun verdictOf(e: IOException): Verdict =
        if (e is UnknownHostException || e is ConnectException || e is NoRouteToHostException || e is SSLHandshakeException) Verdict.Offline
        else Verdict.NoAnswer(null)

    // ---- the status line: S5's words in the reader's language ----------------------------------------------

    /** The words of a refusal the phone answers itself. */
    fun localText(tx: Tx, why: Local): String = when (why) {
        Local.NOT_ON_S5 -> tx.wish_not_s5_yet_project
        Local.APPROVED_FINAL -> tx.refuse_stage_approved_final
        Local.SCOPE_MISSING -> tx.refuse_stage_scope_missing
        Local.NOT_SIGNED_IN -> tx.app_sign_first_menu_server
    }

    /**
     * The status line for [v] (index61c2.html: the strip's and the scope sheet's `role=status` line); null when S5
     * did what was asked. S5's words through their keys (each the asis.js text verbatim); a stale move names who
     * moved the stage and when, in IST, else `Changed meanwhile — not saved`; a 400, a 413 and the not-yet 409 are
     * S5's own words, shown as data, and so is a 401's, after the phone's sign-in words (app.sign_again_menu_server_x).
     */
    fun text(tx: Tx, v: Verdict): String? = when (v) {
        is Verdict.Moved, is Verdict.Recorded -> null
        is Verdict.PmOnly -> tx.refuse_stage_pm_approves
        is Verdict.PmScope -> tx.refuse_stage_pm_scope
        Verdict.StaffOnly -> tx.refuse_stage_staff_only
        is Verdict.Stale -> {
            val t = QuoteStage.instantOf(v.now.at)
            if (v.now.by.isBlank() || t == null) tx.refuse_stage_changed else tx.refuse_stage_changed_x(by = v.now.by, hm = QuoteStage.hm(t))
        }
        is Verdict.Final -> tx.refuse_stage_approved_final
        is Verdict.ScopeStale -> tx.refuse_stage_scope_changed
        is Verdict.ScopeMissing -> tx.refuse_stage_scope_missing
        is Verdict.ScopeFinal -> tx.refuse_stage_scope_final
        is Verdict.NotYet -> v.error
        is Verdict.Refused -> v.error
        Verdict.NotOnS5 -> tx.wish_not_s5_yet_project
        is Verdict.SignIn -> tx.app_sign_again_menu_server_x(why = v.error)
        is Verdict.NoAnswer -> tx.refuse_stage_no_answer
        Verdict.Offline -> tx.refuse_stage_offline
    }
}
