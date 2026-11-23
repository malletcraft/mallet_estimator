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

    @Test
    fun `the phone's delete asks with what S5 holds of the row - read as source, since only CI compiles the app`() {
        val f = File("../app/src/main/kotlin/com/malletcrafts/interiorestimate/MainActivity.kt")
        assertTrue(f.isFile, "the app's MainActivity.kt is beside :pano (${f.absolutePath})")
        val main = f.readText()
        assertEquals(1, Regex("Deletes\\.route\\(").findAll(main.lines().filterNot { it.trim().startsWith("//") }.joinToString("\n")).count(), "one caller of the rule")
        assertTrue(Regex("""val route = Deletes\.route\(tx, t\.kind, t\.serverId, t\.serverKnown,\s*onS5 = cat\.onS5\(masters, queue, t\.kind, t\.client, t\.site, t\.project\)\)""").containsMatchIn(main),
            "the same Catalogue.onS5 the rename's route asks")
        // "Removed from this phone" is said on the LocalOnly route alone
        val a = main.indexOf("} else if (route is DeleteRoute.LocalOnly) {"); val b = main.indexOf("lastResult = { tx -> Deletes.localOnlyMessage(tx, t.current) }")
        assertTrue(a in 0 until b && !main.substring(a, b).contains("} else {"), "the local-only sentence follows the local-only route")
        assertEquals(1, Regex("Deletes\\.localOnlyMessage\\(").findAll(main).count())
    }
}
