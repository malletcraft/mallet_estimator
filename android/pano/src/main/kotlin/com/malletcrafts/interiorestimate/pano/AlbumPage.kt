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
 * the viewer (7550-7572). K3 wrote nothing; since port 61c slice K4 (DESIGN-port61c.md §19) the Project Manager's
 * controls are drawn in the slots K3 left -- the state chip's ▾ and its sheet, Accept / Not for client under a card
 * and in the viewer, Raise change request and Record the client's answer with their sheets, the result line -- each a
 * tap that [AlbumWrite] turns into exactly the op S5 takes, laid over what the phone holds at once ([model]'s
 * overlay: this login's queue on the store's states, the mirror's log, its scope and its photos). K5's shot and K6's
 * vendor upload stay absent.
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

    /**
     * Who is signed in: whoami's `user` and `full_name` (S5Session.who); both blank when nobody is (a legacy ERP key, or
     * signed out). [signedIn] is S5Session.signedIn: a refused renewal signs the phone out of S5 but keeps `who`
     * (S5Session.relogin drops the password and the session only), so the user alone does not say a write can be sent
     * under it (the K4 critic, finding 8).
     */
    data class Me(val user: String, val name: String, val signedIn: Boolean = true)

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
     * the record's updated_by), named [Me.name] when that is this login, else by S5's own `by_name` for it -- which S5
     * does not send on `captures[]` yet (an S5 slice; until it lands the card shows the login, the K3 review, finding
     * 3); only a capture not on S5 yet is credited to the person signed in now (the prototype's `BY[id] || ME`).
     * Unknown is blank, and the card's pill is then empty, as the prototype's `esc(it.who||'')` draws it (finding 2).
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
            val name = if (m == null) me.name else if (isMe(m.by, me)) me.name else m.byName
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
     * WHO WRITES (port 61c slice K4): the Project Manager AS S5 SAYS IT -- the mirror is held and its `role` is `pm`,
     * which is D3 (every staff member is PM while the project names none) and "only the named PM" once one is named
     * (Amit, 27 Oct 2026) -- on a project S5 lists, signed in to S5 ([Me.signedIn]) as a login whoami named ([Me.user]):
     * every album write is attributed to whoami's user, and the outbox is the login's. Every control reads this one
     * test; anybody else sees the K3 album (the prototype's drawing for a person it offers no control to).
     */
    fun canWrite(mirror: Album.View?, notOnS5: Boolean, me: Me): Boolean =
        mirror != null && mirror.role == Album.PM && !notOnS5 && me.user.isNotBlank() && me.signedIn

    /**
     * The works' states as the album draws them: the empty map for a project S5 does not list (nothing can be recorded
     * on a project S5 has no record of), else the store's copy -- and the empty map when it holds none in the read
     * shape, which the album draws as the prototype's stateOf does with no state row: Not started, and `No work open
     * yet` (index61c2.html 7218-7219). K3 drew `not read yet` there, a state the prototype does not have; it is Amit's
     * to approve on the prototype first (the K3 review, finding 2), so the phone draws the prototype's until then.
     */
    fun statesFor(line: QuoteStage.ProjectLine, cache: QuoteStage.Cache?): Map<Int, Album.WorkState> =
        if (notOnS5(line)) emptyMap() else cache?.states.orEmpty()

    // ---- the page ------------------------------------------------------------------------------------------

    /** One state row of the Work log: `<work> → <state>`, then `<when, IST> · <by_name, else by>`, then the remark. */
    data class LogRow(val seq: Long, val head: String, val sub: String, val remark: String)

    /**
     * The Work log toggle `Work log (N)` and its rows, always a toggle, as the prototype draws it (index61c2.html 7418;
     * K3's `Work log · not read yet` with no tap was a state the prototype does not have: the K3 review, finding 2).
     * [empty] is `No work state recorded yet` when it is open with no row.
     */
    data class Log(val toggle: String, val open: Boolean, val rows: List<LogRow>, val empty: String?)

    /**
     * The header (index61c2.html 7405-7423), every line optional but [open] and [line]. [result] is the line a review tap
     * leaves (`.alb59res`, 7413-7416, after the project line and before the Work log): set by [page] from [resultText].
     */
    data class Header(
        val viewingAs: String?,
        val open: String,
        val toReview: String?,
        val changeRequests: String?,
        val line: String,
        val log: Log,
        val result: String? = null,
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
     * One work's chapter. [state] is the store's identity value, [stateText] as shown; [crChip] the live
     * change request's kind as the chip says it; [crNote] the raise line; [decided] the answered requests, newest first.
     * K4's controls, each null or false for anyone but the Project Manager ([canWrite]): [stateTap] the chip opens the
     * state sheet (drawn `<state> ▾`, index61c2.html 7459); [raise] `Raise change request` (7469: at Client approved,
     * with a baseline, no live request); [answer] `Record the client's answer` (7468: at Client approved, on a live one).
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
        val state: String,
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
        val stateTap: Boolean = false,
        val raise: String? = null,
        val answer: String? = null,
    )

    /** The end line: `The album closes when …`, or closed, with the day 140 went Done. */
    data class End(val text: String, val closed: Boolean, val day: String?)

    /**
     * One card (index61c2.html 7428-7433): who (the vendor glyph for a vendor's photo, [whoAria] `Staff` / `Vendor`), the
     * review badge never drawn to a client ([badge], [pending] for its amber ring), when, the filing, the room, the 360 tag.
     * [review] (port 61c slice K4): the Project Manager's Accept / Not for client under it (7434-7437, `.alb60cw` and
     * `.alb60rb`), with [accept], [keep] and the group's TalkBack label [reviewGroup] (`Vendor review`, the prototype's
     * aria-label) -- only on a vendor's photo awaiting review, the overlay included.
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
        val review: Boolean = false,
        val accept: String? = null,
        val keep: String? = null,
        val reviewGroup: String? = null,
    )

    /** Everything the album screen draws, built once ([page]). [order] is the walk ‹ › follows, by [key]; [canWrite] whether the controls are drawn. */
    data class Page(
        val title: String,
        val sub: String,
        val viewer: Viewer,
        val header: Header,
        val empty: String?,
        val quote: QuoteCh,
        val banner: String?,
        val works: List<WorkCh>,
        val end: End,
        val order: List<String>,
        val items: Map<String, Photo>,
        val cards: Map<String, CardLine>,
        val groupOf: Map<String, String>,
        val canWrite: Boolean = false,
    )

    /**
     * The page's DATA, built once per read and kept while the visit taps (the K3 review, finding 4): [base] is the page
     * with no tap -- every chapter at its default, the Work log folded -- and [logRows] every row the log shows when it
     * is opened. A chapter or Work log tap is [page] (model, taps), which only sets each chapter's `open` and the log's
     * rows on [base]: the chapters, the cards, the walk and the groups are never rebuilt for a tap.
     */
    class Model internal constructor(val base: Page, internal val logRows: List<LogRow>, internal val noLog: String, internal val write: Write? = null)

    /**
     * What [AlbumWrite] reads to turn a tap into an op (port 61c slice K4), kept on the [Model] only when the person can
     * write ([canWrite]): the [viewer]; the [mirror], whose bases every op carries (never a PENDING row's); and the
     * OVERLAID [states], [log], [scope] and [items] -- what the screen shows, the queue laid over the store and the
     * mirror -- which every gate checks, so a chain of taps offline is exactly the chain S5 will take. [exec] is the
     * chapters' lock released; [stage] the store's quote stage.
     */
    class Write internal constructor(
        internal val viewer: Viewer,
        internal val mirror: Album.View,
        internal val exec: Boolean,
        internal val stage: String?,
        internal val states: Map<Int, Album.WorkState>,
        internal val log: List<Album.Entry>,
        internal val scope: List<Int>?,
        internal val items: Map<String, Photo>,
    )

    /**
     * The page, from [items] ([compose]'s), the [mirror] (null: not read on this phone, or S5 does not list the project),
     * the store's [cache] and the project [line] -- the stage, its stamp and the states from the store, never from the
     * mirror file (AlbumWire.stored), the mirror's log and stage moves as history -- with this visit's taps: [toggled]
     * the chapters whose header was tapped (n -> open), [logOpen] the Work log. [model] then [page] (model, taps), in one.
     */
    fun page(
        tx: Tx, title: String, items: List<Photo>, mirror: Album.View?, cache: QuoteStage.Cache?, line: QuoteStage.ProjectLine,
        toggled: Map<Int, Boolean> = emptyMap(), logOpen: Boolean = false, queue: List<Album.Op> = emptyList(), me: Me = Me("", ""),
    ): Page = page(model(tx, title, items, mirror, cache, line, queue, me), toggled, logOpen)

    /**
     * [model]'s page with this visit's taps: a chapter's `open` is its tap where it has one and is not locked (a locked
     * chapter takes no tap), else its default; the Work log's rows when [logOpen]; the header's [result] line
     * ([resultText]). Everything else is [Model.base]'s own, the same objects.
     */
    fun page(m: Model, toggled: Map<Int, Boolean> = emptyMap(), logOpen: Boolean = false, result: String? = null): Page {
        val b = m.base
        if (toggled.isEmpty() && !logOpen && result == null) return b
        val log = b.header.log.copy(open = logOpen, rows = if (logOpen) m.logRows else emptyList(),
            empty = if (logOpen && m.logRows.isEmpty()) m.noLog else null)
        return b.copy(
            header = b.header.copy(log = log, result = result),
            quote = toggled[0]?.let { t -> b.quote.copy(open = !b.quote.locked && t) } ?: b.quote,
            works = b.works.map { w -> toggled[w.n]?.let { t -> w.copy(open = !w.locked && t) } ?: w },
        )
    }

    /**
     * The page's data with no tap of this visit ([Model]); built where the mirror, the store, the phone's captures or
     * this login's [queue] change. THE OVERLAY (port 61c slice K4; the prototype's writers write and draw in one step,
     * index61c2.html 7211, 7220-7221): [queue] -- this login's ops for the project, in queue order -- is laid over what
     * the phone holds, every reader from the same overlay: the works' states ([Album.withQueued] over the store's,
     * carrying each tap's time), the log ([Album.provisional] rows under S5's own, S5's winning for the same id:
     * [Album.mergedLog]) -- so the live change request, `Change requests: N` and the Work log -- the scope
     * ([Album.scopeWithQueued]) and each vendor photo's review (the newest queued one, by [me], at its tap's time). A
     * project S5 does not list takes no overlay (nothing can be queued for it). [me] also decides [canWrite].
     */
    fun model(
        tx: Tx, title: String, items: List<Photo>, mirror: Album.View?, cache: QuoteStage.Cache?, line: QuoteStage.ProjectLine,
        queue: List<Album.Op> = emptyList(), me: Me = Me("", ""),
    ): Model {
        val local = notOnS5(line)
        val v = viewer(mirror, local)
        val queued = if (local) emptyList() else queue
        val write = canWrite(mirror, local, me)
        val states = Album.withQueued(statesFor(line, cache), queued)
        val stage = if (local) null else cache?.stage
        val stageAt = if (local) null else cache?.at
        val history = Album.mergedLog(provisionals(queued, v.role, me), mirror?.log.orEmpty())
        // the scope's overlay needs the scope it amends: with no mirror there is no baseline to amend
        val scope = mirror?.let { Album.scopeWithQueued(it.scope, it.scopeAt, queued).first }
        val shownItems = reviewed(items, queued, me)
        val ch = Album.chapters(shownItems, history, mirror?.stages.orEmpty(), scope, stage, stageAt, states, v, local)
        val visible = Album.visible(v, shownItems)
        val approvedStage = Album.approved(stage)

        // the header
        val open = if (ch.open.isEmpty()) tx.album_no_open_work else tx.album_open_x(works = ch.open.joinToString(SEP) { Album.workName(tx, it) })
        val pend = if (v.role == Album.PM) Album.pending(visible).size else 0
        val crs = ch.changeRequests.size
        val stateRows = history.filter { it.kind == Album.KIND_STATE && it.work != null }.sortedByDescending { it.seq }
        val logRows = stateRows.map { e ->
            LogRow(e.seq, Album.workName(tx, e.work) + ARROW + Album.stateText(tx, e.value),
                listOf(Album.whenOf(tx, e.at), e.byName.ifBlank { e.by }).filter { it.isNotBlank() }.joinToString(SEP), e.note)
        }
        val log = Log(tx.album_work_log_x(stateRows.size), false, emptyList(), null)
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
        val open0 = !q.locked && (q.count > 0 || now0 || local)
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
            val shown = Album.stateText(tx, c.state)
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
                open = !c.locked && (c.count > 0 || now), now = now,
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
                stateTap = write && !c.locked,
                raise = if (write && !c.locked && approvedStage && scope != null && c.cr == null) tx.album_cr_raise else null,
                answer = if (write && !c.locked && approvedStage && c.cr != null) tx.album_cr_answer else null,
            )
        }

        val end = if (ch.closed) End(tx.album_closed_cleaning, true, ch.closedAt?.let { Album.dayOf(tx, it) }?.ifEmpty { null })
            else End(tx.album_closes_cleaning, false, null)
        val groupOf = HashMap<String, String>()
        for (g in q.groups) for (it in g.items) groupOf[key(it)] = g.stage
        val byKey = ch.order.associateBy { key(it) }
        return Model(Page(
            title = tx.album_title_x(title = title),
            sub = tx.album_sub_x(ch.total),
            viewer = v,
            header = header,
            empty = if (ch.total == 0) tx.album_empty else null,
            quote = quote,
            banner = if (ch.execLocked) tx.album_exec_lock_banner else null,
            works = works,
            end = end,
            order = ch.order.map { key(it) },
            items = byKey,
            cards = ch.order.associate { key(it) to card(tx, v, it, write) },
            groupOf = groupOf,
            canWrite = write,
        ), logRows, tx.album_no_work_log,
            if (!write) null else Write(v, mirror!!, !ch.execLocked, stage, states, history, scope, byKey))
    }

    /** [queue]'s log ops as the rows they will become ([Album.provisional]: PENDING_SEQ + each one's place in the queue, by [me], at its tap's time). */
    private fun provisionals(queue: List<Album.Op>, role: String, me: Me): List<Album.Entry> =
        queue.withIndex().filter { it.value.kind in LOG_OPS }.map { (i, op) -> Album.provisional(op, i, role, me.user, me.name, op.at) }

    private val LOG_OPS = setOf(Album.KIND_STATE, Album.KIND_CR_RAISE, Album.KIND_CR_CLEAR)

    /** [items] with each vendor photo's newest queued review laid over it: its state, by [me], at its tap's time. */
    private fun reviewed(items: List<Photo>, queue: List<Album.Op>, me: Me): List<Photo> {
        val rv = queue.filter { it.kind == Album.OP_REVIEW && it.photo.isNotBlank() }.associateBy { it.photo }
        if (rv.isEmpty()) return items
        return items.map { p ->
            val op = if (p.source == Source.ALBUM && p.party == Album.PARTY_VENDOR) rv[p.id] else null
            if (op == null) p else p.copy(review = op.value, reviewBy = me.user.ifBlank { null }, reviewByName = me.name.ifBlank { null },
                reviewAt = op.at.ifBlank { null })
        }
    }

    /** An item's filing as the album draws it: 0 the quote journey, one of the sixteen, else not filed to a work. */
    private fun filing(p: Photo): Int? = if (p.work == Album.QUOTE_WORK || Album.isWork(p.work)) p.work else null

    /**
     * Who a card names: a vendor's photo its vendor, else the name, else the login; unknown, blank -- the prototype's
     * `esc(it.who||'')` (index61c2.html 7366), an empty pill beside its glyph, whose TalkBack label still says Staff or
     * Vendor ([CardLine.whoAria]). K3 wrote `Staff` / `Vendor` into it, a text the prototype does not draw there (the K3
     * review, finding 2).
     */
    private fun whoOf(p: Photo): String =
        (if (p.party == Album.PARTY_VENDOR) p.vendor.orEmpty() else "").ifBlank { p.byName }.ifBlank { p.by }

    /** The review a card's badge and the viewer's line name, never to a client: null for a staff photo and for a client. */
    private fun reviewOf(viewer: Viewer, p: Photo): String? =
        if (viewer.role == Album.CLIENT || p.party != Album.PARTY_VENDOR) null else p.review

    /**
     * One card for [viewer]: who, the review badge (`awaiting review` / `Not for client`, never to a client), when -- the
     * day alone for a capture, as the prototype's `dateOnly` (7229), the day and time for an album photo -- the filing
     * ([Album.workName]), the room (Masters, else `Whole project`), and the `360` tag for a 360. No Before / After tag:
     * S5's photo row has no `pairOf` (photoView).
     */
    fun card(tx: Tx, viewer: Viewer, p: Photo, canWrite: Boolean = false): CardLine {
        val rv = reviewOf(viewer, p)
        val review = canWrite && rv == Album.REVIEW_PENDING
        return CardLine(
            key = key(p),
            who = whoOf(p),
            vendor = p.party == Album.PARTY_VENDOR,
            whoAria = if (p.party == Album.PARTY_VENDOR) tx.common_vendor else tx.album_staff,
            badge = when (rv) { Album.REVIEW_PENDING -> tx.album_awaiting_review; Album.REVIEW_INTERNAL -> tx.album_not_for_client; else -> null },
            pending = rv == Album.REVIEW_PENDING,
            whenText = Album.whenOf(tx, p.at, dateOnly = p.source != Source.ALBUM),
            work = Album.workName(tx, filing(p)),
            room = p.room?.let { Masters.room(tx, it) } ?: tx.common_whole_project,
            tag360 = p.kind == Album.PHOTO_360,
            review = review,
            accept = if (review) tx.album_accept else null,
            keep = if (review) tx.album_not_for_client else null,
            reviewGroup = if (review) tx.life_vrev else null,
        )
    }

    /**
     * The viewer (index61c2.html 7550-7572) on one item; [prev] / [next] the keys either side of it in the walk. [accept]
     * and [keep] (port 61c slice K4, `.alb60rb2`, 7563-7564) are the Project Manager's buttons after the review line on a
     * vendor's photo: `Accept` unless it is accepted, `Not for client` unless it is kept -- the other state, never the one
     * the photo has.
     */
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
        val accept: String? = null,
        val keep: String? = null,
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
        val rv = reviewOf(page.viewer, p)
        val review = when (rv) {
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
            accept = if (page.canWrite && rv != null && rv != Album.REVIEW_ACCEPTED) tx.album_accept else null,
            keep = if (page.canWrite && rv != null && rv != Album.REVIEW_INTERNAL) tx.album_not_for_client else null,
        )
    }

    // ---- the Project Manager's sheets and the result line (port 61c slice K4) ----------------------------------

    /** Which sheet this visit has open (index61c2.html memOf's `sheet`), held by MainActivity above the search overlay. */
    sealed interface SheetFor {
        val work: Int

        /** The state sheet of a work (`{state: id}`). */
        data class State(override val work: Int) : SheetFor

        /** The raise sheet of a work (`{cr: id}`). */
        data class Raise(override val work: Int) : SheetFor

        /** The client's-answer sheet of a work's live request (`{cra: id}`). */
        data class Answer(override val work: Int) : SheetFor
    }

    /** A sheet's model, every text a `tx.<key>`; built by [sheet], drawn by app/AlbumSheets.kt. */
    sealed interface Sheet {
        val work: Int
    }

    /** One of the five states on the state sheet: S5's value, as shown, and whether it is the work's state now. */
    data class StateRow(val state: String, val text: String, val selected: Boolean)

    /**
     * THE STATE SHEET (index61c2.html stateSheet, 7543-7548): `State of <work>`, the five states with the current one
     * selected; leaving Done only, the field [reasonLabel] (`Why change a Done work? (required)`) and, after a pick with
     * it blank, the line [need]; the foot [footer] (`Only the Project Manager sets a work's state`).
     */
    data class StateSheet(
        override val work: Int,
        val title: String,
        val rows: List<StateRow>,
        val reasonLabel: String?,
        val need: String,
        val footer: String,
    ) : Sheet

    /** One kind on the raise sheet: the kind, its word (`crkind.*`) and its line (`album.cr_kind_*_sub`). */
    data class KindRow(val kind: Album.CrKind, val head: String, val sub: String)

    /**
     * THE RAISE SHEET (index61c2.html crSheet, 7520-7528): `Change request · <work>`, the kinds the work allows -- in the
     * approved scope remove and change, outside it add ([AlbumWrite.kinds]) -- the first selected, the field [noteLabel]
     * (`A note the client reads`), the button [button].
     */
    data class RaiseSheet(
        override val work: Int,
        val title: String,
        val kinds: List<KindRow>,
        val noteLabel: String,
        val button: String,
    ) : Sheet

    /** One of the client's three answers on the answer sheet. */
    data class OutcomeRow(val outcome: Album.CrOutcome, val text: String)

    /**
     * THE ANSWER SHEET (index61c2.html crAnswerSheet, 7529-7539): `Record the client's answer`; the [head] line (`<work>
     * · Change request · <kind> · <note>`, the note as typed); the three outcomes, approved selected; the field
     * [dateLabel] prefilled with [today] (the IST day, YYYY-MM-DD); the field [howLabel] over [help]; the [note]; the
     * [button]. A date that is not a day is refused in S5's words (`refuse.album.client_at_day`, [Album.Refusal.text]).
     */
    data class AnswerSheet(
        override val work: Int,
        val title: String,
        val head: String,
        val outcomes: List<OutcomeRow>,
        val dateLabel: String,
        val today: String,
        val howLabel: String,
        val help: String,
        val note: String,
        val button: String,
    ) : Sheet

    /**
     * The model of the open sheet [s], or NULL when its subject has gone -- and the screen then closes it, as the
     * prototype's ov() and its sheets close themselves (the K4 critic, finding 6): every sheet once the person cannot
     * write ([canWrite]: 7578 `!pm()`), the raise and the answer sheets once the stage is not Client approved (7520,
     * 7529), the answer sheet once the overlaid log holds no live request on the work (7529).
     */
    fun sheet(tx: Tx, m: Model, s: SheetFor, now: Instant): Sheet? = when (s) {
        is SheetFor.State -> stateSheet(tx, m, s.work)
        is SheetFor.Raise -> raiseSheet(tx, m, s.work)
        is SheetFor.Answer -> answerSheet(tx, m, s.work, now)
    }

    /** The state sheet of [work] ([StateSheet]); null when the person cannot write or [work] is not one of the sixteen. */
    fun stateSheet(tx: Tx, m: Model, work: Int): StateSheet? {
        val w = m.write ?: return null
        if (!Album.isWork(work)) return null
        val cur = Album.stateName(w.states[work]?.state)
        return StateSheet(
            work = work,
            title = tx.album_state_of_x(work = Album.workName(tx, work)),
            rows = Album.STATES.map { StateRow(it, Album.stateText(tx, it), it == cur) },
            reasonLabel = if (Album.remarkNeeded(cur)) tx.album_done_reason_label else null,
            need = Album.Refusal.DONE_REASON_NEED.text(tx),
            footer = tx.album_state_pm_only,
        )
    }

    /** The raise sheet of [work] ([RaiseSheet]); null when the person cannot write or the stage is not Client approved. */
    fun raiseSheet(tx: Tx, m: Model, work: Int): RaiseSheet? {
        val w = m.write ?: return null
        if (!Album.isWork(work) || !Album.approved(w.stage)) return null
        return RaiseSheet(
            work = work,
            title = listOf(tx.album_change_request, Album.workName(tx, work)).joinToString(SEP),
            kinds = AlbumWrite.kinds(m, work).map { KindRow(it, it.word(tx), it.sub(tx)) },
            noteLabel = tx.album_cr_note_client,
            button = tx.album_cr_raise,
        )
    }

    /** The answer sheet of [work]'s live request ([AnswerSheet]); null when the person cannot write, the stage is not Client approved, or no request is live. */
    fun answerSheet(tx: Tx, m: Model, work: Int, now: Instant): AnswerSheet? {
        val w = m.write ?: return null
        if (!Album.isWork(work) || !Album.approved(w.stage)) return null
        val cr = Album.crOf(w.log, work) ?: return null
        val kind = cr.crKind ?: Album.CrKind.ADD
        return AnswerSheet(
            work = work,
            title = tx.album_cr_answer,
            head = listOfNotNull(Album.workName(tx, work), listOf(tx.album_change_request, kind.word(tx)).joinToString(SEP), cr.note.ifBlank { null })
                .joinToString(SEP),
            outcomes = Album.CrOutcome.entries.map { OutcomeRow(it, it.text(tx)) },
            dateLabel = tx.album_cr_client_at,
            today = AlbumWrite.today(now),
            howLabel = tx.album_cr_how,
            help = tx.album_cr_how_eg,
            note = tx.album_cr_answer_note,
            button = tx.album_cr_answer,
        )
    }

    /** This visit's last review tap (the prototype's `m.res`): the state it set and the photo's id. */
    data class Res(val state: String, val photo: String)

    /**
     * THE RESULT LINE (index61c2.html 7413-7416): `Accepted · the client sees it now` or `Kept from the client` after this
     * visit's last review tap [res], while the photo shows that review -- queued (the overlay) or taken (S5's row) -- and
     * none once it does not: S5 refused it or won over it (the K4 critic, finding 4: judged by what the photo shows, never
     * by the outbox's `dropped`, which a refusal of an op S5 already held would put it in). Null with no tap, or a photo
     * the album no longer holds.
     */
    fun resultText(tx: Tx, m: Model, res: Res?): String? {
        if (res == null) return null
        val p = m.base.items.values.firstOrNull { it.source == Source.ALBUM && it.id == res.photo } ?: return null
        if (p.review != res.state) return null
        return when (res.state) {
            Album.REVIEW_ACCEPTED -> tx.album_accepted_res
            Album.REVIEW_INTERNAL -> tx.album_kept_res
            else -> null
        }
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

    // ---- the cache's trim: what a full folder lets go (app/AlbumImages.kt keeps the files) -----------------------

    /** The JPEG cache's cap, and what a trim brings it down to (app/AlbumImages.kt, `cacheDir/album-img`). */
    const val CACHE_CAP_BYTES = 256L * 1024 * 1024
    const val CACHE_TRIM_TO = 192L * 1024 * 1024

    /** A download's temp file: written, then renamed onto its name, inside one lock per file. */
    const val PART = ".part" // text:value a temp file's suffix, never shown

    /** A temp file this old is no download in flight (one lives only between its write and its rename) but an orphan. */
    const val STALE_PART_MS = 60_000L

    /** One file of the cache as [trimmed] reads it: its name, its size and its mtime (epoch ms, which a draw moves). */
    data class CacheFile(val name: String, val bytes: Long, val mtime: Long)

    /**
     * The names a trim deletes from [files] at [now] (the K3 review, finding 6): every [PART] older than [STALE_PART_MS]
     * -- the orphan of a process killed between the write and the rename, or of a write that threw -- which K3's trim
     * neither counted nor ever removed, so up to 8 MB each sat outside the cap until sign-out; never a younger one,
     * which is a download in flight and is not counted either. Then, while the rest is over [CACHE_CAP_BYTES], the
     * least recently drawn files (by mtime) until it is under [CACHE_TRIM_TO].
     */
    fun trimmed(files: List<CacheFile>, now: Long): List<String> {
        val out = ArrayList<String>()
        val kept = ArrayList<CacheFile>()
        for (f in files) {
            if (!f.name.endsWith(PART)) kept += f
            else if (now - f.mtime > STALE_PART_MS) out += f.name
        }
        var total = kept.sumOf { it.bytes }
        if (total <= CACHE_CAP_BYTES) return out
        for (f in kept.sortedBy { it.mtime }) {
            if (total <= CACHE_TRIM_TO) break
            out += f.name
            total -= f.bytes
        }
        return out
    }
}
