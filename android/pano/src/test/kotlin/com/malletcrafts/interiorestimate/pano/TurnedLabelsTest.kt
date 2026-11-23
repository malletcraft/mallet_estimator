package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch.Box
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * EVERY FIGURE RUNS ALONG ITS DIMENSION (port 63 slice I1c; DESIGN-imagemeter §21, P5). Amit, after using 0.3.345: "no
 * way to set alignment of measurements to line direction. its always horizontal which overlaps on other measurements."
 * The turn a label is drawn at ([LabelAngle]) and the layout that places every plate -- a mark's and a Tape's, Line's,
 * area's or chain's alike -- by its TURNED rectangle ([DimLayout.placeShown], [DimLayout.Turned]).
 *
 * A 340 px canvas, 1 px a dp and a unit, monospace figures 0.6 of their size a character -- the rig of DimLayoutTest.
 * (Test names are plain ASCII: MeasureWalkTest's header says why.)
 */
class TurnedLabelsTest {

    private val S = 340.0; private val U = 1.0; private val DP = 1.0
    private val textW = { t: String, fs: Double, _: Boolean -> t.length * fs * 0.6 }
    private fun pt(x: Double, y: Double) = doubleArrayOf(x, y)
    private fun place(ms: List<DimLayout.Mark>, keep: List<Box> = emptyList()) = DimLayout.placeShown(ms, S, S, keep, U, DP)
    private fun line(id: String, text: String, a: DoubleArray, b: DoubleArray, gold: Boolean = false) =
        DimLayout.item(id, "$id.len", text, a, b, false, gold, true, DP, textW)!!
    private fun near(e: Double, g: Double, what: String, tol: Double = 1e-9) = assertTrue(abs(e - g) <= tol, "$what: expected $e, got $g")

    // ------------------------------------------------------------------ the turn

    @Test fun `a label's turn is its line's own angle, folded so it is never upside down, and the same both ways along the line`() {
        assertEquals(LabelAngle.LEVEL, LabelAngle.of(0.0, 0.0, 100.0, 0.0)); assertEquals(LabelAngle.LEVEL, LabelAngle.of(100.0, 0.0, 0.0, 0.0))
        assertEquals(LabelAngle.UP, LabelAngle.of(0.0, 0.0, 0.0, 100.0)); assertEquals(LabelAngle.UP, LabelAngle.of(0.0, 100.0, 0.0, 0.0))
        assertEquals(0.0, LabelAngle.LEVEL); assertEquals(-90.0, LabelAngle.UP)
        // a line rising to the right on the canvas (y down) reads up along it; one falling to the right reads down along it
        near(-45.0, LabelAngle.of(0.0, 100.0, 100.0, 0.0), "rising"); near(45.0, LabelAngle.of(0.0, 0.0, 100.0, 100.0), "falling")
        near(-45.0, LabelAngle.of(100.0, 0.0, 0.0, 100.0), "rising, drawn the other way")
        // every direction round the circle, a degree at a time: always upright, and the reverse line gives the same turn
        for (d in 0 until 360) {
            val r = Math.toRadians(d.toDouble()); val x = 100 * cos(r); val y = 100 * sin(r)
            val a = LabelAngle.of(0.0, 0.0, x, y)
            assertTrue(LabelAngle.upright(a), "$d deg -> $a"); assertTrue(a > -95.0 && a <= 85.0, "$d deg -> $a")
            near(a, LabelAngle.of(x, y, 0.0, 0.0), "$d deg reversed", 1e-6)
            // the turn lies along the line: the line's direction is the label's, or its reverse
            val al = LabelAngle.along(a); assertTrue(abs(abs(al[0] * x / 100 + al[1] * y / 100) - 1) < 1e-9, "$d deg")
        }
        // the level, the upright: exact unit vectors, so a mark's plates turn by whole quarters as they always have
        assertEquals(listOf(1.0, 0.0), LabelAngle.along(0.0).toList()); assertEquals(listOf(0.0, -1.0), LabelAngle.along(-90.0).toList())
        assertEquals(listOf(0.0, 1.0), LabelAngle.across(0.0).toList()); assertEquals(listOf(1.0, 0.0), LabelAngle.across(-90.0).toList())
        assertEquals(0.0, LabelAngle.of(5.0, 5.0, 5.0, 5.0), "a line with no length is level")
    }

    @Test fun `a line within a few degrees of upright reads bottom to top, whichever way it leans`() {
        assertEquals(5.0, LabelAngle.NEAR_UPRIGHT_DEG)
        // leaning a little right or a little left going down the canvas: both read UP, as a turned height plate does
        for (lean in listOf(-4.9, -3.0, -0.5, 0.5, 3.0, 4.9)) {
            val r = Math.toRadians(90.0 + lean)
            val a = LabelAngle.of(0.0, 0.0, cos(r), sin(r))
            assertTrue(a < -85.0 && a > -95.0, "lean $lean -> $a"); near(-90.0 + lean, a, "lean $lean", 1e-6)
        }
        // past the few degrees on the one side the fold falls the other way: the text then reads down, still upright
        near(85.0, LabelAngle.of(0.0, 0.0, cos(Math.toRadians(85.0)), sin(Math.toRadians(85.0))), "85 deg", 1e-6)
        near(80.0, LabelAngle.fold(-100.0), "fold"); near(-90.0, LabelAngle.fold(90.0), "fold"); near(-90.0, LabelAngle.fold(270.0), "fold")
        near(0.0, LabelAngle.fold(180.0), "fold"); near(0.0, LabelAngle.fold(-180.0), "fold"); near(-30.0, LabelAngle.fold(150.0), "fold")
    }

    // ------------------------------------------------------------------ the turned rectangle

    @Test fun `a turned rectangle - its corners, its bounding box, and two level or upright ones overlap exactly as their boxes do`() {
        val r = DimLayout.Turned(100.0, 100.0, 20.0, 5.0, -90.0)
        r.bounds.let { assertEquals(listOf(95.0, 80.0, 105.0, 120.0), listOf(it.l, it.t, it.r, it.b)) }
        // at -90 its text runs UP: the along-start is at the bottom
        r.corners().let { c -> near(120.0, c[0][1], "start below"); near(80.0, c[1][1], "end above") }
        val t = DimLayout.Turned(0.0, 0.0, 10.0, 5.0, 30.0)
        for (c in t.corners()) near(hypot(10.0, 5.0), hypot(c[0], c[1]), "corner radius", 1e-9)
        // level rectangles on a grid of offsets: hits() is exactly the box test the old rule used, apart included
        fun boxHit(a: Box, b: Box, apart: Double) = a.l < b.r + apart && a.r > b.l - apart && a.t < b.b + apart && a.b > b.t - apart
        val a = Box(100.0, 100.0, 140.0, 117.0)
        for (dx in -50..50 step 5) for (dy in -25..25 step 5) for (apart in listOf(0.0, 1.0)) {
            val b = Box(100.0 + dx, 100.0 + dy, 130.0 + dx, 117.0 + dy)
            assertEquals(boxHit(a, b, apart), DimLayout.Turned.of(a).hits(DimLayout.Turned.of(b), apart), "$dx $dy $apart")
        }
        // the nearest point of a turned plate to a point beside it lies on its edge
        val n = DimLayout.Turned(0.0, 0.0, 10.0, 5.0, LabelAngle.LEVEL).nearest(0.0, 30.0); assertEquals(listOf(0.0, 5.0), n.toList())
    }

    // ------------------------------------------------------------------ a Line, upright, level, slanting, short

    @Test fun `a slanting Line's label is turned to its angle, centred on it and breaking it`() {
        val out = place(listOf(line("m1", "1240", pt(60.0, 260.0), pt(260.0, 60.0))))
        val p = assertNotNull(out.of("m1.len"))
        assertEquals(DimLayout.How.ON_LINE, p.how); near(-45.0, p.angle, "its turn", 1e-9); assertTrue(p.turned)
        // centred on the line's middle, along it: its length is the text's plate, its height across the line
        val r = p.rect; near(160.0, r.cx, "x"); near(160.0, r.cy, "y")
        near(DimLayout.plateW(textW("1240", 10.0, true), DP), p.pw, "along"); near(DimLayout.plateH(10.0, DP), p.ph, "across")
        // box is the turned plate's bounding box, and the tap box is grown from it
        r.bounds.let { b -> near(b.l, p.box.l, "l"); near(b.t, p.box.t, "t"); near(b.r, p.box.r, "r"); near(b.b, p.box.b, "b") }
        assertTrue(p.tap.w >= 32.0 && p.tap.h >= 32.0); assertNull(p.leader); assertFalse(p.arrowsIn)
        assertEquals(listOf("m1.len"), out.tags.map { it.code })
    }

    @Test fun `an upright Line reads bottom to top whichever way it was drawn, and a level one lies level`() {
        for ((a, b) in listOf(pt(100.0, 60.0) to pt(100.0, 260.0), pt(100.0, 260.0) to pt(100.0, 60.0))) {
            val p = place(listOf(line("m1", "2400", a, b))).of("m1.len")!!
            assertEquals(DimLayout.How.ON_LINE, p.how); assertEquals(LabelAngle.UP, p.angle); assertTrue(p.turned)
            near(100.0, p.rect.cx, "on the line"); near(160.0, p.rect.cy, "mid-span")
            // upright: its box is the plate stood up, ph wide and pw tall
            near(p.ph, p.box.w, "w"); near(p.pw, p.box.h, "h")
        }
        val lv = place(listOf(line("m2", "905", pt(60.0, 200.0), pt(280.0, 200.0)))).of("m2.len")!!
        assertEquals(DimLayout.How.ON_LINE, lv.how); assertEquals(LabelAngle.LEVEL, lv.angle); assertFalse(lv.turned)
        near(170.0, lv.rect.cx, "x"); near(200.0, lv.rect.cy, "y"); near(lv.pw, lv.box.w, "w"); near(lv.ph, lv.box.h, "h")
    }

    @Test fun `two crossing Lines whose labels would overlap level do not when each is turned along its own line`() {
        // two steep Lines that cross above their middles, which stand 30 px apart at one height (-55 and -75 degrees)
        fun dir(deg: Double) = Math.toRadians(deg).let { doubleArrayOf(cos(it), sin(it)) }
        val da = dir(-55.0); val db = dir(-75.0); val half = 120.0
        val ma = pt(150.0, 200.0); val mb = pt(180.0, 200.0)
        val a = line("a", "12400", pt(ma[0] - da[0] * half, ma[1] - da[1] * half), pt(ma[0] + da[0] * half, ma[1] + da[1] * half))
        val b = line("b", "12400", pt(mb[0] - db[0] * half, mb[1] - db[1] * half), pt(mb[0] + db[0] * half, mb[1] + db[1] * half))
        // they DO cross, within their lengths
        val la = a.figs[0].line!!; val lb = b.figs[0].line!!
        fun side(s: DimLayout.Seg, x: Double, y: Double) = (s.x2 - s.x1) * (y - s.y1) - (s.y2 - s.y1) * (x - s.x1)
        assertTrue(side(la, lb.x1, lb.y1) * side(la, lb.x2, lb.y2) < 0 && side(lb, la.x1, la.y1) * side(lb, la.x2, la.y2) < 0, "they cross")
        val out = place(listOf(a, b))
        val pa = out.of("a.len")!!; val pb = out.of("b.len")!!
        // BOTH sit on their own lines, mid-span, turned to them
        assertEquals(DimLayout.How.ON_LINE, pa.how); assertEquals(DimLayout.How.ON_LINE, pb.how)
        near(150.0, pa.rect.cx, "a x"); near(180.0, pb.rect.cx, "b x"); near(-55.0, pa.angle, "a", 1e-9); near(-75.0, pb.angle, "b", 1e-9)
        assertFalse(pa.rect.hits(pb.rect, DimLayout.APART_U * U), "the turned plates lie clear of each other")
        // LEVEL, the same two plates at the same middles would lie on each other
        val lvA = DimLayout.Turned(150.0, 200.0, pa.pw / 2, pa.ph / 2, 0.0); val lvB = DimLayout.Turned(180.0, 200.0, pb.pw / 2, pb.ph / 2, 0.0)
        assertTrue(lvA.hits(lvB, 0.0), "level they overlap")
        // ... and so would their BOUNDING boxes: which is why the layout keeps turned rectangles apart, not boxes
        assertTrue(DimLayout.Turned.of(pa.box).hits(DimLayout.Turned.of(pb.box), 0.0), "their bounding boxes overlap")
        // the old level rule, handed the same two, cannot keep the second on its line's middle
        val old = DimLayout.place(listOf(a, b), DimLayout.Scope.ALL, S, S, emptyList(), U, DP)
        old.of("b.len")!!.let { assertFalse(it.how == DimLayout.How.ON_LINE && abs((it.box.l + it.box.r) / 2 - 180.0) < 1e-6 && abs((it.box.t + it.box.b) / 2 - 200.0) < 1e-6) }
    }

    @Test fun `a short Line's label steps off it on a leader, square to the line, and only when there is no room along it`() {
        // 20 px long, a 34 px plate: no room on it
        val out = place(listOf(line("m1", "1240", pt(150.0, 150.0), pt(170.0, 150.0))))
        val p = out.of("m1.len")!!
        assertEquals(DimLayout.How.OFF_LINE, p.how); assertTrue(p.arrowsIn); assertEquals(LabelAngle.LEVEL, p.angle)
        val g = assertNotNull(p.leader)
        // from the line's middle, straight up to the plate's nearest edge
        near(160.0, g.x1, "from"); near(150.0, g.y1, "from"); near(160.0, g.x2, "to"); near(p.box.b, g.y2, "to the plate")
        // a slanting short one: off its line ACROSS it, still turned to it
        val q = place(listOf(line("m2", "1240", pt(150.0, 170.0), pt(164.0, 156.0)))).of("m2.len")!!
        assertEquals(DimLayout.How.OFF_LINE, q.how); near(-45.0, q.angle, "turned", 1e-9)
        val gl = q.leader!!; val v = doubleArrayOf(gl.x2 - gl.x1, gl.y2 - gl.y1)
        near(0.0, v[0] * LabelAngle.along(-45.0)[0] + v[1] * LabelAngle.along(-45.0)[1], "the leader is square to the line", 1e-6)
        // the same plate with room along its line stays on it: steps off only when there is none
        assertEquals(DimLayout.How.ON_LINE, place(listOf(line("m3", "1240", pt(100.0, 150.0), pt(160.0, 150.0)))).of("m3.len")!!.how)
    }

    @Test fun `the one being measured on a crowded line is placed first and keeps the middle, and nothing lies on another turned label`() {
        val ms = listOf(
            line("a", "1111", pt(40.0, 300.0), pt(300.0, 40.0)), line("b", "2222", pt(40.0, 290.0), pt(290.0, 40.0)),
            line("c", "?", pt(60.0, 300.0), pt(300.0, 60.0), gold = true), line("d", "3333", pt(170.0, 20.0), pt(170.0, 320.0)))
        val out = place(ms)
        out.of("c.len")!!.let { assertEquals(DimLayout.How.ON_LINE, it.how); near(180.0, it.rect.cx, "c on its middle"); near(180.0, it.rect.cy, "c") }
        val solid = out.plates.filter { !it.dot }
        for (i in solid.indices) for (j in i + 1 until solid.size) assertFalse(solid[i].rect.hits(solid[j].rect, 0.0), "${solid[i].code} on ${solid[j].code}")
        for (p in solid) assertTrue(p.box.l >= 2 - 1e-9 && p.box.t >= 2 - 1e-9 && p.box.r <= S - 2 + 1e-9 && p.box.b <= S - 2 + 1e-9, "${p.code} inside: ${p.box}")
    }

    // ------------------------------------------------------------------ a mark's plates

    private val W = 2678.0; private val H = 2677.0
    private fun toPx(x: Double, y: Double) = doubleArrayOf(x * S / W, S - y * S / H)
    private fun shown(e: SurveyPrep.Mark, actual: Map<String, Double>, picked: Boolean, held: Boolean = false, gold: String? = null) =
        DimLayout.markShown(Tx.EN, actual, "front", e, W, H, picked, held, gold, ::toPx, U, DP, textW)

    @Test fun `a mark's width lies level on its line and its height and every upright position figure run bottom to top up theirs`() {
        val win = SurveyPrep.Mark("w", "W", "FW1", 900.0, 1800.0, 900.0, 2100.0)
        val read = mapOf("FW1.w" to 900.0, "FW1.h" to 1200.0, "FW1.r" to 878.0, "FW1.s" to 900.0, "FW1.p" to 110.0)
        val out = place(listOf(shown(win, read, picked = false)))
        assertEquals(LabelAngle.LEVEL, out.of("FW1.w")!!.angle); assertEquals(LabelAngle.UP, out.of("FW1.h")!!.angle)
        // the corner line runs across: level; the floor line runs up: turned up -- where 0.3.345 stood it upright
        assertEquals(LabelAngle.LEVEL, out.of("FW1.r")!!.angle); assertEquals(LabelAngle.UP, out.of("FW1.s")!!.angle)
        // a `p` has no line: its chip is level
        assertEquals(LabelAngle.LEVEL, out.of("FW1.p")!!.angle)
        for (k in listOf("FW1.w", "FW1.h", "FW1.r", "FW1.s")) assertEquals(DimLayout.How.ON_LINE, out.of(k)!!.how, k)
        // a Beam's drop runs up to the ceiling: turned up
        val beam = SurveyPrep.Mark("b", "B", "FB1", 0.0, W, H - 400, H)
        val bo = place(listOf(shown(beam, mapOf("FB1.d" to 400.0), picked = false)))
        assertEquals(LabelAngle.UP, bo.of("FB1.d")!!.angle)
        // the old rule left the floor figure upright: the one step is the only path that turns it
        val old = DimLayout.place(listOf(DimLayout.mark(read, "front", win, W, H, true, false, null, ::toPx, U, DP, textW)), DimLayout.Scope.ALL, S, S, emptyList(), U, DP)
        assertFalse(old.of("FW1.s")!!.turned); assertEquals(LabelAngle.LEVEL, old.of("FW1.s")!!.angle)
    }

    @Test fun `a four-corner mark's sloping side runs along its slope - its level and upright sides level and up`() {
        val e = SurveyPrep.Mark("q", "W", "FW1", 600.0, 2400.0, 900.0, 2100.0,
            linkedMapOf(Quad.KEY to listOf(listOf(0.0, 0.5), listOf(1.0, 0.0), listOf(1.0, 1.0), listOf(0.0, 1.0))))
        val read = mapOf("FW1.bottom" to 1900.0, "FW1.right" to 1200.0, "FW1.top" to 1800.0, "FW1.left" to 600.0)
        val out = place(listOf(shown(e, read, picked = false)))
        val bottom = MarkDims.of("front", e, W, H).first { it.key == "bottom" }
        val a = toPx(bottom.x1, bottom.y1); val b = toPx(bottom.x2, bottom.y2)
        out.of("FW1.bottom")!!.let { near(LabelAngle.of(a[0], a[1], b[0], b[1]), it.angle, "the slope", 1e-9); assertTrue(it.angle != 0.0 && it.angle != -90.0) }
        assertEquals(LabelAngle.LEVEL, out.of("FW1.top")!!.angle)
        assertEquals(LabelAngle.UP, out.of("FW1.right")!!.angle); assertEquals(LabelAngle.UP, out.of("FW1.left")!!.angle)
    }

    @Test fun `marks and Lines are placed together - no Line's label lies on a mark's plate`() {
        val p = SurveyPrep.fresh(3319.0, W, H)
        val door = SurveyPrep.Mark("d", "D", "FD1", 300.0, 1200.0, 0.0, 2100.0).also { p.surface("front").marks.add(it) }
        p.addMeas("front", SurveyMeas.LINE, listOf(doubleArrayOf(300.0, 2160.0), doubleArrayOf(1200.0, 2160.0)), "m1")
        p.actual["FD1.w"] = 900.0; p.actual["F-M1.len"] = 905.0
        assertTrue(door.code == "FD1")
        val ms = PhotoFigures.layout(Tx.EN, p, "front", W, H, Pick.NONE, ::toPx, U, DP, textW)
        assertEquals(listOf("d", "m1"), ms.map { it.id })
        val out = place(ms)
        val w = out.of("FD1.w")!!; val m = out.of("F-M1.len")!!
        assertEquals("900", w.fig.text); assertEquals("905", m.fig.text)
        assertFalse(w.rect.hits(m.rect, 0.0), "the Line just above the door's top edge gave way to the door's width")
        assertEquals(setOf("FD1.w", "F-M1.len"), out.tags.map { it.code }.toSet())
    }
}
