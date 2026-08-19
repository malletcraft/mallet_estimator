package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

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
}
