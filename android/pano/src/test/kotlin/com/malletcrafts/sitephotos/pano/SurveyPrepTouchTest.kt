package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Items 14 and 15 of v52, on the phone's own photo box: the Mi A3 is
 * 360 x 780 dp at 2 px per dp, so the photo is 340 dp across in portrait
 * (10 dp padding each side) and 760 x 0.62 * 360 dp in landscape.
 * Ported from the prototype suite's X5 (every tab drags its own edge), X6 and
 * FX1 (the magnifier is never under the finger nor under a button).
 */
class SurveyPrepTouchTest {

    // ------------------------------------------------- X5: every one of the eight tabs

    private val L = 3319.0; private val W = 2678.0; private val H = 2677.0
    private val DP = 2.0

    /** The photo box, px: portrait 340 dp square-ish, landscape 760 x 223 dp, as SpPhoto sizes it. */
    private class View(val bw: Double, val bh: Double, val sq: SurveyPrep.Square, val z: Double, val cx: Double, val cy: Double) {
        val k = SurveyPrep.fitScale(bw, bh, sq.width.toDouble(), sq.height.toDouble()) * z
        val u = min(bw, bh * sq.width / sq.height) / 480
        fun x(mm: Double) = (sq.x(mm) - cx) * k + bw / 2
        fun y(mm: Double) = (sq.y(mm) - cy) * k + bh / 2
        /** mm along x (and up) per screen px */
        val mmPerPx get() = sq.mpp / k
    }

    private fun view(portrait: Boolean, z: Double = 1.0, centreOn: SurveyPrep.Mark? = null): View {
        val sq = SurveyPrep.Square.screen(W, H)
        val (bw, bh) = if (portrait) 340 * DP to 340 * DP / max(1.0, sq.width.toDouble() / sq.height) else 760 * DP to 0.62 * 360 * DP
        val cx = centreOn?.let { sq.x((it.x0 + it.x1) / 2) } ?: (sq.width / 2.0)
        val cy = centreOn?.let { sq.y((it.y0 + it.y1) / 2) } ?: (sq.height / 2.0)
        return View(bw, bh, sq, z, cx, cy)
    }

    /** The picked door's tags as SurveyPrepDraw places them (w, h and the side offset), monospace text 0.6 em wide. */
    private fun tags(v: View, e: SurveyPrep.Mark, hand: Boolean): List<SurveyPrepTouch.Tag> {
        val fs = 12 * v.u; val u = v.u
        val px = v.x(e.x0); val py = v.y(e.y1); val qx = v.x(e.x1); val qy = v.y(e.y0)
        fun w(chars: Int) = chars * 0.6 * fs + 10 * u
        val out = mutableListOf<SurveyPrepTouch.Tag>()
        fun add(code: String, a: SurveyPrepTouch.Anchor, chars: Int) =
            out.add(SurveyPrepTouch.Tag(code, SurveyPrepTouch.tagShape(a, w(chars), fs, u, v.bw, v.bh).tap))
        add("FD1.w", SurveyPrepTouch.widthTag(px, py, qx, fs, u, DP, hand), 8)
        add("FD1.h", SurveyPrepTouch.heightTag(py, qx, qy, fs, u, DP, hand), 9)
        val hy = (py + qy) / 2 + fs * 2.6
        add("FD1.l", SurveyPrepTouch.Anchor((v.x(0.0) + px) / 2, hy - 4 * u, false), 8)
        return out
    }

    private fun drawn(v: View, e: SurveyPrep.Mark) = SurveyPrepTouch.Drawn(e.id, v.x(e.x0), v.x(e.x1), v.y(e.y1), v.y(e.y0))

    /** Each of the eight tabs pressed and dragged 40 dp outward (15 dp across on an edge tab): exactly its own edge(s) move. */
    private fun eightTabs(portrait: Boolean, door: SurveyPrep.Mark, zoom: Double) {
        val tabs = mapOf("tl" to "lt", "t" to "t", "tr" to "rt", "l" to "l", "r" to "r", "bl" to "lb", "b" to "b", "br" to "rb")
        for ((name, part) in tabs) {
            val e = door.copy()
            val v = view(portrait, zoom, if (zoom > 1) e else null)
            val d = drawn(v, e)
            val at = SurveyPrepTouch.tabAt(part, d.x0, d.x1, d.yT, d.yB)
            assertTrue(at[0] in 0.0..v.bw && at[1] in 0.0..v.bh, "$name on screen ($portrait, $zoom×)")
            val h = SurveyPrepTouch.hit(listOf(drawn(v, SurveyPrep.Mark("other", "W", "FW1", 0.0, 400.0, 0.0, 400.0)), d),
                e.id, tags(v, e, hand = true), at[0], at[1], 16 * DP)
            assertIs<SurveyPrepTouch.Hit.Mark>(h, "$name ($portrait, $zoom×)")
            assertEquals(part, h.part, "$name ($portrait, $zoom×)")
            val sdx = if ('l' in part) -40.0 else if ('r' in part) 40.0 else 15.0
            val sdy = if ('t' in part) -40.0 else if ('b' in part) 40.0 else 15.0
            val start = e.copy()
            SurveyPrep.moveMark(e, start, h.part, sdx * DP * v.mmPerPx, -sdy * DP * v.mmPerPx, W, H)
            SurveyPrep.snapMark(e, h.part, W, H)
            val exp = 40 * DP * v.mmPerPx
            fun moved(a: Double, b: Double) = abs(a - b)
            assertEquals(if ('l' in part) exp else 0.0, moved(e.x0, start.x0), 1e-6, "$name x0 ($portrait, $zoom×)")
            assertEquals(if ('r' in part) exp else 0.0, moved(e.x1, start.x1), 1e-6, "$name x1 ($portrait, $zoom×)")
            assertEquals(if ('t' in part) exp else 0.0, moved(e.y1, start.y1), 1e-6, "$name y1 ($portrait, $zoom×)")
            assertEquals(if ('b' in part) exp else 0.0, moved(e.y0, start.y0), 1e-6, "$name y0 ($portrait, $zoom×)")
            if ('l' in part) assertTrue(e.x0 < start.x0); if ('r' in part) assertTrue(e.x1 > start.x1)
            if ('t' in part) assertTrue(e.y1 > start.y1); if ('b' in part) assertTrue(e.y0 < start.y0)
        }
    }

    private val door = SurveyPrep.Mark("d", "D", "FD1", 900.0, 1800.0, 600.0, 2000.0)
    private val small = SurveyPrep.Mark("d", "D", "FD1", 1200.0, 1500.0, 1000.0, 1600.0)

    @Test fun `each of the eight tabs moves exactly its own edges at fit, portrait and landscape`() {
        eightTabs(true, door, 1.0); eightTabs(false, door, 1.0)
    }

    @Test fun `each of the eight tabs moves exactly its own edges zoomed in 2_5x, portrait and landscape`() {
        eightTabs(true, small, 2.5); eightTabs(false, small, 2.5)
    }

    @Test fun `the old width tag sat on the top-centre tab and took it, the picked mark's tags now stand clear`() {
        for (portrait in listOf(true, false)) {
            val v = view(portrait); val d = drawn(v, door)
            val top = SurveyPrepTouch.tabAt("t", d.x0, d.x1, d.yT, d.yB)
            val right = SurveyPrepTouch.tabAt("r", d.x0, d.x1, d.yT, d.yB)
            val old = tags(v, door, hand = false)
            assertTrue(old.first { it.code == "FD1.w" }.box.contains(top[0], top[1]), "the bug: the w tag covered the top-centre tab")
            // even with the old placement the tab now wins: it is tested first and its centre is nearer
            val h = SurveyPrepTouch.hit(listOf(d), door.id, old, top[0], top[1], 16 * DP)
            assertEquals("t", (h as SurveyPrepTouch.Hit.Mark).part)
            // and the drawn boxes of the picked mark's tags stand clear of the tab squares (5 units each way)
            val fs = 12 * v.u; val u = v.u
            val wS = SurveyPrepTouch.tagShape(SurveyPrepTouch.widthTag(d.x0, d.yT, d.x1, fs, u, DP, true), 8 * 0.6 * fs + 10 * u, fs, u, v.bw, v.bh)
            val hS = SurveyPrepTouch.tagShape(SurveyPrepTouch.heightTag(d.yT, d.x1, d.yB, fs, u, DP, true), 9 * 0.6 * fs + 10 * u, fs, u, v.bw, v.bh)
            assertTrue(wS.drawn.b <= top[1] - 5 * u - 5 * DP, "w tag ${wS.drawn} clear above the top tab at ${top[1]}")
            assertTrue(hS.drawn.l >= right[0] + 5 * u + 5 * DP, "h tag ${hS.drawn} clear right of the right tab at ${right[0]}")
        }
    }

    @Test fun `a tap on the middle of a tag still picks its measure and moves nothing`() {
        val v = view(true); val d = drawn(v, door)
        for (t in tags(v, door, hand = true)) {
            val cx = (t.box.l + t.box.r) / 2; val cy = (t.box.t + t.box.b) / 2
            val h = SurveyPrepTouch.hit(listOf(d), door.id, tags(v, door, true), cx, cy, 16 * DP)
            assertIs<SurveyPrepTouch.Hit.Tag>(h); assertEquals(t.code, h.code)
        }
    }

    @Test fun `with nothing picked a tag wins over a mark body, a body moves whole, and a miss is nothing`() {
        val v = view(true); val d = drawn(v, door)
        val mid = doubleArrayOf((d.x0 + d.x1) / 2, (d.yT + d.yB) / 2)
        val h = SurveyPrepTouch.hit(listOf(d), null, emptyList(), mid[0], mid[1], 16 * DP)
        assertEquals("body", (h as SurveyPrepTouch.Hit.Mark).part)
        // an unpicked mark has no tabs: its corner is the body only from inside
        val c = SurveyPrepTouch.hit(listOf(d), null, emptyList(), d.x0 - 4, d.yT - 4, 16 * DP)
        assertEquals(null, c)
        val tg = listOf(SurveyPrepTouch.Tag("FD1.w", SurveyPrepTouch.Box(mid[0] - 10, mid[1] - 10, mid[0] + 10, mid[1] + 10)))
        assertIs<SurveyPrepTouch.Hit.Tag>(SurveyPrepTouch.hit(listOf(d), null, tg, mid[0], mid[1], 16 * DP))
        // the topmost of two overlapping marks takes the body; the PICKED one's tab beats a mark drawn over it
        val over = SurveyPrepTouch.Drawn("top", d.x0 - 50, d.x1, d.yT - 50, d.yB)
        assertEquals("top", (SurveyPrepTouch.hit(listOf(d, over), null, emptyList(), mid[0], mid[1], 16 * DP) as SurveyPrepTouch.Hit.Mark).id)
        val tab = SurveyPrepTouch.hit(listOf(d, over), door.id, emptyList(), d.x0, d.yT, 16 * DP) as SurveyPrepTouch.Hit.Mark
        assertEquals(door.id, tab.id); assertEquals("lt", tab.part)
    }

    // ------------------------------------------------- v52 slice 4: a point is a spot, moved, never resized

    private val fe = SurveyPrep.Mark("fe1", "E", "FE1", 350.0, 400.0, 350.0, 400.0)

    private fun spotDrawn(v: View, e: SurveyPrep.Mark) = SurveyPrepTouch.Drawn(e.id, v.x(e.x0), v.x(e.x1), v.y(e.y1), v.y(e.y0),
        spot = SurveyPrep.SPOT / 2 / v.mmPerPx)

    @Test fun `a picked spot has no tabs -- grabbed whole within 12 dp, never resized, portrait and landscape, zoomed or not`() {
        for (portrait in listOf(true, false)) for (z in listOf(1.0, 2.5)) {
            val v = view(portrait, z, fe); val d = spotDrawn(v, fe)
            val r = max(12 * DP, d.spot!! + 6 * DP)
            // on its box corner, where a box's tab would be: still the body
            val c = SurveyPrepTouch.hit(listOf(d), fe.id, emptyList(), d.x1, d.yT, 16 * DP, DP) as SurveyPrepTouch.Hit.Mark
            assertEquals("body", c.part, "corner $portrait $z")
            // just inside its reach, and just outside it (nothing else there: the photo slides)
            assertIs<SurveyPrepTouch.Hit.Mark>(SurveyPrepTouch.hit(listOf(d), fe.id, emptyList(), d.cx + r - 0.5, d.cy, 16 * DP, DP))
            assertEquals(null, SurveyPrepTouch.hit(listOf(d), fe.id, emptyList(), d.cx + max(r, SurveyPrepTouch.spotReach(d.spot!!, 16 * DP)) + 1, d.cy, 16 * DP, DP))
        }
    }

    @Test fun `an unpicked spot is picked within 16 dp, or its 25 mm and 8 dp more`() {
        val v = view(true, 1.0, fe); val d = spotDrawn(v, fe)
        val r = max(16 * DP, d.spot!! + 8 * DP)
        assertEquals("body", (SurveyPrepTouch.hit(listOf(d), null, emptyList(), d.cx, d.cy - r + 0.5, 16 * DP, DP) as SurveyPrepTouch.Hit.Mark).part)
        assertEquals(null, SurveyPrepTouch.hit(listOf(d), null, emptyList(), d.cx, d.cy - r - 0.5, 16 * DP, DP))
        // zoomed in far, the 25 mm is wider than 16 dp and the spot is picked all over its dot
        val vz = view(true, 8.0, fe); val dz = spotDrawn(vz, fe)
        assertTrue(dz.spot!! > 16 * DP)
        assertIs<SurveyPrepTouch.Hit.Mark>(SurveyPrepTouch.hit(listOf(dz), null, emptyList(), dz.cx + dz.spot!! + 7 * DP, dz.cy, 16 * DP, DP))
    }

    @Test fun `FX10 -- a tag over a picked spot takes the tap when its centre is nearer, and the spot keeps it otherwise`() {
        val v = view(true, 1.0, fe); val d = spotDrawn(v, fe)
        // the cl tag as SurveyPrepDraw places it: centred between the left corner and the spot's centre, just above the line
        val tag = SurveyPrepTouch.Tag("FE1.cl", SurveyPrepTouch.Box(d.cx - 20 * DP, d.cy - 20 * DP, d.cx + 4 * DP, d.cy - 2 * DP))
        val tc = doubleArrayOf((tag.box.l + tag.box.r) / 2, (tag.box.t + tag.box.b) / 2)
        val onTag = SurveyPrepTouch.hit(listOf(d), fe.id, listOf(tag), tc[0], tc[1], 16 * DP, DP)
        assertIs<SurveyPrepTouch.Hit.Tag>(onTag); assertEquals("FE1.cl", onTag.code)
        // at the spot's own centre, still inside the tag's tap box: the spot is nearer
        val onSpot = SurveyPrepTouch.hit(listOf(d), fe.id, listOf(tag), d.cx, d.cy - 2 * DP, 16 * DP, DP)
        assertIs<SurveyPrepTouch.Hit.Mark>(onSpot)
    }

    // ------------------------------------------------- X6 / FX1: the magnifier

    private val dia = 120.0; private val gap = 8.0
    /** The photo, dp: 340 wide in portrait; the fit button in its top-right corner, 6 dp in. */
    private val pw = 340.0; private val ph = 283.0
    private fun fitCol(w: Double) = SurveyPrepTouch.Box(w - 6 - 34, 6.0, w - 6, 32.0)

    private fun clearOf(s: SurveyPrepTouch.Spot, b: SurveyPrepTouch.Box) =
        s.left >= b.r || s.left + dia <= b.l || s.top >= b.b || s.top + dia <= b.t

    private fun under(s: SurveyPrepTouch.Spot, fx: Double, fy: Double) = hypot(fx - (s.left + dia / 2), fy - (s.top + dia / 2)) <= dia / 2

    @Test fun `finger at the top edge -- the far top corner, moved in past the fit button, never below or under the finger`() {
        val ob = fitCol(pw)
        // left half: the far corner is the right one, where the fit button is -- moved in to its left, still at the top
        val a = SurveyPrepTouch.loupePlace(pw * .3, 30.0, pw, ph, dia, gap, 1.0, listOf(ob))
        assertEquals("corner", a.how); assertEquals(8.0, a.top, .5); assertTrue(a.left + dia / 2 > pw / 2)
        assertTrue(clearOf(a, ob)); assertFalse(under(a, pw * .3, 30.0)); assertFalse(a.top > 30.0)
        assertEquals(ob.l - dia - gap, a.left, 1e-9)
        // right half: the left corner, at the gap
        val b = SurveyPrepTouch.loupePlace(pw * .7, 30.0, pw, ph, dia, gap, 1.0, listOf(ob))
        assertEquals("corner", b.how); assertEquals(8.0, b.left, .5); assertEquals(8.0, b.top, .5)
        assertTrue(clearOf(b, ob)); assertFalse(under(b, pw * .7, 30.0))
    }

    @Test fun `finger low on the photo -- above it, 40 dp up, as before`() {
        val s = SurveyPrepTouch.loupePlace(pw * .5, ph * .8, pw, ph, dia, gap, 1.0, listOf(fitCol(pw)))
        assertEquals("above", s.how); assertEquals(40.0, ph * .8 - s.top - dia, 1e-9)
    }

    @Test fun `above but over the fit button -- slid aside off it`() {
        val ob = fitCol(pw)
        val s = SurveyPrepTouch.loupePlace(pw - 20, 170.0, pw, ph, dia, gap, 1.0, listOf(ob))
        assertEquals("above", s.how); assertTrue(clearOf(s, ob)); assertTrue(s.left >= 4 && s.left + dia <= pw - 4)
    }

    @Test fun `the point a drag is measured from, in the far corner -- the other corner`() {
        // landscape (760 x 223 dp): a finger just left of the middle, whose far corner is the right one
        val w = 760.0; val h = 0.62 * 360; val ob = fitCol(w)
        val free = SurveyPrepTouch.loupePlace(379.0, 30.0, w, h, dia, gap, 1.0, listOf(ob))
        assertEquals("corner", free.how); assertTrue(free.left + dia / 2 > w / 2)
        val s = SurveyPrepTouch.loupePlace(379.0, 30.0, w, h, dia, gap, 1.0, listOf(ob), listOf(doubleArrayOf(690.0, 60.0)))
        assertEquals("corner", s.how); assertEquals(gap, s.left, 1e-9); assertEquals(gap, s.top, 1e-9)
        assertTrue(clearOf(s, ob)); assertFalse(under(s, 379.0, 30.0))
        // in portrait both top corners are within reach of any finger up there: beside it, away from the anchor
        val p = SurveyPrepTouch.loupePlace(pw * .7, 30.0, pw, ph, dia, gap, 1.0, listOf(fitCol(pw)), listOf(doubleArrayOf(60.0, 60.0)))
        assertEquals("beside", p.how); assertFalse(under(p, pw * .7, 30.0)); assertTrue(p.top <= 30.0)
    }

    @Test fun `both corners taken -- beside the finger, on the side away from the anchor, never below it`() {
        val w = 600.0; val h = 300.0; val ob = fitCol(w)
        val an = listOf(doubleArrayOf(68.0, 68.0), doubleArrayOf(520.0, 68.0))
        val s = SurveyPrepTouch.loupePlace(300.0, 30.0, w, h, dia, gap, 1.0, listOf(ob), an)
        assertEquals("beside", s.how); assertTrue(s.left > 300.0, "away from the anchor on the left: ${s.left}")
        assertFalse(under(s, 300.0, 30.0)); assertTrue(s.top <= 30.0)
    }

    @Test fun `swept over the whole photo, portrait and landscape -- never under the finger, never below it, never under the button`() {
        for ((w, h) in listOf(pw to ph, 760.0 to 0.62 * 360)) {
            val ob = fitCol(w)
            var fy = 30.0
            while (fy <= h) {
                var fx = 4.0
                while (fx <= w - 4) {
                    val s = SurveyPrepTouch.loupePlace(fx, fy, w, h, dia, gap, 1.0, listOf(ob))
                    val at = "finger $fx,$fy in $w x $h: ${s.how} ${s.left},${s.top}"
                    assertTrue(s.how != "corner-covered", at)
                    assertFalse(under(s, fx, fy), at)
                    assertTrue(s.top <= fy || abs(fx - (s.left + dia / 2)) > dia / 2 + 20, at)
                    assertTrue(clearOf(s, ob), at)
                    assertTrue(s.left >= 4 - 1e-9 && s.left + dia <= w - 4 + 1e-9 && s.top >= 0 && s.top + dia <= h, at)
                    fx += 7
                }
                fy += 9
            }
        }
    }

    @Test fun `two magnifiers -- at the top, or at the bottom when the finger is up there, each off the fit button`() {
        val ob = fitCol(pw)
        val top = SurveyPrepTouch.loupePair(250.0, pw, ph, dia, gap, 1.0, listOf(ob))
        assertTrue(top.left1 == gap)
        for (l in listOf(top.left1, top.left2)) assertTrue(clearOf(SurveyPrepTouch.Spot(l, top.top, ""), ob), "$l ${top.top}")
        val low = SurveyPrepTouch.loupePair(100.0, pw, ph, dia, gap, 1.0, listOf(ob))
        assertEquals(ph - dia - gap, low.top, 1e-9); assertEquals(gap, low.left1); assertEquals(pw - dia - gap, low.left2)
    }
}
