package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * The room from TWO DIAGONALLY OPPOSITE CORNERS and the four wall lines that
 * leave them -- SketchUp's Match Photo origin and axis lines, for a room that
 * need not be square.
 *
 * Amit, 2026-10-08, after the room box: "tried. it . its not good yet. having
 * diffrent perspective line for opposite corners will work?" and then "get me
 * exact setup like sketchup. i will set only two diagonally opposite corners
 * of a room . will that work?" -- the hardest thing on the box being "Room not
 * square". A box has one turn for all four walls, so in a room whose walls
 * are not at 90 degrees no line can sit on two edges at once.
 *
 * Two corners alone fix a room only if it is a true rectangle. So each placed
 * corner also sends out its two wall lines, as SketchUp's origin sends out
 * the red and green axes: front-left (FL) carries the front and left walls,
 * back-right (BR) the right and back walls. Each line has its OWN angle. The
 * other two corners are where the lines cross: front-right where the front
 * line meets the right line, back-left where the left line meets the back.
 * Nine numbers in all -- two floor points, four angles, the ceiling -- which
 * is exactly a four-sided room of any shape with a flat ceiling.
 *
 * Walls stay vertical and floor and ceiling flat, so every WALL is still a
 * true rectangle and its elevation is exact; only the floor and ceiling take
 * the room's own shape.
 *
 * Units are camera heights (floor at y = -1), so the room has a shape before
 * it has a size; the typed room height gives the mm. Frame: Panorama's -- x
 * right, y up, z forward. Floor points are (x, z). A wall's angle is the
 * direction of its line on the floor, (cos a, sin a) in (x, z), pointing away
 * from the corner that carries it.
 */
data class RoomQuad(
    val flX: Double, val flZ: Double,
    val brX: Double, val brZ: Double,
    /** Camera to the ceiling, in camera heights. */
    val ceiling: Double,
    /** Wall lines, degrees: front and left leave FL, right and back leave BR. */
    val frontDeg: Double, val leftDeg: Double,
    val rightDeg: Double, val backDeg: Double,
) {
    /** What a finger can hold. The two corners have a floor and a ceiling end. */
    enum class Handle { FL_FLOOR, FL_CEILING, BR_FLOOR, BR_CEILING, FRONT, LEFT, RIGHT, BACK }

    private fun dir(deg: Double) = Math.toRadians(deg).let { doubleArrayOf(cos(it), sin(it)) }

    /** Where two floor lines cross, or null if they are (nearly) parallel or
     *  meet behind either corner. */
    private fun cross(p: DoubleArray, a: Double, q: DoubleArray, b: Double): DoubleArray? {
        val u = dir(a); val v = dir(b)
        val den = u[0] * v[1] - u[1] * v[0]
        if (abs(den) < 1e-6) return null
        val dx = q[0] - p[0]; val dz = q[1] - p[1]
        val s = (dx * v[1] - dz * v[0]) / den
        val t = (dx * u[1] - dz * u[0]) / den
        if (s <= 0 || t <= 0) return null
        return doubleArrayOf(p[0] + s * u[0], p[1] + s * u[1])
    }

    /** The four floor corners FL FR BR BL as (x, z), or null when the lines do
     *  not close into a room round the camera. */
    fun floorPlan(): List<DoubleArray>? {
        val fl = doubleArrayOf(flX, flZ); val br = doubleArrayOf(brX, brZ)
        val fr = cross(fl, frontDeg, br, rightDeg) ?: return null
        val bl = cross(fl, leftDeg, br, backDeg) ?: return null
        val pts = listOf(fl, fr, br, bl)
        // Convex, and the camera inside: seen from above FL FR BR BL runs
        // clockwise, so the camera is on the same (negative) side of every wall.
        for (i in 0 until 4) {
            val a = pts[i]; val b = pts[(i + 1) % 4]; val c = pts[(i + 2) % 4]
            val ex = b[0] - a[0]; val ez = b[1] - a[1]
            if (ex * (-a[1]) - ez * (-a[0]) >= -1e-9) return null
            if (ex * (c[1] - a[1]) - ez * (c[0] - a[0]) >= -1e-9) return null
        }
        return pts
    }

    val valid: Boolean get() = floorPlan() != null

    private fun p3(xz: DoubleArray, y: Double) = doubleArrayOf(xz[0], y, xz[1])

    /** The eight corners, for the elevations: FL FR BR BL, ceiling then floor. */
    fun corners(): RoomCorners {
        val p = requireNotNull(floorPlan()) { "the wall lines do not close a room" }
        return RoomCorners(p.map { WallCorners.unit(p3(it, ceiling)) }, p.map { WallCorners.unit(p3(it, -1.0)) })
    }

    /** One wall's ends on the floor, left then right as seen from inside. */
    fun wallEnds(face: String): Pair<DoubleArray, DoubleArray> {
        val p = requireNotNull(floorPlan())
        return when (face) {
            "front" -> p[0] to p[1]; "right" -> p[1] to p[2]
            "back" -> p[2] to p[3]; "left" -> p[3] to p[0]
            else -> error("not a wall: $face")
        }
    }

    /** Every edge of the room as 3D segments, for drawing the wireframe. */
    fun allEdges(): List<Pair<DoubleArray, DoubleArray>> {
        val p = floorPlan() ?: return emptyList()
        return (0 until 4).flatMap { i ->
            val a = p[i]; val b = p[(i + 1) % 4]
            listOf(p3(a, -1.0) to p3(b, -1.0), p3(a, ceiling) to p3(b, ceiling), p3(a, -1.0) to p3(a, ceiling))
        }
    }

    /** One wall's floor and ceiling edges, for highlighting and grabbing. */
    fun wallEdges(face: String): Pair<Pair<DoubleArray, DoubleArray>, Pair<DoubleArray, DoubleArray>> {
        val (a, b) = wallEnds(face)
        return (p3(a, -1.0) to p3(b, -1.0)) to (p3(a, ceiling) to p3(b, ceiling))
    }

    /** The handle a wall's floor line is turned by on a wall screen. */
    fun lineOf(face: String): Handle = when (face) {
        "front" -> Handle.FRONT; "left" -> Handle.LEFT; "right" -> Handle.RIGHT; "back" -> Handle.BACK
        else -> error("not a wall: $face")
    }

    /** The placed corner a wall line pivots about. */
    fun pivotOf(h: Handle): DoubleArray = when (h) {
        Handle.FRONT, Handle.LEFT, Handle.FL_FLOOR, Handle.FL_CEILING -> doubleArrayOf(flX, flZ)
        else -> doubleArrayOf(brX, brZ)
    }

    private fun angleOf(h: Handle) = when (h) {
        Handle.FRONT -> frontDeg; Handle.LEFT -> leftDeg; Handle.RIGHT -> rightDeg; Handle.BACK -> backDeg
        else -> error("not a line: $h")
    }

    /** A wall line from its corner, [length] camera heights long, on the floor. */
    fun ray3d(h: Handle, length: Double): Pair<DoubleArray, DoubleArray> {
        val p = pivotOf(h); val u = dir(angleOf(h))
        return p3(p, -1.0) to doubleArrayOf(p[0] + u[0] * length, -1.0, p[1] + u[1] * length)
    }

    /** A placed corner's vertical, floor to ceiling. */
    fun vertical(h: Handle): Pair<DoubleArray, DoubleArray> {
        val p = pivotOf(h)
        return p3(p, -1.0) to p3(p, ceiling)
    }

    /**
     * [h] moved so that it lies under [ray], the direction under the finger.
     * A change that would leave the lines not closing a room round the camera
     * is refused -- the room stays where it was rather than flying apart.
     *
     * A CORNER's floor end goes to the floor point under the finger; its
     * ceiling end sets the ceiling. A LINE turns about its own corner until it
     * passes through the floor point under the finger, and no other line
     * moves -- which is the whole difference from the box.
     */
    fun dragged(h: Handle, ray: DoubleArray): RoomQuad {
        val next = when (h) {
            Handle.FL_FLOOR, Handle.BR_FLOOR -> {
                if (ray[1] >= -1e-3) return this
                val x = -ray[0] / ray[1]; val z = -ray[2] / ray[1]
                if (hypot(x, z) > MAX) return this
                if (h == Handle.FL_FLOOR) copy(flX = x, flZ = z) else copy(brX = x, brZ = z)
            }
            Handle.FL_CEILING, Handle.BR_CEILING -> {
                if (ray[1] <= 1e-3) return this
                val p = pivotOf(h)
                copy(ceiling = (ray[1] * hypot(p[0], p[1]) / hypot(ray[0], ray[2])).coerceIn(MIN, MAX))
            }
            else -> {
                if (ray[1] >= -1e-3) return this
                val p = pivotOf(h)
                val dx = -ray[0] / ray[1] - p[0]; val dz = -ray[2] / ray[1] - p[1]
                if (hypot(dx, dz) < 1e-3) return this
                val a = Math.toDegrees(atan2(dz, dx))
                when (h) {
                    Handle.FRONT -> copy(frontDeg = a); Handle.LEFT -> copy(leftDeg = a)
                    Handle.RIGHT -> copy(rightDeg = a); else -> copy(backDeg = a)
                }
            }
        }
        return if (next.valid) next else this
    }

    /**
     * The ceiling set from a point on one wall's ceiling line: the finger's
     * ray is carried to that wall's plane and its height read off there.
     */
    fun ceilingFrom(face: String, ray: DoubleArray): RoomQuad {
        if (ray[1] <= 1e-3) return this
        val (a, b) = wallEnds(face)
        val ux = b[0] - a[0]; val uz = b[1] - a[1]
        val den = ray[0] * uz - ray[2] * ux
        if (abs(den) < 1e-9) return this
        val s = (a[0] * uz - a[1] * ux) / den
        if (s <= 0) return this
        return copy(ceiling = (ray[1] * s).coerceIn(MIN, MAX))
    }

    /** Millimetres per room unit, given the room's height floor to ceiling. */
    fun mmPerUnit(heightMm: Double): Double = heightMm / (1.0 + ceiling)

    /** Each wall's length in mm, front right back left. */
    fun wallsMm(heightMm: Double): List<Double> {
        val p = requireNotNull(floorPlan()); val u = mmPerUnit(heightMm)
        return (0 until 4).map { i -> val a = p[i]; val b = p[(i + 1) % 4]; hypot(b[0] - a[0], b[1] - a[1]) * u }
    }

    /** The inside angle at each corner, FL FR BR BL, degrees. 90 is square. */
    fun cornerAngles(): List<Double> {
        val p = requireNotNull(floorPlan())
        return (0 until 4).map { i ->
            val a = p[(i + 3) % 4]; val o = p[i]; val b = p[(i + 1) % 4]
            val ux = a[0] - o[0]; val uz = a[1] - o[1]; val vx = b[0] - o[0]; val vz = b[1] - o[1]
            Math.toDegrees(acos(((ux * vx + uz * vz) / (hypot(ux, uz) * hypot(vx, vz))).coerceIn(-1.0, 1.0)))
        }
    }

    /** Where to look to see a placed corner and its two walls leaving it. */
    fun cornerView(h: Handle): Pair<Double, Double> {
        val p = pivotOf(h)
        return Math.toDegrees(atan2(p[0], p[1])) to CORNER_FOV
    }

    /**
     * Where to look to see one whole wall: the bearing of its middle, and a
     * field of view that holds both its ends, floor to ceiling, with a little
     * to spare.
     */
    fun view(face: String): Pair<Double, Double> {
        val (a, b) = wallEnds(face)
        val yaw = Math.toDegrees(atan2((a[0] + b[0]) / 2, (a[1] + b[1]) / 2))
        val bs = WallCorners.basis(yaw, 0.0, 90.0)
        var k = 0.3
        for (q in listOf(p3(a, -1.0), p3(b, -1.0), p3(a, ceiling), p3(b, ceiling))) {
            val zz = WallCorners.dot(q, bs.f)
            if (zz > 1e-3) k = max(k, max(abs(WallCorners.dot(q, bs.r) / zz), abs(WallCorners.dot(q, bs.u) / zz)))
        }
        val fov = Math.toDegrees(2 * atan(k * 1.15)).coerceIn(60.0, Panorama.FOV_MAX)
        return yaw to fov
    }

    companion object {
        val WALLS = listOf("front", "right", "back", "left")
        private const val MIN = 0.15
        private const val MAX = 40.0
        /** Wide enough to see a corner and a good run of both walls. */
        const val CORNER_FOV = 120.0

        /**
         * A first room, square. With length and width typed, that room with
         * the camera at its centre and half height; otherwise a room three
         * ceilings across. Either way it only has to be near enough to drag.
         */
        fun start(lengthMm: Double?, widthMm: Double?, heightMm: Double?): RoomQuad {
            val h = heightMm ?: 2700.0
            val cam = h / 2
            val l = (lengthMm ?: (h * 1.5)) / 2 / cam
            val w = (widthMm ?: (h * 1.5)) / 2 / cam
            return RoomQuad(-l, w, l, -w, (h - cam) / cam,
                frontDeg = 0.0, leftDeg = -90.0, rightDeg = 90.0, backDeg = 180.0)
        }
    }
}
