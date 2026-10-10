package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomSetupTest {

    // A room that is not square, turned 7 degrees off the camera, with the
    // camera well off centre: floor corners FL FR BR BL as (x, z) mm, camera
    // at the origin, front wall ahead (+z).
    private val turn = Math.toRadians(7.0)
    private fun rot(x: Double, z: Double) =
        doubleArrayOf(x * cos(turn) - z * sin(turn), x * sin(turn) + z * cos(turn))
    private val truth = listOf(
        rot(-1150.0, 2050.0), rot(1720.0, 1980.0), rot(1580.0, -1310.0), rot(-1240.0, -1190.0))
    private val camH = 1340.0
    private val roomH = 2765.0

    /** The carpet maximums as a tape would give them: the extent along the
     *  front wall and across it. */
    private fun carpet(c: List<DoubleArray>): Pair<Double, Double> {
        var ex = c[1][0] - c[0][0]; var ez = c[1][1] - c[0][1]
        val n = hypot(ex, ez); ex /= n; ez /= n
        val along = c.map { it[0] * ex + it[1] * ez }
        val across = c.map { it[0] * ez - it[1] * ex }
        return (across.max() - across.min()) to (along.max() - along.min())
    }

    private val px = 1024
    /** Where a person who tapped exactly would put each dot. */
    private fun taps() = truth.map { RoomSetup.floorFrac(it[0] / camH, it[1] / camH, px) }

    @Test fun `floor face fractions round-trip and match faceFromEquirect's pixel centres`() {
        val step = 2 * RoomSetup.T / (px - 1)
        for (k in listOf(0, 1, 511, 1023)) {
            val a = -RoomSetup.T + k * step
            val f = RoomSetup.floorFrac(a, -a, px)
            assertEquals((k + 0.5) / px, f[0], 1e-12)
            assertEquals((k + 0.5) / px, f[1], 1e-12)
            val back = RoomSetup.floorPoint(f[0], f[1], px)
            assertEquals(a, back[0], 1e-12); assertEquals(-a, back[1], 1e-12)
        }
    }

    @Test fun `the down face is a parallel picture of the floor`() {
        // A floor point (x, -h, z) sits at (x/h, z/h) on the down face: check
        // against the face basis faceFromEquirect itself uses.
        val b = WallCorners.basis(0.0, -90.0, RoomSetup.FLOOR_FOV)
        for (c in truth) {
            val p = WallCorners.pointOf(b, WallCorners.unit(doubleArrayOf(c[0], -camH, c[1])))!!
            assertEquals((c[0] / camH + b.t) / (2 * b.t), p.first, 1e-9)
            assertEquals((b.t - c[1] / camH) / (2 * b.t), p.second, 1e-9)
        }
    }

    @Test fun `exact taps give the camera height, the corners and zero disagreement`() {
        val (l, w) = carpet(truth)
        val pts = taps().map { RoomSetup.floorPoint(it[0], it[1], px) }
        val plan = assertNotNull(RoomSetup.plan(pts, l, w))
        assertEquals(camH, plan.h, 1e-6)
        assertEquals(0.0, plan.agreement, 1e-9)
        assertTrue(plan.agrees)
        for (i in 0 until 4) {
            assertEquals(truth[i][0], plan.corners[i][0], 1e-6)
            assertEquals(truth[i][1], plan.corners[i][1], 1e-6)
        }
        val sides = (0 until 4).map { i ->
            val a = truth[i]; val c = truth[(i + 1) % 4]; hypot(c[0] - a[0], c[1] - a[1]) }
        for (i in 0 until 4) assertEquals(sides[i], plan.sidesMm[i], 1e-6)
    }

    @Test fun `a carpet that disagrees with the dots says by how much`() {
        val (l, w) = carpet(truth)
        val pts = taps().map { RoomSetup.floorPoint(it[0], it[1], px) }
        val plan = assertNotNull(RoomSetup.plan(pts, l * 1.06, w))
        assertTrue(plan.agreement > 0.02 && plan.agreement < 0.06, "agreement ${plan.agreement}")
        assertTrue(!plan.agrees)
    }

    @Test fun `a quarter-pixel tap error stays within a few centimetres`() {
        val (l, w) = carpet(truth)
        val wobble = listOf(0.25 to -0.25, -0.25 to 0.25, 0.25 to 0.25, -0.25 to -0.25)
        val pts = taps().mapIndexed { i, f ->
            RoomSetup.floorPoint(f[0] + wobble[i].first / px, f[1] + wobble[i].second / px, px) }
        val plan = assertNotNull(RoomSetup.plan(pts, l, w))
        for (i in 0 until 4) assertTrue(hypot(plan.corners[i][0] - truth[i][0],
            plan.corners[i][1] - truth[i][1]) < 40.0)
        assertTrue(abs(plan.h - camH) < 30.0)
    }

    @Test fun `the plan becomes the RoomQuad the elevations are built from`() {
        val (l, w) = carpet(truth)
        val pts = taps().map { RoomSetup.floorPoint(it[0], it[1], px) }
        val plan = RoomSetup.plan(pts, l, w)!!
        val q = assertNotNull(RoomSetup.roomQuad(plan, roomH))
        assertEquals((roomH - camH) / camH, q.ceiling, 1e-9)
        assertEquals(camH, q.mmPerUnit(roomH), 1e-6)
        val fp = q.floorPlan()!!
        for (i in 0 until 4) {
            assertEquals(truth[i][0] / camH, fp[i][0], 1e-9)
            assertEquals(truth[i][1] / camH, fp[i][1], 1e-9)
        }
        val walls = q.wallsMm(roomH)
        for (i in 0 until 4) assertEquals(plan.sidesMm[i], walls[i], 1e-6)
        // And the corners hand each wall face a true rectangle of the right shape.
        val rc = q.corners()
        for ((face, li, ri) in RoomSetup.WALLS) {
            val m = WallCorners.measure(rc.forFace(face))
            assertTrue(m.valid && m.square, face)
            val len = hypot(truth[ri][0] - truth[li][0], truth[ri][1] - truth[li][1])
            assertEquals(len / roomH, m.ratio, 1e-6, face)
        }
        // A ceiling below the camera is not a room.
        assertNull(RoomSetup.roomQuad(plan, camH * 0.9))
    }

    // ------------------------------------------------- synthetic 360 of the room

    private val FLOOR = 0x404040; private val CEIL = 0xF0F0F0
    private val WALL_COL = listOf(0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00)

    /** What a ray from the camera hits in the true room, as a flat colour. */
    private fun hit(d: DoubleArray): Int {
        var best = Double.MAX_VALUE; var col = 0
        if (d[1] < 0) { best = -camH / d[1]; col = FLOOR }
        if (d[1] > 0) { val t = (roomH - camH) / d[1]; if (t < best) { best = t; col = CEIL } }
        for (i in 0 until 4) {
            val a = truth[i]; val b = truth[(i + 1) % 4]
            val ex = b[0] - a[0]; val ez = b[1] - a[1]
            val den = d[0] * ez - d[2] * ex
            if (abs(den) < 1e-12) continue
            val t = (a[0] * ez - a[1] * ex) / den
            val s = (a[0] * d[2] - a[1] * d[0]) / den
            if (t > 0 && s in 0.0..1.0 && t < best) { best = t; col = WALL_COL[i] }
        }
        return col
    }

    private fun synthPano(w: Int): Panorama.Image {
        val h = w / 2
        val px = IntArray(w * h)
        for (y in 0 until h) {
            val phi = (0.5 - (y + 0.5) / h) * Math.PI
            for (x in 0 until w) {
                val lam = ((x + 0.5) / w - 0.5) * 2 * Math.PI
                px[y * w + x] = hit(doubleArrayOf(cos(phi) * sin(lam), sin(phi), cos(phi) * cos(lam)))
            }
        }
        return Panorama.Image(w, h, px)
    }

    @Test fun `the floor face of a synthetic 360 shows the floor where the plan says`() {
        val pano = synthPano(2048)
        val face = RoomSetup.floorFace(pano, 512)
        // The middle of the floor, between the four corners, is floor.
        val cx = truth.sumOf { it[0] } / 4 / camH; val cz = truth.sumOf { it[1] } / 4 / camH
        val f = RoomSetup.floorFrac(cx, cz, 512)
        assertEquals(FLOOR, face.pixels[(f[1] * 512).toInt() * 512 + (f[0] * 512).toInt()])
    }

    @Test fun `a squared-on wall puts the wall between the floor line and the ceiling line`() {
        val pano = synthPano(2048)
        for ((face, li, ri) in RoomSetup.WALLS) {
            val wall = RoomSetup.wall(pano, truth[li], truth[ri], camH, roomH, 240)
            val img = wall.image!!
            val col = img.width / 2
            fun at(aboveFloor: Double) =
                img.pixels[(wall.fracOf(aboveFloor) * img.height).toInt().coerceIn(0, img.height - 1) * img.width + col]
            val wallIx = RoomSetup.WALLS.indexOfFirst { it.first == face }
            assertEquals(WALL_COL[wallIx], at(roomH * 0.5), face)
            assertEquals(WALL_COL[wallIx], at(60.0), face)
            assertEquals(WALL_COL[wallIx], at(roomH - 60.0), face)
            assertEquals(FLOOR, at(-100.0), face)
            assertEquals(CEIL, at(roomH + 150.0), face)
            // Corners land where the frame says they do.
            assertEquals(1.0 / (1 + 2 * RoomSetup.WALL_MARGIN) * RoomSetup.WALL_MARGIN, wall.xFracOf(0.0), 1e-12)
            assertEquals(roomH, wall.heightAt(wall.fracOf(roomH)), 1e-9)
            // True proportions: pixels per mm the same both ways (to a pixel).
            val pxPerMmX = img.width / (wall.lengthMm * (1 + 2 * RoomSetup.WALL_MARGIN))
            val pxPerMmY = img.height / (wall.yTop - wall.yBot)
            assertTrue(abs(pxPerMmX - pxPerMmY) / pxPerMmX < 0.02 || img.height == img.width / 4 ||
                img.height == img.width * 3 / 2, face)
        }
    }

    @Test fun `the magnifier zooms about a point and never leaves the picture`() {
        val v0 = RoomSetup.View()
        val v = v0.zoomAt(2.0, 0.25, 0.25)
        val p = v.fromScreen(0.25, 0.25)
        assertEquals(0.25, p[0], 1e-12); assertEquals(0.25, p[1], 1e-12)
        val far = v.slid(5.0, 5.0)
        val win = far.window()
        assertTrue(win[0] >= -1e-12 && win[1] >= -1e-12 && win[0] + win[2] <= 1 + 1e-12)
        val s = v.toScreen(0.4, 0.3); val back = v.fromScreen(s[0], s[1])
        assertEquals(0.4, back[0], 1e-12); assertEquals(0.3, back[1], 1e-12)
        assertEquals(RoomSetup.View.Z_MAX, v0.zoomAt(100.0, 0.5, 0.5).z, 1e-12)
        assertEquals(1.0, v.zoomAt(0.01, 0.5, 0.5).z, 1e-12)
    }

    @Test fun `start points sit inside the floor face`() {
        for (p in RoomSetup.startPoints(9000.0, 7000.0)) {
            assertTrue(max(abs(p[0]), abs(p[1])) <= RoomSetup.T)
        }
        val s = RoomSetup.startPoints(3300.0, 2700.0)
        assertTrue(s[0][0] < 0 && s[0][1] > 0 && s[2][0] > 0 && s[2][1] < 0)
    }
}
