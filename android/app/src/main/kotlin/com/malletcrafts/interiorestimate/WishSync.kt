package com.malletcrafts.interiorestimate

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.malletcrafts.interiorestimate.pano.WishList
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * THE CLIENT'S WISH LIST, KEPT ON THE PHONE AND SYNCED TO S5 (v52 item 16, slice 5b). Amit,
 * 14 Oct 2026, 06:35 IST: "i should be able to quickly mark scope while talking with different
 * service vendor using this app ... this is kind of wish list client will have and vendor will
 * fulfil"; 06:36 IST: "but the wishlist should never get mixed with survey as this wishlist
 * comes after survey".
 *
 * One list per project, in preferences under the project's key (WishList.keyOf: the ERP id, or
 * 'local:<client>|<title>' for a project made on this phone), holding the rows, the
 * `wishlist_at` S5 last showed (the next PUT's base), a dirty flag and a note. Every rule --
 * what an edit does, the 200, the 409, the GET, the re-key -- is WishList in :pano; this file
 * only stores and talks.
 *
 *  - [rekey]: SyncWorker, once the masters are fresh -- a list made on a phone-only project
 *    moves under the ERP id that project now has, so it is not orphaned (decision 3), and a
 *    forward is left at its old key so a screen still holding the phone-only project reads
 *    and edits the moved list ([load] and [edit] follow it).
 *  - [renameLocal]: a phone-only client or project renamed on this phone takes its list along.
 *  - [push]: SyncWorker, after QuoteStageSync -- ONE PUT of every dirty list that has an ERP
 *    id, with its base. 200: S5's stamp is the base and the list is clean. 409 with S5's rows:
 *    S5 WINS (decision 2) -- the phone takes them and says so. 409 at Survey and 404 (S5 has
 *    never had the project): kept on the phone, dirty, sent next sync. 400/403/413 (a key that
 *    is not staff, a shape S5 will not take): refused for good -- one note, no longer dirty,
 *    never PUT again until the next edit (WishList.afterRefused).
 *  - [pull]: on project open -- a clean list takes S5's copy, so staff edits on /projects show.
 *
 * NEVER A FAILURE OF THE SYNC (slice 5's pattern): 400, 401, 403, 405, a 5xx, no network or an
 * answer that does not parse is one Log.w and nothing else, and SyncWorker never counts it, so
 * S5 being unreachable or refusing can never make the capture upload back off. A phone-only
 * project's list never calls S5.
 */
object WishSync {

    private const val TAG = "WishList"
    private const val PREFS = "wish_list"
    private val LOCK = Any()

    /** The lists' preferences; the screens listen on them so any write redraws the chip and the list. */
    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun url(project: String) =
        BuildConfig.WEB_URL.trimEnd('/') + "/api/asis/" + AsIsUpload.seg(project) + "/wishlist"

    // ---- the store ----------------------------------------------------------

    private fun rowJson(r: WishList.Row): JSONObject = JSONObject()
        .put("on", r.on)
        .put("rooms", JSONArray().apply { r.rooms.forEach { put(it) } })
        .put("note", r.note).put("vendor", r.vendor)

    private fun rowsJson(rows: Map<String, WishList.Row>): JSONObject =
        JSONObject().apply { for ((id, r) in rows) put(id, rowJson(r)) }

    /** S5's {id: {on, rooms, note, vendor}}, or this phone's own copy of it. A row that does not parse is left out. */
    private fun rowsOf(o: JSONObject?): Map<String, WishList.Row> {
        val out = linkedMapOf<String, WishList.Row>()
        if (o == null) return out
        for (id in o.keys()) {
            val r = o.optJSONObject(id) ?: continue
            val a = r.optJSONArray("rooms")
            val rooms = if (a == null) emptyList()
                else (0 until a.length()).mapNotNull { i -> a.optString(i).ifBlank { null } }
            out[id] = WishList.Row(
                on = r.optBoolean("on", false),
                rooms = rooms,
                note = if (r.isNull("note")) "" else r.optString("note"),
                vendor = if (r.isNull("vendor")) "" else r.optString("vendor"))
        }
        return out
    }

    private fun encode(l: WishList.Local): String = JSONObject()
        .put("client", l.client).put("title", l.title)
        .put("rows", rowsJson(l.rows))
        .put("at", l.at).put("dirty", l.dirty)
        .put("savedAt", l.savedAt).put("note", l.note)
        .toString()

    private fun decode(raw: String?): WishList.Local? {
        if (raw == null) return null
        return try {
            val o = JSONObject(raw)
            WishList.Local(
                client = o.optString("client"),
                title = o.optString("title"),
                rows = rowsOf(o.optJSONObject("rows")),
                at = o.optString("at"),
                dirty = o.optBoolean("dirty", false),
                savedAt = o.optString("savedAt"),
                note = o.optString("note"))
        } catch (e: Exception) {
            null
        }
    }

    /** The key [key]'s list is really kept under: a moved local key follows its forward. */
    private fun resolve(context: Context, key: String): String {
        val p = prefs(context)
        return WishList.resolve(key) { f -> p.getString(f, null) }
    }

    /** The list kept under [key] (or where it moved), or null when this phone has none. */
    fun load(context: Context, key: String): WishList.Local? =
        if (key.isBlank()) null else decode(prefs(context).getString(resolve(context, key), null))

    /** How many services are wanted on the list kept under [key]. */
    fun count(context: Context, key: String): Int = load(context, key)?.let { WishList.count(it.rows) } ?: 0

    private fun all(context: Context): Map<String, WishList.Local> {
        val out = linkedMapOf<String, WishList.Local>()
        for ((k, v) in prefs(context).all) {
            if (WishList.isForward(k)) continue
            decode(v as? String)?.let { out[k] = it }
        }
        return out
    }

    private fun write(context: Context, key: String, l: WishList.Local) {
        prefs(context).edit().putString(key, encode(l)).apply()
    }

    /**
     * An edit from the screen, on the main thread: [change] applied to the rows as they are
     * stored now, kept dirty with this moment as its save time. Returns the list as stored.
     */
    fun edit(context: Context, key: String, client: String, title: String,
             change: (Map<String, WishList.Row>) -> Map<String, WishList.Row>): WishList.Local =
        synchronized(LOCK) {
            // Where the list really is: a stale phone-only project edits the list it moved to.
            val at = resolve(context, key)
            val cur = load(context, at) ?: WishList.Local(client, title)
            val next = WishList.edited(cur, change(cur.rows), Instant.now().toString())
            write(context, at, next)
            next
        }

    /**
     * A phone-only client or project renamed on this phone (Catalogue.renameLocal, already
     * done): its lists move to the new local key in one edit, so the chip and the screen find
     * them under the new name and the re-key later matches the new name. [live] is the local
     * keys of the phone-only projects as they are AFTER the rename. Never throws.
     */
    fun renameLocal(context: Context, kind: String, client: String, title: String, newName: String,
                    live: Set<String>) {
        try {
            synchronized(LOCK) {
                val moves = WishList.renamed(all(context), kind, client, title, newName, live)
                if (moves.isEmpty()) return
                val ed = prefs(context).edit()
                for ((from, m) in moves) {
                    ed.putString(m.to, encode(m.list))
                    if (!m.keepOld) ed.remove(from)
                }
                ed.apply()
            }
        } catch (e: Exception) {
            Log.w(TAG, "wish lists not renamed: " + (e.message?.take(160) ?: e.javaClass.simpleName))
        }
    }

    // ---- the sync -----------------------------------------------------------

    /**
     * SyncWorker, once the masters are fresh: every list kept under a local key whose client
     * and title now name an ERP project moves under that project's id (WishList.adopt), and a
     * forward is left at the old key. A list adopt will not move (an unsent list is already
     * under the id) stays where it is and is tried again next sync. Never throws.
     */
    fun rekey(context: Context, projects: List<Catalogue.Project>) {
        try {
            val erp = projects.filter { it.synced }.map { WishList.Erp(it.serverId, it.client, it.title) }
            if (erp.isEmpty()) return
            synchronized(LOCK) {
                val lists = all(context)
                val moves = WishList.rekeys(lists, erp)
                if (moves.isEmpty()) return
                val ed = prefs(context).edit()
                var moved = false
                for ((from, to) in moves) {
                    val local = lists[from] ?: continue
                    val next = WishList.adopt(local, lists[to])
                    if (next == null) {
                        Log.i(TAG, "$from: an unsent list is already under $to; this one waits on the phone")
                        continue
                    }
                    ed.putString(to, encode(next))
                    ed.putString(WishList.forwardKey(from), to)
                    ed.remove(from)
                    moved = true
                }
                if (moved) ed.apply()
            }
        } catch (e: Exception) {
            Log.w(TAG, "wish lists not re-keyed: " + (e.message?.take(160) ?: e.javaClass.simpleName))
        }
    }

    /**
     * SyncWorker, after QuoteStageSync: ONE PUT per dirty list that has an ERP id. Blocking;
     * never throws, never retries, never a failure of the sync that called it.
     */
    fun push(context: Context) {
        val auth = FrappeClient.authorization(context) ?: return
        val dirty = try {
            all(context).filter { (k, l) -> l.dirty && !WishList.isLocal(k) }
        } catch (e: Exception) {
            Log.w(TAG, "wish lists not read: " + (e.message?.take(160) ?: e.javaClass.simpleName)); return
        }
        for ((key, l) in dirty) pushOne(context, auth, key, l)
    }

    private fun pushOne(context: Context, auth: String, key: String, l: WishList.Local) {
        try {
            val sent = WishList.forPut(l.rows)
            val body = JSONObject().put("rows", rowsJson(sent)).put("base", l.at).toString()
            val a = AsIsUpload.exchange("PUT", url(key), auth, body.toByteArray(Charsets.UTF_8),
                "application/json; charset=utf-8")
            when {
                a.ok -> {
                    val o = JSONObject(a.body)
                    val at = o.getString("wishlist_at")
                    synchronized(LOCK) {
                        val cur = load(context, key) ?: return
                        write(context, key, WishList.afterPut(cur, sent, at))
                    }
                }
                a.code == 409 -> {
                    val o = runCatching { JSONObject(a.body) }.getOrNull()
                    val rows = o?.optJSONObject("rows")
                    if (o != null && rows != null) {
                        // The base was stale: S5 WINS (decision 2).
                        val at = if (o.isNull("wishlist_at")) "" else o.optString("wishlist_at")
                        synchronized(LOCK) {
                            val cur = load(context, key) ?: return
                            write(context, key, WishList.afterConflict(cur, rowsOf(rows), at))
                        }
                        Log.i(TAG, "$key: S5 had a newer wish list; the phone took it")
                    } else {
                        // At Survey S5 takes no list: it waits here, dirty, for the stage to move.
                        Log.w(TAG, "$key: " + AsIsUpload.errorOf(a).message)
                    }
                }
                // S5 has never had this project: the list waits on the phone (decision 3).
                a.code == 404 -> Log.i(TAG, "$key: not on S5 yet; the wish list stays on this phone")
                // Refused for good (a key that is not staff, a bad shape): one note, no endless retry.
                WishList.refusedForGood(a.code) -> {
                    synchronized(LOCK) {
                        val cur = load(context, key) ?: return
                        write(context, key, WishList.afterRefused(cur, sent, s5Error(a)))
                    }
                    Log.w(TAG, "$key: wish list refused: " + AsIsUpload.errorOf(a).message)
                }
                else -> Log.w(TAG, "$key: wish list not sent: " + AsIsUpload.errorOf(a).message)
            }
        } catch (e: Exception) {
            // Never the header: the status and S5's own error text at most.
            Log.w(TAG, "$key: wish list not sent: " + (e.message?.take(160) ?: e.javaClass.simpleName))
        }
    }

    /** S5's own reason for a refusal (its `error`), one line; its status when it gave none. */
    private fun s5Error(a: AsIsUpload.Answer): String {
        val text = runCatching { JSONObject(a.body).optString("error") }.getOrNull().orEmpty()
        return text.replace(Regex("\\s+"), " ").trim().take(160).ifBlank { "S5 answered ${a.code}" }
    }

    /**
     * On project open (off the main thread): S5's copy for one ERP project. A clean list takes
     * it; an unsent edit is never touched; and anything written while the GET was out (a PUT's
     * 200, an edit) wins over the GET's older answer (WishList.afterGet's compare-and-set).
     * Returns whether anything was written. Never throws.
     */
    fun pull(context: Context, project: Catalogue.Project): Boolean {
        if (!project.synced) return false
        val auth = FrappeClient.authorization(context) ?: return false
        val key = WishList.keyOf(project.serverId, project.client, project.title)
        val blank = WishList.Local(project.client, project.title)
        return try {
            // The list as stored when the GET goes out: the compare half of the compare-and-set.
            val seen = synchronized(LOCK) { load(context, key) } ?: blank
            val o = JSONObject(AsIsUpload.send("GET", url(key), auth, null, null))
            // A 200 without the rows is not "nothing wanted": it would clear the list.
            val rows = rowsOf(o.getJSONObject("rows"))
            val at = if (o.isNull("wishlist_at")) "" else o.optString("wishlist_at")
            synchronized(LOCK) {
                val cur = load(context, key) ?: blank
                val next = WishList.afterGet(cur, rows, at, seen)
                // The same list again (or S5's blank one over none here) is not a change: no write, no redraw.
                if (next.at == cur.at && WishList.forPut(next.rows) == WishList.forPut(cur.rows)) return false
                write(context, key, next)
            }
            true
        } catch (e: Exception) {
            // 404: S5 has never had the project; anything else is a refusal, a 5xx or no network.
            Log.w(TAG, "$key: wish list not read from S5: " + (e.message?.take(160) ?: e.javaClass.simpleName))
            false
        }
    }
}
