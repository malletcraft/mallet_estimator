package com.malletcrafts.interiorestimate.pano

import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The floating trash beside a picked item (v52, sp.js placeFTrash; re-ruled in port 63 slice F1). Amit, 16 Nov 2026,
 * with build 0.3.337 on his phone: "navigation button and D2 aer very close where d2 get accidentally fired. same for
 * delete button for any item." The 36 dp disc stood 2 dp off the picked mark's corner -- 10 dp from its corner tab,
 * inside the tab's own 16 dp reach, and the disc took the press first.
 *
 * So the disc is 16 dp (DeleteGuard.CLEAR_DP) clear of the picked item's own handles and of every figure, and where no
 * place is clear of the handles and off every figure there is NO disc. Canvas px at 2 px per dp, on the Mi A3's 340 dp
 * photo.
 */
class TrashSpotTest {

    private val dp = 2.0
    private val w = 340 * dp; private val h = 283 * dp
    private val rad = 18.0
    private fun box(l: Double, t: Double, r: Double, b: Double) = SurveyPrepTouch.Box(l * dp, t * dp, r * dp, b * dp)
    private fun dpOf(s: SurveyPrepTouch.Spot) = listOf(s.left / dp, s.top / dp)
    private fun covers(s: SurveyPrepTouch.Spot, r: SurveyPrepTouch.Box) =
        !(s.left + 36 * dp <= r.l || s.left >= r.r || s.top + 36 * dp <= r.t || s.top >= r.b)
    /** How far the disc's EDGE is from the point [p] (canvas px), dp. */
    private fun edgeTo(s: SurveyPrepTouch.Spot, p: DoubleArray) = hypot(s.left + rad * dp - p[0], s.top + rad * dp - p[1]) / dp - rad
    /** ... from the nearest of [k]'s points and pieces (a piece's own half-width taken off), dp. */
    private fun clearOf(s: SurveyPrepTouch.Spot, k: SurveyPrepTouch.TrashKeep): Double {
        val cx = s.left + rad * dp; val cy = s.top + rad * dp
        val pts = k.points.map { hypot(cx - it[0], cy - it[1]) / dp - rad }
        val body = k.body.map { (p, q) -> (SurveyMeas.segDist(cx, cy, p, q) - k.bodyHalf) / dp - rad }
        return (pts + body).min()
    }
    private fun mark(bb: SurveyPrepTouch.Box) = SurveyPrepTouch.keepOfMark(bb, false)
    private fun pt(x: Double, y: Double) = doubleArrayOf(x * dp, y * dp)

    @Test fun `the disc is 36 dp, and it keeps DeleteGuard's 16 dp by default`() {
        assertEquals(36.0, SurveyPrepTouch.TRASH_DP)
        assertEquals(16, DeleteGuard.CLEAR_DP)
        // a box mark's eight tabs, a spot's own centre
        val bb = box(100.0, 150.0, 140.0, 190.0)
        val k = mark(bb)
        assertEquals(8, k.points.size); assertTrue(k.body.isEmpty())
        assertEquals(SurveyPrepTouch.TABS.map { t -> SurveyPrepTouch.tabAt(t, bb.l, bb.r, bb.t, bb.b).toList() }, k.points.map { it.toList() })
        assertEquals(listOf(listOf(120.0 * dp, 170.0 * dp)), SurveyPrepTouch.keepOfMark(bb, true).points.map { it.toList() })
        // a Tape's two ends and the one piece between them; an area's corners and its closed outline
        val tape = SurveyPrepTouch.keepOfLine(listOf(pt(10.0, 10.0), pt(90.0, 10.0)), false, 7.0)
        assertEquals(2, tape.points.size); assertEquals(1, tape.body.size); assertEquals(7.0, tape.bodyHalf)
        val area = SurveyPrepTouch.keepOfLine(listOf(pt(0.0, 0.0), pt(50.0, 0.0), pt(50.0, 50.0), pt(0.0, 50.0)), true)
        assertEquals(4, area.points.size); assertEquals(4, area.body.size)
        assertTrue(SurveyPrepTouch.keepOfLine(listOf(pt(1.0, 1.0)), false).body.isEmpty())
    }

    @Test fun `with nothing in the way it goes above-right of the mark - 16 dp clear of the corner tab it used to sit on`() {
        val bb = box(100.0, 150.0, 140.0, 190.0)
        val s = assertNotNull(SurveyPrepTouch.trashSpot(bb, emptyList(), w, h, dp, mark(bb)))
        // 7 dp out of the corner along its diagonal: the least whole dp at which the disc's edge is 16 dp from the corner
        assertEquals(listOf(147.0, 107.0), dpOf(s)); assertEquals("above-right", s.how)
        assertTrue(clearOf(s, mark(bb)) >= 16.0, "${clearOf(s, mark(bb))} dp")
        assertTrue(edgeTo(s, pt(140.0, 150.0)) < 18.0, "and no further out than it must be: ${edgeTo(s, pt(140.0, 150.0))} dp from the corner tab")
        // v52 put it 2 dp out: its edge 10.3 dp from the corner tab, inside the 16 dp within which that tab is grabbed
        val v52 = SurveyPrepTouch.Spot(142.0 * dp, 112.0 * dp, "v52")
        assertEquals(10.28, edgeTo(v52, pt(140.0, 150.0)), 0.01)
        // every one of the eight tabs is at least 16 dp from the disc, whatever the mark's size
        for (size in listOf(12.0, 40.0, 90.0, 200.0)) {
            val b2 = box(60.0, 120.0, 60.0 + size, 120.0 + size * 0.6)
            val sp = assertNotNull(SurveyPrepTouch.trashSpot(b2, emptyList(), w, h, dp, mark(b2)), "$size")
            assertTrue(clearOf(sp, mark(b2)) >= 16.0, "$size: ${clearOf(sp, mark(b2))} dp")
        }
    }

    @Test fun `a figure within 16 dp of where it would go sends it to the next candidate, in the prototype's order`() {
        val bb = box(100.0, 150.0, 140.0, 190.0)
        val tags = listOf(box(130.0, 115.0, 190.0, 130.0))            // over above-right
        val s = SurveyPrepTouch.trashSpot(bb, tags, w, h, dp, mark(bb))!!
        assertEquals("above-left", s.how); assertEquals(listOf(57.0, 107.0), dpOf(s))
        val both = tags + box(50.0, 100.0, 100.0, 140.0)              // and over above-left
        assertEquals("below-right", SurveyPrepTouch.trashSpot(bb, both, w, h, dp, mark(bb))!!.how)
        // a figure that is NEAR and not under: 10 dp right of the above-right disc -- v52 kept the disc there, F1 does not
        val near = listOf(box(193.0, 100.0, 240.0, 120.0))
        assertEquals("above-left", SurveyPrepTouch.trashSpot(bb, near, w, h, dp, mark(bb))!!.how)
        // ... and exactly 16 dp off is clear
        assertEquals("above-right", SurveyPrepTouch.trashSpot(bb, listOf(box(199.0, 100.0, 240.0, 120.0)), w, h, dp, mark(bb))!!.how)
    }

    @Test fun `figures crowding the mark - the first place that covers none, never one over a figure`() {
        val bb = box(100.0, 150.0, 140.0, 190.0)
        // all four near places are under a figure, and each of the four further out is within 16 dp of one
        val four = listOf(box(130.0, 115.0, 190.0, 130.0), box(50.0, 100.0, 100.0, 140.0), box(140.0, 190.0, 180.0, 230.0), box(60.0, 190.0, 100.0, 230.0))
        val far = SurveyPrepTouch.trashSpot(bb, four, w, h, dp, mark(bb))!!
        assertEquals(listOf(147.0, 71.0), dpOf(far)); assertEquals("above-right", far.how)
        assertTrue(four.none { covers(far, it) })
        assertTrue(clearOf(far, mark(bb)) >= 16.0)
        // a figure over the whole photo: NO disc -- the prototype's "none clear, the first" put it on the figure
        assertNull(SurveyPrepTouch.trashSpot(bb, listOf(box(0.0, 0.0, 340.0, 283.0)), w, h, dp, mark(bb)))
    }

    @Test fun `kept inside the photo and clear of the fit button - and never clamped onto a tab`() {
        // a mark in the top-right corner: above would be off the photo, so it is clamped 4 dp in -- and with its top
        // under 100 dp, no further right than 104 dp from the right edge: still 16 dp and more from every tab
        val top = box(300.0, 10.0, 330.0, 40.0)
        val s = SurveyPrepTouch.trashSpot(top, emptyList(), w, h, dp, mark(top))!!
        assertEquals(listOf(340.0 - 104, 4.0), dpOf(s)); assertTrue(clearOf(s, mark(top)) >= 16.0)
        // low on the photo the right edge clamps above-right ONTO the mark's top-right tab (v52 left it there, 12 dp
        // from the corner: 300, 162); now that place is refused and the disc goes above-left
        val low = box(300.0, 200.0, 330.0, 230.0)
        val l = SurveyPrepTouch.trashSpot(low, emptyList(), w, h, dp, mark(low))!!
        assertEquals("above-left", l.how); assertEquals(listOf(257.0, 157.0), dpOf(l))
        assertTrue(clearOf(l, mark(low)) >= 16.0)
        val clamped = SurveyPrepTouch.Spot(300.0 * dp, 157.0 * dp, "above-right, clamped")
        assertTrue(edgeTo(clamped, pt(330.0, 200.0)) < 16.0, "the clamped place is ${edgeTo(clamped, pt(330.0, 200.0))} dp from the corner tab")
    }

    @Test fun `a mark off the photo has no trash`() {
        val a = box(-80.0, 10.0, -5.0, 40.0); val b = box(10.0, 300.0, 40.0, 340.0)
        assertNull(SurveyPrepTouch.trashSpot(a, emptyList(), w, h, dp, mark(a)))
        assertNull(SurveyPrepTouch.trashSpot(b, emptyList(), w, h, dp, mark(b)))
    }

    @Test fun `FX10 - beside a picked point, never over or near its two figures, and clear of the point itself`() {
        // a spot at (170, 140) dp, radius 6 dp; its cl tag left of it on the line, its cs tag under it
        val bb = box(164.0, 134.0, 176.0, 146.0)
        val tags = listOf(box(60.0, 122.0, 130.0, 140.0), box(150.0, 190.0, 220.0, 208.0))
        val keep = SurveyPrepTouch.keepOfMark(bb, true)
        val s = SurveyPrepTouch.trashSpot(bb, tags, w, h, dp, keep)!!
        assertTrue(tags.none { covers(s, it) }, dpOf(s).toString())
        // a picked spot is grabbed within 12 dp of its centre: the disc's edge is 16 dp and more from that centre
        assertTrue(clearOf(s, keep) >= 16.0, "${clearOf(s, keep)} dp")
    }

    @Test fun `the disc keeps 16 dp off a picked tape's ends and its band, even where the top-right clamp pushes it`() {
        // a tape along the ceiling line, its right end at (w - 80 dp, 30 dp): the above-right candidate is clamped to
        // the top and pushed left of the fit column, onto the handle
        val l = 340.0 - 300.0; val r = 340.0 - 80.0
        val ends = listOf(pt(l, 30.0), pt(r, 30.0))
        val bb = box(l, 30.0, r, 30.0)
        val half = 3.5 * dp
        val keep = SurveyPrepTouch.keepOfLine(ends, false, half)
        val bare = SurveyPrepTouch.trashSpot(bb, emptyList(), w, h, dp)!!
        assertTrue(edgeTo(bare, ends[1]) < 0, "with nothing to keep off, the clamped disc sits over the end: ${edgeTo(bare, ends[1])} dp")
        val s = SurveyPrepTouch.trashSpot(bb, emptyList(), w, h, dp, keep)!!
        assertTrue(clearOf(s, keep) >= 16.0, "${dpOf(s)} is ${clearOf(s, keep)} dp off the tape")
        assertTrue(ends.all { edgeTo(s, it) >= 16.0 })
        // v52 kept 7.5 dp off an end handle; the tape's BODY takes a press too now (it moves the tape whole)
        assertEquals(listOf(236.0, 75.0), dpOf(s)); assertEquals("below-right", s.how)
        // in the open it is above-right, out of the far end: clear of the end and of the band
        val mid = box(100.0, 150.0, 140.0, 150.0)
        val km = SurveyPrepTouch.keepOfLine(listOf(pt(100.0, 150.0), pt(140.0, 150.0)), false, half)
        val open = SurveyPrepTouch.trashSpot(mid, emptyList(), w, h, dp, km)!!
        assertEquals("above-right", open.how); assertEquals(listOf(149.0, 105.0), dpOf(open))
        assertTrue(clearOf(open, km) >= 16.0, "${clearOf(open, km)} dp")
        // a wider band pushes it further out, by the band
        val wide = SurveyPrepTouch.keepOfLine(listOf(pt(100.0, 150.0), pt(140.0, 150.0)), false, 12.0 * dp)
        val ws = SurveyPrepTouch.trashSpot(mid, emptyList(), w, h, dp, wide)!!
        assertTrue(clearOf(ws, wide) >= 16.0, "${clearOf(ws, wide)} dp"); assertTrue(ws.left > open.left)
    }

    @Test fun `a slanted Line, an area and a chain - the disc is 16 dp from every point and every piece`() {
        // a Line up to the right: the box's above-right corner IS its far end
        val a = pt(80.0, 220.0); val b = pt(200.0, 120.0)
        val line = SurveyPrepTouch.keepOfLine(listOf(a, b), false)
        val s = SurveyPrepTouch.trashSpot(box(80.0, 120.0, 200.0, 220.0), emptyList(), w, h, dp, line)!!
        assertTrue(clearOf(s, line) >= 16.0, "${dpOf(s)}: ${clearOf(s, line)} dp")
        // a Line down to the right: its body crosses the box, and the corners are far from it
        val line2 = SurveyPrepTouch.keepOfLine(listOf(pt(80.0, 120.0), pt(200.0, 220.0)), false)
        val s2 = SurveyPrepTouch.trashSpot(box(80.0, 120.0, 200.0, 220.0), emptyList(), w, h, dp, line2)!!
        assertTrue(clearOf(s2, line2) >= 16.0, "${dpOf(s2)}: ${clearOf(s2, line2)} dp")
        // an area of four corners: clear of its closed outline
        val sq = listOf(pt(100.0, 100.0), pt(180.0, 100.0), pt(180.0, 180.0), pt(100.0, 180.0))
        val area = SurveyPrepTouch.keepOfLine(sq, true)
        val s3 = SurveyPrepTouch.trashSpot(box(100.0, 100.0, 180.0, 180.0), emptyList(), w, h, dp, area)!!
        assertTrue(clearOf(s3, area) >= 16.0, "${dpOf(s3)}: ${clearOf(s3, area)} dp")
        // a chain a room already has: its three points and the two pieces between them
        val nodes = listOf(pt(60.0, 200.0), pt(150.0, 200.0), pt(250.0, 200.0))
        val chain = SurveyPrepTouch.keepOfLine(nodes, false)
        val s4 = SurveyPrepTouch.trashSpot(box(60.0, 200.0, 250.0, 200.0), emptyList(), w, h, dp, chain)!!
        assertTrue(clearOf(s4, chain) >= 16.0, "${dpOf(s4)}: ${clearOf(s4, chain)} dp")
    }

    @Test fun `no place clear of its handles - no disc at all, never one on a handle`() {
        // handles every 30 dp over the whole photo (an item zoomed to fill it, its points everywhere): every candidate
        // is within 16 dp of one
        val grid = (0..12).flatMap { i -> (0..10).map { j -> pt(i * 30.0, j * 30.0) } }
        val bb = box(100.0, 150.0, 140.0, 190.0)
        assertNull(SurveyPrepTouch.trashSpot(bb, emptyList(), w, h, dp, SurveyPrepTouch.TrashKeep(grid)))
        // the same box with its own tabs alone has one
        assertNotNull(SurveyPrepTouch.trashSpot(bb, emptyList(), w, h, dp, mark(bb)))
        // a line right across the photo both ways leaves no place either
        val cross = SurveyPrepTouch.TrashKeep(emptyList(), (0..9).map { j -> pt(0.0, j * 32.0) to pt(340.0, j * 32.0) })
        assertNull(SurveyPrepTouch.trashSpot(bb, emptyList(), w, h, dp, cross))
    }
}
