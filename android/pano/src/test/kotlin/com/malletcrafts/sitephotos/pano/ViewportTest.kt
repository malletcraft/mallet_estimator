package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The viewer's pan/zoom, checked as arithmetic.
 *
 * A phone in a hand can be made to look right by fiddling until it does; what
 * cannot be checked that way is whether the picture can be pushed off the
 * screen, or whether the thing under your fingers stays under them, because
 * both fail only at particular numbers.
 *
 * The geometry throughout: a 1080x1440 box showing a 2400x1800 face. Fitted,
 * that face is 1080 wide and 810 tall — wider than it is tall, so at 1x the
 * horizontal axis exactly fills the box and the vertical one is letterboxed.
 * That asymmetry is the point: the two axes must behave differently.
 */
class ViewportTest {

    private val boxW = 1080f
    private val boxH = 1440f
    private val imgW = 2400f
    private val imgH = 1800f
    private val fit = Viewport.fitScale(imgW, imgH, boxW, boxH)
    private val fitW = imgW * fit
    private val fitH = imgH * fit

    private fun near(a: Float, b: Float, tol: Float = 0.01f) =
        assertTrue(abs(a - b) <= tol, "expected $b, got $a")

    @Test
    fun `the whole image fits, letterboxed on the short axis`() {
        near(fitW, 1080f)
        near(fitH, 810f)
        assertTrue(fitH < boxH, "a fit that filled the box would be cropping")
    }

    @Test
    fun `fully zoomed out the picture is centred and cannot be dragged`() {
        val v = Viewport.FIT.pan(400f, 400f, fitW, fitH, boxW, boxH)
        near(v.offX, 0f)
        near(v.offY, 0f)
        assertFalse(v.zoomed)
    }

    @Test
    fun `zooming about the centre moves nothing sideways`() {
        val v = Viewport.FIT.zoomAbout(boxW / 2f, boxH / 2f, 3f, fitW, fitH, boxW, boxH)
        assertEquals(3f, v.scale)
        near(v.offX, 0f)
        near(v.offY, 0f)
    }

    @Test
    fun `the point under the fingers stays under the fingers`() {
        // A point a quarter across and a third down the face — off-centre on
        // both axes, so a wrong sign anywhere shows up.
        val u = 0.25f
        val vv = 0.33f
        val before = Viewport.FIT
        val sx = before.screenX(u, fitW, boxW)
        val sy = before.screenY(vv, fitH, boxH)

        // 4x, not 2x, and the reason is the letterbox. At 2x the fitted 810
        // is 1620 against a 1440 box — only 90 px of vertical travel exist, so
        // the clamp reaches this point before the fixed point does and the
        // test would be measuring the clamp. By 4x both axes have room, which
        // is the case this test is actually about.
        val after = before.zoomAbout(sx, sy, 4f, fitW, fitH, boxW, boxH)

        near(after.screenX(u, fitW, boxW), sx)
        near(after.screenY(vv, fitH, boxH), sy)
    }

    @Test
    fun `at an edge the clamp beats the fixed point`() {
        // The other half of the rule above, and the one that keeps the screen
        // honest: a picture with no edge left cannot keep travelling to hold a
        // point still, because the only way to do that is to open a gap beside
        // it. Vertically at 2x there are 90 px of travel and the fixed point
        // wants 137.7, so the offset must land on the limit exactly.
        val vv = 0.33f
        val sy = Viewport.FIT.screenY(vv, fitH, boxH)
        val after = Viewport.FIT.zoomAbout(boxW / 2f, sy, 2f, fitW, fitH, boxW, boxH)

        val slackY = (fitH * after.scale - boxH) / 2f
        near(slackY, 90f)
        near(after.offY, slackY)
        // No gap: the top edge of the picture sits on the top of the box.
        near(after.screenY(0f, fitH, boxH), 0f)
    }

    @Test
    fun `panning stops at the image edge, never past it`() {
        val z = Viewport.FIT.zoomAbout(boxW / 2f, boxH / 2f, 4f, fitW, fitH, boxW, boxH)
        // Shove it far harder than any finger could.
        val v = z.pan(9000f, 9000f, fitW, fitH, boxW, boxH)
        val slackX = (fitW * v.scale - boxW) / 2f
        val slackY = (fitH * v.scale - boxH) / 2f
        near(v.offX, slackX)
        near(v.offY, slackY)
        // At the limit the image's own edge has arrived at the box edge: no
        // further, and no gap.
        near(v.screenX(0f, fitW, boxW), 0f)
        near(v.screenY(0f, fitH, boxH), 0f)
    }

    @Test
    fun `the short axis is still pinned while it is smaller than the box`() {
        // At 1.5x the fitted height is 1215, still under the box's 1440, so
        // vertical travel must remain zero while horizontal travel is free.
        val v = Viewport.FIT
            .zoomAbout(boxW / 2f, boxH / 2f, 1.5f, fitW, fitH, boxW, boxH)
            .pan(500f, 500f, fitW, fitH, boxW, boxH)
        assertTrue(fitH * 1.5f < boxH)
        near(v.offY, 0f)
        assertTrue(v.offX > 0f, "the wide axis had room and should have moved")
    }

    @Test
    fun `zoom stops at the ceiling and at the floor`() {
        var v = Viewport.FIT
        repeat(12) { v = v.zoomAbout(boxW / 2f, boxH / 2f, 2f, fitW, fitH, boxW, boxH) }
        assertEquals(Viewport.MAX, v.scale)

        repeat(12) { v = v.zoomAbout(boxW / 2f, boxH / 2f, 0.5f, fitW, fitH, boxW, boxH) }
        assertEquals(Viewport.MIN, v.scale)
    }

    @Test
    fun `zooming back out re-centres instead of leaving the picture adrift`() {
        // In at a corner, which parks the offset hard against a limit...
        val v = Viewport.FIT
            .zoomAbout(0f, 0f, 5f, fitW, fitH, boxW, boxH)
            .pan(-4000f, -4000f, fitW, fitH, boxW, boxH)
        assertTrue(v.offX != 0f)
        // ...then all the way out. A stale offset here would leave the fitted
        // picture sitting off to one side with a band of dead screen beside it.
        val out = v.zoomAbout(boxW / 2f, boxH / 2f, 0.01f, fitW, fitH, boxW, boxH)
        assertEquals(Viewport.MIN, out.scale)
        near(out.offX, 0f)
        near(out.offY, 0f)
    }

    @Test
    fun `a degenerate box asks for no scale rather than dividing by zero`() {
        assertEquals(0f, Viewport.fitScale(imgW, imgH, 0f, boxH))
        assertEquals(0f, Viewport.fitScale(0f, 0f, boxW, boxH))
    }
}
