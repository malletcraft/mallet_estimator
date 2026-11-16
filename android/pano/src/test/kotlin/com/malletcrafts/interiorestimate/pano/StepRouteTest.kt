package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * The step strip's touch area and the tabs' sizes (v52 slice 6, ct52 X1 and
 * FX3), on the Mi A3: 360 x 780 dp at density 2 (320 dpi). Everything is in px
 * as the app measures it, so the dp arithmetic is part of what is checked.
 *
 * The strip as laid out in portrait: its top at 120 dp, 28 dp tall; each step
 * 2 dp under the strip's top (its padding), 26 dp tall. Step 1 (circle only)
 * 28 dp wide centred at x 120 dp; step 2, the current one with its name
 * ("2 · Mark"), 72 dp wide centred at x 200 dp; step 3 28 dp wide at x 290.
 *
 * PORT 63 SLICE M2 (16 Nov 2026): the third step is 3 · Measure -- the squared photo with its figures, not a list --
 * and the room's LIST tab is gone, so the tabs are four and three. Amit, 15 Nov 2026, after his first use of the app on
 * a client site: "The laundry list of measurements shown below screen is also frustrating." The landscape list's own
 * tab-row and strip heights went with it: every budget below is now the face's or the Plan tab's.
 */
class StepRouteTest {

    private val d = 2.0
    private fun px(dp: Double) = dp * d
    private val stripTop = px(120.0)

    private fun step(key: String, cxDp: Double, wDp: Double, topDp: Double = 122.0, hDp: Double = 26.0) =
        StepRoute.Step(key, px(cxDp - wDp / 2), px(topDp), px(cxDp + wDp / 2), px(topDp + hDp))

    private val steps = listOf(step("lines", 120.0, 28.0), step("mark", 200.0, 72.0), step("measure", 290.0, 28.0))
    private fun at(xDp: Double, yDp: Double, s: List<StepRoute.Step> = steps, top: Double = stripTop) =
        StepRoute.stepFor(px(xDp), px(yDp), s, top, d)

    @Test
    fun `a press on a step's circle is that step's`() {
        assertEquals("lines", at(120.0, 135.0))
        assertEquals("mark", at(200.0, 135.0))
        assertEquals("measure", at(290.0, 135.0))
    }

    @Test
    fun `40 dp under a step's top selects it -- the caption line and the top of the photo`() {
        assertEquals("mark", at(200.0, 122.0 + 40.0))
        // FX3: 6, 14 and 20 dp under the step's bottom are all within 48 dp of its top
        for (dy in listOf(6.0, 14.0, 20.0)) assertEquals("mark", at(200.0, 122.0 + 26.0 + dy), "dy $dy")
    }

    @Test
    fun `70 dp under a step the press is the photo's again`() {
        assertNull(at(200.0, 122.0 + 70.0))
        // the edge of the reach: 47.5 dp is the step's, 48 is not
        assertEquals("mark", at(200.0, 122.0 + 47.5))
        assertNull(at(200.0, 122.0 + 48.0))
    }

    @Test
    fun `a press never routes up into the tabs`() {
        // directly above step 2, inside the bottom tab row: nobody's, however close
        assertNull(at(200.0, 119.0))
        assertNull(at(200.0, 100.0))
        // even a step drawn above the strip's top (a stale bound) cannot pull a press out of the tabs
        assertNull(at(200.0, 110.0, listOf(step("mark", 200.0, 72.0, topDp = 100.0))))
        // the strip's own 2 dp padding above the steps: in the strip, but above every step -- nobody's
        assertNull(at(200.0, 121.0))
    }

    @Test
    fun `horizontally a step answers max(24 dp, half its width) from its centre`() {
        // a narrow step (28 dp wide) still answers 24 dp either side
        assertEquals("lines", at(120.0 + 23.5, 150.0))
        assertEquals("lines", at(120.0 - 24.0, 150.0))
        assertNull(at(120.0 - 25.0, 150.0))
        // the wide current step answers its own half width, 36 dp
        assertEquals("mark", at(200.0 + 35.5, 150.0))
        assertNull(at(200.0 + 37.0 + 10.0, 150.0))
        // the face's name, left of step 1, is no step's
        assertNull(at(40.0, 150.0))
    }

    @Test
    fun `the first matching step wins where two touch areas overlap`() {
        // two narrow steps 40 dp apart: their 24 dp halves overlap between 8 and -8 of the midpoint
        val close = listOf(step("lines", 100.0, 20.0), step("mark", 140.0, 20.0))
        assertEquals("lines", at(120.0, 150.0, close))
        assertEquals("mark", at(120.0, 150.0, close.reversed()))
    }

    @Test
    fun `a step not laid out yet is skipped, and an empty strip routes nothing`() {
        val ghost = StepRoute.Step("lines", 0.0, px(122.0), 0.0, px(148.0))
        assertEquals("mark", at(200.0, 150.0, listOf(ghost, step("mark", 200.0, 72.0))))
        assertNull(at(200.0, 150.0, emptyList()))
    }

    @Test
    fun `landscape, a 24 dp strip, reaches the same 48 dp -- 3 Measure's step as any other`() {
        // landscape: the strip at 70 dp, steps 2 dp down and 22 dp tall
        val top = px(70.0)
        val land = listOf(step("lines", 30.0, 28.0, 72.0, 22.0), step("mark", 100.0, 72.0, 72.0, 22.0), step("measure", 200.0, 86.0, 72.0, 22.0))
        assertEquals("mark", at(100.0, 72.0 + 40.0, land, top))
        assertNull(at(100.0, 72.0 + 70.0, land, top))
        assertNull(at(100.0, 69.0, land, top))
        // 3 · Measure, the current step with its name: the same reach, and its own half width
        assertEquals("measure", at(200.0, 72.0, land, top))
        assertEquals("measure", at(200.0, 72.0 + 47.0, land, top))
        assertEquals("measure", at(200.0 + 42.0, 72.0 + 20.0, land, top))
        assertNull(at(200.0, 72.0 + 70.0, land, top))
    }

    @Test
    fun `the tabs, in order -- four and three, PLAN the last, and no LIST tab (port 63 slice M2)`() {
        assertEquals(listOf(listOf("front", "right", "back", "left"), listOf("floor", "ceil", "plan")), StepRoute.TAB_ROWS)
        assertEquals("front,right,back,left,floor,ceil,plan", StepRoute.TAB_ROWS.flatten().joinToString(","))
        assertEquals(listOf(4, 3), StepRoute.TAB_ROWS.map { it.size })
        assertEquals(listOf("Front", "Right", "Back", "Left", "Floor", "Ceiling", "Plan"), StepRoute.TAB_ROWS.flatten().map { StepRoute.tabName(Tx.EN, it) })
        // the six faces in the walk's order, then Plan: every tab but the last is a face
        assertEquals(SurveyPrep.SURFACES, StepRoute.TAB_ROWS.flatten().dropLast(1)); assertEquals(MeasureWalk.FACES, StepRoute.TAB_ROWS.flatten().filter { StepRoute.isFace(it) })
        assertTrue(StepRoute.tabTick("ceil", true))
        assertFalse(StepRoute.tabTick("ceil", false))
        assertFalse(StepRoute.tabTick("plan", true))
        assertTrue(StepRoute.isFace("front")); assertFalse(StepRoute.isFace("plan"))
        // THE LIST IS GONE: no tab, no face and no step is called so, and a stale "list" is nobody's
        assertFalse("list" in StepRoute.TAB_ROWS.flatten()); assertFalse("list" in StepRoute.STEPS)
        assertFalse(StepRoute.isFace("list")); assertFalse(StepRoute.isPlan("list")); assertFalse(StepRoute.showStrip("list")); assertEquals(-1, StepRoute.stepIndex("list"))
        // the tab's name under मराठी is the key it always was
        assertEquals("प्लॅन", StepRoute.tabName(Tx.MR, "plan"))
    }

    @Test
    fun `the Plan tab has no step strip - its tab rows alone, 64 dp portrait and 56 landscape`() {
        assertFalse(StepRoute.showStrip("plan")); assertTrue(StepRoute.isPlan("plan"))
        assertEquals(64, StepRoute.budgetDp(false, "plan"))
        assertEquals(56, StepRoute.budgetDp(true, "plan"))
    }

    @Test
    fun `the steps are 1 · Lines, 2 · Mark, 3 · Measure, 4 · 360 -- the third is the photo, not a list (port 63 slice M2)`() {
        assertEquals(listOf("lines", "mark", "measure", "360"), StepRoute.STEPS)
        assertEquals("measure", StepRoute.MEASURE)
        assertEquals(listOf("1 · Lines", "2 · Mark", "3 · Measure", "4 · 360"), StepRoute.STEPS.indices.map { StepRoute.label(Tx.EN, it) })
        assertEquals(listOf("Lines", "Mark", "Measure", "360"), StepRoute.STEPS.map { StepRoute.stepName(Tx.EN, it) })
        assertEquals(1, StepRoute.stepIndex("mark"))
        assertEquals(2, StepRoute.stepIndex(StepRoute.MEASURE))
        assertEquals(3, StepRoute.stepIndex("360"))
        assertEquals(-1, StepRoute.stepIndex("plan"))
        assertEquals("Front wall ·", StepRoute.stripFace(Tx.EN, "front"))
        assertEquals("Ceiling ·", StepRoute.stripFace(Tx.EN, "ceil"))
        assertTrue(StepRoute.showStrip("front"))
        // the step's name is an EXISTING key (common.measure), with the Marathi it always had: nothing new to translate
        assertEquals("3 · मोजा", StepRoute.label(Tx.MR, 2))
    }

    @Test
    fun `tabs and strip take 92 dp portrait and 80 landscape on every face and every step - the list's own heights are gone`() {
        for (s in SurveyPrep.SURFACES) {
            assertEquals(92, StepRoute.budgetDp(false, s))
            assertEquals(80, StepRoute.budgetDp(true, s))
        }
        assertEquals(32, StepRoute.tabRowDp(false)); assertEquals(28, StepRoute.tabRowDp(true))
        assertEquals(28, StepRoute.stripDp(false)); assertEquals(24, StepRoute.stripDp(true))
        // the strip is always smaller than a tab row
        for (land in listOf(false, true)) assertTrue(StepRoute.stripDp(land) < StepRoute.tabRowDp(land))
        // never above v51's 110 / 82
        assertTrue(StepRoute.budgetDp(false, "front") <= 110)
        assertTrue(StepRoute.budgetDp(true, "front") <= 82)
    }

    @Test
    fun `a tab row holds a face's name under Marathi over a Ref line, in both orientations (M3a review)`() {
        // SpTab draws a face's name on a 17 sp line under मराठी (design-marathi §4, measured for the prototype's 32 px tab)
        // and, on a face squared from its reference, "Ref 905 × 2053" on a 10 sp line (9 in landscape). The landscape
        // LIST's 24 dp row once cut that line and was made 27 under मराठी; that row is gone with the list (port 63 slice
        // M2), and the two rows that are left had the room already.
        assertTrue(StepRoute.tabRowDp(false) >= 17 + 10)
        assertTrue(StepRoute.tabRowDp(true) >= 17 + 9)
    }

    // ------------------------------------------------------------------ the measure bar and its pad (port 63 slice M1)

    @Test
    fun `the measure bar is 56 dp and a pad row 44, 48 and 36 in landscape, four rows`() {
        assertEquals(56, StepRoute.measureBarDp(false)); assertEquals(48, StepRoute.measureBarDp(true))
        assertEquals(44, StepRoute.padRowDp(false)); assertEquals(36, StepRoute.padRowDp(true))
        assertEquals(4, StepRoute.PAD_ROWS)
        // bar + pad: 232 dp in portrait, 192 in landscape (DESIGN-port63 §1.4)
        assertEquals(232, StepRoute.measureBarDp(false) + StepRoute.PAD_ROWS * StepRoute.padRowDp(false))
        assertEquals(192, StepRoute.measureBarDp(true) + StepRoute.PAD_ROWS * StepRoute.padRowDp(true))
    }

    @Test
    fun `in landscape the bar, the tabs, the strip, the measure bar and the open pad fit the column`() {
        assertEquals(32, StepRoute.LAND_TOPBAR_DP); assertEquals(324, StepRoute.LAND_COLUMN_DP)
        // the tabs 56 and the strip 24, in English and under मराठी alike (the heights name no language any more)
        for (mr in listOf(false, true)) {
            assertEquals(56, 2 * StepRoute.tabRowDp(true)); assertEquals(24, StepRoute.stripDp(true))
            val column = StepRoute.LAND_TOPBAR_DP + StepRoute.budgetDp(true, "front") +
                StepRoute.measureBarDp(true) + StepRoute.PAD_ROWS * StepRoute.padRowDp(true)
            assertEquals(304, column)                                   // 32 + 56 + 24 + 48 + 144
            assertTrue(column <= StepRoute.LAND_COLUMN_DP)
        }
        // what is left under the top bar, the tabs and the strip is 212 dp: the picked mark's row (about 34), the
        // measure bar, the palette (52) and the pad together are 278 -- so an open pad takes the palette's and the
        // row's place (the plan critic's finding 4), and this is why
        val under = StepRoute.LAND_COLUMN_DP - StepRoute.LAND_TOPBAR_DP - StepRoute.budgetDp(true, "front")
        assertEquals(212, under)
        assertTrue(34 + StepRoute.measureBarDp(true) + 52 + StepRoute.PAD_ROWS * StepRoute.padRowDp(true) > under)
        assertTrue(StepRoute.measureBarDp(true) + StepRoute.PAD_ROWS * StepRoute.padRowDp(true) <= under)
    }
}
