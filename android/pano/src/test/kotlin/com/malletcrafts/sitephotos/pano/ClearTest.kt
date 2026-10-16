package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * THE CLEARS (v52 slice 8, sp.js 839-850): ⋮ Clear this surface… (the open face; none on the List tab) and
 * Clear whole room… take every UNLOCKED item and its readings, and keep every locked one with its readings.
 * The synthetic room: L 3319, W 2678, H 2677.
 */
class ClearTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0

    /** Front: FD1 (locked), FW1, FE1; right: RD1 (locked), RW1; floor: GE1. Every mark with a reading or two. */
    private fun room(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        p.addMarkAt("front", "D", 600.0, 1050.0, W, H, "fd1")
        p.addMarkAt("front", "W", 1800.0, 1500.0, W, H, "fw1")
        p.addMarkAt("front", "E", 300.0, 300.0, W, H, "fe1")
        p.addMarkAt("right", "D", 600.0, 1050.0, L, H, "rd1")
        p.addMarkAt("right", "W", 1800.0, 1500.0, L, H, "rw1")
        p.addMarkAt("floor", "E", 1000.0, 1000.0, W, L, "ge1")
        for ((k, v) in listOf("FD1.w" to 905.0, "FD1.h" to 2053.0, "FW1.w" to 1200.0, "FE1.cl" to 300.0,
                "RD1.w" to 900.0, "RW1.h" to 1210.0, "GE1.l" to 1000.0)) p.actual[k] = v
        SurveyPrepEdit.setLocked(p, "fd1", true); SurveyPrepEdit.setLocked(p, "rd1", true)
        return p
    }

    private fun codes(p: SurveyPrep.Prep) = SurveyPrep.SURFACES.flatMap { s -> p.surface(s).marks.map { it.code } }

    @Test fun `counts -- what a clear would take, never a locked item, and what it keeps`() {
        val p = room()
        assertEquals(2, SurveyPrepEdit.clearCount(p, "surf", "front")); assertEquals(1, SurveyPrepEdit.lockedCount(p, "surf", "front"))
        assertEquals(4, SurveyPrepEdit.clearCount(p, "room", "front")); assertEquals(2, SurveyPrepEdit.lockedCount(p, "room", "front"))
        assertEquals(0, SurveyPrepEdit.clearCount(p, "surf", "ceil"))
        // nothing on the List tab: Clear this surface is hidden there and counts nothing
        assertEquals(emptyList(), SurveyPrepEdit.clearList("surf", "list"))
        assertEquals(0, SurveyPrepEdit.clearCount(p, "surf", "list"))
        assertEquals(4, SurveyPrepEdit.clearCount(p, "room", "list"), "the whole room is the same from the List tab")
    }

    @Test fun `Clear this surface takes the unlocked marks and their readings, and keeps the locked one with its readings`() {
        val p = room()
        val r = SurveyPrepEdit.clearItems(p, "surf", "front")
        assertEquals(2, r.n); assertEquals(1, r.kept); assertEquals(listOf("FW1", "FE1"), r.codes)
        assertEquals(listOf("FD1", "RD1", "RW1", "GE1"), codes(p))
        assertEquals(905.0, p.actual["FD1.w"]); assertEquals(2053.0, p.actual["FD1.h"])
        assertNull(p.actual["FW1.w"]); assertNull(p.actual["FE1.cl"])
        assertEquals(1210.0, p.actual["RW1.h"], "another surface's readings are not touched")
        assertEquals("Cleared 2 items, 1 locked kept", SurveyPrepEdit.clearedSnack(r.n, r.kept))
    }

    @Test fun `Clear whole room walks all six surfaces`() {
        val p = room()
        val r = SurveyPrepEdit.clearItems(p, "room", "list")
        assertEquals(4, r.n); assertEquals(2, r.kept)
        assertEquals(listOf("FD1", "RD1"), codes(p))
        assertEquals(setOf("FD1.w", "FD1.h", "RD1.w"), p.actual.keys)
        assertEquals("Cleared 4 items, 2 locked kept", SurveyPrepEdit.clearedSnack(r.n, r.kept))
        // a second clear has nothing to take, and changes nothing
        val again = SurveyPrepEdit.clearItems(p, "room", "list")
        assertEquals(0, again.n); assertEquals(2, again.kept); assertEquals(listOf("FD1", "RD1"), codes(p))
    }

    @Test fun `a reading key is matched by its code and a dot, never by a longer code`() {
        val p = SurveyPrep.fresh(L, W, H)
        // FW1 and FW10..: only FW1's readings go with it
        p.surface("front").marks.add(SurveyPrep.Mark("a", "W", "FW1", 0.0, 600.0, 900.0, 1500.0))
        p.surface("front").marks.add(SurveyPrep.Mark("b", "W", "FW10", 700.0, 1300.0, 900.0, 1500.0, mapOf("locked" to true)))
        p.actual["FW1.w"] = 600.0; p.actual["FW10.w"] = 610.0
        SurveyPrepEdit.clearItems(p, "surf", "front")
        assertEquals(mapOf("FW10.w" to 610.0), p.actual.toMap())
    }

    @Test fun `nothing unlocked -- no change, and the words say why`() {
        val p = room(); SurveyPrepEdit.lockAll(p, "front", null, true)
        assertEquals(0, SurveyPrepEdit.clearItems(p, "surf", "front").n)
        assertEquals(3, p.surface("front").marks.size)
        assertEquals("3 locked — nothing to clear", SurveyPrepEdit.clearLabel("surf", 0, 3, false))
        assertEquals("Nothing on this surface to clear", SurveyPrepEdit.clearLabel("surf", 0, 0, false))
        assertEquals("Nothing in the room to clear", SurveyPrepEdit.clearLabel("room", 0, 0, false))
    }

    @Test fun `the clear buttons' words (sp js 1473-1475 and 847)`() {
        assertEquals("Clear this surface… (3)", SurveyPrepEdit.clearLabel("surf", 3, 1, false))
        assertEquals("Clear whole room… (12)", SurveyPrepEdit.clearLabel("room", 12, 0, false))
        assertEquals("Tap again to clear 3 items", SurveyPrepEdit.clearLabel("surf", 3, 0, true))
        assertEquals("Tap again to clear 1 item", SurveyPrepEdit.clearLabel("room", 1, 0, true))
        assertEquals("5 items cleared", SurveyPrepEdit.clearedSnack(5, 0))
        assertEquals("1 item cleared", SurveyPrepEdit.clearedSnack(1, 0))
        assertEquals(4000L, SurveyPrepEdit.ARM_MS); assertEquals(5000L, SurveyPrepEdit.SNACK_MS); assertEquals(2600L, SurveyPrepEdit.SAY_MS)
    }

    @Test fun `a cleared code is not handed out again`() {
        val p = room()
        SurveyPrepEdit.clearItems(p, "surf", "front")
        assertEquals("FW2", p.nextCode("front", "W"))
        assertEquals("FE2", p.addMark("front", "E", W, H, "x").code)
        assertTrue(p.actual.keys.none { it.startsWith("FE2.") })
    }
}
