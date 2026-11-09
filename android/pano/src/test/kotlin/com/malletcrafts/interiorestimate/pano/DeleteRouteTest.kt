package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
