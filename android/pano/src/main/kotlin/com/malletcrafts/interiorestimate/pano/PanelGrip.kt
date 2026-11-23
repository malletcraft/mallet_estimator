package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch.Box
import kotlin.math.abs
import kotlin.math.max
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
 * on it, or a PULL UP of [PULL_DP] or more that starts in its box, opens the panel exactly as a tap on the chip does:
 * ONE way in for all three -- the chip, the long press at the lift, the grip ([open] is [Pick.options], row 7, and the
 * panel is [MarkSheet.openPanel]'s, so its first 300 ms take no tap whichever way it opened: [MarkSheet.answers]).
 *
 * NOTHING NEW STANDS IN A ROW UNDER THE PHOTO and the photo keeps its height: the grip floats on the photo. IT IS THE
 * PANE'S, NOT THE PHOTO'S: zoom and pan do not move it. Where it stands ALONG the foot is [at]: centred, unless that
 * is within [CLEAR_DP] of a handle of the picked item, of its code chip or of anything else it is told to keep clear
 * of -- then the nearest clear place to the left or the right; never off the pane.
 *
 * WHAT IT BEATS ([SurveyPrepTouch.hit], handed its touch box as `grip`): a figure, a body and an empty tap inside its
 * box. A handle of the held item never lies under it -- that is what the [CLEAR_DP] rule is for.
 *
 * Pure geometry in canvas px, as [SurveyPrepTouch] is -- the photo's canvas IS the pane -- so every rule is tested here
 * (PanelGripTest); :app draws [Place.tab] and [Place.dash], hands [Place.touch] to the press, and calls [press].
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

    /** Its touch box stands this far from every handle of the picked item, from its code chip and from whatever else
     *  it is told to keep clear of, dp: the reach a tab is pressed within ([SurveyPrepTouch.hit]'s `tol`), the chip's
     *  own clearance ([SurveyPrepTouch.CHIP_CLEAR_DP]). */
    const val CLEAR_DP = SurveyPrepTouch.CHIP_CLEAR_DP

    /** ... and this far inside the pane's sides, dp. */
    const val EDGE_DP = 4.0

    /** A pull up of this much from a press in its box opens the panel, dp. */
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
     *    would hop aside as a handle neared it; it is back, wherever it is then clear, when the finger lifts.
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

    /** Where the grip stands, canvas px: the [tab] and the [dash] as drawn, its [touch] box -- what
     *  [SurveyPrepTouch.hit] is handed as `grip`, and what the figures' layout is told to keep clear, as it is told
     *  the chip -- and [how] it was placed: [CENTRE], [LEFT] or [RIGHT] of the centre. */
    class Place(val tab: Box, val dash: Box, val touch: Box, val how: String)

    const val CENTRE = "centre" // text:value which rule placed the grip, never shown
    const val LEFT = "left" // text:value which rule placed the grip, never shown
    const val RIGHT = "right" // text:value which rule placed the grip, never shown

    private const val EPS = 1e-6

    /**
     * WHERE THE GRIP STANDS on a [w] x [h] pane, [dp] px a dp. Its touch box lies along the pane's FOOT -- its bottom
     * the pane's bottom edge, less [foot] px where something stands over the foot ([footDp]) -- and:
     *  1. CENTRED, when there its touch box is at least [CLEAR_DP] from every one of [handles] (canvas px: the picked
     *     item's tabs, free corners, a spot's middle -- [SurveyPrepTouch.handlesOf] -- or a Tape's, a Line's, an
     *     area's or a chain's points) and from every box of [avoid] (the code chip's tap box, [SurveyPrepTouch.Chip.tap];
     *     Fine move's pad, [NudgePad.box], should the two ever stand together; anything drawn over the photo that
     *     takes a press of its own);
     *  2. else THE NEAREST CLEAR PLACE TO THE LEFT OR THE RIGHT along the foot -- the least slide from the centre that
     *     clears everything. A TIE GOES RIGHT: the thumb's side, as Fine move's pad is ([NudgePad.corner]);
     *  3. never off the pane: its touch box stays [EDGE_DP] inside both sides.
     * NULL -- NO GRIP -- where no place along the foot is that clear, or the pane is narrower or shorter than the
     * touch box: the code chip and a long press still open the panel, and a grip within a handle's reach would take
     * the press that resizes the mark.
     *
     * THE ITEM'S BODY IS NOT KEPT CLEAR OF: the grip may lie on the picked item, a figure or bare photo, and beats
     * each ([SurveyPrepTouch.hit]).
     */
    fun at(w: Double, h: Double, dp: Double, handles: List<DoubleArray> = emptyList(), avoid: List<Box> = emptyList(),
           foot: Double = 0.0): Place? {
        val tw = TOUCH_W_DP * dp; val th = TOUCH_H_DP * dp
        val edge = EDGE_DP * dp; val clear = CLEAR_DP * dp
        val bottom = h - max(0.0, foot); val top = bottom - th
        val lo = edge; val hi = w - tw - edge
        if (hi < lo || top < 0) return null
        val mid = (w - tw) / 2
        // what each thing forbids of the touch box's LEFT edge: an open interval (a point is a box of no size)
        val no = ArrayList<DoubleArray>()
        fun forbid(l: Double, t: Double, r: Double, b: Double) {
            val dy = max(0.0, max(top - b, t - bottom))
            if (dy >= clear) return
            val dx = sqrt(clear * clear - dy * dy)
            no += doubleArrayOf(l - dx - tw, r + dx)
        }
        for (p in handles) forbid(p[0], p[1], p[0], p[1])
        for (b in avoid) forbid(b.l, b.t, b.r, b.b)
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

    /** Whether a press at ([x], [y]) is in the grip's touch box; false with no grip ([g] null). */
    fun hit(g: Place?, x: Double, y: Double): Boolean = g != null && g.touch.contains(x, y)

    // ------------------------------------------------------------------ the press

    /** What a finger that went down in the grip's box has done so far. */
    enum class Press {
        /** Nothing yet: it is still down, and has not pulled up far enough. */
        WAIT,
        /** THE PANEL OPENS: he pulled up [PULL_DP] or more, or lifted inside the box -- a tap. */
        OPEN,
        /** It lifted outside the box without a pull up: nothing happens. The press was still the grip's -- it never
         *  panned the photo, picked what lay under it or let the pick go. */
        LOST,
    }

    /**
     * A PRESS THAT STARTED IN THE GRIP'S BOX ([g]; [downY] where the finger went down), now at ([x], [y]), [lifted]
     * or still down:
     *  - PULLED UP [PULL_DP] dp or more from where it went down -- however far sideways: [Press.OPEN], AT ONCE, the
     *    finger still down, so the panel rises under the pull. The rest of that press is nobody's (the panel is its
     *    own window; the photo's gesture ends there);
     *  - lifted inside the box: a TAP, [Press.OPEN];
     *  - lifted anywhere else -- it slid sideways off the grip, or down off the pane: [Press.LOST];
     *  - still down: [Press.WAIT]. A long hold is a slow tap: it opens when it lifts.
     */
    fun press(g: Place, downY: Double, x: Double, y: Double, lifted: Boolean, dp: Double): Press = when {
        downY - y >= PULL_DP * dp - EPS -> Press.OPEN
        !lifted -> Press.WAIT
        g.touch.contains(x, y) -> Press.OPEN
        else -> Press.LOST
    }

    /**
     * THE GRIP OPENS THE PANEL EXACTLY AS THE CODE CHIP DOES: [Pick.options], row 7 -- nothing picked, held or gold
     * changes, and `options` is set while something is picked. The screen then opens the panel through the ONE
     * function the chip and the long press use ([MarkSheet.openPanel], `longPress` false as for the chip), so the
     * grip's panel is the chip's: half way in portrait, its first [MarkSheet.DEAF_MS] deaf.
     */
    fun open(st: Pick.State): Pick.Out = Pick.options(st)
}
