package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CaptureGeometryTest {

    private fun ft(x: Double) = x * 12.0

    private fun plan(lFt: Double, wFt: Double, hFt: Double = 9.5) =
        CaptureGeometry.planForRoom(ft(lFt), ft(wFt), ft(hFt))!!

    // ---- the bug this replaced ------------------------------------------

    @Test
    fun `the floor face is wider than the walls in any ordinary room`() {
        // THE WHOLE DEFECT IN ONE ASSERTION. The old maths sized every face
        // from the walls, so the floor inherited a number computed without
        // it. Floor corners sit at the half-diagonal; wall corners at the
        // perpendicular distance. The floor therefore needs MORE, and a
        // single FOV for all six faces cannot be right for both.
        for ((l, w) in listOf(10.0 to 12.0, 12.0 to 14.0, 20.0 to 18.0)) {
            val p = plan(l, w)
            val widestWall = listOf("front", "back", "left", "right")
                .maxOf { p.fovByFace[it]!! }
            assertTrue(p.floorFovDeg > widestWall,
                "in a ${l}x${w} room the floor needs ${p.floorFovDeg} and the " +
                "widest wall only ${widestWall} — one FOV cannot serve both")
        }
    }

    @Test
    fun `the old single-FOV answer would have truncated a living room floor`() {
        // 20x18: the walls want about 106 and the floor about 144. Sizing
        // from the walls alone loses the floor corners by nearly 40 degrees,
        // which is what "floor is not getting captured all 4 corners" is.
        val p = plan(20.0, 18.0)
        val widestWall = listOf("front", "back", "left", "right")
            .maxOf { p.fovByFace[it]!! }
        assertTrue(p.floorFovDeg - widestWall > 30.0,
            "expected a large gap, got ${p.floorFovDeg} vs $widestWall")
    }

    @Test
    fun `a small room is the accidental pass that hid the bug`() {
        // A 5x7 bathroom's walls need MORE than its floor, so sizing from the
        // walls covered the floor by luck. That is why the failure was
        // intermittent, and why it read as a miscalibration rather than a
        // missing term.
        val p = plan(5.0, 7.0)
        val widestWall = listOf("front", "back", "left", "right")
            .maxOf { p.fovByFace[it]!! }
        assertTrue(widestWall > p.floorFovDeg)
    }

    // ---- the contract with the bench -------------------------------------

    @Test
    fun `a 10x12 room matches the bench's numbers exactly`() {
        // These are capture_geometry.py's output for the same room. The two
        // copies exist because a site has no signal; they are only worth
        // having if they agree, so the numbers are pinned rather than
        // recomputed by the same formula twice.
        val p = plan(10.0, 12.0)
        assertEquals(60, p.station.x)
        assertEquals(72, p.station.y)
        assertEquals(57, p.station.z)
        val expected = mapOf(
            "front" to 90.0, "back" to 90.0,
            "left" to 110.4, "right" to 110.4,
            "down" to 122.1, "up" to 122.1,
        )
        for ((face, want) in expected) {
            val got = p.fovByFace[face]!!
            assertTrue(abs(got - want) < 0.05,
                "$face: bench says $want, phone says $got")
        }
    }

    // ---- Amit's spec ------------------------------------------------------

    @Test
    fun `every face carries ten percent of the adjacent walls`() {
        // "just showing 10% of the adjacent walls to distinguish the face
        // being measured". Without the border the face is cropped exactly at
        // the corner and nothing in the picture says which wall it is.
        val l = ft(10.0); val w = ft(12.0); val h = ft(9.5)
        val withMargin = CaptureGeometry.fovByFace(l, w, h)
        // The front wall is 120 in wide seen from 72 in away. Bare geometry
        // wants 2*atan(60/72); the border makes it 2*atan((60+12)/72).
        val bare = 2 * Math.toDegrees(kotlin.math.atan(60.0 / 72.0))
        assertTrue(withMargin["front"]!! > bare,
            "the border must widen the face, not decorate the comment")
        assertEquals(90.0, withMargin["front"]!!, 0.05)
    }

    @Test
    fun `the station is the room centre at half the ceiling`() {
        // Amit: "that should tell where camera should be in the room by all
        // its axes x,y,z". Half height is not a convention -- raising the
        // camera shrinks the floor's need and grows the ceiling's by the same
        // geometry, so the middle minimises the worse of the two.
        val s = CaptureGeometry.stationFor(ft(10.0), ft(12.0), ft(9.5))
        assertEquals(60.0, s.xIn, 1e-9)
        assertEquals(72.0, s.yIn, 1e-9)
        assertEquals(57.0, s.zIn, 1e-9)
    }

    @Test
    fun `half height minimises the worse of floor and ceiling`() {
        val l = ft(12.0); val w = ft(14.0); val h = ft(9.5)
        val mid = CaptureGeometry.fovByFace(l, w, h)
        val worstMid = maxOf(mid["down"]!!, mid["up"]!!)
        for (z in listOf(0.25, 0.35, 0.65, 0.75)) {
            val off = CaptureGeometry.fovByFace(
                l, w, h, CaptureGeometry.Station(l / 2, w / 2, h * z))
            assertTrue(maxOf(off["down"]!!, off["up"]!!) >= worstMid - 1e-9,
                "standing at ${z} of the height beat the middle")
        }
    }

    @Test
    fun `floor and ceiling are equal from half height`() {
        val p = plan(12.0, 14.0)
        assertEquals(p.floorFovDeg, p.ceilingFovDeg, 1e-9)
    }

    @Test
    fun `the floor requirement holds at any yaw`() {
        // The half-diagonal is the guarantee Amit chose over the cheaper
        // aligned case: the floor square must contain the room corner however
        // the camera happens to be turned.
        val l = ft(10.0); val w = ft(12.0); val h = ft(9.5)
        val f = CaptureGeometry.fovByFace(l, w, h)["down"]!!
        val halfSide = (h / 2) * kotlin.math.tan(Math.toRadians(f / 2))
        val corner = kotlin.math.hypot(l / 2, w / 2)
        assertTrue(halfSide >= corner,
            "the floor face reaches $halfSide in; the corner is at $corner in")
    }

    // ---- refusing what is not a room -------------------------------------

    @Test
    fun `feet typed into an inches field are refused`() {
        // The single likeliest input error, and the one that must not get
        // through: 10 instead of 120 silently halves every FOV and the
        // photographs look plausible.
        assertNull(CaptureGeometry.planForRoom(10.0, 12.0, 9.5))
    }

    @Test
    fun `a cupboard and a mistyped room are both refused`() {
        assertNull(CaptureGeometry.planForRoom(24.0, ft(10.0), ft(9.5)))
        assertNull(CaptureGeometry.planForRoom(ft(200.0), ft(10.0), ft(9.5)))
    }

    @Test
    fun `a missing dimension is null, not a guess`() {
        assertNull(CaptureGeometry.planForRoom(null, ft(10.0), ft(9.5)))
        assertNull(CaptureGeometry.planForRoom(ft(10.0), null, ft(9.5)))
        assertNull(CaptureGeometry.planForRoom(ft(10.0), ft(10.0), null))
    }

    @Test
    fun `an impossible ceiling is refused`() {
        assertNull(CaptureGeometry.planForRoom(ft(10.0), ft(10.0), 60.0))
        assertNull(CaptureGeometry.planForRoom(ft(10.0), ft(10.0), 300.0))
    }

    // ---- never silent -----------------------------------------------------

    @Test
    fun `a face past what the projection can sample is named, not clamped quietly`() {
        // Clamping and saying nothing is what makes a cropped face read as
        // the app getting it wrong rather than the room being too big.
        val huge = CaptureGeometry.planForRoom(ft(60.0), ft(60.0), ft(8.0))
        assertNotNull(huge)
        assertTrue(!huge.fitted)
        assertTrue("down" in huge.unfittedFaces && "up" in huge.unfittedFaces,
            "expected the poles to be the ones that cannot fit, got ${huge.unfittedFaces}")
        // And what is handed to the splitter is still sampleable.
        for ((_, v) in huge.clampedByFace) {
            assertTrue(v <= Panorama.FOV_MAX && v >= Panorama.FOV_MIN)
        }
    }

    @Test
    fun `an ordinary house room always fits`() {
        for ((l, w, h) in listOf(
            Triple(5.0, 7.0, 9.5), Triple(8.0, 11.0, 9.5), Triple(10.0, 12.0, 9.5),
            Triple(12.0, 14.0, 9.5), Triple(16.0, 8.0, 9.5), Triple(20.0, 18.0, 9.5),
            Triple(20.0, 18.0, 11.0), Triple(25.0, 20.0, 10.0),
        )) {
            val p = plan(l, w, h)
            assertTrue(p.fitted, "${l}x${w}x${h} did not fit: ${p.fovByFace}")
        }
    }

    @Test
    fun `naming length and width the other way round cannot change the plan`() {
        val a = plan(12.0, 9.0)
        val b = plan(9.0, 12.0)
        assertEquals(a.floorFovDeg, b.floorFovDeg, 1e-9)
        assertEquals(a.ceilingFovDeg, b.ceilingFovDeg, 1e-9)
        // The walls swap identity with the axes, so the SET must match.
        assertEquals(a.fovByFace.values.sorted(), b.fovByFace.values.sorted())
    }
}
