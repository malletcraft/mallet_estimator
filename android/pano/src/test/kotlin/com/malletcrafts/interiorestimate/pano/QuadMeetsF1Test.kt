package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch.Box
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.io.File
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * WHERE THE FOUR-CORNER MARK (port 63 slice Q1) MEETS H1 AND F1 -- the merge of batch-next (030a2f7f5) into port63-quad,
 * 16 Nov 2026. The two lines of work forked at 7c49835 and each changed the menu, the press, the floating trash, the
 * 360 and MarkDims without the other. Git merged all but one line of them by itself; this test holds what each MEETING
 * POINT was decided to be, by each side's own stated rule:
 *
 *  1. THE MENU. F1: the delete row is the LAST row. Q1: the shape row stands after Duplicate, before Lock. So:
 *     `Duplicate · Four corners · Lock · Lock all … · Cancel · Delete`; where the phase hid Duplicate the shape row is
 *     the first row; it is never after Delete. `menuItemsOf`'s sixth parameter is F1's `chains`, its seventh Q1's `shape`.
 *     SETTLED BY PORT 63 SLICE I2b (23 Nov 2026): the long-press menu is gone and the rows are THE PANEL'S ACTS
 *     (MarkSheet.acts, DESIGN-imagemeter §6, §8.2), where the shape row is line 2's first and Delete is still last and
 *     apart in both orientations (the second test) -- what the six-row menu could not do on the landscape window.
 *  2. THE PRESS on 2 · Mark. F1: the picked Tape's body comes before the marks. Q1: a free mark's corners stand where
 *     the eight tabs do, and its body counts only inside its shape. So a tape across a four-corner window is moved by
 *     its band, the window by the rest of its SHAPE, and the empty corner of its bounding box is neither.
 *  3. THE CODE CHIP (the floating trash until slice I2b, which took it away). F1: what deletes is 16 dp from the
 *     picked item's OWN handles. Q1: a free mark's handles are its four corners, and the bounding box is kept. The
 *     trash is gone; the picked item's CODE CHIP took its place by the same two rules (DESIGN-imagemeter §8.2): it
 *     keeps the bounding box's top-left and stands 16 dp clear of the four corners as they stand -- not of eight tabs
 *     that are not drawn.
 *  4. THE 360. The same two rules through the view: tabs (the free corners) before a chain's point before the picked
 *     tape's body before the marks, whose outline is the real shape. (Nothing on the 360 deletes since slice I2b.)
 *  5. GOLD. H1: the one being measured is gold whatever its state. Q1: two new roles, SIDE and DIAG, and a `!` on the
 *     sloping side. So a side being measured is gold like any figure, its line is there to be drawn gold, and its `!`
 *     takes the gold plate's dark ink.
 *
 * And Q1 is still invisible to `:app` (it is compiled only by CI): the screen's calls are F1's, unchanged.
 *
 * Test names are plain ASCII. The wall is DESIGN-quad's example: 3600 x 2700 mm, the window FW1 with a sloping bottom;
 * on the canvas 0.2 px a mm at 2 px a dp, so the photo is 360 x 270 dp.
 */
class QuadMeetsF1Test {

    private val A = 3600.0; private val B = 2700.0; private val L = 5000.0
    private val DP = 2.0; private val K = 0.2
    private val en = Tx.EN
    private fun px(x: Double, y: Double) = doubleArrayOf(x * K, (B - y) * K)

    /** FW1: its box 600 … 2970 x 900 … 2450, its bottom sloping from (600, 1670) down to (2970, 900). */
    private fun window(id: String = "w1"): SurveyPrep.Mark {
        val e = SurveyPrep.Mark(id, "W", "FW1", 600.0, 2970.0, 900.0, 2450.0)
        assertTrue(Quad.set(e, listOf(doubleArrayOf(600.0, 1670.0), doubleArrayOf(2970.0, 900.0), doubleArrayOf(2970.0, 2450.0), doubleArrayOf(600.0, 2450.0))))
        return e
    }

    private fun drawn(e: SurveyPrep.Mark, free: Boolean): SurveyPrepTouch.Drawn {
        val p = px(e.x0, e.y1); val q = px(e.x1, e.y0)
        return SurveyPrepTouch.Drawn(e.id, p[0], q[0], p[1], q[1], corners = if (free) Quad.corners(e).map { px(it[0], it[1]) } else null)
    }

    // ---------------------------------------------------------------- 1. the menu

    @Test fun `the menu - Delete stays the last row and the shape row stands after Duplicate, before Lock, at every stage`() {
        val p = SurveyPrep.fresh(L, A, B)
        p.surface("front").marks.add(SurveyPrep.Mark("d1", "D", "FD1", 100.0, 500.0, 0.0, 2100.0))
        p.surface("front").marks.add(SurveyPrep.Mark("d2", "D", "FD2", 3000.0, 3400.0, 0.0, 2100.0, mapOf(SurveyPrep.LOCKED to true)))
        p.surface("front").marks.add(window())
        p.surface("front").marks.add(SurveyPrep.Mark("e1", "E", "FE1", 300.0, 350.0, 2300.0, 2350.0))
        p.addMeas("front", SurveyMeas.TAPE, listOf(doubleArrayOf(100.0, 2600.0), doubleArrayOf(900.0, 2600.0)), "t1")
        p.addChain("front", "x", listOf(doubleArrayOf(300.0, 600.0), doubleArrayOf(900.0, 600.0)), listOf(null, null), "k1")
        val at = java.time.Instant.parse("2026-11-16T05:00:00Z")
        PhaseTools.place(p, "front", "CRP", 1300.0, 500.0, A, B, "u1", PhaseTools.WISH_LIST, null, at) { }!!
        PhaseTools.place(p, "front", "D", 2300.0, 400.0, A, B, "v1", PhaseTools.VENDOR_REVIEW, null, at) { }!!
        val shapes = setOf(SurveyPrepEdit.SHAPE_FREE, SurveyPrepEdit.SHAPE_BOX)
        var withRow = 0; var first = 0
        for (id in listOf("d1", "d2", "w1", "e1", "t1", "k1", "u1", "v1", SurveyRef.id("front"))) for (st in listOf<Int?>(0, 1, 2, 3, 4, 5, null))
            for (picked in listOf(null, MarkLife.EXIST, MarkLife.WISH)) for (chains in listOf(false, true)) {
                val plain = SurveyPrepEdit.menuItemsOf(en, p, id, st, picked, chains = chains)
                val rows = SurveyPrepEdit.menuItemsOf(en, p, id, st, picked, chains = chains, shape = true)
                val keys = rows.map { it.first }; val why = "$id at $st, $picked, chains $chains: $keys"
                // F1: the delete row, where the phase leaves one, is the LAST -- with the shape row asked for or not
                assertEquals(DeleteGuard.last(rows) { it.first == SurveyPrepEdit.DELETE }, rows, why)
                assertEquals(DeleteGuard.last(plain) { it.first == SurveyPrepEdit.DELETE }, plain, why)
                if (SurveyPrepEdit.DELETE in keys) assertEquals(SurveyPrepEdit.DELETE, keys.last(), why)
                // Q1: at most one shape row, and without it the menu is exactly F1's
                assertTrue(keys.count { it in shapes } <= 1, why); assertEquals(plain, rows.filter { it.first !in shapes }, why)
                assertTrue(plain.none { it.first in shapes }, why)
                val i = keys.indexOfFirst { it in shapes }
                if (i < 0) continue
                withRow++
                // ... right after Duplicate where the phase left it, else the FIRST row; before Lock; never after Delete
                if (SurveyPrepEdit.DUPLICATE in keys) assertEquals(SurveyPrepEdit.DUPLICATE, keys[i - 1], why) else { assertEquals(0, i, why); first++ }
                if (SurveyPrepEdit.LOCK in keys) assertEquals(SurveyPrepEdit.LOCK, keys[i + 1], why)
                if (SurveyPrepEdit.DELETE in keys) assertTrue(i < keys.indexOf(SurveyPrepEdit.DELETE), why)
                assertTrue(i < keys.indexOf(SurveyPrepEdit.CANCEL), why)
                // only on a mark that may have four corners, where the phase lets it be edited
                assertNotNull(p.markById(id), why); assertTrue(PhaseTools.editable(p, id, st), why)
            }
        assertTrue(withRow > 0 && first > 0, "the row was seen, also as the first row ($withRow, $first)")
        // the words, in order
        assertEquals("Duplicate|Four corners|Lock|Lock all Door|Cancel|Delete", SurveyPrepEdit.menuItemsOf(en, p, "d1", 0, null, shape = true).joinToString("|") { it.second })
        assertEquals("Duplicate|Rectangle|Lock|Lock all Window|Cancel|Delete", SurveyPrepEdit.menuItemsOf(en, p, "w1", 0, null, shape = true).joinToString("|") { it.second })
        assertEquals("Duplicate|Four corners|Unlock|Lock all Door|Cancel|Delete", SurveyPrepEdit.menuItemsOf(en, p, "d2", 0, null, shape = true).joinToString("|") { it.second })
        // a wish-born unit in the Wish list: no copy there, so the shape row is first -- and its Drop is still the last
        val wish = SurveyPrepEdit.menuItemsOf(en, p, "u1", 1, null, shape = true)
        assertEquals(listOf("free", "lock", "lockall", "cancel", "del"), wish.map { it.first }); assertEquals("Drop", wish.last().second)
        // THE SIXTH PARAMETER IS F1's `chains`, THE SEVENTH Q1's `shape`: a bare `true` in the sixth place turns the chain
        // tool's Duplicate on and adds NO shape row -- what batch-next's own callers and tests pass there
        assertEquals(SurveyPrepEdit.menuItemsOf(en, p, "d1", 0, null), SurveyPrepEdit.menuItemsOf(en, p, "d1", 0, null, true))
        assertTrue(SurveyPrepEdit.DUPLICATE in SurveyPrepEdit.menuItemsOf(en, p, "k1", 0, null, true).map { it.first })
        assertFalse(SurveyPrepEdit.DUPLICATE in SurveyPrepEdit.menuItemsOf(en, p, "k1", 0, null, false, true).map { it.first })
        assertEquals(SurveyPrepEdit.menuItemsOf(en, p, "k1", 0, null), SurveyPrepEdit.menuItemsOf(en, p, "k1", 0, null, Features.CHAINS, true), "a chain has no shape row")
        // the menu the screen measures has room for six rows with the delete row apart: DeleteGuard's numbers are per row
        assertEquals(6, SurveyPrepEdit.menuItemsOf(en, p, "d1", 0, null, shape = true).size)
    }

    @Test fun `the shape row in the panel - line 2's first, Delete still last and apart, and nothing under Done - what the six-row menu could not do in landscape`() {
        // UNTIL SLICE I2b this test held a fault for slice Q2 to settle: with the shape row the long-press menu was six
        // rows (288 dp), and on the Mi A3's landscape window (336 to 360 dp) F1's placement could no longer keep its
        // delete row 16 dp from the finger that opened it -- 0 dp at 336 and 348, 8 dp at 360. THE MENU IS GONE. The
        // rows are the panel's acts (MarkSheet.acts), and there the shape row costs nothing: it is line 2's FIRST cell,
        // in a column that stood empty, and Delete does not move.
        val p = SurveyPrep.fresh(L, A, B)
        p.surface("front").marks.add(SurveyPrep.Mark("d1", "D", "FD1", 100.0, 500.0, 0.0, 2100.0))
        p.surface("front").marks.add(SurveyPrep.Mark("d2", "D", "FD2", 3000.0, 3400.0, 0.0, 2100.0, mapOf(SurveyPrep.LOCKED to true)))
        p.surface("front").marks.add(window())
        val plain = MarkSheet.acts(en, p, "d1", 0, null); val shaped = MarkSheet.acts(en, p, "d1", 0, null, shape = true)
        assertEquals(listOf(SurveyPrepEdit.LOCK, SurveyPrepEdit.DUPLICATE, MarkSheet.FINE), shaped.line1.map { it.id }); assertEquals(plain.line1.map { it.id }, shaped.line1.map { it.id })
        assertEquals(listOf(SurveyPrepEdit.LOCK_ALL, SurveyPrepEdit.DELETE), plain.line2.map { it.id })
        assertEquals(listOf(SurveyPrepEdit.SHAPE_FREE, SurveyPrepEdit.LOCK_ALL, SurveyPrepEdit.DELETE), shaped.line2.map { it.id })
        assertEquals(listOf(SurveyPrepEdit.SHAPE_BOX, SurveyPrepEdit.LOCK_ALL, SurveyPrepEdit.DELETE), MarkSheet.acts(en, p, "w1", 0, null, shape = true).line2.map { it.id })
        assertEquals(2, plain.rows); assertEquals(2, shaped.rows, "the panel is no taller for it")
        // PORTRAIT: Delete in the last column of line 2, exactly where it stood without the shape row, 24 dp from Lock all
        val a = MarkSheet.slots(plain, 336.0, false).single { it.cell.delete }; val b = MarkSheet.slots(shaped, 336.0, false).single { it.cell.delete }
        assertEquals(listOf(a.row, a.l, a.r), listOf(b.row, b.l, b.r)); assertEquals(336.0, b.r, 1e-9)
        val all = MarkSheet.slots(shaped, 336.0, false).single { it.cell.id == SurveyPrepEdit.LOCK_ALL }
        assertEquals(MarkSheet.DELETE_GAP_DP, b.l - all.r, 1e-9); assertTrue(MarkSheet.DELETE_GAP_DP > DeleteGuard.CLEAR_DP)
        // LANDSCAPE, every window of the phone: ONE line of six, Delete last, 24 dp from the act before it, and the line
        // ends 16 dp short of Done -- nothing stands under Done, and nothing is placed by where the finger was
        for (wide in listOf(736.0, 756.0, 780.0)) {
            val doneLeft = wide - 70
            val line = MarkSheet.slots(shaped, wide, true, doneLeft)
            assertEquals(6, line.size); assertTrue(line.all { it.row == 0 }); assertTrue(line.last().cell.delete)
            assertEquals(MarkSheet.DELETE_GAP_DP, line.last().l - line[4].r, 1e-9, "$wide")
            assertTrue(line.last().r <= doneLeft - MarkSheet.DONE_CLEAR_LAND_DP + 1e-9, "$wide")
            assertTrue(line.all { it.r - it.l >= 88.0 }, "$wide: every act is a real button: ${line.map { it.r - it.l }}")
        }
        // the row is OFF until slice Q2 turns the switch (the screen passes none: the last test of this file)
        assertFalse(plain.has(SurveyPrepEdit.SHAPE_FREE)); assertFalse(MarkSheet.acts(en, p, "w1", 0, null).has(SurveyPrepEdit.SHAPE_BOX))
        // and its act: Done followed by the act; on a LOCKED mark it is drawn and refused in the lock's own words, in the panel
        val pn = MarkSheet.openPanel(p, "d1", -1000L)!!
        assertIs<MarkSheet.ActStep.Run>(MarkSheet.actStep(en, p, pn, SurveyPrepEdit.SHAPE_FREE, 0, null, shape = true))
        assertIs<MarkSheet.ActStep.Refused>(MarkSheet.actStep(en, p, pn, SurveyPrepEdit.SHAPE_FREE, 0, null), "not drawn: nothing happens")
        val lk = MarkSheet.actStep(en, p, MarkSheet.openPanel(p, "d2", -1000L)!!, SurveyPrepEdit.SHAPE_FREE, 0, null, shape = true)
        assertIs<MarkSheet.ActStep.Refused>(lk); assertEquals(SurveyPrepEdit.refusal(en, "FD2"), lk.panel.said)
    }

    // ---------------------------------------------------------------- 2. the press on 2 · Mark

    @Test fun `the press - a picked tape across a four-corner window is moved by its band, the window by the rest of its shape`() {
        val w = window(); val d = drawn(w, true); val c = d.corners!!
        val p = SurveyPrep.fresh(L, A, B); p.surface("front").marks.add(w)
        // a tape laid level across the window at 2000 mm, from 300 to 3300: across the shape, and across nothing else
        val t = p.addMeas("front", SurveyMeas.TAPE, listOf(doubleArrayOf(300.0, 2000.0), doubleArrayOf(3300.0, 2000.0)), "t1")
        val scr = t.pts.map { px(it[0], it[1]) }; val band = SurveyMeas.TAPE_MIN_HALF_DP * DP; val tol = 16 * DP
        fun body(x: Double, y: Double, sel: SurveyPrep.Meas? = t) =
            SurveyGuide.bodyPress(true, SurveyPrepPalette.SEL, sel, false, scr, x, y, SurveyGuide.END_HIT_DP * DP, SurveyMeas.PICK_DP * DP, band)
        fun hit(x: Double, y: Double, selId: String? = "t1", sel: SurveyPrep.Meas? = t, tags: List<SurveyPrepTouch.Tag> = emptyList(), nodes: List<SurveyPrepTouch.Nodes> = emptyList()) =
            SurveyPrepTouch.hit(listOf(d), selId, tags, x, y, tol, DP, nodes, body = body(x, y, sel) != null)
        val on = px(1800.0, 2000.0)                              // on the band, inside the window's shape
        assertTrue(Pano360.inPoly(on[0], on[1], c)); assertSame(SurveyGuide.BodyPress.Grab, body(on[0], on[1]))
        // F1: THE PICKED TAPE'S BODY BEFORE THE MARKS -- a four-corner one as much as a rectangle
        assertSame(SurveyPrepTouch.Hit.Body, hit(on[0], on[1]))
        // ... and the window by the rest of ITSELF: inside its shape, off the band's 12 dp
        val off = px(1800.0, 1700.0)
        assertNull(body(off[0], off[1])); val rest = hit(off[0], off[1]); assertIs<SurveyPrepTouch.Hit.Mark>(rest); assertEquals("w1", rest.id); assertEquals("body", rest.part)
        // Q1: THE EMPTY CORNER OF ITS BOUNDING BOX IS NOT THE MARK, with a tape picked as with nothing picked -- the finger slides the photo
        val empty = px(700.0, 1000.0)
        assertTrue(empty[0] > d.x0 && empty[0] < d.x1 && empty[1] > d.yT && empty[1] < d.yB); assertFalse(Pano360.inPoly(empty[0], empty[1], c))
        assertNull(hit(empty[0], empty[1])); assertNull(hit(empty[0], empty[1], selId = null, sel = null))
        // with the tape NOT picked (F1: only the picked one moves by its body) the same press on the band is the window's own
        val un = hit(on[0], on[1], selId = null, sel = null); assertIs<SurveyPrepTouch.Hit.Mark>(un); assertEquals("body", un.part)
        // ... and with the WINDOW picked, its four corners answer -- there is no body to hand, the tape is not the picked one
        for ((i, part) in Quad.PARTS.withIndex()) { val h = hit(c[i][0], c[i][1], selId = "w1", sel = null); assertIs<SurveyPrepTouch.Hit.Mark>(h); assertEquals(part, h.part) }
        // F1's order holds over a free mark: a figure under the finger and a chain's point keep the press from the tape's body
        val tag = SurveyPrepTouch.Tag("FW1.top", Box(on[0] - 20, on[1] - 10, on[0] + 20, on[1] + 10))
        val onTag = hit(on[0], on[1], tags = listOf(tag)); assertIs<SurveyPrepTouch.Hit.Tag>(onTag); assertEquals("FW1.top", onTag.code)
        val node = hit(on[0], on[1], nodes = listOf(SurveyPrepTouch.Nodes("k1", listOf(doubleArrayOf(on[0] + 6, on[1])))))
        assertIs<SurveyPrepTouch.Hit.Node>(node)
        // THE PICKED ITEM'S OWN HANDLES COME FIRST, whichever the item is. A tape's end is the end's (the body answers
        // nothing within its 14 dp, so the press goes on to the marks); and were a mark picked with `body` handed in --
        // the screen never does both: one thing is picked -- its free corner would still be its corner, as a tab is
        val end = px(3300.0, 2000.0); assertNull(body(end[0] - 10, end[1]))
        val both = SurveyPrepTouch.hit(listOf(d), "w1", emptyList(), c[2][0], c[2][1], tol, DP, body = true)
        assertIs<SurveyPrepTouch.Hit.Mark>(both); assertEquals("q2", both.part)
        // ... and off its corners the body handed in wins before the picked mark's own body, as before any mark
        assertSame(SurveyPrepTouch.Hit.Body, SurveyPrepTouch.hit(listOf(d), "w1", emptyList(), on[0], on[1], tol, DP, body = true))
        // the same wall with the window a RECTANGLE (no corners handed in): the band, then the box -- F1 as it was
        val boxed = drawn(w, false)
        assertSame(SurveyPrepTouch.Hit.Body, SurveyPrepTouch.hit(listOf(boxed), "t1", emptyList(), on[0], on[1], tol, DP, body = true))
        val inBox = SurveyPrepTouch.hit(listOf(boxed), "t1", emptyList(), empty[0], empty[1], tol, DP, body = false); assertIs<SurveyPrepTouch.Hit.Mark>(inBox); assertEquals("w1", inBox.id)
    }

    // ---------------------------------------------------------------- 3. the code chip (the floating trash, until slice I2b)

    private fun far(tap: Box, pts: List<DoubleArray>) = pts.minOf { SurveyPrepTouch.boxDist(tap, it[0], it[1]) }

    @Test fun `the chip - beside a free mark's bounding box, 16 dp clear of its four corners and not of tabs that are not drawn`() {
        val w = window(); val d = drawn(w, true); val c = d.corners!!
        val bb = Box(d.x0, d.yT, d.x1, d.yB); val pw = A * K; val ph = B * K
        val text = 40.0 * DP; val clear = SurveyPrepTouch.CHIP_CLEAR_DP * DP
        // WHAT IT KEEPS CLEAR OF: the four corners as they stand; a rectangle (no corners) its eight tabs, a spot its centre
        val four = SurveyPrepTouch.handlesOf(bb, false, c)
        assertEquals(c.map { it.toList() }, four.map { it.toList() })
        assertEquals(8, SurveyPrepTouch.handlesOf(bb, false).size); assertEquals(8, SurveyPrepTouch.handlesOf(bb, false, null).size)
        assertEquals(1, SurveyPrepTouch.handlesOf(bb, true, c).size, "a spot has no corners: its centre")
        // THE CHIP keeps the BOUNDING box (DESIGN-imagemeter 8.2), F1's 16 dp from every corner. The example window stands
        // 25 dp under the photo's top -- no room above it -- so its chip is BELOW ITS BOUNDING BOX'S bottom-left corner,
        // not below q0, the free bottom-left corner, which stands 77 dp up the left side
        val chip = assertNotNull(SurveyPrepTouch.codeChip(bb, four, text, pw, ph, DP))
        assertEquals("below", chip.how); assertEquals(bb.l, chip.box.l, 1e-9); assertTrue(chip.tap.t >= bb.b + clear - 1e-9)
        assertTrue(c[0][1] < bb.b - 70 * DP, "q0 is far up the side: ${(bb.b - c[0][1]) / DP} dp")
        assertTrue(far(chip.tap, c) >= clear - 1e-9, "the chip is ${far(chip.tap, c) / DP} dp from a corner")
        // ... and the same window lower on a taller photo has it ABOVE its bounding box's top-left
        val low = Box(bb.l, bb.t + 200, bb.r, bb.b + 200); val lowC = c.map { doubleArrayOf(it[0], it[1] + 200) }
        val above = assertNotNull(SurveyPrepTouch.codeChip(low, SurveyPrepTouch.handlesOf(low, false, lowC), text, pw, ph + 200, DP))
        assertEquals("above", above.how); assertEquals(low.l, above.box.l, 1e-9); assertTrue(above.tap.b <= low.t - clear + 1e-9)
        // ... the very place a rectangle of the same box gets: outside the box, every handle of either kind is 16 dp off
        val asBox = assertNotNull(SurveyPrepTouch.codeChip(bb, SurveyPrepTouch.handlesOf(bb, false), text, pw, ph, DP))
        assertEquals(listOf(asBox.tap.l, asBox.tap.t, asBox.how), listOf(chip.tap.l, chip.tap.t, chip.how))
        // every shape of a walk over the photo: a chip, where there is one, is 16 dp from each corner AS IT STANDS
        var seen = 0
        for (x0 in listOf(8.0, 120.0, 400.0)) for (y0 in listOf(8.0, 90.0, 260.0)) for (ww in listOf(90.0, 240.0, 560.0)) for (hh in listOf(80.0, 200.0, 500.0))
            for (f in listOf(0.0, 0.35, 0.8)) {
                val b = Box(x0, y0, x0 + ww, y0 + hh)
                // q0 bottom-left, q1 bottom-right, q2 top-right pulled in along the top by f, q3 top-left pulled down by f
                val q = listOf(doubleArrayOf(b.l, b.b), doubleArrayOf(b.r, b.b), doubleArrayOf(b.r - f * ww * 0.6, b.t), doubleArrayOf(b.l, b.t + f * hh * 0.6))
                val k = SurveyPrepTouch.codeChip(b, SurveyPrepTouch.handlesOf(b, false, q), text, 680.0, 680.0, DP) ?: continue
                seen++
                assertTrue(far(k.tap, q) >= clear - 1e-9, "$x0 $y0 $ww $hh $f: ${far(k.tap, q) / DP} dp from a corner, ${k.how}")
                assertTrue(k.tap.l >= 0 && k.tap.t >= 0 && k.tap.r <= 680.0 && k.tap.b <= 680.0, "$x0 $y0 $ww $hh $f: on the photo")
            }
        assertTrue(seen > 40, "$seen chips")
        // WHERE THE TWO KINDS OF HANDLE DIFFER: a mark that fills the photo both ways (zoomed into) has its chip INSIDE its
        // box, 16 dp in from the photo's top-left. Its eight tabs are all off the photo, so a rectangle gets that place; a
        // free mark whose top-left corner stands THERE does not -- and gets NO chip rather than one on a corner (a long
        // press still opens its panel)
        val big = Box(-200.0, -200.0, 900.0, 900.0)
        val boxed = assertNotNull(SurveyPrepTouch.codeChip(big, SurveyPrepTouch.handlesOf(big, false), text, 680.0, 680.0, DP))
        assertEquals("inside", boxed.how)
        val q = listOf(doubleArrayOf(-200.0, 900.0), doubleArrayOf(900.0, 900.0), doubleArrayOf(900.0, -200.0), doubleArrayOf(boxed.tap.l + 10, boxed.tap.t + 10))
        assertTrue(far(boxed.tap, q) < clear, "that place is ON the free mark's top-left corner")
        assertNull(SurveyPrepTouch.codeChip(big, SurveyPrepTouch.handlesOf(big, false, q), text, 680.0, 680.0, DP), "never on a corner")
        // the floating trash and its rule are gone: there is one thing beside the picked item, and it deletes nothing
        for (gone in listOf("trashSpot", "keepOfMark", "keepOfLine")) assertTrue(SurveyPrepTouch::class.java.declaredMethods.none { it.name == gone }, gone)
    }

    // ---------------------------------------------------------------- 4. the 360

    @Test fun `the 360 - free corners before a chain point before the picked tape's body before the marks, and nothing there deletes`() {
        val p = SurveyPrep.fresh(L, A, B).also { q -> for (s in SurveyPrep.SURFACES) for (k in SurveyPrep.SLOTS) q.surface(s).set[k] = true }
        val w = window(); p.surface("front").marks.add(w)
        val fr = Pano360.frames(p, L, A, B); val f = fr.getValue("front"); val v = Pano360.faceView("front", 680.0, 840.0)
        fun s(x: Double, y: Double) = Pano360.toS(v, "front", f, x, y)!!
        val t = p.addMeas("front", SurveyMeas.TAPE, listOf(doubleArrayOf(300.0, 2000.0), doubleArrayOf(3300.0, 2000.0)), "t1")
        val poly = assertNotNull(Pano360.markPoly(v, "front", f, w, DP))
        val sel = Pano360.order(SurveyPrepPalette.SEL, false)
        assertEquals(listOf(Pano360.Step.END, Pano360.Step.TABS, Pano360.Step.NODES, Pano360.Step.BODY, Pano360.Step.MARK, Pano360.Step.LOOK), sel)
        // THE TAPE PICKED: its body across the window's real shape is the body; the rest of the shape is the window; the
        // empty corner of the window's bounding box is neither -- the view turns (Q1: the outline is the real shape)
        val line = Pano360.measLine(v, "front", f, t, DP)
        val scT = Pano360.Scene(t.pts.map { s(it[0], it[1]) }, emptyList(), emptyList(), listOf(poly), "t1", line)
        val on = s(1800.0, 2000.0); val off = s(1800.0, 1650.0); val empty = s(700.0, 1000.0)
        assertEquals(Pano360.Pick.Body, Pano360.pick(sel, scT, on[0], on[1], DP))
        assertEquals(Pano360.Pick.Mark("front", "w1"), Pano360.pick(sel, scT, off[0], off[1], DP))
        assertEquals(Pano360.Pick.Look, Pano360.pick(sel, scT, empty[0], empty[1], DP))
        assertEquals(Pano360.Pick.End(1), Pano360.pick(sel, scT, s(3300.0, 2000.0)[0] - 6, s(3300.0, 2000.0)[1], DP))
        // THE WINDOW PICKED, ITS CORNERS FREE: the four corners are its tabs, each its own part, in the tabs' place --
        // before a chain's point on one of them; no body is handed (the tape is not the picked one) and the band is the window's
        val tabs = Pano360.tabsOf(w, true).map { (part, mm) -> part to s(mm[0], mm[1]) }
        assertEquals(Quad.PARTS, tabs.map { it.first })
        val scW = Pano360.Scene(null, tabs, listOf("k1" to listOf(tabs[2].second.copyOf())), listOf(poly), "w1")
        for ((part, at) in tabs) assertEquals(Pano360.Pick.Tab(part), Pano360.pick(sel, scW, at[0] + 3, at[1] - 3, DP))
        assertEquals(Pano360.Pick.Mark("front", "w1"), Pano360.pick(sel, scW, on[0], on[1], DP))
        // ... and were both handed, the tabs' step is before the body's, as the ends' is
        val both = Pano360.Scene(null, tabs, emptyList(), listOf(poly), "w1", line)
        assertEquals(Pano360.Pick.Tab("q2"), Pano360.pick(sel, both, tabs[2].second[0], tabs[2].second[1], DP))
        assertEquals(Pano360.Pick.Body, Pano360.pick(sel, both, on[0], on[1], DP))
        // THE TRASH that stood beside the picked item on the 360 went in slice I2b (Pano360.trashOf): a long press there
        // opens the panel, whose last act is the one Delete -- for the four-corner window as for any item
        assertTrue(Pano360::class.java.declaredMethods.none { it.name == "trashOf" })
        assertTrue(MarkSheet.panelOpens(p, "w1")); assertTrue(MarkSheet.panelOpens(p, "t1"))
        assertNotNull(MarkSheet.acts(en, p, "w1", 0, null, shape = true).delete)
        // a rectangle whose corners were freed and never dragged: the box's own four, as q0 … q3
        val door = SurveyPrep.Mark("d1", "D", "FD1", 100.0, 500.0, 0.0, 2100.0).also { p.surface("front").marks.add(it) }
        assertEquals(Quad.PARTS, Pano360.tabsOf(door, true).map { it.first })
        // a point has no corners to free whatever is asked
        val e1 = SurveyPrep.Mark("e1", "E", "FE1", 300.0, 350.0, 2300.0, 2350.0).also { p.surface("front").marks.add(it) }
        assertFalse(Quad.mayHave(e1))
    }

    // ---------------------------------------------------------------- 5. gold

    @Test fun `gold - a side being measured is the gold one like any figure, and its closing check takes the gold plate's ink`() {
        val w = window()
        fun textW(text: String, fs: Double, bold: Boolean) = text.length * fs * 0.6
        val u = (A * K) / 480
        val dims = MarkDims.of("front", w, A, B)
        assertEquals(4, dims.count { it.role == MarkDims.Role.SIDE })
        // every figure of the mark made the active one in turn: exactly that figure is active, whatever its role -- and
        // a SIDE (and a diagonal, where one is asked) has a LINE for H1's gold line to follow
        for (d in dims) for (free in listOf(false, true)) {
            val m = DimLayout.mark(emptyMap(), "front", w, A, B, true, false, d.code, ::px, u, DP, ::textW, null, free)
            assertEquals(listOf(d.code), m.figs.filter { it.active }.map { it.code }, d.code)
            val fig = m.figs.single { it.active }
            assertEquals(d.role, fig.role); assertEquals(DimLayout.Rank.ACTIVE, fig.rank)
            if (d.role == MarkDims.Role.SIDE || d.role == MarkDims.Role.DIAG) assertNotNull(fig.line, d.code)
            // H1: SOLID GOLD with dark bold text, whatever its state -- the style asks the state and nothing of the role
            val st = MarkDims.style(fig.state, fig.active)
            assertEquals(listOf(MarkDims.ACTIVE_INK, true, MarkDims.ACTIVE_COLOUR, 0xff, 0xffffff, false), listOf(st.ink, st.bold, st.fill, st.fillAlpha, st.outline, st.dashed), d.code)
        }
        // not being measured, a side's plate is its state's own, as every figure's
        val idle = DimLayout.mark(emptyMap(), "front", w, A, B, false, false, null, ::px, u, DP, ::textW, null).figs.first { it.role == MarkDims.Role.SIDE }
        assertFalse(idle.active); assertEquals(0x000000, MarkDims.style(idle.state, idle.active).fill)
        // Q1's `!` ON THE SLOPING SIDE (the three straight sides and its own reading do not close) while that side is the
        // one being measured: the figure carries the check AND is active, so the `!` is drawn in the gold plate's dark ink
        val read = mapOf("FW1.bottom" to 2495.0, "FW1.right" to 1550.0, "FW1.top" to 2370.0, "FW1.left" to 870.0)
        assertTrue(Quad.closing(read, w)!!.fails)
        val m = DimLayout.mark(read, "front", w, A, B, true, false, "FW1.bottom", ::px, u, DP, ::textW, null)
        val slope = m.figs.single { it.code == "FW1.bottom" }
        assertTrue(slope.active && slope.check); assertEquals(MarkDims.State.MEASURED, slope.state)
        assertEquals(MarkDims.ACTIVE_INK, MarkDims.checkInk(slope.active)); assertEquals(MarkDims.ACTIVE_COLOUR, MarkDims.style(slope.state, slope.active).fill)
        // ... and on that plate alone; with another side active the `!` is amber on the sloping side's own black plate
        assertEquals(listOf("FW1.bottom"), m.figs.filter { it.check }.map { it.code })
        val other = DimLayout.mark(read, "front", w, A, B, true, false, "FW1.top", ::px, u, DP, ::textW, null).figs.single { it.code == "FW1.bottom" }
        assertTrue(other.check && !other.active); assertEquals(MarkDims.CHECK_COLOUR, MarkDims.checkInk(other.active))
    }

    // ---------------------------------------------------------------- slice Q2: the four-corner mark is ON in :app

    private val appDir = File("../app/src/main/kotlin/com/malletcrafts/interiorestimate")
    /** A file of `:app` as code: without its comment lines, so a sentence that names a function is never counted as a call. */
    private fun appCode(name: String) = File(appDir, name).also { assertTrue(it.isFile, "$name is where this test reads it") }.readLines()
        .filter { l -> l.trimStart().let { !it.startsWith("//") && !it.startsWith("*") && !it.startsWith("/*") } }.joinToString("\n")
    private fun count(text: String, what: String) = text.windowed(what.length) { if (it == what) 1 else 0 }.sum()

    /**
     * REWRITTEN IN SLICE Q2 (23 Nov 2026), as DESIGN-quad section 10's note 6 said it would be. Until Q2 this test held
     * that NOTHING of Q1 reached `:app` -- no shape row, no free corner, no `Quad.` in any file -- because a row drawn
     * with no act behind it would have been a dead row on the phone. Q2 turns the four-corner mark on, so the test now
     * holds the NEW calls, one by one: each is the call DESIGN-quad section 11.5 gives, to a rule that is tested in
     * `:pano` (QuadScreenTest and the tests this file's first five sections name). What it guards is the same thing
     * from the other side: `:app` draws and calls, and works nothing out itself -- ONE `freeId`, written by one rule;
     * ONE `free`, handed to the painter, the press, the layout and the 360; and no write of the shape that is not a
     * Quad function.
     */
    @Test fun `the screen's calls are slice Q2's - one freeId, one free, the shape row on, and every rule asked of pano`() {
        val screen = appCode("SurveyPrepScreen.kt"); val p3 = appCode("SurveyPrep360.kt"); val draw = appCode("SurveyPrepDraw.kt")
        // THE SWITCH: the panel's two calls name `shape = true`, and its `when` runs the two shape acts as ONE step
        assertEquals(1, count(screen, "MarkSheet.panel(tx, prep, pn, quoteStage, asPicked, shape = true)")); assertEquals(1, count(screen, "MarkSheet.panel("))
        assertEquals(1, count(screen, "MarkSheet.actStep(tx, prep, pn, act, quoteStage, asPicked, shape = true)")); assertEquals(1, count(screen, "MarkSheet.actStep("))
        assertEquals(0, count(screen, "menuItemsOf("))
        val act = screen.substring(screen.indexOf("fun panelAct(act: String)")).let { it.substring(0, it.indexOf("\n    fun ", 1)) }
        assertEquals(1, count(act, "SurveyPrepEdit.SHAPE_FREE, SurveyPrepEdit.SHAPE_BOX -> {"))
        assertEquals(1, count(act, "val r = MarkSheet.shapeStep(tx, prep, pn.id, step.act, quoteStage, freeId, part) { snapshot() }"))
        assertEquals(1, count(act, "freeId = r.freeId; part = r.part"))
        assertEquals(1, count(act, "r.said?.let { t -> say(t) }"))
        assertEquals(1, count(act, "if (r.hold) took(Pick.shaped(scene(), pick, pn.id))")); assertEquals(1, count(act, "if (r.changed) changed()"))
        // ... after freeId and the part are set, so the hold keeps them (Quad.freed(id, id, BODY) is id)
        assertTrue(act.indexOf("freeId = r.freeId; part = r.part") in 0 until act.indexOf("if (r.hold) took("))
        // the shape's act asks no phase and no lock of its own: MarkSheet.shapeStep answers both (PhaseToolsTest's count of 2 stands)
        assertEquals(1, count(screen, "MarkSheet.shapeStep(")); assertEquals(0, count(screen, "Quad.clear(")); assertEquals(0, count(screen, "Quad.set("))

        // ONE freeId, WRITTEN BY ONE RULE: after every event that can move the pick or the part -- a Pick event, a reading
        // landed, readings pulled under the pick, the room changed -- and by the panel's act; handed to the 360 and to nobody else
        assertEquals(1, count(screen, "var freeId by remember { mutableStateOf<String?>(null) }"))
        assertEquals(4, count(screen, "freeId = Quad.freed(freeId, pick.picked, part)"))
        assertEquals(1, count(screen, "part = NudgePad.touched(part, before, o, body)\n        freeId = Quad.freed(freeId, pick.picked, part)"), "took: once the part is set")
        assertEquals(1, count(screen, "part = NudgePad.BODY\n        freeId = Quad.freed(freeId, pick.picked, part)"), "landed")
        assertEquals(1, count(screen, "pick = kept; part = NudgePad.BODY; freeId = Quad.freed(freeId, pick.picked, part);"), "settled")
        assertEquals(1, count(screen, "pick = kept; freeId = Quad.freed(freeId, pick.picked, part);"), "readings pulled under the pick")
        assertEquals(1, count(screen, "freeId = freeId,"), "the 360 is handed it")
        assertEquals(6, count(screen, "freeId = "), "the four above, the panel's act, the 360's argument -- and nothing else writes it")
        // ONE free: Quad.freeOf, once in the screen and once in the 360, and Quad.free itself in neither
        assertEquals(1, count(screen, "fun freeNow(): (SurveyPrep.Mark) -> Boolean = Quad.freeOf(freeId, pick.picked, part)")); assertEquals(1, count(screen, "Quad.freeOf("))
        assertEquals(1, count(screen, "free = freeNow(),"), "the photo: its press and its painter")
        assertEquals(1, count(screen, "val freeS by rememberUpdatedState(free)"), "the gesture outlives recompositions")
        assertEquals(1, count(screen, "free = free,"), "the painter's Marks")
        // ... handed BEFORE the pinned tail of the screen's Marks(...) (MeasureScreenTest)
        assertTrue(screen.indexOf("free = free,") in 0 until screen.indexOf("nudge = if (which == \"main\") fine else null), mm, u)"))
        assertEquals(1, count(p3, "fun free(e: SurveyPrep.Mark): Boolean = Quad.freeOf(freeIdS, selS?.id, selS?.part)(e)")); assertEquals(1, count(p3, "Quad.freeOf("))
        for (name in appDir.list()!!.filter { it.endsWith(".kt") }) assertFalse(Regex("""\bQuad\.free\(""").containsMatchIn(appCode(name)), "$name asks Quad.free itself")

        // THE PRESS: every mark of the face with its free corners, BEFORE its spot (NotePinTest's literal); hit with F1's `body`
        assertEquals(1, count(screen, "SurveyPrepTouch.Drawn("))
        assertEquals(1, count(screen, "corners = Quad.freeCorners(e, freeS(e))?.map { c -> toS(sq.x(c[0]), sq.y(c[1])).let { at -> doubleArrayOf(at[0].toDouble(), at[1].toDouble()) } },"))
        assertEquals(1, count(screen, "corners ="))
        assertTrue(screen.indexOf("corners = Quad.freeCorners(") in 0 until screen.indexOf("spot = if (SurveyPrep.isSpot(e.t)) SurveyPrep.SPOT / 2 / sq.mpp * k() else null)"))
        assertEquals(1, count(screen, "nodes, SurveyChain.NODE_HIT_DP * density.density, body = bodyGrab(sx, sy) != null, chip = shown.chip?.tap)"))
        // A CORNER'S GRAB: Quad.drag with a Line end's snapping distance, the guide through the corner, gated like an end --
        // and the guide goes off however the gesture ends. A refused drag (null) says nothing.
        assertEquals(1, count(screen, "val ci = Quad.cornerOf(part)"))
        assertEquals(1, count(screen, "Quad.drag(e, start, ci, sq.mmX(jx) - c0[0], sq.mmY(jy) - c0[1], ab[0], ab[1], tol)?.let { went -> onEndGuideS(Quad.guide(s, went)) }"))
        assertEquals(1, count(screen, "cancel = { onEndGuideS(null) }, gate = true) { jx, jy ->"))
        assertEquals(1, count(screen, "if (g.locked == null && Quad.cornerOf(pt) != null) cornerGrabbed = true")); assertEquals(1, count(screen, "if (cornerGrabbed) onEndGuideS(null)"))
        assertEquals(1, count(screen, "Quad.drag("))
        // FINE MOVE: told whether the corners are free; a refused corner nudge says its line
        assertEquals(1, count(screen, "free = Quad.freeIn(prep, mid, freeId, pick.picked, last))"))
        assertEquals(1, count(screen, "val r = NudgePad.press(prep, st, key, a[0], a[1]) { snapshot() }\n        pad = r.state\n        r.said?.let { m -> say(m(tx)) }"))

        // THE PAINTER: which handles is Quad.handles'; the chip keeps clear of the free corners; the outline, the gold frame
        // and the hole go round the shape (Quad.of / corners / grown); the plates are handed the one free
        assertEquals(1, count(draw, "val handles = Quad.handles(e, sel && o.mode == \"main\" && !locked && o.tabs, o.free(e))"))
        assertEquals(1, count(draw, "if (handles == Quad.Handles.CORNERS) {")); assertEquals(1, count(draw, "if (handles == Quad.Handles.TABS) {"))
        assertEquals(1, count(draw, "val solid = NudgePad.solid(o.nudge, e.id, Quad.PARTS[i])"), "the corner Fine move nudges is solid")
        assertEquals(1, count(draw, "val r = SurveyGuide.END_R_DP.toFloat() * o.dp"), "round, a Line's end's size")
        assertEquals(1, count(draw, "val hand = Quad.handles(sm, o.tabs && !SurveyPrep.isLocked(sm), isFree) != Quad.Handles.NONE"))
        assertEquals(1, count(draw, "SurveyPrepTouch.codeChip(bb, SurveyPrepTouch.handlesOf(bb, spot, freeAt), pen(chipFs, true).measureText(chipSays).toDouble(),")); assertEquals(1, count(draw, "handlesOf("))
        assertEquals(1, count(draw, "if (Quad.of(e) != null) Quad.corners(e).map { q -> m(sq.x(q[0]), sq.y(q[1])) } else null"), "a mark with no shape is drawn by drawRect, as it always was")
        assertEquals(1, count(draw, "Quad.of("))
        assertEquals(1, count(draw, "Quad.grown(")); assertEquals(1, count(draw, "val frame = polyPath(grown(shape, gap))")); assertEquals(1, count(draw, "if (holeShape != null) poly(grown(holeShape, pad))"))
        assertEquals(1, count(draw, "if (life == MarkLife.DROP) c.drawLine(shape[0][0], shape[0][1], shape[2][0], shape[2][1], slash)"), "a Dropped one's stroke runs q0 to q2")
        assertEquals(1, count(draw, "PhotoFigures.layout(tx, o.prep, s, o.a, o.b, pk, toPx, du, dp, textW, o.live, o.ready, free = o.free, ask = o.ask)")); assertEquals(0, count(draw, "DimLayout.mark("))
        assertEquals(1, count(draw, "val free: (SurveyPrep.Mark) -> Boolean = { false },"), "named and defaulted: a saved picture frees none")
        // THE SAVED PICTURES' OLD PAINTER IS NOT THIS SLICE'S (decision 5): it names no Quad and no free
        val dims = draw.substring(draw.indexOf("    private fun dims(c: Canvas, s: String, e: SurveyPrep.Mark, o: Marks,"))
        assertFalse(dims.contains("Quad.")); assertFalse(dims.contains("free"))
        assertFalse(appCode("FaceWriter.kt").contains("Quad.")); assertFalse(appCode("AsIsUpload.kt").contains("Quad."))

        // THE 360: the same free for the corners a press takes and the handles the painter draws
        assertEquals(2, count(p3, "Pano360.tabsOf(e, free(e))")); assertEquals(2, count(p3, "tabsOf("))
        assertEquals(1, count(p3, "free = { e -> free(e) }), ink, size.width, size.height)")); assertEquals(1, count(p3, "val free = st.free"))
        assertEquals(1, count(p3, "val freeIdS by rememberUpdatedState(freeId)"))
        assertEquals(0, count(screen, "keepOfMark(")); assertEquals(0, count(p3, "trashOf("))

        // NO :app FILE WRITES A MARK'S `extra`, AND EVERY Quad FUNCTION :app NAMES IS ONE OF THESE: what it reads to draw
        // and press, and the one write -- a corner dragged, Quad.drag. The panel's Rectangle is MarkSheet.shapeStep's
        // (Quad.clear, in :pano); a nudge is NudgePad's. A new name here is a rule that left :pano.
        val named = sortedSetOf<String>()
        for (name in appDir.list()!!.filter { it.endsWith(".kt") }) {
            val text = appCode(name)
            assertEquals(0, count(text, ".extra ="), "$name writes a mark's extra")
            for (word in listOf("shapeRow", "Quad.KEY", "\"quad\"")) assertEquals(0, count(text, word), "$name: $word")
            Regex("""\bQuad\.(\w+)""").findAll(text).forEach { named += it.groupValues[1] }
            if (name !in setOf("SurveyPrepScreen.kt", "SurveyPrepDraw.kt", "SurveyPrep360.kt")) assertFalse(text.contains("Quad."), "$name names Quad")
        }
        assertEquals(sortedSetOf("Handles", "PARTS", "cornerOf", "corners", "drag", "freeCorners", "freeIn", "freeOf", "freed", "grown", "guide", "handles", "of"), named)
    }

    // ---------------------------------------------------------------- the design names what pins Q2's lines

    /**
     * MOVED IN SLICE Q2, as it said it would be ("It goes with the test above, in slice Q2"). Until Q2 this test listed the
     * lines of `:app` that Q2 was going to REWRITE and held DESIGN-quad section 10's note 6 to naming every `:pano` test
     * that pinned one -- the review of the merge (16 Nov 2026, finding 2) had found the plan's list two tests long while
     * six tests pinned those lines. Those lines are rewritten now, so that list is of a commit that is gone. The rule it
     * kept is kept the other way round: THE LINES Q2 WROTE are listed here as `:app` has them, and DESIGN-quad section
     * 12.5 has to name every test that pins one -- so the next slice that rewrites one (M3's saved pictures, I3's tool
     * row) reads there which tests move with it, and a later test that pins one turns this red until 12.5 names it.
     */
    @Test fun `DESIGN-quad section 12 names every test that pins a line of app slice Q2 wrote`() {
        val tests = File("src/test/kotlin/com/malletcrafts/interiorestimate/pano")
        val doc = File("../docs/port63/DESIGN-quad.md").also { assertTrue(it.isFile, "DESIGN-quad.md is where this test reads it") }.readText()
        val from = doc.indexOf("\n### 12.5 ")
        assertTrue(from >= 0, "DESIGN-quad section 12 has no 12.5 naming the source pins on the lines Q2 wrote")
        val to = doc.indexOf("\n### ", from + 1).let { if (it < 0) doc.length else it }
        val note = doc.substring(from, to)
        val lines = mapOf(
            "SurveyPrepScreen.kt" to listOf(
                "MarkSheet.panel(tx, prep, pn, quoteStage, asPicked, shape = true)",
                "MarkSheet.actStep(tx, prep, pn, act, quoteStage, asPicked, shape = true)",
                "freeId = Quad.freed(freeId, pick.picked, part)",
                "free = Quad.freeIn(prep, mid, freeId, pick.picked, last))",
                "corners = Quad.freeCorners(e, freeS(e))",
                "MarkSheet.shapeStep(tx, prep, pn.id, step.act, quoteStage, freeId, part) { snapshot() }"),
            "SurveyPrepDraw.kt" to listOf(
                "SurveyPrepTouch.handlesOf(bb, spot, freeAt)",
                "Quad.handles(e, sel && o.mode == \"main\" && !locked && o.tabs, o.free(e))",
                "PhotoFigures.layout(tx, o.prep, s, o.a, o.b, pk, toPx, du, dp, textW, o.live, o.ready, free = o.free, ask = o.ask)"),
            "SurveyPrep360.kt" to listOf("Pano360.tabsOf(e, free(e))"))
        val sources = tests.listFiles()!!.filter { it.name.endsWith("Test.kt") }.associate { it.name.removeSuffix(".kt") to it.readText() }
        assertTrue(sources.size > 90, "the tests are where this test reads them")
        val named = mutableSetOf<String>()
        for ((file, ls) in lines) for (line in ls) {
            assertTrue(File(appDir, file).readText().contains(line), "$file no longer has `$line`: this list is of another commit")
            // (a line of Kotlin held in a test is written with its quotes escaped: both spellings are looked for)
            val pinning = sources.filterValues { it.contains(line) || it.contains(line.replace("\"", "\\\"")) }.keys
            for (t in pinning) { assertTrue(note.contains("`$t`"), "$t pins `$line`, which slice Q2 wrote, and DESIGN-quad 12.5 does not name it"); named += t }
        }
        assertEquals(setOf("DeleteGuardTest", "FeaturesTest", "MarkSheetTest", "MeasureScreenTest", "NotePinTest", "PhaseToolsTest", "QuadMeetsF1Test", "QuadScreenTest"), named)
    }
}
