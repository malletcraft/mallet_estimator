package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.WishList.Local
import com.malletcrafts.interiorestimate.pano.WishList.Row
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The wish list on the phone (v52 item 16, slice 5b; decisions 2, 3 and 8 of 15 Oct 2026),
 * against the prototype's texts and rules (wish.js; ct52 W, W2, FX16) and S5's services.
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

    @Test fun `stage rules - read-only at Survey, editable at Client wish list and Vendor quoted, locked from Estimated on (v61b)`() {
        val idx = QuoteStage.STAGES.map { WishList.stageIndex(cache(it)) }
        assertEquals(listOf(0, 1, 2, 3, 4, 5), idx)
        assertEquals(listOf(false, true, true, false, false, false), idx.map { WishList.editable(it) })
        assertEquals(listOf(false, false, true, false, false, false), idx.map { WishList.warnAfterQuote(it) })
        assertEquals(listOf(WishList.FINISH_SURVEY), WishList.banners(0, warned = true))
        assertEquals(emptyList(), WishList.banners(1, warned = true))
        assertEquals(emptyList(), WishList.banners(2, warned = false))
        assertEquals(listOf("Changing the wish list after a vendor quoted — tell the vendor"), WishList.banners(2, warned = true))
        assertEquals("Finish the survey first — the stage moves to Client wish list on S5", WishList.FINISH_SURVEY)
        // v61b: the prototype's refuse.readonly.wishlist line at Estimated, and the same line named for the two stages after it
        assertEquals(listOf("Estimation — the wish list is read-only; move the phase back on the project screen to change it"), WishList.banners(3, warned = true))
        assertEquals(listOf("Revisions — the wish list is read-only; move the phase back on the project screen to change it"), WishList.banners(4, warned = false))
        assertEquals(listOf("Client approved — the wish list is read-only; move the phase back on the project screen to change it"), WishList.banners(5, warned = true))
        assertEquals(null, WishList.lockedLine(2)); assertEquals(null, WishList.lockedLine(null))
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
                WishList.banners(i, warned = true, notOnS5 = WishList.notOnS5(true, c)))
        }
    }

    @Test fun `not on S5 - phone-only or not listed - editable, and no 'S5 decides when online' under 'not on S5 yet'`() {
        // A phone-only project never asks S5; a not-listed one is 404 until its first room upload (decision 3).
        for ((synced, c) in listOf(false to null, true to QuoteStage.Cache(listed = false),
                                   false to QuoteStage.Cache(listed = true, stage = "Survey"))) {
            assertTrue(WishList.notOnS5(synced, c), "$synced $c")
            val i = if (synced) WishList.stageIndex(c) else null
            assertTrue(WishList.editable(i))
            assertEquals(emptyList(), WishList.banners(i, warned = true, notOnS5 = true))
        }
        // and the sub-line those projects show says the same thing
        assertEquals("QUOTE STAGE · not on S5 yet", QuoteStage.phoneOnly().title)
        assertEquals("QUOTE STAGE · not on S5 yet",
            QuoteStage.lineFor(QuoteStage.Cache(listed = false), java.time.Instant.parse("2026-10-16T05:00:00Z")).title)
    }

    @Test fun `applies-to line and the row's second line (ct52 W(b))`() {
        val svc = WishList.SERVICES[0]
        assertEquals("Whole flat", WishList.subLine(Row(on = true)))
        assertEquals("Whole flat", WishList.rowLine(svc, Row(on = true)))
        assertEquals("not asked for", WishList.rowLine(svc, Row()))
        assertEquals("safety door, aluminium main door, UPVC", WishList.rowLine(WishList.SERVICES[4], Row()))
        val grid = listOf("Living Room", "Kitchen", "Master Bedroom")
        var rows = WishList.toggle(emptyMap(), "electrical")
        rows = WishList.toggleRoom(rows, "electrical", "Kitchen", grid)
        rows = WishList.toggleRoom(rows, "electrical", "Living Room", grid)
        // kept in the grid's order whatever order they were pressed in
        assertEquals(listOf("Living Room", "Kitchen"), WishList.row(rows, "electrical").rooms)
        rows = WishList.setNote(rows, "electrical", "more\n  points")
        assertEquals("Living Room · Kitchen · more points", WishList.rowLine(svc, WishList.row(rows, "electrical")))
        rows = WishList.setVendor(rows, "electrical", "V1")
        assertEquals("Living Room · Kitchen · more points · vendor V1", WishList.rowLine(svc, WishList.row(rows, "electrical")))
        rows = WishList.toggleRoom(rows, "electrical", "Living Room", grid)
        assertEquals(listOf("Kitchen"), WishList.row(rows, "electrical").rooms)
        rows = WishList.wholeFlat(rows, "electrical")
        assertEquals("Whole flat", WishList.subLine(WishList.row(rows, "electrical")))
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

    @Test fun `the head - nothing saved, saved on this phone, on S5 since in IST`() {
        assertEquals("nothing saved yet", WishList.savedText(null))
        assertEquals("nothing saved yet", WishList.savedText(Local("C", "T")))
        assertEquals("saved on this phone", WishList.savedText(Local("C", "T", dirty = true, at = "2026-10-14T02:38:00.000Z")))
        assertEquals("on S5 since 14 Oct, 08:08 IST", WishList.savedText(Local("C", "T", at = "2026-10-14T02:38:00.000Z")))
        assertEquals("saved on this phone", WishList.savedText(Local("C", "T", savedAt = "2026-10-14T02:38:00Z")))
    }
}
