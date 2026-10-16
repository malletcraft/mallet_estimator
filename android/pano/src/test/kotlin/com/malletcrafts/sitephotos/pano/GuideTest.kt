package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * THE PERSPECTIVE GUIDE AND THE PLANE GRID (v52 slice 10, item 7; Amit, 14 Oct 2026 20:45 IST: "i need perpective
 * guidance reference shown when i draw any of these two tools so i will know i am drawing and measuring in correct
 * reference"), against ct52 section T parts 3-5, FX3 and FX11: the two rays run through the start along the face's
 * horizontal and vertical, edge to edge, bold on the snapped axis; the grid steps 100 mm once 100 mm is 12 dp, else
 * 500; the grid fades in .45 → 1 over 150 ms and out over 300 ms; the guide belongs to whatever is being drawn and to
 * nothing left behind.
 */
class GuideTest {

    private fun pt(x: Double, y: Double) = doubleArrayOf(x, y)

    @Test fun `rays on a wall run through the start along the face's horizontal and vertical, edge to edge`() {
        // ct52 T3: a tape pulled from (0, 1200) on the 3310 x 2705 front wall, 1° off level
        val r = SurveyGuide.guideRays(pt(1800.0, 700.0), 3310.0, 2705.0, "h")
        val h = assertNotNull(r.h); val v = assertNotNull(r.v)
        assertEquals(listOf(0.0, 700.0), h.a.toList()); assertEquals(listOf(3310.0, 700.0), h.b.toList())
        assertEquals(listOf(1800.0, 0.0), v.a.toList()); assertEquals(listOf(1800.0, 2705.0), v.b.toList())
        // the snapped-axis flag: the horizontal ray drawn bold (2.8 dp), the vertical at 1.6
        assertTrue(r.bold("h")); assertFalse(r.bold("v"))
        assertEquals(2.8, r.widthDp("h")); assertEquals(1.6, r.widthDp("v"))
        // a diagonal: neither bold
        val d = SurveyGuide.guideRays(pt(500.0, 1600.0), 3310.0, 2705.0, "d")
        assertEquals(1.6, d.widthDp("h")); assertEquals(1.6, d.widthDp("v"))
        // plumb: the vertical bold
        assertTrue(SurveyGuide.guideRays(pt(500.0, 1600.0), 3310.0, 2705.0, "v").bold("v"))
    }

    @Test fun `rays on a plan run left-right and front-back across the floor`() {
        val r = SurveyGuide.guideRays(pt(1000.0, 2000.0), 2678.0, 3319.0)
        assertEquals(listOf(0.0, 2000.0), r.h!!.a.toList()); assertEquals(listOf(2678.0, 2000.0), r.h!!.b.toList())
        assertEquals(listOf(1000.0, 0.0), r.v!!.a.toList()); assertEquals(listOf(1000.0, 3319.0), r.v!!.b.toList())
        assertNull(r.snap)
    }

    @Test fun `rays are clipped to the face - a start on the edge keeps both, one off it loses the ray that would lie off`() {
        // a tape hooked on the left corner (ct52 T3's edge-to-edge pull starts at x = 0)
        val e = SurveyGuide.guideRays(pt(0.0, 1200.0), 3310.0, 2705.0)
        assertNotNull(e.h); assertNotNull(e.v)
        // within the on-face tolerance (max 25 mm, 1 % of 3310 = 33.1 mm) still drawn
        assertNotNull(SurveyGuide.guideRays(pt(-30.0, 1200.0), 3310.0, 2705.0).v)
        // beyond it: that ray is off the face
        val off = SurveyGuide.guideRays(pt(-200.0, 1200.0), 3310.0, 2705.0)
        assertNull(off.v); assertNotNull(off.h)
        assertNull(SurveyGuide.guideRays(pt(1000.0, 3000.0), 3310.0, 2705.0).h)
    }

    @Test fun `the grid steps 100 mm once 100 mm is 12 dp on screen, else 500 - at three zooms`() {
        val dp = 2.0      // the Mi A3: 320 dpi, 2 px per dp
        assertEquals(500, SurveyGuide.gridStep(21.7, dp))           // fit: a 3310 mm wall across 360 dp, 100 mm ≈ 10.9 dp
        assertEquals(100, SurveyGuide.gridStep(12 * dp, dp))        // zoomed until 100 mm is exactly 12 dp
        assertEquals(100, SurveyGuide.gridStep(120.0, dp))          // zoomed in further
        assertEquals(500, SurveyGuide.gridStep(12 * dp - 0.01, dp))
    }

    @Test fun `the grid lines run from 0 to each edge, every 500 strong, the far edge closing it`() {
        val g = SurveyGuide.gridLines(3310.0, 2705.0, 500)
        val xs = g.filter { it.axis == 0 }; val ys = g.filter { it.axis == 1 }
        assertEquals(listOf(0.0, 500.0, 1000.0, 1500.0, 2000.0, 2500.0, 3000.0, 3310.0), xs.map { it.at })
        assertTrue(xs.all { it.strong })
        assertEquals(listOf(0.0, 500.0, 1000.0, 1500.0, 2000.0, 2500.0, 2705.0), ys.map { it.at })
        val f = SurveyGuide.gridLines(1000.0, 400.0, 100)
        assertEquals(11, f.count { it.axis == 0 }, "0 to 1000 by 100 lands on the edge: no extra line")
        assertEquals(listOf(0.0, 500.0, 1000.0), f.filter { it.axis == 0 && it.strong }.map { it.at })
        assertEquals(5, f.count { it.axis == 1 })
        assertTrue(f.filter { it.axis == 1 }.drop(1).none { it.strong }, "100..400 are weak")
    }

    @Test fun `the grid fades in from 45 percent over 150 ms and out over 300 ms`() {
        assertEquals(0.45, SurveyGuide.fadeIn(0.0), 1e-9)
        assertEquals(0.725, SurveyGuide.fadeIn(75.0), 1e-9)
        assertEquals(1.0, SurveyGuide.fadeIn(150.0), 1e-9)
        assertEquals(1.0, SurveyGuide.fadeIn(400.0), 1e-9)
        assertEquals(1.0, SurveyGuide.fadeOut(0.0), 1e-9)
        assertEquals(0.5, SurveyGuide.fadeOut(150.0), 1e-9)
        assertEquals(0.0, SurveyGuide.fadeOut(300.0), 1e-9)
        assertFalse(SurveyGuide.shown(SurveyGuide.fadeOut(295.0)), "under 2 % nothing is drawn: gone within 300 ms")
        assertTrue(SurveyGuide.shown(SurveyGuide.fadeOut(290.0)))
        assertEquals(300, SurveyGuide.fadeOutMs(1.0)); assertEquals(135, SurveyGuide.fadeOutMs(0.45)); assertEquals(1, SurveyGuide.fadeOutMs(0.0))
    }

    @Test fun `the guide comes from the drag, then an end drag, then a Line area being tapped - with the Line on only`() {
        val d = SurveyMeas.Draft("front", SurveyMeas.TAPE, pt(800.0, 1200.0), 3310.0, 2705.0)
        d.move(pt(2000.0, 1215.0))
        val g = assertNotNull(SurveyGuide.now(d, null, null, null, SurveyPrepPalette.TAPE))
        assertEquals("front", g.s); assertEquals(listOf(800.0, 1200.0), g.o.toList())
        assertEquals(SurveyPrepPalette.TAPE, g.tool); assertEquals("h", g.snap); assertFalse(g.off)
        assertEquals(SurveyMeas.TAPE_COLOUR, SurveyGuide.colour(g.tool))
        // a Line pulled off the face: the guide says so (its draft is drawn red)
        val l = SurveyMeas.Draft("front", SurveyMeas.LINE, pt(1000.0, 1500.0), 3310.0, 2705.0); l.move(pt(-220.0, 1500.0))
        val gl = assertNotNull(SurveyGuide.now(l, null, null, null, SurveyPrepPalette.LINE))
        assertTrue(gl.off); assertEquals(SurveyMeas.LINE_COLOUR, SurveyGuide.colour(gl.tool))
        assertEquals(0xff5c5c, SurveyGuide.OFF_COLOUR)
        // an end drag: its own guide, through the fixed end
        val e = SurveyGuide.Guide("front", pt(500.0, 600.0), SurveyPrepPalette.LINE, "h")
        assertSame(e, SurveyGuide.now(null, e, null, null, SurveyPrepPalette.SEL))
        // a Line area tapped: through its last corner -- with Line on; under any other tool, no guide (ct52 FX11)
        val area = listOf(pt(900.0, 1500.0), pt(1400.0, 1500.0))
        assertEquals(listOf(1400.0, 1500.0), SurveyGuide.now(null, null, "front", area, SurveyPrepPalette.LINE)!!.o.toList())
        assertNull(SurveyGuide.now(null, null, "front", area, SurveyPrepPalette.SEL))
        assertNull(SurveyGuide.now(null, null, "front", emptyList(), SurveyPrepPalette.LINE))
        // nothing being drawn: no guide (FX3: a second finger dropped the drag; FX11: leaving the face dropped the draft)
        assertNull(SurveyGuide.now(null, null, null, null, SurveyPrepPalette.TAPE))
    }

    @Test fun `the rays are dashed 9 6 dp with a 5 dp start ring`() {
        assertEquals(9.0, SurveyGuide.DASH_DP); assertEquals(6.0, SurveyGuide.GAP_DP)
        assertEquals(5.0, SurveyGuide.RING_DP); assertEquals(2.0, SurveyGuide.RING_W_DP)
    }
}
