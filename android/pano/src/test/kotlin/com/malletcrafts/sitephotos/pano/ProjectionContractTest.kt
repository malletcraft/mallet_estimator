package com.malletcrafts.sitephotos.pano

import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Holds the device's projection to the contract the server publishes.
 *
 * Reads the SAME panorama PNG and the SAME sampled pixels that
 * mallet_estimator/tests/test_panorama.py checks its own side against. If
 * these two ever disagree, a wall measured on a phone stops meaning what a
 * wall measured on the bench means — and nothing about the picture would look
 * wrong enough for anyone to notice.
 */
class ProjectionContractTest {

    private val goldenDir = File("../../mallet_estimator/tests/golden")

    private fun goldens(): Map<String, Any> {
        val text = File(goldenDir, "projection_goldens.json").readText()
        return MiniJson.parse(text) as Map<String, Any>
    }

    private fun pano(file: String): Panorama.Image {
        val img = ImageIO.read(File(goldenDir, file))
        val w = img.width
        val h = img.height
        val px = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            px[y * w + x] = img.getRGB(x, y) and 0xFFFFFF
        }
        return Panorama.Image(w, h, px)
    }

    @Test
    fun `every face matches the servers projection`() {
        val g = goldens()
        @Suppress("UNCHECKED_CAST")
        val panoSpec = g["pano"] as Map<String, Any>
        val src = pano(panoSpec["file"] as String)
        assertEquals((panoSpec["width"] as Number).toInt(), src.width)
        assertEquals((panoSpec["height"] as Number).toInt(), src.height)

        val fov = (g["fov"] as Number).toDouble()
        val facePx = (g["face_px"] as Number).toInt()
        val tol = (g["tolerance"] as Number).toInt()

        @Suppress("UNCHECKED_CAST")
        val faces = g["faces"] as Map<String, Map<String, Any>>
        assertEquals(6, faces.size, "all six faces must be under contract")

        for ((name, spec) in faces) {
            val face = Panorama.faceFromEquirect(
                src,
                (spec["yaw"] as Number).toDouble(),
                (spec["pitch"] as Number).toDouble(),
                fov, facePx,
            )
            @Suppress("UNCHECKED_CAST")
            val samples = spec["samples"] as List<Map<String, Any>>
            assertTrue(samples.isNotEmpty(), "$name has no samples")
            for (s in samples) {
                val x = (s["x"] as Number).toInt()
                val y = (s["y"] as Number).toInt()
                @Suppress("UNCHECKED_CAST")
                val want = (s["rgb"] as List<Number>).map { it.toInt() }
                val packed = face.pixels[y * facePx + x]
                val got = listOf(
                    packed ushr 16 and 0xFF,
                    packed ushr 8 and 0xFF,
                    packed and 0xFF,
                )
                for (c in 0..2) {
                    val delta = kotlin.math.abs(got[c] - want[c])
                    assertTrue(
                        delta <= tol,
                        "$name ($x,$y) channel $c: got $got, server says $want " +
                            "(delta $delta > tolerance $tol)",
                    )
                }
            }
        }
    }

    @Test
    fun `a normal photo is refused rather than silently mangled`() {
        assertTrue(Panorama.looksEquirect(5376, 2688))   // Theta Z1
        assertTrue(!Panorama.looksEquirect(640, 480))
        val flat = Panorama.Image(64, 48, IntArray(64 * 48))
        var threw = false
        try {
            Panorama.splitEquirect(flat)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw, "splitting a non-equirect must throw, not guess")
    }

    @Test
    fun `parameters are clamped to the same bounds as the server`() {
        assertEquals(Panorama.FOV_MAX, Panorama.clampFov(500.0))
        assertEquals(Panorama.FOV_MIN, Panorama.clampFov(1.0))
        assertEquals(Panorama.DEFAULT_FOV, Panorama.clampFov(null))
        assertEquals(Panorama.FACE_PX_MAX, Panorama.clampFacePx(999999))
        assertEquals(Panorama.FACE_PX_MIN, Panorama.clampFacePx(1))
        assertEquals(Panorama.DEFAULT_FACE_PX, Panorama.clampFacePx(null))
    }
}
