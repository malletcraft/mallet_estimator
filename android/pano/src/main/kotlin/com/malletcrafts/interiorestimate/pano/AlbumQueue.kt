package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Album.Answer
import com.malletcrafts.interiorestimate.pano.Album.Op
import com.malletcrafts.interiorestimate.pano.Album.Outbox
import com.malletcrafts.interiorestimate.pano.Album.Verdict

/**
 * THE ALBUM OUTBOX ON THE WIRE -- each queued op becomes exactly its S5 call (port 61c slice AQ, 3 Nov 2026; the
 * pure half of DESIGN-slice2's K2). Nothing here sends anything: [requestOf] says what one op's next step sends
 * (method, path, body fields with S5's own JSON types, content type), [next] picks the step the drain sends next,
 * and [apply] takes S5's answer -- read by [AlbumWire] -- through [Album.verdict] and K1b's transitions. The drain
 * that sends (WorkManager, `AsIsUpload.exchange`, `S5Auth.fresh`) is K4's, in :app, with the queue's first writer.
 *
 * The routes are S5 QUOTE's as shipped on stage (mcft-interior-estimate 9a51733, functions/_lib/asis.js):
 * - a state, a change request raised or answered: `POST /api/asis/:project/album/log` (validLogEntry);
 * - a photo: `PUT …/album/photos/:id` (validPhoto), the record FIRST, then `PUT …/album/photos/:id/img`, the JPEG
 *   (404 until the record exists, written ONCE: a second PUT answers `written: false`);
 * - a review: `PUT …/album/photos/:photo/review` (validReview);
 * - a re-file (S5-P): `PUT …/album/photos/:photo/work` (validPhotoWork).
 *
 * The verdicts are "Offline and conflicts" (DESIGN-slice2), as [Album.verdict] tables them: 200 written or replay
 * leaves the queue (a photo's record answer moves it on to its JPEG), and a 200 with no boolean `written` -- which
 * every 200 of these routes carries -- is no answer of S5's handler, so it stops the drain; 409 is S5 winning a race,
 * so the op and every LATER queued op on its work are dropped -- a re-file's on its photo instead, and a work's never
 * a re-file -- the mirror taking what the body echoes (`locked: true` the same, in the lock banner's words); 400 /
 * 403 / 413 / 415 is refused for good -- THAT op alone, the next op that rode on it taking back what S5 still has (S5's
 * S5-P note relies on it: a vendor's photo or another uploader's is 403, never a 409 that would drop the work's later
 * ops) -- and a photo op that leaves unsent takes its photo's later re-files and reviews with it; 401 keeps the op for
 * the drain to sign in again and resend; 404, 405, 5xx and no network stop the drain with the queue as it was, for
 * the next pass -- except a re-file's or a review's 404 for a photo the mirror does not hold, refused for good, and a
 * JPEG's 404, after which the next pass sends the record again.
 *
 * THE DRAIN (port 61c slice K4, "K2's drain"): [drain] is the pass :app's AlbumDrainWorker runs -- the head, its
 * request, S5's answer, the commit, the next head -- with nothing Android in it, so AlbumQueueTest drives it against a
 * fake S5; every login's queue as the phone stores it is [Outboxes] ([AlbumWire.storedOutbox]), read, overlaid and
 * drained only for the login it was queued under. Since port 61c slice K5 it sends EVERY step, a photo's record and
 * its JPEG included: K4 held a photo op at the head untouched for this slice (the K4 critic, finding 9), and a hold
 * with no case left would be the fallback the project's rule forbids. A JPEG the phone no longer has is [NO_FILE],
 * refused for good; [landed] and [refiledBack] are what :app's commit reads off the verdict.
 */
object AlbumQueue {

    /** S5's cap on its short fields (asis.js ALBUM_MAX_SHORT): `via`, `client_at`, a review's base. */
    const val MAX_SHORT = 40

    /** `via` on every write that takes one (SPEC61c §9.2; decided in the port plan). */
    const val VIA = "phone"

    const val JSON = "application/json"
    const val JPEG = "image/jpeg"

    /** Where a photo op's JPEG waits on the phone, under filesDir, until it has landed (K5 writes it, X2 keeps it). */
    const val SHOTS = "album/shots"

    /**
     * The phone's own answer to a JPEG step whose file is not on the phone (cleared storage): no retry can send it, so
     * [verdictOf] makes it REFUSED -- that op and its photo's dependants alone (Album.afterRefused). Negative, so it is
     * no HTTP status a proxy could send, and read before every other rule.
     */
    const val NO_FILE = -404

    /** A dropped op's note for [NO_FILE], in place of S5's words (nothing draws it: §19's Q2). */
    const val NO_FILE_WORDS = "The photo's file is not on this phone" // text:value a dropped op's note, never shown

    fun shotPath(id: String): String = "$SHOTS/$id.jpg"

    /** What an op's next request is: the log POST, a photo's record then its JPEG, a review, a re-file. */
    enum class Step { LOG, RECORD, IMG, REVIEW, REFILE }

    /** The step [op] sends next; null for a kind this build cannot send (the drain refuses it for good: see [requestOf]). */
    fun stepOf(op: Op): Step? = when (op.kind) {
        Album.KIND_STATE, Album.KIND_CR_RAISE, Album.KIND_CR_CLEAR -> Step.LOG
        Album.OP_PHOTO -> if (op.recorded) Step.IMG else Step.RECORD
        Album.OP_REVIEW -> Step.REVIEW
        Album.OP_REFILE -> Step.REFILE
        else -> null
    }

    /**
     * One request, with no JSON in it: :app serialises [fields] (`JSONObject(fields)`, which writes a null as JSON
     * null -- `put(k, null)` would DROP the key, and a re-file without `base` is S5's 400) and sends [file] for the
     * JPEG. [segments] follow `/api/asis/`, each to be percent-encoded by the caller ([path]; :app's
     * `AsIsUpload.seg`). [fields] keep S5's order and S5's JSON types: a seq or a work is a number (an Int or a
     * Long), never its text -- validLogEntry refuses `"base":"0"` with a 400, which would drop every state write
     * for good (critic S2) -- a review's base is the `review_at` STRING, and null is JSON null.
     */
    data class Request(
        val method: String,
        val segments: List<String>,
        val fields: Map<String, Any?>,
        val contentType: String,
        val file: String? = null,
    ) {
        fun path(enc: (String) -> String): String = "/api/asis/" + segments.joinToString("/") { enc(it) }
    }

    /**
     * The request [op]'s next step sends for [project] (S5's id: the as-is project the album belongs to). Every
     * text is held to S5's cap on the TRIMMED text, as the op builders hold it, so an op read back from a queue file
     * an older build wrote is no 400 either. Null when [stepOf] is: the drain then applies a 400 to it, which drops
     * that op alone, as an op no retry can send.
     */
    fun requestOf(op: Op, project: String): Request? {
        val photo = listOf(project, "album", "photos", op.photo)
        return when (stepOf(op) ?: return null) {
            Step.LOG -> Request("POST", listOf(project, "album", "log"), linkedMapOf(
                "v" to 1,
                "id" to op.id,
                "kind" to op.kind,
                "work" to op.work,
                "value" to op.value,
                // a change request's prev is not the writer's to send: S5 stores the kind it answers there itself
                "prev" to (if (op.kind == Album.KIND_STATE) op.prev else null),
                "note" to cap(op.note, Album.MAX_NOTE),
                "base" to seqOf(op.base),
                "client_at" to shortOrNull(op.clientAt),
                "via" to VIA,
            ), JSON)
            Step.RECORD -> Request("PUT", photo, linkedMapOf(
                "v" to 1,
                "work" to op.work,
                "room" to op.room?.let { cap(it, Album.MAX_ROOM) }?.ifEmpty { null },
                "caption" to cap(op.caption, Album.MAX_CAPTION),
                "kind" to op.photoKind,
                "client_at" to shortOrNull(op.clientAt),
                "via" to VIA,
            ), JSON)
            Step.IMG -> Request("PUT", photo + "img", emptyMap(), JPEG, shotPath(op.photo))
            Step.REVIEW -> Request("PUT", photo + "review", linkedMapOf(
                "id" to op.id,
                "state" to op.value,
                "base" to op.base,
                "via" to VIA,
            ), JSON)
            // S5's body is {work, base} and nothing else: the work the photo goes to, the work it was last seen in
            Step.REFILE -> Request("PUT", photo + "work", linkedMapOf(
                "work" to op.work,
                "base" to op.base.trim().ifEmpty { null }?.toIntOrNull(),
            ), JSON)
        }
    }

    /** A log op's base as S5's integer: the seq it saw, 0 for none (an unreadable or negative one is 0, never a 400). */
    private fun seqOf(base: String): Long = base.trim().toLongOrNull()?.takeIf { it >= 0 } ?: 0L

    /** [s] trimmed FIRST, then cut to [max] -- S5 measures the trimmed text. */
    private fun cap(s: String, max: Int): String = s.trim().take(max)

    /** A short optional field (S5's shortText at [MAX_SHORT]): blank is null; over the cap a day keeps its day, anything else is null. */
    private fun shortOrNull(s: String): String? {
        val t = s.trim()
        return when {
            t.isEmpty() -> null
            t.length <= MAX_SHORT -> t
            Album.isDay(t) -> t.take(10)
            else -> null
        }
    }

    /** The op the drain sends next, and which of its steps. */
    data class Next(val op: Op, val step: Step?)

    /** The HEAD of the FIFO queue and its step (a photo op's record, then its JPEG); null when nothing is queued. */
    fun next(o: Outbox): Next? = o.queue.firstOrNull()?.let { Next(it, stepOf(it)) }

    /**
     * S5's answer to one request, as [AlbumWire.reply] reads it: `written` (null when the body has no boolean one --
     * a 200 like that is no answer of S5's: [apply]), `locked` (a 409 in the lock banner's words), S5's own `error`
     * words ('' when it sent none), and the rows the mirror takes ([Album.Answer]).
     */
    data class Reply(val written: Boolean? = null, val locked: Boolean = false, val error: String = "", val answer: Answer = Answer())

    /** The queue after one answer, and S5's [verdict] on it. */
    data class Applied(val outbox: Outbox, val verdict: Verdict) {
        /** Whether the drain sends the next head now: not after a STOP, and not after a 401 until it has signed in again. */
        val goOn: Boolean get() = verdict != Verdict.STOP && verdict != Verdict.RELOGIN
    }

    /**
     * [o] after S5 answered [code] with [reply] to [op]'s next step ([op] as [next] gave it). POSTED and REPLAY go
     * through [Album.afterPost] / [Album.afterReplay] -- except a photo's RECORD, which keeps the op at the head with
     * its JPEG next (or lets it go when the answer's row says the JPEG landed on an earlier pass: S5 writes it once);
     * an IMG answered `written: false` is the JPEG S5 already holds, so it counts as landed. CONFLICT and LOCKED go
     * through [Album.afterConflict] / [Album.afterLocked] with S5's words and the body's rows; REFUSED through
     * [Album.afterRefused], the mirror then taking any row the refusal echoes (a re-file's 403 carries the photo as
     * stored, so the phone can put back the work it set locally). RELOGIN and STOP leave [o] as it was -- but a
     * JPEG's 404 (S5 has no record for it: a record answer that was not S5's, or a record lost) clears `recorded`, so
     * the next pass sends the record again, which S5 replays harmlessly for its uploader.
     *
     * Two answers are read here rather than by [Album.verdict] (the AQ review): a 200 with no boolean `written` is
     * STOP -- every 200 S5 sends on these routes carries one (asis.js), so a 200 without it is a proxy's page, a
     * redirect followed to HTML or a cut body, and taking it as POSTED would lose the write, or mark a record answered
     * that S5 never stored; and a re-file's or a review's 404 for a photo the mirror does not hold ([noRow]: a photo
     * op dropped before this was queued, its tile re-filed) is REFUSED, that op alone: S5's row lookup answers 404
     * before anything else and no retry can change it, so as STOP it would hold the head of the queue for good. A 404
     * for a photo the mirror does hold stays STOP (a route an older S5 lacks, a project not visible yet): the next
     * pass may be answered.
     */
    fun apply(o: Outbox, op: Op, code: Int, reply: Reply): Applied {
        val v = verdictOf(o.mirror, op, code, reply)
        val a = reply.answer
        val out = when (v) {
            Verdict.POSTED, Verdict.REPLAY -> when {
                stepOf(op) == Step.RECORD -> recorded(o, op, a)
                v == Verdict.POSTED -> Album.afterPost(o, op, a)
                else -> Album.afterReplay(o, op, a)
            }
            Verdict.CONFLICT -> Album.afterConflict(o, op, reply.error, a)
            Verdict.LOCKED -> Album.afterLocked(o, op, reply.error, a)
            Verdict.REFUSED -> Album.afterRefused(o, op, reply.error.ifEmpty { if (code == NO_FILE) NO_FILE_WORDS else "" })
                .let { it.copy(mirror = Album.merged(it.mirror, a)) }
            Verdict.STOP -> if (code == 404 && stepOf(op) == Step.IMG) unrecorded(o, op) else o
            Verdict.RELOGIN -> o
        }
        return Applied(out, v)
    }

    /**
     * S5's [Verdict] on [op] answered [code] with [reply], as [apply] reads it -- given alone so the drain's commit can
     * put S5's rows on the phone (the mirror, the store) BEFORE it applies the answer to the outbox (port 61c slice K4):
     * [mirror] is the pass's copy, which decides [noRow]. Three answers are read here rather than by [Album.verdict]: a
     * 200 with no boolean `written` (STOP, above); a re-file's or a review's 404 for a photo [mirror] does not hold
     * (REFUSED, above); and an answer recorded on a work whose base S5 still has (STOP, the K4 critic, finding 5): S5
     * computes an approved add's or remove's amended scope from the `scope_at` it reads inside the same handler and
     * guards the batch on it (asis.js, the log POST), so a 409 `The scope was changed meanwhile` -- or its `Changed
     * meanwhile; not saved` -- whose `work.seq` is still this op's base and whose work still has its live request is
     * S5's own read-then-write race with another work's amendment, not a write S5 won: resending the same id is
     * computed afresh and written (a replay if it landed after all). With no live request left (`No change request to
     * clear`) or another base, it stays a CONFLICT.
     */
    fun verdictOf(mirror: Album.View?, op: Op, code: Int, reply: Reply): Verdict = when {
        code == NO_FILE -> Verdict.REFUSED
        code == 200 && reply.written == null -> Verdict.STOP
        code == 404 && noRow(mirror, op) -> Verdict.REFUSED
        code == 409 && !reply.locked && answerRace(op, reply) -> Verdict.STOP
        else -> Album.verdict(code, reply.written != false, reply.locked)
    }

    /** A recorded answer's 409 that names its own base and a live request: S5's scope race (finding 5), sent again. */
    private fun answerRace(op: Op, reply: Reply): Boolean {
        if (op.kind != Album.KIND_CR_CLEAR) return false
        val w = reply.answer.work ?: return false
        return w.id == op.work && w.cr != null && w.seq == (op.base.trim().toLongOrNull() ?: 0L)
    }

    /**
     * A re-file or a review of a photo the mirror does not hold, so of no row S5 is known to have. Its own photo op
     * cannot be what will record it: the queue is FIFO, so a photo op still queued is BEHIND it, and would never be
     * sent while it held the head.
     */
    private fun noRow(mirror: Album.View?, op: Op): Boolean {
        val s = stepOf(op)
        return (s == Step.REFILE || s == Step.REVIEW) && mirror?.photos.orEmpty().none { it.id == op.photo }
    }

    /**
     * Whether S5 now has [op]'s JPEG (port 61c slice K5): its JPEG step answered 200 -- written, or `written: false`, the
     * bytes S5 already holds (written once) -- or its record answered 200 with a row whose bytes have landed (the JPEG
     * went on a pass whose answer was lost). Never a record without bytes, and never another op. :app's commit then
     * moves the shot into the image cache and marks a single photo's capture row synced.
     */
    fun landed(op: Op, verdict: Verdict, reply: Reply): Boolean {
        if (op.kind != Album.OP_PHOTO || (verdict != Verdict.POSTED && verdict != Verdict.REPLAY)) return false
        return when (stepOf(op)) {
            Step.IMG -> true
            Step.RECORD -> (reply.answer.photo?.takeIf { it.id == op.photo }?.bytes ?: 0L) > 0L
            else -> false
        }
    }

    /**
     * S5's filing of a photo whose re-file [op] it refused or won (port 61c slice K5): the work of the photo its answer
     * carries -- S5-P's 403 `Only the login that uploaded the photo re-files it` and its 409s (the lock, `Re-filed
     * meanwhile`) each come with the photo as stored, "so the phone can put back the work it set locally" (S5's
     * CLAUDE.md, S5-P) -- which :app's commit puts back on the single photo's capture row. Null for a re-file S5 took,
     * for one refused with no photo (a 404 for a photo S5 has no row for: the phone's filing stands), and for a photo
     * filed at neither the quote journey nor a work.
     */
    fun refiledBack(op: Op, verdict: Verdict, reply: Reply): Int? {
        if (op.kind != Album.OP_REFILE) return null
        if (verdict != Verdict.REFUSED && verdict != Verdict.LOCKED && verdict != Verdict.CONFLICT) return null
        val p = reply.answer.photo?.takeIf { it.id == op.photo } ?: return null
        val w = p.work
        return if (w == Album.QUOTE_WORK || Album.isWork(w)) w else null
    }

    /**
     * [reply] with a WHOLE row for [op]'s photo seeded into its answer when its JPEG has landed ([landed] at the JPEG
     * step; the K5 critic, finding 4): S5's JPEG answer is `{ok, written, bytes}` and no row, and a GET between the
     * record's answer and the JPEG's removes the bytes-0 row the record put in the mirror (S5 lists no photo without
     * bytes), so the bytes would patch nothing and the landed shot have no row standing for it until the next read. The
     * row is the op's -- its work, room, caption and kind, `client_at` and `at` the shutter's, by [by] as [byName] in
     * [role], `via` phone -- with the bytes S5 answered; [Album.merged] adds it only where the mirror holds none of that
     * id, so S5's own row is never replaced, and the next GET brings S5's. Any other answer is [reply] as it came.
     */
    fun seeded(op: Op, verdict: Verdict, reply: Reply, role: String, by: String, byName: String): Reply {
        if (stepOf(op) != Step.IMG || !landed(op, verdict, reply)) return reply
        val bytes = reply.answer.bytes?.takeIf { it.photo == op.photo }?.bytes ?: return reply
        val at = op.clientAt.ifBlank { op.at }
        val row = Album.Photo(id = op.photo, work = op.work, at = at, party = Album.PARTY_STAFF,
            room = op.room?.trim()?.take(Album.MAX_ROOM)?.ifEmpty { null }, kind = op.photoKind, caption = op.caption.trim().take(Album.MAX_CAPTION),
            role = role, by = by, byName = byName, bytes = bytes, via = VIA, clientAt = op.clientAt.ifBlank { null })
        return reply.copy(answer = reply.answer.copy(seed = row))
    }

    /** [op] with its record to send again: S5 answered its JPEG 404 (no record). */
    private fun unrecorded(o: Outbox, op: Op): Outbox = o.copy(queue = o.queue.map { if (it.id == op.id) it.copy(recorded = false) else it })

    /**
     * A photo's record answered 200 (written, or a replay of the uploader's own): the mirror takes the row, and the
     * op stays at the head with its JPEG next -- or, when that row already has bytes (the JPEG landed on a pass whose
     * answer was lost), it leaves the queue as landed.
     */
    private fun recorded(o: Outbox, op: Op, a: Answer): Outbox {
        if (o.queue.none { it.id == op.id }) return o
        if ((a.photo?.takeIf { it.id == op.photo }?.bytes ?: 0L) > 0L) return Album.afterPost(o, op, a)
        return o.copy(mirror = Album.merged(o.mirror, a), queue = o.queue.map { if (it.id == op.id) it.copy(recorded = true) else it })
    }

    // ---- THE OUTBOX AS THE PHONE STORES IT, AND THE DRAIN (port 61c slice K4) ----------------------------------

    /** `dropped` keeps the newest this many, each with S5's words: nothing draws them yet (Q2), and they cap the entry. */
    const val KEEP_DROPPED = 20

    /** One project's outbox as stored: the FIFO [queue] still to send, and the ops S5 [dropped] (S5's `error` on each). */
    data class Stored(val queue: List<Op> = emptyList(), val dropped: List<Op> = emptyList()) {
        /** The entry with `dropped` held to its newest [KEEP_DROPPED]. */
        fun capped(): Stored = if (dropped.size <= KEEP_DROPPED) this else copy(dropped = dropped.takeLast(KEEP_DROPPED))

        val isEmpty: Boolean get() = queue.isEmpty() && dropped.isEmpty()
    }

    /**
     * EVERY LOGIN'S OUTBOX, by login -- [Album.partyKey] of whoami's `user` -- then by S5 project id (the id the stage
     * and the mirror are read under). A queued op is somebody's unsent work: it survives a sign-out, and it is only ever
     * READ, laid over the album and SENT under the login it was queued under, so the next person signed in on the phone
     * neither sees it nor sends it under their own name (the K4 critic, finding 8: the keying is pinned here, in
     * :pano, not in :app alone). A blank login reads nothing and stores nothing.
     */
    data class Outboxes(val logins: Map<String, Map<String, Stored>> = emptyMap()) {
        /** [login]'s outbox of [project]; empty for a blank login or none stored. */
        fun of(login: String, project: String): Stored {
            val k = Album.partyKey(login)
            if (k.isEmpty() || project.isBlank()) return Stored()
            return logins[k]?.get(project) ?: Stored()
        }

        /** These outboxes with [login]'s entry for [project] set to [s] (dropped from the map when empty); a blank login changes nothing. */
        fun with(login: String, project: String, s: Stored): Outboxes {
            val k = Album.partyKey(login)
            if (k.isEmpty() || project.isBlank()) return this
            val mine = LinkedHashMap(logins[k].orEmpty())
            if (s.isEmpty) mine.remove(project) else mine[project] = s
            val all = LinkedHashMap(logins)
            if (mine.isEmpty()) all.remove(k) else all[k] = mine
            return Outboxes(all)
        }

        /** [login]'s projects with an op still to send, in stored order: what the hourly catch-up enqueues. */
        fun projects(login: String): List<String> {
            val k = Album.partyKey(login)
            if (k.isEmpty()) return emptyList()
            return logins[k].orEmpty().filter { it.value.queue.isNotEmpty() }.keys.toList()
        }

        /**
         * Whether any login's QUEUE still holds the photo op of [photo] (port 61c slice K5): SyncWorker's (c2) passes over a
         * single photo whose album op is waiting, which it would otherwise mark "on this phone only".
         */
        fun holds(photo: String): Boolean =
            photo.isNotBlank() && logins.values.any { m -> m.values.any { s -> s.queue.any { it.kind == Album.OP_PHOTO && it.photo == photo } } }

        /**
         * The login (as stored, [Album.partyKey]) whose QUEUE of [project] holds the photo op of [photo], or null (the K5
         * review, finding 5): a single photo's re-file goes behind its photo op in THAT queue, which is sent only under
         * that login -- the photo's uploader, the one S5-P takes a re-file from -- whoever is signed in now.
         */
        fun holderOf(photo: String, project: String): String? {
            if (photo.isBlank() || project.isBlank()) return null
            return logins.entries.firstOrNull { (_, m) -> m[project]?.queue?.any { it.kind == Album.OP_PHOTO && it.photo == photo } == true }?.key
        }

        /**
         * These outboxes with every op of [photo] -- its photo op, its re-files and reviews -- taken out of every login's
         * queue and `dropped` (port 61c slice K5): its capture was deleted on the phone, as the prototype's delete takes
         * the item out of the album. An op already on S5 stays there (S5 has no delete for an album photo: Q4).
         */
        fun withoutPhoto(photo: String): Outboxes {
            if (photo.isBlank()) return this
            val out = LinkedHashMap<String, Map<String, Stored>>()
            for ((login, m) in logins) {
                val mine = LinkedHashMap<String, Stored>()
                for ((project, s) in m) {
                    val t = Stored(s.queue.filter { it.photo != photo }, s.dropped.filter { it.photo != photo })
                    if (!t.isEmpty) mine[project] = t
                }
                if (mine.isNotEmpty()) out[login] = mine
            }
            return Outboxes(out)
        }
    }

    /**
     * Every photo id whose photo op any login's queue or `dropped` holds (port 61c slice K5): the shots kept on the phone.
     * A shot S5 dropped is kept while its op is in `dropped` (the newest [KEEP_DROPPED]) and goes with it (AlbumShot.orphans).
     */
    fun heldPhotos(o: Outboxes): Set<String> {
        val out = HashSet<String>()
        for (m in o.logins.values) for (s in m.values) for (op in s.queue + s.dropped) if (op.kind == Album.OP_PHOTO && op.photo.isNotBlank()) out += op.photo
        return out
    }

    /**
     * [s] with every op in `dropped` that S5 HOLDS taken out (the K4 critic, finding 4): S5 answers a 404 or a 403 --
     * the project not visible, a PM named meanwhile -- BEFORE it looks an id up (asis.js, the log POST and the review
     * PUT), so an op S5 wrote whose answer was lost and that was resent after that change is refused although S5 has
     * it. [mirror] is the album the end-of-pass GET brought: an op whose id its log holds as that op (the same kind and
     * work, or for a review the same photo) was written, not refused. The queue is never touched.
     */
    fun settle(s: Stored, mirror: Album.View?): Stored {
        if (mirror == null || s.dropped.isEmpty()) return s
        val byId = mirror.log.associateBy { it.id }
        val left = s.dropped.filter { op ->
            val e = byId[op.id] ?: return@filter true
            val held = when (op.kind) {
                Album.OP_REVIEW -> e.kind == Album.KIND_REVIEW && e.photo == op.photo
                Album.KIND_STATE, Album.KIND_CR_RAISE, Album.KIND_CR_CLEAR -> e.kind == op.kind && e.work == op.work
                else -> false
            }
            !held
        }
        return if (left.size == s.dropped.size) s else s.copy(dropped = left)
    }

    /** One request's answer as :app read it: the status ([code] <= 0: no network, no answer) and the body as a JSON tree (null when it is not JSON). */
    data class Sent(val code: Int, val body: Any?)

    /**
     * What one pass did: how many of S5's answers it took ([answered] -- written, replayed, won or refused; never a STOP
     * or a 401), whether it [stopped] before the queue was empty, and the [code] of the answer that stopped it (0 for
     * none: no network, a cancel).
     */
    data class Pass(val answered: Int, val stopped: Boolean, val code: Int = 0)

    /**
     * ONE PASS of the drain for [project] (K2's, built in port 61c slice K4), pure: :app gives it the outbox as stored
     * NOW ([load], its mirror the pass's copy), one HTTP request ([send]), the commit ([commit]: S5's rows onto the phone
     * FIRST -- the mirror and the one store -- then [apply] on the outbox as stored then, under the store's lock, its
     * [Applied] handed back), the sign-in's renewal ([relogin]) and whether to stop ([stopped]: the worker cancelled, or
     * the S5 sign-in no longer the one the pass started under). Each step: [stopped], the head ([next], re-read every
     * step, so an op tapped during the pass goes in it), its request ([requestOf]; a kind this build cannot send is
     * committed as a 400 without a call, AQ's rule), [stopped] again just before the send, the send, S5's answer read
     * ([AlbumWire.reply]), the commit. A 401 renews the sign-in once per pass and sends the same op again; a second 401,
     * or a refused renewal, ends the pass with the queue as it was. STOP (no network, a timeout, a 5xx, a 404 on a log
     * op, a 200 that is not S5's, the answer race of [verdictOf]) ends it too. Every step is sent, a photo's record and
     * its JPEG included (port 61c slice K5): the record's 200 keeps the op at the head `recorded`, so its JPEG goes next
     * in the same pass. [maxSteps] is a guard against a commit that does not move the head.
     */
    fun drain(
        project: String,
        read: AlbumWire.Read,
        load: () -> Outbox,
        send: (Request) -> Sent,
        commit: (Op, Int, Reply) -> Applied,
        relogin: () -> Boolean,
        stopped: () -> Boolean,
        maxSteps: Int = 500,
    ): Pass {
        var answered = 0
        var renewed = false
        repeat(maxSteps) {
            if (stopped()) return Pass(answered, true)
            val n = next(load()) ?: return Pass(answered, false)
            val rq = requestOf(n.op, project)
            if (rq == null) {
                // a kind this build cannot send: AQ's 400, that op alone, with no call
                if (!commit(n.op, 400, Reply()).goOn) return Pass(answered, true, 400)
                return@repeat
            }
            if (stopped()) return Pass(answered, true)
            val s = send(rq)
            if (s.code == 401) {
                if (renewed || !relogin()) return Pass(answered, true, 401)
                renewed = true
                return@repeat
            }
            val a = commit(n.op, s.code, AlbumWire.reply(read, s.body, n.op))
            if (a.verdict != Verdict.STOP && a.verdict != Verdict.RELOGIN) answered += 1
            if (!a.goOn) return Pass(answered, true, s.code)
        }
        return Pass(answered, true)
    }
}
