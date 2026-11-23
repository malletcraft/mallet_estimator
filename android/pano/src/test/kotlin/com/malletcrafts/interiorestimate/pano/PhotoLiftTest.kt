package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * HOW FAR THE PHOTO LIFTS UNDER THE PANEL (port 63 slice I2a; DESIGN-imagemeter §2.3 F, §6, §13). The panel opens half
 * way in portrait, over the photo's lower part; the photo is moved up by just enough that the picked item stays in
 * view above it. The Mi A3 in dp: a 756 dp window, the photo's box from 186 (under the chrome) to 700 (514 dp of it),
 * the tool row under that; the panel's half rest is 378 dp, so its top edge is at 378 and 192 dp of photo show.
 *
 * (Test names are plain ASCII: MeasureWalkTest's header says why.)
 */
class PhotoLiftTest {

    private val top = 186.0; private val bottom = 700.0; private val half = 756.0 - MarkSheet.halfRestDp(756.0)
    private val m = PhotoLift.MARGIN_DP
    private fun lift(itemTop: Double, itemBottom: Double, panelTop: Double = half, land: Boolean = false) =
        PhotoLift.lift(top, bottom, panelTop, itemTop, itemBottom, m, land)

    @Test fun `the numbers - a 12 dp margin, 192 dp of photo above the half-open panel, and no lift in landscape`() {
        assertEquals(12.0, PhotoLift.MARGIN_DP); assertEquals(378.0, half); assertEquals(192.0, half - top, "the design's 192 dp visible at half")
        assertTrue(PhotoLift.lifts(false)); assertFalse(PhotoLift.lifts(true))
        // landscape: the panel opens whole and nothing is lifted, wherever the item is
        assertEquals(0.0, lift(600.0, 640.0, land = true)); assertEquals(0.0, lift(200.0, 690.0, land = true))
    }

    @Test fun `a mark already in view above the panel is not moved`() {
        // a window high on the wall: its foot 12 dp or more above the panel's top
        assertEquals(0.0, lift(210.0, 330.0)); assertEquals(0.0, lift(210.0, 366.0), "its foot exactly a margin above the panel")
        assertTrue(PhotoLift.inView(top, half, 210.0, 330.0, m, 0.0))
        // the panel covers none of the photo (pulled down past its foot, or not open): nothing moves
        assertEquals(0.0, lift(600.0, 640.0, panelTop = 700.0)); assertEquals(0.0, lift(600.0, 640.0, panelTop = 756.0))
    }

    @Test fun `a mark under the panel is lifted until its foot is a margin above the panel's top`() {
        // a switch plate low on the wall, with its code chip over it: 560 to 620
        val by = lift(560.0, 620.0)
        assertEquals(620.0 + 12 - 378, by); assertEquals(254.0, by)
        assertTrue(PhotoLift.inView(top, half, 560.0, 620.0, m, by)); assertFalse(PhotoLift.inView(top, half, 560.0, 620.0, m, 0.0))
        assertEquals(366.0, PhotoLift.onScreen(620.0, by), "its foot is then 12 dp above the panel")
        // a mark half under the panel's top: by just what is hidden, and the margin
        assertEquals(22.0, lift(340.0, 388.0))
        // the lift follows the panel as he pulls it: higher panel, more lift; back down, less
        assertTrue(lift(560.0, 620.0, panelTop = 300.0) > by); assertTrue(lift(560.0, 620.0, panelTop = 500.0) < by)
        assertEquals(0.0, lift(560.0, 620.0, panelTop = 640.0))
    }

    @Test fun `a mark taller than what is left of the photo shows its top - its code chip and top edge - never an empty band`() {
        // a floor-to-ceiling wardrobe, its chip beside its top: 200 to 690. Its top stays a margin under the photo's top
        val by = lift(200.0, 690.0)
        assertEquals(200.0 - 12 - 186, by); assertEquals(2.0, by)
        assertEquals(top + m, PhotoLift.onScreen(200.0, by)); assertFalse(PhotoLift.inView(top, half, 200.0, 690.0, m, by), "its foot is under the panel")
        // a mark that already starts within the margin of the photo's top is not lifted at all
        assertEquals(0.0, lift(190.0, 690.0)); assertEquals(0.0, lift(150.0, 690.0), "zoomed in: its top is above the photo's")
        // 250 dp tall, low down: lifted as far as its top allows, which is not far enough for its foot
        val tall = lift(400.0, 650.0)
        assertEquals(400.0 - 12 - 186, tall); assertTrue(tall < 650.0 + 12 - 378)
        // the photo's foot never rises above the panel's top: the lift is never more than the panel covers
        val low = PhotoLift.lift(top, 400.0, 378.0, 380.0, 399.0, m)      // a short photo, 22 dp of it under the panel
        assertEquals(22.0, low); assertTrue(low < 399.0 + 12 - 378)
    }

    @Test fun `over every item and every rest - never negative, never more than the panel covers, the top never lost, and in view whenever it can be`() {
        var n = 0; var fits = 0
        for (panelTop in listOf(150.0, 186.0, 250.0, 300.0, 378.0, 450.0, 520.0, 612.0, 699.0, 700.0, 756.0))
            for (it in 100..700 step 13) for (ih in listOf(8.0, 28.0, 60.0, 150.0, 300.0, 520.0)) {
                val a = it.toDouble(); val b = a + ih
                val by = PhotoLift.lift(top, bottom, panelTop, a, b, m)
                n++
                assertTrue(by >= 0.0, "lift $by")
                assertTrue(by <= maxOf(0.0, bottom - panelTop) + 1e-9, "lifted $by, more than the ${bottom - panelTop} the panel covers")
                // its top is never lifted out: where it was a margin under the photo's top, it still is
                if (a >= top + m) assertTrue(PhotoLift.onScreen(a, by) >= top + m - 1e-9, "top at ${PhotoLift.onScreen(a, by)}")
                else assertEquals(0.0, by, "an item that starts above the line is not lifted")
                // an item already wholly in view is not moved
                if (b + m <= panelTop) assertEquals(0.0, by)
                // whenever the whole item can be shown above the panel without an empty band, it is
                val need = b + m - panelTop
                if (need > 0 && need <= bottom - panelTop && a - need >= top + m) { fits++; assertTrue(PhotoLift.inView(top, panelTop, a, b, m, by), "$a..$b under a panel at $panelTop, lifted $by") }
            }
        assertTrue(n > 3000 && fits > 300, "$n cases, $fits that fit")
    }

    @Test fun `a touch maps through the lift - what is drawn at y is on screen at y less the lift, and back`() {
        for (by in listOf(0.0, 22.0, 254.0)) for (y in listOf(186.0, 400.5, 700.0)) {
            assertEquals(y - by, PhotoLift.onScreen(y, by)); assertEquals(y, PhotoLift.onPhoto(PhotoLift.onScreen(y, by), by), 1e-9)
        }
        // a tap on the lifted switch plate's middle lands on the plate: screen 336 is the photo's 590
        assertEquals(590.0, PhotoLift.onPhoto(336.0, 254.0))
    }
}
