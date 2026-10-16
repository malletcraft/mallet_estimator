package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * THE REFERENCE RECTANGLE ON SURVEY PREP (prototype v52, sp.js 400-447, 750-824, 1511-1520; v52 slice 12, item 13).
 * Pure, so the frame, the defaults, the words and the hit order are tested here; SurveyPrepScreen only dispatches and
 * SurveyPrepDraw only draws.
 *
 * Amit, 14 Oct 2026 06:45 IST: "does edges or reference serve same purpose? if so, keep only one. still i should be
 * able to feed any mark type measurements by a human knowing distinctly what is calculated and what is actually
 * measured at site." and "Ref is currently treated as Mark Type list. Its not like that. it should be controlled only
 * from lines screen. also it can be visible or invisible but can not be deleted."
 *
 * They square a face two different ways, so both are kept and one is picked per face in 1 · Lines' Size from row:
 * EDGES · LASER squares it from its four lines and the laser's room size (right when all four corners are in the
 * photo); REFERENCE ▭ squares it from a rectangle on that plane whose size is known -- a door 905 × 2053 -- (right when
 * a corner is hidden by furniture or cut off). The reference is NOT A MARK: it is never in 3 · List, the List tab or
 * the chips, it is picked only in 1 · Lines, and nothing deletes it -- every delete path answers [NO_DELETE]. Its eye,
 * "Show on faces", hides its outline on the faces; hidden, it still squares the face.
 *
 * STORED where the prototype keeps it, the surface's `ref` -- {on, q: [[x, y] × 4] view px (bottom-left, bottom-right,
 * top-right, top-left), w, h mm, shown, locked} -- in [SurveyPrep.Surface.extra], which SurveyPrepFile already keeps
 * and writes back as it came; absent means none, so an older file is unchanged. The map is REPLACED on every change,
 * never changed in place (an undo copy may share it), and the keys this build does not read are kept.
 *
 * THE RISK THIS FILE GUARDS (PLAN.md slice 12: "high, quietly"): a phone that placed marks in one frame and wrote or
 * uploaded them in another would misplace every mark with no error at all. So there is ONE [SurveyPrep.frameOf] and ONE
 * [SurveyPrep.sizeOf], and the screen, the saved pictures and the upload all square a face through them.
 */
object SurveyRef {

    /** The surface key it is stored under. */
    const val KEY = "ref"
    /** A reference squares its face only with both sides at least this, mm (sp.js refOn). */
    const val MIN_MM = 50.0
    /** Cyan (sp.js RC). */
    const val COLOUR = 0x22d3ee
    /** A box's figure as it is typed: digits only, at most five (sp.js refin maxlength 5). */
    const val MAX_DIGITS = 5
    /** The face size measured through a reference is believed between these, mm (sp.js frameRef). */
    const val FACE_MIN = 100.0
    const val FACE_MAX = 40000.0
    /** A corner is grabbed within 20 dp (sp.js hitRef); a line end within 18 dp, a line's body within 20 dp. */
    const val CORNER_DP = 20.0
    const val END_DP = 18.0
    const val BODY_DP = 20.0
    /** The measured face differing from the laser's by more than this, mm, is said in amber (sp.js reftxt). */
    const val OFF_MM = 20.0

    // ------------------------------------------------------------ the words (sp.js, exactly)

    /** Every delete path, with no undo entry (sp.js refNoDelete). */
    const val NO_DELETE = "The reference stays — hide it with the eye in 1 · Lines"
    /** The eye's screen-reader label (sp.js aria-label). */
    const val EYE_LABEL = "Show on faces"
    const val EDGES = "Edges · laser"
    const val REFERENCE = "Reference ▭"
    const val SIZE_FROM = "Size from"
    const val PLACE_HINT = "Drag the four cyan corners onto a rectangle on this surface, then type its width and height"

    /** What the eye says (sp.js setRefShown). */
    fun shownSaid(s: String, shown: Boolean) =
        if (shown) "Reference shown on 2 · Mark, 3 · List and 4 · 360"
        else "Reference hidden on the faces — it still squares the ${SurveyPrep.NAME.getValue(s).lowercase()}"

    /** What it is called: "Front wall reference" (the menu's head, a refusal). */
    fun name(s: String) = "${SurveyPrep.NAME.getValue(s)} reference"

    /** A locked reference's corner dragged (sp.js hitLines): "Front wall reference is locked — unlock first". */
    fun lockedSaid(s: String) = SurveyPrepEdit.refusal(name(s))

    /** The long-press menu's head: "Front wall reference", "🔒 Front wall reference". */
    fun menuHeader(s: String, locked: Boolean) = (if (locked) "🔒 " else "") + name(s)

    /** The menu on it: Lock (or Unlock) · Cancel -- no Delete, no Duplicate, no Lock all (ct52 X4). */
    fun menuItems(locked: Boolean): List<Pair<String, String>> =
        listOf(SurveyPrepEdit.LOCK to if (locked) "Unlock" else "Lock", SurveyPrepEdit.CANCEL to "Cancel")

    /** Its padlock's screen-reader label: "Lock the front wall reference". */
    fun lockLabel(s: String, locked: Boolean) = "${if (locked) "Unlock" else "Lock"} the ${name(s).lowercase()}"

    private fun fig(v: Double?): String = v?.let { if (it == Math.rint(it)) it.toLong().toString() else it.roundToInt().toString() } ?: "?"

    /** Its label on the photo: "Ref 905 × 2053", "Ref ? × ?" before its size is typed (sp.js drawRef). */
    fun label(r: Ref) = "Ref ${fig(r.w)} × ${fig(r.h)}"

    /** The face tab's subtitle while it squares that face, 9 sp in cyan: "Ref 905 × 2053"; null otherwise. */
    fun tabText(sf: SurveyPrep.Surface): String? = of(sf)?.takeIf { on(it) }?.let { label(it) }

    // ------------------------------------------------------------ the record

    /** A face's reference as stored: [q] its corners in the face's view px (null until placed), [w] × [h] its size,
     *  mm (null until typed), [shown] its eye (absent is on), [locked] its padlock; [raw] the map as it came. */
    class Ref(val on: Boolean, val q: List<DoubleArray>?, val w: Double?, val h: Double?, val shown: Boolean,
              val locked: Boolean, val raw: Map<String, Any?>)

    private fun num(v: Any?): Double? = when (v) {
        is Number -> v.toDouble().takeIf { it.isFinite() }
        is String -> v.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        else -> null
    }

    private fun flag(v: Any?): Boolean = v == true || (v is String && v.equals("true", ignoreCase = true))

    private fun quad(v: Any?): List<DoubleArray>? {
        val l = v as? List<*> ?: return null
        val out = l.map { p -> (p as? List<*>)?.takeIf { it.size == 2 }?.let { xy ->
            val x = num(xy[0]); val y = num(xy[1]); if (x != null && y != null) doubleArrayOf(x, y) else null } }
        return if (out.size == 4 && out.all { it != null }) out.map { it!! } else null
    }

    /** A surface's reference; null when it has none (or what is there is no object). */
    fun of(sf: SurveyPrep.Surface): Ref? {
        @Suppress("UNCHECKED_CAST")
        val m = (sf.extra[KEY] as? Map<*, *>)?.let { mm -> LinkedHashMap<String, Any?>().apply { for ((k, v) in mm) if (k is String) put(k, v) } }
            ?: return null
        return Ref(flag(m["on"]), quad(m["q"]), num(m["w"]), num(m["h"]), m["shown"] != false && m["shown"] != "false",
            flag(m["locked"]), m)
    }

    /** It squares its face: on, four corners, and both sides at least [MIN_MM] (sp.js refOn). */
    fun on(r: Ref?): Boolean = r != null && r.on && r.q != null && r.q.size == 4 &&
        (r.w ?: 0.0) >= MIN_MM && (r.h ?: 0.0) >= MIN_MM

    fun on(sf: SurveyPrep.Surface): Boolean = on(of(sf))

    /** Its corners are drawn and picked in 1 · Lines: on, and placed (sp.js drawRef / hitRef). */
    fun placed(r: Ref?): Boolean = r != null && r.on && r.q != null

    /** [sf]'s reference with [k] set to [v] -- a NEW map, the other keys kept as they came, in their order. A surface with
     *  none gets the prototype's {on: false, q: null, w: null, h: null} first (sp.js setRefMode). */
    private fun put(sf: SurveyPrep.Surface, vararg kv: Pair<String, Any?>) {
        val m = LinkedHashMap<String, Any?>()
        val old = sf.extra[KEY] as? Map<*, *>
        if (old == null) { m["on"] = false; m["q"] = null; m["w"] = null; m["h"] = null }
        else for ((k, v) in old) if (k is String) m[k] = v
        for ((k, v) in kv) m[k] = v
        sf.extra = SurveyPrep.withKey(sf.extra, KEY, m)
    }

    private fun qList(q: List<DoubleArray>) = q.map { listOf(it[0], it[1]) }

    // ------------------------------------------------------------ the frame

    private fun mul3(a: DoubleArray, b: DoubleArray): DoubleArray = DoubleArray(9) { k ->
        val i = k / 3; val j = k % 3
        a[i * 3] * b[j] + a[i * 3 + 1] * b[3 + j] + a[i * 3 + 2] * b[6 + j]
    }

    /**
     * THE FACE SQUARED FROM ITS REFERENCE (sp.js frameRef): Hr takes the reference's corners onto its w × h; the four
     * crossings of the face's lines go through Hr, then a rigid T turns them so the face's bottom-left is (0, 0) and its
     * bottom edge runs along x. A and B are the means of the opposite edges -- the face's size MEASURED through the
     * reference -- and m2p is the homography from those mm crossings back to the view, so no inverse is needed. Null
     * when the reference does not square ([on]), a crossing is missing, or the size is past [FACE_MIN]..[FACE_MAX].
     */
    fun frameFromReference(lines: Map<String, Array<DoubleArray>>, r: Ref?): SurveyPrep.Frame? {
        if (!on(r)) return null
        val ref = r!!; val w = ref.w!!; val h = ref.h!!
        val q0 = listOf(SurveyPrep.cross(lines.getValue("bottom"), lines.getValue("left")),
            SurveyPrep.cross(lines.getValue("bottom"), lines.getValue("right")),
            SurveyPrep.cross(lines.getValue("top"), lines.getValue("right")),
            SurveyPrep.cross(lines.getValue("top"), lines.getValue("left")))
        if (q0.any { it == null }) return null
        val q = q0.map { it!! }
        val hr = SurveyPrep.homography(ref.q!!, listOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(w, 0.0), doubleArrayOf(w, h), doubleArrayOf(0.0, h)))
            ?: return null
        val m = q.map { SurveyPrep.apply(hr, it) }
        if (!m.all { it[0].isFinite() && it[1].isFinite() }) return null
        val dx = m[1][0] - m[0][0]; val dy = m[1][1] - m[0][1]
        val len = hypot(dx, dy).takeIf { it > 0 } ?: 1.0
        val u = doubleArrayOf(dx / len, dy / len); val v = doubleArrayOf(-u[1], u[0])
        val t = doubleArrayOf(u[0], u[1], -(u[0] * m[0][0] + u[1] * m[0][1]),
            v[0], v[1], -(v[0] * m[0][0] + v[1] * m[0][1]), 0.0, 0.0, 1.0)
        val p2m = mul3(t, hr)
        val mm = q.map { SurveyPrep.apply(p2m, it) }
        fun d(a: DoubleArray, b: DoubleArray) = hypot(a[0] - b[0], a[1] - b[1])
        val a = (d(mm[0], mm[1]) + d(mm[3], mm[2])) / 2; val b = (d(mm[0], mm[3]) + d(mm[1], mm[2])) / 2
        if (!(a > FACE_MIN && b > FACE_MIN && a < FACE_MAX && b < FACE_MAX)) return null
        val m2p = SurveyPrep.homography(mm, q) ?: return null
        return SurveyPrep.Frame(q, a, b, p2m, m2p, key(ref), mm)
    }

    /** What makes a reference frame different from another with the same crossings and size: its w × h and corners. */
    fun key(r: Ref): String = "${r.w}x${r.h}:" + r.q!!.joinToString(";") { "%.3f,%.3f".format(it[0], it[1]) }

    /**
     * WHERE A NEW REFERENCE IS PLACED (sp.js setRefMode): with the face's EDGES · LASER frame, the rectangle 35-65 % across
     * and 30-70 % up the face in mm, through that frame into the view; without one, 38-62 % of the view each way.
     * Bottom-left, bottom-right, top-right, top-left.
     */
    fun refDefaultQuad(s: String, laserFrame: SurveyPrep.Frame?): List<DoubleArray> {
        if (laserFrame != null) return listOf(.35 to .3, .65 to .3, .65 to .7, .35 to .7).map { (fx, fy) ->
            SurveyPrep.apply(laserFrame.m2p, doubleArrayOf(laserFrame.a * fx, laserFrame.b * fy)) }
        val d = SurveyPrep.dimOf(s); val w = d[0].toDouble(); val h = d[1].toDouble()
        return listOf(doubleArrayOf(w * .38, h * .62), doubleArrayOf(w * .62, h * .62), doubleArrayOf(w * .62, h * .38), doubleArrayOf(w * .38, h * .38))
    }

    // ------------------------------------------------------------ changes (each the caller's one undo step)

    /**
     * THE SIZE FROM ROW (sp.js setRefMode): Reference ▭ ([on] true) the first time places [refDefaultQuad]; Edges · laser
     * keeps the corners, the size and the eye stored, so Reference ▭ again brings them back as they were. False: nothing
     * to change (already so, or Edges · laser on a face with no reference) -- the caller takes no undo step.
     */
    fun setMode(sf: SurveyPrep.Surface, s: String, on: Boolean, laserFrame: SurveyPrep.Frame?): Boolean {
        val r = of(sf)
        if (r == null && !on) return false
        if (r != null && r.on == on && (!on || r.q != null)) return false
        if (on && r?.q == null) put(sf, "q" to qList(refDefaultQuad(s, laserFrame)), "on" to true)
        else put(sf, "on" to on)
        return true
    }

    /** A W or H box set (sp.js setRefDim): [k] "w" or "h"; a figure over 0 is kept rounded to the mm, else none (null).
     *  False: no reference, or nothing changes -- no undo step. NEVER a reading: prep.actual is not touched. */
    fun setDim(sf: SurveyPrep.Surface, k: String, v: Double?): Boolean {
        val r = of(sf) ?: return false
        if (k != "w" && k != "h") return false
        val n: Double? = v?.takeIf { it > 0 && it.isFinite() }?.let { Math.round(it).toDouble() }
        val cur = if (k == "w") r.w else r.h
        if (cur == n) return false
        put(sf, k to n)
        return true
    }

    /** What a box's text becomes as it is typed: digits only, at most [MAX_DIGITS]. */
    fun boxText(t: String): String = t.filter { it in '0'..'9' }.take(MAX_DIGITS)

    /** The figure a box's [text] commits, against what is stored ([cur]): null when nothing changes (sp.js fires `change`
     *  only on a change); else a holder with the new figure, null inside when the box was emptied. */
    class Typed(val value: Double?)

    fun typed(cur: Double?, text: String): Typed? {
        val n = text.toIntOrNull()?.takeIf { it > 0 }?.toDouble()
        val c = cur?.let { Math.round(it).toDouble() }
        return if (n == c) null else Typed(n)
    }

    /** The eye (sp.js setRefShown): [on] null toggles. The new state; null when there is no reference (no undo step). */
    fun setShown(sf: SurveyPrep.Surface, on: Boolean?): Boolean? {
        val r = of(sf) ?: return null
        val v = on ?: !r.shown
        if (v != r.shown) put(sf, "shown" to v)
        return v
    }

    /** Its padlock (sp.js setLock 'ref:'): [on] null toggles. The new state; null when there is none. */
    fun setLocked(sf: SurveyPrep.Surface, on: Boolean?): Boolean? {
        val r = of(sf) ?: return null
        val v = on ?: !r.locked
        if (v != r.locked) put(sf, "locked" to v)
        return v
    }

    /** Corner [i] dragged to ([x], [y]) view px (sp.js hitRef's move); false when locked or not placed. */
    fun moveCorner(sf: SurveyPrep.Surface, i: Int, x: Double, y: Double): Boolean {
        val r = of(sf) ?: return false
        val q = r.q ?: return false
        if (r.locked || i !in 0..3) return false
        put(sf, "q" to qList(q.mapIndexed { j, p -> if (j == i) doubleArrayOf(x, y) else p }))
        return true
    }

    // ------------------------------------------------------------ its id: picked, never an item

    /** The id the selection and the long-press menu hold it by (sp.js refId): "ref:front". */
    fun id(s: String) = "$KEY:$s"
    fun isId(id: String?) = id != null && id.startsWith("$KEY:") && id.removePrefix("$KEY:") in SurveyPrep.SURFACES
    fun surfaceOf(id: String): String? = id.takeIf { isId(it) }?.removePrefix("$KEY:")

    /** The reference [id] names, while it is on and placed (sp.js itemById: a reference turned off is no item). */
    fun byId(p: SurveyPrep.Prep, id: String?): Pair<String, Ref>? {
        val s = id?.let { surfaceOf(it) } ?: return null
        val r = p.surfaces[s]?.let { of(it) } ?: return null
        return if (placed(r)) s to r else null
    }

    // ------------------------------------------------------------ on the faces

    /** Its corners in the face's mm through [f] -- what 2 · Mark, 3 · List and the saved pictures draw (sp.js drawRefSq):
     *  only while it is on, placed and SHOWN; null otherwise, or without a frame. */
    fun faceQuad(sf: SurveyPrep.Surface, f: SurveyPrep.Frame?): List<DoubleArray>? {
        val r = of(sf) ?: return null
        if (!placed(r) || !r.shown || f == null) return null
        return r.q!!.map { SurveyPrep.apply(f.p2m, it) }
    }

    /**
     * The line under the W × H boxes (sp.js reftxt): with the face squared from the reference, "Measured W 2678 × H 2677
     * · laser 2678 × 2677" and " ✓", or " · differs by +12 / −30 mm" in amber past [OFF_MM]; else the placing hint.
     * [laser] the face's laser size. Second: amber.
     */
    fun refText(s: String, f: SurveyPrep.Frame?, laser: DoubleArray): Pair<String, Boolean> {
        if (f == null || f.ref == null) return PLACE_HINT to false
        val t = SurveyPrep.sizeTag(s)
        val dA = f.a - laser[0]; val dB = f.b - laser[1]
        val off = abs(dA) > OFF_MM || abs(dB) > OFF_MM
        fun sgn(d: Double): String { val r = d.roundToInt(); return (if (r > 0) "+" else if (r < 0) "−" else "±") + abs(r) }
        return ("Measured ${t.first} ${f.a.roundToInt()} × ${t.second} ${f.b.roundToInt()} · laser ${laser[0].roundToInt()} × ${laser[1].roundToInt()}" +
            (if (off) " · differs by ${sgn(dA)} / ${sgn(dB)} mm" else " ✓")) to off
    }

    /** [refText] for face [s] of [prep] at the laser size [l] x [w] x [h] (what 1 · Lines shows under the boxes). */
    fun refText(s: String, prep: SurveyPrep.Prep, l: Double, w: Double, h: Double): Pair<String, Boolean> {
        val sf = prep.surfaces[s] ?: return PLACE_HINT to false
        return refText(s, frameFromReference(sf.lines, of(sf)), SurveyPrep.size(s, l, w, h))
    }

    // ------------------------------------------------------------ 1 · Lines: what a finger takes

    /** Point in a polygon (sp.js inQuad), even-odd. */
    fun inside(x: Double, y: Double, q: List<DoubleArray>): Boolean {
        var c = false; var j = q.size - 1
        for (i in q.indices) {
            val xi = q[i][0]; val yi = q[i][1]; val xj = q[j][0]; val yj = q[j][1]
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) c = !c
            j = i
        }
        return c
    }

    /** What a finger down on 1 · Lines takes. */
    sealed class LinesHit {
        /** Corner [i] of the (unlocked) reference: it moves. */
        class Corner(val i: Int) : LinesHit()
        /** A corner of the LOCKED reference: it is picked, nothing moves, and a drag says [lockedSaid]. */
        object RefLocked : LinesHit()
        /** End [j] of line [slot] (its grip). */
        class End(val slot: String, val j: Int) : LinesHit()
        /** Line [slot]'s body. */
        class Body(val slot: String) : LinesHit()
        /** The lines are locked and the finger is on one: said, nothing picked (a drag pans). */
        object LinesLocked : LinesHit()
        /** Inside the reference: it is picked, nothing grabbed (a drag pans). */
        object Inside : LinesHit()
    }

    /**
     * THE HIT ORDER ON 1 · LINES (sp.js hitLines), canvas px: the reference's corner within [CORNER_DP] (the nearest);
     * a LOCKED reference's corner; a line's end within [END_DP] (the nearest); with the lines locked, a finger on an end
     * or within [BODY_DP] of a line is refused -- else inside the reference picks it; a line's body within [BODY_DP]
     * (the nearest); inside the reference. [corners]: the reference's corners on the canvas while it is on and placed,
     * else null; [ends] every line's two ends on the canvas; [bodyDist] a line's distance from the finger, canvas px;
     * [insideRef] the finger is inside the reference; [dp] px per dp. Null: nothing -- the press pans.
     */
    fun linesHit(corners: List<DoubleArray>?, refLocked: Boolean, ends: Map<String, List<DoubleArray>>,
                 bodyDist: (String) -> Double, insideRef: Boolean, linesLocked: Boolean,
                 x: Double, y: Double, dp: Double): LinesHit? {
        if (corners != null) {
            val near = corners.indices.map { it to hypot(corners[it][0] - x, corners[it][1] - y) }
                .filter { it.second < CORNER_DP * dp }.minByOrNull { it.second }
            if (near != null) return if (refLocked) LinesHit.RefLocked else LinesHit.Corner(near.first)
        }
        var best: Triple<String, Int, Double>? = null
        for (slot in SurveyPrep.SLOTS) ends[slot]?.forEachIndexed { j, p ->
            val d = hypot(p[0] - x, p[1] - y)
            if (d < END_DP * dp && (best == null || d < best!!.third)) best = Triple(slot, j, d)
        }
        val inRef = corners != null && insideRef
        if (linesLocked) {
            val onLine = best != null || SurveyPrep.SLOTS.any { bodyDist(it) < BODY_DP * dp }
            return if (onLine) LinesHit.LinesLocked else if (inRef) LinesHit.Inside else null
        }
        best?.let { return LinesHit.End(it.first, it.second) }
        val body = SurveyPrep.SLOTS.map { it to bodyDist(it) }.filter { it.second < BODY_DP * dp }.minByOrNull { it.second }
        if (body != null) return LinesHit.Body(body.first)
        return if (inRef) LinesHit.Inside else null
    }
}
