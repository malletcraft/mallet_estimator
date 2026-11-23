package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.RenameRoute.Line
import com.malletcrafts.interiorestimate.pano.RenameRoute.Write
import com.malletcrafts.interiorestimate.pano.Renames.Held
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ONE PROJECT, ONE SITE (port 63 slice F4c; android/docs/port63/DESIGN-tree.md §7): where a move of one project to
 * another site goes ([Renames.moveRoute]), when its Site row is shut ([Renames.moveShut]), which sites are offered
 * ([Renames.siteChoices]) and what a typed name means ([Renames.moveTarget]) -- every branch, on made-up names -- and,
 * read as source because only CI compiles :app, that the phone's dialog takes these routes and no other.
 */
class MoveRouteTest {

    private val en = Tx.EN
    private val notHere = "This project's site cannot be changed from this phone: S5 does not hold the project and it was not made here."
    private val onWay = "A photo of this project has not reached S5 yet — move it once it has; not moved"
    private val pickAnother = "Not moved: pick another site, or type a new site's name."
    private val signIn = "Not signed in to S5, so not saved — sign in (menu → Server) and Save again"
    private val t = "2026-11-20T10:00:00.000Z"

    /** A project's move: by default one S5 lists, at "Flat 9 Tower", moved to "Flat 9" by a signed-in phone with no ERP pair. */
    private fun move(
        project: String = "PROJ-T1", held: Held? = Held("Flat 9 Tower", t), local: Boolean = false, mastersOnly: Boolean = false,
        live: Boolean = false, signedIn: Boolean = true, demo: Boolean = false, from: String = "Flat 9 Tower", to: String = "Flat 9",
        onWay: Boolean = false, listRead: Boolean = false, tx: Tx = en,
    ) = Renames.moveRoute(tx, project, held, local, mastersOnly, live, signedIn, demo, from, to, onWay, listRead)

    // ---- S5 lists the project ------------------------------------------------------------------------------------

    @Test fun `a project S5 lists moves by ONE write to S5, with that project's own base - never the bench, never the phone alone`() {
        assertEquals(RenameRoute.S5(listOf(Write("PROJ-T1", "Flat 9 Tower")), readFirst = false), move())
        // the base is S5's name for THIS project exactly as S5 gave it: not tidied, not the name the phone groups it under
        assertEquals(listOf(Write("PROJ-T1", "flat_9  tower ")), assertIs<RenameRoute.S5>(move(held = Held("flat_9  tower ", t))).writes)
        // a phone-minted key is S5's id like any other
        assertEquals(RenameRoute.S5(listOf(Write("S5-aaaaaaaaaa", "Shop 3")), readFirst = false), move(project = "S5-aaaaaaaaaa", held = Held("Shop 3", t), from = "Shop 3"))
        // whatever else is true of it -- a row of the phone's behind it, an ERP bootstrap's entry behind it, a live pair --
        // S5 holds the name, so S5 is told: one write, one project
        for (local in listOf(true, false)) for (live in listOf(true, false)) for (masters in listOf(true, false)) {
            val r = assertIs<RenameRoute.S5>(move(local = local, live = live, mastersOnly = masters), "local=$local live=$live mastersOnly=$masters")
            assertEquals(1, r.writes.size); assertFalse(r.readFirst)
        }
        assertEquals(Line.S5, Renames.line(move())); assertFalse(Renames.shut(move()))
    }

    @Test fun `a project in the (no site) bucket gets its site the same way - the base is '' because S5 holds no name for it`() {
        // S5 lists the project and holds NO name: the tree files it under the bucket, and the write's base is ''
        val r = move(project = "PROJ-T2", held = Held("", ""), from = "(no site)", to = "Flat 9")
        assertEquals(RenameRoute.S5(listOf(Write("PROJ-T2", "")), readFirst = false), r)
        // the same for a project the phone's own row names while S5 holds none
        assertEquals(RenameRoute.S5(listOf(Write("PROJ-T2", "")), readFirst = false), move(project = "PROJ-T2", held = Held("", ""), local = true, from = "Main site"))
        // the body is the first-name PUT S5 takes: {site, base: ""}
        assertEquals(linkedMapOf<String, Any?>("site" to "Flat 9", "base" to ""), Renames.siteBody("Flat 9", r.let { (it as RenameRoute.S5).writes[0].base }))
    }

    @Test fun `signed out - a project S5 lists is refused at once with the sign-in line, and nothing is moved on the phone instead`() {
        for (local in listOf(true, false)) {
            val r = move(signedIn = false, local = local)
            assertEquals(RenameRoute.Blocked(signIn, Line.S5), r, "local=$local")
            assertFalse(Renames.shut(r), "the Site row stays live: the reason is said when a site is picked")
        }
    }

    // ---- the phone's own row -----------------------------------------------------------------------------------

    @Test fun `a row made on this phone that S5 has not named is moved on the phone - with a signal or without`() {
        for (signed in listOf(true, false)) for (live in listOf(true, false))
            assertEquals(RenameRoute.Local, move(project = "", held = null, local = true, signedIn = signed, live = live), "signed=$signed live=$live")
        assertEquals(Line.PHONE, Renames.line(move(project = "", held = null, local = true)))
        // an id with no name held is not S5's word that it lists the project: the row is still the phone's
        assertEquals(RenameRoute.Local, move(project = "S5-bbbbbbbbbb", held = null, local = true))
    }

    @Test fun `a capture of the row is keyed for S5 and S5's list has not named it - the list is read first, then the move waits or goes to S5`() {
        // the key is a hash of client, site and title: moved now, the next capture would mint a second S5 project
        val first = move(project = "", held = null, local = true, onWay = true)
        assertEquals(RenameRoute.S5(emptyList(), readFirst = true), first)
        // S5's list read for this move, and the project is listed now: it is S5's, and takes the one write
        assertEquals(RenameRoute.S5(listOf(Write("S5-aaaaaaaaaa", "Shop 3")), readFirst = false),
            move(project = "S5-aaaaaaaaaa", held = Held("Shop 3", t), local = true, from = "Shop 3", listRead = true))
        // read, and still not listed: refused under the row, saying why -- never a move of the row alone
        val still = move(project = "", held = null, local = true, onWay = true, listRead = true)
        assertEquals(RenameRoute.Blocked(onWay, Line.S5), still); assertFalse(Renames.shut(still))
        // signed out: the list cannot be read, and that is said
        for (read in listOf(true, false))
            assertEquals(RenameRoute.Blocked(signIn, Line.S5), move(project = "", held = null, local = true, onWay = true, signedIn = false, listRead = read))
        // no capture keyed: the list read changes nothing for a row that is only the phone's
        assertEquals(RenameRoute.Local, move(project = "", held = null, local = true, listRead = true))
    }

    // ---- an entry of an ERP bootstrap that S5 does not list -------------------------------------------------------

    @Test fun `a bootstrap-only entry - with no live pair the phone's copy takes the name, with a live pair the row is shut`() {
        // NO LIVE PAIR. After slice F4a such an entry stays on the phone only while a capture of it has not reached a
        // server (TreeKeep.keepsProject): the phone's copy of its site name is what its first upload carries to S5
        for (signed in listOf(true, false)) for (local in listOf(true, false))
            assertEquals(RenameRoute.Local, move(held = null, mastersOnly = true, live = false, signedIn = signed, local = local), "signed=$signed local=$local")
        assertNull(Renames.moveShut(en, "PROJ-T1", null, hasLocalRow = false, mastersOnly = true, erpLive = false, demo = false))
        // A LIVE PAIR: its site is the bench's, the bench moves no project, and the next bootstrap would put the old
        // site back -- shut, and never a move of the phone's hidden row that would say Moved and change nothing on screen
        for (local in listOf(true, false)) {
            val r = move(held = null, mastersOnly = true, live = true, local = local)
            assertEquals(RenameRoute.Blocked(notHere, Line.SHUT), r, "local=$local")
            assertTrue(Renames.shut(r))
        }
        assertEquals(notHere, Renames.moveShut(en, "PROJ-T1", null, hasLocalRow = true, mastersOnly = true, erpLive = true, demo = false))
    }

    // ---- shut ---------------------------------------------------------------------------------------------------

    @Test fun `the Site row is shut for the demo and for a row that is nobody's - with its sentence, before any site is picked`() {
        // the demo's project: whatever else is claimed for it
        for (held in listOf<Held?>(null, Held("Demo Flat", t))) for (local in listOf(true, false)) for (masters in listOf(true, false)) {
            assertEquals(notHere, Renames.moveShut(en, "PROJ-T1", held, local, masters, erpLive = false, demo = true))
            assertEquals(RenameRoute.Blocked(notHere, Line.SHUT), move(held = held, local = local, mastersOnly = masters, demo = true))
        }
        // not S5's, not an entry of the masters, not made here: nothing a move could change
        assertEquals(notHere, Renames.moveShut(en, "", null, hasLocalRow = false, mastersOnly = false, erpLive = false, demo = false))
        assertEquals(RenameRoute.Blocked(notHere, Line.SHUT), move(project = "", held = null))
        // a name held for no id is nobody's project
        assertEquals(RenameRoute.Blocked(notHere, Line.SHUT), move(project = " ", held = Held("Flat 9", t)))
        // and what CAN be moved is not shut
        assertNull(Renames.moveShut(en, "PROJ-T1", Held("", ""), hasLocalRow = false, mastersOnly = false, erpLive = false, demo = false))
        assertNull(Renames.moveShut(en, "PROJ-T1", Held("Flat 9", t), hasLocalRow = false, mastersOnly = false, erpLive = true, demo = false))
        assertNull(Renames.moveShut(en, "", null, hasLocalRow = true, mastersOnly = false, erpLive = false, demo = false))
        // the route's shut IS moveShut's sentence, for every combination of the facts
        for (held in listOf<Held?>(null, Held("", ""), Held("Flat 9 Tower", t))) for (id in listOf("", "PROJ-T1")) for (local in listOf(true, false))
            for (masters in listOf(true, false)) for (live in listOf(true, false)) for (demo in listOf(true, false)) {
                val why = Renames.moveShut(en, id, held, local, masters, live, demo)
                val r = move(project = id, held = held, local = local, mastersOnly = masters, live = live, demo = demo)
                if (why == null) assertFalse(Renames.shut(r), "$id $held $local $masters $live $demo")
                else assertEquals(RenameRoute.Blocked(why, Line.SHUT), r, "$id $held $local $masters $live $demo")
            }
    }

    // ---- the target ---------------------------------------------------------------------------------------------

    @Test fun `a blank target, the (no site) bucket and the site it is already at are refused - never sent, never Moved`() {
        val kinds = listOf(
            "S5's" to { to: String, from: String -> move(from = from, to = to) },
            "the phone's row" to { to: String, from: String -> move(project = "", held = null, local = true, from = from, to = to) },
            "a row with a capture on its way" to { to: String, from: String -> move(project = "", held = null, local = true, onWay = true, from = from, to = to) },
            "a bootstrap's entry" to { to: String, from: String -> move(held = null, mastersOnly = true, from = from, to = to) },
        )
        for ((what, route) in kinds) {
            for (to in listOf("", "   ", "(no site)", " (No  Site) ", "(NO_SITE)"))
                assertEquals(RenameRoute.Blocked(pickAnother, Line.PHONE), route(to, "Flat 9 Tower"), "$what to '$to'")
            // the same site, as the catalogue compares names
            for (to in listOf("Flat 9 Tower", " flat 9  tower ", "FLAT_9_TOWER"))
                assertEquals(RenameRoute.Blocked(pickAnother, Line.PHONE), route(to, "Flat 9 Tower"), "$what to '$to'")
            // out of the bucket to a named site is a move like any other
            assertTrue(route("Flat 9", "(no site)") !is RenameRoute.Blocked, what)
            assertFalse(Renames.shut(route("", "Flat 9 Tower")), "$what: the row stays live, another site can be picked")
        }
        // a shut project says its own reason, whatever was picked
        assertEquals(RenameRoute.Blocked(notHere, Line.SHUT), move(demo = true, to = ""))
    }

    @Test fun `a move never goes to the bench, and never sends more than the one project`() {
        for (id in listOf("", "PROJ-T1", "S5-aaaaaaaaaa")) for (held in listOf<Held?>(null, Held("", ""), Held("Flat 9 Tower", t)))
            for (local in listOf(true, false)) for (masters in listOf(true, false)) for (live in listOf(true, false)) for (signed in listOf(true, false))
                for (demo in listOf(true, false)) for (way in listOf(true, false)) for (read in listOf(true, false)) for (to in listOf("Flat 9", "", "Flat 9 Tower")) {
                    val r = move(id, held, local, masters, live, signed, demo, "Flat 9 Tower", to, way, read)
                    assertTrue(r !is RenameRoute.Legacy, "the bench is not told of a move")
                    if (r is RenameRoute.S5) {
                        assertTrue(r.writes.size <= 1, "one project, one write")
                        assertEquals(r.writes.isEmpty(), r.readFirst, "nothing to send only while S5's list is still to be read")
                        r.writes.forEach { w -> assertEquals(id, w.project); assertEquals(held?.site, w.base) }
                    }
                }
    }

    // ---- the sites offered, and what a typed name means ---------------------------------------------------------

    @Test fun `the sites offered are the client's named sites but the one it is at - never the bucket, each once, in the tree's order`() {
        val sites = listOf("Flat 9", "Row House 4", "(no site)", " flat 9 ", "", "Shop 3")
        assertEquals(listOf("Row House 4", "Shop 3"), Renames.siteChoices(sites, "Flat 9"))
        assertEquals(listOf("Flat 9", "Shop 3"), Renames.siteChoices(sites, "row_house  4"))
        // a project in the bucket is offered every named site
        assertEquals(listOf("Flat 9", "Row House 4", "Shop 3"), Renames.siteChoices(sites, "(no site)"))
        // a client with one site, and the project at it: nothing to tap, a new site is typed
        assertEquals(emptyList(), Renames.siteChoices(listOf("Flat 9"), "Flat 9"))
        assertEquals(emptyList(), Renames.siteChoices(emptyList(), "Flat 9"))
    }

    @Test fun `a typed name that is one of the client's sites means that site, in its own spelling`() {
        val sites = listOf("Flat 9", "Row House 4", "(no site)")
        assertEquals("Flat 9", Renames.moveTarget(" flat  9 ", sites))
        assertEquals("Row House 4", Renames.moveTarget("ROW_HOUSE_4", sites))
        assertEquals("Shop 3", Renames.moveTarget("  Shop 3 ", sites), "a site the client does not have: as typed, trimmed")
        // the bucket typed is not turned into anything else: the route refuses it
        assertEquals("(no site)", Renames.moveTarget("(no site)", sites))
        assertEquals(RenameRoute.Blocked(pickAnother, Line.PHONE), move(to = Renames.moveTarget("(no site)", sites)))
        assertEquals("", Renames.moveTarget("   ", sites))
    }

    // ---- the words ----------------------------------------------------------------------------------------------

    @Test fun `the four new texts are whole keys, read in a glance, and wait for their Marathi in English`() {
        assertEquals(notHere, en.app_project_site_not_changed_here)
        assertEquals(onWay, en.app_project_capture_on_way_not_moved)
        assertEquals(pickAnother, en.app_not_moved_pick_another_site)
        assertEquals("Moved to Flat 9", en.app_msg_moved_to_x("Flat 9"))
        // English first (Amit, 16 Nov 2026): no Marathi is composed yet, so under मराठी each reads its English (mr-pending.tsv)
        val mr = Tx.MR
        assertEquals(notHere, mr.app_project_site_not_changed_here); assertEquals(onWay, mr.app_project_capture_on_way_not_moved)
        assertEquals(pickAnother, mr.app_not_moved_pick_another_site); assertEquals("Moved to Flat 9", mr.app_msg_moved_to_x("Flat 9"))
        for (s in listOf(notHere, onWay, pickAnother)) {
            assertTrue(s.length <= 140, "too long to read on site: ${s.length}")
            assertFalse(s.contains("server") || s.contains("configured") || s.contains("ERP"), s)
        }
        // the route's refusals follow the language like every other
        assertEquals(RenameRoute.Blocked(mr.refuse_site_signed_out, Line.S5), move(signedIn = false, tx = mr))
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

    @Test fun `what the rule is asked with is gathered in one place - the tree's project, S5's name for it, the phone's row and its captures`() {
        val cat = app("Catalogue.kt")
        val facts = between(cat, "private fun moveFacts(", "fun moveShut(")
        // an entry of the masters: S5's name for its id, and whether it is a bootstrap's alone
        assertTrue(facts.contains("if (!p.local && p.serverId.isNotBlank()) {") && facts.contains("val held = S5Sites.held(appContext, p.serverId)"))
        assertTrue(facts.contains("mastersOnly = held == null && bootstrapEntry(masters, p.serverId)"), "a bootstrap's entry S5 does not list")
        assertTrue(between(cat, "private fun bootstrapEntry(", "private fun moveFacts(").contains("return !o.optBoolean(S5Masters.FROM_S5)"), "never an entry S5's own list added")
        // the phone's own row: the id a capture of it carries, else the key it would have on S5 -- and S5's name for that
        assertTrue(facts.contains("same(localSiteFor(c.customerName, c.projectTitle), p.site)") && facts.contains("!TourDemo.isDemoProject(c.customerName, c.projectTitle)"), "the row's captures, never the demo's")
        assertTrue(facts.contains("val id = carried.ifEmpty { S5Auth.localKey(p.client, p.site, p.title) }") && facts.contains("val held = S5Sites.held(appContext, id)"))
        assertTrue(facts.contains("onWay = held == null && Renames.minted(carried)"), "keyed for S5, and S5's list has not named it here")
        // ONE caller of each rule, both through the gathering
        assertEquals(1, count(cat + app("MainActivity.kt") + app("TreeShell.kt"), "Renames.moveRoute("), "one caller of the route")
        assertEquals(1, count(cat + app("MainActivity.kt") + app("TreeShell.kt"), "Renames.moveShut("), "one caller of the shut")
        assertTrue(cat.contains("from = p.site, to = to, onWay = f.onWay, listRead = listRead)"), "from is the site the TREE lists it under")
        // the phone's row moved: one row per client, site and project, and no address carried to a site nobody named
        val local = between(cat, "fun moveLocal(client: String, site: String, project: String, to: String): Boolean {", "fun setMastersSite(")
        assertTrue(local.contains("if (!Renames.named(n) || same(site, n)) return false"), "never into the bucket, never onto itself")
        assertTrue(local.contains("if (touched) writeLocals(out)") && local.contains("return touched"), "what it moved is its answer")
        // the phone's copy of a bootstrap entry: its site_name alone, and never an entry S5's list added
        val entry = between(cat, "fun setMastersSite(masters: JSONObject, projectId: String, to: String): Boolean {", "fun siteFor(")
        assertTrue(entry.contains("if (o.optString(\"project\") != projectId || o.optBoolean(S5Masters.FROM_S5)) continue") && entry.contains("o.put(\"site_name\", n)"))
        assertEquals(1, count(entry, ".put("), "the entry's link to the bench's site row, and everything else, is not touched")
    }

    @Test fun `the phone's dialog moves ONE project by Renames' route - one PUT through SiteRename, every failure said under the Site row`() {
        val main = app("MainActivity.kt")
        val move = between(main, "fun moveProject(t: RenameTarget, typed: String) {", "// WHERE A RENAME GOES IS DECIDED IN ONE PLACE")
        // the project is re-found in the tree at every try, and the target is the client's own spelling of a site it has
        assertTrue(move.contains("val p = cat.projectOf(masters, t.serverId, t.client, t.site, t.project)"))
        assertTrue(move.contains("val to = Renames.moveTarget(typed, cat.sitesOf(masters, p.client).map { it.name })"))
        assertTrue(move.contains("when (val route = cat.moveRoute(tx, masters, queue, p, to, signedIn = signedIn, erpLive = S5Masters.erpLive(context))) {"), "the route is :pano's")
        for (branch in listOf("RenameRoute.Local -> moveOnPhone(p, to)", "is RenameRoute.S5 -> {", "is RenameRoute.Blocked -> {", "is RenameRoute.Legacy ->"))
            assertTrue(move.contains(branch), "the branch `$branch`")
        // S5's route: the writes :pano routed (one), sent by the site rename's own sender; success is S5's answer alone
        assertTrue(move.contains("val outcome = withContext(Dispatchers.IO) { SiteRename.rename(context, s5.writes, to) }"))
        assertEquals(1, count(move, "SiteRename.rename("))
        val done = between(move, "if (outcome is Renames.Outcome.Done) {", "} else {")
        assertTrue(done.contains("val name = outcome.site") && done.contains("moved(at, name)"), "the site applied is S5's own answer")
        assertTrue(done.contains("cat.moveLocal(at.client, rowSite, at.title, name)"), "the phone's own row follows its project")
        assertFalse(move.contains("FrappeClient.load") || move.contains("renameNode") || move.contains("bootstrap()"), "the bench is not called for a move")
        // S5's list first only when the route says so, and the route asked again on S5's own answer
        val first = between(move, "if (route.readFirst) {", "if (go == RenameRoute.Local) {")
        assertTrue(first.contains("S5Masters.fetch(context, erpLive = S5Masters.erpLive(context))?.also { store.saveMasters(it) }") && first.contains("listRead = true)"))
        assertTrue(first.contains("val again = cat.projectOf(fresh, t.serverId, t.client, t.site, t.project)"), "the project is re-found in the tree the read gave")
        // every failure is said in the dialog: never a banner, never a silent return
        assertEquals(7, count(move, "moveFailed {"), "no project, no list, gone after the list, blocked after it, S5's answer, a blocked route, and the route that cannot be")
        assertFalse(move.contains("lastResult"), "the banner carries a move's success only")
        // the local move is CHECKED, and it carries the chosen rooms where the site it joins has none
        val here = between(main, "fun moveOnPhone(p: Catalogue.Project, to: String) {", "fun moveProject(")
        assertTrue(here.contains("val row = cat.moveLocal(p.client, p.site, p.title, to)") && here.contains("cat.setMastersSite(m, p.serverId, to).also { if (it) store.saveMasters(m) }"))
        val failed = here.indexOf("if (!row && !entry) {"); val ok = here.indexOf("moved(p, to)")
        assertTrue(failed in 0 until ok && between(here, "if (!row && !entry) {", "moved(p, to)").contains("return"), "a move that moved nothing is a failure, never Moved")
        assertTrue(here.contains("cat.moveRoomScope(p.client, p.site, to, leave = cat.projectsOf(store.masters(), p.client, p.site).isEmpty())"))
        // moved: re-read, the place follows the project, an open project is re-read from the tree, and it is said
        val moved = between(main, "fun moved(p: Catalogue.Project, name: String) {", "fun moveOnPhone(")
        val reload = moved.indexOf("reload()"); val nav = moved.indexOf("navSite = now?.site ?: name"); val said = moved.indexOf("lastResult = { tx -> tx.app_msg_moved_to_x(Masters.site_name(tx, name)) }")
        assertTrue(reload in 0 until nav && nav < said && moved.indexOf("closeRename()") > said, "re-read, followed, said, closed")
        assertTrue(moved.contains("if (navProject != null) navProject = now"), "an open project is the tree's, not its old copy")
        assertTrue(main.contains("moveWhy = null\n        deleteBlocker = null"), "the reason goes when the dialog closes")
    }

    @Test fun `a project's dialog has a Site row of its own - live whether or not the name can be edited, shut only by its own rule`() {
        val main = app("MainActivity.kt")
        val open = between(main, "renaming?.let { t ->", "onSave = { newName ->")
        assertTrue(open.contains("if (t.kind == \"project\") cat.projectOf(masters, t.serverId, t.client, t.site, t.project) else null"), "a project's dialog only")
        assertTrue(open.contains("moving?.let { cat.moveShut(tx, masters, queue, it, erpLive = S5Masters.erpLive(context)) }"), "why it is shut is asked when the dialog opens")
        assertTrue(open.contains("moving?.let { m -> Renames.siteChoices(cat.sitesOf(masters, m.client).map { it.name }, m.site) }.orEmpty()"))
        for (arg in listOf("site = moving?.site,", "siteChoices = moveSites,", "moveShut = moveShut.orEmpty(),", "moveWhy = moveWhy?.invoke(tx),",
            "onMoveEdit = { moveWhy = null },", "onMove = if (moving == null) null else ({ to: String -> moveProject(t, to) }),"))
            assertTrue(open.contains(arg), "the dialog is handed `$arg`")
        val dialog = between(app("TreeShell.kt"), "fun RenameDialog(", "internal const val MM_PER_IN")
        val row = between(dialog, "if (site != null && onMove != null) {", "if (onDelete != null) {\n                    Spacer(Modifier.height(14.dp))")
        // the row is its own question: nothing of it reads whether the NAME is shut
        assertFalse(row.contains("isShut") || row.contains("changed"), "a move is not a rename")
        assertTrue(row.contains("val canMove = moveShut.isEmpty()") && row.contains("if (!canMove) Text(moveShut,"), "shut by Renames.moveShut alone, saying why")
        assertTrue(row.contains("if (canMove) Icon(painterResource(R.drawable.ic_mcft_down)"), "a chevron only where a tap opens something")
        assertTrue(row.contains("Text(Masters.site_name(tx, site),"), "the site as the tree names it, the bucket through its master")
        // the picker: the client's other sites, one tap each, and a typed new one -- both hand the name to onMove
        assertTrue(row.contains("siteChoices.forEach { s ->") && row.contains("Modifier.clickableRow { onMove(s) }"))
        assertTrue(row.contains("onClick = { onMove(newSite.trim()) }") && row.contains("TextButton(enabled = newSite.isNotBlank() && !busy,"))
        assertTrue(row.contains("onValueChange = { newSite = it; onMoveEdit() },"), "typing another site clears the reason")
        assertTrue(row.contains("if (moveWhy != null) Text("), "why the last move did not happen, under the row")
    }
}
