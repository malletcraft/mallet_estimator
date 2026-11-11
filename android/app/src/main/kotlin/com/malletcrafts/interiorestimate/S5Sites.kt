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
 * sync legs; SiteRename.hear, at app start on a phone that still holds an ERP pair), and S5's answer to every
 * `PUT …/site` (SiteRename: a rename, the seed, and a 409's name as stored).
 * Each write goes by S5's own stamp, never by arrival ([Renames.takes]): reads overlap, and a list read that went
 * out before a rename must not put the old name back when it lands after it. Nothing is removed: only staff sign in
 * on the phone, staff see every project, and S5 has no project delete.
 *
 * "NO ENTRY" IS NOT "S5 DOES NOT HOLD IT" UNTIL A LIST READ HAS LANDED (the A2 review, finding 1). This file starts
 * empty on the update that brings it, and stays empty until S5's list is read -- so [heard] records that one has
 * been ([takeRead], which the list reads call; an answer to a PUT is not a read of the list). Until then the rename
 * dialog reads S5's list before it routes a site that may be on S5 (Renames.route's `heard`), instead of calling it
 * "not held" or renaming it on the phone alone. The record is kept in a file of its own: this one is keyed by S5's
 * project ids, which are free text, so no key in it is safe to reserve.
 *
 * THE SITE'S CHOSEN ROOMS FOLLOW THE NAME HERE (audit R7; the A2 review, finding 2). The tree lists a project under
 * S5's name the moment this file holds it, and the rooms a site has are kept under the site's name -- so the one
 * place every name is written, [takeList], is where they follow: this phone's own Save, another phone's rename
 * heard by a list read, a 409's name as stored and the one project of two that took the name alike
 * (Renames.scopeSteps decides, Catalogue.followS5Names applies).
 *
 * It is NOT cleared at sign-out. A site S5 holds stays S5's while nobody is signed in -- the rename dialog then says
 * so and refuses (refuse.site.signed_out) instead of renaming a local copy -- and the names are not a person's: the
 * projects only S5 listed for the person who signed out leave the masters (S5Masters.forget), so nothing reads theirs.
 */
object S5Sites {

    private const val PREFS = "s5_sites"

    /** Where it is recorded that a read of S5's list has landed: its own file, since [PREFS] is keyed by free-text project ids. */
    private const val READS = "s5_sites_reads"
    private const val HEARD = "heard"

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

    /**
     * Whether a read of S5's list has landed on this phone since it keeps S5's site names. False -- the update that
     * brought this file, until the first read -- a project with no entry is NOT KNOWN either way, and the rename
     * dialog reads the list before it routes (Renames.route's `heard`). Never cleared, as the names are not.
     */
    fun heard(context: Context): Boolean =
        context.getSharedPreferences(READS, Context.MODE_PRIVATE).getBoolean(HEARD, false)

    /** One answer of S5's about [project]. Returns whether it was taken (it is not older than what is held). */
    fun take(context: Context, project: String, copy: Renames.Held): Boolean = takeList(context, listOf(project to copy)) > 0

    /**
     * The rows of one LANDED read of S5's list (S5Masters.fetch, QuoteStageSync.refresh, SiteRename.hear): taken as
     * [takeList] takes them, and the read is recorded ([heard]) -- whatever it named, none included: a list that
     * names no project, or comes from an S5 that serves no site names yet, is still S5's answer that it holds no
     * name to rename against. Returns how many rows were taken.
     */
    fun takeRead(context: Context, rows: List<Pair<String, Renames.Held>>): Int {
        val taken = takeList(context, rows)
        val reads = context.getSharedPreferences(READS, Context.MODE_PRIVATE)
        if (!reads.getBoolean(HEARD, false)) reads.edit().putBoolean(HEARD, true).apply()
        return taken
    }

    /**
     * Rows of S5's (Renames.listedOf, or one answer), each taken only where it is not older than what is held.
     * Returns how many were. Where a taken row CHANGES a project's name, the chosen rooms of the site the tree
     * listed that project under follow it to the site it is listed under now (see the header): the tree is read
     * just before and just after the names are written, and Renames.scopeSteps says what moves. That step never
     * costs S5's name -- the name is written whatever the step does.
     */
    fun takeList(context: Context, rows: List<Pair<String, Renames.Held>>): Int = synchronized(LOCK) {
        val p = prefs(context)
        val ed = p.edit()
        var taken = 0
        val moved = HashSet<String>()
        for ((project, copy) in rows) {
            if (project.isBlank()) continue
            val stored = decode(p.getString(project, null))
            if (!Renames.takes(stored, copy)) continue
            // a first hearing with no name ('' where nothing was held) changes no name
            if (stored?.site.orEmpty() != copy.site) moved.add(project)
            ed.putString(project, encode(copy))
            taken++
        }
        if (taken > 0) {
            val cat = if (moved.isEmpty()) null else runCatching { Catalogue(context) }.getOrNull()
            val masters = if (cat == null) null else CaptureStore(context).masters()
            val before = if (cat == null) null else runCatching { cat.placed(masters) }.getOrNull()
            // apply() is in memory at once: the tree read below already sees the names just written
            ed.apply()
            if (cat != null && before != null) runCatching {
                cat.followS5Names(Renames.scopeSteps(before, cat.placed(masters), moved))
            }
        }
        taken
    }
}
