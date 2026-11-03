package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * THE APPROVED QUOTE VIEW AND THE SCOPE SHEET, the pure half (port 61c slice QV, 3 Nov 2026; SPEC61c §4.1 item 3 "At
 * Client approved, opened" and §4.2; index61c2.html stripHTML `op56` and scopeSheet). At Client approved the project
 * line is the tap that opens this view for the visit: the five quote-journey steps, every one ticked -- approval is
 * final (§13.5), so none is a tap -- then the Approved chip, which folds the view, then the APPROVED SCOPE row, which
 * reads the baseline ([scopeRow]) or, with none recorded, offers the Project Manager "Record the approved scope" and
 * tells anyone else that only the PM records it (§13.6). The scope sheet ([Mode], [title], [note], [countLine],
 * [rows], [openTicks], [submit]) is the one checklist of the sixteen works in three modes: approve (the strip's Approve
 * the estimate ▸ at Revisions, [Phases.approve], port 61c slice QM2: its Move to Client approved is the guarded move
 * carrying the scope, [QuoteMove.stage]), record (that row's button) and view (the row's tap, read-only).
 *
 * Nothing here talks to S5 or Android: the row and the sheet are drawn by :app from what the album mirror holds
 * (`scope`, `scope_at`, `role`; AlbumWire.view) and what the person ticks; the record itself is [QuoteMove.scope],
 * answered through [QuoteMove.scopeAnswer]. Every text is a `tx.<key>`; a work's name is S5's identity value shown
 * through Masters ([Album.workName]); the stamps are shown as the album shows them ([Album.whenOf], IST).
 */
object QuoteView {

    /** One of the five quote-journey steps in the opened view: its number, its name and its accessible label (`Phase n of 5: name`). */
    data class Segment(val n: Int, val name: String, val label: String)

    /**
     * The five steps as the strip names them (the prototype's PHASES, phase54.js: Survey, Wish list, Vendor review,
     * Estimation, Revisions; [Phases.names], the one home of the five since port 61c slice QM1), each ticked at
     * Client approved and none current -- so each label is the plain `Phase n of 5: name`, never the `(now)` one.
     */
    fun segments(tx: Tx): List<Segment> =
        Phases.names(tx).mapIndexed { i, name -> Segment(i + 1, name, tx.phase_aria_phase_x_of_5_x(n = (i + 1).toString(), name = name)) }

    /** The chip after the five: `Approved`; a tap folds the view. */
    fun approvedChip(tx: Tx): String = tx.phase_approved_chip

    /** The chip's accessible label: `Client approved — in execution. Tap to fold`. */
    fun approvedChipLabel(tx: Tx): String = tx.phase_aria_approved_fold

    /** The second span of the folded line at Client approved: `tap to view` (D7: only while folded). */
    fun tapToView(tx: Tx): String = tx.phase_tap_view

    /** The row's label: `APPROVED SCOPE`. */
    fun scopeLabel(tx: Tx): String = tx.phase_scope_caps

    /**
     * The APPROVED SCOPE row: its [text]; whether the text is the tap that opens the sheet read-only ([tap], a
     * baseline exists); whether `Record the approved scope` is offered ([record], the PM with no baseline); and the
     * line in place of the button for anyone else ([pmOnly], `Only the Project Manager records the approved scope`).
     */
    data class ScopeRow(val text: String, val tap: Boolean, val record: Boolean, val pmOnly: String?)

    /**
     * The APPROVED SCOPE row from the mirror's [scope] (null = no baseline), its [scopeAt] and S5's [role] for this
     * person (null until a mirror has been read: the role is S5's answer, never derived on the phone):
     * - with a baseline: `12 of 16 works · since 27 Sep, 18:00 · tap to view` (no `since` part when S5 sent no stamp);
     * - without one: `none recorded`, and for the PM the Record button; for a Designer, a vendor or a client the
     *   PM-only line; before the mirror has said the role, `none recorded` alone.
     */
    fun scopeRow(tx: Tx, scope: List<Int>?, scopeAt: String, role: String?): ScopeRow {
        if (scope != null) {
            val since = Album.whenOf(tx, scopeAt).ifEmpty { null }?.let { tx.app_since_x(stageSince = it) }
            val text = listOfNotNull(tx.phase_scope_works_x(n = scope.size, m = Album.WORKS.size), since, tapToView(tx)).joinToString(SEP)
            return ScopeRow(text, tap = true, record = false, pmOnly = null)
        }
        return ScopeRow(tx.phase_scope_none, tap = false, record = role == Album.PM, pmOnly = if (role == null || role == Album.PM) null else tx.refuse_stage_pm_scope)
    }

    private const val SEP = " · " // text:value the row's separator, as the prototype joins it

    // ---- the scope sheet (SPEC61c §4.2, with §14.6's notes; index61c2.html scopeSheet) ---------------------------

    /** The sheet's three modes: Approve the estimate ▸ (the strip's, [Phases.approve]), Record the approved scope, and the read-only view. */
    enum class Mode { APPROVE, RECORD, VIEW }

    /**
     * The ticks a sheet opens with (the prototype's openScope: `ticks: mode==='record'?[]:scopeOf(p)`): for approve the
     * [stored] scope (the mirror's; null = no baseline) -- pre-ticked, so an Approve carries on a scope recorded at
     * Estimated or Revisions on /projects, and a stale 409 redraws from S5's echo of it ([QuoteMove.Verdict.ScopeStale])
     * -- and for the view the same; none for a record (port 61c slice QM2). The sixteen only, each once.
     */
    fun openTicks(mode: Mode, stored: List<Int>?): Set<Int> =
        if (mode == Mode.RECORD) emptySet() else QuoteMove.worksOf(stored.orEmpty()).toSet()

    /**
     * What a sheet opens with, held as ONE value until an answer redraws it (QM2 review): the [ticks] and the
     * [scopeAt] they were read beside -- the mirror's scope_at as it was when the sheet opened, '' when none was
     * stored. A submit sends THAT stamp as its base ([QuoteMove.stage]'s scope_base, [QuoteMove.scope]'s base), never
     * the mirror's at submit time: a read landing between open and submit (the project-open read, SyncWorker's hourly
     * read at Revisions) moves the mirror to S5's current stamp while the ticks stay the person's, drawn from the older
     * scope, and a base read then passes S5's guard and writes those ticks over an amendment with a 200 -- the one
     * thing scope_base exists to refuse (asis.js: "a page or phone holding an older scope must not write it over an
     * amendment"). S5's own page sends the stamp it LOADED (projects.js `scope_base: p.scope_at`); so does the phone.
     */
    data class Opened(val ticks: Set<Int>, val scopeAt: String)

    /**
     * The sheet's opening ([Opened]): [openTicks] for [mode] from the [stored] scope, with the mirror's [scopeAt] as
     * they stood together -- or, [redrawn] after S5's stale 409, the scope as stored in every mode (the prototype's
     * `st.ticks=scopeOf(p)` on scopestale: a record's too), with the stamp the mirror took from S5's echo, so the next
     * submit carries S5's.
     */
    fun open(mode: Mode, stored: List<Int>?, scopeAt: String, redrawn: Boolean = false): Opened =
        Opened(if (redrawn) QuoteMove.worksOf(stored.orEmpty()).toSet() else openTicks(mode, stored), scopeAt)

    /** One of the sixteen rows, in WORKS order: the work, whether it is ticked, and whether the tick is disabled (view). */
    data class Row(val id: Int, val on: Boolean, val disabled: Boolean)

    /** `Works in the approved estimate` for approve and record; `Approved scope` for the view. */
    fun title(tx: Tx, mode: Mode): String = if (mode == Mode.VIEW) tx.phase_scope_approved else tx.phase_scope_title

    /**
     * One note per mode, verbatim from S5 /projects' scopeHtml (§14.6): the approve note, the record note, and in
     * the view `the baseline since 27 Sep, 18:00 — approval is final: …` -- blank when S5 sent no stamp, as the
     * prototype draws it.
     */
    fun note(tx: Tx, mode: Mode, scopeAt: String): String = when (mode) {
        Mode.APPROVE -> tx.phase_scope_note
        Mode.RECORD -> tx.phase_scope_record_note
        Mode.VIEW -> Album.whenOf(tx, scopeAt).ifEmpty { null }?.let { tx.phase_scope_since_x(hm = it) }.orEmpty()
    }

    /** `{n} of 16 works ticked`, counting only the sixteen; null in the view, which has no count line. */
    fun countLine(tx: Tx, mode: Mode, ticks: Collection<Int>): String? =
        if (mode == Mode.VIEW) null else tx.phase_scope_ticked_x(n = QuoteMove.worksOf(ticks).size, m = Album.WORKS.size)

    /** The sixteen rows in WORKS order, ticked by [ticks]; in the view every row is disabled (`cb(on, true)`). */
    fun rows(mode: Mode, ticks: Collection<Int>): List<Row> =
        Album.WORK_IDS.map { Row(it, on = it in ticks, disabled = mode == Mode.VIEW) }

    /** The name a row shows: the work's, through Masters. */
    fun rowName(tx: Tx, id: Int): String = Album.workName(tx, id)

    /** [ticks] after a tap on [id]: toggled; a tap on a row in the view changes nothing. */
    fun toggled(mode: Mode, ticks: Set<Int>, id: Int): Set<Int> = when {
        mode == Mode.VIEW || !Album.isWork(id) -> ticks
        id in ticks -> ticks - id
        else -> ticks + id
    }

    /** The text button at the foot: `Close` in the view, `Cancel` otherwise. */
    fun closeLabel(tx: Tx, mode: Mode): String = if (mode == Mode.VIEW) tx.common_close else tx.common_cancel

    /** The filled button: `Move to Client approved` (approve), `Record the approved scope` (record); none in the view. */
    fun submitLabel(tx: Tx, mode: Mode): String? = when (mode) {
        Mode.APPROVE -> tx.phase_scope_move_approved
        Mode.RECORD -> tx.phase_scope_record
        Mode.VIEW -> null
    }

    /**
     * What a tap on the filled button sends: the ticked works, each once, in WORKS order -- or null when nothing is
     * ticked, which the sheet refuses itself in S5's words ([emptyRefusal]), nothing called and nothing logged (§4.2
     * "Move with no tick"), as [QuoteMove.scope] would refuse it too.
     */
    fun submit(ticks: Collection<Int>): List<Int>? = QuoteMove.worksOf(ticks).ifEmpty { null }

    /** The status line for a submit with nothing ticked: `Tick the works in the approved estimate before moving to Client approved`. */
    fun emptyRefusal(tx: Tx): String = QuoteMove.localText(tx, QuoteMove.Local.SCOPE_MISSING)
}
