package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Album.WorkState
import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * THE QUOTE STAGE ON THE PROJECT SCREEN (v52 item 16; Amit, 14 Oct 2026, 06:53 IST,
 * the stages: "survey, client wish list, vendor quoted, client approved, estimated").
 * SIX SINCE v61b (Amit approved the prototype on 26 Oct 2026): Estimated and Client approved
 * swapped and Revisions put between them -- the estimate is made, the designer, client and
 * vendors revise it, the client approves, and only then does execution begin. S5 QUOTE's
 * list changed the same day (functions/_lib/asis.js); the phone shows what S5 answers.
 *
 * The COMMERCIAL state of the job, held by S5 QUOTE (one per project). Since v61c (Amit, 27 Oct
 * 2026, 06:09 IST: "everything should be single truth") it is the project's ONE state: the quote
 * stage until Client approved, then the Album's sixteen works. This is the pure half of the ONE
 * PROJECT-STATE STORE (port 61c slice QM0): what the phone keeps per project ([Cache]: S5's stage,
 * who set it and when, and each work's state at the seq S5 knows it), how every S5 answer that
 * names them goes in ([merge], S5's own order: quote_stage_at for the stage, each work's seq for
 * its state), and the ONE derived line every reader draws ([projectLine]): "Quote · <stage>" before
 * approval, "Execution · …" after, never set by hand. The phone MOVES the stage only through
 * [QuoteMove]'s guarded `PUT …/stage`, which S5 answers; nothing here sends anything.
 *
 * v61c.2 (Amit, 28 Oct 2026, ~10:20 IST, "Read-only until S5 has it"; approved 3 Nov 2026): a
 * project S5 does not list -- made on this phone and never sent ([projectLine]'s `phoneOnly`), or
 * read as not listed -- has NO stage of its own: its line reads "not on S5 yet — this project has
 * not been sent", [QuoteMove] refuses to move it before any call, and nothing is held to send later.
 *
 * The wording of the second line is decision 1 of 15 Oct: 200 with no time -> "<stage> · not set
 * on S5 yet"; 200 with a time -> "since d MMM, HH:mm IST on S5" (en-GB short months, so September
 * is "Sept" as the prototype prints it); the read time on the second line only when the cached copy
 * is older than today (IST). Every text is a tx accessor ([projectLine], [coverageParts]); the six
 * stages, the months and the worksite stage are identity values, marked `// text:value`. Port 61c
 * slice PL deleted the QUOTE STAGE line before v61c (lineFor, Line, phoneOnly, TITLE, the NOT_*
 * texts and since), whose readers -- the project screen, the Wish list's top bar -- draw
 * [projectLine] now, and gave the store its stored shape ([stored], [storedOf]) and its one
 * writer's rule ([writes]).
 */
object QuoteStage {

    /** S5's QUOTE_STAGES (functions/_lib/asis.js), in order. QuoteStageTest holds the two equal. */
    val STAGES: List<String> = listOf("Survey", "Client wish list", "Vendor quoted", "Estimated", "Revisions", "Client approved") // text:value S5's six quote stages, identity values shown through Masters.quote

    /** The last stage: the client has approved the estimate and execution begins (v61b). */
    const val APPROVED = "Client approved" // text:value S5's last quote stage, an identity value shown through Masters.quote

    val IST: ZoneId = ZoneId.of("Asia/Kolkata") // text:value a zone id, never shown

    // Fixed, not the locale's, so the line does not change with the handset's locale data --
    // but spelled as the prototype shows it: its en-GB `month:'short'` gives "Sept" for
    // September in Chrome and WebView, and the prototype is the spec character for character.
    // In both languages: the prototype prints these months under मराठी too ("S5 वर 14 Oct, 08:08 IST पासून").
    private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec") // text:value the prototype's en-GB short months, in both languages

    /**
     * What the phone keeps for one project after reading S5's list. [listed] false means the
     * caller's list did not carry the project (never uploaded, or not visible to this person);
     * [at] is S5's quote_stage_at ("" when never set -- unless [atUnknown]), [readAt] when THIS
     * phone's read of it went out (UTC ISO).
     */
    data class Cache(
        val listed: Boolean,
        val stage: String = "",
        val by: String = "",
        val at: String = "",
        val readAt: String = "",
        /**
         * Each work's state as S5 knows it at a seq (S5-L's `work_states`, the album GET's `works[]`,
         * [Album.WorkState]); a work absent has no state row (Not started). NULL means not read in this
         * shape -- a copy an older build wrote, or an S5 before S5-L -- which the line never reads as
         * "no work started" ([projectLine]). Always null for a project S5 does not list ([merge]).
         */
        val states: Map<Int, WorkState>? = null,
        /**
         * True when an answer NAMED the stage without who set it or when ([named]): [by] and [at] are then
         * UNKNOWN, not "never set", until a read of S5's list or the album GET fills them in ([merge]). No move
         * is built on such a copy ([QuoteMove.stage]): sent as base "", it would be S5's stale 409 for a stage
         * nobody moved after the phone learned it (port 61c slice QM0 review).
         */
        val atUnknown: Boolean = false,
    )

    /** One project as S5's GET /api/asis lists it ([listedOf]); [states] its `work_states`, null when the row has none. */
    data class Listed(val project: String, val stage: String, val by: String, val at: String, val states: Map<Int, WorkState>? = null)

    /** The 0-based place of [stage] in [STAGES], or -1 when it is not one of them. */
    fun indexOf(stage: String): Int = STAGES.indexOf(stage)

    /** The time [iso] names (UTC "Z" or an offset), or null when it is blank or not a time. */
    fun instantOf(iso: String): Instant? {
        if (iso.isBlank()) return null
        return try { OffsetDateTime.parse(iso.trim()).toInstant() } catch (e: Exception) { null }
    }

    /** "14 Oct, 08:08" in IST. */
    fun dayTime(t: Instant): String {
        val z = t.atZone(IST)
        return "${z.dayOfMonth} ${MONTHS[z.monthValue - 1]}, ${hhmm(z)}" // text:value a day and a time, as the prototype's en-GB date prints them in both languages
    }

    private fun hhmm(z: ZonedDateTime): String =
        z.hour.toString().padStart(2, '0') + ":" + z.minute.toString().padStart(2, '0')

    /** "08:08": [t]'s hour and minute in IST. */
    fun hm(t: Instant): String = hhmm(t.atZone(IST))

    /** "14 Oct": [t]'s day and month in IST, months as [dayTime] spells them. */
    fun day(t: Instant): String = t.atZone(IST).let { z -> "${z.dayOfMonth} ${MONTHS[z.monthValue - 1]}" } // text:value a day, as the prototype's en-GB date prints it in both languages

    /** True when [readAt] falls on a day before [now]'s day, both in IST (decision 1). */
    fun readIsOld(readAt: String, now: Instant): Boolean {
        val r = instantOf(readAt) ?: return false
        return r.atZone(IST).toLocalDate().isBefore(now.atZone(IST).toLocalDate())
    }

    /**
     * The cache after one read of S5's list at [readAt]: every listed project as listed, and
     * every [known] project the list did not carry as not listed. Blank ids are ignored.
     */
    fun fromList(listed: List<Listed>, known: Collection<String>, readAt: String): Map<String, Cache> {
        val out = linkedMapOf<String, Cache>()
        for (k in known) if (k.isNotBlank()) out[k] = Cache(listed = false, readAt = readAt)
        for (p in listed) if (p.project.isNotBlank())
            out[p.project] = Cache(listed = true, stage = p.stage, by = p.by, at = p.at, readAt = readAt,
                states = p.states?.filterKeys { Album.isWork(it) })
        return out
    }

    /**
     * The writes of one read of S5's list, ORDERED BY WHEN EACH WENT OUT: [seq] is this read's place
     * in the one sequence every read takes as its GET goes out and every [named] write takes as it
     * writes, and [written] is, per id, the seq of the write that put the stored copy there (an id
     * absent from it has not been written since the process started). An id takes this read only
     * when its stored copy came from something EARLIER in that sequence; one written by a later
     * read or by a later answer that named the stage keeps what is stored, since that is newer
     * than this GET. So of two reads out at once, the one that went out later wins whichever lands
     * first -- and a project-open read that went out before the Approve cannot land after the
     * wish list's `final` 409 and put Revisions back, unlocking the list under S5's "…; not saved"
     * (slice W fix review).
     *
     * A sequence and not equality: the first version compared the copy stored now with the copy
     * stored when the GET went out, which kept the OLDER of two overlapping reads whenever they
     * landed in the order they went out -- the usual case -- because the first to land changed the
     * copy the second had seen (batch B review, 2 Nov 2026). And a sequence and not the clock: a
     * phone whose clock steps back would hold every later read below a stamp in the future. The
     * sequence lives in memory only, which is enough: a read in flight never outlives its process.
     * [readAt] is when the GET went OUT, so a slow answer is not dated as fresh.
     *
     * S5'S OWN STAMP FIRST, the sequence second ([takes]): [stored] is every copy as stored now, by
     * id. When went-out is not served-in -- a GET that waits on its connection or is retried reaches
     * S5 after one sent later -- the earlier read can carry the newer stage, and only S5's
     * quote_stage_at says so (batch B review of 7d13709).
     *
     * Each id goes through [merge] (port 61c slice QM0), so a read whose stage loses to the stored
     * copy still brings the work states S5 knows at a higher seq; an id is in the answer only when
     * its merged copy differs from the one stored.
     */
    fun fromList(
        listed: List<Listed>, known: Collection<String>, readAt: String,
        seq: Long, written: Map<String, Long>, stored: Map<String, Cache>,
    ): Map<String, Cache> = writes(fromList(listed, known, readAt), seq, written, stored)

    /**
     * THE ONE WRITER'S RULE (port 61c slice PL): what the store writes when [copies] -- S5's answers by project id
     * (a read of the list, and from QV on the album GET and the answers to a move) -- land at [seq] on the copies
     * [stored] now, each put there by the write at [written] (QuoteStageSync.write holds the sequence). Each id goes
     * through [merge], and is in the answer only when its merged copy differs from the one stored. Blank ids are
     * ignored.
     */
    fun writes(copies: Map<String, Cache>, seq: Long, written: Map<String, Long>, stored: Map<String, Cache>): Map<String, Cache> {
        val out = linkedMapOf<String, Cache>()
        for ((id, c) in copies) {
            if (id.isBlank()) continue
            val m = merge(stored[id], c, seq, written[id])
            if (m != stored[id]) out[id] = m
        }
        return out
    }

    /**
     * Whether a read's copy [read], from the GET at [seq], replaces [stored], put there by the write
     * at [wrote] (null: not written since the process started). Where both carry an S5 quote_stage_at
     * and the two differ, the later stamp wins whichever GET went out first: S5 stamps every move
     * with its own clock, so the stamp orders what S5 SERVED, which the sequence only guesses at.
     * Otherwise -- a copy named from a 409 or read as not listed has no stamp, and two equal stamps
     * are one state of S5's -- the read takes the id only when the stored copy came from something
     * earlier in the sequence, so an older read in flight still cannot beat a stage S5 named.
     * One exception, as WishList.afterGet has it for wishlist_at: a read that LISTS the project with
     * no stamp never replaces a stored copy that has one, whatever the sequence, because S5 has no
     * way back to a stage never set -- such a read was served before the project's first move,
     * however late it went out or landed.
     */
    fun takes(read: Cache, stored: Cache?, seq: Long, wrote: Long?): Boolean {
        val r = instantOf(read.at)
        val s = stored?.let { instantOf(it.at) }
        if (r != null && s != null && r != s) return r.isAfter(s)
        if (read.listed && r == null && s != null) return false
        return (wrote ?: Long.MIN_VALUE) < seq
    }

    /**
     * The cache after an answer of S5's that NAMED the project's stage without who set it or when:
     * the wish list PUT's refusals for the stage -- the `final` 409 at Client approved and the 409
     * at Survey -- carry `quote_stage` and nothing more (wishView, asis.js). The phone keeps ONE
     * copy of the stage, so the answer goes into it, and the Wish list, the project line and every
     * other reader agree with what S5 has just said (port 61c slice W review: an Approve landing
     * between the sync's read of the list and its PUT left the Wish list editable under S5's
     * "not saved", and each edit there was thrown away by the next sync). Null when nothing
     * changes: the cache already says [stage], or [stage] is not one of the six. Otherwise the
     * project is listed at [stage], read at [readAt], its `by` and `at` blank and marked UNKNOWN
     * ([Cache.atUnknown], never "never set") until the next read of S5's list fills them in.
     */
    fun named(cache: Cache?, stage: String, readAt: String): Cache? {
        val s = stage.trim()
        if (indexOf(s) < 0) return null
        if (cache != null && cache.listed && cache.stage.trim() == s) return null
        return Cache(listed = true, stage = s, readAt = readAt, states = cache?.states, atUnknown = true)
    }


    // ---- THE ONE PROJECT-STATE STORE: S5's answers go in by S5's own order (port 61c slice QM0) ----------

    /**
     * The copy kept after [incoming] -- a read of S5's list ([fromList]), an album GET ([fromAlbum]) or an
     * answer to a move ([QuoteMove.copyOf]) -- lands on [stored], from the [seq] / [wrote] sequence [takes]
     * reads. Two parts, each by S5's own order:
     * - THE STAGE (listed, stage, by, at, readAt) is [incoming]'s when [takes] says so -- S5's quote_stage_at
     *   first, the sequence second, as batch B decided -- and [stored]'s otherwise, so a read that went out
     *   before a move and answered after it never rolls the stage back;
     * - EACH WORK'S STATE by the seq it is known at ([mergeStates]): the higher wins. S5-L's `work_states`
     *   gives the STATE ROW's own seq; the album GET's `works[].seq` is the work's BASE, its newest seq of any
     *   kind (asis.js albumView, `seq: baseOf[id]`), which is never below its state row's -- so a state known
     *   as of a higher seq is never the older one, whichever source it came from (critic S1).
     * A copy that is NOT LISTED holds nothing but when it was read (v61c.2: a project S5 does not list has no
     * stage of its own): no stage, no who or when, no states -- never one kept from before, whatever wrote it.
     * [Cache.atUnknown] goes with the stage: a read that takes the stage fills who and when in.
     */
    fun merge(stored: Cache?, incoming: Cache, seq: Long, wrote: Long?): Cache {
        val head = if (stored == null || takes(incoming, stored, seq, wrote)) incoming else stored
        if (!head.listed) return Cache(listed = false, readAt = head.readAt)
        return head.copy(states = mergeStates(stored?.states, incoming.states))
    }

    /**
     * Two states maps as one, work by work, the higher seq winning; at an equal seq (one row of S5's) the one
     * that carries the row's time. A work absent from one map takes the other's: S5's log is append-only, so a
     * work missing from a read is a read that did not carry it, never a state taken away. Null (not read in
     * this shape) on both sides stays null; only the sixteen works are kept.
     */
    fun mergeStates(a: Map<Int, WorkState>?, b: Map<Int, WorkState>?): Map<Int, WorkState>? {
        if (a == null && b == null) return null
        val out = LinkedHashMap<Int, WorkState>()
        for (w in Album.WORK_IDS) {
            val x = a?.get(w); val y = b?.get(w)
            val pick = when {
                x == null -> y
                y == null -> x
                y.seq > x.seq -> y
                y.seq < x.seq -> x
                x.at.isBlank() && y.at.isNotBlank() -> y
                else -> x
            }
            if (pick != null) out[w] = pick
        }
        return out
    }

    /**
     * THE COPY AS THE STORE KEEPS IT (port 61c slice PL), with no JSON in it: :app writes `JSONObject(stored(c))` into the
     * `quote_stage` preferences, one entry per S5 project id. The six fields every build since 0.3.262 wrote, by the same
     * names, and -- only when [Cache.states] has been read -- `states`, each work by its id as text:
     * `{"<id>": {state, seq, at}}`, a seq a number. A copy whose states were never read has no `states` key at all, so
     * [storedOf] reads it back as not read in this shape, never as "no work started".
     */
    fun stored(c: Cache): Map<String, Any?> {
        val out = linkedMapOf<String, Any?>(
            "listed" to c.listed, "stage" to c.stage, "by" to c.by, "at" to c.at, "readAt" to c.readAt,
            "atUnknown" to c.atUnknown,
        )
        c.states?.let { st ->
            out["states"] = st.entries.associate { (w, ws) ->
                w.toString() to linkedMapOf<String, Any?>("state" to ws.state, "seq" to ws.seq, "at" to ws.at)
            }
        }
        return out
    }

    /**
     * A stored copy read back through [AlbumWire.Read] (:app's org.json tree), or null when [o] is not an object. Tolerant,
     * as an update must read what an older build wrote: a field absent or of another type reads as the [Cache] default --
     * a copy 0.3.270 or older wrote (no `states`, no `atUnknown` before the QM0 review) keeps its stage, who and when, its
     * states not read; a `states` entry without an integral seq, or for an id that is not one of the sixteen, is left out;
     * a state outside the five reads Not started ([Album.stateName]).
     */
    fun storedOf(r: AlbumWire.Read, o: Any?): Cache? {
        if (!r.isObject(o)) return null
        fun s(k: String): String = r.field(o, k) as? String ?: ""
        fun b(k: String): Boolean = r.field(o, k) as? Boolean ?: false
        val st = r.field(o, "states")
        val states = if (!r.isObject(st)) null else Album.WORK_IDS.mapNotNull { w ->
            val e = r.field(st, w.toString())
            val seq = if (r.isObject(e)) AlbumWire.long(r.field(e, "seq")) else null
            if (seq == null) null
            else w to WorkState(Album.stateName(r.field(e, "state") as? String), seq, r.field(e, "at") as? String ?: "")
        }.toMap()
        return Cache(listed = b("listed"), stage = s("stage"), by = s("by"), at = s("at"), readAt = s("readAt"),
            states = states, atUnknown = b("atUnknown"))
    }

    /**
     * The copy S5's album GET ([Album.View], read by [AlbumWire.view]) names, read at [readAt]: its quote
     * stage with who set it and when, and the works that have a state row ([Album.statesOfWorks], each at
     * the work's base). It goes in through [merge] like a read of the list.
     */
    fun fromAlbum(view: Album.View, readAt: String): Cache = Cache(
        listed = true, stage = view.quoteStage.orEmpty(), by = view.quoteStageBy, at = view.quoteStageAt,
        readAt = readAt, states = Album.statesOfWorks(view.works),
    )

    /**
     * One project row of S5's `GET /api/asis` (`{project, quote_stage, quote_stage_by, quote_stage_at,
     * work_states: {"<id>": {state, seq}}, …}`; asis.js at 9a51733), read through [AlbumWire.Read]'s three
     * questions; null without a project id. `work_states` absent (an S5 before S5-L) is null, not "no work
     * started"; an entry without an integral seq, or for an id that is not one of the sixteen, is left out.
     */
    fun listedOf(r: AlbumWire.Read, o: Any?): Listed? {
        if (!r.isObject(o)) return null
        val id = (r.field(o, "project") as? String)?.takeIf { it.isNotBlank() } ?: return null
        fun s(k: String): String = r.field(o, k) as? String ?: ""
        val ws = r.field(o, "work_states")
        val states = if (!r.isObject(ws)) null else Album.WORK_IDS.mapNotNull { w ->
            val e = r.field(ws, w.toString())
            val seq = if (r.isObject(e)) AlbumWire.long(r.field(e, "seq")) else null
            if (seq == null) null else w to WorkState(Album.stateName(r.field(e, "state") as? String), seq)
        }.toMap()
        return Listed(id, s("quote_stage"), s("quote_stage_by"), s("quote_stage_at"), states)
    }

    // ---- THE ONE DERIVED PROJECT LINE (v61c §3.1; index61c2.html projectLine) ------------------------------

    /** What the line says: not read, a project S5 does not list, a quote-journey stage, or execution. */
    enum class LineKind { UNREAD, LOCAL, QUOTE, EXEC }

    /**
     * The project line, drawn by every reader (the project screen, the list row, the album header, the Wish
     * list bar): its [kind]; the S5 stage it shows ([quote], an identity value; null when none is shown); the
     * first line ([text]); at Client approved the counts it was built from ([parts]); the [second] line; how
     * many of the six pips are lit ([lit]); and whether [second] carries the read time ([showRead]).
     */
    data class ProjectLine(
        val kind: LineKind,
        val quote: String?,
        val text: String,
        val parts: List<String>,
        val second: String,
        val lit: Int,
        val showRead: Boolean,
    )

    private const val SEP = " · " // text:value the line's separator, as the prototype joins it

    /**
     * THE ONE LINE, DERIVED, NEVER SET BY HAND (Amit, 27 Oct 2026; index61c2.html projectLine and lineHTML),
     * from the store's copy [cache] (null: never read) and this phone's queued state writes [queued]:
     * - [phoneOnly] (made on this phone, no S5 id) or a copy read as NOT LISTED: `Quote · not on S5 yet`, no
     *   pip, the second line `not on S5 yet — this project has not been sent` -- whatever the phone holds
     *   (v61c.2): a stage, states or queued writes are never shown for it;
     * - never read, a blank stage or a name outside the six (critic G6): `Quote · not read yet`, no pip;
     * - Survey through Revisions: `Quote · <stage>`, its pips, `since d MMM, HH:mm IST on S5` or `<stage> ·
     *   not set on S5 yet` when S5 has not timed it (once S5 lists a project, never the never-sent words);
     * - Client approved: `Execution`, then the non-zero counts in this order: in progress · done · on hold;
     *   `no work started yet` only while every work is Not started; the abandoned ones only when nothing else
     *   counts (§13.4, review 4) -- the states with [queued] laid over them ([Album.withQueued]), so a tap
     *   shows at once; and with no states read yet `Execution · not read yet`, never a count it has not read
     *   and never a bare `Execution` (critic G7). Its second line is `Quote · Client approved`.
     * The read time (decision 1 of 15 Oct) ends the second line of a quote-journey, unread or not-listed copy
     * read before today in IST.
     */
    fun projectLine(cache: Cache?, phoneOnly: Boolean, queued: List<Album.Op>, tx: Tx, now: Instant): ProjectLine {
        if (phoneOnly) return local(tx, "", false)
        val old = cache != null && readIsOld(cache.readAt, now)
        val read = if (old) tx.wish_read_x(hm = hm(instantOf(cache!!.readAt)!!)) else ""
        if (cache != null && !cache.listed) return local(tx, read, old)
        val stage = cache?.stage?.trim().orEmpty()
        val i = indexOf(stage)
        if (cache == null || i < 0) {
            return ProjectLine(LineKind.UNREAD, null, tx.common_quote_x(quote = tx.wish_not_read_yet), emptyList(),
                tx.wish_quote_stage_not_read + read, 0, old)
        }
        if (stage != APPROVED) {
            val shown = Masters.quote(tx, stage)
            val t = instantOf(cache.at)
            val second = if (t != null) tx.wish_since_x_x_ist(d = day(t), h = hm(t)) else tx.wish_x_not_set_s5_yet(stage = shown)
            return ProjectLine(LineKind.QUOTE, stage, tx.common_quote_x(quote = shown), emptyList(), second + read, i + 1, old)
        }
        val states = cache.states
        val parts = if (states == null) emptyList() else execParts(tx, Album.withQueued(states, queued))
        val head = if (states == null) listOf(tx.app_execution, tx.wish_not_read_yet) else listOf(tx.app_execution) + parts
        return ProjectLine(LineKind.EXEC, APPROVED, head.joinToString(SEP), parts,
            tx.common_quote_x(quote = Masters.quote(tx, APPROVED)), STAGES.size, false)
    }

    private fun local(tx: Tx, read: String, old: Boolean): ProjectLine =
        ProjectLine(LineKind.LOCAL, null, tx.common_quote_x(quote = tx.wish_not_on_s5_yet), emptyList(), tx.wish_not_s5_yet_project + read, 0, old)

    /** The counts of the approved line: in progress · done · on hold, the non-zero ones; else abandoned, else none started. */
    private fun execParts(tx: Tx, states: Map<Int, WorkState>): List<String> {
        fun n(st: String): Int = Album.WORK_IDS.count { Album.stateName(states[it]?.state) == st }
        val parts = listOfNotNull(
            n(Album.IN_PROGRESS).takeIf { it > 0 }?.let { tx.app_x_in_progress(n = it) },
            n(Album.DONE).takeIf { it > 0 }?.let { tx.app_x_done(n = it) },
            n(Album.ON_HOLD).takeIf { it > 0 }?.let { tx.app_x_on_hold(n = it) },
        )
        if (parts.isNotEmpty()) return parts
        val abandoned = n(Album.ABANDONED)
        return listOf(if (abandoned > 0) tx.app_x_abandoned(n = abandoned) else tx.app_no_work_started)
    }

    // ---- the rooms grid's dots and SURVEY COVERAGE (SPEC61c §3.2, P19; index61c2.html 8118, 4154) ------------

    /** The worksite stage every survey capture was filed at before v61c (S1's master); compared, never shown here. */
    const val SURVEY_STAGE = "As-built site measure" // text:value a worksite stage S5 and S1 hold, matched as the prototype's key()

    /** One capture of a room as the dot and the coverage read it: its own `work` (null = filed by its stage) and its legacy `stage`. */
    data class Shot(val work: Int?, val stage: String?)

    /** The prototype's key(): trimmed, lower-cased, every run of spaces or underscores one space. */
    private fun key(s: String): String = s.trim().lowercase().replace(Regex("[\\s_]+"), " ")

    /**
     * A capture of the quote journey's survey, as v61b counted it byte for byte (§3.2: "the quote-journey dots
     * and SURVEY COVERAGE keep v61b's rule"): its stage is As-built site measure, or it has none -- a new
     * capture (stage '') and one synced without a stage. NOT `workOfCapture == 0`, which would turn a
     * 'Defect recorded' photo of the quote journey green.
     */
    fun surveyShot(s: Shot): Boolean = s.stage.isNullOrEmpty() || key(s.stage) == key(SURVEY_STAGE)

    /**
     * The rooms grid's dot for a room with [captures] (green when true, amber when false; a room with none
     * draws no dot): before approval the journey rule ([surveyShot]); at Client approved ([approved]) a capture
     * filed to one of the sixteen works ([Album.workOfCapture], neither the quote journey nor unfiled).
     */
    fun roomDot(captures: List<Shot>, approved: Boolean): Boolean =
        if (approved) captures.any { s -> Album.workOfCapture(s.work, s.stage).let { w -> w != null && w != Album.QUOTE_WORK } }
        else captures.any { surveyShot(it) }

    /** SURVEY COVERAGE: [n] of the [of] rooms in scope shot in the survey; [left] not shot yet. */
    data class Coverage(val n: Int, val of: Int) {
        val left: Int get() = maxOf(0, of - n)
    }

    /**
     * SURVEY COVERAGE for the [rooms] in scope, each room's captures in [shots] (by room, exactly as named):
     * how many have a survey capture ([surveyShot]). Null when no room is in scope -- the prototype draws no
     * coverage row then.
     */
    fun coverage(rooms: List<String>, shots: Map<String, List<Shot>>): Coverage? {
        if (rooms.isEmpty()) return null
        return Coverage(rooms.count { r -> shots[r].orEmpty().any { surveyShot(it) } }, rooms.size)
    }

    /** The coverage row's two parts after its SURVEY COVERAGE label: "2 of 3 rooms shot", then "1 room not shot yet" or "every room in scope is shot". */
    fun coverageParts(tx: Tx, c: Coverage): List<String> = listOf(
        tx.phase_x_x_shot(n = c.n.toString(), plN = c.of),
        if (c.left > 0) tx.phase_x_not_shot_yet(plN = c.left) else tx.phase_every_room_scope_shot,
    )
}
