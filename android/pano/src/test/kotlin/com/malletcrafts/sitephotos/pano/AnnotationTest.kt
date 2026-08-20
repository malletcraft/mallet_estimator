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

    @Test
    fun `a dropped opening starts as a rectangle around the given centre`() {
        val q = Annotation.newQuad(Annotation.Kind.WINDOW, 0.5, 0.4, 0.1, 0.2)
        assertEquals("window", q.kind)
        assertEquals(0.4, q.x1, 1e-9)
        assertEquals(0.2, q.y1, 1e-9)
        assertEquals(0.6, q.x2, 1e-9)
        assertEquals(0.6, q.y3, 1e-9)
        assertEquals(0.4, q.x4, 1e-9)
    }

    @Test
    fun `each corner moves on its own, so perspective survives`() {
        // A window seen off-centre is a quadrilateral, never a box: dragging
        // one corner must not pull its neighbours back into a rectangle.
        val q = Annotation.newQuad(Annotation.Kind.WINDOW).withCorner(1, 0.31, 0.22)
        assertEquals(0.31, q.x1, 1e-9)
        assertEquals(0.22, q.y1, 1e-9)
        assertEquals(0.65, q.x2, 1e-9)
        assertEquals(0.30, q.y2, 1e-9)
        assertEquals(0.70, q.y3, 1e-9)
    }

    @Test
    fun `an unknown tag reads as a plain opening rather than throwing`() {
        // Tags arrive from other devices and older builds; the vocabulary is
        // closed, so anything unrecognised degrades instead of crashing.
        assertEquals(Annotation.Kind.OPENING, Annotation.Kind.of("skylight"))
        assertEquals(Annotation.Kind.OPENING, Annotation.Kind.of(null))
        assertEquals(Annotation.Kind.BEAM, Annotation.Kind.of("beam"))
    }

    @Test
    fun `a face holding only openings is not empty`() {
        val only = Annotation.FaceAnnotations(
            quads = listOf(Annotation.newQuad(Annotation.Kind.DOOR)))
        assertEquals(false, only.isEmpty)
        assertEquals(true, Annotation.FaceAnnotations().isEmpty)
    }
}
