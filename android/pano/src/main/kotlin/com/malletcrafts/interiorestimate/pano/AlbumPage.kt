package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Album.Photo
import com.malletcrafts.interiorestimate.pano.Album.Source
import com.malletcrafts.interiorestimate.pano.Album.Viewer
import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.time.Instant

/**
 * THE PROJECT ALBUM ON THE PHONE, READ-ONLY -- the page's model, pure (port 61c slice K3, 9 Nov 2026; the plan is
 * DESIGN-port61c.md §18). As QuoteView.kt is the quote view's and Phases.kt the strip's: everything the album screen
 * draws is built here, once, from what the phone holds -- the album MIRROR (AlbumSync, S5's `GET …/album` in S5's own
 * shape), the ONE project-state store's copy (QuoteStage.Cache: the stage, its stamp and each work's state), the one
 * derived project line, and the phone's own captures -- through the one visibility rule ([Album.canSee]) and the one
 * filing rule ([Album.workOfCapture]). The screen (app/AlbumScreen.kt) has no rule of its own.
 *
 * The spec is the approved prototype v61c.2 (index61c2.html: the core window.ALB60 7070-7338, the page
 * window.__mcftAlbum60 7342-7722): the header (7405-7423), chapter 0 with its six quote groups and the unfiled photos
 * (7438-7451), the lock banner (7477), the sixteen works (7452-7474), the end line (7478), the cards (7428-7433) and
 * the viewer (7550-7572). K3 WRITES NOTHING: no state, no review, no change request, no shot, no scope; every
 * control the prototype draws that writes is absent here, in the slot K4 and K5 fill.
 *
 * Every text is a `tx.<key>`; a work, a state, a quote stage and a room are identity values shown through Masters; a
 * date goes through [Album.dayOf] / [Album.whenOf]. Names, captions, notes and remarks are data, drawn as they come.
 */
object AlbumPage {

    /** The separator the prototype joins every list and line part with. */
    const val SEP = " · " // text:value the prototype's separator, in both languages

    /** A log row's arrow between the work and the state it moved to (index61c2.html 7419). */
    private const val ARROW = " → " // text:value the prototype's arrow, in both languages

    /**
     * The face S5 holds that stands for an as-is capture on a card and in the viewer: S5 keeps six squared faces, not
     * the 360 the prototype draws (index61c2.html 7368 thumbBg(pano)), and the front is the face the phone already names a
     * capture by (Handover.filename(id, "front")).
     */
    const val FACE = "front" // text:value S5's surface name (SURFACES), never shown

    // ---- the phone's side ------------------------------------------------------------------------------------

    /**
     * One CaptureStore row as the album reads it, so :pano needs no Android type: [kind] is the store's `360` or
     * `Photo`; [capturedAt] is AsIsPath.capturedAt(createdAt, the phone's zone) -- byte for byte what the record sends as
     * `captured_at` -- the key S5's C3 matches a renamed capture by.
     */
    data class Local(
        val deviceId: String,
        val serverName: String?,
        val work: Int?,
        val stage: String?,
        val createdAt: Long,
        val capturedAt: String,
        val room: String,
        val kind: String,
    )

    /** Who is signed in: whoami's `user` and `full_name` (S5Session.who); both blank when nobody is (a legacy ERP key, or signed out). */
    data class Me(val user: String, val name: String)

    /** [by] is this phone's login, as S5 matches a party ([Album.partyKey]). */
    private fun isMe(by: String, me: Me): Boolean = by.isNotBlank() && Album.partyKey(by) == Album.partyKey(me.user)

    /**
     * THE ONE FILING (§18): each capture is ONE item, filed by ONE rule.
     * - A capture the phone holds is filed as the room screen files it, [Album.workOfCapture] of its row, whatever S5's
     *   copy says -- the phone is its copy's authority, and S5 converges on it (AS1's record, S5-R's re-file). It is
     *   matched to S5's capture by each name the capture has had: its device id, else its ERP name (MEST-PH-…, which S5
     *   keeps until C3 renames it), else C3's own key -- the same room and S5's `captured_at` equal to the phone's
     *   [Local.capturedAt] -- for a `legacy-…` or not-yet-renamed row that carries neither name (the K3 critic, finding
     *   8). Matched, S5's copy is NOT listed beside it; the item keeps S5's name for it ([Photo.capture]) and S5's `at`
     *   ([Photo.uploadedAt]), so its front face can be fetched when the phone's file has gone.
     * - A capture only S5 holds (taken on another phone, or one this phone deleted) is S5's, filed as S5 files it.
     * - A capture only the phone holds (not sent yet, a single photo, a project S5 does not list) is the phone's alone.
     * - S5's album photos are themselves.
     * WHO (the K3 critic, finding 7): CaptureStore keeps no "taken by", so a capture S5 holds is S5's uploader (`by`,
     * the record's updated_by), named [Me.name] when that is this login; only a capture not on S5 yet is credited to the
     * person signed in now (the prototype's `BY[id] || ME`). Unknown is blank, which a card shows as `Staff`.
     * In time order by instant, then by [key] (the prototype's sort, 7234).
     */
    fun compose(mirror: Album.View?, local: List<Local>, me: Me): List<Photo> {
        val s5 = mirror?.captures.orEmpty()
        val claimed = HashSet<Int>()
        val out = ArrayList<Photo>()
        for (l in local) {
            if (l.deviceId.isBlank()) continue
            val byId = s5.indices.filter { s5[it].id == l.deviceId }
            val byName = if (l.serverName.isNullOrBlank()) emptyList() else s5.indices.filter { s5[it].id == l.serverName }
            // C3's key: S5 renames the NEWEST twin of a room with this captured_at onto the record's id, so that one is
            // the capture's face; every twin is the same 360 and none is listed beside the phone's copy
            val byTaken = if (l.capturedAt.isBlank()) emptyList()
                else s5.indices.filter { s5[it].room == l.room && s5[it].at == l.capturedAt }
            claimed += byId + byName + byTaken
            val m = (byId.firstOrNull() ?: byName.firstOrNull() ?: byTaken.maxByOrNull { s5[it].uploadedAt })?.let { s5[it] }
            val by = if (m != null) m.by else me.user
            val name = if (m == null) me.name else if (isMe(m.by, me)) me.name else ""
            out += Photo(
                id = l.deviceId, work = Album.workOfCapture(l.work, l.stage), at = Instant.ofEpochMilli(l.createdAt).toString(),
                party = Album.PARTY_STAFF, room = l.room.ifBlank { null }, kind = if (l.kind == Album.PHOTO_360) Album.PHOTO_360 else Album.PHOTO,
                by = by, byName = name, source = Source.PHONE, capture = m?.id, uploadedAt = m?.uploadedAt.orEmpty(),
            )
        }
        s5.forEachIndexed { i, c ->
            if (i !in claimed) out += c.copy(source = Source.CAPTURE, byName = if (isMe(c.by, me)) me.name else c.byName)
        }
        mirror?.photos?.forEach { out += it }
        return out.sortedWith(compareBy<Photo>({ millis(it.at) }, { key(it) }))
    }

    private fun millis(at: String): Long = QuoteStage.instantOf(at.trim())?.toEpochMilli() ?: 0L

    /**
     * An item's key: its source, its room and its id -- so two `legacy-…` captures of two rooms uploaded in one second
     * (S5's 0006 ids collide across rooms) are two items, two cache files and two places in the walk.
     */
    fun key(p: Photo): String = p.source.name + "|" + p.room.orEmpty() + "|" + p.id

    // ---- who is looking, and the states ---------------------------------------------------------------------

    /** A project S5 does not list: made on this phone and never sent, or read as not listed (the line's LOCAL). */
    fun notOnS5(line: QuoteStage.ProjectLine): Boolean = line.kind == QuoteStage.LineKind.LOCAL

    /**
     * Who is looking, for VISIBILITY: S5's answer when the mirror is held ([Album.viewerOf]); for a project S5 does not
     * list, the Project Manager -- S5's albumRole answers `pm` to staff when there is no project row (D3, asis.js), and
     * the phone signs in staff only (the K3 critic, finding 11); else, with no mirror read yet, the Designer: both staff
     * roles see everything, and the Designer draws no PM-only line from a guess ([roleKnown] false).
     */
    fun viewer(mirror: Album.View?, notOnS5: Boolean): Viewer = when {
        mirror != null -> Album.viewerOf(mirror)
        notOnS5 -> Viewer(Album.PM)
        else -> Viewer(Album.DESIGNER)
    }

    /** Whether [viewer]'s role is S5's word (the mirror said it, or S5's rule answers it for a project with no row): `Viewing as` is drawn. */
    fun roleKnown(mirror: Album.View?, notOnS5: Boolean): Boolean = mirror != null || notOnS5

    /**
     * The works' states as the album draws them: the empty map for a project S5 does not list (nothing can be recorded
     * on a project S5 has no record of), else the store's copy -- null when it holds none in the read shape, which the
     * album reads as `not read yet`, never as Not started (the PL review's rule: never a count it has not read).
     */
    fun statesFor(line: QuoteStage.ProjectLine, cache: QuoteStage.Cache?): Map<Int, Album.WorkState>? =
        if (notOnS5(line)) emptyMap() else cache?.states

    // ---- the page ------------------------------------------------------------------------------------------

    /** One state row of the Work log: `<work> → <state>`, then `<when, IST> · <by_name, else by>`, then the remark. */
    data class LogRow(val seq: Long, val head: String, val sub: String, val remark: String)

    /**
     * The Work log toggle and its rows. [toggleable] false: the log is not read yet (`Work log · not read yet`), which
     * takes no tap. [empty] is `No work state recorded yet` when it is open with no row.
     */
    data class Log(val toggle: String, val toggleable: Boolean, val open: Boolean, val rows: List<LogRow>, val empty: String?)

    /** The header (index61c2.html 7405-7423), every line optional but [open] and [line]; the result line is K4's. */
    data class Header(
        val viewingAs: String?,
        val open: String,
        val toReview: String?,
        val changeRequests: String?,
        val line: String,
        val log: Log,
    )

    /**
     * One quote group of chapter 0: the stage's name, `now` on the current one, `since <d MMM>` beside a non-empty current
     * one when the store holds its stamp, the count (`n photos`) or, empty, `not reached yet` / `no photos`, and its cards.
     */
    data class Group(
        val stage: String,
        val name: String,
        val now: String?,
        val since: String?,
        val count: String?,
        val empty: String?,
        val items: List<String>,
    )

    /** Chapter 0, the quote journey. [count] is its header's line; [unfiled] the staff's `not filed to a work` group. */
    data class QuoteCh(
        val name: String,
        val count: String,
        val locked: Boolean,
        val open: Boolean,
        val now: Boolean,
        val aria: String,
        val sub: String,
        val groups: List<Group>,
        val unfiledHead: String?,
        val unfiledCount: String?,
        val unfiled: List<String>,
    )

    /**
     * One work's chapter. [state] is the store's identity value (null: not read), [stateText] as shown; [crChip] the live
     * change request's kind as the chip says it; [crNote] the raise line; [decided] the answered requests, newest first.
     */
    data class WorkCh(
        val n: Int,
        val id: Int,
        val name: String,
        val line: String,
        val locked: Boolean,
        val open: Boolean,
        val now: Boolean,
        val aria: String,
        val state: String?,
        val stateText: String,
        val stateAria: String,
        val crChip: String?,
        val crAria: String?,
        val payment: String,
        val paymentAria: String,
        val crNote: String?,
        val decided: List<String>,
        val items: List<String>,
        val empty: String?,
    )

    /** The end line: `The album closes when …`, or closed, with the day 140 went Done. */
    data class End(val text: String, val closed: Boolean, val day: String?)

    /**
     * One card (index61c2.html 7428-7433): who (the vendor glyph for a vendor's photo, [whoAria] `Staff` / `Vendor`), the
     * review badge never drawn to a client ([badge], [pending] for its amber ring), when, the filing, the room, the 360 tag.
     */
    data class CardLine(
        val key: String,
        val who: String,
        val vendor: Boolean,
        val whoAria: String,
        val badge: String?,
        val pending: Boolean,
        val whenText: String,
        val work: String,
        val room: String,
        val tag360: Boolean,
    )

    /** Everything the album screen draws, built once ([page]). [order] is the walk ‹ › follows, by [key]. */
    data class Page(
        val title: String,
        val sub: String,
        val viewer: Viewer,
        val header: Header,
        val more: String?,
        val empty: String?,
        val quote: QuoteCh,
        val banner: String?,
        val works: List<WorkCh>,
        val end: End,
        val order: List<String>,
        val items: Map<String, Photo>,
        val cards: Map<String, CardLine>,
        val groupOf: Map<String, String>,
    )

    /**
     * The page, from [items] ([compose]'s), the [mirror] (null: not read on this phone, or S5 does not list the project),
     * the store's [cache] and the project [line] -- the stage, its stamp and the states from the store, never from the
     * mirror file (AlbumWire.stored), the mirror's log and stage moves as history -- with this visit's taps: [toggled]
     * the chapters whose header was tapped (n -> open), [logOpen] the Work log.
     */
    fun page(
        tx: Tx, title: String, items: List<Photo>, mirror: Album.View?, cache: QuoteStage.Cache?, line: QuoteStage.ProjectLine,
        toggled: Map<Int, Boolean> = emptyMap(), logOpen: Boolean = false,
    ): Page {
        val local = notOnS5(line)
        val v = viewer(mirror, local)
        val states = statesFor(line, cache)
        val stage = if (local) null else cache?.stage
        val stageAt = if (local) null else cache?.at
        val ch = Album.chapters(items, mirror?.log.orEmpty(), mirror?.stages.orEmpty(), mirror?.scope, stage, stageAt, states, v, local)
        val visible = Album.visible(v, items)

        // the header
        val open = when {
            states == null -> tx.album_open_x(works = tx.wish_not_read_yet)
            ch.open.isEmpty() -> tx.album_no_open_work
            else -> tx.album_open_x(works = ch.open.joinToString(SEP) { Album.workName(tx, it) })
        }
        val pend = if (v.role == Album.PM) Album.pending(visible).size else 0
        val crs = ch.changeRequests.size
        val stateRows = mirror?.log.orEmpty().filter { it.kind == Album.KIND_STATE && it.work != null }.sortedByDescending { it.seq }
        val log = if (mirror == null && !local) Log(tx.album_work_log_unread, false, false, emptyList(), null)
            else Log(tx.album_work_log_x(stateRows.size), true, logOpen,
                if (!logOpen) emptyList() else stateRows.map { e ->
                    LogRow(e.seq, Album.workName(tx, e.work) + ARROW + Album.stateText(tx, e.value),
                        listOf(Album.whenOf(tx, e.at), e.byName.ifBlank { e.by }).filter { it.isNotBlank() }.joinToString(SEP), e.note)
                },
                if (logOpen && stateRows.isEmpty()) tx.album_no_work_log else null)
        val header = Header(
            viewingAs = if (roleKnown(mirror, local)) tx.album_viewing_as_x(role = Album.roleLabel(tx, v.role)) else null,
            open = open,
            toReview = if (pend > 0) tx.album_to_review_x(pend) else null,
            changeRequests = if (crs > 0) tx.album_change_requests_x(crs) else null,
            line = line.text,
            log = log,
        )

        // chapter 0: a group's `since` only where the store holds the stamp, as the prototype draws it only with quoteSince
        // (7440) -- never a bare `since ` for a stage S5 never timed, or one NAMED without its stamp (the K3 critic, finding 6)
        val q = ch.quote
        val sinceDay = cache?.takeIf { !local && !it.atUnknown }?.let { Album.dayOf(tx, it.at) }?.takeIf { it.isNotEmpty() }
        val groups = q.groups.map { g ->
            Group(
                stage = g.stage, name = Masters.quote(tx, g.stage),
                now = if (g.now) tx.album_now else null,
                since = if (g.now && g.items.isNotEmpty() && sinceDay != null) tx.app_since_x(stageSince = sinceDay) else null,
                count = if (g.items.isNotEmpty()) tx.pl_photo(g.items.size) else null,
                empty = if (g.items.isNotEmpty()) null else if (g.reached == false) tx.album_not_reached else tx.album_no_photos,
                items = g.items.map { key(it) },
            )
        }
        val now0 = q.groups.any { it.now }
        val open0 = !q.locked && (toggled[0] ?: (q.count > 0 || now0 || local))
        val quote = QuoteCh(
            name = tx.album_quote_journey,
            count = when { q.locked -> tx.album_not_your_work; q.count > 0 -> tx.pl_photo(q.count); else -> tx.album_no_photos_yet },
            locked = q.locked, open = open0, now = now0,
            aria = tx.album_aria_chapter_x(n = "0", name = tx.album_quote_journey),
            sub = tx.album_quote_journey_sub,
            groups = groups,
            unfiledHead = if (v.staff && q.unfiled.isNotEmpty()) tx.album_unfiled else null,
            unfiledCount = if (v.staff && q.unfiled.isNotEmpty()) tx.pl_photo(q.unfiled.size) else null,
            unfiled = if (v.staff) q.unfiled.map { key(it) } else emptyList(),
        )

        // chapters 1-16
        val works = ch.works.map { c ->
            val name = Album.workName(tx, c.id)
            val shown = c.state?.let { Album.stateText(tx, it) } ?: tx.wish_not_read_yet
            val lineW = when {
                c.locked -> if (c.execLocked) tx.album_locked_until_approved else tx.album_not_your_work
                c.count > 0 -> listOf(tx.pl_photo(c.count), tx.album_last_photo_x(day = Album.dayOf(tx, c.last))).joinToString(SEP)
                else -> tx.album_no_photos_yet
            }
            val now = c.state == Album.IN_PROGRESS
            val pay = Album.payOf(tx, c.id).joinToString(SEP)
            val chip = c.cr?.let { (it.crKind ?: Album.CrKind.ADD).word(tx) }
            WorkCh(
                n = c.n, id = c.id, name = name, line = lineW, locked = c.locked,
                open = !c.locked && (toggled[c.n] ?: (c.count > 0 || now)), now = now,
                aria = tx.album_aria_chapter_x(n = c.n.toString(), name = name),
                state = c.state, stateText = shown, stateAria = tx.album_aria_state_x(work = name, state = shown),
                crChip = chip, crAria = chip?.let { tx.album_aria_cr_kind_x(kind = it, name = name) },
                payment = tx.album_payment_x(list = pay), paymentAria = tx.album_aria_payment_x(work = name, list = pay),
                crNote = c.cr?.let { e ->
                    listOfNotNull(tx.album_cr_raised_x(day = Album.dayOf(tx, e.at)) + " " + e.byName.ifBlank { e.by }, e.note.ifBlank { null }).joinToString(SEP)
                },
                decided = c.decided.map { Album.decidedText(tx, it) },
                items = c.items.map { key(it) },
                empty = if (c.count == 0) tx.album_no_photos_yet else null,
            )
        }

        val end = if (ch.closed) End(tx.album_closed_cleaning, true, ch.closedAt?.let { Album.dayOf(tx, it) }?.ifEmpty { null })
            else End(tx.album_closes_cleaning, false, null)
        val groupOf = HashMap<String, String>()
        for (g in q.groups) for (it in g.items) groupOf[key(it)] = g.stage
        return Page(
            title = tx.album_title_x(title = title),
            sub = tx.album_sub_x(ch.total),
            viewer = v,
            header = header,
            more = mirror?.takeIf { it.more }?.let { tx.album_newest_x(plN = it.photos.size) },
            empty = if (ch.total == 0) tx.album_empty else null,
            quote = quote,
            banner = if (ch.execLocked) tx.album_exec_lock_banner else null,
            works = works,
            end = end,
            order = ch.order.map { key(it) },
            items = ch.order.associateBy { key(it) },
            cards = ch.order.associate { key(it) to card(tx, v, it) },
            groupOf = groupOf,
        )
    }

    /** An item's filing as the album draws it: 0 the quote journey, one of the sixteen, else not filed to a work. */
    private fun filing(p: Photo): Int? = if (p.work == Album.QUOTE_WORK || Album.isWork(p.work)) p.work else null

    /** Who a card names: a vendor's photo its vendor, else the name, else the login; unknown, `Staff` (never an empty pill). */
    private fun whoOf(tx: Tx, p: Photo): String =
        (if (p.party == Album.PARTY_VENDOR) p.vendor.orEmpty() else "").ifBlank { p.byName }.ifBlank { p.by }
            .ifBlank { if (p.party == Album.PARTY_VENDOR) tx.common_vendor else tx.album_staff }

    /** The review a card's badge and the viewer's line name, never to a client: null for a staff photo and for a client. */
    private fun reviewOf(viewer: Viewer, p: Photo): String? =
        if (viewer.role == Album.CLIENT || p.party != Album.PARTY_VENDOR) null else p.review

    /**
     * One card for [viewer]: who, the review badge (`awaiting review` / `Not for client`, never to a client), when -- the
     * day alone for a capture, as the prototype's `dateOnly` (7229), the day and time for an album photo -- the filing
     * ([Album.workName]), the room (Masters, else `Whole project`), and the `360` tag for a 360. No Before / After tag:
     * S5's photo row has no `pairOf` (photoView).
     */
    fun card(tx: Tx, viewer: Viewer, p: Photo): CardLine {
        val rv = reviewOf(viewer, p)
        return CardLine(
            key = key(p),
            who = whoOf(tx, p),
            vendor = p.party == Album.PARTY_VENDOR,
            whoAria = if (p.party == Album.PARTY_VENDOR) tx.common_vendor else tx.album_staff,
            badge = when (rv) { Album.REVIEW_PENDING -> tx.album_awaiting_review; Album.REVIEW_INTERNAL -> tx.album_not_for_client; else -> null },
            pending = rv == Album.REVIEW_PENDING,
            whenText = Album.whenOf(tx, p.at, dateOnly = p.source != Source.ALBUM),
            work = Album.workName(tx, filing(p)),
            room = p.room?.let { Masters.room(tx, it) } ?: tx.common_whole_project,
            tag360 = p.kind == Album.PHOTO_360,
        )
    }

    /** The viewer (index61c2.html 7550-7572) on one item; [prev] / [next] the keys either side of it in the walk. */
    data class Shown(
        val key: String,
        val title: String,
        val count: String,
        val card: CardLine,
        val chapterLine: String,
        val cr: String?,
        val payment: String?,
        val caption: String,
        val captionIsData: Boolean,
        val review: String?,
        val prev: String?,
        val next: String?,
    )

    /**
     * The viewer on [key], or null when the walk no longer holds it (it closes then, as the prototype's viewHTML does):
     * the title (the filing), `Photo i of N` over the walk, the card's who, when and room, the chapter line -- `Quote
     * journey · <stage>` naming the group the photo SITS in (the same [Album.quoteAt] that grouped it), or the work -- a
     * live change request on its work, its payment milestones, the caption else `No caption`, and, never to a client, the
     * review line: `awaiting review`, `Accepted by <name> · <when>` or `Not for client · <name> · <when>`.
     */
    fun shown(tx: Tx, page: Page, key: String): Shown? {
        val i = page.order.indexOf(key)
        if (i < 0) return null
        val p = page.items[key] ?: return null
        val card = page.cards[key] ?: card(tx, page.viewer, p)
        val w = filing(p)
        val work = page.works.firstOrNull { it.id == w }
        val chapterLine = if (w == Album.QUOTE_WORK) listOf(tx.album_quote_journey, Masters.quote(tx, page.groupOf[key] ?: QuoteStage.STAGES[0])).joinToString(SEP)
            else Album.workName(tx, w)
        val by = (p.reviewByName ?: p.reviewBy).orEmpty()
        val at = Album.whenOf(tx, p.reviewAt)
        val review = when (reviewOf(page.viewer, p)) {
            Album.REVIEW_PENDING -> tx.album_awaiting_review
            Album.REVIEW_ACCEPTED -> listOf(tx.album_accepted_by + " " + by, at).filter { it.isNotBlank() }.joinToString(SEP)
            Album.REVIEW_INTERNAL -> listOf(tx.album_not_for_client, by, at).filter { it.isNotBlank() }.joinToString(SEP)
            else -> null
        }
        return Shown(
            key = key,
            title = Album.workName(tx, w),
            count = tx.album_photo_x_of_x(i = (i + 1).toString(), n = page.order.size),
            card = card,
            chapterLine = chapterLine,
            cr = work?.crChip?.let { listOf(tx.album_change_request, it).joinToString(SEP) },
            payment = work?.let { tx.album_payment_x(list = Album.payOf(tx, it.id).joinToString(SEP)) },
            caption = p.caption.ifBlank { tx.album_no_caption },
            captionIsData = p.caption.isNotBlank(),
            review = review,
            prev = page.order.getOrNull(i - 1),
            next = page.order.getOrNull(i + 1),
        )
    }

    // ---- the tile --------------------------------------------------------------------------------------------

    /** The tile in the rooms grid: the count of photos this person may see, `Album`, and its TalkBack label. */
    data class Tile(val count: Int, val label: String, val aria: String)

    /** The Album tile (index61c2.html 7385-7387): the count over [Album.canSee] for the person looking. */
    fun tile(tx: Tx, title: String, mirror: Album.View?, line: QuoteStage.ProjectLine, items: List<Photo>): Tile {
        val n = Album.visible(viewer(mirror, notOnS5(line)), items).size
        return Tile(n, tx.album_album, tx.album_aria_tile_x(title = title, plN = n))
    }

    // ---- the bytes: where each card's picture is ---------------------------------------------------------------

    /**
     * Where an item's picture is: [OnPhone] the phone's own file (the 360 or the single photo, no network); [Face] the
     * front face of a capture S5 holds, at S5's [Face.version] (its `at`, so a re-squared face is a new file); [AlbumJpeg]
     * an album photo's JPEG; [None] the plain block. Named so nothing clashes with Compose's Image or material3's Card
     * in :app (the K3 critic, finding 12).
     */
    sealed interface Pic {
        data class OnPhone(val deviceId: String) : Pic
        data class Face(val room: String, val capture: String, val version: String) : Pic
        data class AlbumJpeg(val id: String) : Pic
        data object None : Pic
    }

    /**
     * ONE rule about where a capture's bytes are, not a second answer to anything: the phone's file when it is there
     * ([localFile]); else, for a capture S5 holds, its front face from S5; an album photo's own JPEG; else none.
     */
    fun pic(p: Photo, localFile: Boolean): Pic = when (p.source) {
        Source.PHONE -> if (localFile) Pic.OnPhone(p.id) else faceOf(p.room, p.capture, p.uploadedAt)
        Source.CAPTURE -> faceOf(p.room, p.id, p.uploadedAt)
        Source.ALBUM -> if (p.bytes > 0) Pic.AlbumJpeg(p.id) else Pic.None
    }

    private fun faceOf(room: String?, capture: String?, version: String): Pic =
        if (room.isNullOrBlank() || capture.isNullOrBlank()) Pic.None else Pic.Face(room, capture, version)

    /** An album photo's JPEG, `GET …/album/photos/<id>/img`: AlbumQueue.requestOf's own segments, each encoded once by the caller. */
    fun photoImage(project: String, id: String): List<String> = listOf(project, "album", "photos", id, "img")

    /** A capture's front face, `GET …/<room>/c/<capture>/img/front` (0006's explicit capture route), each segment encoded once by the caller. */
    fun faceImage(project: String, room: String, capture: String): List<String> = AsIsPath.image(project, room, capture, FACE)
}
