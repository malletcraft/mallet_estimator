package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Album.CrKind
import com.malletcrafts.interiorestimate.pano.Album.CrOutcome
import com.malletcrafts.interiorestimate.pano.Album.Entry
import com.malletcrafts.interiorestimate.pano.Album.Op
import com.malletcrafts.interiorestimate.pano.Album.View
import com.malletcrafts.interiorestimate.pano.AlbumPage.Me
import com.malletcrafts.interiorestimate.pano.AlbumWrite.Out
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.time.Instant
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Project Manager's taps as ops (port 61c slice K4; DESIGN-port61c.md §19): each tap checked by K1b's gate on the
 * OVERLAID state and built on the MIRROR's bases, against S5's own album body ([S5Album].GET_PM: PROJ-DEMO at Client
 * approved, 10 Done at seq 7, 90's add approved, a change live on 50, a vendor photo on 50) and the approved prototype
 * v61c.2 (index61c2.html: setState 7220, raiseCR 7289, answerCR 7297, review 7279). Every id and login is invented
 * (PROJ-DEMO, `pm@x`): mallet_estimator is public. Every expected value is written out by hand.
 */
class AlbumWriteTest {

    private fun view(s: String): View = assertNotNull(AlbumWire.view(MiniRead, MiniJson.parse(s)))

    private val me = Me("pm@x", "P M")
    private val now: Instant = Instant.parse("2026-11-09T06:30:00Z")
    private val read = "2026-11-09T06:00:00.000Z"

    private val approved = view(S5Album.GET_PM)
    private val cache = QuoteStage.fromAlbum(approved, read)
    private val vendorKey = "ALBUM||ALB-00000000001e"

    /** S5's vendor photo, kept Not for client on [reviewAt] (so an Accept has a base to carry). */
    private fun kept(reviewAt: String = "2026-11-03T05:16:04.218Z") = approved.copy(photos = approved.photos.map {
        if (it.party == Album.PARTY_VENDOR) it.copy(review = Album.REVIEW_INTERNAL, reviewAt = reviewAt) else it
    })

    private fun model(
        mirror: View? = approved, c: QuoteStage.Cache? = cache, queue: List<Op> = emptyList(), who: Me = me, phoneOnly: Boolean = false,
    ): AlbumPage.Model {
        val line = QuoteStage.projectLine(c, phoneOnly, emptyList(), Tx.EN, now)
        return AlbumPage.model(Tx.EN, "Demo flat", AlbumPage.compose(mirror, emptyList(), who), mirror, c, line, queue, who)
    }

    private fun queued(o: Out): Op = assertIs<Out.Queue>(o).op

    @Test fun `a state tap is the op S5 takes - prev the overlaid state, base the mirror's, the tap's at, an ALB id`() {
        val op = queued(AlbumWrite.state(model(), 20, Album.IN_PROGRESS, "", now, Random(1)))
        assertEquals(Album.KIND_STATE, op.kind)
        assertEquals(20, op.work)
        assertEquals(Album.IN_PROGRESS, op.value)
        assertEquals(Album.NOT_STARTED, op.prev)
        assertEquals("0", op.base, "work 20 has no row on S5")
        assertEquals("2026-11-09T06:30:00Z", op.at)
        assertTrue(Album.ID_RE.matches(op.id), op.id)
        // leaving Done with a reason: prev Done, the base S5's 7, the reason trimmed as the note
        val reopen = queued(AlbumWrite.state(model(), 10, Album.IN_PROGRESS, " snag found ", now, Random(2)))
        assertEquals(listOf(Album.DONE, "7", "snag found"), listOf(reopen.prev, reopen.base, reopen.note))
        // the tap's time is never sent: S5 stamps its own
        val rq = assertNotNull(AlbumQueue.requestOf(reopen, "PROJ-DEMO"))
        assertFalse(rq.fields.containsKey("at"))
        assertEquals(7L, rq.fields["base"])
    }

    @Test fun `the current state writes nothing, leaving Done needs a reason, a locked chapter offers no write`() {
        assertEquals(Out.None, AlbumWrite.state(model(), 10, Album.DONE, "", now), "the current state closes the sheet (7544)")
        assertEquals(Out.Refused(Album.Refusal.DONE_REASON_NEED), AlbumWrite.state(model(), 10, Album.ON_HOLD, "   ", now))
        assertEquals("A reason is needed to change a Done work", Album.Refusal.DONE_REASON_NEED.text(Tx.EN))
        // PROJ-LOCK at Survey, no state row: every work locked, no chip a tap, no write
        val locked = view(S5Album.GET_LOCKED)
        val m = model(locked, QuoteStage.fromAlbum(locked, read))
        assertEquals(Out.None, AlbumWrite.state(m, 10, Album.IN_PROGRESS, "", now))
        assertTrue(m.base.works.none { it.stateTap })
        assertEquals(Out.None, AlbumWrite.state(model(), 15, Album.IN_PROGRESS, "", now), "not a work")
    }

    @Test fun `a chain of three taps offline is the chain S5 takes - each prev the one before, every base the mirror's`() {
        val a = queued(AlbumWrite.state(model(), 20, Album.IN_PROGRESS, "", now, Random(3)))
        val b = queued(AlbumWrite.state(model(queue = listOf(a)), 20, Album.ON_HOLD, "", now, Random(4)))
        val c = queued(AlbumWrite.state(model(queue = listOf(a, b)), 20, Album.IN_PROGRESS, "", now, Random(5)))
        assertEquals(listOf(Album.NOT_STARTED, Album.IN_PROGRESS, Album.ON_HOLD), listOf(a.prev, b.prev, c.prev))
        assertEquals(listOf("0", "0", "0"), listOf(a.base, b.base, c.base), "every base the mirror's: AQ rebases each link")
        // S5 takes the first at seq 12: the second rides on it, the third waits for the second's answer
        val e = Entry(12, a.id, Album.KIND_STATE, 20, Album.IN_PROGRESS, Album.NOT_STARTED)
        val o = Album.afterPost(Album.Outbox(approved, listOf(a, b, c)), a, Album.Answer(entry = e))
        assertEquals(listOf(b.copy(base = "12"), c), o.queue)
    }

    @Test fun `the raise kinds follow the overlaid scope - add off it, remove and change in it - and a queued approved add moves the work into it`() {
        val m = model()
        assertEquals(listOf(CrKind.ADD), AlbumWrite.kinds(m, 80), "80 is outside the approved scope")
        assertEquals(listOf(CrKind.REMOVE, CrKind.CHANGE), AlbumWrite.kinds(m, 20))
        val raise = queued(AlbumWrite.raise(m, 80, CrKind.ADD, " countertop added ", now, Random(6)))
        assertEquals(listOf(Album.KIND_CR_RAISE, "add", "countertop added", "0"), listOf(raise.kind, raise.value, raise.note, raise.base))
        // refused, nothing queued: an add on a work in the scope, any raise where one is live, a remove off the scope
        assertEquals(Out.None, AlbumWrite.raise(m, 20, CrKind.ADD, "", now))
        assertEquals(Out.None, AlbumWrite.raise(m, 50, CrKind.REMOVE, "", now), "50's change is live")
        assertEquals(Out.None, AlbumWrite.raise(m, 80, CrKind.REMOVE, "", now))
        // the raise queued: 80 now has a live request, so no second one, and the client's yes is queued on it
        val m2 = model(queue = listOf(raise))
        assertEquals(Out.None, AlbumWrite.raise(m2, 80, CrKind.ADD, "", now))
        val yes = queued(AlbumWrite.answer(m2, 80, CrOutcome.APPROVED, "2026-11-09", "a call", now, Random(7)))
        // the yes queued: 80 is in the overlaid scope, so its next request is a remove or a change
        val m3 = model(queue = listOf(raise, yes))
        assertEquals(listOf(CrKind.REMOVE, CrKind.CHANGE), AlbumWrite.kinds(m3, 80))
        assertEquals(listOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140),
            Album.scopeWithQueued(approved.scope, approved.scopeAt, listOf(raise, yes)).first)
    }

    @Test fun `an answer carries the kind it answers, from a raise still queued, and a date that is not a day is refused with S5's words`() {
        val raise = queued(AlbumWrite.raise(model(), 80, CrKind.ADD, "", now, Random(8)))
        val m = model(queue = listOf(raise))
        val yes = queued(AlbumWrite.answer(m, 80, CrOutcome.APPROVED, " 2026-11-09 ", "a call", now, Random(9)))
        assertEquals(listOf(Album.KIND_CR_CLEAR, "approved", "add", "2026-11-09", "a call", "0"),
            listOf(yes.kind, yes.value, yes.prev, yes.clientAt, yes.note, yes.base))
        assertEquals(Out.Refused(Album.Refusal.CLIENT_AT_DAY), AlbumWrite.answer(m, 80, CrOutcome.APPROVED, "9 Nov", "", now))
        assertEquals(Out.Refused(Album.Refusal.CLIENT_AT_DAY), AlbumWrite.answer(m, 80, CrOutcome.DECLINED, "", "", now))
        assertEquals("client_at must be the date the client answered, as YYYY-MM-DD", Album.Refusal.CLIENT_AT_DAY.text(Tx.EN))
        // S5's own live change on 50: the answer carries `change`, the base S5's 11 (the review moved it)
        val no = queued(AlbumWrite.answer(model(), 50, CrOutcome.DECLINED, "2026-11-08", "in person", now, Random(10)))
        assertEquals(listOf("declined", "change", "11"), listOf(no.value, no.prev, no.base))
    }

    @Test fun `every answer refusal but the day is None - no request, not approved, not the PM (the K4 critic, finding 6)`() {
        assertEquals(Out.None, AlbumWrite.answer(model(), 20, CrOutcome.APPROVED, "2026-11-09", "", now), "no live request on 20")
        val revisions = cache.copy(stage = "Revisions")
        assertEquals(Out.None, AlbumWrite.answer(model(c = revisions), 50, CrOutcome.APPROVED, "2026-11-09", "", now))
        assertEquals(Out.None, AlbumWrite.raise(model(c = revisions), 80, CrKind.ADD, "", now))
        val designer = approved.copy(role = Album.DESIGNER, pm = "other@x")
        assertEquals(Out.None, AlbumWrite.answer(model(designer), 50, CrOutcome.APPROVED, "not a day", "", now), "not even the day's words")
    }

    @Test fun `withdrawn needs no day - today is the IST day`() {
        val w = queued(AlbumWrite.answer(model(), 50, CrOutcome.WITHDRAWN, "", "", now, Random(11)))
        assertEquals(listOf("withdrawn", ""), listOf(w.value, w.clientAt))
        assertNull(assertNotNull(AlbumQueue.requestOf(w, "PROJ-DEMO")).fields["client_at"])
        assertEquals("2026-11-09", AlbumWrite.today(Instant.parse("2026-11-08T18:30:00Z")), "00:00 IST")
        assertEquals("2026-11-08", AlbumWrite.today(Instant.parse("2026-11-08T18:29:59Z")))
        assertEquals("2026-11-09", AlbumWrite.today(now))
    }

    @Test fun `a review is built on the mirror's review_at, never the overlay's, and a second Accept queues nothing`() {
        val mirror = kept()
        val acc = queued(AlbumWrite.review(model(mirror), vendorKey, Album.REVIEW_ACCEPTED, now, Random(12)))
        assertEquals(listOf(Album.OP_REVIEW, "ALB-00000000001e", "accepted", "2026-11-03T05:16:04.218Z", "2026-11-09T06:30:00Z"),
            listOf(acc.kind, acc.photo, acc.value, acc.base, acc.at))
        assertEquals(50, acc.work)
        // the Accept queued: the overlay shows accepted, so a second Accept is the same review -- nothing
        val m2 = model(mirror, queue = listOf(acc))
        assertEquals(Out.None, AlbumWrite.review(m2, vendorKey, Album.REVIEW_ACCEPTED, now))
        // and Not for client after it is built on S5's review_at, never the overlay's tap time (S5 guards on the OLD one)
        val keep = queued(AlbumWrite.review(m2, vendorKey, Album.REVIEW_INTERNAL, Instant.parse("2026-11-09T06:31:00Z"), Random(13)))
        assertEquals("2026-11-03T05:16:04.218Z", keep.base)
        // a staff photo is never reviewed, nor a key the album does not hold
        assertEquals(Out.None, AlbumWrite.review(model(mirror), "ALBUM|Kitchen|ALB-000000000014", Album.REVIEW_ACCEPTED, now))
        assertEquals(Out.None, AlbumWrite.review(model(mirror), "ALBUM||ALB-0000000000ff", Album.REVIEW_ACCEPTED, now))
    }

    @Test fun `nobody but the PM writes - a Designer, no mirror, a project S5 does not list, no sign-in - every tap None`() {
        val designer = approved.copy(role = Album.DESIGNER, pm = "other@x")
        val cases = listOf(
            "a Designer" to model(designer),
            "no mirror" to model(null),
            "not on S5" to model(phoneOnly = true),
            "nobody signed in" to model(who = Me("", "")),
            "a refused renewal (who kept, signed out)" to model(who = Me("pm@x", "P M", signedIn = false)),
        )
        for ((why, m) in cases) {
            assertEquals(Out.None, AlbumWrite.state(m, 20, Album.IN_PROGRESS, "", now), why)
            assertEquals(Out.None, AlbumWrite.raise(m, 80, CrKind.ADD, "", now), why)
            assertEquals(Out.None, AlbumWrite.answer(m, 50, CrOutcome.WITHDRAWN, "", "", now), why)
            assertEquals(Out.None, AlbumWrite.review(m, vendorKey, Album.REVIEW_INTERNAL, now), why)
            assertFalse(m.base.canWrite, why)
            assertTrue(m.base.works.none { it.stateTap || it.raise != null || it.answer != null }, why)
            assertTrue(m.base.cards.values.none { it.review }, why)
        }
        assertTrue(AlbumPage.canWrite(approved, false, me))
    }

    @Test fun `an op built after its predecessor's answer was committed carries that answer's seq and review_at (the K4 critic, finding 1)`() {
        // the screen drew [a] queued; the drain then committed S5's row for it (seq 12) into the mirror file and took it out
        val a = queued(AlbumWrite.state(model(), 20, Album.IN_PROGRESS, "", now, Random(14)))
        val b = queued(AlbumWrite.state(model(queue = listOf(a)), 20, Album.ON_HOLD, "", now, Random(15)))
        assertEquals("0", b.base, "built on the screen's mirror, which had not caught up")
        val file = approved.copy(log = approved.log + Entry(12, a.id, Album.KIND_STATE, 20, Album.IN_PROGRESS, Album.NOT_STARTED, at = "2026-11-09T06:30:01.000Z"))
        assertEquals(b.copy(base = "12"), AlbumWrite.atAdd(b, seen = listOf(a), stored = emptyList(), mirror = file))
        // [a] still queued: its answer will rebase [b] (AQ), so nothing changes here
        assertEquals(b, AlbumWrite.atAdd(b, seen = listOf(a), stored = listOf(a), mirror = file))
        // another writer's row on 20 that the screen never saw is no predecessor: S5's guard decides it (a 409, S5 wins)
        val other = approved.copy(log = approved.log + Entry(12, "ALB-0000000000bb", Album.KIND_STATE, 20, Album.IN_PROGRESS, Album.NOT_STARTED))
        assertEquals(b, AlbumWrite.atAdd(b, seen = emptyList(), stored = emptyList(), mirror = other))
        // a predecessor on ANOTHER work moves nothing; a predecessor S5 refused (no row in the file) moves nothing
        val x = queued(AlbumWrite.state(model(), 30, Album.IN_PROGRESS, "", now, Random(16)))
        val on30 = approved.copy(log = approved.log + Entry(12, x.id, Album.KIND_STATE, 30, Album.IN_PROGRESS, Album.NOT_STARTED))
        assertEquals(b, AlbumWrite.atAdd(b, seen = listOf(x), stored = emptyList(), mirror = on30))
        assertEquals(b, AlbumWrite.atAdd(b, seen = listOf(a), stored = emptyList(), mirror = approved))
        // a review: the answered Accept's own row gives the next review of the photo S5's review_at
        val mirror = kept()
        val acc = queued(AlbumWrite.review(model(mirror), vendorKey, Album.REVIEW_ACCEPTED, now, Random(17)))
        val keep = queued(AlbumWrite.review(model(mirror, queue = listOf(acc)), vendorKey, Album.REVIEW_INTERNAL, now, Random(18)))
        val row = Entry(12, acc.id, Album.KIND_REVIEW, 50, Album.REVIEW_ACCEPTED, Album.REVIEW_INTERNAL, photo = "ALB-00000000001e",
            at = "2026-11-09T06:30:02.000Z")
        assertEquals(keep.copy(base = "2026-11-09T06:30:02.000Z"),
            AlbumWrite.atAdd(keep, seen = listOf(acc), stored = emptyList(), mirror = mirror.copy(log = mirror.log + row)))
        // and the state op on 50 behind the answered review takes the review row's seq: a review moves the work's base
        val s50 = queued(AlbumWrite.state(model(mirror, queue = listOf(acc)), 50, Album.IN_PROGRESS, "", now, Random(19)))
        assertEquals("12", AlbumWrite.atAdd(s50, listOf(acc), emptyList(), mirror.copy(log = mirror.log + row)).base)
        // no mirror file: the op as it came
        assertEquals(b, AlbumWrite.atAdd(b, listOf(a), emptyList(), null))
    }

    @Test fun `the source keeps the phone's rules - Out None not Nothing, and no LocalDate ofInstant anywhere in pano`() {
        val src = java.io.File("src/main/kotlin/com/malletcrafts/interiorestimate/pano")
        assertTrue(src.isDirectory, src.absolutePath)
        fun code(t: String) = t.replace(Regex("""/\*[\s\S]*?\*/"""), "").lines().joinToString("\n") { it.substringBefore("//") }
        val offenders = src.walkTopDown().filter { it.isFile && it.name.endsWith(".kt") }
            .filter { f -> code(f.readText()).contains("LocalDate.ofInstant") }.map { it.name }.toList()
        assertEquals(emptyList(), offenders, "LocalDate.ofInstant is Android API 34; :pano runs on the phone (minSdk 29)")
        val write = java.io.File(src, "AlbumWrite.kt").readText()
        assertTrue(write.contains("data object None : Out"))
        assertTrue(write.contains("now.atZone(QuoteStage.IST).toLocalDate()"))
    }
}
