package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * A wall's four corners, tapped on a face, turned into a true elevation.
 *
 * Amit, 2026-10-07: "lets drop idea of laser guides as its one more work at
 * site. use only foto and corners placed approximately ... can i place 4
 * corners on flat foto so that only that will be used to mesure?"
 *
 * Yes, and it is the whole fix for the two faults he reported -- side walls
 * spilling into a face, and faces drawn on that are skewed because the only
 * correction was a horizontal turn. Four points on the corners of a flat
 * rectangular wall define that wall's plane, whatever the tripod's tilt or
 * position, so re-sampling the photo until those four points sit on the
 * corners of a rectangle gives an image with ONE scale across the whole wall:
 * an elevation. It stops exactly at the corners, so no side wall survives.
 *
 * THE PROPORTION COMES FROM THE PHOTO, NOT THE TAPE. A face is a picture
 * taken through a known lens (the gnomonic projection below), so the four
 * corner RAYS are known directions, and four rays through the corners of a
 * rectangle fix that rectangle's shape up to scale. That is why the
 * elevation's aspect is measured rather than assumed, and why the tape can be
 * checked against it: given the height, the photo states the length.
 *
 * WHY CORNERS ARE STORED AS DIRECTIONS and not as positions on a face: the
 * same dialog still has the FOV and turn sliders, and a point at 0.2 across a
 * 110 degree face is a different wall position at 120. A direction in the
 * panorama does not move when the framing does.
 *
 * Frame: the one Panorama uses -- x right, y up, z forward (yaw 0), so
 * longitude is atan2(x, z). Pure JVM, so the maths is tested without a device.
 */
object WallCorners {

    /** The order the four handles are named and stored in. */
    val ORDER = listOf("TL", "TR", "BR", "BL")

    /** Out of square by more than this and the corners do not describe a
     *  rectangle -- a corner placed carelessly, or a wall that is not one. */
    const val SQUARE_TOLERANCE_DEG = 3.0

    /** The wall faces. Ceiling and floor have no 4-corner step yet. */
    val WALL_FACES = listOf("front", "right", "back", "left")

    data class Basis(val f: DoubleArray, val r: DoubleArray, val u: DoubleArray, val t: Double)

    /** The camera basis faceFromEquirect uses for a face, so a point on a
     *  preview tile and a ray through the pano are the same thing. */
    fun basis(yawDeg: Double, pitchDeg: Double, fovDeg: Double): Basis {
        val lam = Math.toRadians(yawDeg)
        val phi = Math.toRadians(pitchDeg)
        return Basis(
            f = doubleArrayOf(cos(phi) * sin(lam), sin(phi), cos(phi) * cos(lam)),
            r = doubleArrayOf(cos(lam), 0.0, -sin(lam)),
            u = doubleArrayOf(-sin(phi) * sin(lam), cos(phi), -sin(phi) * cos(lam)),
            t = tan(Math.toRadians(Panorama.clampFov(fovDeg)) / 2.0),
        )
    }

    /** A point on a face, 0..1 across and 0..1 DOWN, to a unit ray. */
    fun rayAt(b: Basis, x: Double, y: Double): DoubleArray {
        val a = -b.t + 2.0 * b.t * x
        val v = b.t - 2.0 * b.t * y
        return unit(DoubleArray(3) { b.f[it] + a * b.r[it] + v * b.u[it] })
    }

    /** A ray to the point on the face it passes through, or null when it is
     *  behind the camera. The point may lie outside 0..1 -- off the tile. */
    fun pointOf(b: Basis, d: DoubleArray): Pair<Double, Double>? {
        val z = dot(d, b.f)
        if (z <= 1e-6) return null
        val a = dot(d, b.r) / z
        val v = dot(d, b.u) / z
        return Pair((a + b.t) / (2.0 * b.t), (b.t - v) / (2.0 * b.t))
    }

    /**
     * Where the handles start on a wall face before anybody drags them.
     *
     * With the room measured, from the same geometry that sizes the FOV: the
     * wall's corners as seen from the planned station. Unmeasured, an inset
     * square -- the handles only have to be near enough to grab.
     */
    fun startCorners(
        face: String,
        yawDeg: Double,
        fovDeg: Double,
        plan: CaptureGeometry.Plan?,
    ): List<DoubleArray> {
        val b = basis(yawDeg, 0.0, fovDeg)
        if (plan == null) {
            return listOf(0.15 to 0.15, 0.85 to 0.15, 0.85 to 0.85, 0.15 to 0.85)
                .map { (x, y) -> rayAt(b, x, y) }
        }
        val s = plan.station
        val (span, dist, left) = when (face) {
            "front", "back" -> Triple(plan.lengthIn,
                if (face == "front") plan.widthIn - s.yIn else s.yIn,
                if (face == "front") s.xIn else plan.lengthIn - s.xIn)
            else -> Triple(plan.widthIn,
                if (face == "right") plan.lengthIn - s.xIn else s.xIn,
                if (face == "right") plan.widthIn - s.yIn else s.yIn)
        }
        val d = dist.coerceAtLeast(1.0)
        val xl = -left / d
        val xr = (span - left) / d
        val zt = (plan.heightIn - s.zIn) / d
        val zb = -s.zIn / d
        fun at(a: Double, v: Double): DoubleArray {
            val x = ((a + b.t) / (2 * b.t)).coerceIn(0.02, 0.98)
            val y = ((b.t - v) / (2 * b.t)).coerceIn(0.02, 0.98)
            return rayAt(b, x, y)
        }
        return listOf(at(xl, zt), at(xr, zt), at(xr, zb), at(xl, zb))
    }

    data class Measure(
        /** Wall width over wall height, from the photo alone. */
        val ratio: Double,
        /** Worst corner's departure from 90 degrees. */
        val outOfSquareDeg: Double,
        /** False when the corners cross over or sit behind the camera. */
        val valid: Boolean,
    ) {
        val square: Boolean get() = valid && outOfSquareDeg <= SQUARE_TOLERANCE_DEG
        /** The length the photo implies for a wall of this height. */
        fun lengthFor(height: Double): Double = ratio * height
    }

    /**
     * The rectangle's proportion from four corner rays, TL TR BR BL.
     *
     * The corners are P = t*d along each ray, and a rectangle is first a
     * parallelogram: TL + BR = TR + BL. With t_TL fixed at 1 that is three
     * equations in the other three distances. Rotation never enters, so the
     * tripod's tilt cannot bias the answer.
     */
    fun measure(corners: List<DoubleArray>): Measure {
        require(corners.size == 4) { "four corners, TL TR BR BL" }
        val (tl, trr, br, bl) = corners
        // -t2*TR + t3*BR - t4*BL = -TL
        val m = arrayOf(
            doubleArrayOf(-trr[0], br[0], -bl[0]),
            doubleArrayOf(-trr[1], br[1], -bl[1]),
            doubleArrayOf(-trr[2], br[2], -bl[2]))
        val t = solve(m, doubleArrayOf(-tl[0], -tl[1], -tl[2]))
            ?: return Measure(1.0, 90.0, false)
        if (t.any { it <= 0 || it.isNaN() }) return Measure(1.0, 90.0, false)
        val p = listOf(tl, scale(trr, t[0]), scale(br, t[1]), scale(bl, t[2]))
        val top = sub(p[1], p[0]); val bottom = sub(p[2], p[3])
        val leftE = sub(p[3], p[0]); val rightE = sub(p[2], p[1])
        val w = (norm(top) + norm(bottom)) / 2
        val h = (norm(leftE) + norm(rightE)) / 2
        if (h <= 0) return Measure(1.0, 90.0, false)
        val worst = listOf(
            angle(top, leftE), angle(scale(top, -1.0), rightE),
            angle(bottom, scale(rightE, -1.0)), angle(scale(bottom, -1.0), scale(leftE, -1.0)))
            .maxOf { abs(90.0 - it) }
        return Measure(w / h, worst, true)
    }

    /**
     * The elevation: the wall re-sampled so its four corners land on the
     * corners of a w:h rectangle, with a small border of what lies beyond.
     *
     * Output pixel -> wall coordinates (0..1 across, 0..1 down) -> through the
     * homography to a point on the marking face -> a ray -> the pano. The
     * homography is exact for a flat wall, which is why only the wall plane is
     * true to scale and a sofa standing in front of it is not.
     */
    fun elevation(
        pano: Panorama.Image,
        corners: List<DoubleArray>,
        widthPx: Int,
        marginFrac: Double = 0.03,
    ): Panorama.Image {
        val m = measure(corners)
        val ratio = if (m.valid) m.ratio.coerceIn(0.1, 10.0) else 1.0
        // Mark on a face aimed at the middle of the four, wide enough for all.
        val mid = unit(DoubleArray(3) { i -> corners.sumOf { it[i] } })
        val yaw = Math.toDegrees(atan2(mid[0], mid[2]))
        val pitch = Math.toDegrees(atan2(mid[1], hypot(mid[0], mid[2])))
        val b = basis(yaw, pitch, 170.0)
        val pts = corners.map { pointOf(b, it) ?: Pair(0.5, 0.5) }
        val hmg = homography(
            listOf(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 1.0), pts)
        val outW = widthPx.coerceIn(64, Panorama.FACE_PX_MAX)
        val outH = (outW / ratio).toInt().coerceIn(32, Panorama.FACE_PX_MAX)
        val out = IntArray(outW * outH)
        val span = 1.0 + 2 * marginFrac
        for (row in 0 until outH) {
            val wy = -marginFrac + span * (row + 0.5) / outH
            for (col in 0 until outW) {
                val wx = -marginFrac + span * (col + 0.5) / outW
                val (sx, sy) = apply(hmg, wx, wy)
                out[row * outW + col] = sample(pano, rayAt(b, sx, sy))
            }
        }
        return Panorama.Image(outW, outH, out)
    }

    // ---------------------------------------------------------------- maths

    /** 3x3 homography taking each src point to its dst point (row-major, h33 = 1). */
    fun homography(src: List<Pair<Double, Double>>, dst: List<Pair<Double, Double>>): DoubleArray {
        val a = Array(8) { DoubleArray(8) }
        val bv = DoubleArray(8)
        for (i in 0 until 4) {
            val (x, y) = src[i]; val (u, v) = dst[i]
            a[2 * i] = doubleArrayOf(x, y, 1.0, 0.0, 0.0, 0.0, -u * x, -u * y); bv[2 * i] = u
            a[2 * i + 1] = doubleArrayOf(0.0, 0.0, 0.0, x, y, 1.0, -v * x, -v * y); bv[2 * i + 1] = v
        }
        val h = solve(a, bv) ?: error("degenerate corners")
        return h + 1.0
    }

    fun apply(h: DoubleArray, x: Double, y: Double): Pair<Double, Double> {
        val w = h[6] * x + h[7] * y + h[8]
        return Pair((h[0] * x + h[1] * y + h[2]) / w, (h[3] * x + h[4] * y + h[5]) / w)
    }

    /** Bilinear lookup in the equirect for a ray, with the same seam and pole
     *  handling as Panorama.faceFromEquirect. */
    fun sample(pano: Panorama.Image, d: DoubleArray): Int {
        val w = pano.width; val h = pano.height
        val lam = atan2(d[0], d[2])
        val phi = atan2(d[1], hypot(d[0], d[2]))
        val x = (lam / (2.0 * Math.PI) + 0.5) * w - 0.5
        val y = (0.5 - phi / Math.PI) * h - 0.5
        val x0 = floor(x).toInt(); val y0 = floor(y).toInt()
        val fx = x - x0; val fy = y - y0
        val xa = Math.floorMod(x0, w); val xb = Math.floorMod(x0 + 1, w)
        val ya = y0.coerceIn(0, h - 1); val yb = (y0 + 1).coerceIn(0, h - 1)
        val p00 = pano.pixels[ya * w + xa]; val p10 = pano.pixels[ya * w + xb]
        val p01 = pano.pixels[yb * w + xa]; val p11 = pano.pixels[yb * w + xb]
        var packed = 0
        for (shift in intArrayOf(16, 8, 0)) {
            val v = (p00 ushr shift and 0xFF) * (1 - fx) * (1 - fy) +
                (p10 ushr shift and 0xFF) * fx * (1 - fy) +
                (p01 ushr shift and 0xFF) * (1 - fx) * fy +
                (p11 ushr shift and 0xFF) * fx * fy
            packed = packed or ((v + 0.5).toInt().coerceIn(0, 255) shl shift)
        }
        return packed
    }

    /** Gaussian elimination with partial pivoting; null when singular. */
    fun solve(a0: Array<DoubleArray>, b0: DoubleArray): DoubleArray? {
        val n = b0.size
        val a = Array(n) { a0[it].copyOf() }
        val b = b0.copyOf()
        for (i in 0 until n) {
            var p = i
            for (k in i + 1 until n) if (abs(a[k][i]) > abs(a[p][i])) p = k
            if (abs(a[p][i]) < 1e-12) return null
            val tr = a[i]; a[i] = a[p]; a[p] = tr
            val tb = b[i]; b[i] = b[p]; b[p] = tb
            for (k in i + 1 until n) {
                val f = a[k][i] / a[i][i]
                for (j in i until n) a[k][j] -= f * a[i][j]
                b[k] -= f * b[i]
            }
        }
        val x = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var s = b[i]
            for (j in i + 1 until n) s -= a[i][j] * x[j]
            x[i] = s / a[i][i]
        }
        return x
    }

    fun dot(a: DoubleArray, b: DoubleArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
    private fun sub(a: DoubleArray, b: DoubleArray) = doubleArrayOf(a[0] - b[0], a[1] - b[1], a[2] - b[2])
    private fun scale(a: DoubleArray, k: Double) = doubleArrayOf(a[0] * k, a[1] * k, a[2] * k)
    private fun norm(a: DoubleArray) = sqrt(dot(a, a))
    fun unit(a: DoubleArray): DoubleArray { val l = norm(a); return doubleArrayOf(a[0] / l, a[1] / l, a[2] / l) }
    private fun angle(a: DoubleArray, b: DoubleArray) =
        Math.toDegrees(acos((dot(a, b) / (norm(a) * norm(b))).coerceIn(-1.0, 1.0)))
}
