package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomQuadTest {

    // A room that is NOT square -- Amit's case, 2026-10-08 -- with the camera
    // off centre. No corner is 90 degrees and no two walls are parallel.
    private val truth = RoomQuad(-1.4, 1.2, 1.8, -1.5, 0.95,
        frontDeg = 4.0, leftDeg = -93.0, rightDeg = 87.5, backDeg = 178.0)

    /** The direction from the camera to a floor point, as a finger would give it. */
    private fun floorRay(x: Double, z: Double) = WallCorners.unit(doubleArrayOf(x, -1.0, z))
    private fun along(a: DoubleArray, b: DoubleArray, k: Double) =
        floorRay(a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k)

    @Test fun `the truth room is valid and not square`() {
        assertTrue(truth.valid)
        assertTrue(truth.cornerAngles().any { abs(it - 90) > 3 })
        assertEquals(360.0, truth.cornerAngles().sum(), 1e-9)
    }

    @Test fun `two corners and four lines find a room that is not square`() {
        val p = truth.floorPlan()!!
        var q = RoomQuad.start(null, null, null)
        // the two corners, floor end then ceiling end
        q = q.dragged(RoomQuad.Handle.FL_FLOOR, floorRay(p[0][0], p[0][1]))
        q = q.dragged(RoomQuad.Handle.BR_FLOOR, floorRay(p[2][0], p[2][1]))
        q = q.dragged(RoomQuad.Handle.FL_CEILING, WallCorners.unit(doubleArrayOf(p[0][0], truth.ceiling, p[0][1])))
        // each line laid on its skirting somewhere along the wall
        q = q.dragged(RoomQuad.Handle.FRONT, along(p[0], p[1], 0.6))
        q = q.dragged(RoomQuad.Handle.LEFT, along(p[0], p[3], 0.5))
        q = q.dragged(RoomQuad.Handle.RIGHT, along(p[2], p[1], 0.7))
        q = q.dragged(RoomQuad.Handle.BACK, along(p[2], p[3], 0.4))
        val got = q.floorPlan()!!
        for (i in 0 until 4) for (k in 0..1) assertEquals(p[i][k], got[i][k], 1e-9, "corner $i")
        assertEquals(truth.ceiling, q.ceiling, 1e-9)
        for ((a, b) in q.cornerAngles().zip(truth.cornerAngles())) assertEquals(b, a, 1e-9)
    }

    @Test fun `turning one line moves no other line`() {
        val q = truth.dragged(RoomQuad.Handle.FRONT, floorRay(0.5, 1.4))
        assertEquals(truth.leftDeg, q.leftDeg); assertEquals(truth.rightDeg, q.rightDeg)
        assertEquals(truth.backDeg, q.backDeg)
        assertEquals(truth.flX, q.flX); assertEquals(truth.brZ, q.brZ)
        assertTrue(abs(q.frontDeg - truth.frontDeg) > 1)
    }

    @Test fun `a drag that would open the room is refused`() {
        // the front line swung to point back at the camera
        assertEquals(truth, truth.dragged(RoomQuad.Handle.FRONT, floorRay(-1.3, -0.5)))
        // a corner dragged above the horizon has no floor point
        assertEquals(truth, truth.dragged(RoomQuad.Handle.FL_FLOOR, WallCorners.unit(doubleArrayOf(0.0, 0.2, 1.0))))
    }

    @Test fun `every wall is still a true rectangle with the right proportions`() {
        val rc = truth.corners()
        val walls = truth.wallsMm(1.0 + truth.ceiling)   // 1 mm per unit
        for ((i, face) in RoomQuad.WALLS.withIndex()) {
            val m = WallCorners.measure(rc.forFace(face))
            assertTrue(m.valid && m.outOfSquareDeg < 1e-6, face)
            assertEquals(walls[i] / (1 + truth.ceiling), m.ratio, 1e-9, face)
        }
    }

    @Test fun `the ceiling read off a wall's ceiling line`() {
        for (face in RoomQuad.WALLS) {
            val (a, b) = truth.wallEnds(face)
            val mid = WallCorners.unit(doubleArrayOf((a[0] + b[0]) / 2, truth.ceiling, (a[1] + b[1]) / 2))
            assertEquals(truth.ceiling, truth.copy(ceiling = 1.6).ceilingFrom(face, mid).ceiling, 1e-9, face)
        }
    }

    @Test fun `typed sizes give the first room and height gives mm`() {
        val q = RoomQuad.start(4364.0, 3015.0, 2680.0)
        val w = q.wallsMm(2680.0)
        assertEquals(4364.0, w[0], 1e-6); assertEquals(3015.0, w[1], 1e-6)
        assertEquals(4364.0, w[2], 1e-6); assertEquals(3015.0, w[3], 1e-6)
        for (a in q.cornerAngles()) assertEquals(90.0, a, 1e-9)
    }

    @Test fun `each wall's view holds the whole wall`() {
        for (face in RoomQuad.WALLS) {
            val (yaw, fov) = truth.view(face)
            val bs = WallCorners.basis(yaw, 0.0, fov)
            val (fl, cl) = truth.wallEdges(face)
            for (p in listOf(fl.first, fl.second, cl.first, cl.second)) {
                val s = WallCorners.pointOf(bs, WallCorners.unit(p))!!
                assertTrue(s.first in 0.0..1.0 && s.second in 0.0..1.0, "$face corner off screen: $s")
            }
        }
    }

    @Test fun `a corner's view looks straight at it`() {
        val (yaw, _) = truth.cornerView(RoomQuad.Handle.FL_FLOOR)
        val bs = WallCorners.basis(yaw, 0.0, RoomQuad.CORNER_FOV)
        val s = WallCorners.pointOf(bs, floorRay(truth.flX, truth.flZ))!!
        assertEquals(0.5, s.first, 1e-9)
    }
}
