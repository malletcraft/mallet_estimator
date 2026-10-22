package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        assertEquals("Locked 1 item · front wall", SurveyPrepEdit.lockedSnack(true, 1, "front"))
        assertEquals("Unlocked 2 items · front wall", SurveyPrepEdit.lockedSnack(false, 2, "front"))
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
        assertEquals("FS1 Switch plate", SurveyPrepEdit.menuHeader("front", s1))
        assertEquals(listOf("Delete", "Duplicate", "Lock", "Lock all Switch plate", "Cancel"), SurveyPrepEdit.menuItems(p, "front", s1).map { it.second })
        assertEquals(listOf("del", "dup", "lock", "lockall", "cancel"), SurveyPrepEdit.menuItems(p, "front", s1).map { it.first })
        val g = SurveyPrepEdit.lockGroup(p, "front", s1)
        assertEquals("Switch plate", g.name); assertEquals(2, g.n); assertFalse(g.all)
        SurveyPrepEdit.lockAll(p, "front", "S", true)
        assertEquals("🔒 FS1 Switch plate", SurveyPrepEdit.menuHeader("front", s1))
        assertEquals(listOf("Delete", "Duplicate", "Unlock", "Unlock all Switch plate", "Cancel"), SurveyPrepEdit.menuItems(p, "front", s1).map { it.second })
        // the floor's S is a Step: its group is named so
        val st = p.addMarkAt("floor", "S", 1300.0, 1600.0, W, L, "g1")
        assertEquals("Lock all Step", SurveyPrepEdit.menuItems(p, "floor", st)[3].second)
        // ct52 1033: "Delete|Duplicate|Lock|Lock all Door|Cancel"
        assertEquals("Delete|Duplicate|Lock|Lock all Door|Cancel", SurveyPrepEdit.menuItems(p, "front", mark(p, "d1")).joinToString("|") { it.second })
    }

    @Test fun `the lines' lock is the surface's own, and says so`() {
        val p = room()
        val sf = p.surface("front")
        assertFalse(SurveyPrep.linesLocked(sf))
        assertTrue(SurveyPrepEdit.setLinesLocked(p, "front", null))
        assertTrue(SurveyPrep.linesLocked(sf)); assertEquals(true, sf.extra[SurveyPrep.LINES_LOCK])
        assertFalse(SurveyPrep.linesLocked(p.surface("right")))
        assertFalse(SurveyPrepEdit.setLinesLocked(p, "front", false))
        assertEquals("Front wall lines locked", SurveyPrepEdit.linesSaid("front", true))
        assertEquals("Floor lines unlocked", SurveyPrepEdit.linesSaid("floor", false))
        assertEquals("Lines are locked — unlock first", SurveyPrepEdit.LINES_REFUSAL)
        assertEquals("Locked", SurveyPrepEdit.linesState(true)); assertEquals("Unlock lines", SurveyPrepEdit.linesAction(true))
        assertEquals("Lock lines", SurveyPrepEdit.linesAction(false))
    }

    @Test fun `the refusal and the delete texts`() {
        assertEquals("FD1 is locked — unlock first", SurveyPrepEdit.refusal("FD1"))
        assertEquals("FD1 deleted", SurveyPrepEdit.deleted("FD1"))
    }
}
