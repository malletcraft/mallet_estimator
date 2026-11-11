package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.math.max

/**
 * LOCKS, DUPLICATE, THE CLEARS AND START OVER ON SURVEY PREP (prototype v52, v52 slice 8). Pure, on a
 * [SurveyPrep.Prep], so every rule is tested here; SurveyPrepScreen only dispatches and shows the texts.
 *
 * Amit, 14 Oct 2026 02:39 IST: "Give locking lines for reference and outer box lines so it won't move
 * accidentally. In fact each mark type should also be locked." So (sp.js 229-252, 823-850, 1683-1690):
 *  - a mark and a surface's four lines can be LOCKED: nothing moves, nothing is deleted, a clear keeps it and
 *    Start over keeps it; its readings stay usable and are kept with it;
 *  - a long press on a mark offers Delete · Duplicate · Lock/Unlock · Lock all <type> · Cancel;
 *  - Duplicate is a copy 100 mm away (back the other way at an edge) with the NEXT code and no readings;
 *  - ⋮ clears this surface or the whole room after a second tap -- every UNLOCKED item and its readings;
 *  - Start over keeps a locked surface's lines, every locked item and its readings; the rest starts fresh.
 * Everything walks [SurveyPrep.Prep.itemsOf], so the Line and Tape (slice 9) and the chains (slice 11) join a clear
 * and a count by joining it; Start over, Lock all, the menu and Duplicate name them.
 *
 * PORT 62 SLICE L2 (11 Nov 2026; the approved prototype v61c.2, index61c2.html 2936-2955): A MARK PAST EXISTING IS
 * PROTECTED BY THE SAME RULES. Its delete control reads Drop, or Restore on a Dropped mark ([deleteWord], the menu's
 * first row), and never deletes it; the clears and Start over keep it, locked or not, with its readings, and say so
 * ([clearedSnack], [startedOver]); a mark in Scope is not unlocked by its padlock or by Unlock all ([setLocked],
 * [unlockRefusal], [lockAll], [scopeKept]). What is kept is [MarkLife.spared]'s to say, in one place.
 */
object SurveyPrepEdit {

    // ------------------------------------------------------------ the words (sp.js, exactly; keyed in port 61c slice M3a)

    /** A surface's name in the language, for the texts that name one. */
    private fun surface(tx: Tx, s: String) = Masters.surface(tx, SurveyPrep.NAME.getValue(s))

    /** Every refused move, resize or delete of a locked mark (sp.js refuse). */
    fun refusal(tx: Tx, code: String) = tx.refuse_locked_unlock_first(code)
    /** A drag or a nudge on a surface whose lines are locked (sp.js 949, 1064). */
    fun linesRefusal(tx: Tx) = tx.refuse_lines_locked
    /** A delete, with UNDO. */
    fun deleted(tx: Tx, code: String) = tx.sp_msg_x_deleted(code)
    /** The lines' padlock tapped (sp.js setLock 'lines:'): "locked" or "unlocked" is chosen in code, so one whole key each. */
    fun linesSaid(tx: Tx, s: String, on: Boolean) = if (on) tx.sp_x_lines_locked(surface(tx, s)) else tx.sp_x_lines_unlocked(surface(tx, s))
    /** The lines' padlock's own label: its state (sp.js .lkl span). */
    fun linesState(tx: Tx, on: Boolean) = if (on) tx.common_locked else tx.common_unlocked
    /** ... and what a tap on it does, for the screen reader. */
    fun linesAction(tx: Tx, on: Boolean) = if (on) tx.sp_aria_unlock_lines else tx.sp_aria_lock_lines

    /** The separator between two things one snackbar says, as the prototype joins them. */
    private const val SEP = " · " // text:value the prototype's separator, the same in both languages

    /** Lock all / Unlock all, counting what it CHANGED (ct52: "Locked 1 item · front wall"): "Locked" or "Unlocked" is
     *  chosen in code, so one whole key each (the card's group 2 split of sp.msg.x_x_item_x). [scopeKept] (port 62 slice
     *  L2): the Scope marks an Unlock all left locked ([scopeKept]), said after it as the prototype's lockAll does --
     *  "Unlocked 2 items · front wall · 1 Scope mark stays locked". */
    fun lockedSnack(tx: Tx, on: Boolean, n: Int, s: String, scopeKept: Int = 0): String {
        val said = if (on) tx.sp_msg_locked_x_item_x(n = n, surface = surface(tx, s).lowercase())
                   else tx.sp_msg_unlocked_x_item_x(n = n, surface = surface(tx, s).lowercase())
        return if (scopeKept > 0) said + SEP + MarkLife.scopeKeptText(tx, scopeKept) else said
    }

    /** The Start over button, and its text once armed (sp.js 1683). */
    fun startOverLabel(tx: Tx) = tx.common_start_over
    fun startOverArmed(tx: Tx) = tx.sp_tap_again_clear_every
    /** The note after Start over, when it kept anything; null when it kept nothing (sp.js 1690). */
    fun startedOver(tx: Tx, kept: Int): String? = if (kept > 0) tx.sp_msg_started_over_x_locked(kept.toString()) else null

    /**
     * The note after Start over [r] (index61c2.html 6942): when it kept marks past Existing, "Started over — 2 marks
     * past Existing kept — drop them one by one" -- the locked ones are not counted in that line, as the prototype's
     * says only the one; else v52's "Started over — 3 locked kept"; null when it kept nothing.
     */
    fun startedOver(tx: Tx, r: Restarted): String? =
        if (r.past > 0) tx.sp_msg_started_over_x(MarkLife.pastKeptText(tx, r.past)) else startedOver(tx, r.kept)

    /** The snackbar after a clear (sp.js 850). */
    fun clearedSnack(tx: Tx, n: Int, kept: Int) =
        if (kept > 0) tx.sp_cleared_x_item_x(n = n, kept = kept.toString()) else tx.sp_x_item_x_cleared(n)

    /**
     * The snackbar after the clear [r] (index61c2.html 5659): when it kept marks past Existing, the line that names
     * them ([MarkLife.clearText]: "Cleared 3 items · 1 locked kept · 2 marks past Existing kept — drop them one by
     * one"); else v52's own, word for word as before.
     */
    fun clearedSnack(tx: Tx, r: Cleared): String =
        if (r.past > 0) MarkLife.clearText(tx, r.n, r.kept, r.past) else clearedSnack(tx, r.n, r.kept)

    /**
     * A clear button's text in ⋮ (sp.js 1473-1477): armed, "Tap again to clear N items"; with something to
     * clear, "Clear this surface… (N)" / "Clear whole room… (N)"; with nothing but locked items, "K locked —
     * nothing to clear"; with nothing at all, "Nothing on this surface to clear" / "Nothing in the room to clear".
     * The room and the surface each have their own whole keys, as the prototype has them.
     */
    fun clearLabel(tx: Tx, scope: String, n: Int, locked: Int, armed: Boolean): String = when {
        armed && n > 0 -> if (scope == ROOM) tx.sp_tap_again_clear_x_2(n) else tx.sp_tap_again_clear_x(n)
        n > 0 -> if (scope == ROOM) tx.sp_clear_whole_room_x(n.toString()) else tx.sp_clear_surface_x(n.toString())
        locked > 0 -> tx.refuse_sp_x_locked_nothing_clear(locked.toString())
        else -> if (scope == ROOM) tx.sp_nothing_room_clear else tx.sp_nothing_surface_clear
    }

    /** How long a clear (and Start over) stays armed for its second tap, ms. */
    const val ARM_MS = 4000L
    /** The snackbar's life, ms (sp.js snack). */
    const val SNACK_MS = 5000L
    /** A passing note's life, ms (sp.js say). */
    const val SAY_MS = 2600L

    // ------------------------------------------------------------ locks

    /**
     * Lock ([on] true), unlock (false) or toggle (null) the mark [id]. The new state; null when there is no
     * such mark. The lock replaces the mark's extra map (an undo copy may share the old one). A SCOPE MARK STAYS
     * LOCKED (port 62 slice L2; the prototype's setLock and unlockRefusal, index61c2.html 5053, 2947): unlocking one
     * changes nothing and answers true -- it is taken Out of scope in its sheet -- and [unlockRefusal] has the words.
     */
    fun setLocked(p: SurveyPrep.Prep, id: String, on: Boolean?): Boolean? {
        // the reference rectangle (v52 slice 12) locks the same way: its padlock and the long press's Lock
        SurveyRef.byId(p, id)?.let { (s, _) -> return SurveyRef.setLocked(p.surface(s), on) }
        val e = p.markById(id)?.second
        if (e == null) {
            // a chain (v52 slice 11) locks the same way
            p.chainById(id)?.second?.let { c ->
                val v = on ?: !SurveyPrep.isLocked(c)
                if (SurveyPrep.isLocked(c) != v) c.extra = SurveyPrep.withKey(c.extra, SurveyPrep.LOCKED, v)
                return v
            }
            // a Tape, a Line or an area (v52 slice 9) locks the same way
            val m = p.measById(id)?.second ?: return null
            val v = on ?: !SurveyPrep.isLocked(m)
            if (SurveyPrep.isLocked(m) != v) m.extra = SurveyPrep.withKey(m.extra, SurveyPrep.LOCKED, v)
            return v
        }
        val v = on ?: !SurveyPrep.isLocked(e)
        if (SurveyPrep.isLocked(e) == v) return v
        if (!v && !MarkLife.unlockable(e)) return true
        e.extra = SurveyPrep.withKey(e.extra, SurveyPrep.LOCKED, v)
        return v
    }

    /**
     * What unlocking the item [id] is refused with, or null when it may be unlocked (or is not locked, or is no
     * mark): a locked mark in Scope answers "FW1 is in Scope — take it Out of scope in its sheet"
     * ([MarkLife.unlockRefusal]). The screen says it in the no-UNDO snackbar and takes no undo step, as the
     * prototype's setLock does.
     */
    fun unlockRefusal(tx: Tx, p: SurveyPrep.Prep, id: String): String? =
        p.markById(id)?.second?.takeIf { SurveyPrep.isLocked(it) }?.let { MarkLife.unlockRefusal(tx, it) }

    /** Lock, unlock or toggle (null) surface [s]'s four lines; the new state. */
    fun setLinesLocked(p: SurveyPrep.Prep, s: String, on: Boolean?): Boolean {
        val sf = p.surface(s)
        val v = on ?: !SurveyPrep.linesLocked(sf)
        if (SurveyPrep.linesLocked(sf) != v) sf.extra = SurveyPrep.withKey(sf.extra, SurveyPrep.LINES_LOCK, v)
        return v
    }

    /** Lock all's kinds besides a mark type (sp.js lockAll): every mark, every TAPE, every LINE MARK -- a Line or an
     *  area, never a tape (Amit, 14 Oct 2026: tapes and Lines are two tools, and lock apart). */
    const val KIND_MARK = "mark"
    const val KIND_TAPE = "tape"
    const val KIND_LINE = "meas"
    /** Every chain (v52 slice 11): "Lock all chains". */
    const val KIND_CHAIN = SurveyChain.KIND

    /**
     * LOCK ALL: every mark on [s] of type [t] (a mark type's letter); or every item of [kind] -- [KIND_MARK] the marks,
     * [KIND_TAPE] the tapes only, [KIND_LINE] the Lines and areas only, never a tape, [KIND_CHAIN] the chains; or
     * EVERY item there when both are null -- locked ([on]) or unlocked. Returns how many CHANGED -- what the
     * snackbar counts (sp.js lockAll). UNLOCK ALL SKIPS SCOPE MARKS (port 62 slice L2; the prototype's unlockable,
     * index61c2.html 5055, 2949): they stay locked and are not counted; [scopeKept] says how many.
     */
    fun lockAll(p: SurveyPrep.Prep, s: String, t: String?, on: Boolean, kind: String? = null): Int {
        var n = 0
        val sf = p.surface(s)
        for (e in lockMarks(sf, t, kind)) {
            if (SurveyPrep.isLocked(e) == on) continue
            if (!on && !MarkLife.unlockable(e)) continue
            e.extra = SurveyPrep.withKey(e.extra, SurveyPrep.LOCKED, on); n++
        }
        if (t == null && kind != KIND_MARK) for (m in sf.meas) {
            val tape = m.kind == SurveyMeas.TAPE
            if (kind == KIND_TAPE && !tape) continue
            if (kind == KIND_LINE && tape) continue
            if (kind != null && kind != KIND_TAPE && kind != KIND_LINE) continue
            if (SurveyPrep.isLocked(m) == on) continue
            m.extra = SurveyPrep.withKey(m.extra, SurveyPrep.LOCKED, on); n++
        }
        if (t == null && (kind == null || kind == KIND_CHAIN)) for (c in sf.chains) {
            if (SurveyPrep.isLocked(c) == on) continue
            c.extra = SurveyPrep.withKey(c.extra, SurveyPrep.LOCKED, on); n++
        }
        return n
    }

    /** The marks a Lock all of type [t] or [kind] walks: that type's; every mark for [KIND_MARK] or when both are null; none for another kind. */
    private fun lockMarks(sf: SurveyPrep.Surface, t: String?, kind: String?): List<SurveyPrep.Mark> =
        if (kind == null || kind == KIND_MARK || t != null) sf.marks.filter { t == null || it.t == t } else emptyList()

    /**
     * How many Scope marks the same [lockAll] leaves LOCKED (the prototype's nScope): none when it locks; when it
     * unlocks, every locked mark of its group that is in Scope. The screen says so -- after "Unlocked N items · front
     * wall" ([lockedSnack]), or alone, with no UNDO, when the Unlock all changed nothing ([MarkLife.scopeKeptText]).
     * Asked BEFORE or after the [lockAll]: a Scope mark's lock does not move.
     */
    fun scopeKept(p: SurveyPrep.Prep, s: String, t: String?, on: Boolean, kind: String? = null): Int =
        if (on) 0 else lockMarks(p.surface(s), t, kind).count { SurveyPrep.isLocked(it) && !MarkLife.unlockable(it) }

    /** The group a long press can lock at once: every mark of [e]'s type on [s] (sp.js lockGroup) -- or, for a Tape,
     *  every tape there ([kind] [KIND_TAPE], "tapes"), for a Line every Line and area ([KIND_LINE], "Line marks").
     *  [all]: every one of them is locked already, so the menu offers "Unlock all <name>". [t]: the mark type, or null.
     *  [name] is the group's ENGLISH identity -- the type's name, or the kind's word; [groupName] draws it. */
    class Group(val t: String?, val name: String, val all: Boolean, val n: Int, val kind: String? = null)

    /** What "Lock all {name}" names, in the language: a type through Masters, a kind through its own key (sp.group.*,
     *  sp.line_marks). */
    fun groupName(tx: Tx, g: Group): String = when {
        g.t != null -> Masters.type(tx, g.name)
        g.kind == KIND_TAPE -> tx.sp_group_tapes
        g.kind == KIND_LINE -> tx.sp_line_marks
        g.kind == KIND_CHAIN -> tx.sp_group_chains
        else -> g.name
    }

    fun lockGroup(p: SurveyPrep.Prep, s: String, e: SurveyPrep.Mark): Group {
        val l = p.surface(s).marks.filter { it.t == e.t }
        return Group(e.t, SurveyPrep.typeOf(s, e.t).name, l.isNotEmpty() && l.all { SurveyPrep.isLocked(it) }, l.size)
    }

    fun lockGroup(p: SurveyPrep.Prep, s: String, m: SurveyPrep.Meas): Group {
        val tape = m.kind == SurveyMeas.TAPE
        val l = p.surface(s).meas.filter { (it.kind == SurveyMeas.TAPE) == tape }
        return Group(null, if (tape) "tapes" else "Line marks", l.isNotEmpty() && l.all { SurveyPrep.isLocked(it) }, l.size, // text:value the group's identity, drawn through groupName
            if (tape) KIND_TAPE else KIND_LINE)
    }

    /** ... for a chain, every chain there ([KIND_CHAIN], "chains": "Lock all chains", v52 slice 11). */
    fun lockGroup(p: SurveyPrep.Prep, s: String, @Suppress("UNUSED_PARAMETER") c: SurveyPrep.Chain): Group {
        val l = p.surface(s).chains
        return Group(null, SurveyChain.GROUP, l.isNotEmpty() && l.all { SurveyPrep.isLocked(it) }, l.size, KIND_CHAIN)
    }

    /** [lockGroup] for any item by its id; null when there is none. */
    fun lockGroupOf(p: SurveyPrep.Prep, id: String): Pair<String, Group>? {
        p.markById(id)?.let { (s, e) -> return s to lockGroup(p, s, e) }
        p.measById(id)?.let { (s, m) -> return s to lockGroup(p, s, m) }
        p.chainById(id)?.let { (s, c) -> return s to lockGroup(p, s, c) }
        return null
    }

    /** What every delete path and the menu call an item (sp.js itemById's label): "FD1 Door", "F-M1 marked length",
     *  "F-T1 tape", "F-M2 marked area". The trash's screen-reader label is "Delete " and this. A mark's type is drawn
     *  through Masters; a Tape's, a Line's and a chain's labels are SurveyMeas's and SurveyChain's (port 61c slice M3b). */
    fun labelOf(tx: Tx, p: SurveyPrep.Prep, id: String): String? {
        p.markById(id)?.let { (s, e) -> return e.code + " " + SurveyPrep.typeName(tx, s, e.t) }
        p.measById(id)?.let { (_, m) -> return SurveyMeas.label(tx, m) }
        p.chainById(id)?.let { (_, c) -> return SurveyChain.label(tx, c) }
        return null
    }

    /** The space between a delete control's word and what it names, as the prototype joins them (`${delWord} ${code}`). */
    private const val WORD_GAP = " " // text:value a space, the same in both languages

    /**
     * THE WORD ON AN ITEM'S DELETE CONTROL (port 62 slice L2; the prototype's delWord on the picked mark's row, a list
     * row and the menu, index61c2.html 5660, 6713, 6748): a mark's own -- Delete, Drop past Existing, Restore on a
     * Dropped one ([MarkLife.deleteLabel]) -- and Delete for a Tape, a Line, an area, a chain and anything else.
     */
    fun deleteWord(tx: Tx, p: SurveyPrep.Prep, id: String): String =
        p.markById(id)?.second?.let { MarkLife.deleteLabel(tx, it) } ?: tx.common_delete

    /**
     * The trash's label: "Delete F-M1 marked length", "Delete FK1 chain" (ct52 FX9). On a mark past Existing it is
     * the word the control reads and then the mark, "Drop FW1 Window", "Restore FW1 Window" -- joined as the
     * prototype's own label is (`${delWord} ${code}`, index61c2.html 6660), there being no whole key for the two.
     */
    fun deleteLabel(tx: Tx, p: SurveyPrep.Prep, id: String): String? {
        val label = labelOf(tx, p, id) ?: return null
        val e = p.markById(id)?.second
        return if (e == null || MarkLife.deleteWord(e) == MarkLife.DeleteWord.DELETE) tx.sp_delete_x(label)
               else MarkLife.deleteLabel(tx, e) + WORD_GAP + label
    }

    // ------------------------------------------------------------ the long-press menu

    const val DELETE = "del"
    const val DUPLICATE = "dup"
    const val LOCK = "lock"
    const val LOCK_ALL = "lockall"
    const val CANCEL = "cancel"

    /** The menu's head: "FD1 Door", "🔒 FD1 Door" when locked (sp.js 1469). */
    fun menuHeader(tx: Tx, s: String, e: SurveyPrep.Mark): String =
        (if (SurveyPrep.isLocked(e)) "🔒 " else "") + e.code + " " + SurveyPrep.typeName(tx, s, e.t)

    /** ... on a Tape or a Line: "F-T1 tape", "🔒 F-M1 marked length". */
    fun menuHeader(tx: Tx, m: SurveyPrep.Meas): String = (if (SurveyPrep.isLocked(m)) "🔒 " else "") + SurveyMeas.label(tx, m) // text:value the padlock glyph

    /** ... on a chain: "FK1 chain", "🔒 FK1 chain" (v52 slice 11). */
    fun menuHeader(tx: Tx, c: SurveyPrep.Chain): String = (if (SurveyPrep.isLocked(c)) "🔒 " else "") + SurveyChain.label(tx, c) // text:value the padlock glyph

    /** The head for any item by its id; null when there is none. The reference's, a Tape's, a Line's and a chain's
     *  heads are SurveyRef's, SurveyMeas's and SurveyChain's (port 61c slice M3b). */
    fun menuHeaderOf(tx: Tx, p: SurveyPrep.Prep, id: String): String? =
        SurveyRef.byId(p, id)?.let { (s, r) -> SurveyRef.menuHeader(tx, s, r.locked) } ?: p.markById(id)?.let { (s, e) -> menuHeader(tx, s, e) } ?: p.measById(id)?.let { (_, m) -> menuHeader(tx, m) }
            ?: p.chainById(id)?.let { (_, c) -> menuHeader(tx, c) }

    /** The menu on a mark, in order, as (action, text): Delete · Duplicate · Lock|Unlock · Lock all X|Unlock all X · Cancel
     *  (ct52: "Delete|Duplicate|Lock|Lock all Door|Cancel"). Its first row reads the mark's own delete word -- Drop
     *  past Existing, Restore on a Dropped mark (port 62 slice L2; index61c2.html 6713) -- and is still [DELETE]. */
    fun menuItems(tx: Tx, p: SurveyPrep.Prep, s: String, e: SurveyPrep.Mark): List<Pair<String, String>> =
        menu(tx, lockGroup(p, s, e), SurveyPrep.isLocked(e), MarkLife.deleteLabel(tx, e))

    /** ... on a Tape ("Lock all tapes") or a Line ("Lock all Line marks"): ct52 T, "Delete|Duplicate|Lock|Lock all tapes|Cancel". */
    fun menuItems(tx: Tx, p: SurveyPrep.Prep, s: String, m: SurveyPrep.Meas): List<Pair<String, String>> =
        menu(tx, lockGroup(p, s, m), SurveyPrep.isLocked(m))

    /** ... on a chain: "Delete|Duplicate|Lock|Lock all chains|Cancel" (ct52 N, v52 slice 11). */
    fun menuItems(tx: Tx, p: SurveyPrep.Prep, s: String, c: SurveyPrep.Chain): List<Pair<String, String>> =
        menu(tx, lockGroup(p, s, c), SurveyPrep.isLocked(c))

    /** The menu for any item by its id; empty when there is none. The reference's is SurveyRef's (port 61c slice M3b). */
    fun menuItemsOf(tx: Tx, p: SurveyPrep.Prep, id: String): List<Pair<String, String>> =
        SurveyRef.byId(p, id)?.let { (_, r) -> SurveyRef.menuItems(tx, r.locked) } ?: p.markById(id)?.let { (s, e) -> menuItems(tx, p, s, e) } ?: p.measById(id)?.let { (s, m) -> menuItems(tx, p, s, m) }
            ?: p.chainById(id)?.let { (s, c) -> menuItems(tx, p, s, c) } ?: emptyList()

    /**
     * THE MENU AS THE PHASE LEAVES IT (port 62 slice L4; the prototype's spPost, index61c2.html 4262-4263): a row
     * that would change an item the phase holds read-only is HIDDEN, never greyed -- Delete (or Drop / Restore) only
     * where its delete control shows ([PhaseTools.delOk]); Lock / Unlock only where its padlock does
     * ([PhaseTools.lockOk]); Lock all only where the item may be edited ([PhaseTools.editable]). Cancel
     * is always there, so on a surveyed mark outside Survey the menu is its head and Cancel, as the prototype's is.
     * The reference rectangle's menu is never cut: its padlock is not the phase's, and the rules answer true
     * for an id that is no item. (Lock all, where it shows outside Survey, answers [PhaseTools.refuseBulk] -- the
     * prototype's own: it draws the row for a mark the phase lets be edited and refuses the act. DESIGN-port62 Q-list 11.)
     *
     * DUPLICATE ONLY WHERE THE COPY MAY BE MADE ([PhaseTools.copyOk]; the L4 review, finding 1): a Tape, a Line, an
     * area and a chain wherever they may be edited, as the prototype's menu has it; A MARK only where its copy -- the
     * box alone, an Existing mark -- is what a tap would place: at Survey, and while the stage is unread with this
     * capture's chip on "as Existing" ([picked], which has no default). The prototype draws the row on a mark born in
     * the phase at the Wish list, Vendor review, Revisions and Client approved as well; there the copy of a thing that
     * is not in the room would leave the office as a surveyed one (DESIGN-port62 Q-list 8).
     */
    fun menuItemsOf(tx: Tx, p: SurveyPrep.Prep, id: String, stage: Int?, picked: String?): List<Pair<String, String>> =
        menuItemsOf(tx, p, id).filter { (k, _) ->
            when (k) {
                DELETE -> PhaseTools.delOk(p, id, stage)
                LOCK -> PhaseTools.lockOk(p, id, stage)
                DUPLICATE -> PhaseTools.copyOk(p, id, stage, picked)
                LOCK_ALL -> PhaseTools.editable(p, id, stage)
                else -> true
            }
        }

    /** "Lock" / "Unlock" and "Lock all" / "Unlock all" are chosen in code, so each is its own whole key (the card's group 2).
     *  [delete]: the first row's word -- Delete, or a mark's own. */
    private fun menu(tx: Tx, g: Group, locked: Boolean, delete: String = tx.common_delete) =
        listOf(DELETE to delete, DUPLICATE to tx.common_duplicate, LOCK to if (locked) tx.common_unlock else tx.common_lock,
            LOCK_ALL to (if (g.all) tx.sp_unlock_all_x(groupName(tx, g)) else tx.sp_lock_all_x(groupName(tx, g))), CANCEL to tx.common_cancel)

    // ------------------------------------------------------------ Duplicate

    /** How far a duplicate sits from its original, mm, across and up. */
    const val DUP_MM = 100.0

    /**
     * DUPLICATE (sp.js duplicateItem): a copy of mark [id] [DUP_MM] right and up, or back the other way where
     * that would leave the [a] x [b] face, never past its edge; the same type and size; the NEXT code of its
     * type (codes are never reused: [SurveyPrep.Prep.claimCode]); NO READINGS -- it is another thing on the
     * wall, measured on its own; and unlocked, even when the original is locked. Null when there is no such mark.
     *
     * THE COPY IS THE BOX ALONE (port 62 slice L4; index61c2.html 5633-5635): the prototype's duplicateItem builds
     * `{id, t, code, x0, x1, y0, y1, locked: false}` and nothing more -- no `born`, `life`, act, note, sub-kind or
     * trade -- so the copy of a wish is an EXISTING mark, which every phase after Survey then holds read-only like a
     * surveyed one. That is what the page DOES (its own comment says otherwise); whether a copy should be born what
     * the phase places is DESIGN-port62 Q-list 8, and until Amit rules nothing is stamped here.
     *
     * WHERE A COPY OF A MARK MAY BE MADE AT ALL is [PhaseTools.copyOk], asked by the menu and by the screen before
     * this (the L4 review, finding 1): only where this unstamped mark is what a tap would place -- an unstamped mark
     * is read as IN THE ROOM by the exported sheet ([SheetMarks.surveyed]) and by S5's answer to a vendor or a client,
     * so the copy of a wish made in the Wish list would have left the office as a surveyed thing.
     */
    fun duplicate(p: SurveyPrep.Prep, id: String, a: Double, b: Double, newId: String): SurveyPrep.Mark? {
        val (s, e) = p.markById(id) ?: return null
        val w = e.x1 - e.x0; val h = e.y1 - e.y0
        var x0 = e.x0 + DUP_MM; var y0 = e.y0 + DUP_MM
        if (x0 + w > a) x0 = max(0.0, e.x0 - DUP_MM)
        if (y0 + h > b) y0 = max(0.0, e.y0 - DUP_MM)
        val c = SurveyPrep.Mark(newId, e.t, p.claimCode(s, e.t), x0, x0 + w, y0, y0 + h)
        p.surface(s).marks.add(c)
        return c
    }

    /** DUPLICATE A CHAIN (v52 slice 11): see SurveyChain.duplicate -- ±100 mm across its axis, the next K number. */
    fun duplicateChain(p: SurveyPrep.Prep, id: String, a: Double, b: Double, newId: String): SurveyPrep.Chain? =
        SurveyChain.duplicate(p, id, a, b, newId)

    /**
     * DUPLICATE A TAPE, A LINE OR AN AREA (sp.js duplicateItem, v52 slice 9): every point [DUP_MM] right and up -- or
     * back the other way where its furthest point would pass the [a] x [b] face's edge -- and kept on the face; the same
     * kind; the NEXT code of ITS OWN prefix (F-T1 with F-T2 there makes F-T3; the Line numbering is untouched); no
     * readings; unlocked. Null when there is no such item.
     */
    fun duplicateMeas(p: SurveyPrep.Prep, id: String, a: Double, b: Double, newId: String): SurveyPrep.Meas? {
        val (s, m) = p.measById(id) ?: return null
        if (m.pts.isEmpty()) return null
        val dx = if (m.pts.maxOf { it[0] } + DUP_MM <= a) DUP_MM else -DUP_MM
        val dy = if (m.pts.maxOf { it[1] } + DUP_MM <= b) DUP_MM else -DUP_MM
        return p.addMeas(s, m.kind, m.pts.map { doubleArrayOf((it[0] + dx).coerceIn(0.0, a), (it[1] + dy).coerceIn(0.0, b)) }, newId)
    }

    // ------------------------------------------------------------ the clears

    const val SURF = "surf"
    const val ROOM = "room"

    /** The surfaces a clear walks: the open face for [SURF] (none on the List tab), all six for [ROOM]. */
    fun clearList(scope: String, tab: String): List<String> =
        if (scope == ROOM) SurveyPrep.SURFACES else if (tab in SurveyPrep.SURFACES) listOf(tab) else emptyList()

    /** What a clear would take -- never a locked item (sp.js clearCount), and never a mark past Existing (port 62 slice
     *  L2; the prototype's spared, index61c2.html 5651): a wish is dropped one by one, with its why. */
    fun clearCount(p: SurveyPrep.Prep, scope: String, tab: String): Int =
        clearList(scope, tab).sumOf { s -> p.itemsOf(s).count { !it.spared } }

    /** What a clear would keep because it is locked (sp.js lockedCount). */
    fun lockedCount(p: SurveyPrep.Prep, scope: String, tab: String): Int =
        clearList(scope, tab).sumOf { s -> p.itemsOf(s).count { it.locked } }

    /** What a clear would keep because it is a mark past Existing, the UNLOCKED ones -- a locked one is counted by
     *  [lockedCount] (the prototype's pastCount, index61c2.html 5652). */
    fun pastCount(p: SurveyPrep.Prep, scope: String, tab: String): Int =
        clearList(scope, tab).sumOf { s -> p.itemsOf(s).count { !it.locked && it.past } }

    /** What a clear did: [n] items taken, [kept] locked ones left, the codes taken, and [past] unlocked marks past
     *  Existing left ([clearedSnack] says which line the snackbar shows). */
    class Cleared(val n: Int, val kept: Int, val codes: List<String>, val past: Int = 0)

    /**
     * CLEAR (sp.js clearItems): every UNLOCKED item on the surfaces of [scope] goes, with its readings (every
     * key starting with its code and a dot); every locked item stays, with its readings -- and so does every mark past
     * Existing, locked or not, with its readings (port 62 slice L2). In place; the caller takes ONE undo snapshot
     * first. Nothing changes when there is nothing to clear ([Cleared.n] 0), a surface of wishes alone included.
     */
    fun clearItems(p: SurveyPrep.Prep, scope: String, tab: String): Cleared {
        val list = clearList(scope, tab)
        val n = clearCount(p, scope, tab); val kept = lockedCount(p, scope, tab); val past = pastCount(p, scope, tab)
        if (n == 0) return Cleared(0, kept, emptyList(), past)
        val codes = list.flatMap { s -> p.itemsOf(s).filter { !it.spared }.map { it.code } }
        for (s in list) p.dropUnlocked(s)
        p.actual.keys.removeAll { k -> codes.any { k.startsWith("$it.") } }
        return Cleared(n, kept, codes, past)
    }

    // ------------------------------------------------------------ Start over

    /**
     * The surface keys Start over carries across besides the locked lines and the code counter: the reference
     * rectangle (v52 slice 12; sp.js 1687: "the reference always survives Start over, shown or hidden, locked or
     * not" -- Amit, 14 Oct 2026 06:45 IST: "can not be deleted"), with its corners, its W × H and its eye.
     */
    val START_OVER_KEEPS: Set<String> = setOf(SurveyRef.KEY)

    /** What Start over made: the new prep, how many locked things it kept (a surface's lines count one), and how many
     *  UNLOCKED marks past Existing it kept besides ([past]; port 62 slice L2). [startedOver] has the words. */
    class Restarted(val prep: SurveyPrep.Prep, val kept: Int, val past: Int = 0)

    /**
     * START OVER (sp.js 1683-1690, v50): every surface back to [SurveyPrep.fresh] at the room size [l] x [w] x
     * [h] -- EXCEPT a surface's four lines when they are locked (kept, with which were set, still locked),
     * every locked item and its readings. Every other line, mark and reading goes. The code counter
     * ([SurveyPrep.SEQ]) is kept, so a code is not handed out again after Start over either; and the file's
     * keys this build does not read at the top level and for unknown surfaces are kept as they were.
     * A MARK PAST EXISTING IS KEPT TOO, locked or not, where it was and with its readings (port 62 slice L2; the
     * prototype's spared, index61c2.html 6940): a wish is dropped one by one, never swept away.
     * Pure: [p] is not changed.
     */
    fun startOver(p: SurveyPrep.Prep, l: Double, w: Double, h: Double): Restarted {
        val f = SurveyPrep.fresh(l, w, h)
        val out = SurveyPrep.Prep(f.surfaces, mutableMapOf(), p.extra, p.extraSurfaces)
        var kept = 0; var past = 0
        for (s in SurveyPrep.SURFACES) {
            val old0 = p.surfaces[s] ?: continue
            val old = old0.copy()
            val sf = out.surface(s)
            val ex = LinkedHashMap<String, Any?>()
            if (SurveyPrep.linesLocked(old)) {
                for (k in SurveyPrep.SLOTS) old.lines[k]?.let { sf.lines[k] = it }
                for (k in SurveyPrep.SLOTS) sf.set[k] = old.set[k] == true
                ex[SurveyPrep.LINES_LOCK] = true
                kept++
            }
            for (k in START_OVER_KEEPS) if (old.extra.containsKey(k)) ex[k] = old.extra[k]
            // a LOCKED reference counts among what was kept (sp.js 1687: `if(o.ref.locked) kept++`)
            if (SurveyRef.of(old)?.locked == true) kept++
            // the codes of everything that goes are counted first, then the counter goes across
            val gone = old.marks.filter { !MarkLife.spared(it) }
            val probe = SurveyPrep.Prep(mutableMapOf(s to old), mutableMapOf())
            probe.recordCodes(s, gone)
            probe.recordTypes(s, old.meas.filter { !SurveyPrep.isLocked(it) }.map { SurveyMeas.seqKey(it.kind) })
            probe.recordTypes(s, old.chains.filter { !SurveyPrep.isLocked(it) }.map { SurveyChain.LETTER })
            old.extra[SurveyPrep.SEQ]?.let { ex[SurveyPrep.SEQ] = it }
            sf.extra = ex
            // every locked mark, and every mark past Existing whether locked or not, in the surface's own order
            val keep = old.marks.filter { MarkLife.spared(it) }
            sf.marks.addAll(keep)
            kept += keep.count { SurveyPrep.isLocked(it) }
            past += keep.count { !SurveyPrep.isLocked(it) }
            for (e in keep) for ((k, v) in p.actual) if (k.startsWith(e.code + ".")) out.actual[k] = v
            // a locked Tape, Line or area stays too, with its reading (v52 slice 9)
            val keepM = old.meas.filter { SurveyPrep.isLocked(it) }
            sf.meas.addAll(keepM)
            sf.measKey = old.measKey
            kept += keepM.size
            for (m in keepM) for ((k, v) in p.actual) if (k.startsWith(m.code + ".")) out.actual[k] = v
            // a locked chain stays too, with its segments' and its total's readings (v52 slice 11)
            val keepC = old.chains.filter { SurveyPrep.isLocked(it) }
            sf.chains.addAll(keepC)
            sf.chainsKey = old.chainsKey
            kept += keepC.size
            for (c in keepC) for ((k, v) in p.actual) if (k.startsWith(c.code + ".")) out.actual[k] = v
        }
        return Restarted(out, kept, past)
    }

    /**
     * ↶ (sp.js undo): the older prep [old] put back -- its geometry -- with the readings of NOW and of then merged, now
     * winning, and kept only where their owner is there again (a mark, a Tape / Line / area, or a chain: FK1.2 stays
     * with FK1, v52 slice 11); and every code counter raised to what [cur] handed out ([keepSeq]). Readings are not
     * geometry: one taken since stays, and one a delete took away comes back with its owner. Pure: neither is changed.
     */
    fun undoTo(cur: SurveyPrep.Prep, old: SurveyPrep.Prep): SurveyPrep.Prep {
        val merged = old.actual.toMutableMap().apply { putAll(cur.actual) }
        val o = old.copy()
        val back = SurveyPrep.Prep(o.surfaces, mutableMapOf(), o.extra, o.extraSurfaces)
        for ((k, v) in merged) if (back.ownerOf(k) != null) back.actual[k] = v
        keepSeq(cur, back)
        return back
    }

    /**
     * After ↶ puts an older prep back: every surface's code counter is raised to what [from] (the prep being
     * replaced) has handed out, so a code given to a mark that ↶ took away is not handed out again -- the web
     * may already hold readings under it. Only ever raises; [into] is changed in place.
     */
    fun keepSeq(from: SurveyPrep.Prep, into: SurveyPrep.Prep) {
        for (s in SurveyPrep.SURFACES) {
            val a = from.surfaces[s] ?: continue
            val b = into.surfaces[s] ?: continue
            val types = (a.marks.map { it.t } + a.meas.map { SurveyMeas.seqKey(it.kind) } + a.chains.map { SurveyChain.LETTER } +
                ((a.extra[SurveyPrep.SEQ] as? Map<*, *>)?.keys?.filterIsInstance<String>() ?: emptyList())).distinct()
            val seq = LinkedHashMap<String, Any?>().apply { (b.extra[SurveyPrep.SEQ] as? Map<*, *>)?.forEach { (k, v) -> if (k is String) put(k, v) } }
            var any = false
            for (t in types) {
                val n = from.highest(s, t)
                if (n > into.highest(s, t)) { seq[t] = n; any = true }
            }
            if (any) b.extra = SurveyPrep.withKey(b.extra, SurveyPrep.SEQ, seq)
        }
    }
}
