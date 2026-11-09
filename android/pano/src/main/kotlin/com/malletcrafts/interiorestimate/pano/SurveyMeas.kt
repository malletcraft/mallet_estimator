package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * LINE AND TAPE ON SURVEY PREP (prototype v52, sp.js 458-574; v52 slice 9). The two measuring tools, pure, so every
 * figure, snap, code and word is tested here; SurveyPrepScreen only dispatches and SurveyPrepDraw only draws.
 *
 * Amit, 14 Oct 2026 11:48 IST: "i am getting confused between measure. add one more tool called as Tape which act as
 * a actual tape and can plot measurements like a real tape measure . keep this measure tool as marking and as usual
 * its measurements can be given by disto or manual"; 06:30: "i can not move measure line once drawn"; 20:45: "i had
 * requested two separate tools. one tape measure like anothr is actual marking simple line." So:
 *  - TAPE (yellow, codes F-T1 …) is a steel tape pulled across the face: its figure is what the photo says, and it
 *    has NO reading -- no key, no reading box, no D2 strip;
 *  - LINE (light blue, codes F-M1 …) is a marked length -- or, built by taps, an area -- whose reading comes from the
 *    D2 or the keyboard: `<code>.len` for a length, `<code>.per` (its perimeter) for an area. S5 QUOTE accepts both
 *    keys (slice 3, live on stage), and KeyGrammarTest pins it.
 * Both live in the surface's `meas` ({id, code, kind, locked, pts}), so delete, lock, duplicate, undo, clear and
 * Start over treat them alike, and a prep without them loads unchanged.
 *
 * Item 7 (Amit, 14 Oct 2026 06:29: "currently measure line can be drawn in any direction. does it give correct
 * measures if drawn in any direction?"; 11:47: "does it make sense to let measure travel only in direction where it
 * can give real measurements by reference?"): every direction measures correctly for things lying flat ON the surface
 * (the whole plane is mapped: an 800 x 600 mm diagonal reads 1000), so a drag SNAPS onto the face's horizontal or
 * vertical within 5° and still allows a diagonal; and a drag that leaves the surface is refused, because nothing off
 * it measures correctly. The perspective guide rays and the faded grid are slice 10.
 */
object SurveyMeas {

    const val TAPE = "tape"
    const val LINE = "line"
    const val AREA = "area"

    /** Tape yellow and Line blue (sp.js TC, MC). */
    const val TAPE_COLOUR = 0xfacc15
    const val LINE_COLOUR = 0x7dd3fc

    /** A tape's band is 25 mm wide on the face (sp.js TAPE_W). */
    const val TAPE_W = 25.0
    /** ... drawn at least this many dp either side of its middle, so it can be seen and tapped. */
    const val TAPE_MIN_HALF_DP = 3.5

    /** A tape or a Line is plotted only from this length, mm (sp.js `plen(L.pts)>=20`). */
    const val MIN_LEN = 20.0

    /** A drag within this of the face's horizontal or vertical snaps onto it, degrees (sp.js SNAPDEG). */
    const val SNAP_DEG = 5.0

    /** A tap within this of an area's first corner, once it has three, closes it, dp (sp.js tolMM: 18 dp). */
    const val CLOSE_DP = 18.0

    /** With Select on, a tap within this of a Line or a tape's middle picks it, dp (sp.js mHit, 12 dp). */
    const val PICK_DP = 12.0

    /** A Line's arrow heads: 7 dp long, 3 dp either side; none on a line shorter than three heads (sp.js AHL, AHW). */
    const val HEAD_LEN_DP = 7.0
    const val HEAD_W_DP = 3.0

    // ------------------------------------------------------------ the words (sp.js, exactly; keyed in port 61c slice M3b)

    /** What a drag that left the surface is refused with, on release (sp.js offRefuse), with no undo entry. */
    fun offRefusal(tx: Tx) = tx.refuse_sp_only_things_surface_measure
    /** The caution line under every Tape and Line chip: the same English, the same key (sp.js caut). */
    fun caution(tx: Tx) = tx.refuse_sp_only_things_surface_measure
    /** The chip while a drag is off the surface. */
    fun offChip(tx: Tx) = tx.refuse_sp_off_surface_nothing_will
    /** A tap with Tape on and no drag. */
    fun tapeTap(tx: Tx) = tx.sp_msg_tape_drag_along_face
    /** The note over a tape in 3 · List and the List tab, which has no reading box. */
    fun tapeNote(tx: Tx) = tx.sp_no_reading_box_tape
    fun tapeChip(tx: Tx) = tx.sp_tape_drag_along_face
    fun lineChip(tx: Tx) = tx.sp_line_drag_marked_length

    /** The Line chip once an area is started: "Line · area · 3 corners · double-tap to end". */
    fun areaChip(tx: Tx, n: Int) = tx.sp_line_area_x_corner(n)

    // ------------------------------------------------------------ codes and keys

    /** The code counter key of a kind ([SurveyPrep.SEQ]): "-T" for a tape, "-M" for a Line or an area. */
    fun seqKey(kind: String) = if (kind == TAPE) "-T" else "-M"

    /** A kind's code prefix on [s]: F-T, F-M, G-T … */
    fun head(s: String, kind: String) = SurveyPrep.CODE.getValue(s) + seqKey(kind)

    /** The kinds this build draws and edits; any other a later build may write is kept in the file untouched. */
    fun known(kind: String) = kind == TAPE || kind == LINE || kind == AREA

    // ------------------------------------------------------------ figures

    /** The length along the points, mm. */
    fun plen(pts: List<DoubleArray>): Double {
        var t = 0.0
        for (i in 1 until pts.size) t += hypot(pts[i][0] - pts[i - 1][0], pts[i][1] - pts[i - 1][1])
        return t
    }

    /** The area inside the corners, mm² (the shoelace). */
    fun parea(pts: List<DoubleArray>): Double {
        var a = 0.0
        for (i in pts.indices) { val p = pts[i]; val q = pts[(i + 1) % pts.size]; a += p[0] * q[1] - q[0] * p[1] }
        return abs(a) / 2
    }

    /** The way round the corners and back to the first, mm. */
    fun pperim(pts: List<DoubleArray>): Double =
        if (pts.isEmpty()) 0.0 else plen(pts) + hypot(pts[0][0] - pts.last()[0], pts[0][1] - pts.last()[1])

    /** mm² as m², two places: "1.86 m²". */
    fun m2txt(a: Double): String = String.format(Locale.ROOT, "%.2f m²", a / 1e6) // text:value the unit m², a figure drawn as written

    /** A length as the screen says it: "1234 mm". */
    fun mmTxt(v: Double) = "${v.roundToInt()} mm" // text:value the unit mm, a figure drawn as written

    /**
     * WHAT A LINE ASKS FOR (sp.js measItems): a length, `len`, with the photo's figure; an area its perimeter, `per`
     * (the laser reads a length, not an area); a tape nothing -- its figure IS the photo's.
     */
    fun measures(m: SurveyPrep.Meas): List<SurveyPrep.Measure> = when (m.kind) {
        LINE -> listOf(SurveyPrep.Measure("len", { tx: Tx -> tx.life_length }, plen(m.pts)))
        AREA -> listOf(SurveyPrep.Measure("per", { tx: Tx -> tx.measure_perimeter }, pperim(m.pts)))
        else -> emptyList()
    }

    /** The reading key a Line takes (F-M1.len, F-M2.per); null for a tape (sp.js measAct). */
    fun key(m: SurveyPrep.Meas): String? = measures(m).firstOrNull()?.let { m.code + "." + it.key }

    /**
     * WHICH READING IS BEING TAKEN WHEN [m] IS PICKED (sp.js mPick): the one a list row named ([measure]) if any; else,
     * on a FRESH pick, the Line's own key -- and for a tape, which has none, nothing (active cleared); re-picking the
     * same item keeps [current].
     */
    fun pickActive(m: SurveyPrep.Meas, fresh: Boolean, measure: String?, current: String?): String? =
        measure ?: if (fresh) key(m) else current

    // ------------------------------------------------------------ the 5° snap

    /** Where a drag's far end goes, and onto which guide: "h" (the face's horizontal), "v" (its vertical), "d" (neither),
     *  null under 1 mm (too short to have a direction). */
    class Snap(val p: DoubleArray, val snap: String?)

    /**
     * Within [SNAP_DEG] of the face's horizontal (constant y in its mm) or vertical (constant x), the far point [p]
     * goes onto that guide through [o]; anything else stays a diagonal (sp.js axisSnap).
     */
    fun axisSnap(o: DoubleArray, p: DoubleArray): Snap {
        val dx = p[0] - o[0]; val dy = p[1] - o[1]
        if (hypot(dx, dy) < 1) return Snap(p.copyOf(), null)
        val a = Math.toDegrees(atan2(abs(dy), abs(dx)))
        if (a <= SNAP_DEG) return Snap(doubleArrayOf(p[0], o[1]), "h")
        if (a >= 90 - SNAP_DEG) return Snap(doubleArrayOf(o[0], p[1]), "v")
        return Snap(p.copyOf(), "d")
    }

    // ------------------------------------------------------------ a drag in progress

    /**
     * A TAPE OR A LINE BEING PULLED on [s] (sp.js st.tline / st.mline): from where the finger went down, [from], to
     * where it is now, snapped ([snap]); [off] once either end is off the [a] x [b] surface. Plotted on release only
     * when it is on the surface and at least [MIN_LEN] long ([end]).
     */
    class Draft(val s: String, val kind: String, val from: DoubleArray, val a: Double, val b: Double) {
        var to: DoubleArray = from.copyOf(); private set
        var snap: String? = null; private set
        var off: Boolean = !SurveyPrep.onFace(a, b, from[0], from[1]); private set
        /** The finger at [p] mm on the surface's plane. */
        fun move(p: DoubleArray) {
            val r = axisSnap(from, p)
            to = r.p; snap = r.snap
            off = !SurveyPrep.onFace(a, b, from[0], from[1]) || !SurveyPrep.onFace(a, b, to[0], to[1])
        }
        /** The finger where no direction meets the surface's PLANE (4 · 360, v52 slice 17; sp.js `if(!m2){ L.off=true }`):
         *  the far end stays where it was last on the plane, and the drag is off -- refused on release. */
        fun offPlane() { off = true }
        fun pts(): List<DoubleArray> = listOf(from, to)
        fun len() = plen(pts())
    }

    /** What the finger's lift does with a drag: refused (off the surface), plotted, or nothing (too short). */
    enum class End { REFUSED, PLOTTED, SHORT }

    fun end(d: Draft): End = if (d.off) End.REFUSED else if (plotted(d.len())) End.PLOTTED else End.SHORT

    /** A tape (or a Line) is plotted only at [MIN_LEN] or more: a 19 mm tape is not. */
    fun plotted(len: Double) = len >= MIN_LEN

    /**
     * WHAT THE CHIP SAYS WHILE A TAPE OR A LINE IS PULLED (sp.js dirText): "↔ along the face · 1234 mm", "↕ up the
     * face · 1234 mm" or "diagonal on the face · 1234 mm" on a wall; on the floor and the ceiling "↔ left–right on the
     * floor · …" and "↕ front–back on the ceiling · …"; and "Off this surface — nothing will be plotted" while off it.
     */
    fun dirText(tx: Tx, s: String, len: Double, snap: String?, off: Boolean): String {
        if (off) return offChip(tx)
        val n = len.roundToInt().toString(); val pl = SurveyPrep.isPlan(s)
        val w = if (s == "floor") tx.sp_on_the_floor else tx.sp_on_the_ceiling
        return when (snap) {
            "h" -> if (pl) tx.sp_left_right_x_x(w = w, n = n) else tx.sp_along_face_x_mm(n)
            "v" -> if (pl) tx.sp_front_back_x_x(w = w, n = n) else tx.sp_up_face_x_mm(n)
            else -> tx.sp_diagonal_face_x_mm(n)
        }
    }

    fun dirText(tx: Tx, d: Draft) = dirText(tx, d.s, d.len(), d.snap, d.off)

    /**
     * THE TOOL CHIP with Tape ("T") or Line ("M") on: the live [drag] when there is one, else the tool's own words, or
     * the area being built ([corners] tapped so far); null for any other tool. The caution line ([caution]) goes under it.
     */
    fun chipText(tx: Tx, tool: String, drag: Draft?, corners: Int): String? = when {
        tool != "T" && tool != "M" -> null
        drag != null -> dirText(tx, drag)
        tool == "T" -> tapeChip(tx)
        corners > 0 -> areaChip(tx, corners)
        else -> lineChip(tx)
    }

    // ------------------------------------------------------------ the area built by taps

    /** What a tap with Line on does to the area being built (sp.js polyTap). */
    enum class Tap { REFUSED, STARTED, ADDED, CLOSED, IGNORED }

    /**
     * A tap at [p] mm with the area so far [pts] (empty: none started): off the surface, REFUSED; the first corner,
     * STARTED; a [double] tap, or one within the closing distance of the first corner ([nearFirst]), with three or more
     * corners, CLOSED (the double tap's own point is not added: the first tap of the pair already was); a double tap
     * with fewer, IGNORED; otherwise the corner is ADDED.
     */
    fun areaTap(pts: List<DoubleArray>, p: DoubleArray, a: Double, b: Double, double: Boolean, nearFirst: Boolean): Tap = when {
        !SurveyPrep.onFace(a, b, p[0], p[1]) -> Tap.REFUSED
        pts.isEmpty() -> Tap.STARTED
        (double || nearFirst) && pts.size >= 3 -> Tap.CLOSED
        double -> Tap.IGNORED
        else -> Tap.ADDED
    }

    // ------------------------------------------------------------ the tape's graduations

    /** One graduation: [at] mm from the hook end; [major] every 500 mm; [figure] its figure is printed. */
    class Tick(val at: Double, val major: Boolean, val figure: Boolean)

    /**
     * A STEEL TAPE'S GRADUATIONS (sp.js drawTapes): a tick every 100 mm from the start, a longer one every 500 mm, and
     * its figure only where it is at least 150 mm from either end, so a figure never sits on the tag at the far end.
     */
    fun tapeTicks(len: Double): List<Tick> {
        val out = mutableListOf<Tick>()
        var k = 100
        while (k < len - 1) {
            val major = k % 500 == 0
            out.add(Tick(k.toDouble(), major, major && len - k >= 150 && k >= 150))
            k += 100
        }
        return out
    }

    // ------------------------------------------------------------ the words on screen

    /** Its type name: "Tape", "Marked length", "Marked area" -- never "Measure" (item 6: the tool is the Line). */
    fun typeName(tx: Tx, m: SurveyPrep.Meas): String =
        when (m.kind) { TAPE -> tx.common_tape; LINE -> tx.common_marked_length; else -> tx.common_marked_area }

    /** The label every delete and the long-press menu use (sp.js itemById): "F-M1 marked length", "F-T1 tape". */
    fun label(tx: Tx, m: SurveyPrep.Meas): String =
        m.code + when (m.kind) { TAPE -> tx.sp_tape_suffix; LINE -> tx.sp_marked_length; else -> tx.sp_marked_area }

    /**
     * Its name under its code in 3 · List and the List tab (sp.js markPanel): "Tape · 1234 mm · from the photo",
     * "Marked length", "Marked area · 1.86 m²" -- and on the List tab, " · Front wall" after it ([room]).
     * sp.tape_x_mm_photo carries its own " mm" (the card's near-match ruling).
     */
    fun listName(tx: Tx, s: String, m: SurveyPrep.Meas, room: Boolean): String = when (m.kind) {
        TAPE -> tx.sp_tape_x_mm_photo(plen(m.pts).roundToInt().toString())
        LINE -> tx.common_marked_length
        else -> tx.sp_marked_area_x(m2txt(parea(m.pts)))
    } + if (room) " · " + Masters.surface(tx, SurveyPrep.NAME.getValue(s)) else "" // text:value the list separator

    /**
     * The selection row under the photo (sp.js 1507): a tape "Tape · 1234 mm"; a Line its reading "1234 mm" when it
     * has one, else the photo's "≈1234 mm" -- composed figures the prototype draws unchanged; an area its "1.86 m²".
     */
    fun rowValue(tx: Tx, m: SurveyPrep.Meas, actual: Map<String, Double>): String = when (m.kind) {
        TAPE -> tx.sp_tape_x_mm(plen(m.pts).roundToInt().toString())
        LINE -> actual[m.code + ".len"]?.let { mmTxt(it) } ?: "≈${mmTxt(plen(m.pts))}" // text:value a figure, drawn as written
        else -> m2txt(parea(m.pts))
    }

    /** The tag drawn at a tape's far end: "1234 mm" while it is pulled, "F-T1 1234 mm" once plotted. A code and a
     *  figure: byte-equal in every language (port 61c slice M3b, SurveyDrawMarathiTest). */
    fun tapeTag(code: String?, len: Double) = (if (code != null) "$code " else "") + mmTxt(len) // text:value a code and a figure

    /** A Line's label on the photo: "F-M1 1234 mm" ("1234 mm" while pulled); an area's "F-M2 1.86 m²" ("1.86 m²…"
     *  while its corners are tapped, from the third). Codes and figures: byte-equal in every language. */
    fun lineTag(code: String?, kind: String, pts: List<DoubleArray>): String = when (kind) {
        LINE -> (if (code != null) "$code " else "") + mmTxt(plen(pts)) // text:value a code and a figure
        AREA -> if (code != null) "$code ${m2txt(parea(pts))}" else if (pts.size > 2) "${m2txt(parea(pts))}…" else "" // text:value a code and a figure
        else -> ""
    }

    // ------------------------------------------------------------ picking on the photo

    /**
     * A TAPE BAND'S HALF-WIDTH IN FACE MM: half of [TAPE_W], or [TAPE_MIN_HALF_DP] dp when that is wider on screen --
     * the ONE rule the drawing and the picking both use. [ppm] canvas px per face mm, [dpPx] px per dp.
     */
    fun bandHalfMm(ppm: Double, dpPx: Double): Double =
        if (ppm > 0) max(TAPE_W / 2, TAPE_MIN_HALF_DP * dpPx / ppm) else TAPE_W / 2

    /** The same half-width on screen, px (0 without a scale): what a pick widens a tape's middle by. */
    fun bandHalfPx(ppm: Double, dpPx: Double): Double = if (ppm > 0) bandHalfMm(ppm, dpPx) * ppm else 0.0

    /** A tap at ([x], [y]) canvas px closes the area being built: it has [n] >= 3 corners and the tap is within
     *  [CLOSE_DP] dp of its first corner, [first] on screen (sp.js polyTap's tolMM). */
    fun nearFirst(first: DoubleArray, x: Double, y: Double, n: Int, dpPx: Double): Boolean =
        n >= 3 && hypot(first[0] - x, first[1] - y) < CLOSE_DP * dpPx

    /**
     * WHERE A TAPE'S TAG GOES (sp.js drawTapes), canvas px: just past the far end [pb] of the tape from [pa], [tw] the
     * text's width and [pw] the padlock's, kept on the [w] x [h] canvas; when there is no room past the end (the clamp
     * would move it more than 3 dp), beside the end on the side ABOVE the band (or right of a plumb one).
     * Returns the text's left and baseline.
     */
    fun tapeTagAt(pa: DoubleArray, pb: DoubleArray, tw: Double, pw: Double, dp: Double, w: Double, h: Double): DoubleArray {
        var dx = pb[0] - pa[0]; var dy = pb[1] - pa[1]; val l = hypot(dx, dy)
        if (!(l > 0)) { dx = 1.0; dy = 0.0 } else { dx /= l; dy /= l }
        val r = abs(dx) * (tw / 2 + pw / 2) + abs(dy) * 9 * dp + 12 * dp
        fun cl(x: Double, y: Double) = doubleArrayOf(
            x.coerceIn(2 * dp, max(2 * dp, w - tw - pw - 2 * dp)), y.coerceIn(14 * dp, max(14 * dp, h - 4 * dp)))
        var x = pb[0] + dx * r - (tw + pw) / 2; var y = pb[1] + dy * r + 4 * dp
        var k = cl(x, y)
        if (hypot(k[0] - x, k[1] - y) > 3 * dp) {
            var nx = -dy; var ny = dx
            if (ny > 0 || (abs(ny) < 1e-6 && nx < 0)) { nx = -nx; ny = -ny }
            val back = abs(dx) * (tw + pw) / 2 + abs(dy) * 9 * dp
            x = pb[0] - dx * back + nx * (abs(nx) * (tw + pw) / 2 + 14 * dp) - (tw + pw) / 2
            y = pb[1] - dy * back + ny * 14 * dp + 4 * dp
            k = cl(x, y)
        }
        return k
    }

    /** The distance from ([x], [y]) to the segment [p]-[q], all canvas px (sp.js segDist). */
    fun segDist(x: Double, y: Double, p: DoubleArray, q: DoubleArray): Double {
        val dx = q[0] - p[0]; val dy = q[1] - p[1]; val l2 = (dx * dx + dy * dy).takeIf { it > 0 } ?: 1.0
        val t = (((x - p[0]) * dx + (y - p[1]) * dy) / l2).coerceIn(0.0, 1.0)
        return hypot(p[0] + t * dx - x, p[1] + t * dy - y)
    }

    /**
     * WITH SELECT ON, A TAP ON A TAPE OR A LINE PICKS IT (sp.js mHit; hit-order step 10 of item 14, after the marks):
     * [shapes] the items' ids with their points on screen, in drawing order -- an area closed -- and the nearest within
     * [r] (12 dp) of its outline wins -- of a tape's BAND, [widen] giving the band's half-width on screen. Null: none.
     */
    fun pick(shapes: List<Pair<SurveyPrep.Meas, List<DoubleArray>>>, x: Double, y: Double, r: Double,
             widen: (SurveyPrep.Meas) -> Double = { 0.0 }): String? {
        var best: Pair<String, Double>? = null
        for ((m, pts) in shapes) {
            val n = if (m.kind == AREA) pts.size else pts.size - 1
            val wd = widen(m)
            for (i in 0 until n) {
                val d = (segDist(x, y, pts[i], pts[(i + 1) % pts.size]) - wd).coerceAtLeast(0.0)
                if (d < r && (best == null || d < best.second)) best = m.id to d
            }
        }
        return best?.first
    }
}
