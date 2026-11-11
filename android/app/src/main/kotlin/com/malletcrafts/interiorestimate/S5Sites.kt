package com.malletcrafts.interiorestimate

import android.content.Context
import com.malletcrafts.interiorestimate.pano.Renames
import org.json.JSONObject

/**
 * S5'S NAME FOR EACH PROJECT'S SITE, AS THIS PHONE LAST HEARD IT (port 62 slice A2, 11 Nov 2026).
 *
 * Amit, 11 Nov 2026: "Yes — S5 keeps the site name". S5 stores the name on each project (its slice A1) and is the
 * one source of it, so the phone keeps S5's answer in ONE place, per S5 project id: the name ('' = S5 lists the
 * project and holds no name for it) and S5's own stamp, `site_at`. A project with no entry here is one S5's list
 * has not named on this phone -- only on the phone, or read from an S5 that does not serve site names.
 *
 * WHY NOT INSIDE THE MASTERS. The masters file is replaced whole by every ERP bootstrap a phone with a live pair
 * makes, and its `sites` rows and `site_name`s are the bench's, which is deliberately not told of a rename. A name
 * kept there would be undone by the next bootstrap, and the old name would be back as a second site (the port 62
 * plan critic, finding 2). Kept here, no bootstrap reaches it; the tree reads it beside the masters
 * (Catalogue.erpProjects, SiteRows.nameOf) and lists the bench's rows under it (SiteRows.listed).
 *
 * Three things feed it: S5's list wherever it is read (S5Masters.fetch; QuoteStageSync.refresh, which runs in both
 * sync legs), and S5's answer to every `PUT …/site` (SiteRename: a rename, the seed, and a 409's name as stored).
 * Each write goes by S5's own stamp, never by arrival ([Renames.takes]): reads overlap, and a list read that went
 * out before a rename must not put the old name back when it lands after it. Nothing is removed: only staff sign in
 * on the phone, staff see every project, and S5 has no project delete.
 *
 * It is NOT cleared at sign-out. A site S5 holds stays S5's while nobody is signed in -- the rename dialog then says
 * so and refuses (refuse.site.signed_out) instead of renaming a local copy -- and the names are not a person's: the
 * projects only S5 listed for the person who signed out leave the masters (S5Masters.forget), so nothing reads theirs.
 */
object S5Sites {

    private const val PREFS = "s5_sites"

    /** Guards every read-compare-write, so two answers landing together are each judged against the other. */
    private val LOCK = Any()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun decode(raw: Any?): Renames.Held? {
        val text = raw as? String ?: return null
        val o = runCatching { JSONObject(text) }.getOrNull() ?: return null
        return Renames.Held(o.optString("site"), o.optString("at"))
    }

    private fun encode(h: Renames.Held): String = JSONObject().put("site", h.site).put("at", h.at).toString()

    /** S5's name for [project], or null when S5's list has not named the project on this phone. */
    fun held(context: Context, project: String): Renames.Held? =
        if (project.isBlank()) null else decode(prefs(context).getString(project, null))

    /** Every project S5 has named here, by S5 project id: one read for the whole tree. */
    fun all(context: Context): Map<String, Renames.Held> {
        val out = HashMap<String, Renames.Held>()
        for ((project, raw) in prefs(context).all) decode(raw)?.let { out[project] = it }
        return out
    }

    /** One answer of S5's about [project]. Returns whether it was taken (it is not older than what is held). */
    fun take(context: Context, project: String, copy: Renames.Held): Boolean = takeList(context, listOf(project to copy)) > 0

    /** The rows of one read of S5's list (Renames.listedOf), each taken only where it is not older than what is held. Returns how many were. */
    fun takeList(context: Context, rows: List<Pair<String, Renames.Held>>): Int = synchronized(LOCK) {
        val p = prefs(context)
        val ed = p.edit()
        var taken = 0
        for ((project, copy) in rows) {
            if (project.isBlank()) continue
            if (!Renames.takes(decode(p.getString(project, null)), copy)) continue
            ed.putString(project, encode(copy))
            taken++
        }
        if (taken > 0) ed.apply()
        taken
    }
}
