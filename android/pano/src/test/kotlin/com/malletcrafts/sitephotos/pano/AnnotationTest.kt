package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals

class AnnotationTest {

    @Test
    fun `every entry unit converts to canonical mm`() {
        assertEquals(1220, Annotation.toMm(1220.0, Annotation.Unit.MM))
        assertEquals(1220, Annotation.toMm(122.0, Annotation.Unit.CM))
        assertEquals(1220, Annotation.toMm(1.22, Annotation.Unit.M))
        assertEquals(305, Annotation.toMm(12.0, Annotation.Unit.IN))
        assertEquals(1219, Annotation.toMm(4.0, Annotation.Unit.FT))
    }

    @Test
    fun `metric label switches to metres past one metre`() {
        assertEquals("860 mm", Annotation.metricLabel(860))
        assertEquals("1.22 m", Annotation.metricLabel(1220))
        assertEquals("3.05 m", Annotation.metricLabel(3050))
    }

    @Test
    fun `imperial label rounds to the eighth a tape measure uses`() {
        assertEquals("4' 0\"", Annotation.imperialLabel(1219))
        assertEquals("2' 6\"", Annotation.imperialLabel(762))
        // 1250mm = 49.21" -> 49 1/4" -> 4' 1 1/4"
        assertEquals("4' 1 1/4\"", Annotation.imperialLabel(1250))
        // eighths reduce: 3/8 stays, 2/8 becomes 1/4, 4/8 becomes 1/2
        assertEquals("0' 0 1/2\"", Annotation.imperialLabel(13))
    }

    @Test
    fun `dual label appears only when imperial is enabled`() {
        assertEquals("1.22 m", Annotation.label(1220, showImperial = false))
        assertEquals("1.22 m · 4' 0\"", Annotation.label(1220, showImperial = true))
    }

    @Test
    fun `dragging grabs the nearer endpoint inside the radius, none outside`() {
        val l = Annotation.Line(0.10, 0.10, 0.90, 0.90, 1000)
        assertEquals(1, Annotation.nearestEndpoint(l, 0.11, 0.12, 0.06))
        assertEquals(2, Annotation.nearestEndpoint(l, 0.88, 0.91, 0.06))
        assertEquals(-1, Annotation.nearestEndpoint(l, 0.50, 0.50, 0.06))
    }
}
