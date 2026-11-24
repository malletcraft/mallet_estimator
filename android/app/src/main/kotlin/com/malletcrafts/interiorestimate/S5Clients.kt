package com.malletcrafts.interiorestimate

import android.content.Context
import android.util.Log
import com.malletcrafts.interiorestimate.pano.ClientNames
import com.malletcrafts.interiorestimate.pano.KeyBook
import com.malletcrafts.interiorestimate.pano.Renames
import com.malletcrafts.interiorestimate.pano.TreeKeep
import org.json.JSONObject

/**
 * S5'S NAME FOR EACH PROJECT'S CLIENT, AS THIS PHONE LAST HEARD IT (port 63 slice 5, 24 Nov 2026; Amit: "Client
 * rename"; android/docs/port63/DESIGN-tree.md §11). The twin of [S5Sites], and the same shape on purpose.
 *
 * S5 keeps the client's name on each project (`client_name`, `client_name_at`) and gives it to staff alone. The
 * phone keeps S5's answer in ONE place, per S5 project id: the name ('' = S5 lists the project and holds no name
 * for its client) and S5's own stamp, as a [Renames.Held] whose `site` field carries the NAME. A project with no
 * entry is one S5 has given no client-name key for on this phone: only on the phone, read by a login that is not
 * staff, or read from an S5 that does not keep the name. The tree reads this file FIRST (Catalogue.erpProjects,
 * ClientNames.nameOf), so the name survives a reinstall: it is S5's, not this phone's memory.
 *
 * Fed wherever [S5Sites] is fed -- S5's list at every read of it (S5Masters.fetch, QuoteStageSync.refresh,
 * SiteRename.hear) and S5's answer to every `PUT …/client` (ClientRename: a rename, the backfill, a 409's name as
 * stored) -- and each write goes by S5's own stamp, never by arrival ([Renames.takes]).
 *
 * THE PHONE'S ROWS FOLLOW THE NAME HERE. A client's name is not only shown: the phone's capture rows, the project
 * rows made on it and the S5 keys it remembers are FILED under it. So [takeList], the one place a name is written,
 * is also where those rows move to it (ClientNames.steps says what moves; Catalogue.followClient, CaptureStore.setNames
 * and S5Keys.remember apply it): this phone's own Save, another phone's rename heard by a list read, a 409's name as
 * stored and each project of "1 of 2" alike. It is asked for EVERY named row of a read, not only a changed one, and
 * moves nothing where the rows already agree -- so a step that failed once is made good by the next read.
 *
 * Like S5's site names it is not cleared at sign-out, and one entry goes only with the delete that proved S5 no
 * longer lists its project ([forget]).
 */
object S5Clients {

    private const val TAG = "S5Clients"
    private const val PREFS = "s5_clients"

    /** Guards every read-compare-write, so two answers landing together are each judged against the other. */
    private val LOCK = Any()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun decode(raw: Any?): Renames.Held? {
        val text = raw as? String ?: return null
        val o = runCatching { JSONObject(text) }.getOrNull() ?: return null
        return Renames.Held(o.optString("name"), o.optString("at"))
    }

    private fun encode(h: Renames.Held): String = JSONObject().put("name", h.site).put("at", h.at).toString()

    /** S5's name for [project]'s client ('' = none held), or null when S5 has given this phone no client-name key for the project. */
    fun held(context: Context, project: String): Renames.Held? =
        if (project.isBlank()) null else decode(prefs(context).getString(project, null))

    /** Every project S5 has given a client-name key for here, by S5 project id: one read for the whole tree. */
    fun all(context: Context): Map<String, Renames.Held> {
        val out = HashMap<String, Renames.Held>()
        for ((project, raw) in prefs(context).all) decode(raw)?.let { out[project] = it }
        return out
    }

    /** S5's list, read for one delete, no longer names [project]: its entry goes, and only it (beside S5Sites.forget). */
    fun forget(context: Context, project: String) {
        if (project.isBlank()) return
        synchronized(LOCK) { prefs(context).edit().remove(project).apply() }
    }

    /** One answer of S5's about [project]'s client. Returns whether it was taken (it is not older than what is held). */
    fun take(context: Context, project: String, copy: Renames.Held): Boolean = takeList(context, listOf(project to copy)) > 0

    /** What this phone files one project under just before S5's name for it is written: every client and title, and the site the tree lists it at. */
    private class Filed(val names: List<KeyBook.Names>, val site: String)

    private fun filed(context: Context, projects: Set<String>): Map<String, Filed> {
        val store = CaptureStore(context)
        val captures = store.all()
        val tree = Catalogue(context).projects(store.masters())
        val book = S5Keys.book(context)
        val out = HashMap<String, Filed>()
        for (id in projects) {
            val listed = tree.filter { it.serverId == id }
            val names = listed.map { KeyBook.Names(it.client, it.title) } +
                captures.filter { it.project == id }.map { KeyBook.Names(it.customerName, it.projectTitle) } +
                listOfNotNull(KeyBook.namesOf(book, id))
            out[id] = Filed(names, listed.firstOrNull()?.site?.takeIf { it != Catalogue.NO_SITE }.orEmpty())
        }
        return out
    }

    /** The phone's rows of each project in [named] (project id -> S5's name for its client) move to that name, where they are filed under another. */
    private fun follow(context: Context, named: Map<String, String>, before: Map<String, Filed>) {
        val steps = named.flatMap { (project, to) -> ClientNames.steps(project, to, before[project]?.names.orEmpty()) }
        if (steps.isEmpty()) return
        val store = CaptureStore(context)
        val cat = Catalogue(context)
        val move = ClientNames.follows(steps)
        for (c in store.all()) {
            val was = TreeKeep.Cap(c.project, c.customerName, c.projectTitle, sent = c.state == "SYNCED") // text:value the store's state, a wire value
            val now = move(was)
            if (now != was) store.setNames(c.deviceId, now.client, now.title)
        }
        for (s in steps) {
            cat.followClient(s.from, s.title, s.to, before[s.project]?.site.orEmpty())
            S5Keys.remember(context, s.project, s.to, s.title)
        }
    }

    /**
     * Rows of S5's (ClientNames.listedOf, or one answer), each taken only where it is not older than what is held.
     * Returns how many were. For every taken row that NAMES a client, the phone's own rows of that project follow
     * the name (see the header): what they are filed under is read just before the names are written, and
     * ClientNames.steps says what moves. That step never costs S5's name -- the name is written whatever the step does.
     */
    fun takeList(context: Context, rows: List<Pair<String, Renames.Held>>): Int = synchronized(LOCK) {
        val p = prefs(context)
        val ed = p.edit()
        var taken = 0
        val named = LinkedHashMap<String, String>()
        for ((project, copy) in rows) {
            if (project.isBlank()) continue
            val stored = decode(p.getString(project, null))
            if (!Renames.takes(stored, copy)) continue
            if (copy.site.isNotBlank()) named[project] = copy.site
            ed.putString(project, encode(copy))
            taken++
        }
        if (taken > 0) {
            val before = if (named.isEmpty()) null else runCatching { filed(context, named.keys) }.getOrNull()
            // apply() is in memory at once: the tree already lists each project under the name just written
            ed.apply()
            if (before != null) {
                try {
                    follow(context, named, before)
                } catch (e: Exception) {
                    Log.w(TAG, "rows did not follow a client's name: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
                }
            }
        }
        taken
    }
}
