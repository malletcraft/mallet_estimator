package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.I18n
import com.malletcrafts.interiorestimate.pano.i18n.LiteralScan
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * A TAPE, A LINE AND AN AREA MOVE BY THEIR BODY (port 63 slice F1, 16 Nov 2026). Amit, the same evening, with build
 * 0.3.337 on his phone: "tape mark does not move unless its end points are moved. difficult for moving it from one
 * point to other."
 *
 * With the item picked, a drag that starts on its body and outside its ends' grab radius moves the whole item: every
 * point by the same delta, stopped at the face's edge so its shape never changes. What a press is (the body, or an
 * end, or neither) and what the drag does are SurveyGuide's, pure, and held here; where the body stands in the hit
 * order is SurveyPrepTouch.hit's and Pano360.order's. The two gesture loops are compiled only by CI, so that they ask
 * these rules is read from their source.
 *
 * Test names are plain ASCII (see MeasureScreenTest). The synthetic room of MeasTest: L 3319, W 2678, H 2677; the
 * front wall is 2678 x 2677 mm. On screen 1 mm is 0.25 px here at 2 px per dp, so 14 dp is 28 px (112 mm).
 */
class BodyMoveTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0
    private val A = W; private val B = H
    private val dp = 2.0
    private val k = 0.25
    private val endPx = SurveyGuide.END_HIT_DP * dp
    private val bodyPx = SurveyMeas.PICK_DP * dp
    private fun pt(x: Double, y: Double) = doubleArrayOf(x, y)
    private fun scr(m: SurveyPrep.Meas) = m.pts.map { doubleArrayOf(it[0] * k, it[1] * k) }
    private fun pts(m: SurveyPrep.Meas) = m.pts.map { it.toList() }
    private fun room() = SurveyPrep.fresh(L, W, H)
    private fun press(m: SurveyPrep.Meas?, xMm: Double, yMm: Double, tool: String = SurveyPrepPalette.SEL, edit: Boolean = true,
                      areaOpen: Boolean = false, widen: Double = 0.0) =
        SurveyGuide.bodyPress(edit, tool, m, areaOpen, m?.let { scr(it) } ?: emptyList(), xMm * k, yMm * k, endPx, bodyPx, widen)

    // ---- the press: the body, an end, or neither --------------------------------------------------------------

    @Test fun `a press on the picked tape's body grabs it whole - and within 14 dp of an end the end keeps its own press`() {
        val p = room()
        val t = p.addMeas("front", SurveyMeas.TAPE, listOf(pt(400.0, 1000.0), pt(1634.0, 1000.0)), "t1")
        val band = 3.5 * dp
        // the middle of the band, and anywhere along it clear of the ends
        assertSame(SurveyGuide.BodyPress.Grab, press(t, 1000.0, 1000.0, widen = band))
        assertSame(SurveyGuide.BodyPress.Grab, press(t, 600.0, 1010.0, widen = band))
        // 12 dp from the band's edge still takes it; past that it does not
        assertSame(SurveyGuide.BodyPress.Grab, press(t, 1000.0, 1000.0 + (band + bodyPx - 1) / k, widen = band))
        assertNull(press(t, 1000.0, 1000.0 + (band + bodyPx + 1) / k, widen = band))
        // AN END GRAB IS STILL AN END GRAB: within 14 dp of either end the body answers nothing, and the end's own rule does
        for ((i, e) in t.pts.withIndex()) for (off in listOf(0.0, 60.0, -100.0)) {
            assertNull(press(t, e[0] + off, e[1] + 20, widen = band), "end $i, $off mm along")
            val ep = SurveyGuide.endPress(true, SurveyPrepPalette.SEL, t, false, scr(t), (e[0] + off) * k, (e[1] + 20) * k, endPx)
            assertTrue(ep is SurveyGuide.EndPress.Grab && ep.i == i, "end $i is the end's")
        }
        // just outside an end's 14 dp, on the band: the body
        assertSame(SurveyGuide.BodyPress.Grab, press(t, 400.0 + 120.0, 1000.0, widen = band))
        assertNull(SurveyGuide.endPress(true, SurveyPrepPalette.SEL, t, false, scr(t), 520.0 * k, 1000.0 * k, endPx))
        // every press is one or the other or neither -- never both
        for (x in 300..1750 step 25) for (y in listOf(900.0, 980.0, 1000.0, 1030.0, 1150.0)) {
            val body = press(t, x.toDouble(), y, widen = band)
            val end = SurveyGuide.endPress(true, SurveyPrepPalette.SEL, t, false, scr(t), x * k, y * k, endPx)
            assertFalse(body != null && end != null, "($x, $y) is the body's and an end's")
        }
    }

    @Test fun `only the picked item, with Select on, where things move - never in another tool, another step or while an area is tapped`() {
        val p = room()
        val t = p.addMeas("front", SurveyMeas.TAPE, listOf(pt(400.0, 1000.0), pt(1634.0, 1000.0)), "t1")
        assertNotNull(press(t, 1000.0, 1000.0))
        // nothing picked on this face: the screen hands null, and a press on a tape that is NOT picked picks it and moves nothing
        assertNull(press(null, 1000.0, 1000.0))
        // a type, the Tape, the Line or the Chain on: never (with Tape or Line on a press there pulls a new one)
        for (tool in listOf("W", "E", SurveyPrepPalette.TAPE, SurveyPrepPalette.LINE, SurveyPrepPalette.CHAIN)) assertNull(press(t, 1000.0, 1000.0, tool = tool), tool)
        // a step that moves nothing (3 - Measure grabs nothing at all)
        assertNull(press(t, 1000.0, 1000.0, edit = false))
        // a Line area being tapped: its taps are corners
        assertNull(press(t, 1000.0, 1000.0, areaOpen = true))
        // a kind this build does not draw, and points that are not the ones on screen
        assertNull(SurveyGuide.bodyPress(true, SurveyPrepPalette.SEL, SurveyPrep.Meas("x", "arc", "F-X1", mutableListOf(pt(0.0, 0.0), pt(9.0, 9.0))), false,
            listOf(pt(0.0, 0.0), pt(9.0, 9.0)), 1.0, 1.0, endPx, bodyPx))
        assertNull(SurveyGuide.bodyPress(true, SurveyPrepPalette.SEL, t, false, listOf(pt(100.0, 250.0)), 100.0, 250.0, endPx, bodyPx))
        // A LOCKED ONE is still the body's press: picked, and it says why when dragged -- the words a locked end says
        SurveyPrepEdit.setLocked(p, "t1", true)
        val lk = press(t, 1000.0, 1000.0)
        assertTrue(lk is SurveyGuide.BodyPress.Locked && lk.code == "F-T1")
        assertEquals("F-T1 is locked — unlock to move it", SurveyGuide.lockedEnd(Tx.EN, (lk as SurveyGuide.BodyPress.Locked).code))
    }

    @Test fun `a Line is grabbed by its line and an area by its outline - the marks inside an area stay within reach`() {
        val p = room()
        // a slanted Line
        val l = p.addMeas("front", SurveyMeas.LINE, listOf(pt(500.0, 600.0), pt(1700.0, 1500.0)), "m1")
        assertSame(SurveyGuide.BodyPress.Grab, press(l, 1100.0, 1050.0))
        assertSame(SurveyGuide.BodyPress.Grab, press(l, 1100.0 + 40, 1050.0 - 40))           // 57 mm off it: 14 px, inside 12 dp
        assertNull(press(l, 1100.0 + 120, 1050.0 - 120))                                     // 170 mm off: 42 px
        assertNull(press(l, 520.0, 610.0)); assertNull(press(l, 1690.0, 1490.0))             // its ends
        // an area of four corners: its outline takes the press, its inside does not
        val a = p.addMeas("front", SurveyMeas.AREA, listOf(pt(600.0, 600.0), pt(1800.0, 600.0), pt(1800.0, 1600.0), pt(600.0, 1600.0)), "m2")
        assertSame(SurveyGuide.BodyPress.Grab, press(a, 1200.0, 610.0)); assertSame(SurveyGuide.BodyPress.Grab, press(a, 1790.0, 1100.0))
        assertSame(SurveyGuide.BodyPress.Grab, press(a, 610.0, 1100.0), "the closing side, from the last corner back to the first")
        assertNull(press(a, 1200.0, 1100.0), "the middle of an area is not its body: a mark there is still pressed")
        for (c in a.pts) assertNull(press(a, c[0] + 10, c[1] + 10), "a corner is an end's")
    }

    // ---- the move: one delta, stopped at the edge --------------------------------------------------------------

    @Test fun `a horizontal, a vertical and a slanted tape move whole - every point by the same delta, its length untouched`() {
        for ((name, a, b) in listOf(Triple("horizontal", pt(400.0, 1000.0), pt(1634.0, 1000.0)), Triple("vertical", pt(900.0, 300.0), pt(900.0, 2100.0)),
            Triple("slanted", pt(500.0, 600.3), pt(1700.7, 1500.0)))) {
            val p = room()
            val t = p.addMeas("front", SurveyMeas.TAPE, listOf(a, b), "t1")
            val start = t.pts.map { it.copyOf() }; val len = SurveyMeas.plen(start)
            assertTrue(SurveyGuide.bodyMove(t, start, 312.34, -150.06, A, B), name)
            // one delta, to 0.1 mm
            assertEquals(listOf(312.3, -150.1), SurveyGuide.bodyDelta(start, 312.34, -150.06, A, B).toList(), name)
            for (i in start.indices) {
                assertEquals(start[i][0] + 312.3, t.pts[i][0], 1e-9, "$name, point $i"); assertEquals(start[i][1] - 150.1, t.pts[i][1], 1e-9, "$name, point $i")
            }
            // the shape is what it was: the same vector from end to end, so the same length and the same tag
            assertEquals(start[1][0] - start[0][0], t.pts[1][0] - t.pts[0][0], 1e-9, name); assertEquals(start[1][1] - start[0][1], t.pts[1][1] - t.pts[0][1], 1e-9, name)
            assertEquals(len, SurveyMeas.plen(t.pts), 1e-9, name)
            assertEquals(SurveyMeas.tapeTag("F-T1", len), SurveyMeas.tapeTag(t.code, SurveyMeas.plen(t.pts)), name)
            // points that were on the 0.1 mm grid stay on it, with no float dust: what the file writes is tidy
            for (q in t.pts) for (v in q) assertEquals(Math.round(v * 10) / 10.0, v, "$name: $v")
            // the drag is measured from where it BEGAN: the same total delta again lands in the same place, never twice as far
            val once = pts(t)
            assertTrue(SurveyGuide.bodyMove(t, start, 312.34, -150.06, A, B)); assertEquals(once, pts(t), name)
            // and back to nothing puts it exactly where it was
            assertTrue(SurveyGuide.bodyMove(t, start, 0.0, 0.0, A, B)); assertEquals(start.map { it.toList() }, pts(t), name)
            // it is still the same tape: its id, code, kind and lock
            assertEquals("t1", t.id); assertEquals("F-T1", t.code); assertEquals(SurveyMeas.TAPE, t.kind); assertFalse(SurveyPrep.isLocked(t))
        }
    }

    @Test fun `a drag that would push one end off the face stops at the edge - no point is clamped alone`() {
        val p = room()
        // a slanted tape, its right end 178 mm from the right corner, its top end 177 mm from the ceiling
        val t = p.addMeas("front", SurveyMeas.TAPE, listOf(pt(1300.0, 1500.0), pt(2500.0, 2500.0)), "t1")
        val start = t.pts.map { it.copyOf() }; val len = SurveyMeas.plen(start)
        // dragged 600 mm right and 900 mm up: it goes as far as the first point reaches an edge, each way
        assertTrue(SurveyGuide.bodyMove(t, start, 600.0, 900.0, A, B))
        assertEquals(listOf(listOf(1300.0 + 178.0, 1500.0 + 177.0), listOf(A, B)), pts(t))
        assertEquals(len, SurveyMeas.plen(t.pts), 1e-9, "its length is what it was: the far end was not moved alone")
        assertEquals(1200.0, t.pts[1][0] - t.pts[0][0], 1e-9); assertEquals(1000.0, t.pts[1][1] - t.pts[0][1], 1e-9)
        // the other way: down to the floor and into the left corner
        assertTrue(SurveyGuide.bodyMove(t, start, -5000.0, -5000.0, A, B))
        assertEquals(listOf(listOf(0.0, 0.0), listOf(1200.0, 1000.0)), pts(t))
        // one axis stopped, the other free
        assertTrue(SurveyGuide.bodyMove(t, start, 5000.0, -200.0, A, B))
        assertEquals(listOf(listOf(1478.0, 1300.0), listOf(A, 2300.0)), pts(t))
        // every point is on the face wherever it is dragged, and the vector between its ends never changes
        for (dx in listOf(-9000.0, -1300.0, -77.7, 0.0, 12.34, 178.0, 4000.0)) for (dy in listOf(-9000.0, -1500.0, 0.0, 177.0, 3000.0)) {
            SurveyGuide.bodyMove(t, start, dx, dy, A, B)
            for (q in t.pts) assertTrue(q[0] in 0.0..A && q[1] in 0.0..B, "($dx, $dy): ${q.toList()}")
            assertEquals(1200.0, t.pts[1][0] - t.pts[0][0], 1e-9, "($dx, $dy)"); assertEquals(1000.0, t.pts[1][1] - t.pts[0][1], 1e-9, "($dx, $dy)")
        }
        // an edge that is not on the 0.1 mm grid is still reached exactly
        val q = room().addMeas("front", SurveyMeas.TAPE, listOf(pt(100.0, 100.0), pt(900.0, 100.0)), "t2")
        val st = q.pts.map { it.copyOf() }
        SurveyGuide.bodyMove(q, st, 9999.0, 0.0, 1234.56, B)
        assertEquals(1234.56, q.pts[1][0], 1e-9); assertEquals(800.0, q.pts[1][0] - q.pts[0][0], 1e-9)
    }

    @Test fun `a tape plotted on the boundary line is never pulled back by a move and never pushed further out`() {
        // onFace allows a drag to end 25 mm past an edge, so a tape can already reach past it: 20 mm left of the left corner
        val p = room()
        val t = p.addMeas("front", SurveyMeas.TAPE, listOf(pt(-20.0, 800.0), pt(1000.0, 800.0)), "t1")
        val start = t.pts.map { it.copyOf() }
        // no move at all is always allowed: picking it up does not make it jump onto the face
        assertEquals(listOf(0.0, 0.0), SurveyGuide.bodyDelta(start, 0.0, 0.0, A, B).toList())
        // further out: stopped; up and down: free; back onto the face: free
        assertEquals(listOf(0.0, 0.0), SurveyGuide.bodyDelta(start, -300.0, 0.0, A, B).toList())
        assertEquals(listOf(0.0, 250.0), SurveyGuide.bodyDelta(start, -300.0, 250.0, A, B).toList())
        assertEquals(listOf(55.0, 0.0), SurveyGuide.bodyDelta(start, 55.0, 0.0, A, B).toList())
        // an item wider than the face cannot go either way across it
        assertEquals(listOf(0.0, -100.0), SurveyGuide.bodyDelta(listOf(pt(-10.0, 500.0), pt(A + 10, 500.0)), 400.0, -100.0, A, B).toList())
        // nothing to move, and a drag that is no number
        assertEquals(listOf(0.0, 0.0), SurveyGuide.bodyDelta(emptyList(), 5.0, 5.0, A, B).toList())
        assertEquals(listOf(0.0, 0.0), SurveyGuide.bodyDelta(start, Double.NaN, 5.0, A, B).toList())
    }

    @Test fun `an area of four corners moves whole - its area, its perimeter and its reading are what they were`() {
        val p = room()
        val a = p.addMeas("front", SurveyMeas.AREA, listOf(pt(600.0, 600.0), pt(1800.0, 600.0), pt(1900.0, 1600.0), pt(500.0, 1500.0)), "m2")
        p.actual["F-M1.per"] = 4567.0
        val start = a.pts.map { it.copyOf() }
        val area = SurveyMeas.parea(start); val per = SurveyMeas.pperim(start)
        assertTrue(SurveyGuide.bodyMove(a, start, 250.0, 400.0, A, B))
        for (i in start.indices) assertEquals(listOf(start[i][0] + 250.0, start[i][1] + 400.0), a.pts[i].toList(), "corner $i")
        assertEquals(area, SurveyMeas.parea(a.pts), 1e-6); assertEquals(per, SurveyMeas.pperim(a.pts), 1e-9)
        // pushed at the right corner and the ceiling: the WHOLE area stops when its furthest corner gets there
        assertTrue(SurveyGuide.bodyMove(a, start, 5000.0, 5000.0, A, B))
        assertEquals(A, a.pts.maxOf { it[0] }, 1e-9); assertEquals(B, a.pts.maxOf { it[1] }, 1e-9)
        for (i in start.indices) {
            assertEquals(start[i][0] - start[0][0], a.pts[i][0] - a.pts[0][0], 1e-9, "corner $i keeps its place in the shape")
            assertEquals(start[i][1] - start[0][1], a.pts[i][1] - a.pts[0][1], 1e-9, "corner $i")
        }
        assertEquals(area, SurveyMeas.parea(a.pts), 1e-6); assertEquals(per, SurveyMeas.pperim(a.pts), 1e-9)
        // MOVING DOES NOT CHANGE WHAT WAS MEASURED: its reading is not touched, and it is still the reading the walk asks
        assertEquals(4567.0, p.actual["F-M1.per"]); assertEquals("F-M1.per", SurveyMeas.key(a)); assertEquals(1, p.actual.size)
        assertEquals("F-M1 ${SurveyMeas.m2txt(area)} · 4567 mm", SurveyMeas.lineTag(a.code, a.kind, a.pts, reading = p.actual["F-M1.per"]))
    }

    @Test fun `a Line keeps its reading when it is moved - and a locked one does not move at all`() {
        val p = room()
        val l = p.addMeas("front", SurveyMeas.LINE, listOf(pt(500.0, 600.0), pt(1700.0, 1500.0)), "m1")
        p.actual["F-M1.len"] = 1502.0
        val start = l.pts.map { it.copyOf() }
        assertTrue(SurveyGuide.bodyMove(l, start, -200.0, 300.0, A, B))
        assertEquals(listOf(listOf(300.0, 900.0), listOf(1500.0, 1800.0)), pts(l))
        assertEquals(1502.0, p.actual["F-M1.len"]); assertEquals("F-M1 1502 mm", SurveyMeas.lineTag(l.code, l.kind, l.pts, reading = p.actual["F-M1.len"], approx = true))
        // LOCKED: nothing moves, nothing is written
        SurveyPrepEdit.setLocked(p, "m1", true)
        val was = pts(l); val arrays = l.pts.toList()
        assertFalse(SurveyGuide.bodyMove(l, l.pts.map { it.copyOf() }, 400.0, 400.0, A, B))
        assertEquals(was, pts(l)); for (i in arrays.indices) assertSame(arrays[i], l.pts[i], "not even rewritten")
        // unlocked again it moves; a start that is not this item's points moves nothing
        SurveyPrepEdit.setLocked(p, "m1", false)
        assertFalse(SurveyGuide.bodyMove(l, listOf(pt(0.0, 0.0)), 50.0, 50.0, A, B)); assertFalse(SurveyGuide.bodyMove(l, emptyList(), 50.0, 50.0, A, B))
        assertEquals(was, pts(l))
        assertTrue(SurveyGuide.bodyMove(l, l.pts.map { it.copyOf() }, 50.0, 50.0, A, B))
    }

    @Test fun `one undo puts the whole item back where it was, with its reading`() {
        val p = room()
        val l = p.addMeas("front", SurveyMeas.LINE, listOf(pt(500.0, 600.0), pt(1700.0, 1500.0)), "m1")
        p.addMeas("front", SurveyMeas.TAPE, listOf(pt(100.0, 200.0), pt(900.0, 200.0)), "t1")
        p.actual["F-M1.len"] = 1502.0
        // the screen's one snapshot, taken at the drag's first move; then the drag, in many steps, all from the same start
        val snap = p.copy()
        val start = l.pts.map { it.copyOf() }
        for (step in 1..20) SurveyGuide.bodyMove(l, start, step * 31.0, step * -17.0, A, B)
        assertEquals(listOf(listOf(500.0 + 620.0, 600.0 - 340.0), listOf(1700.0 + 620.0, 1500.0 - 340.0)), pts(l))
        // the snapshot was not moved with it
        assertEquals(listOf(listOf(500.0, 600.0), listOf(1700.0, 1500.0)), snap.measById("m1")!!.second.pts.map { it.toList() })
        // undo: the geometry of then, the readings of now
        val back = SurveyPrepEdit.undoTo(p, snap)
        assertEquals(listOf(listOf(500.0, 600.0), listOf(1700.0, 1500.0)), back.measById("m1")!!.second.pts.map { it.toList() })
        assertEquals(listOf(listOf(100.0, 200.0), listOf(900.0, 200.0)), back.measById("t1")!!.second.pts.map { it.toList() }, "the other item never moved")
        assertEquals(1502.0, back.actual["F-M1.len"])
        assertEquals(listOf("F-M1", "F-T1"), back.itemsOf("front").map { it.code })
    }

    // ---- where the body stands in the hit order ----------------------------------------------------------------

    @Test fun `the picked item's body comes after a chain's point and a figure, and before every mark under it`() {
        // a window under the tape, 100..200 x 100..200 px; a figure's tap box and a chain's point on the tape too
        val window = SurveyPrepTouch.Drawn("w1", 100.0, 200.0, 100.0, 200.0)
        val tag = SurveyPrepTouch.Tag("FW1.w", SurveyPrepTouch.Box(120.0, 140.0, 160.0, 160.0))
        val nodes = listOf(SurveyPrepTouch.Nodes("k1", listOf(pt(180.0, 150.0), pt(400.0, 150.0))))
        val tol = 16 * dp
        fun hit(x: Double, y: Double, body: Boolean, tags: List<SurveyPrepTouch.Tag> = listOf(tag), nd: List<SurveyPrepTouch.Nodes> = nodes) =
            SurveyPrepTouch.hit(listOf(window), null, tags, x, y, tol, dp, nd, 16 * dp, body = body)
        // on the tape's band over the window, clear of the figure and the point: the BODY, not the window
        assertSame(SurveyPrepTouch.Hit.Body, hit(105.0, 150.0, true, nd = emptyList()))
        val under = hit(105.0, 150.0, false, nd = emptyList())
        assertTrue(under is SurveyPrepTouch.Hit.Mark && under.id == "w1" && under.part == "body", "with no body under the finger it is the window, as before")
        // a figure under the same finger keeps the press: a tap on it still opens its reading
        val fig = hit(140.0, 150.0, true, nd = emptyList())
        assertTrue(fig is SurveyPrepTouch.Hit.Tag && fig.code == "FW1.w")
        // and so does a chain's point within its 16 dp
        val node = hit(185.0, 150.0, true, tags = emptyList())
        assertTrue(node is SurveyPrepTouch.Hit.Node && node.id == "k1" && node.i == 0)
        // off every mark: the body still, and with no body nothing -- the photo slides
        assertSame(SurveyPrepTouch.Hit.Body, hit(300.0, 400.0, true, tags = emptyList(), nd = emptyList()))
        assertNull(hit(300.0, 400.0, false, tags = emptyList(), nd = emptyList()))
        // the default is NO body: 3 - Measure's hit, which hands none, can never answer one
        assertNull(SurveyPrepTouch.hit(emptyList(), null, emptyList(), 300.0, 400.0, tol, dp))
        assertTrue(SurveyPrepTouch.hit(listOf(window), null, emptyList(), 150.0, 150.0, tol, dp) is SurveyPrepTouch.Hit.Mark)
    }

    // ---- the two gesture loops, read as source (only CI compiles :app) ------------------------------------------

    private val app = "app/src/main/kotlin/com/malletcrafts/interiorestimate/"
    private fun code(rel: String): String { val f = I18n.android.resolve(rel); assertTrue(f.isFile, rel); return LiteralScan.scan(I18n.android, rel).masked }
    private fun count(hay: String, needle: String): Int = Regex.fromLiteral(needle).findAll(hay).count()

    @Test fun `2 Mark and 4 360 both ask the body rule, move through bodyMove behind the dead zone, and hold a locked or read-only one`() {
        val c = code(app + "SurveyPrepScreen.kt"); val p3 = code(app + "SurveyPrep360.kt")
        // 2 - MARK: the press is :pano's, with the step, the tool, the area being tapped, the end's 14 dp and the pick's 12 dp
        assertEquals(1, count(c, "fun bodyGrab(sx: Float, sy: Float): SpGrab? {"))
        assertEquals(1, count(c, "SurveyGuide.bodyPress("))
        assertTrue(Regex("""SurveyGuide\.bodyPress\(mode == "[^"]*", toolS, m, !areaS\.isNullOrEmpty\(\), scr, sx\.toDouble\(\), sy\.toDouble\(\),\s*SurveyGuide\.END_HIT_DP \* dpx, SurveyMeas\.PICK_DP \* dpx,\s*if \(m\.kind == SurveyMeas\.TAPE\) SurveyMeas\.bandHalfPx\(k\(\) / sq\.mpp, dpx\) else 0\.0\) \?: return null""").containsMatchIn(c),
            "the body rule is asked with the tape's own band")
        // only the item PICKED on the open face
        assertEquals(2, count(c, "val m = prepS.surface(tab).meas.firstOrNull { it.id == sid } ?: return null"), "the end's press and the body's: each of the item picked on the open face")
        // ... since port 63 slice I1b the HELD one (Pick.handleId): a picked item's body is pressed only once it is held
        val grab = c.substring(c.indexOf("fun bodyGrab(sx: Float, sy: Float): SpGrab? {"), c.indexOf("fun markHit(sx: Float, sy: Float): SurveyPrepTouch.Hit? {"))
        assertEquals(1, count(grab, "val sid = Pick.handleId(pickS) ?: return null"))
        assertEquals(1, count(grab, "val m = prepS.surface(tab).meas.firstOrNull { it.id == sid } ?: return null"))
        // it stands in the hit order where :pano puts it: one hit test, handed whether the finger is on the body
        // (port 63 slice I1b: markHit asks whether the finger is on the body; markGrab makes the grab -- the same rule, twice read)
        assertEquals(2, count(c, "bodyGrab(sx, sy)"))
        assertEquals(1, count(c, "nodes, SurveyChain.NODE_HIT_DP * density.density, body = bodyGrab(sx, sy) != null)"))
        assertEquals(1, count(c, "is SurveyPrepTouch.Hit.Body -> bodyGrab(sx, sy)"))
        assertEquals(1, count(c, "SurveyPrepTouch.hit("), "2 - Mark's alone: 3 - Measure is gone (port 63 slice I1b)")
        // the move: every point from where the drag BEGAN, through the one rule, behind the dead zone (gate), with no guide
        assertEquals(1, count(c, "SurveyGuide.bodyMove("))
        assertEquals(1, count(c, "val start = m.pts.map { it.copyOf() }; val mx0 = sq.mmX(at[0]); val my0 = sq.mmY(at[1])"))
        // ... the body's dead zone is Pick.BODY_SLOP_DP (port 63 slice I1b, SpGrab.slop), an end's stays END_DEAD_DP
        assertEquals(1, count(c, "return SpGrab(at[0], at[1], pick, null, slop = true) { jx, jy ->"))
        assertEquals(1, count(c, "SurveyGuide.bodyMove(m, start, sq.mmX(jx) - mx0, sq.mmY(jy) - my0, ab[0], ab[1])"))
        // a locked one and one the phase holds: picked, nothing moves, and the drag says why
        assertEquals(1, count(c, "if (bp is SurveyGuide.BodyPress.Locked) return SpGrab(at[0], at[1], pick, null, bp.code, endLock = true, gate = true) { _, _ -> }"))
        assertEquals(1, count(c, "if (!PhaseTools.editable(m, stageS)) return SpGrab(at[0], at[1], pick, null, PhaseTools.refuseText(txS, m, stageS), gate = true, said = true) { _, _ -> }"))
        // ONE UNDO STEP: the gesture takes its one snapshot at the first move past the gate, for every grab alike
        val gate = "if (if (g.slop) bodyGate.move(e.dx, e.dy) else !gated || endGate.move(e.dx, e.dy)) {"
        assertEquals(1, count(c, gate)); assertEquals(1, count(c, "if (!snapped) { onBeforeChangeS(); snapped = true }"))
        assertTrue(c.indexOf(gate) < c.indexOf("if (!snapped) { onBeforeChangeS(); snapped = true }"), "the snapshot is taken past the gate")
        assertEquals(1, count(c, "val bodyGate = SurveyGuide.EndGate(max(Pick.BODY_SLOP_DP.dp.toPx().toDouble(), viewConfiguration.touchSlop.toDouble()))"))
        assertEquals(1, count(c, "grab = g; if (g.gate) gated = true; gc.grabbed()"), "a gated grab waits for the dead zone")
        // 4 - 360: the same rule through the 360's own pick order, the same move in the surface's mm
        assertEquals(1, count(p3, "val body = sq?.let { (s, m) -> Pano360.measLine(v, s, fr.getValue(s), m, d) }"))
        assertEquals(1, count(p3, "return P3Scene(Pano360.Scene(ends, tabs, nodes, polys, sid, body), sq, sm, chainL, measL)"))
        assertEquals(1, count(p3, "Pano360.Pick.Body -> {"))
        assertEquals(1, count(p3, "SurveyGuide.bodyMove(m, start, mm[0] - m0[0], mm[1] - m0[1], ab0[0], ab0[1])")); assertEquals(1, count(p3, "SurveyGuide.bodyMove("))
        val at = p3.indexOf("Pano360.Pick.Body -> {"); val end = p3.indexOf("Pano360.Pick.Mark -> {", at)
        assertTrue(at > 0 && end > at)
        val branch = p3.substring(at, end)
        assertEquals(1, count(branch, "actS.grab(SpSel(s, id = m.id)); actS.focus(s); gc.grabbed(); gated = true"))
        assertEquals(1, count(branch, "grab = if (SurveyPrep.isLocked(m)) P3Grab(s, m.id, m.code, endLock = true, gate = true) { }"))
        assertEquals(1, count(branch, "else if (!PhaseTools.editable(m, stageS)) P3Grab(s, m.id, PhaseTools.refuseText(txS, m, stageS), gate = true, said = true) { }"))
        assertEquals(1, count(branch, "val m0 = mmAt(s, e.x, e.y); val start = m.pts.map { it.copyOf() }"))
        assertEquals(1, count(branch, "P3Grab(s, m.id, gate = true) { mm ->"))
        assertEquals(1, count(p3, "if (!snapped) { actS.before(); snapped = true }"))
        // the pure rules the two loops share
        assertEquals(listOf(Pano360.Step.END, Pano360.Step.TABS, Pano360.Step.NODES, Pano360.Step.BODY, Pano360.Step.MARK, Pano360.Step.LOOK), Pano360.order(SurveyPrepPalette.SEL, false))
    }
}
