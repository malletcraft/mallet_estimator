package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.MeasureFlow.Refused
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * THE MEASURING SCREEN'S OWN RULES (port 63 slice M2; DESIGN-port63 §1.3, §1.4, §1.5, §1.8). The screen is compiled
 * only by CI and cannot be run here, so everything it DECIDES is held here: what a tap on a figure and on a mark does,
 * what may be measured in which phase, what a reading leaves active, which bar is drawn and what it says, and when the
 * photo pans. The synthetic room of MeasTest: L 3319, W 2678, H 2677. English first (Amit, 16 Nov 2026): the new
 * `sp.measure.*` texts are pinned in English; what the bar reads from keys the app already had keeps its Marathi.
 *
 * (Test names are plain ASCII: MeasureWalkTest's header says why.)
 */
class MeasureFlowTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0
    private fun ab(s: String): DoubleArray = SurveyPrep.size(s, L, W, H)
    private fun pt(x: Double, y: Double) = doubleArrayOf(x, y)

    private val STAGES: List<Int?> = listOf(0, 1, 2, 3, 4, 5, null)
    private val AFTER = listOf(1, 2, 3, 4, 5)
    private val SURVEY = PhaseTools.SURVEY

    private fun SurveyPrep.Prep.put(s: String, t: String, n: Int, x0: Double, x1: Double, y0: Double, y1: Double,
                                    extra: Map<String, Any?> = emptyMap()): SurveyPrep.Mark =
        SurveyPrep.Mark("id-$s-$t$n", t, SurveyPrep.CODE.getValue(s) + t + n, x0, x1, y0, y1, extra).also { surface(s).marks.add(it) }

    private val wish = mapOf(MarkLife.KEY_BORN to MarkLife.WISH, MarkLife.KEY_LIFE to MarkLife.WISH)
    private val vadd = mapOf(MarkLife.KEY_BORN to MarkLife.VADD, MarkLife.KEY_LIFE to MarkLife.VADD)

    /** MeasureWalkTest's wall: a door, an electric point and a window; a Note, a door born a wish and a tape, never
     *  asked; a Line, an area and a chain that stops short of the corners -- and a window a vendor added. */
    private fun wall(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        p.put("front", "W", 1, 1500.0, 2400.0, 900.0, 2100.0)
        p.put("front", MarkTypes.NOTE, 1, 100.0, 150.0, 1200.0, 1250.0)
        p.put("front", "E", 1, 1325.0, 1375.0, 275.0, 325.0)
        p.put("front", "D", 2, 50.0, 250.0, 0.0, 2100.0, wish)
        p.put("front", "W", 2, 2450.0, 2650.0, 2200.0, 2500.0, vadd)
        p.put("front", "D", 1, 300.0, 1200.0, 0.0, 2100.0)
        p.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 1000.0), pt(1234.0, 1000.0)), "t1")
        p.addMeas("front", SurveyMeas.LINE, listOf(pt(500.0, 1600.0), pt(1300.0, 2200.0)), "m1")
        p.addMeas("front", SurveyMeas.AREA, listOf(pt(1800.0, 300.0), pt(2400.0, 300.0), pt(2400.0, 800.0)), "m2")
        p.addChain("front", "x", listOf(pt(300.0, 2400.0), pt(1200.0, 2400.0), pt(2400.0, 2400.0)), listOf(null, null, null), "k1")
        return p
    }

    private val WALL = listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p", "FE1.cr", "FE1.cs", "FW1.w", "FW1.h", "FW1.r", "FW1.s", "FW1.p",
        "F-M1.len", "F-M2.per", "FK1.1", "FK1.2", "FK1.t")

    private fun id(p: SurveyPrep.Prep, code: String) = p.surface("front").marks.first { it.code == code }.id
    private fun figure(p: SurveyPrep.Prep, code: String, active: String?, stage: Int?) = MeasureFlow.figure(p, code, active, stage, ::ab)
    private fun readAll(p: SurveyPrep.Prep, s: String = "front") { for (k in ab(s).let { MeasureWalk.stops(p, s, it[0], it[1]) }) p.actual[k] = 1000.0 }

    // ------------------------------------------------------------------ what may be measured

    @Test fun `readings are keyed at Survey and while the stage is unread, and in no phase after it`() {
        assertEquals(listOf(true, false, false, false, false, false, true), STAGES.map { MeasureFlow.canRead(it) })
        // the one rule, three ways round: the table's cell, the row survey prep honours, and the refusal's sentence
        for (st in STAGES) {
            assertEquals(PhaseTools.can(PhaseTools.Tool.READINGS, st), MeasureFlow.canRead(st), "$st")
            assertEquals(PhaseTools.readingRefusal(Tx.EN, st) == null, MeasureFlow.canRead(st), "$st")
        }
        assertTrue(PhaseTools.Tool.READINGS in PhaseTools.PREP_HONOURS, "survey prep honours the readings row since slice M2")
        // a project made on this phone and never sent is given Survey's rules: it is measured
        assertTrue(MeasureFlow.canRead(PhaseTools.stageOf(null, phoneOnly = true)))
    }

    @Test fun `what takes a reading - a mark in the room, a Line, an area, a chain - never a wish, a Note, a tape or a key nobody asks`() {
        val p = wall()
        fun takes(code: String, stage: Int? = SURVEY) = MeasureFlow.takes(p, code, stage, ::ab)
        for (k in WALL) assertTrue(takes(k), k)
        // exactly the walk's stops: nothing the walk reaches is refused, nothing else is taken
        assertEquals(WALL, ab("front").let { MeasureWalk.stops(p, "front", it[0], it[1]) })
        for (k in listOf("FD2.w", "FD2.h", "FD2.l",   // born a Client wish: not in the room
            "FW2.w", "FW2.r",                          // a mark a vendor added: not in the room either
            "FNOTE1.w",                                // a Note is only words
            "F-T1.len",                                // a tape's figure is the photo's: it has no reading
            "FD1.s", "FD1.t",                          // a measure the door's geometry drops: it touches the floor
            "FE1.w", "FE1.cl",                         // a point has no width, and is measured from its NEARER corner
            "F-M1.per", "F-M2.len", "FK1.3", "FK1.x",  // not this item's key
            "FZ9.w", "", "FD1"))                       // nothing owns it
            assertFalse(takes(k), k)
        // after Survey NOTHING takes a reading, whatever it is; unread, everything does as at Survey
        for (st in AFTER) for (k in WALL) assertFalse(takes(k, st), "$k at $st")
        for (k in WALL) assertTrue(takes(k, null), "$k, unread")
        // activeFor: the key itself where it takes a reading, else nothing -- what `active` is set to
        assertEquals("FD1.l", MeasureFlow.activeFor(p, "FD1.l", SURVEY, ::ab)); assertEquals("F-M1.len", MeasureFlow.activeFor(p, "F-M1.len", null, ::ab))
        assertNull(MeasureFlow.activeFor(p, "FD2.w", SURVEY, ::ab)); assertNull(MeasureFlow.activeFor(p, "FD1.l", 1, ::ab))
        assertNull(MeasureFlow.activeFor(p, null, SURVEY, ::ab)); assertNull(MeasureFlow.activeFor(p, "F-T1.len", SURVEY, ::ab))
    }

    @Test fun `a chain's total takes a reading only where the laser does not give it`() {
        val p = SurveyPrep.fresh(L, W, H)
        p.addChain("front", "x", listOf(pt(0.0, 1200.0), pt(905.0, 1200.0), pt(W, 1200.0)), listOf(null, null, null), "edge")
        p.addChain("front", "x", listOf(pt(300.0, 2400.0), pt(1200.0, 2400.0)), listOf(null, null), "short")
        assertEquals(listOf("FK1.1", "FK1.2", "FK2.1", "FK2.t"), ab("front").let { MeasureWalk.stops(p, "front", it[0], it[1]) })
        assertFalse(MeasureFlow.takes(p, "FK1.t", SURVEY, ::ab), "edge to edge: the laser's W is the total")
        assertTrue(MeasureFlow.takes(p, "FK2.t", SURVEY, ::ab)); assertTrue(MeasureFlow.takes(p, "FK1.2", SURVEY, ::ab))
        // a tap on the laser's total (were it a target) picks the chain and makes nothing active; what was active on it stays
        val t = figure(p, "FK1.t", "FK1.2", SURVEY)
        assertEquals("edge", t.pick?.id); assertEquals("FK1.2", t.active); assertFalse(t.pad); assertNull(t.refused)
        assertNull(figure(p, "FK1.t", "FK2.1", SURVEY).active, "another item's reading is let go")
    }

    @Test fun `what picking or placing an item makes active - its first reading still missing, nothing after Survey or off the room`() {
        val p = wall()
        fun first(id: String, stage: Int? = SURVEY) = MeasureFlow.firstOpen(p, id, stage, ::ab)
        assertEquals("FD1.w", first(id(p, "FD1"))); assertEquals("FE1.cr", first(id(p, "FE1")))
        assertEquals("F-M1.len", first("m1")); assertEquals("F-M2.per", first("m2")); assertEquals("FK1.1", first("k1"))
        assertNull(first("t1"), "a tape has no reading"); assertNull(first(id(p, "FNOTE1"))); assertNull(first("nothing"))
        assertNull(first(id(p, "FD2")), "born a Client wish"); assertNull(first(id(p, "FW2")), "a vendor's addition")
        // as readings land, the next one missing; with none missing, nothing
        p.actual["FD1.w"] = 905.0; p.actual["FD1.h"] = 2105.0
        assertEquals("FD1.l", first(id(p, "FD1")))
        p.actual["FD1.l"] = 298.0; p.actual["FD1.p"] = 230.0
        assertNull(first(id(p, "FD1")), "a fully measured mark: nothing is active, every plate of it takes a tap")
        p.actual["F-M1.len"] = 1240.0
        assertNull(first("m1"))
        // after Survey nothing is ever made active by a pick or a placement; unread, as at Survey
        for (st in AFTER) for (i in listOf(id(p, "FW1"), "m2", "k1")) assertNull(first(i, st), "$i at $st")
        assertEquals("FW1.w", first(id(p, "FW1"), null))
    }

    // ------------------------------------------------------------------ the taps

    @Test fun `a tap on a figure picks its owner and makes that reading active - on the one already active the pad opens`() {
        val p = wall()
        val t = figure(p, "FD1.h", "FD1.w", SURVEY)
        assertEquals(id(p, "FD1"), t.pick?.id); assertEquals("FD1.h", t.active); assertFalse(t.pad); assertNull(t.refused)
        // from nothing, and from another mark's reading: the tapped one is active, its owner picked
        assertEquals("FW1.s", figure(p, "FW1.s", null, SURVEY).active); assertEquals(id(p, "FW1"), figure(p, "FW1.s", "FD1.w", SURVEY).pick?.id)
        // THE PLATE THAT IS ALREADY ACTIVE: nothing changes but the pad, which opens -- the one way it opens from the photo
        val again = figure(p, "FD1.h", "FD1.h", SURVEY)
        assertEquals("FD1.h", again.active); assertTrue(again.pad); assertNull(again.refused); assertEquals(id(p, "FD1"), again.pick?.id)
        // A READING ALREADY TAKEN IS RE-OPENED THE SAME WAY: read or not, a tap makes it the one being measured
        p.actual["FD1.w"] = 905.0
        assertEquals("FD1.w", figure(p, "FD1.w", null, SURVEY).active)
        // a Line's and an area's tag and a chain's figures are figures too: the list was the only way back to these
        for ((k, owner) in listOf("F-M1.len" to "m1", "F-M2.per" to "m2", "FK1.1" to "k1", "FK1.2" to "k1", "FK1.t" to "k1")) {
            p.actual[k] = 1234.0
            val f = figure(p, k, "FD1.w", SURVEY)
            assertEquals(owner, f.pick?.id, k); assertEquals(k, f.active, k); assertFalse(f.pad); assertNull(f.refused)
        }
        // unread, as at Survey
        assertEquals("FD1.l", figure(p, "FD1.l", null, null).active)
        // a key nothing owns is nobody's tap: nothing is picked, and what was being measured stays
        val none = figure(p, "FZ9.w", "FD1.w", SURVEY)
        assertNull(none.pick); assertEquals("FD1.w", none.active); assertFalse(none.pad); assertNull(none.refused)
    }

    @Test fun `a tap on a figure of a mark that is not in the room picks it, says so, and makes nothing active - in every phase`() {
        val p = wall()
        for (st in STAGES) for ((k, code) in listOf("FD2.w" to "FD2", "FD2.l" to "FD2", "FW2.h" to "FW2")) {
            val t = figure(p, k, "FD1.w", st)
            assertEquals(id(p, code), t.pick?.id, "$k at $st: it is picked, so its label can open its Mark sheet")
            assertNull(t.active, "$k at $st: nothing is active"); assertFalse(t.pad)
            // NOT_IN_ROOM, never READ_ONLY: such a mark is never measured, so "move the phase back" would be a false promise
            assertEquals(Refused.NOT_IN_ROOM, t.refused, "$k at $st")
        }
        // a reading such a mark holds from an earlier build changes nothing: it is shown, never asked for
        p.actual["FD2.w"] = 200.0
        assertEquals(Refused.NOT_IN_ROOM, figure(p, "FD2.w", null, SURVEY).refused)
    }

    @Test fun `after Survey a tap on a figure picks its owner, says the phase's refusal, and makes nothing active`() {
        val p = wall(); p.actual["FD1.w"] = 905.0
        for (st in AFTER) for (k in listOf("FD1.w", "FD1.h", "FE1.cr", "F-M1.len", "FK1.2")) {
            val t = figure(p, k, null, st)
            assertNotNull(t.pick, "$k at $st"); assertNull(t.active, "$k at $st"); assertFalse(t.pad)
            assertEquals(Refused.READ_ONLY, t.refused, "$k at $st")
        }
        // the sentence the screen then says is the phase's own
        assertEquals("Readings are taken in the Survey phase — move the phase back on the project screen to key one", PhaseTools.readingRefusal(Tx.EN, 3))
    }


    // ------------------------------------------------------------------ after a reading

    @Test fun `after a reading in 3 Measure the same mark's next missing one stays active - with none left the walk moves on`() {
        val p = wall()
        fun stay(code: String) = ab("front").let { MeasureFlow.stay(p, "front", it[0], it[1], code, ::ab) }
        fun walk(code: String) = ab("front").let { MeasureWalk.next(p, "front", it[0], it[1], code) }
        p.actual["FD1.w"] = 905.0
        assertEquals("FD1.h", stay("FD1.w"))
        // skipped with the arrow, then the rest read: the mark's own order WRAPS to the one skipped before the walk leaves it
        p.actual.remove("FD1.w"); for (k in listOf("FD1.h", "FD1.l", "FD1.p")) p.actual[k] = 100.0
        assertEquals("FD1.w", stay("FD1.p"))
        p.actual["FD1.w"] = 905.0
        // THE MARK HAS NOTHING LEFT: stay answers nothing, and the walk goes to the next unmeasured dimension of the face
        assertNull(stay("FD1.w")); assertEquals("FE1.cr", walk("FD1.w")); assertEquals("FE1.cr", walk("FD1.p"))
        // a Line has one reading, so none after it; the walk goes on to the area
        p.actual["F-M1.len"] = 1240.0
        assertNull(stay("F-M1.len")); assertEquals("F-M2.per", walk("F-M1.len"))
        // the last reading of the face: nothing stays and nothing is next -- the bar then says the face is finished
        for (k in WALL) p.actual[k] = 100.0
        assertNull(stay("FK1.t")); assertNull(walk("FK1.t"))
    }

    @Test fun `the walk does not stop on a chain's derived segment, where the D2 flow of 2 Mark still does`() {
        val p = SurveyPrep.fresh(L, W, H)
        p.addChain("front", "x", listOf(pt(0.0, 1200.0), pt(905.0, 1200.0), pt(W, 1200.0)), listOf(null, null, null), "edge")
        p.put("front", "D", 1, 300.0, 1200.0, 0.0, 2100.0)
        // edge to edge: the laser gives the total, so with one segment read the other is DERIVED -- it has no reading by design
        p.actual["FK1.1"] = 905.0
        assertEquals("FK1.2", p.nextAfter("FK1.1", ::ab), "2 · Mark's and the 360's flow lands on it, as it always did")
        assertNull(ab("front").let { MeasureFlow.stay(p, "front", it[0], it[1], "FK1.1", ::ab) }, "3 · Measure does not: it is not left")
        assertFalse(ab("front").let { MeasureWalk.left(p, "front", it[0], it[1], "FK1.2") })
        // ... and the walk goes round to the door, the only thing left on the face
        assertEquals("FD1.w", ab("front").let { MeasureWalk.next(p, "front", it[0], it[1], "FK1.1") })
        // the derived segment still takes a reading when he taps its figure: it is a stop, only not a pending one
        assertEquals("FK1.2", figure(p, "FK1.2", null, SURVEY).active)
    }

    @Test fun `a face tab counts what is still to measure there - and nothing after Survey`() {
        val p = wall()
        fun left(s: String, stage: Int? = SURVEY) = ab(s).let { MeasureFlow.tabLeft(p, s, stage, it[0], it[1]) }
        assertEquals(16, left("front")); assertEquals(0, left("right")); assertEquals(0, left("floor"))
        p.actual["FD1.w"] = 905.0; p.actual["FK1.2"] = 1200.0
        assertEquals(14, left("front")); assertEquals(14, left("front", null))
        for (st in AFTER) assertEquals(0, left("front", st), "no count at $st: nothing is asked there")
        readAll(p)
        assertEquals(0, left("front"), "nothing left: no count")
    }

    // ------------------------------------------------------------------ the one figure, and the check



    // ------------------------------------------------------------------ a mark with four free corners (port 63 slice Q1)

    /** The design's example, made up: the front wall 3600 x 2700 of PROJ-T1; window FW1 with uprights 1550 (right) and
     *  780 (left), a level top of 2370 and a sloping bottom; and a door, a rectangle. */
    private val QL = 5000.0; private val QA = 3600.0; private val QB = 2700.0
    private fun qab(s: String): DoubleArray = SurveyPrep.size(s, QL, QA, QB)
    private fun trapezoid(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(QL, QA, QB)
        val w = p.put("front", "W", 1, 600.0, 2970.0, 900.0, 2450.0)
        assertTrue(Quad.set(w, listOf(pt(600.0, 1670.0), pt(2970.0, 900.0), pt(2970.0, 2450.0), pt(600.0, 2450.0))))
        p.put("front", "D", 1, 100.0, 500.0, 0.0, 2100.0)
        p.actual.putAll(mapOf("FD1.w" to 400.0, "FD1.h" to 2100.0, "FD1.l" to 100.0, "FD1.p" to 230.0))
        return p
    }
    /** THE CLOSING CHECK'S SENTENCE, WITH NO FIGURE (port 63 slice Q2a; DESIGN-quad §2.4, §11). Q1's read "The other sides
     *  make the bottom side ≈2466 mm. Check the readings, or the corners." -- a figure nobody measured -- and went in
     *  slice I1b with every other estimate (DESIGN-imagemeter §18). The check compares readings with readings, so it
     *  stays; its sentence names the side and quotes nothing. */
    private val SAY = "The four sides do not agree — see the ! on the bottom side. Check the readings, or the corners."
    private val GUARD = "This reading does not match the mark. Shoot again, or ▸ to keep it."
    private val TW = "id-front-W1"
    private fun qsc(p: SurveyPrep.Prep, stage: Int? = SURVEY) = Pick.Scene(p, stage, StepRoute.MARK, ::qab)

    @Test fun `the closing check's sentence names the side and quotes no figure - said when the reading that completes the four lands, and on the sloping side's plate`() {
        val p = trapezoid()
        fun say(code: String) = MeasureFlow.guardText(Tx.EN, p, code, ::qab)
        // THE WALK READS THE SLOPING BOTTOM FIRST and the left upright last: until the four are in, nothing is said
        p.actual.putAll(mapOf("FW1.bottom" to 2495.0, "FW1.right" to 1550.0, "FW1.top" to 2370.0))
        for (c in listOf("FW1.bottom", "FW1.right", "FW1.top", "FW1.left")) assertNull(say(c), c)
        // the left upright typed 870 for 780: the other three make the bottom 2466, it was read 2495 -- 29 apart, over 25.
        // The sentence is said AS THE 870 LANDS, on the left side's code, and names the BOTTOM side, where the `!` is
        p.actual["FW1.left"] = 870.0
        assertTrue(Quad.closing(p.actual, p.markById(TW)!!.second)!!.fails)
        assertEquals(SAY, say("FW1.left"))
        // ... and on every one of the four side codes while it fails -- the sloping side's own plate as it turns gold
        for (c in listOf("FW1.bottom", "FW1.right", "FW1.top", "FW1.left")) assertEquals(SAY, say(c), c)
        // NO FIGURE: not the 2466 the other sides make, not a reading, no `≈`
        assertFalse(SAY.any { it.isDigit() }); assertFalse("≈" in SAY)
        // the `!` is on the sloping side's plate alone; the other three are not flagged, so the gold moves on from them
        assertEquals(listOf("FW1.bottom"), listOf("FW1.bottom", "FW1.right", "FW1.top", "FW1.left").filter { MeasureFlow.flagged(p, it, ::qab) })
        // where the mark sits, the reveal, and another mark: nothing
        for (c in listOf("FW1.l", "FW1.s", "FW1.p", "FD1.w", "FD1.h")) assertNull(say(c), c)
        // a Msg, rendered in the language on: its Marathi is pending, so it is its English for now
        assertEquals(SAY, MeasureFlow.guardMsg(p, "FW1.left", ::qab)!!(Tx.MR))

        // AS THE LASER'S 870 LANDS on the left side (the gold was there): one tick -- that reading passes its own guard --
        // the note says the sentence, and the gold walks on to where the mark sits
        val q = trapezoid(); q.actual.putAll(mapOf("FW1.bottom" to 2495.0, "FW1.right" to 1550.0, "FW1.top" to 2370.0, "FW1.left" to 870.0))
        val a = Landing.after(qsc(q), Pick.State(TW, false, "FW1.left"), "FW1.left", busy = false)
        assertEquals(Landing.Haptic.TICK, a.haptic); assertEquals(SAY, a.note!!(Tx.EN)); assertEquals("FW1.l", a.state.gold); assertTrue(a.byItself)
        // typed on the keypad and Next: the same note, and the gold moves
        assertEquals(SAY, Landing.keyed(qsc(q), Pick.State(TW, false, "FW1.left"), "FW1.left").note!!(Tx.EN))
        // THE SLOPING SIDE READ LAST: its own plate takes the `!` -- a double buzz, the gold stays on it, the same sentence
        val r = trapezoid(); r.actual.putAll(mapOf("FW1.right" to 1550.0, "FW1.top" to 2370.0, "FW1.left" to 870.0, "FW1.bottom" to 2495.0))
        val b = Landing.after(qsc(r), Pick.State(TW, false, "FW1.bottom"), "FW1.bottom", busy = false)
        assertEquals(Landing.Haptic.DOUBLE, b.haptic); assertEquals(SAY, b.note!!(Tx.EN)); assertEquals("FW1.bottom", b.state.gold); assertFalse(b.byItself)
        // read as 2495, 1550, 2370, 780 it closes (3 mm apart): nothing is said, and the wall's last figure says so instead
        r.actual["FW1.left"] = 780.0
        for (c in listOf("FW1.bottom", "FW1.right", "FW1.top", "FW1.left")) assertNull(MeasureFlow.guardText(Tx.EN, r, c, ::qab), c)
    }

    @Test fun `which sentence, when a side could say two - the guard's on the reading just taken, the closing one on the sloping side's own plate`() {
        val p = trapezoid()
        fun say(code: String) = MeasureFlow.guardText(Tx.EN, p, code, ::qab)
        // the left upright read 1700 where it is drawn 780: MORE THAN TWICE -- that reading fails its own guard, and the
        // four no longer close either. On the LEFT side's code the guard's sentence: it is about the figure just read
        p.actual.putAll(mapOf("FW1.bottom" to 2495.0, "FW1.right" to 1550.0, "FW1.top" to 2370.0, "FW1.left" to 1700.0))
        assertTrue(Quad.closing(p.actual, p.markById(TW)!!.second)!!.fails)
        assertEquals(GUARD, say("FW1.left"))
        // on the other sides, whose own readings pass: the closing sentence
        assertEquals(SAY, say("FW1.right")); assertEquals(SAY, say("FW1.top")); assertEquals(SAY, say("FW1.bottom"))
        // the laser's 1700 lands: a double buzz, the gold stays on the left side, and the note is the guard's
        val a = Landing.after(qsc(p), Pick.State(TW, false, "FW1.left"), "FW1.left", busy = false)
        assertEquals(Landing.Haptic.DOUBLE, a.haptic); assertEquals(GUARD, a.note!!(Tx.EN)); assertEquals("FW1.left", a.state.gold)
        // ON THE SLOPING SIDE'S OWN CODE the closing sentence, also when that reading fails the guard too
        p.actual["FW1.left"] = 870.0; p.actual["FW1.bottom"] = 6000.0
        assertTrue(MeasureFlow.flagged(p, "FW1.bottom", ::qab)); assertEquals(SAY, say("FW1.bottom"))
        // A RECTANGLE says what it always said: the guard's sentence, for a width read five times what is drawn
        p.actual["FD1.w"] = 2000.0
        assertEquals(GUARD, say("FD1.w")); assertNull(say("FD1.h"))
        // ON THE FLOOR AND THE CEILING the bottom side is the back side
        val f = SurveyPrep.fresh(QL, QA, QB)
        val s = f.put("floor", "S", 1, 1000.0, 2000.0, 1000.0, 2000.0)
        assertTrue(Quad.set(s, listOf(pt(1000.0, 1300.0), pt(2000.0, 1000.0), pt(2000.0, 2000.0), pt(1000.0, 2000.0))))
        val c = s.code
        f.actual.putAll(mapOf("$c.bottom" to 1100.0, "$c.right" to 1000.0, "$c.top" to 1000.0, "$c.left" to 700.0))
        assertEquals("The four sides do not agree — see the ! on the back side. Check the readings, or the corners.", MeasureFlow.guardText(Tx.EN, f, "$c.left", ::qab))
        assertEquals(MeasureText.sidesSay(Tx.EN, Quad.sideWhat("floor", 0)(Tx.EN)), MeasureFlow.guardText(Tx.EN, f, "$c.top", ::qab))
    }




    // The measure bar's tests -- its kinds, its words, ‹ and ›, the M2 review's "nothing picked, nothing measured" and
    // the four-step route's moves -- went with the bar in port 63 slice I1b; their rules that survive are PickTest's.

    // ------------------------------------------------------------------ the photo pans only if it must

    @Test fun `what a reading measures on its face - a mark's dimension, a Line's ends, a chain's segment`() {
        val p = wall()
        fun span(code: String) = MeasureFlow.span(p, code, ::ab)?.toList()
        assertEquals(listOf(300.0, 2100.0, 1200.0, 2100.0), span("FD1.w"), "along its top edge")
        assertEquals(listOf(1200.0, 0.0, 1200.0, 2100.0), span("FD1.h"), "up its right edge")
        assertEquals(listOf(0.0, 1050.0, 300.0, 1050.0), span("FD1.l"), "from the left corner at mid-height")
        assertEquals(listOf(1200.0, 0.0, 1200.0, 0.0), span("FD1.p"), "a p has no line: its corner")
        assertEquals(listOf(1350.0, 300.0, W, 300.0), span("FE1.cr"), "a point's, from its centre")
        assertEquals(listOf(500.0, 1600.0, 1300.0, 2200.0), span("F-M1.len"))
        assertEquals(listOf(1800.0, 300.0, 2400.0, 800.0), span("F-M2.per"), "an area: its first corner to its last")
        assertEquals(listOf(300.0, 2400.0, 1200.0, 2400.0), span("FK1.1")); assertEquals(listOf(1200.0, 2400.0, 2400.0, 2400.0), span("FK1.2"))
        assertEquals(listOf(300.0, 2400.0, 2400.0, 2400.0), span("FK1.t"), "the total: the chain's first point to its last")
        for (k in listOf("FZ9.w", "FD1.s", "F-T1.len", "F-M1.per", "FK1.9")) assertNull(span(k), k)
    }

    @Test fun `the photo pans to the figure being measured only when none of it is on screen - at fit nothing ever moves`() {
        // a 600 px box over a 1000 x 1000 picture, fitted whole
        val box = SurveyPrep.PhotoBox(600.0, 600.0, doubleArrayOf(0.0, 0.0, 1000.0, 1000.0), 0.0, 0.0, 1000.0, 1000.0)
        val fit = SurveyPrep.PhotoView()
        assertNull(MeasureFlow.panTo(fit, box, 100.0, 100.0, 300.0, 100.0), "fitted: the whole face is on screen")
        assertNull(MeasureFlow.panTo(fit, box, 990.0, 990.0, 990.0, 990.0), "a p's corner, on screen")
        // zoomed 4 times onto the far corner: a dimension near the other corner is wholly off screen
        val zoomed = SurveyPrep.PhotoView(false, 4.0, 800.0, 800.0)
        val moved = MeasureFlow.panTo(zoomed, box, 100.0, 100.0, 300.0, 100.0)!!
        assertFalse(moved.fitted); assertEquals(4.0, moved.z, "its own zoom is kept")
        assertEquals(200.0, moved.cx); assertEquals(100.0, moved.cy, "centred on the middle of what is measured")
        assertNull(MeasureFlow.panTo(moved, box, 100.0, 100.0, 300.0, 100.0), "and from there it is on screen: nothing moves again")
        // any part of it on screen is enough: the plate is slid into view by the layout, the photo stays
        assertNull(MeasureFlow.panTo(zoomed, box, 100.0, 800.0, 760.0, 800.0), "its far end is on screen")
        assertNull(MeasureFlow.panTo(zoomed, box, 800.0, 800.0, 800.0, 800.0), "a corner at the very centre")
        // a point off screen pans too
        assertNotNull(MeasureFlow.panTo(zoomed, box, 100.0, 100.0, 100.0, 100.0))
        // the centre is kept over the picture
        val edge = MeasureFlow.panTo(zoomed, box, 0.0, 0.0, 0.0, 10.0)!!
        assertTrue(edge.cx >= 0.0 && edge.cy >= 0.0)
    }
}
