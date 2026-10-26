package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Album.Answer
import com.malletcrafts.interiorestimate.pano.Album.Entry
import com.malletcrafts.interiorestimate.pano.Album.Op
import com.malletcrafts.interiorestimate.pano.Album.Outbox
import com.malletcrafts.interiorestimate.pano.Album.Photo
import com.malletcrafts.interiorestimate.pano.Album.Verdict
import com.malletcrafts.interiorestimate.pano.Album.View
import com.malletcrafts.interiorestimate.pano.Album.Viewer
import com.malletcrafts.interiorestimate.pano.Album.Work
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The album by works on the phone (v61b slice 2, K1), against prototype v61b's ALB60 core and
 * its checks (ct61b D1-D3 the exec lock, E7-E8 the change requests), S5's WORKS and the
 * verdict table of DESIGN-slice2.md "Offline and conflicts". Every name here is invented.
 */
class AlbumTest {

    private val pm = Viewer(Album.PM)
    private val designer = Viewer(Album.DESIGNER)
    private val client = Viewer(Album.CLIENT)
    // a vendor doing Waterproofing (50) and Plumbing (30), as two Supplier rows
    private val vendor = Viewer(Album.VENDOR, mine = setOf(50, 30), suppliers = setOf("demo waterproofing co.", "demo plumbing works"))

    private fun state(seq: Long, work: Int, value: String, prev: String = Album.NOT_STARTED, note: String = "", at: String = "2026-10-0${seq}T05:00:00.000Z") =
        Entry(seq, "ALB-%012x".format(seq), Album.KIND_STATE, work, value, prev, note, Album.PM, "pm@x", "P M", at)
    private fun cr(seq: Long, work: Int, kind: String, note: String = "", at: String = "2026-10-1${seq % 10}T05:00:00.000Z") =
        Entry(seq, "ALB-%012x".format(seq), kind, work, note = note, role = Album.PM, by = "pm@x", byName = "P M", at = at)
    private fun stage(seq: Long, value: String, at: String) = Entry(seq, "ALB-%012x".format(seq), Album.KIND_STAGE, null, value, at = at)
    private fun staff(id: String, work: Int?, at: String = "2026-10-10T05:00:00.000Z") = Photo(id, work, at, Album.PARTY_STAFF, by = "d@x")
    private fun vend(id: String, work: Int?, vendor: String, review: String = Album.REVIEW_PENDING, at: String = "2026-10-11T05:00:00.000Z") =
        Photo(id, work, at, Album.PARTY_VENDOR, vendor = vendor, review = review, by = "v@x")

    // ---- vocabulary ---------------------------------------------------------

    @Test fun `the sixteen works are S5's PHASES, ids and names in S5's order`() {
        // A copy of app1_data.js PHASES (S5 QUOTE @ ca74117) = asis.js WORKS. Change it here only with S5.
        val ids = listOf(10, 20, 30, 40, 50, 60, 70, 80, 85, 90, 95, 100, 110, 120, 130, 140)
        val names = listOf("Demolition & site preparation", "Civil & masonry", "Plumbing, concealed", "Electrical, concealed wiring",
            "Waterproofing", "False ceiling", "Flooring & wall tiling", "Countertops", "Iron & metal fabrication",
            "Modular furniture, factory-made", "Onsite carpentry & panelling", "Painting & wall finishes",
            "Sanitaryware & CP fittings", "Lights, fans, fixtures & security", "Glass, mirrors & furnishing", "Cleaning & handover")
        assertEquals(ids, Album.WORK_IDS)
        assertEquals(names, Album.WORKS.map { it.name })
        assertEquals(16, Album.WORKS.size)
        assertEquals(140, Album.LAST_WORK)
        assertTrue(Album.isWork(85))
        assertFalse(Album.isWork(0))
        assertFalse(Album.isWork(null))
        assertFalse(Album.isWork(45))
    }

    @Test fun `the five states, the four roles and their lines, in the prototype's order`() {
        assertEquals(listOf("Not started", "In progress", "On hold", "Done", "Abandoned"), Album.STATES)
        assertEquals(listOf("pm", "designer", "vendor", "client"), Album.ROLES.map { it.id })
        assertEquals(listOf("Project Manager", "Designer", "Vendor", "Client"), Album.ROLES.map { it.label })
        assertEquals(Album.ROLE_PM_LINE, Album.ROLES[0].line)
        assertEquals("sees everything · sets work states · accepts vendor photos", Album.ROLES[0].line)
        assertEquals("sees everything · takes progress shots · changes no state", Album.ROLES[1].line)
        assertEquals("sees only their own work · uploads photos for review", Album.ROLES[2].line)
        assertEquals("sees accepted photos and work states only", Album.ROLES[3].line)
        assertEquals("Not started", Album.stateName(null))
        assertEquals("Not started", Album.stateName("Finished"))
        assertEquals("On hold", Album.stateName("On hold"))
    }

    @Test fun `PAY is S5's PAY_TERMS as distinct kind-at pairs, worded by paySchedule's rule`() {
        assertEquals(16, Album.PAY.size)
        assertEquals(listOf("Before Demolition & site preparation starts", "When Demolition & site preparation is done"), Album.payOf(10))
        // 40's two 's' terms (slab and wall piping) are one "Before ... starts"; the plates ride Lights (120)
        assertEquals(listOf("Before Electrical, concealed wiring starts", "When Electrical, concealed wiring is done",
            "Before Lights, fans, fixtures & security starts"), Album.payOf(40))
        assertEquals(listOf("When Cleaning & handover is done"), Album.payOf(140))
        assertEquals(listOf("Before Waterproofing starts", "When Waterproofing is done", "When Cleaning & handover is done"), Album.payOf(50))
        assertEquals(emptyList(), Album.payOf(0))
        assertEquals(emptyList(), Album.payOf(45))
    }

    @Test fun `STAGE_WORK files the 39 worksite stages - unknown to the quote journey, a null mapping unfiled`() {
        assertEquals(39, Album.STAGE_WORK.size)
        assertEquals(0, Album.workOfStage("As-built site measure"))
        assertEquals(20, Album.workOfStage("Plaster & waterproofing"))
        assertEquals(140, Album.workOfStage("Handover"))
        assertEquals(140, Album.workOfStage("Snagging"))
        assertEquals(85, Album.workOfStage("Window jali, mesh & grill"))
        // exact, then case-insensitive (the prototype's workOfStage)
        assertEquals(70, Album.workOfStage("floor TILING"))
        // a delivery photo is unfiled: only a human can say which work was delivered
        assertNull(Album.workOfStage("Material delivered on site"))
        // not one of the 39, blank or null: the quote journey, as S5 files it
        assertEquals(0, Album.workOfStage("Something else"))
        assertEquals(0, Album.workOfStage(""))
        assertEquals(0, Album.workOfStage(null))
        assertTrue(Album.STAGE_WORK.values.filterNotNull().all { it == 0 || Album.isWork(it) })
    }

    @Test fun `chapter numbers - 0 the quote journey, index plus one for a work, null unfiled`() {
        assertEquals(0, Album.chapterOf(0))
        assertEquals(1, Album.chapterOf(10))
        assertEquals(10, Album.chapterOf(90))
        assertEquals(16, Album.chapterOf(140))
        assertNull(Album.chapterOf(null))
        assertNull(Album.chapterOf(45))
        assertEquals("Quote journey", Album.workName(0))
        assertEquals("not filed to a work", Album.workName(null))
        assertEquals("Modular furniture, factory-made", Album.workName(90))
        assertEquals("", Album.workName(45))
    }

    @Test fun `the lock banner is verbatim and the stale camera string is not ported`() {
        assertEquals("Work starts once the client approves the estimate", Album.EXEC_LOCK_BANNER)
        assertEquals("Only the Project Manager sets a work's state", Album.STATE_PM_ONLY)
        assertEquals("A reason is needed to change a Done work", Album.DONE_REASON_NEED)
        assertEquals("Album closed · Cleaning & handover is Done", Album.CLOSED_CLEANING)
        assertEquals("The album closes when Cleaning & handover is Done", Album.CLOSES_CLEANING)
        assertEquals("No photos yet — the survey's 360s come first", Album.EMPTY)
        assertEquals("Your photos show to the client once the Project Manager accepts them", Album.VENDOR_NOTE)
        assertEquals("Change requests: 3", Album.changeRequestsX(3))
        assertEquals("Change request · raised 20 Oct by", Album.crRaisedX("20 Oct"))
        assertEquals("To review: 1", Album.toReviewX(1))
        assertEquals("Photo 2 of 7", Album.photoXOfX(2, 7))
        assertEquals("Album · Demo flat", Album.titleX("Demo flat"))
        // every string constant of the object: none says the camera's "locked until the quote is Estimated"
        val strings = Album::class.java.declaredFields.filter { it.type == String::class.java }
            .onEach { it.isAccessible = true }.map { it.get(null) as String }
        assertTrue(strings.size >= 90, "ported ${strings.size}")
        assertTrue(strings.none { it.contains("locked until the quote is Estimated") })
        assertTrue(strings.none { it.contains("locked until the estimate is approved") })
    }

    // ---- who the person is (decision 3: pm null means every staff member is PM) ----

    @Test fun `albumRole - staff is pm when no pm is named or it is this login, else designer`() {
        val works = mapOf("Demo Waterproofing Co." to listOf(50), "Demo Plumbing Works" to listOf(30, 999), "Other" to listOf(40))
        assertEquals(Viewer(Album.PM), Album.albumRole("staff", null, "pm1@x", emptyList(), works))
        assertEquals(Viewer(Album.PM), Album.albumRole("staff", "", "pm1@x", emptyList(), works))
        assertEquals(Viewer(Album.PM), Album.albumRole("staff", " PM1@X ", "pm1@x", emptyList(), works))
        assertEquals(Viewer(Album.DESIGNER), Album.albumRole("staff", "pm1@x", "des1@x", emptyList(), works))
        // a vendor: mine is the union over its suppliers, matched as partyKey; an id that is not a work is dropped
        val v = Album.albumRole("vendor", "pm1@x", "v@x", listOf(" demo waterproofing CO. ", "Demo Plumbing Works"), works)!!
        assertEquals(Album.VENDOR, v.role)
        assertEquals(setOf(50, 30), v.mine)
        assertEquals(setOf("demo waterproofing co.", "demo plumbing works"), v.suppliers)
        val none = Album.albumRole("vendor", null, "v@x", listOf("Nobody"), works)!!
        assertEquals(Album.VENDOR, none.role)
        assertEquals(emptySet(), none.mine, "a supplier with no works row: nothing is mine")
        assertEquals(setOf("nobody"), none.suppliers)
        assertEquals(Viewer(Album.CLIENT), Album.albumRole("client", null, "c@x", emptyList(), works))
        assertNull(Album.albumRole("none", null, "x@x", emptyList(), works))
        assertNull(Album.albumRole(null, null, "x@x", emptyList(), works))
        assertEquals("pm1@x", Album.partyKey(" PM1@X "))
        assertEquals("", Album.partyKey(null))
    }

    // ---- derivations from the log --------------------------------------------

    @Test fun `stateOf is the newest state row by seq, never by time - else Not started`() {
        val log = listOf(
            state(1, 10, "In progress", at = "2026-10-01T05:00:00.000Z"),
            state(3, 10, "Done", "In progress", "out", at = "2026-09-01T05:00:00.000Z"),   // older clock, newer seq: wins
            state(2, 50, "In progress"),
            state(4, 60, "Finished"),                                                        // not one of the five
            cr(5, 10, Album.KIND_CR_RAISE))                                                 // not a state row
        assertEquals("Done", Album.stateOf(log, 10))
        assertEquals("In progress", Album.stateOf(log, 50))
        assertEquals("Not started", Album.stateOf(log, 60))
        assertEquals("Not started", Album.stateOf(log, 140))
        assertEquals(3L, Album.entryOf(log, 10)!!.seq)
        assertNull(Album.entryOf(log, 140))
        assertEquals(listOf(50), Album.openWorks(log))
        assertEquals(emptyList(), Album.openWorks(emptyList()))
        assertTrue(Album.hasStateEntry(log))
        assertFalse(Album.hasStateEntry(listOf(cr(5, 10, Album.KIND_CR_RAISE))))
    }

    @Test fun `crOf is live only while the newest cr row is a raise - inScope with a null baseline is everything (E7)`() {
        val raised = listOf(cr(1, 90, Album.KIND_CR_RAISE, "Client added factory-made modular units after approval"))
        assertEquals("Client added factory-made modular units after approval", Album.crOf(raised, 90)!!.note)
        assertNull(Album.crOf(raised, 80))
        val cleared = raised + cr(2, 90, Album.KIND_CR_CLEAR)
        assertNull(Album.crOf(cleared, 90))
        val again = cleared + cr(3, 90, Album.KIND_CR_RAISE, "again")
        assertEquals(3L, Album.crOf(again, 90)!!.seq)
        assertTrue(Album.inScope(null, 90))
        assertTrue(Album.inScope(listOf(10, 90), 90))
        assertFalse(Album.inScope(listOf(10), 90))
        assertFalse(Album.inScope(emptyList(), 10))
    }

    @Test fun `Change requests counts off-baseline works with a live CR (E1, E3, E7)`() {
        // PROJ-DEMO's baseline: 12 of 16; 80, 85, 90, 95 off it; a CR on 90 only
        val scope = listOf(10, 20, 30, 40, 50, 60, 70, 100, 110, 120, 130, 140)
        val log = listOf(cr(1, 90, Album.KIND_CR_RAISE, "x"), cr(2, 10, Album.KIND_CR_RAISE, "in scope: never counted"))
        val crs = Album.changeRequests(scope, log)
        assertEquals(listOf(90), crs.map { it.id })
        assertEquals("Modular furniture, factory-made", crs[0].name)
        assertEquals("Change requests: 1", Album.changeRequestsX(crs.size))
        // E7: clearing 90 drops the count 1 -> 0
        assertEquals(0, Album.changeRequests(scope, log + cr(3, 90, Album.KIND_CR_CLEAR)).size)
        // no baseline: nothing can be off it
        assertEquals(0, Album.changeRequests(null, log).size)
        // re-approval with a wider scope drops a live CR on a now-in-scope work from the count (it stays in the log)
        assertEquals(0, Album.changeRequests(scope + 90, log).size)
    }

    @Test fun `exec - locked from Survey through Revisions (D1), released at Client approved (D2), recorded work stays open (D3)`() {
        for (q in listOf("Survey", "Client wish list", "Vendor quoted", "Estimated", "Revisions"))
            assertFalse(Album.exec(q, hasStateEntry = false), q)
        assertTrue(Album.exec("Client approved", hasStateEntry = false))
        assertTrue(Album.exec(" Client approved ", hasStateEntry = false))
        // D3: PROJ-DEMO at Survey with three state rows stays unlocked
        for (q in QuoteStage.STAGES) assertTrue(Album.exec(q, hasStateEntry = true), q)
        // the prototype's execMode: a stage the phone has not read, or one outside the six, is execution -- S5 decides on reconnect
        assertTrue(Album.exec(null, hasStateEntry = false))
        assertTrue(Album.exec("", hasStateEntry = false))
        assertTrue(Album.exec("Quoted", hasStateEntry = false))
    }

    @Test fun `closed when Cleaning and handover is Done - and only leaving Done needs a remark`() {
        assertFalse(Album.closed(emptyList()))
        val done = listOf(state(1, 140, "In progress"), state(2, 140, "Done", "In progress", at = "2026-12-20T05:00:00.000Z"))
        assertTrue(Album.closed(done))
        assertEquals("2026-12-20T05:00:00.000Z", Album.closedAt(done))
        assertNull(Album.closedAt(done.dropLast(1)))
        assertFalse(Album.closed(done + state(3, 140, "On hold", "Done", "snag")))
        assertTrue(Album.remarkNeeded("Done"))
        for (s in listOf("Not started", "In progress", "On hold", "Abandoned")) assertFalse(Album.remarkNeeded(s), s)
    }

    @Test fun `quoteAt - the newest stage row at or before the photo, else Survey`() {
        val stages = listOf(
            stage(1, "Client wish list", "2026-10-05T05:00:00.000Z"),
            stage(2, "Vendor quoted", "2026-10-09T05:00:00.000Z"),
            stage(3, "Not a stage", "2026-10-09T06:00:00.000Z"),
            stage(4, "Estimated", "2026-10-15T05:00:00.000Z"))
        assertEquals("Survey", Album.quoteAt(stages, "2026-10-01T05:00:00.000Z"))
        assertEquals("Client wish list", Album.quoteAt(stages, "2026-10-05T05:00:00.000Z"))
        assertEquals("Vendor quoted", Album.quoteAt(stages, "2026-10-10T05:00:00.000Z"))
        assertEquals("Estimated", Album.quoteAt(stages, "2026-10-20T05:00:00.000Z"))
        assertEquals("Survey", Album.quoteAt(emptyList(), "2026-10-20T05:00:00.000Z"))
    }

    // ---- canSee: the one rule ----------------------------------------------------

    @Test fun `canSee - PM and Designer see everything`() {
        val items = listOf(staff("a", 0), staff("b", null), staff("c", 10), vend("d", 50, "Demo Waterproofing Co."),
            vend("e", 30, "Other Co.", Album.REVIEW_INTERNAL), vend("f", null, "Other Co."))
        for (v in listOf(pm, designer)) assertEquals(items, Album.visible(v, items), v.role)
        assertEquals(listOf("d", "f"), Album.pending(items).map { it.id })
    }

    @Test fun `canSee - a vendor sees its own works only, its own uploads whatever the review, others' when accepted`() {
        assertFalse(Album.canSee(vendor, staff("q", 0)))
        assertFalse(Album.canSee(vendor, staff("u", null)))
        assertFalse(Album.canSee(vendor, staff("x", 40)))               // not mine
        assertTrue(Album.canSee(vendor, staff("s", 50)))                // staff photo in mine
        assertTrue(Album.canSee(vendor, vend("own", 50, "Demo Waterproofing Co.")))
        assertTrue(Album.canSee(vendor, vend("own2", 30, " DEMO plumbing works ", Album.REVIEW_INTERNAL)))   // partyKey match
        assertFalse(Album.canSee(vendor, vend("other", 50, "Other Co.")))
        assertFalse(Album.canSee(vendor, vend("other", 50, "Other Co.", Album.REVIEW_INTERNAL)))
        assertTrue(Album.canSee(vendor, vend("other", 50, "Other Co.", Album.REVIEW_ACCEPTED)))
        assertFalse(Album.canSee(vendor, vend("other", 40, "Other Co.", Album.REVIEW_ACCEPTED)))   // accepted but not mine
    }

    @Test fun `canSee - a client sees staff photos at once, vendor photos when accepted, never unfiled`() {
        assertTrue(Album.canSee(client, staff("q", 0)))
        assertTrue(Album.canSee(client, staff("s", 50)))
        assertFalse(Album.canSee(client, staff("u", null)))
        assertFalse(Album.canSee(client, vend("p", 50, "Demo Waterproofing Co.")))
        assertFalse(Album.canSee(client, vend("i", 50, "Demo Waterproofing Co.", Album.REVIEW_INTERNAL)))
        assertTrue(Album.canSee(client, vend("a", 50, "Demo Waterproofing Co.", Album.REVIEW_ACCEPTED)))
        assertFalse(Album.canSee(client, vend("a", null, "Demo Waterproofing Co.", Album.REVIEW_ACCEPTED)))
    }

    // ---- chapters ---------------------------------------------------------------------

    private val demoScope = listOf(10, 20, 30, 40, 50, 60, 70, 100, 110, 120, 130, 140)
    private val demoLog = listOf(
        state(1, 10, "In progress", note = "First site visit", at = "2026-09-28T04:45:00.000Z"),
        state(2, 10, "Done", "In progress", "Kitchen platform and old bathroom tiles out", at = "2026-10-01T04:00:00.000Z"),
        state(3, 50, "In progress", note = "Bathroom waterproofing started", at = "2026-10-06T05:30:00.000Z"),
        cr(4, 90, Album.KIND_CR_RAISE, "Client added factory-made modular units after approval", at = "2026-10-20T12:30:00.000Z"))
    private val demoStages = listOf(stage(5, "Client wish list", "2026-10-14T02:38:00.000Z"))
    private val demoItems = listOf(
        Photo("MCAP-1", 0, "2026-09-28T06:30:00.000Z", kind = Album.PHOTO_360, room = "Living Room"),
        Photo("MCAP-2", 0, "2026-10-16T06:30:00.000Z", kind = Album.PHOTO_360, room = "Guest Bedroom"),
        staff("S1", 10, "2026-10-02T11:10:00.000Z"),
        vend("S2", 50, "Demo Waterproofing Co.", Album.REVIEW_ACCEPTED, "2026-10-07T04:35:00.000Z"),
        vend("S3", 50, "Demo Waterproofing Co.", Album.REVIEW_ACCEPTED, "2026-10-08T04:50:00.000Z"),
        vend("S4", 30, "Demo Plumbing Works", Album.REVIEW_ACCEPTED, "2026-10-14T09:40:00.000Z"),
        vend("S5", 40, "Demo Electricals", Album.REVIEW_ACCEPTED, "2026-10-16T07:00:00.000Z"),
        staff("S6", 90, "2026-10-20T12:15:00.000Z"),
        vend("S7", 50, "Demo Waterproofing Co.", at = "2026-10-21T05:50:00.000Z"),
        staff("U1", null, "2026-10-22T05:00:00.000Z"),
        staff("U2", 45, "2026-10-23T05:00:00.000Z"))

    @Test fun `chapters for the PM - 17 chapters, the quote groups, the states, the scope, the CR, the order (D3, E2-E4)`() {
        val c = Album.chapters(demoItems, demoLog, demoStages, demoScope, "Survey", pm)
        assertEquals(16, c.works.size)
        assertEquals((1..16).toList(), c.works.map { it.n })
        assertEquals(11, c.total)
        assertFalse(c.execLocked, "D3: recorded work keeps PROJ-DEMO unlocked at Survey")
        assertTrue(c.works.none { it.locked })
        // chapter 0: the 360s by the stage they were taken in; the two unfiled items (null and an id that is not a work)
        assertEquals(6, c.quote.groups.size)
        assertEquals(listOf("MCAP-1"), c.quote.groups[0].items.map { it.id })
        assertEquals(listOf("MCAP-2"), c.quote.groups[1].items.map { it.id })
        assertEquals(listOf("U1", "U2"), c.quote.unfiled.map { it.id })
        assertEquals(4, c.quote.count)
        assertFalse(c.quote.locked)
        assertEquals(listOf(true, false, false, false, false, false), c.quote.groups.map { it.reached })
        assertEquals(listOf(true, false, false, false, false, false), c.quote.groups.map { it.now })
        // the works
        val w = c.works.associateBy { it.id }
        assertEquals("Done", w[10]!!.state)
        assertEquals("In progress", w[50]!!.state)
        assertEquals("Not started", w[90]!!.state)
        assertEquals(3, w[50]!!.count)
        assertEquals("2026-10-21T05:50:00.000Z", w[50]!!.last)
        assertNull(w[20]!!.last)
        assertEquals(10, w[90]!!.n)
        assertFalse(w[90]!!.inScope)
        assertNotNull(w[90]!!.cr)
        for (id in listOf(80, 85, 95)) { assertFalse(w[id]!!.inScope, "$id"); assertNull(w[id]!!.cr, "$id") }
        for (id in demoScope) assertTrue(w[id]!!.inScope, "$id")
        assertEquals(listOf(90), c.changeRequests.map { it.id })
        assertFalse(c.closed)
        assertNull(c.closedAt)
        assertEquals(listOf("MCAP-1", "MCAP-2", "U1", "U2", "S1", "S4", "S5", "S2", "S3", "S7", "S6"), c.order.map { it.id })
    }

    @Test fun `chapters for a vendor - only its works, chapter 0 locked, the others not your work`() {
        val c = Album.chapters(demoItems, demoLog, demoStages, demoScope, "Survey", vendor)
        assertTrue(c.quote.locked)
        assertEquals(0, c.quote.count)
        assertEquals(setOf(30, 50), c.works.filter { it.mine }.map { it.id }.toSet())
        assertEquals(setOf(30, 50), c.works.filter { !it.locked }.map { it.id }.toSet())
        assertEquals(listOf("S4", "S2", "S3", "S7"), c.order.map { it.id })
        assertEquals(4, c.total)
    }

    @Test fun `chapters for the client - accepted vendor photos and staff photos, nothing unfiled`() {
        val c = Album.chapters(demoItems, demoLog, demoStages, demoScope, "Survey", client)
        assertEquals(listOf("MCAP-1", "MCAP-2", "S1", "S4", "S5", "S2", "S3", "S6"), c.order.map { it.id })
        assertEquals(0, c.quote.unfiled.size)
        assertFalse(c.quote.locked)
        assertTrue(c.works.none { it.locked })
    }

    @Test fun `chapters under the lock - every work chapter locked with the banner's flag, chapter 0 open (D1, D2)`() {
        val items = listOf(Photo("MCAP-1", 0, "2026-10-05T05:00:00.000Z", kind = Album.PHOTO_360))
        for (q in listOf("Survey", "Client wish list", "Vendor quoted", "Estimated", "Revisions")) {
            val c = Album.chapters(items, emptyList(), emptyList(), null, q, pm)
            assertTrue(c.execLocked, q)
            assertTrue(c.works.all { it.locked && it.execLocked }, q)
            assertFalse(c.quote.locked, q)
            assertEquals(1, c.quote.count, q)
            assertTrue(c.works.all { it.inScope }, "no baseline: everything in scope")
        }
        val open = Album.chapters(items, emptyList(), emptyList(), null, "Client approved", pm)
        assertFalse(open.execLocked)
        assertTrue(open.works.none { it.locked })
        assertEquals(listOf(false, false, false, false, false, true), open.quote.groups.map { it.now })
        assertTrue(open.quote.groups.all { it.reached })
    }

    @Test fun `chapters - closed when 140 is Done, with its time`() {
        val log = listOf(state(1, 140, "Done", at = "2026-12-20T05:00:00.000Z"))
        val c = Album.chapters(emptyList(), log, emptyList(), null, "Client approved", pm)
        assertTrue(c.closed)
        assertEquals("2026-12-20T05:00:00.000Z", c.closedAt)
        assertEquals(0, c.total)
    }

    // ---- the writer gates ------------------------------------------------------------

    @Test fun `stateWrite - PM only, open chapters, a work, a state, a change, a remark when leaving Done`() {
        assertNull(Album.stateWrite(pm, true, 10, "Not started", "In progress", ""))
        assertEquals(Album.STATE_PM_ONLY, Album.stateWrite(designer, true, 10, "Not started", "In progress", ""))
        assertEquals(Album.STATE_PM_ONLY, Album.stateWrite(vendor, true, 50, "Not started", "In progress", ""))
        assertEquals(Album.STATE_PM_ONLY, Album.stateWrite(client, true, 10, "Not started", "In progress", ""))
        assertEquals(Album.EXEC_LOCK_BANNER, Album.stateWrite(pm, false, 10, "Not started", "In progress", ""))
        assertEquals(Album.NOT_A_WORK, Album.stateWrite(pm, true, 0, "Not started", "In progress", ""))
        assertEquals(Album.NOT_A_WORK, Album.stateWrite(pm, true, 45, "Not started", "In progress", ""))
        assertEquals(Album.NOT_A_STATE, Album.stateWrite(pm, true, 10, "Not started", "Finished", ""))
        assertEquals(Album.SAME_STATE, Album.stateWrite(pm, true, 10, "Done", "Done", "x"))
        // any state -> any state; only leaving Done needs the reason
        assertEquals(Album.DONE_REASON_NEED, Album.stateWrite(pm, true, 10, "Done", "In progress", "  "))
        assertNull(Album.stateWrite(pm, true, 10, "Done", "In progress", "tiles cracked"))
        assertNull(Album.stateWrite(pm, true, 10, "Abandoned", "Not started", ""))
        assertNull(Album.stateWrite(pm, true, 10, "In progress", "Done", ""))
    }

    @Test fun `crRaise and crClear - PM only (E8), off the baseline, no live CR, a live CR to clear (E7)`() {
        val log = listOf(cr(1, 90, Album.KIND_CR_RAISE, "x"))
        assertNull(Album.crRaise(pm, true, demoScope, 80, log))
        assertEquals(Album.CR_IN_SCOPE, Album.crRaise(pm, true, demoScope, 10, log), "E7: in scope -> refused")
        assertEquals(Album.CR_LIVE, Album.crRaise(pm, true, demoScope, 90, log))
        assertEquals(Album.CR_NO_BASELINE, Album.crRaise(pm, true, null, 80, log))
        assertEquals(Album.NOT_A_WORK, Album.crRaise(pm, true, demoScope, 0, log))
        assertEquals(Album.EXEC_LOCK_BANNER, Album.crRaise(pm, false, demoScope, 80, log))
        assertNull(Album.crClear(pm, true, 90, log))
        assertEquals(Album.CR_NONE, Album.crClear(pm, true, 80, log))
        assertEquals(Album.CR_NONE, Album.crClear(pm, true, 90, log + cr(2, 90, Album.KIND_CR_CLEAR)))
        assertEquals(Album.EXEC_LOCK_BANNER, Album.crClear(pm, false, 90, log))
        // E8: Designer, Client and Vendor can neither raise nor clear
        for (v in listOf(designer, client, vendor)) {
            assertEquals(Album.CR_PM_ONLY, Album.crRaise(v, true, demoScope, 80, log), v.role)
            assertEquals(Album.CR_PM_ONLY, Album.crClear(v, true, 90, log), v.role)
        }
    }

    @Test fun `photoRecord - staff into the quote journey always, into a work when open - a vendor into mine - a client never`() {
        for (v in listOf(pm, designer)) {
            assertNull(Album.photoRecord(v, false, 0), v.role)
            assertNull(Album.photoRecord(v, true, 0), v.role)
            assertEquals(Album.EXEC_LOCK_BANNER, Album.photoRecord(v, false, 10), v.role)
            assertNull(Album.photoRecord(v, true, 10), v.role)
            assertEquals(Album.NOT_A_WORK, Album.photoRecord(v, true, 45), v.role)
        }
        assertNull(Album.photoRecord(vendor, true, 50))
        assertNull(Album.photoRecord(vendor, false, 50), "a vendor's upload has no exec gate (S5: mine else 403)")
        assertEquals(Album.NOT_YOUR_WORK, Album.photoRecord(vendor, true, 40))
        assertEquals(Album.NOT_YOUR_WORK, Album.photoRecord(vendor, true, 0))
        assertEquals("not your work", Album.NOT_YOUR_WORK)
        assertEquals(Album.PHOTO_NO_CLIENT, Album.photoRecord(client, true, 0))
        assertEquals(Album.PHOTO_NO_CLIENT, Album.photoRecord(client, true, 10))
    }

    @Test fun `review - PM only, a vendor's row only, accepted or internal, the same state is a no-op`() {
        val p = vend("S7", 50, "Demo Waterproofing Co.")
        assertNull(Album.review(pm, p, Album.REVIEW_ACCEPTED))
        assertNull(Album.review(pm, p, Album.REVIEW_INTERNAL))
        assertEquals(Album.REVIEW_SAME, Album.review(pm, p.copy(review = Album.REVIEW_ACCEPTED), Album.REVIEW_ACCEPTED))
        assertNull(Album.review(pm, p.copy(review = Album.REVIEW_ACCEPTED), Album.REVIEW_INTERNAL))
        assertEquals(Album.NOT_A_REVIEW, Album.review(pm, p, Album.REVIEW_PENDING))
        assertEquals(Album.REVIEW_VENDOR_ONLY, Album.review(pm, staff("S1", 10), Album.REVIEW_ACCEPTED))
        for (v in listOf(designer, vendor, client)) assertEquals(Album.REVIEW_PM_ONLY, Album.review(v, p, Album.REVIEW_ACCEPTED), v.role)
    }

    // ---- the outbox and the verdict table -------------------------------------------

    private val opA = Op("ALB-a", Album.KIND_STATE, 10, "2", "In progress", "Not started")
    private val opB = Op("ALB-b", Album.KIND_STATE, 10, "2", "Done", "In progress")
    private val opC = Op("ALB-c", Album.KIND_CR_RAISE, 90, "4", note = "more units")
    private val opD = Op("ALB-d", Album.OP_PHOTO, 10, "0", photo = "ALB-d", room = "Kitchen", caption = "debris out")
    private val opE = Op("ALB-e", Album.OP_REVIEW, 50, "", Album.REVIEW_ACCEPTED, photo = "S7")
    private val queue = listOf(opA, opB, opC, opD, opE)
    private val mirror = View(Album.PM, exec = true, scope = demoScope, quoteStage = "Client approved",
        works = Album.WORKS.map { Work(it.id, Album.stateOf(demoLog, it.id), seq = Album.entryOf(demoLog, it.id)?.seq ?: 0, inScope = Album.inScope(demoScope, it.id), cr = Album.crOf(demoLog, it.id)) },
        log = demoLog, stages = demoStages, photos = demoItems)
    private val box = Outbox(mirror, queue)

    @Test fun `the verdict table - 200 posted or replay, 409 conflict or locked, refused for good, relogin, stop`() {
        assertEquals(Verdict.POSTED, Album.verdict(200))
        assertEquals(Verdict.REPLAY, Album.verdict(200, written = false))
        assertEquals(Verdict.CONFLICT, Album.verdict(409))
        assertEquals(Verdict.LOCKED, Album.verdict(409, locked = true))
        for (c in listOf(400, 403, 413, 415)) { assertEquals(Verdict.REFUSED, Album.verdict(c), "$c"); assertTrue(Album.refusedForGood(c), "$c") }
        assertEquals(Verdict.RELOGIN, Album.verdict(401))
        for (c in listOf(404, 405, 500, 502, 503, 0, -1)) { assertEquals(Verdict.STOP, Album.verdict(c), "$c"); assertFalse(Album.refusedForGood(c), "$c") }
        assertFalse(Album.refusedForGood(409))
    }

    @Test fun `afterPost - the op leaves the queue and the mirror takes the entry, the work row and the photo`() {
        val e = state(6, 10, "In progress", "Done", "re-opened", at = "2026-10-24T05:00:00.000Z").copy(id = "ALB-a")
        val w = Work(10, "In progress", "pm@x", "P M", e.at, 6, true, null, 1)
        val next = Album.afterPost(box, opA, Answer(e, w))
        assertEquals(listOf("ALB-b", "ALB-c", "ALB-d", "ALB-e"), next.queue.map { it.id })
        assertEquals(emptyList(), next.dropped)
        val m = next.mirror!!
        assertEquals(5, m.log.size)
        assertEquals(e, m.log.last())
        assertEquals("In progress", Album.stateOf(m.log, 10))
        assertEquals(w, m.works.first { it.id == 10 })
        assertEquals(Album.WORK_IDS, m.works.map { it.id }, "the works stay in S5's order")
        // a photo answer: added, then replaced by id (the JPEG landed: bytes > 0)
        val p = Photo("ALB-d", 10, "2026-10-24T05:01:00.000Z", room = "Kitchen", caption = "debris out", by = "pm@x")
        val n2 = Album.afterPost(next, opD, Answer(photo = p))
        assertEquals("ALB-d", n2.mirror!!.photos.last().id)
        val n3 = Album.afterPost(n2, Op("ALB-z", Album.OP_PHOTO, 10), Answer(photo = p.copy(bytes = 123456)))
        assertEquals(demoItems.size + 1, n3.mirror!!.photos.size)
        assertEquals(123456L, n3.mirror!!.photos.first { it.id == "ALB-d" }.bytes)
        // an entry already in the log (a lost 200, then the GET, then the replay) is replaced, not doubled
        assertEquals(5, Album.afterPost(next, opA, Answer(e.copy(note = "same row"))).mirror!!.log.size)
        // an op not in the queue (already dropped) changes nothing
        assertEquals(n3, Album.afterPost(n3, Op("ALB-q", Album.KIND_STATE, 20), Answer()))
    }

    @Test fun `afterReplay - 200 written false is harmless, the same as a 200`() {
        val e = state(6, 10, "In progress", "Done", at = "2026-10-24T05:00:00.000Z").copy(id = "ALB-a")
        assertEquals(Album.afterPost(box, opA, Answer(e)), Album.afterReplay(box, opA, Answer(e)))
        assertEquals(listOf("ALB-b", "ALB-c", "ALB-d", "ALB-e"), Album.afterReplay(box, opA, Answer()).queue.map { it.id })
    }

    @Test fun `afterConflict - S5 wins - the op and every later op on the same work are dropped, others stay, the mirror takes the body`() {
        // staff on /projects moved 10 to On hold meanwhile (seq 6): the phone's base 2 is stale
        val cur = state(6, 10, "On hold", "Done", "waiting on tiles", at = "2026-10-23T05:00:00.000Z").copy(by = "staff@x")
        val w = Work(10, "On hold", "staff@x", "", cur.at, 6, true, null, 1)
        val next = Album.afterConflict(box, opA, "Someone moved Demolition & site preparation meanwhile", Answer(cur, w))
        assertEquals(listOf("ALB-c", "ALB-e"), next.queue.map { it.id }, "b (same work, later) and d (the photo on 10) go; c and e stay")
        assertEquals(listOf("ALB-a", "ALB-b", "ALB-d"), next.dropped.map { it.id })
        assertTrue(next.dropped.all { it.error == "Someone moved Demolition & site preparation meanwhile" })
        assertEquals("On hold", Album.stateOf(next.mirror!!.log, 10))
        assertEquals(w, next.mirror!!.works.first { it.id == 10 })
        // an EARLIER op on the same work is not touched: only later ones ride on the conflicting base
        val n2 = Album.afterConflict(box, opB, "x", Answer(cur, w))
        assertEquals(listOf("ALB-a", "ALB-c", "ALB-e"), n2.queue.map { it.id })
        assertEquals(listOf("ALB-b", "ALB-d"), n2.dropped.map { it.id })
        // a 409 on the review: its photo's work is 50, nothing else queued on 50
        val n3 = Album.afterConflict(box, opE, "Reviewed meanwhile", Answer(photo = demoItems.first { it.id == "S7" }.copy(review = Album.REVIEW_INTERNAL)))
        assertEquals(listOf("ALB-a", "ALB-b", "ALB-c", "ALB-d"), n3.queue.map { it.id })
        assertEquals(Album.REVIEW_INTERNAL, n3.mirror!!.photos.first { it.id == "S7" }.review)
        // an op not in the queue: nothing moves
        assertEquals(box, Album.afterConflict(box, Op("ALB-q", Album.KIND_STATE, 20), "x", Answer()))
    }

    @Test fun `afterLocked - S5 answered the banner - the op and later same-work ops are dropped saying so, the mirror as it was`() {
        val next = Album.afterLocked(box, opA, Album.EXEC_LOCK_BANNER)
        assertEquals(listOf("ALB-c", "ALB-e"), next.queue.map { it.id })
        assertEquals(listOf("ALB-a", "ALB-b", "ALB-d"), next.dropped.map { it.id })
        assertTrue(next.dropped.all { it.error == "Work starts once the client approves the estimate" })
        assertEquals(mirror, next.mirror)
        // the next op on another work gets its own verdict on this pass
        val n2 = Album.afterLocked(next, opC, Album.EXEC_LOCK_BANNER)
        assertEquals(listOf("ALB-e"), n2.queue.map { it.id })
        assertEquals(4, n2.dropped.size)
    }

    @Test fun `afterRefused - dropped for good with one note - the rest of the queue and the mirror untouched`() {
        val next = Album.afterRefused(box, opB, "A reason is needed to change a Done work")
        assertEquals(listOf("ALB-a", "ALB-c", "ALB-d", "ALB-e"), next.queue.map { it.id })
        assertEquals(listOf(opB.copy(error = "A reason is needed to change a Done work")), next.dropped)
        assertEquals(mirror, next.mirror)
        assertEquals(next, Album.afterRefused(next, opB, "again"), "already dropped: nothing moves, no second note")
        // 401 / 404 / 5xx / IO: no transition exists -- the queue waits for the next pass (the verdict says STOP or RELOGIN)
    }

    @Test fun `a null mirror stays null through every transition - a fragment cannot stand in for a GET`() {
        val b = Outbox(null, queue)
        val e = state(1, 10, "In progress")
        assertNull(Album.afterPost(b, opA, Answer(e, Work(10, "In progress"))).mirror)
        assertNull(Album.afterConflict(b, opA, "x", Answer(e)).mirror)
        assertEquals(listOf("ALB-c", "ALB-e"), Album.afterConflict(b, opA, "x", Answer(e)).queue.map { it.id })
        assertNull(Album.afterLocked(b, opA, "x").mirror)
        assertNull(Album.afterRefused(b, opA, "x").mirror)
        assertNull(Album.merged(null, Answer(e)))
    }

    @Test fun `provisional rows - a queued op shows at once, above every S5 seq, in queue order`() {
        val rows = queue.filter { it.kind in setOf(Album.KIND_STATE, Album.KIND_CR_RAISE, Album.KIND_CR_CLEAR) }
            .mapIndexed { i, op -> Album.provisional(op, i, Album.PM, "pm@x", "P M", "2026-10-24T05:0${i}:00.000Z") }
        assertEquals(3, rows.size)
        assertTrue(rows.all { it.seq >= Album.PENDING_SEQ })
        assertEquals(listOf(Album.PENDING_SEQ, Album.PENDING_SEQ + 1, Album.PENDING_SEQ + 2), rows.map { it.seq })
        assertTrue(rows.all { it.seq > demoLog.maxOf { e -> e.seq } })
        val log = demoLog + rows
        assertEquals("Done", Album.stateOf(log, 10), "b (Done) is after a (In progress) in the queue")
        assertEquals("In progress", Album.stateOf(demoLog + rows[0], 10), "a alone: In progress over the mirror's Done")
        assertNotNull(Album.crOf(log, 90))
        assertTrue(Album.hasStateEntry(rows.take(1)))
        assertEquals("ALB-a", rows[0].id)
        assertEquals("more units", rows[2].note)
        assertNull(rows[0].photo)
        // a queued photo: an item on its work, the party by the role, bytes 0 (its JPEG is on the phone)
        val p = Album.provisionalPhoto(opD, Album.DESIGNER, "d@x", "D", "2026-10-24T05:03:00.000Z")
        assertEquals(Photo("ALB-d", 10, "2026-10-24T05:03:00.000Z", Album.PARTY_STAFF, "Kitchen", caption = "debris out", role = Album.DESIGNER, by = "d@x", byName = "D"), p)
        assertTrue(Album.canSee(client, p))
        val vp = Album.provisionalPhoto(Op("ALB-v", Album.OP_PHOTO, 50), Album.VENDOR, "v@x", "V", "2026-10-24T05:04:00.000Z", "Demo Waterproofing Co.")
        assertEquals(Album.PARTY_VENDOR, vp.party)
        assertTrue(Album.canSee(vendor, vp))
        assertFalse(Album.canSee(client, vp), "pending until the PM accepts it")
    }
}
