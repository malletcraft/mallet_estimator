package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Pano360.Step as S
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 4 · 360 (v52 slice 17, sp.js 1088-1233): the projection both ways, a surface's mm to a direction and back, which
 * surface a direction is on (with its 1 % slack), the near-plane clipping, the placing check by what a type IS (ct52
 * 2523), the 360's pick order (tabs before chain points), the tape narrowing with distance, and the view's pixels.
 * The synthetic room: L 3319, W 2678, H 2677, its six surfaces bounded by the lines SurveyPrep.fresh draws -- an exact
 * box round the camera, so every surface's edges meet its neighbours'.
 */
class Pano360Test {

    private val L = 3319.0; private val W = 2678.0; private val H = 2677.0
    private val DP = 2.0    // the Mi A3: 320 dpi

    /** Every surface's four lines set: all six bounded. */
    private fun room(): SurveyPrep.Prep = SurveyPrep.fresh(L, W, H).also { p ->
        for (s in SurveyPrep.SURFACES) for (k in SurveyPrep.SLOTS) p.surface(s).set[k] = true }

    private fun frames(p: SurveyPrep.Prep = room()) = Pano360.frames(p, L, W, H)

    // ------------------------------------------------------------------ the view

    @Test fun `the view's basis is WallCorners'`() {
        val v = Pano360.View(37.0, -12.0, 75.0, 680.0, 840.0)
        val b = WallCorners.basis(37.0, -12.0, 75.0, clamp = false)
        for (i in 0 until 3) {
            assertEquals(b.f[i], v.basis.f[i], 1e-12); assertEquals(b.r[i], v.basis.r[i], 1e-12); assertEquals(b.u[i], v.basis.u[i], 1e-12)
        }
        // the middle of the screen looks straight along the view's forward
        val c = v.dirAt(340.0, 420.0)
        for (i in 0 until 3) assertEquals(b.f[i], c[i], 1e-9)
    }

    @Test fun `screen px to a direction and back, to a millipixel, in every corner of the screen`() {
        for (v in listOf(Pano360.View(0.0, 0.0, 90.0, 680.0, 840.0), Pano360.View(250.0, 63.0, 40.0, 1560.0, 640.0),
                         Pano360.View(91.0, -80.0, 110.0, 680.0, 840.0))) {
            for (sx in listOf(0.0, 13.0, v.w / 2, v.w - 1)) for (sy in listOf(0.0, 29.0, v.h / 2, v.h - 1)) {
                val p = v.toS(v.dirAt(sx, sy))
                assertNotNull(p)
                assertEquals(sx, p[0], 1e-3); assertEquals(sy, p[1], 1e-3)
                // ... and it is the point WallCorners.pointOf finds on the same tile
                val q = WallCorners.pointOf(v.basis, v.dirAt(sx, sy))!!
                assertEquals(sx, q.first * v.w, 1e-6); assertEquals(sy, v.h / 2 + (q.second - 0.5) * v.w, 1e-6)
            }
        }
    }

    @Test fun `a direction behind the camera is not on the screen`() {
        val v = Pano360.View(0.0, 0.0, 90.0, 680.0, 840.0)
        assertNull(v.toS(doubleArrayOf(0.0, 0.0, -1.0)))
        assertNull(v.toS(doubleArrayOf(1.0, 0.0, 0.0)))     // straight to the right, 90° off a 90° view
        assertNotNull(v.toS(doubleArrayOf(0.5, 0.0, 1.0)))
    }

    @Test fun `a wall opens level and 90 degrees across, facing it, the floor and ceiling 80 down and up, 110 across`() {
        val f = Pano360.faceView("right", 680.0, 840.0)
        assertEquals(90.0, f.yaw); assertEquals(0.0, f.pitch); assertEquals(90.0, f.fov)
        val fl = Pano360.faceView("floor", 680.0, 840.0); val ce = Pano360.faceView("ceil", 680.0, 840.0)
        assertEquals(-80.0, fl.pitch); assertEquals(80.0, ce.pitch); assertEquals(110.0, fl.fov); assertEquals(110.0, ce.fov)
        // facing a wall, the middle of the screen is on that wall
        val fr = frames()
        for (s in listOf("front", "right", "back", "left", "floor", "ceil")) {
            val v = Pano360.faceView(s, 680.0, 840.0)
            assertEquals(s, Pano360.hitSurface(v.dirAt(340.0, 420.0), fr)?.s, s)
        }
    }

    @Test fun `a drag looks round all the way, and up no further than straight up`() {
        val v = Pano360.View(10.0, 0.0, 90.0, 900.0, 800.0)
        // the finger 100 px right turns the view 10 degrees left (fov / w a px)
        assertEquals(0.0, Pano360.look(v, 10.0, 0.0, 100.0, 0.0).yaw, 1e-9)
        assertEquals(350.0, Pano360.look(v, 10.0, 0.0, 200.0, 0.0).yaw, 1e-9)
        assertEquals(20.0, Pano360.look(v, 10.0, 0.0, -100.0, 0.0).yaw, 1e-9)
        // a drag down tilts it up; never past 90
        assertEquals(30.0, Pano360.look(v, 10.0, 0.0, 0.0, 300.0).pitch, 1e-9)
        assertEquals(90.0, Pano360.look(v, 10.0, 80.0, 0.0, 900.0).pitch)
        assertEquals(-90.0, Pano360.look(v, 10.0, -80.0, 0.0, -900.0).pitch)
        assertEquals(90.0, Pano360.look(v, 10.0, 0.0, 0.0, 0.0).fov)
    }

    @Test fun `a pinch zooms between 40 and 110 degrees`() {
        assertEquals(45.0, Pano360.pinchFov(90.0, 100.0, 200.0), 1e-9)   // fingers apart: zoomed in
        assertEquals(40.0, Pano360.pinchFov(90.0, 100.0, 400.0))
        assertEquals(110.0, Pano360.pinchFov(90.0, 300.0, 100.0))
        assertEquals("90°", Pano360.zoomText(90.0)); assertEquals("47°", Pano360.zoomText(46.6))
        assertEquals("4 · 360 · drag to look round · Front wall", Pano360.caption("front"))
    }

    // ------------------------------------------------------------------ mm <-> direction

    @Test fun `mm to a direction and back to mm within half a millimetre, on every surface`() {
        val fr = frames()
        assertEquals(SurveyPrep.SURFACES.toSet(), fr.keys)
        for (s in SurveyPrep.SURFACES) {
            val f = fr.getValue(s)
            for (x in listOf(0.0, 1.0, f.a / 3, f.a / 2, f.a - 7, f.a)) for (y in listOf(0.0, 2.0, f.b / 4, f.b / 2, f.b - 3, f.b)) {
                val m = Pano360.mmOn(s, f, Pano360.dirOfMm(s, f, x, y))
                assertNotNull(m, "$s $x,$y")
                assertTrue(abs(m[0] - x) < 0.5 && abs(m[1] - y) < 0.5, "$s ($x, $y) came back (${m[0]}, ${m[1]})")
            }
        }
    }

    @Test fun `a plan never takes a direction from the wrong hemisphere`() {
        val fr = frames()
        assertNull(Pano360.mmOn("floor", fr["floor"], doubleArrayOf(0.1, 1.0, 0.2)))     // up is never on the floor
        assertNull(Pano360.mmOn("ceil", fr["ceil"], doubleArrayOf(0.1, -1.0, 0.2)))      // down is never on the ceiling
        assertNull(Pano360.mmOn("floor", fr["floor"], doubleArrayOf(1.0, 0.0, 0.0)))     // level is on neither
        assertNotNull(Pano360.mmOn("floor", fr["floor"], doubleArrayOf(0.1, -1.0, 0.2)))
        // a wall's mm is null behind it, and with no frame
        assertNull(Pano360.mmOn("front", fr["front"], doubleArrayOf(0.0, 0.0, -1.0)))
        assertNull(Pano360.mmOn("front", null, doubleArrayOf(0.0, 0.0, 1.0)))
    }

    @Test fun `only a bounded surface is marked on`() {
        val p = room()
        p.surface("back").set["top"] = false
        val fr = Pano360.frames(p, L, W, H)
        assertFalse("back" in fr); assertEquals(5, fr.size)
        // the middle of the back wall is then on no surface: "Set this surface's lines first"
        assertNull(Pano360.hitSurface(Pano360.faceView("back", 680.0, 840.0).dirAt(340.0, 420.0), fr))
        assertEquals("Set this surface's lines first", Pano360.NO_LINES)
    }

    @Test fun `hitSurface at the corners, and just outside with the 1 % slack`() {
        val all = frames()
        // with only the front wall bounded, 0.5 % past its left edge still counts as on it; 1.5 % does not
        val front = all.getValue("front")
        val only = mapOf("front" to front)
        val a = front.a; val b = front.b
        val near = Pano360.hitSurface(Pano360.dirOfMm("front", front, -0.005 * a, b / 2), only)
        assertNotNull(near); assertEquals("front", near.s); assertEquals(-0.005, near.inside, 1e-6)
        assertNull(Pano360.hitSurface(Pano360.dirOfMm("front", front, -0.015 * a, b / 2), only))
        assertNull(Pano360.hitSurface(Pano360.dirOfMm("front", front, a / 2, 1.015 * b), only))
        assertNotNull(Pano360.hitSurface(Pano360.dirOfMm("front", front, 1.009 * a, 1.009 * b), only))
        // on its four corners exactly it is on the front wall, at the corner
        for ((x, y) in listOf(0.0 to 0.0, a to 0.0, a to b, 0.0 to b)) {
            val h = Pano360.hitSurface(Pano360.dirOfMm("front", front, x, y), only)
            assertNotNull(h); assertEquals("front", h.s)
            assertTrue(abs(h.x - x) < 0.5 && abs(h.y - y) < 0.5)
        }
        // with all six bounded, a corner is claimed by one of the three surfaces that meet there, at ITS corner
        for (s in SurveyPrep.SURFACES) {
            val f = all.getValue(s)
            for ((x, y) in listOf(0.0 to 0.0, f.a to 0.0, f.a to f.b, 0.0 to f.b)) {
                val h = Pano360.hitSurface(Pano360.dirOfMm(s, f, x, y), all)
                assertNotNull(h, "$s corner $x,$y")
                val g = all.getValue(h.s)
                assertTrue(minOf(abs(h.x), abs(h.x - g.a)) < 1 && minOf(abs(h.y), abs(h.y - g.b)) < 1, "$s corner -> ${h.s} ${h.x},${h.y}")
            }
        }
        // just inside an edge shared by two surfaces, the one it is squarely inside wins
        val h = Pano360.hitSurface(Pano360.dirOfMm("front", front, 30.0, b / 2), all)
        assertEquals("front", h?.s)
        val g = Pano360.hitSurface(Pano360.dirOfMm("front", front, -60.0, b / 2), all)
        assertEquals("left", g?.s)
    }

    // ------------------------------------------------------------------ the near plane

    @Test fun `a segment half behind the camera is cut at the near plane, one wholly behind is not drawn`() {
        val v = Pano360.View(0.0, 0.0, 90.0, 680.0, 840.0)
        val ahead = doubleArrayOf(0.2, 0.0, 1.0); val behind = doubleArrayOf(0.2, 0.0, -1.0)
        assertNull(v.seg(behind, doubleArrayOf(-0.4, 0.1, -1.0)))
        val sg = v.seg(ahead, behind)
        assertNotNull(sg)
        // the near end is the point ahead, untouched
        val pa = v.toS(ahead)!!
        assertEquals(pa[0], sg.first[0], 1e-9); assertEquals(pa[1], sg.first[1], 1e-9)
        // the cut end is far off the screen, along the same screen line (here level, to the right): never mirrored
        assertTrue(sg.second[0] > 680.0 * 5); assertEquals(420.0, sg.second[1], 1e-6)
        // a quad in front keeps its four corners; one wholly behind keeps none
        val quad = listOf(doubleArrayOf(-.2, -.2, 1.0), doubleArrayOf(.2, -.2, 1.0), doubleArrayOf(.2, .2, 1.0), doubleArrayOf(-.2, .2, 1.0))
        assertEquals(4, v.clipPoly(quad).size)
        assertTrue(v.clipPoly(quad.map { doubleArrayOf(it[0], it[1], -1.0) }).size < 3)
        // half behind: two corners kept and two new ones on the near plane
        val half = listOf(doubleArrayOf(-.2, -.2, 1.0), doubleArrayOf(.2, -.2, 1.0), doubleArrayOf(.2, .2, -1.0), doubleArrayOf(-.2, .2, -1.0))
        assertEquals(4, v.clipPoly(half).size)
    }

    // ------------------------------------------------------------------ placing by name (ct52 2523)

    @Test fun `a Switch plate cannot go on the floor, nor a Step on a wall -- what a type IS, not its letter`() {
        assertEquals("A Switch plate cannot go on the floor", Pano360.placeRefusal("front", "floor", "S"))
        assertEquals("A Step cannot go on the front wall", Pano360.placeRefusal("floor", "front", "S"))
        assertEquals("A Fan cannot go on the floor", Pano360.placeRefusal("ceil", "floor", "F"))
        assertEquals("A Window cannot go on the ceiling", Pano360.placeRefusal("front", "ceil", "W"))
        assertNull(Pano360.placeRefusal("front", "right", "S"))      // a Switch plate onto another wall
        assertNull(Pano360.placeRefusal("front", "ceil", "F"))       // a Fan from a wall to the ceiling
        assertNull(Pano360.placeRefusal("floor", "ceil", "E")); assertNull(Pano360.placeRefusal("front", "floor", "P"))
        assertTrue(Pano360.canPlace("Switch plate", "back")); assertFalse(Pano360.canPlace("Switch plate", "floor"))
        assertTrue(Pano360.canPlace("Step", "floor")); assertFalse(Pano360.canPlace("Step", "left"))
        assertTrue(Pano360.canPlace("Fan", "ceil")); assertFalse(Pano360.canPlace("Fan", "floor"))
        assertEquals("A Switch plate cannot go on the floor", Pano360.cannotGo("Switch plate", "floor"))
        // every type onto every surface: refused exactly when the surface has no type of that name under that letter
        for (from in SurveyPrep.SURFACES) for (to in SurveyPrep.SURFACES) for (t in SurveyPrep.types(from)) {
            val ok = SurveyPrep.types(to).any { it.letter == t.letter && it.name == t.name }
            assertEquals(ok, Pano360.placeRefusal(from, to, t.letter) == null, "${t.letter} $from -> $to")
            assertEquals(ok, SurveyPrep.canPlace(from, to, t.letter), "${t.letter} $from -> $to")
            // a type moving between walls, or a point between any two surfaces that have it, is never refused
            if (from == to) assertNull(Pano360.placeRefusal(from, to, t.letter))
        }
    }

    // ------------------------------------------------------------------ the pick order

    @Test fun `the 360's pick order -- ends, then the picked mark's tabs BEFORE chain points, then as 2 · Mark`() {
        assertEquals(listOf(S.END, S.TABS, S.NODES, S.MARK, S.LOOK), Pano360.order(SurveyPrepPalette.SEL, false))
        assertEquals(listOf(S.TABS, S.MARK, S.PLACE), Pano360.order("W", false))
        assertEquals(listOf(S.END, S.MEAS), Pano360.order(SurveyPrepPalette.TAPE, false))
        assertEquals(listOf(S.END, S.MEAS), Pano360.order(SurveyPrepPalette.LINE, false))
        assertEquals(listOf(S.NODES, S.CHAIN), Pano360.order(SurveyPrepPalette.CHAIN, false))
        // while a Line area or a chain is being tapped, no end is grabbed: its taps are corners
        assertEquals(listOf(S.MEAS), Pano360.order(SurveyPrepPalette.LINE, true))
        assertEquals(listOf(S.TABS, S.NODES, S.MARK, S.LOOK), Pano360.order(SurveyPrepPalette.SEL, true))
    }

    @Test fun `a chain point on the picked door's tab -- the tab wins, an end before both, then the marks`() {
        val door = Pano360.Poly("front", "FD", listOf(doubleArrayOf(100.0, 100.0), doubleArrayOf(200.0, 100.0),
            doubleArrayOf(200.0, 300.0), doubleArrayOf(100.0, 300.0)), 20000.0)
        val win = Pano360.Poly("front", "FW", listOf(doubleArrayOf(150.0, 150.0), doubleArrayOf(180.0, 150.0),
            doubleArrayOf(180.0, 180.0), doubleArrayOf(150.0, 180.0)), 900.0)
        val tabs = listOf("l" to doubleArrayOf(100.0, 200.0), "lt" to doubleArrayOf(100.0, 100.0))
        val nodes = listOf("FK1" to listOf(doubleArrayOf(102.0, 201.0), doubleArrayOf(400.0, 201.0)))
        val sc = Pano360.Scene(null, tabs, nodes, listOf(door, win), "FD")
        val sel = Pano360.order(SurveyPrepPalette.SEL, false)
        assertEquals(Pano360.Pick.Tab("l"), Pano360.pick(sel, sc, 104.0, 202.0, DP))
        // past the tabs' 14 dp, the chain's point (16 dp)
        assertEquals(Pano360.Pick.Node("FK1", 1), Pano360.pick(sel, sc, 400.0, 220.0, DP))
        // the selected line's end within 14 dp comes first of all
        val withEnd = Pano360.Scene(listOf(doubleArrayOf(99.0, 199.0), doubleArrayOf(500.0, 199.0)), tabs, nodes, listOf(door), "FD")
        assertEquals(Pano360.Pick.End(0), Pano360.pick(sel, withEnd, 104.0, 202.0, DP))
        // the picked mark first, though a smaller one is under the finger; with no pick, the smallest
        assertEquals(Pano360.Pick.Mark("front", "FD"), Pano360.pick(sel, sc, 160.0, 160.0, DP))
        val noSel = Pano360.Scene(null, emptyList(), emptyList(), listOf(door, win), null)
        assertEquals(Pano360.Pick.Mark("front", "FW"), Pano360.pick(sel, noSel, 160.0, 160.0, DP))
        assertEquals(Pano360.Pick.Look, Pano360.pick(sel, noSel, 600.0, 600.0, DP))
        // with a type on: a tab, then a mark, else it is placed; never a chain's point
        val typ = Pano360.order("D", false)
        assertEquals(Pano360.Pick.Tab("l"), Pano360.pick(typ, sc, 104.0, 202.0, DP))
        assertEquals(Pano360.Pick.Place, Pano360.pick(typ, sc, 400.0, 220.0, DP))
        // with Chain on, a point is grabbed, anything else is a new point; with Tape on, anything is a tape
        assertEquals(Pano360.Pick.Node("FK1", 0), Pano360.pick(Pano360.order(SurveyPrepPalette.CHAIN, false), sc, 104.0, 202.0, DP))
        assertEquals(Pano360.Pick.Chain, Pano360.pick(Pano360.order(SurveyPrepPalette.CHAIN, false), sc, 160.0, 160.0, DP))
        assertEquals(Pano360.Pick.End(0), Pano360.pick(Pano360.order(SurveyPrepPalette.TAPE, false), withEnd, 104.0, 202.0, DP))
        assertEquals(Pano360.Pick.Meas, Pano360.pick(Pano360.order(SurveyPrepPalette.TAPE, false), sc, 104.0, 202.0, DP))
    }

    @Test fun `a mark's outline and its eight tabs on the 360 sit on its corners and edges`() {
        val p = room(); val fr = frames(p)
        val e = p.addMarkAt("front", "D", 1300.0, 1200.0, W, H, "d1")
        assertEquals(listOf("l", "r", "t", "b", "lt", "rt", "lb", "rb"), Pano360.tabsOf(e).map { it.first })
        assertEquals(listOf(e.x1, e.y1), Pano360.tabsOf(e)[5].second.toList())
        val v = Pano360.faceView("front", 680.0, 840.0)
        val f = fr.getValue("front")
        val poly = Pano360.markPoly(v, "front", f, e, DP)
        assertNotNull(poly); assertEquals(4, poly.pts.size); assertNull(poly.spot)
        // the top-left tab is on the outline's corner (x0, y1)
        val lt = Pano360.toS(v, "front", f, e.x0, e.y1)!!
        assertEquals(lt[0], poly.pts[3][0], 1e-6); assertEquals(lt[1], poly.pts[3][1], 1e-6)
        // its middle is inside; a point well outside is not
        val mid = Pano360.toS(v, "front", f, 1300.0, 1200.0)!!
        assertTrue(Pano360.inPoly(mid[0], mid[1], poly.pts)); assertFalse(Pano360.inPoly(mid[0] + 300, mid[1], poly.pts))
        // a point is a spot: a ring pressed within 14 dp at least, drawn 6 dp at least
        val sp = p.addMarkAt("front", "E", 400.0, 400.0, W, H, "e1")
        val ps = Pano360.markPoly(v, "front", f, sp, DP)!!
        assertNotNull(ps.spot); assertEquals(16, ps.pts.size)
        assertTrue(ps.spot[2] >= 6 * DP); assertTrue(hypot(ps.pts[0][0] - ps.spot[0], ps.pts[0][1] - ps.spot[1]) >= 14 * DP - 1e-9)
        // the back wall's door is behind a view of the front wall: off the view
        val bd = p.addMarkAt("back", "D", 1300.0, 1200.0, W, H, "d2")
        assertNull(Pano360.markPoly(v, "back", fr.getValue("back"), bd, DP))
    }

    @Test fun `a mark dragged on the 360 moves by the mm under the finger, as on 2 · Mark`() {
        val p = room(); val fr = frames(p); val f = fr.getValue("front")
        val v = Pano360.faceView("front", 680.0, 840.0)
        val e = p.addMarkAt("front", "W", 1300.0, 1500.0, W, H, "w1")
        val start = e.copy()
        val a = Pano360.toS(v, "front", f, 1300.0, 1500.0)!!
        val m0 = Pano360.mmOn("front", f, v.dirAt(a[0], a[1]))!!
        // the finger 40 dp right: the window's right tab follows to the mm under it, its left edge still
        val m1 = Pano360.mmOn("front", f, v.dirAt(a[0] + 40 * DP, a[1]))!!
        SurveyPrep.moveMark(e, start, "r", m1[0] - m0[0], m1[1] - m0[1], W, H)
        assertTrue(e.x1 > start.x1 + 20); assertEquals(start.x0, e.x0)
        assertEquals(start.y1, e.y1, 1.0)
    }

    @Test fun `the snapping reach is 25 mm, or 14 dp at the finger when that is more`() {
        val fr = frames(); val f = fr.getValue("front")
        val near = Pano360.faceView("front", 680.0, 840.0)
        val m = Pano360.mmOn("front", f, near.dirAt(340.0, 420.0))
        val t = Pano360.tolMm(near, "front", f, 340.0, 420.0, m, DP)
        assertTrue(t >= 25.0)
        // zoomed in to 40 degrees, 14 dp is fewer mm: the 25 mm floor holds
        val zoom = Pano360.View(0.0, 0.0, 40.0, 680.0, 840.0)
        val mz = Pano360.mmOn("front", f, zoom.dirAt(340.0, 420.0))
        assertTrue(Pano360.tolMm(zoom, "front", f, 340.0, 420.0, mz, DP) <= t)
    }

    // ------------------------------------------------------------------ the tape narrows with distance

    @Test fun `a tape along the right wall narrows with distance, as the wall does`() {
        val fr = frames(); val f = fr.getValue("right")
        // looking ahead-right: the right wall runs from near (its front end) to far (its back end)
        val v = Pano360.View(60.0, 0.0, 100.0, 680.0, 840.0)
        val a = doubleArrayOf(400.0, 1200.0); val b = doubleArrayOf(2400.0, 1200.0)
        val hw = Pano360.tapeHalfMm(v, "right", f, a, b, DP)
        assertTrue(hw >= SurveyMeas.TAPE_W / 2)
        val band = Pano360.band(a, b, hw)
        fun across(p: DoubleArray, q: DoubleArray): Double {
            val s1 = Pano360.toS(v, "right", f, p[0], p[1])!!; val s2 = Pano360.toS(v, "right", f, q[0], q[1])!!
            return hypot(s1[0] - s2[0], s1[1] - s2[1])
        }
        val wa = across(band[0], band[3]); val wb = across(band[1], band[2])
        assertTrue(abs(wa - wb) > 0.5, "$wa vs $wb")
        // the end whose 100 mm along the tape is longer on screen -- the nearer -- has the wider band
        val la = across(a, doubleArrayOf(a[0] + 100, a[1])); val lb = across(b, doubleArrayOf(b[0] - 100, b[1]))
        assertEquals(la > lb, wa > wb)
        // ... and 25 mm is the band on the face itself where it reads wider than 3.5 dp
        val zoom = Pano360.View(90.0, 0.0, 40.0, 680.0, 840.0)
        assertEquals(12.5, Pano360.tapeHalfMm(zoom, "right", f, doubleArrayOf(1500.0, 1200.0), doubleArrayOf(1800.0, 1200.0), DP), 1e-9)
    }

    // ------------------------------------------------------------------ the pixels

    @Test fun `the view samples the 360 by direction -- the front ahead, the right wall 90 degrees round`() {
        // a 360 whose every pixel says which column and row it is
        val pw = 360; val ph = 180
        val pano = Panorama.Image(pw, ph, IntArray(pw * ph) { (it % pw) shl 8 or (it / pw) })
        val w = 64; val h = 48; val out = IntArray(w * h)
        val v = Pano360.View(0.0, 0.0, 90.0, w.toDouble(), h.toDouble())
        Pano360.render(pano, v, out, w, h)
        // opaque, every pixel
        assertTrue(out.all { it ushr 24 == 0xFF })
        // the middle looks along +z: longitude 0, the 360's middle column; latitude 0, its middle row
        val mid = out[(h / 2) * w + w / 2]
        assertEquals(pw / 2.0, ((mid shr 8) and 0xFFFF).toDouble(), 1.0)
        assertEquals(ph / 2.0, (mid and 0xFF).toDouble(), 1.0)
        // the right edge of a 90 degree view is 45 degrees to the right: 45 columns on
        val right = out[(h / 2) * w + w - 1]
        assertEquals(pw / 2.0 + 45, ((right shr 8) and 0xFFFF).toDouble(), 2.0)
        // facing the right wall the middle is 90 degrees round
        Pano360.render(pano, v.let { Pano360.View(90.0, 0.0, 90.0, w.toDouble(), h.toDouble()) }, out, w, h)
        val r90 = out[(h / 2) * w + w / 2]
        assertEquals(pw / 2.0 + 90, ((r90 shr 8) and 0xFFFF).toDouble(), 1.0)
        // the buffer is half the screen
        assertEquals(listOf(340, 420), Pano360.bufferSize(680.0, 840.0).toList())
        assertEquals(listOf(1, 1), Pano360.bufferSize(1.0, 0.4).toList())
        // longitude agrees with WallCorners' (the cut faces'): atan2(x, z)
        val d = v.dirAt(w - 0.5, h / 2.0)
        assertEquals(45.0, Math.toDegrees(atan2(d[0], d[2])), 1.0)
    }

    // ------------------------------------------------------------------ lines picked on the 360, drafts, the chip

    @Test fun `with Select on a tap picks the nearest line within 12 dp -- a tape by its band`() {
        val p = room(); val fr = frames(p); val f = fr.getValue("front")
        val v = Pano360.faceView("front", 680.0, 840.0)
        val tape = p.addMeas("front", SurveyMeas.TAPE, listOf(doubleArrayOf(400.0, 1000.0), doubleArrayOf(1900.0, 1000.0)), "t1")
        val line = p.addMeas("front", SurveyMeas.LINE, listOf(doubleArrayOf(400.0, 1600.0), doubleArrayOf(1900.0, 1600.0)), "m1")
        val lines = listOf(Pano360.measLine(v, "front", f, tape, DP), Pano360.measLine(v, "front", f, line, DP))
        assertTrue(lines[0].widen > 0.0); assertEquals(0.0, lines[1].widen)
        val onTape = Pano360.toS(v, "front", f, 1000.0, 1000.0)!!; val onLine = Pano360.toS(v, "front", f, 1000.0, 1600.0)!!
        assertEquals("t1", Pano360.pickLine(lines, onTape[0], onTape[1] + 10 * DP, 12 * DP))
        assertEquals("m1", Pano360.pickLine(lines, onLine[0], onLine[1] - 8 * DP, 12 * DP))
        assertNull(Pano360.pickLine(lines, onLine[0], onLine[1] - 40 * DP, 12 * DP))
        // an area is closed: its last side picks it too
        val area = p.addMeas("front", SurveyMeas.AREA, listOf(doubleArrayOf(2000.0, 300.0), doubleArrayOf(2500.0, 300.0), doubleArrayOf(2500.0, 800.0)), "a1")
        val al = Pano360.measLine(v, "front", f, area, DP)
        assertEquals(3, al.segs.size)
        val hyp = Pano360.toS(v, "front", f, 2250.0, 550.0)!!
        assertEquals("a1", Pano360.pickLine(listOf(al), hyp[0], hyp[1], 12 * DP))
        // the points of an item behind the camera are kept in their places, far off the screen
        val sp = Pano360.screenPts(Pano360.faceView("back", 680.0, 840.0), "front", f, tape.pts)
        assertEquals(2, sp.size); assertTrue(sp.all { it[0] == Pano360.OFF })
    }

    @Test fun `a Tape pulled where no direction meets the plane is off, and refused on release`() {
        val d = SurveyMeas.Draft("floor", SurveyMeas.TAPE, doubleArrayOf(500.0, 500.0), W, L)
        d.move(doubleArrayOf(1500.0, 500.0))
        assertFalse(d.off); assertEquals(SurveyMeas.End.PLOTTED, SurveyMeas.end(d))
        d.offPlane()
        assertTrue(d.off); assertEquals(SurveyMeas.End.REFUSED, SurveyMeas.end(d))
        assertEquals("Off this surface — nothing will be plotted", SurveyMeas.dirText(d))
    }

    @Test fun `the tool chip on the 360 says tap the 360`() {
        assertEquals("Placing Window · tap the 360", SurveyPrepPalette.chipText("front", "W", "360"))
        assertEquals("Placing Switch plate · tap the photo", SurveyPrepPalette.chipText("front", "S"))
        assertNull(SurveyPrepPalette.chipText("front", SurveyPrepPalette.SEL, "360"))
    }

    // ------------------------------------------------------------------ ct52 T and X5 through the 360's maths

    /** The mm on [s] under the screen point where [v] shows ([x], [y]) mm on [s], moved by ([dx], [dy]) px. */
    private fun under(v: Pano360.View, s: String, f: SurveyPrep.Frame, x: Double, y: Double, dx: Double = 0.0, dy: Double = 0.0): DoubleArray {
        val p = Pano360.toS(v, s, f, x, y)!!
        return Pano360.mmOn(s, f, v.dirAt(p[0] + dx, p[1] + dy))!!
    }

    @Test fun `ct52 T -- on the 360 a Tape snaps along the face, then F-T3's end dragged to about 1900 mm, level`() {
        val p = room(); val f = frames(p).getValue("front")
        val v = Pano360.faceView("front", 680.0, 840.0)
        p.addMeas("front", SurveyMeas.TAPE, listOf(doubleArrayOf(100.0, 100.0), doubleArrayOf(300.0, 100.0)), "t1")
        p.addMeas("front", SurveyMeas.TAPE, listOf(doubleArrayOf(100.0, 200.0), doubleArrayOf(300.0, 200.0)), "t2")
        // pulled from (400, 1000) to (1900, 1040) on the screen: 1.5 degrees off level, so it snaps along the face
        val d = SurveyMeas.Draft("front", SurveyMeas.TAPE, under(v, "front", f, 400.0, 1000.0), f.a, f.b)
        d.move(under(v, "front", f, 1900.0, 1040.0))
        assertEquals("h", d.snap); assertEquals(SurveyMeas.End.PLOTTED, SurveyMeas.end(d))
        assertTrue(SurveyMeas.dirText(d).startsWith("↔ along the face"))
        val t3 = p.addMeas("front", d.kind, d.pts(), "t3")
        assertEquals("F-T3", t3.code); assertEquals(t3.pts[0][1], t3.pts[1][1]); assertEquals(1500.0, SurveyMeas.plen(t3.pts), 1.0)
        // with Select on its far end is dragged to where (2300, 1020) is: the end moves along the face, level, ~1900
        val tol = Pano360.tolMm(v, "front", f, 0.0, 0.0, null, DP)
        val r = SurveyGuide.endMove("front", t3, 1, under(v, "front", f, 2300.0, 1020.0), f.a, f.b, p.surface("front").marks, tol)
        assertNotNull(r); t3.pts[1] = r.p
        assertEquals(400.0, t3.pts[0][0], 0.5); assertEquals(t3.pts[0][1], t3.pts[1][1])
        assertEquals(1900.0, SurveyMeas.plen(t3.pts), 1.0)
    }

    @Test fun `ct52 X5 on the 360 -- each of the eight tabs dragged 40 dp moves exactly its own edges, outward`() {
        val p = room(); val f = frames(p).getValue("front")
        val v = Pano360.faceView("front", 680.0, 840.0)
        val out = mapOf("l" to (-40.0 to 0.0), "r" to (40.0 to 0.0), "t" to (0.0 to -40.0), "b" to (0.0 to 40.0),
            "lt" to (-40.0 to -40.0), "rt" to (40.0 to -40.0), "lb" to (-40.0 to 40.0), "rb" to (40.0 to 40.0))
        for ((part, mm) in Pano360.tabsOf(SurveyPrep.Mark("x", "D", "HD", 900.0, 1800.0, 400.0, 2000.0))) {
            val e = SurveyPrep.Mark("x", "D", "HD", 900.0, 1800.0, 400.0, 2000.0); val start = e.copy()
            val (dx, dy) = out.getValue(part)
            val m0 = under(v, "front", f, mm[0], mm[1])
            val m1 = under(v, "front", f, mm[0], mm[1], dx * DP, dy * DP)
            SurveyPrep.moveMark(e, start, part, m1[0] - m0[0], m1[1] - m0[1], f.a, f.b)
            SurveyPrep.snapMark(e, part, f.a, f.b)
            assertEquals('l' in part, e.x0 < start.x0 - 20, "$part left"); assertEquals('r' in part, e.x1 > start.x1 + 20, "$part right")
            assertEquals('t' in part, e.y1 > start.y1 + 20, "$part top"); assertEquals('b' in part, e.y0 < start.y0 - 20, "$part bottom")
            if ('l' !in part) assertEquals(start.x0, e.x0); if ('r' !in part) assertEquals(start.x1, e.x1)
            if ('t' !in part) assertEquals(start.y1, e.y1); if ('b' !in part) assertEquals(start.y0, e.y0)
        }
    }

    // ------------------------------------------------------------------ slice 17 review fixes

    @Test fun `a quick second tap with Select on is a tap -- the DoubleTap the classifier sends resolves as the Tap does`() {
        // the classifier: a tap, then within 450 ms and 24 dp another, is a DoubleTap at the second press
        val g = GestureClassifier(8.0, GestureClassifier.DOUBLE_TAP_DP * DP)
        g.down(100.0, 100.0, 0); val t1 = g.up(100.0, 100.0, 60).single()
        g.down(130.0, 110.0, 300); val t2 = g.up(130.0, 110.0, 360).single()
        assertTrue(t1 is GestureClassifier.Event.Tap); assertTrue(t2 is GestureClassifier.Event.DoubleTap)
        // ... and on the 360 with Select on it picks, or lets go, exactly as a tap would
        for (chain in listOf(false, true)) for (meas in listOf(false, true)) {
            val want = Pano360.lookTap(t1, chain, meas, false)
            assertNotNull(want)
            assertEquals(want, Pano360.lookTap(t2, chain, meas, false), "chain $chain meas $meas")
        }
        assertEquals(Pano360.LookTap.CHAIN, Pano360.lookTap(t2, true, true, false))
        assertEquals(Pano360.LookTap.MEAS, Pano360.lookTap(t2, false, true, false))
        assertEquals(Pano360.LookTap.LET_GO, Pano360.lookTap(t2, false, false, false))
        // a drag that looked round is no tap; nor is a long press or a drag event
        assertNull(Pano360.lookTap(t2, true, false, true))
        assertNull(Pano360.lookTap(GestureClassifier.Event.LongPress(1.0, 1.0), true, false, false))
    }

    @Test fun `the floating trash on the 360 -- beside the picked mark, Tape or chain, with Select on, never for the reference`() {
        val p = room(); val fr = frames(p)
        val v = Pano360.faceView("front", 680.0, 840.0)
        p.surface("front").marks.add(SurveyPrep.Mark("m1", "D", "F-D1", 900.0, 1800.0, 400.0, 2000.0))
        val t = p.addMeas("front", SurveyMeas.TAPE, listOf(doubleArrayOf(400.0, 1000.0), doubleArrayOf(1400.0, 1000.0)), "t1")
        // a mark: the box of its four corners as drawn, no handles
        val m = assertNotNull(Pano360.trashOf(v, fr, p, SurveyPrepPalette.SEL, "m1"))
        val corners = listOf(900.0 to 400.0, 1800.0 to 400.0, 1800.0 to 2000.0, 900.0 to 2000.0).map { (x, y) -> Pano360.toS(v, "front", fr.getValue("front"), x, y)!! }
        assertEquals(corners.minOf { it[0] }, m.box.l, 1e-9); assertEquals(corners.maxOf { it[0] }, m.box.r, 1e-9)
        assertEquals(corners.minOf { it[1] }, m.box.t, 1e-9); assertEquals(corners.maxOf { it[1] }, m.box.b, 1e-9)
        assertTrue(m.handles.isEmpty())
        // a Tape: its two ends are the box and the handles the disc keeps off
        val tt = assertNotNull(Pano360.trashOf(v, fr, p, SurveyPrepPalette.SEL, t.id))
        assertEquals(2, tt.handles.size)
        assertNotNull(SurveyPrepTouch.trashSpot(tt.box, emptyList(), 680.0, 840.0, DP, tt.handles, SurveyGuide.HANDLE_CLEAR_DP * DP))
        // with a tool on, nothing picked, the reference, or the item behind the camera: no trash
        assertNull(Pano360.trashOf(v, fr, p, SurveyPrepPalette.TAPE, "m1"))
        assertNull(Pano360.trashOf(v, fr, p, SurveyPrepPalette.SEL, null))
        assertNull(Pano360.trashOf(v, fr, p, SurveyPrepPalette.SEL, "${SurveyRef.KEY}:front"))
        assertNull(Pano360.trashOf(Pano360.faceView("back", 680.0, 840.0), fr, p, SurveyPrepPalette.SEL, "m1"))
        // on a surface whose lines are not all set: no trash
        val q = room(); q.surface("front").marks.add(SurveyPrep.Mark("m1", "D", "F-D1", 900.0, 1800.0, 400.0, 2000.0))
        q.surface("front").set[SurveyPrep.SLOTS[0]] = false
        assertNull(Pano360.trashOf(v, Pano360.frames(q, L, W, H), q, SurveyPrepPalette.SEL, "m1"))
    }
}
