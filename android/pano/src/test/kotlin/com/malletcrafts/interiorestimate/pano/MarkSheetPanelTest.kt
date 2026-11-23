package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.MarkSheet.ActStep
import com.malletcrafts.interiorestimate.pano.MarkSheet.Outcome
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * THE PANEL THAT SLIDES UP (port 63 slice I2a; DESIGN-imagemeter §6, §13's I2 rows, §14 "I2"). Amit, 16 Nov 2026: "a
 * slide up screen in imagemeter give more options. imagemeter is gold standard when it comes to image annotation." and
 * "navigation button and D2 aer very close where d2 get accidentally fired. same for delete button for any item."
 *
 * The pure rules of the Mark sheet as a panel: its ACTS ([MarkSheet.acts], carried from the long-press menu through
 * [DeleteGuard.last]) -- Lock / Unlock · Duplicate · Fine move on line 1; the four-corner shape row (off until slice
 * Q2) · Lock all · a 24 dp gap · Delete LAST on line 2; no delete cell past Existing; what the phase forbids not drawn;
 * a Tape and a chain as sheet items; Done in the head; the fold; an act as Done followed by the act; and the 300 ms in
 * which the panel takes no tap. The synthetic room and wall of PickTest. Since slice I2b this is what :app draws
 * (app/MarkSheet.kt, read as source by MeasureScreenTest, MarkSheetTest and DeleteGuardTest): :app is compiled only by
 * CI. And from that slice: the panel stands WHOLE while a box of it has the keyboard.
 *
 * (Test names are plain ASCII: MeasureWalkTest's header says why.)
 */
class MarkSheetPanelTest {

    private val en = Tx.EN
    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0
    private fun pt(x: Double, y: Double) = doubleArrayOf(x, y)
    private val at: Instant = Instant.parse("2026-11-23T09:00:00Z")
    private val STAGES: List<Int?> = listOf(0, 1, 2, 3, 4, 5, null)

    private fun SurveyPrep.Prep.put(s: String, t: String, n: Int, x0: Double, x1: Double, y0: Double, y1: Double,
                                    extra: Map<String, Any?> = emptyMap()): SurveyPrep.Mark =
        SurveyPrep.Mark("id-$s-$t$n", t, SurveyPrep.CODE.getValue(s) + t + n, x0, x1, y0, y1, extra).also { surface(s).marks.add(it) }

    private val wish = mapOf(MarkLife.KEY_BORN to MarkLife.WISH, MarkLife.KEY_LIFE to MarkLife.WISH)
    private val vadd = mapOf(MarkLife.KEY_BORN to MarkLife.VADD, MarkLife.KEY_LIFE to MarkLife.VADD)

    /** PickTest's wall: a door, an electric point and a window; a Note, a door born a wish, a window a vendor added;
     *  a tape, a Line, an area and a chain. */
    private fun wall(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        p.put("front", "W", 1, 1500.0, 2400.0, 900.0, 2100.0)
        p.put("front", MarkTypes.NOTE, 1, 100.0, 150.0, 1200.0, 1250.0, mapOf(MarkLife.KEY_NOTE to "check the lintel"))
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

    private val D1 = "id-front-D1"; private val D2 = "id-front-D2"; private val W1 = "id-front-W1"; private val W2 = "id-front-W2"
    private val E1 = "id-front-E1"; private val N1 = "id-front-${MarkTypes.NOTE}1"
    private val ALL = listOf(D1, W1, E1, N1, D2, W2, "t1", "m1", "m2", "k1")

    private fun lock(p: SurveyPrep.Prep, id: String) { assertEquals(true, SurveyPrepEdit.setLocked(p, id, true)) }
    private fun said(a: MarkSheet.Acts) = a.line1.joinToString("|") { it.text } + " / " + a.line2.joinToString("|") { it.text }
    private fun ids(a: MarkSheet.Acts) = a.all.map { it.id }

    // ------------------------------------------------------------------ the acts

    @Test fun `the acts at Survey - Lock, Duplicate, Fine move on line 1 - Lock all, then Delete last on line 2`() {
        val p = wall()
        for (id in listOf(D1, W1, E1, N1, "t1", "m1", "m2")) {
            val a = MarkSheet.acts(en, p, id, 0, null)
            assertEquals("Lock|Duplicate|Fine move / Lock all|Delete", said(a), id)
            assertEquals(listOf(SurveyPrepEdit.LOCK, SurveyPrepEdit.DUPLICATE, MarkSheet.FINE, SurveyPrepEdit.LOCK_ALL, SurveyPrepEdit.DELETE), ids(a), id)
            assertEquals(2, a.rows); assertSame(a.line2.last(), a.delete); assertTrue(a.delete!!.delete)
            assertEquals(1, a.all.count { it.delete }, "one delete cell, and no other act is drawn as one")
        }
        assertEquals("fine", MarkSheet.FINE)
        // the cell reads "Lock all"; a screen reader is told the group, in the menu's own words -- and what Lock and Delete act on
        val door = MarkSheet.acts(en, p, D1, 0, null)
        assertEquals(listOf("Lock FD1", "Duplicate", "Fine move", "Lock all Door", "Delete FD1 Door"), door.all.map { it.aria })
        assertEquals("Lock all tapes", MarkSheet.acts(en, p, "t1", 0, null).all[3].aria); assertEquals("Delete F-T1 tape", MarkSheet.acts(en, p, "t1", 0, null).delete!!.aria)
        assertEquals("Lock all Line marks", MarkSheet.acts(en, p, "m2", 0, null).all[3].aria)
        // unread is Survey's: the same acts
        for (id in ALL) assertEquals(said(MarkSheet.acts(en, p, id, 0, null)), said(MarkSheet.acts(en, p, id, null, null)), id)
        // an id that is no item, the reference rectangle's included: no act -- it has no panel
        assertEquals(0, MarkSheet.acts(en, p, "nobody", 0, null).rows); assertSame(MarkSheet.Acts.NONE, MarkSheet.acts(en, p, SurveyRef.id("front"), 0, null))
    }

    @Test fun `a locked item reads Unlock, has no Fine move, and keeps its Delete - a group all locked reads Unlock all`() {
        val p = wall(); lock(p, D1)
        val a = MarkSheet.acts(en, p, D1, 0, null)
        // the door born a wish is a Door too, and unlocked: the group is not all locked, so the cell still reads Lock all
        assertEquals("Unlock|Duplicate / Lock all|Delete", said(a))
        assertEquals(listOf("Unlock FD1", "Duplicate", "Lock all Door", "Delete FD1 Door"), a.all.map { it.aria })
        assertFalse(MarkSheet.fineOk(p, D1, 0)); assertNotNull(a.delete, "Delete is drawn on a locked item, and answers the lock's words")
        lock(p, D2)
        val all = MarkSheet.acts(en, p, D1, 0, null)
        assertEquals("Unlock|Duplicate / Unlock all|Delete", said(all))
        assertEquals(listOf("Unlock FD1", "Duplicate", "Unlock all Door", "Delete FD1 Door"), all.all.map { it.aria })
        // a Tape, the only one on the wall: locked, its group is all locked
        lock(p, "t1"); assertEquals("Unlock|Duplicate / Unlock all|Delete", said(MarkSheet.acts(en, p, "t1", 0, null)))
        assertEquals("Unlock all tapes", MarkSheet.acts(en, p, "t1", 0, null).all[2].aria)
    }

    @Test fun `a mark past Existing has no delete cell - Drop and Restore stay STATE's moves - and a Note keeps Delete`() {
        val p = wall()
        val dropped = p.put("front", "W", 3, 100.0, 300.0, 2300.0, 2600.0, mapOf(MarkLife.KEY_BORN to MarkLife.WISH, MarkLife.KEY_LIFE to MarkLife.DROP))
        for (st in STAGES) for (id in listOf(D2, W2, dropped.id)) {
            val a = MarkSheet.acts(en, p, id, st, null)
            assertNull(a.delete, "$id at $st"); assertFalse(a.has(SurveyPrepEdit.DELETE), "$id at $st")
            assertTrue(a.all.none { it.text == "Drop" || it.text == "Restore" }, "$id at $st: ${said(a)}")
        }
        // the menu it replaces offered Drop, and Restore on the Dropped one: those are the sheet's moves now, and only there
        assertEquals("Drop", SurveyPrepEdit.menuItemsOf(en, p, D2, 0, null).last().second); assertEquals("Restore", SurveyPrepEdit.menuItemsOf(en, p, dropped.id, 0, null).last().second)
        assertEquals("Lock|Duplicate|Fine move / Lock all", said(MarkSheet.acts(en, p, D2, 0, null)))
        val moves = MarkSheet.view(en, p, MarkSheet.open(p, D2)!!, 1)!!.state!!.moves.map { it.to }
        assertTrue(MarkLife.DROP in moves, "the wish door's sheet offers Dropped at the Wish list: $moves")
        // an Existing mark and a Note are deleted: the cell is there wherever the phase shows a delete control
        for (id in listOf(D1, N1)) assertEquals("Delete", MarkSheet.acts(en, p, id, 0, null).delete!!.text, id)
        assertEquals("Delete", MarkSheet.acts(en, p, N1, 1, null).delete!!.text, "a Note is the phase's own tool up to Estimation")
    }

    @Test fun `what the phase forbids is not drawn - the menu's own cuts, with no Cancel, and Fine move only where the item may be held`() {
        val p = wall(); lock(p, E1)
        for (st in STAGES) for (picked in listOf(null, MarkLife.EXIST, MarkLife.WISH)) for (id in ALL) for (shape in listOf(false, true)) {
            val what = "$id at $st, as $picked, shape $shape"
            val menu = SurveyPrepEdit.menuItemsOf(en, p, id, st, picked, shape = shape)
            val a = MarkSheet.acts(en, p, id, st, picked, shape = shape)
            val past = p.markById(id)?.second?.let { MarkLife.deleteWord(it) != MarkLife.DeleteWord.DELETE } == true
            val want = menu.map { it.first }.filter { it != SurveyPrepEdit.CANCEL && !(past && it == SurveyPrepEdit.DELETE) }.toSet() +
                (if (MarkSheet.fineOk(p, id, st)) setOf(MarkSheet.FINE) else emptySet())
            assertEquals(want, ids(a).toSet(), what); assertEquals(ids(a).size, ids(a).toSet().size, what)
            // each line in its own order, and Delete the last of all
            assertEquals(a.line1.map { it.id }, listOf(SurveyPrepEdit.LOCK, SurveyPrepEdit.DUPLICATE, MarkSheet.FINE).filter { a.has(it) }, what)
            val shapes = setOf(SurveyPrepEdit.SHAPE_FREE, SurveyPrepEdit.SHAPE_BOX)
            assertEquals(a.line2.map { it.id }, a.line2.map { it.id }.sortedBy { if (it in shapes) 0 else if (it == SurveyPrepEdit.LOCK_ALL) 1 else 2 }, what)
            if (a.has(SurveyPrepEdit.DELETE)) { assertEquals(SurveyPrepEdit.DELETE, ids(a).last(), what); assertEquals(2, a.rows, what); assertTrue(a.line1.isNotEmpty(), "$what: line 1 stands between Done and Delete") }
            assertFalse(a.has(SurveyPrepEdit.CANCEL), what)
            // Fine move is exactly what may be held, less a chain
            assertEquals(Pick.mayHold(Pick.Scene(p, st, StepRoute.MARK) { s -> SurveyPrep.size(s, L, W, H) }, id) && p.chainById(id) == null, a.has(MarkSheet.FINE), what)
            if (!shape) assertTrue(a.all.none { it.id in shapes }, what)
        }
        // by hand, the phases after Survey: a surveyed door has no act at all (its menu was its head and Cancel) ...
        for (st in listOf(1, 2, 3, 4, 5)) { assertEquals(0, MarkSheet.acts(en, p, D1, st, null).rows, "at $st"); assertEquals(" / ", said(MarkSheet.acts(en, p, D1, st, null))) }
        // ... a door born a wish, at the Wish list: locked, moved, never copied, never deleted
        assertEquals("Lock|Fine move / Lock all", said(MarkSheet.acts(en, p, D2, 1, null)))
        // ... a vendor's window: at Vendor review and Revisions the same; at Client approved nothing moves
        for (st in listOf(2, 4)) assertEquals("Lock|Fine move / Lock all", said(MarkSheet.acts(en, p, W2, st, null)), "at $st")
        assertEquals("Lock / Lock all", said(MarkSheet.acts(en, p, W2, 5, null)))
        // ... Estimation changes nothing: no act for anything
        for (id in ALL) assertEquals(0, MarkSheet.acts(en, p, id, 3, null).rows, id)
        // ... the measuring tools stay the phase's own outside Estimation
        assertEquals("Lock|Duplicate|Fine move / Lock all|Delete", said(MarkSheet.acts(en, p, "t1", 1, null)))
        assertEquals("Lock|Duplicate / Lock all|Delete", said(MarkSheet.acts(en, p, "t1", 5, null)), "nothing is held at Client approved")
    }

    @Test fun `the four-corner shape row is OFF until slice Q2 turns the switch - then it is line 2's first, on a mark that may have four corners`() {
        val p = wall()
        // OFF by default: at every stage, for every item, no shape cell
        for (st in STAGES) for (id in ALL) assertTrue(MarkSheet.acts(en, p, id, st, null).all.none { it.id == SurveyPrepEdit.SHAPE_FREE || it.id == SurveyPrepEdit.SHAPE_BOX }, "$id at $st")
        // the switch on: a window's line 2 begins with it; a point, a Note, a tape and a chain have none
        assertEquals("Lock|Duplicate|Fine move / Four corners|Lock all|Delete", said(MarkSheet.acts(en, p, W1, 0, null, shape = true)))
        assertEquals(SurveyPrepEdit.SHAPE_FREE, MarkSheet.acts(en, p, W1, 0, null, shape = true).line2.first().id)
        for (id in listOf(E1, N1, "t1", "m1", "k1")) assertEquals(said(MarkSheet.acts(en, p, id, 0, null)), said(MarkSheet.acts(en, p, id, 0, null, shape = true)), id)
        // a mark that carries four corners reads Rectangle
        val w = p.markById(W1)!!.second
        assertTrue(Quad.set(w, listOf(pt(1500.0, 1200.0), pt(2400.0, 900.0), pt(2400.0, 2100.0), pt(1500.0, 2100.0))))
        val shaped = MarkSheet.acts(en, p, W1, 0, null, shape = true)
        assertEquals("Lock|Duplicate|Fine move / Rectangle|Lock all|Delete", said(shaped)); assertEquals(SurveyPrepEdit.SHAPE_BOX, shaped.line2.first().id)
        // a mark past Existing with the switch on: the shape row and Lock all, and still no delete cell
        assertEquals("Lock|Fine move / Four corners|Lock all", said(MarkSheet.acts(en, p, D2, 1, null, shape = true)))
    }

    // ------------------------------------------------------------------ a Tape and a chain

    @Test fun `a Tape and a chain get the panel - the head and the acts, and no block`() {
        val p = wall()
        // every item opens the panel; the sheet as it stands still opens for neither a Tape nor a chain
        for (id in ALL) assertTrue(MarkSheet.panelOpens(p, id), id)
        assertFalse(MarkSheet.panelOpens(p, "nobody")); assertFalse(MarkSheet.panelOpens(p, SurveyRef.id("front")))
        assertFalse(MarkSheet.opens(p, "t1")); assertFalse(MarkSheet.opens(p, "k1")); assertNull(MarkSheet.open(p, "t1")); assertNull(MarkSheet.itemOf(p, "k1"))
        assertNull(MarkSheet.openPanel(p, "nobody", 0L))
        // the Tape
        val tp = assertNotNull(MarkSheet.openPanel(p, "t1", 1_000L))
        assertNull(tp.draft); assertEquals(1_000L, tp.openedAt); assertFalse(tp.stateFirst)
        val tv = assertNotNull(MarkSheet.panel(en, p, tp, 0, null))
        assertEquals(listOf("F-T1", "Tape", "Done"), listOf(tv.head.code, tv.head.name, tv.head.done)); assertNull(tv.head.trade); assertTrue(tv.head.doneEnabled)
        assertNull(tv.body, "no KIND, TRADE, STATE or NOTE"); assertEquals("Options for F-T1", tv.pane)
        assertEquals("Lock|Duplicate|Fine move / Lock all|Delete", said(tv.acts))
        // the chain: no Duplicate while the chain tool is off, and no Fine move -- nothing moves a chain whole
        val kp = assertNotNull(MarkSheet.openPanel(p, "k1", 0L))
        val kv = assertNotNull(MarkSheet.panel(en, p, kp, 0, null))
        assertEquals(listOf("FK1", "Chain"), listOf(kv.head.code, kv.head.name)); assertNull(kv.head.trade); assertNull(kv.body)
        assertEquals("Lock / Lock all|Delete", said(kv.acts)); assertEquals(2, kv.acts.rows)
        assertFalse(Features.CHAINS, "the chain tool is off")
        assertEquals("Lock|Duplicate / Lock all|Delete", said(MarkSheet.acts(en, p, "k1", 0, null, chains = true)), "the switch comes through the menu's own rule")
        assertFalse(MarkSheet.fineOk(p, "k1", 0)); assertTrue(MarkSheet.fineOk(p, "t1", 0))
        // Done has nothing to write and closes; cancel has nothing to lose; neither takes an undo step
        var steps = 0
        assertEquals(Outcome.UNCHANGED, MarkSheet.panelDone(p, tp, 0, at) { steps++ }.outcome); assertEquals(0, steps)
        assertFalse(MarkSheet.panelCancel(p, tp))
        // its item gone: the panel closes
        assertTrue(p.surface("front").meas.removeIf { it.id == "t1" })
        assertNull(MarkSheet.panel(en, p, tp, 0, null)); assertEquals(Outcome.GONE, MarkSheet.panelDone(p, tp, 0, at) { steps++ }.outcome)
    }

    // ------------------------------------------------------------------ the head, and the sheet's own blocks under it

    @Test fun `Done is in the head - the panel's head is the sheet's own, and its blocks are the sheet's as they are`() {
        val p = wall()
        for (st in STAGES) for (id in listOf(D1, W1, E1, N1, D2, W2, "m1", "m2")) {
            val pn = assertNotNull(MarkSheet.openPanel(p, id, 0L), id)
            val d = assertNotNull(pn.draft, id)
            assertEquals(MarkSheet.open(p, id), d, "$id: the draft is the sheet's own state as it opens")
            val pv = assertNotNull(MarkSheet.panel(en, p, pn, st, null), "$id at $st")
            val v = assertNotNull(MarkSheet.view(en, p, d, st))
            val body = assertNotNull(pv.body)
            assertEquals(listOf(v.code, v.name, v.done), listOf(pv.head.code, pv.head.name, pv.head.done), "$id at $st")
            assertEquals("Done", pv.head.done); assertEquals(v.doneEnabled, pv.head.doneEnabled)
            assertEquals(v.trade.text, pv.head.trade!!.text); assertEquals(v.trade.colour, pv.head.trade!!.colour)
            assertEquals(v.kind?.chips?.map { it.text }, body.kind?.chips?.map { it.text }); assertEquals(v.state?.moves?.map { it.to }, body.state?.moves?.map { it.to })
            assertEquals(v.note.text, body.note.text); assertEquals(v.tradeBlock?.label, body.tradeBlock?.label)
            assertEquals("Options for ${v.code}", pv.pane); assertEquals(MarkSheet.optionsLabel(en, v.code), pv.pane)
            assertEquals(said(MarkSheet.acts(en, p, id, st, null)), said(pv.acts)); assertNull(pv.said)
        }
        assertEquals(listOf("FD1", "Door"), MarkSheet.panel(en, p, MarkSheet.openPanel(p, D1, 0L)!!, 0, null)!!.head.let { listOf(it.code, it.name) })
        // Done's lower edge is in the head: 56 dp down in portrait, 36 in landscape -- above every rest the panel has
        assertEquals(56, MarkSheet.doneBottomDp(false)); assertEquals(36, MarkSheet.doneBottomDp(true))
        // a tap or a keystroke in a block replaces the draft, through the sheet's own pure functions
        val pn = MarkSheet.openPanel(p, D1, 0L)!!
        val typed = pn.with { MarkSheet.typeNote(it, "hinge side") }
        assertEquals("hinge side", typed.draft!!.note); assertEquals("hinge side", MarkSheet.panel(en, p, typed, 0, null)!!.body!!.note.text)
        assertEquals(pn.copy(draft = MarkSheet.typeNote(pn.draft!!, "hinge side")), typed)
        // Done writes it, in ONE undo step, exactly as the sheet's Done does
        var steps = 0
        val done = MarkSheet.panelDone(p, typed, 0, at) { steps++ }
        assertEquals(Outcome.DONE, done.outcome); assertEquals(1, steps); assertEquals("hinge side", MarkLife.noteOf(p.markById(D1)!!.second.extra))
        // a new Note with no words: cancel removes it, as the sheet's cancel does
        val q = wall(); val n = q.put("front", MarkTypes.NOTE, 2, 600.0, 650.0, 600.0, 650.0)
        val fresh = MarkSheet.openPanel(q, n.id, 0L, fresh = true, hist = 3)!!
        assertTrue(fresh.draft!!.fresh); assertTrue(MarkSheet.panelCancel(q, fresh)); assertNull(q.markById(n.id))
    }

    @Test fun `a long press on a mark past Existing opens the panel with STATE in view - so does a Drop's own opening`() {
        val p = wall()
        assertTrue(MarkSheet.openPanel(p, D2, 0L, longPress = true)!!.stateFirst, "a wish: its Drop is STATE's")
        assertTrue(MarkSheet.openPanel(p, W2, 0L, longPress = true)!!.stateFirst)
        // a tap on its chip: as the sheet opens today; an Existing mark, a Note, a Line and a Tape: nothing to bring into view
        assertFalse(MarkSheet.openPanel(p, D2, 0L)!!.stateFirst)
        for (id in listOf(D1, E1, N1, "m1", "t1", "k1")) assertFalse(MarkSheet.openPanel(p, id, 0L, longPress = true)!!.stateFirst, id)
        // opened on a move's Why box (a Drop asked for): STATE is in view, and the sheet's own focus is kept
        val drop = MarkSheet.openPanel(p, D2, 0L, focusMove = MarkLife.DROP)!!
        assertTrue(drop.stateFirst); assertEquals(MarkLife.DROP, drop.draft!!.focusMove)
        assertTrue(MarkSheet.panel(en, p, drop, 1, null)!!.stateFirst); assertEquals(MarkLife.DROP, MarkSheet.panel(en, p, drop, 1, null)!!.body!!.focusWhy)
    }

    // ------------------------------------------------------------------ the first 300 ms

    @Test fun `the panel takes no tap for its first 300 ms after it opens`() {
        assertEquals(300L, MarkSheet.DEAF_MS)
        val p = wall()
        for (how in listOf(true, false)) {
            val pn = MarkSheet.openPanel(p, D1, 10_000L, longPress = how)!!
            for (t in listOf(10_000L, 10_001L, 10_150L, 10_299L)) assertFalse(MarkSheet.answers(pn, t), "long press $how, at $t")
            for (t in listOf(10_300L, 10_301L, 12_000L)) assertTrue(MarkSheet.answers(pn, t), "long press $how, at $t")
            // a clock that ran backwards never leaves the panel deaf
            assertTrue(MarkSheet.answers(pn, 9_000L))
        }
        // typing in it does not start the wait again: the panel is the same panel
        val pn = MarkSheet.openPanel(p, D1, 10_000L)!!
        assertEquals(10_000L, pn.with { MarkSheet.typeNote(it, "x") }.openedAt)
    }

    // ------------------------------------------------------------------ an act picked

    @Test fun `an act is Done followed by the act, in ONE undo step - what was typed is kept`() {
        val p = wall()
        val hist = ArrayList<SurveyPrep.Prep>()
        fun snapshot() { hist += p.copy() }
        val pn = MarkSheet.openPanel(p, D1, 0L)!!.with { MarkSheet.typeNote(it, "hinge side") }
        val step = MarkSheet.actStep(en, p, pn, SurveyPrepEdit.LOCK, 0, null)
        assertTrue(step is ActStep.Run && step.act == SurveyPrepEdit.LOCK)
        val before = hist.size
        val done = MarkSheet.panelDone(p, pn, 0, at, ::snapshot)
        val afterDone = hist.size
        assertEquals(Outcome.DONE, done.outcome); assertNull(done.state, "the panel closes"); assertTrue(MarkSheet.actRuns(done.outcome)); assertEquals(1, afterDone)
        // the act, as the screen's own Lock does it: its step, then the lock
        snapshot(); assertEquals(true, SurveyPrepEdit.setLocked(p, D1, null))
        assertEquals(2, hist.size)
        val drop = MarkSheet.stepsToDrop(before, afterDone, hist.size)
        assertEquals(1, drop); repeat(drop) { hist.removeAt(hist.size - 1) }
        assertEquals(1, hist.size, "Done's step and the act's are ONE")
        // now: the note written AND locked; one undo: neither
        val now = p.markById(D1)!!.second
        assertEquals("hinge side", MarkLife.noteOf(now.extra)); assertTrue(SurveyPrep.isLocked(now))
        val back = SurveyPrepEdit.undoTo(p, hist.last()).markById(D1)!!.second
        assertEquals("", MarkLife.noteOf(back.extra)); assertFalse(SurveyPrep.isLocked(back))
        // which steps are dropped: only the act's own, and only when BOTH took one
        assertEquals(0, MarkSheet.stepsToDrop(4, 4, 5), "nothing was typed: the act's step is the one step")
        assertEquals(0, MarkSheet.stepsToDrop(4, 5, 5), "the act took none (refused, or it had nothing to do): Done's is the one step")
        assertEquals(0, MarkSheet.stepsToDrop(4, 4, 4)); assertEquals(1, MarkSheet.stepsToDrop(4, 5, 6)); assertEquals(2, MarkSheet.stepsToDrop(4, 5, 7))
        // every act but Delete is Done first
        for (act in listOf(SurveyPrepEdit.LOCK, SurveyPrepEdit.DUPLICATE, MarkSheet.FINE, SurveyPrepEdit.LOCK_ALL)) {
            val s = MarkSheet.actStep(en, wall(), MarkSheet.openPanel(wall(), W1, 0L)!!, act, 0, null)
            assertTrue(s is ActStep.Run && s.act == act, act)
        }
        val shaped = MarkSheet.actStep(en, wall(), MarkSheet.openPanel(wall(), W1, 0L)!!, SurveyPrepEdit.SHAPE_FREE, 0, null, shape = true)
        assertTrue(shaped is ActStep.Run && shaped.act == SurveyPrepEdit.SHAPE_FREE)
    }

    @Test fun `the act does not run when Done leaves the sheet open, nor when the item went with it`() {
        assertEquals(listOf(true, true, false, false, false, false),
            listOf(Outcome.DONE, Outcome.UNCHANGED, Outcome.STAYS, Outcome.NEEDS_WORDS, Outcome.REMOVED, Outcome.GONE).map { MarkSheet.actRuns(it) })
        assertEquals(6, Outcome.entries.size)
        // a Note whose words were emptied: Done says so and stays, and the Lock that was picked does nothing
        val p = wall()
        val pn = MarkSheet.openPanel(p, N1, 0L)!!.with { MarkSheet.typeNote(it, "  ") }
        assertTrue(MarkSheet.actStep(en, p, pn, SurveyPrepEdit.LOCK, 0, null) is ActStep.Run)
        var steps = 0
        val done = MarkSheet.panelDone(p, pn, 0, at) { steps++ }
        assertEquals(Outcome.NEEDS_WORDS, done.outcome); assertNotNull(done.state, "the panel stays open"); assertFalse(MarkSheet.actRuns(done.outcome)); assertEquals(0, steps)
        assertFalse(SurveyPrep.isLocked(p.markById(N1)!!.second))
    }

    @Test fun `Delete drops the draft and deletes - and on a locked item it answers the lock's words and the panel stays, with what was typed`() {
        val p = wall()
        val pn = MarkSheet.openPanel(p, D1, 0L)!!.with { MarkSheet.typeNote(it, "never written") }
        assertSame(ActStep.Delete, MarkSheet.actStep(en, p, pn, SurveyPrepEdit.DELETE, 0, null))
        assertFalse(MarkSheet.panelCancel(p, pn)); assertEquals("", MarkLife.noteOf(p.markById(D1)!!.second.extra), "the draft is dropped, not written")
        assertTrue(p.deleteMark(D1)); assertNull(MarkSheet.panel(en, p, pn, 0, null), "its item gone, the panel closes")
        assertEquals("FD1 deleted", SurveyPrepEdit.deleted(en, "FD1"))
        // locked: refused IN THE PANEL, nothing changed, the draft as typed
        val q = wall(); lock(q, W1)
        val qn = MarkSheet.openPanel(q, W1, 0L)!!.with { MarkSheet.typeNote(it, "keep me") }
        val no = MarkSheet.actStep(en, q, qn, SurveyPrepEdit.DELETE, 0, null)
        assertTrue(no is ActStep.Refused)
        val stays = (no as ActStep.Refused).panel
        assertEquals("FW1 is locked — unlock first", stays.said); assertEquals("keep me", stays.draft!!.note); assertEquals(qn.copy(said = stays.said), stays)
        assertNotNull(q.markById(W1)); assertEquals("FW1 is locked — unlock first", MarkSheet.panel(en, q, stays, 0, null)!!.said)
        // the next tap or keystroke in the panel takes the words away
        assertNull(stays.with { MarkSheet.typeNote(it, "keep me too") }.said)
        // the shape row is drawn on a locked mark too (slice Q2's switch on), and answers the same words
        val sh = MarkSheet.actStep(en, q, qn, SurveyPrepEdit.SHAPE_FREE, 0, null, shape = true)
        assertEquals("FW1 is locked — unlock first", (sh as ActStep.Refused).panel.said)
        // Duplicate is not refused by the lock: the copy is a new, unlocked thing
        assertTrue(MarkSheet.actStep(en, q, qn, SurveyPrepEdit.DUPLICATE, 0, null) is ActStep.Run)
        // a locked Tape the same
        lock(q, "t1")
        val t = MarkSheet.actStep(en, q, MarkSheet.openPanel(q, "t1", 0L)!!, SurveyPrepEdit.DELETE, 0, null)
        assertEquals("F-T1 is locked — unlock first", (t as ActStep.Refused).panel.said)
    }

    @Test fun `a refused act changes nothing and says why in the panel - Unlock in Scope, Lock all outside Survey, an act that is not drawn`() {
        val p = wall()
        // a wish taken into Scope is locked by its state: Unlock answers the Scope's words
        val scope = p.put("front", "W", 3, 100.0, 300.0, 2300.0, 2600.0,
            mapOf(MarkLife.KEY_BORN to MarkLife.WISH, MarkLife.KEY_LIFE to MarkLife.SCOPE, SurveyPrep.LOCKED to true))
        val sn = MarkSheet.openPanel(p, scope.id, 0L)!!
        assertTrue(MarkSheet.acts(en, p, scope.id, 1, null).has(SurveyPrepEdit.LOCK))
        val un = MarkSheet.actStep(en, p, sn, SurveyPrepEdit.LOCK, 1, null)
        assertEquals(SurveyPrepEdit.unlockRefusal(en, p, scope.id), (un as ActStep.Refused).panel.said); assertNotNull(un.panel.said)
        assertTrue(un.panel.said!!.contains("FW3"), un.panel.said)
        // Lock all is a Survey tool: where the phase still draws it (a wish at the Wish list), it says so and locks nothing
        val dn = MarkSheet.openPanel(p, D2, 0L)!!
        val la = MarkSheet.actStep(en, p, dn, SurveyPrepEdit.LOCK_ALL, 1, null)
        assertEquals(PhaseTools.refuseBulk(en, 1), (la as ActStep.Refused).panel.said)
        assertTrue(MarkSheet.actStep(en, p, dn, SurveyPrepEdit.LOCK_ALL, 0, null) is ActStep.Run, "at Survey it runs")
        // an act the panel does not draw -- the stage moved under it, a stale tap: nothing, and nothing said
        for ((id, act, st) in listOf(Triple(D1, SurveyPrepEdit.LOCK, 1), Triple(D2, SurveyPrepEdit.DELETE, 0), Triple(D2, SurveyPrepEdit.DUPLICATE, 1),
            Triple(W1, SurveyPrepEdit.SHAPE_FREE, 0), Triple("k1", MarkSheet.FINE, 0), Triple(D1, SurveyPrepEdit.CANCEL, 0), Triple(D1, "nonsense", 0))) {
            val pn = MarkSheet.openPanel(p, id, 0L)!!
            val r = MarkSheet.actStep(en, p, pn, act, st, null)
            assertTrue(r is ActStep.Refused, "$act on $id at $st"); assertSame(pn, (r as ActStep.Refused).panel)
        }
        // an item that is gone
        val gone = MarkSheet.openPanel(p, E1, 0L)!!; assertTrue(p.deleteMark(E1))
        assertSame(gone, (MarkSheet.actStep(en, p, gone, SurveyPrepEdit.LOCK, 0, null) as ActStep.Refused).panel)
    }

    // ------------------------------------------------------------------ where it rests, and the fold

    @Test fun `at the half rest in portrait the grip, the head with Done and both lines of acts are above the fold`() {
        assertEquals(listOf(16, 40, 44, 36, 40), listOf(MarkSheet.GRIP_DP, MarkSheet.HEAD_DP, MarkSheet.ACT_ROW_DP, MarkSheet.HEAD_LAND_DP, MarkSheet.ACT_ROW_LAND_DP))
        // the Mi A3: a 756 dp window, the half rest at 378
        assertEquals(378.0, MarkSheet.halfRestDp(756.0))
        val p = wall()
        val two = MarkSheet.acts(en, p, D1, 0, null)
        // 16 + 40 + 88 is the design's own sum (§6: 144); since the review of I2 (finding 4) the line that holds Delete
        // stands 8 dp further down, 16 dp under the cells over it: 152
        assertEquals(152, MarkSheet.foldTopDp(two), "16 + 40 + 96")
        assertEquals(172, MarkSheet.foldTopDp(two, said = true))
        assertEquals(96, MarkSheet.actsDp(two)); assertEquals(8, MarkSheet.deleteDropDp(two)); assertEquals(0, MarkSheet.deleteDropDp(MarkSheet.Acts.NONE))
        assertEquals(listOf(36, 32, 32), listOf(MarkSheet.ACT_CELL_DP, MarkSheet.ACT_CELL_LAND_DP, MarkSheet.DONE_DP))
        var most = 0
        for (st in STAGES) for (id in ALL) for (shape in listOf(false, true)) for (said in listOf(false, true)) {
            val a = MarkSheet.acts(en, p, id, st, null, shape = shape)
            val top = MarkSheet.foldTopDp(a, said = said); most = maxOf(most, top)
            assertEquals(16 + 40 + 44 * a.rows + (if (a.delete != null) 8 else 0) + (if (said) 20 else 0), top)
            // ... on the Mi A3, and on a window as short as 480 dp
            for (win in listOf(756.0, 640.0, 480.0)) assertTrue(top <= MarkSheet.halfRestDp(win), "$id at $st: $top dp of grip, head and acts under a half rest of ${MarkSheet.halfRestDp(win)}")
            assertTrue(MarkSheet.doneBottomDp(false) <= top)
        }
        assertEquals(172, most)
        // it opens at the half rest when its rows need more than that, else at its own height; whole in landscape
        assertEquals(378.0, MarkSheet.opensAtDp(756.0, 520.0, land = false)); assertEquals(232.0, MarkSheet.opensAtDp(756.0, 232.0, land = false))
        assertEquals(300.0, MarkSheet.opensAtDp(336.0, 300.0, land = true)); assertEquals(336.0, MarkSheet.opensAtDp(336.0, 400.0, land = true))
        assertEquals(listOf(false, true), listOf(false, true).map { MarkSheet.opensWhole(it) })
        assertEquals(listOf(true, false), listOf(false, true).map { MarkSheet.hasGrip(it) })
        // THE HALF REST IS A CAP ON THE PANEL'S OWN CONTENT (the review of I2, finding 1): the grip and the capped content
        // are the half rest together; no cap in landscape, none once the panel is whole; and under every cap the head
        // with Done, both lines of acts and a refused act's line still fit, so only the blocks ever scroll
        assertEquals(362.0, MarkSheet.contentCapDp(756.0, land = false, whole = false)); assertEquals(378.0, MarkSheet.GRIP_DP + MarkSheet.contentCapDp(756.0, false, false)!!)
        assertNull(MarkSheet.contentCapDp(756.0, land = false, whole = true)); assertNull(MarkSheet.contentCapDp(336.0, land = true, whole = false)); assertNull(MarkSheet.contentCapDp(336.0, land = true, whole = true))
        for (win in listOf(756.0, 640.0, 480.0)) assertTrue(most - MarkSheet.GRIP_DP <= MarkSheet.contentCapDp(win, false, false)!!, "$win")
        assertTrue(MarkSheet.SKIPS_LIBRARY_HALF, "the library's sheet is given no half anchor to fall back to")
        // landscape: no grip, ONE line of acts of 40 dp under a 36 dp head
        assertEquals(76, MarkSheet.foldTopDp(two, land = true)); assertEquals(36, MarkSheet.foldTopDp(MarkSheet.Acts.NONE, land = true))
        assertEquals(56, MarkSheet.foldTopDp(MarkSheet.Acts.NONE))
    }

    @Test fun `in portrait Delete is last, at the right end, 24 dp from its neighbour and a whole act row below Done`() {
        assertEquals(24.0, MarkSheet.DELETE_GAP_DP); assertTrue(MarkSheet.DELETE_GAP_DP > DeleteGuard.CLEAR_DP)
        val p = wall()
        val width = 336.0   // a 360 dp sheet, 12 dp in at each side
        val col = (336.0 - 16) / 3
        for (shape in listOf(false, true)) {
            val a = MarkSheet.acts(en, p, W1, 0, null, shape = shape)
            val s = MarkSheet.slots(a, width, land = false)
            assertEquals(a.all.map { it.id }, s.map { it.cell.id })
            val l1 = s.filter { it.row == 0 }; val l2 = s.filter { it.row == 1 }
            assertEquals(listOf("Lock", "Duplicate", "Fine move"), l1.map { it.cell.text })
            // line 1: three equal columns, 8 dp apart, edge to edge
            assertEquals(0.0, l1[0].l); assertEquals(336.0, l1[2].r, 1e-9)
            for (x in l1) assertEquals(col, x.r - x.l, 1e-9)
            assertEquals(8.0, l1[1].l - l1[0].r, 1e-9); assertEquals(8.0, l1[2].l - l1[1].r, 1e-9)
            // line 2: Delete is its last cell, its right edge the panel's, 24 dp from the column beside it
            val del = l2.last(); assertTrue(del.cell.delete); assertEquals(336.0, del.r, 1e-9)
            assertEquals(2 * (col + 8) + 16, del.l, 1e-9); assertEquals(24.0, del.l - (2 * col + 8), 1e-9)
            for (o in l2.dropLast(1)) assertTrue(del.l - o.r >= 24.0 - 1e-9, "Delete stands ${del.l - o.r} dp from ${o.cell.text}")
            assertEquals(if (shape) listOf("Four corners", "Lock all", "Delete") else listOf("Lock all", "Delete"), l2.map { it.cell.text })
            // with no shape row Lock all is in the first column and the middle one is empty: Delete does not move in
            if (!shape) assertEquals(0.0, l2[0].l)
        }
        // Delete is on the SECOND row whatever line 1 holds -- a whole act row (44 dp) under the head, where Done is
        for (st in STAGES) for (id in ALL) {
            val a = MarkSheet.acts(en, p, id, st, null)
            val s = MarkSheet.slots(a, width, land = false)
            val del = s.firstOrNull { it.cell.delete } ?: continue
            assertEquals(1, del.row, "$id at $st"); assertSame(s.last(), del); assertEquals(336.0, del.r, 1e-9)
            assertTrue(s.filter { it.row == 1 && it !== del }.all { del.l - it.r >= 24.0 - 1e-9 }, "$id at $st")
            assertTrue(MarkSheet.doneBottomDp(false) + MarkSheet.ACT_ROW_DP <= MarkSheet.GRIP_DP + MarkSheet.HEAD_DP + MarkSheet.ACT_ROW_DP * del.row)
            // EVERY CELL HAS ITS TOP AND BOTTOM TOO (the review of I2, finding 4): 36 dp high; line 1 at 4..40; the line
            // that holds Delete 16 dp under those cells, at 56..92 -- not at 48, 8 dp under `Fine move`
            for (x in s) assertEquals(36.0, x.b - x.t, 1e-9, "$id at $st: ${x.cell.text}")
            for (x in s.filter { it.row == 0 }) { assertEquals(4.0, x.t, 1e-9); assertEquals(40.0, x.b, 1e-9) }
            for (x in s.filter { it.row == 1 }) { assertEquals(56.0, x.t, 1e-9, "$id at $st: ${x.cell.text}"); assertEquals(92.0, x.b, 1e-9) }
            assertEquals(96, MarkSheet.actsDp(a), "$id at $st"); assertTrue(s.all { it.b <= MarkSheet.actsDp(a) })
        }
        // no two cells of a row overlap, and none leaves the panel
        for (st in STAGES) for (id in ALL) for (shape in listOf(false, true)) {
            val s = MarkSheet.slots(MarkSheet.acts(en, p, id, st, null, shape = shape), width, land = false)
            for (x in s) assertTrue(x.l >= 0 && x.r <= width + 1e-9 && x.r - x.l >= 80, "$id at $st: ${x.cell.text} ${x.l}..${x.r}")
            for (r in 0..1) s.filter { it.row == r }.zipWithNext { a, b -> assertTrue(b.l - a.r >= 8.0 - 1e-9, "$id at $st") }
        }
        // one line only (a vendor's window at Client approved: Lock, and Lock all): both on rows 0 and 1, no gap row
        val w2 = MarkSheet.slots(MarkSheet.acts(en, p, W2, 5, null), width, land = false)
        assertEquals(listOf("Lock" to 0, "Lock all" to 1), w2.map { it.cell.text to it.row })
        assertTrue(MarkSheet.slots(MarkSheet.Acts.NONE, width, land = false).isEmpty())
    }

    @Test fun `in landscape the acts are one line, Delete last, ending 16 dp short of Done's left edge`() {
        val p = wall()
        val width = 756.0; val doneLeft = 676.0     // a 780 dp sheet, 12 dp in at each side; Done 80 dp wide at the head's right end
        for (shape in listOf(false, true)) {
            val a = MarkSheet.acts(en, p, W1, 0, null, shape = shape)
            val s = MarkSheet.slots(a, width, land = true, doneLeftDp = doneLeft)
            assertEquals(a.all.map { it.id }, s.map { it.cell.id }); assertTrue(s.all { it.row == 0 })
            assertEquals(0.0, s.first().l); assertEquals(doneLeft - 16, s.last().r, 1e-9)
            assertTrue(s.last().cell.delete)
            assertEquals(24.0, s.last().l - s[s.size - 2].r, 1e-9)
            s.dropLast(1).zipWithNext { x, y -> assertEquals(8.0, y.l - x.r, 1e-9) }
            val cw = s.first().r - s.first().l
            for (x in s) assertEquals(cw, x.r - x.l, 1e-9)
            // nothing stands under Done, Delete least of all
            for (x in s) assertTrue(x.r <= doneLeft - 16 + 1e-9, x.cell.text)
        }
        // with no Delete the line still ends short of Done
        val w2 = MarkSheet.slots(MarkSheet.acts(en, p, W2, 4, null), width, land = true, doneLeftDp = doneLeft)
        assertEquals(listOf("Lock", "Fine move", "Lock all"), w2.map { it.cell.text }); assertEquals(doneLeft - 16, w2.last().r, 1e-9)
        assertTrue(MarkSheet.slots(MarkSheet.Acts.NONE, width, land = true, doneLeftDp = doneLeft).isEmpty())
        assertEquals(16.0, MarkSheet.DONE_CLEAR_LAND_DP)
    }

    // ------------------------------------------------------------------ while a box has the keyboard (port 63 slice I2b)

    @Test fun `the panel stands whole while a box has the keyboard - it opens whole on a Note just placed and on a Drop's why, half way on anything else`() {
        // THE HALF REST IS HALF THE WINDOW, and the keyboard takes the window's lower part: of the Mi A3's 756 dp the keys
        // take about 280, so at the half rest only the 98 dp between the panel's top and the keys would show -- the grip, the
        // head and part of the acts (152 dp) -- and never the box being typed in
        val keys = 280.0
        assertTrue(756.0 - MarkSheet.halfRestDp(756.0) - keys < MarkSheet.foldTopDp(MarkSheet.acts(en, wall(), D1, 0, null)))
        assertTrue(MarkSheet.standsWhole(false, typing = true)); assertFalse(MarkSheet.standsWhole(false, typing = false))
        assertTrue(MarkSheet.standsWhole(true, typing = false)); assertTrue(MarkSheet.standsWhole(true, typing = true))
        val p = wall()
        fun view(pn: MarkSheet.Panel, stage: Int? = 0) = assertNotNull(MarkSheet.panel(en, p, pn, stage, null))
        // every item opened by its chip or a long press: no box has the keyboard -- half way in portrait, whole in landscape
        // -- BUT WHOLE WHERE IT OPENS WITH STATE IN VIEW (the review of I2, finding 6): a long press on a mark past Existing,
        // whose Drop and Restore are STATE's moves; at the half rest its second move can lie under the fold
        var stateFirst = 0
        for (id in ALL) for (long in listOf(false, true)) {
            val v = view(MarkSheet.openPanel(p, id, 0L, longPress = long)!!)
            assertFalse(MarkSheet.opensTyping(v), id); assertEquals(v.stateFirst, MarkSheet.opensWhole(false, v), id); assertTrue(MarkSheet.opensWhole(true, v), id)
            if (v.stateFirst) { stateFirst++; assertTrue(long, id) }
        }
        assertTrue(stateFirst > 0, "some item of the wall is past Existing")
        assertTrue(MarkSheet.opensWhole(false, view(MarkSheet.openPanel(p, D2, 0L, longPress = true)!!)), "a wish, long-pressed: whole")
        assertFalse(MarkSheet.opensWhole(false, view(MarkSheet.openPanel(p, D2, 0L)!!)), "the same wish by its chip: half way")
        assertFalse(MarkSheet.opensWhole(false, view(MarkSheet.openPanel(p, D1, 0L, longPress = true)!!)), "a surveyed mark, long-pressed: half way")
        // ONCE WHOLE IT STAYS WHOLE; it BECOMES whole when a box takes the keyboard and when the blocks are pulled up
        assertFalse(MarkSheet.wholeAfter(land = false, whole = false)); assertTrue(MarkSheet.wholeAfter(land = false, whole = true))
        assertTrue(MarkSheet.wholeAfter(land = false, whole = false, typing = true)); assertTrue(MarkSheet.wholeAfter(land = false, whole = false, pulled = true))
        assertTrue(MarkSheet.wholeAfter(land = true, whole = false))
        for (t in listOf(false, true)) for (u in listOf(false, true)) assertTrue(MarkSheet.wholeAfter(false, true, typing = t, pulled = u), "never back to half")
        // A NOTE JUST PLACED opens on its note field: whole, or its words would be typed behind the keys
        val note = p.put("front", MarkTypes.NOTE, 2, 700.0, 750.0, 1500.0, 1550.0)
        val fresh = view(MarkSheet.openPanel(p, note.id, 0L, fresh = true, hist = 1)!!)
        assertTrue(fresh.body!!.focusNote); assertTrue(MarkSheet.opensTyping(fresh)); assertTrue(MarkSheet.opensWhole(false, fresh))
        // A DROP opens on Dropped's Why box (a wish at the Wish list): whole
        val drop = view(MarkSheet.openPanel(p, D2, 0L, focusMove = MarkLife.DROP)!!, PhaseTools.WISH_LIST)
        assertEquals(MarkLife.DROP, drop.body!!.focusWhy); assertTrue(MarkSheet.opensTyping(drop)); assertTrue(MarkSheet.opensWhole(false, drop))
        // ... where the phase offers no such box (Survey's gate draws no move) nothing takes the keyboard -- and the panel
        // is whole all the same, since the review of I2: it was opened FOR its STATE, whose gate line is what it has to say
        val gate = view(MarkSheet.openPanel(p, D2, 0L, focusMove = MarkLife.DROP)!!, 0)
        assertNull(gate.body!!.focusWhy); assertFalse(MarkSheet.opensTyping(gate)); assertTrue(gate.stateFirst); assertTrue(MarkSheet.opensWhole(false, gate))
        // a Tape and a chain have no box at all
        for (id in listOf("t1", "k1")) assertFalse(MarkSheet.opensTyping(view(MarkSheet.openPanel(p, id, 0L)!!)), id)
    }
}
