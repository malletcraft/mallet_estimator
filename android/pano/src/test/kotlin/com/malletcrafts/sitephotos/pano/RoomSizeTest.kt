package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The room's L · W · H editable on survey prep (prototype v51; Amit, 14 Oct
 * 2026: "i should be able to edit room outer dimensions details as well";
 * 15 Oct: "Yes, with slice 2"). Ported from the prototype suite's v51 section.
 */
class RoomSizeTest {

    @Test fun `the size range is 300 to 30000 mm, both ends included`() {
        assertTrue(SurveyPrep.valid(300.0)); assertTrue(SurveyPrep.valid(30000.0))
        assertFalse(SurveyPrep.valid(299.9)); assertFalse(SurveyPrep.valid(30000.1))
        assertFalse(SurveyPrep.valid(null)); assertFalse(SurveyPrep.valid(Double.NaN))
        // a surface squares at both ends of it now (it refused anything under 500 or over 20000)
        val f = SurveyPrep.fresh(30000.0, 300.0, 2600.0)
        assertNotNull(SurveyPrep.frame(f.surface("floor").lines, 300.0, 30000.0))
    }

    private val now = doubleArrayOf(5458.0, 3310.0, 2705.0)

    @Test fun `figures out of range, empty or not whole are refused, each box named, and nothing changes`() {
        val a = SurveyPrep.sizeEdit(listOf("200", "3310", "31000"), now)
        assertNull(a.size); assertEquals(listOf(0, 2), a.bad)
        assertEquals(listOf(1), SurveyPrep.sizeEdit(listOf("5458", "", "2705"), now).bad)
        assertEquals("125", SurveyPrep.sizeText("12.5"))
        assertEquals(listOf(0), SurveyPrep.sizeEdit(listOf(SurveyPrep.sizeText("12.5"), "3310", "2705"), now).bad)
        assertEquals(listOf(0), SurveyPrep.sizeEdit(listOf("12.5", "3310", "2705"), now).bad)
        assertEquals("30000", SurveyPrep.sizeText("300001"))
        assertEquals("300 to 30000 mm", SurveyPrep.SIZE_HINT_BAD)
    }

    @Test fun `apply takes whole figures, and a box left at its prefill keeps the size unrounded`() {
        val a = SurveyPrep.sizeEdit(listOf("4000", "2900", "2400"), now)
        assertEquals(listOf(4000.0, 2900.0, 2400.0), a.size!!.toList()); assertFalse(a.same)
        val half = doubleArrayOf(5458.5, 3310.0, 2705.0)
        val same = SurveyPrep.sizeEdit(listOf("5459", "3310", "2705"), half)     // the prefill of 5458.5
        assertTrue(same.same); assertEquals(5458.5, same.size!![0])
        assertEquals(listOf(5458.5, 3300.0, 2705.0), SurveyPrep.sizeEdit(listOf("5459", "3300", "2705"), half).size!!.toList())
        assertEquals(5458.0, SurveyPrep.sizeEdit(listOf("5458", "3310", "2705"), half).size!![0])
        assertTrue(SurveyPrep.sizeEdit(listOf(" 300", "30000 ", "2705"), now).bad.isEmpty())
    }

    @Test fun `a mark past its smaller face is flagged, not moved, and named once with how many more`() {
        val p = SurveyPrep.fresh(5458.0, 3310.0, 2705.0)
        p.surface("front").marks.add(SurveyPrep.Mark("e", "E", "FE1", 2786.0, 2936.0, 300.0, 450.0))
        p.surface("front").marks.add(SurveyPrep.Mark("d", "D", "FD1", 300.0, 1200.0, 0.0, 2100.0))
        assertNull(p.outsideNote(5458.0, 3310.0, 2705.0))
        // L 4000 · W 2900 · H 2400: the front wall is 2900 across, FE1 ends at 2936
        assertEquals("FE1 is outside the face: edit or delete it", p.outsideNote(4000.0, 2900.0, 2400.0))
        assertEquals(listOf("FE1"), p.outside(4000.0, 2900.0, 2400.0).map { it.second.code })
        assertEquals(2936.0, p.surface("front").marks[0].x1)
        p.surface("ceil").marks.add(SurveyPrep.Mark("b", "B", "CB1", 0.0, 230.0, 0.0, 3990.0))
        // H 2000 also takes FD1 (2100 high) past the front wall's top; CB1 still fits the ceiling
        assertEquals(listOf("FE1", "FD1"), p.outside(4000.0, 2900.0, 2000.0).map { it.second.code })
        assertEquals("FE1 is outside the face: edit or delete it (+1 more)", p.outsideNote(4000.0, 2900.0, 2000.0))
        assertTrue(SurveyPrep.outOf(SurveyPrep.Mark("x", "X", "X", 0.0, 2900.4, 0.0, 10.0), 2900.0, 2400.0).not())
        assertTrue(SurveyPrep.outOf(SurveyPrep.Mark("x", "X", "X", 0.0, 2900.6, 0.0, 10.0), 2900.0, 2400.0))
    }

    @Test fun `every face follows the new size, and the capture keeps it in inches`() {
        val (l, w, h) = Triple(4000.0, 2900.0, 2400.0)
        assertEquals(listOf(2900.0, 2400.0), SurveyPrep.size("front", l, w, h).toList())
        assertEquals(listOf(4000.0, 2400.0), SurveyPrep.size("right", l, w, h).toList())
        assertEquals(listOf(2900.0, 4000.0), SurveyPrep.size("floor", l, w, h).toList())
        assertEquals(listOf(2900.0, 4000.0), SurveyPrep.size("ceil", l, w, h).toList())
        val inch = SurveyPrep.inches(doubleArrayOf(l, w, h))
        assertEquals(4000.0 / 25.4, inch[0], 1e-12)
        // and reopening the capture from those inches gives the size back (when the prep file is gone)
        val back = SurveyPrep.reopenSize(null, inch[0], inch[1], inch[2])!!
        assertEquals(listOf(l, w, h), back.map { Math.round(it * 1e6) / 1e6 })
    }
}
