package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Renames.Held
import com.malletcrafts.interiorestimate.pano.TreeNav.At
import com.malletcrafts.interiorestimate.pano.TreeNav.P
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHERE THE APP IS IN THE TREE (port 63 slice F4d; android/docs/port63/DESIGN-tree.md §8): the way down, Back as the
 * same rule read upwards, the place found again when the tree changes under it, a typed project that already exists,
 * and the breadcrumb's chevron -- on a made-up tree -- and, read as source because only CI compiles :app, that the
 * screens move through the tree by these functions and no other.
 */
class TreeNavTest {

    // A made-up tree. Two clients each have a site called "Main site" (a site is a name PER CLIENT).
    private val kitchen = P("PROJ-T1", "Asha Rao", "Flat 9", "kitchen")
    private val wardrobe = P("PROJ-T2", "Asha Rao", "Flat 9", "wardrobe")
    private val loft = P("PROJ-T5", "Asha Rao", "Main site", "loft")
    private val study = P("S5-aaaaaaaaaa", "Bela Nair", "Main site", "study")
    private val balcony = P("dev kulkarni|shop 3|balcony", "Dev Kulkarni", "Shop 3", "balcony")
    private val counter = P("dev kulkarni|shop 3|counter", "Dev Kulkarni", "Shop 3", "counter")
    private val tv = P("esha pal|row 7|tv unit", "Esha Pal", "Row 7", "tv unit")
    private val pooja = P("PROJ-T9", "Farid Khan", "(no site)", "pooja unit")
    private val all = listOf(kitchen, wardrobe, loft, study, balcony, counter, tv, pooja)
    private val clients = listOf("Asha Rao", "Bela Nair", "Dev Kulkarni", "Esha Pal", "Farid Khan")

    /** A client's sites as the tree lists them: a site with no project is still a site ("Villa 2", a bench row with no job on it). */
    private val siteMap = mapOf(
        "Asha Rao" to listOf("Flat 9", "Main site"), "Bela Nair" to listOf("Main site"), "Dev Kulkarni" to listOf("Shop 3"),
        "Esha Pal" to listOf("Row 7", "Villa 2"), "Farid Khan" to listOf("(no site)"))
    private fun sitesOf(m: Map<String, List<String>> = siteMap): (String) -> List<String> =
        { c -> m.entries.firstOrNull { Renames.key(it.key) == Renames.key(c) }?.value ?: emptyList() }
    private val root = TreeNav.ROOT

    private fun openClient(c: String, ps: List<P> = all) = TreeNav.openClient(c, sitesOf()(c), ps)
    private fun back(at: At, ps: List<P> = all) = TreeNav.back(at, sitesOf(), ps)
    private fun settle(at: At, open: P?, ps: List<P> = all, m: Map<String, List<String>> = siteMap, cs: List<String> = clients) =
        TreeNav.settle(at, open, cs, sitesOf(m), ps)

    // ---- the way down --------------------------------------------------------------------------------------------

    @Test fun `opening a client goes through a single site and a single project, and stops where there is a choice`() {
        assertEquals(At("Asha Rao", null, null), openClient("Asha Rao"), "two sites: the list of sites")
        assertEquals(At("Bela Nair", "Main site", "S5-aaaaaaaaaa"), openClient("Bela Nair"), "one site, one project: straight into it")
        assertEquals(At("Dev Kulkarni", "Shop 3", null), openClient("Dev Kulkarni"), "one site, two projects: the site's list")
        assertEquals(At("Farid Khan", "(no site)", "PROJ-T9"), openClient("Farid Khan"), "the bucket is opened like a site")
        // a site row's tap, and the site crumb's switch
        assertEquals(At("Asha Rao", "Flat 9", null), TreeNav.openSite("Asha Rao", "Flat 9", all))
        assertEquals(At("Asha Rao", "Main site", "PROJ-T5"), TreeNav.openSite("Asha Rao", "Main site", all), "a site's single project is opened with it")
        assertEquals(At("Esha Pal", "Villa 2", null), TreeNav.openSite("Esha Pal", "Villa 2", all), "a site with no project is an empty list")
        // names as the catalogue compares them
        assertEquals("PROJ-T5", TreeNav.openSite("asha_rao", " main  site ", all).project)
    }

    @Test fun `a client never opens another client's project - two clients, one site name (audit R12)`() {
        // the old rule found the site BY NAME ACROSS EVERY CLIENT: the first project in the whole tree at a "Main site" is
        // Asha Rao's loft, so tapping Bela Nair opened that -- under Bela Nair's crumbs, and a photo taken there was hers
        val first = all.first { Renames.key(it.site) == Renames.key("Main site") }
        assertEquals("Asha Rao", first.client, "the fixture is the fault's: another client's project comes first under that name")
        val at = openClient("Bela Nair")
        assertEquals("S5-aaaaaaaaaa", at.project)
        // and for every client and every site of it: the project opened, if any, is that client's, at that site
        for (c in clients) {
            val opened = listOf(openClient(c)) + sitesOf()(c).map { TreeNav.openSite(c, it, all) }
            for (o in opened) {
                assertEquals(c, o.client)
                val p = o.project?.let { k -> all.first { it.key == k } } ?: continue
                assertEquals(c, p.client, "$c opened ${p.title}"); assertEquals(Renames.key(o.site!!), Renames.key(p.site))
            }
        }
        // whichever order the tree lists the projects in
        assertEquals("S5-aaaaaaaaaa", TreeNav.openClient("Bela Nair", listOf("Main site"), all.reversed()).project)
    }

    // ---- back ----------------------------------------------------------------------------------------------------

    @Test fun `Back retraces the way in - back(open(x)) is where open started, for every node of the tree`() {
        // a client is opened from the clients
        for (c in clients) assertEquals(root, back(openClient(c)), c)
        for (c in clients) for (s in sitesOf()(c)) {
            val several = sitesOf()(c).size > 1
            // a site is opened from the client's list of sites -- which is a stop only when the client has more than one
            val from = if (several) At(c, null, null) else root
            assertEquals(from, back(TreeNav.openSite(c, s, all)), "$c / $s")
            val here = all.filter { it.client == c && it.site == s }
            for (p in here) {
                // a project is opened from its site's list -- which is a stop only when the site holds more than one
                val list = if (here.size > 1) At(c, s, null) else from
                assertEquals(list, back(At(c, s, p.key)), "$c / $s / ${p.title}")
            }
            // and the list of projects itself goes back to where the site was opened from
            assertEquals(from, back(At(c, s, null)), "$c / $s")
        }
        assertEquals(root, back(At("Asha Rao", null, null))); assertEquals(root, back(root))
    }

    @Test fun `Back never stops at a list of one, and always reaches the clients`() {
        // the level a single child fills in is not a stop on the way out either -- or the two directions disagree
        assertEquals(root, back(At("Bela Nair", "Main site", "S5-aaaaaaaaaa")), "one site, one project: straight out")
        assertEquals(At("Asha Rao", null, null), back(At("Asha Rao", "Main site", "PROJ-T5")), "a single project of one of two sites: to the sites")
        assertEquals(At("Asha Rao", "Flat 9", null), back(At("Asha Rao", "Flat 9", "PROJ-T1")), "one of two projects: to the site's list")
        assertEquals(root, back(At("Dev Kulkarni", "Shop 3", null)), "the list of a client's only site: to the clients")
        // from any place, at most three steps, each strictly higher
        val places = clients.flatMap { c -> listOf(At(c, null, null)) + sitesOf()(c).flatMap { s -> listOf(At(c, s, null)) + all.filter { it.client == c && it.site == s }.map { At(c, s, it.key) } } }
        for (start in places) {
            var at = start; var steps = 0
            while (at != root) {
                val up = back(at)
                assertTrue(depth(up) < depth(at), "$at -> $up"); at = up; steps++
            }
            assertTrue(steps in 1..3, "$start took $steps")
        }
    }

    private fun depth(at: At) = listOf(at.client, at.site, at.project).count { it != null }

    // ---- the place, when the tree changes under it -----------------------------------------------------------------

    @Test fun `nothing changed - every place settles to itself`() {
        for (c in clients) {
            assertEquals(At(c, null, null), settle(At(c, null, null), null))
            for (s in sitesOf()(c)) {
                assertEquals(At(c, s, null), settle(At(c, s, null), null))
                for (p in all.filter { it.client == c && it.site == s }) assertEquals(At(c, s, p.key), settle(At(c, s, p.key), p))
            }
        }
        assertEquals(root, settle(root, null))
        // a name that still matches is given in the tree's own spelling
        assertEquals(At("Asha Rao", "Flat 9", null), settle(At(" asha  rao", "flat_9", null), null))
    }

    @Test fun `a site renamed under an open project - he stays in the project, under the site the tree lists it at now`() {
        // S5 renamed Asha Rao's "Flat 9" to "Flat 9A": the projects keep their keys, their site is the new name
        val after = all.map { if (it.client == "Asha Rao" && it.site == "Flat 9") it.copy(site = "Flat 9A") else it }
        val sites = siteMap + ("Asha Rao" to listOf("Flat 9A", "Main site"))
        assertEquals(At("Asha Rao", "Flat 9A", "PROJ-T1"), settle(At("Asha Rao", "Flat 9", "PROJ-T1"), kitchen, after, sites))
        // with only the SITE open, its name matches nothing any more: one level up, where it is listed under its new name
        assertEquals(At("Asha Rao", null, null), settle(At("Asha Rao", "Flat 9", null), null, after, sites))
        // ONE project moved to another site: he follows it; the site it left still stands for whoever is in its list
        val moved = all.map { if (it.key == "PROJ-T2") it.copy(site = "Main site") else it }
        assertEquals(At("Asha Rao", "Main site", "PROJ-T2"), settle(At("Asha Rao", "Flat 9", "PROJ-T2"), wardrobe, moved))
        assertEquals(At("Asha Rao", "Flat 9", null), settle(At("Asha Rao", "Flat 9", null), null, moved))
    }

    @Test fun `a project whose key changed is found by its client and title - the phone's row that became S5's, a row whose site moved, a twin`() {
        // the phone's own row took S5's key at a sync: the key the place holds is its names
        val synced = all.map { if (it === balcony) it.copy(key = "S5-bbbbbbbbbb") else it }
        assertEquals(At("Dev Kulkarni", "Shop 3", "S5-bbbbbbbbbb"), settle(At("Dev Kulkarni", "Shop 3", balcony.key), balcony, synced))
        // the phone's own row was moved to another site: its key is its names, so it changed with the site
        val moved = all.map { if (it === tv) P("esha pal|villa 2|tv unit", "Esha Pal", "Villa 2", "tv unit") else it }
        assertEquals(At("Esha Pal", "Villa 2", "esha pal|villa 2|tv unit"), settle(At("Esha Pal", "Row 7", tv.key), tv, moved))
        // A TWIN: a project built by hand under another site, which is no row of the tree -- the real one is
        val twin = P("asha rao|main site|kitchen", "Asha Rao", "Main site", "kitchen")
        assertEquals(At("Asha Rao", "Flat 9", "PROJ-T1"), settle(At("Asha Rao", "Main site", twin.key), twin))
        // two rows of one client and title: the one at the site it was opened at
        val two = listOf(P("k1", "Gita Bose", "Row 2", "unit"), P("k2", "Gita Bose", "Row 3", "unit"))
        val gita = mapOf("Gita Bose" to listOf("Row 2", "Row 3"))
        assertEquals("k2", settle(At("Gita Bose", "Row 3", "gone"), P("gone", "Gita Bose", "Row 3", "unit"), two, gita, listOf("Gita Bose")).project)
        assertEquals("k1", settle(At("Gita Bose", "Row 9", "gone"), P("gone", "Gita Bose", "Row 9", "unit"), two, gita, listOf("Gita Bose")).project)
        // never another client's project of the same title
        assertEquals(At("Bela Nair", "Main site", null), settle(At("Bela Nair", "Main site", "gone"), P("gone", "Bela Nair", "Main site", "kitchen")))
        // with no copy of the project to go by, only its key finds it
        assertEquals(At("Dev Kulkarni", "Shop 3", null), settle(At("Dev Kulkarni", "Shop 3", balcony.key), null, synced))
    }

    @Test fun `what is no longer in the tree steps up one level - a project, a site, a client - and the place never names what is not listed`() {
        // the project is gone, its site still holds another: the site's list
        val gone = all - kitchen
        assertEquals(At("Asha Rao", "Flat 9", null), settle(At("Asha Rao", "Flat 9", "PROJ-T1"), kitchen, gone))
        // the project is gone and its site with it: the client's sites
        val noLoft = all - loft
        val sites = siteMap + ("Asha Rao" to listOf("Flat 9"))
        assertEquals(At("Asha Rao", null, null), settle(At("Asha Rao", "Main site", "PROJ-T5"), loft, noLoft, sites))
        // a site that vanished, nothing open in it
        assertEquals(At("Esha Pal", null, null), settle(At("Esha Pal", "Villa 2", null), null, all, siteMap + ("Esha Pal" to listOf("Row 7"))))
        // a client that vanished (signed out: its projects were S5's alone), whatever was open under it
        val without = all.filter { it.client != "Bela Nair" }
        for (at in listOf(At("Bela Nair", null, null), At("Bela Nair", "Main site", null), At("Bela Nair", "Main site", "S5-aaaaaaaaaa")))
            assertEquals(root, settle(at, study.takeIf { at.project != null }, without, siteMap - "Bela Nair", clients - "Bela Nair"), "$at")
        // a site or a project with no client is no place
        assertEquals(root, settle(At(null, "Flat 9", "PROJ-T1"), kitchen))
        // whatever it is given, the answer is a place of the tree
        for (at in listOf(At("Asha Rao", "Nowhere", "none"), At("Nobody", "Flat 9", "PROJ-T1"), At("Asha Rao", "Flat 9", "none"))) {
            val to = settle(at, null)
            to.client?.let { c -> assertTrue(c in clients) }
            to.site?.let { s -> assertTrue(s in sitesOf()(to.client!!)) }
            to.project?.let { k -> assertTrue(all.any { it.key == k && it.client == to.client && it.site == to.site }) }
        }
        // a project found by its key under ANOTHER client (the office renamed the customer): he follows it there
        assertEquals(At("Asha Rao", "Flat 9", "PROJ-T1"), settle(At("Asha R", "Flat 9", "PROJ-T1"), kitchen.copy(client = "Asha R")))
    }

    // ---- New site: a project that already exists, and the prefill -------------------------------------------------

    @Test fun `a typed client and title that the tree already lists is that project - at whatever site it is`() {
        assertEquals(kitchen, TreeNav.existing("Asha Rao", "kitchen", all))
        assertEquals(kitchen, TreeNav.existing(" asha_rao ", "  Kitchen", all), "names as the catalogue compares them")
        assertNull(TreeNav.existing("Bela Nair", "kitchen", all), "another client's project of that title is not this client's")
        assertNull(TreeNav.existing("Asha Rao", "pantry", all)); assertNull(TreeNav.existing("Asha Rao", "  ", all)); assertNull(TreeNav.existing("Asha Rao", "kitchen", emptyList()))
        // another site typed for it is a move, offered -- not for a blank site, the bucket, or the site it is at
        assertTrue(TreeNav.offersMove("Main site", "Flat 9")); assertTrue(TreeNav.offersMove(" Flat 9 ", "(no site)"))
        for (same in listOf("", "  ", "(no site)", "Flat 9", " flat_9 ")) assertFalse(TreeNav.offersMove(same, "Flat 9"), "'$same'")
    }

    @Test fun `New site is never prefilled with a bucket's name, and a new project is never filed under one`() {
        assertEquals("Flat 9", TreeNav.prefill("  Flat 9 ")); assertEquals("Asha Rao", TreeNav.prefill("Asha Rao"))
        for (bucket in listOf("(no site)", " (No  Site) ", "(no client)", "(NO_CLIENT)", "", "   ", null)) assertEquals("", TreeNav.prefill(bucket), "'$bucket'")
        assertEquals("Flat 9", TreeNav.siteTyped(" Flat 9 ", "Main site"))
        for (none in listOf("", "  ", "(no site)", "(No Site)")) assertEquals("Main site", TreeNav.siteTyped(none, "Main site"), "'$none'")
        // the two buckets are the app's own: Catalogue.NO_SITE (pinned in RenameRouteTest) and S5Masters.NO_CLIENT
        assertEquals("(no client)", TreeNav.NO_CLIENT)
        assertTrue(app("S5Session.kt").contains("const val NO_CLIENT = \"${TreeNav.NO_CLIENT}\""), "S5Masters.NO_CLIENT is TreeNav.NO_CLIENT")
        // and each is shown through its master, so neither reads as a typed name in either language
        assertEquals("(no client)", com.malletcrafts.interiorestimate.pano.i18n.Masters.client_name(Tx.EN, TreeNav.NO_CLIENT))
    }

    // ---- the chevron, and S5's names heard on project open ------------------------------------------------------

    @Test fun `a breadcrumb's chevron is drawn only where a tap switches - the last crumb with a sibling`() {
        assertTrue(TreeNav.switches(isLast = true, siblings = 2)); assertTrue(TreeNav.switches(isLast = true, siblings = 15))
        assertFalse(TreeNav.switches(isLast = true, siblings = 1), "the only project of its site: nothing to switch to")
        assertFalse(TreeNav.switches(isLast = true, siblings = 0))
        assertFalse(TreeNav.switches(isLast = false, siblings = 5), "a crumb that is not the last goes up")
    }

    @Test fun `the tree is re-read when a read of S5's list changed a site's name - not for a first hearing of no name, nor a newer stamp`() {
        val held = mapOf("PROJ-T1" to Held("Flat 9", "t1"), "PROJ-T2" to Held("", ""))
        assertFalse(Renames.namesChanged(held, held))
        assertTrue(Renames.namesChanged(held, held + ("PROJ-T1" to Held("Flat 9A", "t2"))), "renamed")
        assertTrue(Renames.namesChanged(held, held + ("PROJ-T2" to Held("Flat 9", "t2"))), "a first name")
        assertTrue(Renames.namesChanged(held, held + ("PROJ-T5" to Held("Main site", "t2"))), "a project heard for the first time, with a name")
        assertFalse(Renames.namesChanged(held, held + ("PROJ-T5" to Held("", ""))), "heard for the first time with no name: the tree lists it where it did")
        assertFalse(Renames.namesChanged(held, held + ("PROJ-T1" to Held("Flat 9", "t9"))), "the same name at a newer stamp")
        assertFalse(Renames.namesChanged(emptyMap(), emptyMap()))
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

    @Test fun `the screens move through the tree by TreeNav - down, back, a site row and the site crumb alike`() {
        val main = app("MainActivity.kt")
        assertTrue(main.contains("val treePs = remember(cs) { cs.projects.map { TreeNav.P(it.key, it.client, it.site, it.title) } }"), "the tree as the rule reads it")
        val nav = between(main, "fun go(to: TreeNav.At) {", "// The project's rooms as its grid shows them")
        // the place :pano answers is applied in one function, the project read from the tree by its key
        assertTrue(nav.contains("navProject = to.project?.let { k -> cs.projects.firstOrNull { it.key == k } }"))
        assertTrue(nav.contains("fun openClient(c: String) = go(TreeNav.openClient(c, cs.sitesOf(c).map { it.name }, treePs))"))
        assertTrue(nav.contains("fun openSite(c: String, s: String) = go(TreeNav.openSite(c, s, treePs))"))
        assertTrue(nav.contains("go(TreeNav.back(TreeNav.At(client, site, proj?.key), { c -> cs.sitesOf(c).map { it.name } }, treePs))"), "Back is the rule's")
        assertFalse(nav.contains("siblings.size") || nav.contains("siteCount"), "no inline copy of the skip rule is left")
        // the cross-client lookup is gone with its function
        assertFalse(main.contains("openSiteInto"), "a site is no longer found by name across every client")
        assertFalse(main.contains("it.site.equals(site, true) }?.client"))
        // a site row's tap and the site crumb's switch take the same way down as a client's tap
        assertTrue(main.contains("onOpen = { openSite(client, it.name) },"), "the site row")
        assertTrue(main.contains("onSibling = { openSite(client, it) }))"), "the site crumb's switch")
        assertTrue(main.contains("onSibling = { openClient(it) }))") && main.contains("onOpen = { openClient(it) },"), "the client crumb's switch and the client row")
        assertFalse(main.contains("onOpen = { navSite = it.name }") || main.contains("onSibling = { navSite = it; navProject = null }"))
        assertEquals(1, count(main, "TreeNav.back("), "one Back for the tree")
    }

    @Test fun `the open place is found again whenever the tree changes, and the project-open read re-reads the tree when it took a name`() {
        val main = app("MainActivity.kt")
        val settle = between(main, "LaunchedEffect(cs) {", "// S5's QUOTE stage (v52 item 16), read ONCE per project open")
        assertTrue(settle.contains("val at = TreeNav.At(navClient, navSite, navProject?.key)") && settle.contains("val open = navProject?.let { TreeNav.P(it.key, it.client, it.site, it.title) }"))
        assertTrue(settle.contains("val to = TreeNav.settle(at, open, cs.clients, { c -> cs.sitesOf(c).map { it.name } }, treePs)"))
        // the project is the TREE's object, so a copy that still says the old site is replaced even where the key stands
        assertTrue(settle.contains("val now = to.project?.let { k -> cs.projects.firstOrNull { it.key == k } }") && settle.contains("if (to != at || now != navProject) {"))
        assertTrue(settle.contains("if (now == null) { navRoom = null; navCapture = null; navFace = null }"), "no room without its project")
        assertTrue(settle.contains("navSite = to.site") && settle.contains("navProject = now"))
        // a wish list open on the project follows it to the key it has now (it is held by the project's key)
        assertTrue(settle.contains("if (now != null && oldKey != null && oldKey != now.key && wishFor == oldKey) wishFor = now.key"))
        assertTrue(settle.indexOf("val oldKey = navProject?.key") in 0 until settle.indexOf("navProject = now"), "the old key is read before the project is replaced")
        assertEquals(1, count(main, "TreeNav.settle("))
        // it is composed above every early return of the tree's screens: before the search overlay, the tabs and the rename dialog
        val at = main.indexOf("LaunchedEffect(cs) {")
        for (later in listOf("renaming?.let { t ->", "if (showSearch) {", "if (tab != \"browse\") {"))
            assertTrue(at in 0 until main.indexOf(later), "the settle is composed before `$later`")
        // the read on project open: S5's names before it, and a reload when it changed one
        val read = between(main, "LaunchedEffect(navProject?.serverId) {\n        val id = navProject?.serverId", "// THE ALBUM MIRROR (port 61c slice QV)")
        val was = read.indexOf("val was = withContext(Dispatchers.IO) { S5Sites.all(context) }")
        val refresh = read.indexOf("val read = withContext(Dispatchers.IO) { QuoteStageSync.refresh(context, known) }")
        val reload = read.indexOf("if (read && Renames.namesChanged(was, withContext(Dispatchers.IO) { S5Sites.all(context) })) reload()")
        assertTrue(was in 0 until refresh && refresh < reload, "held before, read, compared after")
        assertTrue(read.contains("if (read) quoteTick += 1"), "the quote line is redrawn as before")
    }

    @Test fun `New site opens an existing project as itself and never builds one by hand, and is never prefilled with a bucket`() {
        val main = app("MainActivity.kt")
        val dialog = between(main, "if (showNewSite) {", "// The download-and-install itself")
        assertTrue(dialog.contains("client = TreeNav.prefill(navClient),") && dialog.contains("site = TreeNav.prefill(navSite),"), "never a bucket's name")
        assertFalse(dialog.contains("navClient.orEmpty()") || dialog.contains("navSite.orEmpty()"))
        // an existing project: found by the rule, read from the tree, opened as itself -- and nothing is added to the phone's rows
        val was = dialog.indexOf("val was = TreeNav.existing(client, proj, treePs)")
        val real = dialog.indexOf("val real = was?.let { w -> cs.projects.firstOrNull { it.key == w.key } }")
        val add = dialog.indexOf("cat.addLocal(client, at, proj, siteType = siteType,")
        assertTrue(was in 0 until real && real < add, "asked before anything is added")
        val opened = between(dialog, "if (real != null) {", "} else {")
        assertTrue(opened.contains("navProject = real") && opened.contains("navSite = real.site") && opened.contains("navClient = real.client"))
        assertFalse(opened.contains("addLocal"), "no second row for a project the tree lists")
        // another site typed for it is the F4c move, OFFERED: the project's dialog, its Site row open on the site typed
        assertTrue(opened.contains("if (TreeNav.offersMove(site, real.site))") && opened.contains("moveTo = site.trim())"))
        assertFalse(opened.contains("moveProject("), "offered, never made for him")
        // a new project: filed under a site's own name, and read back from the tree
        val made = dialog.substring(dialog.indexOf("} else {"))
        assertTrue(made.contains("val at = TreeNav.siteTyped(site, Catalogue.DEFAULT_SITE)") && made.contains("navProject = cat.projectOf(store.masters(), \"\", client, at, proj)"))
        assertEquals(0, count(main, "navProject = Catalogue.Project("), "no project is built by hand anywhere")
        // the dialog hands back the site AS TYPED, so "none typed" can be told from one typed
        val form = between(main, "private fun NewSiteDialog(", "private fun shortName(")
        assertTrue(Regex("""onSave\(clientName\.trim\(\),\s*siteName\.trim\(\),\s*projectName\.trim\(\), jobType, siteType, address\.trim\(\)\)""").containsMatchIn(form))
        assertFalse(form.contains("ifEmpty { Catalogue.DEFAULT_SITE }"), "the default is the caller's, by TreeNav.siteTyped")
        // the offer reaches the dialog, which opens its picker on it and says why
        assertTrue(main.contains("moveTo = t.moveTo,"))
        val row = between(app("TreeShell.kt"), "fun RenameDialog(", "internal const val MM_PER_IN")
        assertTrue(row.contains("var pickSite by remember(current, site) { mutableStateOf(moveTo.isNotBlank()) }") && row.contains("var newSite by remember(current, site) { mutableStateOf(moveTo) }"))
        // (THIS PIN MOVED WITH THE REVIEW OF F4: why the dialog opened is said WHENEVER New site opened it, also where the
        // project's site cannot be changed from this phone -- there with a line that names no button. MoveRouteTest holds both lines.)
        assertTrue(row.contains("if (moveTo.isNotBlank()) Text(\n                        if (canMove) tx.app_project_exists_use_site else tx.app_project_exists_opened,"))
        val offer = "This client already has this project. To move it to the site you typed, tap Use this site."
        assertEquals(offer, Tx.EN.app_project_exists_use_site)
        assertEquals("Use this site", Tx.EN.app_use_site)
        assertTrue(offer.contains("Use this site"), "it quotes the button's own words")
        // English first (Amit, 16 Nov 2026): no Marathi is composed yet, so under मराठी it reads its English (mr-pending.tsv)
        assertEquals(offer, Tx.MR.app_project_exists_use_site)
    }

    @Test fun `the breadcrumb draws its chevron by the rule its tap reads, and the Switch sheet's rows scroll`() {
        val crumbs = app("Crumbs.kt")
        val rail = between(crumbs, "fun CrumbRail(", "@Composable\nprivate fun Separator()")
        assertTrue(rail.contains("val switches = TreeNav.switches(isLast, c.siblings.size)"), "one answer")
        assertTrue(rail.contains("CrumbPill(c.label, current = isLast, chevron = switches) {") && rail.contains("if (switches) sheetFor = idx"), "the chevron and the tap both read it")
        assertFalse(rail.contains("c.siblings.size > 1"), "no second copy of the rule")
        val pill = between(crumbs, "private fun CrumbPill(label: String, current: Boolean, chevron: Boolean, onClick: () -> Unit) {", "@Composable\nfun SheetTitle(")
        assertTrue(pill.contains("if (chevron) {") && !pill.contains("if (current) {"), "drawn where a tap switches, not on the current crumb always")
        assertTrue(rail.contains("CrumbPill(\"…\", current = false, chevron = false)"), "the fold has none")
        // the sheet's rows are in a scrolling column, capped as the app's other sheets are
        val sheet = between(rail, "SheetTitle(tx.type_s_short)", "Spacer(Modifier.height(12.dp))")
        assertTrue(sheet.contains("Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {") && sheet.contains("c.siblings.forEach { (key, label) ->"))
        assertTrue(crumbs.contains("import androidx.compose.foundation.verticalScroll") && crumbs.contains("import com.malletcrafts.interiorestimate.pano.TreeNav"))
    }
}
