package com.malletcrafts.interiorestimate.pano

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * WHAT A FINGER ON THE SURVEY-PREP PHOTO TAKES, AND WHERE THE MAGNIFIER GOES
 * (prototype v52, items 14 and 15). Pure geometry in canvas pixels, so the
 * order and the placement are tested here; SpPhoto only measures and calls.
 *
 *  14 -- Amit, 13 Oct 2026 21:06 IST: "Check any mark tools handles , not
 *     every handle works as expected like center handles." The picked mark's
 *     width tag was drawn over its top-centre tab and the tags were tested
 *     first, so that tab never resized anything. Now the picked mark's eight
 *     tabs are tested FIRST, a tag under the same finger takes the touch only
 *     when its centre is nearer, and the width and height tags are drawn
 *     clear of the tabs.
 *  15 -- Amit, 13 Oct 2026 23:50 IST: "I can not move measure mark type
 *     handle. Many times magnifier is getting in between fingers and foto
 *     place." The magnifier sits above the finger; with no room above, in the
 *     top corner farthest from it -- never below the finger, never under it,
 *     and never under the buttons that sit on the photo.
 */
object SurveyPrepTouch {

    /** A rectangle, canvas px. */
    class Box(val l: Double, val t: Double, val r: Double, val b: Double) {
        val w get() = r - l
        val h get() = b - t
        fun contains(x: Double, y: Double) = x >= l && x <= r && y >= t && y <= b
        fun centreDist(x: Double, y: Double) = hypot(x - (l + r) / 2, y - (t + b) / 2)
        override fun toString() = "Box($l, $t, $r, $b)"
    }

    /** A number tag on the photo as a tap target: the measure it picks and its tap box. */
    class Tag(val code: String, val box: Box)

    // ------------------------------------------------------------ the tags

    /** How far the picked mark's w and h tags stand off its tabs: 8 of the drawing's units, and never under 6 dp. */
    fun clearance(u: Double, dp: Double) = max(8 * u, 6 * dp)

    /** Where a value tag is anchored: [x] its centre (or its left edge when [left]), [y] its text baseline. */
    class Anchor(val x: Double, val y: Double, val left: Boolean)

    /**
     * The width tag of a mark drawn from [px],[py] (top-left) to [qx],[qy]
     * (bottom-right), text size [fs]. [hand]: the mark is picked and shows its
     * tabs, so the tag stands [clearance] higher than it otherwise would.
     */
    fun widthTag(px: Double, py: Double, qx: Double, fs: Double, u: Double, dp: Double, hand: Boolean) =
        Anchor((px + qx) / 2, py - fs * 0.8 - (if (hand) clearance(u, dp) else 0.0), false)

    /** The height tag: on a picked mark it starts right of the edge by [clearance], left-aligned there. */
    fun heightTag(py: Double, qx: Double, qy: Double, fs: Double, u: Double, dp: Double, hand: Boolean) =
        if (hand) Anchor(qx + 6 * u + clearance(u, dp), (py + qy) / 2 + 4 * u, true)
        else Anchor(qx + 6 * u + fs * 3.4, (py + qy) / 2 + 4 * u, false)

    /** A value tag [w] wide at [a] on a [width] x [height] canvas: the box drawn, its baseline, and its tap box. */
    class TagShape(val x: Double, val baseline: Double, val drawn: Box, val tap: Box)

    fun tagShape(a: Anchor, w: Double, fs: Double, u: Double, width: Double, height: Double): TagShape {
        val x = max(2 * u, min(width - w - 2 * u, if (a.left) a.x else a.x - w / 2))
        val y = max(fs + 4 * u, min(height - 4 * u, a.y))
        return TagShape(x, y, Box(x, y - fs - 2 * u, x + w, y + 5 * u), Box(x - 6 * u, y - fs - 8 * u, x + w + 6 * u, y + 11 * u))
    }

    // ------------------------------------------------------------ the tabs

    /** The eight resize tabs, as the parts they move. */
    val TABS = listOf("l", "r", "t", "b", "lt", "rt", "lb", "rb")

    /** Where a tab sits on a mark drawn from [x0],[yT] to [x1],[yB]. */
    fun tabAt(part: String, x0: Double, x1: Double, yT: Double, yB: Double): DoubleArray {
        val x = when { 'l' in part -> x0; 'r' in part -> x1; else -> (x0 + x1) / 2 }
        val y = when { 't' in part -> yT; 'b' in part -> yB; else -> (yT + yB) / 2 }
        return doubleArrayOf(x, y)
    }

    /** The tab nearest ([x], [y]) within [tol], and its distance; null when none is. */
    fun nearestTab(x0: Double, x1: Double, yT: Double, yB: Double, x: Double, y: Double, tol: Double): Pair<String, Double>? =
        TABS.map { p -> val q = tabAt(p, x0, x1, yT, yB); p to hypot(x - q[0], y - q[1]) }
            .filter { it.second < tol }.minByOrNull { it.second }

    /** A tab under the finger keeps the touch unless a tag under it has its centre NEARER (then the tag takes it). */
    fun tabOrTag(tabDist: Double, tagCentreDist: Double?): Boolean = tagCentreDist == null || tagCentreDist >= tabDist

    /** A mark as drawn: its id and its box, canvas px. [spot]: for a point (E, P, F), the radius its 50 mm
     *  makes on screen (25 mm, canvas px, before the 6 dp floor it is drawn with); null for a box.
     *  [corners] (port 63 slice Q1; DESIGN-quad §3.4): for a mark whose corners are FREE ([Quad.free]), its four
     *  corners on the canvas, q0 … q3 -- bottom-left, bottom-right, top-right, top-left; null for every other mark. Such
     *  a mark is pressed by its four corners where a rectangle is by its eight tabs, and its body only INSIDE its shape. */
    class Drawn(val id: String, val x0: Double, val x1: Double, val yT: Double, val yB: Double, val spot: Double? = null,
                val corners: List<DoubleArray>? = null) {
        val cx get() = (x0 + x1) / 2
        val cy get() = (yT + yB) / 2
    }

    /** The free corner nearest ([x], [y]) within [tol], as the part it is (q0 … q3), and its distance; null when none is. */
    fun nearestCorner(corners: List<DoubleArray>, x: Double, y: Double, tol: Double): Pair<String, Double>? =
        corners.take(Quad.PARTS.size).mapIndexed { i, q -> Quad.PARTS[i] to hypot(x - q[0], y - q[1]) }
            .filter { it.second < tol }.minByOrNull { it.second }

    /** The handle of [m] nearest the finger within [tol]: one of its four free corners, else one of its eight tabs. */
    private fun nearestHandle(m: Drawn, x: Double, y: Double, tol: Double): Pair<String, Double>? =
        m.corners.let { c -> if (c != null) nearestCorner(c, x, y, tol) else nearestTab(m.x0, m.x1, m.yT, m.yB, x, y, tol) }

    /** How far from its centre a PICKED spot is grabbed: its 25 mm and 6 dp more, never under 12 dp. */
    fun spotReachPicked(spot: Double, dp: Double) = max(12 * dp, spot + 6 * dp)

    /** How far from its centre any other spot is picked: its 25 mm and half the tab reach more, never under the reach (16 dp). */
    fun spotReach(spot: Double, tol: Double) = max(tol, spot + tol * .5)

    /** What a finger down on 2 · Mark takes. */
    sealed class Hit {
        /** A mark, moved whole ("body") or by one tab. */
        class Mark(val id: String, val part: String) : Hit()
        /** A number tag: its measure becomes the one being taken. */
        class Tag(val code: String) : Hit()
        /** Point [i] of chain [id] (v52 slice 11): the chain is picked and the point moves along it. */
        class Node(val id: String, val i: Int) : Hit()
        /** The PICKED Tape's, Line's or area's body (port 63 slice F1): it moves whole ([SurveyGuide.bodyMove]). */
        object Body : Hit()
        /** THE PICKED ITEM'S CODE CHIP (port 63 slice I2a; DESIGN-imagemeter §2.2, §3): a tap on it opens the panel that
         *  slides up ([Pick.options], row 7). Where it stands is [codeChip]; it is the picked item's, whichever that
         *  is ([Pick.State.picked]), so it carries no id -- as [Body] carries none. */
        object Code : Hit()
    }

    /** A chain's points as drawn: its id and each point on the canvas, px (v52 slice 11). */
    class Nodes(val id: String, val pts: List<DoubleArray>)

    /**
     * THE HIT ORDER on 2 · Mark, [marks] in drawing order (the last on top),
     * [tags] as drawn, [tol] the tab reach (16 dp), [dp] px per dp:
     *  1. the PICKED mark's eight tabs -- unless a tag under the finger has
     *     its centre nearer than the tab ([tabOrTag]); a picked SPOT has no
     *     tabs and is grabbed whole within [spotReachPicked] of its centre,
     *     unless a tag under the finger has its centre nearer;
     *  2. a CHAIN POINT within [nodeR] (16 dp) of the finger, the nearest (v52 slice 11, sp.js hit: after the picked
     *     mark's tabs -- ct52 X5, "a chain point on a door's left tab resizes the door" -- and before any tag or mark);
     *  3. a tag under the finger, the nearest centre;
     *  4. THE PICKED TAPE'S, LINE'S OR AREA'S BODY when the finger is on it ([body]: the caller asks
     *     [SurveyGuide.bodyPress], which leaves its ends to the ends) -- port 63 slice F1; Amit, 16 Nov 2026: "tape
     *     mark does not move unless its end points are moved. difficult for moving it from one point to other." It is
     *     drawn over the marks and it is the thing picked, so it comes BEFORE them: a tape laid across a window is
     *     moved by its band, and the window by the rest of itself. A figure and a chain's point under the same finger
     *     keep the press, as they keep it from a mark;
     *  5. the marks, topmost first: a spot within [spotReach] of its centre,
     *     moved whole, never resized; the picked box within [tol] of its edge
     *     (a tab there still resizes), any other box only inside it.
     * Null: nothing, and the finger slides the photo.
     *
     * A MARK WHOSE CORNERS ARE FREE ([Drawn.corners]; port 63 slice Q1): picked, its four corners stand where the eight
     * tabs do in this order (step 1), each the part it drags (q0 … q3), a tag under the finger taking the touch only when
     * its centre is nearer; and among the marks (step 5) its body counts only INSIDE ITS SHAPE -- so the empty corner of
     * its bounding box no longer hides what is under it -- while a press on one of the picked one's corners still drags
     * that corner.
     *
     * WHERE THE TWO MEET (the merge of slices F1 and Q1): the picked Tape's body (step 4) comes before a four-corner
     * mark as before any mark -- a tape laid across a sloping window is moved by its band, and the window by the rest
     * of its SHAPE; the empty corner of its bounding box is neither. One thing is picked at a time, so [body] is never
     * handed with a picked mark; were it, that mark's own handles would still come first (step 1), as an end does.
     *
     * THE PICKED ITEM'S CODE CHIP ([chip]: its tap box as [codeChip] placed it, or null -- none is drawn; port 63 slice
     * I2a, DESIGN-imagemeter §3 "The hit order"): AFTER THE HANDLES AND BEFORE EVERYTHING ELSE. A HANDLE BEATS IT --
     * the held mark's tabs or corners (step 1) and a held chain's points (step 2) keep a press that is theirs, though
     * [codeChip] never puts the chip within their reach -- and IT BEATS A FIGURE AND A BODY: a tag under the same finger
     * (step 3), the picked Tape's band (step 4) and every mark (step 5), the picked SPOT's own middle included, which is
     * its body and no handle. Null, which is what every caller before slice I2 hands, changes nothing in this order.
     */
    fun hit(marks: List<Drawn>, selId: String?, tags: List<Tag>, x: Double, y: Double, tol: Double, dp: Double = tol / 16,
            nodes: List<Nodes> = emptyList(), nodeR: Double = tol, body: Boolean = false, chip: Box? = null): Hit? {
        val under = tags.filter { it.box.contains(x, y) }.minByOrNull { it.box.centreDist(x, y) }
        val onChip = chip != null && chip.contains(x, y)
        val sel = selId?.let { id -> marks.lastOrNull { it.id == id } }
        if (sel != null) {
            val sp = sel.spot
            if (sp != null) {
                // a spot has no handle: a press round its middle is its BODY, and the chip beats a body
                if (onChip) return Hit.Code
                val d = hypot(x - sel.cx, y - sel.cy)
                if (d <= spotReachPicked(sp, dp) && tabOrTag(d, under?.box?.centreDist(x, y))) return Hit.Mark(sel.id, "body")
            } else {
                val t = nearestHandle(sel, x, y, tol)
                if (t != null && tabOrTag(t.second, under?.box?.centreDist(x, y))) return Hit.Mark(sel.id, t.first)
            }
        }
        SurveyChain.nodeAt(nodes.map { it.id to it.pts }, x, y, nodeR)?.let { (id, i) -> return Hit.Node(id, i) }
        if (onChip) return Hit.Code
        if (under != null) return Hit.Tag(under.code)
        if (body) return Hit.Body
        for (m in marks.asReversed()) {
            val sp = m.spot
            if (sp != null) {
                if (hypot(x - m.cx, y - m.cy) <= spotReach(sp, tol)) return Hit.Mark(m.id, "body")
                continue
            }
            if (x < m.x0 - tol || x > m.x1 + tol || y < m.yT - tol || y > m.yB + tol) continue
            val free = m.corners
            if (free != null) {
                // four free corners: the picked one's corner under the finger, else its body -- inside its shape only
                if (m.id == selId) nearestCorner(free, x, y, tol)?.let { return Hit.Mark(m.id, it.first) }
                if (!Pano360.inPoly(x, y, free)) continue
                return Hit.Mark(m.id, "body")
            }
            if (m.id == selId) return Hit.Mark(m.id, nearestTab(m.x0, m.x1, m.yT, m.yB, x, y, tol)?.first ?: "body")
            if (x < m.x0 || x > m.x1 || y < m.yT || y > m.yB) continue
            return Hit.Mark(m.id, "body")
        }
        return null
    }

    // ------------------------------------------------------------ the picked item's code chip (port 63 slice I2a)

    /*
     * THE PICKED ITEM'S CODE IS A CHIP (DESIGN-imagemeter §2.2; the plan critic's finding C8): the code and `▾`, drawn
     * OUTSIDE the item, and a tap on it opens the panel that slides up. Until slice I2 the code was drawn INSIDE the box
     * at its top-left, within the `lt` tab's 16 dp reach, and on a switch plate (about 25 x 11 dp) it was larger than the
     * mark -- its left half a handle, its right half the body, and nothing "the code". So the chip stands where
     * NOTHING ELSE TAKES A PRESS FIRST: its tap box at least [CHIP_TAP_W_DP] x [CHIP_TAP_H_DP] (44 x 28 dp) and
     * [CHIP_CLEAR_DP] (16 dp: a tab's own reach) clear of every tab, corner handle and end of the item it names.
     * Pure geometry in canvas px, as everything here; :app measures the code's width and draws.
     */

    /** The chip's tap box is never smaller than this, dp. */
    const val CHIP_TAP_W_DP = 44.0
    const val CHIP_TAP_H_DP = 28.0
    /** The chip as drawn: this high, its text and this much either side of it, dp. */
    const val CHIP_H_DP = 22.0
    const val CHIP_PAD_DP = 8.0
    /** Its tap box stands this far from every handle of its item, dp: the reach a tab is pressed within ([hit]'s `tol`). */
    const val CHIP_CLEAR_DP = 16.0
    /** ... and this far inside the photo, dp. */
    const val CHIP_EDGE_DP = 4.0

    /** After the code on the chip: it opens something. */
    private const val CHIP_OPENS = " ▾" // text:value a glyph, the same in both languages

    /** What the chip reads: the item's code and `▾` -- "FW1 ▾", "F-T1 ▾". The one place a Tape's, a Line's, an area's
     *  and a chain's code is shown ([PhotoFigures.nameOf]; P4: never on its line). */
    fun chipText(code: String): String = code + CHIP_OPENS

    /** Where the chip stands: [box] as drawn, [tap] its tap box (what [hit] is handed, and what the figures' layout is
     *  told to keep clear, as it is told a tab -- [DimLayout.placeShown]'s `keep`), and [how] which rule placed it. */
    class Chip(val box: Box, val tap: Box, val how: String)

    /**
     * WHAT A PRESS MAY GRAB A PICKED MARK BY, canvas px, [bb] the mark as drawn: its eight tabs; its four free
     * [corners] where it shows those instead ([Quad.free]; [Drawn.corners]); a [spot]'s own middle (a point, a Note's
     * pin: no tabs, grabbed round its middle). What [codeChip] keeps the chip clear of. (The floating trash, which
     * went in slice I2b, kept clear of the same.)
     */
    fun handlesOf(bb: Box, spot: Boolean, corners: List<DoubleArray>? = null): List<DoubleArray> =
        if (spot) listOf(doubleArrayOf((bb.l + bb.r) / 2, (bb.t + bb.b) / 2))
        else corners?.take(Quad.PARTS.size) ?: TABS.map { tabAt(it, bb.l, bb.r, bb.t, bb.b) }

    /** How far ([x], [y]) is from the rectangle [b]: 0 inside it. */
    fun boxDist(b: Box, x: Double, y: Double): Double = hypot(max(0.0, max(b.l - x, x - b.r)), max(0.0, max(b.t - y, y - b.b)))

    private fun overlap(a: Box, b: Box): Boolean = a.l < b.r && a.r > b.l && a.t < b.b && a.b > b.t

    /** The chip for a tap box whose top-left is ([l], [t]): [tw] x [th] the tap box, [dw] x [dh] the chip drawn, centred in it. */
    private fun chipAt(l: Double, t: Double, tw: Double, th: Double, dw: Double, dh: Double, how: String): Chip {
        val bl = l + (tw - dw) / 2; val bt = t + (th - dh) / 2
        return Chip(Box(bl, bt, bl + dw, bt + dh), Box(l, t, l + tw, t + th), how)
    }

    /**
     * HOW FAR ABOVE A MARK'S TOP EDGE ITS CHIP'S TAP BOX ENDS, px: [CHIP_CLEAR_DP], the top tabs' own reach -- and, for
     * a mark that shows its handles ([hand]: it is HELD, [Pick]; eight tabs, or four free corners), above the
     * hand-anchored WIDTH plate as well, which stands over the top edge clear of the tabs ([widthTag] with `hand`; the
     * plate's top is its baseline, its text size [fs] and [DimLayout.OVER_DP] up) and is placed AFTER the chip: the
     * chip is in the layout's `keep`, so a chip in that plate's row would push the gold width plate of a mark just
     * placed off its anchor. [u] the drawing's unit, [dp] px a dp.
     */
    fun chipLift(hand: Boolean, fs: Double, u: Double, dp: Double): Double =
        if (!hand) CHIP_CLEAR_DP * dp
        else max(CHIP_CLEAR_DP * dp, fs * 0.8 + clearance(u, dp) + fs + DimLayout.OVER_DP * dp + DimLayout.APART_U * u)

    /**
     * WHERE THE PICKED MARK'S CODE CHIP STANDS (DESIGN-imagemeter §2.2). [bb] the mark as drawn -- a four-corner mark's
     * BOUNDING box (DESIGN-imagemeter §8.2: "the code chip keeps the bounding box's top-left"), a spot's own square;
     * [handles] what a press may grab it by, canvas px -- its eight tabs, its four free corners, a spot's middle
     * ([handlesOf]); [textW] its text (the code and `▾`) as measured; a [w] x [h] photo; [dp] px a dp;
     * [lift] how far over the top edge it stands ([chipLift]); [avoid] what is drawn over the photo and takes a press
     * of its own (fit · grid, the tool chip). The first of these whose tap box is inside the photo, [CHIP_CLEAR_DP]
     * from every handle and off every [avoid]:
     *  1. ABOVE THE TOP-LEFT CORNER, outside the box: its left edge the box's, [lift] over the top edge;
     *  2. BELOW THE BOTTOM-LEFT CORNER -- the mark touches the photo's top, or is too near it;
     *  3. left of the box, at its top; 4. right of it, at its top -- a mark as tall as the photo (a floor-to-ceiling
     *     wardrobe; in landscape every such mark at fit);
     *  5. INSIDE the box, [CHIP_CLEAR_DP] in from its top-left corner -- a mark that fills the photo both ways, or one
     *     zoomed into. It then lies on the mark's body, and beats it ([hit]).
     * Sideways it is slid back inside the photo; it is never slid towards a handle. NULL -- NO CHIP -- when the mark
     * is off the photo, or no place is that clear (a photo narrower than the chip): a long press on the item still
     * opens the panel, and a chip within a tab's reach would be the fault this rule exists to end.
     */
    fun codeChip(bb: Box, handles: List<DoubleArray>, textW: Double, w: Double, h: Double, dp: Double,
                 lift: Double = CHIP_CLEAR_DP * dp, avoid: List<Box> = emptyList()): Chip? {
        if (bb.r < 0 || bb.l > w || bb.b < 0 || bb.t > h) return null
        val dw = textW + 2 * CHIP_PAD_DP * dp; val dh = CHIP_H_DP * dp
        val tw = max(dw, CHIP_TAP_W_DP * dp); val th = max(dh, CHIP_TAP_H_DP * dp)
        val edge = CHIP_EDGE_DP * dp; val clear = CHIP_CLEAR_DP * dp
        if (w < tw + 2 * edge || h < th + 2 * edge) return null
        fun cx(l: Double) = max(edge, min(w - tw - edge, l))
        fun cy(t: Double) = max(edge, min(h - th - edge, t))
        // the DRAWN chip's left edge is the box's: the tap box, where it is wider, reaches the same either side of it
        val l0 = bb.l - (tw - dw) / 2
        val c = ArrayList<Chip>()
        (bb.t - max(lift, clear) - th).let { t -> if (t >= edge) c += chipAt(cx(l0), t, tw, th, dw, dh, "above") }
        (bb.b + clear).let { t -> if (t + th <= h - edge) c += chipAt(cx(l0), t, tw, th, dw, dh, "below") }
        (bb.l - clear - tw).let { l -> if (l >= edge) c += chipAt(l, cy(bb.t), tw, th, dw, dh, "left") }
        (bb.r + clear).let { l -> if (l + tw <= w - edge) c += chipAt(l, cy(bb.t), tw, th, dw, dh, "right") }
        c += chipAt(cx(max(bb.l, 0.0) + clear), cy(max(bb.t, 0.0) + clear), tw, th, dw, dh, "inside")
        return c.firstOrNull { k -> handles.all { p -> boxDist(k.tap, p[0], p[1]) >= clear - 1e-9 } && avoid.none { overlap(k.tap, it) } }
    }

    /**
     * ... AND A PICKED TAPE'S, LINE'S, AREA'S OR CHAIN'S: "the same chip off its first point" (DESIGN-imagemeter §2.2).
     * [scr] its points on screen, in its order. It stands [CHIP_CLEAR_DP] off the FIRST of its points that is on the
     * photo -- above it, below it, left or right of it, centred on it -- its tap box [CHIP_CLEAR_DP] clear of EVERY
     * point of the item (each is an end a press drags), inside the photo and off every [avoid]. Of the places that are
     * clear, the one that points MOST AWAY FROM THE REST OF THE ITEM -- off the end of a level tape pulled to the
     * right, to its left; under a Line drawn up from the floor (ties: above, below, left, right): the chip beats a
     * body ([hit]), so on the line it would take the press that moves the tape. Null when no point is on the photo or
     * no place is clear.
     */
    fun codeChipOfLine(scr: List<DoubleArray>, textW: Double, w: Double, h: Double, dp: Double, avoid: List<Box> = emptyList()): Chip? {
        val p = scr.firstOrNull { it[0] >= 0 && it[0] <= w && it[1] >= 0 && it[1] <= h } ?: return null
        val dw = textW + 2 * CHIP_PAD_DP * dp; val dh = CHIP_H_DP * dp
        val tw = max(dw, CHIP_TAP_W_DP * dp); val th = max(dh, CHIP_TAP_H_DP * dp)
        val edge = CHIP_EDGE_DP * dp; val clear = CHIP_CLEAR_DP * dp
        if (w < tw + 2 * edge || h < th + 2 * edge) return null
        fun cx(l: Double) = max(edge, min(w - tw - edge, l))
        fun cy(t: Double) = max(edge, min(h - th - edge, t))
        // away from the rest of the item: from the middle of its other points to this one (canvas px, y down)
        val rest = scr.filter { it !== p }
        val ox = if (rest.isEmpty()) 0.0 else p[0] - rest.sumOf { it[0] } / rest.size
        val oy = if (rest.isEmpty()) 0.0 else p[1] - rest.sumOf { it[1] } / rest.size
        // (0.0 - x, never -x: a tie must be a tie, and -0.0 sorts under 0.0)
        val c = ArrayList<Pair<Chip, Double>>()
        (p[1] - clear - th).let { t -> if (t >= edge) c += chipAt(cx(p[0] - tw / 2), t, tw, th, dw, dh, "above") to 0.0 - oy }
        (p[1] + clear).let { t -> if (t + th <= h - edge) c += chipAt(cx(p[0] - tw / 2), t, tw, th, dw, dh, "below") to oy }
        (p[0] - clear - tw).let { l -> if (l >= edge) c += chipAt(l, cy(p[1] - th / 2), tw, th, dw, dh, "left") to 0.0 - ox }
        (p[0] + clear).let { l -> if (l + tw <= w - edge) c += chipAt(l, cy(p[1] - th / 2), tw, th, dw, dh, "right") to ox }
        return c.filter { (k, _) -> scr.all { q -> boxDist(k.tap, q[0], q[1]) >= clear - 1e-9 } && avoid.none { overlap(k.tap, it) } }
            .maxByOrNull { it.second }?.first
    }

    // ------------------------------------------------------------ the magnifier

    /** Where one magnifier goes: its top-left, and which rule placed it. */
    class Spot(val left: Double, val top: Double, val how: String)

    /** Where the two magnifiers go (a line's two junctions in 1 · Lines). */
    class Pair2(val left1: Double, val left2: Double, val top: Double)

    private class Room(val w: Double, val dia: Double, val gap: Double, val obst: List<Box>) {
        fun hits(l: Double, t: Double) = obst.filter { b ->
            l < b.r + gap / 2 && l + dia > b.l - gap / 2 && t < b.b + gap / 2 && t + dia > b.t - gap / 2 }
        /** Pushed down below whatever it sits on. */
        fun below(l: Double, t0: Double): Double {
            var t = t0
            repeat(6) { val hs = hits(l, t); if (hs.isEmpty()) return t; t = max(t, hs.maxOf { it.b + gap }) }
            return t
        }
        /** Moved sideways off whatever it sits on, inwards from the edge it is near. */
        fun aside(l: Double, t: Double): Double {
            val hs = hits(l, t); if (hs.isEmpty()) return l
            val lft = hs.minOf { it.l } - dia - gap; val rgt = hs.maxOf { it.r } + gap
            return if (l > w / 2) (if (lft >= 4) lft else rgt) else (if (rgt + dia <= w - 4) rgt else lft)
        }
    }

    /**
     * ONE MAGNIFIER, [dia] across, for a finger at [fx],[fy] on a [w] x [h]
     * photo, kept off [obst] (the buttons drawn over the photo) and clear of
     * [anchors] (the point a drag is measured from, when there is one). [dp]
     * is the screen density, [gap] the margin (8 dp):
     *  1. above the finger, 40 dp up, slid aside off a button;
     *  2. with no room above: the top corner FARTHEST from the finger (or that
     *     corner moved in past a button, or down below it -- whichever stays
     *     nearest the corner), clear of the finger, the anchors and the photo's
     *     bottom; then the other top corner;
     *  3. both taken: beside the finger, 24 dp off, on the side away from the
     *     anchor, centred on the finger's height;
     *  4. never below the finger, never under it.
     */
    fun loupePlace(fx: Double, fy: Double, w: Double, h: Double, dia: Double, gap: Double, dp: Double,
                   obst: List<Box>, anchors: List<DoubleArray> = emptyList()): Spot {
        val r = Room(w, dia, gap, obst)
        fun clearF(l: Double, t: Double) = abs(fx - (l + dia / 2)) > dia / 2 + 20 * dp || t > fy + 40 * dp || t + dia < fy - 40 * dp
        fun clearA(l: Double, t: Double) = anchors.all { a -> hypot(a[0] - (l + dia / 2), a[1] - (t + dia / 2)) > dia / 2 + gap }
        var top = fy - dia - 40 * dp
        var left = min(w - dia - 4, max(4.0, fx - dia / 2))
        if (top >= 4 && r.hits(left, top).isNotEmpty()) {
            val l2 = r.aside(left, top)
            if (l2 >= 4 && l2 + dia <= w - 4 && r.hits(l2, top).isEmpty()) left = l2 else top = -1.0
        }
        if (top >= 4) return Spot(left, top, "above")
        val l0 = if (fx < w / 2) w - dia - gap else gap
        fun pick(c0: Double): DoubleArray? {
            val cols = (listOf(c0, r.aside(c0, gap)) + obst.map { b -> if (c0 > w / 2) b.l - dia - gap else b.r + gap })
                .filter { it >= 4 && it + dia <= w - 4 }
            var best: DoubleArray? = null
            for (l in cols) {
                val t = r.below(l, gap)
                if (r.hits(l, t).isNotEmpty() || !clearF(l, t) || !clearA(l, t) || t + dia > h - 4) continue
                val b = best
                if (b == null || t < b[1] - .5 || (abs(t - b[1]) <= .5 && abs(l - c0) < abs(b[0] - c0))) best = doubleArrayOf(l, t)
            }
            return best
        }
        pick(l0)?.let { return Spot(it[0], it[1], "corner") }
        pick(if (fx < w / 2) gap else w - dia - gap)?.let { return Spot(it[0], it[1], "corner") }
        val ax = anchors.firstOrNull()?.get(0) ?: fx
        val t = min(h - dia - 4, max(4.0, fy - dia / 2))
        for (sd in if (ax < fx) listOf(1, -1) else listOf(-1, 1)) {
            val l = if (sd > 0) fx + 24 * dp else fx - 24 * dp - dia
            val t1 = r.below(l, t)
            if (l >= 4 && l + dia <= w - 4 && t1 + dia <= h - 4 && r.hits(l, t1).isEmpty() && clearA(l, t1)) return Spot(l, t1, "beside")
        }
        return Spot(l0, gap, "corner-covered")
    }

    /**
     * TWO MAGNIFIERS, a line's two junctions: both along the top, or both
     * along the bottom when the finger is within [dia] + 60 dp of the top --
     * each slid off a button, and both pushed down below one if they still sit on it.
     */
    fun loupePair(fy: Double, w: Double, h: Double, dia: Double, gap: Double, dp: Double, obst: List<Box>): Pair2 {
        val r = Room(w, dia, gap, obst)
        var top = if (fy < dia + 60 * dp) h - dia - gap else gap
        val l1 = r.aside(gap, top); val l2 = r.aside(w - dia - gap, top)
        if (r.hits(l1, top).isNotEmpty() || r.hits(l2, top).isNotEmpty()) top = max(r.below(l1, top), r.below(l2, top))
        return Pair2(l1, l2, top)
    }
}
