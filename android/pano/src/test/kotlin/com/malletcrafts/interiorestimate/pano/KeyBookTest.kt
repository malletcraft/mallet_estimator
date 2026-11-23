package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.KeyBook.Book
import com.malletcrafts.interiorestimate.pano.KeyBook.Names
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * AN S5 PROJECT KEY IS REMEMBERED, NEVER RE-DERIVED (port 63, the review of F4; android/docs/port63/DESIGN-tree.md §10).
 * The rule first ([KeyBook]), case by case on made-up names; then -- read as source, because only CI compiles :app --
 * that the phone mints a key only where it remembers none, remembers the one it mints, and reads its book wherever it
 * used to work a key out from names.
 */
class KeyBookTest {

    private val k1 = "S5-aaaaaaaaaa"
    private val k2 = "S5-bbbbbbbbbb"
    private val none: (String) -> Boolean = { false }

    private fun cap(project: String, client: String, title: String) = TreeKeep.Cap(project, client, title, sent = true)

    @Test fun `a key is remembered by its client and title - never by its site, the part a move or a site rename changes`() {
        val book = KeyBook.take(Book(), k1, "Dev Kulkarni", "balcony")
        assertEquals(k1, KeyBook.keyFor(book, "Dev Kulkarni", "balcony"))
        // names as the catalogue compares them
        assertEquals(k1, KeyBook.keyFor(book, " dev_kulkarni ", "BALCONY"))
        assertEquals(Names("Dev Kulkarni", "balcony"), KeyBook.namesOf(book, k1))
        // another title, another client: nothing is remembered, so (and only then) a key is minted
        assertNull(KeyBook.keyFor(book, "Dev Kulkarni", "loft")); assertNull(KeyBook.keyFor(book, "Asha Rao", "balcony"))
        assertNull(KeyBook.namesOf(book, k2)); assertNull(KeyBook.namesOf(book, ""))
        // the fold holds no site at all: there is nothing in the book a move could make stale
        assertEquals("dev kulkarni|balcony", KeyBook.fold(" Dev  Kulkarni", "Balcony "))
        assertEquals(setOf("dev kulkarni|balcony"), book.byName.keys)
    }

    @Test fun `the reviewer's case - a project moved to another site and photographed again is the SAME S5 project`() {
        // minted at "Shop 3" (the hash of client|site|title), remembered; the project is then moved to "Flat 2" and its
        // captures leave the phone. The names now hash to another key -- and the book still answers the first.
        var book = KeyBook.take(Book(), k1, "Dev Kulkarni", "balcony")
        book = KeyBook.learn(book, emptyList(), none)                      // no capture is left on the phone
        assertEquals(k1, KeyBook.keyFor(book, "Dev Kulkarni", "balcony"), "the next capture takes the remembered key, not a new one")
        // and S5's list, which names the project with NO client, is filed under the client the key was remembered for
        assertEquals("Dev Kulkarni", KeyBook.namesOf(book, k1)?.client)
    }

    @Test fun `nothing is remembered for a blank id, client or title, and the book is the same object when nothing changed`() {
        val book = KeyBook.take(Book(), k1, "Dev Kulkarni", "balcony")
        for ((id, c, t) in listOf(Triple("", "Dev Kulkarni", "loft"), Triple("  ", "Dev Kulkarni", "loft"), Triple(k2, " ", "loft"), Triple(k2, "Dev Kulkarni", "")))
            assertSame(book, KeyBook.take(book, id, c, t), "'$id' '$c' '$t'")
        assertSame(book, KeyBook.take(book, k1, "Dev Kulkarni", "balcony"), "the same key for the same names writes nothing")
        assertSame(book, KeyBook.learn(book, listOf(cap(k1, "Dev Kulkarni", "balcony"), cap("", "Dev Kulkarni", "loft")), none))
    }

    @Test fun `of two keys claimed for one client and title, the one S5 lists wins - else the one remembered first stays`() {
        assertEquals(k1, KeyBook.wins(k1, k2, none), "neither listed: the first stays")
        assertEquals(k1, KeyBook.wins(k1, k2) { true }, "both listed: the first stays")
        assertEquals(k2, KeyBook.wins(k1, k2) { it == k2 }, "S5 lists the comer and not the stored: S5's project")
        assertEquals(k1, KeyBook.wins(k1, k2) { it == k1 })
        assertEquals(k2, KeyBook.wins("", k2, none), "nothing stored: the comer")
        // through take: the forward entry is the winner's, and BOTH ids still say whose they are
        val first = KeyBook.take(Book(), k1, "Dev Kulkarni", "balcony")
        val kept = KeyBook.take(first, k2, "Dev Kulkarni", "balcony", none)
        assertEquals(k1, KeyBook.keyFor(kept, "Dev Kulkarni", "balcony"))
        assertEquals("Dev Kulkarni", KeyBook.namesOf(kept, k2)?.client, "a second id is never left with no client to be filed under")
        val swapped = KeyBook.take(first, k2, "Dev Kulkarni", "balcony") { it == k2 }
        assertEquals(k2, KeyBook.keyFor(swapped, "Dev Kulkarni", "balcony"))
        // a bench docname no longer on S5 against a minted key S5 lists: S5's
        assertEquals(k2, KeyBook.keyFor(KeyBook.learn(Book(), listOf(cap("PROJ-T1", "Asha Rao", "kitchen"), cap(k2, "Asha Rao", "kitchen"))) { it == k2 }, "Asha Rao", "kitchen"))
    }

    @Test fun `every capture that carries an id is read into the book, oldest first - the keys minted before the book existed are in it`() {
        val caps = listOf(cap(k1, "Dev Kulkarni", "balcony"), cap("", "Dev Kulkarni", "loft"), cap("PROJ-T1", "Asha Rao", "kitchen"), cap(k2, "Dev Kulkarni", "balcony"))
        val book = KeyBook.learn(Book(), caps, none)
        assertEquals(k1, KeyBook.keyFor(book, "Dev Kulkarni", "balcony"), "the key the project was first shot under")
        assertEquals("PROJ-T1", KeyBook.keyFor(book, "Asha Rao", "kitchen"), "an office id is an id like any other")
        assertNull(KeyBook.keyFor(book, "Dev Kulkarni", "loft"), "a capture with no id yet says nothing")
        assertEquals(setOf(k1, k2, "PROJ-T1"), book.byId.keys)
        // read again, with or without the captures, it is the same book
        assertEquals(book, KeyBook.learn(book, caps, none)); assertSame(book, KeyBook.learn(book, emptyList(), none))
    }

    @Test fun `an id has one client and title at a time - a capture renamed with its row moves the entry and leaves no stale name behind`() {
        var book = KeyBook.take(Book(), k1, "Esha Pal", "tv unit")
        // the same id now says other names (the row was renamed, and its capture with it)
        book = KeyBook.take(book, k1, "Esha Paul", "TV wall")
        assertEquals(k1, KeyBook.keyFor(book, "Esha Paul", "TV wall"))
        assertNull(KeyBook.keyFor(book, "Esha Pal", "tv unit"), "a NEW project typed under the old names is not joined to the renamed one")
        assertEquals(Names("Esha Paul", "TV wall"), KeyBook.namesOf(book, k1))
        assertEquals(1, book.byName.size)
    }

    @Test fun `a local rename takes the row's remembered keys with it - the key stays, the names it is for move`() {
        val book = KeyBook.learn(Book(), listOf(cap(k1, "Esha Pal", "tv unit"), cap(k2, "Esha Pal", "shoe rack"), cap("S5-cccccccccc", "Dev Kulkarni", "balcony")), none)
        val client = KeyBook.renamed(book, "client", "esha_pal", "", "Esha Paul")
        assertEquals(k1, KeyBook.keyFor(client, "Esha Paul", "tv unit")); assertEquals(k2, KeyBook.keyFor(client, "Esha Paul", "shoe rack"))
        assertNull(KeyBook.keyFor(client, "Esha Pal", "tv unit")); assertEquals("Esha Paul", KeyBook.namesOf(client, k1)?.client)
        assertEquals("S5-cccccccccc", KeyBook.keyFor(client, "Dev Kulkarni", "balcony"), "another client's entry is not touched")
        val project = KeyBook.renamed(book, "project", "Esha Pal", "TV  unit", "TV wall")
        assertEquals(k1, KeyBook.keyFor(project, "Esha Pal", "TV wall")); assertNull(KeyBook.keyFor(project, "Esha Pal", "tv unit"))
        assertEquals(k2, KeyBook.keyFor(project, "Esha Pal", "shoe rack"), "the client's other project is not touched")
        // a site rename changes no entry: no key is kept by site. Nor does a blank name, or a kind that is neither
        assertSame(book, KeyBook.renamed(book, "site", "Esha Pal", "", "Row 5")); assertSame(book, KeyBook.renamed(book, "client", "Esha Pal", "", "  "))
        assertSame(book, KeyBook.renamed(book, "room", "Esha Pal", "", "x"))
        // renamed ONTO names another key is already remembered for: that one stays, and the moved id still says its names
        val onto = KeyBook.renamed(book, "project", "Esha Pal", "tv unit", "shoe rack")
        assertEquals(k2, KeyBook.keyFor(onto, "Esha Pal", "shoe rack")); assertEquals(Names("Esha Pal", "shoe rack"), KeyBook.namesOf(onto, k1))
    }

    @Test fun `one key is forgotten, and only it`() {
        val book = KeyBook.learn(Book(), listOf(cap(k1, "Hari Menon", "test unit"), cap(k2, "Dev Kulkarni", "balcony")), none)
        val after = KeyBook.forget(book, k1)
        assertNull(KeyBook.keyFor(after, "Hari Menon", "test unit")); assertNull(KeyBook.namesOf(after, k1))
        assertEquals(k2, KeyBook.keyFor(after, "Dev Kulkarni", "balcony")); assertEquals("Dev Kulkarni", KeyBook.namesOf(after, k2)?.client)
        assertSame(book, KeyBook.forget(book, "S5-zzzzzzzzzz"), "a key the book does not hold changes nothing")
        assertSame(book, KeyBook.forget(book, ""))
        // a second id that lost the name to another: forgetting it leaves the winner's name alone
        val two = KeyBook.take(book, "S5-dddddddddd", "Dev Kulkarni", "balcony")
        assertEquals(k2, KeyBook.keyFor(KeyBook.forget(two, "S5-dddddddddd"), "Dev Kulkarni", "balcony"))
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

    /** [src] without its comment lines: a rule named in a comment is not a caller of it. */
    private fun code(src: String) = src.lines().filterNot { it.trim().startsWith("//") || it.trim().startsWith("*") || it.trim().startsWith("/*") }.joinToString("\n")

    @Test fun `the phone mints a key only where the masters and its book hold none, and remembers the one it mints`() {
        val session = app("S5Session.kt")
        val keyFor = between(session, "fun keyFor(context: Context, client: String, title: String): String {", "fun localKey(")
        val masters = keyFor.indexOf("if (Catalogue.keyOf(c, \"\", p.optString(\"title\")) == want) return id")
        val book = keyFor.indexOf("KeyBook.keyFor(S5Keys.learn(context, store.all()), client, title)?.let { return it }")
        val mint = keyFor.indexOf("val minted = localKey(client, Catalogue(context).localSiteFor(client, title), title)")
        val keep = keyFor.indexOf("S5Keys.remember(context, minted, client, title)")
        assertTrue(masters in 0 until book && book < mint && mint < keep && keep < keyFor.indexOf("return minted"),
            "a masters entry, else the remembered key, and only then a mint -- remembered before it is answered")
        // ONE mint in the whole app, and one place the tree asks what a phone row is called on S5
        val all = listOf("S5Session.kt", "Catalogue.kt", "MainActivity.kt", "SyncWorker.kt", "AsIsUpload.kt", "S5Keys.kt", "S5Sites.kt", "SiteRename.kt").associateWith { code(app(it)) }
        assertEquals(mapOf("S5Session.kt" to 1, "Catalogue.kt" to 1),
            all.mapValues { (_, src) -> Regex("""(?<![A-Za-z.])localKey\(|S5Auth\.localKey\(""").findAll(src).count() - count(src, "fun localKey(") }.filterValues { it != 0 },
            "the hash of the names is worked out in keyFor's mint and in Catalogue.keyOfRow, nowhere else")
        val cat = app("Catalogue.kt")
        assertTrue(cat.contains("private fun keyOfRow(book: KeyBook.Book, client: String, site: String, title: String): String =\n        KeyBook.keyFor(book, client, title) ?: S5Auth.localKey(client, site, title)"),
            "the remembered key first; the hash only for a row no capture has been keyed for yet -- which is what keyFor will mint")
        assertEquals(4, count(code(cat), "keyOfRow("), "its declaration and its three readers: a site's S5 projects, a row's ties to S5, a move's facts")
    }

    @Test fun `S5's list files a project with no customer under the client its key was remembered for`() {
        val fetch = between(app("S5Session.kt"), "fun fetch(context: Context, erpLive: Boolean): JSONObject? = try {", "} catch (e: Exception) {")
        assertTrue(fetch.contains("val book = S5Keys.learn(context, captures)"), "the book, with this phone's captures taken in first")
        assertTrue(fetch.contains("val kept = KeyBook.namesOf(book, key)"))
        assertTrue(fetch.contains("val client = customer.ifBlank { mine?.customerName.orEmpty() }.ifBlank { kept?.client.orEmpty() }.ifBlank { NO_CLIENT }"),
            "S5's customer, else a capture's client, else the remembered client -- and only then the (no client) bucket")
        assertTrue(fetch.contains("val t = title.ifBlank { mine?.projectTitle.orEmpty() }.ifBlank { kept?.title.orEmpty() }"))
    }

    @Test fun `the book is fed by the mint, by every capture row, and by a local rename - and loses a key only to the delete that proved it gone`() {
        val keys = app("S5Keys.kt"); val main = app("MainActivity.kt")
        val learn = between(keys, "fun learn(context: Context, captures: List<CaptureStore.Capture>): KeyBook.Book {", "fun remember(")
        assertTrue(learn.contains(".filter { it.project.isNotBlank() && !TourDemo.isDemoProject(it.customerName, it.projectTitle) }") && learn.contains(".sortedBy { it.createdAt }"),
            "the rows that carry an id, never the demo's, oldest first")
        assertTrue(learn.contains("KeyBook.learn(b, caps) { id -> held.containsKey(id) }") && learn.contains("val held = S5Sites.all(context)"), "the rule is :pano's; listed is S5Sites holding the id")
        // the captures are read at the app's start, signal or none, and before a capture row is deleted
        assertTrue(between(main, "LaunchedEffect(configured) {", "// Back from the camera").contains("S5Keys.learn(context, store.all())"))
        val del = main.indexOf("S5Keys.learn(context, queue)\n                        store.delete(cap.deviceId)")
        assertTrue(del >= 0, "a capture's key and client are remembered before its row goes")
        // a local rename takes the entries with it, after its captures are re-keyed and before the tree is re-read
        val here = between(main, "fun renameHere(t: RenameTarget, newName: String) {", "renaming?.let { t ->")
        val names = here.indexOf("if (now != was) store.setNames(c.deviceId, now.client, now.title)"); val moved = here.indexOf("S5Keys.renamed(context, t.kind, t.client, t.project, newName)")
        assertTrue(names in 0 until moved && moved < here.indexOf("reload()"))
        // ONE remover of each store, both called by the Gone route's step alone
        assertEquals(1, count(code(main), "S5Keys.forget(")); assertEquals(1, count(code(main), "S5Sites.forget(")); assertEquals(1, count(code(main), "S5Masters.forgetKeys("))
        val gone = between(main, "fun removeHere(t: RenameTarget, gone: List<String>) {", "cat.removeLocal(t.kind, t.client, t.site, t.project)")
        assertTrue(gone.contains("if (gone.isNotEmpty()) {") && gone.contains("S5Masters.forgetKeys(context, gone)") && gone.contains("S5Sites.forget(context, k)") && gone.contains("S5Keys.forget(context, k)"),
            "only the keys the rule named, and only with the row's removal")
        for (f in listOf("S5Session.kt", "SyncWorker.kt", "SiteRename.kt", "QuoteStageSync.kt", "Catalogue.kt", "AsIsUpload.kt"))
            assertFalse(code(app(f)).contains("S5Keys.forget(") || code(app(f)).contains("S5Sites.forget("), "$f: no read of S5's list, and no sync, forgets a key")
        assertFalse(between(app("S5Session.kt"), "fun signOut(context: Context) {", "private val COOKIE").contains("S5Keys"), "nor does sign-out: the keys are not a person's")
    }

    @Test fun `a project renamed from inside it stays open, and its dialog has a visible way in from the project`() {
        val main = app("MainActivity.kt")
        val here = between(main, "fun renameHere(t: RenameTarget, newName: String) {", "renaming?.let { t ->")
        assertTrue(here.contains("else -> if (navProject != null) navProject = cat.projectOf(store.masters(), \"\", t.client, t.site, newName)"),
            "re-read from the tree where a project was open; from the list none is opened")
        assertFalse(between(here, "\"site\" -> { navSite = newName; navProject = null }", "closeRename()").contains("else -> navProject = null"))
        // the chip: the first of the project's chip row, the site through its master, opening the dialog the long press builds
        val rooms = between(app("BrowseScreens.kt"), "fun RoomsScreen(", "fun ProjectLineBox(")
        assertTrue(rooms.contains("onSite: (() -> Unit)? = null,"))
        val chip = rooms.indexOf("label = { Text(tx.app_site_x_open(Masters.site_name(tx, project.site))) })"); val shot = rooms.indexOf("label = { Text(tx.app_x_shot(shot.size)) })")
        assertTrue(chip in 0 until shot, "first, so it is on screen without a sideways scroll")
        assertTrue(between(rooms, "onSite?.let {", "AssistChip(onClick = { }, enabled = false,").contains("AssistChip(onClick = it,"))
        val open = between(main, "onSite = { renaming = RenameTarget(", "album = albumTile)")
        assertTrue(Regex(""""project", proj\.title, proj\.client, proj\.site, proj\.title,\s*proj\.serverId, serverKnown = !proj\.local\) \},""").containsMatchIn(open),
            "the same target the project list's long press builds")
        assertEquals("Site · Flat 9 ▾", com.malletcrafts.interiorestimate.pano.i18n.Tx.EN.app_site_x_open("Flat 9"))
    }
}
