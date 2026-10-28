package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * THE CLIENT'S WISH LIST, ON THE PHONE (v52 item 16, built as slice 5b). Amit, 14 Oct 2026,
 * 06:35 IST: "also as you have services list , i should be able to quickly mark scope while
 * talking with different service vendor using this app ... this is kind of wish list client
 * will have and vendor will fulfil"; 06:36 IST: "but the wishlist should never get mixed with
 * survey as this wishlist comes after survey".
 *
 * So it hangs off the PROJECT, never a room or survey prep, and it is gated by S5 QUOTE's
 * quote stage (read by QuoteStageSync), exactly as prototype v61c.1 (Version 69) gates it at
 * every one of the six stages (TOOLS.wishEdit [0,1,1,0,1], index61c1.html:3954, read by can()
 * at :3976; wishMode at :4162-4165):
 *  - the ticks are editable at Client wish list, Vendor quoted and Revisions (v61c ticks again
 *    at Revisions); from Vendor quoted on an edit still saves but warns "tell the vendor";
 *  - READ-ONLY at Survey (finish the survey first; S5 refuses the PUT there too);
 *  - READ-ONLY at Estimated (v61b, 26 Oct 2026: Estimation is the hand-off, refuse.readonly.
 *    wishlist), although S5 still saves there -- the read-only screen is the phone's own choice;
 *  - READ-ONLY AT CLIENT APPROVED (v61c.1; Amit, 28 Oct 2026, ~07:15 IST, "Lock it at
 *    approval"), under refuse.readonly.wishlist_approved, "Approved — changes go through change
 *    requests": after approval the change requests are the ONE place a change is recorded, and
 *    S5's PUT refuses there with its `final` 409. Client approved has no TOOLS column; can()
 *    reads a missing one as 0, so [editable] and [vendorUi] are false there.
 * The Vendor field and a row's "vendor X" show only at Vendor quoted and Revisions (TOOLS.
 * vendorUI [0,0,1,0,1]; v54 finding 4: the Wish list phase shows no vendor text at all), and the
 * head drops "— the vendor quotes against it" wherever the field is hidden ([head]). A stage
 * the phone has not read is UNKNOWN, never silently Survey -- the list stays editable then,
 * with its Vendor field (can() is true for an unread phase; FX16), so a basement with no
 * signal never blocks marking scope with a vendor.
 *
 * The sync model is decided (15 Oct 2026): ONE list per project against ONE `wishlist_at`
 * on S5 (decision 2); on a 409 S5 WINS -- the phone takes S5's rows, says so, and the person
 * re-applies what is missing. Two 409s are not a stale base, and say S5's own words on the
 * visit's status line ([Local.status]) instead of the REPLACED note: `final: true` at Client
 * approved ([afterFinal]: S5's list for good) and the Survey 409, which carries no list
 * ([afterSurvey]: the list waits, dirty); [after409] tells the three apart, final first, and both
 * refusals for the stage name it, which the phone's one copy of the stage takes (QuoteStage.named).
 * A project S5 has never had answers 404 and the list
 * waits on the phone (decision 3); a phone-only project (no ERP id) keeps its list under a local key
 * that is re-keyed to the ERP id once the project has one, so a list made offline is not
 * orphaned. This is the pure half: the services, the texts, the stage rules and every
 * transition of what the phone keeps. WishListTest pins all of it.
 */
object WishList {

    data class Service(val id: String, val name: String, val detail: String)

    /**
     * S5's WISH_SERVICES (functions/_lib/asis.js), in S5's order: the eleven of the approved
     * prototype (v52 wish.js) and the twelfth, Plumbing & sanitary, appended on 16 Oct 2026
     * (Amit). A new service is APPENDED there, never inserted; change this only with S5.
     */
    val SERVICES: List<Service> = listOf(
        Service("electrical", "Electrical works", ""),
        Service("cctv", "CCTV", ""),
        Service("intercom", "Intercom", ""),
        Service("false-ceiling", "False ceiling", ""),
        Service("doors-windows", "Doors & windows", "safety door, aluminium main door, UPVC"),
        Service("balcony-grill", "Balcony grill", ""),
        Service("carpentry", "Carpentry / furniture", ""),
        Service("master-bath", "Master bathroom — complete renovation", ""),
        Service("guest-bath", "Guest bathroom — complete renovation", ""),
        Service("painting", "Painting", ""),
        Service("curtains", "Curtains — roller blinds", ""),
        Service("plumbing", "Plumbing & sanitary",
            "water and drain points, taps, geyser, sanitary ware outside the bathroom renovations"),
    )

    val IDS: Set<String> = SERVICES.map { it.id }.toSet()

    // S5's caps (validWishlist): what the phone lets through is what S5 accepts.
    const val MAX_ROOMS = 40
    const val MAX_ROOM = 140
    const val MAX_NOTE = 500
    const val MAX_VENDOR = 140

    // The prototype's texts (wish.js), character for character. The head and the two lock lines
    // are keyed ([head], [lockedLine]); the rest of the file is keyed in port slice M4.
    const val FINISH_SURVEY = "Finish the survey first — the stage moves to Client wish list on S5"
    const val UNREAD = QuoteStage.UNREAD
    const val AFTER_QUOTE = "Changing the wish list after a vendor quoted — tell the vendor"
    const val WHOLE_FLAT = "Whole flat"
    const val NOT_ASKED = "not asked for"
    const val APPLIES_TO = "APPLIES TO"
    const val SAVED_HERE = "saved on this phone"
    const val NOTHING_SAVED = "nothing saved yet"
    /** Decision 2: shown after a 409 replaced the phone's list with S5's. */
    const val REPLACED = "S5 had a newer list; yours was replaced, re-apply what is missing"
    /** Before S5's own reason when S5 refused the list for good (a non-staff key, a bad shape). */
    const val NOT_SENT = "Not sent to S5"

    /** One service's row: wanted or not, the rooms it applies to (none = the whole flat), a note, a vendor. */
    data class Row(
        val on: Boolean = false,
        val rooms: List<String> = emptyList(),
        val note: String = "",
        val vendor: String = "",
    )

    /**
     * What the phone keeps for one project. [client] and [title] are what a local key is
     * re-keyed by; [at] is S5's wishlist_at last seen ("" never) and is the `base` of the next
     * PUT; [dirty] means an edit S5 has not taken yet; [savedAt] when this phone last saved
     * an edit ("" never); [note] a message for the screen (the REPLACED or NOT_SENT note),
     * cleared by the next edit.
     *
     * [status] is S5's own words when it refused a save for its STAGE -- the `final` 409 at
     * Client approved, the 409 at Survey -- exactly as S5 sent them (English: storage never sees
     * Marathi; [statusLine] draws them). It is the prototype's `[data-wish409][role=status]` line
     * (§16.4), so it belongs to the VISIT: drawn under the banners and cleared when the list is
     * left ([left]), as well as by an edit and by any later answer to a PUT. Without that, a list
     * that can never be edited again would carry "…; not saved" under "Approved — changes go
     * through change requests" for ever (port 61c critic X6). A refusal that lands while the list
     * is not open is shown on the next visit and goes when that visit ends -- drawn only while the
     * stage it refused for still holds ([statusLine]), so it never contradicts the stage line.
     *
     * [statusSeen] is true once a visit has drawn [status] ([shown]). A visit can end without a
     * leave -- a rotation recreates the activity, the process is killed after Home -- and then the
     * next visit clears a status already seen ([entered]), so "a new visit clears that line"
     * (§16.4) holds without losing a refusal that landed while the list was closed, which has not
     * been seen and shows once (slice W review). Every new status starts unseen.
     */
    data class Local(
        val client: String,
        val title: String,
        val rows: Map<String, Row> = emptyMap(),
        val at: String = "",
        val dirty: Boolean = false,
        val savedAt: String = "",
        val note: String = "",
        val status: String = "",
        val statusSeen: Boolean = false,
    )

    // ---- keys ---------------------------------------------------------------

    private const val LOCAL = "local:"

    /** The prototype's key for a project made on the phone: 'local:<client>|<title>'. */
    fun localKey(client: String, title: String): String = "$LOCAL$client|$title"

    /** The key a project's list is kept under: its ERP id once it has one, else the local key. */
    fun keyOf(serverId: String, client: String, title: String): String =
        if (serverId.isNotBlank()) serverId.trim() else localKey(client, title)

    fun isLocal(key: String): Boolean = key.startsWith(LOCAL)

    private const val MOVED = "moved:"

    /**
     * Where a moved local list is forwarded from: written beside the list when [rekeys] moves
     * it, so a screen still holding the phone-only project (MainActivity's navProject is not
     * re-read after a sync) reads and edits the moved list instead of starting an empty one.
     */
    fun forwardKey(localKey: String): String = MOVED + localKey

    fun isForward(key: String): Boolean = key.startsWith(MOVED)

    /** The key a list is really kept under: a moved local key follows its forward ([lookup] reads one). */
    fun resolve(key: String, lookup: (String) -> String?): String =
        if (isLocal(key)) lookup(forwardKey(key))?.trim()?.ifBlank { null } ?: key else key

    /**
     * The ERP id the quote stage of the list kept under [key] is read under, or null when that list
     * is only on this phone: [key] itself when it is an ERP id, else the id a moved local list went
     * to ([resolve]). So a screen still holding the phone-only project after the sync that re-keyed
     * its list reads the stage S5 named under that id -- the lock after a `final` 409 included --
     * for the Wish list, its chip and the project line alike (slice W fix review), not "not on S5".
     */
    fun stageKey(key: String, lookup: (String) -> String?): String? =
        resolve(key, lookup).trim().takeUnless { it.isEmpty() || isLocal(it) }

    /** Names compared as the catalogue compares them (Catalogue.keyOf): trimmed, case-blind, runs of space or _ as one. */
    fun norm(s: String): String = s.trim().lowercase().replace(Regex("[\\s_]+"), " ")

    /** An ERP project as the refreshed masters list it. */
    data class Erp(val serverId: String, val client: String, val title: String)

    /**
     * Which local lists now belong to an ERP project: local key -> ERP id, for every local
     * list whose client and title match a project with an id. The first match wins, as the
     * catalogue's own cover rule matches a title at any site.
     */
    fun rekeys(locals: Map<String, Local>, erp: List<Erp>): Map<String, String> {
        val out = linkedMapOf<String, String>()
        for ((k, l) in locals) {
            if (!isLocal(k)) continue
            val hit = erp.firstOrNull {
                it.serverId.isNotBlank() && norm(it.client) == norm(l.client) && norm(it.title) == norm(l.title)
            } ?: continue
            out[k] = hit.serverId.trim()
        }
        return out
    }

    /**
     * A local list moved under its ERP id, where [existing] is anything already kept there.
     * The list made on the phone carries the person's work, so it moves and stays dirty (it
     * still has to reach S5) -- on its OWN base, "", never the stamp this phone read for the
     * id: whoever typed it offline never saw S5's copy, so taking that stamp would let the
     * PUT overwrite staff's list with no 409. With "" S5 answers 409 whenever it holds a saved
     * list, and S5 wins with the REPLACED note (decision 2).
     * Null when an unsent list is already under the id: that is work too, so the local list
     * is not moved over it (nor dropped) -- it stays under its local key and moves on a later
     * sync, once the list under the id has gone.
     */
    fun adopt(local: Local, existing: Local?): Local? {
        if (existing != null && existing.dirty) return null
        return local.copy(at = "")
    }

    /** One list moved by a rename: its new key, the list as it now reads, and whether the old key stays (copy). */
    data class Rename(val to: String, val list: Local, val keepOld: Boolean)

    /**
     * A phone-only client or project renamed on this phone (Catalogue.renameLocal): which
     * local lists move, old key -> (new key, the list with its new client or title). A site
     * rename moves nothing (the key holds no site). [live] is the local keys still in use
     * AFTER the rename: a list whose old key still names another phone-only project (the same
     * title at another site) is copied ([Rename.keepOld]) rather than taken from it, and a new key that already
     * holds a list keeps that list (it belongs to a project that already has the name).
     */
    fun renamed(
        locals: Map<String, Local>, kind: String, client: String, title: String, newName: String,
        live: Set<String>,
    ): Map<String, Rename> {
        val n = newName.trim()
        if (n.isEmpty()) return emptyMap()
        // Keys are matched EXACTLY (the screen looks a list up by the project's own spelling),
        // so a rename that only changes case still moves the list; names match as the catalogue does.
        val out = linkedMapOf<String, Rename>()
        for ((k, l) in locals) {
            if (!isLocal(k)) continue
            val next = when (kind) {
                "client" -> if (norm(l.client) == norm(client)) l.copy(client = n) else null
                "project" -> if (norm(l.client) == norm(client) && norm(l.title) == norm(title)) l.copy(title = n) else null
                else -> null
            } ?: continue
            val to = localKey(next.client, next.title)
            if (to == k || to in locals) continue
            out[k] = Rename(to, next, keepOld = k in live)
        }
        return out
    }

    // ---- counts and texts ---------------------------------------------------

    fun row(rows: Map<String, Row>, id: String): Row = rows[id] ?: Row()

    /** How many services are wanted. Ids S5 no longer lists are not counted. */
    fun count(rows: Map<String, Row>): Int = SERVICES.count { rows[it.id]?.on == true }

    /** The project screen's chip. */
    fun chip(n: Int): String = "Wish list $n/${SERVICES.size}"

    /**
     * Whether the project screen shows the Wish list chip: everywhere but Survey, the prototype's
     * `#appv[data-ph54="0"] [data-sk=pchips] .chip.wq{display:none}` (index61c1.html:1255; TOOLS.
     * wishlist [0,1,1,1,1] at :3953 documents it) -- the wish list comes after the survey (Amit,
     * 14 Oct 06:36 IST). Shown with the stage unread (no data-ph54, as can() is true for an unread
     * phase) and at Client approved, where the CSS hides nothing and the locked list is read
     * (ct61c opens it there); so this is not can(), which reads Client approved's missing column as 0.
     */
    fun chipShown(stageIdx: Int?): Boolean = stageIdx != 0

    /** The head's count. */
    fun total(n: Int): String = "$n of ${SERVICES.size} wanted"

    /** What the APPLIES TO choice reads as: the whole flat when no room is picked, else the rooms. */
    fun subLine(row: Row): String = if (row.rooms.isEmpty()) WHOLE_FLAT else row.rooms.joinToString(" · ")

    /**
     * The row's second line: where, the note, and the vendor when wanted and the Vendor field is
     * shown ([vendorUi]: v54 finding 4, the Wish list phase shows no vendor text); the service's
     * detail when not wanted.
     */
    fun rowLine(service: Service, row: Row, vendorUi: Boolean): String =
        if (row.on) listOfNotNull(subLine(row), row.note.ifBlank { null },
            row.vendor.takeIf { vendorUi }?.ifBlank { null }?.let { "vendor $it" }).joinToString(" · ")
        else service.detail.ifBlank { NOT_ASKED }

    /** The head's state: unsent here, on S5 since a time, or nothing yet. */
    fun savedText(l: Local?): String {
        if (l == null) return NOTHING_SAVED
        if (l.dirty) return SAVED_HERE
        QuoteStage.instantOf(l.at)?.let { return "on S5 since ${QuoteStage.dayTime(it)} IST" }
        return if (l.savedAt.isNotBlank()) SAVED_HERE else NOTHING_SAVED
    }

    // ---- the stage rules ----------------------------------------------------

    /**
     * The quote stage's 0-based place, or null when the phone does not know it: never read,
     * the project not on S5's list, no stage, or a name that is not one of the six.
     */
    fun stageIndex(cache: QuoteStage.Cache?): Int? {
        if (cache == null || !cache.listed) return null
        val i = QuoteStage.indexOf(cache.stage.trim())
        return if (i < 0) null else i
    }

    /** Estimated (v61b's hand-off phase, Estimation): the ticks are locked; S5 still saves there. */
    const val ESTIMATED = 3

    /** Client approved (v61c.1): the list is locked for good, and S5 refuses the PUT (`final`). */
    const val APPROVED = 5

    /**
     * The prototype's TOOLS rows over its five strip phases (index61c1.html:3954-3955), one column
     * per quote stage from Survey to Revisions. Client approved is past every phase and has no
     * column, which can() reads as 0 (index61c1.html:3976) -- so both are false there.
     */
    private val WISH_EDIT = listOf(false, true, true, false, true)
    private val VENDOR_UI = listOf(false, false, true, false, true)

    /** The prototype's can(tool, phase): true when the phase is not read, else the tool's column, a missing one 0. */
    private fun can(row: List<Boolean>, stageIdx: Int?): Boolean = stageIdx == null || row.getOrElse(stageIdx) { false }

    /**
     * The ticks (TOOLS.wishEdit, v61c.1): editable at Client wish list, Vendor quoted and
     * Revisions, and when the stage is not read (FX16); read-only at Survey, Estimated and
     * Client approved.
     */
    fun editable(stageIdx: Int?): Boolean = can(WISH_EDIT, stageIdx)

    /**
     * The Vendor field and a row's "vendor X" (TOOLS.vendorUI): only at Vendor quoted and
     * Revisions, and when the stage is not read (can() is true for an unread phase).
     */
    fun vendorUi(stageIdx: Int?): Boolean = can(VENDOR_UI, stageIdx)

    /** The head's line: "— the vendor quotes against it" only where the Vendor field is shown (v54 finding 4). */
    fun head(tx: Tx, vendorUi: Boolean): String =
        if (vendorUi) tx.wish_what_client_wants_after else tx.wish_what_client_wants_after_2

    /**
     * From Vendor quoted on an edit warns (and still saves); the prototype only warns (after =
     * idx >= 2). Never at Client approved, where nothing can be edited (§16.5: the after-approval
     * notice cannot be reached; its key stays and no screen renders it).
     */
    fun warnAfterQuote(stageIdx: Int?): Boolean = stageIdx != null && stageIdx >= 2 && stageIdx != APPROVED

    /**
     * The read-only line (wishMode's banner): refuse.readonly.wishlist at Estimated and
     * refuse.readonly.wishlist_approved at Client approved -- S5's APPROVED_FINAL verbatim, without
     * the 409's "; not saved", since nothing was tried. Null at every other stage and when the
     * stage is unknown.
     */
    fun lockedLine(tx: Tx, stageIdx: Int?): String? = when (stageIdx) {
        ESTIMATED -> tx.refuse_readonly_wishlist
        APPROVED -> tx.refuse_readonly_wishlist_approved
        else -> null
    }

    /**
     * The project is not on S5, which is not the same as a stage not read: a phone-only
     * project (it never asks S5) or one S5 answered "not listed" for (404 until its first room
     * upload, decision 3). The sub-line already says "not on S5 yet" (QuoteStage.phoneOnly /
     * lineFor), so the unread banner -- "S5 decides when online" -- would contradict it.
     */
    fun notOnS5(synced: Boolean, cache: QuoteStage.Cache?): Boolean = !synced || (cache != null && !cache.listed)

    /**
     * The banners over the list, in the prototype's order. [warned]: changed after a vendor
     * quoted, this visit. [notOnS5]: no unread banner, since going online changes nothing.
     */
    fun banners(tx: Tx, stageIdx: Int?, warned: Boolean, notOnS5: Boolean = false): List<String> = buildList {
        if (stageIdx == 0) add(FINISH_SURVEY)
        lockedLine(tx, stageIdx)?.let { add(it) }
        if (stageIdx == null && !notOnS5) add(UNREAD)
        if (warned && warnAfterQuote(stageIdx)) add(AFTER_QUOTE)
    }

    /**
     * The note to draw over the banners ([Local.note]: REPLACED, or NOT_SENT and S5's reason), or
     * null. NEVER AT CLIENT APPROVED: both notes ask for something the locked list cannot take --
     * re-applying, or an edit that tries once more -- and only an edit clears them ([edited]), so
     * there they would stay for ever on a list nothing can change (slice W review). That includes a
     * REPLACED written by APK 0.3.262, which read S5's `final` 409 as a stale base. The note is kept
     * as stored: it is not the screen's to rewrite, and a stage read that is not Client approved
     * shows it again. The prototype draws no note under its lock either (§16.3).
     */
    fun noteLine(l: Local?, stageIdx: Int?): String? =
        l?.note?.takeIf { it.isNotBlank() && stageIdx != APPROVED }

    // ---- edits --------------------------------------------------------------

    /** One line, as the prototype and S5 keep a note: a newline and the spaces around it become one space. */
    fun oneLine(v: String): String = v.replace(Regex("\\s*[\\r\\n]+\\s*"), " ")

    fun cleanNote(v: String): String = oneLine(v).take(MAX_NOTE)
    fun cleanVendor(v: String): String = oneLine(v).take(MAX_VENDOR)

    /** Wanted / not wanted. A row keeps its rooms, note and vendor when unticked, as the prototype does. */
    fun toggle(rows: Map<String, Row>, id: String): Map<String, Row> {
        val r = row(rows, id)
        return rows + (id to r.copy(on = !r.on))
    }

    /** The whole flat: no room picked. */
    fun wholeFlat(rows: Map<String, Row>, id: String): Map<String, Row> =
        rows + (id to row(rows, id).copy(rooms = emptyList()))

    /**
     * A room chip pressed: off if it was on; else on, kept in the grid's order ([projectRooms]),
     * with any room the grid does not show (written on S5) kept after them. At most [MAX_ROOMS].
     */
    fun toggleRoom(rows: Map<String, Row>, id: String, room: String, projectRooms: List<String>): Map<String, Row> {
        val r = row(rows, id)
        val next = if (room in r.rooms) r.rooms.filter { it != room }
        else (projectRooms.filter { it == room || it in r.rooms } + r.rooms.filter { it !in projectRooms }).distinct()
        return rows + (id to r.copy(rooms = next.take(MAX_ROOMS)))
    }

    fun setNote(rows: Map<String, Row>, id: String, v: String): Map<String, Row> =
        rows + (id to row(rows, id).copy(note = cleanNote(v)))

    fun setVendor(rows: Map<String, Row>, id: String, v: String): Map<String, Row> =
        rows + (id to row(rows, id).copy(vendor = cleanVendor(v)))

    /**
     * S5's stage refusal ([Local.status]) as the status line under [shown], the banners already
     * drawn; null when there is none, or when a banner already says exactly that (the Survey 409
     * repeats FINISH_SURVEY word for word -- in English, which is how the banner draws it until M4
     * keys it, so the Marathi line is not drawn under it either). Words S5 sends that are a key's
     * English are drawn from that key, as the prototype's text match draws its refused line (L57):
     * the `final` 409 is refuse.stage.approved_final, the Survey one refuse.wish.finish_survey_first_stage.
     * Any other words are S5's, shown as sent.
     *
     * A REFUSAL FOR THE STAGE IS DRAWN ONLY WHILE THAT STAGE HOLDS ([stageIdx], the phone's one copy
     * of the stage): the `final` words at Client approved, the Survey words at Survey, and either
     * while the stage is unread (slice W fix review). The stored status outlives its cause -- a
     * Survey 409 stays until a PUT's 200 or an edit, and the chip is hidden at Survey, so the line
     * was only ever drawn after staff had moved the stage on, "Finish the survey first" over an
     * editable list at Client wish list; and a D1 fix that takes a project back from Client approved
     * would draw "…; not saved" over an editable list. Derived here, not cleared in storage: the
     * words are S5's and stay as sent, and the stage decides only whether they are true now.
     */
    fun statusLine(tx: Tx, l: Local?, shown: List<String>, stageIdx: Int?): String? {
        val words = l?.status?.trim().orEmpty()
        if (words.isEmpty()) return null
        val text = when (words) {
            Tx.EN.refuse_stage_approved_final ->
                tx.refuse_stage_approved_final.takeIf { stageIdx == null || stageIdx == APPROVED }
            Tx.EN.refuse_wish_finish_survey_first_stage ->
                tx.refuse_wish_finish_survey_first_stage.takeIf { stageIdx == null || stageIdx == 0 }
            else -> words
        } ?: return null
        return text.takeIf { it !in shown && words !in shown }
    }

    /** The list is left (the visit ends): S5's status line goes with it; the note keeps its own rule. */
    fun left(l: Local): Local = if (l.status.isEmpty() && !l.statusSeen) l else l.copy(status = "", statusSeen = false)

    /** A visit has drawn S5's status line ([Local.statusSeen]); the same list when there is none or it was already seen. */
    fun shown(l: Local): Local = if (l.status.isEmpty() || l.statusSeen) l else l.copy(statusSeen = true)

    /**
     * A visit begins (the chip is pressed): a status line an earlier visit already drew goes, since
     * that visit ended without [left] (a rotation, the process killed); one not yet seen -- S5's
     * refusal landed while the list was closed -- stays for this visit. The same list otherwise.
     */
    fun entered(l: Local): Local = if (l.statusSeen) l.copy(status = "", statusSeen = false) else l

    /** An edit kept on the phone at [now]: dirty until S5 takes it, and any old note and status gone. */
    fun edited(l: Local, rows: Map<String, Row>, now: String): Local =
        l.copy(rows = rows, dirty = true, savedAt = now, note = "", status = "", statusSeen = false)

    // ---- the sync -----------------------------------------------------------

    /** Every service's row, in S5's order, for the PUT: what is not sent is not wanted. */
    fun forPut(rows: Map<String, Row>): Map<String, Row> = SERVICES.associate { it.id to row(rows, it.id) }

    /**
     * The rows as S5 stores them (validWishlist): every service, rooms trimmed, blank and
     * repeated rooms gone, note and vendor on one line and trimmed. Two lists that agree here
     * are the same list to S5.
     */
    fun normalised(rows: Map<String, Row>): Map<String, Row> = forPut(rows).mapValues { (_, r) ->
        Row(on = r.on, rooms = r.rooms.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
            note = oneLine(r.note).trim(), vendor = oneLine(r.vendor).trim())
    }

    /**
     * After a 200 to the PUT of [sent]: S5's stamp becomes the base. When nothing was edited
     * while the PUT was out the list is clean and KEEPS THE PHONE'S OWN ROWS -- they are what
     * S5 saved, up to S5's trim, and taking S5's trimmed copy would rewrite a Note field under
     * the cursor (a trailing space typed and lost). An edit made meanwhile stays, dirty, and
     * goes next time on the new base.
     */
    fun afterPut(current: Local, sent: Map<String, Row>, at: String): Local =
        if (forPut(current.rows) == forPut(sent)) current.copy(at = at, dirty = false, status = "", statusSeen = false)
        else current.copy(at = at, dirty = true, status = "", statusSeen = false)

    /**
     * A 409 because the base was stale (decision 2): S5 WINS. The phone takes S5's rows and
     * stamp and the dirty flag is cleared. The note says the list was replaced only when it
     * really was: after a lost 200 (S5 saved the PUT, the answer never arrived) the next PUT's
     * 409 carries the phone's own list back, and nothing is missing to re-apply.
     */
    fun afterConflict(current: Local, s5Rows: Map<String, Row>, at: String): Local =
        if (normalised(s5Rows) == normalised(current.rows)) current.copy(at = at, dirty = false, status = "", statusSeen = false)
        else current.copy(rows = s5Rows, at = at, dirty = false, note = REPLACED, status = "", statusSeen = false)

    /**
     * A 409 with `final: true` (v61c.1): the project is at Client approved and S5 will never take
     * a list again -- after approval a change is a change request. The phone takes S5's rows and
     * stamp for good (the stored list, which the answer carries in the stale-base 409's shape),
     * the dirty flag is cleared so nothing is PUT again, and the status line says S5's own words
     * ([error], its `error`) -- never REPLACED, since nothing is to be re-applied: an unsent list,
     * and an edit made while the PUT was out, are discarded (S5 wins, 15 Oct). Any old note goes,
     * and so does [Local.savedAt]: the phone's own save no longer stands, so the head reads "on S5
     * since …" when S5 holds a stamp and "nothing saved yet" when S5 never stored a list (its
     * `wishlist_at` is '' then) -- never "saved on this phone" over rows it just took from S5.
     */
    fun afterFinal(current: Local, s5Rows: Map<String, Row>, at: String, error: String): Local =
        current.copy(rows = s5Rows, at = at, dirty = false, savedAt = "", note = "",
            status = oneLine(error).trim(), statusSeen = false)

    /**
     * A 409 that carries no list: S5 answers so at Survey (asis.js FINISH_SURVEY), where it takes
     * no list yet. The list waits on the phone, still dirty, and goes next sync; the status line
     * says S5's own words ([error]) until the visit ends, a later answer replaces them, or an edit.
     * Each such answer is a refusal of its own, so it starts unseen ([Local.statusSeen]).
     */
    fun afterSurvey(current: Local, error: String): Local =
        current.copy(status = oneLine(error).trim(), statusSeen = false)

    /**
     * The three 409s of S5's `PUT …/wishlist` (asis.js at 943dfd2), told apart in ONE order:
     * [FINAL] first, because the `final` answer carries the stored list in the stale-base 409's
     * shape and read as [STALE] it says REPLACED and asks the person to re-apply an edit S5 can
     * never take (APK 0.3.262's defect); then [STALE], a list in the answer; else [SURVEY].
     * [forStage]: the refusal is for the project's STAGE, so the answer's `quote_stage` is S5's
     * current stage and the phone's copy of it takes that (QuoteStage.named).
     */
    enum class Kind409(val forStage: Boolean) { FINAL(true), STALE(false), SURVEY(true) }

    /** Which 409 an answer is: [final] its `final`, [hasRows] whether it carries `rows`. */
    fun kind409(final: Boolean, hasRows: Boolean): Kind409 = when {
        final -> Kind409.FINAL
        hasRows -> Kind409.STALE
        else -> Kind409.SURVEY
    }

    /**
     * The list after a 409 from the PUT, as WishSync parsed it: [final] its `final`, [s5Rows] its
     * `rows` (null when it carries none), [at] its `wishlist_at` (null when absent), [error] S5's
     * `error`. The one dispatch, pinned by WishListTest, since :app has no tests: [Kind409.FINAL]
     * is [afterFinal] (a final answer without rows keeps the rows shown), [Kind409.STALE]
     * [afterConflict], [Kind409.SURVEY] [afterSurvey]. An answer that does not parse is SURVEY's
     * shape (no final, no rows) and waits dirty, as it did before this function.
     */
    fun after409(current: Local, final: Boolean, s5Rows: Map<String, Row>?, at: String?, error: String): Local =
        when (kind409(final, s5Rows != null)) {
            Kind409.FINAL -> afterFinal(current, s5Rows ?: current.rows, at ?: current.at, error)
            Kind409.STALE -> afterConflict(current, s5Rows!!, at ?: "")
            Kind409.SURVEY -> afterSurvey(current, error)
        }

    /**
     * Whether S5 refused the PUT for good, so sending the same list again can only be refused
     * again: 403 (a key that is not MCFT staff -- vendors and clients may read the list but
     * not change it), 400 (a shape S5 will not take) and 413 (too large). Everything else
     * waits on the phone and goes next sync: 401 (the key is fixed in Settings, then the edit
     * goes), 404 (not on S5 yet), 405, 409 at Survey, a 5xx, no network.
     */
    fun refusedForGood(code: Int): Boolean = code == 400 || code == 403 || code == 413

    /**
     * S5 refused the list for good ([refusedForGood]): it is no longer dirty, so it is not
     * PUT again every sync (PLAN slice 16: "one note and no endless retry") and the next GET
     * on project open takes S5's copy again; the note carries S5's own reason. The rows stay
     * as typed until then, and the next edit clears the note and tries once more -- as does
     * an edit made while the refused PUT was out, which is left dirty here.
     */
    fun afterRefused(current: Local, sent: Map<String, Row>, message: String): Local =
        if (forPut(current.rows) != forPut(sent)) current
        else current.copy(dirty = false, status = "", statusSeen = false,
            note = listOf(NOT_SENT, message.trim()).filter { it.isNotEmpty() }.joinToString(" — "))

    /**
     * A GET on project open: a clean list takes S5's copy (staff edits on /projects); an
     * unsent edit is never touched. The GET is a compare-and-set: [seen] is the list as it was
     * stored when the GET went out, and when anything wrote it meanwhile (a PUT's 200, an
     * edit) the GET's answer is older than what is stored and is dropped. A stamp older than
     * the stored one is dropped too (both are S5's toISOString, so they compare as text).
     */
    fun afterGet(current: Local, s5Rows: Map<String, Row>, at: String, seen: Local? = current): Local =
        if (current.dirty || current != seen ||
            (at.isNotBlank() && current.at.isNotBlank() && at < current.at)) current
        else current.copy(rows = s5Rows, at = at)
}
