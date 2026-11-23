package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Msg
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A MARK WITH FOUR FREE CORNERS (port 63 slice Q1, 16 Nov 2026; android/docs/port63/DESIGN-quad.md §1, §2, §3.1-3.3).
 * Amit, 16 Nov 2026, after a balcony window job surveyed with the phone: "while marking window, I realised that it can
 * draw only rectangular windows. there are scenarios when i need to draw trapezoidal windows as well like one just
 * exported." -- and, on the selector the same afternoon, "Four corners": every box mark can become a four-corner shape;
 * it starts as a rectangle, any corner is dragged, a side close to upright or level snaps straight, and EACH OF THE
 * FOUR SIDES CARRIES ITS OWN FIGURE.
 *
 * THE RECORD. A mark may carry ONE new key, [KEY], its four corners as PARTS OF ITS BOX -- bottom-left, bottom-right,
 * top-right, top-left ([PARTS] q0 … q3, the order the reference rectangle's `q` has), each `[px, py]` with
 * `px = (x − x0) / (x1 − x0)` and `py = (y − y0) / (y1 − y0)`, y up. The box STAYS and is the shape's bounding box by
 * construction, so every reader that knows only x0 x1 y0 y1 -- S5 /projects, S4's "Room from survey", an older phone
 * build, the plan's wall bands, the 360 -- keeps reading a rectangle that holds the shape exactly. The key is stored
 * ONLY WHEN THE SHAPE IS NOT ITS BOX; it lives in [SurveyPrep.Mark.extra] like `sub`, `life` and `locked`, and that map
 * is replaced, never changed in place. PARTS, NOT MM: there is one statement of where the mark is (the box) and one of
 * its shape (this key), so a body move, a nudge, an edge snap and a duplicate need no corner code, an older build that
 * moves the box carries the shape with it, and a box and corners that disagree cannot exist.
 *
 * WHEN A `quad` CAN BE READ ([read], the same five tests on the phone, S5 and S4, on the parts alone): four pairs of
 * JSON NUMBERS from 0 to 1 (text that looks like a number is not one); it spans its box; each side keeps its direction;
 * it is convex and uncrossed; and the mark may have one ([mayHave]: not a point, not a Note). `[[0,0],[1,0],[1,1],[0,1]]`
 * says what the box says and is read as no key ([identity]); anything else unreadable leaves the mark its box, and the
 * phone says so on opening ([SurveyPrepFile.decode]).
 *
 * THE FIGURES ([asked]): a four-corner mark is measured by its four SIDES ([SIDES]: `bottom` `right` `top` `left`,
 * side i from corner i to corner i + 1), by the diagonal q0-q2 ([DIAG]) when it has no square corner -- four side
 * lengths alone never fix a four-sided shape -- and then by everything its type's rectangle asks except `w` and `h`.
 * A reading key never means two things: `w` and `h` stay the box's overall size and are not asked; the sides have
 * their own names, inside S5's key grammar. With exactly three straight sides the fourth is worked out from the other
 * three readings ([closing]) -- the one new check.
 *
 * NO FIGURE NOBODY MEASURED (port 63 slice Q2a; DESIGN-imagemeter §18; DESIGN-quad §11): a side's length corner to
 * corner ([asked]'s `est`) is what the GROSS GUARD compares a reading with ([MarkDims.guard]) and is never shown on the
 * phone -- an unread side shows nothing, or `?` on the picked mark ([PhotoFigures]) -- and the closing check's sentence
 * names the side and quotes no number ([MeasureText.sidesSay]).
 *
 * Pure. The screen (slice Q2) draws and drags from it, and asks the rules under "on the screen" below.
 */
object Quad {

    /** The one new key of a mark. */
    const val KEY = "quad"
    /** The four corners as the parts a press grabs and a nudge moves: bottom-left, bottom-right, top-right, top-left.
     *  No name holds `l`, `r`, `t` or `b`, which [SurveyPrep.moveMark] and [SurveyPrep.snapMark] test by letter. */
    val PARTS: List<String> = listOf("q0", "q1", "q2", "q3")
    /** The four sides' measure names, side i from corner i to corner i + 1: whole words, so `FW1.left 780` reads without
     *  a table. Letters only, 3 to 6 long: S5's KEY_RE takes them, none is taken, and none is a point's two-letter c-key. */
    val SIDES: List<String> = listOf("bottom", "right", "top", "left")
    /** The diagonal's measure name: from q0 to q2, bottom-left to top-right. */
    const val DIAG = "diag"
    /** The closing check fails past the larger of this, mm ... */
    const val CLOSE_MM = 25.0
    /** ... and this part of the derived length. Claude's starting figures: a corner 1° out of square moves the design's
     *  example by about 13 mm, so it stays quiet on an ordinary opening and fires on a wrong target. */
    const val CLOSE_PART = 0.01
    /** How far OUTSIDE the line joining its two neighbours every corner stays, mm ([sound]): only what keeps a shape a
     *  shape -- uncrossed, no corner turned inward, never flattened to a triangle -- and ten steps of the 0.1 mm a corner
     *  is placed to. NOT [SurveyPrep.MIN_SIZE], which is a SIDE's floor (the review of Q1, finding 1): a W x H
     *  rectangle's corner stands W·H / √(W² + H²) outside that line, always under its shorter side, so at 20 mm a mark
     *  squeezed to the floor a tab stops at could never have a corner moved, not even outward. Every rectangle a tab can
     *  leave stands at least 20 / √2 = 14.1 mm clear. */
    const val OUT_MM = 1.0

    // ------------------------------------------------------------------ the record

    /** Whether a mark of type [t] may have a `quad`: every mark that is not a spot (a point, or a Note's pin). */
    fun mayHave(t: String): Boolean = !SurveyPrep.isSpot(t)
    fun mayHave(e: SurveyPrep.Mark): Boolean = mayHave(e.t)

    /** One part: a JSON NUMBER, finite, from 0 to 1. NEVER text that looks like one -- S5's reader tests
     *  `typeof v === 'number'` and S4's `is_a?(Numeric)`, and the three must read one file one way. */
    private fun part(v: Any?): Double? = (v as? Number)?.toDouble()?.takeIf { it.isFinite() && it >= 0.0 && it <= 1.0 }

    /** Rule 1: a list of exactly four items, each a list of exactly two parts. */
    private fun pairs(v: Any?): List<DoubleArray>? {
        val l = v as? List<*> ?: return null
        if (l.size != 4) return null
        return l.map { p ->
            val q = p as? List<*> ?: return null
            if (q.size != 2) return null
            doubleArrayOf(part(q[0]) ?: return null, part(q[1]) ?: return null)
        }
    }

    /** `[[0,0],[1,0],[1,1],[0,1]]`: the box itself. */
    private fun isBox(q: List<DoubleArray>): Boolean =
        q[0][0] == 0.0 && q[0][1] == 0.0 && q[1][0] == 1.0 && q[1][1] == 0.0 && q[2][0] == 1.0 && q[2][1] == 1.0 && q[3][0] == 0.0 && q[3][1] == 1.0

    /** Rules 2 to 4, on the parts: it spans its box; each side keeps its direction (the bottom and the top run left to
     *  right, the left and the right upward -- what makes "left side" mean one thing on every device); and at every
     *  corner the turn from one side to the next is to the left (convex, uncrossed). */
    private fun shape(q: List<DoubleArray>): Boolean {
        if (q.minOf { it[0] } != 0.0 || q.maxOf { it[0] } != 1.0 || q.minOf { it[1] } != 0.0 || q.maxOf { it[1] } != 1.0) return false
        if (!(q[1][0] > q[0][0] && q[2][0] > q[3][0] && q[3][1] > q[0][1] && q[2][1] > q[1][1])) return false
        for (i in 0 until 4) {
            val a = q[i]; val b = q[(i + 1) % 4]; val c = q[(i + 2) % 4]
            if (!((b[0] - a[0]) * (c[1] - b[1]) - (b[1] - a[1]) * (c[0] - b[0]) > 0)) return false
        }
        return true
    }

    /**
     * THE PARTS A STORED VALUE SAYS, when it is a shape that is not its box: rules 1 to 4 of DESIGN-quad §1.2, on the
     * parts alone, so the box plays no role and no stretch by any reader can break it. Null for anything else -- the
     * box's own four corners included ([identity]). Rule 5, that the mark may have one, is [of]'s.
     */
    fun read(v: Any?): List<DoubleArray>? = pairs(v)?.takeIf { !isBox(it) && shape(it) }

    /** Whether a stored value is exactly the box's four corners: it says what the box says, and is read as no key. */
    fun identity(v: Any?): Boolean = pairs(v)?.let(::isBox) == true

    /** The mark's four corners as parts of its box, when it carries a readable `quad` and may have one; else null:
     *  the mark is its box. */
    fun of(e: SurveyPrep.Mark): List<DoubleArray>? = if (mayHave(e)) read(e.extra[KEY]) else null

    /** A part of the way from [lo] to [hi]: exactly the edge at 0 and at 1. */
    private fun at(lo: Double, hi: Double, p: Double): Double = if (p == 0.0) lo else if (p == 1.0) hi else lo + p * (hi - lo)

    /** THE FOUR CORNERS IN MM on the surface, bottom-left, bottom-right, top-right, top-left -- the box's own four when
     *  the mark has no `quad`. A corner on a box edge is exactly on it. */
    fun corners(e: SurveyPrep.Mark): List<DoubleArray> {
        val q = of(e) ?: return listOf(doubleArrayOf(e.x0, e.y0), doubleArrayOf(e.x1, e.y0), doubleArrayOf(e.x1, e.y1), doubleArrayOf(e.x0, e.y1))
        return q.map { doubleArrayOf(at(e.x0, e.x1, it[0]), at(e.y0, e.y1, it[1])) }
    }

    /** Which corner a part is: 0 … 3 for q0 … q3; null for any other part (body, a tab's letters). */
    fun cornerOf(part: String?): Int? = PARTS.indexOf(part).takeIf { it >= 0 }

    /**
     * WHICH HANDLES A MARK SHOWS -- THE ONE RULE (DESIGN-quad §3.1): its four free corners when it may have a `quad`
     * and (1) it has one, or (2) its id is [freeId] -- the mark whose corners the menu's "Four corners" freed, or whose
     * corner was last grabbed -- or (3) [part], the picked part of THIS mark, is one of [PARTS]. Else its eight tabs.
     * Rule 3 is there because a shape is its box the moment a corner is dragged or snapped back, and the key goes at
     * that moment, in the middle of the drag: with rules 1 and 2 alone a mark opened from the file ([freeId] null)
     * would turn into a rectangle with eight tabs under a finger that still holds q0. Every place that asks which
     * handles to show asks this.
     */
    fun free(e: SurveyPrep.Mark, freeId: String?, part: String?): Boolean =
        mayHave(e) && (of(e) != null || (freeId != null && e.id == freeId) || cornerOf(part) != null)

    // ------------------------------------------------------------------ on the screen (port 63 slice Q2a, 23 Nov 2026)

    /*
     * WHAT THE SCREEN ASKS WHEN IT TURNS THE FOUR-CORNER MARK ON (DESIGN-quad §11). :app is compiled only by CI, so
     * every rule it follows is here, pure and tested (QuadScreenTest), and :app only draws and calls.
     */

    /**
     * HOW `freeId` LIVES (DESIGN-quad §3.1) -- the id of the mark whose corners are free with no `quad` stored, after
     * ANY event on the screen: [freeId] before it, [picked] the item picked now ([Pick.State.picked]), [part] the part
     * of it last touched ([NudgePad.touched]: a handle pressed, else the whole item):
     *  - nothing picked: null;
     *  - the part last touched is one of its CORNERS ([PARTS]): [picked] -- a grab of a corner frees the mark, so a shape
     *    dragged or snapped back to its box keeps its four corners, also after a later press on its body;
     *  - [freeId] is still the picked one: kept -- it was set by the panel's `Four corners` ([MarkSheet.shapeStep]) or by
     *    an earlier corner grab;
     *  - THE SELECTION LEFT THAT MARK: null. Only then is a shape that is its box a rectangle with eight tabs again.
     * ONE FUNCTION, asked by the screen after every event that can move the pick or the part, on the photo and on the
     * 360 alike (they share the screen's one `freeId`). It answers ids only: whether that mark may have four corners at
     * all is [free]'s first test.
     */
    fun freed(freeId: String?, picked: String?, part: String?): String? = when {
        picked == null -> null
        cornerOf(part) != null -> picked
        freeId == picked -> freeId
        else -> null
    }

    /**
     * [free] FOR EVERY MARK OF THE SCREEN AT ONCE: [part] is the picked part of the PICKED mark alone -- for any other
     * mark it is null (§3.1, rule 3). The ONE value the painter, the press ([SurveyPrepTouch.Drawn.corners]), the code
     * chip ([SurveyPrepTouch.handlesOf]), the figures' layout ([PhotoFigures.layout]'s `free`) and the 360 are handed,
     * so no two of them can work it out differently.
     */
    fun freeOf(freeId: String?, picked: String?, part: String?): (SurveyPrep.Mark) -> Boolean =
        { e -> free(e, freeId, if (e.id == picked) part else null) }

    /** [freeOf] for the item [id] of [p] -- what Fine move is told as it starts ([NudgePad.start]'s `free`), by the id
     *  the panel's act carries. False for an id that is no mark (a Tape, a Line, a chain: none has corners to free). */
    fun freeIn(p: SurveyPrep.Prep, id: String, freeId: String?, picked: String?, part: String?): Boolean =
        p.markById(id)?.second?.let { freeOf(freeId, picked, part)(it) } == true

    /** What a mark is grabbed by. */
    enum class Handles {
        /** None: it is not held, it is locked, or it is a spot (a point, a Note's pin), which is grabbed whole. */
        NONE,
        /** Its eight resize tabs ([SurveyPrepTouch.TABS]). */
        TABS,
        /** Its four round corners, q0 … q3 ([PARTS]). */
        CORNERS,
    }

    /**
     * WHICH HANDLES THE MARK [e] SHOWS: none unless it [shows] them at all -- the caller's own condition, as built: it
     * is the picked mark, HELD ([Pick.handles]), on the editing photo, and not locked -- and never on a spot; its four
     * CORNERS where they are [free] ([freeOf]); else its eight TABS. The painter draws by this, the code chip keeps
     * clear of these ([SurveyPrepTouch.handlesOf]), and the figures' layout keeps them clear ([DimLayout.markShown]).
     */
    fun handles(e: SurveyPrep.Mark, shows: Boolean, free: Boolean): Handles = when {
        !shows || SurveyPrep.isSpot(e.t) -> Handles.NONE
        free && mayHave(e) -> Handles.CORNERS
        else -> Handles.TABS
    }

    /**
     * THE CORNERS A PRESS AND THE CHIP ARE HANDED, mm on the surface -- q0 … q3, the box's own four on a mark with no
     * `quad` -- when the mark's corners are [free]; null: it is pressed and kept clear of as a rectangle. The caller maps
     * each through its own face-to-canvas function: `corners =` of [SurveyPrepTouch.Drawn] for EVERY mark of the face
     * (a four-corner mark's body counts only inside its shape, picked or not), and the third argument of
     * [SurveyPrepTouch.handlesOf] for the picked one.
     */
    fun freeCorners(e: SurveyPrep.Mark, free: Boolean): List<DoubleArray>? = if (free && mayHave(e)) corners(e) else null

    /** A miter is never longer than this many times the distance grown by: a corner a few degrees wide would otherwise
     *  throw the frame's corner across the photo. */
    const val MITER = 4.0

    /**
     * THE OUTLINE [pts] GROWN BY [by] ON EVERY SIDE (canvas px, either winding) -- what the gold frame round the mark
     * being measured ([MarkDims.ACTIVE_GAP_DP] outside it) and the hole in the dimmed photo round the picked mark are
     * drawn by, ROUND THE SHAPE (DESIGN-quad §3.4; §10 note 4) where they were drawn round the box. Each side is moved
     * [by] out, square to itself, and the corners are where the moved sides meet -- so a rectangle's four corners grown
     * are exactly its box grown by [by], which is what a mark with no `quad` has always been framed by. A corner so
     * sharp that the moved sides would meet more than [MITER] x [by] away is cut back to that. Fewer than three points,
     * or an outline with no area: [pts] as given.
     */
    fun grown(pts: List<DoubleArray>, by: Double): List<DoubleArray> {
        val n = pts.size
        if (n < 3 || by == 0.0) return pts.map { it.copyOf() }
        var area = 0.0
        for (i in 0 until n) { val q = pts[(i + 1) % n]; area += pts[i][0] * q[1] - q[0] * pts[i][1] }
        if (area == 0.0) return pts.map { it.copyOf() }
        // the unit normal of the side from corner i to the next, pointing OUT of the outline
        val sgn = if (area > 0) 1.0 else -1.0
        val nrm = (0 until n).map { i ->
            val p = pts[i]; val q = pts[(i + 1) % n]; val dx = q[0] - p[0]; val dy = q[1] - p[1]; val len = hypot(dx, dy)
            if (len > 0) doubleArrayOf(sgn * dy / len, -sgn * dx / len) else null
        }
        return (0 until n).map { i ->
            val a = nrm[(i + n - 1) % n] ?: nrm[i]; val b = nrm[i] ?: a
            if (a == null || b == null) return@map pts[i].copyOf()
            // where the two moved sides meet: by · (a + b) / (1 + a·b). |a + b| = m and 1 + a·b = m² / 2, so that point is
            // 2 / m times [by] from the corner, along a + b -- cut back to MITER where the corner is too sharp
            val mx = a[0] + b[0]; val my = a[1] + b[1]; val m = hypot(mx, my)
            // (two sides folded straight back on each other have no meeting point: out across the side that arrives)
            if (!(m > 0)) return@map doubleArrayOf(pts[i][0] + by * a[0], pts[i][1] + by * a[1])
            val k = min(MITER, 2 / m) / m
            doubleArrayOf(pts[i][0] + by * mx * k, pts[i][1] + by * my * k)
        }
    }

    private fun r1(v: Double): Double = Math.round(v * 10) / 10.0
    private fun r6(v: Double): Double = Math.round(v * 1e6) / 1e6

    /** What a shape may not have (DESIGN-quad §1.3), mm: a side running under [SurveyPrep.MIN_SIZE] in its own
     *  direction -- the floor a tab's resize stops at, so a rectangle and a four-corner mark have ONE smallest side --
     *  or a corner less than [OUT_MM] outside the line joining its two neighbours: the one test that forbids crossed
     *  sides, a corner turned inward and a shape flattened to a triangle. */
    private fun sound(c: List<DoubleArray>): Boolean {
        val m = SurveyPrep.MIN_SIZE - 1e-6; val out = OUT_MM - 1e-6
        if (c[1][0] - c[0][0] < m || c[2][0] - c[3][0] < m || c[3][1] - c[0][1] < m || c[2][1] - c[1][1] < m) return false
        for (i in 0 until 4) {
            val p = c[(i + 3) % 4]; val n = c[(i + 1) % 4]; val q = c[i]
            val dx = n[0] - p[0]; val dy = n[1] - p[1]; val len = hypot(dx, dy)
            if (!(len > 0)) return false
            // the polygon runs counter-clockwise, so a corner stands to the RIGHT of the line from its neighbour before to its neighbour after
            if (-(dx * (q[1] - p[1]) - dy * (q[0] - p[0])) / len < out) return false
        }
        return true
    }

    /** [set], answering the corners as placed (0.1 mm) when the shape was taken; null, and [e] untouched, when refused. */
    private fun place(e: SurveyPrep.Mark, mm: List<DoubleArray>): List<DoubleArray>? {
        if (!mayHave(e) || mm.size != 4 || mm.any { it.size < 2 || !it[0].isFinite() || !it[1].isFinite() }) return null
        val c = mm.map { doubleArrayOf(r1(it[0]), r1(it[1])) }
        if (!sound(c)) return null
        val x0 = c.minOf { it[0] }; val x1 = c.maxOf { it[0] }; val y0 = c.minOf { it[1] }; val y1 = c.maxOf { it[1] }
        val parts = c.map { listOf(r6((it[0] - x0) / (x1 - x0)), r6((it[1] - y0) / (y1 - y0))) }
        val q = parts.map { doubleArrayOf(it[0], it[1]) }
        val box = isBox(q)
        // never write what the three readers would not read back
        if (!box && !shape(q)) return null
        e.x0 = x0; e.x1 = x1; e.y0 = y0; e.y1 = y1
        e.extra = if (box) without(e.extra) else SurveyPrep.withKey(e.extra, KEY, parts)
        return c
    }

    private fun without(m: Map<String, Any?>): Map<String, Any?> = if (KEY !in m) m else LinkedHashMap(m).apply { remove(KEY) }

    /**
     * THE MARK [e] GIVEN THE FOUR CORNERS [mm] (bottom-left, bottom-right, top-right, top-left, mm on its surface): each
     * placed to 0.1 mm, as a Line's end is; the box set to their bounding box; and the parts written, to 6 decimals
     * (under 0.01 mm on any wall; a corner on a box edge is exactly 0 or 1), as a list of four lists of two numbers in
     * a NEW extra map -- or the key REMOVED when the shape is its box. False, and nothing changed, when the shape is
     * refused ([sound]: DESIGN-quad §1.3) or the mark may not have one. Keeping a corner on the face is [drag]'s.
     */
    fun set(e: SurveyPrep.Mark, mm: List<DoubleArray>): Boolean = place(e, mm) != null

    /** "Rectangle": the key removed, in a new extra map; the box does not move -- the mark becomes its bounding
     *  rectangle. False when it had none. */
    fun clear(e: SurveyPrep.Mark): Boolean {
        if (KEY !in e.extra) return false
        e.extra = without(e.extra)
        return true
    }

    // ------------------------------------------------------------------ dragging a corner

    /** Where a dragged corner went: [p], mm (0.1), and which of its two sides are now straight ([snap]: "h" its level
     *  side, "v" its upright side, "hv" both -- a square corner -- "d" neither), which is the guide ray drawn bold. */
    class Drag(val p: DoubleArray, val snap: String)

    /**
     * CORNER [i] DRAGGED (DESIGN-quad §3.2): the corner of the mark as it was when the drag started ([o]) moved by
     * ([dx], [dy]) mm on an [a] x [b] face, in this order -- kept on the face; taken onto its upright neighbour's x when
     * the side between them is within [SurveyMeas.SNAP_DEG] of upright and the corner within [tol] mm of that line, and
     * onto its level neighbour's y the same way; on an axis no side fixed, onto the face's edge within
     * [SurveyPrep.SNAP]; then [set]. [tol] 0 snaps nothing at all (a nudge never snaps). A mark with no key drags too:
     * its corners are the box's four. Null, and [e] where it last was, when the mark is locked, may not have a `quad`,
     * or the shape would be refused.
     */
    fun drag(e: SurveyPrep.Mark, o: SurveyPrep.Mark, i: Int, dx: Double, dy: Double, a: Double, b: Double, tol: Double): Drag? {
        if (SurveyPrep.isLocked(e) || !mayHave(e) || i !in 0..3) return null
        val c = corners(o)
        var x = (c[i][0] + dx).coerceIn(0.0, a); var y = (c[i][1] + dy).coerceIn(0.0, b)
        val up = c[3 - i]; val lv = c[i xor 1]
        if (tol > 0) {
            val v = straightens(x - up[0], y - up[1], tol); val h = straightens(y - lv[1], x - lv[0], tol)
            if (v) x = up[0] else if (x < SurveyPrep.SNAP) x = 0.0 else if (a - x < SurveyPrep.SNAP) x = a
            if (h) y = lv[1] else if (y < SurveyPrep.SNAP) y = 0.0 else if (b - y < SurveyPrep.SNAP) y = b
        }
        val placed = place(e, c.mapIndexed { k, p -> if (k == i) doubleArrayOf(x, y) else p }) ?: return null
        val p = placed[i]
        val v = p[0] == placed[3 - i][0]; val h = p[1] == placed[i xor 1][1]
        return Drag(p.copyOf(), if (h && v) "hv" else if (h) "h" else if (v) "v" else "d")
    }

    /** Whether a side that is [across] mm off its neighbour's line and runs [along] mm snaps straight: within 5° of
     *  that line, and within [tol] mm of it. */
    private fun straightens(across: Double, along: Double, tol: Double): Boolean =
        hypot(across, along) >= 1 && abs(across) <= tol && Math.toDegrees(atan2(abs(across), abs(along))) <= SurveyMeas.SNAP_DEG

    /** The guide a corner drag shows: the two rays through the corner itself, as through an area's corner, the ray
     *  along a side that is straight drawn bold ([SurveyGuide.Rays.bold]) -- both on a square corner. */
    fun guide(s: String, r: Drag): SurveyGuide.Guide = SurveyGuide.Guide(s, r.p.copyOf(), SurveyPrepPalette.SEL, r.snap)

    // ------------------------------------------------------------------ what four sides fix

    /**
     * WHICH SIDES ARE STRAIGHT, bottom, right, top, left -- as the phone STORED them: the left or the right side is
     * upright when its two corners have the same px, the top or the bottom level when its two have the same py. A mark
     * with no `quad` has all four.
     */
    fun straight(e: SurveyPrep.Mark): List<Boolean> {
        val q = of(e) ?: return listOf(true, true, true, true)
        return listOf(q[0][1] == q[1][1], q[1][0] == q[2][0], q[2][1] == q[3][1], q[3][0] == q[0][0])
    }

    /**
     * WHERE THE SHAPE TOUCHES EACH EDGE OF ITS BOX, which is where a position figure's line ends (DESIGN-quad §2.3):
     * the y of its corner on the box's left edge ([yLeft]) and on its right ([yRight]), the x of its corner on the
     * bottom edge ([xBottom]) and on the top ([xTop]). Where TWO corners touch an edge -- that side is straight -- the
     * midpoint of the two, which for a rectangle is the middle of its edge, where its line ends today.
     */
    class Touch(val yLeft: Double, val yRight: Double, val xBottom: Double, val xTop: Double)

    fun touch(e: SurveyPrep.Mark): Touch {
        val xm = (e.x0 + e.x1) / 2; val ym = (e.y0 + e.y1) / 2
        val q = of(e) ?: return Touch(ym, ym, xm, xm)
        val c = corners(e)
        fun mid(axis: Int, edge: Double, of: Int, none: Double): Double =
            q.indices.filter { q[it][axis] == edge }.map { c[it][of] }.let { if (it.isEmpty()) none else it.average() }
        return Touch(mid(0, 0.0, 1, ym), mid(0, 1.0, 1, ym), mid(1, 0.0, 0, xm), mid(1, 1.0, 0, xm))
    }

    /** Whether the mark has a square corner -- both its sides straight: exactly when it has an upright side and a level
     *  side. Without one, four sides fix nothing and the diagonal is asked. */
    fun square(e: SurveyPrep.Mark): Boolean = straight(e).let { (it[1] || it[3]) && (it[0] || it[2]) }

    /** The level side that spans the box, `bottom` or `top` -- the one whose reading is the mark's overall width; null
     *  when it has none (and for a mark with no `quad`, whose width is its `w`). */
    fun spanW(e: SurveyPrep.Mark): String? {
        val q = of(e) ?: return null
        if (q[0][1] == q[1][1] && q[0][0] == 0.0 && q[1][0] == 1.0) return SIDES[0]
        if (q[2][1] == q[3][1] && q[3][0] == 0.0 && q[2][0] == 1.0) return SIDES[2]
        return null
    }

    /** The upright side that spans the box, `right` or `left` -- the one whose reading is the mark's overall height;
     *  null when it has none (and for a mark with no `quad`, whose height is its `h`). */
    fun spanH(e: SurveyPrep.Mark): String? {
        val q = of(e) ?: return null
        if (q[1][0] == q[2][0] && q[1][1] == 0.0 && q[2][1] == 1.0) return SIDES[1]
        if (q[3][0] == q[0][0] && q[0][1] == 0.0 && q[3][1] == 1.0) return SIDES[3]
        return null
    }

    // ------------------------------------------------------------------ the checklist

    /** Side [i]'s words on surface [s]: "bottom side", "right side", "top side", "left side" on a wall; on the floor
     *  and the ceiling the bottom is the "back side" and the top the "front side". */
    fun sideWhat(s: String, i: Int): Msg {
        val plan = SurveyPrep.isPlan(s)
        return when (i) {
            0 -> { tx -> if (plan) tx.measure_side_back else tx.measure_side_bottom }
            1 -> { tx -> tx.measure_side_right }
            2 -> { tx -> if (plan) tx.measure_side_front else tx.measure_side_top }
            else -> { tx -> tx.measure_side_left }
        }
    }

    /** The side a position figure runs to: `l` the left, `r` the right, `s` / `k` the bottom, `t` / `f` the top. */
    private fun sideOf(key: String): Int? = when (key) { "l" -> 3; "r" -> 1; "s", "k" -> 0; "t", "f" -> 2; else -> null }

    /**
     * EVERYTHING A MARK ASKS (DESIGN-quad §2.1-2.3) -- [base] itself, the very list, when the mark has no `quad`
     * ([base]: its type's rectangle checklist, [SurveyPrep.measures0]'s own body). A four-corner mark asks:
     *  - its four sides, `bottom` `right` `top` `left`, each with the distance between its two corners as its `est`
     *    (the side AS DRAWN: what the gross guard compares a reading with, and nothing a person reads on the phone --
     *    DESIGN-imagemeter §18) -- always, also on a type whose rectangle asks only a width (Column, Curtain track) or neither (a
     *    wall's Beam);
     *  - `diag`, q0 to q2, when it has no square corner;
     *  - then everything [base] asks EXCEPT `w` and `h`, in the type's order. The position figures keep their keys and
     *    their exact meaning -- `l r s t f k` run to the box's edge on every reader, and the box's edge is the shape's
     *    outermost corner there -- and where that side of the shape is not straight their words gain ", to its nearest
     *    corner", as a point's gain ", to centre". A Beam's `d` gains nothing: its line shows which corner.
     */
    fun asked(s: String, e: SurveyPrep.Mark, a: Double, b: Double, base: List<SurveyPrep.Measure>): List<SurveyPrep.Measure> {
        if (of(e) == null) return base
        val c = corners(e); val st = straight(e); val plan = SurveyPrep.isPlan(s)
        val out = ArrayList<SurveyPrep.Measure>()
        for (i in 0 until 4) { val p = c[i]; val q = c[(i + 1) % 4]; out += SurveyPrep.Measure(SIDES[i], sideWhat(s, i), hypot(q[0] - p[0], q[1] - p[1])) }
        if (!square(e)) out += SurveyPrep.Measure(DIAG, { tx -> if (plan) tx.measure_diagonal_plan else tx.measure_diagonal },
            hypot(c[2][0] - c[0][0], c[2][1] - c[0][1]))
        for (m in base) {
            if (m.key == "w" || m.key == "h") continue
            val side = sideOf(m.key)
            out += if (side != null && !st[side]) SurveyPrep.Measure(m.key, { tx -> m.what(tx) + tx.measure_to_nearest_corner }, m.est) else m
        }
        return out
    }

    // ------------------------------------------------------------------ the one new check

    /** The three straight sides against the fourth: the sloping [side] and its reading key [code], what the other three
     *  readings make it ([derived]), its own reading ([read]), and whether the two disagree ([fails]). */
    class Closing(val side: Int, val code: String, val derived: Double, val read: Double, val fails: Boolean)

    /**
     * THE CLOSING CHECK (DESIGN-quad §2.4): when EXACTLY three sides are straight and all four are read, the sloping
     * side is worked out from the other three readings -- `√(joining² + (parallelA − parallelB)²)`, the joining side
     * the one opposite it and the parallel pair its two neighbours -- and it [Closing.fails] when the sloping side's
     * reading differs from that by more than the larger of [CLOSE_MM] and [CLOSE_PART] of it. Readings against
     * readings, so it also runs on a wardrobe or a loft, where the photo check cannot. Null when the mark has no
     * `quad`, not exactly three straight sides, or a side unread. Nothing is stored.
     */
    fun closing(actual: Map<String, Double>, e: SurveyPrep.Mark): Closing? {
        if (of(e) == null) return null
        val st = straight(e)
        if (st.count { it } != 3) return null
        val i = st.indexOf(false)
        val r = SIDES.map { actual[e.code + "." + it] ?: return null }
        val joining = r[(i + 2) % 4]; val pa = r[(i + 1) % 4]; val pb = r[(i + 3) % 4]
        val derived = sqrt(joining * joining + (pa - pb) * (pa - pb))
        return Closing(i, e.code + "." + SIDES[i], derived, r[i], abs(r[i] - derived) > max(CLOSE_MM, CLOSE_PART * derived))
    }

    /** The mark's four side reading keys, `FW1.bottom` … `FW1.left`; none for a mark with no `quad`. */
    fun sideCodes(e: SurveyPrep.Mark): List<String> = if (of(e) == null) emptyList() else SIDES.map { e.code + "." + it }
}
