package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.malletcrafts.interiorestimate.pano.i18n.Tx

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
        assertEquals("Started over — 3 locked kept", SurveyPrepEdit.startedOver(Tx.EN, r.kept))
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
        assertEquals(0, r.kept); assertNull(SurveyPrepEdit.startedOver(Tx.EN, 0))
        assertTrue(SurveyPrep.SURFACES.all { r.prep.surface(it).marks.isEmpty() && !r.prep.surface(it).done() })
        assertTrue(r.prep.actual.isEmpty())
        assertEquals("Tap again to clear every line, mark and reading", SurveyPrepEdit.startOverArmed(Tx.EN))
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

    // ------------------------------------------------------------------ port 62 slice L2: Start over keeps a wish

    private fun life(p: SurveyPrep.Prep, id: String, vararg keys: Pair<String, Any?>) { val e = p.markById(id)!!.second; e.extra = e.extra + keys }

    @Test fun `a mark past Existing survives Start over, locked or not, where it was and with its readings -- and the note names it`() {
        val p = worked()
        // FW1 (unlocked) is a surveyed window the client wants replaced; CB1 (unlocked) a wish since Dropped; FD1 (locked) is in Scope
        life(p, "fw1", "life" to "wish", "lifeAs" to "client", "act" to "replace", "note" to "install UPVC window")
        life(p, "cb1", "life" to "drop", "why" to "not needed")
        life(p, "fd1", "life" to "scope")
        // and a Note and an Existing mark, which go as before
        p.surface("front").marks.add(SurveyPrep.Mark("n", "NOTE", "FNOTE1", 1000.0, 1050.0, 1000.0, 1050.0, mapOf("life" to "wish", "note" to "a Note has no state")))
        p.addMarkAt("front", "S", 1300.0, 1100.0, W, H, "fs1"); p.actual["FS1.w"] = 150.0
        val wish = p.markById("fw1")!!.second
        val r = SurveyPrepEdit.startOver(p, L, W, H); val q = r.prep
        assertEquals(3, r.kept, "locked: the right wall's lines, FD1 and CF1"); assertEquals(2, r.past, "unlocked marks past Existing: FW1 and CB1")
        assertEquals(listOf("FD1", "FW1"), q.surface("front").marks.map { it.code }, "in the surface's own order")
        assertEquals(listOf("CF1", "CB1"), q.surface("ceil").marks.map { it.code })
        // the wish is the mark that was there: its box, its state, its act and its note, and still unlocked
        val kept = q.markById("fw1")!!.second
        assertEquals(listOf(wish.x0, wish.x1, wish.y0, wish.y1), listOf(kept.x0, kept.x1, kept.y0, kept.y1)); assertEquals(wish.extra, kept.extra)
        assertFalse(SurveyPrep.isLocked(kept)); assertEquals("wish", MarkLife.lifeOf(kept)); assertEquals("drop", MarkLife.lifeOf(q.markById("cb1")!!.second))
        // their readings with them, and nothing else
        assertEquals(mapOf("FD1.w" to 905.0, "FW1.w" to 1200.0, "CF1.cl" to 1300.0, "CB1.w" to 230.0), q.actual.toMap())
        // the note names the marks past Existing -- the locked ones are not counted in that line, as the prototype's says only the one
        assertEquals("Started over — 2 marks past Existing kept — drop them one by one", SurveyPrepEdit.startedOver(Tx.EN, r))
        assertEquals("पुन्हा सुरुवात — \"सध्याचे\" च्या पुढे गेलेले 2 मार्क ठेवले — ते एकेक करून वगळा", SurveyPrepEdit.startedOver(Tx.MR, r))
        // the codes that went are not handed out again, and a kept wish's is still its own
        assertEquals("FW2", q.nextCode("front", "W")); assertEquals("FS2", q.nextCode("front", "S")); assertEquals("FNOTE2", q.nextCode("front", "NOTE"))
        assertEquals("CB2", q.nextCode("ceil", "B"))
        // the prep it was given is untouched, and a kept wish is a copy
        kept.x0 = 0.0; assertEquals(wish.x0, p.markById("fw1")!!.second.x0); assertTrue(wish.x0 > 0.0)
    }

    @Test fun `with no mark past Existing kept, Start over says what v52 said -- a locked Scope mark is a locked item`() {
        val v = SurveyPrepEdit.startOver(worked(), L, W, H)
        assertEquals(0, v.past); assertEquals("Started over — 3 locked kept", SurveyPrepEdit.startedOver(Tx.EN, v))
        assertEquals(SurveyPrepEdit.startedOver(Tx.EN, v.kept), SurveyPrepEdit.startedOver(Tx.EN, v))
        val p = worked(); life(p, "fd1", "life" to "scope")
        val s = SurveyPrepEdit.startOver(p, L, W, H)
        assertEquals(3, s.kept); assertEquals(0, s.past); assertEquals("Started over — 3 locked kept", SurveyPrepEdit.startedOver(Tx.EN, s))
        val n = worked()
        SurveyPrepEdit.setLocked(n, "fd1", false); SurveyPrepEdit.setLocked(n, "cf1", false); SurveyPrepEdit.setLinesLocked(n, "right", false)
        assertNull(SurveyPrepEdit.startedOver(Tx.EN, SurveyPrepEdit.startOver(n, L, W, H)))
        // one mark, singular
        life(n, "fw1", "life" to "wish")
        assertEquals("Started over — 1 mark past Existing kept — drop them one by one", SurveyPrepEdit.startedOver(Tx.EN, SurveyPrepEdit.startOver(n, L, W, H)))
    }
}
