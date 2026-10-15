package com.malletcrafts.sitephotos

import android.content.Context
import android.util.Log
import com.malletcrafts.sitephotos.pano.SurveyPrep
import com.malletcrafts.sitephotos.pano.SurveyPrepFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.ZoneId

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
 *
 * TOLERANT, AND NEVER WIPED (v52 slice 2). The reading and writing are
 * SurveyPrepFile in :pano, where they are tested; this is the disk and the
 * org.json. A key this build does not read is written back as it came; a
 * file read only in part is COPIED aside before anything is saved over it;
 * a file that is no prep at all is MOVED aside, and the room starts fresh.
 * Either way the original sits beside it as <id>.bad-<yyyyMMdd-HHmmss>.json.
 */
class SurveyPrepStore(context: Context) {
    private val dir = File(context.filesDir, "surveyprep").apply { mkdirs() }
    private fun file(id: String) = File(dir, "$id.json")

    fun exists(id: String) = file(id).exists()
    fun delete(id: String) {
        runCatching { file(id).delete() }
        // its readings baseline goes with it (ReadingsSync), and any copy set aside
        runCatching { File(dir, "$id.sync.json").delete() }
        runCatching { dir.listFiles()?.filter { SurveyPrepFile.isAside(id, it.name) }?.forEach { it.delete() } }
        synchronized(held) { held.remove(id) }
    }

    fun loadText(id: String): String? = runCatching { file(id).takeIf { it.exists() }?.readText() }.getOrNull()

    private fun tree(text: String): Map<String, Any?>? = runCatching { toMap(JSONObject(text)) }.getOrNull()

    /** The laser L, W, H the saved prep was squared to, mm; null when there is none. */
    fun loadSize(id: String): DoubleArray? = loadText(id)?.let { tree(it) }?.let { SurveyPrepFile.sizeOf(it) }

    /** The saved prep as far as it can be read, for a reader that only looks (the room tile); null when there is none. */
    fun load(id: String): SurveyPrep.Prep? = loadText(id)?.let { tree(it) }?.let { SurveyPrepFile.decode(it)?.prep }

    /**
     * The saved prep for the screen that will edit it -- and the one place a
     * file that cannot be read whole is set aside, BEFORE the screen saves.
     * The decision is SurveyPrepFile.open in :pano, where it is tested; this
     * hands it the disk. When the copy or the move fails the capture is HELD:
     * every save is refused, so the only copy is never written over.
     */
    fun open(id: String): SurveyPrepFile.Opened {
        val r = SurveyPrepFile.open(loadText(id), file(id).exists(), { tree(it) },
            copyAside = {
                runCatching { asideFile(id).also { file(id).copyTo(it) }.name }
                    .onFailure { Log.w(TAG, "$id: could not copy the prep aside; saves are refused", it) }.getOrNull()
            },
            moveAside = {
                val f = file(id); val to = asideFile(id)
                val moved = runCatching { f.renameTo(to) || (f.copyTo(to).exists() && f.delete()) }
                    .onFailure { Log.w(TAG, "$id: could not move the prep aside", it) }.getOrDefault(false)
                if (moved) to.name else null
            })
        synchronized(held) { if (r.hold) held.add(id) else held.remove(id) }
        when (val o = r.opened) {
            is SurveyPrepFile.Opened.Salvaged -> for (p in o.decoded.problems) Log.w(TAG, "$id: $p")
            is SurveyPrepFile.Opened.Unreadable ->
                Log.w(TAG, if (o.aside != null) "$id: unreadable prep set aside as ${o.aside}" else "$id: unreadable prep could not be set aside; saves are refused")
            is SurveyPrepFile.Opened.Missing, is SurveyPrepFile.Opened.Restored -> Unit
        }
        return r.opened
    }

    private fun asideFile(id: String): File {
        val now = System.currentTimeMillis(); val zone = ZoneId.systemDefault()
        var n = 1
        while (true) {
            val f = File(dir, SurveyPrepFile.asideName(id, now, zone, n))
            if (!f.exists()) return f
            n++
        }
    }

    /**
     * Write-then-rename, then READ BACK: true only when what is on disk is what
     * was meant (mock-up v33: "Saved ✓ is shown only after the record has been
     * READ BACK and matches"). Refused for a capture whose unreadable file could
     * not be set aside: that file is the only copy of it.
     */
    fun save(id: String, prep: SurveyPrep.Prep, l: Double, w: Double, h: Double): Boolean {
        if (synchronized(held) { id in held }) return false
        return runCatching {
            val body = encode(prep, l, w, h).toString()
            val tmp = File(dir, "$id.json.tmp")
            tmp.writeText(body)
            if (!tmp.renameTo(file(id))) { file(id).writeText(body); tmp.delete() }
            loadText(id) == body
        }.getOrDefault(false)
    }

    companion object {
        private const val TAG = "SurveyPrepStore"

        /** Captures whose unreadable prep is still in place: never saved over (process-wide, as every reader makes its own store). */
        private val held = mutableSetOf<String>()

        fun encode(p: SurveyPrep.Prep, l: Double, w: Double, h: Double): JSONObject =
            toJson(SurveyPrepFile.encode(p, l, w, h)) as JSONObject

        /** A prep file's JSON read as far as it can be; null when it is no prep at all. */
        fun decode(o: JSONObject): SurveyPrep.Prep? = runCatching { SurveyPrepFile.decode(toMap(o))?.prep }.getOrNull()

        /** org.json as SurveyPrepFile reads and builds a JSON tree (the conversion itself is tested in :pano). */
        private val ORG = SurveyPrepFile.JsonShape(
            isNull = { it == null || it === JSONObject.NULL },
            entries = { v -> (v as? JSONObject)?.let { o -> o.keys().asSequence().map { k -> k to o.opt(k) }.toList() } },
            items = { v -> (v as? JSONArray)?.let { a -> List(a.length()) { i -> a.opt(i) } } },
            newObject = { kv -> JSONObject().also { o -> for ((k, x) in kv) o.put(k, x) } },
            newArray = { xs -> JSONArray().also { a -> for (x in xs) a.put(x) } },
            nul = JSONObject.NULL,
        )

        /** org.json to the plain maps and lists SurveyPrepFile reads: JSON null is null. */
        fun toMap(o: JSONObject): Map<String, Any?> = SurveyPrepFile.plainObject(o, ORG) ?: emptyMap()

        /** The reverse: maps and lists back to org.json, null as JSON null, so a kept key keeps its value. */
        fun toJson(v: Any?): Any = SurveyPrepFile.unplain(v, ORG)
    }
}
