package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.QuoteStage.LineKind
import com.malletcrafts.interiorestimate.pano.QuoteStage.ProjectLine
import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * THE PHASE STRIP, the pure half (port 61c slice QM1, 3 Nov 2026; SPEC61c §4.1 item 3; index61c2.html PHASES and
 * stripHTML through the quote journey). Under the project line, five segments -- the five quote-journey phases, each
 * standing for one of S5's quote stages ([all]: Survey, Wish list, Vendor review, Estimation, Revisions; Client approved
 * is execution and has no segment) -- the past ones ticked, the current one marked, and a tap on another one THE
 * phone's mover of the project's one state before approval (Amit, 27 Oct 2026: "everything should be single truth"):
 * a later phase moves at once, an earlier one asks first ([tap], [ask]), each move a guarded `PUT …/stage` built by
 * [QuoteMove.stage] and answered by S5. Under the segments the does-line ([doesLine]): what the phase is for, or why
 * there is no phase -- `Phase not read yet — S5 decides when online`, or for a project S5 does not list (v61c.2, Amit,
 * 28 Oct 2026: "Read-only until S5 has it") `not on S5 yet — this project has not been sent`, read whole; then at
 * Survey the SURVEY COVERAGE row ([coverageRow]) and at Revisions the IN REVISION row ([revisionRow]).
 *
 * A LISTED SUBSET of the prototype's strip (Amit's answer 1 of 28 Oct 2026, "Ship the mover and Approve now"): the
 * VENDOR PACKS row at Vendor review, the GOES TO ESTIMATION row at Estimation and the hand-off counts of IN REVISION
 * rest on the v53/v54 marks, trades and packs the phone does not have, and come with that port; the does-lines
 * describing those tools ship word for word as approved. `Approve the estimate ▸` and the PM-only line at Revisions
 * are slice QM2's. At Client approved the strip is the opened quote view ([QuoteView]), which this file does not draw.
 *
 * Nothing here talks to S5 or Android: :app's PhaseStrip draws from these, QuoteStageSync.move sends the request.
 * Every text is a `tx.<key>`; a quote stage is an identity value shown through [Masters.quote].
 */
object Phases {

    /** How many segments the strip has: the five quote-journey phases. Client approved is past every one of them. */
    const val N = 5

    /** The phase whose row is SURVEY COVERAGE. */
    const val SURVEY = 0

    /** The phase whose row is IN REVISION (and, from QM2, Approve the estimate ▸). */
    const val REVISIONS = 4

    /** One of the five: its number, its name (the phase's, not the stage's), the quote stage it stands for, its does-line. */
    data class Phase(val n: Int, val name: String, val quote: String, val does: String)

    /**
     * The five as the prototype's PHASES hold them, in the quote journey's order: each one's quote stage is
     * [QuoteStage.STAGES] at the same index (the prototype's PHASE_OF_STAGE is the identity), so a segment's tap
     * moves the project to `STAGES[i]` ([stageOf]).
     */
    fun all(tx: Tx): List<Phase> = listOf(
        Phase(1, tx.quote_survey, QuoteStage.STAGES[0], tx.phase_capture_bound_walls_mark),
        Phase(2, tx.common_wish_list, QuoteStage.STAGES[1], tx.phase_walk_client_through_mark),
        Phase(3, tx.life_vrev, QuoteStage.STAGES[2], tx.phase_trades_vendor_packs_wishes),
        Phase(4, tx.common_estimation, QuoteStage.STAGES[3], tx.phase_hand_off_estimate_nothing),
        Phase(5, tx.common_revisions, QuoteStage.STAGES[4], tx.phase_rev_does),
    )

    /** The five names, as the opened quote view's ticked steps show them too ([QuoteView.segments]). */
    fun names(tx: Tx): List<String> = all(tx).map { it.name }

    /** The quote stage the segment at [i] (0..4) moves the project to: S5's identity value, never shown as is. */
    fun stageOf(i: Int): String = QuoteStage.STAGES[i]

    /**
     * The strip's phase for the project line: 0..4 through the quote journey, [N] at Client approved (past every
     * segment: the strip folds into the quote view), null when the stage is not read or the project is not on S5 --
     * then no segment is current, past or a tap (the prototype's `ph==null`).
     */
    fun phaseOf(line: ProjectLine): Int? = when (line.kind) {
        LineKind.QUOTE -> QuoteStage.indexOf(line.quote.orEmpty()).takeIf { it in 0 until N }
        LineKind.EXEC -> N
        LineKind.UNREAD, LineKind.LOCAL -> null
    }

    /**
     * One segment as drawn: its number and name; its accessible label (`Phase n of 5: name`, with ` (now)` on the
     * current one); whether it is [past] (a ✓ in its circle), the current one ([on], filled), and a [tap] -- any
     * segment but the current while the stage is read and not Client approved (approval is final: no back-move).
     */
    data class Segment(val n: Int, val name: String, val label: String, val past: Boolean, val on: Boolean, val tap: Boolean)

    /** The five segments for the strip at [ph] ([phaseOf]). */
    fun segments(tx: Tx, ph: Int?): List<Segment> = all(tx).mapIndexed { i, p ->
        val on = ph == i
        val n = p.n.toString()
        Segment(p.n, p.name,
            if (on) tx.phase_aria_phase_x_of_5_x_now(n = n, name = p.name) else tx.phase_aria_phase_x_of_5_x(n = n, name = p.name),
            past = ph != null && i < ph, on = on, tap = ph != null && !on && ph != N)
    }

    /**
     * The does-line under the segments: the current phase's (`Capture · bound the walls · mark what is there`, …);
     * unread, `Phase not read yet — S5 decides when online`; for a project S5 does not list, the whole never-sent
     * words (v61c.2: why the stage cannot move is read whole, on two lines if it needs them -- [wraps]); blank at
     * Client approved, where the strip is the quote view's and has no does-line.
     */
    fun doesLine(tx: Tx, line: ProjectLine): String = when (line.kind) {
        LineKind.LOCAL -> tx.wish_not_s5_yet_project
        LineKind.UNREAD -> tx.phase_phase_not_read_yet
        LineKind.QUOTE -> phaseOf(line)?.let { all(tx)[it].does } ?: tx.phase_phase_not_read_yet
        LineKind.EXEC -> ""
    }

    /** Whether the does-line may take two lines (the prototype's `data-s5none`): only the never-sent words wrap; every other line is cut with an ellipsis. */
    fun wraps(line: ProjectLine): Boolean = line.kind == LineKind.LOCAL

    /** What a tap on a segment does: nothing, asks first (a step back), or moves at once (a step forward). */
    enum class Tap { NONE, ASK, MOVE }

    /**
     * The prototype's `request`: a tap on segment [to] while the strip is at [ph]. Nothing while the stage is unread or
     * the project not on S5 (no segment is a tap then), at Client approved (approval is final), or on the current
     * segment; a step BACK asks first ([ask]); a step forward moves at once, as a staff move on /projects does.
     */
    fun tap(ph: Int?, to: Int): Tap = when {
        ph == null || ph == N || to !in 0 until N || to == ph -> Tap.NONE
        to < ph -> Tap.ASK
        else -> Tap.MOVE
    }

    /**
     * The backward confirm: the [question] (`Back to Wish list? The quote stage on S5 goes back from Estimated to
     * Client wish list.`), its [aux] sentence (`Every mark stays; the tools follow the phase.`, shown after it in
     * portrait only -- landscape keeps the question to two lines), the two buttons ([keep], `Keep Estimation`, the
     * CURRENT phase's name; [moveBack], `Move back`) and the dialog's accessible [label] (`Move the phase back`).
     */
    data class Ask(val question: String, val aux: String, val keep: String, val moveBack: String, val label: String) {
        /** The question with its aux sentence after it, as portrait draws them. */
        val whole: String get() = question + " " + aux
    }

    /** The confirm of a step back from [ph] (0..4) to [to] (below it): the names are the phases', the stages S5's through Masters. */
    fun ask(tx: Tx, ph: Int, to: Int): Ask {
        val ps = all(tx)
        return Ask(
            tx.phase_back_x_quote_stage(name = ps[to].name, quote = Masters.quote(tx, ps[ph].quote), quote2 = Masters.quote(tx, ps[to].quote)),
            tx.phase_every_mark_stays_tools, tx.phase_keep_x(name = ps[ph].name), tx.phase_move_back, tx.phase_aria_move_phase_back)
    }

    /** A phase's row under the does-line: its small-caps [label] and its [parts], joined with [SEP] when drawn on one line. */
    data class Panel(val label: String, val parts: List<String>)

    /** The separator between a row's parts, as the prototype joins them (` · `). */
    const val SEP = " · " // text:value the row's separator, as the prototype joins it

    /** SURVEY COVERAGE, at Survey: `2 of 3 rooms shot`, then `1 room not shot yet` or `every room in scope is shot`. */
    fun coverageRow(tx: Tx, c: QuoteStage.Coverage): Panel = Panel(tx.phase_survey_coverage, QuoteStage.coverageParts(tx, c))

    /**
     * IN REVISION, at Revisions: `open for the client and vendors`. The hand-off counts the prototype puts before it
     * (`3 marks in Scope · …`) are the v53/v54 port's and are absent here (answer 1); Approve the estimate ▸ is QM2's.
     */
    fun revisionRow(tx: Tx): Panel = Panel(tx.phase_in_revision, listOf(tx.phase_open_client_vendors))

    /** The strip's accessible label as a group: `Phase of this project`. */
    fun groupLabel(tx: Tx): String = tx.phase_aria_phase_project
}
