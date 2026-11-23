package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch.Box
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * WHERE EVERY FIGURE GOES ON THE PHOTO (port 63 slice M1, 16 Nov 2026; DESIGN-port63 §1.6 and §4). Amit, 15 Nov 2026:
 * "imagemeter shows all dimensions on the annotation there itself which is easy to use." A busy wall -- a door, a
 * window, a switch plate and three electric points -- asks for 19 figures on a photo about 340 dp square, so where
 * the plates go, and which give way, is a rule of its own. It is here, pure and in canvas px, so it is tested in this
 * module; the painter (slice M2) only measures its texts, calls [placeShown] and draws what comes back. Nothing is on
 * screen yet.
 *
 * WHICH FIGURES are drawn is decided before this is asked ([markShown], [PhotoFigures]; since slice I1b there is no
 * scope and no `all`: every mark shows every figure it has been READ, the picked mark everything it asks).
 *
 * THE PLACING RULE ([placeShown]; [placeBy] its level-plate form), in this order. Plates keep a constant size on screen.
 *  1. Place in rank order ([Rank]): the active dimension, the picked mark's others, unmeasured, measured, then the
 *     figures of a mark not in the room.
 *  2. A plate sits mid-span on its line, which it breaks; the `h` plate turned to read bottom to top. A span too
 *     short for it ([SHORT_DP]): the plate steps off the line by one plate height on a short leader, the arrowheads
 *     outside pointing in ([Plate.arrowsIn]).
 *  3. If it would cover a placed plate, or a box it is told to keep clear (the marks' codes, a picked mark's eight
 *     tabs, the fit / grid / all column): ¼ and ¾ along the line, then off it on a leader, either side -- and, with
 *     that taken too, one plate further out again.
 *  4. Still no room: it shrinks to a 6 dp dot on its line -- hollow not measured, solid measured -- with a 32 dp tap
 *     box. THE ACTIVE DIMENSION IS PLACED FIRST AND NEVER SHRINKS. A dot looks for its own clear spot: the middle of
 *     the part of its line the canvas shows, then along that part either way, a quarter dot at a time (a `p`: the
 *     mark's four corners), clear of the plates and dots already placed and of the kept boxes -- beside its line
 *     when the whole of it lies under plates; it then keeps that spot, so no later plate covers it.
 *  5. Every plate stays inside the photo.
 *
 * THE CANVAS IS THE VIEWPORT, NOT THE WALL (the review of M1, 16 Nov 2026). The painter maps the face through zoom
 * and pan, so a mark can be wholly off the canvas. A figure whose dimension -- the span it measures on the mark, or a
 * `p`'s corner ([Fig.span], [Fig.at]) -- lies wholly outside the canvas IS NOT PLACED: no plate, no dot, no tap box.
 * Two exceptions, slid into view as the picked mark's tags are today: the ACTIVE figure, and the PICKED mark's. Each
 * says so ([Plate.off]), and [Out.activeOff] tells the screen that the figure being measured is off the canvas, so
 * it can pan to it (§1.4: "The photo pans only if that plate is off screen"). A dot is never slid onto the canvas
 * from outside it: with its dimension off the canvas it stays on its own line, unseen.
 * A SMALL BOX -- both spans shorter than their plates plus [SHORT_DP] -- gets ONE compact plate beside it,
 * "≈200 × ≈90", each figure in its own style and its own tap half ([How.COMPACT]). A `p` has no line: a chip at the
 * mark's lower-right corner ([How.CHIP]).
 *
 * TWO UNITS, AND WHICH IS WHICH (the review of M1). The painter's unit [u] is the canvas / 480 -- on the Mi A3's
 * 340 dp photo 0.71 dp -- and follows the canvas, so it is smaller again in landscape. The LINES are in units, where
 * build 332 draws them. A PLATE IS IN dp, so it keeps a constant size on screen whatever the canvas: its figure the
 * design's 12 sp on the picked mark and 10 sp on every other (§1.6; [TEXT_PICKED_DP], [TEXT_OTHER_DP] -- dp, since
 * nothing drawn on the photo follows the system's font scale), its padding to match ([PAD_DP], [OVER_DP],
 * [UNDER_DP]). Sized in units a figure was 8.5 dp on the picked mark and 7 dp on every other.
 *
 * IN 2 · MARK THE PICKED MARK KEEPS ITS TABS CLEAR (v52 item 14, Amit's "center handles"; the plan critic's finding
 * 15): for a picked box mark that shows its eight tabs ([Mark.tabs]) the `w` and `h` plates are anchored by
 * [SurveyPrepTouch.widthTag] / [SurveyPrepTouch.heightTag] with `hand = true`, as the painter anchors them today,
 * not mid-span on the line, and the eight tab squares join the boxes to keep clear.
 *
 * A MARK WITH FOUR FREE CORNERS (port 63 slice Q1, 16 Nov 2026; DESIGN-quad §3.5) is placed by the same rule: its four
 * sides ([MarkDims.Role.SIDE]) each on a line parallel to its side and [LINE_OFF_U] units outside the shape, the plate
 * mid-span -- upright, or turned to read bottom to top on a side more upright than level, never turned to the slope --
 * and its diagonal ([MarkDims.Role.DIAG]) from q0 to q2 inside it. They are its SIZE: every mark of the face shows them
 * ([isSize]). It has no `w` and no `h`, so no hand-anchored tag and no compact plate: a very small one falls to
 * leaders and dots. Picked in 2 · Mark with its corners free it keeps its four corner handles clear ([Mark.corners]),
 * not eight tabs.
 *
 * FROM SLICE I1b THERE IS NO SCOPE (port 63 slice I1a; DESIGN-imagemeter §13, §18): the one step that marks and
 * measures hands [placeShown] only the figures the photo draws -- an unpicked mark what has been READ (since slice I1c
 * its position too), the picked mark everything it asks, with no photo estimate anywhere ([markShown], [PhotoFigures]),
 * every Tape, Line, area and chain figure with them ([item]), each plate TURNED ALONG ITS LINE -- and the HELD mark,
 * the one that shows its tabs, keeps them clear. The scope of the four-step route (its Scope, shows and `place`) went in
 * slice I1b; the placing rule itself is one function, [placeShown] and [placeBy] its two forms. [mark] and [placeBy]
 * have no caller on the screen: they are the level-plate form the placing rule's own tests are written in.
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
    /** A plate is its text and 10 dp across; its text size, 2 dp over it and 5 under, up. Corners of 5 dp. */
    const val PAD_DP = 10.0
    const val OVER_DP = 2.0
    const val UNDER_DP = 5.0
    const val RADIUS_DP = 5.0
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
    /** The picked mark's figures are 12 dp high, every other mark's 10 (the design's 12 sp and 10 sp). */
    const val TEXT_PICKED_DP = 12.0
    const val TEXT_OTHER_DP = 10.0
    /** The corner line and the floor or ceiling line stand off the mark's middle by so many "text sizes"
     *  ([CORNER_UNDER_FS], [FLOOR_LEFT_FS]) of THIS size, units: the 12 units build 332 draws a picked mark's figures
     *  at -- so the lines stay where that build draws them, and do not move when the mark is picked. */
    const val LINE_FS_U = 12.0

    /** A plate's width for a text [textW] wide, and its height at text size [fs], px; [dp] px a dp. */
    fun plateW(textW: Double, dp: Double): Double = textW + PAD_DP * dp
    fun plateH(fs: Double, dp: Double): Double = fs + (OVER_DP + UNDER_DP) * dp

    /** The text size of a figure, px: larger on the picked mark; [dp] px a dp. */
    fun textSize(picked: Boolean, dp: Double): Double = (if (picked) TEXT_PICKED_DP else TEXT_OTHER_DP) * dp

    // ------------------------------------------------------------------ which figures, in what order

    /** A mark's SIZE: its `w` and `h` -- and a four-corner mark's four sides and its diagonal, which are its size (port
     *  63 slice Q1; DESIGN-quad §3.5), so every mark of the face shows them, picked or not. */
    fun isSize(role: MarkDims.Role): Boolean =
        role == MarkDims.Role.WIDTH || role == MarkDims.Role.HEIGHT || role == MarkDims.Role.SIDE || role == MarkDims.Role.DIAG

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
     * of centre -- both through the centre for a [point]; [fs] that text size, px ([LINE_FS_U] units, not the plate's
     * own). Null for a `p`, which has no line ([anchor]).
     *
     * A FOUR-CORNER MARK'S (port 63 slice Q1; DESIGN-quad §3.5): a SIDE's line runs parallel to its side, [LINE_OFF_U]
     * units OUTSIDE the shape ([outward]); its DIAGONAL runs from q0 to q2 inside it, unshifted; and its position lines
     * run to the corner and are not shifted off-centre, as a point's are not -- its caller passes [point] true for them.
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
            MarkDims.Role.SIDE -> outward(d, toPx)?.let { n -> dx = n[0] * LINE_OFF_U * u; dy = n[1] * LINE_OFF_U * u }
            else -> {}
        }
        return Seg(p[0] + dx, p[1] + dy, q[0] + dx, q[1] + dy)
    }

    /**
     * THE WAY OUT OF THE SHAPE ACROSS A SIDE, on the canvas: the unit vector square to [d]'s line there that points the
     * way its face normal ([MarkDims.Dim.nx], [MarkDims.Dim.ny]) does. Null for a dimension that is not a side, and for
     * a side with no length on the canvas.
     */
    fun outward(d: MarkDims.Dim, toPx: (Double, Double) -> DoubleArray): DoubleArray? {
        if (d.role != MarkDims.Role.SIDE) return null
        val p = toPx(d.x1, d.y1); val q = toPx(d.x2, d.y2)
        val len = hypot(q[0] - p[0], q[1] - p[1])
        if (!(len > 0)) return null
        val nx = -(q[1] - p[1]) / len; val ny = (q[0] - p[0]) / len
        // which of the two ways across the line is out: the one the face's own normal points, seen on the canvas
        val o = toPx(d.x1 + d.nx, d.y1 + d.ny)
        return if ((o[0] - p[0]) * nx + (o[1] - p[1]) * ny >= 0) doubleArrayOf(nx, ny) else doubleArrayOf(-nx, -ny)
    }

    /** Where a `p`'s chip is anchored on the canvas: the mark's lower-right corner. */
    fun anchor(d: MarkDims.Dim, toPx: (Double, Double) -> DoubleArray): DoubleArray = toPx(d.x1, d.y1)

    /** The span [d] measures, on the canvas and unshifted -- the mark's own edge for its size, the gap to the corner,
     *  floor or ceiling for its position. Null for a `p`. Whether a figure is on the canvas is asked of THIS, not of
     *  its [line]: a mark against the ceiling has its `w` line 6 units above the canvas and is still there to measure. */
    fun span(d: MarkDims.Dim, toPx: (Double, Double) -> DoubleArray): Seg? {
        if (!d.line) return null
        val p = toPx(d.x1, d.y1); val q = toPx(d.x2, d.y2)
        return Seg(p[0], p[1], q[0], q[1])
    }

    /** The part of [s] inside the [width] x [height] canvas, its edges included; null when none of it is. */
    fun clip(s: Seg, width: Double, height: Double): Seg? {
        var t0 = 0.0; var t1 = 1.0
        // one edge of the canvas: the segment moves p across it for its whole length, and starts q inside it
        fun edge(p: Double, q: Double): Boolean {
            if (p == 0.0) return q >= 0
            val t = q / p
            if (p < 0) { if (t > t1) return false; if (t > t0) t0 = t } else { if (t < t0) return false; if (t < t1) t1 = t }
            return true
        }
        if (!edge(s.x1 - s.x2, s.x1) || !edge(s.x2 - s.x1, width - s.x1) || !edge(s.y1 - s.y2, s.y1) || !edge(s.y2 - s.y1, height - s.y1)) return null
        val a = s.at(t0); val b = s.at(t1)
        return Seg(a[0], a[1], b[0], b[1])
    }

    private fun inside(p: DoubleArray, width: Double, height: Double) = p[0] >= 0 && p[0] <= width && p[1] >= 0 && p[1] <= height

    /** Whether any of the figure's dimension is on the [width] x [height] canvas: some of its [Fig.span], or a `p`'s
     *  corner. A figure with neither is taken as there. */
    fun onCanvas(f: Fig, width: Double, height: Double): Boolean =
        f.span?.let { clip(it, width, height) != null } ?: f.at?.let { inside(it, width, height) } ?: true

    // ------------------------------------------------------------------ what goes in

    /**
     * ONE FIGURE TO PLACE: its reading key [code] and [role]; its [line] on the canvas, or for a `p` the corner it is
     * anchored [at]; its plate's width [w] ([plateW] of its text) and text size [fs], px; its [rank]; and whether it
     * is [measured] (a dot is solid then, hollow otherwise). What the painter then draws on the plate rides along,
     * untouched by the placing: the [text] as [MarkDims.shown] prints it, the figure's [state] (its
     * [MarkDims.style]), whether the `!` follows it ([check]) and whether it is the one being measured ([active]).
     * [typing]: the text is the pad's uncommitted digits, not the stored figure ([mark]'s `live`). [span]: what the
     * dimension measures on the canvas, unshifted ([DimLayout.span]) -- what [onCanvas] asks; its line when not given.
     * [out]: a four-corner mark's SIDE only -- the way out of the shape across it, on the canvas ([outward]), which is
     * the side its plate steps off to first; null for every other figure.
     * [taps] (port 63 slice I1c): whether its plate is a tap target ([Out.tags]); false only for a figure that takes
     * no reading -- a Tape's, which IS the photo's -- placed so that nothing lies on it, and touched by nobody.
     */
    class Fig(val code: String, val role: MarkDims.Role, val line: Seg?, val at: DoubleArray?,
              val w: Double, val fs: Double, val rank: Rank, val measured: Boolean,
              val text: String = "", val state: MarkDims.State = if (measured) MarkDims.State.MEASURED else MarkDims.State.ESTIMATE,
              val check: Boolean = false, val active: Boolean = rank == Rank.ACTIVE, val typing: Boolean = false,
              val span: Seg? = line, val out: DoubleArray? = null, val taps: Boolean = true)

    /**
     * ONE MARK'S FIGURES: its [id], its [box] as drawn on the canvas, whether it is the [picked] one, and whether it
     * shows its eight resize [tabs] -- a picked box mark in 2 · Mark that is not a point, not locked and editable.
     * [corners] (port 63 slice Q1; DESIGN-quad §3.5): such a mark whose corners are FREE shows its four corners
     * instead -- q0 … q3 on the canvas -- and no tabs; null for every other mark. [box] is then its bounding box.
     */
    class Mark(val id: String, val box: Box, val figs: List<Fig>, val picked: Boolean = false, val tabs: Boolean = false,
               val corners: List<DoubleArray>? = null)

    /**
     * THE MARK [e] ON [s] AS [placeBy] TAKES IT: one [Fig] per [MarkDims.of], its text [MarkDims.shown] (with the `!`
     * after it where the check fires), measured by [textW] (text, text size px, bold) and sized by [plateW]; its line
     * by [line]; its rank by [rank] -- [active] the reading key being measured now. The painter builds its input
     * here, so which figure, text and style a plate has is decided once, in this module. [u] the drawing's unit and
     * [dp] px a dp.
     *
     * [live] is the pad's uncommitted text, [MeasurePad.live]'s value (§1.4: "From the first key press the digits show
     * live in the bar's box and on the gold plate"). Null -- the pad is closed or untouched -- and every figure is the
     * stored one. Digits: the ACTIVE figure's plate reads the active glyph and those digits (a `p`'s after its `⊥`),
     * is sized from that text, is drawn as a reading (white, bold, on the gold plate) and carries no `!`, since
     * nothing has been checked yet; [Fig.typing] says so. An emptied pad (`""`) keeps the figure the plate had until
     * the commit. Whether the dimension is [Fig.measured] stays what is STORED. No other figure is touched.
     *
     * [free] (port 63 slice Q1; DESIGN-quad §3.1, §3.5): the mark's corners are free ([Quad.free], the one rule the
     * painter, the press and this ask). Where it would show its tabs ([tabs]) it then shows its four CORNERS instead
     * ([Mark.corners]), which [placeBy] keeps clear in the tabs' place. The LAST parameter, false by default: every
     * caller that passes the thirteen before it by position reads as it did. A four-corner mark's position lines are
     * not shifted off-centre, whether or not its corners are shown.
     */
    fun mark(actual: Map<String, Double>, s: String, e: SurveyPrep.Mark, a: Double, b: Double, picked: Boolean, tabs: Boolean,
             active: String?, toPx: (Double, Double) -> DoubleArray, u: Double, dp: Double,
             textW: (text: String, fs: Double, bold: Boolean) -> Double, live: String? = null, free: Boolean = false): Mark {
        // a point's position lines run through its centre and a four-corner mark's to its corner: neither is shifted
        val pt = SurveyPrep.isPoint(e.t) || Quad.of(e) != null; val away = !MarkDims.asked(e); val fs = textSize(picked, dp)
        val figs = MarkDims.of(s, e, a, b).map { d ->
            val stored = MarkDims.value(actual, s, e, d.code, d.est, a, b)
            val on = d.code == active
            val typing = on && !live.isNullOrEmpty()
            val v = if (typing) MarkDims.Value(live!!, MarkDims.State.MEASURED, false) else stored
            val text = MarkDims.shown(v, d.role, on) + (if (v.check) MarkDims.CHECK_GLYPH else "")
            val measured = stored.state == MarkDims.State.MEASURED
            Fig(d.code, d.role, line(d, pt, toPx, LINE_FS_U * u, u), if (d.line) null else anchor(d, toPx),
                plateW(textW(text, fs, MarkDims.style(v.state, on).bold), dp), fs, rank(on, picked, away, measured), measured,
                MarkDims.shown(v, d.role, on), v.state, v.check, on, typing, span(d, toPx), outward(d, toPx))
        }
        val p = toPx(e.x0, e.y1); val q = toPx(e.x1, e.y0)
        val handles = tabs && free && Quad.mayHave(e)
        return Mark(e.id, Box(min(p[0], q[0]), min(p[1], q[1]), max(p[0], q[0]), max(p[1], q[1])), figs, picked, tabs && !handles,
            if (handles) Quad.corners(e).map { toPx(it[0], it[1]) } else null)
    }

    /**
     * THE MARK [e] ON [s] AS [placeShown] TAKES IT, WITH NO PHOTO ESTIMATE (port 63 slice I1a; DESIGN-imagemeter §18
     * rule 2; Amit, 16 Nov 2026: "No foto estimation at all. It confuses user"). Not read is shown only where it can
     * be acted on, so WHICH figures the mark has here depends on whether it is picked:
     *  - NOT PICKED: every figure that has been READ, its size and its position alike (port 63 slice I1c, P3: "i want
     *    to see all measurements in its units always visible") -- nothing at all for one that has not. A wall nobody
     *    has measured is outlines and codes;
     *  - PICKED: every dimension it asks, size and position -- a read one as its figure, an unread one as the quiet `?`
     *    ([MarkDims.read], [MarkDims.plate]), each a tap target; the one being measured ([gold], a reading key) the
     *    gold plate, `▶?` until the reading lands or digits are typed ([MarkDims.figureText]; [ready] false: `▷?`);
     *  - A MARK THAT IS NOT IN THE ROOM (born a Client wish or Vendor added: [MarkDims.asked]) asks no reading and
     *    shows NO figure at all, picked or not (§18 rule 6) -- its handles are still its own.
     * A small box's one compact plate so carries its read halves, and an unread half only while the mark is picked.
     *
     * [held]: the mark is picked BY HAND and may move ([Pick.State.held]) -- it then shows its eight tabs
     * ([Mark.tabs]; a point has none), or with its corners [free] its four corners ([Mark.corners]), and [placeShown]
     * anchors its `w` and `h` clear of them. [live]: the keypad's uncommitted digits ([MeasurePad.live]), drawn on the
     * gold plate in the reading's own style with no `!`, exactly as [mark] draws them. [tx]: the language a gold `⊥`
     * chip names itself in ([MarkDims.figureText]) -- the one word a plate carries. Everything else is [mark]'s.
     */
    fun markShown(tx: Tx, actual: Map<String, Double>, s: String, e: SurveyPrep.Mark, a: Double, b: Double, picked: Boolean, held: Boolean,
                  gold: String?, toPx: (Double, Double) -> DoubleArray, u: Double, dp: Double,
                  textW: (text: String, fs: Double, bold: Boolean) -> Double, live: String? = null, free: Boolean = false,
                  ready: Boolean = true, ask: Boolean = true): Mark {
        val pt = SurveyPrep.isPoint(e.t) || Quad.of(e) != null; val fs = textSize(picked, dp)
        // WHICH figures, their texts and their looks are PhotoFigures' -- the one place rule 2 is written ([ask]: the
        // phase keys readings; false after Survey, where the picked mark shows no `?`)
        val figs = PhotoFigures.ofMark(tx, actual, s, e, a, b, picked, gold, live, ready, ask).map { f ->
            val d = f.dim
            Fig(d.code, d.role, line(d, pt, toPx, LINE_FS_U * u, u), if (d.line) null else anchor(d, toPx),
                plateW(textW(f.text + (if (f.check) MarkDims.CHECK_GLYPH else ""), fs, f.style.bold), dp), fs,
                rank(f.gold, picked, false, f.measured), f.measured, f.text, f.state, f.check, f.gold, f.typing, span(d, toPx), outward(d, toPx))
        }
        val p = toPx(e.x0, e.y1); val q = toPx(e.x1, e.y0)
        val tabs = picked && held && !SurveyPrep.isPoint(e.t)
        val handles = tabs && free && Quad.mayHave(e)
        return Mark(e.id, Box(min(p[0], q[0]), min(p[1], q[1]), max(p[0], q[0]), max(p[1], q[1])), figs, picked, tabs && !handles,
            if (handles) Quad.corners(e).map { toPx(it[0], it[1]) } else null)
    }

    /**
     * ONE FIGURE THAT IS NOT A MARK'S, AS [placeShown] TAKES IT (port 63 slice I1c; DESIGN-imagemeter §21, P4 and P5): a
     * Tape's, a Line's or a chain segment's figure ALONG ITS LINE from [from] to [to] (canvas px), or -- [from] alone,
     * [to] null -- an area's perimeter or a chain's total AT that point, level. Placed with the marks' plates, so that
     * no label lies on another: centred on its line and breaking it, turned to the line's own angle ([LabelAngle]),
     * stepped off on a leader only when there is no room along it.
     *
     * [id] the item's own id (the Mark it makes); [key] the reading key a tap on it takes -- null for a figure that
     * takes no reading (a Tape's: its plate is placed and is no tap target, [Fig.taps]); [text] the label exactly as
     * drawn, THE NUMBER ALONE ([PhotoFigures]) -- empty: nothing to place, null. [gold] it is the one being measured
     * (placed first, never a dot); [picked] its item is the picked one (12 dp, else 10); [measured] it is a reading.
     * [textW] the painter's own measure of a text, [dp] px a dp.
     */
    fun item(id: String, key: String?, text: String, from: DoubleArray, to: DoubleArray?, picked: Boolean, gold: Boolean,
             measured: Boolean, dp: Double, textW: (text: String, fs: Double, bold: Boolean) -> Double): Mark? {
        if (text.isEmpty()) return null
        val fs = textSize(picked || gold, dp)
        val style = MarkDims.plate(if (measured) MarkDims.State.MEASURED else MarkDims.State.UNKNOWN, gold)
        val ln = to?.let { Seg(from[0], from[1], it[0], it[1]) }
        val f = Fig(key ?: "$id#", MarkDims.Role.ITEM, ln, if (ln == null) from else null, plateW(textW(text, fs, style.bold), dp), fs,
            rank(gold, picked, false, measured), measured, text, if (measured) MarkDims.State.MEASURED else MarkDims.State.UNKNOWN,
            false, gold, false, ln, null, key != null)
        val xs = listOfNotNull(from, to)
        return Mark(id, Box(xs.minOf { it[0] }, xs.minOf { it[1] }, xs.maxOf { it[0] }, xs.maxOf { it[1] }), listOf(f), picked)
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
     * stand outside pointing in ([arrowsIn]: the span is too short for the plate). [off]: its dimension is wholly off
     * the canvas ([onCanvas]) -- the active figure or the picked mark's, the only ones placed then: a plate was slid
     * into view from outside; a dot never is, and lies off the canvas with its line.
     */
    class Plate(val fig: Fig, val markId: String, val how: How, val box: Box, val tap: Box,
                val turned: Boolean, val leader: Seg?, val arrowsIn: Boolean, val whole: Box?, val off: Boolean = false,
                /** Port 63 slice I1c (P5): the turn its label is drawn at, degrees clockwise on the canvas
                 *  ([LabelAngle]): 0 level, -90 bottom to top -- and from [placeShown] any angle its line has. [box] is
                 *  then the turned plate's BOUNDING box and [rect] the plate itself, [pw] along its text by [ph] across. */
                val angle: Double = if (turned) LabelAngle.UP else LabelAngle.LEVEL,
                val pw: Double = if (turned) box.h else box.w, val ph: Double = if (turned) box.w else box.h) {
        val code: String get() = fig.code
        val role: MarkDims.Role get() = fig.role
        /** Its dimension line on the canvas; null for a `p`. */
        val line: Seg? get() = fig.line
        val measured: Boolean get() = fig.measured
        val dot: Boolean get() = how == How.DOT
        /** The plate as drawn: [pw] by [ph] about [box]'s middle, turned by [angle] ([Turned.corners] for its outline). */
        val rect: Turned get() = Turned((box.l + box.r) / 2, (box.t + box.b) / 2, pw / 2, ph / 2, angle)
    }

    /**
     * A TURNED RECTANGLE ON THE CANVAS (port 63 slice I1c; DESIGN-imagemeter §21, P5): centred on ([cx], [cy]), [hw]
     * along its text either side of the middle and [hh] across it, turned by [deg] ([LabelAngle]). What the layout
     * keeps apart when a label runs along a slanting line: two labels turned along two close, slanting Lines lie clear
     * of each other while their BOUNDING boxes overlap, so the bounding box is not enough at the phone's numbers (a
     * 48 x 19 dp plate at 60° is 41 x 51 dp around) -- and it is exactly the bounding box for a level or an upright
     * plate, so a mark's plates are kept apart exactly as before.
     */
    class Turned(val cx: Double, val cy: Double, val hw: Double, val hh: Double, val deg: Double) {
        private val a = LabelAngle.along(deg); private val n = LabelAngle.across(deg)
        /** Its four corners: along-start top, along-end top, along-end bottom, along-start bottom. */
        fun corners(): List<DoubleArray> = listOf(-1.0 to -1.0, 1.0 to -1.0, 1.0 to 1.0, -1.0 to 1.0).map { (i, j) ->
            doubleArrayOf(cx + a[0] * hw * i + n[0] * hh * j, cy + a[1] * hw * i + n[1] * hh * j) }
        /** How far it reaches either side of its middle across [ax] (a unit vector). */
        private fun reach(ax: DoubleArray): Double = hw * abs(a[0] * ax[0] + a[1] * ax[1]) + hh * abs(n[0] * ax[0] + n[1] * ax[1])
        /** Its bounding box. */
        val bounds: Box get() { val ex = reach(doubleArrayOf(1.0, 0.0)); val ey = reach(doubleArrayOf(0.0, 1.0)); return Box(cx - ex, cy - ey, cx + ex, cy + ey) }
        /** Whether it lies on [o] or nearer to it than [apart] (separating axes: the two pairs of sides). Two level or
         *  upright rectangles answer exactly what their boxes would. */
        fun hits(o: Turned, apart: Double): Boolean {
            for (ax in listOf(a, n, o.a, o.n)) {
                val d = abs((o.cx - cx) * ax[0] + (o.cy - cy) * ax[1])
                if (d >= reach(ax) + o.reach(ax) + apart) return false
            }
            return true
        }
        /** The point of it nearest ([x], [y]). */
        fun nearest(x: Double, y: Double): DoubleArray {
            val dx = x - cx; val dy = y - cy
            val s = (dx * a[0] + dy * a[1]).coerceIn(-hw, hw); val t = (dx * n[0] + dy * n[1]).coerceIn(-hh, hh)
            return doubleArrayOf(cx + a[0] * s + n[0] * t, cy + a[1] * s + n[1] * t)
        }
        companion object {
            /** A box as a level rectangle. */
            fun of(b: Box): Turned = Turned((b.l + b.r) / 2, (b.t + b.b) / 2, b.w / 2, b.h / 2, LabelAngle.LEVEL)
        }
    }

    /** Where an upright plate's text starts and sits: half its padding inside the box, [UNDER_DP] above its bottom. */
    fun textLeft(box: Box, dp: Double): Double = box.l + PAD_DP * dp / 2
    fun baseline(box: Box, dp: Double): Double = box.b - UNDER_DP * dp

    /** Every figure placed, in the order given (marks, then each mark's figures). */
    class Out(val plates: List<Plate>) {
        fun of(code: String): Plate? = plates.firstOrNull { it.code == code }
        /** The tap targets, as [SurveyPrepTouch.hit] takes them: every plate, compact half and dot under its reading key. */
        val tags: List<SurveyPrepTouch.Tag> get() = plates.filter { it.fig.taps }.map { SurveyPrepTouch.Tag(it.code, it.tap) }
        /** How many shrank to a dot. */
        val dots: Int get() = plates.count { it.dot }
        /** The figure being measured now, as placed; null when none is. */
        val active: Plate? get() = plates.firstOrNull { it.fig.active }
        /** The figure being measured is wholly off the canvas, its plate slid into view: the screen pans to it
         *  (§1.4, "The photo pans only if that plate is off screen"). */
        val activeOff: Boolean get() = active?.off == true
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

    /** The four free corners of a mark, as the squares its plates keep clear: each handle is a disc of
     *  [SurveyGuide.END_R_DP] dp, never under a tab's own square ([TAB_HALF_U]). */
    fun cornerBoxes(corners: List<DoubleArray>, u: Double, dp: Double): List<Box> = corners.map { c ->
        val h = max(TAB_HALF_U * u, SurveyGuide.END_R_DP * dp)
        Box(c[0] - h, c[1] - h, c[0] + h, c[1] + h)
    }

    /** A plate's extent ALONG its line: its width, but the plate's height for an unturned plate on a vertical line. */
    private fun along(f: Fig, turned: Boolean, dp: Double): Double =
        if (turned || f.line?.vertical != true) f.w else plateH(f.fs, dp)

    /** The `h` plate is turned to read bottom to top; every other plate is upright -- but for a four-corner mark's side
     *  or diagonal that is more upright than level, which reads bottom to top as an `h` does. Such a plate is never
     *  turned to the slope: it stands upright or a quarter turn, and breaks the true sloping line. */
    private fun turns(f: Fig): Boolean = f.role == MarkDims.Role.HEIGHT ||
        ((f.role == MarkDims.Role.SIDE || f.role == MarkDims.Role.DIAG) && f.line?.vertical == true)

    /** The span is too short for the plate to sit on (rule 2; and for both of a small box's, the compact plate). */
    private fun short(f: Fig, dp: Double): Boolean {
        val ln = f.line ?: return false
        return ln.len < along(f, turns(f), dp) + SHORT_DP * dp
    }

    /** A place a plate may go: its [box] -- with [rect], the turned plate it bounds (port 63 slice I1c) -- how it got
     *  there, whether it is [turned], and its [leader]. */
    private class Cand(val box: Box, val how: How, val turned: Boolean, val leader: Seg?, val rect: Turned? = null)

    /** The side a plate steps off its line to first: away from the mark for its size, up for a corner line, left for a
     *  floor or ceiling line. -1 or +1 along the canvas axis across the line. */
    private fun side(role: MarkDims.Role): Double = when (role) {
        MarkDims.Role.HEIGHT, MarkDims.Role.DROP -> 1.0
        else -> -1.0
    }

    private fun lineCands(mk: Mark, f: Fig, hand: Boolean, width: Double, height: Double, u: Double, dp: Double): List<Cand> {
        val ln = f.line ?: return emptyList()
        val out = ArrayList<Cand>()
        val ph = plateH(f.fs, dp)
        if (hand && (f.role == MarkDims.Role.WIDTH || f.role == MarkDims.Role.HEIGHT)) {
            val a = if (f.role == MarkDims.Role.WIDTH) SurveyPrepTouch.widthTag(mk.box.l, mk.box.t, mk.box.r, f.fs, u, dp, true)
                    else SurveyPrepTouch.heightTag(mk.box.t, mk.box.r, mk.box.b, f.fs, u, dp, true)
            // the anchor is the tag's centre (or its left edge) and its text's baseline, as SurveyPrepTouch.tagShape reads it
            val l = if (a.left) a.x else a.x - f.w / 2; val t = a.y - f.fs - OVER_DP * dp
            out += Cand(boxAt(l + f.w / 2, t + ph / 2, f.w, ph, width, height, u), How.HAND, false, null)
        }
        // a HAND plate that finds no room falls back as any plate does; it is then upright, as the anchor's is (a `w`
        // or an `h` only: a four-corner mark has neither, and its sides' plates turn as they always do)
        val turned = turns(f) && !(hand && (f.role == MarkDims.Role.WIDTH || f.role == MarkDims.Role.HEIGHT))
        val bw = if (turned) ph else f.w; val bh = if (turned) f.w else ph
        val al = along(f, turned, dp); val len = ln.len
        val ts = doubleArrayOf(0.5, 0.25, 0.75)
        for (t in ts) {
            // on the line: the whole plate within the span, half of SHORT_DP clear of each end
            if (min(t, 1 - t) * len < al / 2 + SHORT_DP * dp / 2) continue
            val c = ln.at(t)
            out += Cand(boxAt(c[0], c[1], bw, bh, width, height, u), How.ON_LINE, turned, null)
        }
        // off the line: half a plate height clear of it (centre to line, one plate height for a plate lying along it),
        // either side; and, with that taken too, one plate further out again
        // a four-corner mark's side steps off OUT of the shape first (Fig.out); every other line by its role
        val first = f.out?.let { n -> (if (ln.vertical) n[0] else n[1]).let { if (it > 0) 1.0 else if (it < 0) -1.0 else null } } ?: side(f.role)
        val across = if (ln.vertical) bw else bh
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

    // ------------------------------------------------------------------ every figure along its dimension (port 63 slice I1c)

    /**
     * THE TURN A FIGURE'S LABEL RUNS AT ON THE ONE STEP THAT MARKS AND MEASURES (DESIGN-imagemeter §21, P5): its own
     * line's ([LabelAngle.of]) -- a width level, a height and every upright position line bottom to top, a four-corner
     * mark's side and diagonal along their slope, a Line or a Tape along itself -- and LEVEL for a figure with no line
     * (a `p`'s chip, an area's or a chain total's figure at its middle).
     */
    fun turnOf(f: Fig): Double = f.line?.let { LabelAngle.of(it) } ?: LabelAngle.LEVEL

    /** A [pw] x [ph] plate turned by [deg], centred on ([cx], [cy]) and slid back inside the [width] x [height] canvas
     *  ([EDGE_U] inside it) by its BOUNDING box, as [boxAt] slides a level one. */
    private fun turnedAt(cx: Double, cy: Double, pw: Double, ph: Double, deg: Double, width: Double, height: Double, u: Double): Turned {
        val r0 = Turned(cx, cy, pw / 2, ph / 2, deg).bounds
        val ex = r0.w / 2; val ey = r0.h / 2; val e = EDGE_U * u
        return Turned(max(e + ex, min(width - e - ex, cx)), max(e + ey, min(height - e - ey, cy)), pw / 2, ph / 2, deg)
    }

    private fun cand(r: Turned, how: How, leader: Seg?): Cand = Cand(r.bounds, how, r.deg != LabelAngle.LEVEL, leader, r)

    /** The span is too short for a plate laid ALONG it: its width and [SHORT_DP]. */
    private fun shortAlong(f: Fig, dp: Double): Boolean { val ln = f.line ?: return false; return ln.len < f.w + SHORT_DP * dp }

    /**
     * WHERE A FIGURE WITH A LINE MAY GO WHEN EVERY LABEL RUNS ALONG ITS DIMENSION ([placeShown]), best first -- rule 2
     * and 3 of this object's header with the plate TURNED TO ITS LINE ([turnOf]): mid-span on the line, which it breaks,
     * then ¼ and ¾ along (the whole plate within the span, half of [SHORT_DP] clear of each end); then off the line,
     * square to it, on a leader -- half a plate height clear, either side, the first side as [placeBy] takes it (away
     * from the mark for its size, up or left for a position, out of the shape for a four-corner side) -- and one
     * plate further out again. The HELD mark's `w` and `h` are anchored by hand first, clear of its tabs, as built: the
     * width level where [SurveyPrepTouch.widthTag] puts it, the height TURNED UP from where [SurveyPrepTouch.heightTag]
     * starts it, so it too runs along its line.
     */
    private fun alongCands(mk: Mark, f: Fig, hand: Boolean, width: Double, height: Double, u: Double, dp: Double): List<Cand> {
        val ln = f.line ?: return emptyList()
        val out = ArrayList<Cand>()
        val ph = plateH(f.fs, dp); val pw = f.w
        val deg = turnOf(f)
        if (hand && f.role == MarkDims.Role.WIDTH) {
            val a = SurveyPrepTouch.widthTag(mk.box.l, mk.box.t, mk.box.r, f.fs, u, dp, true)
            val l = if (a.left) a.x else a.x - pw / 2; val t = a.y - f.fs - OVER_DP * dp
            out += cand(turnedAt(l + pw / 2, t + ph / 2, pw, ph, LabelAngle.LEVEL, width, height, u), How.HAND, null)
        }
        if (hand && f.role == MarkDims.Role.HEIGHT) {
            // the anchor's left edge and middle height; the plate stands up from there, ph wide and pw tall
            val a = SurveyPrepTouch.heightTag(mk.box.t, mk.box.r, mk.box.b, f.fs, u, dp, true)
            val l = if (a.left) a.x else a.x - pw / 2; val t = a.y - f.fs - OVER_DP * dp
            out += cand(turnedAt(l + ph / 2, t + ph / 2, pw, ph, LabelAngle.UP, width, height, u), How.HAND, null)
        }
        val across = LabelAngle.across(deg); val len = ln.len
        val ts = doubleArrayOf(0.5, 0.25, 0.75)
        for (t in ts) {
            if (min(t, 1 - t) * len < pw / 2 + SHORT_DP * dp / 2) continue
            val c = ln.at(t)
            out += cand(turnedAt(c[0], c[1], pw, ph, deg, width, height, u), How.ON_LINE, null)
        }
        // which way across the line is first: the side [placeBy] steps off to, seen across this line
        val want = f.out ?: if (ln.vertical) doubleArrayOf(side(f.role), 0.0) else doubleArrayOf(0.0, side(f.role))
        val first = if (across[0] * want[0] + across[1] * want[1] >= 0) 1.0 else -1.0
        for (reach in doubleArrayOf(ph, 2 * ph + APART_U * u))
            for (t in ts) for (sd in doubleArrayOf(first, -first)) {
                val c = ln.at(t)
                val r = turnedAt(c[0] + across[0] * sd * reach, c[1] + across[1] * sd * reach, pw, ph, deg, width, height, u)
                // the leader: from the line to the nearest point of the plate
                val near = r.nearest(c[0], c[1])
                out += cand(r, How.OFF_LINE, Seg(c[0], c[1], near[0], near[1]))
            }
        return out
    }

    /** How many plate heights further out a READ figure with no free spot may step on the one step ([farCands]). */
    private const val FAR_STEPS = 4

    /**
     * WHERE A READ FIGURE MAY GO WHEN NOTHING [alongCands] OR [pointCands] OFFERED IS FREE (the review of I1, finding 3):
     * further off its line, square to it, on a leader -- 3 to 3 + [FAR_STEPS] - 1 plate heights out, at ½, ¼ and ¾ along,
     * either side, turned along its line as every plate is; a figure with no line steps 2 to 1 + [FAR_STEPS] plate
     * heights above, below, right and left of its point, level, on a leader back to it.
     */
    private fun farCands(f: Fig, width: Double, height: Double, u: Double, dp: Double): List<Cand> {
        val ph = plateH(f.fs, dp); val pw = f.w
        val out = ArrayList<Cand>()
        val ln = f.line
        if (ln != null) {
            val deg = turnOf(f); val across = LabelAngle.across(deg)
            for (k in 3 until 3 + FAR_STEPS) for (t in doubleArrayOf(0.5, 0.25, 0.75)) for (sd in doubleArrayOf(1.0, -1.0)) {
                val c = ln.at(t)
                val r = turnedAt(c[0] + across[0] * sd * k * ph, c[1] + across[1] * sd * k * ph, pw, ph, deg, width, height, u)
                val near = r.nearest(c[0], c[1])
                out += cand(r, How.OFF_LINE, Seg(c[0], c[1], near[0], near[1]))
            }
            return out
        }
        val at = f.at ?: return out
        for (k in 2 until 2 + FAR_STEPS) for ((dx, dy) in listOf(0.0 to -1.0, 0.0 to 1.0, 1.0 to 0.0, -1.0 to 0.0)) {
            val r = turnedAt(at[0] + dx * (pw / 2 + k * ph), at[1] + dy * k * ph, pw, ph, LabelAngle.LEVEL, width, height, u)
            val near = r.nearest(at[0], at[1])
            out += cand(r, How.OFF_LINE, Seg(at[0], at[1], near[0], near[1]))
        }
        return out
    }

    /** Where a figure with NO line may go on the one step: a mark's `p` as [chipCands] puts it; a Tape's, a Line's or
     *  a chain's figure that has none -- an area's perimeter, a chain's total -- centred on its point, then above it,
     *  below it, right of it and left of it, one plate height off. Level. */
    private fun pointCands(f: Fig, hand: Boolean, width: Double, height: Double, u: Double, dp: Double): List<Cand> {
        if (f.role != MarkDims.Role.ITEM) return chipCands(f, hand, width, height, u, dp)
        val at = f.at ?: return emptyList()
        val ph = plateH(f.fs, dp); val pw = f.w
        return listOf(0.0 to 0.0, 0.0 to -ph, 0.0 to ph, (pw / 2 + ph / 2) to 0.0, -(pw / 2 + ph / 2) to 0.0).map { (dx, dy) ->
            cand(turnedAt(at[0] + dx, at[1] + dy, pw, ph, LabelAngle.LEVEL, width, height, u), How.CHIP, null) }
    }

    private fun chipCands(f: Fig, hand: Boolean, width: Double, height: Double, u: Double, dp: Double): List<Cand> {
        val at = f.at ?: return emptyList()
        // off the corner by GAP_U -- and on a mark that shows its tabs, by what its w and h tags stand off them
        val g = if (hand) SurveyPrepTouch.clearance(u, dp) else GAP_U * u
        val w = f.w; val h = plateH(f.fs, dp); val x = at[0]; val y = at[1]
        fun c(l: Double, t: Double) = Cand(boxAt(l + w / 2, t + h / 2, w, h, width, height, u), How.CHIP, false, null)
        // outside the corner below and right; inside it; below, inside the right edge; right, above the corner
        return listOf(c(x + g, y + g), c(x - g - w, y - g - h), c(x - g - w, y + g), c(x + g, y - g - h))
    }

    /**
     * WHERE A FIGURE'S DOT MAY GO, best first, and whether those spots are on the canvas. A line: the middle of the
     * part of it the canvas shows, then along that part either way, a [step] at a time, towards its start first -- of
     * the part of its [Fig.span] when the line itself runs outside (a mark against the ceiling). A `p`: the corner it
     * is anchored at, then the mark's other three, those on the canvas. With none of it on the canvas: the line's own
     * middle, the anchor -- where it is, never slid in.
     */
    private fun dotSpots(f: Fig, mk: Mark, width: Double, height: Double, step: Double): Pair<List<DoubleArray>, Boolean> {
        val ln = f.line
        if (ln != null) {
            val part = clip(ln, width, height) ?: f.span?.let { clip(it, width, height) } ?: return Pair(listOf(ln.at(0.5)), false)
            val spots = arrayListOf(part.at(0.5)); val len = part.len
            var k = 1
            while (len > 0 && k * step <= len / 2) { spots += part.at(0.5 - k * step / len); spots += part.at(0.5 + k * step / len); k++ }
            return Pair(spots, true)
        }
        val at = f.at ?: return Pair(listOf(doubleArrayOf((mk.box.l + mk.box.r) / 2, (mk.box.t + mk.box.b) / 2)), true)
        val b = mk.box
        val corners = listOf(at, doubleArrayOf(b.l, b.b), doubleArrayOf(b.r, b.t), doubleArrayOf(b.l, b.t)).filter { inside(it, width, height) }
        return if (corners.isNotEmpty()) Pair(corners, true) else Pair(listOf(at), false)
    }

    /**
     * THE DOT OF A FIGURE WITH NO ROOM FOR ITS PLATE (rule 4): the first of [dotSpots] that is [clear] of what is
     * placed and kept; else the first that [open] finds clear of what is placed. A line wholly under plates -- two
     * points at one height share their corner line, and the first one's plate lies along it -- has neither: the dot
     * then stands BESIDE its line, the nearest clear place within two plate heights of it, a quarter dot at a time,
     * either side. Only with none of those is it drawn at its first spot, whatever is there. On the canvas it stays
     * wholly inside it (half a dot at most from its spot); off it, it is not moved.
     */
    private fun dot(f: Fig, mk: Mark, width: Double, height: Double, u: Double, dp: Double, arrowsIn: Boolean, off: Boolean,
                    clear: (Box) -> Boolean, open: (Box) -> Boolean): Plate {
        val r = DOT_DP * dp / 2; val step = r / 2
        val (spots, on) = dotSpots(f, mk, width, height, step)
        fun round(x: Double, y: Double): Box {
            val cx = if (on) max(r, min(width - r, x)) else x; val cy = if (on) max(r, min(height - r, y)) else y
            return Box(cx - r, cy - r, cx + r, cy + r)
        }
        val boxes = spots.map { round(it[0], it[1]) }
        var b = boxes.firstOrNull(clear) ?: boxes.firstOrNull(open)
        val ln = f.line
        if (b == null && on && ln != null) {
            var k = 1
            while (b == null && k * step <= 2 * plateH(f.fs, dp)) {
                val d = k * step
                b = spots.firstNotNullOfOrNull { c ->
                    listOf(-d, d).map { if (ln.vertical) round(c[0] + it, c[1]) else round(c[0], c[1] + it) }.firstOrNull(open) }
                k++
            }
        }
        val at = b ?: boxes[0]
        return Plate(f, mk.id, How.DOT, at, tapOf(at, u, dp), false, null, arrowsIn, null, off)
    }

    /**
     * THE PLACING RULE IN ITS LEVEL-PLATE FORM -- every plate level or turned a quarter, kept apart by its box -- with
     * [shows] deciding which of a mark's figures are placed at all and [handed] whether a mark shows handles its plates
     * keep clear of. The four-step route's `place` (with its scope) was this; since slice I1b nothing on the screen asks
     * it, and the placing rule's own tests do. [placeShown] is the screen's.
     */
    internal fun placeBy(marks: List<Mark>, width: Double, height: Double, keep: List<Box>, u: Double, dp: Double,
                         times: Double, shows: (Mark, Fig) -> Boolean, handed: (Mark) -> Boolean): Out =
        lay(marks, width, height, keep, u, dp, times, shows, handed, slant = false)

    /**
     * EVERY FIGURE HANDED IN, PLACED (port 63 slice I1a; DESIGN-imagemeter §13 and §18 "In the models"): the placing
     * rule with NO SCOPE -- the one step that marks and measures has no `all` and no "which marks show which figures"
     * of its own. WHICH figures a mark shows is decided before this is called ([markShown]: an unpicked mark only what
     * has been read of its size, the picked mark everything it asks), so [marks] carry only those and an unpicked,
     * unmeasured mark costs the layout nothing.
     *
     * THE HELD MARK KEEPS ITS TABS CLEAR, re-keyed from the four-step route's picked scope to the mark itself (the plan critic's finding
     * C5): a picked mark that shows its eight tabs ([Mark.tabs]: it is HELD, [Pick]) has its `w` and `h` plates
     * anchored by hand, clear of every tab, and its tab squares -- or its four free corners ([Mark.corners]) -- kept
     * clear by every plate. A mark the walk reached is picked and shows no tabs: its plates sit on their lines.
     *
     * EVERY FIGURE RUNS ALONG ITS DIMENSION (port 63 slice I1c; DESIGN-imagemeter §21, P5; Amit, 23 Nov 2026: "no way
     * to set alignment of measurements to line direction. its always horizontal which overlaps on other
     * measurements"). Each plate is TURNED TO ITS OWN LINE ([turnOf], [LabelAngle]) -- level on a width, bottom to top
     * on a height and on every upright position line, along the slope on a four-corner mark's side and diagonal and on
     * a Line or a Tape ([item]) -- and is placed BY ITS TURNED RECTANGLE ([Plate.rect], [Turned.hits]): two labels
     * turned along two close, slanting Lines sit on their lines though their bounding boxes overlap, and a label steps
     * off its line on a leader only when there is no room ALONG it ([alongCands]). [Plate.box] is the turned plate's
     * bounding box, [Plate.angle] its turn. A small box's compact plate and a `p`'s chip stay level, as built.
     */
    fun placeShown(marks: List<Mark>, width: Double, height: Double, keep: List<Box>, u: Double, dp: Double,
                   times: Double = 0.0): Out =
        lay(marks, width, height, keep, u, dp, times, { _, _ -> true }, { mk -> mk.picked && (mk.tabs || mk.corners != null) }, slant = true)

    /** The placing rule itself (this object's header): [shows] which of a mark's figures are drawn at all, [handed]
     *  whether the mark shows handles its plates must keep clear of; [slant] every plate turned to its line and kept
     *  apart by its turned rectangle ([placeShown]) -- false: the level-plate rule exactly as built ([placeBy]). */
    private fun lay(marks: List<Mark>, width: Double, height: Double, keep: List<Box>, u: Double, dp: Double, times: Double,
                    shows: (Mark, Fig) -> Boolean, handed: (Mark) -> Boolean, slant: Boolean): Out {
        class Job(val mk: Mark, val figs: List<Fig>, val rank: Rank, val order: Int, val hand: Boolean)
        val jobs = ArrayList<Job>()
        val blocked = ArrayList(keep)
        var n = 0
        for (mk in marks) {
            val shown = mk.figs.filter { shows(mk, it) && (mk.picked || it.active || onCanvas(it, width, height)) }
            val hand = handed(mk)
            if (hand) blocked += mk.corners?.let { cornerBoxes(it, u, dp) } ?: tabBoxes(mk.box, u)
            val w = shown.firstOrNull { it.role == MarkDims.Role.WIDTH && it.line != null }
            val h = shown.firstOrNull { it.role == MarkDims.Role.HEIGHT && it.line != null }
            // a small box: both spans too short for their plates -- one compact plate beside it
            val pair = if (!hand && w != null && h != null && short(w, dp) && short(h, dp)) listOf(w, h) else null
            if (pair != null) jobs += Job(mk, pair, minOf(w!!.rank, h!!.rank), n++, false)
            for (f in shown) if (pair == null || f !in pair) jobs += Job(mk, listOf(f), f.rank, n++, hand)
        }
        val apart = APART_U * u
        val placed = ArrayList<Box>()
        // on the one step every plate is kept apart by the rectangle it IS, turned or not (slant); else by its box
        val placedR = ArrayList<Turned>()
        val blockedR = if (slant) blocked.map { Turned.of(it) } else emptyList()
        fun openR(r: Turned) = placedR.none { r.hits(it, apart) }
        fun freeR(r: Turned) = openR(r) && blockedR.none { r.hits(it, 0.0) }
        fun open(b: Box) = if (slant) openR(Turned.of(b)) else placed.none { overlaps(b, it, apart) }
        fun free(b: Box) = if (slant) freeR(Turned.of(b)) else open(b) && blocked.none { overlaps(b, it, 0.0) }
        fun put(b: Box, r: Turned? = null) { if (slant) placedR += (r ?: Turned.of(b)) else placed += b }
        fun freeC(c: Cand) = if (slant) freeR(c.rect ?: Turned.of(c.box)) else free(c.box)
        val out = HashMap<String, Plate>()
        // a dot keeps the spot it found: no later plate, and no later dot, is put over it
        fun dotOf(f: Fig, mk: Mark, arrowsIn: Boolean) {
            val d = dot(f, mk, width, height, u, dp, arrowsIn, !onCanvas(f, width, height), { free(it) }, { open(it) })
            put(d.box); out[f.code] = d
        }
        for (j in jobs.sortedWith(compareBy<Job> { it.rank.ordinal }.thenBy { it.order })) {
            val mk = j.mk
            if (j.figs.size == 2) {
                // THE COMPACT PLATE: "≈200 × ≈90" right of the box, else left of it, above it, below it
                val w = j.figs[0]; val h = j.figs[1]
                val cw = w.w + times + h.w; val ch = max(plateH(w.fs, dp), plateH(h.fs, dp))
                val g = (LINE_OFF_U + GAP_U) * u; val b = mk.box
                val cx = (b.l + b.r) / 2; val cy = (b.t + b.b) / 2
                val cands = listOf(
                    boxAt(b.r + g + cw / 2, cy, cw, ch, width, height, u), boxAt(b.l - g - cw / 2, cy, cw, ch, width, height, u),
                    boxAt(cx, b.t - g - ch / 2, cw, ch, width, height, u), boxAt(cx, b.b + g + ch / 2, cw, ch, width, height, u))
                // A READ PAIR IS NEVER TWO DOTS on the one step (the review of I1, finding 3; P3, "all measurements ...
                // always visible"): further out first, then the first spot even if it is not free -- as the gold one is
                val whole = cands.firstOrNull { free(it) }
                    ?: if (j.rank == Rank.ACTIVE) cands[0]
                    else if (slant && j.figs.any { it.measured }) (1..FAR_STEPS).asSequence().flatMap { k ->
                        val o = g + k * ch
                        sequenceOf(boxAt(b.r + o + cw / 2, cy, cw, ch, width, height, u), boxAt(b.l - o - cw / 2, cy, cw, ch, width, height, u),
                            boxAt(cx, b.t - o - ch / 2, cw, ch, width, height, u), boxAt(cx, b.b + o + ch / 2, cw, ch, width, height, u)) }
                        .firstOrNull { free(it) } ?: cands[0]
                    else null
                if (whole == null) {
                    for (f in j.figs) dotOf(f, mk, true)
                    continue
                }
                put(whole)
                val split = whole.l + w.w + times / 2
                val tap = tapOf(whole, u, dp); val m = TAP_MIN_DP * dp
                out[w.code] = Plate(w, mk.id, How.COMPACT, Box(whole.l, whole.t, whole.l + w.w, whole.b),
                    Box(min(tap.l, split - m), tap.t, split, tap.b), false, null, true, whole, !onCanvas(w, width, height))
                out[h.code] = Plate(h, mk.id, How.COMPACT, Box(whole.r - h.w, whole.t, whole.r, whole.b),
                    Box(split, tap.t, max(tap.r, split + m), tap.b), false, null, true, whole, !onCanvas(h, width, height))
                continue
            }
            val f = j.figs[0]
            val tooShort = if (slant) shortAlong(f, dp) else short(f, dp)
            val cands = if (slant) { if (f.line != null) alongCands(mk, f, j.hand, width, height, u, dp) else pointCands(f, j.hand, width, height, u, dp) }
                        else if (f.line != null) lineCands(mk, f, j.hand, width, height, u, dp) else chipCands(f, j.hand, width, height, u, dp)
            // A READ FIGURE IS NEVER A DOT on the one step (the review of I1, finding 3; P3, "i want to see all
            // measurements in its units always visible"): with no free spot on or beside its line it steps further off on
            // a longer leader ([farCands]), and with none there either it takes its first spot, free or not, as the gold
            // one does -- a reading he took is a number on the photo, never a 6 dp dot. An unread `?` may still be one.
            val c = cands.firstOrNull { freeC(it) }
                ?: if (j.rank == Rank.ACTIVE) cands.firstOrNull()
                else if (slant && f.measured) farCands(f, width, height, u, dp).firstOrNull { freeC(it) } ?: cands.firstOrNull()
                else null
            if (c == null) { dotOf(f, mk, tooShort); continue }
            put(c.box, c.rect)
            val r = c.rect
            out[f.code] = if (r == null) Plate(f, mk.id, c.how, c.box, tapOf(c.box, u, dp), c.turned, c.leader, tooShort, null, !onCanvas(f, width, height))
                else Plate(f, mk.id, c.how, c.box, tapOf(c.box, u, dp), c.turned, c.leader, tooShort, null, !onCanvas(f, width, height),
                           r.deg, 2 * r.hw, 2 * r.hh)
        }
        return Out(marks.flatMap { mk -> mk.figs.mapNotNull { out[it.code] } })
    }
}
