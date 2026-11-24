package com.malletcrafts.interiorestimate

import android.content.Context
import android.util.Log
import com.malletcrafts.interiorestimate.pano.ClientNames
import com.malletcrafts.interiorestimate.pano.RenameRoute
import com.malletcrafts.interiorestimate.pano.Renames
import com.malletcrafts.interiorestimate.pano.TreeKeep
import com.malletcrafts.interiorestimate.pano.WishList
import org.json.JSONObject

/**
 * A CLIENT'S NAME, SENT TO S5 (port 63 slice 5, 24 Nov 2026; Amit: "Client rename"; android/docs/port63/
 * DESIGN-tree.md §11). The twin of [SiteRename], and the same shape on purpose: S5 keeps the name on each project
 * and `PUT /api/asis/:project/client {client_name, base}` is the one way it changes. The route, the request, the
 * reading of each answer and what the Save came to are :pano's (ClientNames); this object makes the calls and
 * hands every name S5 gives to [S5Clients], which is where the phone's own rows follow it.
 *
 * `client_name` is a LABEL: nothing here sends, reads or changes `customer`, the ERP Customer id.
 *
 * Blocking; call off the main thread. Never throws.
 */
object ClientRename {

    private const val TAG = "ClientRename"

    /** One `PUT …/client`. With a signed-in S5 session a 401 is answered by one fresh login and one retry, as every S5 write does. */
    private fun put(context: Context, auth: String, project: String, name: String, base: String): Renames.Verdict = try {
        val req = ClientNames.request(project, name, base)
        val url = BuildConfig.WEB_URL.trimEnd('/') + req.path { AsIsUpload.seg(it) }
        val body = JSONObject(req.fields as Map<*, *>).toString().toByteArray(Charsets.UTF_8)
        val type = req.contentType + "; charset=utf-8" // text:value a wire value, the body's content type
        var a = AsIsUpload.exchange("PUT", url, auth, body, type)
        if (a.code == 401 && S5Session.signedIn(context) && S5Session.relogin(context)) {
            a = AsIsUpload.exchange("PUT", url, S5Auth.current(context) ?: auth, body, type)
        }
        ClientNames.verdict(JsonRead, a.code, runCatching { JSONObject(a.body) }.getOrNull())
    } catch (e: Exception) {
        Log.w(TAG, "$project: client name not answered: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
        Renames.Verdict.Offline
    }

    /**
     * RENAME THE CLIENT OF [writes] -- its projects on S5 -- to [name]: ONE write per project, in order, each with
     * the name S5 holds for THAT project as its base (the phone's freshest copy; [writes]' own where it has none).
     * Every name an answer carries is kept at once -- a rename, the same name, a 409's name as stored -- and the
     * phone's rows of that project follow it there (S5Clients), so after "1 of 2" the tree shows what S5 holds.
     * It stops where the rest could only fail the same way (Renames.stops); the projects not reached are named as
     * not sent (ClientNames.outcome).
     */
    fun rename(context: Context, writes: List<RenameRoute.Write>, name: String): ClientNames.Outcome {
        val sent = writes.map { w -> RenameRoute.Write(w.project, S5Clients.held(context, w.project)?.site ?: w.base) }
        val auth = S5Auth.fresh(context)
            // Nothing to call S5 with. A session whose renewal failed for want of a signal is no answer; none at all is the sign-in line.
            ?: return ClientNames.outcome(sent, listOf(
                if (S5Session.signedIn(context)) Renames.Verdict.Offline else Renames.Verdict.Refused(401, "")))
        val verdicts = ArrayList<Renames.Verdict>()
        for (w in sent) {
            val v = put(context, auth, w.project, name, w.base)
            Renames.nowIn(v)?.let { S5Clients.take(context, w.project, Renames.heldOf(it)) }
            verdicts += v
            if (Renames.stops(v)) break
        }
        return ClientNames.outcome(sent, verdicts)
    }

    /**
     * EVERY PROJECT TOOK THE NAME: what this phone still keeps under [from] becomes [to]'s -- the rows made here and
     * never sent with their chosen rooms, every capture still filed under the old client (ClientNames.rekeyDone),
     * the S5 keys remembered for it, the masters rows that name it and the wish lists kept under its name. The
     * projects S5 lists have followed already, one by one, as S5 answered. [sites] are the client's sites as the tree
     * listed them before the Save.
     */
    fun finish(context: Context, from: String, to: String, sites: List<String>) {
        try {
            val store = CaptureStore(context)
            val cat = Catalogue(context)
            cat.renameClientHere(from, to, sites)
            val rekey = ClientNames.rekeyDone(from, to)
            for (c in store.all()) {
                val was = TreeKeep.Cap(c.project, c.customerName, c.projectTitle, sent = c.state == "SYNCED") // text:value the store's state, a wire value
                val now = rekey(was)
                if (now != was) store.setNames(c.deviceId, now.client, now.title)
            }
            S5Keys.renamed(context, "client", from, "", to) // text:value the wire kind
            store.masters()?.let { m -> if (cat.renameClientRows(m, from, to)) store.saveMasters(m) }
            WishSync.renameLocal(context, "client", from, "", to, // text:value the wire kind
                cat.projects(store.masters()).filter { !it.synced }.map { WishList.localKey(it.client, it.title) }.toSet())
        } catch (e: Exception) {
            Log.w(TAG, "rows under the old client name not moved: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
        }
    }

    /**
     * THE BACKFILL, once per project (the sync's S5 leg, after the masters are fresh): a project S5 lists to this
     * login WITH a client-name key and NO name, for which the phone holds one, is given it with base '' -- the first
     * write wins, and a 409 carries the name another phone gave first, which is taken (S5 wins; the phone's rows
     * follow it). One pass, never a loop; quiet on a refusal or no answer -- the next sync tries again -- and it
     * stops where the rest could only fail the same way. Never the demo, never a name S5 would refuse.
     */
    fun seed(context: Context) {
        try {
            val auth = S5Auth.current(context) ?: return
            for (p in Catalogue(context).projects(CaptureStore(context).masters())) {
                val s = ClientNames.seed(p.serverId, p.client, p.title, S5Clients.held(context, p.serverId), p.clientId) ?: continue
                val v = put(context, auth, s.project, s.name, "")
                Renames.nowIn(v)?.let { S5Clients.take(context, s.project, Renames.heldOf(it)) }
                if (Renames.stops(v)) break
            }
        } catch (e: Exception) {
            Log.w(TAG, "client names not backfilled: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
        }
    }
}
