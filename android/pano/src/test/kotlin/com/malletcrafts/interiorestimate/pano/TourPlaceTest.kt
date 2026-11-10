package com.malletcrafts.interiorestimate.pano

import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The bubble's placement (port 61c §31), held to the prototype's own bar: the bubble never
 * covers the hole, never leaves the frame, and the squeeze (a scrolling body) and sitting
 * over the target never happen on any step of the driven run.
 *
 * The fixtures are the REAL run: all 146 steps (7 flows × EN and मराठी) of the approved
 * v61c.2 page driven at the Mi A3 frame on 9/10 Nov 2026, each step's frame / target / hole /
 * bubble rect exported from `T55.state().rect` (the driver is android/docs/port61c/
 * tour-drive.mjs). They are COMMITTED as a test resource — §31 critic finding 6: a gate's
 * load-bearing inputs never live only in the session scratchpad. The first export
 * (report.json) carried the verdicts but NOT the rects; this one carries both.
 */
class TourPlaceTest {

    private class Fx(
        val id: String, val lang: String, val side: String, val overlap: Boolean,
        val clip: Double, val trim: Boolean,
        val frame: TourPlace.R, val target: TourPlace.R?, val hole: TourPlace.R?, val bubble: TourPlace.R,
    )

    private fun rect(v: Any?): TourPlace.R? {
        val a = v as? List<*> ?: return null
        val n = a.map { (it as Number).toFloat() }
        return TourPlace.R(n[0], n[1], n[2], n[3])
    }

    @Suppress("UNCHECKED_CAST")
    private val fixtures: List<Fx> by lazy {
        val text = requireNotNull(javaClass.getResourceAsStream("/tour/tour-place-rects.json")).readBytes().decodeToString()
        val arr = MiniJson.parse(text) as List<Map<String, Any?>>
        arr.map { e ->
            val r = e["rect"] as Map<String, Any?>
            Fx(e["id"] as String, e["lang"] as String, e["side"] as String, e["overlap"] as Boolean,
                (e["clip"] as Number).toDouble(), e["trim"] as Boolean,
                requireNotNull(rect(r["frame"])), rect(r["target"]), rect(r["hole"]), requireNotNull(rect(r["bubble"])))
        }
    }

    private val eps = 0.6f

    private fun inFrame(f: TourPlace.R, b: TourPlace.R, what: String) {
        assertTrue(b.x >= -eps && b.y >= -eps && b.x1 <= f.w + eps && b.y1 <= f.h + eps, what)
    }

    @Test fun `the driven run holds the bar - 146 steps, no overlap, no clip, bubble inside the frame and clear of the hole`() {
        assertEquals(146, fixtures.size)
        assertEquals(73, fixtures.count { it.lang == "en" })
        for (fx in fixtures) {
            assertTrue(!fx.overlap && fx.clip == 0.0, fx.id)
            inFrame(fx.frame, fx.bubble, "${fx.lang} ${fx.id}: bubble leaves the frame")
            val h = assertNotNull(fx.hole, fx.id)
            assertTrue(!fx.bubble.intersects(h), "${fx.lang} ${fx.id}: bubble covers the hole")
        }
        assertTrue(fixtures.count { it.trim } in 1..10, "trim only where designed (the run had 5)")
    }

    @Test fun `this port places every driven step without overlap, squeeze or leaving the frame`() {
        for (fx in fixtures) {
            val f = fx.frame
            val land = f.w > f.h
            val bw = TourPlace.bubbleWidth(f.w, land)
            // the measured bubble, re-flowed: its recorded height at its recorded width, scaled
            // by area for any other width the algorithm probes (text keeps its area, roughly)
            val need = { w: Float -> ceil((fx.bubble.h * fx.bubble.w / w).toDouble()).toFloat() }
            val p = TourPlace.place(f, fx.hole, bw, land, need)
            assertTrue(!p.overlap, "${fx.lang} ${fx.id}: over the target")
            assertTrue(!p.clip, "${fx.lang} ${fx.id}: squeezed")
            inFrame(f, p.rect, "${fx.lang} ${fx.id}: bubble leaves the frame")
            val lit = p.trim ?: fx.hole!!
            assertTrue(!p.rect.intersects(lit), "${fx.lang} ${fx.id}: bubble covers the lit part")
        }
    }

    // ---- the shapes, by hand ----

    private val need200 = { w: Float -> 200f * 328f / w }

    @Test fun `no hole centres the bubble with no spotlight`() {
        val p = TourPlace.place(TourPlace.R(0f, 0f, 360f, 708f), null, 328f, false, { 200f })
        assertEquals("centre", p.side)
        assertEquals((360f - 328f) / 2, p.x)
        assertTrue(!p.overlap && p.trim == null)
    }

    @Test fun `portrait prefers below, then above, GAP from the hole`() {
        val f = TourPlace.R(0f, 0f, 360f, 708f)
        val top = TourPlace.place(f, TourPlace.R(16f, 100f, 328f, 40f), 328f, false, need200)
        assertEquals("below", top.side)
        assertEquals(100f + 40f + TourPlace.GAP, top.y)
        val bottom = TourPlace.place(f, TourPlace.R(16f, 600f, 328f, 40f), 328f, false, need200)
        assertEquals("above", bottom.side)
        assertEquals(600f - TourPlace.GAP - bottom.h, bottom.y)
    }

    @Test fun `landscape prefers a horizontal side, only with 200 of room`() {
        val f = TourPlace.R(0f, 0f, 708f, 360f)
        val p = TourPlace.place(f, TourPlace.R(20f, 100f, 120f, 60f), 300f, true, { w -> 120f * 300f / w })
        assertEquals("right", p.side)
        assertEquals(20f + 120f + TourPlace.GAP, p.x)
    }

    @Test fun `portrait trims a tall hole from below, keeping at least 48 lit`() {
        val f = TourPlace.R(0f, 0f, 360f, 708f)
        // the hole fills the frame's height: no side holds the bubble whole
        val p = TourPlace.place(f, TourPlace.R(16f, 10f, 328f, 688f), 328f, false, { 200f })
        assertEquals("below", p.side)
        val t = assertNotNull(p.trim)
        assertTrue(t.h >= 48f)
        assertTrue(!p.rect.intersects(t))
        inFrame(f, p.rect, "trimmed bubble leaves the frame")
    }

    @Test fun `the hole pads the target and clamps to the frame - the trim keeps the intersection`() {
        val f = TourPlace.R(0f, 0f, 360f, 708f)
        val h = assertNotNull(TourPlace.hole(f, TourPlace.R(0f, 0f, 50f, 40f)))
        assertEquals(0f, h.x); assertEquals(0f, h.y)
        assertEquals(50f + TourPlace.PAD, h.w); assertEquals(40f + TourPlace.PAD, h.h)
        assertNull(TourPlace.hole(f, TourPlace.R(-20f, -20f, 10f, 10f)), "a target off the frame lights nothing")
        val kept = TourPlace.trimTarget(TourPlace.R(10f, 10f, 100f, 100f), TourPlace.R(0f, 0f, 60f, 60f))
        assertEquals(TourPlace.R(10f, 10f, 50f, 50f), kept)
    }
}
