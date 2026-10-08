package com.malletcrafts.sitephotos.pano

import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RoomCornersTest {

    private fun angleDeg(a: DoubleArray, b: DoubleArray) =
        Math.toDegrees(acos(WallCorners.dot(WallCorners.unit(a), WallCorners.unit(b)).coerceIn(-1.0, 1.0)))

    @Test fun `eight points make all six faces of a measured room`() {
        val plan = assertNotNull(CaptureGeometry.planForRoom(172.0, 119.0, 105.0))
        val rc = RoomCorners.start(plan)
        val want = mapOf(
            "front" to 172.0 / 105.0, "back" to 172.0 / 105.0,
            "left" to 119.0 / 105.0, "right" to 119.0 / 105.0,
            "up" to 172.0 / 119.0, "down" to 172.0 / 119.0)
        for ((face, ratio) in want) {
            val m = WallCorners.measure(rc.forFace(face))
            assertTrue(m.valid, face)
            assertTrue(m.outOfSquareDeg < 0.5, "$face out by ${m.outOfSquareDeg}")
            assertEquals(ratio, m.ratio, 0.01, face)
        }
    }

    @Test fun `neighbouring walls share their corner`() {
        val rc = RoomCorners.start(null)
        // front's right edge is right's left edge
        val front = rc.forFace("front"); val right = rc.forFace("right")
        assertTrue(angleDeg(front[1], right[0]) < 1e-9)
        assertTrue(angleDeg(front[2], right[3]) < 1e-9)
    }

    @Test fun `an unmeasured room still gives six valid faces`() {
        val rc = RoomCorners.start(null)
        for (f in listOf("front", "right", "back", "left", "up", "down"))
            assertTrue(WallCorners.measure(rc.forFace(f)).valid, f)
    }

    // A pano of one bright rectangular wall on black: its corners are real
    // corners for the snap to find.
    private val dist = 1500.0
    private val wallW = 4000.0; private val wallH = 2600.0
    private val camX = 2000.0; private val camZ = 1300.0
    private fun wallPano(w: Int = 2048, h: Int = 1024): Panorama.Image {
        val px = IntArray(w * h)
        for (row in 0 until h) for (col in 0 until w) {
            val lam = ((col + 0.5) / w - 0.5) * 2 * Math.PI
            val phi = (0.5 - (row + 0.5) / h) * Math.PI
            val d = doubleArrayOf(cos(phi) * sin(lam), sin(phi), cos(phi) * cos(lam))
            if (d[2] <= 0) continue
            val k = dist / d[2]
            val a = d[0] * k + camX; val z = d[1] * k + camZ
            if (a in 0.0..wallW && z in 0.0..wallH) px[row * w + col] = 0xE0E0E0
        }
        return Panorama.Image(w, h, px)
    }
    private fun ray(a: Double, z: Double) = WallCorners.unit(doubleArrayOf(a - camX, z - camZ, dist))

    @Test fun `a corner placed roughly snaps onto the real one`() {
        val pano = wallPano()
        val truth = ray(wallW, 0.0)              // bottom-right corner of the wall
        val rough = ray(wallW - 120.0, 100.0)     // a thumb's width off
        assertTrue(angleDeg(rough, truth) > 1.0)
        val snapped = RoomCorners.snap(pano, rough)
        assertTrue(angleDeg(snapped, truth) < 0.4, "still ${angleDeg(snapped, truth)} deg off")
    }

    @Test fun `on a plain surface the snap leaves the point alone`() {
        val pano = wallPano()
        val middle = ray(wallW / 2, wallH / 2)
        assertTrue(angleDeg(RoomCorners.snap(pano, middle), middle) < 1e-9)
    }
}
