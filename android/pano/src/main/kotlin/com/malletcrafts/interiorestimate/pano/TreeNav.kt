package com.malletcrafts.interiorestimate.pano

/**
 * WHERE THE APP IS IN THE TREE, AND HOW IT GETS THERE AND BACK (port 63 slice F4d, 23 Nov 2026; the design record is
 * android/docs/port63/DESIGN-tree.md §8).
 *
 * Amit, 23 Nov 2026, with build 0.3.350 on his phone: "site switch is not happening correctly. fix it." The tree is
 * client > site > project > room, and the place the app is at was four loose values in the screen -- two NAMES and a
 * COPY of a project taken the moment it was opened -- moved by rules written inline, each a little different:
 *
 *  - opening a client with one site looked that site up BY NAME ACROSS EVERY CLIENT (audit R12), so a client whose
 *    only site was called what an earlier client's site was called opened the other client's project;
 *  - opening a client went down through a single site and a single project, tapping a site row did not, and Back
 *    skipped a single-project list either way -- so Back skipped a list he had just walked through;
 *  - nothing re-read the place when the tree changed under it: a project whose site S5 renamed, or that a sync
 *    re-keyed, kept its old crumb, and Back from it showed the old site's list, empty;
 *  - the breadcrumb's last pill always drew a chevron, though a tap opens "Switch" only where there is a sibling
 *    to switch to: on a site that holds one project, a chevron with nothing behind it;
 *  - "New site" with the client and title of a project the tree already lists built a second project by hand that
 *    was not a row of the tree, showing the real one's photos under another site.
 *
 * So the place is one value ([At]) and every move through the tree is a function of it and of the tree, here, where
 * it is tested:
 *
 *  - DOWN ([openClient], [openSite]): a level nobody chooses from is not presented -- a client's single site, a
 *    site's single project -- and a site is opened ONLY INSIDE ITS OWN CLIENT.
 *  - BACK ([back]) is the same rule read upwards, so it retraces the way in: what the way down passed through is
 *    not a stop on the way up either.
 *  - WHEN THE TREE CHANGES ([settle]) the place is found again in it: the open project by its key, else by its
 *    client and title, and the site is the one the TREE lists that project under now; a site or a client that is
 *    no longer there steps up one level. The place never names something the tree does not list.
 *  - A TYPED CLIENT AND TITLE THAT IS A PROJECT ALREADY ([existing]) is that project, opened as itself.
 *  - THE CHEVRON ([switches]) is drawn exactly where a tap switches.
 *
 * Names are compared as the catalogue compares them ([Renames.key]): trimmed, lower case, runs of space and
 * underscore as one.
 */
object TreeNav {

    /** One project as the tree lists it: its key (`Catalogue.Project.key`: the server id, else its names), client, site and title. */
    data class P(val key: String, val client: String, val site: String, val title: String)

    /** A place in the tree: the open client, site and project (its [P.key]), each null when the app is not that deep. */
    data class At(val client: String?, val site: String?, val project: String?)

    /** Nothing open: the list of clients. */
    val ROOT = At(null, null, null)

    /** The client bucket a project S5 lists with no customer is filed under (`S5Masters.NO_CLIENT` in :app, pinned equal by TreeNavTest). */
    const val NO_CLIENT = "(no client)" // text:value a stored bucket value, shown through Masters.client_name

    private fun k(s: String?): String = Renames.key(s.orEmpty())

    private fun inSite(client: String, site: String, projects: List<P>): List<P> =
        projects.filter { k(it.client) == k(client) && k(it.site) == k(site) }

    /**
     * Open [site] of [client]: the site, and THROUGH it into its project when it holds exactly one -- a list of one
     * is a level nobody chooses from. The projects asked are that client's alone: a site is a name per client, and
     * four clients may each have a "Main site".
     */
    fun openSite(client: String, site: String, projects: List<P>): At =
        At(client, site, inSite(client, site, projects).singleOrNull()?.key)

    /** Open [client]: its sites ([sites], as the tree lists them), or straight through a single site ([openSite]). */
    fun openClient(client: String, sites: List<String>, projects: List<P>): At =
        if (sites.size == 1) openSite(client, sites[0], projects) else At(client, null, null)

    /**
     * One step back from [at]: the way down, read upwards. From a project to its site's list of projects -- unless
     * that list holds this project alone, which the way down passed through; from a site to the client's list of
     * sites -- unless the client has one site, which the way down passed through; from a client to the clients.
     * [sitesOf] is a client's sites as the tree lists them (a site with no project is still a site).
     */
    fun back(at: At, sitesOf: (String) -> List<String>, projects: List<P>): At {
        val client = at.client ?: return ROOT
        val site = at.site ?: return ROOT
        val above = if (sitesOf(client).size == 1) ROOT else At(client, null, null)
        if (at.project == null) return above
        return if (inSite(client, site, projects).size == 1) above else At(client, site, null)
    }

    /**
     * [at], FOUND AGAIN IN THE TREE AS IT IS NOW. Asked whenever the tree changes: a sync landed, S5 gave a project
     * another site name, a row was renamed, moved or removed.
     *
     *  - A PROJECT IS OPEN: it is found by its key; else -- the phone's own row has taken S5's key since, or its key
     *    was its names and its site has changed -- by the client and title of [open] (the project as it was when
     *    opened), at its old site if more than one matches. Found, the place is that project's client, SITE and key
     *    as the tree lists them now: a project S5 moved to another site keeps him in it, under the right crumb.
     *    Not found, the project is no longer a row of the tree: one level up, to its site.
     *  - A SITE IS OPEN: it stays while the client still has a site of that name; else the client's sites.
     *  - A CLIENT IS OPEN: it stays while the tree lists that client; else the clients.
     *
     * A name that still matches is given in the tree's own spelling.
     */
    fun settle(at: At, open: P?, clients: List<String>, sitesOf: (String) -> List<String>, projects: List<P>): At {
        val was = at.client ?: return ROOT
        if (at.project != null) {
            val found = projects.firstOrNull { it.key == at.project } ?: open?.let { o ->
                val same = projects.filter { k(it.client) == k(o.client) && k(it.title) == k(o.title) }
                same.firstOrNull { k(it.site) == k(o.site) } ?: same.firstOrNull()
            }
            if (found != null) return At(found.client, found.site, found.key)
        }
        val client = clients.firstOrNull { k(it) == k(was) } ?: return ROOT
        val site = at.site?.let { s -> sitesOf(client).firstOrNull { k(it) == k(s) } } ?: return At(client, null, null)
        return At(client, site, null)
    }

    /**
     * The project a typed client and title ALREADY IS, at whatever site the tree lists it: the tree hides the phone's
     * own row behind a listed project of the same client and title at ANY site, and a capture joins its project by
     * those two, so a second project of that client and title is not a second row -- it is this one. Null when the
     * tree lists none, and for a blank title.
     */
    fun existing(client: String, title: String, projects: List<P>): P? =
        if (k(title).isEmpty()) null else projects.firstOrNull { k(it.client) == k(client) && k(it.title) == k(title) }

    /** Whether a tap on a breadcrumb opens its "Switch" sheet: the last crumb, with another of its level to switch to. Only then is its chevron drawn. */
    fun switches(isLast: Boolean, siblings: Int): Boolean = isLast && siblings > 1

    /**
     * What a field of the New site dialog is prefilled with from the open place: the open client's or site's own
     * name, and NOTHING for a bucket -- "(no site)" and "(no client)" are where a project with no site or no client
     * is filed, not names anybody typed, and a project made under one would carry the bucket's name as its own.
     */
    fun prefill(name: String?): String {
        val n = name?.trim().orEmpty()
        return if (Renames.named(n) && k(n) != k(NO_CLIENT)) n else ""
    }

    /** The site a NEW project is filed under: the name typed when it is a site's own name, else [fallback] -- never the bucket. */
    fun siteTyped(typed: String, fallback: String): String = typed.trim().takeIf { Renames.named(it) } ?: fallback

    /**
     * Whether opening an EXISTING project from the New site dialog also offers to move it: a site was typed, it is a
     * site's own name, and it is not the site the tree lists the project under ([at]).
     */
    fun offersMove(typed: String, at: String): Boolean = Renames.named(typed.trim()) && k(typed) != k(at)
}
