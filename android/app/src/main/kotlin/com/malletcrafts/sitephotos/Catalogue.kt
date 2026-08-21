package com.malletcrafts.sitephotos

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * The folder tree the app browses: client → SITE → project → room.
 *
 * The site level is the one ERPNext has nothing native behind — a Project
 * links straight to a Customer — and it exists because people own more than
 * one flat. A project belongs to a building, not to a person.
 *
 * Two sources, one list. ERP's `bootstrap` supplies the real clients, sites,
 * projects and the Estimate Room master; anything typed on site while there
 * was no signal is held here until `ensure_site` can turn it into masters. A
 * technician standing in an empty flat should never be blocked because the
 * office has not created the lead yet, and should never be shown two versions
 * of the same client because they were.
 *
 * Rooms are deliberately NOT invented per project. They come from the same
 * Estimate Room master the SKU codes use, so a photo files itself beside
 * YS_MB_WAR instead of beside a free-text "master bedrm" typed at 7pm.
 */
class Catalogue(context: Context) {

    private val prefs = context.getSharedPreferences("catalogue", Context.MODE_PRIVATE)

    data class Site(
        val client: String,
        val name: String,
        /** ERP's Mallet Site docname. Empty until this site syncs. */
        val serverId: String,
        val type: String,
        val city: String,
        val local: Boolean,
    ) {
        val synced: Boolean get() = serverId.isNotBlank()
    }

    data class Project(
        val client: String,
        val site: String,
        val siteId: String,
        val title: String,
        /** ERP's Project name (PROJ-0004). Empty until this site syncs. */
        val serverId: String,
        val jobType: String,
        val stage: String,
        val local: Boolean,
        /** ERPNext's Project.status — Open, Completed, Cancelled. */
        val status: String = "",
        val start: String = "",
        val end: String = "",
        /** When the project last moved stage, as the server recorded it. */
        val stageSince: String = "",
    ) {
        val synced: Boolean get() = serverId.isNotBlank()
        /** Stable across a sync only for ERP rows; local rows key on names. */
        val key: String get() = if (synced) serverId else keyOf(client, site, title)

        /** "12 Aug → 30 Sep", or one end of it, or nothing. A range with one
         *  date missing still says more than no range at all. */
        val dateRange: String get() = when {
            start.isNotBlank() && end.isNotBlank() -> "${Catalogue.day(start)} → ${Catalogue.day(end)}"
            start.isNotBlank() -> "from ${Catalogue.day(start)}"
            end.isNotBlank() -> "due ${Catalogue.day(end)}"
            else -> ""
        }

        /** The word on the pill. A project that never reached ERP says so
         *  first — that is the fact that changes what you do next. */
        val statusLabel: String get() = when {
            local -> "offline"
            status.equals("Completed", true) -> "done"
            status.equals("Cancelled", true) -> "cancelled"
            status.isBlank() -> ""
            else -> "active"
        }

        /** Amber, not grey: offline and cancelled are both things somebody
         *  has to act on, and an active project needs no colour at all. */
        val statusWarn: Boolean get() = local || status.equals("Cancelled", true)
    }

    data class Stage(
        val name: String,
        val phase: String,
        val sequence: Int,
        val jobTypes: List<String>,
    )

    data class Article(val code: String, val name: String, val jobTypes: List<String>)

    data class Sku(val name: String, val code: String, val room: String, val article: String)

    // ---- what was typed on site, before ERP knew about it ---------------

    private data class Local(
        val client: String, val site: String, val project: String,
        val siteType: String, val jobType: String,
    )

    private fun locals(): List<Local> {
        val raw = prefs.getString("local_sites", "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val c = o.optString("client").trim()
            val p = o.optString("project").trim()
            if (c.isEmpty() || p.isEmpty()) null
            else Local(c,
                o.optString("site").trim().ifEmpty { DEFAULT_SITE },
                p,
                o.optString("site_type").ifEmpty { "Flat" },
                o.optString("job_type").ifEmpty { JOB_NEW })
        }
    }

    private fun writeLocals(list: List<Local>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject()
                .put("client", it.client).put("site", it.site)
                .put("project", it.project).put("site_type", it.siteType)
                .put("job_type", it.jobType))
        }
        prefs.edit().putString("local_sites", arr.toString()).apply()
    }

    /** Remember a site typed on site. Idempotent, and case-insensitive so a
     *  second visit does not create a second folder. */
    fun addLocal(client: String, site: String, project: String,
                 siteType: String = "Flat", jobType: String = JOB_NEW) {
        val c = client.trim()
        val s = site.trim().ifEmpty { DEFAULT_SITE }
        val p = project.trim()
        if (c.isEmpty() || p.isEmpty()) return
        val have = locals()
        if (have.any { same(it.client, c) && same(it.site, s) && same(it.project, p) }) return
        writeLocals(have + Local(c, s, p, siteType, jobType))
    }

    /** Once ERP has the site, its local copy is redundant — dropping it is
     *  what stops the same folder appearing twice after a sync. */
    fun forgetLocal(client: String, site: String, project: String) {
        writeLocals(locals().filterNot {
            same(it.client, client) && same(it.site, site) && same(it.project, project)
        })
    }

    fun pendingCount(): Int = locals().size

    /** What was typed for this client and project while there was no signal.
     *
     *  The site is looked up here rather than stored on the capture row on
     *  purpose: the captures table has no migration path (onUpgrade is a
     *  no-op), so adding a column would take out every phone that already has
     *  a queue on it. The catalogue is a preference blob and can grow freely. */
    fun localSiteFor(client: String, project: String): String =
        locals().firstOrNull { same(it.client, client) && same(it.project, project) }
            ?.site ?: ""

    fun localJobTypeFor(client: String, project: String): String =
        locals().firstOrNull { same(it.client, client) && same(it.project, project) }
            ?.jobType ?: ""

    // ---- the merged tree -------------------------------------------------

    private fun erpProjects(masters: JSONObject?): List<Project> {
        val arr = masters?.optJSONArray("projects") ?: return emptyList()
        val out = mutableListOf<Project>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val client = o.optString("customer_name").ifBlank { o.optString("customer") }
            val title = o.optString("title").ifBlank { o.optString("project") }
            if (client.isBlank() || title.isBlank()) continue
            out.add(Project(
                client = client,
                // A project the office made before the site level existed has
                // no site. It is filed under one named bucket rather than
                // dropped: an invisible project is worse than an awkward folder.
                site = o.optString("site_name").ifBlank { NO_SITE },
                siteId = o.optString("site"),
                title = title,
                serverId = o.optString("project"),
                jobType = o.optString("job_type").ifBlank { JOB_NEW },
                stage = o.optString("stage"),
                local = false,
                status = o.optString("status"),
                start = o.optString("start"),
                end = o.optString("end"),
                stageSince = o.optString("stage_since")))
        }
        return out
    }

    fun projects(masters: JSONObject?): List<Project> {
        val erp = erpProjects(masters)
        val seen = erp.map { keyOf(it.client, it.site, it.title) }.toMutableSet()
        // A local row also loses to an ERP project of the same name at ANY
        // site: the office's record beats the site's memory of where it was.
        val erpTitles = erp.map { keyOf(it.client, "", it.title) }.toSet()
        val out = erp.toMutableList()
        for (l in locals()) {
            if (keyOf(l.client, l.site, l.project) in seen) continue
            if (keyOf(l.client, "", l.project) in erpTitles) continue
            out.add(Project(l.client, l.site, "", l.project, "", l.jobType, "", local = true))
            seen.add(keyOf(l.client, l.site, l.project))
        }
        return out.sortedWith(compareBy({ it.client.lowercase() },
            { siteOrder(it.site) }, { it.site.lowercase() }, { it.title.lowercase() }))
    }

    fun clients(masters: JSONObject?): List<String> =
        projects(masters).map { it.client }.distinctBy { it.lowercase() }.sorted()

    fun sitesOf(masters: JSONObject?, client: String): List<Site> {
        val mine = projects(masters).filter { same(it.client, client) }
        val typeOf = locals().associate { key(it.site) to it.siteType }
        val grouped = LinkedHashMap<String, MutableList<Project>>()
        for (p in mine) grouped.getOrPut(key(p.site)) { mutableListOf() }.add(p)
        return grouped.map { (k, ps) ->
            Site(
                client = client,
                name = ps.first().site,
                serverId = ps.firstOrNull { it.siteId.isNotBlank() }?.siteId ?: "",
                type = typeOf[k] ?: "",
                city = "",
                // A site shows the offline badge when ANYTHING under it has
                // not reached ERP. A folder that says synced while one project
                // inside it never left the phone is the lie that loses a
                // capture, so the badge is pessimistic on purpose.
                local = ps.any { it.local })
        }.sortedWith(compareBy({ siteOrder(it.name) }, { it.name.lowercase() }))
    }

    fun projectsOf(masters: JSONObject?, client: String, site: String): List<Project> =
        projects(masters).filter { same(it.client, client) && same(it.site, site) }

    /** The room master, with a usable fallback: a phone that has never been
     *  online still has to be able to file a capture somewhere sensible. */
    fun rooms(masters: JSONObject?): List<String> {
        val arr = masters?.optJSONArray("rooms")
        if (arr != null && arr.length() > 0) {
            return (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        }
        return FALLBACK_ROOMS
    }

    // ---- stages and articles --------------------------------------------

    /** The work-stage master, in trade order, narrowed to one job type.
     *  Thirty-nine stages is a picker; it is not a filter row, so the phase
     *  is what the capture list filters by. */
    fun stages(masters: JSONObject?, jobType: String? = null): List<Stage> {
        val arr = masters?.optJSONArray("stages") ?: return emptyList()
        val out = mutableListOf<Stage>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val jobs = splitJobs(o.optString("job_types"))
            if (jobType != null && jobType !in jobs) continue
            out.add(Stage(o.optString("stage"), o.optString("phase"),
                o.optInt("sequence"), jobs))
        }
        return out.sortedBy { it.sequence }
    }

    fun phases(masters: JSONObject?, jobType: String? = null): List<String> =
        stages(masters, jobType).map { it.phase }.distinct()

    /** The phase a work stage belongs to, or "" when the word is not one of
     *  the thirty-nine — which is exactly what a capture taken before the
     *  master existed carries, and why this returns blank rather than
     *  throwing. Its caller falls back to the stored word. */
    fun phaseOfStage(masters: JSONObject?, stage: String): String {
        if (stage.isBlank()) return ""
        return stages(masters).firstOrNull { it.name.equals(stage, true) }?.phase ?: ""
    }

    /** True when this word IS one of the thirty-nine, so it can be sent as
     *  work_stage rather than as a bare phase. */
    fun isWorkStage(masters: JSONObject?, stage: String): Boolean =
        stage.isNotBlank() && stages(masters).any { it.name.equals(stage, true) }

    fun articles(masters: JSONObject?, jobType: String? = null): List<Article> {
        val arr = masters?.optJSONArray("articles") ?: return emptyList()
        val out = mutableListOf<Article>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val jobs = splitJobs(o.optString("job_types"))
            if (jobType != null && jobType !in jobs) continue
            out.add(Article(o.optString("code"), o.optString("article"), jobs))
        }
        return out.sortedBy { it.code }
    }

    /** The SKUs a capture on this project may be tagged to. They ride
     *  bootstrap so a technician in a basement can still tag a photo. */
    fun skusOf(masters: JSONObject?, projectServerId: String): List<Sku> {
        if (projectServerId.isBlank()) return emptyList()
        val arr = masters?.optJSONArray("projects") ?: return emptyList()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optString("project") != projectServerId) continue
            val skus = o.optJSONArray("skus") ?: return emptyList()
            return (0 until skus.length()).mapNotNull { j ->
                val s = skus.optJSONObject(j) ?: return@mapNotNull null
                Sku(s.optString("name"), s.optString("code"),
                    s.optString("room"), s.optString("article"))
            }
        }
        return emptyList()
    }

    fun jobTypes(masters: JSONObject?): List<String> {
        val arr = masters?.optJSONArray("job_types") ?: return listOf(JOB_NEW, JOB_REPAIR, JOB_INSTALL)
        val out = (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        return out.ifEmpty { listOf(JOB_NEW, JOB_REPAIR, JOB_INSTALL) }
    }

    // ---- naming ----------------------------------------------------------

    private fun key(s: String) = s.trim().lowercase().replace(Regex("[\\s_]+"), " ")
    private fun same(a: String, b: String) = key(a) == key(b)
    /** '(no site)' sorts last: it is a bucket, not a place, and a real site
     *  should never be pushed below it. */
    private fun siteOrder(name: String) = if (name == NO_SITE) 1 else 0

    private fun splitJobs(raw: String) =
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    companion object {
        const val DEFAULT_SITE = "Main site"
        const val NO_SITE = "(no site)"
        const val JOB_NEW = "New work"
        const val JOB_REPAIR = "Repair"
        const val JOB_INSTALL = "Supply & install"

        private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        /** "2026-08-12" → "12 Aug". ISO dates are for machines; a date on a
         *  phone row is read at a glance and the year is almost always this
         *  one. Anything that is not an ISO date is passed through untouched
         *  rather than mangled. */
        fun day(iso: String): String {
            val p = iso.split("-")
            if (p.size != 3) return iso
            val m = p[1].toIntOrNull() ?: return iso
            val d = p[2].toIntOrNull() ?: return iso
            if (m !in 1..12) return iso
            return "$d ${MONTHS[m - 1]}"
        }

        fun keyOf(client: String, site: String, project: String) =
            listOf(client, site, project)
                .joinToString("|") { it.trim().lowercase().replace(Regex("[\\s_]+"), " ") }

        /** Only used before the phone has ever reached ERP. These are the
         *  rooms the house already names in SKU codes. */
        val FALLBACK_ROOMS = listOf(
            "Master Bedroom", "Kids Bedroom", "Guest Bedroom", "Living Room",
            "Dining", "Kitchen", "Foyer", "Passage", "Balcony", "Study",
            "Utility", "Toilet", "Pooja",
        )
    }
}
