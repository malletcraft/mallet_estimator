package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.MeasureBarLayout.Part
import com.malletcrafts.interiorestimate.pano.i18n.I18n
import com.malletcrafts.interiorestimate.pano.i18n.LiteralScan
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * D2 CANNOT BE FIRED BY ACCIDENT (port 63 slice F1, 16 Nov 2026). Amit, the same evening, with build 0.3.337 -- the
 * first with the measure bar -- on his phone: "also navigation button and D2 aer very close where d2 get accidentally
 * fired."
 *
 * The bar's order, widths and gaps are MeasureBarLayout's, pure; they are held here, with the width the two lines are
 * left in portrait and in landscape. SurveyMeasure.kt is compiled only by CI, so that it lays the bar out from these
 * slots and from nothing else is read from its source.
 *
 * Test names are plain ASCII (see MeasureScreenTest).
 */
class MeasureBarLayoutTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0
    private fun ab(s: String): DoubleArray = SurveyPrep.size(s, L, W, H)

    /** A front wall with a door and a Line, nothing read. */
    private fun wall(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        p.surface("front").marks.add(SurveyPrep.Mark("d1", "D", "FD1", 300.0, 1200.0, 0.0, 2100.0))
        p.addMeas("front", SurveyMeas.LINE, listOf(doubleArrayOf(500.0, 1600.0), doubleArrayOf(1300.0, 2200.0)), "m1")
        return p
    }

    private fun onFace(p: SurveyPrep.Prep, picked: String?, active: String?, stage: Int? = 0) = MeasureFlow.onFace(Tx.EN, p, "front", picked, active, stage) { ab(it) }
    private fun parts(bar: MeasureFlow.Bar) = MeasureBarLayout.parts(bar)

    private val whole = listOf(Part.CELL, Part.PREV, Part.NEXT, Part.LINES, Part.BOX, Part.D2)

    @Test fun `the bar is label, the two arrows together, the two lines, the figure box, and D2 alone at the right end`() {
        assertEquals(listOf(Part.CELL, Part.PREV, Part.NEXT, Part.LINES, Part.BOX, Part.D2, Part.NEXT_FACE), MeasureBarLayout.ORDER)
        // 3 - Measure with a dimension being measured: every part but the next-face button, in that order
        val bar = onFace(wall(), "d1", "FD1.w")
        assertEquals(MeasureFlow.Kind.ACTIVE, bar.kind)
        assertEquals(whole, parts(bar))
        // M2's order was label, PREV, the lines, the box, D2, NEXT: D2 between the box and the arrow pressed most
        val i = whole.indexOf(Part.D2)
        assertEquals(whole.size - 1, i, "D2 is the last: nothing stands on its right")
        assertEquals(Part.BOX, whole[i - 1], "its one neighbour is the figure box")
        assertEquals(1, whole.indexOf(Part.NEXT) - whole.indexOf(Part.PREV), "the two arrows are side by side")
        assertTrue(whole.indexOf(Part.NEXT) < whole.indexOf(Part.LINES), "and both on the left of the words")
        // every state of 3 - Measure's bar: D2 is never beside an arrow, in any of them
        val p = wall()
        val states = listOf(onFace(p, "d1", "FD1.w"), onFace(p, null, null), onFace(p, "d1", null), onFace(p, "d1", "FD1.w", 2), onFace(SurveyPrep.fresh(L, W, H), null, null))
        assertEquals(listOf(MeasureFlow.Kind.ACTIVE, MeasureFlow.Kind.IDLE, MeasureFlow.Kind.IDLE, MeasureFlow.Kind.READ_ONLY, MeasureFlow.Kind.EMPTY), states.map { it.kind })
        for (b in states) {
            val ps = parts(b)
            assertEquals(ps, MeasureBarLayout.ORDER.filter { it in ps }, "${b.kind}: always in the one order")
            assertEquals(1, ps.count { it == Part.LINES }, "${b.kind}: the two lines are always there")
            val at = ps.indexOf(Part.D2)
            if (at >= 0) for (n in listOfNotNull(ps.getOrNull(at - 1), ps.getOrNull(at + 1)))
                assertFalse(n == Part.PREV || n == Part.NEXT, "${b.kind}: D2 beside $n")
            assertEquals(b.d2, Part.D2 in ps); assertEquals(b.prev, Part.PREV in ps); assertEquals(b.next, Part.NEXT in ps)
            assertEquals(b.cell != null, Part.CELL in ps); assertEquals(b.figure != null, Part.BOX in ps)
        }
        assertEquals(listOf(Part.PREV, Part.NEXT, Part.LINES), parts(onFace(p, null, null)), "nothing picked, something left: the arrows and the progress")
        assertEquals(listOf(Part.CELL, Part.LINES), parts(onFace(p, "d1", "FD1.w", 2)), "after Survey: the label and the refusal")
        assertEquals(listOf(Part.LINES), parts(onFace(SurveyPrep.fresh(L, W, H), null, null)))
    }

    @Test fun `D2 is larger, and 12 dp of bar that takes no tap lies between it and anything that does`() {
        assertEquals(48, MeasureBarLayout.D2_DP); assertTrue(MeasureBarLayout.D2_H_DP >= 36)
        assertTrue(MeasureBarLayout.D2_DP > 40 && MeasureBarLayout.D2_H_DP > 32, "M2's D2 was 40 x 32")
        assertTrue(MeasureBarLayout.D2_CLEAR_DP >= 12)
        // the gaps: none between the two arrows, 12 on either side of D2, 4 anywhere else, none before the first part
        assertEquals(0, MeasureBarLayout.gapBefore(null, Part.CELL)); assertEquals(0, MeasureBarLayout.gapBefore(null, Part.D2))
        assertEquals(0, MeasureBarLayout.gapBefore(Part.PREV, Part.NEXT))
        for (before in Part.values()) if (before != Part.D2) assertEquals(MeasureBarLayout.D2_CLEAR_DP, MeasureBarLayout.gapBefore(before, Part.D2), "$before, then D2")
        for (after in Part.values()) if (after != Part.D2) assertEquals(MeasureBarLayout.D2_CLEAR_DP, MeasureBarLayout.gapBefore(Part.D2, after), "D2, then $after")
        assertEquals(4, MeasureBarLayout.gapBefore(Part.CELL, Part.PREV)); assertEquals(4, MeasureBarLayout.gapBefore(Part.NEXT, Part.LINES))
        assertEquals(4, MeasureBarLayout.gapBefore(Part.LINES, Part.BOX)); assertEquals(4, MeasureBarLayout.gapBefore(Part.LINES, Part.NEXT_FACE))
        // the slots as laid out: each part's own width, the dead bar before it
        val sl = MeasureBarLayout.slotsOf(whole)
        assertEquals(listOf(52, 36, 36, 0, 64, 48), sl.map { it.widthDp })
        assertEquals(listOf(0, 4, 0, 4, 4, 12), sl.map { it.gapDp })
        assertEquals(whole, sl.map { it.part })
        // HOW FAR EVERY PART THAT TAKES A PRESS IS FROM D2: the box 12 dp; the arrows the whole of the box and the words
        // further (the words' own width, never under nothing, is left out)
        val clear = MeasureBarLayout.clearOfD2(whole)
        assertEquals(setOf(Part.CELL, Part.PREV, Part.NEXT, Part.BOX), clear.keys, "the two lines take no press")
        assertEquals(12, clear.getValue(Part.BOX))
        assertEquals(12 + 64 + 4 + 4, clear.getValue(Part.NEXT)); assertEquals(12 + 64 + 4 + 4 + 36, clear.getValue(Part.PREV))
        assertTrue(clear.values.all { it >= 12 }, "$clear")
        assertTrue(clear.getValue(Part.NEXT) >= 80, "the arrow pressed after every skipped reading is 84 dp and the words away; in M2 it was 4")
        // in every bar the screen can draw -- with and without the label, with one key and so no arrows, with no figure
        for (ps in listOf(whole, whole - Part.CELL, listOf(Part.LINES, Part.BOX, Part.D2), listOf(Part.CELL, Part.LINES, Part.BOX, Part.D2),
            listOf(Part.CELL, Part.PREV, Part.NEXT, Part.LINES, Part.D2)))
            assertTrue(MeasureBarLayout.clearOfD2(ps).values.all { it >= 12 }, "$ps: ${MeasureBarLayout.clearOfD2(ps)}")
        assertTrue(MeasureBarLayout.clearOfD2(listOf(Part.PREV, Part.NEXT, Part.LINES)).isEmpty(), "no D2, nothing to keep off")
        assertFalse(MeasureBarLayout.takesPress(Part.LINES)); assertTrue(Part.values().filter { it != Part.LINES }.all { MeasureBarLayout.takesPress(it) })
        // UNDER AN OPEN PAD: the pad's first row -- its backspace is right under D2 -- starts 6 dp below the bar, so D2's
        // lower edge is 12 dp and more from it in both orientations, and the bar with its pad still fits landscape's column
        assertEquals(6, MeasureBarLayout.PAD_GAP_DP)
        assertEquals(16, MeasureBarLayout.d2ClearOfPadDp(false)); assertEquals(12, MeasureBarLayout.d2ClearOfPadDp(true))
        assertTrue(MeasureBarLayout.D2_H_DP <= StepRoute.measureBarDp(true), "D2 fits the landscape bar")
        val column = StepRoute.LAND_TOPBAR_DP + StepRoute.budgetDp(true, "front") + StepRoute.measureBarDp(true) +
            MeasureBarLayout.PAD_GAP_DP + StepRoute.PAD_ROWS * StepRoute.padRowDp(true)
        assertEquals(310, column); assertTrue(column + 4 <= StepRoute.LAND_COLUMN_DP, "with 3 - Measure's 4 dp over the bar")
    }

    @Test fun `no part that takes a press is under 36 dp wide - the arrows keep what M2 gave them, side by side`() {
        // THE F1 REVIEW, finding 2: F1 as built paid for D2's distance out of the arrows, 36 -> 32 dp each. An arrow is
        // pressed after every reading he skips; a narrower button beside another is not how a button stops being fired
        // by accident. The words pay instead.
        assertEquals(36, MeasureBarLayout.TOUCH_MIN_DP)
        for (part in Part.values()) if (MeasureBarLayout.takesPress(part))
            assertTrue(MeasureBarLayout.widthOf(part) >= MeasureBarLayout.TOUCH_MIN_DP, "$part is ${MeasureBarLayout.widthOf(part)} dp wide")
        assertEquals(36, MeasureBarLayout.ARROW_DP); assertEquals(MeasureBarLayout.widthOf(Part.PREV), MeasureBarLayout.widthOf(Part.NEXT))
        // the pair: nothing between them -- every dp of the 72 is one arrow's or the other's, and 4 dp of bar either side
        assertEquals(0, MeasureBarLayout.gapBefore(Part.PREV, Part.NEXT))
        val sl = MeasureBarLayout.slotsOf(whole)
        val prev = sl.first { it.part == Part.PREV }; val next = sl.first { it.part == Part.NEXT }
        assertEquals(72, prev.widthDp + next.gapDp + next.widthDp)
        assertEquals(4, prev.gapDp); assertEquals(4, sl[sl.indexOf(next) + 1].gapDp)
        // the two arrows' middles are 36 dp apart: what M2's one arrow was wide
        assertEquals(36, prev.widthDp / 2 + next.gapDp + next.widthDp / 2)
        // and D2 is no nearer for it: still 84 dp and the words from the arrow pressed most
        assertEquals(84, MeasureBarLayout.clearOfD2(whole).getValue(Part.NEXT))
    }

    @Test fun `the width budget - the two lines get 68 dp in portrait and 46 in landscape under the whole bar`() {
        assertEquals(340, MeasureBarLayout.barDp(false)); assertEquals(318, MeasureBarLayout.barDp(true))
        assertEquals(6, MeasureBarLayout.PAD_DP)
        // 3 - Measure, a dimension being measured: label 52, the arrows 36 + 36, the box 64, D2 48, gaps 4 + 0 + 4 + 4 + 12
        assertEquals(68, MeasureBarLayout.linesDp(whole, 340)); assertEquals(46, MeasureBarLayout.linesDp(whole, 318))
        assertEquals(340 - 12 - (52 + 36 + 36 + 64 + 48) - (4 + 0 + 4 + 4 + 12), MeasureBarLayout.linesDp(whole, MeasureBarLayout.barDp(false)))
        // M2's bar left them 80 and 58: label 52, an arrow 36, the box 64, D2 40, an arrow 36, five gaps of 4
        assertEquals(80, 340 - 12 - (52 + 36 + 64 + 40 + 36) - 5 * 4); assertEquals(58, 318 - 12 - (52 + 36 + 64 + 40 + 36) - 5 * 4)
        // so D2's 8 dp more and its 8 dp wider gap, less the one gap saved, cost the words 12 dp -- and the arrows nothing
        assertEquals(12, 80 - MeasureBarLayout.linesDp(whole, 340)); assertEquals(12, 58 - MeasureBarLayout.linesDp(whole, 318))
        // 2 - Mark and 4 - 360 with the pad shut draw no label: 124 and 102; a Line there has one key and no arrows: 200 and 178
        assertEquals(124, MeasureBarLayout.linesDp(whole - Part.CELL, 340)); assertEquals(102, MeasureBarLayout.linesDp(whole - Part.CELL, 318))
        assertEquals(200, MeasureBarLayout.linesDp(listOf(Part.LINES, Part.BOX, Part.D2), 340)); assertEquals(178, MeasureBarLayout.linesDp(listOf(Part.LINES, Part.BOX, Part.D2), 318))
        // a finished face: label, the arrow back, the line and the next face's button at the right end, where it always was
        val done = listOf(Part.CELL, Part.PREV, Part.LINES, Part.NEXT_FACE)
        assertEquals(Part.NEXT_FACE, MeasureBarLayout.ORDER.last())
        assertEquals(112, MeasureBarLayout.widthOf(Part.NEXT_FACE))
        assertEquals(340 - 12 - (52 + 36 + 112) - 3 * 4, MeasureBarLayout.linesDp(done, 340))
        assertTrue(MeasureBarLayout.linesDp(done, 318) >= 90, "its one line keeps what M2 gave it: ${MeasureBarLayout.linesDp(done, 318)} dp")
        // nothing the screen can draw leaves the words less than 46 dp
        for (land in listOf(false, true)) for (ps in listOf(whole, whole - Part.CELL, done, listOf(Part.CELL, Part.LINES), listOf(Part.PREV, Part.NEXT, Part.LINES)))
            assertTrue(MeasureBarLayout.linesDp(ps, MeasureBarLayout.barDp(land)) >= 46, "$ps, landscape $land")
    }

    @Test fun `a finished face keeps its next-face button at the right end, and draws no D2`() {
        // every dimension of the front wall read, something left on the right wall
        val p = wall()
        p.surface("right").marks.add(SurveyPrep.Mark("w2", "W", "RW1", 500.0, 1400.0, 900.0, 2100.0))
        for (k in MeasureWalk.stops(p, "front", ab("front")[0], ab("front")[1])) p.actual[k] = 1000.0
        val bar = onFace(p, null, null)
        assertEquals(MeasureFlow.Kind.DONE, bar.kind)
        val ps = parts(bar)
        assertEquals(listOf(Part.PREV, Part.LINES, Part.NEXT_FACE), ps)
        assertEquals(Part.NEXT_FACE, ps.last()); assertFalse(Part.D2 in ps); assertFalse(Part.BOX in ps)
        assertNull(bar.figure)
    }

    // ---- the screen, read as source (only CI compiles :app) ---------------------------------------------------

    private val barSrc = "app/src/main/kotlin/com/malletcrafts/interiorestimate/SurveyMeasure.kt"
    private fun code(rel: String): String { val f = I18n.android.resolve(rel); assertTrue(f.isFile, rel); return LiteralScan.scan(I18n.android, rel).masked }
    private fun count(hay: String, needle: String): Int = Regex.fromLiteral(needle).findAll(hay).count()

    @Test fun `SurveyMeasure lays the bar out from the slots alone - no order, width or gap of its own`() {
        val b = code(barSrc)
        assertEquals(1, count(b, "for (slot in MeasureBarLayout.slots(bar)) {"), "the bar is drawn slot by slot")
        assertEquals(1, count(b, "if (slot.gapDp > 0) Spacer(Modifier.width(slot.gapDp.dp))"), "the dead bar is a Spacer: it takes no tap")
        assertEquals(1, count(b, "when (slot.part) {"))
        for (part in Part.values()) assertEquals(1, count(b, "MeasureBarLayout.Part.${part.name} -> "), "${part.name} is drawn in one place")
        // every part that has a width takes the slot's; the two lines take what is left
        assertEquals(4, count(b, "Modifier.width(slot.widthDp.dp)"), "the label, the box, D2 and the next-face button")
        assertEquals(2, count(b, "slot.widthDp, on"), "and the two arrows")
        assertEquals(1, count(b, "MeasureBarLayout.Part.LINES -> Column(Modifier.weight(1f)) {"))
        assertEquals(1, count(b, "Box(Modifier.width(widthDp.dp).fillMaxHeight().clickable(role = Role.Button) { onClick() }"), "an arrow is as wide as its slot and as tall as the bar")
        assertEquals(1, count(b, ".height(MeasureBarLayout.D2_H_DP.dp)")); assertEquals(1, count(b, ".height(MeasureBarLayout.BOX_H_DP.dp)"))
        assertEquals(1, count(b, ".padding(horizontal = MeasureBarLayout.PAD_DP.dp)"))
        // the bar's row spaces nothing by itself, and holds none of M2's numbers
        val row = b.substring(b.indexOf("Row(modifier.fillMaxWidth().height(StepRoute.measureBarDp(land).dp)"), b.indexOf("for (slot in MeasureBarLayout.slots(bar)) {"))
        assertEquals(0, count(row, "spacedBy"), "the bar's own row adds no gap: every gap is a slot's")
        for (old in listOf("width(52.dp)", "width(36.dp)", "width(64.dp)", "width(40.dp)", "width(48.dp)", "width(112.dp)", "height(32.dp)", "height(36.dp)"))
            assertEquals(0, count(b, old), "SurveyMeasure.kt still holds $old")
        // D2 is drawn once, fires the laser and nothing else does; the arrows keep their own callbacks
        assertEquals(1, count(b, "onD2()")); assertEquals(1, count(b, ".clickable(role = Role.Button) { onD2() }"))
        assertTrue(b.indexOf("MeasureBarLayout.Part.D2 -> ") < b.indexOf("onD2()") && b.indexOf("onD2()") < b.indexOf("MeasureBarLayout.Part.NEXT_FACE -> "))
        assertEquals(1, count(b, "tx.sp_measure_aria_previous, slot.widthDp, onPrev)")); assertEquals(1, count(b, "tx.sp_measure_aria_skip, slot.widthDp, onNext)"))
        // the pad: its dead gap first, then its rows; none of its keys fires the laser
        val pad = b.substring(b.indexOf("internal fun SpMeasurePad("))
        assertEquals(1, count(pad, "Spacer(Modifier.height(MeasureBarLayout.PAD_GAP_DP.dp))"))
        assertTrue(pad.indexOf("MeasureBarLayout.PAD_GAP_DP") < pad.indexOf("StepRoute.PAD_ROWS * StepRoute.padRowDp(land)"))
        assertEquals(0, count(pad, "onD2"))
    }
}
