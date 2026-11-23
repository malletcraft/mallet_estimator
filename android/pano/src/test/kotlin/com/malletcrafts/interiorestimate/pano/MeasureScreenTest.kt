package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Pick.State
import com.malletcrafts.interiorestimate.pano.i18n.I18n
import com.malletcrafts.interiorestimate.pano.i18n.LiteralScan
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * THE ONE STEP THAT MARKS AND MEASURES, READ AS SOURCE (port 63 slice I1b, 23 Nov 2026; DESIGN-imagemeter §13 "Tests
 * that are rewritten", §14 I1, §18, §21). Rewritten from slice M2's pins for 3 · Measure, which went in this slice.
 * Amit, 16 Nov 2026: "do we really need two screen now? which is mark and measure? as we should be measure where marking
 * is done." -- "i do not want explicit box below foto which show the measurements." -- "No foto estimation at all. It
 * confuses user". And on 0.3.345 (23 Nov 2026): "every time i open a foto, it always start from 1 - lines", "Click on
 * save always take me out to room foto", "i want to see all measurements in its units always visible", "Measurements on
 * any mark type alwyas show FM1 , 2 etc.", "no way to set alignment of measurements to line direction".
 *
 * :app is compiled only by CI, so what the screen CALLS is read here; the rules themselves are :pano's and are tested
 * where they live (PickTest, LandingTest, LaserChipTest, PhotoFiguresTest, TurnedLabelsTest, OpenStepTest, SaveStayTest,
 * OneStepTest). These pins hold that the screen asks them, in ONE place each, that what the slice deleted is gone, and
 * -- with the pure half beside each -- the things the design names: no figure text under the photo in any state, no
 * estimate on the editing photo or the 360, every read figure on an unpicked mark, a Line's label without its code, a
 * turned label on a slanted Line, the face opening on Mark once its lines are set, and Save not closing the screen.
 *
 * AND, SINCE SLICE I2b (the panel that slides up; DESIGN-imagemeter §6, §14 I2; Amit, 16 Nov 2026: "a slide up screen in
 * imagemeter give more options", "same for delete button for any item"): NOTHING under the photo but the tool row, NO
 * delete control anywhere but the panel, DONE in the panel's head, the picked item's code a chip that opens it, the
 * photo lifted under it, and Fine move's pad in the nudge row's place.
 *
 * Test names are plain ASCII: this container's Kotlin daemon cannot write a class file for a local function or a
 * lambda of a method whose name carries a middle dot or a long dash.
 */
class MeasureScreenTest {

    private val app = "app/src/main/kotlin/com/malletcrafts/interiorestimate/"
    private val pano = "pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/"
    private val sps = app + "SurveyPrepScreen.kt"
    private val spd = app + "SurveyPrepDraw.kt"
    private val pad = app + "SurveyMeasure.kt"
    private val s360 = app + "SurveyPrep360.kt"
    private val tools = app + "SurveyPrepTools.kt"
    private val main = app + "MainActivity.kt"
    private val writer = app + "FaceWriter.kt"

    private val lexed = HashMap<String, String>()

    /** One source under android/ with its comments and the bodies of its string literals blanked (the scanner's own lexer): CODE alone. */
    private fun code(rel: String): String = lexed.getOrPut(rel) {
        val f = I18n.android.resolve(rel)
        assertTrue(f.isFile, "$rel is there (${f.absolutePath})")
        LiteralScan.scan(I18n.android, rel).masked
    }

    /** One source with its comment LINES taken out and its string literals as written: for a pin that names a literal. */
    private fun text(rel: String): String {
        val f = I18n.android.resolve(rel)
        assertTrue(f.isFile, "$rel is there (${f.absolutePath})")
        return f.readLines().filter { l -> l.trimStart().let { !it.startsWith("//") && !it.startsWith("*") && !it.startsWith("/*") } }.joinToString("\n")
    }

    private fun count(hay: String, needle: String): Int = Regex.fromLiteral(needle).findAll(hay).count()

    /** The six quote stages, then a project whose stage has not been read. */
    private val stages: List<Int?> = listOf(0, 1, 2, 3, 4, 5, null)

    /** Every Kotlin source of the app and the camera module, as its path under android/. */
    private fun appSources(): List<String> = listOf("app/src/main/kotlin", "camera/src/main/kotlin").flatMap { root ->
        I18n.android.resolve(root).walkTopDown().filter { it.isFile && it.name.endsWith(".kt") }
            .map { it.relativeTo(I18n.android).invariantSeparatorsPath }.sorted().toList()
    }

    /** The body of the ONE function (or lambda) declared as [head] in [src], brace to brace. */
    private fun body(src: String, head: String): String {
        assertEquals(1, count(src, head), "one declaration of `$head`")
        val open = src.indexOf('{', src.indexOf(head) + head.length)
        assertTrue(open > 0, "`$head` has a body")
        var depth = 0
        var i = open
        while (i < src.length) {
            when (src[i]) { '{' -> depth++; '}' -> { depth--; if (depth == 0) return src.substring(open, i + 1) } }
            i++
        }
        throw AssertionError("`$head`: its body never closes")
    }

    // ---- a small wall, for the pure halves ---------------------------------------------------------------------

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0
    private fun ab(s: String) = SurveyPrep.size(s, L, W, H)
    private fun wall(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        p.surface("front").marks.add(SurveyPrep.Mark("d1", "D", "FD1", 300.0, 1200.0, 0.0, 2100.0))
        p.surface("front").marks.add(SurveyPrep.Mark("w1", "W", "FW1", 1500.0, 2400.0, 900.0, 2100.0))
        p.addMeas("front", SurveyMeas.LINE, listOf(doubleArrayOf(500.0, 1600.0), doubleArrayOf(1300.0, 2200.0)), "m1")
        p.addMeas("front", SurveyMeas.TAPE, listOf(doubleArrayOf(0.0, 1000.0), doubleArrayOf(1234.0, 1000.0)), "t1")
        return p
    }
    private fun sc(p: SurveyPrep.Prep, stage: Int? = 0, step: String = StepRoute.MARK) = Pick.Scene(p, stage, step, ::ab)

    // ---- 1 · Lines, 2 · Mark, 3 · 360 ------------------------------------------------------------------------

    @Test fun `the steps are Lines Mark and 360 - 3 Measure, its panel, its bar, all and fire are gone from the screen`() {
        assertEquals(listOf("lines", "mark", "360"), StepRoute.ROUTE)
        val c = code(sps)
        for (gone in listOf("StepRoute.MEASURE", "measurePanel", "prepSteps", "prepMode", "MeasureFlow.move", "MeasureFlow.onFace",
            "MeasureFlow.onItem", "SpMeasureBar", "allFigs", "sp_measure_all", "fun fire()", "fun barStep", "fun walkOn", "fun measureAt",
            "fun sayCheck", "fun figureTap", "fun markTap", "fun selectMark", "fun selectMeas", "fun selectChain", "measureHit",
            "photoSays", "checkEst", "DimLayout.Scope", "var active by", "var sel by"))
            assertEquals(0, count(c, gone), "SurveyPrepScreen.kt still has $gone")
        assertEquals(2, count(c, "if (mode == StepRoute.LINES) linesPanel() else markPanel()"), "portrait and landscape")
        assertEquals(1, count(c, "val mode = PhaseTools.routeMode(modeSet, quoteStage) { StepRoute.isFace(tab) && frameOf(tab) != null }"))
        assertEquals(2, count(c, "PhaseTools.routeSteps(quoteStage)"), "the strip and the press routed to a step")
        assertEquals(1, count(c, "val ask = PhaseTools.routeAsk(m, quoteStage) { StepRoute.isFace(tab) && frameOf(tab) != null }"))
        assertEquals(0, count(code(pad), "SpMeasureBar"), "the measure bar is deleted, not hidden")
        for (rel in appSources()) assertEquals(0, count(code(rel), "StepRoute.MEASURE"), "$rel names the old third step")
        // nothing scrolls, and there is no list
        assertEquals(0, count(c, "verticalScroll")); assertEquals(0, count(c, "listView"))
    }

    // ---- picked, held, gold: ONE value, Pick's -----------------------------------------------------------------

    @Test fun `what is picked held and gold is one value and every event that changes it asks Pick`() {
        val c = code(sps)
        assertEquals(1, count(c, "var pick by remember { mutableStateOf(Pick.NONE) }"))
        assertEquals(1, count(c, "fun scene() = Pick.Scene(prep, quoteStage, mode) { s -> ab(s) }"))
        // the events of the design's table, each asked of Pick
        for (call in listOf("Pick.tap(scene(), pick, hit, line)", "Pick.handle(pick, pt)", "Pick.longPress(scene(), pick, mid)",
            "Pick.placed(scene(), ", "Pick.duplicated(scene(), c.id)", "Pick.locked(scene(), pick, mid)", "Pick.deleted(pick, mid)",
            "Pick.undone(scene(), pick)", "Pick.tab(pick, tab, t)", "Pick.step(pick, mode, m, tab, ask)", "Pick.next(scene(), pick, tab)",
            "Pick.settle(scene(), pick)", "Pick.shaped(scene(), pick, mid)", "Pick.empty(pick)"))
            assertTrue(count(c, call) >= 1, "the screen asks $call")
        // the keypad is shut or opened as the answer says, AFTER Pick was asked
        val took = body(c, "fun took(o: Pick.Out, face: String = tab, body: Boolean = false)")
        assertTrue(took.indexOf("if (o.stay) return") in 0 until took.indexOf("padDoes(o.pad)"))
        assertTrue(took.indexOf("padDoes(o.pad)") < took.indexOf("pick = o.state"))
        // only the HELD item moves: the gesture asks Pick.drag, and only the held item's handles are handed to the hit test
        assertEquals(1, count(c, "when (Pick.drag(sceneS, pickS, e.id)) {"))
        assertEquals(3, count(c, "Pick.handleId(pickS)"), "the hit test, the body grab and the ends")
        // a press picks nothing until it lifts: the tap is the one place the press acts on 2 - Mark
        assertEquals(2, count(c, "if (selecting) { if (!endGrabbed) onTapS(pressHit, pressLine) }\n"), "a tap, and a double tap until slice I4 -- and a tap on an END of the held item is that end's, never one on empty photo")
        // the pure half: a mark the walk reached is not held, and a drag on it pans
        val p = wall(); val walked = Pick.next(sc(p), Pick.NONE, "front").state
        assertEquals(State("d1", false, "FD1.w"), walked)
        assertEquals(Pick.Drag.PAN, Pick.drag(sc(p), walked, "d1"))
        assertEquals(Pick.Drag.MOVE, Pick.drag(sc(p), Pick.body(sc(p), walked, "d1").state, "d1"), "one tap gives the handles back")
    }

    // ---- nothing under the photo shows a measurement, deletes, or fires the laser -----------------------------

    @Test fun `nothing stands under the photo but the tool row - no figure text in any state, and the picked item's row is gone`() {
        val c = code(sps)
        val panel = body(c, "val markPanel: @Composable () -> Unit =")
        // what stands there is StepRoute's answer, and WHAT IS PICKED OR HELD IS NOT ASKED: the tool row, the keypad in its
        // place, or nothing (port 63 slice I2b -- for the one build I1 the picked item's row stood over the tool row)
        assertEquals(1, count(panel, "when (StepRoute.underMark(toolRow, keypad = padOpen && pick.gold != null)) {"))
        assertEquals(1, count(panel, "StepRoute.Under.TOOLS -> SpPalette(tab, tool, land,")); assertEquals(1, count(panel, "StepRoute.Under.KEYPAD ->"))
        assertEquals(1, count(panel, "SpMeasurePad(land,")); assertEquals(1, count(panel, "StepRoute.Under.NOTHING -> Unit"))
        // nothing of the picked item is drawn there: no label, no code, no nudge, no padlock, no way to its sheet
        for (gone in listOf("Row(", "Text(", "SpButton(", "SpPadlock(", "selNow()", "openPanel(", "toggleLock(", "sm.code", "PhotoFigures.nameOf", "Pick."))
            assertEquals(0, count(panel, gone), "under the photo: $gone")
        // none of it reads a reading, a figure, an estimate or the keypad's digits
        for (figure in listOf("prep.actual", "rowValue", "MarkDims", "MeasurePad.live", "SurveyChain.vals", "SurveyMeas.plen", "MeasureText",
            "PhotoFigures.ofMark", "padNow()"))
            assertEquals(0, count(panel, figure), "the panel under the photo reads $figure")
        for (gone in listOf("fun nudgeMark(", "heldRowDp", "Under.HELD", "Under.ROW", "row.nudges", "row.padlock", "row.sheet")) assertEquals(0, count(c, gone), "SurveyPrepScreen.kt still has $gone")
        // the keypad has NO DISPLAY LINE -- its digits are on the gold plate -- and the tool row's `▸` carries no number
        val k = code(pad)
        assertEquals(0, count(k, "Text(live"), "a display line on the keypad"); assertEquals(0, count(k, "MeasurePad.live"))
        val t = code(tools)
        assertEquals(1, count(t, "Text(SurveyPrepPalette.NEXT_GLYPH,"), "`▸` is the glyph alone")
        assertEquals(1, count(t, "contentDescription = next.label ?:"), "its count is in its description only")
        // the pure half, in every state: under the photo it is the tool row whatever is picked, held or gold, and the photo
        // keeps its 514 dp -- "picking a mark changes nothing under the photo" (§14 I2)
        val p = wall(); p.actual["FD1.w"] = 905.0
        for (st in listOf(Pick.NONE, State("d1", true, "FD1.h"), State("d1", false, "FD1.h"), State("m1", true, "F-M1.len"))) {
            val under = StepRoute.underMark(toolRow = true, keypad = false)
            assertEquals(StepRoute.Under.TOOLS, under, "$st"); assertEquals(514, StepRoute.markPhotoDp(under), "$st")
            val next = SurveyPrepPalette.nextSlot(Tx.EN, p, "front", st.gold, 0, StepRoute.MARK, ::ab)
            assertFalse(next.label.orEmpty().isEmpty() && next.shown, "$st")
        }
        assertEquals(listOf("NOTHING", "TOOLS", "KEYPAD"), StepRoute.Under.values().map { it.name })
        assertEquals(SurveyPrepPalette.NEXT_GLYPH, "▸")
    }

    @Test fun `no delete control anywhere but the panel - no row, no disc on the photo or the 360, no menu - and there it is the last act`() {
        val c = code(sps); val p3 = code(s360); val sheet = code(app + "MarkSheet.kt")
        val panel = body(c, "val markPanel: @Composable () -> Unit =")
        assertEquals(0, count(panel, "deleteMark("), "a row under the photo deletes"); assertEquals(0, count(panel, "SP_DELETE"))
        assertEquals(0, count(code(pad), "elete")); assertEquals(0, count(body(code(tools), "internal fun SpPalette("), "elete"))
        // THE FLOATING TRASH AND THE LONG-PRESS MENU ARE GONE from every source of the app (port 63 slice I2b)
        for (rel in appSources()) for (gone in listOf("trashAt", "trashSpot(", "Pick.trash(", "onDeleteS", "SpCtxMenu", "ctxPick(", "DeleteGuard.menuTop(", "trashOf("))
            assertEquals(0, count(code(rel), gone), "$rel still has $gone")
        assertEquals(0, count(p3, "elete("), "the 360 deletes nothing by itself")
        // ONE WAY IN: the panel's act, where MarkSheet.actStep answers Delete -- and the panel's own file only hands the id back
        assertEquals(3, count(c, "deleteMark("), "its declaration, the model's own call, and the panel's Delete")
        assertEquals(1, count(body(c, "fun panelAct(act: String)"), "deleteMark(pn.id)"))
        assertEquals(1, count(c, "onPanelAct = { act -> panelAct(act) },"))
        assertEquals(0, count(sheet, "deleteMark")); assertEquals(1, count(sheet, ".clickable(role = Role.Button) { onAct(c.id) }"))
        // the pure half: for every kind of item the delete cell is the LAST act, on line 2 -- and it is picked by a deliberate
        // tap on the code chip or a long press, held or not, never by a control that stands beside the laser's thumb
        val p = wall()
        for (id in listOf("d1", "m1", "t1")) {
            val acts = MarkSheet.acts(Tx.EN, p, id, 0, null)
            assertEquals(SurveyPrepEdit.DELETE, acts.all.last().id, id); assertTrue(acts.line2.last().delete && acts.line1.none { it.delete }, id)
            assertTrue(MarkSheet.actStep(Tx.EN, p, MarkSheet.openPanel(p, id, -1000L)!!, SurveyPrepEdit.DELETE, 0, null) is MarkSheet.ActStep.Delete, id)
        }
        assertFalse(MarkSheet.acts(Tx.EN, p, "d1", 1, null).has(SurveyPrepEdit.DELETE), "a surveyed mark after Survey has no act at all")
    }

    @Test fun `Done is in the panel's head - no close glyph and no foot Done - and the panel rests half way in portrait, whole in landscape and while typing`() {
        val sheet = code(app + "MarkSheet.kt"); val st = text(app + "MarkSheet.kt"); val c = code(sps)
        // DONE AT THE RIGHT END OF THE HEAD, where no fold can hide it; the foot's Done and the ✕ are gone
        val head = body(sheet, "private fun MsHead(h: MarkSheet.Head, tall: Dp, onWide: (Int) -> Unit, onDoneWide: (Int) -> Unit, onDone: () -> Unit)")
        assertEquals(1, count(head, "Text(h.done, color = MS_INK,")); assertEquals(1, count(head, ".clickable(enabled = h.doneEnabled, role = Role.Button) { onDone() }"))
        assertTrue(head.indexOf("Text(buildAnnotatedString {") in 0 until head.indexOf("Text(h.done,"), "the name first, Done last in the row")
        assertEquals(1, count(sheet, "Text(h.done,"), "ONE Done"); assertEquals(0, count(sheet, "MsDone")); assertEquals(0, count(sheet, "v.close")); assertEquals(0, count(st, "\"✕\""))
        assertEquals(2, count(sheet, "MsHead(v.head, MarkSheet.HEAD"), "portrait and landscape, each its head")
        // the head and the acts are OUTSIDE what scrolls: in each layout MsHead and MsActs come before the scrolling column
        val port = "private fun MsPortrait(v: MarkSheet.PanelView, a: MarkSheetActions, cap: Double?, typing: () -> Unit, pulled: () -> Unit)"
        for ((layout, scrolls) in listOf(port to "verticalScroll(scroll)", "private fun MsLandscape(v: MarkSheet.PanelView, a: MarkSheetActions, typing: () -> Unit)" to "verticalScroll(rememberScrollState())")) {
            val b = body(sheet, layout)
            assertTrue(b.indexOf("MsHead(") in 0 until b.indexOf("MsActs(")); assertTrue(b.indexOf("MsActs(") < b.indexOf(scrolls), layout)
            assertEquals(1, count(b, "verticalScroll("), layout); assertEquals(1, count(b, scrolls), layout)
        }
        // WHERE IT RESTS is MarkSheet's answer, asked once as it opens; the grip is the sheet's own handle in portrait only
        val sp = body(sheet, "internal fun SpMarkSheet(v: MarkSheet.PanelView, land: Boolean, a: MarkSheetActions)")
        assertEquals(1, count(sp, "var whole by remember(land) { mutableStateOf(MarkSheet.opensWhole(land, v)) }"))
        assertEquals(1, count(sp, "val grip: (@Composable () -> Unit)? = if (MarkSheet.hasGrip(land)) { { MsGrip { y -> onTopS(y) } } } else null")); assertEquals(1, count(sp, "dragHandle = grip,"))
        // THE HALF REST IS THE PANEL'S OWN, NEVER THE LIBRARY'S ANCHOR (the review of I2, finding 1): material3 1.3 sends an
        // expanded sheet back to its half anchor at every resize -- the keyboard coming up above all -- so the library's sheet
        // is given NO half anchor, nothing here asks it to expand, and the half rest is a cap on the content, lifted when
        // a box takes the keyboard or the blocks are pulled up
        assertEquals(1, count(sp, "rememberModalBottomSheetState(skipPartiallyExpanded = MarkSheet.SKIPS_LIBRARY_HALF, confirmValueChange = mayHide)"))
        assertEquals(1, count(sheet, "skipPartiallyExpanded"), "one sheet state, and its half anchor is :pano's to switch")
        for (gone in listOf("sheet.expand(", "partialExpand(", "rememberCoroutineScope", "scope.launch")) assertEquals(0, count(sheet, gone), "the sheet composable still has $gone")
        assertEquals(1, count(sp, "val cap = MarkSheet.contentCapDp(LocalConfiguration.current.screenHeightDp.toDouble(), land, whole)"))
        assertEquals(1, count(sp, "val boxFocused: () -> Unit = { whole = MarkSheet.wholeAfter(land, whole, typing = true) }"))
        assertEquals(1, count(sp, "val pulled: () -> Unit = { whole = MarkSheet.wholeAfter(land, whole, pulled = true) }"))
        assertEquals(1, count(sp, "if (land) MsLandscape(v, a, boxFocused) else MsPortrait(v, a, cap, boxFocused, pulled)"))
        val pb = body(sheet, port)
        assertEquals(1, count(pb, "val rest = if (cap != null) Modifier.heightIn(max = cap.toFloat().dp) else Modifier"))
        assertEquals(1, count(pb, "Column(rest.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {"), "the cap is on the head, the acts and the blocks together")
        assertEquals(1, count(pb, "val up by remember { derivedStateOf { scroll.value > 0 } }")); assertEquals(1, count(pb, "LaunchedEffect(up) { if (up) pulled() }"))
        // a box that takes the keyboard makes it whole: both boxes say so
        assertEquals(2, count(sheet, ".onFocusChanged { f -> if (f.isFocused) typing() }"), "a Why box and the note")
        // IT TAKES NO TAP FOR ITS FIRST 300 MS: the sheet asks before it lets itself be closed, the screen before everything else
        assertEquals(1, count(sp, "remember { { to: SheetValue -> to != SheetValue.Hidden || answersS() } }"))
        assertEquals(1, count(c, "if (MarkSheet.answers(cur, SystemClock.uptimeMillis())) panel = cur.with(f)"))
        assertEquals(9, count(c, " typed { cur -> MarkSheet."), "every chip, box and keystroke of the blocks goes through the one gate")
        assertEquals(1, count(c, "if (!MarkSheet.answers(pn, SystemClock.uptimeMillis())) return"), "an act")
        assertEquals(1, count(c, "onDone = { if (panel?.let { cur -> MarkSheet.answers(cur, SystemClock.uptimeMillis()) } == true) panelDone() },"))
        assertEquals(1, count(c, "panel = MarkSheet.openPanel(prep, mid, SystemClock.uptimeMillis(), longPress, focusMove, fresh, hist.size)"), "the one place it opens, by the clock it is asked by")
        // the pure half: where Done is, and where the panel rests
        val p = wall(); val acts = MarkSheet.acts(Tx.EN, p, "d1", 0, null)
        assertEquals(56, MarkSheet.doneBottomDp(false)); assertEquals(36, MarkSheet.doneBottomDp(true))
        assertTrue(MarkSheet.doneBottomDp(false) < MarkSheet.foldTopDp(acts)); assertEquals(152, MarkSheet.foldTopDp(acts))
        assertTrue(MarkSheet.foldTopDp(acts) < MarkSheet.halfRestDp(756.0), "Done and both lines of acts are above the half rest")
        val view = MarkSheet.panel(Tx.EN, p, MarkSheet.openPanel(p, "d1", 0L)!!, 0, null)!!
        assertEquals("Done", view.head.done); assertTrue(view.head.doneEnabled)
        assertFalse(MarkSheet.opensWhole(false, view), "portrait: half way"); assertTrue(MarkSheet.opensWhole(true, view), "landscape: whole")
        assertTrue(MarkSheet.standsWhole(false, typing = true)); assertFalse(MarkSheet.standsWhole(false, typing = false))
        assertEquals(378.0, MarkSheet.GRIP_DP + MarkSheet.contentCapDp(756.0, land = false, whole = false)!!, "half way: the grip and the capped content")
        assertEquals(null, MarkSheet.contentCapDp(756.0, land = false, whole = MarkSheet.wholeAfter(false, false, typing = true)), "typing: no cap, whatever the keyboard does to the window")
        assertTrue(MarkSheet.wholeAfter(false, false, pulled = true)); assertTrue(MarkSheet.wholeAfter(false, true), "and it never falls back to half")
    }

    @Test fun `the picked item's code is a chip - a tap on it, and a long press when the finger lifts, open the panel - and the photo lifts under it`() {
        val c = code(sps); val d = code(spd); val p3 = code(s360)
        // THE CHIP: placed by SurveyPrepTouch before the figures, its tap box kept clear by their layout, drawn over everything
        val marks = body(d, "fun drawMarks(c: Canvas, tx: Tx, audience: SheetMarks.Audience, s: String, o: Marks, m: (Double, Double) -> FloatArray, u: Float)")
        assertEquals(1, count(marks, "chipOf = SurveyPrepTouch.codeChip(bb, SurveyPrepTouch.handlesOf(bb, spot), pen(chipFs, true).measureText(chipSays).toDouble(),"))
        assertEquals(1, count(marks, "cw, ch, dpx, SurveyPrepTouch.chipLift(hand, chipFs, u.toDouble(), dpx), o.keep)"))
        assertEquals(1, count(marks, "chipOf = SurveyPrepTouch.codeChipOfLine(scr, pen(chipFs, true).measureText(chipSays).toDouble(), cw, ch, dpx, o.keep)"))
        assertEquals(1, count(marks, "val name = PhotoFigures.nameOf(o.prep, o.pick ?: Pick.State(o.selId, false, o.active))"), "its words are the item's name, P4's one place")
        assertEquals(2, count(marks, "SurveyPrepTouch.chipText("))
        assertTrue(marks.indexOf("chipOf?.let { k -> codes.add(k.tap) }") in 0 until marks.indexOf("val laid = if (o.mode =="), "placed before the figures, and handed to their layout")
        assertTrue(marks.indexOf("drawGuide(c, g, o.a, o.b, ::face, o.guideAlpha, o.dp)") < marks.indexOf("chipOf?.let { k -> codeChip(c, k, chipSays, chipInk, chipFs, o.dp) }"), "drawn last")
        assertEquals(2, count(marks, "val chipped = sel && chipOf != null"), "a box mark and a point: its code is not drawn inside as well")
        // it is drawn for the PICKED item, held or not, on 2 - Mark with no tool on -- and the press finds it after the handles
        assertEquals(1, count(c, "chip = mode == StepRoute.MARK && !SurveyPrepPalette.armed(tool),"))
        assertEquals(1, count(text(sps), "shown = if (which == \"main\") shown else null,"), "the loupe fills nothing")
        assertEquals(1, count(c, "nodes, SurveyChain.NODE_HIT_DP * density.density, body = bodyGrab(sx, sy) != null, chip = shown.chip?.tap)"))
        assertEquals(1, count(c, "if (o.options) pick.picked?.let { pid -> openPanel(pid) }"), "Pick row 7")
        // A LONG PRESS takes the item at the press and opens its panel WHEN THE FINGER LIFTS -- not after a pinch or a cancel
        assertEquals(1, count(c, "if (onLongPressS(gid)) longId = gid")); assertEquals(1, count(c, "if (opens != null && lifted && !pinch) onOptionsS(opens)"))
        assertTrue(c.indexOf("if (onLongPressS(gid)) longId = gid") < c.indexOf("if (opens != null && lifted && !pinch) onOptionsS(opens)"))
        assertEquals(1, count(c, "fun longLifted(mid: String) { if (pick.picked == mid) openPanel(mid, longPress = true) }"))
        assertEquals(1, count(p3, "if (actS.longPress(gid)) longId = gid")); assertEquals(1, count(p3, "if (opens != null && lifted && !pinch) actS.options(opens)"))
        // THE PHOTO LIFTS UNDER THE PANEL by what PhotoLift answers -- a layout offset of the whole photo, so every touch
        // maps through it -- from the panel's top edge as the panel's own grip reports it
        assertEquals(1, count(c, "PhotoLift.lift(y0, y0 + boxH, top.toDouble(), y0 + min(item.t, tap?.t ?: item.t), y0 + max(item.b, tap?.b ?: item.b),"))
        assertEquals(1, count(c, "PhotoLift.MARGIN_DP * density.density, land).roundToInt()"))
        assertEquals(1, count(c, "Box(Modifier.fillMaxSize().offset { IntOffset(0, -liftPx()) }.background(Color.Black)"))
        assertEquals(1, count(c, "onTop = { y -> panelTop = y }))")); assertEquals(1, count(c, "shown = shown, panelTop = { panelTop },"))
        assertEquals(1, count(code(app + "MarkSheet.kt"), "onTop(at[1] + c.positionInRoot().y)"))
        // FINE MOVE: the pad is NudgePad's, over the photo, and it follows every Pick event
        assertEquals(1, count(c, "pad = NudgePad.start(prep, mid, quoteStage, last, box, shown.w, shown.h, pxPerDp, stepTaken)"))
        // DONE'S UNDO STEP IS THE FINE MOVE'S (the review of I2, finding 2): typed in the panel, then Fine move, is one step
        assertEquals(1, count(c, "MarkSheet.FINE -> fineMove(pn.id, NudgePad.doneTookStep(before, mid))")); assertEquals(1, count(c, "fun fineMove(mid: String, stepTaken: Boolean) {"))
        assertEquals(2, count(c, "fineMove("), "its declaration and the panel's act: nothing starts one any other way")
        assertEquals(1, count(c, "val r = NudgePad.press(prep, st, key, a[0], a[1]) { snapshot() }"))
        assertEquals(1, count(c, "pad = NudgePad.afterEvent(prep, pad, pick, quoteStage, o.nudge, body)")); assertEquals(1, count(c, "part = NudgePad.touched(part, before, o, body)"))
        assertEquals(1, count(c, "val fineNow = if (marking) NudgePad.after(prep, pad, pick, quoteStage) else null"))
        assertEquals(1, count(c, "for (row in NudgePad.GRID) {")); assertEquals(1, count(c, "eh?.i?.let { pi -> onHandleS(NudgePad.pointPart(pi)) }"))
        // NOTHING ON THE PAD IS THE PHOTO'S (the review of I2, finding 5; NudgePad.swallows): the pad's whole box -- edge and
        // empty cells -- takes the press before the photo does, and a key takes its press over its whole 40 dp cell: the
        // press comes BEFORE the 2 dp the key is drawn inside, so there is no gutter between two keys
        val np = body(c, "private fun SpNudgePad(st: NudgePad.State, modifier: Modifier, onKey: (NudgePad.Key) -> Unit)")
        assertEquals(1, count(np, "Column(modifier.pointerInput(Unit) { detectTapGestures { } }.padding(NudgePad.EDGE_DP.toFloat().dp)) {"))
        assertEquals(1, count(np, "Box(cell.clickable(role = Role.Button) { onKey(k) }")); assertEquals(1, count(np, ".clickable("))
        assertTrue(np.indexOf("Box(cell.clickable(role = Role.Button) { onKey(k) }") < np.indexOf(".padding(2.dp).clip(shape)"), "the press is on the cell, the inset is how it is drawn")
        assertEquals(0, count(np, "cell.padding(")); assertEquals(1, count(np, "val cell = Modifier.size(NudgePad.CELL_DP.toFloat().dp)"))
        // THE PART IT NUDGES IS DRAWN SOLID (finding 6; NudgePad.solid): the photo is handed the Fine move, the editing photo
        // alone draws it, and each handle asks the one rule -- a mark's tabs by their own keys, a Line's and a Tape's ends
        assertEquals(1, count(c, "fine = fineNow,")); assertEquals(1, count(text(sps), "nudge = if (which == \"main\") fine else null), mm, u)"), "the loupe draws none solid")
        assertEquals(1, count(d, "val solid = NudgePad.solid(o.nudge, e.id, k)")); assertEquals(1, count(d, "for (k in SurveyPrepTouch.TABS) {"))
        assertEquals(1, count(d, "val at = SurveyPrepTouch.tabAt(k, p[0].toDouble(), q[0].toDouble(), p[1].toDouble(), q[1].toDouble())"))
        assertEquals(2, count(d, "solid = { i -> NudgePad.solid(o.nudge, q.id, NudgePad.pointPart(i)) })"), "a Line or an area, and a Tape")
        assertEquals(3, count(d, "NudgePad.solid("))
        assertEquals(1, count(c, "fun sheetOrFinger(): Boolean = photoBusy || panel != null || moreOpen\n"), "the pad holds no reading back: it is no sheet")
        // the pure half: the wall's window, long-pressed, then lifted over a half-open panel
        val p = wall(); val o = Pick.longPress(sc(p), Pick.NONE, "d1")
        assertTrue(o.options && o.state.held); assertTrue(MarkSheet.openPanel(p, "d1", 0L, longPress = true) != null)
        assertEquals(0.0, PhotoLift.lift(180.0, 694.0, 378.0, 200.0, 330.0, 12.0), "an item high on the wall is left where it is")
        assertEquals(134.0, PhotoLift.lift(180.0, 694.0, 378.0, 400.0, 500.0, 12.0), "one under the panel comes up 12 dp clear of it")
        assertEquals(0.0, PhotoLift.lift(180.0, 694.0, 378.0, 400.0, 500.0, 12.0, land = true), "nothing lifts in landscape")
    }

    @Test fun `nothing on 2 Mark fires the laser - the meter's own button reads, and the chip in the top bar only connects`() {
        val c = code(sps)
        assertEquals(1, count(c, "disto.measure()"), "only 1 - Lines' reference D2 still fires, until slice I4")
        assertEquals(1, count(body(c, "fun fireRef(k: String)"), "disto.measure()"))
        assertEquals(1, count(c, "if (LaserChip.shown(quoteStage)) SpLaserChip(link(), land) {"))
        assertEquals(0, count(body(c, "private fun SpLaserChip(link: LaserChip.Link, land: Boolean, onClick: () -> Unit)"), "measure("), "the chip never fires")
        // the link starts when a figure turns gold and when survey prep opens, as LaserChip says
        assertEquals(1, count(c, "LaserChip.gold(laserMem, link(), blePermitted(), quoteStage, System.currentTimeMillis())"))
        assertEquals(1, count(c, "LaserChip.opened(laserMem, link(), blePermitted(), quoteStage, System.currentTimeMillis())"))
        assertEquals(1, count(c, "chipDid(LaserChip.state(laserMem, LaserChip.Link.valueOf(st.name), n))"))
        // a reading goes where Landing says, and nowhere else
        assertEquals(1, count(c, "val route = Landing.route(refShot, scene(), pick, r.mm, System.currentTimeMillis())"))
        assertEquals(1, count(c, "val after = Landing.after(scene(), pick, route.code, sheetOrFinger())"))
    }

    // ---- no photo estimate, anywhere -----------------------------------------------------------------------------

    @Test fun `no estimate reaches the editing photo or the 360 - the painter asks PhotoFigures and placeShown alone`() {
        val c = code(spd); val t = text(spd)
        val figs = body(c, "private fun figures(c: Canvas, tx: Tx, s: String, o: Marks, m: (Double, Double) -> FloatArray, u: Float,")
        assertEquals(1, count(figs, "PhotoFigures.layout(tx, o.prep, s, o.a, o.b, pk, toPx, du, dp, textW, o.live, o.ready, ask = o.ask)"))
        assertEquals(1, count(figs, "DimLayout.placeShown(laid, o.width.toDouble(), o.height.toDouble(), codes + o.keep, du, dp, times)"))
        assertEquals(0, count(c, "DimLayout.mark("), "the old builder printed estimates"); assertEquals(0, count(c, "OldPlace"))
        assertEquals(1, count(c, "MarkDims.plate(f.state, f.active)"), "a plate's look is READ, ASK or GOLD")
        // a Line's, area's, Tape's and chain's own tag is not drawn on the editing photo: their figures are placed with the rest
        assertEquals(1, count(t, "val quiet = o.mode == \"main\""))
        assertEquals(2, count(t, "quiet = o.mode == \"main\")"), "a plotted chain and one being drawn")
        // the saved pictures are M3's: their old painter and their tags are untouched
        assertEquals(2, count(t, "if (o.mode == \"sheet\" && (sel || o.all)) dims(c, s, e, o, m, p, q, (if (o.all) 22f else 12f) * u, u)"))
        val w = code(writer)
        for (new in listOf("PhotoFigures", "Pick.", "ready =", "ring = ring")) assertEquals(0, count(w, new), "FaceWriter.kt names $new")
        // THE 360: the number alone, no `≈`, no code -- PhotoFigures' 360 tags
        val p3 = code(s360); val p3t = text(s360)
        for (gone in listOf("SurveyMeas.lineTag(", "SurveyMeas.tapeTag(", "SurveyChain.segFigure(", "SurveyChain.totalTag(")) assertEquals(0, count(p3, gone), "the 360 still asks $gone")
        assertEquals(0, count(p3t, "\"≈\""), "a ≈ written on the 360")
        assertEquals(1, count(p3, "PhotoFigures.chainSeg360(it, i - 1)")); assertEquals(1, count(p3, "PhotoFigures.chainTotal360(vals)"))
        assertEquals(1, count(p3, "SurveyMeas.shownTag(code, kind, reading, ask = false)"))
        // the pure half: a wall nobody has measured is outlines and codes -- no figure on any unpicked mark, and none of
        // what the photo draws for its Lines and Tapes carries an estimate's glyph
        val p = wall()
        for ((_, figs2) in PhotoFigures.onFace(Tx.EN, p, "front", W, H, Pick.NONE)) assertTrue(figs2.isEmpty())
        for (f in PhotoFigures.ofMark(Tx.EN, p.actual, "front", p.markById("d1")!!.second, W, H, true, null)) assertFalse("≈" in f.text, f.text)
        for (m in p.surface("front").meas) assertFalse("≈" in PhotoFigures.line(m, p.actual, Pick.NONE).text + PhotoFigures.tape(m) + PhotoFigures.line360(m, p.actual))
    }

    @Test fun `every read figure is drawn on an unpicked mark - its size and where it sits`() {
        val p = wall(); p.actual["FD1.w"] = 905.0; p.actual["FD1.l"] = 300.0
        val figs = PhotoFigures.ofMark(Tx.EN, p.actual, "front", p.markById("d1")!!.second, W, H, picked = false, gold = null)
        assertEquals(listOf("FD1.w", "FD1.l"), figs.map { it.code }, "its width and its distance from the corner, read: both shown")
        assertTrue(figs.all { it.look == PhotoFigures.Look.READ })
        assertEquals(listOf("905", "300"), figs.map { it.text })
        // picked, the unread ones are the quiet `?`; the screen hands the painter the pick as it is, never a scope
        val picked = PhotoFigures.ofMark(Tx.EN, p.actual, "front", p.markById("d1")!!.second, W, H, picked = true, gold = null)
        assertTrue(picked.any { it.look == PhotoFigures.Look.ASK && it.text == MarkDims.ASK_TEXT })
        assertEquals(1, count(code(sps), "live = live, keep = figKeep, pick = pick, ready = laserReady, ring = ring,"))
        // ... and after Survey it asks nothing: the phase's own rule, never a `?` that cannot be acted on (the review of I1, finding 5)
        assertEquals(1, count(code(sps), "ask = MeasureFlow.canRead(scene.stage),\n"))
    }

    @Test fun `a Line's label is the number alone - its code only on its chip - and runs along a slanted Line`() {
        val p = wall(); p.actual["F-M1.len"] = 1240.0
        val m = p.measById("m1")!!.second
        assertEquals("1240", PhotoFigures.line(m, p.actual, Pick.NONE).text, "not F-M1 1240 mm")
        assertEquals("F-M1", PhotoFigures.nameOf(p, State("m1", true, null)), "its code names it on its chip")
        assertEquals("F-M1 ▾", SurveyPrepTouch.chipText(PhotoFigures.nameOf(p, State("m1", false, null))!!), "picked by the walk too")
        assertEquals(1, count(code(spd), "val name = PhotoFigures.nameOf(o.prep, o.pick ?: Pick.State(o.selId, false, o.active))")); assertEquals(0, count(code(sps), "PhotoFigures.nameOf("))
        // TURNED TO ITS LINE: the Line runs from (500, 1600) to (1300, 2200) mm on the face -- up and to the right
        val toPx: (Double, Double) -> DoubleArray = { x, y -> doubleArrayOf(x / 10, (H - y) / 10) }
        val items = PhotoFigures.items(p, "front", W, H, Pick.NONE, toPx, 1.0, { s, fs, _ -> s.length * fs * 0.6 })
        val out = DimLayout.placeShown(items, W / 10, H / 10, emptyList(), 1.0, 1.0)
        val plate = out.of("F-M1.len")!!
        val want = LabelAngle.of(50.0, (H - 1600) / 10, 130.0, (H - 2200) / 10)
        assertEquals(want, plate.angle, 1e-9); assertNotEquals(0.0, plate.angle); assertTrue(LabelAngle.upright(plate.angle))
        // the painter turns the plate by that angle, about its own middle
        assertEquals(1, count(code(spd), "if (turned) { c.save(); c.rotate(p.angle.toFloat(), cx, cy) }"))
    }

    // ---- the face opens on Mark; Save saves and stays -------------------------------------------------------------

    @Test fun `the face opens on Mark once its lines are set - on opening and on a face tab - and 1 Lines is one tap away`() {
        val c = code(sps)
        assertEquals(1, count(c, "mutableStateOf(PhaseTools.openedStep(tab, quoteStage, PhaseTools.linesSet(prep, tab, roomSize[0], roomSize[1], roomSize[2])))"))
        assertEquals(1, count(body(c, "fun showTab(t: String)"), "modeSet = PhaseTools.openStep(t, mode, quoteStage, linesSetOf(t))"))
        assertEquals(0, count(c, "mutableStateOf(\"lines\")"), "survey prep no longer opens on 1 - Lines by itself")
        // the pure half
        val p = SurveyPrep.fresh(L, W, H)
        assertEquals(StepRoute.LINES, PhaseTools.openedStep("front", 0, PhaseTools.linesSet(p, "front", L, W, H)), "a fresh capture")
        for (k in SurveyPrep.SLOTS) p.surface("front").set[k] = true
        assertEquals(StepRoute.MARK, PhaseTools.openedStep("front", 0, PhaseTools.linesSet(p, "front", L, W, H)), "its lines set")
        for (st in stages) assertEquals(PhaseTools.Ask.OPEN, PhaseTools.routeAsk(StepRoute.LINES, st) { true }, "1 - Lines at $st")
    }

    @Test fun `Save does not close the screen - it saves, uploads, keeps the size and rewrites the pictures once a press, and only back leaves`() {
        val c = code(sps)
        val save = body(c, "fun saveNow()")
        assertEquals(0, count(save, "onCancel("), "Save leaves"); assertEquals(0, count(save, "onDone"))
        assertEquals(1, count(save, "val fx = SaveStay.effects(done, ok, SaveStay.flow(saveMem))"))
        assertEquals(1, count(save, "if (SaveStay.Effect.UPLOAD in fx) { AsIsUpload.enqueue(context, id); webState = AsIsUpload.status(context, id) }"))
        assertEquals(1, count(save, "onSaved(prep.copy(), roomSize.copyOf()) { n, why ->"))
        assertEquals(1, count(c, "onCancel(prep.copy(), roomSize.copyOf())"), "← is the one way out")
        assertEquals(1, count(body(c, "fun back()"), "onCancel(prep.copy(), roomSize.copyOf())"))
        assertEquals(1, count(body(c, "fun back()"), "if (SaveStay.Effect.UPLOAD in l.then) AsIsUpload.enqueue(context, id)"))
        assertEquals(1, count(c, "Text(SaveStay.bar(tx, saveMem, dirty, savedMsg),"), "the bar says it was saved")
        // MainActivity: the kept capture's save keeps survey prep open and serialises the picture rewrite
        val m = code(main)
        assertEquals(2, count(m, "onSaved = { p, size, report ->"), "the kept capture's, and (port 63 P2N) the new capture's")
        val kept = m.substringAfter("onSaved = { p, size, report ->").substringBefore("onCancel = { _, size -> surveyKept = null;")
        assertEquals(0, count(kept, "keepNew("), "the first of the two is the kept capture's own")
        assertEquals(0, count(kept, "surveyKept = null"), "a save closes survey prep")
        assertEquals(1, count(kept, "keepRoomSize(row.deviceId, size)")); assertEquals(1, count(kept, "FaceWriter.rewriteSurvey("))
        assertEquals(1, count(kept, "surveyWrite.withLock {")); assertEquals(1, count(m, "val surveyWrite = remember { Mutex() }"))
        assertEquals(0, count(m, "onSaved = null"), "a new capture's press files it (port 63 P2N, below)")
        assertEquals(0, count(m, "onDone = { p, size ->"))
        // the pure half: each effect once, in order, and leaving is none of them
        assertEquals(listOf(SaveStay.Effect.SAVE, SaveStay.Effect.UPLOAD, SaveStay.Effect.KEEP_SIZE, SaveStay.Effect.PICTURES), SaveStay.effects(6, true))
        assertEquals(listOf(SaveStay.Effect.SAVE, SaveStay.Effect.FILE), SaveStay.effects(6, true, SaveStay.Flow.NEW))
        assertTrue(SaveStay.leave(SaveStay.saved(SaveStay.OPENED, 3, true), 3).repeats.isEmpty())
    }

    @Test fun `P2N a new capture's Save files it on the press and stays - one keep, one at a time, is the only place that files`() {
        // Port 63 P2N (DESIGN-imagemeter section 23.1; the review of I1, finding 2): the rule is SaveStay's and is tested in
        // SaveStayTest; these pins hold that the screen and MainActivity ASK it, each in one place, in the order it needs.
        val c = code(sps)
        // THE SCREEN. It opens knowing whether the capture is filed; the flow is asked at EVERY press (it changes under
        // the open screen); the press goes to the caller's keep; the upload a press held back is queued by the answer
        assertEquals(1, count(c, "mutableStateOf(SaveStay.opened(filed))")); assertEquals(0, count(c, "mutableStateOf(SaveStay.OPENED)"))
        assertEquals(0, count(c, "onSaved == null")); assertEquals(0, count(c, "onSaved?."))
        val save = body(c, "fun saveNow()")
        assertEquals(1, count(save, "saveMem = SaveStay.saved(saveMem, dirty, ok)"))
        assertEquals(1, count(save, "if (SaveStay.toKeep(fx))"))
        assertEquals(1, count(save, "val a = SaveStay.answered(saveMem, seq, n, why)")); assertEquals(1, count(save, "saveMem = a.mem"))
        assertEquals(1, count(save, "if (SaveStay.Effect.UPLOAD in a.then) { AsIsUpload.enqueue(context, id); webState = AsIsUpload.status(context, id) }"))
        assertEquals(2, count(save, "AsIsUpload.enqueue("), "at the press on a filed capture, or at the answer that files -- nowhere else")
        assertEquals(0, count(save, "FaceWriter")); assertEquals(0, count(save, "SaveStay.keep("), "the screen never decides to file")
        // ←: what was held is taken ONCE and forgotten before the keep is asked, so a late answer queues no second upload
        val back = body(c, "fun back()")
        assertEquals(1, count(back, "val l = SaveStay.leave(saveMem, dirty)")); assertEquals(1, count(back, "saveMem = SaveStay.left(saveMem)"))
        assertTrue(back.indexOf("val l = SaveStay.leave(") < back.indexOf("saveMem = SaveStay.left(") && back.indexOf("saveMem = SaveStay.left(") < back.indexOf("onCancel("))
        assertTrue(back.indexOf("onCancel(") < back.indexOf("AsIsUpload.enqueue("))
        // Discard this 360 (and the new-360 note) only while the capture is not filed and nothing is filing it
        assertEquals(2, count(c, "if (onDiscard != null && SaveStay.mayDiscard(saveMem)) {")); assertEquals(0, count(c, "if (onDiscard != null) {"))
        assertEquals(1, count(c, "onDiscard()"))

        // MAINACTIVITY. Both callers say whether the capture is filed; the new capture's press and its ← both come to keepNew
        val m = code(main)
        assertEquals(1, count(m, "filed = true,")); assertEquals(1, count(m, "filed = false,"))
        assertEquals(1, count(m, "onSaved = { p, size, report -> keepNew(p, size, leaving = false, report = report) },"))
        assertEquals(1, count(m, "onKeep = { p, size -> keepNew(p, size, leaving = true, report = null) })"))
        assertEquals(1, count(m, "onCancel = { p, size -> onKeep(p, size) },")); assertEquals(0, count(m, "fun keepWith("))
        val keep = body(m, "fun keepNew(")
        // one at a time, in the order asked: taken on the main thread (a plain scope.launch), the heavy work off it
        assertEquals(1, count(keep, "scope.launch {")); assertEquals(0, count(keep, "scope.launch(Dispatchers"))
        assertEquals(1, count(keep, "newKeep.lock.withLock {")); assertEquals(1, count(m, "val newKeep = remember(ps) { NewKeep() }"))
        assertEquals(2, count(keep, "withContext(Dispatchers.Default) {"))
        // what to do is asked UNDER the lock, of the row as it is THEN
        assertEquals(1, count(keep, "val fx = SaveStay.keep(filed = newKeep.row != null, leaving = leaving)"))
        val lock = body(keep, "newKeep.lock.withLock")
        assertEquals(1, count(lock, "val fx = SaveStay.keep("))
        // THE FILING: the six faces and the row, in the FILE branch and nowhere else in the app; the row remembered only on success
        val file = body(lock, "if (SaveStay.Effect.FILE in fx)")
        assertEquals(1, count(m, "FaceWriter.commit(")); assertEquals(1, count(file, "FaceWriter.commit(context, ps.session)"))
        assertEquals(1, count(file, "store.insert(row)")); assertEquals(1, count(keep, "store.insert("))
        assertEquals(1, count(keep, "newKeep.row = row")); assertEquals(1, count(file, "newKeep.row = row"))
        assertEquals(1, count(m, "newKeep.row = "), "nothing else ever says the capture is filed")
        assertTrue(file.indexOf("ps.session.survey = p; ps.session.surveySize = size; ps.session.heightMm = size[2]") in 0 until file.indexOf("FaceWriter.commit("),
            "the session takes THIS call's prep and size under the lock, before the faces are written")
        assertTrue(file.indexOf("FaceWriter.commit(") < file.indexOf("store.insert(row)") && file.indexOf("store.insert(row)") < file.indexOf("newKeep.row = row"))
        // a filed capture: the size, and for a press the pictures -- a kept capture's rewrite, from its row
        assertEquals(1, count(lock, "if (kept != null && SaveStay.Effect.KEEP_SIZE in fx) keepRoomSize(kept.deviceId, size)"))
        val pics = body(lock, "if (kept != null && SaveStay.Effect.PICTURES in fx)")
        assertEquals(1, count(pics, "FaceWriter.rewriteSurvey(context, File(kept.panoPath), kept.deviceId, kept.room,"))
        assertEquals(2, count(m, "FaceWriter.rewriteSurvey("), "the kept capture's and this one")
        assertTrue(lock.indexOf(file) < lock.indexOf("val kept = newKeep.row") && lock.indexOf("val kept = newKeep.row") < lock.indexOf("if (kept != null && SaveStay.Effect.KEEP_SIZE"))
        // every call is answered inside its turn (so the answers come in the order of the presses): four reports, all under the lock
        assertEquals(4, count(lock, "report?.invoke(")); assertEquals(4, count(keep, "report?.invoke("))
        // A PRESS STAYS: the dialog is closed by ← alone
        assertEquals(1, count(keep, "pending = null")); assertEquals(0, count(lock, "pending = null"))
        assertEquals(1, count(keep, "if (leaving) pending = null")); assertTrue(keep.indexOf("if (leaving) pending = null") in 0 until keep.indexOf("scope.launch {"))
        // the pure half, as the three places ask it
        assertEquals(listOf(SaveStay.Effect.FILE), SaveStay.keep(filed = false, leaving = false)); assertEquals(listOf(SaveStay.Effect.FILE), SaveStay.keep(filed = false, leaving = true))
        assertEquals(listOf(SaveStay.Effect.KEEP_SIZE, SaveStay.Effect.PICTURES), SaveStay.keep(filed = true, leaving = false))
        assertEquals(listOf(SaveStay.Effect.KEEP_SIZE), SaveStay.keep(filed = true, leaving = true))
        assertEquals(SaveStay.Flow.NEW, SaveStay.flow(SaveStay.opened(false))); assertEquals(SaveStay.Flow.KEPT, SaveStay.flow(SaveStay.opened(true)))
    }

    // ---- a reading is written in one place ----------------------------------------------------------------------

    @Test fun `a reading is assigned in one place and removed in one place, the removal where MmBox commit says so`() {
        val assign = Regex("""prep\.actual\[[^\]\n]*\]\s*=(?!=)""")
        val both = code(sps) + "\n" + code(pad)
        assertEquals(1, assign.findAll(both).count(), "prep.actual is assigned in one place on the measuring screen")
        assertEquals(1, count(both, "prep.actual.remove("), "and a reading is removed in one place")
        val c = code(sps)
        assertEquals(1, count(body(c, "fun reading(code: String, v: Double)"), "prep.actual[code] = v"))
        val commit = body(c, "fun commitPad(): Boolean")
        assertEquals(1, count(c, "MeasurePad.commit("), "the keypad's text is committed in one place")
        assertEquals(1, count(commit, "val c = MeasurePad.commit(prep.actual[code], typed) ?: return false"))
        assertTrue(commit.indexOf("padKey = null") in 0 until commit.indexOf("MeasurePad.commit("))
        assertEquals(1, count(code(pano + "MeasurePad.kt"), "fun commit(stored: Double?, state: State): MmBox.Commit? = MmBox.commit(stored, state.text)"))
        // whatever changes the gold commits what was typed for the figure it was typed for
        assertEquals(1, count(c, "LaunchedEffect(pick.gold) { commitPad() }"))
        for (head in listOf("fun showTab(t: String)", "fun back()", "fun saveNow()"))
            assertTrue(count(body(c, head), "commitPad()") + count(body(c, head), "took(o)") >= 1, "`$head` leaves the figure without committing what was typed")
        for (rel in appSources()) if (rel != sps) {
            assertEquals(0, assign.findAll(code(rel)).count(), "$rel assigns prep.actual")
            assertEquals(0, count(code(rel), "prep.actual.remove("), "$rel removes a reading")
        }
    }

    // ---- what the hand feels ------------------------------------------------------------------------------------

    @Test fun `what the hand feels is Landing's pattern, played through Compose's haptics - no Vibrator and no vibrate permission`() {
        val c = code(sps)
        assertEquals(1, count(c, "for (p in Landing.pulses(h)) {"))
        assertEquals(1, count(c, "performHapticFeedback("), "one place plays every pulse")
        for (rel in appSources()) assertEquals(0, count(code(rel), "Vibrator"), "$rel reaches for the Vibrator")
        assertFalse(I18n.android.resolve("app/src/main/AndroidManifest.xml").readText().contains("VIBRATE"))
        assertEquals(1, Landing.pulses(Landing.Haptic.TICK).size); assertEquals(2, Landing.pulses(Landing.Haptic.DOUBLE).size)
        assertTrue(Landing.pulses(Landing.Haptic.LONG).single().strong)
    }

    // ---- Capture 360 follows the phase -----------------------------------------------------------------------

    @Test fun `the room screen draws Capture 360 only in a phase that captures, read in one place`() {
        val m = code(main)
        assertEquals(1, count(m, "PhaseTools.Tool.CAPTURE"))
        assertEquals(1, count(m, "val captureShown = PhaseTools.can(PhaseTools.Tool.CAPTURE,"))
        assertEquals(1, count(m, "ExtendedFloatingActionButton("), "the one opener of the capture sheet")
        assertEquals(1, count(m, "if (captureShown) ExtendedFloatingActionButton("))
        for (rel in appSources()) if (rel != main) assertEquals(0, count(code(rel), "PhaseTools.Tool.CAPTURE"), "$rel reads the capture rule too")
        assertEquals("1000011", stages.joinToString("") { if (PhaseTools.can(PhaseTools.Tool.CAPTURE, it)) "1" else "0" })
    }

    // ---- the tour: off, and all of it kept -------------------------------------------------------------------

    @Test fun `the survey tour is off with this screen and nothing of it is deleted`() {
        assertFalse(Features.HELP_TOURS)
        val c = code(sps)
        assertEquals(4, count(c, "tourAnchor("), "the four tour anchors in survey prep")
        assertEquals(1, count(c, "TourBindSurvey(tab, mode,"))
        assertEquals(1, count(code(app + "TourOverlay.kt"), "object TourSeed"), "the Demo Client's seeding is kept")
        assertTrue(I18n.android.resolve(pano + "Tour.kt").isFile)
        assertTrue(TourFlows.shipped(Tx.EN).isEmpty())
        assertTrue(TourFlows.shipped(Tx.EN, tours = true).any { it.id == "survey" })
    }
}
