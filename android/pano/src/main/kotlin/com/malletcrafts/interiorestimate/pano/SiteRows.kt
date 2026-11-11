package com.malletcrafts.interiorestimate.pano

/**
 * WHICH SITES THE TREE LISTS, AND UNDER WHAT NAME (port 62 slice A2, 11 Nov 2026): the rule that used to sit
 * inside :app's Catalogue, moved here so it is tested.
 *
 * ONE SOURCE FOR A SITE'S NAME ([nameOf]). S5 keeps each project's site name since slice A1, so where S5 holds
 * one it is the name shown (audit R4: the phone used to show a copy of its own that a rename did not update).
 * Where S5 holds none, the name the masters carry (an ERP bootstrap's); for a project only S5 lists, the name
 * the phone's own row gives it; else none, and the project is filed under the "(no site)" bucket.
 *
 * THE OLD NAME MUST NOT STAY BEHIND AS AN EMPTY SITE ([listed]; the port 62 plan critic, finding 2). The tree
 * makes a site group for every row of the masters' `sites` array a client owns, projects or none -- "an empty
 * group is a site the office knows about that simply has no jobs on it yet" -- and that array is kept as an ERP
 * bootstrap brought it. So after S5 took a site's new name, the bootstrap's row of the OLD name would be listed
 * beside it as an empty site that can only answer "cannot be renamed" -- on any phone that ever bootstrapped.
 * The rename step re-names that row in the stored masters; this is the read-time half, which also covers a phone
 * whose NEXT bootstrap brings the bench's old name back: a `sites` row whose docname is the `site` of a project
 * that S5 names differently is listed under S5's name, never as a second site.
 */
object SiteRows {

    /** A row of the masters' `sites` array: the bench's docname for the site (MEST-SITE-…), whose it is, and its own name. */
    data class Row(val docname: String, val client: String, val name: String)

    /** A project as the tree lists it: the site name it is listed under ([nameOf]) and the `sites` row its masters entry links to ('' when none). */
    data class Proj(val client: String, val site: String, val siteId: String)

    /**
     * The site name a project is listed under, '' for none (the caller files that under its bucket): [s5] is S5's
     * name for the project (null when S5 does not list it, '' when S5 lists it with no name), [masters] the masters
     * entry's own `site_name`, [local] the name this phone's own row gives the project -- read only for a project
     * that S5 alone lists, by the caller.
     */
    fun nameOf(s5: String?, masters: String, local: String): String =
        s5?.trim()?.takeIf { it.isNotEmpty() } ?: masters.trim().takeIf { it.isNotEmpty() } ?: local.trim()

    /**
     * The name each of [rows] is listed under, in the rows' order. A row no project links to keeps its own name:
     * a site with no project is still a site, listed empty. So does a row some linked project is still listed
     * under. A row whose every linked project is listed under ANOTHER name -- S5 renamed the site, and the bench's
     * row still says the old one -- is listed under that name, the first linked project's, so it is the same
     * site as its projects and never a second, empty one.
     */
    fun listed(rows: List<Row>, projects: List<Proj>): List<String> = rows.map { row ->
        val own = row.name.trim()
        val linked = if (row.docname.isBlank()) emptyList() else projects.filter { it.siteId == row.docname && Renames.named(it.site) }
        if (linked.isEmpty() || linked.any { Renames.key(it.site) == Renames.key(own) }) own else linked.first().site.trim()
    }
}
