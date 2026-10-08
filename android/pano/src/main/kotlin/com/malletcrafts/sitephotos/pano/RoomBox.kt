package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * The room as a box around the camera, fitted by dragging its EDGES onto the
 * room's edges in the 360 -- SketchUp's Match Photo, for a room.
 *
 * Amit, 2026-10-08, after trying the eight corner dots on the phone: "setting
 * up corners is still difficult. how bout seting axex like this?" (SketchUp's
 * matching a photo, where axis lines are dragged onto parallel edges rather
 * than points onto corners). Then: "Build straight into APK".
 *
 * A line is easier than a point. A floor line is matched along its whole
 * visible length -- a long skirting, a cornice -- where a dot is aimed at one
 * pixel, and a corner hidden behind a wardrobe does not matter as long as
 * some of the edge shows.
 *
 * SIX NUMBERS ARE THE WHOLE ROOM: the turn of the room against the photo, the
 * distance from the camera to each of the four walls, and the ceiling. Units
 * are the camera's height above the floor (the floor is at y = -1), so the
 * box has a shape before it has a size; the typed room height gives the mm.
 * Every face is a true rectangle by construction, which is why the box is
 * also the strictest check: in a room that is NOT square the lines cannot sit
 * on both edges at once, and that shows on screen.
 *
 * Frame: Panorama's -- x right, y up, z forward at yaw 0. Walls in order
 * front, right, back, left, as the faces.
 */
data class RoomBox(
    /** Turn of the room against the photo, degrees: the front wall faces this yaw. */
    val yawDeg: Double,
    /** Camera to the left, right, front and back walls, in camera heights. */
    val left: Double,
    val right: Double,
    val front: Double,
    val back: Double,
    /** Camera to the ceiling, in camera heights. */
    val ceiling: Double,
) {
    enum class Edge { FLOOR, CEILING, LEFT, RIGHT }

    /** One wall's frame: [n] points at the wall, [t] runs to its right. */
    class Frame(val n: DoubleArray, val t: DoubleArray, val dist: Double, val leftEnd: Double, val rightEnd: Double)

    fun frame(face: String): Frame {
        val i = WALLS.indexOf(face).also { require(it >= 0) { "not a wall: $face" } }
        val phi = Math.toRadians(yawDeg + 90.0 * i)
        val n = doubleArrayOf(sin(phi), 0.0, cos(phi))
        val t = doubleArrayOf(cos(phi), 0.0, -sin(phi))
        // Distance to this wall, and how far its left and right ends run along t.
        return when (face) {
            "front" -> Frame(n, t, front, left, right)
            "right" -> Frame(n, t, right, front, back)
            "back" -> Frame(n, t, back, right, left)
            else -> Frame(n, t, left, back, front)
        }
    }

    /** A point on a wall in that wall's own terms -> a direction from the camera. */
    private fun at(f: Frame, along: Double, y: Double): DoubleArray =
        doubleArrayOf(f.n[0] * f.dist + f.t[0] * along, y, f.n[2] * f.dist + f.t[2] * along)

    /** The four edges of one wall as 3D segments: floor, ceiling, left, right. */
    fun edges(face: String): Map<Edge, Pair<DoubleArray, DoubleArray>> {
        val f = frame(face)
        val bl = at(f, -f.leftEnd, -1.0); val br = at(f, f.rightEnd, -1.0)
        val tl = at(f, -f.leftEnd, ceiling); val tr = at(f, f.rightEnd, ceiling)
        return mapOf(Edge.FLOOR to (bl to br), Edge.CEILING to (tl to tr),
            Edge.LEFT to (bl to tl), Edge.RIGHT to (br to tr))
    }

    /** All twelve edges of the box, for drawing the wireframe. */
    fun allEdges(): List<Pair<DoubleArray, DoubleArray>> = WALLS.flatMap { edges(it).values }

    /** The eight corners, for the elevations: FL FR BR BL, ceiling then floor. */
    fun corners(): RoomCorners {
        val f = frame("front"); val b = frame("back")
        val fl = { y: Double -> at(f, -f.leftEnd, y) }; val fr = { y: Double -> at(f, f.rightEnd, y) }
        val br = { y: Double -> at(b, -b.leftEnd, y) }; val bl = { y: Double -> at(b, b.rightEnd, y) }
        return RoomCorners(
            listOf(fl(ceiling), fr(ceiling), br(ceiling), bl(ceiling)).map { WallCorners.unit(it) },
            listOf(fl(-1.0), fr(-1.0), br(-1.0), bl(-1.0)).map { WallCorners.unit(it) })
    }

    /**
     * One edge dragged so that it passes through [ray] -- the direction under
     * the finger. [at] is where along the edge it was grabbed, 0 at its left
     * (or bottom) end, 1 at its right (or top).
     *
     * FLOOR LINE, grabbed near an END: that end follows the finger and the
     * other end stays put -- the wall swings about it, which turns the whole
     * room. This is how the box is turned to match the photo, and it is what
     * a hand does without being told; a separate turn handle let the box
     * settle on a wrong turn with every line through one point but not along
     * its length (RoomBoxTest caught it). Grabbed in the MIDDLE, the wall only
     * slides nearer or further.
     * CEILING LINE: the ceiling. A VERTICAL: the neighbouring wall at that end.
     */
    fun dragged(face: String, edge: Edge, ray: DoubleArray, at: Double = 0.5): RoomBox {
        val f = frame(face)
        val x = WallCorners.dot(ray, f.t); val z = WallCorners.dot(ray, f.n); val y = ray[1]
        if (z <= 1e-3) return this
        return when (edge) {
            Edge.FLOOR -> when {
                y >= -1e-3 -> this
                at < END_GRAB -> swung(face, ray, pivotRight = true)
                at > 1 - END_GRAB -> swung(face, ray, pivotRight = false)
                else -> withDist(face, (-z / y).coerceIn(MIN, MAX))
            }
            Edge.CEILING -> if (y <= 1e-3) this else copy(ceiling = (f.dist * y / z).coerceIn(MIN, MAX))
            Edge.RIGHT -> if (x <= 1e-3) this else withRight(face, (f.dist * x / z).coerceIn(MIN, MAX))
            Edge.LEFT -> if (x >= -1e-3) this else withLeft(face, (-f.dist * x / z).coerceIn(MIN, MAX))
        }
    }

    /** The floor line swung about one of its ends so it passes through the
     *  floor point under the finger. The fixed end is a corner, so the wall
     *  meeting it there is moved to keep that corner where it was. */
    private fun swung(face: String, ray: DoubleArray, pivotRight: Boolean): RoomBox {
        val i = WALLS.indexOf(face)
        val f = frame(face)
        val p = at(f, if (pivotRight) f.rightEnd else -f.leftEnd, -1.0)
        val qx = -ray[0] / ray[1]; val qz = -ray[2] / ray[1]
        var dx = qx - p[0]; var dz = qz - p[2]
        val len = kotlin.math.hypot(dx, dz)
        if (len < 1e-6) return this
        dx /= len; dz /= len
        if (dx * f.t[0] + dz * f.t[2] < 0) { dx = -dx; dz = -dz }
        // t = (cos phi, -sin phi) on the floor, so phi = atan2(-t.z, t.x).
        val phi = Math.toDegrees(atan2(-dz, dx))
        val turned = copy(yawDeg = wrap(phi - 90.0 * i))
        val nf = turned.frame(face)
        val d = p[0] * nf.n[0] + p[2] * nf.n[2]
        if (d < MIN) return this
        val adj = WALLS[if (pivotRight) (i + 1) % 4 else (i + 3) % 4]
        val na = turned.frame(adj)
        val da = p[0] * na.n[0] + p[2] * na.n[2]
        if (da < MIN) return this
        return turned.withDist(face, d.coerceIn(MIN, MAX)).withDist(adj, da.coerceIn(MIN, MAX))
    }

    private fun withDist(face: String, d: Double) = when (face) {
        "front" -> copy(front = d); "right" -> copy(right = d); "back" -> copy(back = d); else -> copy(left = d)
    }
    private fun withRight(face: String, d: Double) = when (face) {
        "front" -> copy(right = d); "right" -> copy(back = d); "back" -> copy(left = d); else -> copy(front = d)
    }
    private fun withLeft(face: String, d: Double) = when (face) {
        "front" -> copy(left = d); "right" -> copy(front = d); "back" -> copy(right = d); else -> copy(back = d)
    }

    /** Millimetres per box unit, given the room's height floor to ceiling. */
    fun mmPerUnit(heightMm: Double): Double = heightMm / (1.0 + ceiling)

    /** Length (front/back walls), width (side walls) and camera height, in mm. */
    fun sizeMm(heightMm: Double): Triple<Double, Double, Double> {
        val u = mmPerUnit(heightMm)
        return Triple((left + right) * u, (front + back) * u, u)
    }

    /**
     * Where to look to see one whole wall: the bearing of its middle, and a
     * field of view that holds both its ends, floor to ceiling, with a little
     * to spare. The wall is shown square to the camera's line of sight.
     */
    fun view(face: String): Pair<Double, Double> {
        val f = frame(face)
        val mid = (f.rightEnd - f.leftEnd) / 2
        val yaw = Math.toDegrees(atan2(WallCorners.dot(at(f, mid, 0.0), doubleArrayOf(1.0, 0.0, 0.0)),
            WallCorners.dot(at(f, mid, 0.0), doubleArrayOf(0.0, 0.0, 1.0))))
        val b = WallCorners.basis(yaw, 0.0, 90.0)
        var k = 0.3
        for (p in listOf(at(f, -f.leftEnd, -1.0), at(f, f.rightEnd, -1.0),
                         at(f, -f.leftEnd, ceiling), at(f, f.rightEnd, ceiling))) {
            val zz = WallCorners.dot(p, b.f)
            if (zz > 1e-3) k = max(k, max(abs(WallCorners.dot(p, b.r) / zz), abs(WallCorners.dot(p, b.u) / zz)))
        }
        val fov = Math.toDegrees(2 * atan(k * 1.15)).coerceIn(60.0, Panorama.FOV_MAX)
        return yaw to fov
    }

    companion object {
        val WALLS = listOf("front", "right", "back", "left")
        private const val MIN = 0.15
        /** An angle into (-180, 180], so the same turn always reads the same. */
        fun wrap(deg: Double): Double { var d = deg % 360.0; if (d > 180) d -= 360; if (d <= -180) d += 360; return d }
        /** Grabbed within this fraction of an end, a floor line swings. */
        const val END_GRAB = 0.3
        private const val MAX = 40.0

        /**
         * A first box. With length and width typed, that room with the camera
         * at its centre and half height; otherwise a room three ceilings
         * across. Either way it only has to be near enough to drag.
         */
        fun start(lengthMm: Double?, widthMm: Double?, heightMm: Double?): RoomBox {
            val h = heightMm ?: 2700.0
            val cam = h / 2
            val l = lengthMm ?: (h * 1.5)
            val w = widthMm ?: (h * 1.5)
            return RoomBox(0.0, l / 2 / cam, l / 2 / cam, w / 2 / cam, w / 2 / cam, (h - cam) / cam)
        }
    }
}
