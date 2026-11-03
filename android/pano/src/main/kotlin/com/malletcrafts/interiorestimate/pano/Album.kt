package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random

/**
 * THE ALBUM BY WORKS, ON THE PHONE -- the pure rules (S2 APK v61b slice 2, Kotlin slice K1;
 * brought to S5 as shipped and keyed in port 61c slice K1b, 3 Nov 2026).
 *
 * Amit, 23 Oct 2026, 06:47 IST, after testing prototype v59: "Client/Vendor/Designer/Project
 * Manager (aka supervisor) these are typical roles ... The whole suite should revolve around
 * these roles. the stages should be from work point of view ... delivery is vague as lot of
 * things get delivered. and stages can not be sequential as many work can happen in parallel."
 * So the album is built around the WORK: chapter 0 is the quote journey (S5 QUOTE's six quote
 * stages, holding the as-is survey captures); chapters 1-16 are S5's sixteen payment work
 * groups ([WORKS]), each with its own STATE that only the Project Manager sets, because many
 * works run at once. Four ROLES see four albums ([canSee] is the one rule every count, chapter
 * and tile goes through); vendors UPLOAD photos into their own work and the Project Manager
 * ACCEPTS them (the client sees them from then on) or keeps them Not for client; every work
 * carries its PAYMENT milestones, in S5's own words, as the evidence its photos are for.
 *
 * The spec is the approved prototype v61c.1 (window.ALB60, index61c1.html 7100-7530) over
 * v61b's core; the store is S5 QUOTE as shipped on stage (functions/_lib/asis.js: one
 * append-only log, one photo table, `GET .../album`). Since 27 Oct 2026 APPROVAL IS FINAL and a
 * change after it is a CHANGE REQUEST with a KIND ([CrKind]: add a work off the approved scope,
 * remove or change one in it), which the Project Manager raises and then ANSWERS with the
 * client's yes or no ([CrOutcome], with the day the client answered); an approved add or remove
 * amends the scope ([amendedScope]). Nothing here talks to a server and nothing here is Android:
 * this is the vocabulary, the derivations, the writer gates and the transitions of what the phone
 * keeps -- the mirror of S5's last GET and the FIFO outbox -- under the verdict table of "Offline
 * and conflicts". AlbumTest pins all of it.
 *
 * WHAT IS STORED STAYS ENGLISH, WHAT IS SHOWN COMES FROM Tx (port 61c slice K1b). The work
 * names, the five states and the 39 worksite stages are S5's identity values, sent and stored
 * byte for byte and SHOWN through [Masters]; every other text is a `tx.<key>` from android/i18n,
 * read through a function here that takes a [Tx] ([workName], [payOf], [Refusal.text], [dayOf]).
 * The gates return a typed [Refusal], never a sentence. Names, captions, remarks, notes and dates
 * are data.
 *
 * Current state is DERIVED, never cached: a work's state comes from the caller's project-state
 * store ([WorkState], S5-L's `work_states` and the album GET's `works[]`, with this phone's queued
 * writes laid over it by [withQueued]); the log is the history ([stateOf], [crOf], [crDone]).
 * Order is by `seq`, never by time. Identity is whoami's `user` / `full_name`, never the
 * prototype's ME, and the role is S5's answer ([viewerOf]), never derived on the phone.
 */
object Album {

    // ---- vocabulary ---------------------------------------------------------

    /** One of the sixteen works: S5's id and its name (an identity value, shown through [Masters.work]). */
    data class WorkDef(val id: Int, val name: String)

    /**
     * S5 QUOTE's sixteen payment work groups (asis.js WORKS = app1_data.js PHASES @ ca74117), ids
     * and names verbatim and in S5's order; the prototype's WORKS. Chapter n = index + 1. Change
     * here only with S5's WORKS.
     */
    val WORKS: List<WorkDef> = listOf(
        WorkDef(10, "Demolition & site preparation"), // text:value S5's work name, shown through Masters.work
        WorkDef(20, "Civil & masonry"), // text:value S5's work name, shown through Masters.work
        WorkDef(30, "Plumbing, concealed"), // text:value S5's work name, shown through Masters.work
        WorkDef(40, "Electrical, concealed wiring"), // text:value S5's work name, shown through Masters.work
        WorkDef(50, "Waterproofing"), // text:value S5's work name, shown through Masters.work
        WorkDef(60, "False ceiling"), // text:value S5's work name, shown through Masters.work
        WorkDef(70, "Flooring & wall tiling"), // text:value S5's work name, shown through Masters.work
        WorkDef(80, "Countertops"), // text:value S5's work name, shown through Masters.work
        WorkDef(85, "Iron & metal fabrication"), // text:value S5's work name, shown through Masters.work
        WorkDef(90, "Modular furniture, factory-made"), // text:value S5's work name, shown through Masters.work
        WorkDef(95, "Onsite carpentry & panelling"), // text:value S5's work name, shown through Masters.work
        WorkDef(100, "Painting & wall finishes"), // text:value S5's work name, shown through Masters.work
        WorkDef(110, "Sanitaryware & CP fittings"), // text:value S5's work name, shown through Masters.work
        WorkDef(120, "Lights, fans, fixtures & security"), // text:value S5's work name, shown through Masters.work
        WorkDef(130, "Glass, mirrors & furnishing"), // text:value S5's work name, shown through Masters.work
        WorkDef(140, "Cleaning & handover"), // text:value S5's work name, shown through Masters.work
    )

    val WORK_IDS: List<Int> = WORKS.map { it.id }

    /** The quote journey, chapter 0: a photo's `work` of 0. */
    const val QUOTE_WORK = 0

    /** The last work; the album closes for display when it is Done ([Chapters.closed]). */
    const val LAST_WORK = 140

    /** The five states, in the prototype's order (v61a: On hold and Abandoned joined the three). Shown through [Masters.state]. */
    const val NOT_STARTED = "Not started" // text:value S5's state, stored and sent as is
    const val IN_PROGRESS = "In progress" // text:value S5's state, stored and sent as is
    const val ON_HOLD = "On hold" // text:value S5's state, stored and sent as is
    const val DONE = "Done" // text:value S5's state, stored and sent as is
    const val ABANDONED = "Abandoned" // text:value S5's state, stored and sent as is
    val STATES: List<String> = listOf(NOT_STARTED, IN_PROGRESS, ON_HOLD, DONE, ABANDONED)

    /** The four roles as S5 answers them (`role` on the album GET) and stores them on every row. */
    const val PM = "pm"
    const val DESIGNER = "designer"
    const val VENDOR = "vendor"
    const val CLIENT = "client"
    val ROLE_IDS: List<String> = listOf(PM, DESIGNER, VENDOR, CLIENT)

    /** The log row kinds (`asis_album_log.kind`). */
    const val KIND_STATE = "state"
    const val KIND_CR_RAISE = "cr_raise"
    const val KIND_CR_CLEAR = "cr_clear"
    const val KIND_REVIEW = "review"
    const val KIND_STAGE = "stage"

    /** The kinds a log POST writes, each with a seq base (validLogEntry's ALBUM_KINDS). */
    private val LOG_KINDS = setOf(KIND_STATE, KIND_CR_RAISE, KIND_CR_CLEAR)

    /** The photo parties and reviews (`asis_album_photos.party` / `.review`). */
    const val PARTY_STAFF = "staff"
    const val PARTY_VENDOR = "vendor"
    const val REVIEW_PENDING = "pending"
    const val REVIEW_ACCEPTED = "accepted"
    const val REVIEW_INTERNAL = "internal"
    const val PHOTO = "photo"
    const val PHOTO_360 = "360"

    /**
     * S5's caps (asis.js ALBUM_MAX_NOTE, _CAPTION, _ROOM), each on the TRIMMED text: a remark or a CR
     * note, a photo's caption, a photo's room. Over a cap S5 answers 400, which drops the op for good,
     * so the op builders trim and cut to it ([clip]).
     */
    const val MAX_NOTE = 500
    const val MAX_CAPTION = 500
    const val MAX_ROOM = 140

    /**
     * A change request's KIND (27 Oct 2026; S5 CR_KINDS), on the cr_raise row's `value`: [ADD] a
     * work outside the approved scope comes in; [REMOVE] a work in it comes out; [CHANGE] a work in
     * it changes. A raise v61b wrote has no value and reads [ADD], the only kind v61b had.
     */
    enum class CrKind(val wire: String) {
        ADD("add"), REMOVE("remove"), CHANGE("change");

        /** The kind as the chip and the decided list show it (crkind.*). */
        fun word(tx: Tx): String = when (this) { ADD -> tx.crkind_add; REMOVE -> tx.crkind_remove; CHANGE -> tx.crkind_change }

        /** The line under the kind on the raise sheet (album.cr_kind_*_sub). */
        fun sub(tx: Tx): String = when (this) {
            ADD -> tx.album_cr_kind_add_sub; REMOVE -> tx.album_cr_kind_remove_sub; CHANGE -> tx.album_cr_kind_change_sub
        }

        companion object {
            /** The kind S5 wrote, or null when [v] is none of the three (a blank raise reads ADD through [Entry.crKind]). */
            fun of(v: String?): CrKind? = entries.firstOrNull { it.wire == v }
        }
    }

    /**
     * The client's ANSWER to a change request (S5 CR_OUTCOMES), on the cr_clear row's `value`, which
     * the Project Manager records with `client_at`, the day the client answered (required for
     * [APPROVED] and [DECLINED]) and the note saying how. A clear v61b wrote has no value and reads
     * [WITHDRAWN], all an answer could be before outcomes.
     */
    enum class CrOutcome(val wire: String) {
        APPROVED("approved"), DECLINED("declined"), WITHDRAWN("withdrawn");

        /** Only a withdrawal may leave out the day the client answered (S5's validLogEntry). */
        val needsDay: Boolean get() = this != WITHDRAWN

        /** The decided request's head (album.cr_out_*). */
        fun text(tx: Tx): String = when (this) {
            APPROVED -> tx.album_cr_out_approved; DECLINED -> tx.album_cr_out_declined; WITHDRAWN -> tx.album_cr_out_withdrawn
        }

        companion object {
            fun of(v: String?): CrOutcome? = entries.firstOrNull { it.wire == v }
        }
    }

    /** One payment milestone of a work: before (`s`) or when done (`e`), and the work it keys off. */
    data class Pay(val kind: Char, val at: Int)

    /**
     * S5's PAY_TERMS (app1_data.js) as the prototype's PAY: each work's distinct (kind, at)
     * pairs, kind 's' = "Before <at> starts", 'e' = "When <at> is done" (paySchedule's labelling
     * rule, worded by [payOf]). No amounts, no percentages -- the evidence a work's photos are
     * for, not its money.
     */
    val PAY: Map<Int, List<Pay>> = mapOf(
        10 to listOf(Pay('s', 10), Pay('e', 10)),
        20 to listOf(Pay('s', 20), Pay('e', 20), Pay('e', 100)),
        30 to listOf(Pay('s', 30), Pay('e', 30), Pay('e', 110)),
        40 to listOf(Pay('s', 40), Pay('e', 40), Pay('s', 120)),
        50 to listOf(Pay('s', 50), Pay('e', 50), Pay('e', 140)),
        60 to listOf(Pay('s', 60), Pay('e', 60), Pay('s', 100)),
        70 to listOf(Pay('s', 70), Pay('e', 70), Pay('e', 140)),
        80 to listOf(Pay('s', 80), Pay('e', 80)),
        85 to listOf(Pay('s', 85), Pay('e', 85), Pay('e', 100)),
        90 to listOf(Pay('s', 90), Pay('e', 90), Pay('e', 140)),
        95 to listOf(Pay('s', 95), Pay('e', 95), Pay('e', 100)),
        100 to listOf(Pay('s', 100), Pay('e', 100), Pay('e', 140)),
        110 to listOf(Pay('s', 110), Pay('e', 110)),
        120 to listOf(Pay('s', 120), Pay('e', 120)),
        130 to listOf(Pay('s', 130), Pay('e', 130)),
        140 to listOf(Pay('e', 140)),
    )

    /**
     * The 39 worksite stages (mallet_estimator worksite_data.py WORK_STAGES) -> the work that
     * does them; 0 = the quote journey; null = not filed to a work (a delivery photo belongs to the
     * work being delivered, and only a human can say which). S5's STAGE_WORK, verbatim. Since v61c
     * the ONLY reader is [workOfCapture]: a capture taken before the phone filed by work keeps its
     * legacy stage, and this table files it.
     */
    val STAGE_WORK: Map<String, Int?> = mapOf(
        "As-built site measure" to 0, "Existing condition — builder handover" to 0, "Defect recorded" to 0, "Opening measured" to 0, // text:value worksite stages, shown through Masters.stage
        "Demolition & debris removal" to 10, "Masonry & block work" to 20, "Plaster & waterproofing" to 20, "Door & window frames (chowkat)" to 20, // text:value worksite stages
        "Electrical first fix — conduit & wiring" to 40, "Plumbing first fix — concealed lines" to 30, "HVAC / AC piping & drain" to 40, "Data, network & CCTV conduit" to 40, // text:value worksite stages
        "False ceiling framing (GI grid)" to 60, "POP / gypsum board & finish" to 60, "Cove & profile-light recesses" to 60, // text:value worksite stages
        "Floor tiling" to 70, "Wall tiling / dado" to 70, "Wooden, vinyl or laminate flooring" to 70, "Stone & marble polish" to 70, // text:value worksite stages
        "Material delivered on site" to null, "Modular units delivered" to 90, // text:value worksite stages
        "Wall panelling" to 95, "Wall moulding & trims" to 95, "Modular carpentry install" to 90, "Loose furniture" to 130, // text:value worksite stages
        "Electrical second fix — switches & points" to 40, "Light installation" to 120, "Plumbing second fix — CP & sanitary" to 110, // text:value worksite stages
        "Window jali, mesh & grill" to 85, "Doors, shutters & hardware" to 95, // text:value worksite stages
        "Putty & primer" to 100, "Paint, wallpaper or texture" to 100, "Glass & mirrors" to 130, "Curtains & blinds" to 130, "Deep clean" to 140, // text:value worksite stages
        "Snagging" to 140, "Snag rectification" to 140, "Rectified & verified" to 140, "Handover" to 140, // text:value worksite stages
    )

    // ---- the refusals: typed, worded only at the screen through Tx ----------------

    /**
     * Why a writer gate refused (nothing is written). The prototype has no words for most of these
     * -- it hides the control instead (the state sheet marks the current state, Raise shows only at
     * Client approved off a baseline with no live request, the vendor's Upload only in `mine`) -- so
     * the phone's own lines are the v61c card's `refuse.album.*` keys (L2), worded the §13 way and,
     * where S5 has words, in S5's (asis.js). On a queue row S5's own `error` replaces them.
     */
    enum class Refusal {
        STATE_PM_ONLY, EXEC_LOCKED, NOT_A_WORK, NOT_A_STATE, SAME_STATE, DONE_REASON_NEED,
        CR_PM_ONLY, CR_NOT_APPROVED, CR_NO_BASELINE, CR_LIVE, CR_IN_SCOPE, CR_OFF_SCOPE_REMOVE, CR_OFF_SCOPE_CHANGE,
        CR_NONE, CLIENT_AT_DAY,
        PHOTO_NO_CLIENT, NOT_YOUR_WORK,
        REVIEW_PM_ONLY, NOT_A_REVIEW, REVIEW_VENDOR_ONLY, REVIEW_NO_IMAGE, REVIEW_SAME;

        fun text(tx: Tx): String = when (this) {
            STATE_PM_ONLY -> tx.album_state_pm_only
            EXEC_LOCKED -> tx.album_exec_lock_banner
            NOT_A_WORK -> tx.refuse_album_not_a_work
            NOT_A_STATE -> tx.refuse_album_not_a_state
            SAME_STATE -> tx.refuse_album_same_state
            DONE_REASON_NEED -> tx.album_done_reason_need
            CR_PM_ONLY -> tx.refuse_album_cr_pm_only
            CR_NOT_APPROVED -> tx.refuse_album_cr_not_approved
            CR_NO_BASELINE -> tx.refuse_album_cr_no_baseline
            CR_LIVE -> tx.refuse_album_cr_live
            CR_IN_SCOPE -> tx.refuse_album_cr_in_scope
            CR_OFF_SCOPE_REMOVE -> tx.refuse_album_cr_off_scope_remove
            CR_OFF_SCOPE_CHANGE -> tx.refuse_album_cr_off_scope_change
            CR_NONE -> tx.refuse_album_cr_none
            CLIENT_AT_DAY -> tx.refuse_album_client_at_day
            PHOTO_NO_CLIENT -> tx.refuse_album_photo_no_client
            NOT_YOUR_WORK -> tx.album_not_your_work
            REVIEW_PM_ONLY -> tx.refuse_album_review_pm_only
            NOT_A_REVIEW -> tx.refuse_album_not_review_state
            REVIEW_VENDOR_ONLY -> tx.refuse_album_review_vendor_only
            REVIEW_NO_IMAGE -> tx.refuse_album_review_no_image
            REVIEW_SAME -> tx.refuse_album_review_same
        }
    }

    // ---- the model, shaped like S5's GET .../album answer --------------------

    /**
     * One row of the append-only log (`log[]` and `stages[]`). `work` null on stage rows; `photo`
     * on review rows. A `stages[]` row comes from S5's stageView, which sends NO `kind` (S5's query is
     * `kind = 'stage'`), so a mapper may leave [kind] blank there: [quoteAt] reads every row it is
     * given as a stage move. A change request's rows read the way S5's logView reads them
     * ([crKind], [crOutcome]): a raise's `value` is its kind, a clear's `value` the client's answer
     * and its `prev` the kind it answered.
     */
    data class Entry(
        val seq: Long,
        val id: String,
        val kind: String,
        val work: Int? = null,
        val value: String = "",
        val prev: String = "",
        val note: String = "",
        val role: String = "",
        val by: String = "",
        val byName: String = "",
        val at: String = "",
        val photo: String? = null,
        val via: String = "",
        val clientAt: String? = null,
    ) {
        /** A raise's kind, or the kind a clear answered; a blank one is [CrKind.ADD]; null on any other row. */
        val crKind: CrKind? get() = when (kind) {
            KIND_CR_RAISE -> CrKind.of(value.ifBlank { null }) ?: CrKind.ADD
            KIND_CR_CLEAR -> CrKind.of(prev.ifBlank { null }) ?: CrKind.ADD
            else -> null
        }

        /** A clear's answer; a blank one is [CrOutcome.WITHDRAWN]; null on any other row. */
        val crOutcome: CrOutcome? get() = if (kind == KIND_CR_CLEAR) CrOutcome.of(value.ifBlank { null }) ?: CrOutcome.WITHDRAWN else null
    }

    /**
     * One of `works[16]`: S5's derived row for a work. `seq` is the BASE (the work's newest seq of
     * any kind); `at` / `by` are its newest STATE row's, blank when none was ever recorded. `cr` is
     * the live cr_raise row, if any, its kind in `value`.
     */
    data class Work(
        val id: Int,
        val state: String = NOT_STARTED,
        val by: String = "",
        val byName: String = "",
        val at: String = "",
        val seq: Long = 0,
        val inScope: Boolean = true,
        val cr: Entry? = null,
        val count: Int = 0,
    ) {
        val crKind: CrKind? get() = cr?.crKind
    }

    /**
     * One work's state as the project-state store holds it: the state, the seq S5 knows it at
     * (S5-L's `work_states` gives the state row's own seq, the album GET's `works[]` the base) and
     * the state row's time when known. A work absent from a states map has no recorded state.
     */
    data class WorkState(val state: String, val seq: Long = 0, val at: String = "")

    /**
     * One album item: a photo row of `photos[]`, or an as-is capture (`captures[]`, or the
     * phone's own CaptureStore row filed through [workOfCapture]) as the same shape with `kind`
     * [PHOTO_360] and party [PARTY_STAFF]. `work` 0 = quote journey, null = unfiled; `vendor`
     * the ERP Supplier as spelt on the access list; `review` matters only on a vendor's row.
     */
    data class Photo(
        val id: String,
        val work: Int?,
        val at: String,
        val party: String = PARTY_STAFF,
        val room: String? = null,
        val kind: String = PHOTO,
        val caption: String = "",
        val vendor: String? = null,
        val role: String = "",
        val by: String = "",
        val byName: String = "",
        val review: String = REVIEW_PENDING,
        val reviewBy: String? = null,
        val reviewByName: String? = null,
        val reviewAt: String? = null,
        val bytes: Long = 0,
        val pairOf: String? = null,
        val via: String = "",
        val clientAt: String? = null,
    )

    /**
     * S5's `GET .../album` answer -- the phone's MIRROR of the project's album. `role` is S5's
     * albumRole for this caller ([viewerOf]); `quoteStage` / `quoteStageAt` / `quoteStageBy` null / blank
     * when S5 sent none (the stage, its stamp and who set it go into the project-state store,
     * [QuoteStage.fromAlbum]); `scope` null = no baseline; `pm` null = every staff member is PM. `toReview`,
     * `changeRequests`, `closed` and `closedAt` are S5's figures AS SENT, for a list; the screen
     * derives its own through [chapters], so a merge after a POST never leaves a count behind.
     * `logNext` / `logMore` are S5's cursor: a `?since=` page is followed while `logMore`
     * ([paged]). `stages` are S5's `stages[]`, every row a stage move (stageView sends no `kind`).
     */
    data class View(
        val role: String,
        val mine: List<Int> = emptyList(),
        val exec: Boolean = false,
        val scope: List<Int>? = null,
        val scopeAt: String = "",
        val pm: String? = null,
        val quoteStage: String? = null,
        val quoteStageAt: String = "",
        val quoteStageBy: String = "",
        val works: List<Work> = emptyList(),
        val log: List<Entry> = emptyList(),
        val logNext: Long = 0,
        val logMore: Boolean = false,
        val stages: List<Entry> = emptyList(),
        val photos: List<Photo> = emptyList(),
        val captures: List<Photo> = emptyList(),
        val toReview: Int = 0,
        val changeRequests: Int = 0,
        val closed: Boolean = false,
        val closedAt: String? = null,
        val more: Boolean = false,
    )

    /** Who is looking: the role, the vendor's works (`mine`) and the vendor's own supplier keys ([partyKey]). */
    data class Viewer(val role: String, val mine: Set<Int> = emptySet(), val suppliers: Set<String> = emptySet()) {
        val staff: Boolean get() = role == PM || role == DESIGNER
    }

    // ---- lookups ----------------------------------------------------------

    fun isWork(id: Int?): Boolean = id != null && WORK_IDS.contains(id)

    /** The chapter an item sits in: 0 the quote journey, index + 1 for a work, null when unfiled or not a work. */
    fun chapterOf(work: Int?): Int? = when {
        work == null -> null
        work == QUOTE_WORK -> 0
        else -> WORK_IDS.indexOf(work).let { if (it < 0) null else it + 1 }
    }

    /** The name of a work as shown: 0 the quote journey, null not filed to a work, a work its Masters name, an unknown id "". */
    fun workName(tx: Tx, id: Int?): String = when {
        id == null -> tx.album_unfiled
        id == QUOTE_WORK -> tx.album_quote_journey
        else -> WORKS.firstOrNull { it.id == id }?.let { Masters.work(tx, it.name) } ?: ""
    }

    /** A state as the five name it; anything else is [NOT_STARTED]. The stored value: show it through [stateText]. */
    fun stateName(state: String?): String = if (state != null && state in STATES) state else NOT_STARTED

    /** A state as shown. */
    fun stateText(tx: Tx, state: String?): String = Masters.state(tx, stateName(state))

    /** A role as the album's "Viewing as" names it; a role S5 might add later is shown as sent. */
    fun roleLabel(tx: Tx, role: String): String = when (role) {
        PM -> tx.role_project_manager
        DESIGNER -> tx.role_designer
        VENDOR -> tx.common_vendor
        CLIENT -> tx.common_client
        else -> role
    }

    /** The line under a role (the prototype's ROLES); "" for a role the four do not hold. */
    fun roleLine(tx: Tx, role: String): String = when (role) {
        PM -> tx.album_role_pm_line
        DESIGNER -> tx.album_role_designer_line
        VENDOR -> tx.album_role_vendor_line
        CLIENT -> tx.album_role_client_line
        else -> ""
    }

    /** A work's payment milestones in words: "Before X starts" / "When X is done". */
    fun payOf(tx: Tx, id: Int): List<String> = (PAY[id] ?: emptyList()).map {
        if (it.kind == 's') tx.album_pay_before_x(work = workName(tx, it.at)) else tx.album_pay_when_x(work = workName(tx, it.at))
    }

    /**
     * The work a worksite stage files to: exact, then case-insensitive (as the prototype's
     * workOfStage). A stage not among the 39, blank or null files to the quote journey (0); a
     * stage mapped to null is unfiled (null) -- S5's own filing rule ("unknown -> 0, a null
     * mapping -> unfiled").
     */
    fun workOfStage(stage: String?): Int? {
        if (stage.isNullOrBlank()) return QUOTE_WORK
        if (STAGE_WORK.containsKey(stage)) return STAGE_WORK[stage]
        val k = STAGE_WORK.keys.firstOrNull { it.equals(stage, ignoreCase = true) } ?: return QUOTE_WORK
        return STAGE_WORK[k]
    }

    /**
     * THE ONE FILING RULE (v61c, the prototype's workOfCapture; S5's workOfRecord): a capture's
     * `work` stands when it is 0 (the quote journey) or one of the sixteen; anything else -- NULL,
     * the store's "filed by its stage" for every capture taken before the phone filed by work, or a
     * stray id -- files by its legacy `stage` through [workOfStage]. The ONLY place [STAGE_WORK]
     * is read.
     */
    fun workOfCapture(work: Int?, stage: String?): Int? =
        if (work == QUOTE_WORK || isWork(work)) work else workOfStage(stage)

    /** S5's partyKey: trimmed and lower-cased, so a login or a Supplier is matched as ERP matches it. */
    fun partyKey(v: String?): String = v?.trim()?.lowercase() ?: ""

    /**
     * Who the person is in this album: S5's own answer (`role`, `mine`; asis.js albumView), never a
     * rule of the phone's -- S5 decides per project (D3: while no PM is named every staff member is
     * PM; a vendor only where one of its Suppliers is listed). [suppliers] are whoami's, matched as
     * [partyKey], for the vendor's own uploads.
     */
    fun viewerOf(v: View, suppliers: Collection<String> = emptyList()): Viewer =
        Viewer(v.role, v.mine.filter { isWork(it) }.toSet(), suppliers.map { partyKey(it) }.filter { it.isNotEmpty() }.toSet())

    // ---- time: S5's stamps are UTC, the album shows IST ---------------------

    /** S5's isDay: YYYY-MM-DD that exists (2026-02-30 does not), a time after it allowed and ignored. */
    fun isDay(t: String?): Boolean {
        val m = DAY.find(t?.trim().orEmpty()) ?: return false
        return try { LocalDate.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()); true } catch (e: Exception) { false }
    }

    private val DAY = Regex("""^(\d{4})-(\d{2})-(\d{2})(?:T|$)""")
    private val BARE_DAY = Regex("""^\d{4}-\d{2}-\d{2}$""")

    /** The IST calendar day [iso] names: a bare YYYY-MM-DD as it is (a day, not a moment), else the moment in IST. */
    private fun istDay(iso: String?): LocalDate? {
        val s = iso?.trim().orEmpty()
        if (BARE_DAY.matches(s)) return try { LocalDate.parse(s) } catch (e: Exception) { null }
        return QuoteStage.instantOf(s)?.atZone(QuoteStage.IST)?.toLocalDate()
    }

    /** The album's months, "Sep" for September (index61c1.html 7156), from the table (decision 3 of 21 Oct: Jan-Dec). */
    private fun month(tx: Tx, m: Int): String = when (m) {
        1 -> tx.date_month_1; 2 -> tx.date_month_2; 3 -> tx.date_month_3; 4 -> tx.date_month_4
        5 -> tx.date_month_5; 6 -> tx.date_month_6; 7 -> tx.date_month_7; 8 -> tx.date_month_8
        9 -> tx.date_month_9; 10 -> tx.date_month_10; 11 -> tx.date_month_11; else -> tx.date_month_12
    }

    /** "28 Oct" in IST; "" when [iso] is not a time or a day. */
    fun dayOf(tx: Tx, iso: String?): String {
        val d = istDay(iso) ?: return ""
        return "${d.dayOfMonth} ${month(tx, d.monthValue)}"
    }

    /**
     * The prototype's whenOf: "28 Oct, 01:30" in IST, or the day alone when [dateOnly] (a capture
     * that carries only its date). A bare day has no time, so it is always the day alone.
     */
    fun whenOf(tx: Tx, iso: String?, dateOnly: Boolean = false): String {
        val day = dayOf(tx, iso)
        if (dateOnly || day.isEmpty()) return day
        val z = QuoteStage.instantOf(iso?.trim().orEmpty())?.atZone(QuoteStage.IST) ?: return day
        return "$day, ${z.hour.toString().padStart(2, '0')}:${z.minute.toString().padStart(2, '0')}"
    }

    /** Epoch ms of [iso], 0 when it is not a time (the prototype's t0). */
    private fun t0(iso: String?): Long = QuoteStage.instantOf(iso?.trim().orEmpty())?.toEpochMilli() ?: 0L

    // ---- the derivations: the log is history, by seq ---------------------------

    private fun bySeq(log: List<Entry>): List<Entry> = log.sortedBy { it.seq }

    /** The newest log row of [kinds] on [work], by seq. */
    fun newest(log: List<Entry>, work: Int, vararg kinds: String): Entry? =
        bySeq(log).lastOrNull { it.work == work && it.kind in kinds }

    /** The newest `state` row of a work, or null when none was ever recorded. */
    fun entryOf(log: List<Entry>, work: Int): Entry? = newest(log, work, KIND_STATE)

    /** A work's state by its newest `state` row, else Not started (a value outside the five reads as Not started). */
    fun stateOf(log: List<Entry>, work: Int): String = stateName(entryOf(log, work)?.value)

    /** The works In progress by [states], in S5's order: the header's "Open: …". */
    fun openWorks(states: Map<Int, WorkState>): List<Int> = WORK_IDS.filter { stateName(states[it]?.state) == IN_PROGRESS }

    /** The live change request on a work: the newest cr-kind row, when it is a `cr_raise`; else null. */
    fun crOf(log: List<Entry>, work: Int): Entry? =
        newest(log, work, KIND_CR_RAISE, KIND_CR_CLEAR)?.takeIf { it.kind == KIND_CR_RAISE }

    /** In the approved scope: a null baseline (not yet approved) puts everything in scope -- nothing to drift from. */
    fun inScope(scope: List<Int>?, work: Int): Boolean = scope == null || scope.contains(work)

    /** One LIVE change request as the header counts it. */
    data class ChangeRequest(val id: Int, val cr: Entry) {
        val kind: CrKind get() = cr.crKind ?: CrKind.ADD
    }

    /**
     * "Change requests: N" -- every LIVE request (raised, not yet answered), WHATEVER ITS KIND, in
     * S5's order (asis.js `change_requests: works.filter(w => w.cr).length`, 27 Oct 2026): a remove
     * or a change is on a work IN the scope and an approved add brings its work into it, so "off
     * the baseline" no longer says anything.
     */
    fun changeRequests(log: List<Entry>): List<ChangeRequest> =
        WORKS.mapNotNull { w -> crOf(log, w.id)?.let { ChangeRequest(w.id, it) } }

    /**
     * One decided request: the client's [outcome], the request's [kind], and [day] -- the day the
     * client answered (`client_at`, when S5's isDay holds) or else the day the answer was recorded,
     * in IST -- as YYYY-MM-DD. §13.5: "the album keeps the decided ones in the work's log"; §14.9:
     * "a withdrawal with no client date shows the day it was recorded".
     */
    data class Decided(val outcome: CrOutcome, val kind: CrKind, val day: String, val entry: Entry)

    /** A work's answered requests, newest first (index61c1.html 7441-7442). */
    fun crDone(log: List<Entry>, work: Int): List<Decided> =
        bySeq(log).filter { it.work == work && it.kind == KIND_CR_CLEAR }.asReversed().map { e ->
            val day = e.clientAt?.trim()?.takeIf { isDay(it) }?.take(10) ?: istDay(e.at)?.toString().orEmpty()
            Decided(e.crOutcome ?: CrOutcome.WITHDRAWN, e.crKind ?: CrKind.ADD, day, e)
        }

    /** A decided request as the work's list draws it: "Change approved by client · 27 Oct · add". */
    fun decidedText(tx: Tx, d: Decided): String = listOf(d.outcome.text(tx), dayOf(tx, d.day), d.kind.word(tx)).joinToString(" · ")

    /**
     * The scope after the client's answer, as S5 amends it in the same batch as the cr_clear row:
     * an approved add puts the work in, an approved remove takes it out (in S5's order), anything
     * else leaves [scope] as it is.
     */
    fun amendedScope(scope: List<Int>?, kind: CrKind, outcome: CrOutcome, work: Int): List<Int>? {
        if (outcome != CrOutcome.APPROVED) return scope
        val was = scope ?: emptyList()
        return when (kind) {
            CrKind.ADD -> (was + work).distinct().sorted()
            CrKind.REMOVE -> was.filter { it != work }
            CrKind.CHANGE -> scope
        }
    }

    /** True when any `state` row exists in [log]: the recorded-work exemption from the lock (ct61b D3). */
    fun hasStateEntry(log: List<Entry>): Boolean = log.any { it.kind == KIND_STATE }

    /**
     * Whether the work chapters are open for work. S5's rule: the quote stage is Client
     * approved OR a state row exists. The phone unlocks as the prototype's execMode does:
     * a stage it has not read (null or blank) or a name outside the six is execution too,
     * because the strip cannot place it -- S5 is the authority and may answer the lock banner
     * on reconnect (the queue row then says so). Survey through Revisions lock (ct61b D1);
     * Client approved releases (D2); recorded work stays unlocked whatever the stage (D3).
     */
    fun exec(quoteStage: String?, hasStateEntry: Boolean): Boolean {
        if (hasStateEntry) return true
        val s = quoteStage?.trim().orEmpty()
        if (s.isEmpty()) return true
        if (QuoteStage.indexOf(s) < 0) return true
        return s == QuoteStage.APPROVED
    }

    /** The quote stage IS Client approved: the only stage at which a change request is raised or answered (§14.8). */
    fun approved(quoteStage: String?): Boolean = quoteStage?.trim() == QuoteStage.APPROVED

    /** Only leaving Done needs a remark (v61a's Done guard). */
    fun remarkNeeded(from: String): Boolean = from == DONE

    /**
     * The quote stage a quote-journey item was taken in, as the prototype's quoteAt: the logged
     * moves (`stage` rows) AND the current stage at its quote_stage_at ([current], [currentAt])
     * are one history, in time order; the newest point at or before [at] wins, else Survey. The
     * current point matters for a project that stood at a later stage before migration 0008 (26
     * Oct 2026) began logging moves: without it every capture read Survey (critic G2b). Every row of
     * [stages] is a move whatever its `kind`: S5's `stages[]` is its `kind = 'stage'` query and its
     * stageView sends no kind, so a kind test would drop every move S5 sends (only a name outside the
     * six is left out).
     */
    fun quoteAt(stages: List<Entry>, at: String, current: String? = null, currentAt: String? = null): String {
        val h = ArrayList<Pair<String, Long>>()
        for (e in bySeq(stages)) if (e.value in QuoteStage.STAGES) h += e.value to t0(e.at)
        if (current != null && current.trim() in QuoteStage.STAGES && !currentAt.isNullOrBlank()) h += current.trim() to t0(currentAt)
        val t = t0(at)
        var q = QuoteStage.STAGES[0]
        for ((s, ts) in h.withIndex().sortedWith(compareBy({ it.value.second }, { it.index })).map { it.value }) if (ts <= t) q = s
        return q
    }

    // ---- the project-state store's view of the works ---------------------------------

    /** The works with a recorded state in the album GET's `works[]` (a blank `at` = no state row ever). */
    fun statesOfWorks(works: List<Work>): Map<Int, WorkState> =
        works.filter { isWork(it.id) && it.at.isNotBlank() }.associate { it.id to WorkState(stateName(it.state), it.seq, it.at) }

    /** The works with a state row in [log], by their newest one: the history's own answer, for a fully paged log. */
    fun statesOfLog(log: List<Entry>): Map<Int, WorkState> =
        WORK_IDS.mapNotNull { w -> entryOf(log, w)?.let { w to WorkState(stateName(it.value), it.seq, it.at) } }.toMap()

    /**
     * [states] with this phone's queued state writes laid over them, in queue order, each at
     * [PENDING_SEQ] + its place in [queue] (above any S5 seq), as [provisional] numbers them: a
     * tap shows at once with no signal (the queue half of QM0's line).
     */
    fun withQueued(states: Map<Int, WorkState>, queue: List<Op>): Map<Int, WorkState> {
        val out = LinkedHashMap(states)
        queue.forEachIndexed { i, op -> if (op.kind == KIND_STATE && isWork(op.work)) out[op.work!!] = WorkState(stateName(op.value), PENDING_SEQ + i) }
        return out
    }

    // ---- THE ONE VISIBILITY RULE: every count, chapter, viewer and tile goes through it ----

    /**
     * PM and Designer see everything. A vendor sees nothing at work 0 or unfiled, and within
     * `mine` every staff photo, its own uploads whatever the review, and another vendor's only
     * when accepted. A client sees staff photos at once, a vendor's only when accepted, and
     * never an unfiled one.
     */
    fun canSee(viewer: Viewer, it: Photo): Boolean {
        if (viewer.staff) return true
        val w = it.work
        if (viewer.role == VENDOR) {
            if (w == null || w == QUOTE_WORK || w !in viewer.mine) return false
            if (it.party != PARTY_VENDOR) return true
            if (partyKey(it.vendor) in viewer.suppliers) return true
            return it.review == REVIEW_ACCEPTED
        }
        // a client never sees an unfiled item: null, or a work id that is not one of the sixteen (it files as unfiled)
        if (w == null || (w != QUOTE_WORK && !isWork(w))) return false
        return it.party != PARTY_VENDOR || it.review == REVIEW_ACCEPTED
    }

    fun visible(viewer: Viewer, items: List<Photo>): List<Photo> = items.filter { canSee(viewer, it) }

    /** A vendor's uploads awaiting review -- the PM's "To review: N". */
    fun pending(items: List<Photo>): List<Photo> = items.filter { it.party == PARTY_VENDOR && it.review == REVIEW_PENDING }

    // ---- the chapters, for the person looking --------------------------------------

    /** One quote-stage group of chapter 0. `stage` is S5's name, shown through [Masters.quote]. */
    data class QuoteGroup(val stage: String, val i: Int, val items: List<Photo>, val reached: Boolean, val now: Boolean)

    /** Chapter 0, the quote journey: a group per quote stage, the unfiled items (staff only, by [canSee]). */
    data class QuoteChapter(val groups: List<QuoteGroup>, val unfiled: List<Photo>, val count: Int, val locked: Boolean)

    /** Chapters 1-16, one per work, each a grid in time order with its own state, live request and decided ones. */
    data class Chapter(
        val n: Int,
        val id: Int,
        val state: String,
        val items: List<Photo>,
        val mine: Boolean,
        val locked: Boolean,
        val execLocked: Boolean,
        val inScope: Boolean,
        val cr: Entry?,
        val decided: List<Decided>,
    ) {
        val count: Int get() = items.size
        val last: String? get() = items.lastOrNull()?.at
    }

    /** The album as displayed. `order` is the display order the viewer's ‹ › walks; `open` the works In progress. */
    data class Chapters(
        val quote: QuoteChapter,
        val works: List<Chapter>,
        val total: Int,
        val closed: Boolean,
        val closedAt: String?,
        val order: List<Photo>,
        val execLocked: Boolean,
        val changeRequests: List<ChangeRequest>,
        val open: List<Int>,
    )

    /**
     * The chapters for [viewer]: chapter 0 (six quote groups, plus the unfiled items), then the
     * sixteen works in S5's order. [items] are the album's items -- S5's photos and captures
     * with the phone's own, composed by the caller -- in time order; [log] and [stages] the
     * mirror's rows with any provisional ones ([provisional]), the HISTORY (live and decided change
     * requests); [states] each work's state from the caller's project-state store with the queue
     * laid over it ([withQueued]) -- absent = no recorded state. [quoteStage] / [quoteStageAt] what
     * the phone knows of the quote stage (null = not read). Work chapters lock under [exec]; a
     * vendor's chapter 0 and the works outside `mine` lock too, and a vendor sees no change request
     * outside `mine` (S5 sends none). An item on a work id that is not one of the sixteen is unfiled.
     */
    fun chapters(
        items: List<Photo>, log: List<Entry>, stages: List<Entry>, scope: List<Int>?,
        quoteStage: String?, quoteStageAt: String?, states: Map<Int, WorkState>, viewer: Viewer,
    ): Chapters {
        val all = visible(viewer, items)
        val cur = QuoteStage.indexOf(quoteStage?.trim().orEmpty())
        val ven = viewer.role == VENDOR
        val xlock = !exec(quoteStage, states.isNotEmpty())
        val groups = QuoteStage.STAGES.mapIndexed { i, q ->
            QuoteGroup(q, i, all.filter { it.work == QUOTE_WORK && quoteAt(stages, it.at, quoteStage, quoteStageAt) == q },
                reached = if (cur < 0) i == 0 else i <= cur, now = i == cur)
        }
        val unfiled = all.filter { it.work == null || (it.work != QUOTE_WORK && !isWork(it.work)) }
        val quote = QuoteChapter(groups, unfiled, groups.sumOf { it.items.size } + unfiled.size, locked = ven)
        val works = WORKS.mapIndexed { i, w ->
            val seen = !ven || w.id in viewer.mine
            Chapter(
                n = i + 1, id = w.id, state = stateName(states[w.id]?.state),
                items = all.filter { it.work == w.id },
                mine = ven && w.id in viewer.mine,
                locked = (ven && w.id !in viewer.mine) || xlock,
                execLocked = xlock,
                inScope = inScope(scope, w.id),
                cr = if (seen) crOf(log, w.id) else null,
                decided = if (seen) crDone(log, w.id) else emptyList(),
            )
        }
        val order = groups.flatMap { it.items } + unfiled + works.flatMap { it.items }
        val closed = stateName(states[LAST_WORK]?.state) == DONE
        val closedAt = if (closed) states[LAST_WORK]?.at?.ifBlank { null } else null
        val crs = changeRequests(log).filter { !ven || it.id in viewer.mine }
        return Chapters(quote, works, all.size, closed, closedAt, order, xlock, crs, openWorks(states))
    }

    // ---- the writer gates: null = allowed; a [Refusal] = refused, and why (nothing is written) ----

    /**
     * Setting a work's state (PM only; ct61b D1-D3). [from] is the DERIVED state, which the POST
     * carries as `prev` -- S5 refuses a `prev` that is not what its log says. Refused: not PM,
     * the chapters locked ([exec] false), not a work, not a state, the same state, or leaving Done
     * with no remark.
     */
    fun stateWrite(viewer: Viewer, exec: Boolean, work: Int, from: String, to: String, remark: String): Refusal? = when {
        viewer.role != PM -> Refusal.STATE_PM_ONLY
        !exec -> Refusal.EXEC_LOCKED
        !isWork(work) -> Refusal.NOT_A_WORK
        to !in STATES -> Refusal.NOT_A_STATE
        from == to -> Refusal.SAME_STATE
        remarkNeeded(from) && remark.isBlank() -> Refusal.DONE_REASON_NEED
        else -> null
    }

    /**
     * Raising a change request of [kind] on [work] (PM only, ct61b E8; v61c §13.5, §14.8): only
     * once the client has approved -- before that the scope changes through Revisions and Approve
     * (the prototype's approvedQ, index61c1.html 7258) -- with a baseline (S5: 409 "No approved
     * scope yet"; critic G4) and no live request on the work; an [CrKind.ADD] only for a work off
     * the approved scope, a [CrKind.REMOVE] or [CrKind.CHANGE] only for one in it (S5's SQL, in
     * S5's words).
     */
    fun crRaise(viewer: Viewer, quoteStage: String?, scope: List<Int>?, work: Int, kind: CrKind, log: List<Entry>): Refusal? = when {
        viewer.role != PM -> Refusal.CR_PM_ONLY
        !approved(quoteStage) -> Refusal.CR_NOT_APPROVED
        !isWork(work) -> Refusal.NOT_A_WORK
        scope == null -> Refusal.CR_NO_BASELINE
        crOf(log, work) != null -> Refusal.CR_LIVE
        kind == CrKind.ADD && inScope(scope, work) -> Refusal.CR_IN_SCOPE
        kind == CrKind.REMOVE && !inScope(scope, work) -> Refusal.CR_OFF_SCOPE_REMOVE
        kind == CrKind.CHANGE && !inScope(scope, work) -> Refusal.CR_OFF_SCOPE_CHANGE
        else -> null
    }

    /**
     * Recording the client's answer to the live request on [work] (PM only; the prototype's
     * answerCR): only at Client approved, only on a live request, and for [CrOutcome.APPROVED] and
     * [CrOutcome.DECLINED] with [clientAt] a real day (S5's isDay; its 400 is
     * refuse.album.client_at_day) -- never a silent today. A withdrawal may leave the day out.
     */
    fun crAnswer(viewer: Viewer, quoteStage: String?, work: Int, outcome: CrOutcome, clientAt: String, log: List<Entry>): Refusal? = when {
        viewer.role != PM -> Refusal.CR_PM_ONLY
        !approved(quoteStage) -> Refusal.CR_NOT_APPROVED
        !isWork(work) -> Refusal.NOT_A_WORK
        crOf(log, work) == null -> Refusal.CR_NONE
        outcome.needsDay && !isDay(clientAt) -> Refusal.CLIENT_AT_DAY
        else -> null
    }

    /**
     * Recording a photo: PM or Designer into the quote journey (0) always, into a work only
     * when the chapters are open; a vendor into `mine` only (S5: 403 otherwise; no exec gate on
     * a vendor's upload, as the prototype's `upload`); a client never. The phone never records
     * an unfiled photo -- unfiled is the server's filing of an as-is capture whose stage maps
     * to null -- so [work] is 0 or a work id.
     */
    fun photoRecord(viewer: Viewer, exec: Boolean, work: Int): Refusal? = when (viewer.role) {
        PM, DESIGNER -> when {
            work != QUOTE_WORK && !isWork(work) -> Refusal.NOT_A_WORK
            work != QUOTE_WORK && !exec -> Refusal.EXEC_LOCKED
            else -> null
        }
        VENDOR -> if (work in viewer.mine) null else Refusal.NOT_YOUR_WORK
        else -> Refusal.PHOTO_NO_CLIENT
    }

    /**
     * Accept / Not for client (PM only), in S5's order: a review state, a vendor's row, and its
     * JPEG landed (`bytes` > 0: S5 answers 409 until it has -- a review is of the bytes the PM saw);
     * the same state again is a no-op, so it is refused here.
     */
    fun review(viewer: Viewer, photo: Photo, state: String): Refusal? = when {
        viewer.role != PM -> Refusal.REVIEW_PM_ONLY
        state != REVIEW_ACCEPTED && state != REVIEW_INTERNAL -> Refusal.NOT_A_REVIEW
        photo.party != PARTY_VENDOR -> Refusal.REVIEW_VENDOR_ONLY
        photo.bytes <= 0L -> Refusal.REVIEW_NO_IMAGE
        photo.review == state -> Refusal.REVIEW_SAME
        else -> null
    }

    // ---- the outbox: what the phone queued, and what S5's verdict does to it ----

    /** The op kinds the queue holds: the three log kinds, a photo record (then its JPEG), a review, a photo re-file. */
    const val OP_PHOTO = "photo"
    const val OP_REVIEW = "review"

    /**
     * A staff photo re-filed into one of the sixteen works (S5-P, `PUT .../album/photos/:id/work {work, base}`,
     * live on stage since 2 Nov 2026; port 61c slice AQ). Amit's answer 3 of 28 Oct: a single photo from the
     * Capture 360 sheet after approval goes to the album as a staff photo of its work, and re-filing it after it
     * is sent needs S5's own guarded write -- the record is immutable. Its only caller is FW2's WORK line
     * ([refileOp]).
     */
    const val OP_REFILE = "refile"

    /**
     * One queued write, with its writer-minted id (the replay key, [mintId]: kept by every retry)
     * and the `base` it saw: a log op's `base` is the work's newest `seq` of any kind (0 = none),
     * as text here and an integer on the wire (AQ); a review's is the row's `review_at` ('' =
     * never); a re-file's is the photo's work as last seen, as text ('' = unfiled, null on the
     * wire). `work` is the work the op writes on (a review's is its photo's work, a re-file's the
     * work it files into) and what a 409 drops by -- but for a re-file, which rides on its PHOTO,
     * never on a work's log (dropWork). `error` is S5's own words once it refused or won; blank
     * while pending.
     * [recorded] is a photo op whose record S5 has answered: its JPEG is the next step
     * (AlbumQueue), so a retry never sends the record again for nothing.
     */
    data class Op(
        val id: String,
        val kind: String,
        val work: Int?,
        val base: String = "0",
        val value: String = "",
        val prev: String = "",
        val note: String = "",
        val photo: String = "",
        val room: String? = null,
        val caption: String = "",
        val photoKind: String = PHOTO,
        val clientAt: String = "",
        val error: String = "",
        val recorded: Boolean = false,
    )

    /** S5's ALBUM_ID_RE: ALB- and 12 lower-case hex digits. */
    val ID_RE: Regex = Regex("""^ALB-[0-9a-f]{12}$""")

    private const val HEX = "0123456789abcdef"

    /**
     * A writer-minted album id, `ALB-` and 12 hex digits from [random] (the prototype's mintId): one
     * per op, so a retry of the op keeps its id (S5 replays it as `written: false`) and two ops --
     * two reviews of one photo above all -- never share one (S5 answers 409 "That id already names
     * another album entry").
     */
    fun mintId(random: Random = Random.Default): String {
        val b = random.nextBytes(6)
        val sb = StringBuilder("ALB-")
        for (x in b) { val v = x.toInt() and 0xff; sb.append(HEX[v shr 4]).append(HEX[v and 15]) }
        return sb.toString()
    }

    /** The base a new log op on [work] carries: S5's newest seq of any kind for it, from the mirror ([PENDING_SEQ] rows never count). */
    fun baseOf(mirror: View?, work: Int): Long {
        if (mirror == null) return 0
        val w = mirror.works.firstOrNull { it.id == work }?.seq ?: 0
        val l = mirror.log.filter { it.work == work && it.seq < PENDING_SEQ }.maxOfOrNull { it.seq } ?: 0
        return maxOf(w, l)
    }

    /** [s] as S5 measures it against [max]: trimmed FIRST, then cut (a cut before the trim would lose characters S5 accepts). */
    private fun clip(s: String, max: Int): String = s.trim().take(max)

    /** A state write as its op: [from] is the derived state (sent as `prev`), [remark] the note. */
    fun stateOp(mirror: View?, work: Int, from: String, to: String, remark: String, random: Random = Random.Default): Op =
        Op(mintId(random), KIND_STATE, work, base = baseOf(mirror, work).toString(), value = to, prev = from, note = clip(remark, MAX_NOTE))

    /** A change request raised: its kind on `value`, the note the client reads. */
    fun crRaiseOp(mirror: View?, work: Int, kind: CrKind, note: String, random: Random = Random.Default): Op =
        Op(mintId(random), KIND_CR_RAISE, work, base = baseOf(mirror, work).toString(), value = kind.wire, note = clip(note, MAX_NOTE))

    /**
     * The client's answer recorded: the outcome on `value`, the day the client answered on
     * `client_at` (blank for a withdrawal that names none), how on the note; `prev` carries the
     * kind it answers, as S5's SQL writes it, so the provisional row reads whole ([crDone]).
     */
    fun crAnswerOp(mirror: View?, log: List<Entry>, work: Int, outcome: CrOutcome, clientAt: String, how: String, random: Random = Random.Default): Op =
        Op(mintId(random), KIND_CR_CLEAR, work, base = baseOf(mirror, work).toString(), value = outcome.wire,
            prev = crOf(log, work)?.crKind?.wire ?: CrKind.ADD.wire, note = clip(how, MAX_NOTE), clientAt = clientAt.trim())

    /** A review of [photo]: a fresh id every time (the log row's), the base its `review_at` as seen. */
    fun reviewOp(photo: Photo, state: String, random: Random = Random.Default): Op =
        Op(mintId(random), OP_REVIEW, photo.work, base = photo.reviewAt ?: "", value = state, photo = photo.id)

    /**
     * A photo record (its JPEG follows): the record's id is the photo's. The caption and the room are
     * held to S5's caps ([MAX_CAPTION], [MAX_ROOM]) as the notes are: S5's 400 over a cap would drop
     * the record for good, and its JPEG with it. A blank room is none, as S5 stores it.
     */
    fun photoOp(work: Int, room: String?, caption: String, kind: String = PHOTO, random: Random = Random.Default): Op {
        val id = mintId(random)
        return Op(id, OP_PHOTO, work, base = "", photo = id, room = room?.let { clip(it, MAX_ROOM) }?.ifEmpty { null },
            caption = clip(caption, MAX_CAPTION), photoKind = kind)
    }

    /**
     * A staff photo re-filed [to] one of the sixteen works (S5-P): `base` is [photo]'s work as the caller last saw
     * it -- S5 compares and sets on it in ONE UPDATE -- as text, '' for unfiled. The op's own id is the queue's key
     * only (S5's body is `{work, base}`). S5 refuses for good what no retry can change (403: not staff, not the
     * uploading login, a vendor's photo) and answers 409 for a race (re-filed meanwhile) or the lock (Work starts
     * once the client approves the estimate), each with the photo as stored, and 404 for a photo it has no row for
     * (AlbumQueue refuses that for good when the mirror does not hold the photo); the screen's gate is FW2's.
     * A re-file writes no log row and moves no works[].seq (S5-P), so its 409 drops it and the later re-files of
     * the same photo, never a work's ops, and no work's 409 drops it ([dropWork]).
     */
    fun refileOp(photo: Photo, to: Int, random: Random = Random.Default): Op =
        Op(mintId(random), OP_REFILE, to, base = photo.work?.toString() ?: "", photo = photo.id)

    /**
     * The phone's copy of one project's album: the [mirror] (S5's last GET; null until the
     * first 200), the FIFO [queue] of ops still to send, and the ops S5 [dropped] -- each with
     * S5's `error` on it, for the queue row -- which are never sent again. The caller clears
     * `dropped` when the person has seen them.
     */
    data class Outbox(val mirror: View? = null, val queue: List<Op> = emptyList(), val dropped: List<Op> = emptyList())

    /**
     * A review answer's photo fields, as S5 sends them FLAT beside the photo's id (asis.js, the review
     * PUT's 200, its replay and its 409s: `{photo: <id>, review, review_by, review_by_name, review_at}`)
     * -- never a photo row. Blank = none, as S5's reviewOf writes it.
     */
    data class ReviewPatch(val photo: String, val review: String, val reviewBy: String = "", val reviewByName: String = "", val reviewAt: String = "")

    /** A JPEG answer (`{ok, written, bytes}`, no id: the photo is the op's) as the bytes now landed on that photo. */
    data class BytesPatch(val photo: String, val bytes: Long)

    /**
     * What S5's answer to one op carried, typed: the row(s) the mirror takes, each in the shape S5
     * sends. [photo] is a WHOLE row (the photo record PUT's `photo`); [review] and [bytes] patch the
     * mirror's own row by id and never stand in for one. Absent pieces leave the mirror as it was;
     * [scopeAt] non-null means the answer carried the scope (every log POST answer does, asis.js:
     * an approved add or remove has just moved it), [scope] null then being "no baseline".
     */
    data class Answer(
        val entry: Entry? = null,
        val work: Work? = null,
        val photo: Photo? = null,
        val scope: List<Int>? = null,
        val scopeAt: String? = null,
        val review: ReviewPatch? = null,
        val bytes: BytesPatch? = null,
    )

    /** The verdict table of "Offline and conflicts". */
    enum class Verdict { POSTED, REPLAY, CONFLICT, LOCKED, REFUSED, RELOGIN, STOP }

    /** Refused for good -- sending the same op again can only be refused again: 400, 403, 413, 415. */
    fun refusedForGood(code: Int): Boolean = code == 400 || code == 403 || code == 413 || code == 415

    /**
     * The verdict for an HTTP answer: 200 is [Verdict.POSTED], or [Verdict.REPLAY] when the
     * body says `written: false`; 409 is [Verdict.CONFLICT] (S5 wins), or [Verdict.LOCKED] when
     * the body says `locked: true`; [refusedForGood] codes are [Verdict.REFUSED]; 401 is
     * [Verdict.RELOGIN] (the key is fixed, then the op goes); everything else -- 404, 405, 5xx,
     * no network (code <= 0) -- is [Verdict.STOP]: the drain ends and the queue waits for the
     * next pass.
     */
    fun verdict(code: Int, written: Boolean = true, locked: Boolean = false): Verdict = when {
        code == 200 -> if (written) Verdict.POSTED else Verdict.REPLAY
        code == 409 -> if (locked) Verdict.LOCKED else Verdict.CONFLICT
        refusedForGood(code) -> Verdict.REFUSED
        code == 401 -> Verdict.RELOGIN
        else -> Verdict.STOP
    }

    /** The sentinel seq of a provisional (not yet posted) log row: above any S5 seq, in queue order. */
    const val PENDING_SEQ: Long = 1L shl 62

    /**
     * A queued log op as the row it will become, so the screen composes mirror + outbox and
     * a tap shows at once with no signal: seq [PENDING_SEQ] + its place in the queue, the
     * person's whoami identity, S5's `at` unknown so the device clock stands in.
     */
    fun provisional(op: Op, index: Int, role: String, by: String, byName: String, at: String): Entry =
        Entry(seq = PENDING_SEQ + index, id = op.id, kind = op.kind, work = op.work, value = op.value, prev = op.prev,
            note = op.note, role = role, by = by, byName = byName, at = at, photo = op.photo.ifBlank { null },
            clientAt = op.clientAt.ifBlank { null })

    /** A queued photo op as the item it will become (bytes 0: the JPEG is in `shots/<id>.jpg`, not on S5 yet). */
    fun provisionalPhoto(op: Op, role: String, by: String, byName: String, at: String, vendor: String? = null): Photo =
        Photo(id = op.id, work = op.work, at = at, party = if (role == VENDOR) PARTY_VENDOR else PARTY_STAFF, room = op.room,
            kind = op.photoKind, caption = op.caption, vendor = vendor, role = role, by = by, byName = byName,
            clientAt = op.clientAt.ifBlank { null })

    /** Two log pages as one: every row once by id (the later page's copy wins), in seq order. */
    fun mergedLog(a: List<Entry>, b: List<Entry>): List<Entry> {
        val byId = LinkedHashMap<String, Entry>()
        for (e in a) byId[e.id] = e
        for (e in b) byId[e.id] = e
        return bySeq(byId.values.toList())
    }

    /**
     * The mirror after one more `?since=<logNext>` page: everything else as the newer answer [page]
     * says, the log the union of both ([mergedLog]). The caller asks again while `page.logMore`.
     */
    fun paged(mirror: View, page: View): View = page.copy(log = mergedLog(mirror.log, page.log))

    /**
     * The mirror with S5's row(s) taken: an entry appended once by id (log order by seq), a work row
     * replaced, a whole photo row replaced or added, a review or a bytes patch applied to the photo
     * row of that id (a row the mirror does not hold is left to the next GET: a patch is not a row),
     * the scope when sent.
     */
    fun merged(mirror: View?, a: Answer): View? {
        if (mirror == null) return null
        var m: View = mirror
        a.entry?.let { e -> m = m.copy(log = mergedLog(m.log, listOf(e))) }
        a.work?.let { w ->
            val rows = if (m.works.any { it.id == w.id }) m.works.map { if (it.id == w.id) w else it } else m.works + w
            m = m.copy(works = rows.sortedBy { WORK_IDS.indexOf(it.id).let { i -> if (i < 0) Int.MAX_VALUE else i } })
        }
        a.photo?.let { p ->
            val rows = if (m.photos.any { it.id == p.id }) m.photos.map { if (it.id == p.id) p else it } else m.photos + p
            m = m.copy(photos = rows)
        }
        a.review?.let { r ->
            m = m.copy(photos = m.photos.map {
                if (it.id != r.photo) it
                else it.copy(review = r.review, reviewBy = r.reviewBy.ifBlank { null }, reviewByName = r.reviewByName.ifBlank { null }, reviewAt = r.reviewAt.ifBlank { null })
            })
        }
        a.bytes?.let { b -> m = m.copy(photos = m.photos.map { if (it.id == b.photo) it.copy(bytes = b.bytes) else it }) }
        a.scopeAt?.let { m = m.copy(scope = a.scope, scopeAt = it) }
        return m
    }

    private fun without(queue: List<Op>, id: String): List<Op> = queue.filter { it.id != id }

    /**
     * The queue after [done] leaves it, with the ops that next ride on it given what S5 now has.
     * The next LOG op on the same work, of any kind, takes [seq] (the base is the work's newest seq
     * of ANY kind, so a review's row moves it too); the next STATE op on the same work takes [prev]
     * when given -- looked up on its own, because a change request between two state writes carries
     * a base but no state (its `prev` is the kind it answers); the next op of [done]'s own kind on
     * the same photo -- a review after a review, a re-file after a re-file -- takes [photoBase]
     * (the `review_at`, or the photo's work as text).
     */
    private fun rebased(queue: List<Op>, done: Op, seq: Long?, prev: String?, photoBase: String?): List<Op> {
        val i = queue.indexOfFirst { it.id == done.id }
        if (i < 0) return queue
        val rest = queue.filterIndexed { j, _ -> j != i }.toMutableList()
        val after = queue.subList(i + 1, queue.size).map { it.id }.toSet()
        if (seq != null) {
            val k = rest.indexOfFirst { it.id in after && it.kind in LOG_KINDS && it.work == done.work }
            if (k >= 0) rest[k] = rest[k].copy(base = seq.toString())
        }
        if (prev != null) {
            val k = rest.indexOfFirst { it.id in after && it.kind == KIND_STATE && it.work == done.work }
            if (k >= 0) rest[k] = rest[k].copy(prev = prev)
        }
        if (photoBase != null && (done.kind == OP_REVIEW || done.kind == OP_REFILE)) {
            val k = rest.indexOfFirst { it.id in after && it.kind == done.kind && it.photo == done.photo }
            if (k >= 0) rest[k] = rest[k].copy(base = photoBase)
        }
        return rest
    }

    /**
     * 200 `written: true`: the op leaves the queue and the mirror takes what S5 sent back; the
     * next queued op on the same work rides on S5's new row -- its base becomes the answer
     * entry's seq (a review's answer carries its log row too), and the next review of the same
     * photo takes the `review_at` of the answer's [ReviewPatch] for THAT photo -- so an offline
     * chain of writes on one work is not answered "Changed meanwhile" by its own first link. A
     * re-file answers the whole photo row, so the next re-file of the same photo takes that row's
     * work as its base (a re-file writes no log row and moves no seq). The rebase reads the answer
     * alone, so it holds with no mirror. A null mirror stays null -- a fragment cannot stand in for
     * a GET, and K2 GETs after a drain.
     */
    fun afterPost(o: Outbox, op: Op, a: Answer): Outbox {
        val photoBase = when (op.kind) {
            OP_REVIEW -> if (a.review != null && a.review.photo == op.photo) a.review.reviewAt else null
            OP_REFILE -> a.photo?.takeIf { it.id == op.photo }?.let { it.work?.toString() ?: "" }
            else -> null
        }
        val q = if (o.queue.any { it.id == op.id }) rebased(o.queue, op, a.entry?.seq, null, photoBase) else without(o.queue, op.id)
        return o.copy(mirror = merged(o.mirror, a), queue = q)
    }

    /**
     * 200 `written: false`: a replay of an id S5 already holds (the first answer was lost) --
     * harmless. The op leaves the queue; what S5 sends back is the same row, merged and rebased on
     * as a 200.
     */
    fun afterReplay(o: Outbox, op: Op, a: Answer): Outbox = afterPost(o, op, a)

    /** [leaving] (queue copies) moved from the queue to `dropped`, each marked with [error]; the rest stay, in order. */
    private fun gone(o: Outbox, leaving: List<Op>, error: String): Outbox {
        val ids = leaving.map { it.id }.toSet()
        return o.copy(queue = o.queue.filter { it.id !in ids }, dropped = o.dropped + o.queue.filter { it.id in ids }.map { it.copy(error = error) })
    }

    /**
     * [leaving] and, for every photo op among them, the LATER queued re-files and reviews of its photo (port 61c AQ
     * review). A photo op that leaves the queue unsent leaves S5 with no row for its photo -- or none with bytes, which
     * no list ever shows -- and S5 answers a re-file or a review of a photo it has no row for 404 on every pass
     * (asis.js, the row lookup), which would stop the drain at the head of the queue for good. So they go with it.
     */
    private fun withDependants(queue: List<Op>, leaving: List<Op>): List<Op> {
        val ids = leaving.map { it.id }.toSet()
        val photos = queue.withIndex().filter { (_, x) -> x.id in ids && x.kind == OP_PHOTO }
        return queue.filterIndexed { j, x ->
            x.id in ids || ((x.kind == OP_REFILE || x.kind == OP_REVIEW) && photos.any { (i, p) -> j > i && x.photo == p.photo })
        }
    }

    /**
     * The op and every LATER queued op that rode on it, marked with [error], moved from the queue to `dropped`; the
     * rest stay, in order. What rode on it is what S5's 409 is about. For any op but a re-file, that is the later ops
     * on the same work, re-files excepted: a re-file rides on no work's log (S5-P moves no works[].seq, so a work's
     * 409 says nothing about it). For a re-file it is the later re-files of the same PHOTO, whose base is where this
     * one would have put it -- never the target work's ops, which S5 still takes. A photo op dropped this way takes
     * its photo's later re-files and reviews with it ([withDependants]).
     */
    private fun dropWork(o: Outbox, op: Op, error: String): Outbox {
        val i = o.queue.indexOfFirst { it.id == op.id }
        if (i < 0) return o
        val rode = o.queue.filterIndexed { j, x ->
            j == i || (j > i && if (op.kind == OP_REFILE) x.kind == OP_REFILE && x.photo == op.photo else x.kind != OP_REFILE && x.work == op.work)
        }
        return gone(o, withDependants(o.queue, rode), error)
    }

    /**
     * 409: S5 WINS. The op and every later queued op on the same work are dropped (each row
     * showing S5's [error]) -- for a re-file, the later re-files of the same photo instead, and
     * no work's 409 drops a re-file ([dropWork]); the mirror takes the current state the 409 body
     * echoes ([a]).
     */
    fun afterConflict(o: Outbox, op: Op, error: String, a: Answer): Outbox = dropWork(o, op, error).let { it.copy(mirror = merged(it.mirror, a)) }

    /**
     * 409 `locked: true`: S5, the authority, answered the lock banner -- the quote stage there
     * is not Client approved and no work is recorded, whatever the phone believed (an unread
     * stage). The op and every later queued op on the same work are dropped, each row saying
     * so in S5's words -- a re-file by its photo, as on [afterConflict]; the lock is the
     * project's, so every op left behind meets its own locked 409 and leaves the same way. The
     * mirror takes what the body echoes, as on [afterConflict]: the log
     * POST's locked 409 carries the work row, its newest entry and the scope (asis.js `...cur`);
     * the photo record's carries nothing, so [a] is empty there and the mirror stays as it was.
     * The next GET (or the quote-stage refresh) tells the phone the stage.
     */
    fun afterLocked(o: Outbox, op: Op, error: String, a: Answer = Answer()): Outbox =
        dropWork(o, op, error).let { it.copy(mirror = merged(it.mirror, a)) }

    /**
     * 400 / 403 / 413 / 415: refused for good -- dropped with one note (S5's [error]); nothing
     * else moves on S5, so ops after it, on the same work or not, still go -- and the next one
     * that rode on it takes back what S5 still has: the next log op on the same work, of any
     * kind, inherits the refused log op's base, and a refused STATE write's `prev` goes to the next
     * state op on the work, past any change request queued between them (S5 still holds the state
     * the refused op left); the next review of the same photo inherits the refused review's base,
     * and the next re-file of the same photo the refused re-file's (S5 still files it there).
     * A refused photo op takes its photo's later re-files and reviews with it, in the same words
     * ([withDependants]: S5 has no row for them to act on). The mirror is untouched (AlbumQueue
     * takes the photo a re-file's 403 echoes).
     */
    fun afterRefused(o: Outbox, op: Op, error: String): Outbox {
        if (o.queue.none { it.id == op.id }) return o
        if (op.kind == OP_PHOTO) return gone(o, withDependants(o.queue, o.queue.filter { it.id == op.id }), error)
        val q = when {
            op.kind in LOG_KINDS -> rebased(o.queue, op, op.base.toLongOrNull() ?: 0, if (op.kind == KIND_STATE) op.prev else null, null)
            op.kind == OP_REVIEW || op.kind == OP_REFILE -> rebased(o.queue, op, null, null, op.base)
            else -> without(o.queue, op.id)
        }
        return o.copy(queue = q, dropped = o.dropped + op.copy(error = error))
    }
}
