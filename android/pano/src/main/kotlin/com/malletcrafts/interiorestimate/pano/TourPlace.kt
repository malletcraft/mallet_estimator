package com.malletcrafts.interiorestimate.pano

import kotlin.math.max
import kotlin.math.min

/**
 * THE BUBBLE'S PLACEMENT (port 61c §31) — the engine's `place()` as pure rect-in/rect-out,
 * byte-faithful to the page (index61c2.html, Builder E). Units are dp (the prototype's CSS px
 * at the Mi A3's 360 dp frame); the overlay converts.
 *
 * The bubble takes a side of the hole with room for ALL of it, so its text is never cut. In
 * order: (1) the usual width on the usual sides — below / above first in portrait, right /
 * left first in landscape (a horizontal side only with >= 200 of room); (2) above / below at
 * the frame's full width, for a wide short target; (2b) beside it, down to 176 wide; (3)
 * TRIM: light only the target's leading part (its top in portrait — >= 48 of it left — its
 * left in landscape, >= 96) so a side takes the whole bubble; (4) only then the squeeze (the
 * body scrolls, the side with most room*width) and sitting over the target as the very last
 * resort. [TourPlaceTest] holds the prototype's bar to this port: no overlap and no clip on
 * any step of the driven run, trim only where the prototype trimmed.
 */
object TourPlace {

    const val PAD = 6f
    const val EDGE = 8f
    const val GAP = 10f
    const val MINB = 72f

    data class R(val x: Float, val y: Float, val w: Float, val h: Float) {
        val x1: Float get() = x + w
        val y1: Float get() = y + h
        fun intersects(o: R): Boolean = x < o.x1 && o.x < x1 && y < o.y1 && o.y < y1
    }

    data class Placement(
        val x: Float, val y: Float, val w: Float, val h: Float,
        val side: String,
        val overlap: Boolean,
        val clip: Boolean = false,
        /** Non-null when the lit part was trimmed so a side could take the whole bubble. */
        val trim: R? = null,
    ) {
        val rect: R get() = R(x, y, w, h)
    }

    /** The bubble's usual width: 300 in landscape, else min(328, frame − 2·EDGE). */
    fun bubbleWidth(frameW: Float, land: Boolean): Float =
        if (land) 300f else min(328f, frameW - 2 * EDGE)

    /** The hole round a (clipped) target: PAD all round, clamped to the frame; null when nothing is left. */
    fun hole(f: R, target: R): R? {
        val x0 = max(0f, target.x - PAD)
        val y0 = max(0f, target.y - PAD)
        val x1 = min(f.w, target.x1 + PAD)
        val y1 = min(f.h, target.y1 + PAD)
        return if (x1 > x0 && y1 > y0) R(x0, y0, x1 - x0, y1 - y0) else null
    }

    /** What stays lit of [target] inside a trimmed hole — the engine's intersection after `P.trim`. */
    fun trimTarget(target: R, trim: R): R {
        val x0 = max(target.x, trim.x)
        val y0 = max(target.y, trim.y)
        val x1 = min(target.x1, trim.x1)
        val y1 = min(target.y1, trim.y1)
        return R(x0, y0, max(0f, x1 - x0), max(0f, y1 - y0))
    }

    private class Side(
        val k: String, val room: Float, val w: Float, val x: Float,
        val y: (Float) -> Float, val ok: Boolean = true,
    )

    /**
     * Place the bubble beside hole [h] in frame [f]. [need] measures the bubble: its height at
     * a given width, as the overlay's subcompose measures the real text. [h] null centres it
     * with no spotlight.
     */
    fun place(f: R, h: R?, bw: Float, land: Boolean, need: (Float) -> Float): Placement {
        if (h == null) {
            val w = min(bw, f.w - 2 * EDGE)
            val hh = min(need(w), f.h - 2 * EDGE)
            return Placement((f.w - w) / 2, (f.h - hh) / 2, w, hh, "centre", overlap = false)
        }
        val below = f.h - (h.y + h.h) - GAP - EDGE
        val above = h.y - GAP - EDGE
        val right = f.w - (h.x + h.w) - GAP - EDGE
        val left = h.x - GAP - EDGE
        val wide = f.w - 2 * EDGE
        val vw = min(bw, wide)
        fun vx(w: Float) = min(max(h.x + h.w / 2 - w / 2, EDGE), f.w - EDGE - w)
        fun hy(hh: Float) = min(max(h.y + h.h / 2 - hh / 2, EDGE), f.h - EDGE - hh)
        fun v(w: Float) = listOf(
            Side("below", below, w, vx(w), { h.y + h.h + GAP }),
            Side("above", above, w, vx(w), { hh -> h.y - GAP - hh }))
        fun sw(s: Float) = min(bw, s)
        val hs = listOf(
            Side("right", f.h - 2 * EDGE, sw(right), h.x + h.w + GAP, ::hy, right >= 200f),
            Side("left", f.h - 2 * EDGE, sw(left), h.x - GAP - sw(left), ::hy, left >= 200f))
        val order = ArrayList<Side>()
        if (land) { order += hs; order += v(vw) } else { order += v(vw); order += hs }
        if (vw < wide - 0.5f) order += v(wide)
        order += hs.map { Side(it.k, it.room, it.w, it.x, it.y, (if (it.k == "right") right else left) >= 176f) }
        for (s in order) {
            if (!s.ok) continue
            val hh = need(s.w)
            if (s.room >= hh) return Placement(s.x, s.y(hh), s.w, hh, s.k, overlap = false)
        }
        // (3) trim the lit part, so a side takes the whole bubble
        if (land) {
            val hh = need(bw)
            if (hh <= f.h - 2 * EDGE) {
                val x1 = f.w - EDGE - GAP - bw
                if (x1 - h.x >= 96f) {
                    val t = R(h.x, h.y, x1 - h.x, h.h)
                    return Placement(x1 + GAP, min(max(t.y + t.h / 2 - hh / 2, EDGE), f.h - EDGE - hh), bw, hh, "right", overlap = false, trim = t)
                }
                val x0 = EDGE + bw + GAP
                if (h.x + h.w - x0 >= 96f) {
                    val t = R(x0, h.y, h.x + h.w - x0, h.h)
                    return Placement(EDGE, min(max(t.y + t.h / 2 - hh / 2, EDGE), f.h - EDGE - hh), bw, hh, "left", overlap = false, trim = t)
                }
            }
        } else {
            val hh = need(vw)
            val y1 = f.h - EDGE - GAP - hh
            if (y1 - h.y >= 48f) {
                val t = R(h.x, h.y, h.w, y1 - h.y)
                return Placement(vx(vw), y1 + GAP, vw, hh, "below", overlap = false, trim = t)
            }
            val y0 = EDGE + hh + GAP
            if (h.y + h.h - y0 >= 48f) {
                val t = R(h.x, y0, h.w, h.y + h.h - y0)
                return Placement(vx(vw), EDGE, vw, hh, "above", overlap = false, trim = t)
            }
        }
        // (4) the squeeze: the side with most room, the body shrunk to it; over the target last of all
        val best = order.filter { it.ok && it.room >= MINB }.maxByOrNull { it.room * it.w }
        if (best != null) {
            val hh = min(need(best.w), best.room)
            return Placement(best.x, best.y(hh), best.w, hh, best.k, overlap = false, clip = true)
        }
        val hh = min(need(vw), f.h - 2 * EDGE)
        return Placement(vx(vw), f.h - EDGE - hh, vw, hh, "over", overlap = true)
    }
}
