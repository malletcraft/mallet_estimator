package com.malletcrafts.interiorestimate.pano

/**
 * WHERE EACH PART OF THE MEASURE BAR STANDS (port 63 slice F1, 16 Nov 2026). Amit, the same evening, with build
 * 0.3.337 -- the first with the measure bar -- on his phone: "also navigation button and D2 aer very close where d2
 * get accidentally fired."
 *
 * The bar M2 shipped was `label · ‹ · two lines · figure box · D2 · ›`, 4 dp apart: D2, a 40 x 32 dp button that FIRES
 * THE LASER, stood between the figure box and ›, which is pressed after every reading he skips. A thumb aimed at › was
 * 4 dp from a shot he had not asked for -- and a shot that lands is written.
 *
 * So the order is `label · ‹ › · two lines · figure box · D2`:
 *  - ‹ and › are a PAIR on the left, side by side ([ARROW_DP] each, nothing between them): the two ways to walk;
 *  - D2 is ALONE AT THE RIGHT END, larger ([D2_DP] x [D2_H_DP]), with [D2_CLEAR_DP] of bar that takes no tap between
 *    it and the figure box -- its only neighbour in the bar; past it there is only the bar's own edge;
 *  - under an open pad the pad's first row starts [PAD_GAP_DP] below the bar, so its ⌫ is never nearer D2 than
 *    [D2_CLEAR_DP] either ([d2ClearOfPadDp]);
 *  - a finished face's "Next: Right wall · 4 to measure ▸" keeps the place it had, the right end ([NEXT_FACE_DP]);
 *    no D2 is drawn beside it -- nothing is being measured then.
 *
 * WHAT THE TWO LINES GET ([linesDp]): the bar is as wide as its column, [PORTRAIT_DP] in portrait (a 360 dp screen
 * less the screen's 10 dp either side) and [LAND_DP] in landscape (the column of controls), less [PAD_DP] either
 * side. With the whole bar -- label, both arrows, the box and D2 -- that is 76 dp in portrait and 54 in landscape
 * (M2: 80 and 58); where 2 · Mark's bar draws no label, 132 and 110. D2's 8 dp more and its 8 dp wider gap are paid
 * for mostly by the arrows -- 36 dp each in M2, 32 now, and one 4 dp gap fewer since they stand together -- so the
 * two lines give up 4 dp, and line 1 is cut with an ellipsis where it must, as before.
 *
 * Pure data: SurveyMeasure.kt lays the bar out from [slots] and nothing else, so what is tested here -- the order,
 * every gap, and that D2 is never beside ‹ or › -- is what CI compiles onto the phone.
 */
object MeasureBarLayout {

    /** One part of the bar. Every one takes a press but [LINES], which is words. */
    enum class Part {
        /** The label cell: the picked item's code over its type; a tap opens its Mark sheet. */
        CELL,
        /** ‹: the measurement before. */
        PREV,
        /** ›: skip to the next one. */
        NEXT,
        /** The measure's own words over the progress: no press. It takes the width the others leave. */
        LINES,
        /** The figure box: a tap opens the pad. */
        BOX,
        /** D2: the laser fires. */
        D2,
        /** A finished face's button: the next face with something left. */
        NEXT_FACE,
    }

    /** The bar's order, left to right. A part the bar does not have ([parts]) leaves no gap. */
    val ORDER: List<Part> = listOf(Part.CELL, Part.PREV, Part.NEXT, Part.LINES, Part.BOX, Part.D2, Part.NEXT_FACE)

    /** The bar's own padding, left and right, dp. */
    const val PAD_DP = 6
    /** The label cell, dp. */
    const val CELL_DP = 52
    /** ‹ and ›, each, dp wide; they are as tall as the bar. */
    const val ARROW_DP = 32
    /** The figure box, dp wide and tall. */
    const val BOX_DP = 64
    const val BOX_H_DP = 32
    /** D2, dp wide and tall: larger than M2's 40 x 32. */
    const val D2_DP = 48
    const val D2_H_DP = 36
    /** A finished face's next-face button, dp. */
    const val NEXT_FACE_DP = 112
    /** Between two parts, dp ... */
    const val GAP_DP = 4
    /** ... but between D2 and whatever stands beside it: bar that takes no tap, dp. */
    const val D2_CLEAR_DP = 12
    /** Between the bar and the first row of an open pad, dp: dead, so D2 is [D2_CLEAR_DP] clear of the pad's ⌫ too. */
    const val PAD_GAP_DP = 6

    /** The bar's whole width, dp: portrait (360 less the screen's 10 dp either side), and landscape's column of controls. */
    const val PORTRAIT_DP = 340
    const val LAND_DP = 318

    /** One part as laid out: [gapDp] of bar that takes no tap, then the part, [widthDp] wide (0: [Part.LINES], which
     *  takes what is left). */
    class Slot(val part: Part, val widthDp: Int, val gapDp: Int)

    /** A part's own width, dp; 0 for the two lines. */
    fun widthOf(part: Part): Int = when (part) {
        Part.CELL -> CELL_DP
        Part.PREV, Part.NEXT -> ARROW_DP
        Part.LINES -> 0
        Part.BOX -> BOX_DP
        Part.D2 -> D2_DP
        Part.NEXT_FACE -> NEXT_FACE_DP
    }

    /** Whether a press on [part] does anything. */
    fun takesPress(part: Part): Boolean = part != Part.LINES

    /** The parts [bar] draws, in [ORDER]: the two lines always; the rest as [MeasureFlow.Bar] says. */
    fun parts(bar: MeasureFlow.Bar): List<Part> = ORDER.filter { p ->
        when (p) {
            Part.CELL -> bar.cell != null
            Part.PREV -> bar.prev
            Part.NEXT -> bar.next
            Part.LINES -> true
            Part.BOX -> bar.figure != null
            Part.D2 -> bar.d2
            Part.NEXT_FACE -> bar.nextFaceText != null
        }
    }

    /** The dead bar before [part] when [before] stands on its left (null: it is the first): none between ‹ and ›,
     *  [D2_CLEAR_DP] on either side of D2, [GAP_DP] anywhere else. */
    fun gapBefore(before: Part?, part: Part): Int = when {
        before == null -> 0
        part == Part.D2 || before == Part.D2 -> D2_CLEAR_DP
        before == Part.PREV && part == Part.NEXT -> 0
        else -> GAP_DP
    }

    /** [bar] as laid out, left to right: what SurveyMeasure.kt draws, slot by slot. */
    fun slots(bar: MeasureFlow.Bar): List<Slot> = slotsOf(parts(bar))

    /** The same for any list of parts in [ORDER]. */
    fun slotsOf(parts: List<Part>): List<Slot> = parts.mapIndexed { i, p -> Slot(p, widthOf(p), gapBefore(parts.getOrNull(i - 1), p)) }

    /** The bar's width, dp, in portrait or in landscape. */
    fun barDp(land: Boolean): Int = if (land) LAND_DP else PORTRAIT_DP

    /** WHAT THE TWO LINES GET, dp, in a bar [barDp] wide drawing [parts]: what the padding, the parts and the gaps leave. */
    fun linesDp(parts: List<Part>, barDp: Int): Int = barDp - 2 * PAD_DP - slotsOf(parts).sumOf { it.widthDp + it.gapDp }

    /**
     * HOW FAR EACH PART THAT TAKES A PRESS STANDS FROM D2, dp, in a bar drawing [parts]: the dead bar and the words
     * between them (the two lines' own width is left out -- at least nothing). Empty when the bar draws no D2.
     */
    fun clearOfD2(parts: List<Part>): Map<Part, Int> {
        val sl = slotsOf(parts); val at = sl.indexOfFirst { it.part == Part.D2 }
        if (at < 0) return emptyMap()
        val out = LinkedHashMap<Part, Int>()
        for (i in sl.indices) if (i != at && takesPress(sl[i].part)) {
            val lo = minOf(i, at); val hi = maxOf(i, at)
            // every gap between the two, and the fixed widths of whatever stands between them
            out[sl[i].part] = (lo + 1..hi).sumOf { sl[it].gapDp } + (lo + 1 until hi).sumOf { sl[it].widthDp }
        }
        return out
    }

    /** How far D2's lower edge is from the first row of an open pad, dp: D2 is centred in the bar, and [PAD_GAP_DP]
     *  of nothing lies between the bar and the pad. */
    fun d2ClearOfPadDp(land: Boolean): Int = (StepRoute.measureBarDp(land) - D2_H_DP) / 2 + PAD_GAP_DP
}
