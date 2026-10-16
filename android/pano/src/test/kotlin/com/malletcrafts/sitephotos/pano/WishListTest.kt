package com.malletcrafts.sitephotos.pano

import com.malletcrafts.sitephotos.pano.WishList.Local
import com.malletcrafts.sitephotos.pano.WishList.Row
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

    @Test fun `stage rules - read-only at Survey, editable from Client wish list on, warn from Vendor quoted on`() {
        val idx = QuoteStage.STAGES.map { WishList.stageIndex(cache(it)) }
        assertEquals(listOf(0, 1, 2, 3, 4), idx)
        assertEquals(listOf(false, true, true, true, true), idx.map { WishList.editable(it) })
        assertEquals(listOf(false, false, true, true, true), idx.map { WishList.warnAfterQuote(it) })
        assertEquals(listOf(WishList.FINISH_SURVEY), WishList.banners(0, warned = true))
        assertEquals(emptyList(), WishList.banners(1, warned = true))
        assertEquals(emptyList(), WishList.banners(2, warned = false))
        assertEquals(listOf("Changing the wish list after a vendor quoted — tell the vendor"), WishList.banners(2, warned = true))
        assertEquals("Finish the survey first — the stage moves to Client wish list on S5", WishList.FINISH_SURVEY)
    }

    @Test fun `stage not read - editable, says so, never Finish the survey first (FX16)`() {
        for (c in listOf(null, QuoteStage.Cache(listed = false), QuoteStage.Cache(listed = true, stage = ""),
                         QuoteStage.Cache(listed = true, stage = "Quoted"))) {
            val i = WishList.stageIndex(c)
            assertEquals(null, i, "$c")
            assertTrue(WishList.editable(i))
            assertFalse(WishList.warnAfterQuote(i))
            assertEquals(listOf("Quote stage not read yet — S5 decides when online"), WishList.banners(i, warned = true))
        }
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

    @Test fun `adopt - the phone's list moves under the id and stays dirty, only an unsent list there beats it`() {
        val local = Local("C", "T", mapOf("cctv" to Row(on = true)), dirty = true, savedAt = "2026-10-16T05:00:00Z")
        val moved = WishList.adopt(local, null)
        assertEquals("", moved.at)
        assertTrue(moved.dirty)
        assertEquals(1, WishList.count(moved.rows))
        val fetched = Local("C", "T", mapOf("painting" to Row(on = true)), at = "2026-10-15T10:00:00Z")
        val onTop = WishList.adopt(local, fetched)
        assertEquals("2026-10-15T10:00:00Z", onTop.at)
        assertEquals(local.rows, onTop.rows)
        val unsent = fetched.copy(dirty = true)
        assertEquals(unsent, WishList.adopt(local, unsent))
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

    @Test fun `200 - S5's stamp is the base, an edit made while the PUT was out stays dirty`() {
        val sent = mapOf("cctv" to Row(on = true, note = "x "))
        val l = Local("C", "T", sent, dirty = true)
        val s5 = WishList.forPut(mapOf("cctv" to Row(on = true, note = "x")))
        val ok = WishList.afterPut(l, sent, s5, "2026-10-16T05:00:01Z")
        assertFalse(ok.dirty)
        assertEquals("2026-10-16T05:00:01Z", ok.at)
        assertEquals("x", WishList.row(ok.rows, "cctv").note)
        val moved = l.copy(rows = WishList.toggle(sent, "painting"))
        val later = WishList.afterPut(moved, sent, s5, "2026-10-16T05:00:01Z")
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

    @Test fun `the head - nothing saved, saved on this phone, on S5 since in IST`() {
        assertEquals("nothing saved yet", WishList.savedText(null))
        assertEquals("nothing saved yet", WishList.savedText(Local("C", "T")))
        assertEquals("saved on this phone", WishList.savedText(Local("C", "T", dirty = true, at = "2026-10-14T02:38:00.000Z")))
        assertEquals("on S5 since 14 Oct, 08:08 IST", WishList.savedText(Local("C", "T", at = "2026-10-14T02:38:00.000Z")))
        assertEquals("saved on this phone", WishList.savedText(Local("C", "T", savedAt = "2026-10-14T02:38:00Z")))
    }
}
