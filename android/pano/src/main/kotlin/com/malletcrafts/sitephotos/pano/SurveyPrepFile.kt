package com.malletcrafts.sitephotos.pano

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
 *    -- the store then copies the file aside before anything is saved;
 *  - a file that is not a prep at all decodes to null and is set aside
 *    whole, never overwritten.
 *
 * Shape (v = 1, unchanged: the change is additive): {v, L, W, H, w: {front:
 * {bottom, top, left, right: [[x, y], [x, y]], set: {bottom: true, ...},
 * els: [{id, t, code, x0, x1, y0, y1}]}, ...}, actual: {"FW1.w": 1200}}.
 */
object SurveyPrepFile {

    const val VERSION = 1

    /** The keys this build reads, at each level; every other key is kept as it came. */
    val PREP_KEYS = setOf("v", "L", "W", "H", "w", "actual")
    val SURFACE_KEYS = SurveyPrep.SLOTS.toSet() + setOf("set", "els")
    val MARK_KEYS = setOf("id", "t", "code", "x0", "x1", "y0", "y1")

    /** A prep as read: the prep, the size it was squared to (null when the file has no valid one), and what had to be dropped. */
    class Decoded(val prep: SurveyPrep.Prep, val size: DoubleArray?, val problems: List<String>)

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
        val problems = mutableListOf<String>()
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
                problems.add("${SurveyPrep.NAME.getValue(s)}: ${if (wo.containsKey(s)) "unreadable" else "missing"}, started again")
                out[s] = fresh.surface(s).copy()
                continue
            }
            val lines = mutableMapOf<String, Array<DoubleArray>>()
            val set = mutableMapOf<String, Boolean>()
            val setO = so["set"] as? Map<*, *>
            for (k in SurveyPrep.SLOTS) {
                val l = line(so[k])
                if (l == null) {
                    problems.add("${SurveyPrep.NAME.getValue(s)}: its ${SurveyPrep.lineNames(s).getValue(k)} was unreadable, placed again")
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
            if (els != null && els !is List<*>) problems.add("${SurveyPrep.NAME.getValue(s)}: its marks were unreadable")
            (els as? List<*>)?.forEachIndexed { i, mv ->
                val m = mv as? Map<*, *>
                val id = str(m?.get("id")); val t = str(m?.get("t")); val code = str(m?.get("code"))
                val c = listOf("x0", "x1", "y0", "y1").map { num(m?.get(it)) }
                if (m == null || id == null || t == null || code == null || c.any { it == null }) {
                    problems.add("${SurveyPrep.NAME.getValue(s)}: mark ${code ?: "#" + (i + 1)} was unreadable, left out")
                    return@forEachIndexed
                }
                marks.add(SurveyPrep.Mark(id, t, code, c[0]!!, c[1]!!, c[2]!!, c[3]!!, unknown(m, MARK_KEYS)))
            }
            out[s] = SurveyPrep.Surface(lines, set, marks, unknown(so, SURFACE_KEYS))
        }
        val actual = LinkedHashMap<String, Double>()
        val ao = o["actual"]
        if (ao != null && ao !is Map<*, *>) problems.add("the readings were unreadable")
        (ao as? Map<*, *>)?.forEach { (k, v) ->
            val key = k as? String ?: return@forEach
            val d = num(v)
            if (d == null) problems.add("reading $key was unreadable, left out") else actual[key] = d
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

    /**
     * The name a prep file is set aside under: `<id>.bad-<yyyyMMdd-HHmmss>.json`
     * at [epochMs] in [zone]; [n] > 1 when that name is already taken.
     */
    fun asideName(id: String, epochMs: Long, zone: ZoneId, n: Int = 1): String =
        "$id.bad-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(zone).format(Instant.ofEpochMilli(epochMs)) +
            (if (n > 1) "-$n" else "") + ".json"

    /** Whether a file name is a prep set aside for [id]. */
    fun isAside(id: String, name: String): Boolean = name.startsWith("$id.bad-") && name.endsWith(".json")

    /** The screen's first word on a prep: the saved-state text beside the title. */
    fun savedText(o: Opened): String = when (o) {
        is Opened.Missing -> "Nothing saved yet"
        is Opened.Restored -> "Restored ✓"
        is Opened.Salvaged -> "Restored in part"
        is Opened.Unreadable -> "Started fresh"
    }

    /** The line that stays on screen when the saved prep could not be read whole; null when it was. */
    fun openedNote(o: Opened): String? = when (o) {
        is Opened.Missing, is Opened.Restored -> null
        is Opened.Salvaged -> {
            val n = o.decoded.problems.size
            "Part of the saved prep could not be read ($n thing${if (n == 1) "" else "s"}: " +
                o.decoded.problems.first() + (if (n > 1) " …" else "") + "). " +
                (o.aside?.let { "The file as it was is kept as $it." } ?: "The file as it was could not be copied aside.")
        }
        is Opened.Unreadable ->
            o.aside?.let { "The saved prep could not be read. It is kept as $it, and this room starts fresh." }
                ?: "The saved prep could not be read, nor moved aside: nothing here is saved, so it is not overwritten."
    }
}
