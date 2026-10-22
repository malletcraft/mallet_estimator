package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * THE DIMENSION CHAIN (v52 slice 11, the chain halves of items 7 and 14), against ct52 sections K, L, N, T ("palette:
 * exactly 8 slots"), X5 and FX9. The fixture is the prototype's front wall, 3310 x 2705, with FD1 -- a door 905.25 wide
 * from the left corner -- so FK1 tapped at the left corner, FD1's right edge and the right corner reads ≈905 · ≈2405
 * and its total is the laser's 3310 (ct52 K).
 */
class ChainTest {

    private val L = 5458.5; private val A = 3310.0; private val B = 2705.0
    private val tol = 25.0

    private fun pt(x: Double, y: Double) = doubleArrayOf(x, y)

    /** The front wall with FD1 at the left corner. */
    private fun room(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, A, B)
        p.surface("front").marks.add(SurveyPrep.Mark("fd1", "D", "FD1", 0.0, 905.25, 0.0, 2052.7))
        return p
    }

    /** Taps [taps] with the Chain tool on [s] and ends it as the screen does: the new chain, or null when refused. */
    private fun tapChain(p: SurveyPrep.Prep, s: String, id: String, vararg taps: DoubleArray): SurveyPrep.Chain? {
        val ab = SurveyPrep.size(s, L, A, B)
        var d: SurveyChain.Draft? = null
        for (t in taps) d = (SurveyChain.press(d, s, t, ab[0], ab[1], p.surface(s).marks, tol) as SurveyChain.Press.Added).draft
        val f = SurveyChain.finish(d!!) as? SurveyChain.Finish.Done ?: return null
        return p.addChain(s, f.axis, f.nodes, f.snaps, id)
    }

    /** FK1 as ct52 K draws it. */
    private fun fk1(p: SurveyPrep.Prep) = tapChain(p, "front", "k1", pt(10.0, 1200.0), pt(915.0, 1230.0), pt(3300.0, 1300.0))!!

    @Test fun `K is never a mark letter -- the chain's code counter can never meet a mark's`() {
        for ((kind, ts) in SurveyPrep.TYPES) assertTrue(ts.none { it.letter == SurveyChain.LETTER }, kind)
        assertEquals("K", SurveyChain.LETTER)
        assertEquals(0xfb7185, SurveyChain.COLOUR)
    }

    @Test fun `the axis from the first two taps, and every point after snapped onto it (sp js kPlace)`() {
        val p = room()
        val k = fk1(p)
        // ct52 K: "FK1: snapped to the left corner, FD1's right edge and the right corner; segments ≈905 and ≈2405"
        assertEquals("FK1", k.code); assertEquals("x", k.axis)
        assertEquals(listOf(0.0, 905.25, 3310.0), k.nodes.map { it[0] })
        assertTrue(k.nodes.all { it[1] == 1200.0 }, "every point on the first one's line, not where the finger was")
        assertEquals(listOf<String?>("left corner", "FD1 right", "right corner"), k.snaps)
        assertEquals(listOf(905.25, 2404.75), SurveyChain.segs(k))
        // up the face: the second tap further up than across fixes the vertical
        val v = tapChain(p, "front", "k2", pt(1500.0, 15.0), pt(1520.0, 900.0), pt(1490.0, 2690.0))!!
        assertEquals("y", v.axis); assertEquals("FK2", v.code)
        assertEquals(listOf(1500.0, 1500.0, 1500.0), v.nodes.map { it[0] })
        assertEquals(listOf(0.0, 900.0, 2705.0), v.nodes.map { it[1] })
        assertEquals(listOf("floor", null, "ceiling"), v.snaps)
        // the first point snaps both ways, and says so
        val d = SurveyChain.Draft("front")
        assertEquals("left corner · floor", SurveyChain.place(d, pt(12.0, 8.0), 0, A, B, emptyList(), tol).tag)
        // a point is kept on the face
        assertEquals(listOf(3310.0, 2705.0), SurveyChain.place(d, pt(4000.0, 3000.0), 0, A, B, emptyList(), tol).p.toList())
    }

    @Test fun `taps in any order come out in order along the axis, and a chain needs two different points`() {
        val p = room()
        val k = tapChain(p, "front", "k1", pt(2000.0, 600.0), pt(500.0, 610.0), pt(1200.0, 640.0))!!
        assertEquals(listOf(500.0, 1200.0, 2000.0), k.nodes.map { it[0] })
        // one point: refused, said
        val one = SurveyChain.Draft("front")
        SurveyChain.press(one, "front", pt(100.0, 100.0), A, B, emptyList(), tol)
        assertEquals(SurveyChain.NEED_TWO, (SurveyChain.finish(one) as SurveyChain.Finish.Refused).text)
        assertEquals("A chain needs two points or more", SurveyChain.NEED_TWO)
        assertNull((SurveyChain.finish(SurveyChain.Draft("front")) as SurveyChain.Finish.Refused).text, "an empty draft says nothing")
        // two points on top of each other along the axis
        val same = SurveyChain.Draft("front")
        for (t in listOf(pt(1000.0, 600.0), pt(1300.0, 600.0))) SurveyChain.press(same, "front", t, A, B, emptyList(), tol)
        same.nodes[1][0] = 1000.4
        assertEquals("A chain needs two different points", (SurveyChain.finish(same) as SurveyChain.Finish.Refused).text)
        // a tap on another surface while a chain is drawn adds nothing (sp.js kDown)
        val busy = SurveyChain.press(same, "right", pt(10.0, 10.0), L, B, emptyList(), tol)
        assertEquals("Finish the chain on the front wall first — ✓ or a double tap", (busy as SurveyChain.Press.Busy).text)
        assertEquals(2, same.nodes.size)
        // a second finger takes back the point its press added; the draft goes when it is empty
        val c = SurveyChain.Draft("front")
        val i = (SurveyChain.press(c, "front", pt(400.0, 400.0), A, B, emptyList(), tol) as SurveyChain.Press.Added).idx
        assertTrue(SurveyChain.cancel(c, i)); assertTrue(c.nodes.isEmpty())
    }

    @Test fun `segment keys FK1_1 to FK1_n, then FK1_t, and the D2 flow skips a total the laser gives`() {
        val p = room(); val k = fk1(p)
        assertEquals(listOf("FK1.1", "FK1.2", "FK1.t"), p.keysOf(k))
        assertEquals(listOf("segment 1", "segment 2", "total"), SurveyChain.measures(k).map { it.what })
        // ct52 K: "the chain is selected: ... the D2 strip on FK1.1"; "D2 fires into FK1.1 and moves on to FK1.2"
        assertEquals("FK1.1", p.nextOpen(k, null, A, B))
        p.actual["FK1.1"] = 905.0
        assertEquals("FK1.2", p.nextAfter("FK1.1") { SurveyPrep.size(it, L, A, B) })
        p.actual["FK1.2"] = 2405.0
        // edge to edge: the laser gives the total, so nothing is left to read
        assertNull(p.nextAfter("FK1.2") { SurveyPrep.size(it, L, A, B) })
        assertEquals(listOf("FK1.1", "FK1.2"), SurveyChain.openKeys(k, A, B))
        // short of the edge, the total is asked for after the segments
        val s = tapChain(p, "front", "k2", pt(500.0, 600.0), pt(1500.0, 600.0))!!
        assertEquals(listOf("FK2.1", "FK2.t"), SurveyChain.openKeys(s, A, B))
        p.actual["FK2.1"] = 1000.0
        assertEquals("FK2.t", p.nextAfter("FK2.1") { SurveyPrep.size(it, L, A, B) })
        // the strip's line, and the owner of each key
        val (sf, m) = assertNotNull(p.readingOf("FK1.2") { SurveyPrep.size(it, L, A, B) })
        assertEquals("front", sf); assertEquals("segment 2", m.what); assertEquals(2404.75, m.est!!, 1e-9)
        assertEquals("k1", p.ownerOf("FK1.t")!!.id); assertEquals("chain", p.ownerOf("FK1.1")!!.kind)
        assertNull(p.ownerOf("FK9.1"))
    }

    @Test fun `the total -- the laser's edge to edge, derived from the segments short of it, and the one unknown segment derived`() {
        val p = room(); val k = fk1(p)
        val v0 = SurveyChain.vals(k, p.actual, A, B)
        // ct52 K: 'total "laser 3310"', the row "FK1 ≈905 · ≈2405 · Σ 3310 laser"
        assertEquals(SurveyChain.LASER, v0.src); assertEquals(3310.0, v0.total)
        assertEquals("≈905 · ≈2405 · Σ 3310 laser", SurveyChain.rowValue(v0))
        assertEquals("FK1 Σ 3310 laser", SurveyChain.totalTag(k.code, v0))
        assertTrue(SurveyChain.totalReadOnly(v0), "the laser's total is not typed over")
        // ct52 K: "typing 900 into FK1.1 derives FK1.2 = 2410 (dashed, derived); the total reads laser 3310"
        p.actual["FK1.1"] = 900.0
        val v1 = SurveyChain.vals(k, p.actual, A, B)
        assertEquals(1, v1.der); assertEquals(2410.0, v1.value[1]!!, 1e-9)
        assertEquals("2410 d" to SurveyChain.DERIVED_COLOUR, SurveyChain.segFigure(v1, 1))
        assertEquals("900" to SurveyChain.MEASURED_COLOUR, SurveyChain.segFigure(v1, 0))
        assertEquals("900 · 2410 d · Σ 3310 laser", SurveyChain.rowValue(v1))
        // both read: checked against the laser (0 off: rose); 60 off is amber
        p.actual["FK1.2"] = 2410.0
        assertEquals(0.0, SurveyChain.vals(k, p.actual, A, B).check!!, 1e-9)
        assertEquals(SurveyChain.COLOUR, SurveyChain.totalColour(SurveyChain.vals(k, p.actual, A, B)))
        p.actual["FK1.2"] = 2350.0
        assertEquals(-60.0, SurveyChain.vals(k, p.actual, A, B).check!!, 1e-9)
        assertEquals(SurveyChain.CHECK_COLOUR, SurveyChain.totalColour(SurveyChain.vals(k, p.actual, A, B)))
        // ct52 K: "a chain short of the edge: its total is derived from the measured segments (900 + 2410 = 3310)"
        p.actual["FK1.2"] = 2410.0
        k.nodes[2][0] = 3000.0
        val v2 = SurveyChain.vals(k, p.actual, A, B)
        assertNull(v2.laser); assertEquals(SurveyChain.DERIVED, v2.src); assertEquals(3310.0, v2.total)
        assertEquals("900 · 2410 · Σ 3310 d", SurveyChain.rowValue(v2))
        assertFalse(SurveyChain.totalReadOnly(v2))
        // short of the edge with nothing read: the photo's sum, unknown
        val v3 = SurveyChain.vals(k, emptyMap(), A, B)
        assertNull(v3.total); assertEquals("≈905 · ≈2095 · Σ ≈3000", SurveyChain.rowValue(v3))
        // a measured total wins over the laser, and derives the one unknown segment
        k.nodes[2][0] = 3310.0
        val v4 = SurveyChain.vals(k, mapOf("FK1.t" to 3300.0, "FK1.1" to 900.0), A, B)
        assertEquals(SurveyChain.MEASURED, v4.src); assertEquals(2400.0, v4.value[1]!!, 1e-9)
    }

    @Test fun `a point snaps onto FD1, and a finished chain's point drags along its axis between its neighbours`() {
        val p = room(); val k = fk1(p)
        val marks = p.surface("front").marks
        // ct52 K: "a chain's point drags along the chain (FD1 right → ≈1500): the segments follow, the height does not"
        assertTrue(SurveyChain.nodeMove("front", k, 1, pt(1500.0, 1700.0), A, B, marks, tol))
        assertEquals(1500.0, k.nodes[1][0]); assertEquals(1200.0, k.nodes[1][1])
        assertEquals(3310.0, SurveyChain.segs(k).sum(), 1e-9); assertNull(k.snaps[1])
        // back near FD1's right edge: onto it
        SurveyChain.nodeMove("front", k, 1, pt(920.0, 1200.0), A, B, marks, tol)
        assertEquals(905.25, k.nodes[1][0]); assertEquals("FD1 right", k.snaps[1])
        // never past a neighbour: 1 mm off it
        SurveyChain.nodeMove("front", k, 1, pt(-200.0, 1200.0), A, B, marks, tol)
        assertEquals(1.0, k.nodes[1][0])
        SurveyChain.nodeMove("front", k, 1, pt(9000.0, 1200.0), A, B, marks, tol)
        assertEquals(3309.0, k.nodes[1][0])
        // the end point stays on the face
        SurveyChain.nodeMove("front", k, 2, pt(5000.0, 1200.0), A, B, marks, tol)
        assertEquals(3310.0, k.nodes[2][0])
        // locked: nothing moves, and the press says so
        SurveyPrepEdit.setLocked(p, "k1", true)
        assertFalse(SurveyChain.nodeMove("front", k, 1, pt(2000.0, 1200.0), A, B, marks, tol))
        assertEquals(3309.0, k.nodes[1][0])
        assertEquals("FK1 is locked — unlock to move it", SurveyChain.lockedNode(k.code))
    }

    @Test fun `deleteMark removes FK1 and FK1's readings only`() {
        val p = room(); val k = fk1(p)
        for ((key, v) in listOf("FK1.1" to 900.0, "FK1.2" to 2410.0, "FK1.t" to 3310.0, "FK10.1" to 5.0, "FD1.w" to 905.0)) p.actual[key] = v
        assertTrue(p.deleteMark(k.id))
        assertTrue(p.surface("front").chains.isEmpty())
        assertEquals(setOf("FK10.1", "FD1.w"), p.actual.keys)
        // and FK1 is not handed out again: the web may still hold readings under it
        assertEquals("FK2", fk1(p).code)
        // a locked chain is not deleted
        val q = room(); val c = fk1(q); SurveyPrepEdit.setLocked(q, c.id, true); q.actual["FK1.1"] = 900.0
        assertFalse(q.deleteMark(c.id)); assertEquals(1, q.surface("front").chains.size); assertEquals(900.0, q.actual["FK1.1"])
    }

    @Test fun `undo keeps FK1_2 -- a reading taken since stays while its chain is there`() {
        val p = room(); fk1(p)
        val before = p.copy()                       // the screen's snapshot
        p.actual["FK1.2"] = 2410.0                  // a reading is not an undo step
        SurveyChain.nodeMove("front", p.surface("front").chains[0], 1, pt(1500.0, 1200.0), A, B, emptyList(), tol)
        val back = SurveyPrepEdit.undoTo(p, before)
        assertEquals(905.25, back.surface("front").chains[0].nodes[1][0], "the geometry goes back")
        assertEquals(2410.0, back.actual["FK1.2"], "the reading stays")
        // ↶ after a delete brings the chain back WITH its readings
        val del = back.copy(); assertTrue(back.deleteMark("k1")); assertNull(back.actual["FK1.2"])
        val again = SurveyPrepEdit.undoTo(back, del)
        assertEquals(1, again.surface("front").chains.size); assertEquals(2410.0, again.actual["FK1.2"])
        // ↶ of the chain's own making drops its readings -- they have no owner -- and FK1 is not handed out again
        val q = room(); val none = q.copy(); fk1(q); q.actual["FK1.1"] = 900.0
        val gone = SurveyPrepEdit.undoTo(q, none)
        assertTrue(gone.surface("front").chains.isEmpty()); assertNull(gone.actual["FK1.1"])
        assertEquals("FK2", fk1(gone).code)
    }

    @Test fun `clear takes an unlocked chain and its readings, and keeps a locked one with its readings`() {
        val p = room()
        val k1 = fk1(p)
        val k2 = tapChain(p, "front", "k2", pt(500.0, 600.0), pt(1500.0, 600.0))!!
        for ((key, v) in listOf("FK1.1" to 900.0, "FK1.t" to 3310.0, "FK2.1" to 1000.0, "FD1.w" to 905.0)) p.actual[key] = v
        SurveyPrepEdit.setLocked(p, k1.id, true)
        assertEquals(2, SurveyPrepEdit.clearCount(p, "surf", "front"), "FD1 and FK2")
        assertEquals(1, SurveyPrepEdit.lockedCount(p, "surf", "front"))
        val r = SurveyPrepEdit.clearItems(p, "surf", "front")
        assertEquals(2, r.n); assertEquals(1, r.kept); assertEquals(setOf("FD1", k2.code), r.codes.toSet())
        assertEquals(listOf("FK1"), p.surface("front").chains.map { it.code })
        assertEquals(setOf("FK1.1", "FK1.t"), p.actual.keys)
        assertEquals("Cleared 2 items, 1 locked kept", SurveyPrepEdit.clearedSnack(r.n, r.kept))
        // FK2 is not handed out again after a clear either
        SurveyPrepEdit.setLocked(p, k1.id, false)
        assertEquals("FK3", tapChain(p, "front", "k3", pt(500.0, 600.0), pt(1500.0, 600.0))!!.code)
    }

    @Test fun `Start over keeps a locked chain with its readings and the counter, and drops the rest`() {
        val p = room(); val k1 = fk1(p); tapChain(p, "front", "k2", pt(500.0, 600.0), pt(1500.0, 600.0))
        p.actual["FK1.1"] = 900.0; p.actual["FK2.1"] = 1000.0
        SurveyPrepEdit.setLocked(p, k1.id, true)
        val r = SurveyPrepEdit.startOver(p, L, A, B)
        assertEquals(1, r.kept)
        assertEquals(listOf("FK1"), r.prep.surface("front").chains.map { it.code })
        assertEquals(mapOf("FK1.1" to 900.0), r.prep.actual.toMap())
        assertEquals("FK3", r.prep.nextCode("front", SurveyChain.LETTER), "FK2's number is not handed out again")
    }

    @Test fun `Duplicate -- 100 mm across the axis, back the other way at an edge, the next K number, no readings, unlocked`() {
        val p = room(); val k1 = fk1(p)
        tapChain(p, "front", "k2", pt(500.0, 600.0), pt(1500.0, 600.0))
        p.actual["FK1.1"] = 900.0
        SurveyPrepEdit.setLocked(p, k1.id, true)
        val d = assertNotNull(SurveyPrepEdit.duplicateChain(p, k1.id, A, B, "d1"))
        assertEquals("FK3", d.code); assertEquals("x", d.axis)
        assertEquals(listOf(0.0, 905.25, 3310.0), d.nodes.map { it[0] })
        assertTrue(d.nodes.all { it[1] == 1300.0 }, "100 mm up")
        assertFalse(SurveyPrep.isLocked(d)); assertNull(p.actual["FK3.1"])
        assertEquals(listOf<String?>("left corner", "FD1 right", "right corner"), d.snaps)
        // deleted, the next duplicate is FK4: a code is never reused
        assertTrue(p.deleteMark("d1"))
        assertEquals("FK4", SurveyPrepEdit.duplicateChain(p, k1.id, A, B, "d2")!!.code)
        // at the ceiling: back down
        val top = tapChain(p, "front", "k5", pt(500.0, 2650.0), pt(1500.0, 2650.0))!!
        assertTrue(SurveyPrepEdit.duplicateChain(p, top.id, A, B, "d3")!!.nodes.all { it[1] == 2550.0 })
        // up the face: across is to the right
        val v = tapChain(p, "front", "k6", pt(1500.0, 15.0), pt(1520.0, 900.0))!!
        assertTrue(SurveyPrepEdit.duplicateChain(p, v.id, A, B, "d4")!!.nodes.all { it[0] == 1600.0 })
        assertNull(SurveyPrepEdit.duplicateChain(p, "none", A, B, "x"))
    }

    @Test fun `Lock all chains, the menu, and every label (ct52 N, L, FX9)`() {
        val p = room(); val k1 = fk1(p); tapChain(p, "front", "k2", pt(500.0, 600.0), pt(1500.0, 600.0))
        assertEquals("Delete|Duplicate|Lock|Lock all chains|Cancel", SurveyPrepEdit.menuItemsOf(p, k1.id).joinToString("|") { it.second })
        assertEquals("FK1 chain", SurveyPrepEdit.menuHeaderOf(p, k1.id))
        assertEquals("FK1 chain", SurveyPrepEdit.labelOf(p, k1.id))
        assertEquals("Delete FK1 chain", SurveyPrepEdit.deleteLabel(p, k1.id))
        assertEquals("FK1 deleted", SurveyPrepEdit.deleted(k1.code))
        val (s, g) = assertNotNull(SurveyPrepEdit.lockGroupOf(p, k1.id))
        assertEquals("front", s); assertEquals("chains", g.name); assertEquals(SurveyPrepEdit.KIND_CHAIN, g.kind); assertNull(g.t)
        // "Lock all chains" locks the chains -- nothing else
        assertEquals(2, SurveyPrepEdit.lockAll(p, "front", g.t, true, g.kind))
        assertFalse(SurveyPrep.isLocked(p.surface("front").marks[0]))
        assertEquals("🔒 FK1 chain", SurveyPrepEdit.menuHeaderOf(p, k1.id))
        assertEquals("Delete|Duplicate|Unlock|Unlock all chains|Cancel", SurveyPrepEdit.menuItemsOf(p, k1.id).joinToString("|") { it.second })
        // Lock all marks (every item, kind null) takes the chains too; a mark type's or Lines' do not
        assertEquals(0, SurveyPrepEdit.lockAll(p, "front", null, true, SurveyPrepEdit.KIND_LINE))
        assertEquals(2, SurveyPrepEdit.lockAll(p, "front", null, false))
        assertEquals(3, SurveyPrepEdit.lockAll(p, "front", null, true), "FD1 and both chains")
        assertTrue(p.itemsOf("front").all { it.locked })
        // toggled one at a time
        assertEquals(false, SurveyPrepEdit.setLocked(p, k1.id, null))
        // the List names it: "Chain · horizontal · 2 parts", and on the List tab the face after it
        assertEquals("Chain · horizontal · 2 parts", SurveyChain.listName("front", k1, false))
        assertEquals("Chain · horizontal · 2 parts · Front wall", SurveyChain.listName("front", k1, true))
    }

    @Test fun `a chain round-trips SurveyPrepFile -- points, snaps, lock, readings and its code counter`() {
        val p = room(); val k1 = fk1(p); val k2 = tapChain(p, "front", "k2", pt(500.0, 600.0), pt(1500.0, 600.0))!!
        p.actual["FK1.1"] = 900.0; p.actual["FK1.t"] = 3310.0
        SurveyPrepEdit.setLocked(p, k1.id, true)
        assertTrue(p.deleteMark(k2.id))
        val o = SurveyPrepFile.encode(p, L, A, B)
        @Suppress("UNCHECKED_CAST") val front = (o["w"] as Map<String, Map<String, Any?>>).getValue("front")
        @Suppress("UNCHECKED_CAST") val c0 = (front["chains"] as List<Map<String, Any?>>).single()
        assertEquals(listOf("id", "code", "axis", "locked", "nodes", "snaps"), c0.keys.toList())
        assertEquals(listOf("left corner", "FD1 right", "right corner"), c0["snaps"])
        // a surface with no chain and no `chains` key writes none
        @Suppress("UNCHECKED_CAST") val right = (o["w"] as Map<String, Map<String, Any?>>).getValue("right")
        assertFalse("chains" in right)
        val d = assertNotNull(SurveyPrepFile.decode(o))
        assertTrue(d.problems.isEmpty(), d.problems.toString())
        val k = d.prep.surface("front").chains.single()
        assertEquals("FK1", k.code); assertTrue(SurveyPrep.isLocked(k))
        assertEquals(k1.nodes.map { it.toList() }, k.nodes.map { it.toList() })
        assertEquals(mapOf("FK1.1" to 900.0, "FK1.t" to 3310.0), d.prep.actual.toMap())
        assertEquals("FK3", d.prep.nextCode("front", SurveyChain.LETTER), "FK2 was deleted, and is not handed out again after an open")
        // and back once more, unchanged
        assertEquals(o.toString(), SurveyPrepFile.encode(d.prep, L, A, B).toString())
        // a malformed chain costs only itself
        val bad = LinkedHashMap(o)
        @Suppress("UNCHECKED_CAST") val w = LinkedHashMap(o["w"] as Map<String, Any?>)
        w["front"] = LinkedHashMap(front).apply { put("chains", listOf(mapOf("id" to "x", "code" to "FK7", "nodes" to listOf(listOf(1.0, 2.0)))) + (front["chains"] as List<*>)) }
        bad["w"] = w
        val d2 = assertNotNull(SurveyPrepFile.decode(bad))
        assertEquals(listOf("FK1"), d2.prep.surface("front").chains.map { it.code })
        assertTrue(d2.problems.single().contains("chain FK7"), d2.problems.toString())
    }

    @Test fun `X5 -- a chain point on the picked door's left tab, the tab resizes the door, and unpicked the point is grabbed`() {
        // ct52 X5: "a chain point on the picked door's left tab: the drag resizes the door, the chain point stays"
        val dp = 2.0; val tol = 16 * dp
        val door = SurveyPrepTouch.Drawn("hd", 200.0, 400.0, 100.0, 500.0)
        val nodes = listOf(SurveyPrepTouch.Nodes("fk9", listOf(doubleArrayOf(200.0, 300.0), doubleArrayOf(500.0, 300.0))))
        val h = SurveyPrepTouch.hit(listOf(door), "hd", emptyList(), 201.0, 300.0, tol, dp, nodes, SurveyChain.NODE_HIT_DP * dp)
        assertIs<SurveyPrepTouch.Hit.Mark>(h); assertEquals("l", h.part)
        // not picked: the point under the finger is the chain's, before the door's body
        val n = SurveyPrepTouch.hit(listOf(door), null, emptyList(), 201.0, 300.0, tol, dp, nodes, SurveyChain.NODE_HIT_DP * dp)
        assertIs<SurveyPrepTouch.Hit.Node>(n); assertEquals("fk9", n.id); assertEquals(0, n.i)
        // ... and before a tag under the same finger
        val tag = SurveyPrepTouch.Tag("HD.w", SurveyPrepTouch.Box(180.0, 280.0, 260.0, 320.0))
        assertIs<SurveyPrepTouch.Hit.Node>(SurveyPrepTouch.hit(listOf(door), null, listOf(tag), 201.0, 300.0, tol, dp, nodes, 32.0))
        // 16 dp out of reach: the door's body as before
        assertIs<SurveyPrepTouch.Hit.Mark>(SurveyPrepTouch.hit(listOf(door), null, emptyList(), 300.0, 200.0, tol, dp, nodes, 32.0))
        // the nearest point wins; with Select, a press near the line picks the chain
        assertEquals("fk9" to 1, SurveyChain.nodeAt(listOf("fk9" to nodes[0].pts), 490.0, 305.0, 32.0))
        assertEquals("fk9", SurveyChain.pick(listOf("fk9" to nodes[0].pts), 350.0, 320.0, SurveyChain.PICK_DP * dp))
        assertNull(SurveyChain.pick(listOf("fk9" to nodes[0].pts), 350.0, 330.0, SurveyChain.PICK_DP * dp))
    }

    @Test fun `the chip, its tick, and the guide while a chain is drawn (item 7)`() {
        assertEquals("Chain · tap points along the surface, double-tap or ✓ to end", SurveyChain.chipText(0))
        assertEquals("Chain · 1 point · tap points along the surface, double-tap or ✓ to end", SurveyChain.chipText(1))
        assertEquals("Chain · 3 points · tap points along the surface, double-tap or ✓ to end", SurveyChain.chipText(3))
        assertFalse(SurveyChain.showDone(1)); assertTrue(SurveyChain.showDone(2))
        assertEquals("End the chain", SurveyChain.DONE_LABEL)
        val d = SurveyChain.Draft("front")
        assertNull(SurveyChain.guide(d)); assertNull(SurveyGuide.now(null, null, null, null, SurveyPrepPalette.CHAIN, d))
        SurveyChain.press(d, "front", pt(400.0, 1200.0), A, B, emptyList(), tol)
        val g = assertNotNull(SurveyGuide.now(null, null, null, null, SurveyPrepPalette.CHAIN, d))
        assertEquals(SurveyPrepPalette.CHAIN, g.tool); assertEquals(0xfb7185, SurveyGuide.colour(g.tool)); assertNull(g.snap)
        assertEquals(listOf(400.0, 1200.0), g.o.toList())
        SurveyChain.press(d, "front", pt(1400.0, 1250.0), A, B, emptyList(), tol)
        assertEquals("h", SurveyChain.guide(d)!!.snap, "bold along the face once the axis is fixed")
        // the second point, dragged, re-decides the axis as it goes (sp.js kPlace on every move)
        SurveyChain.move(d, 1, pt(450.0, 2200.0), A, B, emptyList(), tol)
        assertEquals("y", d.axis); assertEquals(listOf(400.0, 2200.0), d.nodes[1].toList())
        // a Tape's drag still wins over a chain draft (sp.js guideNow's order)
        val t = SurveyMeas.Draft("front", SurveyMeas.TAPE, pt(0.0, 0.0), A, B)
        assertEquals(SurveyPrepPalette.TAPE, SurveyGuide.now(t, null, null, null, SurveyPrepPalette.TAPE, d)!!.tool)
        // the Chain is not a press on a selected Line's end (its taps are its points)
        val line = SurveyPrep.Meas("m", SurveyMeas.LINE, "F-M1", mutableListOf(pt(0.0, 0.0), pt(100.0, 0.0)))
        assertNull(SurveyGuide.endPress(true, SurveyPrepPalette.CHAIN, line, false, listOf(pt(10.0, 10.0), pt(90.0, 10.0)), 10.0, 10.0, 28.0))
    }
}
