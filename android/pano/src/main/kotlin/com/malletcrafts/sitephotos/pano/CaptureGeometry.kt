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

    /**
     * What to shoot this room at, and whether the geometry actually fits.
     *
     * Amit, 2026-09-04: "i always measure room length and width to determine
     * the center of the room. give me a option to enter these dimensions
     * before i shoot 360 so that fov can be adjusted automatically per room."
     * He is already holding the two numbers this maths needs, so asking him to
     * pick Small/Medium/Large from them is asking him to round his own
     * measurement into somebody else's bucket. A room between two presets got
     * the wrong FOV either way — too tight and the corners are cut, too wide
     * and the walls shrink — and "still small" is what the tight side looks
     * like.
     *
     * `fitted` is the part that must not stay silent. Below roughly 6 ft of
     * clear distance the required FOV runs past RECOMMEND_MAX, and the honest
     * answer is that no single face can hold every corner from the centre of
     * a room that small — the remedy is to step back or accept the crop, and
     * only a person can choose. Clamping quietly and saying nothing produces
     * exactly the complaint this replaces: a picture that looks like the app
     * simply got it wrong.
     */
    data class Advice(
        val fovDeg: Double,
        val requiredDeg: Double,
        val fitted: Boolean,
    ) {
        val rounded: Int get() = Math.round(fovDeg).toInt()
    }

    /** Length and width in feet, in either order — the maths takes the worst
     *  of both wall pairs, so which one you call length cannot change the
     *  answer. Returns null for anything not usable as a room. */
    fun adviseForRoom(
        lengthFt: Double?,
        widthFt: Double?,
        ceilingFt: Double = DEFAULT_CEILING_FT,
    ): Advice? {
        val l = lengthFt ?: return null
        val w = widthFt ?: return null
        if (l < MIN_ROOM_FT || w < MIN_ROOM_FT) return null
        if (l > MAX_ROOM_FT || w > MAX_ROOM_FT) return null
        if (ceilingFt <= 0) return null
        val required = requiredFovDeg(l, w, ceilingFt) + MARGIN_DEG
        val fov = required.coerceIn(RECOMMEND_MIN, RECOMMEND_MAX)
        return Advice(fov, required, fitted = required <= RECOMMEND_MAX)
    }

    /** Sanity bounds on typed input. A room under 3 ft is a cupboard and a
     *  mistyped 200 is a mistyped 20 — both should be refused at the keyboard
     *  rather than turned into a confident wrong number. */
    const val MIN_ROOM_FT = 3.0
    const val MAX_ROOM_FT = 100.0

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
