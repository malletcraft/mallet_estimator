package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * THE CLIENT'S NAME IS KEPT ON S5, AND A CLIENT RENAME REALLY WORKS (port 63 slice 5, 24 Nov 2026; Amit: "Client
 * rename"; android/docs/port63/DESIGN-tree.md §11).
 *
 * Until this slice the client's name lived in each phone's capture rows and nowhere else: a reinstall, or clearing
 * the app's storage, lost it, and a client S5 held a project of could not be renamed at all (slice F4b shut the
 * dialog, saying so). S5 now keeps the name ON THE PROJECT (S5 QUOTE slice 5: `client_name`, `client_name_by`,
 * `client_name_at`), by the site name's rule word for word:
 *
 *  - staff's list, project GET and room GET carry the three keys ('' while S5 holds none); a vendor's or a client's
 *    login is given NONE of them, so a row without the key says nothing ([listedOf]);
 *  - a room record's optional `client_name` gives a FIRST name only -- an upload never renames ([recordName]);
 *  - `PUT /api/asis/:project/client {client_name, base}` renames ONE project's client, the base being the name S5
 *    last gave for that project exactly ('' while it holds none) ([request], [verdict]).
 *
 * `client_name` is a LABEL. It is not `customer`, the ERP Customer id S5 grants a client's own login by: nothing
 * here reads or writes that id as a name, and a tree row that shows the id because no name is known never sends
 * the id as the name ([recordName]).
 *
 * The phone side copies the site name's shape and adds no second way:
 *
 *  - S5's name for a project has ONE home on the phone (S5Clients in :app, as S5Sites is the site names'), fed by
 *    every read of S5's list and every answer to a rename, kept by S5's own stamp ([Renames.takes]). The tree reads
 *    it FIRST ([nameOf]), so a phone with nothing of its own -- a reinstall -- lists each S5 project under its client.
 *  - THE PHONE'S OWN ROWS FOLLOW S5'S NAME WHEREVER IT IS WRITTEN ([steps], [follows]). A site's name is read from
 *    S5 each time, but a client's name is also what the phone's capture rows, its own project rows and its remembered
 *    S5 keys are FILED under -- so the moment S5's name for a project is taken, those rows move to it. This phone's
 *    own Save, another phone's rename heard by a list read, a 409's name as stored and each project of "1 of 2" all
 *    go through the one step: the tree shows what S5 holds, and a later capture is the same S5 project.
 *  - A RENAME IS ONE WRITE PER PROJECT, each with that project's own base, and its result is honest ([outcome],
 *    [text]): "Renamed" only when every project took the name; otherwise how many did, and for each one left, why.
 *  - WHERE A RENAME GOES is decided before anything is sent ([route]), ahead of [Renames.route], whose answers for a
 *    client stand wherever S5 lists no project of it.
 *
 * A PROJECT'S OWN TITLE is still not renamed where S5 holds the project: S5 has no route for a title, and the
 * dialog stays shut with slice F4b's reason ([Renames.heldByS5]). Only the client's name moved.
 *
 * Pure JVM: every rule here is tested in ClientNamesTest; :app only stores, sends and draws.
 */
object ClientNames {

    /** The field S5 keeps the client's name in, on the list, in a record and in the rename's body and answers. */
    const val KEY = "client_name" // text:value a wire key

    /** S5's cap on a label, in UTF-16 units as S5 counts them (asis.js SITE_MAX). */
    const val MAX = 140

    private fun key(s: String): String = Renames.key(s)

    /** Whether [name] is a client's own name: not blank, and not the "(no client)" bucket ([TreeNav.NO_CLIENT]). */
    fun named(name: String?): Boolean = !name.isNullOrBlank() && key(name) != key(TreeNav.NO_CLIENT)

    // ---- the name S5 takes: asis.js oneLine and validLabel, the rule the site's name is held to ----------------

    /** [typed] on one line as S5 folds it (asis.js oneLine): split at line breaks, each piece trimmed, the empty ones dropped, joined by one space. */
    fun fold(typed: String): String =
        typed.split('\r', '\n').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")

    /**
     * Whether S5 takes [name] (already [fold]ed) as a label: 1 to [MAX] UTF-16 units, no control character
     * (U+0000-001F, U+007F-009F), no line or paragraph separator (U+2028, U+2029) and no half of a surrogate pair on
     * its own (asis.js SITE_BAD). Asked BEFORE anything is sent, so a name S5 would refuse with a 400 is refused on
     * the phone and no project of the client is touched.
     */
    fun ok(name: String): Boolean {
        if (name.isEmpty() || name.length > MAX) return false
        var i = 0
        while (i < name.length) {
            val c = name[i]
            val code = c.code
            if (code <= 0x1f || code in 0x7f..0x9f || code == 0x2028 || code == 0x2029) return false
            if (c.isHighSurrogate()) {
                if (i + 1 >= name.length || !name[i + 1].isLowSurrogate()) return false
                i += 2
                continue
            }
            if (c.isLowSurrogate()) return false
            i++
        }
        return true
    }

    /** The name a Save sends for what was [typed]: folded, and null when S5 would refuse it ([ok]). */
    fun typed(typed: String): String? = fold(typed).takeIf { ok(it) }

    // ---- the wire: PUT /api/asis/:project/client {client_name, base} (S5 QUOTE slice 5, functions/_lib/asis.js) --

    /** The request for one project of the client: the new name, and the name S5 last gave for THIS project exactly ('' while it holds none). */
    fun request(project: String, name: String, base: String): AlbumQueue.Request =
        AlbumQueue.Request("PUT", listOf(project, "client"), linkedMapOf<String, Any?>(KEY to name, "base" to base), AlbumQueue.JSON) // text:value the route's last segment and a wire key

    /** S5's verdict on one `PUT …/client`: the site route's answers, under `client_name` ([Renames.labelVerdict]). [Renames.Now.site] is the client's name. */
    fun verdict(r: AlbumWire.Read, code: Int?, body: Any?): Renames.Verdict = Renames.labelVerdict(r, code, body, KEY)

    /**
     * One row of `GET /api/asis` -> the project and S5's name for its client; null for a row with no project, or
     * with no `client_name` text -- an S5 before its slice 5, or a login that is not staff, which S5 gives none of
     * the three keys. Such a row is not "listed with no name": it says nothing, and nothing is taken or sent on it.
     */
    fun listedOf(r: AlbumWire.Read, row: Any?): Pair<String, Renames.Held>? {
        val project = (r.field(row, "project") as? String)?.takeIf { it.isNotBlank() } ?: return null
        val name = r.field(row, KEY) as? String ?: return null
        return project to Renames.Held(name, r.field(row, KEY + "_at") as? String ?: "") // text:value the wire key's suffix
    }

    // ---- reading: S5's name first ------------------------------------------------------------------------------

    /**
     * The client a project is listed under: S5's name for it where S5 holds one ([s5]: null when S5 gave none for
     * the project, '' when it lists the project with no name), else [phone] -- the name the masters entry carries,
     * as the tree read it before this slice. So a project S5 holds no name for keeps the behaviour it had.
     */
    fun nameOf(s5: String?, phone: String): String = s5?.trim()?.takeIf { it.isNotEmpty() } ?: phone

    // ---- the first value: the record's `client_name`, and the backfill -----------------------------------------

    /**
     * The `client_name` an upload's record carries: the client's name as the phone holds it for the capture's
     * project, or null (the key is not sent) when it holds none. Never blank, never the "(no client)" bucket, never
     * the demo client, and never [customerId] -- an S5 project with a customer and no name is listed under the ERP
     * Customer ID, and an id sent as the name would be kept as one. S5 takes the value only while it holds none.
     */
    fun recordName(name: String?, customerId: String): String? {
        val n = name?.trim().orEmpty()
        if (!named(n) || key(n) == key(TourDemo.CLIENT)) return null
        if (customerId.isNotBlank() && key(n) == key(customerId)) return null
        return n
    }

    /** One backfill: `PUT …/client {client_name, base: ""}` for [project]. */
    data class Seed(val project: String, val name: String)

    /**
     * The backfill for a project S5 held before it kept client names: S5 lists it to this login WITH the key
     * ([held] not null: staff, and an S5 that serves it) and NO name, and the phone holds one ([client], as the tree
     * lists the project; [customerId] the project's ERP Customer id, '' for none). The base is '', so the first
     * write wins: a phone that comes second gets S5's 409 with the name the first one gave, and takes it. Null
     * otherwise -- and for a name S5 would refuse, which would be refused again at every sync. Never the demo.
     */
    fun seed(project: String, client: String, title: String, held: Renames.Held?, customerId: String): Seed? {
        if (project.isBlank() || held == null || held.site.isNotEmpty()) return null
        if (TourDemo.isDemoProject(client, title) || TourDemo.isDemoKey(project)) return null
        val name = typed(recordName(client, customerId) ?: return null) ?: return null
        return Seed(project, name)
    }

    // ---- where a client's rename goes --------------------------------------------------------------------------

    /**
     * WHERE A CLIENT'S RENAME GOES, asked BEFORE [Renames.route]; null means "not S5's -- the routes a client had"
     * (the phone's own row, the bench with a live pair, or shut with slice F4b's reasons).
     *
     * @param name       the client as the tree lists it
     * @param customerId the ERP Customer id its projects carry, '' for none
     * @param erpLive    whether this phone holds a LIVE ERP pair ([TreeKeep.erpLive])
     * @param writes     the client's projects that S5 lists AND gives a client-name key for, each with S5's own name
     *                   for it as the base ('' while S5 holds none)
     * @param signedIn   whether this phone holds anything to call S5 with
     * @param listRead   S5's list has just been read for this Save, so [writes] and their bases are S5's own answer
     *
     *  - No such project: null. A client whose projects are only on this phone, only on their way to S5, or listed
     *    by an S5 that keeps no client name is not renamed through S5.
     *  - The demo client: null. The "(no client)" bucket: shut -- a bucket has no name to change, and a rename would
     *    name every unnamed project at once.
     *  - A bench Customer with a live pair: null -- the bench rename a client had ([RenameRoute.Legacy]).
     *  - Signed out: said at once, nothing sent.
     *  - Else [RenameRoute.S5], one write per project. It ALWAYS reads S5's list first ([RenameRoute.S5.readFirst]
     *    until [listRead]): each base must be the name S5 holds NOW, a project another phone gave this client since
     *    the last read is renamed with the rest, and "offline" is said before anything is sent.
     */
    fun route(
        tx: Tx, name: String, customerId: String, erpLive: Boolean, writes: List<RenameRoute.Write>,
        signedIn: Boolean, listRead: Boolean = false,
    ): RenameRoute? {
        val ws = writes.filter { it.project.isNotBlank() }.distinctBy { it.project }
        if (ws.isEmpty() || name.isBlank() || key(name) == key(TourDemo.CLIENT)) return null
        if (!named(name)) return RenameRoute.Blocked(tx.app_client_bucket_not_named, RenameRoute.Line.SHUT)
        val id = customerId.trim()
        if (erpLive && id.isNotEmpty() && !Renames.minted(id)) return null
        if (!signedIn) return RenameRoute.Blocked(tx.refuse_site_signed_out, RenameRoute.Line.S5)
        return RenameRoute.S5(ws, readFirst = !listRead)
    }

    /** The grey line of a rename that goes to S5, per kind: the client's own sentence, else the site's. */
    fun s5Line(tx: Tx, kind: String): String =
        if (kind == "client") tx.app_client_on_s5_so_new else tx.app_site_on_s5_so_new // text:value the wire kind, compared

    // ---- the phone's rows follow the name S5 holds -------------------------------------------------------------

    /** [project]'s rows on this phone, filed under [from] with the title [title], are [to]'s from now on. */
    data class Step(val project: String, val from: String, val title: String, val to: String)

    /**
     * WHAT MOVES WHEN S5'S NAME FOR [project] IS TAKEN AS [to]. [held] is every client and title this phone files
     * the project under just before the write: the tree's entry, each capture that carries the project's id, and
     * the names its S5 key is remembered for. One step for each such pair filed under ANOTHER client (names as the
     * catalogue compares them: a change of case or spacing alone moves nothing). No step when S5 holds no name, or
     * holds the bucket's.
     */
    fun steps(project: String, to: String, held: List<KeyBook.Names>): List<Step> {
        val n = to.trim()
        if (project.isBlank() || !named(n)) return emptyList()
        return held
            .filter { it.client.isNotBlank() && it.title.isNotBlank() && key(it.client) != key(n) }
            .distinctBy { KeyBook.fold(it.client, it.title) }
            .map { Step(project.trim(), it.client.trim(), it.title.trim(), n) }
    }

    /**
     * [steps] for ONE capture: a capture that carries a step's project id is that project's, whatever it has been
     * sent or not -- its client is the name S5 holds; a capture with no id yet, shot under a step's old client and
     * title, is the same project's and follows too. Every other capture, and the demo's, is left exactly as it is.
     */
    fun follows(steps: List<Step>): (TreeKeep.Cap) -> TreeKeep.Cap = { c ->
        val s = if (TourDemo.isDemoProject(c.client, c.title)) null
        else if (c.project.isNotBlank()) steps.firstOrNull { it.project == c.project.trim() }
        else steps.firstOrNull { key(it.from) == key(c.client) && key(it.title) == key(c.title) }
        if (s == null || c.client == s.to) c else c.copy(client = s.to)
    }

    /**
     * EVERY project of the client took the name: what is still filed under [from] on this phone -- a project made
     * here and never sent, a capture on its way to S5, one whose project S5 no longer lists -- is [to]'s as well,
     * sent or not, keyed or not (slice F4b's local re-key left those alone because S5 could not be told; now it has
     * been). For one capture; the demo's is left as it is.
     */
    fun rekeyDone(from: String, to: String): (TreeKeep.Cap) -> TreeKeep.Cap {
        val n = to.trim()
        return { c ->
            if (n.isEmpty() || c.client == n || key(c.client) != key(from) || TourDemo.isDemoProject(c.client, c.title)) c
            else c.copy(client = n)
        }
    }

    // ---- what one Save came to ---------------------------------------------------------------------------------

    /** One project that did not take the name: [base] as it was sent, [verdict] S5's answer -- null when the Save stopped before it was sent. */
    data class Left(val project: String, val base: String, val verdict: Renames.Verdict?)

    sealed interface Outcome {
        /** Every project of the client carries the name: [name] as S5 stored it. */
        data class Done(val name: String) : Outcome

        /** [done] of [of] projects carry the name; [left] are the others, in the order they were sent. */
        data class Part(val done: Int, val of: Int, val left: List<Left>) : Outcome
    }

    private fun took(v: Renames.Verdict?): Boolean = v is Renames.Verdict.Renamed || v is Renames.Verdict.Same

    /**
     * [verdicts] are S5's answers to [writes], in order; fewer when the Save stopped ([Renames.stops]). Success is
     * S5's answers ALONE: every project answered Renamed or Same. Anything else is [Outcome.Part] and names each
     * project left -- with no writes at all, nothing was renamed and nothing is left.
     */
    fun outcome(writes: List<RenameRoute.Write>, verdicts: List<Renames.Verdict>): Outcome {
        val left = writes.mapIndexedNotNull { i, w ->
            val v = verdicts.getOrNull(i)
            if (took(v)) null else Left(w.project, w.base, v)
        }
        val name = verdicts.firstNotNullOfOrNull { v -> if (took(v)) Renames.nowIn(v)?.site else null }
        if (writes.isNotEmpty() && left.isEmpty() && name != null) return Outcome.Done(name)
        return Outcome.Part(writes.size - left.size, writes.size, left)
    }

    /** Why ONE project did not take the name, as a sentence of its own: S5's 409 from its fields, a 401 as the sign-in line, no answer as the offline line, any other refusal in S5's own words. */
    private fun why(tx: Tx, l: Left): String = when (val v = l.verdict) {
        is Renames.Verdict.Stale ->
            if (renamedMeanwhile(l, v)) tx.refuse_site_renamed_x(by = v.now.by, site = v.now.site) else v.error
        is Renames.Verdict.Refused -> if (v.code == 401) tx.refuse_site_signed_out else v.error
        else -> tx.refuse_site_offline
    }

    /** S5's 409 for a name somebody set meanwhile: a name is stored, it is not the base sent, and S5 says who set it. */
    private fun renamedMeanwhile(l: Left, v: Renames.Verdict.Stale): Boolean =
        v.now.site.isNotEmpty() && v.now.site != l.base && v.now.by.isNotBlank()

    /**
     * The lines under the name for [o]; null when the client is renamed. [titleOf] gives a project's title as the
     * tree lists it (its id where the tree has none).
     *
     * NOTHING renamed, and the answer is the caller's or the only project's -- no signal, signed out, not staff, the
     * one project refused: that one sentence, as a site's rename says it. Otherwise it is said how many took the
     * name, then ONE LINE FOR EACH PROJECT LEFT: who renamed it meanwhile and the name S5 holds; that S5 does not
     * list it; S5's own words or the offline / sign-in line; or that it was not sent because the Save had stopped.
     */
    fun text(tx: Tx, o: Outcome, titleOf: (String) -> String): String? = when (o) {
        is Outcome.Done -> null
        is Outcome.Part -> {
            val first = o.left.firstOrNull()
            if (first == null) tx.refuse_site_offline
            else if (o.done == 0 && (o.of == 1 || first.verdict == null || Renames.stops(first.verdict))) why(tx, first)
            else (listOf(tx.refuse_client_part_x(done = o.done, of = o.of)) + o.left.map { l ->
                val p = titleOf(l.project).ifBlank { l.project }
                when (val v = l.verdict) {
                    null -> tx.refuse_client_left_not_sent_x(p)
                    is Renames.Verdict.Stale ->
                        if (renamedMeanwhile(l, v)) tx.refuse_client_left_renamed_x(project = p, by = v.now.by, client = v.now.site)
                        else tx.refuse_client_left_said_x(project = p, why = v.error)
                    is Renames.Verdict.Refused ->
                        if (v.code == 404) tx.refuse_client_left_gone_x(p)
                        else tx.refuse_client_left_said_x(project = p, why = why(tx, l))
                    else -> tx.refuse_client_left_said_x(project = p, why = why(tx, l))
                }
            }).joinToString("\n")
        }
    }
}
