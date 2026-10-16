package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * START OVER KEEPS WHAT IS LOCKED (v52 slice 8, sp.js 1683-1690; v50): a locked surface's four lines, every
 * locked item and its readings survive; every other line, mark and reading starts fresh. The synthetic room:
 * L 3319, W 2678, H 2677.
 */
class StartOverTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0

    private fun worked(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        // every line moved and set by hand, so "fresh" is told apart
        for (s in SurveyPrep.SURFACES) {
            val sf = p.surface(s)
            for (k in SurveyPrep.SLOTS) { sf.lines[k] = sf.lines.getValue(k).map { doubleArrayOf(it[0] + 7, it[1] + 3) }.toTypedArray(); sf.set[k] = true }
        }
        p.addMarkAt("front", "D", 600.0, 1050.0, W, H, "fd1")
        p.addMarkAt("front", "W", 1800.0, 1500.0, W, H, "fw1")
        p.addMarkAt("ceil", "F", 1300.0, 1600.0, W, L, "cf1")
        p.addMarkAt("ceil", "B", 1300.0, 1600.0, W, L, "cb1")
        for ((k, v) in listOf("FD1.w" to 905.0, "FW1.w" to 1200.0, "CF1.cl" to 1300.0, "CB1.w" to 230.0)) p.actual[k] = v
        SurveyPrepEdit.setLocked(p, "fd1", true); SurveyPrepEdit.setLocked(p, "cf1", true)
        SurveyPrepEdit.setLinesLocked(p, "right", true)
        return p
    }

    private fun lines(sf: SurveyPrep.Surface) = SurveyPrep.SLOTS.map { k -> sf.lines.getValue(k).map { it.toList() } }

    @Test fun `locked lines and locked items with their readings survive, the rest is fresh`() {
        val p = worked(); val f = SurveyPrep.fresh(L, W, H)
        val r = SurveyPrepEdit.startOver(p, L, W, H)
        val q = r.prep
        assertEquals(3, r.kept, "the right wall's lines, FD1 and CF1")
        assertEquals("Started over — 3 locked kept", SurveyPrepEdit.startedOver(r.kept))
        // the right wall's locked lines: kept where they were, still set and still locked
        assertEquals(lines(p.surface("right")), lines(q.surface("right")))
        assertTrue(q.surface("right").done()); assertTrue(SurveyPrep.linesLocked(q.surface("right")))
        // every other surface's lines: fresh, none set
        for (s in SurveyPrep.SURFACES.filter { it != "right" }) {
            assertEquals(lines(f.surface(s)), lines(q.surface(s)), s)
            assertFalse(q.surface(s).set.values.any { it }, s); assertFalse(SurveyPrep.linesLocked(q.surface(s)), s)
        }
        // the locked marks stay, locked, where they were; the others are gone
        assertEquals(listOf("FD1"), q.surface("front").marks.map { it.code })
        assertEquals(listOf("CF1"), q.surface("ceil").marks.map { it.code })
        assertTrue(q.surface("front").marks.all { SurveyPrep.isLocked(it) })
        val a = p.markById("fd1")!!.second; val b = q.markById("fd1")!!.second
        assertEquals(listOf(a.x0, a.x1, a.y0, a.y1), listOf(b.x0, b.x1, b.y0, b.y1))
        // and their readings with them; nothing else
        assertEquals(mapOf("FD1.w" to 905.0, "CF1.cl" to 1300.0), q.actual.toMap())
    }

    @Test fun `nothing locked -- everything fresh, and nothing to say`() {
        val p = worked()
        SurveyPrepEdit.setLocked(p, "fd1", false); SurveyPrepEdit.setLocked(p, "cf1", false); SurveyPrepEdit.setLinesLocked(p, "right", false)
        val r = SurveyPrepEdit.startOver(p, L, W, H)
        assertEquals(0, r.kept); assertNull(SurveyPrepEdit.startedOver(0))
        assertTrue(SurveyPrep.SURFACES.all { r.prep.surface(it).marks.isEmpty() && !r.prep.surface(it).done() })
        assertTrue(r.prep.actual.isEmpty())
        assertEquals("Tap again to clear every line, mark and reading", SurveyPrepEdit.START_OVER_ARMED)
    }

    @Test fun `Start over leaves the prep it was given alone, and does not hand out a code again`() {
        val p = worked(); val before = SurveyPrepFile.encode(p, L, W, H).toString()
        val q = SurveyPrepEdit.startOver(p, L, W, H).prep
        assertEquals(before, SurveyPrepFile.encode(p, L, W, H).toString())
        assertEquals("FW2", q.nextCode("front", "W"), "FW1 went, and its number with it")
        assertEquals("FD2", q.nextCode("front", "D"))
        assertEquals("CB2", q.nextCode("ceil", "B"))
        // a kept mark is a copy: moving it on the new prep leaves the old one
        q.markById("fd1")!!.second.x0 = 0.0
        assertEquals(150.0, p.markById("fd1")!!.second.x0)
    }

    @Test fun `the file's keys this build does not read are kept, and a kept surface's own keys go with the lines`() {
        val o = SurveyPrepFile.encode(worked(), L, W, H)
        o["note"] = mapOf("by" to "later")
        @Suppress("UNCHECKED_CAST") val w = LinkedHashMap(o["w"] as Map<String, Any?>).apply { put("wall360", listOf(1, 2)) }
        o["w"] = w
        val p = SurveyPrepFile.decode(o)!!.prep
        val q = SurveyPrepEdit.startOver(p, L, W, H).prep
        assertEquals(setOf("note"), q.extra.keys); assertEquals(setOf("wall360"), q.extraSurfaces.keys)
        assertEquals(setOf(SurveyRef.KEY), SurveyPrepEdit.START_OVER_KEEPS, "the reference rectangle (v52 slice 12)")
    }
}
