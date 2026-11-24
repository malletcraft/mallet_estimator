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
 * Fed wherever [S5Sites] is fed -- S5's list at every read of it ([takeRead]: S5Masters.fetch, QuoteStageSync.refresh,
 * SiteRename.hear) and S5's answer to every `PUT …/client` ([take]: ClientRename's rename, the backfill, a 409's name
 * as stored) -- and each write goes by S5's own stamp, never by arrival ([Renames.takes]).
 *
 * THE PHONE'S ROWS FOLLOW THE NAME HERE. A client's name is not only shown: the phone's capture rows, the project
 * rows made on it and the S5 keys it remembers are FILED under it. So [takeList], the one place a name is written,
 * is also where those rows move to it (ClientNames.steps says what moves; Catalogue.followClient, CaptureStore.setNames
 * and S5Keys.remember apply it): this phone's own Save, another phone's rename heard by a list read, a 409's name as
 * stored and each project of "1 of 2" alike. It is asked for EVERY named row of a read, not only a changed one, and
 * moves nothing where the rows already agree -- so a step that failed once is made good by the next read.
 *
 * THE NAMES ARE STAFF'S, AND ONE LOGIN'S (the review of slice 5, fault 3; DESIGN-tree.md §11.6). Unlike a site's
 * name, which S5 gives every login it admits, a client's name is given to MCFT staff alone -- and this file used to
 * outlive the sign-out, so the next person on the phone, or nobody, was shown every name the last one had read, and
 * the tree's rows followed them. Now the file carries a mark of the login that read it (ClientNames.owner, kept in a
 * file of its own because a project id is free text) and:
 *
 *  - [held] and [all] give NOTHING to any other login, or to a signed-out phone (ClientNames.shown) -- so the tree
 *    shows none of it and the backfill, which sends only for a project S5 gave a key for, sends nothing;
 *  - the first answer taken for another login empties the file first, and so does a read of S5's list that carries
 *    a client-name key on NO project: that login is not staff (ClientNames.forgets);
 *  - sign-out empties it at once ([forgetAll]: S5Session.signOut, and FrappeClient.forget on a phone with no S5 sign-in).
 *
 * One entry still goes with the delete that proved S5 no longer lists its project ([forget]).
 */
object S5Clients {

    private const val TAG = "S5Clients"
    private const val PREFS = "s5_clients"

    /** The mark of the login the names were read by, in a file of its own: a project id is free text, and no id may ever be the mark's key. */
    private const val OWNER_PREFS = "s5_clients_owner"
    private const val OWNER = "owner"

    /** Guards every read-compare-write, so two answers landing together are each judged against the other. */
    private val LOCK = Any()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun marks(context: Context) = context.getSharedPreferences(OWNER_PREFS, Context.MODE_PRIVATE)

    /**
     * The login S5 is called with NOW, as ClientNames.owner marks it: the S5 session's while this phone is signed in
     * to S5, else the legacy ERP key's (the key alone -- `token <key>:<secret>` cut at the colon, never the secret);
     * '' when it holds neither. The order S5Auth.current picks its credentials in.
     */
    private fun ownerNow(context: Context): String = ClientNames.owner(
        if (S5Session.signedIn(context)) S5Session.loginKey(context) else null,
        FrappeClient.authorization(context)?.substringAfter(' ')?.substringBefore(':'))

    private fun ownerHeld(context: Context): String? = marks(context).getString(OWNER, null)

    private fun decode(raw: Any?): Renames.Held? {
        val text = raw as? String ?: return null
        val o = runCatching { JSONObject(text) }.getOrNull() ?: return null
        return Renames.Held(o.optString("name"), o.optString("at"))
    }

    private fun encode(h: Renames.Held): String = JSONObject().put("name", h.site).put("at", h.at).toString()

    /** S5's name for [project]'s client ('' = none held), or null when S5 has given this phone no client-name key for the project. */
    fun held(context: Context, project: String): Renames.Held? =
        if (project.isBlank()) null
        // only to the login that read it (the review of slice 5, fault 3): nobody else is shown it, and nothing is sent on it
        else ClientNames.shown(ownerHeld(context), ownerNow(context), decode(prefs(context).getString(project, null)))

    /** Every project S5 has given a client-name key for here, by S5 project id: one read for the whole tree. */
    fun all(context: Context): Map<String, Renames.Held> {
        val out = HashMap<String, Renames.Held>()
        // another login's, or a signed-out phone: none of it (ClientNames.kept)
        if (!ClientNames.kept(ownerHeld(context), ownerNow(context))) return out
        for ((project, raw) in prefs(context).all) decode(raw)?.let { out[project] = it }
        return out
    }

    /** S5's list, read for one delete, no longer names [project]: its entry goes, and only it (beside S5Sites.forget). */
    fun forget(context: Context, project: String) {
        if (project.isBlank()) return
        synchronized(LOCK) { prefs(context).edit().remove(project).apply() }
    }

    /**
     * SIGN-OUT (S5Session.signOut; FrappeClient.forget where the legacy key was the login): every name goes, and
     * the mark with it. The names are the signed-in staff member's to read; the next person on this phone starts
     * with none, and S5 gives them back at the first read to a login it gives them to.
     */
    fun forgetAll(context: Context) {
        synchronized(LOCK) {
            prefs(context).edit().clear().apply()
            marks(context).edit().clear().apply()
        }
    }

    /**
     * BEFORE AN ANSWER IS TAKEN: what is held is made the present login's. It is emptied where ClientNames.forgets
     * says so -- held for another login, or S5's list was read ([listed] projects) and carried a client-name key on
     * none of them ([keyed] 0) -- and then marked as the present login's. False when there is nothing to keep: no
     * login (an answer that landed after the sign-out), or no row with a key.
     */
    private fun claim(context: Context, listed: Int, keyed: Int): Boolean {
        val now = ownerNow(context)
        if (ClientNames.forgets(ownerHeld(context), now, listed, keyed)) {
            prefs(context).edit().clear().apply()
            marks(context).edit().clear().apply()
        }
        if (now.isEmpty() || keyed == 0) return false
        if (ownerHeld(context) != now) marks(context).edit().putString(OWNER, now).apply()
        return true
    }

    /** One answer of S5's about [project]'s client -- a `PUT …/client`'s, which S5 gives staff alone. Returns whether it was taken (it is not older than what is held). */
    fun take(context: Context, project: String, copy: Renames.Held): Boolean = synchronized(LOCK) {
        claim(context, listed = 1, keyed = 1) && takeList(context, listOf(project to copy)) > 0
    }

    /**
     * ONE READ OF S5'S LIST: [rows] are its rows that carry a client-name key (ClientNames.listedOf), of [listed]
     * projects in all. A read that names projects and gives the key on none of them is a login that is not staff:
     * everything held is forgotten and nothing is taken ([claim]). Returns how many rows were taken.
     */
    fun takeRead(context: Context, rows: List<Pair<String, Renames.Held>>, listed: Int): Int = synchronized(LOCK) {
        if (claim(context, listed, rows.size)) takeList(context, rows) else 0
    }

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
     * Returns how many were. Reached through [takeRead] and [take] alone, which first make what is held the present
     * login's. For every taken row that NAMES a client, the phone's own rows of that project follow
     * the name (see the header): what they are filed under is read just before the names are written, and
     * ClientNames.steps says what moves. That step never costs S5's name -- the name is written whatever the step does.
     */
    private fun takeList(context: Context, rows: List<Pair<String, Renames.Held>>): Int = synchronized(LOCK) {
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
