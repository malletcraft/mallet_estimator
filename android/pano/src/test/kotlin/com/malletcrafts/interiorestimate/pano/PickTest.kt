package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.MeasureFlow.Refused
import com.malletcrafts.interiorestimate.pano.Pick.Drag
import com.malletcrafts.interiorestimate.pano.Pick.Finished
import com.malletcrafts.interiorestimate.pano.Pick.Pad
import com.malletcrafts.interiorestimate.pano.Pick.State
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * PICKED, HELD, GOLD (port 63 slice I1a; DESIGN-imagemeter §3.1). The design's table of 28 rows is the specification
 * of [Pick], and this is ONE TEST PER ROW, each named by its row -- then the rules the table leans on (what may be
 * held, what may be gold, what a drag does, what the held item shows) and the three rules of the M2 screen that are
 * re-anchored here: the M2 review's guard, Move.STAY, and MeasureFlow.body's rule. The synthetic room of MeasTest
 * (L 3319, W 2678, H 2677) with MeasureFlowTest's wall. Nothing here is on screen: :app is compiled only by CI.
 *
 * (Test names are plain ASCII: MeasureWalkTest's header says why.)
 */
class PickTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0
    private fun ab(s: String): DoubleArray = SurveyPrep.size(s, L, W, H)
    private fun pt(x: Double, y: Double) = doubleArrayOf(x, y)

    private val SURVEY = PhaseTools.SURVEY
    private val STAGES: List<Int?> = listOf(0, 1, 2, 3, 4, 5, null)
    private val AFTER = listOf(1, 2, 3, 4, 5)

    private fun SurveyPrep.Prep.put(s: String, t: String, n: Int, x0: Double, x1: Double, y0: Double, y1: Double,
                                    extra: Map<String, Any?> = emptyMap()): SurveyPrep.Mark =
        SurveyPrep.Mark("id-$s-$t$n", t, SurveyPrep.CODE.getValue(s) + t + n, x0, x1, y0, y1, extra).also { surface(s).marks.add(it) }

    private val wish = mapOf(MarkLife.KEY_BORN to MarkLife.WISH, MarkLife.KEY_LIFE to MarkLife.WISH)
    private val vadd = mapOf(MarkLife.KEY_BORN to MarkLife.VADD, MarkLife.KEY_LIFE to MarkLife.VADD)

    /** MeasureFlowTest's wall: a door, an electric point and a window; a Note, a door born a wish and a tape, never
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

    /** The walk's order on that wall. */
    private val WALL = listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p", "FE1.cr", "FE1.cs", "FW1.w", "FW1.h", "FW1.r", "FW1.s", "FW1.p",
        "F-M1.len", "F-M2.per", "FK1.1", "FK1.2", "FK1.t")

    private val D1 = "id-front-D1"; private val D2 = "id-front-D2"; private val W1 = "id-front-W1"; private val W2 = "id-front-W2"
    private val E1 = "id-front-E1"

    private fun sc(p: SurveyPrep.Prep, stage: Int? = SURVEY, step: String = StepRoute.MARK) = Pick.Scene(p, stage, step, ::ab)
    private fun lock(p: SurveyPrep.Prep, id: String) { assertEquals(true, SurveyPrepEdit.setLocked(p, id, true)) }
    /** A reading that passes the gross guard for every key of the wall: each dimension as it is drawn, or 100. */
    private fun read(p: SurveyPrep.Prep, vararg keys: String) {
        for (k in keys) p.actual[k] = p.readingOf(k, ::ab)?.second?.est ?: 100.0
    }

    // ------------------------------------------------------------------ what the table leans on

    @Test fun `the state is one value - nothing picked, nothing held, nothing gold`() {
        assertEquals(State(null, false, null), Pick.NONE); assertEquals(Pick.NONE, State())
        assertEquals(450L, Pick.DOUBLE_MS); assertEquals(8.0, Pick.BODY_SLOP_DP)
    }

    @Test fun `what may be held - what the phase lets be edited, not locked, and nothing at all at Estimation or Client approved`() {
        val p = wall()
        // at Survey and unread: everything, a wish and a vendor's mark included
        for (st in listOf(SURVEY, null)) for (id in listOf(D1, W1, E1, D2, W2, "t1", "m1", "m2", "k1")) assertTrue(Pick.mayHold(sc(p, st), id), "$id at $st")
        // the Wish list: only what was born a Client wish, and the measuring tools; Vendor review and Revisions: born Vendor added
        assertEquals(listOf(false, false, true, false, true, true), listOf(D1, W1, D2, W2, "t1", "k1").map { Pick.mayHold(sc(p, 1), it) })
        for (st in listOf(2, 4)) assertEquals(listOf(false, false, true, true), listOf(D1, D2, W2, "m1").map { Pick.mayHold(sc(p, st), it) }, "$st")
        // ESTIMATION AND CLIENT APPROVED: 2 · Mark is the view -- NOTHING is held, a mark born Vendor added included,
        // which PhaseTools.editable alone would let be dragged after approval
        assertTrue(PhaseTools.editable(p, W2, PhaseTools.APPROVED), "editable's own answer at Client approved")
        for (st in listOf(PhaseTools.ESTIMATION, PhaseTools.APPROVED)) for (id in listOf(D1, W1, D2, W2, "t1", "m1", "k1")) assertFalse(Pick.mayHold(sc(p, st), id), "$id at $st")
        assertEquals(listOf(true, true, true, false, true, false, true), STAGES.map { PhaseTools.holds(it) })
        // a locked item is never held; an id that is no item never is
        lock(p, D1); lock(p, "t1")
        assertFalse(Pick.mayHold(sc(p), D1)); assertFalse(Pick.mayHold(sc(p), "t1")); assertTrue(Pick.mayHold(sc(p), W1))
        assertFalse(Pick.mayHold(sc(p), "nobody"))
        // the step does not decide who may be held: the 360 keeps its own gestures
        assertTrue(Pick.mayHold(sc(p, SURVEY, StepRoute.PANO), W1))
    }

    @Test fun `gold is 2 Mark's at Survey and while unread - never on the 360, never in Lines, never after Survey`() {
        val p = wall()
        for (st in STAGES) for (step in StepRoute.ROUTE)
            assertEquals(step == StepRoute.MARK && (st == 0 || st == null), Pick.goldOk(sc(p, st, step)), "$step at $st")
        assertFalse(Pick.goldOk(sc(p, SURVEY, StepRoute.MEASURE)), "the old third step is no step")
    }

    @Test fun `a figure turned gold - a new key, never none and never the same one`() {
        assertTrue(Pick.turnedGold(Pick.NONE, State(D1, true, "FD1.w"))); assertTrue(Pick.turnedGold(State(D1, false, "FD1.w"), State(D1, false, "FD1.h")))
        assertFalse(Pick.turnedGold(State(D1, false, "FD1.w"), State(D1, true, "FD1.w"))); assertFalse(Pick.turnedGold(State(D1, false, "FD1.w"), Pick.NONE))
    }

    @Test fun `settle - the pick goes with an item that is gone, the hold with one that may not be held, the gold where nothing may be gold`() {
        val p = wall(); val st = State(D1, true, "FD1.h")
        assertEquals(st, Pick.settle(sc(p), st))
        // the stage moved past Survey under the screen: D1 is read-only and nothing is gold
        assertEquals(State(D1, false, null), Pick.settle(sc(p, 1), st))
        // on the 360 nothing is gold; the hold is the item's own
        assertEquals(State(D1, true, null), Pick.settle(sc(p, SURVEY, StepRoute.PANO), st))
        // locked meanwhile: picked, not held, its figure still gold -- a locked mark takes readings
        lock(p, D1); assertEquals(State(D1, false, "FD1.h"), Pick.settle(sc(p), st))
        // a gold that is another item's, or a key nobody asks: let go
        assertEquals(State(W1, true, null), Pick.settle(sc(p), State(W1, true, "FD1.h"))); assertEquals(State(W1, false, null), Pick.settle(sc(p), State(W1, false, "FW1.zz")))
        // the item gone
        p.surface("front").marks.removeAll { it.id == D1 }
        assertEquals(Pick.NONE, Pick.settle(sc(p), st)); assertEquals(Pick.NONE, Pick.settle(sc(p), Pick.NONE))
    }

    // ------------------------------------------------------------------ rows 1 - 8: taps with no tool armed

    @Test fun `row 1 - a tap on the body of an item that is not the picked one picks it, holds it, and its first unread figure is gold`() {
        val p = wall()
        val o = Pick.body(sc(p), Pick.NONE, D1)
        assertEquals(State(D1, true, "FD1.w"), o.state); assertEquals(Pad.SHUT_STORE, o.pad); assertFalse(o.byItself); assertFalse(o.options)
        // from another item, picked or held, with its figure gold: the new item's first unread
        assertEquals(State(W1, true, "FW1.w"), Pick.body(sc(p), State(D1, true, "FD1.l"), W1).state)
        assertEquals(State(E1, true, "FE1.cr"), Pick.body(sc(p), State(D1, false, "FD1.h"), E1).state)
        // its first UNREAD: what is read is passed over
        read(p, "FD1.w", "FD1.h"); assertEquals(State(D1, true, "FD1.l"), Pick.body(sc(p), Pick.NONE, D1).state)
        // none when it has none left ...
        read(p, "FD1.l", "FD1.p"); assertEquals(State(D1, true, null), Pick.body(sc(p), Pick.NONE, D1).state)
        // ... when it is not in the room (a Client wish, a vendor's mark), a Note or a tape
        for (id in listOf(D2, W2, "id-front-NOTE1", "t1")) assertEquals(State(id, true, null), Pick.body(sc(p), Pick.NONE, id).state, id)
        // a Line, an area, a chain: its own reading, its first segment
        assertEquals(State("m1", true, "F-M1.len"), Pick.body(sc(p), Pick.NONE, "m1").state); assertEquals(State("m2", true, "F-M2.per"), Pick.body(sc(p), Pick.NONE, "m2").state)
        assertEquals(State("k1", true, "FK1.1"), Pick.body(sc(p), Pick.NONE, "k1").state)
        // AFTER SURVEY: picked, nothing gold -- and held only where the phase lets that item be edited
        for (st in AFTER) assertNull(Pick.body(sc(p, st), Pick.NONE, D1).state.gold, "$st")
        assertEquals(State(D1, false, null), Pick.body(sc(p, 1), Pick.NONE, D1).state); assertEquals(State(D2, true, null), Pick.body(sc(p, 1), Pick.NONE, D2).state)
        assertEquals(State(W2, true, null), Pick.body(sc(p, 2), Pick.NONE, W2).state); assertEquals(State(W2, false, null), Pick.body(sc(p, 5), Pick.NONE, W2).state)
        // a locked item: picked, never held, and it still takes readings
        lock(p, W1); assertEquals(State(W1, false, "FW1.w"), Pick.body(sc(p), Pick.NONE, W1).state)
        // an id that is no item: nobody's tap
        val st = State(D1, true, null); assertSame(st, Pick.body(sc(p), st, "nobody").state); assertEquals(Pad.KEEP, Pick.body(sc(p), st, "nobody").pad)
    }

    @Test fun `row 2 - a second tap on the picked item keeps the dimension he chose, and gives the handles back`() {
        val p = wall()
        // the walk reached D1 (no handles) and he tapped FD1.l: a tap on its body HOLDS it and the gold stays (MeasureFlow.body's rule, kept)
        assertEquals(State(D1, true, "FD1.l"), Pick.body(sc(p), State(D1, false, "FD1.l"), D1).state)
        assertEquals(State(D1, true, "FD1.p"), Pick.body(sc(p), State(D1, true, "FD1.p"), D1).state)
        // ... also when that figure is already read: it is the one he chose to correct
        read(p, "FD1.l"); assertEquals(State(D1, true, "FD1.l"), Pick.body(sc(p), State(D1, false, "FD1.l"), D1).state)
        // nothing gold on it: its first unread
        assertEquals(State(D1, true, "FD1.w"), Pick.body(sc(p), State(D1, false, null), D1).state)
        // the same answer as the rule it is re-anchored from
        for (g in listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p", null))
            assertEquals(MeasureFlow.body(p, D1, D1, g, SURVEY, ::ab).active, Pick.body(sc(p), State(D1, false, g), D1).state.gold, "$g")
        // after Survey nothing is gold, whatever was
        assertEquals(State(D1, false, null), Pick.body(sc(p, 2), State(D1, false, "FD1.l"), D1).state)
    }

    @Test fun `row 3 - a long press picks and holds as a tap on the body does, and the item's options open on the lift`() {
        val p = wall()
        val o = Pick.longPress(sc(p), Pick.NONE, W1)
        assertEquals(State(W1, true, "FW1.w"), o.state); assertTrue(o.options)
        assertEquals(Pick.body(sc(p), State(W1, false, "FW1.r"), W1).state, Pick.longPress(sc(p), State(W1, false, "FW1.r"), W1).state)
        // a mark the phase holds read-only still opens its options: that is how its sheet is reached
        Pick.longPress(sc(p, 3), Pick.NONE, D1).let { assertEquals(State(D1, false, null), it.state); assertTrue(it.options) }
        // nobody's: nothing opens
        assertFalse(Pick.longPress(sc(p), Pick.NONE, "nobody").options)
    }

    @Test fun `row 4 - a tap on a figure that takes a reading makes it gold, picks its mark, and holds it only if it already was`() {
        val p = wall()
        val o = Pick.figure(sc(p), Pick.NONE, "FW1.h")
        assertEquals(State(W1, false, "FW1.h"), o.state); assertEquals(Pad.SHUT_STORE, o.pad); assertNull(o.refused)
        // already held: the handles stay
        assertEquals(State(W1, true, "FW1.h"), Pick.figure(sc(p), State(W1, true, "FW1.w"), "FW1.h").state)
        // another mark was held: its hold does not pass to this one
        assertEquals(State(W1, false, "FW1.h"), Pick.figure(sc(p), State(D1, true, "FD1.w"), "FW1.h").state)
        // a read figure is re-opened the same way; a Line's tag, an area's, a chain's segment and its total too
        read(p, "FD1.w"); assertEquals(State(D1, false, "FD1.w"), Pick.figure(sc(p), Pick.NONE, "FD1.w").state)
        for ((k, id) in listOf("F-M1.len" to "m1", "F-M2.per" to "m2", "FK1.2" to "k1", "FK1.t" to "k1")) assertEquals(State(id, false, k), Pick.figure(sc(p), Pick.NONE, k).state, k)
        // every key the wall asks, unread or read: gold is that key
        for (k in WALL) assertEquals(k, Pick.figure(sc(p), Pick.NONE, k).state.gold, k)
        // a key nothing owns: nobody's tap
        val st = State(D1, true, "FD1.l"); assertSame(st, Pick.figure(sc(p), st, "FZ9.w").state)
    }

    @Test fun `row 5 - a tap on the gold figure changes nothing and opens the keypad`() {
        val p = wall()
        for (st in listOf(State(W1, false, "FW1.h"), State(W1, true, "FW1.h"))) {
            val o = Pick.figure(sc(p), st, "FW1.h")
            assertSame(st, o.state); assertEquals(Pad.OPEN, o.pad); assertNull(o.refused)
        }
        // on a figure that is not yet gold it is two taps: the first makes it gold and opens nothing
        assertEquals(Pad.SHUT_STORE, Pick.figure(sc(p), State(W1, false, "FW1.w"), "FW1.h").pad)
    }

    @Test fun `row 6 - a tap on a figure after Survey, or of a mark not in the room, picks its mark, makes nothing gold and says why`() {
        val p = wall(); read(p, "FD1.w")
        for (st in AFTER) {
            val o = Pick.figure(sc(p, st), State(W1, false, null), "FD1.w")
            assertEquals(State(D1, false, null), o.state); assertEquals(Refused.READ_ONLY, o.refused); assertEquals(Pad.SHUT_STORE, o.pad)
            assertNotNull(PhaseTools.readingRefusal(Tx.EN, st))
        }
        // a mark that is NOT IN THE ROOM, at Survey: asked first, in every phase; held kept if it already was, else no
        Pick.figure(sc(p), Pick.NONE, "FD2.w").let { assertEquals(State(D2, false, null), it.state); assertEquals(Refused.NOT_IN_ROOM, it.refused) }
        Pick.figure(sc(p), State(D2, true, null), "FD2.h").let { assertEquals(State(D2, true, null), it.state); assertEquals(Refused.NOT_IN_ROOM, it.refused) }
        assertEquals(Refused.NOT_IN_ROOM, Pick.figure(sc(p, 1), Pick.NONE, "FW2.w").refused)
        assertEquals("Not in the room — a mark placed after the survey has nothing to measure", MeasureText.notInRoom(Tx.EN))
        // whatever was gold is let go
        assertNull(Pick.figure(sc(p), State(W1, true, "FW1.w"), "FD2.w").state.gold)
    }

    @Test fun `row 7 - a tap on the picked item's code chip opens its options and changes nothing`() {
        val st = State(D1, false, "FD1.h")
        Pick.options(st).let { assertSame(st, it.state); assertTrue(it.options); assertEquals(Pad.KEEP, it.pad) }
        // with nothing picked there is nothing to open: the slot is grey
        assertFalse(Pick.options(Pick.NONE).options)
    }

    @Test fun `row 8 - a tap on a handle of the held item moves nothing and makes that part the nudge's`() {
        val st = State(D1, true, "FD1.w")
        for (part in listOf("lt", "r", "b", "q2")) Pick.handle(st, part).let { assertSame(st, it.state); assertEquals(part, it.nudge); assertEquals(Pad.KEEP, it.pad) }
        // not held: no handle is drawn, so there is none to tap
        assertNull(Pick.handle(State(D1, false, "FD1.w"), "lt").nudge)
    }

    // ------------------------------------------------------------------ row 9: drags -- only a held item moves

    @Test fun `row 9 - a drag of the held item changes neither the pick, the hold nor the gold`() {
        val st = State(D1, true, "FD1.w")
        Pick.dragged(st).let { assertSame(st, it.state); assertEquals(Pad.KEEP, it.pad); assertFalse(it.stay) }
    }

    @Test fun `only a held item moves - an unpicked mark, a mark the walk reached and empty photo pan - the M2 guard, kept`() {
        val p = wall()
        assertEquals(Drag.MOVE, Pick.drag(sc(p), State(D1, true, "FD1.w"), D1)); assertEquals(Drag.MOVE, Pick.drag(sc(p), State("t1", true, null), "t1"))
        // "a thumb resting on the photo while the other hand holds the laser must change nothing": the walk's mark is
        // picked WITHOUT handles, and a drag on it pans -- as a drag on any other mark and on nothing does
        assertEquals(Drag.PAN, Pick.drag(sc(p), State(D1, false, "FD1.w"), D1))
        assertEquals(Drag.PAN, Pick.drag(sc(p), State(D1, true, "FD1.w"), W1)); assertEquals(Drag.PAN, Pick.drag(sc(p), Pick.NONE, W1))
        assertEquals(Drag.PAN, Pick.drag(sc(p), State(D1, true, null), null))
        // a locked item says the lock's refusal; a mark of an earlier phase the phase's; neither moves
        lock(p, W1); assertEquals(Drag.LOCKED, Pick.drag(sc(p), State(W1, false, null), W1))
        assertEquals(Drag.READ_ONLY, Pick.drag(sc(p, 1), State(D1, false, null), D1))
        // where the phase is the view nothing is said: it pans
        for (st in listOf(3, 5)) assertEquals(Drag.PAN, Pick.drag(sc(p, st), State(W2, false, null), W2), "$st")
        // a hold the room no longer allows moves nothing
        assertEquals(Drag.LOCKED, Pick.drag(sc(p), State(W1, true, null), W1))
    }

    @Test fun `only a held item's handles take a press - the hit test is handed the held id, and a tap is the event its hit is`() {
        val p = wall()
        assertEquals(D1, Pick.handleId(State(D1, true, null))); assertNull(Pick.handleId(State(D1, false, "FD1.w"))); assertNull(Pick.handleId(Pick.NONE))
        // the door drawn 100 px wide, its top-left tab pressed: a held door's tab, a walked-to door's nothing at all
        val drawn = listOf(SurveyPrepTouch.Drawn(D1, 100.0, 200.0, 50.0, 300.0))
        fun hit(st: State) = SurveyPrepTouch.hit(drawn, Pick.handleId(st), emptyList(), 96.0, 46.0, 16.0)
        assertEquals("lt", (hit(State(D1, true, null)) as SurveyPrepTouch.Hit.Mark).part); assertNull(hit(State(D1, false, "FD1.w")))
        // tap: a figure, a handle of the held item, a body, a chain's point, the picked Tape's band, a line, nothing
        val held = State(D1, true, "FD1.w")
        assertEquals(State(W1, false, "FW1.h"), Pick.tap(sc(p), held, SurveyPrepTouch.Hit.Tag("FW1.h")).state)
        assertEquals("lt", Pick.tap(sc(p), held, SurveyPrepTouch.Hit.Mark(D1, "lt")).nudge)
        assertEquals(State(D1, true, "FD1.w"), Pick.tap(sc(p), State(D1, false, "FD1.w"), SurveyPrepTouch.Hit.Mark(D1, "lt")).state)
        assertEquals(State(W1, true, "FW1.w"), Pick.tap(sc(p), held, SurveyPrepTouch.Hit.Mark(W1, "body")).state)
        assertEquals("n1", Pick.tap(sc(p), State("k1", true, "FK1.1"), SurveyPrepTouch.Hit.Node("k1", 1)).nudge)
        assertEquals(State("k1", true, "FK1.1"), Pick.tap(sc(p), held, SurveyPrepTouch.Hit.Node("k1", 1)).state)
        assertEquals(State("t1", true, null), Pick.tap(sc(p), State("t1", false, null), SurveyPrepTouch.Hit.Body).state)
        assertEquals(State("m1", true, "F-M1.len"), Pick.tap(sc(p), held, null, line = "m1").state)
        assertEquals(Pick.NONE, Pick.tap(sc(p), held, null).state)
    }

    // ------------------------------------------------------------------ rows 10 - 13: a tap on empty photo

    @Test fun `row 10 - a tap on empty photo with an item held clears the handles, the pick and the gold`() {
        for (st in listOf(State(D1, true, "FD1.w"), State(D1, true, null), State("t1", true, null)))
            Pick.empty(st).let { assertEquals(Pick.NONE, it.state); assertEquals(Pad.SHUT_STORE, it.pad) }
    }

    @Test fun `row 11 - a tap on empty photo with a figure gold and nothing held does NOTHING - the M2 guard, kept`() {
        // "a stray thumb must not lose the dimension the laser is about to fill"
        val st = State(D1, false, "FD1.h")
        Pick.empty(st).let { assertSame(st, it.state); assertFalse(it.stay) }
        // an open keypad stores what was typed and shuts; the gold stays
        assertEquals(Pad.SHUT_STORE, Pick.empty(st).pad)
        // to let it go: tap the mark (held), then empty photo
        val p = wall()
        val held = Pick.body(sc(p), st, D1).state
        assertEquals(State(D1, true, "FD1.h"), held); assertEquals(Pick.NONE, Pick.empty(held).state)
    }

    @Test fun `row 12 - a tap on empty photo with an item picked, not held and nothing gold clears the pick`() {
        assertEquals(Pick.NONE, Pick.empty(State(D1, false, null)).state); assertEquals(Pick.NONE, Pick.empty(Pick.NONE).state)
    }

    @Test fun `row 13 - a double tap on empty photo zooms and puts back what its first tap cleared`() {
        val p = wall(); val before = State(D1, true, "FD1.w")
        // the first tap of the double tap has already cleared the held mark ...
        val first = Pick.emptied(before, 1000L); val cleared = Pick.empty(before).state
        assertEquals(Pick.NONE, cleared)
        // ... and the double tap, within 450 ms, puts it back and zooms
        Pick.doubleTap(sc(p), cleared, first, 1300L).let { assertEquals(before, it.state); assertTrue(it.zoom); assertEquals(Pad.KEEP, it.pad) }
        assertEquals(before, Pick.doubleTap(sc(p), cleared, first, 1450L).state)
        // too late, or no first tap: it zooms and leaves the state as it is
        Pick.doubleTap(sc(p), cleared, first, 1451L).let { assertEquals(Pick.NONE, it.state); assertTrue(it.zoom) }
        Pick.doubleTap(sc(p), before, null, 0L).let { assertEquals(before, it.state); assertTrue(it.zoom) }
        // a gold figure with nothing held was never cleared, and stays
        val gold = State(D1, false, "FD1.h")
        assertEquals(gold, Pick.doubleTap(sc(p), Pick.empty(gold).state, Pick.emptied(gold, 0L), 200L).state)
        // what is put back is what the room still allows
        lock(p, D1); assertEquals(State(D1, false, "FD1.w"), Pick.doubleTap(sc(p), cleared, first, 1300L).state)
    }

    // ------------------------------------------------------------------ rows 14 - 17, 24: placing and the acts

    @Test fun `row 14 - an item placed is picked and held, its first unread figure gold, and the tool goes off`() {
        val p = wall()
        val door = PhaseTools.place(p, "front", "D", 1800.0, 1000.0, W, H, "new1", SURVEY, null, java.time.Instant.EPOCH) { }!!.mark
        Pick.placed(sc(p), door.id).let { assertEquals(State("new1", true, door.code + ".w"), it.state); assertEquals(Pad.SHUT_STORE, it.pad) }
        // a Tape has no reading; a Line's is its own; an area's its perimeter
        val tape = p.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 500.0), pt(900.0, 500.0)), "t2")
        val line = p.addMeas("front", SurveyMeas.LINE, listOf(pt(0.0, 600.0), pt(900.0, 600.0)), "m3")
        assertEquals(State("t2", true, null), Pick.placed(sc(p), tape.id).state); assertEquals(State("m3", true, line.code + ".len"), Pick.placed(sc(p), line.id).state)
        // after Survey a mark is placed AS a Client wish: held, nothing gold
        val wished = PhaseTools.place(p, "front", "W", 600.0, 2400.0, W, H, "new2", 1, null, java.time.Instant.EPOCH) { }!!.mark
        assertEquals(State("new2", true, null), Pick.placed(sc(p, 1), wished.id).state)
        // THE TOOL GOES OFF: one item, then no tool armed -- until slice I3 nothing keeps one on
        for (t in listOf("D", "W", "E", SurveyPrepPalette.TAPE, SurveyPrepPalette.LINE)) assertEquals(SurveyPrepPalette.SEL, SurveyPrepPalette.afterPlaced(t), t)
    }

    @Test fun `row 15 - Duplicate picks and holds the copy, its first unread figure gold`() {
        val p = wall(); read(p, "FD1.w", "FD1.h")
        val c = SurveyPrepEdit.duplicate(p, D1, W, H, "copy1")!!
        Pick.duplicated(sc(p), c.id).let { assertEquals(State("copy1", true, c.code + ".w"), it.state) }
    }

    @Test fun `row 16 - an act that edits shape or position holds the item, and the gold is unchanged`() {
        val p = wall()
        // a mark the walk reached shows the handles the act needs
        assertEquals(State(W1, true, "FW1.h"), Pick.shaped(sc(p), State(W1, false, "FW1.h"), W1).state)
        assertEquals(State(W1, true, null), Pick.shaped(sc(p), State(W1, false, null), W1).state)
        // never a locked one, never where the phase holds it
        lock(p, D1); assertEquals(State(D1, false, "FD1.w"), Pick.shaped(sc(p), State(D1, false, "FD1.w"), D1).state)
        assertEquals(State(W1, false, null), Pick.shaped(sc(p, 3), State(W1, false, null), W1).state)
    }

    @Test fun `row 17 - Lock and Unlock leave the item picked and not held, the gold unchanged`() {
        val p = wall(); val st = State(W1, true, "FW1.w")
        lock(p, W1); assertEquals(State(W1, false, "FW1.w"), Pick.locked(sc(p), st, W1).state)
        // unlocked: picked, NOT held until it is tapped -- and one tap then holds it
        assertEquals(false, SurveyPrepEdit.setLocked(p, W1, false))
        val un = Pick.locked(sc(p), State(W1, false, "FW1.w"), W1).state
        assertEquals(State(W1, false, "FW1.w"), un); assertEquals(State(W1, true, "FW1.w"), Pick.body(sc(p), un, W1).state)
    }

    @Test fun `row 24 - deleting the picked item leaves nothing picked, held or gold - another item's delete changes nothing`() {
        Pick.deleted(State(D1, true, "FD1.w"), D1).let { assertEquals(Pick.NONE, it.state); assertEquals(Pad.SHUT_DROP, it.pad) }
        val st = State(D1, false, "FD1.w"); Pick.deleted(st, W1).let { assertSame(st, it.state); assertEquals(Pad.KEEP, it.pad) }
    }

    // ------------------------------------------------------------------ rows 18 - 22: a reading lands, the gold walks on

    @Test fun `row 18 - a reading that passes drops the handles and moves the gold - inside the mark, then along the wall`() {
        val p = wall()
        var st = State(D1, true, "FD1.w")
        // inside the mark: width, height, then position -- the checklist's order
        for ((k, next) in listOf("FD1.w" to "FD1.h", "FD1.h" to "FD1.l", "FD1.l" to "FD1.p")) {
            read(p, k)
            val o = Pick.landed(sc(p), st, k, busy = false)
            assertEquals(State(D1, false, next), o.state, k); assertTrue(o.byItself); assertNull(o.finished); assertFalse(o.waits)
            st = o.state
        }
        // then to the wall's next unread figure, whose mark becomes the picked one WITHOUT handles
        read(p, "FD1.p")
        Pick.landed(sc(p), st, "FD1.p", busy = false).let { assertEquals(State(E1, false, "FE1.cr"), it.state); assertTrue(it.byItself) }
        // the whole wall, shot by shot, in the walk's order -- the one order MeasureWalk gives
        val q = wall(); var s2 = Pick.next(sc(q), Pick.NONE, "front").state
        val order = ArrayList<String>()
        while (s2.gold != null) { val g = s2.gold!!; order += g; read(q, g); s2 = Pick.landed(sc(q), s2, g, busy = false).state; assertFalse(s2.held) }
        // ... every stop but the chain's total: with both its segments read the total is their sum, which is not LEFT
        // (MeasureWalk), so the walk does not stop on a figure that needs no shot
        assertEquals(WALL - "FK1.t", order)
    }

    @Test fun `row 18 - with nothing left the pick is unchanged, nothing is gold, and the note says the wall or the room is finished`() {
        val p = wall(); p.put("left", "W", 1, 500.0, 1500.0, 900.0, 2100.0)
        read(p, *WALL.toTypedArray())
        // the wall's last: another wall still has figures left -- "Front wall · all 16 measured ✓"
        Pick.landed(sc(p), State("k1", true, "FK1.t"), "FK1.t", busy = false).let { o ->
            assertEquals(State("k1", false, null), o.state); assertEquals(Finished.FACE, o.finished); assertFalse(o.byItself)
            assertEquals("Front wall · all 16 measured ✓", Pick.finishedText(Tx.EN, sc(p), "front", o.finished!!))
        }
        // the room's last: "Room measured · all 21 ✓"
        read(p, *ab("left").let { MeasureWalk.stops(p, "left", it[0], it[1]) }.toTypedArray())
        Pick.landed(sc(p), State("k1", false, "FK1.t"), "FK1.t", busy = false).let { o ->
            assertEquals(Finished.ROOM, o.finished); assertEquals("Room measured · all 21 ✓", Pick.finishedText(Tx.EN, sc(p), "front", o.finished!!))
        }
    }

    @Test fun `row 18 - a typed figure is deliberate - it keeps its mark of the check and the gold still moves`() {
        val p = wall(); p.actual["FD1.w"] = 2100.0
        assertTrue(MeasureFlow.flagged(p, "FD1.w", ::ab))
        assertEquals(State(D1, false, "FD1.h"), Pick.landed(sc(p), State(D1, true, "FD1.w"), "FD1.w", busy = false, typed = true).state)
    }

    @Test fun `row 19 - a reading that fails the check stays gold where it is, and the handles go`() {
        val p = wall(); p.actual["FD1.w"] = 2100.0   // the door's height shot into its width: more than twice 900
        val o = Pick.landed(sc(p), State(D1, true, "FD1.w"), "FD1.w", busy = false)
        assertEquals(State(D1, false, "FD1.w"), o.state); assertFalse(o.byItself); assertNull(o.finished); assertFalse(o.waits)
        // the next reading replaces it, and then the gold moves
        p.actual["FD1.w"] = 905.0
        assertEquals(State(D1, false, "FD1.h"), Pick.landed(sc(p), o.state, "FD1.w", busy = false).state)
        // so a double press of the meter cannot quietly put a door's width into its height
        p.actual["FD1.h"] = 905.0
        assertEquals(State(D1, false, "FD1.h"), Pick.landed(sc(p), State(D1, false, "FD1.h"), "FD1.h", busy = false).state)
    }

    @Test fun `row 20 - a reading with a sheet open or a finger on a grab is in, and the gold waits`() {
        val p = wall(); read(p, "FD1.w"); val st = State(D1, true, "FD1.w")
        // nothing changes: the picked item never changes under a sheet, a handle is never taken from under a finger
        Pick.landed(sc(p), st, "FD1.w", busy = true).let { assertSame(st, it.state); assertTrue(it.waits); assertFalse(it.byItself) }
        // the sheet closes or the finger lifts: row 18 applies
        Pick.released(sc(p), st, "FD1.w").let { assertEquals(State(D1, false, "FD1.h"), it.state); assertTrue(it.byItself) }
        // ... or row 19, when what landed fails the check
        p.actual["FD1.w"] = 300.0
        assertTrue(Pick.landed(sc(p), st, "FD1.w", busy = true).waits); assertEquals(State(D1, false, "FD1.w"), Pick.released(sc(p), st, "FD1.w").state)
        // he moved the gold himself meanwhile: nothing is moved for him
        val moved = State(D1, true, "FD1.l"); assertSame(moved, Pick.released(sc(p), moved, "FD1.w").state)
    }

    @Test fun `row 21 - the next slot with a figure gold goes to the wall's next unread figure after it, without handles`() {
        val p = wall()
        Pick.next(sc(p), State(D1, true, "FD1.w"), "front").let { assertEquals(State(D1, false, "FD1.h"), it.state); assertTrue(it.byItself); assertEquals(Pad.KEEP, it.pad) }
        // across marks: FD1.p's next is the electric point's, whose mark becomes the picked one
        assertEquals(State(E1, false, "FE1.cr"), Pick.next(sc(p), State(D1, false, "FD1.p"), "front").state)
        // the one skipped comes round again, and the only one left is its own next
        read(p, *WALL.filter { it != "FD1.h" && it != "FW1.s" }.toTypedArray())
        assertEquals("FW1.s", Pick.next(sc(p), State(D1, false, "FD1.h"), "front").state.gold); assertEquals("FD1.h", Pick.next(sc(p), State(W1, false, "FW1.s"), "front").state.gold)
        read(p, "FW1.s"); assertEquals(State(D1, false, "FD1.h"), Pick.next(sc(p), State(D1, false, "FD1.h"), "front").state)
    }

    @Test fun `row 21 - it accepts a reading that failed the check, and on the wall's last figure clears the gold and says the wall is finished`() {
        val p = wall()
        // FD1.w failed and stayed gold: the next slot accepts it and moves on
        p.actual["FD1.w"] = 2100.0
        assertEquals(State(D1, false, "FD1.h"), Pick.next(sc(p), State(D1, false, "FD1.w"), "front").state)
        // the wall's LAST unread figure failed: it is stored, so nothing is "left" -- the slot is still drawn while it is gold ...
        read(p, *WALL.toTypedArray()); p.actual["FD1.w"] = 2100.0
        assertTrue(SurveyPrepPalette.nextSlot(Tx.EN, p, "front", "FD1.w", SURVEY, StepRoute.MARK, ::ab).shown)
        // ... and then it clears the gold and the wall is finished
        Pick.next(sc(p), State(D1, false, "FD1.w"), "front").let { assertEquals(State(D1, false, null), it.state); assertEquals(Finished.ROOM, it.finished); assertFalse(it.byItself) }
    }

    @Test fun `row 22 - the next slot with nothing gold goes to the picked item's first unread figure, else the wall's first`() {
        val p = wall()
        // nothing picked: the wall's first unread, from the top
        Pick.next(sc(p), Pick.NONE, "front").let { assertEquals(State(D1, false, "FD1.w"), it.state); assertTrue(it.byItself) }
        // an item picked: ITS first unread -- and its handles go
        assertEquals(State(W1, false, "FW1.w"), Pick.next(sc(p), State(W1, true, null), "front").state)
        read(p, "FW1.w", "FW1.h"); assertEquals(State(W1, false, "FW1.r"), Pick.next(sc(p), State(W1, false, null), "front").state)
        // the picked item has nothing to read (a tape; a measured mark): the wall's first unread
        assertEquals(State(D1, false, "FD1.w"), Pick.next(sc(p), State("t1", true, null), "front").state)
        read(p, "FW1.r", "FW1.s", "FW1.p"); assertEquals(State(D1, false, "FD1.w"), Pick.next(sc(p), State(W1, true, null), "front").state)
        // nothing left anywhere on the wall (the slot is not drawn then): nothing changes
        read(p, *WALL.toTypedArray()); val st = State(W1, true, null); assertSame(st, Pick.next(sc(p), st, "front").state)
        // the Plan is no face: nothing
        assertSame(st, Pick.next(sc(wall()), st, "plan").state)
    }

    // ------------------------------------------------------------------ rows 23, 25 - 28: undo, a face, a step

    @Test fun `row 23 - undo keeps the gold while its owner is still there and picks that owner, never held`() {
        val p = wall()
        Pick.undone(sc(p), State(D1, true, "FD1.h")).let { assertEquals(State(D1, false, "FD1.h"), it.state); assertEquals(Pad.KEEP, it.pad) }
        // the undo took the gold's owner away (a placement undone): nothing is picked, and what was typed has no owner
        p.surface("front").marks.removeAll { it.id == D1 }
        Pick.undone(sc(p), State(D1, true, "FD1.h")).let { assertEquals(Pick.NONE, it.state); assertEquals(Pad.SHUT_STORE, it.pad) }
        // nothing was gold: nothing is picked after an undo, as today
        assertEquals(Pick.NONE, Pick.undone(sc(p), State(W1, true, null)).state)
    }

    @Test fun `row 25 - another face tab, another step or the Plan leaves nothing picked, held or gold`() {
        val st = State(D1, true, "FD1.w")
        for (to in listOf("right", "floor", "ceil", "plan")) Pick.tab(st, "front", to).let { assertEquals(Pick.NONE, it.state); assertFalse(it.stay); assertEquals(Pad.SHUT_STORE, it.pad) }
        // back from the Plan to a face, and the Plan's own tab: what it always was
        assertEquals(Pick.NONE, Pick.tab(st, "plan", "front").state); assertFalse(Pick.tab(Pick.NONE, "plan", "plan").stay)
        for ((from, to) in listOf("mark" to "lines", "mark" to "360", "lines" to "mark", "360" to "mark"))
            Pick.step(st, from, to, "front", PhaseTools.Ask.OPEN).let { assertEquals(Pick.NONE, it.state, "$from -> $to"); assertFalse(it.stay) }
        // NOTHING is gold on entry: he may be about to mark. The next slot or a tap on a figure starts
        assertEquals(State(D1, false, "FD1.w"), Pick.next(sc(wall()), Pick.tab(st, "right", "front").state, "front").state)
    }

    @Test fun `row 26 - the open face's own tab and the open step are no change at all - Move STAY, kept`() {
        val st = State(D1, false, "FD1.l")
        for (s in SurveyPrep.SURFACES) Pick.tab(st, s, s).let { assertSame(st, it.state); assertTrue(it.stay); assertEquals(Pad.KEEP, it.pad) }
        for (step in StepRoute.ROUTE) Pick.step(st, step, step, "front", PhaseTools.Ask.OPEN).let { assertSame(st, it.state); assertTrue(it.stay) }
        // exactly where the rule it is re-anchored from stayed: the same step and the same face, a face only
        assertEquals(MeasureFlow.Move.STAY, MeasureFlow.move(StepRoute.MEASURE, StepRoute.MEASURE, "front", "front"))
        assertEquals(MeasureFlow.Move.WALK, MeasureFlow.move(StepRoute.MEASURE, StepRoute.MEASURE, "plan", "plan"))
        assertFalse(Pick.step(st, "mark", "mark", "plan", PhaseTools.Ask.OPEN).stay)
    }

    @Test fun `row 27 - on the 360 and after Survey nothing is ever gold, whatever the event`() {
        val p = wall(); read(p, "FD1.w")
        val scenes = AFTER.map { sc(p, it) } + STAGES.map { sc(p, it, StepRoute.PANO) } + STAGES.map { sc(p, it, StepRoute.LINES) }
        val before = listOf(Pick.NONE, State(D1, false, null), State(D1, true, null), State(W1, true, null))
        for (s in scenes) for (st in before) {
            val outs = listOf(Pick.body(s, st, D1), Pick.body(s, st, W1), Pick.longPress(s, st, "m1"), Pick.figure(s, st, "FD1.h"), Pick.figure(s, st, "FD1.w"),
                Pick.figure(s, st, "F-M1.len"), Pick.placed(s, W1), Pick.duplicated(s, "k1"), Pick.shaped(s, st, D1), Pick.locked(s, st, D1),
                Pick.next(s, st, "front"), Pick.undone(s, st), Pick.doubleTap(s, st, Pick.emptied(State(D1, true, "FD1.w"), 0L), 100L),
                Pick.tap(s, st, SurveyPrepTouch.Hit.Tag("FW1.w")), Pick.tap(s, st, SurveyPrepTouch.Hit.Mark(W1, "body")))
            for (o in outs) assertNull(o.state.gold, "step ${s.step}, stage ${s.stage}, from $st")
            assertNull(Pick.settle(s, State(D1, false, "FD1.h")).gold)
            // the next slot does nothing at all there
            assertSame(st, Pick.next(s, st, "front").state)
        }
        // and a laser reading there is thrown away, never written (Landing.route)
        for (s in scenes) assertTrue(Landing.route(null, s, State(D1, false, "FD1.h"), 1203, 0L) is Landing.Route.Refused, "step ${s.step}, stage ${s.stage}")
    }

    @Test fun `row 28 - 2 Mark asked for on a face whose four lines do not close is refused in every phase, and nothing changes`() {
        val st = State(D1, true, "FD1.w")
        for (stage in STAGES) {
            assertEquals(PhaseTools.Ask.LINES_OPEN, PhaseTools.routeAsk(StepRoute.MARK, stage) { false }, "$stage")
            assertEquals(PhaseTools.Ask.OPEN, PhaseTools.routeAsk(StepRoute.MARK, stage) { true }, "$stage")
            // the step falls to 1 · Lines, never to a step that no longer exists
            assertEquals(StepRoute.LINES, PhaseTools.routeMode(StepRoute.MARK, stage) { false }, "$stage")
            assertEquals(StepRoute.LINES, PhaseTools.routeMode(StepRoute.MEASURE, stage) { false }, "$stage")
        }
        Pick.step(st, "lines", "mark", "front", PhaseTools.Ask.LINES_OPEN).let { assertSame(st, it.state); assertTrue(it.stay); assertEquals(Pad.KEEP, it.pad) }
        Pick.step(st, "mark", "360", "front", PhaseTools.Ask.HIDDEN).let { assertSame(st, it.state); assertTrue(it.stay) }
        assertEquals("These four lines do not make a closed shape yet", PhaseTools.routeRefusal(Tx.EN))
    }

    // ------------------------------------------------------------------ what a held item shows (slice I1 only)

    @Test fun `the floating trash is drawn and pressed only while the item is held, with no tool armed, in 2 Mark`() {
        val p = wall()
        assertTrue(Pick.trash(sc(p), State(D1, true, "FD1.w"), toolArmed = false)); assertTrue(Pick.handles(State(D1, true, null), D1))
        // a mark the walk reached has no delete control near the laser's thumb, and no handles
        assertFalse(Pick.trash(sc(p), State(D1, false, "FD1.w"), toolArmed = false)); assertFalse(Pick.handles(State(D1, false, "FD1.w"), D1))
        assertFalse(Pick.handles(State(D1, true, null), W1))
        assertFalse(Pick.trash(sc(p), Pick.NONE, toolArmed = false)); assertFalse(Pick.trash(sc(p), State(D1, true, null), toolArmed = true))
        assertFalse(Pick.trash(sc(p, SURVEY, StepRoute.PANO), State(D1, true, null), toolArmed = false))
        // where the phase hides the held item's delete (a mark past Existing at Client approved has none: it is never held there anyway)
        assertTrue(Pick.trash(sc(p, 1), State(D2, true, null), toolArmed = false))
    }

    @Test fun `the held item's row is what moves it - a label that opens its sheet, nudges, a padlock - no value and no Delete`() {
        val p = wall()
        Pick.row(sc(p), State(D1, true, "FD1.w"))!!.let { assertTrue(it.sheet); assertTrue(it.nudges); assertTrue(it.padlock) }
        // a Line's and an area's code opens the sheet; a Tape's and a chain's do not; none has nudges
        Pick.row(sc(p), State("m1", true, null))!!.let { assertTrue(it.sheet); assertFalse(it.nudges); assertTrue(it.padlock) }
        for (id in listOf("t1", "k1")) Pick.row(sc(p), State(id, true, null))!!.let { assertFalse(it.sheet, id); assertFalse(it.nudges, id); assertTrue(it.padlock, id) }
        // nothing held: no row at all, so the photo keeps its height while he lasers along the wall
        assertNull(Pick.row(sc(p), State(D1, false, "FD1.w"))); assertNull(Pick.row(sc(p), Pick.NONE))
        // the row has two things to say and neither is a figure or a delete: its class has no such field
        assertEquals(setOf("sheet", "nudges", "padlock"), Pick.Row::class.java.declaredFields.map { it.name }.toSet())
    }
}
