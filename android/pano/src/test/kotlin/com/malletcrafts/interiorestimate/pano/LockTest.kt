package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * LOCKS (v52 slice 8; Amit, 14 Oct 2026 02:39 IST: "Give locking lines for reference and outer box lines so it
 * won't move accidentally. In fact each mark type should also be locked."). A locked mark does not move, resize
 * or delete; Lock all counts what it changed; the lines' lock is the surface's own. The synthetic room: L 3319,
 * W 2678, H 2677.
 */
class LockTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0

    private fun room(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        p.addMarkAt("front", "D", 600.0, 1050.0, W, H, "d1")
        p.addMarkAt("front", "D", 2000.0, 1050.0, W, H, "d2")
        p.addMarkAt("front", "S", 1300.0, 1100.0, W, H, "s1")
        p.addMarkAt("front", "S", 1700.0, 1100.0, W, H, "s2")
        p.addMarkAt("front", "E", 300.0, 300.0, W, H, "e1")
        p.addMarkAt("right", "D", 600.0, 1050.0, L, H, "r1")
        return p
    }

    private fun mark(p: SurveyPrep.Prep, id: String) = p.markById(id)!!.second

    @Test fun `lock, unlock and toggle a mark, a lock replaces its extra map, never changes it in place`() {
        val p = room(); val e = mark(p, "d1")
        assertFalse(SurveyPrep.isLocked(e))
        val before = p.copy()
        assertEquals(true, SurveyPrepEdit.setLocked(p, "d1", true))
        assertTrue(SurveyPrep.isLocked(e)); assertEquals(true, e.extra[SurveyPrep.LOCKED])
        assertFalse(SurveyPrep.isLocked(before.markById("d1")!!.second), "an undo copy taken before keeps the old state")
        assertEquals(false, SurveyPrepEdit.setLocked(p, "d1", null)); assertFalse(SurveyPrep.isLocked(e))
        assertEquals(true, SurveyPrepEdit.setLocked(p, "d1", null)); assertTrue(SurveyPrep.isLocked(e))
        assertEquals(false, SurveyPrepEdit.setLocked(p, "d1", false)); assertFalse(SurveyPrep.isLocked(e))
        assertNull(SurveyPrepEdit.setLocked(p, "nope", true))
    }

    @Test fun `a locked flag read from a file as a string still locks`() {
        val e = SurveyPrep.Mark("x", "D", "FD9", 0.0, 900.0, 0.0, 2100.0, mapOf("locked" to "true"))
        assertTrue(SurveyPrep.isLocked(e))
        assertFalse(SurveyPrep.isLocked(SurveyPrep.Mark("y", "D", "FD8", 0.0, 900.0, 0.0, 2100.0, mapOf("locked" to false))))
    }

    @Test fun `a locked mark refuses move, resize and snap in the pure move functions`() {
        val p = room(); val e = mark(p, "d1"); SurveyPrepEdit.setLocked(p, "d1", true)
        val was = listOf(e.x0, e.x1, e.y0, e.y1)
        for (part in listOf("body", "l", "r", "t", "b", "lt", "rb")) {
            assertFalse(SurveyPrep.moveMark(e, e.copy(), part, 37.0, -12.0, W, H), part)
            assertEquals(was, listOf(e.x0, e.x1, e.y0, e.y1), part)
        }
        e.x0 = 10.0; e.x1 = 910.0          // as if near the edge: a locked mark is not snapped onto it either
        SurveyPrep.snapMark(e, "body", W, H)
        assertEquals(10.0, e.x0)
        SurveyPrepEdit.setLocked(p, "d1", false)
        assertTrue(SurveyPrep.moveMark(e, e.copy(), "body", 100.0, 0.0, W, H)); assertEquals(110.0, e.x0)
    }

    @Test fun `a locked mark refuses delete, and keeps its readings, unlocked, it goes with them`() {
        val p = room(); val e = mark(p, "d1"); p.actual["${e.code}.w"] = 905.0
        SurveyPrepEdit.setLocked(p, "d1", true)
        assertFalse(p.deleteMark("d1"))
        assertEquals(e, p.markById("d1")?.second); assertEquals(905.0, p.actual["${e.code}.w"])
        SurveyPrepEdit.setLocked(p, "d1", false)
        assertTrue(p.deleteMark("d1"))
        assertNull(p.markById("d1")); assertNull(p.actual["${e.code}.w"])
    }

    @Test fun `Lock all counts what it changed, per type`() {
        val p = room()
        SurveyPrepEdit.setLocked(p, "s1", true)
        // two Switch plates, one already locked: Lock all Switch plate changes one
        assertEquals(1, SurveyPrepEdit.lockAll(p, "front", "S", true))
        assertTrue(SurveyPrep.isLocked(mark(p, "s1")) && SurveyPrep.isLocked(mark(p, "s2")))
        assertFalse(SurveyPrep.isLocked(mark(p, "d1")) || SurveyPrep.isLocked(mark(p, "e1")), "nothing of another type")
        assertFalse(SurveyPrep.isLocked(mark(p, "r1")), "nothing on another surface")
        assertEquals(0, SurveyPrepEdit.lockAll(p, "front", "S", true), "again: nothing changes")
        assertEquals(2, SurveyPrepEdit.lockAll(p, "front", "S", false))
        assertEquals("Locked 1 item · front wall", SurveyPrepEdit.lockedSnack(Tx.EN, true, 1, "front"))
        assertEquals("Unlocked 2 items · front wall", SurveyPrepEdit.lockedSnack(Tx.EN, false, 2, "front"))
    }

    @Test fun `Lock all per kind, and every item on a surface`() {
        val p = room()
        assertEquals(5, SurveyPrepEdit.lockAll(p, "front", null, true, kind = "mark"))
        assertEquals(0, SurveyPrepEdit.lockAll(p, "front", null, true, kind = "chain"), "no chains on this room")
        assertEquals(5, SurveyPrepEdit.lockAll(p, "front", null, false))
        assertEquals(1, SurveyPrepEdit.lockAll(p, "right", null, true))
        assertTrue(p.itemsOf("right").all { it.locked }); assertTrue(p.itemsOf("front").none { it.locked })
    }

    @Test fun `the long-press menu and its Lock all group name the type (ct52 X2-X3, sp js lockGroup)`() {
        val p = room()
        val s1 = mark(p, "s1")
        assertEquals("FS1 Switch plate", SurveyPrepEdit.menuHeader(Tx.EN, "front", s1))
        assertEquals(listOf("Delete", "Duplicate", "Lock", "Lock all Switch plate", "Cancel"), SurveyPrepEdit.menuItems(Tx.EN, p, "front", s1).map { it.second })
        assertEquals(listOf("del", "dup", "lock", "lockall", "cancel"), SurveyPrepEdit.menuItems(Tx.EN, p, "front", s1).map { it.first })
        val g = SurveyPrepEdit.lockGroup(p, "front", s1)
        assertEquals("Switch plate", g.name); assertEquals(2, g.n); assertFalse(g.all)
        SurveyPrepEdit.lockAll(p, "front", "S", true)
        assertEquals("🔒 FS1 Switch plate", SurveyPrepEdit.menuHeader(Tx.EN, "front", s1))
        assertEquals(listOf("Delete", "Duplicate", "Unlock", "Unlock all Switch plate", "Cancel"), SurveyPrepEdit.menuItems(Tx.EN, p, "front", s1).map { it.second })
        // the floor's S is a Step: its group is named so
        val st = p.addMarkAt("floor", "S", 1300.0, 1600.0, W, L, "g1")
        assertEquals("Lock all Step", SurveyPrepEdit.menuItems(Tx.EN, p, "floor", st)[3].second)
        // ct52 1033: "Delete|Duplicate|Lock|Lock all Door|Cancel"
        assertEquals("Delete|Duplicate|Lock|Lock all Door|Cancel", SurveyPrepEdit.menuItems(Tx.EN, p, "front", mark(p, "d1")).joinToString("|") { it.second })
    }

    @Test fun `the lines' lock is the surface's own, and says so`() {
        val p = room()
        val sf = p.surface("front")
        assertFalse(SurveyPrep.linesLocked(sf))
        assertTrue(SurveyPrepEdit.setLinesLocked(p, "front", null))
        assertTrue(SurveyPrep.linesLocked(sf)); assertEquals(true, sf.extra[SurveyPrep.LINES_LOCK])
        assertFalse(SurveyPrep.linesLocked(p.surface("right")))
        assertFalse(SurveyPrepEdit.setLinesLocked(p, "front", false))
        assertEquals("Front wall lines locked", SurveyPrepEdit.linesSaid(Tx.EN, "front", true))
        assertEquals("Floor lines unlocked", SurveyPrepEdit.linesSaid(Tx.EN, "floor", false))
        assertEquals("Lines are locked — unlock first", SurveyPrepEdit.linesRefusal(Tx.EN))
        assertEquals("Locked", SurveyPrepEdit.linesState(Tx.EN, true)); assertEquals("Unlock lines", SurveyPrepEdit.linesAction(Tx.EN, true))
        assertEquals("Lock lines", SurveyPrepEdit.linesAction(Tx.EN, false))
    }

    @Test fun `the refusal and the delete texts`() {
        assertEquals("FD1 is locked — unlock first", SurveyPrepEdit.refusal(Tx.EN, "FD1"))
        assertEquals("FD1 deleted", SurveyPrepEdit.deleted(Tx.EN, "FD1"))
    }

    // ------------------------------------------------------------------ port 62 slice L2: a mark past Existing

    private fun life(p: SurveyPrep.Prep, id: String, vararg keys: Pair<String, Any?>) { val e = mark(p, id); e.extra = e.extra + keys }

    @Test fun `a mark past Existing is never deleted -- locked or not, with or without readings -- and a Note or an unknown state is`() {
        val p = room()
        for ((i, st) in MarkLife.STORED.withIndex()) {
            val id = "w$i"; p.addMarkAt("front", "W", 1400.0, 1500.0, W, H, id); life(p, id, "life" to st)
            val e = mark(p, id); p.actual["${e.code}.w"] = 1200.0 + i
            assertFalse(p.deleteMark(id), st)
            assertEquals(e, p.markById(id)?.second, "$st: still there"); assertEquals(1200.0 + i, p.actual["${e.code}.w"], "$st: with its reading")
            assertEquals(st, MarkLife.lifeOf(e), "... and in the state it was in: a refused delete moves nothing")
        }
        // born a wish, a vendor's, and a locked wish: the same
        p.addMarkAt("front", "W", 1400.0, 1500.0, W, H, "b1"); life(p, "b1", "born" to "wish", "life" to "wish"); assertFalse(p.deleteMark("b1"))
        p.addMarkAt("front", "W", 1400.0, 1500.0, W, H, "b2"); life(p, "b2", "born" to "vadd", "life" to "vadd"); assertFalse(p.deleteMark("b2"))
        life(p, "d1", "life" to "wish", "locked" to true); assertFalse(p.deleteMark("d1"))
        // what the control reads, and what a tap does, is MarkLife's: Drop opens the sheet (slice L3), Restore moves back at once
        assertEquals(listOf("DROP_SHEET", "RESTORE", "PLAIN"), listOf("b1", "w6", "d1").map { MarkLife.deleteAct(mark(p, it)).name })
        // a Note has no state, and a state this build does not know reads Existing: both are deleted as v52 deletes
        p.surface("front").marks.add(SurveyPrep.Mark("n", "NOTE", "FNOTE1", 1000.0, 1050.0, 1000.0, 1050.0, mapOf("life" to "wish", "note" to "words")))
        assertTrue(p.deleteMark("n")); assertNull(p.markById("n"))
        life(p, "d2", "life" to "zzz"); assertTrue(p.deleteMark("d2"))
        // back at Existing, it is an ordinary mark again
        val w0 = mark(p, "w0")
        assertTrue(MarkLife.move(w0, "exist", PhaseTools.WISH_LIST, java.time.Instant.parse("2026-11-11T05:00:00Z")).ok)
        assertTrue(p.deleteMark("w0")); assertNull(p.actual["${w0.code}.w"])
    }

    @Test fun `the delete control's word, its label and the menu's first row follow the mark -- Drop past Existing, Restore on a Dropped one`() {
        val p = room()
        p.addMeas("front", SurveyMeas.LINE, listOf(doubleArrayOf(100.0, 200.0), doubleArrayOf(900.0, 200.0)), "m1")
        val k = p.addChain("front", "x", listOf(doubleArrayOf(0.0, 500.0), doubleArrayOf(900.0, 500.0)), listOf(null, null), "k1")
        fun row(id: String) = listOf(Tx.EN, Tx.MR).joinToString(" | ") { tx -> SurveyPrepEdit.deleteWord(tx, p, id) + " / " + SurveyPrepEdit.deleteLabel(tx, p, id) }
        // an Existing mark, a Line, a chain: v52's own, the label through its whole key
        assertEquals("Delete / Delete FD1 Door | हटवा / FD1 दरवाजा हटवा", row("d1"))
        assertEquals("Delete / Delete F-M1 marked length", row("m1").substringBefore(" | "))
        assertEquals("Delete / Delete ${k.code} chain", row("k1").substringBefore(" | "))
        assertEquals(listOf("Delete", "Duplicate", "Lock", "Lock all Door", "Cancel"), SurveyPrepEdit.menuItemsOf(Tx.EN, p, "d1").map { it.second })
        // a wish: Drop
        life(p, "d1", "life" to "wish", "act" to "replace")
        assertEquals("Drop / Drop FD1 Door | वगळा / वगळा FD1 दरवाजा", row("d1"))
        assertEquals(listOf("Drop", "Duplicate", "Lock", "Lock all Door", "Cancel"), SurveyPrepEdit.menuItemsOf(Tx.EN, p, "d1").map { it.second })
        assertEquals(listOf("del", "dup", "lock", "lockall", "cancel"), SurveyPrepEdit.menuItemsOf(Tx.EN, p, "d1").map { it.first }, "the row is still the delete row: the screen decides what a tap does")
        // a Dropped mark: Restore
        life(p, "d1", "life" to "drop", "why" to "not needed")
        assertEquals("Restore / Restore FD1 Door | परत आणा / परत आणा FD1 दरवाजा", row("d1"))
        assertEquals("Restore|Duplicate|Lock|Lock all Door|Cancel", SurveyPrepEdit.menuItems(Tx.EN, p, "front", mark(p, "d1")).joinToString("|") { it.second })
        assertEquals("परत आणा", SurveyPrepEdit.menuItems(Tx.MR, p, "front", mark(p, "d1"))[0].second)
        // the other marks of the surface, a Line's and a chain's menus are untouched
        assertEquals("Delete", SurveyPrepEdit.menuItemsOf(Tx.EN, p, "d2")[0].second); assertEquals("Delete", SurveyPrepEdit.menuItemsOf(Tx.EN, p, "m1")[0].second)
        assertEquals("Delete", SurveyPrepEdit.menuItemsOf(Tx.EN, p, "k1")[0].second)
        // an id that is nothing: the word is Delete, and there is no label
        assertEquals("Delete", SurveyPrepEdit.deleteWord(Tx.EN, p, "nope")); assertNull(SurveyPrepEdit.deleteLabel(Tx.EN, p, "nope"))
    }

    @Test fun `a mark in Scope stays locked -- its padlock, the menu's Unlock and Unlock all leave it, and say why`() {
        val p = room()
        life(p, "d1", "life" to "scope", "locked" to true); val e = mark(p, "d1"); val was = e.extra
        // its own padlock: unlock and toggle change nothing and answer locked
        assertEquals(true, SurveyPrepEdit.setLocked(p, "d1", false)); assertEquals(true, SurveyPrepEdit.setLocked(p, "d1", null))
        assertTrue(SurveyPrep.isLocked(e)); assertTrue(was === e.extra, "nothing was written")
        assertEquals(true, SurveyPrepEdit.setLocked(p, "d1", true), "locking a locked mark is still nothing")
        assertEquals("FD1 is in Scope — take it Out of scope in its sheet", SurveyPrepEdit.unlockRefusal(Tx.EN, p, "d1"))
        assertEquals("FD1 स्कोपमध्ये आहे — त्याच्या तपशीलात \"स्कोपबाहेर\" करा", SurveyPrepEdit.unlockRefusal(Tx.MR, p, "d1"))
        // every other state, locked, unlocks as before -- and so does a mark that is not locked, a Line and an id that is nothing
        for (st in MarkLife.STORED - "scope") {
            life(p, "d2", "life" to st, "locked" to true)
            assertNull(SurveyPrepEdit.unlockRefusal(Tx.EN, p, "d2"), st); assertEquals(false, SurveyPrepEdit.setLocked(p, "d2", false), st)
        }
        assertNull(SurveyPrepEdit.unlockRefusal(Tx.EN, p, "s1")); assertNull(SurveyPrepEdit.unlockRefusal(Tx.EN, p, "nope"))
        p.addMeas("front", SurveyMeas.LINE, listOf(doubleArrayOf(100.0, 200.0), doubleArrayOf(900.0, 200.0)), "m1"); SurveyPrepEdit.setLocked(p, "m1", true)
        assertNull(SurveyPrepEdit.unlockRefusal(Tx.EN, p, "m1")); assertEquals(false, SurveyPrepEdit.setLocked(p, "m1", false))
        // a Scope mark that is NOT locked (a file may hold one) may be locked, and then stays so
        life(p, "s1", "life" to "scope"); assertNull(SurveyPrepEdit.unlockRefusal(Tx.EN, p, "s1"), "nothing to unlock")
        assertEquals(true, SurveyPrepEdit.setLocked(p, "s1", null)); assertEquals(true, SurveyPrepEdit.setLocked(p, "s1", null))

        // Unlock all skips Scope marks and says how many stayed
        val q = room()
        life(q, "d1", "life" to "scope", "locked" to true); SurveyPrepEdit.setLocked(q, "d2", true); SurveyPrepEdit.setLocked(q, "s1", true)
        assertEquals(1, SurveyPrepEdit.scopeKept(q, "front", "D", false)); assertEquals(0, SurveyPrepEdit.scopeKept(q, "front", "D", true), "locking keeps nothing back")
        assertEquals(1, SurveyPrepEdit.lockAll(q, "front", "D", false), "FD2 alone: FD1 is in Scope")
        assertTrue(SurveyPrep.isLocked(mark(q, "d1"))); assertFalse(SurveyPrep.isLocked(mark(q, "d2")))
        assertEquals("Unlocked 1 item · front wall · 1 Scope mark stays locked", SurveyPrepEdit.lockedSnack(Tx.EN, false, 1, "front", 1))
        assertEquals("1 आयटम अनलॉक केले · समोरील भिंत · स्कोपमधील 1 मार्क लॉक राहतात", SurveyPrepEdit.lockedSnack(Tx.MR, false, 1, "front", 1))
        // again: nothing changes, and the Scope mark is all there is to say (alone, with no UNDO: MarkLife.scopeKeptText)
        assertEquals(0, SurveyPrepEdit.lockAll(q, "front", "D", false)); assertEquals(1, SurveyPrepEdit.scopeKept(q, "front", "D", false))
        assertEquals("1 Scope mark stays locked", MarkLife.scopeKeptText(Tx.EN, SurveyPrepEdit.scopeKept(q, "front", "D", false)))
        // every item on the surface, every mark, another type, another kind
        assertEquals(1, SurveyPrepEdit.scopeKept(q, "front", null, false)); assertEquals(1, SurveyPrepEdit.scopeKept(q, "front", null, false, kind = "mark"))
        assertEquals(0, SurveyPrepEdit.scopeKept(q, "front", "S", false)); assertEquals(0, SurveyPrepEdit.scopeKept(q, "front", null, false, kind = "chain"))
        assertEquals(0, SurveyPrepEdit.scopeKept(q, "right", null, false))
        // only a LOCKED Scope mark is "kept locked": one that is not locked (a file may hold it) has nothing to keep
        life(q, "s2", "life" to "scope")
        assertEquals(0, SurveyPrepEdit.scopeKept(q, "front", "S", false)); assertEquals(1, SurveyPrepEdit.scopeKept(q, "front", null, false))
        assertEquals(1, SurveyPrepEdit.lockAll(q, "front", null, false), "FS1; FD1 stays"); assertTrue(SurveyPrep.isLocked(mark(q, "d1")))
        // Lock all is unchanged -- it locks the unlocked Scope mark like any other, and then that one stays locked too
        assertEquals(4, SurveyPrepEdit.lockAll(q, "front", null, true))
        assertEquals(2, SurveyPrepEdit.scopeKept(q, "front", null, false)); assertEquals(1, SurveyPrepEdit.scopeKept(q, "front", "S", false))
        assertEquals(3, SurveyPrepEdit.lockAll(q, "front", null, false), "FD2, FS1 and FE1; FD1 and FS2 are in Scope")
        assertEquals("Unlocked 3 items · front wall · 2 Scope marks stay locked", SurveyPrepEdit.lockedSnack(Tx.EN, false, 3, "front", SurveyPrepEdit.scopeKept(q, "front", null, false)))
        // with no Scope mark the snackbar is v52's own
        assertEquals("Unlocked 2 items · front wall", SurveyPrepEdit.lockedSnack(Tx.EN, false, 2, "front", 0))
        assertEquals(SurveyPrepEdit.lockedSnack(Tx.EN, true, 4, "front"), SurveyPrepEdit.lockedSnack(Tx.EN, true, 4, "front", 0))
    }
}
