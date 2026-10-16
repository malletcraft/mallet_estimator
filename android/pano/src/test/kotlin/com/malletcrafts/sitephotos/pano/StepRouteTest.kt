package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The step strip's touch area and the tabs' sizes (v52 slice 6, ct52 X1 and
 * FX3), on the Mi A3: 360 x 780 dp at density 2 (320 dpi). Everything is in px
 * as the app measures it, so the dp arithmetic is part of what is checked.
 *
 * The strip as laid out in portrait: its top at 120 dp, 28 dp tall; each step
 * 2 dp under the strip's top (its padding), 26 dp tall. Step 1 (circle only)
 * 28 dp wide centred at x 120 dp; step 2, the current one with its name
 * ("2 · Mark"), 72 dp wide centred at x 200 dp; step 3 28 dp wide at x 290.
 */
class StepRouteTest {

    private val d = 2.0
    private fun px(dp: Double) = dp * d
    private val stripTop = px(120.0)

    private fun step(key: String, cxDp: Double, wDp: Double, topDp: Double = 122.0, hDp: Double = 26.0) =
        StepRoute.Step(key, px(cxDp - wDp / 2), px(topDp), px(cxDp + wDp / 2), px(topDp + hDp))

    private val steps = listOf(step("lines", 120.0, 28.0), step("mark", 200.0, 72.0), step("list", 290.0, 28.0))
    private fun at(xDp: Double, yDp: Double, s: List<StepRoute.Step> = steps, top: Double = stripTop) =
        StepRoute.stepFor(px(xDp), px(yDp), s, top, d)

    @Test
    fun `a press on a step's circle is that step's`() {
        assertEquals("lines", at(120.0, 135.0))
        assertEquals("mark", at(200.0, 135.0))
        assertEquals("list", at(290.0, 135.0))
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
    fun `landscape, a 24 dp strip -- and a landscape 3 · List, a 20 dp one -- reach the same 48 dp`() {
        // landscape: the strip at 70 dp, steps 2 dp down and 22 dp tall
        val top = px(70.0)
        val land = listOf(step("lines", 30.0, 28.0, 72.0, 22.0), step("mark", 100.0, 72.0, 72.0, 22.0))
        assertEquals("mark", at(100.0, 72.0 + 40.0, land, top))
        assertNull(at(100.0, 72.0 + 70.0, land, top))
        assertNull(at(100.0, 69.0, land, top))
        // landscape 3 · List: no padding, the steps fill the 20 dp strip
        val lst = listOf(step("list", 200.0, 64.0, 70.0, 20.0))
        assertEquals("list", at(200.0, 70.0, lst, top))
        assertEquals("list", at(200.0, 70.0 + 47.0, lst, top))
        assertNull(at(200.0, 70.0 + 70.0, lst, top))
    }

    @Test
    fun `the tabs, in order, with PLAN hidden until the drawings`() {
        assertEquals(listOf(listOf("front", "right", "back", "left"), listOf("floor", "ceil", "list")), StepRoute.TAB_ROWS)
        assertFalse(StepRoute.TAB_ROWS.flatten().contains("plan"))
        assertEquals(listOf("Front", "Right", "Back", "Left", "Floor", "Ceiling", "List"), StepRoute.TAB_ROWS.flatten().map { StepRoute.tabName(it) })
        assertTrue(StepRoute.tabTick("ceil", true))
        assertFalse(StepRoute.tabTick("ceil", false))
        assertFalse(StepRoute.tabTick("list", true))
    }

    @Test
    fun `the steps are 1 · Lines, 2 · Mark, 3 · List -- no 4 · 360 until it works`() {
        assertEquals(listOf("1 · Lines", "2 · Mark", "3 · List"), StepRoute.STEPS.indices.map { StepRoute.label(it) })
        assertEquals(1, StepRoute.stepIndex("mark"))
        assertEquals(2, StepRoute.stepIndex("list"))
        assertEquals(-1, StepRoute.stepIndex("360"))
        assertEquals("Front wall ·", StepRoute.stripFace("front"))
        assertEquals("Ceiling ·", StepRoute.stripFace("ceil"))
        assertTrue(StepRoute.showStrip("front"))
        assertFalse(StepRoute.showStrip("list"))
    }

    @Test
    fun `tabs and strip take 92 dp portrait, 80 landscape, 68 landscape 3 · List`() {
        for (m in listOf("lines", "mark")) {
            assertEquals(92, StepRoute.budgetDp(false, "front", m))
            assertEquals(80, StepRoute.budgetDp(true, "front", m))
        }
        assertEquals(68, StepRoute.budgetDp(true, "front", "list"))
        assertEquals(92, StepRoute.budgetDp(false, "front", "list"))
        // the room's List tab: no strip, the tab rows only
        assertEquals(64, StepRoute.budgetDp(false, "list", "mark"))
        assertEquals(48, StepRoute.budgetDp(true, "list", "lines"))
        // the strip is always smaller than a tab row
        for (land in listOf(false, true)) for (list in listOf(false, true))
            assertTrue(StepRoute.stripDp(land, list) < StepRoute.tabRowDp(land, list))
        // never above v51's 110 / 82 / 68
        assertTrue(StepRoute.budgetDp(false, "front", "mark") <= 110)
        assertTrue(StepRoute.budgetDp(true, "front", "mark") <= 82)
    }
}
