package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Album.Answer
import com.malletcrafts.interiorestimate.pano.Album.BytesPatch
import com.malletcrafts.interiorestimate.pano.Album.Entry
import com.malletcrafts.interiorestimate.pano.Album.Op
import com.malletcrafts.interiorestimate.pano.Album.Photo
import com.malletcrafts.interiorestimate.pano.Album.ReviewPatch
import com.malletcrafts.interiorestimate.pano.Album.View
import com.malletcrafts.interiorestimate.pano.Album.Work

/**
 * S5'S ALBUM ANSWERS, READ (port 61c slice AQ, 3 Nov 2026): the album GET ([view]) and the answer to every queued
 * op ([reply]), in the shapes S5 QUOTE sends on stage (mcft-interior-estimate 9a51733, functions/_lib/asis.js:
 * albumView, logView, stageView, photoView, the log POST, the photo record, its JPEG, the review and the S5-P
 * re-file). JSON is parsed elsewhere and read through [Read]'s three questions: org.json in :app, MiniJson in the
 * tests -- so this file holds the field names and S5's types once, and :app no parser of its own. The mirror the
 * phone keeps of the album GET is written in the same shape ([stored], port 61c slice QV), so [view] reads it back.
 *
 * Reading is tolerant, never inventive: an unknown field is ignored, a missing one takes the [Album] default, a row
 * without its key (`seq` and `id` for a log row, `id` for a work or a photo, `capture` for a capture) is left out.
 * A number is S5's when it is integral, whatever type the reader holds it as. Blank strings S5 sends for "none"
 * (`vendor`, `review_by`, `review_at`, `pm`, `closed_at`) read as null where [Album] keeps null for none.
 */
object AlbumWire {

    /**
     * Three questions about an already-parsed JSON value, all a reader needs. A field or an item is a String, a
     * Number, a Boolean, an object or an array as the reader holds them, and JSON null reads as null (org.json's
     * `JSONObject.NULL` included).
     */
    interface Read {
        /** [o]'s member [key]; null when [o] is not an object, the key is absent or its value is JSON null. */
        fun field(o: Any?, key: String): Any?

        /** [a]'s items in order when [a] is an array, else null. */
        fun items(a: Any?): List<Any?>?

        /** Whether [v] is a JSON object (so a review answer's `photo` id is told from a record's `photo` row). */
        fun isObject(v: Any?): Boolean
    }

    private fun str(v: Any?): String? = v as? String
    private fun bool(v: Any?): Boolean? = v as? Boolean

    /**
     * An integral number, whatever the reader made of it (org.json an Integer or a Long, MiniJson a Long, 4.0 a Double).
     * Internal: QuoteStage.listedOf reads S5-L's `work_states` seqs with it, so S5's numbers are read one way.
     */
    internal fun long(v: Any?): Long? = when (v) {
        is Long -> v
        is Number -> v.toDouble().let { d -> if (d.isFinite() && d == Math.floor(d) && Math.abs(d) < 9.0e15) d.toLong() else null }
        else -> null
    }

    private fun int(v: Any?): Int? = long(v)?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()

    internal fun ints(r: Read, v: Any?): List<Int> = r.items(v)?.mapNotNull { int(it) } ?: emptyList()

    /** S5's '' for none, as null. */
    private fun none(v: Any?): String? = str(v)?.ifBlank { null }

    /**
     * One log row (logView: `{seq, id, kind, work, photo, value, prev, note, role, by, by_name, via, client_at, at}`),
     * or a `stages[]` row (stageView, which sends no `kind`: [kind] is given for it). A null `value` or `prev` reads
     * '' ([Entry]'s none); a change request's rows come as S5's logView reads them, so [Entry.crKind] and
     * [Entry.crOutcome] read them whole.
     */
    fun entry(r: Read, o: Any?, kind: String = ""): Entry? {
        if (!r.isObject(o)) return null
        val seq = long(r.field(o, "seq")) ?: return null
        val id = str(r.field(o, "id")) ?: return null
        return Entry(
            seq = seq, id = id, kind = str(r.field(o, "kind")) ?: kind, work = int(r.field(o, "work")),
            value = str(r.field(o, "value")) ?: "", prev = str(r.field(o, "prev")) ?: "", note = str(r.field(o, "note")) ?: "",
            role = str(r.field(o, "role")) ?: "", by = str(r.field(o, "by")) ?: "", byName = str(r.field(o, "by_name")) ?: "",
            at = str(r.field(o, "at")) ?: "", photo = none(r.field(o, "photo")), via = str(r.field(o, "via")) ?: "",
            clientAt = none(r.field(o, "client_at")),
        )
    }

    /**
     * One of `works[16]` (`{id, state, by, by_name, at, seq, in_scope, cr, count}`). `seq` is the BASE; `cr` the live
     * request as S5 sums it (`{id, kind, by, by_name, at, note, seq}`), read as its cr_raise row with the kind in
     * `value`.
     */
    fun work(r: Read, o: Any?): Work? {
        if (!r.isObject(o)) return null
        val id = int(r.field(o, "id")) ?: return null
        val c = r.field(o, "cr")
        val cr = if (!r.isObject(c)) null else long(r.field(c, "seq"))?.let { seq ->
            Entry(seq = seq, id = str(r.field(c, "id")) ?: "", kind = Album.KIND_CR_RAISE, work = id, value = str(r.field(c, "kind")) ?: "",
                note = str(r.field(c, "note")) ?: "", by = str(r.field(c, "by")) ?: "", byName = str(r.field(c, "by_name")) ?: "",
                at = str(r.field(c, "at")) ?: "")
        }
        return Work(
            id = id, state = str(r.field(o, "state")) ?: Album.NOT_STARTED, by = str(r.field(o, "by")) ?: "",
            byName = str(r.field(o, "by_name")) ?: "", at = str(r.field(o, "at")) ?: "", seq = long(r.field(o, "seq")) ?: 0,
            inScope = bool(r.field(o, "in_scope")) ?: true, cr = cr, count = int(r.field(o, "count")) ?: 0,
        )
    }

    /**
     * One photo row (photoView: `{id, work, room, kind, caption, party, vendor, role, by, by_name, via, client_at, at,
     * bytes, review, review_by, review_by_name, review_at, own}`), the same shape on the GET, the record PUT's answer
     * and the re-file's. A vendor's names S5 blanked for this viewer stay blank.
     */
    fun photo(r: Read, o: Any?): Photo? {
        if (!r.isObject(o)) return null
        val id = str(r.field(o, "id")) ?: return null
        return Photo(
            id = id, work = int(r.field(o, "work")), at = str(r.field(o, "at")) ?: "",
            party = str(r.field(o, "party")) ?: Album.PARTY_STAFF, room = none(r.field(o, "room")),
            kind = str(r.field(o, "kind")) ?: Album.PHOTO, caption = str(r.field(o, "caption")) ?: "",
            vendor = none(r.field(o, "vendor")), role = str(r.field(o, "role")) ?: "", by = str(r.field(o, "by")) ?: "",
            byName = str(r.field(o, "by_name")) ?: "", review = str(r.field(o, "review")) ?: Album.REVIEW_PENDING,
            reviewBy = none(r.field(o, "review_by")), reviewByName = none(r.field(o, "review_by_name")),
            reviewAt = none(r.field(o, "review_at")), bytes = long(r.field(o, "bytes")) ?: 0,
            via = str(r.field(o, "via")) ?: "", clientAt = none(r.field(o, "client_at")),
        )
    }

    /**
     * One as-is capture (capturesOf: `{room, capture, captured_at, stage, work, by, at}`) as an album item: a staff
     * 360 filed by S5 (`work` 0, null or one of the sixteen), at the moment it was TAKEN (`captured_at`; the upload's
     * `at` when that is blank), which is what [Album.quoteAt] groups it by. Its [Photo.source] is
     * [Album.Source.CAPTURE] (port 61c slice K3), and S5's `at` -- the record's `updated_at`, moved by every record PUT
     * and not by S5-R's re-file -- is kept as [Photo.uploadedAt], which names the phone's cached copy of its front face
     * (the K3 critic, finding 2: a re-saved survey prep re-squares the faces under the same capture name).
     * `by_name`, the uploader's full name, is read when S5 sends it: capturesOf sends only `by`, the ERP login, so until
     * an S5 slice stores the name with the room record and sends it, a capture by another login shows that login where
     * the prototype shows a person's name (the K3 review, finding 3).
     */
    fun capture(r: Read, o: Any?): Photo? {
        if (!r.isObject(o)) return null
        val id = str(r.field(o, "capture"))?.ifBlank { null } ?: return null
        return Photo(
            id = id, work = int(r.field(o, "work")), at = none(r.field(o, "captured_at")) ?: str(r.field(o, "at")) ?: "",
            party = Album.PARTY_STAFF, room = none(r.field(o, "room")), kind = Album.PHOTO_360, by = str(r.field(o, "by")) ?: "",
            byName = str(r.field(o, "by_name")) ?: "", source = Album.Source.CAPTURE, uploadedAt = str(r.field(o, "at")) ?: "",
        )
    }

    /**
     * `GET /api/asis/:project/album[?since=]` as the phone's mirror; null when [body] is not S5's album (no object, or
     * no `role`). `scope` JSON null is "no baseline"; S5 sends `scope_at` and `pm` as '' to anyone but staff.
     */
    fun view(r: Read, body: Any?): View? {
        if (!r.isObject(body)) return null
        val role = str(r.field(body, "role")) ?: return null
        val sc = r.field(body, "scope")
        return View(
            role = role,
            mine = ints(r, r.field(body, "mine")),
            exec = bool(r.field(body, "exec")) ?: false,
            scope = if (r.items(sc) == null) null else ints(r, sc),
            scopeAt = str(r.field(body, "scope_at")) ?: "",
            pm = none(r.field(body, "pm")),
            quoteStage = none(r.field(body, "quote_stage")),
            quoteStageAt = str(r.field(body, "quote_stage_at")) ?: "",
            quoteStageBy = str(r.field(body, "quote_stage_by")) ?: "",
            works = r.items(r.field(body, "works"))?.mapNotNull { work(r, it) } ?: emptyList(),
            log = r.items(r.field(body, "log"))?.mapNotNull { entry(r, it) } ?: emptyList(),
            logNext = long(r.field(body, "log_next")) ?: 0,
            logMore = bool(r.field(body, "log_more")) ?: false,
            stages = r.items(r.field(body, "stages"))?.mapNotNull { entry(r, it, Album.KIND_STAGE) } ?: emptyList(),
            photos = r.items(r.field(body, "photos"))?.mapNotNull { photo(r, it) } ?: emptyList(),
            captures = r.items(r.field(body, "captures"))?.mapNotNull { capture(r, it) } ?: emptyList(),
            toReview = int(r.field(body, "to_review")) ?: 0,
            changeRequests = int(r.field(body, "change_requests")) ?: 0,
            closed = bool(r.field(body, "closed")) ?: false,
            closedAt = none(r.field(body, "closed_at")),
            more = bool(r.field(body, "more")) ?: false,
        )
    }

    /**
     * S5's answer to [op]'s next step ([AlbumQueue.stepOf]) -- a 200, a 409 or a refusal alike -- as the queue reads
     * it. The rows by step:
     * - LOG (`{ok, written, entry, work, scope, scope_at}`, a 409 `{error, locked?, work, entry, scope, scope_at}`): the
     *   entry, the work row and the scope as it now stands (`scope_at` sent = the scope is in the answer, null scope
     *   then being "no baseline");
     * - RECORD and REFILE (`{ok, written, photo}`; a 409 or a re-file's 403 `{error, locked?, photo}`): the photo row;
     * - IMG (`{ok, written, bytes}`): the bytes now on [op]'s photo;
     * - REVIEW (`{ok, written, id, photo: <id>, review, review_by, review_by_name, review_at, entry?}`, its 409s
     *   `{error, photo: <id>, review…}`): the review fields FLAT, as a patch of that photo, and the review's log row.
     * A body that is not an object (none, or not JSON) reads as nothing: no `written`, no rows, no words -- and a 200
     * without a boolean `written` is not S5's answer, so the queue does not move on it ([AlbumQueue.apply]).
     */
    fun reply(r: Read, body: Any?, op: Op): AlbumQueue.Reply {
        val o = body.takeIf { r.isObject(it) }
        val written = bool(r.field(o, "written"))
        val locked = bool(r.field(o, "locked")) ?: false
        val error = str(r.field(o, "error")) ?: ""
        val answer = when (AlbumQueue.stepOf(op)) {
            AlbumQueue.Step.LOG -> {
                val scopeAt = str(r.field(o, "scope_at"))
                Answer(entry = entry(r, r.field(o, "entry")), work = work(r, r.field(o, "work")),
                    scope = if (scopeAt == null) null else r.field(o, "scope").let { if (r.items(it) == null) null else ints(r, it) },
                    scopeAt = scopeAt)
            }
            AlbumQueue.Step.RECORD, AlbumQueue.Step.REFILE -> Answer(photo = photo(r, r.field(o, "photo")))
            AlbumQueue.Step.IMG -> Answer(bytes = long(r.field(o, "bytes"))?.let { BytesPatch(op.photo, it) })
            AlbumQueue.Step.REVIEW -> Answer(
                entry = entry(r, r.field(o, "entry")),
                review = str(r.field(o, "review"))?.let { rv ->
                    ReviewPatch(photo = str(r.field(o, "photo")) ?: op.photo, review = rv, reviewBy = str(r.field(o, "review_by")) ?: "",
                        reviewByName = str(r.field(o, "review_by_name")) ?: "", reviewAt = str(r.field(o, "review_at")) ?: "")
                })
            null -> Answer()
        }
        return AlbumQueue.Reply(written, locked, error, answer)
    }

    // ---- THE MIRROR AS THE PHONE KEEPS IT (port 61c slice QV) ----------------------------------------------------

    private fun entryRow(e: Entry): Map<String, Any?> = linkedMapOf(
        "seq" to e.seq, "id" to e.id, "kind" to e.kind, "work" to e.work, "photo" to e.photo, "value" to e.value, "prev" to e.prev,
        "note" to e.note, "role" to e.role, "by" to e.by, "by_name" to e.byName, "via" to e.via, "client_at" to e.clientAt, "at" to e.at,
    )

    private fun workRow(w: Work): Map<String, Any?> = linkedMapOf(
        "id" to w.id, "state" to w.state, "by" to w.by, "by_name" to w.byName, "at" to w.at, "seq" to w.seq, "in_scope" to w.inScope,
        "cr" to w.cr?.let { c -> linkedMapOf<String, Any?>("id" to c.id, "kind" to c.value, "by" to c.by, "by_name" to c.byName, "at" to c.at, "note" to c.note, "seq" to c.seq) },
        "count" to w.count,
    )

    private fun photoRow(p: Photo): Map<String, Any?> = linkedMapOf(
        "id" to p.id, "work" to p.work, "room" to p.room, "kind" to p.kind, "caption" to p.caption, "party" to p.party, "vendor" to p.vendor,
        "role" to p.role, "by" to p.by, "by_name" to p.byName, "via" to p.via, "client_at" to p.clientAt, "at" to p.at, "bytes" to p.bytes,
        "review" to p.review, "review_by" to p.reviewBy, "review_by_name" to p.reviewByName, "review_at" to p.reviewAt,
    )

    private fun captureRow(c: Photo): Map<String, Any?> = linkedMapOf(
        "room" to c.room, "capture" to c.id, "captured_at" to c.at, "work" to c.work, "by" to c.by, "by_name" to c.byName, "at" to c.uploadedAt,
    )

    /**
     * [v] as the phone STORES it (`filesDir/album/<project>.json`, written by :app as `JSONObject(stored(v))`): S5's
     * own field names and shapes, so [view] reads the stored mirror back exactly as it reads S5's answer -- one reader,
     * and a mirror written by this build is read by the next through the same tolerance. A null is JSON null (S5's
     * `scope` null = no baseline; a photo's `room` none), never a dropped key. Every field a [View] holds is written;
     * what S5 sends and [view] never reads (a capture's `stage`, a photo's `own`) is not kept, as it was never read.
     *
     * HISTORY, NOT THE PROJECT'S STATE (QV review): the stage (`quote_stage*`), each work's `state`, `by`, `by_name`
     * and `at`, and `stages` are written as S5 sent them and read back only as history -- [Album.quoteAt]'s moves,
     * the log's rows -- never as what the project IS. The file is ordered by its GET's ticket alone
     * ([Album.MirrorOrder]) and the one project-state store by S5's stamp first (QuoteStage.takes), so after one
     * pair of reads the two can hold different copies of the stage; every reader of the stage or of a work's state
     * takes the store's (QuoteStageSync.cached, QuoteStage.fromAlbum feeds it), and K3 passes [Album.chapters] its
     * `states`, `quoteStage` and `quoteStageAt` from there, never from this file.
     */
    fun stored(v: View): Map<String, Any?> = linkedMapOf(
        "role" to v.role, "mine" to v.mine, "exec" to v.exec, "scope" to v.scope, "scope_at" to v.scopeAt, "pm" to v.pm,
        "quote_stage" to v.quoteStage, "quote_stage_by" to v.quoteStageBy, "quote_stage_at" to v.quoteStageAt,
        "works" to v.works.map { workRow(it) }, "log" to v.log.map { entryRow(it) }, "log_next" to v.logNext, "log_more" to v.logMore,
        "stages" to v.stages.map { entryRow(it) }, "photos" to v.photos.map { photoRow(it) }, "captures" to v.captures.map { captureRow(it) },
        "to_review" to v.toReview, "change_requests" to v.changeRequests, "closed" to v.closed, "closed_at" to v.closedAt, "more" to v.more,
    )

    // ---- THE OUTBOX AS THE PHONE KEEPS IT (port 61c slice K4) -----------------------------------------------------

    private fun opRow(o: Op): Map<String, Any?> = linkedMapOf(
        "id" to o.id, "kind" to o.kind, "work" to o.work, "base" to o.base, "value" to o.value, "prev" to o.prev, "note" to o.note,
        "photo" to o.photo, "room" to o.room, "caption" to o.caption, "photo_kind" to o.photoKind, "client_at" to o.clientAt,
        "error" to o.error, "recorded" to o.recorded, "at" to o.at,
    )

    /**
     * Every login's outbox as the phone STORES it (one entry of the `album_outbox` preferences, written by :app as
     * `JSONObject(storedOutbox(o))`): `{v: 1, logins: [{login, projects: [{project, queue: [op…], dropped: [op…]}]}]}`,
     * each op `{id, kind, work, base, value, prev, note, photo, room, caption, photo_kind, client_at, error, recorded,
     * at}` with AlbumQueue's types -- `base` TEXT (a log op's seq as digits, a review's `review_at`, a re-file's work),
     * `work` a number or null, a null as JSON null. The per-login map lives in this shape (the K4 critic, finding 8), so
     * :pano pins that one login's ops are never read for another; its keys are [Album.partyKey]'s. Lists of entries
     * rather than objects keyed by login and project, because [Read] has no way to list an object's members -- and a
     * reader that could not would read every queued op as none, the silent loss this shape exists to rule out. Read
     * back by [outboxOf].
     */
    fun storedOutbox(o: AlbumQueue.Outboxes): Map<String, Any?> = linkedMapOf(
        "v" to 1,
        "logins" to o.logins.map { (login, projects) ->
            linkedMapOf<String, Any?>("login" to login, "projects" to projects.map { (p, s) ->
                linkedMapOf<String, Any?>("project" to p, "queue" to s.queue.map { opRow(it) }, "dropped" to s.dropped.map { opRow(it) })
            })
        },
    )

    /** One stored op, or null without an `id` (it could never be replayed: S5 keys a replay on it). An unknown kind is KEPT. */
    private fun op(r: Read, o: Any?): Op? {
        if (!r.isObject(o)) return null
        val id = str(r.field(o, "id"))?.ifBlank { null } ?: return null
        val d = Op(id = id, kind = "", work = null)
        return Op(
            id = id, kind = str(r.field(o, "kind")) ?: d.kind, work = int(r.field(o, "work")),
            base = str(r.field(o, "base")) ?: long(r.field(o, "base"))?.toString() ?: d.base,
            value = str(r.field(o, "value")) ?: d.value, prev = str(r.field(o, "prev")) ?: d.prev, note = str(r.field(o, "note")) ?: d.note,
            photo = str(r.field(o, "photo")) ?: d.photo, room = str(r.field(o, "room")), caption = str(r.field(o, "caption")) ?: d.caption,
            photoKind = str(r.field(o, "photo_kind")) ?: d.photoKind, clientAt = str(r.field(o, "client_at")) ?: d.clientAt,
            error = str(r.field(o, "error")) ?: d.error, recorded = bool(r.field(o, "recorded")) ?: d.recorded, at = str(r.field(o, "at")) ?: d.at,
        )
    }

    /**
     * The stored outboxes read back through [Read], tolerant as the mirror is: anything not in the shape reads as none;
     * a field missing takes the [Op] default; an op without an `id` is left out; an op of a kind this build cannot send
     * is KEPT (the drain refuses it with AQ's 400, that op alone); a `base` stays text (a number an older writer stored
     * is read as its digits); a blank login or project is left out, and so is an empty entry; two entries for one login
     * (or one project) are one, the later one's ops after the earlier one's.
     */
    fun outboxOf(r: Read, o: Any?): AlbumQueue.Outboxes {
        var out = AlbumQueue.Outboxes()
        for (l in r.items(r.field(o, "logins")).orEmpty()) {
            val login = Album.partyKey(str(r.field(l, "login")))
            if (login.isEmpty()) continue
            for (e in r.items(r.field(l, "projects")).orEmpty()) {
                val p = str(r.field(e, "project"))?.takeIf { it.isNotBlank() } ?: continue
                val was = out.of(login, p)
                val s = AlbumQueue.Stored(
                    queue = was.queue + (r.items(r.field(e, "queue"))?.mapNotNull { op(r, it) } ?: emptyList()),
                    dropped = was.dropped + (r.items(r.field(e, "dropped"))?.mapNotNull { op(r, it) } ?: emptyList()),
                )
                out = out.with(login, p, s)
            }
        }
        return out
    }
}
