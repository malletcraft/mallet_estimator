package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.PhaseTools.AsChip
import com.malletcrafts.interiorestimate.pano.PhaseTools.Gate
import com.malletcrafts.interiorestimate.pano.PhaseTools.Tool
import com.malletcrafts.interiorestimate.pano.QuoteStage.Cache
import com.malletcrafts.interiorestimate.pano.QuoteStage.LineKind
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What each phase shows in survey prep and what it lets a mark do (port 62 slice L1), against the approved prototype
 * v61c.2 (index61c2.html, P54: PHASES 3931-3941, TOOLS 3947-3961, AS_FIXED 3964, moveOk 3985, noMoveText 3990, spPre
 * 4204-4224, editable / delOk 4232-4237, the refusals 4241-4250), cell by cell over SIX stages, unread, and a project
 * S5 does not list. The first five columns of every table are the prototype's own; the sixth, Client approved, is what
 * the page does there -- and, for the moves, the gate, the step strip and the delete control of a mark past Existing
 * (the L1 review), the phone's rule until Amit decides (DESIGN-port62 Q-list 6). Every expected value is written out
 * by hand.
 */
class PhaseToolsTest {

    /** Survey, Client wish list, Vendor quoted, Estimated, Revisions, Client approved -- and unread. */
    private val STAGES: List<Int?> = listOf(0, 1, 2, 3, 4, 5, null)
    private fun bits(f: (Int?) -> Boolean) = STAGES.joinToString("") { if (f(it)) "1" else "0" }

    private val now: Instant = Instant.parse("2026-11-11T05:00:00Z")
    private fun listed(stage: String) = Cache(listed = true, stage = stage, by = "pm@x", at = "2026-11-11T04:00:00.000Z", readAt = "2026-11-11T04:59:00Z")

    private fun mark(extra: Map<String, Any?> = emptyMap(), t: String = "W", code: String = "FW1") = SurveyPrep.Mark("a", t, code, 0.0, 1.0, 0.0, 1.0, extra)
    private fun mk(life: String, born: String = "exist", t: String = "W"): SurveyPrep.Mark {
        val x = LinkedHashMap<String, Any?>()
        if (born != "exist") x["born"] = born
        if (life != "exist") x["life"] = life
        return mark(x, t)
    }
    private fun meas(kind: String, code: String = "F-M1") = SurveyPrep.Meas("m", kind, code, mutableListOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(9.0, 0.0)))
    private fun chain(code: String = "FK1") = SurveyPrep.Chain("k", code, "x", mutableListOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(9.0, 0.0)))

    // ------------------------------------------------------------------ the stage

    @Test fun `the six stages are S5's quote stages by their place, and a phase is its stage's index`() {
        assertEquals(listOf("Survey", "Client wish list", "Vendor quoted", "Estimated", "Revisions", "Client approved"), QuoteStage.STAGES)
        assertEquals(listOf(0, 1, 2, 3, 4, 5),
            listOf(PhaseTools.SURVEY, PhaseTools.WISH_LIST, PhaseTools.VENDOR_REVIEW, PhaseTools.ESTIMATION, PhaseTools.REVISIONS, PhaseTools.APPROVED))
        assertEquals(6, PhaseTools.STAGES); assertEquals(QuoteStage.STAGES.size, PhaseTools.STAGES)
        assertEquals(PhaseTools.APPROVED, QuoteStage.indexOf(QuoteStage.APPROVED)); assertEquals(Phases.N, PhaseTools.APPROVED)
        assertEquals(Phases.SURVEY, PhaseTools.SURVEY); assertEquals(Phases.REVISIONS, PhaseTools.REVISIONS)
        // a stage that is none is unread
        assertEquals(listOf(0, 1, 2, 3, 4, 5, null), STAGES.map { PhaseTools.phaseOf(it) })
        for (bad in listOf(-1, 6, 9, 100)) assertNull(PhaseTools.phaseOf(bad), "$bad")
    }

    @Test fun `the stage survey prep is given - S5's index, Survey for a project S5 does not list, null while unread`() {
        for ((i, s) in QuoteStage.STAGES.withIndex()) assertEquals(i, PhaseTools.stageOf(listed(s), phoneOnly = false), s)
        assertEquals(1, PhaseTools.stageOf(listed("  Client wish list "), false), "trimmed, as the project line reads it")
        // made on this phone and never sent: Survey's rules, whatever the store holds for it
        assertEquals(0, PhaseTools.stageOf(null, phoneOnly = true)); assertEquals(0, PhaseTools.stageOf(listed("Vendor quoted"), phoneOnly = true))
        // read, and S5's list did not carry it: not on S5 either
        assertEquals(0, PhaseTools.stageOf(Cache(listed = false), false)); assertEquals(0, PhaseTools.stageOf(Cache(listed = false, stage = "Estimated"), false))
        // listed, its stage not read: never read, blank, a name outside the six
        assertNull(PhaseTools.stageOf(null, false)); assertNull(PhaseTools.stageOf(listed(""), false)); assertNull(PhaseTools.stageOf(listed("Quoted"), false))
        assertNull(PhaseTools.stageOf(listed("survey"), false), "S5's stages are compared as S5 spells them")
        // a stage an answer named without its stamp is still its stage: the tools follow it
        assertEquals(3, PhaseTools.stageOf(listed("Estimated").copy(at = "", atUnknown = true), false))
    }

    @Test fun `the stage is the project line's own decision -- the tools and the line cannot disagree`() {
        val cases: List<Pair<Cache?, Boolean>> = listOf(null to true, listed("Revisions") to true, Cache(listed = false) to false, null to false,
            listed("") to false, listed("nonsense") to false) + QuoteStage.STAGES.map { listed(it) to false }
        for ((cache, phoneOnly) in cases) {
            val line = QuoteStage.projectLine(cache, phoneOnly, emptyList(), Tx.EN, now)
            val want = when (line.kind) {
                LineKind.LOCAL -> 0                                  // not on S5: Survey's rules
                LineKind.UNREAD -> null
                LineKind.QUOTE -> QuoteStage.indexOf(line.quote!!)
                LineKind.EXEC -> 5
            }
            assertEquals(want, PhaseTools.stageOf(cache, phoneOnly), "${line.kind} ${cache?.stage} phoneOnly=$phoneOnly")
        }
    }

    // ------------------------------------------------------------------ TOOLS

    @Test fun `TOOLS - the prototype's five columns, cell by cell`() {
        // Survey, Wish list, Vendor review, Estimation, Revisions: 1 = shown, 0 = HIDDEN
        assertEquals(mapOf(
            "lines" to "10000",
            "mark" to "11101",
            "list" to "11111",
            "stateFilter" to "01111",
            "view360" to "11111",
            "palette" to "11101",
            "readings" to "10000",
            "trade" to "00101",
            "wishlist" to "01111",
            "wishEdit" to "01101",
            "vendorUI" to "00101",
            "vendorPacks" to "00101",
            "handoff" to "00010",
            "capture" to "10000",
        ), PhaseTools.TOOLS.entries.associate { (tool, row) -> tool.id to row.take(5).joinToString("") { if (it) "1" else "0" } })
        assertEquals(listOf("lines", "mark", "list", "stateFilter", "view360", "palette", "readings", "trade", "wishlist", "wishEdit", "vendorUI",
            "vendorPacks", "handoff", "capture"), PhaseTools.TOOLS.keys.map { it.id })
        assertEquals(Tool.values().toList(), PhaseTools.TOOLS.keys.toList(), "every tool has its row")
    }

    @Test fun `TOOLS - the sixth column, Client approved, written out tool by tool -- never a missing cell read as hidden`() {
        for ((tool, row) in PhaseTools.TOOLS) assertEquals(6, row.size, tool.id)
        assertEquals(mapOf(
            "lines" to false,        // can() hides it: a Lines step is moved on to List
            "mark" to false,         // can() hides it: a Mark step is moved on to List
            "list" to true,          // the page holds the mode on List there
            "stateFilter" to false,  // can() resets the List's state filter (the phone draws no State ▾ in any phase)
            "view360" to true,       // the 360 opens there; nothing hides it
            "palette" to false,      // can() puts the tool back to Select: no tools row
            "readings" to false,     // a reading is a figure without its box
            "trade" to false,        // can() puts the trade filter back to All (the phone draws no Trade chip)
            "wishlist" to true,      // the Wish list opens there, read-only
            "wishEdit" to false,     // locked at approval: changes go through change requests
            "vendorUI" to false,     // the Wish list draws no vendor field there
            "vendorPacks" to false,  // the strip is folded
            "handoff" to false,      // the strip is folded
            "capture" to true,       // Capture 360 is shown again in execution: a work-stage photo needs the capture sheet
        ), PhaseTools.TOOLS.entries.associate { (tool, row) -> tool.id to row[5] })
    }

    @Test fun `can - every tool at each of the six stages and unread, where every tool shows`() {
        //                             S W V E R A unread
        assertEquals(mapOf(
            Tool.LINES to "1000001",
            Tool.MARK to "1110101",
            Tool.LIST to "1111111",
            Tool.STATE_FILTER to "0111101",
            Tool.VIEW_360 to "1111111",
            Tool.PALETTE to "1110101",
            Tool.READINGS to "1000001",
            Tool.TRADE to "0010101",
            Tool.WISHLIST to "0111111",
            Tool.WISH_EDIT to "0110101",
            Tool.VENDOR_UI to "0010101",
            Tool.VENDOR_PACKS to "0010101",
            Tool.HANDOFF to "0001001",
            Tool.CAPTURE to "1000011",
        ), Tool.values().associateWith { t -> bits { PhaseTools.can(t, it) } })
        // a stage that is none is unread: everything shows
        for (t in Tool.values()) for (bad in listOf(-1, 6, 42)) assertTrue(PhaseTools.can(t, bad), "${t.id} at $bad")
        // a project S5 does not list is given Survey's tools
        val notOnS5 = PhaseTools.stageOf(null, phoneOnly = true)
        assertEquals(Tool.values().filter { PhaseTools.can(it, PhaseTools.SURVEY) }, Tool.values().filter { PhaseTools.can(it, notOnS5) })
        assertEquals(listOf("lines", "mark", "list", "view360", "palette", "readings", "capture"), Tool.values().filter { PhaseTools.can(it, notOnS5) }.map { it.id })
    }

    @Test fun `at Estimated and at Client approved the mark tools are hidden, so nothing is placed and the as-rule is never asked`() {
        for (stage in listOf(PhaseTools.ESTIMATION, PhaseTools.APPROVED)) {
            assertFalse(PhaseTools.can(Tool.MARK, stage), "2 · Mark at $stage"); assertFalse(PhaseTools.can(Tool.PALETTE, stage), "the palette at $stage")
            assertNull(PhaseTools.AS_FIXED[stage], "the phase fixes no 'as' at $stage")
            // no answer at all -- never the prototype's fall-through to a Client wish -- whatever a capture's button was cycled to
            for (picked in listOf(null, "exist", "wish", "vadd")) assertNull(PhaseTools.asFor(stage, picked), "asFor at $stage, picked $picked")
            assertEquals(AsChip.NONE, PhaseTools.asChip(stage)); assertNull(PhaseTools.nextAs(stage, "wish"))
            assertEquals(SurveyPrepPalette.SEL, PhaseTools.toolFor("W", stage)); assertEquals(SurveyPrepPalette.SEL, PhaseTools.toolFor(SurveyPrepPalette.LINE, stage))
            assertFalse("mark" in PhaseTools.steps(stage)); assertEquals("list", PhaseTools.modeFor("mark", stage, markable = true))
            // and so no mark is born a wish there: with nothing answered, placeAs writes nothing
            val e = mark(); assertNull(MarkLife.placeAs(e, PhaseTools.asFor(stage, "wish"), stage, now)); assertEquals(emptyMap(), e.extra)
        }
        // every other stage has both, together
        for (stage in listOf(0, 1, 2, 4, null)) { assertTrue(PhaseTools.can(Tool.MARK, stage), "$stage"); assertTrue(PhaseTools.can(Tool.PALETTE, stage), "$stage") }
    }

    @Test fun `Capture 360 is shown at Survey and again at Client approved, hidden between`() {
        assertEquals(listOf(true, false, false, false, false, true), (0..5).map { PhaseTools.can(Tool.CAPTURE, it) })
        assertTrue(PhaseTools.can(Tool.CAPTURE, PhaseTools.APPROVED), "a five-column port would hide the photos-by-work capture for good: approval is final")
        assertTrue(PhaseTools.can(Tool.CAPTURE, null)); assertTrue(PhaseTools.can(Tool.CAPTURE, PhaseTools.stageOf(null, phoneOnly = true)))
    }

    // ------------------------------------------------------------------ the step strip

    @Test fun `the steps the strip shows at each stage -- a step whose tool the phase hides is not drawn`() {
        assertEquals(listOf("lines", "mark", "list", "360"), StepRoute.STEPS)
        assertEquals(listOf(Tool.LINES, Tool.MARK, Tool.LIST, Tool.VIEW_360), StepRoute.STEPS.map { PhaseTools.toolOf(it) })
        assertNull(PhaseTools.toolOf("plan")); assertFalse(PhaseTools.stepShown("plan", null))
        assertEquals(listOf(
            "0: lines mark list 360",
            "1: mark list 360",
            "2: mark list 360",
            "3: list 360",
            "4: mark list 360",
            "5: list 360",          // 3 · List alone of the prototype's three: its steps 1 and 2 are drawn there but cannot be entered (Q-list 6)
            "null: lines mark list 360",
        ), STAGES.map { "$it: " + PhaseTools.steps(it).joinToString(" ") })
        assertEquals(PhaseTools.steps(0), PhaseTools.steps(PhaseTools.stageOf(null, phoneOnly = true)), "not on S5: Survey's strip")
        for (st in STAGES) for (s in StepRoute.STEPS) assertEquals(s in PhaseTools.steps(st), PhaseTools.stepShown(s, st), "$s at $st")
    }

    @Test fun `a step the phase hides is moved on - Lines to Mark where the face can be marked, else to List - Mark to List`() {
        fun row(mode: String, markable: Boolean) = STAGES.joinToString(" ") { PhaseTools.modeFor(mode, it, markable) }
        //                           Survey Wish  Vendor Estim. Revis. Appr.  unread
        assertEquals("lines mark mark list mark list lines", row("lines", true))
        assertEquals("lines list list list list list lines", row("lines", false), "a face not bounded yet cannot be marked")
        assertEquals("mark mark mark list mark list mark", row("mark", true))
        assertEquals("mark mark mark list mark list mark", row("mark", false))
        assertEquals("list list list list list list list", row("list", true))
        assertEquals("360 360 360 360 360 360 360", row("360", false))
        // the palette's tool goes back to Select where there is no palette, and is kept everywhere else
        assertEquals("W W W sel W sel W", STAGES.joinToString(" ") { PhaseTools.toolFor("W", it) })
        assertEquals("T T T sel T sel T", STAGES.joinToString(" ") { PhaseTools.toolFor(SurveyPrepPalette.TAPE, it) })
        assertEquals("sel sel sel sel sel sel sel", STAGES.joinToString(" ") { PhaseTools.toolFor(SurveyPrepPalette.SEL, it) })
    }

    // ------------------------------------------------------------------ what a new mark is placed as

    @Test fun `AS_FIXED - what a mark is placed as in each of the six stages`() {
        assertEquals(listOf(null, "wish", "vadd", null, "vadd", null), PhaseTools.AS_FIXED)
        assertEquals(6, PhaseTools.AS_FIXED.size)
        assertEquals(listOf("exist", "wish", "vadd"), PhaseTools.AS_CYCLE)
        //                    Survey  Wish  Vendor Estim. Revis. Appr. unread
        assertEquals(listOf("exist", "wish", "vadd", null, "vadd", null, "exist"), STAGES.map { PhaseTools.asFor(it) })
        // what a capture's button was cycled to counts only while the stage is unread
        for (picked in listOf("exist", "wish", "vadd")) {
            assertEquals(listOf("exist", "wish", "vadd", null, "vadd", null, picked), STAGES.map { PhaseTools.asFor(it, picked) }, picked)
        }
        assertEquals("exist", PhaseTools.asFor(null, "cok"), "anything else is Existing"); assertEquals("exist", PhaseTools.asFor(null, ""))
        // a project S5 does not list is at Survey: every mark is Existing, so no wish is made during a first survey
        assertEquals("exist", PhaseTools.asFor(PhaseTools.stageOf(null, phoneOnly = true), "wish"))
        // every answer is one placeAs takes or ignores: a wish, Vendor added, or Existing (no key written)
        for (st in STAGES) assertTrue(PhaseTools.asFor(st) in listOf(null, MarkLife.EXIST, MarkLife.WISH, MarkLife.VADD), "$st")
    }

    @Test fun `the tool chip's second half - none at Survey, the phase's own words where it is fixed, the cycling button only while unread`() {
        assertEquals(listOf(AsChip.NONE, AsChip.FIXED, AsChip.FIXED, AsChip.NONE, AsChip.FIXED, AsChip.NONE, AsChip.CYCLE), STAGES.map { PhaseTools.asChip(it) })
        // Existing → Client wish → Vendor added → Existing, per capture
        assertEquals("wish", PhaseTools.nextAs(null, null)); assertEquals("wish", PhaseTools.nextAs(null, "exist"))
        assertEquals("vadd", PhaseTools.nextAs(null, "wish")); assertEquals("exist", PhaseTools.nextAs(null, "vadd"))
        assertEquals("wish", PhaseTools.nextAs(null, "junk"), "a stored choice that is none reads Existing")
        for (st in 0..5) assertNull(PhaseTools.nextAs(st, "wish"), "nothing to pick at $st")
    }

    // ------------------------------------------------------------------ the moves a phase offers

    @Test fun `moveOk - every row of the life-cycle table at each of the six stages and unread`() {
        //                                    S W V E R A unread
        val want = listOf(
            "exist>wish 0100001",   // a wish is made in the Wish list phase alone
            "wish>vrev 0010101",
            "wish>drop 0110101",
            "wish>exist 0100001",   // and unmade there alone
            "vrev>vok 0010101",
            "vrev>wish 0010101",
            "vrev>drop 0010101",
            "vok>cok 0010101",
            "vok>wish 0010101",
            "vok>drop 0010101",
            "vadd>cok 0010101",
            "vadd>drop 0010101",
            "cok>scope 0010101",
            "cok>wish 0010101",
            "cok>drop 0010101",
            "scope>cok 0010101",
            "drop>wish 0110101",    // Restore
        )
        assertEquals(want, MarkLife.MOVE_TABLE.map { m -> "${m.from}>${m.to} " + bits { PhaseTools.moveOk(m.from, m, it) } })
        // Survey, Estimation and -- on the phone -- Client approved offer NO move (the prototype's own column there is Vendor review's)
        for (st in listOf(PhaseTools.SURVEY, PhaseTools.ESTIMATION, PhaseTools.APPROVED)) assertTrue(MarkLife.MOVE_TABLE.none { PhaseTools.moveOk(it.from, it, st) }, "$st")
        // Revisions offers exactly what Vendor review does; unread, every row
        assertEquals(MarkLife.MOVE_TABLE.map { PhaseTools.moveOk(it.from, it, 2) }, MarkLife.MOVE_TABLE.map { PhaseTools.moveOk(it.from, it, 4) })
        assertTrue(MarkLife.MOVE_TABLE.all { PhaseTools.moveOk(it.from, it, null) })
        assertFalse(MarkLife.MOVE_TABLE.any { PhaseTools.moveOk(it.from, it, PhaseTools.stageOf(null, phoneOnly = true)) }, "not on S5: Survey's gate")
    }

    @Test fun `the gate - Survey's and Client approved's - and its line in both languages`() {
        assertEquals(listOf(Gate.SURVEY, null, null, null, null, Gate.APPROVED, null), STAGES.map { PhaseTools.gate(it) })
        assertEquals(Gate.SURVEY, PhaseTools.gate(PhaseTools.stageOf(null, phoneOnly = true)), "a project not on S5: Finish the survey first")
        assertEquals("Finish the survey first — the stage moves to Client wish list on S5", PhaseTools.gateText(Tx.EN, Gate.SURVEY))
        assertEquals("आधी सर्व्हे पूर्ण करा — S5 वर टप्पा \"ग्राहकाची इच्छा-यादी\" वर जातो", PhaseTools.gateText(Tx.MR, Gate.SURVEY))
        // the approval lock's own words, as the Wish list's banner says them there
        assertEquals("Approved — changes go through change requests", PhaseTools.gateText(Tx.EN, Gate.APPROVED))
        assertEquals("मंजूर झाले — पुढचे बदल \"बदल विनंती\" मधूनच होतात", PhaseTools.gateText(Tx.MR, Gate.APPROVED))
        // a gate is exactly a stage that offers no move and is not Estimation, which has its own line
        for (st in STAGES) if (PhaseTools.gate(st) != null) assertTrue(MarkLife.MOVE_TABLE.none { PhaseTools.moveOk(it.from, it, st) }, "$st")
    }

    @Test fun `the line the Mark sheet shows when the phase leaves a mark no move`() {
        fun en(life: String, st: Int?) = PhaseTools.noMoveText(Tx.EN, life, st)
        assertEquals("Existing · its next move is in Vendor review", en("exist", 1))
        assertEquals("Vendor review · its next move is in Vendor review", en("vrev", 1))
        assertEquals("Existing · a wish is marked in the Wish list phase", en("exist", 2)); assertEquals("Existing · a wish is marked in the Wish list phase", en("exist", 4))
        assertEquals("No move from Client wish", en("wish", 2)); assertEquals("No move from Scope", en("scope", 4))
        for (life in MarkLife.IDS) assertEquals(MarkLife.name(Tx.EN, life) + " · Estimation moves nothing — move the phase back on the project screen", en(life, 3), life)
        assertEquals("Client wish · Estimation moves nothing — move the phase back on the project screen", en("wish", 3))
        // Survey and Client approved show their gate instead; asked anyway, and unread, it is the plain line
        for (st in listOf(0, 5, null)) { assertEquals("No move from Existing", en("exist", st), "$st"); assertEquals("No move from Client wish", en("wish", st), "$st") }
        assertEquals("सध्याचे · पुढचे पाऊल \"व्हेंडर तपासणी\" मध्ये आहे", PhaseTools.noMoveText(Tx.MR, "exist", 1))
        assertEquals("सध्याचे · इच्छा \"इच्छा-यादी\" फेजमध्ये मार्क होते", PhaseTools.noMoveText(Tx.MR, "exist", 2))
        assertEquals("ग्राहकाची इच्छा · अंदाजपत्रक फेजमध्ये काहीही हलवता येत नाही — प्रोजेक्टच्या स्क्रीनवर फेज मागे न्या", PhaseTools.noMoveText(Tx.MR, "wish", 3))
        assertEquals("स्कोप वरून हलवता येत नाही", PhaseTools.noMoveText(Tx.MR, "scope", 4))
    }

    // ------------------------------------------------------------------ marks of earlier phases are read-only

    @Test fun `editable and delOk - each kind of item at each of the six stages and unread`() {
        // each row: edit, then delete, at Survey, Wish list, Vendor quoted, Estimated, Revisions, Client approved, unread
        val want = listOf(
            "a surveyed mark: 1000001 1000001",
            "a wish on a surveyed mark: 1000001 1110101",   // Delete is Drop there: a move, which the phase filters
            "a mark born a wish: 1100001 1110101",
            "a mark born Vendor added: 1010111 1110101",    // still editable after approval, but its Delete is a Drop
            "a Dropped surveyed mark: 1000001 1110101",     // Delete is Restore
            "a Note: 1110111 1110111",
            "a Line: 1110111 1110111",
            "a Tape: 1110111 1110111",
            "a chain: 1110111 1110111",
        )
        val got = listOf("a surveyed mark" to mk("exist"), "a wish on a surveyed mark" to mk("wish"), "a mark born a wish" to mk("wish", "wish"),
            "a mark born Vendor added" to mk("vadd", "vadd"), "a Dropped surveyed mark" to mk("drop"), "a Note" to mark(t = "NOTE"))
            .map { (what, e) -> "$what: ${bits { PhaseTools.editable(e, it) }} ${bits { PhaseTools.delOk(e, it) }}" } +
            listOf("a Line" to meas("line"), "a Tape" to meas("tape"))
                .map { (what, m) -> "$what: ${bits { PhaseTools.editable(m, it) }} ${bits { PhaseTools.delOk(m, it) }}" } +
            "a chain: ${bits { PhaseTools.editable(chain(), it) }} ${bits { PhaseTools.delOk(chain(), it) }}"
        assertEquals(want, got)
        // nothing at all changes at Estimation; everything at Survey, unread, and for a project not on S5
        val notOnS5 = PhaseTools.stageOf(null, phoneOnly = true)
        for (e in listOf(mk("exist"), mk("wish", "wish"), mk("vadd", "vadd"), mk("scope"), mark(t = "NOTE"))) {
            assertFalse(PhaseTools.editable(e, 3)); assertFalse(PhaseTools.delOk(e, 3))
            for (st in listOf(0, null, notOnS5)) { assertTrue(PhaseTools.editable(e, st)); assertTrue(PhaseTools.delOk(e, st)) }
        }
        // born is what counts, not the state it has reached: a wish the vendor approved is still the Wish list's to edit
        assertTrue(PhaseTools.editable(mk("vok", "wish"), 1)); assertFalse(PhaseTools.editable(mk("vok", "wish"), 2))
        assertTrue(PhaseTools.editable(mk("cok", "vadd"), 4)); assertFalse(PhaseTools.editable(mk("cok", "vadd"), 1))
        // a Note carrying a life is still a Note: its delete follows editable, never the Drop rule
        assertEquals(bits { PhaseTools.editable(mark(t = "NOTE"), it) }, bits { PhaseTools.delOk(mk("wish", t = "NOTE"), it) })
    }

    /**
     * The L1 review, finding 1. The prototype keeps Delete on a mark past Existing because it is "a life-cycle move, which
     * the phase already filters (moveOk) -- so it stays, except in Estimation" (index61c2.html 4229-4230). The phone offers
     * no move at Client approved either (Q-list 6), so there the same reason hides it: the control could only open the
     * sheet on the approval lock's line (Drop) or be refused with it (Restore).
     */
    @Test fun `at Client approved a delete control that shows really deletes -- a mark past Existing has none, as at Estimation`() {
        val lives = MarkLife.IDS.filter { it != MarkLife.EXIST }
        assertEquals(7, lives.size)
        for (life in lives) for (born in listOf("exist", "wish", "vadd")) for (locked in listOf(false, true)) {
            val e = mk(life, born).also { if (locked) it.extra = it.extra + (SurveyPrep.LOCKED to true) }
            val what = "$life born $born${if (locked) ", locked" else ""}"
            // what the control would do there: nothing but refuse
            assertTrue(MarkLife.allowedMoves(e, PhaseTools.APPROVED).isEmpty(), what)
            assertEquals(MarkLife.Refusal.APPROVED, MarkLife.move(e, MarkLife.DROP, PhaseTools.APPROVED, now, why = "client changed her mind").refusal, what)
            assertEquals(MarkLife.Refusal.APPROVED, MarkLife.restore(e, PhaseTools.APPROVED, now).refusal, what)
            // so it is not drawn -- at Client approved as at Estimation
            assertFalse(PhaseTools.delOk(e, PhaseTools.APPROVED), what)
            assertFalse(PhaseTools.delOk(e, PhaseTools.ESTIMATION), what)
            // where the phase does offer moves, Drop / Restore stays on every one of them, as the prototype has it
            for (st in listOf(PhaseTools.WISH_LIST, PhaseTools.VENDOR_REVIEW, PhaseTools.REVISIONS, null)) assertTrue(PhaseTools.delOk(e, st), "$what at $st")
        }
        // every delete control that does show at Client approved is v52's own delete, never a Drop or a Restore
        val shown = (MarkLife.IDS.flatMap { life -> listOf("exist", "wish", "vadd").map { mk(life, it) } } + mark(t = "NOTE") + mk("wish", t = "NOTE"))
            .filter { PhaseTools.delOk(it, PhaseTools.APPROVED) }
        assertTrue(shown.isNotEmpty())
        for (e in shown) {
            assertEquals(MarkLife.DeleteAct.PLAIN, MarkLife.deleteAct(e), e.extra.toString())
            assertEquals(MarkLife.DeleteWord.DELETE, MarkLife.deleteWord(e), e.extra.toString())
        }
        // a Note is still deleted there, and a Line, a Tape and a chain: the prototype's own answer for a stage past Estimation
        assertTrue(PhaseTools.delOk(mark(t = "NOTE"), PhaseTools.APPROVED)); assertTrue(PhaseTools.delOk(mk("wish", t = "NOTE"), PhaseTools.APPROVED))
        assertTrue(PhaseTools.delOk(meas("line"), PhaseTools.APPROVED)); assertTrue(PhaseTools.delOk(meas("tape"), PhaseTools.APPROVED)); assertTrue(PhaseTools.delOk(chain(), PhaseTools.APPROVED))
        // and a surveyed mark has no delete there, as before: nothing born in Survey is the stage's to change
        assertFalse(PhaseTools.delOk(mk("exist"), PhaseTools.APPROVED))
    }

    /**
     * Slice L2, as the L1 review left it ("L2 decides whether that padlock is hidden there too"): a mark in Scope stays
     * locked, and its padlock answers "take it Out of scope in its sheet" -- a move the phone's sheet does not offer at
     * Client approved. There the padlock could only refuse, so it is not drawn; everywhere else it follows editable.
     */
    @Test fun `the padlock follows editable -- but a locked mark in Scope has none at Client approved, where it could only refuse`() {
        fun locked(e: SurveyPrep.Mark) = e.also { it.extra = it.extra + (SurveyPrep.LOCKED to true) }
        // the one cell: Scope, locked, born Vendor added (the only mark still editable there), at Client approved
        val scope = locked(mk("scope", "vadd"))
        assertEquals("1010111", bits { PhaseTools.editable(scope, it) }); assertEquals("1010101", bits { PhaseTools.lockOk(scope, it) })
        // what its padlock would do there: refuse, with words that point at a move nothing offers
        assertEquals("FW1 is in Scope — take it Out of scope in its sheet", MarkLife.unlockRefusal(Tx.EN, scope))
        assertTrue(MarkLife.allowedMoves(scope, PhaseTools.APPROVED).isEmpty())
        // ... while at Vendor review and Revisions the sheet offers ← Out of scope, so the padlock stays and its words can be followed
        for (st in listOf(PhaseTools.VENDOR_REVIEW, PhaseTools.REVISIONS)) assertEquals(listOf(MarkLife.COK), MarkLife.allowedMoves(scope, st).map { it.to }, "$st")
        // every other mark, state and lock: exactly editable, at every stage
        val marks = MarkLife.IDS.flatMap { life -> listOf("exist", "wish", "vadd").flatMap { born -> listOf(mk(life, born), locked(mk(life, born))) } } +
            mark(t = "NOTE") + locked(mk("scope", t = "NOTE"))
        for (e in marks) for (st in STAGES) {
            val theCell = st == PhaseTools.APPROVED && MarkLife.lifeOf(e) == MarkLife.SCOPE && SurveyPrep.isLocked(e) && MarkLife.bornOf(e) == MarkLife.VADD
            assertEquals(if (theCell) false else PhaseTools.editable(e, st), PhaseTools.lockOk(e, st), "${e.t} ${e.extra} at $st")
        }
        // a Scope mark that is NOT locked (a file may hold one) keeps its padlock: locking it is an act that works
        assertTrue(PhaseTools.lockOk(mk("scope", "vadd"), PhaseTools.APPROVED))
        // a Note has no state; a Line, a Tape and a chain lock as they are edited
        assertTrue(PhaseTools.lockOk(locked(mk("scope", t = "NOTE")), PhaseTools.APPROVED))
        assertEquals(bits { PhaseTools.editable(meas("line"), it) }, bits { PhaseTools.lockOk(meas("line"), it) })
        assertEquals(bits { PhaseTools.editable(meas("tape"), it) }, bits { PhaseTools.lockOk(meas("tape"), it) })
        assertEquals(bits { PhaseTools.editable(chain(), it) }, bits { PhaseTools.lockOk(chain(), it) })
    }

    @Test fun `Clear, Lock all and Start over act on every phase's marks - Survey only, and unread`() {
        assertEquals("1000001", bits { PhaseTools.bulk(it) })
        assertTrue(PhaseTools.bulk(PhaseTools.stageOf(null, phoneOnly = true)))
    }

    @Test fun `what a refused act says - the phase the mark was born in, or that Estimation changes nothing`() {
        fun en(e: SurveyPrep.Mark, st: Int?) = PhaseTools.refuseText(Tx.EN, e, st)
        assertEquals("FW1 was marked in Survey — move the phase back to change it", en(mk("exist"), 1))
        assertEquals("FW1 was marked in Survey — move the phase back to change it", en(mk("wish"), 2), "a wish on a surveyed mark was born in Survey")
        assertEquals("FW1 was marked in Wish list — move the phase back to change it", en(mk("wish", "wish"), 2))
        assertEquals("FW1 was marked in Wish list — move the phase back to change it", en(mk("vok", "wish"), 4))
        assertEquals("FW1 was marked in Vendor review — move the phase back to change it", en(mk("vadd", "vadd"), 1))
        for (e in listOf(mk("exist"), mk("wish", "wish"), mk("vadd", "vadd")))
            assertEquals("FW1 · Estimation changes nothing — move the phase back on the project screen", en(e, 3))
        // a Line and a chain are refused at Estimation alone, by their code
        assertEquals("F-M1 · Estimation changes nothing — move the phase back on the project screen", PhaseTools.refuseText(Tx.EN, meas("line"), 3))
        assertEquals("FK1 · Estimation changes nothing — move the phase back on the project screen", PhaseTools.refuseText(Tx.EN, chain(), 3))
        assertEquals("F-T1 was marked in Survey — move the phase back to change it", PhaseTools.refuseText(Tx.EN, meas("tape", "F-T1"), 1))
        // an item with no code is "This"
        assertEquals("This · Estimation changes nothing — move the phase back on the project screen", en(mark(code = ""), 3))
        assertEquals("This was marked in Survey — move the phase back to change it", en(mark(code = ""), 1))
        // मराठी: the phase's name is the strip's own
        assertEquals("FW1 सर्व्हे मध्ये मार्क झाला होता — बदलण्यासाठी फेज मागे न्या", PhaseTools.refuseText(Tx.MR, mk("exist"), 1))
        assertEquals("FW1 इच्छा-यादी मध्ये मार्क झाला होता — बदलण्यासाठी फेज मागे न्या", PhaseTools.refuseText(Tx.MR, mk("wish", "wish"), 2))
        assertEquals("FW1 व्हेंडर तपासणी मध्ये मार्क झाला होता — बदलण्यासाठी फेज मागे न्या", PhaseTools.refuseText(Tx.MR, mk("vadd", "vadd"), 1))
        assertEquals("FW1 · अंदाजपत्रक फेजमध्ये काहीही बदलता येत नाही — प्रोजेक्टच्या स्क्रीनवर फेज मागे न्या", PhaseTools.refuseText(Tx.MR, mk("exist"), 3))
        assertEquals("ही · अंदाजपत्रक फेजमध्ये काहीही बदलता येत नाही — प्रोजेक्टच्या स्क्रीनवर फेज मागे न्या", PhaseTools.refuseText(Tx.MR, mark(code = ""), 3))
    }

    @Test fun `what a refused Clear, Lock all, Start over and reading say`() {
        assertEquals("Clearing and locking every mark is a Survey tool — move the phase back to use it", PhaseTools.refuseBulk(Tx.EN, 1))
        assertEquals("Clearing and locking every mark is a Survey tool — move the phase back to use it", PhaseTools.refuseBulk(Tx.EN, 4))
        assertEquals("This · Estimation changes nothing — move the phase back on the project screen", PhaseTools.refuseBulk(Tx.EN, 3))
        assertEquals("सर्व मार्क काढणे आणि लॉक करणे हे सर्व्हेचे साधन आहे — वापरण्यासाठी फेज मागे न्या", PhaseTools.refuseBulk(Tx.MR, 2))
        assertEquals("Start over is a Survey tool — move the phase back to use it", PhaseTools.refuseStartOver(Tx.EN))
        assertEquals("पुन्हा सुरुवात हे सर्व्हेचे साधन आहे — वापरण्यासाठी फेज मागे न्या", PhaseTools.refuseStartOver(Tx.MR))
        // a reading is keyed in the Survey phase alone
        assertEquals(listOf(false, true, true, true, true, true, false), STAGES.map { PhaseTools.readingRefusal(Tx.EN, it) != null })
        assertEquals("Readings are taken in the Survey phase — move the phase back on the project screen to key one", PhaseTools.readingRefusal(Tx.EN, 2))
        assertEquals("रीडिंग सर्व्हे फेजमध्ये घेतले जातात — रीडिंग टाकण्यासाठी प्रोजेक्टच्या स्क्रीनवर फेज मागे न्या", PhaseTools.readingRefusal(Tx.MR, 2))
        assertNull(PhaseTools.readingRefusal(Tx.EN, PhaseTools.stageOf(null, phoneOnly = true)))
    }

    // ------------------------------------------------------------------ the band

    @Test fun `the band - the phase's number and name, then its words, the pips on up to it`() {
        assertEquals(listOf(
            "1/5 | 1 · Survey · capture · bound · mark what is there · readings",
            "2/5 | 2 · Wish list · every mark you place is a client wish",
            "3/5 | 3 · Vendor review · trades · vendor packs · new marks are vendor added",
            "4/5 | 4 · Estimation · hand-off · read-only · nothing new is marked",
            "5/5 | 5 · Revisions · iterate the estimate · marks and packs move · nothing new is surveyed",
        ), (0..4).map { PhaseTools.band(Tx.EN, it).let { b -> "${b.lit}/5 | ${b.whole}" } })
        val wish = PhaseTools.band(Tx.EN, PhaseTools.WISH_LIST)
        assertEquals("2 · Wish list", wish.head); assertEquals("every mark you place is a client wish", wish.tail); assertFalse(wish.unread)
        assertEquals("Phase 2 of 5: Wish list — every mark you place is a client wish. The phase moves on the project screen.", wish.label)
        assertEquals("Phase 4 of 5: Estimation — hand-off · read-only · nothing new is marked. The phase moves on the project screen.", PhaseTools.band(Tx.EN, 3).label)
        // the names are the strip's five, so the band and the strip never name a phase two ways
        assertEquals(Phases.names(Tx.EN), (0..4).map { PhaseTools.band(Tx.EN, it).head.substringAfter(" · ") })
        // a line for the project changes nothing before approval
        assertEquals(PhaseTools.band(Tx.EN, 2), PhaseTools.band(Tx.EN, 2, "Execution · 1 in progress"))
        assertEquals(PhaseTools.band(Tx.EN, 0), PhaseTools.band(Tx.EN, PhaseTools.stageOf(null, phoneOnly = true)), "not on S5: Survey's band")
    }

    @Test fun `the band in Marathi`() {
        assertEquals(listOf(
            "1 · सर्व्हे · कॅप्चर · हद्द · जे आहे ते मार्क · रीडिंग",
            "2 · इच्छा-यादी · तुम्ही ठेवलेला प्रत्येक मार्क ग्राहकाची इच्छा असतो",
            "3 · व्हेंडर तपासणी · ट्रेड · व्हेंडर पॅक · नवीन मार्क = व्हेंडरने जोडले",
            "4 · अंदाजपत्रक · सुपूर्द · फक्त वाचन · नवीन काहीही मार्क होत नाही",
            "5 · फेरबदल · अंदाजपत्रकात फेरबदल · मार्क व पॅक हलतात · नवीन सर्व्हे नाही",
        ), (0..4).map { PhaseTools.band(Tx.MR, it).whole })
        assertEquals("फेज 2/5: इच्छा-यादी — तुम्ही ठेवलेला प्रत्येक मार्क ग्राहकाची इच्छा असतो. फेज प्रोजेक्टच्या स्क्रीनवर पुढे सरकते.", PhaseTools.band(Tx.MR, 1).label)
        assertEquals("ग्राहक मंजूर · अंमलबजावणी · 1 चालू", PhaseTools.band(Tx.MR, 5, "अंमलबजावणी · 1 चालू").whole)
        assertEquals("फेज अजून वाचलेली नाही — ऑनलाइन आल्यावर S5 ठरवते", PhaseTools.band(Tx.MR, null).whole)
    }

    @Test fun `the band at Client approved carries the project's one line, and unread it says the phase is not read`() {
        val exec = PhaseTools.band(Tx.EN, PhaseTools.APPROVED, "Execution · 1 in progress")
        assertEquals(5, exec.lit, "every pip on"); assertEquals("Client approved", exec.head); assertEquals("Execution · 1 in progress", exec.tail)
        assertEquals("Client approved · Execution · 1 in progress", exec.whole); assertEquals("Client approved · Execution · 1 in progress", exec.label); assertFalse(exec.unread)
        // with no line for the project, the name alone
        for (none in listOf(null, "")) {
            val bare = PhaseTools.band(Tx.EN, 5, none)
            assertEquals("Client approved", bare.whole); assertEquals("Client approved", bare.label); assertEquals(5, bare.lit)
        }
        // the line is the project line's own text
        val states = mapOf(3 to Album.WorkState(Album.IN_PROGRESS, 7))
        val line = QuoteStage.projectLine(listed("Client approved").copy(states = states), false, emptyList(), Tx.EN, now)
        assertEquals("Client approved · " + line.text, PhaseTools.band(Tx.EN, PhaseTools.stageOf(listed("Client approved"), false), line.text).whole)
        assertTrue(line.text.startsWith("Execution"))
        val unread = PhaseTools.band(Tx.EN, null, "Execution · 1 in progress")
        assertEquals(PhaseTools.Band(0, "", "Phase not read yet — S5 decides when online", "Phase not read yet — S5 decides when online", unread = true), unread)
        assertEquals("Phase not read yet — S5 decides when online", unread.whole)
        assertEquals(unread, PhaseTools.band(Tx.EN, 9), "a stage that is none is unread")
    }
}
