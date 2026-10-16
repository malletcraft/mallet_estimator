package com.malletcrafts.sitephotos

import android.content.Context
import com.malletcrafts.sitephotos.pano.Readings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * SITE READINGS ON THE WEB TOO (Amit, 2026-10-11, "Readings on the web"):
 * an estimator types or corrects readings on MCFT Interior Estimate's
 * /projects page, and the phone and the web show the same list. The web's
 * per-key table is the one truth; the merge rule is Readings in :pano.
 *
 * Beside the prep file, filesDir/surveyprep/<captureId>.sync.json holds
 * what this phone last SAW synced per key (value and the server's `at`), and
 * any conflict the upload job met that survey prep has not applied yet.
 *
 *  - push: the upload job, after the room record, PATCHes the keys whose
 *    local value differs from what was seen, each with its base `at`.
 *  - pull: survey prep, on open and resume, GETs the readings and takes what
 *    changed on the web, never an unsent local edit.
 *
 * Without signal everything works on the phone as before; the next upload
 * or open reconciles. A web without the endpoint yet (404/405) is a quiet
 * "web readings not available yet", never a failed room upload.
 */
object ReadingsSync {

    const val NOT_YET = "web readings not available yet"

    private val LOCK = Any()

    private fun file(context: Context, id: String) =
        File(File(context.filesDir, "surveyprep").apply { mkdirs() }, "$id.sync.json")

    class State(val seen: MutableMap<String, Readings.Seen>, val conflicts: MutableMap<String, Readings.Remote>)

    private fun num(o: JSONObject, k: String): Double? = if (!o.has(k) || o.isNull(k)) null else o.optDouble(k)
    private fun str(o: JSONObject, k: String): String? = if (!o.has(k) || o.isNull(k)) null else o.optString(k)

    /** A {key: {v, by, at}} map from the web. */
    fun remoteOf(o: JSONObject?): Map<String, Readings.Remote> {
        val out = mutableMapOf<String, Readings.Remote>()
        if (o == null) return out
        for (k in o.keys()) {
            val r = o.optJSONObject(k) ?: continue
            val at = str(r, "at") ?: continue
            out[k] = Readings.Remote(num(r, "v"), str(r, "by") ?: "", at)
        }
        return out
    }

    private fun remoteJson(r: Readings.Remote): JSONObject =
        JSONObject().put("v", r.v ?: JSONObject.NULL).put("by", r.by).put("at", r.at)

    private fun load(context: Context, id: String): State {
        val seen = mutableMapOf<String, Readings.Seen>()
        val conflicts = mutableMapOf<String, Readings.Remote>()
        runCatching {
            val f = file(context, id)
            if (f.exists()) {
                val o = JSONObject(f.readText())
                o.optJSONObject("seen")?.let { so ->
                    for (k in so.keys()) {
                        val e = so.optJSONObject(k) ?: continue
                        seen[k] = Readings.Seen(num(e, "v"), str(e, "at"))
                    }
                }
                conflicts.putAll(remoteOf(o.optJSONObject("conflicts")))
            }
        }
        return State(seen, conflicts)
    }

    private fun save(context: Context, id: String, st: State) {
        runCatching {
            val o = JSONObject()
                .put("seen", JSONObject().apply {
                    for ((k, s) in st.seen) put(k, JSONObject().put("v", s.v ?: JSONObject.NULL).put("at", s.at ?: JSONObject.NULL))
                })
                .put("conflicts", JSONObject().apply { for ((k, r) in st.conflicts) put(k, remoteJson(r)) })
            file(context, id).writeText(o.toString())
        }
    }

    /** Read, change and write the state as one step (the screen and the upload job share it). */
    fun <T> update(context: Context, id: String, change: (State) -> T): T = synchronized(LOCK) {
        val st = load(context, id)
        val out = change(st)
        save(context, id, st)
        out
    }

    fun delete(context: Context, id: String) { runCatching { file(context, id).delete() } }

    fun notAvailable(e: Throwable): Boolean = e is AsIsUpload.HttpError && (e.code == 404 || e.code == 405)

    private fun url(project: String, room: String) =
        BuildConfig.WEB_URL.trimEnd('/') + "/api/asis/" + AsIsUpload.seg(project) + "/" + AsIsUpload.seg(room) + "/readings"

    /** GET the room's readings from the web. */
    fun fetch(auth: String, project: String, room: String): Map<String, Readings.Remote> =
        remoteOf(JSONObject(AsIsUpload.send("GET", url(project, room), auth, null, null)).optJSONObject("readings"))

    /**
     * The upload job's half: PATCH the unsent edits with their bases. Written
     * keys become seen; conflicts wait in the state for survey prep to apply
     * (the server value wins). Returns a note for the screen, or null.
     */
    fun push(context: Context, id: String, auth: String, project: String, room: String,
             local: Map<String, Double>): String? {
        val seen = update(context, id) { it.seen.toMap() }
        val p = Readings.push(local, seen)
        if (p.isEmpty()) return null
        val body = JSONObject()
            .put("set", JSONObject().apply { for ((k, v) in p.set) put(k, v ?: JSONObject.NULL) })
            .put("base", JSONObject().apply { for ((k, a) in p.base) put(k, a ?: JSONObject.NULL) })
        val ans = JSONObject(AsIsUpload.send("PATCH", url(project, room), auth,
            body.toString().toByteArray(Charsets.UTF_8), "application/json; charset=utf-8"))
        val written = mutableListOf<String>()
        ans.optJSONArray("written")?.let { a: JSONArray -> for (i in 0 until a.length()) written.add(a.optString(i)) }
        val conflicts = remoteOf(ans.optJSONObject("conflicts"))
        val readings = remoteOf(ans.optJSONObject("readings"))
        val m = Readings.afterPush(local, seen, p, written, conflicts, readings)
        update(context, id) { st ->
            for (k in written) {
                val s = m.seen[k]
                if (s == null) st.seen.remove(k) else st.seen[k] = s
            }
            st.conflicts.putAll(conflicts)
        }
        return if (conflicts.isEmpty()) null else conflictText(conflicts)
    }

    fun conflictText(conflicts: Map<String, Readings.Remote>): String =
        conflicts.entries.sortedBy { it.key }.joinToString("; ") { (k, r) ->
            "$k changed on the web" + if (r.by.isNotBlank()) " by ${r.by}" else "" }

    /**
     * Survey prep's half, on the main thread: any waiting conflict is applied
     * (the server value wins), then [remote] (null: not fetched) is merged into
     * [actual] in place. A conflict on a key whose mark is gone ([live] false: deleted or cleared here) is not
     * put back: the phone's delete stands and is pushed again on the server's new `at` (Readings.orphans).
     * Returns the conflicts applied (those excepted), whether [actual] changed, and the keys deleted again.
     */
    fun merge(context: Context, id: String, actual: MutableMap<String, Double>,
              remote: Map<String, Readings.Remote>?,
              live: (String) -> Boolean = { true }): Triple<Map<String, Readings.Remote>, Boolean, List<String>> =
        update(context, id) { st ->
            var changed = false
            val applied = st.conflicts.toMap()
            for ((k, r) in applied) {
                val v = r.v
                if (v == null) { actual.remove(k); st.seen.remove(k) }
                else { actual[k] = v; st.seen[k] = Readings.Seen(v, r.at) }
                changed = true
            }
            val orphans = Readings.orphans(actual, st.seen, applied, live)
            st.conflicts.clear()
            if (remote != null) {
                val m = Readings.pull(actual.toMap(), st.seen.toMap(), remote)
                if (m.taken.isNotEmpty()) {
                    actual.clear(); actual.putAll(m.actual); changed = true
                }
                st.seen.clear(); st.seen.putAll(m.seen)
            }
            Triple(applied.filterKeys { it !in orphans }, changed, orphans)
        }

    fun now(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
}
