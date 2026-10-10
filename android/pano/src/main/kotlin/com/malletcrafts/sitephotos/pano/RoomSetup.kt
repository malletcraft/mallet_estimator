package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.tan

/**
 * The room from its FOUR FLOOR CORNERS on the straight-down photo, then ONE
 * ceiling line. The maths of the room-setup screen, approved by Amit on
 * 2026-10-10 ("verify yourself if my method looks correct? and if so ...
 * deploy it"), replacing both Match Photo and Claude's corner finder ("Remove
 * it, Amit marks corners").
 *
 * WHY THE FLOOR PHOTO IS ENOUGH: the X3 levels its 360 by gravity, so the face
 * looking straight down (yaw 0, pitch -90) sees a level floor square on. A
 * gnomonic face of a plane parallel to it is a PARALLEL picture of that plane:
 * a floor point (x, -h, z) lands at (a, b) = (x/h, z/h) on the face, exactly,
 * with screen right = +x and screen up = +z = the front wall. So four dots on
 * the floor corners are the floor plan in camera heights, true angles and true
 * proportions, and a room that is not square needs nothing special.
 *
 * The SIZE comes from the carpet: the typed maximum length L (front to back)
 * and width W (left to right). Measured along the front edge FL -> FR and
 * across it, the four dots span Wu and Lu camera heights; the least-squares
 * scale s = (W Wu + L Lu) / (Wu^2 + Lu^2) is mm per camera height, which IS the
 * camera height in mm. How far the two disagree is the check on the dots.
 *
 * Then each wall is shown squared on -- a parallel projection of its plane,
 * between its two floor corners -- with the floor line fixed by the plan, and
 * only the level ceiling line is dragged. It sets the room height H, one value
 * for all four walls.
 *
 * Verified before it was built: exact with exact taps; about 5 mm median error
 * with 0.25 px tap error and about 21 mm at 1 px, which is why the floor photo
 * has a zoom.
 *
 * Frame: Panorama's -- x right, y up, z forward. Floor points are (x, z).
 * Corner order FL FR BR BL, as everywhere else. Pure JVM, so it is tested here.
 */
object RoomSetup {

    /** The floor face's field of view: wide enough for a room's corners from
     *  a tripod at the usual height, narrow enough to keep them sharp. */
    const val FLOOR_FOV = 150.0

    /** Half-width of the floor face in camera heights: tan(FOV/2). */
    val T: Double = tan(Math.toRadians(FLOOR_FOV / 2.0))

    /** Agreement at or under this, as a fraction, and the room may be saved. */
    const val AGREE_MAX = 0.03

    /** Where the ceiling line may go, mm. */
    const val H_MIN = 1800.0
    const val H_MAX = 6000.0

    /** Border beyond the wall's corners on a squared-on wall, of its length. */
    const val WALL_MARGIN = 0.06

    val NAMES = listOf("front-left", "front-right", "back-right", "back-left")

    /** Each wall as seen from inside: name, left corner, right corner. */
    val WALLS: List<Triple<String, Int, Int>> = listOf(
        Triple("front", 0, 1), Triple("right", 1, 2), Triple("back", 2, 3), Triple("left", 3, 0))

    fun wall(name: String): Triple<String, Int, Int> = WALLS.first { it.first == name }

    /** The straight-down face, exactly as Panorama renders every face. */
    fun floorFace(pano: Panorama.Image, px: Int): Panorama.Image =
        Panorama.faceFromEquirect(pano, 0.0, -90.0, FLOOR_FOV, px)

    // ------------------------------------------------- floor face <-> floor

    /**
     * A point on the floor face, as fractions 0..1 across and 0..1 DOWN of a
     * picture [px] pixels wide drawn edge to edge, to (a, b) in camera heights.
     *
     * Exact to faceFromEquirect, whose columns are linspace(-t, t, px) with
     * the endpoints INCLUSIVE: pixel k's centre is at fraction (k + 0.5)/px
     * and at a = -t + k * 2t/(px - 1). A continuous -t..t mapping would be
     * half a pixel out at the edges, and at the edges is where the corners are.
     */
    fun floorPoint(u: Double, v: Double, px: Int): DoubleArray {
        val step = 2.0 * T / (px - 1)
        val a = -T + (u * px - 0.5) * step
        val b = T - (v * px - 0.5) * step
        return doubleArrayOf(a, b)
    }

    /** The inverse: (a, b) in camera heights to fractions on the floor face. */
    fun floorFrac(a: Double, b: Double, px: Int): DoubleArray {
        val step = 2.0 * T / (px - 1)
        return doubleArrayOf(((a + T) / step + 0.5) / px, ((T - b) / step + 0.5) / px)
    }

    /** Where the dots start: the typed carpet, centred, from a guessed camera
     *  height. Only near enough to drag -- the dots are what count. */
    fun startPoints(lengthMm: Double, widthMm: Double, guessHeightMm: Double = 1300.0): List<DoubleArray> {
        val x = widthMm / 2 / guessHeightMm
        val z = lengthMm / 2 / guessHeightMm
        // Kept inside the face, so a big room still starts with grabbable dots.
        val k = min(1.0, 0.85 * T / max(x, z))
        return listOf(doubleArrayOf(-x * k, z * k), doubleArrayOf(x * k, z * k),
            doubleArrayOf(x * k, -z * k), doubleArrayOf(-x * k, -z * k))
    }

    // ------------------------------------------------------------- the plan

    class Plan(
        /** Camera height above the floor, mm. */
        val h: Double,
        /** Floor corners FL FR BR BL as (x, z) mm, camera at the origin. */
        val corners: List<DoubleArray>,
        /** Extent along the front wall (width) and across it (length), mm. */
        val widthMm: Double,
        val lengthMm: Double,
        /** Typed carpet maximums, mm. */
        val typedLength: Double,
        val typedWidth: Double,
        /** Worst disagreement of the two extents with the typed sizes, as a
         *  fraction (0.03 is 3%). */
        val agreement: Double,
    ) {
        /** Each side's length, mm: front, right, back, left. */
        val sidesMm: List<Double> get() = (0 until 4).map { i ->
            val a = corners[i]; val b = corners[(i + 1) % 4]; hypot(b[0] - a[0], b[1] - a[1])
        }
        val agrees: Boolean get() = agreement <= AGREE_MAX
    }

    /**
     * The floor plan from four dots (a, b) in camera heights, FL FR BR BL, and
     * the typed carpet maximums. Null when the dots have no extent.
     */
    fun plan(points: List<DoubleArray>, lengthMm: Double, widthMm: Double): Plan? {
        require(points.size == 4) { "four floor corners, FL FR BR BL" }
        if (lengthMm <= 0 || widthMm <= 0) return null
        var ex = points[1][0] - points[0][0]; var eb = points[1][1] - points[0][1]
        val n = hypot(ex, eb)
        if (n < 1e-9) return null
        ex /= n; eb /= n
        val along = points.map { it[0] * ex + it[1] * eb }
        val across = points.map { it[0] * eb - it[1] * ex }
        val wu = along.max() - along.min()
        val lu = across.max() - across.min()
        val den = wu * wu + lu * lu
        if (den < 1e-12) return null
        val s = (widthMm * wu + lengthMm * lu) / den
        val agree = max(abs(wu * s - widthMm) / widthMm, abs(lu * s - lengthMm) / lengthMm)
        return Plan(s, points.map { doubleArrayOf(it[0] * s, it[1] * s) },
            wu * s, lu * s, lengthMm, widthMm, agree)
    }

    // ------------------------------------------------------ squared-on wall

    /**
     * One wall squared on: a parallel picture of its plane between floor
     * corners [a] and [b] (x, z mm, left then right as seen from inside), at
     * true proportions, [WALL_MARGIN] of its length either side. Rows run from
     * [yTop] down to [yBot], both camera-relative heights in mm (the floor is
     * at -h). [image] is null until rendered.
     */
    class Wall(
        val a: DoubleArray, val b: DoubleArray,
        val h: Double, val yTop: Double, val yBot: Double,
        val widthPx: Int, val heightPx: Int,
        val image: Panorama.Image?,
    ) {
        val lengthMm: Double get() = hypot(b[0] - a[0], b[1] - a[1])
        private val span get() = 1.0 + 2 * WALL_MARGIN

        /** A height above the FLOOR, mm, to a fraction 0..1 down the picture. */
        fun fracOf(aboveFloorMm: Double): Double = (yTop - (aboveFloorMm - h)) / (yTop - yBot)

        /** The inverse: a fraction down the picture to height above floor, mm. */
        fun heightAt(frac: Double): Double = yTop - frac * (yTop - yBot) + h

        /** Where a floor corner falls across the picture: 0 for [a], 1 for [b]. */
        fun xFracOf(s: Double): Double = (s + WALL_MARGIN) / span
    }

    /**
     * The geometry of a squared-on wall for a room height [heightMm] (the
     * picture spans it with headroom so the ceiling line can move) and an
     * output [widthPx]. [render] samples the pano; without it only the frame
     * is worked out, which is what the ceiling drag needs.
     */
    fun wall(
        pano: Panorama.Image?, a: DoubleArray, b: DoubleArray, h: Double,
        heightMm: Double, widthPx: Int, render: Boolean = true,
    ): Wall {
        val len = max(1.0, hypot(b[0] - a[0], b[1] - a[1]))
        val yTop = -h + heightMm * 1.25
        val yBot = -h - WALL_MARGIN * heightMm
        val span = 1.0 + 2 * WALL_MARGIN
        val ew = widthPx.coerceIn(64, Panorama.FACE_PX_MAX)
        val eh = (ew * (yTop - yBot) / (len * span)).roundToInt().coerceIn(ew / 4, ew * 3 / 2)
        if (!render || pano == null) return Wall(a, b, h, yTop, yBot, ew, eh, null)
        val out = IntArray(ew * eh)
        val d = DoubleArray(3)
        for (row in 0 until eh) {
            val wy = yTop - (row + 0.5) / eh * (yTop - yBot)
            for (col in 0 until ew) {
                val s = -WALL_MARGIN + span * (col + 0.5) / ew
                d[0] = a[0] + s * (b[0] - a[0]); d[1] = wy; d[2] = a[1] + s * (b[1] - a[1])
                out[row * ew + col] = WallCorners.sample(pano, d)
            }
        }
        return Wall(a, b, h, yTop, yBot, ew, eh, Panorama.Image(ew, eh, out))
    }

    // ------------------------------------------------------------- the room

    /**
     * The room for the split: floor corners at (x, -h, z), ceiling corners at
     * (x, H - h, z), as directions, into the RoomQuad the elevations are built
     * from. Null when the dots do not close a room round the camera.
     */
    fun roomQuad(plan: Plan, heightMm: Double): RoomQuad? {
        val h = plan.h
        if (heightMm <= h) return null
        val floor = plan.corners.map { WallCorners.unit(doubleArrayOf(it[0], -h, it[1])) }
        val ceiling = plan.corners.map { WallCorners.unit(doubleArrayOf(it[0], heightMm - h, it[1])) }
        return RoomQuad.fromCorners(ceiling, floor)
    }

    // ------------------------------------------------------------ magnifier

    /**
     * A magnifier on a square picture: zoom [z] about centre ([cx], [cy]),
     * fractions of the picture. Scale and slide only -- the picture is never
     * turned or tilted, so what is up on it stays up.
     */
    data class View(val z: Double = 1.0, val cx: Double = 0.5, val cy: Double = 0.5) {
        fun toScreen(u: Double, v: Double): DoubleArray =
            doubleArrayOf((u - cx) * z + 0.5, (v - cy) * z + 0.5)

        fun fromScreen(x: Double, y: Double): DoubleArray =
            doubleArrayOf((x - 0.5) / z + cx, (y - 0.5) / z + cy)

        /** Never past the picture's edge. */
        fun clamped(): View {
            val zz = z.coerceIn(Z_MIN, Z_MAX)
            val half = 0.5 / zz
            return View(zz, cx.coerceIn(half, 1 - half), cy.coerceIn(half, 1 - half))
        }

        /** Zoom by [k], keeping the screen point ([x], [y]) still. */
        fun zoomAt(k: Double, x: Double, y: Double): View {
            val before = fromScreen(x, y)
            val zz = (z * k).coerceIn(Z_MIN, Z_MAX)
            val mid = View(zz, cx, cy)
            val after = mid.fromScreen(x, y)
            return View(zz, cx + before[0] - after[0], cy + before[1] - after[1]).clamped()
        }

        /** Slide by a screen drag of ([dx], [dy]) fractions. */
        fun slid(dx: Double, dy: Double): View = View(z, cx - dx / z, cy - dy / z).clamped()

        /** The visible part of the picture: left, top, side, all fractions. */
        fun window(): DoubleArray = doubleArrayOf(cx - 0.5 / z, cy - 0.5 / z, 1.0 / z)

        companion object {
            const val Z_MIN = 1.0
            const val Z_MAX = 8.0
            const val STEP = 1.4
        }
    }
}
