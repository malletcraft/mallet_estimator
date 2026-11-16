package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.MeasureFlow.Kind
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
    private fun body(p: SurveyPrep.Prep, id: String, picked: String?, active: String?, stage: Int?) = MeasureFlow.body(p, id, picked, active, stage, ::ab)
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
        assertEquals("200", MeasureFlow.value(p, "FD2.w", ::ab)?.text)
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

    @Test fun `a tap on a mark in 3 Measure picks it and makes its first unmeasured dimension active - never by a second tap, never off the room`() {
        val p = wall()
        val d1 = id(p, "FD1"); val w1 = id(p, "FW1")
        // a fresh pick: its first unmeasured dimension
        body(p, d1, null, null, SURVEY).let { assertEquals(d1, it.pick?.id); assertEquals("FD1.w", it.active); assertFalse(it.pad); assertNull(it.refused) }
        assertEquals("FW1.w", body(p, w1, d1, "FD1.l", SURVEY).active, "another mark picked: its own first, the other's let go")
        // TAPPED AGAIN while one of its own dimensions is active: that one stays -- a second tap must not lose what he chose
        assertEquals("FD1.l", body(p, d1, d1, "FD1.l", SURVEY).active)
        // picked, but what is active is another item's (or nothing): its first unmeasured
        assertEquals("FD1.w", body(p, d1, d1, "FW1.s", SURVEY).active); assertEquals("FD1.w", body(p, d1, d1, null, SURVEY).active)
        // as readings land the first unmeasured moves on; a measured mark is picked with nothing active
        p.actual["FD1.w"] = 905.0
        assertEquals("FD1.h", body(p, d1, null, null, SURVEY).active)
        for (k in listOf("FD1.h", "FD1.l", "FD1.p")) p.actual[k] = 100.0
        body(p, d1, null, "FW1.w", SURVEY).let { assertEquals(d1, it.pick?.id); assertNull(it.active, "fully measured: picked, nothing active") }
        // a mark not in the room, and a Note: picked -- its label opens its sheet -- nothing active, and NOTHING SAID
        for (code in listOf("FD2", "FW2", "FNOTE1")) body(p, id(p, code), null, "FW1.w", SURVEY).let {
            assertEquals(id(p, code), it.pick?.id, code); assertNull(it.active, code); assertNull(it.refused, code) }
        // after Survey: picked, nothing active, nothing said (a tap on a mark is how its label is reached there)
        for (st in AFTER) body(p, w1, null, null, st).let { assertEquals(w1, it.pick?.id); assertNull(it.active); assertNull(it.refused) }
        assertEquals("FW1.w", body(p, w1, null, null, null).active, "unread: as at Survey")
        // an id nothing has: nobody's tap
        body(p, "nothing", d1, "FD1.l", SURVEY).let { assertNull(it.pick); assertEquals("FD1.l", it.active) }
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

    @Test fun `the one figure of any reading - the reading, else the photo's estimate, else a question mark`() {
        val p = wall()
        fun v(code: String) = MeasureFlow.value(p, code, ::ab)?.let { Triple(it.text, it.state, it.check) }
        assertEquals(Triple("≈900", MarkDims.State.ESTIMATE, false), v("FD1.w"))
        assertEquals(Triple("?", MarkDims.State.UNKNOWN, false), v("FD1.p"), "what the photo cannot show")
        assertEquals(Triple("≈1000", MarkDims.State.ESTIMATE, false), v("F-M1.len"), "a Line: 800 across and 600 up")
        assertEquals(Triple("≈900", MarkDims.State.ESTIMATE, false), v("FK1.1")); assertEquals(Triple("≈2100", MarkDims.State.ESTIMATE, false), v("FK1.t"))
        p.actual["FD1.w"] = 905.0; p.actual["FD1.p"] = 230.4; p.actual["F-M1.len"] = 1240.0; p.actual["F-M2.per"] = 5734.0; p.actual["FK1.t"] = 2105.0
        assertEquals(Triple("905", MarkDims.State.MEASURED, false), v("FD1.w")); assertEquals(Triple("230", MarkDims.State.MEASURED, false), v("FD1.p"))
        assertEquals(Triple("1240", MarkDims.State.MEASURED, false), v("F-M1.len")); assertEquals(Triple("5734", MarkDims.State.MEASURED, false), v("F-M2.per"))
        assertEquals(Triple("2105", MarkDims.State.MEASURED, false), v("FK1.t"))
        // a mark's figure is MarkDims' own, the plate's: one rule, so the bar's box and the photo cannot disagree
        for (k in listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p", "FE1.cr", "FW1.s")) {
            val (s, e) = p.markOf(k)!!; val z = ab(s)
            val d = MarkDims.of(s, e, z[0], z[1]).first { it.code == k }
            val plate = MarkDims.value(p.actual, s, e, k, d.est, z[0], z[1])
            assertEquals(Triple(plate.text, plate.state, plate.check), v(k), k)
        }
        // a key nothing asks for has no figure
        for (k in listOf("FZ9.w", "FD1.s", "F-T1.len", "FK1.9")) assertNull(v(k), k)
    }

    @Test fun `the check's sentence names the photo's estimate - only for a mark's reading far from it, on a mark in the squared plane`() {
        val p = wall()
        p.put("front", "CRP", 1, 150.0, 1350.0, 0.0, 2100.0)   // a wardrobe: it stands off the wall, and is never checked
        fun est(code: String) = MeasureFlow.checkEst(p, code, ::ab)
        assertNull(est("FD1.w"), "no reading yet: nothing to check")
        p.actual["FD1.w"] = 1100.0
        assertEquals(900.0, est("FD1.w"), "200 mm off a 900 door: more than the larger of 150 mm and 15 %")
        assertEquals("The photo says ≈900 mm. Check the mark, or shoot again.", MeasureText.photoSays(Tx.EN, est("FD1.w")!!))
        assertTrue(MeasureFlow.value(p, "FD1.w", ::ab)!!.check, "the same answer as the plate's !")
        p.actual["FD1.w"] = 1040.0
        assertNull(est("FD1.w"), "140 mm: an ordinary estimate, not a shot at the wrong target")
        // a mark that stands off the squared plane: its true reading is far from the photo, and it is never flagged
        p.actual["FCRP1.w"] = 700.0
        assertNull(est("FCRP1.w"), "a 1200 box read 700: a wardrobe nearer the camera, not a wrong shot")
        // what the photo cannot estimate has nothing to compare; a Line, an area and a chain are not checked; nor a key nobody owns
        p.actual["FD1.p"] = 5000.0; p.actual["F-M1.len"] = 9999.0; p.actual["FK1.1"] = 9999.0
        for (k in listOf("FD1.p", "F-M1.len", "FK1.1", "FZ9.w", "FD1.s")) assertNull(est(k), k)
    }

    // ------------------------------------------------------------------ the measure bar

    private fun onFace(p: SurveyPrep.Prep, s: String, picked: String?, active: String?, stage: Int?, tx: Tx = Tx.EN) =
        MeasureFlow.onFace(tx, p, s, picked, active, stage, ::ab)

    @Test fun `3 Measure's bar while a dimension is being measured - the label, its own words over the progress, the one figure, D2, both arrows`() {
        val p = wall()
        val b = onFace(p, "front", id(p, "FD1"), "FD1.l", SURVEY)
        assertEquals(Kind.ACTIVE, b.kind)
        assertEquals("FD1\nDoor", b.cell?.text)
        assertEquals("from left corner", b.what, "the measure's own words ALONE: the code and the type are in the label cell")
        assertEquals("0 of 16 measured", b.line)
        assertEquals("≈300", b.figure?.text); assertEquals(MarkDims.State.ESTIMATE, b.figure?.state)
        assertTrue(b.d2); assertTrue(b.prev); assertTrue(b.next); assertNull(b.nextFace); assertNull(b.nextFaceText)
        // the progress and the figure follow the readings
        p.actual["FD1.l"] = 298.0; p.actual["FD1.w"] = 905.0
        onFace(p, "front", id(p, "FD1"), "FD1.l", SURVEY).let { assertEquals("2 of 16 measured", it.line); assertEquals("298", it.figure?.text); assertEquals(MarkDims.State.MEASURED, it.figure?.state) }
        // a Line, an area and a chain: their own words; a chain has no label cell (it opens no sheet), a Line its code
        onFace(p, "front", "m1", "F-M1.len", SURVEY).let { assertEquals("length", it.what); assertEquals("F-M1", it.cell?.text); assertEquals("≈1000", it.figure?.text) }
        onFace(p, "front", "m2", "F-M2.per", SURVEY).let { assertEquals("perimeter", it.what); assertEquals("F-M2", it.cell?.text) }
        onFace(p, "front", "k1", "FK1.2", SURVEY).let { assertEquals("segment 2", it.what); assertNull(it.cell); assertEquals(Kind.ACTIVE, it.kind) }
        assertEquals("total", onFace(p, "front", "k1", "FK1.t", SURVEY).what)
        // nothing picked: no label cell; the bar is still the whole bar
        onFace(p, "front", null, "FE1.cr", SURVEY).let { assertNull(it.cell); assertEquals("from right corner, to centre", it.what); assertTrue(it.d2) }
        // unread: as at Survey
        assertEquals(Kind.ACTIVE, onFace(p, "front", null, "FD1.h", null).kind)
        // the measure's words are keys the app already had: under मराठी they read as they always did
        assertEquals("डाव्या कोपऱ्यापासून", onFace(p, "front", id(p, "FD1"), "FD1.l", SURVEY, Tx.MR).what)
    }

    @Test fun `3 Measure's bar with nothing being measured - the progress and both arrows while something is left`() {
        val p = wall(); p.actual["FD1.w"] = 905.0
        val b = onFace(p, "front", id(p, "FD1"), null, SURVEY)
        assertEquals(Kind.IDLE, b.kind); assertEquals("FD1\nDoor", b.cell?.text); assertNull(b.what); assertEquals("1 of 16 measured", b.line)
        assertNull(b.figure); assertFalse(b.d2); assertTrue(b.prev); assertTrue(b.next, "the arrow goes to the first unmeasured")
        // what is active on ANOTHER face is not this face's: the bar is the progress alone
        p.put("floor", "S", 1, 800.0, 1700.0, 150.0, 450.0)
        assertEquals(Kind.IDLE, onFace(p, "front", null, "GS1.w", SURVEY).kind); assertEquals(Kind.ACTIVE, onFace(p, "floor", null, "GS1.w", SURVEY).kind)
        // ... nor is a key of a mark that is not in the room, were one ever set
        assertEquals(Kind.IDLE, onFace(p, "front", id(p, "FD2"), "FD2.w", SURVEY).kind)
    }

    @Test fun `a finished face keeps its back arrow and offers the next face with something left - it never turns the room for him`() {
        val p = wall()
        p.put("right", "W", 1, 500.0, 1700.0, 900.0, 2100.0)
        p.put("floor", "S", 1, 800.0, 1700.0, 150.0, 450.0)
        readAll(p)
        val b = onFace(p, "front", null, null, SURVEY)
        assertEquals(Kind.DONE, b.kind); assertEquals("Front wall · all 16 measured ✓", b.line)
        assertEquals("right", b.nextFace?.s); assertEquals(5, b.nextFace?.left); assertEquals("Next: Right wall · 5 to measure ▸", b.nextFaceText)
        assertTrue(b.prev, "a reading on a finished face is still reached: the back arrow goes to its last stop"); assertFalse(b.next); assertFalse(b.d2); assertNull(b.figure); assertNull(b.what)
        assertEquals("FK1.t", ab("front").let { MeasureWalk.previous(p, "front", it[0], it[1], null) })
        // a reading re-opened on a finished face (a tap on its plate, or the back arrow): the whole bar again
        assertEquals(Kind.ACTIVE, onFace(p, "front", id(p, "FD1"), "FD1.w", SURVEY).kind); assertEquals("16 of 16 measured", onFace(p, "front", null, "FD1.w", SURVEY).line)
        // the next face in TAB order, wrapping: from the floor it is the front wall's turn only if something is left there
        readAll(p, "right")
        onFace(p, "front", null, null, SURVEY).let { assertEquals("floor", it.nextFace?.s); assertEquals("Next: Floor · 5 to measure ▸", it.nextFaceText) }
        // NO FACE HAS ANYTHING LEFT: the room is measured, and there is no next face to offer
        readAll(p, "floor")
        onFace(p, "front", null, null, SURVEY).let {
            assertEquals(Kind.DONE, it.kind); assertEquals("Room measured · all 26 ✓", it.line); assertNull(it.nextFace); assertNull(it.nextFaceText); assertTrue(it.prev) }
        assertEquals("Room measured · all 26 ✓", onFace(p, "floor", null, null, SURVEY).line)
    }

    @Test fun `a face with nothing to measure says so`() {
        val p = wall()
        val b = onFace(p, "back", null, null, SURVEY)
        assertEquals(Kind.EMPTY, b.kind); assertEquals("Nothing to measure on this face yet", b.line)
        assertNull(b.cell); assertNull(b.what); assertNull(b.figure); assertFalse(b.d2); assertFalse(b.prev); assertFalse(b.next); assertNull(b.nextFace)
        // a face that holds only what is never asked -- a Note, a wish, a tape -- is empty too, and its picked mark keeps its label
        val q = SurveyPrep.fresh(L, W, H)
        q.put("front", "D", 2, 50.0, 250.0, 0.0, 2100.0, wish); q.put("front", MarkTypes.NOTE, 1, 100.0, 150.0, 1200.0, 1250.0)
        q.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 1000.0), pt(1234.0, 1000.0)), "t1")
        onFace(q, "front", "id-front-D2", null, SURVEY).let { assertEquals(Kind.EMPTY, it.kind); assertEquals("FD2\nDoor", it.cell?.text) }
        // a chain whose one segment the laser's total already gives: a stop with nothing pending -- the back arrow still reaches it
        val c = SurveyPrep.fresh(L, W, H)
        c.addChain("front", "x", listOf(pt(0.0, 1200.0), pt(W, 1200.0)), listOf(null, null), "edge")
        onFace(c, "front", null, null, SURVEY).let { assertEquals(Kind.EMPTY, it.kind); assertTrue(it.prev); assertFalse(it.next) }
        // the sentence is an existing key: its Marathi is what Face Prep already said
        assertEquals("या बाजूवर मोजायला अजून काही नाही", onFace(p, "back", null, null, SURVEY, Tx.MR).line)
    }

    @Test fun `after Survey 3 Measure's bar is the picked item's label and the phase's refusal, standing - no box, no D2, no arrows, no count`() {
        val p = wall(); p.actual["FD1.w"] = 905.0
        for (st in AFTER) {
            val b = onFace(p, "front", id(p, "FD1"), null, st)
            assertEquals(Kind.READ_ONLY, b.kind, "$st")
            assertEquals("Readings are taken in the Survey phase — move the phase back on the project screen to key one", b.line)
            assertEquals("FD1\nDoor", b.cell?.text, "the label stays at every stage: it opens the Mark sheet (L4: the label stays, controls are hidden)")
            assertNull(b.what); assertNull(b.figure); assertFalse(b.d2); assertFalse(b.prev); assertFalse(b.next); assertNull(b.nextFace); assertNull(b.nextFaceText)
            // whatever was active before the stage moved, nothing is being measured
            assertEquals(Kind.READ_ONLY, onFace(p, "front", null, "FD1.h", st).kind)
            assertNull(onFace(p, "front", null, null, st).cell, "nothing picked: the sentence alone")
            // a mark born a wish is picked there too, and keeps its label
            assertEquals("FD2\nDoor", onFace(p, "front", id(p, "FD2"), null, st).cell?.text)
            // an empty face says the same: the step never reads as a broken screen
            assertEquals(Kind.READ_ONLY, onFace(p, "back", null, null, st).kind)
        }
        // the refusal is an existing key, with the Marathi it had
        assertEquals("रीडिंग सर्व्हे फेजमध्ये घेतले जातात — रीडिंग टाकण्यासाठी प्रोजेक्टच्या स्क्रीनवर फेज मागे न्या", onFace(p, "front", null, null, 2, Tx.MR).line)
    }

    private fun onItem(p: SurveyPrep.Prep, active: String?, stage: Int?, withCell: Boolean = false) = MeasureFlow.onItem(Tx.EN, p, active, stage, withCell, ::ab)

    @Test fun `in 2 Mark and 4 360 the bar shows only while a dimension is active, and its arrows stay inside the picked item`() {
        val p = wall()
        p.put("floor", "S", 1, 800.0, 1700.0, 150.0, 450.0)
        val b = onItem(p, "FD1.l", SURVEY)!!
        assertEquals(Kind.ACTIVE, b.kind); assertEquals("from left corner", b.what); assertEquals("0 of 16 measured", b.line)
        assertEquals("≈300", b.figure?.text); assertTrue(b.d2)
        assertNull(b.cell, "the picked mark's own row carries its label, as it always did")
        assertTrue(b.prev && b.next, "a door asks four things: the arrows move among them")
        // WITH THE PAD OPEN the pad has taken that row's place, and the bar carries the label
        assertEquals("FD1\nDoor", onItem(p, "FD1.l", SURVEY, withCell = true)!!.cell?.text)
        // an item with ONE key draws neither arrow; a chain moves among its own, and has no label even then
        onItem(p, "F-M1.len", SURVEY, withCell = true)!!.let { assertFalse(it.prev); assertFalse(it.next); assertEquals("F-M1", it.cell?.text); assertEquals("length", it.what) }
        onItem(p, "FK1.2", SURVEY, withCell = true)!!.let { assertTrue(it.prev && it.next); assertNull(it.cell); assertEquals("segment 2", it.what) }
        // the arrows' keys are the owner's own, read or not, wrapping -- never another mark's
        val keys = MeasureWalk.stopsOf(p, id(p, "FD1"), ::ab)
        assertEquals(listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p"), keys)
        assertEquals("FD1.w", MeasureWalk.step(keys, "FD1.p", forward = true)); assertEquals("FD1.p", MeasureWalk.step(keys, "FD1.w", forward = false))
        // LINE 2 COUNTS THE OWNER'S FACE (4 · 360 has no open face): the floor's step counts the floor
        assertEquals("0 of 5 measured", onItem(p, "GS1.w", SURVEY)!!.line)
        p.actual["GS1.w"] = 900.0
        assertEquals("1 of 5 measured", onItem(p, "GS1.h", SURVEY)!!.line); assertEquals("0 of 16 measured", onItem(p, "FD1.w", SURVEY)!!.line)
        // NO BAR: nothing active, after Survey, a mark not in the room, a key nobody asks -- and unread is as Survey
        assertNull(onItem(p, null, SURVEY)); assertNull(onItem(p, "FD2.w", SURVEY)); assertNull(onItem(p, "FZ9.w", SURVEY)); assertNull(onItem(p, "F-T1.len", SURVEY))
        for (st in AFTER) assertNull(onItem(p, "FD1.l", st), "$st: the refusal is said on the tap, there is no standing bar")
        assertNotNull(onItem(p, "FD1.l", null))
    }

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
