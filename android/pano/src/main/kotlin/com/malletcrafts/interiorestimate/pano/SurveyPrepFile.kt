package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Msg
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * THE SURVEY PREP FILE, read tolerantly and written back whole (v52 slice 2).
 * filesDir/surveyprep/<captureId>.json on the phone; SurveyPrepStore does the
 * disk and the org.json, this does everything else, as plain maps and lists,
 * so it is tested here with no device.
 *
 * Why it exists: the file used to be read all-or-nothing. A prep missing one
 * surface, or carrying one malformed mark, read as NO prep, and the screen
 * then saved a fresh one over it -- a whole room's lines, marks and readings
 * gone because of one bad value. And the fields later builds add (a mark's
 * `locked`, a surface's `meas`, `chains`, `ref`, `lock`) would have been
 * dropped by the first save from a build that did not know them. Now:
 *
 *  - a key this build does not read is KEPT, at every level (the file, `w`,
 *    a surface, a mark), and written back exactly where it was;
 *  - one bad surface line or mark costs that line (its default from
 *    [SurveyPrep.fresh]) or that mark, and is reported in [Decoded.problems]
 *    -- the file is then copied aside before anything is saved, and when the
 *    copy fails nothing is saved at all ([open]);
 *  - a file that is not a prep at all decodes to null and is set aside
 *    whole, never overwritten.
 *
 * Shape (v = 1, unchanged: the change is additive): {v, L, W, H, w: {front:
 * {bottom, top, left, right: [[x, y], [x, y]], set: {bottom: true, ...},
 * els: [{id, t, code, x0, x1, y0, y1}], meas: [{id, code, kind, locked, pts:
 * [[x, y], ...]}], chains: [{id, code, axis, locked, nodes: [[x, y], ...], snaps:
 * [tag or null, ...]}]}, ...}, actual: {"FW1.w": 1200, "F-M1.len": 1000, "FK1.1": 905,
 * "FK1.t": 3310}}. `meas` (v52 slice 9: Tapes, Lines and areas) and `chains`
 * (v52 slice 11) are written only where there is one, or where the file had
 * the key, so an older prep re-encodes unchanged.
 */
object SurveyPrepFile {

    const val VERSION = 1

    /** The keys this build reads, at each level; every other key is kept as it came. */
    val PREP_KEYS = setOf("v", "L", "W", "H", "w", "actual")
    val SURFACE_KEYS = SurveyPrep.SLOTS.toSet() + setOf("set", "els", "meas", "chains")
    val MARK_KEYS = setOf("id", "t", "code", "x0", "x1", "y0", "y1")
    /** A Tape's, a Line's or an area's keys this build reads (v52 slice 9); its `locked` and anything else are kept as they came. */
    val MEAS_KEYS = setOf("id", "code", "kind", "pts")
    /** A chain's keys this build reads (v52 slice 11); its `locked` and anything else are kept as they came. */
    val CHAIN_KEYS = setOf("id", "code", "axis", "nodes", "snaps")

    /** A prep as read: the prep, the size it was squared to (null when the file has no valid one), and what had to be
     *  dropped -- each a [Msg], drawn in the language on when the note is drawn (port 61c slice M3b; the stored file,
     *  its aside names and its keys stay byte-identical).
     *  [points] / [pointLog]: the older points [open] made 50 mm spots, and what that did to them and their readings
     *  (SurveyPrep.normPoints, v52 slice 4). */
    class Decoded(val prep: SurveyPrep.Prep, val size: DoubleArray?, val problems: List<Msg>,
                  val points: Int = 0, val pointLog: List<String> = emptyList(), val converted: Boolean = points > 0)

    private fun num(v: Any?): Double? = when (v) {
        is Number -> v.toDouble().takeIf { it.isFinite() }
        is String -> v.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        else -> null
    }

    private fun str(v: Any?): String? = when (v) {
        is String -> v
        is Number, is Boolean -> v.toString()
        else -> null
    }

    private fun point(v: Any?): DoubleArray? {
        val a = v as? List<*> ?: return null
        if (a.size != 2) return null
        val x = num(a[0]) ?: return null; val y = num(a[1]) ?: return null
        return doubleArrayOf(x, y)
    }

    private fun line(v: Any?): Array<DoubleArray>? {
        val a = v as? List<*> ?: return null
        if (a.size != 2) return null
        val p = point(a[0]) ?: return null; val q = point(a[1]) ?: return null
        return arrayOf(p, q)
    }

    /** The laser L, W, H a prep was squared to, mm; null when any is missing or out of range. */
    fun sizeOf(o: Map<String, Any?>): DoubleArray? {
        val v = listOf("L", "W", "H").map { num(o[it]) }
        return if (v.all { SurveyPrep.valid(it) }) DoubleArray(3) { v[it]!! } else null
    }

    /**
     * A prep file's content (its JSON as maps, lists, numbers, strings,
     * booleans and nulls), read as far as it can be. Null only when it is not
     * a prep at all: no `w` object. Everything else is salvaged, and each
     * thing that could not be is named in [Decoded.problems].
     */
    fun decode(o: Map<String, Any?>): Decoded? {
        val wo = o["w"] as? Map<*, *> ?: return null
        val problems = mutableListOf<Msg>()
        // the surface's name in the note, in the language on when it is DRAWN (Masters.surface; NAME is the identity)
        fun surf(tx: Tx, s: String) = Masters.surface(tx, SurveyPrep.NAME.getValue(s))
        val size = sizeOf(o)
        // A missing surface or line takes its default from the room as the file says it is
        // (SurveyPrep.fresh copes with a missing size on its own).
        val fresh by lazy {
            val z = size ?: DoubleArray(3) { Double.NaN }
            SurveyPrep.fresh(z[0], z[1], z[2])
        }
        val out = LinkedHashMap<String, SurveyPrep.Surface>()
        for (s in SurveyPrep.SURFACES) {
            val so = wo[s] as? Map<*, *>
            if (so == null) {
                val present = wo.containsKey(s)
                problems.add { tx -> if (present) tx.sp_file_x_unreadable_started(surf(tx, s)) else tx.sp_file_x_missing_started(surf(tx, s)) }
                out[s] = fresh.surface(s).copy()
                continue
            }
            val lines = mutableMapOf<String, Array<DoubleArray>>()
            val set = mutableMapOf<String, Boolean>()
            val setO = so["set"] as? Map<*, *>
            for (k in SurveyPrep.SLOTS) {
                val l = line(so[k])
                if (l == null) {
                    problems.add { tx -> tx.sp_file_x_line_unreadable(surface = surf(tx, s), line = SurveyPrep.lineName(tx, s, k)) }
                    lines[k] = fresh.surface(s).lines.getValue(k).let { f -> Array(2) { f[it].copyOf() } }
                    set[k] = false
                } else {
                    lines[k] = l
                    val v = setO?.get(k)
                    set[k] = v == true || (v is String && v.equals("true", ignoreCase = true))
                }
            }
            val marks = mutableListOf<SurveyPrep.Mark>()
            val els = so["els"]
            if (els != null && els !is List<*>) problems.add { tx -> tx.sp_file_x_marks_unreadable(surf(tx, s)) }
            (els as? List<*>)?.forEachIndexed { i, mv ->
                val m = mv as? Map<*, *>
                val id = str(m?.get("id")); val t = str(m?.get("t")); val code = str(m?.get("code"))
                val c = listOf("x0", "x1", "y0", "y1").map { num(m?.get(it)) }
                if (m == null || id == null || t == null || code == null || c.any { it == null }) {
                    val named = code ?: "#" + (i + 1) // text:value a code, or its place in the list
                    problems.add { tx -> tx.sp_file_x_mark_x_left_out(surface = surf(tx, s), code = named) }
                    return@forEachIndexed
                }
                marks.add(SurveyPrep.Mark(id, t, code, c[0]!!, c[1]!!, c[2]!!, c[3]!!, unknown(m, MARK_KEYS)))
            }
            // THE TAPES, LINES AND AREAS (v52 slice 9): {id, code, kind, locked, pts}. One that cannot be read costs
            // itself; a kind a later build adds is kept and written back as it came (it is drawn by nothing here)
            val meas = mutableListOf<SurveyPrep.Meas>()
            val mo = so["meas"]
            if (mo != null && mo !is List<*>) problems.add { tx -> tx.sp_file_x_measurements_unreadable(surf(tx, s)) }
            (mo as? List<*>)?.forEachIndexed { i, mv ->
                val m = mv as? Map<*, *>
                val id = str(m?.get("id")); val code = str(m?.get("code")); val kind = str(m?.get("kind"))
                val pl = m?.get("pts") as? List<*>
                val pts = pl?.map { point(it) }
                val need = when (kind) { SurveyMeas.TAPE, SurveyMeas.LINE -> 2; SurveyMeas.AREA -> 3; else -> 1 }
                val ok = m != null && id != null && code != null && kind != null && pts != null && pts.all { it != null } &&
                    (if (kind == SurveyMeas.TAPE || kind == SurveyMeas.LINE) pts.size == 2 else pts.size >= need)
                if (!ok) {
                    val named = code ?: "#" + (i + 1) // text:value a code, or its place in the list
                    problems.add { tx -> tx.sp_file_x_measurement_x_left_out(surface = surf(tx, s), code = named) }
                    return@forEachIndexed
                }
                meas.add(SurveyPrep.Meas(id!!, kind!!, code!!, pts!!.map { it!! }.toMutableList(), unknown(m!!, MEAS_KEYS)))
            }
            // THE CHAINS (v52 slice 11): {id, code, axis, locked, nodes, snaps}. Two points or more; an axis that is not
            // "y" is "x" (sp.js: `axis:D.axis||'x'`); snaps padded with nulls to the points. One that cannot be read costs itself
            val chains = mutableListOf<SurveyPrep.Chain>()
            val co = so["chains"]
            if (co != null && co !is List<*>) problems.add { tx -> tx.sp_file_x_chains_unreadable(surf(tx, s)) }
            (co as? List<*>)?.forEachIndexed { i, cv ->
                val c = cv as? Map<*, *>
                val id = str(c?.get("id")); val code = str(c?.get("code"))
                val nodes = (c?.get("nodes") as? List<*>)?.map { point(it) }
                if (c == null || id == null || code == null || nodes == null || nodes.size < 2 || nodes.any { it == null }) {
                    val named = code ?: "#" + (i + 1) // text:value a code, or its place in the list
                    problems.add { tx -> tx.sp_file_x_chain_x_left_out(surface = surf(tx, s), code = named) }
                    return@forEachIndexed
                }
                val sl = c["snaps"] as? List<*>
                val snaps = MutableList(nodes.size) { k -> sl?.getOrNull(k) as? String }
                chains.add(SurveyPrep.Chain(id, code, if (str(c["axis"]) == "y") "y" else "x", nodes.map { it!! }.toMutableList(),
                    snaps, unknown(c, CHAIN_KEYS)))
            }
            out[s] = SurveyPrep.Surface(lines, set, marks, unknown(so, SURFACE_KEYS), meas, so.containsKey("meas"),
                chains, so.containsKey("chains"))
        }
        val actual = LinkedHashMap<String, Double>()
        val ao = o["actual"]
        if (ao != null && ao !is Map<*, *>) problems.add { tx -> tx.sp_file_readings_unreadable }
        (ao as? Map<*, *>)?.forEach { (k, v) ->
            val key = k as? String ?: return@forEach
            val d = num(v)
            if (d == null) problems.add { tx -> tx.sp_file_reading_x_left_out(key) } else actual[key] = d
        }
        val extraSurfaces = LinkedHashMap<String, Any?>()
        for ((k, v) in wo) if (k is String && k !in SurveyPrep.SURFACES) extraSurfaces[k] = v
        return Decoded(SurveyPrep.Prep(out, actual, unknown(o, PREP_KEYS), extraSurfaces), size, problems)
    }

    /** The keys of [m] not in [known], in the order they came. */
    fun unknown(m: Map<*, *>, known: Set<String>): Map<String, Any?> {
        val out = LinkedHashMap<String, Any?>()
        for ((k, v) in m) if (k is String && k !in known) out[k] = v
        return out
    }

    /**
     * A prep as its file, in the order the file has always been written
     * (v, L, W, H, w, actual), with every kept key back at its own level
     * after the known ones. Numbers are Doubles, except `v`.
     */
    fun encode(p: SurveyPrep.Prep, l: Double, w: Double, h: Double): LinkedHashMap<String, Any?> {
        val o = LinkedHashMap<String, Any?>()
        o["v"] = VERSION; o["L"] = l; o["W"] = w; o["H"] = h
        val wo = LinkedHashMap<String, Any?>()
        for (s in SurveyPrep.SURFACES) {
            val sf = p.surfaces[s] ?: continue
            val so = LinkedHashMap<String, Any?>()
            for (k in SurveyPrep.SLOTS) sf.lines[k]?.let { ln -> so[k] = listOf(listOf(ln[0][0], ln[0][1]), listOf(ln[1][0], ln[1][1])) }
            so["set"] = LinkedHashMap<String, Any?>().apply { for (k in SurveyPrep.SLOTS) put(k, sf.set[k] == true) }
            so["els"] = sf.marks.map { m ->
                LinkedHashMap<String, Any?>().apply {
                    put("id", m.id); put("t", m.t); put("code", m.code)
                    put("x0", m.x0); put("x1", m.x1); put("y0", m.y0); put("y1", m.y1)
                    for ((k, v) in m.extra) if (k !in MARK_KEYS) put(k, v)
                }
            }
            if (sf.meas.isNotEmpty() || sf.measKey) so["meas"] = sf.meas.map { m ->
                LinkedHashMap<String, Any?>().apply {
                    put("id", m.id); put("code", m.code); put("kind", m.kind)
                    for ((k, v) in m.extra) if (k !in MEAS_KEYS) put(k, v)
                    put("pts", m.pts.map { listOf(it[0], it[1]) })
                }
            }
            // the chains, in the prototype's own key order: id, code, axis, then what it keeps (locked), nodes, snaps
            if (sf.chains.isNotEmpty() || sf.chainsKey) so["chains"] = sf.chains.map { c ->
                LinkedHashMap<String, Any?>().apply {
                    put("id", c.id); put("code", c.code); put("axis", c.axis)
                    for ((k, v) in c.extra) if (k !in CHAIN_KEYS) put(k, v)
                    put("nodes", c.nodes.map { listOf(it[0], it[1]) })
                    put("snaps", c.nodes.indices.map { c.snaps.getOrNull(it) })
                }
            }
            for ((k, v) in sf.extra) if (k !in SURFACE_KEYS) so[k] = v
            wo[s] = so
        }
        for ((k, v) in p.extraSurfaces) if (k !in SurveyPrep.SURFACES) wo[k] = v
        o["w"] = wo
        o["actual"] = LinkedHashMap<String, Any?>(p.actual)
        for ((k, v) in p.extra) if (k !in PREP_KEYS) o[k] = v
        return o
    }

    // ------------------------------------------------------------ opening one

    /** What opening a capture's prep found. */
    sealed class Opened {
        /** No file: a fresh prep is started and saved. */
        object Missing : Opened()
        /** Read whole. */
        class Restored(val decoded: Decoded) : Opened()
        /** Read in part; the file as it was is kept as [aside] (null when even that copy failed). */
        class Salvaged(val decoded: Decoded, val aside: String?) : Opened()
        /** Not a prep at all: moved aside as [aside] (null when it could not be, and then it is never overwritten). */
        class Unreadable(val aside: String?) : Opened()
    }

    /** What opening decided: what the screen is told, and whether every save over the file must be refused. */
    class Opening(val opened: Opened, val hold: Boolean)

    /**
     * THE OPENING DECISION, with the disk passed in: [text] is the file's text
     * (null when there is none or it cannot be read), [exists] whether the file
     * is there at all, [tree] its JSON as plain maps (null or a throw when it is
     * no JSON object), [copyAside] / [moveAside] the store's two ways of keeping
     * the original, each answering the name it was kept as, or null (or a throw)
     * when it could not be.
     *
     * The rule it holds: NOTHING IS EVER SAVED OVER THE ONLY COPY OF A FILE
     * THAT WAS NOT READ WHOLE. Read in part -> copied aside first; not a prep
     * at all -> moved aside; and when the copy or the move fails, [Opening.hold]
     * is true and the store refuses every save for that capture.
     */
    fun open(text: String?, exists: Boolean, tree: (String) -> Map<String, Any?>?,
             copyAside: () -> String?, moveAside: () -> String?): Opening {
        fun unreadable(): Opening {
            val to = runCatching(moveAside).getOrNull()
            return Opening(Opened.Unreadable(to), hold = to == null)
        }
        if (text == null) return if (exists) unreadable() else Opening(Opened.Missing, hold = false)
        val d0 = runCatching { tree(text)?.let { decode(it) } }.getOrNull() ?: return unreadable()
        // OLDER POINTS ARE MADE SPOTS ON OPENING (v52 slice 4), their readings converted with them; once
        // saved, the next open finds nothing to do (normPoints is idempotent)
        val fix = SurveyPrep.normPoints(d0.prep)
        val d = if (!fix.changed) d0 else Decoded(fix.prep, d0.size, d0.problems, fix.points, fix.log, true)
        if (d.problems.isEmpty()) return Opening(Opened.Restored(d), hold = false)
        val to = runCatching(copyAside).getOrNull()
        return Opening(Opened.Salvaged(d, to), hold = to == null)
    }

    /**
     * Whether the screen writes the prep the moment it opens: a new one (so the
     * laser sizes it squares to are kept); a whole one whose older points were
     * just made spots (so the conversion is kept, done once, and its readings
     * reach the readings sync as the edits they are); and a salvaged one whose original is
     * SAFELY ASIDE -- so the live file is clean, the next open is Restored, and
     * the same damage is not copied aside again on every visit. Never a salvaged
     * prep whose copy failed: that file is the only copy of what was lost.
     */
    fun saveOnOpen(o: Opened): Boolean = o is Opened.Missing || (o is Opened.Salvaged && o.aside != null) ||
        (o is Opened.Restored && o.decoded.converted)

    /** The points [open] re-centred, for the log and the note; 0 when none (or nothing was read). */
    fun pointsOf(o: Opened): Int = when (o) {
        is Opened.Restored -> o.decoded.points
        is Opened.Salvaged -> o.decoded.points
        is Opened.Missing, is Opened.Unreadable -> 0
    }

    /** Whether [open] changed anything about older points (a box re-centred, or a reading converted). */
    fun convertedOf(o: Opened): Boolean = when (o) {
        is Opened.Restored -> o.decoded.converted
        is Opened.Salvaged -> o.decoded.converted
        is Opened.Missing, is Opened.Unreadable -> false
    }

    /** The one-time note when opening re-centred older points; null when it did not. */
    fun pointsNote(o: Opened): Msg? = pointsOf(o).takeIf { it > 0 }?.let { n -> { tx: Tx -> tx.sp_file_x_points_recentred(n) } }

    // ------------------------------------------------------- the JSON library

    /**
     * How a JSON library's tree is read and built, so the conversion between it
     * and the plain maps above is tested here (org.json is Android's, and this
     * module has no device): [isNull] its null, [entries] an object's keys and
     * values in order (null when [v] is no object), [items] an array's values
     * (null when no array), [newObject] / [newArray] build one, [nul] its null.
     */
    class JsonShape(
        val isNull: (Any?) -> Boolean,
        val entries: (Any) -> List<Pair<String, Any?>>?,
        val items: (Any) -> List<Any?>?,
        val newObject: (List<Pair<String, Any>>) -> Any,
        val newArray: (List<Any>) -> Any,
        val nul: Any,
    )

    /** A library value as plain maps (in key order), lists, scalars and null: JSON null is null, at every depth. */
    fun plain(v: Any?, j: JsonShape): Any? {
        if (v == null || j.isNull(v)) return null
        j.entries(v)?.let { kv -> return LinkedHashMap<String, Any?>().apply { for ((k, x) in kv) put(k, plain(x, j)) } }
        j.items(v)?.let { xs -> return xs.map { plain(it, j) } }
        return v
    }

    /** A library object as a plain map; null when [v] is no object. */
    @Suppress("UNCHECKED_CAST")
    fun plainObject(v: Any?, j: JsonShape): Map<String, Any?>? = plain(v, j) as? Map<String, Any?>

    /** The reverse: plain back to the library, null as ITS null (never the text "null"), so a kept key keeps its value. */
    fun unplain(v: Any?, j: JsonShape): Any = when (v) {
        null -> j.nul
        is Map<*, *> -> j.newObject(v.entries.map { (k, x) -> k.toString() to unplain(x, j) })
        is List<*> -> j.newArray(v.map { unplain(it, j) })
        else -> v
    }

    /**
     * The name a prep file is set aside under: `<id>.bad-<yyyyMMdd-HHmmss>.json`
     * at [epochMs] in [zone]; [n] > 1 when that name is already taken.
     */
    fun asideName(id: String, epochMs: Long, zone: ZoneId, n: Int = 1): String =
        "$id.bad-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", java.util.Locale.ROOT).withZone(zone).format(Instant.ofEpochMilli(epochMs)) +
            (if (n > 1) "-$n" else "") + ".json"

    /** Whether a file name is a prep set aside for [id]. */
    fun isAside(id: String, name: String): Boolean = name.startsWith("$id.bad-") && name.endsWith(".json")

    /** The screen's first word on a prep: the saved-state text beside the title (a [Msg], port 61c slice M3b). */
    fun savedText(o: Opened): Msg = when (o) {
        is Opened.Missing -> { tx -> tx.common_nothing_saved_yet }
        is Opened.Restored -> { tx -> tx.common_restored }
        is Opened.Salvaged -> { tx -> tx.sp_file_restored_in_part }
        is Opened.Unreadable -> { tx -> tx.sp_file_started_fresh }
    }

    /** The line that stays on screen when the saved prep could not be read whole; null when it was. {first} renders
     *  problems[0] with the same tx; the aside file NAME is data, byte-identical in every language. */
    fun openedNote(o: Opened): Msg? = when (o) {
        is Opened.Missing, is Opened.Restored -> null
        is Opened.Salvaged -> { tx: Tx ->
            val n = o.decoded.problems.size
            val first = o.decoded.problems.first()(tx)
            val more = if (n > 1) " …" else "" // text:value the more-than-one glyph
            o.aside?.let { tx.sp_file_part_unreadable_kept_x(n = n, first = first, more = more, file = it) }
                ?: tx.sp_file_part_unreadable_not_aside(n = n, first = first, more = more)
        }
        is Opened.Unreadable -> { tx: Tx ->
            o.aside?.let { tx.sp_file_unreadable_kept_x(it) } ?: tx.sp_file_unreadable_not_aside
        }
    }
}
