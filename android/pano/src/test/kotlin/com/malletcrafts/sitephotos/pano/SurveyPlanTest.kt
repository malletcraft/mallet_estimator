package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * SIX DRAWINGS ON THE PLAN TAB (v52 slice 13, items 10 and 11), against the prototype's suite ct52: section H (l.473, the
 * plan tab: the room to scale, W / L, FRONT, every opening where its mm put it, FD1's swing), FX2 (2437-2480: the plan
 * is a TRUE TOP VIEW -- FRONT at the top on BOTH plans, the walls clockwise F → R → B → L, each door at its wall's LEFT
 * corner as Amit faces it), FX4 (points and switch plates dimensioned on the elevations, the fan from its nearer walls)
 * and DR (1770-1875: the titles and info lines, the ceiling plan, the four elevations). DR's own "FRONT at the bottom"
 * label is stale (v52 moved it to the top) and is NOT copied: FRONT is over the front wall on both plans.
 *
 * The synthetic room of every slice, L 3319 / W 2678 / H 2677 (ct52's Guest Bedroom, "gb"), and ct52's Living Room
 * ("htu"), whose front wall is 3310 × 2705 with the door FD1 at its left corner.
 */
class SurveyPlanTest {

    private val L = 3319.0; private val W = 2678.0; private val H = 2677.0

    private fun room(l: Double = L, w: Double = W, h: Double = H): SurveyPrep.Prep = SurveyPrep.fresh(l, w, h)

    private var n = 0
    private fun put(p: SurveyPrep.Prep, s: String, t: String, code: String, x0: Double, x1: Double, y0: Double, y1: Double): SurveyPrep.Mark =
        SurveyPrep.Mark("m${n++}", t, code, x0, x1, y0, y1).also { p.surface(s).marks.add(it) }

    private fun near(e: Double, g: Double, tol: Double, what: String) = assertTrue(abs(e - g) <= tol, "$what: expected $e, got $g")

    /** ct52's Guest Bedroom: the window FW1 (sill ≈919, head ≈2100, width ≈1171, ≈725 from the right corner), the door
     *  BD1 (≈1900 high), the niche LN1, the floor step GS1, the point FE1; the ceiling's CB1 CE1 CP1 CO1 and the right
     *  wall's loft RL1 and beam RB1 spread out as DR places them. */
    private fun gb(): SurveyPrep.Prep {
        val p = room()
        put(p, "front", "W", "FW1", 782.0, 1953.0, 919.0, 2100.0)
        put(p, "front", "E", "FE1", 325.0, 375.0, 325.0, 375.0)
        put(p, "back", "D", "BD1", 1500.0, 2300.0, 0.0, 1900.0)
        put(p, "left", "N", "LN1", 1200.0, 2100.0, 750.0, 1950.0)
        put(p, "floor", "S", "GS1", 889.0, 1789.0, 150.0, 450.0)
        put(p, "ceil", "B", "CB1", 600.0, 830.0, 0.0, 3319.0)
        put(p, "ceil", "E", "CE1", 1725.0, 1875.0, 725.0, 875.0)
        put(p, "ceil", "P", "CP1", 1325.0, 1475.0, 2125.0, 2275.0)
        put(p, "ceil", "O", "CO1", 1700.0, 2300.0, 2400.0, 2900.0)
        put(p, "right", "L", "RL1", 0.0, 1800.0, 2077.0, 2677.0)
        put(p, "right", "B", "RB1", 2200.0, 3319.0, 2277.0, 2677.0)
        return p
    }

    /** ct52's Living Room (htu): front wall 3310 × 2705, the door FD1 at its left corner (≈905 × ≈2053) and FE1. */
    private val hL = 5458.0; private val hW = 3310.0; private val hH = 2705.0
    private fun htu(): SurveyPrep.Prep {
        val p = room(hL, hW, hH)
        put(p, "front", "D", "FD1", 0.0, 905.0, 0.0, 2053.0)
        put(p, "front", "E", "FE1", 1400.0, 1450.0, 325.0, 375.0)
        return p
    }

    private fun centre(pts: List<DoubleArray>) = doubleArrayOf(pts.sumOf { it[0] } / pts.size, pts.sumOf { it[1] } / pts.size)
    private fun poly(d: SurveyPlan.Drawing, role: String, key: String): SurveyPlan.Poly =
        assertNotNull(d.items.filterIsInstance<SurveyPlan.Poly>().firstOrNull { it.role == role && it.key == key }, "$role $key")
    private fun text(d: SurveyPlan.Drawing, role: String): SurveyPlan.Text =
        assertNotNull(d.texts(role).firstOrNull(), role)
    private fun dimLine(d: SurveyPlan.Drawing, key: String): SurveyPlan.Line =
        assertNotNull(d.items.filterIsInstance<SurveyPlan.Line>().firstOrNull { it.role == "dim.line" && it.key == key }, key)

    // ------------------------------------------------------------------ the mapping

    @Test fun `the four walls land on the plan as item 10 says -- front x to (x,0), right to (W,x), back to (W-x,L), left to (0,L-x)`() {
        fun pt(s: String, x: Double) = SurveyPlan.planPoint(s, x, W, L).toList()
        assertEquals(listOf(500.0, 0.0), pt("front", 500.0))
        assertEquals(listOf(W, 500.0), pt("right", 500.0))
        assertEquals(listOf(W - 500.0, L), pt("back", 500.0))
        assertEquals(listOf(0.0, L - 500.0), pt("left", 500.0))
        // the corners meet: front's right end is right's left end, and so round the room, clockwise
        assertEquals(pt("front", W), pt("right", 0.0)); assertEquals(pt("right", L), pt("back", 0.0))
        assertEquals(pt("back", W), pt("left", 0.0)); assertEquals(pt("left", L), pt("front", 0.0))
        // every inward normal points into the room
        for (s in SurveyPlan.WALLS) {
            val m = SurveyPlan.planPoint(s, if (s == "front" || s == "back") W / 2 else L / 2, W, L); val nn = SurveyPlan.inward(s)
            val q = doubleArrayOf(m[0] + nn[0] * 10, m[1] + nn[1] * 10)
            assertTrue(q[0] in 0.0..W && q[1] in 0.0..L, "$s normal")
        }
    }

    @Test fun `planY is L - y for the floor AND the ceiling, elevY is B - y`() {
        assertEquals(L, SurveyPlan.planY(0.0, L), "the back edge (y 0) is at the bottom of the page")
        assertEquals(0.0, SurveyPlan.planY(L, L), "the front edge (y L) is at the top")
        assertEquals(H - 900.0, SurveyPlan.elevY(900.0, H))
        // the same rectangle of the floor and of the ceiling lands in the same place on both plans
        val p = room(); put(p, "floor", "C", "GC1", 100.0, 400.0, 100.0, 400.0); put(p, "ceil", "C", "CC1", 100.0, 400.0, 100.0, 400.0)
        val fl = SurveyPlan.drawing("floor", p, L, W, H)
        val a = poly(fl, "mk", "GC1").rings[0]; val b = poly(fl, "mk", "CC1").rings[0]
        assertEquals(a.map { it.toList() }, b.map { it.toList() })
        assertEquals(listOf(100.0, L - 400.0), a[0].toList())
        val cl = SurveyPlan.drawing("ceil", p, L, W, H)
        assertEquals(a.map { it.toList() }, poly(cl, "el", "CC1").rings[0].map { it.toList() })
    }

    // ------------------------------------------------------------------ FX2: the right way round

    @Test fun `FX2 -- a true top view, FRONT at the top, F R B L clockwise, each door at its wall's LEFT corner`() {
        val p = room()
        for (s in SurveyPlan.WALLS) put(p, s, "D", SurveyPrep.CODE.getValue(s) + "D9", 200.0, 1000.0, 0.0, 2000.0)
        put(p, "ceil", "B", "CB9", 100.0, 400.0, 100.0, 400.0)   // near the left and BACK walls
        val d = SurveyPlan.drawing("floor", p, L, W, H)
        val c = SurveyPlan.WALLS.associateWith { s -> centre(poly(d, "op", SurveyPrep.CODE.getValue(s) + "D9").rings[0]) }
        // FRONT over the front wall, at the top
        val fr = text(d, "frontlab"); assertEquals("FRONT", fr.text); assertTrue(fr.y < 0, "FRONT above the room"); near(W / 2, fr.x, 1e-9, "FRONT x")
        assertTrue(c.getValue("front")[1] < 5); assertTrue(c.getValue("right")[0] > W - 5)
        assertTrue(c.getValue("back")[1] > L - 5); assertTrue(c.getValue("left")[0] < 5)
        // clockwise on the page (y down): increasing angle F → R → B → L
        val ang = SurveyPlan.WALLS.map { atan2(c.getValue(it)[1] - L / 2, c.getValue(it)[0] - W / 2) }
        for (i in 0 until 4) { var dd = ang[(i + 1) % 4] - ang[i]; while (dd <= 0) dd += 2 * Math.PI; assertTrue(dd < Math.PI, "clockwise $i") }
        // each door near its LEFT corner as Amit faces it: front top-left, right top-right, back bottom-right, left bottom-left
        assertTrue(c.getValue("front")[0] < W / 2); assertTrue(c.getValue("right")[1] < L / 2)
        assertTrue(c.getValue("back")[0] > W / 2); assertTrue(c.getValue("left")[1] > L / 2)
        // the reflected ceiling plan the same way round: a beam by the left and back walls is bottom-left, FRONT at the top
        val cl = SurveyPlan.drawing("ceil", p, L, W, H)
        val b = centre(poly(cl, "el", "CB9").rings[0]); assertTrue(b[0] < W / 2 && b[1] > L / 2, "beam bottom-left: ${b.toList()}")
        val cf = text(cl, "frontlab"); assertEquals("FRONT", cf.text); assertTrue(cf.y < 0, "FRONT at the top of the ceiling plan too")
    }

    @Test fun `H -- every opening sits where its mm put it, its face on the wall line, the band T thick`() {
        val p = gb(); val d = SurveyPlan.drawing("floor", p, L, W, H)
        for (s in SurveyPlan.WALLS) for (e in p.surface(s).marks.filter { it.t in setOf("D", "W", "O", "N") }) {
            val r = poly(d, "op", e.code); assertEquals(s, r.s)
            val q = r.rings[0]
            assertEquals(SurveyPlan.planPoint(s, e.x0, W, L).toList(), q[0].toList(), "${e.code} x0")
            assertEquals(SurveyPlan.planPoint(s, e.x1, W, L).toList(), q[1].toList(), "${e.code} x1")
        }
        // the walls band runs T = 150 outside the room
        assertEquals(150.0, SurveyPlan.T)
        val band = d.items.filterIsInstance<SurveyPlan.Poly>().single { it.role == "walls" }
        assertEquals(listOf(-150.0, -150.0), band.rings[0][0].toList()); assertEquals(listOf(W + 150, L + 150), band.rings[0][2].toList())
        assertEquals(2, band.rings.size, "a band: the outer rectangle less the room")
        assertEquals("W 2678", text(d, "dimW").text); assertEquals("L 3319", text(d, "dimL").text)
        // the 1 m bar is 1000 mm long
        val bar = d.items.filterIsInstance<SurveyPlan.Line>().first { it.role == "scalebar" }; assertEquals(1000.0, bar.x2 - bar.x1)
        // GB: window, door with its swing, niche, floor step and point drawn
        assertTrue(d.withKey("op", "FW1").isNotEmpty() && d.withKey("swing", "BD1").isNotEmpty() && d.withKey("op", "LN1").isNotEmpty())
        assertTrue(d.withKey("mk", "GS1").isNotEmpty() && d.withKey("mk", "FE1").isNotEmpty())
    }

    // ------------------------------------------------------------------ the door swing

    @Test fun `htu -- FD1 hinged at the front wall's left corner (top-left of the plan), its leaf into the room, its swing to the far jamb`() {
        val p = htu(); val d = SurveyPlan.drawing("floor", p, hL, hW, hH, room = "Living Room")
        val leaf = d.items.filterIsInstance<SurveyPlan.Line>().single { it.role == "leaf" && it.key == "FD1" }
        near(0.0, leaf.x1, 1e-9, "hinge x"); near(0.0, leaf.y1, 1e-9, "hinge y")
        near(0.0, leaf.x2, 1e-9, "leaf x"); near(905.0, leaf.y2, 1e-9, "the leaf runs 905 into the room")
        val sw = d.items.filterIsInstance<SurveyPlan.Arc>().single { it.role == "swing" && it.key == "FD1" }
        near(905.0, sw.r, 1e-9, "radius = the door's width"); near(90.0, sw.start, 1e-9, "from the leaf"); near(-90.0, sw.sweep, 1e-9, "a quarter turn")
        val end = doubleArrayOf(sw.cx + sw.r * cos(Math.toRadians(sw.start + sw.sweep)), sw.cy + sw.r * sin(Math.toRadians(sw.start + sw.sweep)))
        near(905.0, end[0], 1e-6, "to the far jamb x"); near(0.0, end[1], 1e-6, "to the far jamb y")
        assertNotNull(sw.dash, "the swing is dashed")
        // FE1 a point symbol at its x; FD1's labels
        val fe = d.items.filterIsInstance<SurveyPlan.Circle>().first { it.role == "mk" && it.key == "FE1" }; near(1425.0, fe.cx, 1e-9, "FE1 x")
        val labs = d.items.filterIsInstance<SurveyPlan.Text>().filter { it.key == "FD1" }.map { it.text }
        assertTrue("FD1 ≈905" in labs, labs.toString()); assertTrue("at the left corner" in labs, labs.toString())
        assertEquals("Living Room · 3310 × 5458", d.info)
        // a laser reading turns its figure from grey ≈ to ink
        p.actual["FD1.w"] = 900.0
        val t = SurveyPlan.drawing("floor", p, hL, hW, hH).texts("opl").single { it.key == "FD1" }
        assertEquals("FD1 900", t.text); assertEquals(SurveyPlan.INK, t.colour); assertEquals(0x1d1f1a, t.colour)
        p.actual.remove("FD1.w")
        assertEquals(0x8a8f86, SurveyPlan.drawing("floor", p, hL, hW, hH).texts("opl").single { it.key == "FD1" }.colour)
    }

    @Test fun `a door in a wall's right half is hinged at its right jamb`() {
        val p = room(); put(p, "front", "D", "FD9", 2000.0, 2600.0, 0.0, 2000.0)
        val d = SurveyPlan.drawing("floor", p, L, W, H)
        val leaf = d.items.filterIsInstance<SurveyPlan.Line>().single { it.role == "leaf" }
        near(2600.0, leaf.x1, 1e-9, "hinge at x1"); near(600.0, leaf.y2, 1e-9, "leaf 600 in")
        val sw = d.items.filterIsInstance<SurveyPlan.Arc>().single()
        near(90.0, sw.start, 1e-9, "from the leaf"); near(90.0, sw.sweep, 1e-9, "turning the other way, to x0")
    }

    // ------------------------------------------------------------------ the elevations

    @Test fun `htu front elevation -- "Front · 3310 × 2705 · 2 marks", FD1 width ≈905 and height ≈2053, no offset line, FE1 a symbol`() {
        val d = SurveyPlan.drawing("front", htu(), hL, hW, hH)
        assertEquals("Front · 3310 × 2705 · 2 marks", d.info)
        assertEquals("≈905", d.dim("FD1.w")?.text); assertEquals("≈2053", d.dim("FD1.h")?.text)
        assertNull(d.dims.firstOrNull { it.key.startsWith("FD1.l") }, "at the corner: no offset")
        assertTrue(d.items.any { it is SurveyPlan.Circle && it.role == "el" && it.key == "FE1" })
        // FE1 placed by its centre: from the nearer corner and from the floor
        assertTrue(d.dims.any { it.key == "FE1.cl" || it.key == "FE1.cr" }); assertTrue(d.dims.any { it.key == "FE1.cs" || it.key == "FE1.ct" })
    }

    @Test fun `DR -- each elevation is the wall face on at its size, W or L along the top, H up the side, its neighbours and its name`() {
        val p = gb()
        for (s in SurveyPlan.WALLS) {
            val d = SurveyPlan.drawing(s, p, L, W, H); val ab = SurveyPrep.sizeOf(s, p, L, W, H)
            val r = poly(d, "wall", s).rings[0]
            assertEquals(listOf(0.0, 0.0), r[0].toList()); assertEquals(listOf(ab[0], ab[1]), r[2].toList())
            val tag = if (s == "front" || s == "back") "W" else "L"
            assertEquals("$tag ${Math.round(ab[0])}", text(d, "dimA").text); assertEquals("H 2677", text(d, "dimH").text)
            assertEquals(2, d.items.count { it.role == "dimA.head" }, "arrow heads")
            assertEquals(SurveyPrep.NAME.getValue(s).uppercase(), text(d, "walllab").text)
            assertTrue(d.items.count { it.role == "hatch" } > 5); assertEquals(2, d.items.count { it.role == "corner" })
            assertTrue(d.items.any { it.role == "scalebar" })
            val n = p.surface(s).marks.size
            assertEquals("${SurveyPrep.NAME.getValue(s).removeSuffix(" wall")} · ${Math.round(ab[0])} × ${Math.round(ab[1])} · $n mark${if (n == 1) "" else "s"}", d.info)
            // only this wall's marks
            assertTrue(d.items.filterIsInstance<SurveyPlan.Poly>().filter { it.role == "el" }.all { it.s == s })
            // every mark with a width is dimensioned along the bottom
            for (e in p.surface(s).marks.filter { !SurveyPrep.isPoint(it.t) && SurveyPrep.measures(s, it, ab[0], ab[1]).any { m -> m.key == "w" } })
                assertNotNull(d.dim(e.code + ".w"), "${e.code}.w")
            // every mark where its mm put it, page y = B - y
            for (e in p.surface(s).marks.filter { !SurveyPrep.isPoint(it.t) }) {
                val q = poly(d, "el", e.code).rings[0]
                assertEquals(listOf(e.x0, ab[1] - e.y1), q[0].toList(), e.code); assertEquals(listOf(e.x1, ab[1] - e.y0), q[2].toList(), e.code)
                assertTrue(d.texts("elc").any { it.text.split(" ")[0] == e.code })
            }
        }
        val f = SurveyPlan.drawing("front", p, L, W, H)
        assertEquals(listOf("◂ Left wall", "Right wall ▸"), f.texts("neigh").map { it.text })
        assertEquals(listOf("◂ Front wall", "Back wall ▸"), SurveyPlan.drawing("right", p, L, W, H).texts("neigh").map { it.text })
        assertEquals(listOf("◂ Right wall", "Left wall ▸"), SurveyPlan.drawing("back", p, L, W, H).texts("neigh").map { it.text })
        assertEquals(listOf("◂ Back wall", "Front wall ▸"), SurveyPlan.drawing("left", p, L, W, H).texts("neigh").map { it.text })
    }

    @Test fun `DR -- the window carries its sill and head from the floor, its width and its offset -- readings turn them to ink`() {
        val p = gb()
        var d = SurveyPlan.drawing("front", p, L, W, H)
        assertEquals("≈919", d.dim("FW1.s")?.text); assertEquals("≈2100", d.dim("FW1.head")?.text)
        assertEquals("≈1171", d.dim("FW1.w")?.text); assertEquals("≈725", d.dim("FW1.r")?.text)
        p.actual["FW1.s"] = 920.0; p.actual["FW1.h"] = 1180.0
        d = SurveyPlan.drawing("front", p, L, W, H)
        val s = assertNotNull(d.dim("FW1.s")); assertEquals("920", s.text); assertTrue(s.read)
        val hd = assertNotNull(d.dim("FW1.head")); assertEquals("2100", hd.text); assertTrue(hd.read, "920 + 1180")
        val w = assertNotNull(d.dim("FW1.w")); assertTrue(w.text.startsWith("≈")); assertFalse(w.read)
        assertEquals(SurveyPlan.INK, d.items.filterIsInstance<SurveyPlan.Text>().single { it.role == "dim" && it.key == "FW1.s" }.colour)
        assertEquals(SurveyPlan.GREY, d.items.filterIsInstance<SurveyPlan.Text>().single { it.role == "dim" && it.key == "FW1.w" }.colour)
        // the right wall: the loft's underside from the floor, the beam's drop; the back door's height
        val r = SurveyPlan.drawing("right", p, L, W, H)
        assertEquals("≈2077", r.dim("RL1.s")?.text); assertEquals("≈400", r.dim("RB1.d")?.text); assertEquals("≈1800", r.dim("RL1.w")?.text)
        assertEquals("≈1900", SurveyPlan.drawing("back", p, L, W, H).dim("BD1.h")?.text)
    }

    @Test fun `FX4 -- a switch plate has its height from the floor, a point its corner offset and height to its centre`() {
        val p = room()
        val sd = SurveyPrep.defaults("front", "S", W, H); put(p, "front", "S", "FS1", sd[0], sd[1], sd[2], sd[3])
        val ed = SurveyPrep.defaults("front", "E", W, H); put(p, "front", "E", "FE1", ed[0], ed[1], ed[2], ed[3])
        val fd = SurveyPlan.drawing("front", p, L, W, H).dims.map { it.key }
        assertTrue("FS1.s" in fd, fd.toString())
        assertTrue(fd.any { it == "FE1.cl" || it == "FE1.cr" } && fd.any { it == "FE1.cs" || it == "FE1.ct" }, fd.toString())
        // a switch plate high on the wall is still measured from the floor, never from the ceiling
        put(p, "front", "S", "FS2", 1500.0, 1700.0, 2400.0, 2490.0)
        val f2 = SurveyPlan.drawing("front", p, L, W, H).dims.map { it.key }
        assertTrue("FS2.s" in f2 && "FS2.t" !in f2, f2.toString())
    }

    // ------------------------------------------------------------------ lanes

    @Test fun `the lanes -- dimensions that do not overlap share a row, ones that do go a row down -- an offset under 1 mm is drawn only once read`() {
        val p = room()
        put(p, "front", "W", "FW1", 300.0, 800.0, 900.0, 1500.0)    // offset 300 from left: 0..300, then 300..800
        put(p, "front", "W", "FW2", 1900.0, 2300.0, 900.0, 1500.0)  // nearer the right: 1900..2300, then 2300..2678
        put(p, "front", "N", "FN1", 600.0, 1100.0, 100.0, 600.0)    // overlaps FW1: a row down
        val d = SurveyPlan.drawing("front", p, L, W, H); val fs = d.fs
        val y0 = dimLine(d, "FW1.w").y1
        near(H + fs * 0.8 + fs * 1.7, y0, 1e-9, "row 0 under the floor")
        assertEquals(y0, dimLine(d, "FW1.l").y1); assertEquals(y0, dimLine(d, "FW2.w").y1); assertEquals(y0, dimLine(d, "FW2.r").y1)
        near(y0 + fs * 1.9, dimLine(d, "FN1.w").y1, 1e-9, "FN1 a row down")
        // a window 0.5 mm off the left corner: nothing from the corner ...
        val q = room(); put(q, "front", "W", "FW3", 0.5, 800.0, 900.0, 1500.0)
        assertNull(SurveyPlan.drawing("front", q, L, W, H).dim("FW3.l"))
        assertNotNull(SurveyPlan.drawing("front", q, L, W, H).dim("FW3.w"), "its width still is")
        // ... and once it is READ the offset joins its row (plan.js 127: `hx[2]>=ZERO || st.actual[...] != null`), so the row
        // starts at the corner: FW3 (0.5 mm off) now packs ahead of FW5 (0.3 mm off) and takes row 0. Its own line is
        // still not drawn -- the drawing places it by the mark's 0.5 mm, and plan.js's dim draws nothing under 1 mm.
        put(q, "front", "W", "FW5", 0.3, 1500.0, 900.0, 1500.0)
        val unread = SurveyPlan.drawing("front", q, L, W, H)
        val row0 = dimLine(unread, "FW5.w").y1
        near(row0 + unread.fs * 1.9, dimLine(unread, "FW3.w").y1, 1e-9, "unread: FW3 a row down, behind FW5")
        q.actual["FW3.l"] = 3.0
        val read = SurveyPlan.drawing("front", q, L, W, H)
        near(row0, dimLine(read, "FW3.w").y1, 1e-9, "read: FW3's row starts at the corner, so it takes row 0")
        near(row0 + read.fs * 1.9, dimLine(read, "FW5.w").y1, 1e-9, "and FW5 goes a row down")
        assertNull(read.dim("FW3.l"), "a line under 1 mm is never drawn, read or not")
        // ... and from 1 mm it is
        val q1 = room(); put(q1, "front", "W", "FW4", 1.0, 800.0, 900.0, 1500.0)
        assertEquals("≈1", SurveyPlan.drawing("front", q1, L, W, H).dim("FW4.l")?.text)
    }

    // ------------------------------------------------------------------ fs and T

    @Test fun `fs is the longer side over 34, T 150 -- the plans by W × L, an elevation by its own wall`() {
        val p = gb()
        near(max(W, L) / 34, SurveyPlan.drawing("floor", p, L, W, H).fs, 1e-12, "floor fs")
        near(max(W, L) / 34, SurveyPlan.drawing("ceil", p, L, W, H).fs, 1e-12, "ceiling fs")
        near(max(W, H) / 34, SurveyPlan.drawing("front", p, L, W, H).fs, 1e-12, "front fs")
        near(max(L, H) / 34, SurveyPlan.drawing("right", p, L, W, H).fs, 1e-12, "right fs")
        val fs = max(W, L) / 34; val T = SurveyPlan.T
        val b = SurveyPlan.drawing("floor", p, L, W, H).box
        assertEquals(listOf(-T - fs * 3.2, -T - fs * 4.6, W + 2 * T + fs * 9.2, L + 2 * T + fs * 11.6), b.toList())
        assertEquals(L + 2 * T + fs * 12.6, SurveyPlan.drawing("ceil", p, L, W, H).box[3])
    }

    // ------------------------------------------------------------------ the ceiling and the fan

    @Test fun `DR ceiling -- REFLECTED CEILING PLAN, W and L, its own marks only, each dimensioned from its nearer walls`() {
        val p = gb(); val d = SurveyPlan.drawing("ceil", p, L, W, H)
        assertEquals("REFLECTED CEILING PLAN", text(d, "rcplab").text); assertEquals("FRONT", text(d, "frontlab").text)
        assertEquals("W 2678", text(d, "dimW").text); assertEquals("L 3319", text(d, "dimL").text)
        assertEquals("Ceiling · 2678 × 3319 · 4 marks", d.info); assertEquals("Reflected ceiling plan", d.title)
        for (c in listOf("CB1", "CE1", "CP1", "CO1")) assertTrue(d.items.any { it.role == "el" && it.key == c }, c)
        assertFalse(d.items.any { it.key == "GS1" }, "no floor marks"); assertFalse(d.items.any { it.role == "op" }, "no doors or windows")
        // wall-hung beam and loft are seen from below
        assertTrue(d.items.any { it.role == "el" && it.key == "RL1" } && d.items.any { it.role == "el" && it.key == "RB1" })
        // a point from both nearer walls to its centre; a beam touching front and back has its side offset only
        for (c in listOf("CE1", "CP1")) assertEquals(2, d.dims.count { it.key.startsWith("$c.") }, c)
        assertEquals(listOf("CB1.l"), d.dims.filter { it.key.startsWith("CB1.") }.map { it.key })
        assertEquals(setOf("CO1.r", "CO1.f"), d.dims.filter { it.key.startsWith("CO1.") }.map { it.key }.toSet())
        assertEquals(setOf("CE1.r", "CE1.k"), d.dims.filter { it.key.startsWith("CE1.") }.map { it.key }.toSet())
        assertEquals(setOf("CP1.cr", "CP1.cf"), d.dims.filter { it.key.startsWith("CP1.") }.map { it.key }.toSet())
        // the marks at their plan mm, front at the top
        val co = poly(d, "el", "CO1").rings[0]
        assertEquals(listOf(1700.0, L - 2900.0), co[0].toList()); assertEquals(listOf(2300.0, L - 2400.0), co[2].toList())
        assertEquals(2, d.items.count { it.role == "cross" && it.key == "CO1" }, "an opening is crossed through")
        assertEquals("Guest Bedroom · 2678 × 3319", SurveyPlan.drawing("floor", p, L, W, H, room = "Guest Bedroom").info)
    }

    @Test fun `the fan -- a ceiling fan is three blades in a dashed 1200 mm sweep circle, a wall fan has no sweep -- dimensioned to its centre`() {
        val p = room()
        val cd = SurveyPrep.defaults("ceil", "F", W, L); put(p, "ceil", "F", "CF1", cd[0], cd[1], cd[2], cd[3])
        val wd = SurveyPrep.defaults("front", "F", W, H); put(p, "front", "F", "FF1", wd[0], wd[1], wd[2], wd[3])
        for (v in listOf("ceil", "floor")) {
            val d = SurveyPlan.drawing(v, p, L, W, H)
            val sw = d.items.filterIsInstance<SurveyPlan.Circle>().filter { it.role == "sweep" }
            assertEquals(listOf("CF1"), sw.map { it.key }, "$v: only the ceiling fan sweeps")
            assertEquals(600.0, sw[0].r, "r 600 mm: a 1200 mm circle"); assertNotNull(sw[0].dash, "dashed")
            near((cd[0] + cd[1]) / 2, sw[0].cx, 1e-9, "$v sweep x"); near(L - (cd[2] + cd[3]) / 2, sw[0].cy, 1e-9, "$v sweep y")
            assertEquals(3, d.items.count { it is SurveyPlan.Ellipse && it.key == "CF1" }, "three blades")
        }
        val fr = SurveyPlan.drawing("front", p, L, W, H)
        assertFalse(fr.items.any { it.role == "sweep" }); assertEquals(3, fr.items.count { it is SurveyPlan.Ellipse && it.key == "FF1" })
        val cdims = SurveyPlan.drawing("ceil", p, L, W, H).dims.map { it.key }
        assertTrue(cdims.any { it == "CF1.cl" || it == "CF1.cr" } && cdims.any { it == "CF1.cf" || it == "CF1.ck" }, cdims.toString())
    }

    // ------------------------------------------------------------------ slice 12's sizes

    @Test fun `a reference-squared wall draws at its reference size, not the laser's`() {
        val p = room()
        val f = assertNotNull(SurveyPrep.laserFrame("front", p, L, W, H))
        val q = listOf(doubleArrayOf(900.0, 400.0), doubleArrayOf(1900.0, 400.0), doubleArrayOf(1900.0, 1600.0), doubleArrayOf(900.0, 1600.0))
            .map { SurveyPrep.apply(f.m2p, it) }
        // typed 10 % small: the face measures 10 % small
        p.surface("front").extra = SurveyPrep.withKey(p.surface("front").extra, SurveyRef.KEY,
            linkedMapOf<String, Any?>("on" to true, "q" to q.map { listOf(it[0], it[1]) }, "w" to 900.0, "h" to 1080.0))
        val ab = SurveyPrep.sizeOf("front", p, L, W, H)
        near(W * 0.9, ab[0], 1.0, "A"); near(H * 0.9, ab[1], 1.0, "B")
        val d = SurveyPlan.drawing("front", p, L, W, H)
        val r = poly(d, "wall", "front").rings[0]
        assertEquals(ab[0], r[2][0]); assertEquals(ab[1], r[2][1])
        assertEquals("W ${Math.round(ab[0])}", text(d, "dimA").text); assertEquals("H ${Math.round(ab[1])}", text(d, "dimH").text)
        assertEquals("Front · ${Math.round(ab[0])} × ${Math.round(ab[1])} · 0 marks", d.info)
        near(max(ab[0], ab[1]) / 34, d.fs, 1e-9, "fs from the reference size")
        // the plans keep the room's laser W × L
        assertEquals("W 2678", text(SurveyPlan.drawing("floor", p, L, W, H), "dimW").text)
    }

    // ------------------------------------------------------------------ the chips, the line under it, fit

    @Test fun `six drawings, Floor first -- titles -- the room's name from the screen's title`() {
        assertEquals("Floor|Ceiling|Front|Right|Back|Left", SurveyPlan.VIEWS.joinToString("|") { it.second })
        assertEquals(listOf("floor", "ceil", "front", "right", "back", "left"), SurveyPlan.VIEWS.map { it.first })
        assertEquals("Front wall elevation", SurveyPlan.DTITLE["front"]); assertEquals("Floor plan", SurveyPlan.DTITLE["floor"])
        assertEquals("Living Room", SurveyPlan.roomName("LR · Living Room")); assertEquals("Living Room", SurveyPlan.roomName("Living Room"))
        assertEquals("Room · 2678 × 3319", SurveyPlan.infoOf("floor", room(), L, W, H, null))
        assertEquals("Back · 2678 × 2677 · 1 mark", SurveyPlan.infoOf("back", gb(), L, W, H))
        for ((v, _) in SurveyPlan.VIEWS) { val d = SurveyPlan.drawing(v, gb(), L, W, H); assertEquals(v, d.view); assertEquals(SurveyPlan.DTITLE[v], d.title) }
    }

    @Test fun `fit, pinch and pan -- the point under the fingers stays, never wider than the drawing, fit · 150 percent on the button`() {
        val box = doubleArrayOf(-100.0, -200.0, 3000.0, 4000.0); val cw = 340.0; val ch = 500.0
        assertEquals("fit", SurveyPlan.fitLabel(box, box))
        val sx = 200.0; val sy = 260.0
        val u = SurveyPlan.fromScreen(box, cw, ch, sx, sy)
        val z = SurveyPlan.zoom(box, box, 1.5, sx, sy, cw, ch)
        val u2 = SurveyPlan.fromScreen(z, cw, ch, sx, sy); near(u[0], u2[0], 1e-6, "x under the finger"); near(u[1], u2[1], 1e-6, "y")
        assertEquals("fit · 150%", SurveyPlan.fitLabel(z, box)); assertFalse(SurveyPlan.fitted(z, box))
        // a drag moves it with the finger
        val pz = SurveyPlan.pan(z, box, 30.0, -20.0, cw, ch); val k = SurveyPlan.scale(z, cw, ch)
        near(z[0] - 30 / k, pz[0], 1e-9, "pan x"); near(z[1] + 20 / k, pz[1], 1e-9, "pan y")
        // a fitted drawing does not pan; zooming out past the drawing fits it again
        assertEquals(box.toList(), SurveyPlan.pan(box, box, 30.0, 30.0, cw, ch).toList())
        val back = SurveyPlan.zoom(z, box, 1 / 3.0, sx, sy, cw, ch); assertEquals(box.toList(), back.toList()); assertEquals("fit", SurveyPlan.fitLabel(back, box))
        // never narrower than an eighth
        var v = box; repeat(30) { v = SurveyPlan.zoom(v, box, 2.0, sx, sy, cw, ch) }; near(box[2] / 8, v[2], 1e-9, "max zoom")
        // toScreen centres a fitted drawing (xMidYMid meet)
        val s0 = SurveyPlan.toScreen(box, cw, ch, box[0], box[1]); val s1 = SurveyPlan.toScreen(box, cw, ch, box[0] + box[2], box[1] + box[3])
        near(cw - s1[0], s0[0], 1e-9, "centred x"); near(ch - s1[1], s0[1], 1e-9, "centred y")
        assertTrue(min(s0[0], s0[1]) >= -1e-9)
        // origin is toScreen's (0, 0): screen = origin + mm × k, for a fitted and a zoomed view alike
        for (vv in listOf(box, z)) {
            val o = SurveyPlan.origin(vv, cw, ch); val kk = SurveyPlan.scale(vv, cw, ch)
            val t = SurveyPlan.toScreen(vv, cw, ch, 1234.0, -56.0)
            near(t[0], o[0] + 1234.0 * kk, 1e-9, "origin x"); near(t[1], o[1] - 56.0 * kk, 1e-9, "origin y")
        }
    }

    @Test fun `a pinch only zooms -- the centroid drifting does not pan it -- a one-finger drag only pans, a zoom out to fit is null`() {
        val box = doubleArrayOf(-100.0, -200.0, 3000.0, 4000.0); val cw = 340.0; val ch = 500.0
        val sx = 200.0; val sy = 260.0
        // a pinch out with the fingers' midpoint drifting 40 px right: the point under the midpoint stays, no translation
        val u = SurveyPlan.fromScreen(box, cw, ch, sx, sy)
        val z = assertNotNull(SurveyPlan.gesture(null, box, sx, sy, 40.0, 0.0, 1.5, cw, ch), "zoomed in")
        val u2 = SurveyPlan.fromScreen(z, cw, ch, sx, sy); near(u[0], u2[0], 1e-6, "x under the fingers"); near(u[1], u2[1], 1e-6, "y")
        assertEquals(SurveyPlan.zoom(box, box, 1.5, sx, sy, cw, ch).toList(), z.toList(), "the drift adds nothing")
        // a one-finger drag pans a zoomed drawing with the finger, and only pans
        val p = assertNotNull(SurveyPlan.gesture(z, box, sx, sy, 30.0, -20.0, 1.0, cw, ch))
        assertEquals(SurveyPlan.pan(z, box, 30.0, -20.0, cw, ch).toList(), p.toList())
        // a drag on a fitted drawing leaves it fitted (null); a zoom out past the drawing fits it again (null)
        assertNull(SurveyPlan.gesture(null, box, sx, sy, 30.0, 30.0, 1.0, cw, ch))
        assertNull(SurveyPlan.gesture(z, box, sx, sy, 0.0, 0.0, 1 / 3.0, cw, ch))
        // no canvas yet: the view is left as it is
        assertEquals(z.toList(), SurveyPlan.gesture(z, box, sx, sy, 5.0, 5.0, 1.0, 0.0, 0.0)?.toList())
    }
}
