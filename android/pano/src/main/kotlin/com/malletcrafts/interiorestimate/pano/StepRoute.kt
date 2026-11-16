package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.math.abs
import kotlin.math.max

/**
 * SURVEY PREP'S FACE TABS AND STEP STRIP (v52 slice 6, item 9). Amit, 14 Oct
 * 2026 06:23 IST: "all having same size confuses me. its actually parent child
 * relation for face and 1 2 3 4."; 20:45 IST: "Why all faces amd 1 2 3 4 have
 * same look and feel. i had requested distinction in them."
 *
 * The PARENT, the faces, are text tabs in two rows; the CHILD, the steps of the
 * open face, a strip under them, smaller and of another shape. Everything here
 * is arithmetic and lists -- which tabs, in what order, which steps, how tall,
 * and which step a press belongs to -- so it is tested in this module; the
 * composable only lays it out and dispatches.
 *
 * Coordinates are any ONE space the caller measures both the press and the
 * steps in (the app uses the window root, px); [density] is px per dp.
 */
object StepRoute {

    /** The third step's key: 3 · Measure (port 63 slice M2). A step's key is never shown; its name is [stepName]. */
    const val MEASURE = "measure"

    /** How far below a step's top a press still belongs to it: the APK's minimum touch size. */
    const val REACH_DP = 48.0

    /** A step answers at least this far either side of its centre, however narrow it is drawn. */
    const val HALF_MIN_DP = 24.0

    /** One step of the strip as laid out: its mode key and its bounds. */
    data class Step(val key: String, val left: Double, val top: Double, val right: Double, val bottom: Double)

    /**
     * Which step a press at ([x], [y]) is for, or null when it is nobody's.
     *
     * A step's circle is the drawing; its touch area runs from its top DOWN
     * [REACH_DP] dp over whatever lies under the strip (the caption line, the
     * top of the photo), and horizontally
     * max([HALF_MIN_DP] dp, half its width) either side of its centre -- so a
     * press just under a step changes the step instead of panning, without
     * the row itself growing to 48 dp and costing the photo the difference
     * (which Modifier.minimumInteractiveComponentSize would). A press NEVER
     * routes up: anything above [stripTop] (the face tabs) is not a step's.
     * 70 dp below a step the press is the photo's again. The first step that
     * matches wins; a step not laid out yet (no width) is skipped.
     */
    fun stepFor(x: Double, y: Double, steps: List<Step>, stripTop: Double, density: Double): String? {
        if (y < stripTop) return null
        val reach = REACH_DP * density
        val halfMin = HALF_MIN_DP * density
        for (s in steps) {
            val w = s.right - s.left
            if (w <= 0.0) continue
            val cx = (s.left + s.right) / 2.0
            if (y >= s.top && y < s.top + reach && abs(x - cx) <= max(halfMin, w / 2.0)) return s.key
        }
        return null
    }

    // ------------------------------------------------------------------ what the tabs and strip show

    /**
     * The face tabs, two rows: FRONT RIGHT BACK LEFT / FLOOR CEILING PLAN (port 63 slice M2). The room's LIST tab is
     * gone with the list -- Amit, 15 Nov 2026: "The laundry list of measurements shown below screen is also
     * frustrating." -- so PLAN, the six drawings (slice 13), is the seventh and last. Each row's tabs share its width
     * equally, four in the first and three in the second: every tab is weighted alike in its own row.
     */
    val TAB_ROWS: List<List<String>> = listOf(listOf("front", "right", "back", "left"), listOf("floor", "ceil", "plan"))

    /** A tab that is one of the six faces -- not Plan. Only a face has steps, lines, marks and a clear. */
    fun isFace(t: String): Boolean = t in SurveyPrep.SURFACES

    /** The Plan tab: the six drawings (slice 13). */
    fun isPlan(t: String): Boolean = t == "plan"

    /** A tab's text before it is upper-cased: the face's short name (surface.*.short, through Masters), or Plan. */
    fun tabName(tx: Tx, t: String): String = when (t) {
        "plan" -> tx.common_plan
        else -> SurveyPrep.TAB[t]?.let { Masters.surface_tab(tx, it) } ?: t
    }

    /** The ✓ goes on a face whose four lines are all set; Plan never carries one. */
    fun tabTick(t: String, bounded: Boolean): Boolean = isFace(t) && bounded

    /**
     * The steps of a face, in order: 1 · Lines, 2 · Mark, 3 · Measure, 4 · 360. THE THIRD STEP IS THE PHOTO, NOT A
     * LIST (port 63 slice M2; DESIGN-port63 §1.3): the same squared face with one figure on every dimension and a walk
     * through the unmeasured ones, where nothing can be moved, resized or placed. 4 · 360 joined with slice 17, once
     * it works (Amit, 15 Oct 2026: "Yes, last, after everything else"); until then it was hidden, never greyed -- no
     * dead step, by the no-fallbacks rule.
     */
    val STEPS: List<String> = listOf("lines", "mark", MEASURE, "360")

    /** A step's name: Lines, Mark, Measure (common.*) and 360, which is a number in every language. */
    fun stepName(tx: Tx, key: String): String = when (key) {
        "lines" -> tx.common_lines
        "mark" -> tx.common_mark
        MEASURE -> tx.common_measure
        else -> key // "360"
    }

    /** The step's name as it is read out and labelled: "2 · Mark" -- its number, the separator, its name. */
    fun label(tx: Tx, i: Int): String = "${i + 1}$SEP${stepName(tx, STEPS[i])}"

    /** Where [mode] sits in the strip (-1: not a step shown). */
    fun stepIndex(mode: String): Int = STEPS.indexOf(mode)

    /** The separator in a step's label and after the strip's face. */
    private const val SEP = " · " // text:value the prototype's separator, the same in both languages

    /** "Front wall ·": the face the strip belongs to (surface.*, through Masters). */
    fun stripFace(tx: Tx, tab: String): String = (SurveyPrep.NAME[tab]?.let { Masters.surface(tx, it) } ?: "") + SEP.trimEnd()

    /** The strip is the open face's; the Plan tab has no steps (slice 13). */
    fun showStrip(tab: String): Boolean = isFace(tab)

    /** One row of tabs, dp: 32 in portrait, 28 in landscape -- which holds a face's name on its 17 sp line under मराठी
     *  over a Ref line's 9 sp (the landscape LIST's 24 / 27 dp rows went with the list, port 63 slice M2). */
    fun tabRowDp(land: Boolean): Int = if (land) 28 else 32

    /** The strip, dp: 28 in portrait, 24 in landscape. */
    fun stripDp(land: Boolean): Int = if (land) 24 else 28

    /** The tabs and the strip together, dp: 92 portrait, 80 landscape (v51 took 110 and 82); the Plan tab, no strip:
     *  64 portrait, 56 landscape. */
    fun budgetDp(land: Boolean, tab: String): Int = 2 * tabRowDp(land) + if (showStrip(tab)) stripDp(land) else 0

    // ------------------------------------------------------------------ the measure bar and its pad (port 63 slice M1)

    /*
     * MEASURING HAPPENS ON THE PHOTO (DESIGN-port63 §1.4): under it, one bar -- label · ‹ · two lines · the figure box
     * · D2 · › -- and, only when asked for, the app's own pad of four rows under the bar. Their heights are here so the
     * arithmetic of what still fits is tested in this module; slice M2 draws them (app/SurveyMeasure.kt).
     */

    /** The measure bar, dp: 56 in portrait, 48 in landscape. */
    fun measureBarDp(land: Boolean): Int = if (land) 48 else 56

    /** One row of the pad, dp: 44 in portrait, 36 in landscape. */
    fun padRowDp(land: Boolean): Int = if (land) 36 else 44

    /** The pad's rows: 1 2 3 ⌫ / 4 5 6 Clear / 7 8 9 Next / ⌄ 0 (Next, two rows tall). */
    const val PAD_ROWS = 4

    /** Landscape's column of controls, as SurveyPrepScreen lays it out (a ~336 dp window under the status bar, less
     *  its 12 dp of padding): the top bar, dp ... */
    const val LAND_TOPBAR_DP = 32

    /** ... and the column's whole height, dp. The bar, the tabs, the strip, the measure bar and the open pad must fit
     *  in it (StepRouteTest); the palette and the picked mark's row do NOT fit beside the pad, which is why an open pad
     *  takes their place in 2 · Mark and 4 · 360. */
    const val LAND_COLUMN_DP = 324
}
