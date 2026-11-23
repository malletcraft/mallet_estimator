package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch.Box
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * FINE MOVE'S PAD (port 63 slice I2a, 23 Nov 2026; DESIGN-imagemeter §6, §8.1, §13). Until slice I2 a row of nudges
 * stands under the photo whenever a mark is held (◀ ▶ ▼ ▲, or ▼5 ▼1 ▲1 ▲5 for an edge) -- one of the "explicit box
 * below foto" rows Amit asked to be rid of (16 Nov 2026: "very less clutter"). Fine move is the panel's act that
 * replaces it: the panel closes, the item is held, and a small pad stands OVER THE PHOTO only while he is using it --
 * ▲ ◀ ▶ ▼ round a centre cell that switches the step between 1 and 5 mm, and ✓ -- in the bottom corner FARTHEST from
 * the item. It nudges the part LAST TOUCHED (the whole item, an edge, a corner, an end), and while it is up a tap on
 * another handle of the same item makes THAT the part nudged: "four corners are four taps, not four trips through the
 * panel". It never snaps -- it is the way past a snap, for the last millimetres.
 *
 * IT ENDS by ✓, by a tap on empty photo, by a reading that lands -- by anything at all that leaves its item no longer
 * picked and held ([after]): the pad is that item's, and a mark the walk has reached is not moved by a thumb.
 *
 * ONE ↶ STEP FOR THE WHOLE OF A FINE MOVE (ours): taken just before its first nudge that moves anything, as a drag
 * takes one however far it goes; an overshoot is the opposite arrow, not ↶.
 *
 * Pure: every rule is here and tested (NudgePadTest); :app draws [GRID] in [box] and calls [press].
 */
object NudgePad {

    /** The pad is [CELLS] x [CELLS] cells of [CELL_DP], and stands [EDGE_DP] inside the photo's bottom corner, dp. */
    const val CELL_DP = 40.0
    const val CELLS = 3
    const val SIZE_DP = CELL_DP * CELLS
    const val EDGE_DP = 8.0

    /** The two steps, mm: the centre cell switches between them. It opens on the fine one. */
    const val FINE_MM = 1
    const val COARSE_MM = 5

    /** The whole item ([SurveyPrep.moveMark]'s "body"). */
    const val BODY = "body" // text:value a part's key, never shown

    /** Point [i] of a Tape, a Line or an area as the part nudged: one of its ends, or an area's corner. */
    fun pointPart(i: Int): String = "p$i" // text:value a part's key, never shown

    /** Which point a part is, or null: it is no point's. */
    fun pointOf(part: String): Int? = if (part.length > 1 && part[0] == 'p') part.substring(1).toIntOrNull()?.takeIf { it >= 0 } else null

    /** A cell of the pad. */
    enum class Key { UP, LEFT, RIGHT, DOWN, STEP, DONE }

    /** The pad as drawn, row by row from the top: ▲ ◀ ▶ ▼ round the step, ✓ in the top right corner; null: an empty cell. */
    val GRID: List<List<Key?>> = listOf(
        listOf(null, Key.UP, Key.DONE),
        listOf(Key.LEFT, Key.STEP, Key.RIGHT),
        listOf(null, Key.DOWN, null),
    )

    /** An arrow's or ✓'s glyph; the step's cell reads [stepText]. */
    fun glyph(k: Key): String = when (k) {
        Key.UP -> "▲"; Key.LEFT -> "◀"; Key.RIGHT -> "▶"; Key.DOWN -> "▼"; Key.DONE -> "✓"; Key.STEP -> "" // text:value glyphs, the same in both languages
    }

    /** Which bottom corner of the photo the pad stands in. */
    enum class Corner { LEFT, RIGHT }

    /**
     * A FINE MOVE IN PROGRESS: the item [id]; the [part] its arrows move -- [BODY], one of a mark's eight tabs
     * ([SurveyPrepTouch.TABS]) or four corners ([Quad.PARTS]), a point of a Tape, a Line or an area ([pointPart]);
     * [stepMm], 1 or 5; the [corner] the pad stands in, chosen ONCE as it opens -- a pad that changed sides as the item
     * crossed the middle would leave the thumb that is pressing it; [stepTaken]: this Fine move's one ↶ step is taken.
     */
    data class State(val id: String, val part: String, val stepMm: Int, val corner: Corner, val stepTaken: Boolean = false)

    // ------------------------------------------------------------------ where the pad stands

    /** The pad's box in the [corner] of a [w] x [h] photo, canvas px; [dp] px a dp. */
    fun box(corner: Corner, w: Double, h: Double, dp: Double): Box {
        val s = SIZE_DP * dp; val e = EDGE_DP * dp
        val l = if (corner == Corner.LEFT) e else w - e - s
        return Box(l, h - e - s, l + s, h - e)
    }

    /** The cell [k]'s box inside the pad at [pad], canvas px; null for a key that is not on the pad. */
    fun cell(pad: Box, k: Key, dp: Double): Box? {
        val c = CELL_DP * dp
        GRID.forEachIndexed { r, row -> row.forEachIndexed { i, x -> if (x == k) return Box(pad.l + i * c, pad.t + r * c, pad.l + (i + 1) * c, pad.t + (r + 1) * c) } }
        return null
    }

    /** The key under ([x], [y]) on the pad at [pad], or null: an empty cell, or off the pad. The pad is asked BEFORE
     *  anything on the photo: it is drawn over it. */
    fun keyAt(pad: Box, x: Double, y: Double, dp: Double): Key? {
        if (!pad.contains(x, y)) return null
        val c = CELL_DP * dp
        val i = min(CELLS - 1, ((x - pad.l) / c).toInt()); val r = min(CELLS - 1, ((y - pad.t) / c).toInt())
        return GRID[r][i]
    }

    private fun gap(a: Box, b: Box): Double = hypot(max(0.0, max(a.l - b.r, b.l - a.r)), max(0.0, max(a.t - b.b, b.t - a.b)))
    private fun shared(a: Box, b: Box): Double = max(0.0, min(a.r, b.r) - max(a.l, b.l)) * max(0.0, min(a.b, b.b) - max(a.t, b.t))

    /**
     * THE BOTTOM CORNER FARTHEST FROM THE ITEM, whose box on screen is [bb] (null: it is not on the photo): the one
     * whose pad stands farther from that box; where both pads would lie on the item (a unit along the whole foot of the
     * wall), the one that covers less of it; with nothing between them, the RIGHT -- the thumb's.
     */
    fun corner(bb: Box?, w: Double, h: Double, dp: Double): Corner {
        if (bb == null) return Corner.RIGHT
        val l = box(Corner.LEFT, w, h, dp); val r = box(Corner.RIGHT, w, h, dp)
        val gl = gap(l, bb); val gr = gap(r, bb)
        return when {
            gl > gr -> Corner.LEFT
            gr > gl -> Corner.RIGHT
            shared(l, bb) < shared(r, bb) -> Corner.LEFT
            else -> Corner.RIGHT
        }
    }

    // ------------------------------------------------------------------ the part it moves

    /** The parts the item [id] can be nudged by; empty: nothing here is nudged (no such item, or a chain -- [MarkSheet.fineOk]). */
    fun parts(p: SurveyPrep.Prep, id: String): List<String> {
        p.markById(id)?.let { (_, e) ->
            return if (SurveyPrep.isSpot(e.t)) listOf(BODY) else listOf(BODY) + SurveyPrepTouch.TABS + Quad.PARTS
        }
        p.measById(id)?.let { (_, m) -> return listOf(BODY) + m.pts.indices.map { pointPart(it) } }
        return emptyList()
    }

    /** [part] where the item has it, else the whole item: a point has no tab, and "the part last touched" may be
     *  another item's. */
    private fun partOf(p: SurveyPrep.Prep, id: String, part: String?): String = part?.takeIf { it in parts(p, id) } ?: BODY

    /**
     * FINE MOVE BEGINS on [id] at [stage] -- the panel has closed and the item is held ([Pick.shaped], row 16) -- or
     * null where it is not offered ([MarkSheet.fineOk]). It nudges [part], the part last touched, the whole item when
     * that is none of its own; by 1 mm; the pad in the corner farthest from [bb], the item's box on a [w] x [h] photo.
     */
    fun start(p: SurveyPrep.Prep, id: String, stage: Int?, part: String?, bb: Box?, w: Double, h: Double, dp: Double): State? =
        if (!MarkSheet.fineOk(p, id, stage)) null else State(id, partOf(p, id, part), FINE_MM, corner(bb, w, h, dp))

    /**
     * WHILE THE PAD IS UP, A TAP ON ANOTHER HANDLE, CORNER OR END OF THE SAME ITEM MAKES THAT THE PART NUDGED
     * ([Pick.handle]'s `nudge`); a tap on its body, the whole item again. A part the item does not have changes
     * nothing. The step, the corner and the one ↶ step stay.
     */
    fun aim(p: SurveyPrep.Prep, st: State, part: String): State = if (part in parts(p, st.id)) st.copy(part = part) else st

    /** The centre cell: 1 mm ↔ 5 mm. */
    fun toggle(st: State): State = st.copy(stepMm = if (st.stepMm == FINE_MM) COARSE_MM else FINE_MM)

    /** The centre cell's words, "1 mm" / "5 mm", and what a screen reader calls it and ✓. */
    fun stepText(st: State): String = "${st.stepMm} mm" // text:value the unit mm, a figure drawn as written
    fun stepLabel(tx: Tx, st: State): String = tx.sp_fine_aria_step_x(st.stepMm.toString())
    fun doneLabel(tx: Tx): String = tx.sp_fine_aria_done

    /**
     * WHAT AN ARROW MOVES, mm across and UP (a mark's y runs up the wall): ▲ up, ▼ down, ◀ left, ▶ right, by [stepMm].
     * Null for the step's cell and ✓.
     */
    fun delta(k: Key, stepMm: Int): DoubleArray? = when (k) {
        Key.UP -> doubleArrayOf(0.0, stepMm.toDouble()); Key.DOWN -> doubleArrayOf(0.0, -stepMm.toDouble())
        Key.LEFT -> doubleArrayOf(-stepMm.toDouble(), 0.0); Key.RIGHT -> doubleArrayOf(stepMm.toDouble(), 0.0)
        Key.STEP, Key.DONE -> null
    }

    /**
     * WHETHER THE CELL [k] ANSWERS for the part being nudged: an EDGE moves one way only -- a mark's top or bottom
     * edge by ▲ ▼, its left or right by ◀ ▶ (the other two are drawn dim, as the row drew two pairs) -- and everything
     * else by all four. The step's cell and ✓ always answer.
     */
    fun live(st: State, k: Key): Boolean = when (k) {
        Key.STEP, Key.DONE -> true
        Key.UP, Key.DOWN -> st.part != "l" && st.part != "r" // text:value a mark's left and right edge, the tabs' keys
        Key.LEFT, Key.RIGHT -> st.part != "t" && st.part != "b" // text:value a mark's top and bottom edge, the tabs' keys
    }

    // ------------------------------------------------------------------ a press

    /** What a press did: the Fine move after it ([state]; null: it has ENDED, by ✓), and whether anything [moved]. */
    class Pressed(val state: State?, val moved: Boolean)

    private fun r1(v: Double): Double = Math.round(v * 10) / 10.0

    /** Whether two marks stand in the same place with the same shape: their boxes, and their four corners in mm. */
    private fun same(x: SurveyPrep.Mark, y: SurveyPrep.Mark): Boolean =
        x.x0 == y.x0 && x.x1 == y.x1 && x.y0 == y.y0 && x.y1 == y.y1 &&
            Quad.corners(x).zip(Quad.corners(y)).all { (p, q) -> p[0] == q[0] && p[1] == q[1] }

    /** [v] moved by [d] along a face [lim] long, to 0.1 mm: kept on the face -- and a point that already stands past
     *  an edge (a tape plotted on the boundary line) is never pulled back by a nudge the other way, nor pushed further out. */
    private fun slide(v: Double, d: Double, lim: Double): Double = if (d == 0.0) v else r1((v + d).coerceIn(min(0.0, v), max(lim, v)))

    /**
     * A PRESS ON THE PAD'S CELL [k] during the Fine move [st], on an [a] x [b] face:
     *  - ✓ ends it; the centre cell switches the step; neither moves anything;
     *  - AN ARROW moves the part by the step ([delta]) and NEVER SNAPS: a mark through [SurveyPrep.moveMark] -- the
     *    whole mark kept on the face, an edge never under [SurveyPrep.MIN_SIZE], a corner never breaking its shape
     *    ([Quad.drag] with no snapping distance); a Tape, a Line or an area whole through [SurveyGuide.bodyMove], which
     *    stops at the edge with its shape intact; one of its points alone, kept on the face, a Tape or a Line never
     *    left shorter than [SurveyMeas.MIN_LEN]. Its readings are not touched: moving does not change what was measured.
     * [snapshot] is called ONCE in a Fine move, just before the first write that changes anything -- never for a press
     * that moves nothing (an arrow the part does not answer to, an item hard against the edge, a locked item, one that
     * is gone).
     */
    fun press(p: SurveyPrep.Prep, st: State, k: Key, a: Double, b: Double, snapshot: () -> Unit): Pressed {
        if (k == Key.DONE) return Pressed(null, false)
        if (k == Key.STEP) return Pressed(toggle(st), false)
        val d = delta(k, st.stepMm) ?: return Pressed(st, false)
        if (!live(st, k)) return Pressed(st, false)
        fun step() { if (!st.stepTaken) snapshot() }
        var moved = false
        val mark = p.markById(st.id)?.second; val meas = p.measById(st.id)?.second
        if (mark != null && !SurveyPrep.isLocked(mark) && st.part in parts(p, st.id)) {
            // tried on a copy first: the one ↶ step is taken only for a nudge that changes the mark
            val t = mark.copy()
            if (SurveyPrep.moveMark(t, mark.copy(), st.part, d[0], d[1], a, b) && !same(t, mark)) {
                step(); moved = SurveyPrep.moveMark(mark, mark.copy(), st.part, d[0], d[1], a, b)
            }
        } else if (meas != null && !SurveyPrep.isLocked(meas)) {
            val i = pointOf(st.part)
            if (st.part == BODY) {
                val start = meas.pts.map { it.copyOf() }
                val by = SurveyGuide.bodyDelta(start, d[0], d[1], a, b)
                if (by[0] != 0.0 || by[1] != 0.0) { step(); moved = SurveyGuide.bodyMove(meas, start, d[0], d[1], a, b) }
            } else if (i != null && i in meas.pts.indices) {
                val q = doubleArrayOf(slide(meas.pts[i][0], d[0], a), slide(meas.pts[i][1], d[1], b))
                val pts = meas.pts.mapIndexed { n, pt -> if (n == i) q else pt }
                val long = meas.kind == SurveyMeas.AREA || pts.size != 2 || SurveyMeas.plotted(SurveyMeas.plen(pts))
                if (long && (q[0] != meas.pts[i][0] || q[1] != meas.pts[i][1])) { step(); meas.pts[i] = q; moved = true }
            }
        }
        return Pressed(if (moved) st.copy(stepTaken = true) else st, moved)
    }

    // ------------------------------------------------------------------ when it ends

    /**
     * THE FINE MOVE AFTER ANYTHING ELSE HAPPENED ON THE SCREEN ([pick] the state then): it goes on only while ITS item
     * is still picked AND HELD and may still be nudged ([MarkSheet.fineOk]) -- its part kept where the item still has
     * it, else the whole item. Null: IT HAS ENDED --
     *  - a tap on empty photo ([Pick.empty], row 10: the hold goes);
     *  - a reading that lands, passing the check or not ([Pick.landed], rows 18 and 19: "held NO") -- "a reading that
     *    lands while its pad is up ends Fine move as ✓ does (every nudge is already written) and then moves on as any
     *    reading" (§4.3, 6); the pad never holds a reading back (it is no sheet and no finger on a grab);
     *  - `▸`, ↶, another item picked, the item locked or deleted, another face or step, the stage moved under it.
     * A tap on a FIGURE of the same item, held, turns it gold and leaves the pad up (row 4 keeps the hold).
     */
    fun after(p: SurveyPrep.Prep, st: State?, pick: Pick.State, stage: Int?): State? {
        if (st == null || pick.picked != st.id || !pick.held || !MarkSheet.fineOk(p, st.id, stage)) return null
        return if (st.part in parts(p, st.id)) st else st.copy(part = BODY)
    }

    /**
     * WHETHER A TAP WAS ON AN ITEM'S BODY -- [hit] and [line] as the screen hands them to [Pick.tap]: a mark pressed
     * whole, the picked Tape's, Line's or area's band, or the Tape, Line, area or chain whose line the finger was on.
     * Not a handle, a figure, the code chip, or empty photo.
     */
    fun onBody(hit: SurveyPrepTouch.Hit?, line: String?): Boolean = when (hit) {
        is SurveyPrepTouch.Hit.Mark -> hit.part == BODY
        is SurveyPrepTouch.Hit.Body -> true
        null -> line != null
        else -> false
    }

    /**
     * THE PART LAST TOUCHED -- what Fine move nudges as it starts ([start]'s `part`), kept by the screen from one
     * [Pick] event to the next ([part] before the event, [before] the pick before it, [out] Pick's answer, [body]: the
     * event was a tap or a long press on an item's body): a handle, a corner or an end pressed IS the part
     * ([Pick.Out.nudge]); a press on a body, another item picked, or the hold gone, and it is the whole item again;
     * ANYTHING ELSE LEAVES IT -- a tap on the code chip above all, which opens the panel `Fine move` is in: the edge he
     * dragged a moment ago is the edge its arrows then move. (Until slice I2b every event but a handle's reset it, and
     * the row's nudges, always on screen, did not care.)
     */
    fun touched(part: String, before: Pick.State, out: Pick.Out, body: Boolean): String {
        val nudge = out.nudge
        return when {
            nudge != null -> nudge
            body || out.state.picked != before.picked || !out.state.held -> BODY
            else -> part
        }
    }

    /**
     * THE FINE MOVE AFTER A [Pick] EVENT, AIM AND ALL -- what the screen asks after every event it hands to Pick (port
     * 63 slice I2b): first the aim, then [after]. [nudge]: the event's own answer to "which part do the nudges move"
     * ([Pick.Out.nudge] -- a handle, a corner, an end of the held item pressed or tapped): THAT is the part nudged now
     * ([aim]). [body]: the event was a tap on an item's BODY -- the whole item again (decision 11 of I2a). Neither: the
     * aim stays -- a tap on one of its figures turns it gold and leaves the pad as it is. Then [after]: the pad goes
     * whenever its item is no longer picked and held.
     */
    fun afterEvent(p: SurveyPrep.Prep, st: State?, pick: Pick.State, stage: Int?, nudge: String?, body: Boolean): State? {
        val aimed = st?.let { s -> if (nudge != null) aim(p, s, nudge) else if (body) aim(p, s, BODY) else s }
        return after(p, aimed, pick, stage)
    }
}
