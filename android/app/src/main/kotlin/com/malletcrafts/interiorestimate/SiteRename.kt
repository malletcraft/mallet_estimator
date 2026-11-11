package com.malletcrafts.interiorestimate

import android.content.Context
import android.util.Log
import com.malletcrafts.interiorestimate.pano.RenameRoute
import com.malletcrafts.interiorestimate.pano.Renames
import org.json.JSONObject

/**
 * A SITE'S NAME GOES TO S5, NOT TO THE BENCH (port 62 slice A2, 11 Nov 2026; Amit: "Yes — S5 keeps the site name").
 *
 * S5 keeps the name on each project, so a site is renamed by ONE `PUT /api/asis/:project/site {site, base}` per
 * project of it that S5 lists (S5 QUOTE slice A1), each carrying the name the phone last heard for THAT project as
 * its base. S5 compares the base with what it stores, in the one UPDATE that writes: a site somebody renamed
 * meanwhile comes back as its 409 with the name as stored, never overwritten; the same name again is `written:
 * false`, so a Save repeated after a lost answer, or after 1 of 2 landed, finishes instead of failing.
 *
 * Everything decided is :pano's and tested there (Renames: the request, S5's verdict read from the body's fields,
 * what a Save over several projects comes to, the line it draws). This object only sends, with the credentials
 * every S5 call sends (S5Auth), and puts S5's name from every answer that carries one into S5Sites AT ONCE -- the
 * tree draws from there, so nothing waits for a sync (audit R4, R6: a rename is judged on S5's answers alone, and
 * no bootstrap is read to decide it). Online and unqueued: refused, offline or unanswered, nothing is kept to send
 * later -- the dialog stays open and says why.
 */
object SiteRename {

    private const val TAG = "SiteRename"

    /**
     * ONE `PUT …/site` for [project]: S5's verdict, whatever its status. S5's 401 (a session it no longer holds)
     * renews the sign-in once and sends once more, as every S5 write does; a second refusal stands. Anything that is
     * no answer of S5's -- no signal, a timeout, a page that is not S5's -- is [Renames.Verdict.Offline]. Never throws.
     */
    private fun put(context: Context, auth: String, project: String, site: String, base: String): Renames.Verdict = try {
        val req = Renames.siteRequest(project, site, base)
        val url = BuildConfig.WEB_URL.trimEnd('/') + req.path { AsIsUpload.seg(it) }
        // JSONObject(fields): the two strings as Renames built them, the base byte for byte
        val body = JSONObject(req.fields as Map<*, *>).toString().toByteArray(Charsets.UTF_8)
        val type = req.contentType + "; charset=utf-8" // text:value a wire value, the body's content type
        var a = AsIsUpload.exchange("PUT", url, auth, body, type)
        if (a.code == 401 && S5Session.signedIn(context) && S5Session.relogin(context)) {
            a = AsIsUpload.exchange("PUT", url, S5Auth.current(context) ?: auth, body, type)
        }
        Renames.siteVerdict(JsonRead, a.code, runCatching { JSONObject(a.body) }.getOrNull())
    } catch (e: Exception) {
        // The reason only: never the header, never the name typed.
        Log.w(TAG, "$project: site name not answered: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
        Renames.Verdict.Offline
    }

    /**
     * Rename a site through S5: [site] sent to every project of [writes], in order, stopping where sending on cannot
     * help (Renames.stops). BLOCKING -- call it off the main thread. Never throws. The outcome is S5's answers ALONE
     * (Renames.outcome): Done only when every project answered that it carries the name.
     *
     * With nothing to send with it answers as S5 would: the offline line while a sign-in is held and the session
     * could not be renewed (no signal), else the sign-in line.
     */
    fun rename(context: Context, writes: List<RenameRoute.Write>, site: String): Renames.Outcome {
        // The base sent is the name held NOW for each project: a 409's stored name is what the next Save sends against.
        val sent = writes.map { w -> RenameRoute.Write(w.project, S5Sites.held(context, w.project)?.site ?: w.base) }
        val auth = S5Auth.fresh(context)
            ?: return Renames.outcome(sent, listOf(
                if (S5Session.signedIn(context)) Renames.Verdict.Offline else Renames.Verdict.Refused(401, "")))
        val verdicts = ArrayList<Renames.Verdict>()
        for (w in sent) {
            val v = put(context, auth, w.project, site, w.base)
            Renames.nowIn(v)?.let { S5Sites.take(context, w.project, Renames.heldOf(it)) }
            verdicts += v
            if (Renames.stops(v)) break
        }
        return Renames.outcome(sent, verdicts)
    }

    /**
     * THE ONE-OFF SEED (SyncWorker's S5 leg, after the list read): every project S5 lists with NO site name that this
     * phone knows one for gets `PUT …/site {site, base: ""}` -- the projects uploaded before S5 kept site names. Which
     * projects, and never the demo, is :pano's rule (Renames.seed); the base is '' , so the first write wins and a
     * phone that seeds second is answered with the name the first one gave, which is kept like any other answer.
     * Read-only as far as the sync is concerned: never a failure of it, every refusal one log line. Blocking.
     */
    fun seed(context: Context) {
        try {
            val auth = S5Auth.current(context) ?: return
            for (p in Catalogue(context).projects(CaptureStore(context).masters())) {
                val s = Renames.seed(p.serverId, p.client, p.title, S5Sites.held(context, p.serverId), p.site) ?: continue
                val v = put(context, auth, s.project, s.site, "")
                Renames.nowIn(v)?.let { S5Sites.take(context, s.project, Renames.heldOf(it)) }
                if (Renames.stops(v)) break
            }
        } catch (e: Exception) {
            Log.w(TAG, "site names not seeded: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
        }
    }
}
