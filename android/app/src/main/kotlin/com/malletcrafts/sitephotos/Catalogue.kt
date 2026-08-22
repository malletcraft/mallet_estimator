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
        /** Free text, keyed on site or at the desk. Blank until someone types one. */
        val address: String = "",
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
        /** ERP's Customer docname. Needed to rename the client, and already
         *  in the bootstrap payload — it was simply never read. */
        val clientId: String = "",
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

    data class Article(
        val code: String,
        val name: String,
        val jobTypes: List<String>,
        /** Build / Install / Subcontract — who does the work. */
        val kind: String = KIND_BUILD,
        /** Sqft / Rft / Point / Nos / Lumpsum — the unit it is QUOTED in,
         *  and therefore the unit the site is asked for. */
        val basis: String = BASIS_NOS,
    ) {
        /** A wardrobe has three dimensions; POP has an area and no shape. */
        val wantsDimensions: Boolean get() = kind == KIND_BUILD
        /** Lumpsum is a deal, not a measurement — asking for a number would
         *  invite somebody to invent one. */
        val wantsQuantity: Boolean get() = basis != BASIS_LUMPSUM
    }

    data class Sku(val name: String, val code: String, val room: String,
                   val article: String,
                   /** Recorded on site and not yet in ERP. */
                   val local: Boolean = false)

    // ---- what was typed on site, before ERP knew about it ---------------

    private data class Local(
        val client: String, val site: String, val project: String,
        val siteType: String, val jobType: String,
        /** Typed on site; carried to ERP on the next sync. */
        val address: String = "",
    )

    /** Work the SITE said was needed, before the bench knew about it.
     *
     *  Kept beside the offline sites rather than in the capture database: it
     *  is a small list that syncs and disappears, and the captures table has
     *  no migration path worth spending on a queue that empties itself. */
    data class LocalSku(
        val deviceId: String,
        val client: String,
        val projectTitle: String,
        val projectId: String,
        val room: String,
        val articleCode: String,
        val articleName: String,
        val basis: String,
        val qty: Double?,
        val widthMm: Int?,
        val heightMm: Int?,
        val depthMm: Int?,
        val note: String,
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
                o.optString("job_type").ifEmpty { JOB_NEW },
                o.optString("address"))
        }
    }

    private fun writeLocals(list: List<Local>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject()
                .put("client", it.client).put("site", it.site)
                .put("project", it.project).put("site_type", it.siteType)
                .put("job_type", it.jobType).put("address", it.address))
        }
        prefs.edit().putString("local_sites", arr.toString()).apply()
    }

    /** Remember a site typed on site. Idempotent, and case-insensitive so a
     *  second visit does not create a second folder. */
    fun addLocal(client: String, site: String, project: String,
                 siteType: String = "Flat", jobType: String = JOB_NEW,
                 address: String = "") {
        val c = client.trim()
        val s = site.trim().ifEmpty { DEFAULT_SITE }
        val p = project.trim()
        if (c.isEmpty() || p.isEmpty()) return
        val have = locals()
        // A second visit must not mint a second folder — but it MAY carry an
        // address or a type the first visit did not know, so an existing row
        // has its blanks filled instead of being left alone.
        val at = have.indexOfFirst { same(it.client, c) && same(it.site, s) && same(it.project, p) }
        if (at >= 0) {
            val old = have[at]
            val merged = old.copy(
                siteType = if (siteType.isNotBlank()) siteType else old.siteType,
                address = if (old.address.isBlank()) address else old.address)
            if (merged != old) writeLocals(have.toMutableList().also { it[at] = merged })
            return
        }
        writeLocals(have + Local(c, s, p, siteType, jobType, address))
    }

    /** The address typed for a site that has not reached ERP yet. */
    fun localAddress(site: String): String =
        locals().firstOrNull { same(it.site, site) }?.address ?: ""

    /** The type chosen for a site that has not reached ERP yet. */
    fun localSiteType(site: String): String =
        locals().firstOrNull { same(it.site, site) }?.siteType ?: ""

    /**
     * Correct a name that has not reached ERP yet.
     *
     * A local row is only words in this phone's preferences, so fixing one is
     * a local edit and must work with no signal at all — which is the state
     * the person is usually in when they notice the typo. The ERP-backed case
     * goes through sitephoto.rename_node instead; the caller picks by whether
     * the row still has a local copy.
     *
     * kind: "client" renames it everywhere on this phone, "site" within that
     * client, "project" within that client and site.
     */
    fun renameLocal(kind: String, client: String, site: String, project: String,
                    newName: String): Boolean {
        val n = newName.trim()
        if (n.isEmpty()) return false
        var touched = false
        val next = locals().map { l ->
            when (kind) {
                "client" -> if (same(l.client, client)) {
                    touched = true; l.copy(client = n)
                } else l
                "site" -> if (same(l.client, client) && same(l.site, site)) {
                    touched = true; l.copy(site = n)
                } else l
                "project" -> if (same(l.client, client) && same(l.site, site) &&
                    same(l.project, project)) {
                    touched = true; l.copy(project = n)
                } else l
                else -> l
            }
        }
        if (touched) writeLocals(next)
        return touched
    }

    /** Once ERP has the site, its local copy is redundant — dropping it is
     *  what stops the same folder appearing twice after a sync. */
    fun forgetLocal(client: String, site: String, project: String) {
        writeLocals(locals().filterNot {
            same(it.client, client) && same(it.site, site) && same(it.project, project)
        })
    }

    // ---- work recorded on site ------------------------------------------

    fun localSkus(): List<LocalSku> {
        val raw = prefs.getString("local_skus", "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("device_id").trim()
            if (id.isEmpty()) return@mapNotNull null
            LocalSku(
                deviceId = id,
                client = o.optString("client"),
                projectTitle = o.optString("project_title"),
                projectId = o.optString("project"),
                room = o.optString("room"),
                articleCode = o.optString("article_code"),
                articleName = o.optString("article_name"),
                basis = o.optString("basis").ifEmpty { BASIS_NOS },
                qty = if (o.has("qty")) o.optDouble("qty") else null,
                widthMm = if (o.has("w")) o.optInt("w") else null,
                heightMm = if (o.has("h")) o.optInt("h") else null,
                depthMm = if (o.has("d")) o.optInt("d") else null,
                note = o.optString("note"))
        }
    }

    private fun writeLocalSkus(list: List<LocalSku>) {
        val arr = JSONArray()
        list.forEach { k ->
            val o = JSONObject()
                .put("device_id", k.deviceId).put("client", k.client)
                .put("project_title", k.projectTitle).put("project", k.projectId)
                .put("room", k.room).put("article_code", k.articleCode)
                .put("article_name", k.articleName).put("basis", k.basis)
                .put("note", k.note)
            k.qty?.let { o.put("qty", it) }
            k.widthMm?.let { o.put("w", it) }
            k.heightMm?.let { o.put("h", it) }
            k.depthMm?.let { o.put("d", it) }
            arr.put(o)
        }
        prefs.edit().putString("local_skus", arr.toString()).apply()
    }

    fun addLocalSku(sku: LocalSku) {
        // Keyed on the device id, exactly as the bench is: two wardrobes in
        // one room are two real SKUs, so nothing here may collapse them.
        if (localSkus().any { it.deviceId == sku.deviceId }) return
        writeLocalSkus(localSkus() + sku)
    }

    fun forgetLocalSku(deviceId: String) {
        writeLocalSkus(localSkus().filterNot { it.deviceId == deviceId })
    }

    /** What this project has recorded on site and not yet sent. */
    fun localSkusOf(projectTitle: String): List<LocalSku> =
        localSkus().filter { same(it.projectTitle, projectTitle) }

    /** Offline sites AND offline SKUs — both are work the bench has not seen,
     *  and the Queue badge must never be optimistic about either. */
    fun pendingCount(): Int = locals().size + localSkus().size

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
                clientId = o.optString("customer"),
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
            out.add(Project(l.client, l.site, "", l.project, "",
                clientId = "", jobType = l.jobType, stage = "", local = true))
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
        val addrOf = locals().associate { key(it.site) to it.address }
        val erp = serverSites(masters)
        val grouped = LinkedHashMap<String, MutableList<Project>>()
        for (p in mine) grouped.getOrPut(key(p.site)) { mutableListOf() }.add(p)
        return grouped.map { (k, ps) ->
            Site(
                client = client,
                name = ps.first().site,
                serverId = ps.firstOrNull { it.siteId.isNotBlank() }?.siteId ?: "",
                // ERP first, the phone's own note second: the office's record
                // beats the site's memory of it, which is the same order the
                // sync uses when it refuses to overwrite a filled field.
                type = erp[k]?.optString("site_type").orEmpty().ifEmpty { typeOf[k] ?: "" },
                city = erp[k]?.optString("city").orEmpty(),
                address = erp[k]?.optString("site_address").orEmpty()
                    .ifEmpty { addrOf[k] ?: "" },
                // A site shows the offline badge when ANYTHING under it has
                // not reached ERP. A folder that says synced while one project
                // inside it never left the phone is the lie that loses a
                // capture, so the badge is pessimistic on purpose.
                local = ps.any { it.local })
        }.sortedWith(compareBy({ siteOrder(it.name) }, { it.name.lowercase() }))
    }

    fun projectsOf(masters: JSONObject?, client: String, site: String): List<Project> =
        projects(masters).filter { same(it.client, client) && same(it.site, site) }

    // ---- one read, many questions ---------------------------------------

    /**
     * The whole tree, computed ONCE.
     *
     * Every one of the functions above re-parses the masters JSON and the
     * locals blob from scratch. A screen that asks for the clients, then each
     * client's sites, then each client's project count, pays for that whole
     * parse once per question — and the tree asks a question per row, on
     * every recomposition. Amit, 2026-08-22: "refresh of data is very slow."
     * That is where the time went.
     *
     * A snapshot answers all of it from maps built in a single pass. It is
     * also the honest shape for Compose: an immutable value that can be
     * remembered, so the screen recomposes when the DATA changed rather than
     * whenever something happened to call a function.
     */
    class Snapshot internal constructor(
        val projects: List<Project>,
        val clients: List<String>,
        private val sites: Map<String, List<Site>>,
        private val bySite: Map<String, List<Project>>,
        private val counts: Map<String, Int>,
    ) {
        fun sitesOf(client: String): List<Site> = sites[k(client)] ?: emptyList()
        fun projectsOf(client: String, site: String): List<Project> =
            bySite[k(client) + "\u0000" + k(site)] ?: emptyList()
        fun projectCount(client: String): Int = counts[k(client)] ?: 0
        fun siteCount(client: String): Int = sitesOf(client).size

        private companion object {
            fun k(s: String) = s.trim().lowercase().replace(Regex("[\\s_]+"), " ")
        }
    }

    fun snapshot(masters: JSONObject?): Snapshot {
        val all = projects(masters)
        val locals = locals()
        val typeOf = locals.associate { key(it.site) to it.siteType }
        val addrOf = locals.associate { key(it.site) to it.address }
        val erp = serverSites(masters)

        val byClient = LinkedHashMap<String, MutableList<Project>>()
        val bySite = LinkedHashMap<String, MutableList<Project>>()
        for (p in all) {
            byClient.getOrPut(key(p.client)) { mutableListOf() }.add(p)
            bySite.getOrPut(key(p.client) + "\u0000" + key(p.site)) { mutableListOf() }.add(p)
        }

        val sites = LinkedHashMap<String, List<Site>>()
        for ((ck, ps) in byClient) {
            val grouped = LinkedHashMap<String, MutableList<Project>>()
            for (p in ps) grouped.getOrPut(key(p.site)) { mutableListOf() }.add(p)
            sites[ck] = grouped.map { (sk, sps) ->
                Site(
                    client = sps.first().client,
                    name = sps.first().site,
                    serverId = sps.firstOrNull { it.siteId.isNotBlank() }?.siteId ?: "",
                    type = erp[sk]?.optString("site_type").orEmpty().ifEmpty { typeOf[sk] ?: "" },
                    city = erp[sk]?.optString("city").orEmpty(),
                    address = erp[sk]?.optString("site_address").orEmpty()
                        .ifEmpty { addrOf[sk] ?: "" },
                    // Same pessimism as sitesOf: a folder that says synced
                    // while one project inside it never left the phone is the
                    // lie that loses a capture.
                    local = sps.any { it.local })
            }.sortedWith(compareBy({ siteOrder(it.name) }, { it.name.lowercase() }))
        }

        return Snapshot(
            projects = all,
            clients = all.map { it.client }.distinctBy { it.lowercase() }.sorted(),
            sites = sites,
            bySite = bySite,
            counts = byClient.mapValues { (_, v) -> v.size },
        )
    }

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
    /** Whether this user may re-stage a photograph, as the bench answered on
     *  the last bootstrap. Absent (an older bench) means NO: refusing an act
     *  the server would refuse anyway is the safe way to be wrong. */
    fun canRestage(masters: JSONObject?): Boolean =
        masters?.optBoolean("can_restage", false) ?: false

    /** Trade order for a stage name. Unknown words sort to the BOTTOM, not
     *  the top, so a capture from before the master existed cannot push
     *  today's work off the screen. */
    fun stageOrder(masters: JSONObject?, stage: String): Int =
        stages(masters).firstOrNull { it.name.equals(stage, true) }?.sequence ?: -1

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
            out.add(Article(o.optString("code"), o.optString("article"), jobs,
                kind = o.optString("kind").ifBlank { KIND_BUILD },
                basis = o.optString("basis").ifBlank { BASIS_NOS }))
        }
        return out.sortedBy { it.code }
    }

    /** The SKUs a capture on this project may be tagged to. They ride
     *  bootstrap so a technician in a basement can still tag a photo. */
    /**
     * Every SKU on a project — ERP's AND the ones recorded here.
     *
     * The local half was missing entirely, which read on the phone as "adding
     * a SKU does not refresh". It was worse than that: a SKU typed on site
     * did not appear in this list at all until it had synced AND the masters
     * had been fetched again, so the work somebody had just recorded looked
     * like it had not been taken. Amit, 2026-08-22: "Adding a sku is also not
     * quick refresh."
     *
     * Takes the whole Project rather than its server id, because a project
     * that has not synced HAS no server id — and that is exactly the case
     * where every SKU on it is local.
     */
    fun skusOf(masters: JSONObject?, project: Project?): List<Sku> {
        if (project == null) return emptyList()
        val erp = mutableListOf<Sku>()
        val arr = masters?.optJSONArray("projects")
        if (arr != null && project.serverId.isNotBlank()) {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                if (o.optString("project") != project.serverId) continue
                val skus = o.optJSONArray("skus") ?: break
                for (j in 0 until skus.length()) {
                    val s = skus.optJSONObject(j) ?: continue
                    erp.add(Sku(s.optString("name"), s.optString("code"),
                        s.optString("room"), s.optString("article")))
                }
                break
            }
        }
        // A local row loses to an ERP one for the same article in the same
        // room: once the bench has it, the bench's code is the real one.
        val taken = erp.map { key(it.room) + "\u0000" + key(it.article) }.toSet()
        val locals = localSkusOf(project.title).filterNot {
            key(it.room) + "\u0000" + key(it.articleName) in taken
        }.map {
            Sku(name = "", code = previewCode(project.client, it.room, it.articleCode),
                room = it.room, article = it.articleName, local = true)
        }
        return erp + locals
    }

    /** ERP's own site rows, keyed the same way site names are matched.
     *
     *  Without this the type and address were read from the OFFLINE list
     *  only, so a site the office created — the common case — showed neither,
     *  and typing them on the phone looked like it had not saved. */
    private fun serverSites(masters: JSONObject?): Map<String, JSONObject> {
        val arr = masters?.optJSONArray("sites") ?: return emptyMap()
        val out = LinkedHashMap<String, JSONObject>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val n = o.optString("site_name").trim()
            if (n.isNotEmpty()) out[key(n)] = o
        }
        return out
    }

    /** The Site Type options the SERVER offers. Never a list held here: a
     *  type added to the doctype and not to the app is one nobody can pick. */
    fun siteTypes(masters: JSONObject?): List<String> {
        val arr = masters?.optJSONArray("site_types") ?: return DEFAULT_SITE_TYPES
        val out = (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        return out.ifEmpty { DEFAULT_SITE_TYPES }
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

        /** Only a fallback for a phone that has never synced. The live list
         *  comes from the server (masters.site_types) so the doctype stays the
         *  one place a type is added or retired. */
        val DEFAULT_SITE_TYPES = listOf(
            "Flat", "Bungalow", "Row House", "Office", "Shop", "Other")
        const val NO_SITE = "(no site)"
        const val JOB_NEW = "New work"
        const val JOB_REPAIR = "Repair"
        const val JOB_INSTALL = "Supply & install"
        const val KIND_BUILD = "Build"
        const val KIND_INSTALL = "Install"
        const val KIND_SUBCONTRACT = "Subcontract"
        const val BASIS_NOS = "Nos"
        const val BASIS_LUMPSUM = "Lumpsum"
        /** The order the picker groups by: the shop's own work first, then
         *  what it fits, then what it gives away. */
        val KIND_ORDER = listOf(KIND_BUILD, KIND_INSTALL, KIND_SUBCONTRACT)

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
