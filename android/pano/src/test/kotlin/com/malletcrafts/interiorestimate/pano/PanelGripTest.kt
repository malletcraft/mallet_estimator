package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.PanelGrip.Press
import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch.Box
import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch.Hit
import com.malletcrafts.interiorestimate.pano.i18n.I18n
import com.malletcrafts.interiorestimate.pano.i18n.LiteralScan
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.math.hypot
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * THE PANEL'S GRIP (port 63 slice F3a, 23 Nov 2026; DESIGN-imagemeter §24). Amit, with build 0.3.350 on his phone:
 * "options like unlock duplicate etc are coming when i tap the mark type very hard. there should be a simple pull out
 * dash which will let me pull up those optios."
 *
 * What is held here: WHEN the grip is shown ([PanelGrip.shows]); its tab, dash and touch box, and WHERE it stands along
 * the foot of the photo pane ([PanelGrip.at]) -- centred, else the nearest place 16 dp clear of every handle the picked
 * item SHOWS, a tie going right, never off the pane, and no grip where nothing is clear; that as placed no press in its
 * box is within a handle's reach; THE PRESS ([PanelGrip.press]): a tap, a rise of 12 dp, a slip, a second finger; and
 * that it opens the chip's own panel.
 *
 * The Mi A3's photo pane in 2 · Mark as CodeChipTest and NudgePadTest take it, 348 x 514 dp, at 2 px a dp; the 360 x
 * 514 dp the brief names; and the landscape pane, 436 x 302 dp, where the phase band lies over its foot.
 *
 * AND, SINCE SLICE F3b (§24.7), what the screen asks as it composes the photo: the ONE question, [PanelGrip.onPhoto]
 * -- shown, the picked item on this face, a clear place; the handles the picked item SHOWS ([PanelGrip.handles]);
 * what stands over the foot ([PanelGrip.foot]); what a screen reader calls it ([PanelGrip.said]); and that the control
 * :app lays over the photo -- the tab at the bottom centre of its touch box, the dash in the tab's middle -- is the
 * rule's. :app is compiled only by CI, and nothing has been seen on a phone; that the screen CALLS these is read as
 * source in MeasureScreenTest.
 *
 * AND, SINCE THE REVIEW OF F3 (§24.8): (2) what F3b left with no caller is GONE, and the tests of it with it -- the
 * grip's place in SurveyPrepTouch.hit's order, PanelGrip.hit, PanelGrip.open and the `avoid` boxes of `at`; each case
 * that was retired or re-pointed says so where it stood. (3) THE PRESS IS JUDGED BY HOW FAR THE FINGER MOVED, never by
 * where the grip stands. (4) A SECOND FINGER on the photo hides the grip and loses a press on it ([PanelGrip.twoDown]).
 * (5) THE GRIP KEEPS CLEAR ONLY OF HANDLES THAT ARE DRAWN: a held item's.
 *
 * (Test names are plain ASCII: MeasureWalkTest's header says why.)
 */
class PanelGripTest {

    private val dp = 2.0
    private val w = 348 * dp; private val h = 514 * dp
    private val tol = 16 * dp
    private val en = Tx.EN

    /** A box given in dp. */
    private fun box(l: Double, t: Double, r: Double, b: Double) = Box(l * dp, t * dp, r * dp, b * dp)
    private fun pt(x: Double, y: Double) = doubleArrayOf(x * dp, y * dp)
    private fun dpOf(b: Box) = listOf(b.l / dp, b.t / dp, b.r / dp, b.b / dp)
    private fun gap(a: Box, b: Box): Double = hypot(max(0.0, max(a.l - b.r, b.l - a.r)), max(0.0, max(a.t - b.b, b.t - a.b)))
    private fun tabs(bb: Box) = SurveyPrepTouch.handlesOf(bb, false)

    /** Every px of a box's grid, [step] px apart, its edges included. */
    private fun grid(b: Box, step: Double = dp): List<DoubleArray> {
        val out = ArrayList<DoubleArray>()
        var y = b.t
        while (y <= b.b + 1e-9) { var x = b.l; while (x <= b.r + 1e-9) { out += doubleArrayOf(x, y); x += step }; y += step }
        return out
    }

    private val L = 3319.0; private val W = 2678.0; private val H = 2677.0

    /** A wall with one of everything: a door, a Note, a door born a wish, a Tape, a Line, an area and a chain. */
    private fun wall(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        p.surface("front").marks.add(SurveyPrep.Mark("d1", "D", "FD1", 300.0, 1200.0, 0.0, 2100.0))
        p.surface("front").marks.add(SurveyPrep.Mark("n1", MarkTypes.NOTE, "FNOTE1", 100.0, 150.0, 1200.0, 1250.0, mapOf(MarkLife.KEY_NOTE to "check the lintel")))
        p.surface("front").marks.add(SurveyPrep.Mark("d2", "D", "FD2", 1500.0, 2400.0, 0.0, 2100.0,
            mapOf(MarkLife.KEY_BORN to MarkLife.WISH, MarkLife.KEY_LIFE to MarkLife.WISH)))
        p.addMeas("front", SurveyMeas.TAPE, listOf(doubleArrayOf(0.0, 1000.0), doubleArrayOf(1234.0, 1000.0)), "t1")
        p.addMeas("front", SurveyMeas.LINE, listOf(doubleArrayOf(500.0, 1600.0), doubleArrayOf(1300.0, 2200.0)), "m1")
        p.addMeas("front", SurveyMeas.AREA, listOf(doubleArrayOf(1800.0, 300.0), doubleArrayOf(2400.0, 300.0), doubleArrayOf(2400.0, 800.0)), "m2")
        p.addChain("front", "x", listOf(doubleArrayOf(300.0, 2400.0), doubleArrayOf(1200.0, 2400.0)), listOf(null, null), "k1")
        return p
    }

    private val ITEMS = listOf("d1", "n1", "d2", "t1", "m1", "m2", "k1")
    private val SEL = SurveyPrepPalette.SEL

    private fun shows(p: SurveyPrep.Prep, pick: Pick.State, step: String = StepRoute.MARK, tool: String = SEL, panelUp: Boolean = false,
                      keypadUp: Boolean = false, padUp: Boolean = false, dragging: Boolean = false) =
        PanelGrip.shows(p, pick, step, tool, panelUp, keypadUp, padUp, dragging)

    // ------------------------------------------------------------------ when it is there

    @Test fun `it is shown while an item is picked in 2 - Mark and its panel may open - held or not, gold or not`() {
        val p = wall()
        for (id in ITEMS) for (held in listOf(true, false)) for (gold in listOf(null, "FD1.w")) {
            assertTrue(MarkSheet.panelOpens(p, id), id)
            assertTrue(shows(p, Pick.State(id, held, gold)), "$id held $held gold $gold")
        }
        // it is the picked item's way to its panel, so it follows what the panel does: every item, never the reference
        // rectangle, never an id that is gone
        assertFalse(shows(p, Pick.NONE), "nothing picked")
        assertFalse(shows(p, Pick.State(null, false, "FD1.w")), "a figure gold with nothing picked")
        assertFalse(shows(p, Pick.State(SurveyRef.id("front"), true, null)), "the reference rectangle has no panel")
        assertFalse(shows(p, Pick.State("gone", true, null)), "an item that is gone")
        assertTrue(p.deleteMark("t1")); assertFalse(shows(p, Pick.State("t1", false, null)), "deleted under the pick")
    }

    @Test fun `it is not shown on another step, with a tool armed, with the panel, the keypad or Fine move's pad up, or while an item is dragged`() {
        val p = wall(); val st = Pick.State("d1", true, null)
        assertTrue(shows(p, st))
        // 2 - Mark alone: on 1 - Lines there is no item to pick, and on the 360 the panel opens by a long press only
        for (step in listOf(StepRoute.LINES, StepRoute.PANO, "measure", "plan", "")) assertFalse(shows(p, st, step = step), step)
        assertEquals(listOf(StepRoute.MARK), StepRoute.ROUTE.filter { shows(p, st, step = it) })
        // a tool armed: a tap places, and the chip that names the picked item is not drawn either
        for (tool in listOf("D", "W", "N", SurveyPrepPalette.LINE, SurveyPrepPalette.TAPE, SurveyPrepPalette.CHAIN)) assertFalse(shows(p, st, tool = tool), tool)
        // the panel already up; the keypad up; Fine move's pad up; a grab moving the held item
        assertFalse(shows(p, st, panelUp = true)); assertFalse(shows(p, st, keypadUp = true)); assertFalse(shows(p, st, padUp = true))
        assertFalse(shows(p, st, dragging = true))
        // each alone is enough, and none of them is needed for it to show
        for (a in listOf(true, false)) for (b in listOf(true, false)) for (c in listOf(true, false)) for (d in listOf(true, false))
            assertEquals(!a && !b && !c && !d, shows(p, st, panelUp = a, keypadUp = b, padUp = c, dragging = d), "$a $b $c $d")
        // at every quote stage, the unread one too: the stage is not asked -- after Survey the panel is still the way
        // to a mark's state and its note (the chip is drawn at every stage too)
        for (stage in listOf(0, 1, 2, 3, 4, 5, null)) assertTrue(MarkSheet.panel(en, p, MarkSheet.openPanel(p, "d2", 0L)!!, stage, null) != null, "at $stage")
    }

    // ------------------------------------------------------------------ what it is, and where it stands

    @Test fun `the numbers - a 72 x 20 dp tab with a 36 x 4 dp dash, a 96 x 32 dp touch box, 16 dp clear, a 12 dp pull`() {
        assertEquals(listOf(72.0, 20.0, 36.0, 4.0), listOf(PanelGrip.TAB_W_DP, PanelGrip.TAB_H_DP, PanelGrip.DASH_W_DP, PanelGrip.DASH_H_DP))
        assertEquals(listOf(96.0, 32.0), listOf(PanelGrip.TOUCH_W_DP, PanelGrip.TOUCH_H_DP))
        assertEquals(16.0, PanelGrip.CLEAR_DP); assertEquals(SurveyPrepTouch.CHIP_CLEAR_DP, PanelGrip.CLEAR_DP, "a tab's own reach, as the chip keeps")
        assertEquals(12.0, PanelGrip.PULL_DP); assertEquals(4.0, PanelGrip.EDGE_DP); assertEquals(10.0, PanelGrip.TAB_RADIUS_DP)
        // a DARK tab and a LIGHT dash: the dash is far brighter than the tab it lies on
        fun luma(c: Int) = ((c shr 16) and 0xff) * 0.299 + ((c shr 8) and 0xff) * 0.587 + (c and 0xff) * 0.114
        assertTrue(luma(PanelGrip.TAB_COLOUR) < 40); assertTrue(luma(PanelGrip.DASH_COLOUR) > 200); assertTrue(PanelGrip.TAB_ALPHA in 0xC0..0xFF)
        // the touch box is no smaller than a finger wants, and holds the tab; the tab holds the dash
        assertTrue(PanelGrip.TOUCH_W_DP >= 96 && PanelGrip.TOUCH_H_DP >= 32)
        assertTrue(PanelGrip.TOUCH_W_DP >= PanelGrip.TAB_W_DP && PanelGrip.TOUCH_H_DP >= PanelGrip.TAB_H_DP)
        assertTrue(PanelGrip.TAB_W_DP >= PanelGrip.DASH_W_DP && PanelGrip.TAB_H_DP >= PanelGrip.DASH_H_DP)
    }

    @Test fun `with nothing in its way it stands centred at the foot of the pane - portrait, the brief's 360 dp, and landscape on the band`() {
        // THE MI A3'S PANE, 348 x 514 dp: the touch box 96 x 32 at the bottom centre, the tab 72 x 20 flush with the
        // foot, the dash 36 x 4 in the tab's middle
        val g = assertNotNull(PanelGrip.at(w, h, dp))
        assertEquals(listOf(126.0, 482.0, 222.0, 514.0), dpOf(g.touch)); assertEquals(listOf(138.0, 494.0, 210.0, 514.0), dpOf(g.tab))
        assertEquals(listOf(156.0, 502.0, 192.0, 506.0), dpOf(g.dash)); assertEquals(PanelGrip.CENTRE, g.how)
        // FLUSH ABOVE THE TOOL ROW: its foot is the pane's own bottom edge, and it takes nothing from the photo's height
        assertEquals(h, g.tab.b); assertEquals(h, g.touch.b)
        assertEquals(514, StepRoute.markPhotoDp(StepRoute.Under.TOOLS), "the photo keeps its height: nothing new stands in a row under it")
        assertEquals(listOf("NOTHING", "TOOLS", "KEYPAD"), StepRoute.Under.values().map { it.name }, "under the photo is what it was: no row for the grip")
        // the brief's 360 x 514 dp
        val b = assertNotNull(PanelGrip.at(360 * dp, 514 * dp, dp))
        assertEquals(listOf(132.0, 482.0, 228.0, 514.0), dpOf(b.touch)); assertEquals(listOf(144.0, 494.0, 216.0, 514.0), dpOf(b.tab)); assertEquals(PanelGrip.CENTRE, b.how)
        // LANDSCAPE, 436 x 302 dp: the phase band lies over the pane's foot, 20 dp, and the grip stands on it
        assertEquals(20.0, PanelGrip.footDp(true)); assertEquals(0.0, PanelGrip.footDp(false)); assertEquals(20.0, PanelGrip.LAND_BAND_DP)
        val l = assertNotNull(PanelGrip.at(436 * dp, 302 * dp, dp, foot = PanelGrip.footDp(true) * dp))
        assertEquals(listOf(170.0, 250.0, 266.0, 282.0), dpOf(l.touch)); assertEquals(listOf(182.0, 262.0, 254.0, 282.0), dpOf(l.tab))
        assertEquals(listOf(200.0, 270.0, 236.0, 274.0), dpOf(l.dash)); assertEquals(PanelGrip.CENTRE, l.how)
        // the band's height is the screen's own: the composable that draws it says 20 dp (SurveyPrepTools.kt, read as source)
        val tools = I18n.android.resolve("app/src/main/kotlin/com/malletcrafts/interiorestimate/SurveyPrepTools.kt").readText()
        val band = tools.substringAfter("internal fun SpPhaseBand(band: PhaseTools.Band, land: Boolean, modifier: Modifier = Modifier) {\n").lineSequence().first()
        assertEquals("Row(modifier.fillMaxWidth().height(${PanelGrip.LAND_BAND_DP.toInt()}.dp)", band.trim(), "the phase band's height")
        // every other density gives the same grip in dp
        for (d in listOf(1.0, 1.5, 2.75, 3.0)) assertEquals(dpOf(g.touch), PanelGrip.at(348 * d, 514 * d, d)!!.touch.let { listOf(it.l / d, it.t / d, it.r / d, it.b / d) }, "at $d px a dp")
    }

    @Test fun `it is the pane's - handles and boxes far from the foot do not move it, and it has no part of the photo's zoom or pan`() {
        val centre = dpOf(PanelGrip.at(w, h, dp)!!.touch)
        // a window in the middle of the wall, held: its tabs are nowhere near the foot
        assertEquals(centre, dpOf(PanelGrip.at(w, h, dp, tabs(box(120.0, 150.0, 240.0, 300.0)))!!.touch))
        // a handle exactly 16 dp over the touch box, and one 16 dp beside it, are clear: `at least` 16 dp
        assertEquals(centre, dpOf(PanelGrip.at(w, h, dp, listOf(pt(174.0, 466.0), pt(110.0, 500.0), pt(238.0, 500.0)))!!.touch))
        // ... and a hair nearer is not
        for (p in listOf(pt(174.0, 466.5), pt(110.5, 500.0), pt(237.5, 500.0))) assertFalse(PanelGrip.at(w, h, dp, listOf(p))!!.how == PanelGrip.CENTRE, "${p.toList()}")
        // a handle off the corner of the box is measured to the corner, not along one axis: 12 dp left and 12 dp up of
        // the top-left corner is 17 dp away, and clear; 11 and 11 is 15.6, and is not
        assertEquals(PanelGrip.CENTRE, PanelGrip.at(w, h, dp, listOf(pt(114.0, 470.0)))!!.how)
        assertFalse(PanelGrip.at(w, h, dp, listOf(pt(115.0, 471.0)))!!.how == PanelGrip.CENTRE)
        // the rule is handed the pane and what is drawn on it, canvas px -- no view, no zoom, no offset: the same pane
        // gives the same grip however the photo under it is zoomed or panned
        val src = I18n.android.resolve("pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/PanelGrip.kt").readText()
        // (the review of F3, finding 2, moved this pin: `avoid`, the boxes to keep clear of, went -- no caller handed one)
        assertTrue("    fun at(w: Double, h: Double, dp: Double, handles: List<DoubleArray> = emptyList(), foot: Double = 0.0): Place? {" in src,
            "the pane, what a press grabs the picked item by, the foot -- and nothing of the view")
    }

    @Test fun `a door's bottom tab at the bottom centre - the grip steps aside by the least slide that clears every handle`() {
        // a door standing a little LEFT of centre, its foot 6 dp over the pane's: its three bottom tabs lie in the grip's row
        val door = box(100.0, 300.0, 190.0, 508.0)
        val g = assertNotNull(PanelGrip.at(w, h, dp, tabs(door)))
        // its right-bottom tab is at x 190: the box's left edge goes 16 dp past it, and no further
        assertEquals(PanelGrip.RIGHT, g.how); assertEquals(listOf(206.0, 482.0, 302.0, 514.0), dpOf(g.touch))
        // a door a little RIGHT of centre: the grip goes left, to 16 dp short of its left-bottom tab at x 190
        val g2 = assertNotNull(PanelGrip.at(w, h, dp, tabs(box(190.0, 300.0, 280.0, 508.0))))
        assertEquals(PanelGrip.LEFT, g2.how); assertEquals(listOf(78.0, 482.0, 174.0, 514.0), dpOf(g2.touch))
        // it only slides ALONG the foot: the row it stands in, and its own shape, never change
        for (k in listOf(g, g2)) {
            assertEquals(482.0, k.touch.t / dp); assertEquals(514.0, k.touch.b / dp); assertEquals(96.0, k.touch.w / dp)
            assertEquals(listOf(12.0, 12.0), listOf((k.tab.l - k.touch.l) / dp, (k.touch.r - k.tab.r) / dp), "the tab in the middle of its box")
            assertEquals(listOf(18.0, 8.0), listOf((k.dash.l - k.tab.l) / dp, (k.dash.t - k.tab.t) / dp), "the dash in the middle of the tab")
        }
        // every handle of each door is at least 16 dp from its grip's box
        for ((bb, k) in listOf(door to g, box(190.0, 300.0, 280.0, 508.0) to g2))
            for (q in tabs(bb)) assertTrue(SurveyPrepTouch.boxDist(k.touch, q[0], q[1]) >= 16 * dp - 1e-6, "${q.toList()}")
        // a wide unit along the foot, its three bottom tabs 164 dp apart: the grip fits between two of them
        val unit = box(10.0, 380.0, 338.0, 508.0)
        val g3 = assertNotNull(PanelGrip.at(w, h, dp, tabs(unit)))
        for (q in tabs(unit)) assertTrue(SurveyPrepTouch.boxDist(g3.touch, q[0], q[1]) >= 16 * dp - 1e-6)
        // a mark whose corners are free: its four corners are its handles, wherever they stand
        val free = listOf(pt(120.0, 500.0), pt(230.0, 470.0), pt(230.0, 300.0), pt(120.0, 300.0))
        val g4 = assertNotNull(PanelGrip.at(w, h, dp, SurveyPrepTouch.handlesOf(box(120.0, 300.0, 230.0, 500.0), false, free)))
        for (q in free) assertTrue(SurveyPrepTouch.boxDist(g4.touch, q[0], q[1]) >= 16 * dp - 1e-6)
        assertFalse(g4.how == PanelGrip.CENTRE)
        // a picked spot near the foot: its middle is what a press grabs it by
        val g5 = assertNotNull(PanelGrip.at(w, h, dp, SurveyPrepTouch.handlesOf(box(170.0, 496.0, 178.0, 504.0), true)))
        assertTrue(SurveyPrepTouch.boxDist(g5.touch, 174 * dp, 500 * dp) >= 16 * dp - 1e-6); assertFalse(g5.how == PanelGrip.CENTRE)
    }

    @Test fun `a tie goes right - the thumb's side - and the answer is the same every time`() {
        // a door exactly in the middle of the wall: 16 dp past its right-bottom tab and 16 dp short of its left-bottom
        // tab are the same slide from the centre -- 109 dp
        val door = box(129.0, 300.0, 219.0, 508.0)
        val g = assertNotNull(PanelGrip.at(w, h, dp, tabs(door)))
        assertEquals(PanelGrip.RIGHT, g.how); assertEquals(listOf(235.0, 482.0, 331.0, 514.0), dpOf(g.touch))
        assertEquals(126.0 - 17.0, 235.0 - 126.0, "the left place, 17, is exactly as far")
        // one lone handle at the very centre of the foot: 64 dp either way -- right
        val one = assertNotNull(PanelGrip.at(w, h, dp, listOf(pt(174.0, 510.0))))
        assertEquals(PanelGrip.RIGHT, one.how); assertEquals(190.0, one.touch.l / dp)
        // in whatever order the handles are handed, and however often it is asked
        repeat(5) { i -> assertEquals(dpOf(g.touch), dpOf(PanelGrip.at(w, h, dp, tabs(door).let { if (i % 2 == 0) it else it.reversed() })!!.touch)) }
        // a hair off the middle, the nearer side wins -- left, when left is nearer
        assertEquals(PanelGrip.LEFT, PanelGrip.at(w, h, dp, listOf(pt(174.5, 510.0)))!!.how)
        assertEquals(PanelGrip.RIGHT, PanelGrip.at(w, h, dp, listOf(pt(173.5, 510.0)))!!.how)
        // as Fine move's pad: with nothing between its two corners, the right
        assertEquals(NudgePad.Corner.RIGHT, NudgePad.corner(null, w, h, dp))
    }

    @Test fun `never off the pane - and no grip at all where no place along the foot is clear`() {
        // a handle near the right end of the foot leaves the grip centred; one that would push it past the left edge
        // is answered from the other side instead
        val far = assertNotNull(PanelGrip.at(w, h, dp, listOf(pt(100.0, 510.0), pt(200.0, 510.0))))
        assertEquals(PanelGrip.RIGHT, far.how); assertEquals(216.0, far.touch.l / dp, "left would be -12, off the pane")
        // wherever handles stand, the box is 4 dp inside both sides
        var n = 0
        for (hx in 0..348 step 6) for (hx2 in listOf(-1, 60, 174, 290)) {
            val hs = listOf(pt(hx.toDouble(), 508.0)) + (if (hx2 < 0) emptyList() else listOf(pt(hx2.toDouble(), 500.0)))
            val g = PanelGrip.at(w, h, dp, hs) ?: continue
            assertTrue(g.touch.l >= 4 * dp - 1e-6 && g.touch.r <= w - 4 * dp + 1e-6, "$hx, $hx2: ${dpOf(g.touch)}")
            assertTrue(g.tab.l >= g.touch.l && g.tab.r <= g.touch.r)
            for (q in hs) assertTrue(SurveyPrepTouch.boxDist(g.touch, q[0], q[1]) >= 16 * dp - 1e-6, "$hx, $hx2")
            n++
        }
        assertTrue(n > 200, "$n places looked at")
        // AN AREA WITH A CORNER EVERY 60 dp ALONG THE FOOT: nowhere is 16 dp clear of them all -- no grip; the chip and a
        // long press still open the panel
        val corners = (0..5).map { pt(20.0 + 60 * it, 500.0) }
        assertNull(PanelGrip.at(w, h, dp, corners))
        // a pane narrower than the box and its two edges, or lower than the box: none
        assertNull(PanelGrip.at(103 * dp, h, dp)); assertNotNull(PanelGrip.at(104 * dp, h, dp))
        assertNull(PanelGrip.at(w, 31 * dp, dp)); assertNotNull(PanelGrip.at(w, 32 * dp, dp))
        assertNull(PanelGrip.at(w, 40 * dp, dp, foot = 20 * dp), "the band leaves it no room")
        // (RETIRED with the review of F3, finding 2: "no grip, no hit" asked PanelGrip.hit, which was
        //  `Place.touch.contains` under a second name and had no caller)
    }

    private fun overlap(a: Box, b: Box) = a.l < b.r && a.r > b.l && a.t < b.b && a.b > b.t

    // (RE-POINTED with the review of F3, finding 2. This was "it keeps 16 dp from the code chip and from Fine move's
    //  pad - boxes it is told of": it handed `at` boxes to AVOID, which no screen ever did -- slice F3b turned the order
    //  round, the grip is placed first and THE CHIP KEEPS OFF THE GRIP, and Fine move's pad hides the grip. `avoid` is
    //  gone from `at` and `onPhoto`. What is held here is the rule that is live: the chip, handed the grip's touch box
    //  among the boxes it keeps off, is never on the grip -- and where its only clear place is, there is no chip.)
    @Test fun `the code chip keeps off the grip, never the reverse - and an item whose only clear chip place is on the grip has no chip`() {
        // a tall mark touching the top of the photo, held: its chip would stand BELOW it, in the grip's row
        val bb = box(120.0, 0.0, 230.0, 440.0)
        val g = assertNotNull(PanelGrip.at(w, h, dp, tabs(bb)))
        assertEquals(PanelGrip.CENTRE, g.how, "its bottom tabs are 42 dp over the grip's row: the grip does not move for the mark, nor for its chip")
        val alone = assertNotNull(SurveyPrepTouch.codeChip(bb, tabs(bb), 40 * dp, w, h, dp))
        assertEquals("below", alone.how); assertTrue(overlap(alone.tap, g.touch), "untold, the chip would lie on the grip")
        // told of the grip -- the painter hands its touch box among the boxes the chip keeps off -- the chip takes its next place
        val kept = assertNotNull(SurveyPrepTouch.codeChip(bb, tabs(bb), 40 * dp, w, h, dp, avoid = listOf(g.touch)))
        assertEquals("left", kept.how); assertFalse(overlap(kept.tap, g.touch))
        for (q in tabs(bb)) assertTrue(SurveyPrepTouch.boxDist(kept.tap, q[0], q[1]) >= 16 * dp - 1e-6, "and still a tab's reach from every tab")
        // a chip above its mark, as most are, is nowhere near the foot: told or not, the same chip
        val up = box(120.0, 200.0, 230.0, 320.0)
        val gUp = assertNotNull(PanelGrip.at(w, h, dp, tabs(up)))
        assertEquals(dpOf(SurveyPrepTouch.codeChip(up, tabs(up), 40 * dp, w, h, dp)!!.tap), dpOf(SurveyPrepTouch.codeChip(up, tabs(up), 40 * dp, w, h, dp, avoid = listOf(gUp.touch))!!.tap))
        // AN ITEM WHOSE ONLY CLEAR CHIP PLACE IS ON THE GRIP HAS NO CHIP (decided, §24.7): an area whose first corner is
        // 42 dp over the foot's middle, its other corners where the chip would stand above, left and right of it
        val area = listOf(pt(174.0, 440.0), pt(213.0, 440.0), pt(174.0, 404.0), pt(135.0, 440.0))
        val ga = assertNotNull(PanelGrip.at(w, h, dp, area)); assertEquals(PanelGrip.CENTRE, ga.how)
        val only = assertNotNull(SurveyPrepTouch.codeChipOfLine(area, 60.0, w, h, dp))
        assertEquals("below", only.how); assertTrue(overlap(only.tap, ga.touch))
        assertNull(SurveyPrepTouch.codeChipOfLine(area, 60.0, w, h, dp, listOf(ga.touch)), "no chip: the grip opens the same panel")
        // ... and the grip is there for it, held or not: the panel has its way in
        assertNotNull(PanelGrip.at(w, h, dp)); assertNotNull(PanelGrip.at(w, h, dp, area))
        // FINE MOVE'S PAD in either bottom corner: the grip is not shown while it is up, so the two never stand together
        assertFalse(shows(wall(), Pick.State("d1", true, null), padUp = true))
        for (c in NudgePad.Corner.values()) assertTrue(NudgePad.box(c, w, h, dp).b > PanelGrip.at(w, h, dp)!!.touch.t, "$c: the pad reaches into the grip's row -- which is why it hides the grip")
    }

    @Test fun `what stands over the foot lifts it - and the handles it keeps clear of are then the ones up there`() {
        // 36 dp of note line over the foot: the grip stands on it
        val g = assertNotNull(PanelGrip.at(w, h, dp, foot = 36 * dp))
        assertEquals(listOf(126.0, 446.0, 222.0, 478.0), dpOf(g.touch)); assertEquals(478.0, g.tab.b / dp)
        // a handle at the pane's own foot is now 30 dp under it, and no longer in its way; one at 470 is
        assertEquals(PanelGrip.CENTRE, PanelGrip.at(w, h, dp, listOf(pt(174.0, 508.0)), foot = 36 * dp)!!.how)
        assertFalse(PanelGrip.at(w, h, dp, listOf(pt(174.0, 470.0)), foot = 36 * dp)!!.how == PanelGrip.CENTRE)
        // a foot below nothing is nothing
        assertEquals(dpOf(PanelGrip.at(w, h, dp)!!.touch), dpOf(PanelGrip.at(w, h, dp, foot = -5.0)!!.touch))
    }

    // ------------------------------------------------------------------ what lies under it

    private fun said(x: Hit?): String = when (x) {
        null -> "-"; is Hit.Grip -> "grip"; is Hit.Code -> "code"; is Hit.Tag -> "tag ${x.code}"; is Hit.Body -> "body"
        is Hit.Mark -> "mark ${x.id} ${x.part}"; is Hit.Node -> "node ${x.id} ${x.i}"
    }

    /** One source under android/ with its comments and the bodies of its string literals blanked: CODE alone. */
    private fun code(rel: String): String = LiteralScan.scan(I18n.android, rel).masked

    // (RETIRED with the review of F3, finding 2: "it beats a figure, a body and an empty tap inside its box - and a
    //  handle keeps its press". It handed SurveyPrepTouch.hit a `grip` box, which no screen ever did: on the screen the
    //  grip is a control laid over the photo, and a press on it never reaches `hit`. The parameter and its two branches
    //  are gone, and so is the order they kept. What is held in its place: `hit` never answers Hit.Grip, and the rules
    //  that had no caller are out of the sources -- while Hit.Grip itself stays, for the grip to hand the tap's function.)
    @Test fun `the grip has no place in the photo's press order - hit never answers it - and what had no caller is gone`() {
        val g = PanelGrip.at(w, h, dp)!!.touch
        val unit = SurveyPrepTouch.Drawn("u", 60 * dp, 300 * dp, 380 * dp, 514 * dp)
        val spot = SurveyPrepTouch.Drawn("e", 171 * dp, 177 * dp, 497 * dp, 503 * dp, spot = 3 * dp)
        val marks = listOf(unit, SurveyPrepTouch.Drawn("win", 150 * dp, 240 * dp, 200 * dp, 320 * dp), spot)
        val tag = SurveyPrepTouch.Tag("FU1.w", box(150.0, 490.0, 200.0, 510.0))
        val chain = SurveyPrepTouch.Nodes("k", listOf(pt(100.0, 500.0), pt(180.0, 500.0)))
        var n = 0; var under = 0
        for (sel in listOf("u", "win", "e", null)) for (body in listOf(true, false)) for (q in grid(Box(0.0, 0.0, w, h), 6 * dp)) {
            val x = SurveyPrepTouch.hit(marks, sel, listOf(tag), q[0], q[1], tol, dp, nodes = listOf(chain), body = body, chip = box(20.0, 20.0, 70.0, 48.0))
            assertFalse(x is Hit.Grip, "$sel at ${q.toList()}: ${said(x)}"); n++
            // under the grip's box the photo's own order still finds what is there -- which is why the grip must take
            // the press itself, as a control over the photo, and why no HANDLE may lie under it (the next test)
            if (g.contains(q[0], q[1])) under++
        }
        assertTrue(n > 40_000 && under > 500, "$n presses, $under under the grip's box")
        // THE SOURCES: the photo's order has no grip, and the rules nobody called are gone -- not kept beside the live ones
        val touch = code("pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/SurveyPrepTouch.kt")
        val grip = code("pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/PanelGrip.kt")
        assertTrue("nodes: List<Nodes> = emptyList(), nodeR: Double = tol, body: Boolean = false, chip: Box? = null): Hit? {" in touch, "hit's last parameter is the chip")
        for (gone in listOf("grip: Box?", "onGrip", "return Hit.Grip")) assertFalse(gone in touch, "SurveyPrepTouch still has `$gone`")
        for (gone in listOf("fun hit(", "fun open(", "avoid", "Pick.options")) assertFalse(gone in grip, "PanelGrip still has `$gone`")
        // Hit.Grip stays, with its case in Pick.tap: it is what the grip hands the tap's one function
        assertTrue("object Grip : Hit()" in touch)
        assertTrue("is SurveyPrepTouch.Hit.Code, is SurveyPrepTouch.Hit.Grip -> options(st)" in code("pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/Pick.kt"))
        // and :app hands the photo's hit test no grip, nor asks a rule that is gone (it is compiled only by CI)
        val app = code("app/src/main/kotlin/com/malletcrafts/interiorestimate/SurveyPrepScreen.kt")
        for (gone in listOf("grip = ", "PanelGrip.hit(", "PanelGrip.open(", "avoid = ")) assertFalse(gone in app, "the screen still has `$gone`")
    }

    // (RE-POINTED with the review of F3, finding 2: this asked `hit(..., grip = …)` to answer "grip" at every px of the
    //  box, and PanelGrip.hit. The rule it is there for is live and is asked of the photo's own order now: as `at`
    //  places the grip, NO PRESS IN ITS BOX IS WITHIN A HANDLE'S REACH -- the control over the photo takes every press in
    //  its box, so a handle under it could never be dragged.)
    @Test fun `as the rule places it no press in its box is within a handle's reach - the held item's handles never lie under it`() {
        // a held door, in the middle, left and right of it; a held unit along the foot; a held mark with four free
        // corners; a held chain along the foot -- and the grip where `at` puts it for each
        val cases = ArrayList<Triple<String, List<SurveyPrepTouch.Drawn>, List<SurveyPrepTouch.Nodes>>>()
        for ((name, bb) in listOf("door mid" to box(129.0, 300.0, 219.0, 508.0), "door left" to box(100.0, 300.0, 190.0, 508.0),
                "door right" to box(190.0, 300.0, 280.0, 508.0), "unit" to box(10.0, 380.0, 338.0, 508.0), "low" to box(140.0, 470.0, 210.0, 512.0)))
            cases += Triple(name, listOf(SurveyPrepTouch.Drawn("m", bb.l, bb.r, bb.t, bb.b)), emptyList())
        val free = listOf(pt(120.0, 500.0), pt(230.0, 470.0), pt(230.0, 300.0), pt(120.0, 300.0))
        cases += Triple("free", listOf(SurveyPrepTouch.Drawn("m", 120 * dp, 230 * dp, 300 * dp, 500 * dp, corners = free)), emptyList())
        cases += Triple("chain", emptyList(), listOf(SurveyPrepTouch.Nodes("m", listOf(pt(40.0, 505.0), pt(170.0, 505.0), pt(300.0, 505.0)))))
        var presses = 0
        for ((name, marks, nodes) in cases) {
            val handles = marks.flatMap { m -> SurveyPrepTouch.handlesOf(Box(m.x0, m.yT, m.x1, m.yB), false, m.corners) } + nodes.flatMap { it.pts }
            val g = assertNotNull(PanelGrip.at(w, h, dp, handles), name)
            for (q in handles) assertTrue(SurveyPrepTouch.boxDist(g.touch, q[0], q[1]) >= tol - 1e-6, "$name: ${q.toList()}")
            // every px of its box, its edges included, with the item held: what the photo's order finds there is the
            // item's body or nothing -- never a tab, a corner or a chain's point
            for (q in grid(g.touch)) {
                val x = SurveyPrepTouch.hit(marks, "m".takeIf { marks.isNotEmpty() }, emptyList(), q[0], q[1], tol, dp, nodes = nodes)
                assertTrue(x == null || (x is Hit.Mark && x.part == "body"), "$name at ${q.toList()}: ${said(x)}"); presses++
            }
        }
        assertTrue(presses > 20_000, "$presses presses")
        // ... and it is the 16 dp that does it: the same doors with the grip left at the centre have a tab in its box
        val door = SurveyPrepTouch.Drawn("m", 129 * dp, 219 * dp, 300 * dp, 508 * dp)
        val mid = PanelGrip.at(w, h, dp)!!.touch
        assertTrue(grid(mid).any { q -> (SurveyPrepTouch.hit(listOf(door), "m", emptyList(), q[0], q[1], tol, dp) as? Hit.Mark)?.part == "b" }, "centred, the grip would lie on the door's bottom tab")
    }

    // ------------------------------------------------------------------ the press, and what it opens

    private val bw = PanelGrip.TOUCH_W_DP * dp; private val bh = PanelGrip.TOUCH_H_DP * dp

    // (MOVED with the review of F3, finding 3: `press` was handed the grip's Place, where the finger went down and where
    //  it is now, all on the pane -- and the screen read "where it is now" against where the grip stood at the press,
    //  in a frame that moves when the grip is re-placed. It is handed where the finger went down IN THE BOX and HOW FAR
    //  IT HAS MOVED since. Every case of the old test is here, said in the new terms; none is dropped.)
    @Test fun `the press - a tap opens, a rise of 12 dp opens at once, and a slip off the grip does nothing`() {
        // the finger goes down in the middle of the box, 22 dp from its top (the tab's own middle)
        val x = 48 * dp; val y = 22 * dp
        fun press(dx: Double, dy: Double, lifted: Boolean, fromX: Double = x, fromY: Double = y) = PanelGrip.press(fromX, fromY, dx * dp, dy * dp, lifted, second = false, dp)
        // A TAP: down and up where it went down; and up a few dp away, still on the box
        assertEquals(Press.WAIT, press(0.0, 0.0, false)); assertEquals(Press.OPEN, press(0.0, 0.0, true))
        assertEquals(Press.OPEN, press(30.0, -6.0, true)); assertEquals(Press.OPEN, press(-40.0, 8.0, true))
        // a long hold is a slow tap: nothing while it is down, open when it lifts
        repeat(3) { assertEquals(Press.WAIT, press(0.0, 0.0, false)) }
        // A RISE: 12 dp or more from where it went down opens AT ONCE, the finger still down -- 11.5 dp does not
        assertEquals(Press.WAIT, press(0.0, -11.5, false)); assertEquals(Press.OPEN, press(0.0, -12.0, false))
        assertEquals(Press.OPEN, press(0.0, -60.0, false)); assertEquals(Press.OPEN, press(0.0, -12.0, true))
        // ... however far sideways it wandered, and from anywhere in the box
        assertEquals(Press.OPEN, press(70.0, -14.0, false)); assertEquals(Press.OPEN, press(-70.0, -200.0, true))
        assertEquals(Press.OPEN, press(0.0, -12.0, false, fromX = 0.0, fromY = 0.0))
        assertEquals(Press.OPEN, press(0.0, -12.0, false, fromX = bw, fromY = bh))
        // A SLIP: sideways off the grip, or down off the pane -- nothing while it is down, nothing when it lifts
        assertEquals(Press.WAIT, press(80.0, 0.0, false)); assertEquals(Press.LOST, press(80.0, 0.0, true))
        assertEquals(Press.LOST, press(-80.0, 2.0, true)); assertEquals(Press.LOST, press(0.0, 14.0, true), "22 + 14 dp is under the box's foot")
        // a pull DOWN is no pull: lifted on the box it is a tap, off it a slip
        assertEquals(Press.WAIT, press(0.0, 9.0, false)); assertEquals(Press.OPEN, press(0.0, 9.0, true))
        // up 8 dp from the top of the box and lifted there: neither a tap (it is off the box) nor a pull
        assertEquals(Press.LOST, press(0.0, -8.0, true, fromY = 0.0))
        // the box's own edges are on it; a px past them is not
        for ((fx, fy) in listOf(0.0 to 0.0, bw to 0.0, 0.0 to bh, bw to bh)) assertEquals(Press.OPEN, press(0.0, 0.0, true, fromX = fx, fromY = fy))
        assertEquals(Press.LOST, PanelGrip.press(x, y, bw - x + 1, 0.0, true, second = false, dp)); assertEquals(Press.LOST, PanelGrip.press(x, y, -x - 1, 0.0, true, second = false, dp))
        // density: the 12 dp are dp, and so is the box
        assertEquals(Press.WAIT, PanelGrip.press(100.0, 60.0, 0.0, -35.0, false, second = false, 3.0)); assertEquals(Press.OPEN, PanelGrip.press(100.0, 60.0, 0.0, -36.0, false, second = false, 3.0))
        assertEquals(Press.OPEN, PanelGrip.press(280.0, 90.0, 0.0, 0.0, true, second = false, 3.0), "96 x 32 dp at 3 px a dp is 288 x 96 px"); assertEquals(Press.LOST, PanelGrip.press(280.0, 90.0, 9.0, 0.0, true, second = false, 3.0))
        assertEquals(listOf("WAIT", "OPEN", "LOST"), Press.values().map { it.name })
    }

    @Test fun `the press is judged by how far the finger moved, never by where the grip stands - a grip re-placed under a resting finger changes nothing`() {
        // THE FAULT (the review of F3, finding 3). The screen read the finger's place inside the control, which is a
        // place RELATIVE TO WHERE THE CONTROL STANDS NOW, and added where the grip stood WHEN THE FINGER WENT DOWN. A note
        // line or the snackbar coming under a resting finger lifts the grip -- 36 dp, say -- so the finger, which has not
        // moved, read 36 dp LOWER in the control: a tap lost. A note going drops the grip, and the still finger read
        // 36 dp HIGHER: the panel opened with no pull. The rule is handed no place on the pane at all now.
        val src = I18n.android.resolve("pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/PanelGrip.kt").readText()
        assertTrue("    fun press(downX: Double, downY: Double, dx: Double, dy: Double, lifted: Boolean, second: Boolean, dp: Double): Press = when {" in src,
            "where it went down in the box, how far it has moved, lifted, a second finger, the density -- and no Place")
        // a finger that has not moved, wherever the grip is by now: nothing while it is down, a tap when it lifts
        for (fy in listOf(0.0, 10.0, 22.0, 32.0)) for (fx in listOf(0.0, 48.0, 96.0)) {
            assertEquals(Press.WAIT, PanelGrip.press(fx * dp, fy * dp, 0.0, 0.0, false, second = false, dp), "$fx, $fy: resting")
            assertEquals(Press.OPEN, PanelGrip.press(fx * dp, fy * dp, 0.0, 0.0, true, second = false, dp), "$fx, $fy: lifted where it rested")
        }
        // what the old feeding made of the same still finger: the control re-placed 36 dp up or down read as the finger
        // 36 dp down or up -- said here as a move of that size, which is what the rule would have been handed
        assertEquals(Press.LOST, PanelGrip.press(48 * dp, 22 * dp, 0.0, 36 * dp, true, second = false, dp), "the grip lifted by a note: the tap was lost")
        assertEquals(Press.OPEN, PanelGrip.press(48 * dp, 22 * dp, 0.0, -36 * dp, false, second = false, dp), "the note gone: the panel opened with no pull")
        // WHAT THE SCREEN FEEDS IS THE SUM OF THE FINGER'S OWN MOVES: each event's place less its place before, both read
        // as the control stands at that event, so the control's own move is in neither. Three events of a real pull,
        // with the grip re-placed 36 dp between the first and the second:
        var dx = 0.0; var dy = 0.0
        val seen = ArrayList<Press>()
        for ((prev, now) in listOf((48.0 to 22.0) to (49.0 to 18.0), (49.0 to 54.0) to (50.0 to 49.0), (50.0 to 49.0) to (50.0 to 45.0))) {
            dx += (now.first - prev.first) * dp; dy += (now.second - prev.second) * dp
            seen += PanelGrip.press(48 * dp, 22 * dp, dx, dy, false, second = false, dp)
        }
        assertEquals(listOf(Press.WAIT, Press.WAIT, Press.OPEN), seen, "4, then 9, then 13 dp of rise -- the 36 dp the grip itself moved is in none of them")
        // A PRESS THE CONTROL IS HANDED FROM JUST OUTSIDE ITS BOX IS NOT THE GRIP'S (a control 32 dp high is handed the 8 dp
        // over it, by the system's minimum touch target): `onBox` of where it went down says so, the control leaves it
        // untouched, and it is the photo's. The box is the rule's 96 x 32 dp, its edges included -- which is the box `at`
        // keeps 16 dp from every handle and the painter keeps the chip and the plates off.
        for ((fx, fy) in listOf(0.0 to 0.0, 96.0 to 0.0, 0.0 to 32.0, 96.0 to 32.0, 48.0 to 16.0)) assertTrue(PanelGrip.onBox(fx * dp, fy * dp, dp), "$fx, $fy")
        for ((fx, fy) in listOf(48.0 to -0.5, 48.0 to -8.0, 48.0 to 32.5, -0.5 to 16.0, 96.5 to 16.0)) assertFalse(PanelGrip.onBox(fx * dp, fy * dp, dp), "$fx, $fy is off it")
        assertTrue(PanelGrip.onBox(288.0, 96.0, 3.0)); assertFalse(PanelGrip.onBox(289.0, 96.0, 3.0), "the box is dp: 288 x 96 px at 3 px a dp")
        // it is the Place's touch box, said in the control's own frame -- wherever the grip stands
        for (g in listOf(PanelGrip.at(w, h, dp)!!, PanelGrip.at(w, h, dp, listOf(pt(174.0, 510.0)))!!, PanelGrip.at(w, h, dp, foot = 96.0)!!)) {
            for (q in grid(g.touch, 8 * dp)) assertTrue(PanelGrip.onBox(q[0] - g.touch.l, q[1] - g.touch.t, dp), "${q.toList()}")
            for (q in listOf(doubleArrayOf(g.touch.l - 1, g.touch.t), doubleArrayOf(g.touch.r + 1, g.touch.b), doubleArrayOf(g.touch.l, g.touch.t - 1), doubleArrayOf(g.touch.r, g.touch.b + 1)))
                assertFalse(PanelGrip.onBox(q[0] - g.touch.l, q[1] - g.touch.t, dp), "${q.toList()} is off it")
        }
        // a handle `at` leaves a tab's reach over the box keeps its WHOLE reach: the press that lands 8 dp over the box,
        // straight under that handle, is 8 dp from it -- the handle's, and not the grip's
        val over = PanelGrip.at(w, h, dp, listOf(pt(174.0, 466.0)))!!
        assertEquals(PanelGrip.CENTRE, over.how); assertEquals(16.0, (over.touch.t - 466 * dp) / dp)
        assertFalse(PanelGrip.onBox(174 * dp - over.touch.l, 474 * dp - over.touch.t, dp), "8 dp over the box: not the grip's")
        assertEquals("mark m b", said(SurveyPrepTouch.hit(listOf(SurveyPrepTouch.Drawn("m", 129 * dp, 219 * dp, 300 * dp, 466 * dp)), "m", emptyList(), 174 * dp, 474 * dp, tol, dp)), "the photo's order gives it to the tab")
        // ... and were the rule asked about such a press all the same, a lift where it went down is no tap
        assertEquals(Press.LOST, PanelGrip.press(48 * dp, -6 * dp, 0.0, 0.0, true, second = false, dp))
    }

    @Test fun `a second finger on the photo hides the grip and loses a press on it - whichever finger went down first`() {
        // THE COUNT (the review of F3, finding 4): the screen counts the fingers on the photo's box at every event, the
        // grip's own among them, and keeps one answer -- two or more: yes; none: no; one: what it was
        assertFalse(PanelGrip.twoDown(false, 0)); assertFalse(PanelGrip.twoDown(false, 1)); assertTrue(PanelGrip.twoDown(false, 2)); assertTrue(PanelGrip.twoDown(false, 3))
        assertTrue(PanelGrip.twoDown(true, 2)); assertTrue(PanelGrip.twoDown(true, 1), "the finger left behind is the end of a pinch, not a new press"); assertFalse(PanelGrip.twoDown(true, 0))
        // a whole touch, event by event: one finger, a second, the first lifts, the last lifts, a new touch
        var two = false
        val said = listOf(1, 1, 2, 2, 1, 1, 0, 1, 0).map { n -> two = PanelGrip.twoDown(two, n); two }
        assertEquals(listOf(false, false, true, true, true, true, false, false, false), said)
        assertFalse(PanelGrip.twoDown(true, -1), "a count below nothing is nothing")
        // WHILE IT IS YES THE GRIP IS NOT SHOWN -- the screen hands it as `dragging`, with the magnifier
        val p = wall(); val st = Pick.State("d1", true, null)
        assertTrue(shows(p, st, dragging = PanelGrip.twoDown(false, 1))); assertFalse(shows(p, st, dragging = PanelGrip.twoDown(false, 2)))
        assertFalse(shows(p, st, dragging = PanelGrip.twoDown(true, 1)), "still hidden while the last finger of the pinch is down")
        // ... AND A PRESS ALREADY ON IT IS LOST, whatever else is true of it: resting, risen 12 dp or 60, lifted on the
        // box or off it -- with the first finger on the grip the photo's own gesture never starts, so nothing else can say it
        for (lifted in listOf(true, false)) for (rise in listOf(0.0, 11.0, 12.0, 60.0)) for (side in listOf(0.0, 30.0, 90.0))
            assertEquals(Press.LOST, PanelGrip.press(48 * dp, 22 * dp, side * dp, -rise * dp, lifted, second = true, dp), "lifted $lifted, risen $rise, $side sideways")
        // the same press with no second finger is what it was
        assertEquals(Press.OPEN, PanelGrip.press(48 * dp, 22 * dp, 0.0, -12 * dp, false, second = false, dp)); assertEquals(Press.OPEN, PanelGrip.press(48 * dp, 22 * dp, 0.0, 0.0, true, second = false, dp))
        assertEquals(Press.WAIT, PanelGrip.press(48 * dp, 22 * dp, 0.0, 0.0, false, second = false, dp))
        // the finger on the grip drifts up 12 dp while a second finger pinches: before the review that opened the panel
        var out = Press.WAIT; var twoNow = false
        for ((n, rise) in listOf(1 to 0.0, 1 to 4.0, 2 to 5.0, 2 to 13.0)) {
            twoNow = PanelGrip.twoDown(twoNow, n)
            if (out == Press.WAIT) out = PanelGrip.press(48 * dp, 22 * dp, 0.0, -rise * dp, false, second = twoNow, dp)
        }
        assertEquals(Press.LOST, out, "lost at the event the second finger came down in -- it never reaches the 13 dp")
    }

    @Test fun `it opens the panel exactly as the code chip does - one way in, and the panel's first 300 ms take no tap`() {
        val p = wall(); val sc = Pick.Scene(p, 0, StepRoute.MARK) { s -> SurveyPrep.size(s, L, W, H) }
        for (id in ITEMS) for (held in listOf(true, false)) for (gold in listOf(null, "FD1.w")) {
            val st = Pick.State(id, held, gold)
            // (the review of F3, finding 2, took PanelGrip.open out of this test: it was Pick.options under a second name
            //  and nothing called it -- the grip's tap goes where the chip's does, through Pick.tap)
            val byChip = Pick.tap(sc, st, Hit.Code); val byGrip = Pick.tap(sc, st, Hit.Grip)
            // nothing picked, held or gold changes; the options open -- the same answer from both, and it is row 7's
            for (o in listOf(byChip, byGrip, Pick.options(st))) { assertTrue(o.options, "$st"); assertEquals(st, o.state, "$st"); assertEquals(Pick.Pad.KEEP, o.pad); assertNull(o.nudge) }
        }
        // nothing picked: nothing to open
        assertFalse(Pick.tap(sc, Pick.NONE, Hit.Grip).options); assertFalse(Pick.tap(sc, Pick.NONE, Hit.Code).options)
        // it is no item's body and no handle: Fine move's "part last touched" is not moved by it
        assertFalse(NudgePad.onBody(Hit.Grip, null))
        // THE ONE FUNCTION: the panel the grip opens is MarkSheet.openPanel's, as the chip's is (`longPress` false) --
        // and it takes no tap for its first 300 ms, whichever way it opened
        val now = 5_000L
        val byGrip = assertNotNull(MarkSheet.openPanel(p, "d2", now)); val byChip = assertNotNull(MarkSheet.openPanel(p, "d2", now, longPress = false))
        assertEquals(listOf(byChip.id, byChip.openedAt, byChip.stateFirst, byChip.said), listOf(byGrip.id, byGrip.openedAt, byGrip.stateFirst, byGrip.said))
        assertFalse(byGrip.stateFirst, "a wish opened from the grip opens as from the chip: half way")
        assertTrue(MarkSheet.openPanel(p, "d2", now, longPress = true)!!.stateFirst, "the long press is the one that opens a wish whole")
        assertEquals(300L, MarkSheet.DEAF_MS)
        for (t in listOf(0L, 1L, 150L, 299L)) assertFalse(MarkSheet.answers(byGrip, now + t), "$t ms after the grip opened it")
        assertTrue(MarkSheet.answers(byGrip, now + 300)); assertTrue(MarkSheet.answers(byGrip, now + 5_000))
        // what it shows is the chip's panel: the head, the acts with Delete last, the blocks
        val v = assertNotNull(MarkSheet.panel(en, p, MarkSheet.openPanel(p, "d1", now)!!, 0, null))
        assertEquals("FD1", v.head.code); assertEquals(SurveyPrepEdit.DELETE, v.acts.all.last().id)
        // the reference rectangle and an id that is gone have no panel, so the grip is not shown for them
        assertNull(MarkSheet.openPanel(p, SurveyRef.id("front"), now)); assertFalse(shows(p, Pick.State(SurveyRef.id("front"), false, null)))
    }

    // ------------------------------------------------------------------ on the editing photo (slice F3b)

    /** The wall across the pane's 348 dp, 2 px a dp, ITS FLOOR ON THE PANE'S FOOT (a face zoomed until its skirting is
     *  at the bottom edge -- where a door's bottom tabs and the grip meet): 1 mm is 696 / W px, y down. */
    private val k = w / W
    private val toPx: (Double, Double) -> DoubleArray = { x, y -> doubleArrayOf(x * k, h - y * k) }
    private val plain: (SurveyPrep.Mark) -> Boolean = { false }
    private fun pts(l: List<DoubleArray>?) = l?.map { q -> listOf(Math.round(q[0] * 1000) / 1000.0, Math.round(q[1] * 1000) / 1000.0) }

    private fun held(id: String) = Pick.State(id, true, null)
    private fun reached(id: String) = Pick.State(id, false, null)

    // (MOVED with the review of F3, finding 5. This was "the handles it keeps clear of are the picked item's own on
    //  this face - the list the painter hands the code chip": F3b kept the grip clear of every tab and corner of the
    //  picked item HELD OR NOT, so after Survey, for a locked mark and for one the walk reached -- none of which shows a
    //  handle or answers a press on one -- the grip stepped aside, or went, for nothing. The list is what the screen
    //  DRAWS now: a held item's. Every shape of the old test is here for the held item; the not-held half is new.)
    @Test fun `the handles it keeps clear of are the ones the picked item shows - a held item's, as the painter draws them - and none for one that is not held`() {
        val p = wall()
        // A HELD MARK: the eight tabs round its box as drawn -- its top-left is (x0, y1) on the canvas, y down
        val d1 = p.markById("d1")!!.second
        val a = toPx(d1.x0, d1.y1); val b = toPx(d1.x1, d1.y0)
        val bb = Box(a[0], a[1], b[0], b[1])
        assertEquals(pts(SurveyPrepTouch.handlesOf(bb, false)), pts(PanelGrip.handles(p, "front", held("d1"), plain, toPx)))
        assertEquals(8, PanelGrip.handles(p, "front", held("d1"), plain, toPx)!!.size)
        // ... whichever way the mapping runs: a mirrored canvas gives the same box, so the same tabs
        val mirrored: (Double, Double) -> DoubleArray = { x, y -> doubleArrayOf((W - x) * k, y * k) }
        val ma = mirrored(d1.x0, d1.y1); val mb = mirrored(d1.x1, d1.y0)
        assertEquals(pts(SurveyPrepTouch.handlesOf(Box(minOf(ma[0], mb[0]), minOf(ma[1], mb[1]), maxOf(ma[0], mb[0]), maxOf(ma[1], mb[1])), false)),
            pts(PanelGrip.handles(p, "front", held("d1"), plain, mirrored)))
        // ITS FOUR CORNERS where they are free -- by the one value the screen hands the painter and the press
        val freed = PanelGrip.handles(p, "front", held("d1"), { it.id == "d1" }, toPx)
        assertEquals(pts(Quad.corners(d1).map { q -> toPx(q[0], q[1]) }), pts(freed)); assertEquals(4, freed!!.size)
        assertEquals(pts(SurveyPrepTouch.handlesOf(bb, false, Quad.freeCorners(d1, true)!!.map { q -> toPx(q[0], q[1]) })), pts(freed))
        // A HELD SPOT -- a Note's pin: it shows no tab and is grabbed round its own middle; freeing it changes nothing
        val n1 = p.markById("n1")!!.second
        val mid = toPx((n1.x0 + n1.x1) / 2, (n1.y0 + n1.y1) / 2)
        for (f in listOf(plain, { _: SurveyPrep.Mark -> true })) assertEquals(pts(listOf(mid)), pts(PanelGrip.handles(p, "front", held("n1"), f, toPx)))
        // A HELD TAPE, LINE, AREA, CHAIN: its points, each an end a press drags
        for (id in listOf("t1", "m1", "m2")) assertEquals(pts(p.measById(id)!!.second.pts.map { q -> toPx(q[0], q[1]) }), pts(PanelGrip.handles(p, "front", held(id), plain, toPx)), id)
        assertEquals(pts(p.chainById("k1")!!.second.nodes.map { q -> toPx(q[0], q[1]) }), pts(PanelGrip.handles(p, "front", held("k1"), plain, toPx)))
        assertEquals(listOf(2, 2, 3, 2), listOf("t1", "m1", "m2", "k1").map { PanelGrip.handles(p, "front", held(it), plain, toPx)!!.size })
        // PICKED BUT NOT HELD -- a mark the walk reached, any item after a reading landed: NONE. An EMPTY list, not null:
        // the grip is there, and nothing along the foot is in its way
        for (id in ITEMS) for (f in listOf(plain, { _: SurveyPrep.Mark -> true })) {
            assertEquals(emptyList(), PanelGrip.handles(p, "front", reached(id), f, toPx), id)
            assertEquals(emptyList(), PanelGrip.handles(p, "front", Pick.State(id, false, "FD1.w"), f, toPx), "$id, with a figure gold")
        }
        // IT IS THE RULE THE PAINTER DRAWS BY AND THE PRESS IS HANDED ITS ITEM BY: Pick.handles -- for every item, any state
        for (id in ITEMS) for (st in listOf(held(id), reached(id), Pick.State("d2", true, null), Pick.NONE)) {
            val hs = PanelGrip.handles(p, "front", st, plain, toPx)
            if (st.picked == null) assertNull(hs) else assertEquals(Pick.handles(st, st.picked!!), hs!!.isNotEmpty(), "$st")
            assertEquals(Pick.handleId(st) != null, hs?.isNotEmpty() == true, "$st: handles exactly where a press is handed the item")
        }
        // A LOCKED MARK shows none, though something calls it held: the painter's own `!locked` (a locked item is never
        // held -- Pick.mayHold -- so this is the rule not depending on that)
        assertEquals(true, SurveyPrepEdit.setLocked(p, "d1", true))
        assertEquals(emptyList(), PanelGrip.handles(p, "front", held("d1"), plain, toPx)); assertEquals(emptyList(), PanelGrip.handles(p, "front", held("d1"), { true }, toPx))
        assertEquals(Quad.Handles.NONE, Quad.handles(d1, Pick.handles(held("d1"), "d1") && !SurveyPrep.isLocked(d1), false), "what the painter asks for the same mark")
        assertEquals(false, SurveyPrepEdit.setLocked(p, "d1", false))
        assertEquals(8, PanelGrip.handles(p, "front", held("d1"), plain, toPx)!!.size)
        // AFTER SURVEY NOTHING SURVEYED IS HELD (Pick.mayHold: the phase), so a tap on the door picks it and holds nothing:
        // no handle, and the grip keeps clear of none. At Survey the same tap holds it, and its eight tabs are back.
        for (stage in listOf(1, 2, 3, 4, 5)) {
            val sc = Pick.Scene(p, stage, StepRoute.MARK) { s -> SurveyPrep.size(s, L, W, H) }
            val st = Pick.body(sc, Pick.NONE, "d1").state
            assertEquals(Pick.State("d1", false, null), st, "at $stage"); assertEquals(emptyList(), PanelGrip.handles(p, "front", st, plain, toPx), "at $stage")
        }
        val survey = Pick.body(Pick.Scene(p, 0, StepRoute.MARK) { s -> SurveyPrep.size(s, L, W, H) }, Pick.NONE, "d1").state
        assertTrue(survey.held); assertEquals(8, PanelGrip.handles(p, "front", survey, plain, toPx)!!.size)
        // NULL -- NO GRIP: nothing picked, an item of ANOTHER face (the pick can name one; no chip is drawn then either),
        // an id that is gone, the reference rectangle, a face that is not there -- held or not
        assertNull(PanelGrip.handles(p, "front", Pick.NONE, plain, toPx)); assertNull(PanelGrip.handles(p, "front", Pick.State(null, true, null), plain, toPx))
        for (id in ITEMS) for (st in listOf(held(id), reached(id))) assertNull(PanelGrip.handles(p, "back", st, plain, toPx), "$id is not on the back wall")
        for (st in listOf(held("gone"), reached("gone"), held(SurveyRef.id("front")), reached(SurveyRef.id("front")))) assertNull(PanelGrip.handles(p, "front", st, plain, toPx), "$st")
        assertNull(PanelGrip.handles(p, "nowhere", held("d1"), plain, toPx))
        // every item of the wall answers on its own face, held or not: the grip has a list for each
        for (id in ITEMS) for (st in listOf(held(id), reached(id))) assertNotNull(PanelGrip.handles(p, "front", st, plain, toPx), "$st")
        // (THE PAINTER'S OWN CONDITION is read as source where the screen is: MeasureScreenTest, beside the grip's pins)
    }

    @Test fun `what stands over the foot lifts it by the taller of the band and the notes - never by their sum`() {
        // portrait: nothing, until a note or the snackbar is drawn there -- then by that column's own height
        assertEquals(0.0, PanelGrip.foot(false, dp)); assertEquals(0.0, PanelGrip.foot(false, dp, 0.0))
        assertEquals(96.0, PanelGrip.foot(false, dp, 96.0)); assertEquals(0.0, PanelGrip.foot(false, dp, -5.0), "a height is never negative")
        // landscape: the phase band's 20 dp; a note stands OVER the band, so the taller of the two
        assertEquals(40.0, PanelGrip.foot(true, dp)); assertEquals(40.0, PanelGrip.foot(true, dp, 30.0)); assertEquals(96.0, PanelGrip.foot(true, dp, 96.0))
        assertEquals(PanelGrip.footDp(true) * 3.0, PanelGrip.foot(true, 3.0))
        // the grip then stands that far over the pane's foot, its shape unchanged
        val g = assertNotNull(PanelGrip.at(w, h, dp, foot = PanelGrip.foot(false, dp, 96.0)))
        assertEquals(listOf(126.0, 434.0, 222.0, 466.0), dpOf(g.touch)); assertEquals(h - 96.0, g.tab.b)
    }

    private fun onPhoto(p: SurveyPrep.Prep, pick: Pick.State, step: String = StepRoute.MARK, tool: String = SEL, panelUp: Boolean = false,
                        keypadUp: Boolean = false, padUp: Boolean = false, dragging: Boolean = false, s: String = "front",
                        free: (SurveyPrep.Mark) -> Boolean = plain, foot: Double = 0.0) =
        PanelGrip.onPhoto(p, pick, step, tool, panelUp, keypadUp, padUp, dragging, s, free, toPx, w, h, dp, foot)

    @Test fun `the one question the screen asks - shown, on this face, and a clear place - and the chip is then kept off it`() {
        val p = wall()
        // EVERY ITEM OF THE WALL, picked -- held or not: there is a grip, at the foot, and it is at(...)'s answer for the
        // handles the item SHOWS
        for (id in ITEMS) for (isHeld in listOf(true, false)) {
            val st = Pick.State(id, isHeld, null)
            val g = assertNotNull(onPhoto(p, st), "$id held $isHeld")
            val want = assertNotNull(PanelGrip.at(w, h, dp, PanelGrip.handles(p, "front", st, plain, toPx)!!))
            assertEquals(dpOf(want.touch), dpOf(g.touch), id); assertEquals(want.how, g.how, id); assertEquals(h, g.touch.b)
            for (q in PanelGrip.handles(p, "front", st, plain, toPx)!!) assertTrue(SurveyPrepTouch.boxDist(g.touch, q[0], q[1]) >= 16 * dp - 1e-6, "$id ${q.toList()}")
            // (the review of F3, finding 5) NOT HELD, it shows no handle: the grip is CENTRED, whatever the item is
            if (!isHeld) assertEquals(PanelGrip.CENTRE, g.how, "$id, not held")
        }
        // the door stands on the floor left of the middle, its bottom tabs on the foot: HELD, its grip has stepped aside;
        // the Line high on the wall leaves it centred
        assertFalse(onPhoto(p, Pick.State("d1", true, null))!!.how == PanelGrip.CENTRE); assertEquals(PanelGrip.CENTRE, onPhoto(p, Pick.State("m1", true, null))!!.how)
        // ... and the SAME door picked but not held -- the walk reached it, or the phase holds it read-only: no tab is
        // drawn and none takes a press, so the grip stands in the middle, on the door's foot. Before the review it stood aside.
        assertEquals(PanelGrip.CENTRE, onPhoto(p, Pick.State("d1", false, null))!!.how); assertEquals(PanelGrip.CENTRE, onPhoto(p, Pick.State("d1", false, "FD1.w"))!!.how)
        // AN AREA WITH A CORNER EVERY 60 dp ALONG THE FOOT: held, no place is clear of its corners and there is no grip;
        // not held, it has one -- before the review it had none either way, for corners nobody could drag
        val fence = SurveyPrep.fresh(L, W, H)
        fence.addMeas("front", SurveyMeas.AREA, (0..5).map { i -> doubleArrayOf((20.0 + 60 * i) * dp / k, 14 * dp / k) }, "a1")
        assertNull(PanelGrip.onPhoto(fence, Pick.State("a1", true, null), StepRoute.MARK, SEL, false, false, false, false, "front", plain, toPx, w, h, dp))
        assertEquals(PanelGrip.CENTRE, assertNotNull(PanelGrip.onPhoto(fence, Pick.State("a1", false, null), StepRoute.MARK, SEL, false, false, false, false, "front", plain, toPx, w, h, dp)).how)
        // NONE where shows() says none: nothing picked, another step, a tool armed, the panel, the keypad or the pad up, a drag
        val d = Pick.State("d1", true, null)
        assertNull(onPhoto(p, Pick.NONE)); assertNull(onPhoto(p, d, step = StepRoute.LINES)); assertNull(onPhoto(p, d, step = StepRoute.PANO))
        assertNull(onPhoto(p, d, tool = SurveyPrepPalette.LINE)); assertNull(onPhoto(p, d, tool = "D"))
        assertNull(onPhoto(p, d, panelUp = true)); assertNull(onPhoto(p, d, keypadUp = true)); assertNull(onPhoto(p, d, padUp = true)); assertNull(onPhoto(p, d, dragging = true))
        // NONE for an item of another face, though it is picked and its panel could open -- no chip is drawn for it either
        assertTrue(shows(p, d)); assertNull(onPhoto(p, d, s = "back")); assertNull(onPhoto(p, Pick.State(SurveyRef.id("front"), false, null)))
        // the foot it is handed lifts it; a pane too small has none
        assertEquals(h - 96.0, onPhoto(p, Pick.State("m1", true, null), foot = 96.0)!!.touch.b)
        assertNull(PanelGrip.onPhoto(p, d, StepRoute.MARK, SEL, false, false, false, false, "front", plain, toPx, 1.0, 1.0, dp), "before the pane is measured")
        // A FREED mark, held: the grip keeps clear of its corners, not of tabs it no longer shows
        val freed = assertNotNull(onPhoto(p, d, free = { it.id == "d1" }))
        for (q in Quad.corners(p.markById("d1")!!.second).map { c -> toPx(c[0], c[1]) }) assertTrue(SurveyPrepTouch.boxDist(freed.touch, q[0], q[1]) >= 16 * dp - 1e-6)
        // THE GRIP IS PLACED FIRST, THE CHIP AFTER IT: handed the grip's touch box among the boxes it keeps off, the chip
        // of a mark low on the wall never lies on the grip -- with it or without it the chip is still placed
        val low = SurveyPrep.fresh(L, W, H)
        low.surface("front").marks.add(SurveyPrep.Mark("s1", "D", "FD1", 1100.0, 1600.0, 250.0, 700.0))
        val gl = assertNotNull(PanelGrip.onPhoto(low, Pick.State("s1", true, null), StepRoute.MARK, SEL, false, false, false, false, "front", plain, toPx, w, h, dp))
        val m = low.markById("s1")!!.second
        val a = toPx(m.x0, m.y1); val b = toPx(m.x1, m.y0); val bb = Box(a[0], a[1], b[0], b[1])
        val tabs = SurveyPrepTouch.handlesOf(bb, false)
        for (lift in listOf(16 * dp, 60 * dp)) for (tw in listOf(40.0, 90.0, 140.0)) {
            val chip = assertNotNull(SurveyPrepTouch.codeChip(bb, tabs, tw, w, h, dp, lift, listOf(gl.touch)))
            assertFalse(chip.tap.l < gl.touch.r && chip.tap.r > gl.touch.l && chip.tap.t < gl.touch.b && chip.tap.b > gl.touch.t, "the chip's tap box overlaps the grip's")
        }
        // ... and a Line whose first point is just over the foot's middle: its chip would stand BELOW that point, on the
        // grip's row -- told of the grip, it stands somewhere else
        val line = listOf(pt(174.0, 440.0), pt(174.0, 200.0))
        val g2 = assertNotNull(PanelGrip.at(w, h, dp, line)); assertEquals(PanelGrip.CENTRE, g2.how)
        val alone = assertNotNull(SurveyPrepTouch.codeChipOfLine(line, 60.0, w, h, dp))
        assertTrue(alone.tap.l < g2.touch.r && alone.tap.r > g2.touch.l && alone.tap.t < g2.touch.b && alone.tap.b > g2.touch.t, "untold, the chip would lie on the grip")
        val kept = assertNotNull(SurveyPrepTouch.codeChipOfLine(line, 60.0, w, h, dp, listOf(g2.touch)))
        assertFalse(kept.tap.l < g2.touch.r && kept.tap.r > g2.touch.l && kept.tap.t < g2.touch.b && kept.tap.b > g2.touch.t)
    }

    @Test fun `a screen reader calls it what it calls the panel it opens - Options for the picked item's code - with no text of its own`() {
        val p = wall()
        for ((id, code) in listOf("d1" to "FD1", "t1" to "F-T1", "m1" to "F-M1", "m2" to "F-M2", "k1" to "FK1", "n1" to "FNOTE1")) {
            for (held in listOf(true, false)) assertEquals("Options for $code", PanelGrip.said(en, p, Pick.State(id, held, null)), id)
            // it IS the panel's own name, in both languages: the key the code chip and the panel already have
            for (tx in listOf(Tx.EN, Tx.MR)) {
                assertEquals(MarkSheet.optionsLabel(tx, code), PanelGrip.said(tx, p, Pick.State(id, true, null)))
                assertEquals(MarkSheet.panel(tx, p, MarkSheet.openPanel(p, id, 0L)!!, 0, null)!!.pane, PanelGrip.said(tx, p, Pick.State(id, true, null)), "$id: the panel it opens is called the same")
            }
        }
        assertEquals("", PanelGrip.said(en, p, Pick.NONE)); assertEquals("", PanelGrip.said(en, p, Pick.State("gone", true, null)))
    }

    @Test fun `as built it is a control over the photo - the tab at the bottom centre of its touch box, the dash in the tab's middle, by the rule's own numbers`() {
        // :app lays a box TOUCH_W x TOUCH_H at Place.touch and aligns the tab to its bottom centre and the dash to the
        // tab's centre: that is where Place.tab and Place.dash are, wherever the grip stands
        val door = box(100.0, 300.0, 190.0, 508.0)
        for (g in listOf(PanelGrip.at(w, h, dp)!!, PanelGrip.at(w, h, dp, tabs(door))!!, PanelGrip.at(436 * dp, 302 * dp, dp, foot = 40.0)!!, PanelGrip.at(w, h, dp, foot = 96.0)!!)) {
            assertEquals(PanelGrip.TOUCH_W_DP, g.touch.w / dp, 1e-9); assertEquals(PanelGrip.TOUCH_H_DP, g.touch.h / dp, 1e-9)
            assertEquals(PanelGrip.TAB_W_DP, g.tab.w / dp, 1e-9); assertEquals(PanelGrip.TAB_H_DP, g.tab.h / dp, 1e-9)
            assertEquals(PanelGrip.DASH_W_DP, g.dash.w / dp, 1e-9); assertEquals(PanelGrip.DASH_H_DP, g.dash.h / dp, 1e-9)
            assertEquals((g.touch.l + g.touch.r) / 2, (g.tab.l + g.tab.r) / 2, 1e-9, "the tab: centred across its box"); assertEquals(g.touch.b, g.tab.b, 1e-9, "... and on its foot")
            assertEquals((g.tab.l + g.tab.r) / 2, (g.dash.l + g.dash.r) / 2, 1e-9); assertEquals((g.tab.t + g.tab.b) / 2, (g.dash.t + g.dash.b) / 2, 1e-9, "the dash: in the tab's middle")
        }
        // THE PRESS AS THE CONTROL FEEDS IT (the review of F3, finding 3, moved these lines: the control fed its own box's
        // coordinates PLUS where the box stood at the press, which is a frame that moves): where the finger went down in
        // the control's own box, and the sum of its moves since -- a tap in the box's corner opens, a rise of 12 dp from
        // anywhere in it opens while the finger is down, a slip off it does not; where the grip stands is not asked
        val downX = 3 * dp; val downY = 30 * dp
        assertEquals(Press.OPEN, PanelGrip.press(downX, downY, 0.0, 0.0, lifted = true, second = false, dp))
        assertEquals(Press.WAIT, PanelGrip.press(downX, downY, 0.0, -11 * dp, lifted = false, second = false, dp))
        assertEquals(Press.OPEN, PanelGrip.press(downX, downY, 37 * dp, -12 * dp, lifted = false, second = false, dp), "risen 12 dp: at once")
        assertEquals(Press.LOST, PanelGrip.press(downX, downY, -23 * dp, 0.0, lifted = true, second = false, dp))
        // the box the control is laid as IS the rule's touch box, so "on the box" in the control's own frame is the Place's
        for (g in listOf(PanelGrip.at(w, h, dp)!!, PanelGrip.at(w, h, dp, tabs(door))!!)) {
            assertTrue(PanelGrip.onBox(0.0, 0.0, dp) && PanelGrip.onBox(g.touch.w, g.touch.h, dp)); assertFalse(PanelGrip.onBox(g.touch.w + 1, g.touch.h, dp))
        }
        // and what it then hands the tap's one function is the grip's own hit, which Pick answers as it answers the chip
        val p = wall(); val sc = Pick.Scene(p, 0, StepRoute.MARK) { s -> SurveyPrep.size(s, L, W, H) }
        val st = Pick.State("d1", false, "FD1.w")
        assertEquals(Pick.tap(sc, st, Hit.Code).options, Pick.tap(sc, st, Hit.Grip).options); assertTrue(Pick.tap(sc, st, Hit.Grip).options)
        assertEquals(st, Pick.tap(sc, st, Hit.Grip).state, "nothing picked, held or gold changes")
    }
}
