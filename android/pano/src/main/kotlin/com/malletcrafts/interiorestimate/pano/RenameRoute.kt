package com.malletcrafts.interiorestimate.pano

/**
 * WHERE A RENAME GOES, decided before anything is renamed anywhere (port 62 slice A2, 11 Nov 2026; beside
 * [DeleteRoute], which was written for the same reason on the delete side).
 *
 * Amit, 11 Nov 2026, after testing APK 0.3.318: a site's name would not change -- Save showed "Saving…", then
 * "Save" again, the dialog still open, the name as it was, and no reason. The audit (android/docs/port62/
 * GAP-0.3.318.md, R1-R12) found the shape behind it: a site the phone believed was "on the server" was sent to
 * the bench with a key that died on 23 Oct, a phone signed in to S5 had no working route at all, every refusal
 * was said on a banner drawn on another screen, and the local branch reported success without checking that
 * anything had been renamed. His answer the same day: "Yes — S5 keeps the site name" -- S5 stores each project's
 * site name and gets a rename route (S5 QUOTE slice A1: `PUT /api/asis/:project/site {site, base}`); the phone
 * renames through S5; ERP is not involved; a failed rename says why in the dialog.
 *
 * So the decision is made here, out of the UI, and it is tested ([Renames.route]):
 *
 *  - A SITE never goes to the bench. Any project under it that S5 lists -> [RenameRoute.S5], one write per listed
 *    project, each with THAT project's own base (the site name exactly as S5 gave it: the phone groups a site by a
 *    normalised key, S5 compares the stored string exactly, so two projects of one phone-site can hold two
 *    spellings and one shared base would make the second PUT a 409). None listed but a row made on this phone ->
 *    [RenameRoute.Local]. Neither -- "(no site)", the demo site, a site only an old ERP bootstrap named ->
 *    [RenameRoute.Blocked], the name shown and not edited (the approved prototype's P0 c).
 *  - R9, closed for a site: the phone-minted S5 project key is a hash of client|site|title, re-derived and not
 *    stored, and the SITE is the part a site rename changes. A capture of the site queued or uploaded under an
 *    `S5-…` key the masters do not list yet would get a NEW hash on its next capture -- two S5 projects. So with
 *    such a capture ([Renames.route]'s `unlisted`) the S5 route reads S5's list first ([RenameRoute.S5.readFirst])
 *    and is routed again; still unlisted, and the Local route with one, are [RenameRoute.Blocked] (P0 d).
 *  - A CLIENT and a PROJECT keep the two routes they had (the phone's own row, else the bench by docname), with
 *    [RenameRoute.Blocked] in place of a call that cannot succeed: no stored ERP pair to call the bench with, or
 *    no row on this phone to rename. Their other defects (R10-R12, and R9's project and client thirds) are slice A3.
 *
 * The same object holds the wire ([Renames.siteRequest], [Renames.siteVerdict], pinned to S5's own answers in
 * `S5Site`), what one Save comes to over several projects ([Renames.outcome], [Renames.text]), the order S5's
 * names are kept in on the phone ([Renames.takes]) and the one-off seed ([Renames.seed]).
 */
import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Tx

sealed class RenameRoute {

    /** One S5 write of a site rename: the S5 project, and the site name S5 last gave for it -- the base, sent as it is ('' = S5 holds none). */
    data class Write(val project: String, val base: String)

    /**
     * The grey line under the name, which says where this rename goes: [S5] a site S5 holds (app.site_on_s5_so_new);
     * [SERVER] a client or a project on the office system (app.x_server_so_new); [PHONE] a row made on this phone
     * (app.x_has_not_synced); [SHUT] a site that cannot be renamed here at all -- the name is shown and not edited,
     * Save is off, and the reason stands in the grey line's place (app.site_not_renamed_here, P0 c).
     */
    enum class Line { S5, SERVER, PHONE, SHUT }

    /**
     * Rename through S5: one `PUT …/site` per entry of [writes]. [readFirst]: a capture of the site is still under
     * an `S5-…` key the masters do not list, so S5's list is read first and the route asked again (R9).
     */
    data class S5(val writes: List<Write>, val readFirst: Boolean) : RenameRoute()

    /** Rename the phone's own rows. The caller reads what the rename touched: nothing renamed is a failure, never a success. */
    object Local : RenameRoute()

    /** A client or a project the bench holds: `rename_node` with [docname], as before port 62. Never a site. */
    data class Legacy(val docname: String) : RenameRoute()

    /** Do nothing, and show [reason]: with [line] [Line.SHUT] in the grey line's place from the moment the dialog opens, else under the name when Save is tapped. */
    data class Blocked(val reason: String, val line: Line) : RenameRoute()
}

object Renames {

    /** The bucket a project with no site is filed under (`Catalogue.NO_SITE` in :app, pinned equal by RenameRouteTest): a bucket, not a place, so it has no name to change. */
    const val NO_SITE = "(no site)" // text:value a stored site name, shown through Masters.site_name

    /** A name as the catalogue compares one: trimmed, lower case, runs of space and underscore as one space. */
    fun key(s: String): String = WishList.norm(s)

    /** Whether [name] is a site's own name: not blank, and not the [NO_SITE] bucket. */
    fun named(name: String?): Boolean = !name.isNullOrBlank() && key(name) != key(NO_SITE)

    /**
     * @param tx          the live language: every refusal is a whole key
     * @param kind        "client", "site" or "project"
     * @param name        the row's current name
     * @param serverId    the bench docname the row carries, blank if none -- never read for a site
     * @param hasLocalRow whether a row made on this phone carries the name (Catalogue.hasLocal)
     * @param s5Projects  for a site: the projects under it that S5 lists, each with S5's own name for it as the base
     * @param signedIn    whether this phone holds anything to call S5 with (an S5 sign-in, or the legacy key S5 also takes)
     * @param legacyPair  whether an ERP pair is stored, for a client's or a project's bench rename
     * @param unlisted    for a site: a capture of it is queued or uploaded under an `S5-…` key the masters do not list
     * @param listRead    S5's list has just been read for this Save, so [unlisted] is S5's own answer
     */
    @JvmStatic
    fun route(
        tx: Tx, kind: String, name: String, serverId: String, hasLocalRow: Boolean,
        s5Projects: List<RenameRoute.Write>, signedIn: Boolean, legacyPair: Boolean, unlisted: Boolean,
        listRead: Boolean = false,
    ): RenameRoute {
        if (kind == "site") {
            // The bucket has no name to change, whatever is filed in it: renaming it would name every unnamed project at once.
            if (!named(name)) return RenameRoute.Blocked(tx.app_site_not_renamed_here, RenameRoute.Line.SHUT)
            val writes = s5Projects.filter { it.project.isNotBlank() }.distinctBy { it.project }
            if (writes.isNotEmpty()) {
                // Known on the phone, so said at once and nothing is sent (the prototype's rn62Save).
                if (!signedIn) return RenameRoute.Blocked(tx.refuse_site_signed_out, RenameRoute.Line.S5)
                // S5's list was read first and the capture's project is still not on it.
                if (unlisted && listRead) return RenameRoute.Blocked(tx.refuse_site_capture_on_way, RenameRoute.Line.S5)
                return RenameRoute.S5(writes, readFirst = unlisted)
            }
            if (hasLocalRow) {
                return if (unlisted) RenameRoute.Blocked(tx.refuse_site_capture_on_way, RenameRoute.Line.PHONE) else RenameRoute.Local
            }
            // Nothing on S5 and nothing made here: "(no site)" is caught above; this is the demo site, a site only an
            // old ERP bootstrap named, or a site whose phone row is gone. A local rename would report a success
            // that renamed nothing (R3) and the bench is not called for a site, so it is refused from the start.
            return RenameRoute.Blocked(tx.app_site_not_renamed_here, RenameRoute.Line.SHUT)
        }
        val id = serverId.trim()
        if (id.isNotEmpty()) {
            if (legacyPair) return RenameRoute.Legacy(id)
            // No stored pair: the call cannot be made. The words are the ones the banner used to carry.
            return RenameRoute.Blocked(tx.app_msg_could_not_rename_x(why = tx.app_no_server_configured), RenameRoute.Line.SERVER)
        }
        if (hasLocalRow) return RenameRoute.Local
        // No docname and no row on this phone (the demo client or project, the "(no client)" bucket): nothing a
        // rename could change, so it is not reported as one.
        return RenameRoute.Blocked(nothingRenamed(tx, kind, name), RenameRoute.Line.PHONE)
    }

    /**
     * The line for a local rename with no row of this phone's to rename -- known before Save for a client or a
     * project, and found at Save when the row went meanwhile (Catalogue.renameLocal answering false). For a site it
     * is the prototype's own sentence (P0 c). For a client or a project the prototype draws no such state, so it is
     * the one approved line for a rename that failed (app.msg.could_not_rename_x), naming the row as the tree does.
     */
    fun nothingRenamed(tx: Tx, kind: String, name: String): String = when (kind) {
        "site" -> tx.app_site_not_renamed_here
        "client" -> tx.app_msg_could_not_rename_x(why = Masters.client_name(tx, name))
        else -> tx.app_msg_could_not_rename_x(why = name)
    }

    /** The line for a bench rename (a client's or a project's) that the bench refused or could not be reached for: its own words, as the banner used to carry them. */
    fun benchRefused(tx: Tx, why: String?): String = tx.app_msg_could_not_rename_x(why = why.orEmpty())

    /** The grey line [route] draws: where the rename is about to go. */
    @JvmStatic
    fun line(route: RenameRoute): RenameRoute.Line = when (route) {
        is RenameRoute.S5 -> RenameRoute.Line.S5
        RenameRoute.Local -> RenameRoute.Line.PHONE
        is RenameRoute.Legacy -> RenameRoute.Line.SERVER
        is RenameRoute.Blocked -> route.line
    }

    /** Whether [route] leaves nothing to type: the name is shown, not edited, and Save is off (P0 c). */
    @JvmStatic
    fun shut(route: RenameRoute): Boolean = route is RenameRoute.Blocked && route.line == RenameRoute.Line.SHUT

    // ---- the wire: PUT /api/asis/:project/site {site, base} (S5 QUOTE slice A1, functions/_lib/asis.js) ----------

    /** The body: the new name, and the name the phone last saw for THIS project, exactly as S5 gave it ('' when S5 holds none). */
    fun siteBody(site: String, base: String): LinkedHashMap<String, Any?> = linkedMapOf("site" to site, "base" to base)

    /** The request for one project of the site. No Tx, no `via`: S5 reads the two fields and nothing else. */
    fun siteRequest(project: String, site: String, base: String): AlbumQueue.Request =
        AlbumQueue.Request("PUT", listOf(project, "site"), siteBody(site, base), AlbumQueue.JSON)

    /** S5's name for one project as an answer or the list gives it: the name ('' = S5 holds none), who set it and S5's own stamp. */
    data class Now(val site: String, val by: String, val at: String)

    /** S5's verdict on one `PUT …/site`, read from the body's fields. */
    sealed interface Verdict {
        /** 200, `written: true`: S5 took the name ([now] is the name as S5 stored it). */
        data class Renamed(val now: Now) : Verdict

        /** 200, `written: false`: the project already carries this name -- the retry after a lost answer, or a project an earlier Save reached. */
        data class Same(val now: Now) : Verdict

        /** 409: the base is not S5's name any more; [now] is the name as stored (blank: S5 holds none), [error] S5's own words. */
        data class Stale(val error: String, val now: Now) : Verdict

        /** 400, 401, 403, 404 or 413 with S5's `error`: refused, [error] S5's own words, shown as data (a 401 as the sign-in line). */
        data class Refused(val code: Int, val error: String) : Verdict

        /** No answer of S5's handler: nothing came back, a body that is not S5's, a 405 or a 5xx. The name changes only when S5 answers. */
        data object Offline : Verdict
    }

    private fun str(v: Any?): String? = v as? String

    private fun nowOf(r: AlbumWire.Read, o: Any?): Now? {
        val site = str(r.field(o, "site")) ?: return null
        return Now(site, str(r.field(o, "site_by")) ?: "", str(r.field(o, "site_at")) ?: "")
    }

    /**
     * S5's answer to one site PUT: [code] the HTTP status (null when no answer came back), [body] the parsed body.
     * An answer is S5's only when its body is an object carrying what S5 sends with that status: `ok` and a name
     * with a 200, `error` and the name as stored with a 409, `error` with a refusal. Anything else is [Verdict.Offline].
     */
    fun siteVerdict(r: AlbumWire.Read, code: Int?, body: Any?): Verdict {
        if (code == null) return Verdict.Offline
        val o = body.takeIf { r.isObject(it) } ?: return Verdict.Offline
        val error = str(r.field(o, "error"))
        val now = nowOf(r, o)
        return when (code) {
            200 -> {
                val written = r.field(o, "written") as? Boolean
                if (r.field(o, "ok") != true || written == null || now == null || now.site.isEmpty()) Verdict.Offline
                else if (written) Verdict.Renamed(now) else Verdict.Same(now)
            }
            409 -> if (error != null && now != null) Verdict.Stale(error, now) else Verdict.Offline
            400, 401, 403, 404, 413 -> if (error != null) Verdict.Refused(code, error) else Verdict.Offline
            else -> Verdict.Offline
        }
    }

    /** The name S5 holds for the project after [v], when the answer says: a rename, the same name, and a 409's name as stored. */
    fun nowIn(v: Verdict): Now? = when (v) {
        is Verdict.Renamed -> v.now
        is Verdict.Same -> v.now
        is Verdict.Stale -> v.now
        else -> null
    }

    /**
     * Whether the rest of a site's projects are not sent after [v]: with no answer there is no signal to send
     * them on, and a refusal of the caller or of the name itself (not signed in, not staff, a name S5 does not
     * take) is the same for every project. A stale base or a project S5 does not hold is that project's alone.
     */
    fun stops(v: Verdict): Boolean = v == Verdict.Offline || (v is Verdict.Refused && v.code != 404)

    /** What one Save came to over the site's projects. */
    sealed interface Outcome {
        /** Every project carries the name: [site] as S5 stored it. */
        data class Done(val site: String) : Outcome

        /** [done] of [of] projects carry the name and the rest can still take it: Save again finishes. */
        data class Part(val done: Int, val of: Int) : Outcome

        /** Nothing more can be said than S5's verdict on [sent] ([verdict] is never Renamed or Same). */
        data class Failed(val verdict: Verdict, val sent: RenameRoute.Write) : Outcome
    }

    /**
     * [verdicts] are S5's answers to [writes], in order; fewer when the Save stopped ([stops]). Success is S5's
     * answers ALONE: every project answered Renamed or Same. With some renamed and the first failure one that
     * Save again can get past (no answer, a stale base whose stored name the phone now holds) it is [Outcome.Part];
     * otherwise the first failure speaks for itself.
     */
    fun outcome(writes: List<RenameRoute.Write>, verdicts: List<Verdict>): Outcome {
        val done = verdicts.count { it is Verdict.Renamed || it is Verdict.Same }
        if (writes.isNotEmpty() && done == writes.size) {
            return Outcome.Done(verdicts.firstNotNullOf { v -> nowIn(v)?.site })
        }
        val at = verdicts.indexOfFirst { !(it is Verdict.Renamed || it is Verdict.Same) }
        // No failure among the answers, yet not every project answered: the Save stopped with nothing said, which is no answer.
        val failed = if (at < 0) Verdict.Offline else verdicts[at]
        val sent = writes.getOrNull(if (at < 0) verdicts.size else at) ?: RenameRoute.Write("", "")
        if (done > 0 && (failed == Verdict.Offline || failed is Verdict.Stale)) return Outcome.Part(done, writes.size)
        return Outcome.Failed(failed, sent)
    }

    /**
     * The line under the name for [o] (the approved prototype's P0 a); null when the site is renamed. S5's 409 for
     * a site somebody renamed meanwhile is drawn from its FIELDS through its key (the prototype keys it, so it
     * reads in Marathi too); every other refusal of S5's is S5's own words, shown as data; a 401 is the sign-in
     * line; no answer is the offline line.
     */
    fun text(tx: Tx, o: Outcome): String? = when (o) {
        is Outcome.Done -> null
        is Outcome.Part -> tx.refuse_site_part_x(done = o.done, of = o.of)
        is Outcome.Failed -> when (val v = o.verdict) {
            is Verdict.Stale ->
                // "Renamed by … meanwhile — now …": a name is stored, it is not the base sent, and S5 says who set it.
                if (v.now.site.isNotEmpty() && v.now.site != o.sent.base && v.now.by.isNotBlank())
                    tx.refuse_site_renamed_x(by = v.now.by, site = v.now.site)
                else v.error
            is Verdict.Refused -> if (v.code == 401) tx.refuse_site_signed_out else v.error
            else -> tx.refuse_site_offline
        }
    }

    // ---- S5's names as the phone keeps them ------------------------------------------------------------------

    /** S5's name for one project as this phone last heard it: [site] ('' = S5 lists the project and holds no name), [at] S5's `site_at` ('' with no name). */
    data class Held(val site: String, val at: String)

    /** [now] as the phone keeps it. */
    fun heldOf(now: Now): Held = Held(now.site, now.at)

    /**
     * One row of `GET /api/asis` -> the project and S5's name for it; null for a row with no project, or from an S5
     * that does not serve site names (no `site` text on the row: S5 before slice A1) -- such a project is not
     * "listed with no name", it is not known either way, and nothing is seeded or routed on it.
     */
    fun listedOf(r: AlbumWire.Read, row: Any?): Pair<String, Held>? {
        val project = str(r.field(row, "project"))?.takeIf { it.isNotBlank() } ?: return null
        val site = str(r.field(row, "site")) ?: return null
        return project to Held(site, str(r.field(row, "site_at")) ?: "")
    }

    /**
     * Whether [copy] replaces [stored]: by S5's own stamp, never by arrival. Reads overlap -- a sync's list read
     * can land after the answer to a rename that went out later -- and S5 stamps every name it stores (`site_at`,
     * `Date.toISOString()`: one fixed-width UTC form, so the text orders as the instants do; '' for no name, below
     * every stamp). A copy at the same stamp is the same write and is taken.
     */
    fun takes(stored: Held?, copy: Held): Boolean = stored == null || copy.at >= stored.at

    // ---- the first value: the record's `site`, and the one-off seed ------------------------------------------

    /** The `site` an upload's record carries: the phone's name for the capture's site, or null (not sent) when it has none -- never the bucket. */
    fun recordSite(name: String?): String? = name?.trim()?.takeIf { named(it) }

    /** One seed: `PUT …/site {site, base: ""}` for [project]. */
    data class Seed(val project: String, val site: String)

    /**
     * The seed for a project already on S5 from before S5 kept site names: S5 lists it ([held] not null) with NO
     * name, and the phone knows one ([phoneSite], as the tree shows it). The base is '' , so the first write wins:
     * a phone that seeds second gets S5's 409 with the name the first one gave. Null otherwise -- S5 has a name,
     * S5 does not list the project, the phone has no name for it but the bucket's -- and never for the demo.
     */
    fun seed(project: String, client: String, title: String, held: Held?, phoneSite: String): Seed? {
        if (project.isBlank() || held == null || held.site.isNotEmpty()) return null
        if (TourDemo.isDemoProject(client, title) || TourDemo.isDemoKey(project)) return null
        val site = recordSite(phoneSite) ?: return null
        return Seed(project, site)
    }
}
