package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The demo flat (port 61c §31): the bundled seed parses as a prep — six surfaces bounded,
 * the prototype's marks present — and the never-sync rule covers every seeded row and
 * nothing else. The :pano-side queue logic (AlbumQueue) refuses demo keys here; the one-line
 * :app guards (AsIsUpload.enqueue, WishSync.push, AlbumOutbox.add, ReadingsSync, the
 * QuoteStageSync mover) are read in review, and CI compiles them.
 */
class TourDemoTest {

    @Suppress("UNCHECKED_CAST")
    private fun decoded(): SurveyPrepFile.Decoded {
        val o = MiniJson.parse(TourDemo.prepJson()) as Map<String, Any?>
        return assertNotNull(SurveyPrepFile.decode(o), "the seed is a prep")
    }

    @Test fun `the seed parses whole - six surfaces bounded, at the demo room's laser size`() {
        val d = decoded()
        assertTrue(d.problems.isEmpty(), d.problems.map { it(com.malletcrafts.interiorestimate.pano.i18n.Tx.EN) }.toString())
        assertEquals(TourDemo.SIZE_MM.toList(), assertNotNull(d.size).toList())
        for (s in SurveyPrep.SURFACES) assertTrue(d.prep.surface(s).done(), "$s is bounded")
    }

    @Test fun `the prototype's marks are present, with their readings`() {
        val d = decoded()
        val codes = SurveyPrep.SURFACES.flatMap { s -> d.prep.surface(s).marks.map { it.code } }
        assertEquals(
            listOf("FW1", "FN1", "FN2", "FE1", "RE1", "BD1", "BE1", "BL1", "LN1", "GS1", "CE1"),
            codes)
        assertTrue(d.prep.actual.isNotEmpty(), "the readings travelled")
        assertTrue("FW1.l" in d.prep.actual.keys, "FW1's laser reading is among them")
    }

    @Test fun `isDemo covers every seeded row and nothing else`() {
        assertTrue(TourDemo.isDemoCapture(TourDemo.CAPTURE))
        assertTrue(TourDemo.isDemoCapture("DEMO-anything-else"), "the DEMO- grammar is the rule, not one id")
        assertFalse(TourDemo.isDemoCapture("MCAP-61753b40f4f7"))
        assertFalse(TourDemo.isDemoCapture("MEST-PH-2026-00042"))
        assertFalse(TourDemo.isDemoCapture(null))

        assertTrue(TourDemo.isDemoProject(TourDemo.CLIENT, TourDemo.PROJECT))
        assertTrue(TourDemo.isDemoProject(" demo client ", "DEMO FLAT INTERIORS"), "trimmed, ignoring case, as the catalogue compares")
        assertFalse(TourDemo.isDemoProject(TourDemo.CLIENT, "Real flat interiors"))
        assertFalse(TourDemo.isDemoProject(null, TourDemo.PROJECT))

        assertTrue(TourDemo.isDemoKey(TourDemo.wishKey()))
        assertTrue(TourDemo.isDemoKey(TourDemo.CAPTURE))
        assertTrue(TourDemo.isDemoKey(TourDemo.PROJECT))
        assertFalse(TourDemo.isDemoKey("PROJ-0001"))
        assertFalse(TourDemo.isDemoKey(""))
        assertFalse(TourDemo.isDemoKey(null))
    }

    @Test fun `the demo's wish key is its local key - it has no ERP id and never will`() {
        val k = TourDemo.wishKey()
        assertEquals(WishList.keyOf("", TourDemo.CLIENT, TourDemo.PROJECT), k)
        assertTrue(WishList.isLocal(k))
    }

    @Test fun `AlbumQueue refuses demo keys - a demo op builds no request, a real one does`() {
        val op = Album.stateOp(null, 3, "Not started", "In progress", "")
        assertNull(AlbumQueue.requestOf(op, TourDemo.PROJECT))
        assertNull(AlbumQueue.requestOf(op, TourDemo.wishKey()))
        assertNotNull(AlbumQueue.requestOf(op, "PROJ-0001"))
    }
}
