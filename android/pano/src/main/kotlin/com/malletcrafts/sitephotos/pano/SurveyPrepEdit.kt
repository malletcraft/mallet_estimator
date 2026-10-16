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
 * Everything walks [SurveyPrep.Prep.itemsOf], so the Line, Tape (slice 9) and chains (slice 11) join a clear,
 * a count and Start over by joining that list, with no change here.
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
        val e = p.markById(id)?.second ?: return null
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

    /**
     * LOCK ALL: every item on [s] of type [t] (a mark type's letter), or of [kind] ("mark"; "meas", "tape" and
     * "chain" join with their slices), or EVERY item there when both are null, locked ([on]) or unlocked.
     * Returns how many CHANGED -- what the snackbar counts (sp.js lockAll).
     */
    fun lockAll(p: SurveyPrep.Prep, s: String, t: String?, on: Boolean, kind: String? = null): Int {
        var n = 0
        for (e in p.surface(s).marks) {
            if (t != null && e.t != t) continue
            if (kind != null && kind != "mark") continue
            if (SurveyPrep.isLocked(e) == on) continue
            e.extra = SurveyPrep.withKey(e.extra, SurveyPrep.LOCKED, on); n++
        }
        return n
    }

    /** The group a long press can lock at once: every mark of [e]'s type on [s] (sp.js lockGroup).
     *  [all]: every one of them is locked already, so the menu offers "Unlock all <name>". */
    class Group(val t: String, val name: String, val all: Boolean, val n: Int)

    fun lockGroup(p: SurveyPrep.Prep, s: String, e: SurveyPrep.Mark): Group {
        val l = p.surface(s).marks.filter { it.t == e.t }
        return Group(e.t, SurveyPrep.typeOf(s, e.t).name, l.isNotEmpty() && l.all { SurveyPrep.isLocked(it) }, l.size)
    }

    // ------------------------------------------------------------ the long-press menu

    const val DELETE = "del"
    const val DUPLICATE = "dup"
    const val LOCK = "lock"
    const val LOCK_ALL = "lockall"
    const val CANCEL = "cancel"

    /** The menu's head: "FD1 Door", "🔒 FD1 Door" when locked (sp.js 1469). */
    fun menuHeader(s: String, e: SurveyPrep.Mark): String =
        (if (SurveyPrep.isLocked(e)) "🔒 " else "") + e.code + " " + SurveyPrep.typeOf(s, e.t).name

    /** The menu on a mark, in order, as (action, text): Delete · Duplicate · Lock|Unlock · Lock all X|Unlock all X · Cancel
     *  (ct52: "Delete|Duplicate|Lock|Lock all Door|Cancel"). */
    fun menuItems(p: SurveyPrep.Prep, s: String, e: SurveyPrep.Mark): List<Pair<String, String>> {
        val g = lockGroup(p, s, e)
        return listOf(DELETE to "Delete", DUPLICATE to "Duplicate", LOCK to if (SurveyPrep.isLocked(e)) "Unlock" else "Lock",
            LOCK_ALL to "${if (g.all) "Unlock" else "Lock"} all ${g.name}", CANCEL to "Cancel")
    }

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
     * The surface keys Start over carries across besides the locked lines and the code counter. The
     * reference rectangle joins here in slice 12 ("ref": sp.js 1687 keeps it always, locked or not).
     */
    val START_OVER_KEEPS: Set<String> = emptySet()

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
            // the codes of everything that goes are counted first, then the counter goes across
            val gone = old.marks.filter { !SurveyPrep.isLocked(it) }
            val probe = SurveyPrep.Prep(mutableMapOf(s to old), mutableMapOf())
            probe.recordCodes(s, gone)
            old.extra[SurveyPrep.SEQ]?.let { ex[SurveyPrep.SEQ] = it }
            sf.extra = ex
            val keep = old.marks.filter { SurveyPrep.isLocked(it) }
            sf.marks.addAll(keep)
            kept += keep.size
            for (e in keep) for ((k, v) in p.actual) if (k.startsWith(e.code + ".")) out.actual[k] = v
        }
        return Restarted(out, kept)
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
            val types = (a.marks.map { it.t } + ((a.extra[SurveyPrep.SEQ] as? Map<*, *>)?.keys?.filterIsInstance<String>() ?: emptyList())).distinct()
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
