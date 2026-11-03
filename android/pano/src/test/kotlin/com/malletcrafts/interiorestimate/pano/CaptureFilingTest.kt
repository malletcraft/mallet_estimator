package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Port 61c slice FW1, "photos are read by work": the room screen reads every capture's filing through
 * [Album.workOfCapture] and groups by it -- works high to low, then the Quote journey, then not filed to a
 * work (the prototype's room(), index61c2.html; SPEC61c §4.5). The ORDER is Album's and is pinned here as
 * pure code. The screen that applies it is :app, which only CI compiles, so its wiring is read as source
 * (AsIsPathTest's way): the one place the cards are built files them through the one rule, the grouping
 * and the chips take Album's order, and nothing passes the screen a phase list or a stage order any more.
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
        assertTrue(src.contains("work = Album.workOfCapture(storedWork, newStage ?: cap.workStage),"), "a re-filed stage moves only a row the phone never filed by work")
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
        // what the ERP leg and the capture sheet still read, until FW2
        for (kept in listOf("fun stages(", "fun phaseOfStage(", "fun isWorkStage("))
            assertTrue(src.contains(kept), "`$kept` stays")
    }
}
