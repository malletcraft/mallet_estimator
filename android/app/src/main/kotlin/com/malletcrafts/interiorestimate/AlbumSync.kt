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
 * READING. `?since=0` first, then `?since=<log_next>` while S5 says `log_more` (asis.js albumView: log_more
 * is set only when `since` is sent), each page merged through Album.paged (K1b's mergedLog: every row once
 * by id, in seq order), the next page's cursor read by Album.nextPage, which stops when the log is whole or
 * S5 made no progress. The pages' cost is paid on the first open only; from then on the mirror is as old as
 * its last read, and the next read pages the whole log again -- a `since` of the stored log_next would miss
 * nothing S5 appends, but a mirror is also S5's current works, scope and role, which only a fresh read gives.
 *
 * WHEN. On project open, beside QuoteStageSync.refresh, and in SyncWorker after it, for every project the
 * store holds at Client approved (the quote view exists only there). A 404 means S5 does not list the
 * project (never uploaded, or not visible to this person): the mirror, if any, goes -- "not on S5 yet"
 * keeps no mirror. A 401 renews the S5 sign-in once (S5Session.relogin) and asks again.
 *
 * THE STORE. Every read writes the project's stage, who set it and when, and each work's state at its base
 * into the one project-state store (QuoteStage.fromAlbum through QuoteStageSync.write, by S5's own order:
 * the stamp, then each work's seq), so the project line and the album agree on one copy. The mirror file
 * itself is ordered the same way a read of the list is: by the ticket its GET took as it went out
 * (QuoteStageSync.ticket), kept in memory per id, so a slow read that went out earlier never writes over
 * the mirror a later read already put there.
 *
 * KEYS. A mirror exists only for a project S5 lists, under the S5 id the project's stage is read under
 * (WishSync.stageKey: its own, or the id a re-keyed phone-only project's list moved to). A phone-only project
 * has no S5 record (404) and so never a mirror, so there is nothing to re-key here as WishSync.rekey does
 * for a list made offline: the screen simply looks the mirror up under the same key as the stage.
 *
 * NEVER A FAILURE OF THE SYNC (slice 5's pattern): any refusal, a 5xx, no network or an answer that does
 * not parse is one Log.w and nothing else; the mirror kept before stays. The preferences hold only each
 * project's read stamp (and the scope's, after a record), never the mirror: the screens listen on them so
 * any write redraws the view.
 */
object AlbumSync {

    private const val TAG = "AlbumSync"
    private const val PREFS = "album_mirror"
    private const val DIR = "album"

    // Guards the file and the per-id ticket of the write that put it there (in memory: a read in flight never
    // outlives its process, and a fresh process has no read to order against -- as QuoteStageSync.written).
    private val LOCK = Any()
    private val written = HashMap<String, Long>()

    /** The read stamps' preferences; the project screen listens on them so any write redraws the quote view. */
    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun dir(context: Context) = File(context.filesDir, DIR).apply { mkdirs() }

    private fun file(context: Context, project: String) = File(dir(context), AsIsUpload.seg(project) + ".json")

    private fun url(project: String, since: Long) =
        BuildConfig.WEB_URL.trimEnd('/') + "/api/asis/" + AsIsUpload.seg(project) + "/album?since=" + since

    /** The mirror kept for [project] (an S5 id), or null when this phone holds none, or it does not read. */
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
            var view: Album.View? = null
            var since = 0L
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
                    forget(context, project)
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

    /** The mirror [v] of [project], read at [readAt] by the GET that took the ticket [at], kept unless a later read already wrote. */
    private fun keep(context: Context, project: String, v: Album.View, readAt: String, at: Long): Boolean = synchronized(LOCK) {
        if ((written[project] ?: Long.MIN_VALUE) >= at) return false
        file(context, project).writeText(JSONObject(AlbumWire.stored(v) as Map<*, *>).toString(), Charsets.UTF_8)
        prefs(context).edit().putString(project, readAt).apply()
        written[project] = at
        true
    }

    /** The mirror of [project] goes: S5 does not list it. Never throws. */
    fun forget(context: Context, project: String) {
        if (project.isBlank()) return
        synchronized(LOCK) {
            runCatching { file(context, project).delete() }
            prefs(context).edit().remove(project).remove(scopeKey(project)).apply()
        }
    }

    private fun scopeKey(project: String) = "$project/scope"

    /**
     * S5's word on the scope as an answer to a record carries it (QuoteStageSync.recordScope: the 200, and every 409
     * that echoes the scope as stored): the mirror takes [scope] at [scopeAt] through Album.merged, as a log
     * answer's scope goes in, and the view redraws. An answer is S5's word as it lands, newer than any read in
     * flight, so no ticket orders it. Returns whether the mirror changed; false with no mirror to amend.
     */
    fun takeScope(context: Context, project: String, scope: List<Int>?, scopeAt: String): Boolean {
        if (project.isBlank()) return false
        return try {
            synchronized(LOCK) {
                val m = mirror(context, project) ?: return false
                val next = Album.merged(m, Album.Answer(scope = scope, scopeAt = scopeAt)) ?: return false
                if (next == m) return false
                file(context, project).writeText(JSONObject(AlbumWire.stored(next) as Map<*, *>).toString(), Charsets.UTF_8)
                prefs(context).edit().putString(scopeKey(project), scopeAt).apply()
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "$project: album scope not kept: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
            false
        }
    }
}
