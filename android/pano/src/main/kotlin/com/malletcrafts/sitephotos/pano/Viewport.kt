package com.malletcrafts.sitephotos.pano

/**
 * Pan-and-zoom arithmetic for looking at one photograph.
 *
 * Amit, 2026-08-23: "need pinch zoom on apk annotated fotos. hard to read it
 * currently." The annotated face is an ImageMeter export whose entire payload
 * is small text written over a picture, and the viewer was showing it scaled
 * to fit a phone with no way to get closer.
 *
 * This is deliberately in the pano module rather than beside the composable
 * that uses it. Everything here is arithmetic on floats — no Compose type, no
 * Android type — and the module is the one CI actually runs tests against
 * (`gradle :pano:test`). Left in the `:app` module the same code would compile
 * only on a build machine and be verified only by looking at it, which for
 * "the picture must not escape the screen" is not verification at all.
 *
 * Coordinates are the BOX's: (0,0) is its top-left corner, and the transform
 * is applied about the box's centre, which is where a graphicsLayer scales
 * from by default. [offX]/[offY] are the translation applied after that
 * scaling, in the same pixels.
 */
data class Viewport(val scale: Float, val offX: Float, val offY: Float) {

    companion object {
        /** Fully zoomed out is the whole picture, letterboxed. There is no
         *  reason to allow smaller: it would only add empty screen. */
        const val MIN = 1f

        /** The ceiling. The annotation EDITOR goes to 10x because it places
         *  endpoints on individual pixels; reading a number someone already
         *  wrote needs far less, and a lower ceiling stops the zoom running
         *  past the point where a sampled-down decode has detail left. */
        const val MAX = 6f

        val FIT = Viewport(MIN, 0f, 0f)

        /**
         * The scale at which the whole image fits inside the box.
         *
         * FIT, not fill. A tile crops, which is right for a tile — a square of
         * wall reads as that room. It is wrong here: what a crop removes is
         * the edges, and an annotation near an edge is precisely the one you
         * would never know you were missing.
         */
        fun fitScale(imgW: Float, imgH: Float, boxW: Float, boxH: Float): Float =
            if (imgW <= 0f || imgH <= 0f || boxW <= 0f || boxH <= 0f) 0f
            else minOf(boxW / imgW, boxH / imgH)
    }

    /**
     * How far the picture may travel on each axis before its own edge would
     * come inside the box. An axis that is smaller than the box has no slack
     * at all and is pinned to the centre.
     */
    private fun slack(fitLen: Float, boxLen: Float): Float =
        maxOf(0f, (fitLen * scale - boxLen) / 2f)

    /** Nothing may be flung into the void. */
    fun clamp(fitW: Float, fitH: Float, boxW: Float, boxH: Float): Viewport {
        val sx = slack(fitW, boxW)
        val sy = slack(fitH, boxH)
        return copy(
            offX = if (sx <= 0f) 0f else offX.coerceIn(-sx, sx),
            offY = if (sy <= 0f) 0f else offY.coerceIn(-sy, sy))
    }

    /** A drag. */
    fun pan(dx: Float, dy: Float, fitW: Float, fitH: Float, boxW: Float, boxH: Float) =
        copy(offX = offX + dx, offY = offY + dy).clamp(fitW, fitH, boxW, boxH)

    /**
     * Zoom by [factor] about the point ([cx], [cy]) and keep whatever was
     * under that point under it.
     *
     * Without the compensating translation, pinching walks the picture away
     * from the fingers doing the pinching, and you end up chasing the thing
     * you were trying to look at. Solving `screen(p)` before = `screen(p)`
     * after for the new offset gives `off' = (1-k)·(c - centre) + k·off`.
     *
     * The fixed point survives only while the result is inside the pan limits;
     * at an edge the clamp wins, because a picture that has run out of edge
     * cannot keep travelling and a hole beside it would be worse.
     */
    fun zoomAbout(
        cx: Float, cy: Float, factor: Float,
        fitW: Float, fitH: Float, boxW: Float, boxH: Float,
    ): Viewport {
        if (scale <= 0f || factor <= 0f) return this
        val ns = (scale * factor).coerceIn(MIN, MAX)
        val k = ns / scale
        return Viewport(
            scale = ns,
            offX = (1f - k) * (cx - boxW / 2f) + k * offX,
            offY = (1f - k) * (cy - boxH / 2f) + k * offY,
        ).clamp(fitW, fitH, boxW, boxH)
    }

    /** Whether the picture is currently magnified at all. Float equality is
     *  not a question worth asking of a value a pinch produced. */
    val zoomed: Boolean get() = scale > MIN + 0.01f

    /**
     * Where a point of the image lands on screen, as a fraction ([u], [v]) of
     * the fitted image from its top-left. The composable does not need this —
     * a graphicsLayer does it — but it is the only way to state in a test what
     * "stays under your finger" means.
     */
    fun screenX(u: Float, fitW: Float, boxW: Float): Float =
        boxW / 2f + (u - 0.5f) * fitW * scale + offX

    fun screenY(v: Float, fitH: Float, boxH: Float): Float =
        boxH / 2f + (v - 0.5f) * fitH * scale + offY
}
