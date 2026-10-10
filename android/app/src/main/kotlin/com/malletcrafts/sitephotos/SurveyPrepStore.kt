package com.malletcrafts.sitephotos

import android.content.Context
import com.malletcrafts.sitephotos.pano.SurveyPrep
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Survey prep for one capture, on the phone: filesDir/surveyprep/<captureId>.json
 * -- every surface's four lines, its marks, and the site readings. Saved after
 * each change and restored when the screen opens again (mock-up v33: "lines,
 * marks and readings save a moment after each change ... and come back when
 * you reopen it").
 *
 * Local only, like Face Prep: nothing on the bench reads the room setup, and
 * the bench is pull-only for now, so a sync would be a copy with no reader.
 * It outlives the upload with the capture's 360 (Amit, 2026-10-10: "Keep a
 * local copy"), so site readings can be added on a kept capture. Deleting or
 * discarding the capture, or "Remove local copy", deletes this file with it.
 */
class SurveyPrepStore(context: Context) {
    private val dir = File(context.filesDir, "surveyprep").apply { mkdirs() }
    private fun file(id: String) = File(dir, "$id.json")

    fun exists(id: String) = file(id).exists()
    fun delete(id: String) { runCatching { file(id).delete() } }

    fun loadText(id: String): String? = runCatching { file(id).takeIf { it.exists() }?.readText() }.getOrNull()

    /** The laser L, W, H the saved prep was squared to, mm; null when there is none. */
    fun loadSize(id: String): DoubleArray? = runCatching {
        loadText(id)?.let { JSONObject(it) }?.let { o -> doubleArrayOf(o.getDouble("L"), o.getDouble("W"), o.getDouble("H")) }
    }.getOrNull()

    fun load(id: String): SurveyPrep.Prep? = runCatching { loadText(id)?.let { decode(JSONObject(it)) } }.getOrNull()

    /**
     * Write-then-rename, then READ BACK: true only when what is on disk is what
     * was meant (mock-up v33: "Saved ✓ is shown only after the record has been
     * READ BACK and matches").
     */
    fun save(id: String, prep: SurveyPrep.Prep, l: Double, w: Double, h: Double): Boolean = runCatching {
        val body = encode(prep, l, w, h).toString()
        val tmp = File(dir, "$id.json.tmp")
        tmp.writeText(body)
        if (!tmp.renameTo(file(id))) { file(id).writeText(body); tmp.delete() }
        loadText(id) == body
    }.getOrDefault(false)

    companion object {
        private fun pt(p: DoubleArray) = JSONArray().put(p[0]).put(p[1])

        fun encode(p: SurveyPrep.Prep, l: Double, w: Double, h: Double): JSONObject = JSONObject().apply {
            put("v", 1); put("L", l); put("W", w); put("H", h)
            put("w", JSONObject().apply {
                for (s in SurveyPrep.SURFACES) {
                    val sf = p.surfaces[s] ?: continue
                    put(s, JSONObject().apply {
                        for (k in SurveyPrep.SLOTS) sf.lines[k]?.let { ln -> put(k, JSONArray().put(pt(ln[0])).put(pt(ln[1]))) }
                        put("set", JSONObject().apply { for (k in SurveyPrep.SLOTS) put(k, sf.set[k] == true) })
                        put("els", JSONArray().apply {
                            for (m in sf.marks) put(JSONObject().put("id", m.id).put("t", m.t).put("code", m.code)
                                .put("x0", m.x0).put("x1", m.x1).put("y0", m.y0).put("y1", m.y1))
                        })
                    })
                }
            })
            put("actual", JSONObject().apply { for ((k, v) in p.actual) put(k, v) })
        }

        /** Null when any surface is missing or malformed: a half-read prep would look like lost work. */
        fun decode(o: JSONObject): SurveyPrep.Prep? {
            val wo = o.optJSONObject("w") ?: return null
            val out = mutableMapOf<String, SurveyPrep.Surface>()
            for (s in SurveyPrep.SURFACES) {
                val so = wo.optJSONObject(s) ?: return null
                val lines = mutableMapOf<String, Array<DoubleArray>>()
                for (k in SurveyPrep.SLOTS) {
                    val a = so.optJSONArray(k) ?: return null
                    if (a.length() != 2) return null
                    lines[k] = Array(2) { i -> val q = a.getJSONArray(i); doubleArrayOf(q.getDouble(0), q.getDouble(1)) }
                }
                val setO = so.optJSONObject("set")
                val set = SurveyPrep.SLOTS.associateWith { setO?.optBoolean(it, false) ?: false }.toMutableMap()
                val marks = mutableListOf<SurveyPrep.Mark>()
                so.optJSONArray("els")?.let { a ->
                    for (i in 0 until a.length()) {
                        val m = a.getJSONObject(i)
                        marks.add(SurveyPrep.Mark(m.getString("id"), m.getString("t"), m.getString("code"),
                            m.getDouble("x0"), m.getDouble("x1"), m.getDouble("y0"), m.getDouble("y1")))
                    }
                }
                out[s] = SurveyPrep.Surface(lines, set, marks)
            }
            val actual = mutableMapOf<String, Double>()
            o.optJSONObject("actual")?.let { ao -> for (k in ao.keys()) actual[k] = ao.getDouble(k) }
            return SurveyPrep.Prep(out, actual)
        }
    }
}
