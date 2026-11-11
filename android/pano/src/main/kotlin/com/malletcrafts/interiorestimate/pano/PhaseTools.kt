package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.time.Instant

/**
 * WHAT EACH PHASE SHOWS IN SURVEY PREP, AND WHAT IT LETS A MARK DO (port 62 slice L1, 11 Nov 2026; the approved
 * prototype v61c.2, index61c2.html, module P54: PHASES 3931-3941, TOOLS 3947-3961, AS_FIXED 3964, can 3979, moveOk
 * 3985, noMoveText 3990, spPre 4204-4224, editable / delOk 4232-4237, the refusals 4241-4250). Amit, 19 Oct 2026,
 * 23:05 IST: "flow of client wish list , vendor review should come up better . purpose is to survey the site as is
 * and then mark the client wishlist which can be picked up later for vendor review and estimation."
 *
 * A project has ONE phase, read from its S5 quote stage. Each phase shows ONLY its own tools -- HIDDEN, never
 * disabled -- and the marks of earlier phases stay drawn and change only by the life-cycle moves the phase offers
 * ([MarkLife.MOVES], filtered by [moveOk]).
 *
 * SIX STAGES, NEVER FIVE WITH A FALL-THROUGH. The prototype's tables have five columns: Client approved has none, and
 * `can()` reads a missing column as hidden. Here every table has its sixth cell written out, each from what the page
 * does there -- what `can()` answers where the page reads it, what the page draws where it does not, and the page's
 * CSS for Capture 360 ([TOOLS]). Three things at Client approved are the PHONE'S, until Amit rules on the prototype
 * (DESIGN-port62 Q-list 6): no life-cycle move is offered ([moveOk]; the prototype falls through to Vendor review's
 * moves, which would record a change after approval outside the change requests), the gate's line there is the
 * approval lock's own words ([gate]), and the step strip shows 3 · List without the two steps the prototype draws
 * but cannot enter ([steps]). Two more follow from the first: a mark past Existing has no delete control there
 * ([delOk]) -- its Delete is a Drop or a Restore, a move, and a control that could only refuse is hidden, as at
 * Estimation -- and a locked mark in Scope has no padlock there ([lockOk], slice L2): its unlock could only say
 * "take it Out of scope in its sheet". Everything else there is as the prototype's own code answers for a stage
 * past Estimation ([editable], [refuseText]).
 *
 * THE STAGE SURVEY PREP IS GIVEN is [stageOf]: 0-5 for a project S5 lists; 0, Survey's rules, for one S5 does not list
 * ("The tools keep following the project's own phase, Survey for a project made on the phone", index61c2.html
 * 4091-4093); null while a listed project's stage is unread, when every tool shows and every move is offered, as v53
 * did. Pure. SLICE L3 wired the moves and the gate (the Mark sheet); SLICE L4 (11 Nov 2026) wires survey prep itself:
 * the band ([band]), the `mark` and `palette` rows ([prepSteps], [prepMode], [toolFor]), what a type tool's tap places
 * ([place], and the chip's second half through SurveyPrepPalette.chip), and the marks of earlier phases held read-only
 * ([editable], [delOk], [lockOk], [bulk] and what a refused act says). The `lines`, `readings` and `capture` rows are
 * slice L10's ([PREP_HONOURS]).
 */
object PhaseTools {

    // ------------------------------------------------------------------ the six stages

    /** The quote stages by their place in [QuoteStage.STAGES]; a phase IS its stage's index (PHASE_OF_STAGE is the identity). */
    const val SURVEY = 0
    const val WISH_LIST = 1
    const val VENDOR_REVIEW = 2
    const val ESTIMATION = 3
    const val REVISIONS = 4
    /** Client approved: execution. It has no segment on the strip and no column in the prototype's tables. */
    const val APPROVED = 5

    /** How many stages every table here holds a cell for. */
    const val STAGES = 6

    /** [stage] as a phase: itself when it is 0-5, else null -- unread (phaseOfStage). */
    fun phaseOf(stage: Int?): Int? = stage?.takeIf { it in 0 until STAGES }

    /**
     * The stage survey prep is given, from the store's copy [cache] of the project (null: never read) and whether the
     * project is [phoneOnly] (made on this phone, never sent). The one decision [QuoteStage.projectLine] makes for the
     * project line, so the tools and the line never disagree: not on S5 -- phoneOnly, or a copy read as not listed --
     * is Survey; never read, a blank stage or a name outside the six is null (unread); else the stage's index.
     */
    fun stageOf(cache: QuoteStage.Cache?, phoneOnly: Boolean): Int? = when {
        phoneOnly || (cache != null && !cache.listed) -> SURVEY
        cache == null -> null
        else -> QuoteStage.indexOf(cache.stage.trim()).takeIf { it >= 0 }
    }

    // ------------------------------------------------------------------ the tools, per stage

    /** The tools the phases show or hide, by the prototype's own names ([id]). */
    enum class Tool(val id: String) {
        LINES("lines"), MARK("mark"), LIST("list"), STATE_FILTER("stateFilter"), VIEW_360("view360"), PALETTE("palette"),
        READINGS("readings"), TRADE("trade"), WISHLIST("wishlist"), WISH_EDIT("wishEdit"), VENDOR_UI("vendorUI"),
        VENDOR_PACKS("vendorPacks"), HANDOFF("handoff"), CAPTURE("capture")
    }

    private fun row(vararg shown: Int): List<Boolean> = shown.map { it == 1 }.also { require(it.size == STAGES) }

    /**
     * What each stage shows, per tool: Survey, Wish list, Vendor review, Estimation, Revisions -- the prototype's
     * TOOLS, cell for cell -- then Client approved, from what the page does there:
     *  - `lines`, `mark`, `palette`, `readings`, `trade`, `stateFilter`, `wishEdit`, `vendorUI`: 0, what `can()`
     *    answers where the page asks it (spPre moves a Lines or Mark step on to List, puts the tool back to Select,
     *    and resets the trade and state filters; a reading is a figure without its box; wishMode LOCKS the wish
     *    list; the Wish list draws no vendor field);
     *  - `list`, `view360`, `wishlist`: 1 -- nothing asks `can()` for them, and the page shows all three at Client
     *    approved (the mode is held on List; the 360 opens; the Wish list chip is hidden at Survey alone);
     *  - `vendorPacks`, `handoff`: 0 -- the strip, which draws them by phase, is folded at Client approved;
     *  - `capture`: 1 -- the page's CSS hides Capture 360 at phases 1-4 only and shows it again in execution: "a
     *    work-stage photo needs the capture sheet" (index61c2.html CSS 1362, 1372).
     * The phone draws no Trade chip, State ▾, vendor pack or hand-off panel in any phase (the plan's listed subset,
     * Q-list 3): those rows are here because the table is one piece.
     */
    val TOOLS: Map<Tool, List<Boolean>> = linkedMapOf(
        //                          Survey Wish Vendor Estim. Revis.   Approved
        Tool.LINES        to row(1, 0, 0, 0, 0,   0),   // survey only (bounding); Revisions surveys nothing
        Tool.MARK         to row(1, 1, 1, 0, 1,   0),   // Revisions can re-place a mark (as Vendor added)
        Tool.LIST         to row(1, 1, 1, 1, 1,   1),   // 3 · List and the List tab
        Tool.STATE_FILTER to row(0, 1, 1, 1, 1,   0),   // Survey hides State ▾ / Move N ▸ (every mark there is Existing)
        Tool.VIEW_360     to row(1, 1, 1, 1, 1,   1),   // the 360
        Tool.PALETTE      to row(1, 1, 1, 0, 1,   0),   // Revisions has the type tools, like Vendor review
        Tool.READINGS     to row(1, 0, 0, 0, 0,   0),   // survey only
        Tool.TRADE        to row(0, 0, 1, 0, 1,   0),   // the trades view, like Vendor review
        Tool.WISHLIST     to row(0, 1, 1, 1, 1,   1),   // the project's Wish list chip / screen
        Tool.WISH_EDIT    to row(0, 1, 1, 0, 1,   0),   // the wish ticks: refused at Survey, read-only at Estimated, locked at approval
        Tool.VENDOR_UI    to row(0, 0, 1, 0, 1,   0),   // the Wish list's Vendor field and Vendor pack buttons
        Tool.VENDOR_PACKS to row(0, 0, 1, 0, 1,   0),   // the strip's per-trade vendor packs
        Tool.HANDOFF      to row(0, 0, 0, 1, 0,   0),   // the GOES-TO-ESTIMATION panel stays Estimation's
        Tool.CAPTURE      to row(1, 0, 0, 0, 0,   1),   // the room screen's Capture 360: a Survey tool, and execution's again
    )

    /** Shown? With the stage unread EVERY tool shows (v53's rule for an unread stage); else the table's cell (can). */
    fun can(tool: Tool, stage: Int?): Boolean = phaseOf(stage)?.let { TOOLS.getValue(tool)[it] } ?: true

    // ------------------------------------------------------------------ the step strip

    /** The tool a step of [StepRoute.STEPS] stands for; null for a key that is no step. */
    fun toolOf(step: String): Tool? = when (step) {
        "lines" -> Tool.LINES
        "mark" -> Tool.MARK
        "list" -> Tool.LIST
        "360" -> Tool.VIEW_360
        else -> null
    }

    /** Whether the strip shows [step] at [stage]: a step whose tool the phase hides is not drawn at all. */
    fun stepShown(step: String, stage: Int?): Boolean = toolOf(step)?.let { can(it, stage) } ?: false

    /**
     * The steps the strip shows at [stage], in [StepRoute.STEPS] order: all four at Survey and while unread; 2 · Mark
     * on through the Wish list, Vendor review and Revisions; 3 · List (and the 360) alone at Estimation -- and at
     * Client approved, where the prototype draws steps 1 and 2 but lets neither be entered (Q-list 6).
     */
    fun steps(stage: Int?): List<String> = StepRoute.STEPS.filter { stepShown(it, stage) }

    /**
     * The step survey prep is on, moved on from one the phase hides (spPre): a hidden 1 · Lines goes to 2 · Mark when
     * the phase shows it and the open tab is [markable] (a face whose four lines are set, so it can be squared), else
     * to 3 · List; a hidden 2 · Mark goes to 3 · List. Any other [mode] is kept.
     */
    fun modeFor(mode: String, stage: Int?, markable: Boolean): String {
        var m = mode
        if (m == "lines" && !can(Tool.LINES, stage)) m = if (can(Tool.MARK, stage) && markable) "mark" else "list"
        if (m == "mark" && !can(Tool.MARK, stage)) m = "list"
        return m
    }

    /** The palette's tool, put back to Select where the phase has no palette (spPre): nothing is placed or drawn there. */
    fun toolFor(tool: String, stage: Int?): String = if (can(Tool.PALETTE, stage)) tool else SurveyPrepPalette.SEL

    // ------------------------------------------------------------------ survey prep, as far as it is wired (slice L4)

    /**
     * THE ROWS OF [TOOLS] SURVEY PREP'S OWN SCREEN HONOURS SO FAR (port 62 slice L4, which closes build B2): `mark` and
     * `palette` -- at Estimation and at Client approved there is no 2 · Mark and no palette, so no build can place a
     * mark where the prototype has no tool (the plan critic's blocker). The rest of the phase's hiding -- 1 · Lines
     * and the reading boxes after Survey -- reaches the phone with slice L10 (build B5), which adds [Tool.LINES] and
     * [Tool.READINGS] here; until then the strip keeps 1 · Lines in every phase, as before. `list` and `view360` are
     * shown in every phase, so honouring them changes nothing. ONE PLACE: the strip, the step survey prep is on and
     * every press routed to a step read [prepSteps] / [prepMode], never a rule of their own.
     */
    val PREP_HONOURS: Set<Tool> = setOf(Tool.MARK, Tool.PALETTE)

    /** Whether survey prep shows [tool] at [stage] in this build: the table's cell for a row it honours, else shown. */
    fun prepShows(tool: Tool, stage: Int?): Boolean = tool !in PREP_HONOURS || can(tool, stage)

    /**
     * The steps survey prep's strip draws at [stage], in [StepRoute.STEPS] order -- each keeps its own number, so with
     * 2 · Mark hidden the strip reads 1 · 3 · 4, as the prototype's does with its hidden step's circle gone (CSS
     * 1272-1274): all four at Survey, the Wish list, Vendor review, Revisions and while unread; no 2 · Mark at
     * Estimation and at Client approved.
     */
    fun prepSteps(stage: Int?): List<String> = StepRoute.STEPS.filter { k -> toolOf(k)?.let { prepShows(it, stage) } ?: false }

    /**
     * The step survey prep is on at [stage], moved on from one it does not show (spPre, by the rows it honours): 2 ·
     * Mark goes to 3 · List where the phase has no Mark; every other [mode] is kept. (A hidden 1 · Lines will go to
     * 2 · Mark when [markable] -- the open tab is a face whose four lines close -- else to 3 · List, once slice L10
     * honours that row; [markable] is asked only then.)
     */
    fun prepMode(mode: String, stage: Int?, markable: () -> Boolean): String {
        var m = mode
        if (m == "lines" && !prepShows(Tool.LINES, stage)) m = if (prepShows(Tool.MARK, stage) && markable()) "mark" else "list"
        if (m == "mark" && !prepShows(Tool.MARK, stage)) m = "list"
        return m
    }

    // ------------------------------------------------------------------ what a new mark is placed as

    /**
     * What a mark is PLACED as in each stage (AS_FIXED), a [MarkLife] state: Wish list a Client wish, Vendor review and
     * Revisions Vendor added. Null: the phase fixes nothing -- Survey (every mark there is Existing, [asFor]), and
     * Estimation and Client approved, which have no palette, so nothing is placed and the rule is never asked.
     */
    val AS_FIXED: List<String?> = listOf(null, MarkLife.WISH, MarkLife.VADD, null, MarkLife.VADD, null)

    /** The order the "as Existing ▾" button cycles in while the stage is unread (AS_CYCLE). */
    val AS_CYCLE: List<String> = listOf(MarkLife.EXIST, MarkLife.WISH, MarkLife.VADD)

    /**
     * What a NEW mark is placed as at [stage] (asFor), for [MarkLife.placeAs]: NULL where the phase has no palette --
     * no type tool is on there, so nothing is placed and no mark is ever born a wish at Estimation or after approval
     * (the prototype's own asFor would answer a Client wish there, had anything asked it); Existing at Survey; the
     * phase's own at the Wish list, Vendor review and Revisions; and with the stage unread, what this capture's button
     * was last cycled to ([picked]; anything else is Existing). A type tool's tap is what asks this. What a DUPLICATE
     * is born as is not decided here: the prototype's duplicateItem copies the box alone and never asks (DESIGN-port62
     * Q-list 8, for slice L4).
     */
    fun asFor(stage: Int?, picked: String? = null): String? {
        if (!can(Tool.PALETTE, stage)) return null
        val ph = phaseOf(stage) ?: return picked?.takeIf { it in AS_CYCLE } ?: MarkLife.EXIST
        return AS_FIXED[ph] ?: MarkLife.EXIST
    }

    /** The second half of a type tool's chip: none; the phase's fixed "as …" in plain words; or the cycling button. */
    enum class AsChip { NONE, FIXED, CYCLE }

    /**
     * Which second half the tool chip shows at [stage] (asChipHTML): [AsChip.NONE] at Survey, where every mark is
     * Existing -- and where there is no palette, so no chip at all; [AsChip.FIXED] where the phase fixes it (nothing
     * to pick); [AsChip.CYCLE] only while the stage is unread.
     */
    fun asChip(stage: Int?): AsChip = when {
        !can(Tool.PALETTE, stage) -> AsChip.NONE
        phaseOf(stage) == null -> AsChip.CYCLE
        AS_FIXED[phaseOf(stage)!!] != null -> AsChip.FIXED
        else -> AsChip.NONE
    }

    /** A tap on the cycling button (cycleAs): the next of [AS_CYCLE] after what [asFor] answers now; null where it does not cycle. */
    fun nextAs(stage: Int?, picked: String?): String? {
        if (asChip(stage) != AsChip.CYCLE) return null
        return AS_CYCLE[(AS_CYCLE.indexOf(asFor(stage, picked)) + 1) % AS_CYCLE.size]
    }

    /** What [place] did: the new [mark]; [amber]: "After the estimate — tell the office" is said ([MarkLife.afterEstimate]). */
    class Placed(val mark: SurveyPrep.Mark, val amber: Boolean)

    /**
     * A TYPE TOOL'S TAP (port 62 slice L4; the prototype's addElAt and L53.placed, index61c2.html 5922-5926, 2959-2961):
     * a mark of type [t] on [s] where the finger is, ([x], [y]) mm on the [a] x [b] face, with the next code of its
     * type and the id [id] ([SurveyPrep.Prep.addMarkAt]) -- BORN WHAT THE PHASE PLACES ([asFor], [MarkLife.placeAs]):
     * a v52 mark with no key at Survey (and on a project S5 does not list, which is given Survey's stage); a Client
     * wish at the Wish list; Vendor added at Vendor review and Revisions; while the stage is unread, what this
     * capture's chip was last cycled to ([picked]). `born`, `life`, `lifeAt` ([at]) and `lifeAs` are written ONCE,
     * here, and never for a Note.
     *
     * NULL, WITH NOTHING PLACED AND NO UNDO STEP, WHERE THE PHASE HAS NO PALETTE -- Estimation and Client approved:
     * the palette is not drawn there and the tool is Select ([toolFor]), so no press asks; this is the same rule read
     * once more where the mark would be made, so no path places one there. [snapshot] is the caller's ONE undo step,
     * taken just before the mark is added. [Placed.amber]: from Estimation on a placement is told to the office (the
     * prototype's `stage>=3`; in practice Revisions, the one later phase with a palette).
     * A DUPLICATE is not a placement: the prototype's duplicateItem copies the box alone and stamps nothing
     * ([SurveyPrepEdit.duplicate]; DESIGN-port62 Q-list 8).
     */
    fun place(p: SurveyPrep.Prep, s: String, t: String, x: Double, y: Double, a: Double, b: Double, id: String,
              stage: Int?, picked: String?, at: Instant, snapshot: () -> Unit): Placed? {
        if (!can(Tool.PALETTE, stage)) return null
        snapshot()
        val e = p.addMarkAt(s, t, x, y, a, b, id)
        MarkLife.placeAs(e, asFor(stage, picked), stage, at)
        return Placed(e, phaseOf(stage).let { it != null && it >= ESTIMATION })
    }

    // ------------------------------------------------------------------ the life-cycle moves a phase offers

    /**
     * Whether the phase at [stage] offers the move [m] (a row of [MarkLife.MOVES], unresolved) from state [from]
     * (moveOk). Survey: none (v53's gate). Wish list: the client's own -- Existing → Client wish, a wish dropped or
     * taken back, a Dropped mark restored. Vendor review and Revisions: every move past the wish (to Vendor review,
     * the vendor's and the client's answers, Scope) but never making or unmaking a wish. Estimation: none. Unread:
     * every one. CLIENT APPROVED: NONE -- the phone's rule until Amit decides on the prototype (Q-list 6); the
     * prototype names no rule there and falls through to Vendor review's.
     */
    fun moveOk(from: String, m: MarkLife.Move, stage: Int?): Boolean = when (phaseOf(stage)) {
        null -> true
        SURVEY, ESTIMATION, APPROVED -> false
        WISH_LIST -> (from == MarkLife.EXIST && m.to == MarkLife.WISH) ||
            (from == MarkLife.WISH && (m.to == MarkLife.DROP || m.to == MarkLife.EXIST)) ||
            (from == MarkLife.DROP && m.restore)
        else -> from != MarkLife.EXIST && !(from == MarkLife.WISH && m.to == MarkLife.EXIST)
    }

    /** A stage at which the Mark sheet offers no move at all and says why in one line, and [MarkLife.move] refuses with it. */
    enum class Gate { SURVEY, APPROVED }

    /** The gate at [stage], or null: Survey's (v53's `stage===0`), and on the phone Client approved's (Q-list 6). */
    fun gate(stage: Int?): Gate? = when (phaseOf(stage)) {
        SURVEY -> Gate.SURVEY
        APPROVED -> Gate.APPROVED
        else -> null
    }

    /**
     * The gate's line: at Survey "Finish the survey first — the stage moves to Client wish list on S5"; at Client
     * approved the approval lock's own words, "Approved — changes go through change requests" (the Wish list's banner
     * there, Amit, 28 Oct 2026: the change requests are the ONE place a change after approval is recorded).
     */
    fun gateText(tx: Tx, g: Gate): String = when (g) {
        Gate.SURVEY -> tx.refuse_life_finish_survey_first_stage
        Gate.APPROVED -> tx.refuse_readonly_wishlist_approved
    }

    /**
     * The Mark sheet's line when the phase leaves a mark in state [life] no move (noMoveText): at Estimation
     * "Existing · Estimation moves nothing — move the phase back on the project screen"; at the Wish list "Vendor
     * review · its next move is in Vendor review"; at Vendor review and Revisions, for an Existing mark, "Existing · a
     * wish is marked in the Wish list phase"; else "No move from Scope". (At a [gate] the sheet shows the gate's line
     * in its place.)
     */
    fun noMoveText(tx: Tx, life: String, stage: Int?): String {
        val name = MarkLife.name(tx, life)
        return when (phaseOf(stage)) {
            ESTIMATION -> tx.refuse_phase_x_estimation_moves_nothing(name)
            WISH_LIST -> tx.refuse_phase_x_next_move_vendor(name)
            VENDOR_REVIEW, REVISIONS -> if (life == MarkLife.EXIST) tx.refuse_phase_x_wish_marked_wish(name) else tx.refuse_phase_no_move_x(name)
            else -> tx.refuse_phase_no_move_x(name)
        }
    }

    // ------------------------------------------------------------------ marks of earlier phases are READ-ONLY

    /** A Tape, a Line, an area, a chain and a Note are the phase's own measuring and note tools: editable up to Estimation. */
    private fun measuring(stage: Int?): Boolean = phaseOf(stage) != ESTIMATION

    /**
     * Whether the mark [e] may be moved, resized, nudged, duplicated or locked at [stage] (editable): everything at
     * Survey and while unread; nothing at Estimation; at the Wish list only what was BORN a Client wish; at Vendor
     * review, Revisions -- and Client approved, as the prototype's code answers for any later stage -- only what was
     * born Vendor added. A Note is editable up to Estimation. A refused act's control is HIDDEN, never greyed.
     */
    fun editable(e: SurveyPrep.Mark, stage: Int?): Boolean = when (val ph = phaseOf(stage)) {
        null, SURVEY -> true
        ESTIMATION -> false
        else -> MarkLife.isNote(e) || MarkLife.bornOf(e) == (if (ph == WISH_LIST) MarkLife.WISH else MarkLife.VADD)
    }

    fun editable(@Suppress("UNUSED_PARAMETER") m: SurveyPrep.Meas, stage: Int?): Boolean = measuring(stage)
    fun editable(@Suppress("UNUSED_PARAMETER") c: SurveyPrep.Chain, stage: Int?): Boolean = measuring(stage)

    /**
     * Whether the mark's delete control shows at [stage] (delOk): as [editable] -- except that on a mark past
     * Existing Delete is v53's Drop / Restore, a life-cycle move the phase already filters ([moveOk]), so it stays
     * everywhere but Estimation, which offers no move ("so it stays, except in Estimation", index61c2.html
     * 4229-4230). ON THE PHONE CLIENT APPROVED OFFERS NO MOVE EITHER (Q-list 6), so there the prototype's own reason
     * hides it as well: a Drop would open the Mark sheet on the approval lock's line and a Restore be refused with
     * it, and a control that can only refuse is HIDDEN, never shown. This holds for a mark born Vendor added too,
     * which [editable] still lets be moved, resized and locked there: past Existing its Delete is a Drop, not a
     * delete. (The prototype shows the control at Client approved because its moveOk falls through to Vendor
     * review's moves there; the L1 review, DESIGN-port62 section 3.1.)
     */
    fun delOk(e: SurveyPrep.Mark, stage: Int?): Boolean {
        val ph = phaseOf(stage)
        if (ph == null || ph == SURVEY) return true
        if (ph == ESTIMATION) return false
        if (!MarkLife.isNote(e) && MarkLife.pastExisting(e)) return ph != APPROVED   // Drop / Restore: a move, never a delete
        return editable(e, stage)
    }

    fun delOk(m: SurveyPrep.Meas, stage: Int?): Boolean = editable(m, stage)
    fun delOk(c: SurveyPrep.Chain, stage: Int?): Boolean = editable(c, stage)

    /**
     * Whether the mark's PADLOCK shows at [stage] -- the picked row's, and the menu's Lock / Unlock: as [editable],
     * the prototype's own answer (spEdit), with ONE cell the phone hides. At Client approved a LOCKED mark in Scope has
     * none: its unlock can only answer "FW1 is in Scope — take it Out of scope in its sheet"
     * ([MarkLife.unlockRefusal]), a move the phone's sheet does not offer after approval ([moveOk]; Q-list 6) -- so the
     * control could only refuse, and is HIDDEN, the L1 review's rule for [delOk] there. Decided in slice L2, which
     * wires that refusal, as the L1 review left it (DESIGN-port62 section 3.1). Only a mark born Vendor added reaches
     * this cell: [editable] has already hidden every other mark's controls there. Slice L4 asks it.
     */
    fun lockOk(e: SurveyPrep.Mark, stage: Int?): Boolean =
        editable(e, stage) && !(phaseOf(stage) == APPROVED && SurveyPrep.isLocked(e) && !MarkLife.unlockable(e))

    fun lockOk(m: SurveyPrep.Meas, stage: Int?): Boolean = editable(m, stage)
    fun lockOk(c: SurveyPrep.Chain, stage: Int?): Boolean = editable(c, stage)

    /**
     * [editable], [delOk] and [lockOk] for the item of [p] with the id [id], whatever it is -- a mark, a Tape, a Line,
     * an area or a chain (slice L4: the screen's rows, menus and discs ask by id). An id that is no item answers
     * TRUE: the four lines' and the reference rectangle's padlocks are never hidden by the phase (spPost,
     * index61c2.html 4259: `!/^(lines|ref):/.test(id)`).
     */
    fun editable(p: SurveyPrep.Prep, id: String, stage: Int?): Boolean =
        p.markById(id)?.let { editable(it.second, stage) } ?: p.measById(id)?.let { editable(it.second, stage) }
            ?: p.chainById(id)?.let { editable(it.second, stage) } ?: true

    fun delOk(p: SurveyPrep.Prep, id: String, stage: Int?): Boolean =
        p.markById(id)?.let { delOk(it.second, stage) } ?: p.measById(id)?.let { delOk(it.second, stage) }
            ?: p.chainById(id)?.let { delOk(it.second, stage) } ?: true

    fun lockOk(p: SurveyPrep.Prep, id: String, stage: Int?): Boolean =
        p.markById(id)?.let { lockOk(it.second, stage) } ?: p.measById(id)?.let { lockOk(it.second, stage) }
            ?: p.chainById(id)?.let { lockOk(it.second, stage) } ?: true

    /** ⋮'s Clear this surface, Clear whole room, Lock all and Start over act on every phase's marks: Survey only, and unread (spBulk). */
    fun bulk(stage: Int?): Boolean = phaseOf(stage).let { it == null || it == SURVEY }

    /** The phase a mark born so was marked in: Wish list for a wish, Vendor review for Vendor added, Survey for the rest (BORN_PH). */
    private fun bornPhase(born: String): Int = when (born) {
        MarkLife.WISH -> WISH_LIST
        MarkLife.VADD -> VENDOR_REVIEW
        else -> SURVEY
    }

    private fun refusal(tx: Tx, code: String, born: String, stage: Int?): String {
        val named = code.ifEmpty { tx.phase_this }
        return if (phaseOf(stage) == ESTIMATION) tx.refuse_phase_x_estimation_changes_nothing(named)
        else tx.refuse_phase_x_was_marked_x(code = named, v = Phases.all(tx)[bornPhase(born)].name)
    }

    /**
     * What a refused act on the mark [e] says (refuseText): at Estimation "FW1 · Estimation changes nothing — move
     * the phase back on the project screen"; else "FW1 was marked in Survey — move the phase back to change it",
     * naming the phase the mark was born in.
     */
    fun refuseText(tx: Tx, e: SurveyPrep.Mark, stage: Int?): String = refusal(tx, e.code, MarkLife.bornOf(e), stage)
    fun refuseText(tx: Tx, m: SurveyPrep.Meas, stage: Int?): String = refusal(tx, m.code, MarkLife.EXIST, stage)
    fun refuseText(tx: Tx, c: SurveyPrep.Chain, stage: Int?): String = refusal(tx, c.code, MarkLife.EXIST, stage)

    /** [refuseText] for the item of [p] with the id [id]; null when there is no such item. */
    fun refuseText(tx: Tx, p: SurveyPrep.Prep, id: String, stage: Int?): String? =
        p.markById(id)?.let { refuseText(tx, it.second, stage) } ?: p.measById(id)?.let { refuseText(tx, it.second, stage) }
            ?: p.chainById(id)?.let { refuseText(tx, it.second, stage) }

    /** What a refused Clear or Lock all says (refuseText with no item): at Estimation "This · Estimation changes nothing — …". */
    fun refuseBulk(tx: Tx, stage: Int?): String =
        if (phaseOf(stage) == ESTIMATION) tx.refuse_phase_x_estimation_changes_nothing(tx.phase_this)
        else tx.refuse_phase_clearing_locking_every_mark

    /** What a refused Start over says (refuseText('restart')), in every phase. */
    fun refuseStartOver(tx: Tx): String = tx.refuse_phase_start_over_survey_tool

    /**
     * A reading is keyed only where the phase shows readings (spReadingRefused): null where it may be, else "Readings
     * are taken in the Survey phase — move the phase back on the project screen to key one". Outside Survey a reading
     * is a figure, not a box.
     */
    fun readingRefusal(tx: Tx, stage: Int?): String? = if (can(Tool.READINGS, stage)) null else tx.refuse_readonly_readings

    // ------------------------------------------------------------------ the band over survey prep

    /** The separator in the band, as the prototype joins it. */
    private const val SEP = " · " // text:value the prototype's separator, the same in both languages

    /**
     * The band over survey prep: [lit] of its five pips are on; [head] is its bold part ("2 · Wish list", "Client
     * approved"; "" while unread); [tail] the rest ("every mark you place is a client wish"), drawn after ` · `
     * when both are there ([whole]); [label] what a screen reader says; [unread]: the amber "Phase not read yet" line.
     */
    data class Band(val lit: Int, val head: String, val tail: String, val label: String, val unread: Boolean = false) {
        val whole: String get() = listOf(head, tail).filter { it.isNotEmpty() }.joinToString(SEP)

        /**
         * The band's SECOND part as it is drawn beside the bold [head] (slice L4): `· every mark you place is a client
         * wish` -- the separator's dot leads it, its space is the gap between the two parts (the prototype's `.p54bt`
         * is ` · words` in a flex row, where the leading space is dropped). With no [head] (unread) the words alone;
         * with no [tail] (Client approved with no line to say) nothing.
         */
        val rest: String get() = if (head.isEmpty() || tail.isEmpty()) tail else SEP.trimStart() + tail
    }

    /** A phase's band words (PHASES[i].band), for the five phases of the strip. */
    private fun bandWords(tx: Tx, ph: Int): String = when (ph) {
        SURVEY -> tx.phase_band_survey
        WISH_LIST -> tx.phase_band_wish
        VENDOR_REVIEW -> tx.phase_band_vendor
        ESTIMATION -> tx.phase_band_estimation
        else -> tx.phase_band_rev
    }

    /**
     * The band at [stage] (spPre): through the quote journey the phase's number and name, then its band words, the
     * pips on up to it; at Client approved every pip on, "Client approved", then the project's one line [line] (the
     * project line's own text, "Execution · 1 in progress"; none: the name alone); unread, no pip and "Phase not read
     * yet — S5 decides when online".
     */
    fun band(tx: Tx, stage: Int?, line: String? = null): Band {
        val ph = phaseOf(stage) ?: return Band(0, "", tx.phase_phase_not_read_yet, tx.phase_phase_not_read_yet, unread = true)
        if (ph == APPROVED) {
            val approved = Masters.quote(tx, QuoteStage.APPROVED)
            val said = line.orEmpty()
            return Band(Phases.N, approved, said, listOf(approved, said).filter { it.isNotEmpty() }.joinToString(SEP))
        }
        val p = Phases.all(tx)[ph]
        val words = bandWords(tx, ph)
        return Band(ph + 1, "${p.n}$SEP${p.name}", words, tx.phase_phase_x_4_x(n = p.n.toString(), name = p.name, band = words))
    }
}
