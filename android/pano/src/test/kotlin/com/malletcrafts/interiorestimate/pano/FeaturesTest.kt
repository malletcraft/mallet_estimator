package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Album.WorkState
import com.malletcrafts.interiorestimate.pano.QuoteStage.Cache
import com.malletcrafts.interiorestimate.pano.QuoteStage.LineKind
import com.malletcrafts.interiorestimate.pano.QuoteStage.ProjectLine
import com.malletcrafts.interiorestimate.pano.QuoteStage.Shot
import com.malletcrafts.interiorestimate.pano.i18n.I18n
import com.malletcrafts.interiorestimate.pano.i18n.LiteralScan
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * SITE PROGRESS BEHIND ONE FLAG, OFF, NOTHING DELETED (port 63 slice SP0, 16 Nov 2026). Amit, 15 Nov 2026, after his
 * first use of the app on a client site: "Lets focus on survey to quote approval workflow first. Site progress will do
 * post I confirm that survey to quote looks good. Do not delete any site progress code but toggle it as feature flag."
 *
 * Three things are held here. (1) The switch itself ([Features.SITE_PROGRESS] off, [Features.HELP_TOURS] on).
 * (2) What each pure rule answers with site progress OFF -- every call states the switch, so these pass whichever way
 * the constant stands; the same rules with it ON are held where they always were (QuoteStageTest, CaptureFilingTest,
 * PhaseToolsTest, the Album tests), each stating `siteProgress = true`. (3) :app read as source, since only CI
 * compiles it: every gate reads the ONE constant, the list of gates is complete, and every site-progress file and
 * its wiring is still there.
 *
 * TO TURN SITE PROGRESS BACK ON: `SITE_PROGRESS = true` in Features.kt, and the first test and the declaration's pin
 * below follow it. Nothing else moves.
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

    @Test fun `site progress is off, and the help tours are still on`() {
        val siteProgress: Boolean = Features.SITE_PROGRESS
        assertFalse(siteProgress, "Features.SITE_PROGRESS is on: everything after Client approved shows again (Amit, 15 Nov 2026: site progress " +
            "waits until he confirms that survey to quote looks good)")
        val tours: Boolean = Features.HELP_TOURS
        assertTrue(tours, "Features.HELP_TOURS is off: the survey tour goes with the build that ships the new measuring screen (slice M2), not before")
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
        // before approval the row is what it always was, on or off: the lock's words
        assertEquals("Survey · locked until the client approves the estimate", Album.workSub(Tx.EN, false, 0, "Survey", null, siteProgress = false))
        for (w in listOf(0, 20, null)) for (tx in listOf(Tx.EN, Tx.MR))
            assertEquals(Album.workSub(tx, false, w, "Survey", null, siteProgress = true), Album.workSub(tx, false, w, "Survey", null, siteProgress = false), "$w")
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

    @Test fun `one constant is the switch - declared once, off, with the tours' switch beside it`() {
        val features = code(pano + "Features.kt")
        assertEquals(1, count(features, "const val SITE_PROGRESS: Boolean = false"), "the switch is a constant, and it is off")
        assertEquals(1, count(features, "const val HELP_TOURS: Boolean = true"), "the tours' switch has its home, and is on")
        assertEquals(2, count(features, "const val "), "two switches and nothing else")
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
            // the five pure rules: the default of each one's last parameter
            pano + "QuoteStage.kt" to 3,     // projectLine (the execution line), roomDot (execution's dot), bandLine (survey prep's band)
            pano + "Album.kt" to 2,          // execution (filing by work begins), workSub (the WORK row's and line's words)
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
        assertEquals(3, count(stage, default)); assertEquals(2, count(album, default))
        for ((src, fn) in listOf(stage to "projectLine", stage to "roomDot", stage to "bandLine", album to "execution", album to "workSub"))
            assertTrue(Regex("""fun $fn\((?:[^()]|\([^()]*\))*siteProgress: Boolean = Features\.SITE_PROGRESS,?\s*\)""").containsMatchIn(src), "$fn's last parameter is the switch")
        assertTrue("if (!siteProgress) {" in stage, "projectLine turns to the approved state's words")
        assertTrue("if (approved && siteProgress) captures.any" in stage, "roomDot keeps the survey's rule")
        assertTrue("line.text.takeIf { siteProgress && line.kind == LineKind.EXEC }" in stage, "bandLine hands the band no line")
        assertTrue("siteProgress && approved(quoteStage)" in album, "execution is approval AND the switch")
        assertTrue("!siteProgress -> lead" in album, "workSub stops at where the capture is filed")

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
}
