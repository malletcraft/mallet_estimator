package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Album.WorkState
import com.malletcrafts.interiorestimate.pano.QuoteStage.Cache
import com.malletcrafts.interiorestimate.pano.QuoteStage.Coverage
import com.malletcrafts.interiorestimate.pano.QuoteStage.LineKind
import com.malletcrafts.interiorestimate.pano.QuoteStage.Listed
import com.malletcrafts.interiorestimate.pano.QuoteStage.Shot
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
        // another stage: listed at it, read now, by and at blank -- and marked UNKNOWN, not never set -- until the list is read again
        assertEquals(Cache(listed = true, stage = "Client approved", readAt = "2026-10-28T03:00:00Z", atUnknown = true),
            QuoteStage.named(read, "Client approved", "2026-10-28T03:00:00Z"))
        // the same stage changes nothing, so the who and when read from the list stay
        assertNull(QuoteStage.named(read, "Revisions", "2026-10-28T03:00:00Z"))
        assertNull(QuoteStage.named(read, " Revisions ", "2026-10-28T03:00:00Z"))
        // a name that is not one of the six, or none, changes nothing
        for (bad in listOf("", "  ", "Quoted", "client approved")) assertNull(QuoteStage.named(read, bad, "x"), bad)
        // never read, or read as not listed: S5 has just answered for the project, so it is listed
        assertEquals(Cache(listed = true, stage = "Survey", readAt = "r", atUnknown = true), QuoteStage.named(null, "Survey", "r"))
        assertEquals(Cache(listed = true, stage = "Survey", readAt = "r", atUnknown = true), QuoteStage.named(Cache(listed = false, readAt = "q"), "Survey", "r"))
        // the line reads the stage at once, every pip lit
        val line = QuoteStage.lineFor(QuoteStage.named(read, "Client approved", "2026-10-28T03:00:00Z"), Instant.parse("2026-10-28T04:00:00Z"))
        assertEquals("QUOTE STAGE · Client approved", line.title)
        assertEquals(6, line.lit)
    }

    @Test fun `a read that went out before S5 named the stage never puts the old stage back (ordered by when each went out)`() {
        val p = "PROJ-0001"; val q = "PROJ-0002"; val r = "PROJ-0003"
        // A, the project-open read, goes out as seq 1; p and q were written before this process's reads
        val readA = "2026-10-28T03:00:00Z"
        val listedA = listOf(Listed(p, "Revisions", "pm@mcft", at), Listed(q, "Estimated", "pm@mcft", at),
            Listed(r, "Survey", "", ""))
        // the PM approves; the worker's PUT meets the final 409 and named writes Client approved for p as seq 2 ...
        val written = mapOf(p to 2L)
        val named = QuoteStage.named(Cache(true, "Revisions", "pm@mcft", at, "2026-10-27T10:00:00Z"), "Client approved", "2026-10-28T03:00:01Z")!!
        val stored = mapOf(p to named, q to Cache(true, "Estimated", "pm@mcft", at, "2026-10-27T10:00:00Z"))
        // ... and A's slow answer, read before the Approve, lands now: p keeps S5's later word (the named
        // copy has no stamp to compare, so the sequence decides)
        val afterNamed = QuoteStage.fromList(listedA, listOf(p, q), readA, 1L, written, stored)
        assertFalse(p in afterNamed, "the older read put Revisions back over Client approved")
        // q and r: nothing later wrote them, so the read takes them, dated when the GET went OUT
        assertEquals(Cache(true, "Estimated", "pm@mcft", at, readA), afterNamed[q])
        assertEquals(Cache(true, "Survey", "", "", readA), afterNamed[r])
        // the worker's own read B goes out after named (seq 3) and takes p, who and when filled in
        val b = QuoteStage.fromList(listOf(Listed(p, "Client approved", "pm@mcft", "2026-10-28T02:59:30.000Z")),
            listOf(p), "2026-10-28T03:00:02Z", 3L, written, stored)
        assertEquals("Client approved", b[p]!!.stage)
        // A landing after B wrote p (seq 3) still leaves p alone
        assertFalse(p in QuoteStage.fromList(listedA, listOf(p, q), readA, 1L, mapOf(p to 3L), stored + b))
        // an id first written while the read was out, by something later, keeps that copy too
        assertFalse(r in QuoteStage.fromList(listedA, listOf(p, q), readA, 1L, mapOf(r to 2L),
            mapOf(r to Cache(true, "Survey", readAt = "2026-10-28T03:00:03Z"))))
        // and a known id the list does not carry is "not listed" only where nothing later wrote it
        val gone = QuoteStage.fromList(emptyList(), listOf(p, q), readA, 1L, written, stored)
        assertEquals(setOf(q), gone.keys)
        assertEquals(Cache(listed = false, readAt = readA), gone[q])
        // nothing written since the process started: the plain read, every id
        assertEquals(QuoteStage.fromList(listedA, listOf(p, q), readA), QuoteStage.fromList(listedA, listOf(p, q), readA, 1L, emptyMap(), emptyMap()))
    }

    @Test fun `of two reads out at once, the one that went out later wins whichever lands first`() {
        val p = "PROJ-0001"
        // A goes out (seq 1) while S5 says Revisions; the PM approves; C goes out (seq 2) and S5 says Client approved
        val listedA = listOf(Listed(p, "Revisions", "pm@mcft", at))
        val listedC = listOf(Listed(p, "Client approved", "pm@mcft", "2026-10-28T03:00:05.000Z"))
        val readA = "2026-10-28T03:00:00Z"; val readC = "2026-10-28T03:00:06Z"
        // IN ORDER, the usual case: A lands and writes p as seq 1, then C lands -- C must be kept.
        // The equality compare of the first version dropped C here: A's write changed the copy C had seen.
        val aFirst = QuoteStage.fromList(listedA, listOf(p), readA, 1L, emptyMap(), emptyMap())
        assertEquals("Revisions", aFirst[p]!!.stage)
        val thenC = QuoteStage.fromList(listedC, listOf(p), readC, 2L, aFirst.keys.associateWith { 1L }, aFirst)
        assertEquals(Cache(true, "Client approved", "pm@mcft", "2026-10-28T03:00:05.000Z", readC), thenC[p])
        // OUT OF ORDER: C lands first and writes p as seq 2, then A lands -- A must be dropped
        val cFirst = QuoteStage.fromList(listedC, listOf(p), readC, 2L, emptyMap(), emptyMap())
        assertEquals("Client approved", cFirst[p]!!.stage)
        assertTrue(QuoteStage.fromList(listedA, listOf(p), readA, 1L, cFirst.keys.associateWith { 2L }, cFirst).isEmpty())
        // the same read landing twice (its own seq stored) writes nothing the second time
        assertTrue(QuoteStage.fromList(listedC, listOf(p), readC, 2L, mapOf(p to 2L), cFirst).isEmpty())
    }

    @Test fun `a read that went out first but reached S5 after the Approve wins by S5's stamp, landing first or last`() {
        val p = "PROJ-0001"
        // R1 goes out (seq 1), then R2 (seq 2); S5 serves R2 BEFORE the Approve and R1 after it (R1 waited
        // on its connection, or was retried), so the read that went out first carries the newer stage
        val tRev = "2026-10-28T02:00:00.000Z"; val tApproved = "2026-10-28T03:00:05.000Z"
        val listedR1 = listOf(Listed(p, "Client approved", "pm@mcft", tApproved))
        val listedR2 = listOf(Listed(p, "Revisions", "pm@mcft", tRev))
        val readR1 = "2026-10-28T03:00:00Z"; val readR2 = "2026-10-28T03:00:01Z"
        // R2 LANDS FIRST and writes p as seq 2; then R1 lands -- its later stamp is kept, though its seq is lower.
        // Ordered by the sequence alone, R1 was dropped and the phone held Revisions under S5's final stage.
        val r2First = QuoteStage.fromList(listedR2, listOf(p), readR2, 2L, emptyMap(), emptyMap())
        assertEquals("Revisions", r2First[p]!!.stage)
        assertEquals(Cache(true, "Client approved", "pm@mcft", tApproved, readR1),
            QuoteStage.fromList(listedR1, listOf(p), readR1, 1L, mapOf(p to 2L), r2First)[p])
        // R1 LANDS FIRST and writes p as seq 1; the wish list's final 409 finds Client approved already
        // cached, so named writes nothing and records no seq ...
        val r1First = QuoteStage.fromList(listedR1, listOf(p), readR1, 1L, emptyMap(), emptyMap())
        assertEquals("Client approved", r1First[p]!!.stage)
        assertNull(QuoteStage.named(r1First[p], "Client approved", "2026-10-28T03:00:06Z"))
        // ... and R2 lands with seq 2 above p's 1: its older stamp is dropped, so Revisions never comes back
        assertTrue(QuoteStage.fromList(listedR2, listOf(p), readR2, 2L, mapOf(p to 1L), r1First).isEmpty(),
            "a read served before the Approve put Revisions back over S5's final stage")
    }

    @Test fun `without two different stamps to compare, the sequence decides`() {
        val stamped = Cache(true, "Revisions", "pm@mcft", "2026-10-28T02:00:00.000Z", "2026-10-28T02:00:01Z")
        val newer = Cache(true, "Client approved", "pm@mcft", "2026-10-28T03:00:05.000Z", "2026-10-28T03:00:06Z")
        // two stamps: the later wins, whatever the seqs
        assertTrue(QuoteStage.takes(newer, stamped, 1L, 2L))
        assertFalse(QuoteStage.takes(stamped, newer, 3L, 2L))
        // a copy named from a 409 has no stamp: an older read cannot beat it, a later one fills it in
        val named = Cache(listed = true, stage = "Client approved", readAt = "2026-10-28T03:00:07Z")
        assertFalse(QuoteStage.takes(stamped, named, 1L, 2L))
        assertTrue(QuoteStage.takes(newer, named, 3L, 2L))
        // a read that does not list the project has no stamp either
        assertFalse(QuoteStage.takes(Cache(listed = false, readAt = "r"), newer, 1L, 2L))
        assertTrue(QuoteStage.takes(Cache(listed = false, readAt = "r"), newer, 3L, 2L))
        // a stage never set on S5, or a stamp that is not a time, is no stamp
        assertFalse(QuoteStage.takes(Cache(true, "Survey", "", "", "r"), newer, 1L, 2L))
        assertFalse(QuoteStage.takes(Cache(true, "Survey", "", "soon", "r"), stamped, 1L, 2L))
        // ... and a listed read with no stamp never replaces a copy that has one, even sent later:
        // S5 has no way back to never set, so it was served before the project's first move
        assertFalse(QuoteStage.takes(Cache(true, "Survey", "", "", "r"), newer, 3L, 2L))
        assertFalse(QuoteStage.takes(Cache(true, "Survey", "", "", "r"), newer, 3L, null))
        // a copy with no stamp of its own (never set, or named from a 409) is still replaced by the sequence
        assertTrue(QuoteStage.takes(Cache(true, "Survey", "", "", "r"), Cache(true, "Survey", "", "", "q"), 3L, 2L))
        // equal stamps are one state of S5's: the sequence alone, so the same read twice writes once
        assertFalse(QuoteStage.takes(newer, newer, 2L, 2L))
        assertTrue(QuoteStage.takes(newer.copy(readAt = "later"), newer, 3L, 2L))
        // nothing stored and nothing written since the process started: the read takes it ...
        assertTrue(QuoteStage.takes(stamped, null, 1L, null))
        // ... but a copy kept from before this process still beats an older stamp
        assertFalse(QuoteStage.takes(stamped, newer, 1L, null))
    }

    // ---- port 61c slice QM0: the one project line, the store's merge rule, the dots and the coverage ----------

    private fun tree(t: String): Any? = MiniJson.parse(t)
    private fun rows(body: String): List<Listed> =
        ((tree(body) as Map<*, *>)["projects"] as List<*>).map { assertNotNull(QuoteStage.listedOf(MiniRead, it)) }
    private fun line(c: Cache?, tx: Tx = Tx.EN, phoneOnly: Boolean = false, queued: List<Album.Op> = emptyList(), t: Instant = now) =
        QuoteStage.projectLine(c, phoneOnly, queued, tx, t)
    private fun approved(states: Map<Int, WorkState>?) = Cache(true, "Client approved", "pm@x", at, now.toString(), states)

    // S5's stamps in the QM0 harness run (S5Quote)
    private val tApproved = "2026-11-03T07:01:08.769Z"

    @Test fun `S5's list row reads whole - the stage, who and when, and work_states at each state row's own seq (S5-L)`() {
        val l = rows(S5Quote.LIST)
        assertEquals(listOf("PROJ-REC", "PROJ-DEMO"), l.map { it.project })
        assertEquals(Listed("PROJ-DEMO", "Client approved", "pm@x", tApproved,
            mapOf(10 to WorkState(Album.IN_PROGRESS, 7), 20 to WorkState(Album.DONE, 9))), l[1])
        assertEquals(Listed("PROJ-REC", "Client approved", "pm@x", "2026-10-20T04:30:00.000Z", emptyMap()), l[0], "no state row: {} -- read, nothing started")
        // a row of an S5 before S5-L has no work_states: not read in this shape, never "no work started"
        assertNull(QuoteStage.listedOf(MiniRead, tree("""{"project":"P","quote_stage":"Survey","quote_stage_by":null,"quote_stage_at":null}"""))!!.states)
        assertEquals(Listed("P", "Survey", "", ""), QuoteStage.listedOf(MiniRead, tree("""{"project":"P","quote_stage":"Survey","quote_stage_by":null,"quote_stage_at":null}""")))
        // an entry with no integral seq, or for an id that is not a work, is left out; a state outside the five is Not started
        val odd = QuoteStage.listedOf(MiniRead, tree("""{"project":"P","work_states":{"10":{"state":"Done"},"999":{"state":"Done","seq":4},"20":{"state":"Halfway","seq":5},"30":{"state":"Done","seq":6.0}}}"""))!!
        assertEquals(mapOf(20 to WorkState(Album.NOT_STARTED, 5), 30 to WorkState(Album.DONE, 6)), odd.states)
        assertNull(QuoteStage.listedOf(MiniRead, tree("""{"project":" "}""")))
        assertNull(QuoteStage.listedOf(MiniRead, tree("[]")))
        // the 3-argument fromList carries the states into the copy
        assertEquals(mapOf(10 to WorkState(Album.IN_PROGRESS, 7), 20 to WorkState(Album.DONE, 9)),
            QuoteStage.fromList(l, emptyList(), now.toString())["PROJ-DEMO"]!!.states)
    }

    @Test fun `projectLine - PROJ-DEMO reads Execution, 1 in progress, 1 done, over Quote, Client approved`() {
        val demo = QuoteStage.fromList(rows(S5Quote.LIST), emptyList(), now.toString())["PROJ-DEMO"]
        val l = line(demo)
        assertEquals(LineKind.EXEC, l.kind)
        assertEquals("Execution · 1 in progress · 1 done", l.text)
        assertEquals(listOf("1 in progress", "1 done"), l.parts)
        assertEquals("Quote · Client approved", l.second)
        assertEquals("Client approved", l.quote)
        assertEquals(6, l.lit)
        val mr = line(demo, Tx.MR)
        assertEquals("अंमलबजावणी · 1 चालू · 1 पूर्ण", mr.text)
        assertEquals("कोटेशन · ग्राहक मंजूर", mr.second)
        // the album GET's copy says the same, through the store
        val album = QuoteStage.fromAlbum(assertNotNull(AlbumWire.view(MiniRead, tree(S5Quote.ALBUM))), now.toString())
        assertEquals("Execution · 1 in progress · 1 done", line(album).text)
    }

    @Test fun `projectLine at Client approved - no work started, only abandoned, on hold in its place, never a bare Execution`() {
        assertEquals("Execution · no work started yet", line(approved(emptyMap())).text, "every work Not started")
        assertEquals("Execution · no work started yet", line(approved(mapOf(10 to WorkState(Album.NOT_STARTED, 3)))).text)
        assertEquals("अंमलबजावणी · अजून कोणतेही काम सुरू नाही", line(approved(emptyMap()), Tx.MR).text)
        // a work started and then abandoned WAS started: named when nothing else counts (review 4)
        assertEquals("Execution · 2 abandoned", line(approved(mapOf(10 to WorkState(Album.ABANDONED, 4), 20 to WorkState(Album.ABANDONED, 5)))).text)
        assertEquals("अंमलबजावणी · 2 सोडून दिलेले", line(approved(mapOf(10 to WorkState(Album.ABANDONED, 4), 20 to WorkState(Album.ABANDONED, 5))), Tx.MR).text)
        assertEquals("Execution · 1 in progress", line(approved(mapOf(10 to WorkState(Album.ABANDONED, 4), 20 to WorkState(Album.IN_PROGRESS, 5)))).text)
        // in progress · done · on hold, whatever the works' order
        val mixed = mapOf(140 to WorkState(Album.ON_HOLD, 9), 10 to WorkState(Album.DONE, 3), 20 to WorkState(Album.IN_PROGRESS, 5), 30 to WorkState(Album.DONE, 6))
        assertEquals("Execution · 1 in progress · 2 done · 1 on hold", line(approved(mixed)).text)
        assertEquals(listOf("1 in progress", "2 done", "1 on hold"), line(approved(mixed)).parts)
        assertEquals("Execution · 1 on hold", line(approved(mapOf(50 to WorkState(Album.ON_HOLD, 2)))).text)
        assertEquals("अंमलबजावणी · 1 थांबवलेले", line(approved(mapOf(50 to WorkState(Album.ON_HOLD, 2))), Tx.MR).text)
        // no states read yet (a copy 0.3.262 wrote, an S5 before S5-L): never a count it has not read, never bare
        val unread = line(approved(null))
        assertEquals("Execution · not read yet", unread.text)
        assertEquals(emptyList(), unread.parts)
        assertEquals("Quote · Client approved", unread.second)
        assertEquals("अंमलबजावणी · अजून वाचलेले नाही", line(approved(null), Tx.MR).text)
    }

    @Test fun `projectLine - this phone's queued state writes lay over the states at once`() {
        val ip = Album.stateOp(null, 30, Album.NOT_STARTED, Album.IN_PROGRESS, "")
        assertEquals("Execution · 1 in progress", line(approved(emptyMap()), queued = listOf(ip)).text)
        // a queued Done over a work S5 has In progress moves the count; a later op on the same work wins
        val done = Album.stateOp(null, 10, Album.IN_PROGRESS, Album.DONE, "")
        assertEquals("Execution · 1 done", line(approved(mapOf(10 to WorkState(Album.IN_PROGRESS, 7))), queued = listOf(done)).text)
        assertEquals("Execution · 1 in progress", line(approved(mapOf(10 to WorkState(Album.IN_PROGRESS, 7))),
            queued = listOf(done, Album.stateOp(null, 10, Album.DONE, Album.IN_PROGRESS, "back"))).text)
        // a change request or a photo op counts nothing
        assertEquals("Execution · no work started yet", line(approved(emptyMap()), queued = listOf(Album.crRaiseOp(null, 90, Album.CrKind.ADD, "x"))).text)
        // and with no states read, a queued op is no count either
        assertEquals("Execution · not read yet", line(approved(null), queued = listOf(ip)).text)
    }

    @Test fun `projectLine through the quote journey - Quote, the stage, its pips and since, or not set on S5 yet`() {
        val l = line(Cache(true, "Client wish list", "staff@x", at, "2026-10-14T09:00:00Z"))
        assertEquals(LineKind.QUOTE, l.kind)
        assertEquals("Quote · Client wish list", l.text)
        assertEquals("since 14 Oct, 08:08 IST on S5", l.second)
        assertEquals(2, l.lit)
        assertEquals("Client wish list", l.quote)
        assertFalse(l.showRead)
        assertEquals("कोटेशन · ग्राहकाची इच्छा-यादी", line(Cache(true, "Client wish list", "staff@x", at, "2026-10-14T09:00:00Z"), Tx.MR).text)
        assertEquals("S5 वर 14 Oct, 08:08 IST पासून", line(Cache(true, "Client wish list", "staff@x", at, "2026-10-14T09:00:00Z"), Tx.MR).second)
        QuoteStage.STAGES.dropLast(1).forEachIndexed { i, s -> assertEquals(i + 1, line(Cache(true, s, "", at, now.toString())).lit, s) }
        // once S5 lists the project, a stage it has not timed reads 'not set on S5 yet' -- never the never-sent words
        val untimed = line(Cache(true, "Survey", "", "", now.toString()))
        assertEquals("Quote · Survey", untimed.text)
        assertEquals("Survey · not set on S5 yet", untimed.second)
        assertEquals("सर्व्हे · अजून S5 वर सेट नाही", line(Cache(true, "Survey", "", "", now.toString()), Tx.MR).second)
        assertEquals("Vendor quoted · not set on S5 yet", line(Cache(true, "Vendor quoted", "", "yesterday", now.toString())).second)
        // the read time from before today (IST)
        assertEquals("since 14 Oct, 08:08 IST on S5 · read 17:45", line(Cache(true, "Survey", "", at, "2026-10-13T12:15:00Z")).second)
        assertTrue(line(Cache(true, "Survey", "", at, "2026-10-13T12:15:00Z")).showRead)
        assertEquals("S5 वर 14 Oct, 08:08 IST पासून · 17:45 ला वाचले", line(Cache(true, "Survey", "", at, "2026-10-13T12:15:00Z"), Tx.MR).second)
    }

    @Test fun `projectLine unread - never read, a blank stage, or a name outside the six (critic G6)`() {
        for (c in listOf(null, Cache(true, "", "", "", now.toString()), Cache(true, "On hold", "", at, now.toString()), Cache(true, "Quoted", "", at, now.toString()))) {
            val l = line(c)
            assertEquals(LineKind.UNREAD, l.kind, "$c")
            assertEquals("Quote · not read yet", l.text, "$c")
            assertEquals("Quote stage not read yet — S5 decides when online", l.second, "$c")
            assertEquals(0, l.lit, "$c")
            assertNull(l.quote, "$c")
        }
        assertEquals("कोटेशन · अजून वाचलेले नाही", line(null, Tx.MR).text)
        assertEquals("कोटेशनचा टप्पा अजून वाचलेला नाही — ऑनलाइन आल्यावर S5 ठरवते", line(null, Tx.MR).second)
    }

    @Test fun `v61c2 - a project S5 does not list reads not on S5 yet, whatever the phone holds`() {
        val held = Cache(listed = false, stage = "Revisions", by = "pm@x", at = at, readAt = now.toString(),
            states = mapOf(10 to WorkState(Album.DONE, 7)))
        val queued = listOf(Album.stateOp(null, 10, Album.NOT_STARTED, Album.IN_PROGRESS, ""))
        // read as not listed, with a stage, states and a queued write the phone still holds; and made on this phone,
        // whatever its copy says -- even listed at Client approved with work under way
        for ((c, phoneOnly) in listOf(Cache(listed = false, readAt = now.toString()) to false, held to false, null to true,
                held to true, approved(mapOf(10 to WorkState(Album.IN_PROGRESS, 7))) to true)) {
            val l = line(c, phoneOnly = phoneOnly, queued = queued)
            assertEquals(LineKind.LOCAL, l.kind, "$c $phoneOnly")
            assertEquals("Quote · not on S5 yet", l.text, "$c $phoneOnly")
            assertEquals("not on S5 yet — this project has not been sent", l.second, "$c $phoneOnly")
            assertEquals(0, l.lit, "no pip is lit")
            assertNull(l.quote, "no stage is named")
            assertEquals(emptyList(), l.parts)
            val mr = line(c, Tx.MR, phoneOnly, queued)
            assertEquals("कोटेशन · अजून S5 वर नाही", mr.text)
            assertEquals("अजून S5 वर नाही — हा प्रोजेक्ट पाठवलेला नाही", mr.second)
        }
        // a not-listed copy read before today carries the read time; a phone-only project is never read, so never does
        assertEquals("not on S5 yet — this project has not been sent · read 17:45", line(Cache(listed = false, readAt = "2026-10-13T12:15:00Z")).second)
        assertEquals("not on S5 yet — this project has not been sent", line(Cache(listed = false, readAt = "2026-10-13T12:15:00Z"), phoneOnly = true).second)
        assertFalse(line(Cache(listed = false, readAt = "2026-10-13T12:15:00Z"), phoneOnly = true).showRead)
    }

    @Test fun `projectLine's read time - an unread, not-listed or quote-journey copy read before today carries it, the approved line never does`() {
        // read 17:45 IST on 13 Oct; now 15:00 IST on 14 Oct (QM0 review: only the QUOTE kind was pinned)
        val old = "2026-10-13T12:15:00Z"
        // unread: a blank stage, a name outside the six, 'On hold' -- each read yesterday
        for (c in listOf(Cache(true, "", "", "", old), Cache(true, "On hold", "", at, old), Cache(true, "Quoted", "", at, old))) {
            val l = line(c)
            assertEquals(LineKind.UNREAD, l.kind, "$c")
            assertEquals("Quote stage not read yet — S5 decides when online · read 17:45", l.second, "$c")
            assertTrue(l.showRead, "$c")
        }
        assertEquals("कोटेशनचा टप्पा अजून वाचलेला नाही — ऑनलाइन आल्यावर S5 ठरवते · 17:45 ला वाचले", line(Cache(true, "", "", "", old), Tx.MR).second)
        // ... and never read: no read time to show
        assertEquals("Quote stage not read yet — S5 decides when online", line(null).second)
        assertFalse(line(null).showRead)
        // unread and read today: none
        assertFalse(line(Cache(true, "", "", "", now.toString())).showRead)
        assertEquals("Quote stage not read yet — S5 decides when online", line(Cache(true, "On hold", "", at, now.toString())).second)
        // not listed, read yesterday: the second line carries it and says so
        val gone = line(Cache(listed = false, readAt = old))
        assertEquals(LineKind.LOCAL, gone.kind)
        assertEquals("not on S5 yet — this project has not been sent · read 17:45", gone.second)
        assertTrue(gone.showRead)
        assertFalse(line(Cache(listed = false, readAt = now.toString())).showRead)
        // Client approved, read yesterday: the second line is exactly the quote, with no read time
        for (states in listOf(null, emptyMap(), mapOf(10 to WorkState(Album.DONE, 7)))) {
            val ex = line(Cache(true, "Client approved", "pm@x", at, old, states))
            assertEquals(LineKind.EXEC, ex.kind, "$states")
            assertEquals("Quote · Client approved", ex.second, "$states")
            assertFalse(ex.showRead, "$states")
        }
        assertEquals("कोटेशन · ग्राहक मंजूर", line(Cache(true, "Client approved", "pm@x", at, old, emptyMap()), Tx.MR).second)
    }

    @Test fun `v61c2 - the store holds no stage of its own for a project S5 does not list`() {
        val stored = approved(mapOf(10 to WorkState(Album.IN_PROGRESS, 7)))
        // a later read that does not list it: the copy is not listed, with no stage, no stamp and no states kept from before
        val gone = QuoteStage.merge(stored, Cache(listed = false, readAt = "2026-10-14T10:00:00Z"), 3L, 2L)
        assertEquals(Cache(listed = false, readAt = "2026-10-14T10:00:00Z"), gone)
        assertNull(gone.states)
        // the same through a read of the list, for a known id the list does not carry
        assertEquals(Cache(listed = false, readAt = "r"), QuoteStage.fromList(emptyList(), listOf("P"), "r", 3L, mapOf("P" to 2L), mapOf("P" to stored))["P"])
        // a held not-listed copy never lends its stage or states to anything (a copy written by an older build, or by hand)
        val held = Cache(listed = false, stage = "Revisions", at = at, readAt = "r", states = mapOf(10 to WorkState(Album.DONE, 7)))
        assertEquals(Cache(listed = false, readAt = "r"), QuoteStage.merge(null, held, 1L, null))
        assertEquals(Cache(listed = false, readAt = "q"), QuoteStage.merge(held, Cache(listed = false, readAt = "q"), 2L, 1L))
        assertEquals(Cache(listed = false, readAt = "r"), QuoteStage.merge(held, Cache(listed = false, readAt = "q"), 1L, 2L), "kept: still nothing of its own")
        // S5 answering for the project (named from a 409) makes it listed: that stage is S5's, not the phone's
        assertEquals(Cache(listed = true, stage = "Survey", readAt = "r", atUnknown = true), QuoteStage.named(Cache(listed = false, readAt = "q"), "Survey", "r"))
    }

    @Test fun `merge - an older read never wins the stage, and a work's state goes by the seq S5 knows it at, both orders (critic S1)`() {
        val list = QuoteStage.fromList(rows(S5Quote.LIST), emptyList(), "2026-11-03T07:01:09Z")["PROJ-DEMO"]!!
        val album = QuoteStage.fromAlbum(assertNotNull(AlbumWire.view(MiniRead, tree(S5Quote.ALBUM))), "2026-11-03T07:01:10Z")
        // S5-L gives work 10 at its STATE ROW's seq (7); the album GET at its BASE (10: a change request was raised after it)
        assertEquals(WorkState(Album.IN_PROGRESS, 7), list.states!![10])
        assertEquals(Album.IN_PROGRESS, album.states!![10]!!.state)
        assertEquals(10L, album.states!![10]!!.seq)
        assertEquals("pm@x", album.by)
        assertEquals(tApproved, album.at)
        // list then album, and album then list: the same states, each at the higher seq it is known at
        val a = QuoteStage.merge(list, album, 2L, 1L).states
        val b = QuoteStage.merge(album, list, 2L, 1L).states
        assertEquals(a, b)
        assertEquals(10L, a!![10]!!.seq)
        assertEquals(Album.DONE, a[20]!!.state)
        assertEquals(album.states!![20], a[20], "an equal seq is one row of S5's: the copy that carries its time")
        // a state row newer than the album's base (a later read of the list) wins; one older never does, whichever lands last
        val later = list.copy(states = mapOf(10 to WorkState(Album.DONE, 11)), readAt = "2026-11-03T07:02:00Z")
        assertEquals(WorkState(Album.DONE, 11), QuoteStage.merge(album, later, 3L, 2L).states!![10])
        assertEquals(WorkState(Album.DONE, 11), QuoteStage.merge(QuoteStage.merge(album, later, 3L, 2L), album, 4L, 3L).states!![10],
            "a slow album GET from before the state row never puts In progress back")
        assertEquals("Execution · 2 done", line(QuoteStage.merge(album, later, 3L, 2L)).text)
        // the stage: an older read whose stage loses still brings newer states; it never rolls the stage back
        val revisions = Cache(true, "Revisions", "pm@x", "2026-11-03T07:01:08.710Z", "2026-11-03T07:01:08Z", mapOf(30 to WorkState(Album.ON_HOLD, 12)))
        val kept = QuoteStage.merge(album, revisions, 5L, 4L)
        assertEquals("Client approved", kept.stage, "S5's later stamp keeps Client approved")
        assertEquals(tApproved, kept.at)
        assertEquals(WorkState(Album.ON_HOLD, 12), kept.states!![30])
        assertEquals(album.states!![10], kept.states!![10])
        // a work absent from a read is a read that did not carry it: the log is append-only, nothing is taken away
        assertEquals(album.states, QuoteStage.merge(album, album.copy(states = emptyMap(), readAt = "x"), 6L, 5L).states)
        // fromList answers an id only when its merged copy differs: a read whose stage loses and brings nothing new writes nothing
        assertTrue(QuoteStage.fromList(rows(S5Quote.LIST).filter { it.project == "PROJ-DEMO" }.map { it.copy(states = emptyMap()) },
            emptyList(), "2026-11-03T07:00:00Z", 1L, mapOf("PROJ-DEMO" to 2L), mapOf("PROJ-DEMO" to album)).isEmpty())
        // ... and one whose stage loses but whose states are newer writes them, the stage kept
        val newer = QuoteStage.fromList(listOf(Listed("PROJ-DEMO", "Revisions", "pm@x", "2026-11-03T07:01:08.710Z", mapOf(10 to WorkState(Album.DONE, 11)))),
            emptyList(), "2026-11-03T07:00:00Z", 1L, mapOf("PROJ-DEMO" to 2L), mapOf("PROJ-DEMO" to album))["PROJ-DEMO"]!!
        assertEquals("Client approved", newer.stage)
        assertEquals(WorkState(Album.DONE, 11), newer.states!![10])
        // both sides null stays null (not read in this shape); one side read is that side
        assertNull(QuoteStage.mergeStates(null, null))
        assertEquals(emptyMap(), QuoteStage.mergeStates(null, emptyMap()))
        assertEquals(mapOf(10 to WorkState(Album.DONE, 2)), QuoteStage.mergeStates(mapOf(10 to WorkState(Album.DONE, 2), 999 to WorkState(Album.DONE, 3)), null))
    }

    @Test fun `an answer that names the stage keeps the work states the store holds`() {
        val withStates = approved(mapOf(10 to WorkState(Album.DONE, 7))).copy(stage = "Revisions")
        assertEquals(mapOf(10 to WorkState(Album.DONE, 7)), QuoteStage.named(withStates, "Client approved", "r")!!.states)
        assertEquals("Execution · 1 done", line(QuoteStage.named(withStates, "Client approved", "r")).text)
    }

    @Test fun `roomDot - before approval the survey journey rule, after it a capture filed to one of the sixteen works`() {
        // before approval (v61b, byte for byte): As-built site measure, or no stage at all
        assertTrue(QuoteStage.roomDot(listOf(Shot(null, QuoteStage.SURVEY_STAGE)), false))
        assertTrue(QuoteStage.roomDot(listOf(Shot(0, "")), false), "a new capture carries stage ''")
        assertTrue(QuoteStage.roomDot(listOf(Shot(null, null)), false), "synced without a stage")
        assertTrue(QuoteStage.roomDot(listOf(Shot(null, "  AS-BUILT  site_measure ")), false), "the prototype's key(): trimmed, lower-cased, runs of spaces and _ as one")
        assertFalse(QuoteStage.roomDot(listOf(Shot(0, "Defect recorded")), false), "NOT workOfCapture == 0: a defect photo of the quote journey stays amber")
        assertFalse(QuoteStage.roomDot(listOf(Shot(null, " ")), false), "a stage of spaces is a stage, as in JavaScript")
        assertFalse(QuoteStage.roomDot(listOf(Shot(50, "Floor tiling")), false))
        assertTrue(QuoteStage.roomDot(listOf(Shot(0, "Defect recorded"), Shot(null, "")), false))
        assertFalse(QuoteStage.roomDot(emptyList(), false))
        // after approval: any capture whose filing (workOfCapture) is a work -- neither the quote journey nor unfiled
        assertTrue(QuoteStage.roomDot(listOf(Shot(50, "")), true))
        assertTrue(QuoteStage.roomDot(listOf(Shot(null, "Plaster & waterproofing")), true), "an old capture filed by its stage, at 20")
        assertFalse(QuoteStage.roomDot(listOf(Shot(0, ""), Shot(null, QuoteStage.SURVEY_STAGE), Shot(null, "")), true), "the quote journey")
        assertFalse(QuoteStage.roomDot(listOf(Shot(null, "Material delivered on site")), true), "unfiled")
        assertTrue(QuoteStage.roomDot(listOf(Shot(999, "Snagging")), true), "a stray id files by its stage (140)")
        assertEquals("As-built site measure", QuoteStage.SURVEY_STAGE)
    }

    @Test fun `coverage - the rooms in scope with a survey capture, and its two parts`() {
        val shots = mapOf("Kitchen" to listOf(Shot(null, QuoteStage.SURVEY_STAGE)), "Hall" to listOf(Shot(0, "Defect recorded")),
            "Bedroom" to listOf(Shot(0, ""), Shot(null, "Opening measured")), "Balcony" to listOf(Shot(null, "")))
        val c = assertNotNull(QuoteStage.coverage(listOf("Kitchen", "Hall", "Bedroom"), shots))
        assertEquals(Coverage(2, 3), c)
        assertEquals(1, c.left)
        assertEquals(listOf("2 of 3 rooms shot", "1 room not shot yet"), QuoteStage.coverageParts(Tx.EN, c))
        assertEquals(listOf("3 खोल्या पैकी 2 चे फोटो झाले", "1 खोली: अजून फोटो नाहीत"), QuoteStage.coverageParts(Tx.MR, c))
        val all = assertNotNull(QuoteStage.coverage(listOf("Kitchen", "Balcony"), shots))
        assertEquals(listOf("2 of 2 rooms shot", "every room in scope is shot"), QuoteStage.coverageParts(Tx.EN, all))
        assertEquals(listOf("स्कोपमधील प्रत्येक खोलीचे फोटो झाले"), QuoteStage.coverageParts(Tx.MR, all).drop(1))
        assertEquals(Coverage(0, 1), QuoteStage.coverage(listOf("Study"), shots), "a room with no capture")
        assertEquals(listOf("0 of 1 room shot", "1 room not shot yet"), QuoteStage.coverageParts(Tx.EN, Coverage(0, 1)))
        assertNull(QuoteStage.coverage(emptyList(), shots), "no room in scope: no coverage row")
    }
}
