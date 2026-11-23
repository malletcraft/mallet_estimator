package com.malletcrafts.interiorestimate

import android.content.Context
import com.malletcrafts.interiorestimate.pano.KeyBook
import com.malletcrafts.interiorestimate.pano.TourDemo
import com.malletcrafts.interiorestimate.pano.TreeKeep
import org.json.JSONObject

/**
 * THE S5 KEY OF EACH PROJECT THIS PHONE HAS SHOT, REMEMBERED (port 63, the review of F4, 23 Nov 2026;
 * android/docs/port63/DESIGN-tree.md §10).
 *
 * A project made on a phone is S5's under a key the phone mints from its names (S5Auth.localKey), and until this the
 * key was kept nowhere: it was worked out again from the names whenever it was wanted. A project moved to another
 * site, or whose site was renamed, then had names that no longer gave its key -- and once its captures had left the
 * phone nothing tied it to its client either (S5 holds no client for a project made on a phone), so its next capture
 * minted a SECOND S5 project and the first was listed under "(no client)".
 *
 * So the key is kept here from the moment it exists, both ways round (the rule is :pano's, KeyBook):
 *  - a client's project title -> its S5 project id, read before a key is ever minted (S5Auth.keyFor) and wherever the
 *    tree asks what a row made on this phone is called on S5 (Catalogue);
 *  - an S5 project id -> the client and the title it is for, read when S5's list names a project with no client and no
 *    capture of it is on this phone (S5Masters.fetch): the phone's only copy of that client's name once the photos
 *    are gone.
 *
 * WHAT FEEDS IT: the mint itself ([remember]); every capture row that carries a project id, read oldest first
 * ([learn] -- called wherever the captures are in hand, so the keys minted before this file existed are in it from the
 * first run); and a local rename of a row that was never sent, which takes its entries with it ([renamed]).
 *
 * WHAT IS REMOVED: one key, by the one delete that proved S5 no longer lists it ([forget]; Deletes.route). Nothing
 * else: not sign-out (the keys are not a person's -- S5Sites keeps its names the same way), not a read of S5's list.
 *
 * Two files, because each is keyed by free text (an S5 project id; a folded client and title): no key in either is
 * safe to reserve, so the two maps cannot share one.
 */
object S5Keys {

    /** S5 project id -> {"client", "title"}: the names the id is for. */
    private const val IDS = "s5_keys" // text:value a prefs file name, never shown
    /** Folded client and title (KeyBook.fold) -> S5 project id. */
    private const val NAMES = "s5_key_names" // text:value a prefs file name, never shown

    /** Guards every read-change-write: a sync's worker and the screen may both learn at once. */
    private val LOCK = Any()

    private fun ids(context: Context) = context.getSharedPreferences(IDS, Context.MODE_PRIVATE)
    private fun names(context: Context) = context.getSharedPreferences(NAMES, Context.MODE_PRIVATE)

    private fun decode(raw: Any?): KeyBook.Names? {
        val text = raw as? String ?: return null
        val o = runCatching { JSONObject(text) }.getOrNull() ?: return null
        return KeyBook.Names(o.optString("client"), o.optString("title")) // text:value the stored field names
    }

    private fun encode(n: KeyBook.Names): String =
        JSONObject().put("client", n.client).put("title", n.title).toString() // text:value the stored field names

    /** What the phone remembers now. One read for every question a caller has. */
    fun book(context: Context): KeyBook.Book = synchronized(LOCK) { read(context) }

    private fun read(context: Context): KeyBook.Book {
        val byId = LinkedHashMap<String, KeyBook.Names>()
        for ((id, raw) in ids(context).all) decode(raw)?.let { byId[id] = it }
        val byName = LinkedHashMap<String, String>()
        for ((name, raw) in names(context).all) (raw as? String)?.takeIf { it.isNotBlank() }?.let { byName[name] = it }
        return KeyBook.Book(byName, byId)
    }

    /** Writes [now] where it differs from [was]: only the entries that changed, so nothing is cleared on the way. */
    private fun write(context: Context, was: KeyBook.Book, now: KeyBook.Book) {
        if (now.byId != was.byId) {
            val ed = ids(context).edit()
            for (id in was.byId.keys) if (id !in now.byId) ed.remove(id)
            for ((id, n) in now.byId) if (was.byId[id] != n) ed.putString(id, encode(n))
            ed.apply()
        }
        if (now.byName != was.byName) {
            val ed = names(context).edit()
            for (name in was.byName.keys) if (name !in now.byName) ed.remove(name)
            for ((name, id) in now.byName) if (was.byName[name] != id) ed.putString(name, id)
            ed.apply()
        }
    }

    private fun change(context: Context, how: (KeyBook.Book) -> KeyBook.Book): KeyBook.Book = synchronized(LOCK) {
        val was = read(context)
        val now = how(was)
        write(context, was, now)
        now
    }

    /**
     * EVERY CAPTURE ROW THAT CARRIES A PROJECT ID IS READ INTO THE BOOK, oldest first (KeyBook.learn), and the book as
     * it then stands is the answer. Never the demo's captures. Where two ids are claimed for one client and title, the
     * one S5's list has named here is kept (KeyBook.wins; "named" is S5Sites holding it). Called wherever the captures
     * are already in hand; it writes only what changed, so a second call costs a read.
     */
    fun learn(context: Context, captures: List<CaptureStore.Capture>): KeyBook.Book {
        val caps = captures
            .filter { it.project.isNotBlank() && !TourDemo.isDemoProject(it.customerName, it.projectTitle) }
            .sortedBy { it.createdAt }
            .map { TreeKeep.Cap(it.project, it.customerName, it.projectTitle, sent = it.state == "SYNCED") } // text:value the store's state, a wire value
        if (caps.isEmpty()) return book(context)
        val held = S5Sites.all(context)
        return change(context) { b -> KeyBook.learn(b, caps) { id -> held.containsKey(id) } }
    }

    /** A key was just minted for [client]'s [title] (S5Auth.keyFor): it is that project's from now on. */
    fun remember(context: Context, project: String, client: String, title: String): KeyBook.Book =
        change(context) { b -> KeyBook.take(b, project, client, title) }

    /** A row made on this phone and never sent was renamed (MainActivity.renameHere): its entries follow (KeyBook.renamed). */
    fun renamed(context: Context, kind: String, client: String, title: String, to: String): KeyBook.Book =
        change(context) { b -> KeyBook.renamed(b, kind, client, title, to) }

    /** S5's list, read for one delete, no longer names [project]: that key, and only it, is forgotten. */
    fun forget(context: Context, project: String): KeyBook.Book =
        change(context) { b -> KeyBook.forget(b, project) }
}
