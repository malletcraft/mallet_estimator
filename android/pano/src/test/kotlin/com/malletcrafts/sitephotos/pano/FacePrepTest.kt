package com.malletcrafts.sitephotos.pano

import com.malletcrafts.sitephotos.pano.FacePrep.Detail
import com.malletcrafts.sitephotos.pano.FacePrep.MLine
import com.malletcrafts.sitephotos.pano.FacePrep.Room
import com.malletcrafts.sitephotos.pano.FacePrep.Step
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The numbers here are the ones the browser prototype gave on the Vrushal
 * site sheets (2026-10-09), so the app and the prototype Amit approved agree.
 */
class FacePrepTest {

    /** A floor-plan step in mm, as the site sheet gives it: [x0, z0, x1, z1] from the front-left corner. */
    private fun Room.siteStep(kind: String, x0: Int, z0: Int, x1: Int, z1: Int) =
        face("floor").steps.add(Step(kind, x0.toDouble() / X, z0.toDouble() / Z, x1.toDouble() / X, z1.toDouble() / Z, site = true))

    @Test
    fun `a missing corner shortens the walls it ends - master bedroom, kitchen, guest`() {
        val mb = Room(2665, 3336, 4318).apply { siteStep("step", 1638, 0, 3336, 292) }
        assertEquals(4026, FacePrep.faceDims(mb, "right").first)   // site sheet 4013
        assertEquals(3336, FacePrep.faceDims(mb, "front").first)   // the recess runs along the front wall: not shortened
        val kitchen = Room(2665, 2815, 4026).apply { siteStep("step", 1605, 0, 2815, 311) }
        assertEquals(3715, FacePrep.faceDims(kitchen, "right").first)   // site sheet 3713
        val guest = Room(2677, 2678, 3319).apply { siteStep("step", 0, 0, 55, 627); siteStep("step", 454, 0, 2314, 309) }
        assertEquals(2623, FacePrep.faceDims(guest, "front").first)    // site sheet 2621-2630
        val living = Room(2680, 4365, 3024).apply { siteStep("step", 0, 2624, 102, 3024) }
        assertEquals(4263, FacePrep.faceDims(living, "back").first)    // site sheet 4263
        assertEquals(4365, FacePrep.faceDims(living, "front").first)
    }

    @Test
    fun `columns never shorten a wall`() {
        val r = Room(2600, 3000, 4000).apply { siteStep("column", 0, 0, 300, 600) }
        assertEquals(3000, FacePrep.faceDims(r, "front").first)
        assertEquals(4000, FacePrep.faceDims(r, "left").first)
    }

    @Test
    fun `the outline steps around a corner column and still adds up to the box`() {
        val r = Room(2600, 3000, 4000)
        r.face("front").steps.add(Step("column", 0.0, 0.0, 0.1, 1.0))   // 300 wide, full height, at the left
        val ps = FacePrep.outline(r, "front")
        val top = ps.filter { it.side == "top" }.sumOf { it.len }
        val stepFaces = ps.filter { it.side == null }.map { it.len }
        assertEquals(2700, top)
        assertEquals(listOf(2600), stepFaces)   // the column's own face, full height
        assertEquals(2 * 2700, ps.filter { it.h }.sumOf { it.len })   // a full-height column takes the whole left strip
    }

    @Test
    fun `box dividers cut the edge pieces`() {
        val r = Room(2600, 3000, 4000)
        r.face("front").splits.getValue("top").add(0.25)
        assertEquals(listOf(750, 2250), FacePrep.outline(r, "front").filter { it.side == "top" }.map { it.len })
    }

    @Test
    fun `a wall column shows on the floor plan corrected for its depth`() {
        val r = Room(2665, 3336, 4318).apply { siteStep("column", 3036, 1500, 3336, 1800) }   // 300x300 against the right wall
        r.face("right").steps.add(Step("column", 0.3, 0.0, 0.4, 1.0))
        val g = FacePrep.plateGuides(r, "floor").single()
        assertTrue(g.corrected, "matched to the plan column, so depth-corrected")
        val wg = FacePrep.wallGuides(r, "right").single()
        assertTrue(wg.u1 > wg.u0)
    }

    @Test
    fun `a door's locators and laser readings - size from the drag corner, then slide`() {
        val r = Room(2677, 2678, 3319)
        val f = r.face("front")
        f.dets.add(Detail("door", 0.30, 0.25, 0.62, 1.0, sx = 0, sy = 1))
        val locs = FacePrep.locators("front", f, 0)
        assertEquals(listOf("left wall"), locs.map { it.to })   // touches the floor: only the across locator
        val t = FacePrep.laserTargets(r, "front").associateBy { it.key }
        t.getValue("d0:w").set(900); t.getValue("d0:h").set(2100)
        val (w, h) = FacePrep.faceDims(r, "front")
        assertEquals(900, FacePrep.mmU(w, f.dets[0].u0, f.dets[0].u1))
        assertEquals(0.30, f.dets[0].u0, 1e-9)                   // started at the left: the left edge stays
        assertEquals(1.0, f.dets[0].v1, 1e-9)                    // started at the bottom: the bottom stays
        assertEquals(2100, FacePrep.mmV(h, f.dets[0].v0, f.dets[0].v1))
        FacePrep.laserTargets(r, "front").first { it.key == "d0:lh" }.set(650)
        assertEquals(650, FacePrep.mmU(w, 0.0, f.dets[0].u0))
        val after = FacePrep.laserTargets(r, "front").associateBy { it.key }
        assertTrue(after.getValue("d0:w").agrees && after.getValue("d0:lh").agrees)
        f.dets[0].u0 += 0.02; f.dets[0].u1 += 0.02                // dragged after measuring
        val moved = FacePrep.laserTargets(r, "front").first { it.key == "d0:lh" }
        assertEquals(false, moved.agrees)
        assertTrue(FacePrep.lzText(moved.now, moved.got, " from left wall").contains("≠ laser 650"))
    }

    @Test
    fun `room readings change the room`() {
        val r = Room(2600, 3000, 4000)
        FacePrep.laserTargets(r, "floor").first { it.key == "room:X" }.set(3012)
        assertEquals(3012, r.X); assertEquals(3012, r.laser["X"])
    }

    @Test
    fun `lines - true mm, level snap, pieces, laser stretch`() {
        val r = Room(2677, 2623, 3319)
        val (w, h) = FacePrep.faceDims(r, "front")
        val l = MLine(doubleArrayOf(0.1, 0.9), doubleArrayOf(0.9, 0.9))
        assertEquals((0.8 * w).toInt(), FacePrep.lineMm(w, h, l.a, l.b))
        val snapped = FacePrep.lineSnapAxis(w, h, l.a, doubleArrayOf(0.9, 0.905))
        assertEquals(0.9, snapped[1], 1e-12)
        l.ts.add(0.5)
        assertEquals(2, FacePrep.linePieces(w, h, l).size)
        r.face("front").lines.add(l)
        FacePrep.laserTargets(r, "front").first { it.key == "l0" }.set(1500)
        assertEquals(1500, FacePrep.lineMm(w, h, l.a, l.b), "stretched from its start")
        assertEquals(0.1, l.a[0], 1e-12)
        assertEquals(0.5, FacePrep.lineProj(w, h, l, l.at(0.5)), 1e-9)
    }

    @Test
    fun `line divider guide turns on a door edge`() {
        val r = Room(2677, 2678, 3319)
        val f = r.face("front")
        f.dets.add(Detail("door", 0.30, 0.25, 0.62, 1.0))
        val l = MLine(doubleArrayOf(0.1, 0.9), doubleArrayOf(0.9, 0.9))
        val g = FacePrep.lineGuide(r, "front", FacePrep.Grid(), l, 0.25)   // at u = 0.30
        assertTrue(g.onRef); assertNotNull(g.upright); assertNull(g.level)
        assertTrue(!FacePrep.lineGuide(r, "front", FacePrep.Grid(), l, 0.45).onRef)
    }

    @Test
    fun `grid - spacing, corner and shift as the prototype gave them`() {
        val (us, vs) = FacePrep.gridLines(FacePrep.Grid(on = true, x = 600, y = 300, fromLeft = false, sy = 75), 2623, 2677)
        assertEquals(listOf(600, 1200, 1800, 2400), us.map { it.second })
        assertEquals(listOf(75, 375, 675, 975, 1275, 1575, 1875, 2175, 2475), vs.map { it.second })
        assertEquals(1 - 600.0 / 2623, us[0].first, 1e-9)
        assertEquals(1 - 75.0 / 2677, vs[0].first, 1e-9)
        assertTrue(FacePrep.gridLines(FacePrep.Grid(on = false), 2623, 2677).first.isEmpty())
    }

    @Test
    fun `camera guess - centred camera sees the front wall centred`() {
        val r = Room(2600, 3000, 4000, ch = 1300)
        val b = FacePrep.cameraGuess(r, "front", 110.0)!!
        assertEquals(0.5, (b.x0 + b.x1) / 2, 1e-9)
        assertEquals(0.5, (b.y0 + b.y1) / 2, 1e-9)
        val fl = FacePrep.cameraGuess(r, "floor", 110.0)!!
        assertTrue(fl.y1 - fl.y0 > fl.x1 - fl.x0, "the room's length runs up the floor photo")
    }

    @Test
    fun `an elevation's box is where the split put the corners, caption strip excluded`() {
        // a 1600 x 1200 elevation; captionedBitmap adds max(1200*0.052, 28) = 62 px below
        val b = FacePrep.elevationBox(1600, 1262)
        val span = 1.12
        assertEquals(0.06 / span, b.x0, 1e-12); assertEquals(1.06 / span, b.x1, 1e-12)
        assertEquals(0.06 / span * 1200 / 1600, b.y0, 1e-12); assertEquals(1.06 / span * 1200 / 1600, b.y1, 1e-12)
        val small = FacePrep.elevationBox(400, 328)   // 300 tall: strip clamps to 28
        assertEquals(1.06 / span * 300 / 400, small.y1, 1e-12)
    }

    @Test
    fun `measures list every figure`() {
        val r = Room(2677, 2678, 3319)
        r.face("front").dets.add(Detail("window", 0.3, 0.3, 0.6, 0.6))
        val rows = FacePrep.measures(r, "front").map { it.first }
        assertTrue("Window 1 width" in rows && "Window 1 height" in rows && rows.any { it.startsWith("Window 1 from") })
    }
}
