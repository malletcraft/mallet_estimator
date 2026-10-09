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
    fun `a door's site figures are stored beside the calculated ones and never move it`() {
        val r = Room(2677, 2678, 3319)
        val f = r.face("front")
        val door = Detail("door", 0.30, 0.25, 0.62, 1.0, sx = 0, sy = 1)
        f.dets.add(door)
        val locs = FacePrep.locators("front", f, 0)
        assertEquals(listOf("left wall"), locs.map { it.to })   // touches the floor: only the across locator
        val (w, h) = FacePrep.faceDims(r, "front")
        val calcW = FacePrep.mmU(w, door.u0, door.u1); val calcH = FacePrep.mmV(h, door.v0, door.v1)
        val keys = FacePrep.dims(r, "front").map { it.key }
        assertTrue(FacePrep.detKey(door, "w") in keys && FacePrep.detKey(door, "h") in keys && FacePrep.detKey(door, "lh") in keys)
        FacePrep.setSite(f, FacePrep.detKey(door, "w"), 900)
        FacePrep.setSite(f, FacePrep.detKey(door, "h"), 2100)
        FacePrep.setSite(f, FacePrep.detKey(door, "lh"), 650)
        // geometry unchanged
        assertEquals(0.30, door.u0, 1e-12); assertEquals(0.62, door.u1, 1e-12)
        assertEquals(0.25, door.v0, 1e-12); assertEquals(1.0, door.v1, 1e-12)
        val ds = FacePrep.dims(r, "front").associateBy { it.key }
        val dw = ds.getValue(FacePrep.detKey(door, "w"))
        assertEquals(calcW, dw.calc); assertEquals(900, dw.site)
        assertEquals(calcH, ds.getValue(FacePrep.detKey(door, "h")).calc)
        assertEquals(2100, ds.getValue(FacePrep.detKey(door, "h")).site)
        assertEquals(650, ds.getValue(FacePrep.detKey(door, "lh")).site)
        FacePrep.clearSite(f, FacePrep.detKey(door, "lh"))
        assertNull(FacePrep.dims(r, "front").first { it.key == FacePrep.detKey(door, "lh") }.site)
    }

    @Test
    fun `site keys survive deleting a neighbour`() {
        val r = Room(2677, 2678, 3319)
        val f = r.face("front")
        val a = Detail("window", 0.1, 0.2, 0.3, 0.5); val b = Detail("window", 0.6, 0.2, 0.8, 0.5)
        f.dets.add(a); f.dets.add(b)
        FacePrep.setSite(f, FacePrep.detKey(a, "w"), 700); FacePrep.setSite(f, FacePrep.detKey(b, "w"), 710)
        f.dets.removeAt(0); FacePrep.forgetSites(f, "d:${a.id}")
        assertEquals(mapOf(FacePrep.detKey(b, "w") to 710), f.meas)
        assertEquals(710, FacePrep.dims(r, "front").first { it.key == FacePrep.detKey(b, "w") }.site)
    }

    @Test
    fun `next unmeasured walks the drawn order and wraps`() {
        val r = Room(2600, 3000, 4000)
        val f = r.face("front")
        val ds0 = FacePrep.dims(r, "front")
        assertEquals(listOf("E:top:1", "E:right:1", "E:bottom:1", "E:left:1", "T:top", "T:right", "T:bottom", "T:left"), ds0.map { it.key })
        assertEquals("E:right:1", FacePrep.nextUnmeasured(ds0, "E:top:1"))
        FacePrep.setSite(f, "E:right:1", 2601); FacePrep.setSite(f, "E:bottom:1", 2999)
        assertEquals("E:left:1", FacePrep.nextUnmeasured(FacePrep.dims(r, "front"), "E:top:1"))
        for (k in listOf("E:left:1", "T:top", "T:right", "T:bottom", "T:left")) FacePrep.setSite(f, k, 1)
        assertEquals("E:top:1", FacePrep.nextUnmeasured(FacePrep.dims(r, "front"), "T:left"), "wraps round to the start")
        FacePrep.setSite(f, "E:top:1", 1)
        assertNull(FacePrep.nextUnmeasured(FacePrep.dims(r, "front"), "E:top:1"))
        assertEquals(2, FacePrep.faceStatus(r, "front"))
        assertEquals(0, FacePrep.faceStatus(r, "back"))
        r.face("left").lines.add(MLine(doubleArrayOf(0.1, 0.5), doubleArrayOf(0.9, 0.5)))
        assertEquals(1, FacePrep.faceStatus(r, "left"))
    }

    @Test
    fun `duplicate makes a new thing with its own id and no site figures`() {
        val r = Room(2677, 2678, 3319); val f = r.face("front")
        f.dets.add(Detail("window", 0.1, 0.2, 0.3, 0.5)); FacePrep.setSite(f, FacePrep.detKey(f.dets[0], "w"), 700)
        val i = FacePrep.duplicate(f, "dets", 0)!!
        assertEquals(1, i); assertTrue(f.dets[1].id != f.dets[0].id)
        assertEquals(0.14, f.dets[1].u0, 1e-12)
        assertNull(FacePrep.dims(r, "front").first { it.key == FacePrep.detKey(f.dets[1], "w") }.site)
    }

    @Test
    fun `room readings change the room`() {
        val r = Room(2600, 3000, 4000)
        FacePrep.roomReading(r, "X", 3012)
        assertEquals(3012, r.X); assertEquals(3012, r.laser["X"])
    }

    @Test
    fun `lines - true mm, level snap, pieces, site figure leaves the line alone`() {
        val r = Room(2677, 2623, 3319)
        val (w, h) = FacePrep.faceDims(r, "front")
        val l = MLine(doubleArrayOf(0.1, 0.9), doubleArrayOf(0.9, 0.9))
        assertEquals((0.8 * w).toInt(), FacePrep.lineMm(w, h, l.a, l.b))
        val snapped = FacePrep.lineSnapAxis(w, h, l.a, doubleArrayOf(0.9, 0.905))
        assertEquals(0.9, snapped[1], 1e-12)
        l.ts.add(0.5)
        assertEquals(2, FacePrep.linePieces(w, h, l).size)
        r.face("front").lines.add(l)
        val lk = FacePrep.dims(r, "front").filter { it.key.startsWith("l:") }.map { it.key }
        assertEquals(listOf(FacePrep.lineKey(l, 1), FacePrep.lineKey(l, 2), FacePrep.lineKey(l)), lk)
        FacePrep.setSite(r.face("front"), FacePrep.lineKey(l), 1500)
        assertEquals((0.8 * w).toInt(), FacePrep.lineMm(w, h, l.a, l.b), "the line is not stretched")
        assertEquals(0.9, l.b[0], 1e-12)
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
    fun `measures CSV lists every figure, calculated and site`() {
        val r = Room(2677, 2678, 3319)
        val win = Detail("window", 0.3, 0.3, 0.6, 0.6)
        r.face("front").dets.add(win)
        FacePrep.setSite(r.face("front"), FacePrep.detKey(win, "w"), 801)
        val rows = FacePrep.csvRows(r, "front")
        val labels = rows.map { it.first }
        assertTrue("Window 1 width" in labels && "Window 1 height" in labels && labels.any { it.startsWith("Window 1 from") })
        val ww = rows.first { it.first == "Window 1 width" }
        assertEquals(FacePrep.mmU(2678, 0.3, 0.6), ww.second); assertEquals("801", ww.third)
        assertEquals("", rows.first { it.first == "Window 1 height" }.third)
        assertTrue(rows.any { it.first == "Top total" && it.second == 2678 })
    }
}
