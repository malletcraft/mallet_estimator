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
 * few hundred bytes. ONE PASS PER PROJECT AT A TIME ([passLock], in process), pinned to the login and the sign-out
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
 * NEVER A FAILURE OF THE SYNC, and never a header in a log line: S5's words at most.
 */
object AlbumOutbox {

    private const val TAG = "AlbumOutbox"
    private const val PREFS = "album_outbox"
    private const val KEY = "outbox"
    private const val KEY_PROJECT = "project"

    /** The outbox's preferences; the screens listen on them so the overlay, the line and the album redraw as the queue moves. */
    fun prefs(context: Context): SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Guards every read-modify-write of the stored outboxes: a tap's add and a pass's apply never interleave.
    private val LOCK = Any()

    // The stored string and its parse, so the project list's rows read the queue without parsing it once per row.
    @Volatile private var parsed: Pair<String, AlbumQueue.Outboxes>? = null

    /** Every login's outboxes as stored now (AlbumWire.outboxOf); none when nothing is stored or it does not read. */
    fun all(context: Context): AlbumQueue.Outboxes {
        val raw = prefs(context).getString(KEY, null) ?: return AlbumQueue.Outboxes()
        parsed?.let { if (it.first == raw) return it.second }
        val o = try {
            AlbumWire.outboxOf(JsonRead, JSONObject(raw))
        } catch (e: Exception) {
            Log.w(TAG, "album outbox not read: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
            AlbumQueue.Outboxes()
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
        val pass = try {
            synchronized(passLock(project)) {
                var mirror = AlbumSync.mirror(context, project)
                AlbumQueue.drain(
                    project = project,
                    read = JsonRead,
                    load = { all(context).of(login, project).let { Album.Outbox(mirror, it.queue, it.dropped) } },
                    send = { rq -> send(context, project, rq, login, epoch) },
                    commit = { op, code, reply -> commit(context, project, login, mirror, op, code, reply).also { mirror = it.outbox.mirror } },
                    relogin = { pinned() && S5Session.relogin(context, asLogin = login, atEpoch = epoch) },
                    stopped = { cancelled() || !pinned() },
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "$project: album drain stopped: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
            return Ends.RETRY
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
            !pass.stopped || pass.held -> Ends.SUCCESS
            // S5 does not list the project, or lacks the route: nothing a quick retry changes -- the hourly catch-up
            // and the next project open ask again
            pass.code == 404 -> Ends.SUCCESS
            !S5Session.signedIn(context) -> Ends.FAILURE
            else -> Ends.RETRY
        }
    }

    /** One request with the S5 session of [login] -- never the legacy ERP key, whose owner S5 would store as `by`. */
    private fun send(context: Context, project: String, rq: AlbumQueue.Request, login: String, epoch: Long): AlbumQueue.Sent {
        // no live session of this login: the drain's 401, which renews it once
        val auth = S5Session.cookieFor(context, login, epoch) ?: return AlbumQueue.Sent(401, null)
        val url = BuildConfig.WEB_URL.trimEnd('/') + rq.path { AsIsUpload.seg(it) }
        // JSONObject(fields): S5's JSON types as AlbumQueue built them, a null kept as JSON null (a put would drop it)
        val body = JSONObject(rq.fields as Map<*, *>).toString().toByteArray(Charsets.UTF_8)
        val type = rq.contentType + "; charset=utf-8" // text:value the body's content type, a wire value
        return try {
            val a = AsIsUpload.exchange(rq.method, url, auth, body, type)
            AlbumQueue.Sent(a.code, runCatching { JSONObject(a.body) }.getOrNull())
        } catch (e: IOException) {
            // no signal, a reset, a timeout once the request may have gone out: STOP, and the next pass resends the same id
            Log.w(TAG, "$project: album write not sent: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
            AlbumQueue.Sent(0, null)
        }
    }

    /** THE COMMIT (the class note): S5's rows onto the phone first, then the answer applied to the outbox as stored now. */
    private fun commit(
        context: Context, project: String, login: String, mirror: Album.View?, op: Album.Op, code: Int, reply: AlbumQueue.Reply,
    ): AlbumQueue.Applied {
        val v = AlbumQueue.verdictOf(mirror, op, code, reply)
        if (v != Album.Verdict.STOP && v != Album.Verdict.RELOGIN) {
            AlbumSync.take(context, project, reply.answer)
            reply.answer.work?.let { w ->
                QuoteStage.workCopy(QuoteStageSync.cached(context, project), w)?.let { QuoteStageSync.write(context, mapOf(project to it)) }
            }
        }
        val applied = synchronized(LOCK) {
            val all = all(context)
            val s = all.of(login, project)
            val a = AlbumQueue.apply(Album.Outbox(mirror, s.queue, s.dropped), op, code, reply)
            write(context, all.with(login, project, AlbumQueue.Stored(a.outbox.queue, a.outbox.dropped).capped()))
            a
        }
        if (applied.verdict == Album.Verdict.CONFLICT || applied.verdict == Album.Verdict.LOCKED || applied.verdict == Album.Verdict.REFUSED) {
            Log.w(TAG, "$project: ${op.kind} ${op.id} not taken by S5 ($code): " + reply.error.take(160)) // text:value log line
        }
        return applied
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
