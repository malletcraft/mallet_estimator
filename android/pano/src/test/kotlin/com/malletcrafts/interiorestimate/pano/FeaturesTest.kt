package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Album.WorkState
import com.malletcrafts.interiorestimate.pano.QuoteStage.Cache
import com.malletcrafts.interiorestimate.pano.QuoteStage.LineKind
import com.malletcrafts.interiorestimate.pano.QuoteStage.ProjectLine
import com.malletcrafts.interiorestimate.pano.QuoteStage.Shot
import com.malletcrafts.interiorestimate.pano.i18n.I18n
import com.malletcrafts.interiorestimate.pano.i18n.LiteralScan
import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * SITE PROGRESS BEHIND ONE FLAG, OFF, NOTHING DELETED (port 63 slice SP0, 16 Nov 2026). Amit, 15 Nov 2026, after his
 * first use of the app on a client site: "Lets focus on survey to quote approval workflow first. Site progress will do
 * post I confirm that survey to quote looks good. Do not delete any site progress code but toggle it as feature flag."
 *
 * Three things are held here. (1) The switches themselves ([Features.SITE_PROGRESS] off; [Features.HELP_TOURS] off too
 * since port 63 slice M2, the build that changes the measuring screen -- its gates and what it keeps are the last tests).
 * (2) What each pure rule answers with site progress OFF -- every call states the switch, so these pass whichever way
 * the constant stands; the same rules with it ON are held where they always were (QuoteStageTest, CaptureFilingTest,
 * PhaseToolsTest, the Album tests), each stating `siteProgress = true`. (3) :app read as source, since only CI
 * compiles it: every gate reads the ONE constant, the list of gates is complete, and every site-progress file and
 * its wiring is still there.
 *
 * TO TURN SITE PROGRESS BACK ON: `SITE_PROGRESS = true` in Features.kt, and the first test and the declaration's pin
 * below follow it. Nothing else moves.
 *
 * (5) THE CHAIN TOOL'S SWITCH (port 63 slice F1, 16 Nov 2026; Amit, with build 0.3.337 on his phone: "chain mark is
 * rarely used. get rid of it."): [Features.CHAINS] is off; every entry that could start a new chain reads it -- three
 * pure rules of :pano, each taking it as the default of its last parameter, which :app asks with no answer of its own;
 * a room that already has a chain keeps it, drawn, picked, measured, counted and deletable; and every file of the
 * chain is still there.
 *
 * (6) THE TAPE TOOL'S SWITCH (port 63 slice F3a, 23 Nov 2026; Amit, with build 0.3.350 on his phone: "does tape tool
 * make any sense now? as we don't want any foto measured dimensions on fone."): [Features.TAPES] is off; every entry
 * that could lay a new Tape reads it, and so does every pure rule that gives a Tape a figure or a graduation on the
 * screen's own painters -- each taking it as the default of its last parameter, which :app asks with no answer of its
 * own; a room that already has a Tape keeps it, drawn, picked, moved, opened, locked and deletable; and every file of
 * the Tape is still there. What :app's two painters still ask by themselves until slice F3b switches them over is
 * pinned as a list, so the hand-over is four named lines. The last tests.
 */
class FeaturesTest {

    // 14 Oct 2026, 08:08 IST, as S5 writes it; "now" on the phone is 15:00 IST the same day.
    private val at = "2026-10-14T02:38:00.000Z"
    private val now: Instant = Instant.parse("2026-10-14T09:30:00Z")
    private val yesterday = "2026-10-13T09:00:00Z"

    private val busy = mapOf(10 to WorkState(Album.DONE, 2), 20 to WorkState(Album.IN_PROGRESS, 3))

    private fun approved(states: Map<Int, WorkState>? = busy, at: String = this.at, readAt: String = now.toString(), atUnknown: Boolean = false) =
        Cache(true, QuoteStage.APPROVED, "pm@x", at, readAt, states, atUnknown)

    private fun line(c: Cache?, siteProgress: Boolean, tx: Tx = Tx.EN, phoneOnly: Boolean = false, queued: List<Album.Op> = emptyList()) =
        QuoteStage.projectLine(c, phoneOnly, queued, tx, now, siteProgress = siteProgress)

    // ---- (1) the switches -----------------------------------------------------------------------------------

    @Test fun `site progress is off, and so are the help tours - with the build that ships the new measuring screen`() {
        val siteProgress: Boolean = Features.SITE_PROGRESS
        assertFalse(siteProgress, "Features.SITE_PROGRESS is on: everything after Client approved shows again (Amit, 15 Nov 2026: site progress " +
            "waits until he confirms that survey to quote looks good)")
        // Amit, 15 Nov 2026: "For time being hold help tour as well from development as it can be done once I approve survey
        // to quote journey its functionality etc." -- and the survey tour, on the phone since 0.3.318, goes off (code kept)
        // in the SAME build that ships the new measuring screen, slice M2: its step 11 teaches the list that build removes
        val tours: Boolean = Features.HELP_TOURS
        assertFalse(tours, "Features.HELP_TOURS is on: the survey tour would teach 3 · List, a step the phone no longer has (port 63 slice M2)")
        assertFalse("list" in StepRoute.ROUTE, "the screen the tour's step 11 teaches is gone")
        assertEquals(TourDone.Mode("list"), TourFlows.all(Tx.EN).single { it.id == "survey" }.steps[10].done, "survey.11 still asks for the list: the tour is rewritten after Amit approves the journey")
    }

    // ---- (2) the pure rules with site progress off ----------------------------------------------------------

    @Test fun `with site progress off the approved project's line says the approved state in the quote journey's words, and counts nothing`() {
        val want = ProjectLine(LineKind.EXEC, "Client approved", "Quote · Client approved", emptyList(), "since 14 Oct, 08:08 IST on S5", 6, false, "")
        assertEquals(want, line(approved(), false))
        // whatever the phone holds of the works: read or not, none started, some running, held, abandoned
        for (states in listOf(null, emptyMap(), busy, mapOf(50 to WorkState(Album.ON_HOLD, 2)), mapOf(10 to WorkState(Album.ABANDONED, 4))))
            assertEquals(want, line(approved(states), false), "$states")
        // a state write this login queued shows nowhere on it
        val tapped = Album.stateOp(null, 90, Album.NOT_STARTED, Album.IN_PROGRESS, "")
        assertEquals(want, line(approved(), false, queued = listOf(tapped)))
        // approval is final, so a copy read yesterday still says where the project is: no read time, as the execution line has none
        assertEquals(want, line(approved(readAt = yesterday), false))
        // the second line is the quote journey's own rule: a stage an answer NAMED without its stamp, and one S5 has not timed
        val named = line(approved(at = "", atUnknown = true), false)
        assertEquals("Quote · Client approved", named.text)
        assertEquals("Client approved · not read yet", named.second)
        assertTrue(named.atUnknown)
        assertEquals("Client approved · not set on S5 yet", line(approved(at = ""), false).second)
        // in Marathi, hand-typed (a Masters lookup wired to the wrong map prints the same English under Tx.EN)
        val mr = line(approved(), false, tx = Tx.MR)
        assertEquals("कोटेशन · ग्राहक मंजूर", mr.text)
        assertEquals("S5 वर 14 Oct, 08:08 IST पासून", mr.second)
        // nothing of execution, in either language, in either line
        for (l in listOf(want, named, mr)) for (word in listOf("Execution", "अंमलबजावणी", "in progress", "done", "started")) {
            assertFalse(word in l.text, "$word in ${l.text}")
            assertFalse(word in l.second, "$word in ${l.second}")
        }
        // and the side that is hidden is whole: the same copy with site progress on
        val on = line(approved(), true)
        assertEquals(ProjectLine(LineKind.EXEC, "Client approved", "Execution · 1 in progress · 1 done", listOf("1 in progress", "1 done"),
            "Quote · Client approved", 6, false, ""), on)
    }

    @Test fun `before approval every line is the same with site progress on or off`() {
        val copies: List<Pair<Cache?, Boolean>> = listOf<Pair<Cache?, Boolean>>(
            null to false, null to true, Cache(listed = false, readAt = yesterday) to false,
            Cache(true, "", "", "", now.toString()) to false, Cache(true, "Quoted", "", at, now.toString()) to false,
        ) + QuoteStage.STAGES.dropLast(1).flatMap { s ->
            listOf(
                Cache(true, s, "staff@x", at, now.toString()), Cache(true, s, "staff@x", at, yesterday), Cache(true, s, "", "", now.toString()),
                Cache(true, s, "", "", now.toString(), atUnknown = true), Cache(true, s, "staff@x", at, now.toString(), busy),
            ).map { it to false }
        }
        assertEquals(5 + 5 * 5, copies.size, "never read, only on this phone, not listed, a blank stage, a name outside the six, and five copies of each quote-journey stage")
        for ((c, phoneOnly) in copies) for (tx in listOf(Tx.EN, Tx.MR))
            assertEquals(line(c, true, tx, phoneOnly), line(c, false, tx, phoneOnly), "$c")
        // the lines compared are the quote journey's, not two blanks that agree
        val estimated = line(Cache(true, "Estimated", "staff@x", at, now.toString()), false)
        assertEquals("Quote · Estimated", estimated.text)
        assertEquals("since 14 Oct, 08:08 IST on S5", estimated.second)
        assertEquals(LineKind.QUOTE, estimated.kind)
    }

    @Test fun `the approved line stays the Client approved line - the strip stays folded, nothing moves, and survey prep's band says the stage alone`() {
        val off = line(approved(), false)
        // every reader that asks WHERE the project is gets Client approved, as with site progress on
        assertEquals(Phases.N, Phases.phaseOf(off))
        assertFalse(Phases.movable(off))
        assertTrue(Phases.segments(Tx.EN, off).none { it.tap || it.on }, "approval is final: no segment is a tap")
        assertTrue(Phases.segments(Tx.EN, off).all { it.past })
        assertEquals("", Phases.doesLine(Tx.EN, off))
        assertEquals(Phases.Approve(null, null), Phases.approve(Tx.EN, off, Album.PM))
        // survey prep's band: handed no line, so it says the stage's name alone, never `Client approved · Quote · Client approved`
        assertNull(QuoteStage.bandLine(off, siteProgress = false))
        val band = PhaseTools.band(Tx.EN, 5, QuoteStage.bandLine(off, siteProgress = false))
        assertEquals("Client approved", band.whole)
        assertEquals("Client approved", band.label)
        assertEquals("", band.rest)
        assertEquals(5, band.lit)
        // with site progress on the band carries the execution line, as it did
        val on = line(approved(), true)
        assertEquals("Execution · 1 in progress · 1 done", QuoteStage.bandLine(on, siteProgress = true))
        assertEquals("Client approved · Execution · 1 in progress · 1 done", PhaseTools.band(Tx.EN, 5, QuoteStage.bandLine(on, siteProgress = true)).whole)
        // through the quote journey, unread and for a project S5 does not list the band is handed no line either way
        for (c in listOf(null, Cache(listed = false), Cache(true, "Revisions", "", at, now.toString()))) for (sp in listOf(true, false))
            assertNull(QuoteStage.bandLine(line(c, sp), siteProgress = sp), "$c, site progress $sp")
    }

    @Test fun `with site progress off the room's dot keeps the survey's rule after approval`() {
        val rooms = listOf(
            listOf(Shot(0, "")), listOf(Shot(null, QuoteStage.SURVEY_STAGE)), listOf(Shot(null, null)), listOf(Shot(0, "Defect recorded")),
            listOf(Shot(50, "Floor tiling")), listOf(Shot(null, "Plaster & waterproofing")), listOf(Shot(999, "Snagging")), listOf(Shot(50, "")),
            listOf(Shot(0, "Defect recorded"), Shot(null, "")), emptyList(),
        )
        for (r in rooms) {
            assertEquals(QuoteStage.roomDot(r, false, siteProgress = false), QuoteStage.roomDot(r, true, siteProgress = false), "approved or not: $r")
            assertEquals(QuoteStage.roomDot(r, false, siteProgress = true), QuoteStage.roomDot(r, false, siteProgress = false), "before approval, on or off: $r")
        }
        // the two rules do differ after approval, so the equalities above are not two of the same thing
        val survey = listOf(Shot(0, ""))
        assertTrue(QuoteStage.roomDot(survey, true, siteProgress = false), "a surveyed room stays green")
        assertFalse(QuoteStage.roomDot(survey, true, siteProgress = true), "execution's dot asks for a capture filed to a work")
        val work = listOf(Shot(null, "Plaster & waterproofing"))
        assertFalse(QuoteStage.roomDot(work, true, siteProgress = false))
        assertTrue(QuoteStage.roomDot(work, true, siteProgress = true))
    }

    @Test fun `with site progress off execution never begins - a new capture stays the Quote journey's, and the WORK row says where it is filed and no more`() {
        for (stage in QuoteStage.STAGES + listOf(null, "", " Client approved ", "client approved"))
            assertFalse(Album.execution(stage, siteProgress = false), "$stage")
        assertTrue(Album.execution("Client approved", siteProgress = true))
        assertTrue(Album.execution(" Client approved ", siteProgress = true), "trimmed, as Album.approved reads it")
        for (stage in QuoteStage.STAGES.dropLast(1) + listOf(null, "", "client approved"))
            assertFalse(Album.execution(stage, siteProgress = true), "$stage")
        // WHERE THE NEXT CAPTURE FILES (MainActivity: Album.newCaptureWork(fileApproved, …), fileApproved being Album.execution)
        val inProgress = mapOf(20 to WorkState(Album.IN_PROGRESS, 3))
        assertEquals(Album.QUOTE_WORK, Album.newCaptureWork(Album.execution("Client approved", siteProgress = false), 90, inProgress))
        assertEquals(Album.QUOTE_WORK, Album.newCaptureWork(Album.execution("Client approved", siteProgress = false), null, null),
            "never `Pick the work`: the sheet's buttons do not wait")
        assertEquals(90, Album.newCaptureWork(Album.execution("Client approved", siteProgress = true), 90, inProgress))
        assertNull(Album.newCaptureWork(Album.execution("Client approved", siteProgress = true), null, null))
        // THE WORK ROW AND LINE'S SECOND TEXT after approval: where it is filed, and neither the tap nor the lock
        assertEquals("Client approved", Album.workSub(Tx.EN, true, 0, "Client approved", null, siteProgress = false), "the capture sheet's row")
        assertEquals("Estimated", Album.workSub(Tx.EN, true, 0, "Estimated", null, siteProgress = false), "a photo's line: the stage it was taken in")
        assertEquals("In progress", Album.workSub(Tx.EN, true, 20, "Survey", Album.IN_PROGRESS, siteProgress = false))
        assertEquals("not filed to a work", Album.workSub(Tx.EN, true, null, "Survey", null, siteProgress = false))
        assertEquals("कोणत्याही कामात ठेवलेला नाही", Album.workSub(Tx.MR, true, null, "", null, siteProgress = false))
        for (w in listOf(0, 20, null)) for (tx in listOf(Tx.EN, Tx.MR)) {
            val sub = Album.workSub(tx, true, w, "Client approved", Album.IN_PROGRESS, siteProgress = false)
            for (gone in listOf("tap to change", "बदलण्यासाठी टॅप करा", "locked", "लॉक")) assertFalse(gone in sub, "$gone in $sub")
        }
        // BEFORE approval too (SP0 review): the lock's `until the client approves the estimate` promises a pick that the
        // switch never gives, so with site progress off the words stop at where the capture is filed at EVERY stage
        assertEquals("Survey", Album.workSub(Tx.EN, false, 0, "Survey", null, siteProgress = false))
        assertEquals("Not started", Album.workSub(Tx.EN, false, 20, "Survey", null, siteProgress = false), "an old capture filed to a work by its stage")
        assertEquals("In progress", Album.workSub(Tx.EN, false, 20, "Survey", Album.IN_PROGRESS, siteProgress = false))
        assertEquals("not filed to a work", Album.workSub(Tx.EN, false, null, "Survey", null, siteProgress = false))
        for (approved in listOf(false, true)) for (w in listOf(0, 20, 90, null)) for (tx in listOf(Tx.EN, Tx.MR)) {
            val sub = Album.workSub(tx, approved, w, Masters.quote(tx, "Survey"), null, siteProgress = false)
            for (gone in listOf("tap to change", "बदलण्यासाठी टॅप करा", "locked", "लॉक", "approves", "मंजूर")) assertFalse(gone in sub, "$gone in `$sub` (approved $approved, work $w)")
            assertEquals(Album.workSub(tx, true, w, Masters.quote(tx, "Survey"), null, siteProgress = false), sub, "the same words on both sides of approval")
        }
        // with it ON the lock is what it always was (CaptureFilingTest holds the rest)
        assertEquals("Survey · locked until the client approves the estimate", Album.workSub(Tx.EN, false, 0, "Survey", null, siteProgress = true))
    }

    @Test fun `with site progress off the WORK row and line are drawn only for a capture filed to a work - never for the Quote journey, never an unanswerable Pick the work`() {
        // THE CAPTURE 360 SHEET: every new capture files at the Quote journey with the switch off, so its row is never drawn
        for (stage in QuoteStage.STAGES + listOf(null, "")) for (pick in listOf(null, 20, 90)) {
            val next = Album.newCaptureWork(Album.execution(stage, siteProgress = false), pick, mapOf(20 to WorkState(Album.IN_PROGRESS, 3)))
            assertEquals(Album.QUOTE_WORK, next, "$stage / $pick")
            assertFalse(Album.workRowShown(next, siteProgress = false), "no WORK row on the sheet at $stage")
        }
        // THE PHOTO'S LINE: the Quote journey has none; an unfiled capture has none (its head would be `Pick the work`, which
        // nothing answers -- the top bar's `not filed to a work` is where that is read); a capture filed to a work keeps it
        assertFalse(Album.workRowShown(Album.QUOTE_WORK, siteProgress = false))
        assertFalse(Album.workRowShown(null, siteProgress = false))
        assertEquals("Pick the work", Album.filingHead(Tx.EN, null), "the head the unfiled line would have drawn")
        assertEquals("not filed to a work", Album.workName(Tx.EN, null), "and what the photo's top bar reads instead")
        for (w in Album.WORKS) assertTrue(Album.workRowShown(w.id, siteProgress = false), "${w.id} is a filing worth reading")
        for (stray in listOf(-1, 5, 999)) assertFalse(Album.workRowShown(stray, siteProgress = false), "$stray is no work")
        // with site progress ON the row and the line are always drawn, as they always were
        for (w in listOf<Int?>(null, Album.QUOTE_WORK, 20, 90, 999)) assertTrue(Album.workRowShown(w, siteProgress = true), "$w")
    }

    // ---- (3) the gates, read as source (only CI compiles :app) ----------------------------------------------

    private val app = "app/src/main/kotlin/com/malletcrafts/interiorestimate/"
    private val pano = "pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/"
    private val tests = "pano/src/test/kotlin/com/malletcrafts/interiorestimate/pano/"

    private val lexed = HashMap<String, String>()

    /** One source under android/ with its comments and the bodies of its string literals blanked (the scanner's own lexer): CODE alone. */
    private fun code(rel: String): String = lexed.getOrPut(rel) {
        val f = I18n.android.resolve(rel)
        assertTrue(f.isFile, "$rel is there (${f.absolutePath})")
        LiteralScan.scan(I18n.android, rel).masked
    }

    private fun count(hay: String, needle: String): Int = Regex.fromLiteral(needle).findAll(hay).count()

    /** Every Kotlin source of a module's main tree, as its path under android/. */
    private fun sources(root: String): List<String> = I18n.android.resolve(root).walkTopDown()
        .filter { it.isFile && it.name.endsWith(".kt") }.map { it.relativeTo(I18n.android).invariantSeparatorsPath }.sorted().toList()

    private val read = "Features.SITE_PROGRESS"

    @Test fun `one constant is the switch - declared once, off, with the tours' switch beside it, off too`() {
        val features = code(pano + "Features.kt")
        assertEquals(1, count(features, "const val SITE_PROGRESS: Boolean = false"), "the switch is a constant, and it is off")
        assertEquals(1, count(features, "const val HELP_TOURS: Boolean = false"), "the tours' switch has its home, and is off (port 63 slice M2)")
        assertEquals(1, count(features, "const val CHAINS: Boolean = false"), "the chain tool's switch has its home, and is off (port 63 slice F1)")
        // (port 63 slice F3a moved this pin from three to four: the Tape tool's switch joined, off)
        assertEquals(1, count(features, "const val TAPES: Boolean = false"), "the Tape tool's switch has its home, and is off (port 63 slice F3)")
        assertEquals(4, count(features, "const val "), "four switches and nothing else")
        // no second switch, anywhere a screen is built from: nothing else declares or assigns one
        val all = LiteralScan.ROOTS.flatMap { sources(it) }
        assertTrue(all.size > 100, "the three main trees were walked (${all.size} sources)")
        val declared = all.filter { Regex("""\bSITE_PROGRESS\b\s*(?::\s*Boolean\s*)?=[^=]""").containsMatchIn(code(it)) }
        assertEquals(listOf(pano + "Features.kt"), declared, "SITE_PROGRESS is given a value in one place")
        // and no rule is handed the answer by the app: every gate takes the constant itself (a `siteProgress = …` is a test's)
        val handed = all.filter { it.startsWith("app/") || it.startsWith("camera/") }.filter { "siteProgress" in code(it) }
        assertEquals(emptyList(), handed, ":app never overrides the switch")
    }

    @Test fun `every gate reads the flag, and the gates are these and no others`() {
        // THE INVENTORY: each source that reads the switch, and how many times. A new gate is added HERE, with its reason.
        val readers = mapOf(
            // the six pure rules: the default of each one's last parameter
            pano + "QuoteStage.kt" to 3,     // projectLine (the execution line), roomDot (execution's dot), bandLine (survey prep's band)
            pano + "Album.kt" to 3,          // execution (filing by work begins), workSub (the WORK row's and line's words), workRowShown (whether they are drawn)
            // :app's entries
            app + "MainActivity.kt" to 1,    // the album's tile: the album's one entry
            app + "AlbumOutbox.kt" to 3,     // enqueue, enqueueAll, AlbumDrainWorker.doWork: the album outbox's drain
            app + "WorkRefile.kt" to 3,      // enqueue, sweep, WorkRefileWorker.doWork: the re-file by work
            app + "AsIsUpload.kt" to 1,      // refileRefused: the refused re-file's line on survey prep
        )
        val found = LiteralScan.ROOTS.flatMap { sources(it) }.associateWith { count(code(it), read) }.filterValues { it > 0 }
        assertEquals(readers.toSortedMap(), found.toSortedMap(), "the sources that read Features.SITE_PROGRESS, and how often")

        // :pano -- each rule takes the switch as the DEFAULT of its last parameter, so both of its sides stay tested
        val stage = code(pano + "QuoteStage.kt")
        val album = code(pano + "Album.kt")
        val default = "siteProgress: Boolean = Features.SITE_PROGRESS"
        assertEquals(3, count(stage, default)); assertEquals(3, count(album, default))
        for ((src, fn) in listOf(stage to "projectLine", stage to "roomDot", stage to "bandLine", album to "execution", album to "workSub", album to "workRowShown"))
            assertTrue(Regex("""fun $fn\((?:[^()]|\([^()]*\))*siteProgress: Boolean = Features\.SITE_PROGRESS,?\s*\)""").containsMatchIn(src), "$fn's last parameter is the switch")
        assertTrue("if (!siteProgress) {" in stage, "projectLine turns to the approved state's words")
        assertTrue("if (approved && siteProgress) captures.any" in stage, "roomDot keeps the survey's rule")
        assertTrue("line.text.takeIf { siteProgress && line.kind == LineKind.EXEC }" in stage, "bandLine hands the band no line")
        assertTrue("siteProgress && approved(quoteStage)" in album, "execution is approval AND the switch")
        assertTrue("!siteProgress -> lead" in album, "workSub stops at where the capture is filed")
        assertTrue(album.indexOf("!siteProgress -> lead") in 0 until album.indexOf("!approved -> lead + tx.app_stg_locked_quote"),
            "and the switch is asked BEFORE the stage: the lock's words are site progress's on both sides of approval (SP0 review)")
        assertTrue("Boolean = siteProgress || isWork(work)" in album, "workRowShown: always with the switch on, else only a capture filed to a work")

        // :app -- THE ALBUM: no tile, and the tile is the one place the album opens from
        val main = code(app + "MainActivity.kt")
        val tile = main.indexOf("val albumTile = if (!Features.SITE_PROGRESS) null else proj?.let { p ->")
        assertTrue(tile > 0, "the album's tile is behind the switch")
        assertEquals(2, count(main, "albumFor = "), "the album is opened in one place and left in one")
        assertEquals(1, count(main, "albumFor = null"))
        val opened = main.indexOf("albumFor = p.key")
        assertTrue(opened in tile until main.indexOf("val quoteArgs", tile), "and it is opened by the tile's tap alone")
        assertEquals(1, count(main, "album = albumTile)"), "the rooms grid is handed the tile, or none")
        assertEquals(1, count(code(app + "BrowseScreens.kt"), "album?.let { a -> item { AlbumTile(a) } }"), "no tile, no cell: the grid closes up")

        // :app -- FILING BY WORK: both screens ask Album.execution, and the project's own stage only for the row's words
        assertEquals(1, count(main, "val fileApproved = Album.execution(fileCache?.stage)"), "the capture sheet")
        assertEquals(1, count(main, "val capApproved = Album.execution(capCache?.stage)"), "the photo's WORK line")
        assertEquals(2, count(main, "Album.execution("))
        assertEquals(1, count(main, "Album.workSub(tx, Album.approved(fileCache?.stage), newWork,"))
        assertEquals(1, count(main, "Album.workSub(tx, Album.approved(capCache?.stage), cap.work,"))
        assertEquals(2, count(main, "Album.approved("), "nothing in MainActivity decides by the stage alone but the two rows' words")
        // THE WORK ROW AND LINE THEMSELVES (SP0 review): each is drawn only when the rule says so, asked with the work it
        // would name -- the sheet's with where the next capture files, the photo's with the capture's own filing
        assertEquals(1, count(main, "workShown = Album.workRowShown(newWork),"), "the capture sheet's row")
        assertEquals(1, count(main, "workShown = Album.workRowShown(cap.work),"), "the photo's line")
        assertEquals(2, count(main, "Album.workRowShown("))
        val tree = code(app + "TreeShell.kt")
        assertEquals(1, count(tree, "workShown: Boolean,"), "CaptureSheet takes the answer")
        assertTrue(Regex("""if \(workShown\) \{\s*ListItem\(\s*headlineContent = \{ Text\(workHead\) \},""").containsMatchIn(tree), "and draws its WORK row only then")
        assertEquals(1, count(tree, "headlineContent = { Text(workHead) },"), "the one place the row is drawn")
        val shots = code(app + "CaptureScreens.kt")
        assertEquals(1, count(shots, "workShown: Boolean,"), "CaptureScreen takes the answer")
        assertTrue(Regex("""if \(workShown\) \{\s*DetailRow\(\s*lead = tx\.album_work_caps,""").containsMatchIn(shots), "and draws its WORK line only then")
        assertEquals(1, count(shots, "lead = tx.album_work_caps,"), "the one place the line is drawn")
        // what hangs off the two answers, each still wired as it was
        for (use in listOf(
            "val newWork = Album.newCaptureWork(fileApproved, pickKey?.let { capWorkFor[it] }, fileStates)",
            "if (fileApproved && workNow == null) {",
            "if (fileApproved && albumKey != null && login.isNotEmpty() && S5Session.signedIn(context)) {",
            "onWork = if (fileApproved) ({ showCaptureSheet = false; pickWork = true }) else null,",
            "noWork = fileApproved && newWork == null,",
            "onPickWork = if (capApproved) ({ refileWork = true }) else null,",
        )) assertEquals(1, count(main, use), use)
        // the room's dot and the project line take the switch from the rule's default: no third argument, no second rule
        assertEquals(1, count(main, "}.map { QuoteStage.Shot(it.work, it.stage) }, projectLine.kind == QuoteStage.LineKind.EXEC)"))
        val sync = code(app + "QuoteStageSync.kt")
        assertEquals(1, count(sync, "QuoteStage.bandLine(lineFor(context, stageKey, tx))"), "survey prep's band asks the rule")
        assertEquals(1, count(sync, "QuoteStage.projectLine("), "one caller draws the line")
        assertFalse("LineKind.EXEC" in sync, "and none decides for itself which line is execution's")

        // :app -- THE WORKERS: the gate is the FIRST statement of each way in
        fun first(src: String, signature: String, gate: String) =
            assertTrue(Regex(Regex.escape(signature) + """\s*\{\s*""" + Regex.escape(gate) + """(?![\w.])""").containsMatchIn(src), "$signature opens with `$gate`")
        val outbox = code(app + "AlbumOutbox.kt")
        first(outbox, "fun enqueue(context: Context, project: String, replace: Boolean = true)", "if (!Features.SITE_PROGRESS) return")
        first(outbox, "suspend fun enqueueAll(context: Context)", "if (!Features.SITE_PROGRESS) return")
        first(outbox.substring(outbox.indexOf("class AlbumDrainWorker")), "override suspend fun doWork(): Result", "if (!Features.SITE_PROGRESS) return Result.success()")
        val refile = code(app + "WorkRefile.kt")
        first(refile, "fun enqueue(context: Context, captureId: String)", "if (!Features.SITE_PROGRESS) return")
        first(refile, "fun sweep(context: Context, store: CaptureStore)", "if (!Features.SITE_PROGRESS) return")
        first(refile.substring(refile.indexOf("class WorkRefileWorker")), "override suspend fun doWork(): Result", "if (!Features.SITE_PROGRESS) return Result.success()")
        assertEquals(1, count(code(app + "AsIsUpload.kt"),
            "fun refileRefused(context: Context, id: String): String? = if (!Features.SITE_PROGRESS) null else runCatching<String?> {"), "the refused re-file's line")
        // every way a drain or a re-file is queued goes through those two functions: nobody else builds their requests
        val builders = sources("app/src/main/kotlin").filter { val c = code(it); "OneTimeWorkRequestBuilder<AlbumDrainWorker>" in c || "OneTimeWorkRequestBuilder<WorkRefileWorker>" in c }
        assertEquals(listOf(app + "AlbumOutbox.kt", app + "WorkRefile.kt"), builders)
        assertEquals(1, count(outbox, "OneTimeWorkRequestBuilder<AlbumDrainWorker>")); assertEquals(1, count(refile, "OneTimeWorkRequestBuilder<WorkRefileWorker>"))
    }

    @Test fun `nothing of site progress was deleted - every file, every screen and worker, and the wiring that reaches them`() {
        // THE FILES, and in each what it is there for
        val kept = mapOf(
            app + "AlbumScreen.kt" to listOf("fun AlbumScreen(", "fun AlbumTile(", "class AlbumTileArgs("),
            app + "AlbumSheets.kt" to listOf("fun AlbumStateSheet(", "fun AlbumRaiseSheet(", "fun AlbumAnswerSheet(", "fun AlbumRoomSheet("),
            app + "AlbumCamera.kt" to listOf("fun AlbumCamera("),
            app + "AlbumImages.kt" to listOf("object AlbumImages", "fun AlbumImage("),
            app + "AlbumShots.kt" to listOf("object AlbumShots", "fun file(", "fun single("),
            app + "AlbumOutbox.kt" to listOf("object AlbumOutbox", "fun add(", "fun addSingle(", "fun refileSingle(", "internal fun run(", "class AlbumDrainWorker("),
            app + "AlbumSync.kt" to listOf("object AlbumSync", "fun refresh(", "fun mirror(", "fun take("),
            app + "WorkRefile.kt" to listOf("object WorkRefile", "fun enqueue(", "fun sweep(", "class WorkRefileWorker("),
            app + "TreeShell.kt" to listOf("fun WorkSheet("),
            pano + "Album.kt" to listOf("object Album", "fun chapters(", "fun stateWrite(", "fun crRaise(", "fun crAnswer(", "fun photoRecord(", "fun review(", "fun newCaptureWork("),
            pano + "AlbumPage.kt" to listOf("object AlbumPage", "fun model(", "fun page(", "fun tile(", "fun sheet(", "fun camera("),
            pano + "AlbumQueue.kt" to listOf("object AlbumQueue"),
            pano + "AlbumShot.kt" to listOf("object AlbumShot"),
            pano + "AlbumWire.kt" to listOf("object AlbumWire"),
            pano + "AlbumWrite.kt" to listOf("object AlbumWrite", "fun state(", "fun raise(", "fun answer(", "fun review(", "fun shot(", "fun single("),
        )
        for ((rel, has) in kept) {
            val c = code(rel)
            for (d in has) assertTrue(d in c, "$rel still has `$d`")
        }
        // THEIR TESTS, which hold the hidden side whichever way the switch stands
        for (t in listOf("AlbumTest.kt", "AlbumPageTest.kt", "AlbumQueueTest.kt", "AlbumShotTest.kt", "AlbumWireTest.kt", "AlbumWriteTest.kt",
            "CaptureFilingTest.kt", "S5Album.kt"))
            assertTrue(I18n.android.resolve(tests + t).isFile, "$t is still there")
        // THE WIRING: gated at its entry, never unhooked -- the album's branch and every tap on it, the two Which work?
        // sheets, the single photo's hand-over, the re-file, and the sync's two catch-ups
        val main = code(app + "MainActivity.kt")
        for (wired in listOf(
            "if (proj != null && albumFor != null && proj.key == albumFor) {", "AlbumScreen(", "AlbumPage.model(", "AlbumPage.page(",
            "AlbumWrite.state(albumModel,", "AlbumWrite.raise(albumModel,", "AlbumWrite.answer(albumModel,", "AlbumWrite.review(albumModel,",
            "AlbumWrite.openCam(albumModel)", "AlbumWrite.shot(albumModel, it)", "AlbumShots.file(context, pend,", "AlbumOutbox.add(context, k, login, op, seen)",
            "AlbumShots.single(context, albumKey, login, op, pano)", "AlbumOutbox.refileSingle(context, key, login, cap.deviceId, from, work, landed)",
            "store.setWork(cap.deviceId, work)", "if (WorkRefile.onWeb(context, cap.deviceId)) WorkRefile.enqueue(context, cap.deviceId)",
            "AlbumPage.tile(tx, p.title, mirror, projectLine, albumItems)",
        )) assertEquals(1, count(main, wired), wired)
        assertEquals(2, count(main, "WorkSheet("), "Which work? from the capture sheet's row and from the photo's line")
        assertEquals(2, count(main, "AlbumOutbox.enqueue("), "the project open's drain and a tap's")
        val sync = code(app + "SyncWorker.kt")
        assertEquals(1, count(sync, "WorkRefile.sweep(applicationContext, store)"))
        assertEquals(1, count(sync, "AlbumOutbox.enqueueAll(applicationContext)"))
        // THE MIRROR'S READ IS NOT GATED: the Approve and the approved quote view draw S5's role and scope from it
        assertFalse(read in code(app + "AlbumSync.kt"))
        assertEquals(1, count(sync, "AlbumSync.refresh(applicationContext, p.serverId)"))
        assertEquals(1, count(main, "val got = withContext(Dispatchers.IO) { AlbumSync.refresh(context, id) }"))
    }

    // ---- (4) the help tours' switch (port 63 slice M2) -------------------------------------------------------

    @Test fun `with the help tours off nothing ships, and with them on the survey flow does - the rule takes the switch as its default`() {
        // the default IS the switch: off, the Help screen would list nothing and the drawer's row would read "0 tours"
        assertEquals(emptyList(), TourFlows.shipped(Tx.EN).map { it.id }); assertEquals(emptyList(), TourFlows.shipped(Tx.MR).map { it.id })
        assertEquals(TourFlows.shipped(Tx.EN, tours = Features.HELP_TOURS).map { it.id }, TourFlows.shipped(Tx.EN).map { it.id })
        assertEquals(emptyList(), TourFlows.shipped(Tx.EN, tours = false).map { it.id })
        // both sides stay tested: on, exactly what shipped before -- nothing was deleted
        assertEquals(listOf("survey"), TourFlows.shipped(Tx.EN, tours = true).map { it.id })
        assertEquals(12, TourFlows.shipped(Tx.MR, tours = true).single().steps.size)
        assertEquals(7, TourFlows.all(Tx.EN).size, "every flow is still there")
    }

    @Test fun `every gate of the help tours reads the one constant - the shipped set, the drawer's Help row and the demo's seeding - and nothing of the tours was deleted`() {
        val tours = "Features.HELP_TOURS"
        // THE INVENTORY: each source that reads the switch, and how many times
        val found = LiteralScan.ROOTS.flatMap { sources(it) }.associateWith { count(code(it), tours) }.filterValues { it > 0 }
        assertEquals(mapOf(
            pano + "Tour.kt" to 1,           // TourFlows.shipped: the default of its last parameter
            app + "MainActivity.kt" to 2,    // the demo flat's seeding at start, and the drawer's Help group
        ).toSortedMap(), found.toSortedMap(), "the sources that read Features.HELP_TOURS, and how often")
        val declared = LiteralScan.ROOTS.flatMap { sources(it) }.filter { Regex("""\bHELP_TOURS\b\s*(?::\s*Boolean\s*)?=[^=]""").containsMatchIn(code(it)) }
        assertEquals(listOf(pano + "Features.kt"), declared, "HELP_TOURS is given a value in one place")
        // :pano -- the rule takes the switch as the DEFAULT of its last parameter
        assertEquals(1, count(code(pano + "Tour.kt"),
            "fun shipped(tx: Tx, tours: Boolean = Features.HELP_TOURS): List<TourFlow> = if (!tours) emptyList() else all(tx).filter { it.ships }"))
        // :app -- the Help screen and the drawer's count still ask the rule, with no answer of their own
        val main = code(app + "MainActivity.kt")
        assertEquals(1, count(main, "tours = remember(tx) { TourFlows.shipped(tx).size },"))
        assertEquals(1, count(code(app + "HelpScreen.kt"), "val flows = remember(tx) { TourFlows.shipped(tx) }"))
        assertFalse("tours = " in code(app + "HelpScreen.kt"), "the Help screen never overrides the switch")
        // THE DRAWER'S HELP GROUP: not drawn with the tours off (it would read "0 tours"), so the Help screen has no way in
        assertEquals(1, count(main, "if (Features.HELP_TOURS) DrawerGroup(tx.common_help, listOf("))
        assertTrue(Regex("""if \(Features\.HELP_TOURS\) DrawerGroup\(tx\.common_help, listOf\(\s*DrawerLine\(tx\.help_row, value = [^\n]*\n\s*icon = R\.drawable\.ic_mcft_info, onClick = onHelp\),\s*\)\) else null,""").containsMatchIn(main),
            "the whole group is behind the switch")
        assertEquals(1, count(main, "): List<DrawerGroup> = listOfNotNull("), "a group that is not drawn leaves no gap")
        assertEquals(1, count(main, "onHelp = { helpOpen = true },"), "the row's tap is still wired")
        assertEquals(1, count(main, "DrawerLine(tx.help_row,"), "and it is the one way in")
        // THE DEMO FLAT: not seeded on a fresh install with the tours off; what a phone holds is left alone (nothing removes it)
        assertEquals(1, count(main, "if (Features.HELP_TOURS) withContext(Dispatchers.IO) { runCatching { TourSeed.ensure(context, store) } }"))
        assertEquals(1, count(main, "TourSeed.ensure("), "the one place the demo is seeded")
        // NOTHING OF THE TOURS WAS DELETED: the flows, the machine, the overlay, the Help screen, the seeding, the bridge
        // into survey prep and its four anchors there
        val kept = mapOf(
            pano + "Tour.kt" to listOf("object TourFlows", "object TourAnchors", "fun all(tx: Tx): List<TourFlow>", "private fun survey(tx: Tx)"),
            pano + "TourMachine.kt" to listOf("class TourMachine("),
            pano + "TourDemo.kt" to listOf("object TourDemo"),
            pano + "TourPlace.kt" to listOf("object TourPlace"),
            app + "TourOverlay.kt" to listOf("object TourState", "fun Modifier.tourAnchor(", "internal fun TourBindSurvey(", "fun TourOverlay(host: Int)", "object TourSeed",
                "fun ensure(context: Context, store: CaptureStore)"),
            app + "HelpScreen.kt" to listOf("fun HelpScreen(onBack: () -> Unit, onStart: (TourFlow) -> Unit)"),
        )
        for ((rel, has) in kept) { val c = code(rel); for (d in has) assertTrue(d in c, "$rel still has `$d`") }
        assertEquals(1, count(main, "HelpScreen("), "the Help screen's branch is still there")
        assertEquals(1, count(main, "TourSeed.restore(context, store, r.snap)")); assertEquals(1, count(main, "TourSeed.snapshot(context, store)"))
        // survey prep's four anchors (save, tab:<tab>, mode:<step>, lines:row), its bridge and its own overlay: the anchors'
        // names are string bodies, blanked in the code read here, so they are counted by their call and their host
        val prepScreen = code(app + "SurveyPrepScreen.kt")
        assertEquals(4, count(prepScreen, "tourAnchor("), "save, tab:<tab>, mode:<step> and lines:row")
        assertEquals(5, count(prepScreen, "TourState.HOST_SURVEY"), "each of the four in survey prep's own window, and its own overlay")
        assertEquals(1, count(prepScreen, "TourBindSurvey(tab, mode,")); assertEquals(1, count(prepScreen, "TourOverlay(TourState.HOST_SURVEY)"))
    }

    // ---- (5) the chain tool's switch (port 63 slice F1) -------------------------------------------------------

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0
    private fun ab(s: String): DoubleArray = SurveyPrep.size(s, L, W, H)
    private fun pt(x: Double, y: Double) = doubleArrayOf(x, y)

    /** A front wall a surveyor already drew a chain on, and read one segment of: a door, a tape and the chain FK1. */
    private fun roomWithChain(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        p.surface("front").marks.add(SurveyPrep.Mark("d1", "D", "FD1", 300.0, 1200.0, 0.0, 2100.0))
        p.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 1000.0), pt(1234.0, 1000.0)), "t1")
        p.addChain("front", "x", listOf(pt(300.0, 2400.0), pt(1200.0, 2400.0), pt(2400.0, 2400.0)), listOf(null, "FD1 right", null), "k1")
        p.actual["FK1.1"] = 905.0
        return p
    }

    @Test fun `the chain tool is off - no slot offers it, the tool that is on is never it, and a chain is never copied`() {
        // Amit, 16 Nov 2026, with build 0.3.337 on his phone: "chain mark is rarely used. get rid of it."
        val chains: Boolean = Features.CHAINS
        assertFalse(chains, "Features.CHAINS is on: the palette offers the Chain again and a new chain can be drawn")
        // ENTRY 1, THE PALETTE: its default IS the switch; off, no slot's key is the Chain's on any kind of surface
        assertEquals(SurveyPrepPalette.offered(chains = Features.CHAINS), SurveyPrepPalette.offered())
        for (kind in listOf("wall", "floor", "ceil")) {
            assertEquals(SurveyPrepPalette.row(kind, SurveyPrepPalette.offered(chains = Features.CHAINS)).map { it.key }, SurveyPrepPalette.row(kind).map { it.key })
            assertFalse(SurveyPrepPalette.CHAIN in SurveyPrepPalette.row(kind, SurveyPrepPalette.offered(chains = false)).map { it.key }, kind)
            assertTrue(SurveyPrepPalette.CHAIN in SurveyPrepPalette.row(kind, SurveyPrepPalette.offered(chains = true)).map { it.key }, "$kind: the other side is whole")
        }
        // ENTRY 2, THE TOOL THAT IS ON: whatever was picked, at every stage and unread
        for (st in listOf(0, 1, 2, 3, 4, 5, null)) {
            assertEquals(SurveyPrepPalette.SEL, PhaseTools.toolFor(SurveyPrepPalette.CHAIN, st, chains = false), "at $st")
            assertEquals(PhaseTools.toolFor(SurveyPrepPalette.CHAIN, st, chains = Features.CHAINS), PhaseTools.toolFor(SurveyPrepPalette.CHAIN, st))
        }
        assertEquals(SurveyPrepPalette.CHAIN, PhaseTools.toolFor(SurveyPrepPalette.CHAIN, 0, chains = true), "on, Survey draws chains as before")
        // ... so the 360's press never reaches the Chain's own step, and 2 - Mark's never its branch: both ask the tool that is on
        for (st in listOf(0, 1, 2, 3, 4, 5, null)) for (drafting in listOf(false, true))
            assertFalse(Pano360.Step.CHAIN in Pano360.order(PhaseTools.toolFor(SurveyPrepPalette.CHAIN, st), drafting), "at $st")
        // ENTRY 3, DUPLICATE ON A CHAIN: a copy would be a new chain
        val p = roomWithChain()
        for (st in listOf(0, 1, 2, 3, 4, 5, null)) for (picked in listOf(null, "exist", "wish", "vadd")) {
            assertFalse(PhaseTools.copyOk(p, "k1", st, picked, chains = false), "at $st, $picked")
            assertEquals(PhaseTools.copyOk(p, "k1", st, picked, chains = Features.CHAINS), PhaseTools.copyOk(p, "k1", st, picked))
            assertFalse(SurveyPrepEdit.DUPLICATE in SurveyPrepEdit.menuItemsOf(Tx.EN, p, "k1", st, picked).map { it.first }, "the menu at $st, $picked")
        }
        assertTrue(PhaseTools.copyOk(p, "k1", 0, null, chains = true), "on, a chain is copied wherever it may be edited")
        // NO HELP LINE NAMES IT: no tour that ships -- none does while the tours are off -- says Chain, in either language
        for (tx in listOf(Tx.EN, Tx.MR)) for (f in TourFlows.shipped(tx, tours = true)) for (st in f.steps)
            for (word in listOf("Chain", "chain", tx.common_chain))
                assertFalse(word in st.title || word in st.body || word in f.name || word in f.line, "${f.id} ${st.id} names $word")
    }

    @Test fun `a room that already has a chain keeps it - read and written unchanged, picked, measured, counted, locked and deleted`() {
        val p = roomWithChain(); val z = ab("front")
        val k = p.chainById("k1")!!.second
        // THE FILE: it is written, read back and written again byte for byte -- nothing of it is dropped on the way
        val o = SurveyPrepFile.encode(p, L, W, H)
        val d = SurveyPrepFile.decode(o)!!
        assertTrue(d.problems.isEmpty())
        val k2 = d.prep.chainById("k1")!!.second
        assertEquals("FK1", k2.code); assertEquals("x", k2.axis); assertEquals(k.nodes.map { it.toList() }, k2.nodes.map { it.toList() })
        assertEquals(listOf(null, "FD1 right", null), k2.snaps); assertEquals(905.0, d.prep.actual["FK1.1"])
        assertEquals(o.toString(), SurveyPrepFile.encode(d.prep, L, W, H).toString())
        // IT IS AN ITEM OF THE FACE LIKE ANY OTHER: listed, owned, labelled
        assertEquals(listOf("FD1", "F-T1", "FK1"), p.itemsOf("front").map { it.code })
        assertEquals("k1", p.ownerOf("FK1.2")!!.id); assertEquals("FK1 chain", SurveyPrepEdit.labelOf(Tx.EN, p, "k1"))
        // PICKED: a tap within 12 dp of its line; its point within 16 dp (screen px = mm here)
        val scr = listOf("k1" to k.nodes)
        assertEquals("k1", SurveyChain.pick(scr, 800.0, 2405.0, 12.0)); assertEquals("k1" to 1, SurveyChain.nodeAt(scr, 1205.0, 2395.0, 16.0))
        // MEASURED: its segments and its total are stops of 3 - Measure's walk, in the counts, and take a reading
        val stops = MeasureWalk.stops(p, "front", z[0], z[1])
        assertEquals(listOf("FK1.1", "FK1.2", "FK1.t"), stops.filter { it.startsWith("FK1.") })
        assertEquals(listOf("FK1.1", "FK1.2", "FK1.t"), MeasureWalk.stopsOf(p, "k1") { ab(it) })
        val before = MeasureWalk.count(p, "front", z[0], z[1])
        assertTrue(MeasureFlow.takes(p, "FK1.2", 0) { ab(it) }); assertEquals("FK1.2", MeasureFlow.firstOpen(p, "k1", 0) { ab(it) })
        assertEquals("FK1.2", MeasureFlow.figure(p, "FK1.2", null, 0) { ab(it) }.active)
        p.actual["FK1.2"] = 1200.0
        val after = MeasureWalk.count(p, "front", z[0], z[1])
        // one more read; and with both segments read the total is DERIVED, so it is no longer left -- on neither side, as ever
        assertEquals(before.read + 1, after.read); assertEquals(before.left - 2, after.left)
        assertFalse(MeasureWalk.left(p, "front", z[0], z[1], "FK1.t"))
        assertEquals(MeasureWalk.count(p, "front", z[0], z[1]).left, MeasureFlow.tabLeft(p, "front", 0, z[0], z[1]))
        // its own menu: everything but Duplicate -- Lock, Lock all chains, Cancel, and Delete last
        assertEquals("Lock|Lock all chains|Cancel|Delete", SurveyPrepEdit.menuItemsOf(Tx.EN, p, "k1", 0, null).joinToString("|") { it.second })
        assertTrue(PhaseTools.editable(p, "k1", 0) && PhaseTools.lockOk(p, "k1", 0) && PhaseTools.delOk(p, "k1", 0))
        assertEquals(true, SurveyPrepEdit.setLocked(p, "k1", true)); assertFalse(p.deleteMark("k1"), "locked, it is not deleted")
        assertEquals(false, SurveyPrepEdit.setLocked(p, "k1", false))
        // its points still move along it (the chain's own code, untouched)
        assertTrue(SurveyChain.nodeMove("front", k, 1, pt(1250.0, 2400.0), z[0], z[1], emptyList(), 0.0))
        assertEquals(1250.0, k.nodes[1][0])
        // DELETED: it goes with its readings and nothing else's; one undo brings it and them back
        val snap = p.copy()
        assertTrue(p.deleteMark("k1"))
        assertNull(p.chainById("k1")); assertTrue(p.actual.keys.none { it.startsWith("FK1.") })
        assertEquals(listOf("FD1", "F-T1"), p.itemsOf("front").map { it.code })
        val back = SurveyPrepEdit.undoTo(p, snap)
        assertEquals(listOf("FD1", "F-T1", "FK1"), back.itemsOf("front").map { it.code })
        assertEquals(905.0, back.actual["FK1.1"]); assertEquals(1200.0, back.actual["FK1.2"])
    }

    @Test fun `every entry of the chain tool reads the one constant, the app overrides none, and nothing of the chain was deleted`() {
        val chains = "Features.CHAINS"
        val all = LiteralScan.ROOTS.flatMap { sources(it) }
        // THE INVENTORY: each source that reads the switch, and how many times -- all in :pano, each the default of a
        // pure rule's last parameter
        val found = all.associateWith { count(code(it), chains) }.filterValues { it > 0 }
        assertEquals(mapOf(
            pano + "SurveyPrepPalette.kt" to 1,   // offered: what the palette draws
            pano + "PhaseTools.kt" to 2,          // toolFor: the tool that is on; copyOk: Duplicate on a chain
            pano + "SurveyPrepEdit.kt" to 1,      // menuItemsOf: the menu's Duplicate row, through copyOk
            // port 63 slice I2a: the panel that slides up carries the menu's rows -- acts, panel and actStep hand the
            // switch on to menuItemsOf, each as the default of its last parameter but one (shape is the last, as there)
            pano + "MarkSheet.kt" to 3,
        ).toSortedMap(), found.toSortedMap(), "the sources that read Features.CHAINS, and how often")
        val declared = all.filter { Regex("""\bCHAINS\b\s*(?::\s*Boolean\s*)?=[^=]""").containsMatchIn(code(it)) }
        assertEquals(listOf(pano + "Features.kt"), declared, "CHAINS is given a value in one place")
        // (port 63 slice F3a moved the seven signature pins below: each rule gained the Tape tool's switch, `tapes`, as the
        // default of its LAST parameter -- the chain switch is where it was in each, and still its own default)
        assertEquals(1, count(code(pano + "SurveyPrepPalette.kt"), "fun offered(chains: Boolean = Features.CHAINS, tapes: Boolean = Features.TAPES): List<String> ="))
        assertEquals(1, count(code(pano + "SurveyPrepPalette.kt"), "TOOLS.filter { (it != CHAIN || chains) && (it != TAPE || tapes) }"))
        assertEquals(1, count(code(pano + "SurveyPrepPalette.kt"), "fun row(kind: String, tools: List<String> = offered()): List<RowSlot> {"))
        assertEquals(1, count(code(pano + "PhaseTools.kt"), "fun toolFor(tool: String, stage: Int?, chains: Boolean = Features.CHAINS, tapes: Boolean = Features.TAPES): String ="))
        assertEquals(1, count(code(pano + "PhaseTools.kt"), "fun copyOk(p: SurveyPrep.Prep, id: String, stage: Int?, picked: String?, chains: Boolean = Features.CHAINS,"))
        assertEquals(1, count(code(pano + "PhaseTools.kt"), "!chains && p.chainById(id) != null -> false"))
        assertEquals(1, count(code(pano + "SurveyPrepEdit.kt"), "DUPLICATE -> PhaseTools.copyOk(p, id, stage, picked, chains, tapes)"))
        assertEquals(1, count(code(pano + "MarkSheet.kt"), "fun acts(tx: Tx, p: SurveyPrep.Prep, id: String, stage: Int?, picked: String?, chains: Boolean = Features.CHAINS, shape: Boolean = false,"))
        assertEquals(1, count(code(pano + "MarkSheet.kt"), "val rows = SurveyPrepEdit.menuItemsOf(tx, p, id, stage, picked, chains, shape, tapes)"))
        assertEquals(3, count(code(pano + "MarkSheet.kt"), "chains: Boolean = Features.CHAINS, shape: Boolean = false,"), "acts, panel and actStep")
        // :app NEVER OVERRIDES IT: no app source names the switch or hands a rule its own answer
        val apps = all.filter { it.startsWith("app/") || it.startsWith("camera/") }
        assertTrue(apps.size > 40, "the app's sources were walked (${apps.size})")
        for (rel in apps) {
            assertEquals(0, count(code(rel), "chains ="), "$rel hands a rule the answer")
            assertEquals(0, count(code(rel), "offered("), "$rel asks what is offered by itself")
        }
        val screen = code(app + "SurveyPrepScreen.kt"); val tools = code(app + "SurveyPrepTools.kt"); val p3 = code(app + "SurveyPrep360.kt")
        // ENTRY 1: the palette draws the slots the rule gives, weighed alike -- no list of its own, so no gap
        assertEquals(1, count(tools, "for (slot in SurveyPrepPalette.row(kind)) {"))
        for (rel in apps) assertEquals(if (rel == app + "SurveyPrepTools.kt") 1 else 0, count(code(rel), "SurveyPrepPalette.row("), rel)
        assertEquals(1, count(tools, "val slotMod = Modifier.weight(1f).height(if (land) 46.dp else 50.dp)"), "every slot the same share of the row")
        // ENTRY 2: ONE tool is on, the rule's answer, and both views, the palette and More are handed that one
        assertEquals(1, count(screen, "val tool = PhaseTools.toolFor(toolSet, quoteStage)"))
        for (rel in apps) assertEquals(if (rel == app + "SurveyPrepScreen.kt") 1 else 0, count(code(rel), "PhaseTools.toolFor("), rel)
        assertEquals(1, count(screen, "Sp360(Modifier.fillMaxSize(), previewPano, p3Look, tab, rev, prep, roomSize, selNow(), pick.gold, tool,"))
        assertEquals(1, count(screen, "tool = tool,"), "2 - Mark's photo")
        assertEquals(1, count(screen, "SpPalette(tab, tool, land,"))
        assertEquals(1, count(screen, "onTool = { t -> pickTool(t) }, onMore = { moreOpen = !moreOpen }, onNext = { nextFigure() })"))
        assertEquals(1, count(screen, "SpMoreSheet(tab, tool, land,"))
        // ... and the tool last picked is only ever what a slot gave, the tool that is on, or Select
        assertEquals(10, count(screen, "toolSet = "), "pickTool, the phase's move, a face, a step, Start over, and the five placings")
        // A TOOL PLACES ONE ITEM AND GOES OFF (port 63 slice I1b): a mark on the photo and on the 360, a chain, a Tape or a Line, an area
        assertEquals(5, count(screen, "toolSet = SurveyPrepPalette.afterPlaced(tool)"))
        assertEquals(1, count(screen, "toolSet = SurveyPrepPalette.next(tool, t)")); assertEquals(1, count(screen, "toolSet = tool;"))
        assertEquals(3, count(screen, "toolSet = SurveyPrepPalette.SEL"))
        // a new chain's point is added in one place, behind the tool: 2 - Mark's branch and the 360's pick order
        for (rel in apps) assertEquals(if (rel == app + "SurveyPrepScreen.kt") 1 else 0, count(code(rel), "SurveyChain.press("), rel)
        assertEquals(1, count(screen, "val chainOn = mode == "), "2 - Mark: only with the Chain's key on")
        assertTrue(Regex("""val chainOn = mode == "[^"]*" && tool0 == SurveyPrepPalette\.CHAIN""").containsMatchIn(screen))
        assertEquals(1, count(p3, "val order = Pano360.order(tool0, drafting)"))
        for (rel in apps) assertEquals(if (rel == app + "SurveyPrepScreen.kt") 1 else 0, count(code(rel), "prep.addChain("), "$rel plots a chain")
        // ENTRY 3: the panel's acts (the long-press menu's rows until port 63 slice I2b) and the act both ask the copy
        // rule, with no answer of their own: drawn and asked through MarkSheet, which hands the menu's rule the switch
        // (port 63 slice Q2 moved this pin: both calls name `shape = true`; the chain switch they hand on is untouched)
        assertEquals(1, count(screen, "MarkSheet.panel(tx, prep, pn, quoteStage, asPicked, shape = true)")); assertEquals(1, count(screen, "MarkSheet.actStep(tx, prep, pn, act, quoteStage, asPicked, shape = true)"))
        assertEquals(1, count(screen, "MarkSheet.panel(")); assertEquals(1, count(screen, "MarkSheet.actStep("))
        assertEquals(0, count(screen, "menuItemsOf("))
        val dup = screen.indexOf("if (!PhaseTools.copyOk(prep, mid, quoteStage, asPicked)) { phaseRefuse(null); return }")
        val made = screen.indexOf("SurveyPrepEdit.duplicateChain(prep, mid, a[0], a[1], nid)")
        assertTrue(dup in 0 until made, "the copy rule is asked before the one call that copies a chain")
        for (rel in apps) assertEquals(if (rel == app + "SurveyPrepScreen.kt") 1 else 0, count(code(rel), "duplicateChain("), rel)
        // NOTHING OF THE CHAIN WAS DELETED: its model, its file's reader and writer, its two gesture branches, its
        // drawing on the photo, the 360 and the Plan, its icon, and its own tests. (ITS ROW under the photo went in port 63
        // slice I2b WITH EVERY ITEM'S ROW -- not a chain's alone: a room's chain is now named by its code chip on the
        // photo, as every picked item is, and its padlock and its Delete are the acts of the panel that chip opens.)
        val kept = mapOf(
            pano + "SurveyChain.kt" to listOf("object SurveyChain", "fun press(d: Draft?,", "fun finish(d: Draft): Finish", "fun vals(c: SurveyPrep.Chain,", "fun nodeMove(",
                "fun duplicate(p: SurveyPrep.Prep,", "fun chipText(tx: Tx, points: Int)", "fun openKeys(c: SurveyPrep.Chain,"),
            pano + "SurveyPrep.kt" to listOf("class Chain(val id: String,", "fun addChain(s: String,", "fun chainById(id: String)"),
            pano + "SurveyPrepFile.kt" to listOf("chains.add(SurveyPrep.Chain(id, code,", "if (sf.chains.isNotEmpty() || sf.chainsKey)"),
            pano + "SurveyPrepPalette.kt" to listOf("val TOOLS: List<String> = listOf(TAPE, LINE, CHAIN)", "CHAIN -> tx.common_chain"),
            pano + "Pano360.kt" to listOf("if (chain) { out.add(Step.CHAIN); return out }", "Step.CHAIN -> return Pick.Chain"),
            pano + "MeasureWalk.kt" to listOf("chains"),
            app + "SurveyPrepScreen.kt" to listOf("fun finishChain(d: SurveyChain.Draft)", "fun flushChain()", "fun chainPress(mm: DoubleArray, tol: Double): Int?",
                "fun nodeGrab(cid: String, i: Int): SpGrab?", "fun hitChain(sx: Float, sy: Float): String?", "pickChain = { s, cid ->",
                "else if (chainTool) SurveyChain.chipText(tx, chainN)"),
            app + "SurveyPrepDraw.kt" to listOf("private fun drawChains(c: Canvas,", "if (o.mode != ",
                "val pk = if (pm == null) sf.chains.firstOrNull { it.id == o.selId && it.nodes.isNotEmpty() } else null", "chipInk = if (pk != null) SurveyChain.COLOUR"),
            app + "SurveyPrepTools.kt" to listOf("private fun DrawScope.drawChainIcon(c: Color)", "SurveyPrepPalette.CHAIN -> drawChainIcon(col)"),
            app + "SurveyPrep360.kt" to listOf("Pano360.Pick.Chain -> chainOn = true", "is Pano360.Pick.Node -> {", "fun chainAt(x: Double, y: Double): Int?"),
        )
        for ((rel, has) in kept) { val c = code(rel); for (dcl in has) assertTrue(dcl in c, "$rel still has `$dcl`") }
        assertTrue(I18n.android.resolve(tests + "ChainTest.kt").isFile, "the chain's own tests still run")
        // a chain a room already has still has its panel -- the head, Lock, Lock all and Delete -- and no Duplicate while the switch is off
        val room = SurveyPrep.fresh(3319.0, 2678.0, 2677.0)
        room.addChain("front", "x", listOf(doubleArrayOf(300.0, 600.0), doubleArrayOf(900.0, 600.0)), listOf(null, null), "k1")
        assertTrue(MarkSheet.panelOpens(room, "k1"))
        assertEquals(listOf(SurveyPrepEdit.LOCK, SurveyPrepEdit.LOCK_ALL, SurveyPrepEdit.DELETE), MarkSheet.acts(Tx.EN, room, "k1", 0, null).all.map { it.id })
        // every text of the chain is still keyed, for the chains a room already has and for the day the switch goes on
        for (tx in listOf(Tx.EN, Tx.MR)) {
            assertTrue(tx.common_chain.isNotBlank()); assertTrue(SurveyChain.chipText(tx, 2).isNotBlank()); assertTrue(SurveyChain.doneLabel(tx).isNotBlank())
        }
        assertEquals("Chain", Tx.EN.common_chain)
    }

    // ---- (6) the Tape tool's switch (port 63 slice F3a) -----------------------------------------------------

    /** A front wall a surveyor already laid a Tape on -- and a door, a Line he read, and an area. */
    private fun roomWithTape(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        p.surface("front").marks.add(SurveyPrep.Mark("d1", "D", "FD1", 300.0, 1200.0, 0.0, 2100.0))
        p.addMeas("front", SurveyMeas.TAPE, listOf(pt(200.0, 1000.0), pt(1434.0, 1000.0)), "t1")
        p.addMeas("front", SurveyMeas.LINE, listOf(pt(500.0, 1600.0), pt(1300.0, 2200.0)), "m1")
        p.addMeas("front", SurveyMeas.AREA, listOf(pt(1800.0, 300.0), pt(2400.0, 300.0), pt(2400.0, 800.0)), "m2")
        p.actual["F-M1.len"] = 1240.0
        return p
    }

    private val toPx: (Double, Double) -> DoubleArray = { x, y -> doubleArrayOf(x / 8, 340 - y / 8) }
    private val textW: (String, Double, Boolean) -> Double = { t, fs, _ -> t.length * fs * 0.6 }

    @Test fun `the tape tool is off - no slot offers it, the tool that is on is never it, and a tape is never copied`() {
        // Amit, 23 Nov 2026, ~22:00 IST, with build 0.3.350 on his phone: "does tape tool make any sense now? as we don't
        // want any foto measured dimensions on fone."
        val tapes: Boolean = Features.TAPES
        assertFalse(tapes, "Features.TAPES is on: the palette offers the Tape again and a new Tape can be laid")
        // ENTRY 1, THE PALETTE: its default IS the switch; off, no slot's key is the Tape's on any kind of surface
        assertEquals(SurveyPrepPalette.offered(tapes = Features.TAPES), SurveyPrepPalette.offered())
        for (kind in listOf("wall", "floor", "ceil")) for (ch in listOf(true, false)) {
            assertFalse(SurveyPrepPalette.TAPE in SurveyPrepPalette.row(kind, SurveyPrepPalette.offered(chains = ch, tapes = false)).map { it.key }, kind)
            assertTrue(SurveyPrepPalette.TAPE in SurveyPrepPalette.row(kind, SurveyPrepPalette.offered(chains = ch, tapes = true)).map { it.key }, "$kind: the other side is whole")
            // the Line stays, whichever way either switch stands: a length the surveyor wants is a Line
            assertTrue(SurveyPrepPalette.LINE in SurveyPrepPalette.row(kind, SurveyPrepPalette.offered(chains = ch, tapes = false)).map { it.key }, kind)
        }
        for (kind in listOf("wall", "floor", "ceil")) assertFalse(SurveyPrepPalette.TAPE in SurveyPrepPalette.row(kind).map { it.key }, "$kind, as the screen asks")
        // ... and More lists mark types only, on every surface: the Tape was never there, and is not now
        for (s in SurveyPrep.SURFACES) assertTrue(SurveyPrepPalette.moreRows(Tx.EN, s).flatMap { it.tiles }.none { it.letter == SurveyPrepPalette.TAPE }, s)
        // ENTRY 2, THE TOOL THAT IS ON: whatever was picked, at every stage and unread
        for (st in listOf(0, 1, 2, 3, 4, 5, null)) {
            assertEquals(SurveyPrepPalette.SEL, PhaseTools.toolFor(SurveyPrepPalette.TAPE, st, tapes = false), "at $st")
            assertEquals(PhaseTools.toolFor(SurveyPrepPalette.TAPE, st, tapes = Features.TAPES), PhaseTools.toolFor(SurveyPrepPalette.TAPE, st))
        }
        assertEquals(SurveyPrepPalette.TAPE, PhaseTools.toolFor(SurveyPrepPalette.TAPE, 0, tapes = true), "on, Survey lays tapes as before")
        // ... so the 360's press never reaches the pull's own step with the Tape's key, 2 - Mark's never starts a drag, no
        // guide is drawn for one, and a press on the end of a Tape that is there starts no new Tape from it
        val room = roomWithTape(); val t1 = room.measById("t1")!!.second
        for (st in listOf(0, 1, 2, 3, 4, 5, null)) {
            val tool = PhaseTools.toolFor(SurveyPrepPalette.TAPE, st)
            for (drafting in listOf(false, true)) assertFalse(Pano360.Step.MEAS in Pano360.order(tool, drafting), "at $st")
            assertFalse(SurveyPrepPalette.isMeasTool(tool)); assertNull(SurveyMeas.chipText(Tx.EN, tool, null, 0))
            assertNull(SurveyGuide.now(null, null, null, null, tool, null), "at $st: no guide with nothing being drawn")
            val scr = t1.pts.map { toPx(it[0], it[1]) }
            assertFalse(SurveyGuide.endPress(true, tool, t1, false, scr, scr[1][0], scr[1][1], 28.0) is SurveyGuide.EndPress.StartNew, "at $st: an end is grabbed, never a new Tape's start")
        }
        assertTrue(Pano360.Step.MEAS in Pano360.order(PhaseTools.toolFor(SurveyPrepPalette.TAPE, 0, tapes = true), false), "on, the 360 pulls one as before")
        // ENTRY 3, DUPLICATE ON A TAPE: a copy would be a new Tape -- a Line and an area are copied as before
        for (st in listOf(0, 1, 2, 3, 4, 5, null)) for (picked in listOf(null, "exist", "wish", "vadd")) {
            assertFalse(PhaseTools.copyOk(room, "t1", st, picked, tapes = false), "at $st, $picked")
            assertEquals(PhaseTools.copyOk(room, "t1", st, picked, tapes = Features.TAPES), PhaseTools.copyOk(room, "t1", st, picked))
            assertFalse(SurveyPrepEdit.DUPLICATE in SurveyPrepEdit.menuItemsOf(Tx.EN, room, "t1", st, picked).map { it.first }, "the menu at $st, $picked")
            assertFalse(MarkSheet.acts(Tx.EN, room, "t1", st, picked).has(SurveyPrepEdit.DUPLICATE), "the panel at $st, $picked")
            assertTrue(MarkSheet.actStep(Tx.EN, room, MarkSheet.openPanel(room, "t1", 0L)!!, SurveyPrepEdit.DUPLICATE, st, picked) is MarkSheet.ActStep.Refused, "the act at $st, $picked")
            for (id in listOf("m1", "m2", "d1")) for (on in listOf(true, false))
                assertEquals(PhaseTools.copyOk(room, id, st, picked, tapes = true), PhaseTools.copyOk(room, id, st, picked, tapes = on), "$id at $st: the switch moves the Tape alone")
        }
        assertTrue(PhaseTools.copyOk(room, "t1", 0, null, tapes = true), "on, a Tape is copied wherever it may be edited")
        assertTrue(SurveyPrepEdit.DUPLICATE in SurveyPrepEdit.menuItemsOf(Tx.EN, room, "t1", 0, null, tapes = true).map { it.first })
        assertTrue(PhaseTools.copyOk(room, "m1", 0, null) && PhaseTools.copyOk(room, "m2", 0, null), "a Line and an area are copied")
        // NO HELP LINE NAMES IT among the tours that would ship -- none does while the tours are off -- in either language
        for (tx in listOf(Tx.EN, Tx.MR)) for (f in TourFlows.shipped(tx, tours = true)) for (st in f.steps)
            for (word in listOf("Tape", "tape", tx.common_tape))
                assertFalse(word in st.title || word in st.body || word in f.name || word in f.line, "${f.id} ${st.id} names $word")
    }

    @Test fun `with the tape tool off nothing measured from the photo is drawn on a tape - no figure, no graduation - on the screen's painters`() {
        val p = roomWithTape(); val t1 = p.measById("t1")!!.second
        val len = SurveyMeas.plen(t1.pts)
        assertEquals(1234.0, len)
        // ITS FIGURE: nothing, picked or not, held or not; on, the number alone, as it was
        assertEquals("", PhotoFigures.tape(t1, tapes = false)); assertEquals("1234", PhotoFigures.tape(t1, tapes = true))
        assertEquals(PhotoFigures.tape(t1, tapes = Features.TAPES), PhotoFigures.tape(t1))
        assertEquals("", SurveyMeas.tapeFigure(len, tapes = false)); assertEquals("1234", SurveyMeas.tapeFigure(len, tapes = true))
        assertEquals(SurveyMeas.tapeFigure(len, tapes = Features.TAPES), SurveyMeas.tapeFigure(len))
        // ... one being pulled has none either (unreachable while the tool is off; the rule does not depend on that)
        assertEquals("", SurveyMeas.tapeFigure(640.0, tapes = false))
        // THE FIGURES' LAYOUT IS NOT HANDED IT: with the switch off no plate of the face is the Tape's
        for (pick in listOf(Pick.NONE, Pick.State("t1", false, null), Pick.State("t1", true, null))) {
            val off = PhotoFigures.items(p, "front", W, H, pick, toPx, 1.0, textW, tapes = false)
            val on = PhotoFigures.items(p, "front", W, H, pick, toPx, 1.0, textW, tapes = true)
            assertTrue(off.none { it.id == "t1" }, "$pick"); assertEquals(listOf("1234"), on.filter { it.id == "t1" }.flatMap { m -> m.figs.map { it.text } }, "$pick")
            // the switch moves the Tape's figure alone: the Line's reading is there on both sides
            assertEquals(on.filter { it.id != "t1" }.map { m -> m.id to m.figs.map { it.text } }, off.map { m -> m.id to m.figs.map { it.text } }, "$pick")
            assertTrue(off.any { it.id == "m1" && it.figs.any { f -> f.text == "1240" } }, "$pick: a Line's reading is a measured figure")
            assertEquals(off.map { it.id }, PhotoFigures.items(p, "front", W, H, pick, toPx, 1.0, textW).map { it.id }, "the default IS the switch")
            val lay = PhotoFigures.layout(Tx.EN, p, "front", W, H, pick, toPx, 1.0, 1.0, textW)
            assertTrue(lay.none { it.id == "t1" }, "$pick: the layout the painter asks")
            assertTrue(PhotoFigures.layout(Tx.EN, p, "front", W, H, pick, toPx, 1.0, 1.0, textW, tapes = true).any { it.id == "t1" })
        }
        // ITS GRADUATIONS: none on the screen's own painters; a saved picture's old painter keeps them, whichever way the
        // switch stands (slice M3's); on, every painter's are tapeTicks, as before
        assertTrue(SurveyMeas.ticksOf(len, screen = true, tapes = false).isEmpty())
        for (l in listOf(90.0, 500.0, 1234.0, 3310.0)) {
            val all = SurveyMeas.tapeTicks(l).map { Triple(it.at, it.major, it.figure) }
            assertEquals(all, SurveyMeas.ticksOf(l, screen = true, tapes = true).map { Triple(it.at, it.major, it.figure) }, "$l on the screen, on")
            for (on in listOf(true, false)) assertEquals(all, SurveyMeas.ticksOf(l, screen = false, tapes = on).map { Triple(it.at, it.major, it.figure) }, "$l in a saved picture, $on")
            assertTrue(SurveyMeas.ticksOf(l, screen = true, tapes = false).isEmpty(), "$l on the screen, off")
            assertEquals(SurveyMeas.ticksOf(l, screen = true, tapes = Features.TAPES).size, SurveyMeas.ticksOf(l, screen = true).size, "the default IS the switch")
        }
        assertTrue(SurveyMeas.tapeTicks(1234.0).any { it.figure }, "what goes: `500` and `1000` beside the long ticks are the photo's figures too")
        // WHAT IS LEFT OF IT ON THE PHOTO: its band -- the one half-width the drawing and the pick share -- and, picked,
        // its code on its chip, where every picked item's is
        assertEquals(12.5, SurveyMeas.bandHalfMm(1.0, 2.0)); assertEquals(0xfacc15, SurveyMeas.TAPE_COLOUR)
        assertEquals("F-T1", PhotoFigures.nameOf(p, Pick.State("t1", false, null))); assertEquals("F-T1 ▾", SurveyPrepTouch.chipText("F-T1"))
        // no word its panel says carries a figure
        val pv = MarkSheet.panel(Tx.EN, p, MarkSheet.openPanel(p, "t1", 0L)!!, 0, null)!!
        for (t in listOf(pv.head.code, pv.head.name, pv.pane) + pv.acts.all.flatMap { listOf(it.text, it.aria) }) {
            assertFalse("1234" in t, t); assertFalse("mm" in t, t)
        }
        assertEquals("F-T1 tape", SurveyPrepEdit.labelOf(Tx.EN, p, "t1"), "what its delete says: its code and what it is")
        // A SAVED PICTURE'S TAG IS NOT THIS SWITCH'S: the old painter still prints the photo's length, until slice M3
        assertEquals("F-T1 1234 mm", SurveyMeas.tapeTag(t1.code, len))
    }

    @Test fun `a room that already has a tape keeps it - read and written unchanged, picked, moved, opened in its panel, locked and deleted`() {
        val p = roomWithTape(); val z = ab("front")
        val t = p.measById("t1")!!.second
        // THE FILE: it is written, read back and written again byte for byte -- nothing of it is dropped on the way
        val o = SurveyPrepFile.encode(p, L, W, H)
        val d = SurveyPrepFile.decode(o)!!
        assertTrue(d.problems.isEmpty())
        val t2 = d.prep.measById("t1")!!.second
        assertEquals("F-T1", t2.code); assertEquals(SurveyMeas.TAPE, t2.kind); assertEquals(t.pts.map { it.toList() }, t2.pts.map { it.toList() })
        assertEquals(o.toString(), SurveyPrepFile.encode(d.prep, L, W, H).toString())
        // IT IS AN ITEM OF THE FACE LIKE ANY OTHER: listed, labelled; it asks no reading and is in no count
        assertEquals(listOf("FD1", "F-T1", "F-M1", "F-M2"), p.itemsOf("front").map { it.code })
        assertEquals("F-T1 tape", SurveyPrepEdit.labelOf(Tx.EN, p, "t1")); assertNull(SurveyMeas.key(t))
        assertTrue(MeasureWalk.stopsOf(p, "t1") { ab(it) }.isEmpty())
        // PICKED: a tap on its band (screen px = mm here), 12 dp off its middle and its half-width more
        val shapes = p.surface("front").meas.map { it to it.pts }
        assertEquals("t1", SurveyMeas.pick(shapes, 800.0, 1020.0, 12.0) { m -> if (m.kind == SurveyMeas.TAPE) 12.5 else 0.0 })
        val sc = Pick.Scene(p, 0, StepRoute.MARK) { ab(it) }
        val held = Pick.tap(sc, Pick.NONE, null, "t1").state
        assertEquals(Pick.State("t1", true, null), held, "picked and held; nothing gold -- a Tape asks no reading")
        // MOVED: by its band, whole (slice F1's rule), and by one end
        val start = t.pts.map { it.copyOf() }
        assertTrue(SurveyGuide.bodyPress(true, SurveyPrepPalette.SEL, t, false, t.pts, 800.0, 1005.0, 28.0, 24.0, 12.5) is SurveyGuide.BodyPress.Grab)
        assertTrue(SurveyGuide.bodyMove(t, start, 100.0, 50.0, z[0], z[1]))
        assertEquals(listOf(listOf(300.0, 1050.0), listOf(1534.0, 1050.0)), t.pts.map { it.toList() })
        val end = assertNotNull(SurveyGuide.endMove("front", t, 1, pt(1600.0, 1050.0), z[0], z[1], emptyList(), 0.0), "an end still answers a drag")
        assertEquals(listOf(1600.0, 1050.0), end.p.toList()); assertEquals("h", end.snap)
        t.pts[1] = end.p
        // ... and under Fine move's pad, a millimetre a press
        val pad = assertNotNull(NudgePad.start(p, "t1", 0, null, null, 696.0, 1028.0, 2.0))
        assertTrue(NudgePad.press(p, pad, NudgePad.Key.UP, z[0], z[1]) {}.moved); assertEquals(listOf(300.0, 1051.0), t.pts[0].toList())
        // OPENED IN ITS PANEL: the head, and Lock, Fine move, Lock all and Delete -- everything but Duplicate
        assertTrue(MarkSheet.panelOpens(p, "t1"))
        val pv = MarkSheet.panel(Tx.EN, p, MarkSheet.openPanel(p, "t1", 0L)!!, 0, null)!!
        assertEquals(listOf("F-T1", "Tape"), listOf(pv.head.code, pv.head.name))
        assertEquals(listOf(SurveyPrepEdit.LOCK, MarkSheet.FINE, SurveyPrepEdit.LOCK_ALL, SurveyPrepEdit.DELETE), pv.acts.all.map { it.id })
        assertEquals("Lock|Lock all tapes|Cancel|Delete", SurveyPrepEdit.menuItemsOf(Tx.EN, p, "t1", 0, null).joinToString("|") { it.second })
        assertTrue(PhaseTools.editable(p, "t1", 0) && PhaseTools.lockOk(p, "t1", 0) && PhaseTools.delOk(p, "t1", 0) && MarkSheet.fineOk(p, "t1", 0))
        // LOCKED: it does not move and is not deleted; unlocked again it is as it was
        assertEquals(true, SurveyPrepEdit.setLocked(p, "t1", true)); assertFalse(p.deleteMark("t1"), "locked, it is not deleted")
        assertFalse(SurveyGuide.bodyMove(t, t.pts.map { it.copyOf() }, 10.0, 0.0, z[0], z[1]))
        assertEquals(false, SurveyPrepEdit.setLocked(p, "t1", false))
        // DELETED: it goes, and nothing else; one undo brings it back
        val snap = p.copy()
        assertTrue(p.deleteMark("t1"))
        assertNull(p.measById("t1")); assertEquals(listOf("FD1", "F-M1", "F-M2"), p.itemsOf("front").map { it.code }); assertEquals(1240.0, p.actual["F-M1.len"])
        val back = SurveyPrepEdit.undoTo(p, snap)
        assertEquals(listOf("FD1", "F-T1", "F-M1", "F-M2"), back.itemsOf("front").map { it.code })
        // AND THE MODEL STILL MAKES ONE: nothing of the Tape was taken out of the room's own code -- only no screen asks for it
        val made = back.addMeas("front", SurveyMeas.TAPE, listOf(pt(100.0, 100.0), pt(600.0, 100.0)), "t9")
        assertEquals("F-T2", made.code)
    }

    @Test fun `every entry of the tape tool reads the one constant, the app overrides none, and nothing of the tape was deleted`() {
        val tapes = "Features.TAPES"
        val all = LiteralScan.ROOTS.flatMap { sources(it) }
        // THE INVENTORY: each source that reads the switch, and how many times -- all in :pano, each the default of a
        // pure rule's last parameter. A new reader is added HERE, with its reason.
        val found = all.associateWith { count(code(it), tapes) }.filterValues { it > 0 }
        assertEquals(mapOf(
            pano + "SurveyPrepPalette.kt" to 1,   // offered: what the palette draws
            pano + "PhaseTools.kt" to 2,          // toolFor: the tool that is on; copyOk: Duplicate on a Tape
            pano + "SurveyPrepEdit.kt" to 1,      // menuItemsOf: the menu's Duplicate row, through copyOk
            pano + "MarkSheet.kt" to 3,           // acts, panel, actStep: the panel's acts, through menuItemsOf
            pano + "SurveyMeas.kt" to 2,          // ticksOf: its graduations on the screen's painters; tapeFigure: its length there
            pano + "PhotoFigures.kt" to 3,        // tape: its figure; items and layout: what the figures' layout is handed
        ).toSortedMap(), found.toSortedMap(), "the sources that read Features.TAPES, and how often")
        val declared = all.filter { Regex("""\bTAPES\b\s*(?::\s*Boolean\s*)?=[^=]""").containsMatchIn(code(it)) }
        assertEquals(listOf(pano + "Features.kt"), declared, "TAPES is given a value in one place")
        assertEquals(1, count(code(pano + "PhaseTools.kt"), "!tapes && p.measById(id)?.second?.kind == SurveyMeas.TAPE -> false"))
        assertEquals(1, count(code(pano + "PhaseTools.kt"), "(tool == SurveyPrepPalette.TAPE && !tapes)"))
        assertEquals(1, count(code(pano + "SurveyMeas.kt"), "fun ticksOf(len: Double, screen: Boolean, tapes: Boolean = Features.TAPES): List<Tick> ="))
        assertEquals(1, count(code(pano + "SurveyMeas.kt"), "fun tapeFigure(len: Double, tapes: Boolean = Features.TAPES): String = if (tapes) tapeShown(len) else "))
        assertEquals(1, count(code(pano + "PhotoFigures.kt"), "fun tape(m: SurveyPrep.Meas, tapes: Boolean = Features.TAPES): String ="))
        assertEquals(1, count(code(pano + "PhotoFigures.kt"), "DimLayout.item(m.id, null, tape(m, tapes), px(m.pts[0]), px(m.pts[1]), pick.picked == m.id, false, false, dp, textW)"))
        assertEquals(1, count(code(pano + "PhotoFigures.kt"), "} + items(prep, s, a, b, pick, toPx, dp, textW, ready, ask, tapes)"))
        // :app NEVER OVERRIDES IT: no app source names the switch or hands a rule its own answer
        val apps = all.filter { it.startsWith("app/") || it.startsWith("camera/") }
        assertTrue(apps.size > 40, "the app's sources were walked (${apps.size})")
        for (rel in apps) {
            assertEquals(0, count(code(rel), "tapes ="), "$rel hands a rule the answer")
            assertEquals(0, count(code(rel), tapes), "$rel reads the switch by itself: every gate is a pure rule's default")
        }
        val screen = code(app + "SurveyPrepScreen.kt"); val tools = code(app + "SurveyPrepTools.kt"); val p3 = code(app + "SurveyPrep360.kt")
        val draw = code(app + "SurveyPrepDraw.kt")
        // ENTRIES 1 AND 2 are the chain's own lines, pinned by its test above: the palette draws the row the rule gives
        // (`SurveyPrepPalette.row(kind)`), every slot weighed alike, and ONE tool is on, the rule's answer
        // (`PhaseTools.toolFor(toolSet, quoteStage)`) -- so a Tape is pulled in one place on each view, behind that tool
        assertEquals(1, count(screen, "val d = SurveyMeas.Draft(tab, if (tool0 == SurveyPrepPalette.TAPE) SurveyMeas.TAPE else SurveyMeas.LINE, m0, ab0[0], ab0[1])"))
        assertEquals(1, count(p3, "val dr = SurveyMeas.Draft(s, if (tool0 == SurveyPrepPalette.TAPE) SurveyMeas.TAPE else SurveyMeas.LINE, m0, ab0[0], ab0[1])"))
        for (rel in apps) assertEquals(if (rel == app + "SurveyPrepScreen.kt" || rel == app + "SurveyPrep360.kt") 1 else 0, count(code(rel), "SurveyMeas.Draft("), "$rel pulls a Tape or a Line")
        assertEquals(1, count(screen, "val measOn = mode == "), "2 - Mark: only with the Tape's or the Line's key on")
        assertTrue(Regex("""val measOn = mode == "[^"]*" && SurveyPrepPalette\.isMeasTool\(tool0\)""").containsMatchIn(screen))
        // ENTRY 3: the copy rule is asked before the one call that copies a Tape, a Line or an area
        val dup = screen.indexOf("if (!PhaseTools.copyOk(prep, mid, quoteStage, asPicked)) { phaseRefuse(null); return }")
        val made = screen.indexOf("SurveyPrepEdit.duplicateMeas(prep, mid, a[0], a[1], nid)")
        assertTrue(dup in 0 until made, "the copy rule is asked before the one call that copies a Tape")
        for (rel in apps) assertEquals(if (rel == app + "SurveyPrepScreen.kt") 1 else 0, count(code(rel), "duplicateMeas("), rel)
        // ITS FIGURE ON THE PHOTO is the layout's, which the painter asks with no answer of its own (the default: the switch)
        assertEquals(1, count(draw, "PhotoFigures.layout(tx, o.prep, s, o.a, o.b, pk, toPx, du, dp, textW, o.live, o.ready, free = o.free, ask = o.ask)"))
        for (rel in apps) { assertEquals(0, count(code(rel), "PhotoFigures.tape("), rel); assertEquals(0, count(code(rel), "PhotoFigures.items("), rel) }
        // WHAT THE TWO PAINTERS STILL ASK BY THEMSELVES -- F3b's WORK LIST (this slice edits no painter: :app is compiled
        // only by CI, and the switch-over is one commit). Until then a Tape a room already has is drawn on the photo with
        // its graduations and no length, and on the 360 with both. F3b turns each `tapeTicks(len)` into
        // `ticksOf(len, screen = …)` and each `tapeShown(len)` into `tapeFigure(len)`, and moves these four pins with it.
        assertEquals(1, count(draw, "for (tk in SurveyMeas.tapeTicks(len)) {"), "the photo's painter and a saved picture's: F3b asks ticksOf(len, screen = quiet)")
        assertEquals(1, count(draw, "val txt = if (quiet) SurveyMeas.tapeShown(len) else SurveyMeas.tapeTag(if (draft) null else code, len)"), "a Tape being pulled on the photo: F3b asks tapeFigure(len)")
        assertEquals(1, count(p3, "for (tk in SurveyMeas.tapeTicks(len)) {"), "the 360's painter: F3b asks ticksOf(len, screen = true)")
        assertEquals(1, count(p3, "val txt = SurveyMeas.tapeShown(len)"), "the 360's tag: F3b asks tapeFigure(len)")
        for (rel in apps) { assertEquals(0, count(code(rel), "ticksOf("), "$rel: not switched over yet"); assertEquals(0, count(code(rel), "tapeFigure("), "$rel: not switched over yet") }
        // NOTHING OF THE TAPE WAS DELETED: its model, its file's reader and writer, its drag, its graduations and its
        // tags, its drawing on the photo and the 360, its icon, its hint, and its own tests
        val kept = mapOf(
            pano + "SurveyMeas.kt" to listOf("const val TAPE = ", "class Draft(val s: String, val kind: String,", "fun tapeTicks(len: Double): List<Tick> {", "fun tapeTag(code: String?, len: Double)",
                "fun tapeShown(len: Double): String", "fun tapeTagAt(pa: DoubleArray, pb: DoubleArray,", "fun bandHalfMm(ppm: Double, dpPx: Double): Double", "fun tapeChip(tx: Tx)", "fun tapeTap(tx: Tx)"),
            pano + "SurveyPrep.kt" to listOf("class Meas(val id: String,", "fun addMeas(s: String,", "fun measById(id: String)"),
            pano + "SurveyPrepFile.kt" to listOf("val need = when (kind) { SurveyMeas.TAPE, SurveyMeas.LINE -> 2; SurveyMeas.AREA -> 3; else -> 1 }"),
            pano + "SurveyPrepPalette.kt" to listOf("val TOOLS: List<String> = listOf(TAPE, LINE, CHAIN)", "TAPE -> tx.common_tape", "fun isMeasTool(tool: String) = tool == TAPE || tool == LINE"),
            pano + "SurveyPrepEdit.kt" to listOf("fun duplicateMeas(p: SurveyPrep.Prep, id: String,", "g.kind == KIND_TAPE -> tx.sp_group_tapes"),
            pano + "Pano360.kt" to listOf("fun tapeHalfMm(v: View,", "fun tapeScale(v: View,", "if (meas) { out.add(Step.MEAS); return out }"),
            app + "SurveyPrepDraw.kt" to listOf("private fun tape(c: Canvas, a: DoubleArray, b: DoubleArray, code: String?,", "o.drag?.takeIf { it.s == s && it.kind == SurveyMeas.TAPE }?.let { d ->"),
            app + "SurveyPrep360.kt" to listOf("private fun tape(c: android.graphics.Canvas, v: Pano360.View,", "st.drag?.takeIf { it.s == s && it.kind == SurveyMeas.TAPE }?.let { d ->"),
            app + "SurveyPrepTools.kt" to listOf("private fun DrawScope.drawTapeIcon(c: Color)", "SurveyPrepPalette.TAPE -> drawTapeIcon(col)"),
            app + "SurveyPrepScreen.kt" to listOf("onTapeTap = { say(SurveyMeas.tapeTap(tx)) },", "if (tool0 == SurveyPrepPalette.TAPE) { onTapeTapS(); return }"),
        )
        for ((rel, has) in kept) { val c = code(rel); for (dcl in has) assertTrue(dcl in c, "$rel still has `$dcl`") }
        assertTrue(I18n.android.resolve(tests + "MeasTest.kt").isFile, "the Tape's own tests still run")
        // every text of the Tape is still keyed, for the Tapes a room already has and for the day the switch goes on
        for (tx in listOf(Tx.EN, Tx.MR)) {
            assertTrue(tx.common_tape.isNotBlank()); assertTrue(SurveyMeas.tapeChip(tx).isNotBlank()); assertTrue(SurveyMeas.tapeTap(tx).isNotBlank())
            assertTrue(tx.sp_tape_suffix.isNotBlank()); assertTrue(tx.sp_group_tapes.isNotBlank())
        }
        assertEquals("Tape", Tx.EN.common_tape)
    }
}
