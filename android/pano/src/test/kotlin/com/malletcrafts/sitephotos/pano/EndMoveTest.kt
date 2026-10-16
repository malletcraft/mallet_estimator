package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * END POINTS THAT MOVE (v52 slice 10, item 8; Amit, 14 Oct 2026 06:30 IST: "i can not move measure line once drawn. i
 * should be able to place it by moving its end points."), against ct52 section T part 5 and FX3/FX13: F-M1's right
 * end dragged to ≈2200 lands level and the length follows (≈1700); an end dragged near FD1's right edge snaps onto it
 * (x = 905.25); an area's corner moves alone; the end stays on the face; a locked end refuses. The fixture is the
 * prototype's front wall 3310 x 2705 with FD1, a door 905.25 wide from the left corner.
 */
class EndMoveTest {

    private val A = 3310.0; private val B = 2705.0
    private val fd1 = SurveyPrep.Mark("fd1", "D", "FD1", 0.0, 905.25, 0.0, 2052.7)
    private val marks = listOf(fd1)

    private fun pt(x: Double, y: Double) = doubleArrayOf(x, y)
    private fun meas(kind: String, code: String, vararg p: DoubleArray, locked: Boolean = false) =
        SurveyPrep.Meas("m-$code", kind, code, p.map { it.copyOf() }.toMutableList(),
            if (locked) mapOf(SurveyPrep.LOCKED to true) else emptyMap())

    /** Moves point [i] of [m] as the screen does: the item's point replaced by the result. */
    private fun drag(m: SurveyPrep.Meas, i: Int, to: DoubleArray, tol: Double = 25.0): SurveyGuide.EndMove? =
        SurveyGuide.endMove("front", m, i, to, A, B, marks, tol)?.also { m.pts[i] = it.p }

    @Test fun `F-M1's right end dragged 1 degree off level lands level at 2200 and the length follows to 1700`() {
        // ct52 T5: "the left end stays, the right end lands level at ≈2200 (snapped), the length follows (≈1700)"
        val m = meas(SurveyMeas.LINE, "F-M1", pt(500.0, 600.0), pt(1500.0, 600.0))
        assertEquals(1000.0, SurveyMeas.plen(m.pts), 1e-9)
        val r = assertNotNull(drag(m, 1, pt(2200.0, 615.0)))
        assertEquals("h", r.snap)
        assertEquals(listOf(500.0, 600.0), m.pts[0].toList(), "the other end stays")
        assertEquals(listOf(2200.0, 600.0), m.pts[1].toList(), "level with the other end")
        assertEquals(1700.0, SurveyMeas.plen(m.pts), 1e-9)
        // the guide runs through the FIXED end, in the Line's blue, bold on the horizontal
        assertEquals(listOf(500.0, 600.0), r.o.toList())
        val g = SurveyGuide.endGuide("front", m, r)
        assertEquals(SurveyPrepPalette.LINE, g.tool); assertEquals(SurveyMeas.LINE_COLOUR, SurveyGuide.colour(g.tool))
        assertEquals("h", g.snap)
    }

    @Test fun `an end dragged near FD1's right edge snaps onto it, x = 905_25`() {
        // ct52 T5: "an end dragged near FD1's right edge snaps onto it (x = 905.25, within 25 mm / 14 dp)"
        val m = meas(SurveyMeas.LINE, "F-M1", pt(500.0, 600.0), pt(1500.0, 600.0))
        drag(m, 0, pt(920.0, 600.0))
        assertEquals(905.25, m.pts[0][0], 0.1)
        assertEquals(905.3, m.pts[0][0], 1e-9, "rounded to 0.1 mm, as sp.js")
        assertEquals(600.0, m.pts[0][1], 1e-9)
        // 935 is 29.75 mm off: outside 25 mm, inside 14 dp when zoomed out (2 px per dp, 1.2 mm per px: 33.6 mm)
        val n = meas(SurveyMeas.LINE, "F-M2", pt(500.0, 600.0), pt(1500.0, 600.0))
        drag(n, 0, pt(935.0, 600.0))
        assertEquals(935.0, n.pts[0][0], 1e-9, "25 mm does not reach")
        val tol = SurveyGuide.endTol(2.0, 1.2)
        assertEquals(33.6, tol, 1e-9)
        drag(n, 0, pt(935.0, 600.0), tol)
        assertEquals(905.3, n.pts[0][0], 1e-9, "14 dp does")
        // and the tag says what it snapped onto
        assertEquals("FD1 right", SurveyGuide.snapCoord("front", A, B, marks, 935.0, 0, tol).tag)
    }

    @Test fun `the snap tolerance is 25 mm close up and 14 dp zoomed out`() {
        assertEquals(25.0, SurveyGuide.endTol(2.75, 0.5), 1e-9)     // 14 dp = 19.25 mm: 25 wins
        assertEquals(77.0, SurveyGuide.endTol(2.75, 2.0), 1e-9)     // 14 dp = 77 mm
    }

    @Test fun `snapCoord snaps to the face's edges and every mark's x0 x1 y0 y1, with its tag`() {
        fun sx(v: Double) = SurveyGuide.snapCoord("front", A, B, marks, v, 0, 25.0)
        fun sy(v: Double) = SurveyGuide.snapCoord("front", A, B, marks, v, 1, 25.0)
        assertEquals("left corner", sx(10.0).tag); assertEquals(0.0, sx(10.0).v)
        assertEquals("right corner", sx(3300.0).tag); assertEquals(A, sx(3300.0).v)
        assertEquals("FD1 right", sx(890.0).tag); assertEquals(905.25, sx(890.0).v)
        assertEquals("floor", sy(-5.0).tag)
        assertEquals("ceiling", sy(2690.0).tag)
        assertEquals("FD1 top", sy(2040.0).tag); assertEquals(2052.7, sy(2040.0).v)
        assertNull(sx(1500.0).tag); assertEquals(1500.0, sx(1500.0).v)
        // a tie: FD1's left edge sits on the left corner, 0 -- the face's edge, first in order, keeps it
        assertEquals("left corner", sx(3.0).tag)
        // the floor and the ceiling name their edges as a plan does
        assertEquals("back edge", SurveyGuide.snapCoord("floor", 2678.0, 3319.0, emptyList(), 4.0, 1, 25.0).tag)
        assertEquals("front edge", SurveyGuide.snapCoord("ceil", 2678.0, 3319.0, emptyList(), 3310.0, 1, 25.0).tag)
        assertEquals("right edge", SurveyGuide.snapCoord("floor", 2678.0, 3319.0, emptyList(), 2670.0, 0, 25.0).tag)
    }

    @Test fun `an area's corner moves alone and the polygon stays closed`() {
        // ct52 T5: "a selected area's corner drags too: only that corner moves"
        val m = meas(SurveyMeas.AREA, "F-M3", pt(1800.0, 300.0), pt(2400.0, 300.0), pt(2400.0, 1000.0), pt(1800.0, 1000.0))
        val before = m.pts.map { it.toList() }
        val r = assertNotNull(drag(m, 2, pt(2560.0, 1350.0)))
        assertNull(r.snap, "a corner has no other end to snap along")
        assertEquals(listOf(2560.0, 1350.0), m.pts[2].toList())
        assertEquals(before.filterIndexed { i, _ -> i != 2 }, m.pts.map { it.toList() }.filterIndexed { i, _ -> i != 2 })
        assertEquals(4, m.pts.size, "still four corners, closed by the area's own last side")
        assertEquals(listOf(2560.0, 1350.0), r.o.toList(), "the guide runs through the corner itself")
        val shoelace = SurveyMeas.parea(m.pts)
        assertTrue(shoelace > 600.0 * 700.0, "bigger than the 600 x 700 it was: $shoelace")
        assertEquals(SurveyMeas.pperim(m.pts), SurveyMeas.plen(m.pts + listOf(m.pts[0])), 1e-9)
    }

    @Test fun `the end is kept inside the face`() {
        val c = meas(SurveyMeas.AREA, "F-M4", pt(1000.0, 1000.0), pt(1500.0, 1000.0), pt(1500.0, 1500.0))
        drag(c, 0, pt(-300.0, 5000.0))
        assertEquals(listOf(0.0, B), c.pts[0].toList())
        val t = meas(SurveyMeas.TAPE, "F-T1", pt(800.0, 1200.0), pt(2000.0, 1200.0))
        val r = assertNotNull(drag(t, 1, pt(4000.0, 1210.0)))
        assertEquals("h", r.snap)
        assertEquals(listOf(A, 1200.0), t.pts[1].toList(), "a tape pulled past the right corner stops on it")
        assertEquals(SurveyPrepPalette.TAPE, SurveyGuide.endGuide("front", t, r).tool)
    }

    @Test fun `a plumb drag snaps up the face onto the other end's vertical`() {
        val t = meas(SurveyMeas.TAPE, "F-T2", pt(1700.0, 100.0), pt(1700.0, 900.0))
        val r = assertNotNull(drag(t, 1, pt(1740.0, 2000.0)))
        assertEquals("v", r.snap)
        assertEquals(listOf(1700.0, 2000.0), t.pts[1].toList())
    }

    @Test fun `a locked end refuses, and says so`() {
        val m = meas(SurveyMeas.LINE, "F-M1", pt(500.0, 600.0), pt(1500.0, 600.0), locked = true)
        assertNull(SurveyGuide.endMove("front", m, 1, pt(2200.0, 600.0), A, B, marks, 25.0))
        assertEquals(listOf(1500.0, 600.0), m.pts[1].toList())
        assertEquals("F-M1 is locked — unlock to move it", SurveyGuide.lockedEnd(m.code))
        val t = meas(SurveyMeas.TAPE, "F-T1", pt(800.0, 1200.0), pt(2000.0, 1200.0), locked = true)
        assertNull(SurveyGuide.endMove("front", t, 0, pt(400.0, 1200.0), A, B, marks, 25.0))
        assertEquals("F-T1 is locked — unlock to move it", SurveyGuide.lockedEnd(t.code))
    }

    @Test fun `an end is grabbed within 14 dp, the nearest`() {
        val scr = listOf(doubleArrayOf(100.0, 100.0), doubleArrayOf(130.0, 100.0))
        val r = SurveyGuide.END_HIT_DP * 2.0     // 2 px per dp: 28 px
        assertEquals(0, SurveyGuide.endAt(scr, 110.0, 100.0, r))
        assertEquals(1, SurveyGuide.endAt(scr, 120.0, 100.0, r))
        assertEquals(1, SurveyGuide.endAt(scr, 150.0, 110.0, r))
        assertNull(SurveyGuide.endAt(scr, 100.0, 140.0, r))
        assertEquals(5.5, SurveyGuide.END_R_DP)
    }
}
