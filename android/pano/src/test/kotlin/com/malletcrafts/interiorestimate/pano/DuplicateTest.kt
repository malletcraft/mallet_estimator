package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * DUPLICATE (v52 slice 8, sp.js duplicateItem): 100 mm right and up, back the other way at an edge, never past
 * it; the next code of its type; no readings; unlocked. Codes are never handed out twice, not even after a
 * delete. The synthetic room: L 3319, W 2678, H 2677 (the front wall 2678 x 2677).
 */
class DuplicateTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0

    @Test fun `a copy 100 mm right and up, the same type and size, the next code`() {
        val p = SurveyPrep.fresh(L, W, H)
        val s1 = p.addMarkAt("front", "S", 600.0, 1150.0, W, H, "s1")     // 500..700 x 1105..1195
        val c = assertNotNull(SurveyPrepEdit.duplicate(p, "s1", W, H, "s2"))
        assertEquals("FS2", c.code)                     // ct52 X2/X3: Duplicate FS1 makes FS2
        assertEquals("S", c.t)
        assertEquals(listOf(600.0, 800.0, 1205.0, 1295.0), listOf(c.x0, c.x1, c.y0, c.y1))
        assertEquals(s1.x1 - s1.x0, c.x1 - c.x0); assertEquals(s1.y1 - s1.y0, c.y1 - c.y0)
        assertEquals(c, p.markById("s2")?.second)
        assertEquals(2, p.surface("front").marks.size)
    }

    @Test fun `at the right or top edge it goes back the other way, and never past the face`() {
        val p = SurveyPrep.fresh(L, W, H)
        // a Door against the right corner and the ceiling: 900 x 2100 at x 1778..2678, y 577..2677
        val d = p.addMarkAt("front", "D", 2600.0, 2600.0, W, H, "d1")
        assertEquals(listOf(1778.0, 2678.0, 577.0, 2677.0), listOf(d.x0, d.x1, d.y0, d.y1))
        val c = SurveyPrepEdit.duplicate(p, "d1", W, H, "d2")!!
        assertEquals(listOf(1678.0, 2578.0, 477.0, 2577.0), listOf(c.x0, c.x1, c.y0, c.y1))
        // a Beam spans the wall: no room either way across, it stays at 0; up is blocked, so 100 down
        val b = p.addMark("front", "B", W, H, "b1")      // 0..2678 x 2277..2677
        val cb = SurveyPrepEdit.duplicate(p, "b1", W, H, "b2")!!
        assertEquals(listOf(0.0, W, 2177.0, 2577.0), listOf(cb.x0, cb.x1, cb.y0, cb.y1))
        // a Column the full height at the floor: up is past the ceiling, and down past the floor -- kept at 0
        val k = p.addMark("front", "C", W, H, "c1")      // 2228..2528 x 0..2677
        val ck = SurveyPrepEdit.duplicate(p, "c1", W, H, "c2")!!
        assertEquals(listOf(2328.0, 2628.0, 0.0, H), listOf(ck.x0, ck.x1, ck.y0, ck.y1))
        for (e in p.surface("front").marks) assertFalse(SurveyPrep.outOf(e, W, H), e.code)
        assertEquals(k.y1, ck.y1)
    }

    @Test fun `a duplicate starts with no readings and unlocked, even from a locked, measured original`() {
        val p = SurveyPrep.fresh(L, W, H)
        val d = p.addMarkAt("front", "D", 700.0, 1100.0, W, H, "d1")
        p.actual["${d.code}.w"] = 905.0; p.actual["${d.code}.h"] = 2053.0
        SurveyPrepEdit.setLocked(p, "d1", true)
        val c = SurveyPrepEdit.duplicate(p, "d1", W, H, "d2")!!
        assertEquals("FD2", c.code)
        assertFalse(SurveyPrep.isLocked(c))
        assertTrue(p.actual.keys.none { it.startsWith(c.code + ".") })
        assertEquals(2, p.actual.size, "the original's readings stay")
        assertTrue(c.extra.isEmpty(), "nothing of the original's kept keys (its lock, a point's conversion) is copied")
    }

    @Test fun `codes are never reused after a delete -- not even the highest`() {
        val p = SurveyPrep.fresh(L, W, H)
        p.addMarkAt("front", "S", 600.0, 1150.0, W, H, "s1")
        SurveyPrepEdit.duplicate(p, "s1", W, H, "s2")                       // FS2
        assertTrue(p.deleteMark("s2"))
        assertEquals("FS3", p.nextCode("front", "S"))
        assertEquals("FS3", SurveyPrepEdit.duplicate(p, "s1", W, H, "s3")!!.code)
        // the new mark tool runs on the same counter
        assertTrue(p.deleteMark("s3"))
        assertEquals("FS4", p.addMarkAt("front", "S", 1500.0, 1150.0, W, H, "s4").code)
        // other types and surfaces count apart
        assertEquals("FD1", p.addMark("front", "D", W, H, "d1").code)
        assertEquals("RS1", p.addMark("right", "S", L, H, "r1").code)
    }

    @Test fun `a file from before slice 8 has no counter -- a delete records the code it took`() {
        val p = SurveyPrep.fresh(L, W, H)
        p.surface("front").marks.add(SurveyPrep.Mark("w1", "W", "FW1", 100.0, 1300.0, 900.0, 2100.0))
        p.surface("front").marks.add(SurveyPrep.Mark("w2", "W", "FW2", 1400.0, 2600.0, 900.0, 2100.0))
        assertNull(p.surface("front").extra[SurveyPrep.SEQ])
        assertEquals("FW3", p.nextCode("front", "W"))
        p.deleteMark("w2")
        assertEquals(mapOf("W" to 2), p.surface("front").extra[SurveyPrep.SEQ])
        assertEquals("FW3", p.nextCode("front", "W"))
    }

    @Test fun `the counter survives an undo (keepSeq) and a file round trip`() {
        val p = SurveyPrep.fresh(L, W, H)
        p.addMarkAt("front", "W", 1300.0, 1500.0, W, H, "w1")
        val before = p.copy()                                     // the snapshot an add takes
        p.addMarkAt("front", "W", 1300.0, 1500.0, W, H, "w2")     // FW2, its readings maybe already on the web
        val back = before.copy(); SurveyPrepEdit.keepSeq(p, back) // ↶ puts the older prep back
        assertNull(back.markById("w2"))
        assertEquals("FW3", back.nextCode("front", "W"))
        // and a number read from a file as a Double counts the same
        val f = SurveyPrepFile.decode(SurveyPrepFile.encode(back, L, W, H).let { o ->
            @Suppress("UNCHECKED_CAST")
            val w = (o["w"] as Map<String, Map<String, Any?>>).mapValues { (s, so) ->
                if (s != "front") so else LinkedHashMap(so).apply { put(SurveyPrep.SEQ, mapOf("W" to 2.0)) } }
            LinkedHashMap(o).apply { put("w", w) } })!!
        assertEquals("FW3", f.prep.nextCode("front", "W"))
        // keepSeq only ever raises
        val q = SurveyPrep.fresh(L, W, H); SurveyPrepEdit.keepSeq(q, back)
        assertEquals("FW3", back.nextCode("front", "W"))
    }
}
