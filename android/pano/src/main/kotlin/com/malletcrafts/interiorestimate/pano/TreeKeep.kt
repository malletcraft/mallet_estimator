package com.malletcrafts.interiorestimate.pano

/**
 * WHAT THE TREE KEEPS OF AN ERP BOOTSTRAP ONCE S5 IS THE LIST (port 63 slice F4a, 23 Nov 2026; the design record is
 * android/docs/port63/DESIGN-tree.md).
 *
 * Amit, 23 Nov 2026, with build 0.3.350 on his phone: "get rid of any other clients." The other clients' projects
 * had been cancelled in ERP the same evening and were still on his phone, because nothing on the phone could ever
 * take them off: the masters file is the last ERP bootstrap this phone received, a bootstrap is replaced only by the
 * next one, and a phone whose ERP pair is gone or refused never receives a next one. Every read of S5's list KEPT the
 * bootstrap's project entries and its whole `sites` array as they were (S5Masters.fetch: "Bootstrap entries stay"),
 * sign-out dropped only the entries S5 had added, and the tree makes a client of every site row's owner. So an S5-only
 * phone listed the ERP of the day its key died, for ever -- and no clean-up in ERP could reach it.
 *
 * THE RULE: ON A PHONE WITH NO LIVE ERP PAIR, S5'S LIST IS THE LIST. Asked of each bootstrap entry at every landed
 * read of S5's list, before the merge:
 *
 *  - a LIVE pair ([erpLive]: one is stored and the bench has not refused it) keeps everything -- its bootstrap is
 *    replaced whole by its own next sync, and S5's list is merged into it as before;
 *  - otherwise a bootstrap PROJECT stays only if S5 lists its id, or a capture of it on this phone has not reached a
 *    server yet ([keepsProject]): work that exists only here is never hidden by a list it has not reached;
 *  - a bootstrap SITE row stays only while a kept project links to it ([keepsSite]), so no client is left standing
 *    as the owner of an empty site.
 *
 * Rooms, job types, site types and stages are not asked: S5 serves none of them, and emptying them would empty every
 * picker. The rows MADE ON THIS PHONE (Catalogue's local rows) are not asked either: they are this phone's own
 * words, and one of them is the only name the phone has for the site of a project S5 has not named.
 *
 * The same file holds what goes with it: the Demo Client is listed only while the help tours are on
 * ([demoListed], [listsCapture]), and a capture is a "recent" only while its project is in the tree ([projectOf]).
 */
object TreeKeep {

    /**
     * One project entry of an ERP bootstrap, as the masters hold it: its ERP id, the client it is filed under (the
     * customer's name, else its id), its title, and the docname of the bench site row it links to ('' = none).
     */
    data class Entry(val project: String, val client: String, val title: String, val siteId: String)

    /**
     * One capture on this phone: the project id it carries ('' until a sync has given it one), the client and title
     * it was shot under, and whether a server already holds it ([sent]: the store's state is SYNCED).
     */
    data class Cap(val project: String, val client: String, val title: String, val sent: Boolean)

    /**
     * Whether this phone has a LIVE ERP pair: one is stored, and the bench has not refused it (FrappeClient.refused,
     * set by the sync's one bootstrap probe). A refused pair is no pair: its bootstrap will not be replaced, so it is
     * not the list.
     */
    fun erpLive(pair: Boolean, refused: Boolean): Boolean = pair && !refused

    private fun k(s: String) = Renames.key(s)

    /** Whether [c] is a capture of [e]: by the project id once the capture carries one, else by client and title as the catalogue compares names. */
    private fun isOf(c: Cap, e: Entry): Boolean =
        if (c.project.isNotBlank()) c.project == e.project
        else k(c.title).isNotEmpty() && k(c.client) == k(e.client) && k(c.title) == k(e.title)

    /**
     * Whether the bootstrap entry [e] stays in the masters after a read of S5's list.
     *
     * @param erpLive   [erpLive]: with a live pair every entry stays
     * @param s5Listed  the project ids S5's list just named
     * @param captures  every capture on this phone
     *
     * THE ONE DECISION THAT IS AMIT'S, AND ITS ONE HOME. Built as Claude recommended it and as he was asked on
     * 23 Nov 2026: a project that ONLY ERP holds leaves the tree of a phone with no live pair -- every client's,
     * his own among them -- and stays in ERP; S5's list is the one answer. That is the LAST LINE of this function.
     * Should he rule that the ERP-only projects of a client this phone works for stay, that line alone becomes
     *
     *     return captures.any { c -> (!c.sent && isOf(c, e)) || (c.project in s5Listed && k(c.client) == k(e.client)) }
     *
     * (a capture on this phone ties the entry's client to a project S5 lists), with its cases added to TreeKeepTest;
     * nothing in :app changes, because S5Masters.fetch asks this function and holds no answer of its own.
     */
    fun keepsProject(e: Entry, erpLive: Boolean, s5Listed: Set<String>, captures: List<Cap>): Boolean {
        // A live pair's bootstrap is the bench's own answer and is replaced whole by its next sync.
        if (erpLive) return true
        // S5 lists it: it is one of S5's projects, and its entry carries what S5 does not serve (dates, job type, SKUs).
        if (e.project.isNotBlank() && e.project in s5Listed) return true
        // Only ERP holds it. It stays while a capture of it exists only on this phone, and no longer.
        return captures.any { c -> !c.sent && isOf(c, e) }
    }

    /**
     * Whether a bootstrap `sites` row stays: with a live pair always; otherwise only while a KEPT project links to
     * it ([keptSiteIds]: the `site` docnames of the entries [keepsProject] kept). A row with no docname links nothing.
     */
    fun keepsSite(docname: String, erpLive: Boolean, keptSiteIds: Set<String>): Boolean =
        erpLive || (docname.isNotBlank() && docname in keptSiteIds)

    /**
     * Whether the Demo Client is a row of the tree. It exists only for the help tours (the demo flat a tour walks
     * through), so it is listed exactly while they are on -- the default IS the switch ([Features.HELP_TOURS]). With
     * the tours off it was a third client on a phone that has two. What a phone seeded while the tours were on stays
     * in its store, untouched, and is listed again the day the switch is.
     */
    fun demoListed(tours: Boolean = Features.HELP_TOURS): Boolean = tours

    /**
     * Whether the capture [deviceId] is a row of the Queue's lists: every capture but the demo's own, which is listed
     * only while the Demo Client is ([demoListed]) -- a "Demo Client" line under Sent is the same third client by
     * another door.
     */
    fun listsCapture(deviceId: String, demo: Boolean = demoListed()): Boolean = demo || !TourDemo.isDemoCapture(deviceId)

    /**
     * WHICH PROJECT OF THE TREE A CAPTURE IS OF, for the Work tab's recent rooms: the index in [titles] (the tree's
     * projects, in the tree's order) of the first whose title is the capture's, ignoring case; -1 when the tree lists
     * no such project -- and then the capture is NOT a recent. It used to be listed all the same, under its own
     * names, as a card that opened nothing: the project of an old ERP bootstrap that has left the tree, the demo's.
     * The join is the TITLE alone, as every screen that shows a project's photos joins them today; whoever joins
     * photos by project id (DESIGN-tree.md, "not fixed") changes it here.
     */
    fun projectOf(title: String, titles: List<String>): Int = titles.indexOfFirst { it.equals(title, ignoreCase = true) }
}
