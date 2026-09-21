package com.malletcrafts.sitephotos.pano

import kotlin.math.atan
import kotlin.math.hypot
import kotlin.math.max

/**
 * Where to stand, and how wide each of the six faces has to be.
 *
 * Amit, 2026-09-21: "capture all 3 sides of a room in inches by the user and
 * decide correct FOV based on the room. that should tell where camera should
 * be in the room by all its axes x,y,z. you can automatically adjust FOV for
 * all 6 faces so that all 4 corners of each face is available by just showing
 * 10% of the adjacent walls to distinguish the face being measured."
 *
 * WHAT WAS WRONG BEFORE, because the replacement only makes sense against it:
 * the old maths sized the FOV from the WALLS alone -- for each wall, the angle
 * to its edges and to its floor and ceiling LINES, measured at the
 * perpendicular distance to that wall. The floor and ceiling FACES were never
 * in the formula, and their corners sit at the room's half-DIAGONAL, which is
 * always further away than any wall's perpendicular distance. Then
 * splitEquirect applied ONE FOV to all six faces, so the floor face inherited
 * a number derived from geometry that had never considered it.
 *
 * The result ran the wrong way as rooms grew. A 10x12 room got 105 degrees
 * where its floor needed 117; a 20x18 living room got 101 where its floor
 * needed 141. Small rooms passed by accident -- their wall requirement is so
 * large it covers the floor too -- which is exactly why the failure was
 * intermittent rather than constant, and why it read as "the FOV is a bit
 * small" rather than as a missing term.
 *
 * THE FIX IS THAT THE SIX FACES DISAGREE, and must be allowed to. In a 20x18
 * room the walls want 106 degrees and the floor wants 144. Forcing one number
 * on both either truncates the floor or throws away the wall resolution that
 * every measurement is taken from.
 *
 * Inches throughout, because that is what a tape measure reads and rounding a
 * room to feet moves the answer by degrees.
 *
 * Pure JVM on purpose: this is estimating maths, so it is unit-tested like
 * estimating maths -- and the bench holds a second copy of it, which
 * test_panorama and the projection-contract CI job hold to the same numbers.
 */
object CaptureGeometry {

    /** The border of adjacent wall each face carries beyond its own edges, as
     *  a fraction of that face's own dimension. Amit's "10% of the adjacent
     *  walls to distinguish the face being measured" -- a face cropped exactly
     *  at the corner gives the eye nothing to place it by, and a face that
     *  might be the left wall or the back wall is a face nobody can measure
     *  from with confidence. */
    const val MARGIN_FRACTION = 0.10

    /** A room under 3 ft is a cupboard and a mistyped 200 is a mistyped 20. */
    const val MIN_ROOM_IN = 36.0
    const val MAX_ROOM_IN = 1200.0

    /** Under this a room has no usable headroom; over it, somebody typed feet
     *  into a field that reads inches -- the single likeliest input error, and
     *  it silently halves every FOV if it gets through. */
    const val MIN_CEILING_IN = 72.0
    const val MAX_CEILING_IN = 240.0

    /** Panorama refuses to sample beyond this, so a face needing more cannot
     *  be delivered whatever we compute. */
    const val FOV_CEILING = Panorama.FOV_MAX

    /** Where to stand and how high to hold the camera, in inches from the
     *  room's near-left floor corner. */
    data class Station(val xIn: Double, val yIn: Double, val zIn: Double) {
        val x: Int get() = Math.round(xIn).toInt()
        val y: Int get() = Math.round(yIn).toInt()
        val z: Int get() = Math.round(zIn).toInt()
    }

    /**
     * The whole answer for one room: where to stand, what each face needs, and
     * which faces the projection cannot actually deliver.
     */
    data class Plan(
        val station: Station,
        val fovByFace: Map<String, Double>,
        val lengthIn: Double,
        val widthIn: Double,
        val heightIn: Double,
    ) {
        /** Faces whose requirement runs past what Panorama will sample. Never
         *  silent: a clamped face is a cropped face, and only a person can
         *  decide whether to accept the crop or shoot the room in two halves. */
        val unfittedFaces: List<String>
            get() = fovByFace.filterValues { it > FOV_CEILING }.keys.sorted()

        val fitted: Boolean get() = unfittedFaces.isEmpty()

        /** What to actually hand the splitter: the requirement, clamped to what
         *  the projection can sample. */
        val clampedByFace: Map<String, Double>
            get() = fovByFace.mapValues { (_, v) -> Panorama.clampFov(v) }

        val wallFovDeg: Double get() = fovByFace["front"] ?: 0.0
        val floorFovDeg: Double get() = fovByFace["down"] ?: 0.0
        val ceilingFovDeg: Double get() = fovByFace["up"] ?: 0.0
    }

    /**
     * Stand in the middle of the floor, at half the ceiling height.
     *
     * Centring horizontally is what equalises the two walls of each pair, and
     * half height is the minimising choice in z: raising the camera shrinks
     * what the floor face needs and grows what the ceiling needs by the same
     * geometry, so any departure from the middle makes the worse of the two
     * worse. It also minimises the vertical span each WALL face has to carry.
     */
    fun stationFor(lengthIn: Double, widthIn: Double, heightIn: Double): Station =
        Station(lengthIn / 2.0, widthIn / 2.0, heightIn / 2.0)

    /**
     * Per-face FOV in degrees, keyed by the same face names Panorama uses.
     *
     * A gnomonic face of FOV f, aimed perpendicular at a plane D away, covers
     * a SQUARE on that plane of half-size D*tan(f/2), centred on the foot of
     * the perpendicular -- so the requirement is set by the furthest edge from
     * that foot, per axis, and a square face takes the larger of the two.
     *
     * The floor and ceiling use the half-DIAGONAL rather than the longer side,
     * which is the deliberate cost of not knowing which way the camera is
     * pointing. Amit, 2026-09-21, choosing it: the aligned case needs about 14
     * fewer degrees on a 10x12, and buying them back means the floor truncates
     * whenever he happens to stand rotated -- discovered back at the desk,
     * long after the room is inaccessible.
     */
    fun fovByFace(
        lengthIn: Double,
        widthIn: Double,
        heightIn: Double,
        station: Station = stationFor(lengthIn, widthIn, heightIn),
    ): Map<String, Double> {
        require(lengthIn > 0 && widthIn > 0 && heightIn > 0) { "room dims must be positive" }
        require(station.zIn > 0 && station.zIn < heightIn) { "camera must be inside the room" }

        val m = MARGIN_FRACTION

        // A wall face: distance to its plane, and the furthest edge from the
        // perpendicular foot along each in-plane axis, each with its border.
        fun wall(distance: Double, spanA: Double, offA: Double, spanB: Double, offB: Double): Double {
            val a = offA + m * spanA
            val b = offB + m * spanB
            return 2 * deg(atan(max(a, b) / distance))
        }

        val vertOff = max(station.zIn, heightIn - station.zIn)
        // front/back span the LENGTH and are seen across the width.
        val frontBack = wall(
            distance = min(station.yIn, widthIn - station.yIn),
            spanA = lengthIn, offA = max(station.xIn, lengthIn - station.xIn),
            spanB = heightIn, offB = vertOff,
        )
        // left/right span the WIDTH and are seen across the length.
        val leftRight = wall(
            distance = min(station.xIn, lengthIn - station.xIn),
            spanA = widthIn, offA = max(station.yIn, widthIn - station.yIn),
            spanB = heightIn, offB = vertOff,
        )

        // Floor and ceiling: the furthest floor corner from directly below the
        // camera, plus its border, at any yaw.
        val reach = hypot(
            max(station.xIn, lengthIn - station.xIn),
            max(station.yIn, widthIn - station.yIn),
        ) * (1.0 + m)

        return mapOf(
            "front" to frontBack, "back" to frontBack,
            "left" to leftRight, "right" to leftRight,
            "down" to 2 * deg(atan(reach / station.zIn)),
            "up" to 2 * deg(atan(reach / (heightIn - station.zIn))),
        )
    }

    /**
     * The whole plan for a room given in inches, or null when the numbers are
     * not a room. Returning null rather than a confident wrong answer is the
     * point: a mistyped dimension produces a plausible FOV, and a plausible
     * FOV is indistinguishable from a correct one until the photographs come
     * back cropped.
     */
    fun planForRoom(lengthIn: Double?, widthIn: Double?, heightIn: Double?): Plan? {
        val l = lengthIn ?: return null
        val w = widthIn ?: return null
        val h = heightIn ?: return null
        if (l < MIN_ROOM_IN || w < MIN_ROOM_IN) return null
        if (l > MAX_ROOM_IN || w > MAX_ROOM_IN) return null
        if (h < MIN_CEILING_IN || h > MAX_CEILING_IN) return null
        val station = stationFor(l, w, h)
        return Plan(station, fovByFace(l, w, h, station), l, w, h)
    }

    private fun deg(rad: Double): Double = Math.toDegrees(rad)
    private fun min(a: Double, b: Double): Double = if (a < b) a else b
}
