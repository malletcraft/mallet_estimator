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
 * The room from TWO DIAGONALLY OPPOSITE CORNERS, each set the way SketchUp's
 * Match Photo is set: an ORIGIN grip on the floor corner, and for each of the
 * two walls leaving it a PAIR of perspective bars -- one on the skirting, one
 * on the cornice -- each with a handle at both ends.
 *
 * Amit, 2026-10-08: "get me exact setup like sketchup. i will set only two
 * diagonally opposite corners of a room" (the room box failing on "Room not
 * square"), and on 0.3.174's single line per wall, "not useful": "i need set
 * origin and then vanishing perspective lines just as foto match of sketchup
 * using handles . setting ceiling and floor lines running from origing to
 * other direction is easiest way along with floor corner."
 *
 * WHY PAIRS: a wall's floor and ceiling edges are parallel, so in the photo
 * they run to one vanishing point, and that point IS the wall's direction --
 * SketchUp's red and green axes. In a 360 each bar is a plane through the
 * camera, and the two planes of a pair meet along exactly that direction, so
 * no focal length or horizon has to be guessed. The bars need not touch the
 * origin; lay them wherever the edge shows clearly. The ceiling bars also
 * give the ceiling: where their plane crosses the vertical over the origin.
 *
 * Two corners alone fix only a rectangle, so each wall keeps its OWN
 * direction: front and left from the front-left corner, back and right from
 * the back-right. The other two corners are where the wall lines cross. Nine
 * numbers in all -- two floor points, four directions, the ceiling -- which is
 * any four-sided room with a flat ceiling. Walls stay vertical, so every WALL
 * is a true rectangle and its elevation is exact; only the floor and ceiling
 * take the room's own shape.
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
    /** The two corners that are set; the other two follow. */
    enum class Origin { FL, BR }

    /** The four walls' lines: front and left leave FL, back and right leave BR. */
    enum class Line { FRONT, LEFT, RIGHT, BACK }

    /**
     * One wall's perspective bars, as SketchUp draws them: a floor bar and a
     * ceiling bar, each two handles. Every handle is a DIRECTION from the
     * camera (what is under the finger), so the bars stay where they were
     * put whatever the room does.
     */
    class Bars(val floorA: DoubleArray, val floorB: DoubleArray, val ceilA: DoubleArray, val ceilB: DoubleArray) {
        fun handles() = listOf(floorA, floorB, ceilA, ceilB)
        fun with(i: Int, ray: DoubleArray): Bars {
            val h = handles().toMutableList(); h[i] = ray
            return Bars(h[0], h[1], h[2], h[3])
        }
    }

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

    fun lines(o: Origin) = if (o == Origin.FL) listOf(Line.FRONT, Line.LEFT) else listOf(Line.BACK, Line.RIGHT)

    fun originOf(l: Line) = if (l == Line.FRONT || l == Line.LEFT) Origin.FL else Origin.BR

    fun pointOf(o: Origin): DoubleArray = if (o == Origin.FL) doubleArrayOf(flX, flZ) else doubleArrayOf(brX, brZ)

    fun angleOf(l: Line) = when (l) {
        Line.FRONT -> frontDeg; Line.LEFT -> leftDeg; Line.RIGHT -> rightDeg; Line.BACK -> backDeg
    }

    /** A corner's vertical, floor to ceiling, for drawing the origin. */
    fun vertical(o: Origin): Pair<DoubleArray, DoubleArray> {
        val p = pointOf(o)
        return p3(p, -1.0) to p3(p, ceiling)
    }

    /**
     * Where a wall's bars start before anybody drags them: on this room's
     * floor and ceiling edge of that wall, from [near] to [far] of the way
     * along it from its corner -- close to the origin, as Amit asked, and
     * where a corner view still shows them.
     */
    fun startBars(l: Line, near: Double = 0.12, far: Double = 0.45): Bars {
        val p = requireNotNull(floorPlan())
        val (from, to) = when (l) {
            Line.FRONT -> p[0] to p[1]; Line.LEFT -> p[0] to p[3]
            Line.RIGHT -> p[2] to p[1]; Line.BACK -> p[2] to p[3]
        }
        fun at(k: Double, y: Double) = WallCorners.unit(doubleArrayOf(
            from[0] + (to[0] - from[0]) * k, y, from[1] + (to[1] - from[1]) * k))
        return Bars(at(near, -1.0), at(far, -1.0), at(near, ceiling), at(far, ceiling))
    }

    /**
     * The room with one corner set from Match Photo: its origin under
     * [originRay], each of its two walls along the vanishing direction of its
     * bars, and the ceiling where the ceiling bars cross the vertical over
     * the origin. The other corner is untouched. A setting that does not
     * close a room round the camera is refused, and the room stays as it was.
     */
    fun fitted(o: Origin, originRay: DoubleArray, bars: Map<Line, Bars>): RoomQuad {
        if (originRay[1] >= -1e-3) return this
        val ox = -originRay[0] / originRay[1]; val oz = -originRay[2] / originRay[1]
        if (hypot(ox, oz) > MAX) return this
        val angles = lines(o).map { l ->
            val b = bars[l] ?: return this
            val d = direction(b) ?: return this
            var dx = d[0]; var dz = d[2]
            // The pair gives a line, not a way along it: point it away from the
            // origin, towards where the floor bar was laid.
            val towards = floorPoint(b.floorA)?.let { a -> floorPoint(b.floorB)?.let { c ->
                doubleArrayOf((a[0] + c[0]) / 2 - ox, (a[1] + c[1]) / 2 - oz) } }
                ?: Math.toRadians(angleOf(l)).let { doubleArrayOf(cos(it), sin(it)) }
            if (dx * towards[0] + dz * towards[1] < 0) { dx = -dx; dz = -dz }
            l to Math.toDegrees(atan2(dz, dx))
        }.toMap()
        val ceils = lines(o).mapNotNull { l -> ceilingOver(bars.getValue(l), ox, oz) }
        if (ceils.isEmpty()) return this
        val c = ceils.average().coerceIn(MIN, MAX)
        val next = if (o == Origin.FL)
            copy(flX = ox, flZ = oz, ceiling = c,
                frontDeg = angles.getValue(Line.FRONT), leftDeg = angles.getValue(Line.LEFT))
        else
            copy(brX = ox, brZ = oz, ceiling = c,
                backDeg = angles.getValue(Line.BACK), rightDeg = angles.getValue(Line.RIGHT))
        return if (next.valid) next else this
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

    /** Where to look to see a corner and its two walls leaving it. */
    fun cornerView(o: Origin): Pair<Double, Double> {
        val p = pointOf(o)
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

        private fun crossV(a: DoubleArray, b: DoubleArray) = doubleArrayOf(
            a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])

        /**
         * A pair's vanishing direction: each bar and the camera make a plane,
         * and the two planes meet along the wall. Null when a bar has no
         * length or the bars lie in one plane.
         */
        fun direction(b: Bars): DoubleArray? {
            val n1 = crossV(b.floorA, b.floorB); val n2 = crossV(b.ceilA, b.ceilB)
            val d = crossV(n1, n2)
            val l = kotlin.math.sqrt(WallCorners.dot(d, d))
            val scale = kotlin.math.sqrt(WallCorners.dot(n1, n1) * WallCorners.dot(n2, n2))
            if (scale < 1e-12 || l < 1e-6 * scale) return null
            return doubleArrayOf(d[0] / l, d[1] / l, d[2] / l)
        }

        /** Where the ceiling bar's plane crosses the vertical over (x, z). */
        private fun ceilingOver(b: Bars, x: Double, z: Double): Double? {
            val n = crossV(b.ceilA, b.ceilB)
            if (abs(n[1]) < 1e-9) return null
            val y = -(n[0] * x + n[2] * z) / n[1]
            return if (y > 1e-3) y else null
        }

        private fun floorPoint(r: DoubleArray): DoubleArray? =
            if (r[1] < -1e-3) doubleArrayOf(-r[0] / r[1], -r[2] / r[1]) else null
        private const val MIN = 0.15
        private const val MAX = 40.0
        /** Wide enough to see a corner and a good run of both walls. */
        const val CORNER_FOV = 120.0

        /**
         * The room from its eight corners as directions -- since 2026-10-10
         * what the room-setup screen (RoomSetup: four floor corners on the
         * straight-down photo, then the ceiling line) hands the split. The
         * floor corners fix the floor plan (floor at y = -1); each ceiling
         * corner, over its floor corner, gives the ceiling, and the median of
         * the four is taken. FL and BR become the two origins, and the four
         * wall lines run through neighbouring floor corners, so the room
         * reproduces all four floor corners exactly.
         *
         * Null when a floor corner is not below the horizon, or the corners do
         * not close a room round the camera.
         */
        fun fromCorners(ceiling: List<DoubleArray>, floor: List<DoubleArray>): RoomQuad? {
            if (ceiling.size != 4 || floor.size != 4) return null
            val p = floor.map { floorPoint(it) ?: return null }
            if (p.any { hypot(it[0], it[1]) > MAX }) return null
            val heights = (0 until 4).mapNotNull { i ->
                val c = ceiling[i]; val run = hypot(c[0], c[2])
                if (run < 1e-6 || c[1] <= 0) null else c[1] / run * hypot(p[i][0], p[i][1])
            }.sorted()
            if (heights.isEmpty()) return null
            val ceil = (if (heights.size % 2 == 1) heights[heights.size / 2]
                        else (heights[heights.size / 2 - 1] + heights[heights.size / 2]) / 2).coerceIn(MIN, MAX)
            fun deg(a: DoubleArray, b: DoubleArray) = Math.toDegrees(atan2(b[1] - a[1], b[0] - a[0]))
            val q = RoomQuad(p[0][0], p[0][1], p[2][0], p[2][1], ceil,
                frontDeg = deg(p[0], p[1]), leftDeg = deg(p[0], p[3]),
                rightDeg = deg(p[2], p[1]), backDeg = deg(p[2], p[3]))
            return if (q.valid) q else null
        }

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
