package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The as-is picture each surface is uploaded as (MCFT Interior Estimate,
 * contract 2026-10-11): exactly the surface rectangle, no margin, pixel (0,0)
 * = mm (0, b), (width, height) = mm (a, 0), same mm per pixel both ways, long
 * side 2000. The web draws the marks on it in mm, so a margin or a stretch
 * here would put every mark in the wrong place on the web and nowhere else.
 */
class AsIsSquareTest {

    // The guest bedroom of mock-up v33, camera off-centre and 1340 mm up.
    private val L = 3319.0; private val W = 2678.0; private val H = 2677.0
    private val xMin = -1100.0; private val xMax = xMin + W
    private val zMin = -1450.0; private val zMax = zMin + L
    private val yF = -1340.0; private val yC = yF + H

    private fun corners3d(s: String): List<DoubleArray> {
        fun p(x: Double, y: Double, z: Double) = doubleArrayOf(x, y, z)
        return when (s) {
            "front" -> listOf(p(xMin, yF, zMax), p(xMax, yF, zMax), p(xMax, yC, zMax), p(xMin, yC, zMax))
            "right" -> listOf(p(xMax, yF, zMax), p(xMax, yF, zMin), p(xMax, yC, zMin), p(xMax, yC, zMax))
            "back" -> listOf(p(xMax, yF, zMin), p(xMin, yF, zMin), p(xMin, yC, zMin), p(xMax, yC, zMin))
            "left" -> listOf(p(xMin, yF, zMin), p(xMin, yF, zMax), p(xMin, yC, zMax), p(xMin, yC, zMin))
            "floor" -> listOf(p(xMin, yF, zMin), p(xMax, yF, zMin), p(xMax, yF, zMax), p(xMin, yF, zMax))
            else -> listOf(p(xMin, yC, zMin), p(xMax, yC, zMin), p(xMax, yC, zMax), p(xMin, yC, zMax))
        }
    }

    private fun frameOf(s: String): SurveyPrep.Frame {
        val q = corners3d(s).map { assertNotNull(SurveyPrep.proj(s, it[0], it[1], it[2])) }
        val lines = mapOf("bottom" to arrayOf(q[0], q[1]), "top" to arrayOf(q[3], q[2]),
            "left" to arrayOf(q[0], q[3]), "right" to arrayOf(q[1], q[2]))
        val ab = SurveyPrep.size(s, L, W, H)
        return assertNotNull(SurveyPrep.frame(lines, ab[0], ab[1]), "no frame for $s")
    }

    private fun unit(v: DoubleArray): DoubleArray {
        val n = hypot(hypot(v[0], v[1]), v[2]); return DoubleArray(3) { v[it] / n }
    }

    private fun sameDirection(exp: DoubleArray, got: DoubleArray, what: String) {
        val e = unit(exp); val g = unit(got)
        for (i in 0 until 3) assertTrue(abs(e[i] - g[i]) < 1e-6, "$what: expected ${e.toList()}, got ${g.toList()}")
    }

    @Test fun `the long side is 2000 px and the aspect is a over b`() {
        for (s in SurveyPrep.SURFACES) {
            val ab = SurveyPrep.size(s, L, W, H)
            val sq = SurveyPrep.asisSquare(ab[0], ab[1])
            assertEquals(2000, max(sq.width, sq.height), s)
            // same mm per pixel both ways: the short side is the rounding of a/b, never stretched
            assertTrue(abs(sq.width.toDouble() / sq.height - ab[0] / ab[1]) * sq.height <= 1.0, "$s aspect")
            assertEquals(0.0, sq.mX); assertEquals(0.0, sq.mY)
        }
    }

    @Test fun `pixel 0,0 is mm 0,b and the far corner is mm a,0`() {
        for (s in SurveyPrep.SURFACES) {
            val ab = SurveyPrep.size(s, L, W, H)
            val sq = SurveyPrep.asisSquare(ab[0], ab[1])
            assertEquals(0.0, sq.mmX(0.0), 1e-9, "$s left")
            assertEquals(ab[1], sq.mmY(0.0), 1e-9, "$s top")
            // the far corner is within half a pixel of (a, 0): the long side exactly
            assertEquals(ab[0], sq.mmX(sq.width.toDouble()), sq.mpp / 2 + 1e-9, "$s right")
            assertEquals(0.0, sq.mmY(sq.height.toDouble()), sq.mpp / 2 + 1e-9, "$s bottom")
        }
    }

    @Test fun `the corners of the picture look at the corners of the surface`() {
        for (s in SurveyPrep.SURFACES) {
            val f = frameOf(s)
            val sq = SurveyPrep.asisSquare(f.a, f.b)
            val c = corners3d(s)   // bottom-left, bottom-right, top-right, top-left
            // pixel (0,0) is the top-left corner exactly
            sameDirection(c[3], assertNotNull(SurveyPrep.asisDirection(s, f, sq, 0.0, 0.0), "$s 0,0"), "$s top-left")
            // pixel (a/mpp, b/mpp) is the bottom-right corner exactly
            sameDirection(c[1], assertNotNull(SurveyPrep.asisDirection(s, f, sq, f.a / sq.mpp, f.b / sq.mpp), "$s far"),
                "$s bottom-right")
        }
    }

    @Test fun `floor and ceiling have the front wall at the top and the left wall on the left`() {
        for (s in listOf("floor", "ceil")) {
            val f = frameOf(s)
            val sq = SurveyPrep.asisSquare(f.a, f.b)
            val tl = assertNotNull(SurveyPrep.asisDirection(s, f, sq, 0.0, 0.0))
            assertTrue(tl[0] < 0, "$s: top-left of the picture is on the LEFT wall's side")
            assertTrue(tl[2] > 0, "$s: top of the picture is the FRONT wall")
        }
    }

    @Test fun `render as-is is exactly that square`() {
        val pano = Panorama.Image(64, 32, IntArray(64 * 32) { 0x808080 })
        val f = frameOf("front")
        val img = SurveyPrep.renderAsIs(pano, "front", f)
        val sq = SurveyPrep.asisSquare(f.a, f.b)
        assertEquals(sq.width, img.width); assertEquals(sq.height, img.height)
    }
}
