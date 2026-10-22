package com.malletcrafts.interiorestimate

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * THE PHONE SIGNS IN TO S5, NOT TO ERP (migration slices A+C pulled forward; Amit,
 * 22 Oct 2026: "remove the erp key pair dependency and let S5 be store for apk").
 *
 * The person types their ERP email and password once. S5 (MCFT Interior Estimate's web
 * half) checks them against ERP, ends that ERP session, and hands back its OWN session --
 * the cookie mie_s, 12 hours -- which every /api/asis* route accepts in place of the ERP
 * key. So no API key and secret have to be generated on a User page and typed into a
 * phone any more.
 *
 * The password is KEPT so an expired session renews itself (relogin) instead of failing
 * an upload: a survey is taken on site and uploaded whenever there is signal, often after
 * the 12 hours are gone. Stored in plain app-private preferences for the same reason
 * FrappeClient.save gives -- the process is the trust boundary on a work-profile phone,
 * and half-configured encrypted prefs fail at first sync in a way plain prefs never do.
 *
 * DURING THE MIGRATION the ERP legs stay working for a phone that still holds a pair
 * (slices B and D still feed off bootstrap), and when both exist the S5 session wins
 * (S5Auth.current). Nothing here ever touches the capture queue.
 */
object S5Session {

    private const val TAG = "S5Session"
    private const val PREFS = "s5_session"
    /** Renewed an hour before S5's 12 h end, so a sync never sends a session about to lapse. */
    private const val FRESH_MS = 11L * 60 * 60 * 1000
    /** S5's own session lifetime (SESSION_TTL in asis.js). */
    private const val TTL_MS = 12L * 60 * 60 * 1000

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun url(path: String) = BuildConfig.WEB_URL.trimEnd('/') + path

    /** Whether this phone holds an S5 sign-in (email and password), whatever the cookie's age. */
    fun signedIn(context: Context): Boolean {
        val p = prefs(context)
        return !p.getString("usr", null).isNullOrBlank() && !p.getString("pwd", null).isNullOrBlank()
    }

    /** Who S5 says this is -- {user, full_name, kind, suppliers, customers} -- or null when signed out. */
    fun who(context: Context): JSONObject? =
        prefs(context).getString("who", null)?.let { runCatching { JSONObject(it) }.getOrNull() }

    /** The session as the Cookie header value, or null when there is none. Never log it. */
    fun cookieAuth(context: Context): String? =
        prefs(context).getString("cookie", null)?.takeIf { it.isNotBlank() }?.let { "mie_s=$it" }

    /** Sign-in writes and sign-out are serialised on this, so a background renewal cannot write
     *  a session back after the person signed out (review, 22 Oct 2026). */
    private val lock = Any()

    /** Bumped by every sign-out; a sign-in started before it writes nothing. */
    private fun epochOf(context: Context) = prefs(context).getLong("epoch", 0L)

    /** One sign-in's outcome: [error] null when signed in. [refused] = ERP or S5 said no to THESE
     *  credentials (wrong password, account not linked, not staff) -- trying them again cannot
     *  help and only counts failed logins against the person's ERP account. */
    private class Outcome(val error: String?, val refused: Boolean)

    /**
     * Sign in with ERP's email and password. BLOCKING -- call it off the main thread.
     * Returns null on success, else one short line for the person: S5's own error text
     * when it gave one (wrong password, account not linked, ERP unreachable), or
     * "web unreachable" when there was no answer at all.
     */
    fun login(context: Context, usr: String, pwd: String): String? =
        attempt(context, usr, pwd, synchronized(lock) { epochOf(context) }).error

    private fun attempt(context: Context, usr: String, pwd: String, epoch: Long): Outcome {
        val u = usr.trim()
        if (u.isBlank() || pwd.isBlank()) return Outcome("Email and password are needed", false)
        val body = JSONObject().put("usr", u).put("pwd", pwd).toString().toByteArray(Charsets.UTF_8)
        val a = try {
            AsIsUpload.exchange("POST", url("/api/auth/login"), "", body, "application/json; charset=utf-8")
        } catch (e: Exception) {
            // IOException above all (no signal, DNS, TLS); never the request, which carries the password.
            return Outcome("web unreachable", false)
        }
        if (!a.ok) {
            val text = runCatching { JSONObject(a.body).optString("error") }.getOrNull().orEmpty()
                .replace(Regex("\\s+"), " ").trim().take(160)
            val msg = when {
                a.code == 401 -> text.ifBlank { "ERP did not accept that email and password" }
                text.isNotBlank() -> text
                else -> "web answered ${a.code}"
            }
            // S5 answers 401 for a refused email/password and 403 for an account linked to nobody.
            return Outcome(msg, a.code == 401 || a.code == 403)
        }
        val cookie = a.setCookie?.let { COOKIE.find(it)?.groupValues?.get(1) }
            ?: return Outcome("the web did not start a session", false)
        val who = runCatching { JSONObject(a.body) }.getOrNull() ?: JSONObject()
        who.remove("ok")
        // STAFF ONLY. S5 signs in vendors and clients too, but every write this phone makes
        // (room, photos, readings, wish list) is staff-gated there, so a vendor's session would
        // look configured and have every capture refused. The session S5 just made is ended.
        val kind = who.optString("kind")
        if (kind != "staff") {
            runCatching { AsIsUpload.exchange("POST", url("/api/auth/logout"), "mie_s=$cookie", null, null) }
            return Outcome("Only MCFT staff can send captures from this app; this ERP account is " +
                (if (kind.isBlank()) "not staff" else "a $kind account"), true)
        }
        synchronized(lock) {
            if (epochOf(context) != epoch) return Outcome("signed out meanwhile", false)
            prefs(context).edit()
                .putString("usr", u)
                .putString("pwd", pwd)
                .putString("cookie", cookie)
                .putString("who", who.toString())
                .putLong("cookie_at", System.currentTimeMillis())
                .remove("refused")
                .apply()
        }
        return Outcome(null, false)
    }

    /**
     * Sign in again with the stored email and password. Blocking. Whether it worked.
     *
     * A REFUSAL SIGNS THE PHONE OUT OF S5 (review, 22 Oct 2026): the password and session are
     * dropped -- the email is kept to prefill the sign-in -- so [signedIn] turns false, nothing
     * posts that password to ERP again (Frappe locks an account after repeated failed logins),
     * and the app asks the person to sign in, saying why ([refusal]). A network failure keeps
     * everything: it says nothing about the password.
     */
    fun relogin(context: Context): Boolean {
        val (u, w, e) = synchronized(lock) {
            val p = prefs(context)
            Triple(p.getString("usr", null), p.getString("pwd", null), epochOf(context))
        }
        if (u.isNullOrBlank() || w.isNullOrBlank()) return false
        val out = attempt(context, u, w, e)
        // The reason only, never the credentials.
        if (out.error != null) Log.w(TAG, "S5 sign-in not renewed: ${out.error}")
        if (out.error != null && out.refused) synchronized(lock) {
            if (epochOf(context) == e) prefs(context).edit()
                .remove("pwd").remove("cookie").remove("cookie_at")
                .putString("refused", out.error)
                .apply()
        }
        return out.error == null
    }

    /** Why the last renewal was refused and the phone signed out of S5, or null. */
    fun refusal(context: Context): String? =
        prefs(context).getString("refused", null)?.takeIf { it.isNotBlank() }

    /** Renew the session when it is missing or older than 11 hours. Blocking. Whether a usable session is held. */
    fun ensureFresh(context: Context): Boolean {
        if (!signedIn(context)) return false
        val p = prefs(context)
        val at = p.getLong("cookie_at", 0L)
        val stale = cookieAuth(context) == null || System.currentTimeMillis() - at > FRESH_MS
        return if (stale) relogin(context) else true
    }

    /** The session as the Cookie header ONLY while S5 still honours it (under 12 h old), else
     *  null -- so an expired cookie never hides a legacy pair that still works. Non-blocking. */
    fun liveCookie(context: Context): String? {
        val at = prefs(context).getLong("cookie_at", 0L)
        return cookieAuth(context)?.takeIf { System.currentTimeMillis() - at < TTL_MS }
    }

    /**
     * Sign out: forget the email, password, session and who -- and nothing else. A
     * technician handing the phone on must not take unsent captures with them (the same
     * rule as FrappeClient.forget). S5 is told to end the session on a background thread,
     * best effort: this is called from the UI, and a session S5 is never told about simply
     * lapses after its 12 hours. The projects only S5 listed for this person go too
     * (S5Masters.forget), so the next person does not see them.
     */
    fun signOut(context: Context) {
        val cookie = synchronized(lock) {
            val c = cookieAuth(context)
            // clear() runs first whatever the order, so the bumped epoch survives it.
            prefs(context).edit().clear().putLong("epoch", epochOf(context) + 1).apply()
            c
        }
        S5Masters.forget(context)
        if (cookie != null) Thread {
            runCatching { AsIsUpload.exchange("POST", url("/api/auth/logout"), cookie, null, null) }
        }.start()
    }

    /** mie_s in a set-cookie line: S5's random id is 64 hex; logout sets it blank, which never matches. */
    private val COOKIE = Regex("(?:^|[\\s,;])mie_s=([0-9a-f]{64})")
}

/** The ONE place a request to S5 picks its credentials. */
object S5Auth {

    /**
     * The S5 session when this phone has a live one, else the legacy ERP key -- the session
     * WINS when both exist (22 Oct 2026), so a phone moved onto S5 is not still acting as the
     * key's owner on the web. A cookie past S5's 12 hours counts as none (S5Session.liveCookie).
     * Null when neither is held. Non-blocking: [fresh] is the one that renews.
     */
    fun current(context: Context): String? =
        S5Session.liveCookie(context) ?: FrappeClient.authorization(context)

    /** [current] after renewing a session near its end (S5Session.ensureFresh). BLOCKING --
     *  for the calls that run off the main thread anyway (the project list, quote stage, wish
     *  list, readings), so a screen opened after 12 h offline does not meet a 401. */
    fun fresh(context: Context): String? {
        if (S5Session.signedIn(context)) S5Session.ensureFresh(context)
        return current(context)
    }

    /** Whether [project] is a phone-minted key ([localKey]) rather than an ERP Project id. */
    fun isLocalKey(project: String) = project.startsWith("S5-")

    /**
     * The S5 project for a capture of a site with no project id yet, when there is no ERP leg
     * to mint one: a project the masters already hold for this client and title (normalised
     * as Catalogue does, at any site -- as ERP's ensureSite matched), else [localKey].
     */
    fun keyFor(context: Context, client: String, title: String): String {
        val want = Catalogue.keyOf(client, "", title)
        val ps = CaptureStore(context).masters()?.optJSONArray("projects")
        if (ps != null) for (i in 0 until ps.length()) {
            val p = ps.optJSONObject(i) ?: continue
            val id = p.optString("project")
            if (id.isBlank()) continue
            val c = p.optString("customer_name").ifBlank { p.optString("customer") }
            if (Catalogue.keyOf(c, "", p.optString("title")) == want) return id
        }
        return localKey(client, Catalogue(context).localSiteFor(client, title), title)
    }

    /**
     * The S5 project key for a site that exists ONLY on this phone (no ERP leg to mint a
     * Project id): "S5-" + the first 10 hex of SHA-256 of client|site|title, normalised
     * exactly as Catalogue.keyOf (trimmed, lower case, runs of space and underscore as one
     * space). DETERMINISTIC on purpose, so two captures of one new site -- or a capture made
     * after the app was reinstalled -- share one S5 project rather than forking it.
     */
    fun localKey(client: String, site: String, title: String): String {
        val src = Catalogue.keyOf(client, site, title)
        val d = MessageDigest.getInstance("SHA-256").digest(src.toByteArray(Charsets.UTF_8))
        return "S5-" + d.joinToString("") { "%02x".format(it) }.take(10)
    }
}

/** The project list from S5, in the masters shape the tree reads (Catalogue.erpProjects). */
object S5Masters {

    private const val TAG = "S5Masters"

    /**
     * GET /api/asis, folded into the masters this phone already holds. Blocking. Null on
     * ANY failure -- never throws -- so the stale masters are kept rather than emptied.
     *
     * MERGED, NOT REPLACED: the masters also carry the room, stage, site and job-type lists
     * a bootstrap brought, which S5 does not serve; replacing the file would empty every
     * picker. Only "projects" changes: an entry this phone already had (an ERP bootstrap's,
     * with its site and stage) is kept as it is, and a project only S5 knows is added.
     *
     * S5's `customer` is an ERP Customer id and stays exactly that -- it is what S5's
     * maySee compares and what the upload sends back -- while `customer_name`, which the
     * tree files a project under, falls back to the name typed on this phone's own captures
     * of that project when S5 has no customer (a site that exists only on phones).
     */
    fun fetch(context: Context): JSONObject? = try {
        val auth = S5Auth.fresh(context) ?: throw IllegalStateException("not signed in")
        val o = JSONObject(AsIsUpload.send("GET", BuildConfig.WEB_URL.trimEnd('/') + "/api/asis", auth, null, null))
        // A 200 without the list is not "no projects": it would hide every one.
        val arr = o.getJSONArray("projects")
        val store = CaptureStore(context)
        val captures = store.all()
        val cat = Catalogue(context)
        val base = store.masters()?.let { JSONObject(it.toString()) } ?: JSONObject()
        val had = base.optJSONArray("projects") ?: JSONArray()
        val known = mutableSetOf<String>()
        val out = JSONArray()
        for (i in 0 until had.length()) {
            val p = had.optJSONObject(i) ?: continue
            // An entry an earlier S5 read added is REPLACED by this read, so access S5 has since
            // taken away -- or a previous person's list -- does not linger. Bootstrap entries stay.
            if (p.optBoolean(FROM_S5)) continue
            out.put(p)
            known.add(p.optString("project"))
        }
        for (i in 0 until arr.length()) {
            val p = arr.optJSONObject(i) ?: continue
            val key = p.optString("project")
            if (key.isBlank() || key in known) continue
            val title = p.optString("title")
            val customer = p.optString("customer")
            val mine = captures.firstOrNull { it.project == key }
            // A project S5 holds with no customer and no capture of it on this phone (one made
            // on another phone, or before a reinstall) is filed under a visible bucket: the tree
            // drops a project with no client, and an invisible project is worse than an awkward
            // folder. The phone never sends a typed name as the customer: S5 grants a client
            // account access by that id.
            val client = customer.ifBlank { mine?.customerName.orEmpty() }.ifBlank { NO_CLIENT }
            val t = title.ifBlank { mine?.projectTitle.orEmpty() }
            val m = JSONObject()
                .put("project", key)
                .put("title", t)
                .put("customer", customer)
                .put("customer_name", client)
                .put(FROM_S5, true)
            val site = if (t.isNotBlank()) cat.localSiteFor(client, t) else ""
            if (site.isNotBlank()) m.put("site_name", site)
            out.put(m)
            known.add(key)
        }
        base.put("projects", out)
        base
    } catch (e: Exception) {
        Log.w(TAG, "projects not read from S5: " + (e.message?.take(160) ?: e.javaClass.simpleName))
        null
    }

    /** Drop every project an S5 read added (sign-out): the next person sees only their own. */
    fun forget(context: Context) {
        runCatching {
            val store = CaptureStore(context)
            val base = store.masters() ?: return
            val had = base.optJSONArray("projects") ?: return
            val out = JSONArray()
            for (i in 0 until had.length()) {
                val p = had.optJSONObject(i) ?: continue
                if (!p.optBoolean(FROM_S5)) out.put(p)
            }
            base.put("projects", out)
            store.saveMasters(base)
        }
    }

    /** Marks a masters project added from S5's list, as opposed to an ERP bootstrap's. */
    private const val FROM_S5 = "from_s5"
    /** The client folder for an S5 project nobody has named a customer for. */
    const val NO_CLIENT = "(no client)"
}
