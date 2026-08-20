package com.malletcrafts.sitephotos

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * The folder tree the app browses: client → project → room.
 *
 * Two sources, one list. ERP's `bootstrap` supplies the real clients,
 * projects and the Estimate Room master; anything typed on site while there
 * was no signal is held here until `ensure_site` can turn it into masters.
 * A technician standing in an empty flat should never be blocked because
 * the office has not created the lead yet, and should never be shown two
 * versions of the same client because they were.
 *
 * Rooms are deliberately NOT invented per project. They come from the same
 * Estimate Room master the SKU codes use, so a photo files itself beside
 * YS_MB_WAR instead of beside a free-text "master bedrm" typed at 7pm.
 */
class Catalogue(context: Context) {

    private val prefs = context.getSharedPreferences("catalogue", Context.MODE_PRIVATE)

    data class Project(
        val client: String,
        val title: String,
        /** ERP's Project name (PROJ-0004). Empty until this site syncs. */
        val serverId: String,
        val local: Boolean,
    ) {
        val synced: Boolean get() = serverId.isNotBlank()
    }

    // ---- what was typed on site, before ERP knew about it ---------------

    private fun localSites(): List<Pair<String, String>> {
        val raw = prefs.getString("local_sites", "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val c = o.optString("client").trim()
            val p = o.optString("project").trim()
            if (c.isEmpty() || p.isEmpty()) null else c to p
        }
    }

    /** Remember a site typed on site. Idempotent, and case-insensitive so a
     *  second visit does not create a second folder. */
    fun addLocalSite(client: String, project: String) {
        val c = client.trim()
        val p = project.trim()
        if (c.isEmpty() || p.isEmpty()) return
        val have = localSites()
        if (have.any { it.first.equals(c, true) && it.second.equals(p, true) }) return
        val arr = JSONArray()
        (have + (c to p)).forEach {
            arr.put(JSONObject().put("client", it.first).put("project", it.second))
        }
        prefs.edit().putString("local_sites", arr.toString()).apply()
    }

    /** Once ERP has the site, its local copy is redundant — dropping it is
     *  what stops the same folder appearing twice after a sync. */
    fun forgetLocalSite(client: String, project: String) {
        val arr = JSONArray()
        localSites().filterNot {
            it.first.equals(client.trim(), true) && it.second.equals(project.trim(), true)
        }.forEach { arr.put(JSONObject().put("client", it.first).put("project", it.second)) }
        prefs.edit().putString("local_sites", arr.toString()).apply()
    }

    // ---- the merged tree -------------------------------------------------

    fun projects(masters: JSONObject?): List<Project> {
        val out = mutableListOf<Project>()
        val seen = mutableSetOf<String>()

        val arr = masters?.optJSONArray("projects")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val client = o.optString("customer_name").ifBlank { o.optString("customer") }
                val title = o.optString("title").ifBlank { o.optString("project") }
                if (client.isBlank() || title.isBlank()) continue
                out.add(Project(client, title, o.optString("project"), local = false))
                seen.add(key(client, title))
            }
        }
        for ((c, p) in localSites()) {
            if (key(c, p) in seen) continue     // ERP has it now; theirs wins
            out.add(Project(c, p, "", local = true))
        }
        return out.sortedWith(compareBy({ it.client.lowercase() }, { it.title.lowercase() }))
    }

    fun clients(masters: JSONObject?): List<String> =
        projects(masters).map { it.client }.distinctBy { it.lowercase() }.sorted()

    fun projectsOf(masters: JSONObject?, client: String): List<Project> =
        projects(masters).filter { it.client.equals(client, true) }

    /** The room master, with a usable fallback: a phone that has never been
     *  online still has to be able to file a capture somewhere sensible. */
    fun rooms(masters: JSONObject?): List<String> {
        val arr = masters?.optJSONArray("rooms")
        if (arr != null && arr.length() > 0) {
            return (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        }
        return FALLBACK_ROOMS
    }

    private fun key(client: String, project: String) =
        "${client.trim().lowercase()}|${project.trim().lowercase()}"

    companion object {
        /** Only used before the phone has ever reached ERP. These are the
         *  rooms the house already names in SKU codes. */
        val FALLBACK_ROOMS = listOf(
            "Master Bedroom", "Kids Bedroom", "Guest Bedroom", "Living Room",
            "Dining", "Kitchen", "Foyer", "Passage", "Balcony", "Study",
            "Utility", "Toilet", "Pooja",
        )
    }
}
