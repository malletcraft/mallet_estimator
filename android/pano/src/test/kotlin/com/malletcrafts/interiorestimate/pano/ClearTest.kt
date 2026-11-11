package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.malletcrafts.interiorestimate.pano.i18n.Tx

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
        assertEquals("Cleared 2 items, 1 locked kept", SurveyPrepEdit.clearedSnack(Tx.EN, r.n, r.kept))
    }

    @Test fun `Clear whole room walks all six surfaces`() {
        val p = room()
        val r = SurveyPrepEdit.clearItems(p, "room", "list")
        assertEquals(4, r.n); assertEquals(2, r.kept)
        assertEquals(listOf("FD1", "RD1"), codes(p))
        assertEquals(setOf("FD1.w", "FD1.h", "RD1.w"), p.actual.keys)
        assertEquals("Cleared 4 items, 2 locked kept", SurveyPrepEdit.clearedSnack(Tx.EN, r.n, r.kept))
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
        assertEquals("3 locked — nothing to clear", SurveyPrepEdit.clearLabel(Tx.EN, "surf", 0, 3, false))
        assertEquals("Nothing on this surface to clear", SurveyPrepEdit.clearLabel(Tx.EN, "surf", 0, 0, false))
        assertEquals("Nothing in the room to clear", SurveyPrepEdit.clearLabel(Tx.EN, "room", 0, 0, false))
    }

    @Test fun `the clear buttons' words (sp js 1473-1475 and 847)`() {
        assertEquals("Clear this surface… (3)", SurveyPrepEdit.clearLabel(Tx.EN, "surf", 3, 1, false))
        assertEquals("Clear whole room… (12)", SurveyPrepEdit.clearLabel(Tx.EN, "room", 12, 0, false))
        assertEquals("Tap again to clear 3 items", SurveyPrepEdit.clearLabel(Tx.EN, "surf", 3, 0, true))
        assertEquals("Tap again to clear 1 item", SurveyPrepEdit.clearLabel(Tx.EN, "room", 1, 0, true))
        assertEquals("5 items cleared", SurveyPrepEdit.clearedSnack(Tx.EN, 5, 0))
        assertEquals("1 item cleared", SurveyPrepEdit.clearedSnack(Tx.EN, 1, 0))
        assertEquals(4000L, SurveyPrepEdit.ARM_MS); assertEquals(5000L, SurveyPrepEdit.SNACK_MS); assertEquals(2600L, SurveyPrepEdit.SAY_MS)
    }

    @Test fun `a cleared code is not handed out again`() {
        val p = room()
        SurveyPrepEdit.clearItems(p, "surf", "front")
        assertEquals("FW2", p.nextCode("front", "W"))
        assertEquals("FE2", p.addMark("front", "E", W, H, "x").code)
        assertTrue(p.actual.keys.none { it.startsWith("FE2.") })
    }

    // ------------------------------------------------------------------ port 62 slice L2: a clear keeps a wish

    private fun life(p: SurveyPrep.Prep, id: String, vararg keys: Pair<String, Any?>) { val e = p.markById(id)!!.second; e.extra = e.extra + keys }

    /**
     * [room] once wishes are marked on it: on the front wall FW1 is a surveyed window the client wants replaced and FE1
     * a wish since Dropped; FW2 (new) was born a wish; a Note stands beside them; on the right wall RW1 is in Scope, so
     * locked. FD1 and RD1 are locked as before, GE1 on the floor is an Existing spot.
     */
    private fun wished(): SurveyPrep.Prep {
        val p = room()
        life(p, "fw1", "life" to "wish", "lifeAs" to "client", "act" to "replace", "note" to "install UPVC window")
        life(p, "fe1", "life" to "drop", "why" to "not needed")
        p.addMarkAt("front", "W", 600.0, 2300.0, W, H, "fw2"); life(p, "fw2", "born" to "wish", "life" to "wish")
        p.surface("front").marks.add(SurveyPrep.Mark("note", "NOTE", "FNOTE1", 1000.0, 1050.0, 1000.0, 1050.0, mapOf("locked" to false, "life" to "wish", "note" to "a Note has no state")))
        life(p, "rw1", "life" to "scope", "locked" to true)
        p.actual["FW2.w"] = 1500.0
        return p
    }

    @Test fun `a clear never counts a mark past Existing -- it counts what it would keep of them apart from the locked`() {
        val p = wished()
        // front: FD1 locked; FW1, FE1 and FW2 past Existing and unlocked; only the Note would go
        assertEquals(1, SurveyPrepEdit.clearCount(p, "surf", "front")); assertEquals(1, SurveyPrepEdit.lockedCount(p, "surf", "front")); assertEquals(3, SurveyPrepEdit.pastCount(p, "surf", "front"))
        // right: RD1 locked; RW1 in Scope is LOCKED, so it counts among the locked and not among the marks past Existing
        assertEquals(0, SurveyPrepEdit.clearCount(p, "surf", "right")); assertEquals(2, SurveyPrepEdit.lockedCount(p, "surf", "right")); assertEquals(0, SurveyPrepEdit.pastCount(p, "surf", "right"))
        // the room: the Note and GE1 go
        assertEquals(2, SurveyPrepEdit.clearCount(p, "room", "list")); assertEquals(3, SurveyPrepEdit.lockedCount(p, "room", "list")); assertEquals(3, SurveyPrepEdit.pastCount(p, "room", "list"))
        // an item says so itself
        val items = p.itemsOf("front").associateBy { it.code }
        assertEquals(listOf(false, true, true, true, false), listOf("FD1", "FW1", "FE1", "FW2", "FNOTE1").map { items.getValue(it).past })
        assertEquals(listOf(true, true, true, true, false), listOf("FD1", "FW1", "FE1", "FW2", "FNOTE1").map { items.getValue(it).spared })
        assertTrue(p.itemById("rw1")!!.let { it.past && it.locked && it.spared }); assertFalse(p.itemById("ge1")!!.past)
        assertTrue(p.ownerOf("FW1.w")!!.past)
    }

    @Test fun `Clear this surface keeps every wish and its readings, takes the Note, and says what it kept`() {
        val p = wished(); val before = p.markById("fw1")!!.second.extra
        val r = SurveyPrepEdit.clearItems(p, "surf", "front")
        assertEquals(1, r.n); assertEquals(1, r.kept); assertEquals(3, r.past); assertEquals(listOf("FNOTE1"), r.codes)
        assertEquals(listOf("FD1", "FW1", "FE1", "FW2"), p.surface("front").marks.map { it.code }, "in the order they were")
        assertEquals(before, p.markById("fw1")!!.second.extra, "the wish as it was: its state, act and note")
        assertEquals(1200.0, p.actual["FW1.w"]); assertEquals(300.0, p.actual["FE1.cl"]); assertEquals(1500.0, p.actual["FW2.w"]); assertEquals(905.0, p.actual["FD1.w"])
        assertEquals("Cleared 1 item · 1 locked kept · 3 marks past Existing kept — drop them one by one", SurveyPrepEdit.clearedSnack(Tx.EN, r))
        assertEquals("1 आयटम काढले · 1 लॉक केलेले ठेवले · \"सध्याचे\" च्या पुढे गेलेले 3 मार्क ठेवले — ते एकेक करून वगळा", SurveyPrepEdit.clearedSnack(Tx.MR, r))
        // a second clear has nothing to take: nothing changes, whatever is still there
        val again = SurveyPrepEdit.clearItems(p, "surf", "front")
        assertEquals(0, again.n); assertEquals(3, again.past); assertEquals(4, p.surface("front").marks.size)
        // the button's own words do not change: the locked are named, the wishes are simply not there to clear
        fun label(q: SurveyPrep.Prep) = SurveyPrepEdit.clearLabel(Tx.EN, "surf", SurveyPrepEdit.clearCount(q, "surf", "front"), SurveyPrepEdit.lockedCount(q, "surf", "front"), false)
        assertEquals("1 locked — nothing to clear", label(p))
        val only = SurveyPrep.fresh(L, W, H); only.addMarkAt("front", "W", 1800.0, 1500.0, W, H, "w"); life(only, "w", "life" to "wish")
        assertEquals("Nothing on this surface to clear", label(only))
        assertEquals(0, SurveyPrepEdit.clearItems(only, "surf", "front").n); assertEquals(listOf("FW1"), only.surface("front").marks.map { it.code })
    }

    @Test fun `Clear whole room keeps the wishes on every surface -- and with none kept the snackbar is v52's, word for word`() {
        val p = wished()
        val r = SurveyPrepEdit.clearItems(p, "room", "list")
        assertEquals(2, r.n); assertEquals(3, r.kept); assertEquals(3, r.past); assertEquals(listOf("FNOTE1", "GE1"), r.codes)
        assertEquals(listOf("FD1", "FW1", "FE1", "FW2", "RD1", "RW1"), codes(p))
        assertEquals(setOf("FD1.w", "FD1.h", "FW1.w", "FE1.cl", "FW2.w", "RD1.w", "RW1.h"), p.actual.keys)
        assertEquals("Cleared 2 items · 3 locked kept · 3 marks past Existing kept — drop them one by one", SurveyPrepEdit.clearedSnack(Tx.EN, r))
        // no mark past Existing kept: exactly what v52 said, with locked items kept and with none
        val v = SurveyPrepEdit.clearItems(room(), "room", "list")
        assertEquals(0, v.past); assertEquals("Cleared 4 items, 2 locked kept", SurveyPrepEdit.clearedSnack(Tx.EN, v))
        assertEquals(SurveyPrepEdit.clearedSnack(Tx.EN, v.n, v.kept), SurveyPrepEdit.clearedSnack(Tx.EN, v))
        val u = room(); SurveyPrepEdit.setLocked(u, "fd1", false); SurveyPrepEdit.setLocked(u, "rd1", false)
        assertEquals("6 items cleared", SurveyPrepEdit.clearedSnack(Tx.EN, SurveyPrepEdit.clearItems(u, "room", "list")))
        // a locked Scope mark alone kept is a locked item kept: v52's line still
        val s = room(); life(s, "rw1", "life" to "scope", "locked" to true)
        assertEquals("Cleared 3 items, 3 locked kept", SurveyPrepEdit.clearedSnack(Tx.EN, SurveyPrepEdit.clearItems(s, "room", "list")))
    }

    @Test fun `a kept wish keeps its code -- the next mark of its type runs past it, and past the Note that went`() {
        val p = wished()
        SurveyPrepEdit.clearItems(p, "surf", "front")
        assertEquals("FW3", p.nextCode("front", "W"), "FW1 and FW2 are still there")
        assertEquals("FNOTE2", p.nextCode("front", "NOTE"), "FNOTE1 went, and its number with it")
        assertEquals("FE2", p.nextCode("front", "E"), "FE1, Dropped, is still there")
    }
}
