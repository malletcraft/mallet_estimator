package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.I18n
import com.malletcrafts.interiorestimate.pano.i18n.LiteralScan
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * THE MEASURING SCREEN, READ AS SOURCE (port 63 slice M2, 16 Nov 2026; DESIGN-port63 §4 "M2 - the screen", its list of
 * source pins). Amit, 15 Nov 2026, after his first use of the app on a client site: "Having both measurements foto and
 * measured and both is very confusing" and "The laundry list of measurements shown below screen is also frustrating".
 *
 * :app is compiled only by CI, so what the screen CALLS is read here. The rules themselves are :pano's and are tested
 * where they live (MarkDimsTest, DimLayoutTest, MeasureWalkTest, MeasurePadTest, MeasureTextTest, MeasureFlowTest,
 * StepRouteTest, PhaseToolsTest); these pins hold that the screen asks them, in ONE place each, and that what the
 * slice deleted is gone and what it promised to keep is there.
 *
 * Test names are plain ASCII: this container's Kotlin daemon cannot write a class file for a local function or a
 * lambda of a method whose name carries a middle dot or a long dash.
 */
class MeasureScreenTest {

    private val app = "app/src/main/kotlin/com/malletcrafts/interiorestimate/"
    private val pano = "pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/"
    private val sps = app + "SurveyPrepScreen.kt"
    private val spd = app + "SurveyPrepDraw.kt"
    private val bar = app + "SurveyMeasure.kt"
    private val s360 = app + "SurveyPrep360.kt"
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

    /** The body of the ONE function declared as [head] in [src], brace to brace. */
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

    // ---- the list is gone -----------------------------------------------------------------------------------

    @Test fun `the list, its tab, its typed boxes and the Show row are gone from survey prep`() {
        val c = code(sps)
        for (gone in listOf("SpTable", "SpMmField", "listPane", "surfaceBlock", "showRow", "showAt", "listView", "SP_DER",
            "common_show", "common_both", "common_measured", "common_list", "sp_x_its_list", "sp_site_only", "sp_derived"))
            assertEquals(0, count(c, gone), "$gone is still in SurveyPrepScreen.kt: the list of measurements under the photo was deleted in M2")
        val t = text(sps)
        // the Show row's three values, and the step's old key
        for (lit in listOf("\"both\"", "\"derived\"", "\"actual\"", "\"list\""))
            assertEquals(0, count(t, lit), "$lit is still written in SurveyPrepScreen.kt")
        // nothing scrolls (DESIGN-port63 §1.9): the photo takes every dp the controls leave
        assertEquals(0, count(c, "verticalScroll"), "survey prep scrolls again")
        assertEquals(0, count(c, "rememberScrollState"))
        // and the step route has no list either
        val route = code(pano + "StepRoute.kt")
        assertEquals(0, count(route, "listView"))
        assertEquals(listOf("lines", "mark", "measure", "360"), StepRoute.STEPS)
        assertEquals(listOf(listOf("front", "right", "back", "left"), listOf("floor", "ceil", "plan")), StepRoute.TAB_ROWS)
        for (rel in appSources()) assertEquals(0, count(code(rel), "StepRoute.listView"), "$rel asks for the list view")
    }

    @Test fun `the third step is asked of StepRoute by name, and each orientation draws its panel`() {
        val c = code(sps)
        assertEquals(2, count(c, "if (mode == StepRoute.MEASURE) measurePanel() else markPanel()"), "portrait and landscape both draw the measure panel")
        assertEquals(1, count(c, "val measurePanel: @Composable () -> Unit = {"))
        assertEquals(1, count(c, "val measuring = mode == StepRoute.MEASURE"))
        assertEquals(StepRoute.MEASURE, StepRoute.STEPS[2])
    }

    // ---- the walk -------------------------------------------------------------------------------------------

    @Test fun `MeasureWalk next has one caller, walkOn - and the bar's arrows are previous and step`() {
        val c = code(sps)
        assertEquals(1, count(c, "MeasureWalk.next("), "the walk is asked in one place")
        assertEquals(1, count(body(c, "fun walkOn(after: String?)"), "measureAt(MeasureWalk.next(prep, s, z[0], z[1], after))"))
        // not after Survey, and only on a face
        assertEquals(1, count(body(c, "fun walkOn(after: String?)"), "if (!StepRoute.isFace(s) || !MeasureFlow.canRead(quoteStage)) return"))
        for (rel in appSources()) if (rel != sps) assertEquals(0, count(code(rel), "MeasureWalk."), "$rel walks by itself")
        val step = body(c, "fun barStep(forward: Boolean)")
        assertEquals(1, count(c, "MeasureWalk.previous("))
        assertEquals(1, count(step, "MeasureWalk.previous(prep, tab, z[0], z[1], active)"))
        assertEquals(1, count(c, "MeasureWalk.step("))
        assertEquals(1, count(step, "MeasureWalk.step(MeasureWalk.stopsOf(prep, o.id) { ab(it) }, a, forward)"))
        assertEquals(1, count(step, "if (forward) walkOn(active)"))
        // what was typed is written before the bar moves
        assertTrue(step.indexOf("commitPad()") in 0 until step.indexOf("walkOn(active)"), "the pad is committed before the walk moves on")
        // entering the step and changing face in it start the walk from the top: both come through walkOn(null)
        assertEquals(2, count(c, "{ active = null; walkOn(null) }"))
        assertEquals(1, count(body(c, "fun setMode(m: String)"), "if (m == StepRoute.MEASURE) { active = null; walkOn(null) }"))
        // a reading that leaves its thing with nothing left walks on by itself, in 3 - Measure only
        val reading = body(c, "fun reading(code: String, v: Double, moveOn: Boolean = true)")
        assertEquals(1, count(reading, "val walking = mode == StepRoute.MEASURE && StepRoute.isFace(tab)"))
        assertEquals(1, count(reading, "if (walking && active == null) walkOn(code)"))
        assertEquals(1, count(reading, "else prep.nextAfter(code) { ab(it) }"), "2 - Mark and 4 - 360 keep v52's rule: the next one missing on the same thing")
    }

    // ---- a reading is written in one place -------------------------------------------------------------------

    @Test fun `a reading is assigned in one place and removed in one place, the removal where MmBox commit says so`() {
        val assign = Regex("""prep\.actual\[[^\]\n]*\]\s*=(?!=)""")
        val both = code(sps) + "\n" + code(bar)
        assertEquals(1, assign.findAll(both).count(), "prep.actual is assigned in one place on the measuring screen")
        assertEquals(1, count(both, "prep.actual.remove("), "and a reading is removed in one place")
        assertEquals(0, assign.findAll(code(bar)).count())
        val c = code(sps)
        val reading = body(c, "fun reading(code: String, v: Double, moveOn: Boolean = true)")
        assertEquals(1, count(reading, "prep.actual[code] = v"))
        val commit = body(c, "fun commitPad()")
        assertEquals(1, count(c, "MeasurePad.commit("), "the pad's text is committed in one place")
        assertEquals(1, count(commit, "val c = MeasurePad.commit(prep.actual[code], typed) ?: return"))
        assertEquals(1, count(commit, "if (v != null) reading(code, v, moveOn = false) else { prep.actual.remove(code); changed() }"))
        // twice is once: the digits are forgotten before they are written
        assertTrue(commit.indexOf("padKey = null") in 0 until commit.indexOf("MeasurePad.commit("))
        // and the pad's commit IS MmBox.commit, unchanged: the rule that a derived or laser figure is never saved as a reading
        assertEquals(1, count(code(pano + "MeasurePad.kt"), "fun commit(stored: Double?, state: State): MmBox.Commit? = MmBox.commit(stored, state.text)"))
        // whatever changes the active reading without one of the named exits commits too
        assertEquals(1, count(c, "LaunchedEffect(active) { commitPad() }"))
        for (head in listOf("fun showTab(t: String)", "fun setMode(m: String)", "fun back()"))
            assertTrue(count(body(c, head), "commitPad()") >= 1, "`$head` leaves the dimension without committing what was typed")
        // a laser reading that lands while digits are typed discards them and is the reading
        assertEquals(2, count(c, "dropTyped("), "its declaration and the laser's branch")
        assertTrue(c.indexOf("dropTyped(a)") in 0 until c.indexOf("reading(a, r.mm.toDouble())"), "the typed digits are dropped before the laser's reading is written")
        // no other app source writes a reading: the undo's copy, ReadingsSync's merge and a delete's sweep are other names
        for (rel in appSources()) if (rel != sps) {
            assertEquals(0, assign.findAll(code(rel)).count(), "$rel assigns prep.actual")
            assertEquals(0, count(code(rel), "prep.actual.remove("), "$rel removes a reading")
        }
    }

    // ---- readings follow the phase ---------------------------------------------------------------------------

    @Test fun `after Survey nothing is measured - the laser and a tapped figure say the refusal, and nothing stays active`() {
        val c = code(sps)
        assertEquals(2, count(c, "PhaseTools.readingRefusal(tx, quoteStage)"), "the laser's branch and a tapped figure")
        val laser = c.substring(c.indexOf("val no = PhaseTools.readingRefusal(tx, quoteStage)"))
        val say = laser.indexOf("if (no != null) say(no)"); val read = laser.indexOf("reading(a, r.mm.toDouble())"); val tick = laser.indexOf("haptic.performHapticFeedback(")
        assertTrue(say in 0 until read && read < tick, "the laser's branch refuses first, else writes, then ticks")
        assertEquals(1, count(body(c, "fun figureTap(code: String)"), "MeasureFlow.Refused.READ_ONLY -> PhaseTools.readingRefusal(tx, quoteStage)?.let { no -> say(no) }"))
        assertEquals(1, count(body(c, "fun figureTap(code: String)"), "MeasureFlow.Refused.NOT_IN_ROOM -> say(MeasureText.notInRoom(tx))"))
        assertEquals(1, count(c, "if (active != null && !MeasureFlow.canRead(quoteStage)) { active = null; rev++ }"), "a stage that moves past Survey leaves nothing active")
        // picking and placing never make a reading active by themselves: MeasureFlow answers, with the stage
        assertEquals(0, count(c, "prep.nextOpen("), "what a pick makes active is MeasureFlow.firstOpen, which knows the phase")
        assertTrue(count(c, "MeasureFlow.firstOpen(prep, ") >= 8)
        // the rule itself, at every stage
        assertEquals("1000001", stages.joinToString("") { if (MeasureFlow.canRead(it)) "1" else "0" })
        assertTrue(PhaseTools.Tool.READINGS in PhaseTools.PREP_HONOURS)
    }

    // ---- the Measure pane ------------------------------------------------------------------------------------

    @Test fun `the Measure pane's tap asks the hit test with no picked mark, and no drag starts there`() {
        val c = code(sps)
        assertEquals(2, count(c, "SurveyPrepTouch.hit("), "2 - Mark's hit and 3 - Measure's")
        assertEquals(1, count(c, "return SurveyPrepTouch.hit(drawnMarks(), null, tags.toList(), sx.toDouble(), sy.toDouble(), tol, density.density.toDouble())"),
            "selId = null and no chain nodes: nothing is dragged, resized or deleted in 3 - Measure")
        assertEquals(1, count(c, "is GestureClassifier.Event.Down -> if (!measuring) {"), "a finger down starts no drag while measuring")
        assertEquals(1, count(c, "is GestureClassifier.Event.LongPress -> if (measuring || measOn) { heldTap = true }"), "and a long press opens no menu there")
        assertEquals(1, count(c, "is SurveyPrepTouch.Hit.Tag -> onFigureS(h.code)"))
        assertEquals(1, count(c, "is SurveyPrepTouch.Hit.Mark -> onMarkTapS(h.id)"))
        assertEquals(1, count(c, "onFigure = { code -> figureTap(code) },"))
        assertEquals(1, count(c, "onMarkTap = { mid -> markTap(mid) },"))
        // what a tap does is :pano's
        assertEquals(1, count(body(c, "fun figureTap(code: String)"), "val t = MeasureFlow.figure(prep, code, active, quoteStage) { ab(it) }"))
        assertEquals(1, count(body(c, "fun markTap(mid: String)"), "val t = MeasureFlow.body(prep, mid, sel?.id, active, quoteStage) { ab(it) }"))
        // 2 - Mark's own figure tap is the same rule (a Line's, an area's and a chain's figures are tags now)
        assertEquals(2, count(c, "onFigureS(h.code)"))
        // the photo turns to a dimension that is off the screen: the rule is :pano's
        assertEquals(1, count(c, "MeasureFlow.span(prep, code)"))
        assertEquals(1, count(c, "MeasureFlow.panTo(view, box(), "))
    }

    // ---- the bar and the pad ---------------------------------------------------------------------------------

    @Test fun `the bar and the pad are drawn from MeasureFlow in both steps, and SurveyMeasure reads no rule`() {
        val c = code(sps)
        assertEquals(2, count(c, "SpMeasureBar(mbar, land,"), "2 - Mark / 4 - 360 and 3 - Measure")
        assertEquals(2, count(c, "SpMeasurePad(land, onKey = { k -> padPress(k) }, onNext = { barStep(true) }, onHide = { commitPad(); padOpen = false; rev++ })"))
        assertEquals(1, count(c, "MeasureFlow.onFace(tx, prep, tab, pid, active, quoteStage) { ab(it) }"))
        assertEquals(1, count(c, "MeasureFlow.onItem(tx, prep, active?.takeIf { a -> prep.ownerOf(a)?.s == tab }, quoteStage, withCell = padOpen) { ab(it) }"))
        assertEquals(2, count(c, "onPrev = { barStep(false) }, onNext = { barStep(true) },"))
        assertEquals(2, count(c, "onD2 = { fire() }"), "D2 on the bar is the laser's own trigger")
        // while the pad is up in 2 - Mark the palette is not drawn (PhaseToolsTest holds the gate's text)
        assertEquals(1, count(c, "val padUp = padOpen && mbar != null"))
        val b = code(bar)
        assertEquals(1, count(b, "internal fun SpMeasureBar("))
        assertEquals(1, count(b, "internal fun SpMeasurePad("))
        for (rule in listOf("MeasureWalk", "PhaseTools", "MmBox", "SurveyPrep.", "prep.", "MeasurePad.press", "MeasurePad.commit", "MeasurePad.start", "QuoteStage"))
            assertEquals(0, count(b, rule), "SurveyMeasure.kt only draws; it reads $rule")
        // its heights are the route's own arithmetic
        assertEquals(1, count(b, "height(StepRoute.measureBarDp(land).dp)"))
        assertEquals(1, count(b, "height((StepRoute.PAD_ROWS * StepRoute.padRowDp(land)).dp)"))
        // every word on it is a key: the label cell's sheet opener, the two arrows, the box, Clear and Next
        for (key in listOf("tx.life_aria_open_mark_sheet_x(cell.code)", "tx.sp_measure_aria_previous", "tx.sp_measure_aria_skip", "tx.sp_measure_aria_type", "tx.fp_clear", "tx.common_next"))
            assertEquals(1, count(b, key), "$key on the bar or the pad")
        // the pad's keys are handed over, never interpreted there
        assertEquals(2, count(b, "onKey(MeasurePad.Key.Digit("))
        assertEquals(1, count(b, "onKey(MeasurePad.Key.Back)"))
        assertEquals(1, count(b, "onKey(MeasurePad.Key.Clear)"))
    }

    @Test fun `a face tab draws its count, and the all toggle lives in 3 Measure alone and is off on entering a step`() {
        val c = code(sps)
        assertEquals(1, count(c, "MeasureFlow.tabLeft(prep, t, quoteStage, z[0], z[1])"))
        assertEquals(1, count(c, "ref = refT, land = land, left = left) { showTab(t) }"))
        assertEquals(1, count(c, "left: Int = 0, onClick: () -> Unit) {"))
        assertEquals(1, count(c, "if (left > 0) stateDescription = tx.sp_measure_x_to_measure(left.toString())"))
        assertEquals(1, count(c, "var allFigs by remember { mutableStateOf(false) }"))
        assertEquals(1, count(body(c, "fun setMode(m: String)"), "allFigs = false"))
        assertEquals(1, count(c, "allFigs = false"))
        assertEquals(1, count(c, "all = allFigs,"))
        assertEquals(1, count(c, "onAll = { allFigs = !allFigs; rev++ },"))
        assertEquals(1, count(c, "if (mode == StepRoute.MEASURE) Text(tx.sp_measure_all,"))
        assertEquals(1, count(c, "contentDescription = tx.sp_measure_aria_all; selected = all"))
    }

    // ---- the tick --------------------------------------------------------------------------------------------

    @Test fun `a laser reading ticks through Compose's haptics - no Vibrator and no vibrate permission`() {
        val t = text(sps)
        assertEquals(1, count(t, "import androidx.compose.ui.hapticfeedback.HapticFeedbackType\n"))
        assertEquals(1, count(t, "import androidx.compose.ui.platform.LocalHapticFeedback\n"))
        val c = code(sps)
        assertEquals(1, count(c, "val haptic = LocalHapticFeedback.current"))
        assertEquals(1, count(c, "haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)"))
        assertEquals(1, count(c, "performHapticFeedback("), "one tick: a typed figure makes none")
        for (rel in appSources()) assertEquals(0, count(code(rel), "Vibrator"), "$rel reaches for the Vibrator")
        val manifest = I18n.android.resolve("app/src/main/AndroidManifest.xml")
        assertTrue(manifest.isFile)
        assertFalse(manifest.readText().contains("VIBRATE"), "the app asks for no vibrate permission")
    }

    // ---- the check's sentence --------------------------------------------------------------------------------

    @Test fun `the check's sentence is said when a reading lands and when a flagged figure becomes active`() {
        val c = code(sps)
        val say = body(c, "fun sayCheck(code: String?)")
        assertEquals(1, count(say, "MeasureFlow.checkEst(prep, c) { ab(it) }"))
        assertEquals(1, count(say, "say(MeasureText.photoSays(tx, est))"))
        assertEquals(1, count(c, "MeasureText.photoSays("))
        assertEquals(1, count(body(c, "fun reading(code: String, v: Double, moveOn: Boolean = true)"), "sayCheck(code)"))
        assertEquals(1, count(body(c, "fun measureAt(code: String?)"), "sayCheck(code)"))
    }

    // ---- Capture 360 follows the phase -----------------------------------------------------------------------

    @Test fun `the room screen draws Capture 360 only in a phase that captures, read in one place`() {
        val m = code(main)
        assertEquals(1, count(m, "PhaseTools.Tool.CAPTURE"))
        assertEquals(1, count(m, "val captureShown = PhaseTools.can(PhaseTools.Tool.CAPTURE,"))
        assertEquals(1, count(m, "remember(fileKey, dataTick, quoteTick) { QuoteStageSync.prepStage(context, fileKey) })"),
            "the stage is the one a new capture's survey prep is handed")
        assertEquals(1, count(m, "ExtendedFloatingActionButton("), "the one opener of the capture sheet")
        assertEquals(1, count(m, "if (captureShown) ExtendedFloatingActionButton("))
        for (rel in appSources()) if (rel != main) assertEquals(0, count(code(rel), "PhaseTools.Tool.CAPTURE"), "$rel reads the capture rule too")
        // the row itself: Survey and Client approved, and a project not read yet or only on the phone
        assertEquals("1000011", stages.joinToString("") { if (PhaseTools.can(PhaseTools.Tool.CAPTURE, it)) "1" else "0" })
    }

    // ---- the painter -----------------------------------------------------------------------------------------

    @Test fun `the screen's figures come from MarkDims and DimLayout, and the saved pictures keep the old path`() {
        val t = text(spd)
        val c = code(spd)
        assertEquals(1, count(c, "DimLayout.mark("), "one figure per dimension is laid out in one place")
        assertEquals(1, count(c, "DimLayout.place("))
        assertEquals(1, count(t, "if (o.mode == \"main\") figures(c, s, els, o, m, u, codes, dim)"), "the screen")
        assertEquals(2, count(t, "if (o.mode == \"sheet\" && (sel || o.all)) dims(c, s, e, o, m, p, q, (if (o.all) 22f else 12f) * u, u)"),
            "the saved pictures: a box and a point, by the painter they always had (M3 moves them)")
        // a Line's, an area's and a chain's figures are taps under their reading keys, on the screen only
        assertEquals(1, count(t, "figure = if (o.mode == \"main\") SurveyMeas.key(q) else null, o = o, u = u)"))
        assertEquals(1, count(t, "keys = if (o.mode == \"main\") o.prep.keysOf(k) else null, u = u)"))
        assertEquals(1, count(c, "SurveyChain.totalReadOnly("), "a total the laser gives is not a tap")
        assertEquals(1, count(c, "DimLayout.tapOf("), "every such tap is 32 dp at least each way")
        // lineTag: the screen passes the reading; FaceWriter's pictures pass neither (figure is null there, so read is null)
        assertEquals(1, count(c, "SurveyMeas.lineTag("))
        assertEquals(1, count(t, "val read: Double? = if (figure != null && o != null) o.prep.actual[figure] else null"))
        assertEquals(1, count(c, "SurveyMeas.lineTag(code, if (kind == "))
        assertTrue(Regex("""SurveyMeas\.lineTag\(code, if \(kind == "open"\) SurveyMeas\.AREA else kind, pts, reading = read, approx = shown\)""").containsMatchIn(t))
        val pano360 = text(s360)
        assertEquals(1, count(code(s360), "SurveyMeas.lineTag("))
        assertTrue(Regex("""SurveyMeas\.lineTag\(code, if \(kind == "open"\) SurveyMeas\.AREA else kind, pts, reading = reading, approx = code != null\)""").containsMatchIn(pano360))
        assertEquals(1, count(code(s360), "meas(c, v, s, f, sf, prep.actual, st, ink, w, h)"))
        // the Show row's value survives only as the saved pictures' default
        assertEquals(1, count(t, "val show: String = \"both\","))
        // FaceWriter is as it was: positional through `all`, no scope, no live digits, none of the new models
        val w = text(writer)
        assertEquals(1, count(w, "SurveyPrepDraw.Marks(prep, size[0], size[1], sq, \"sheet\", true,"))
        for (new in listOf("DimLayout", "MarkDims", "MeasureFlow", "MeasurePad", "scope =", "live =", "keep ="))
            assertEquals(0, count(code(writer), new), "FaceWriter.kt names $new: the saved pictures are not M2's to change")
        // the screen names what it draws: no Show value is passed any more
        val screen = code(sps)
        assertEquals(1, count(screen, "scope = figScope, live = live, keep = figKeep)"))
        assertEquals(0, count(screen, "show = "))
    }

    // ---- the tour: off, and all of it kept -------------------------------------------------------------------

    @Test fun `the survey tour is off with this screen and nothing of it is deleted`() {
        assertFalse(Features.HELP_TOURS)
        val c = code(sps)
        assertEquals(4, count(c, "tourAnchor("), "the four tour anchors in survey prep")
        assertEquals(1, count(c, "TourBindSurvey(tab, mode,"))
        assertEquals(1, count(code(app + "TourOverlay.kt"), "object TourSeed"), "the Demo Client's seeding is kept")
        assertTrue(I18n.android.resolve(pano + "Tour.kt").isFile)
        // the flow itself still stands, for whoever rewrites it after the journey is approved
        assertTrue(TourFlows.shipped(Tx.EN).isEmpty())
        assertTrue(TourFlows.shipped(Tx.EN, tours = true).any { it.id == "survey" })
    }
}
