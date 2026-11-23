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
 * He asked for distance: no confirm, no second tap. So the one-tap delete of an item of survey prep stands clear of
 * the nearest thing that takes a press, last in what it is part of, and drawn apart; and a delete is ONE undo step that
 * says what went. SINCE PORT 63 SLICE I2b DELETE HAS ONE PLACE -- the panel that slides up, its last act -- and the
 * long-press menu, the floating trash (TrashSpotTest went with it) and every row under the photo are gone. The numbers
 * and the ordering are DeleteGuard's, SurveyPrepEdit's and MarkSheet's, pure, held here; the panel is drawn by :app,
 * which only CI compiles, so that it asks these rules -- and that every delete comes through the one function -- is
 * read from source.
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
        // QUIET: the outline is darker than the red the floating trash's filled disc had (#E5484D)
        assertTrue(rgb(DeleteGuard.OUTLINE).first < 0xE5)
        // THE ONE DELETE CONTROL -- the panel's last act -- stands MORE than that 16 dp from the act beside it
        assertTrue(MarkSheet.DELETE_GAP_DP > DeleteGuard.CLEAR_DP)
        val acts = MarkSheet.acts(Tx.EN, room(), "d1", 0, null)
        val slots = MarkSheet.slots(acts, 336.0, false)
        val del = slots.single { it.cell.delete }
        val beside = slots.filter { it.row == del.row && !it.cell.delete }.maxOf { it.r }
        assertTrue(del.l - beside >= MarkSheet.DELETE_GAP_DP - 1e-9, "Delete is ${del.l - beside} dp from the act beside it")
    }

    @Test fun `in every list of acts the delete row is the last, whatever it reads, and the rest keep their order`() {
        // the ordering rule itself
        assertEquals(listOf("b", "c", "d", "a"), DeleteGuard.last(listOf("a", "b", "c", "d")) { it == "a" })
        assertEquals(listOf("a", "b", "d", "c"), DeleteGuard.last(listOf("a", "b", "c", "d")) { it == "c" })
        assertEquals(listOf("a", "b", "c"), DeleteGuard.last(listOf("a", "b", "c")) { false })
        assertEquals(emptyList(), DeleteGuard.last(emptyList<String>()) { true })
        // every item's own rows, as the long-press menu listed them and the panel's acts are still taken from them
        // (MarkSheet.acts): Duplicate first, then the locks, Cancel, and Delete last
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
        // the reference rectangle is never deleted: its rows have no such row at all -- and it has no panel
        SurveyRef.setMode(p, "front", true, L, W, H)
        assertFalse(SurveyPrepEdit.DELETE in SurveyPrepEdit.menuItemsOf(Tx.EN, p, SurveyRef.id("front")).map { it.first })
        assertNull(MarkSheet.openPanel(p, SurveyRef.id("front"), 0L))
        // THE PANEL'S ACTS carry them through DeleteGuard.last: where there is a delete cell it is the LAST of line 2
        val q = room()
        for (st in listOf(0, 1, 2, 3, 4, 5, null)) for (picked in listOf(null, "exist", "wish")) for (id in listOf("d1", "t1", "m1", "m2", "k1")) {
            val a = MarkSheet.acts(Tx.EN, q, id, st, picked)
            assertEquals(a.all.count { it.delete }, if (a.delete != null) 1 else 0, "$id at $st: one delete cell at most, and it is line 2's last")
            assertTrue(a.line1.none { it.delete }, "$id at $st: no delete cell on line 1")
            assertEquals(SurveyPrepEdit.DELETE in SurveyPrepEdit.menuItemsOf(Tx.EN, q, id, st, picked).map { it.first }, a.delete != null, "$id at $st")
            assertTrue(a.all.none { it.id == SurveyPrepEdit.CANCEL }, "$id at $st: Cancel is no act")
        }
    }

    @Test fun `the panel's Delete is never under the finger that opened it - a whole act row below Done, last, apart, and deaf at first`() {
        // THE LONG-PRESS MENU IS GONE (port 63 slice I2b), and with it the rule that kept its delete row from opening under
        // the finger (DeleteGuard.menuTop). The panel opens over the photo's foot, where the long press and the code chip
        // that open it are pressed -- so the same guard is kept by WHERE Delete stands in it, and by the first 300 ms.
        val p = room()
        for (id in listOf("d1", "t1", "m1", "m2", "k1")) {
            val acts = MarkSheet.acts(Tx.EN, p, id, 0, null)
            assertTrue(acts.delete != null && acts.rows == 2, "$id: a delete cell, and both act rows")
            // PORTRAIT, the Mi A3's 336 dp of content: Delete is on the SECOND line, in the LAST column, at the right end
            val slots = MarkSheet.slots(acts, 336.0, false)
            val del = slots.single { it.cell.delete }
            assertEquals(1, del.row, id); assertEquals(336.0, del.r, 1e-9, id)
            assertTrue(slots.all { it === del || it.row < del.row || it.r <= del.l - MarkSheet.DELETE_GAP_DP + 1e-9 }, "$id: nothing stands beside Delete nearer than 24 dp")
            // ... a WHOLE ACT ROW below Done, and more: Done ends at the head's foot, Delete's cell starts 56 dp further down
            val deleteTop = MarkSheet.GRIP_DP + MarkSheet.HEAD_DP + del.t
            assertTrue(deleteTop - MarkSheet.doneBottomDp(false) >= MarkSheet.ACT_ROW_DP, id)
            // ... AND CLEAR_DP FROM EVERYTHING ELSE THAT TAKES A PRESS, IN EVERY DIRECTION (the review of I2, finding 4):
            // the act OVER it as well as the one beside it. Two rows of 44 with cells of 36 left Delete 8 dp under
            // `Fine move`, the act pressed most; its line is moved down so that it is 16
            for (o in slots) if (o !== del) assertTrue(clear(del, o) >= DeleteGuard.CLEAR_DP - 1e-9, "$id: Delete stands ${clear(del, o)} dp from ${o.cell.text}")
            slots.filter { it.row == 0 }.forEach { o -> assertEquals(DeleteGuard.CLEAR_DP.toDouble(), del.t - o.b, 1e-9, "$id: under ${o.cell.text}") }
            assertEquals(MarkSheet.deleteDropDp(acts) + 2 * MarkSheet.ACT_ROW_DP, MarkSheet.actsDp(acts)); assertEquals(8, MarkSheet.deleteDropDp(acts), id)
            assertTrue(del.b <= MarkSheet.actsDp(acts), "$id: the acts' box holds it")
            // Done's own box, from the top of the acts: 32 dp high, centred in the head over them, at the right end
            assertTrue(clear(del, 336.0 - 70, 336.0, -(MarkSheet.HEAD_DP + MarkSheet.DONE_DP) / 2.0, -(MarkSheet.HEAD_DP - MarkSheet.DONE_DP) / 2.0) >= DeleteGuard.CLEAR_DP, id)
            // ... and above the fold of the half rest, with Done: nothing he needs to cancel a slip is hidden
            assertTrue(MarkSheet.foldTopDp(acts) <= MarkSheet.halfRestDp(756.0), id)
            // LANDSCAPE: one line, Delete last, 24 dp from the act before it, and the line ends 16 dp short of Done
            val doneLeft = 756.0 - 70
            val land = MarkSheet.slots(acts, 756.0, true, doneLeft)
            assertTrue(land.last().cell.delete && land.all { it.row == 0 }, id)
            assertTrue(land.last().r <= doneLeft - MarkSheet.DONE_CLEAR_LAND_DP + 1e-9, "$id: nothing stands under Done")
            assertEquals(MarkSheet.DELETE_GAP_DP, land.last().l - land[land.size - 2].r, 1e-9, id)
            // ... and there too Delete is CLEAR_DP from every other act and from Done (16 dp to its left, 6 dp under it)
            val ld = land.last()
            for (o in land) if (o !== ld) assertTrue(clear(ld, o) >= DeleteGuard.CLEAR_DP - 1e-9, "$id, landscape: ${clear(ld, o)} dp from ${o.cell.text}")
            assertTrue(clear(ld, doneLeft, 756.0, -(MarkSheet.HEAD_LAND_DP + MarkSheet.DONE_DP) / 2.0, -(MarkSheet.HEAD_LAND_DP - MarkSheet.DONE_DP) / 2.0) >= DeleteGuard.CLEAR_DP, "$id, landscape: Done")
            assertEquals(0, MarkSheet.deleteDropDp(MarkSheet.Acts(acts.line1, acts.line2.filterNot { it.delete })), "$id: no Delete, no move")
        }
        // WITH THE FOUR-CORNER SHAPE ROW DRAWN (port 63 slice Q2a: the switch slice Q2 turns on) NOTHING ABOUT DELETE MOVES:
        // the row is line 2's first cell, in a column that stood empty -- on a rectangle (`Four corners`) and on a mark
        // that carries four corners (`Rectangle`) alike. Delete is still the last act, in the same place, and still
        // CLEAR_DP from every other act and from Done, in portrait and in landscape
        val quad = SurveyPrep.Mark("w1", "W", "FW1", 1500.0, 2400.0, 900.0, 2100.0).also { p.surface("front").marks.add(it) }
        assertTrue(Quad.set(quad, listOf(doubleArrayOf(1500.0, 1300.0), doubleArrayOf(2400.0, 900.0), doubleArrayOf(2400.0, 2100.0), doubleArrayOf(1500.0, 2100.0))))
        for ((id, row) in listOf("d1" to SurveyPrepEdit.SHAPE_FREE, "w1" to SurveyPrepEdit.SHAPE_BOX)) {
            val plain = MarkSheet.acts(Tx.EN, p, id, 0, null); val acts = MarkSheet.acts(Tx.EN, p, id, 0, null, shape = true)
            assertEquals(plain.all.size + 1, acts.all.size, id); assertEquals(row, acts.line2.first().id, id); assertTrue(acts.all.last().delete, "$id: Delete is the last act")
            assertEquals(MarkSheet.actsDp(plain), MarkSheet.actsDp(acts), "$id: the panel is no taller for the row"); assertEquals(2, acts.rows, id)
            val slots = MarkSheet.slots(acts, 336.0, false); val del = slots.single { it.cell.delete }
            val was = MarkSheet.slots(plain, 336.0, false).single { it.cell.delete }
            assertEquals(listOf(was.row, was.l, was.r, was.t, was.b), listOf(del.row, del.l, del.r, del.t, del.b), "$id: Delete stands where it stood")
            for (o in slots) if (o !== del) assertTrue(clear(del, o) >= DeleteGuard.CLEAR_DP - 1e-9, "$id, the shape row drawn: Delete stands ${clear(del, o)} dp from ${o.cell.text}")
            assertTrue(clear(del, 336.0 - 70, 336.0, -(MarkSheet.HEAD_DP + MarkSheet.DONE_DP) / 2.0, -(MarkSheet.HEAD_DP - MarkSheet.DONE_DP) / 2.0) >= DeleteGuard.CLEAR_DP, "$id: Done")
            // the shape row itself is a real button, a whole column wide, and stands under Lock -- nowhere near Delete
            val shape = slots.single { it.cell.id == row }
            assertEquals(1, shape.row, id); assertEquals(0.0, shape.l, 1e-9, id); assertTrue(del.l - shape.r >= 100.0, "$id: ${del.l - shape.r} dp from Delete")
            for (wide in listOf(736.0, 756.0, 780.0)) {
                val doneLeft = wide - 70
                val land = MarkSheet.slots(acts, wide, true, doneLeft); val ld = land.last()
                assertTrue(ld.cell.delete && land.all { it.row == 0 }, "$id $wide")
                for (o in land) if (o !== ld) assertTrue(clear(ld, o) >= DeleteGuard.CLEAR_DP - 1e-9, "$id, landscape $wide, the shape row drawn: ${clear(ld, o)} dp from ${o.cell.text}")
                assertTrue(clear(ld, doneLeft, wide, -(MarkSheet.HEAD_LAND_DP + MarkSheet.DONE_DP) / 2.0, -(MarkSheet.HEAD_LAND_DP - MarkSheet.DONE_DP) / 2.0) >= DeleteGuard.CLEAR_DP, "$id, landscape $wide: Done")
                assertTrue(ld.r <= doneLeft - MarkSheet.DONE_CLEAR_LAND_DP + 1e-9, "$id $wide: nothing stands under Done")
            }
        }
        // IT TAKES NO TAP FOR ITS FIRST 300 MS: a finger that had not quite left the photo deletes nothing
        val pn = MarkSheet.openPanel(p, "d1", 10_000L, longPress = true)!!
        for (t in listOf(10_000L, 10_150L, 10_299L)) assertFalse(MarkSheet.answers(pn, t), "$t")
        assertTrue(MarkSheet.answers(pn, 10_300L))
        assertTrue(MarkSheet.actStep(Tx.EN, p, pn, SurveyPrepEdit.DELETE, 0, null) is MarkSheet.ActStep.Delete)
        // A MARK PAST EXISTING HAS NO DELETE CELL AT ALL: Drop and Restore are its STATE moves
        p.markById("d1")!!.second.extra = mapOf(MarkLife.KEY_BORN to MarkLife.WISH, MarkLife.KEY_LIFE to MarkLife.WISH)
        assertNull(MarkSheet.acts(Tx.EN, p, "d1", 1, null).delete)
        // the menu's own placement rule went with the menu
        val dg = code("pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/DeleteGuard.kt")
        for (gone in listOf("fun menuTop(", "fun menuClear(", "MENU_ROW_DP", "MENU_FOOT_DP", "MENU_ABOVE_PRESS_DP", "MENU_EDGE_DP")) assertEquals(0, count(dg, gone), "DeleteGuard still has $gone")
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

    /** How far two boxes stand apart, dp: 0 when they touch or overlap. */
    private fun clear(a: MarkSheet.Slot, l: Double, r: Double, t: Double, b: Double): Double =
        Math.hypot(maxOf(0.0, a.l - r, l - a.r), maxOf(0.0, a.t - b, t - a.b))
    private fun clear(a: MarkSheet.Slot, o: MarkSheet.Slot): Double = clear(a, o.l, o.r, o.t, o.b)

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

    @Test fun `nothing under the photo deletes - the picked item's row is gone, and the tool row and the keypad delete nothing`() {
        // PORT 63 SLICE I1b took the Delete and every value out of the held item's row; SLICE I2b TAKES THE ROW ITSELF
        // (DESIGN-imagemeter §14 I2): what stands under the photo in 2 · Mark is the tool row, or the keypad in its place,
        // and nothing of the picked item -- its code is a chip on the photo, its padlock, its nudges and its Delete the
        // acts of the panel that chip opens.
        val c = code(sps)
        val panel = body(c, "val markPanel: @Composable () -> Unit =")
        assertEquals(0, count(panel, "deleteMark("), "a row under the photo deletes")
        assertEquals(0, count(panel, "SP_DELETE"), "a delete control's look under the photo")
        assertEquals(0, count(panel, "rowValue("), "a value printed under the photo")
        assertEquals(0, count(panel, "SpMeasureBar("), "the measure bar")
        for (gone in listOf("SpPadlock(", "SpButton(", "nudgeMark(", "openPanel(", "toggleLock(", "Pick.row(", "selNow()", "heldRowDp"))
            assertEquals(0, count(panel, gone), "under the photo: $gone")
        assertEquals(1, count(panel, "when (StepRoute.underMark(toolRow, keypad = padOpen && pick.gold != null)) {"))
        assertEquals(1, count(panel, "StepRoute.Under.TOOLS -> SpPalette(tab, tool, land,")); assertEquals(1, count(panel, "StepRoute.Under.KEYPAD ->"))
        assertEquals(1, count(panel, "StepRoute.Under.NOTHING -> Unit"))
        // no delete control, no dead gap before one, no nudge row and nothing of the measure bar is left on the screen or in the rule
        for (gone in listOf("SpDeleteGap", "SP_DELETE", "SP_MARK_ROW_DP", "SP_LINE_ROW_DP", "SpMeasureBar", "MeasureBarLayout", "fun fire()", "fun nudgeMark(", "Pick.row(", "Pick.trash("))
            assertEquals(0, count(c, gone), "SurveyPrepScreen.kt still has $gone")
        val dg = code("pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/DeleteGuard.kt")
        for (gone in listOf("rowGapDp", "rowClearDp", "rowPadDp", "rowToBarDp", "clearOfD2Dp", "rowToPaletteDp", "clearOfPaletteDp"))
            assertEquals(0, count(dg, "fun $gone("), "DeleteGuard still has $gone")
        assertFalse(I18n.android.resolve("pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/MeasureBarLayout.kt").exists(), "the measure bar's layout")
        val pick = code("pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/Pick.kt")
        for (gone in listOf("fun row(", "class Row(", "fun trash(")) assertEquals(0, count(pick, gone), "Pick still has $gone")
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
            // for a mark whatever is not a plain delete (Restore, or the panel on Dropped)
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

    @Test fun `the one delete comes through deleteMark - the panel's last act, one undo step, and the snackbar says the code with UNDO`() {
        val c = code(sps); val p3 = code(app + "SurveyPrep360.kt")
        val del = body(c, "fun deleteMark(mid: String)")
        // ONE step on the plain path, taken with the one call that deletes -- and NEVER taken and then taken back (the F1
        // review, finding 4): snapshot() drops the oldest of 40 held steps, so a pop after it loses a step for nothing.
        // The model's own refusals are answered before the step (the test above holds that those are all it has).
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
        // ... and so is a mark past Existing: Restore and the Drop panel both return before the plain path
        assertTrue(del.indexOf("MarkLife.DeleteAct.DROP_SHEET -> { openPanel(mid, MarkLife.DROP); return }") in 0 until del.indexOf("snapshot(); prep.deleteMark(mid)"))
        assertTrue(del.indexOf("MarkLife.DeleteAct.RESTORE -> {") in 0 until del.indexOf("MarkLife.DeleteAct.DROP_SHEET"))
        // THE ONE WAY IN (port 63 slice I2b): the panel's Delete, where MarkSheet.actStep answers Delete -- no row, no disc
        // on the photo or the 360, no menu
        val act = body(c, "fun panelAct(act: String)")
        // (port 63 slice Q2 moved this pin: the call names `shape = true` since the four-corner mark is on -- the shape row
        //  is an act the panel may be asked, and Delete is still answered by the same call)
        assertEquals(1, count(act, "when (val step = MarkSheet.actStep(tx, prep, pn, act, quoteStage, asPicked, shape = true)) {"))
        assertEquals(1, count(act, "if (MarkSheet.panelCancel(prep, pn)) pn.draft?.let { st -> noteRemoved(st) } else deleteMark(pn.id)"))
        assertTrue(act.indexOf("is MarkSheet.ActStep.Delete -> {") in 0 until act.indexOf("deleteMark(pn.id)"))
        assertTrue(act.indexOf("if (!MarkSheet.answers(pn, SystemClock.uptimeMillis())) return") in 0 until act.indexOf("MarkSheet.actStep("), "deaf for its first 300 ms")
        assertEquals(3, count(c, "deleteMark("), "its declaration, the model's own call, and the ONE way in")
        for (gone in listOf("onDelete", "onDeleteS", "delete = { mid ->", "SurveyPrepEdit.DELETE -> deleteMark(mid)", "trashAt", "Pick.trash(", "ctxPick(", "SpCtx"))
            assertEquals(0, count(c, gone), "SurveyPrepScreen.kt still has $gone")
        for (gone in listOf("actS.delete(", "val delete:", "trashAt", "trashOf(", "fun trash(")) assertEquals(0, count(p3, gone), "SurveyPrep360.kt still has $gone")
        // THE PANEL'S OWN FILE deletes nothing itself: it hands the act's id back
        val sheet = code(app + "MarkSheet.kt")
        for (word in listOf("deleteMark", "onDelete", "common_delete")) assertEquals(0, count(sheet, word), "MarkSheet.kt has $word")
        assertEquals(0, count(code(app + "SurveyMeasure.kt"), "elete"), "the keypad deletes no item")
    }

    @Test fun `the long-press menu and both floating discs are gone, and the panel draws its Delete last and apart in DeleteGuard's look`() {
        val locks = code(app + "SurveyPrepLocks.kt"); val c = code(sps); val p3 = code(app + "SurveyPrep360.kt"); val sheet = code(app + "MarkSheet.kt")
        // GONE, from every source of the app: the menu, its placement, both discs and what placed them
        for (rel in appSources()) {
            val src = code(rel)
            for (gone in listOf("SpCtxMenu", "SpCtx(", "DeleteGuard.menuTop(", "DeleteGuard.MENU_", "SurveyPrepTouch.trashSpot(", "keepOfMark(", "keepOfLine(", "Pano360.trashOf(", "trashAt", "P3Draw.trash("))
                assertEquals(0, count(src, gone), "$rel still has $gone")
            assertEquals(0, count(src, "HANDLE_CLEAR_DP"), "$rel still names the 7.5 dp the disc used to keep off an end")
        }
        assertEquals(0, count(code("pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/SurveyGuide.kt"), "HANDLE_CLEAR_DP"))
        val touch = code("pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/SurveyPrepTouch.kt")
        for (gone in listOf("fun trashSpot(", "fun keepOfMark(", "fun keepOfLine(", "class TrashKeep(", "TRASH_DP")) assertEquals(0, count(touch, gone), "SurveyPrepTouch still has $gone")
        assertEquals(0, count(code("pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/Pano360.kt"), "fun trashOf("))
        assertEquals(0, count(locks, "DeleteGuard"), "the locks' file draws no delete control any more")
        // THE PANEL draws the acts where :pano puts them -- Delete last and apart (MarkSheet.slots) -- and the delete cell in
        // the delete control's own look, which is DeleteGuard's
        val acts = body(sheet, "private fun MsActs(v: MarkSheet.PanelView, land: Boolean, wide: Int, doneWide: Int, onAct: (String) -> Unit)")
        assertEquals(1, count(acts, "val slots = MarkSheet.slots(v.acts, w, land, w - doneWide / dens)"))
        assertEquals(1, count(acts, "for (s in slots) {")); assertEquals(1, count(sheet, "MarkSheet.slots("))
        // EVERY EDGE of a cell is its slot's, and the acts' height MarkSheet.actsDp's: the file holds no row arithmetic of
        // its own, so the 16 dp between Delete and the act over it cannot be drawn as 8 (the review of I2, finding 4)
        assertEquals(1, count(acts, "Box(Modifier.padding(start = s.l.toFloat().dp, top = s.t.toFloat().dp).width((s.r - s.l).toFloat().dp).height((s.b - s.t).toFloat().dp)"))
        assertEquals(1, count(acts, "val tall = MarkSheet.actsDp(v.acts, land)")); assertEquals(1, count(acts, "Box(Modifier.fillMaxWidth().height(tall.dp)) {"))
        assertEquals(0, count(acts, "ACT_ROW")); assertEquals(0, count(acts, "s.row")); assertEquals(0, count(acts, "rowDp"))
        assertEquals(1, count(sheet, ".height(MarkSheet.DONE_DP.dp)"), "Done's height is the one the clearance is tested with")
        assertEquals(1, count(acts, ".border(if (c.delete) 1.5.dp else 1.dp, if (c.delete) MS_DEL_LINE else MS_LINE, shape)"))
        assertEquals(1, count(acts, "Text(c.text, color = if (c.delete) MS_DEL else MS_FG,"))
        assertEquals(1, count(acts, ".clickable(role = Role.Button) { onAct(c.id) }"), "one tap still picks an act: Delete and every other")
        assertEquals(1, count(sheet, "private val MS_DEL_LINE = Color(SurveyPrepDraw.argb(DeleteGuard.OUTLINE))"))
        assertEquals(1, count(sheet, "private val MS_DEL = Color(SurveyPrepDraw.argb(DeleteGuard.INK))"))
        // the screen hands the panel the phase's acts and nothing reorders them on the way
        // (port 63 slice Q2 moved this pin: the panel is drawn WITH its shape row, `shape = true` -- Delete still last and
        //  apart with it drawn, which this file's figures hold: "the panel with the shape row drawn")
        assertEquals(1, count(c, "val drawn = MarkSheet.panel(tx, prep, pn, quoteStage, asPicked, shape = true)"))
        assertEquals(1, count(c, "onPanelAct = { act -> panelAct(act) },"))
        // and nothing in the 360 deletes or draws a delete control
        assertEquals(0, count(p3, "elete("), "the 360 deletes nothing by itself")
    }
}
