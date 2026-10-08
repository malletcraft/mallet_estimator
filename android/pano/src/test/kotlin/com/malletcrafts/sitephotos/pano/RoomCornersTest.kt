package com.malletcrafts.sitephotos.pano

import kotlin.math.acos
import kotlin.test.Test
import kotlin.test.assertTrue

class RoomCornersTest {

    private fun angleDeg(a: DoubleArray, b: DoubleArray) =
        Math.toDegrees(acos(WallCorners.dot(WallCorners.unit(a), WallCorners.unit(b)).coerceIn(-1.0, 1.0)))

    @Test fun `neighbouring walls share their corner`() {
        val rc = RoomBox.start(4364.0, 3015.0, 2680.0).corners()
        for ((a, b) in listOf("front" to "right", "right" to "back", "back" to "left", "left" to "front")) {
            val x = rc.forFace(a); val y = rc.forFace(b)
            // a wall's right edge is the next wall's left edge, top and bottom
            assertTrue(angleDeg(x[1], y[0]) < 1e-9, "$a/$b top")
            assertTrue(angleDeg(x[2], y[3]) < 1e-9, "$a/$b bottom")
        }
    }

    @Test fun `ceiling and floor take the four top and four bottom corners`() {
        val rc = RoomBox.start(null, null, null).corners()
        for (d in rc.forFace("up")) assertTrue(d[1] > 0)
        for (d in rc.forFace("down")) assertTrue(d[1] < 0)
    }
}
