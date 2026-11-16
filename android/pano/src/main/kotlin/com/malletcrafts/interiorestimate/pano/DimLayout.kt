package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch.Box
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * WHERE EVERY FIGURE GOES ON THE PHOTO (port 63 slice M1, 16 Nov 2026; DESIGN-port63 §1.6 and §4). Amit, 15 Nov 2026:
 * "imagemeter shows all dimensions on the annotation there itself which is easy to use." A busy wall -- a door, a
 * window, a switch plate and three electric points -- asks for 19 figures on a photo about 340 dp square, so where
 * the plates go, and which give way, is a rule of its own. It is here, pure and in canvas px, so it is tested in this
 * module; the painter (slice M2) only measures its texts, calls [place] and draws what comes back. Nothing is on
 * screen yet.
 *
 * WHICH FIGURES ([Scope], [shows]):
 *  - [Scope.PICKED] -- 2 · Mark: the picked mark, everything it asks; no other mark shows a figure, as today;
 *  - [Scope.FACE] -- 3 · Measure as it opens: every mark its SIZE (`w` on its top edge, `h` on its right edge), and
 *    only the picked mark its position (corner, floor or ceiling, drop, the `⊥` chip);
 *  - [Scope.ALL] -- 3 · Measure with the `all` toggle on: every figure of every mark.
 *
 * THE PLACING RULE ([place]), in this order. Plates keep a constant size on screen.
 *  1. Place in rank order ([Rank]): the active dimension, the picked mark's others, unmeasured, measured, then the
 *     figures of a mark not in the room.
 *  2. A plate sits mid-span on its line, which it breaks; the `h` plate turned to read bottom to top. A span too
 *     short for it ([SHORT_DP]): the plate steps off the line by one plate height on a short leader, the arrowheads
 *     outside pointing in ([Plate.arrowsIn]).
 *  3. If it would cover a placed plate, or a box it is told to keep clear (the marks' codes, a picked mark's eight
 *     tabs, the fit / grid / all column): ¼ and ¾ along the line, then off it on a leader, either side -- and, with
 *     that taken too, one plate further out again.
 *  4. Still no room: it shrinks to a 6 dp dot on its line -- hollow not measured, solid measured -- with a 32 dp tap
 *     box. THE ACTIVE DIMENSION IS PLACED FIRST AND NEVER SHRINKS.
 *  5. Every plate stays inside the photo.
 * A SMALL BOX -- both spans shorter than their plates plus [SHORT_DP] -- gets ONE compact plate beside it,
 * "≈200 × ≈90", each figure in its own style and its own tap half ([How.COMPACT]). A `p` has no line: a chip at the
 * mark's lower-right corner ([How.CHIP]).
 *
 * IN 2 · MARK THE PICKED MARK KEEPS ITS TABS CLEAR (v52 item 14, Amit's "center handles"; the plan critic's finding
 * 15): for a picked box mark that shows its eight tabs ([Mark.tabs]) the `w` and `h` plates are anchored by
 * [SurveyPrepTouch.widthTag] / [SurveyPrepTouch.heightTag] with `hand = true`, as the painter anchors them today,
 * not mid-span on the line, and the eight tab squares join the boxes to keep clear.
 *
 * The same input gives the same output: no clock, no randomness, and ties keep the order they were given in.
 */
object DimLayout {

    // ------------------------------------------------------------------ the painter's constants (SurveyPrepDraw.dims / vtag)

    /** The `w` line stands this many units above the top edge, the `h` line as many right of the right edge. */
    const val LINE_OFF_U = 6.0
    /** The corner line runs this many text sizes UNDER mid-height (a point's: through its centre) ... */
    const val CORNER_UNDER_FS = 2.6
    /** ... and the floor or ceiling line this many text sizes LEFT of centre (a point's: through its centre). */
    const val FLOOR_LEFT_FS = 1.6
    /** A dimension line is 1 unit wide, with an arrowhead of 4 at each end. */
    const val LINE_U = 1.0
    const val ARROW_U = 4.0
    /** A plate is its text and 10 units across; its text size, 2 units over it and 5 under, up. Corners of 5. */
    const val PAD_U = 10.0
    const val OVER_U = 2.0
    const val UNDER_U = 5.0
    const val RADIUS_U = 5.0
    /** A plate's tap box is its drawn box and 6 units each way -- and never under 32 dp either way. */
    const val TAP_GROW_U = 6.0
    const val TAP_MIN_DP = 32.0
    /** A figure with no room is a 6 dp dot. */
    const val DOT_DP = 6.0
    /** A span shorter than its plate and this much, dp, is too short for the plate to sit on. */
    const val SHORT_DP = 8.0
    /** Plates stay 2 units inside the photo, and 1 unit off each other. */
    const val EDGE_U = 2.0
    const val APART_U = 1.0
    /** A resize tab is a square 5 units either side of its point (SurveyPrepDraw). */
    const val TAB_HALF_U = 5.0
    /** A `p`'s chip and a small box's compact plate stand 4 units off the corner, and off the `h` line. */
    const val GAP_U = 4.0
    /** The picked mark's figures are 12 units high, every other mark's 10. */
    const val TEXT_PICKED_U = 12.0
    const val TEXT_OTHER_U = 10.0

    /** A plate's width for a text [textW] wide, and its height at text size [fs], px. */
    fun plateW(textW: Double, u: Double): Double = textW + PAD_U * u
    fun plateH(fs: Double, u: Double): Double = fs + (OVER_U + UNDER_U) * u

    /** The text size of a figure, px: larger on the picked mark. */
    fun textSize(picked: Boolean, u: Double): Double = (if (picked) TEXT_PICKED_U else TEXT_OTHER_U) * u

    // ------------------------------------------------------------------ which figures, in what order

    /** Which marks show which figures: 2 · Mark, 3 · Measure, and 3 · Measure with `all` on. */
    enum class Scope { PICKED, FACE, ALL }

    /** Whether a figure of [role] is drawn in [scope], on the [picked] mark or on another. */
    fun shows(scope: Scope, picked: Boolean, role: MarkDims.Role): Boolean = when (scope) {
        Scope.PICKED -> picked
        Scope.FACE -> picked || role == MarkDims.Role.WIDTH || role == MarkDims.Role.HEIGHT
        Scope.ALL -> true
    }

    /** The order plates are placed in: an earlier one is never moved for a later one. */
    enum class Rank { ACTIVE, PICKED, UNMEASURED, MEASURED, AWAY }

    /** A figure's rank: the [active] dimension; else the [picked] mark's; else a mark not in the room's ([away]),
     *  last; else by whether it is [measured]. */
    fun rank(active: Boolean, picked: Boolean, away: Boolean, measured: Boolean): Rank = when {
        active -> Rank.ACTIVE
        picked -> Rank.PICKED
        away -> Rank.AWAY
        measured -> Rank.MEASURED
        else -> Rank.UNMEASURED
    }

    // ------------------------------------------------------------------ lines on the canvas

    /** A segment, canvas px. */
    class Seg(val x1: Double, val y1: Double, val x2: Double, val y2: Double) {
        val len: Double get() = hypot(x2 - x1, y2 - y1)
        /** Up the canvas rather than across it. */
        val vertical: Boolean get() = abs(y2 - y1) > abs(x2 - x1)
        /** The point a part [t] (0 … 1) of the way along. */
        fun at(t: Double): DoubleArray = doubleArrayOf(x1 + (x2 - x1) * t, y1 + (y2 - y1) * t)
        override fun toString() = "Seg($x1, $y1 -> $x2, $y2)" // text:value a test failure's own words, never on screen
    }

    /**
     * WHERE A DIMENSION'S LINE RUNS ON THE CANVAS, where 0.3.332 draws it: [d]'s line on the face through [toPx] (face
     * mm to canvas px, y down), then `w` [LINE_OFF_U] units above the top edge, `h` as far right of the right edge,
     * the corner line [CORNER_UNDER_FS] text sizes under mid-height and the floor or ceiling line [FLOOR_LEFT_FS] left
     * of centre -- both through the centre for a [point]. Null for a `p`, which has no line ([anchor]).
     */
    fun line(d: MarkDims.Dim, point: Boolean, toPx: (Double, Double) -> DoubleArray, fs: Double, u: Double): Seg? {
        if (!d.line) return null
        val p = toPx(d.x1, d.y1); val q = toPx(d.x2, d.y2)
        var dx = 0.0; var dy = 0.0
        when (d.role) {
            MarkDims.Role.WIDTH -> dy = -LINE_OFF_U * u
            MarkDims.Role.HEIGHT -> dx = LINE_OFF_U * u
            MarkDims.Role.OFF_H -> if (!point) dy = CORNER_UNDER_FS * fs
            MarkDims.Role.OFF_V -> if (!point) dx = -FLOOR_LEFT_FS * fs
            else -> {}
        }
        return Seg(p[0] + dx, p[1] + dy, q[0] + dx, q[1] + dy)
    }

    /** Where a `p`'s chip is anchored on the canvas: the mark's lower-right corner. */
    fun anchor(d: MarkDims.Dim, toPx: (Double, Double) -> DoubleArray): DoubleArray = toPx(d.x1, d.y1)

    // ------------------------------------------------------------------ what goes in

    /**
     * ONE FIGURE TO PLACE: its reading key [code] and [role]; its [line] on the canvas, or for a `p` the corner it is
     * anchored [at]; its plate's width [w] ([plateW] of its text) and text size [fs], px; its [rank]; and whether it
     * is [measured] (a dot is solid then, hollow otherwise). What the painter then draws on the plate rides along,
     * untouched by the placing: the [text] as [MarkDims.shown] prints it, the figure's [state] (its
     * [MarkDims.style]), whether the `!` follows it ([check]) and whether it is the one being measured ([active]).
     */
    class Fig(val code: String, val role: MarkDims.Role, val line: Seg?, val at: DoubleArray?,
              val w: Double, val fs: Double, val rank: Rank, val measured: Boolean,
              val text: String = "", val state: MarkDims.State = if (measured) MarkDims.State.MEASURED else MarkDims.State.ESTIMATE,
              val check: Boolean = false, val active: Boolean = rank == Rank.ACTIVE)

    /**
     * ONE MARK'S FIGURES: its [id], its [box] as drawn on the canvas, whether it is the [picked] one, and whether it
     * shows its eight resize [tabs] -- a picked box mark in 2 · Mark that is not a point, not locked and editable.
     */
    class Mark(val id: String, val box: Box, val figs: List<Fig>, val picked: Boolean = false, val tabs: Boolean = false)

    /**
     * THE MARK [e] ON [s] AS [place] TAKES IT: one [Fig] per [MarkDims.of], its text [MarkDims.shown] (with the `!`
     * after it where the check fires), measured by [textW] (text, text size px, bold) and sized by [plateW]; its line
     * by [line]; its rank by [rank] -- [active] the reading key being measured now. The painter builds its input
     * here, so which figure, text and style a plate has is decided once, in this module.
     */
    fun mark(actual: Map<String, Double>, s: String, e: SurveyPrep.Mark, a: Double, b: Double, picked: Boolean, tabs: Boolean,
             active: String?, toPx: (Double, Double) -> DoubleArray, u: Double,
             textW: (text: String, fs: Double, bold: Boolean) -> Double): Mark {
        val pt = SurveyPrep.isPoint(e.t); val away = !MarkDims.asked(e); val fs = textSize(picked, u)
        val figs = MarkDims.of(s, e, a, b).map { d ->
            val v = MarkDims.value(actual, s, e, d.code, d.est, a, b)
            val on = d.code == active
            val text = MarkDims.shown(v, d.role, on) + (if (v.check) MarkDims.CHECK_GLYPH else "")
            val measured = v.state == MarkDims.State.MEASURED
            Fig(d.code, d.role, line(d, pt, toPx, fs, u), if (d.line) null else anchor(d, toPx),
                plateW(textW(text, fs, MarkDims.style(v.state, on).bold), u), fs, rank(on, picked, away, measured), measured,
                MarkDims.shown(v, d.role, on), v.state, v.check, on)
        }
        val p = toPx(e.x0, e.y1); val q = toPx(e.x1, e.y0)
        return Mark(e.id, Box(min(p[0], q[0]), min(p[1], q[1]), max(p[0], q[0]), max(p[1], q[1])), figs, picked, tabs)
    }

    // ------------------------------------------------------------------ what comes out

    /** How a figure ended up. */
    enum class How {
        /** On its line, which it breaks: mid-span, or ¼ or ¾ along. */
        ON_LINE,
        /** Off its line by one plate height, on a short [Plate.leader]. */
        OFF_LINE,
        /** The picked mark's `w` or `h` in 2 · Mark, anchored clear of its tabs. */
        HAND,
        /** A `p`: a chip at the mark's lower-right corner. */
        CHIP,
        /** Half of a small box's one compact plate: [Plate.whole] is the plate, [Plate.box] this figure's half. */
        COMPACT,
        /** No room: a 6 dp dot on its line. */
        DOT,
    }

    /**
     * ONE FIGURE AS PLACED: the [fig] it is, of the mark [markId]; the box drawn ([box]; a dot's 6 dp square; a
     * compact half's part of [whole]), its tap box ([tap], at least 32 dp each way), whether its text is [turned] to
     * read bottom to top, the [leader] from its line to a plate that stepped off it, and whether the line's arrowheads
     * stand outside pointing in ([arrowsIn]: the span is too short for the plate).
     */
    class Plate(val fig: Fig, val markId: String, val how: How, val box: Box, val tap: Box,
                val turned: Boolean, val leader: Seg?, val arrowsIn: Boolean, val whole: Box?) {
        val code: String get() = fig.code
        val role: MarkDims.Role get() = fig.role
        /** Its dimension line on the canvas; null for a `p`. */
        val line: Seg? get() = fig.line
        val measured: Boolean get() = fig.measured
        val dot: Boolean get() = how == How.DOT
    }

    /** Where an upright plate's text starts and sits: half its padding inside the box, [UNDER_U] above its bottom. */
    fun textLeft(box: Box, u: Double): Double = box.l + PAD_U * u / 2
    fun baseline(box: Box, u: Double): Double = box.b - UNDER_U * u

    /** Every figure placed, in the order given (marks, then each mark's figures). */
    class Out(val plates: List<Plate>) {
        fun of(code: String): Plate? = plates.firstOrNull { it.code == code }
        /** The tap targets, as [SurveyPrepTouch.hit] takes them: every plate, compact half and dot under its reading key. */
        val tags: List<SurveyPrepTouch.Tag> get() = plates.map { SurveyPrepTouch.Tag(it.code, it.tap) }
        /** How many shrank to a dot. */
        val dots: Int get() = plates.count { it.dot }
    }

    // ------------------------------------------------------------------ the rule

    private fun overlaps(a: Box, b: Box, apart: Double): Boolean =
        a.l < b.r + apart && a.r > b.l - apart && a.t < b.b + apart && a.b > b.t - apart

    /** A [w] x [h] box centred on ([cx], [cy]), slid back inside the [width] x [height] canvas ([EDGE_U] inside it). */
    private fun boxAt(cx: Double, cy: Double, w: Double, h: Double, width: Double, height: Double, u: Double): Box {
        val e = EDGE_U * u
        val l = max(e, min(width - w - e, cx - w / 2)); val t = max(e, min(height - h - e, cy - h / 2))
        return Box(l, t, l + w, t + h)
    }

    /** A drawn box's tap box: [TAP_GROW_U] units each way, and never under [TAP_MIN_DP] dp either way. */
    fun tapOf(b: Box, u: Double, dp: Double): Box {
        val gx = max(TAP_GROW_U * u, (TAP_MIN_DP * dp - b.w) / 2); val gy = max(TAP_GROW_U * u, (TAP_MIN_DP * dp - b.h) / 2)
        return Box(b.l - gx, b.t - gy, b.r + gx, b.b + gy)
    }

    /** The eight resize tabs of a mark drawn as [b], as squares ([TAB_HALF_U]). */
    fun tabBoxes(b: Box, u: Double): List<Box> = SurveyPrepTouch.TABS.map { part ->
        val p = SurveyPrepTouch.tabAt(part, b.l, b.r, b.t, b.b); val h = TAB_HALF_U * u
        Box(p[0] - h, p[1] - h, p[0] + h, p[1] + h)
    }

    /** A plate's extent ALONG its line: its width, but the plate's height for an unturned plate on a vertical line. */
    private fun along(f: Fig, turned: Boolean, u: Double): Double =
        if (turned || f.line?.vertical != true) f.w else plateH(f.fs, u)

    /** The `h` plate is turned to read bottom to top; every other plate is upright. */
    private fun turns(f: Fig): Boolean = f.role == MarkDims.Role.HEIGHT

    /** The span is too short for the plate to sit on (rule 2; and for both of a small box's, the compact plate). */
    private fun short(f: Fig, u: Double, dp: Double): Boolean {
        val ln = f.line ?: return false
        return ln.len < along(f, turns(f), u) + SHORT_DP * dp
    }

    private class Cand(val box: Box, val how: How, val turned: Boolean, val leader: Seg?)

    /** The side a plate steps off its line to first: away from the mark for its size, up for a corner line, left for a
     *  floor or ceiling line. -1 or +1 along the canvas axis across the line. */
    private fun side(role: MarkDims.Role): Double = when (role) {
        MarkDims.Role.HEIGHT, MarkDims.Role.DROP -> 1.0
        else -> -1.0
    }

    private fun lineCands(mk: Mark, f: Fig, hand: Boolean, width: Double, height: Double, u: Double, dp: Double): List<Cand> {
        val ln = f.line ?: return emptyList()
        val out = ArrayList<Cand>()
        val ph = plateH(f.fs, u)
        if (hand && (f.role == MarkDims.Role.WIDTH || f.role == MarkDims.Role.HEIGHT)) {
            val a = if (f.role == MarkDims.Role.WIDTH) SurveyPrepTouch.widthTag(mk.box.l, mk.box.t, mk.box.r, f.fs, u, dp, true)
                    else SurveyPrepTouch.heightTag(mk.box.t, mk.box.r, mk.box.b, f.fs, u, dp, true)
            val d = SurveyPrepTouch.tagShape(a, f.w, f.fs, u, width, height).drawn
            out += Cand(boxAt((d.l + d.r) / 2, (d.t + d.b) / 2, d.w, d.h, width, height, u), How.HAND, false, null)
        }
        // a HAND plate that finds no room falls back as any plate does; it is then upright, as the anchor's is
        val turned = turns(f) && !hand
        val bw = if (turned) ph else f.w; val bh = if (turned) f.w else ph
        val al = along(f, turned, u); val len = ln.len
        val ts = doubleArrayOf(0.5, 0.25, 0.75)
        for (t in ts) {
            // on the line: the whole plate within the span, half of SHORT_DP clear of each end
            if (min(t, 1 - t) * len < al / 2 + SHORT_DP * dp / 2) continue
            val c = ln.at(t)
            out += Cand(boxAt(c[0], c[1], bw, bh, width, height, u), How.ON_LINE, turned, null)
        }
        // off the line: half a plate height clear of it (centre to line, one plate height for a plate lying along it),
        // either side; and, with that taken too, one plate further out again
        val first = side(f.role); val across = if (ln.vertical) bw else bh
        for (reach in doubleArrayOf(across / 2 + ph / 2, across / 2 + ph / 2 + across + APART_U * u))
            for (t in ts) for (sd in doubleArrayOf(first, -first)) {
                val c = ln.at(t)
                val bx = if (ln.vertical) boxAt(c[0] + sd * reach, c[1], bw, bh, width, height, u)
                         else boxAt(c[0], c[1] + sd * reach, bw, bh, width, height, u)
                // the leader: from the line to the nearest point of the plate
                val near = doubleArrayOf(c[0].coerceIn(bx.l, bx.r), c[1].coerceIn(bx.t, bx.b))
                out += Cand(bx, How.OFF_LINE, turned, Seg(c[0], c[1], near[0], near[1]))
            }
        return out
    }

    private fun chipCands(f: Fig, hand: Boolean, width: Double, height: Double, u: Double, dp: Double): List<Cand> {
        val at = f.at ?: return emptyList()
        // off the corner by GAP_U -- and on a mark that shows its tabs, by what its w and h tags stand off them
        val g = if (hand) SurveyPrepTouch.clearance(u, dp) else GAP_U * u
        val w = f.w; val h = plateH(f.fs, u); val x = at[0]; val y = at[1]
        fun c(l: Double, t: Double) = Cand(boxAt(l + w / 2, t + h / 2, w, h, width, height, u), How.CHIP, false, null)
        // outside the corner below and right; inside it; below, inside the right edge; right, above the corner
        return listOf(c(x + g, y + g), c(x - g - w, y - g - h), c(x - g - w, y + g), c(x + g, y - g - h))
    }

    private fun dot(f: Fig, mk: Mark, width: Double, height: Double, u: Double, dp: Double, arrowsIn: Boolean): Plate {
        val c = f.line?.at(0.5) ?: f.at ?: doubleArrayOf((mk.box.l + mk.box.r) / 2, (mk.box.t + mk.box.b) / 2)
        val r = DOT_DP * dp / 2
        val x = max(r, min(width - r, c[0])); val y = max(r, min(height - r, c[1]))
        val b = Box(x - r, y - r, x + r, y + r)
        return Plate(f, mk.id, How.DOT, b, tapOf(b, u, dp), false, null, arrowsIn, null)
    }

    /**
     * EVERY FIGURE PLACED. [marks] in drawing order, each with all its figures -- [scope] decides which are shown
     * ([shows]); the canvas is [width] x [height] px; [keep] the boxes no plate may cover (the marks' codes, the fit /
     * grid / all column, whatever else the screen passes); [u] the drawing's unit and [dp] px per dp; [times] the
     * width of the " × " between a compact plate's two figures, px. A figure [scope] does not show is not in the
     * answer at all.
     */
    fun place(marks: List<Mark>, scope: Scope, width: Double, height: Double, keep: List<Box>, u: Double, dp: Double,
              times: Double = 0.0): Out {
        class Job(val mk: Mark, val figs: List<Fig>, val rank: Rank, val order: Int, val hand: Boolean)
        val jobs = ArrayList<Job>()
        val blocked = ArrayList(keep)
        var n = 0
        for (mk in marks) {
            val shown = mk.figs.filter { shows(scope, mk.picked, it.role) }
            val hand = scope == Scope.PICKED && mk.picked && mk.tabs
            if (hand) blocked += tabBoxes(mk.box, u)
            val w = shown.firstOrNull { it.role == MarkDims.Role.WIDTH && it.line != null }
            val h = shown.firstOrNull { it.role == MarkDims.Role.HEIGHT && it.line != null }
            // a small box: both spans too short for their plates -- one compact plate beside it
            val pair = if (!hand && w != null && h != null && short(w, u, dp) && short(h, u, dp)) listOf(w, h) else null
            if (pair != null) jobs += Job(mk, pair, minOf(w!!.rank, h!!.rank), n++, false)
            for (f in shown) if (pair == null || f !in pair) jobs += Job(mk, listOf(f), f.rank, n++, hand)
        }
        val apart = APART_U * u
        val placed = ArrayList<Box>()
        fun free(b: Box) = placed.none { overlaps(b, it, apart) } && blocked.none { overlaps(b, it, 0.0) }
        val out = HashMap<String, Plate>()
        for (j in jobs.sortedWith(compareBy<Job> { it.rank.ordinal }.thenBy { it.order })) {
            val mk = j.mk
            if (j.figs.size == 2) {
                // THE COMPACT PLATE: "≈200 × ≈90" right of the box, else left of it, above it, below it
                val w = j.figs[0]; val h = j.figs[1]
                val cw = w.w + times + h.w; val ch = max(plateH(w.fs, u), plateH(h.fs, u))
                val g = (LINE_OFF_U + GAP_U) * u; val b = mk.box
                val cx = (b.l + b.r) / 2; val cy = (b.t + b.b) / 2
                val cands = listOf(
                    boxAt(b.r + g + cw / 2, cy, cw, ch, width, height, u), boxAt(b.l - g - cw / 2, cy, cw, ch, width, height, u),
                    boxAt(cx, b.t - g - ch / 2, cw, ch, width, height, u), boxAt(cx, b.b + g + ch / 2, cw, ch, width, height, u))
                val whole = cands.firstOrNull { free(it) } ?: if (j.rank == Rank.ACTIVE) cands[0] else null
                if (whole == null) {
                    for (f in j.figs) out[f.code] = dot(f, mk, width, height, u, dp, true)
                    continue
                }
                placed += whole
                val split = whole.l + w.w + times / 2
                val tap = tapOf(whole, u, dp); val m = TAP_MIN_DP * dp
                out[w.code] = Plate(w, mk.id, How.COMPACT, Box(whole.l, whole.t, whole.l + w.w, whole.b),
                    Box(min(tap.l, split - m), tap.t, split, tap.b), false, null, true, whole)
                out[h.code] = Plate(h, mk.id, How.COMPACT, Box(whole.r - h.w, whole.t, whole.r, whole.b),
                    Box(split, tap.t, max(tap.r, split + m), tap.b), false, null, true, whole)
                continue
            }
            val f = j.figs[0]
            val tooShort = short(f, u, dp)
            val cands = if (f.line != null) lineCands(mk, f, j.hand, width, height, u, dp) else chipCands(f, j.hand, width, height, u, dp)
            val c = cands.firstOrNull { free(it.box) } ?: if (j.rank == Rank.ACTIVE) cands.firstOrNull() else null
            if (c == null) { out[f.code] = dot(f, mk, width, height, u, dp, tooShort); continue }
            placed += c.box
            out[f.code] = Plate(f, mk.id, c.how, c.box, tapOf(c.box, u, dp), c.turned, c.leader, tooShort, null)
        }
        return Out(marks.flatMap { mk -> mk.figs.mapNotNull { out[it.code] } })
    }
}
