package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WallCornersTest {

    // Vrushal's living room front wall, mm, seen from 1.5 m away, camera off
    // centre and below mid-height -- the case a symmetric face gets wrong.
    private val wallW = 4364.0
    private val wallH = 2680.0
    private val dist = 1500.0
    private val camX = 2300.0   // from the wall's left end
    private val camZ = 1350.0   // above the floor

    /** Corner rays TL TR BR BL in the Panorama frame (x right, y up, z fwd). */
    private fun cornerRays(tilt: (DoubleArray) -> DoubleArray = { it }): List<DoubleArray> =
        listOf(0.0 to wallH, wallW to wallH, wallW to 0.0, 0.0 to 0.0).map { (a, z) ->
            WallCorners.unit(tilt(doubleArrayOf(a - camX, z - camZ, dist)))
        }

    /** A tripod off level: pitch then roll, a few degrees each. */
    private fun tilted(pDeg: Double, rDeg: Double): (DoubleArray) -> DoubleArray = { v ->
        val p = Math.toRadians(pDeg); val r = Math.toRadians(rDeg)
        val y1 = v[1] * cos(p) - v[2] * sin(p); val z1 = v[1] * sin(p) + v[2] * cos(p)
        val x2 = v[0] * cos(r) - y1 * sin(r); val y2 = v[0] * sin(r) + y1 * cos(r)
        doubleArrayOf(x2, y2, z1)
    }

    @Test fun `a point and its ray round-trip on a face`() {
        val b = WallCorners.basis(37.0, -12.0, 118.0)
        for ((x, y) in listOf(0.1 to 0.2, 0.5 to 0.5, 0.93 to 0.71)) {
            val p = assertNotNull(WallCorners.pointOf(b, WallCorners.rayAt(b, x, y)))
            assertEquals(x, p.first, 1e-9); assertEquals(y, p.second, 1e-9)
        }
    }

    @Test fun `the photo alone gives the wall's proportion`() {
        val m = WallCorners.measure(cornerRays())
        assertTrue(m.valid)
        assertEquals(wallW / wallH, m.ratio, 1e-6)
        assertEquals(wallW, m.lengthFor(wallH), 1e-3)
        assertTrue(m.outOfSquareDeg < 1e-6)
    }

    @Test fun `a tripod off level does not change the measurement`() {
        val m = WallCorners.measure(cornerRays(tilted(3.5, -2.5)))
        assertEquals(wallW / wallH, m.ratio, 1e-6)
        assertTrue(m.square)
    }

    @Test fun `a corner dragged off the wall shows as out of square`() {
        val c = cornerRays().toMutableList()
        c[1] = WallCorners.unit(doubleArrayOf(wallW - camX, wallH - camZ - 400.0, dist + 900.0))
        val m = WallCorners.measure(c)
        assertFalse(m.square, "out by ${m.outOfSquareDeg}")
    }

    @Test fun `crossed corners are refused rather than measured`() {
        val c = cornerRays()
        assertFalse(WallCorners.measure(listOf(c[1], c[0], c[2], c[3])).valid)
    }

    @Test fun `the homography lands each corner on its target`() {
        val src = listOf(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 1.0)
        val dst = listOf(0.12 to 0.2, 0.9 to 0.1, 0.8 to 0.93, 0.05 to 0.8)
        val h = WallCorners.homography(src, dst)
        src.zip(dst).forEach { (s, d) ->
            val (u, v) = WallCorners.apply(h, s.first, s.second)
            assertEquals(d.first, u, 1e-9); assertEquals(d.second, v, 1e-9)
        }
    }

    @Test fun `the elevation fills exactly the wall, whatever the tilt`() {
        // A pano that is white wherever a ray hits the wall rectangle and black
        // elsewhere, photographed by a tilted camera.
        val tilt = tilted(4.0, 3.0)
        val w = 1024; val h = 512
        val px = IntArray(w * h)
        for (row in 0 until h) for (col in 0 until w) {
            val lam = ((col + 0.5) / w - 0.5) * 2 * Math.PI
            val phi = (0.5 - (row + 0.5) / h) * Math.PI
            val d = doubleArrayOf(cos(phi) * sin(lam), sin(phi), cos(phi) * cos(lam))
            // undo the tilt to find where this ray goes in the room
            val inv = untilt(d, 4.0, 3.0)
            if (inv[2] <= 0) continue
            val k = dist / inv[2]
            val a = inv[0] * k + camX; val z = inv[1] * k + camZ
            if (a in 0.0..wallW && z in 0.0..wallH) px[row * w + col] = 0xFFFFFF
        }
        val pano = Panorama.Image(w, h, px)
        val e = WallCorners.elevation(pano, cornerRays(tilt), 300, marginFrac = 0.1)
        assertEquals(wallW / wallH, e.width.toDouble() / e.height, 0.02)
        fun at(fx: Double, fy: Double) = e.pixels[(fy * e.height).toInt() * e.width + (fx * e.width).toInt()] and 0xFF
        // inside the wall: white, at the centre and near each corner
        for ((fx, fy) in listOf(0.5 to 0.5, 0.15 to 0.15, 0.85 to 0.15, 0.85 to 0.85, 0.15 to 0.85))
            assertTrue(at(fx, fy) > 200, "inside at $fx,$fy")
        // the border beyond the corners: black
        for ((fx, fy) in listOf(0.02 to 0.5, 0.98 to 0.5, 0.5 to 0.02, 0.5 to 0.98))
            assertTrue(at(fx, fy) < 50, "border at $fx,$fy")
    }

    private fun untilt(v: DoubleArray, pDeg: Double, rDeg: Double): DoubleArray {
        val p = Math.toRadians(pDeg); val r = Math.toRadians(rDeg)
        val x1 = v[0] * cos(r) + v[1] * sin(r); val y1 = -v[0] * sin(r) + v[1] * cos(r)
        val y2 = y1 * cos(p) + v[2] * sin(p); val z2 = -y1 * sin(p) + v[2] * cos(p)
        return doubleArrayOf(x1, y2, z2)
    }

    @Test fun `start corners from a measured room describe that room's wall`() {
        val plan = assertNotNull(CaptureGeometry.planForRoom(172.0, 119.0, 105.0))
        val front = WallCorners.startCorners("front", 0.0, plan.clampedByFace["front"]!!, plan)
        assertEquals(172.0 / 105.0, WallCorners.measure(front).ratio, 0.02)
        val left = WallCorners.startCorners("left", 270.0, plan.clampedByFace["left"]!!, plan)
        assertEquals(119.0 / 105.0, WallCorners.measure(left).ratio, 0.02)
    }

    @Test fun `ceiling and floor start corners describe the room outline`() {
        val plan = assertNotNull(CaptureGeometry.planForRoom(172.0, 119.0, 105.0))
        for ((face, pitch) in listOf("up" to 90.0, "down" to -90.0)) {
            val c = WallCorners.startCorners(face, 0.0, plan.clampedByFace[face]!!, plan, pitch)
            val m = WallCorners.measure(c)
            assertTrue(m.valid, face)
            assertTrue(m.outOfSquareDeg < 1.0, "$face out by ${m.outOfSquareDeg}")
            // top edge runs along the front wall: the room's length over its width
            assertEquals(172.0 / 119.0, m.ratio, 0.02, face)
        }
    }

    @Test fun `unmeasured rooms still get handles to grab`() {
        val c = WallCorners.startCorners("front", 0.0, 110.0, null)
        assertEquals(4, c.size)
        assertTrue(WallCorners.measure(c).valid)
        assertTrue(abs(WallCorners.measure(c).ratio - 1.0) < 1e-6)
    }
}
