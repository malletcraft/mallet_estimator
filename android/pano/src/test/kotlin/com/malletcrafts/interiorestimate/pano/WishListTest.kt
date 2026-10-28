package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.WishList.Local
import com.malletcrafts.interiorestimate.pano.WishList.Row
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The wish list on the phone (v52 item 16, slice 5b; decisions 2, 3 and 8 of 15 Oct 2026),
 * against the prototype's texts and rules (wish.js; ct52 W, W2, FX16; v61c.1's lock at Client
 * approved, ct61c C61c-W) and S5's services and 409s (functions/_lib/asis.js at 943dfd2).
 */
class WishListTest {

    private fun cache(stage: String?) =
        if (stage == null) null else QuoteStage.Cache(listed = true, stage = stage, at = "2026-10-14T02:38:00.000Z")

    @Test fun `the twelve services are S5's WISH_SERVICES, ids and names in S5's order`() {
        // A copy of functions/_lib/asis.js WISH_SERVICES (S5 QUOTE, stage 72b687a). Change it here only with S5.
        val ids = listOf("electrical", "cctv", "intercom", "false-ceiling", "doors-windows", "balcony-grill",
            "carpentry", "master-bath", "guest-bath", "painting", "curtains", "plumbing")
        val names = listOf("Electrical works", "CCTV", "Intercom", "False ceiling", "Doors & windows", "Balcony grill",
            "Carpentry / furniture", "Master bathroom — complete renovation", "Guest bathroom — complete renovation",
            "Painting", "Curtains — roller blinds", "Plumbing & sanitary")
        assertEquals(ids, WishList.SERVICES.map { it.id })
        assertEquals(names, WishList.SERVICES.map { it.name })
        assertEquals(12, WishList.IDS.size)
        assertEquals("safety door, aluminium main door, UPVC", WishList.SERVICES[4].detail)
        assertEquals("water and drain points, taps, geyser, sanitary ware outside the bathroom renovations",
            WishList.SERVICES[11].detail)
    }

    @Test fun `count, chip and total (ct52 W(b), N of 12)`() {
        assertEquals(0, WishList.count(emptyMap()))
        assertEquals("Wish list 0/12", WishList.chip(0))
        var rows = WishList.toggle(emptyMap(), "electrical")
        rows = WishList.toggle(rows, "painting")
        assertEquals(2, WishList.count(rows))
        assertEquals("2 of 12 wanted", WishList.total(2))
        // unticked keeps its rooms and note but is not counted; an id S5 does not list is not counted
        rows = WishList.toggle(rows, "painting") + ("gone" to Row(on = true))
        assertEquals(1, WishList.count(rows))
    }

    @Test fun `stage rules - read-only at Survey, Estimated and Client approved, warn from Vendor quoted to Revisions (v61c_1)`() {
        val idx = QuoteStage.STAGES.map { WishList.stageIndex(cache(it)) }
        assertEquals(listOf(0, 1, 2, 3, 4, 5), idx)
        assertEquals(3, WishList.ESTIMATED)
        assertEquals(QuoteStage.indexOf("Estimated"), WishList.ESTIMATED)
        assertEquals(QuoteStage.indexOf(QuoteStage.APPROVED), WishList.APPROVED)
        // TOOLS.wishEdit [0,1,1,0,1] (index61c1.html:3954); Client approved has no column, which can() reads as 0
        assertEquals(listOf(false, true, true, false, true, false), idx.map { WishList.editable(it) })
        // the after-quote notice: never at Client approved, where nothing can be edited (§16.5)
        assertEquals(listOf(false, false, true, true, true, false), idx.map { WishList.warnAfterQuote(it) })
        assertEquals(listOf(WishList.FINISH_SURVEY), WishList.banners(Tx.EN, 0, warned = true))
        assertEquals(emptyList(), WishList.banners(Tx.EN, 1, warned = true))
        assertEquals(emptyList(), WishList.banners(Tx.EN, 2, warned = false))
        assertEquals(listOf("Changing the wish list after a vendor quoted — tell the vendor"), WishList.banners(Tx.EN, 2, warned = true))
        assertEquals("Finish the survey first — the stage moves to Client wish list on S5", WishList.FINISH_SURVEY)
        // v61b: the prototype's refuse.readonly.wishlist line at Estimated and, when an edit was made after a vendor
        // quoted this visit, the vendor reminder beside it (the prototype keeps after = idx >= 2)
        assertEquals(listOf("Estimation — the wish list is read-only; move the phase back on the project screen to change it"), WishList.banners(Tx.EN, 3, warned = false))
        assertEquals(listOf("Estimation — the wish list is read-only; move the phase back on the project screen to change it",
                            "Changing the wish list after a vendor quoted — tell the vendor"), WishList.banners(Tx.EN, 3, warned = true))
        // Revisions: editable (v61c), with the after-quote warning only
        assertEquals(emptyList(), WishList.banners(Tx.EN, 4, warned = false))
        assertEquals(listOf("Changing the wish list after a vendor quoted — tell the vendor"), WishList.banners(Tx.EN, 4, warned = true))
        // v61c.1: Client approved is READ-ONLY under S5's APPROVED_FINAL verbatim, without the 409's "; not saved",
        // and no after-quote notice there even with an edit earlier in the visit
        for (w in listOf(false, true)) assertEquals(listOf("Approved — changes go through change requests"), WishList.banners(Tx.EN, 5, warned = w))
        assertEquals("Approved — changes go through change requests", WishList.lockedLine(Tx.EN, 5))
        assertEquals("मंजूर झाले — पुढचे बदल \"बदल विनंती\" मधूनच होतात", WishList.lockedLine(Tx.MR, 5))
        assertEquals("Estimation — the wish list is read-only; move the phase back on the project screen to change it", WishList.lockedLine(Tx.EN, 3))
        assertEquals("अंदाजपत्रक — इच्छा-यादी फक्त वाचण्यासाठी आहे; बदलण्यासाठी प्रोजेक्टच्या स्क्रीनवर फेज मागे न्या", WishList.lockedLine(Tx.MR, 3))
        for (i in listOf(null, 0, 1, 2, 4)) assertEquals(null, WishList.lockedLine(Tx.EN, i), "lockedLine($i)")
    }

    @Test fun `the Vendor field shows only at Vendor quoted and Revisions, and with the stage unread (TOOLS vendorUI)`() {
        // TOOLS.vendorUI [0,0,1,0,1]; can() is true for an unread phase and reads Client approved's missing column as 0
        assertEquals(listOf(true, false, false, true, false, true, false),
            listOf(null, 0, 1, 2, 3, 4, 5).map { WishList.vendorUi(it) })
        // an unread stage (never read, not listed, a name outside the six) shows it
        for (c in listOf(null, QuoteStage.Cache(listed = false), QuoteStage.Cache(listed = true, stage = "Quoted")))
            assertTrue(WishList.vendorUi(WishList.stageIndex(c)), "$c")
    }

    @Test fun `the head drops the vendor half where the Vendor field is hidden (v54 finding 4)`() {
        assertEquals("What the client wants, after the survey — the vendor quotes against it", WishList.head(Tx.EN, vendorUi = true))
        assertEquals("What the client wants, after the survey", WishList.head(Tx.EN, vendorUi = false))
        assertEquals("सर्व्हेनंतर ग्राहकाला काय हवे — व्हेंडर त्यावर कोटेशन देतो", WishList.head(Tx.MR, vendorUi = true))
        assertEquals("सर्व्हेनंतर ग्राहकाला काय हवे", WishList.head(Tx.MR, vendorUi = false))
        // at each stage, as the screen picks it
        assertEquals(listOf(true, false, false, true, false, true, false).map { if (it) "What the client wants, after the survey — the vendor quotes against it" else "What the client wants, after the survey" },
            listOf(null, 0, 1, 2, 3, 4, 5).map { WishList.head(Tx.EN, WishList.vendorUi(it)) })
    }

    @Test fun `stage not read - editable, says so, never Finish the survey first (FX16)`() {
        for (c in listOf(null, QuoteStage.Cache(listed = true, stage = ""),
                         QuoteStage.Cache(listed = true, stage = "Quoted"))) {
            val i = WishList.stageIndex(c)
            assertEquals(null, i, "$c")
            assertTrue(WishList.editable(i))
            assertFalse(WishList.warnAfterQuote(i))
            assertFalse(WishList.notOnS5(synced = true, cache = c), "$c")
            assertEquals(listOf("Quote stage not read yet — S5 decides when online"),
                WishList.banners(Tx.EN, i, warned = true, notOnS5 = WishList.notOnS5(true, c)))
        }
    }

    @Test fun `not on S5 - phone-only or not listed - editable, and no 'S5 decides when online' under 'not on S5 yet'`() {
        // A phone-only project never asks S5; a not-listed one is 404 until its first room upload (decision 3).
        for ((synced, c) in listOf(false to null, true to QuoteStage.Cache(listed = false),
                                   false to QuoteStage.Cache(listed = true, stage = "Survey"))) {
            assertTrue(WishList.notOnS5(synced, c), "$synced $c")
            val i = if (synced) WishList.stageIndex(c) else null
            assertTrue(WishList.editable(i))
            assertEquals(emptyList(), WishList.banners(Tx.EN, i, warned = true, notOnS5 = true))
        }
        // and the sub-line those projects show says the same thing
        assertEquals("QUOTE STAGE · not on S5 yet", QuoteStage.phoneOnly().title)
        assertEquals("QUOTE STAGE · not on S5 yet",
            QuoteStage.lineFor(QuoteStage.Cache(listed = false), java.time.Instant.parse("2026-10-16T05:00:00Z")).title)
    }

    @Test fun `applies-to line and the row's second line (ct52 W(b))`() {
        val svc = WishList.SERVICES[0]
        assertEquals("Whole flat", WishList.subLine(Row(on = true)))
        assertEquals("Whole flat", WishList.rowLine(svc, Row(on = true), vendorUi = true))
        assertEquals("not asked for", WishList.rowLine(svc, Row(), vendorUi = true))
        assertEquals("safety door, aluminium main door, UPVC", WishList.rowLine(WishList.SERVICES[4], Row(), vendorUi = false))
        val grid = listOf("Living Room", "Kitchen", "Master Bedroom")
        var rows = WishList.toggle(emptyMap(), "electrical")
        rows = WishList.toggleRoom(rows, "electrical", "Kitchen", grid)
        rows = WishList.toggleRoom(rows, "electrical", "Living Room", grid)
        // kept in the grid's order whatever order they were pressed in
        assertEquals(listOf("Living Room", "Kitchen"), WishList.row(rows, "electrical").rooms)
        rows = WishList.setNote(rows, "electrical", "more\n  points")
        assertEquals("Living Room · Kitchen · more points", WishList.rowLine(svc, WishList.row(rows, "electrical"), vendorUi = true))
        rows = WishList.setVendor(rows, "electrical", "V1")
        assertEquals("Living Room · Kitchen · more points · vendor V1", WishList.rowLine(svc, WishList.row(rows, "electrical"), vendorUi = true))
        // v54 finding 4: where the Vendor field is hidden (Client wish list, Estimated, Client approved) the row
        // names no vendor, though the vendor is kept: the rooms and the note still read what was asked for
        assertEquals("Living Room · Kitchen · more points", WishList.rowLine(svc, WishList.row(rows, "electrical"), vendorUi = false))
        assertEquals("V1", WishList.row(rows, "electrical").vendor)
        rows = WishList.toggleRoom(rows, "electrical", "Living Room", grid)
        assertEquals(listOf("Kitchen"), WishList.row(rows, "electrical").rooms)
        rows = WishList.wholeFlat(rows, "electrical")
        assertEquals("Whole flat", WishList.subLine(WishList.row(rows, "electrical")))
        assertEquals("Whole flat · more points", WishList.rowLine(svc, WishList.row(rows, "electrical"), vendorUi = false))
    }

    @Test fun `a room written on S5 that the grid does not show is kept`() {
        val rows = mapOf("cctv" to Row(on = true, rooms = listOf("Terrace")))
        val next = WishList.toggleRoom(rows, "cctv", "Kitchen", listOf("Living Room", "Kitchen"))
        assertEquals(listOf("Kitchen", "Terrace"), WishList.row(next, "cctv").rooms)
    }

    @Test fun `note and vendor - newlines become spaces, capped at S5's 500 and 140`() {
        assertEquals("a b c", WishList.cleanNote("a\nb \r\n c"))
        assertEquals(500, WishList.cleanNote("x".repeat(600)).length)
        assertEquals(140, WishList.cleanVendor("v".repeat(200)).length)
        assertEquals("one two", WishList.cleanVendor("one\ntwo"))
    }

    @Test fun `keys - the ERP id, else local client and title (FX6), re-keyed when the id appears`() {
        assertEquals("PROJ-0025", WishList.keyOf("PROJ-0025", "Demo Client", "Demo flat"))
        val k = WishList.keyOf("", "Demo Client", "Flat \"A\"")
        assertEquals("local:Demo Client|Flat \"A\"", k)
        assertTrue(WishList.isLocal(k))
        val locals = mapOf(
            k to Local("Demo Client", "Flat \"A\"", mapOf("cctv" to Row(on = true)), dirty = true),
            "local:Other|Nowhere" to Local("Other", "Nowhere"),
            "PROJ-0001" to Local("X", "Y"))
        val erp = listOf(WishList.Erp("", "Demo Client", "Flat \"A\""),
            WishList.Erp("PROJ-0031", " demo  client", "flat \"a\" "))
        assertEquals(mapOf(k to "PROJ-0031"), WishList.rekeys(locals, erp))
    }

    @Test fun `adopt - the phone's list moves under the id, dirty, on its own base so S5 answers 409 and wins`() {
        val local = Local("C", "T", mapOf("cctv" to Row(on = true)), dirty = true, savedAt = "2026-10-16T05:00:00Z")
        val moved = WishList.adopt(local, null)!!
        assertEquals("", moved.at)
        assertTrue(moved.dirty)
        assertEquals(1, WishList.count(moved.rows))
        // S5's list (staff's Painting) pulled under the id: the offline list does NOT take its
        // stamp, so its PUT goes on base "" and S5 answers 409 rather than losing staff's rows
        val fetched = Local("C", "T", mapOf("painting" to Row(on = true)), at = "2026-10-15T10:00:00Z")
        val onTop = WishList.adopt(local, fetched)!!
        assertEquals("", onTop.at)
        assertEquals(local.rows, onTop.rows)
        assertTrue(onTop.dirty)
        val after409 = WishList.afterConflict(onTop, fetched.rows, fetched.at)
        assertEquals(fetched.rows, after409.rows)
        assertEquals(WishList.REPLACED, after409.note)
        // an unsent list already under the id: the local one is not moved over it, nor dropped
        assertEquals(null, WishList.adopt(local, fetched.copy(dirty = true)))
    }

    @Test fun `a stale phone-only project reads and edits the moved list through its forward`() {
        val localKey = WishList.localKey("C", "T")
        val store = linkedMapOf<String, Any>(
            localKey to Local("C", "T", mapOf("cctv" to Row(on = true), "painting" to Row(on = true)), dirty = true))
        // the re-key, as WishSync.rekey writes it: the list under the id, a forward at the old key
        val to = WishList.rekeys(mapOf(localKey to store[localKey] as Local), listOf(WishList.Erp("PROJ-0042", "C", "T")))[localKey]!!
        store[to] = WishList.adopt(store[localKey] as Local, null)!!
        store[WishList.forwardKey(localKey)] = to
        store.remove(localKey)
        val lookup: (String) -> String? = { k -> store[k] as? String }
        // the screen still holding the local project resolves to the moved list: 2 of 12, not 0
        val k = WishList.resolve(localKey, lookup)
        assertEquals("PROJ-0042", k)
        val cur = store[k] as Local
        assertEquals(2, WishList.count(cur.rows))
        // an edit there lands on the full list, never a one-row list under the old key
        store[k] = WishList.edited(cur, WishList.toggle(cur.rows, "intercom"), "2026-10-16T06:00:00Z")
        assertEquals(3, WishList.count((store[k] as Local).rows))
        assertFalse(localKey in store)
        // a key that was never moved, and an ERP id, resolve to themselves
        assertEquals("local:C|Other", WishList.resolve("local:C|Other", lookup))
        assertEquals("PROJ-0042", WishList.resolve("PROJ-0042", lookup))
        assertTrue(WishList.isForward(WishList.forwardKey(localKey)))
        assertFalse(WishList.isLocal(WishList.forwardKey(localKey)))
    }

    @Test fun `a phone-only project or client renamed takes its list along`() {
        val a = WishList.localKey("Demo Client", "Flat A")
        val b = WishList.localKey("Demo Client", "Flat B")
        val locals = mapOf(
            a to Local("Demo Client", "Flat A", mapOf("cctv" to Row(on = true)), dirty = true),
            b to Local("Demo Client", "Flat B", mapOf("painting" to Row(on = true)), dirty = true),
            WishList.localKey("Other", "Flat A") to Local("Other", "Flat A"),
            "PROJ-0001" to Local("Demo Client", "Flat A"))
        // a project rename: only that project's list, under its new title
        val newA = WishList.localKey("Demo Client", "Flat A Kothrud")
        val p = WishList.renamed(locals, "project", "demo client", "flat a", " Flat A Kothrud ", live = setOf(newA, b))
        assertEquals(setOf(a), p.keys)
        assertEquals(newA, p.getValue(a).to)
        assertEquals("Flat A Kothrud", p.getValue(a).list.title)
        assertEquals(locals.getValue(a).rows, p.getValue(a).list.rows)
        assertTrue(p.getValue(a).list.dirty)
        assertFalse(p.getValue(a).keepOld)
        // and the re-key then finds it under the new name
        assertEquals(mapOf(newA to "PROJ-0050"),
            WishList.rekeys(mapOf(newA to p.getValue(a).list), listOf(WishList.Erp("PROJ-0050", "Demo Client", "Flat A Kothrud"))))
        // a client rename: every phone-only project's list under that client
        val c = WishList.renamed(locals, "client", "Demo Client", "", "Demo Client Pune", live = emptySet())
        assertEquals(setOf(a, b), c.keys)
        assertEquals(WishList.localKey("Demo Client Pune", "Flat B"), c.getValue(b).to)
        assertEquals("Demo Client Pune", c.getValue(b).list.client)
        // a site rename moves nothing; neither does a blank name
        assertEquals(emptyMap(), WishList.renamed(locals, "site", "Demo Client", "", "Wing B", emptySet()))
        assertEquals(emptyMap(), WishList.renamed(locals, "project", "Demo Client", "Flat A", "  ", emptySet()))
        // the old title still names a project at another site: copied, not taken from it
        assertTrue(WishList.renamed(locals, "project", "Demo Client", "Flat A", "Flat C", setOf(a)).getValue(a).keepOld)
        // a new name whose key already holds a list keeps that list
        assertEquals(emptyMap(), WishList.renamed(locals, "project", "Demo Client", "Flat A", "Flat B", setOf(b)))
        // a rename that only changes case still moves the list (keys are exact)
        assertEquals(WishList.localKey("Demo Client", "flat a"),
            WishList.renamed(locals, "project", "Demo Client", "Flat A", "flat a", emptySet()).getValue(a).to)
    }

    @Test fun `409 - S5 wins, the phone takes its rows and stamp, the note says so, dirty cleared (decision 2)`() {
        val mine = WishList.edited(Local("C", "T", at = "2026-10-15T10:00:00Z"),
            mapOf("cctv" to Row(on = true)), "2026-10-16T05:00:00Z")
        assertTrue(mine.dirty)
        val s5 = mapOf("painting" to Row(on = true, rooms = listOf("Kitchen")))
        val after = WishList.afterConflict(mine, s5, "2026-10-16T04:00:00Z")
        assertEquals(s5, after.rows)
        assertEquals("2026-10-16T04:00:00Z", after.at)
        assertFalse(after.dirty)
        assertEquals("S5 had a newer list; yours was replaced, re-apply what is missing", after.note)
        // the next edit clears the note
        assertEquals("", WishList.edited(after, after.rows, "2026-10-16T06:00:00Z").note)
    }

    @Test fun `409 after a lost 200 - S5 holds the phone's own list - no 'replaced' note, the stamp adopted`() {
        val mine = WishList.edited(Local("C", "T", at = "2026-10-15T10:00:00Z"),
            mapOf("cctv" to Row(on = true, note = "tiles ", rooms = listOf("Kitchen"))), "2026-10-16T05:00:00Z")
        // what S5 saved from the PUT whose answer was lost: every service, trimmed
        val s5 = WishList.forPut(mapOf("cctv" to Row(on = true, note = "tiles", rooms = listOf("Kitchen"))))
        val after = WishList.afterConflict(mine, s5, "2026-10-16T05:00:02Z")
        assertEquals("", after.note)
        assertFalse(after.dirty)
        assertEquals("2026-10-16T05:00:02Z", after.at)
        assertEquals(mine.rows, after.rows)
    }

    @Test fun `refused for good - 403 for a key that is not staff, 400, 413 - one note, not dirty, the next GET takes S5's copy`() {
        assertEquals(listOf(400, 403, 413), listOf(400, 401, 403, 404, 405, 409, 413, 500, 503).filter { WishList.refusedForGood(it) })
        val sent = mapOf("cctv" to Row(on = true))
        val mine = WishList.edited(Local("C", "T", at = "2026-10-15T10:00:00Z"), sent, "2026-10-16T05:00:00Z")
        val after = WishList.afterRefused(mine, sent, "Only MCFT staff can change the wish list")
        assertFalse(after.dirty)
        assertEquals("Not sent to S5 — Only MCFT staff can change the wish list", after.note)
        assertEquals(sent, after.rows)
        // no longer dirty, so the GET on open shows staff's list again
        val s5 = mapOf("painting" to Row(on = true))
        assertEquals(s5, WishList.afterGet(after, s5, "2026-10-16T06:00:00Z").rows)
        // an edit made while the refused PUT was out stays dirty and tries once more
        val moved = mine.copy(rows = WishList.toggle(sent, "intercom"))
        assertEquals(moved, WishList.afterRefused(moved, sent, "Only MCFT staff can change the wish list"))
        // and the next edit clears the note
        assertEquals("", WishList.edited(after, after.rows, "2026-10-16T07:00:00Z").note)
    }

    @Test fun `200 - S5's stamp is the base, the phone's own rows kept, an edit made while the PUT was out stays dirty`() {
        val sent = mapOf("cctv" to Row(on = true, note = "x "))
        val l = Local("C", "T", sent, dirty = true)
        val ok = WishList.afterPut(l, sent, "2026-10-16T05:00:01Z")
        assertFalse(ok.dirty)
        assertEquals("2026-10-16T05:00:01Z", ok.at)
        // S5 saved "x" (trimmed); the Note field under the cursor keeps the space being typed
        assertEquals("x ", WishList.row(ok.rows, "cctv").note)
        val moved = l.copy(rows = WishList.toggle(sent, "painting"))
        val later = WishList.afterPut(moved, sent, "2026-10-16T05:00:01Z")
        assertTrue(later.dirty)
        assertEquals("2026-10-16T05:00:01Z", later.at)
        assertEquals(2, WishList.count(later.rows))
        assertEquals(12, WishList.forPut(emptyMap()).size)
    }

    @Test fun `GET on open - a clean list takes S5's copy, an unsent edit is never touched`() {
        val s5 = mapOf("intercom" to Row(on = true))
        val clean = Local("C", "T", mapOf("cctv" to Row(on = true)), at = "a")
        assertEquals(s5, WishList.afterGet(clean, s5, "b").rows)
        val dirty = clean.copy(dirty = true)
        assertEquals(dirty, WishList.afterGet(dirty, s5, "b"))
    }

    @Test fun `GET on open is a compare-and-set - a PUT's 200 landing while it was out is not rolled back`() {
        val before = Local("C", "T", mapOf("cctv" to Row(on = true)), at = "2026-10-16T05:00:00.000Z", dirty = true)
        val s5Old = mapOf("cctv" to Row(on = true))
        // the GET went out with `before` stored; SyncWorker's PUT then landed (clean, newer stamp)
        val sent = WishList.edited(before, WishList.toggle(before.rows, "painting"), "2026-10-16T05:01:00Z")
        val afterPut = WishList.afterPut(sent, sent.rows, "2026-10-16T05:02:00.000Z")
        val got = WishList.afterGet(afterPut, s5Old, "2026-10-16T05:00:00.000Z", seen = before)
        assertEquals(afterPut, got)
        assertEquals(2, WishList.count(got.rows))
        // an answer older than the stored stamp is dropped even when nothing else moved
        assertEquals(afterPut, WishList.afterGet(afterPut, s5Old, "2026-10-16T05:00:00.000Z"))
        // the same list unchanged while the GET was out takes S5's newer copy
        val staff = mapOf("intercom" to Row(on = true))
        assertEquals(staff, WishList.afterGet(afterPut, staff, "2026-10-16T06:00:00.000Z", seen = afterPut).rows)
    }

    /** S5's words, verbatim from functions/_lib/asis.js at 943dfd2 (APPROVED_FINAL + '; not saved', and FINISH_SURVEY). */
    private val s5Final = "Approved — changes go through change requests; not saved"
    private val s5Survey = "Finish the survey first — the stage moves to Client wish list on S5"

    @Test fun `409 final at Client approved - S5's list for good, not dirty, in S5's words, never REPLACED (v61c_1)`() {
        val mine = WishList.edited(Local("C", "T", at = "2026-10-27T10:00:00.000Z"),
            mapOf("cctv" to Row(on = true), "painting" to Row(on = true, note = "two coats")), "2026-10-28T05:00:00Z")
        assertTrue(mine.dirty)
        // the answer's rows: the stored list, every service, in the stale-base 409's shape
        val s5 = WishList.forPut(mapOf("painting" to Row(on = true, rooms = listOf("Kitchen"))))
        val after = WishList.afterFinal(mine, s5, "2026-10-28T02:00:00.000Z", s5Final)
        assertEquals(s5, after.rows)
        assertEquals("2026-10-28T02:00:00.000Z", after.at)
        assertFalse(after.dirty)
        assertEquals("", after.note)
        assertEquals(s5Final, after.status)
        assertFalse(after.note == WishList.REPLACED || after.status == WishList.REPLACED)
        // an old REPLACED or NOT_SENT note goes too: S5's words are the one line
        assertEquals("", WishList.afterFinal(mine.copy(note = WishList.REPLACED), s5, "x", s5Final).note)
        // even when S5 holds exactly the phone's list (a lost 200 before the Approve), it is S5's words, not silence
        val same = WishList.afterFinal(mine, WishList.forPut(mine.rows), "y", s5Final)
        assertEquals(s5Final, same.status)
        assertFalse(same.dirty)
        // S5's error is one line, trimmed
        assertEquals(s5Final, WishList.afterFinal(mine, s5, "x", "  Approved — changes go through change requests;\n not saved ").status)
        // the phone's own save no longer stands (slice W review): the head reads S5's stamp, and "nothing saved yet"
        // when S5 never stored a list (wishlist_at ''), never "saved on this phone" over the rows just taken from S5
        assertEquals("", after.savedAt)
        assertEquals("on S5 since 28 Oct, 07:30 IST", WishList.savedText(after))
        val neverOnS5 = WishList.afterFinal(mine, WishList.forPut(emptyMap()), "", s5Final)
        assertEquals("", neverOnS5.savedAt)
        assertEquals(0, WishList.count(neverOnS5.rows))
        assertEquals("nothing saved yet", WishList.savedText(neverOnS5))
        // the screen draws it under the banner at Client approved: S5's 409, beside the lock line, in the table's words
        val banners = WishList.banners(Tx.EN, 5, warned = false)
        assertEquals(s5Final, WishList.statusLine(Tx.EN, after, banners, 5))
        assertEquals("मंजूर झाले — पुढचे बदल \"बदल विनंती\" मधूनच होतात; सेव्ह झाले नाही",
            WishList.statusLine(Tx.MR, after, WishList.banners(Tx.MR, 5, warned = false), 5))
    }

    @Test fun `the 409 at Survey - no list in it - the list waits dirty, the status says S5's words`() {
        val mine = WishList.edited(Local("C", "T", at = "2026-10-15T10:00:00Z"), mapOf("cctv" to Row(on = true)), "2026-10-16T05:00:00Z")
        val after = WishList.afterSurvey(mine, s5Survey)
        assertTrue(after.dirty)
        assertEquals(mine.rows, after.rows)
        assertEquals(mine.at, after.at)
        assertEquals("", after.note)
        assertEquals(s5Survey, after.status)
        // the cache read Survey too: the banner already says it word for word, so it is not drawn twice
        assertEquals(null, WishList.statusLine(Tx.EN, after, WishList.banners(Tx.EN, 0, warned = false), 0))
        // in Marathi too, under the banner still drawn in English until M4 keys it (no line said twice in two languages)
        assertEquals(null, WishList.statusLine(Tx.MR, after, WishList.banners(Tx.MR, 0, warned = false), 0))
        // the cache past Survey: the words are no longer true, so nothing is drawn (slice W fix review; it was drawn here)
        assertEquals(null, WishList.statusLine(Tx.EN, after, WishList.banners(Tx.EN, 1, warned = false), 1))
        // the stage unread: S5's latest word on it, drawn
        assertEquals("आधी सर्व्हे पूर्ण करा — S5 वर टप्पा \"ग्राहकाची इच्छा-यादी\" वर जातो",
            WishList.statusLine(Tx.MR, after, emptyList(), null))
        // words S5 sends that are no key's English are shown as sent
        assertEquals("Some other refusal", WishList.statusLine(Tx.MR, WishList.afterSurvey(mine, "Some other refusal"), emptyList(), null))
        // when the stage moves, the next PUT's 200 takes the list and the status goes; so does an edit
        assertEquals("", WishList.afterPut(after, after.rows, "2026-10-17T05:00:00.000Z").status)
        assertFalse(WishList.afterPut(after, after.rows, "2026-10-17T05:00:00.000Z").dirty)
        assertEquals("", WishList.edited(after, after.rows, "2026-10-17T06:00:00Z").status)
        // and a later stale-base 409 or a refusal for good replaces it with their own note
        assertEquals("", WishList.afterConflict(after, mapOf("painting" to Row(on = true)), "z").status)
        assertEquals("", WishList.afterRefused(after, after.rows, "Only MCFT staff can change the wish list").status)
        // nothing to draw without a status
        assertEquals(null, WishList.statusLine(Tx.EN, mine, emptyList(), null))
        assertEquals(null, WishList.statusLine(Tx.EN, null, emptyList(), null))
    }

    @Test fun `the status line belongs to the visit - leaving the list clears it, the note keeps its own rule (critic X6)`() {
        val approved = WishList.afterFinal(Local("C", "T", dirty = true), WishList.forPut(emptyMap()), "a", s5Final)
        val left = WishList.left(approved)
        assertEquals("", left.status)
        assertEquals(approved.rows, left.rows)
        assertEquals(approved.at, left.at)
        assertFalse(left.dirty)
        // the next visit shows no "not saved" under the lock line
        assertEquals(null, WishList.statusLine(Tx.EN, left, WishList.banners(Tx.EN, 5, warned = false), 5))
        // REPLACED keeps today's rule: only an edit clears it, never a leave
        val replaced = WishList.afterConflict(Local("C", "T", mapOf("cctv" to Row(on = true)), dirty = true),
            mapOf("painting" to Row(on = true)), "b")
        assertEquals(WishList.REPLACED, replaced.note)
        assertEquals(replaced, WishList.left(replaced))
        // a list with nothing to clear is left as it was
        val plain = Local("C", "T", mapOf("cctv" to Row(on = true)))
        assertTrue(WishList.left(plain) === plain)
    }

    @Test fun `a 409 is told apart in one order - final before the rows, which the final answer carries too (0_3_262's defect)`() {
        val mine = WishList.edited(Local("C", "T", at = "2026-10-27T10:00:00.000Z"),
            mapOf("cctv" to Row(on = true), "painting" to Row(on = true, note = "two coats")), "2026-10-28T05:00:00Z")
        val s5 = WishList.forPut(mapOf("painting" to Row(on = true, rooms = listOf("Kitchen"))))
        assertEquals(WishList.Kind409.FINAL, WishList.kind409(final = true, hasRows = true))
        assertEquals(WishList.Kind409.FINAL, WishList.kind409(final = true, hasRows = false))
        assertEquals(WishList.Kind409.STALE, WishList.kind409(final = false, hasRows = true))
        assertEquals(WishList.Kind409.SURVEY, WishList.kind409(final = false, hasRows = false))
        // the final answer WITH rows: S5's rows, not dirty, S5's words on the status line, never REPLACED
        val fin = WishList.after409(mine, final = true, s5Rows = s5, at = "2026-10-28T02:00:00.000Z", error = s5Final)
        assertEquals(WishList.afterFinal(mine, s5, "2026-10-28T02:00:00.000Z", s5Final), fin)
        assertEquals(s5, fin.rows)
        assertFalse(fin.dirty)
        assertEquals(s5Final, fin.status)
        assertEquals("", fin.note)
        assertFalse(fin.note == WishList.REPLACED)
        // read as a stale base -- the rows looked at first -- it is REPLACED: the order is what this pins
        assertEquals(WishList.REPLACED, WishList.afterConflict(mine, s5, "2026-10-28T02:00:00.000Z").note)
        // a final answer without rows keeps the rows shown and S5's stamp, else the stamp it had
        assertEquals(mine.rows, WishList.after409(mine, true, null, "s", s5Final).rows)
        assertEquals(mine.at, WishList.after409(mine, true, s5, null, s5Final).at)
        // the stale base: S5 WINS (decision 2); a missing stamp reads as none
        val stale = WishList.after409(mine, final = false, s5Rows = s5, at = null, error = "Changed by staff meanwhile")
        assertEquals(WishList.afterConflict(mine, s5, ""), stale)
        assertEquals(WishList.REPLACED, stale.note)
        // no list (Survey, or an answer that did not parse): the list waits, dirty, in S5's words
        val survey = WishList.after409(mine, final = false, s5Rows = null, at = null, error = s5Survey)
        assertEquals(WishList.afterSurvey(mine, s5Survey), survey)
        assertTrue(survey.dirty)
        assertEquals(mine.rows, survey.rows)
        // the two refusals for the stage name it; the stale base does not refuse for the stage
        assertEquals(listOf(true, false, true), WishList.Kind409.entries.map { it.forStage })
    }

    @Test fun `a final answer while the cached stage is Revisions locks the list now - S5's stage is the phone's copy`() {
        // the sync read Revisions, then the PM approved before this sync's PUT (or the read failed and kept Revisions)
        val revisions = cache("Revisions")
        assertTrue(WishList.editable(WishList.stageIndex(revisions)))
        val named = QuoteStage.named(revisions, "Client approved", "2026-10-28T03:00:00Z")!!
        val i = WishList.stageIndex(named)
        assertEquals(WishList.APPROVED, i)
        assertFalse(WishList.editable(i))
        assertFalse(WishList.vendorUi(i))
        assertFalse(WishList.warnAfterQuote(i))
        assertEquals("Approved — changes go through change requests", WishList.lockedLine(Tx.EN, i))
        // so the screen reads the lock line with S5's 409 under it, and no field to type in
        val after = WishList.after409(WishList.edited(Local("C", "T", at = "a"), mapOf("cctv" to Row(on = true)), "b"),
            final = true, s5Rows = WishList.forPut(emptyMap()), at = "c", error = s5Final)
        val banners = WishList.banners(Tx.EN, i, warned = true)
        assertEquals(listOf("Approved — changes go through change requests"), banners)
        assertEquals(s5Final, WishList.statusLine(Tx.EN, after, banners, i))
        // and the 409 at Survey, read while the cache said Client wish list: read-only under Finish the survey first,
        // the status line not drawn twice
        val surveyed = QuoteStage.named(cache("Client wish list"), "Survey", "2026-10-28T03:00:00Z")!!
        assertEquals(0, WishList.stageIndex(surveyed))
        assertFalse(WishList.editable(0))
        assertEquals(null, WishList.statusLine(Tx.EN, WishList.afterSurvey(Local("C", "T", dirty = true), s5Survey),
            WishList.banners(Tx.EN, 0, warned = false), 0))
    }

    @Test fun `a status line already drawn does not come back on a visit that began without a leave (rotation, process death)`() {
        // the refusal landed while the list was closed: unseen, so the next visit shows it
        val refused = WishList.afterFinal(Local("C", "T", dirty = true), WishList.forPut(emptyMap()), "a", s5Final)
        assertFalse(refused.statusSeen)
        assertTrue(WishList.entered(refused) === refused)
        val banners = WishList.banners(Tx.EN, 5, warned = false)
        assertEquals(s5Final, WishList.statusLine(Tx.EN, WishList.entered(refused), banners, 5))
        // drawn: marked seen, once
        val seen = WishList.shown(refused)
        assertTrue(seen.statusSeen)
        assertEquals(s5Final, seen.status)
        assertTrue(WishList.shown(seen) === seen)
        // the visit ends with no leave (the activity recreated); the next one begins without the line
        val next = WishList.entered(seen)
        assertEquals("", next.status)
        assertFalse(next.statusSeen)
        assertEquals(null, WishList.statusLine(Tx.EN, next, banners, 5))
        assertEquals(seen.rows, next.rows)
        assertEquals(seen.at, next.at)
        // a leave clears it whether drawn or not
        assertEquals(Local("C", "T", rows = seen.rows, at = "a"), WishList.left(seen))
        assertEquals("", WishList.left(refused).status)
        // a NEW refusal after the line was seen starts unseen again, and so shows on the next visit
        val again = WishList.afterSurvey(seen, s5Survey)
        assertFalse(again.statusSeen)
        assertEquals(s5Survey, WishList.entered(again).status)
        // nothing to draw: shown changes nothing
        val plain = Local("C", "T")
        assertTrue(WishList.shown(plain) === plain)
        // every answer and edit that clears the status clears the mark with it
        for (l in listOf(WishList.edited(seen, seen.rows, "t"), WishList.afterPut(seen, seen.rows, "t"),
                         WishList.afterConflict(seen, mapOf("cctv" to Row(on = true)), "t"),
                         WishList.afterRefused(seen, seen.rows, "x"))) {
            assertEquals("", l.status); assertFalse(l.statusSeen)
        }
    }

    @Test fun `no REPLACED or NOT_SENT note under the lock at Client approved, including one 0_3_262 wrote`() {
        // 0.3.262 met S5's final 409 as a stale base: REPLACED, not dirty; the new build then opens and leaves it
        val old = WishList.afterConflict(Local("C", "T", mapOf("cctv" to Row(on = true)), dirty = true),
            WishList.forPut(mapOf("painting" to Row(on = true))), "2026-10-28T02:42:00.000Z")
        assertEquals(WishList.REPLACED, old.note)
        assertFalse(old.dirty)
        val visited = WishList.afterGet(WishList.left(WishList.entered(old)), old.rows, old.at)
        assertEquals(WishList.REPLACED, visited.note)   // kept as stored: not the screen's to rewrite
        assertFalse(WishList.editable(WishList.APPROVED))
        assertEquals(null, WishList.noteLine(visited, WishList.APPROVED))
        assertEquals(null, WishList.noteLine(old, WishList.APPROVED))
        // NOT_SENT: S5 checks a 400's shape before the final check
        val notSent = WishList.afterRefused(WishList.edited(Local("C", "T"), mapOf("cctv" to Row(on = true)), "t"),
            mapOf("cctv" to Row(on = true)), "Send JSON")
        assertEquals("Not sent to S5 — Send JSON", notSent.note)
        assertEquals(null, WishList.noteLine(notSent, WishList.APPROVED))
        // everywhere else the note shows as before, the stage unread included
        for (i in listOf(null, 0, 1, 2, 3, 4)) {
            assertEquals(WishList.REPLACED, WishList.noteLine(old, i), "noteLine($i)")
            assertEquals("Not sent to S5 — Send JSON", WishList.noteLine(notSent, i), "noteLine($i)")
        }
        assertEquals(null, WishList.noteLine(Local("C", "T"), 2))
        assertEquals(null, WishList.noteLine(null, 2))
    }

    @Test fun `a stage refusal is drawn only while that stage holds - no Survey words at Client wish list, no Approved ones off Client approved`() {
        // FX16 offline: ticks made with the stage unread, the PUT meets S5's Survey 409, the phone's copy of the stage takes Survey
        val mine = WishList.edited(Local("C", "T"), mapOf("cctv" to Row(on = true)), "2026-10-28T05:00:00Z")
        assertTrue(WishList.editable(null))
        val refused = WishList.after409(mine, final = false, s5Rows = null, at = null, error = s5Survey)
        assertEquals(0, WishList.stageIndex(QuoteStage.named(null, "Survey", "2026-10-28T05:10:00Z")))
        assertFalse(WishList.chipShown(0))   // the list cannot be opened while the words are true
        // staff move the stage on; the project open reads Client wish list, the chip is back, the visit begins
        val moved = WishList.stageIndex(cache("Client wish list"))
        assertEquals(1, moved)
        assertTrue(WishList.chipShown(moved) && WishList.editable(moved))
        val visit = WishList.entered(refused)
        assertEquals(s5Survey, visit.status)   // unseen, so kept for this visit, as S5 sent it
        assertEquals(emptyList(), WishList.banners(Tx.EN, moved, warned = false))
        assertEquals(null, WishList.statusLine(Tx.EN, visit, emptyList(), moved))
        assertEquals(null, WishList.statusLine(Tx.MR, visit, emptyList(), moved))
        // the Survey words: drawn only with the stage unread (at Survey the banner already says them)
        for (i in 1..5) assertEquals(null,
            WishList.statusLine(Tx.EN, refused, WishList.banners(Tx.EN, i, warned = false), i), "Survey words at $i")
        assertEquals(s5Survey, WishList.statusLine(Tx.EN, refused, WishList.banners(Tx.EN, null, warned = false), null))
        // the Approved words: drawn only at Client approved and unread -- a D1 fix that takes a project back from
        // Client approved (PROJ-0025, 28 Oct) draws no "not saved" over an editable list
        val approved = WishList.after409(mine, final = true, s5Rows = WishList.forPut(emptyMap()), at = "", error = s5Final)
        for (i in 0..4) assertEquals(null,
            WishList.statusLine(Tx.EN, approved, WishList.banners(Tx.EN, i, warned = false), i), "Approved words at $i")
        assertEquals(s5Final, WishList.statusLine(Tx.EN, approved, WishList.banners(Tx.EN, 5, warned = false), 5))
        assertEquals(s5Final, WishList.statusLine(Tx.EN, approved, WishList.banners(Tx.EN, null, warned = false), null))
        // the stored words stay as S5 sent them: the stage decides only whether they are drawn
        assertEquals(s5Final, approved.status)
        // words that are no key's English are tied to no stage
        val other = WishList.afterSurvey(mine, "S5 answered 409")
        for (i in listOf(null, 0, 1, 2, 3, 4, 5))
            assertEquals("S5 answered 409", WishList.statusLine(Tx.EN, other, emptyList(), i), "other words at $i")
    }

    @Test fun `a stale phone-only project reads the stage under the id its list moved to, so a final 409 locks it`() {
        val localKey = WishList.localKey("C", "T")
        val store = linkedMapOf<String, String>()
        val lookup: (String) -> String? = { store[it] }
        // only on this phone, never moved: no stage to read, and the screen says "not on S5"
        assertEquals(null, WishList.stageKey(localKey, lookup))
        assertTrue(WishList.notOnS5(WishList.stageKey(localKey, lookup) != null, null))
        // a synced project reads its own id
        assertEquals("PROJ-0042", WishList.stageKey(WishList.keyOf(" PROJ-0042 ", "C", "T"), lookup))
        // the sync re-keyed the list (WishSync.rekey leaves the forward) while the screen held the phone-only project
        store[WishList.forwardKey(localKey)] = "PROJ-0042"
        val k = WishList.stageKey(localKey, lookup)
        assertEquals("PROJ-0042", k)
        // the PUT met S5's final 409, and QuoteStageSync.named wrote Client approved under the id: the screen reads it
        val named = QuoteStage.named(null, "Client approved", "2026-10-28T03:00:00Z")
        val i = WishList.stageIndex(named)
        assertEquals(WishList.APPROVED, i)
        assertFalse(WishList.notOnS5(k != null, named))
        assertFalse(WishList.editable(i))
        assertTrue(WishList.chipShown(i))
        assertEquals(listOf("Approved — changes go through change requests"),
            WishList.banners(Tx.EN, i, warned = false, notOnS5 = false))
        assertEquals("QUOTE STAGE · Client approved",
            QuoteStage.lineFor(named, java.time.Instant.parse("2026-10-28T04:00:00Z")).title)
        // what the stale screen drew before: no stage, so editable, with no banner and no lock
        assertTrue(WishList.editable(WishList.stageIndex(null)))
        assertEquals(emptyList(), WishList.banners(Tx.EN, null, warned = false, notOnS5 = true))
        // a blank forward is no forward
        store[WishList.forwardKey(localKey)] = "  "
        assertEquals(null, WishList.stageKey(localKey, lookup))
    }

    @Test fun `the Wish list chip is hidden at Survey only, as the prototype's CSS hides it (index61c1 1255)`() {
        assertEquals(listOf(true, false, true, true, true, true, true),
            listOf(null, 0, 1, 2, 3, 4, 5).map { WishList.chipShown(it) })
        // an unread stage shows it: never read, not listed, a name outside the six
        for (c in listOf(null, QuoteStage.Cache(listed = false), QuoteStage.Cache(listed = true, stage = "Quoted")))
            assertTrue(WishList.chipShown(WishList.stageIndex(c)), "$c")
        assertFalse(WishList.chipShown(WishList.stageIndex(cache("Survey"))))
        assertTrue(WishList.chipShown(WishList.stageIndex(cache("Client approved"))))
    }

    @Test fun `the head - nothing saved, saved on this phone, on S5 since in IST`() {
        assertEquals("nothing saved yet", WishList.savedText(null))
        assertEquals("nothing saved yet", WishList.savedText(Local("C", "T")))
        assertEquals("saved on this phone", WishList.savedText(Local("C", "T", dirty = true, at = "2026-10-14T02:38:00.000Z")))
        assertEquals("on S5 since 14 Oct, 08:08 IST", WishList.savedText(Local("C", "T", at = "2026-10-14T02:38:00.000Z")))
        assertEquals("saved on this phone", WishList.savedText(Local("C", "T", savedAt = "2026-10-14T02:38:00Z")))
    }
}
