package com.malletcrafts.interiorestimate

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.malletcrafts.interiorestimate.pano.Album
import com.malletcrafts.interiorestimate.pano.AlbumWire
import com.malletcrafts.interiorestimate.pano.QuoteStage
import org.json.JSONObject
import java.io.File
import java.time.Instant

/**
 * THE ALBUM MIRROR (port 61c slice QV, the read half of DESIGN-slice2's K2): the phone's copy of S5's
 * `GET /api/asis/:project/album` per S5 project id -- the role S5 gives this person, the PM, the approved
 * scope and its stamp, the sixteen works with their states, and the whole log, paged -- kept as a file,
 * `filesDir/album/<project>.json`, in S5's own shape (AlbumWire.stored), so AlbumWire.view reads it back
 * exactly as it reads S5's answer. The quote view draws the APPROVED SCOPE row from it (QuoteView.scopeRow:
 * `scope`, `scope_at`, `role`); the album screen (K3) and the outbox (K4) are its later readers. It is a
 * READ: nothing here sends anything, and no outbox exists yet -- that lands with its first writer, K4.
 *
 * READING. From the stored mirror's cursor, `?since=<log_next>` (Album.sinceOf), when this phone holds a
 * staff mirror of the project; `?since=0` with none, and for a vendor's or a client's (S5 hides their rows
 * by what is visible now). Then `?since=<log_next>` while S5 says `log_more` (asis.js albumView: log_more
 * is set only when `since` is sent), each page merged through Album.paged (K1b's mergedLog: every row once
 * by id, in seq order) -- the first onto the stored mirror -- the next page's cursor read by Album.nextPage,
 * which stops when the log is whole or S5 made no progress. One GET brings S5's CURRENT role, works, scope,
 * stage, photos and captures with only the rows appended since, because albumView computes everything but
 * the log on every call whatever `since` and pages only the log (asis.js); the log is append-only, so the
 * cursor misses nothing. (QV review: the first version re-paged the whole log on every read, hourly per
 * approved project, on the ground that only a read from 0 is fresh -- which asis.js contradicts.)
 *
 * WHEN. On project open, beside QuoteStageSync.refresh, and in SyncWorker after it, for every project the
 * store holds at Client approved (the quote view exists only there). A 404 means S5 does not list the
 * project (never uploaded, or not visible to this person): the mirror, if any, goes -- "not on S5 yet"
 * keeps no mirror -- at the GET's own ticket, so an older read still in flight cannot write it back. A 401
 * renews the S5 sign-in once (S5Session.relogin) and asks again. A sign-out forgets EVERY mirror
 * ([forgetAll], from S5Session.signOut): a mirror is the album as that person's role saw it.
 *
 * THE STORE. Every read writes the project's stage, who set it and when, and each work's state at its base
 * into the one project-state store (QuoteStage.fromAlbum through QuoteStageSync.write, by S5's own order:
 * the stamp, then each work's seq), so the project line and the album agree on one copy. The mirror file
 * itself is ordered by the one sequence alone (Album.MirrorOrder: the ticket a GET took as it went out, the
 * ticket an answer to a record or a 404's forget takes as it lands, after every read then in flight), never
 * by S5's stamp as the store is (QuoteStage.takes) -- so a slow read that went out earlier never writes over
 * the mirror a later read put there, nor one a record's answer amended, nor one a 404 forgot (QV review).
 * And because the two are ordered differently, the file's `quote_stage*`, each work's `state`/`by`/`at` and
 * `stages` are HISTORY (Album.quoteAt's moves, the log), never the project's state: that is the store's
 * copy, and every reader of the stage or a work's state takes it from there (QuoteStageSync.cached; K3
 * passes chapters' states from it). AlbumWire.stored says the same.
 *
 * KEYS. A mirror exists only for a project S5 lists, under the S5 id the project's stage is read under
 * (WishSync.stageKey: its own, or the id a re-keyed phone-only project's list moved to). A phone-only project
 * has no S5 record (404) and so never a mirror, so there is nothing to re-key here as WishSync.rekey does
 * for a list made offline: the screen simply looks the mirror up under the same key as the stage.
 *
 * NEVER A FAILURE OF THE SYNC (slice 5's pattern): any refusal, a 5xx, no network or an answer that does
 * not parse is one Log.w and nothing else; the mirror kept before stays. The preferences hold only each
 * project's read stamp (and the scope's, after a record), never the mirror: the screens listen on them so
 * any write redraws the view. The screen reads the mirror off the main thread ([mirror] is a file read and
 * a whole parse: the works, the whole paged log, the stages and up to a thousand photos), never in composition.
 */
object AlbumSync {

    private const val TAG = "AlbumSync"
    private const val PREFS = "album_mirror"
    private const val DIR = "album"

    // Guards the file; the order of its writers is Album.MirrorOrder, per id and in memory (a read in flight never
    // outlives its process, and a fresh process has no read to order against -- as QuoteStageSync.written).
    private val LOCK = Any()
    private val order = Album.MirrorOrder()

    /** The read stamps' preferences; the project screen listens on them so any write redraws the quote view. */
    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun dir(context: Context) = File(context.filesDir, DIR).apply { mkdirs() }

    private fun file(context: Context, project: String) = File(dir(context), AsIsUpload.seg(project) + ".json")

    private fun url(project: String, since: Long) =
        BuildConfig.WEB_URL.trimEnd('/') + "/api/asis/" + AsIsUpload.seg(project) + "/album?since=" + since

    /**
     * The mirror kept for [project] (an S5 id), or null when this phone holds none, or it does not read. A file read
     * and a whole parse: call it off the main thread (the screen reads it under Dispatchers.IO, QV review).
     */
    fun mirror(context: Context, project: String): Album.View? {
        if (project.isBlank()) return null
        return try {
            val f = file(context, project)
            if (!f.isFile) null else AlbumWire.view(JsonRead, JSONObject(f.readText(Charsets.UTF_8)))
        } catch (e: Exception) {
            Log.w(TAG, "$project: album mirror not read: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
            null
        }
    }

    /** When this phone's last read of [project]'s album went out (UTC ISO), or null when it never read one. */
    fun readAt(context: Context, project: String): String? =
        if (project.isBlank()) null else prefs(context).getString(project, null)

    /**
     * Read S5's album of [project] once -- every page -- keep it, and write its stage and work states into the
     * one store. Blocking; call it off the main thread. Returns whether a mirror was kept. Never throws, never
     * retries of its own: the next sync or the next project open reads again.
     */
    fun refresh(context: Context, project: String): Boolean {
        if (project.isBlank()) return false
        var auth = S5Auth.fresh(context) ?: return false
        return try {
            // When the first GET goes out, and its place in the one sequence (QuoteStage.takes): the store's copy
            // and the mirror file are both ordered by it, so an older read never writes over a newer one.
            val readAt = Instant.now().toString()
            val mine = QuoteStageSync.ticket()
            // From the stored mirror's cursor when it has one (Album.sinceOf): the new rows are laid onto its log and
            // everything else comes fresh from the page (Album.paged); whole from 0 otherwise.
            val stored = mirror(context, project)
            var since = Album.sinceOf(stored)
            var view: Album.View? = stored?.takeIf { since > 0 }
            var renewed = false
            while (true) {
                val a = AsIsUpload.exchange("GET", url(project, since), auth, null, null)
                if (a.code == 401 && !renewed && S5Session.signedIn(context)) {
                    // S5 no longer holds the session: signed in again once, then the same page asked again;
                    // a second 401 is a refusal like any other (the renewal that failed signed the phone out).
                    renewed = true
                    if (!S5Session.relogin(context)) return false
                    auth = S5Auth.current(context) ?: return false
                    continue
                }
                if (a.code == 404) {
                    // S5 does not list the project (never uploaded, or not visible to this person): no mirror
                    forget(context, project, mine)
                    Log.i(TAG, "$project: not on S5; no album mirror is kept") // text:value log line
                    return false
                }
                if (!a.ok) throw AsIsUpload.errorOf(a)
                // A 200 without S5's album (no `role`) is not an empty album: nothing is kept from it.
                val page = AlbumWire.view(JsonRead, JSONObject(a.body)) ?: throw IllegalStateException("not S5's album") // text:value log line
                val merged = view?.let { Album.paged(it, page) } ?: page
                view = merged
                since = Album.nextPage(merged, since) ?: break
            }
            val v = view ?: return false
            keep(context, project, v, readAt, mine)
            QuoteStageSync.write(context, mapOf(project to QuoteStage.fromAlbum(v, readAt)), mine)
            true
        } catch (e: Exception) {
            // Never the header: the message is the status and S5's own error text at most.
            Log.w(TAG, "$project: album not read from S5: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
            false
        }
    }

    /** The mirror [v] of [project], read at [readAt] by the GET that took the ticket [at], kept unless a later write already landed (Album.MirrorOrder). */
    private fun keep(context: Context, project: String, v: Album.View, readAt: String, at: Long): Boolean = synchronized(LOCK) {
        if (!order.take(project, at)) return false
        file(context, project).writeText(JSONObject(AlbumWire.stored(v) as Map<*, *>).toString(), Charsets.UTF_8)
        prefs(context).edit().putString(project, readAt).apply()
        true
    }

    /**
     * The mirror of [project] goes: S5 does not list it, answered to the GET that took the ticket [at] -- unless a
     * later write already put one there, and from then on a read older than the 404 cannot write it back
     * (Album.MirrorOrder; QV review: the first version took no ticket). Never throws.
     */
    fun forget(context: Context, project: String, at: Long) {
        if (project.isBlank()) return
        synchronized(LOCK) {
            if (!order.take(project, at)) return
            runCatching { file(context, project).delete() }
            prefs(context).edit().remove(project).remove(scopeKey(project)).apply()
        }
    }

    /**
     * Every mirror goes (S5Session.signOut, QV review): a mirror is the album as THAT person's role saw it -- the role,
     * the PM, the stamp, the names S5 blanks for others -- and the next person signing in on this phone must not draw
     * from it (a Designer offered Record from a PM's mirror) while offline, or until a read of their own lands. Taken
     * at a fresh ticket, so a read of the old person's still in flight cannot write a mirror back. Never throws.
     */
    fun forgetAll(context: Context) {
        val at = QuoteStageSync.ticket()
        synchronized(LOCK) {
            order.clear(at)
            runCatching { dir(context).listFiles()?.forEach { it.delete() } }
            runCatching { prefs(context).edit().clear().apply() }
        }
    }

    private fun scopeKey(project: String) = "$project/scope"

    /**
     * S5's word in an answer to a record (QuoteStageSync.recordScope) goes into the mirror through Album.merged, as a
     * log answer's would: the scope at its stamp (the 200's baseline, and every 409 that echoes the scope as stored),
     * and the role and the PM a 403 names (so the row redraws who records it, as S5 /projects does). The answer lands
     * at a fresh ticket (Album.MirrorOrder), after every read in flight: a GET served before the PUT and landing after
     * it is refused by [keep], as the store refuses an older read (QV review: the first version took no ticket, on the
     * ground that an answer is newer than any read in flight -- which is exactly the read that then put its
     * `scope: null` back over the recorded baseline). Returns whether the mirror changed; false with no mirror to amend.
     */
    fun take(context: Context, project: String, a: Album.Answer): Boolean {
        if (project.isBlank()) return false
        val at = QuoteStageSync.ticket()
        return try {
            synchronized(LOCK) {
                if (!order.take(project, at)) return false
                val m = mirror(context, project) ?: return false
                val next = Album.merged(m, a) ?: return false
                if (next == m) return false
                file(context, project).writeText(JSONObject(AlbumWire.stored(next) as Map<*, *>).toString(), Charsets.UTF_8)
                a.scopeAt?.let { prefs(context).edit().putString(scopeKey(project), it).apply() }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "$project: album answer not kept: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
            false
        }
    }
}
