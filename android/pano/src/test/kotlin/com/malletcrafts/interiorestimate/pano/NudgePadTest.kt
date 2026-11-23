package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.NudgePad.Corner
import com.malletcrafts.interiorestimate.pano.NudgePad.Key
import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch.Box
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * FINE MOVE'S PAD (port 63 slice I2a; DESIGN-imagemeter §6, §8.1, §13): which bottom corner it stands in -- the one
 * farthest from the item; what ▲ ◀ ▶ ▼ move, by 1 or 5 mm, on the part last touched; a tap on another handle of the
 * same item re-aiming it; what ends it -- ✓, a tap on empty photo, a reading that lands; and that it never snaps.
 * PickTest's room and wall; the Mi A3's photo, 348 x 514 dp at 2 px a dp.
 *
 * (Test names are plain ASCII: MeasureWalkTest's header says why.)
 */
class NudgePadTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0
    private fun ab(s: String): DoubleArray = SurveyPrep.size(s, L, W, H)
    private fun pt(x: Double, y: Double) = doubleArrayOf(x, y)
    private val dp = 2.0; private val w = 348 * dp; private val h = 514 * dp
    private fun box(l: Double, t: Double, r: Double, b: Double) = Box(l * dp, t * dp, r * dp, b * dp)

    private fun SurveyPrep.Prep.put(s: String, t: String, n: Int, x0: Double, x1: Double, y0: Double, y1: Double,
                                    extra: Map<String, Any?> = emptyMap()): SurveyPrep.Mark =
        SurveyPrep.Mark("id-$s-$t$n", t, SurveyPrep.CODE.getValue(s) + t + n, x0, x1, y0, y1, extra).also { surface(s).marks.add(it) }

    private fun wall(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        p.put("front", "W", 1, 1500.0, 2400.0, 900.0, 2100.0)
        p.put("front", "E", 1, 1325.0, 1375.0, 275.0, 325.0)
        p.put("front", "D", 1, 300.0, 1200.0, 0.0, 2100.0)
        p.put("front", "D", 2, 50.0, 250.0, 0.0, 2100.0, mapOf(MarkLife.KEY_BORN to MarkLife.WISH, MarkLife.KEY_LIFE to MarkLife.WISH))
        p.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 1000.0), pt(1234.0, 1000.0)), "t1")
        p.addMeas("front", SurveyMeas.LINE, listOf(pt(500.0, 1600.0), pt(1300.0, 2200.0)), "m1")
        p.addMeas("front", SurveyMeas.AREA, listOf(pt(1800.0, 300.0), pt(2400.0, 300.0), pt(2400.0, 800.0)), "m2")
        p.addChain("front", "x", listOf(pt(300.0, 2400.0), pt(1200.0, 2400.0), pt(2400.0, 2400.0)), listOf(null, null, null), "k1")
        return p
    }

    private val D1 = "id-front-D1"; private val D2 = "id-front-D2"; private val W1 = "id-front-W1"; private val E1 = "id-front-E1"
    private fun mark(p: SurveyPrep.Prep, id: String) = p.markById(id)!!.second
    private fun meas(p: SurveyPrep.Prep, id: String) = p.measById(id)!!.second
    private fun boxOf(e: SurveyPrep.Mark) = listOf(e.x0, e.x1, e.y0, e.y1)
    private fun start(p: SurveyPrep.Prep, id: String, part: String? = null, stage: Int? = 0) = NudgePad.start(p, id, stage, part, null, w, h, dp)
    private fun sc(p: SurveyPrep.Prep, stage: Int? = 0) = Pick.Scene(p, stage, StepRoute.MARK, ::ab)

    /** A Fine move as the screen runs it: every press through [NudgePad.press], its one undo step counted. */
    private inner class Run(val p: SurveyPrep.Prep, var st: NudgePad.State) {
        var steps = 0
        fun press(k: Key): Boolean { val r = NudgePad.press(p, st, k, W, H) { steps++ }; st = r.state ?: st; return r.moved }
    }

    // ------------------------------------------------------------------ the pad, and where it stands

    @Test fun `the pad is 3 x 3 cells of 40 dp - four arrows round the step, and the tick`() {
        assertEquals(listOf(40.0, 120.0, 8.0), listOf(NudgePad.CELL_DP, NudgePad.SIZE_DP, NudgePad.EDGE_DP)); assertEquals(3, NudgePad.CELLS)
        assertEquals(listOf(listOf(null, Key.UP, Key.DONE), listOf(Key.LEFT, Key.STEP, Key.RIGHT), listOf(null, Key.DOWN, null)), NudgePad.GRID)
        assertEquals(listOf("▲", "◀", "▶", "▼", "✓"), listOf(Key.UP, Key.LEFT, Key.RIGHT, Key.DOWN, Key.DONE).map { NudgePad.glyph(it) })
        // every key is on the pad once
        for (k in Key.entries) assertEquals(1, NudgePad.GRID.flatten().count { it == k }, k.name)
        // where each cell is, and that a press finds it: the pad in the bottom right corner
        val pad = NudgePad.box(Corner.RIGHT, w, h, dp)
        assertEquals(listOf(348.0 - 8 - 120, 514.0 - 8 - 120, 348.0 - 8, 514.0 - 8), listOf(pad.l, pad.t, pad.r, pad.b).map { it / dp })
        assertEquals(listOf(8.0, 386.0, 128.0, 506.0), NudgePad.box(Corner.LEFT, w, h, dp).let { listOf(it.l, it.t, it.r, it.b).map { v -> v / dp } })
        for (k in Key.entries) {
            val c = assertNotNull(NudgePad.cell(pad, k, dp))
            assertEquals(40.0 * dp, c.w); assertEquals(40.0 * dp, c.h)
            assertEquals(k, NudgePad.keyAt(pad, (c.l + c.r) / 2, (c.t + c.b) / 2, dp)); assertEquals(k, NudgePad.keyAt(pad, c.l + 1, c.t + 1, dp))
        }
        // the three empty cells answer nothing; off the pad, nothing
        assertNull(NudgePad.keyAt(pad, pad.l + 5, pad.t + 5, dp)); assertNull(NudgePad.keyAt(pad, pad.l + 5, pad.b - 5, dp)); assertNull(NudgePad.keyAt(pad, pad.r - 5, pad.b - 5, dp))
        assertNull(NudgePad.keyAt(pad, pad.l - 1, pad.t + 50, dp)); assertNull(NudgePad.keyAt(pad, 10.0, 10.0, dp))
        // the centre cell's words, and what a screen reader calls it and the tick
        val p = wall(); val st = start(p, D1)!!
        assertEquals("1 mm", NudgePad.stepText(st)); assertEquals("5 mm", NudgePad.stepText(NudgePad.toggle(st)))
        assertEquals("Step 1 mm · tap to change", NudgePad.stepLabel(Tx.EN, st)); assertEquals("Step 5 mm · tap to change", NudgePad.stepLabel(Tx.EN, NudgePad.toggle(st)))
        assertEquals("Finish fine move", NudgePad.doneLabel(Tx.EN))
    }

    @Test fun `the pad stands in the bottom corner farthest from the item, chosen once`() {
        // an item on the left: the right corner; on the right: the left corner
        assertEquals(Corner.RIGHT, NudgePad.corner(box(20.0, 300.0, 120.0, 480.0), w, h, dp)); assertEquals(Corner.LEFT, NudgePad.corner(box(230.0, 300.0, 330.0, 480.0), w, h, dp))
        // high on the wall, left of the middle: still the right corner; right of the middle: the left
        assertEquals(Corner.RIGHT, NudgePad.corner(box(60.0, 40.0, 160.0, 120.0), w, h, dp)); assertEquals(Corner.LEFT, NudgePad.corner(box(200.0, 40.0, 300.0, 120.0), w, h, dp))
        // dead centre, and an item that is not on the photo: the right, the thumb's
        assertEquals(Corner.RIGHT, NudgePad.corner(box(124.0, 200.0, 224.0, 300.0), w, h, dp)); assertEquals(Corner.RIGHT, NudgePad.corner(null, w, h, dp))
        // a unit along the whole foot of the wall lies under both corners: the one that covers less of it
        assertEquals(Corner.LEFT, NudgePad.corner(box(60.0, 420.0, 348.0, 514.0), w, h, dp)); assertEquals(Corner.RIGHT, NudgePad.corner(box(0.0, 420.0, 300.0, 514.0), w, h, dp))
        // the pad never covers an item that leaves one corner free
        for (bb in listOf(box(20.0, 300.0, 120.0, 480.0), box(230.0, 300.0, 330.0, 480.0), box(140.0, 380.0, 200.0, 500.0))) {
            val pad = NudgePad.box(NudgePad.corner(bb, w, h, dp), w, h, dp)
            assertFalse(pad.l < bb.r && pad.r > bb.l && pad.t < bb.b && pad.b > bb.t, "the pad $pad lies on $bb")
        }
        // chosen as Fine move starts, and kept: a re-aim, a step change and a nudge do not move the pad from under the thumb
        val p = wall()
        val st = assertNotNull(NudgePad.start(p, D1, 0, null, box(20.0, 300.0, 120.0, 480.0), w, h, dp))
        assertEquals(Corner.RIGHT, st.corner)
        assertEquals(Corner.RIGHT, NudgePad.aim(p, st, "r").corner); assertEquals(Corner.RIGHT, NudgePad.toggle(st).corner)
        assertEquals(Corner.RIGHT, NudgePad.press(p, st, Key.RIGHT, W, H) {}.state!!.corner)
    }

    // ------------------------------------------------------------------ where it is offered, and on what part

    @Test fun `Fine move starts only on what may be held - on the part last touched, the whole item when that is not its own`() {
        val p = wall()
        val st = assertNotNull(start(p, D1))
        assertEquals(NudgePad.State(D1, "body", 1, Corner.RIGHT, false), st); assertEquals("body", NudgePad.BODY)
        assertEquals("rt", start(p, D1, "rt")!!.part); assertEquals("t", start(p, W1, "t")!!.part); assertEquals("q2", start(p, W1, "q2")!!.part)
        // a point has no tab; a part that is another item's (a Line's end) is none of a mark's
        assertEquals("body", start(p, E1, "rt")!!.part); assertEquals("body", start(p, D1, NudgePad.pointPart(1))!!.part); assertEquals("body", start(p, D1, "nonsense")!!.part)
        assertEquals("p1", start(p, "m1", NudgePad.pointPart(1))!!.part); assertEquals("body", start(p, "m1", "p2")!!.part, "a Line has two points"); assertEquals("p2", start(p, "m2", "p2")!!.part)
        assertEquals("body", start(p, "t1", "lt")!!.part)
        assertEquals(listOf(0, 3, 12, null, null, null), listOf("p0", "p3", "p12", "p", "body", "q1").map { NudgePad.pointOf(it) })
        // the parts each kind of item has
        assertEquals(listOf("body", "l", "r", "t", "b", "lt", "rt", "lb", "rb", "q0", "q1", "q2", "q3"), NudgePad.parts(p, D1))
        assertEquals(listOf("body"), NudgePad.parts(p, E1)); assertEquals(listOf("body", "p0", "p1"), NudgePad.parts(p, "t1")); assertEquals(listOf("body", "p0", "p1", "p2"), NudgePad.parts(p, "m2"))
        assertTrue(NudgePad.parts(p, "k1").isEmpty()); assertTrue(NudgePad.parts(p, "nobody").isEmpty())
        // NOT a chain, not a locked item, not what the phase holds read-only, and nothing at Estimation or Client approved
        assertNull(start(p, "k1")); assertNull(start(p, "nobody"))
        assertNull(start(p, D1, stage = 1), "a surveyed door at the Wish list"); assertNotNull(start(p, D2, stage = 1), "a door born a wish, there")
        for (stg in listOf(3, 5)) for (id in listOf(D1, D2, "t1")) assertNull(start(p, id, stage = stg), "$id at $stg")
        assertEquals(true, SurveyPrepEdit.setLocked(p, W1, true)); assertNull(start(p, W1))
        // and it is exactly where the panel draws the act
        for (stg in listOf(0, 1, 2, 3, 4, 5, null)) for (id in listOf(D1, D2, W1, E1, "t1", "m1", "m2", "k1"))
            assertEquals(MarkSheet.acts(Tx.EN, p, id, stg, null).has(MarkSheet.FINE), start(p, id, stage = stg) != null, "$id at $stg")
    }

    @Test fun `a tap on another handle of the same item makes that the part nudged - the step and the one undo step stay`() {
        val p = wall()
        val r = Run(p, NudgePad.toggle(start(p, W1, "q0")!!))
        assertTrue(r.press(Key.UP)); assertEquals(1, r.steps)
        // four corners are four taps, not four trips through the panel
        for (part in listOf("q1", "q2", "q3", "t", "lb", "body")) {
            r.st = NudgePad.aim(p, r.st, part)
            assertEquals(part, r.st.part); assertEquals(5, r.st.stepMm); assertTrue(r.st.stepTaken); assertEquals(W1, r.st.id)
        }
        // a part the item does not have changes nothing
        val was = r.st
        assertSame(was, NudgePad.aim(p, was, "p1")); assertSame(was, NudgePad.aim(p, was, "nonsense"))
        // a Line: its other end, then the whole Line
        val line = start(p, "m1", "p0")!!
        assertEquals("p1", NudgePad.aim(p, line, "p1").part); assertEquals("body", NudgePad.aim(p, line, "body").part); assertSame(line, NudgePad.aim(p, line, "rt"))
    }

    // ------------------------------------------------------------------ what the arrows move

    @Test fun `the arrows move the whole mark by 1 mm, or by 5 - up is up the wall - and the tick ends it`() {
        assertEquals(listOf(1, 5), listOf(NudgePad.FINE_MM, NudgePad.COARSE_MM))
        assertEquals(listOf(listOf(0.0, 1.0), listOf(0.0, -1.0), listOf(-1.0, 0.0), listOf(1.0, 0.0)), listOf(Key.UP, Key.DOWN, Key.LEFT, Key.RIGHT).map { NudgePad.delta(it, 1)!!.toList() })
        assertEquals(listOf(0.0, 5.0), NudgePad.delta(Key.UP, 5)!!.toList()); assertNull(NudgePad.delta(Key.STEP, 1)); assertNull(NudgePad.delta(Key.DONE, 5))
        val p = wall(); val e = mark(p, W1)
        val r = Run(p, start(p, W1)!!)
        assertTrue(r.press(Key.UP)); assertEquals(listOf(1500.0, 2400.0, 901.0, 2101.0), boxOf(e))
        assertTrue(r.press(Key.LEFT)); assertEquals(listOf(1499.0, 2399.0, 901.0, 2101.0), boxOf(e))
        assertTrue(r.press(Key.DOWN)); assertTrue(r.press(Key.RIGHT)); assertEquals(listOf(1500.0, 2400.0, 900.0, 2100.0), boxOf(e))
        // the centre cell: 5 mm, and back; it moves nothing itself
        assertFalse(r.press(Key.STEP)); assertEquals(5, r.st.stepMm)
        assertTrue(r.press(Key.RIGHT)); assertEquals(listOf(1505.0, 2405.0, 900.0, 2100.0), boxOf(e))
        assertFalse(r.press(Key.STEP)); assertEquals(1, r.st.stepMm)
        // the tick ends the Fine move and moves nothing
        val done = NudgePad.press(p, r.st, Key.DONE, W, H) { r.steps++ }
        assertNull(done.state); assertFalse(done.moved); assertEquals(listOf(1505.0, 2405.0, 900.0, 2100.0), boxOf(e))
        // ONE undo step for the whole of it
        assertEquals(1, r.steps)
        // readings are not touched: moving does not change what was measured
        p.actual["FW1.w"] = 912.0
        Run(p, start(p, W1)!!).press(Key.UP); assertEquals(912.0, p.actual["FW1.w"])
    }

    @Test fun `an edge moves one way only, a corner tab both - and nothing is ever under the smallest size`() {
        val p = wall(); val e = mark(p, W1)
        // the top edge: up and down move it; left and right are not its arrows -- nothing moves and no undo step is taken
        val top = Run(p, start(p, W1, "t")!!)
        assertEquals(listOf(true, true, false, false, true, true), listOf(Key.UP, Key.DOWN, Key.LEFT, Key.RIGHT, Key.STEP, Key.DONE).map { NudgePad.live(top.st, it) })
        assertFalse(top.press(Key.LEFT)); assertFalse(top.press(Key.RIGHT)); assertEquals(0, top.steps)
        assertTrue(top.press(Key.UP)); assertEquals(listOf(1500.0, 2400.0, 900.0, 2101.0), boxOf(e)); assertEquals(1, top.steps)
        // the left edge: the other two
        val left = Run(p, start(p, W1, "l")!!)
        assertEquals(listOf(false, false, true, true), listOf(Key.UP, Key.DOWN, Key.LEFT, Key.RIGHT).map { NudgePad.live(left.st, it) })
        assertFalse(left.press(Key.UP)); assertTrue(left.press(Key.LEFT)); assertEquals(listOf(1499.0, 2400.0, 900.0, 2101.0), boxOf(e))
        for (part in listOf("r", "b")) assertEquals(part == "b", NudgePad.live(start(p, W1, part)!!, Key.UP), part)
        // a corner tab, the whole mark, a free corner and a Line's end answer all four
        for (part in listOf("body", "lt", "rb", "q1")) for (k in Key.entries) assertTrue(NudgePad.live(start(p, W1, part)!!, k), "$part $k")
        for (k in Key.entries) assertTrue(NudgePad.live(start(p, "m1", "p1")!!, k))
        val rb = Run(p, start(p, W1, "rb")!!)
        assertTrue(rb.press(Key.RIGHT)); assertTrue(rb.press(Key.DOWN)); assertEquals(listOf(1499.0, 2401.0, 899.0, 2101.0), boxOf(e))
        // an edge stops at the smallest size a tab's resize stops at
        val s = p.put("front", "S", 1, 2000.0, 2000.0 + SurveyPrep.MIN_SIZE + 1, 1200.0, 1290.0)
        val sr = Run(p, start(p, s.id, "r")!!)
        assertTrue(sr.press(Key.LEFT)); assertEquals(SurveyPrep.MIN_SIZE, s.x1 - s.x0, 1e-9)
        assertFalse(sr.press(Key.LEFT)); assertEquals(SurveyPrep.MIN_SIZE, s.x1 - s.x0, 1e-9); assertEquals(1, sr.steps)
    }

    @Test fun `it never snaps - a mark a few mm off the corner goes a millimetre at a time, and stops at the face's edge`() {
        val p = wall()
        // 4 mm off the left corner: a DRAG would snap it onto the corner (SurveyPrep.SNAP); the pad steps 3, 2, 1, 0
        val e = p.put("front", "N", 1, 4.0, 304.0, 1000.0, 1300.0)
        assertTrue(SurveyPrep.SNAP > 4.0, "a drag snaps within ${SurveyPrep.SNAP} mm")
        val r = Run(p, start(p, e.id)!!)
        for (want in listOf(3.0, 2.0, 1.0, 0.0)) { assertTrue(r.press(Key.LEFT)); assertEquals(want, e.x0); assertEquals(want + 300, e.x1) }
        // hard against the edge: nothing moves, and a press that moves nothing takes no undo step
        val fresh = Run(p, start(p, e.id)!!)
        assertFalse(fresh.press(Key.LEFT)); assertEquals(0.0, e.x0); assertEquals(0, fresh.steps)
        // and away again by exactly 1 mm: it did not stick
        assertTrue(fresh.press(Key.RIGHT)); assertEquals(1.0, e.x0); assertEquals(1, fresh.steps)
        // the door stands on the floor: down does nothing; 5 mm up, then 5 down, lands exactly back
        val d = mark(p, D1); val dr = Run(p, NudgePad.toggle(start(p, D1)!!))
        assertFalse(dr.press(Key.DOWN)); assertTrue(dr.press(Key.UP)); assertEquals(5.0, d.y0); assertTrue(dr.press(Key.DOWN)); assertEquals(0.0, d.y0)
        // a Line's end 3 mm off its other end's level: a drag snaps it level within 5 degrees; the pad leaves it where it is put
        val m = p.addMeas("front", SurveyMeas.LINE, listOf(pt(100.0, 500.0), pt(900.0, 503.0)), "m9")
        val lr = Run(p, start(p, m.id, "p1")!!)
        assertTrue(lr.press(Key.DOWN)); assertEquals(listOf(900.0, 502.0), m.pts[1].toList()); assertEquals(listOf(100.0, 500.0), m.pts[0].toList())
        assertTrue(lr.press(Key.RIGHT)); assertEquals(listOf(901.0, 502.0), m.pts[1].toList())
    }

    @Test fun `a corner of a four-corner mark moves alone, and stops where the shape would break`() {
        val p = wall(); val e = mark(p, W1)
        val r = Run(p, NudgePad.toggle(start(p, W1, "q0")!!))
        // the bottom-left corner up by 5: the mark is no longer its box, and its box did not move
        assertTrue(r.press(Key.UP)); assertEquals(listOf(1500.0, 2400.0, 900.0, 2100.0), boxOf(e))
        // (its corners are stored as parts of its box to six decimals: read back, they are within a hundredth of a mm)
        fun near(want: List<List<Double>>) = Quad.corners(e).forEachIndexed { i, c -> for (k in 0..1) assertEquals(want[i][k], c[k], 0.01, "corner $i") }
        near(listOf(listOf(1500.0, 905.0), listOf(2400.0, 900.0), listOf(2400.0, 2100.0), listOf(1500.0, 2100.0)))
        assertNotNull(Quad.of(e)); assertEquals(1, r.steps)
        // back down: its box again, the key gone -- the same one undo step
        assertTrue(r.press(Key.DOWN)); assertNull(Quad.of(e)); assertEquals(1, r.steps)
        // the next corner, re-aimed with the pad up: in by 5
        r.st = NudgePad.aim(p, r.st, "q2")
        assertTrue(r.press(Key.LEFT)); near(listOf(listOf(1500.0, 900.0), listOf(2400.0, 900.0), listOf(2395.0, 2100.0), listOf(1500.0, 2100.0))); assertEquals(1, r.steps)
        // a shape that would be refused does not move: a 20 mm switch plate's corner cannot cross its neighbour
        val s = p.put("front", "S", 1, 2000.0, 2000.0 + SurveyPrep.MIN_SIZE, 1200.0, 1290.0)
        val sr = Run(p, NudgePad.toggle(start(p, s.id, "q0")!!))
        assertFalse(sr.press(Key.RIGHT)); assertEquals(listOf(2000.0, 2000.0 + SurveyPrep.MIN_SIZE, 1200.0, 1290.0), boxOf(s)); assertNull(Quad.of(s)); assertEquals(0, sr.steps)
    }

    @Test fun `a Tape, a Line and an area move whole or by one point - a Tape never shorter than it may be plotted`() {
        val p = wall(); val t = meas(p, "t1")
        // whole: both ends by the same step
        val r = Run(p, start(p, "t1")!!)
        assertTrue(r.press(Key.UP)); assertTrue(r.press(Key.RIGHT))
        assertEquals(listOf(listOf(1.0, 1001.0), listOf(1235.0, 1001.0)), t.pts.map { it.toList() }); assertEquals(1, r.steps)
        // ... and it stops at the edge with its shape intact: its first end is 1 mm from the left corner
        assertTrue(r.press(Key.LEFT)); assertFalse(r.press(Key.LEFT)); assertEquals(listOf(listOf(0.0, 1001.0), listOf(1234.0, 1001.0)), t.pts.map { it.toList() })
        // one end alone
        r.st = NudgePad.aim(p, NudgePad.toggle(r.st), "p1")
        assertTrue(r.press(Key.LEFT)); assertEquals(listOf(listOf(0.0, 1001.0), listOf(1229.0, 1001.0)), t.pts.map { it.toList() }); assertEquals(1, r.steps)
        // a Tape is never left shorter than one may be plotted
        val short = p.addMeas("front", SurveyMeas.TAPE, listOf(pt(500.0, 200.0), pt(500.0 + SurveyMeas.MIN_LEN + 2, 200.0)), "t2")
        val sr = Run(p, NudgePad.toggle(start(p, "t2", "p1")!!))
        assertFalse(sr.press(Key.LEFT), "5 mm would leave it 17 mm long"); assertEquals(0, sr.steps)
        sr.st = NudgePad.toggle(sr.st)
        assertTrue(sr.press(Key.LEFT)); assertTrue(sr.press(Key.LEFT)); assertFalse(sr.press(Key.LEFT)); assertEquals(SurveyMeas.MIN_LEN, short.pts[1][0] - short.pts[0][0], 1e-9)
        // an area's corner, and the area whole; its reading is not touched
        val a = meas(p, "m2"); p.actual["F-M2.per"] = 1880.0
        val ar = Run(p, start(p, "m2", "p2")!!)
        assertTrue(ar.press(Key.UP)); assertEquals(listOf(2400.0, 801.0), a.pts[2].toList()); assertEquals(listOf(2400.0, 300.0), a.pts[1].toList())
        ar.st = NudgePad.aim(p, ar.st, "body")
        assertTrue(ar.press(Key.LEFT)); assertEquals(listOf(listOf(1799.0, 300.0), listOf(2399.0, 300.0), listOf(2399.0, 801.0)), a.pts.map { it.toList() })
        assertEquals(1880.0, p.actual["F-M2.per"]); assertEquals(1, ar.steps)
    }

    @Test fun `a locked item, one that is gone and a chain are not moved, and take no undo step`() {
        val p = wall(); val e = mark(p, W1)
        val st = start(p, W1)!!
        assertEquals(true, SurveyPrepEdit.setLocked(p, W1, true))
        var steps = 0
        val r = NudgePad.press(p, st, Key.UP, W, H) { steps++ }
        assertFalse(r.moved); assertSame(st, r.state); assertEquals(0, steps); assertEquals(listOf(1500.0, 2400.0, 900.0, 2100.0), boxOf(e))
        val tape = start(p, "t1")!!; assertEquals(true, SurveyPrepEdit.setLocked(p, "t1", true))
        assertFalse(NudgePad.press(p, tape, Key.UP, W, H) { steps++ }.moved)
        val gone = start(p, D1)!!; assertTrue(p.deleteMark(D1))
        assertFalse(NudgePad.press(p, gone, Key.UP, W, H) { steps++ }.moved)
        // a state that names a chain (none is ever started): nothing
        val k = NudgePad.State("k1", "body", 1, Corner.RIGHT)
        assertFalse(NudgePad.press(p, k, Key.RIGHT, W, H) { steps++ }.moved); assertEquals(listOf(300.0, 2400.0), p.chainById("k1")!!.second.nodes[0].toList())
        assertEquals(0, steps)
    }

    // ------------------------------------------------------------------ what ends it

    @Test fun `it ends by the tick, a tap on empty photo and a reading that lands - by whatever leaves its item not held`() {
        val p = wall(); val s = sc(p)
        // Fine move holds its item (Pick row 16): a window the walk had reached
        val held = Pick.shaped(s, Pick.State(W1, false, "FW1.w"), W1).state
        assertEquals(Pick.State(W1, true, "FW1.w"), held)
        val st = start(p, W1, "t")!!
        assertSame(st, NudgePad.after(p, st, held, 0), "the pad is up while its item is picked and held")
        // a tap on empty photo, an item held (row 10): the hold goes, and the pad with it
        assertNull(NudgePad.after(p, st, Pick.empty(held).state, 0))
        // a reading that lands and passes (row 18), and one that fails the check (row 19): the handles go, so does the pad
        p.actual["FW1.w"] = 900.0
        val landed = Pick.landed(s, held, "FW1.w", busy = false)
        assertFalse(landed.state.held); assertNull(NudgePad.after(p, st, landed.state, 0))
        p.actual["FW1.h"] = 9000.0
        val failed = Pick.landed(s, Pick.State(W1, true, "FW1.h"), "FW1.h", busy = false)
        assertFalse(failed.state.held); assertNull(NudgePad.after(p, st, failed.state, 0))
        // the pad is no sheet and no finger on a grab: it never holds a reading back -- the screen hands `busy = false`
        assertFalse(Pick.landed(s, held, "FW1.w", busy = false).waits)
        // the next figure by hand, undo, another item picked, the item locked, deleted, another face
        assertNull(NudgePad.after(p, st, Pick.next(s, held, "front").state, 0)); assertNull(NudgePad.after(p, st, Pick.undone(s, held).state, 0))
        assertNull(NudgePad.after(p, st, Pick.body(s, held, D1).state, 0)); assertNull(NudgePad.after(p, st, Pick.locked(s, held, W1).state, 0))
        assertNull(NudgePad.after(p, st, Pick.deleted(held, W1).state, 0)); assertNull(NudgePad.after(p, st, Pick.tab(held, "front", "left").state, 0))
        assertNull(NudgePad.after(p, null, held, 0)); assertNull(NudgePad.after(p, st, Pick.NONE, 0))
        // the stage moved under it to one that holds nothing; the item locked by another hand
        assertNull(NudgePad.after(p, st, held, 3)); assertNull(NudgePad.after(p, st, held, 1), "a surveyed window at the Wish list")
        // WHAT DOES NOT END IT: a tap on a handle of the same item (re-aimed), on its body, and on one of its figures
        assertSame(st, NudgePad.after(p, st, Pick.handle(held, "rb").state, 0)); assertEquals("rb", NudgePad.aim(p, st, Pick.handle(held, "rb").nudge!!).part)
        assertSame(st, NudgePad.after(p, st, Pick.body(s, held, W1).state, 0))
        val fig = Pick.figure(s, held, "FW1.h").state
        assertEquals(Pick.State(W1, true, "FW1.h"), fig); assertSame(st, NudgePad.after(p, st, fig, 0))
        // a part its item no longer has (an area that lost a corner to undo, were it still held): the whole item
        val area = start(p, "m2", "p2")!!
        meas(p, "m2").pts.removeAt(2)
        assertEquals("body", NudgePad.after(p, area, Pick.State("m2", true, null), 0)!!.part)
    }
}
