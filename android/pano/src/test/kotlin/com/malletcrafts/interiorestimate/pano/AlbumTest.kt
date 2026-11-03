package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Album.Answer
import com.malletcrafts.interiorestimate.pano.Album.CrKind
import com.malletcrafts.interiorestimate.pano.Album.CrOutcome
import com.malletcrafts.interiorestimate.pano.Album.Entry
import com.malletcrafts.interiorestimate.pano.Album.Op
import com.malletcrafts.interiorestimate.pano.Album.Outbox
import com.malletcrafts.interiorestimate.pano.Album.Photo
import com.malletcrafts.interiorestimate.pano.Album.Refusal
import com.malletcrafts.interiorestimate.pano.Album.Verdict
import com.malletcrafts.interiorestimate.pano.Album.View
import com.malletcrafts.interiorestimate.pano.Album.Viewer
import com.malletcrafts.interiorestimate.pano.Album.Work
import com.malletcrafts.interiorestimate.pano.Album.WorkState
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The album by works on the phone (v61b slice 2, K1; port 61c slice K1b), against the approved
 * prototype v61c.1's ALB60 core (index61c1.html 7100-7530) and its checks (ct61b D1-D3 the exec
 * lock, E7-E8 the change requests), S5 QUOTE as shipped on stage (functions/_lib/asis.js: the
 * change-request kinds and answers, approval final, the review's 409 before the JPEG) and the
 * verdict table of DESIGN-slice2.md "Offline and conflicts". Every name here is invented. The
 * English and the Marathi a pin expects are written out by hand: a pin is a literal.
 */
class AlbumTest {

    private val pm = Viewer(Album.PM)
    private val designer = Viewer(Album.DESIGNER)
    private val client = Viewer(Album.CLIENT)
    // a vendor doing Waterproofing (50) and Plumbing (30), as two Supplier rows
    private val vendor = Viewer(Album.VENDOR, mine = setOf(50, 30), suppliers = setOf("demo waterproofing co.", "demo plumbing works"))

    private fun state(seq: Long, work: Int, value: String, prev: String = Album.NOT_STARTED, note: String = "", at: String = "2026-10-0${seq}T05:00:00.000Z") =
        Entry(seq, "ALB-%012x".format(seq), Album.KIND_STATE, work, value, prev, note, Album.PM, "pm@x", "P M", at)
    private fun raise(seq: Long, work: Int, kind: String = "add", note: String = "", at: String = "2026-10-1${seq % 10}T05:00:00.000Z") =
        Entry(seq, "ALB-%012x".format(seq), Album.KIND_CR_RAISE, work, value = kind, note = note, role = Album.PM, by = "pm@x", byName = "P M", at = at)
    private fun clear(seq: Long, work: Int, outcome: String = "withdrawn", kind: String = "add", clientAt: String? = null, at: String = "2026-10-2${seq % 10}T05:00:00.000Z") =
        Entry(seq, "ALB-%012x".format(seq), Album.KIND_CR_CLEAR, work, value = outcome, prev = kind, role = Album.PM, by = "pm@x", byName = "P M", at = at, clientAt = clientAt)
    private fun stage(seq: Long, value: String, at: String) = Entry(seq, "ALB-%012x".format(seq), Album.KIND_STAGE, null, value, at = at)
    private fun staff(id: String, work: Int?, at: String = "2026-10-10T05:00:00.000Z") = Photo(id, work, at, Album.PARTY_STAFF, by = "d@x")
    private fun vend(id: String, work: Int?, vendor: String, review: String = Album.REVIEW_PENDING, at: String = "2026-10-11T05:00:00.000Z", bytes: Long = 1000) =
        Photo(id, work, at, Album.PARTY_VENDOR, vendor = vendor, review = review, by = "v@x", bytes = bytes)

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

    @Test fun `the five states and the four roles - S5's values stored, their names and lines shown from the table`() {
        assertEquals(listOf("Not started", "In progress", "On hold", "Done", "Abandoned"), Album.STATES)
        assertEquals(listOf("pm", "designer", "vendor", "client"), Album.ROLE_IDS)
        assertEquals(listOf("Project Manager", "Designer", "Vendor", "Client"), Album.ROLE_IDS.map { Album.roleLabel(Tx.EN, it) })
        assertEquals(listOf("प्रोजेक्ट मॅनेजर", "डिझायनर", "व्हेंडर", "ग्राहक"), Album.ROLE_IDS.map { Album.roleLabel(Tx.MR, it) })
        assertEquals(listOf("sees everything · sets work states · accepts vendor photos", "sees everything · takes progress shots · changes no state",
            "sees only their own work · uploads photos for review", "sees accepted photos and work states only"), Album.ROLE_IDS.map { Album.roleLine(Tx.EN, it) })
        assertEquals("सगळे पाहतो · कामाची स्थिती ठरवतो · व्हेंडरचे फोटो स्वीकारतो", Album.roleLine(Tx.MR, Album.PM))
        // a role S5 might add later is shown as sent, with no line
        assertEquals("auditor", Album.roleLabel(Tx.MR, "auditor"))
        assertEquals("", Album.roleLine(Tx.EN, "auditor"))
        assertEquals("Not started", Album.stateName(null))
        assertEquals("Not started", Album.stateName("Finished"))
        assertEquals("On hold", Album.stateName("On hold"))
        assertEquals("Done", Album.stateText(Tx.EN, "Done"))
        assertEquals("पूर्ण", Album.stateText(Tx.MR, "Done"))
        assertEquals("चालू आहे", Album.stateText(Tx.MR, "In progress"))
        assertEquals("सुरू झाले नाही", Album.stateText(Tx.MR, "Finished"))
    }

    @Test fun `PAY is S5's PAY_TERMS as distinct kind-at pairs, worded by paySchedule's rule from the table`() {
        assertEquals(16, Album.PAY.size)
        assertEquals(listOf("Before Demolition & site preparation starts", "When Demolition & site preparation is done"), Album.payOf(Tx.EN, 10))
        // 40's two 's' terms (slab and wall piping) are one "Before ... starts"; the plates ride Lights (120)
        assertEquals(listOf("Before Electrical, concealed wiring starts", "When Electrical, concealed wiring is done",
            "Before Lights, fans, fixtures & security starts"), Album.payOf(Tx.EN, 40))
        assertEquals(listOf("When Cleaning & handover is done"), Album.payOf(Tx.EN, 140))
        assertEquals(listOf("Before Waterproofing starts", "When Waterproofing is done", "When Cleaning & handover is done"), Album.payOf(Tx.EN, 50))
        assertEquals(listOf("तोडकाम व साइट तयारी सुरू होण्यापूर्वी", "तोडकाम व साइट तयारी पूर्ण झाल्यावर"), Album.payOf(Tx.MR, 10))
        assertEquals(emptyList(), Album.payOf(Tx.EN, 0))
        assertEquals(emptyList(), Album.payOf(Tx.EN, 45))
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

    @Test fun `workOfCapture - the one filing rule - a filed work stands, NULL files by its legacy stage`() {
        // the plan's table (DESIGN-port61c K1b), the prototype's workOfCapture and S5's workOfRecord
        assertEquals(20, Album.workOfCapture(null, "Plaster & waterproofing"))
        assertEquals(0, Album.workOfCapture(null, ""))
        assertEquals(0, Album.workOfCapture(null, null))
        assertNull(Album.workOfCapture(null, "Material delivered on site"))
        for (st in listOf(null, "", "Handover", "Material delivered on site")) {
            assertEquals(0, Album.workOfCapture(0, st), "(0, $st)")
            assertEquals(90, Album.workOfCapture(90, st), "(90, $st)")
        }
        // a stray id is not a filing: S5's validWork refuses it, so the stage files it (as workOfRecord does)
        assertEquals(140, Album.workOfCapture(45, "Handover"))
        assertEquals(70, Album.workOfCapture(null, "floor TILING"))
    }

    @Test fun `chapter numbers - 0 the quote journey, index plus one for a work, null unfiled - and the names as shown`() {
        assertEquals(0, Album.chapterOf(0))
        assertEquals(1, Album.chapterOf(10))
        assertEquals(10, Album.chapterOf(90))
        assertEquals(16, Album.chapterOf(140))
        assertNull(Album.chapterOf(null))
        assertNull(Album.chapterOf(45))
        assertEquals("Quote journey", Album.workName(Tx.EN, 0))
        assertEquals("not filed to a work", Album.workName(Tx.EN, null))
        assertEquals("Modular furniture, factory-made", Album.workName(Tx.EN, 90))
        assertEquals("", Album.workName(Tx.EN, 45))
        assertEquals("कोटेशनचा प्रवास", Album.workName(Tx.MR, 0))
        assertEquals("कोणत्याही कामात ठेवलेला नाही", Album.workName(Tx.MR, null))
        assertEquals("मॉड्युलर फर्निचर, कारखान्यात बनवलेले", Album.workName(Tx.MR, 90))
        // the two works whose key lives in another family (masters.tsv)
        assertEquals("वॉटरप्रूफिंग", Album.workName(Tx.MR, 50))
        assertEquals("फॉल्स सीलिंग", Album.workName(Tx.MR, 60))
    }

    @Test fun `Album holds no display string - every String it declares is a wire value or S5's state`() {
        // replaces K1's ">= 90 String fields": since K1b every text the album shows comes from Tx (design-marathi §5)
        val strings = Album::class.java.declaredFields.filter { it.type == String::class.java }
            .onEach { it.isAccessible = true }.map { it.get(null) as String }
        assertTrue(strings.isNotEmpty())
        val wire = Regex("^[a-z0-9_]+$")
        val off = strings.filter { !wire.matches(it) && it !in Album.STATES }
        assertEquals(emptyList(), off, "a display text in Album: key it in android/i18n and read it through a Tx")
        // the old constants are gone, the stale camera string with them
        val names = Album::class.java.declaredFields.map { it.name }.toSet()
        for (gone in listOf("EXEC_LOCK_BANNER", "ACCEPT", "CR_IN_SCOPE", "SITE_IS_AT", "MOVE_PROJECT", "ROLES", "VIEWING_AS_TITLE"))
            assertFalse(gone in names, gone)
        assertTrue(strings.none { it.contains("locked until") })
    }

    @Test fun `every refusal is worded from the table, in English and in Marathi`() {
        for (r in Refusal.entries) {
            val en = r.text(Tx.EN); val mr = r.text(Tx.MR)
            assertTrue(en.isNotBlank(), "$r")
            assertTrue(mr.any { it in 'ऀ'..'ॿ' }, "$r has no Marathi: $mr")
            assertNotEquals(en, mr, "$r")
        }
        assertEquals("Work starts once the client approves the estimate", Refusal.EXEC_LOCKED.text(Tx.EN))
        assertEquals("ग्राहकाने अंदाजपत्रक मंजूर केल्यावर काम सुरू होते", Refusal.EXEC_LOCKED.text(Tx.MR))
        assertEquals("Only the Project Manager sets a work's state", Refusal.STATE_PM_ONLY.text(Tx.EN))
        assertEquals("A reason is needed to change a Done work", Refusal.DONE_REASON_NEED.text(Tx.EN))
        assertEquals("This work is in the approved scope: nothing to add — a change to it is a remove or a change request", Refusal.CR_IN_SCOPE.text(Tx.EN))
        // S5's own 409 words for a remove or a change off the scope (asis.js), one whole key per kind
        assertEquals("This work is not in the approved scope: nothing to remove — bringing it in is an add request", Refusal.CR_OFF_SCOPE_REMOVE.text(Tx.EN))
        assertEquals("This work is not in the approved scope: nothing to change — bringing it in is an add request", Refusal.CR_OFF_SCOPE_CHANGE.text(Tx.EN))
        assertEquals("हे काम मंजूर स्कोपमध्ये नाही: वगळण्यासारखे काही नाही — ते स्कोपमध्ये आणण्यासाठी समावेशाची विनंती करा", Refusal.CR_OFF_SCOPE_REMOVE.text(Tx.MR))
        assertEquals("Change requests start once the client approves the estimate", Refusal.CR_NOT_APPROVED.text(Tx.EN))
        assertEquals("ग्राहकाने अंदाजपत्रक मंजूर केल्यावरच बदल विनंत्या सुरू होतात", Refusal.CR_NOT_APPROVED.text(Tx.MR))
        assertEquals("No live change request to answer on this work", Refusal.CR_NONE.text(Tx.EN))
        assertEquals("Only the Project Manager raises a change request or records the client's answer", Refusal.CR_PM_ONLY.text(Tx.EN))
        assertEquals("client_at must be the date the client answered, as YYYY-MM-DD", Refusal.CLIENT_AT_DAY.text(Tx.EN))
        assertEquals("No image yet: a photo is reviewed once its JPEG has landed", Refusal.REVIEW_NO_IMAGE.text(Tx.EN))
        assertEquals("अजून फोटो पोहोचलेला नाही: फोटो पूर्ण अपलोड झाल्यावरच तो तपासता येतो", Refusal.REVIEW_NO_IMAGE.text(Tx.MR))
        assertEquals("not your work", Refusal.NOT_YOUR_WORK.text(Tx.EN))
    }

    @Test fun `the change-request kinds and answers - S5's wire values, shown from the table`() {
        assertEquals(listOf("add", "remove", "change"), CrKind.entries.map { it.wire })
        assertEquals(listOf("approved", "declined", "withdrawn"), CrOutcome.entries.map { it.wire })
        assertEquals(listOf("add", "remove", "change"), CrKind.entries.map { it.word(Tx.EN) })
        assertEquals(listOf("समावेश", "वगळणे", "बदल"), CrKind.entries.map { it.word(Tx.MR) })
        assertEquals("a work outside the approved scope comes in", CrKind.ADD.sub(Tx.EN))
        assertEquals(listOf("Change approved by client", "Change declined by client", "Change request withdrawn"), CrOutcome.entries.map { it.text(Tx.EN) })
        assertEquals("ग्राहकाने बदल मंजूर केला", CrOutcome.APPROVED.text(Tx.MR))
        assertEquals(listOf(true, true, false), CrOutcome.entries.map { it.needsDay })
        assertNull(CrKind.of("Add"))
        assertNull(CrOutcome.of(""))
    }

    // ---- time: S5's stamps are UTC, the album shows IST ---------------------------

    @Test fun `whenOf and dayOf show IST, with the album's months`() {
        // 20:00 UTC on the 27th is 01:30 IST on the 28th: the prototype's stand-in stamps were IST, S5's are UTC
        assertEquals("28 Oct, 01:30", Album.whenOf(Tx.EN, "2026-10-27T20:00:00.000Z"))
        assertEquals("28 Oct", Album.dayOf(Tx.EN, "2026-10-27T20:00:00.000Z"))
        assertEquals("28 Oct", Album.whenOf(Tx.EN, "2026-10-27T20:00:00.000Z", dateOnly = true))
        // September is "Sep" in the album (index61c1.html 7156), not QuoteStage's "Sept"
        assertEquals("5 Sep, 09:00", Album.whenOf(Tx.EN, "2026-09-05T03:30:00Z"))
        // months stay Jan-Dec under Marathi (decision 3 of 21 Oct)
        assertEquals("5 Sep, 09:00", Album.whenOf(Tx.MR, "2026-09-05T03:30:00Z"))
        // an offset stamp is the same moment
        assertEquals("28 Oct, 01:30", Album.whenOf(Tx.EN, "2026-10-28T01:30:00+05:30"))
        // a bare day is a calendar day, not a moment: no shift, no time
        assertEquals("27 Oct", Album.dayOf(Tx.EN, "2026-10-27"))
        assertEquals("27 Oct", Album.whenOf(Tx.EN, "2026-10-27"))
        assertEquals("", Album.whenOf(Tx.EN, "yesterday"))
        assertEquals("", Album.dayOf(Tx.EN, null))
    }

    @Test fun `isDay is S5's - a real calendar day, a time after it allowed`() {
        assertTrue(Album.isDay("2026-10-27"))
        assertTrue(Album.isDay(" 2026-10-27 "))
        assertTrue(Album.isDay("2026-10-27T09:00:00"))
        assertFalse(Album.isDay("2026-02-30"))
        assertFalse(Album.isDay("27-10-2026"))
        assertFalse(Album.isDay("2026-10-27 09:00"))
        assertFalse(Album.isDay(""))
        assertFalse(Album.isDay(null))
    }

    // ---- derivations from the log --------------------------------------------

    @Test fun `stateOf is the newest state row by seq, never by time - else Not started`() {
        val log = listOf(
            state(1, 10, "In progress", at = "2026-10-01T05:00:00.000Z"),
            state(3, 10, "Done", "In progress", "out", at = "2026-09-01T05:00:00.000Z"),   // older clock, newer seq: wins
            state(2, 50, "In progress"),
            state(4, 60, "Finished"),                                                        // not one of the five
            raise(5, 10))                                                                    // not a state row
        assertEquals("Done", Album.stateOf(log, 10))
        assertEquals("In progress", Album.stateOf(log, 50))
        assertEquals("Not started", Album.stateOf(log, 60))
        assertEquals("Not started", Album.stateOf(log, 140))
        assertEquals(3L, Album.entryOf(log, 10)!!.seq)
        assertNull(Album.entryOf(log, 140))
        assertEquals(mapOf(10 to WorkState("Done", 3, "2026-09-01T05:00:00.000Z"), 50 to WorkState("In progress", 2, "2026-10-02T05:00:00.000Z"),
            60 to WorkState("Not started", 4, "2026-10-04T05:00:00.000Z")), Album.statesOfLog(log))
        assertEquals(listOf(50), Album.openWorks(Album.statesOfLog(log)))
        assertEquals(emptyList(), Album.openWorks(emptyMap()))
        assertTrue(Album.hasStateEntry(log))
        assertFalse(Album.hasStateEntry(listOf(raise(5, 10))))
    }

    @Test fun `a change request's rows read as S5's logView reads them - a blank raise is add, a blank clear withdrawn`() {
        assertEquals(CrKind.ADD, raise(1, 90, kind = "").crKind)
        assertEquals(CrKind.REMOVE, raise(1, 90, kind = "remove").crKind)
        assertEquals(CrKind.CHANGE, raise(1, 90, kind = "change").crKind)
        assertNull(raise(1, 90).crOutcome)
        assertEquals(CrOutcome.WITHDRAWN, clear(2, 90, outcome = "", kind = "").crOutcome)
        assertEquals(CrKind.ADD, clear(2, 90, outcome = "", kind = "").crKind, "a clear's prev is the kind it answered; blank reads add")
        assertEquals(CrOutcome.APPROVED, clear(2, 90, outcome = "approved", kind = "remove").crOutcome)
        assertEquals(CrKind.REMOVE, clear(2, 90, outcome = "approved", kind = "remove").crKind)
        assertNull(state(3, 90, "Done").crKind)
        assertNull(state(3, 90, "Done").crOutcome)
        assertEquals(CrKind.CHANGE, Work(90, cr = raise(4, 90, kind = "change")).crKind)
        assertNull(Work(90).crKind)
    }

    @Test fun `crOf is live only while the newest cr row is a raise - inScope with a null baseline is everything (E7)`() {
        val raised = listOf(raise(1, 90, note = "Client added factory-made modular units after approval"))
        assertEquals("Client added factory-made modular units after approval", Album.crOf(raised, 90)!!.note)
        assertNull(Album.crOf(raised, 80))
        val cleared = raised + clear(2, 90)
        assertNull(Album.crOf(cleared, 90))
        val again = cleared + raise(3, 90, kind = "change", note = "again")
        assertEquals(3L, Album.crOf(again, 90)!!.seq)
        assertTrue(Album.inScope(null, 90))
        assertTrue(Album.inScope(listOf(10, 90), 90))
        assertFalse(Album.inScope(listOf(10), 90))
        assertFalse(Album.inScope(emptyList(), 10))
    }

    @Test fun `Change requests counts LIVE requests of any kind, in scope or not (asis js change_requests)`() {
        val scope = listOf(10, 20, 30, 40, 50, 60, 70, 100, 110, 120, 130, 140)
        // an add off the scope, a remove and a change in it: three live requests (K1 counted only the off-scope one)
        val log = listOf(raise(1, 90, "add", "x"), raise(2, 10, "remove", "drop the kitchen demolition"), raise(3, 60, "change", "cove lights"))
        val crs = Album.changeRequests(log)
        assertEquals(listOf(10, 60, 90), crs.map { it.id })
        assertEquals(listOf(CrKind.REMOVE, CrKind.CHANGE, CrKind.ADD), crs.map { it.kind })
        assertEquals("Change requests: 3", Tx.EN.album_change_requests_x(crs.size))
        // answering one drops the count; the request stays in the log as a decided one
        assertEquals(listOf(60, 90), Album.changeRequests(log + clear(4, 10, "declined", "remove", "2026-10-28")).map { it.id })
        // a scope that now holds the work changes nothing: the count is of live requests, not of drift
        assertEquals(3, Album.changeRequests(log).size)
        assertTrue(Album.inScope(scope + 90, 90))
        assertEquals(0, Album.changeRequests(emptyList()).size)
    }

    @Test fun `crDone - the work's decided requests, newest first, the client's day else the day recorded in IST`() {
        val log = listOf(
            raise(1, 90, "add"),
            clear(2, 90, "approved", "add", clientAt = "2026-10-27", at = "2026-10-28T05:00:00.000Z"),
            raise(3, 90, "change"),
            // a withdrawal with no client date shows the day it was recorded (§14.9), in IST: 20:00 UTC on the 30th is the 31st
            clear(4, 90, "withdrawn", "change", clientAt = null, at = "2026-10-30T20:00:00.000Z"),
            raise(5, 80, "add"),
            clear(6, 80, "", "", clientAt = "not a day", at = "2026-10-29T05:00:00.000Z"),     // a v61b clear: withdrawn, add
            raise(7, 90, "remove"))                                                            // live, not decided
        val d = Album.crDone(log, 90)
        assertEquals(listOf(4L, 2L), d.map { it.entry.seq })
        assertEquals(listOf(CrOutcome.WITHDRAWN, CrOutcome.APPROVED), d.map { it.outcome })
        assertEquals(listOf(CrKind.CHANGE, CrKind.ADD), d.map { it.kind })
        assertEquals(listOf("2026-10-31", "2026-10-27"), d.map { it.day })
        assertEquals("Change approved by client · 27 Oct · add", Album.decidedText(Tx.EN, d[1]))
        assertEquals("Change request withdrawn · 31 Oct · change", Album.decidedText(Tx.EN, d[0]))
        assertEquals("ग्राहकाने बदल मंजूर केला · 27 Oct · समावेश", Album.decidedText(Tx.MR, d[1]))
        val old = Album.crDone(log, 80).single()
        assertEquals(CrOutcome.WITHDRAWN, old.outcome)
        assertEquals(CrKind.ADD, old.kind)
        assertEquals("2026-10-29", old.day)
        // a client_at with a time after the day is a day (S5's isDay)
        assertEquals("2026-10-26", Album.crDone(listOf(clear(9, 50, "declined", "remove", clientAt = "2026-10-26T18:00:00")), 50).single().day)
        assertEquals(emptyList(), Album.crDone(log, 10))
    }

    @Test fun `amendedScope - an approved add puts the work in, an approved remove takes it out, nothing else moves it`() {
        val scope = listOf(10, 20, 50, 140)
        assertEquals(listOf(10, 20, 50, 90, 140), Album.amendedScope(scope, CrKind.ADD, CrOutcome.APPROVED, 90))
        assertEquals(listOf(10, 50, 140), Album.amendedScope(scope, CrKind.REMOVE, CrOutcome.APPROVED, 20))
        assertEquals(scope, Album.amendedScope(scope, CrKind.CHANGE, CrOutcome.APPROVED, 20))
        for (o in listOf(CrOutcome.DECLINED, CrOutcome.WITHDRAWN)) for (k in CrKind.entries)
            assertEquals(scope, Album.amendedScope(scope, k, o, 90), "$k $o")
        assertEquals(scope, Album.amendedScope(scope, CrKind.ADD, CrOutcome.APPROVED, 50), "already in: no duplicate")
        assertEquals(listOf(90), Album.amendedScope(null, CrKind.ADD, CrOutcome.APPROVED, 90), "S5: (scope || []) with the work")
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
        // a change request is not: only Client approved itself
        assertTrue(Album.approved(" Client approved "))
        for (q in listOf(null, "", "Revisions", "Quoted")) assertFalse(Album.approved(q), "$q")
    }

    @Test fun `only leaving Done needs a remark`() {
        assertTrue(Album.remarkNeeded("Done"))
        for (s in listOf("Not started", "In progress", "On hold", "Abandoned")) assertFalse(Album.remarkNeeded(s), s)
    }

    @Test fun `quoteAt - the newest logged move at or before the photo, else Survey`() {
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
        // the current stage at its time is the same history: it agrees with the last logged move
        assertEquals("Estimated", Album.quoteAt(stages, "2026-10-20T05:00:00.000Z", "Estimated", "2026-10-15T05:00:00.000Z"))
    }

    @Test fun `quoteAt counts the current stage at its quote_stage_at - a project at Vendor quoted since before migration 0008 (critic G2b)`() {
        // S5 began logging stage moves on 26 Oct 2026; this project stood at Vendor quoted from 20 Oct with no logged move
        val noLog = emptyList<Entry>()
        val since = "2026-10-20T06:00:00.000Z"
        assertEquals("Vendor quoted", Album.quoteAt(noLog, "2026-10-25T05:00:00.000Z", "Vendor quoted", since))
        assertEquals("Vendor quoted", Album.quoteAt(noLog, since, "Vendor quoted", since), "at the very moment")
        // before it the phone knows nothing better than Survey, as the prototype
        assertEquals("Survey", Album.quoteAt(noLog, "2026-10-18T05:00:00.000Z", "Vendor quoted", since))
        // a stage S5 never set (no time) or a name outside the six adds no point
        assertEquals("Survey", Album.quoteAt(noLog, "2026-10-25T05:00:00.000Z", "Vendor quoted", ""))
        assertEquals("Survey", Album.quoteAt(noLog, "2026-10-25T05:00:00.000Z", "On hold", since))
        // a logged move after the current point (both S5's) still wins for a later photo
        val later = listOf(stage(1, "Estimated", "2026-10-27T05:00:00.000Z"))
        assertEquals("Estimated", Album.quoteAt(later, "2026-10-28T05:00:00.000Z", "Vendor quoted", since))
        assertEquals("Vendor quoted", Album.quoteAt(later, "2026-10-26T05:00:00.000Z", "Vendor quoted", since))
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

    @Test fun `viewerOf takes the role and the works from S5's answer - the phone derives no role`() {
        val v = Album.viewerOf(View(Album.VENDOR, mine = listOf(50, 30, 999)), listOf(" Demo Waterproofing CO. ", " "))
        assertEquals(Viewer(Album.VENDOR, setOf(50, 30), setOf("demo waterproofing co.")), v)
        assertEquals(Viewer(Album.DESIGNER), Album.viewerOf(View(Album.DESIGNER)))
        assertTrue(Album.viewerOf(View(Album.PM)).staff)
        assertFalse(Album.viewerOf(View(Album.CLIENT)).staff)
    }

    // ---- the project-state store's view --------------------------------------------

    @Test fun `statesOfWorks keeps the works S5 has a state row for, and withQueued lays the queue over them`() {
        val works = Album.WORKS.map { Work(it.id, seq = 3) } +
            Work(10, "Done", at = "2026-10-01T04:00:00.000Z", seq = 7)
        val rows = listOf(Work(10, "Done", "pm@x", "P M", "2026-10-01T04:00:00.000Z", 7), Work(50, "In progress", at = "2026-10-06T05:30:00.000Z", seq = 9),
            Work(60, seq = 4), Work(45, "Done", at = "2026-10-06T05:30:00.000Z", seq = 1))
        // a blank at = no state row ever (asis.js albumView); 45 is not a work
        assertEquals(mapOf(10 to WorkState("Done", 7, "2026-10-01T04:00:00.000Z"), 50 to WorkState("In progress", 9, "2026-10-06T05:30:00.000Z")), Album.statesOfWorks(rows))
        assertEquals(1, Album.statesOfWorks(works).size)
        val q = listOf(Op("ALB-a", Album.KIND_STATE, 10, "7", "On hold", "Done", "snag"), Op("ALB-b", Album.KIND_CR_RAISE, 90, "0", "add"),
            Op("ALB-c", Album.KIND_STATE, 60, "4", "In progress", "Not started"))
        val s = Album.withQueued(Album.statesOfWorks(rows), q)
        assertEquals(WorkState("On hold", Album.PENDING_SEQ + 0), s[10])
        assertEquals(WorkState("In progress", 9, "2026-10-06T05:30:00.000Z"), s[50])
        assertEquals(WorkState("In progress", Album.PENDING_SEQ + 2), s[60])
        assertNull(s[90], "a change request is not a state")
        assertEquals(listOf(50, 60), Album.openWorks(s))
    }

    // ---- chapters ---------------------------------------------------------------------

    private val demoScope = listOf(10, 20, 30, 40, 50, 60, 70, 100, 110, 120, 130, 140)
    private val demoLog = listOf(
        state(1, 10, "In progress", note = "First site visit", at = "2026-09-28T04:45:00.000Z"),
        state(2, 10, "Done", "In progress", "Kitchen platform and old bathroom tiles out", at = "2026-10-01T04:00:00.000Z"),
        state(3, 50, "In progress", note = "Bathroom waterproofing started", at = "2026-10-06T05:30:00.000Z"),
        raise(4, 90, "add", "Client added factory-made modular units after approval", at = "2026-10-20T12:30:00.000Z"))
    private val demoStates = Album.statesOfLog(demoLog)
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

    private fun ch(items: List<Photo> = demoItems, log: List<Entry> = demoLog, stages: List<Entry> = demoStages, scope: List<Int>? = demoScope,
                   q: String? = "Survey", qAt: String? = "", states: Map<Int, WorkState> = demoStates, viewer: Viewer = pm) =
        Album.chapters(items, log, stages, scope, q, qAt, states, viewer)

    @Test fun `chapters for the PM - 17 chapters, the quote groups, the states, the scope, the CR, the order (D3, E2-E4)`() {
        val c = ch()
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
        assertEquals(listOf(50), c.open)
        assertFalse(c.closed)
        assertNull(c.closedAt)
        assertEquals(listOf("MCAP-1", "MCAP-2", "U1", "U2", "S1", "S4", "S5", "S2", "S3", "S7", "S6"), c.order.map { it.id })
    }

    @Test fun `chapters read the work states from the caller's store, not the log - the log is history`() {
        // the store says 50 is On hold (S5-L, newer than the mirror's page) and 10 was never recorded
        val c = ch(states = mapOf(50 to WorkState("On hold", 12)))
        val w = c.works.associateBy { it.id }
        assertEquals("On hold", w[50]!!.state)
        assertEquals("Not started", w[10]!!.state, "the log's Done is history; the store has no state for 10")
        assertEquals(emptyList(), c.open)
        // no state anywhere in the store: the recorded-work exemption does not apply, whatever the log holds
        assertTrue(ch(states = emptyMap()).execLocked)
        assertFalse(ch(states = emptyMap(), q = "Client approved").execLocked)
    }

    @Test fun `chapters for a vendor - only its works, chapter 0 locked, no change request outside them`() {
        val c = ch(viewer = vendor)
        assertTrue(c.quote.locked)
        assertEquals(0, c.quote.count)
        assertEquals(setOf(30, 50), c.works.filter { it.mine }.map { it.id }.toSet())
        assertEquals(setOf(30, 50), c.works.filter { !it.locked }.map { it.id }.toSet())
        assertEquals(listOf("S4", "S2", "S3", "S7"), c.order.map { it.id })
        assertEquals(4, c.total)
        // 90's request is not the vendor's work: S5 sends it no cr, and neither the chapter nor the count shows one
        assertNull(c.works.first { it.id == 90 }.cr)
        assertEquals(emptyList(), c.changeRequests)
        val mine = ch(log = demoLog + raise(6, 50, "change", "membrane spec"), viewer = vendor)
        assertEquals(listOf(50), mine.changeRequests.map { it.id })
        assertNotNull(mine.works.first { it.id == 50 }.cr)
    }

    @Test fun `chapters for the client - accepted vendor photos and staff photos, nothing unfiled`() {
        val c = ch(viewer = client)
        assertEquals(listOf("MCAP-1", "MCAP-2", "S1", "S4", "S5", "S2", "S3", "S6"), c.order.map { it.id })
        assertEquals(0, c.quote.unfiled.size)
        assertFalse(c.quote.locked)
        assertTrue(c.works.none { it.locked })
    }

    @Test fun `chapters under the lock - every work chapter locked with the banner's flag, chapter 0 open (D1, D2)`() {
        val items = listOf(Photo("MCAP-1", 0, "2026-10-05T05:00:00.000Z", kind = Album.PHOTO_360))
        for (q in listOf("Survey", "Client wish list", "Vendor quoted", "Estimated", "Revisions")) {
            val c = ch(items, emptyList(), emptyList(), null, q, "", emptyMap())
            assertTrue(c.execLocked, q)
            assertTrue(c.works.all { it.locked && it.execLocked }, q)
            assertFalse(c.quote.locked, q)
            assertEquals(1, c.quote.count, q)
            assertTrue(c.works.all { it.inScope }, "no baseline: everything in scope")
        }
        val open = ch(items, emptyList(), emptyList(), null, "Client approved", "2026-10-26T15:09:00.000Z", emptyMap())
        assertFalse(open.execLocked)
        assertTrue(open.works.none { it.locked })
        assertEquals(listOf(false, false, false, false, false, true), open.quote.groups.map { it.now })
        assertTrue(open.quote.groups.all { it.reached })
        // the 360 was taken on 5 Oct, before the approval on 26 Oct: it stays in the Survey group
        assertEquals(listOf("MCAP-1"), open.quote.groups[0].items.map { it.id })
    }

    @Test fun `chapters group a quote-journey item by the current stage when no move was logged (critic G2b)`() {
        val items = listOf(Photo("MCAP-1", 0, "2026-10-18T05:00:00.000Z", kind = Album.PHOTO_360),
            Photo("MCAP-2", 0, "2026-10-24T05:00:00.000Z", kind = Album.PHOTO_360))
        val c = ch(items, emptyList(), emptyList(), null, "Vendor quoted", "2026-10-20T06:00:00.000Z", emptyMap())
        assertEquals(listOf("MCAP-1"), c.quote.groups[0].items.map { it.id })
        assertEquals(listOf("MCAP-2"), c.quote.groups[2].items.map { it.id })
    }

    @Test fun `chapters - closed when the store has 140 Done, with its time - and each work's decided requests`() {
        val c = ch(emptyList(), log = listOf(raise(1, 90, "add"), clear(2, 90, "approved", "add", "2026-12-01")),
            q = "Client approved", states = mapOf(140 to WorkState("Done", 3, "2026-12-20T05:00:00.000Z")))
        assertTrue(c.closed)
        assertEquals("2026-12-20T05:00:00.000Z", c.closedAt)
        assertEquals(0, c.total)
        assertEquals(listOf(CrOutcome.APPROVED), c.works.first { it.id == 90 }.decided.map { it.outcome })
        assertNull(c.works.first { it.id == 90 }.cr)
        // the log saying 140 is Done does not close the album: the store is the state
        assertFalse(ch(emptyList(), log = listOf(state(1, 140, "Done")), states = emptyMap()).closed)
    }

    // ---- the writer gates ------------------------------------------------------------

    @Test fun `stateWrite - PM only, open chapters, a work, a state, a change, a remark when leaving Done`() {
        assertNull(Album.stateWrite(pm, true, 10, "Not started", "In progress", ""))
        assertEquals(Refusal.STATE_PM_ONLY, Album.stateWrite(designer, true, 10, "Not started", "In progress", ""))
        assertEquals(Refusal.STATE_PM_ONLY, Album.stateWrite(vendor, true, 50, "Not started", "In progress", ""))
        assertEquals(Refusal.STATE_PM_ONLY, Album.stateWrite(client, true, 10, "Not started", "In progress", ""))
        assertEquals(Refusal.EXEC_LOCKED, Album.stateWrite(pm, false, 10, "Not started", "In progress", ""))
        assertEquals(Refusal.NOT_A_WORK, Album.stateWrite(pm, true, 0, "Not started", "In progress", ""))
        assertEquals(Refusal.NOT_A_WORK, Album.stateWrite(pm, true, 45, "Not started", "In progress", ""))
        assertEquals(Refusal.NOT_A_STATE, Album.stateWrite(pm, true, 10, "Not started", "Finished", ""))
        assertEquals(Refusal.SAME_STATE, Album.stateWrite(pm, true, 10, "Done", "Done", "x"))
        // any state -> any state; only leaving Done needs the reason
        assertEquals(Refusal.DONE_REASON_NEED, Album.stateWrite(pm, true, 10, "Done", "In progress", "  "))
        assertNull(Album.stateWrite(pm, true, 10, "Done", "In progress", "tiles cracked"))
        assertNull(Album.stateWrite(pm, true, 10, "Abandoned", "Not started", ""))
        assertNull(Album.stateWrite(pm, true, 10, "In progress", "Done", ""))
    }

    @Test fun `crRaise - PM only, at Client approved, a baseline, no live request, add off the scope, remove or change in it`() {
        val ok = "Client approved"
        val log = listOf(raise(1, 90, "add", "x"))
        assertNull(Album.crRaise(pm, ok, demoScope, 80, CrKind.ADD, log))
        assertEquals(Refusal.CR_IN_SCOPE, Album.crRaise(pm, ok, demoScope, 10, CrKind.ADD, log), "E7: an add on a work in scope")
        assertNull(Album.crRaise(pm, ok, demoScope, 10, CrKind.REMOVE, log))
        assertNull(Album.crRaise(pm, ok, demoScope, 10, CrKind.CHANGE, log))
        assertEquals(Refusal.CR_OFF_SCOPE_REMOVE, Album.crRaise(pm, ok, demoScope, 80, CrKind.REMOVE, log))
        assertEquals(Refusal.CR_OFF_SCOPE_CHANGE, Album.crRaise(pm, ok, demoScope, 80, CrKind.CHANGE, log))
        for (k in CrKind.entries) assertEquals(Refusal.CR_LIVE, Album.crRaise(pm, ok, demoScope, 90, k, log), "$k on a live request")
        // critic G4: no baseline, no raise (S5: 409 "No approved scope yet")
        assertEquals(Refusal.CR_NO_BASELINE, Album.crRaise(pm, ok, null, 80, CrKind.ADD, log))
        assertEquals(Refusal.NOT_A_WORK, Album.crRaise(pm, ok, demoScope, 0, CrKind.ADD, log))
        // §14.8: before approval the scope changes through Revisions -> Approve, even when recorded work opened the chapters
        for (q in listOf(null, "", "Revisions", "Estimated", "Quoted"))
            assertEquals(Refusal.CR_NOT_APPROVED, Album.crRaise(pm, q, demoScope, 80, CrKind.ADD, log), "$q")
        assertNull(Album.crRaise(pm, " Client approved ", demoScope, 80, CrKind.ADD, log))
        // a request answered is no longer live: the work may take another
        assertNull(Album.crRaise(pm, ok, demoScope, 90, CrKind.ADD, log + clear(2, 90, "declined", "add", "2026-10-28")))
        // E8: Designer, Client and Vendor can neither raise nor answer
        for (v in listOf(designer, client, vendor)) {
            assertEquals(Refusal.CR_PM_ONLY, Album.crRaise(v, ok, demoScope, 80, CrKind.ADD, log), v.role)
            assertEquals(Refusal.CR_PM_ONLY, Album.crAnswer(v, ok, 90, CrOutcome.WITHDRAWN, "", log), v.role)
        }
    }

    @Test fun `crAnswer - PM only, at Client approved, a live request, a real day for approved and declined`() {
        val ok = "Client approved"
        val log = listOf(raise(1, 90, "add", "x"))
        assertNull(Album.crAnswer(pm, ok, 90, CrOutcome.APPROVED, "2026-10-27", log))
        assertNull(Album.crAnswer(pm, ok, 90, CrOutcome.DECLINED, " 2026-10-27 ", log))
        assertNull(Album.crAnswer(pm, ok, 90, CrOutcome.WITHDRAWN, "", log), "only a withdrawal may leave the day out")
        // never a silent today: S5's 400 in its own words (refuse.album.client_at_day)
        for (bad in listOf("", "today", "2026-02-30", "27/10/2026")) {
            assertEquals(Refusal.CLIENT_AT_DAY, Album.crAnswer(pm, ok, 90, CrOutcome.APPROVED, bad, log), bad)
            assertEquals(Refusal.CLIENT_AT_DAY, Album.crAnswer(pm, ok, 90, CrOutcome.DECLINED, bad, log), bad)
        }
        assertEquals(Refusal.CR_NONE, Album.crAnswer(pm, ok, 80, CrOutcome.APPROVED, "2026-10-27", log))
        assertEquals(Refusal.CR_NONE, Album.crAnswer(pm, ok, 90, CrOutcome.WITHDRAWN, "", log + clear(2, 90)))
        assertEquals(Refusal.NOT_A_WORK, Album.crAnswer(pm, ok, 45, CrOutcome.WITHDRAWN, "", log))
        for (q in listOf(null, "Revisions")) assertEquals(Refusal.CR_NOT_APPROVED, Album.crAnswer(pm, q, 90, CrOutcome.WITHDRAWN, "", log), "$q")
    }

    @Test fun `photoRecord - staff into the quote journey always, into a work when open - a vendor into mine - a client never`() {
        for (v in listOf(pm, designer)) {
            assertNull(Album.photoRecord(v, false, 0), v.role)
            assertNull(Album.photoRecord(v, true, 0), v.role)
            assertEquals(Refusal.EXEC_LOCKED, Album.photoRecord(v, false, 10), v.role)
            assertNull(Album.photoRecord(v, true, 10), v.role)
            assertEquals(Refusal.NOT_A_WORK, Album.photoRecord(v, true, 45), v.role)
        }
        assertNull(Album.photoRecord(vendor, true, 50))
        assertNull(Album.photoRecord(vendor, false, 50), "a vendor's upload has no exec gate (S5: mine else 403)")
        assertEquals(Refusal.NOT_YOUR_WORK, Album.photoRecord(vendor, true, 40))
        assertEquals(Refusal.NOT_YOUR_WORK, Album.photoRecord(vendor, true, 0))
        assertEquals(Refusal.PHOTO_NO_CLIENT, Album.photoRecord(client, true, 0))
        assertEquals(Refusal.PHOTO_NO_CLIENT, Album.photoRecord(client, true, 10))
    }

    @Test fun `review - PM only, a vendor's row only, its JPEG landed, accepted or internal, the same state is a no-op`() {
        val p = vend("S7", 50, "Demo Waterproofing Co.")
        assertNull(Album.review(pm, p, Album.REVIEW_ACCEPTED))
        assertNull(Album.review(pm, p, Album.REVIEW_INTERNAL))
        assertEquals(Refusal.REVIEW_SAME, Album.review(pm, p.copy(review = Album.REVIEW_ACCEPTED), Album.REVIEW_ACCEPTED))
        assertNull(Album.review(pm, p.copy(review = Album.REVIEW_ACCEPTED), Album.REVIEW_INTERNAL))
        assertEquals(Refusal.NOT_A_REVIEW, Album.review(pm, p, Album.REVIEW_PENDING))
        assertEquals(Refusal.REVIEW_VENDOR_ONLY, Album.review(pm, staff("S1", 10), Album.REVIEW_ACCEPTED))
        // S5 answers 409 until the JPEG has landed: a review is of the bytes the PM saw (K1 let it go)
        assertEquals(Refusal.REVIEW_NO_IMAGE, Album.review(pm, p.copy(bytes = 0), Album.REVIEW_ACCEPTED))
        assertEquals(Refusal.REVIEW_NO_IMAGE, Album.review(pm, p.copy(bytes = 0), Album.REVIEW_INTERNAL))
        for (v in listOf(designer, vendor, client)) assertEquals(Refusal.REVIEW_PM_ONLY, Album.review(v, p, Album.REVIEW_ACCEPTED), v.role)
    }

    // ---- ids and ops ---------------------------------------------------------------------

    @Test fun `mintId gives ALB- and 12 hex digits, one per op - two reviews of one photo never share one`() {
        val a = Album.mintId(); val b = Album.mintId()
        assertTrue(Album.ID_RE.matches(a), a)
        assertTrue(Album.ID_RE.matches(b), b)
        assertNotEquals(a, b)
        assertEquals(Album.mintId(Random(7)), Album.mintId(Random(7)), "the id is the random source's alone")
        assertTrue(Album.ID_RE.matches("ALB-0123456789ab"))
        assertFalse(Album.ID_RE.matches("ALB-0123456789AB"))
        val p = vendAt("ALB-000000000077", 50, "Demo Waterproofing Co.", "2026-10-21T06:00:00.000Z")
        val r1 = Album.reviewOp(p, Album.REVIEW_ACCEPTED); val r2 = Album.reviewOp(p, Album.REVIEW_INTERNAL)
        assertNotEquals(r1.id, r2.id)
        assertNotEquals(p.id, r1.id, "the review's log row has its own id, never the photo's")
        assertEquals(listOf(p.id, p.id), listOf(r1.photo, r2.photo))
        assertEquals("2026-10-21T06:00:00.000Z", r1.base, "a review's base is the review_at it saw")
        assertEquals("", Album.reviewOp(vend("S7", 50, "x"), Album.REVIEW_ACCEPTED).base, "never reviewed")
        // a photo op is the record and its JPEG: the photo's id is the op's
        val ph = Album.photoOp(90, "Kitchen", "units in")
        assertEquals(ph.id, ph.photo)
        assertTrue(Album.ID_RE.matches(ph.id))
    }

    private fun vendAt(id: String, work: Int?, vendor: String, reviewAtFor: String?) =
        Photo(id, work, "2026-10-21T05:50:00.000Z", Album.PARTY_VENDOR, vendor = vendor, by = "v@x", bytes = 1000, reviewAt = reviewAtFor)

    @Test fun `the ops carry S5's base - the work's newest seq of any kind, never a provisional one`() {
        val m = View(Album.PM, works = listOf(Work(10, "Done", at = "2026-10-01T04:00:00.000Z", seq = 6), Work(50, seq = 0)),
            log = listOf(state(2, 10, "Done", "In progress"), Entry(9, "ALB-r", Album.KIND_REVIEW, 50, Album.REVIEW_ACCEPTED, photo = "S7"),
                Entry(Album.PENDING_SEQ, "ALB-p", Album.KIND_STATE, 50, "In progress")))
        assertEquals(6L, Album.baseOf(m, 10), "works[].seq, above the log page's")
        assertEquals(9L, Album.baseOf(m, 50), "a review row moves the base; a provisional row never does")
        assertEquals(0L, Album.baseOf(m, 90))
        assertEquals(0L, Album.baseOf(null, 10))
        val s = Album.stateOp(m, 10, "Done", "On hold", "snag", Random(1))
        assertEquals(Op(s.id, Album.KIND_STATE, 10, "6", "On hold", "Done", "snag"), s)
        assertEquals(Op(Album.crRaiseOp(m, 50, CrKind.CHANGE, "membrane", Random(2)).id, Album.KIND_CR_RAISE, 50, "9", "change", note = "membrane"),
            Album.crRaiseOp(m, 50, CrKind.CHANGE, "membrane", Random(2)))
        val live = listOf(raise(3, 90, "remove"))
        val a = Album.crAnswerOp(m, live, 90, CrOutcome.APPROVED, " 2026-10-27 ", "on a call", Random(3))
        assertEquals(listOf("approved", "remove", "on a call", "2026-10-27", "0"), listOf(a.value, a.prev, a.note, a.clientAt, a.base))
        assertEquals("", Album.crAnswerOp(m, live, 90, CrOutcome.WITHDRAWN, "", "", Random(4)).clientAt)
        assertEquals(Album.MAX_NOTE, Album.stateOp(m, 10, "Done", "On hold", "x".repeat(600)).note.length)
    }

    // ---- the outbox and the verdict table -------------------------------------------

    private val opA = Op("ALB-a", Album.KIND_STATE, 10, "2", "In progress", "Not started")
    private val opB = Op("ALB-b", Album.KIND_STATE, 10, "2", "Done", "In progress")
    private val opC = Op("ALB-c", Album.KIND_CR_RAISE, 90, "4", "add", note = "more units")
    private val opD = Op("ALB-d", Album.OP_PHOTO, 10, "", photo = "ALB-d", room = "Kitchen", caption = "debris out")
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

    @Test fun `afterPost - the op leaves the queue, the mirror takes the entry, the work row, the photo and the scope`() {
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
        // every log POST answer carries the scope as it now stands: an approved add has just moved it
        val n4 = Album.afterPost(Outbox(mirror, listOf(opC)), opC, Answer(scope = demoScope + 90, scopeAt = "2026-10-28T05:00:00.000Z"))
        assertEquals(demoScope + 90, n4.mirror!!.scope)
        assertEquals("2026-10-28T05:00:00.000Z", n4.mirror!!.scopeAt)
        assertEquals(demoScope, Album.afterPost(Outbox(mirror, listOf(opC)), opC, Answer()).mirror!!.scope, "no scope in the answer: kept")
    }

    @Test fun `afterPost rebases the next op on the same work onto S5's new row - K1 left it on the stale base`() {
        val q = listOf(opA, Op("ALB-x", Album.KIND_STATE, 50, "3", "Done", "In progress"), opB, opD)
        val e = state(7, 10, "In progress", "Not started", at = "2026-10-24T05:00:00.000Z").copy(id = "ALB-a")
        val next = Album.afterPost(Outbox(mirror, q), opA, Answer(e))
        assertEquals(listOf("ALB-x", "ALB-b", "ALB-d"), next.queue.map { it.id })
        assertEquals("7", next.queue[1].base, "opB rides on opA's row (seq 7), not on the 2 both saw")
        assertEquals("In progress", next.queue[1].prev)
        assertEquals("3", next.queue[0].base, "another work's op is untouched")
        assertEquals(opD, next.queue[2], "a photo op has no base")
        // a replay answers the same row: the same rebase
        assertEquals(next, Album.afterReplay(Outbox(mirror, q), opA, Answer(e)))
        // no entry in the answer: nothing to ride on, the base stays
        assertEquals("2", Album.afterPost(Outbox(mirror, q), opA, Answer()).queue[1].base)
    }

    @Test fun `a review's answer carries its log row - the next state op on its work takes that seq, the next review of the photo its review_at`() {
        val rv = Op("ALB-r1", Album.OP_REVIEW, 50, "", Album.REVIEW_ACCEPTED, photo = "S7")
        val st = Op("ALB-s", Album.KIND_STATE, 50, "3", "Done", "In progress")
        val rv2 = Op("ALB-r2", Album.OP_REVIEW, 50, "", Album.REVIEW_INTERNAL, photo = "S7")
        val row = Entry(9, "ALB-r1", Album.KIND_REVIEW, 50, Album.REVIEW_ACCEPTED, Album.REVIEW_PENDING, photo = "S7", at = "2026-10-24T06:00:00.000Z")
        val photo = demoItems.first { it.id == "S7" }.copy(review = Album.REVIEW_ACCEPTED, reviewAt = "2026-10-24T06:00:00.000Z")
        val next = Album.afterPost(Outbox(mirror, listOf(rv, st, rv2)), rv, Answer(entry = row, photo = photo))
        assertEquals(listOf("ALB-s", "ALB-r2"), next.queue.map { it.id })
        assertEquals("9", next.queue[0].base)
        assertEquals("2026-10-24T06:00:00.000Z", next.queue[1].base)
        assertEquals(Album.REVIEW_ACCEPTED, next.mirror!!.photos.first { it.id == "S7" }.review)
    }

    @Test fun `a three-op offline chain on one work - posted, refused, posted - each op on the base S5 really has`() {
        val m = View(Album.PM, works = listOf(Work(10, "Done", at = "2026-10-01T04:00:00.000Z", seq = 6)), log = listOf(state(6, 10, "Done", "In progress")))
        val op1 = Album.stateOp(m, 10, "Done", "On hold", "snag", Random(11))
        val op2 = Album.stateOp(m, 10, "On hold", "In progress", "", Random(12))
        val op3 = Album.stateOp(m, 10, "In progress", "Done", "", Random(13))
        val other = Op("ALB-o", Album.KIND_STATE, 50, "0", "In progress", "Not started")
        assertEquals(listOf("6", "6", "6"), listOf(op1, op2, op3).map { it.base }, "all three were queued offline against seq 6")
        assertEquals(3, setOf(op1.id, op2.id, op3.id).size)
        var o = Outbox(m, listOf(op1, other, op2, op3))
        // 1. op1 posts as seq 7: op2 rides on it
        o = Album.afterPost(o, op1, Answer(state(7, 10, "On hold", "Done", "snag").copy(id = op1.id)))
        assertEquals(listOf(other.id, op2.id, op3.id), o.queue.map { it.id })
        assertEquals("7", o.queue[1].base)
        assertEquals("6", o.queue[2].base, "op3 waits for op2")
        // 2. op2 is refused for good (S5 wrote nothing): op3 inherits op2's base and prev -- what S5 still has
        val sent2 = o.queue[1]
        o = Album.afterRefused(o, sent2, "refused")
        assertEquals(listOf(other.id, op3.id), o.queue.map { it.id })
        assertEquals(op3.copy(base = "7", prev = "On hold"), o.queue[1])
        assertEquals(listOf(sent2.copy(error = "refused")), o.dropped)
        // 3. op3 posts On hold -> Done as seq 8
        val sent3 = o.queue[1]
        o = Album.afterPost(o, sent3, Answer(state(8, 10, "Done", "On hold").copy(id = op3.id)))
        assertEquals(listOf(other.id), o.queue.map { it.id })
        assertEquals("Done", Album.stateOf(o.mirror!!.log, 10))
        assertEquals(listOf(6L, 7L, 8L), o.mirror!!.log.map { it.seq })
        assertEquals(other, o.queue.single(), "the other work's op never moved")
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
        // a 409 on an answer carries the scope as stored (asis.js `...sc`): the mirror takes it
        val n4 = Album.afterConflict(box, opC, "The scope was changed meanwhile", Answer(scope = demoScope + 85, scopeAt = "2026-10-29T05:00:00.000Z"))
        assertEquals(demoScope + 85, n4.mirror!!.scope)
        // an op not in the queue: nothing moves
        assertEquals(box, Album.afterConflict(box, Op("ALB-q", Album.KIND_STATE, 20), "x", Answer()))
    }

    @Test fun `afterLocked - S5 answered the banner - the op and later same-work ops are dropped saying so, the mirror as it was`() {
        val banner = Refusal.EXEC_LOCKED.text(Tx.EN)
        val next = Album.afterLocked(box, opA, banner)
        assertEquals(listOf("ALB-c", "ALB-e"), next.queue.map { it.id })
        assertEquals(listOf("ALB-a", "ALB-b", "ALB-d"), next.dropped.map { it.id })
        assertTrue(next.dropped.all { it.error == "Work starts once the client approves the estimate" })
        assertEquals(mirror, next.mirror)
        // the next op on another work gets its own verdict on this pass
        val n2 = Album.afterLocked(next, opC, banner)
        assertEquals(listOf("ALB-e"), n2.queue.map { it.id })
        assertEquals(4, n2.dropped.size)
    }

    @Test fun `afterRefused - dropped for good with one note - the next op that rode on it takes back S5's base and prev`() {
        val next = Album.afterRefused(box, opA, "A reason is needed to change a Done work")
        // opB (the next state op on 10) inherits opA's base and prev; c, d, e stay as they were
        assertEquals(listOf(opB.copy(base = "2", prev = "Not started"), opC, opD, opE), next.queue)
        assertEquals(listOf(opA.copy(error = "A reason is needed to change a Done work")), next.dropped)
        assertEquals(mirror, next.mirror)
        assertEquals(next, Album.afterRefused(next, opA, "again"), "already dropped: nothing moves, no second note")
        // the last state op on 10 refused: nothing after it on 10 carries a base (the photo has none)
        assertEquals(listOf(opA, opC, opD, opE), Album.afterRefused(box, opB, "x").queue)
        // a refused change request gives the next state op its base, never its prev (a CR is not a state)
        val q = listOf(Op("ALB-k", Album.KIND_CR_RAISE, 10, "5", "remove"), Op("ALB-l", Album.KIND_STATE, 10, "9", "Done", "In progress"))
        assertEquals(Op("ALB-l", Album.KIND_STATE, 10, "5", "Done", "In progress"), Album.afterRefused(Outbox(mirror, q), q[0], "x").queue.single())
        // a refused review hands its review_at to the next review of the same photo
        val r1 = Op("ALB-r1", Album.OP_REVIEW, 50, "2026-10-21T06:00:00.000Z", Album.REVIEW_ACCEPTED, photo = "S7")
        val r2 = Op("ALB-r2", Album.OP_REVIEW, 50, "stale", Album.REVIEW_INTERNAL, photo = "S7")
        assertEquals(r2.copy(base = "2026-10-21T06:00:00.000Z"), Album.afterRefused(Outbox(mirror, listOf(r1, r2)), r1, "x").queue.single())
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
        // a queued answer reads whole, as S5's row will: the outcome and the kind it answered
        val ans = Album.provisional(Album.crAnswerOp(mirror, demoLog, 90, CrOutcome.APPROVED, "2026-10-27", "call"), 3, Album.PM, "pm@x", "P M", "2026-10-28T05:00:00.000Z")
        assertEquals(CrOutcome.APPROVED to CrKind.ADD, ans.crOutcome to ans.crKind)
        assertEquals("2026-10-27", Album.crDone(demoLog + ans, 90).single().day)
        assertNull(Album.crOf(demoLog + ans, 90), "answered: no longer live")
        // a queued photo: an item on its work, the party by the role, bytes 0 (its JPEG is on the phone)
        val p = Album.provisionalPhoto(opD, Album.DESIGNER, "d@x", "D", "2026-10-24T05:03:00.000Z")
        assertEquals(Photo("ALB-d", 10, "2026-10-24T05:03:00.000Z", Album.PARTY_STAFF, "Kitchen", caption = "debris out", role = Album.DESIGNER, by = "d@x", byName = "D"), p)
        assertTrue(Album.canSee(client, p))
        val vp = Album.provisionalPhoto(Op("ALB-v", Album.OP_PHOTO, 50), Album.VENDOR, "v@x", "V", "2026-10-24T05:04:00.000Z", "Demo Waterproofing Co.")
        assertEquals(Album.PARTY_VENDOR, vp.party)
        assertTrue(Album.canSee(vendor, vp))
        assertFalse(Album.canSee(client, vp), "pending until the PM accepts it")
    }

    // ---- the log, paged ---------------------------------------------------------------

    @Test fun `mergedLog - every row once by id, the later page's copy, in seq order`() {
        val a = listOf(state(3, 10, "Done"), state(1, 10, "In progress"))
        val b = listOf(state(3, 10, "Done", note = "again"), state(2, 50, "In progress"))
        val m = Album.mergedLog(a, b)
        assertEquals(listOf(1L, 2L, 3L), m.map { it.seq })
        assertEquals("again", m.last().note)
    }

    @Test fun `450 log rows paged 200, 200, 50 derive the same states and bases as S5's works`() {
        // a synthetic log: state moves, change requests and reviews over five works, as S5's seq numbers them
        val rows = (1L..450L).map { s ->
            val w = listOf(10, 20, 50, 90, 140)[(s % 5).toInt()]
            when (s % 7) {
                0L -> raise(s, w, "add").copy(id = "ALB-%012x".format(s))
                1L -> clear(s, w, "declined", "add", "2026-10-27").copy(id = "ALB-%012x".format(s))
                2L -> Entry(s, "ALB-%012x".format(s), Album.KIND_REVIEW, w, Album.REVIEW_ACCEPTED, photo = "P$s")
                else -> state(s, w, Album.STATES[((s / 7) % 5).toInt()], at = "2026-10-20T05:00:00.000Z").copy(id = "ALB-%012x".format(s))
            }
        }
        // S5's works[]: the newest state row's value and time per work, the base the newest seq of ANY kind
        val works = Album.WORKS.map { def ->
            val st = rows.filter { it.work == def.id && it.kind == Album.KIND_STATE }.maxByOrNull { it.seq }
            Work(def.id, st?.value ?: Album.NOT_STARTED, at = st?.at ?: "", seq = rows.filter { it.work == def.id }.maxOfOrNull { it.seq } ?: 0)
        }
        // ?since=0, then ?since=log_next while log_more; the second page overlaps the first by one row
        val p1 = View(Album.PM, works = works, log = rows.subList(0, 200), logNext = 200, logMore = true)
        val p2 = View(Album.PM, works = works, log = rows.subList(199, 400), logNext = 400, logMore = true)
        val p3 = View(Album.PM, works = works, log = rows.subList(400, 450), logNext = 450, logMore = false)
        var m = p1
        for (p in listOf(p2, p3)) { assertTrue(m.logMore); m = Album.paged(m, p) }
        assertFalse(m.logMore)
        assertEquals(450, m.log.size)
        assertEquals(rows.map { it.seq }, m.log.map { it.seq })
        assertEquals(Album.statesOfWorks(works).mapValues { it.value.state }, Album.statesOfLog(m.log).mapValues { it.value.state })
        for (w in Album.WORK_IDS) assertEquals(works.first { it.id == w }.seq, m.log.filter { it.work == w }.maxOfOrNull { it.seq } ?: 0L, "base of $w")
        // the live requests agree with S5's derivation too: a work's newest cr row
        for (w in listOf(10, 20, 50, 90, 140)) {
            val newestCr = rows.filter { it.work == w && it.kind in setOf(Album.KIND_CR_RAISE, Album.KIND_CR_CLEAR) }.maxByOrNull { it.seq }
            assertEquals(newestCr?.kind == Album.KIND_CR_RAISE, Album.crOf(m.log, w) != null, "cr of $w")
        }
        // the first page alone is behind S5 on every work: its newest rows are missing
        for (w in listOf(10, 20, 50, 90, 140)) assertTrue((p1.log.filter { it.work == w }.maxOfOrNull { it.seq } ?: 0L) < works.first { it.id == w }.seq, "page 1 of $w")
    }
}
