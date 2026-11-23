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

    // `erpLive` was called legacy-pair until port 63 slice F4b: Renames.route's parameter was renamed with its meaning ("a
    // pair is stored" became "a LIVE pair: stored and not refused"), so the six cases below that named the old argument
    // name the new one. A site never reads it, so no site case changed what it expects.
    private fun site(
        name: String = "Demo Site, Pune", serverId: String = "", hasLocalRow: Boolean = false, s5: List<Write> = emptyList(),
        signedIn: Boolean = true, erpLive: Boolean = false, unlisted: Boolean = false, listRead: Boolean = false, tx: Tx = en,
        heard: Boolean = true, mayHold: Boolean = false,
    ) = Renames.route(tx, "site", name, serverId, hasLocalRow, s5, signedIn, erpLive, unlisted, listRead, heard, mayHold)

    private val shutWords = "This site cannot be renamed from this phone: S5 does not hold it and it was not made here."

    // ---- the route, one case per row kind ------------------------------------------------------------------

    @Test fun `a site S5 holds renames through S5 - with a local row or without - and never through the bench`() {
        val w = listOf(Write(P, "Demo Site, Pune"))
        assertEquals(RenameRoute.S5(w, readFirst = false), site(s5 = w))
        assertEquals(RenameRoute.S5(w, readFirst = false), site(s5 = w, hasLocalRow = true))
        // the bench's docname and a stored ERP pair change nothing: a site is never sent to the bench (R1, R5)
        assertEquals(RenameRoute.S5(w, readFirst = false), site(s5 = w, serverId = "MEST-SITE-0005", erpLive = true))
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
        assertEquals(RenameRoute.Local, site(hasLocalRow = true, signedIn = false, erpLive = true))
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
            assertEquals(RenameRoute.Blocked(shutWords, Line.SHUT), site(name = "Main site", serverId = "MEST-SITE-0002", erpLive = pair, signedIn = signed), "pair=$pair signed=$signed")
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

    @Test fun `S5's list not heard yet on this phone - a site that may be on S5 reads the list first, never SHUT and never a local rename (the A2 review, 1)`() {
        // After the update that brings S5's names to the phone, nothing has read S5's list yet: no project of any site is
        // named, and that is NOT S5 saying it does not hold the site. A site with a project that carries a server id, or
        // a capture that carries a project id, may be on S5 -- so the Save reads S5's list first and the route is asked again.
        val first = RenameRoute.S5(emptyList(), readFirst = true)
        // no row made here: it used to open as "cannot be renamed — S5 does not hold it", the name greyed and Save off
        val bare = site(name = "Main site", serverId = "MEST-SITE-0002", heard = false, mayHold = true)
        assertEquals(first, bare)
        assertFalse(Renames.shut(bare), "the name is editable: nothing says yet that S5 does not hold it")
        assertEquals(Line.S5, Renames.line(bare))
        // a row made here: it used to rename on the phone alone and say "Renamed", with nothing sent to S5
        val row = site(hasLocalRow = true, heard = false, mayHold = true)
        assertEquals(first, row); assertTrue(row !is RenameRoute.Local)
        // a stored pair changes nothing (the phone this was found on holds one): a site never takes the bench's route
        assertEquals(first, site(hasLocalRow = true, serverId = "MEST-SITE-0005", erpLive = true, heard = false, mayHold = true))
        // R9's capture is S5's to answer too once the list is read: not the Local route's refusal before it
        assertEquals(first, site(hasLocalRow = true, unlisted = true, heard = false, mayHold = true))
        // signed out, the list cannot be read: said, under the S5 line, the name editable -- never SHUT, never Local
        for (local in listOf(true, false)) {
            val out = site(hasLocalRow = local, signedIn = false, heard = false, mayHold = true)
            assertEquals(RenameRoute.Blocked("Not signed in to S5, so not saved — sign in (menu → Server) and Save again", Line.S5), out, "local=$local")
            assertFalse(Renames.shut(out))
        }
        // THE LIST READ FOR THIS SAVE IS S5'S ANSWER, whatever it named: the route asked again is the ordinary one, and
        // never "read first" a second time
        assertEquals(RenameRoute.Local, site(hasLocalRow = true, heard = false, mayHold = true, listRead = true))
        assertEquals(RenameRoute.Blocked(shutWords, Line.SHUT), site(name = "Main site", heard = false, mayHold = true, listRead = true))
        val w = listOf(Write(P, "Demo Site, Pune"))
        assertEquals(RenameRoute.S5(w, readFirst = false), site(s5 = w, hasLocalRow = true, heard = true, mayHold = true, listRead = true))
        // and once a read has landed (heard), an empty list of S5 projects IS "S5 does not hold it", as before
        assertEquals(RenameRoute.Local, site(hasLocalRow = true, heard = true, mayHold = true))
        assertEquals(RenameRoute.Blocked(shutWords, Line.SHUT), site(name = "Main site", heard = true, mayHold = true))
        // NOTHING S5 COULD HOLD -- made on this phone and never sent, the demo site: its routes do not wait for a read
        assertEquals(RenameRoute.Local, site(hasLocalRow = true, heard = false, mayHold = false))
        assertEquals(RenameRoute.Local, site(hasLocalRow = true, heard = false, mayHold = false, signedIn = false))
        assertEquals(RenameRoute.Blocked(shutWords, Line.SHUT), site(name = TourDemo.SITE, heard = false, mayHold = false))
        // a name S5 already gave for a project (an answer to a PUT, a read that landed between) is S5's answer: its route
        assertEquals(RenameRoute.S5(w, readFirst = false), site(s5 = w, heard = false, mayHold = true))
        // the bucket has no name to change, heard or not
        assertEquals(RenameRoute.Blocked(shutWords, Line.SHUT), site(name = "(no site)", hasLocalRow = true, heard = false, mayHold = true))
        // a client and a project are not S5's to name: neither fact moves them off their routes
        for (kind in listOf("client", "project"))
            assertEquals(RenameRoute.Local, Renames.route(en, kind, "Asha flat", "", true, emptyList(), true, false, false, false, heard = false, mayHold = true), kind)
        // the defaults are the heard phone: every caller that does not say otherwise routes as before the review
        assertEquals(site(hasLocalRow = true), Renames.route(en, "site", "Demo Site, Pune", "", true, emptyList(), true, false, false))
    }

    @Test fun `the chosen rooms follow the name S5 gives a site - from another phone, a 409 or 1 of 2 alike (R7 - the A2 review, 2)`() {
        fun p(project: String, site: String, client: String = "Asha") = Renames.Placed(project, client, site)
        fun step(from: String, to: String, leave: Boolean, client: String = "Asha") = Renames.ScopeStep(client, from, to, leave)
        // (a) another phone renamed the site and a list read hears it -- or (b) this dialog's own 409 gave the name as
        // stored: the project is listed under the new name, nothing is left under the old one, and the rooms go with it
        assertEquals(listOf(step("Old flat", "New flat", leave = true)),
            Renames.scopeSteps(listOf(p("PROJ-1", "Old flat")), listOf(p("PROJ-1", "New flat")), setOf("PROJ-1")))
        // (c) 1 of 2: the project that took the name is listed under it WITH the rooms; the other is still under the
        // old name, and that site keeps its rooms too
        assertEquals(listOf(step("Old flat", "New flat", leave = false)),
            Renames.scopeSteps(listOf(p("PROJ-1", "Old flat"), p("PROJ-2", "Old flat")), listOf(p("PROJ-1", "New flat"), p("PROJ-2", "Old flat")), setOf("PROJ-1")))
        // a row only this phone holds (no S5 id) still under the old name keeps that site's rooms the same way
        assertEquals(listOf(step("Old flat", "New flat", leave = false)),
            Renames.scopeSteps(listOf(p("PROJ-1", "Old flat"), p("", "old  FLAT")), listOf(p("PROJ-1", "New flat"), p("", "old  FLAT")), setOf("PROJ-1")))
        // both projects renamed by one write: one step, not two
        assertEquals(listOf(step("Old flat", "New flat", leave = true)),
            Renames.scopeSteps(listOf(p("PROJ-1", "Old flat"), p("PROJ-2", "Old flat")), listOf(p("PROJ-1", "New flat"), p("PROJ-2", "new_flat")), setOf("PROJ-1", "PROJ-2")))
        // two projects of one phone-site that S5 names differently: each new name gets the rooms
        assertEquals(listOf(step("Old flat", "North", leave = true), step("Old flat", "South", leave = true)),
            Renames.scopeSteps(listOf(p("PROJ-1", "Old flat"), p("PROJ-2", "Old flat")), listOf(p("PROJ-1", "North"), p("PROJ-2", "South")), setOf("PROJ-1", "PROJ-2")))
        // the first name S5 gives, where it is not the name the phone listed the project under (seeded by another phone)
        assertEquals(listOf(step("Flat 12", "Flat 12, Baner", leave = true)),
            Renames.scopeSteps(listOf(p("PROJ-1", "Flat 12")), listOf(p("PROJ-1", "Flat 12, Baner")), setOf("PROJ-1")))
        // "nothing left under the old name" is asked of THIS client's sites: another client's site of the same name is not it
        assertEquals(listOf(step("Old flat", "New flat", leave = true)),
            Renames.scopeSteps(listOf(p("PROJ-1", "Old flat"), p("PROJ-9", "Old flat", "Bhave")), listOf(p("PROJ-1", "New flat"), p("PROJ-9", "Old flat", "Bhave")), setOf("PROJ-1")))

        // NO STEP: the same name as the catalogue compares names (case, spacing, underscores) is the same key
        assertEquals(emptyList(), Renames.scopeSteps(listOf(p("PROJ-1", "Old flat")), listOf(p("PROJ-1", " old_FLAT ")), setOf("PROJ-1")))
        // the "(no site)" bucket is a bucket, not a site: nothing is taken from it, and nothing is given to it
        assertEquals(emptyList(), Renames.scopeSteps(listOf(p("PROJ-1", Renames.NO_SITE)), listOf(p("PROJ-1", "New flat")), setOf("PROJ-1")))
        assertEquals(emptyList(), Renames.scopeSteps(listOf(p("PROJ-1", "Old flat")), listOf(p("PROJ-1", Renames.NO_SITE)), setOf("PROJ-1")))
        // only the projects whose name THIS write changed: a listing that differs for another reason moves nothing
        assertEquals(emptyList(), Renames.scopeSteps(listOf(p("PROJ-1", "Old flat")), listOf(p("PROJ-1", "New flat")), setOf("PROJ-2")))
        assertEquals(emptyList(), Renames.scopeSteps(listOf(p("PROJ-1", "Old flat")), listOf(p("PROJ-1", "New flat")), emptySet()))
        // a row with no S5 id is never S5's to move; a project the tree no longer lists, or lists under another client, moves nothing
        assertEquals(emptyList(), Renames.scopeSteps(listOf(p("", "Old flat")), listOf(p("", "New flat")), setOf("")))
        assertEquals(emptyList(), Renames.scopeSteps(listOf(p("PROJ-1", "Old flat")), emptyList(), setOf("PROJ-1")))
        assertEquals(emptyList(), Renames.scopeSteps(listOf(p("PROJ-1", "Old flat")), listOf(p("PROJ-1", "New flat", "Bhave")), setOf("PROJ-1")))
    }

    @Test fun `a client and a project keep their two routes - with a refusal in place of a call that cannot succeed`() {
        for (kind in listOf("client", "project")) {
            fun r(serverId: String, local: Boolean, pair: Boolean, name: String = "Asha flat") =
                Renames.route(en, kind, name, serverId, local, listOf(Write(P, "x")), signedIn = true, erpLive = pair, unlisted = true)
            // the bench holds it and a pair is stored: rename_node by docname, as before port 62
            assertEquals(RenameRoute.Legacy("PROJ-0003"), r("PROJ-0003", local = false, pair = true), kind)
            assertEquals(RenameRoute.Legacy("PROJ-0003"), r(" PROJ-0003 ", local = true, pair = true), kind)
            assertEquals(Line.SERVER, Renames.line(r("PROJ-0003", local = false, pair = true)))
            // the bench holds it and there is NO live pair: the call cannot be made -- said, not attempted (R1, R2).
            // THIS PIN MOVED WITH PORT 63 SLICE F4b, and it is stricter than it was: the refusal used to be
            // Blocked("Could not rename: no server configured", Line.SERVER) -- drawn only after Save, the name editable,
            // and untrue of a phone that is signed in to S5. It is now the row's own reason, in the grey line's place,
            // from the moment the dialog opens (Amit, 23 Nov 2026: "verify if renaming a site, client works actually correct").
            val noPair = r("PROJ-0003", local = true, pair = false)
            assertEquals(RenameRoute.Blocked(
                if (kind == "client") "This client is the office's record. It is renamed at the office, not from this phone."
                else "This project is the office's record. It is renamed at the office, not from this phone.", Line.SHUT), noPair, kind)
            assertTrue(Renames.shut(noPair), "the name is shown, not edited, and Save is off")
            assertFalse(noPair.toString().contains("no server configured"))
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

    // ---- port 63 slice F4b: a rename that cannot work says so before Save, and never forks ----------------------

    private val officeClient = "This client is the office's record. It is renamed at the office, not from this phone."
    private val officeProject = "This project is the office's record. It is renamed at the office, not from this phone."
    private val s5Client = "This client cannot be renamed from this phone: a project of it is on S5, or on its way there, and the new name would not reach it."
    private val s5Project = "This project cannot be renamed from this phone: it is on S5, or on its way there, and the new name would not reach it."

    /** A client's or a project's route: no S5 writes and no R9 capture (both are a site's alone). */
    private fun row(kind: String, serverId: String = "", local: Boolean = false, live: Boolean = false, onS5: Boolean = false, name: String = "Asha Rao", signedIn: Boolean = true) =
        Renames.route(en, kind, name, serverId, local, emptyList(), signedIn, erpLive = live, unlisted = false, onS5 = onS5)

    @Test fun `the rename table of a phone with no live ERP pair, row by row - what works works, and what cannot is shut before Save`() {
        // SITE the phone lists under a name S5 holds: works, on S5 and the phone, exactly as before this slice
        val w = listOf(Write("PROJ-T1", "Flat 9"), Write("S5-aaaaaaaaaa", "Flat 9"))
        assertEquals(RenameRoute.S5(w, readFirst = false), site(name = "Flat 9", s5 = w))
        // SITE "(no site)": shut -- a bucket has no name to change
        assertEquals(RenameRoute.Blocked(shutWords, Line.SHUT), site(name = "(no site)", s5 = listOf(Write("PROJ-T2", ""))))
        // SITE only an old ERP bootstrap names: shut
        assertEquals(RenameRoute.Blocked(shutWords, Line.SHUT), site(name = "Flat 9 Tower", serverId = "SITE-T1"))
        // CLIENT that is an ERP customer (its id reaches the phone on a project S5 lists): it answered "Could not rename:
        // no server configured" after Save. Shut, as the office's record -- signed in to S5 or not, a phone row or not,
        // and whatever S5 holds of it: the docname decides
        for (signed in listOf(true, false)) for (local in listOf(true, false)) for (on in listOf(true, false))
            assertEquals(RenameRoute.Blocked(officeClient, Line.SHUT), row("client", serverId = "Asha Rao", local = local, onS5 = on, signedIn = signed), "signed=$signed local=$local onS5=$on")
        // CLIENT made on this phone whose project S5 holds: it said "Renamed" and forked into two clients. Shut, never Local
        val made = row("client", local = true, onS5 = true, name = "Bela Nair")
        assertEquals(RenameRoute.Blocked(s5Client, Line.SHUT), made)
        assertTrue(made !is RenameRoute.Local)
        // ANY PROJECT: an ERP project by its docname; a project under a phone-minted key; a phone row of a project S5 holds
        assertEquals(RenameRoute.Blocked(officeProject, Line.SHUT), row("project", serverId = "PROJ-T1", onS5 = true, name = "kitchen"))
        assertEquals(RenameRoute.Blocked(s5Project, Line.SHUT), row("project", serverId = "S5-aaaaaaaaaa", local = true, name = "balcony"))
        assertEquals(RenameRoute.Blocked(s5Project, Line.SHUT), row("project", local = true, onS5 = true, name = "balcony"))
        // every one of them is SHUT: the grey line's place holds the reason from the moment the dialog opens
        for (r in listOf(row("client", serverId = "Asha Rao"), made, row("project", serverId = "PROJ-T1"), row("project", serverId = "S5-aaaaaaaaaa"))) {
            assertTrue(Renames.shut(r)); assertEquals(Line.SHUT, Renames.line(r))
        }
        // (a room has no rename anywhere on the phone: there is no route to ask)
    }

    @Test fun `a phone-minted S5 key is never a bench docname - with a live pair, a dead one or none`() {
        assertEquals("S5-", Renames.MINTED)
        assertTrue(Renames.minted("S5-aaaaaaaaaa")); assertTrue(Renames.minted(" S5-0123456789 "))
        for (no in listOf("", "PROJ-T3", "SITE-T1", "s5-aaaaaaaaaa", "Asha Rao", "XS5-1")) assertFalse(Renames.minted(no), "'$no'")
        // it is :app's own grammar for the key it mints
        assertTrue(app("S5Session.kt").contains("fun isLocalKey(project: String) = project.startsWith(\"${Renames.MINTED}\")"), "S5Auth.isLocalKey is Renames.minted's prefix")
        assertTrue(app("S5Session.kt").contains("return \"${Renames.MINTED}\" + d.joinToString(\"\")"), "S5Auth.localKey mints under it")
        // with a LIVE pair it used to be Legacy("S5-…"): rename_node asked the bench for a Project it has never held
        for (live in listOf(true, false)) for (on in listOf(true, false)) for (local in listOf(true, false)) {
            val r = row("project", serverId = "S5-aaaaaaaaaa", local = local, live = live, onS5 = on)
            assertEquals(RenameRoute.Blocked(s5Project, Line.SHUT), r, "live=$live onS5=$on local=$local")
        }
        // a real docname with a live pair still goes to the bench, as before
        assertEquals(RenameRoute.Legacy("PROJ-T3"), row("project", serverId = "PROJ-T3", live = true))
        assertEquals(RenameRoute.Legacy("Asha Rao"), row("client", serverId = "Asha Rao", live = true, onS5 = true), "the bench rename of a live pair is not this slice's")
    }

    @Test fun `the Local route is only for a row nothing of which is on S5`() {
        for (kind in listOf("client", "project")) {
            // made on this phone, nothing sent: renamed here
            assertEquals(RenameRoute.Local, row(kind, local = true), kind)
            assertEquals(RenameRoute.Local, row(kind, local = true, live = true), kind)
            assertEquals(RenameRoute.Local, row(kind, local = true, signedIn = false), kind)
            // the same row once S5 holds something of it: shut, whatever else is true
            for (live in listOf(true, false)) for (signed in listOf(true, false)) {
                val r = row(kind, local = true, live = live, onS5 = true, signedIn = signed)
                assertEquals(RenameRoute.Blocked(if (kind == "client") s5Client else s5Project, Line.SHUT), r, "$kind live=$live signed=$signed")
            }
            // no row here, no docname, nothing on S5 (the demo's): nothing a rename could change, as before
            assertEquals(RenameRoute.Blocked("Could not rename: Asha Rao", Line.PHONE), row(kind), kind)
            // no row here, no docname, but S5 holds it -- the "(no client)" bucket of projects S5 lists with no customer: shut
            assertEquals(Line.SHUT, assertIs<RenameRoute.Blocked>(row(kind, onS5 = true)).line, kind)
        }
        // a site never reads onS5: it has its own route to S5
        assertEquals(RenameRoute.Local, Renames.route(en, "site", "Flat 9", "", true, emptyList(), true, false, false, onS5 = true))
        // the default is "nothing on S5": a caller that does not say otherwise routes as before the slice
        assertEquals(row("client", local = true), Renames.route(en, "client", "Asha Rao", "", true, emptyList(), true, false, false))
    }

    @Test fun `whether S5 holds something of a row - an id S5 lists, a phone-minted key a project or a capture carries, a phone row's key only when S5 lists it`() {
        val none: (String) -> Boolean = { false }
        fun lists(vararg ids: String): (String) -> Boolean = { it in ids }
        assertFalse(Renames.onS5(emptyList(), emptyList(), none))
        assertFalse(Renames.onS5(listOf("", " ", ""), listOf(""), none), "a row only this phone holds carries no id")
        // CARRIED: an ERP id counts when S5 lists it
        assertTrue(Renames.onS5(listOf("PROJ-T1"), emptyList(), lists("PROJ-T1")))
        assertFalse(Renames.onS5(listOf("PROJ-T1"), emptyList(), lists("PROJ-T2")), "an ERP project S5 does not list is not on S5")
        // CARRIED: a phone-minted key counts by itself -- the tree's project, or a capture keyed for S5 that the list has not named yet
        assertTrue(Renames.onS5(listOf("", "S5-aaaaaaaaaa"), emptyList(), none))
        // DERIVED: what a phone row WOULD be called on S5 always looks minted, so it counts only when S5 lists it
        assertFalse(Renames.onS5(listOf(""), listOf("S5-bbbbbbbbbb"), none), "typed on the phone and never sent")
        assertTrue(Renames.onS5(listOf(""), listOf("S5-bbbbbbbbbb"), lists("S5-bbbbbbbbbb")), "the project made here that S5 holds, seen while signed out")
        assertFalse(Renames.onS5(emptyList(), listOf(""), { true }))
    }

    @Test fun `a local rename takes the row's captures with it - the ones no server holds and that carry no project id (R10)`() {
        fun c(project: String = "", client: String = "Bela Nair", title: String = "study", sent: Boolean = false) = TreeKeep.Cap(project, client, title, sent)
        // a CLIENT rename: every capture of that client, whatever its title, names as the catalogue compares them
        val client = Renames.rekey("client", "Bela Nair", "", " Bela N ")
        assertEquals(c(client = "Bela N"), client(c()))
        assertEquals(c(client = "Bela N", title = "loft"), client(c(client = "bela_nair", title = "loft")))
        assertEquals(c(client = "Asha Rao"), client(c(client = "Asha Rao")), "another client's capture is not touched")
        // a PROJECT rename: the captures of that client AND title
        val project = Renames.rekey("project", "Bela Nair", "study", "Study wall")
        assertEquals(c(title = "Study wall"), project(c()))
        assertEquals(c(client = "BELA  NAIR", title = "Study wall"), project(c(client = "BELA  NAIR", title = " Study ")), "the client's own spelling stays: only the title is renamed")
        assertEquals(c(title = "loft"), project(c(title = "loft")), "the same client's other project")
        assertEquals(c(client = "Asha Rao"), project(c(client = "Asha Rao")), "another client's project of the same title")
        // LEFT EXACTLY AS IT IS: a capture a server holds, and a capture that carries a project id (it is that project's)
        for (re in listOf(client, project)) {
            assertEquals(c(sent = true), re(c(sent = true)))
            assertEquals(c(project = "S5-aaaaaaaaaa"), re(c(project = "S5-aaaaaaaaaa")))
            assertEquals(c(project = "PROJ-T3"), re(c(project = "PROJ-T3")))
        }
        // a SITE rename changes no capture (a capture carries no site), nor does an unknown kind or a blank name
        for (kind in listOf("site", "room", "")) assertEquals(c(), Renames.rekey(kind, "Bela Nair", "study", "X")(c()), kind)
        for (blank in listOf("", "   ")) { assertEquals(c(), Renames.rekey("client", "Bela Nair", "", blank)(c())); assertEquals(c(), Renames.rekey("project", "Bela Nair", "study", blank)(c())) }
        // a change of case or spacing alone is still a write of the new spelling, on the same capture
        assertEquals(c(client = "Bela Nair"), Renames.rekey("client", "bela nair", "", "Bela Nair")(c(client = "bela nair")))
    }

    @Test fun `the four new reasons are whole keys per kind, and wait for their Marathi in English`() {
        assertEquals(officeClient, Renames.officeRecord(en, "client")); assertEquals(officeProject, Renames.officeRecord(en, "project"))
        assertEquals(s5Client, Renames.heldByS5(en, "client")); assertEquals(s5Project, Renames.heldByS5(en, "project"))
        // English first (Amit, 16 Nov 2026): no Marathi is composed yet, so under मराठी each reads its English (mr-pending.tsv)
        assertEquals(officeClient, Renames.officeRecord(Tx.MR, "client")); assertEquals(s5Project, Renames.heldByS5(Tx.MR, "project"))
        // each is read in a glance, and none blames a server that is not configured
        for (t in listOf(officeClient, officeProject, s5Client, s5Project)) {
            assertTrue(t.length <= 140, "too long to read on site: ${t.length}")
            assertFalse(t.contains("server") || t.contains("configured"), t)
        }
    }

    @Test fun `the phone's dialog routes on a LIVE pair and on what S5 holds, and its local rename carries the captures`() {
        val main = app("MainActivity.kt")
        // nothing in :app says "a pair is stored" to the rule any more
        for (f in listOf("MainActivity.kt", "TreeShell.kt", "Catalogue.kt", "S5Session.kt", "SyncWorker.kt"))
            assertFalse(app(f).contains("legacyPair"), "$f still names the old argument")
        val route = between(main, "fun renameRoute(t: RenameTarget, listRead: Boolean = false): RenameRoute {", "fun renameHere(")
        assertTrue(route.contains("erpLive = S5Masters.erpLive(context),"), "a live pair: stored and not refused")
        assertFalse(route.contains("FrappeClient.load(context) != null"), "never 'a pair is stored'")
        assertTrue(route.contains("onS5 = !isSite && cat.onS5(masters, queue, t.kind, t.client, t.site, t.project))"), "what S5 holds of a client or a project")
        // the dialog OPENS on that route: the reason is drawn before any Save
        assertTrue(main.contains("val opened = remember(t, masters, queue, dataTick, renameIds, renameWhyTick, tx) { renameRoute(t) }"))
        assertTrue(main.contains("shut = if (opened is RenameRoute.Blocked && Renames.shut(opened)) opened.reason else \"\","))
        // and SHUT is drawn for any kind: the name not editable, Save off, the reason in the grey line's place
        val dialog = between(app("TreeShell.kt"), "fun RenameDialog(", "internal const val MM_PER_IN")
        assertTrue(dialog.contains("val isShut = line == RenameRoute.Line.SHUT") && dialog.contains("enabled = !busy && !isShut,") &&
            dialog.contains("RenameRoute.Line.SHUT -> shut") && dialog.contains("TextButton(enabled = changed && !busy && !isShut,"))
        // what S5 holds of a row is gathered in ONE place and judged by :pano
        val cat = app("Catalogue.kt")
        // THESE FIVE PINS MOVED WITH THE REVIEW OF F4, and hold no less. The gathering is still ONE place, now named
        // s5Ties because a delete needs the keys themselves and not only "there is one": onS5 is that gathering's
        // `keys.isNotEmpty()`, the rule asked is Renames.heldKeys (Renames.onS5 IS heldKeys(...).isNotEmpty(), pinned
        // below), and a phone row's key is the one the phone REMEMBERS for it (keyOfRow) instead of a hash of its names.
        val on = between(cat, "fun onS5(masters: JSONObject?, captures: List<CaptureStore.Capture>, kind: String,", "fun hasUnlistedCapture(")
        assertTrue(on.contains("s5Ties(masters, captures, kind, client, site, project).keys.isNotEmpty()"), "onS5 is 'a key the gathering found'")
        assertTrue(on.contains("val under = projects(masters).filter { mine(it.client, it.site, it.title) }") && on.contains("val carried = under.map { it.serverId } +"), "the tree's projects of the row")
        assertTrue(on.contains("!TourDemo.isDemoProject(c.customerName, c.projectTitle)") && on.contains("mine(c.customerName, localSiteFor(c.customerName, c.projectTitle), c.projectTitle)") && on.contains("}.map { it.project }"), "its captures, never the demo's")
        assertTrue(on.contains(".map { keyOfRow(book, it.client, it.site, it.project) }"), "its phone rows' keys, as remembered")
        assertTrue(on.contains("Renames.heldKeys(carried, derived) { id -> held.containsKey(id) }") && on.contains("val held = S5Sites.all(appContext)"), "judged by :pano on what S5's list has named")
        assertEquals(1, Regex("Renames\\.heldKeys\\(").findAll(cat + main).count(), "one caller of the rule")
        assertEquals(0, Regex("Renames\\.onS5\\(").findAll(cat + main).count(), "and no second gathering beside it")
        // THE LOCAL RENAME CARRIES THE CAPTURES, in the same step, before the tree is re-read
        val here = between(main, "fun renameHere(t: RenameTarget, newName: String) {", "renaming?.let { t ->")
        val renamed = here.indexOf("if (!cat.renameLocal(t.kind, t.client, t.site, t.project, newName))")
        val rekey = here.indexOf("val rekey = Renames.rekey(t.kind, t.client, t.project, newName)")
        val write = here.indexOf("if (now != was) store.setNames(c.deviceId, now.client, now.title)")
        val reload = here.indexOf("reload()")
        assertTrue(renamed in 0 until rekey && rekey < write && write < reload, "renamed, re-keyed, written, and only then re-read")
        assertTrue(between(here, "if (t.kind != \"site\") {", "WishSync.renameLocal(").contains("for (c in store.all()) {"), "every capture is asked; a site rename changes none")
        assertTrue(here.contains("val was = TreeKeep.Cap(c.project, c.customerName, c.projectTitle, sent = c.state == \"SYNCED\")") && here.contains("val now = rekey(was)"), "which captures is :pano's answer")
        assertEquals(1, Regex("store\\.setNames\\(").findAll(main).count(), "one writer of a capture's names")
        // the store's write: the two name columns of one row, and nothing else
        val set = between(app("CaptureStore.kt"), "fun setNames(deviceId: String, customerName: String, projectTitle: String) {", "fun setState(")
        assertTrue(set.contains("put(\"customer_name\", customerName)") && set.contains("put(\"project_title\", projectTitle)") && set.contains("\"device_id = ?\", arrayOf(deviceId))"))
        assertEquals(2, Regex("put\\(").findAll(set).count(), "the project id, the state and the files are not touched")
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

    @Test fun `the keys a refusal rests on are named, each once - and onS5 is 'there is one' (the review of F4)`() {
        val listed: (String) -> Boolean = { it == "PROJ-T1" || it == "S5-bbbbbbbbbb" }
        // carried: what S5's list has named, and every phone-minted key; derived: only what S5's list has named
        assertEquals(listOf("PROJ-T1", "S5-aaaaaaaaaa", "S5-bbbbbbbbbb"),
            Renames.heldKeys(listOf("", "PROJ-T1", "PROJ-T9", "S5-aaaaaaaaaa", "PROJ-T1"), listOf("S5-cccccccccc", "S5-bbbbbbbbbb", " "), listed))
        assertEquals(emptyList(), Renames.heldKeys(listOf("", "PROJ-T9"), listOf("S5-cccccccccc"), listed))
        // onS5 is exactly "a key was found", for every combination
        for (carried in listOf(emptyList(), listOf(""), listOf("PROJ-T1"), listOf("PROJ-T9"), listOf("S5-aaaaaaaaaa")))
            for (derived in listOf(emptyList(), listOf("S5-bbbbbbbbbb"), listOf("S5-cccccccccc")))
                assertEquals(Renames.heldKeys(carried, derived, listed).isNotEmpty(), Renames.onS5(carried, derived, listed), "$carried $derived")
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
        // (the pin moved with port 63 slice F4a: the read now says whether the pair is live -- TreeKeepTest holds what it does with that)
        assertTrue(s5.contains("if (route.readFirst)") && s5.contains("S5Masters.fetch(context, erpLive = S5Masters.erpLive(context))") && s5.contains("renameRoute(t, listRead = true)"), "S5's list first, then the route again")
        // the one step, only once S5 has answered for every project: the phone's rows, the room-scope key, the masters' rows
        val done = between(s5, "if (outcome is Renames.Outcome.Done)", "} else {")
        for (step in listOf("cat.renameLocal(\"site\", t.client, t.site, \"\", name)", "cat.moveRoomScope(t.client, t.site, name)", "cat.renameSiteRows(m, t.client, t.site, name)"))
            assertTrue(done.contains(step), "the success step holds `$step`")
        assertTrue(done.contains("val name = outcome.site"), "the name applied is S5's own answer")
        // R2: no failure of a rename goes to the banner any more
        assertFalse(main.contains("app_msg_could_not_rename_x"), "the could-not-rename banner is gone from MainActivity: the dialog says it")
        // the local rename is ONE function, the Local route's Save and the S5 route's alike (below): the Local branch is that call
        val here = between(main, "fun renameHere(t: RenameTarget, newName: String) {", "renaming?.let { t ->")
        assertTrue(between(save, "RenameRoute.Local ->", "is RenameRoute.Legacy ->").contains("RenameRoute.Local -> renameHere(t, newName)"), "the Local route renames through renameHere")
        for ((branch, src) in listOf("Local" to here, "Legacy" to legacy, "S5" to s5, "Blocked" to save.substring(save.indexOf("is RenameRoute.Blocked ->"))))
            assertTrue(src.contains("renameFailed {"), "the $branch route says its failure in the dialog")
        assertEquals(3, Regex("renameFailed \\{").findAll(s5).count(), "the S5 route: no list read, still blocked after it, and S5's own answer")
        assertFalse(save.contains("lastResult = { tx -> Renames") || save.contains("lastResult = { _ ->"), "the banner carries a rename's success only")
        assertFalse(here.contains("lastResult = { tx -> Renames") || here.contains("lastResult = { _ ->"), "nor does the local rename's")
        // R3: the local rename's Boolean is read
        assertTrue(Regex("if \\(!cat\\.renameLocal\\(t\\.kind, t\\.client, t\\.site, t\\.project, newName\\)\\)").containsMatchIn(here), "a local rename that renamed nothing is a failure")
        assertEquals(1, Regex("cat\\.renameLocal\\(t\\.kind,").findAll(main).count(), "and there is one local rename, not a copy of it")
        for (step in listOf("cat.moveRoomScope(t.client, t.site, newName)", "cat.renameSiteRows(m, t.client, t.site, newName)"))
            assertTrue(here.contains(step), "a local site rename holds `$step`")
    }

    @Test fun `S5's list not heard yet - the phone records a landed read, reads the list at start with a pair stored, and the dialog routes on it (the A2 review, 1)`() {
        // a landed read of the list is recorded, in a file of its own, by the list reads and by nothing else
        val sites = app("S5Sites.kt")
        val heard = between(sites, "fun heard(context: Context): Boolean", "fun take(")
        val take = between(sites, "fun takeRead(context: Context", "fun takeList(context: Context")
        assertTrue(heard.contains("getSharedPreferences(READS, Context.MODE_PRIVATE).getBoolean(HEARD, false)"), "heard reads the record")
        assertTrue(take.contains("takeList(context, rows)") && take.contains("putBoolean(HEARD, true)"), "a read's rows are taken and the read recorded")
        assertEquals(1, Regex("putBoolean\\(HEARD").findAll(sites).count(), "one writer of the record")
        assertFalse(between(sites, "fun takeList(context: Context", "\n}").contains("HEARD"), "an answer to a PUT is not a read of the list")
        val rename = app("SiteRename.kt")
        assertFalse(between(rename, "private fun put(", "fun hear(context: Context)").contains("takeRead("), "the PUTs' answers are taken, not recorded as a read")
        assertFalse(between(rename, "fun seed(context: Context)", "\n    }\n").contains("takeRead("), "nor the seed's")
        // the three reads of S5's list: the S5 leg's, the one both legs make, and the one made at start with a pair stored
        // (fetch's signature moved with port 63 slice F4a -- it is told whether the pair is live; what it feeds is unchanged)
        assertTrue(between(app("S5Session.kt"), "fun fetch(context: Context, erpLive: Boolean)", "fun forget(context: Context)").contains("S5Sites.takeRead(context, "), "S5Masters.fetch")
        assertTrue(between(app("QuoteStageSync.kt"), "fun refresh(context: Context", "// ---- Record the approved scope").contains("S5Sites.takeRead(context, "), "QuoteStageSync.refresh")
        val hear = between(rename, "fun hear(context: Context): Boolean", "fun seed(context: Context)")
        assertTrue(hear.contains("\"/api/asis\"") && hear.contains("o.getJSONArray(\"projects\")") && hear.contains("S5Sites.takeRead(context, "), "SiteRename.hear reads the list for its names")
        assertFalse(hear.contains("saveMasters") || hear.contains("S5Masters"), "and leaves the masters the bootstrap's")
        // app start: with a pair stored the S5 leg's read is not made, so the list is read there too
        // THESE TWO PINS MOVED WITH PORT 63 SLICE F4a, and what they hold did not get weaker: "a pair is stored" became
        // "a pair is LIVE" (stored and not refused, or its bootstrap landed just now), because a phone holding a dead key
        // took neither the S5 leg's read at start nor a bootstrap, and drew the ERP of the day the key died. Every phone
        // still makes exactly one of the two reads; TreeKeepTest pins how `erpLive` is found.
        val main = app("MainActivity.kt")
        val start = between(main, "LaunchedEffect(configured) {", "// Back from the camera")
        assertTrue(start.contains("if (erpLive && S5Auth.current(context) != null) SiteRename.hear(context)"), "a live pair: S5's list is read at start")
        assertTrue(Regex("""if \(!erpLive && S5Session\.signedIn\(context\)\)\s*S5Masters\.fetch\(context, erpLive = false\)\?\.let \{ store\.saveMasters\(it\) \}""").containsMatchIn(start), "no live pair: the S5 leg's read")
        assertTrue(start.contains("reload()"), "and the tree is redrawn from what was heard")
        // the dialog: both facts go to Renames.route, the second asked only while the first is false
        val route = between(main, "fun renameRoute(t: RenameTarget, listRead: Boolean = false): RenameRoute {", "fun renameHere(")
        assertTrue(route.contains("val heard = !isSite || S5Sites.heard(context)") && route.contains("heard = heard,"), "whether a read has landed")
        assertTrue(route.contains("mayHold = !heard && cat.mayBeOnS5(masters, queue, t.client, t.site)"), "and whether S5 may hold the site")
        val may = between(app("Catalogue.kt"), "fun mayBeOnS5(", "fun hasUnlistedCapture(")
        assertTrue(may.contains("projectsOf(masters, client, site).any { it.serverId.isNotBlank() }"), "a project of it carries a server id")
        assertTrue(may.contains("c.project.isNotBlank() && !TourDemo.isDemoProject(c.customerName, c.projectTitle)") && may.contains("same(localSiteFor(c.customerName, c.projectTitle), site)"), "or a capture of it a project id -- never the demo's")
        // the Save: the route asked again after the read may be the phone's own row, and is then renamed as one --
        // before the review anything but an S5 route there was said as "offline"
        val save = between(main, "onSave = { newName ->", "blocker = deleteBlocker")
        val s5 = between(save, "is RenameRoute.S5 ->", "is RenameRoute.Blocked ->")
        val again = s5.indexOf("go = renameRoute(t, listRead = true)"); val local = s5.indexOf("if (go == RenameRoute.Local) {"); val sent = s5.indexOf("SiteRename.rename(")
        assertTrue(again in 0 until local && local < sent, "Local is taken after the route is asked again and before anything is sent")
        assertTrue(between(s5, "if (go == RenameRoute.Local) {", "val s5 = go as? RenameRoute.S5").contains("renameHere(t, newName)"), "through the one local rename")
    }

    @Test fun `the chosen rooms follow S5's name where it is written - S5Sites takeList, by Renames scopeSteps (R7 - the A2 review, 2)`() {
        val sites = app("S5Sites.kt")
        val take = between(sites, "fun takeList(context: Context", "\n}")
        // which projects' names this write changes, read before the write
        assertTrue(take.contains("if (stored?.site.orEmpty() != copy.site) moved.add(project)"), "a name that changes, not a first hearing of no name")
        // the tree before, the write, the tree after -- in that order -- and :pano's rule between them
        val before = take.indexOf("cat.placed(masters) }.getOrNull()"); val write = take.indexOf("ed.apply()")
        val after = take.indexOf("cat.followS5Names(Renames.scopeSteps(before, cat.placed(masters), moved))")
        assertTrue(before in 0 until write && write < after, "the tree is read before the names are written and again after")
        assertEquals(1, Regex("ed\\.apply\\(\\)").findAll(take).count(), "S5's names are written once, whatever the rooms' step does")
        // every writer of S5's names goes through it: the single answer and a read's rows
        assertTrue(sites.contains("fun take(context: Context, project: String, copy: Renames.Held): Boolean = takeList(context, listOf(project to copy)) > 0"), "an answer to a PUT")
        assertTrue(between(sites, "fun takeRead(context: Context", "fun takeList(context: Context").contains("takeList(context, rows)"), "a list read")
        assertEquals(1, Regex("putString\\(project, encode\\(copy\\)\\)").findAll(sites).count(), "one place writes a name")
        // the catalogue's half: every new name is given the rooms first, then an old name nothing is listed under loses its own
        val cat = app("Catalogue.kt")
        assertTrue(cat.contains("projects(masters).map { Renames.Placed(it.serverId, it.client, it.site) }"), "the tree as scopeSteps reads it")
        val follow = between(cat, "fun followS5Names(steps: List<Renames.ScopeStep>) {", "\n    }\n")
        val give = follow.indexOf("for (s in steps) moveRoomScope(s.client, s.from, s.to, leave = false)"); val drop = follow.indexOf("for (s in steps) if (s.leave) moveRoomScope(s.client, s.from, s.to)")
        assertTrue(give in 0 until drop, "given first, dropped after")
        val move = between(cat, "fun moveRoomScope(client: String, from: String, to: String, leave: Boolean = true) {", "\n    }\n")
        assertTrue(move.contains("if (rooms.isNotEmpty() && there.isEmpty())") && move.contains("if (leave) ed.remove(scopeKey(client, from))"), "a site with its own list keeps it; the old list goes only when nothing is left there")
        // the dialog's own success step still moves it for what S5's names do not cover: the phone's own rows re-keyed there
        assertTrue(app("MainActivity.kt").contains("cat.moveRoomScope(t.client, t.site, name)"), "the Done step's move stays")
    }

    @Test fun `S5's names have one home on the phone, fed by every list read and every answer - and the tree reads it`() {
        val sites = app("S5Sites.kt")
        assertTrue(sites.contains("Renames.takes(") && sites.contains("Renames.Held("), "kept by S5's own stamp")
        // the two list reads feed it, by :pano's reader
        val session = app("S5Session.kt")
        assertTrue(between(session, "fun fetch(context: Context, erpLive: Boolean)", "fun forget(context: Context)").contains("S5Sites.takeRead(context, "), "S5Masters.fetch feeds the names")
        assertFalse(session.contains("m.put(\"site_name\""), "an S5 entry is no longer stamped with the local row's name (R4)")
        assertTrue(between(app("QuoteStageSync.kt"), "fun refresh(context: Context", "// ---- Record the approved scope").contains("S5Sites.takeRead(context, "), "the list read both legs make feeds them too")
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
        // (THIS PIN MOVED WITH THE REVIEW OF F4: the second half's key is the one the phone REMEMBERS for the row -- Catalogue.keyOfRow,
        // which reads S5Keys and mints nothing -- where it used to be worked out from the row's names, which a move or a
        // site rename changes. It holds no less: both halves are still there, and KeyBookTest pins what keyOfRow reads.)
        assertTrue(mine.contains("S5Sites.held(appContext, it) != null") && mine.contains(".map { keyOfRow(book, it.client, it.site, it.project) }"), "both halves")
        assertTrue(mine.contains("it.site.isBlank() || same(it.site, site)"), "a minted project S5 names as another site is not this site's")
        // R9's capture: under a phone-minted key the MASTERS do not list (the key is re-derived from them), of this client and site
        val r9 = between(cat, "fun hasUnlistedCapture(", "fun renameSiteRows(")
        assertTrue(r9.contains("S5Auth.isLocalKey(c.project) && c.project !in listed") && r9.contains("same(localSiteFor(c.customerName, c.projectTitle), site)"), "the unlisted capture is told as the key was minted")
    }

    @Test fun `the seed and the record's site - sent after the S5 list read, never for the demo, the record's through AsIsPath`() {
        val sync = app("SyncWorker.kt")
        val leg = between(sync, "private fun s5Leg(store: CaptureStore)", "private data class ErpOutcome")
        val read = leg.indexOf("S5Masters.fetch(ctx, erpLive = false)"); val seed = leg.indexOf("SiteRename.seed(ctx)")
        assertTrue(read >= 0 && seed > read, "the seed follows the list read in the S5 leg")
        val rename = app("SiteRename.kt")
        assertTrue(between(rename, "fun seed(context: Context)", "\n    }\n").contains("Renames.seed(p.serverId, p.client, p.title, S5Sites.held(context, p.serverId), p.site)"), "which projects are seeded is :pano's rule")
        // the upload's record carries the site as the tree names it
        val up = app("AsIsUpload.kt")
        assertTrue(between(up, "val body = AsIsPath.body(", "val recordUrl").contains("site = Catalogue(ctx).siteFor(CaptureStore(ctx).masters(), c.project, c.customerName, c.projectTitle),"), "the record's site is the tree's name for the capture's project")
    }
}
