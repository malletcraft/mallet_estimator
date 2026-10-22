package com.malletcrafts.interiorestimate.pano

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * THE DIMENSION CHAIN ON SURVEY PREP (prototype v52, sp.js 628-741; v52 slice 11, the chain halves of items 7 and 14).
 * Pure, so every axis, snap, key, total, word and hit is tested here; SurveyPrepScreen only dispatches and
 * SurveyPrepDraw only draws.
 *
 * Amit, 13 Oct 2026 19:20 IST: "where is simple line measure tool and divider?" -- the divider being a DIMENSION CHAIN
 * "along with capacity for derived measurements and entered measurements via disto or keyboard entry". Tap point after
 * point along a surface: the chain runs along the line of its first two taps, kept horizontal or vertical on the
 * surface, and every point snaps onto the surface's edges and the marks' edges within 25 mm (14 dp when zoomed out,
 * SurveyGuide.snapCoord). A double tap, or ✓ on the chip, ends it. Each segment has its ≈ photo figure and a reading
 * box (the D2 into the active one, which then moves on, or the keyboard). The total is the laser's W / L / H when the
 * chain runs edge to edge; with one segment unknown that segment is DERIVED from the total; with every segment
 * measured an unmeasured total is derived from them. Code K: FK1, its segments FK1.1, FK1.2 …, its total FK1.t --
 * keys S5 QUOTE accepts since slice 3 (KeyGrammarTest).
 *
 * Item 7 (Amit, 14 Oct 2026 20:45 IST: "i need perpective guidance reference shown when i draw any of these two tools
 * so i will know i am drawing and measuring in correct reference"): while a chain is drawn the guide runs through its
 * first point, bold on its axis once two points have fixed it. Item 14 (13 Oct 21:06: "Check any mark tools handles ,
 * not every handle works as expected like center handles"): a chain point under the picked mark's tab never takes the
 * tab's drag -- the tabs come first (SurveyPrepTouch.hit).
 */
object SurveyChain {

    /** The chain's code letter (FK1) and its code counter's key in the surface's SEQ. Never a mark type's letter. */
    const val LETTER = "K"
    /** An item of this kind, as a clear, a lock and the menus see it (SurveyPrep.Item.kind). */
    const val KIND = "chain"
    /** Rose (sp.js KC). */
    const val COLOUR = 0xfb7185
    /** The total's key: FK1.t. */
    const val TOTAL = "t"

    /** A chain point is grabbed within 16 dp (sp.js kHitNode) ... */
    const val NODE_HIT_DP = 16.0
    /** ... and, with Select on, a chain is picked within 12 dp of its line (sp.js kHitLine). */
    const val PICK_DP = 12.0
    /** The chain line: 2.6 dp, 3.6 picked (sp.js drawChains); a draft dashed 7 / 5 dp. */
    const val LINE_DP = 2.6
    const val LINE_SEL_DP = 3.6
    const val DASH_DP = 7.0
    const val GAP_DP = 5.0
    /** A tick across the chain at every point, 10 dp either side, 2.2 dp wide; the point a white dot, 3.6 dp (4.6
     *  picked), ringed 1.6 dp. */
    const val TICK_DP = 10.0
    const val TICK_W_DP = 2.2
    const val NODE_R_DP = 3.6
    const val NODE_R_SEL_DP = 4.6
    const val NODE_RING_DP = 1.6
    /** A segment's figure stands 15 dp off the chain, 12 dp text; the total 11 dp text, 8 dp right of and 20 dp under
     *  the last point. */
    const val FIGURE_OFF_DP = 15.0

    /** A segment's figure: measured white, derived violet (with " d"), the photo's ≈ grey; a total whose readings
     *  disagree with the laser by more than SurveyPrep.TOL amber. */
    const val MEASURED_COLOUR = 0xffffff
    const val DERIVED_COLOUR = 0xc4b5fd
    const val PHOTO_COLOUR = 0xd9dcd4
    const val CHECK_COLOUR = 0xffc46b

    /** Where the total came from (sp.js kVals src). */
    const val MEASURED = "measured"
    const val LASER = "laser"
    const val DERIVED = "derived"

    // ------------------------------------------------------------ the words (sp.js, exactly)

    /** The chip with Chain on: "Chain · 2 points · tap points along the surface, double-tap or ✓ to end". */
    fun chipText(points: Int): String =
        "Chain · " + (if (points > 0) "$points point${if (points == 1) "" else "s"} · " else "") +
            "tap points along the surface, double-tap or ✓ to end"

    /** ✓ on the chip ends the chain -- shown from its second point (ct52 K: "no ✓ before two points"). */
    fun showDone(points: Int) = points >= 2

    /** ✓'s screen-reader label (sp.html data-kdone). */
    const val DONE_LABEL = "End the chain"

    /** A chain let go with one point. */
    const val NEED_TWO = "A chain needs two points or more"
    /** ... or with every point on top of another. */
    const val NEED_DIFFERENT = "A chain needs two different points"

    /** A tap on another surface while a chain is being drawn (sp.js kDown). */
    fun busy(s: String) = "Finish the chain on the ${SurveyPrep.NAME.getValue(s).lowercase()} first — ✓ or a double tap"

    /** What a locked chain's point says when it is dragged: "FK1 is locked — unlock to move it". */
    fun lockedNode(code: String) = SurveyGuide.lockedEnd(code)

    /** What every delete path and the menu call a chain (sp.js itemById): "FK1 chain"; the trash says "Delete FK1 chain". */
    fun label(c: SurveyPrep.Chain) = "${c.code} chain"

    /** The long-press menu's group (sp.js lockGroup): "Lock all chains". */
    const val GROUP = "chains"

    /** "horizontal" or "vertical" (sp.js chainPanel). */
    fun direction(c: SurveyPrep.Chain) = if (c.axis == "y") "vertical" else "horizontal"

    /** Under its code in 3 · List and the List tab (sp.js chainPanel): "Chain · horizontal · 2 parts" -- and on the List
     *  tab ([room]), " · Front wall" after it. */
    fun listName(s: String, c: SurveyPrep.Chain, room: Boolean): String {
        val n = max(0, c.nodes.size - 1)
        return "Chain · ${direction(c)} · $n part${if (n == 1) "" else "s"}" + if (room) " · " + SurveyPrep.NAME.getValue(s) else ""
    }

    // ------------------------------------------------------------ figures (sp.js kA, kSegs, kLaser, kVals, kItems)

    /** The coordinate the chain runs along: 0 (x) or 1 (y). */
    fun axisIndex(axis: String?) = if (axis == "y") 1 else 0

    /** Each segment's length along the axis, as the photo says, mm. */
    fun segs(c: SurveyPrep.Chain): List<Double> = segsOf(c.nodes, c.axis)

    fun segsOf(nodes: List<DoubleArray>, axis: String?): List<Double> {
        val a = axisIndex(axis)
        return (1 until nodes.size).map { abs(nodes[it][a] - nodes[it - 1][a]) }
    }

    /**
     * THE LASER'S SIZE ALONG THE CHAIN, when it runs EDGE TO EDGE on the [a] x [b] face (its first point within 0.5 mm of
     * one edge, its last of the other): then the total is the face's laser W, L or H and needs no reading. Else null.
     */
    fun laser(c: SurveyPrep.Chain, a: Double, b: Double): Double? {
        if (c.nodes.size < 2) return null
        val ai = axisIndex(c.axis); val lim = if (ai == 1) b else a
        val lo = c.nodes.minOf { it[ai] }; val hi = c.nodes.maxOf { it[ai] }
        return if (abs(lo) < 0.5 && abs(hi - lim) < 0.5) lim else null
    }

    /**
     * Every figure of a chain (sp.js kVals): [est] each segment's photo figure and [estT] their sum; [meas] the
     * readings; [value] what each segment IS -- its reading, or, for the one unknown segment of a known total, the total
     * less the others ([der] its index, -1 for none); [total] the total and [src] where it came from -- its own reading
     * ([MEASURED]), the laser ([LASER]) or, with every segment read, their sum ([DERIVED]) -- null when unknown; [sum]
     * the segments' values added once each is known; [check] every segment read against a measured or laser total
     * (their difference, else null).
     */
    class Vals(val est: List<Double>, val estT: Double, val meas: List<Double?>, val value: List<Double?>, val der: Int,
               val total: Double?, val src: String?, val laser: Double?, val sum: Double?, val check: Double?)

    fun vals(c: SurveyPrep.Chain, actual: Map<String, Double>, a: Double, b: Double): Vals {
        val est = segs(c); val n = est.size
        val meas: List<Double?> = (0 until n).map { actual[c.code + "." + (it + 1)] }
        val tm = actual[c.code + "." + TOTAL]; val lz = laser(c, a, b)
        var total: Double? = tm ?: lz
        var src: String? = if (tm != null) MEASURED else if (lz != null) LASER else null
        val unk = (0 until n).filter { meas[it] == null }
        val value = meas.toMutableList(); var der = -1
        val t0 = total
        if (t0 != null && unk.size == 1) {
            der = unk[0]
            value[der] = t0 - (0 until n).sumOf { if (it == der) 0.0 else meas[it] ?: 0.0 }
        }
        if (total == null && n > 0 && unk.isEmpty()) { total = meas.sumOf { it ?: 0.0 }; src = DERIVED }
        val sum = if (value.all { it != null }) value.sumOf { it ?: 0.0 } else null
        val t1 = total
        val check = if (unk.isEmpty() && t1 != null && (src == MEASURED || src == LASER)) meas.sumOf { it ?: 0.0 } - t1 else null
        return Vals(est, est.sum(), meas, value, der, total, src, lz, sum, check)
    }

    /** What a chain asks for (sp.js kItems): "segment 1" … with its photo figure, then "total" with theirs. */
    fun measures(c: SurveyPrep.Chain): List<SurveyPrep.Measure> {
        val est = segs(c)
        return est.mapIndexed { i, v -> SurveyPrep.Measure((i + 1).toString(), "segment ${i + 1}", v) } +
            SurveyPrep.Measure(TOTAL, "total", est.sum())
    }

    /** The keys the D2 flow walks: the segments, then the total -- not when the laser gives it (sp.js nextOpen). */
    fun openKeys(c: SurveyPrep.Chain, a: Double, b: Double): List<String> {
        val lz = laser(c, a, b) != null
        return measures(c).filter { !(it.key == TOTAL && lz) }.map { c.code + "." + it.key }
    }

    /** The total's box takes no reading while the laser gives the total: it shows the laser's figure, read-only. */
    fun totalReadOnly(v: Vals) = v.src == LASER

    private fun r(v: Double) = v.roundToInt().toString()

    /** A total as the screen shows it: "3310 laser", "3310 d" (derived), "3310" (measured), "≈3702" (unknown). */
    fun totalText(v: Vals): String = v.total?.let { r(it) + when (v.src) { LASER -> " laser"; DERIVED -> " d"; else -> "" } } ?: "≈" + r(v.estT)

    /** Segment [i]'s figure on the photo and its colour: its reading (white), "2410 d" derived (violet), or "≈905". */
    fun segFigure(v: Vals, i: Int): Pair<String, Int> {
        val m = v.meas.getOrNull(i); val d = v.value.getOrNull(i)
        return when {
            m != null -> r(m) to MEASURED_COLOUR
            v.der == i && d != null -> "${r(d)} d" to DERIVED_COLOUR
            else -> "≈" + r(v.est.getOrElse(i) { 0.0 }) to PHOTO_COLOUR
        }
    }

    /** The tag under the last point (sp.js drawChains): "FK1 Σ 3310 laser". */
    fun totalTag(code: String, v: Vals) = "$code Σ ${totalText(v)}"

    /** Its colour: the chain's rose, amber when the readings and a laser or measured total disagree by more than TOL. */
    fun totalColour(v: Vals): Int { val ck = v.check; return if (ck != null && abs(ck) > SurveyPrep.TOL) CHECK_COLOUR else COLOUR }

    /** The selection row under the photo (sp.js 1503, ct52 K): "≈905 · ≈2405 · Σ 3310 laser". */
    fun rowValue(v: Vals): String =
        v.est.indices.joinToString(" · ") { segFigure(v, it).first } + " · Σ " + totalText(v)

    // ------------------------------------------------------------ drawing one (sp.js kPlace, kDown, kFinish)

    /** A chain being drawn on [s]: its points so far, what each snapped onto, and its axis once two points fix it. */
    class Draft(val s: String) {
        val nodes: MutableList<DoubleArray> = mutableListOf()
        val tags: MutableList<String?> = mutableListOf()
        var axis: String? = null
    }

    /** Where a point goes, and what it snapped onto. */
    class Placed(val p: DoubleArray, val tag: String?)

    /**
     * POINT [idx] OF [d] AT [mm] (sp.js kPlace): kept on the [a] x [b] face. The FIRST snaps both ways onto the face's
     * and [marks]' edges within [tol] (SurveyGuide.snapCoord), "left corner · floor"; the SECOND fixes the axis --
     * along the face when it is further across than up from the first, else up it -- and every point from the second
     * sits ON that axis through the first, snapped along it.
     */
    fun place(d: Draft, mm: DoubleArray, idx: Int, a: Double, b: Double, marks: List<SurveyPrep.Mark>, tol: Double): Placed {
        val p = doubleArrayOf(mm[0].coerceIn(0.0, a), mm[1].coerceIn(0.0, b))
        if (idx == 0 || d.nodes.isEmpty()) {
            val x = SurveyGuide.snapCoord(d.s, a, b, marks, p[0], 0, tol); val y = SurveyGuide.snapCoord(d.s, a, b, marks, p[1], 1, tol)
            return Placed(doubleArrayOf(x.v, y.v), listOfNotNull(x.tag, y.tag).joinToString(" · ").ifEmpty { null })
        }
        val f = d.nodes[0]
        if (idx == 1) d.axis = if (abs(p[0] - f[0]) >= abs(p[1] - f[1])) "x" else "y"
        val ai = axisIndex(d.axis)
        val sn = SurveyGuide.snapCoord(d.s, a, b, marks, p[ai], ai, tol)
        val q = f.copyOf(); q[ai] = sn.v
        return Placed(q, sn.tag)
    }

    /** What a tap with Chain on did (sp.js kDown, its double tap aside: GestureClassifier reports that). */
    sealed class Press {
        /** A chain is being drawn on another surface: nothing added, [text] said. */
        class Busy(val text: String) : Press()
        /** Point [idx] added to [draft] -- a new draft when there was none. */
        class Added(val draft: Draft, val idx: Int) : Press()
    }

    fun press(d: Draft?, s: String, mm: DoubleArray, a: Double, b: Double, marks: List<SurveyPrep.Mark>, tol: Double): Press {
        if (d != null && d.s != s) return Press.Busy(busy(d.s))
        val dd = d ?: Draft(s)
        val idx = dd.nodes.size
        val q = place(dd, mm, idx, a, b, marks, tol)
        dd.nodes.add(q.p); dd.tags.add(q.tag)
        return Press.Added(dd, idx)
    }

    /** Point [idx], just added, follows the finger to [mm] (sp.js kDown's move); false when it is no longer there. */
    fun move(d: Draft, idx: Int, mm: DoubleArray, a: Double, b: Double, marks: List<SurveyPrep.Mark>, tol: Double): Boolean {
        if (idx !in d.nodes.indices) return false
        val q = place(d, mm, idx, a, b, marks, tol)
        d.nodes[idx] = q.p; d.tags[idx] = q.tag
        return true
    }

    /** A second finger takes back the point the press added, if it is still the last (sp.js kDown's cancel); true when
     *  the draft is then empty, and is dropped. */
    fun cancel(d: Draft, idx: Int): Boolean {
        if (d.nodes.size == idx + 1) { d.nodes.removeAt(idx); d.tags.removeAt(idx) }
        if (d.nodes.isEmpty()) d.axis = null
        return d.nodes.isEmpty()
    }

    /** What ending a draft makes (sp.js kFinish). */
    sealed class Finish {
        /** Nothing plotted; [text] said, or nothing for an empty draft. */
        class Refused(val text: String?) : Finish()
        /** A chain along [axis] through [nodes] (in order along it, 0.01 mm), each with what it snapped onto. */
        class Done(val axis: String, val nodes: List<DoubleArray>, val snaps: List<String?>) : Finish()
    }

    /**
     * A DRAFT ENDED -- a double tap, ✓, or the tool, the face or the step changed with two points or more (sp.js
     * kFinish, kFlush): its points in order along the axis, any within 1 mm of the one before dropped; fewer than two
     * left, nothing is plotted.
     */
    fun finish(d: Draft): Finish {
        if (d.nodes.size < 2) return Finish.Refused(if (d.nodes.isNotEmpty()) NEED_TWO else null)
        val ai = axisIndex(d.axis)
        val pr = d.nodes.indices.map { d.nodes[it] to d.tags.getOrNull(it) }.sortedBy { it.first[ai] }
        val kept = pr.filterIndexed { i, x -> i == 0 || x.first[ai] - pr[i - 1].first[ai] >= 1 }
        if (kept.size < 2) return Finish.Refused(NEED_DIFFERENT)
        return Finish.Done(d.axis ?: "x", kept.map { (p, _) -> doubleArrayOf(Math.round(p[0] * 100) / 100.0, Math.round(p[1] * 100) / 100.0) },
            kept.map { it.second })
    }

    /** The guide while a chain is drawn (sp.js guideNow): through its first point, in its rose, bold on its axis once
     *  fixed ("h" along, "v" up). Null without a point. */
    fun guide(d: Draft?): SurveyGuide.Guide? {
        if (d == null || d.nodes.isEmpty()) return null
        return SurveyGuide.Guide(d.s, d.nodes[0].copyOf(), SurveyPrepPalette.CHAIN,
            when (d.axis) { "y" -> "v"; "x" -> "h"; else -> null })
    }

    // ------------------------------------------------------------ a finished chain's points (sp.js kNodeGrab)

    /**
     * POINT [i] OF [c] DRAGGED TO [mm] (sp.js kNodeGrab): it moves ALONG the chain's axis only, kept between its
     * neighbours (1 mm off each) and on the [a] x [b] face, snapped onto the face's and [marks]' edges within [tol]
     * (SurveyGuide.snapCoord), 0.01 mm; what it snapped onto is kept. A locked chain does not move: false.
     */
    fun nodeMove(s: String, c: SurveyPrep.Chain, i: Int, mm: DoubleArray, a: Double, b: Double,
                 marks: List<SurveyPrep.Mark>, tol: Double): Boolean {
        if (SurveyPrep.isLocked(c) || i !in c.nodes.indices) return false
        val ai = axisIndex(c.axis); val lim = if (ai == 1) b else a
        val lo = if (i > 0) c.nodes[i - 1][ai] + 1 else 0.0
        val hi = if (i < c.nodes.size - 1) c.nodes[i + 1][ai] - 1 else lim
        fun clamp(v: Double) = max(lo, min(hi, v))
        val sn = SurveyGuide.snapCoord(s, a, b, marks, clamp(mm[ai]), ai, tol)
        c.nodes[i][ai] = Math.round(clamp(sn.v) * 100) / 100.0
        while (c.snaps.size < c.nodes.size) c.snaps.add(null)
        c.snaps[i] = sn.tag
        return true
    }

    /** A chain point under a finger at ([x], [y]) canvas px: the nearest within [r] of [chains]' points on screen
     *  (id, points), as (id, index). Null: none. */
    fun nodeAt(chains: List<Pair<String, List<DoubleArray>>>, x: Double, y: Double, r: Double): Pair<String, Int>? {
        var best: Pair<String, Int>? = null; var bd = 0.0
        for ((id, scr) in chains) scr.forEachIndexed { i, q ->
            val d = kotlin.math.hypot(q[0] - x, q[1] - y)
            if (d < r && (best == null || d < bd)) { best = id to i; bd = d }
        }
        return best
    }

    /** With Select on, a chain within [r] of its line (sp.js kHitLine): the nearest's id; null: none. */
    fun pick(chains: List<Pair<String, List<DoubleArray>>>, x: Double, y: Double, r: Double): String? {
        var best: String? = null; var bd = 0.0
        for ((id, scr) in chains) for (i in 1 until scr.size) {
            val d = SurveyMeas.segDist(x, y, scr[i - 1], scr[i])
            if (d < r && (best == null || d < bd)) { best = id; bd = d }
        }
        return best
    }

    // ------------------------------------------------------------ Duplicate (sp.js duplicateItem)

    /** How far a duplicate sits off its original, mm: across the chain's axis. */
    const val DUP_MM = 100.0

    /**
     * DUPLICATE A CHAIN: every point [DUP_MM] off ACROSS its axis (up for a chain along the face, right for one up it) --
     * back the other way where that would pass the [a] x [b] face -- and kept on it; the same axis and snaps; the NEXT
     * chain code (FK1 with FK2 there makes FK3, never one deleted before); no readings; unlocked. Null: no such chain.
     */
    fun duplicate(p: SurveyPrep.Prep, id: String, a: Double, b: Double, newId: String): SurveyPrep.Chain? {
        val (s, c) = p.chainById(id) ?: return null
        if (c.nodes.isEmpty()) return null
        val pi = 1 - axisIndex(c.axis); val lim = if (pi == 1) b else a
        val d = if (c.nodes.maxOf { it[pi] } + DUP_MM <= lim) DUP_MM else -DUP_MM
        return p.addChain(s, c.axis, c.nodes.map { n -> n.copyOf().also { it[pi] = (it[pi] + d).coerceIn(0.0, lim) } }, c.snaps.toList(), newId)
    }
}
