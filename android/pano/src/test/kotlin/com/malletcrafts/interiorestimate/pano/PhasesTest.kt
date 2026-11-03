package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Phases.Tap
import com.malletcrafts.interiorestimate.pano.QuoteStage.Cache
import com.malletcrafts.interiorestimate.pano.QuoteStage.LineKind
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The phase strip's pure half (port 61c slice QM1), against SPEC61c §4.1 item 3 and index61c2.html (PHASES, stripHTML,
 * request): the five segments and what a tap on each does, the does-line, the backward confirm, SURVEY COVERAGE and
 * IN REVISION -- the listed subset of the prototype's strip (answer 1 of 28 Oct 2026) -- and v61c.2's never-sent
 * project, which offers no move. The English and the Marathi a pin expects are written out by hand.
 */
class PhasesTest {

    private val now: Instant = Instant.parse("2026-11-03T08:00:00Z")
    private fun at(stage: String) = Cache(listed = true, stage = stage, by = "pm@x", at = "2026-11-03T07:01:08.710Z", readAt = "2026-11-03T07:01:09Z")
    private fun line(cache: Cache?, phoneOnly: Boolean = false, tx: Tx = Tx.EN) = QuoteStage.projectLine(cache, phoneOnly, emptyList(), tx, now)

    @Test fun `the five phases are the prototype's PHASES - names, does-lines and quote stages, in both languages`() {
        val en = Phases.all(Tx.EN)
        assertEquals(listOf(1, 2, 3, 4, 5), en.map { it.n })
        assertEquals(listOf("Survey", "Wish list", "Vendor review", "Estimation", "Revisions"), en.map { it.name })
        // each stands for S5's stage at the same index: the prototype's PHASE_OF_STAGE is the identity
        assertEquals(QuoteStage.STAGES.dropLast(1), en.map { it.quote })
        for (i in 0 until Phases.N) assertEquals(QuoteStage.STAGES[i], Phases.stageOf(i))
        assertEquals(5, Phases.N)
        // the does-lines, word for word as approved -- the three describing marks, trades and packs included (answer 1)
        assertEquals(listOf(
            "Capture · bound the walls · mark what is there",
            "Walk the client through: mark the wishes",
            "Trades and vendor packs, from the wishes",
            "Hand-off to the estimate · nothing new",
            "Iterate the estimate with the client and vendors",
        ), en.map { it.does })
        val mr = Phases.all(Tx.MR)
        assertEquals(listOf("सर्व्हे", "इच्छा-यादी", "व्हेंडर तपासणी", "अंदाजपत्रक", "फेरबदल"), mr.map { it.name })
        assertEquals(listOf(
            "कॅप्चर · भिंतींची हद्द आखा · जे आहे ते मार्क करा",
            "ग्राहकाला सोबत फिरवा: इच्छा मार्क करा",
            "ट्रेड आणि व्हेंडर पॅक, इच्छांवरून",
            "अंदाजपत्रकाकडे सुपूर्द · नवीन काही नाही",
            "ग्राहक व व्हेंडरसोबत अंदाजपत्रकात फेरबदल करा",
        ), mr.map { it.does })
        assertEquals(QuoteStage.STAGES.dropLast(1), mr.map { it.quote }, "the stage is an identity value in either language")
        // the opened quote view's five ticked steps are these names (QuoteView.segments draws them from here)
        assertEquals(Phases.names(Tx.EN), QuoteView.segments(Tx.EN).map { it.name })
        assertEquals(Phases.names(Tx.MR), QuoteView.segments(Tx.MR).map { it.name })
    }

    @Test fun `phaseOf - the line's kind decides - 0 to 4 through the quote journey, 5 at Client approved, null unread or not on S5`() {
        for ((i, s) in QuoteStage.STAGES.dropLast(1).withIndex()) assertEquals(i, Phases.phaseOf(line(at(s))), s)
        assertEquals(Phases.N, Phases.phaseOf(line(at("Client approved"))))
        // unread: never read, a blank stage, a name outside the six (critic G6)
        assertNull(Phases.phaseOf(line(null)))
        assertNull(Phases.phaseOf(line(Cache(listed = true, readAt = "r"))))
        assertNull(Phases.phaseOf(line(at("On hold"))))
        // not on S5: made on this phone, or read as not listed -- whatever the phone holds (v61c.2)
        assertNull(Phases.phaseOf(line(at("Revisions"), phoneOnly = true)))
        assertNull(Phases.phaseOf(line(Cache(listed = false, readAt = "r"))))
    }

    @Test fun `segments at Vendor review - the past ticked, the current marked, every other a tap, labels with now`() {
        val segs = Phases.segments(Tx.EN, line(at("Vendor quoted")))
        assertEquals(listOf(true, true, false, false, false), segs.map { it.past })
        assertEquals(listOf(false, false, true, false, false), segs.map { it.on })
        assertEquals(listOf(true, true, false, true, true), segs.map { it.tap }, "forward and back are taps; the current one is not")
        assertEquals("Phase 3 of 5: Vendor review (now)", segs[2].label)
        assertEquals("Phase 1 of 5: Survey", segs[0].label)
        assertEquals("Phase 5 of 5: Revisions", segs[4].label)
        assertEquals(listOf(1, 2, 3, 4, 5), segs.map { it.n })
        val mr = Phases.segments(Tx.MR, line(at("Vendor quoted"), tx = Tx.MR))
        assertEquals("फेज 3/5: व्हेंडर तपासणी (सध्या)", mr[2].label)
        assertEquals("फेज 1/5: सर्व्हे", mr[0].label)
        // at Survey nothing is past and four are taps forward; at Revisions four are past and four are taps back
        assertEquals(listOf(false, true, true, true, true), Phases.segments(Tx.EN, line(at("Survey"))).map { it.tap })
        assertTrue(Phases.segments(Tx.EN, line(at("Survey"))).none { it.past })
        assertEquals(listOf(true, true, true, true, false), Phases.segments(Tx.EN, line(at("Revisions"))).map { it.past })
        assertEquals(listOf(true, true, true, true, false), Phases.segments(Tx.EN, line(at("Revisions"))).map { it.tap })
        // movable: a read stage before Client approved, with its stamp
        for (s in QuoteStage.STAGES.dropLast(1)) assertTrue(Phases.movable(line(at(s))), s)
    }

    @Test fun `unread or not on S5 - no segment is current, past or a tap, and the does-line says why, whole - and at Client approved none is a tap`() {
        for (l in listOf(line(null), line(at("Revisions"), phoneOnly = true), line(Cache(listed = false, readAt = "r")), line(at("Client approved")))) {
            val segs = Phases.segments(Tx.EN, l)
            assertTrue(segs.none { it.on }, "${l.kind}"); assertTrue(segs.none { it.tap }, "${l.kind}")
            assertTrue(segs.none { it.label.contains("(now)") }, "${l.kind}")
            assertFalse(Phases.movable(l), "${l.kind}")
        }
        assertTrue(Phases.segments(Tx.EN, line(null)).none { it.past })
        assertTrue(Phases.segments(Tx.EN, line(at("Revisions"), phoneOnly = true)).none { it.past })
        assertTrue(Phases.segments(Tx.EN, line(at("Client approved"))).all { it.past }, "at Client approved every segment is past: the strip folds into the quote view")
        // the does-line: the never-sent words for a project S5 does not list, read whole (data-s5none wraps)
        val local = line(at("Revisions"), phoneOnly = true)
        assertEquals(LineKind.LOCAL, local.kind)
        assertEquals("not on S5 yet — this project has not been sent", Phases.doesLine(Tx.EN, local))
        assertEquals("अजून S5 वर नाही — हा प्रोजेक्ट पाठवलेला नाही", Phases.doesLine(Tx.MR, line(at("Revisions"), phoneOnly = true, tx = Tx.MR)))
        assertTrue(Phases.wraps(local))
        assertEquals("not on S5 yet — this project has not been sent", Phases.doesLine(Tx.EN, line(Cache(listed = false, readAt = "r"))), "read as not listed: the same words")
        // the same words the mover refuses with, so the strip and the status line never say two things
        assertEquals(QuoteMove.localText(Tx.EN, QuoteMove.Local.NOT_ON_S5), Phases.doesLine(Tx.EN, local))
        // unread: the prototype's UNREAD, cut with an ellipsis like every phase's line (no wrap)
        val unread = line(null)
        assertEquals(LineKind.UNREAD, unread.kind)
        assertEquals("Phase not read yet — S5 decides when online", Phases.doesLine(Tx.EN, unread))
        assertEquals("फेज अजून वाचलेली नाही — ऑनलाइन आल्यावर S5 ठरवते", Phases.doesLine(Tx.MR, line(null, tx = Tx.MR)))
        assertFalse(Phases.wraps(unread))
        assertEquals("Phase not read yet — S5 decides when online", Phases.doesLine(Tx.EN, line(at("On hold"))), "a stage outside the six is unread (critic G6)")
        // through the quote journey: the phase's does-line
        assertEquals("Capture · bound the walls · mark what is there", Phases.doesLine(Tx.EN, line(at("Survey"))))
        assertEquals("Iterate the estimate with the client and vendors", Phases.doesLine(Tx.EN, line(at("Revisions"))))
        assertFalse(Phases.wraps(line(at("Survey"))))
        // at Client approved the strip is the quote view's: no does-line
        assertEquals("", Phases.doesLine(Tx.EN, line(at("Client approved"))))
    }

    @Test fun `a stage named without its stamp - the phase is shown, no segment is a tap, and the does-line says S5 decides (QM1 review)`() {
        // the wish list's Survey 409 names the stage and nothing more (QuoteStageSync.named): the store's copy has no base to
        // send, so QuoteMove.stage builds nothing on it -- and the first version still offered every other segment as a tap,
        // which then did nothing and said nothing
        val named = QuoteStage.named(at("Client wish list"), "Survey", "2026-11-03T07:01:10Z")!!
        assertTrue(named.atUnknown)
        val l = line(named)
        assertEquals(LineKind.QUOTE, l.kind)
        assertTrue(l.atUnknown, "the line carries the mark")
        assertEquals(0, Phases.phaseOf(l), "the phase is known: Survey")
        assertFalse(Phases.movable(l))
        val segs = Phases.segments(Tx.EN, l)
        assertEquals(listOf(false, false, false, false, false), segs.map { it.tap }, "no segment is a tap")
        assertEquals(listOf(true, false, false, false, false), segs.map { it.on }, "the current one is still marked")
        assertTrue(segs.none { it.past })
        assertEquals("Phase 1 of 5: Survey (now)", segs[0].label)
        // the does-line says why, in both languages: S5's read decides when the next tap may go
        assertEquals("Phase not read yet — S5 decides when online", Phases.doesLine(Tx.EN, l))
        assertEquals("फेज अजून वाचलेली नाही — ऑनलाइन आल्यावर S5 ठरवते", Phases.doesLine(Tx.MR, line(named, tx = Tx.MR)))
        assertFalse(Phases.wraps(l))
        // the same at a later stage, named by the wish list's final 409's sibling: Revisions, the past ones ticked, no tap
        val later = line(QuoteStage.named(at("Estimated"), "Revisions", "r")!!)
        assertEquals(4, Phases.phaseOf(later))
        assertEquals(listOf(true, true, true, true, false), Phases.segments(Tx.EN, later).map { it.past })
        assertTrue(Phases.segments(Tx.EN, later).none { it.tap })
        assertEquals("Phase not read yet — S5 decides when online", Phases.doesLine(Tx.EN, later))
        // the mover agrees: nothing is built on the named copy, so the strip and the mover never disagree
        assertEquals(QuoteMove.Move.None, QuoteMove.stage("PROJ-DEMO", named, Phases.stageOf(1)))
        // the read that fills the stamp in makes every other segment a tap again, with the phase's own does-line
        val filled = QuoteStage.merge(named, Cache(true, "Survey", "pm@x", "2026-11-03T07:01:09.500Z", "2026-11-03T07:01:11Z"), 3L, 2L)
        assertFalse(filled.atUnknown)
        val f = line(filled)
        assertFalse(f.atUnknown); assertTrue(Phases.movable(f))
        assertEquals(listOf(false, true, true, true, true), Phases.segments(Tx.EN, f).map { it.tap })
        assertEquals("Capture · bound the walls · mark what is there", Phases.doesLine(Tx.EN, f))
        assertEquals(QuoteMove.Move.Send::class, QuoteMove.stage("PROJ-DEMO", filled, Phases.stageOf(1))::class)
        // no other line carries the mark: a stamped copy, one S5 never timed, unread, not on S5
        assertFalse(line(at("Revisions")).atUnknown)
        assertFalse(line(Cache(listed = true, stage = "Survey", readAt = "r")).atUnknown, "a stage S5 never set is not unknown: base '' is sent")
        assertTrue(Phases.movable(line(Cache(listed = true, stage = "Survey", readAt = "r"))))
        assertFalse(line(null).atUnknown); assertFalse(line(at("Revisions"), phoneOnly = true).atUnknown)
        assertFalse(line(Cache(listed = false, readAt = "r")).atUnknown)
    }

    @Test fun `tap - forward moves at once, back asks first - the current segment, an unread stage and Client approved do nothing`() {
        assertEquals(Tap.MOVE, Phases.tap(0, 1)); assertEquals(Tap.MOVE, Phases.tap(0, 4)); assertEquals(Tap.MOVE, Phases.tap(2, 3))
        assertEquals(Tap.ASK, Phases.tap(1, 0)); assertEquals(Tap.ASK, Phases.tap(4, 0)); assertEquals(Tap.ASK, Phases.tap(3, 2))
        for (i in 0 until Phases.N) {
            assertEquals(Tap.NONE, Phases.tap(i, i), "the current segment")
            assertEquals(Tap.NONE, Phases.tap(null, i), "unread, or not on S5: no segment is a tap")
            assertEquals(Tap.NONE, Phases.tap(Phases.N, i), "approval is final: no back-move from Client approved")
        }
        assertEquals(Tap.NONE, Phases.tap(2, 5), "no segment stands for Client approved: Approve is the scope sheet's (QM2)")
        assertEquals(Tap.NONE, Phases.tap(2, -1))
    }

    @Test fun `the backward confirm - the question names the phase and S5's stages, Keep the current phase, Move back, in both languages`() {
        val a = Phases.ask(Tx.EN, 3, 1)
        assertEquals("Back to Wish list? The quote stage on S5 goes back from Estimated to Client wish list.", a.question)
        assertEquals("Every mark stays; the tools follow the phase.", a.aux)
        assertEquals("Back to Wish list? The quote stage on S5 goes back from Estimated to Client wish list. Every mark stays; the tools follow the phase.", a.whole)
        assertEquals("Keep Estimation", a.keep)
        assertEquals("Move back", a.moveBack)
        assertEquals("Move the phase back", a.label)
        // the phase's name where the prototype prints to.name, S5's stage where it prints p.quote / to.quote
        val b = Phases.ask(Tx.EN, 4, 0)
        assertEquals("Back to Survey? The quote stage on S5 goes back from Revisions to Survey.", b.question)
        assertEquals("Keep Revisions", b.keep)
        val c = Phases.ask(Tx.EN, 2, 1)
        assertEquals("Back to Wish list? The quote stage on S5 goes back from Vendor quoted to Client wish list.", c.question)
        assertEquals("Keep Vendor review", c.keep)
        val m = Phases.ask(Tx.MR, 3, 1)
        assertEquals("इच्छा-यादी वर परत जायचे? S5 वरचा कोटेशनचा टप्पा अंदाजपत्रक झाले वरून ग्राहकाची इच्छा-यादी वर परत जातो.", m.question)
        assertEquals("प्रत्येक मार्क राहतो; साधने फेजनुसार बदलतात.", m.aux)
        assertEquals("अंदाजपत्रक ठेवा", m.keep)
        assertEquals("मागे न्या", m.moveBack)
        assertEquals("फेज मागे न्या", m.label)
    }

    @Test fun `SURVEY COVERAGE at Survey and IN REVISION at Revisions - the listed subset's two rows, and the strip's group label`() {
        val two = Phases.coverageRow(Tx.EN, QuoteStage.Coverage(2, 3))
        assertEquals("SURVEY COVERAGE", two.label)
        assertEquals(listOf("2 of 3 rooms shot", "1 room not shot yet"), two.parts)
        assertEquals(listOf("3 of 3 rooms shot", "every room in scope is shot"), Phases.coverageRow(Tx.EN, QuoteStage.Coverage(3, 3)).parts)
        assertEquals(listOf("0 of 1 room shot", "1 room not shot yet"), Phases.coverageRow(Tx.EN, QuoteStage.Coverage(0, 1)).parts)
        assertEquals(" · ", Phases.SEP)
        assertEquals("SURVEY COVERAGE · 2 of 3 rooms shot · 1 room not shot yet", (listOf(two.label) + two.parts).joinToString(Phases.SEP), "landscape draws the row on one line")
        val mr = Phases.coverageRow(Tx.MR, QuoteStage.Coverage(2, 3))
        assertEquals("सर्व्हे कव्हरेज", mr.label)
        assertEquals(2, mr.parts.size)
        assertTrue(mr.parts.none { it.isBlank() } && mr.parts != two.parts, "the figures are Marathi too")
        assertEquals("स्कोपमधील प्रत्येक खोलीचे फोटो झाले", Phases.coverageRow(Tx.MR, QuoteStage.Coverage(3, 3)).parts[1])
        // IN REVISION: the one part the phone has (the hand-off counts come with the v53/v54 port)
        assertEquals(Phases.Panel("IN REVISION", listOf("open for the client and vendors")), Phases.revisionRow(Tx.EN))
        assertEquals(Phases.Panel("फेरबदल सुरू", listOf("ग्राहक व व्हेंडरसाठी खुले")), Phases.revisionRow(Tx.MR))
        assertEquals(0, Phases.SURVEY); assertEquals(4, Phases.REVISIONS)
        assertEquals("Phase of this project", Phases.groupLabel(Tx.EN)); assertEquals("या प्रोजेक्टची फेज", Phases.groupLabel(Tx.MR))
    }
}
