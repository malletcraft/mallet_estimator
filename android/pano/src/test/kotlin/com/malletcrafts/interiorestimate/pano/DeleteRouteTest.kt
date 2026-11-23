package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeleteRouteTest {

    @Test
    fun `a row with a docname goes to the server`() {
        assertEquals(DeleteRoute.Server("MEST-SITE-0002"),
            Deletes.route(Tx.EN, "site", "MEST-SITE-0002", serverKnown = true))
        assertEquals(DeleteRoute.Server("PROJ-0003"),
            Deletes.route(Tx.EN, "project", "PROJ-0003", serverKnown = true))
    }

    @Test
    fun `a row this phone invented is a local delete`() {
        assertEquals(DeleteRoute.LocalOnly,
            Deletes.route(Tx.EN, "site", "", serverKnown = false))
        assertEquals(DeleteRoute.LocalOnly,
            Deletes.route(Tx.EN, "project", "  ", serverKnown = false))
    }

    @Test
    fun `the incident case is refused, not deleted locally`() {
        // 2026-08-24: a site the bench knew about arrived with a blank id
        // because the tree inferred the docname from projects that were
        // filtered out. The app removed it locally and said so in words that
        // read as success; the record was still there three days later.
        //
        // This is the assertion that makes that outcome impossible. Not
        // "the id is now correct" — that is a different fix, and fixing a
        // cause does not remove a failure mode — but "a blank id on a row
        // the office knows can never reach the local branch".
        val r = Deletes.route(Tx.EN, "site", "", serverKnown = true)
        assertTrue(r is DeleteRoute.Blocked, "a known row must never delete locally")
        assertTrue(r.reason.contains("refresh"),
            "the refusal has to say what to do about it: ${r.reason}")
        assertTrue(r.reason.contains("this phone only"),
            "and the consequence, which survived the shortening: ${r.reason}")
        // Shortened on Amit's instruction. The cap is the guard: a sentence
        // read standing in a dusty flat gets skimmed, and a skimmed refusal
        // gets tapped past.
        assertTrue(r.reason.length <= 110, "too long to read on site: ${r.reason.length}")
    }

    @Test
    fun `a client is never deleted from a phone whatever its id says`() {
        // Both directions, because the danger is a future screen that offers
        // the action on a row that happens to carry a docname.
        for (known in listOf(true, false)) {
            for (id in listOf("", "CUST-0001", "ZZ Probe Client")) {
                val r = Deletes.route(Tx.EN, "client", id, serverKnown = known)
                assertTrue(r is DeleteRoute.Blocked,
                    "client id='$id' known=$known routed to $r")
            }
        }
    }

    @Test
    fun `whitespace is not a docname`() {
        // A JSON field that came back as " " is absent, not present. Treating
        // it as an id would send delete_node a blank name, which deletes
        // nothing and reports success — the same failure in a new costume.
        assertEquals(DeleteRoute.LocalOnly,
            Deletes.route(Tx.EN, "site", "\t\n ", serverKnown = false))
        assertTrue(Deletes.route(Tx.EN, "site", " ", serverKnown = true) is DeleteRoute.Blocked)
    }

    @Test
    fun `the local-only sentence says the office was never involved`() {
        val msg = Deletes.localOnlyMessage(Tx.EN, "This site")
        assertTrue(msg.contains("this phone"), msg)
        assertTrue(msg.contains("never reached"), msg)
        // "Removed from this phone" full stop was read as "deleted
        // everywhere". The sentence has to close that reading itself,
        // because nobody reads a toast twice — and it has to do it in one
        // line, which is why the clause is joined by a dash rather than
        // being a second sentence.
        assertTrue(msg.contains("office system"), msg)
        assertTrue(msg.length <= 90, "a toast nobody finishes: ${msg.length}")
    }

    @Test
    fun `each branch says the page's Marathi under Tx MR`() {
        // port 61c slice M2: whole keys per kind — a gendered हा/ही cannot share one template
        val cant = Deletes.route(Tx.MR, "project", "", serverKnown = true) as DeleteRoute.Blocked
        assertEquals("ऑफिस सिस्टमवर हा प्रोजेक्ट ओळखता येत नाही. रिफ्रेशसाठी खाली ओढा — आता हटवल्यास तो फक्त या फोनवरून जाईल.", cant.reason)
        val cantSite = Deletes.route(Tx.MR, "site", "", serverKnown = true) as DeleteRoute.Blocked
        assertEquals("ऑफिस सिस्टमवर ही साइट ओळखता येत नाही. रिफ्रेशसाठी खाली ओढा — आता हटवल्यास ती फक्त या फोनवरून जाईल.", cantSite.reason)
        val client = Deletes.route(Tx.MR, "client", "", serverKnown = false) as DeleteRoute.Blocked
        assertEquals("ग्राहक हा ऑफिसचा कस्टमर रेकॉर्ड आहे, त्यामुळे तो फोनवरून काढता येत नाही. त्याखालच्या साइट्स इथे हटवा; ग्राहक हटवणे हे ऑफिसचे काम आहे.", client.reason)
        assertEquals("या फोनवरून काढले — मुख्य साइट कधीच ऑफिस सिस्टमवर पोहोचले नव्हते.",
            Deletes.localOnlyMessage(Tx.MR, "Main site"))
    }

    // ---- port 63 slice F4b: the third fact -- S5 holds something of the row --------------------------------------

    private val siteOnS5 = "This site has a project on S5, or on its way there. This phone cannot delete it there, so nothing was removed."
    private val projectOnS5 = "This project is on S5, or on its way there. This phone cannot delete it there, so nothing was removed."

    @Test
    fun `a site S5 holds is refused - never removed from this phone as though it had never left it`() {
        // 23 Nov 2026, found by reading and reproduced on made-up names: a site made on a phone and uploaded through S5
        // has no bench docname and was never in a bootstrap, so it was "a row this phone invented". Deleting it removed
        // the phone's row, said "Removed from this phone — … had never reached the office system", and the site stayed,
        // listed by S5. The August sentence, by a new road.
        val r = Deletes.route(Tx.EN, "site", "", serverKnown = false, onS5 = true)
        assertEquals(DeleteRoute.Blocked(siteOnS5), r)
        assertTrue(r !is DeleteRoute.LocalOnly)
        // the same row with nothing on S5 is still this phone's own, and still a local delete
        assertEquals(DeleteRoute.LocalOnly, Deletes.route(Tx.EN, "site", "", serverKnown = false, onS5 = false))
        // a project the same way
        assertEquals(DeleteRoute.Blocked(projectOnS5), Deletes.route(Tx.EN, "project", "", serverKnown = false, onS5 = true))
        assertEquals(DeleteRoute.LocalOnly, Deletes.route(Tx.EN, "project", "", serverKnown = false, onS5 = false))
    }

    @Test
    fun `a phone-minted S5 key is never sent to the bench as a docname`() {
        // "S5-…" is an id, so it used to be Server("S5-…"): delete_node asked the bench for a Project it has never held.
        for (known in listOf(true, false)) for (on in listOf(true, false)) {
            val r = Deletes.route(Tx.EN, "project", "S5-aaaaaaaaaa", serverKnown = known, onS5 = on)
            assertEquals(DeleteRoute.Blocked(projectOnS5), r, "known=$known onS5=$on")
        }
        assertEquals(DeleteRoute.Blocked(projectOnS5), Deletes.route(Tx.EN, "project", " S5-0123456789 ", serverKnown = false))
    }

    @Test
    fun `a row with a bench docname that S5 also holds is refused - a bench delete would leave it listed by S5`() {
        assertEquals(DeleteRoute.Blocked(projectOnS5), Deletes.route(Tx.EN, "project", "PROJ-T3", serverKnown = true, onS5 = true))
        // and for a site it is also the guard against a cascade aimed at the WHOLE bench site from a row S5 named differently
        assertEquals(DeleteRoute.Blocked(siteOnS5), Deletes.route(Tx.EN, "site", "SITE-T2", serverKnown = false, onS5 = true))
        assertEquals(DeleteRoute.Blocked(siteOnS5), Deletes.route(Tx.EN, "site", "", serverKnown = true, onS5 = true), "S5's answer before 'cannot identify'")
        // nothing on S5: the three routes exactly as they were
        assertEquals(DeleteRoute.Server("PROJ-T3"), Deletes.route(Tx.EN, "project", "PROJ-T3", serverKnown = true, onS5 = false))
        assertEquals(DeleteRoute.Server("SITE-T2"), Deletes.route(Tx.EN, "site", "SITE-T2", serverKnown = true, onS5 = false))
        assertTrue(Deletes.route(Tx.EN, "site", "", serverKnown = true, onS5 = false) is DeleteRoute.Blocked)
        // the default is "nothing on S5": a caller that does not say routes as before the slice
        for (kind in listOf("site", "project")) for (id in listOf("", "PROJ-T3")) for (known in listOf(true, false))
            assertEquals(Deletes.route(Tx.EN, kind, id, known, onS5 = false), Deletes.route(Tx.EN, kind, id, known), "$kind '$id' known=$known")
    }

    @Test
    fun `a client keeps its own refusal whatever S5 holds`() {
        val office = Deletes.route(Tx.EN, "client", "", serverKnown = false)
        for (on in listOf(true, false)) for (id in listOf("", "CUST-T1", "S5-aaaaaaaaaa"))
            assertEquals(office, Deletes.route(Tx.EN, "client", id, serverKnown = false, onS5 = on), "id='$id' onS5=$on")
    }

    @Test
    fun `the refusal says S5 holds it and that nothing was removed, in one line`() {
        for (t in listOf(siteOnS5, projectOnS5)) {
            assertTrue(t.contains("S5"), t)
            assertTrue(t.contains("nothing was removed"), "the phone's row is still there, and the sentence says so: $t")
            assertFalse(t.contains("Removed from this phone") || t.contains("never reached"), t)
            assertTrue(t.length <= 120, "too long to read on site: ${t.length}")
        }
        // English first (Amit, 16 Nov 2026): under मराठी each reads its English until the Marathi is written (mr-pending.tsv)
        assertEquals(DeleteRoute.Blocked(siteOnS5), Deletes.route(Tx.MR, "site", "", serverKnown = false, onS5 = true))
        assertEquals(DeleteRoute.Blocked(projectOnS5), Deletes.route(Tx.MR, "project", "", serverKnown = false, onS5 = true))
    }

    // ---- the review of F4: what S5 once listed is asked of S5 again, at the delete; and the office's record -------

    private val K = "S5-aaaaaaaaaa"
    private val projectPhotos = "Nothing was removed: this project has photos on this phone. Delete its photos first. It is then removed only if S5 does not hold it."
    private val sitePhotos = "Nothing was removed: this site has photos on this phone. Delete its photos first. It is then removed only if S5 holds no project of it."
    private val signIn = "Not signed in to S5, so not saved — sign in (menu → Server) and Save again"
    private val offline = "Offline — the name changes only when S5 answers; not saved"

    /** A row made on this phone that S5's list once named under [K]: no docname, not in a bootstrap, no ERP pair. */
    private fun gone(
        kind: String = "project", id: String = "", keys: List<String> = listOf(K), captures: Boolean = false, signedIn: Boolean = true,
        read: Deletes.ListRead = Deletes.ListRead.NOT_READ, fresh: Set<String> = emptySet(), onS5: Boolean = true, live: Boolean = false, known: Boolean = false,
    ) = Deletes.route(Tx.EN, kind, id, known, onS5 = onS5, erpLive = live, tie = Deletes.Tie(keys, captures, signedIn, read, fresh))

    @Test
    fun `a photo of the row still on this phone keeps the refusal, and the sentence says to delete the photos first`() {
        // whatever a read would say: a capture may still be on its way up, so no list can call the row gone
        for (read in Deletes.ListRead.values()) for (fresh in listOf(emptySet(), setOf(K))) for (signed in listOf(true, false)) {
            assertEquals(DeleteRoute.Blocked(projectPhotos), gone(captures = true, read = read, fresh = fresh, signedIn = signed), "$read $fresh signedIn=$signed")
            assertEquals(DeleteRoute.Blocked(sitePhotos), gone(kind = "site", captures = true, read = read, fresh = fresh, signedIn = signed))
        }
        // a minted id on the row itself is the same case
        assertEquals(DeleteRoute.Blocked(projectPhotos), gone(id = K, keys = emptyList(), onS5 = false, captures = true))
        for (t in listOf(projectPhotos, sitePhotos)) {
            assertTrue(t.contains("Nothing was removed") && t.contains("Delete its photos first"), t)
            assertTrue(t.contains("only if S5"), "it does not promise the delete will then work: S5 may still hold it -- $t")
            assertTrue(t.length <= 150, "too long to read on site: ${t.length}")
        }
    }

    @Test
    fun `with no photo left S5's list is read first - and never when nobody is signed in to S5`() {
        assertEquals(DeleteRoute.ReadFirst, gone())
        assertEquals(DeleteRoute.ReadFirst, gone(kind = "site", keys = listOf(K, "S5-bbbbbbbbbb")))
        assertEquals(DeleteRoute.ReadFirst, gone(id = K, keys = emptyList(), onS5 = false), "a minted id is itself a key the refusal rests on")
        // signed out is known on the phone: said at once, nothing read
        for (read in Deletes.ListRead.values())
            assertEquals(DeleteRoute.Blocked(signIn), gone(signedIn = false, read = read, fresh = emptySet()), "$read")
        // a refusal that rests on no key has nothing to ask S5 about: it stands as it was
        assertEquals(DeleteRoute.Blocked(projectOnS5), gone(keys = emptyList()))
        assertEquals(DeleteRoute.Blocked(projectOnS5), gone(keys = listOf(" ", "")))
    }

    @Test
    fun `the read decides - S5 names none of the keys and the row goes, S5 names one and the refusal stands, a read that proves nothing changes nothing`() {
        // READ by staff, and S5 names NONE of the keys: the row is the phone's own again, and exactly those keys are forgotten
        assertEquals(DeleteRoute.Gone(listOf(K)), gone(read = Deletes.ListRead.READ, fresh = setOf("PROJ-T1", "S5-cccccccccc")))
        assertEquals(DeleteRoute.Gone(listOf(K)), gone(read = Deletes.ListRead.READ, fresh = emptySet()), "S5 lists nothing at all")
        assertEquals(DeleteRoute.Gone(listOf(K, "S5-bbbbbbbbbb")), gone(kind = "site", keys = listOf(K, " S5-bbbbbbbbbb ", K), read = Deletes.ListRead.READ), "each key once, trimmed")
        assertEquals(DeleteRoute.Gone(listOf("S5-dddddddddd", K)), gone(id = K, keys = listOf("S5-dddddddddd"), read = Deletes.ListRead.READ), "the row's own minted id is one of them")
        // S5 still names ONE of them: S5 holds it, as the refusal said -- for a site, one project of several is enough
        assertEquals(DeleteRoute.Blocked(projectOnS5), gone(read = Deletes.ListRead.READ, fresh = setOf(K)))
        assertEquals(DeleteRoute.Blocked(siteOnS5), gone(kind = "site", keys = listOf(K, "S5-bbbbbbbbbb"), read = Deletes.ListRead.READ, fresh = setOf("S5-bbbbbbbbbb")))
        // no answer of S5's: the offline line, nothing removed
        assertEquals(DeleteRoute.Blocked(offline), gone(read = Deletes.ListRead.OFFLINE))
        assertEquals(DeleteRoute.Blocked(offline), gone(read = Deletes.ListRead.OFFLINE, fresh = emptySet(), kind = "site"))
        // a list read by a login the phone cannot call staff is short for another reason: it proves nothing, whatever it names
        for (fresh in listOf(emptySet(), setOf(K)))
            assertEquals(DeleteRoute.Blocked(projectOnS5), gone(read = Deletes.ListRead.UNPROVEN, fresh = fresh))
        // Gone is never the answer without a READ, and never while a photo is on the phone or nobody is signed in
        for (read in Deletes.ListRead.values()) for (cap in listOf(true, false)) for (signed in listOf(true, false)) for (fresh in listOf(emptySet(), setOf(K))) {
            val r = gone(captures = cap, signedIn = signed, read = read, fresh = fresh)
            if (r is DeleteRoute.Gone) assertTrue(read == Deletes.ListRead.READ && !cap && signed && K !in fresh, "$read $cap $signed $fresh")
            assertTrue(r !is DeleteRoute.LocalOnly && r !is DeleteRoute.Server, "a row S5 once listed is never 'never reached the office', and never the bench's: $r")
        }
    }

    @Test
    fun `a row with nothing on S5 is not asked about S5 at all, whatever the tie says`() {
        for (read in Deletes.ListRead.values()) for (cap in listOf(true, false)) for (signed in listOf(true, false)) {
            val tie = Deletes.Tie(emptyList(), cap, signed, read, emptySet())
            assertEquals(DeleteRoute.LocalOnly, Deletes.route(Tx.EN, "project", "", serverKnown = false, onS5 = false, erpLive = false, tie = tie))
            assertEquals(DeleteRoute.LocalOnly, Deletes.route(Tx.EN, "site", "", serverKnown = false, onS5 = false, erpLive = true, tie = tie))
            assertTrue(Deletes.route(Tx.EN, "site", "", serverKnown = true, onS5 = false, erpLive = false, tie = tie) is DeleteRoute.Blocked)
        }
        // and a caller that gathered no tie routes exactly as before the review, for every combination
        for (kind in listOf("site", "project")) for (id in listOf("", "PROJ-T3", K)) for (known in listOf(true, false)) for (on in listOf(true, false))
            assertEquals(Deletes.route(Tx.EN, kind, id, known, onS5 = on), Deletes.route(Tx.EN, kind, id, known, onS5 = on, erpLive = true, tie = null), "$kind '$id' $known $on")
    }

    @Test
    fun `a bench docname with no live ERP pair is the office's record - refused at once, never sent out with a dead key`() {
        val project = "This project is the office's record. It is removed at the office, not from this phone."
        val site = "This site is the office's record. It is removed at the office, not from this phone."
        // whatever S5 holds of it, and whatever the phone gathered: the docname decides first, as it does for a rename
        for (on in listOf(true, false)) for (known in listOf(true, false)) for (tie in listOf(null, Deletes.Tie(listOf(K), false, true), Deletes.Tie(listOf(K), true, false, Deletes.ListRead.READ))) {
            assertEquals(DeleteRoute.Blocked(project), Deletes.route(Tx.EN, "project", "PROJ-T3", known, onS5 = on, erpLive = false, tie = tie), "onS5=$on known=$known")
            assertEquals(DeleteRoute.Blocked(site), Deletes.route(Tx.EN, "site", " SITE-T2 ", known, onS5 = on, erpLive = false, tie = tie))
        }
        // a LIVE pair: the bench is asked as before, and S5's refusal still comes first
        assertEquals(DeleteRoute.Server("PROJ-T3"), Deletes.route(Tx.EN, "project", "PROJ-T3", serverKnown = true, onS5 = false, erpLive = true))
        assertEquals(DeleteRoute.Blocked(projectOnS5), Deletes.route(Tx.EN, "project", "PROJ-T3", serverKnown = true, onS5 = true, erpLive = true))
        // ...and where S5's list, read for the delete, no longer names it, the bench's own record is the bench's to delete
        assertEquals(DeleteRoute.Server("PROJ-T3"), Deletes.route(Tx.EN, "project", "PROJ-T3", serverKnown = true, onS5 = true, erpLive = true,
            tie = Deletes.Tie(listOf("PROJ-T3"), false, true, Deletes.ListRead.READ, emptySet())))
        // a phone-minted key is never the office's record, pair or none; nor is a row with no id
        for (live in listOf(true, false)) {
            assertEquals(DeleteRoute.Blocked(projectOnS5), Deletes.route(Tx.EN, "project", K, serverKnown = false, erpLive = live))
            assertEquals(DeleteRoute.LocalOnly, Deletes.route(Tx.EN, "project", "", serverKnown = false, erpLive = live))
        }
        // a client keeps its own refusal
        assertEquals(Deletes.route(Tx.EN, "client", "", false), Deletes.route(Tx.EN, "client", "CUST-T1", true, erpLive = false))
        for (t in listOf(project, site)) assertTrue(t.length <= 110 && !t.contains("server") && !t.contains("configured"), t)
    }

    @Test
    fun `the banner after a row S5 no longer lists was removed says so - never that it had never reached the office`() {
        val msg = Deletes.goneMessage(Tx.EN, "test unit")
        assertEquals("Removed from this phone — S5 no longer lists test unit.", msg)
        assertFalse(msg.contains("never reached"), msg)
        assertEquals("Removed from this phone — S5 no longer lists Main site.", Deletes.goneMessage(Tx.EN, "Main site"))
        // English first (Amit, 16 Nov 2026): under मराठी the new sentences read their English until the Marathi is written
        assertEquals(DeleteRoute.Blocked(projectPhotos), Deletes.route(Tx.MR, "project", "", false, onS5 = true, erpLive = false, tie = Deletes.Tie(listOf(K), true, true)))
        assertEquals(Deletes.officeRecord(Tx.EN, "site"), Deletes.officeRecord(Tx.MR, "site"))
    }

    @Test
    fun `the delete reads S5's list itself, as staff, and forgets nothing but the keys the rule names - read as source`() {
        val session = File("../app/src/main/kotlin/com/malletcrafts/interiorestimate/S5Session.kt").readText()
        val read = session.substring(session.indexOf("fun listedNow(context: Context): Pair<Deletes.ListRead, Set<String>> = try {")).substringBefore("fun forgetKeys(")
        // the S5 SESSION, never the legacy key (S5Auth), and staff by the phone's own record of the sign-in
        assertTrue(read.contains("val cookie = S5Session.liveCookie(context)") && !read.contains("S5Auth.") && !read.contains("FrappeClient"), "the session alone")
        assertTrue(read.contains("val staff = S5Session.who(context)?.optString(\"kind\") == \"staff\""))
        assertTrue(read.contains("Pair(if (staff) Deletes.ListRead.READ else Deletes.ListRead.UNPROVEN, named)"))
        assertTrue(read.contains("val arr = o.getJSONArray(\"projects\")"), "a 200 without the list is no list: it throws, and that is OFFLINE")
        assertEquals(2, Regex("Pair\\(Deletes\\.ListRead\\.OFFLINE, emptySet<String>\\(\\)\\)").findAll(read).count(), "no session S5 honours, and any failure")
        // the read writes NOTHING: not the masters, not S5's names, not the keys
        for (w in listOf("saveMasters", "S5Sites.", "S5Keys.", ".edit()")) assertFalse(read.contains(w), "the delete's read writes nothing ($w)")
        // the masters' half of Gone: only entries S5's list had added, and only for the keys named
        val drop = session.substring(session.indexOf("fun forgetKeys(context: Context, keys: List<String>) {")).substringBefore("fun forget(context: Context) {")
        assertTrue(drop.contains("if (!(p.optBoolean(FROM_S5) && p.optString(\"project\") in keys)) out.put(p)"), "never a bootstrap's entry")
        // the handler: read first only on the rule's word, the route asked again with the read, and the row dropped only on Gone or LocalOnly
        val main = File("../app/src/main/kotlin/com/malletcrafts/interiorestimate/MainActivity.kt").readText()
        val tap = main.substring(main.indexOf("val route = deleteRoute(t)")).substringBefore("FrappeClient.load(context)")
        val first = tap.indexOf("} else if (route is DeleteRoute.ReadFirst) {"); val listed = tap.indexOf("val listed = withContext(Dispatchers.IO) { S5Masters.listedNow(context) }")
        val again = tap.indexOf("val go = deleteRoute(t, listed.first, listed.second)"); val drops = tap.indexOf("if (go is DeleteRoute.Gone) removeHere(t, go.keys)")
        assertTrue(first in 0 until listed && listed < again && again < drops, "ReadFirst, the read, the route again, and only then a removal")
        assertTrue(tap.contains("deleteBlocker = (go as? DeleteRoute.Blocked)?.reason ?: tx.refuse_site_offline"), "anything else is said in the dialog, nothing removed")
        assertEquals(1, Regex("S5Masters\\.listedNow\\(").findAll(main).count(), "one reader, the delete's")
        val sites = File("../app/src/main/kotlin/com/malletcrafts/interiorestimate/S5Sites.kt").readText()
        assertTrue(sites.contains("synchronized(LOCK) { prefs(context).edit().remove(project).apply() }") && Regex("\\.remove\\(").findAll(sites).count() == 1, "S5Sites has one remover: forget")
    }

    @Test
    fun `the phone's delete asks with what S5 holds of the row - read as source, since only CI compiles the app`() {
        val f = File("../app/src/main/kotlin/com/malletcrafts/interiorestimate/MainActivity.kt")
        assertTrue(f.isFile, "the app's MainActivity.kt is beside :pano (${f.absolutePath})")
        val main = f.readText()
        assertEquals(1, Regex("Deletes\\.route\\(").findAll(main.lines().filterNot { it.trim().startsWith("//") }.joinToString("\n")).count(), "one caller of the rule")
        // THESE PINS MOVED WITH THE REVIEW OF F4, and hold no less. The one caller is now `deleteRoute`, because the
        // route is asked twice in one delete (when Delete is tapped, and again with S5's list read for it), and it is
        // asked with MORE: the same gathering the rename's route asks (Catalogue.s5Ties, whose `keys.isNotEmpty()` IS
        // Catalogue.onS5), whether the ERP pair is live, and what the phone holds of the row's tie to S5.
        val ask = main.substring(main.indexOf("fun deleteRoute(t: RenameTarget, read: Deletes.ListRead = Deletes.ListRead.NOT_READ, fresh: Set<String> = emptySet()): DeleteRoute {"))
            .substringBefore("fun removeHere(")
        assertTrue(ask.contains("val ties = cat.s5Ties(masters, queue, t.kind, t.client, t.site, t.project)"), "the same gathering the rename's route asks")
        assertTrue(Regex("""return Deletes\.route\(tx, t\.kind, t\.serverId, t\.serverKnown,\s*onS5 = ties\.keys\.isNotEmpty\(\),""").containsMatchIn(ask))
        assertTrue(ask.contains("erpLive = S5Masters.erpLive(context),"), "a bench docname goes out only with a LIVE pair")
        assertTrue(ask.contains("tie = Deletes.Tie(ties.keys, ties.captures, signedIn = S5Session.signedIn(context), listRead = read, freshNames = fresh))"),
            "the keys, the photos, an S5 sign-in (never the legacy key), and the read made for this delete")
        // "Removed from this phone ... never reached the office system" is said for the LocalOnly route alone: the one
        // function that drops a row says it only where no key was forgotten, and the Gone route says its own sentence
        val here = main.substring(main.indexOf("fun removeHere(t: RenameTarget, gone: List<String>) {")).substringBefore("// ---- ONE PROJECT, ONE SITE")
        assertTrue(here.contains("if (gone.isEmpty()) lastResult = { tx -> Deletes.localOnlyMessage(tx, t.current) }") &&
            here.contains("else lastResult = { tx -> Deletes.goneMessage(tx, t.current) }"), "the sentence follows the route")
        assertEquals(1, Regex("Deletes\\.localOnlyMessage\\(").findAll(main).count())
        assertEquals(1, Regex("Deletes\\.goneMessage\\(").findAll(main).count())
        val tap = main.substring(main.indexOf("val route = deleteRoute(t)")).substringBefore("FrappeClient.load(context)")
        assertTrue(tap.contains("} else if (route is DeleteRoute.LocalOnly) {\n                    removeHere(t, emptyList())"), "the local-only route drops the row and forgets nothing")
    }
}
