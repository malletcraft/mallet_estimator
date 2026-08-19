package com.malletcrafts.sitephotos.pano

import kotlin.math.atan
import kotlin.math.max

/**
 * The physics of "show me all four corners of every wall".
 *
 * A split face is a gnomonic view with one FOV for both axes. Standing at
 * the room centre, a wall of width w at distance d needs
 *     horizontally: 2·atan((w/2)/d)
 *     vertically:   2·max(atan((ceiling−camera)/d), atan(camera/d))
 * and the face FOV must beat the worst wall on both counts. Two things
 * follow, and the capture screen teaches both:
 *
 *  - Small rooms need WIDE faces. A 5×7 ft bathroom seen from its centre
 *    needs ≈125° before margin — the historical fixed 110° is exactly why
 *    walls came back truncated.
 *  - The camera belongs at HALF the ceiling height. Any lower and the
 *    ceiling corners need more degrees than the floor ones; mid-height
 *    splits the need evenly and minimises the FOV that must carry it.
 *
 * Pure JVM on purpose: this is estimating maths, so it is unit-tested like
 * estimating maths.
 */
object CaptureGeometry {

    /** Above this, gnomonic corner-stretch stops being worth the coverage. */
    const val RECOMMEND_MAX = 135.0

    /** Below this there is no reason to go — coverage only shrinks. */
    const val RECOMMEND_MIN = 95.0

    /** Headroom over the geometric minimum: nobody stands at the exact
     *  centre and no camera is at the exact height. */
    const val MARGIN_DEG = 5.0

    const val DEFAULT_CEILING_FT = 9.5

    /** Mid-height splits the up/down need evenly — the minimising choice. */
    fun recommendedCameraHeightFt(ceilingFt: Double = DEFAULT_CEILING_FT): Double =
        ceilingFt / 2.0

    /**
     * The FOV that just captures every wall, floor line to ceiling line and
     * corner to corner, from the room centre. No margin, no clamp — the raw
     * geometry, so tests can reason about it.
     */
    fun requiredFovDeg(
        widthFt: Double,
        depthFt: Double,
        ceilingFt: Double = DEFAULT_CEILING_FT,
        cameraFt: Double = recommendedCameraHeightFt(ceilingFt),
    ): Double {
        require(widthFt > 0 && depthFt > 0 && ceilingFt > 0) { "room dims must be positive" }
        require(cameraFt > 0 && cameraFt < ceilingFt) { "camera must be inside the room" }

        fun wall(w: Double, d: Double): Double {
            val horizontal = 2 * deg(atan((w / 2) / d))
            val vertical = 2 * deg(max(atan((ceilingFt - cameraFt) / d), atan(cameraFt / d)))
            return max(horizontal, vertical)
        }
        // Two walls of `width` faced from depth/2 away, and vice versa.
        return max(wall(widthFt, depthFt / 2), wall(depthFt, widthFt / 2))
    }

    /** What the app should actually use: geometry + margin, kept inside the
     *  range where gnomonic faces stay readable. */
    fun recommendedFovDeg(
        widthFt: Double,
        depthFt: Double,
        ceilingFt: Double = DEFAULT_CEILING_FT,
        cameraFt: Double = recommendedCameraHeightFt(ceilingFt),
    ): Double = (requiredFovDeg(widthFt, depthFt, ceilingFt, cameraFt) + MARGIN_DEG)
        .coerceIn(RECOMMEND_MIN, RECOMMEND_MAX)

    /** The room sizes the shop actually meets, per the owner (2026-08-19):
     *  5×7 bathroom, 8×11 balcony, 20×18 living room. */
    data class RoomPreset(val label: String, val widthFt: Double, val depthFt: Double) {
        val fov: Double get() = recommendedFovDeg(widthFt, depthFt)
    }

    val PRESETS: List<RoomPreset> = listOf(
        RoomPreset("Small (bathroom ~5×7 ft)", 5.0, 7.0),
        RoomPreset("Medium (balcony ~8×11 ft)", 8.0, 11.0),
        RoomPreset("Large (living ~20×18 ft)", 20.0, 18.0),
    )

    private fun deg(rad: Double): Double = Math.toDegrees(rad)
}
