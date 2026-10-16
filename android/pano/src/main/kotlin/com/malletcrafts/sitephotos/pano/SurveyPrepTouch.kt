package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

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
    }

    /**
     * THE HIT ORDER on 2 · Mark, [marks] in drawing order (the last on top),
     * [tags] as drawn, [tol] the tab reach (16 dp), [dp] px per dp:
     *  1. the PICKED mark's eight tabs -- unless a tag under the finger has
     *     its centre nearer than the tab ([tabOrTag]); a picked SPOT has no
     *     tabs and is grabbed whole within [spotReachPicked] of its centre,
     *     unless a tag under the finger has its centre nearer;
     *  2. a tag under the finger, the nearest centre;
     *  3. the marks, topmost first: a spot within [spotReach] of its centre,
     *     moved whole, never resized; the picked box within [tol] of its edge
     *     (a tab there still resizes), any other box only inside it.
     * Null: nothing, and the finger slides the photo.
     */
    fun hit(marks: List<Drawn>, selId: String?, tags: List<Tag>, x: Double, y: Double, tol: Double, dp: Double = tol / 16): Hit? {
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
        if (under != null) return Hit.Tag(under.code)
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

    /**
     * WHERE THE FLOATING TRASH GOES (v52, sp.js placeFTrash): a [size] (36 dp) disc beside the
     * picked mark, whose box on screen is [bb]; [tags] are its figures' boxes as drawn, on a
     * [w] x [h] photo, all canvas px; [dp] px per dp. Eight candidates, each kept 4 dp inside
     * the photo and clear of the fit / zoom buttons in the top-right corner: above-right,
     * above-left, below-right, below-left, then the same four one trash-height further out.
     * The first that covers NO figure wins (v52 fix: a point's "from corner" tag sat where the
     * trash went, so a tap meant for the figure deleted the point); none clear, the first.
     * Null when the mark is off the photo: no trash.
     */
    fun trashSpot(bb: Box, tags: List<Box>, w: Double, h: Double, dp: Double): Spot? {
        if (bb.r < 0 || bb.l > w || bb.b < 0 || bb.t > h) return null
        val size = 36 * dp
        fun at(l0: Double, t0: Double, how: String): Spot {
            var left = max(4 * dp, min(w - 40 * dp, l0)); val top = max(4 * dp, min(h - 40 * dp, t0))
            if (top < 100 * dp && left > w - 104 * dp) left = w - 104 * dp     // clear of fit / zoom
            return Spot(left, top, how)
        }
        fun clear(sp: Spot) = tags.all { r -> sp.left + size <= r.l || sp.left >= r.r || sp.top + size <= r.t || sp.top >= r.b }
        val c = mutableListOf<Spot>()
        for (f in 0..1) {
            val out = 36 * dp * f
            c.add(at(bb.r + 2 * dp, bb.t - 38 * dp - out, "above-right"))
            c.add(at(bb.l - 38 * dp, bb.t - 38 * dp - out, "above-left"))
            c.add(at(bb.r + 2 * dp, bb.b + 2 * dp + out, "below-right"))
            c.add(at(bb.l - 38 * dp, bb.b + 2 * dp + out, "below-left"))
        }
        return c.firstOrNull { clear(it) } ?: c[0]
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
