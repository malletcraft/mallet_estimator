package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch.Box
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * THE PANEL'S GRIP (port 63 slice F3a, 23 Nov 2026; DESIGN-imagemeter §24). Amit, with build 0.3.350 on his phone:
 * "options like unlock duplicate etc are coming when i tap the mark type very hard. there should be a simple pull out
 * dash which will let me pull up those optios." The panel that slides up (slice I2) opened from the picked item's
 * small code chip, or by a long press on the item -- and the long press is the only one he found.
 *
 * So whenever an item is PICKED in 2 · Mark and its panel may open, A GRIP STANDS AT THE FOOT OF THE PHOTO PANE, on
 * the photo, flush above the tool row: a dark rounded tab, [TAB_W_DP] x [TAB_H_DP], with a light [DASH_W_DP] x
 * [DASH_H_DP] dash in it -- the handle every bottom sheet has -- and a touch box of [TOUCH_W_DP] x [TOUCH_H_DP]. A TAP
 * on it, or a PULL UP of [PULL_DP] or more that starts on it, opens the panel exactly as a tap on the chip does: ONE
 * way in for all three -- the chip, the long press at the lift, the grip (each is [Pick.options], row 7, and the panel
 * is [MarkSheet.openPanel]'s, so its first 300 ms take no tap whichever way it opened: [MarkSheet.answers]).
 *
 * NOTHING NEW STANDS IN A ROW UNDER THE PHOTO and the photo keeps its height: the grip floats on the photo. IT IS THE
 * PANE'S, NOT THE PHOTO'S: zoom and pan do not move it. Where it stands ALONG the foot is [at]: centred, unless that
 * is within [CLEAR_DP] of a handle the picked item SHOWS ([handles]: only a HELD item shows any) -- then the nearest
 * clear place to the left or the right; never off the pane.
 *
 * IT IS A CONTROL OF ITS OWN LAID OVER THE PHOTO (slice F3b, §24.7), where the screen lays the fit and grid buttons --
 * not drawn on the photo's canvas and not pressed through the photo's gesture, as slice F3a first had it. So it takes
 * its own press and NOTHING UNDER ITS BOX GETS ONE: a figure, a body, bare photo. (A mark that lies under it while
 * another item is picked is reached by tapping empty photo first, which lets the pick go and the grip with it: §24.7.)
 * A HANDLE OF THE HELD ITEM NEVER LIES UNDER IT -- that is what the [CLEAR_DP] rule is for. A screen reader finds it
 * as a button, "Options for FW1" ([said]). The screen asks ONE question as it composes the photo, [onPhoto] --
 * [shows], then the picked item's [handles], then [at] -- lays a box the size of the touch box at [Place.touch] with
 * the tab at its bottom centre and the dash in the tab's middle, which is where [Place.tab] and [Place.dash] are, and
 * hands [Place.touch] to the painter as a box THE CODE CHIP AND EVERY PLATE KEEP OFF: the chip keeps clear of the
 * grip, never the reverse (an item whose only clear chip place is on the grip gets no chip; the grip opens the same
 * panel). Its press is [press]'s; on [Press.OPEN] it hands [SurveyPrepTouch.Hit.Grip] to the very function a tap on
 * the photo is handed to, and [Pick.tap] answers [Pick.options] for it: the chip's own line then opens the panel.
 *
 * Pure geometry in canvas px, as [SurveyPrepTouch] is -- the photo's canvas IS the pane -- so every rule is tested here
 * (PanelGripTest); :app only asks and draws.
 *
 * THE REVIEW OF F3 (23 Nov 2026; §24.8) took out what F3b had left with no caller -- a second answer each: the grip's
 * place in [SurveyPrepTouch.hit]'s order (the photo's gesture never sees a press on it), `hit` (it was
 * `Place.touch.contains`), `open` (the tap goes through [Pick.tap]) and the `avoid` boxes of [at] (the screen hands
 * none) -- and changed three rules: the press is judged by HOW FAR THE FINGER MOVED, never by where the grip stands
 * ([press]), and is the grip's only if it went down ON THE BOX ([onBox]); a SECOND FINGER on the photo ends it
 * ([twoDown]); and the grip keeps clear only of handles that are DRAWN ([handles]).
 */
object PanelGrip {

    /** The tab as drawn, dp: dark, its top corners rounded [TAB_RADIUS_DP], its foot flush with the pane's. */
    const val TAB_W_DP = 72.0
    const val TAB_H_DP = 20.0
    const val TAB_RADIUS_DP = 10.0

    /** The dash in it, dp: light, centred, its ends round. */
    const val DASH_W_DP = 36.0
    const val DASH_H_DP = 4.0

    /** Its touch box, dp: the tab and 12 dp either side of it, 12 dp over it. */
    const val TOUCH_W_DP = 96.0
    const val TOUCH_H_DP = 32.0

    /** Its touch box stands this far from every handle the picked item shows, dp: the reach a tab is pressed within
     *  ([SurveyPrepTouch.hit]'s `tol`), the chip's own clearance ([SurveyPrepTouch.CHIP_CLEAR_DP]). */
    const val CLEAR_DP = SurveyPrepTouch.CHIP_CLEAR_DP

    /** ... and this far inside the pane's sides, dp. */
    const val EDGE_DP = 4.0

    /** A rise of this much from where the finger went down on it opens the panel, dp. */
    const val PULL_DP = 12.0

    /** In landscape the phase band lies over the foot of the photo (SpPhaseBand, 20 dp): the grip stands on it. */
    const val LAND_BAND_DP = 20.0

    /** The tab's dark (0xRRGGBB, at [TAB_ALPHA] of 255 -- the note line's own ground) and the dash's light. */
    const val TAB_COLOUR = 0x111311
    const val TAB_ALPHA = 0xE6
    const val DASH_COLOUR = 0xe6e9e2

    // ------------------------------------------------------------------ when it is there

    /**
     * WHETHER THE GRIP IS SHOWN: an item is PICKED ([pick]) in 2 · Mark ([step], [StepRoute.MARK]) and its panel may
     * open ([MarkSheet.panelOpens]: every item -- a mark, a Note, a Tape, a Line, an area, a chain; never the reference
     * rectangle, and not an id that is gone), and
     *  - NO TOOL IS ARMED ([tool], the tool that is on: [SurveyPrepPalette.armed]) -- with one on a tap places, and the
     *    chip that names the picked item is not drawn either: the two ways in are offered together or not at all;
     *  - THE PANEL IS NOT ALREADY UP ([panelUp]) -- it has its own grip, and this one would stand under it;
     *  - THE KEYPAD IS NOT UP ([keypadUp]) -- he is typing a figure, and the pane's foot is the keys' top edge;
     *  - FINE MOVE'S PAD IS NOT UP ([padUp]) -- the pad stands in the pane's bottom corner and is what he is using;
     *  - NO ITEM IS BEING DRAGGED ([dragging]: a grab is moving the held item, by its body or a handle) -- the grip
     *    would hop aside as a handle neared it; it is back, wherever it is then clear, when the finger lifts. The
     *    screen says the same while A SECOND FINGER IS ON THE PHOTO ([twoDown]): two fingers are a pinch, never a
     *    pull on the grip, whichever of them went down first.
     * Held or not, at every quote stage: after Survey the panel is still the way to a mark's state and note.
     */
    fun shows(p: SurveyPrep.Prep, pick: Pick.State, step: String, tool: String, panelUp: Boolean, keypadUp: Boolean,
              padUp: Boolean, dragging: Boolean = false): Boolean {
        val id = pick.picked ?: return false
        return step == StepRoute.MARK && !SurveyPrepPalette.armed(tool) && !panelUp && !keypadUp && !padUp && !dragging &&
            MarkSheet.panelOpens(p, id)
    }

    /** What stands over the foot of the photo pane and lifts the grip, dp: in landscape the phase band
     *  ([LAND_BAND_DP]); in portrait nothing -- the tool row is under the pane, and the grip is flush above it. A
     *  caller that draws anything else there (the note line, the snackbar) adds its height. */
    fun footDp(land: Boolean): Double = if (land) LAND_BAND_DP else 0.0

    // ------------------------------------------------------------------ where it stands

    /** Where the grip stands, canvas px: the [tab] and the [dash] as drawn; its [touch] box -- where :app lays the
     *  control that takes its press, and what the painter is told the code chip and every figure's plate keep off --
     *  and [how] it was placed: [CENTRE], [LEFT] or [RIGHT] of the centre. */
    class Place(val tab: Box, val dash: Box, val touch: Box, val how: String)

    const val CENTRE = "centre" // text:value which rule placed the grip, never shown
    const val LEFT = "left" // text:value which rule placed the grip, never shown
    const val RIGHT = "right" // text:value which rule placed the grip, never shown

    private const val EPS = 1e-6

    /**
     * WHERE THE GRIP STANDS on a [w] x [h] pane, [dp] px a dp. Its touch box lies along the pane's FOOT -- its bottom
     * the pane's bottom edge, less [foot] px where something stands over the foot ([footDp]) -- and:
     *  1. CENTRED, when there its touch box is at least [CLEAR_DP] from every one of [handles] (canvas px: what the
     *     picked item SHOWS and a press may grab it by -- [handles]: the held mark's tabs or free corners, a held
     *     spot's middle, a held Tape's, Line's, area's or chain's points; none for an item that is not held);
     *  2. else THE NEAREST CLEAR PLACE TO THE LEFT OR THE RIGHT along the foot -- the least slide from the centre that
     *     clears every handle. A TIE GOES RIGHT: the thumb's side, as Fine move's pad is ([NudgePad.corner]);
     *  3. never off the pane: its touch box stays [EDGE_DP] inside both sides.
     * NULL -- NO GRIP -- where no place along the foot is that clear, or the pane is narrower or shorter than the
     * touch box: the code chip and a long press still open the panel, and a grip within a handle's reach would take
     * the press that resizes the mark.
     *
     * NOTHING ELSE IS KEPT CLEAR OF. The item's BODY is not: the grip may lie on the picked item, a figure or bare
     * photo, and takes the press there. The CODE CHIP is not: the grip is placed first and the chip keeps off IT
     * ([onPhoto]). Nothing else that takes a press stands at the foot while the grip is shown (Fine move's pad hides
     * it: [shows]) -- so there is no list of boxes to avoid (the review of F3 took F3a's out: no caller handed one).
     */
    fun at(w: Double, h: Double, dp: Double, handles: List<DoubleArray> = emptyList(), foot: Double = 0.0): Place? {
        val tw = TOUCH_W_DP * dp; val th = TOUCH_H_DP * dp
        val edge = EDGE_DP * dp; val clear = CLEAR_DP * dp
        val bottom = h - max(0.0, foot); val top = bottom - th
        val lo = edge; val hi = w - tw - edge
        if (hi < lo || top < 0) return null
        val mid = (w - tw) / 2
        // what each handle forbids of the touch box's LEFT edge: an open interval
        val no = ArrayList<DoubleArray>()
        for (p in handles) {
            val dy = max(0.0, max(top - p[1], p[1] - bottom))
            if (dy >= clear) continue
            val dx = sqrt(clear * clear - dy * dy)
            no += doubleArrayOf(p[0] - dx - tw, p[0] + dx)
        }
        fun free(l: Double) = no.none { l > it[0] + EPS && l < it[1] - EPS }
        // the centre, else an end of a forbidden stretch: the nearest clear place to either side is always one of those
        var best: Double? = null
        for (c in listOf(mid) + no.flatMap { listOf(it[0], it[1]) }) {
            if (c < lo - EPS || c > hi + EPS || !free(c)) continue
            val l = c.coerceIn(lo, hi)
            val b = best
            if (b == null || abs(l - mid) < abs(b - mid) - EPS || (abs(abs(l - mid) - abs(b - mid)) <= EPS && l > b)) best = l
        }
        val l = best ?: return null
        val touch = Box(l, top, l + tw, bottom)
        val tl = l + (tw - TAB_W_DP * dp) / 2
        val tab = Box(tl, bottom - TAB_H_DP * dp, tl + TAB_W_DP * dp, bottom)
        val dl = tl + (TAB_W_DP - DASH_W_DP) * dp / 2; val dt = tab.t + (TAB_H_DP - DASH_H_DP) * dp / 2
        return Place(tab, Box(dl, dt, dl + DASH_W_DP * dp, dt + DASH_H_DP * dp), touch,
            if (abs(l - mid) <= EPS) CENTRE else if (l < mid) LEFT else RIGHT)
    }

    // ------------------------------------------------------------------ on the editing photo (slice F3b)

    /**
     * WHAT STANDS OVER THE FOOT OF THE PHOTO PANE, px -- [at]'s `foot` (port 63 slice F3b): the phase band in landscape
     * ([footDp]), and THE NOTE LINE AND THE SNACKBAR while one of them is drawn there ([notes]: the height of their
     * column as laid out, its own 8 dp margins included; 0 while neither shows). They are drawn over the same foot --
     * over the band, in landscape -- so the grip is lifted by the TALLER of the two, never by their sum: it stands
     * just above a note for the few seconds the note is there, and is back on the foot when it goes. (A note is a
     * `foot`, never something to keep clear of sideways: it is as wide as the foot.)
     */
    fun foot(land: Boolean, dp: Double, notes: Double = 0.0): Double = max(footDp(land) * dp, max(0.0, notes))

    /**
     * THE HANDLES THE PICKED ITEM SHOWS ON THE FACE [s], canvas px -- what [at] keeps the grip clear of. ONLY WHAT
     * THE SCREEN DRAWS AND A PRESS CAN GRAB (the review of F3, finding 5): an item shows its handles only while it is
     * HELD ([Pick.handles] -- the rule the painter draws by and the press is handed its `selId` by,
     * [Pick.handleId]), and a mark only while it is not locked, which is the condition the painter hands
     * [Quad.handles]. So:
     *  - a HELD MARK, not locked: its eight tabs round its box as drawn; its four corners where they are [free]
     *    ([Quad.freeCorners], by the screen's one value); a spot's own middle (a point, a Note's pin: it shows no tab,
     *    and is grabbed round its middle) -- [SurveyPrepTouch.handlesOf];
     *  - a HELD TAPE, LINE or AREA: its points; a HELD CHAIN: its points;
     *  - AN ITEM THAT IS PICKED BUT NOT HELD -- one the walk reached, a locked one, every mark after Survey, where
     *    2 · Mark holds nothing ([Pick.mayHold]): NONE, an empty list. It shows no handle and none of it takes a
     *    press, so the grip stands centred; before the review it kept clear of tabs that were not there, and near the
     *    foot stepped aside or went for nothing.
     * [toPx] maps face mm (x, y up) to the canvas, as the painter's own mapping does.
     * NULL: nothing is picked, or the item is not on this face -- the pick can name an item of another face, and the
     * chip is not drawn then either -- or it is gone, or it has no point at all. NO GRIP then ([onPhoto]).
     */
    fun handles(p: SurveyPrep.Prep, s: String, pick: Pick.State, free: (SurveyPrep.Mark) -> Boolean,
                toPx: (Double, Double) -> DoubleArray): List<DoubleArray>? {
        val sf = p.surfaces[s] ?: return null
        val id = pick.picked ?: return null
        val held = Pick.handles(pick, id)
        sf.marks.firstOrNull { it.id == id }?.let { e ->
            if (!held || SurveyPrep.isLocked(e)) return emptyList()
            val a = toPx(e.x0, e.y1); val b = toPx(e.x1, e.y0)
            val bb = Box(min(a[0], b[0]), min(a[1], b[1]), max(a[0], b[0]), max(a[1], b[1]))
            return SurveyPrepTouch.handlesOf(bb, SurveyPrep.isSpot(e.t), Quad.freeCorners(e, free(e))?.map { q -> toPx(q[0], q[1]) })
        }
        sf.meas.firstOrNull { it.id == id && it.pts.isNotEmpty() }?.let { m -> return if (held) m.pts.map { q -> toPx(q[0], q[1]) } else emptyList() }
        sf.chains.firstOrNull { it.id == id && it.nodes.isNotEmpty() }?.let { c -> return if (held) c.nodes.map { q -> toPx(q[0], q[1]) } else emptyList() }
        return null
    }

    /**
     * THE GRIP ON THE EDITING PHOTO NOW, OR NULL -- NONE (port 63 slice F3b): the ONE question the screen asks, each
     * time it composes the photo. It is there when [shows] says so AND the picked item is on the face on show ([s];
     * [handles]) AND a place along the foot is clear of the handles it shows ([at]): the pane [w] x [h] px, [dp] px a
     * dp, [foot] what stands over its foot ([foot]).
     *
     * THE GRIP IS PLACED FIRST AND THE CODE CHIP AFTER IT (slice F3b turned F3a's order round): this is asked where
     * the photo is COMPOSED, before it is drawn, and the chip is placed by the painter -- so the painter is handed the
     * grip's [Place.touch] among the boxes the chip and every figure's plate keep off (`keep`), and the two never
     * overlap. Where the grip stands on the chip's only clear place the item has no chip, and the grip is the way in.
     */
    fun onPhoto(p: SurveyPrep.Prep, pick: Pick.State, step: String, tool: String, panelUp: Boolean, keypadUp: Boolean,
                padUp: Boolean, dragging: Boolean, s: String, free: (SurveyPrep.Mark) -> Boolean,
                toPx: (Double, Double) -> DoubleArray, w: Double, h: Double, dp: Double, foot: Double = 0.0): Place? {
        if (!shows(p, pick, step, tool, panelUp, keypadUp, padUp, dragging)) return null
        val hs = handles(p, s, pick, free, toPx) ?: return null
        return at(w, h, dp, hs, foot)
    }

    /**
     * WHAT A SCREEN READER CALLS THE GRIP (port 63 slice F3b): what it calls the panel the grip opens, "Options for
     * FW1" -- [MarkSheet.optionsLabel] of the picked item's code ([PhotoFigures.nameOf]). No text of its own is keyed
     * for it. Empty with nothing picked, or its item gone: there is no grip then ([shows]).
     */
    fun said(tx: Tx, p: SurveyPrep.Prep, pick: Pick.State): String =
        PhotoFigures.nameOf(p, pick)?.let { code -> MarkSheet.optionsLabel(tx, code) }.orEmpty()

    // ------------------------------------------------------------------ the press

    /** What a finger that went down on the grip has done so far. */
    enum class Press {
        /** Nothing yet: it is still down, and has not risen far enough. */
        WAIT,
        /** THE PANEL OPENS: it rose [PULL_DP] or more, or lifted on the box -- a tap. */
        OPEN,
        /** Nothing happens: it lifted off the box without a pull up, or a second finger came down on the photo. The
         *  press was still the grip's -- it never panned the photo, picked what lay under it or let the pick go. */
        LOST,
    }

    /**
     * WHETHER A SECOND FINGER IS ON THE PHOTO IN THIS TOUCH (the review of F3, finding 4) -- what the screen keeps as
     * it counts the fingers pressed on the photo pane, the grip's own among them, at every pointer event: [was] its
     * answer before the event, [pressed] how many fingers are down in it.
     *  - TWO OR MORE: yes;
     *  - NONE: no -- the touch is over;
     *  - ONE: what it [was]. After a second finger there is no going back to one in the same touch: the finger left
     *    behind when the other lifts is the end of a pinch, not a new press.
     * While it is yes the grip is not shown ([shows]' `dragging`) and a press already on it is [Press.LOST] ([press]),
     * WHICHEVER FINGER WENT DOWN FIRST. The photo's own gesture cannot say this: with the first finger on the grip it
     * never starts (the grip took that finger), so it never sees the second.
     */
    fun twoDown(was: Boolean, pressed: Int): Boolean = if (pressed >= 2) true else if (pressed <= 0) false else was

    /**
     * WHETHER ([x], [y]) IS ON THE GRIP'S TOUCH BOX, px in the box's own frame: [TOUCH_W_DP] x [TOUCH_H_DP] from (0, 0),
     * its edges included. Asked twice: of where a finger WENT DOWN -- is this press the grip's at all -- and, through
     * [press], of where it lifted.
     *
     * A PRESS THAT WENT DOWN OFF THE BOX IS NOT THE GRIP'S, though the control was handed it. A control lower than a
     * finger's reach is handed the presses that land just over it (the system widens a small control's touch target
     * to its minimum, 48 dp: 8 dp over a box 32 dp high). [at] keeps the BOX [CLEAR_DP] from every handle, and the
     * painter keeps the chip and every plate off the BOX; were the grip to take what lands in those 8 dp, a handle
     * that stands a tab's reach over it would lose the lower half of that reach, and a tap on bare photo there would
     * do nothing at all. So the control leaves such a press untouched, and it is the photo's: the touch box is the
     * rule's box, by construction. (The review of F3, with finding 3: that the system hands the control those presses
     * is read from the toolkit's rule, not seen on a phone -- if it hands none, this never answers false at a press.)
     */
    fun onBox(x: Double, y: Double, dp: Double): Boolean = x >= 0.0 && y >= 0.0 && x <= TOUCH_W_DP * dp && y <= TOUCH_H_DP * dp

    /**
     * A PRESS THAT WENT DOWN ON THE GRIP, JUDGED BY HOW FAR THE FINGER HAS MOVED SINCE (the review of F3, finding 3).
     * [downX], [downY]: where it went down, px IN THE TOUCH BOX'S OWN FRAME -- (0, 0) its top-left AS IT STOOD THEN
     * ([onBox] of it: the control takes no other press);
     * [dx], [dy]: how far the finger has moved since, px, y down -- THE SUM OF ITS OWN MOVES, so nothing in this rule
     * knows where the grip stands NOW: a note line or the snackbar coming or going under a resting finger re-places
     * the grip, and before the review that read as the finger moving -- a tap lost, or the panel opened with no pull;
     * [lifted] or still down; [second]: a second finger is on the photo ([twoDown]); [dp] px a dp.
     *  - A SECOND FINGER: [Press.LOST], whatever else is true -- two fingers are a pinch;
     *  - RISEN [PULL_DP] dp or more from where it went down -- however far sideways: [Press.OPEN], AT ONCE, the
     *    finger still down, so the panel rises under the pull. The rest of that press is nobody's (the panel is its
     *    own window; the photo's gesture never saw the finger go down);
     *  - lifted ON THE BOX AS IT STOOD WHEN THE FINGER WENT DOWN ([onBox]): a TAP, [Press.OPEN];
     *  - lifted anywhere else -- it slid sideways off the grip, or down off the pane: [Press.LOST];
     *  - still down: [Press.WAIT]. A long hold is a slow tap: it opens when it lifts.
     */
    fun press(downX: Double, downY: Double, dx: Double, dy: Double, lifted: Boolean, second: Boolean, dp: Double): Press = when {
        second -> Press.LOST
        -dy >= PULL_DP * dp - EPS -> Press.OPEN
        !lifted -> Press.WAIT
        onBox(downX + dx, downY + dy, dp) -> Press.OPEN
        else -> Press.LOST
    }
}
