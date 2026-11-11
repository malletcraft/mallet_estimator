package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.time.Instant

/**
 * A MARK'S LIFE CYCLE (port 62 slice L1, 11 Nov 2026; the approved prototype v61c.2, index61c2.html, module L53: LIFE
 * 2756-2764, STROKE 2768, ACTS 2770, spared 2789, MOVES 2794-2811, allowedMoves 2818, move 2834-2846, asPlace 2848,
 * the delete, unlock and clear rules 2936-2955, the counts 2964-2968). Amit, 16 Oct 2026, 06:37 IST: "Also mark type
 * should also follow, survey which is existing, wish list which clients want, Vendor review and vendor approved and
 * vendor added. Client approved And finally scope for estimate."
 *
 * Every mark keeps v52's {id, t, code, x0, x1, y0, y1, locked}; v53 adds, ONLY WHEN NOT DEFAULT, `life` `lifeAt`
 * `lifeAs` `why` `born` `act` `note` `vis` (and `sub` / `cat`, which are [Trades]'). A mark with no `life` is
 * Existing, so every v52 prep reads as all Existing and saves byte-identical: nothing here writes a key whose value is
 * its default, and Existing is written by DELETING `life` and its companions, so a mark taken there and back is the
 * mark that left. The keys live in [SurveyPrep.Mark.extra], which the prep file already keeps whole at every level
 * (SurveyPrepFile) -- so there is no file migration -- and every write REPLACES the map, never changes it in place:
 * an undo copy may share the old one.
 *
 * The life-cycle table is ported WHOLE; which of its moves a phase offers is [PhaseTools.moveOk]'s. Pure: NOTHING ON
 * SCREEN READS THIS YET. Slice L2 draws from [stroke] and wires the delete, unlock and clear rules; L3's Mark sheet
 * offers [allowedMoves] and applies [move], [setAct] and [setNote]; L4 places with [placeAs]; L9 shows [countLife].
 * Not here, each with its slice: the pill, the act glyph and the note badge after a code (L2); the sheet's "since"
 * and "decided by" line (L3 -- `staff` has no `life.*` key: the prototype's own text layer draws it through
 * `album.staff`, lower-cased); the roll-up's words (L9 -- `existing`, `wish`, `scope` and `dropped` have no key yet,
 * and the prototype leaves that line English under मराठी). android/i18n/README.md, "Port 62 slice L1", has both.
 */
object MarkLife {

    // ------------------------------------------------------------------ 4.1 the eight states

    /** The state every mark is in until it is moved or placed otherwise. NEVER STORED: a mark with no `life` is Existing. */
    const val EXIST = "exist"
    const val WISH = "wish"
    const val VREV = "vrev"
    const val VOK = "vok"
    const val VADD = "vadd"
    const val COK = "cok"
    const val SCOPE = "scope"
    const val DROP = "drop"

    /**
     * A state's outline, in dp, in place of v52's (STROKE): the dash pattern (empty = solid), the width (null = v52's
     * own width, which follows the selection), round caps (a dotted line), the alpha and a grey stroke in place of the
     * type's colour. Existing has none: it is drawn exactly as v52 draws it.
     */
    class Stroke(val dash: List<Double>, val width: Double?, val round: Boolean = false, val alpha: Double = 1.0, val grey: Int? = null)

    /** One state: its stored [id], its pill's [colour] (0xRRGGBB) and its outline ([stroke]; null for Existing). */
    class State(val id: String, val colour: Int, val stroke: Stroke?)

    /** The eight, in the life cycle's own order (LIFE): the order of every count and every state list. */
    val STATES: List<State> = listOf(
        State(EXIST, 0x6b6f68, null),                                                   // solid 1.5 dp, as v52
        State(WISH, 0x0F766E, Stroke(listOf(6.0, 4.0), null)),                          // dashed 6/4 dp
        State(VREV, 0x9a6700, Stroke(listOf(2.0, 3.0), null)),                          // dashed 2/3 dp
        State(VOK, 0x1864ab, Stroke(emptyList(), 1.5)),                                 // solid 1.5 dp
        State(VADD, 0x6741d9, Stroke(listOf(2.0, 3.0), null, round = true)),            // dotted 2/3 dp
        State(COK, 0x2b8a3e, Stroke(emptyList(), 2.5)),                                 // solid 2.5 dp
        State(SCOPE, 0x1d1f1a, Stroke(emptyList(), 2.5)),                               // solid 2.5 dp, padlock
        State(DROP, 0xb3261e, Stroke(emptyList(), 1.0, alpha = 0.4, grey = 0x8a8f86)),  // 1 dp grey, red diagonal, 40 %
    )

    val IDS: List<String> = STATES.map { it.id }

    /** The seven a file may hold in `life`: every state but Existing. */
    val STORED: List<String> = IDS.drop(1)

    /** The state [id] names; anything else is Existing (byId). */
    fun byId(id: String?): State = STATES.firstOrNull { it.id == id } ?: STATES[0]

    /** A state's outline; null for Existing (and for an id that is no state), which v52 draws. */
    fun stroke(life: String?): Stroke? = byId(life).stroke

    /** A state's name on screen ("Client wish"); an id that is no state reads Existing's, as [byId]. */
    fun name(tx: Tx, life: String?): String = when (byId(life).id) {
        WISH -> tx.life_wish
        VREV -> tx.life_vrev
        VOK -> tx.life_vok
        VADD -> tx.life_vadd
        COK -> tx.life_cok
        SCOPE -> tx.life_scope
        DROP -> tx.life_drop
        else -> tx.life_exist
    }

    // ------------------------------------------------------------------ the acts, who decided, the limits

    const val REPLACE = "replace"
    const val SHIFT = "shift"
    const val REPAIR = "repair"
    const val REMOVE = "remove"

    /** What the client wants done to something that is THERE: its stored [id] and the [glyph] drawn after the mark's code. */
    class Act(val id: String, val glyph: String)

    /** The four acts (ACTS), in the sheet's order. Only a mark born Existing carries one: a new thing has nothing to replace. */
    val ACTS: List<Act> = listOf(Act(REPLACE, "↻"), Act(SHIFT, "⇢"), Act(REPAIR, "⚒"), Act(REMOVE, "✕"))

    /** An act's glyph, or null when [act] is not one of the four. */
    fun glyph(act: String?): String? = ACTS.firstOrNull { it.id == act }?.glyph

    /** An act's name: Replace, Shift, Repair, Remove. */
    fun actName(tx: Tx, act: String): String = when (act) {
        SHIFT -> tx.life_shift
        REPAIR -> tx.job_repair
        REMOVE -> tx.common_remove
        else -> tx.life_replace
    }

    /** Who decided a move, stored in `lifeAs` (a row's `as`; AS_WORDS names a fourth, `surveyor`, which nothing writes). */
    const val AS_CLIENT = "client"
    const val AS_VENDOR = "vendor"
    const val AS_STAFF = "staff"

    /** The Note's type token: a Note is a mark with no state, no act and no measures. */
    const val NOTE_TYPE = Trades.NOTE

    /** A note is at most this many characters (code points), a why this many; a why needs at least [WHY_MIN]. */
    const val NOTE_MAX = 280
    const val WHY_MAX = 140
    const val WHY_MIN = 3

    // ------------------------------------------------------------------ the keys, and reading a mark

    const val KEY_LIFE = "life"
    const val KEY_LIFE_AT = "lifeAt"
    const val KEY_LIFE_AS = "lifeAs"
    const val KEY_WHY = "why"
    const val KEY_BORN = "born"
    const val KEY_ACT = "act"
    const val KEY_NOTE = "note"
    const val KEY_VIS = "vis"

    /** `vis` when a note is for the office alone; "Client & vendors" is the default and is never stored. */
    const val VIS_OFFICE = "office"

    /** The ten keys v53 adds to a mark, each only when not default; a v52 mark has none of them. */
    val KEYS: List<String> = listOf(KEY_LIFE, KEY_LIFE_AT, KEY_LIFE_AS, KEY_WHY, KEY_BORN, KEY_ACT, KEY_NOTE, KEY_VIS, Trades.KEY_SUB, Trades.KEY_CAT)

    fun isNote(e: SurveyPrep.Mark): Boolean = e.t == NOTE_TYPE

    /**
     * The mark's state. An unknown `life` (a newer build's state, a typo, a value that is no string) is KEPT in the
     * file and read as Existing (spec 7.3); a Note has no state, so it is always Existing.
     */
    fun lifeOf(e: SurveyPrep.Mark): String = if (isNote(e)) EXIST else (e.extra[KEY_LIFE] as? String)?.takeIf { it in STORED } ?: EXIST

    /** What the mark was PLACED as: a Client wish, Vendor added, or -- anything else -- Existing: it was there. */
    fun bornOf(e: SurveyPrep.Mark): String = (e.extra[KEY_BORN] as? String)?.takeIf { it == WISH || it == VADD } ?: EXIST

    fun pastExisting(e: SurveyPrep.Mark): Boolean = lifeOf(e) != EXIST

    /** The mark's act, or null when it has none or it is not one of the four. */
    fun actOf(e: SurveyPrep.Mark): String? = (e.extra[KEY_ACT] as? String)?.takeIf { glyph(it) != null }

    /** Why it was dropped or sent back, as stored; "" when none. */
    fun whyOf(e: SurveyPrep.Mark): String = (e.extra[KEY_WHY] as? String).orEmpty()

    /** When the mark took its state (an IST stamp, [istIso]) and who decided ([AS_CLIENT] ...), as stored; null when none. */
    fun lifeAt(e: SurveyPrep.Mark): String? = e.extra[KEY_LIFE_AT] as? String
    fun lifeAs(e: SurveyPrep.Mark): String? = e.extra[KEY_LIFE_AS] as? String

    /** The note of a mark, a Line or an area, as stored ("" when none), and whether it is for the office alone. */
    fun noteOf(extra: Map<String, Any?>): String = (extra[KEY_NOTE] as? String).orEmpty()
    fun isOffice(extra: Map<String, Any?>): Boolean = extra[KEY_VIS] == VIS_OFFICE

    /**
     * What Clear this surface…, Clear whole room… and Start over KEEP (spared): every locked item (v50) and every
     * mark past Existing -- a wish is dropped one by one, with its why, never swept away.
     */
    fun spared(e: SurveyPrep.Mark): Boolean = SurveyPrep.isLocked(e) || pastExisting(e)
    fun spared(m: SurveyPrep.Meas): Boolean = SurveyPrep.isLocked(m)
    fun spared(c: SurveyPrep.Chain): Boolean = SurveyPrep.isLocked(c)

    // ------------------------------------------------------------------ 4.2 the moves, as data (the only ones the sheet offers)

    /** A move's words on its button. */
    enum class Label { TO_WISH, TO_VREV, TO_DROP, BACK_EXIST, TO_VOK, BACK_WISH, TO_COK, TO_SCOPE, OUT_SCOPE, RESTORE, BACK_VADD }

    /** The small line under a move's words. [TO_STATE] is a Restore's, once resolved: "to Client wish". */
    enum class Hint { CLIENT_WANTS, VENDOR_ASKS, VENDOR_CANNOT, CLIENT_CHANGES, TO_STATE }

    /**
     * One move of the table: [from] a state [to] another; [asWho] decided it (stored in `lifeAs`); [why]: it needs a
     * why; [back]: a back move; [bornOnly]: offered only to a mark born so; [restore]: a Dropped mark's way back,
     * whose [to] depends on what the mark was born ([allowedMoves] resolves it); [act]: the move that takes an act
     * with it. [amber]: set by [allowedMoves] alone -- after the estimate, a move into or out of Scope is told to the
     * office ([afterEstimate]).
     */
    data class Move(
        val from: String, val to: String, val asWho: String, val why: Boolean = false, val back: Boolean = false,
        val bornOnly: String? = null, val restore: Boolean = false, val act: Boolean = false,
        val label: Label, val hint: Hint? = null, val amber: Boolean = false,
    )

    /** The table (MOVES), by the state a mark is in, each state's moves in the sheet's order. */
    val MOVES: Map<String, List<Move>> = linkedMapOf(
        EXIST to listOf(
            Move(EXIST, WISH, AS_CLIENT, act = true, label = Label.TO_WISH, hint = Hint.CLIENT_WANTS)),
        WISH to listOf(
            Move(WISH, VREV, AS_STAFF, label = Label.TO_VREV),
            Move(WISH, DROP, AS_CLIENT, why = true, label = Label.TO_DROP),
            Move(WISH, EXIST, AS_STAFF, back = true, bornOnly = EXIST, label = Label.BACK_EXIST)),
        VREV to listOf(
            Move(VREV, VOK, AS_VENDOR, label = Label.TO_VOK),
            Move(VREV, WISH, AS_VENDOR, why = true, back = true, label = Label.BACK_WISH, hint = Hint.VENDOR_ASKS),
            Move(VREV, DROP, AS_VENDOR, why = true, label = Label.TO_DROP, hint = Hint.VENDOR_CANNOT)),
        VOK to listOf(
            Move(VOK, COK, AS_CLIENT, label = Label.TO_COK),
            Move(VOK, WISH, AS_CLIENT, back = true, label = Label.BACK_WISH, hint = Hint.CLIENT_CHANGES),
            Move(VOK, DROP, AS_CLIENT, why = true, label = Label.TO_DROP)),
        VADD to listOf(
            Move(VADD, COK, AS_CLIENT, label = Label.TO_COK),
            Move(VADD, DROP, AS_CLIENT, why = true, label = Label.TO_DROP)),
        COK to listOf(
            Move(COK, SCOPE, AS_STAFF, label = Label.TO_SCOPE),
            Move(COK, WISH, AS_CLIENT, back = true, label = Label.BACK_WISH, hint = Hint.CLIENT_CHANGES),
            Move(COK, DROP, AS_CLIENT, why = true, label = Label.TO_DROP)),
        SCOPE to listOf(
            Move(SCOPE, COK, AS_STAFF, back = true, label = Label.OUT_SCOPE)),
        DROP to listOf(
            Move(DROP, WISH, AS_STAFF, restore = true, label = Label.RESTORE)),
    )

    /** Every row of the table, flattened (MOVE_TABLE): seventeen. */
    val MOVE_TABLE: List<Move> = MOVES.values.flatten()

    /** A move's words: "→ Client wish", "← Back to Existing", "↺ Restore" ... */
    fun label(tx: Tx, m: Move): String = when (m.label) {
        Label.TO_WISH -> tx.life_client_wish
        Label.TO_VREV -> tx.life_vendor_review
        Label.TO_DROP -> tx.life_dropped
        Label.BACK_EXIST -> tx.life_back_existing
        Label.TO_VOK -> tx.life_vendor_approved
        Label.BACK_WISH -> tx.life_client_wish_2
        Label.TO_COK -> tx.life_client_approved
        Label.TO_SCOPE -> tx.life_scope_2
        Label.OUT_SCOPE -> tx.life_out_scope
        Label.RESTORE -> tx.life_restore
        Label.BACK_VADD -> tx.life_vendor_added
    }

    /** The small line under a move's words, or null when it has none; a resolved Restore's names where it goes. */
    fun hint(tx: Tx, m: Move): String? = when (m.hint) {
        null -> null
        Hint.CLIENT_WANTS -> tx.life_client_wants_changed
        Hint.VENDOR_ASKS -> tx.life_vendor_asks_change
        Hint.VENDOR_CANNOT -> tx.life_vendor_cannot_do
        Hint.CLIENT_CHANGES -> tx.life_client_changes
        Hint.TO_STATE -> tx.life_to_x(name(tx, m.to))
    }

    /**
     * A move resolved for one mark (resolve): a Restore goes to what the mark was born -- Vendor added, else Client
     * wish -- and says so in its hint; a Vendor added mark at Client approved goes BACK to Vendor added, never to
     * Client wish, and its button says so.
     */
    private fun resolve(e: SurveyPrep.Mark, m: Move, amber: Boolean): Move {
        val born = bornOf(e)
        return when {
            m.restore -> m.copy(to = if (born == VADD) VADD else WISH, hint = Hint.TO_STATE, amber = amber)
            m.back && m.to == WISH && born == VADD && lifeOf(e) == COK -> m.copy(to = VADD, label = Label.BACK_VADD, amber = amber)
            else -> m.copy(amber = amber)
        }
    }

    /**
     * The moves this mark is offered at quote stage [stage] ([PhaseTools.stageOf]: 0-5, null = not read), in the
     * sheet's order (allowedMoves): none for a Note; none at Survey; else its state's rows of [MOVES], less the ones
     * offered only to a mark born otherwise, less the ones the phase does not offer ([PhaseTools.moveOk]) -- with the
     * stage unread, every one -- each resolved for this mark.
     */
    fun allowedMoves(e: SurveyPrep.Mark, stage: Int?): List<Move> {
        if (isNote(e) || stage == PhaseTools.SURVEY) return emptyList()
        val life = lifeOf(e); val born = bornOf(e)
        val after = PhaseTools.phaseOf(stage).let { it != null && it >= PhaseTools.ESTIMATION }
        return MOVES.getValue(life)
            .filter { it.bornOnly == null || it.bornOnly == born }
            .filter { PhaseTools.moveOk(life, it, stage) }
            .map { resolve(e, it, amber = after && (it.to == SCOPE || life == SCOPE)) }
    }

    // ------------------------------------------------------------------ time, IST

    /** "2026-10-16T06:37:09+05:30": [at] as the IST stamp `lifeAt` holds, to the second (istISO). */
    fun istIso(at: Instant): String {
        val z = at.atZone(QuoteStage.IST)
        fun p(n: Int, w: Int = 2) = n.toString().padStart(w, '0')
        return "${p(z.year, 4)}-${p(z.monthValue)}-${p(z.dayOfMonth)}T${p(z.hour)}:${p(z.minute)}:${p(z.second)}+05:30"
    }

    // ------------------------------------------------------------------ a move

    /** Why a move was refused. [APPROVED] is the phone's own, until Amit rules on the prototype (DESIGN-port62 Q-list 6). */
    enum class Refusal { NOTE, SURVEY, APPROVED, NOT_OFFERED, WHY }

    /** What [move] did: [refusal] null when the mark moved [from] its state [to] the other; [amber] as [Move.amber]. */
    data class Moved(val refusal: Refusal?, val from: String, val to: String, val amber: Boolean = false) {
        val ok: Boolean get() = refusal == null
    }

    /**
     * Moves the mark [e] to state [to] at quote stage [stage], at time [at], and says what happened (move). Refused,
     * with nothing written: a Note (it has no state); at Survey, and on the phone at Client approved, by the phase's
     * gate ([PhaseTools.gate]); a move this mark is not offered at this stage ([allowedMoves], by its RESOLVED
     * target); a move that needs a why given fewer than [WHY_MIN] characters once trimmed.
     *
     * Written, into a NEW extra map: back at Existing, `life` `lifeAt` `lifeAs` `act` and `why` are DELETED; else
     * `life`, `lifeAt` ([istIso]) and `lifeAs` (the row's), `why` (trimmed, cut to [WHY_MAX]) or its deletion, and --
     * only on Existing → Client wish -- `act` ([act] when it is one of the four, else Replace). Scope LOCKS the mark,
     * and only leaving Scope unlocks it. It does not look at the lock otherwise: the Mark sheet offers a locked mark
     * no move (L3).
     */
    fun move(e: SurveyPrep.Mark, to: String, stage: Int?, at: Instant, why: String = "", act: String? = null): Moved {
        val from = lifeOf(e)
        if (isNote(e)) return Moved(Refusal.NOTE, from, to)
        when (PhaseTools.gate(stage)) {
            PhaseTools.Gate.SURVEY -> return Moved(Refusal.SURVEY, from, to)
            PhaseTools.Gate.APPROVED -> return Moved(Refusal.APPROVED, from, to)
            null -> Unit
        }
        val m = allowedMoves(e, stage).firstOrNull { it.to == to } ?: return Moved(Refusal.NOT_OFFERED, from, to)
        val said = why.trim()
        if (m.why && said.length < WHY_MIN) return Moved(Refusal.WHY, from, to)
        val x = LinkedHashMap(e.extra)
        if (to == EXIST) {
            x.remove(KEY_LIFE); x.remove(KEY_LIFE_AT); x.remove(KEY_LIFE_AS); x.remove(KEY_ACT); x.remove(KEY_WHY)
        } else {
            x[KEY_LIFE] = to; x[KEY_LIFE_AT] = istIso(at); x[KEY_LIFE_AS] = m.asWho
            if (m.why) x[KEY_WHY] = said.take(WHY_MAX) else x.remove(KEY_WHY)
            if (from == EXIST && to == WISH) x[KEY_ACT] = act?.takeIf { glyph(it) != null } ?: REPLACE
        }
        if (to == SCOPE) x[SurveyPrep.LOCKED] = true                   // Scope locks the mark ...
        if (from == SCOPE && to != SCOPE) x[SurveyPrep.LOCKED] = false // ... and only Out of scope unlocks it
        e.extra = x
        return Moved(null, from, to, m.amber)
    }

    /** What a refused move says ([m] is [move]'s answer about [e]); null when it was not refused. */
    fun refusalText(tx: Tx, e: SurveyPrep.Mark, m: Moved): String? = when (m.refusal) {
        null -> null
        Refusal.NOTE -> tx.life_note_has_no_state
        Refusal.SURVEY -> PhaseTools.gateText(tx, PhaseTools.Gate.SURVEY)
        Refusal.APPROVED -> PhaseTools.gateText(tx, PhaseTools.Gate.APPROVED)
        Refusal.NOT_OFFERED -> tx.refuse_life_x_x_cannot_move(code = e.code, v = name(tx, m.from), v2 = name(tx, m.to))
        Refusal.WHY -> whyNeeded(tx)
    }

    /** "Why? — at least 3 characters": the why box's hint, and what a move without its why is refused with. */
    fun whyNeeded(tx: Tx): String = tx.life_why_least_3_characters

    /** "After the estimate — tell the office": said after a move that came back [Moved.amber]. */
    fun afterEstimate(tx: Tx): String = tx.life_after_estimate_tell_office

    /** "Quote stage not read yet — S5 decides when online": the Mark sheet's footer while the stage is unread. */
    fun unreadFoot(tx: Tx): String = tx.life_quote_stage_not_read

    /**
     * Gives a mark born Existing that has already left Existing another act (the sheet's act chips with no move
     * chosen, apply's `actCh`). True when it changed; false -- nothing written -- for a mark born a wish or Vendor
     * added, one still Existing, an [act] that is not one of the four, or the act it already has.
     */
    fun setAct(e: SurveyPrep.Mark, act: String): Boolean {
        if (bornOf(e) != EXIST || !pastExisting(e) || glyph(act) == null || actOf(e) == act) return false
        e.extra = SurveyPrep.withKey(e.extra, KEY_ACT, act)
        return true
    }

    /**
     * Placing a mark "as …" (asPlace): `born` and `life` set ONCE, at placement, with `lifeAt` and who decided -- the
     * client for a wish, the vendor for Vendor added. Nothing is written -- and null answered -- for a Note, at
     * Survey, and for any [asId] but [WISH] and [VADD]: a mark placed as Existing is a v52 mark, with no key at all.
     * Which "as" a stage places is [PhaseTools.asFor]; where it answers null nothing is placed and this is not asked.
     */
    fun placeAs(e: SurveyPrep.Mark, asId: String?, stage: Int?, at: Instant): String? {
        if (isNote(e) || stage == PhaseTools.SURVEY || (asId != WISH && asId != VADD)) return null
        val x = LinkedHashMap(e.extra)
        x[KEY_BORN] = asId; x[KEY_LIFE] = asId; x[KEY_LIFE_AT] = istIso(at); x[KEY_LIFE_AS] = if (asId == WISH) AS_CLIENT else AS_VENDOR
        e.extra = x
        return asId
    }

    // ------------------------------------------------------------------ the note (a mark's, a Line's, an area's)

    /**
     * [extra] with the note as the Mark sheet's Done stores it (apply): [typed] cut to [NOTE_MAX] characters (code
     * points, so an emoji is never halved) and kept as typed; "Office only" kept only WITH a note; a note that is
     * blank DELETES both keys. A NEW map.
     */
    fun withNote(extra: Map<String, Any?>, typed: String, office: Boolean): Map<String, Any?> {
        val words = if (typed.codePointCount(0, typed.length) <= NOTE_MAX) typed else typed.substring(0, typed.offsetByCodePoints(0, NOTE_MAX))
        return LinkedHashMap(extra).apply {
            if (words.trim().isEmpty()) { remove(KEY_NOTE); remove(KEY_VIS) }
            else { put(KEY_NOTE, words); if (office) put(KEY_VIS, VIS_OFFICE) else remove(KEY_VIS) }
        }
    }

    fun setNote(e: SurveyPrep.Mark, typed: String, office: Boolean) { e.extra = withNote(e.extra, typed, office) }
    fun setNote(m: SurveyPrep.Meas, typed: String, office: Boolean) { m.extra = withNote(m.extra, typed, office) }

    // ------------------------------------------------------------------ delete, unlock and clear rules (4.2)

    /** The word on a mark's delete control (delWord). */
    enum class DeleteWord { DELETE, DROP, RESTORE }

    /** Delete for an Existing mark and a Note; Drop for a mark past Existing; Restore for a Dropped one. */
    fun deleteWord(e: SurveyPrep.Mark): DeleteWord = when {
        isNote(e) -> DeleteWord.DELETE
        lifeOf(e) == DROP -> DeleteWord.RESTORE
        pastExisting(e) -> DeleteWord.DROP
        else -> DeleteWord.DELETE
    }

    /** ... in the language. Drop's key is the one the prototype's own text layer gives the word (`type.cdp.short`, where: delWord). */
    fun deleteLabel(tx: Tx, e: SurveyPrep.Mark): String = when (deleteWord(e)) {
        DeleteWord.DELETE -> tx.common_delete
        DeleteWord.DROP -> tx.type_cdp_short
        DeleteWord.RESTORE -> tx.common_restore
    }

    /**
     * What a tap on a mark's delete control does (interceptDelete): [PLAIN], v52's own delete -- an Existing mark or
     * a Note is deleted, and a LOCKED mark is refused with v52's "locked — unlock first" (Scope is locked);
     * [RESTORE], a Dropped mark moves back at once ([restore]); [DROP_SHEET], any other mark past Existing is never
     * deleted: its Mark sheet opens on Dropped, where the why is typed.
     */
    enum class DeleteAct { PLAIN, RESTORE, DROP_SHEET }

    fun deleteAct(e: SurveyPrep.Mark): DeleteAct = when {
        !pastExisting(e) || SurveyPrep.isLocked(e) -> DeleteAct.PLAIN
        lifeOf(e) == DROP -> DeleteAct.RESTORE
        else -> DeleteAct.DROP_SHEET
    }

    /** A Dropped mark back to what it was born -- Vendor added, else Client wish -- as [move] answers it. */
    fun restore(e: SurveyPrep.Mark, stage: Int?, at: Instant): Moved = move(e, if (bornOf(e) == VADD) VADD else WISH, stage, at)

    /** "FW1 restored to Client wish": said after a [restore] that moved; the state is the one the mark is in NOW. */
    fun restoredText(tx: Tx, e: SurveyPrep.Mark): String = tx.life_msg_x_restored_x(code = e.code, v = name(tx, lifeOf(e)))

    /** Unlocking a Scope mark is refused with this (unlockRefusal); null: it may be unlocked. */
    fun unlockRefusal(tx: Tx, e: SurveyPrep.Mark): String? =
        if (lifeOf(e) == SCOPE) tx.refuse_life_x_scope_take_out(e.code) else null

    /** Unlock all marks skips Scope marks (unlockable) ... */
    fun unlockable(e: SurveyPrep.Mark): Boolean = lifeOf(e) != SCOPE

    /** ... and says how many stayed: "2 Scope marks stay locked". */
    fun scopeKeptText(tx: Tx, n: Int): String = tx.life_x_scope_mark_x(n)

    /** The separator between what a clear kept, as the prototype joins the parts. */
    private const val SEP = " · " // text:value the prototype's separator, the same in both languages

    /**
     * What a clear says (clearText): "3 items cleared"; or, when it kept anything, "Cleared 3 items · 1 locked kept ·
     * 2 marks past Existing kept — drop them one by one" -- [lockedKept] the locked items it kept, [pastKept] the
     * unlocked marks past Existing it kept. The prototype's clear says this only when [pastKept] is not 0; with
     * locked items alone kept it says v52's own line ([SurveyPrepEdit.clearedSnack]), as before.
     */
    fun clearText(tx: Tx, n: Int, lockedKept: Int, pastKept: Int): String {
        val parts = listOfNotNull(
            if (lockedKept > 0) tx.life_x_locked_kept(lockedKept.toString()) else null,
            if (pastKept > 0) tx.life_x_mark_x_past(pastKept) else null)
        return if (parts.isEmpty()) tx.life_x_item_x_cleared(n) else tx.life_cleared_x_item_x(n = n, v = parts.joinToString(SEP))
    }

    /** "2 marks past Existing kept — drop them one by one": what Start over says it kept, after its own "Started over — " (pastKeptText). */
    fun pastKeptText(tx: Tx, n: Int): String = tx.life_x_mark_x_past_2(n)

    // ------------------------------------------------------------------ 4.3 the roll-up's counts

    /** How many marks of [p] are in each state (countLife): every state present, in [IDS] order, zero included; Notes are not counted. */
    fun countLife(p: SurveyPrep.Prep): Map<String, Int> {
        val c = LinkedHashMap<String, Int>(); IDS.forEach { c[it] = 0 }
        for (s in SurveyPrep.SURFACES) for (e in p.surfaces[s]?.marks.orEmpty()) if (!isNote(e)) c[lifeOf(e)] = c.getValue(lifeOf(e)) + 1
        return c
    }

    /** Two counts added, state by state (addCounts); a state missing from either counts as none. */
    fun addCounts(a: Map<String, Int>, b: Map<String, Int>): Map<String, Int> =
        LinkedHashMap<String, Int>().apply { IDS.forEach { put(it, (a[it] ?: 0) + (b[it] ?: 0)) } }

    /** A project's marks by state: its rooms' preps added up (rollUp). Slice L9 shows it as "Marks: …". */
    fun rollUp(preps: List<SurveyPrep.Prep>): Map<String, Int> = preps.fold(addCounts(emptyMap(), emptyMap())) { a, p -> addCounts(a, countLife(p)) }
}
