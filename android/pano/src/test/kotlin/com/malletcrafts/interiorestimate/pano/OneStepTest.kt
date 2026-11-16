package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.StepRoute.Under
import com.malletcrafts.interiorestimate.pano.SurveyPrepPalette.RowKind
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ONE STEP TO MARK AND MEASURE (port 63 slice I1a; DESIGN-imagemeter §2, §7, §9, §10, §14 I1). Amit, 16 Nov 2026, on
 * build 0.3.337: "do we really need two screen now? which is mark and measure? as we should be measure where marking
 * is done." The route of three steps, what each phase shows of them, what 2 · Mark stands under the photo and how
 * tall the photo then is, the row without its Select slot and with the next-figure slot last, the tool that places one
 * item and goes off, and the tabs' counts. All of it stands BESIDE the four-step route, which the screen still draws
 * until slice I1b switches it over: the old answers are pinned here too, so that this slice is seen to change none.
 *
 * (Test names are plain ASCII: MeasureWalkTest's header says why.)
 */
class OneStepTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0
    private fun ab(s: String): DoubleArray = SurveyPrep.size(s, L, W, H)
    private val STAGES: List<Int?> = listOf(0, 1, 2, 3, 4, 5, null)
    private val SURVEY = PhaseTools.SURVEY

    private fun SurveyPrep.Prep.put(s: String, t: String, n: Int, x0: Double, x1: Double, y0: Double, y1: Double): SurveyPrep.Mark =
        SurveyPrep.Mark("id-$s-$t$n", t, SurveyPrep.CODE.getValue(s) + t + n, x0, x1, y0, y1, emptyMap()).also { surface(s).marks.add(it) }

    // ------------------------------------------------------------------ the steps

    @Test fun `the steps are 1 Lines, 2 Mark, 3 360 - marking and measuring are one step`() {
        assertEquals(listOf("lines", "mark", "360"), StepRoute.ROUTE)
        assertEquals(listOf(StepRoute.LINES, StepRoute.MARK, StepRoute.PANO), StepRoute.ROUTE)
        assertEquals(listOf("1 · Lines", "2 · Mark", "3 · 360"), StepRoute.ROUTE.indices.map { StepRoute.routeLabel(Tx.EN, it) })
        assertEquals(listOf("Lines", "Mark", "360"), StepRoute.ROUTE.map { StepRoute.routeName(Tx.EN, it) })
        // existing keys, with the Marathi they always had: nothing new to translate
        assertEquals(listOf("1 · रेषा", "2 · मार्क", "3 · 360"), StepRoute.ROUTE.indices.map { StepRoute.routeLabel(Tx.MR, it) })
        assertEquals(listOf(0, 1, 2), StepRoute.ROUTE.map { StepRoute.routeIndex(it) })
        // 3 · MEASURE IS NO STEP: its key is on no part of the route
        assertFalse(StepRoute.MEASURE in StepRoute.ROUTE); assertEquals(-1, StepRoute.routeIndex(StepRoute.MEASURE)); assertEquals(-1, StepRoute.routeIndex("plan"))
        // a key somebody still holds lands on a real step: the old third step is 2 · Mark, where its figures now are
        assertEquals(listOf("lines", "mark", "360", "mark", "lines", "lines"), listOf("lines", "mark", "360", "measure", "list", "").map { StepRoute.routeOf(it) })
        // the four-step route is untouched by this slice: the screen still draws it until I1b
        assertEquals(listOf("lines", "mark", "measure", "360"), StepRoute.STEPS)
    }

    @Test fun `every phase shows all three steps - 2 Mark is the view where the phase has no Mark tool`() {
        for (st in STAGES) assertEquals(StepRoute.ROUTE, PhaseTools.routeSteps(st), "$st")
        assertEquals(listOf(PhaseTools.Tool.LINES, PhaseTools.Tool.LIST, PhaseTools.Tool.VIEW_360), StepRoute.ROUTE.map { PhaseTools.routeTool(it) })
        assertNull(PhaseTools.routeTool(StepRoute.MEASURE)); assertNull(PhaseTools.routeTool("plan"))
        // what the phase takes away is what can be DONE in 2 · Mark, never the step
        //                                      Survey Wish  Vendor Estim. Revis. Approved unread
        assertEquals(listOf(true, true, true, false, true, false, true), STAGES.map { PhaseTools.can(PhaseTools.Tool.PALETTE, it) }, "the tool row")
        assertEquals(listOf(true, true, true, false, true, false, true), STAGES.map { PhaseTools.holds(it) }, "holding and moving")
        assertEquals(listOf(true, false, false, false, false, false, true), STAGES.map { MeasureFlow.canRead(it) }, "gold, the keypad, the laser chip, the counts, the next slot")
        // the four-step answers stand: at Estimation and Client approved the screen still has no 2 · Mark until I1b
        assertEquals(listOf("lines", "measure", "360"), PhaseTools.prepSteps(PhaseTools.ESTIMATION)); assertEquals(StepRoute.MEASURE, PhaseTools.prepMode("mark", PhaseTools.APPROVED) { true })
    }

    @Test fun `the step survey prep is on - kept, the old third step is 2 Mark, and 2 Mark without four closed lines falls to 1 Lines`() {
        for (st in STAGES) {
            for (m in StepRoute.ROUTE) assertEquals(m, PhaseTools.routeMode(m, st) { true }, "$m at $st")
            assertEquals(StepRoute.MARK, PhaseTools.routeMode(StepRoute.MEASURE, st) { true }, "$st")
            // no step falls to one that no longer exists
            for (m in listOf("lines", "mark", "measure", "360", "list")) for (markable in listOf(true, false))
                assertTrue(PhaseTools.routeMode(m, st) { markable } in StepRoute.ROUTE, "$m at $st")
            assertEquals(StepRoute.LINES, PhaseTools.routeMode(StepRoute.MARK, st) { false }, "$st")
            // 1 · Lines and 3 · 360 do not ask whether the lines close
            assertEquals(StepRoute.PANO, PhaseTools.routeMode(StepRoute.PANO, st) { false }); assertEquals(StepRoute.LINES, PhaseTools.routeMode(StepRoute.LINES, st) { false })
            assertEquals(PhaseTools.Ask.OPEN, PhaseTools.routeAsk(StepRoute.PANO, st) { false }); assertEquals(PhaseTools.Ask.OPEN, PhaseTools.routeAsk(StepRoute.LINES, st) { false })
            assertEquals(PhaseTools.Ask.HIDDEN, PhaseTools.routeAsk(StepRoute.MEASURE, st) { true }, "the old step cannot be asked for")
        }
    }

    // ------------------------------------------------------------------ what stands under the photo

    @Test fun `under the photo in 2 Mark - the tool row, the held item's row over it, or the keypad in their place - never a measurement`() {
        assertEquals(Under.TOOLS, StepRoute.underMark(toolRow = true, held = false, keypad = false))
        assertEquals(Under.HELD, StepRoute.underMark(toolRow = true, held = true, keypad = false))
        // the keypad takes the tool row's place AND the held row's: the photo never loses both
        assertEquals(Under.KEYPAD, StepRoute.underMark(toolRow = true, held = true, keypad = true)); assertEquals(Under.KEYPAD, StepRoute.underMark(true, false, true))
        // Estimation and Client approved: no tool row at all, and nothing is held there
        assertEquals(Under.NOTHING, StepRoute.underMark(toolRow = false, held = false, keypad = false)); assertEquals(Under.NOTHING, StepRoute.underMark(false, true, false))
        // there are four things it can be, and none is a bar, a box or a list
        assertEquals(listOf("NOTHING", "TOOLS", "HELD", "KEYPAD"), Under.values().map { it.name })
    }

    @Test fun `the photo's height in portrait - 514 with the row, 466 held, 388 with the keypad, 570 with no row`() {
        assertEquals(56, StepRoute.toolRowDp(false)); assertEquals(52, StepRoute.toolRowDp(true))
        assertEquals(182, StepRoute.keypadDp(false)); assertEquals(150, StepRoute.keypadDp(true))   // 6 + 4 x 44, 6 + 4 x 36
        assertEquals(48, StepRoute.heldRowDp(false))
        // DESIGN-imagemeter §2.4, "After I1"
        assertEquals(514, StepRoute.markPhotoDp(Under.TOOLS)); assertEquals(466, StepRoute.markPhotoDp(Under.HELD))
        assertEquals(388, StepRoute.markPhotoDp(Under.KEYPAD)); assertEquals(570, StepRoute.markPhotoDp(Under.NOTHING))
        // the chrome above the photo: the padding 12, the band 20, the top bar 40, tabs and strip 92, the caption 22
        assertEquals(186, StepRoute.PORTRAIT_CHROME_DP); assertEquals(12 + 20 + 40 + StepRoute.budgetDp(false, "front") + 22, StepRoute.PORTRAIT_CHROME_DP)
        // AGAINST TODAY (the four-step screen): the keypad is no longer under a 56 dp measure bar, so the photo is 60 dp
        // taller while he types (328 -> 388), and a mark being measured by the walk costs no height at all
        val today = StepRoute.PORTRAIT_WINDOW_DP - StepRoute.PORTRAIT_CHROME_DP - StepRoute.measureBarDp(false) - StepRoute.PAD_ROWS * StepRoute.padRowDp(false) - 4 - 6
        assertEquals(328, today); assertEquals(60, StepRoute.markPhotoDp(Under.KEYPAD) - today)
    }

    @Test fun `in landscape everything 2 Mark can stand under the photo fits the column, and the photo keeps its height`() {
        assertEquals(212, StepRoute.landUnderDp())
        for (u in Under.values()) assertTrue(StepRoute.landFits(u), "$u: ${StepRoute.underDp(u, true)} dp")
        assertEquals(listOf(0, 52, 86, 150), Under.values().map { StepRoute.underDp(it, true) })
        assertEquals(listOf(0, 56, 104, 182), Under.values().map { StepRoute.underDp(it, false) })
        // the keypad and the tool row do NOT both fit under a held row there (150 + 52 + 34): which is why the keypad takes their place
        assertTrue(StepRoute.keypadDp(true) + StepRoute.toolRowDp(true) + StepRoute.heldRowDp(true) > StepRoute.landUnderDp())
    }

    // ------------------------------------------------------------------ the row, and the tool that goes off

    @Test fun `the row has no Select slot and the next-figure slot is last - seven equal slots`() {
        for (kind in listOf("wall", "floor", "ceil")) {
            val row = SurveyPrepPalette.row(kind)
            assertEquals(listOf(RowKind.TOOL, RowKind.TOOL, RowKind.TYPE, RowKind.TYPE, RowKind.TYPE, RowKind.MORE, RowKind.NEXT), row.map { it.kind }, kind)
            assertEquals(7, row.size)
            // today's row is Select and the same six: the tools, the types and More are untouched, Select goes, the last slot is new
            val old = SurveyPrepPalette.slots(kind)
            assertEquals(SurveyPrepPalette.SlotKind.SELECT, old.first().kind); assertEquals(old.drop(1).map { it.key }, row.dropLast(1).map { it.key }, kind)
            assertFalse(row.any { it.key == SurveyPrepPalette.SEL })
        }
        assertEquals(listOf("T", "M", "W", "D", "E", "", ""), SurveyPrepPalette.row("wall").map { it.key })
        assertEquals("▸", SurveyPrepPalette.NEXT_GLYPH)
        // the Chain's slot stays gone while its switch is off, and comes back with it
        assertFalse(SurveyPrepPalette.row("wall").any { it.key == SurveyPrepPalette.CHAIN }); assertEquals(8, SurveyPrepPalette.row("wall", SurveyPrepPalette.offered(chains = true)).size)
        // which slot is lit: the armed tool's; More while a More type is armed; never the next-figure slot
        val row = SurveyPrepPalette.row("wall")
        assertEquals(listOf(false, false, false, true, false, false, false), row.map { SurveyPrepPalette.rowOn(it, "wall", "D") })
        assertEquals(listOf(true, false, false, false, false, false, false), row.map { SurveyPrepPalette.rowOn(it, "wall", SurveyPrepPalette.TAPE) })
        assertEquals(listOf(false, false, false, false, false, true, false), row.map { SurveyPrepPalette.rowOn(it, "wall", "O") })
        assertTrue(row.none { SurveyPrepPalette.rowOn(it, "wall", SurveyPrepPalette.SEL) }, "no tool armed: nothing is lit")
    }

    @Test fun `a tool places one item and goes off - and the lit slot tapped again turns it off`() {
        for (t in listOf("D", "W", "E", "O", SurveyPrepPalette.TAPE, SurveyPrepPalette.LINE)) {
            assertEquals(SurveyPrepPalette.SEL, SurveyPrepPalette.afterPlaced(t), t); assertTrue(SurveyPrepPalette.armed(t))
            // kept on (slice I3's long press): it stays
            assertEquals(t, SurveyPrepPalette.afterPlaced(t, keep = true))
        }
        assertFalse(SurveyPrepPalette.armed(SurveyPrepPalette.SEL)); assertEquals(SurveyPrepPalette.SEL, SurveyPrepPalette.afterPlaced(SurveyPrepPalette.SEL))
        // arming and disarming by the slot is what it was
        assertEquals("D", SurveyPrepPalette.next(SurveyPrepPalette.SEL, "D")); assertEquals(SurveyPrepPalette.SEL, SurveyPrepPalette.next("D", "D")); assertEquals("W", SurveyPrepPalette.next("D", "W"))
        // so a stray tap after a placement places no second door: with no tool armed an empty tap only lets the pick go
        assertEquals(Pick.NONE, Pick.empty(Pick.State("new", true, null)).state)
    }

    @Test fun `the next-figure slot is drawn at Survey while the face has a figure left or one is gold - with no number on it`() {
        val p = SurveyPrep.fresh(L, W, H); p.put("front", "D", 1, 300.0, 1200.0, 0.0, 2100.0)
        fun slot(gold: String? = null, stage: Int? = SURVEY, step: String = StepRoute.MARK, face: String = "front") = SurveyPrepPalette.nextSlot(Tx.EN, p, face, gold, stage, step, ::ab)
        slot().let { assertTrue(it.shown); assertEquals("Next figure to measure · 4 left", it.label) }
        p.actual["FD1.w"] = 898.0; assertEquals("Next figure to measure · 3 left", slot("FD1.h").label)
        // the count is in its description ONLY: the glyph has no digit, and the open face's tab shows the same number
        assertFalse(SurveyPrepPalette.NEXT_GLYPH.any { it.isDigit() }); assertEquals(3, MeasureFlow.tabLeft(p, "front", SURVEY, W, H))
        // nothing left and nothing gold: not drawn (its place stays empty: it is still a slot of the row)
        for (k in listOf("FD1.h", "FD1.l", "FD1.p")) p.actual[k] = 100.0
        slot().let { assertFalse(it.shown); assertNull(it.label) }
        // nothing left but a figure gold (a reading that failed on the wall's last figure): drawn, to keep it
        slot("FD1.p").let { assertTrue(it.shown); assertEquals("Keep this reading · nothing else left on this wall", it.label) }
        // a face with nothing on it; the Plan; after Survey; the 360; 1 · Lines: never
        assertFalse(slot(face = "right").shown); assertFalse(slot(gold = "FD1.p", face = "plan").shown)
        p.actual.clear()
        for (st in listOf(1, 2, 3, 4, 5)) assertFalse(slot(stage = st).shown, "$st")
        assertFalse(slot(step = StepRoute.PANO).shown); assertFalse(slot(step = StepRoute.LINES).shown); assertTrue(slot(stage = null).shown)
    }

    // ------------------------------------------------------------------ the tabs' counts, the texts

    @Test fun `a face tab's amber count is the figures left on that face, in every step - the one visible count`() {
        val p = SurveyPrep.fresh(L, W, H); p.put("front", "D", 1, 300.0, 1200.0, 0.0, 2100.0); p.put("left", "W", 1, 500.0, 1500.0, 900.0, 2100.0)
        fun left(s: String, stage: Int? = SURVEY) = ab(s).let { MeasureFlow.tabLeft(p, s, stage, it[0], it[1]) }
        // from the first mark placed, with nothing measured -- the rule names no step, so it holds in 2 · Mark as it did in 3 · Measure
        assertEquals(listOf(4, 0, 0, 5, 0, 0), SurveyPrep.SURFACES.map { left(it) })
        // (the room, the face, the stage and the face's size: five things, and the step is not one of them)
        assertEquals(5, MeasureFlow::class.java.declaredMethods.first { it.name == "tabLeft" }.parameterCount)
        p.actual["FD1.w"] = 898.0; assertEquals(3, left("front"))
        // none after Survey: nothing is keyed there
        for (st in listOf(1, 2, 3, 4, 5)) assertEquals(0, left("front", st), "$st")
        assertEquals(3, left("front", null))
    }

    @Test fun `the new texts, English first - pinned in English, and their Marathi is their English until Amit has verified the journey`() {
        assertEquals("Reference shown on 2 · Mark and 3 · 360", SurveyRef.shownOnRoute(Tx.EN, "front", true))
        assertEquals("Reference hidden on the faces — it still squares the front wall", SurveyRef.shownOnRoute(Tx.EN, "front", false))
        assertEquals(SurveyRef.shownSaid(Tx.MR, "front", false), SurveyRef.shownOnRoute(Tx.MR, "front", false), "the hidden line is the key it always was")
        assertEquals("D2 1203 mm — not used: readings are taken on the wall's own tab", MeasureText.on360(Tx.EN, 1203))
        assertEquals("This reading does not match the mark. Shoot again, or ▸ to keep it.", MeasureText.checkReading(Tx.EN))
        for (tx in listOf(Tx.EN, Tx.MR)) {
            assertEquals("Reference shown on 2 · Mark and 3 · 360", SurveyRef.shownOnRoute(tx, "front", true))
            assertEquals("D2 905 mm — not used: readings are taken on the wall's own tab", MeasureText.on360(tx, 905))
            assertEquals("Laser not connected · tap to connect", LaserChip.label(tx, LaserChip.Link.OFF))
            assertEquals("D2 connected · press the meter's own button to read", LaserChip.label(tx, LaserChip.Link.READY))
        }
        // the old line named a step that is going: it is still what the screen says until I1b
        assertEquals("Reference shown on 2 · Mark, 3 · Measure and 4 · 360", SurveyRef.shownSaid(Tx.EN, "front", true))
    }
}
