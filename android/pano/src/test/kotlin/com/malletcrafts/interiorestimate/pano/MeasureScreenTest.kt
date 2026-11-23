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
 * -- with the pure half beside each -- the nine things the design names: no figure text under the photo in any state,
 * no delete control in a row under the photo, the trash only for a held item, no estimate on the editing photo or the
 * 360, every read figure on an unpicked mark, a Line's label without its code, a turned label on a slanted Line, the
 * face opening on Mark once its lines are set, and Save not closing the screen.
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
            "Pick.settle(scene(), pick)", "Pick.row(scene(), pick)", "Pick.empty(pick)"))
            assertTrue(count(c, call) >= 1, "the screen asks $call")
        // the keypad is shut or opened as the answer says, AFTER Pick was asked
        val took = body(c, "fun took(o: Pick.Out, face: String = tab)")
        assertTrue(took.indexOf("if (o.stay) return") in 0 until took.indexOf("padDoes(o.pad)"))
        assertTrue(took.indexOf("padDoes(o.pad)") < took.indexOf("pick = o.state"))
        // only the HELD item moves: the gesture asks Pick.drag, and only the held item's handles are handed to the hit test
        assertEquals(1, count(c, "when (Pick.drag(sceneS, pickS, e.id)) {"))
        assertEquals(3, count(c, "Pick.handleId(pickS)"), "the hit test, the body grab and the ends")
        // a press picks nothing until it lifts: the tap is the one place the press acts on 2 - Mark
        assertEquals(2, count(c, "if (selecting) onTapS(pressHit, pressLine)\n"), "a tap, and a double tap until slice I4")
        // the pure half: a mark the walk reached is not held, and a drag on it pans
        val p = wall(); val walked = Pick.next(sc(p), Pick.NONE, "front").state
        assertEquals(State("d1", false, "FD1.w"), walked)
        assertEquals(Pick.Drag.PAN, Pick.drag(sc(p), walked, "d1"))
        assertEquals(Pick.Drag.MOVE, Pick.drag(sc(p), Pick.body(sc(p), walked, "d1").state, "d1"), "one tap gives the handles back")
    }

    // ---- nothing under the photo shows a measurement, deletes, or fires the laser -----------------------------

    @Test fun `no figure text under the photo in any state - the held row, the keypad and the tool row print no value`() {
        val c = code(sps)
        val panel = body(c, "val markPanel: @Composable () -> Unit =")
        // what stands there is StepRoute's answer: the keypad, the held item's row over the tool row, the tool row, or nothing
        assertEquals(1, count(panel, "val under = StepRoute.underMark(toolRow, held = row != null, keypad = padOpen && pick.gold != null)"))
        // none of it reads a reading, a figure, an estimate or the keypad's digits
        for (figure in listOf("prep.actual", "rowValue", "MarkDims", "MeasurePad.live", "SurveyChain.vals", "SurveyMeas.plen", "MeasureText",
            "PhotoFigures.ofMark", "padNow()"))
            assertEquals(0, count(panel, figure), "the panel under the photo reads $figure")
        // the held mark's row: its code over what its nudges move (a word, never a figure), its nudges, its padlock
        assertEquals(1, count(panel, "if (lk) tx.sp_locked else if (row.nudges) what else"))
        // the keypad has NO DISPLAY LINE -- its digits are on the gold plate -- and the tool row's `▸` carries no number
        val k = code(pad)
        assertEquals(0, count(k, "Text(live"), "a display line on the keypad"); assertEquals(0, count(k, "MeasurePad.live"))
        val t = code(tools)
        assertEquals(1, count(t, "Text(SurveyPrepPalette.NEXT_GLYPH,"), "`▸` is the glyph alone")
        assertEquals(1, count(t, "contentDescription = next.label ?:"), "its count is in its description only")
        // the pure half, in every state: nothing the row or the slot is given is a figure
        val p = wall(); p.actual["FD1.w"] = 905.0
        for (st in listOf(Pick.NONE, State("d1", true, "FD1.h"), State("d1", false, "FD1.h"), State("m1", true, "F-M1.len"))) {
            val row = Pick.row(sc(p), st)
            if (st.held) assertTrue(row != null, "$st: the held item has its row") else assertEquals(null, row, "$st: nothing held, no row")
            val next = SurveyPrepPalette.nextSlot(Tx.EN, p, "front", st.gold, 0, StepRoute.MARK, ::ab)
            assertFalse(next.label.orEmpty().isEmpty() && next.shown, "$st")
        }
        assertEquals(SurveyPrepPalette.NEXT_GLYPH, "▸")
    }

    @Test fun `no delete control in a row under the photo, and the trash drawn and pressed only for a held item`() {
        val c = code(sps)
        val panel = body(c, "val markPanel: @Composable () -> Unit =")
        assertEquals(0, count(panel, "deleteMark("), "a row under the photo deletes"); assertEquals(0, count(panel, "SP_DELETE"))
        assertEquals(0, count(code(pad), "elete")); assertEquals(0, count(body(code(tools), "internal fun SpPalette("), "elete"))
        // the floating trash: pressed and drawn only where Pick.trash says -- the HELD item, no tool on, in 2 - Mark
        assertEquals(1, count(c, "val tr = if (mode == StepRoute.MARK && Pick.trash(sceneS, pickS, SurveyPrepPalette.armed(toolS))) trashAt[0] else null"))
        assertEquals(1, count(c, "val pickedId = if (mode == StepRoute.MARK && Pick.trash(scene, pick, SurveyPrepPalette.armed(tool))) pickedHere else null"))
        assertEquals(2, count(c, "Pick.trash("))
        // the pure half: picked by the walk -- no trash; held -- the trash; a tool on -- none
        val p = wall()
        assertFalse(Pick.trash(sc(p), State("d1", false, "FD1.w"), false), "a mark the walk reached has no delete near the laser's thumb")
        assertTrue(Pick.trash(sc(p), State("d1", true, "FD1.w"), false))
        assertFalse(Pick.trash(sc(p), State("d1", true, null), true), "with a tool on the disc is not there")
        assertFalse(Pick.trash(sc(p, 0, StepRoute.PANO), State("d1", true, null), false), "the 360 keeps its own")
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
        assertEquals(1, count(figs, "PhotoFigures.layout(tx, o.prep, s, o.a, o.b, pk, toPx, du, dp, textW, o.live, o.ready)"))
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
        assertEquals(1, count(code(sps), "live = live, keep = figKeep, pick = pick, ready = laserReady, ring = ring), mm, u)"))
    }

    @Test fun `a Line's label is the number alone - its code only on its row - and runs along a slanted Line`() {
        val p = wall(); p.actual["F-M1.len"] = 1240.0
        val m = p.measById("m1")!!.second
        assertEquals("1240", PhotoFigures.line(m, p.actual, Pick.NONE).text, "not F-M1 1240 mm")
        assertEquals("F-M1", PhotoFigures.nameOf(p, State("m1", true, null)), "its code names it on its row")
        assertEquals(1, count(code(sps), "val code = PhotoFigures.nameOf(prep, pick) ?: sq?.code ?: sk?.code"))
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
        assertEquals(1, count(save, "val fx = SaveStay.effects(done, ok, flow)"))
        assertEquals(1, count(save, "if (SaveStay.Effect.UPLOAD in fx) { AsIsUpload.enqueue(context, id); webState = AsIsUpload.status(context, id) }"))
        assertEquals(1, count(save, "onSaved?.invoke(prep.copy(), roomSize.copyOf()) { n, why ->"))
        assertEquals(1, count(c, "onCancel(prep.copy(), roomSize.copyOf())"), "← is the one way out")
        assertEquals(1, count(body(c, "fun back()"), "onCancel(prep.copy(), roomSize.copyOf())"))
        assertEquals(1, count(body(c, "fun back()"), "if (SaveStay.Effect.UPLOAD in l.then) AsIsUpload.enqueue(context, id)"))
        assertEquals(1, count(c, "Text(SaveStay.bar(tx, saveMem, dirty, savedMsg),"), "the bar says it was saved")
        // MainActivity: the kept capture's save keeps survey prep open and serialises the picture rewrite
        val m = code(main)
        assertEquals(1, count(m, "onSaved = { p, size, report ->"))
        val kept = m.substringAfter("onSaved = { p, size, report ->").substringBefore("onCancel = { _, size -> surveyKept = null;")
        assertEquals(0, count(kept, "surveyKept = null"), "a save closes survey prep")
        assertEquals(1, count(kept, "keepRoomSize(row.deviceId, size)")); assertEquals(1, count(kept, "FaceWriter.rewriteSurvey("))
        assertEquals(1, count(kept, "surveyWrite.withLock {")); assertEquals(1, count(m, "val surveyWrite = remember { Mutex() }"))
        assertEquals(1, count(m, "onSaved = null,"), "a new capture: the press is the read-back save, ← keeps it")
        assertEquals(0, count(m, "onDone = { p, size ->"))
        // the pure half: each effect once, in order, and leaving is none of them
        assertEquals(listOf(SaveStay.Effect.SAVE, SaveStay.Effect.UPLOAD, SaveStay.Effect.KEEP_SIZE, SaveStay.Effect.PICTURES), SaveStay.effects(6, true))
        assertEquals(listOf(SaveStay.Effect.SAVE), SaveStay.effects(6, true, SaveStay.Flow.NEW))
        assertTrue(SaveStay.leave(SaveStay.saved(SaveStay.OPENED, 3, true), 3).repeats.isEmpty())
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
