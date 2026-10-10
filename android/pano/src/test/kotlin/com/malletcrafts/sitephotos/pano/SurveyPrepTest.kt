package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Survey prep (mock-up v33) ported: each case below is a behaviour the
 * approved mock-up shows, checked against the same numbers.
 */
class SurveyPrepTest {

    // The guest bedroom of the mock-up: L 3319 front->back, W 2678 left->right,
    // H 2677 floor->ceiling, with the camera NOT in the middle and 1340 mm up.
    private val L = 3319.0; private val W = 2678.0; private val H = 2677.0
    private val xMin = -1100.0; private val xMax = xMin + W
    private val zMin = -1450.0; private val zMax = zMin + L
    private val yF = -1340.0; private val yC = yF + H

    /** A surface's true corners in 3D, bottom-left, bottom-right, top-right, top-left as seen on it. */
    private fun corners3d(s: String): List<DoubleArray> {
        fun p(x: Double, y: Double, z: Double) = doubleArrayOf(x, y, z)
        return when (s) {
            "front" -> listOf(p(xMin, yF, zMax), p(xMax, yF, zMax), p(xMax, yC, zMax), p(xMin, yC, zMax))
            "right" -> listOf(p(xMax, yF, zMax), p(xMax, yF, zMin), p(xMax, yC, zMin), p(xMax, yC, zMax))
            "back" -> listOf(p(xMax, yF, zMin), p(xMin, yF, zMin), p(xMin, yC, zMin), p(xMax, yC, zMin))
            "left" -> listOf(p(xMin, yF, zMin), p(xMin, yF, zMax), p(xMin, yC, zMax), p(xMin, yC, zMin))
            "floor" -> listOf(p(xMin, yF, zMin), p(xMax, yF, zMin), p(xMax, yF, zMax), p(xMin, yF, zMax))
            else -> listOf(p(xMin, yC, zMin), p(xMax, yC, zMin), p(xMax, yC, zMax), p(xMin, yC, zMax))
        }
    }

    /** A point on a surface, mm from its left edge and up from its bottom edge, to 3D. */
    private fun at(s: String, mx: Double, my: Double): DoubleArray {
        val c = corners3d(s)
        val ex = DoubleArray(3) { (c[1][it] - c[0][it]) }; val ey = DoubleArray(3) { (c[3][it] - c[0][it]) }
        val nx = hypot(hypot(ex[0], ex[1]), ex[2]); val ny = hypot(hypot(ey[0], ey[1]), ey[2])
        return DoubleArray(3) { c[0][it] + ex[it] / nx * mx + ey[it] / ny * my }
    }

    private fun px(s: String, p: DoubleArray) = assertNotNull(SurveyPrep.proj(s, p[0], p[1], p[2]))

    /** Lines a careful hand would set: exactly through the true corners. */
    private fun trueLines(s: String): Map<String, Array<DoubleArray>> {
        val q = corners3d(s).map { px(s, it) }
        return mapOf("bottom" to arrayOf(q[0], q[1]), "top" to arrayOf(q[3], q[2]),
            "left" to arrayOf(q[0], q[3]), "right" to arrayOf(q[1], q[2]))
    }

    private fun sz(s: String) = SurveyPrep.size(s, L, W, H)

    private fun close(exp: Double, got: Double, tol: Double, what: String) =
        assertTrue(abs(exp - got) <= tol, "$what: expected $exp, got $got")

    @Test fun `each surface squares to the laser size it is named for`() {
        assertEquals(listOf(W, H), sz("front").toList()); assertEquals(listOf(W, H), sz("back").toList())
        assertEquals(listOf(L, H), sz("left").toList()); assertEquals(listOf(L, H), sz("right").toList())
        assertEquals(listOf(W, L), sz("floor").toList()); assertEquals(listOf(W, L), sz("ceil").toList())
        assertEquals("W" to "H", SurveyPrep.sizeTag("front")); assertEquals("L" to "H", SurveyPrep.sizeTag("right"))
        assertEquals("W" to "L", SurveyPrep.sizeTag("ceil"))
    }

    @Test fun `ray and proj are inverse on every view`() {
        for (s in SurveyPrep.SURFACES) {
            val d = SurveyPrep.dimOf(s)
            for ((u, v) in listOf(0.1 to 0.2, 0.5 to 0.5, 0.9 to 0.7)) {
                val r = SurveyPrep.ray(s, u * d[0], v * d[1])
                val p = px(s, doubleArrayOf(r[0] * 3, r[1] * 3, r[2] * 3))
                close(u * d[0], p[0], 1e-9, "$s x"); close(v * d[1], p[1], 1e-9, "$s y")
            }
        }
        // front at the top of the floor and the ceiling, left on the left
        for (s in listOf("floor", "ceil")) {
            val y = if (s == "floor") -1.0 else 1.0
            assertTrue(px(s, doubleArrayOf(0.0, y, 1.0))[1] < SurveyPrep.PS / 2.0, "$s: front above the middle")
            assertTrue(px(s, doubleArrayOf(-1.0, y, 0.0))[0] < SurveyPrep.PS / 2.0, "$s: left left of the middle")
        }
    }

    @Test fun `true lines give a frame whose corners are exactly the laser sizes`() {
        for (s in SurveyPrep.SURFACES) {
            val (a, b) = sz(s).let { it[0] to it[1] }
            val f = assertNotNull(SurveyPrep.frame(trueLines(s), a, b), s)
            val want = listOf(0.0 to 0.0, a to 0.0, a to b, 0.0 to b)
            f.q.forEachIndexed { i, q ->
                val m = SurveyPrep.apply(f.p2m, q)
                close(want[i].first, m[0], 1e-6, "$s corner $i x"); close(want[i].second, m[1], 1e-6, "$s corner $i y")
            }
            // any point on the surface reads back its true mm: the map is exact for a plane
            for ((mx, my) in listOf(300.0 to 900.0, a / 2 to b / 3, a - 120 to b - 40)) {
                val m = SurveyPrep.apply(f.p2m, px(s, at(s, mx, my)))
                close(mx, m[0], 1e-6, "$s x"); close(my, m[1], 1e-6, "$s y")
                // and the other way, through the ray, lands on the same direction in the room
                val v = SurveyPrep.apply(f.m2p, doubleArrayOf(mx, my))
                val r = SurveyPrep.ray(s, v[0], v[1])
                val t = at(s, mx, my)
                val nr = hypot(hypot(r[0], r[1]), r[2]); val nt = hypot(hypot(t[0], t[1]), t[2])
                for (k in 0..2) close(t[k] / nt, r[k] / nr, 1e-9, "$s ray $k")
            }
        }
    }

    @Test fun `a laser-sized box gives back exactly the laser sizes and no offsets`() {
        for (s in SurveyPrep.SURFACES) {
            val (a, b) = sz(s).let { it[0] to it[1] }
            val f = SurveyPrep.frame(trueLines(s), a, b)!!
            val bl = SurveyPrep.apply(f.p2m, f.q[0]); val tr = SurveyPrep.apply(f.p2m, f.q[2])
            val m = SurveyPrep.Mark("x", "X", "XX1", bl[0], tr[0], bl[1], tr[1])
            val ms = SurveyPrep.measures(s, m, a, b).associateBy { it.key }
            close(a, ms.getValue("w").est!!, 1e-6, "$s width"); close(b, ms.getValue("h").est!!, 1e-6, "$s height")
            assertTrue(ms.keys.none { it in SurveyPrep.OFFS }, "$s: touching every side, nothing to measure from: ${ms.keys}")
        }
    }

    @Test fun `homography round trip`() {
        val src = listOf(doubleArrayOf(12.0, 30.0), doubleArrayOf(800.0, 55.0), doubleArrayOf(760.0, 690.0), doubleArrayOf(40.0, 610.0))
        val dst = listOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(2678.0, 0.0), doubleArrayOf(2678.0, 2677.0), doubleArrayOf(0.0, 2677.0))
        val h = SurveyPrep.homography(src, dst)!!; val g = SurveyPrep.homography(dst, src)!!
        for (i in 0 until 4) {
            val m = SurveyPrep.apply(h, src[i]); close(dst[i][0], m[0], 1e-6, "x$i"); close(dst[i][1], m[1], 1e-6, "y$i")
        }
        val p = doubleArrayOf(333.3, 444.4); val back = SurveyPrep.apply(g, SurveyPrep.apply(h, p))
        close(p[0], back[0], 1e-6, "x"); close(p[1], back[1], 1e-6, "y")
        // three points in a row cannot be squared
        assertNull(SurveyPrep.homography(listOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(1.0, 1.0), doubleArrayOf(2.0, 2.0), doubleArrayOf(3.0, 3.0)), dst))
    }

    @Test fun `lines that do not close make no frame, nor does a size off the laser`() {
        val par = mapOf("bottom" to arrayOf(doubleArrayOf(0.0, 700.0), doubleArrayOf(900.0, 700.0)),
            "top" to arrayOf(doubleArrayOf(0.0, 100.0), doubleArrayOf(900.0, 100.0)),
            "left" to arrayOf(doubleArrayOf(0.0, 650.0), doubleArrayOf(900.0, 650.0)),
            "right" to arrayOf(doubleArrayOf(800.0, 100.0), doubleArrayOf(800.0, 700.0)))
        assertNull(SurveyPrep.frame(par, W, H))
        assertNull(SurveyPrep.frame(trueLines("front"), 0.0, H))
        assertNull(SurveyPrep.cross(par.getValue("bottom"), par.getValue("top")))
    }

    @Test fun `a niche in the corner at floor level lists only width, height and depth`() {
        val m = SurveyPrep.Mark("n", "N", "FN1", 0.0, 900.0, 0.0, 1200.0)
        assertEquals(listOf("w", "h", "p"), SurveyPrep.measures("front", m, W, H).map { it.key })
        assertEquals(listOf("width", "height", "depth into wall"), SurveyPrep.measures("front", m, W, H).map { it.what })
        // the dropped ones are only the zero ones
        assertEquals(listOf("w", "h", "l", "s", "p"), SurveyPrep.measures0("front", m, W, H).map { it.key })
    }

    @Test fun `positions are measured from the nearer corner and the nearer line`() {
        val near = SurveyPrep.Mark("n", "N", "FN1", 2000.0, 2500.0, 1800.0, 2400.0)
        val ms = SurveyPrep.measures("front", near, W, H).associate { it.key to it.est }
        close(W - 2500, ms.getValue("r")!!, 1e-9, "right corner"); assertFalse("l" in ms)
        close(H - 2400, ms.getValue("t")!!, 1e-9, "ceiling"); assertFalse("s" in ms)
        val far = SurveyPrep.Mark("n", "N", "FN2", 300.0, 800.0, 200.0, 600.0)
        val mf = SurveyPrep.measures("front", far, W, H).associate { it.key to it.est }
        close(300.0, mf.getValue("l")!!, 1e-9, "left corner"); close(200.0, mf.getValue("s")!!, 1e-9, "floor")
        // on a plan: left/right wall and front/back wall
        val plan = SurveyPrep.Mark("c", "C", "GC1", 2000.0, 2300.0, 2900.0, 3100.0)
        val mp = SurveyPrep.measures("floor", plan, W, L).associate { it.key to it.what }
        assertEquals("from right wall", mp["r"]); assertEquals("from front wall", mp["f"])
        val plan2 = SurveyPrep.Mark("c", "C", "GC2", 100.0, 400.0, 150.0, 450.0)
        val mp2 = SurveyPrep.measures("floor", plan2, W, L).associate { it.key to it.what }
        assertEquals("from left wall", mp2["l"]); assertEquals("from back wall", mp2["k"])
    }

    @Test fun `a window always gives its sill from the floor`() {
        val high = SurveyPrep.Mark("w", "W", "FW1", 700.0, 1900.0, 1800.0, 2500.0)
        val ms = SurveyPrep.measures("front", high, W, H)
        assertEquals(listOf("w", "h", "l", "s", "p"), ms.map { it.key })
        assertEquals("sill, from floor", ms.first { it.key == "s" }.what)
        close(1800.0, ms.first { it.key == "s" }.est!!, 1e-9, "sill")
        assertNull(ms.first { it.key == "p" }.est, "reveal depth is site only")
    }

    @Test fun `depths, projections and drops are site only`() {
        val col = SurveyPrep.Mark("c", "C", "FC1", W - 450, W - 150, 0.0, H)
        val mc = SurveyPrep.measures("front", col, W, H)
        assertEquals(listOf("w", "r", "p"), mc.map { it.key }); assertNull(mc.last().est)
        val beam = SurveyPrep.Mark("b", "B", "FB1", 0.0, W, H - 400, H)
        val mb = SurveyPrep.measures("front", beam, W, H)
        assertEquals(listOf("d", "p"), mb.map { it.key }); close(400.0, mb[0].est!!, 1e-9, "drop")
        val step = SurveyPrep.Mark("s", "S", "GS1", 889.0, 1789.0, 150.0, 450.0)
        assertNull(SurveyPrep.measures("floor", step, W, L).last { it.key == "p" }.est)
        assertEquals("height", SurveyPrep.measures("floor", step, W, L).last().what)
    }

    @Test fun `a loft gives width, its height, underside, nearer corner and depth`() {
        val d = SurveyPrep.defaults("front", "L", W, H)
        val loft = SurveyPrep.Mark("l", "L", "FL1", 0.0, 1200.0, d[2], d[3])
        val ms = SurveyPrep.measures("front", loft, W, H)
        assertEquals(listOf("w", "h", "s", "p"), ms.map { it.key })
        assertEquals(listOf("width", "loft height", "underside, from floor", "loft depth"), ms.map { it.what })
        close(H - 600, ms[2].est!!, 1e-9, "underside")
    }

    @Test fun `a plan electric point is measured to its centre, even in a corner`() {
        val e = SurveyPrep.Mark("e", "E", "GE1", 0.0, 150.0, 0.0, 150.0)
        val ms = SurveyPrep.measures("floor", e, W, L)
        assertEquals(listOf("l", "k"), ms.map { it.key })
        assertEquals("from left wall, to centre", ms[0].what); close(75.0, ms[0].est!!, 1e-9, "centre")
        // a wall's electric point is a box like any other, so in a corner it drops out
        val we = SurveyPrep.Mark("e", "E", "FE1", 0.0, 150.0, 0.0, 150.0)
        assertEquals(listOf("w", "h"), SurveyPrep.measures("front", we, W, H).map { it.key })
    }

    @Test fun `marks snap to a boundary on a drag, never on a nudge`() {
        val o = SurveyPrep.Mark("n", "N", "FN1", 500.0, 1400.0, 750.0, 1950.0)
        // a drag 480 mm left lands 20 mm off the corner, and snaps onto it
        val dragged = o.copy(); SurveyPrep.moveMark(dragged, o, "body", -480.0, 0.0, W, H); SurveyPrep.snapMark(dragged, "body", W, H)
        close(0.0, dragged.x0, 0.0, "x0"); close(900.0, dragged.x1, 0.0, "width kept")
        // the same 480 mm by buttons stays 20 mm off
        val nudged = o.copy(); SurveyPrep.moveMark(nudged, nudged.copy(), "body", -480.0, 0.0, W, H)
        close(20.0, nudged.x0, 1e-9, "x0")
        // 30 mm is outside the snap
        val far = o.copy(); SurveyPrep.moveMark(far, o, "body", -470.0, 0.0, W, H); SurveyPrep.snapMark(far, "body", W, H)
        close(30.0, far.x0, 1e-9, "x0")
        // an edge snaps alone and the other stays put
        val edge = o.copy(); SurveyPrep.moveMark(edge, o, "t", 0.0, H - 1950 - 10, W, H); SurveyPrep.snapMark(edge, "t", W, H)
        close(H, edge.y1, 0.0, "top"); close(750.0, edge.y0, 0.0, "bottom")
    }

    @Test fun `moving keeps a mark on the surface and resizing keeps it at least 20 mm`() {
        val o = SurveyPrep.Mark("n", "N", "FN1", 500.0, 1400.0, 750.0, 1950.0)
        val e = o.copy(); SurveyPrep.moveMark(e, o, "body", 5000.0, -5000.0, W, H)
        close(W, e.x1, 1e-9, "right"); close(0.0, e.y0, 1e-9, "floor"); close(900.0, e.x1 - e.x0, 1e-9, "w")
        val r = o.copy(); SurveyPrep.moveMark(r, o, "rb", -5000.0, 5000.0, W, H)
        close(520.0, r.x1, 1e-9, "min width"); close(1930.0, r.y0, 1e-9, "min height")
    }

    @Test fun `a nudge moves a line that many mm on the surface`() {
        for (s in listOf("front", "right", "floor", "ceil")) {
            val (a, b) = sz(s).let { it[0] to it[1] }
            val lines = trueLines(s); val f = SurveyPrep.frame(lines, a, b)!!
            val up = SurveyPrep.nudgeLine(lines.getValue("bottom"), "bottom", f, 5.0)!!
            val mid = doubleArrayOf((up[0][0] + up[1][0]) / 2, (up[0][1] + up[1][1]) / 2)
            close(5.0, SurveyPrep.apply(f.p2m, mid)[1], 0.05, "$s bottom +5")
            val right = SurveyPrep.nudgeLine(lines.getValue("left"), "left", f, 1.0)!!
            val mr = doubleArrayOf((right[0][0] + right[1][0]) / 2, (right[0][1] + right[1][1]) / 2)
            close(1.0, SurveyPrep.apply(f.p2m, mr)[0], 0.01, "$s left +1")
            val down = SurveyPrep.nudgeLine(lines.getValue("top"), "top", f, -5.0)!!
            val md = doubleArrayOf((down[0][0] + down[1][0]) / 2, (down[0][1] + down[1][1]) / 2)
            close(b - 5.0, SurveyPrep.apply(f.p2m, md)[1], 0.05, "$s top -5")
        }
        assertNull(SurveyPrep.nudgeLine(trueLines("front").getValue("bottom"), "bottom", null, 5.0))
    }

    @Test fun `a turn is about the middle and undoes itself`() {
        val l = arrayOf(doubleArrayOf(100.0, 600.0), doubleArrayOf(800.0, 620.0))
        val t = SurveyPrep.turnLine(l, 0.1); val back = SurveyPrep.turnLine(t, -0.1)
        for (i in 0..1) for (k in 0..1) close(l[i][k], back[i][k], 1e-9, "p$i$k")
        close(450.0, (t[0][0] + t[1][0]) / 2, 1e-9, "mid x"); close(610.0, (t[0][1] + t[1][1]) / 2, 1e-9, "mid y")
        assertTrue(t[1][1] > l[1][1], "positive turns clockwise on the picture")
    }

    @Test fun `a fresh room starts with every surface closed and none set`() {
        val p = SurveyPrep.fresh(L, W, H)
        for (s in SurveyPrep.SURFACES) {
            val (a, b) = sz(s).let { it[0] to it[1] }
            assertNotNull(SurveyPrep.frame(p.surface(s).lines, a, b), s)
            assertFalse(p.surface(s).done())
            assertTrue(p.surface(s).marks.isEmpty())
        }
        assertFalse(p.done())
        for (s in SurveyPrep.SURFACES) for (k in SurveyPrep.SLOTS) p.surface(s).set[k] = true
        assertTrue(p.done())
    }

    @Test fun `codes are surface, type and number, never handed out twice`() {
        val p = SurveyPrep.fresh(L, W, H)
        val w1 = p.addMark("front", "W", W, H, "a"); val w2 = p.addMark("front", "W", W, H, "b")
        assertEquals("FW1", w1.code); assertEquals("FW2", w2.code)
        assertEquals("CB1", p.addMark("ceil", "B", W, L, "c").code)
        assertEquals("GS1", p.addMark("floor", "S", W, L, "d").code)
        assertEquals("RD1", p.addMark("right", "D", L, H, "e").code)
        p.actual["FW1.w"] = 1203.0; p.actual["FW2.w"] = 1190.0
        p.deleteMark("a")
        assertNull(p.actual["FW1.w"], "a deleted mark's readings go with it"); assertEquals(1190.0, p.actual["FW2.w"])
        assertEquals("FW3", p.addMark("front", "W", W, H, "f").code)
        assertEquals("front" to w2, p.markOf("FW2.h")?.let { it.first to it.second })
        assertNull(p.markOf("FW1.w"))
    }

    @Test fun `a reading moves the active field to the next one unmeasured`() {
        val p = SurveyPrep.fresh(L, W, H)
        val w = p.addMark("front", "W", W, H, "a")
        assertEquals(listOf("FW1.w", "FW1.h", "FW1.l", "FW1.s", "FW1.p"), p.keysOf("front", w, W, H))
        assertEquals("FW1.w", p.nextOpen("front", w, null, W, H))
        p.actual["FW1.h"] = 1200.0
        assertEquals("FW1.l", p.nextOpen("front", w, "FW1.w", W, H))
        p.actual["FW1.p"] = 150.0
        // wraps to the first still open
        assertEquals("FW1.w", p.nextOpen("front", w, "FW1.s", W, H))
        for (k in p.keysOf("front", w, W, H)) p.actual[k] = 1.0
        assertNull(p.nextOpen("front", w, null, W, H))
    }

    @Test fun `defaults are as in the mock-up`() {
        val win = SurveyPrep.defaults("front", "W", W, H)
        assertEquals(listOf((W - 1200) / 2, (W - 1200) / 2 + 1200, 900.0, 2100.0), win.toList())
        val door = SurveyPrep.defaults("front", "D", W, H)
        assertEquals(listOf(300.0, 1200.0, 0.0, 2100.0), door.toList())
        val beam = SurveyPrep.defaults("ceil", "B", W, L)
        assertEquals(listOf((W - 230) / 2, (W - 230) / 2 + 230, 0.0, L), beam.toList())
        assertEquals(listOf("W", "D", "C", "B", "O", "N", "L", "E", "X"), SurveyPrep.types("front").map { it.letter })
        assertEquals(listOf("C", "S", "O", "E", "X"), SurveyPrep.types("floor").map { it.letter })
        assertEquals(listOf("B", "C", "E", "O", "X"), SurveyPrep.types("ceil").map { it.letter })
    }

    @Test fun `the squared picture maps mm and pixels both ways`() {
        val sq = SurveyPrep.Square.screen(W, H)
        close(0.04 * W, sq.mX, 1e-9, "margin"); assertEquals(960, sq.width)
        close(sq.mpp, (W + 2 * sq.mX) / 960, 1e-12, "mpp")
        close(123.0, sq.mmX(sq.x(123.0)), 1e-9, "x"); close(456.0, sq.mmY(sq.y(456.0)), 1e-9, "y")
        assertTrue(sq.y(0.0) > sq.y(H), "up is up")
        val el = SurveyPrep.Square.elevation(L, H, 1600, 0.06)
        close(0.06 / 1.12, el.x(0.0) / el.width, 1e-9, "left margin as Face Prep reads it")
        close(0.06 / 1.12, el.y(H) / el.height, 2e-3, "top margin as Face Prep reads it")
    }

    @Test fun `render samples the room where the frame says`() {
        // a 360 whose colour says which way it looks: red up the sky, green round the yaw
        val pw = 720; val ph = 360
        val px = IntArray(pw * ph) { i -> val x = i % pw; val y = i / pw; ((y * 255 / (ph - 1)) shl 16) or ((x * 255 / (pw - 1)) shl 8) }
        val pano = Panorama.Image(pw, ph, px)
        for (s in SurveyPrep.SURFACES) {
            val (a, b) = sz(s).let { it[0] to it[1] }
            val f = SurveyPrep.frame(trueLines(s), a, b)!!
            val sq = SurveyPrep.Square.screen(a, b)
            val img = SurveyPrep.render(pano, s, f, sq)
            val col = sq.width / 3; val row = sq.height / 2
            val want = WallCorners.sample(pano, at(s, sq.mmX(col + 0.5), sq.mmY(row + 0.5)))
            assertEquals(want, img.pixels[row * sq.width + col], s)
            // flipped, the same point is on the mirrored row
            val fl = SurveyPrep.render(pano, s, f, sq, flipY = true)
            assertEquals(want, fl.pixels[(sq.height - 1 - row) * sq.width + col], "$s flipped")
        }
    }

    @Test fun `a kept capture reopens on the sizes its prep was squared to`() {
        val saved = doubleArrayOf(L, W, H)
        assertEquals(saved.toList(), SurveyPrep.reopenSize(saved, 100.0, 100.0, 100.0)!!.toList())
        // no saved prep: the capture's own sizes, inches to mm
        val fromCapture = SurveyPrep.reopenSize(null, 130.0, 105.0, 105.4)!!
        close(3302.0, fromCapture[0], 1e-9, "L"); close(2667.0, fromCapture[1], 1e-9, "W"); close(2677.16, fromCapture[2], 1e-9, "H")
        // a saved size that is not a laser size is not used
        close(3302.0, SurveyPrep.reopenSize(doubleArrayOf(0.0, W, H), 130.0, 105.0, 105.4)!![0], 1e-9, "fallback")
        // nothing measured at all: nothing to square to
        assertNull(SurveyPrep.reopenSize(null, 0.0, 0.0, 0.0))
        assertNull(SurveyPrep.reopenSize(null, 130.0, 105.0, 0.0))
    }

    @Test fun `the local copy goes only once the bench has its own`() {
        assertTrue(SurveyPrep.localCopyRemovable("SYNCED"))
        for (st in listOf("LOCAL", "ERROR", "UPLOADING", "")) assertFalse(SurveyPrep.localCopyRemovable(st), st)
    }
}
