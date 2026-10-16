package com.malletcrafts.sitephotos.pano

/**
 * THE CLIENT'S WISH LIST, ON THE PHONE (v52 item 16, built as slice 5b). Amit, 14 Oct 2026,
 * 06:35 IST: "also as you have services list , i should be able to quickly mark scope while
 * talking with different service vendor using this app ... this is kind of wish list client
 * will have and vendor will fulfil"; 06:36 IST: "but the wishlist should never get mixed with
 * survey as this wishlist comes after survey".
 *
 * So it hangs off the PROJECT, never a room or survey prep, and it is gated by S5 QUOTE's
 * quote stage (read by QuoteStageSync): read-only at Survey, editable from Client wish list
 * on, and from Vendor quoted on an edit still saves but warns. A stage the phone has not read
 * is UNKNOWN, never silently Survey -- the list stays editable then (FX16), so a basement with
 * no signal never blocks marking scope with a vendor.
 *
 * The sync model is decided (15 Oct 2026): ONE list per project against ONE `wishlist_at`
 * on S5 (decision 2); on a 409 S5 WINS -- the phone takes S5's rows, says so, and the person
 * re-applies what is missing. A project S5 has never had answers 404 and the list waits on
 * the phone (decision 3); a phone-only project (no ERP id) keeps its list under a local key
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

    // The prototype's texts (wish.js), character for character.
    const val HEAD = "What the client wants, after the survey — the vendor quotes against it"
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
     * an edit ("" never); [note] a message for the screen (the 409 note), cleared by the next edit.
     */
    data class Local(
        val client: String,
        val title: String,
        val rows: Map<String, Row> = emptyMap(),
        val at: String = "",
        val dirty: Boolean = false,
        val savedAt: String = "",
        val note: String = "",
    )

    // ---- keys ---------------------------------------------------------------

    private const val LOCAL = "local:"

    /** The prototype's key for a project made on the phone: 'local:<client>|<title>'. */
    fun localKey(client: String, title: String): String = "$LOCAL$client|$title"

    /** The key a project's list is kept under: its ERP id once it has one, else the local key. */
    fun keyOf(serverId: String, client: String, title: String): String =
        if (serverId.isNotBlank()) serverId.trim() else localKey(client, title)

    fun isLocal(key: String): Boolean = key.startsWith(LOCAL)

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
     * The list made on the phone carries the person's work, so it is kept and stays dirty
     * (it still has to reach S5), on top of the base S5 last showed for that id ("" when
     * nothing was ever read, so S5 answers 409 if staff made one meanwhile -- and S5 wins).
     * Only an existing list that is itself unsent beats it: that is already work under the id.
     */
    fun adopt(local: Local, existing: Local?): Local {
        if (existing != null && existing.dirty) return existing
        return local.copy(at = existing?.at ?: "")
    }

    // ---- counts and texts ---------------------------------------------------

    fun row(rows: Map<String, Row>, id: String): Row = rows[id] ?: Row()

    /** How many services are wanted. Ids S5 no longer lists are not counted. */
    fun count(rows: Map<String, Row>): Int = SERVICES.count { rows[it.id]?.on == true }

    /** The project screen's chip. */
    fun chip(n: Int): String = "Wish list $n/${SERVICES.size}"

    /** The head's count. */
    fun total(n: Int): String = "$n of ${SERVICES.size} wanted"

    /** What the APPLIES TO choice reads as: the whole flat when no room is picked, else the rooms. */
    fun subLine(row: Row): String = if (row.rooms.isEmpty()) WHOLE_FLAT else row.rooms.joinToString(" · ")

    /** The row's second line: where, the note, the vendor when wanted; the service's detail when not. */
    fun rowLine(service: Service, row: Row): String =
        if (row.on) listOfNotNull(subLine(row), row.note.ifBlank { null },
            row.vendor.ifBlank { null }?.let { "vendor $it" }).joinToString(" · ")
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
     * the project not on S5's list, no stage, or a name that is not one of the five.
     */
    fun stageIndex(cache: QuoteStage.Cache?): Int? {
        if (cache == null || !cache.listed) return null
        val i = QuoteStage.indexOf(cache.stage.trim())
        return if (i < 0) null else i
    }

    /** Read-only at Survey; editable from Client wish list on, and when the stage is not read (FX16). */
    fun editable(stageIdx: Int?): Boolean = stageIdx == null || stageIdx >= 1

    /** From Vendor quoted on an edit warns (and still saves); the prototype only warns. */
    fun warnAfterQuote(stageIdx: Int?): Boolean = stageIdx != null && stageIdx >= 2

    /** The banners over the list, in the prototype's order. [warned]: changed after a vendor quoted, this visit. */
    fun banners(stageIdx: Int?, warned: Boolean): List<String> = buildList {
        if (stageIdx == 0) add(FINISH_SURVEY)
        if (stageIdx == null) add(UNREAD)
        if (warned && warnAfterQuote(stageIdx)) add(AFTER_QUOTE)
    }

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

    /** An edit kept on the phone at [now]: dirty until S5 takes it, and any old note gone. */
    fun edited(l: Local, rows: Map<String, Row>, now: String): Local =
        l.copy(rows = rows, dirty = true, savedAt = now, note = "")

    // ---- the sync -----------------------------------------------------------

    /** Every service's row, in S5's order, for the PUT: what is not sent is not wanted. */
    fun forPut(rows: Map<String, Row>): Map<String, Row> = SERVICES.associate { it.id to row(rows, it.id) }

    /**
     * After a 200 to the PUT of [sent]: S5's stamp becomes the base. When nothing was edited
     * while the PUT was out, S5's normalised rows are kept and the list is clean; an edit made
     * meanwhile stays, dirty, and goes next time on the new base.
     */
    fun afterPut(current: Local, sent: Map<String, Row>, s5Rows: Map<String, Row>?, at: String): Local =
        if (forPut(current.rows) == forPut(sent))
            current.copy(rows = s5Rows ?: current.rows, at = at, dirty = false)
        else current.copy(at = at, dirty = true)

    /**
     * A 409 because the base was stale (decision 2): S5 WINS. The phone takes S5's rows and
     * stamp, the dirty flag is cleared and the note says what happened.
     */
    fun afterConflict(current: Local, s5Rows: Map<String, Row>, at: String): Local =
        current.copy(rows = s5Rows, at = at, dirty = false, note = REPLACED)

    /** A GET on project open: a clean list takes S5's copy (staff edits on /projects); an unsent edit is never touched. */
    fun afterGet(current: Local, s5Rows: Map<String, Row>, at: String): Local =
        if (current.dirty) current else current.copy(rows = s5Rows, at = at)
}
