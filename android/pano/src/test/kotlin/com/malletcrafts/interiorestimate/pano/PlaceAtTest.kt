package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A tapped mark goes where the finger is (v52 slice 7, sp.js addElAt): the type's default size centred on the
 * tap, slid inside the face; a point stays a 50 mm spot centred on the tap. The synthetic room: L 3319,
 * W 2678, H 2677, so the front wall is 2678 x 2677 and the floor 2678 x 3319.
 */
class PlaceAtTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0

    private fun box(t: String, s: String, x: Double, y: Double, a: Double = W, b: Double = H) =
        SurveyPrep.placeAt(t, s, x, y, a, b).toList()

    @Test fun `centred on the tap`() {
        // a Door is 900 x 2100: centred at (1300, 1200) it spans 850..1750 and 150..2250
        assertEquals(listOf(850.0, 1750.0, 150.0, 2250.0), box("D", "front", 1300.0, 1200.0))
        // a Window 1200 x 1200 at (1000, 1500)
        assertEquals(listOf(400.0, 1600.0, 900.0, 2100.0), box("W", "front", 1000.0, 1500.0))
        // a Switch plate 200 x 90 at (600, 1150)
        assertEquals(listOf(500.0, 700.0, 1105.0, 1195.0), box("S", "front", 600.0, 1150.0))
        // the floor's Step is 900 x 300
        assertEquals(listOf(850.0, 1750.0, 1500.0, 1800.0), box("S", "floor", 1300.0, 1650.0, W, L))
    }

    @Test fun `slid back inside the face`() {
        // a Door tapped 100 mm from the left corner, at the floor: it starts at the corner and stands on the floor
        assertEquals(listOf(0.0, 900.0, 0.0, 2100.0), box("D", "front", 100.0, 50.0))
        // and near the right corner, high up: against the corner and the ceiling
        assertEquals(listOf(W - 900, W, H - 2100, H), box("D", "front", 2650.0, 2600.0))
        // a Column spans the wall's height wherever it is tapped; a Beam and a Loft its width
        assertEquals(listOf(1150.0, 1450.0, 0.0, H), box("C", "front", 1300.0, 2000.0))
        assertEquals(listOf(0.0, W, H - 400 - 300, H - 300), box("B", "front", 900.0, H - 500).let { listOf(it[0], it[1], it[2], it[3]) })
        assertEquals(listOf(0.0, W), box("L", "front", 300.0, 2400.0).subList(0, 2))
    }

    @Test fun `clamped on a wall smaller than the type`() {
        // a 600 x 1800 wall: a 1200 x 1200 Window is cut to the wall's width and kept on it
        assertEquals(listOf(0.0, 600.0, 300.0, 1500.0), box("W", "front", 100.0, 900.0, 600.0, 1800.0))
        // a 2100 mm Door on a 1500 mm high wall is cut to its height
        assertEquals(listOf(0.0, 900.0, 0.0, 1500.0), box("D", "front", 0.0, 1400.0, 1000.0, 1500.0))
        // a Switch plate on a 150 x 60 face: 150 x 60
        assertEquals(listOf(0.0, 150.0, 0.0, 60.0), box("S", "front", 75.0, 30.0, 150.0, 60.0))
    }

    @Test fun `points stay 50 x 50 centred on the tap`() {
        for ((t, s) in listOf("E" to "front", "P" to "front", "F" to "front", "E" to "floor", "P" to "floor", "F" to "ceil", "E" to "ceil")) {
            val b = box(t, s, 1234.0, 987.0, W, if (SurveyPrep.isPlan(s)) L else H)
            assertEquals(listOf(1209.0, 1259.0, 962.0, 1012.0), b, "$t on $s")
        }
        // at the very corner a spot is kept whole on the face: its centre 25 mm in
        assertEquals(listOf(0.0, 50.0, 0.0, 50.0), box("E", "front", -10.0, 5.0))
        assertEquals(listOf(W - 50, W, H - 50, H), box("E", "front", W + 10, H))
    }

    @Test fun `addMarkAt numbers as addMark does`() {
        val p = SurveyPrep.fresh(L, W, H)
        val a = p.addMarkAt("front", "D", 1300.0, 1200.0, W, H, "a")
        val b = p.addMark("front", "D", W, H, "b")
        val c = p.addMarkAt("front", "D", 400.0, 1200.0, W, H, "c")
        assertEquals(listOf("FD1", "FD2", "FD3"), listOf(a.code, b.code, c.code))
        assertEquals("GE1", p.addMarkAt("floor", "E", 100.0, 100.0, W, L, "d").code)
        assertEquals(1300.0, (a.x0 + a.x1) / 2)
        assertEquals(3, p.surface("front").marks.size)
    }


    // ------------------------------------------------------------------ port 62 slice L4: a tap places "as …"

    private val at: java.time.Instant = java.time.Instant.parse("2026-11-11T05:00:09Z")   // 10:30:09 IST

    /** A type tool's tap on the front wall at [stage]: what [PhaseTools.place] answers, and how many undo steps it took. */
    private fun tap(p: SurveyPrep.Prep, stage: Int?, picked: String? = null, t: String = "E", id: String = "n"): Pair<PhaseTools.Placed?, Int> {
        var steps = 0
        val r = PhaseTools.place(p, "front", t, 1300.0, 600.0, W, H, id, stage, picked, at) { steps++ }
        return r to steps
    }

    @Test fun `at Survey a tap places a v52 mark - no key at all - and so it does on a project S5 does not list`() {
        for (stage in listOf(PhaseTools.SURVEY, PhaseTools.stageOf(null, phoneOnly = true),
            PhaseTools.stageOf(QuoteStage.Cache(listed = false, stage = "Client wish list", by = "", at = "", readAt = ""), false))) {
            val p = SurveyPrep.fresh(L, W, H)
            val (r, steps) = tap(p, stage, picked = "wish")
            val e = r!!.mark
            assertEquals(1, steps, "one undo step"); assertEquals(false, r.amber)
            assertEquals("FE1", e.code); assertEquals(listOf(1275.0, 1325.0, 575.0, 625.0), listOf(e.x0, e.x1, e.y0, e.y1))
            assertEquals(emptyMap<String, Any?>(), e.extra, "no born, no life: a surveyed mark, whatever a chip was once cycled to")
            assertEquals(MarkLife.EXIST, MarkLife.lifeOf(e)); assertEquals(MarkLife.EXIST, MarkLife.bornOf(e))
            assertEquals(e, p.markById("n")?.second)
            // and it is exactly the mark v52 placed
            val q = SurveyPrep.fresh(L, W, H); val v52 = q.addMarkAt("front", "E", 1300.0, 600.0, W, H, "n")
            assertEquals(SurveyPrepFile.encode(q, L, W, H), SurveyPrepFile.encode(p, L, W, H)); assertEquals(v52.code, e.code)
        }
    }

    @Test fun `in the Wish list stage a tap places a Client wish - born, life, when and who, written once`() {
        val p = SurveyPrep.fresh(L, W, H)
        val (r, steps) = tap(p, PhaseTools.WISH_LIST)
        val e = r!!.mark
        assertEquals(1, steps); assertEquals(false, r.amber)
        // as the page's own placed mark reads (driven): born wish, life wish, lifeAt IST, lifeAs client
        assertEquals(listOf("born" to "wish", "life" to "wish", "lifeAt" to "2026-11-11T10:30:09+05:30", "lifeAs" to "client"), e.extra.toList())
        assertEquals(MarkLife.WISH, MarkLife.lifeOf(e)); assertEquals(MarkLife.WISH, MarkLife.bornOf(e))
        assertEquals("FE1", e.code); assertEquals(listOf(1275.0, 1325.0, 575.0, 625.0), listOf(e.x0, e.x1, e.y0, e.y1), "placed where v52 places it")
        // it is drawn a wish, it is the phase's own to edit, and its delete control reads Drop
        assertEquals("Client wish", MarkLife.tag(Tx.EN, e)!!.pill); assertEquals(listOf(6.0, 4.0), MarkLife.stroke(MarkLife.lifeOf(e))!!.dash)
        assertEquals(true, PhaseTools.editable(e, 1)); assertEquals(true, PhaseTools.delOk(e, 1)); assertEquals("Drop", MarkLife.deleteLabel(Tx.EN, e))
        // a mark born a wish has no act: a new thing has nothing to replace
        assertEquals(null, MarkLife.actOf(e)); assertEquals(false, e.extra.containsKey("act"))
        // every type the palette offers is born the same, whatever a chip was once cycled to
        for ((i, t) in listOf("W", "D", "X", "S").withIndex()) {
            val m = tap(p, 1, picked = "vadd", t = t, id = "t$i").first!!.mark
            assertEquals("wish", m.extra["born"], t); assertEquals("client", m.extra["lifeAs"], t)
        }
        // the file keeps it, and reads it back the same
        val back = SurveyPrepFile.decode(SurveyPrepFile.encode(p, L, W, H))!!.prep.markById("n")!!.second
        assertEquals(e.extra, back.extra)
    }

    @Test fun `at Vendor review and Revisions a tap places Vendor added - and after the estimate the office is told`() {
        for ((stage, amber) in listOf(PhaseTools.VENDOR_REVIEW to false, PhaseTools.REVISIONS to true)) {
            val p = SurveyPrep.fresh(L, W, H)
            val (r, steps) = tap(p, stage, picked = "wish")
            assertEquals(1, steps); assertEquals(amber, r!!.amber, "stage $stage")
            assertEquals(listOf("born" to "vadd", "life" to "vadd", "lifeAt" to "2026-11-11T10:30:09+05:30", "lifeAs" to "vendor"), r.mark.extra.toList())
            assertEquals(true, PhaseTools.editable(r.mark, stage)); assertEquals("V+", MarkLife.tag(Tx.EN, r.mark)!!.vplus)
        }
        assertEquals("After the estimate — tell the office", MarkLife.afterEstimate(Tx.EN))
    }

    @Test fun `nothing is placed at Estimated or at Client approved - no mark, no undo step, no code used`() {
        for (stage in listOf(PhaseTools.ESTIMATION, PhaseTools.APPROVED)) for (picked in listOf(null, "exist", "wish", "vadd")) for (t in listOf("E", "W", "X")) {
            val p = SurveyPrep.fresh(L, W, H); val before = SurveyPrepFile.encode(p, L, W, H)
            val (r, steps) = tap(p, stage, picked, t)
            assertEquals(null, r, "stage $stage, cycled to $picked, type $t"); assertEquals(0, steps, "no undo step")
            assertEquals(0, p.surface("front").marks.size); assertEquals(before, SurveyPrepFile.encode(p, L, W, H), "the prep is untouched")
            assertEquals(false, PhaseTools.can(PhaseTools.Tool.PALETTE, stage)); assertEquals(null, PhaseTools.asFor(stage, picked))
        }
        // the code it would have taken is still the next one
        val p = SurveyPrep.fresh(L, W, H)
        tap(p, 3); tap(p, 5)
        assertEquals("FE1", tap(p, 1).first!!.mark.code)
    }

    @Test fun `while the stage is unread a tap places what this capture's chip was cycled to - Existing until then`() {
        fun born(picked: String?) = tap(SurveyPrep.fresh(L, W, H), null, picked).first!!.let { (it.mark.extra["born"] as String?) to it.amber }
        assertEquals(null to false, born(null)); assertEquals(null to false, born("exist")); assertEquals(null to false, born("nonsense"))
        assertEquals("wish" to false, born("wish")); assertEquals("vadd" to false, born("vadd"))
        // what is placed is what the chip says, at every stage that has a chip
        for (stage in listOf(0, 1, 2, 4, null)) for (picked in listOf(null, "exist", "wish", "vadd")) {
            val e = tap(SurveyPrep.fresh(L, W, H), stage, picked).first!!.mark
            val chip = SurveyPrepPalette.chip(Tx.EN, "front", "E", stage, picked)!!
            assertEquals(SurveyPrepPalette.asName(Tx.EN, MarkLife.lifeOf(e)), (chip.asText ?: "as Existing").removeSuffix(" ▾"), "stage $stage, cycled to $picked")
            assertEquals(MarkLife.lifeOf(e), MarkLife.bornOf(e))
        }
    }

    @Test fun `marking a wish end to end - a new thing placed, and a surveyed thing left alone`() {
        // the room as surveyed: a window, placed at Survey
        val p = SurveyPrep.fresh(L, W, H)
        val fw1 = tap(p, 0, t = "W", id = "w").first!!.mark
        // the stage moves to Client wish list on S5; a new socket is tapped onto the same wall
        val fe1 = tap(p, 1, id = "e").first!!.mark
        assertEquals("wish", MarkLife.lifeOf(fe1)); assertEquals("exist", MarkLife.lifeOf(fw1))
        assertEquals(mapOf("exist" to 1, "wish" to 1), MarkLife.countLife(p).filterValues { it > 0 })
        // the surveyed window cannot be moved, resized, nudged, locked, duplicated or deleted by mistake ...
        assertEquals(false, PhaseTools.editable(p, "w", 1)); assertEquals(false, PhaseTools.lockOk(p, "w", 1)); assertEquals(false, PhaseTools.delOk(p, "w", 1))
        assertEquals("FW1 was marked in Survey — move the phase back to change it", PhaseTools.refuseText(Tx.EN, p, "w", 1))
        // ... it is wished through its sheet (slice L3), and the new socket is the phase's own
        assertEquals(listOf(MarkLife.WISH), MarkLife.allowedMoves(fw1, 1).map { it.to })
        assertEquals(true, PhaseTools.editable(p, "e", 1)); assertEquals(true, PhaseTools.delOk(p, "e", 1))
        // a sheet for anyone but the office still draws the room as surveyed: the window, never the wished socket
        assertEquals(listOf("FW1"), SheetMarks.marks(p.surface("front").marks, SheetMarks.Audience.OTHERS).map { it.code })
    }
}
