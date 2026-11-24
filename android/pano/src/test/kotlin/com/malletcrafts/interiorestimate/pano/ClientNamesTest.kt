package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.ClientNames.Left
import com.malletcrafts.interiorestimate.pano.ClientNames.Next
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
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * THE CLIENT'S NAME IS S5'S, AND A CLIENT RENAME REALLY WORKS (port 63 slice 5; android/docs/port63/DESIGN-tree.md
 * §11): the name S5 takes, the request and every answer read into its verdict, the list row, S5's name read first,
 * the record's first value and the backfill, where a client's rename goes, how the phone's rows follow the name S5
 * holds, what a Save over several projects comes to and the lines it draws -- and, read as source because only CI
 * compiles :app, that the phone feeds, sends and follows by these rules and no other.
 *
 * THE REVIEW OF SLICE 5 (24 Nov 2026; DESIGN-tree.md §11.6) is tested here by CASES, not by the text of :app: S5 and
 * the bench are both written where both hold the client, a rename onto another client's project of the same title is
 * refused, the names are one staff login's and nobody else's, every Save works its projects out again, and after the
 * list read each route says its own sentence. The source pins below only hold :app to calling those rules.
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
        name: String = "Client T1", writes: List<Write> = listOf(Write(P1, "Client T1")),
        signedIn: Boolean = true, listRead: Boolean = false,
    ) = ClientNames.route(en, name, writes, signedIn, listRead)

    @Test fun `a client whose projects S5 lists renames through S5 - one write per project with its own base, the list read first`() {
        val two = listOf(Write(P1, "Client T1"), Write(P2, ""))
        assertEquals(RenameRoute.S5(two, readFirst = true), route(writes = two))
        assertEquals(RenameRoute.S5(two, readFirst = false), route(writes = two, listRead = true))
        assertEquals(Line.S5, Renames.line(route()!!)); assertFalse(Renames.shut(route()!!))
        // doubled and blank ids are not writes
        assertEquals(RenameRoute.S5(listOf(Write(P1, "Client T1")), readFirst = true), route(writes = listOf(Write(P1, "Client T1"), Write(P1, "x"), Write("", ""))))
        assertEquals("This client's projects are on S5, so the new name is saved there for each of them and reaches every other phone. Photos already filed keep their place — only the label changes.",
            ClientNames.s5Line(en, "client"))
        assertEquals(en.app_site_on_s5_so_new, ClientNames.s5Line(en, "site"))
    }

    @Test fun `it is not S5's - and the routes a client had stand - with no project S5 lists, and for the demo`() {
        assertNull(route(writes = emptyList()))
        assertNull(route(writes = listOf(Write("", ""))))
        assertNull(route(name = TourDemo.CLIENT))
        assertNull(route(name = " "))
        // ...and what those routes then answer for a client is unchanged (Renames.route)
        assertEquals(RenameRoute.Local, Renames.route(en, "client", "Client T1", "", true, emptyList(), true, false, false))
        assertEquals(RenameRoute.Legacy("CUST-T1"), Renames.route(en, "client", "Client T1", "CUST-T1", false, emptyList(), true, true, false))
        assertIs<RenameRoute.Blocked>(Renames.route(en, "client", "Client T1", "", true, emptyList(), true, false, false, onS5 = true))
    }

    // ---- the review of slice 5, fault 1: S5 and the bench are written together ------------------------------

    @Test fun `a bench customer with a live pair is no way round S5 - where S5 holds a name key the rename goes to S5, and the bench is told by the same Save`() {
        // THE CASE THAT WENT WRONG: PROJ-T1 is on S5 under "Client T1", the client is the bench's CUST-T1 and the pair
        // is live. The route used to answer null here -- the bench alone was renamed and the screen said "Renamed",
        // while the tree went on reading S5's "Client T1". The route has no bench question left to ask:
        val s5 = assertIs<RenameRoute.S5>(route(writes = listOf(Write(P1, "Client T1"))))
        assertEquals(Next.Send(listOf(Write(P1, "Client T1"))), ClientNames.next(en, s5.copy(readFirst = false), "Client T1"))
        // ...and the same Save tells the bench: a live pair, and a docname that is the bench's own
        assertEquals("CUST-T1", ClientNames.bench(" CUST-T1 ", erpLive = true))
        assertNull(ClientNames.bench("CUST-T1", erpLive = false), "no live pair: the call cannot be made, S5 alone holds the name")
        assertNull(ClientNames.bench("", erpLive = true), "no Customer: S5 alone")
        assertNull(ClientNames.bench("S5-0123456789", erpLive = true), "a phone-minted key is no bench docname")
        // S5 holds a name for one project and none for the other: S5 is written for both
        assertEquals(RenameRoute.S5(listOf(Write(P1, "Client T1"), Write(P2, "")), readFirst = true), route(writes = listOf(Write(P1, "Client T1"), Write(P2, ""))))
        // S5 gives no key for any project of the client: only then is it the bench's alone, as it was
        assertNull(route(writes = emptyList()))
        assertEquals(RenameRoute.Legacy("CUST-T1"), Renames.route(en, "client", "Client T1", "CUST-T1", false, emptyList(), true, true, false))
    }

    @Test fun `the bench took the name and S5 left a project behind - it is said, with the bench's state first, and never as Renamed`() {
        // both took it: renamed, nothing to say
        assertNull(ClientNames.text(en, Outcome.Done("Client T2"), titleOf, office = "Client T2"))
        // the bench took it, S5 took it for one of two
        val part = ClientNames.outcome(listOf(w1, w2), listOf(renamed, verdict(409, STALE)))
        assertEquals(
            "The office's record is now Client T2, but S5 has not taken the name for every project — Save again. S5's answer:\n" +
                "Renamed on 1 of 2 projects\n" +
                "Flat T2: renamed by lead@mcft.test meanwhile — now Client T3; not saved",
            ClientNames.text(en, part, titleOf, office = "Client T2"))
        // the bench took it and S5 did not answer at all: "not saved" alone would be untrue of the bench
        assertEquals(
            "The office's record is now Client T2, but S5 has not taken the name for every project — Save again. S5's answer:\n" + en.refuse_site_offline,
            ClientNames.text(en, ClientNames.outcome(listOf(w1, w2), listOf(Verdict.Offline)), titleOf, office = "Client T2"))
        // the bench was not told (S5 alone holds the client): S5's lines as they were
        assertEquals(ClientNames.text(en, part, titleOf), ClientNames.text(en, part, titleOf, office = null))
        // a bench refusal comes BEFORE anything is sent to S5, and is the bench's own sentence
        assertEquals("Could not rename: Customer already has a customer name called \"Client T2\".",
            Renames.benchRefused(en, "Customer already has a customer name called \"Client T2\"."))
    }

    // ---- fault 5: every Save works its projects out again ----------------------------------------------------

    @Test fun `every Save works the client's projects out again from the list just read - the second Save heals a partial`() {
        // S5's names as the phone holds them (S5Clients), and the tree's grouping by them
        val held = HashMap<String, Held>()
        held[P1] = Held("Client T1", AT1); held[P2] = Held("Client T1", AT1)
        fun under(client: String) = held.filter { Renames.key(it.value.site) == Renames.key(client) }.keys.sorted()
        // SAVE 1: both projects, each with its own base; the second gets no answer
        val first = ClientNames.writes(under("Client T1")) { held[it] }
        assertEquals(listOf(Write(P1, "Client T1"), Write(P2, "Client T1")), first)
        assertEquals(Outcome.Part(1, 2, listOf(Left(P2, "Client T1", Verdict.Offline))), ClientNames.outcome(first, listOf(renamed, Verdict.Offline)))
        held[P1] = Renames.heldOf(Renames.nowIn(renamed)!!) // each answer is kept at once, and the tree regroups
        // ...and before the second Save another phone gives the old client a project
        held["PROJ-T3"] = Held("Client T1", AT2)
        // SAVE 2, from the list read for it: the project that took the name is not sent again, the new one is --
        // the first Save's list (PROJ-T1, PROJ-T2) would have missed it and reported the client renamed
        val second = ClientNames.writes(under("Client T1")) { held[it] }
        assertEquals(listOf(Write(P2, "Client T1"), Write("PROJ-T3", "Client T1")), second)
        assertEquals(Outcome.Done("Client T2"), ClientNames.outcome(second, listOf(renamed, renamed)))
        // a project S5 gives this login no key for is not a write; a blank or doubled id is not one either
        assertEquals(listOf(Write(P1, "")), ClientNames.writes(listOf(P1, " PROJ-T1 ", "", "PROJ-T9")) { if (it == P1) Held("", "") else null })
        assertEquals(emptyList<Write>(), ClientNames.writes(emptyList()) { Held("Client T1", AT1) })
    }

    // ---- fault 2: never onto another client's project of the same title --------------------------------------

    private val listing = listOf(
        "Client T1" to "Flat T1", "Client T1" to "Flat T2", "Client T2" to "flat  t2", "Client T3" to "Flat T9", TreeNav.NO_CLIENT to "Flat T5")

    @Test fun `a client is not renamed onto another client that already has a project of the same title - refused before anything is sent`() {
        assertEquals("Not renamed: Client T2 already has a project called flat  t2.", ClientNames.refusal(en, "Client T1", "Client T2", listing))
        assertEquals(ClientNames.refusal(en, "Client T1", "Client T2", listing), ClientNames.refusal(en, " client_t1", " CLIENT  t2 ", listing), "names as the catalogue compares them")
        // the other way round is the same clash
        assertEquals("Not renamed: Client T1 already has a project called Flat T2.", ClientNames.refusal(en, "Client T2", "Client T1", listing))
        // A MERGE with no title in common is allowed: every project keeps its own key and title
        assertNull(ClientNames.refusal(en, "Client T1", "Client T3", listing))
        assertNull(ClientNames.refusal(en, "Client T3", "Client T2", listing))
        // a name nobody has, and the client's own name in another case, are no clash
        assertNull(ClientNames.refusal(en, "Client T1", "Client T8", listing))
        assertNull(ClientNames.refusal(en, "Client T1", "client  t1", listing))
        assertNull(ClientNames.refusal(en, "Client T1", "  ", listing), "a blank name is the name check's to refuse")
    }

    @Test fun `a client is not given the bucket's name or the demo's`() {
        val said = "Not renamed: that name cannot be a client's — type another."
        assertEquals(said, ClientNames.refusal(en, "Client T1", TreeNav.NO_CLIENT, listing))
        assertEquals(said, ClientNames.refusal(en, "Client T1", " (No  Client) ", listing), "the bucket cannot be renamed, so the client could never be given another name")
        assertEquals(said, ClientNames.refusal(en, "Client T1", TourDemo.CLIENT, emptyList()))
    }

    @Test fun `the Save that finishes this dialog's own partial rename is not refused for the projects that already moved`() {
        // one client, two sites, a project called Wardrobe at each; Save 1 moved the first to "Client T2" and stopped
        val after = listOf("Client T2" to "Wardrobe", "Client T1" to "Wardrobe")
        assertEquals("Not renamed: Client T2 already has a project called Wardrobe.", ClientNames.refusal(en, "Client T1", "Client T2", after))
        assertNull(ClientNames.refusal(en, "Client T1", "Client T2", after, begun = "Client T2"), "the project under the new name is this client's own")
        assertNull(ClientNames.refusal(en, "Client T1", " client t2", after, begun = "Client T2"))
        // another name than the one begun is judged as any rename is
        assertEquals("Not renamed: Client T2 already has a project called Wardrobe.", ClientNames.refusal(en, "Client T1", "Client T2", after, begun = "Client T9"))
        assertEquals(en.refuse_client_name_reserved, ClientNames.refusal(en, "Client T1", TreeNav.NO_CLIENT, after, begun = TreeNav.NO_CLIENT))
    }

    // ---- fault 3: the names are staff's, and one login's ------------------------------------------------------

    @Test fun `S5's client names are kept for the login that read them - a signed-out phone, the next person and a login that is not staff are shown none and send no backfill`() {
        val staff = ClientNames.owner("Staff@MCFT.test ", null)
        val other = ClientNames.owner("lead@mcft.test", null)
        val keyOnly = ClientNames.owner(null, "KEY-T1")
        assertEquals(24, staff.length); assertTrue(staff.all { it in "0123456789abcdef" }, "a digest: the mark names nobody")
        assertEquals(staff, ClientNames.owner("staff@mcft.test", "KEY-T1"), "the S5 session wins over the legacy key, as S5Auth picks")
        assertEquals(keyOnly, ClientNames.owner("  ", " KEY-T1 "))
        assertEquals(3, setOf(staff, other, keyOnly).size)
        assertNotEquals(ClientNames.owner("KEY-T1", null), keyOnly, "a login and a key of the same text are not one owner")
        assertEquals("", ClientNames.owner(null, null)); assertEquals("", ClientNames.owner(" ", ""))

        val held = Held("Client T1", AT1)
        // SHOWN only to the login that read them
        assertEquals(held, ClientNames.shown(staff, staff, held))
        assertNull(ClientNames.shown(staff, "", held), "signed out")
        assertNull(ClientNames.shown(staff, other, held), "the next person")
        assertNull(ClientNames.shown(staff, keyOnly, held), "the legacy key's owner, once the session is gone")
        assertNull(ClientNames.shown(null, staff, held), "names with no mark are nobody's")
        assertNull(ClientNames.shown("", "", held)); assertNull(ClientNames.shown(staff, staff, null))

        // CLEARED: at the first answer taken for another login, and by a list read that carries the key on no project
        assertFalse(ClientNames.forgets(staff, staff, listed = 12, keyed = 12))
        assertFalse(ClientNames.forgets(staff, staff, listed = 12, keyed = 5), "some projects keyed: staff")
        assertTrue(ClientNames.forgets(staff, staff, listed = 12, keyed = 0), "S5 named twelve projects and gave no client name: not staff any more")
        assertFalse(ClientNames.forgets(staff, staff, listed = 0, keyed = 0), "an empty list says nothing")
        assertTrue(ClientNames.forgets(staff, other, listed = 12, keyed = 12)); assertTrue(ClientNames.forgets(staff, "", listed = 1, keyed = 1))
        assertTrue(ClientNames.forgets(null, staff, listed = 1, keyed = 1))
        // a vendor's or a client's list: S5 gives none of the three keys, so no row is read from it at all
        val vendorList = listOf(tree("""{"project":"PROJ-T1","title":"Flat T1","site":"Demo Site","site_at":"$AT1"}"""), tree("""{"project":"PROJ-T2","title":"Flat T2"}"""))
        val rows = vendorList.mapNotNull { ClientNames.listedOf(MiniRead, it) }
        assertEquals(0, rows.size); assertTrue(ClientNames.forgets(keyOnly, keyOnly, listed = vendorList.size, keyed = rows.size))

        // SENT: the backfill goes only for a project S5 gave THIS login a key for -- so never for anybody else
        assertEquals(ClientNames.Seed(P1, "Client T1"), ClientNames.seed(P1, "Client T1", "Flat T1", ClientNames.shown(staff, staff, Held("", "")), ""))
        for (now in listOf("", other, keyOnly))
            assertNull(ClientNames.seed(P1, "Client T1", "Flat T1", ClientNames.shown(staff, now, Held("", "")), ""), "nothing is sent for another login")
        // ...and a rename has nothing to write for it either
        assertNull(route(writes = ClientNames.writes(listOf(P1, P2)) { ClientNames.shown(staff, keyOnly, Held("Client T1", AT1)) }))
    }

    // ---- fault 6: after the list read, each route says its own sentence ---------------------------------------

    @Test fun `after the list read every route has its own answer - and offline is said only when the read failed`() {
        val two = listOf(Write(P1, "Client T1"), Write(P2, ""))
        assertEquals(Next.Send(two), ClientNames.next(en, RenameRoute.S5(two, readFirst = false), "Client T1"))
        assertEquals(Next.Here, ClientNames.next(en, RenameRoute.Local, "Client T1"))
        // THE CASE THAT WENT WRONG: S5 lists no project of the client any more and the bench holds it -- "Offline"
        // was said, with the list read a second before. The real route, asked as the Save asks it:
        val legacy = Renames.route(en, "client", "Client T1", "CUST-T1", false, emptyList(), true, true, false, listRead = true)
        assertEquals(Next.Bench("CUST-T1"), ClientNames.next(en, legacy, "Client T1"))
        // shut routes say their own reason: the office's record with no live pair, a project on its way to S5, signed out
        val office = Renames.route(en, "client", "Client T1", "CUST-T1", false, emptyList(), true, false, false, listRead = true)
        assertEquals(Next.Say("This client is the office's record. It is renamed at the office, not from this phone."), ClientNames.next(en, office, "Client T1"))
        val onWay = Renames.route(en, "client", "Client T1", "", true, emptyList(), true, false, false, listRead = true, onS5 = true)
        assertEquals(Next.Say(Renames.heldByS5(en, "client")), ClientNames.next(en, onWay, "Client T1"))
        assertEquals(Next.Say(en.refuse_site_signed_out), ClientNames.next(en, route(signedIn = false, listRead = true)!!, "Client T1"))
        // nothing of the client left anywhere, or an S5 route with nothing to write: no rename, and it is said
        val gone = Renames.route(en, "client", "Client T1", "", false, emptyList(), true, false, false, listRead = true)
        assertEquals(Next.Say("Could not rename: Client T1"), ClientNames.next(en, gone, "Client T1"))
        assertEquals(Next.Say("Could not rename: Client T1"), ClientNames.next(en, RenameRoute.S5(emptyList(), readFirst = false), "Client T1"))
        // not one of them is the offline line
        for (r in listOf(RenameRoute.S5(two, false), RenameRoute.Local, legacy, office, onWay, gone, RenameRoute.S5(emptyList(), false)))
            assertNotEquals<Next>(Next.Say(en.refuse_site_offline), ClientNames.next(en, r, "Client T1"))
        // the read itself failed: no signal -- or nothing to call S5 with, which is said as that
        assertEquals("Offline — the name changes only when S5 answers; not saved", ClientNames.notRead(en, signedIn = true))
        assertEquals(en.refuse_site_signed_out, ClientNames.notRead(en, signedIn = false))
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

    // every row of the read that carries the key, and how many projects the read named in all (the review, fault 3)
    private val feed = "S5Clients.takeRead(context, (0 until arr.length()).mapNotNull { i -> ClientNames.listedOf(JsonRead, arr.opt(i)) }, listed = arr.length())"

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
        assertTrue(client.contains("return ClientNames.route("))
        assertTrue(client.contains("writes = ClientNames.writes(cat.s5Ties(masters, queue, t.kind, t.client, \"\", \"\").keys) { id -> S5Clients.held(context, id) },"),
            "the client's projects are worked out at EVERY asking, from the tree as it is then")
        assertFalse(client.contains("renameIds") || client.contains("erpLive"), "no list kept from an earlier Save, and no bench question: a live pair is no way round S5")
        val save = between(main, "onSave = { newName ->", "blocker = deleteBlocker")
        assertTrue(save.contains("if (t.kind == \"client\" && renameRoute(t) is RenameRoute.S5) renameClient(t, newName)"))
        val go = between(main, "fun renameClient(t: RenameTarget, newName: String) {", "renaming?.let { t ->")
        val check = go.indexOf("ClientNames.typed(newName)"); val read = go.indexOf("S5Masters.fetch("); val send = go.indexOf("ClientRename.rename(context, writes, name)")
        assertTrue(check in 0 until read && read < send, "the name is checked before anything is sent, and S5's list is read first")
        assertTrue(go.contains("renameFailed { tx -> tx.refuse_client_name_bad }") && go.contains("renameFailed { tx -> ClientNames.notRead(tx, signedIn) }"))
        assertFalse(go.contains("tx.refuse_site_offline"), "offline is said by the rule, for a read that failed, and by no other branch")
        assertEquals(1, count(go, "} else {"), "one else in the Save: the Done branch's")
        val done = between(go, "if (outcome is ClientNames.Outcome.Done) {", "} else {")
        assertTrue(done.contains("ClientRename.finish(context, t.client, stored, sites)") && done.contains("lastResult = { tx -> tx.app_msg_renamed_x(stored) }") && done.contains("closeRename()"))
        val part = go.substringAfter("} else {")
        assertTrue(part.contains("renameFailed { tx -> ClientNames.text(tx, outcome, titleOf, office).orEmpty() }"), "said in the dialog, which stays open -- with the bench's state first where the bench was told")
        assertFalse(part.contains("ClientRename.finish(") || part.contains("closeRename()") || part.contains("lastResult ="), "not every project took it: nothing is reported as renamed")
        assertTrue(app("TreeShell.kt").contains("RenameRoute.Line.S5 -> ClientNames.s5Line(tx, what)"), "the grey line is the client's own")
    }

    // ---- the review of slice 5: :app is held to the rules above -----------------------------------------------

    @Test fun `review - the Save follows the rules in their order - the clash, the route's own sentence, the bench first, then S5`() {
        val main = app("MainActivity.kt")
        val go = between(main, "fun renameClient(t: RenameTarget, newName: String) {", "renaming?.let { t ->")
        val read = go.indexOf("S5Masters.fetch(")
        val clash = go.indexOf("val clash = clientClash(t, name, listed.map { it.client to it.title })")
        val next = go.indexOf("val next = ClientNames.next(tx, renameRoute(t, listRead = true), t.client)")
        val asked = go.indexOf("val docname = ClientNames.bench(t.serverId, S5Masters.erpLive(context))")
        val bench = go.indexOf("ClientRename.bench(context, docname, name, noServer)", asked)
        val send = go.indexOf("ClientRename.rename(context, writes, name)")
        assertTrue(read in 0 until clash, "the clash is judged on the list just read")
        assertTrue(clash < next && next < asked && asked < bench && bench < send, "the bench is told before S5, and both after the refusals")
        assertTrue(go.contains("val listed = cat.projects(now)") && go.contains("val writes = (next as ClientNames.Next.Send).writes"), "the projects written are the route's, asked after the read")
        // every route has its branch, and each says the rule's sentence
        for (branch in listOf("if (next is ClientNames.Next.Here) {", "if (next is ClientNames.Next.Say) {", "if (next is ClientNames.Next.Bench) {"))
            assertTrue(go.contains(branch), branch)
        assertEquals(2, count(go, "renameFailed { tx -> Renames.benchRefused(tx, refused) }"), "a bench refusal is the bench's own sentence, on either route to it")
        assertTrue(between(go, asked.let { "val docname = ClientNames.bench(" }, "val outcome =").contains("return@launch"), "a bench refusal sends nothing to S5")
        assertFalse(go.contains("renameIds"), "no list is kept from one Save to the next")
        // the dialog's Save asks the clash first, on the tree as it stands, for every route a client has
        val save = between(main, "onSave = { newName ->", "blocker = deleteBlocker")
        val first = save.indexOf("val clash = clientClash(t, newName, cat.projects(masters).map { it.client to it.title })")
        assertTrue(first in 0 until save.indexOf("renameRoute(t)"), "before any route is followed")
        val rule = between(main, "fun clientClash(t: RenameTarget, typed: String, rows: List<Pair<String, String>>): Msg? {", "fun renameRoute(")
        assertEquals(2, count(rule, "ClientNames.refusal(tx, t.client, to, rows, begun)"), "the rule is :pano's, in the live language")
    }

    @Test fun `review - a client's Save and an upload's record take one gate, and the record's name is read inside it`() {
        val rename = app("ClientRename.kt")
        assertTrue(rename.contains("val GATE = Mutex()"))
        val go = between(app("MainActivity.kt"), "fun renameClient(t: RenameTarget, newName: String) {", "renaming?.let { t ->")
        val gate = go.indexOf("ClientRename.GATE.withLock {")
        assertTrue(gate in 0 until go.indexOf("S5Masters.fetch("), "from the read of S5's list")
        assertTrue(go.indexOf("ClientRename.finish(") > gate, "until the phone's rows have moved")
        val up = app("AsIsUpload.kt")
        assertEquals(1, count(up, "ClientRename.GATE.withLock { putRecord() }"), "the upload's first record PUT")
        val put = between(up, "fun putRecord() {", "ClientRename.GATE.withLock { putRecord() }")
        assertTrue(put.contains("val named = CaptureStore(ctx).all().firstOrNull { it.deviceId == id }?.customerName ?: c.customerName"), "the capture's row as it is NOW, not as the upload began")
        assertTrue(put.contains("val sent = ClientNames.recordName(named, customer)"))
        assertTrue(put.contains("if (sent == null) body.remove(ClientNames.KEY) else body[ClientNames.KEY] = sent"))
        assertTrue(between(up, "} catch (e: kotlinx.coroutines.CancellationException) {", "} catch (e: Exception) {").contains("throw e"), "a cancelled wait for the gate is not an upload that failed")
    }

    @Test fun `review - S5's client names go at sign-out, are marked with the login that read them, and are taken only through the claim`() {
        val home = app("S5Clients.kt")
        val out = between(app("S5Session.kt"), "fun signOut(context: Context", "\n    }\n")
        assertTrue(out.contains("S5Clients.forgetAll(context)"), "sign-out empties them")
        assertTrue(between(app("FrappeClient.kt"), "fun forget(context: Context", "\n        }\n").contains("if (!S5Session.signedIn(context)) S5Clients.forgetAll(context)"),
            "and so does forgetting the legacy key, where that key was the login")
        assertTrue(between(home, "fun held(context: Context, project: String)", "fun all(").contains("ClientNames.shown(ownerHeld(context), ownerNow(context), decode("))
        assertTrue(between(home, "fun all(context: Context)", "fun forget(").contains("if (!ClientNames.kept(ownerHeld(context), ownerNow(context))) return out"))
        val claim = between(home, "private fun claim(context: Context, listed: Int, keyed: Int): Boolean {", "fun take(")
        assertTrue(claim.contains("if (ClientNames.forgets(ownerHeld(context), now, listed, keyed)) {") && claim.contains("if (now.isEmpty() || keyed == 0) return false"))
        assertTrue(home.contains("claim(context, listed = 1, keyed = 1) && takeList(context, listOf(project to copy)) > 0"))
        assertTrue(home.contains("if (claim(context, listed, rows.size)) takeList(context, rows) else 0"))
        assertTrue(home.contains("private fun takeList("), "nothing is written but through take and takeRead")
        assertEquals(2, count(home, "takeList(context, ") - count(home, "fun takeList(context, "), "its two callers")
        // the mark is the login S5 is called with: the session's, else the legacy key's -- the key alone, never its secret
        val now = between(home, "private fun ownerNow(context: Context): String = ClientNames.owner(", "private fun ownerHeld(")
        assertTrue(now.contains("if (S5Session.signedIn(context)) S5Session.loginKey(context) else null,"))
        assertTrue(now.contains("FrappeClient.authorization(context)?.substringAfter(' ')?.substringBefore(':'))"))
    }

    @Test fun `review - the bench is told by one call judged on the rename alone, and its masters come back in a call of their own`() {
        val rename = app("ClientRename.kt")
        val bench = between(rename, "fun bench(context: Context, docname: String, name: String, noServer: String): String? = try {", "fun refresh(context: Context) {")
        assertTrue(bench.contains("client.renameNode(\"client\", docname, name)") && !bench.contains("bootstrap()"), "what decided the rename is the rename")
        assertTrue(bench.contains("e.message.orEmpty()"), "a refusal in the bench's own words")
        val refresh = between(rename, "fun refresh(context: Context) {", "fun seed(context: Context) {")
        assertTrue(refresh.contains("bootstrap()") && refresh.contains("S5Masters.fetch(context, erpLive = S5Masters.erpLive(context))"), "S5's list is folded back in")
        assertEquals(2, count(refresh, "runCatching {"), "never throws")
    }
}
