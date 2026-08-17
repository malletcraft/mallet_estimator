package com.malletcrafts.sitephotos.pano

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.tan

/**
 * The 360 → cube-face projection, on the device.
 *
 * This is the second implementation of the formula in mallet_estimator's
 * panorama.py. It exists because both apps live on one phone and a site has no
 * signal: a face that has to travel to a server and back is a face ImageMeter
 * never sees while anyone is still standing in the room.
 *
 * Two implementations of one formula drift silently — nobody notices a face
 * two degrees off, they measure the wrong wall months later. So this file is
 * held to the goldens the Python side publishes
 * (mallet_estimator/tests/golden/), and both sides read the SAME source
 * panorama rather than each building its own.
 *
 * Deliberately free of android.graphics: it takes and returns plain int
 * arrays, so it runs — and is tested — on an ordinary JVM. The Android layer
 * converts Bitmap ↔ IntArray at the edges.
 */
object Panorama {

    const val DEFAULT_FOV = 110.0
    const val DEFAULT_FACE_PX = 1600

    const val FOV_MIN = 30.0
    const val FOV_MAX = 160.0
    const val FACE_PX_MIN = 256
    const val FACE_PX_MAX = 4096

    /** name, yaw°, pitch° — the same six, in the same order, as the server. */
    val FACES: List<Triple<String, Double, Double>> = listOf(
        Triple("front", 0.0, 0.0),
        Triple("right", 90.0, 0.0),
        Triple("back", 180.0, 0.0),
        Triple("left", 270.0, 0.0),
        Triple("up", 0.0, 90.0),
        Triple("down", 0.0, -90.0),
    )

    /** An equirectangular source: packed 0xRRGGBB, row-major, width*height. */
    class Image(val width: Int, val height: Int, val pixels: IntArray) {
        init {
            require(pixels.size == width * height) {
                "expected ${width * height} pixels, got ${pixels.size}"
            }
        }
    }

    /**
     * True when the image is plausibly a 2:1 equirectangular panorama.
     *
     * Splitting a normal photo produces convincing-looking garbage, which is
     * worse than refusing: nobody doubts a picture that looks like a room.
     */
    fun looksEquirect(width: Int, height: Int, tolerance: Double = 0.10): Boolean {
        if (width <= 0 || height <= 0) return false
        val ratio = width.toDouble() / height.toDouble()
        return kotlin.math.abs(ratio - 2.0) <= tolerance * 2.0
    }

    fun clampFov(fov: Double?): Double =
        (fov ?: DEFAULT_FOV).coerceIn(FOV_MIN, FOV_MAX)

    fun clampFacePx(px: Int?): Int =
        (px ?: DEFAULT_FACE_PX).coerceIn(FACE_PX_MIN, FACE_PX_MAX)

    /**
     * Sample one gnomonic face. Bilinear; longitude WRAPS (the back face
     * straddles the ±180° seam) and latitude CLAMPS at the poles.
     */
    fun faceFromEquirect(
        pano: Image,
        yawDeg: Double,
        pitchDeg: Double,
        fovDeg: Double,
        facePx: Int,
    ): Image {
        val w = pano.width
        val h = pano.height
        val lam0 = Math.toRadians(yawDeg)
        val phi0 = Math.toRadians(pitchDeg)

        // Camera basis. `up` is the analytic ∂forward/∂pitch, which stays
        // valid at the poles where crossing with world-up degenerates — and
        // the poles are exactly where the ceiling and floor faces point.
        val fx = cos(phi0) * sin(lam0)
        val fy = sin(phi0)
        val fz = cos(phi0) * cos(lam0)
        val rx = cos(lam0)
        val ry = 0.0
        val rz = -sin(lam0)
        val ux = -sin(phi0) * sin(lam0)
        val uy = cos(phi0)
        val uz = -sin(phi0) * cos(lam0)

        val t = tan(Math.toRadians(fovDeg) / 2.0)
        // linspace(-t, t, facePx): endpoints INCLUSIVE, which is why the step
        // divides by facePx - 1. Getting this wrong shifts every face by half
        // a pixel — invisible by eye, and a permanent disagreement with the
        // server's copy.
        val step = if (facePx > 1) (2.0 * t) / (facePx - 1) else 0.0

        val out = IntArray(facePx * facePx)
        for (row in 0 until facePx) {
            val b = t - row * step                 // top → bottom
            for (col in 0 until facePx) {
                val a = -t + col * step            // left → right

                val dx = fx + a * rx + b * ux
                val dy = fy + a * ry + b * uy
                val dz = fz + a * rz + b * uz

                val lam = atan2(dx, dz)
                val phi = atan2(dy, hypot(dx, dz))

                // Angle → source pixel, sampling at pixel centres.
                val x = (lam / (2.0 * Math.PI) + 0.5) * w - 0.5
                val y = (0.5 - phi / Math.PI) * h - 0.5

                val x0 = floor(x).toInt()
                val y0 = floor(y).toInt()
                val wx = (x - x0).toFloat()
                val wy = (y - y0).toFloat()

                val x0m = Math.floorMod(x0, w)
                val x1m = Math.floorMod(x0 + 1, w)
                val y0c = y0.coerceIn(0, h - 1)
                val y1c = (y0 + 1).coerceIn(0, h - 1)

                val p00 = pano.pixels[y0c * w + x0m]
                val p10 = pano.pixels[y0c * w + x1m]
                val p01 = pano.pixels[y1c * w + x0m]
                val p11 = pano.pixels[y1c * w + x1m]

                val c00 = (1 - wx) * (1 - wy)
                val c10 = wx * (1 - wy)
                val c01 = (1 - wx) * wy
                val c11 = wx * wy

                var packed = 0
                for (shift in intArrayOf(16, 8, 0)) {
                    val v = ((p00 ushr shift and 0xFF) * c00 +
                             (p10 ushr shift and 0xFF) * c10 +
                             (p01 ushr shift and 0xFF) * c01 +
                             (p11 ushr shift and 0xFF) * c11)
                    // +0.5 then truncate, matching the server's rounding.
                    val bch = (v + 0.5f).toInt().coerceIn(0, 255)
                    packed = packed or (bch shl shift)
                }
                out[row * facePx + col] = packed
            }
        }
        return Image(facePx, facePx, out)
    }

    /** All six faces, keyed by name. */
    fun splitEquirect(
        pano: Image,
        fov: Double? = null,
        facePx: Int? = null,
    ): Map<String, Image> {
        require(looksEquirect(pano.width, pano.height)) {
            "not a 2:1 equirectangular panorama: ${pano.width}x${pano.height}"
        }
        val f = clampFov(fov)
        val px = clampFacePx(facePx)
        return FACES.associate { (name, yaw, pitch) ->
            name to faceFromEquirect(pano, yaw, pitch, f, px)
        }
    }
}
