package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class CaptureGeometryTest {

    @Test
    fun `smaller rooms need wider faces`() {
        val (small, medium, large) = CaptureGeometry.PRESETS.map { it.fov }
        assertTrue(small > medium, "small $small should beat medium $medium")
        assertTrue(medium > large, "medium $medium should beat large $large")
    }

    @Test
    fun `the historical fixed 110 truly truncates a small room`() {
        // The bug this module exists to fix: 5x7 bathroom, camera mid-height.
        val needed = CaptureGeometry.requiredFovDeg(5.0, 7.0)
        assertTrue(needed > 110.0, "geometry says $needed — 110 cannot show the corners")
    }

    @Test
    fun `preset recommendations land where the geometry says`() {
        val (small, medium, large) = CaptureGeometry.PRESETS.map { it.fov }
        assertTrue(small in 125.0..135.0, "small was $small")
        assertTrue(medium in 108.0..120.0, "medium was $medium")
        assertTrue(large in 95.0..108.0, "large was $large")
    }

    @Test
    fun `mid-height camera minimises the required fov`() {
        val mid = CaptureGeometry.requiredFovDeg(5.0, 7.0, 9.5, cameraFt = 4.75)
        val low = CaptureGeometry.requiredFovDeg(5.0, 7.0, 9.5, cameraFt = 2.5)
        val high = CaptureGeometry.requiredFovDeg(5.0, 7.0, 9.5, cameraFt = 7.5)
        assertTrue(mid <= low, "mid $mid vs low $low")
        assertTrue(mid <= high, "mid $mid vs high $high")
    }

    @Test
    fun `a lower camera needs more degrees, which is the truncation the site saw`() {
        // Camera at 3 ft (tripod too short) vs mid-height in the same room.
        val lowCam = CaptureGeometry.requiredFovDeg(8.0, 11.0, 9.5, cameraFt = 3.0)
        val midCam = CaptureGeometry.requiredFovDeg(8.0, 11.0, 9.5)
        assertTrue(lowCam > midCam + 5, "low $lowCam should clearly exceed mid $midCam")
    }

    @Test
    fun `recommendation clamps to the readable range`() {
        // A closet forces the ceiling of the range, a hall the floor.
        assertEquals(CaptureGeometry.RECOMMEND_MAX,
            CaptureGeometry.recommendedFovDeg(3.0, 3.0))
        assertEquals(CaptureGeometry.RECOMMEND_MIN,
            CaptureGeometry.recommendedFovDeg(60.0, 60.0))
    }

    @Test
    fun `recommended camera height is half the ceiling`() {
        assertEquals(4.75, CaptureGeometry.recommendedCameraHeightFt(9.5))
    }

    @Test
    fun `recommended fov stays inside the projection clamp`() {
        for (p in CaptureGeometry.PRESETS) {
            assertEquals(p.fov, Panorama.clampFov(p.fov), "preset ${p.label}")
        }
    }

    @Test
    fun `impossible rooms are refused, not guessed`() {
        assertFailsWith<IllegalArgumentException> {
            CaptureGeometry.requiredFovDeg(0.0, 7.0)
        }
        assertFailsWith<IllegalArgumentException> {
            CaptureGeometry.requiredFovDeg(5.0, 7.0, 9.5, cameraFt = 10.0)
        }
    }

    // ---- Typed room dimensions (Amit, 2026-09-04). The presets rounded a
    // measured room into somebody else's bucket; these hold the maths that
    // replaced them.

    @Test
    fun `length and width are interchangeable`() {
        val a = CaptureGeometry.adviseForRoom(12.0, 9.0)!!
        val b = CaptureGeometry.adviseForRoom(9.0, 12.0)!!
        assertEquals(a.fovDeg, b.fovDeg, 1e-9,
            "which dimension he calls length must not change the answer")
    }

    @Test
    fun `a smaller room needs a wider face`() {
        val small = CaptureGeometry.adviseForRoom(6.0, 8.0)!!
        val large = CaptureGeometry.adviseForRoom(20.0, 18.0)!!
        assertTrue(small.fovDeg > large.fovDeg,
            "small ${small.fovDeg} should exceed large ${large.fovDeg}")
    }

    @Test
    fun `the advised fov actually covers every corner it claims to`() {
        for (l in listOf(6.0, 9.0, 12.0, 16.0, 20.0)) {
            for (w in listOf(5.0, 8.0, 11.0, 18.0)) {
                val a = CaptureGeometry.adviseForRoom(l, w)!!
                if (!a.fitted) continue           // honestly reported as short
                assertTrue(a.fovDeg >= CaptureGeometry.requiredFovDeg(l, w),
                    "$l x $w advised ${a.fovDeg} but needs " +
                    "${CaptureGeometry.requiredFovDeg(l, w)}")
            }
        }
    }

    @Test
    fun `a room too small to cover says so instead of pretending`() {
        val a = CaptureGeometry.adviseForRoom(3.5, 3.5)!!
        assertFalse(a.fitted,
            "a 3.5 ft room cannot be covered from its centre and must admit it")
        assertTrue(a.requiredDeg > CaptureGeometry.RECOMMEND_MAX)
        assertEquals(CaptureGeometry.RECOMMEND_MAX, a.fovDeg, 1e-9,
            "it still returns the widest usable face rather than nothing")
    }

    @Test
    fun `nonsense input is refused rather than turned into a confident number`() {
        assertNull(CaptureGeometry.adviseForRoom(null, 10.0))
        assertNull(CaptureGeometry.adviseForRoom(10.0, null))
        assertNull(CaptureGeometry.adviseForRoom(0.0, 10.0))
        assertNull(CaptureGeometry.adviseForRoom(-4.0, 10.0))
        assertNull(CaptureGeometry.adviseForRoom(2.0, 10.0), "under 3 ft is a cupboard")
        assertNull(CaptureGeometry.adviseForRoom(200.0, 10.0), "a mistyped 20")
    }

    @Test
    fun `rounding a long room into a preset leaves the far wall truncated`() {
        // The failure the presets actually cause. A 8x16 ft room — a long
        // bedroom, entirely ordinary — is nearest "Medium (8x11)", and that
        // preset is not merely different, it is SHORT: the 16 ft wall is
        // faced from 4 ft away and needs far more degrees than an 11 ft one.
        // Shooting it at the preset crops the far corners, which is exactly
        // what "fov is still small" looks like from the room.
        val needed = CaptureGeometry.requiredFovDeg(8.0, 16.0)
        val preset = CaptureGeometry.PRESETS[1].fov
        assertTrue(preset < needed,
            "preset $preset already covers $needed — then the presets were fine " +
            "and this test is about the wrong room")
        val measured = CaptureGeometry.adviseForRoom(8.0, 16.0)!!
        assertTrue(measured.fovDeg > preset,
            "measured ${measured.fovDeg} must beat the preset $preset it replaces")
        assertTrue(measured.fovDeg >= needed || !measured.fitted,
            "it either covers the room or admits it cannot")
    }

}
