package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.time.Instant

/**
 * THE MARK SHEET (port 62 slice L3, 11 Nov 2026; the approved prototype v61c.2, index61c2.html, module L53: openSheet
 * 3012-3020, paint 3023-3060, refreshApply 3062, apply 3071-3088, onClick 3096-3101, onInput 3112-3115; its look is
 * the CSS at 1004-1068, drawn by app/MarkSheet.kt). Amit, 11 Nov 2026, after testing APK 0.3.318: "i want to mark
 * client wish list on foto itself the way i mark window door etc" -- "remove this window and install UPVC window",
 * "close this door with brickwork".
 *
 * A tap on a mark's code opens it. Top to bottom: the head (code · name, the trade's dot and short name, ✕); KIND, on a
 * mark whose type has sub-kinds on that surface (slice L6: its chips, the one the mark is ringed); TRADE, on
 * an Other, a Note, a Line and an area (the 13-trade picker); STATE, never on a Note, a Line or an area -- the
 * now-line ("● Client wish · since 11 Nov, 10:30 IST · decided by the client"), then what the phase offers the mark:
 * the four acts on a mark born Existing that has left, or is leaving, Existing (↻ Replace · ⇢ Shift · ⚒ Repair ·
 * ✕ Remove), its moves ([MarkLife.allowedMoves]) each with its hint and, where it needs one, its Why box; or the
 * gate's line (Survey; on the phone Client approved, DESIGN-port62 Q-list 6), or the line that says why the phase
 * leaves it no move; NOTE (two lines, 0/280, "Client & vendors | Office only"); ONE Done. ✕, the scrim and ◁ cancel.
 *
 * PURE: the sheet's [State] is a value, every tap and keystroke answers a NEW one ([pickSub], [pickMove], [typeWhy],
 * [pickAct], [typeNote], [pickOffice], [togglePicker], [pickCat]), [view] is everything the screen draws -- every text
 * here, no rule there -- and [apply] is Done: ONE undo step per sheet, taken through its `snapshot` just before the
 * first write, and none when nothing changed. EVERY WRITE GOES THROUGH [MarkLife], [Trades] AND [MarkTypes]
 * ([MarkLife.move], [MarkLife.setAct], [MarkLife.setNote], [Trades.setCat], [MarkTypes.setSub]), which replace the
 * item's `extra` map and never change it in place, so the prep file keeps every unknown key whole and an undo copy
 * keeps the map it had; the order of the writes is the prototype's (the kind, the trade, the note, then the move or
 * the act), so the keys land in its order.
 *
 * KIND AND A NEW NOTE (port 62 slice L6, 12 Nov 2026; index61c2.html 3013-3014, 3030, 3065-3070, 3073-3081). KIND: the
 * head names the mark by its kind ("FCRP1 · Wardrobe", following the chip picked), a Defect's trade follows its kind,
 * and Done stores the kind -- never the type's first, which is the default -- and tells the caller, so the tool
 * remembers the last kind used ([Applied.kind]). A NEW NOTE's sheet is `fresh`: it opens on its note field
 * ([View.focusNote]); its Done takes NO undo step (the placement took the one); and a Note still with no words goes
 * again -- on Done ([Outcome.REMOVED]) and on ✕, the scrim and ◁ ([cancel]) -- with the placement's own undo step
 * taken back ([takesBackUndo]), so it leaves nothing behind. A Note a file already holds opens like any item, and Done
 * on one with no words is refused in the prototype's own words.
 */
object MarkSheet {

    /** The separator the sheet joins with, as the prototype does: the head's "FW1 · Window", the parts of the now-line. */
    const val SEP = " · " // text:value the prototype's separator, the same in both languages

    /** After the trade's name on the TRADE button: it opens a picker. */
    private const val OPENS = " ▾" // text:value a glyph, the same in both languages

    // ------------------------------------------------------------------ what a sheet is open on

    /**
     * The item a sheet is open on, on surface [s]: a mark ([mark]), or a Line or an area ([meas]) -- exactly one of
     * the two. Found afresh by its id for every question ([itemOf]), never held: an undo copy is another object.
     */
    class Item(val s: String, val mark: SurveyPrep.Mark?, val meas: SurveyPrep.Meas?) {
        init { require((mark == null) != (meas == null)) }   // a mark, or a Line or area: one of the two
        val code: String get() = mark?.code ?: meas?.code.orEmpty()
        val extra: Map<String, Any?> get() = mark?.extra ?: meas?.extra ?: emptyMap()
        val locked: Boolean get() = if (mark != null) SurveyPrep.isLocked(mark) else meas != null && SurveyPrep.isLocked(meas)
    }

    /**
     * What the sheet opens on for the id [id]: a mark; else a Line or an area, which take a TRADE and a NOTE (the
     * prototype's lineById). Null for a Tape (the surveyor's own measurement: no trade, no note), a chain, the
     * reference rectangle and an id nothing has: no sheet.
     */
    fun itemOf(prep: SurveyPrep.Prep, id: String): Item? {
        prep.markById(id)?.let { (s, e) -> return Item(s, e, null) }
        prep.measById(id)?.let { (s, m) -> if (Trades.takesCat(m)) return Item(s, null, m) }
        return null
    }

    /** Whether a tap on [id]'s code opens a sheet at all: the code is a button only then. */
    fun opens(prep: SurveyPrep.Prep, id: String): Boolean = itemOf(prep, id) != null

    private fun lifeOf(it: Item): String = it.mark?.let { e -> MarkLife.lifeOf(e) } ?: MarkLife.EXIST
    private fun isNote(it: Item): Boolean = it.mark?.let { e -> MarkLife.isNote(e) } == true

    /** The mark whose STATE the sheet draws: never a Note's, a Line's or an area's -- they have none. */
    private fun stated(it: Item): SurveyPrep.Mark? = it.mark?.takeUnless { e -> MarkLife.isNote(e) }

    /** Locked, and not by Scope: "FD1 is locked — only its note can change" (the prototype's `ro`). A mark in Scope is locked BY its state, and its way out is a move. */
    private fun readOnly(it: Item): Boolean = it.locked && lifeOf(it) != MarkLife.SCOPE

    /** TRADE is drawn: on an Other and a Note ([Trades.takesCat]), and on every Line and area. */
    private fun takesCat(it: Item): Boolean = it.meas != null || it.mark?.let { e -> Trades.takesCat(e) } == true

    // ------------------------------------------------------------------ the sheet's own state

    /**
     * An open sheet (the prototype's S): the item's [id]; [focusMove], the move whose Why box the sheet opened on
     * (Dropped, when a Delete opened it); [to] the move picked, or none; [why] what is typed under each move that
     * needs one, by its target; [act] the act chip that is on and [act0] the mark's own act when the sheet opened
     * (null: none); [note] and [office] as typed and picked; [cat] the trade picked and [cat0] the one it opened
     * with; [picker]: the trade picker is open. Nothing here is written to the prep until [apply].
     *
     * Slice L6: [sub] the KIND chip that is on and [sub0] the one the sheet opened on -- the mark's stored sub-kind,
     * its type's first when it stores none or one this build does not know; null for a type with no sub-kinds on that
     * surface, and for a Line or an area. [fresh]: the sheet of a Note JUST PLACED, and [hist] how many undo steps the
     * screen held as it opened -- the placement's own the last of them ([takesBackUndo]).
     */
    data class State(
        val id: String, val focusMove: String?, val to: String?, val why: Map<String, String>,
        val act: String, val act0: String?, val note: String, val office: Boolean,
        val cat: String, val cat0: String, val picker: Boolean,
        val sub: String? = null, val sub0: String? = null, val fresh: Boolean = false, val hist: Int = 0,
    )

    /**
     * The sheet for [id] as it opens (openSheet), or null when nothing opens for it ([itemOf]): no move picked, the
     * act chip on the mark's own act, else Replace; the note and its audience as stored; the trade as stored, General
     * when none. [focusMove]: [MarkLife.DROP] when a tap on Drop opened it -- the sheet opens on Dropped's Why box,
     * and a why typed there picks the move ([typeWhy]). The KIND chip is on the mark's own sub-kind, its type's first
     * when it stores none ([MarkTypes.subInfo]). [fresh]: the sheet of a Note the tap before it placed -- it opens on
     * the note field, and [hist] is the screen's count of undo steps now, the placement's included.
     */
    fun open(prep: SurveyPrep.Prep, id: String, focusMove: String? = null, fresh: Boolean = false, hist: Int = 0): State? {
        val it = itemOf(prep, id) ?: return null
        val act0 = it.mark?.let { e -> MarkLife.actOf(e) }
        val cat = Trades.catOf(it.extra)?.takeIf { c -> c.isNotEmpty() } ?: Trades.GENERAL
        val sub = it.mark?.let { e -> MarkTypes.subOf(it.s, e)?.id }
        return State(id, focusMove, null, emptyMap(), act0 ?: MarkLife.REPLACE, act0, MarkLife.noteOf(it.extra), MarkLife.isOffice(it.extra),
            cat, cat, false, sub, sub, fresh, hist)
    }

    /**
     * A tap on a KIND chip (the page's `S.sub=d.l53sub`): that kind on -- the head, and a Defect's trade, follow it.
     * Nothing changes on a locked mark (its chips are drawn off, a Scope mark's too), nor for an id that is not one
     * of the type's sub-kinds on that surface.
     */
    fun pickSub(prep: SurveyPrep.Prep, st: State, sub: String): State {
        val it = itemOf(prep, st.id) ?: return st
        val e = it.mark ?: return st
        if (it.locked || MarkTypes.subsOf(e.t, it.s).none { x -> x.id == sub }) return st
        return st.copy(sub = sub)
    }

    /** The moves the phase offers this item, in the sheet's order: none for a Note, a Line or an area, and none at a gate. */
    private fun offered(it: Item, stage: Int?): List<MarkLife.Move> {
        val e = stated(it) ?: return emptyList()
        return if (PhaseTools.gate(stage) != null) emptyList() else MarkLife.allowedMoves(e, stage)
    }

    /** Whether the why typed for the move to [to] is enough: [MarkLife.WHY_MIN] characters once trimmed. */
    private fun whyOk(st: State, to: String): Boolean = st.why[to].orEmpty().trim().length >= MarkLife.WHY_MIN

    /**
     * The act chips are drawn: on a mark born Existing that has left Existing, or is about to (→ Client wish is
     * picked) -- and not while ← Back to Existing is (actOn). Never at a gate, where the sheet draws its line alone.
     */
    private fun actsShown(it: Item, st: State, stage: Int?): Boolean {
        val e = stated(it) ?: return false
        if (PhaseTools.gate(stage) != null) return false
        return MarkLife.bornOf(e) == MarkLife.EXIST && (MarkLife.lifeOf(e) != MarkLife.EXIST || st.to == MarkLife.WISH) && st.to != MarkLife.EXIST
    }

    /**
     * A tap on the move to [to]: picked, or put back when it was the one picked. Nothing changes when the phase does
     * not offer that move, the mark is locked (only its note can change), or the move still needs its why.
     */
    fun pickMove(prep: SurveyPrep.Prep, st: State, stage: Int?, to: String): State {
        val it = itemOf(prep, st.id) ?: return st
        val m = offered(it, stage).firstOrNull { x -> x.to == to } ?: return st
        if (readOnly(it) || (m.why && !whyOk(st, to))) return st
        return st.copy(to = if (st.to == to) null else to)
    }

    /**
     * A keystroke in the Why box under the move to [to] (onInput): [typed] kept, cut to [MarkLife.WHY_MAX]; a why
     * that falls under three characters un-picks its move; and when the sheet opened on this box ([State.focusMove]),
     * a why that reaches three picks it. Nothing changes on a locked mark, or for a move the phase does not offer or
     * that takes no why: there is no box.
     */
    fun typeWhy(prep: SurveyPrep.Prep, st: State, stage: Int?, to: String, typed: String): State {
        val it = itemOf(prep, st.id) ?: return st
        if (readOnly(it) || offered(it, stage).none { x -> x.to == to && x.why }) return st
        val next = st.copy(why = st.why + (to to typed.take(MarkLife.WHY_MAX)))
        val ok = whyOk(next, to)
        return when {
            !ok && next.to == to -> next.copy(to = null)
            ok && next.to != to && next.focusMove == to -> next.copy(to = to)
            else -> next
        }
    }

    /** A tap on an act chip: that act on. Nothing changes where the chips are not drawn, on a locked mark, or for an [act] that is not one of the four. */
    fun pickAct(prep: SurveyPrep.Prep, st: State, stage: Int?, act: String): State {
        val it = itemOf(prep, st.id) ?: return st
        if (!actsShown(it, st, stage) || readOnly(it) || MarkLife.glyph(act) == null) return st
        return st.copy(act = act)
    }

    /** [typed] cut to [MarkLife.NOTE_MAX] characters -- code points, so an emoji is never halved -- as the note box holds it. */
    fun noteCut(typed: String): String =
        if (typed.codePointCount(0, typed.length) <= MarkLife.NOTE_MAX) typed else typed.substring(0, typed.offsetByCodePoints(0, MarkLife.NOTE_MAX))

    /** A keystroke in the note: kept as typed, never past 280 characters. A locked item's note can change too. */
    fun typeNote(st: State, typed: String): State = st.copy(note = noteCut(typed))

    /** The audience picked: "Office only" ([office]) or "Client & vendors". Kept on the item only with a note ([apply]). */
    fun pickOffice(st: State, office: Boolean): State = st.copy(office = office)

    /** A tap on the TRADE button: the picker opens, or shuts. Nothing changes where TRADE is not drawn, or on a locked item. */
    fun togglePicker(prep: SurveyPrep.Prep, st: State): State {
        val it = itemOf(prep, st.id) ?: return st
        return if (!takesCat(it) || it.locked) st else st.copy(picker = !st.picker)
    }

    /** A tap anywhere else on the sheet: the picker shuts. */
    fun closePicker(st: State): State = if (st.picker) st.copy(picker = false) else st

    /** A trade picked in the picker: [cat] it is, and the picker shuts. Nothing changes while it is shut, or for an id outside the 13. */
    fun pickCat(st: State, cat: String): State = if (!st.picker || Trades.byId(cat) == null) st else st.copy(cat = cat, picker = false)

    // ------------------------------------------------------------------ what the screen draws

    /** A dot and a word: a trade (its dot, its short name), or a state. [colour] 0xRRGGBB. */
    class Dot(val colour: Int, val text: String)

    /** One trade in the picker: its [id], its dot and short name, and whether it is the one picked ([on]). */
    class TradeChip(val id: String, val colour: Int, val text: String, val on: Boolean)

    /**
     * TRADE: its [label]; the [button], the picked trade's dot and "Carpentry ▾" -- [enabled] false on a locked item;
     * and, while the picker is open, its [title] ("Trade · FX1") over the 13 [chips], else none.
     */
    class TradeBlock(val label: String, val button: Dot, val enabled: Boolean, val title: String, val chips: List<TradeChip>?)

    /** One kind in KIND: its stored [id], its label in the language, and whether it is the one picked ([on]). */
    class KindChip(val id: String, val text: String, val on: Boolean)

    /** KIND: its [label] over the type's sub-kinds on that surface, the first the default; [enabled] false on a locked mark. */
    class KindBlock(val label: String, val chips: List<KindChip>, val enabled: Boolean)

    /** One act chip: "↻ Replace"; [on]: the act picked; [enabled] false on a locked mark. */
    class ActChip(val id: String, val text: String, val on: Boolean, val enabled: Boolean)

    /** The Why box under a move that needs one: what is typed, and whether it takes typing (not on a locked mark). */
    class WhyBox(val text: String, val enabled: Boolean)

    /**
     * One move: [to] its target, whose state [colour] is its dot's; [label] "→ Client wish" and its small [hint], or
     * none; [on]: it is the move picked; [enabled] false on a locked mark and while it still needs its why; [why] its
     * box, when it takes one.
     */
    class MoveRow(val to: String, val colour: Int, val label: String, val hint: String?, val on: Boolean, val enabled: Boolean, val why: WhyBox?)

    /**
     * The now-line: the state's dot and [name] in bold, then [rest] -- " · since 11 Nov, 10:30 IST · decided by the
     * client", either part only when the mark carries it -- and [vplus], the badge of a mark a vendor added, drawn
     * after one more [SEP]; null: none.
     */
    class Now(val colour: Int, val name: String, val rest: String, val vplus: String?)

    /**
     * STATE: its [label] and the [now]-line; then EITHER the [gate]'s line and nothing else, OR the [acts] (empty:
     * none drawn), the [moves], [noMove] -- why the phase leaves this mark none -- the [amber] line of a move the
     * office is told of, and the [foots]: the unread stage's, a locked mark's.
     */
    class StateBlock(val label: String, val now: Now, val gate: String?, val acts: List<ActChip>, val moves: List<MoveRow>,
                     val noMove: String?, val amber: String?, val foots: List<String>)

    /** NOTE: its [label], "20/280" ([count]), what is typed, the box's [placeholder], and the audience -- [office] picked, or [cv]. */
    class NoteBlock(val label: String, val count: String, val text: String, val placeholder: String, val office: Boolean,
                    val cv: String, val officeOnly: String)

    /**
     * Everything the sheet draws: [pane] what a screen reader calls it; the head -- [code], [name], the [trade]'s dot
     * and short name, ✕ labelled [close]; [tradeBlock] and [state], each only where the item has one; [note]; [done]
     * and whether it answers ([doneEnabled]: not while the move picked still needs its why); [whyHint] every Why
     * box's placeholder; [focusWhy] the move whose Why box takes the keyboard as the sheet opens, or none. Slice L6:
     * [kind], KIND, only for a mark whose type has sub-kinds on that surface (drawn FIRST, over TRADE); [focusNote]:
     * the note field takes the keyboard as the sheet opens -- a Note just placed.
     */
    class View(val pane: String, val code: String, val name: String, val trade: Dot, val close: String, val tradeBlock: TradeBlock?,
               val state: StateBlock?, val note: NoteBlock, val done: String, val doneEnabled: Boolean, val whyHint: String, val focusWhy: String?,
               val kind: KindBlock? = null, val focusNote: Boolean = false)

    /** "since 16 Oct, 06:37 IST": when a mark took its state, from its `lifeAt`; "" when it has none or it is not a time (sinceText). */
    fun sinceText(tx: Tx, lifeAt: String?): String {
        val t = lifeAt?.let { QuoteStage.instantOf(it) } ?: return ""
        return tx.life_since_x_x_ist(day = QuoteStage.day(t), hm = QuoteStage.hm(t))
    }

    /**
     * Who decided, for the now-line (AS_WORDS): the surveyor, the client, the vendor, staff -- and a `lifeAs` that is
     * none of the four, as stored. `staff` has no key of its own: the prototype's text layer draws it through
     * `album.staff`, lower-cased ("decided by staff", "ठरवणारे: स्टाफ").
     */
    fun decidedBy(tx: Tx, lifeAs: String): String = when (lifeAs) {
        MarkLife.AS_SURVEYOR -> tx.life_the_surveyor
        MarkLife.AS_CLIENT -> tx.life_the_client
        MarkLife.AS_VENDOR -> tx.life_the_vendor
        MarkLife.AS_STAFF -> tx.album_staff.lowercase()
        else -> lifeAs
    }

    private fun now(tx: Tx, e: SurveyPrep.Mark): Now {
        val life = MarkLife.lifeOf(e)
        val since = if (life != MarkLife.EXIST) sinceText(tx, MarkLife.lifeAt(e)) else ""
        val by = MarkLife.lifeAs(e)?.takeIf { it.isNotEmpty() }?.let { tx.life_decided_x(decidedBy(tx, it)) }.orEmpty()
        return Now(MarkLife.byId(life).colour, MarkLife.name(tx, life), (if (since.isEmpty()) "" else SEP + since) + by,
            if (MarkLife.bornOf(e) == MarkLife.VADD) tx.life_vadd_badge else null)
    }

    private fun stateBlock(tx: Tx, it: Item, e: SurveyPrep.Mark, st: State, stage: Int?): StateBlock {
        val label = tx.common_state.uppercase()
        val gate = PhaseTools.gate(stage)
        if (gate != null) return StateBlock(label, now(tx, e), PhaseTools.gateText(tx, gate), emptyList(), emptyList(), null, null, emptyList())
        val life = MarkLife.lifeOf(e); val ro = readOnly(it)
        val moves = offered(it, stage)
        val acts = if (actsShown(it, st, stage)) MarkLife.ACTS.map { a -> ActChip(a.id, actText(tx, a.id), st.act == a.id, !ro) } else emptyList()
        val rows = moves.map { m ->
            MoveRow(m.to, MarkLife.byId(m.to).colour, MarkLife.label(tx, m), MarkLife.hint(tx, m), st.to == m.to,
                !(ro || (m.why && !whyOk(st, m.to))), if (m.why) WhyBox(st.why[m.to].orEmpty(), !ro) else null)
        }
        val picked = moves.firstOrNull { m -> m.to == st.to }
        val foots = listOfNotNull(
            if (PhaseTools.phaseOf(stage) == null) MarkLife.unreadFoot(tx) else null,
            if (ro) tx.life_x_locked_only_note(it.code) else null)
        return StateBlock(label, now(tx, e), null, acts, rows, if (moves.isEmpty()) PhaseTools.noMoveText(tx, life, stage) else null,
            if (picked?.amber == true) MarkLife.afterEstimate(tx) else null, foots)
    }

    /** An act chip's words, its glyph first: "↻ Replace", "✕ Remove" -- one whole text of the tables each. */
    fun actText(tx: Tx, act: String): String = when (act) {
        MarkLife.SHIFT -> tx.act_shift_sym
        MarkLife.REPAIR -> tx.act_repair_sym
        MarkLife.REMOVE -> tx.act_remove_sym
        else -> tx.act_replace_sym
    }

    /** The trade the head shows: a mark's own -- an Other's and a Note's the one PICKED, so the head follows the picker,
     *  and a Defect's its KIND chip's (Broken / hollow tile is the tiler's) -- a Line's and an area's the one picked. */
    private fun tradeOf(it: Item, st: State): String {
        val e = it.mark ?: return Trades.known(st.cat)
        return Trades.tradeOf(e.t, it.s, st.sub ?: Trades.subOf(e.extra), st.cat)
    }

    /** KIND for the item, or null: a Line, an area, and a mark whose type has no sub-kinds on that surface. */
    private fun kindBlock(tx: Tx, it: Item, st: State): KindBlock? {
        val e = it.mark ?: return null
        val subs = MarkTypes.subsOf(e.t, it.s)
        if (subs.isEmpty()) return null
        return KindBlock(tx.life_kind, subs.map { x -> KindChip(x.id, MarkTypes.subLabel(tx, x), x.id == st.sub) }, !it.locked)
    }

    private fun dot(tx: Tx, trade: String, tail: String = ""): Dot = Dot(Trades.byId(Trades.known(trade))?.dot ?: 0, Trades.short(tx, trade) + tail)

    /**
     * What the sheet draws for [st] at quote stage [stage] ([PhaseTools.stageOf]: 0-5, null = not read), in the
     * language; null when its item is gone -- the sheet closes (paint).
     */
    fun view(tx: Tx, prep: SurveyPrep.Prep, st: State, stage: Int?): View? {
        val it = itemOf(prep, st.id) ?: return null
        val m = it.meas; val e = stated(it)
        // a mark is named by its KIND where its type has kinds -- the chip picked, so the head follows it (subName)
        val name = if (m != null) SurveyMeas.typeName(tx, m) else MarkTypes.displayName(tx, it.mark?.t.orEmpty(), it.s, st.sub)
        val tradeBlock = if (!takesCat(it)) null else TradeBlock(tx.life_trade, dot(tx, st.cat, OPENS), !it.locked, tx.life_trade_x(it.code),
            if (st.picker) Trades.ALL.map { t -> TradeChip(t.id, t.dot, Trades.short(tx, t.id), st.cat == t.id) } else null)
        val state = if (e != null) stateBlock(tx, it, e, st, stage) else null
        val placeholder = when {
            isNote(it) -> tx.life_what_should_office_vendor
            m != null -> if (m.kind == SurveyMeas.LINE) tx.life_note_length_optional else tx.life_note_area_optional
            else -> tx.life_note_mark_optional
        }
        val count = "${st.note.codePointCount(0, st.note.length)}/${MarkLife.NOTE_MAX}"
        val note = NoteBlock(tx.life_note, count, st.note, placeholder, st.office, tx.common_client_vendors, tx.common_office_only)
        val picked = st.to?.let { to -> offered(it, stage).firstOrNull { x -> x.to == to } }
        val needsWhy = picked != null && picked.why && !whyOk(st, picked.to)
        val focusWhy = st.focusMove?.takeIf { f -> state?.moves?.any { r -> r.to == f && r.why?.enabled == true } == true }
        return View(tx.life_aria_mark_sheet, it.code, name, dot(tx, tradeOf(it, st)), tx.common_close, tradeBlock, state, note,
            tx.common_done, !needsWhy, MarkLife.whyNeeded(tx), focusWhy, kindBlock(tx, it, st), st.fresh)
    }

    // ------------------------------------------------------------------ Done

    /**
     * What Done did: [GONE] the item is gone, and [UNCHANGED] nothing was changed -- the sheet closes, with NO undo
     * step; [STAYS] the sheet stays open -- the move picked is no longer offered (it is un-picked), or still needs
     * its why; [NEEDS_WORDS] a Note with no words: it stays open and says so ([needsWords]); [REMOVED] a Note JUST
     * PLACED, still with no words: it is taken away again and the sheet closes ([emptyRemoved], [takesBackUndo]);
     * [DONE] written, in one undo step -- a new Note's in none of its own, its placement took the one -- and the sheet
     * closes.
     */
    enum class Outcome { GONE, UNCHANGED, STAYS, NEEDS_WORDS, REMOVED, DONE }

    /** A kind Done stored: type [t] on surface [s] is now of sub-kind [sub] -- what the tool remembers ([SurveyPrepPalette.rememberSub]). */
    class Kind(val t: String, val s: String, val sub: String)

    /**
     * Done's answer: its [outcome]; the [state] the sheet keeps while it stays open, else null; [amber]: a move the
     * office is told of ([MarkLife.afterEstimate]); [kind]: the KIND that was changed, or null -- "the tool remembers
     * the last kind used" (index61c2.html 7059).
     */
    class Applied(val outcome: Outcome, val state: State?, val amber: Boolean = false, val kind: Kind? = null)

    /**
     * A NEW NOTE THAT IS STILL EMPTY GOES AGAIN (removeFresh, index61c2.html 3068-3070): the Note of the fresh sheet
     * [st] is taken off its surface. True when it went. Only ever a Note just placed with no stored words -- it has
     * no reading and no state, and it is not locked. Its code is not handed out again: a code never is on the phone
     * (v52 slice 8), so the next Note there is FNOTE2 where the page, which counts the marks still there, says FNOTE1.
     */
    private fun removeFresh(prep: SurveyPrep.Prep, it: Item): Boolean {
        val e = it.mark ?: return false
        return MarkLife.isNote(e) && prep.deleteMark(e.id)
    }

    /**
     * ✕, THE SCRIM, ◁ AND A SWIPE DOWN (cancel, index61c2.html 3065-3067): the sheet closes and what was typed is
     * lost. TRUE when it was the sheet of a Note just placed that still has no stored words: that Note is REMOVED
     * here, and the caller takes the placement's undo step back ([takesBackUndo]) and says [emptyRemoved]. False:
     * nothing was changed.
     */
    fun cancel(prep: SurveyPrep.Prep, st: State): Boolean {
        if (!st.fresh) return false
        val it = itemOf(prep, st.id) ?: return false
        val e = it.mark ?: return false
        if (!MarkLife.isNote(e) || MarkLife.noteOf(e.extra).trim().isNotEmpty()) return false
        return removeFresh(prep, it)
    }

    /**
     * Whether the screen's LAST undo step goes with a removed new Note (removeFresh: `hist.length===S.hist`): the
     * sheet was a fresh one and nothing has been done since it opened, so the last of the [histSize] steps held is the
     * placement's own -- taken back, an empty Note leaves no undo entry at all.
     */
    fun takesBackUndo(st: State, histSize: Int): Boolean = st.fresh && histSize > 0 && histSize == st.hist

    /** "Empty note removed": said, with no UNDO, when a new Note with no words went again. */
    fun emptyRemoved(tx: Tx): String = tx.life_msg_empty_note_removed

    /** JavaScript's `x || null`, for the two keys the prototype compares that way: nothing, "" and false are none. */
    private fun orNone(v: Any?): Any? = if (v == null || v == "" || v == false) null else v

    /**
     * DONE (apply), at quote stage [stage] and time [at]. Nothing changed -- no kind, trade, note, audience, move or act
     * -- closes the sheet with no undo step. Otherwise [snapshot] is called ONCE, just before the first write (NOT for
     * the fresh sheet of a new Note: its placement took the step), and then, in the prototype's order: the kind
     * ([MarkTypes.setSub]: the type's first deletes `sub`; never on a locked mark; the box stays as it is); the trade
     * (General deletes `cat`; never on a locked item); the note, cut to 280
     * characters and kept as typed, with "Office only" only beside a note -- a blank note deletes both keys; then
     * the move picked ([MarkLife.move], with its why, and with the act on Existing → Client wish), or, with no move
     * picked, the act chip changed on a mark born Existing that has left Existing ([MarkLife.setAct]). A locked mark
     * changes its note alone: its moves, its acts and its trade are not offered, and none is applied.
     *
     * AT CLIENT APPROVED NO ACT IS WRITTEN (the L3 review, 11 Nov 2026): the phone's own gate draws no act chip there
     * ([actsShown]; DESIGN-port62 Q-list 6), and the chip a sheet opens with stands on Replace for a mark whose stored
     * act is absent or none of the four ([open]) -- so without this any Done there, a note's included, wrote
     * `act: replace` on a wish after approval, with no control shown. At Survey the act IS written, as the page writes
     * it: its apply asks the stage only through the moves, and Survey's gate is the page's own.
     */
    fun apply(prep: SurveyPrep.Prep, st: State, stage: Int?, at: Instant, snapshot: () -> Unit): Applied {
        val it = itemOf(prep, st.id) ?: return Applied(Outcome.GONE, null)
        val mark = it.mark; val meas = it.meas
        val ro = readOnly(it)
        val before = it.extra
        val noted = MarkLife.withNote(before, st.note, st.office)
        if (isNote(it) && noted[MarkLife.KEY_NOTE] == null)
            return if (st.fresh && removeFresh(prep, it)) Applied(Outcome.REMOVED, null) else Applied(Outcome.NEEDS_WORDS, st)
        val subCh = !ro && mark != null && st.sub != st.sub0
        val catCh = !ro && st.cat != st.cat0
        val noteCh = noted[MarkLife.KEY_NOTE] != orNone(before[MarkLife.KEY_NOTE]) || noted[MarkLife.KEY_VIS] != orNone(before[MarkLife.KEY_VIS])
        val actCh = st.to == null && mark != null && !MarkLife.isNote(mark) && MarkLife.bornOf(mark) == MarkLife.EXIST &&
            MarkLife.pastExisting(mark) && !ro && st.act != st.act0 && PhaseTools.gate(stage) != PhaseTools.Gate.APPROVED
        if (!subCh && !catCh && !noteCh && st.to == null && !actCh) return Applied(Outcome.UNCHANGED, null)
        val move = st.to?.let { to -> offered(it, stage).firstOrNull { x -> x.to == to } }
        if (st.to != null && (move == null || ro)) return Applied(Outcome.STAYS, st.copy(to = null))
        if (move != null && move.why && !whyOk(st, move.to)) return Applied(Outcome.STAYS, st)
        if (!st.fresh) snapshot()   // ONE undo step per sheet; a new Note's placement already took it
        // the kind first, as the page writes it: the type's first sub-kind is the default and is never stored
        if (subCh) mark?.let { m -> MarkTypes.setSub(it.s, m, st.sub) }
        if (catCh) { if (mark != null) Trades.setCat(mark, st.cat) else if (meas != null) Trades.setCat(meas, st.cat) }
        if (mark != null) MarkLife.setNote(mark, st.note, st.office) else if (meas != null) MarkLife.setNote(meas, st.note, st.office)
        var amber = false
        if (mark != null) {
            if (move != null) {
                val r = MarkLife.move(mark, move.to, stage, at, st.why[move.to].orEmpty(), st.act)
                amber = r.ok && r.amber
            } else if (actCh) MarkLife.setAct(mark, st.act)
        }
        val kind = if (!subCh) null else mark?.let { m -> st.sub?.let { sub -> Kind(m.t, it.s, sub) } }
        return Applied(Outcome.DONE, null, amber, kind)
    }

    /** "A Note needs its words — type them, or delete the Note": said when Done finds a Note with none ([Outcome.NEEDS_WORDS]). */
    fun needsWords(tx: Tx): String = tx.refuse_life_note_needs_words_type
}
