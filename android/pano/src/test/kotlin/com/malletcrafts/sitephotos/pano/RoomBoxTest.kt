package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomBoxTest {

    // A real-shaped room, turned 12 degrees against the photo, camera off centre.
    private val truth = RoomBox(12.0, left = 1.3, right = 1.9, front = 1.1, back = 1.6, ceiling = 0.95)

    private fun mid(seg: Pair<DoubleArray, DoubleArray>, k: Double = 0.5) =
        WallCorners.unit(DoubleArray(3) { seg.first[it] * (1 - k) + seg.second[it] * k })

    @Test fun `the box's corners make six true rectangles of the right proportions`() {
        val rc = truth.corners()
        val h = 1 + truth.ceiling
        val want = mapOf(
            "front" to (truth.left + truth.right) / h, "back" to (truth.left + truth.right) / h,
            "right" to (truth.front + truth.back) / h, "left" to (truth.front + truth.back) / h,
            "up" to (truth.left + truth.right) / (truth.front + truth.back),
            "down" to (truth.left + truth.right) / (truth.front + truth.back))
        for ((face, r) in want) {
            val m = WallCorners.measure(rc.forFace(face))
            assertTrue(m.valid && m.outOfSquareDeg < 1e-6, face)
            assertEquals(r, m.ratio, 1e-9, face)
        }
    }

    @Test fun `each edge drag sets exactly its own number`() {
        val s = truth.copy(front = 0.7, ceiling = 1.4, right = 1.0)
        val t = truth.edges("front")
        assertEquals(truth.front, s.dragged("front", RoomBox.Edge.FLOOR, mid(t.getValue(RoomBox.Edge.FLOOR), 0.3)).front, 1e-9)
        val f = s.copy(front = truth.front)
        assertEquals(truth.ceiling, f.dragged("front", RoomBox.Edge.CEILING, mid(t.getValue(RoomBox.Edge.CEILING))).ceiling, 1e-9)
        assertEquals(truth.right, f.dragged("front", RoomBox.Edge.RIGHT, mid(t.getValue(RoomBox.Edge.RIGHT))).right, 1e-9)
    }

    @Test fun `dragging edges onto the photo's edges converges on the real room`() {
        // Start from a guess with the wrong turn and the wrong sizes, and drag
        // each edge onto where the real room's edge is, round the room, a few
        // times -- what a person does with a thumb.
        var b = RoomBox.start(null, null, null)
        repeat(8) {
            for (face in RoomBox.WALLS) {
                val e = truth.edges(face)
                // floor line by each end in turn: the far end holds, this one follows
                b = b.dragged(face, RoomBox.Edge.FLOOR, mid(e.getValue(RoomBox.Edge.FLOOR), 0.15), at = 0.1)
                b = b.dragged(face, RoomBox.Edge.FLOOR, mid(e.getValue(RoomBox.Edge.FLOOR), 0.85), at = 0.9)
                b = b.dragged(face, RoomBox.Edge.RIGHT, mid(e.getValue(RoomBox.Edge.RIGHT)))
                b = b.dragged(face, RoomBox.Edge.CEILING, mid(e.getValue(RoomBox.Edge.CEILING)))
            }
        }
        assertTrue(abs(b.yawDeg - truth.yawDeg) < 0.05, "yaw ${b.yawDeg}")
        for ((got, want) in listOf(b.left to truth.left, b.right to truth.right, b.front to truth.front,
                                   b.back to truth.back, b.ceiling to truth.ceiling))
            assertEquals(want, got, 2e-3)
    }

    @Test fun `swinging a floor line by one end keeps the other corner fixed`() {
        val b = RoomBox.start(null, null, null)
        val before = b.corners().floor[1]               // FR, the front wall's right end
        val e = truth.edges("front").getValue(RoomBox.Edge.FLOOR)
        val after = b.dragged("front", RoomBox.Edge.FLOOR, mid(e, 0.1), at = 0.05).corners().floor[1]
        for (k in 0..2) assertEquals(before[k], after[k], 1e-9)
    }

    @Test fun `typed height turns the box into millimetres`() {
        val (l, w, cam) = truth.sizeMm(2680.0)
        val u = 2680.0 / (1 + truth.ceiling)
        assertEquals((truth.left + truth.right) * u, l, 1e-9)
        assertEquals((truth.front + truth.back) * u, w, 1e-9)
        assertEquals(u, cam, 1e-9)
    }

    @Test fun `the first box matches typed sizes`() {
        val b = RoomBox.start(4364.0, 3015.0, 2680.0)
        val (l, w, _) = b.sizeMm(2680.0)
        assertEquals(4364.0, l, 1e-6); assertEquals(3015.0, w, 1e-6)
    }

    @Test fun `each wall's view holds the whole wall`() {
        for (face in RoomBox.WALLS) {
            val (yaw, fov) = truth.view(face)
            val bs = WallCorners.basis(yaw, 0.0, fov)
            for (seg in truth.edges(face).values) for (p in listOf(seg.first, seg.second)) {
                val q = WallCorners.pointOf(bs, WallCorners.unit(p))!!
                assertTrue(q.first in 0.0..1.0 && q.second in 0.0..1.0, "$face corner off screen: $q")
            }
        }
    }
}
