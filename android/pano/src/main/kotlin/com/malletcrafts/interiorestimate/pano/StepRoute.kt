package com.malletcrafts.interiorestimate.pano

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
     * top of the photo, the empty part of the Show row), and horizontally
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

    /** The face tabs, two rows: PLAN is the 8th, after List, with the six drawings (slice 13; ct52 X1:
     *  front,right,back,left,floor,ceil,list,plan). */
    val TAB_ROWS: List<List<String>> = listOf(listOf("front", "right", "back", "left"), listOf("floor", "ceil", "list", "plan"))

    /** A tab that is one of the six faces -- not List, not Plan. Only a face has steps, lines, marks and a clear. */
    fun isFace(t: String): Boolean = t in SurveyPrep.SURFACES

    /** The Plan tab: the six drawings (slice 13). */
    fun isPlan(t: String): Boolean = t == "plan"

    /** A tab's text before it is upper-cased: the face's short name, List or Plan. */
    fun tabName(t: String): String = when (t) { "list" -> "List"; "plan" -> "Plan"; else -> SurveyPrep.TAB[t] ?: t }

    /** The ✓ goes on a face whose four lines are all set; List and Plan never carry one. */
    fun tabTick(t: String, bounded: Boolean): Boolean = isFace(t) && bounded

    /**
     * The steps of a face, in order: 1 · Lines, 2 · Mark, 3 · List, 4 · 360. 4 · 360 joined with slice 17, once it
     * works (Amit, 15 Oct 2026: "Yes, last, after everything else"); until then it was hidden, never greyed -- no dead
     * step, by the no-fallbacks rule.
     */
    val STEPS: List<Pair<String, String>> = listOf("lines" to "Lines", "mark" to "Mark", "list" to "List", "360" to "360")

    /** The step's name as it is read out and labelled: "2 · Mark". */
    fun label(i: Int): String = "${i + 1} · ${STEPS[i].second}"

    /** Where [mode] sits in the strip (-1: not a step shown). */
    fun stepIndex(mode: String): Int = STEPS.indexOfFirst { it.first == mode }

    /** "Front wall ·": the face the strip belongs to. */
    fun stripFace(tab: String): String = (SurveyPrep.NAME[tab] ?: "") + " ·"

    /** The strip is the open face's; the room's List tab and the Plan tab have no steps (slice 13). */
    fun showStrip(tab: String): Boolean = isFace(tab)

    /** A list view: the room's List tab, or a FACE's 3 · List -- never the Plan tab, whatever step a face was left on. */
    fun listView(tab: String, mode: String): Boolean = tab == "list" || (isFace(tab) && mode == "list")

    /** One row of tabs, dp: 32 in portrait, 28 in landscape, 24 in a landscape list (so CEILING ✓ is never cut). */
    fun tabRowDp(land: Boolean, list: Boolean): Int = if (land && list) 24 else if (land) 28 else 32

    /** The strip, dp: 28 in portrait, 24 in landscape, 20 in a landscape list. */
    fun stripDp(land: Boolean, list: Boolean): Int = if (land && list) 20 else if (land) 24 else 28

    /** The tabs and the strip together, dp: 92 portrait, 80 landscape, 68 landscape 3 · List (v51 took 110, 82, 68);
     *  the List and Plan tabs, no strip: 64 portrait, 56 landscape (48 for List). */
    fun budgetDp(land: Boolean, tab: String, mode: String): Int {
        val list = listView(tab, mode)
        return 2 * tabRowDp(land, list) + if (showStrip(tab)) stripDp(land, list) else 0
    }
}
