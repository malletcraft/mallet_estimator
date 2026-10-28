package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.QuoteStage.Cache
import com.malletcrafts.interiorestimate.pano.QuoteStage.Listed
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The quote-stage line on the project screen (v52 item 16, decision 1 of 15 Oct 2026),
 * against the prototype's texts (ct52 W(a), FX16) and S5's stage list.
 */
class QuoteStageTest {

    // 14 Oct 2026, 08:08 IST, as S5 writes it (new Date().toISOString(), UTC).
    private val at = "2026-10-14T02:38:00.000Z"
    // "Now" on the phone: 14 Oct 2026, 15:00 IST.
    private val now = Instant.parse("2026-10-14T09:30:00Z")

    @Test fun `the six stages are S5's QUOTE_STAGES, in order`() {
        // A copy of functions/_lib/asis.js QUOTE_STAGES (S5 QUOTE; five since 14 Oct 08:08 IST, migration
        // 0004; six, reordered, since v61b on 26 Oct 2026). Change it here only together with S5.
        val s5 = listOf("Survey", "Client wish list", "Vendor quoted", "Estimated", "Revisions", "Client approved")
        assertEquals(s5, QuoteStage.STAGES)
        assertEquals(1, QuoteStage.indexOf("Client wish list"))
        assertEquals(3, QuoteStage.indexOf("Estimated"))
        assertEquals(4, QuoteStage.indexOf("Revisions"))
        assertEquals(5, QuoteStage.indexOf(QuoteStage.APPROVED))
        assertEquals("Client approved", QuoteStage.STAGES.last(), "execution begins at the last stage (v61b)")
        assertEquals(-1, QuoteStage.indexOf("Quoted"))
    }

    @Test fun `Client approved lights every pip - Estimated is the fourth of six (v61b)`() {
        assertEquals(6, QuoteStage.lineFor(Cache(true, "Client approved", "", at, now.toString()), now).lit)
        assertEquals(4, QuoteStage.lineFor(Cache(true, "Estimated", "", at, now.toString()), now).lit)
        assertEquals(5, QuoteStage.lineFor(Cache(true, "Revisions", "", at, now.toString()), now).lit)
    }

    @Test fun `never read - not read yet, S5 decides when online, nothing lit (FX16)`() {
        val l = QuoteStage.lineFor(null, now)
        assertEquals("QUOTE STAGE · not read yet", l.title)
        assertEquals("Quote stage not read yet — S5 decides when online", l.second)
        assertEquals(0, l.lit)
        assertFalse(l.showRead)
    }

    @Test fun `listed with a time - the stage, its pips and since in IST (ct52 W(a))`() {
        val l = QuoteStage.lineFor(Cache(true, "Client wish list", "staff@x", at, "2026-10-14T09:00:00Z"), now)
        assertEquals("QUOTE STAGE · Client wish list", l.title)
        assertEquals(2, l.lit)
        assertEquals("since 14 Oct, 08:08 IST on S5", l.second)
        assertFalse(l.showRead)
    }

    @Test fun `pips are lit to the index, one per stage`() {
        QuoteStage.STAGES.forEachIndexed { i, s ->
            assertEquals(i + 1, QuoteStage.lineFor(Cache(true, s, "", at, now.toString()), now).lit)
        }
    }

    @Test fun `listed with no time - the stage, not set on S5 yet`() {
        val l = QuoteStage.lineFor(Cache(true, "Survey", "", "", now.toString()), now)
        assertEquals("QUOTE STAGE · Survey", l.title)
        assertEquals(1, l.lit)
        assertEquals("Survey · not set on S5 yet", l.second)
    }

    @Test fun `an unreadable time reads as not set, never a made-up date`() {
        val l = QuoteStage.lineFor(Cache(true, "Vendor quoted", "", "yesterday", now.toString()), now)
        assertEquals("Vendor quoted · not set on S5 yet", l.second)
        assertEquals(3, l.lit)
    }

    @Test fun `not listed - not on S5 yet, this project has not been sent`() {
        val l = QuoteStage.lineFor(Cache(listed = false, readAt = now.toString()), now)
        assertEquals("QUOTE STAGE · not on S5 yet", l.title)
        assertEquals("not on S5 yet — this project has not been sent", l.second)
        assertEquals(0, l.lit)
        assertFalse(l.showRead)
    }

    @Test fun `a phone-only project reads as not on S5 yet`() {
        val l = QuoteStage.phoneOnly()
        assertEquals("QUOTE STAGE · not on S5 yet", l.title)
        assertEquals("not on S5 yet — this project has not been sent", l.second)
        assertEquals(0, l.lit)
        assertFalse(l.showRead)
    }

    @Test fun `a stage name the phone does not know is shown as written, nothing lit`() {
        val l = QuoteStage.lineFor(Cache(true, "On hold", "", at, now.toString()), now)
        assertEquals("QUOTE STAGE · On hold", l.title)
        assertEquals(0, l.lit)
    }

    @Test fun `a UTC time is shown in IST, across the date line`() {
        // 20:00 UTC on 31 Oct is 01:30 IST on 1 Nov.
        assertEquals("since 1 Nov, 01:30 IST on S5", QuoteStage.since("2026-10-31T20:00:00Z"))
        // An offset time is taken as the instant it names.
        assertEquals("since 14 Oct, 08:08 IST on S5", QuoteStage.since("2026-10-14T08:08:00+05:30"))
        assertEquals("since 5 Sept, 05:30 IST on S5", QuoteStage.since("2026-09-05T00:00:00Z"))
        assertNull(QuoteStage.since(""))
        assertNull(QuoteStage.since("not a time"))
    }

    @Test fun `the read time shows only when the copy is from before today in IST`() {
        val c = { readAt: String -> Cache(true, "Survey", "", at, readAt) }
        // Read earlier today (IST): no read time.
        assertFalse(QuoteStage.lineFor(c("2026-10-14T00:00:00Z"), now).showRead)
        // Read yesterday (IST): the second line carries it.
        val l = QuoteStage.lineFor(c("2026-10-13T12:15:00Z"), now)
        assertTrue(l.showRead)
        assertEquals("since 14 Oct, 08:08 IST on S5 · read 17:45", l.second)
        // Not listed, read yesterday: the same suffix.
        assertEquals("not on S5 yet — this project has not been sent · read 17:45",
            QuoteStage.lineFor(Cache(listed = false, readAt = "2026-10-13T12:15:00Z"), now).second)
        // No read time recorded: never shown.
        assertFalse(QuoteStage.lineFor(c(""), now).showRead)
    }

    @Test fun `the read-time rule turns over at midnight IST, not UTC`() {
        // Read 23:50 IST on 13 Oct (18:20 UTC).
        val c = Cache(true, "Survey", "", at, "2026-10-13T18:20:00Z")
        // Now 23:59 IST on 13 Oct: same IST day, not shown.
        assertFalse(QuoteStage.lineFor(c, Instant.parse("2026-10-13T18:29:00Z")).showRead)
        // Now 00:01 IST on 14 Oct (still 13 Oct in UTC): a day old in IST, shown.
        val l = QuoteStage.lineFor(c, Instant.parse("2026-10-13T18:31:00Z"))
        assertTrue(l.showRead)
        assertTrue(l.second.endsWith(" · read 23:50"), l.second)
        // Read 00:10 IST on 14 Oct (18:40 UTC on 13 Oct), now 23:00 IST on 14 Oct: same IST day.
        assertFalse(QuoteStage.readIsOld("2026-10-13T18:40:00Z", Instant.parse("2026-10-14T17:30:00Z")))
    }

    @Test fun `one read of the list caches every listed project, and every known one absent as not listed`() {
        val read = "2026-10-14T09:30:00Z"
        val m = QuoteStage.fromList(
            listOf(Listed("PROJ-0001", "Client wish list", "staff@x", at), Listed("PROJ-0009", "Survey", "", "")),
            listOf("PROJ-0001", "PROJ-0002", ""), read)
        assertEquals(setOf("PROJ-0001", "PROJ-0002", "PROJ-0009"), m.keys)
        assertEquals(Cache(true, "Client wish list", "staff@x", at, read), m["PROJ-0001"])
        assertEquals(Cache(listed = false, readAt = read), m["PROJ-0002"])
        assertEquals(Cache(true, "Survey", "", "", read), m["PROJ-0009"])
        assertEquals("not on S5 yet — this project has not been sent",
            QuoteStage.lineFor(m["PROJ-0002"], Instant.parse(read)).second)
    }

    @Test fun `an answer that names the stage goes into the one copy, its who and when unknown until the next read`() {
        val read = Cache(listed = true, stage = "Revisions", by = "pm@mcft", at = at, readAt = "2026-10-28T01:00:00Z")
        // another stage: listed at it, read now, by and at blank until the list is read again
        assertEquals(Cache(listed = true, stage = "Client approved", readAt = "2026-10-28T03:00:00Z"),
            QuoteStage.named(read, "Client approved", "2026-10-28T03:00:00Z"))
        // the same stage changes nothing, so the who and when read from the list stay
        assertNull(QuoteStage.named(read, "Revisions", "2026-10-28T03:00:00Z"))
        assertNull(QuoteStage.named(read, " Revisions ", "2026-10-28T03:00:00Z"))
        // a name that is not one of the six, or none, changes nothing
        for (bad in listOf("", "  ", "Quoted", "client approved")) assertNull(QuoteStage.named(read, bad, "x"), bad)
        // never read, or read as not listed: S5 has just answered for the project, so it is listed
        assertEquals(Cache(listed = true, stage = "Survey", readAt = "r"), QuoteStage.named(null, "Survey", "r"))
        assertEquals(Cache(listed = true, stage = "Survey", readAt = "r"), QuoteStage.named(Cache(listed = false, readAt = "q"), "Survey", "r"))
        // the line reads the stage at once, every pip lit
        val line = QuoteStage.lineFor(QuoteStage.named(read, "Client approved", "2026-10-28T03:00:00Z"), Instant.parse("2026-10-28T04:00:00Z"))
        assertEquals("QUOTE STAGE · Client approved", line.title)
        assertEquals(6, line.lit)
    }

    @Test fun `a read that went out before S5 named the stage never puts the old stage back (compare-and-set per id)`() {
        val p = "PROJ-0001"; val q = "PROJ-0002"; val r = "PROJ-0003"
        val revisions = Cache(listed = true, stage = "Revisions", by = "pm@mcft", at = at, readAt = "2026-10-28T01:00:00Z")
        // A, the project-open read, goes out at 03:00:00 with p and q stored at Revisions and r never read
        val seen = mapOf(p to revisions, q to revisions.copy(stage = "Vendor quoted"))
        val readA = "2026-10-28T03:00:00Z"
        val listedA = listOf(Listed(p, "Revisions", "pm@mcft", at), Listed(q, "Estimated", "pm@mcft", at),
            Listed(r, "Survey", "", ""))
        // the PM approves; the worker's PUT meets the final 409 and named writes Client approved for p ...
        val named = QuoteStage.named(revisions, "Client approved", "2026-10-28T03:00:01Z")!!
        // ... and A's slow answer, read before the Approve, lands now: p keeps S5's later word
        val afterNamed = QuoteStage.fromList(listedA, listOf(p, q), readA, seen, current = seen + (p to named))
        assertFalse(p in afterNamed, "the older read put Revisions back over Client approved")
        // q and r: nothing wrote them meanwhile, so the read takes them, dated when the GET went OUT
        assertEquals(Cache(true, "Estimated", "pm@mcft", at, readA), afterNamed[q])
        assertEquals(Cache(true, "Survey", "", "", readA), afterNamed[r])
        // the same when the worker's own read (B, after named) filled in who and when before A landed
        val b = QuoteStage.fromList(listOf(Listed(p, "Client approved", "pm@mcft", "2026-10-28T02:59:30.000Z")),
            listOf(p), "2026-10-28T03:00:02Z", seen + (p to named), seen + (p to named))
        assertEquals("Client approved", b[p]!!.stage)
        assertFalse(p in QuoteStage.fromList(listedA, listOf(p, q), readA, seen, current = seen + b))
        // an id first cached while the read was out (absent from the snapshot) keeps that copy too
        assertFalse(r in QuoteStage.fromList(listedA, listOf(p, q), readA, seen, current = seen + (r to named)))
        // and a known id the list does not carry is "not listed" only where nothing wrote it meanwhile
        val gone = QuoteStage.fromList(emptyList(), listOf(p, q), readA, seen, current = seen + (p to named))
        assertEquals(setOf(q), gone.keys)
        assertEquals(Cache(listed = false, readAt = readA), gone[q])
        // nothing written meanwhile: the plain read, every id
        assertEquals(QuoteStage.fromList(listedA, listOf(p, q), readA), QuoteStage.fromList(listedA, listOf(p, q), readA, seen, seen))
        assertEquals(QuoteStage.fromList(listedA, listOf(p, q), readA), QuoteStage.fromList(listedA, listOf(p, q), readA, emptyMap(), emptyMap()))
    }
}
