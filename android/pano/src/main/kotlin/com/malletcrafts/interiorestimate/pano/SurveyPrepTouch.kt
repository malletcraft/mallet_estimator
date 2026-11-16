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
     *  makes on screen (25 mm, canvas px, before the 6 dp floor it is drawn with); null for a box. */
    class Drawn(val id: String, val x0: Double, val x1: Double, val yT: Double, val yB: Double, val spot: Double? = null) {
        val cx get() = (x0 + x1) / 2
        val cy get() = (yT + yB) / 2
    }

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
     */
    fun hit(marks: List<Drawn>, selId: String?, tags: List<Tag>, x: Double, y: Double, tol: Double, dp: Double = tol / 16,
            nodes: List<Nodes> = emptyList(), nodeR: Double = tol, body: Boolean = false): Hit? {
        val under = tags.filter { it.box.contains(x, y) }.minByOrNull { it.box.centreDist(x, y) }
        val sel = selId?.let { id -> marks.lastOrNull { it.id == id } }
        if (sel != null) {
            val sp = sel.spot
            if (sp != null) {
                val d = hypot(x - sel.cx, y - sel.cy)
                if (d <= spotReachPicked(sp, dp) && tabOrTag(d, under?.box?.centreDist(x, y))) return Hit.Mark(sel.id, "body")
            } else {
                val t = nearestTab(sel.x0, sel.x1, sel.yT, sel.yB, x, y, tol)
                if (t != null && tabOrTag(t.second, under?.box?.centreDist(x, y))) return Hit.Mark(sel.id, t.first)
            }
        }
        SurveyChain.nodeAt(nodes.map { it.id to it.pts }, x, y, nodeR)?.let { (id, i) -> return Hit.Node(id, i) }
        if (under != null) return Hit.Tag(under.code)
        if (body) return Hit.Body
        for (m in marks.asReversed()) {
            val sp = m.spot
            if (sp != null) {
                if (hypot(x - m.cx, y - m.cy) <= spotReach(sp, tol)) return Hit.Mark(m.id, "body")
                continue
            }
            if (x < m.x0 - tol || x > m.x1 + tol || y < m.yT - tol || y > m.yB + tol) continue
            if (m.id == selId) return Hit.Mark(m.id, nearestTab(m.x0, m.x1, m.yT, m.yB, x, y, tol)?.first ?: "body")
            if (x < m.x0 || x > m.x1 || y < m.yT || y > m.yB) continue
            return Hit.Mark(m.id, "body")
        }
        return null
    }

    // ------------------------------------------------------------ the floating trash

    /** The floating trash is a disc this many dp across. */
    const val TRASH_DP = 36.0

    /**
     * WHAT THE FLOATING TRASH KEEPS CLEAR OF BESIDES THE FIGURES (port 63 slice F1; [DeleteGuard]): the picked item's
     * own handles, on screen, canvas px. [points]: a box mark's eight tabs, a spot's centre, a Tape's / Line's /
     * area's ends, a chain's points. [body]: the straight pieces of a Tape's, a Line's, an area's or a chain's line --
     * a press there moves or picks it -- each [bodyHalf] wide either side (a tape's band; 0 for a line).
     */
    class TrashKeep(val points: List<DoubleArray>, val body: List<Pair<DoubleArray, DoubleArray>> = emptyList(), val bodyHalf: Double = 0.0) {
        companion object { val NONE = TrashKeep(emptyList()) }
    }

    /** What the disc keeps clear of beside a picked MARK drawn as the box [bb]: its eight tabs -- or, for a [spot] (a
     *  point, a Note's pin), which has no tabs and is grabbed round its middle, its centre. */
    fun keepOfMark(bb: Box, spot: Boolean): TrashKeep =
        if (spot) TrashKeep(listOf(doubleArrayOf((bb.l + bb.r) / 2, (bb.t + bb.b) / 2)))
        else TrashKeep(TABS.map { tabAt(it, bb.l, bb.r, bb.t, bb.b) })

    /** ... beside a picked Tape, Line, area or chain whose points on screen are [scr]: every point, and the pieces
     *  between them ([closed]: an area, whose last corner joins its first), [half] wide either side (a tape's band). */
    fun keepOfLine(scr: List<DoubleArray>, closed: Boolean, half: Double = 0.0): TrashKeep {
        val n = if (closed) scr.size else scr.size - 1
        return TrashKeep(scr, (0 until max(0, n)).map { scr[it] to scr[(it + 1) % scr.size] }, half)
    }

    /**
     * WHERE THE FLOATING TRASH GOES (v52, sp.js placeFTrash; RE-RULED IN PORT 63 SLICE F1): a [TRASH_DP] (36 dp) disc
     * beside the picked item, whose box on screen is [bb]; [tags] are its figures' boxes as drawn, on a [w] x [h]
     * photo, all canvas px; [dp] px per dp.
     *
     * Amit, 16 Nov 2026, with build 0.3.337 on his phone: "navigation button and D2 aer very close where d2 get
     * accidentally fired. same for delete button for any item." The disc sat 2 dp off the picked mark's corner -- its
     * edge 10 dp from the corner tab, well inside that tab's 16 dp reach -- and took the press BEFORE the tab did; it
     * stood 7.5 dp off a tape's end handle, and anywhere at all beside a figure. So a finger a little wide of a handle
     * deleted the thing it meant to stretch.
     *
     * NOW THE DISC IS [clear] (16 dp, [DeleteGuard.CLEAR_DP]) FROM EVERYTHING ELSE THAT TAKES A PRESS: its edge that
     * far from every point and every piece of [keep] -- the picked item's own handles ([TrashKeep]) -- and its square
     * that far from every figure. Eight candidates at its corners, each kept 4 dp inside the photo and clear of the
     * fit / zoom buttons in the top-right corner: above-right, above-left, below-right, below-left -- out of the box's
     * corner along its diagonal, just far enough that the disc's edge is [clear] from the corner itself -- then the
     * same four one trash-height further out.
     *
     * THEN SIX BESIDE IT (the F1 review, finding 3): right of the box and left of it, the disc's square [clear] (and a
     * tape's band) off the box's side, at the middle of what the photo shows of the item, then a quarter and three
     * quarters down it. An item AS TALL AS THE PHOTO -- a floor-to-ceiling wardrobe, a tape from floor to ceiling;
     * in landscape the wall fills the photo's height, so every such item at fit zoom -- has no corner place: the
     * photo's edge clamps all eight to the same two rows beside a corner tab or the band's end, and F1 as built gave
     * it no disc. A place beside it is tried only where it FITS: one the photo's edge or the fit / zoom corner would
     * move sideways is moved onto the item itself, between its tabs, and is not a candidate.
     *
     * The first of the fourteen that is [clear] from the handles AND from the figures wins; where the figures crowd
     * round the item and none is, the first that is [clear] from the handles and COVERS no figure (v52's own rule
     * for a figure: "a tap meant for the figure deleted the point").
     *
     * NULL -- NO DISC AT ALL -- when the item is off the photo, and WHEN NO CANDIDATE IS CLEAR OF ITS HANDLES AND OFF
     * EVERY FIGURE: an item that fills the photo BOTH ways (wall to wall and floor to ceiling, or zoomed in), or one
     * with a figure on every side. The prototype's rule was "none clear, the first": a disc on a handle or over a
     * figure. The picked item's row under the photo and the long-press menu still delete it; a delete control
     * standing where a slip lands on it is worse than none.
     */
    fun trashSpot(bb: Box, tags: List<Box>, w: Double, h: Double, dp: Double,
                  keep: TrashKeep = TrashKeep.NONE, clear: Double = DeleteGuard.CLEAR_DP * dp): Spot? {
        if (bb.r < 0 || bb.l > w || bb.b < 0 || bb.t > h) return null
        val size = TRASH_DP * dp; val rad = size / 2
        // how far out of the box's corner the disc's square starts, in whole dp: its edge is then [clear] (and a
        // tape's band) from the corner, where a mark's corner tab or a Tape's end is
        val off = max(2.0, ceil(((clear + keep.bodyHalf + rad) / sqrt(2.0) - rad) / dp)) * dp
        fun at(l0: Double, t0: Double, how: String): Spot {
            var left = max(4 * dp, min(w - 40 * dp, l0)); val top = max(4 * dp, min(h - 40 * dp, t0))
            if (top < 100 * dp && left > w - 104 * dp) left = w - 104 * dp     // clear of fit / zoom
            return Spot(left, top, how)
        }
        // off every figure: the disc's square, grown by [g], covers none (g 0: v52's fix, where a point's "from corner"
        // tag sat where the trash went, so a tap meant for the figure deleted the point)
        fun figures(sp: Spot, g: Double) = tags.all { r ->
            sp.left + size + g <= r.l || sp.left - g >= r.r || sp.top + size + g <= r.t || sp.top - g >= r.b }
        // clear of the picked item's own handles: the disc's edge [clear] from each point and each piece of its line
        fun handles(sp: Spot): Boolean {
            val cx = sp.left + rad; val cy = sp.top + rad
            return keep.points.all { p -> hypot(cx - p[0], cy - p[1]) >= rad + clear } &&
                keep.body.all { (p, q) -> SurveyMeas.segDist(cx, cy, p, q) >= rad + clear + keep.bodyHalf }
        }
        val c = mutableListOf<Spot>()
        for (f in 0..1) {
            val out = size * f
            c.add(at(bb.r + off, bb.t - size - off - out, "above-right"))
            c.add(at(bb.l - size - off, bb.t - size - off - out, "above-left"))
            c.add(at(bb.r + off, bb.b + off + out, "below-right"))
            c.add(at(bb.l - size - off, bb.b + off + out, "below-left"))
        }
        // BESIDE IT: the disc's square [clear] (and a tape's band) off the box's side, in whole dp, at three heights of
        // what the photo shows of the item -- and only where the place was not moved sideways, onto the item
        val side = ceil((clear + keep.bodyHalf) / dp) * dp
        val vt = max(bb.t, 0.0); val vb = min(bb.b, h)
        for (f in doubleArrayOf(0.5, 0.25, 0.75)) {
            val t0 = vt + (vb - vt) * f - rad
            for ((l0, how) in listOf(bb.r + side to "right", bb.l - size - side to "left")) {
                val sp = at(l0, t0, how)
                if (sp.left == l0) c.add(sp)
            }
        }
        return c.firstOrNull { handles(it) && figures(it, clear) } ?: c.firstOrNull { handles(it) && figures(it, 0.0) }
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
