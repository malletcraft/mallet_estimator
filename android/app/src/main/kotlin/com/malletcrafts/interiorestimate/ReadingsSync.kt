package com.malletcrafts.interiorestimate

import android.content.Context
import com.malletcrafts.interiorestimate.pano.AsIsPath
import com.malletcrafts.interiorestimate.pano.Readings
import com.malletcrafts.interiorestimate.pano.TourDemo
import com.malletcrafts.interiorestimate.pano.i18n.Tx
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
 *  - push: the upload job, after the room record and its photos, PATCHes the
 *    keys whose local value differs from what was seen, each with its base `at`.
 *  - pull: survey prep, on open and resume, GETs the readings and takes what
 *    changed on the web, never an unsent local edit.
 *
 * THE READINGS ARE ONE CAPTURE'S (port 61c slice AS1, 3 Nov 2026): every
 * address names the capture by its device id, …/c/<deviceId>/readings
 * (AsIsPath), so a room with two 360s never has one capture's figures merged
 * into the other's survey prep -- S5's "ONE SERVER ROOM PER CAPTURE". The pull
 * also asks the ERP name (MEST-PH-…) while S5 still holds the capture under it
 * ([fetch]); a 404 on both stays the quiet not-yet line (sp.web_readings_not_yet, by its
 * Readings.NoteKind since port 61c slice M3b).
 *
 * Every key is sent alike, whatever owns it: a mark's FD1.w, a Line's F-M1.len,
 * a chain's segments FK1.1 … FK1.n and its total FK1.t (v52 slice 11; S5's
 * grammar takes them since slice 3, KeyGrammarTest). A chain deleted or cleared
 * takes its keys out of `actual`, so the next push sends each as a delete.
 *
 * Without signal everything works on the phone as before; the next upload
 * or open reconciles. The PULL's 404 on both names is the quiet "web readings
 * not available yet" ([notAvailable]): a capture not on the web yet (queued,
 * or never uploaded). The PUSH's 404 is something else -- the capture gone
 * from the web since the job's own record went -- and the job runs again from
 * the record (AsIsUpload.RecordMissing), never a quiet "done" (the AS1 review,
 * 3 Nov 2026).
 */
object ReadingsSync {

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

    /**
     * The PULL's reading of a refusal (survey prep): a 404 on both of the capture's names, or a 405, is a capture
     * whose readings the web does not have yet -- the quiet not-yet line (sp.web_readings_not_yet, grey by its
     * Readings.NoteKind since port 61c slice M3b). Not the push's: after the job's own record
     * PUT, S5 serves the readings route of that capture (the explicit routes landed together, 14 Oct 2026), so a 404
     * there is the capture gone from the web, and AsIsUpload runs the job again from the record.
     */
    fun notAvailable(e: Throwable): Boolean = e is AsIsUpload.HttpError && (e.code == 404 || e.code == 405)

    /**
     * Forget everything this phone last saw synced for [id]: the seen baseline AND any conflict waiting
     * for survey prep. The upload job calls it the moment S5 answers the record with `created: true` -- the
     * capture is new on the web, or was deleted there and is back -- so what was seen is of rows that no
     * longer exist, and the push that follows sends every local reading as a first write (port item 4:
     * without this, only the keys that changed would go, and the next pull would delete the rest on the
     * phone as "gone from the web"). At once, before the photos: a job that fails after the record runs
     * again from it, and the second PUT is never `created` (the AS1 review). A waiting conflict is the
     * web's value from before the delete, and is not applied either.
     */
    fun forget(context: Context, id: String) {
        update(context, id) { st -> st.seen.clear(); st.conflicts.clear() }
    }

    /** The capture's readings on the web: …/<project>/<room>/c/<capture>/readings (AsIsPath), each segment encoded once. */
    private fun url(project: String, room: String, capture: String) =
        BuildConfig.WEB_URL.trimEnd('/') + AsIsPath.path(AsIsPath.readings(project, room, capture)) { AsIsUpload.seg(it) }

    private fun get(auth: String, project: String, room: String, capture: String): Map<String, Readings.Remote> =
        remoteOf(JSONObject(AsIsUpload.send("GET", url(project, room, capture), auth, null, null)).optJSONObject("readings"))

    /**
     * GET one capture's readings from the web, by its device id -- and, when that is a 404 and ERP has named the
     * capture ([erpCapture], the row's serverName, MEST-PH-…), by that name (port 61c slice AS1, critic S3): until
     * the capture's first record PUT from this build lands, S5 still holds it under the name 0.3.212's record gave
     * it, and a pull that asked only the device id would read 404 as the quiet not-yet line (Readings.NoteKind.NOT_YET)
     * and quietly stop taking the web's edits. The two names are ONE capture (S5's C3 renames it onto the device id at
     * that first PUT, after which the device id answers and the second name is never asked). Any other refusal, and a
     * 404 on both, throws.
     */
    fun fetch(auth: String, project: String, room: String, capture: String, erpCapture: String? = null): Map<String, Readings.Remote> {
        // THE DEMO NEVER REACHES S5 (port 61c §31): a demo capture has no web readings to pull
        if (TourDemo.isDemoCapture(capture)) return emptyMap()
        val older = erpCapture?.takeIf { it != capture }
        return try {
            get(auth, project, room, capture)
        } catch (e: AsIsUpload.HttpError) {
            if (e.code == 404 && older != null) get(auth, project, room, older) else throw e
        }
    }

    /**
     * The upload job's half: PATCH the unsent edits with their bases, to THIS capture (by its device id: the
     * job has just put the record under it, so S5's C3 has renamed any older row and the PATCH needs no second
     * name). Written keys become seen; conflicts wait in the state for survey prep to apply (the server value
     * wins). Returns a note for the screen, or null. A refusal is thrown as it came: the job reads a 404 as the
     * capture gone from the web (RecordMissing) and any other 4xx as a note.
     */
    fun push(context: Context, id: String, auth: String, project: String, room: String, capture: String,
             local: Map<String, Double>): String? {
        // THE DEMO NEVER REACHES S5 (port 61c §31): its readings stay on the phone
        if (TourDemo.isDemoCapture(capture) || TourDemo.isDemoCapture(id)) return null
        val seen = update(context, id) { it.seen.toMap() }
        val p = Readings.push(local, seen)
        if (p.isEmpty()) return null
        val body = JSONObject()
            .put("set", JSONObject().apply { for ((k, v) in p.set) put(k, v ?: JSONObject.NULL) })
            .put("base", JSONObject().apply { for ((k, a) in p.base) put(k, a ?: JSONObject.NULL) })
        val ans = JSONObject(AsIsUpload.send("PATCH", url(project, room, capture), auth,
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
        // THE PUSH PATH'S NOTE IS STORED ENGLISH FOR GOOD, re-rendered by Readings.conflictNoteLine (port 61c
        // slice M2): it is kept in the job's state (AsIsUpload.setState), old files already hold these bytes,
        // and the status line draws it in the live language through UploadNote.line -> conflictNoteLine. The
        // PULL path (survey prep) renders Readings.conflictText with the screen's live tx, as before.
        return if (conflicts.isEmpty()) null else Readings.conflictText(Tx.EN, conflicts)
    }

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

    fun now(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.ROOT))
}
