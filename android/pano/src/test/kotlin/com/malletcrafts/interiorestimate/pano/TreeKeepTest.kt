package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.TreeKeep.Cap
import com.malletcrafts.interiorestimate.pano.TreeKeep.Entry
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHAT THE TREE KEEPS OF AN ERP BOOTSTRAP ONCE S5 IS THE LIST (port 63 slice F4a; android/docs/port63/DESIGN-tree.md).
 * Amit, 23 Nov 2026, with build 0.3.350 on his phone: "get rid of any other clients." Every name here is made up.
 *
 * The rule first, case by case; then -- read as source, because only CI compiles :app -- that the one read of S5's
 * list asks it and holds no answer of its own, that each of its three callers says whether the pair is live, and
 * that the Demo Client, the search's suggestions, the Work tab's recents and the Queue's Sent list follow.
 */
class TreeKeepTest {

    private val e = Entry("PROJ-T7", "Asha Rao", "kitchen", "SITE-T1")
    private fun cap(project: String = "", client: String = "Asha Rao", title: String = "kitchen", sent: Boolean = false) = Cap(project, client, title, sent)

    // ---- the rule ----------------------------------------------------------------------------------------------

    @Test fun `a live pair keeps every bootstrap entry - S5 lists it or not, a capture or none`() {
        assertTrue(TreeKeep.keepsProject(e, erpLive = true, s5Listed = emptySet(), captures = emptyList()))
        assertTrue(TreeKeep.keepsProject(e, true, setOf("PROJ-T8"), listOf(cap("PROJ-T7", sent = true))))
        assertTrue(TreeKeep.keepsProject(Entry("", "", "", ""), true, emptySet(), emptyList()), "whatever the entry holds: its own next bootstrap replaces it")
    }

    @Test fun `no live pair - a project S5 lists stays, and a project only ERP holds goes`() {
        assertTrue(TreeKeep.keepsProject(e, false, setOf("PROJ-T7"), emptyList()))
        assertTrue(TreeKeep.keepsProject(e, false, setOf("PROJ-T1", "PROJ-T7", "S5-aaaaaaaaaa"), emptyList()))
        assertFalse(TreeKeep.keepsProject(e, false, setOf("PROJ-T8"), emptyList()), "S5 lists another project, not this one")
        assertFalse(TreeKeep.keepsProject(e, false, emptySet(), emptyList()), "S5 lists nothing: the list is still S5's answer")
        // ids are compared exactly: they are ids, not names
        assertFalse(TreeKeep.keepsProject(e, false, setOf("proj-t7", "PROJ-T7 "), emptyList()))
        // an entry with no id is never "listed", whatever S5's list holds
        assertFalse(TreeKeep.keepsProject(Entry("", "Asha Rao", "kitchen", ""), false, setOf(""), emptyList()))
    }

    @Test fun `no live pair - a project only ERP holds stays while a capture of it exists only on this phone`() {
        // by the project id the capture carries
        assertTrue(TreeKeep.keepsProject(e, false, emptySet(), listOf(cap("PROJ-T7", client = "x", title = "y"))), "by id, whatever names the capture carries")
        // by client and title, as the catalogue compares names, for a capture no sync has given an id yet
        assertTrue(TreeKeep.keepsProject(e, false, emptySet(), listOf(cap("", client = "asha_rao", title = " Kitchen "))))
        // a capture a server already holds keeps nothing listed: it is not only on this phone
        assertFalse(TreeKeep.keepsProject(e, false, emptySet(), listOf(cap("PROJ-T7", sent = true))))
        assertFalse(TreeKeep.keepsProject(e, false, emptySet(), listOf(cap("", sent = true))))
        // another project's capture, another client's capture of the same title, the same client's other title
        assertFalse(TreeKeep.keepsProject(e, false, emptySet(), listOf(cap("PROJ-T8"))), "a capture that carries an id is of THAT project, whatever its names")
        assertFalse(TreeKeep.keepsProject(e, false, emptySet(), listOf(cap("", client = "Bela Nair"))))
        assertFalse(TreeKeep.keepsProject(e, false, emptySet(), listOf(cap("", title = "wardrobe"))))
        // one unsent capture among sent ones is enough
        assertTrue(TreeKeep.keepsProject(e, false, emptySet(), listOf(cap("PROJ-T7", sent = true), cap("PROJ-T8"), cap("PROJ-T7"))))
        // a capture with no names and no id is of no project: it never keeps a nameless entry
        assertFalse(TreeKeep.keepsProject(Entry("PROJ-T9", "", "", ""), false, emptySet(), listOf(cap("", client = "", title = ""))))
    }

    @Test fun `a bootstrap site row stays only while a kept project links to it`() {
        assertTrue(TreeKeep.keepsSite("SITE-T1", erpLive = false, keptSiteIds = setOf("SITE-T1")))
        assertFalse(TreeKeep.keepsSite("SITE-T2", false, setOf("SITE-T1")), "no kept project links to it: its owner would be a client with an empty site")
        assertFalse(TreeKeep.keepsSite("SITE-T1", false, emptySet()))
        // a kept project with no site link ('' among the kept ids) keeps no row, and a row with no docname links nothing
        assertFalse(TreeKeep.keepsSite("", false, setOf("")))
        assertFalse(TreeKeep.keepsSite(" ", false, setOf(" ")))
        // a live pair keeps every row: the desk-made site with no project yet is still listed
        assertTrue(TreeKeep.keepsSite("SITE-T2", true, emptySet()))
        assertTrue(TreeKeep.keepsSite("", true, emptySet()))
    }

    @Test fun `a live pair is one that is stored and not refused`() {
        assertTrue(TreeKeep.erpLive(pair = true, refused = false))
        assertFalse(TreeKeep.erpLive(pair = true, refused = true), "a refused pair receives no next bootstrap: it is no pair")
        assertFalse(TreeKeep.erpLive(pair = false, refused = false))
        assertFalse(TreeKeep.erpLive(pair = false, refused = true))
    }

    @Test fun `a whole stale bootstrap - what is left is S5's list and the work that is only on this phone`() {
        // The made-up phone: an ERP bootstrap of three clients from the day its key died, S5 listing ONE of those
        // projects, an unsent capture of a second, and nothing of the third client.
        val boot = listOf(
            Entry("PROJ-T1", "Asha Rao", "kitchen", "SITE-T1"), Entry("PROJ-T2", "Asha Rao", "wardrobe", "SITE-T1"),
            Entry("PROJ-T3", "Bela Nair", "study", "SITE-T2"), Entry("PROJ-T4", "Bela Nair", "loft", "SITE-T3"),
            Entry("PROJ-T5", "Chitra Sen", "pooja unit", "SITE-T4"))
        val sites = listOf("SITE-T1", "SITE-T2", "SITE-T3", "SITE-T4", "SITE-T5")
        val s5 = setOf("PROJ-T1", "S5-aaaaaaaaaa")
        val caps = listOf(Cap("PROJ-T3", "Bela Nair", "study", sent = false), Cap("PROJ-T2", "Asha Rao", "wardrobe", sent = true), Cap("S5-aaaaaaaaaa", "Dev Kulkarni", "balcony", sent = true))
        fun kept(live: Boolean) = boot.filter { TreeKeep.keepsProject(it, live, s5, caps) }
        assertEquals(listOf("PROJ-T1", "PROJ-T3"), kept(false).map { it.project })
        val links = kept(false).map { it.siteId }.toSet()
        assertEquals(listOf("SITE-T1", "SITE-T2"), sites.filter { TreeKeep.keepsSite(it, false, links) })
        // the same phone with a live pair: nothing is cut
        assertEquals(boot, kept(true))
        assertEquals(sites, sites.filter { TreeKeep.keepsSite(it, true, kept(true).map { e -> e.siteId }.toSet()) })
    }

    @Test fun `the Demo Client is listed only while the help tours are on - the rule takes the switch as its default`() {
        assertEquals(Features.HELP_TOURS, TreeKeep.demoListed(), "the default IS the switch")
        assertFalse(TreeKeep.demoListed(), "the tours are off (Features.HELP_TOURS): no Demo Client in the tree")
        assertTrue(TreeKeep.demoListed(tours = true)); assertFalse(TreeKeep.demoListed(tours = false))
        // the Queue's lists: every capture but the demo's own, which goes and comes back with the Demo Client's row
        assertFalse(TreeKeep.listsCapture(TourDemo.CAPTURE))
        assertTrue(TreeKeep.listsCapture(TourDemo.CAPTURE, demo = true))
        for (demo in listOf(true, false)) assertTrue(TreeKeep.listsCapture("MCAP-0123456789ab", demo), "a real capture is listed whatever the switch")
        assertEquals(TreeKeep.listsCapture(TourDemo.CAPTURE, demo = TreeKeep.demoListed()), TreeKeep.listsCapture(TourDemo.CAPTURE))
    }

    @Test fun `a capture is a recent only while the tree lists its project - joined by title, as the screens join photos`() {
        val titles = listOf("kitchen", "Wardrobe", "study")
        assertEquals(0, TreeKeep.projectOf("kitchen", titles))
        assertEquals(1, TreeKeep.projectOf("WARDROBE", titles), "ignoring case, as every screen's join does")
        assertEquals(-1, TreeKeep.projectOf("loft", titles), "its project has left the tree: not a recent")
        assertEquals(-1, TreeKeep.projectOf(TourDemo.PROJECT, titles), "the demo's capture, while the Demo Client is not listed")
        assertEquals(-1, TreeKeep.projectOf("kitchen", emptyList()))
        assertEquals(0, TreeKeep.projectOf("study", listOf("study", "study")), "the first of two, as the tree orders them")
    }

    // ---- :app, read as source (only CI compiles :app) ---------------------------------------------------------

    private fun app(name: String): String {
        val f = File("../app/src/main/kotlin/com/malletcrafts/interiorestimate/$name")
        assertTrue(f.isFile, "the app's $name is beside :pano (${f.absolutePath})")
        return f.readText()
    }

    /** [src] from the first [from] up to the first [to] after it. */
    private fun between(src: String, from: String, to: String): String {
        val a = src.indexOf(from); assertTrue(a >= 0, "`$from` is not in the source")
        val b = src.indexOf(to, a + from.length); assertTrue(b > a, "`$to` does not follow `$from`")
        return src.substring(a, b)
    }

    private fun count(src: String, what: String) = Regex(Regex.escape(what)).findAll(src).count()

    @Test fun `the read of S5's list asks the rule for every bootstrap project and site row, and holds no answer of its own`() {
        val session = app("S5Session.kt")
        val fetch = between(session, "fun fetch(context: Context, erpLive: Boolean): JSONObject? = try {", "} catch (e: Exception) {")
        // what the rule is asked with: the ids S5 just listed, every capture with whether a server holds it
        assertTrue(fetch.contains("if (id.isNotBlank()) listed.add(id)"), "the ids S5's list names")
        assertTrue(fetch.contains("captures.map { TreeKeep.Cap(it.project, it.customerName, it.projectTitle, sent = it.state == \"SYNCED\") }"), "sent is the store's own state")
        // the entry as the tree reads one: the customer's name else its id, the title else the id, the site link
        for (part in listOf("project = p.optString(\"project\"),", "client = p.optString(\"customer_name\").ifBlank { p.optString(\"customer\") },",
            "title = p.optString(\"title\").ifBlank { p.optString(\"project\") },", "siteId = p.optString(\"site\"))"))
            assertTrue(fetch.contains(part), "the entry holds `$part`")
        // asked BEFORE the entry is kept, and an entry S5 itself added earlier is never asked (it is replaced by this read)
        val drop = fetch.indexOf("if (p.optBoolean(FROM_S5)) continue"); val ask = fetch.indexOf("if (!TreeKeep.keepsProject(entry, erpLive, listed, caps)) continue"); val keep = fetch.indexOf("out.put(p)")
        assertTrue(drop in 0 until ask && ask < keep, "an S5 entry is dropped, a bootstrap entry asked, and only then kept")
        assertTrue(fetch.contains("keptSites.add(entry.siteId)"), "the kept projects' site links are what a site row is asked with")
        assertTrue(fetch.contains("if (TreeKeep.keepsSite(row.optString(\"name\"), erpLive, keptSites)) keep.put(row)") && fetch.contains("base.put(\"sites\", keep)"), "every row of the bootstrap's sites is asked")
        assertEquals(1, count(session, "TreeKeep.keepsProject(")); assertEquals(1, count(session, "TreeKeep.keepsSite("))
        // no answer of its own: the caller's erpLive goes to the rule as it is, and nothing else here reads the pair
        assertFalse(fetch.contains("FrappeClient"), "the read never decides for itself whether the pair is live")
        // what S5 does not serve is not touched: only `projects` and `sites` are written
        assertEquals(setOf("projects", "sites"), Regex("base\\.put\\(\"(\\w+)\"").findAll(fetch).map { it.groupValues[1] }.toSet())
        // the two facts are read in ONE place in :app
        assertTrue(session.contains("TreeKeep.erpLive(pair = FrappeClient.load(context) != null, refused = FrappeClient.refused(context))"), "S5Masters.erpLive")
        assertEquals(1, count(app("S5Session.kt") + app("MainActivity.kt") + app("SyncWorker.kt") + app("Catalogue.kt"), "TreeKeep.erpLive("), "one reader of the pair and its refusal")
    }

    @Test fun `each caller of the read says whether the pair is live - the sync's S5 leg never, the app's start by what the bench just answered, the rename by the stored facts`() {
        val main = app("MainActivity.kt"); val sync = app("SyncWorker.kt")
        assertEquals(3, count(main + sync, "S5Masters.fetch("), "three callers: the app's start, the rename's read-first, the sync's S5 leg")
        assertFalse((main + sync).contains("S5Masters.fetch(context)") || (main + sync).contains("S5Masters.fetch(ctx)"), "none leaves the answer out")
        // the S5 leg runs only with no pair or a refused one (doWork), so it is never live there
        val leg = between(sync, "private fun s5Leg(store: CaptureStore)", "private data class ErpOutcome")
        assertTrue(leg.contains("S5Masters.fetch(ctx, erpLive = false)?.let { store.saveMasters(it) }"))
        assertTrue(between(sync, "override suspend fun doWork(): Result {", "val failures =").contains("if (erp == null || erp.keyDead) { if (s5) s5Leg(store) }"), "the leg's own condition is no live pair")
        // the app's start: a bootstrap that landed just now is a live pair whatever an earlier sync recorded; else the stored facts
        val start = between(main, "LaunchedEffect(configured) {", "// Back from the camera")
        assertTrue(Regex("""val booted = runCatching \{\s*FrappeClient\.load\(context\)\?\.bootstrap\(\)\?\.let \{\s*store\.saveMasters\(it\)\s*true\s*\}\s*\}\.getOrNull\(\) == true""").containsMatchIn(start), "whether the bench answered")
        assertTrue(start.contains("val erpLive = booted || S5Masters.erpLive(context)"))
        assertTrue(Regex("""if \(!erpLive && S5Session\.signedIn\(context\)\)\s*S5Masters\.fetch\(context, erpLive = false\)\?\.let \{ store\.saveMasters\(it\) \}""").containsMatchIn(start),
            "no live pair and an S5 sign-in: S5's list is read at start, a refused pair's phone included")
        assertTrue(start.contains("if (erpLive && S5Auth.current(context) != null) SiteRename.hear(context)"), "a live pair: the list is read for its site names alone")
        // the rename's read-first
        assertTrue(main.contains("S5Masters.fetch(context, erpLive = S5Masters.erpLive(context))?.also { store.saveMasters(it) }"))
    }

    @Test fun `the Demo Client, the search's suggestions, the recents and the Queue's Sent list follow the tree`() {
        val cat = app("Catalogue.kt"); val main = app("MainActivity.kt")
        // the demo is appended to the tree only while the rule lists it, and nowhere else
        assertTrue(cat.contains("if (TreeKeep.demoListed() && out.none { TourDemo.isDemoProject(it.client, it.title) })"), "Catalogue.projects asks the rule")
        assertEquals(1, count(cat, "Project(TourDemo.CLIENT, TourDemo.SITE,"), "one place appends the demo flat")
        assertFalse(cat.contains("demoListed(tours") || cat.contains("demoListed(true") || cat.contains("demoListed(false"), "with no answer of its own")
        // the search overlay: no hard-coded suggestions, and no "Try" head over an empty list
        assertFalse(main.contains("Kothrud") || main.contains("listOf(\"MB\""), "the demo suggestions are gone")
        assertTrue(between(main, "SearchOverlay(", "onQuery = { searchQuery = it },").contains("recents = emptyList(),"))
        assertTrue(app("HomeScreens.kt").contains("if (recents.isNotEmpty()) Head(tx.app_try)"))
        // the Work tab's recents: a capture whose project the tree does not list is skipped, and the card is the tree's project
        val recent = between(main, "private fun recentRooms(", "private fun skuQueueRow(")
        assertTrue(recent.contains("val titles = projects.map { it.title }") && recent.contains("val at = TreeKeep.projectOf(c.projectTitle, titles)"))
        val skip = recent.indexOf("if (at < 0) continue"); val read = recent.indexOf("val p = projects[at]"); val card = recent.indexOf("seen[key] = RecentRoom(")
        assertTrue(skip in 0 until read && read < card, "skipped before anything is listed")
        assertTrue(recent.contains("client = p.client,") && recent.contains("site = p.site,") && recent.contains("projectKey = p.key,"), "the card names the tree's project")
        assertFalse(recent.contains("c.customerName") || recent.contains("Catalogue.DEFAULT_SITE"), "never the capture's own names in a project's place")
        // the Queue's Sent list
        assertTrue(main.contains("sent = queue.filter { it.state == \"SYNCED\" && TreeKeep.listsCapture(it.deviceId) }.take(12).map { queueRow(tx, it) },"))
    }
}
