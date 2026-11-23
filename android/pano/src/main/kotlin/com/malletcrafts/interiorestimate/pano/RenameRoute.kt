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
 *  - S5'S LIST NOT HEARD YET (the A2 review, finding 1). "No name held for any project of the site" has two causes
 *    that must not be read as one: S5 does not hold the site, or this phone has not read S5's list since it began
 *    keeping S5's names -- the first minutes after the update, or longer with no signal. Read as the first, a site
 *    S5 holds opened as "S5 does not hold it" or was renamed on the phone alone and reported as renamed. So until
 *    a read of the list has landed ([Renames.route]'s `heard`), a site that MAY be on S5 (`mayHold`: a project of it
 *    carries a server id, or a capture of it a project id) takes [RenameRoute.S5] with no writes yet and
 *    [RenameRoute.S5.readFirst]: the Save reads S5's list and the route is asked again, now on S5's own answer.
 *    Signed out, the list cannot be read: [RenameRoute.Blocked] with the sign-in line. A site with nothing S5 could
 *    hold -- made on this phone and never sent -- keeps its routes, heard or not.
 *  - A CLIENT and a PROJECT keep the two routes they had (the phone's own row, else the bench by docname), with
 *    [RenameRoute.Blocked] in place of a call that cannot succeed: no stored ERP pair to call the bench with, or
 *    no row on this phone to rename. Their other defects (R10-R12, and R9's project and client thirds) are slice A3.
 *  - A CLIENT OR A PROJECT THAT CANNOT BE RENAMED SAYS SO BEFORE SAVE, AND A RENAME NEVER FORKS (port 63 slice F4b,
 *    23 Nov 2026; android/docs/port63/DESIGN-tree.md). Amit, with build 0.3.350 on his phone: "verify if renaming a
 *    site, client works actually correct", and what he had seen was "Rename did not take". Verified: on a phone with
 *    no ERP pair a client or a project that carries an id answered "Could not rename: no server configured" -- after
 *    Save, and of a phone that IS signed in; with a dead pair stored the call went out with the dead key; a
 *    phone-minted `S5-…` id was sent to the bench as a Project docname; and a client made on the phone was renamed
 *    on the phone alone and reported as renamed while the project S5 holds stayed under the old name, so two clients
 *    were listed and the next capture minted a second S5 project. So, for a client and a project:
 *      · a bench docname goes to the bench only with a LIVE pair ([Renames.route]'s `erpLive`: stored and not
 *        refused); without one the row is the office's record and is shut, saying so ([Renames.officeRecord]:
 *        app.client_office_record_renamed, app.project_office_record_renamed);
 *      · a phone-minted `S5-…` id ([Renames.minted]) is never a docname and never goes to the bench;
 *      · a row S5 holds anything of, or is being sent anything of ([Renames.route]'s `onS5`, [Renames.onS5]), is shut
 *        with the reason ([Renames.heldByS5]: app.client_s5_not_renamed, app.project_s5_not_renamed): S5 keeps no
 *        client name and has no route for a project's title, so a rename on the phone would not reach it. It is shut
 *        from the moment the dialog opens -- the name shown, not edited -- never refused after Save;
 *      · the phone's own row is renamed ([RenameRoute.Local]) only where nothing of it is on S5, and its capture
 *        rows that no server holds are renamed in the same step ([Renames.rekey]; audit R10), so the photos stay
 *        with the row and its first upload goes up under the new name, to ONE project.
 *    S5 keeping a client's name, and a project's title rename, are a later slice that needs Amit's yes.
 *  - ONE PROJECT, ONE SITE (port 63 slice F4c, 23 Nov 2026; DESIGN-tree.md §7). A site is the name each project
 *    carries, so a rename covers every project of a site and nothing gave ONE project a site: a project S5 listed
 *    with no site name sat in the "(no site)" bucket, which cannot be renamed. A project's dialog now has a Site
 *    row, and where a move goes is decided here as a rename's is ([Renames.moveRoute], [Renames.moveShut]): the same
 *    `PUT …/site`, for that one project, with its own base ('' for the bucket's); the phone's own row where nothing
 *    of it is on S5; never the bench.
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
     * (app.x_has_not_synced); [SHUT] a row that cannot be renamed here at all -- the name is shown and not edited,
     * Save is off, and the reason stands in the grey line's place: a site (app.site_not_renamed_here, P0 c), and
     * since port 63 slice F4b a client or a project that is the office's record or is on S5.
     */
    enum class Line { S5, SERVER, PHONE, SHUT }

    /**
     * Rename through S5: one `PUT …/site` per entry of [writes]. [readFirst]: a capture of the site is still under
     * an `S5-…` key the masters do not list (R9), or S5's list has not been read on this phone yet and [writes] is
     * still empty (the A2 review, finding 1) -- S5's list is read first and the route asked again. Nothing is sent
     * on a route with [readFirst]: the route asked again is the one that is followed.
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
     * @param erpLive     whether this phone holds a LIVE ERP pair -- stored, and not refused ([TreeKeep.erpLive]) --
     *                    for a client's or a project's bench rename. It took the place of "a pair is stored" in port
     *                    63 slice F4b: a refused pair cannot rename anything, and the call used to go out with it
     * @param unlisted    for a site: a capture of it is queued or uploaded under an `S5-…` key the masters do not list
     * @param listRead    S5's list has just been read for this Save, so [unlisted] is S5's own answer
     * @param heard       for a site: a read of S5's list has landed on this phone since it keeps S5's site names
     *                    (S5Sites.heard in :app). False, an empty [s5Projects] says nothing about S5
     * @param mayHold     for a site, read only while not [heard]: S5 may hold something of it -- a project under it
     *                    carries a server id, or a capture of it a project id (Catalogue.mayBeOnS5)
     * @param onS5        for a client or a project: S5 holds something of the row, or is being sent something of it
     *                    ([onS5], Catalogue.onS5 in :app). Never read for a site, which has its own route to S5
     */
    @JvmStatic
    fun route(
        tx: Tx, kind: String, name: String, serverId: String, hasLocalRow: Boolean,
        s5Projects: List<RenameRoute.Write>, signedIn: Boolean, erpLive: Boolean, unlisted: Boolean,
        listRead: Boolean = false, heard: Boolean = true, mayHold: Boolean = false, onS5: Boolean = false,
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
            // S5'S LIST NOT HEARD YET, and the site may be on it: "no project of it is named" is not S5's answer, so it
            // is neither "S5 does not hold it" (SHUT) nor a rename of the phone's own row (Local). The Save reads S5's
            // list first and the route is asked again; signed out, the list cannot be read, and that is said. A read
            // made for this Save IS S5's answer, whatever it named.
            if (!heard && !listRead && mayHold) {
                if (!signedIn) return RenameRoute.Blocked(tx.refuse_site_signed_out, RenameRoute.Line.S5)
                return RenameRoute.S5(emptyList(), readFirst = true)
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
        // A BENCH DOCNAME -- never a phone-minted `S5-…` key, which the bench has never held ("No such Project").
        if (id.isNotEmpty() && !minted(id)) {
            if (erpLive) return RenameRoute.Legacy(id)
            // No live pair: the call cannot be made, and the row is not this phone's to rename. Said from the moment
            // the dialog opens, in the row's own terms -- it used to be "Could not rename: no server configured",
            // after Save, on a phone that was signed in.
            return RenameRoute.Blocked(officeRecord(tx, kind), RenameRoute.Line.SHUT)
        }
        // S5 HOLDS SOMETHING OF IT, OR IS BEING SENT IT: a rename on the phone would not reach S5, so the project S5
        // holds would stay under the old name beside a copy under the new one -- two clients, and a second S5 project
        // at the next capture. Shut, with the reason, until S5 can be told a client's or a project's name.
        if (onS5 || minted(id)) return RenameRoute.Blocked(heldByS5(tx, kind), RenameRoute.Line.SHUT)
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

    /**
     * Why a client or a project that carries a bench docname is shut on a phone with no live ERP pair: it is the
     * office's record and is renamed there. A whole key per kind (a gendered Marathi word cannot share a template).
     */
    fun officeRecord(tx: Tx, kind: String): String =
        if (kind == "client") tx.app_client_office_record_renamed else tx.app_project_office_record_renamed

    /** Why a client or a project S5 holds something of is shut: the new name would not reach S5. A whole key per kind. */
    fun heldByS5(tx: Tx, kind: String): String =
        if (kind == "client") tx.app_client_s5_not_renamed else tx.app_project_s5_not_renamed

    /** How every phone-minted S5 project key begins (S5Auth.localKey in :app, pinned equal by RenameRouteTest). */
    const val MINTED = "S5-" // text:value a wire value, the phone-minted S5 project key's prefix

    /** Whether [id] is a phone-minted S5 project key and not a bench docname: such an id never goes to the bench. */
    fun minted(id: String): Boolean = id.trim().startsWith(MINTED)

    /**
     * WHETHER S5 HOLDS SOMETHING OF A ROW, OR IS BEING SENT SOMETHING OF IT -- the row a rename or a delete is aimed
     * at: a client with everything under it, a site, or one project.
     *
     * @param carried the project ids the row's projects and captures CARRY: the server id of each project the tree
     *                lists under the row, and the project id of each capture of the row ('' entries are ignored)
     * @param derived the keys the row's phone-made rows WOULD have on S5 (S5Auth.localKey of each): every one looks
     *                minted, so one counts only when S5's list has named it
     * @param listed  whether S5's list has named a project id on this phone (S5Sites holds it)
     *
     * A carried id counts when S5 lists it, or when it is a phone-minted key -- a capture keyed for S5 has gone up
     * under that key or is about to, and the key is a hash of the very names a rename would change. A derived key
     * counts only when S5 lists it: that is the project made on this phone that S5 holds, seen while nobody is
     * signed in and the masters no longer list it.
     */
    fun onS5(carried: List<String>, derived: List<String>, listed: (String) -> Boolean): Boolean =
        carried.any { it.isNotBlank() && (minted(it) || listed(it)) } || derived.any { it.isNotBlank() && listed(it) }

    /**
     * THE ROW'S CAPTURES FOLLOW ITS NAME ON A LOCAL RENAME (audit R10; port 63 slice F4b). A capture row carries the
     * client and the project title it was shot under, and nothing ever changed them: after a local rename the photos
     * stayed under the old name (the renamed project showed none), and the upload that followed went up under the
     * old name -- a second project, or the old client back beside the renamed one.
     *
     * The answer is the change for ONE capture: a "client" rename gives [to] to every capture of that client, a
     * "project" rename to every capture of that client and title (names as the catalogue compares them). A "site"
     * rename changes no capture: a capture carries no site. Left exactly as it is: a capture a server already holds
     * ([TreeKeep.Cap.sent]) and a capture that carries a project id -- that one is the project's, its names are the
     * server's to change, and a row with such a capture never takes the Local route ([onS5]).
     */
    fun rekey(kind: String, client: String, title: String, to: String): (TreeKeep.Cap) -> TreeKeep.Cap {
        val n = to.trim()
        return { c ->
            when {
                n.isEmpty() || c.sent || c.project.isNotBlank() -> c
                kind == "client" && key(c.client) == key(client) -> c.copy(client = n)
                kind == "project" && key(c.client) == key(client) && key(c.title) == key(title) -> c.copy(title = n)
                else -> c
            }
        }
    }

    /** The line for a bench rename (a client's or a project's) that the bench refused or could not be reached for: its own words, as the banner used to carry them. */
    fun benchRefused(tx: Tx, why: String?): String = tx.app_msg_could_not_rename_x(why = why.orEmpty())

    // ---- one project, one site: the "Site" row of a project's dialog (port 63 slice F4c) --------------------

    /**
     * WHY ONE PROJECT'S SITE CANNOT BE CHANGED FROM THIS PHONE AT ALL -- asked when the project's dialog opens, before
     * any site is picked; null when it can be. The Site row is then drawn without its picker, the sentence under it.
     *
     * Amit, 23 Nov 2026, with build 0.3.350 on his phone, said of one of his own projects that it belongs to one flat
     * and no other (his words name the project and the flat, so they are in the session's notes, not here), and:
     * "site switch is not happening correctly. fix it." That project sat outside its flat's site and nothing on the
     * phone could put it there: the "(no site)" bucket cannot be renamed, the seed and the upload send nothing for
     * it, and no screen moved a project. So a project carries its own Site row (android/docs/port63/DESIGN-tree.md
     * §7), and where that row goes is [moveRoute]. It is shut only where no move could be true:
     *  - the demo's project, and a row that is neither S5's, nor an entry of the masters, nor made on this phone;
     *  - an entry of an ERP bootstrap that S5 does not list, on a phone WITH a live ERP pair: its site is the
     *    bench's, the bench moves no project (it only fills a site that is blank), and the next bootstrap would put
     *    the old site back over anything changed here.
     * One sentence for all of them (app.project_site_not_changed_here), the project's twin of the site's own
     * app.site_not_renamed_here: S5 does not hold the project and it was not made here.
     *
     * @param project     the id S5 knows the project by ('' when this phone knows none)
     * @param held        S5's name for the project's site as this phone last heard it; null when S5's list has not
     *                    named the project (S5Sites holds no entry for [project])
     * @param hasLocalRow a row made on this phone carries the project
     * @param mastersOnly the tree lists the project from an entry of an ERP bootstrap that S5 does not list
     * @param erpLive     this phone holds a LIVE ERP pair ([TreeKeep.erpLive])
     * @param demo        it is the demo's project
     */
    fun moveShut(tx: Tx, project: String, held: Held?, hasLocalRow: Boolean, mastersOnly: Boolean, erpLive: Boolean, demo: Boolean): String? = when {
        demo -> tx.app_project_site_not_changed_here
        held != null && project.isNotBlank() -> null
        mastersOnly -> if (erpLive) tx.app_project_site_not_changed_here else null
        hasLocalRow -> null
        else -> tx.app_project_site_not_changed_here
    }

    /**
     * WHERE A MOVE OF ONE PROJECT TO ANOTHER SITE GOES, decided before anything is moved anywhere. A site is not an
     * entity on S5 or on the phone: it is the name each project carries, so "move this project to that site" is "give
     * this ONE project that name" -- by the very write a site's rename makes once per project
     * (`PUT /api/asis/:project/site {site, base}`), here made for one project with that project's own base.
     *
     *  - S5 LISTS THE PROJECT ([held] not null) -> [RenameRoute.S5] with ONE write, its base S5's name for THIS
     *    project exactly as S5 gave it ('' when S5 holds none: the bucket's projects). Signed out: the sign-in line,
     *    known on the phone, so nothing is sent.
     *  - AN ENTRY OF AN ERP BOOTSTRAP S5 DOES NOT LIST, on a phone with no live pair -- kept only because a capture of
     *    it has not reached a server yet ([TreeKeep.keepsProject]) -> [RenameRoute.Local]: the phone's copy of the
     *    entry takes the name, and its first upload carries it to S5 as S5's first value. With a live pair it is shut
     *    ([moveShut]).
     *  - A ROW MADE ON THIS PHONE that S5's list has not named -> [RenameRoute.Local]: the row is words on this phone.
     *    But where a capture of it already carries a phone-minted S5 key ([onWay]) the key is a hash of
     *    client|site|title, and a row moved now would mint a NEW key at its next capture -- two S5 projects (audit R9,
     *    as for a site). The capture has usually gone up already and only S5's list has not been read since; so S5's
     *    list is read first ([RenameRoute.S5] with no writes yet and readFirst) and the route asked again with
     *    [listRead]: listed now, it is S5's project and takes the write; still not, the move is refused until it is.
     *  - [moveShut]'s cases -> [RenameRoute.Blocked] with [RenameRoute.Line.SHUT].
     *  - THE TARGET: a blank name, the "(no site)" bucket (a bucket is not a place a project is moved to) and the
     *    site the project is already at are refused, saying to pick another -- never sent, never "moved".
     *  - Never the bench ([RenameRoute.Legacy]): it is not told of a move, as it is not told of a site's rename.
     *
     * @param from     the site the tree lists the project under now (the bucket's name for none)
     * @param to       the site picked or typed ([moveTarget])
     * @param signedIn this phone holds something to call S5 with
     * @param onWay    for a row made on this phone: a capture of it carries a phone-minted S5 key that S5's list has
     *                 not named on this phone
     * @param listRead S5's list has just been read for this move, so "not named" is S5's own answer
     * The other parameters are [moveShut]'s.
     */
    fun moveRoute(
        tx: Tx, project: String, held: Held?, hasLocalRow: Boolean, mastersOnly: Boolean,
        erpLive: Boolean, signedIn: Boolean, demo: Boolean, from: String, to: String,
        onWay: Boolean = false, listRead: Boolean = false,
    ): RenameRoute {
        moveShut(tx, project, held, hasLocalRow, mastersOnly, erpLive, demo)?.let { return RenameRoute.Blocked(it, RenameRoute.Line.SHUT) }
        val site = to.trim()
        if (!named(site) || key(site) == key(from)) return RenameRoute.Blocked(tx.app_not_moved_pick_another_site, RenameRoute.Line.PHONE)
        if (held != null && project.isNotBlank()) {
            if (!signedIn) return RenameRoute.Blocked(tx.refuse_site_signed_out, RenameRoute.Line.S5)
            return RenameRoute.S5(listOf(RenameRoute.Write(project.trim(), held.site)), readFirst = false)
        }
        // The phone's copy of a bootstrap entry (no live pair: moveShut), which its first upload carries to S5.
        if (mastersOnly) return RenameRoute.Local
        if (onWay) {
            if (!signedIn) return RenameRoute.Blocked(tx.refuse_site_signed_out, RenameRoute.Line.S5)
            if (listRead) return RenameRoute.Blocked(tx.app_project_capture_on_way_not_moved, RenameRoute.Line.S5)
            return RenameRoute.S5(emptyList(), readFirst = true)
        }
        return RenameRoute.Local
    }

    /**
     * The sites a project can be moved to by a tap: the client's NAMED sites other than the one it is at, in the
     * tree's order, each once (names as the catalogue compares them). Never the "(no site)" bucket: nothing is moved
     * into it. A new site is typed ([moveTarget]).
     */
    fun siteChoices(sites: List<String>, current: String): List<String> =
        sites.map { it.trim() }.filter { named(it) && key(it) != key(current) }.distinctBy { key(it) }

    /**
     * The site a typed or picked name MEANS: one of the client's sites in that site's own spelling where the name is
     * one of them as the catalogue compares names -- so "flat 9" typed for a client who has "Flat 9" joins that site
     * and does not give S5 a second spelling of it -- else the name as typed, trimmed.
     */
    fun moveTarget(typed: String, sites: List<String>): String {
        val name = typed.trim()
        return sites.firstOrNull { named(it) && key(it) == key(name) }?.trim() ?: name
    }

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

    // ---- the site's chosen rooms follow the name S5 gives it (audit R7; the A2 review, finding 2) ------------

    /** One project as the tree lists it: its S5 project id ('' for a row only this phone holds), its client, and the site it is listed under. */
    data class Placed(val project: String, val client: String, val site: String)

    /**
     * The rooms chosen for [client]'s site [from] are [to]'s as well, where [to] has no list of its own. [leave]:
     * nothing is listed under [from] any more, so its own list goes; false while a project or a row of this phone's
     * is still listed there -- that site keeps its rooms.
     */
    data class ScopeStep(val client: String, val from: String, val to: String, val leave: Boolean)

    /**
     * WHERE THE CHOSEN ROOMS GO WHEN S5'S NAME FOR A PROJECT CHANGES ON THIS PHONE. The rooms a site has are kept
     * under the site's NAME, and the tree lists a project under S5's name the moment the phone holds it -- from this
     * phone's own Save, and just as much from a list read that hears another phone's rename, a 409's name as stored,
     * or the one project that took the name on "1 of 2". Moved only in the dialog's success step, the list stayed
     * under the old name in the other three: the rooms grid fell back to the 2 BHK preset and the chooser asked
     * again, which is R7.
     *
     * [before] and [after] are the tree's projects just before and just after S5's names were written; [moved] the
     * S5 project ids whose name that write changed. One step for each (client, old site, new site) such a project
     * went through -- compared as the catalogue compares names ([key]), so a change of case or spacing alone is no
     * step. The "(no site)" bucket is a bucket, not a site: nothing is taken from it or given to it. A project of
     * another write, or one whose listing changed for another reason, gives no step.
     */
    fun scopeSteps(before: List<Placed>, after: List<Placed>, moved: Set<String>): List<ScopeStep> {
        val out = LinkedHashMap<String, ScopeStep>()
        for (b in before) {
            if (b.project.isBlank() || b.project !in moved) continue
            val a = after.firstOrNull { it.project == b.project && key(it.client) == key(b.client) } ?: continue
            if (!named(b.site) || !named(a.site) || key(a.site) == key(b.site)) continue
            val leave = after.none { key(it.client) == key(b.client) && key(it.site) == key(b.site) }
            out.getOrPut(key(b.client) + "\u0000" + key(b.site) + "\u0000" + key(a.site)) { ScopeStep(b.client, b.site, a.site, leave) }
        }
        return out.values.toList()
    }

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
