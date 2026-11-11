package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.RenameRoute.Line
import com.malletcrafts.interiorestimate.pano.RenameRoute.Write
import com.malletcrafts.interiorestimate.pano.Renames.Held
import com.malletcrafts.interiorestimate.pano.Renames.Now
import com.malletcrafts.interiorestimate.pano.Renames.Outcome
import com.malletcrafts.interiorestimate.pano.Renames.Verdict
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * WHERE A RENAME GOES (port 62 slice A2): one case per row kind of the tree, the site's S5 route with a base per
 * project, R9's unlisted capture on both routes, the requests pinned to the bodies S5 accepted and S5's every answer
 * read into its verdict ([S5Site]: S5's own answers, from its router on its real SQL), what a Save over several
 * projects comes to and the line it draws, the order S5's names are kept in, the seed -- and, read as source because
 * only CI compiles :app, that the phone's Save takes these routes and no other.
 */
class RenameRouteTest {

    private val en = Tx.EN
    private val P = "PROJ-DEMO"

    private fun site(
        name: String = "Demo Site, Pune", serverId: String = "", hasLocalRow: Boolean = false, s5: List<Write> = emptyList(),
        signedIn: Boolean = true, legacyPair: Boolean = false, unlisted: Boolean = false, listRead: Boolean = false, tx: Tx = en,
    ) = Renames.route(tx, "site", name, serverId, hasLocalRow, s5, signedIn, legacyPair, unlisted, listRead)

    private val shutWords = "This site cannot be renamed from this phone: S5 does not hold it and it was not made here."

    // ---- the route, one case per row kind ------------------------------------------------------------------

    @Test fun `a site S5 holds renames through S5 - with a local row or without - and never through the bench`() {
        val w = listOf(Write(P, "Demo Site, Pune"))
        assertEquals(RenameRoute.S5(w, readFirst = false), site(s5 = w))
        assertEquals(RenameRoute.S5(w, readFirst = false), site(s5 = w, hasLocalRow = true))
        // the bench's docname and a stored ERP pair change nothing: a site is never sent to the bench (R1, R5)
        assertEquals(RenameRoute.S5(w, readFirst = false), site(s5 = w, serverId = "MEST-SITE-0005", legacyPair = true))
        assertEquals(Line.S5, Renames.line(site(s5 = w)))
        assertFalse(Renames.shut(site(s5 = w)))
        // a project S5 lists with NO name is still S5's to name: its base is ''
        assertEquals(RenameRoute.S5(listOf(Write(P, "")), readFirst = false), site(s5 = listOf(Write(P, "")), hasLocalRow = true))
    }

    @Test fun `a base PER PROJECT - two projects of one phone-site can hold two spellings, and each write carries its own`() {
        // the phone groups a site by a normalised key; S5 compares the stored string exactly (the plan's critic, finding 10)
        val two = listOf(Write("PROJ-1", "Demo Site, Pune"), Write("PROJ-2", "demo_site,  pune"))
        val r = assertIs<RenameRoute.S5>(site(s5 = two))
        assertEquals(two, r.writes)
        assertEquals(listOf("Demo Site, Pune", "demo_site,  pune"), r.writes.map { it.base }, "never one shared base, never a tidied one")
        // one write per project: a project listed twice is sent once, and an entry with no project is not sent
        assertEquals(listOf(Write("PROJ-1", "A")), assertIs<RenameRoute.S5>(site(s5 = listOf(Write("PROJ-1", "A"), Write("PROJ-1", "B"), Write(" ", "C")))).writes)
    }

    @Test fun `a site made on this phone and not on S5 is a local rename`() {
        assertEquals(RenameRoute.Local, site(hasLocalRow = true))
        assertEquals(Line.PHONE, Renames.line(RenameRoute.Local))
        // signed in or not, a pair or not: nothing is sent anywhere for a row that is words on this phone
        assertEquals(RenameRoute.Local, site(hasLocalRow = true, signedIn = false, legacyPair = true))
    }

    @Test fun `nothing on S5 and nothing made here is refused from the start - the demo site, the row gone, a bootstrap-only site (R3)`() {
        // the demo site: appended to the tree in memory, never a local row, never on S5
        val demo = site(name = TourDemo.SITE)
        assertEquals(RenameRoute.Blocked(shutWords, Line.SHUT), demo)
        assertTrue(Renames.shut(demo)); assertEquals(Line.SHUT, Renames.line(demo))
        // an S5-listed project's site whose phone row is gone and S5's list no longer names it: no local success that renames nothing
        assertEquals(RenameRoute.Blocked(shutWords, Line.SHUT), site(name = "Main site"))
        // a site only an old ERP bootstrap named: the docname is there, a pair even -- and the bench is still not called
        for (pair in listOf(true, false)) for (signed in listOf(true, false))
            assertEquals(RenameRoute.Blocked(shutWords, Line.SHUT), site(name = "Main site", serverId = "MEST-SITE-0002", legacyPair = pair, signedIn = signed), "pair=$pair signed=$signed")
    }

    @Test fun `the (no site) bucket has no name to change, whatever is filed in it`() {
        val listed = listOf(Write(P, ""))
        for (n in listOf("(no site)", " (No  Site) ", "", "  ")) {
            val r = site(name = n, s5 = listed, hasLocalRow = true)
            assertEquals(RenameRoute.Blocked(shutWords, Line.SHUT), r, "'$n'")
        }
        assertEquals("(no site)", Renames.NO_SITE)
        assertFalse(Renames.named("(no site)")); assertFalse(Renames.named(null)); assertTrue(Renames.named("Main site"))
        // and it IS the app's bucket: Catalogue files a project with no site under this very string
        assertTrue(app("Catalogue.kt").contains("const val NO_SITE = \"${Renames.NO_SITE}\""), "Catalogue.NO_SITE is Renames.NO_SITE")
    }

    @Test fun `signed out of S5 - a site S5 holds is refused at once, under the S5 line, and nothing is sent`() {
        val r = site(s5 = listOf(Write(P, "Demo Site, Pune")), signedIn = false, hasLocalRow = true)
        assertEquals(RenameRoute.Blocked("Not signed in to S5, so not saved — sign in (menu → Server) and Save again", Line.S5), r)
        assertFalse(Renames.shut(r), "the name stays editable: the reason is said under it when Save is tapped")
        // never the local rename instead, though a local row is there
        assertTrue(r !is RenameRoute.Local)
    }

    @Test fun `a capture still on its way (R9) - the S5 route reads S5's list first, and refuses if it is still not listed - the local route refuses at once`() {
        val w = listOf(Write(P, "Demo Site, Pune"))
        val way = "A capture of this site has not reached S5 yet — rename the site once it has; not saved"
        // S5 route: the list is read first (the Save needs the signal anyway) and the route asked again
        assertEquals(RenameRoute.S5(w, readFirst = true), site(s5 = w, unlisted = true))
        // read, and the capture's project is on the list now: it renames (the caller's writes then hold that project too)
        assertEquals(RenameRoute.S5(w, readFirst = false), site(s5 = w, unlisted = false, listRead = true))
        // read, and still not listed: refused, under the S5 line
        val still = site(s5 = w, unlisted = true, listRead = true)
        assertEquals(RenameRoute.Blocked(way, Line.S5), still); assertFalse(Renames.shut(still))
        // a site that is not on S5: known on the phone, said at once (the prototype's rn62Save), whether or not a list was read
        for (read in listOf(false, true)) assertEquals(RenameRoute.Blocked(way, Line.PHONE), site(hasLocalRow = true, unlisted = true, listRead = read), "read=$read")
        // signed out wins: nothing can be read or sent
        assertEquals(Line.S5, assertIs<RenameRoute.Blocked>(site(s5 = w, unlisted = true, signedIn = false)).line)
        assertTrue(assertIs<RenameRoute.Blocked>(site(s5 = w, unlisted = true, signedIn = false)).reason.startsWith("Not signed in to S5"))
    }

    @Test fun `a client and a project keep their two routes - with a refusal in place of a call that cannot succeed`() {
        for (kind in listOf("client", "project")) {
            fun r(serverId: String, local: Boolean, pair: Boolean, name: String = "Asha flat") =
                Renames.route(en, kind, name, serverId, local, listOf(Write(P, "x")), signedIn = true, legacyPair = pair, unlisted = true)
            // the bench holds it and a pair is stored: rename_node by docname, as before port 62
            assertEquals(RenameRoute.Legacy("PROJ-0003"), r("PROJ-0003", local = false, pair = true), kind)
            assertEquals(RenameRoute.Legacy("PROJ-0003"), r(" PROJ-0003 ", local = true, pair = true), kind)
            assertEquals(Line.SERVER, Renames.line(r("PROJ-0003", local = false, pair = true)))
            // the bench holds it and NO pair is stored: the call cannot be made -- said, not attempted (R1, R2)
            val noPair = r("PROJ-0003", local = true, pair = false)
            assertEquals(RenameRoute.Blocked("Could not rename: no server configured", Line.SERVER), noPair, kind)
            assertFalse(Renames.shut(noPair))
            // made on this phone: the local rename; whitespace is not a docname
            assertEquals(RenameRoute.Local, r("", local = true, pair = false), kind)
            assertEquals(RenameRoute.Local, r("\t ", local = true, pair = true), kind)
            // no docname and no row here: nothing a rename could change, so no local "success" (R3's shape)
            assertEquals(RenameRoute.Blocked("Could not rename: Asha flat", Line.PHONE), r("", local = false, pair = true), kind)
            // S5's list and R9's capture are a site's alone: neither moves a client or a project off its route
            assertTrue(r("", local = true, pair = false) !is RenameRoute.S5)
        }
        // the client bucket is named as the tree names it
        assertEquals("Could not rename: (no client)",
            assertIs<RenameRoute.Blocked>(Renames.route(en, "client", "(no client)", "", false, emptyList(), true, false, false)).reason)
    }

    @Test fun `each refusal says the prototype's Marathi under Tx MR`() {
        val mr = Tx.MR
        assertEquals("या फोनवरून या साइटचे नाव बदलता येत नाही: ती S5 वर नाही आणि इथे बनवलेलीही नाही.",
            assertIs<RenameRoute.Blocked>(site(tx = mr)).reason)
        assertEquals("S5 मध्ये साइन इन केलेले नाही, म्हणून सेव्ह झाले नाही — साइन इन करा (मेनू → सर्व्हर) आणि पुन्हा सेव्ह करा",
            assertIs<RenameRoute.Blocked>(site(tx = mr, s5 = listOf(Write(P, "x")), signedIn = false)).reason)
        assertEquals("या साइटचे एक कॅप्चर अजून S5 वर पोहोचलेले नाही — ते पोहोचल्यावर साइटचे नाव बदला; सेव्ह झाले नाही",
            assertIs<RenameRoute.Blocked>(site(tx = mr, hasLocalRow = true, unlisted = true)).reason)
        assertEquals("ऑफलाइन — S5 उत्तर देईल तेव्हाच नाव बदलते; सेव्ह झाले नाही", Renames.text(mr, Outcome.Failed(Verdict.Offline, Write(P, "x"))))
        assertEquals("2 पैकी 1 प्रोजेक्टवर नाव बदलले — पूर्ण करण्यासाठी पुन्हा सेव्ह करा", Renames.text(mr, Outcome.Part(1, 2)))
        assertEquals("मधल्या काळात lead@mcft.test यांनी नाव बदलले — आता Demo Site, Baner; सेव्ह झाले नाही",
            Renames.text(mr, Outcome.Failed(verdict(409, S5Site.STALE), Write(P, "Demo Site, Kothrud"))))
    }

    // ---- the wire: PUT /api/asis/:project/site {site, base} --------------------------------------------------

    private fun tree(s: String): Any? = MiniJson.parse(s)
    private fun verdict(code: Int?, body: String?): Verdict = Renames.siteVerdict(MiniRead, code, body?.let { runCatching { tree(it) }.getOrNull() })

    /** [v] as org.json writes `JSONObject(fields)`, as QuoteMoveTest serialises a move. */
    private fun json(v: Any?): String = when (v) {
        null -> "null"
        is String -> "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        is Number, is Boolean -> v.toString()
        is Map<*, *> -> v.entries.joinToString(",", "{", "}") { json(it.key as String) + ":" + json(it.value) }
        is List<*> -> v.joinToString(",", "[", "]") { json(it) }
        else -> fail("not a JSON value: $v (${v::class})")
    }

    private fun body(project: String, site: String, base: String): String = json(Renames.siteRequest(project, site, base).fields)

    @Test fun `each write is exactly the body S5 accepted - built by Renames, serialised as org-json writes it`() {
        assertEquals(S5Site.SENT_NAMED_FIRST, body(P, "Demo Site, Pune", ""))
        assertEquals(S5Site.SENT_SAME, body(P, "Demo Site, Pune", ""))
        assertEquals(S5Site.SENT_RENAMED, body(P, "Demo Site, Kothrud", "Demo Site, Pune"))
        assertEquals(S5Site.SENT_RENAMED_BY_LEAD, body(P, "Demo Site, Baner", "Demo Site, Kothrud"))
        assertEquals(S5Site.SENT_STALE, body(P, "Demo Site, Aundh", "Demo Site, Kothrud"))
        assertEquals(S5Site.SENT_STALE_NONE, body("PROJ-TWO", "Demo Site, Aundh", "Demo Site, Pune"))
        assertEquals(S5Site.SENT_STALE_AGAIN, body(P, "Demo Site, Aundh", "Demo Site, Baner"))
        for (sent in listOf(S5Site.SENT_VENDOR, S5Site.SENT_CLIENT, S5Site.SENT_NO_SIGN_IN, S5Site.SENT_KEY_REFUSED))
            assertEquals(sent, body(P, "Demo Site, Aundh", "Demo Site, Baner"))
        assertEquals(S5Site.SENT_NEVER, body("PROJ-NEVER", "Demo Site, Aundh", ""))
        assertEquals(S5Site.SENT_BAD_NAME, body(P, "D".repeat(141), "Demo Site, Baner"))
        // the one the phone never builds: every request carries a base
        assertEquals("""{"site":"Demo Site, Aundh"}""", S5Site.SENT_BAD_BASE)
        assertEquals(listOf("site", "base"), Renames.siteRequest(P, "x", "").fields.keys.toList())
    }

    @Test fun `the route, the JSON types, and the base sent byte for byte - never trimmed, folded or re-cased`() {
        val enc: (String) -> String = { s -> s.replace("%", "%25").replace(" ", "%20").replace("/", "%2F") }
        val r = Renames.siteRequest("Demo flat/1", "Demo Site, Kothrud", "Demo Site, Pune")
        assertEquals("PUT /api/asis/Demo%20flat%2F1/site application/json", "${r.method} ${r.path(enc)} ${r.contentType}")
        assertEquals(listOf("Demo flat/1", "site"), r.segments)
        assertNull(r.file)
        assertTrue(r.fields.values.all { it is String })
        // S5's guard compares the stored string exactly: a base is S5's own spelling, sent as it is
        for (base in listOf("", " Demo Site ", "demo_site,  pune", "मुख्य साइट", "Main site")) {
            assertEquals(base, Renames.siteRequest(P, "New", base).fields["base"])
            assertEquals(base, Renames.siteBody("New", base)["base"])
        }
    }

    private val tFirst = at(S5Site.NAMED_FIRST)
    private fun at(body: String): String = (tree(body) as Map<*, *>)["site_at"] as String

    @Test fun `S5's every answer is read into its verdict from the body's fields`() {
        assertEquals(Verdict.Renamed(Now("Demo Site, Pune", "staff@mcft.test", tFirst)), verdict(200, S5Site.NAMED_FIRST))
        // the same name again: not written, and S5 says who set it and when -- the stamp of the FIRST write
        assertEquals(Verdict.Same(Now("Demo Site, Pune", "staff@mcft.test", tFirst)), verdict(200, S5Site.SAME))
        assertEquals(Verdict.Renamed(Now("Demo Site, Kothrud", "staff@mcft.test", at(S5Site.RENAMED))), verdict(200, S5Site.RENAMED))
        val lead = Now("Demo Site, Baner", "lead@mcft.test", at(S5Site.RENAMED_BY_LEAD))
        assertEquals(Verdict.Renamed(lead), verdict(200, S5Site.RENAMED_BY_LEAD))
        // 409: S5's words and the name AS STORED, which is the base the next Save sends
        assertEquals(Verdict.Stale("Renamed by lead@mcft.test meanwhile — now Demo Site, Baner; not saved", lead), verdict(409, S5Site.STALE))
        assertEquals(Verdict.Stale("This project has no site name on S5 yet: send base \"\" to give it one; not saved", Now("", "", "")), verdict(409, S5Site.STALE_NONE))
        assertEquals(Verdict.Stale("Changed meanwhile; not saved — send it again", lead), verdict(409, S5Site.STALE_AGAIN))
        // refusals: S5's own words
        assertEquals(Verdict.Refused(403, "Only MCFT staff can rename a site"), verdict(403, S5Site.VENDOR))
        assertEquals(Verdict.Refused(403, "Only MCFT staff can rename a site"), verdict(403, S5Site.CLIENT))
        assertEquals(Verdict.Refused(404, "This project has never been uploaded"), verdict(404, S5Site.NEVER))
        assertEquals(Verdict.Refused(400, "site must be the site's name: text of 1 to 140 characters, on one line, with no control characters"), verdict(400, S5Site.BAD_NAME))
        assertEquals(Verdict.Refused(400, "base must be the site name last seen, or \"\" when the project has none"), verdict(400, S5Site.BAD_BASE))
        assertEquals(Verdict.Refused(401, "Sign in with your ERP account"), verdict(401, S5Site.NO_SIGN_IN))
        assertEquals(Verdict.Refused(401, "ERP did not accept this API key"), verdict(401, S5Site.KEY_REFUSED))
        assertEquals(Verdict.Refused(413, "Request too large"), verdict(413, """{"error":"Request too large"}"""))
    }

    @Test fun `no answer of S5's handler is Offline - nothing came back, a page that is not S5's, a 5xx, a 200 without S5's fields`() {
        assertEquals(Verdict.Offline, verdict(null, null))
        assertEquals(Verdict.Offline, verdict(null, S5Site.NAMED_FIRST), "no status is no answer, whatever was parsed")
        assertEquals(Verdict.Offline, verdict(200, "<html>captive portal</html>"))
        assertEquals(Verdict.Offline, verdict(200, "[1,2]"))
        assertEquals(Verdict.Offline, verdict(200, """{"ok":true}"""))
        assertEquals(Verdict.Offline, verdict(200, """{"ok":true,"written":true,"site":""}"""), "S5 never stores a blank name")
        assertEquals(Verdict.Offline, verdict(200, """{"ok":false,"written":true,"site":"A"}"""))
        assertEquals(Verdict.Offline, verdict(200, """{"ok":true,"written":"yes","site":"A"}"""))
        assertEquals(Verdict.Offline, verdict(409, """{"error":"Renamed"}"""), "a 409 without the name as stored is not S5's")
        assertEquals(Verdict.Offline, verdict(409, """{"site":"A"}"""))
        for (code in listOf(400, 401, 403, 404, 413)) assertEquals(Verdict.Offline, verdict(code, """{"message":"nope"}"""), "$code")
        for (code in listOf(405, 500, 502, 503)) assertEquals(Verdict.Offline, verdict(code, """{"error":"x","site":"A"}"""), "$code")
    }

    // ---- what a Save comes to, and the line under the name -------------------------------------------------

    private val w1 = Write("PROJ-1", "Demo Site, Pune")
    private val w2 = Write("PROJ-2", "Demo Site, Pune")
    private fun renamed(site: String = "Demo Site, Kothrud") = Verdict.Renamed(Now(site, "staff@mcft.test", "2026-11-11T17:45:42.211Z"))
    private fun same(site: String = "Demo Site, Kothrud") = Verdict.Same(Now(site, "staff@mcft.test", "2026-11-11T17:45:42.211Z"))
    private val stale = verdict(409, S5Site.STALE)

    @Test fun `success is S5's answers alone - every project Renamed or Same`() {
        assertEquals(Outcome.Done("Demo Site, Kothrud"), Renames.outcome(listOf(w1), listOf(renamed())))
        assertEquals(Outcome.Done("Demo Site, Kothrud"), Renames.outcome(listOf(w1), listOf(same())), "the retry after a lost answer is done, not an error")
        assertEquals(Outcome.Done("Demo Site, Kothrud"), Renames.outcome(listOf(w1, w2), listOf(same(), renamed())), "Save again after 1 of 2")
        assertNull(Renames.text(en, Outcome.Done("Demo Site, Kothrud")))
        // no writes and no answers is not a success
        assertIs<Outcome.Failed>(Renames.outcome(emptyList(), emptyList()))
    }

    @Test fun `1 of 2 - a site with two projects on S5, one renamed and the other still able to take the name`() {
        // no signal after the first landed (the Save stopped: one answer for two writes)
        assertEquals(Outcome.Part(1, 2), Renames.outcome(listOf(w1, w2), listOf(renamed(), Verdict.Offline)))
        assertEquals(Outcome.Part(1, 2), Renames.outcome(listOf(w1, w2), listOf(renamed())), "stopped with nothing said for the second: no answer")
        // the other project was renamed by somebody meanwhile: the phone holds S5's name for it now, so Save again finishes
        assertEquals(Outcome.Part(1, 2), Renames.outcome(listOf(w1, w2), listOf(stale, renamed())))
        assertEquals(Outcome.Part(2, 3), Renames.outcome(listOf(w1, w2, Write("PROJ-3", "")), listOf(renamed(), same(), Verdict.Offline)))
        assertEquals("Renamed on 1 of 2 projects — Save again to finish", Renames.text(en, Outcome.Part(1, 2)))
        // a refusal Save again cannot get past speaks for itself, renamed projects or not
        val never = verdict(404, S5Site.NEVER)
        assertEquals(Outcome.Failed(never, w2), Renames.outcome(listOf(w1, w2), listOf(renamed(), never)))
        assertEquals("This project has never been uploaded", Renames.text(en, Outcome.Failed(never, w2)))
    }

    @Test fun `the line under the name - offline, S5's own 409 through its key, S5's other words as sent, a 401 as the sign-in line`() {
        // nothing renamed and no answer: the offline line
        val off = Renames.outcome(listOf(w1, w2), listOf(Verdict.Offline))
        assertEquals(Outcome.Failed(Verdict.Offline, w1), off)
        assertEquals("Offline — the name changes only when S5 answers; not saved", Renames.text(en, off))
        // S5's 409 for a site renamed meanwhile: drawn from the reply's FIELDS, and it reads as S5's own sentence
        val o = Renames.outcome(listOf(Write(P, "Demo Site, Kothrud")), listOf(stale))
        assertEquals(Outcome.Failed(stale, Write(P, "Demo Site, Kothrud")), o)
        assertEquals("Renamed by lead@mcft.test meanwhile — now Demo Site, Baner; not saved", Renames.text(en, o))
        assertEquals((tree(S5Site.STALE) as Map<*, *>)["error"], Renames.text(en, o), "the keyed line is S5's sentence, word for word")
        // the other two 409s are S5's words as sent: no name stored; the stored name is the base after all
        assertEquals("This project has no site name on S5 yet: send base \"\" to give it one; not saved",
            Renames.text(en, Outcome.Failed(verdict(409, S5Site.STALE_NONE), Write("PROJ-TWO", "Demo Site, Pune"))))
        assertEquals("Changed meanwhile; not saved — send it again",
            Renames.text(en, Outcome.Failed(verdict(409, S5Site.STALE_AGAIN), Write(P, "Demo Site, Baner"))))
        // a 409 that names nobody is S5's words too: the key has no word for "somebody"
        assertEquals("Renamed by somebody meanwhile — now B; not saved",
            Renames.text(en, Outcome.Failed(Verdict.Stale("Renamed by somebody meanwhile — now B; not saved", Now("B", "", "t")), Write(P, "A"))))
        // S5's refusals, shown as S5 sent them
        assertEquals("Only MCFT staff can rename a site", Renames.text(en, Outcome.Failed(verdict(403, S5Site.VENDOR), w1)))
        assertEquals("site must be the site's name: text of 1 to 140 characters, on one line, with no control characters",
            Renames.text(en, Outcome.Failed(verdict(400, S5Site.BAD_NAME), w1)))
        // a 401 -- no sign-in, or a key ERP refuses (the legacy pair that died on 23 Oct) -- is the sign-in line
        for (b in listOf(S5Site.NO_SIGN_IN, S5Site.KEY_REFUSED))
            assertEquals("Not signed in to S5, so not saved — sign in (menu → Server) and Save again", Renames.text(en, Outcome.Failed(verdict(401, b), w1)))
    }

    @Test fun `a Save stops where sending on cannot help, and goes on where the failure is one project's alone`() {
        assertTrue(Renames.stops(Verdict.Offline))
        for (code in listOf(400, 401, 403, 413)) assertTrue(Renames.stops(Verdict.Refused(code, "x")), "$code is the caller's or the name's: the same for every project")
        assertFalse(Renames.stops(Verdict.Refused(404, "x")), "a project S5 does not hold says nothing of the next")
        assertFalse(Renames.stops(stale)); assertFalse(Renames.stops(renamed())); assertFalse(Renames.stops(same()))
    }

    // ---- S5's names as the phone keeps them, the list, the seed --------------------------------------------

    @Test fun `S5's list rows give each project's name and stamp - and an S5 without site names gives none`() {
        fun rows(body: String) = ((tree(body) as Map<*, *>)["projects"] as List<*>).map { Renames.listedOf(MiniRead, it) }
        assertEquals(listOf("PROJ-TWO" to Held("", ""), P to Held("", "")), rows(S5Site.LIST_NONE))
        val named = rows(S5Site.LIST_NAMED).associate { it!! }
        assertEquals(Held("Demo Site, Baner", at(S5Site.RENAMED_BY_LEAD)), named[P], "the last rename, and an upload naming the first name again changed nothing")
        assertEquals(Held("", ""), named["PROJ-TWO"])
        assertEquals("Demo Office, Pune", named["PROJ-UP"]?.site, "named by its first upload alone: the second upload's site was not taken")
        // an S5 before slice A1 sends no `site`: not "listed with no name" -- unknown, so nothing is routed or seeded on it
        assertNull(Renames.listedOf(MiniRead, tree("""{"project":"PROJ-OLD","title":"Demo flat","quote_stage":"Survey"}""")))
        assertNull(Renames.listedOf(MiniRead, tree("""{"project":"PROJ-OLD","site":7}""")))
        assertNull(Renames.listedOf(MiniRead, tree("""{"project":" ","site":"A","site_at":"t"}""")))
        assertNull(Renames.listedOf(MiniRead, tree("""{"site":"A"}""")))
        assertNull(Renames.listedOf(MiniRead, null))
        // an answer's name is kept the same way
        assertEquals(Held("Demo Site, Pune", tFirst), Renames.heldOf(Renames.nowIn(verdict(200, S5Site.NAMED_FIRST))!!))
        assertEquals(Held("Demo Site, Baner", at(S5Site.RENAMED_BY_LEAD)), Renames.heldOf(Renames.nowIn(verdict(409, S5Site.STALE))!!), "a 409's stored name is the next base")
        assertNull(Renames.nowIn(verdict(403, S5Site.VENDOR))); assertNull(Renames.nowIn(Verdict.Offline))
    }

    @Test fun `a name is replaced by S5's own stamp, never by arrival - a list read that went out earlier cannot undo a rename`() {
        val old = Held("Demo Site, Pune", tFirst)
        val new = Held("Demo Site, Kothrud", at(S5Site.RENAMED))
        assertTrue(tFirst < at(S5Site.RENAMED), "S5's stamps order as text: one fixed-width UTC form")
        assertTrue(Renames.takes(null, old)); assertTrue(Renames.takes(null, Held("", "")))
        assertTrue(Renames.takes(old, new), "the rename's answer")
        assertFalse(Renames.takes(new, old), "a sync's list read that went out before the rename lands after it")
        assertTrue(Renames.takes(old, old), "the same write again")
        assertTrue(Renames.takes(Held("", ""), old), "the first name")
        assertFalse(Renames.takes(old, Held("", "")), "S5 never clears a name: a row with none is older than any name")
    }

    @Test fun `the seed - S5 lists the project with no name and the phone knows one - first write wins, never the demo`() {
        assertEquals(Renames.Seed(P, "Main site"), Renames.seed(P, "Asha P", "Asha flat", Held("", ""), " Main site "))
        // the body is the first-name PUT S5 accepted: base ''
        val s = Renames.seed(P, "Asha P", "Asha flat", Held("", ""), "Demo Site, Pune")!!
        assertEquals(S5Site.SENT_NAMED_FIRST, body(s.project, s.site, ""))
        // S5 already holds a name (the phone's or not): nothing is sent -- a seed never renames
        assertNull(Renames.seed(P, "Asha P", "Asha flat", Held("Demo Site, Baner", "t"), "Main site"))
        // S5 does not list the project, or does not serve names: nothing to seed
        assertNull(Renames.seed(P, "Asha P", "Asha flat", null, "Main site"))
        // the phone knows no name: never the bucket's, never a blank
        for (n in listOf("", "  ", "(no site)", "(No Site)")) assertNull(Renames.seed(P, "Asha P", "Asha flat", Held("", ""), n), "'$n'")
        assertNull(Renames.seed(" ", "Asha P", "Asha flat", Held("", ""), "Main site"))
        // never the demo
        assertNull(Renames.seed(P, TourDemo.CLIENT, TourDemo.PROJECT, Held("", ""), TourDemo.SITE))
        assertNull(Renames.seed(TourDemo.PROJECT, "x", "y", Held("", ""), TourDemo.SITE))
        // the record's own `site` follows the same rule
        assertEquals("Main site", Renames.recordSite(" Main site ")); assertNull(Renames.recordSite("(no site)")); assertNull(Renames.recordSite("")); assertNull(Renames.recordSite(null))
    }

    // ---- :app, read as source (only CI compiles :app) -----------------------------------------------------

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

    @Test fun `the dialog's Save takes Renames' route - a site never reaches the bench, and no failure is said on a banner`() {
        val main = app("MainActivity.kt")
        val save = between(main, "onSave = { newName ->", "blocker = deleteBlocker")
        // the route is asked ONCE per Save, and the S5 route again after the list read
        assertTrue(save.contains("renameRoute(t)"), "Save asks Renames.route through renameRoute")
        assertTrue(save.contains("is RenameRoute.S5 ->") && save.contains("RenameRoute.Local ->") && save.contains("is RenameRoute.Legacy ->") && save.contains("is RenameRoute.Blocked ->"),
            "every route has its branch")
        // the bench is called on the Legacy route alone, with the route's docname
        assertEquals(1, Regex("renameNode\\(").findAll(main).count(), "one caller of rename_node")
        assertTrue(between(save, "is RenameRoute.Legacy ->", "is RenameRoute.S5 ->").contains("renameNode(t.kind, route.docname, newName)"), "and it is the Legacy route's")
        val legacy = between(save, "is RenameRoute.Legacy ->", "is RenameRoute.S5 ->")
        val s5 = between(save, "is RenameRoute.S5 ->", "is RenameRoute.Blocked ->")
        assertFalse(s5.contains("FrappeClient"), "the S5 route names no ERP client: no bench call for a site")
        assertTrue(s5.contains("SiteRename.rename(context, s5.writes, newName)"), "it sends through SiteRename, the writes :pano routed")
        // R6: a rename is judged on the rename alone. The bench rename's refresh is a call of its own, made after the
        // judged one and only when it succeeded; the S5 route reads no bootstrap at all, before or after.
        val i = legacy.indexOf("renameNode("); val j = legacy.indexOf("bootstrap()")
        assertTrue(i in 0 until j && legacy.substring(i, j).contains("if (res.isSuccess) runCatching {"), "the refresh is outside the call that decides the rename")
        assertFalse(s5.contains("bootstrap()"), "no bootstrap inside a site's rename")
        // R9: the list is read first only when the route says so, and the route is asked again with the list read
        assertTrue(s5.contains("if (route.readFirst)") && s5.contains("S5Masters.fetch(context)") && s5.contains("renameRoute(t, listRead = true)"), "S5's list first, then the route again")
        // the one step, only once S5 has answered for every project: the phone's rows, the room-scope key, the masters' rows
        val done = between(s5, "if (outcome is Renames.Outcome.Done)", "} else {")
        for (step in listOf("cat.renameLocal(\"site\", t.client, t.site, \"\", name)", "cat.moveRoomScope(t.client, t.site, name)", "cat.renameSiteRows(m, t.client, t.site, name)"))
            assertTrue(done.contains(step), "the success step holds `$step`")
        assertTrue(done.contains("val name = outcome.site"), "the name applied is S5's own answer")
        // R2: no failure of a rename goes to the banner any more
        assertFalse(main.contains("app_msg_could_not_rename_x"), "the could-not-rename banner is gone from MainActivity: the dialog says it")
        for ((branch, src) in listOf("Local" to between(save, "RenameRoute.Local ->", "is RenameRoute.Legacy ->"), "Legacy" to legacy, "S5" to s5, "Blocked" to save.substring(save.indexOf("is RenameRoute.Blocked ->"))))
            assertTrue(src.contains("renameFailed {"), "the $branch route says its failure in the dialog")
        assertEquals(3, Regex("renameFailed \\{").findAll(s5).count(), "the S5 route: no list read, still blocked after it, and S5's own answer")
        assertFalse(save.contains("lastResult = { tx -> Renames") || save.contains("lastResult = { _ ->"), "the banner carries a rename's success only")
        // R3: the local rename's Boolean is read
        assertTrue(Regex("if \\(!cat\\.renameLocal\\(t\\.kind, t\\.client, t\\.site, t\\.project, newName\\)\\)").containsMatchIn(save), "a local rename that renamed nothing is a failure")
    }

    @Test fun `S5's names have one home on the phone, fed by every list read and every answer - and the tree reads it`() {
        val sites = app("S5Sites.kt")
        assertTrue(sites.contains("Renames.takes(") && sites.contains("Renames.Held("), "kept by S5's own stamp")
        // the two list reads feed it, by :pano's reader
        val session = app("S5Session.kt")
        assertTrue(between(session, "fun fetch(context: Context)", "fun forget(context: Context)").contains("S5Sites.takeList(context, "), "S5Masters.fetch feeds the names")
        assertFalse(session.contains("m.put(\"site_name\""), "an S5 entry is no longer stamped with the local row's name (R4)")
        assertTrue(between(app("QuoteStageSync.kt"), "fun refresh(context: Context", "// ---- Record the approved scope").contains("S5Sites.takeList(context, "), "the list read both legs make feeds them too")
        // every answer to a PUT …/site is kept at once
        val rename = app("SiteRename.kt")
        assertTrue(rename.contains("Renames.siteRequest(") && rename.contains("Renames.siteVerdict(JsonRead, "), "the request and the verdict are :pano's")
        assertTrue(rename.contains("Renames.nowIn(v)?.let { S5Sites.take(context, "), "S5's name from every answer that carries one")
        assertTrue(rename.contains("S5Sites.held(context, w.project)?.site ?: w.base"), "the base sent is the name held NOW: a 409's stored name is what the next Save sends against")
        // the tree: S5's name first, by :pano's rule; the bootstrap's rows listed by :pano's rule
        val cat = app("Catalogue.kt")
        assertTrue(cat.contains("SiteRows.nameOf(") && cat.contains("SiteRows.listed("), "Catalogue reads the site's name and lists the bootstrap's rows through SiteRows")
        assertFalse(Regex("site = o\\.optString\\(\"site_name\"\\)\\.ifBlank \\{ NO_SITE \\}").containsMatchIn(cat), "the masters' copy alone no longer names a site")
        // a site's S5 projects: the tree's S5-listed ones, and a phone-made project S5 holds under its minted key -- so the
        // site stays S5's (and the dialog says "sign in") while sign-out has dropped the S5-only projects from the masters
        val mine = between(cat, "fun s5ProjectsOf(", "fun hasUnlistedCapture(")
        assertTrue(mine.contains("S5Sites.held(appContext, it) != null") && mine.contains("S5Auth.localKey(it.client, it.site, it.project)"), "both halves")
        assertTrue(mine.contains("it.site.isBlank() || same(it.site, site)"), "a minted project S5 names as another site is not this site's")
        // R9's capture: under a phone-minted key the MASTERS do not list (the key is re-derived from them), of this client and site
        val r9 = between(cat, "fun hasUnlistedCapture(", "fun renameSiteRows(")
        assertTrue(r9.contains("S5Auth.isLocalKey(c.project) && c.project !in listed") && r9.contains("same(localSiteFor(c.customerName, c.projectTitle), site)"), "the unlisted capture is told as the key was minted")
    }

    @Test fun `the seed and the record's site - sent after the S5 list read, never for the demo, the record's through AsIsPath`() {
        val sync = app("SyncWorker.kt")
        val leg = between(sync, "private fun s5Leg(store: CaptureStore)", "private data class ErpOutcome")
        val read = leg.indexOf("S5Masters.fetch(ctx)"); val seed = leg.indexOf("SiteRename.seed(ctx)")
        assertTrue(read >= 0 && seed > read, "the seed follows the list read in the S5 leg")
        val rename = app("SiteRename.kt")
        assertTrue(between(rename, "fun seed(context: Context)", "\n    }\n").contains("Renames.seed(p.serverId, p.client, p.title, S5Sites.held(context, p.serverId), p.site)"), "which projects are seeded is :pano's rule")
        // the upload's record carries the site as the tree names it
        val up = app("AsIsUpload.kt")
        assertTrue(between(up, "val body = AsIsPath.body(", "val recordUrl").contains("site = Catalogue(ctx).siteFor(CaptureStore(ctx).masters(), c.project, c.customerName, c.projectTitle),"), "the record's site is the tree's name for the capture's project")
    }
}
