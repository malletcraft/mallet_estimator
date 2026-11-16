package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * THE WALK AND ITS COUNTS (port 63 slice M1; DESIGN-port63 §1.5): the order of a face's stops, what counts as left,
 * next and previous, the counts the bar and the tabs show, the faces' order, and `‹` `›` inside one picked item. The
 * synthetic room of MeasTest: L 3319, W 2678, H 2677.
 */
class MeasureWalkTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0

    private fun ab(s: String): DoubleArray = SurveyPrep.size(s, L, W, H)
    private fun pt(x: Double, y: Double) = doubleArrayOf(x, y)

    private fun SurveyPrep.Prep.put(s: String, t: String, n: Int, x0: Double, x1: Double, y0: Double, y1: Double,
                                    extra: Map<String, Any?> = emptyMap()): SurveyPrep.Mark =
        SurveyPrep.Mark("id-$s-$t$n", t, SurveyPrep.CODE.getValue(s) + t + n, x0, x1, y0, y1, extra).also { surface(s).marks.add(it) }

    private fun stops(p: SurveyPrep.Prep, s: String = "front") = ab(s).let { MeasureWalk.stops(p, s, it[0], it[1]) }
    private fun next(p: SurveyPrep.Prep, after: String?, s: String = "front") = ab(s).let { MeasureWalk.next(p, s, it[0], it[1], after) }
    private fun prev(p: SurveyPrep.Prep, before: String?, s: String = "front") = ab(s).let { MeasureWalk.previous(p, s, it[0], it[1], before) }
    private fun left(p: SurveyPrep.Prep, code: String, s: String = "front") = ab(s).let { MeasureWalk.left(p, s, it[0], it[1], code) }
    private fun count(p: SurveyPrep.Prep, s: String = "front") = ab(s).let { MeasureWalk.count(p, s, it[0], it[1]) }.let { it.read to it.total }

    /** A front wall with a door, an electric point and a window; a Note, a wish-born door and a tape, which are never
     *  asked; a Line, an area and a chain that stops short of the corners. */
    private fun wall(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        // stored out of order on purpose: the walk goes by where a mark IS, not when it was placed
        p.put("front", "W", 1, 1500.0, 2400.0, 900.0, 2100.0)
        p.put("front", MarkTypes.NOTE, 1, 100.0, 150.0, 1200.0, 1250.0)
        p.put("front", "E", 1, 1325.0, 1375.0, 275.0, 325.0)
        p.put("front", "D", 2, 50.0, 250.0, 0.0, 2100.0, mapOf(MarkLife.KEY_BORN to MarkLife.WISH, MarkLife.KEY_LIFE to MarkLife.WISH))
        p.put("front", "D", 1, 300.0, 1200.0, 0.0, 2100.0)
        p.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 1000.0), pt(1234.0, 1000.0)), "t1")
        p.addMeas("front", SurveyMeas.LINE, listOf(pt(500.0, 1600.0), pt(1300.0, 2200.0)), "m1")
        p.addMeas("front", SurveyMeas.AREA, listOf(pt(1800.0, 300.0), pt(2400.0, 300.0), pt(2400.0, 800.0)), "m2")
        p.addChain("front", "x", listOf(pt(300.0, 2400.0), pt(1200.0, 2400.0), pt(2400.0, 2400.0)), listOf(null, null, null), "k1")
        return p
    }

    private val WALL = listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p", "FE1.cr", "FE1.cs", "FW1.w", "FW1.h", "FW1.r", "FW1.s", "FW1.p",
        "F-M1.len", "F-M2.per", "FK1.1", "FK1.2", "FK1.t")

    @Test fun `the stops of a face -- marks left to right, each in its checklist's order, then lines and areas, then chains`() {
        val p = wall()
        assertEquals(WALL, stops(p))
        // the Note, the tape and the door born a wish are in no stop; nothing is asked twice
        assertTrue(stops(p).none { it.startsWith("FNOTE") || it.startsWith("F-T") || it.startsWith("FD2") })
        assertEquals(WALL.size, stops(p).toSet().size)
        // the marks' order is rule 1's, whatever order they were stored in
        assertEquals(listOf("FD1", "FE1", "FW1"), MeasureWalk.marks(p, "front").map { it.code })
        // a face with nothing on it, and a surface the prep does not hold, ask nothing
        assertEquals(emptyList(), stops(p, "back"))
        assertEquals(emptyList(), MeasureWalk.stops(p, "nowhere", W, H))
        assertNull(MeasureWalk.next(p, "nowhere", W, H, null)); assertNull(MeasureWalk.previous(p, "nowhere", W, H, null))
        assertFalse(MeasureWalk.left(p, "nowhere", W, H, "FD1.w")); assertEquals(0, MeasureWalk.count(p, "nowhere", W, H).total)
        assertEquals(listOf("front", "right", "back", "left", "floor", "ceil"), MeasureWalk.FACES)
    }

    @Test fun `the order is exact -- x0 to the whole mm, then the lower first, then by code`() {
        val p = SurveyPrep.fresh(L, W, H)
        // two whose left edges differ by under a mm, either side of 300: both round to 300, so the LOWER goes first --
        // though the higher one's edge is the further left by that fraction of a mm
        p.put("front", "X", 1, 299.6, 900.0, 1500.0, 2000.0)
        p.put("front", "X", 2, 300.4, 900.0, 200.0, 700.0)
        // two that share the very same left edge: the LOWER first, though it was stored second and has the later code
        p.put("front", "X", 3, 1200.0, 1500.0, 1800.0, 2100.0)
        p.put("front", "X", 4, 1200.0, 1400.0, 300.0, 600.0)
        // two that share the very same box: by code, though stored the other way round
        p.put("front", "X", 7, 2000.0, 2300.0, 800.0, 1100.0)
        p.put("front", "X", 6, 2000.0, 2300.0, 800.0, 1100.0)
        // a point is ordered by its box like any mark: its left edge 1000, between the pairs
        p.put("front", "E", 1, 1000.0, 1050.0, 300.0, 350.0)
        // one whose left edge rounds a whole mm further left than the first pair goes before both
        p.put("front", "X", 5, 299.4, 600.0, 2300.0, 2500.0)
        val order = listOf("FX5", "FX2", "FX1", "FE1", "FX4", "FX3", "FX6", "FX7")
        assertEquals(order, MeasureWalk.marks(p, "front").map { it.code })
        assertEquals(order, stops(p).map { it.substringBefore('.') }.distinct())
        // the same marks stored in the opposite order walk in the same order: nothing depends on when a mark was placed
        val q = SurveyPrep.fresh(L, W, H)
        for (e in p.surface("front").marks.reversed()) q.surface("front").marks.add(e.copy())
        assertEquals(order, MeasureWalk.marks(q, "front").map { it.code })
        assertEquals(stops(p), stops(q))
    }

    @Test fun `next takes the next one left and wraps, previous the stop before read or not, and the counts follow`() {
        val p = wall()
        assertEquals(0 to 16, count(p))
        assertEquals("FD1.w", next(p, null))                 // entering 3 · Measure: the first unmeasured
        assertEquals("FD1.h", next(p, "FD1.w"))              // › skips: the dimension stays unmeasured
        assertEquals("FD1.w", next(p, "FK1.t"))              // ... and the walk comes round again
        assertEquals("FK1.t", prev(p, null))                 // ‹ with nothing active: the face's LAST stop
        assertEquals("FK1.t", prev(p, "FD1.w"))              // ‹ wraps
        assertEquals("FD1.p", prev(p, "FE1.cr"))             // ‹ crosses from one mark to the one before
        // readings land: the door's width and height, then the point
        for (k in listOf("FD1.w", "FD1.h", "FE1.cr", "FE1.cs")) p.actual[k] = 900.0
        assertEquals(4 to 16, count(p))
        assertFalse(left(p, "FD1.w")); assertTrue(left(p, "FD1.l"))
        assertEquals("FD1.l", next(p, null))
        assertEquals("FD1.l", next(p, "FD1.h"))
        assertEquals("FW1.w", next(p, "FD1.p"))              // skips the measured point
        assertEquals("FD1.h", prev(p, "FD1.l"))              // ‹ still reaches what is read, to correct it
        assertEquals("FE1.cs", prev(p, "FW1.w"))
        // a key that is no stop of this face any more (its mark deleted) reads as none: the first left, the last stop
        assertEquals("FD1.l", next(p, "FZ9.w")); assertEquals("FK1.t", prev(p, "FZ9.w"))
        assertFalse(left(p, "FZ9.w")); assertFalse(left(p, "FD2.w")); assertFalse(left(p, "FNOTE1.w"))
        // the only one left is its own next
        for (k in WALL) if (k != "FW1.s") p.actual[k] = 500.0
        assertEquals(15 to 16, count(p))
        assertEquals("FW1.s", next(p, "FW1.s")); assertEquals("FW1.s", next(p, null))
        // the last reading: nothing is left, the face is finished, and ‹ still walks back into it
        p.actual["FW1.s"] = 905.0
        assertNull(next(p, null)); assertNull(next(p, "FW1.s"))
        assertEquals(16 to 16, count(p))
        assertTrue(MeasureWalk.count(p, "front", W, H).done); assertEquals(0, MeasureWalk.count(p, "front", W, H).left)
        assertEquals("FK1.t", prev(p, null))
        // a face that asks nothing: no next, no previous, 0 of 0, and not "done"
        assertNull(next(p, null, "back")); assertNull(prev(p, null, "back")); assertEquals(0 to 0, count(p, "back"))
        assertFalse(MeasureWalk.count(p, "back", W, H).done)
        // a reading a wish-born door already holds from an earlier build is in no count, and is not removed
        p.actual["FD2.w"] = 200.0
        assertEquals(16 to 16, count(p)); assertEquals(200.0, p.actual["FD2.w"])
    }

    @Test fun `a derived chain segment and a derived total are not left, not counted, skipped by next, still reached by previous`() {
        val p = SurveyPrep.fresh(L, W, H)
        // FK1: three segments, stopping short of both corners
        p.addChain("front", "x", listOf(pt(200.0, 500.0), pt(1100.0, 500.0), pt(1900.0, 500.0), pt(2500.0, 500.0)), listOf(null, null, null, null), "k1")
        assertEquals(listOf("FK1.1", "FK1.2", "FK1.3", "FK1.t"), stops(p))
        assertEquals(0 to 4, count(p))
        // read two segments and the total: the third segment is DERIVED -- a figure, not a reading
        p.actual["FK1.1"] = 905.0; p.actual["FK1.2"] = 795.0; p.actual["FK1.t"] = 2300.0
        assertEquals(600.0, SurveyChain.vals(p.surface("front").chains[0], p.actual, W, H).value[2])
        assertFalse(left(p, "FK1.3"))
        assertEquals(3 to 3, count(p))                       // "all 3 measured" is true of three readings
        assertNull(next(p, null)); assertNull(next(p, "FK1.2"))
        assertEquals("FK1.3", prev(p, "FK1.t"))              // still a stop: ‹ reaches it, and it takes a reading
        assertEquals("FK1.3", stops(p)[2])
        // read every segment instead and leave the total: the total is derived
        p.actual.remove("FK1.t"); p.actual["FK1.3"] = 601.0
        assertFalse(left(p, "FK1.t")); assertEquals(3 to 3, count(p)); assertNull(next(p, null))
        assertEquals("FK1.t", prev(p, null))
        // with two segments unread nothing can be derived: both are left, and so is the total
        p.actual.remove("FK1.2"); p.actual.remove("FK1.3")
        assertTrue(left(p, "FK1.2")); assertTrue(left(p, "FK1.3")); assertTrue(left(p, "FK1.t"))
        assertEquals(1 to 4, count(p)); assertEquals("FK1.2", next(p, null)); assertEquals("FK1.2", next(p, "FK1.1"))
    }

    @Test fun `a chain total the laser gives is not a stop at all`() {
        val p = SurveyPrep.fresh(L, W, H)
        // corner to corner along the wall: the laser's W is its total
        p.addChain("front", "x", listOf(pt(0.0, 300.0), pt(905.0, 300.0), pt(W, 300.0)), listOf(null, null, null), "k1")
        assertEquals(listOf("FK1.1", "FK1.2"), stops(p))
        assertFalse(left(p, "FK1.t"))
        assertEquals(0 to 2, count(p))
        // one segment read: the other is derived from the laser's total, so nothing is left
        p.actual["FK1.1"] = 905.0
        assertEquals(1 to 1, count(p)); assertNull(next(p, null)); assertFalse(left(p, "FK1.2"))
        assertEquals("FK1.2", prev(p, null))
    }

    @Test fun `faces follow the tabs, and a finished face offers the next one with something left`() {
        val p = SurveyPrep.fresh(L, W, H)
        p.put("front", "D", 1, 300.0, 1200.0, 0.0, 2100.0)                     // 4
        p.put("back", "E", 1, 1000.0, 1050.0, 300.0, 350.0)                    // 2
        p.put("floor", "C", 1, 150.0, 450.0, 2800.0, 3100.0)                   // w h l f = 4
        p.put("ceil", "F", 1, 1300.0, 1350.0, 1600.0, 1650.0)                  // 2
        val r = MeasureWalk.room(p, ::ab)
        assertEquals(0 to 12, r.read to r.total)
        fun nf(from: String) = MeasureWalk.nextFace(p, from, ::ab)?.let { it.s to it.left }
        assertEquals("back" to 2, nf("front"))               // right has nothing on it: skipped
        assertEquals("back" to 2, nf("right"))
        assertEquals("floor" to 4, nf("back"))
        assertEquals("ceil" to 2, nf("floor"))
        assertEquals("front" to 4, nf("ceil"))               // wrapping
        // the front wall finished: from the ceiling the next is the back wall
        for (k in stops(p)) p.actual[k] = 700.0
        assertEquals("back" to 2, nf("ceil")); assertEquals("back" to 2, nf("front"))
        // one of the floor's four taken: the count it offers is what is LEFT there
        p.actual["GC1.w"] = 300.0
        assertEquals("floor" to 3, nf("back"))
        val r2 = MeasureWalk.room(p, ::ab); assertEquals(5 to 12, r2.read to r2.total); assertFalse(r2.done)
        // the room finished: nothing is offered from anywhere, and the room is done
        for (s in MeasureWalk.FACES) for (k in stops(p, s)) p.actual[k] = 700.0
        for (s in MeasureWalk.FACES) assertNull(nf(s), s)
        val r3 = MeasureWalk.room(p, ::ab); assertEquals(12 to 12, r3.read to r3.total); assertTrue(r3.done)
        // the only face with something left is never its own next
        p.actual.remove("BE1.cs")
        assertNull(nf("back")); assertEquals("back" to 1, nf("front"))
    }

    @Test fun `in step 2 Mark and step 4 360 the arrows step inside the picked item, read or not, wrapping`() {
        val p = wall()
        val door = assertNotNull(p.markOf("FD1.w")).second
        val keys = MeasureWalk.stopsOf(p, door.id, ::ab)
        assertEquals(listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p"), keys)
        assertEquals("FD1.h", MeasureWalk.step(keys, "FD1.w", true))
        assertEquals("FD1.w", MeasureWalk.step(keys, "FD1.p", true))        // › wraps inside the mark, never on to FE1
        assertEquals("FD1.p", MeasureWalk.step(keys, "FD1.w", false))       // ‹ wraps
        assertEquals("FD1.l", MeasureWalk.step(keys, "FD1.p", false))
        p.actual["FD1.h"] = 2100.0
        assertEquals("FD1.h", MeasureWalk.step(keys, "FD1.w", true))        // read or not: that is how it is corrected
        assertEquals("FD1.w", MeasureWalk.step(keys, null, true)); assertEquals("FD1.p", MeasureWalk.step(keys, null, false))
        assertEquals("FD1.w", MeasureWalk.step(keys, "FW1.w", true))        // a key of another item reads as none
        // a Line's and an area's one key: itself either way; a tape has none
        val line = MeasureWalk.stopsOf(p, "m1", ::ab)
        assertEquals(listOf("F-M1.len"), line)
        assertEquals("F-M1.len", MeasureWalk.step(line, "F-M1.len", true)); assertEquals("F-M1.len", MeasureWalk.step(line, "F-M1.len", false))
        assertEquals(listOf("F-M2.per"), MeasureWalk.stopsOf(p, "m2", ::ab))
        assertEquals(emptyList(), MeasureWalk.stopsOf(p, "t1", ::ab))
        assertNull(MeasureWalk.step(emptyList(), null, true)); assertNull(MeasureWalk.step(emptyList(), "FD1.w", false))
        // a chain's keys, wrapping from its total to its first segment
        val chain = MeasureWalk.stopsOf(p, "k1", ::ab)
        assertEquals(listOf("FK1.1", "FK1.2", "FK1.t"), chain)
        assertEquals("FK1.1", MeasureWalk.step(chain, "FK1.t", true)); assertEquals("FK1.t", MeasureWalk.step(chain, "FK1.1", false))
        // a mark that is not in the room is never active: it has no keys to step through; nor has a Note, nor an id nothing has
        assertEquals(emptyList(), MeasureWalk.stopsOf(p, assertNotNull(p.markOf("FD2.w")).second.id, ::ab))
        assertEquals(emptyList(), MeasureWalk.stopsOf(p, assertNotNull(p.markOf("FNOTE1.x")).second.id, ::ab))
        assertEquals(emptyList(), MeasureWalk.stopsOf(p, "nothing", ::ab))
    }

    // ------------------------------------------------------------------ a mark with four free corners (port 63 slice Q1)

    @Test fun `a four-corner mark is walked side by side -- bottom, right, top, left, then its type's own`() {
        // the design's example, made up: the front wall 3600 x 2700; a door, and window FW1 with a sloping bottom
        val l = 5000.0; val a = 3600.0; val b = 2700.0
        val p = SurveyPrep.fresh(l, a, b)
        val w = p.put("front", "W", 1, 600.0, 2970.0, 900.0, 2450.0)
        assertTrue(Quad.set(w, listOf(pt(600.0, 1670.0), pt(2970.0, 900.0), pt(2970.0, 2450.0), pt(600.0, 2450.0))))
        p.put("front", "D", 1, 100.0, 500.0, 0.0, 2100.0)
        val seven = listOf("FW1.bottom", "FW1.right", "FW1.top", "FW1.left", "FW1.l", "FW1.s", "FW1.p")
        // marks are ordered as before, by the box's left edge: the door first; inside the window, its sides and then its own
        assertEquals(listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p") + seven, MeasureWalk.stops(p, "front", a, b))
        assertEquals(0 to 11, MeasureWalk.count(p, "front", a, b).let { it.read to it.total })
        for (k in listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p")) p.actual[k] = 900.0
        // the walk reads the sloping bottom FIRST and the left upright LAST of the four
        var at: String? = null; val walked = ArrayList<String>()
        repeat(7) { at = MeasureWalk.next(p, "front", a, b, at); walked += at!!; p.actual[at!!] = 1000.0 }
        assertEquals(seven, walked); assertNull(MeasureWalk.next(p, "front", a, b, at))
        assertEquals(11 to 11, MeasureWalk.count(p, "front", a, b).let { it.read to it.total }); assertTrue(MeasureWalk.count(p, "front", a, b).done)
        assertEquals("FW1.left", MeasureWalk.previous(p, "front", a, b, "FW1.l")); assertEquals("FW1.p", MeasureWalk.previous(p, "front", a, b, null))
        // the picked item's own keys, for 2 Mark's arrows
        assertEquals(seven, MeasureWalk.stopsOf(p, w.id) { SurveyPrep.size(it, l, a, b) })
        // a reading it kept from its time as a rectangle is nobody's stop and in no count -- and is not removed
        p.actual["FW1.w"] = 2370.0; p.actual["FW1.h"] = 1550.0
        assertFalse(MeasureWalk.left(p, "front", a, b, "FW1.w")); assertEquals(11, MeasureWalk.count(p, "front", a, b).total)
        assertEquals(2370.0, p.actual["FW1.w"])
        // put back to a rectangle it asks w and h again, and finds the readings it kept; its side readings wait in turn
        assertTrue(Quad.clear(w))
        assertEquals(listOf("FW1.w", "FW1.h", "FW1.l", "FW1.s", "FW1.p"), MeasureWalk.stopsOf(p, w.id) { SurveyPrep.size(it, l, a, b) })
        assertEquals(9 to 9, MeasureWalk.count(p, "front", a, b).let { it.read to it.total }); assertEquals(1000.0, p.actual["FW1.left"])
        // a shape with no square corner is walked through its diagonal after its four sides
        val x = p.put("front", "X", 1, 1000.0, 1900.0, 300.0, 800.0)
        assertTrue(Quad.set(x, listOf(pt(1000.0, 400.0), pt(1800.0, 300.0), pt(1900.0, 700.0), pt(1200.0, 800.0))))
        assertEquals(listOf("FX1.bottom", "FX1.right", "FX1.top", "FX1.left", "FX1.diag", "FX1.l", "FX1.s"), MeasureWalk.stopsOf(p, x.id) { SurveyPrep.size(it, l, a, b) })
        // a wished four-corner unit is not in the room: it shows its side estimates and takes no reading
        val u = p.put("front", "CRP", 1, 2000.0, 3200.0, 0.0, 2100.0, mapOf(MarkLife.KEY_BORN to MarkLife.WISH, MarkLife.KEY_LIFE to MarkLife.WISH))
        assertTrue(Quad.set(u, listOf(pt(2000.0, 0.0), pt(3200.0, 0.0), pt(3200.0, 1800.0), pt(2000.0, 2100.0))))
        assertEquals(emptyList(), MeasureWalk.stopsOf(p, u.id) { SurveyPrep.size(it, l, a, b) })
        assertTrue(MeasureWalk.stops(p, "front", a, b).none { it.startsWith("FCRP1.") })
        assertEquals(listOf("≈1200", "≈1800", "≈1237", "≈2100"), MarkDims.of("front", u, a, b).take(4).map { MarkDims.value(p.actual, "front", u, it.code, it.est, a, b).text })
    }
}
