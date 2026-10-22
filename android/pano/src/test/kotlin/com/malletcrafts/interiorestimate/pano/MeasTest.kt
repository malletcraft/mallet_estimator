package com.malletcrafts.interiorestimate.pano

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * LINE AND TAPE (v52 slice 9; items 5, 6 and item 7's snap, live chip text and off-surface refusal), against ct52
 * sections T and T2, FX9 and the fixtures: the 800 x 600 mm diagonal reads 1000; a tape 1° off level and a Line 2.3°
 * off level snap along the face, one 1.7° off plumb snaps up it; codes are counted per kind; a 19 mm tape is not
 * plotted. The synthetic room: L 3319, W 2678, H 2677 (the front wall 2678 x 2677).
 */
class MeasTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0

    private fun pt(x: Double, y: Double) = doubleArrayOf(x, y)

    /** A point [len] mm from [o] at [deg] degrees above the face's horizontal. */
    private fun at(o: DoubleArray, len: Double, deg: Double) =
        pt(o[0] + len * cos(deg * PI / 180), o[1] + len * sin(deg * PI / 180))

    @Test fun `the 800 x 600 diagonal reads 1000, and an area its shoelace and perimeter`() {
        // ct52 T: "a 800 × 600 mm diagonal on the squared face measures 1000 mm"
        assertEquals(1000.0, SurveyMeas.plen(listOf(pt(500.0, 1600.0), pt(1300.0, 2200.0))), 1e-9)
        val sq = listOf(pt(1800.0, 300.0), pt(2400.0, 300.0), pt(2400.0, 1000.0), pt(1800.0, 1000.0))
        assertEquals(600.0 * 700.0, SurveyMeas.parea(sq), 1e-6)
        assertEquals(2 * (600.0 + 700.0), SurveyMeas.pperim(sq), 1e-9)
        assertEquals("0.42 m²", SurveyMeas.m2txt(SurveyMeas.parea(sq)))
        // either winding, the same area; a triangle half its box
        assertEquals(600.0 * 700.0, SurveyMeas.parea(sq.reversed()), 1e-6)
        assertEquals(1.0e6, SurveyMeas.parea(listOf(pt(0.0, 0.0), pt(2000.0, 0.0), pt(0.0, 1000.0))), 1e-6)
        assertEquals("1.86 m²", SurveyMeas.m2txt(1.86e6))
    }

    @Test fun `the 5 degree snap -- 1, 1·7 and 2·3 degrees snap, 6 does not, under 1 mm nothing`() {
        val o = pt(0.0, 1200.0)
        // a tape pulled edge to edge 1° off level: onto the horizontal, the far end level with the start
        val a = SurveyMeas.axisSnap(o, at(o, 3310.0, 1.0))
        assertEquals("h", a.snap); assertEquals(o[1], a.p[1])
        // 1.7° off plumb, floor to ceiling: up the face, plumb
        val o2 = pt(2600.0, 0.0)
        val v = SurveyMeas.axisSnap(o2, at(o2, 2705.0, 90 - 1.7))
        assertEquals("v", v.snap); assertEquals(o2[0], v.p[0])
        // a Line 2.3° off level (ct52: 500,600 → 1500,640)
        val l = SurveyMeas.axisSnap(pt(500.0, 600.0), pt(1500.0, 640.0))
        assertEquals("h", l.snap); assertEquals(listOf(1500.0, 600.0), l.p.toList())
        // and both ways round: right to left, downwards
        assertEquals("h", SurveyMeas.axisSnap(pt(1500.0, 640.0), pt(500.0, 600.0)).snap)
        assertEquals("v", SurveyMeas.axisSnap(o2, pt(2600.0 - 50.0, -2000.0)).snap)
        // exactly 5° snaps; 6° is a diagonal and keeps its point
        val o3 = pt(0.0, 0.0)
        assertEquals("h", SurveyMeas.axisSnap(o3, pt(1000.0, 1000.0 * tan(5 * PI / 180) - 1e-9)).snap)
        val d = SurveyMeas.axisSnap(o3, at(o3, 1000.0, 6.0))
        assertEquals("d", d.snap); assertEquals(at(o3, 1000.0, 6.0).toList(), d.p.toList())
        assertEquals("d", SurveyMeas.axisSnap(o3, at(o3, 1000.0, 84.0)).snap)
        // under 1 mm: no direction yet
        assertNull(SurveyMeas.axisSnap(o3, pt(0.6, 0.6)).snap)
    }

    @Test fun `a drag snaps, says where it runs, is refused off the face and plotted only from 20 mm`() {
        val d = SurveyMeas.Draft("front", SurveyMeas.TAPE, pt(0.0, 1200.0), W, H)
        d.move(at(pt(0.0, 1200.0), 2678.0, 1.0))
        assertEquals("h", d.snap); assertFalse(d.off)
        assertTrue(SurveyMeas.dirText(d).startsWith("↔ along the face · "), SurveyMeas.dirText(d))
        assertEquals(SurveyMeas.End.PLOTTED, SurveyMeas.end(d))
        // past the left corner into the next wall (ct52: to x = -220): off, the chip says so, refused on release
        val off = SurveyMeas.Draft("front", SurveyMeas.TAPE, pt(1000.0, 1500.0), W, H)
        off.move(pt(-220.0, 1500.0))
        assertTrue(off.off)
        assertEquals("Off this surface — nothing will be plotted", SurveyMeas.dirText(off))
        assertEquals(SurveyMeas.End.REFUSED, SurveyMeas.end(off))
        assertEquals("Only things on this surface measure correctly.", SurveyMeas.OFF_REFUSAL)
        // within 25 mm past an edge still counts as on (sp.js onFace), as a finger on the boundary line is
        val edge = SurveyMeas.Draft("front", SurveyMeas.LINE, pt(1000.0, 1500.0), W, H)
        edge.move(pt(-20.0, 1500.0)); assertFalse(edge.off)
        // a Line past the ceiling line (ct52: 1200,2500 → 1210,2835): refused the same way
        val up = SurveyMeas.Draft("front", SurveyMeas.LINE, pt(1200.0, 2500.0), W, H)
        up.move(pt(1210.0, 2835.0)); assertTrue(up.off); assertEquals("v", up.snap)
        // a 19 mm tape is not plotted; 20 mm is
        val short = SurveyMeas.Draft("front", SurveyMeas.TAPE, pt(1000.0, 1000.0), W, H)
        short.move(pt(1019.0, 1000.0))
        assertEquals(SurveyMeas.End.SHORT, SurveyMeas.end(short))
        assertFalse(SurveyMeas.plotted(19.0)); assertTrue(SurveyMeas.plotted(20.0))
        short.move(pt(1020.0, 1000.0)); assertEquals(SurveyMeas.End.PLOTTED, SurveyMeas.end(short))
    }

    @Test fun `dirText on a wall, the floor and the ceiling`() {
        assertEquals("↔ along the face · 1234 mm", SurveyMeas.dirText("front", 1234.4, "h", false))
        assertEquals("↕ up the face · 2705 mm", SurveyMeas.dirText("left", 2705.0, "v", false))
        assertEquals("diagonal on the face · 1000 mm", SurveyMeas.dirText("back", 1000.0, "d", false))
        assertEquals("↔ left–right on the floor · 900 mm", SurveyMeas.dirText("floor", 900.0, "h", false))
        assertEquals("↕ front–back on the floor · 900 mm", SurveyMeas.dirText("floor", 900.0, "v", false))
        assertEquals("↔ left–right on the ceiling · 900 mm", SurveyMeas.dirText("ceil", 900.0, "h", false))
        assertEquals("↕ front–back on the ceiling · 900 mm", SurveyMeas.dirText("ceil", 900.0, "v", false))
        assertEquals("diagonal on the face · 900 mm", SurveyMeas.dirText("ceil", 900.0, "d", false))
        assertEquals("Off this surface — nothing will be plotted", SurveyMeas.dirText("floor", 900.0, "h", true))
        // the en dash, as sp.js has it
        assertTrue(SurveyMeas.dirText("floor", 1.0, "h", false).contains("left–right"))
    }

    @Test fun `the chips -- Tape, Line, an area started, a drag in progress`() {
        assertEquals("Tape · drag along the face · the photo gives its length", SurveyMeas.chipText("T", null, 0))
        assertEquals("Line · drag = a marked length · tap corners = an area", SurveyMeas.chipText("M", null, 0))
        assertEquals("Line · area · 1 corner · double-tap to end", SurveyMeas.chipText("M", null, 1))
        assertEquals("Line · area · 3 corners · double-tap to end", SurveyMeas.chipText("M", null, 3))
        val d = SurveyMeas.Draft("floor", SurveyMeas.LINE, pt(500.0, 500.0), W, L).also { it.move(pt(1400.0, 520.0)) }
        assertEquals("↔ left–right on the floor · 900 mm", SurveyMeas.chipText("M", d, 2))
        assertNull(SurveyMeas.chipText("W", null, 0)); assertNull(SurveyMeas.chipText("sel", null, 0))
        // nothing calls a tool "Measure" (item 6): the tools are Tape and Line
        for (t in listOf(SurveyMeas.TAPE_CHIP, SurveyMeas.LINE_CHIP, SurveyMeas.areaChip(3), SurveyMeas.TAPE_NOTE, SurveyMeas.CAUTION))
            assertFalse(Regex("\\bMeasure\\b").containsMatchIn(t), t)
        assertEquals("Tape", SurveyPrepPalette.toolName("T")); assertEquals("Line", SurveyPrepPalette.toolName("M"))
    }

    @Test fun `the tape's graduations -- every 100 mm, figures every 500 mm at least 150 mm from either end`() {
        val t = SurveyMeas.tapeTicks(3310.0)
        assertEquals((1..33).map { it * 100.0 }, t.map { it.at })
        assertEquals(listOf(500.0, 1000.0, 1500.0, 2000.0, 2500.0, 3000.0), t.filter { it.major }.map { it.at })
        assertEquals(listOf(500.0, 1000.0, 1500.0, 2000.0, 2500.0, 3000.0), t.filter { it.figure }.map { it.at })
        // 1600 long: the 1500 figure would sit 100 mm from the end, under the tag -- its tick stays, its figure goes
        val u = SurveyMeas.tapeTicks(1600.0)
        assertEquals(listOf(500.0, 1000.0, 1500.0), u.filter { it.major }.map { it.at })
        assertEquals(listOf(500.0, 1000.0), u.filter { it.figure }.map { it.at })
        // a tick never sits on the far end itself, and a short tape has none
        assertEquals(listOf(100.0, 200.0, 300.0, 400.0), SurveyMeas.tapeTicks(500.0).map { it.at })
        assertTrue(SurveyMeas.tapeTicks(90.0).isEmpty())
        assertFalse(SurveyMeas.tapeTicks(500.0).any { it.figure })
    }

    @Test fun `codes are counted per kind, never handed out twice, and survive the file`() {
        val p = SurveyPrep.fresh(L, W, H)
        val t1 = p.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 1200.0), pt(2678.0, 1200.0)), "t1")
        val t2 = p.addMeas("front", SurveyMeas.TAPE, listOf(pt(2600.0, 0.0), pt(2600.0, 2677.0)), "t2")
        val m1 = p.addMeas("front", SurveyMeas.LINE, listOf(pt(500.0, 600.0), pt(1500.0, 600.0)), "m1")
        val m2 = p.addMeas("front", SurveyMeas.AREA, listOf(pt(1800.0, 300.0), pt(2400.0, 300.0), pt(2400.0, 1000.0)), "m2")
        val g1 = p.addMeas("floor", SurveyMeas.TAPE, listOf(pt(0.0, 100.0), pt(900.0, 100.0)), "g1")
        // ct52 T2: the Line numbering is apart from the tapes'; G for the floor
        assertEquals(listOf("F-T1", "F-T2", "F-M1", "F-M2", "G-T1"), listOf(t1, t2, m1, m2, g1).map { it.code })
        // a mark's codes are untouched by them
        assertEquals("FW1", p.addMarkAt("front", "W", 1300.0, 1500.0, W, H, "w1").code)
        // kept to 0.1 mm, unlocked
        assertFalse(SurveyPrep.isLocked(t1)); assertEquals(false, t1.extra[SurveyPrep.LOCKED])
        // a deleted code is never handed out again
        assertTrue(p.deleteMark("t2"))
        assertEquals("F-T3", p.addMeas("front", SurveyMeas.TAPE, listOf(pt(10.0, 10.0), pt(500.0, 10.0)), "t3").code)
        // the file round trip keeps every field and the counter
        val o = SurveyPrepFile.encode(p, L, W, H)
        val back = assertNotNull(SurveyPrepFile.decode(o))
        assertTrue(back.problems.isEmpty(), back.problems.toString())
        val f = back.prep.surface("front").meas
        assertEquals(listOf("F-T1", "F-M1", "F-M2", "F-T3"), f.map { it.code })
        assertEquals(listOf("tape", "line", "area", "tape"), f.map { it.kind })
        assertEquals(listOf(listOf(1800.0, 300.0), listOf(2400.0, 300.0), listOf(2400.0, 1000.0)), f[2].pts.map { it.toList() })
        assertEquals(listOf("t1", "m1", "m2", "t3"), f.map { it.id })
        assertEquals("F-T4", back.prep.addMeas("front", SurveyMeas.TAPE, listOf(pt(10.0, 10.0), pt(500.0, 10.0)), "t4").code)
        assertEquals("F-M3", back.prep.addMeas("front", SurveyMeas.LINE, listOf(pt(10.0, 10.0), pt(500.0, 10.0)), "m3").code)
        // and encodes again to the same content
        assertEquals(o.toString(), SurveyPrepFile.encode(SurveyPrepFile.decode(o)!!.prep, L, W, H).toString())
    }

    @Test fun `an older prep without meas re-encodes with no meas key, and a bad one costs only itself`() {
        val p = SurveyPrep.fresh(L, W, H)
        @Suppress("UNCHECKED_CAST") val f0 = ((SurveyPrepFile.encode(p, L, W, H)["w"] as Map<String, Map<String, Any?>>).getValue("front"))
        assertFalse("meas" in f0.keys)
        val o = SurveyPrepFile.encode(p, L, W, H)
        @Suppress("UNCHECKED_CAST") val w = (o["w"] as Map<String, Any?>).toMutableMap()
        @Suppress("UNCHECKED_CAST") val front = (w["front"] as Map<String, Any?>).toMutableMap()
        front["meas"] = listOf(
            mapOf("id" to "a", "code" to "F-T1", "kind" to "tape", "locked" to true, "pts" to listOf(listOf(0, 0), listOf(500, 0))),
            mapOf("id" to "b", "code" to "F-M1", "kind" to "line", "pts" to listOf(listOf(0, 0))),            // one point: unreadable
            mapOf("id" to "c", "code" to "F-X1", "kind" to "chain?", "pts" to listOf(listOf(1, 2)), "z" to 9))  // a later kind: kept
        w["front"] = front
        val d = assertNotNull(SurveyPrepFile.decode(o.toMutableMap().apply { put("w", w) }))
        assertEquals(1, d.problems.size); assertTrue(d.problems[0].contains("F-M1"), d.problems[0])
        val m = d.prep.surface("front").meas
        assertEquals(listOf("F-T1", "F-X1"), m.map { it.code })
        assertTrue(SurveyPrep.isLocked(m[0]))
        assertEquals(9, m[1].extra["z"])
        assertFalse(SurveyMeas.known(m[1].kind)); assertTrue(SurveyMeas.measures(m[1]).isEmpty())
        // an empty meas the file had is written back
        front["meas"] = emptyList<Any?>(); w["front"] = front
        val e = SurveyPrepFile.decode(o.toMutableMap().apply { put("w", w) })!!
        @Suppress("UNCHECKED_CAST") val fe = ((SurveyPrepFile.encode(e.prep, L, W, H)["w"] as Map<String, Map<String, Any?>>).getValue("front"))
        assertEquals(emptyList<Any?>(), fe["meas"])
    }

    @Test fun `keys -- len for a Line, per for an area, none for a tape, and deleteMark takes exactly its own`() {
        val p = SurveyPrep.fresh(L, W, H)
        val t = p.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 1200.0), pt(1000.0, 1200.0)), "t")
        val m1 = p.addMeas("front", SurveyMeas.LINE, listOf(pt(500.0, 1600.0), pt(1300.0, 2200.0)), "m1")
        val m2 = p.addMeas("front", SurveyMeas.AREA, listOf(pt(0.0, 0.0), pt(1000.0, 0.0), pt(1000.0, 1000.0), pt(0.0, 1000.0)), "m2")
        assertTrue(p.keysOf(t).isEmpty()); assertNull(SurveyMeas.key(t)); assertNull(p.nextOpen(t, null))
        assertEquals(listOf("F-M1.len"), p.keysOf(m1)); assertEquals(listOf("F-M2.per"), p.keysOf(m2))
        assertEquals("F-M1.len", p.nextOpen(m1, null))
        val ab = { s: String -> SurveyPrep.size(s, L, W, H) }
        val (s, len) = assertNotNull(p.readingOf("F-M1.len", ab))
        assertEquals("front", s); assertEquals("length", len.what); assertEquals(1000.0, len.est!!, 1e-9)
        assertEquals(4000.0, p.readingOf("F-M2.per", ab)!!.second.est!!, 1e-9)
        assertNull(p.readingOf("F-T1.len", ab))
        // a D2 reading on F-M1: nothing more to take on it
        p.actual["F-M1.len"] = 1003.0
        assertNull(p.nextAfter("F-M1.len", ab)); assertNull(p.nextOpen(m1, "F-M1.len"))
        p.actual["F-M2.per"] = 3990.0
        // a mark beside them, with a reading that shares the prefix F
        val d = p.addMarkAt("front", "D", 2000.0, 1050.0, W, H, "d")
        p.actual["FD1.w"] = 905.0
        assertEquals("m1", p.ownerOf("F-M1.len")!!.id); assertEquals("meas", p.ownerOf("F-M1.len")!!.kind)
        assertEquals("d", p.ownerOf("FD1.w")!!.id); assertNull(p.ownerOf("F-M9.len"))
        assertEquals("line", p.itemById("m1")!!.measKind)
        // deleteMark on F-M1 removes F-M1.len only
        assertTrue(p.deleteMark("m1"))
        assertEquals(setOf("F-M2.per", "FD1.w"), p.actual.keys)
        assertNull(p.measById("m1")); assertEquals(2, p.surface("front").meas.size)
        assertEquals(d, p.markById("d")?.second)
        // a locked Line is not deleted
        SurveyPrepEdit.setLocked(p, "m2", true)
        assertFalse(p.deleteMark("m2")); assertEquals(3990.0, p.actual["F-M2.per"])
        // the labels every delete and the menu use (ct52 FX9)
        assertEquals("Delete F-M2 marked area", SurveyPrepEdit.deleteLabel(p, "m2"))
        assertEquals("Delete F-T1 tape", SurveyPrepEdit.deleteLabel(p, "t"))
        assertEquals("Delete FD1 Door", SurveyPrepEdit.deleteLabel(p, "d"))
        val q = SurveyPrep.fresh(L, W, H).also { it.addMeas("front", SurveyMeas.LINE, listOf(pt(0.0, 0.0), pt(10.0, 0.0)), "x") }
        assertEquals("Delete F-M1 marked length", SurveyPrepEdit.deleteLabel(q, "x"))
    }

    @Test fun `Duplicate takes the next number of its own prefix, 100 mm away, kept on the face`() {
        val p = SurveyPrep.fresh(L, W, H)
        p.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 1200.0), pt(2678.0, 1200.0)), "t1")
        p.addMeas("front", SurveyMeas.TAPE, listOf(pt(2600.0, 0.0), pt(2600.0, 2677.0)), "t2")
        p.addMeas("front", SurveyMeas.LINE, listOf(pt(500.0, 600.0), pt(1500.0, 600.0)), "m1")
        p.actual["F-M1.len"] = 1001.0
        // ct52 T: "Duplicate makes F-T3, a tape of the same length (the Line numbering is untouched)"
        val c = assertNotNull(SurveyPrepEdit.duplicateMeas(p, "t1", W, H, "t3"))
        assertEquals("F-T3", c.code); assertEquals(SurveyMeas.TAPE, c.kind)
        // the tape runs to the right edge: back 100 to the left, 100 up -- kept on the face, so cut at its left edge (sp.js does the same)
        assertEquals(listOf(listOf(0.0, 1300.0), listOf(2578.0, 1300.0)), c.pts.map { it.toList() })
        assertEquals(2578.0, SurveyMeas.plen(c.pts), 1e-9)
        assertNull(p.surface("front").meas.firstOrNull { it.code == "F-M3" })
        val l = assertNotNull(SurveyPrepEdit.duplicateMeas(p, "m1", W, H, "m2"))
        assertEquals("F-M2", l.code)
        assertEquals(listOf(listOf(600.0, 700.0), listOf(1600.0, 700.0)), l.pts.map { it.toList() })
        // no reading, unlocked, even from a locked, read original
        SurveyPrepEdit.setLocked(p, "m1", true)
        val l2 = SurveyPrepEdit.duplicateMeas(p, "m1", W, H, "m3")!!
        assertFalse(SurveyPrep.isLocked(l2)); assertNull(p.actual["${l2.code}.len"]); assertEquals("F-M3", l2.code)
        assertNull(SurveyPrepEdit.duplicateMeas(p, "nope", W, H, "x"))
    }

    @Test fun `Lock all Line marks never locks a tape, Lock all tapes never a Line, and the menus say so`() {
        val p = SurveyPrep.fresh(L, W, H)
        p.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 1200.0), pt(2678.0, 1200.0)), "t1")
        p.addMeas("front", SurveyMeas.TAPE, listOf(pt(2600.0, 0.0), pt(2600.0, 2677.0)), "t2")
        p.addMeas("front", SurveyMeas.LINE, listOf(pt(500.0, 600.0), pt(1500.0, 600.0)), "m1")
        p.addMeas("front", SurveyMeas.AREA, listOf(pt(0.0, 0.0), pt(900.0, 0.0), pt(900.0, 900.0)), "m2")
        p.addMarkAt("front", "D", 2000.0, 1050.0, W, H, "d1")
        // ct52 T: holding F-T1, then F-M1
        assertEquals("Delete|Duplicate|Lock|Lock all tapes|Cancel", SurveyPrepEdit.menuItemsOf(p, "t1").joinToString("|") { it.second })
        assertEquals("Delete|Duplicate|Lock|Lock all Line marks|Cancel", SurveyPrepEdit.menuItemsOf(p, "m1").joinToString("|") { it.second })
        assertEquals("F-T1 tape", SurveyPrepEdit.menuHeaderOf(p, "t1")); assertEquals("F-M2 marked area", SurveyPrepEdit.menuHeaderOf(p, "m2"))
        assertEquals("FD1 Door", SurveyPrepEdit.menuHeaderOf(p, "d1"))
        val (s, g) = assertNotNull(SurveyPrepEdit.lockGroupOf(p, "m1"))
        assertEquals("front", s); assertEquals(SurveyPrepEdit.KIND_LINE, g.kind); assertEquals(2, g.n); assertNull(g.t)
        // "Lock all Line marks" locks F-M1 and F-M2, not the tapes, not the Door
        assertEquals(2, SurveyPrepEdit.lockAll(p, "front", null, true, g.kind))
        val by = { c: String -> p.surface("front").meas.first { it.code == c } }
        assertTrue(SurveyPrep.isLocked(by("F-M1"))); assertTrue(SurveyPrep.isLocked(by("F-M2")))
        assertFalse(SurveyPrep.isLocked(by("F-T1"))); assertFalse(SurveyPrep.isLocked(by("F-T2")))
        assertFalse(SurveyPrep.isLocked(p.markById("d1")!!.second))
        assertEquals("Delete|Duplicate|Unlock|Unlock all Line marks|Cancel", SurveyPrepEdit.menuItemsOf(p, "m2").joinToString("|") { it.second })
        assertEquals("🔒 F-M1 marked length", SurveyPrepEdit.menuHeaderOf(p, "m1"))
        // "Lock all tapes" locks the two tapes only
        assertEquals(2, SurveyPrepEdit.lockAll(p, "front", null, true, SurveyPrepEdit.KIND_TAPE))
        assertFalse(SurveyPrep.isLocked(p.markById("d1")!!.second))
        // a mark type's Lock all leaves every Tape and Line alone; "mark" the same
        SurveyPrepEdit.lockAll(p, "front", null, false, SurveyPrepEdit.KIND_TAPE)
        assertEquals(1, SurveyPrepEdit.lockAll(p, "front", "D", true))
        assertFalse(SurveyPrep.isLocked(by("F-T1")))
        assertEquals(0, SurveyPrepEdit.lockAll(p, "front", null, true, SurveyPrepEdit.KIND_MARK))
        // Lock all with neither: everything
        assertEquals(2, SurveyPrepEdit.lockAll(p, "front", null, true))
        assertTrue(p.itemsOf("front").all { it.locked })
        // the lock toggles a Line like a mark
        assertEquals(false, SurveyPrepEdit.setLocked(p, "m1", null))
    }

    @Test fun `clears and Start over take unlocked Tapes and Lines with their readings, and keep the locked ones`() {
        val p = SurveyPrep.fresh(L, W, H)
        p.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 1200.0), pt(2678.0, 1200.0)), "t1")
        p.addMeas("front", SurveyMeas.LINE, listOf(pt(500.0, 600.0), pt(1500.0, 600.0)), "m1")
        p.addMeas("front", SurveyMeas.AREA, listOf(pt(0.0, 0.0), pt(900.0, 0.0), pt(900.0, 900.0)), "m2")
        p.addMarkAt("front", "D", 2000.0, 1050.0, W, H, "d1")
        p.actual.putAll(mapOf("F-M1.len" to 1001.0, "F-M2.per" to 3170.0, "FD1.w" to 905.0))
        SurveyPrepEdit.setLocked(p, "m2", true)
        assertEquals(listOf("mark", "meas", "meas", "meas"), p.itemsOf("front").map { it.kind })
        assertEquals(3, SurveyPrepEdit.clearCount(p, SurveyPrepEdit.SURF, "front"))
        assertEquals(1, SurveyPrepEdit.lockedCount(p, SurveyPrepEdit.SURF, "front"))
        // Start over (pure): the locked area and its reading only
        val r = SurveyPrepEdit.startOver(p, L, W, H)
        assertEquals(1, r.kept)
        assertEquals(listOf("F-M2"), r.prep.surface("front").meas.map { it.code })
        assertEquals(mapOf("F-M2.per" to 3170.0), r.prep.actual.toMap())
        // ... and a code that went is not handed out again after it
        assertEquals("F-T2", r.prep.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 0.0), pt(100.0, 0.0)), "n").code)
        assertEquals("F-M3", r.prep.addMeas("front", SurveyMeas.LINE, listOf(pt(0.0, 0.0), pt(100.0, 0.0)), "n2").code)
        // the clear: the tape, the Line and the Door go with their readings
        val c = SurveyPrepEdit.clearItems(p, SurveyPrepEdit.SURF, "front")
        assertEquals(3, c.n); assertEquals(1, c.kept)
        assertEquals(listOf("F-M2"), p.surface("front").meas.map { it.code })
        assertEquals(mapOf("F-M2.per" to 3170.0), p.actual.toMap())
        assertEquals("F-M3", p.addMeas("front", SurveyMeas.LINE, listOf(pt(0.0, 0.0), pt(100.0, 0.0)), "n3").code)
        assertEquals("F-T2", p.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 0.0), pt(100.0, 0.0)), "n4").code)
    }

    @Test fun `undo keeps a Line's reading -- a reading belongs while its Line is there`() {
        // the screen's ↶ keeps a reading whose owner the older prep still has (Prep.ownerOf): a Line's included
        val p = SurveyPrep.fresh(L, W, H)
        p.addMeas("front", SurveyMeas.LINE, listOf(pt(500.0, 600.0), pt(1500.0, 600.0)), "m1")
        val before = p.copy()
        p.actual["F-M1.len"] = 1001.0
        p.addMarkAt("front", "W", 1300.0, 1500.0, W, H, "w1")
        val merged = before.actual.toMutableMap().apply { putAll(p.actual) }
        val kept = merged.filterKeys { before.ownerOf(it) != null }
        assertEquals(mapOf("F-M1.len" to 1001.0), kept)
        // and keepSeq carries the Tape / Line counter across, so a code ↶ took is not handed out again
        val q = SurveyPrep.fresh(L, W, H)
        val older = q.copy()
        q.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 0.0), pt(100.0, 0.0)), "t")
        SurveyPrepEdit.keepSeq(q, older)
        assertEquals("F-T2", older.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 0.0), pt(100.0, 0.0)), "t2").code)
    }

    @Test fun `the words on screen -- list names, the selection row, the tags`() {
        val p = SurveyPrep.fresh(L, W, H)
        val t = p.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 1200.0), pt(1234.0, 1200.0)), "t")
        val m = p.addMeas("front", SurveyMeas.LINE, listOf(pt(500.0, 1600.0), pt(1300.0, 2200.0)), "m")
        val a = p.addMeas("front", SurveyMeas.AREA, listOf(pt(0.0, 0.0), pt(1240.0, 0.0), pt(1240.0, 1500.0)), "a")
        assertEquals("Tape · 1234 mm · from the photo", SurveyMeas.listName("front", t, false))
        assertEquals("Tape · 1234 mm · from the photo · Front wall", SurveyMeas.listName("front", t, true))
        assertEquals("Marked length", SurveyMeas.listName("front", m, false))
        assertEquals("Marked area · 0.93 m²", SurveyMeas.listName("front", a, false))
        assertEquals("Marked length · Front wall", SurveyMeas.listName("front", m, true))
        assertEquals("Tape · 1234 mm", SurveyMeas.rowValue(t, p.actual))
        assertEquals("≈1000 mm", SurveyMeas.rowValue(m, p.actual))
        p.actual["F-M1.len"] = 1004.0
        assertEquals("1004 mm", SurveyMeas.rowValue(m, p.actual))
        assertEquals("0.93 m²", SurveyMeas.rowValue(a, p.actual))
        assertEquals("1234 mm", SurveyMeas.tapeTag(null, 1234.4)); assertEquals("F-T1 1234 mm", SurveyMeas.tapeTag("F-T1", 1234.4))
        assertEquals("F-M1 1000 mm", SurveyMeas.lineTag("F-M1", SurveyMeas.LINE, m.pts))
        assertEquals("F-M2 0.93 m²", SurveyMeas.lineTag("F-M2", SurveyMeas.AREA, a.pts))
        assertEquals("0.93 m²…", SurveyMeas.lineTag(null, SurveyMeas.AREA, a.pts))
        assertEquals("", SurveyMeas.lineTag(null, SurveyMeas.AREA, a.pts.take(2)))
        assertEquals("Tape", SurveyMeas.typeName(t)); assertEquals("Marked length", SurveyMeas.typeName(m)); assertEquals("Marked area", SurveyMeas.typeName(a))
    }

    @Test fun `an area by taps -- refused off the face, closed by a double tap or the first corner once it has three`() {
        val none = emptyList<DoubleArray>()
        assertEquals(SurveyMeas.Tap.REFUSED, SurveyMeas.areaTap(none, pt(-300.0, 100.0), W, H, false, false))
        assertEquals(SurveyMeas.Tap.STARTED, SurveyMeas.areaTap(none, pt(100.0, 100.0), W, H, false, false))
        val two = listOf(pt(100.0, 100.0), pt(900.0, 100.0))
        assertEquals(SurveyMeas.Tap.ADDED, SurveyMeas.areaTap(two, pt(900.0, 900.0), W, H, false, false))
        assertEquals(SurveyMeas.Tap.IGNORED, SurveyMeas.areaTap(two, pt(900.0, 100.0), W, H, true, false))
        assertEquals(SurveyMeas.Tap.ADDED, SurveyMeas.areaTap(two, pt(100.0, 100.0), W, H, false, true))   // near the first, but two corners only
        val three = two + listOf(pt(900.0, 900.0))
        assertEquals(SurveyMeas.Tap.CLOSED, SurveyMeas.areaTap(three, pt(900.0, 900.0), W, H, true, false))
        assertEquals(SurveyMeas.Tap.CLOSED, SurveyMeas.areaTap(three, pt(105.0, 102.0), W, H, false, true))
        assertEquals(SurveyMeas.Tap.REFUSED, SurveyMeas.areaTap(three, pt(100.0, 3000.0), W, H, true, false))
    }

    @Test fun `with Select, a tap within 12 dp of a Line or a tape picks it, the nearest winning`() {
        val p = SurveyPrep.fresh(L, W, H)
        val t = p.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 0.0), pt(100.0, 0.0)), "t")
        val a = p.addMeas("front", SurveyMeas.AREA, listOf(pt(0.0, 0.0), pt(100.0, 0.0), pt(100.0, 100.0), pt(0.0, 100.0)), "a")
        // screen px = mm here; r = 12
        val shapes = listOf(t to t.pts.toList(), a to a.pts.toList())
        assertEquals("a", SurveyMeas.pick(shapes, 50.0, 95.0, 12.0))          // the area's top edge
        assertEquals("a", SurveyMeas.pick(shapes, 5.0, 50.0, 12.0))           // its closing edge (last corner to first)
        assertEquals("t", SurveyMeas.pick(listOf(t to t.pts.toList()), 50.0, 11.0, 12.0))
        assertNull(SurveyMeas.pick(shapes, 50.0, 50.0, 12.0))
        assertNull(SurveyMeas.pick(listOf(t to t.pts.toList()), 50.0, 13.0, 12.0))
        // a tape's band 10 px either side of its middle: 20 px off the middle is within 12 of the band
        assertEquals("t", SurveyMeas.pick(listOf(t to t.pts.toList()), 50.0, 20.0, 12.0) { if (it.kind == SurveyMeas.TAPE) 10.0 else 0.0 })
        assertNull(SurveyMeas.pick(listOf(t to t.pts.toList()), 50.0, 23.0, 12.0) { 10.0 })
        assertEquals(5.0, SurveyMeas.segDist(-5.0, 0.0, pt(0.0, 0.0), pt(100.0, 0.0)), 1e-9)
    }

    @Test fun `the tape band is 12_5 mm either side, or 3_5 dp when that is wider, the same rule drawn and picked`() {
        // 2 px per mm, 3 px per dp: 12.5 mm = 25 px beats 3.5 dp = 10.5 px
        assertEquals(12.5, SurveyMeas.bandHalfMm(2.0, 3.0), 1e-9)
        assertEquals(25.0, SurveyMeas.bandHalfPx(2.0, 3.0), 1e-9)
        // zoomed out to 0.2 px per mm: 12.5 mm is 2.5 px, thinner than 3.5 dp = 10.5 px, so the floor applies
        assertEquals(10.5 / 0.2, SurveyMeas.bandHalfMm(0.2, 3.0), 1e-9)
        assertEquals(10.5, SurveyMeas.bandHalfPx(0.2, 3.0), 1e-9)
        // the drawn band (mm, mapped back to px) and the picked band (px) agree at every scale
        for (ppm in listOf(0.05, 0.3, 0.84, 1.0, 4.0)) assertEquals(SurveyMeas.bandHalfMm(ppm, 2.75) * ppm, SurveyMeas.bandHalfPx(ppm, 2.75), 1e-9)
        // no scale yet: the face's own 12.5 mm, and nothing to widen a pick by
        assertEquals(12.5, SurveyMeas.bandHalfMm(0.0, 3.0), 1e-9)
        assertEquals(0.0, SurveyMeas.bandHalfPx(0.0, 3.0), 1e-9)
    }

    @Test fun `a tap within 18 dp of the first corner closes an area of three or more, 19 dp does not`() {
        val first = pt(100.0, 100.0); val dpPx = 2.0
        assertTrue(SurveyMeas.nearFirst(first, 100.0 + 17 * dpPx, 100.0, 3, dpPx))
        assertFalse(SurveyMeas.nearFirst(first, 100.0 + 19 * dpPx, 100.0, 3, dpPx))
        assertFalse(SurveyMeas.nearFirst(first, 100.0 + 17 * dpPx, 100.0, 2, dpPx))   // two corners: no area yet
        assertTrue(SurveyMeas.nearFirst(first, 100.0, 100.0 - 17 * dpPx, 5, dpPx))
    }

    @Test fun `a tape's tag sits past its end, and at the canvas edge flips above the band and stays on`() {
        val dp = 1.0; val tw = 80.0; val w = 800.0; val h = 600.0
        // room past the end: 12 dp beyond it plus half the text, on the band's line
        val k = SurveyMeas.tapeTagAt(pt(100.0, 300.0), pt(300.0, 300.0), tw, 0.0, dp, w, h)
        assertEquals(300.0 + 52.0 - 40.0, k[0], 1e-9); assertEquals(304.0, k[1], 1e-9)
        // the end at the right edge: beside it instead, above the band, inside the canvas
        val e = SurveyMeas.tapeTagAt(pt(100.0, 300.0), pt(790.0, 300.0), tw, 0.0, dp, w, h)
        assertTrue(e[0] >= 2 * dp && e[0] + tw <= w - 2 * dp, "inside: ${e[0]}")
        assertTrue(e[1] < 300.0, "above the band: ${e[1]}")
        assertEquals(710.0, e[0], 1e-9); assertEquals(290.0, e[1], 1e-9)
        // pulled right to left to the left edge: still above, still inside (with a padlock)
        val l = SurveyMeas.tapeTagAt(pt(700.0, 300.0), pt(5.0, 300.0), tw, 26.0, dp, w, h)
        assertTrue(l[0] >= 2 * dp && l[0] + tw + 26.0 <= w - 2 * dp)
        assertTrue(l[1] < 300.0)
        // a plumb tape down to the bottom edge: its tag goes to the side, clamped inside
        val v = SurveyMeas.tapeTagAt(pt(400.0, 100.0), pt(400.0, 598.0), tw, 0.0, dp, w, h)
        assertTrue(v[1] <= h - 4 * dp && v[1] >= 14 * dp)
        assertTrue(v[0] >= 2 * dp && v[0] + tw <= w - 2 * dp)
        // no length: treated as pointing right, never NaN
        val z = SurveyMeas.tapeTagAt(pt(300.0, 300.0), pt(300.0, 300.0), tw, 0.0, dp, w, h)
        assertFalse(z[0].isNaN() || z[1].isNaN())
    }

    @Test fun `picking a tape clears the reading being taken, a Line takes its own, a list row's wins`() {
        val p = SurveyPrep.fresh(L, W, H)
        val t = p.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 0.0), pt(500.0, 0.0)), "t")
        val m = p.addMeas("front", SurveyMeas.LINE, listOf(pt(0.0, 100.0), pt(500.0, 100.0)), "m")
        val a = p.addMeas("front", SurveyMeas.AREA, listOf(pt(0.0, 0.0), pt(100.0, 0.0), pt(100.0, 100.0)), "a")
        assertNull(SurveyMeas.pickActive(t, true, null, "F-M1.len"))
        assertEquals(m.code + ".len", SurveyMeas.pickActive(m, true, null, null))
        assertEquals(a.code + ".per", SurveyMeas.pickActive(a, true, null, "F-M1.len"))
        // picked again: whatever is being taken stays
        assertEquals("F-W1.w", SurveyMeas.pickActive(m, false, null, "F-W1.w"))
        // a list row named the reading
        assertEquals(m.code + ".len", SurveyMeas.pickActive(m, false, m.code + ".len", null))
    }
}
