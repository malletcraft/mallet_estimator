package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.PhotoFigures.Look
import com.malletcrafts.interiorestimate.pano.Pick.State
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * NO PHOTO ESTIMATE, ANYWHERE (port 63 slice I1a; DESIGN-imagemeter §18). Amit, 16 Nov 2026, shown the pictures of the
 * one-step screen: "No foto estimation at all. It confuses user". Which figures the photo draws, in what style and
 * with what text -- for a mark by rule 2, for a Line, an area and a chain by rule 3, on the 360 by the same rule --
 * and the gross guard that replaces the photo's check (rule 5). The painter is compiled only by CI, so every answer
 * it will draw is held here. The synthetic room of MeasTest: L 3319, W 2678, H 2677.
 *
 * AS AMIT'S FIVE POINTS FROM 0.3.345 AMEND IT (port 63 slice I1c, 23 Nov 2026; DESIGN-imagemeter §21): P3, "i want to
 * see all measurements in its units always visible" -- every mark shows every figure that has been READ, its size and
 * its position, picked or not, mark type by mark type; nothing for an unread one unless the mark is picked (`?`) or the
 * figure is gold. P4, "Measurements on any mark type alwyas show FM1 , 2 etc. it cosumes lot of space on line" -- a
 * figure's label is the number alone: no code, no unit. §18's guarantee stays pinned: no `≈` and no photo estimate
 * comes out of any of them.
 *
 * (Test names are plain ASCII: MeasureWalkTest's header says why.)
 */
class PhotoFiguresTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0
    private fun ab(s: String): DoubleArray = SurveyPrep.size(s, L, W, H)
    private fun pt(x: Double, y: Double) = doubleArrayOf(x, y)

    private fun SurveyPrep.Prep.put(s: String, t: String, n: Int, x0: Double, x1: Double, y0: Double, y1: Double,
                                    extra: Map<String, Any?> = emptyMap()): SurveyPrep.Mark =
        SurveyPrep.Mark("id-$s-$t$n", t, SurveyPrep.CODE.getValue(s) + t + n, x0, x1, y0, y1, extra).also { surface(s).marks.add(it) }

    private val wish = mapOf(MarkLife.KEY_BORN to MarkLife.WISH, MarkLife.KEY_LIFE to MarkLife.WISH)

    /** A door, a window, an electric point, a door born a wish, a Note; a tape, a Line, an area and a chain. */
    private fun wall(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        p.put("front", "D", 1, 300.0, 1200.0, 0.0, 2100.0)
        p.put("front", "W", 1, 1500.0, 2400.0, 900.0, 2100.0)
        p.put("front", "E", 1, 1325.0, 1375.0, 275.0, 325.0)
        p.put("front", "D", 2, 50.0, 250.0, 0.0, 2100.0, wish)
        p.put("front", MarkTypes.NOTE, 1, 100.0, 150.0, 1200.0, 1250.0)
        p.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 1000.0), pt(1234.0, 1000.0)), "t1")
        p.addMeas("front", SurveyMeas.LINE, listOf(pt(500.0, 1600.0), pt(1300.0, 2200.0)), "m1")
        p.addMeas("front", SurveyMeas.AREA, listOf(pt(1800.0, 300.0), pt(2400.0, 300.0), pt(2400.0, 800.0)), "m2")
        p.addChain("front", "x", listOf(pt(300.0, 2400.0), pt(1200.0, 2400.0), pt(2400.0, 2400.0)), listOf(null, null, null), "k1")
        return p
    }

    private val D1 = "id-front-D1"; private val W1 = "id-front-W1"; private val E1 = "id-front-E1"; private val D2 = "id-front-D2"
    private fun mark(p: SurveyPrep.Prep, id: String) = p.markById(id)!!.second
    private fun figs(p: SurveyPrep.Prep, id: String, picked: Boolean, gold: String? = null, live: String? = null, ready: Boolean = true) =
        PhotoFigures.ofMark(Tx.EN, p.actual, "front", mark(p, id), W, H, picked, gold, live, ready)
    private fun texts(fs: List<PhotoFigures.Figure>) = fs.map { it.code to it.text }

    // ------------------------------------------------------------------ rule 2: a mark's figures

    @Test fun `a wall nobody has measured is outlines and codes - no mark that is not picked shows any figure`() {
        val p = wall()
        val face = PhotoFigures.onFace(Tx.EN, p, "front", W, H, Pick.NONE)
        // every mark of the face but the Note, in drawing order, and not one figure
        assertEquals(listOf("FD1", "FW1", "FE1", "FD2"), face.map { it.first.code })
        assertTrue(face.all { it.second.isEmpty() })
    }

    @Test fun `a mark that is not picked shows every figure that has been read - its size AND where it sits - and nothing for an unread one`() {
        val p = wall()
        p.actual["FD1.w"] = 898.0
        assertEquals(listOf("FD1.w" to "898"), texts(figs(p, D1, picked = false)))
        // its height read too: both, each a reading
        p.actual["FD1.h"] = 2095.0
        figs(p, D1, false).let { assertEquals(listOf("FD1.w" to "898", "FD1.h" to "2095"), texts(it)); assertTrue(it.all { f -> f.look == Look.READ && f.measured }) }
        // P3: its position and its reveal depth read as well -- SHOWN, picked or not (0.3.345 showed them only picked)
        p.actual["FD1.l"] = 310.0; p.actual["FD1.p"] = 120.0
        assertEquals(listOf("FD1.w" to "898", "FD1.h" to "2095", "FD1.l" to "310", "FD1.p" to "⊥ 120"), texts(figs(p, D1, false)))
        // ... and the door's other position figure, never read, is not there: nothing for an unread figure, not even `?`
        assertEquals(p.keysOf("front", mark(p, D1), W, H).filter { it in p.actual }, figs(p, D1, false).map { it.code })
        // a point has no size, but it has a place: its read position figures, always
        assertTrue(figs(p, E1, false).isEmpty())
        p.actual["FE1.cr"] = 1340.0
        assertEquals(listOf("FE1.cr" to "1340"), texts(figs(p, E1, false)))
        p.actual["FE1.cs"] = 300.0
        assertEquals(listOf("FE1.cr" to "1340", "FE1.cs" to "300"), texts(figs(p, E1, false)))
        assertTrue(figs(p, E1, false).all { it.look == Look.READ })
        // a reading is rounded as a figure is, half up
        p.actual["FW1.w"] = 911.5; assertEquals("912", figs(p, W1, false).single().text)
    }

    @Test fun `mark type by mark type, on a wall, the floor and the ceiling - every read figure always, nothing unread unless picked or gold`() {
        var types = 0; var checked = 0
        for ((s, kind) in listOf("front" to MarkTypes.WALL, "floor" to MarkTypes.FLOOR, "ceil" to MarkTypes.CEIL)) {
            val a = ab(s)[0]; val b = ab(s)[1]
            val letters = (SurveyPrep.TYPES.getValue(kind).map { it.letter } + MarkTypes.typesOn(kind).map { it.letter }).distinct()
            for (t in letters) {
                types++
                val p = SurveyPrep.fresh(L, W, H)
                val pt = SurveyPrep.isPoint(t)
                val x0 = a * 0.35; val y0 = b * 0.3
                val e = SurveyPrep.Mark("id-$t", t, SurveyPrep.CODE.getValue(s) + t + 1, x0, if (pt) x0 + 50 else x0 + 700, y0, if (pt) y0 + 50 else y0 + 600)
                p.surface(s).marks.add(e)
                val keys = p.keysOf(s, e, a, b)
                fun f(picked: Boolean, gold: String? = null) = PhotoFigures.ofMark(Tx.EN, p.actual, s, e, a, b, picked, gold)
                if (!MarkDims.asked(e)) { assertTrue(f(true).isEmpty() && f(false).isEmpty(), "$s $t asks nothing"); continue }
                // nothing read: a mark that is not picked shows nothing at all; picked, every figure it asks as `?`
                assertTrue(f(false).isEmpty(), "$s $t")
                assertEquals(keys, f(true).map { it.code }, "$s $t"); assertTrue(f(true).all { it.look == Look.ASK }, "$s $t")
                // every other figure read -- size and position alike -- and an unpicked mark shows exactly those
                val read = keys.filterIndexed { i, _ -> i % 2 == 0 }
                read.forEachIndexed { i, k -> p.actual[k] = 101.0 + i }
                assertEquals(read, f(false).map { it.code }, "$s $t")
                for (fig in f(false)) { assertEquals(Look.READ, fig.look, "$s ${fig.code}"); assertTrue(fig.measured); assertEquals((101 + read.indexOf(fig.code)).toString(), fig.text.removePrefix(MarkDims.DEPTH_GLYPH)) }
                // ... picked, all of them, the unread ones the quiet `?`
                f(true).let { fs -> assertEquals(keys, fs.map { it.code }, "$s $t"); for (fig in fs) assertEquals(if (fig.code in read) Look.READ else Look.ASK, fig.look, "$s ${fig.code}") }
                // an unread figure made gold shows -- gold -- on a mark that is not picked too (the walk never leaves it so,
                // but the rule does not hang on that)
                keys.firstOrNull { it !in read }?.let { g ->
                    assertEquals((read + g).sortedBy { keys.indexOf(it) }, f(false, g).map { it.code }, "$s $t gold $g")
                    assertEquals(Look.GOLD, f(false, g).first { it.code == g }.look)
                }
                // every figure read: every one shown, picked or not, and none of them is ever an estimate
                for (k in keys) p.actual[k] = 222.0
                assertEquals(keys, f(false).map { it.code }, "$s $t"); assertEquals(keys, f(true).map { it.code }, "$s $t")
                for (fig in f(false) + f(true)) { assertFalse("≈" in fig.text); assertFalse(fig.state == MarkDims.State.ESTIMATE); checked++ }
            }
        }
        assertTrue(types >= 40, "every type of every kind of surface: $types"); assertTrue(checked > 200, "$checked")
    }

    @Test fun `a four-corner mark shows its read sides and its read position, always - its unread sides only picked`() {
        val p = SurveyPrep.fresh(L, W, H)
        val e = SurveyPrep.Mark("q", "W", "FW1", 600.0, 2400.0, 900.0, 2100.0,
            linkedMapOf(Quad.KEY to listOf(listOf(0.0, 0.5), listOf(1.0, 0.0), listOf(1.0, 1.0), listOf(0.0, 1.0))))
        p.surface("front").marks.add(e)
        assertTrue(Quad.of(e) != null)
        val keys = p.keysOf("front", e, W, H)
        assertEquals(listOf("FW1.bottom", "FW1.right", "FW1.top", "FW1.left"), keys.take(4))
        fun f(picked: Boolean) = PhotoFigures.ofMark(Tx.EN, p.actual, "front", e, W, H, picked, null)
        assertTrue(f(false).isEmpty())
        p.actual["FW1.top"] = 1800.0; p.actual["FW1.r"] = 278.0
        assertEquals(listOf("FW1.top" to "1800", "FW1.r" to "278"), texts(f(false)))
        assertEquals(keys, f(true).map { it.code })
    }

    @Test fun `the picked mark shows every dimension it asks - a read one as its figure, an unread one as the quiet question mark`() {
        val p = wall(); p.actual["FW1.w"] = 912.0
        val fs = figs(p, W1, picked = true)
        assertEquals(listOf("FW1.w" to "912", "FW1.h" to "?", "FW1.r" to "?", "FW1.s" to "?", "FW1.p" to "⊥ ?"), texts(fs))
        assertEquals(listOf(Look.READ, Look.ASK, Look.ASK, Look.ASK, Look.ASK), fs.map { it.look })
        // the same keys, in the same order, as the checklist asks: nothing is left out and nothing is invented
        assertEquals(p.keysOf("front", mark(p, W1), W, H), fs.map { it.code })
        // READ: white and bold on solid black. NOT READ: grey on half-black inside a dashed grey outline -- ONE not-read,
        // so a site-only depth is the same grey, not amber
        fs[0].style.let { assertEquals(0xffffff, it.ink); assertTrue(it.bold); assertEquals(0xdd, it.fillAlpha); assertNull(it.outline) }
        for (f in fs.drop(1)) f.style.let { assertEquals(MarkDims.ASK_INK, it.ink); assertFalse(it.bold); assertEquals(0x88, it.fillAlpha); assertEquals(MarkDims.ASK_INK, it.outline); assertTrue(it.dashed) }
        assertEquals(0xcfd3cb, MarkDims.ASK_INK); assertFalse(MarkDims.ASK_INK == MarkDims.CHECK_COLOUR)
        // a picked point: both its position figures
        assertEquals(listOf("FE1.cr" to "?", "FE1.cs" to "?"), texts(figs(p, E1, true)))
        // the state a plate is laid out by is MEASURED or UNKNOWN, never ESTIMATE
        assertEquals(setOf(MarkDims.State.MEASURED, MarkDims.State.UNKNOWN), fs.map { it.state }.toSet())
    }

    @Test fun `the one being measured is the gold plate - its glyph hollow until the laser is ready, the digits typed on it as they are typed`() {
        val p = wall()
        fun gold(live: String? = null, ready: Boolean = true) = figs(p, W1, true, "FW1.h", live, ready).first { it.code == "FW1.h" }
        gold().let { assertEquals("▶?", it.text); assertEquals(Look.GOLD, it.look); assertTrue(it.gold); assertFalse(it.measured) }
        // gold, whatever it holds: solid gold, dark bold text, a white edge (H1, kept as built)
        gold().style.let { assertEquals(MarkDims.ACTIVE_COLOUR, it.fill); assertEquals(0xff, it.fillAlpha); assertEquals(MarkDims.ACTIVE_INK, it.ink); assertTrue(it.bold); assertEquals(0xffffff, it.outline); assertFalse(it.dashed) }
        // THE LASER NOT READY: the glyph is hollow, at the figure he is looking at
        assertEquals("▷?", gold(ready = false).text)
        // A GOLD ⊥ CHIP CARRIES ITS OWN NAME WHILE IT IS GOLD: the photo cannot draw what a depth is, and no bar says it any more
        assertEquals("▶⊥ reveal depth ?", figs(p, W1, true, "FW1.p").last().text); assertEquals("▷⊥ reveal depth ?", figs(p, W1, true, "FW1.p", ready = false).last().text)
        assertEquals("▶⊥ reveal depth 110", figs(p, W1, true, "FW1.p", live = "110").last().text)
        // ... in the language, by the checklist's own key; not gold it is the quiet chip again, and no other figure is ever named
        assertEquals("▶⊥ रिव्हील डेप्थ ?", PhotoFigures.ofMark(Tx.MR, p.actual, "front", mark(p, W1), W, H, true, "FW1.p").last().text)
        assertEquals("⊥ ?", figs(p, W1, true, "FW1.h").last().text); assertEquals("▶?", figs(p, W1, true, "FW1.s").first { it.code == "FW1.s" }.text)
        // the digits are on the gold plate as they are typed: no display line under the photo
        gold(live = "110").let { assertEquals("▶110", it.text); assertTrue(it.typing); assertFalse(it.check); assertEquals(MarkDims.State.MEASURED, it.state); assertFalse(it.measured) }
        // an untouched keypad (null) and an emptied one ("") leave the plate what it was
        assertEquals("▶?", gold(live = null).text); assertEquals("▶?", gold(live = "").text)
        // over a stored reading: the reading, gold; and only the gold figure is touched by the keypad
        p.actual["FW1.h"] = 1106.0
        assertEquals("▶1106", gold().text); assertEquals("▶12", gold(live = "12").text)
        assertEquals("?", figs(p, W1, true, "FW1.h", "12").first { it.code == "FW1.r" }.text)
    }

    @Test fun `a mark that is not in the room shows no figure at all, picked or not, whatever it holds`() {
        val p = wall(); p.actual["FD2.w"] = 800.0   // a reading an earlier build left on a wished door
        assertTrue(figs(p, D2, picked = false).isEmpty()); assertTrue(figs(p, D2, picked = true).isEmpty())
        assertTrue(figs(p, D2, true, gold = "FD2.w").isEmpty())
        assertTrue(figs(p, "id-front-NOTE1", true).isEmpty())
        // it is never asked for a reading either, so nothing is hidden that could be acted on
        assertFalse(MeasureFlow.takes(p, "FD2.w", PhaseTools.SURVEY, ::ab))
    }

    @Test fun `no figure of any mark, in any state, reads an estimate`() {
        val p = wall(); p.actual["FD1.w"] = 898.0; p.actual["FW1.p"] = 110.0
        val all = ArrayList<PhotoFigures.Figure>()
        for (e in p.surface("front").marks) for (picked in listOf(false, true)) for (gold in listOf(null) + p.keysOf("front", e, W, H)) for (ready in listOf(true, false))
            all += PhotoFigures.ofMark(Tx.EN, p.actual, "front", e, W, H, picked, gold, null, ready)
        assertTrue(all.size > 100)
        for (f in all) {
            assertFalse("≈" in f.text, "${f.code}: ${f.text}"); assertFalse(f.state == MarkDims.State.ESTIMATE, f.code)
            // a figure is its reading, or the question mark -- after its glyphs
            val bare = f.text.removePrefix("▶").removePrefix("▷").removePrefix("⊥ ").let { t -> if (f.gold && f.dim.role == MarkDims.Role.DEPTH) t.substringAfterLast(' ') else t }
            assertTrue(bare == "?" || bare.all { it.isDigit() }, "${f.code}: ${f.text}")
            assertEquals(bare != "?", f.measured, f.code)
        }
    }

    // ------------------------------------------------------------------ rule 5: the gross guard

    @Test fun `the gross guard - more than twice or less than half the side as the mark is drawn, on a mark in the wall's plane`() {
        // exactly twice and exactly half pass; a hair over or under does not
        assertFalse(MarkDims.gross(1800.0, 900.0, true)); assertTrue(MarkDims.gross(1800.5, 900.0, true))
        assertFalse(MarkDims.gross(450.0, 900.0, true)); assertTrue(MarkDims.gross(449.5, 900.0, true))
        assertFalse(MarkDims.gross(898.0, 900.0, true)); assertEquals(2.0, MarkDims.GROSS_OVER); assertEquals(0.5, MarkDims.GROSS_UNDER)
        // no reading, nothing drawn, or a thing that stands off the plane: never
        assertFalse(MarkDims.gross(null, 900.0, true)); assertFalse(MarkDims.gross(2500.0, null, true)); assertFalse(MarkDims.gross(2500.0, 0.0, true)); assertFalse(MarkDims.gross(2500.0, 900.0, false))
        // IT IS NOT THE OLD RULE: a door drawn 900 and read 1100 failed "150 mm or 15 %" and passes this -- with loosely
        // drawn marks the old rule would cry wolf, and nobody now sees the estimate it compared with
        assertTrue(MarkDims.check(1100.0, 900.0, true)); assertFalse(MarkDims.gross(1100.0, 900.0, true))
    }

    @Test fun `the guard reads a mark's sides, never its position - and its flag rides the reading's plate`() {
        val p = wall(); val door = mark(p, D1)
        assertEquals(listOf(true, true, false, false, false, false, true, true, false), MarkDims.Role.values().map { MarkDims.guarded(it) })
        val dims = MarkDims.of("front", door, W, H).associateBy { it.key }
        // the door's height (2100) shot into its width (drawn 900): the wrong thing
        p.actual["FD1.w"] = 2100.0
        assertTrue(MarkDims.guard(p.actual, "front", door, dims.getValue("w"), W, H)); assertTrue(MarkDims.read(p.actual, "front", door, dims.getValue("w"), W, H).check)
        assertTrue(MeasureFlow.flagged(p, "FD1.w", ::ab))
        // its width into its height: less than half
        p.actual["FD1.h"] = 905.0; assertTrue(MeasureFlow.flagged(p, "FD1.h", ::ab))
        // a POSITION is a distance to a wall, where a mark drawn 300 from the corner that is really 90 away is no wrong shot
        p.actual["FD1.l"] = 90.0
        assertFalse(MarkDims.guard(p.actual, "front", door, dims.getValue("l"), W, H)); assertFalse(MeasureFlow.flagged(p, "FD1.l", ::ab))
        // the figure with its flag: `!` drawn after it -- and on the gold plate in the plate's own dark ink
        figs(p, D1, true, "FD1.w").first().let { assertEquals("▶2100", it.text); assertTrue(it.check) }
        assertEquals(MarkDims.ACTIVE_INK, MarkDims.checkInk(true)); assertEquals(MarkDims.CHECK_COLOUR, MarkDims.checkInk(false))
        // a wardrobe stands off the wall's plane and reads larger than it is on the squared photo: never flagged
        val unit = p.put("front", "CRP", 1, 600.0, 2400.0, 0.0, 2100.0)
        p.actual["FCRP1.w"] = 700.0
        assertFalse(MarkDims.inPlane("front", unit, W, H)); assertFalse(MeasureFlow.flagged(p, "FCRP1.w", ::ab))
        // a Line, an area, a chain and a key nobody asks are checked against nothing
        p.actual["F-M1.len"] = 5.0; for (k in listOf("F-M1.len", "F-M2.per", "FK1.1", "FZ9.w")) assertFalse(MeasureFlow.flagged(p, k, ::ab), k)
    }

    @Test fun `the check's sentence quotes no figure, and is said for the reading that carries the flag`() {
        val p = wall(); p.actual["FD1.w"] = 2100.0
        val said = MeasureFlow.guardText(Tx.EN, p, "FD1.w", ::ab)
        assertEquals("This reading does not match the mark. Shoot again, or ▸ to keep it.", said); assertEquals(said, MeasureText.checkReading(Tx.EN))
        assertFalse(said!!.any { it.isDigit() }); assertFalse("≈" in said)
        // English first: its Marathi is its English until Amit has verified the journey
        assertEquals(said, MeasureText.checkReading(Tx.MR))
        // nothing to say of a reading that passes, of an unread figure, of a Line
        p.actual["FD1.h"] = 2095.0
        for (k in listOf("FD1.h", "FD1.l", "F-M1.len", "FZ9.w")) assertNull(MeasureFlow.guardText(Tx.EN, p, k, ::ab), k)
        // the old sentence quoted the photo's estimate: it went with MeasureFlow.checkText in slice I1b, and its key with it
    }

    // ------------------------------------------------------------------ rule 3: a Line, an area, a chain

    @Test fun `a Line's figure is the number alone - shown once read, picked or not, nothing until then, a question mark only while it is picked`() {
        val p = wall(); val m1 = p.measById("m1")!!.second; val key = "F-M1.len"
        // P4: no code and no unit on the line -- "F-M1" took the line's room for nothing
        PhotoFigures.line(m1, p.actual, Pick.NONE).let { assertEquals("", it.text); assertNull(it.key); assertFalse(it.gold) }
        PhotoFigures.line(m1, p.actual, State("m1", true, null)).let { assertEquals("?", it.text); assertEquals(key, it.key) }
        PhotoFigures.line(m1, p.actual, State("m1", false, key)).let { assertEquals("▶?", it.text); assertEquals(key, it.key); assertTrue(it.gold) }
        assertEquals("▷?", PhotoFigures.line(m1, p.actual, State("m1", false, key), ready = false).text)
        p.actual[key] = 1240.0
        PhotoFigures.line(m1, p.actual, Pick.NONE).let { assertEquals("1240", it.text); assertEquals(key, it.key) }
        assertEquals("1240", PhotoFigures.line(m1, p.actual, State("m1", true, null)).text)
        assertEquals("▶1240", PhotoFigures.line(m1, p.actual, State("m1", true, key)).text)
        // NEVER the photo's length (it is 1000 mm here): not with ≈, not without; never the code, never "mm"
        for (pick in listOf(Pick.NONE, State("m1", true, null), State("m1", true, key))) PhotoFigures.line(m1, wall().actual, pick).text.let {
            assertFalse("1000" in it); assertFalse("F-M1" in it); assertFalse("mm" in it) }
        // where the code IS shown: where the picked item is named
        assertEquals("F-M1", PhotoFigures.nameOf(p, State("m1", true, key))); assertNull(PhotoFigures.nameOf(p, Pick.NONE))
        assertEquals("FW1", PhotoFigures.nameOf(p, State(W1, false, "FW1.w"))); assertEquals("FK1", PhotoFigures.nameOf(p, State("k1", true, null)))
        assertEquals("F-T1", PhotoFigures.nameOf(p, State("t1", true, null))); assertNull(PhotoFigures.nameOf(p, State("gone", true, null)))
    }

    @Test fun `an area's square metres are the photo's, so they are not shown - its perimeter is, the number alone, once read`() {
        val p = wall(); val m2 = p.measById("m2")!!.second
        assertEquals("", PhotoFigures.line(m2, p.actual, Pick.NONE).text); assertEquals("?", PhotoFigures.line(m2, p.actual, State("m2", true, null)).text)
        p.actual["F-M2.per"] = 5734.0
        assertEquals("5734", PhotoFigures.line(m2, p.actual, Pick.NONE).text)
        for (pick in listOf(Pick.NONE, State("m2", true, "F-M2.per"))) assertFalse("m²" in PhotoFigures.line(m2, p.actual, pick).text)
        // the old tag printed both, with the code: "F-M2 0.15 m² · 5734 mm"
        assertTrue("m²" in SurveyMeas.lineTag(m2.code, m2.kind, m2.pts, reading = 5734.0, approx = true))
    }

    @Test fun `a Line being pulled and an area being tapped show nothing - and the Tape keeps its own figure, the number alone`() {
        assertEquals("", PhotoFigures.DRAFT_TAG)
        assertEquals("", SurveyMeas.shownTag(null, SurveyMeas.LINE, null, ask = true)); assertEquals("", SurveyMeas.shownTag(null, SurveyMeas.AREA, 5.0, ask = true))
        // a tape has no reading key, so it has no such tag: its figure IS what the photo says (the one exception) -- now
        // without its code and its unit
        val p = wall(); val t1 = p.measById("t1")!!.second
        PhotoFigures.line(t1, p.actual, State("t1", true, null)).let { assertEquals("", it.text); assertNull(it.key) }
        assertEquals("1234", PhotoFigures.tape(t1)); assertEquals("1234", SurveyMeas.tapeShown(SurveyMeas.plen(t1.pts)))
        assertEquals("", PhotoFigures.tape(p.measById("m1")!!.second), "a Line is not a Tape")
        assertEquals("F-T1 1234 mm", SurveyMeas.tapeTag(t1.code, SurveyMeas.plen(t1.pts)), "what 0.3.345 prints, which this replaces")
        assertEquals("", SurveyMeas.shownTag("F-T1", SurveyMeas.TAPE, null, true))
        // what takes a tap: a figure or a question mark
        assertTrue(SurveyMeas.tagTaps(1240.0, false)); assertTrue(SurveyMeas.tagTaps(null, true)); assertFalse(SurveyMeas.tagTaps(null, false))
    }

    @Test fun `a chain shows read segments and a read total, the number alone - no derived figure and no photo figure`() {
        val p = wall(); val k1 = p.chainById("k1")!!.second
        fun v() = SurveyChain.vals(k1, p.actual, W, H)
        // nothing read, not picked: nothing at all -- not even its code
        assertNull(PhotoFigures.chainSeg(k1, v(), 0, Pick.NONE)); assertNull(PhotoFigures.chainSeg(k1, v(), 1, Pick.NONE))
        PhotoFigures.chainTotal(k1, v(), Pick.NONE).let { assertEquals("", it.text); assertNull(it.key) }
        // picked: a question mark on each, each a tap target; the gold one with its glyph
        val held = State("k1", true, "FK1.1")
        PhotoFigures.chainSeg(k1, v(), 0, held)!!.let { assertEquals("▶?", it.text); assertEquals("FK1.1", it.key); assertTrue(it.gold) }
        PhotoFigures.chainSeg(k1, v(), 1, held)!!.let { assertEquals("?", it.text); assertEquals("FK1.2", it.key); assertEquals(MarkDims.ASK_INK, it.colour) }
        PhotoFigures.chainTotal(k1, v(), held).let { assertEquals("Σ ?", it.text); assertEquals("FK1.t", it.key) }
        assertEquals("▶Σ ?", PhotoFigures.chainTotal(k1, v(), State("k1", false, "FK1.t")).text)
        // one segment read: its figure, white, picked or not
        p.actual["FK1.1"] = 905.0
        PhotoFigures.chainSeg(k1, v(), 0, Pick.NONE)!!.let { assertEquals("905", it.text); assertEquals(SurveyChain.MEASURED_COLOUR, it.colour); assertEquals("FK1.1", it.key) }
        // both read: the total is their SUM, a figure nobody measured -- not shown
        p.actual["FK1.2"] = 1195.0
        assertEquals(SurveyChain.DERIVED, v().src); assertEquals("", PhotoFigures.chainTotal(k1, v(), Pick.NONE).text)
        assertEquals("FK1 Σ 2100 d", SurveyChain.totalTag(Tx.EN, "FK1", v()), "what the old tag printed")
        // the total read: shown, Σ and the number -- no code
        p.actual["FK1.t"] = 2104.0
        PhotoFigures.chainTotal(k1, v(), Pick.NONE).let { assertEquals("Σ 2104", it.text); assertEquals("FK1.t", it.key) }
        // a total read and one segment left to DERIVE: the derived segment is not shown; picked, it is a question mark
        p.actual.remove("FK1.2")
        assertEquals(1, v().der); assertNull(PhotoFigures.chainSeg(k1, v(), 1, Pick.NONE)); assertEquals("?", PhotoFigures.chainSeg(k1, v(), 1, State("k1", true, null))!!.text)
        assertEquals("1199 d", SurveyChain.segFigure(v(), 1).first, "what the old figure printed")
    }

    @Test fun `a chain that runs wall to wall shows the laser's total - the room's own reading, the number alone - and it takes no tap`() {
        val p = SurveyPrep.fresh(L, W, H)
        val k = p.addChain("front", "x", listOf(pt(0.0, 1200.0), pt(905.0, 1200.0), pt(W, 1200.0)), listOf(null, null, null), "edge")
        val v = SurveyChain.vals(k, p.actual, W, H)
        assertEquals(SurveyChain.LASER, v.src)
        for (pick in listOf(Pick.NONE, State("edge", true, "FK1.1"))) PhotoFigures.chainTotal(k, v, pick).let { assertEquals("Σ 2678", it.text); assertNull(it.key) }
        assertEquals("FK1 Σ 2678 laser", SurveyChain.totalTag(Tx.EN, "FK1", v), "what 0.3.345 prints, which this replaces")
    }

    // ------------------------------------------------------------------ the 360's tags, by the same rule

    @Test fun `on the 360 a tag is a reading, the number alone, or nothing - never a question mark, never gold, never the photo's figure`() {
        val p = wall(); val m1 = p.measById("m1")!!.second; val m2 = p.measById("m2")!!.second; val k1 = p.chainById("k1")!!.second
        assertEquals("", PhotoFigures.line360(m1, p.actual)); assertEquals("", PhotoFigures.line360(m2, p.actual))
        assertEquals("", PhotoFigures.line360(p.measById("t1")!!.second, p.actual), "the tape's own figure is PhotoFigures.tape")
        assertNull(PhotoFigures.chainSeg360(SurveyChain.vals(k1, p.actual, W, H), 0)); assertEquals("", PhotoFigures.chainTotal360(SurveyChain.vals(k1, p.actual, W, H)))
        p.actual["F-M1.len"] = 1240.0; p.actual["F-M2.per"] = 5734.0; p.actual["FK1.1"] = 905.0; p.actual["FK1.t"] = 2104.0
        assertEquals("1240", PhotoFigures.line360(m1, p.actual)); assertEquals("5734", PhotoFigures.line360(m2, p.actual))
        val v = SurveyChain.vals(k1, p.actual, W, H)
        assertEquals("905", PhotoFigures.chainSeg360(v, 0)!!.first); assertNull(PhotoFigures.chainSeg360(v, 1), "the derived segment")
        assertEquals("Σ 2104", PhotoFigures.chainTotal360(v))
        // what the 360 prints today, which this replaces: the code, and the photo's length after ≈
        assertEquals("F-M1 ≈1000 mm", SurveyMeas.lineTag("F-M1", SurveyMeas.LINE, m1.pts, reading = null, approx = true))
    }

    @Test fun `no label of a Tape, Line, area or chain carries a code, a unit or an estimate, in any state`() {
        val p = wall(); p.actual["F-M1.len"] = 1240.0; p.actual["FK1.1"] = 905.0
        val labels = ArrayList<String>()
        val picks = listOf(Pick.NONE, State("m1", true, null), State("m1", false, "F-M1.len"), State("m2", true, "F-M2.per"),
            State("k1", true, "FK1.2"), State("k1", false, "FK1.t"), State("t1", true, null))
        for (pick in picks) for (ready in listOf(true, false)) {
            for (m in p.surface("front").meas) { labels += PhotoFigures.line(m, p.actual, pick, ready).text; labels += PhotoFigures.tape(m); labels += PhotoFigures.line360(m, p.actual) }
            val k1 = p.chainById("k1")!!.second; val v = SurveyChain.vals(k1, p.actual, W, H)
            for (i in 0 until k1.nodes.size - 1) PhotoFigures.chainSeg(k1, v, i, pick, ready)?.let { labels += it.text }
            labels += PhotoFigures.chainTotal(k1, v, pick, ready).text; labels += PhotoFigures.chainTotal360(v)
            labels += PhotoFigures.items(p, "front", W, H, pick, { x, y -> doubleArrayOf(x / 8, 340 - y / 8) }, 1.0, { t, fs, _ -> t.length * fs * 0.6 }, ready).flatMap { mk -> mk.figs.map { it.text } }
        }
        assertTrue(labels.count { it.isNotEmpty() } > 40, "${labels.count { it.isNotEmpty() }}")
        for (t in labels) {
            assertFalse("≈" in t, t); assertFalse("mm" in t, t); assertFalse("m²" in t, t); assertFalse("laser" in t, t)
            for (code in listOf("F-M1", "F-M2", "F-T1", "FK1")) assertFalse(code in t, t)
            val bare = t.removePrefix("▶").removePrefix("▷").removePrefix("Σ ")
            assertTrue(bare.isEmpty() || bare == "?" || bare.all { it.isDigit() }, t)
        }
    }

    // ------------------------------------------------------------------ DimLayout is handed only those

    @Test fun `the layout is handed only the figures rule 2 shows, and the held mark alone shows its tabs`() {
        val p = wall(); p.actual["FD1.w"] = 898.0
        val toPx = { x: Double, y: Double -> doubleArrayOf(x * 340 / W, 340 - y * 340 / H) }
        val textW = { t: String, fs: Double, _: Boolean -> t.length * fs * 0.6 }
        fun lay(pick: State) = PhotoFigures.layout(Tx.EN, p, "front", W, H, pick, toPx, 1.0, 1.0, textW)
        // nothing picked: the door's one read figure; the window, the point and the wished door cost nothing -- and of
        // the Tapes, Lines, areas and chains (P5: placed with the marks) only the Tape, whose figure is the photo's
        lay(Pick.NONE).let { ms ->
            assertEquals(listOf(D1, W1, E1, D2, "t1"), ms.map { it.id }); assertEquals(listOf(1, 0, 0, 0, 1), ms.map { it.figs.size })
            assertTrue(ms.none { it.picked || it.tabs })
            val out = DimLayout.placeShown(ms, 340.0, 340.0, emptyList(), 1.0, 1.0)
            assertEquals(listOf("FD1.w", "t1#"), out.plates.map { it.code }); assertEquals(listOf("898", "1234"), out.plates.map { it.fig.text })
            // the Tape's figure is placed so that nothing lies on it, and is no tap target: it takes no reading
            assertEquals(listOf("FD1.w"), out.tags.map { it.code })
        }
        // P3: the door's position read too -- shown on the door that is not picked
        p.actual["FD1.l"] = 300.0
        assertEquals(listOf("FD1.w", "FD1.l"), lay(Pick.NONE)[0].figs.map { it.code })
        p.actual.remove("FD1.l")
        // the window HELD, its width gold: its five figures, its tabs; the door still its one
        lay(State(W1, true, "FW1.w")).let { ms ->
            assertEquals(listOf(1, 5, 0, 0, 1), ms.map { it.figs.size }); assertEquals(listOf(false, true, false, false, false), ms.map { it.tabs }); assertTrue(ms[1].picked)
            assertEquals(listOf("▶?", "?", "?", "?", "⊥ ?"), ms[1].figs.map { it.text }); assertEquals(listOf(true, false, false, false, false), ms[1].figs.map { it.active })
            // the figures the layout is handed are exactly the ones the rule answers
            assertEquals(PhotoFigures.onFace(Tx.EN, p, "front", W, H, State(W1, true, "FW1.w")).map { f -> f.second.map { it.code } }, ms.take(4).map { m -> m.figs.map { it.code } })
        }
        // reached by the walk: picked, all its figures, NO tabs
        lay(State(W1, false, "FW1.w")).let { ms -> assertTrue(ms[1].picked); assertFalse(ms[1].tabs); assertEquals(5, ms[1].figs.size) }
        // a held point has no tabs; a held wished door shows its tabs and no figure
        assertFalse(lay(State(E1, true, "FE1.cr"))[2].tabs)
        lay(State(D2, true, null))[3].let { assertTrue(it.tabs); assertTrue(it.figs.isEmpty()) }
    }
}
