package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Landing.Haptic
import com.malletcrafts.interiorestimate.pano.NudgePad.Key
import com.malletcrafts.interiorestimate.pano.Pick.State
import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch.Box
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * THE FOUR-CORNER MARK ON THE SCREEN -- THE PURE RULES SLICE Q2 CALLS (port 63 slice Q2a, 23 Nov 2026;
 * android/docs/port63/DESIGN-quad.md §11). Amit, 16 Nov 2026, after a balcony window job: "while marking window, I
 * realised that it can draw only rectangular windows. there are scenarios when i need to draw trapezoidal windows as
 * well like one just exported." -- "Four corners": any corner is dragged, and each of the four sides carries its own
 * figure. And, on the screen itself: "No foto estimation at all. It confuses user" and "i want to see all measurements
 * in its units always visible".
 *
 * :app is compiled only by CI, so everything the screen does for a four-corner mark is asked of :pano and held HERE:
 * how `freeId` lives, which handles a mark shows and what the code chip and the press are handed, the outline grown
 * for the gold frame and the dim hole, its figures with no photo estimate, the walk inside it, the gross guard on a
 * side, Fine move on a corner, the panel's two shape acts as one step, and the 360's one `free`. The closing check's
 * sentence is MeasureFlowTest's; the panel's figures with the row drawn are DeleteGuardTest's and QuadMeetsF1Test's.
 *
 * The design's own made-up example throughout (never a job's figures): the front wall 3600 x 2700 of PROJ-T1; window
 * FW1 with uprights 1550 (right) and 780 (left), a level top of 2370 and a sloping bottom. The photo as the Mi A3
 * shows that wall: 680 x 510 px, 2 px a dp. (Test names are plain ASCII: MeasureWalkTest's header says why.)
 */
class QuadScreenTest {

    private val L = 5000.0; private val A = 3600.0; private val B = 2700.0
    private fun ab(s: String): DoubleArray = SurveyPrep.size(s, L, A, B)
    private fun pt(x: Double, y: Double) = doubleArrayOf(x, y)
    private val en = Tx.EN
    private val SURVEY = PhaseTools.SURVEY

    private val CW = 680.0; private val CH = 510.0; private val DP = 2.0; private val U = CW / 480
    private fun toPx(x: Double, y: Double): DoubleArray = doubleArrayOf(x * CW / A, (B - y) * CH / B)
    private val textW = { t: String, fs: Double, _: Boolean -> t.length * fs * 0.6 }

    private val W1 = "w1"; private val D1 = "d1"
    private val SIDES = listOf("FW1.bottom", "FW1.right", "FW1.top", "FW1.left")
    private val ASKED = SIDES + listOf("FW1.l", "FW1.s", "FW1.p")

    private fun window(): SurveyPrep.Mark = SurveyPrep.Mark(W1, "W", "FW1", 600.0, 2970.0, 900.0, 2450.0).also {
        assertTrue(Quad.set(it, listOf(pt(600.0, 1670.0), pt(2970.0, 900.0), pt(2970.0, 2450.0), pt(600.0, 2450.0))))
    }
    /** The example's wall: the four-corner window alone. */
    private fun wall(): SurveyPrep.Prep = SurveyPrep.fresh(L, A, B).also { it.surface("front").marks.add(window()) }
    /** ... and with a plain door beside it. */
    private fun wallWithDoor(): SurveyPrep.Prep = wall().also {
        it.surface("front").marks.add(SurveyPrep.Mark(D1, "D", "FD1", 3050.0, 3450.0, 0.0, 2100.0)) }
    private fun mark(p: SurveyPrep.Prep, id: String) = p.markById(id)!!.second
    private fun sc(p: SurveyPrep.Prep, stage: Int? = SURVEY, step: String = StepRoute.MARK) = Pick.Scene(p, stage, step, ::ab)
    private fun boxOf(e: SurveyPrep.Mark) = listOf(e.x0, e.x1, e.y0, e.y1)
    private fun near(e: Double, g: Double, what: String, tol: Double = 1e-6) = assertTrue(abs(e - g) <= tol, "$what: expected $e, got $g")

    // ------------------------------------------------------------------ 1. how freeId lives

    @Test fun `freeId lives by one rule - set by the act and by a corner grabbed, kept while its mark is picked, gone when the selection leaves`() {
        // nothing picked: nothing is free
        assertNull(Quad.freed(null, null, NudgePad.BODY)); assertNull(Quad.freed(W1, null, "q0")); assertNull(Quad.freed(W1, null, null))
        // the panel's act set it (MarkSheet.shapeStep): kept while that mark is the picked one, whatever part is touched
        for (part in listOf(NudgePad.BODY, "q2", null)) assertEquals(D1, Quad.freed(D1, D1, part), "$part")
        // a corner of the picked mark grabbed: that mark, with nothing set before -- and over another mark's stale id
        for (q in Quad.PARTS) { assertEquals(D1, Quad.freed(null, D1, q), q); assertEquals(D1, Quad.freed(W1, D1, q), q) }
        // any other part of a mark nobody freed: nothing -- a tab, the body, a Line's end
        for (part in SurveyPrepTouch.TABS + NudgePad.BODY + NudgePad.pointPart(0) + Pick.nodePart(1)) assertNull(Quad.freed(null, D1, part), part)
        // THE SELECTION LEFT THAT MARK: cleared, so a shape that is its box is a rectangle with eight tabs again
        assertNull(Quad.freed(D1, W1, NudgePad.BODY)); assertNull(Quad.freed(D1, "m1", NudgePad.pointPart(1)))

        // THE WHOLE LIFE, as the screen runs it: a door opened from the file, with no key and nothing freed
        val p = wallWithDoor(); val door = mark(p, D1)
        var freeId: String? = null; var part = NudgePad.BODY; var pick = Pick.NONE
        fun took(o: Pick.Out, body: Boolean = false) { val before = pick; pick = o.state; part = NudgePad.touched(part, before, o, body); freeId = Quad.freed(freeId, pick.picked, part) }
        fun handlesNow(e: SurveyPrep.Mark) = Quad.handles(e, Pick.handles(pick, e.id) && !SurveyPrep.isLocked(e), Quad.freeOf(freeId, pick.picked, part)(e))
        took(Pick.body(sc(p), pick, D1), body = true)
        assertNull(freeId); assertEquals(Quad.Handles.TABS, handlesNow(door))
        // `Four corners`: nothing in the record, its corners free, the mark held
        val s = MarkSheet.shapeStep(en, p, D1, SurveyPrepEdit.SHAPE_FREE, SURVEY, freeId, part) { error("no undo step") }
        freeId = s.freeId; part = s.part; took(Pick.shaped(sc(p), pick, D1))
        assertEquals(D1, freeId); assertEquals(Quad.Handles.CORNERS, handlesNow(door)); assertNull(Quad.of(door))
        // a corner pressed and dragged out: the key is stored; dragged back to the box: the key GOES, in the middle of
        // the drag, and its handles stay four corners -- under the finger that still holds q2
        took(Pick.handle(pick, "q2")); assertEquals("q2", part)
        assertTrue(SurveyPrep.moveMark(door, door.copy(), "q2", -120.0, 0.0, A, B)); assertNotNull(Quad.of(door)); assertEquals(Quad.Handles.CORNERS, handlesNow(door))
        assertTrue(SurveyPrep.moveMark(door, door.copy(), "q2", 120.0, 0.0, A, B)); assertNull(Quad.of(door)); assertEquals(Quad.Handles.CORNERS, handlesNow(door))
        // a later press on its body: the whole mark is the part again, and it is STILL free
        took(Pick.body(sc(p), pick, D1), body = true); assertEquals(NudgePad.BODY, part); assertEquals(D1, freeId); assertEquals(Quad.Handles.CORNERS, handlesNow(door))
        // the window picked: the selection left the door, which is a rectangle with eight tabs when it is picked next
        took(Pick.body(sc(p), pick, W1), body = true); assertNull(freeId)
        assertEquals(Quad.Handles.CORNERS, handlesNow(mark(p, W1)), "the window carries a quad: free with nothing set")
        took(Pick.body(sc(p), pick, D1), body = true); assertNull(freeId); assertEquals(Quad.Handles.TABS, handlesNow(door))
        // A CORNER GRABBED WITH NOTHING FREED (the mark came from the file with its quad, was dragged back to its box):
        // the grab itself frees it -- rule 3 while the finger is down, freeId from then on
        val w = mark(p, W1)
        took(Pick.body(sc(p), pick, W1), body = true); took(Pick.handle(pick, "q0")); assertEquals(W1, freeId)
        assertTrue(SurveyPrep.moveMark(w, w.copy(), "q0", 0.0, -770.0, A, B)); assertNull(Quad.of(w), "back to its box")
        took(Pick.body(sc(p), pick, W1), body = true); assertEquals(Quad.Handles.CORNERS, handlesNow(w))
        // a tap on empty photo lets it go: nothing is picked, so nothing is free
        took(Pick.empty(pick)); assertNull(pick.picked); assertNull(freeId); assertEquals(Quad.Handles.NONE, handlesNow(w))
        took(Pick.body(sc(p), pick, W1), body = true); assertEquals(Quad.Handles.TABS, handlesNow(w), "its box, and nothing freed: a rectangle again")
    }

    @Test fun `one free for every mark of the screen - the picked part is the picked mark's alone`() {
        val p = wallWithDoor(); val w = mark(p, W1); val d = mark(p, D1)
        val e1 = SurveyPrep.Mark("e1", "E", "FE1", 300.0, 350.0, 2300.0, 2350.0).also { p.surface("front").marks.add(it) }
        // nothing picked: a mark with a quad is free, a rectangle is not
        Quad.freeOf(null, null, null).let { f -> assertTrue(f(w)); assertFalse(f(d)); assertFalse(f(e1)) }
        // the door picked by a corner: the door is free by ITS part -- which frees no other mark
        Quad.freeOf(null, D1, "q1").let { f -> assertTrue(f(d)); assertTrue(f(w)); assertFalse(f(e1)) }
        val bare = SurveyPrep.Mark("d2", "D", "FD2", 100.0, 500.0, 0.0, 2100.0)
        assertFalse(Quad.freeOf(null, D1, "q1")(bare), "another mark is not freed by the picked mark's part")
        assertFalse(Quad.freeOf(D1, D1, NudgePad.BODY)(bare)); assertTrue(Quad.freeOf(D1, D1, NudgePad.BODY)(d))
        // a point is never free, whatever the three say
        assertFalse(Quad.freeOf("e1", "e1", "q0")(e1))
        // by id, as Fine move asks it: a Line, a chain and nobody have no corners to free
        assertTrue(Quad.freeIn(p, D1, D1, D1, NudgePad.BODY)); assertTrue(Quad.freeIn(p, W1, null, D1, "lt")); assertFalse(Quad.freeIn(p, D1, null, D1, "lt"))
        assertTrue(Quad.freeIn(p, D1, null, D1, "q3")); assertFalse(Quad.freeIn(p, "e1", "e1", "e1", "q0")); assertFalse(Quad.freeIn(p, "nobody", "nobody", "nobody", "q0"))
        // it IS Quad.free, mark by mark
        for (e in listOf(w, d, e1, bare)) for (fid in listOf(null, D1, "d2")) for (pk in listOf(null, D1, "d2")) for (part in listOf(null, NudgePad.BODY, "lt", "q3"))
            assertEquals(Quad.free(e, fid, if (e.id == pk) part else null), Quad.freeOf(fid, pk, part)(e), "${e.id} $fid $pk $part")
    }

    // ------------------------------------------------------------------ 2. which handles, and what the press and the chip are handed

    @Test fun `which handles a mark shows - none unless it shows them, never on a spot, four corners where they are free, else eight tabs`() {
        val w = window(); val d = SurveyPrep.Mark(D1, "D", "FD1", 3050.0, 3450.0, 0.0, 2100.0)
        val e = SurveyPrep.Mark("e1", "E", "FE1", 300.0, 350.0, 2300.0, 2350.0)
        val n = SurveyPrep.Mark("n1", MarkTypes.NOTE, "FNOTE1", 100.0, 150.0, 1200.0, 1250.0)
        for (m in listOf(w, d, e, n)) for (free in listOf(false, true)) assertEquals(Quad.Handles.NONE, Quad.handles(m, false, free), m.code)
        assertEquals(Quad.Handles.TABS, Quad.handles(d, true, false)); assertEquals(Quad.Handles.CORNERS, Quad.handles(d, true, true))
        assertEquals(Quad.Handles.CORNERS, Quad.handles(w, true, true)); assertEquals(Quad.Handles.TABS, Quad.handles(w, true, false))
        for (free in listOf(false, true)) { assertEquals(Quad.Handles.NONE, Quad.handles(e, true, free)); assertEquals(Quad.Handles.NONE, Quad.handles(n, true, free)) }
        // THE FIGURES' LAYOUT ASKS THE SAME RULE: what it keeps clear is what the painter draws
        for (m in listOf(w, d)) for (picked in listOf(false, true)) for (held in listOf(false, true)) for (free in listOf(false, true)) {
            val laid = DimLayout.markShown(en, emptyMap(), "front", m, A, B, picked, held, null, ::toPx, U, DP, textW, null, free)
            val hs = Quad.handles(m, picked && held, free)
            assertEquals(hs == Quad.Handles.TABS, laid.tabs, "${m.code} $picked $held $free"); assertEquals(hs == Quad.Handles.CORNERS, laid.corners != null, "${m.code} $picked $held $free")
            laid.corners?.let { cs -> assertEquals(Quad.corners(m).map { toPx(it[0], it[1]).toList() }, cs.map { it.toList() }) }
        }
        // the corners a press and the chip are handed: q0 ... q3 in mm, the box's own four with no key; null when not free
        assertNull(Quad.freeCorners(d, false)); assertNull(Quad.freeCorners(e, true)); assertNull(Quad.freeCorners(n, true))
        assertEquals(listOf(listOf(3050.0, 0.0), listOf(3450.0, 0.0), listOf(3450.0, 2100.0), listOf(3050.0, 2100.0)), Quad.freeCorners(d, true)!!.map { it.toList() })
        assertEquals(listOf(listOf(600.0, 1670.0), listOf(2970.0, 900.0), listOf(2970.0, 2450.0), listOf(600.0, 2450.0)), Quad.freeCorners(w, true)!!.map { it.map { v -> Math.round(v * 10) / 10.0 } })
    }

    @Test fun `the press and the chip - four corners where the eight tabs stand, the body only inside the shape, the chip 16 dp clear of every corner`() {
        val p = wallWithDoor(); val w = mark(p, W1)
        val free = Quad.freeOf(null, W1, NudgePad.BODY)
        fun drawn(e: SurveyPrep.Mark): SurveyPrepTouch.Drawn {
            val a = toPx(e.x0, e.y1); val b = toPx(e.x1, e.y0)
            return SurveyPrepTouch.Drawn(e.id, a[0], b[0], a[1], b[1], corners = Quad.freeCorners(e, free(e))?.map { toPx(it[0], it[1]) }, spot = null)
        }
        val marks = p.surface("front").marks.map { drawn(it) }
        assertNotNull(marks.first { it.id == W1 }.corners); assertNull(marks.first { it.id == D1 }.corners)
        val tol = 16 * DP
        fun hit(x: Double, y: Double, sel: String? = W1) = SurveyPrepTouch.hit(marks, sel, emptyList(), x, y, tol, DP)
        // each corner of the HELD window is the part it drags; the middle of an edge is no handle any more
        Quad.corners(w).forEachIndexed { i, c -> val at = toPx(c[0], c[1]); val h = assertIs<SurveyPrepTouch.Hit.Mark>(hit(at[0] + 3, at[1] - 3)); assertEquals(W1, h.id); assertEquals(Quad.PARTS[i], h.part) }
        val topMid = toPx((w.x0 + w.x1) / 2, w.y1)
        assertEquals("body", assertIs<SurveyPrepTouch.Hit.Mark>(hit(topMid[0], topMid[1] + 6)).part, "inside, just under the top side: the body, not a `t` tab")
        // THE EMPTY CORNER OF ITS BOUNDING BOX is nobody's: under the sloping bottom, inside the box (x 900, y 1000 mm)
        val hole = toPx(900.0, 1000.0)
        assertNull(hit(hole[0], hole[1])); assertNull(hit(hole[0], hole[1], sel = null))
        // not held (the walk reached it): no corner takes a press -- inside the shape is its body, as any mark's
        val c2 = toPx(2970.0, 2450.0)
        assertEquals("body", assertIs<SurveyPrepTouch.Hit.Mark>(hit(c2[0] - 4, c2[1] + 4, sel = null)).part)
        // THE CHIP keeps the bounding box and stands 16 dp clear of the four corners AS THEY STAND -- not of eight tabs
        val a = toPx(w.x0, w.y1); val b = toPx(w.x1, w.y0); val bb = Box(a[0], a[1], b[0], b[1])
        val corners = Quad.freeCorners(w, true)!!.map { toPx(it[0], it[1]) }
        val handles = SurveyPrepTouch.handlesOf(bb, false, corners)
        assertEquals(corners.map { it.toList() }, handles.map { it.toList() })
        val chip = assertNotNull(SurveyPrepTouch.codeChip(bb, handles, 60.0, CW, CH, DP, SurveyPrepTouch.chipLift(true, 24.0, U, DP)))
        for (h in handles) assertTrue(SurveyPrepTouch.boxDist(chip.tap, h[0], h[1]) >= SurveyPrepTouch.CHIP_CLEAR_DP * DP - 1e-9)
        // (the window's top is 47 px under the photo's: no room above it, so the chip stands below its bottom-left corner)
        assertEquals("below", chip.how)
        // a spot's handle is its middle whatever corners it is handed; a rectangle's are its eight tabs
        assertEquals(8, SurveyPrepTouch.handlesOf(bb, false, Quad.freeCorners(mark(p, D1), free(mark(p, D1)))?.map { toPx(it[0], it[1]) }).size)
    }

    // ------------------------------------------------------------------ 3. the gold frame and the dim hole round the SHAPE

    private fun lineDist(p: DoubleArray, a: DoubleArray, b: DoubleArray): Double =
        abs((b[0] - a[0]) * (a[1] - p[1]) - (a[0] - p[0]) * (b[1] - a[1])) / hypot(b[0] - a[0], b[1] - a[1])

    @Test fun `the outline grown - a rectangle's is its box grown, a shape's every side moved out square to itself, a sharp corner cut back`() {
        // a rectangle, either winding, on a canvas whose y runs down: exactly the box grown by 3 each side
        val box = listOf(pt(100.0, 400.0), pt(300.0, 400.0), pt(300.0, 150.0), pt(100.0, 150.0))
        val want = listOf(listOf(97.0, 403.0), listOf(303.0, 403.0), listOf(303.0, 147.0), listOf(97.0, 147.0))
        Quad.grown(box, 3.0).forEachIndexed { i, g -> near(want[i][0], g[0], "x$i"); near(want[i][1], g[1], "y$i") }
        Quad.grown(box.reversed(), 3.0).forEachIndexed { i, g -> near(want.reversed()[i][0], g[0], "rx$i"); near(want.reversed()[i][1], g[1], "ry$i") }
        // so the frame and the hole of a mark with NO quad are what they always were: the box, grown
        val d = SurveyPrep.Mark(D1, "D", "FD1", 3050.0, 3450.0, 0.0, 2100.0)
        val a = toPx(d.x0, d.y1); val b = toPx(d.x1, d.y0); val gap = MarkDims.ACTIVE_GAP_DP * DP
        val g = Quad.grown(Quad.corners(d).map { toPx(it[0], it[1]) }, gap)
        near(a[0] - gap, g.minOf { it[0] }, "l"); near(b[0] + gap, g.maxOf { it[0] }, "r"); near(a[1] - gap, g.minOf { it[1] }, "t"); near(b[1] + gap, g.maxOf { it[1] }, "b")
        // THE EXAMPLE: every grown side is parallel to its side and exactly `by` outside it, and the shape lies inside
        val w = window(); val px = Quad.corners(w).map { toPx(it[0], it[1]) }
        for (by in listOf(6.0, 8.5)) {
            val out = Quad.grown(px, by)
            assertEquals(4, out.size)
            for (i in 0 until 4) {
                val p0 = px[i]; val p1 = px[(i + 1) % 4]
                near(by, lineDist(out[i], p0, p1), "side $i, its first corner, by $by"); near(by, lineDist(out[(i + 1) % 4], p0, p1), "side $i, its second corner, by $by")
            }
            for (c in px) assertTrue(Pano360.inPoly(c[0], c[1], out), "the shape is inside its frame")
            for (c in out) assertFalse(Pano360.inPoly(c[0], c[1], px), "the frame is outside the shape")
        }
        // by 0, and too few points: as given (copies)
        assertEquals(px.map { it.toList() }, Quad.grown(px, 0.0).map { it.toList() }); assertEquals(2, Quad.grown(px.take(2), 5.0).size)
        // A CORNER A FEW DEGREES WIDE: its moved sides would meet far away -- cut back to MITER x by, never across the photo
        val spike = listOf(pt(0.0, 0.0), pt(400.0, 0.0), pt(400.0, 10.0))
        val s = Quad.grown(spike, 5.0)
        assertTrue(hypot(s[0][0] - 0.0, s[0][1] - 0.0) <= Quad.MITER * 5.0 + 1e-9, "the sharp corner's frame stays within ${Quad.MITER} x 5 of it")
        assertEquals(4.0, Quad.MITER)
        assertTrue(s.all { it[0].isFinite() && it[1].isFinite() })
    }

    // ------------------------------------------------------------------ 4. its figures, with no photo estimate

    private fun layout(p: SurveyPrep.Prep, pick: State, freeId: String? = null, part: String? = NudgePad.BODY, ask: Boolean = true): Pair<List<DimLayout.Mark>, DimLayout.Out> {
        val ms = PhotoFigures.layout(en, p, "front", A, B, pick, ::toPx, U, DP, textW, free = Quad.freeOf(freeId, pick.picked, part), ask = ask)
        return ms to DimLayout.placeShown(ms, CW, CH, emptyList(), U, DP)
    }
    private fun segDist(p: DoubleArray, a: DoubleArray, b: DoubleArray): Double {
        val dx = b[0] - a[0]; val dy = b[1] - a[1]; val t = (((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / (dx * dx + dy * dy)).coerceIn(0.0, 1.0)
        return hypot(p[0] - a[0] - t * dx, p[1] - a[1] - t * dy)
    }

    @Test fun `a four-corner mark's figures - a read side its number alone along its side, an unread one nothing unless picked, never an estimate`() {
        val p = wall(); val w = mark(p, W1)
        assertEquals(ASKED, p.keysOf("front", w, A, B))
        val shape = Quad.corners(w).map { toPx(it[0], it[1]) }
        // NOT PICKED, NOTHING READ: outlines and codes -- no plate, no dot, no tap box
        layout(p, Pick.NONE).second.let { out -> assertTrue(out.plates.isEmpty()); assertTrue(out.tags.isEmpty()) }
        // PICKED, NOTHING READ: every dimension it asks as the quiet `?`, each a tap target; nothing says a number
        layout(p, State(W1, false, null)).second.let { out ->
            assertEquals(ASKED, out.plates.map { it.code })
            assertEquals(listOf("?", "?", "?", "?", "?", "?", "⊥ ?"), out.plates.map { it.fig.text })
            assertTrue(out.plates.all { it.fig.state == MarkDims.State.UNKNOWN && !it.fig.active })
            assertEquals(ASKED.toSet(), out.tags.map { it.code }.toSet())
        }
        // THE ONE BEING MEASURED is gold: `▶?` on its own side, with a line for the gold line to follow
        layout(p, State(W1, false, "FW1.bottom")).second.let { out ->
            val g = assertNotNull(out.active); assertEquals("FW1.bottom", g.code); assertEquals("▶?", g.fig.text); assertEquals(MarkDims.Role.SIDE, g.role); assertNotNull(g.line)
            assertEquals(MarkDims.ACTIVE_COLOUR, MarkDims.plate(g.fig.state, g.fig.active).fill)
        }
        // not picked, with the gold on it (the walk reached it): the gold one alone
        assertEquals(listOf("FW1.top"), layout(p, State(null, false, "FW1.top")).second.plates.map { it.code })
        // READ SIDES SHOW ALWAYS, THE NUMBER ALONE -- no code, no unit, no `≈` -- and an unread side shows NOTHING
        p.actual["FW1.bottom"] = 2495.0; p.actual["FW1.right"] = 1550.0; p.actual["FW1.s"] = 900.0
        val (_, out) = layout(p, Pick.NONE)
        assertEquals(listOf("FW1.bottom" to "2495", "FW1.right" to "1550", "FW1.s" to "900"), out.plates.map { it.code to it.fig.text })
        assertTrue(out.plates.all { it.fig.state == MarkDims.State.MEASURED })
        // ALONG ITS SIDE: the sloping bottom's plate is turned to the slope, the upright right side's reads bottom to top
        val bs = MarkDims.of("front", w, A, B).first { it.key == "bottom" }
        val b0 = toPx(bs.x1, bs.y1); val b1 = toPx(bs.x2, bs.y2)
        out.of("FW1.bottom")!!.let { near(LabelAngle.of(b0[0], b0[1], b1[0], b1[1]), it.angle, "the slope"); assertTrue(it.angle != LabelAngle.LEVEL && it.angle != LabelAngle.UP) }
        assertEquals(LabelAngle.UP, out.of("FW1.right")!!.angle)
        // EVERYTHING READ: every figure, picked or not; each SIDE'S PLATE STANDS ON ITS OWN SIDE, outside the shape
        for ((k, v) in mapOf("FW1.top" to 2370.0, "FW1.left" to 780.0, "FW1.l" to 600.0, "FW1.p" to 150.0)) p.actual[k] = v
        for (pick in listOf(Pick.NONE, State(W1, false, null), State(W1, true, null))) {
            val all = layout(p, pick).second
            assertEquals(ASKED, all.plates.map { it.code }, "$pick")
            assertEquals(listOf("2495", "1550", "2370", "780", "600", "900", "⊥ 150"), all.plates.map { it.fig.text }, "$pick")
            SIDES.forEachIndexed { i, code ->
                val pl = all.of(code)!!
                assertFalse(pl.dot, "$code is a plate at the phone's own numbers")
                val c = pt((pl.box.l + pl.box.r) / 2, (pl.box.t + pl.box.b) / 2)
                assertFalse(Pano360.inPoly(c[0], c[1], shape), "$code stands outside the shape ($pick)")
                val own = segDist(c, shape[i], shape[(i + 1) % 4])
                for (j in 0 until 4) if (j != i) assertTrue(own < segDist(c, shape[j], shape[(j + 1) % 4]), "$code is nearest its own side ($pick)")
            }
        }
        // NO FIGURE OF IT, IN ANY STATE, IS AN ESTIMATE -- read, unread, picked, held, gold, typing, after Survey
        val q = wall()
        q.actual["FW1.top"] = 2370.0
        for (pick in listOf(Pick.NONE, State(W1, false, null), State(W1, true, null), State(W1, true, "FW1.left"), State(W1, false, "FW1.top"))) for (ask in listOf(true, false)) {
            for (pl in layout(q, pick, ask = ask).second.plates) {
                assertFalse("≈" in pl.fig.text, "${pl.code} $pick"); assertFalse(pl.fig.state == MarkDims.State.ESTIMATE, "${pl.code} $pick")
                if (!pl.fig.measured && !pl.fig.typing) assertFalse(pl.fig.text.any { it.isDigit() }, "${pl.code} $pick: an unread figure shows no number")
            }
        }
        // after Survey nothing can be read: the picked mark shows its READ figures only, and no `?`
        assertEquals(listOf("FW1.top"), layout(q, State(W1, false, null), ask = false).second.plates.map { it.code })
        // A WISHED FOUR-CORNER UNIT is not in the room: no figure at all, picked or not (§18 rule 6)
        mark(q, W1).extra = mark(q, W1).extra + mapOf(MarkLife.KEY_BORN to MarkLife.WISH, MarkLife.KEY_LIFE to MarkLife.WISH)
        assertTrue(layout(q, State(W1, true, null)).second.plates.isEmpty())
    }

    @Test fun `the diagonal lies inside the shape, and the held mark's four corners are kept clear by every plate`() {
        // a shape with no square corner asks its diagonal, q0 to q2
        val p = SurveyPrep.fresh(L, A, B)
        val x = SurveyPrep.Mark("x1", "O", "FO1", 1000.0, 2600.0, 600.0, 2000.0).also { p.surface("front").marks.add(it) }
        assertTrue(Quad.set(x, listOf(pt(1000.0, 800.0), pt(2400.0, 600.0), pt(2600.0, 1800.0), pt(1300.0, 2000.0))))
        val keys = p.keysOf("front", x, A, B)
        assertEquals(listOf("FO1.bottom", "FO1.right", "FO1.top", "FO1.left", "FO1.diag"), keys.take(5))
        for (k in keys) p.actual[k] = 1234.0
        val shape = Quad.corners(x).map { toPx(it[0], it[1]) }
        val out = layout(p, State("x1", false, null)).second
        val dg = out.of("FO1.diag")!!
        assertEquals(MarkDims.Role.DIAG, dg.role); assertFalse(dg.dot)
        val ln = assertNotNull(dg.line)
        // its line IS q0 - q2, unshifted, and its plate sits on it, inside the shape, turned along it
        near(shape[0][0], ln.x1, "x1"); near(shape[0][1], ln.y1, "y1"); near(shape[2][0], ln.x2, "x2"); near(shape[2][1], ln.y2, "y2")
        val c = pt((dg.box.l + dg.box.r) / 2, (dg.box.t + dg.box.b) / 2)
        assertTrue(Pano360.inPoly(c[0], c[1], shape), "the diagonal's figure is inside the shape"); near(LabelAngle.of(ln.x1, ln.y1, ln.x2, ln.y2), dg.angle, "along the diagonal")
        // HELD, ITS CORNERS FREE: the layout is handed its four corners, not eight tabs, and no plate lies on a corner
        val (ms, held) = layout(p, State("x1", true, null))
        val m = ms.single(); assertFalse(m.tabs); assertEquals(shape.map { it.toList() }, m.corners!!.map { it.toList() })
        val keep = DimLayout.cornerBoxes(m.corners!!, U, DP)
        assertEquals(4, keep.size); assertTrue(keep.all { it.w >= 2 * SurveyGuide.END_R_DP * DP - 1e-9 })
        for (pl in held.plates) for (k in keep) assertFalse(pl.rect.hits(DimLayout.Turned.of(k), 0.0), "${pl.code} lies on a corner handle")
        // the same mark picked but NOT held shows no handle, and nothing is kept clear for one
        layout(p, State("x1", false, null)).first.single().let { assertFalse(it.tabs); assertNull(it.corners) }
        // A RECTANGLE WHOSE CORNERS WERE FREED and not yet dragged: four corners kept clear, its w and h on their lines
        val q = wallWithDoor(); q.actual["FD1.w"] = 400.0; q.actual["FD1.h"] = 2100.0
        val (fm, fo) = layout(q, State(D1, true, null), freeId = D1)
        fm.first { it.id == D1 }.let { assertFalse(it.tabs); assertEquals(4, it.corners!!.size) }
        assertEquals(listOf("400", "2100"), listOf(fo.of("FD1.w")!!.fig.text, fo.of("FD1.h")!!.fig.text))
        layout(q, State(D1, true, null)).first.first { it.id == D1 }.let { assertTrue(it.tabs); assertNull(it.corners) }
    }

    // ------------------------------------------------------------------ 5. the walk inside the mark

    @Test fun `the gold walks inside a four-corner mark - bottom, right, top, left, the diagonal if asked, then its type's own`() {
        val p = wall()
        // picking it makes its first unread figure gold: the bottom side
        var st = Pick.body(sc(p), Pick.NONE, W1).state
        assertEquals(State(W1, true, "FW1.bottom"), st)
        // a reading lands on each in turn (the laser's): the gold moves on by itself, in the checklist's order
        val read = listOf(2492.0, 1550.0, 2370.0, 780.0, 600.0, 900.0, 150.0)
        val walked = ArrayList<String>()
        for (v in read) {
            val g = assertNotNull(st.gold); walked += g
            assertEquals(g, assertIs<Landing.Route.Gold>(Landing.route(null, sc(p), st, v.toInt(), 0L)).code)
            p.actual[g] = v
            val a = Landing.after(sc(p), st, g, busy = false)
            assertEquals(Haptic.TICK, a.haptic, g); st = a.state
            assertFalse(st.held, "a landed reading drops the hold: a mark the walk reached cannot be dragged")
        }
        assertEquals(ASKED, walked); assertNull(st.gold, "nothing left on the wall")
        assertTrue(MarkDims.done(p, "front", mark(p, W1), A, B))
        // `▸` walks the same order without a reading, wrapping, and skips what is read
        val q = wall(); q.actual["FW1.right"] = 1550.0
        var s2 = State(W1, false, "FW1.bottom"); val seen = ArrayList<String>()
        repeat(6) { s2 = Pick.next(sc(q), s2, "front").state; seen += s2.gold!! }
        assertEquals(listOf("FW1.top", "FW1.left", "FW1.l", "FW1.s", "FW1.p", "FW1.bottom"), seen)
        // a shape with no square corner: its diagonal after its four sides
        val r = SurveyPrep.fresh(L, A, B)
        val x = SurveyPrep.Mark("x1", "O", "FO1", 1000.0, 2600.0, 600.0, 2000.0).also { r.surface("front").marks.add(it) }
        assertTrue(Quad.set(x, listOf(pt(1000.0, 800.0), pt(2400.0, 600.0), pt(2600.0, 1800.0), pt(1300.0, 2000.0))))
        var s3 = Pick.body(sc(r), Pick.NONE, "x1").state; val order = ArrayList<String>()
        while (s3.gold != null && order.size < 12) { val g = s3.gold!!; order += g; r.actual[g] = 1000.0; s3 = Landing.after(sc(r), s3, g, busy = false).state }
        assertEquals(listOf("FO1.bottom", "FO1.right", "FO1.top", "FO1.left", "FO1.diag"), order.take(5)); assertEquals(r.keysOf("front", x, A, B), order)
        // after Survey nothing turns gold on it
        assertNull(Pick.body(sc(p, 1), Pick.NONE, W1).state.gold)
    }

    // ------------------------------------------------------------------ 6. the gross guard on a side

    @Test fun `the gross guard reads a four-corner mark's sides and diagonal - more than twice or less than half the side as drawn, never a position`() {
        val p = wall(); val w = mark(p, W1)
        val dims = MarkDims.of("front", w, A, B).associateBy { it.code }
        fun flagged(code: String, v: Double): Boolean { p.actual.clear(); p.actual[code] = v; return MeasureFlow.flagged(p, code, ::ab) }
        // the sloping bottom is drawn 2491.9 mm, corner to corner: that is what its reading is compared with
        near(2491.94, dims.getValue("FW1.bottom").est!!, "the bottom as drawn", 0.01)
        assertTrue(flagged("FW1.bottom", 4984.0)); assertFalse(flagged("FW1.bottom", 4983.0)); assertTrue(flagged("FW1.bottom", 1245.0)); assertFalse(flagged("FW1.bottom", 1246.0))
        // the upright right side is drawn 1550 and the left 780 (to the hundredth of a mm its corners are stored at)
        assertFalse(flagged("FW1.right", 3099.0)); assertTrue(flagged("FW1.right", 3101.0)); assertFalse(flagged("FW1.left", 391.0)); assertTrue(flagged("FW1.left", 389.0))
        // an ordinary reading, tens of millimetres from the drawing, says nothing: the 150 mm / 15 % rule is gone
        for ((k, v) in mapOf("FW1.bottom" to 2800.0, "FW1.right" to 1300.0, "FW1.top" to 2000.0, "FW1.left" to 1100.0)) assertFalse(flagged(k, v), k)
        // WHERE IT SITS is never guarded: a distance to a wall is not the mark's size
        assertFalse(flagged("FW1.l", 5.0)); assertFalse(flagged("FW1.s", 9000.0)); assertFalse(flagged("FW1.p", 9000.0))
        // its `!` is the plate's: the figure carries the check, in the reading's own state
        p.actual.clear(); p.actual["FW1.right"] = 4000.0
        PhotoFigures.ofMark(en, p.actual, "front", w, A, B, false, null).single().let { assertTrue(it.check); assertEquals("4000", it.text); assertEquals(PhotoFigures.Look.READ, it.look) }
        assertEquals("This reading does not match the mark. Shoot again, or ▸ to keep it.", MeasureFlow.guardText(en, p, "FW1.right", ::ab))
        // THE DIAGONAL is its size too
        val q = SurveyPrep.fresh(L, A, B)
        val x = SurveyPrep.Mark("x1", "O", "FO1", 1000.0, 2600.0, 600.0, 2000.0).also { q.surface("front").marks.add(it) }
        assertTrue(Quad.set(x, listOf(pt(1000.0, 800.0), pt(2400.0, 600.0), pt(2600.0, 1800.0), pt(1300.0, 2000.0))))
        val d = MarkDims.of("front", x, A, B).first { it.key == Quad.DIAG }
        assertTrue(MarkDims.guarded(d.role)); near(hypot(1600.0, 1000.0), d.est!!, "q0 to q2", 0.01)
        q.actual["FO1.diag"] = d.est!! * 2.01; assertTrue(MeasureFlow.flagged(q, "FO1.diag", ::ab))
        q.actual["FO1.diag"] = d.est!! * 1.99; assertFalse(MeasureFlow.flagged(q, "FO1.diag", ::ab))
        // A UNIT THAT STANDS OFF THE WALL is drawn larger than it is: its sides are not guarded -- but its four readings
        // are still held against each other (the closing check compares readings with readings)
        val r = SurveyPrep.fresh(L, A, B)
        val u = SurveyPrep.Mark("u1", "CRP", "FCRP1", 600.0, 2970.0, 900.0, 2450.0).also { r.surface("front").marks.add(it) }
        assertTrue(Quad.set(u, listOf(pt(600.0, 1670.0), pt(2970.0, 900.0), pt(2970.0, 2450.0), pt(600.0, 2450.0))))
        assertFalse(MarkDims.inPlane("front", u, A, B))
        r.actual["FCRP1.right"] = 400.0; assertFalse(MeasureFlow.flagged(r, "FCRP1.right", ::ab))
        r.actual.putAll(mapOf("FCRP1.bottom" to 1700.0, "FCRP1.right" to 1000.0, "FCRP1.top" to 1500.0, "FCRP1.left" to 500.0))
        assertTrue(Quad.closing(r.actual, u)!!.fails); assertTrue(MeasureFlow.flagged(r, "FCRP1.bottom", ::ab)); assertFalse(MeasureFlow.flagged(r, "FCRP1.left", ::ab))
    }

    // ------------------------------------------------------------------ 7. Fine move on a corner

    private val PW = 348 * DP; private val PH = 514 * DP
    private inner class Run(val p: SurveyPrep.Prep, var st: NudgePad.State) {
        var steps = 0; var said: String? = null
        fun press(k: Key): Boolean { val r = NudgePad.press(p, st, k, A, B) { steps++ }; st = r.state ?: st; said = r.said?.invoke(en); return r.moved }
    }

    @Test fun `Fine move on a corner - the corner last touched, re-aimed by a tap, drawn solid, and a refused nudge takes no undo step and says one line`() {
        val p = wallWithDoor(); val w = mark(p, W1)
        // the part last touched is the corner he pressed: Fine move starts on it
        val st = assertNotNull(NudgePad.start(p, W1, SURVEY, "q2", null, PW, PH, DP, free = true)); assertEquals("q2", st.part)
        // ... and a TAB pressed before `Four corners` freed the mark is the whole mark: a free mark shows no tab to move
        for (tab in SurveyPrepTouch.TABS) {
            assertEquals(NudgePad.BODY, NudgePad.start(p, D1, SURVEY, tab, null, PW, PH, DP, free = true)!!.part, tab)
            assertEquals(tab, NudgePad.start(p, D1, SURVEY, tab, null, PW, PH, DP)!!.part, "$tab: not free, the tab it is")
        }
        for (q in Quad.PARTS) for (free in listOf(false, true)) assertEquals(q, NudgePad.start(p, D1, SURVEY, q, null, PW, PH, DP, free = free)!!.part, q)
        // the handle drawn SOLID is that corner's alone; with the whole mark as the part, all four
        for (q in Quad.PARTS) assertEquals(q == "q2", NudgePad.solid(st, W1, q), q)
        NudgePad.aim(p, st, NudgePad.BODY).let { whole -> for (q in Quad.PARTS) assertTrue(NudgePad.solid(whole, W1, q), q) }
        for (q in Quad.PARTS) assertFalse(NudgePad.solid(st, D1, q), "another mark's corners are never solid")
        // a tap on another corner while the pad is up re-aims it: four corners are four taps
        val run = Run(p, st)
        assertTrue(run.press(Key.LEFT)); assertEquals(1, run.steps); assertNull(run.said)
        near(2969.0, Quad.corners(w)[2][0], "q2 one mm left", 0.01)
        run.st = NudgePad.aim(p, run.st, "q0"); assertEquals("q0", run.st.part); assertTrue(run.st.stepTaken)
        assertTrue(run.press(Key.UP)); near(1671.0, Quad.corners(w)[0][1], "q0 one mm up", 0.01); assertEquals(1, run.steps, "one undo step for the whole Fine move")
        // every arrow answers on a corner
        for (k in listOf(Key.UP, Key.DOWN, Key.LEFT, Key.RIGHT)) assertTrue(NudgePad.live(run.st, k), k.name)

        // A REFUSED NUDGE: a 20 mm wide switch plate's bottom-left corner cannot go right -- the bottom side would run
        // under 20 mm. Nothing moves, NO undo step, and the line says why, naming the mark
        val plate = SurveyPrep.Mark("s1", "S", "FS1", 2000.0, 2000.0 + SurveyPrep.MIN_SIZE, 1200.0, 1290.0).also { p.surface("front").marks.add(it) }
        val sr = Run(p, NudgePad.start(p, "s1", SURVEY, "q0", null, PW, PH, DP, free = true)!!)
        val before = sr.st
        assertFalse(sr.press(Key.RIGHT)); assertEquals(0, sr.steps); assertEquals(before, sr.st, "the Fine move is as it was: no step taken")
        assertEquals(listOf(2000.0, 2020.0, 1200.0, 1290.0), boxOf(plate)); assertNull(Quad.of(plate))
        assertEquals("FS1: the corner stops here. No side goes under 20 mm, and the shape cannot fold.", sr.said)
        assertEquals(sr.said, NudgePad.cornerStops("FS1")(en))
        // ... and its top-left corner cannot go right either, by 5: the top side would run 15 mm. It stops and says so
        val fold = Run(p, NudgePad.toggle(NudgePad.start(p, "s1", SURVEY, "q3", null, PW, PH, DP)!!))
        assertFalse(fold.press(Key.RIGHT)); assertNotNull(fold.said); assertEquals(0, fold.steps)
        // the nudge that DOES move says nothing, and takes the one step
        assertTrue(sr.press(Key.LEFT)); assertNull(sr.said); assertEquals(1, sr.steps); assertNotNull(Quad.of(plate))
        // A CORNER HARD AGAINST THE FACE'S EDGE is not refused: it moves nothing and says nothing, as any part there
        val low = SurveyPrep.Mark("d3", "D", "FD3", 0.0, 400.0, 0.0, 2100.0).also { p.surface("front").marks.add(it) }
        val er = Run(p, NudgePad.start(p, "d3", SURVEY, "q0", null, PW, PH, DP)!!)
        assertFalse(er.press(Key.LEFT)); assertFalse(er.press(Key.DOWN)); assertNull(er.said); assertEquals(0, er.steps); assertNull(Quad.of(low))
        // AN EDGE AT ITS FLOOR says nothing either: the line is a corner's
        val tr = Run(p, NudgePad.start(p, "s1", SURVEY, "r", null, PW, PH, DP)!!)
        Quad.clear(plate); plate.x0 = 2000.0; plate.x1 = 2020.0
        assertFalse(tr.press(Key.LEFT)); assertNull(tr.said)
        // the text is keyed, English first
        assertEquals(NudgePad.cornerStops("FS1")(Tx.EN), NudgePad.cornerStops("FS1")(Tx.MR))
    }

    // ------------------------------------------------------------------ 8. the panel's two shape acts, as one step

    @Test fun `Four corners changes nothing in the record, frees the mark and says what to do - Rectangle is one undo step back to the box`() {
        val p = wallWithDoor(); val door = mark(p, D1); val w = mark(p, W1)
        fun file() = SurveyPrepFile.encode(p, L, A, B).toString()
        // FOUR CORNERS on the door: the record is byte for byte what it was, and NO undo step
        val was = file(); var steps = 0
        val f = MarkSheet.shapeStep(en, p, D1, SurveyPrepEdit.SHAPE_FREE, SURVEY, null, "r") { steps++ }
        assertEquals(was, file()); assertEquals(0, steps); assertNull(Quad.of(door))
        assertEquals(D1, f.freeId); assertEquals(NudgePad.BODY, f.part, "a tab is no part of a free mark"); assertTrue(f.hold); assertFalse(f.changed)
        assertEquals("Drag a corner. A side near upright or level snaps straight.", f.said)
        assertTrue(Quad.free(door, f.freeId, null)); assertEquals(D1, Quad.freed(f.freeId, D1, f.part))
        // picked again while already free: the same, and still nothing written
        MarkSheet.shapeStep(en, p, D1, SurveyPrepEdit.SHAPE_FREE, SURVEY, D1, "q1") { steps++ }.let {
            assertEquals(D1, it.freeId); assertEquals(NudgePad.BODY, it.part); assertEquals(f.said, it.said); assertFalse(it.changed) }
        assertEquals(was, file()); assertEquals(0, steps)

        // RECTANGLE on the window: ONE undo step, taken BEFORE the key goes; the box does not move; the selection is the
        // whole mark again and nothing is free, so it shows eight tabs
        val box = boxOf(w); var seen: Any? = "never"
        val r = MarkSheet.shapeStep(en, p, W1, SurveyPrepEdit.SHAPE_BOX, SURVEY, W1, "q2") { steps++; seen = w.extra[Quad.KEY] }
        assertEquals(1, steps); assertNotNull(seen, "the undo step holds the shape"); assertTrue(seen != "never")
        assertNull(Quad.of(w)); assertFalse(Quad.KEY in w.extra); assertEquals(box, boxOf(w))
        assertNull(r.freeId); assertEquals(NudgePad.BODY, r.part); assertNull(r.said); assertTrue(r.hold); assertTrue(r.changed)
        assertFalse(Quad.free(w, Quad.freed(r.freeId, W1, r.part), r.part)); assertEquals(Quad.Handles.TABS, Quad.handles(w, true, Quad.freeOf(r.freeId, W1, r.part)(w)))
        // (had the part stayed q2, the corner would have freed it again: that is why the step answers the part)
        assertEquals(W1, Quad.freed(null, W1, "q2"))
        // its readings stay, also those it stops asking for (DESIGN-quad section 2.6)
        p.actual["FD1.w"] = 400.0
        assertTrue(Quad.set(door, listOf(pt(3050.0, 100.0), pt(3450.0, 0.0), pt(3450.0, 2100.0), pt(3050.0, 2100.0)))); p.actual["FD1.left"] = 2000.0
        MarkSheet.shapeStep(en, p, D1, SurveyPrepEdit.SHAPE_BOX, SURVEY, D1, NudgePad.BODY) { steps++ }
        assertEquals(2, steps); assertEquals(400.0, p.actual["FD1.w"]); assertEquals(2000.0, p.actual["FD1.left"])
        // RECTANGLE WITH NO KEY (its corners were freed and never dragged): nothing to undo, nothing changed; freeId goes
        MarkSheet.shapeStep(en, p, D1, SurveyPrepEdit.SHAPE_BOX, SURVEY, D1, "q0") { steps++ }.let {
            assertEquals(2, steps); assertFalse(it.changed); assertNull(it.freeId); assertEquals(NudgePad.BODY, it.part); assertTrue(it.hold) }

        // A LOCKED MARK: nothing happens, in the lock's own words -- for both acts
        assertEquals(true, SurveyPrepEdit.setLocked(p, D1, true))
        for (act in listOf(SurveyPrepEdit.SHAPE_FREE, SurveyPrepEdit.SHAPE_BOX)) MarkSheet.shapeStep(en, p, D1, act, SURVEY, null, "q1") { steps++ }.let {
            assertEquals(SurveyPrepEdit.refusal(en, "FD1"), it.said); assertNull(it.freeId); assertEquals("q1", it.part); assertFalse(it.hold); assertFalse(it.changed) }
        // WHERE THE PHASE HOLDS THE MARK READ-ONLY (a surveyed mark after Survey): nothing, in the phase's own words
        assertTrue(Quad.set(w, listOf(pt(600.0, 1670.0), pt(2970.0, 900.0), pt(2970.0, 2450.0), pt(600.0, 2450.0))))
        for (act in listOf(SurveyPrepEdit.SHAPE_FREE, SurveyPrepEdit.SHAPE_BOX)) MarkSheet.shapeStep(en, p, W1, act, 1, W1, NudgePad.BODY) { steps++ }.let {
            assertEquals(PhaseTools.refuseText(en, w, 1), it.said); assertEquals(W1, it.freeId); assertFalse(it.hold); assertFalse(it.changed) }
        assertNotNull(Quad.of(w)); assertEquals(2, steps)
        // a point, a Note, a Line, no such id, another act: nothing at all, and nothing said
        val e1 = SurveyPrep.Mark("e1", "E", "FE1", 300.0, 350.0, 2300.0, 2350.0).also { p.surface("front").marks.add(it) }
        p.addMeas("front", SurveyMeas.LINE, listOf(pt(100.0, 500.0), pt(900.0, 500.0)), "m1")
        for ((id, act) in listOf(e1.id to SurveyPrepEdit.SHAPE_FREE, "m1" to SurveyPrepEdit.SHAPE_FREE, "nobody" to SurveyPrepEdit.SHAPE_BOX, W1 to SurveyPrepEdit.DELETE, W1 to MarkSheet.FINE))
            MarkSheet.shapeStep(en, p, id, act, SURVEY, "keep", "q3") { steps++ }.let { assertEquals("keep", it.freeId); assertEquals("q3", it.part); assertNull(it.said); assertFalse(it.hold); assertFalse(it.changed) }
        assertEquals(2, steps)
    }

    @Test fun `the panel draws the shape row only where it is asked for, and its act is Done followed by the step`() {
        val p = wallWithDoor()
        // OFF by default in this build's pure rules: the screen turns the switch on, in the commit that runs the acts
        assertFalse(MarkSheet.acts(en, p, D1, SURVEY, null).has(SurveyPrepEdit.SHAPE_FREE)); assertFalse(MarkSheet.acts(en, p, W1, SURVEY, null).has(SurveyPrepEdit.SHAPE_BOX))
        assertEquals("Four corners", MarkSheet.acts(en, p, D1, SURVEY, null, shape = true).line2.first().text)
        assertEquals("Rectangle", MarkSheet.acts(en, p, W1, SURVEY, null, shape = true).line2.first().text)
        // hidden where the phase holds the mark read-only: a surveyed mark after Survey has no act at all
        for (st in 1..5) assertFalse(MarkSheet.acts(en, p, W1, st, null, shape = true).has(SurveyPrepEdit.SHAPE_BOX), "$st")
        // the act: asked first (actStep), then Done, then the step -- and Done's undo step and Rectangle's are ONE
        val pn = MarkSheet.openPanel(p, W1, -1000L)!!
        val step = assertIs<MarkSheet.ActStep.Run>(MarkSheet.actStep(en, p, pn, SurveyPrepEdit.SHAPE_BOX, SURVEY, null, shape = true)); assertEquals(SurveyPrepEdit.SHAPE_BOX, step.act)
        val hist = ArrayList<String>()
        val typed = pn.with { MarkSheet.typeNote(it, "sloping sill") }
        val before = hist.size
        val done = MarkSheet.panelDone(p, typed, SURVEY, java.time.Instant.parse("2026-11-23T06:00:00Z")) { hist += "done" }
        val mid = hist.size
        assertTrue(MarkSheet.actRuns(done.outcome))
        val sh = MarkSheet.shapeStep(en, p, W1, step.act, SURVEY, null, NudgePad.BODY) { hist += "shape" }
        assertTrue(sh.changed); assertEquals(listOf("done", "shape"), hist)
        assertEquals(1, MarkSheet.stepsToDrop(before, mid, hist.size), "the act's own step is taken off: one undo returns to the room before either")
        // Four corners after something was typed: Done's step stands alone, the act took none
        val q = wallWithDoor(); val qn = MarkSheet.openPanel(q, D1, -1000L)!!.with { MarkSheet.typeNote(it, "check the head") }
        val h2 = ArrayList<String>()
        MarkSheet.panelDone(q, qn, SURVEY, java.time.Instant.parse("2026-11-23T06:00:00Z")) { h2 += "done" }
        MarkSheet.shapeStep(en, q, D1, SurveyPrepEdit.SHAPE_FREE, SURVEY, null, NudgePad.BODY) { h2 += "shape" }
        assertEquals(listOf("done"), h2); assertEquals(0, MarkSheet.stepsToDrop(0, 1, h2.size))
    }

    // ------------------------------------------------------------------ 9. the 360's one free

    @Test fun `the 360 asks the same free - a free mark's four corners are its tabs there, any other mark's the eight`() {
        val p = wallWithDoor(); val w = mark(p, W1); val d = mark(p, D1)
        // the door held on the 360 after `Four corners`: the screen's one freeId, the picked part
        val free = Quad.freeOf(D1, D1, NudgePad.BODY)
        assertEquals(Quad.PARTS, Pano360.tabsOf(d, free(d)).map { it.first })
        assertEquals(Quad.corners(d).map { it.toList() }, Pano360.tabsOf(d, free(d)).map { it.second.toList() })
        // the window carries a quad: free whatever is picked
        assertEquals(Quad.PARTS, Pano360.tabsOf(w, Quad.freeOf(null, null, null)(w)).map { it.first })
        // nothing freed: the eight tabs, as every caller had them
        assertEquals(SurveyPrepTouch.TABS.toSet(), Pano360.tabsOf(d, Quad.freeOf(null, D1, NudgePad.BODY)(d)).map { it.first }.toSet())
        assertEquals(Pano360.tabsOf(d).map { it.first }, Pano360.tabsOf(d, Quad.freeOf(null, D1, "lt")(d)).map { it.first })
        // a corner dragged on the 360 goes through moveMark and snapMark, with the 25 mm floor as its snapping distance:
        // 20 mm short of level snaps straight, and the key goes
        assertTrue(SurveyPrep.moveMark(d, d.copy(), "q0", 0.0, 20.0, A, B)); assertNotNull(Quad.of(d))
        SurveyPrep.snapMark(d, "q0", A, B); assertNull(Quad.of(d), "snapped back level: its box again")
        assertTrue(Quad.freeOf(D1, D1, "q0")(d), "and still free under the finger")
    }

    // ------------------------------------------------------------------ 10. the review of Q2 (23 Nov 2026; DESIGN-quad §13)

    /** The screen's own sequence, as a model: `took` as SurveyPrepScreen runs it (the pick, the part last touched,
     *  freeId, Fine move's pad), the panel's shape act as panelAct runs it, a drag's end and a pad's press. */
    private inner class Screen(val p: SurveyPrep.Prep) {
        var pick = Pick.NONE; var part = NudgePad.BODY; var freeId: String? = null; var pad: NudgePad.State? = null
        var steps = 0; var pads = ArrayList<Pick.Pad>()
        fun took(o: Pick.Out, body: Boolean = false) {
            if (o.stay) return
            val before = pick; pads += o.pad
            pick = o.state; part = NudgePad.touched(part, before, o, body); freeId = Quad.freed(freeId, pick.picked, part)
            pad = NudgePad.afterEvent(p, pad, pick, SURVEY, o.nudge, body)
        }
        fun tapBody(id: String) = took(Pick.body(sc(p), pick, id), body = true)
        fun tapHandle(h: String) = took(Pick.handle(pick, h))
        fun chip() = took(Pick.options(pick))
        fun fineMove(id: String) {
            val last = part
            took(Pick.shaped(sc(p), pick, id))
            pad = NudgePad.start(p, id, SURVEY, last, null, PW, PH, DP, false, free = Quad.freeIn(p, id, freeId, pick.picked, last))
        }
        /** panelAct's branch for the two shape acts, line for line. */
        fun shape(id: String, act: String): MarkSheet.Shaped {
            val r = MarkSheet.shapeStep(en, p, id, act, SURVEY, freeId, part) { steps++ }
            freeId = r.freeId; part = r.part
            if (r.hold) took(Pick.shaped(sc(p), pick, id))
            pad = NudgePad.reshaped(pad, id, Quad.freeIn(p, id, freeId, pick.picked, part))
            if (r.changed) reshaped()
            return r
        }
        fun reshaped() = took(Pick.reshaped(sc(p), pick))
        /** finePress: a cell of the pad. */
        fun press(k: Key): NudgePad.Pressed {
            val r = NudgePad.press(p, pad!!, k, A, B) { steps++ }
            pad = r.state
            if (r.moved) reshaped()
            return r
        }
        fun handles(e: SurveyPrep.Mark) = Quad.handles(e, Pick.handles(pick, e.id) && !SurveyPrep.isLocked(e), Quad.freeOf(freeId, pick.picked, part)(e))
        fun route(mm: Int) = Landing.route(null, sc(p), pick, mm, 0L)
    }

    /** A Line end's snapping distance on the example wall as the Mi A3 shows it, zoomed [zoom] times: 148 mm at fit. */
    private fun tolAt(zoom: Double = 1.0) = SurveyGuide.endTol(DP, A / CW / zoom)

    @Test fun `review 1 - Rectangle or Four corners with Fine move's pad up - the pad never nudges a part the mark no longer shows`() {
        // THE RULE: a corner once the corners are not free, a tab once they are, is the whole mark; anything else stays
        val on = NudgePad.State(W1, "q0", NudgePad.COARSE_MM, NudgePad.Corner.LEFT, stepTaken = true)
        assertEquals(on.copy(part = NudgePad.BODY), NudgePad.reshaped(on, W1, false), "the step, the corner it stands in and its one undo step stay")
        for (q in Quad.PARTS) { assertEquals(NudgePad.BODY, NudgePad.reshaped(on.copy(part = q), W1, false)!!.part, q); assertEquals(q, NudgePad.reshaped(on.copy(part = q), W1, true)!!.part, q) }
        for (t in SurveyPrepTouch.TABS) { assertEquals(NudgePad.BODY, NudgePad.reshaped(on.copy(part = t), W1, true)!!.part, t); assertEquals(t, NudgePad.reshaped(on.copy(part = t), W1, false)!!.part, t) }
        for (free in listOf(false, true)) {
            assertEquals(NudgePad.BODY, NudgePad.reshaped(on.copy(part = NudgePad.BODY), W1, free)!!.part)
            // a Line's end is no tab and no corner; a pad on another item is not this act's; no pad stays none
            assertEquals(NudgePad.pointPart(1), NudgePad.reshaped(on.copy(part = NudgePad.pointPart(1)), W1, free)!!.part)
            assertEquals(on, NudgePad.reshaped(on, D1, free)); assertNull(NudgePad.reshaped(null, W1, free))
        }

        // THE FOUR-CORNER WINDOW, AS THE SCREEN RUNS IT: held, its corner q0 pressed, Fine move, then the chip and `Rectangle`
        val p = wallWithDoor(); val w = mark(p, W1); val scr = Screen(p)
        scr.tapBody(W1); scr.tapHandle("q0"); scr.fineMove(W1)
        assertEquals("q0", scr.pad!!.part); assertEquals(Quad.Handles.CORNERS, scr.handles(w))
        scr.chip(); assertEquals("q0", scr.pad!!.part, "a tap on the code chip opens the panel and leaves the pad up")
        // WITHOUT THE RULE the pad would still be aimed at q0 -- which [NudgePad.after] keeps, a corner being a part of
        // every box mark -- and its next arrow writes `quad` straight back into the mark just made a rectangle
        val stale = p.copy().let { c ->
            val e = c.markById(W1)!!.second; Quad.clear(e)
            val kept = assertNotNull(NudgePad.after(c, scr.pad, scr.pick, SURVEY)); assertEquals("q0", kept.part)
            assertTrue(NudgePad.press(c, kept, Key.UP, A, B) {}.moved); e
        }
        assertNotNull(Quad.of(stale), "the hazard: one arrow, and the rectangle is a four-corner mark again")
        // WITH IT: `Rectangle` is one undo step, the mark shows its eight tabs, and the pad is the WHOLE mark's
        val r = scr.shape(W1, SurveyPrepEdit.SHAPE_BOX)
        assertTrue(r.changed); assertEquals(1, scr.steps); assertNull(Quad.of(w)); assertNull(scr.freeId); assertEquals(Quad.Handles.TABS, scr.handles(w))
        assertEquals(NudgePad.BODY, scr.pad!!.part); assertEquals(W1, scr.pad!!.id)
        for (t in SurveyPrepTouch.TABS) assertTrue(NudgePad.solid(scr.pad, W1, t), "$t: all eight solid, the whole mark moves")
        // one arrow: the whole mark moves 1 mm, and NO `quad` is written
        val box = boxOf(w)
        assertTrue(scr.press(Key.RIGHT).moved)
        assertEquals(listOf(box[0] + 1, box[1] + 1, box[2], box[3]), boxOf(w)); assertFalse(Quad.KEY in w.extra); assertEquals(Quad.Handles.TABS, scr.handles(w))
        assertTrue(scr.press(Key.UP).moved); assertFalse(Quad.KEY in w.extra); assertEquals(listOf(box[0] + 1, box[1] + 1, box[2] + 1, box[3] + 1), boxOf(w))

        // THE REVERSE: a plain door, its left tab pressed, Fine move, then the chip and `Four corners`
        val d = mark(p, D1); val s2 = Screen(p)
        s2.tapBody(D1); s2.tapHandle("l"); s2.fineMove(D1)
        assertEquals("l", s2.pad!!.part); assertEquals(Quad.Handles.TABS, s2.handles(d))
        s2.chip()
        val f = s2.shape(D1, SurveyPrepEdit.SHAPE_FREE)
        assertFalse(f.changed); assertEquals(0, s2.steps); assertEquals(D1, s2.freeId); assertEquals(Quad.Handles.CORNERS, s2.handles(d))
        assertEquals(NudgePad.BODY, s2.pad!!.part, "no tab is on screen: the arrows move the whole mark")
        for (q in Quad.PARTS) assertTrue(NudgePad.solid(s2.pad, D1, q), q)
        for (k in listOf(Key.UP, Key.DOWN, Key.LEFT, Key.RIGHT)) assertTrue(NudgePad.live(s2.pad!!, k), "${k.name}: every arrow answers, where the left tab's took two")
        assertTrue(s2.press(Key.LEFT).moved)
        assertEquals(listOf(3049.0, 3449.0, 0.0, 2100.0), boxOf(d), "the whole door, not its left edge alone"); assertNull(Quad.of(d))
        // ... and `Four corners` again on a mark already free, the pad on a corner: the corner it is
        s2.tapHandle("q2"); assertEquals("q2", s2.pad!!.part)
        s2.chip(); s2.shape(D1, SurveyPrepEdit.SHAPE_FREE); assertEquals("q2", s2.pad!!.part)
        // with no pad up, neither act makes one
        val s3 = Screen(wallWithDoor()); s3.tapBody(D1); s3.shape(D1, SurveyPrepEdit.SHAPE_FREE); assertNull(s3.pad)
    }

    @Test fun `review 2 - the gold follows what the mark asks - after the corner drag that makes the trapezoid the bottom side is gold and the laser's reading lands on it`() {
        // A PLAIN WINDOW, tapped: held, and its first unread figure -- its width -- is gold
        val p = SurveyPrep.fresh(L, A, B)
        val w = SurveyPrep.Mark(W1, "W", "FW1", 600.0, 2970.0, 900.0, 2450.0).also { p.surface("front").marks.add(it) }
        val rect = listOf("FW1.w", "FW1.h", "FW1.l", "FW1.s", "FW1.p")
        assertEquals(rect, p.keysOf("front", w, A, B))
        val scr = Screen(p)
        scr.tapBody(W1); assertEquals(State(W1, true, "FW1.w"), scr.pick)
        // `Four corners`: nothing in the record, so nothing it asks has changed -- the gold stays on the width
        scr.shape(W1, SurveyPrepEdit.SHAPE_FREE); assertEquals(State(W1, true, "FW1.w"), scr.pick); assertEquals(rect, p.keysOf("front", w, A, B))
        // the bottom-left corner pressed and dragged 770 mm up: the left side stays upright, and the mark asks its SIDES
        scr.tapHandle("q0")
        val start = w.copy()
        assertEquals("v", assertNotNull(Quad.drag(w, start, 0, 0.0, 770.0, A, B, tolAt())).snap)
        assertEquals(ASKED, p.keysOf("front", w, A, B)); assertFalse(MeasureFlow.takes(p, "FW1.w", SURVEY, ::ab))
        // while the finger is down nothing of the pick changes (row 9) -- and that is the fault if it is left there: the
        // gold is a key the mark stopped asking, no plate is gold, and the laser's next reading is thrown away
        assertEquals(scr.pick, Pick.dragged(scr.pick).state)
        assertIs<Landing.Route.Refused>(scr.route(2495))
        assertTrue(PhotoFigures.ofMark(en, p.actual, "front", w, A, B, true, scr.pick.gold).none { it.gold }, "no figure of it is the gold one")
        // THE DRAG ENDS: the gold is asked again -- its first unread figure, the bottom side -- and the reading lands on it
        val before = scr.pick
        val o = Pick.reshaped(sc(p), scr.pick)
        assertFalse(o.stay); assertEquals(State(W1, true, "FW1.bottom"), o.state); assertTrue(Pick.turnedGold(before, o.state))
        assertEquals(Pick.Pad.SHUT_DROP, o.pad, "what was typed for the width is dropped: the mark no longer asks it")
        scr.reshaped(); assertEquals(State(W1, true, "FW1.bottom"), scr.pick); assertEquals(W1, scr.freeId); assertEquals("q0", scr.part)
        assertEquals("FW1.bottom", assertIs<Landing.Route.Gold>(scr.route(2495)).code)
        assertEquals(listOf("FW1.bottom"), PhotoFigures.ofMark(en, p.actual, "front", w, A, B, true, scr.pick.gold).filter { it.gold }.map { it.dim.code })
        // ... and from there the walk is the design's: bottom, right, top, left, then the type's own
        var st = scr.pick; val walked = ArrayList<String>()
        for (v in listOf(2492.0, 1550.0, 2370.0, 780.0, 600.0, 900.0, 150.0)) { val g = st.gold!!; walked += g; p.actual[g] = v; st = Landing.after(sc(p), st, g, busy = false).state }
        assertEquals(ASKED, walked); assertNull(st.gold)

        // THE REVERSE, BY A DRAG: the four-corner window with a SIDE gold, its corner dragged back onto its box
        val q = wall(); val w2 = mark(q, W1); val s2 = Screen(q)
        s2.tapBody(W1); assertEquals("FW1.bottom", s2.pick.gold)
        s2.took(Pick.figure(sc(q), s2.pick, "FW1.left")); assertEquals(State(W1, true, "FW1.left"), s2.pick)
        s2.tapHandle("q0")
        assertEquals("hv", assertNotNull(Quad.drag(w2, w2.copy(), 0, 0.0, -770.0, A, B, tolAt())).snap); assertNull(Quad.of(w2))
        assertEquals(rect, q.keysOf("front", w2, A, B))
        s2.reshaped(); assertEquals(State(W1, true, "FW1.w"), s2.pick); assertEquals("FW1.w", assertIs<Landing.Route.Gold>(s2.route(2370)).code)
        assertEquals(Quad.Handles.CORNERS, s2.handles(w2), "still four corners under the finger: only the gold moved")
        // THE REVERSE, BY THE PANEL: `Rectangle` with a side gold -- the width is gold when the panel has closed
        val r = wall(); val s3 = Screen(r)
        s3.tapBody(W1); assertEquals("FW1.bottom", s3.pick.gold)
        assertTrue(s3.shape(W1, SurveyPrepEdit.SHAPE_BOX).changed)
        assertEquals(State(W1, true, "FW1.w"), s3.pick); assertEquals("FW1.w", assertIs<Landing.Route.Gold>(s3.route(2370)).code)
        // BY FINE MOVE: a freed door's corner nudged 1 mm off its box stores its shape -- the gold moves to its bottom side
        val d = wallWithDoor(); val s4 = Screen(d); val door = mark(d, D1)
        s4.tapBody(D1); assertEquals("FD1.w", s4.pick.gold)
        s4.shape(D1, SurveyPrepEdit.SHAPE_FREE); s4.tapHandle("q3"); s4.fineMove(D1); assertEquals("q3", s4.pad!!.part)
        assertTrue(s4.press(Key.RIGHT).moved); assertNotNull(Quad.of(door))
        assertEquals(State(D1, true, "FD1.bottom"), s4.pick); assertEquals("q3", s4.pad!!.part, "the pad stays up, on its corner")

        // NOTHING CHANGES AT ALL (Out.stay) WHERE THE ASKED FIGURES ARE AS THEY WERE: a body move, a read figure that is
        // gold, a Line, nothing picked, an item that is gone
        val t = wallWithDoor(); val tw = mark(t, W1)
        val held = State(W1, true, "FW1.s")
        assertTrue(SurveyPrep.moveMark(tw, tw.copy(), NudgePad.BODY, 40.0, 25.0, A, B))
        Pick.reshaped(sc(t), held).let { assertTrue(it.stay); assertEquals(held, it.state); assertEquals(Pick.Pad.KEEP, it.pad) }
        t.actual["FW1.bottom"] = 2492.0
        assertTrue(Pick.reshaped(sc(t), State(W1, true, "FW1.bottom")).stay, "a read figure he chose stays gold while it is asked")
        assertTrue(Pick.reshaped(sc(t), Pick.NONE).stay); assertTrue(Pick.reshaped(sc(t), State("gone", true, null)).stay)
        val line = SurveyPrep.Meas("m1", SurveyMeas.LINE, "F-M1", mutableListOf(pt(200.0, 300.0), pt(1400.0, 300.0))).also { t.surface("front").meas.add(it) }
        assertTrue(Pick.reshaped(sc(t), State("m1", true, t.keysOf(line).first())).stay)
        // nothing is ever gold on the 360, nor after Survey: nothing turns gold there, whatever moved
        assertTrue(Pick.reshaped(sc(t, step = StepRoute.PANO), State(W1, true, null)).stay); assertTrue(Pick.reshaped(sc(t, 1), State(W1, true, null)).stay)
        // a held mark with NOTHING gold and figures unread takes its first unread one, as a tap on its body gives it --
        // and nothing typed is dropped, there being no figure it was typed for
        Pick.reshaped(sc(t), State(W1, true, null)).let { assertEquals(State(W1, true, "FW1.right"), it.state); assertEquals(Pick.Pad.KEEP, it.pad) }
        assertEquals(Pick.body(sc(t), State(W1, true, null), W1).state, Pick.reshaped(sc(t), State(W1, true, null)).state)
        // held is never changed by it: a mark the walk reached stays without handles
        assertEquals(State(W1, false, "FW1.right"), Pick.reshaped(sc(t), State(W1, false, "FW1.w")).state)
        // (the 40 mm it was carried put it nearer the RIGHT corner: it asks `r` where it asked `l` -- a move changes
        // what is asked, with no shape in it)
        assertEquals(SIDES + listOf("FW1.r", "FW1.s", "FW1.p"), t.keysOf("front", tw, A, B))
        assertEquals(State(W1, true, "FW1.right"), Pick.reshaped(sc(t), State(W1, true, "FW1.l")).state)
        // every figure read and the gold on a key it stopped asking: nothing is gold
        for (k in t.keysOf("front", tw, A, B)) t.actual[k] = 1000.0
        Pick.reshaped(sc(t), State(W1, true, "FW1.w")).let { assertEquals(State(W1, true, null), it.state); assertEquals(Pick.Pad.SHUT_DROP, it.pad) }

        // NOT ONLY A SHAPE: a door carried onto the wall's edge stops asking its distance from that corner (the rule is as
        // old as the checklist) -- the gold left on it was refused the same way, and follows now
        val e = wallWithDoor(); val ed = mark(e, D1)
        assertTrue("FD1.r" in e.keysOf("front", ed, A, B))
        assertTrue(SurveyPrep.moveMark(ed, ed.copy(), NudgePad.BODY, 150.0, 0.0, A, B)); assertEquals(A, ed.x1)
        assertFalse("FD1.r" in e.keysOf("front", ed, A, B))
        assertEquals(State(D1, true, "FD1.w"), Pick.reshaped(sc(e), State(D1, true, "FD1.r")).state)
    }

    @Test fun `review 4 - a corner let go where it started is unmoved - its drag takes the undo step back`() {
        val p = wallWithDoor(); val w = mark(p, W1); val d = mark(p, D1)
        // a mark is unmoved against itself and its copy; any move of its box or of one corner is a move
        assertTrue(Quad.unmoved(w, w)); assertTrue(Quad.unmoved(w, w.copy())); assertTrue(Quad.unmoved(d, d.copy()))
        for (part in listOf(NudgePad.BODY, "l", "rt", "q1")) {
            val m = w.copy(); assertTrue(SurveyPrep.moveMark(m, w.copy(), part, 7.0, 7.0, A, B), part); assertFalse(Quad.unmoved(m, w), part)
        }
        // the SHAPE counts, not only the box: the window against its own bounding rectangle
        assertFalse(Quad.unmoved(w.copy().also { Quad.clear(it) }, w))
        // nothing else is looked at: a drag writes no other key, and no reading
        assertTrue(Quad.unmoved(w.copy().also { it.extra = SurveyPrep.withKey(it.extra, SurveyPrep.LOCKED, true) }, w))

        // THE COMMON CASE ON THE PHONE: a slope smaller than the snapping distance at this zoom. At fit on the example
        // wall that is 148 mm -- the bottom-left corner of the window as a plain rectangle, dragged up 50, 100, 140 mm,
        // snaps back level, no `quad` is stored, and the mark is exactly as it was: UNMOVED, so the drag's undo step goes
        // and nothing is saved
        val r = SurveyPrep.Mark("w9", "W", "FW9", 600.0, 2970.0, 900.0, 2450.0)
        assertEquals(148.0, Math.floor(tolAt()))
        for (up in listOf(50.0, 100.0, 140.0)) {
            val start = r.copy()
            assertEquals("hv", assertNotNull(Quad.drag(r, start, 0, 0.0, up, A, B, tolAt()), "$up: the drag itself is taken").snap, "$up")
            assertNull(Quad.of(r)); assertTrue(Quad.unmoved(r, start), "$up")
        }
        // 160 mm is the first slope that stays: moved, its step kept
        r.copy().let { start -> val m = r.copy(); assertEquals("v", Quad.drag(m, start, 0, 0.0, 160.0, A, B, tolAt())!!.snap); assertNotNull(Quad.of(m)); assertFalse(Quad.unmoved(m, start)) }
        // zoomed in four times the limit is 37 mm, eight times the 25 mm floor: a small slope is drawn zoomed in
        for ((zoom, back, stays) in listOf(Triple(4.0, 30.0, 40.0), Triple(8.0, 20.0, 30.0))) {
            val start = r.copy()
            val a = r.copy(); assertNotNull(Quad.drag(a, start, 0, 0.0, back, A, B, tolAt(zoom))); assertTrue(Quad.unmoved(a, start), "$zoom x, $back mm")
            val b = r.copy(); assertNotNull(Quad.drag(b, start, 0, 0.0, stays, A, B, tolAt(zoom))); assertFalse(Quad.unmoved(b, start), "$zoom x, $stays mm")
        }
        // (a side snaps only within 5 degrees too: on the 400 mm door the limit is 35 mm at any zoom that allows more)
        d.copy().let { start ->
            val a = d.copy(); assertEquals("hv", Quad.drag(a, start, 3, 0.0, -30.0, A, B, tolAt())!!.snap); assertTrue(Quad.unmoved(a, start))
            val b = d.copy(); assertEquals("v", Quad.drag(b, start, 3, 0.0, -50.0, A, B, tolAt())!!.snap); assertFalse(Quad.unmoved(b, start))
        }
        // dragged far out and BACK in one gesture (every move is from the mark as it was at the press): unmoved again
        val start = w.copy()
        assertNotNull(Quad.drag(w, start, 2, -400.0, -300.0, A, B, tolAt())); assertFalse(Quad.unmoved(w, start))
        assertNotNull(Quad.drag(w, start, 2, 0.0, 0.0, A, B, tolAt())); assertTrue(Quad.unmoved(w, start))
        // a four-corner mark's corner dragged a little along its sloping side and let go off its lines: moved
        assertNotNull(Quad.drag(w, start, 0, 0.0, 300.0, A, B, tolAt())); assertFalse(Quad.unmoved(w, start))
        // it is the nudge's own test too (NudgePad.press takes its step only for a nudge that changes the mark): a corner
        // hard against the face's edge moves nothing and takes none
        val low = SurveyPrep.Mark("d3", "D", "FD3", 0.0, 400.0, 0.0, 2100.0).also { p.surface("front").marks.add(it) }
        var steps = 0
        assertFalse(NudgePad.press(p, NudgePad.start(p, "d3", SURVEY, "q0", null, PW, PH, DP)!!, Key.LEFT, A, B) { steps++ }.moved); assertEquals(0, steps)
        assertTrue(Quad.unmoved(low, SurveyPrep.Mark("d3", "D", "FD3", 0.0, 400.0, 0.0, 2100.0)))
    }
}
