package com.malletcrafts.interiorestimate.pano

/**
 * THE ALBUM BY WORKS, ON THE PHONE -- the pure rules (S2 APK v61b slice 2, Kotlin slice K1).
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
 * The spec is prototype v61b (window.ALB60, index61b.html 6870-7445), which Amit approved on
 * 26 Oct 2026; the store is S5 QUOTE (DESIGN-slice2.md: one append-only log, one photo table,
 * three project columns, one access column, `GET .../album`). Nothing here talks to a server
 * and nothing here is Android: this is the vocabulary, the derivations, the writer gates and
 * the transitions of what the phone keeps -- the mirror of S5's last GET and the FIFO outbox --
 * under the verdict table of "Offline and conflicts". AlbumTest pins all of it against the
 * prototype's checks (ct61b D1-D3, E7, E8).
 *
 * Current state is always DERIVED from the log ([stateOf], [crOf], [closed]); there is no
 * cached state that can drift. Order is by `seq`, never by time. Identity is whoami's `user`
 * / `full_name`, never the prototype's ME.
 */
object Album {

    // ---- vocabulary ---------------------------------------------------------

    /** One of the sixteen works: S5's id and its name (strings-en.json `work.*`). */
    data class WorkDef(val id: Int, val name: String)

    /**
     * S5 QUOTE's sixteen payment work groups, app1_data.js PHASES (@ ca74117), ids and names
     * verbatim and in S5's order; the prototype's WORKS. Chapter n = index + 1. Change here
     * only with S5's WORKS in asis.js.
     */
    val WORKS: List<WorkDef> = listOf(
        WorkDef(10, "Demolition & site preparation"),
        WorkDef(20, "Civil & masonry"),
        WorkDef(30, "Plumbing, concealed"),
        WorkDef(40, "Electrical, concealed wiring"),
        WorkDef(50, "Waterproofing"),
        WorkDef(60, "False ceiling"),
        WorkDef(70, "Flooring & wall tiling"),
        WorkDef(80, "Countertops"),
        WorkDef(85, "Iron & metal fabrication"),
        WorkDef(90, "Modular furniture, factory-made"),
        WorkDef(95, "Onsite carpentry & panelling"),
        WorkDef(100, "Painting & wall finishes"),
        WorkDef(110, "Sanitaryware & CP fittings"),
        WorkDef(120, "Lights, fans, fixtures & security"),
        WorkDef(130, "Glass, mirrors & furnishing"),
        WorkDef(140, "Cleaning & handover"),
    )

    val WORK_IDS: List<Int> = WORKS.map { it.id }

    /** The quote journey, chapter 0: a photo's `work` of 0. */
    const val QUOTE_WORK = 0

    /** The last work; the album closes for display when it is Done ([closed]). */
    const val LAST_WORK = 140

    /** The five states, in the prototype's order (v61a: On hold and Abandoned joined the three). */
    val STATES: List<String> = listOf("Not started", "In progress", "On hold", "Done", "Abandoned")
    const val NOT_STARTED = "Not started"
    const val IN_PROGRESS = "In progress"
    const val ON_HOLD = "On hold"
    const val DONE = "Done"
    const val ABANDONED = "Abandoned"

    /** The four roles as S5 stores them on every row (`role` column). */
    const val PM = "pm"
    const val DESIGNER = "designer"
    const val VENDOR = "vendor"
    const val CLIENT = "client"
    val ROLE_IDS: List<String> = listOf(PM, DESIGNER, VENDOR, CLIENT)

    /** One role: its id, its label and the line under it, as the prototype's ROLES (the demo names are not ported). */
    data class RoleDef(val id: String, val label: String, val line: String)

    /**
     * The prototype's ROLES, verbatim: `role.project_manager` and `role.designer` are keyed in
     * strings-en.json; "Vendor" and "Client" are the prototype's labels for the other two.
     */
    val ROLES: List<RoleDef> = listOf(
        RoleDef(PM, "Project Manager", "sees everything · sets work states · accepts vendor photos"),
        RoleDef(DESIGNER, "Designer", "sees everything · takes progress shots · changes no state"),
        RoleDef(VENDOR, "Vendor", "sees only their own work · uploads photos for review"),
        RoleDef(CLIENT, "Client", "sees accepted photos and work states only"),
    )

    /** The log row kinds (`asis_album_log.kind`). */
    const val KIND_STATE = "state"
    const val KIND_CR_RAISE = "cr_raise"
    const val KIND_CR_CLEAR = "cr_clear"
    const val KIND_REVIEW = "review"
    const val KIND_STAGE = "stage"

    /** The photo parties and reviews (`asis_album_photos.party` / `.review`). */
    const val PARTY_STAFF = "staff"
    const val PARTY_VENDOR = "vendor"
    const val REVIEW_PENDING = "pending"
    const val REVIEW_ACCEPTED = "accepted"
    const val REVIEW_INTERNAL = "internal"
    const val PHOTO = "photo"
    const val PHOTO_360 = "360"

    /** S5's cap on a remark or a CR note (`note` <= 500). */
    const val MAX_NOTE = 500

    /** One payment milestone of a work: before (`s`) or when done (`e`), and the work it keys off. */
    data class Pay(val kind: Char, val at: Int)

    /**
     * S5's PAY_TERMS (app1_data.js) as the prototype's PAY: each work's distinct (kind, at)
     * pairs, kind 's' = "Before <at> starts", 'e' = "When <at> is done" (paySchedule's labelling
     * rule). No amounts, no percentages -- the evidence a work's photos are for, not its money.
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
     * does them; 0 = the quote journey; null = not filed to a work (a delivery photo belongs to
     * the work being delivered, and only a human can say which). S5 files an as-is capture
     * through the same table on the server; the phone files its own captures here.
     */
    val STAGE_WORK: Map<String, Int?> = mapOf(
        "As-built site measure" to 0, "Existing condition — builder handover" to 0, "Defect recorded" to 0, "Opening measured" to 0,
        "Demolition & debris removal" to 10, "Masonry & block work" to 20, "Plaster & waterproofing" to 20, "Door & window frames (chowkat)" to 20,
        "Electrical first fix — conduit & wiring" to 40, "Plumbing first fix — concealed lines" to 30, "HVAC / AC piping & drain" to 40, "Data, network & CCTV conduit" to 40,
        "False ceiling framing (GI grid)" to 60, "POP / gypsum board & finish" to 60, "Cove & profile-light recesses" to 60,
        "Floor tiling" to 70, "Wall tiling / dado" to 70, "Wooden, vinyl or laminate flooring" to 70, "Stone & marble polish" to 70,
        "Material delivered on site" to null, "Modular units delivered" to 90,
        "Wall panelling" to 95, "Wall moulding & trims" to 95, "Modular carpentry install" to 90, "Loose furniture" to 130,
        "Electrical second fix — switches & points" to 40, "Light installation" to 120, "Plumbing second fix — CP & sanitary" to 110,
        "Window jali, mesh & grill" to 85, "Doors, shutters & hardware" to 95,
        "Putty & primer" to 100, "Paint, wallpaper or texture" to 100, "Glass & mirrors" to 130, "Curtains & blinds" to 130, "Deep clean" to 140,
        "Snagging" to 140, "Snag rectification" to 140, "Rectified & verified" to 140, "Handover" to 140,
    )

    // ---- the strings: every `album.*`, `work.*` and `role.*` key of strings-en.json, verbatim ----
    // (the work names ride in WORKS, the two role names in ROLES). English at the call site;
    // the Marathi values come under task #19. Names, captions, remarks, CR notes and dates are
    // DATA, never strings. The camera's stale "locked until the quote is Estimated"
    // (app.stg_locked_quote) is NOT ported: the lock fires at Client approved (v61b).

    const val ACCEPT = "Accept"
    const val ACCEPTED_BY = "Accepted by"
    const val ACCEPTED_RES = "Accepted · the client sees it now"
    const val AFTER = "After"
    const val ALBUM = "Album"
    const val AWAITING_REVIEW = "awaiting review"
    const val BEFORE = "Before"
    const val CAPTION_OPTIONAL = "Caption (optional)"
    const val CHANGE_REQUEST = "Change request"
    const val CLOSED_CLEANING = "Album closed · Cleaning & handover is Done"
    const val CLOSES_CLEANING = "The album closes when Cleaning & handover is Done"
    const val CR_CLEAR = "Clear change request"
    const val CR_RAISE = "Raise change request"
    const val DONE_REASON_LABEL = "Why change a Done work? (required)"
    const val DONE_REASON_NEED = "A reason is needed to change a Done work"
    const val EMPTY = "No photos yet — the survey's 360s come first"
    /** The lock banner over chapter 1 while the work chapters are locked (ct61b D1): the lock fires at Client approved. */
    const val EXEC_LOCK_BANNER = "Work starts once the client approves the estimate"
    const val KEPT_RES = "Kept from the client"
    const val NO_CAPTION = "No caption"
    const val NO_OPEN_WORK = "No work open yet"
    const val NO_PHOTOS = "no photos"
    const val NO_PHOTOS_YET = "no photos yet"
    const val NO_WORK_LOG = "No work state recorded yet"
    const val NOT_FOR_CLIENT = "Not for client"
    const val NOT_REACHED = "not reached yet"
    const val NOT_YOUR_WORK = "not your work"
    const val NOW = "now"
    const val PICK_WORK = "Pick the work"
    const val PROGRESS_SHOT = "Progress shot"
    const val QUOTE_JOURNEY = "Quote journey"
    const val QUOTE_JOURNEY_SUB = "the as-is survey, before any work"
    const val ROLE_CLIENT_LINE = "sees accepted photos and work states only"
    const val ROLE_DESIGNER_LINE = "sees everything · takes progress shots · changes no state"
    const val ROLE_PM_LINE = "sees everything · sets work states · accepts vendor photos"
    const val ROLE_VENDOR_LINE = "sees only their own work · uploads photos for review"
    const val ROOM_CAPS = "ROOM"
    const val SEE_AFTER = "See the after photo"
    const val SEE_BEFORE = "See the before photo"
    const val ST_ABANDONED = "Abandoned"
    const val ST_IN_PROGRESS = "In progress"
    const val ST_NOT_STARTED = "Not started"
    const val ST_ON_HOLD = "On hold"
    const val STAFF = "Staff"
    const val STATE_PM_ONLY = "Only the Project Manager sets a work's state"
    const val TAP_TO_CHANGE = "tap to change"
    const val UNFILED = "not filed to a work"
    const val UPLOAD_PHOTO = "Upload a photo"
    const val VENDOR_NOTE = "Your photos show to the client once the Project Manager accepts them"
    const val VIEWING_AS_TITLE = "Viewing as — prototype test"
    const val WHICH_ROOM = "Which room?"
    const val WHICH_WORK = "Which work?"
    const val WORK_CAPS = "WORK"
    const val ARIA_NEXT_PHOTO = "Next photo"
    const val ARIA_PREVIOUS_PHOTO = "Previous photo"
    const val ARIA_TAKE_PHOTO = "Take the photo"
    const val ARIA_TAKE_PROGRESS = "Take a progress shot"
    const val ARIA_NUDGE = "Suggestion from the latest photos"

    // Keyed under album.* from v59 (the 39-stage pointer, its stage log and the nudge), which the
    // v60 album no longer draws (SPEC60 §0.3). Ported because they are keyed; nothing here reads them.
    const val CLOSED_HANDOVER = "Album closed at Handover"
    const val CLOSES_HANDOVER = "The album closes at Handover"
    const val MOVE_PROJECT = "Move the project"
    const val MOVED_BY = "moved by"
    const val NO_MOVES_YET = "No stage moves recorded yet"
    const val SITE_IS_AT = "SITE IS AT"
    const val STAGE_CAPS = "STAGE"
    const val VIA_NUDGE = "from the album's latest photos"
    const val VIA_SHEET = "picked from the stage list"
    const val WORK_STAGE_MOVES = "the work stage moves the project once the quote is Estimated"

    // The template keys, each with its placeholders filled; the text is the key's `en` verbatim.
    fun ariaChapterX(n: Int, name: String): String = "Chapter $n: $name"
    fun ariaCrX(name: String): String = "Change request: $name"
    fun ariaPaymentX(work: String, list: String): String = "Payment milestones of $work: $list"
    fun ariaStateX(work: String, state: String): String = "State of $work: $state"
    fun ariaTileX(title: String, pl: String): String = "Album of $title: $pl, survey to handover"
    fun ariaUploadX(work: String): String = "Upload a photo to $work"
    fun changeRequestsX(n: Int): String = "Change requests: $n"
    fun crRaisedX(day: String): String = "Change request · raised $day by"
    fun filedX(stage: String): String = "Progress shot filed at $stage"
    fun lastPhotoX(day: String): String = "last photo $day"
    fun movedX(stage: String): String = "Moved to $stage · in the stage log"
    fun nudgeX(stage: String): String = "Latest photos are $stage — move the project?"
    fun openX(works: String): String = "Open: $works"
    fun payBeforeX(work: String): String = "Before $work starts"
    fun payWhenX(work: String): String = "When $work is done"
    fun paymentX(list: String): String = "Payment: $list"
    fun photoXOfX(i: Int, n: Int): String = "Photo $i of $n"
    fun stageLogX(n: Int): String = "Stage log ($n)"
    fun stateOfX(work: String): String = "State of $work"
    fun subX(pl: String): String = "$pl · survey to handover"
    fun titleX(title: String): String = "Album · $title"
    fun toReviewX(n: Int): String = "To review: $n"
    fun uploadedToX(work: String): String = "Uploaded to $work"
    fun viewingAsX(role: String): String = "Viewing as $role"
    fun workLogX(n: Int): String = "Work log ($n)"

    // The refusals the prototype has no words for -- it hides the control instead (the state
    // sheet marks the current state, Raise shows only off a baseline with no live CR, the
    // vendor's Upload only in `mine`), and its writers return null in silence. A gate here has
    // to say WHY it refused, so these are the phone's own plain lines, kept short; on the queue
    // row S5's own `error` replaces them. Not in strings-en.json, so not translated until added.
    const val NOT_A_WORK = "Not one of the sixteen works"
    const val NOT_A_STATE = "Not one of the five states"
    const val SAME_STATE = "Already in that state"
    const val CR_NO_BASELINE = "No approved scope recorded yet"
    const val CR_IN_SCOPE = "In the approved scope — nothing to request"
    const val CR_LIVE = "A change request is already raised"
    const val CR_NONE = "No change request to clear"
    const val CR_PM_ONLY = "Only the Project Manager raises or clears a change request"
    const val PHOTO_NO_CLIENT = "A client adds no photos"
    const val REVIEW_PM_ONLY = "Only the Project Manager reviews a vendor's photo"
    const val REVIEW_VENDOR_ONLY = "Only a vendor's photo is reviewed"
    const val REVIEW_SAME = "Already reviewed that way"
    const val NOT_A_REVIEW = "Not a review state"

    // ---- the model, shaped like S5's GET .../album answer --------------------

    /** One row of the append-only log (`log[]` and `stages[]`). `work` null on stage rows; `photo` on review rows. */
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
    )

    /** One of `works[16]`: S5's derived row for a work. `cr` is the live cr_raise row, if any. */
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
    )

    /**
     * One album item: a photo row of `photos[]`, or an as-is capture (`captures[]`, or the
     * phone's own CaptureStore row filed through [workOfStage]) as the same shape with `kind`
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
     * S5's `GET .../album` answer -- the phone's MIRROR of the project's album. `quoteStage`
     * null when S5 sent none; `scope` null = no baseline; `pm` null = every staff member is PM.
     * `toReview`, `changeRequests`, `closed` and `closedAt` are S5's figures AS SENT, for a
     * list; the screen derives its own from `log` and `photos` through [chapters], so a merge
     * after a POST never leaves a count behind.
     */
    data class View(
        val role: String,
        val mine: List<Int> = emptyList(),
        val exec: Boolean = false,
        val scope: List<Int>? = null,
        val scopeAt: String = "",
        val pm: String? = null,
        val quoteStage: String? = null,
        val works: List<Work> = emptyList(),
        val log: List<Entry> = emptyList(),
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

    /** The name of a work; 0 reads "Quote journey"; null "not filed to a work"; an unknown id "". */
    fun workName(id: Int?): String = when {
        id == null -> UNFILED
        id == QUOTE_WORK -> QUOTE_JOURNEY
        else -> WORKS.firstOrNull { it.id == id }?.name ?: ""
    }

    /** A state as the five name it; anything else reads "Not started". */
    fun stateName(state: String?): String = if (state != null && state in STATES) state else NOT_STARTED

    /** A work's payment milestones in words: "Before X starts" / "When X is done". */
    fun payOf(id: Int): List<String> =
        (PAY[id] ?: emptyList()).map { if (it.kind == 's') payBeforeX(workName(it.at)) else payWhenX(workName(it.at)) }

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

    /** S5's partyKey: trimmed and lower-cased, so a login or a Supplier is matched as ERP matches it. */
    fun partyKey(v: String?): String = v?.trim()?.lowercase() ?: ""

    // ---- who the person is in this album (D3: pm null means every staff member is PM) ----

    /**
     * The role whoami's `kind` gives in this project, with the vendor's works. Staff is `pm`
     * when the project's `pm` is null (no PM named yet, decision 3) or is this login (compared
     * as [partyKey]), else `designer`; a vendor is `vendor` with `mine` the union of `works` over
     * the Supplier rows of its suppliers ([worksBySupplier] keyed as spelt on the access list);
     * a client is `client`. Any other kind (S5's `none`) is null: maySee refused it already.
     */
    fun albumRole(
        kind: String?, pmLogin: String?, userLogin: String?, suppliers: Collection<String>,
        worksBySupplier: Map<String, List<Int>>,
    ): Viewer? = when (kind) {
        "staff" -> Viewer(if (pmLogin.isNullOrBlank() || partyKey(pmLogin) == partyKey(userLogin)) PM else DESIGNER)
        VENDOR -> {
            val keys = suppliers.map { partyKey(it) }.filter { it.isNotEmpty() }.toSet()
            val mine = worksBySupplier.filterKeys { partyKey(it) in keys }.values.flatten().filter { isWork(it) }.toSet()
            Viewer(VENDOR, mine, keys)
        }
        CLIENT -> Viewer(CLIENT)
        else -> null
    }

    // ---- the derivations: always from the log, by seq ---------------------------

    private fun bySeq(log: List<Entry>): List<Entry> = log.sortedBy { it.seq }

    /** The newest log row of [kinds] on [work], by seq. */
    fun newest(log: List<Entry>, work: Int, vararg kinds: String): Entry? =
        bySeq(log).lastOrNull { it.work == work && it.kind in kinds }

    /** The newest `state` row of a work, or null when none was ever recorded. */
    fun entryOf(log: List<Entry>, work: Int): Entry? = newest(log, work, KIND_STATE)

    /** A work's state: its newest `state` row's value, else "Not started" (a value outside the five reads as Not started). */
    fun stateOf(log: List<Entry>, work: Int): String = stateName(entryOf(log, work)?.value)

    /** The works In progress, in S5's order. */
    fun openWorks(log: List<Entry>): List<Int> = WORK_IDS.filter { stateOf(log, it) == IN_PROGRESS }

    /** The live change request on a work: the newest cr-kind row, when it is a `cr_raise`; else null. */
    fun crOf(log: List<Entry>, work: Int): Entry? =
        newest(log, work, KIND_CR_RAISE, KIND_CR_CLEAR)?.takeIf { it.kind == KIND_CR_RAISE }

    /** In the approved scope: a null baseline (not yet approved) puts everything in scope -- nothing to drift from. */
    fun inScope(scope: List<Int>?, work: Int): Boolean = scope == null || scope.contains(work)

    /** One change request as the header counts it: an off-baseline work with a live CR. */
    data class ChangeRequest(val id: Int, val name: String, val cr: Entry)

    /** "Change requests: N" -- every work that is off the baseline AND has a live CR, in S5's order (ct61b E1, E7). */
    fun changeRequests(scope: List<Int>?, log: List<Entry>): List<ChangeRequest> =
        WORKS.mapNotNull { w -> if (inScope(scope, w.id)) null else crOf(log, w.id)?.let { ChangeRequest(w.id, w.name, it) } }

    /** True when any `state` row exists: the recorded-work exemption from the lock (ct61b D3). */
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

    /** The album is closed for display when Cleaning & handover (140) is Done. */
    fun closed(log: List<Entry>): Boolean = stateOf(log, LAST_WORK) == DONE

    /** When it closed: the row that put 140 at Done, or null while open. */
    fun closedAt(log: List<Entry>): String? = if (closed(log)) entryOf(log, LAST_WORK)?.at else null

    /** Only leaving Done needs a remark (v61a's Done guard). */
    fun remarkNeeded(from: String): Boolean = from == DONE

    /**
     * The quote stage a quote-journey item was taken in: the newest `stage` row (by seq) at or
     * before [at], else Survey. Both times are S5's toISOString, so they compare as text.
     */
    fun quoteAt(stages: List<Entry>, at: String): String {
        var q = QuoteStage.STAGES[0]
        for (e in bySeq(stages)) if (e.kind == KIND_STAGE && e.value in QuoteStage.STAGES && e.at <= at) q = e.value
        return q
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

    /** One quote-stage group of chapter 0. */
    data class QuoteGroup(val stage: String, val i: Int, val items: List<Photo>, val reached: Boolean, val now: Boolean)

    /** Chapter 0, the quote journey: a group per quote stage, the unfiled items (staff only, by [canSee]). */
    data class QuoteChapter(val groups: List<QuoteGroup>, val unfiled: List<Photo>, val count: Int, val locked: Boolean)

    /** Chapters 1-16, one per work, each a grid in time order with its own state. */
    data class Chapter(
        val n: Int,
        val id: Int,
        val name: String,
        val state: String,
        val items: List<Photo>,
        val mine: Boolean,
        val locked: Boolean,
        val execLocked: Boolean,
        val inScope: Boolean,
        val cr: Entry?,
    ) {
        val count: Int get() = items.size
        val last: String? get() = items.lastOrNull()?.at
    }

    /** The album as displayed. `order` is the display order the viewer's ‹ › walks. */
    data class Chapters(
        val quote: QuoteChapter,
        val works: List<Chapter>,
        val total: Int,
        val closed: Boolean,
        val closedAt: String?,
        val order: List<Photo>,
        val execLocked: Boolean,
        val changeRequests: List<ChangeRequest>,
    )

    /**
     * The chapters for [viewer]: chapter 0 (six quote groups, plus the unfiled items), then the
     * sixteen works in S5's order. [items] are the album's items -- S5's photos and captures
     * with the phone's own, composed by the caller -- in time order; [log] and [stages] the
     * mirror's rows with any provisional ones ([provisional]); [quoteStage] what the phone
     * knows of the quote stage (null = not read). Work chapters lock under [exec]; a vendor's
     * chapter 0 and the works outside `mine` lock too. An item on a work id that is not one
     * of the sixteen is unfiled.
     */
    fun chapters(
        items: List<Photo>, log: List<Entry>, stages: List<Entry>, scope: List<Int>?, quoteStage: String?, viewer: Viewer,
    ): Chapters {
        val all = visible(viewer, items)
        val cur = QuoteStage.indexOf(quoteStage?.trim().orEmpty())
        val ven = viewer.role == VENDOR
        val xlock = !exec(quoteStage, hasStateEntry(log))
        val groups = QuoteStage.STAGES.mapIndexed { i, q ->
            QuoteGroup(q, i, all.filter { it.work == QUOTE_WORK && quoteAt(stages, it.at) == q },
                reached = if (cur < 0) i == 0 else i <= cur, now = i == cur)
        }
        val unfiled = all.filter { it.work == null || (it.work != QUOTE_WORK && !isWork(it.work)) }
        val quote = QuoteChapter(groups, unfiled, groups.sumOf { it.items.size } + unfiled.size, locked = ven)
        val works = WORKS.mapIndexed { i, w ->
            Chapter(
                n = i + 1, id = w.id, name = w.name, state = stateOf(log, w.id),
                items = all.filter { it.work == w.id },
                mine = ven && w.id in viewer.mine,
                locked = (ven && w.id !in viewer.mine) || xlock,
                execLocked = xlock,
                inScope = inScope(scope, w.id),
                cr = crOf(log, w.id),
            )
        }
        val order = groups.flatMap { it.items } + unfiled + works.flatMap { it.items }
        return Chapters(quote, works, all.size, closed(log), closedAt(log), order, xlock, changeRequests(scope, log))
    }

    // ---- the writer gates: null = allowed; a line = refused, and why (nothing is written) ----

    /**
     * Setting a work's state (PM only; ct61b D1-D3). [from] is the DERIVED state ([stateOf]),
     * which the POST carries as `prev` -- S5 refuses a `prev` that is not what its log says.
     * Refused: not PM, the chapters locked ([exec] false), not a work, not a state, the same
     * state, or leaving Done with no remark.
     */
    fun stateWrite(viewer: Viewer, exec: Boolean, work: Int, from: String, to: String, remark: String): String? = when {
        viewer.role != PM -> STATE_PM_ONLY
        !exec -> EXEC_LOCK_BANNER
        !isWork(work) -> NOT_A_WORK
        to !in STATES -> NOT_A_STATE
        from == to -> SAME_STATE
        remarkNeeded(from) && remark.isBlank() -> DONE_REASON_NEED
        else -> null
    }

    /**
     * Raising a change request (PM only, ct61b E8): a baseline must exist, the work must be
     * off it, and no CR may be live on it (E7: in scope -> refused). The chapters must be open
     * -- S5's log POST holds the exec rule for every kind.
     */
    fun crRaise(viewer: Viewer, exec: Boolean, scope: List<Int>?, work: Int, log: List<Entry>): String? = when {
        viewer.role != PM -> CR_PM_ONLY
        !exec -> EXEC_LOCK_BANNER
        !isWork(work) -> NOT_A_WORK
        scope == null -> CR_NO_BASELINE
        inScope(scope, work) -> CR_IN_SCOPE
        crOf(log, work) != null -> CR_LIVE
        else -> null
    }

    /** Clearing a change request (PM only, E8): only a live one (E7: clearing drops the count). */
    fun crClear(viewer: Viewer, exec: Boolean, work: Int, log: List<Entry>): String? = when {
        viewer.role != PM -> CR_PM_ONLY
        !exec -> EXEC_LOCK_BANNER
        !isWork(work) -> NOT_A_WORK
        crOf(log, work) == null -> CR_NONE
        else -> null
    }

    /**
     * Recording a photo: PM or Designer into the quote journey (0) always, into a work only
     * when the chapters are open; a vendor into `mine` only (S5: 403 otherwise; no exec gate on
     * a vendor's upload, as the prototype's `upload`); a client never. The phone never records
     * an unfiled photo -- unfiled is the server's filing of an as-is capture whose stage maps
     * to null -- so [work] is 0 or a work id.
     */
    fun photoRecord(viewer: Viewer, exec: Boolean, work: Int): String? = when (viewer.role) {
        PM, DESIGNER -> when {
            work != QUOTE_WORK && !isWork(work) -> NOT_A_WORK
            work != QUOTE_WORK && !exec -> EXEC_LOCK_BANNER
            else -> null
        }
        VENDOR -> if (work in viewer.mine) null else NOT_YOUR_WORK
        else -> PHOTO_NO_CLIENT
    }

    /** Accept / Not for client (PM only): a vendor's row only; the same state again is a no-op, so it is refused here. */
    fun review(viewer: Viewer, photo: Photo, state: String): String? = when {
        viewer.role != PM -> REVIEW_PM_ONLY
        state != REVIEW_ACCEPTED && state != REVIEW_INTERNAL -> NOT_A_REVIEW
        photo.party != PARTY_VENDOR -> REVIEW_VENDOR_ONLY
        photo.review == state -> REVIEW_SAME
        else -> null
    }

    // ---- the outbox: what the phone queued, and what S5's verdict does to it ----

    /** The op kinds the queue holds: the three log kinds, a photo record (then its JPEG), a review. */
    const val OP_PHOTO = "photo"
    const val OP_REVIEW = "review"

    /**
     * One queued write, with its writer-minted id (the replay key) and the `base` it saw: a log
     * op's `base` is the work's newest `seq` of any kind (0 = none); a review's is the row's
     * `review_at` ('' = never). `work` is what a 409 drops by (a review's is its photo's work).
     * `error` is S5's own words once it refused or won; blank while pending.
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
    )

    /**
     * The phone's copy of one project's album: the [mirror] (S5's last GET; null until the
     * first 200), the FIFO [queue] of ops still to send, and the ops S5 [dropped] -- each with
     * S5's `error` on it, for the queue row -- which are never sent again. The caller clears
     * `dropped` when the person has seen them.
     */
    data class Outbox(val mirror: View? = null, val queue: List<Op> = emptyList(), val dropped: List<Op> = emptyList())

    /** What S5's answer to one op carried, typed: the row(s) the mirror takes. Absent pieces leave the mirror as it was. */
    data class Answer(val entry: Entry? = null, val work: Work? = null, val photo: Photo? = null)

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

    /** The mirror with S5's row(s) taken: an entry appended once by id (log order by seq), a work row replaced, a photo replaced or added. */
    fun merged(mirror: View?, a: Answer): View? {
        if (mirror == null) return null
        var m: View = mirror
        a.entry?.let { e ->
            if (m.log.none { it.id == e.id }) m = m.copy(log = bySeq(m.log + e))
            else m = m.copy(log = bySeq(m.log.map { if (it.id == e.id) e else it }))
        }
        a.work?.let { w ->
            val rows = if (m.works.any { it.id == w.id }) m.works.map { if (it.id == w.id) w else it } else m.works + w
            m = m.copy(works = rows.sortedBy { WORK_IDS.indexOf(it.id).let { i -> if (i < 0) Int.MAX_VALUE else i } })
        }
        a.photo?.let { p ->
            val rows = if (m.photos.any { it.id == p.id }) m.photos.map { if (it.id == p.id) p else it } else m.photos + p
            m = m.copy(photos = rows)
        }
        return m
    }

    private fun without(queue: List<Op>, id: String): List<Op> = queue.filter { it.id != id }

    /**
     * 200 `written: true`: the op leaves the queue and the mirror takes what S5 sent back. A
     * null mirror stays null -- a fragment cannot stand in for a GET, and K2 GETs after a drain.
     */
    fun afterPost(o: Outbox, op: Op, a: Answer): Outbox = o.copy(mirror = merged(o.mirror, a), queue = without(o.queue, op.id))

    /**
     * 200 `written: false`: a replay of an id S5 already holds (the first answer was lost) --
     * harmless. The op leaves the queue; what S5 sends back is the same row, merged as a 200.
     */
    fun afterReplay(o: Outbox, op: Op, a: Answer): Outbox = afterPost(o, op, a)

    /**
     * The op and every LATER queued op on the same work, marked with [error], moved from the
     * queue to `dropped`; ops on other works stay, in order.
     */
    private fun dropWork(o: Outbox, op: Op, error: String): Outbox {
        val i = o.queue.indexOfFirst { it.id == op.id }
        if (i < 0) return o
        val gone = o.queue.filterIndexed { j, x -> j == i || (j > i && x.work == op.work) }
        return o.copy(queue = o.queue.filter { x -> gone.none { it.id == x.id } },
            dropped = o.dropped + gone.map { it.copy(error = error) })
    }

    /**
     * 409: S5 WINS. The op and every later queued op on the same work are dropped (each row
     * showing S5's [error]); the mirror takes the current state the 409 body echoes ([a]).
     */
    fun afterConflict(o: Outbox, op: Op, error: String, a: Answer): Outbox = dropWork(o, op, error).let { it.copy(mirror = merged(it.mirror, a)) }

    /**
     * 409 `locked: true`: S5, the authority, answered the lock banner -- the quote stage there
     * is not Client approved and no work is recorded, whatever the phone believed (an unread
     * stage). The op and every later queued op on the same work are dropped, each row saying
     * so in S5's words; the body carries no rows, so the mirror is as it was, and the next GET
     * (or the quote-stage refresh) tells the phone the stage.
     */
    fun afterLocked(o: Outbox, op: Op, error: String): Outbox = dropWork(o, op, error)

    /**
     * 400 / 403 / 413 / 415: refused for good -- dropped with one note (S5's [error]); nothing
     * else moves. Ops after it, on the same work or not, still go: the refusal was this op's shape or right, not the work's state.
     */
    fun afterRefused(o: Outbox, op: Op, error: String): Outbox =
        if (o.queue.none { it.id == op.id }) o
        else o.copy(queue = without(o.queue, op.id), dropped = o.dropped + op.copy(error = error))
}
