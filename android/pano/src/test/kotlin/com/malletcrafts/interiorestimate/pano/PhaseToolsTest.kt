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


    // ------------------------------------------------------------------ slice L4: the phase in survey prep, as wired

    /** One row of DESIGN-port62 section 3.4's table: 2 · Mark and the palette, the chip's second half, the band. */
    private fun l4row(stage: Int?, line: String? = null): String {
        val shown = PhaseTools.can(Tool.MARK, stage) && PhaseTools.can(Tool.PALETTE, stage)
        assertEquals(PhaseTools.can(Tool.MARK, stage), PhaseTools.can(Tool.PALETTE, stage), "2 · Mark and its palette go together at $stage")
        val chip = SurveyPrepPalette.chip(Tx.EN, "front", PhaseTools.toolFor("W", stage), stage, null)
        val half = when {
            chip == null -> "never drawn"
            chip.asText == null -> "none"
            chip.cycles -> "button: " + chip.asText
            else -> "plain: " + chip.asText
        }
        return (if (shown) "shown" else "HIDDEN") + " | " + half + " | " + PhaseTools.band(Tx.EN, stage, line).whole
    }

    @Test fun `L4 - the plan's table row by row - 2 Mark and the palette, the chip's second half, the band`() {
        // driven on the approved page (v61c.2, 360 x 780, the Demo project) at all six stages and unread, 11 Nov 2026
        assertEquals("shown | none | 1 · Survey · capture · bound · mark what is there · readings", l4row(0))
        assertEquals("shown | plain: as Client wish | 2 · Wish list · every mark you place is a client wish", l4row(1))
        assertEquals("shown | plain: as Vendor added | 3 · Vendor review · trades · vendor packs · new marks are vendor added", l4row(2))
        assertEquals("HIDDEN | never drawn | 4 · Estimation · hand-off · read-only · nothing new is marked", l4row(3))
        assertEquals("shown | plain: as Vendor added | 5 · Revisions · iterate the estimate · marks and packs move · nothing new is surveyed", l4row(4))
        assertEquals("HIDDEN | never drawn | Client approved · Execution · 1 in progress · 1 done", l4row(5, "Execution · 1 in progress · 1 done"))
        assertEquals("shown | button: as Existing ▾ | Phase not read yet — S5 decides when online", l4row(null))
        // a project S5 does not list is given Survey's stage: Survey's row, never a wish (Amit, 14 Oct: "the wishlist
        // should never get mixed with survey")
        assertEquals(l4row(0), l4row(PhaseTools.stageOf(null, phoneOnly = true)))
        assertEquals(l4row(0), l4row(PhaseTools.stageOf(Cache(listed = false, stage = "Client wish list", by = "", at = "", readAt = "2026-11-11T04:59:00Z"), false)))
    }

    @Test fun `L4 - the strip survey prep draws - no 2 Mark at Estimated and at Client approved, 1 Lines kept until slice L10`() {
        assertEquals(setOf(Tool.MARK, Tool.PALETTE), PhaseTools.PREP_HONOURS)
        val all = listOf("lines", "mark", "list", "360"); val noMark = listOf("lines", "list", "360")
        assertEquals(listOf(all, all, all, noMark, all, noMark, all), STAGES.map { PhaseTools.prepSteps(it) })
        // every step it draws is one of the strip's, in the strip's order, so each keeps its own number
        for (st in STAGES) assertEquals(PhaseTools.prepSteps(st), StepRoute.STEPS.filter { it in PhaseTools.prepSteps(st) })
        // what it shows is the table's cell for a row it honours, and shown for one it does not yet
        for (t in Tool.entries) assertEquals(STAGES.joinToString("") { if (t !in PhaseTools.PREP_HONOURS || PhaseTools.can(t, it)) "1" else "0" },
            bits { PhaseTools.prepShows(t, it) }, t.id)
        assertEquals("1110101", bits { PhaseTools.prepShows(Tool.MARK, it) }); assertEquals("1110101", bits { PhaseTools.prepShows(Tool.PALETTE, it) })
        assertEquals("1111111", bits { PhaseTools.prepShows(Tool.LINES, it) }, "slice L10's row: 1 · Lines stays in every phase until then")
        assertEquals("1111111", bits { PhaseTools.prepShows(Tool.READINGS, it) }, "slice L10's row")
        // ... which is exactly where it differs from the whole table: the two L10 rows
        assertEquals(listOf("", "lines", "lines", "lines", "lines", "lines", ""), STAGES.map { st -> (PhaseTools.prepSteps(st) - PhaseTools.steps(st).toSet()).joinToString(",") })
        for (st in STAGES) assertTrue(PhaseTools.prepSteps(st).containsAll(PhaseTools.steps(st)), "$st: nothing the whole table shows is missing")
    }

    @Test fun `L4 - a step and a tool the phase does not show are moved on - Mark to List, the tool to Select`() {
        val never = { error("1 · Lines is not hidden by this build, so whether the face can be marked is never asked") }
        fun row(mode: String) = STAGES.joinToString(" ") { PhaseTools.prepMode(mode, it, never) }
        assertEquals("mark mark mark list mark list mark", row("mark"))
        assertEquals("lines lines lines lines lines lines lines", row("lines"))
        assertEquals("list list list list list list list", row("list"))
        assertEquals("360 360 360 360 360 360 360", row("360"))
        // the step it lands on is always one the strip draws
        for (st in STAGES) for (m in StepRoute.STEPS) assertTrue(PhaseTools.prepMode(m, st, never) in PhaseTools.prepSteps(st), "$m at $st")
        // the tool: a type, a Tape, a Line and a Chain all go back to Select where there is no palette
        for (t in listOf("W", "E", "T", "M", "K")) assertEquals(listOf(t, t, t, "sel", t, "sel", t), STAGES.map { PhaseTools.toolFor(t, it) }, t)
        assertEquals(List(7) { "sel" }, STAGES.map { PhaseTools.toolFor("sel", it) })
    }

    @Test fun `L4 - the band's two parts as drawn - the bold head, then the dot and the words`() {
        fun parts(b: PhaseTools.Band) = "${b.lit} [${b.head}] [${b.rest}]"
        assertEquals("1 [1 · Survey] [· capture · bound · mark what is there · readings]", parts(PhaseTools.band(Tx.EN, 0)))
        assertEquals("2 [2 · Wish list] [· every mark you place is a client wish]", parts(PhaseTools.band(Tx.EN, 1)))
        assertEquals("3 [3 · Vendor review] [· trades · vendor packs · new marks are vendor added]", parts(PhaseTools.band(Tx.EN, 2)))
        assertEquals("4 [4 · Estimation] [· hand-off · read-only · nothing new is marked]", parts(PhaseTools.band(Tx.EN, 3)))
        assertEquals("5 [5 · Revisions] [· iterate the estimate · marks and packs move · nothing new is surveyed]", parts(PhaseTools.band(Tx.EN, 4)))
        assertEquals("5 [Client approved] [· Execution · 1 in progress · 1 done]", parts(PhaseTools.band(Tx.EN, 5, "Execution · 1 in progress · 1 done")))
        assertEquals("5 [Client approved] []", parts(PhaseTools.band(Tx.EN, 5, null)), "no line to say: the name alone, no dot")
        assertEquals("0 [] [Phase not read yet — S5 decides when online]", parts(PhaseTools.band(Tx.EN, null)), "unread: the words alone, amber")
        assertTrue(PhaseTools.band(Tx.EN, null).unread)
        // the two parts and the gap between them are the whole line
        for (st in STAGES) PhaseTools.band(Tx.EN, st, "Execution · 2 done").let { b ->
            assertEquals(b.whole, listOf(b.head, b.rest).filter { it.isNotEmpty() }.joinToString(" "), "$st") }
        // as the page draws it under मराठी (driven)
        assertEquals("2 [2 · इच्छा-यादी] [· तुम्ही ठेवलेला प्रत्येक मार्क ग्राहकाची इच्छा असतो]", parts(PhaseTools.band(Tx.MR, 1)))
        assertEquals("0 [] [फेज अजून वाचलेली नाही — ऑनलाइन आल्यावर S5 ठरवते]", parts(PhaseTools.band(Tx.MR, null)))
        // what a screen reader is told
        assertEquals("Phase 2 of 5: Wish list — every mark you place is a client wish. The phase moves on the project screen.", PhaseTools.band(Tx.EN, 1).label)
        assertEquals("Client approved · Execution · 1 in progress · 1 done", PhaseTools.band(Tx.EN, 5, "Execution · 1 in progress · 1 done").label)
    }

    /** The picked item's controls the phase leaves: nudges (and the eight tabs), the padlock, the delete control. */
    private fun ctl(p: SurveyPrep.Prep, id: String) = STAGES.joinToString(" ") { st ->
        (if (PhaseTools.editable(p, id, st)) "N" else "-") + (if (PhaseTools.lockOk(p, id, st)) "L" else "-") + (if (PhaseTools.delOk(p, id, st)) "D" else "-") }

    private fun room(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(3319.0, 2678.0, 2677.0)
        p.addMarkAt("front", "W", 1300.0, 1500.0, 2678.0, 2677.0, "w")                                        // FW1, surveyed
        p.addMarkAt("front", "E", 600.0, 600.0, 2678.0, 2677.0, "e").extra = mapOf("born" to "wish", "life" to "wish")   // FE1, born a wish
        p.addMarkAt("front", "D", 400.0, 1200.0, 2678.0, 2677.0, "d").extra = mapOf("born" to "vadd", "life" to "vadd")  // FD1, born Vendor added
        p.addMarkAt("front", "N", 2200.0, 1200.0, 2678.0, 2677.0, "n").extra = mapOf("life" to "wish", "act" to "replace") // FN1, a surveyed thing wished
        p.addMeas("front", SurveyMeas.LINE, listOf(doubleArrayOf(200.0, 200.0), doubleArrayOf(1200.0, 200.0)), "m")
        p.addChain("front", "x", listOf(doubleArrayOf(0.0, 100.0), doubleArrayOf(900.0, 100.0)), listOf(null, null), "k")
        return p
    }

    @Test fun `L4 - the picked item's controls by its id - hidden where the phase holds it read-only, as the page hides them`() {
        val p = room()
        //                    Survey Wish Vendor Estim. Revis. Approved unread     (driven: FW1's row, the placed mark's, the Line's)
        assertEquals("NLD --- --- --- --- --- NLD", ctl(p, "w"), "a surveyed mark: its label alone after Survey")
        assertEquals("NLD NLD --D --- --D --- NLD", ctl(p, "e"), "born a wish: the Wish list's own; later only its Drop")
        assertEquals("NLD --D NLD --- NLD NL- NLD", ctl(p, "d"), "born Vendor added: Vendor review's and Revisions' own")
        assertEquals("NLD --D --D --- --D --- NLD", ctl(p, "n"), "a surveyed thing the client wished: only its Drop")
        assertEquals("NLD NLD NLD --- NLD NLD NLD", ctl(p, "m"), "a Line: editable up to Estimation")
        assertEquals("NLD NLD NLD --- NLD NLD NLD", ctl(p, "k"), "a chain: the same")
        // an id that is no item -- the four lines' and the reference's padlocks -- is never hidden by the phase
        for (id in listOf(SurveyRef.id("front"), "lines:front", "nothing")) assertEquals(List(7) { "NLD" }.joinToString(" "), ctl(p, id), id)
        // and by id it is the item's own answer
        for (st in STAGES) {
            assertEquals(PhaseTools.editable(p.markById("e")!!.second, st), PhaseTools.editable(p, "e", st))
            assertEquals(PhaseTools.delOk(p.measById("m")!!.second, st), PhaseTools.delOk(p, "m", st))
            assertEquals(PhaseTools.lockOk(p.chainById("k")!!.second, st), PhaseTools.lockOk(p, "k", st))
        }
    }

    @Test fun `L4 - what a refused act on an item says, by its id`() {
        val p = room()
        assertEquals("FW1 was marked in Survey — move the phase back to change it", PhaseTools.refuseText(Tx.EN, p, "w", 1))
        assertEquals("FE1 was marked in Wish list — move the phase back to change it", PhaseTools.refuseText(Tx.EN, p, "e", 2))
        assertEquals("FD1 was marked in Vendor review — move the phase back to change it", PhaseTools.refuseText(Tx.EN, p, "d", 1))
        assertEquals("FW1 · Estimation changes nothing — move the phase back on the project screen", PhaseTools.refuseText(Tx.EN, p, "w", 3))
        assertEquals("F-M1 · Estimation changes nothing — move the phase back on the project screen", PhaseTools.refuseText(Tx.EN, p, "m", 3))
        assertEquals("FK1 · Estimation changes nothing — move the phase back on the project screen", PhaseTools.refuseText(Tx.EN, p, "k", 3))
        assertEquals("FW1 Survey मध्ये मार्क झाला होता — बदलण्यासाठी फेज मागे न्या".replace("Survey", Phases.all(Tx.MR)[0].name), PhaseTools.refuseText(Tx.MR, p, "w", 1))
        assertNull(PhaseTools.refuseText(Tx.EN, p, "nothing", 1)); assertNull(PhaseTools.refuseText(Tx.EN, p, SurveyRef.id("front"), 3))
        // ⋮'s clears, Lock all and Start over are Survey's (and an unread stage's): what each says elsewhere
        assertEquals("1000001", bits { PhaseTools.bulk(it) })
        assertEquals("Clearing and locking every mark is a Survey tool — move the phase back to use it", PhaseTools.refuseBulk(Tx.EN, 1))
        assertEquals("This · Estimation changes nothing — move the phase back on the project screen", PhaseTools.refuseBulk(Tx.EN, 3))
        assertEquals("Start over is a Survey tool — move the phase back to use it", PhaseTools.refuseStartOver(Tx.EN))
    }

    /** The menu's rows at [st]; [picked]: what the capture's chip was cycled to, read only while the stage is unread. */
    private fun menu(p: SurveyPrep.Prep, id: String, st: Int?, picked: String? = null) =
        SurveyPrepEdit.menuItemsOf(Tx.EN, p, id, st, picked).joinToString("|") { it.second }

    @Test fun `L4 - the long-press menu's rows by phase - a row that would change a read-only item is hidden, Cancel stays`() {
        val p = room()
        // driven on the page: FW1 (surveyed), the mark just placed in its own phase, and a Line
        assertEquals("Delete|Duplicate|Lock|Lock all Window|Cancel", menu(p, "w", 0)); assertEquals(menu(p, "w", 0), menu(p, "w", null))
        for (st in 1..5) assertEquals("Cancel", menu(p, "w", st), "a surveyed mark at $st")
        // ... WITHOUT ITS DUPLICATE, which the page draws there (the L4 review, finding 1): the copy would be an
        // unstamped box -- a thing that is in the room, to the sheet and to S5 -- of a thing that is not (copyOk)
        assertEquals("Drop|Lock|Lock all Electric point|Cancel", menu(p, "e", 1))
        assertEquals("Drop|Lock|Lock all Door|Cancel", menu(p, "d", 2)); assertEquals(menu(p, "d", 2), menu(p, "d", 4))
        assertEquals("Drop|Cancel", menu(p, "e", 2), "a wish in Vendor review: its Drop alone"); assertEquals("Drop|Cancel", menu(p, "n", 1))
        assertEquals("Delete|Duplicate|Lock|Lock all Line marks|Cancel", menu(p, "m", 1))
        for (st in listOf(0, 1, 2, 4, 5, null)) assertEquals("Delete|Duplicate|Lock|Lock all Line marks|Cancel", menu(p, "m", st), "the Line at $st")
        assertEquals("Cancel", menu(p, "m", 3)); assertEquals("Cancel", menu(p, "k", 3)); assertEquals("Cancel", menu(p, "e", 3))
        // Client approved, the phone's own (Q-list 6): no Drop on a mark past Existing; a mark born Vendor added keeps
        // its lock rows -- and no Duplicate, which would make a new mark after approval (the L4 review)
        assertEquals("Cancel", menu(p, "e", 5)); assertEquals("Lock|Lock all Door|Cancel", menu(p, "d", 5))
        // a LOCKED mark in Scope, born Vendor added: its Unlock is on the menu where its padlock shows -- and at Client
        // approved neither is there, for the unlock could only say "take it Out of scope in its sheet" (PhaseTools.lockOk)
        p.addMarkAt("front", "L", 2200.0, 2400.0, 2678.0, 2677.0, "l").extra = mapOf("born" to "vadd", "life" to "scope", SurveyPrep.LOCKED to true)
        assertEquals("Drop|Unlock|Unlock all Loft|Cancel", menu(p, "l", 2)); assertEquals(menu(p, "l", 2), menu(p, "l", 4))
        assertEquals("Unlock all Loft|Cancel", menu(p, "l", 5))
        // the rows are the unfiltered menu's own, in its order, and the reference's menu is never cut
        for (st in STAGES) for (picked in PICKED) for (id in listOf("w", "e", "d", "n", "m", "k", "l")) {
            val whole = SurveyPrepEdit.menuItemsOf(Tx.EN, p, id); val cut = SurveyPrepEdit.menuItemsOf(Tx.EN, p, id, st, picked)
            assertEquals(whole.filter { it in cut }, cut, "$id at $st"); assertEquals(SurveyPrepEdit.CANCEL, cut.last().first)
        }
        SurveyRef.setMode(p, "front", true, 3319.0, 2678.0, 2677.0)
        for (st in STAGES) for (picked in PICKED)
            assertEquals(SurveyPrepEdit.menuItemsOf(Tx.EN, p, SurveyRef.id("front")), SurveyPrepEdit.menuItemsOf(Tx.EN, p, SurveyRef.id("front"), st, picked), "the reference at $st")
        assertTrue(SurveyPrepEdit.menuItemsOf(Tx.EN, p, SurveyRef.id("front")).isNotEmpty())
    }

    // ------------------------------------------------------------------ the L4 review, finding 1: where a mark is copied

    /** What a capture's chip can have been cycled to, and two things it never was. */
    private val PICKED: List<String?> = listOf(null, "exist", "wish", "vadd", "nonsense")

    @Test fun `the L4 review - a mark is duplicated only where its copy, the box alone, is what a tap would place`() {
        val p = room()
        p.surface("front").marks.add(SurveyPrep.Mark("note", MarkLife.NOTE_TYPE, "FNOTE1", 2100.0, 2300.0, 300.0, 500.0))   // a Note (slice L6 places them)
        fun copy(id: String, picked: String? = null) = bits { PhaseTools.copyOk(p, id, it, picked) }
        //            Survey Wish Vendor Estim. Revis. Approved unread
        // A MARK: at Survey, and while unread -- whatever it was born. The page's own answer is spEdit's: 1000001 for the
        // surveyed window, 1100001 born a wish, 1010111 born Vendor added (proto-tables "L4": the menu's Duplicate)
        for (id in listOf("w", "e", "d", "n")) assertEquals("1000001", copy(id), id)
        // ... and while unread only as long as the chip reads "as Existing": cycled to a wish, a tap is stamped and a copy would not be
        for (id in listOf("w", "e", "d", "n")) {
            assertEquals("1000001", copy(id, "exist"), id); assertEquals("1000001", copy(id, "nonsense"), id)
            assertEquals("1000000", copy(id, "wish"), id); assertEquals("1000000", copy(id, "vadd"), id)
        }
        // a Tape, a Line, an area and a chain carry no state: copied wherever they may be edited, whatever the chip says
        for (id in listOf("m", "k")) for (picked in PICKED) {
            assertEquals("1110111", copy(id, picked), "$id, $picked"); assertEquals(bits { PhaseTools.editable(p, id, it) }, copy(id, picked))
        }
        // a Note is stamped by no tap and shown on no document for others: as it may be edited (slice L6's)
        for (picked in PICKED) assertEquals(bits { PhaseTools.editable(p, "note", it) }, copy("note", picked), "the Note, $picked")
        assertEquals("1110111", copy("note"))
        // an id that is no item is never cut
        for (id in listOf(SurveyRef.id("front"), "lines:front", "nothing")) for (picked in PICKED) assertEquals("1111111", copy(id, picked), id)
        // by id it is the mark's own answer; and never where the mark may not be edited
        for (st in STAGES) for (picked in PICKED) for (id in listOf("w", "e", "d", "n", "note")) {
            val e = p.markById(id)!!.second
            assertEquals(PhaseTools.copyOk(e, st, picked), PhaseTools.copyOk(p, id, st, picked), "$id at $st, $picked")
            if (PhaseTools.copyOk(e, st, picked)) assertTrue(PhaseTools.editable(e, st), "$id at $st, $picked")
        }

        // THE RULE ITSELF, NOT ITS TABLE: wherever a mark's copy is offered, a type tool's tap there places a mark with
        // no `born` -- so the copy, the box alone, is exactly what the phase would have placed, and a sheet for others
        // and S5 read both as a thing that is in the room. And wherever a tap is stamped, no mark is copied.
        val at = java.time.Instant.parse("2026-11-11T05:00:00Z")
        for (st in STAGES) for (picked in PICKED) {
            val q = SurveyPrep.fresh(3319.0, 2678.0, 2677.0)
            val tapped = PhaseTools.place(q, "front", "E", 1300.0, 600.0, 2678.0, 2677.0, "t", st, picked, at) { }?.mark
            val stamped = tapped == null || tapped.extra.containsKey(MarkLife.KEY_BORN)
            for (id in listOf("w", "e", "d", "n")) assertEquals(!stamped, PhaseTools.copyOk(p, id, st, picked), "$id at $st, $picked")
            if (tapped != null && !stamped) {
                val c = SurveyPrepEdit.duplicate(q, "t", 2678.0, 2677.0, "t2")!!
                assertTrue(c.extra.isEmpty()); assertEquals(MarkLife.bornOf(tapped), MarkLife.bornOf(c)); assertEquals(MarkLife.lifeOf(tapped), MarkLife.lifeOf(c))
                assertTrue(SheetMarks.surveyed(tapped) && SheetMarks.surveyed(c))
            }
        }
    }

    @Test fun `the L4 review - the menu offers no Duplicate on a mark after Survey, nor while unread with the chip cycled to a wish`() {
        val p = room()
        fun keys(id: String, st: Int?, picked: String?) = SurveyPrepEdit.menuItemsOf(Tx.EN, p, id, st, picked).map { it.first }
        // stages 1-5: no mark of any birth has the row -- so no copy of a wish is ever made there
        for (st in 1..5) for (picked in PICKED) for (id in listOf("w", "e", "d", "n"))
            assertFalse(SurveyPrepEdit.DUPLICATE in keys(id, st, picked), "$id at $st, $picked")
        // Survey, and a project not on S5 (given Survey's stage): every mark has it, as v52 and the page
        for (picked in PICKED) for (id in listOf("w", "e", "d", "n")) assertTrue(SurveyPrepEdit.DUPLICATE in keys(id, 0, picked), "$id, $picked")
        assertEquals("Delete|Duplicate|Lock|Lock all Window|Cancel", menu(p, "w", 0, "wish"), "the chip is not read at a stage that is read")
        // unread: by the chip
        assertEquals("Delete|Duplicate|Lock|Lock all Window|Cancel", menu(p, "w", null)); assertEquals(menu(p, "w", null), menu(p, "w", null, "exist"))
        assertEquals("Delete|Lock|Lock all Window|Cancel", menu(p, "w", null, "wish")); assertEquals(menu(p, "w", null, "wish"), menu(p, "w", null, "vadd"))
        assertEquals("Drop|Lock|Lock all Electric point|Cancel", menu(p, "e", null, "wish"))
        // a Line and a chain keep the row wherever they may be edited, as the page's menu has it
        for (st in listOf(0, 1, 2, 4, 5, null)) for (picked in PICKED) for (id in listOf("m", "k"))
            assertTrue(SurveyPrepEdit.DUPLICATE in keys(id, st, picked), "$id at $st, $picked")
        // ONLY Duplicate moved: every other row is where the phase's three rules put it
        for (st in STAGES) for (picked in PICKED) for (id in listOf("w", "e", "d", "n", "m", "k")) {
            val rows = keys(id, st, picked)
            assertEquals(PhaseTools.delOk(p, id, st), SurveyPrepEdit.DELETE in rows); assertEquals(PhaseTools.lockOk(p, id, st), SurveyPrepEdit.LOCK in rows)
            assertEquals(PhaseTools.editable(p, id, st), SurveyPrepEdit.LOCK_ALL in rows); assertEquals(PhaseTools.copyOk(p, id, st, picked), SurveyPrepEdit.DUPLICATE in rows)
        }
    }

    // ------------------------------------------------------------------ slice L4: the screen asks these rules, read from its source

    private val appDir = java.io.File("../app/src/main/kotlin/com/malletcrafts/interiorestimate")

    /** An app source with its comment lines taken out (the app is compiled only by CI; what it calls is read here). */
    private fun app(name: String): String = java.io.File(appDir, name).readLines()
        .filter { l -> l.trimStart().let { !it.startsWith("//") && !it.startsWith("*") && !it.startsWith("/*") } }.joinToString("\n")
    private fun count(src: String, what: String) = src.windowed(what.length) { if (it == what) 1 else 0 }.sum()

    @Test fun `L4 - survey prep asks the phase - every placement, the step, the tool, the strip, the band and the menu`() {
        val screen = app("SurveyPrepScreen.kt"); val p3 = app("SurveyPrep360.kt"); val main = app("MainActivity.kt")
        // EVERY PLACEMENT GOES THROUGH PhaseTools.place -- 2 · Mark's and the 360's -- and nothing in the app adds a mark itself
        assertEquals(2, count(screen, "PhaseTools.place(prep, s, t, mx, my, a[0], a[1],"))
        for (f in appDir.listFiles { x -> x.name.endsWith(".kt") }!!) assertEquals(0, count(app(f.name), "addMarkAt("), f.name + " places no mark of its own")
        // ... of the kind the tool remembers for the surface the mark is placed ON (slice L6; the L5 review, finding 2 -- PaletteTest reads which surface)
        assertEquals(2, count(screen, "quoteStage, asPicked, java.time.Instant.now(), SurveyPrepPalette.subFor(spKinds, t, s)) { snapshot() } ?: return null"), "born what the phase places, one undo step, or nothing")
        assertEquals(2, count(screen, "if (placed.amber) say(MarkLife.afterEstimate(tx))"))
        // the step and the tool that are ON are the phase's answer, and only a step or tool change writes the ones last set
        assertEquals(1, count(screen, "val mode = PhaseTools.prepMode(modeSet, quoteStage) {"))
        assertEquals(1, count(screen, "val tool = PhaseTools.toolFor(toolSet, quoteStage)"))
        assertFalse(Regex("""(?<![A-Za-z.])mode = "(lines|mark|list|360)"""").containsMatchIn(screen), "no step is set past the phase")
        assertFalse(Regex("""(?<![A-Za-z.])tool = SurveyPrepPalette""").containsMatchIn(screen), "no tool is set past the phase")
        // the strip, a press routed to a step and setMode all read the steps the phase shows -- never the strip's whole list
        assertEquals(3, count(screen, "PhaseTools.prepSteps(quoteStage)")); assertEquals(0, count(screen, "StepRoute.STEPS"))
        // the palette and the More sheet are drawn only where the phase has a palette
        assertEquals(2, count(screen, "PhaseTools.can(PhaseTools.Tool.PALETTE, quoteStage)"))
        assertTrue(screen.contains("if (PhaseTools.can(PhaseTools.Tool.PALETTE, quoteStage))\n            SpPalette("))
        assertTrue(screen.contains("if (moreOpen && marking && PhaseTools.can(PhaseTools.Tool.PALETTE, quoteStage))"))
        // the band: one line from one rule, drawn in both orientations
        assertEquals(1, count(screen, "val band = PhaseTools.band(tx, quoteStage, projectLine)"))
        assertEquals(1, count(screen, "SpPhaseBand(band, false)")); assertEquals(1, count(screen, "SpPhaseBand(band, true, Modifier.align(Alignment.BottomCenter))"))
        assertEquals(3, count(screen, "            bandOver()\n            notesOver()") + count(screen, "        bandOver()\n        if (planExp)"), "over the photo, the list and the drawing, under the notes")
        // neither the stage nor the band's line has a default: a caller that forgot one would open every room under one rule
        assertTrue(screen.contains("    quoteStage: Int?,\n")); assertTrue(screen.contains("    projectLine: String?,\n"))
        assertEquals(2, count(main, "quoteStage = QuoteStageSync.prepStage(context,")); assertEquals(2, count(main, "projectLine = QuoteStageSync.prepLine(context,"))
        // the menu's rows are the phase's, and both views are handed the stage
        assertEquals(1, count(screen, "SurveyPrepEdit.menuItemsOf(tx, prep, c.id, quoteStage, asPicked)")); assertEquals(0, count(screen, "menuItemsOf(tx, prep, c.id)"))
        assertEquals(1, count(screen, "menuItemsOf("), "the one menu, cut by the stage and the capture's chip")
        assertEquals(2, count(screen, "stage = quoteStage, asPicked = asPicked, onAs = { cycleAs() })"), "2 · Mark's photo and the 360")
        // a refused act: the phase is asked BEFORE the lock, as sp.js asks it
        fun before(a: String, b: String) { val i = screen.indexOf(a); val j = screen.indexOf(b, i); assertTrue(i >= 0 && j > i, "$a before $b") }
        before("if (!PhaseTools.delOk(prep, mid, quoteStage)) { phaseRefuse(", "if (it0.locked) { refuse(code); return }")
        before("if (!PhaseTools.editable(e, quoteStage)) { phaseRefuse(", "if (SurveyPrep.isLocked(e)) { refuse(e.code); return }")
        assertEquals(2, count(screen, "if (!PhaseTools.editable(prep, mid, quoteStage)) { phaseRefuse("), "the padlock and Duplicate")
        // DUPLICATE ASKS ONE THING MORE (the L4 review, finding 1): the copy rule, before the undo step and before the
        // one call in the app that copies a mark -- a press that arrives where the row is hidden makes nothing
        val ask = "if (!PhaseTools.copyOk(prep, mid, quoteStage, asPicked)) { phaseRefuse(null); return }"
        assertEquals(1, count(screen, ask)); assertEquals(1, count(screen, "PhaseTools.copyOk("))
        val made = "SurveyPrepEdit.duplicate(prep, mid, a[0], a[1], nid)"
        for (f in appDir.listFiles { x -> x.name.endsWith(".kt") }!!)
            assertEquals(if (f.name == "SurveyPrepScreen.kt") 1 else 0, count(app(f.name), "SurveyPrepEdit.duplicate("), f.name + " copies no mark of its own")
        screen.indexOf(ask).let { i -> val j = screen.indexOf("snapshot()", i); val k = screen.indexOf(made, i)
            assertTrue(i >= 0 && j > i && k > j, "the copy rule, then the undo step, then the copy")
            assertTrue(screen.lastIndexOf("fun duplicateMark(mid: String) {", i).let { it >= 0 && screen.indexOf("\n    fun ", it + 1) > k }, "all three in duplicateMark") }
        assertEquals(2, count(screen, "if (!PhaseTools.bulk(quoteStage)) { phaseRefuse(PhaseTools.refuseBulk(tx, quoteStage)); return }"), "Lock all and the clears")
        // a mark of an earlier phase is held, not moved, on both views: the grab that refuses, and no tabs, no trash
        for (src in listOf(screen, p3)) {
            assertEquals(3, count(src, "PhaseTools.refuseText(txS, "), "a mark, a chain's point, a Line's end")
            assertEquals(3, Regex("""PhaseTools\.refuseText\(txS, \w+, stageS\), (gate = true, )?said = true\)""").findAll(src).count(), "each said as it is, nothing moved")
        }
        for (src in listOf(screen, p3)) { assertEquals(1, count(src, "PhaseTools.delOk(prep, sid, stage)"), "the trash"); assertEquals(1, count(src, "PhaseTools.editable(prep, sid, stage)"), "the tabs") }
    }
}
