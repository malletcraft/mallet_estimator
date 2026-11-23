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

    // ------------------------------------------------------------------ the keypad (port 63 slices M1, I1)

    /*
     * THE KEYPAD STANDS IN THE COLUMN, in the tool row's place, and only while he types (DESIGN-imagemeter §5). The
     * measure bar that stood over it until slice I1b is gone: nothing under the photo shows a measurement. Its heights
     * are here so the arithmetic of what still fits is tested in this module (app/SurveyMeasure.kt draws it).
     */

    /** One row of the keypad, dp: 44 in portrait, 36 in landscape. */
    fun padRowDp(land: Boolean): Int = if (land) 36 else 44

    /** The pad's rows: 1 2 3 ⌫ / 4 5 6 Clear / 7 8 9 Next / ⌄ 0 (Next, two rows tall). */
    const val PAD_ROWS = 4

    /** Landscape's column of controls, as SurveyPrepScreen lays it out (a ~336 dp window under the status bar, less
     *  its 12 dp of padding): the top bar, dp ... */
    const val LAND_TOPBAR_DP = 32

    /** ... and the column's whole height, dp. The bar, the tabs, the strip and whatever 2 · Mark stands under them --
     *  the tool row, or the keypad in its place -- must fit in it ([landFits]). */
    const val LAND_COLUMN_DP = 324

    // ------------------------------------------------------------------ ONE STEP TO MARK AND MEASURE (port 63 slice I1a)

    /*
     * Amit, 16 Nov 2026, on build 0.3.337: "do we really need two screen now? which is mark and measure? as we should
     * be measure where marking is done." So the steps are 1 · Lines, 2 · Mark, 3 · 360 (DESIGN-imagemeter §0, §2.1,
     * §14 I1): 3 · Measure went in slice I1b, and with it the measure bar and the four-step route.
     */

    /** The three steps' keys. A key is never shown; its name is [routeName]. */
    const val LINES = "lines"
    const val MARK = "mark"
    const val PANO = "360"

    /** THE STEPS OF A FACE, in order: 1 · Lines, 2 · Mark, 3 · 360. Marking and measuring are ONE step. */
    val ROUTE: List<String> = listOf(LINES, MARK, PANO)

    /** A step's name: Lines, Mark (common.*) and 360, which is a number in every language. */
    fun routeName(tx: Tx, key: String): String = when (key) {
        LINES -> tx.common_lines
        MARK -> tx.common_mark
        else -> key // "360"
    }

    /** Where [mode] sits in the three steps (-1: not one of them -- the old "measure" among those). */
    fun routeIndex(mode: String): Int = ROUTE.indexOf(mode)

    /** The step as it is read out and labelled: "2 · Mark", "3 · 360" -- its number in [ROUTE], the separator, its name. */
    fun routeLabel(tx: Tx, i: Int): String = "${i + 1}$SEP${routeName(tx, ROUTE[i])}"

    /**
     * THE STEP A MODE KEY MEANS ON THE THREE-STEP ROUTE: itself when it is one of [ROUTE]; anything else is 1 · Lines,
     * the step every face starts on. Nothing stores a step, so this only ever meets a key a caller holds in memory --
     * but a key that is nobody's must never leave the screen on no step at all.
     */
    fun routeOf(mode: String): String = if (mode in ROUTE) mode else LINES

    /** The palette's row under the photo, dp: 6 over 50 in portrait, 6 over 46 in landscape (as it stands today). */
    fun toolRowDp(land: Boolean): Int = SurveyPrepPalette.TOP_DP + if (land) 46 else 50

    /** The keypad, dp: 6 over its four rows -- 182 in portrait (6 + 4 x 44), 150 in landscape (6 + 4 x 36). It stands
     *  IN THE COLUMN in place of the tool row (DESIGN-imagemeter §2.3 E, §5), with no display line. */
    fun keypadDp(land: Boolean): Int = SurveyPrepPalette.TOP_DP + PAD_ROWS * padRowDp(land)

    /** Portrait's window under the status bar on the Mi A3 (360 x 780 dp), and what stands above the photo in it --
     *  the column's padding 12, the phase band 20, the top bar 40, the tabs and the strip 92, the caption 22 -- so
     *  the photo and whatever is under it share 570 dp (DESIGN-imagemeter §2.1, §2.4). */
    const val PORTRAIT_WINDOW_DP = 756
    const val PORTRAIT_CHROME_DP = 186

    /** What stands under the photo in 2 · Mark: the tool row, the keypad in its place, or nothing. NEVER ANYTHING OF
     *  THE PICKED ITEM (port 63 slice I2b): its row went, so picking a mark changes nothing under the photo. */
    enum class Under {
        /** No tool row at all: Estimation and Client approved, where nothing is placed. */
        NOTHING,
        /** The tool row: the ONE row under the photo, whatever is picked or held. */
        TOOLS,
        /** The keypad, in the tool row's place. */
        KEYPAD,
    }

    /** How much of the column what is [under] the photo takes in 2 · Mark, dp. */
    fun underDp(under: Under, land: Boolean): Int = when (under) {
        Under.NOTHING -> 0
        Under.TOOLS -> toolRowDp(land)
        Under.KEYPAD -> keypadDp(land)
    }

    /**
     * WHAT 2 · MARK STANDS UNDER THE PHOTO (DESIGN-imagemeter §2.3, §14 I2): the keypad while it is open, in the tool
     * row's place; else the tool row; and where the phase has no tool row ([toolRow] false: Estimation and Client
     * approved, [PhaseTools.can] of the palette) nothing at all -- but for a keypad, which is never open there. WHAT IS
     * PICKED OR HELD DOES NOT ENTER IT: the picked item's row stood here for the one build I1 and is gone (slice I2b) --
     * its code is a chip on the photo, and its padlock, its nudges and its Delete are the panel's acts. NOTHING HERE
     * SHOWS A MEASUREMENT: no bar, no figure box, no number.
     */
    fun underMark(toolRow: Boolean, keypad: Boolean): Under = when {
        keypad -> Under.KEYPAD
        toolRow -> Under.TOOLS
        else -> Under.NOTHING
    }

    /** The photo's height in 2 · Mark in PORTRAIT on the Mi A3, dp, with [under] under it: 514 with the tool row --
     *  picked, held or not -- 388 with the keypad up, 570 with no row (DESIGN-imagemeter §2.4, "After I2"). In
     *  landscape every row lives in the right column and the photo keeps its height: [landFits] is the question there. */
    fun markPhotoDp(under: Under): Int = PORTRAIT_WINDOW_DP - PORTRAIT_CHROME_DP - underDp(under, false)

    /** Landscape's column under its top bar, the tabs and the strip: 212 dp for whatever 2 · Mark stands there. */
    fun landUnderDp(): Int = LAND_COLUMN_DP - LAND_TOPBAR_DP - budgetDp(true, "front")

    /** Whether what is [under] the photo fits landscape's column ([landUnderDp]). */
    fun landFits(under: Under): Boolean = underDp(under, true) <= landUnderDp()
}
