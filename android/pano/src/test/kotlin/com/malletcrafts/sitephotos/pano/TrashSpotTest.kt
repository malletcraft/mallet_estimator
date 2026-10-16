package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The floating trash beside a picked mark (v52, sp.js placeFTrash): a 36 dp
 * disc that never covers one of the mark's figures, so a tap meant for a
 * figure can never delete the mark (ct52 FX10). Canvas px at 2 px per dp,
 * on the Mi A3's 340 dp photo.
 */
class TrashSpotTest {

    private val dp = 2.0
    private val w = 340 * dp; private val h = 283 * dp
    private fun box(l: Double, t: Double, r: Double, b: Double) = SurveyPrepTouch.Box(l * dp, t * dp, r * dp, b * dp)
    private fun dpOf(s: SurveyPrepTouch.Spot) = listOf(s.left / dp, s.top / dp)
    private fun covers(s: SurveyPrepTouch.Spot, r: SurveyPrepTouch.Box) =
        !(s.left + 36 * dp <= r.l || s.left >= r.r || s.top + 36 * dp <= r.t || s.top >= r.b)

    @Test fun `with nothing in the way it goes above-right of the mark`() {
        val s = assertNotNull(SurveyPrepTouch.trashSpot(box(100.0, 150.0, 140.0, 190.0), emptyList(), w, h, dp))
        assertEquals(listOf(142.0, 112.0), dpOf(s)); assertEquals("above-right", s.how)
    }

    @Test fun `a figure where it would go sends it to the next candidate, in the prototype's order`() {
        val bb = box(100.0, 150.0, 140.0, 190.0)
        val tags = listOf(box(130.0, 115.0, 190.0, 130.0))            // over above-right
        val s = SurveyPrepTouch.trashSpot(bb, tags, w, h, dp)!!
        assertEquals("above-left", s.how); assertEquals(listOf(62.0, 112.0), dpOf(s))
        val both = tags + box(50.0, 100.0, 100.0, 140.0)              // and over above-left
        assertEquals("below-right", SurveyPrepTouch.trashSpot(bb, both, w, h, dp)!!.how)
        // all four near ones covered: one trash-height further out (the first of the outer four that is clear)
        val near4 = both + box(140.0, 190.0, 180.0, 230.0) + box(60.0, 190.0, 100.0, 230.0)
        val far = SurveyPrepTouch.trashSpot(bb, near4, w, h, dp)!!
        assertEquals(listOf(142.0, 76.0), dpOf(far)); assertTrue(near4.none { covers(far, it) })
    }

    @Test fun `nothing clear -- the first candidate, never none`() {
        val bb = box(100.0, 150.0, 140.0, 190.0)
        val s = SurveyPrepTouch.trashSpot(bb, listOf(box(0.0, 0.0, 340.0, 283.0)), w, h, dp)!!
        assertEquals("above-right", s.how)
    }

    @Test fun `kept inside the photo, and clear of the fit button in the top-right corner`() {
        // a mark in the top-right corner: above would be off the photo, so it is clamped 4 dp in --
        // and with its top under 100 dp, no further right than 104 dp from the right edge
        val s = SurveyPrepTouch.trashSpot(box(300.0, 10.0, 330.0, 40.0), emptyList(), w, h, dp)!!
        assertEquals(listOf(340.0 - 104, 4.0), dpOf(s))
        // low on the photo the right edge only keeps it 40 dp in
        val low = SurveyPrepTouch.trashSpot(box(300.0, 200.0, 330.0, 230.0), emptyList(), w, h, dp)!!
        assertEquals(listOf(300.0, 162.0), dpOf(low))
    }

    @Test fun `a mark off the photo has no trash`() {
        assertNull(SurveyPrepTouch.trashSpot(box(-80.0, 10.0, -5.0, 40.0), emptyList(), w, h, dp))
        assertNull(SurveyPrepTouch.trashSpot(box(10.0, 300.0, 40.0, 340.0), emptyList(), w, h, dp))
    }

    @Test fun `FX10 -- beside a picked point, never over its two figures`() {
        // a spot at (170, 140) dp, radius 6 dp; its cl tag left of it on the line, its cs tag under it
        val bb = box(164.0, 134.0, 176.0, 146.0)
        val tags = listOf(box(60.0, 122.0, 130.0, 140.0), box(150.0, 190.0, 220.0, 208.0))
        val s = SurveyPrepTouch.trashSpot(bb, tags, w, h, dp)!!
        assertTrue(tags.none { covers(s, it) }, dpOf(s).toString())
    }

    @Test fun `the disc keeps off the selected tape's end handles, even where the top-right clamp pushes it`() {
        // a tape along the ceiling line, its right end at (w - 80 dp, 30 dp): the above-right candidate is clamped to
        // the top and pushed left of the fit column, onto the handle
        val l = 340.0 - 300.0; val r = 340.0 - 80.0
        val ends = listOf(doubleArrayOf(l * dp, 30 * dp), doubleArrayOf(r * dp, 30 * dp))
        val bb = box(l, 30.0, r, 30.0)
        fun gap(s: SurveyPrepTouch.Spot) = ends.minOf { p -> kotlin.math.hypot(s.left + 18 * dp - p[0], s.top + 18 * dp - p[1]) } / dp - 18
        val old = SurveyPrepTouch.trashSpot(bb, emptyList(), w, h, dp)!!
        assertTrue(gap(old) < 0, "without the handles the disc sits over one: ${gap(old)} dp")
        val keepR = SurveyGuide.HANDLE_CLEAR_DP * dp
        val s = SurveyPrepTouch.trashSpot(bb, emptyList(), w, h, dp, ends, keepR)!!
        assertTrue(gap(s) >= SurveyGuide.HANDLE_CLEAR_DP, "${dpOf(s)} is ${gap(s)} dp off the nearer handle")
        // in the open, the handles do not move it from where the prototype puts it
        val mid = box(100.0, 150.0, 140.0, 150.0)
        val open = SurveyPrepTouch.trashSpot(mid, emptyList(), w, h, dp, listOf(doubleArrayOf(100 * dp, 150 * dp), doubleArrayOf(140 * dp, 150 * dp)), keepR)!!
        assertEquals(dpOf(SurveyPrepTouch.trashSpot(mid, emptyList(), w, h, dp)!!), dpOf(open))
        assertEquals("above-right", open.how)
    }
}
