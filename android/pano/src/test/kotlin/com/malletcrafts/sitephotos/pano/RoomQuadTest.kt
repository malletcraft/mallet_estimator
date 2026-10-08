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

    /** Bars laid on the TRUE room's edges, at fractions that are not the
     *  model's own -- a person puts handles wherever the edge shows. */
    private fun truthBars(l: RoomQuad.Line) = truth.startBars(l, 0.27, 0.81)

    private fun set(q0: RoomQuad, o: RoomQuad.Origin): RoomQuad {
        val p = truth.pointOf(o)
        return q0.fitted(o, floorRay(p[0], p[1]), truth.lines(o).associateWith { truthBars(it) })
    }

    @Test fun `origin and two bar pairs at each of two corners find a room that is not square`() {
        val p = truth.floorPlan()!!
        val q = set(set(RoomQuad.start(null, null, null), RoomQuad.Origin.FL), RoomQuad.Origin.BR)
        val got = q.floorPlan()!!
        for (i in 0 until 4) for (k in 0..1) assertEquals(p[i][k], got[i][k], 1e-9, "corner $i")
        assertEquals(truth.ceiling, q.ceiling, 1e-9)
        for ((a, b) in q.cornerAngles().zip(truth.cornerAngles())) assertEquals(b, a, 1e-9)
    }

    @Test fun `a pair's vanishing direction is the wall's, wherever its handles sit`() {
        for (l in RoomQuad.Line.entries) {
            val want = Math.toRadians(truth.angleOf(l))
            for ((n, f) in listOf(0.05 to 0.3, 0.4 to 0.95, 0.2 to 0.6)) {
                val d = RoomQuad.direction(truth.startBars(l, n, f))!!
                // parallel to the wall line, either way along it
                assertEquals(0.0, d[0] * kotlin.math.sin(want) - d[2] * kotlin.math.cos(want), 1e-9, "$l")
                assertEquals(0.0, d[1], 1e-9, "$l")
            }
        }
    }

    @Test fun `setting one corner leaves the other corner's walls alone`() {
        val q = set(RoomQuad.start(null, null, null), RoomQuad.Origin.FL)
        val s = RoomQuad.start(null, null, null)
        assertEquals(s.brX, q.brX); assertEquals(s.brZ, q.brZ)
        assertEquals(s.backDeg, q.backDeg); assertEquals(s.rightDeg, q.rightDeg)
        assertEquals(truth.frontDeg, q.frontDeg, 1e-9); assertEquals(truth.leftDeg, q.leftDeg, 1e-9)
    }

    @Test fun `an origin above the horizon or collapsed bars are refused`() {
        val bars = truth.lines(RoomQuad.Origin.FL).associateWith { truthBars(it) }
        assertEquals(truth, truth.fitted(RoomQuad.Origin.FL, WallCorners.unit(doubleArrayOf(0.0, 0.2, 1.0)), bars))
        val a = floorRay(truth.flX, truth.flZ)
        val flat = RoomQuad.Bars(a, a, a, a)
        assertEquals(truth, truth.fitted(RoomQuad.Origin.FL, a,
            mapOf(RoomQuad.Line.FRONT to flat, RoomQuad.Line.LEFT to truthBars(RoomQuad.Line.LEFT))))
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
        val (yaw, _) = truth.cornerView(RoomQuad.Origin.FL)
        val bs = WallCorners.basis(yaw, 0.0, RoomQuad.CORNER_FOV)
        val s = WallCorners.pointOf(bs, floorRay(truth.flX, truth.flZ))!!
        assertEquals(0.5, s.first, 1e-9)
    }
}
