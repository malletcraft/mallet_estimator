package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch.Box
import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch.Hit
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * THE PICKED ITEM'S CODE CHIP (port 63 slice I2a; DESIGN-imagemeter §2.2, §3 "The hit order", §13; the plan critic's
 * finding C8). A tap on it opens the panel that slides up. Until slice I2 the code is drawn INSIDE the box at its
 * top-left, within the `lt` tab's 16 dp reach, and on a switch plate it is larger than the mark. So: where the chip
 * stands ([SurveyPrepTouch.codeChip], [SurveyPrepTouch.codeChipOfLine]) -- outside the box above its top-left corner,
 * or below its bottom-left at the photo's top; at least 44 x 28 dp; 16 dp clear of every tab, corner handle and end --
 * and what it beats ([SurveyPrepTouch.Hit.Code] in [SurveyPrepTouch.hit]): a figure and a body; a handle beats it.
 *
 * The Mi A3's photo in 2 · Mark, 348 x 514 dp, at 2 px a dp. Nothing here is on screen: :app is compiled only by CI.
 * (Test names are plain ASCII: MeasureWalkTest's header says why.)
 */
class CodeChipTest {

    private val dp = 2.0
    private val w = 348 * dp; private val h = 514 * dp
    private val tol = 16 * dp
    /** "FW1 ▾" at 12 sp, bold mono: about 38 dp of text. */
    private val text = 38 * dp
    private fun box(l: Double, t: Double, r: Double, b: Double) = Box(l * dp, t * dp, r * dp, b * dp)
    private fun tabs(bb: Box) = SurveyPrepTouch.handlesOf(bb, false)
    private fun chip(bb: Box, textW: Double = text, lift: Double = 16 * dp, avoid: List<Box> = emptyList(), handles: List<DoubleArray> = tabs(bb)) =
        SurveyPrepTouch.codeChip(bb, handles, textW, w, h, dp, lift, avoid)
    private fun inDp(b: Box) = listOf(b.l / dp, b.t / dp, b.r / dp, b.b / dp)
    private fun clearOf(c: SurveyPrepTouch.Chip, pts: List<DoubleArray>) = pts.minOf { SurveyPrepTouch.boxDist(c.tap, it[0], it[1]) } / dp
    private fun pt(x: Double, y: Double) = doubleArrayOf(x * dp, y * dp)

    /** What must hold of every chip: its tap box at least 44 x 28 dp, 4 dp inside the photo, the drawn chip inside it, 22 dp high. */
    private fun sound(c: SurveyPrepTouch.Chip, what: String) {
        assertTrue(c.tap.w >= 44 * dp - 1e-9 && c.tap.h >= 28 * dp - 1e-9, "$what: the tap box is ${c.tap.w / dp} x ${c.tap.h / dp} dp")
        assertTrue(c.tap.l >= 4 * dp - 1e-9 && c.tap.t >= 4 * dp - 1e-9 && c.tap.r <= w - 4 * dp + 1e-9 && c.tap.b <= h - 4 * dp + 1e-9, "$what: ${c.tap} leaves the photo")
        assertTrue(c.box.l >= c.tap.l - 1e-9 && c.box.r <= c.tap.r + 1e-9 && c.box.t >= c.tap.t - 1e-9 && c.box.b <= c.tap.b + 1e-9, "$what: the chip is drawn outside its tap box")
        assertEquals(22.0, c.box.h / dp, 1e-9, what)
    }

    // ------------------------------------------------------------------ its size

    @Test fun `the chip is 22 dp high, its tap box never under 44 x 28 dp, and it stands a tab's own reach from every handle`() {
        assertEquals(44.0, SurveyPrepTouch.CHIP_TAP_W_DP); assertEquals(28.0, SurveyPrepTouch.CHIP_TAP_H_DP)
        assertEquals(22.0, SurveyPrepTouch.CHIP_H_DP); assertEquals(8.0, SurveyPrepTouch.CHIP_PAD_DP)
        assertEquals(16.0, SurveyPrepTouch.CHIP_CLEAR_DP); assertEquals(DeleteGuard.CLEAR_DP.toDouble(), SurveyPrepTouch.CHIP_CLEAR_DP)
        val bb = box(150.0, 200.0, 240.0, 320.0)
        // its handles: a box mark's eight tabs, a four-corner mark's four corners, a spot's own middle -- what the floating
        // trash, which went in slice I2b, kept clear of
        assertEquals(SurveyPrepTouch.TABS.map { SurveyPrepTouch.tabAt(it, bb.l, bb.r, bb.t, bb.b).toList() }, SurveyPrepTouch.handlesOf(bb, false).map { it.toList() })
        assertEquals(listOf(listOf(195 * dp, 260 * dp)), SurveyPrepTouch.handlesOf(bb, true).map { it.toList() })
        val four = listOf(pt(150.0, 300.0), pt(240.0, 320.0), pt(240.0, 200.0), pt(150.0, 200.0))
        assertEquals(four.map { it.toList() }, SurveyPrepTouch.handlesOf(bb, false, four).map { it.toList() })
        // a spot is its middle whatever corners it is handed: it has none to free
        assertEquals(SurveyPrepTouch.handlesOf(bb, true).map { it.toList() }, SurveyPrepTouch.handlesOf(bb, true, four).map { it.toList() })
        // what the chip reads: the code and the glyph that says it opens something
        assertEquals("FW1 ▾", SurveyPrepTouch.chipText("FW1")); assertEquals("F-T1 ▾", SurveyPrepTouch.chipText("F-T1"))
        // a short code ("FE1 ▾" drawn small): 20 dp of text, a 36 dp chip -- its tap box is still 44 x 28, the chip centred in it
        val small = assertNotNull(chip(bb, 20 * dp))
        assertEquals(listOf(36.0, 22.0), listOf(small.box.w / dp, small.box.h / dp)); assertEquals(listOf(44.0, 28.0), listOf(small.tap.w / dp, small.tap.h / dp))
        assertEquals(4.0, (small.box.l - small.tap.l) / dp, 1e-9); assertEquals(3.0, (small.box.t - small.tap.t) / dp, 1e-9)
        // a long code ("FNOTE12 ▾"): the tap box is as wide as the chip
        val long = assertNotNull(chip(bb, 70 * dp))
        assertEquals(86.0, long.box.w / dp, 1e-9); assertEquals(86.0, long.tap.w / dp, 1e-9); assertEquals(28.0, long.tap.h / dp, 1e-9)
        sound(small, "short"); sound(long, "long")
    }

    // ------------------------------------------------------------------ where a mark's stands

    @Test fun `it stands outside the box above its top-left corner - its left edge the box's, 16 dp over the top edge`() {
        val bb = box(150.0, 200.0, 240.0, 320.0)
        val c = assertNotNull(chip(bb))
        assertEquals("above", c.how); sound(c, "a window")
        assertEquals(54.0, c.box.w / dp, 1e-9)                       // 38 dp of text and 8 either side
        assertEquals(150.0, c.box.l / dp, 1e-9, "the drawn chip's left edge is the box's")
        assertEquals(200.0 - 16, c.tap.b / dp, 1e-9, "its tap box ends 16 dp over the top edge")
        assertTrue(c.tap.b < bb.t, "outside the box")
        // 16 dp clear of all eight tabs: no point of it is within a tab's reach
        assertTrue(clearOf(c, tabs(bb)) >= 16 - 1e-9, "clear of the tabs by ${clearOf(c, tabs(bb))} dp")
    }

    @Test fun `a held mark's chip stands above its hand-anchored width plate, and the figures' layout keeps both`() {
        // a held window on the wall of DimLayoutTest's phone rig: the w plate is anchored by hand over the top edge,
        // clear of the tabs -- and the chip, which is placed FIRST and handed to the layout as a box to keep clear,
        // stands above it, so the gold width plate of a mark just placed stays on its anchor
        val W = 3600.0; val H = 2700.0; val size = 680.0; val u = 680.0 / 480; val d = 2.0
        fun toPx(x: Double, y: Double) = doubleArrayOf(x * size / W, size - y * size / H)
        fun textW(t: String, fs: Double, @Suppress("UNUSED_PARAMETER") bold: Boolean) = t.length * fs * 0.6
        val e = SurveyPrep.Mark("id-W1", "W", "FW1", 1500.0, 2400.0, 900.0, 2100.0)
        val fs = DimLayout.textSize(true, d)
        for ((what, gold) in listOf("just placed, its width gold" to "FW1.w", "held, nothing gold" to null)) {
            val mk = DimLayout.markShown(Tx.EN, emptyMap(), "front", e, W, H, picked = true, held = true, gold = gold, toPx = ::toPx, u = u, dp = d, textW = ::textW)
            assertTrue(mk.tabs, what)
            val lift = SurveyPrepTouch.chipLift(true, fs, u, d)
            assertTrue(lift > 16 * d, "$what: a held mark's chip is lifted over the plate's row (${lift / d} dp)")
            assertEquals(16 * d, SurveyPrepTouch.chipLift(false, fs, u, d), "a mark with no handles: a tab's reach, no more")
            val c = assertNotNull(SurveyPrepTouch.codeChip(mk.box, SurveyPrepTouch.handlesOf(mk.box, false), textW("FW1 ▾", fs, true), size, size, d, lift))
            assertEquals("above", c.how, what)
            val out = DimLayout.placeShown(listOf(mk), size, size, listOf(c.tap), u, d)
            val wp = assertNotNull(out.of("FW1.w"))
            assertEquals(DimLayout.How.HAND, wp.how, "$what: the width plate keeps its hand anchor with the chip in the layout's keep")
            assertEquals(DimLayout.How.HAND, DimLayout.placeShown(listOf(mk), size, size, emptyList(), u, d).of("FW1.w")!!.how)
            assertTrue(c.tap.b <= wp.box.t, "$what: the chip's tap box ${c.tap} ends over the width plate ${wp.box}")
            for (p in out.plates) if (!p.dot) assertFalse(p.box.l < c.tap.r && p.box.r > c.tap.l && p.box.t < c.tap.b && p.box.b > c.tap.t, "$what: ${p.code} ${p.box} lies on the chip ${c.tap}")
            // and with the chip in the plate's own row -- the lift a mark with no handles has -- the layout would have to move the plate
            val low = assertNotNull(SurveyPrepTouch.codeChip(mk.box, SurveyPrepTouch.handlesOf(mk.box, false), textW("FW1 ▾", fs, true), size, size, d))
            assertTrue(low.tap.b > wp.box.t && low.tap.r > wp.box.l, "$what: without the lift the chip would stand in the width plate's row")
        }
    }

    @Test fun `a switch plate's chip is larger than the mark and still 16 dp clear of its tabs - every press is one thing's`() {
        // about 25 x 11 dp on a wall at fit: under its own 44 x 28 dp chip (§16, risk 1)
        val bb = box(160.0, 300.0, 185.0, 311.0)
        val c = assertNotNull(chip(bb)); sound(c, "a switch plate")
        assertEquals("above", c.how)
        assertTrue(c.tap.w > bb.w && c.tap.h > bb.h, "the chip is larger than the mark")
        assertTrue(clearOf(c, tabs(bb)) >= 16 - 1e-9)
        val drawn = listOf(SurveyPrepTouch.Drawn("s1", bb.l, bb.r, bb.t, bb.b))
        // held: every point of the chip's tap box is the chip's; every tab is still its tab; its middle a tab's too (they crowd)
        var n = 0
        var y = c.tap.t
        while (y <= c.tap.b) { var x = c.tap.l; while (x <= c.tap.r) {
            assertTrue(SurveyPrepTouch.hit(drawn, "s1", emptyList(), x, y, tol, dp, chip = c.tap) is Hit.Code, "($x, $y) of the chip"); n++; x += dp } ; y += dp }
        assertTrue(n > 1000, "the whole tap box was walked ($n)")
        for (part in SurveyPrepTouch.TABS) {
            val q = SurveyPrepTouch.tabAt(part, bb.l, bb.r, bb.t, bb.b)
            val hit = SurveyPrepTouch.hit(drawn, "s1", emptyList(), q[0], q[1], tol, dp, chip = c.tap)
            assertTrue(hit is Hit.Mark && hit.part == part, "the $part tab is still the $part tab")
        }
    }

    @Test fun `at the photo's top it stands below the bottom-left corner`() {
        // a ceiling-height window: its top edge 10 dp under the photo's top -- no room for 16 + 28 dp over it
        val bb = box(60.0, 10.0, 180.0, 150.0)
        val c = assertNotNull(chip(bb)); sound(c, "at the top")
        assertEquals("below", c.how)
        assertEquals(60.0, c.box.l / dp, 1e-9); assertEquals(150.0 + 16, c.tap.t / dp, 1e-9)
        assertTrue(clearOf(c, tabs(bb)) >= 16 - 1e-9)
        // a mark that TOUCHES the top, and one whose top is off the photo (zoomed in): below, the same
        assertEquals("below", chip(box(60.0, 0.0, 180.0, 150.0))!!.how); assertEquals("below", chip(box(60.0, -80.0, 180.0, 150.0))!!.how)
        // the first row with room over it: 4 dp of edge, 28 of chip, 16 clear
        assertEquals("below", chip(box(60.0, 47.9, 180.0, 150.0))!!.how); assertEquals("above", chip(box(60.0, 48.0, 180.0, 150.0))!!.how)
    }

    @Test fun `a mark as tall as the photo has it beside its top, and one that fills the photo has it inside, clear of the tabs`() {
        // a floor-to-ceiling wardrobe, near the right: left of it
        val tall = box(200.0, 0.0, 330.0, 514.0)
        val l = assertNotNull(chip(tall)); sound(l, "tall"); assertEquals("left", l.how)
        assertEquals(200.0 - 16, l.tap.r / dp, 1e-9); assertTrue(clearOf(l, tabs(tall)) >= 16 - 1e-9)
        // ... against the left edge: right of it
        val tl = box(0.0, 0.0, 130.0, 514.0)
        val r = assertNotNull(chip(tl)); sound(r, "tall, left"); assertEquals("right", r.how); assertEquals(130.0 + 16, r.tap.l / dp, 1e-9)
        // wall to wall and floor to ceiling, or zoomed into: inside, 16 dp in from the corner, on the mark's own body
        val full = box(0.0, 0.0, 348.0, 514.0)
        val i = assertNotNull(chip(full)); sound(i, "full"); assertEquals("inside", i.how)
        assertEquals(listOf(16.0, 16.0), listOf(i.tap.l / dp, i.tap.t / dp)); assertTrue(clearOf(i, tabs(full)) >= 16 - 1e-9)
        val zoomed = box(-400.0, -300.0, 900.0, 1200.0)
        val z = assertNotNull(chip(zoomed)); sound(z, "zoomed"); assertEquals("inside", z.how); assertEquals(listOf(16.0, 16.0), listOf(z.tap.l / dp, z.tap.t / dp))
        // the chip beats the body it lies on, held or not
        val drawn = listOf(SurveyPrepTouch.Drawn("w", full.l, full.r, full.t, full.b))
        val mx = (i.tap.l + i.tap.r) / 2; val my = (i.tap.t + i.tap.b) / 2
        for (sel in listOf("w", null)) assertTrue(SurveyPrepTouch.hit(drawn, sel, emptyList(), mx, my, tol, dp, chip = i.tap) is Hit.Code, "sel $sel")
        val body = SurveyPrepTouch.hit(drawn, "w", emptyList(), mx, my, tol, dp)
        assertTrue(body is Hit.Mark && body.part == "body", "with no chip that press is the body's")
    }

    @Test fun `it is slid back inside the photo, never towards a handle, and keeps off what is drawn over the photo`() {
        // a narrow mark against the right edge: the chip is slid left, still over the top edge
        val bb = box(320.0, 200.0, 345.0, 300.0)
        val c = assertNotNull(chip(bb)); sound(c, "right edge"); assertEquals("above", c.how)
        assertEquals(348.0 - 4, c.tap.r / dp, 1e-9); assertTrue(clearOf(c, tabs(bb)) >= 16 - 1e-9)
        // ... and one half off the left edge: slid right
        val lb = box(-30.0, 200.0, 20.0, 300.0)
        val lc = assertNotNull(chip(lb)); assertEquals(4.0, lc.tap.l / dp, 1e-9); assertEquals("above", lc.how)
        // fit · grid stand in the photo's top-right corner: a mark under them has its chip below itself
        val column = box(298.0, 6.0, 342.0, 84.0)
        val under = box(290.0, 110.0, 340.0, 170.0)
        assertEquals("above", chip(under)!!.how)
        val k = assertNotNull(chip(under, avoid = listOf(column))); sound(k, "under the buttons"); assertEquals("below", k.how)
        assertFalse(k.tap.l < column.r && k.tap.r > column.l && k.tap.t < column.b && k.tap.b > column.t)
    }

    @Test fun `no chip when the mark is off the photo, when the photo cannot hold one, or when no place is clear of the handles`() {
        assertNull(chip(box(400.0, 100.0, 500.0, 200.0))); assertNull(chip(box(100.0, -200.0, 200.0, -10.0))); assertNull(chip(box(100.0, 600.0, 200.0, 700.0)))
        // a photo narrower than the chip and its edges
        assertNull(SurveyPrepTouch.codeChip(box(5.0, 5.0, 20.0, 20.0), emptyList(), text, 60 * dp, 400 * dp, dp))
        assertNull(SurveyPrepTouch.codeChip(box(5.0, 5.0, 20.0, 20.0), emptyList(), text, 400 * dp, 30 * dp, dp))
        // every place outside is taken by something drawn over the photo, and inside it would lie within a tab's reach:
        // NO chip -- never one a slip on a handle could press (a long press still opens the panel)
        val bb = box(140.0, 240.0, 200.0, 290.0)
        val all = listOf(box(0.0, 0.0, 348.0, 238.0), box(0.0, 292.0, 348.0, 514.0), box(0.0, 0.0, 138.0, 514.0), box(202.0, 0.0, 348.0, 514.0))
        assertNull(chip(bb, avoid = all))
        assertNotNull(chip(bb))
    }

    @Test fun `a point's chip is clear of its middle, and a four-corner mark's keeps its bounding box's top-left clear of every corner`() {
        // an electric point: a 6 dp spot; its handle is its middle, grabbed within 12 dp when picked
        val spot = box(97.0, 397.0, 103.0, 403.0)
        val mid = SurveyPrepTouch.handlesOf(spot, true)
        val c = assertNotNull(chip(spot, handles = mid)); sound(c, "a point"); assertEquals("above", c.how)
        assertTrue(clearOf(c, mid) >= 16 - 1e-9)
        val drawn = listOf(SurveyPrepTouch.Drawn("e1", spot.l, spot.r, spot.t, spot.b, spot = 3 * dp))
        assertTrue(clearOf(c, mid) * dp > SurveyPrepTouch.spotReachPicked(3 * dp, dp), "the chip is outside the picked spot's own reach")
        val onSpot = SurveyPrepTouch.hit(drawn, "e1", emptyList(), 100 * dp, 400 * dp, tol, dp, chip = c.tap)
        assertTrue(onSpot is Hit.Mark && onSpot.part == "body")
        assertTrue(SurveyPrepTouch.hit(drawn, "e1", emptyList(), (c.tap.l + c.tap.r) / 2, c.tap.b - 1, tol, dp, chip = c.tap) is Hit.Code)
        // a spot has no handle, so a chip laid over its middle would beat it: its middle is its BODY (the rule's order,
        // which codeChip never needs)
        assertTrue(SurveyPrepTouch.hit(drawn, "e1", emptyList(), 100 * dp, 400 * dp, tol, dp, chip = box(90.0, 390.0, 134.0, 418.0)) is Hit.Code)
        // a sloping window: its corners q0 … q3 inside its bounding box
        val bb = box(100.0, 200.0, 220.0, 320.0)
        val corners = listOf(pt(100.0, 290.0), pt(220.0, 320.0), pt(220.0, 200.0), pt(100.0, 200.0))
        val q = assertNotNull(chip(bb, handles = SurveyPrepTouch.handlesOf(bb, false, corners))); sound(q, "four corners")
        assertEquals("above", q.how); assertEquals(100.0, q.box.l / dp, 1e-9)
        assertTrue(clearOf(q, corners) >= 16 - 1e-9, "16 dp clear of every corner handle")
    }

    @Test fun `wherever a mark is and whatever its size, its chip is sound and 16 dp clear of all eight tabs`() {
        var n = 0; var none = 0
        for (l in listOf(-60.0, 0.0, 3.0, 40.0, 150.0, 300.0, 330.0)) for (t in listOf(-80.0, 0.0, 6.0, 47.0, 60.0, 250.0, 470.0, 500.0))
            for (bw in listOf(8.0, 25.0, 44.0, 90.0, 200.0, 348.0, 600.0)) for (bh in listOf(8.0, 11.0, 40.0, 120.0, 400.0, 514.0, 800.0))
                for (tw in listOf(20.0, 38.0, 70.0)) for (lift in listOf(16.0, 37.0)) {
                    val bb = box(l, t, l + bw, t + bh)
                    val c = chip(bb, tw * dp, lift * dp)
                    n++
                    if (c == null) { none++; continue }
                    sound(c, "$bb")
                    assertTrue(clearOf(c, tabs(bb)) >= 16 - 1e-6, "$bb: ${c.how} is ${clearOf(c, tabs(bb))} dp from a tab")
                    if (c.how == "above") assertTrue(c.tap.b <= bb.t - lift * dp + 1e-9, "$bb: above, lifted")
                    if (c.how != "inside") assertFalse(c.tap.l < bb.r && c.tap.r > bb.l && c.tap.t < bb.b && c.tap.b > bb.t, "$bb: ${c.how} is outside the box")
                }
        assertTrue(n > 8000, "$n places"); assertTrue(none < n / 4, "$none of $n had no chip")
    }

    // ------------------------------------------------------------------ a Tape's, a Line's, an area's, a chain's

    @Test fun `a Tape's, a Line's, an area's and a chain's chip stands off its first point, clear of every end and away from its own line`() {
        fun line(vararg p: DoubleArray, avoid: List<Box> = emptyList()) = SurveyPrepTouch.codeChipOfLine(p.toList(), text, w, h, dp, avoid)
        // a level tape pulled to the right: its chip stands off its first end, in line with it -- not on the band
        val tape = arrayOf(pt(80.0, 300.0), pt(280.0, 300.0))
        val t = assertNotNull(line(*tape)); sound(t, "a tape"); assertEquals("left", t.how)
        assertEquals(80.0 - 16, t.tap.r / dp, 1e-9); assertEquals(300.0, (t.tap.t + t.tap.b) / 2 / dp, 1e-9)
        assertTrue(clearOf(t, tape.toList()) >= 16 - 1e-9)
        // a Line drawn up from the floor: below its first point; drawn down from the top: above it
        assertEquals("below", line(pt(150.0, 400.0), pt(150.0, 150.0))!!.how); assertEquals("above", line(pt(150.0, 150.0), pt(150.0, 400.0))!!.how)
        // pulled to the left: right of its first point
        assertEquals("right", line(pt(250.0, 300.0), pt(80.0, 300.0))!!.how)
        // ... and with no room there (its first end near the photo's right edge), above: a tie between above and below goes above
        assertEquals("above", line(pt(280.0, 300.0), pt(80.0, 300.0))!!.how)
        // with no room on that side (its first end against the photo's left edge): the next best, above -- still off the line
        val edge = assertNotNull(line(pt(10.0, 300.0), pt(280.0, 300.0))); sound(edge, "at the edge"); assertEquals("above", edge.how)
        // an area: clear of every corner, its outline closed
        val area = arrayOf(pt(100.0, 200.0), pt(220.0, 200.0), pt(220.0, 320.0), pt(100.0, 320.0))
        val a = assertNotNull(line(*area)); sound(a, "an area")
        assertTrue(clearOf(a, area.toList()) >= 16 - 1e-9); assertEquals("above", a.how, "out of its first corner; a tie goes above")
        // a slanting Line: the side it leans away from most
        assertEquals("left", line(pt(100.0, 300.0), pt(260.0, 240.0))!!.how); assertEquals("below", line(pt(100.0, 300.0), pt(140.0, 120.0))!!.how)
        // a chain's points close together: clear of every one of them, not only the first
        val chain = arrayOf(pt(100.0, 400.0), pt(120.0, 400.0), pt(150.0, 400.0), pt(300.0, 400.0))
        val k = assertNotNull(line(*chain)); assertTrue(clearOf(k, chain.toList()) >= 16 - 1e-9)
        // its first point off the photo (zoomed in): off the first point that is on it; none on it: no chip
        val off = assertNotNull(line(pt(-50.0, 300.0), pt(120.0, 300.0), pt(280.0, 300.0)))
        assertTrue(clearOf(off, listOf(pt(120.0, 300.0))) in 16.0 - 1e-9..17.0, "it stands off the point at 120, not the one off the photo")
        assertNull(line(pt(-50.0, 300.0), pt(-10.0, 300.0)))
        // what is drawn over the photo is kept off
        val blocked = assertNotNull(line(*tape, avoid = listOf(box(0.0, 280.0, 70.0, 320.0)))); assertTrue(blocked.how != "left", blocked.how)
        // and it beats the picked Tape's own band, which a press would otherwise move
        val on = SurveyPrepTouch.hit(emptyList(), null, emptyList(), (t.tap.l + t.tap.r) / 2, (t.tap.t + t.tap.b) / 2, tol, dp, body = true, chip = t.tap)
        assertTrue(on is Hit.Code)
        assertTrue(SurveyPrepTouch.hit(emptyList(), null, emptyList(), 180 * dp, 300 * dp, tol, dp, body = true, chip = t.tap) is Hit.Body, "the band is still the band")
    }

    // ------------------------------------------------------------------ the hit order

    @Test fun `a handle beats the chip, and the chip beats a figure, a body and every mark under it`() {
        val bb = box(150.0, 200.0, 240.0, 320.0)
        val other = SurveyPrepTouch.Drawn("door", 20 * dp, 140 * dp, 100 * dp, 500 * dp)
        val win = SurveyPrepTouch.Drawn("win", bb.l, bb.r, bb.t, bb.b)
        val marks = listOf(other, win)
        // an artificial chip laid over the window's top-left corner, where codeChip never puts one: the rule's ORDER
        val over = box(130.0, 180.0, 180.0, 215.0)
        val lt = SurveyPrepTouch.hit(marks, "win", emptyList(), bb.l, bb.t, tol, dp, chip = over)
        assertTrue(lt is Hit.Mark && lt.id == "win" && lt.part == "lt", "the held mark's corner tab keeps its press")
        // ... but only a HELD mark has handles: picked by the walk (selId null), the same point is the chip's
        assertTrue(SurveyPrepTouch.hit(marks, null, emptyList(), bb.l, bb.t, tol, dp, chip = over) is Hit.Code)
        // a held chain's point under the chip keeps its press too
        val nodes = listOf(SurveyPrepTouch.Nodes("k1", listOf(pt(140.0, 190.0), pt(300.0, 190.0))))
        val node = SurveyPrepTouch.hit(marks, null, emptyList(), 140 * dp, 190 * dp, tol, dp, nodes, chip = over)
        assertTrue(node is Hit.Node && node.id == "k1" && node.i == 0)
        // a figure under the chip: the chip
        val tag = SurveyPrepTouch.Tag("FW1.w", box(128.0, 178.0, 170.0, 200.0))
        assertTrue(SurveyPrepTouch.hit(marks, "win", listOf(tag), 135 * dp, 185 * dp, tol, dp, chip = over) is Hit.Code)
        val fig = SurveyPrepTouch.hit(marks, "win", listOf(tag), 135 * dp, 185 * dp, tol, dp)
        assertTrue(fig is Hit.Tag && fig.code == "FW1.w", "with no chip that press is the figure's")
        // another mark's body under the chip (the door, under the chip's left end): the chip
        assertTrue(SurveyPrepTouch.hit(marks, "win", emptyList(), 132 * dp, 210 * dp, tol, dp, chip = over) is Hit.Code)
        val door = SurveyPrepTouch.hit(marks, "win", emptyList(), 132 * dp, 210 * dp, tol, dp)
        assertTrue(door is Hit.Mark && door.id == "door")
        // the picked Tape's body: the chip
        assertTrue(SurveyPrepTouch.hit(marks, null, emptyList(), 135 * dp, 185 * dp, tol, dp, body = true, chip = over) is Hit.Code)
    }

    @Test fun `off the chip every press is what it was, and with no chip the order is unchanged`() {
        val bb = box(150.0, 200.0, 240.0, 320.0)
        val marks = listOf(SurveyPrepTouch.Drawn("door", 20 * dp, 140 * dp, 100 * dp, 500 * dp), SurveyPrepTouch.Drawn("win", bb.l, bb.r, bb.t, bb.b),
            SurveyPrepTouch.Drawn("e1", 297 * dp, 303 * dp, 397 * dp, 403 * dp, spot = 3 * dp))
        val tags = listOf(SurveyPrepTouch.Tag("FW1.w", box(175.0, 160.0, 215.0, 192.0)), SurveyPrepTouch.Tag("FD1.h", box(142.0, 280.0, 174.0, 330.0)))
        val c = assertNotNull(chip(bb, lift = 40 * dp))
        fun said(h: Hit?): String = when (h) {
            null -> "-"; is Hit.Code -> "code"; is Hit.Tag -> "tag ${h.code}"; is Hit.Body -> "body"
            is Hit.Mark -> "mark ${h.id} ${h.part}"; is Hit.Node -> "node ${h.id} ${h.i}"
        }
        var on = 0; var off = 0
        for (sel in listOf("win", "door", "e1", null)) {
            var y = 0.0
            while (y <= h) { var x = 0.0; while (x <= w) {
                val plain = said(SurveyPrepTouch.hit(marks, sel, tags, x, y, tol, dp))
                val with = said(SurveyPrepTouch.hit(marks, sel, tags, x, y, tol, dp, chip = c.tap))
                if (c.tap.contains(x, y)) { on++; assertEquals("code", with, "($x, $y), $sel held") } else { off++; assertEquals(plain, with, "($x, $y), $sel held") }
                assertTrue(plain != "code")
                x += 6 * dp }; y += 6 * dp }
        }
        assertTrue(on > 100 && off > 15000, "$on on the chip, $off off it")
    }

    @Test fun `a tap on the chip is Pick's row 7 - nothing changes and the options open`() {
        val p = SurveyPrep.fresh(3319.0, 2678.0, 2677.0)
        p.surface("front").marks.add(SurveyPrep.Mark("w1", "W", "FW1", 1500.0, 2400.0, 900.0, 2100.0))
        val sc = Pick.Scene(p, PhaseTools.SURVEY, StepRoute.MARK) { s -> SurveyPrep.size(s, 3319.0, 2678.0, 2677.0) }
        for (st in listOf(Pick.State("w1", true, "FW1.w"), Pick.State("w1", false, "FW1.h"), Pick.State("w1", false, null))) {
            val o = Pick.tap(sc, st, Hit.Code)
            assertEquals(st, o.state); assertTrue(o.options); assertEquals(Pick.Pad.KEEP, o.pad); assertNull(o.nudge); assertNull(o.refused)
            assertEquals(Pick.options(st).state, o.state)
        }
        // nothing picked: no chip is drawn, and a Code that still arrives opens nothing
        assertFalse(Pick.tap(sc, Pick.NONE, Hit.Code).options)
    }
}
