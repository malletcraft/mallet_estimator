package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Port 61c slice FW1, "photos are read by work": the room screen reads every capture's filing through
 * [Album.workOfCapture] and groups by it -- works high to low, then the Quote journey, then not filed to a
 * work (the prototype's room(), index61c2.html; SPEC61c §4.5). The ORDER is Album's and is pinned here as
 * pure code. The screen that applies it is :app, which only CI compiles, so its wiring is read as source
 * (AsIsPathTest's way): the one place the cards are built files them through the one rule, the grouping
 * and the chips take Album's order, and nothing passes the screen a phase list or a stage order any more.
 *
 * Port 61c slice FW2, "new photos and re-files are written by work; the last stage pickers go": a NEW capture's
 * filing ([Album.newCaptureWork]), the WORK row's and line's two texts ([Album.filingHead], [Album.quoteLead],
 * [Album.workSub]) and the sweep's rule ([Album.needsRefile]) are pure and pinned here, in English and in Marathi;
 * the :app wiring -- the capture sheet's WORK row and Which work?, the photo's WORK line and its re-file through
 * CaptureStore.setWork and the WorkRefile job, the sweep in SyncWorker after both legs, and the deleted stage
 * pickers -- is read as source the same way.
 *
 * The FW2 review (3 Nov 2026) adds the photo line's own lead ([Album.photoQuoteLead]: Survey for a project only on
 * this phone, the prototype's photo line) and the honest stage ([Album.quoteTakenAt]: null, `not read yet`, where
 * the phone's inputs cannot support one), both pure, and reads as source that a refused re-file leaves the sweep's
 * mark alone and is shown on survey prep's web line.
 */
class CaptureFilingTest {

    @Test fun `filingRank - a work by its id, the quote journey under every work, not filed to a work last`() {
        assertEquals(-2, Album.filingRank(null))
        assertEquals(-1, Album.filingRank(Album.QUOTE_WORK))
        assertEquals(10, Album.filingRank(10))
        assertEquals(140, Album.filingRank(140))
        assertTrue(Album.filingRank(Album.QUOTE_WORK) > Album.filingRank(null))
        assertTrue(Album.WORK_IDS.all { Album.filingRank(it) > Album.filingRank(Album.QUOTE_WORK) })
    }

    @Test fun `filingOrder - the filings present, once each - works high to low, then the quote journey, then unfiled`() {
        assertEquals(listOf(140, 20, 0, null), Album.filingOrder(listOf(null, 0, 20, 140, 20, null, 0)))
        assertEquals(listOf(90, 20, 0, null), Album.filingOrder(listOf(0, 20, 90, null)))
        // the plan's example (DESIGN-port61c §15): an old 'Plaster & waterproofing' photo under Civil &
        // masonry, the survey's 360s under the Quote journey, a delivery photo not filed to a work -- each
        // filed through the one rule first, then ordered
        val filings = listOf(
            null to "Plaster & waterproofing", null to "As-built site measure", 90 to "",
            null to "Material delivered on site", null to "", 0 to "Handover",
        ).map { (w, st) -> Album.workOfCapture(w, st) }
        assertEquals(listOf(90, 20, 0, null), Album.filingOrder(filings))
        // one filing present: one group (the screen then draws no chip row)
        assertEquals(listOf(0), Album.filingOrder(listOf(0, 0, 0)))
        assertEquals(listOf<Int?>(null), Album.filingOrder(listOf(null)))
        assertEquals(emptyList(), Album.filingOrder(emptyList()))
        // every work: S5's order reversed, the newest work at the top, whatever order the captures came in
        assertEquals(Album.WORK_IDS.reversed(), Album.filingOrder(Album.WORK_IDS.shuffled(Random(7))))
    }

    @Test fun `a group's heading is the filing's name - the Quote journey, the work, not filed to a work`() {
        assertEquals("Quote journey", Album.workName(Tx.EN, Album.QUOTE_WORK))
        assertEquals("not filed to a work", Album.workName(Tx.EN, null))
        assertEquals("Civil & masonry", Album.workName(Tx.EN, 20))
        assertEquals("Cleaning & handover", Album.workName(Tx.EN, 140))
        // hand-typed Marathi pins (SurveyPrepMarathiTest's rule: a Masters lookup wired to the wrong map
        // prints the same English under Tx.EN and leaks Latin only under मराठी)
        assertEquals("कोटेशनचा प्रवास", Album.workName(Tx.MR, Album.QUOTE_WORK))
        assertEquals("कोणत्याही कामात ठेवलेला नाही", Album.workName(Tx.MR, null))
    }

    // ---- :app, read as source (only CI compiles :app) ----------------------------------------------------

    private fun app(name: String): String {
        val f = File("../app/src/main/kotlin/com/malletcrafts/interiorestimate/$name")
        assertTrue(f.isFile, "the app's $name is beside :pano (${f.absolutePath})")
        return f.readText()
    }

    @Test fun `the room screen groups by the filing, through Album, and takes no phases and no stage order (CaptureScreens kt)`() {
        val src = app("CaptureScreens.kt")
        // FW1 review: no default on the filing -- null is itself a filing (not filed to a work), so a
        // site that omitted it would compile and file every card there; the compiler must refuse it
        assertTrue(src.contains("val work: Int?,"), "CaptureCard carries its filing, and every site states it")
        assertFalse(src.contains("val work: Int? = null"), "the filing has no default: an omitted work is a compile error, not a quiet mis-filing")
        // FW1 review: the prototype's `cols:S.land?4:2` -- four tiles across in landscape, two in portrait
        assertTrue(src.contains("val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE"), "the room grid reads the orientation")
        assertTrue(src.contains("columns = GridCells.Fixed(if (landscape) 4 else 2),"), "four tiles across at 780 x 360, two at 360 x 780")
        assertFalse(src.contains("GridCells.Fixed(2)"), "no fixed two-column grid is left")
        assertTrue(src.contains("val present = Album.filingOrder(captures.map { it.work })"), "the chips are the filings present, in Album's order")
        assertTrue(src.contains(".sortedByDescending { (work, _) -> Album.filingRank(work) }"), "the groups take Album's order")
        assertTrue(src.contains("Text(Album.workName(tx, work).uppercase(),"), "a heading is the filing's name, upper-cased as the prototype's CSS draws it")
        assertTrue(src.contains("label = { Text(Album.workName(tx, w)) }"), "a chip is the filing's name")
        assertTrue(src.contains("label = { Text(tx.app_all_x(captures.size)) }"), "the All chip is keyed")
        assertTrue(src.contains("Text(tx.app_no_captures_room_yet,"), "the empty line is keyed")
        assertTrue(src.contains("tx.app_x_over_x(plN = photos, pl2N = days) else tx.pl_photo(photos)"), "n photos (over n days)")
        for (gone in listOf("phases", "stageOrder", "StageHeading", "\"No stage\"", "it.workStage.ifBlank"))
            assertFalse(src.contains(gone), "`$gone` has left the room screen")
        // the signature: captures, onDetails, onOpen and nothing else
        val sig = src.substring(src.indexOf("fun CapturesScreen("), src.indexOf(") {", src.indexOf("fun CapturesScreen(")))
        assertEquals(listOf("captures", "onDetails", "onOpen"), Regex("""\n\s+(\w+):""").findAll(sig).map { it.groupValues[1] }.toList())
    }

    @Test fun `the cards are filed through the one rule, and the title and the viewer sub-line drop the stage (MainActivity kt)`() {
        val src = app("MainActivity.kt")
        assertTrue(src.contains("work = Album.workOfCapture(c.work, c.stage),"), "roomCaptures files each card through workOfCapture")
        assertTrue(src.contains("title = \"\${cap.date} · \${Album.workName(tx, cap.work)}\","), "the capture title is `date · where filed`")
        assertFalse(src.contains("\${cap.stage}"), "no reader shows the capture's stage")
        assertFalse(src.contains("\"no stage\""), "no stand-in stage word")
        val call = src.substring(src.indexOf("CapturesScreen("), src.indexOf("onDetails", src.indexOf("CapturesScreen(")))
        assertFalse(call.contains("phases") || call.contains("stageOrder"), "the CapturesScreen call passes no phases and no stage order")
        // FW1 review: the chip pick is the room's (the prototype's `S.capWork=null` on the room crumb's
        // onSibling, index61c2.html:8158) -- the screen is keyed on the room, so its remembered pick is
        // rebuilt on a sibling switch instead of filtering the next room's grid to nothing
        assertTrue(src.contains("import androidx.compose.runtime.key"), "key is imported")
        assertTrue(Regex("""key\(roomName\) \{\s*CapturesScreen\(""").containsMatchIn(src), "the CapturesScreen call is keyed on the room")
    }

    @Test fun `phases, stageOrder and canRestage are gone with their last callers (Catalogue kt)`() {
        val src = app("Catalogue.kt")
        for (gone in listOf("fun phases(", "fun stageOrder(", "fun canRestage(", "can_restage"))
            assertFalse(src.contains(gone), "`$gone` is gone")
        // what the ERP leg still reads: the thirty-nine as the vocabulary of an older row's stage
        for (kept in listOf("fun stages(masters: JSONObject?): List<Stage>", "fun phaseOfStage(", "fun isWorkStage("))
            assertTrue(src.contains(kept), "`$kept` stays")
        // FW2: the job-type narrowing fed the two stage pickers and went with them
        assertFalse(src.contains("jobType: String? = null"), "stages() no longer narrows to a job type: nothing picks a stage")
    }

    // ---- FW2: a new capture's filing, the WORK row and line, the sweep's rule ------------------------------

    private val inProgress = mapOf(20 to Album.WorkState(Album.IN_PROGRESS, 3), 90 to Album.WorkState(Album.IN_PROGRESS, 5),
        10 to Album.WorkState(Album.DONE, 2))

    @Test fun `newCaptureWork - the Quote journey before approval, this project's pick after it, else the first In progress, else none`() {
        // before Client approved every new capture is the Quote journey's, whatever is picked or In progress
        assertEquals(0, Album.newCaptureWork(false, null, null))
        assertEquals(0, Album.newCaptureWork(false, 90, inProgress))
        // after it: the pick held for this project
        assertEquals(90, Album.newCaptureWork(true, 90, inProgress))
        assertEquals(140, Album.newCaptureWork(true, 140, emptyMap()))
        // the default pick: the first work In progress in S5's order (20 before 90); Done is not open
        assertEquals(20, Album.newCaptureWork(true, null, inProgress))
        // no default when nothing is In progress: Pick the work, the buttons wait (the album camera's rule)
        assertNull(Album.newCaptureWork(true, null, null))
        assertNull(Album.newCaptureWork(true, null, emptyMap()))
        assertNull(Album.newCaptureWork(true, null, mapOf(10 to Album.WorkState(Album.DONE), 20 to Album.WorkState(Album.ON_HOLD))))
        // a pick that is not one of the sixteen (0, a stray id) is no pick
        assertEquals(20, Album.newCaptureWork(true, 0, inProgress))
        assertNull(Album.newCaptureWork(true, 45, emptyMap()))
    }

    private fun line(kind: QuoteStage.LineKind, quote: String?) =
        QuoteStage.ProjectLine(kind, quote, "", emptyList(), "", 0, false, "")

    @Test fun `filingHead and quoteLead - Pick the work when unfiled, the stage, not on S5 yet, not read yet`() {
        assertEquals("Pick the work", Album.filingHead(Tx.EN, null))
        assertEquals("Quote journey", Album.filingHead(Tx.EN, 0))
        assertEquals("Civil & masonry", Album.filingHead(Tx.EN, 20))
        assertEquals("काम निवडा", Album.filingHead(Tx.MR, null))
        // the capture sheet passes the line's current stage; the photo's line quoteAt's answer
        assertEquals("Client wish list", Album.quoteLead(Tx.EN, line(QuoteStage.LineKind.QUOTE, "Client wish list"), "Client wish list"))
        assertEquals("Survey", Album.quoteLead(Tx.EN, line(QuoteStage.LineKind.QUOTE, "Vendor quoted"), "Survey"))
        assertEquals("ग्राहकाची इच्छा-यादी", Album.quoteLead(Tx.MR, line(QuoteStage.LineKind.QUOTE, "Client wish list"), "Client wish list"))
        // a project only on this phone has no stage of its own (v61c.2): the prototype's NOT_ON_S5_MSG, whatever is passed
        assertEquals("not on S5 yet", Album.quoteLead(Tx.EN, line(QuoteStage.LineKind.LOCAL, null), "Survey"))
        assertEquals("अजून S5 वर नाही", Album.quoteLead(Tx.MR, line(QuoteStage.LineKind.LOCAL, null), null))
        // never read, or a name outside the six: not read yet
        assertEquals("not read yet", Album.quoteLead(Tx.EN, line(QuoteStage.LineKind.UNREAD, null), null))
        assertEquals("not read yet", Album.quoteLead(Tx.EN, line(QuoteStage.LineKind.QUOTE, "Survey"), "Approved?"))
        assertEquals("अजून वाचलेले नाही", Album.quoteLead(Tx.MR, line(QuoteStage.LineKind.UNREAD, null), null))
    }

    @Test fun `photoQuoteLead - the photo's line reads Survey for a project only on this phone, else quoteLead of the stage taken at`() {
        // the prototype's two rows differ on a never-sent project (FW2 review): the capture sheet's row reads
        // NOT_ON_S5_MSG (workRow61c, index61c2.html:7878), the photo's WORK line reads quoteAt's `Survey` for a
        // project S5 does not list (7249, captureScreen 8205) -- the phone follows each row's own words
        assertEquals("Survey", Album.photoQuoteLead(Tx.EN, line(QuoteStage.LineKind.LOCAL, null), null))
        assertEquals("Survey", Album.photoQuoteLead(Tx.EN, line(QuoteStage.LineKind.LOCAL, null), "Client wish list"))
        assertEquals("सर्व्हे", Album.photoQuoteLead(Tx.MR, line(QuoteStage.LineKind.LOCAL, null), null))
        assertEquals("not on S5 yet", Album.quoteLead(Tx.EN, line(QuoteStage.LineKind.LOCAL, null), null), "the sheet's row keeps its words")
        // a listed project: quoteLead of the stage the capture was taken at, or not read yet
        assertEquals("Client wish list", Album.photoQuoteLead(Tx.EN, line(QuoteStage.LineKind.QUOTE, "Vendor quoted"), "Client wish list"))
        assertEquals("not read yet", Album.photoQuoteLead(Tx.EN, line(QuoteStage.LineKind.QUOTE, "Vendor quoted"), null))
        assertEquals("not read yet", Album.photoQuoteLead(Tx.EN, line(QuoteStage.LineKind.UNREAD, null), null))
        assertEquals("अजून वाचलेले नाही", Album.photoQuoteLead(Tx.MR, line(QuoteStage.LineKind.EXEC, "Client approved"), null))
    }

    private fun stage(seq: Long, value: String, at: String) = Album.Entry(seq, "ALB-%012x".format(seq), Album.KIND_STAGE, null, value, at = at)

    @Test fun `quoteTakenAt - the line's stage is quoteAt's with a mirror, and null where the phone cannot say (FW2 review)`() {
        val since = "2026-10-20T06:00:00.000Z"
        val before = "2026-10-18T05:00:00.000Z"
        val after = "2026-10-25T05:00:00.000Z"
        // NAMED without its stamp (QuoteStage.named, atUnknown): the current point cannot be placed, so no answer --
        // the project line reads `<stage> · not read yet` for the same copy; quoteAt alone would print Survey
        assertNull(Album.quoteTakenAt(null, after, "Client approved", "", true))
        assertNull(Album.quoteTakenAt(emptyList(), after, "Client approved", "", true))
        assertNull(Album.quoteTakenAt(listOf(stage(1, "Estimated", since)), after, "Client approved", "", true))
        assertEquals("Survey", Album.quoteAt(emptyList(), after, "Client approved", ""), "what the line printed before the review")
        // no mirror (before Revisions none is read): the current stage at its stamp is the whole history --
        // a capture after the stamp is the current stage's, one before it is not known: null, not Survey
        assertEquals("Vendor quoted", Album.quoteTakenAt(null, after, "Vendor quoted", since, false))
        assertEquals("Vendor quoted", Album.quoteTakenAt(null, since, "Vendor quoted", since, false), "at the very moment")
        assertNull(Album.quoteTakenAt(null, before, "Vendor quoted", since, false))
        assertEquals("Survey", Album.quoteAt(emptyList(), before, "Vendor quoted", since), "the album's chapters keep the prototype's default")
        // a project at Survey with no stamp never moved (S5's default at creation): every capture is Survey's
        assertEquals("Survey", Album.quoteTakenAt(null, before, "Survey", "", false))
        assertEquals("Survey", Album.quoteTakenAt(null, after, "Survey", null, false))
        // a stage S5 lists without a time that is not Survey, or a name outside the six: not known
        assertNull(Album.quoteTakenAt(null, after, "Vendor quoted", "", false))
        assertNull(Album.quoteTakenAt(null, after, "On hold", since, false))
        assertNull(Album.quoteTakenAt(null, after, "", since, false))
        // with a mirror the answer is quoteAt's, the album's own rule over the whole log: Survey before the first
        // logged move, the logged stage after it, the current point counted
        val log = listOf(stage(1, "Client wish list", "2026-10-05T05:00:00.000Z"), stage(2, "Vendor quoted", since))
        assertEquals("Survey", Album.quoteTakenAt(log, "2026-10-01T05:00:00.000Z", "Vendor quoted", since, false))
        assertEquals("Client wish list", Album.quoteTakenAt(log, before, "Vendor quoted", since, false))
        assertEquals("Vendor quoted", Album.quoteTakenAt(log, after, "Vendor quoted", since, false))
        assertEquals("Estimated", Album.quoteTakenAt(log, "2026-10-28T05:00:00.000Z", "Estimated", "2026-10-27T05:00:00.000Z", false))
        assertEquals("Survey", Album.quoteTakenAt(emptyList(), before, "Vendor quoted", since, false), "an empty log held is still the whole log")
    }

    @Test fun `workSub - the stage, the state or not filed, then the lock before approval and tap to change after`() {
        val lock = " · locked until the client approves the estimate"
        // the capture sheet before approval (workRow61c): the Quote journey over the stage and the lock
        assertEquals("Vendor quoted$lock", Album.workSub(Tx.EN, false, 0, "Vendor quoted", null))
        assertEquals("not read yet$lock", Album.workSub(Tx.EN, false, 0, "not read yet", null))
        // the photo's line before approval: an old photo filed under a work by its stage reads the work's state
        assertEquals("Not started$lock", Album.workSub(Tx.EN, false, 20, "Survey", null))
        assertEquals("not filed to a work$lock", Album.workSub(Tx.EN, false, null, "Survey", null))
        // after approval: the state and tap to change; an unfiled one says so; a quote-journey one keeps its stage
        assertEquals("In progress · tap to change", Album.workSub(Tx.EN, true, 20, "Survey", Album.IN_PROGRESS))
        assertEquals("Not started · tap to change", Album.workSub(Tx.EN, true, 90, "Survey", null))
        assertEquals("not filed to a work · tap to change", Album.workSub(Tx.EN, true, null, "Survey", null))
        assertEquals("Survey · tap to change", Album.workSub(Tx.EN, true, 0, "Survey", null))
        // hand-typed Marathi pins (SurveyPrepMarathiTest's rule)
        assertEquals("व्हेंडरचे कोटेशन · ग्राहकाने अंदाजपत्रक मंजूर करेपर्यंत लॉक", Album.workSub(Tx.MR, false, 0, "व्हेंडरचे कोटेशन", null))
        assertEquals("चालू आहे · बदलण्यासाठी टॅप करा", Album.workSub(Tx.MR, true, 20, "", Album.IN_PROGRESS))
        assertEquals("कोणत्याही कामात ठेवलेला नाही · बदलण्यासाठी टॅप करा", Album.workSub(Tx.MR, true, null, "", null))
    }

    @Test fun `needsRefile - on web, filed off the Quote journey, and not yet told to S5 as filed now`() {
        // an upload from before FW2 told S5 nothing: S5 holds 0, the phone files by the old stage
        assertTrue(Album.needsRefile(true, 20, null, false))
        assertTrue(Album.needsRefile(true, null, null, false))
        // the Quote journey needs no re-file: S5 filed it there already
        assertFalse(Album.needsRefile(true, 0, null, false))
        // not on the web: its next record carries the work
        assertFalse(Album.needsRefile(false, 20, null, false))
        // told as it is filed now: once per capture
        assertFalse(Album.needsRefile(true, 20, 20, true))
        assertFalse(Album.needsRefile(true, null, null, true))
        // the pick moved since S5 was told: the next sync sends it
        assertTrue(Album.needsRefile(true, 90, 20, true))
        assertTrue(Album.needsRefile(true, null, 20, true))
    }

    @Test fun `the capture sheet's first row is WORK and Which work? lists the sixteen, and the stage picker is gone (TreeShell kt)`() {
        val src = app("TreeShell.kt")
        for (gone in listOf("fun StageSheet(", "onStage", "\"FILED AT\"", "\"No stage set\"", "Move the project to", "stage: String,"))
            assertFalse(src.contains(gone), "`$gone` has left the capture sheet")
        assertTrue(src.contains("overlineContent = { Text(tx.album_work_caps) }"), "the row's overline is WORK")
        assertTrue(src.contains("modifier = if (onWork != null) Modifier.clickableRow(onWork) else Modifier)"), "the row takes a tap only after approval")
        assertTrue(src.contains("enabled = !busy && cameraConnected && sizesOk && !noWork,"), "Shoot waits for a pick")
        assertTrue(src.contains("enabled = !busy && sizesOk && !noWork,"), "Pick waits for a pick")
        assertEquals(2, Regex("""enabled = !busy && !noWork,""").findAll(src).count(), "Camera and Gallery wait for a pick")
        assertTrue(src.contains("fun WorkSheet("), "Which work? exists")
        assertTrue(src.contains("SheetTitle(tx.album_which_work)"), "its title is keyed")
        assertTrue(src.contains("Album.WORKS.forEach { w ->"), "it lists the sixteen works, never the Quote journey")
        assertTrue(src.contains("supportingContent = { Text(Album.stateText(tx, states?.get(w.id)?.state)) },"), "each with its state")
    }

    @Test fun `the STG row is the WORK line, read-only before approval, and every text of the capture screen is keyed (CaptureScreens kt)`() {
        val src = app("CaptureScreens.kt")
        for (gone in listOf("\"STG\"", "onPickStage", "workStage", "val stage: String", "\"No stage set\"", "\"no phase\"", "tap to correct"))
            assertFalse(src.contains(gone), "`$gone` has left the capture screen")
        assertTrue(src.contains("lead = tx.album_work_caps,"), "the line's lead is WORK")
        assertTrue(src.contains("title = Album.filingHead(tx, capture.work),"), "its title is the filing's name, or Pick the work")
        assertTrue(src.contains("subtitle = workSub,"), "its second text is the caller's (Album.workSub)")
        assertTrue(src.contains("onClick = onPickWork)"), "a tap re-files it, when the caller allows one")
        assertTrue(src.contains("onClick: (() -> Unit)?,"), "DetailRow takes no tap before approval")
        assertTrue(src.contains(".then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)"), "and draws no tap then")
        assertTrue(src.contains("if (onClick != null) {"), "and no chevron")
        assertEquals(2, Regex("""Text\(tx\.app_filed_at,""").findAll(src).count(), "the two FILED AT folder blocks are keyed, not deleted (critic C2)")
    }

    @Test fun `a new capture carries its work and no stage, the pick is per project, and the re-file writes the store then the job (MainActivity kt)`() {
        val src = app("MainActivity.kt")
        for (gone in listOf("StageSheet", "showStagePicker", "retagStage", "stageNow", "var stage by", "Stage for new photos", "Stage of this photo",
            "setCaptureTags(serverName, workStage", "cat.stages(", "workStage = newStage"))
            assertFalse(src.contains(gone), "`$gone` is gone with the stage pickers")
        assertTrue(src.contains("val newWork = Album.newCaptureWork(fileApproved, pickKey?.let { capWorkFor[it] }, fileCache?.states)"), "where the next capture files")
        assertTrue(src.contains("val capWorkFor = remember { mutableStateMapOf<String, Int>() }"), "the pick is held per project, in memory")
        assertTrue(src.contains("if (fileApproved && workNow == null) { lastResult = { tx -> tx.album_pick_work }; return }"), "approved with no work to file at: refused, never filed to 0 (a deliberate departure from the prototype's ingest, index61c2.html:8016, which files at null; on the phone NULL is a pre-FW2 row's legacy filing)")
        assertEquals(2, Regex("""captureDate = today, stage = "",""").findAll(src).count(), "FaceWriter.begin and single get stage ''")
        assertTrue(src.contains("room = r, work = workNow, captureDate = today,"), "PendingSplit carries the work")
        assertTrue(src.contains("work = workNow))"), "the Photo insert carries the work")
        assertTrue(src.contains("work = keep.work,"), "the kept 360's insert carries the work")
        assertEquals(4, Regex("""stage = "",""").findAll(src).count(), "every new row's stage is blank: begin, single, the Photo insert, the kept insert")
        assertTrue(src.contains("onPickWork = if (capApproved) ({ refileWork = true }) else null,"), "the WORK line takes a tap after approval only")
        assertTrue(src.contains("store.setWork(cap.deviceId, work)"), "a re-file writes the store first")
        assertTrue(src.contains("if (WorkRefile.onWeb(context, cap.deviceId)) WorkRefile.enqueue(context, cap.deviceId)"), "then the job, for a capture S5 holds")
        assertTrue(src.contains("Album.quoteTakenAt(\n            capMirror?.stages, "), "a Quote-journey capture's line reads quoteTakenAt over the mirror, null when none is held")
        assertTrue(src.contains("capCache.stage, capCache.at, capCache.atUnknown)"), "with the store's current point and whether its stamp is unknown")
        assertTrue(src.contains("Album.photoQuoteLead(tx, capLine, quoteAt)"), "the photo's line takes its own lead: Survey for a project only on this phone")
        assertTrue(src.contains("Album.quoteLead(tx, fileLine, fileLine.quote)"), "the capture sheet's row keeps quoteLead: not on S5 yet for such a project")
        assertTrue(src.contains("onDismiss = { pickWork = false; showCaptureSheet = true })"), "a dismissed Which work? reopens the capture sheet unchanged")
        assertTrue(src.contains("onDismiss = { refileWork = false })"), "a dismissed re-file changes nothing")
    }

    @Test fun `the stage halves of setTags and setCaptureTags are gone, and the sweep runs after both legs (CaptureStore, FrappeClient, SyncWorker, AsIsUpload, WorkRefile kt)`() {
        assertTrue(app("CaptureStore.kt").contains("fun setTags(deviceId: String, sku: String) {"), "setTags tags a SKU alone")
        assertTrue(app("FrappeClient.kt").contains("fun setCaptureTags(name: String, sku: String): JSONObject {"), "setCaptureTags sends a SKU alone")
        val tags = app("FrappeClient.kt").let { it.substring(it.indexOf("fun setCaptureTags(")).let { f -> f.substring(0, f.indexOf("fun ", 4)) } }
        assertFalse(tags.contains("work_stage"), "no work_stage goes to the bench from a re-tag (createCapture still sends the row's, for the ERP leg)")
        val sync = app("SyncWorker.kt")
        val sweep = sync.indexOf("WorkRefile.sweep(applicationContext, store)")
        assertTrue(sweep > 0, "the sweep runs")
        assertTrue(sync.indexOf("if (erp == null || erp.keyDead) { if (s5) s5Leg(store) }") < sweep, "after both legs")
        assertTrue(sync.indexOf("AsIsUpload.requeueSignInRefused") < sweep && sweep < sync.indexOf("QuoteStageSync.refresh(applicationContext"), "beside the re-queue, before the quote stage read")
        assertFalse(sync.substring(sync.indexOf("private fun s5Leg")).contains("WorkRefile"), "never inside the S5 leg, which a phone holding a pair never runs")
        val up = app("AsIsUpload.kt")
        assertTrue(up.contains("told = AsIsUpload.Told(body[\"work\"] as Int?))"), "the record's own work is the done-mark")
        assertTrue(up.contains("val kept = told ?: runCatching { toldOf(JSONObject(f.readText())) }.getOrNull()"), "a state change keeps the mark")
        val job = app("WorkRefile.kt")
        // FW2 review: a refusal is not agreement -- the mark is left alone (the sweep asks again next sync) and
        // S5's words are kept beside it and drawn on survey prep's web line, not only in logcat
        assertEquals(2, Regex("""AsIsUpload\.markTold\(ctx, id, work\)""").findAll(job).count(), "the mark is written on a 200 and on a 404 under every name, nowhere else")
        val refused = job.substring(job.indexOf("else -> {"), job.indexOf("return Result.failure()", job.indexOf("else -> {")))
        assertFalse(refused.contains("markTold"), "a refusal writes no done-mark")
        assertTrue(refused.contains("AsIsUpload.markRefileRefused(ctx, id, why)"), "a refusal keeps S5's words")
        assertTrue(up.contains("fun markRefileRefused(context: Context, id: String, why: String)"), "beside the mark")
        assertTrue(up.contains("o.remove(\"refile\")"), "a re-file S5 takes clears them")
        assertTrue(up.contains("fun refileRefused(context: Context, id: String): String?"), "read back for the screen")
        val prep = app("SurveyPrepScreen.kt")
        assertTrue(prep.contains("tx.app_not_refiled_web_x(why = why)"), "and shown, keyed, under the web line")
        assertTrue(prep.contains("refileRefused = withContext(Dispatchers.IO) { AsIsUpload.refileRefused(context, id) }"), "read beside the state, off the main thread")
        assertTrue(job.contains("fun workName(captureId: String) = \"mcft-work-\$captureId\""), "one unique job per capture")
        assertTrue(job.contains("ExistingWorkPolicy.REPLACE"), "a later pick replaces it")
        assertTrue(job.contains("val names = listOfNotNull(c.deviceId, c.serverName?.takeIf { it.isNotBlank() && it != c.deviceId })"), "each name the capture has had: the device id, then ERP's")
        assertTrue(job.contains("a.code == 404 -> i += 1"), "the next name on a 404")
        assertTrue(job.contains("AsIsPath.path(AsIsPath.work(c.project, c.room, capture))"), "S5-R's address")
        assertTrue(job.contains("JSONObject(AsIsPath.workBody(work) as Map<*, *>)"), "S5-R's body, serialised whole so a null work is JSON null")
        assertTrue(job.contains("Album.needsRefile(onWeb(context, c.deviceId), Album.workOfCapture(c.work, c.stage), told?.work, told != null)"), "the sweep's rule is Album's")
    }
}
