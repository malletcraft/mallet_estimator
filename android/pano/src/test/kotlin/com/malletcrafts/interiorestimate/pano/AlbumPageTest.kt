package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Album.Photo
import com.malletcrafts.interiorestimate.pano.Album.Source
import com.malletcrafts.interiorestimate.pano.Album.View
import com.malletcrafts.interiorestimate.pano.Album.Viewer
import com.malletcrafts.interiorestimate.pano.Album.WorkState
import com.malletcrafts.interiorestimate.pano.AlbumPage.Local
import com.malletcrafts.interiorestimate.pano.AlbumPage.Me
import com.malletcrafts.interiorestimate.pano.AlbumPage.Pic
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The Project Album, read-only (port 61c slice K3; DESIGN-port61c.md §18): the page's model, against the approved
 * prototype v61c.2 (index61c2.html, the ALB60 core and window.__mcftAlbum60) and S5's own album bodies ([S5Album], what
 * S5 QUOTE 9a51733 answered on its real SQL). Every capture, id and login here is invented (PROJ-T1, MCAP-T…, `pm@x`):
 * mallet_estimator is public. The English and the Marathi a pin expects are written out by hand: a pin is a literal.
 */
class AlbumPageTest {

    private fun view(s: String): View = assertNotNull(AlbumWire.view(MiniRead, MiniJson.parse(s)))

    private val me = Me("pm@x", "P M")
    private val now: Instant = Instant.parse("2026-11-03T06:00:00Z")
    private val read = "2026-11-03T05:20:00.000Z"

    private fun line(cache: QuoteStage.Cache?, phoneOnly: Boolean = false) = QuoteStage.projectLine(cache, phoneOnly, emptyList(), Tx.EN, now)

    /** The approved project of S5's scenario (PROJ-DEMO, Client approved, 10 Done, a change live on 50, 90's add approved). */
    private val approved = view(S5Album.GET_PM)
    private val approvedCache = QuoteStage.fromAlbum(approved, read)

    /** The project left at Survey (PROJ-LOCK, never moved: no stamp). */
    private val locked = view(S5Album.GET_LOCKED)
    private val lockedCache = QuoteStage.fromAlbum(locked, read)

    private fun local(id: String, room: String = "Kitchen", createdAt: Long = Instant.parse("2026-10-20T05:00:00Z").toEpochMilli(),
                      serverName: String? = null, work: Int? = 0, stage: String? = "", capturedAt: String = "", kind: String = "360") =
        Local(id, serverName, work, stage, createdAt, capturedAt, room, kind)

    private fun page(items: List<Photo>, mirror: View?, cache: QuoteStage.Cache?, phoneOnly: Boolean = false, tx: Tx = Tx.EN,
                     toggled: Map<Int, Boolean> = emptyMap(), logOpen: Boolean = false) =
        AlbumPage.page(tx, "Demo flat", items, mirror, cache, line(cache, phoneOnly), toggled, logOpen)

    // ---- THE ONE FILING ------------------------------------------------------------------------------------

    @Test fun `compose files a capture the phone holds by the phone, once - matched by device id, by serverName and by C3's captured_at`() {
        // by device id: S5 holds MCAP-DEMO-1 at 0; the phone re-filed it to Civil & masonry by its legacy stage
        val a = AlbumPage.compose(approved, listOf(local("MCAP-DEMO-1", work = null, stage = "Plaster & waterproofing")), me)
        val one = a.single { it.id == "MCAP-DEMO-1" }
        assertEquals(1, a.count { it.id == "MCAP-DEMO-1" }, "S5's copy is not listed beside the phone's")
        assertEquals(Source.PHONE, one.source)
        assertEquals(20, one.work, "the phone's filing, whatever S5's copy says")
        assertEquals("MCAP-DEMO-1", one.capture, "S5's name for it, for its face")
        assertEquals("2026-11-03T05:16:04.175Z", one.uploadedAt)
        // by its ERP name, which S5 keeps until C3 renames it
        val withName = approved.copy(captures = listOf(approved.captures.single().copy(id = "MEST-PH-T2")))
        val b = AlbumPage.compose(withName, listOf(local("MCAP-T2", serverName = "MEST-PH-T2")), me)
        assertEquals(listOf("MCAP-T2"), b.filter { it.kind == Album.PHOTO_360 }.map { it.id })
        assertEquals("MEST-PH-T2", b.single { it.id == "MCAP-T2" }.capture)
        // by C3's own key (the K3 critic, finding 8): a legacy-… row carries neither name; the same room and S5's
        // captured_at byte for byte the phone's AsIsPath.capturedAt
        val legacy = approved.copy(captures = listOf(approved.captures.single().copy(id = "legacy-20261011083000")))
        val c = AlbumPage.compose(legacy, listOf(local("MCAP-T3", capturedAt = "2026-10-11T08:30:00+05:30")), me)
        assertEquals(listOf("MCAP-T3"), c.filter { it.kind == Album.PHOTO_360 }.map { it.id })
        assertEquals("legacy-20261011083000", c.single { it.id == "MCAP-T3" }.capture)
        // the same captured_at in ANOTHER room is another capture (C3 is per room), and a byte off is no match
        assertEquals(2, AlbumPage.compose(legacy, listOf(local("MCAP-T3", room = "Hall", capturedAt = "2026-10-11T08:30:00+05:30")), me)
            .count { it.kind == Album.PHOTO_360 })
        assertEquals(2, AlbumPage.compose(legacy, listOf(local("MCAP-T3", capturedAt = "2026-10-11T08:30+05:30")), me)
            .count { it.kind == Album.PHOTO_360 })
        // C3 renames the NEWEST twin: that one is the face, and neither twin is listed beside the phone's copy
        val twins = approved.copy(captures = listOf(
            approved.captures.single().copy(id = "legacy-a", uploadedAt = "2026-11-01T05:00:00.000Z"),
            approved.captures.single().copy(id = "legacy-b", uploadedAt = "2026-11-02T05:00:00.000Z")))
        val d = AlbumPage.compose(twins, listOf(local("MCAP-T4", capturedAt = "2026-10-11T08:30:00+05:30")), me)
        assertEquals(listOf("MCAP-T4"), d.filter { it.kind == Album.PHOTO_360 }.map { it.id })
        assertEquals("legacy-b", d.first { it.id == "MCAP-T4" }.capture)
    }

    @Test fun `compose keeps a capture only S5 holds as S5 files it, and an unsent one as the phone's`() {
        val items = AlbumPage.compose(approved, listOf(local("MCAP-T5", work = null, stage = "Floor tiling", kind = "Photo")), me)
        val s5 = items.single { it.id == "MCAP-DEMO-1" }
        assertEquals(Source.CAPTURE, s5.source)
        assertEquals(0, s5.work, "S5's filing")
        val mine = items.single { it.id == "MCAP-T5" }
        assertEquals(Source.PHONE, mine.source)
        assertEquals(70, mine.work)
        assertNull(mine.capture, "not on S5 yet: no S5 name, so no face to fetch")
        assertEquals(Album.PHOTO, mine.kind, "the store's Photo is the album's photo")
        // S5's album photos are themselves
        assertEquals(setOf("ALB-000000000014", "ALB-00000000001e"), items.filter { it.source == Source.ALBUM }.map { it.id }.toSet())
        // no mirror: the phone's own alone
        assertEquals(listOf("MCAP-T5"), AlbumPage.compose(null, listOf(local("MCAP-T5")), me).map { it.id })
    }

    @Test fun `compose orders by instant then key, and two legacy ids in two rooms stay two items`() {
        val cap = approved.captures.single()
        val two = approved.copy(photos = emptyList(), captures = listOf(cap.copy(id = "legacy-20261011083000", room = "Kitchen"),
            cap.copy(id = "legacy-20261011083000", room = "Hall")))
        val items = AlbumPage.compose(two, listOf(local("MCAP-T6", createdAt = Instant.parse("2026-10-01T05:00:00Z").toEpochMilli())), me)
        assertEquals(listOf("PHONE|Kitchen|MCAP-T6", "CAPTURE|Hall|legacy-20261011083000", "CAPTURE|Kitchen|legacy-20261011083000"),
            items.map { AlbumPage.key(it) }, "the oldest first; one instant, then by key")
        val page = page(items, two, approvedCache)
        assertEquals(3, page.order.size)
        assertEquals(3, page.order.toSet().size, "three places in the walk")
    }

    @Test fun `who is the vendor for a vendor photo, by_name for a photo, S5's uploader for a capture it holds, the person signed in for one not on S5 yet`() {
        val items = AlbumPage.compose(approved, listOf(local("MCAP-DEMO-1"), local("MCAP-T7")), me)
        val p = page(items, approved, approvedCache)
        fun who(id: String) = p.cards.values.single { it.key.endsWith("|$id") }
        assertEquals("Demo Waterproofing Co.", who("ALB-00000000001e").who)
        assertTrue(who("ALB-00000000001e").vendor)
        assertEquals("Vendor", who("ALB-00000000001e").whoAria)
        assertEquals("P M", who("ALB-000000000014").who)
        assertEquals("Staff", who("ALB-000000000014").whoAria)
        assertEquals("P M", who("MCAP-DEMO-1").who, "S5's uploader pm@x is this login: its name")
        assertEquals("P M", who("MCAP-T7").who, "not on S5 yet: the person signed in")
        // another login signed in now: S5's uploader stays S5's, shown as its login while S5 sends no name for it
        val other = AlbumPage.compose(approved, listOf(local("MCAP-DEMO-1"), local("MCAP-T7")), Me("d@x", "D N"))
        val q = page(other, approved, approvedCache)
        assertEquals("pm@x", q.cards.values.single { it.key.endsWith("|MCAP-DEMO-1") }.who)
        assertEquals("D N", q.cards.values.single { it.key.endsWith("|MCAP-T7") }.who)
        // the K3 review, finding 3: S5's own name for the uploader, when it sends one, is kept -- for a capture the phone
        // also holds (K3 blanked it, so the pill fell back to the login) and for one only S5 holds
        val named = approved.copy(captures = listOf(approved.captures.single().copy(by = "d@x", byName = "D N")))
        assertEquals("D N", page(AlbumPage.compose(named, listOf(local("MCAP-DEMO-1")), me), named, approvedCache)
            .cards.values.single { it.key.endsWith("|MCAP-DEMO-1") }.who)
        assertEquals("D N", page(AlbumPage.compose(named, emptyList(), me), named, approvedCache)
            .cards.values.single { it.key.startsWith("CAPTURE") }.who)
        // nobody signed in (a legacy ERP key): the prototype's `esc(it.who||'')`, an empty pill whose TalkBack says Staff
        // (the K3 review, finding 2: K3 wrote `Staff` into it, which the prototype does not draw)
        val none = page(AlbumPage.compose(null, listOf(local("MCAP-T7")), Me("", "")), null, null, phoneOnly = true)
        assertEquals("", none.cards.values.single().who)
        assertEquals("Staff", none.cards.values.single().whoAria)
        // S5's own capture taken by another login: its login
        val theirs = AlbumPage.compose(approved.copy(captures = listOf(approved.captures.single().copy(by = "d@x"))), emptyList(), me)
        assertEquals("d@x", page(theirs, approved, approvedCache).cards.values.single { it.key.startsWith("CAPTURE") }.who)
    }

    // ---- the header ----------------------------------------------------------------------------------------

    @Test fun `the header - Viewing as from the mirror, none without it, the PM for a project S5 does not list - Open, No work open yet`() {
        val items = AlbumPage.compose(approved, emptyList(), me)
        val h = page(items, approved, approvedCache).header
        assertEquals("Viewing as Project Manager", h.viewingAs)
        assertEquals("No work open yet", h.open, "10 is Done, nothing In progress")
        assertEquals("Execution · 1 done", h.line, "the project line's first line, the project screen's own")
        assertEquals("Viewing as Designer", page(items, approved.copy(role = Album.DESIGNER), approvedCache).header.viewingAs)
        // a listed project whose album this phone has not read: the role is not said
        assertNull(page(items, null, approvedCache).header.viewingAs)
        // a project S5 does not list: S5's albumRole answers pm to staff with no project row (D3; the K3 critic, finding 11)
        assertEquals("Viewing as Project Manager", page(emptyList(), null, null, phoneOnly = true).header.viewingAs)
        val notListed = QuoteStage.Cache(listed = false, readAt = read)
        assertEquals("Viewing as Project Manager", page(emptyList(), null, notListed).header.viewingAs)
        // Open
        val open = approvedCache.copy(states = mapOf(50 to WorkState(Album.IN_PROGRESS, 12), 110 to WorkState(Album.IN_PROGRESS, 13)))
        assertEquals("Open: Waterproofing · Sanitaryware & CP fittings", page(items, approved, open).header.open)
        // a copy holding no states: the prototype's openWorks over no state row, `No work open yet` (the K3 review, finding 2)
        assertEquals("No work open yet", page(items, approved, approvedCache.copy(states = null)).header.open)
        assertEquals("No work open yet", page(emptyList(), null, null, phoneOnly = true).header.open, "nothing can be recorded on it")
    }

    @Test fun `To review is the PM's alone and only above zero, Change requests only above zero`() {
        val pendingPhoto = approved.copy(photos = approved.photos.map {
            if (it.party == Album.PARTY_VENDOR) it.copy(review = Album.REVIEW_PENDING, reviewBy = null, reviewByName = null, reviewAt = null) else it
        })
        val items = AlbumPage.compose(pendingPhoto, emptyList(), me)
        assertEquals("To review: 1", page(items, pendingPhoto, approvedCache).header.toReview)
        assertNull(page(items, pendingPhoto.copy(role = Album.DESIGNER), approvedCache).header.toReview, "a Designer reviews nothing")
        assertNull(page(AlbumPage.compose(approved, emptyList(), me), approved, approvedCache).header.toReview, "the vendor's photo is accepted")
        assertEquals("Change requests: 1", page(items, approved, approvedCache).header.changeRequests, "the change live on 50")
        assertNull(page(emptyList(), locked, lockedCache).header.changeRequests)
    }

    @Test fun `Work log counts state rows only, newest first by seq - always the prototype's toggle, (0) with no mirror and for a project S5 does not list`() {
        val items = AlbumPage.compose(approved, emptyList(), me)
        val closed = page(items, approved, approvedCache).header.log
        assertEquals("Work log (2)", closed.toggle, "the two state rows of 10, not the stage moves, the requests or the review")
        assertFalse(closed.open)
        assertEquals(emptyList(), closed.rows, "folded")
        val open = page(items, approved, approvedCache, logOpen = true).header.log
        assertEquals(listOf(7L, 6L), open.rows.map { it.seq })
        assertEquals("Demolition & site preparation → Done", open.rows[0].head)
        assertEquals("3 Nov, 10:46 · P M", open.rows[0].sub)
        assertEquals("debris out", open.rows[0].remark)
        assertEquals("Demolition & site preparation → In progress", open.rows[1].head)
        assertNull(open.empty)
        // a listed project with no mirror held: the prototype's toggle over the rows the phone holds, none (the K3 review,
        // finding 2: K3's `Work log · not read yet` with no tap is the prototype's to carry first)
        val unread = page(items, null, approvedCache, logOpen = true).header.log
        assertEquals("Work log (0)", unread.toggle)
        assertTrue(unread.open)
        assertEquals(emptyList(), unread.rows)
        assertEquals("No work state recorded yet", unread.empty)
        // a project S5 does not list: none can exist, which is true
        val local = page(emptyList(), null, null, phoneOnly = true, logOpen = true).header.log
        assertEquals("Work log (0)", local.toggle)
        assertEquals("No work state recorded yet", local.empty)
        assertEquals("No work state recorded yet", page(emptyList(), locked, lockedCache, logOpen = true).header.log.empty)
    }

    // ---- chapter 0 and the lock ------------------------------------------------------------------------------

    @Test fun `a project S5 does not list - no group now, none not reached, chapter 0 opens, every capture at Survey, and its works LOCK under the banner`() {
        val items = AlbumPage.compose(null, listOf(local("MCAP-T8", createdAt = Instant.parse("2026-10-01T05:00:00Z").toEpochMilli()),
            local("MCAP-T9", createdAt = Instant.parse("2026-11-01T05:00:00Z").toEpochMilli())), me)
        for (p in listOf(page(items, null, null, phoneOnly = true), page(items, null, QuoteStage.Cache(listed = false, readAt = read)))) {
            assertTrue(p.quote.groups.none { it.now != null })
            assertEquals(listOf("MCAP-T8", "MCAP-T9"), p.quote.groups[0].items.map { it.substringAfterLast('|') }, "at Survey, S5's default")
            assertEquals(listOf("no photos", "no photos", "no photos", "no photos", "no photos"), p.quote.groups.drop(1).map { it.empty })
            assertTrue(p.quote.open)
            assertTrue(page(emptyList(), null, null, phoneOnly = true).quote.open, "opens with no photo too (v61c.2)")
            // the K3 critic, finding 1: the prototype makes such a project at quote 'Survey', so its works lock
            assertEquals("Work starts once the client approves the estimate", p.banner)
            assertTrue(p.works.all { it.locked && !it.open })
            assertTrue(p.works.all { it.line == "locked until the estimate is approved" })
            assertEquals(List(16) { "Not started" }, p.works.map { it.stateText })
            assertTrue(page(items, null, null, phoneOnly = true, toggled = (1..16).associateWith { true }).works.none { it.open }, "a locked chapter takes no tap")
        }
    }

    @Test fun `an unread stage draws the prototype's own rule - Survey reached, the five after it not reached yet, none now - and leaves the works open as its execMode does`() {
        val items = AlbumPage.compose(null, listOf(local("MCAP-T10")), me)
        for (stage in listOf("", "On hold")) {
            val cache = QuoteStage.Cache(listed = true, stage = stage, readAt = read, states = emptyMap())
            val p = page(items, null, cache)
            assertTrue(p.quote.groups.none { it.now != null })
            // index61c2.html 7257 `cur<0?i===0:i<=cur` (the K3 review, finding 2): K3 had drawn `no photos` here
            assertEquals(List(5) { "not reached yet" }, p.quote.groups.drop(1).map { it.empty }, stage)
            assertEquals("1 photo", p.quote.groups[0].count, stage)
            assertNull(p.banner, stage)
            assertTrue(p.works.none { it.locked }, stage)
        }
    }

    @Test fun `the lock - the banner before chapter 1 and locked until the estimate is approved, the chapters taking no tap`() {
        val items = AlbumPage.compose(locked, emptyList(), me)
        for (stage in listOf("Survey", "Client wish list", "Vendor quoted", "Estimated", "Revisions")) {
            val p = page(items, locked, lockedCache.copy(stage = stage), toggled = (0..16).associateWith { true })
            assertEquals("Work starts once the client approves the estimate", p.banner, stage)
            assertTrue(p.works.all { it.locked && !it.open && it.line == "locked until the estimate is approved" }, stage)
            assertFalse(p.quote.locked, "chapter 0 never locks for staff")
        }
        // Survey since creation: S5 never timed it, so the now group says no `since` at all (the K3 critic, finding 6)
        val survey = page(items, locked, lockedCache)
        assertEquals("now", survey.quote.groups[0].now)
        assertNull(survey.quote.groups[0].since)
        assertEquals("1 photo", survey.quote.groups[0].count)
        assertEquals(listOf("not reached yet", "not reached yet", "not reached yet", "not reached yet", "not reached yet"),
            survey.quote.groups.drop(1).map { it.empty })
        // released at Client approved, and by any recorded state (D3)
        assertNull(page(items, locked, lockedCache.copy(stage = "Client approved", at = "2026-11-03T05:16:04.183Z")).banner)
        assertNull(page(items, locked, lockedCache.copy(states = mapOf(10 to WorkState(Album.NOT_STARTED, 3, "2026-11-01T05:00:00.000Z")))).banner)
    }

    @Test fun `since is drawn beside the current group only when the store holds its stamp, never for a stage named without it`() {
        val cap = Instant.parse("2026-11-04T05:00:00Z").toEpochMilli()
        val items = AlbumPage.compose(null, listOf(local("MCAP-T11", createdAt = cap)), me)
        val timed = QuoteStage.Cache(listed = true, stage = "Revisions", at = "2026-11-03T05:16:04.183Z", readAt = read, states = emptyMap())
        assertEquals("since 3 Nov", page(items, null, timed).quote.groups[4].since)
        assertEquals("1 photo", page(items, null, timed).quote.groups[4].count)
        assertEquals("3 Nov पासून", page(items, null, timed, tx = Tx.MR).quote.groups[4].since, "the months stay Jan-Dec in both languages")
        assertNull(page(items, null, timed.copy(atUnknown = true)).quote.groups.firstNotNullOfOrNull { it.since }, "named without its stamp")
        assertNull(page(items, null, timed.copy(at = "")).quote.groups.firstNotNullOfOrNull { it.since }, "never timed")
    }

    // ---- the works -----------------------------------------------------------------------------------------

    @Test fun `a chapter's line, state, change request, payment and decided requests at Client approved`() {
        val p = page(AlbumPage.compose(approved, emptyList(), me), approved, approvedCache)
        val w = p.works.associateBy { it.id }
        assertNull(p.banner)
        assertEquals("1 photo · last photo 3 Nov", w[50]!!.line)
        assertEquals("no photos yet", w[10]!!.line)
        assertEquals("no photos yet", w[10]!!.empty)
        assertEquals(Album.DONE, w[10]!!.state)
        assertEquals("Done", w[10]!!.stateText)
        assertEquals("State of Demolition & site preparation: Done", w[10]!!.stateAria)
        assertEquals("Chapter 1: Demolition & site preparation", w[10]!!.aria)
        assertEquals("change", w[50]!!.crChip)
        assertEquals("Change request · change: Waterproofing", w[50]!!.crAria)
        assertEquals("Change request · raised 3 Nov by P M · second coat", w[50]!!.crNote)
        assertEquals("Payment: Before Waterproofing starts · When Waterproofing is done · When Cleaning & handover is done", w[50]!!.payment)
        assertEquals("Payment milestones of Waterproofing: Before Waterproofing starts · When Waterproofing is done · When Cleaning & handover is done",
            w[50]!!.paymentAria)
        assertEquals(listOf("Change approved by client · 2 Nov · add"), w[90]!!.decided)
        assertNull(w[90]!!.crChip, "answered")
        assertEquals(10, w[90]!!.n)
    }

    @Test fun `a copy holding no states - every chip is the prototype's Not started, nothing is open, the album is not closed`() {
        // the K3 review, finding 2: K3 drew `not read yet` in the chip, a state the prototype does not have
        val cache = approvedCache.copy(states = null)
        val p = page(AlbumPage.compose(approved, emptyList(), me), approved, cache)
        assertTrue(p.works.all { it.state == Album.NOT_STARTED && it.stateText == "Not started" })
        assertTrue(p.works.none { it.now })
        assertFalse(p.end.closed)
        assertEquals("The album closes when Cleaning & handover is Done", p.end.text)
        assertNull(p.banner, "Client approved: the lock is the stage's alone")
        // the store's empty map is a read: Not started
        assertTrue(page(emptyList(), approved, approvedCache.copy(states = emptyMap())).works.all { it.stateText == "Not started" })
    }

    @Test fun `statesFor - the empty map for a project S5 does not list and for a copy holding none, the store's copy otherwise`() {
        assertEquals(emptyMap(), AlbumPage.statesFor(line(null, phoneOnly = true), null))
        assertEquals(emptyMap(), AlbumPage.statesFor(line(QuoteStage.Cache(listed = false, readAt = read)), QuoteStage.Cache(listed = false, readAt = read)))
        assertEquals(emptyMap(), AlbumPage.statesFor(line(approvedCache.copy(states = null)), approvedCache.copy(states = null)))
        assertEquals(emptyMap(), AlbumPage.statesFor(line(null), null), "never read")
        assertEquals(approvedCache.states, AlbumPage.statesFor(line(approvedCache), approvedCache))
    }

    @Test fun `the default open - a photo, now or a work In progress opens a chapter, a tap toggles it for the visit, a locked one never opens`() {
        val items = AlbumPage.compose(approved, emptyList(), me)
        val cache = approvedCache.copy(states = mapOf(110 to WorkState(Album.IN_PROGRESS, 12)))
        val p = page(items, approved, cache)
        val w = p.works.associateBy { it.id }
        assertTrue(p.quote.open, "a photo")
        assertTrue(w[50]!!.open, "a photo")
        assertTrue(w[110]!!.open, "In progress, no photo")
        assertTrue(w[110]!!.now)
        assertFalse(w[10]!!.open, "Done, no photo")
        val t = page(items, approved, cache, toggled = mapOf(0 to false, 1 to true, 5 to false)).works.associateBy { it.id }
        assertTrue(t[10]!!.open)
        assertFalse(t[50]!!.open)
        assertFalse(page(items, approved, cache, toggled = mapOf(0 to false)).quote.open)
        // chapter 0 with nothing in it and no current group stays folded
        assertFalse(page(emptyList(), locked, lockedCache.copy(stage = "")).quote.open)
    }

    @Test fun `a tap sets only each chapter's open and the log's rows on the model - the chapters, cards, walk and groups are the model's own (the K3 review, finding 4)`() {
        val items = AlbumPage.compose(approved, emptyList(), me)
        val m = AlbumPage.model(Tx.EN, "Demo flat", items, approved, approvedCache, line(approvedCache))
        assertEquals(page(items, approved, approvedCache), m.base, "the model's base is the page with no tap")
        val taps = mapOf(0 to false, 1 to true, 5 to false)
        val t = AlbumPage.page(m, taps, logOpen = true)
        // the same objects, not equal copies: nothing heavy is rebuilt for a tap
        assertSame(m.base.cards, t.cards)
        assertSame(m.base.items, t.items)
        assertSame(m.base.order, t.order)
        assertSame(m.base.groupOf, t.groupOf)
        assertSame(m.base.quote.groups, t.quote.groups)
        // and it is the page the whole build gives for the same taps
        assertEquals(page(items, approved, approvedCache, toggled = taps, logOpen = true), t)
        assertEquals(listOf(7L, 6L), t.header.log.rows.map { it.seq })
        assertSame(m.base, AlbumPage.page(m), "no tap: the base itself")
        // a locked chapter still takes no tap
        val lm = AlbumPage.model(Tx.EN, "Demo flat", AlbumPage.compose(locked, emptyList(), me), locked, lockedCache, line(lockedCache))
        assertTrue(AlbumPage.page(lm, (1..16).associateWith { true }).works.none { it.open })
    }

    @Test fun `the end line, closed with the day 140 went Done`() {
        val done = approvedCache.copy(states = mapOf(140 to WorkState(Album.DONE, 30, "2026-12-20T05:00:00.000Z")))
        val e = page(emptyList(), approved, done).end
        assertTrue(e.closed)
        assertEquals("Album closed · Cleaning & handover is Done", e.text)
        assertEquals("20 Dec", e.day)
        assertEquals("अल्बम पूर्ण झाला · \"स्वच्छता व हस्तांतरण\" पूर्ण", page(emptyList(), approved, done, tx = Tx.MR).end.text)
    }

    @Test fun `S5's more draws no line of its own, the empty line only when nothing is visible`() {
        val items = AlbumPage.compose(approved, emptyList(), me)
        // the K3 review, finding 2: `Showing the newest N photos` is a line the prototype does not have, so it waits for
        // the prototype; S5's `more` changes nothing on the page until then
        assertEquals(page(items, approved, approvedCache), page(items, approved.copy(more = true), approvedCache))
        assertNull(page(items, approved, approvedCache).empty)
        assertEquals("No photos yet — the survey's 360s come first", page(emptyList(), null, null, phoneOnly = true).empty)
    }

    // ---- the cards, the viewer, the tile ---------------------------------------------------------------------

    @Test fun `a card - the 360 tag, the store's kinds, the day alone for a capture and the day and time for an album photo`() {
        val items = AlbumPage.compose(approved, listOf(local("MCAP-T12", kind = "Photo", room = "Living Room")), me)
        val p = page(items, approved, approvedCache)
        fun c(id: String) = p.cards.values.single { it.key.endsWith("|$id") }
        assertTrue(c("MCAP-DEMO-1").tag360)
        assertFalse(c("MCAP-T12").tag360)
        assertEquals("11 Oct", c("MCAP-DEMO-1").whenText, "S5's captured_at, the day alone")
        assertEquals("20 Oct", c("MCAP-T12").whenText, "the shutter instant, the day alone")
        assertEquals("3 Nov, 10:46", c("ALB-000000000014").whenText)
        assertEquals("Quote journey", c("MCAP-DEMO-1").work)
        assertEquals("Onsite carpentry & panelling", c("ALB-000000000014").work)
        assertEquals("Living Room", c("MCAP-T12").room)
        assertEquals("Whole project", c("ALB-00000000001e").room, "no room")
        assertEquals("हॉल", page(items, approved, approvedCache, tx = Tx.MR).cards.values.single { it.key.endsWith("|MCAP-T12") }.room)
        // the badge: a pending vendor photo, never to a client
        val pend = Photo("ALB-p", 50, "2026-11-03T05:00:00.000Z", Album.PARTY_VENDOR, vendor = "Demo Waterproofing Co.", bytes = 9)
        assertEquals("awaiting review", AlbumPage.card(Tx.EN, Viewer(Album.PM), pend).badge)
        assertTrue(AlbumPage.card(Tx.EN, Viewer(Album.PM), pend).pending)
        assertEquals("Not for client", AlbumPage.card(Tx.EN, Viewer(Album.PM), pend.copy(review = Album.REVIEW_INTERNAL)).badge)
        assertNull(AlbumPage.card(Tx.EN, Viewer(Album.CLIENT), pend).badge)
        assertNull(AlbumPage.card(Tx.EN, Viewer(Album.PM), pend.copy(review = Album.REVIEW_ACCEPTED)).badge)
    }

    @Test fun `the viewer walks the order, names the group a quote-journey photo sits in, and draws no review line to a client`() {
        val items = AlbumPage.compose(approved, emptyList(), me)
        val p = page(items, approved, approvedCache)
        assertEquals(listOf("MCAP-DEMO-1", "ALB-00000000001e", "ALB-000000000014"), p.order.map { it.substringAfterLast('|') }, "chapter 0, then 50, then 95")
        val first = assertNotNull(AlbumPage.shown(Tx.EN, p, p.order[0]))
        assertEquals("Quote journey", first.title)
        assertEquals("Photo 1 of 3", first.count)
        assertEquals("Quote journey · Survey", first.chapterLine, "taken before any logged move")
        assertNull(first.prev)
        assertEquals(p.order[1], first.next)
        assertEquals("No caption", first.caption)
        assertFalse(first.captionIsData)
        assertNull(first.payment)
        assertNull(first.review)
        val v = assertNotNull(AlbumPage.shown(Tx.EN, p, p.order[1]))
        assertEquals("Waterproofing", v.title)
        assertEquals("Photo 2 of 3", v.count)
        assertEquals("Waterproofing", v.chapterLine)
        assertEquals("Change request · change", v.cr)
        assertEquals("Payment: Before Waterproofing starts · When Waterproofing is done · When Cleaning & handover is done", v.payment)
        assertEquals("membrane, second coat", v.caption)
        assertTrue(v.captionIsData)
        assertEquals("Accepted by P M · 3 Nov, 10:46", v.review)
        assertEquals("स्वीकारणारे: P M · 3 Nov, 10:46", assertNotNull(AlbumPage.shown(Tx.MR, page(items, approved, approvedCache, tx = Tx.MR), p.order[1])).review)
        val last = assertNotNull(AlbumPage.shown(Tx.EN, p, p.order[2]))
        assertNull(last.next)
        assertNull(last.cr)
        assertNull(AlbumPage.shown(Tx.EN, p, "ALBUM||ALB-gone"), "a photo gone from the walk closes the viewer")
        // the client: S5 blanks the vendor's names, and no review line or badge is drawn
        val cv = view(S5Album.GET_CLIENT)
        val cp = page(AlbumPage.compose(cv, emptyList(), me), cv, QuoteStage.fromAlbum(cv, read))
        val vp = cp.order.first { it.endsWith("|ALB-00000000001e") }
        assertNull(assertNotNull(AlbumPage.shown(Tx.EN, cp, vp)).review)
        assertEquals("", cp.cards.getValue(vp).who, "the prototype's empty pill: S5 blanked the vendor's names")
        assertEquals("Vendor", cp.cards.getValue(vp).whoAria)
    }

    @Test fun `the tile counts what this person may see, with its TalkBack label`() {
        val items = AlbumPage.compose(approved, listOf(local("MCAP-T13")), me)
        val t = AlbumPage.tile(Tx.EN, "Demo flat", approved, line(approvedCache), items)
        assertEquals(4, t.count)
        assertEquals("Album", t.label)
        assertEquals("Album of Demo flat: 4 photos, survey to handover", t.aria)
        assertEquals("Demo flat चा अल्बम: 4 फोटो, सर्व्हेपासून हस्तांतरणापर्यंत", AlbumPage.tile(Tx.MR, "Demo flat", approved, line(approvedCache), items).aria)
        assertEquals(1, AlbumPage.tile(Tx.EN, "Demo flat", null, line(null, phoneOnly = true), listOf(items.first())).count)
        assertEquals("Album of Demo flat: 1 photo, survey to handover",
            AlbumPage.tile(Tx.EN, "Demo flat", null, line(null, phoneOnly = true), listOf(items.first())).aria)
    }

    @Test fun `the image rule - the phone's file, else S5's front face, else none - an album photo's own address, each segment encoded once`() {
        val items = AlbumPage.compose(approved, listOf(local("MCAP-DEMO-1"), local("MCAP-T14")), me)
        val held = items.single { it.id == "MCAP-DEMO-1" }
        val unsent = items.single { it.id == "MCAP-T14" }
        assertEquals(Pic.OnPhone("MCAP-DEMO-1"), AlbumPage.pic(held, localFile = true))
        assertEquals(Pic.Face("Kitchen", "MCAP-DEMO-1", "2026-11-03T05:16:04.175Z"), AlbumPage.pic(held, localFile = false), "its local copy removed")
        assertEquals(Pic.None, AlbumPage.pic(unsent, localFile = false), "neither")
        val s5Only = AlbumPage.compose(approved, emptyList(), me).single { it.source == Source.CAPTURE }
        assertEquals(Pic.Face("Kitchen", "MCAP-DEMO-1", "2026-11-03T05:16:04.175Z"), AlbumPage.pic(s5Only, localFile = false))
        assertEquals(Pic.None, AlbumPage.pic(s5Only.copy(room = null), localFile = false))
        val photo = items.single { it.id == "ALB-000000000014" }
        assertEquals(Pic.AlbumJpeg("ALB-000000000014"), AlbumPage.pic(photo, localFile = true))
        assertEquals(Pic.None, AlbumPage.pic(photo.copy(bytes = 0), localFile = false))
        // the addresses: AlbumQueue's photo segments and AsIsPath's explicit face route, each segment through enc ONCE
        val enc: (String) -> String = { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
        assertEquals("/api/asis/PROJ-T1/album/photos/ALB-000000000014/img", AsIsPath.path(AlbumPage.photoImage("PROJ-T1", "ALB-000000000014"), enc))
        assertEquals("/api/asis/PROJ-T1/Living%20Room/c/MCAP-T1/img/front", AsIsPath.path(AlbumPage.faceImage("PROJ-T1", "Living Room", "MCAP-T1"), enc))
        assertEquals("/api/asis/PROJ%20T1/Room%2F1/c/legacy-1/img/front", AsIsPath.path(AlbumPage.faceImage("PROJ T1", "Room/1", "legacy-1"), enc))
    }

    @Test fun `the cache's trim - a temp file a minute old goes, a younger one stays uncounted, then the oldest drawn down to 192 MB (the K3 review, finding 6)`() {
        val mb = 1024L * 1024
        val now = 1_000_000_000L
        // under the cap: only the orphaned temp file goes
        val small = listOf(AlbumPage.CacheFile("a.jpg", 10 * mb, now - 5_000), AlbumPage.CacheFile("b.jpg.part", 8 * mb, now - 61_000),
            AlbumPage.CacheFile("c.jpg.part", 8 * mb, now - 2_000))
        assertEquals(listOf("b.jpg.part"), AlbumPage.trimmed(small, now))
        // a young temp file is a download in flight: neither deleted nor counted against the cap
        val busy = listOf(AlbumPage.CacheFile("a.jpg", 250 * mb, now - 5_000), AlbumPage.CacheFile("c.jpg.part", 8 * mb, now - 2_000))
        assertEquals(emptyList(), AlbumPage.trimmed(busy, now))
        // over 256 MB: the least recently drawn first, until under 192 MB
        val full = (1..30).map { i -> AlbumPage.CacheFile("f$i.jpg", 9 * mb, now - (31 - i) * 1_000L) } +
            AlbumPage.CacheFile("old.jpg.part", 8 * mb, now - 3_600_000)
        val gone = AlbumPage.trimmed(full, now)
        assertTrue("old.jpg.part" in gone)
        val left = full.filter { !it.name.endsWith(".part") && it.name !in gone }.sumOf { it.bytes }
        assertTrue(left <= AlbumPage.CACHE_TRIM_TO, "left $left")
        assertEquals((1..9).map { "f$it.jpg" }, gone.filter { it.endsWith(".jpg") }, "oldest first, and no more than needed")
    }

    // ---- :app, read as source (only CI compiles :app) ------------------------------------------------------

    private fun app(name: String): String {
        val f = java.io.File("../app/src/main/kotlin/com/malletcrafts/interiorestimate/$name")
        assertTrue(f.isFile, "the app's $name is beside :pano (${f.absolutePath})")
        return f.readText()
    }

    @Test fun `the app wires the album as section 18 says - the widened read, the branch, the Backs, the tile, the forget, and no write`() {
        val main = app("MainActivity.kt")
        assertTrue(main.contains("if (QuoteStageSync.cached(context, id)?.listed == true) {"), "the project open reads every listed project's album")
        assertTrue(app("SyncWorker.kt").contains("Phases.needsMirror(QuoteStageSync.cached(applicationContext, p.serverId)?.stage)"), "the hourly read stays")
        val album = main.indexOf("if (proj != null && albumFor != null && proj.key == albumFor) {")
        val wish = main.indexOf("if (proj != null && wishFor != null && proj.key == wishFor) {")
        assertTrue(album in 0 until wish, "the album's branch before the wish list's (index61c2.html 8271)")
        assertTrue(main.contains("BackHandler { leaveAlbum() }"))
        assertTrue(main.contains("album = albumTile)"))
        val screen = app("AlbumScreen.kt")
        assertTrue(screen.contains("BackHandler(enabled = view != null) { onView(null) }"), "the viewer's Back only while it is open (finding 12)")
        assertFalse(Regex("""import com\.malletcrafts\.interiorestimate\.pano\.AlbumPage\.""").containsMatchIn(screen + main), "AlbumPage's types referenced qualified")
        assertTrue(app("BrowseScreens.kt").contains("album?.let { a -> item { AlbumTile(a) } }"), "the tile, last in the grid")
        assertTrue(app("AlbumSync.kt").contains("AlbumImages.forgetAll(context)"), "a sign-out forgets the pictures with the mirrors")
        // K3 writes nothing: no write method, no outbox, in the code (not the comments) of either new file
        fun code(src: String) = src.replace(Regex("""/\*[\s\S]*?\*/"""), "").lines().joinToString("\n") { it.substringBefore("//") }
        for (src in listOf(screen, app("AlbumImages.kt")).map { code(it) }) {
            assertTrue(src.contains("fun "), "the comment stripper left the code")
            assertFalse(listOf("\"PUT\"", "\"POST\"", "\"PATCH\"", "AlbumQueue", "AlbumSync.take").any { it in src }, "K3 writes nothing")
        }
    }

    @Test fun `the K3 review's app fixes, read as source - no space under a work's grid, the taps on the model, the album held above Search, the cache's trim and sign-out`() {
        val screen = app("AlbumScreen.kt")
        val main = app("MainActivity.kt")
        val images = app("AlbumImages.kt")
        // finding 1: an open work's grid ends at the next chapter; only a quote group and the unfiled photos keep 8 dp
        assertFalse(screen.contains("\"wp\""), "no spacer after a work's grid")
        assertTrue(screen.contains("item(key = \"gp:\" + g.stage) { Spacer(Modifier.height(8.dp)) }"))
        assertTrue(screen.contains("item(key = \"up\") { Spacer(Modifier.height(8.dp)) }"))
        // finding 2: no line of its own for S5's more, and the Work log always the prototype's toggle
        assertFalse(screen.contains("page.more") || screen.contains("toggleable"))
        // finding 4: the model is remembered on the data alone, and a tap applies to it
        assertTrue(main.contains("remember(tx, proj.title, albumItems, mirror, wishCache, projectLine) {\n                AlbumPage.model("))
        assertTrue(main.contains("remember(albumModel, toggled, albumLog) { AlbumPage.page(albumModel, toggled, albumLog) }"))
        // finding 5: the mirror, the kept files and the list's scroll are held above the search overlay's early return
        val search = main.indexOf("    if (showSearch) {")
        assertTrue(search > 0)
        for (held in listOf("var heldMirror by remember", "var heldFiles by remember", "var albumList by remember")) {
            assertTrue(main.indexOf(held) in 0 until search, "$held above Search")
        }
        assertFalse(main.contains("var mirror by remember(stageKey)"), "no mirror state below Search")
        assertTrue(main.contains("listState = albumList,"))
        assertTrue(screen.contains("LazyColumn(modifier.fillMaxWidth(), state = listState,"))
        // finding 6: one trim rule (AlbumPage.trimmed, the temp files included), a write that throws takes its temp file,
        // and the sign-out's deletes on a thread of their own
        assertTrue(images.contains("AlbumPage.trimmed("))
        assertFalse(images.contains("!it.name.endsWith(\".part\")"), "the trim no longer skips the temp files")
        assertTrue(images.contains("catch (e: Exception) { tmp.delete(); throw e }"))
        val forget = images.substring(images.indexOf("fun forgetAll("))
        assertTrue(forget.substringBefore("\n    }").contains("Thread {"), "the deletes off the main thread")
    }

    // ---- Marathi -------------------------------------------------------------------------------------------

    @Test fun `every new line and each header line in Marathi`() {
        val items = AlbumPage.compose(approved, emptyList(), me)
        val mr = page(items, approved, approvedCache, tx = Tx.MR)
        assertEquals("प्रोजेक्ट मॅनेजर म्हणून पाहत आहे", mr.header.viewingAs)
        assertEquals("अजून कोणतेही काम चालू नाही", mr.header.open)
        assertEquals("अजून कोणतेही काम चालू नाही", page(items, approved, approvedCache.copy(states = null), tx = Tx.MR).header.open)
        assertEquals("बदल विनंत्या: 1", mr.header.changeRequests)
        assertEquals("कामांची नोंद (2)", mr.header.log.toggle)
        assertEquals("कामांची नोंद (0)", page(items, null, approvedCache, tx = Tx.MR).header.log.toggle)
        val lockMr = page(emptyList(), locked, lockedCache, tx = Tx.MR)
        assertEquals("अंदाजपत्रक मंजूर होईपर्यंत लॉक", lockMr.works.first().line)
        assertEquals("ग्राहकाने अंदाजपत्रक मंजूर केल्यावर काम सुरू होते", lockMr.banner)
        val pendingPhoto = approved.copy(photos = approved.photos.map { if (it.party == Album.PARTY_VENDOR) it.copy(review = Album.REVIEW_PENDING) else it })
        assertEquals("तपासायचे: 1", page(AlbumPage.compose(pendingPhoto, emptyList(), me), pendingPhoto, approvedCache, tx = Tx.MR).header.toReview)
        assertEquals("अल्बम · Demo flat", mr.title)
        assertEquals("3 फोटो · सर्व्हेपासून हस्तांतरणापर्यंत", mr.sub)
    }
}
