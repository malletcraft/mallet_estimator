package com.malletcrafts.sitephotos.pano

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
 */
object SurveyPrepEdit {

    // ------------------------------------------------------------ the words (sp.js, exactly)

    /** Every refused move, resize or delete of a locked mark (sp.js refuse). */
    fun refusal(code: String) = "$code is locked — unlock first"
    /** A drag or a nudge on a surface whose lines are locked (sp.js 949, 1064). */
    const val LINES_REFUSAL = "Lines are locked — unlock first"
    /** A delete, with UNDO. */
    fun deleted(code: String) = "$code deleted"
    /** The lines' padlock tapped (sp.js setLock 'lines:'). */
    fun linesSaid(s: String, on: Boolean) = "${SurveyPrep.NAME.getValue(s)} lines ${if (on) "locked" else "unlocked"}"
    /** The lines' padlock's own label: its state (sp.js .lkl span). */
    fun linesState(on: Boolean) = if (on) "Locked" else "Unlocked"
    /** ... and what a tap on it does, for the screen reader. */
    fun linesAction(on: Boolean) = if (on) "Unlock lines" else "Lock lines"

    private fun items(n: Int) = "$n item${if (n == 1) "" else "s"}"

    /** Lock all / Unlock all, counting what it CHANGED (ct52: "Locked 1 item · front wall"). */
    fun lockedSnack(on: Boolean, n: Int, s: String) =
        "${if (on) "Locked" else "Unlocked"} ${items(n)} · ${SurveyPrep.NAME.getValue(s).lowercase()}"

    const val START_OVER = "Start over"
    const val START_OVER_ARMED = "Tap again to clear every line, mark and reading"
    /** The note after Start over, when it kept anything; null when it kept nothing (sp.js 1690). */
    fun startedOver(kept: Int): String? = if (kept > 0) "Started over — $kept locked kept" else null

    /** The snackbar after a clear (sp.js 850). */
    fun clearedSnack(n: Int, kept: Int) = if (kept > 0) "Cleared ${items(n)}, $kept locked kept" else "${items(n)} cleared"

    /**
     * A clear button's text in ⋮ (sp.js 1473-1477): armed, "Tap again to clear N items"; with something to
     * clear, "Clear this surface… (N)" / "Clear whole room… (N)"; with nothing but locked items, "K locked —
     * nothing to clear"; with nothing at all, "Nothing on this surface to clear" / "Nothing in the room to clear".
     */
    fun clearLabel(scope: String, n: Int, locked: Int, armed: Boolean): String = when {
        armed && n > 0 -> "Tap again to clear ${items(n)}"
        n > 0 -> if (scope == ROOM) "Clear whole room… ($n)" else "Clear this surface… ($n)"
        locked > 0 -> "$locked locked — nothing to clear"
        else -> if (scope == ROOM) "Nothing in the room to clear" else "Nothing on this surface to clear"
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
     * such mark. The lock replaces the mark's extra map (an undo copy may share the old one).
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
        if (SurveyPrep.isLocked(e) != v) e.extra = SurveyPrep.withKey(e.extra, SurveyPrep.LOCKED, v)
        return v
    }

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
     * snackbar counts (sp.js lockAll).
     */
    fun lockAll(p: SurveyPrep.Prep, s: String, t: String?, on: Boolean, kind: String? = null): Int {
        var n = 0
        val sf = p.surface(s)
        if (kind == null || kind == KIND_MARK || t != null) for (e in sf.marks) {
            if (t != null && e.t != t) continue
            if (SurveyPrep.isLocked(e) == on) continue
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

    /** The group a long press can lock at once: every mark of [e]'s type on [s] (sp.js lockGroup) -- or, for a Tape,
     *  every tape there ([kind] [KIND_TAPE], "tapes"), for a Line every Line and area ([KIND_LINE], "Line marks").
     *  [all]: every one of them is locked already, so the menu offers "Unlock all <name>". [t]: the mark type, or null. */
    class Group(val t: String?, val name: String, val all: Boolean, val n: Int, val kind: String? = null)

    fun lockGroup(p: SurveyPrep.Prep, s: String, e: SurveyPrep.Mark): Group {
        val l = p.surface(s).marks.filter { it.t == e.t }
        return Group(e.t, SurveyPrep.typeOf(s, e.t).name, l.isNotEmpty() && l.all { SurveyPrep.isLocked(it) }, l.size)
    }

    fun lockGroup(p: SurveyPrep.Prep, s: String, m: SurveyPrep.Meas): Group {
        val tape = m.kind == SurveyMeas.TAPE
        val l = p.surface(s).meas.filter { (it.kind == SurveyMeas.TAPE) == tape }
        return Group(null, if (tape) "tapes" else "Line marks", l.isNotEmpty() && l.all { SurveyPrep.isLocked(it) }, l.size,
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
     *  "F-T1 tape", "F-M2 marked area". The trash's screen-reader label is "Delete " and this. */
    fun labelOf(p: SurveyPrep.Prep, id: String): String? {
        p.markById(id)?.let { (s, e) -> return e.code + " " + SurveyPrep.typeOf(s, e.t).name }
        p.measById(id)?.let { (_, m) -> return SurveyMeas.label(m) }
        p.chainById(id)?.let { (_, c) -> return SurveyChain.label(c) }
        return null
    }

    /** The trash's label: "Delete F-M1 marked length", "Delete FK1 chain" (ct52 FX9). */
    fun deleteLabel(p: SurveyPrep.Prep, id: String): String? = labelOf(p, id)?.let { "Delete $it" }

    // ------------------------------------------------------------ the long-press menu

    const val DELETE = "del"
    const val DUPLICATE = "dup"
    const val LOCK = "lock"
    const val LOCK_ALL = "lockall"
    const val CANCEL = "cancel"

    /** The menu's head: "FD1 Door", "🔒 FD1 Door" when locked (sp.js 1469). */
    fun menuHeader(s: String, e: SurveyPrep.Mark): String =
        (if (SurveyPrep.isLocked(e)) "🔒 " else "") + e.code + " " + SurveyPrep.typeOf(s, e.t).name

    /** ... on a Tape or a Line: "F-T1 tape", "🔒 F-M1 marked length". */
    fun menuHeader(m: SurveyPrep.Meas): String = (if (SurveyPrep.isLocked(m)) "🔒 " else "") + SurveyMeas.label(m)

    /** ... on a chain: "FK1 chain", "🔒 FK1 chain" (v52 slice 11). */
    fun menuHeader(c: SurveyPrep.Chain): String = (if (SurveyPrep.isLocked(c)) "🔒 " else "") + SurveyChain.label(c)

    /** The head for any item by its id; null when there is none. */
    fun menuHeaderOf(p: SurveyPrep.Prep, id: String): String? =
        SurveyRef.byId(p, id)?.let { (s, r) -> SurveyRef.menuHeader(s, r.locked) } ?: p.markById(id)?.let { (s, e) -> menuHeader(s, e) } ?: p.measById(id)?.let { (_, m) -> menuHeader(m) }
            ?: p.chainById(id)?.let { (_, c) -> menuHeader(c) }

    /** The menu on a mark, in order, as (action, text): Delete · Duplicate · Lock|Unlock · Lock all X|Unlock all X · Cancel
     *  (ct52: "Delete|Duplicate|Lock|Lock all Door|Cancel"). */
    fun menuItems(p: SurveyPrep.Prep, s: String, e: SurveyPrep.Mark): List<Pair<String, String>> =
        menu(lockGroup(p, s, e), SurveyPrep.isLocked(e))

    /** ... on a Tape ("Lock all tapes") or a Line ("Lock all Line marks"): ct52 T, "Delete|Duplicate|Lock|Lock all tapes|Cancel". */
    fun menuItems(p: SurveyPrep.Prep, s: String, m: SurveyPrep.Meas): List<Pair<String, String>> =
        menu(lockGroup(p, s, m), SurveyPrep.isLocked(m))

    /** ... on a chain: "Delete|Duplicate|Lock|Lock all chains|Cancel" (ct52 N, v52 slice 11). */
    fun menuItems(p: SurveyPrep.Prep, s: String, c: SurveyPrep.Chain): List<Pair<String, String>> =
        menu(lockGroup(p, s, c), SurveyPrep.isLocked(c))

    /** The menu for any item by its id; empty when there is none. */
    fun menuItemsOf(p: SurveyPrep.Prep, id: String): List<Pair<String, String>> =
        SurveyRef.byId(p, id)?.let { (_, r) -> SurveyRef.menuItems(r.locked) } ?: p.markById(id)?.let { (s, e) -> menuItems(p, s, e) } ?: p.measById(id)?.let { (s, m) -> menuItems(p, s, m) }
            ?: p.chainById(id)?.let { (s, c) -> menuItems(p, s, c) } ?: emptyList()

    private fun menu(g: Group, locked: Boolean) =
        listOf(DELETE to "Delete", DUPLICATE to "Duplicate", LOCK to if (locked) "Unlock" else "Lock",
            LOCK_ALL to "${if (g.all) "Unlock" else "Lock"} all ${g.name}", CANCEL to "Cancel")

    // ------------------------------------------------------------ Duplicate

    /** How far a duplicate sits from its original, mm, across and up. */
    const val DUP_MM = 100.0

    /**
     * DUPLICATE (sp.js duplicateItem): a copy of mark [id] [DUP_MM] right and up, or back the other way where
     * that would leave the [a] x [b] face, never past its edge; the same type and size; the NEXT code of its
     * type (codes are never reused: [SurveyPrep.Prep.claimCode]); NO READINGS -- it is another thing on the
     * wall, measured on its own; and unlocked, even when the original is locked. Null when there is no such mark.
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

    /** What a clear would take -- never a locked item (sp.js clearCount). */
    fun clearCount(p: SurveyPrep.Prep, scope: String, tab: String): Int =
        clearList(scope, tab).sumOf { s -> p.itemsOf(s).count { !it.locked } }

    /** What a clear would keep because it is locked (sp.js lockedCount). */
    fun lockedCount(p: SurveyPrep.Prep, scope: String, tab: String): Int =
        clearList(scope, tab).sumOf { s -> p.itemsOf(s).count { it.locked } }

    /** What a clear did: [n] items taken, [kept] locked ones left, the codes taken. */
    class Cleared(val n: Int, val kept: Int, val codes: List<String>)

    /**
     * CLEAR (sp.js clearItems): every UNLOCKED item on the surfaces of [scope] goes, with its readings (every
     * key starting with its code and a dot); every locked item stays, with its readings. In place; the caller
     * takes ONE undo snapshot first. Nothing changes when there is nothing to clear ([Cleared.n] 0).
     */
    fun clearItems(p: SurveyPrep.Prep, scope: String, tab: String): Cleared {
        val list = clearList(scope, tab)
        val n = clearCount(p, scope, tab); val kept = lockedCount(p, scope, tab)
        if (n == 0) return Cleared(0, kept, emptyList())
        val codes = list.flatMap { s -> p.itemsOf(s).filter { !it.locked }.map { it.code } }
        for (s in list) p.dropUnlocked(s)
        p.actual.keys.removeAll { k -> codes.any { k.startsWith("$it.") } }
        return Cleared(n, kept, codes)
    }

    // ------------------------------------------------------------ Start over

    /**
     * The surface keys Start over carries across besides the locked lines and the code counter: the reference
     * rectangle (v52 slice 12; sp.js 1687: "the reference always survives Start over, shown or hidden, locked or
     * not" -- Amit, 14 Oct 2026 06:45 IST: "can not be deleted"), with its corners, its W × H and its eye.
     */
    val START_OVER_KEEPS: Set<String> = setOf(SurveyRef.KEY)

    /** What Start over made: the new prep, and how many locked things it kept (a surface's lines count one). */
    class Restarted(val prep: SurveyPrep.Prep, val kept: Int)

    /**
     * START OVER (sp.js 1683-1690, v50): every surface back to [SurveyPrep.fresh] at the room size [l] x [w] x
     * [h] -- EXCEPT a surface's four lines when they are locked (kept, with which were set, still locked),
     * every locked item and its readings. Every other line, mark and reading goes. The code counter
     * ([SurveyPrep.SEQ]) is kept, so a code is not handed out again after Start over either; and the file's
     * keys this build does not read at the top level and for unknown surfaces are kept as they were.
     * Pure: [p] is not changed.
     */
    fun startOver(p: SurveyPrep.Prep, l: Double, w: Double, h: Double): Restarted {
        val f = SurveyPrep.fresh(l, w, h)
        val out = SurveyPrep.Prep(f.surfaces, mutableMapOf(), p.extra, p.extraSurfaces)
        var kept = 0
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
            val gone = old.marks.filter { !SurveyPrep.isLocked(it) }
            val probe = SurveyPrep.Prep(mutableMapOf(s to old), mutableMapOf())
            probe.recordCodes(s, gone)
            probe.recordTypes(s, old.meas.filter { !SurveyPrep.isLocked(it) }.map { SurveyMeas.seqKey(it.kind) })
            probe.recordTypes(s, old.chains.filter { !SurveyPrep.isLocked(it) }.map { SurveyChain.LETTER })
            old.extra[SurveyPrep.SEQ]?.let { ex[SurveyPrep.SEQ] = it }
            sf.extra = ex
            val keep = old.marks.filter { SurveyPrep.isLocked(it) }
            sf.marks.addAll(keep)
            kept += keep.size
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
        return Restarted(out, kept)
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
