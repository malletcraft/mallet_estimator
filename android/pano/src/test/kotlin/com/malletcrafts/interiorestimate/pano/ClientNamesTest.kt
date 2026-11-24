package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.ClientNames.Left
import com.malletcrafts.interiorestimate.pano.ClientNames.Outcome
import com.malletcrafts.interiorestimate.pano.ClientNames.Step
import com.malletcrafts.interiorestimate.pano.RenameRoute.Line
import com.malletcrafts.interiorestimate.pano.RenameRoute.Write
import com.malletcrafts.interiorestimate.pano.Renames.Held
import com.malletcrafts.interiorestimate.pano.Renames.Now
import com.malletcrafts.interiorestimate.pano.Renames.Verdict
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * THE CLIENT'S NAME IS S5'S, AND A CLIENT RENAME REALLY WORKS (port 63 slice 5; android/docs/port63/DESIGN-tree.md
 * §11): the name S5 takes, the request and every answer read into its verdict, the list row, S5's name read first,
 * the record's first value and the backfill, where a client's rename goes, how the phone's rows follow the name S5
 * holds, what a Save over several projects comes to and the lines it draws -- and, read as source because only CI
 * compiles :app, that the phone feeds, sends and follows by these rules and no other.
 *
 * The answers below are written from S5's source (mcft-interior-estimate functions/_lib/asis.js, its slice 5): the
 * client route answers as the site route does, under `client_name`. They are not captured from a run. Every name
 * here is made up.
 */
class ClientNamesTest {

    private val en = Tx.EN
    private val P1 = "PROJ-T1"
    private val P2 = "PROJ-T2"
    private val AT1 = "2026-11-24T06:10:00.000Z"
    private val AT2 = "2026-11-24T06:20:00.000Z"

    private fun tree(s: String): Any? = MiniJson.parse(s)
    private fun verdict(code: Int?, body: String?): Verdict = ClientNames.verdict(MiniRead, code, body?.let { runCatching { tree(it) }.getOrNull() })

    private val RENAMED = """{"ok":true,"written":true,"project":"PROJ-T1","client_name":"Client T2","client_name_by":"staff@mcft.test","client_name_at":"$AT2"}"""
    private val SAME = """{"ok":true,"written":false,"project":"PROJ-T1","client_name":"Client T2","client_name_by":"staff@mcft.test","client_name_at":"$AT2"}"""
    private val STALE = """{"error":"Renamed by lead@mcft.test meanwhile — now Client T3; not saved","client_name":"Client T3","client_name_by":"lead@mcft.test","client_name_at":"$AT2"}"""
    private val NONE = """{"error":"This project has no client name on S5 yet: send base \"\" to give it one; not saved","client_name":"","client_name_by":"","client_name_at":""}"""
    private val MEANWHILE = """{"error":"Changed meanwhile; not saved — send it again","client_name":"Client T1","client_name_by":"staff@mcft.test","client_name_at":"$AT1"}"""
    private val NOT_STAFF = """{"error":"Only MCFT staff can rename a client"}"""
    private val NEVER = """{"error":"This project has never been uploaded"}"""

    // ---- the name S5 takes ---------------------------------------------------------------------------------

    @Test fun `a typed name is folded onto one line as S5 folds it, and refused before anything is sent where S5 would refuse it`() {
        assertEquals("Client T1", ClientNames.fold("  Client T1  "))
        assertEquals("Client T1 second line", ClientNames.fold("Client T1 \r\n\r\n  second line\n"))
        assertEquals("", ClientNames.fold(" \n \r "))
        assertTrue(ClientNames.ok("Client T1"))
        assertTrue(ClientNames.ok("क्लायंट T1"), "Devanagari, with its joiners, is a name")
        assertTrue(ClientNames.ok("a‍b"), "a zero-width joiner is not a control character")
        assertTrue(ClientNames.ok("x".repeat(140)))
        assertFalse(ClientNames.ok("x".repeat(141)), "S5's cap, in UTF-16 units")
        assertFalse(ClientNames.ok(""))
        for (bad in listOf("a\tb", "a\u0000b", "a\u007Fb", "a\u0085b", "a b", "a b"))
            assertFalse(ClientNames.ok(bad), "a control character or a line separator is refused")
        assertTrue(ClientNames.ok("a😀b"), "a whole surrogate pair is a character")
        assertFalse(ClientNames.ok("a\uD83Db"), "half a pair is refused")
        assertFalse(ClientNames.ok("a\uDE00b"))
        assertFalse(ClientNames.ok("ab\uD83D"))
        // what the Save sends: the folded name, or nothing
        assertEquals("Client T2", ClientNames.typed(" Client T2\n"))
        assertNull(ClientNames.typed("   "))
        assertNull(ClientNames.typed("x".repeat(141)))
        assertNull(ClientNames.typed("Client\tT2"))
        assertEquals("A client's name is 1 to 140 characters, on one line; not saved", en.refuse_client_name_bad)
    }

    // ---- the wire ------------------------------------------------------------------------------------------

    @Test fun `one write is PUT project client with the new name and that project's own base - and never the customer`() {
        val r = ClientNames.request(P1, "Client T2", "Client T1")
        assertEquals("PUT", r.method)
        assertEquals(listOf(P1, "client"), r.segments)
        assertEquals("/api/asis/<PROJ-T1>/<client>", r.path { "<$it>" })
        assertEquals(AlbumQueue.JSON, r.contentType)
        assertEquals(listOf("client_name", "base"), r.fields.keys.toList())
        assertEquals("Client T2", r.fields["client_name"]); assertEquals("Client T1", r.fields["base"])
        assertEquals("", ClientNames.request(P1, "Client T2", "").fields["base"], "'' while S5 holds none")
        assertFalse(r.fields.containsKey("customer"), "the label is not the ERP Customer id")
    }

    @Test fun `every answer of S5's is read into its verdict, under client_name`() {
        assertEquals(Verdict.Renamed(Now("Client T2", "staff@mcft.test", AT2)), verdict(200, RENAMED))
        assertEquals(Verdict.Same(Now("Client T2", "staff@mcft.test", AT2)), verdict(200, SAME))
        val stale = assertIs<Verdict.Stale>(verdict(409, STALE))
        assertEquals(Now("Client T3", "lead@mcft.test", AT2), stale.now)
        assertEquals("", assertIs<Verdict.Stale>(verdict(409, NONE)).now.site, "S5 holds none: the phone then holds none, and Save again sends base ''")
        assertEquals("Client T1", assertIs<Verdict.Stale>(verdict(409, MEANWHILE)).now.site)
        assertEquals(Verdict.Refused(403, "Only MCFT staff can rename a client"), verdict(403, NOT_STAFF))
        assertEquals(Verdict.Refused(404, "This project has never been uploaded"), verdict(404, NEVER))
        assertIs<Verdict.Refused>(verdict(400, """{"error":"client_name must be the client's name"}"""))
        // not S5's handler: nothing came back, a body that is not S5's, a site answer where a client's was asked, a 5xx
        assertEquals(Verdict.Offline, verdict(null, null))
        assertEquals(Verdict.Offline, verdict(200, "<html>"))
        assertEquals(Verdict.Offline, verdict(200, """{"ok":true,"written":true,"project":"PROJ-T1","site":"Demo Site","site_by":"x","site_at":"$AT1"}"""))
        assertEquals(Verdict.Offline, verdict(200, """{"ok":true,"written":true,"project":"PROJ-T1","client_name":"","client_name_by":"","client_name_at":""}"""), "a 200 names the client")
        assertEquals(Verdict.Offline, verdict(502, NOT_STAFF))
        // the site's reading is the same rule on its own key, and unchanged
        assertEquals(Verdict.Renamed(Now("Demo Site", "x", AT1)),
            Renames.siteVerdict(MiniRead, 200, tree("""{"ok":true,"written":true,"project":"PROJ-T1","site":"Demo Site","site_by":"x","site_at":"$AT1"}""")))
    }

    @Test fun `a row of S5's list gives the client's name to staff, and says nothing where the key is absent`() {
        val staff = tree("""{"project":"PROJ-T1","title":"Flat T1","customer":"","site":"Demo Site","site_at":"$AT1","client_name":"Client T1","client_name_by":"staff@mcft.test","client_name_at":"$AT1"}""")
        assertEquals(P1 to Held("Client T1", AT1), ClientNames.listedOf(MiniRead, staff))
        // S5 lists the project and holds no name: '' -- which the backfill reads
        assertEquals(P1 to Held("", ""), ClientNames.listedOf(MiniRead, tree("""{"project":"PROJ-T1","client_name":"","client_name_by":"","client_name_at":""}""")))
        // a vendor's or a client's login is given NONE of the three keys, and an S5 before its slice 5 has none
        assertNull(ClientNames.listedOf(MiniRead, tree("""{"project":"PROJ-T1","title":"Flat T1","site":"Demo Site","site_at":"$AT1"}""")))
        assertNull(ClientNames.listedOf(MiniRead, tree("""{"project":"","client_name":"Client T1"}""")))
        assertNull(ClientNames.listedOf(MiniRead, tree("""{"project":"PROJ-T1","client_name":7}""")))
    }

    // ---- reading: S5's name first --------------------------------------------------------------------------

    @Test fun `the tree lists a project under S5's name for its client first - a project S5 holds no name for is read as it was`() {
        assertEquals("Client T2", ClientNames.nameOf("Client T2", "Client T1"))
        assertEquals("Client T2", ClientNames.nameOf("Client T2", TreeNav.NO_CLIENT), "a reinstalled phone: the masters have only the bucket")
        assertEquals("Client T1", ClientNames.nameOf("", "Client T1"), "S5 lists it with no name")
        assertEquals("Client T1", ClientNames.nameOf(null, "Client T1"), "S5 gave no key")
        assertEquals(TreeNav.NO_CLIENT, ClientNames.nameOf("  ", TreeNav.NO_CLIENT))
        assertTrue(ClientNames.named("Client T1"))
        assertFalse(ClientNames.named("")); assertFalse(ClientNames.named(null)); assertFalse(ClientNames.named(" (No  Client) "))
    }

    // ---- the first value ------------------------------------------------------------------------------------

    @Test fun `the record carries client_name only where the phone holds a name - never blank, the bucket, the demo or the customer id`() {
        assertEquals("Client T1", ClientNames.recordName(" Client T1 ", ""))
        assertEquals("Client T1", ClientNames.recordName("Client T1", "CUST-T1"))
        assertNull(ClientNames.recordName(null, "")); assertNull(ClientNames.recordName("  ", ""))
        assertNull(ClientNames.recordName(TreeNav.NO_CLIENT, ""))
        assertNull(ClientNames.recordName(TourDemo.CLIENT, ""))
        assertNull(ClientNames.recordName("cust-t1", "CUST-T1"), "the id the tree shows for want of a name is not a name")

        fun body(client: String?, customer: String = "") = AsIsPath.body(
            project = P1, projectTitle = "Flat T1", customer = customer, site = "Demo Site", room = "Living Room", capture = "cap-1", erpCapture = null,
            capturedAt = "2026-11-24T11:40:00+05:30", appVersion = "0.3.357", l = 1.0, w = 1.0, h = 1.0, prep = null, surfaces = null,
            work = null, stage = "", clientName = client)
        val b = body("Client T1")
        assertEquals("Client T1", b["client_name"])
        assertEquals(listOf("v", "project", "project_title", "customer", "site", "client_name", "room"), b.keys.toList().take(7))
        assertEquals("", b["customer"], "the name is never sent as the customer")
        assertFalse(body(null).containsKey("client_name"))
        assertFalse(body(TreeNav.NO_CLIENT).containsKey("client_name"), "the bucket is never written over a name: the key is left out")
        assertFalse(body("CUST-T1", customer = "CUST-T1").containsKey("client_name"))
        // a caller that says nothing sends nothing: the record is as it was
        assertFalse(AsIsPath.body(P1, "Flat T1", "", null, "Living Room", "cap-1", null, "t", "v", 1.0, 1.0, 1.0, null, null, null, "").containsKey("client_name"))
    }

    @Test fun `the backfill - a project S5 lists to staff with no name, for which the phone holds one, is sent it once with base none`() {
        assertEquals(ClientNames.Seed(P1, "Client T1"), ClientNames.seed(P1, "Client T1", "Flat T1", Held("", ""), ""))
        assertNull(ClientNames.seed(P1, "Client T1", "Flat T1", Held("Client T9", AT1), ""), "S5 has a name: S5 wins, nothing is sent")
        assertNull(ClientNames.seed(P1, "Client T1", "Flat T1", null, ""), "S5 gave no key: not staff, or an S5 that keeps no name")
        assertNull(ClientNames.seed("", "Client T1", "Flat T1", Held("", ""), ""))
        assertNull(ClientNames.seed(P1, TreeNav.NO_CLIENT, "Flat T1", Held("", ""), ""), "the phone holds none either")
        assertNull(ClientNames.seed(P1, "CUST-T1", "Flat T1", Held("", ""), "CUST-T1"))
        assertNull(ClientNames.seed(P1, TourDemo.CLIENT, TourDemo.PROJECT, Held("", ""), ""))
        assertNull(ClientNames.seed(P1, "x".repeat(141), "Flat T1", Held("", ""), ""), "a name S5 would refuse is not sent at every sync")
    }

    // ---- where a client's rename goes ----------------------------------------------------------------------

    private fun route(
        name: String = "Client T1", customer: String = "", live: Boolean = false, writes: List<Write> = listOf(Write(P1, "Client T1")),
        signedIn: Boolean = true, listRead: Boolean = false,
    ) = ClientNames.route(en, name, customer, live, writes, signedIn, listRead)

    @Test fun `a client whose projects S5 lists renames through S5 - one write per project with its own base, the list read first`() {
        val two = listOf(Write(P1, "Client T1"), Write(P2, ""))
        assertEquals(RenameRoute.S5(two, readFirst = true), route(writes = two))
        assertEquals(RenameRoute.S5(two, readFirst = false), route(writes = two, listRead = true))
        assertEquals(Line.S5, Renames.line(route()!!)); assertFalse(Renames.shut(route()!!))
        // doubled and blank ids are not writes
        assertEquals(RenameRoute.S5(listOf(Write(P1, "Client T1")), readFirst = true), route(writes = listOf(Write(P1, "Client T1"), Write(P1, "x"), Write("", ""))))
        // the ERP Customer id is not the name: with no live pair such a client is renamed through S5 all the same
        assertIs<RenameRoute.S5>(route(customer = "CUST-T1"))
        // a phone-minted key is no bench docname
        assertIs<RenameRoute.S5>(route(customer = "S5-0123456789", live = true))
        assertEquals("This client's projects are on S5, so the new name is saved there for each of them and reaches every other phone. Photos already filed keep their place — only the label changes.",
            ClientNames.s5Line(en, "client"))
        assertEquals(en.app_site_on_s5_so_new, ClientNames.s5Line(en, "site"))
    }

    @Test fun `it is not S5's - and the routes a client had stand - with no project S5 lists, for the demo, and for a bench customer with a live pair`() {
        assertNull(route(writes = emptyList()))
        assertNull(route(writes = listOf(Write("", ""))))
        assertNull(route(name = TourDemo.CLIENT))
        assertNull(route(name = " "))
        assertNull(route(customer = "CUST-T1", live = true), "the bench rename it had")
        // ...and what those routes then answer for a client is unchanged (Renames.route)
        assertEquals(RenameRoute.Local, Renames.route(en, "client", "Client T1", "", true, emptyList(), true, false, false))
        assertEquals(RenameRoute.Legacy("CUST-T1"), Renames.route(en, "client", "Client T1", "CUST-T1", false, emptyList(), true, true, false))
        assertIs<RenameRoute.Blocked>(Renames.route(en, "client", "Client T1", "", true, emptyList(), true, false, false, onS5 = true))
    }

    @Test fun `signed out it says so at once - and the bucket has no name to change`() {
        assertEquals(RenameRoute.Blocked(en.refuse_site_signed_out, Line.S5), route(signedIn = false))
        val bucket = route(name = TreeNav.NO_CLIENT, writes = listOf(Write(P1, "")))
        assertEquals(RenameRoute.Blocked("This is not a client: it holds the projects S5 has no client name for, so there is no name here to change.", Line.SHUT), bucket)
        assertTrue(Renames.shut(bucket!!))
    }

    @Test fun `a PROJECT's own title is still not renamed where S5 holds it - only the client's name moved`() {
        val r = Renames.route(en, "project", "Flat T1", "", true, emptyList(), true, false, false, onS5 = true)
        assertEquals(RenameRoute.Blocked(Renames.heldByS5(en, "project"), Line.SHUT), r)
        assertEquals("This project cannot be renamed from this phone: it is on S5, or on its way there, and the new name would not reach it.", Renames.heldByS5(en, "project"))
    }

    // ---- the phone's rows follow the name S5 holds ---------------------------------------------------------

    private fun names(client: String, title: String = "Flat T1") = KeyBook.Names(client, title)

    @Test fun `when S5's name for a project is taken, every pair the phone files it under another client is a step`() {
        assertEquals(listOf(Step(P1, "Client T1", "Flat T1", "Client T2")),
            ClientNames.steps(P1, " Client T2 ", listOf(names("Client T1"), names("client  t1"), names("Client T2"))))
        // a change of case or spacing alone moves nothing
        assertEquals(emptyList(), ClientNames.steps(P1, "Client T2", listOf(names("client_t2"))))
        // the bucket and the customer id the tree showed are left behind like any other name
        assertEquals(listOf(Step(P1, TreeNav.NO_CLIENT, "Flat T1", "Client T2"), Step(P1, "CUST-T1", "Flat T1", "Client T2")),
            ClientNames.steps(P1, "Client T2", listOf(names(TreeNav.NO_CLIENT), names("CUST-T1"))))
        // S5 holds no name, or the bucket's: nothing moves
        assertEquals(emptyList(), ClientNames.steps(P1, "", listOf(names("Client T1"))))
        assertEquals(emptyList(), ClientNames.steps(P1, TreeNav.NO_CLIENT, listOf(names("Client T1"))))
        assertEquals(emptyList(), ClientNames.steps("", "Client T2", listOf(names("Client T1"))))
        assertEquals(emptyList(), ClientNames.steps(P1, "Client T2", listOf(names("", "Flat T1"), names("Client T1", ""))))
    }

    @Test fun `a step moves the project's captures - sent or not - and the ones not keyed yet, and nothing else`() {
        val move = ClientNames.follows(listOf(Step(P1, "Client T1", "Flat T1", "Client T2")))
        fun cap(project: String, client: String, title: String = "Flat T1", sent: Boolean = false) = TreeKeep.Cap(project, client, title, sent)
        assertEquals(cap(P1, "Client T2", sent = true), move(cap(P1, "Client T1", sent = true)), "S5 holds the capture: its client is S5's name")
        assertEquals(cap(P1, "Client T2"), move(cap(P1, "Client T1")))
        assertEquals(cap(P1, "Client T2"), move(cap(P1, "Somebody else")), "by the project's id, whatever it was shot under")
        assertEquals(cap("", "Client T2"), move(cap("", "client t1")), "not keyed yet: the same client and title is the same project")
        // another project of the old client stays the old client's: only this project took the name
        assertEquals(cap(P2, "Client T1"), move(cap(P2, "Client T1")))
        assertEquals(cap("", "Client T1", "Flat T9"), move(cap("", "Client T1", "Flat T9")))
        assertEquals(cap(P1, "Client T2"), move(cap(P1, "Client T2")))
        val demo = cap("", TourDemo.CLIENT, TourDemo.PROJECT)
        assertEquals(demo, ClientNames.follows(listOf(Step(P1, TourDemo.CLIENT, TourDemo.PROJECT, "Client T2")))(demo))
        assertEquals(cap(P1, "Client T1"), ClientNames.follows(emptyList())(cap(P1, "Client T1")))
    }

    @Test fun `once every project took the name, every capture still under the old client follows - sent, keyed or neither`() {
        val rekey = ClientNames.rekeyDone("Client T1", " Client T2 ")
        assertEquals(TreeKeep.Cap("S5-0123456789", "Client T2", "Flat T9", true), rekey(TreeKeep.Cap("S5-0123456789", "client_t1", "Flat T9", true)))
        assertEquals(TreeKeep.Cap("", "Client T2", "Flat T9", false), rekey(TreeKeep.Cap("", "Client T1", "Flat T9", false)))
        val other = TreeKeep.Cap("", "Client T5", "Flat T9", false)
        assertEquals(other, rekey(other))
        val demo = TreeKeep.Cap("", TourDemo.CLIENT, TourDemo.PROJECT, false)
        assertEquals(demo, ClientNames.rekeyDone(TourDemo.CLIENT, "Client T2")(demo))
        assertEquals(other, ClientNames.rekeyDone("Client T5", "  ")(other), "no name, no change")
    }

    // ---- what one Save came to -----------------------------------------------------------------------------

    private val w1 = Write(P1, "Client T1")
    private val w2 = Write(P2, "Client T1")
    private val renamed = Verdict.Renamed(Now("Client T2", "staff@mcft.test", AT2))
    private val same = Verdict.Same(Now("Client T2", "staff@mcft.test", AT2))
    private val titleOf: (String) -> String = { id -> if (id == P1) "Flat T1" else if (id == P2) "Flat T2" else "" }

    @Test fun `Renamed only when every project took the name - by S5's answers alone`() {
        assertEquals(Outcome.Done("Client T2"), ClientNames.outcome(listOf(w1, w2), listOf(renamed, same)))
        assertNull(ClientNames.text(en, Outcome.Done("Client T2"), titleOf))
        assertEquals(Outcome.Part(1, 2, listOf(Left(P2, "Client T1", verdict(409, STALE)))), ClientNames.outcome(listOf(w1, w2), listOf(renamed, verdict(409, STALE))))
        assertEquals(Outcome.Part(1, 2, listOf(Left(P2, "Client T1", null))), ClientNames.outcome(listOf(w1, w2), listOf(renamed)), "an answer short is not a success")
        assertEquals(Outcome.Part(0, 2, listOf(Left(P1, "Client T1", Verdict.Offline), Left(P2, "Client T1", null))), ClientNames.outcome(listOf(w1, w2), listOf(Verdict.Offline)))
        assertEquals(Outcome.Part(0, 0, emptyList()), ClientNames.outcome(emptyList(), emptyList()), "nothing sent is nothing renamed")
    }

    @Test fun `nothing renamed and the answer is the caller's - one sentence - offline, signed out, not staff`() {
        fun said(v: Verdict) = ClientNames.text(en, ClientNames.outcome(listOf(w1, w2), listOf(v)), titleOf)
        assertEquals("Offline — the name changes only when S5 answers; not saved", said(Verdict.Offline))
        assertEquals(en.refuse_site_signed_out, said(Verdict.Refused(401, "")))
        assertEquals("Only MCFT staff can rename a client", said(verdict(403, NOT_STAFF)))
        assertEquals(en.refuse_site_offline, ClientNames.text(en, Outcome.Part(0, 0, emptyList()), titleOf))
        // one project, refused: its own sentence, as a site's rename says it
        assertEquals("Renamed by lead@mcft.test meanwhile — now Client T3; not saved", ClientNames.text(en, ClientNames.outcome(listOf(w1), listOf(verdict(409, STALE))), titleOf))
        assertEquals("This project has never been uploaded", ClientNames.text(en, ClientNames.outcome(listOf(w1), listOf(verdict(404, NEVER))), titleOf))
    }

    @Test fun `some took it - it says how many, and for each project left, which and why`() {
        val w3 = Write("PROJ-T3", "Client T1"); val w4 = Write("PROJ-T4", "Client T1")
        val o = ClientNames.outcome(listOf(w1, w2, w3, w4), listOf(renamed, verdict(409, STALE), verdict(404, NEVER), Verdict.Offline))
        assertEquals(
            "Renamed on 1 of 4 projects\n" +
                "Flat T2: renamed by lead@mcft.test meanwhile — now Client T3; not saved\n" +
                "PROJ-T3: S5 does not list this project; not saved\n" +
                "PROJ-T4: Offline — the name changes only when S5 answers; not saved",
            ClientNames.text(en, o, titleOf))
        // the Save stopped: the projects it did not reach are named as not sent
        assertEquals("Renamed on 1 of 3 projects\nFlat T2: Only MCFT staff can rename a client\nPROJ-T3: not sent",
            ClientNames.text(en, ClientNames.outcome(listOf(w1, w2, w3), listOf(same, verdict(403, NOT_STAFF))), titleOf))
        // none took it, and the first answer is one project's own: each is still named
        assertEquals("Renamed on 0 of 2 projects\nFlat T1: renamed by lead@mcft.test meanwhile — now Client T3; not saved\nFlat T2: Changed meanwhile; not saved — send it again",
            ClientNames.text(en, ClientNames.outcome(listOf(w1, w2), listOf(verdict(409, STALE), verdict(409, MEANWHILE))), titleOf))
        // a 409 with no name stored is S5's own words
        assertTrue(ClientNames.text(en, ClientNames.outcome(listOf(w1, w2), listOf(renamed, verdict(409, NONE))), titleOf)!!.endsWith("Flat T2: This project has no client name on S5 yet: send base \"\" to give it one; not saved"))
    }

    // ---- :app, read as source (only CI compiles :app) ------------------------------------------------------

    private fun app(name: String): String {
        val f = File("../app/src/main/kotlin/com/malletcrafts/interiorestimate/$name")
        assertTrue(f.isFile, "the app's $name is beside :pano (${f.absolutePath})")
        return f.readText()
    }

    private fun between(src: String, from: String, to: String): String {
        val a = src.indexOf(from); assertTrue(a >= 0, "`$from` is not in the source")
        val b = src.indexOf(to, a + from.length); assertTrue(b > a, "`$to` does not follow `$from`")
        return src.substring(a, b)
    }

    private fun count(src: String, what: String) = Regex(Regex.escape(what)).findAll(src).count()

    private val feed = "S5Clients.takeList(context, (0 until arr.length()).mapNotNull { i -> ClientNames.listedOf(JsonRead, arr.opt(i)) })"

    @Test fun `every read of S5's list feeds S5's client names, beside its site names`() {
        val fetch = between(app("S5Session.kt"), "fun fetch(context: Context, erpLive: Boolean)", "val store = CaptureStore(context)")
        assertTrue(fetch.contains("S5Sites.takeRead(") && fetch.contains(feed), "before the captures are read, so the entries built there carry the name")
        assertTrue(between(app("QuoteStageSync.kt"), "fun refresh(context: Context", "// ---- Record the approved scope").contains(feed))
        assertTrue(between(app("SiteRename.kt"), "fun hear(context: Context): Boolean", "fun seed(context: Context)").contains(feed))
    }

    @Test fun `the tree and the key lookup read S5's client name first`() {
        val cat = app("Catalogue.kt")
        val erp = between(cat, "private fun erpProjects(masters: JSONObject?): List<Project> {", "fun projects(masters: JSONObject?): List<Project> {")
        assertTrue(erp.contains("val s5Clients = S5Clients.all(appContext)"))
        assertTrue(erp.contains("val client = ClientNames.nameOf(s5Clients[o.optString(\"project\")]?.site,"))
        assertTrue(erp.contains("clientId = o.optString(\"customer\"),"), "the customer id stays the id")
        val key = between(app("S5Session.kt"), "fun keyFor(context: Context, client: String, title: String): String {", "KeyBook.keyFor(")
        assertTrue(key.contains("val c = ClientNames.nameOf(S5Clients.held(context, id)?.site,"), "a capture shot under S5's name finds its project, never a new key")
        // a bootstrap site row is listed under its project's client, so it is not left behind as an empty site of the old name
        assertTrue(between(cat, "private fun serverSites(masters: JSONObject?, projects: List<Project>): ServerSites {", "private fun siteKey(").contains("val owner = ClientNames.nameOf(linked[o.optString(\"name\")],"))
    }

    @Test fun `S5's client names have one home, and the phone's rows follow the name where it is written`() {
        val home = app("S5Clients.kt")
        val take = between(home, "fun takeList(context: Context, rows: List<Pair<String, Renames.Held>>): Int = synchronized(LOCK) {", "\n}\n")
        assertTrue(take.contains("if (!Renames.takes(stored, copy)) continue"), "by S5's own stamp, never by arrival")
        val before = take.indexOf("filed(context, named.keys)"); val write = take.indexOf("ed.apply()"); val after = take.indexOf("follow(context, named, before)")
        assertTrue(before in 0 until write && write < after, "what the phone files the project under is read BEFORE the name is written, and the rows follow after")
        val follow = between(home, "private fun follow(", "fun takeList(")
        assertTrue(follow.contains("ClientNames.steps(project, to, before[project]?.names.orEmpty())") && follow.contains("ClientNames.follows(steps)"), "which rows move is :pano's answer")
        for (step in listOf("store.setNames(c.deviceId, now.client, now.title)", "cat.followClient(s.from, s.title, s.to,", "S5Keys.remember(context, s.project, s.to, s.title)"))
            assertTrue(follow.contains(step), "$step: the captures, the phone's own row and the remembered key")
        // the name is S5's alone to write: no other file writes this store
        for (f in listOf("MainActivity.kt", "Catalogue.kt", "SyncWorker.kt", "AsIsUpload.kt"))
            assertFalse(app(f).contains("S5Clients.take"), "$f does not write S5's client names")
        assertTrue(app("MainActivity.kt").contains("S5Clients.forget(context, k)"), "forgotten only with the delete that proved S5 no longer lists the project")
    }

    @Test fun `the upload's record carries the client's name, and the sync backfills it once per project`() {
        val up = between(app("AsIsUpload.kt"), "val body = AsIsPath.body(", "val recordUrl")
        assertTrue(up.contains("clientName = c.customerName,"))
        val leg = between(app("SyncWorker.kt"), "private fun s5Leg(store: CaptureStore)", "private data class ErpOutcome")
        val fetch = leg.indexOf("S5Masters.fetch(ctx, erpLive = false)"); val site = leg.indexOf("SiteRename.seed(ctx)"); val client = leg.indexOf("ClientRename.seed(ctx)")
        assertTrue(fetch in 0 until site && site < client, "after the list was read, beside the site's seed")
        val seed = app("ClientRename.kt").substringAfter("fun seed(context: Context) {")
        assertTrue(seed.contains("ClientNames.seed(p.serverId, p.client, p.title, S5Clients.held(context, p.serverId), p.clientId) ?: continue"))
        assertTrue(seed.contains("val v = put(context, auth, s.project, s.name, \"\")"), "base '': the first write wins")
        assertTrue(seed.contains("Renames.nowIn(v)?.let { S5Clients.take(context, s.project, Renames.heldOf(it)) }"), "a 409's name is taken: S5 wins")
        assertTrue(seed.contains("if (Renames.stops(v)) break"))
        assertEquals(1, count(seed, "put(context, auth,"), "one write per project, in one pass: never a loop")
    }

    @Test fun `a client's Save sends one PUT per project with that project's own base, and finishes on the phone only when every project took it`() {
        val rename = app("ClientRename.kt")
        val put = between(rename, "private fun put(", "fun rename(context: Context")
        assertTrue(put.contains("ClientNames.request(project, name, base)") && put.contains("ClientNames.verdict(JsonRead, "), "the request and the verdict are :pano's")
        val run = between(rename, "fun rename(context: Context", "fun finish(context: Context")
        assertTrue(run.contains("RenameRoute.Write(w.project, S5Clients.held(context, w.project)?.site ?: w.base)"), "each project's own base: the name S5 last gave for it")
        assertTrue(run.contains("Renames.nowIn(v)?.let { S5Clients.take(context, w.project, Renames.heldOf(it)) }"), "every name an answer carries is kept, a 409's as stored included")
        assertTrue(run.contains("if (Renames.stops(v)) break") && run.contains("return ClientNames.outcome(sent, verdicts)"))
        assertFalse(rename.contains("\"customer\""), "the label is never the ERP Customer id")

        val main = app("MainActivity.kt")
        val route = between(main, "fun renameRoute(t: RenameTarget, listRead: Boolean = false): RenameRoute {", "val isSite = t.kind == \"site\"")
        assertTrue(route.contains("clientRoute(t, listRead)?.let { return it }"), "asked first")
        val client = between(main, "fun clientRoute(t: RenameTarget, listRead: Boolean): RenameRoute? {", "fun renameRoute(")
        assertTrue(client.contains("return ClientNames.route(") && client.contains("S5Clients.held(context, id)?.let { RenameRoute.Write(id, it.site) }"))
        val save = between(main, "onSave = { newName ->", "blocker = deleteBlocker")
        assertTrue(save.contains("if (t.kind == \"client\" && renameRoute(t) is RenameRoute.S5) renameClient(t, newName)"))
        val go = between(main, "fun renameClient(t: RenameTarget, newName: String) {", "renaming?.let { t ->")
        val check = go.indexOf("ClientNames.typed(newName)"); val read = go.indexOf("S5Masters.fetch("); val send = go.indexOf("ClientRename.rename(context, s5.writes, name)")
        assertTrue(check in 0 until read && read < send, "the name is checked before anything is sent, and S5's list is read first")
        assertTrue(go.contains("renameFailed { tx -> tx.refuse_client_name_bad }") && go.contains("renameFailed { tx -> tx.refuse_site_offline }"))
        val done = between(go, "if (outcome is ClientNames.Outcome.Done) {", "} else {")
        assertTrue(done.contains("ClientRename.finish(context, t.client, stored, sites)") && done.contains("lastResult = { tx -> tx.app_msg_renamed_x(stored) }") && done.contains("closeRename()"))
        val part = go.substringAfter("} else {")
        assertTrue(part.contains("renameFailed { tx -> ClientNames.text(tx, outcome, titleOf).orEmpty() }"), "said in the dialog, which stays open")
        assertFalse(part.contains("ClientRename.finish(") || part.contains("closeRename()") || part.contains("lastResult ="), "not every project took it: nothing is reported as renamed")
        assertTrue(app("TreeShell.kt").contains("RenameRoute.Line.S5 -> ClientNames.s5Line(tx, what)"), "the grey line is the client's own")
    }
}
