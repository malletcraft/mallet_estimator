package com.malletcrafts.interiorestimate

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.malletcrafts.interiorestimate.pano.Album
import com.malletcrafts.interiorestimate.pano.AlbumQueue
import com.malletcrafts.interiorestimate.pano.AlbumWire
import com.malletcrafts.interiorestimate.pano.AlbumWrite
import com.malletcrafts.interiorestimate.pano.TourDemo
import com.malletcrafts.interiorestimate.pano.QuoteStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * THE ALBUM OUTBOX AND ITS DRAIN (port 61c slice K4, "K2's drain"; DESIGN-port61c.md §19): the Project Manager's taps
 * on the album -- a work's state, Accept / Not for client, a change request raised or answered -- each queued as
 * exactly its S5 call (AlbumQueue / AlbumWire, unchanged on the wire), shown at once by the overlay (AlbumPage.model),
 * and sent by [AlbumDrainWorker] whenever there is signal. S5's answer replaces the overlay; S5 wins every race.
 *
 * THE STORE: one entry of the `album_outbox` preferences, every login's outboxes in AlbumWire.storedOutbox's shape --
 * by login (Album.partyKey of whoami's user), then by the S5 id the stage and the mirror are read under. Preferences,
 * not a file: the project list reads the line for every row in composition, and its listener redraws the screens as
 * the queue moves. Written with commit() under [LOCK] (a tap is somebody's work: it is on the disk before the tap
 * returns); parsed once per write ([all]). It SURVIVES SIGN-OUT -- SyncWorker's rule, "the queue is the record of what
 * the site said" -- and is only ever read, drawn and SENT under the login it was queued under ([S5Session.cookieFor]).
 *
 * THE DRAIN: one unique job per project, `mcft-album-<id>`, CONNECTED, EXPONENTIAL backoff from 30 s, the login's
 * queue read when it RUNS. A tap and a project open with a queue enqueue it with REPLACE, so the newest tap's job runs
 * at once; SyncWorker's hourly catch-up ([enqueueAll]) REPLACEs every project's job that is not RUNNING -- one in
 * backoff is brought forward, one that ended (a 404, a sign-in changed) starts again -- and leaves a running pass to
 * finish (the K4 critic, finding 3: KEEP does nothing to a job in backoff). Not Wi-Fi-only: a log POST or a review is a
 * few hundred bytes, and a progress shot's JPEG (port 61c slice K5) goes on any network too -- the "Upload on Wi-Fi only"
 * toggle is the hourly sync's WorkManager constraint (SyncWorker.schedule), and since AS1 the as-is upload sends each
 * 360's six squared faces, more bytes than one 3072 px shot, on CONNECTED as well; holding the shot alone to Wi-Fi would
 * make the toggle mean less, not more. Whether the toggle should hold every S5 upload is Amit's (§20, Q5); until then it
 * does what it does today. ONE PASS PER PROJECT AT A TIME ([passLock], in process), pinned to the login and the sign-out
 * epoch it started under: a sign-in that changes meanwhile stops it before the next send or renewal, the queue as it
 * was (the K4 critic, finding 8). The pass itself is AlbumQueue.drain, pure; this file gives it the store, the HTTP
 * call, the commit and the renewal. A pass with no usable session ends FAILURE (not signed in: the next sign-in's
 * project open or the hourly sync enqueues it again) or RETRY (signed in, the renewal could not reach the web).
 *
 * THE COMMIT, in this order: (1) S5's rows onto the phone -- the answer into the mirror (AlbumSync.take, at its own
 * ticket after every read in flight) and its work row into the one store (QuoteStage.workCopy through
 * QuoteStageSync.write, by S5's order) -- then (2) AlbumQueue.apply on the outbox AS STORED NOW, under [LOCK] alone, ops
 * found by id, so a tap meanwhile rides behind and is rebased. Answer before removal: the screen reads the queue and
 * then the mirror (MainActivity), so no frame pairs a queue without the op with a mirror without S5's row. The pass
 * lock is the outermost and only a pass takes it; [LOCK] is never held across (1). A crash between (1) and (2) sends
 * the op again: the same id, S5's replay -- unless S5 now refuses before it looks the id up (a 403 for a PM named
 * meanwhile), which [settle] puts right after the end-of-pass GET (the K4 critic, finding 4).
 *
 * THE PROGRESS SHOT (port 61c slice K5, DESIGN-port61c.md §20): a photo op is its RECORD then its JPEG in the same pass
 * ([send] reads the shot from filesDir and sends it as `image/jpeg` exactly; a shot not on the phone is AlbumQueue.NO_FILE,
 * refused for good). The commit gains three steps, each read off the verdict: a single photo's capture row marked
 * SYNCED when its album photo lands and S5's work put back on it when S5 refused or won its re-file -- both before
 * (2), so a crash between resends the op and S5's replay does them again -- and, AFTER (2), the shot moved into the
 * image cache (AlbumImages.adopt; the K5 critic, finding 3: moved before (2), a crash between left the op queued at
 * its JPEG with no file, a NO_FILE that dropped the photo's queued re-files). A landed JPEG brings a whole row into the
 * mirror where a read had taken the record's bytes-0 row out (AlbumQueue.seeded; finding 4). A capture row moved tells
 * the screen through [KEY_ROWS] (finding 6). [trim] keeps the shots the outboxes hold, and none when they cannot be read.
 *
 * NEVER A FAILURE OF THE SYNC, and never a header in a log line: S5's words at most.
 */
object AlbumOutbox {

    private const val TAG = "AlbumOutbox"
    private const val PREFS = "album_outbox"
    private const val KEY = "outbox"
    private const val KEY_PROJECT = "project"

    /**
     * Written when the drain moved a capture row (a single photo SYNCED, S5's work put back: port 61c slice K5): the
     * screen's listener on these preferences re-reads the capture store when this key changes (the K5 critic, finding
     * 6), as it does on resume -- every other write of the outbox only redraws the overlay.
     */
    const val KEY_ROWS = "rows" // text:value a preference key, never shown

    /** The outbox's preferences; the screens listen on them so the overlay, the line and the album redraw as the queue moves. */
    fun prefs(context: Context): SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Guards every read-modify-write of the stored outboxes: a tap's add and a pass's apply never interleave.
    private val LOCK = Any()

    // The stored string and its parse (null: it did not read), so the project list's rows read the queue without
    // parsing it once per row.
    @Volatile private var parsed: Pair<String, AlbumQueue.Outboxes?>? = null

    /** Every login's outboxes as stored now (AlbumWire.outboxOf); none when nothing is stored or it does not read. */
    fun all(context: Context): AlbumQueue.Outboxes = allOrNull(context) ?: AlbumQueue.Outboxes()

    /**
     * Every login's outboxes as stored now, or NULL when what is stored does not read -- which is not an empty outbox
     * (port 61c slice K5; the K5 critic, finding 5): the shots' trim reads this and deletes nothing then, where
     * [all]'s empty answer would have deleted every queued JPEG. Nothing stored is the empty outbox.
     */
    fun allOrNull(context: Context): AlbumQueue.Outboxes? {
        val raw = prefs(context).getString(KEY, null) ?: return AlbumQueue.Outboxes()
        parsed?.let { if (it.first == raw) return it.second }
        val o = try {
            AlbumWire.outboxOf(JsonRead, JSONObject(raw))
        } catch (e: Exception) {
            Log.w(TAG, "album outbox not read: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
            null
        }
        parsed = raw to o
        return o
    }

    private fun write(context: Context, o: AlbumQueue.Outboxes) {
        val raw = JSONObject(AlbumWire.storedOutbox(o) as Map<*, *>).toString()
        prefs(context).edit().putString(KEY, raw).commit()
        parsed = raw to o
    }

    /** The signed-in login's queued ops for [project] (an S5 id), in queue order; none for a project only on this phone or nobody signed in. */
    fun queued(context: Context, project: String?): List<Album.Op> {
        if (project.isNullOrBlank()) return emptyList()
        return all(context).of(S5Session.loginKey(context), project).queue
    }

    /** The ops S5 refused or won over for [project], newest last, each with S5's words: nothing draws them yet (Q2). */
    fun dropped(context: Context, project: String?): List<Album.Op> {
        if (project.isNullOrBlank()) return emptyList()
        return all(context).of(S5Session.loginKey(context), project).dropped
    }

    /**
     * Queue [op] for [project] under [login] (the screen's Me.user), [seen] the queue the screen drew when the tap was
     * built: under [LOCK], after the ops stored now, taking from the mirror FILE the base of a predecessor answered
     * since the screen last read (AlbumWrite.atAdd; the K4 critic, finding 1). Refused -- false, nothing stored -- when
     * the S5 sign-in is not [login]'s any more. Blocking: call it off the main thread, then [enqueue]. Never throws.
     */
    fun add(context: Context, project: String, login: String, op: Album.Op, seen: List<Album.Op>): Boolean {
        if (project.isBlank()) return false
        // THE DEMO NEVER REACHES S5 (port 61c §31): no album op is ever queued against it
        if (TourDemo.isDemoKey(project)) return false
        return try {
            synchronized(LOCK) {
                val me = S5Session.loginKey(context)
                if (me.isEmpty() || me != Album.partyKey(login) || !S5Session.signedIn(context)) return false
                val all = all(context)
                val s = all.of(me, project)
                val next = AlbumWrite.atAdd(op, seen, s.queue, AlbumSync.mirror(context, project))
                write(context, all.with(me, project, s.copy(queue = s.queue + next)))
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "$project: album write not queued: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
            false
        }
    }

    /**
     * THE SINGLE PHOTO'S OP queued for [project] under [login] (port 61c slice K5), from its capture row AS STORED NOW
     * (the K5 review, finding 1): built at ingest, it is added only once its shot is encoded, a second or two in which
     * the row can be re-filed on its WORK line or deleted. So under [LOCK] -- the lock [refileSingle] and [unqueuePhoto]
     * take -- the row is read again: gone, nothing is queued (false: the caller deletes the shot); else the op files at
     * the row's filing now (AlbumWrite.singleAtAdd). A re-file whose lock came first found nothing to base on and moved
     * the phone alone: this op carries it. One whose lock comes after is based on this op. Refused, as [add] is, when the
     * S5 sign-in is not [login]'s any more. Blocking: call it off the main thread, then [enqueue]. Never throws.
     */
    fun addSingle(context: Context, project: String, login: String, op: Album.Op): Boolean {
        if (project.isBlank()) return false
        return try {
            synchronized(LOCK) {
                val me = S5Session.loginKey(context)
                if (me.isEmpty() || me != Album.partyKey(login) || !S5Session.signedIn(context)) return false
                val id = Album.deviceIdOf(op.photo) ?: return false
                val row = rowsOf(context).all().firstOrNull { it.deviceId == id }
                val next = AlbumWrite.singleAtAdd(op, row?.let { Album.workOfCapture(it.work, it.stage) }) ?: return false
                val all = all(context)
                val s = all.of(me, project)
                write(context, all.with(me, project, s.copy(queue = s.queue + next)))
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "$project: single photo not queued: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
            false
        }
    }

    /**
     * Whether any login's queue still holds the album photo op of the single photo [photoId] (port 61c slice K5):
     * SyncWorker's (c2) passes over such a photo, which would otherwise read "on this phone only".
     */
    fun holdsPhoto(context: Context, photoId: String?): Boolean = !photoId.isNullOrBlank() && all(context).holds(photoId)

    /**
     * Every op of the album photo [photoId] -- its photo op, its re-files -- taken out of whichever login's outbox holds
     * it, with its shot, under the store's lock (port 61c slice K5): its capture was deleted on the phone before the
     * photo reached S5. One already on S5 stays there: S5 has no delete for an album photo (§20, Q4). Blocking: call it
     * off the main thread. Never throws.
     */
    fun unqueuePhoto(context: Context, photoId: String?) {
        if (photoId.isNullOrBlank()) return
        try {
            synchronized(LOCK) {
                val all = allOrNull(context) ?: return
                val next = all.withoutPhoto(photoId)
                if (next != all) write(context, next)
            }
            AlbumShots.delete(context, photoId)
        } catch (e: Exception) {
            Log.w(TAG, "$photoId: album photo not unqueued: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
        }
    }

    // A single photo's re-files are built and queued in TAP ORDER (the K5 critic, finding 1): one thread, so the second of
    // two quick re-files is built on a stored queue that already holds the first.
    private val serial = java.util.concurrent.Executors.newSingleThreadExecutor()

    /**
     * THE SINGLE PHOTO'S RE-FILE on its WORK line (port 61c slice K5, S5-P): built by AlbumWrite.refileSingleAt on the
     * outboxes AS STORED and the mirror FILE, under [LOCK], in tap order ([serial]) -- so its base is the overlaid filing
     * whatever is queued before it (the K5 critic, finding 1) -- and queued behind the photo op in WHICHEVER login's queue
     * holds it, signed in or not (the K5 review, finding 5: that queue is sent only under its own login, the photo's
     * uploader); else under [login] while the S5 sign-in is still [login]'s; then the drain asked to run. Nothing is
     * queued when nothing holds the photo (the phone's filing moves alone). [from] is the row's filing before this
     * re-file, [landed] whether the row reads synced; [login] may be blank (nobody signed in). Never throws.
     */
    fun refileSingle(context: Context, project: String, login: String, deviceId: String, from: Int?, to: Int, landed: Boolean) {
        if (project.isBlank()) return
        val app = context.applicationContext
        val now = java.time.Instant.now()
        serial.execute {
            val queued = try {
                synchronized(LOCK) {
                    val key = S5Session.loginKey(app)
                    val me = key.takeIf { it.isNotEmpty() && it == Album.partyKey(login) && S5Session.signedIn(app) }
                    val all = all(app)
                    val (who, op) = AlbumWrite.refileSingleAt(all, me, project, AlbumSync.mirror(app, project), deviceId, from, to, landed, now)
                        ?: return@synchronized false
                    val s = all.of(who, project)
                    write(app, all.with(who, project, s.copy(queue = s.queue + op)))
                    true
                }
            } catch (e: Exception) {
                Log.w(TAG, "$project: album re-file not queued: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
                false
            }
            if (queued) enqueue(app, project, replace = true)
        }
    }

    /** The shots the outboxes do not hold, trimmed (AlbumShots.trim): none deleted when the outbox does not read (finding 5). */
    private fun trimShots(context: Context) {
        AlbumShots.trim(context, allOrNull(context)?.let { AlbumQueue.heldPhotos(it) })
    }

    fun workName(project: String) = "mcft-album-$project" // text:value a WorkManager name, never shown

    internal fun projectOf(input: Data): String? = input.getString(KEY_PROJECT)

    /** Queue the drain of [project]: REPLACE (a tap, a project open, the catch-up of a job not running) runs it now; KEEP leaves one queued. */
    fun enqueue(context: Context, project: String, replace: Boolean = true) {
        if (project.isBlank()) return
        val req = OneTimeWorkRequestBuilder<AlbumDrainWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(Data.Builder().putString(KEY_PROJECT, project).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(project), if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP, req)
    }

    /**
     * THE HOURLY CATCH-UP (SyncWorker, after both legs): every project of the signed-in login's outbox with an op to
     * send gets its drain -- REPLACE unless a pass is RUNNING now, so a job waiting out a long backoff is brought
     * forward and one that ended starts again, and a pass in flight is never cancelled. Reads only; the jobs send.
     */
    suspend fun enqueueAll(context: Context) {
        trimShots(context)
        val login = S5Session.loginKey(context)
        if (login.isEmpty() || !S5Session.signedIn(context)) return
        val wm = WorkManager.getInstance(context)
        for (p in all(context).projects(login)) {
            val running = runCatching {
                wm.getWorkInfosForUniqueWorkFlow(workName(p)).first().any { it.state == WorkInfo.State.RUNNING }
            }.getOrDefault(false)
            if (!running) enqueue(context, p, replace = true)
        }
    }

    // ---- the pass ------------------------------------------------------------------------------------------------

    /** How a pass ends, for the worker: done (or nothing a quick retry changes), WorkManager's backoff, or for good. */
    enum class Ends { SUCCESS, RETRY, FAILURE }

    // ONE PASS PER PROJECT AT A TIME, in process: a REPLACE-cancelled pass still finishing its blocking call and the
    // new one never interleave.
    private val passLocks = ConcurrentHashMap<String, Any>()

    private fun passLock(project: String): Any = passLocks.getOrPut(project) { Any() }

    /**
     * One pass of [project]'s drain for the signed-in login. Blocking: the worker calls it under Dispatchers.IO.
     * [cancelled] is the worker's isStopped. Never throws.
     */
    internal fun run(context: Context, project: String, cancelled: () -> Boolean): Ends {
        if (project.isBlank()) return Ends.FAILURE
        // A session near its 12 h end is renewed before the first send, as WorkRefileWorker does; refused, the phone
        // is signed out of S5 and the queue waits for that login's next sign-in.
        if (S5Session.signedIn(context)) S5Session.ensureFresh(context)
        if (!S5Session.signedIn(context)) return Ends.FAILURE
        val login = S5Session.loginKey(context)
        if (login.isEmpty()) return Ends.FAILURE
        val epoch = S5Session.epoch(context)
        if (all(context).of(login, project).queue.isEmpty()) return Ends.SUCCESS
        // signed in, and the renewal could not reach the web: a wait
        if (S5Session.cookieFor(context, login, epoch) == null) return Ends.RETRY
        var lost = false
        fun pinned(): Boolean {
            val ok = S5Session.epoch(context) == epoch && S5Session.loginKey(context) == login && S5Session.signedIn(context)
            if (!ok) lost = true
            return ok
        }
        // who S5 says the pass's login is (whoami's user and full name): a landed shot's row is theirs (AlbumQueue.seeded)
        val who = S5Session.who(context)
        val me = who?.optString("user").orEmpty() to who?.optString("full_name").orEmpty()
        val pass = try {
            synchronized(passLock(project)) {
                var mirror = AlbumSync.mirror(context, project)
                AlbumQueue.drain(
                    project = project,
                    read = JsonRead,
                    load = { all(context).of(login, project).let { Album.Outbox(mirror, it.queue, it.dropped) } },
                    send = { rq -> send(context, project, rq, login, epoch) },
                    commit = { op, code, reply -> commit(context, project, login, me, mirror, op, code, reply).also { mirror = it.outbox.mirror } },
                    relogin = { pinned() && S5Session.relogin(context, asLogin = login, atEpoch = epoch) },
                    stopped = { cancelled() || !pinned() },
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "$project: album drain stopped: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
            return Ends.RETRY
        } finally {
            // the shots no outbox holds any more, at the end of every pass (port 61c slice K5)
            trimShots(context)
        }
        if (lost) {
            Log.i(TAG, "$project: the S5 sign-in changed during the pass; its queue waits for its own login") // text:value log line
            return Ends.SUCCESS
        }
        if (pass.answered > 0) {
            // "K2 GETs after a drain": one album GET writes the mirror and the store in S5's own order and brings S5's
            // current role (a PM named meanwhile turns this phone's controls off); then what S5 holds settles `dropped`
            AlbumSync.refresh(context, project)
            settle(context, project, login)
        }
        return when {
            !pass.stopped -> Ends.SUCCESS
            // S5 does not list the project, or lacks the route: nothing a quick retry changes -- the hourly catch-up
            // and the next project open ask again
            pass.code == 404 -> Ends.SUCCESS
            !S5Session.signedIn(context) -> Ends.FAILURE
            else -> Ends.RETRY
        }
    }

    /**
     * One request with the S5 session of [login] -- never the legacy ERP key, whose owner S5 would store as `by`. A JPEG
     * step (port 61c slice K5) sends the shot read whole from filesDir (at most 8 MB: AlbumShots wrote it under S5's cap,
     * and the app is largeHeap) as `image/jpeg` EXACTLY -- S5 compares the type before any `;` -- on the as-is images'
     * client and timeouts (AsIsUpload.exchange: a 300 s write timeout); a shot not on the phone is AlbumQueue.NO_FILE.
     */
    private fun send(context: Context, project: String, rq: AlbumQueue.Request, login: String, epoch: Long): AlbumQueue.Sent {
        // no live session of this login: the drain's 401, which renews it once
        val auth = S5Session.cookieFor(context, login, epoch) ?: return AlbumQueue.Sent(401, null)
        val url = BuildConfig.WEB_URL.trimEnd('/') + rq.path { AsIsUpload.seg(it) }
        val file = rq.file
        val body: ByteArray
        val type: String
        if (file != null) {
            val f = java.io.File(context.filesDir, file)
            val shot: ByteArray? = try { if (f.isFile) f.readBytes() else null } catch (e: IOException) { null }
            if (shot == null) {
                // cleared storage: no retry can send it -- refused for good, that op and its photo's dependants alone
                Log.w(TAG, "$project: the shot of ${rq.segments.getOrNull(3).orEmpty()} is not on this phone") // text:value log line
                return AlbumQueue.Sent(AlbumQueue.NO_FILE, null)
            }
            body = shot
            type = rq.contentType
        } else {
            // JSONObject(fields): S5's JSON types as AlbumQueue built them, a null kept as JSON null (a put would drop it)
            body = JSONObject(rq.fields as Map<*, *>).toString().toByteArray(Charsets.UTF_8)
            type = rq.contentType + "; charset=utf-8" // text:value the body's content type, a wire value
        }
        return try {
            val a = AsIsUpload.exchange(rq.method, url, auth, body, type)
            AlbumQueue.Sent(a.code, runCatching { JSONObject(a.body) }.getOrNull())
        } catch (e: IOException) {
            // no signal, a reset, a timeout once the request may have gone out: STOP, and the next pass resends the same id
            Log.w(TAG, "$project: album write not sent: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
            AlbumQueue.Sent(0, null)
        }
    }

    /**
     * THE COMMIT (the class note): S5's rows onto the phone first, then the answer applied to the outbox as stored now --
     * and for a photo (port 61c slice K5) the single photo's capture row before the apply, the shot's move into the
     * cache after it. [me] is whoami's user and full name for the pass's login (a landed shot's seeded row).
     */
    private fun commit(
        context: Context, project: String, login: String, me: Pair<String, String>, mirror: Album.View?, op: Album.Op, code: Int,
        reply0: AlbumQueue.Reply,
    ): AlbumQueue.Applied {
        val v = AlbumQueue.verdictOf(mirror, op, code, reply0)
        // a landed JPEG carries a whole row for a mirror a read took the record's row out of (the K5 critic, finding 4)
        val reply = AlbumQueue.seeded(op, v, reply0, mirror?.role.orEmpty(), me.first, me.second)
        if (v != Album.Verdict.STOP && v != Album.Verdict.RELOGIN) {
            AlbumSync.take(context, project, reply.answer)
            reply.answer.work?.let { w ->
                QuoteStage.workCopy(QuoteStageSync.cached(context, project), w)?.let { QuoteStageSync.write(context, mapOf(project to it)) }
            }
        }
        val landed = AlbumQueue.landed(op, v, reply)
        val rowsMoved = singlePhotoRow(context, op, landed, AlbumQueue.refiledBack(op, v, reply))
        val applied = synchronized(LOCK) {
            val all = all(context)
            val s = all.of(login, project)
            val a = AlbumQueue.apply(Album.Outbox(mirror, s.queue, s.dropped), op, code, reply)
            write(context, all.with(login, project, AlbumQueue.Stored(a.outbox.queue, a.outbox.dropped).capped()))
            a
        }
        // AFTER the apply (the K5 critic, finding 3): a crash before it leaves the shot where the next pass's JPEG step reads it
        if (landed) AlbumImages.adopt(context, project, op.photo)
        if (rowsMoved) prefs(context).edit().putLong(KEY_ROWS, System.currentTimeMillis()).apply()
        if (applied.verdict == Album.Verdict.CONFLICT || applied.verdict == Album.Verdict.LOCKED || applied.verdict == Album.Verdict.REFUSED) {
            Log.w(TAG, "$project: ${op.kind} ${op.id} not taken by S5 ($code): " + reply.error.take(160)) // text:value log line
        }
        return applied
    }

    // The capture store, for a single photo's row (port 61c slice K5): one helper for the process.
    @Volatile private var rows: CaptureStore? = null

    private fun rowsOf(context: Context): CaptureStore =
        rows ?: synchronized(this) { rows ?: CaptureStore(context.applicationContext).also { rows = it } }

    /**
     * A SINGLE PHOTO'S CAPTURE ROW after S5's answer to its album photo (port 61c slice K5): SYNCED (no serverName: "on
     * S5, not on the bench", which the ERP leg picks up) once its JPEG has [landed]; S5's filing put back ([back]: S5
     * refused or won its re-file, S5-P's answer carrying the photo as stored) when it differs from the row's. Only for
     * an album photo that is a capture's own (Album.deviceIdOf). Returns whether a row moved. Never throws.
     */
    private fun singlePhotoRow(context: Context, op: Album.Op, landed: Boolean, back: Int?): Boolean {
        if (!landed && back == null) return false
        val id = Album.deviceIdOf(op.photo) ?: return false
        return try {
            val store = rowsOf(context)
            val row = store.all().firstOrNull { it.deviceId == id } ?: return false
            var moved = false
            if (landed && row.state != "SYNCED") { store.setState(id, "SYNCED"); moved = true } // text:value the store's state, a wire value
            if (back != null && Album.workOfCapture(row.work, row.stage) != back) { store.setWork(id, back); moved = true }
            moved
        } catch (e: Exception) {
            Log.w(TAG, "$id: capture row not updated: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
            false
        }
    }

    /** The end-of-pass GET's album settles `dropped`: an op S5 holds was written, whatever its answer said (AlbumQueue.settle). */
    private fun settle(context: Context, project: String, login: String) {
        val m = AlbumSync.mirror(context, project) ?: return
        try {
            synchronized(LOCK) {
                val all = all(context)
                val s = all.of(login, project)
                val t = AlbumQueue.settle(s, m)
                if (t != s) write(context, all.with(login, project, t))
            }
        } catch (e: Exception) {
            Log.w(TAG, "$project: album outbox not settled: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
        }
    }
}

/** The drain of one project's album outbox (AlbumOutbox.run): its input the S5 id, under Dispatchers.IO. */
class AlbumDrainWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val project = AlbumOutbox.projectOf(inputData) ?: return Result.failure()
        val ends = withContext(Dispatchers.IO) { AlbumOutbox.run(applicationContext, project) { isStopped } }
        return when (ends) {
            AlbumOutbox.Ends.SUCCESS -> Result.success()
            AlbumOutbox.Ends.RETRY -> Result.retry()
            AlbumOutbox.Ends.FAILURE -> Result.failure()
        }
    }
}
