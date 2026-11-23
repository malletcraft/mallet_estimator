package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.I18n
import com.malletcrafts.interiorestimate.pano.i18n.LiteralScan
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * DELETE CANNOT BE PRESSED BY ACCIDENT (port 63 slice F1, 16 Nov 2026). Amit, the same evening, with build 0.3.337 on
 * his phone: "also navigation button and D2 aer very close where d2 get accidentally fired. same for delete button for
 * any item."
 *
 * He asked for distance: no confirm, no second tap. So every one-tap delete of an item of survey prep stands 16 dp
 * clear of the nearest thing that takes a press, last in what it is part of, and drawn apart; and a delete is ONE undo
 * step that says what went. The numbers and the ordering are DeleteGuard's and SurveyPrepEdit's, pure, held here (the
 * floating trash's own rule is TrashSpotTest's); the rows, the menu and the two discs are drawn by :app, which only CI
 * compiles, so that they ask these rules -- and that every delete comes through the one function -- is read from source.
 *
 * Test names are plain ASCII (see MeasureScreenTest).
 */
class DeleteGuardTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0

    private fun room(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        p.surface("front").marks.add(SurveyPrep.Mark("d1", "D", "FD1", 300.0, 1200.0, 0.0, 2100.0))
        p.addMeas("front", SurveyMeas.TAPE, listOf(doubleArrayOf(0.0, 1000.0), doubleArrayOf(1234.0, 1000.0)), "t1")
        p.addMeas("front", SurveyMeas.LINE, listOf(doubleArrayOf(500.0, 1600.0), doubleArrayOf(1300.0, 2200.0)), "m1")
        p.addMeas("front", SurveyMeas.AREA, listOf(doubleArrayOf(1800.0, 300.0), doubleArrayOf(2400.0, 300.0), doubleArrayOf(2400.0, 800.0)), "m2")
        p.addChain("front", "x", listOf(doubleArrayOf(300.0, 2400.0), doubleArrayOf(1200.0, 2400.0)), listOf(null, null), "k1")
        return p
    }

    // ---- the numbers ------------------------------------------------------------------------------------------

    @Test fun `a delete control stands 16 dp clear, in a quiet red that is not its neighbours' gold`() {
        assertEquals(16, DeleteGuard.CLEAR_DP)
        // its look: an outline and a word in reds -- red is the strongest channel of each -- and never the nudges' gold
        fun rgb(c: Int) = Triple((c shr 16) and 255, (c shr 8) and 255, c and 255)
        for (c in listOf(DeleteGuard.OUTLINE, DeleteGuard.INK)) { val (r, g, b) = rgb(c); assertTrue(r > g + 40 && r > b + 40, "%06X is a red".format(c)) }
        assertNotEquals(DeleteGuard.NEIGHBOUR, DeleteGuard.OUTLINE); assertNotEquals(DeleteGuard.NEIGHBOUR, DeleteGuard.INK)
        assertEquals(0xE0B040, DeleteGuard.NEIGHBOUR)
        // QUIET: the outline is darker than the floating trash's own red (#E5484D), which is a filled disc
        assertTrue(rgb(DeleteGuard.OUTLINE).first < 0xE5)
        // the floating trash keeps the same 16 dp by default
        val bb = SurveyPrepTouch.Box(200.0, 300.0, 280.0, 380.0)
        val s = SurveyPrepTouch.trashSpot(bb, emptyList(), 680.0, 566.0, 2.0, SurveyPrepTouch.keepOfMark(bb, false))!!
        assertTrue(SurveyPrepTouch.keepOfMark(bb, false).points.all { q -> kotlin.math.hypot(s.left + 36.0 - q[0], s.top + 36.0 - q[1]) >= (18 + DeleteGuard.CLEAR_DP) * 2.0 })
    }

    @Test fun `in every menu the delete row is the last, whatever it reads, and the rest keep their order`() {
        // the ordering rule itself
        assertEquals(listOf("b", "c", "d", "a"), DeleteGuard.last(listOf("a", "b", "c", "d")) { it == "a" })
        assertEquals(listOf("a", "b", "d", "c"), DeleteGuard.last(listOf("a", "b", "c", "d")) { it == "c" })
        assertEquals(listOf("a", "b", "c"), DeleteGuard.last(listOf("a", "b", "c")) { false })
        assertEquals(emptyList(), DeleteGuard.last(emptyList<String>()) { true })
        // every item's own menu: Duplicate first (under the finger: it loses nothing), then the locks, Cancel, and Delete last
        val p = room()
        assertEquals("Duplicate|Lock|Lock all Door|Cancel|Delete", SurveyPrepEdit.menuItemsOf(Tx.EN, p, "d1").joinToString("|") { it.second })
        assertEquals("Duplicate|Lock|Lock all tapes|Cancel|Delete", SurveyPrepEdit.menuItemsOf(Tx.EN, p, "t1").joinToString("|") { it.second })
        assertEquals("Duplicate|Lock|Lock all Line marks|Cancel|Delete", SurveyPrepEdit.menuItemsOf(Tx.EN, p, "m1").joinToString("|") { it.second })
        assertEquals("Duplicate|Lock|Lock all Line marks|Cancel|Delete", SurveyPrepEdit.menuItemsOf(Tx.EN, p, "m2").joinToString("|") { it.second })
        assertEquals("Duplicate|Lock|Lock all chains|Cancel|Delete", SurveyPrepEdit.menuItemsOf(Tx.EN, p, "k1").joinToString("|") { it.second })
        for (tx in listOf(Tx.EN, Tx.MR)) for (id in listOf("d1", "t1", "m1", "m2", "k1")) {
            val rows = SurveyPrepEdit.menuItemsOf(tx, p, id)
            assertEquals(listOf(SurveyPrepEdit.DUPLICATE, SurveyPrepEdit.LOCK, SurveyPrepEdit.LOCK_ALL, SurveyPrepEdit.CANCEL, SurveyPrepEdit.DELETE), rows.map { it.first }, id)
            assertEquals(SurveyPrepEdit.deleteWord(tx, p, id), rows.last().second, "$id: the row reads the control's own word")
        }
        // ... and as the phase cuts it, at every stage: where a delete row is left it is the last, and never the first of two
        for (st in listOf(0, 1, 2, 3, 4, 5, null)) for (picked in listOf(null, "exist", "wish")) for (id in listOf("d1", "t1", "m1", "m2", "k1")) {
            val rows = SurveyPrepEdit.menuItemsOf(Tx.EN, p, id, st, picked).map { it.first }
            if (SurveyPrepEdit.DELETE in rows) {
                assertEquals(SurveyPrepEdit.DELETE, rows.last(), "$id at $st"); assertEquals(1, rows.count { it == SurveyPrepEdit.DELETE })
                assertTrue(rows.size >= 2 && rows[rows.size - 2] == SurveyPrepEdit.CANCEL, "$id at $st: Cancel is the row before it")
            } else assertEquals(SurveyPrepEdit.CANCEL, rows.last(), "$id at $st")
        }
        // a mark past Existing: its Drop and its Restore are that same last row
        p.markById("d1")!!.second.extra = mapOf(MarkLife.KEY_BORN to MarkLife.WISH, MarkLife.KEY_LIFE to MarkLife.WISH)
        assertEquals("Lock|Lock all Door|Cancel|Drop", SurveyPrepEdit.menuItemsOf(Tx.EN, p, "d1", 1, null).joinToString("|") { it.second })
        // the reference rectangle is never deleted: its menu has no such row at all
        SurveyRef.setMode(p, "front", true, L, W, H)
        assertFalse(SurveyPrepEdit.DELETE in SurveyPrepEdit.menuItemsOf(Tx.EN, p, SurveyRef.id("front")).map { it.first })
    }

    @Test fun `the menu never opens with its delete row under the finger - low on the screen it opens above the press`() {
        val dp = 2.0
        // the menu as SpCtxMenu draws it: 4 of padding, a 24 dp head, its rows of 40, 16 dp of dead menu before the delete row, 4 under it
        fun menuDp(rows: Int) = DeleteGuard.MENU_FOOT_DP + 24 + (rows - 1) * DeleteGuard.MENU_ROW_DP + DeleteGuard.CLEAR_DP + DeleteGuard.MENU_ROW_DP + DeleteGuard.MENU_FOOT_DP
        assertEquals(248, menuDp(5)); assertEquals(128, menuDp(2))
        fun top(pressDp: Double, screenDp: Int, rows: Int = 5, del: Boolean = true) = DeleteGuard.menuTop(pressDp * dp, menuDp(rows) * dp, screenDp * dp, dp, del) / dp
        fun clear(pressDp: Double, screenDp: Int, rows: Int = 5) = DeleteGuard.menuClear(pressDp * dp, top(pressDp, screenDp, rows) * dp, menuDp(rows) * dp, dp) / dp
        // WHERE THERE IS ROOM it is where it always was: 12 dp above the press, the finger on its head, Delete far below
        assertEquals(288.0, top(300.0, 780)); assertEquals(108.0, top(150.0, 360), "landscape: pushed up from 138 to fit, the finger still nowhere near its foot")
        assertTrue(clear(300.0, 780) > 180.0)
        // LANDSCAPE, a mark near the floor line: pushed up to fit, the foot of the menu -- its delete row -- would be under
        // the finger (the row at 312..352 dp, the press at 320). It opens above the press: the row ends 16 dp over the finger
        val pushed = minOf(320.0 - 12, 360.0 - 248 - 4)
        assertEquals(108.0, pushed); assertEquals(0.0, DeleteGuard.menuClear(320.0 * dp, pushed * dp, 248 * dp, dp), "the old placement: the finger ON the delete row")
        assertEquals(60.0, top(320.0, 360)); assertEquals(16.0, clear(320.0, 360))
        assertEquals(60.0 + 248 - 4, 320.0 - 16, 1e-9)
        // just above that band nothing moves: the finger is on Cancel, the dead gap between it and Delete
        assertEquals(108.0, top(295.0, 360)); assertEquals(17.0, clear(295.0, 360))
        // AT EVERY PRESS on the phone's two screens (portrait 780 dp, landscape 336 to 360 dp) and for every menu with a
        // delete row -- five rows on a mark, down to Cancel and Drop alone -- the finger is 16 dp or more from that row,
        // and the menu is on the screen
        for (screen in listOf(336, 348, 360, 780)) for (rows in 2..5) {
            var y = 0.0
            while (y <= screen) {
                val t = top(y, screen, rows)
                assertTrue(clear(y, screen, rows) >= DeleteGuard.CLEAR_DP - 1e-9, "screen $screen, $rows rows, press $y: ${clear(y, screen, rows)} dp from Delete")
                assertTrue(t >= DeleteGuard.MENU_EDGE_DP - 1e-9 && t + menuDp(rows) <= screen + DeleteGuard.MENU_ABOVE_PRESS_DP, "screen $screen, $rows rows, press $y: top $t")
                y += 1.0
            }
        }
        // A MENU WITH NO DELETE ROW (a read-only mark's, the reference's) is placed exactly as before, wherever the press is
        for (screen in listOf(360, 780)) for (y in listOf(0.0, 10.0, 150.0, 320.0, 355.0, 700.0).filter { it <= screen }) {
            val was = minOf(y * dp - 12 * dp, screen * dp - 248 * dp - 4 * dp).coerceAtLeast(4 * dp) / dp
            assertEquals(was, top(y, screen, del = false), "screen $screen, press $y")
        }
        assertEquals(108.0, top(320.0, 360, del = false), "its foot is Cancel: under the finger it is harmless")
    }

    @Test fun `a delete takes the item and its readings, says its code, and one undo brings both back`() {
        val p = room()
        p.actual["FD1.w"] = 905.0; p.actual["F-M1.len"] = 1000.0; p.actual["FK1.1"] = 900.0
        for ((id, code, key) in listOf(Triple("d1", "FD1", "FD1.w"), Triple("m1", "F-M1", "F-M1.len"), Triple("k1", "FK1", "FK1.1"), Triple("t1", "F-T1", null))) {
            // what the screen says: the item's code, in the one existing sentence -- with UNDO
            assertEquals("$code deleted", SurveyPrepEdit.deleted(Tx.EN, code))
            assertEquals(code, p.itemById(id)!!.code)
            val snap = p.copy()                                   // the screen's ONE snapshot
            assertTrue(p.deleteMark(id), id)
            assertNull(p.itemById(id)); if (key != null) assertNull(p.actual[key], key)
            val back = SurveyPrepEdit.undoTo(p, snap)
            assertEquals(code, back.itemById(id)!!.code, "one undo brings $code back")
            if (key != null) assertEquals(snap.actual[key], back.actual[key], "with its reading")
            assertEquals(snap.itemsOf("front").map { it.code }, back.itemsOf("front").map { it.code })
        }
        // a locked item is not deleted, and nothing changes: the screen then takes its undo step back off
        val q = room(); SurveyPrepEdit.setLocked(q, "t1", true)
        assertFalse(q.deleteMark("t1")); assertEquals("F-T1", q.itemById("t1")!!.code)
        assertFalse(q.deleteMark("nothing"))
        assertEquals("F-T1 is locked — unlock first", SurveyPrepEdit.refusal(Tx.EN, "F-T1"))
    }

    // ---- the screen, read as source (only CI compiles :app) ---------------------------------------------------

    private val app = "app/src/main/kotlin/com/malletcrafts/interiorestimate/"
    private val sps = app + "SurveyPrepScreen.kt"
    private fun code(rel: String): String { val f = I18n.android.resolve(rel); assertTrue(f.isFile, rel); return LiteralScan.scan(I18n.android, rel).masked }
    private fun count(hay: String, needle: String): Int = Regex.fromLiteral(needle).findAll(hay).count()
    private fun appSources(): List<String> = listOf("app/src/main/kotlin", "camera/src/main/kotlin").flatMap { root ->
        I18n.android.resolve(root).walkTopDown().filter { it.isFile && it.name.endsWith(".kt") }
            .map { it.relativeTo(I18n.android).invariantSeparatorsPath }.sorted().toList()
    }
    /** The body of the ONE function declared as [head] in [src], brace to brace. */
    private fun body(src: String, head: String): String {
        assertEquals(1, count(src, head), "one declaration of `$head`")
        val open = src.indexOf('{', src.indexOf(head) + head.length)
        var depth = 0; var i = open
        while (i < src.length) { when (src[i]) { '{' -> depth++; '}' -> { depth--; if (depth == 0) return src.substring(open, i + 1) } }; i++ }
        throw AssertionError("`$head`: its body never closes")
    }

    // ---- the F1 review ----------------------------------------------------------------------------------------

    @Test fun `no row under the photo deletes - the held item's row has no Delete and no value, and the row's clearances are gone`() {
        // PORT 63 SLICE I1b (DESIGN-imagemeter §14 I1, the plan critic's C1): the held item's row is reduced to what moves it
        // -- a mark's label, its nudges and its padlock; a Tape's, Line's, area's or chain's code and padlock -- so it prints
        // no value and has no Delete, and the measure bar with its D2 is gone. The trash and the long-press menu delete.
        val c = code(sps)
        val panel = body(c, "val markPanel: @Composable () -> Unit =")
        assertEquals(0, count(panel, "deleteMark("), "a row under the photo deletes")
        assertEquals(0, count(panel, "SP_DELETE"), "a delete control's look under the photo")
        assertEquals(0, count(panel, "rowValue("), "a value printed under the photo")
        assertEquals(0, count(panel, "SpMeasureBar("), "the measure bar")
        assertEquals(1, count(panel, "if (row.nudges) for ((lab, dx, dy) in bs) SpButton(lab, Modifier.weight(1f), SP_GOLD, compact = land) { nudgeMark(dx, dy) }"))
        assertEquals(2, count(panel, "if (row.padlock) SpPadlock("), "a mark's padlock, and a Tape's / Line's / area's / chain's")
        assertEquals(1, count(panel, "val row = if (sl != null) Pick.row(scene(), pick) else null"), "the row is Pick's: held items only")
        // no delete control, no dead gap before one and nothing of the measure bar is left on the screen or in the rule
        for (gone in listOf("SpDeleteGap", "SP_DELETE", "SP_MARK_ROW_DP", "SP_LINE_ROW_DP", "SpMeasureBar", "MeasureBarLayout", "fun fire()"))
            assertEquals(0, count(c, gone), "SurveyPrepScreen.kt still has $gone")
        val dg = code("pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/DeleteGuard.kt")
        for (gone in listOf("rowGapDp", "rowClearDp", "rowPadDp", "rowToBarDp", "clearOfD2Dp", "rowToPaletteDp", "clearOfPaletteDp"))
            assertEquals(0, count(dg, "fun $gone("), "DeleteGuard still has $gone")
        assertFalse(I18n.android.resolve("pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/MeasureBarLayout.kt").exists(), "the measure bar's layout")
        // THE TOOL ROW AND THE KEYPAD delete nothing and fire nothing
        val pal = code(app + "SurveyPrepTools.kt")
        assertEquals(1, count(pal, "Row(Modifier.fillMaxWidth().padding(top = SurveyPrepPalette.TOP_DP.dp)) {")); assertEquals(0, count(pal, "padding(top = 6.dp)"))
        assertEquals(0, count(body(pal, "internal fun SpPalette("), "elete"))
        assertEquals(0, count(code(app + "SurveyMeasure.kt"), "elete"), "the keypad deletes no item")
        assertEquals(0, count(code(app + "SurveyMeasure.kt"), "D2"), "the keypad fires nothing")
    }

    @Test fun `the model refuses a locked item and a mark past Existing and nothing else - and the screen answers both before its undo step`() {
        // every kind of item, free and locked; a mark in every stored state; a Note (it has no state)
        val p = room()
        fun mark(id: String, code: String, extra: Map<String, Any?>) = p.surface("front").marks.add(SurveyPrep.Mark(id, "W", code, 1500.0, 1900.0, 900.0, 1500.0, extra))
        mark("lk", "FW1", mapOf(SurveyPrep.LOCKED to true))
        val lives = MarkLife.STORED.filter { it != MarkLife.EXIST }
        assertTrue(MarkLife.WISH in lives && MarkLife.DROP in lives && MarkLife.SCOPE in lives)
        lives.forEachIndexed { i, life ->
            mark("life$i", "FW${i + 2}", mapOf(MarkLife.KEY_LIFE to life))
            mark("lifelk$i", "FW${i + 20}", mapOf(MarkLife.KEY_LIFE to life, SurveyPrep.LOCKED to true))
        }
        p.surface("front").marks.add(SurveyPrep.Mark("n1", MarkLife.NOTE_TYPE, "FNOTE1", 100.0, 150.0, 100.0, 150.0, mapOf(MarkLife.KEY_LIFE to MarkLife.WISH)))
        p.addMeas("front", SurveyMeas.LINE, listOf(doubleArrayOf(100.0, 100.0), doubleArrayOf(200.0, 200.0)), "m9")
        p.addChain("front", "x", listOf(doubleArrayOf(300.0, 300.0), doubleArrayOf(900.0, 300.0)), listOf(null, null), "k9")
        assertEquals(true, SurveyPrepEdit.setLocked(p, "m9", true)); assertEquals(true, SurveyPrepEdit.setLocked(p, "k9", true))
        val ids = SurveyPrep.SURFACES.flatMap { s -> p.itemsOf(s).map { it.id } }
        assertEquals(5 + 1 + 2 * lives.size + 3, ids.size, "$ids")
        var refused = 0
        for (id in ids) {
            val item = p.itemById(id)!!
            val e = p.markById(id)?.second
            // WHAT THE SCREEN'S deleteMark ANSWERS BEFORE ITS SNAPSHOT: the lock ("FD1 is locked -- unlock first"), and
            // for a mark whatever is not a plain delete (Restore, or the Mark sheet on Dropped)
            val answered = item.locked || (e != null && MarkLife.deleteAct(e) != MarkLife.DeleteAct.PLAIN)
            // WHAT THE MODEL REFUSES
            val q = p.copy(); val took = q.deleteMark(id)
            assertEquals(!item.spared, took, "$id: the model refuses exactly a locked item and a mark past Existing")
            assertEquals(!took, answered, "$id: the screen answers every refusal of the model's before any undo step, and no delete it would make")
            assertEquals(took, q.itemById(id) == null, id)
            if (!took) refused++
        }
        assertEquals(1 + 2 * lives.size + 2, refused, "the locked mark, every mark past Existing free or locked, the locked Line and chain")
        // a Note is deleted whatever its `life` says: it has no state
        assertTrue(p.copy().deleteMark("n1")); assertEquals(MarkLife.DeleteAct.PLAIN, MarkLife.deleteAct(p.markById("n1")!!.second))
        // an id nothing has is the model's only other false, and the screen returns on it before anything (itemById)
        assertFalse(p.copy().deleteMark("nobody")); assertNull(p.itemById("nobody"))
        val del = body(code(sps), "fun deleteMark(mid: String)")
        assertTrue(del.indexOf("val it0 = prep.itemById(mid) ?: return") in 0 until del.indexOf("snapshot()"))
    }

    @Test fun `every one-tap delete comes through deleteMark - one undo step, and the snackbar says the code with UNDO`() {
        val c = code(sps); val p3 = code(app + "SurveyPrep360.kt")
        val del = body(c, "fun deleteMark(mid: String)")
        // ONE step on the plain path, taken with the one call that deletes -- and NEVER taken and then taken back (the F1
        // review, finding 4): snapshot() drops the oldest of 40 held steps, so a pop after it loses a step for nothing.
        // The model's own refusals are answered before the step (the test below holds that those are all it has).
        assertEquals(1, count(del, "\n        snapshot(); prep.deleteMark(mid)\n"))
        assertEquals(1, count(del, "prep.deleteMark(mid)"))
        assertEquals(0, count(del, "if (!prep.deleteMark(mid))"), "no branch for a refusal that cannot happen")
        // the one pop in the whole function is RESTORE's own, and nothing after the plain path's snapshot takes it back
        assertEquals(1, count(del, "hist.removeAt(")); assertEquals(2, count(del, "snapshot()"))
        assertEquals(0, count(del.substring(del.indexOf("snapshot(); prep.deleteMark(mid)")), "hist.removeAt("))
        for (rel in appSources()) assertEquals(if (rel == sps) 1 else 0, count(code(rel), "prep.deleteMark("), "$rel deletes an item by itself")
        // it says WHAT went, by the existing sentence, in the snackbar that carries UNDO
        assertEquals(1, count(del, "snack(SurveyPrepEdit.deleted(tx, code))")); assertEquals(1, count(c, "SurveyPrepEdit.deleted("))
        assertTrue(del.indexOf("snapshot(); prep.deleteMark(mid)") < del.indexOf("snack(SurveyPrepEdit.deleted(tx, code))"))
        assertEquals(1, count(c, "fun snack(text: String, undo: Boolean = true) { snackText = text; snackUndo = undo; snackSeq++; snackHist = hist.size }"))
        assertEquals("FD1 deleted", SurveyPrepEdit.deleted(Tx.EN, "FD1"))
        // no confirm and no second tap: he asked for distance
        assertEquals(0, count(del, "armed")); assertEquals(0, count(del, "Dialog("))
        // the phase and the lock are asked before the step, as they were
        assertTrue(del.indexOf("if (!PhaseTools.delOk(prep, mid, quoteStage))") in 0 until del.indexOf("if (it0.locked) { refuse(code); return }"))
        assertTrue(del.indexOf("if (it0.locked) { refuse(code); return }") < del.indexOf("snapshot(); prep.deleteMark(mid)"))
        // ... and so is a mark past Existing: Restore and the Drop sheet both return before the plain path
        assertTrue(del.indexOf("MarkLife.DeleteAct.DROP_SHEET -> { openSheet(mid, MarkLife.DROP); return }") in 0 until del.indexOf("snapshot(); prep.deleteMark(mid)"))
        assertTrue(del.indexOf("MarkLife.DeleteAct.RESTORE -> {") in 0 until del.indexOf("MarkLife.DeleteAct.DROP_SHEET"))
        // EVERY PATH: the photo's disc (the held item's only, port 63 slice I1b), the 360's disc and the menu's row -- no row
        for (v in listOf("sm", "sq", "sk")) assertEquals(0, count(c, "{ deleteMark($v.id) }"), v)
        assertEquals(1, count(c, "onDelete = { deleteMark(it) },")); assertEquals(1, count(c, "delete = { mid -> deleteMark(mid) },"))
        assertEquals(1, count(c, "SurveyPrepEdit.DELETE -> deleteMark(mid)"))
        assertEquals(1, count(c, "if (picked != null && hypot(at.x - tr[0], at.y - tr[1]) <= tr[2]) onDeleteS(picked)"))
        assertEquals(1, count(p3, "if (picked != null && hypot(at.x - tr[0], at.y - tr[1]) <= tr[2]) actS.delete(picked)"))
        assertEquals(5, count(c, "deleteMark("), "its declaration, the model's own call, and the three ways in")
        // the photo's disc is pressed only for the HELD item (Pick.trash), and drawn only for it
        assertEquals(1, count(c, "val tr = if (mode == StepRoute.MARK && Pick.trash(sceneS, pickS, SurveyPrepPalette.armed(toolS))) trashAt[0] else null"))
        assertEquals(1, count(c, "val pickedId = if (mode == StepRoute.MARK && Pick.trash(scene, pick, SurveyPrepPalette.armed(tool))) pickedHere else null"))
        assertEquals(1, count(c, "val picked = pickS.picked?.takeIf { pickS.held }"))
        // THE MARK SHEET deletes nothing by one tap
        val sheet = code(app + "MarkSheet.kt")
        for (word in listOf("deleteMark", "onDelete", "common_delete")) assertEquals(0, count(sheet, word), "MarkSheet.kt has $word")
        assertEquals(0, count(code(app + "SurveyMeasure.kt"), "elete"), "the keypad deletes no item")
    }

    @Test fun `the long-press menu draws its delete row last and apart, and both floating discs keep DeleteGuard's 16 dp`() {
        val locks = code(app + "SurveyPrepLocks.kt"); val c = code(sps); val p3 = code(app + "SurveyPrep360.kt")
        val menu = body(locks, "internal fun SpCtxMenu(header: String, items: List<Pair<String, String>>, modifier: Modifier, onPick: (String) -> Unit)")
        // the rows are drawn in the order :pano gives -- Delete last -- and the delete row has 16 dp of dead menu above it
        assertEquals(1, count(menu, "for ((k, t) in items) {"))
        assertEquals(1, count(menu, "if (k == SurveyPrepEdit.DELETE) {"))
        // the menu's own measures are DeleteGuard's -- its rows, and the padding under its last -- so the placement rule
        // (menuTop) knows where the delete row is
        assertEquals(2, count(menu, ".height(DeleteGuard.MENU_ROW_DP.dp)")); assertEquals(1, count(menu, ".padding(vertical = DeleteGuard.MENU_FOOT_DP.dp)"))
        assertEquals(0, count(menu, "height(40.dp)"))
        val delRow = menu.substring(menu.indexOf("if (k == SurveyPrepEdit.DELETE) {"), menu.indexOf("} else {"))
        assertEquals(1, count(delRow, "Spacer(Modifier.height(DeleteGuard.CLEAR_DP.dp))"), "a Spacer takes no tap")
        assertTrue(delRow.indexOf("Spacer(Modifier.height(DeleteGuard.CLEAR_DP.dp))") < delRow.indexOf(".clickable(role = Role.Button) { onPick(k) }"))
        assertEquals(1, count(delRow, ".border(1.5.dp, CM_DEL_LINE, del)")); assertEquals(1, count(delRow, "Text(t, color = CM_DEL,"))
        assertEquals(1, count(locks, "private val CM_DEL_LINE = Color(SurveyPrepDraw.argb(DeleteGuard.OUTLINE))"))
        assertEquals(1, count(locks, "private val CM_DEL = Color(SurveyPrepDraw.argb(DeleteGuard.INK))"))
        assertEquals(2, count(menu, ".clickable(role = Role.Button) { onPick(k) }"), "one tap still picks a row: the delete row and every other")
        // the screen hands the menu the phase's rows and nothing reorders them on the way; and it places the menu by the
        // rule that keeps the delete row from under the finger, told whether these rows have one
        assertEquals(1, count(c, "val rows = SurveyPrepEdit.menuItemsOf(tx, prep, c.id, quoteStage, asPicked)"))
        assertEquals(1, count(c, "SpCtxMenu(head, rows,"))
        assertEquals(1, count(c, "val top = DeleteGuard.menuTop((c.y - rootTop).toDouble(), mh.toDouble(), rootH.toDouble(), dens.density.toDouble(),\n                    rows.any { it.first == SurveyPrepEdit.DELETE }).toFloat()"))
        assertEquals(1, count(c, "DeleteGuard.menuTop("))
        assertEquals(1, count(c, "with(dens) { (232 + DeleteGuard.CLEAR_DP).dp.toPx() }"), "the menu's height before it is first measured: one dead gap taller")
        // BOTH DISCS are placed by the one rule, handed the picked item's own handles, and neither passes a clearance of its own
        assertEquals(1, count(c, "SurveyPrepTouch.trashSpot(bb, tags.map { it.box }, size.width.toDouble(), size.height.toDouble(), dpx.toDouble(), keep)?.let { ts ->"))
        assertEquals(1, count(c, "else SurveyPrepTouch.keepOfMark(bb, picked?.let { SurveyPrep.isSpot(it.t) } == true)"))
        assertEquals(1, count(c, "val keep = if (scrPts != null) SurveyPrepTouch.keepOfLine(scrPts, pickedM?.kind == SurveyMeas.AREA,"))
        assertEquals(1, count(c, "if (pickedM?.kind == SurveyMeas.TAPE) SurveyMeas.bandHalfPx(kk / rr.sq.mpp, dpx.toDouble()) else 0.0)"))
        assertEquals(1, count(p3, "SurveyPrepTouch.trashSpot(t.box, emptyList(), size.width.toDouble(), size.height.toDouble(), dpx.toDouble(), t.keep)"))
        assertEquals(1, count(p3, "Pano360.trashOf(v, fr, prep, tool, sel?.id?.takeIf { sid -> PhaseTools.delOk(prep, sid, stage) }, dpx.toDouble())?.let { t ->"))
        for (rel in appSources()) {
            val src = code(rel)
            assertEquals(if (rel == sps || rel == app + "SurveyPrep360.kt") 1 else 0, count(src, "SurveyPrepTouch.trashSpot("), rel)
            assertEquals(0, count(src, "clear ="), "$rel hands the trash a clearance of its own")
            assertEquals(0, count(src, "HANDLE_CLEAR_DP"), "$rel still names the 7.5 dp the disc used to keep off an end")
        }
        assertEquals(0, count(code("pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/SurveyGuide.kt"), "HANDLE_CLEAR_DP"))
        // a disc that is not drawn takes no press: both gestures read the place the draw last wrote
        assertEquals(1, count(c, "trashAt[0] = null")); assertEquals(1, count(p3, "trashAt[0] = null"))
    }
}
