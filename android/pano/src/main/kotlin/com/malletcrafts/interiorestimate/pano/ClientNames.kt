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
 * THE REVIEW OF SLICE 5 (24 Nov 2026; DESIGN-tree.md §11.6) added what a Save decides beyond the route:
 *
 *  - S5 AND THE BENCH ARE BOTH WRITTEN where both hold the client ([bench]): the bench first -- it refuses a name
 *    another Customer holds, and then nothing has been sent anywhere -- then S5; what S5 then leaves is said with
 *    the bench's state in front ([text] with `office`), and never as "Renamed".
 *  - A CLIENT IS NEVER RENAMED ONTO ANOTHER CLIENT'S PROJECT OF THE SAME TITLE ([refusal]): the phone finds an S5
 *    project by client and title, so two projects under one such pair cannot be told apart again.
 *  - THE NAMES ARE STAFF'S, AND ONE LOGIN'S ([owner], [kept], [forgets], [shown]): kept for the login that read
 *    them and for nobody else -- not for a signed-out phone, the next person, or a login S5 gives no name to.
 *  - EVERY SAVE STARTS FROM THE LIST JUST READ ([writes]): the client's projects are worked out again each time.
 *  - AFTER THE LIST READ ONLY WHAT IS TRUE IS SAID ([next], [notRead]): "offline" when the read failed, and
 *    otherwise the route's own answer.
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
     * @param name     the client as the tree lists it
     * @param writes   the client's projects that S5 lists AND gives a client-name key for, each with S5's own name
     *                 for it as the base ('' while S5 holds none) ([writes])
     * @param signedIn whether this phone holds anything to call S5 with
     * @param listRead S5's list has just been read for this Save, so [writes] and their bases are S5's own answer
     *
     *  - No such project: null. A client whose projects are only on this phone, only on their way to S5, or listed
     *    by an S5 that keeps no client name is not renamed through S5.
     *  - The demo client: null. The "(no client)" bucket: shut -- a bucket has no name to change, and a rename would
     *    name every unnamed project at once.
     *  - Signed out: said at once, nothing sent.
     *  - Else [RenameRoute.S5], one write per project. It ALWAYS reads S5's list first ([RenameRoute.S5.readFirst]
     *    until [listRead]): each base must be the name S5 holds NOW, a project another phone gave this client since
     *    the last read is renamed with the rest, and "offline" is said before anything is sent.
     *
     * A BENCH CUSTOMER WITH A LIVE PAIR IS NO WAY ROUND S5 (the review of slice 5, fault 1). It used to answer null
     * here -- "the bench rename it had" -- so the bench alone was renamed, the screen said "Renamed", and the tree,
     * which reads S5's name first, went on listing the client under the name S5 still held; the next read of S5's
     * list moved the phone's rows back to it. Wherever S5 holds a name key for a project of the client the rename
     * goes to S5, and the bench is told beside it ([bench]).
     */
    fun route(
        tx: Tx, name: String, writes: List<RenameRoute.Write>, signedIn: Boolean, listRead: Boolean = false,
    ): RenameRoute? {
        val ws = writes.filter { it.project.isNotBlank() }.distinctBy { it.project }
        if (ws.isEmpty() || name.isBlank() || key(name) == key(TourDemo.CLIENT)) return null
        if (!named(name)) return RenameRoute.Blocked(tx.app_client_bucket_not_named, RenameRoute.Line.SHUT)
        if (!signedIn) return RenameRoute.Blocked(tx.refuse_site_signed_out, RenameRoute.Line.S5)
        return RenameRoute.S5(ws, readFirst = !listRead)
    }

    /**
     * THE CLIENT'S PROJECTS A SAVE WRITES, worked out again AT EVERY SAVE (the review of slice 5, fault 5): [keys]
     * are the S5 projects the tree lists under the client NOW -- after the read of S5's list that Save made -- and
     * [held] gives S5's own name for each (null where S5 gave this login no client-name key: not a write). One
     * write per project, each with that project's own base.
     *
     * The first Save's list used to be kept for the dialog's later Saves. After "1 of 2" the project that took the
     * name is the new client's and is not sent again; a project another phone gave the old client meanwhile IS
     * sent; and one S5 no longer lists is not sent to be refused.
     */
    fun writes(keys: Collection<String>, held: (String) -> Renames.Held?): List<RenameRoute.Write> =
        keys.map { it.trim() }.filter { it.isNotEmpty() }.distinct().mapNotNull { id -> held(id)?.let { RenameRoute.Write(id, it.site) } }

    /**
     * THE BENCH CUSTOMER THE SAME SAVE MUST ALSO RENAME (the review of slice 5, fault 1): [customerId] when this
     * phone holds a LIVE ERP pair ([erpLive], [TreeKeep.erpLive]) and the id is a bench docname -- not blank, not a
     * phone-minted `S5-…` key; null otherwise, and then S5 alone holds the client's name.
     *
     * The bench is told FIRST. It refuses a name another Customer already holds, and a refusal there leaves S5
     * untouched: nothing was renamed anywhere, and it is said in the bench's own words ([Renames.benchRefused]).
     * Told the same name twice it answers the same (rename_node sets the Customer's name and keeps its docname), so
     * a Save repeated after S5 left a project behind tells it again and harms nothing.
     */
    fun bench(customerId: String, erpLive: Boolean): String? {
        val id = customerId.trim()
        return id.takeIf { erpLive && it.isNotEmpty() && !Renames.minted(it) }
    }

    // ---- what a Save does once S5's list has been read ----------------------------------------------------------

    /** What a client's Save does next, S5's list having been read for it ([next]). */
    sealed interface Next {
        /** One `PUT …/client` for each of [writes] -- after the bench, where the client is also a bench Customer ([bench]). */
        data class Send(val writes: List<RenameRoute.Write>) : Next

        /** S5 lists no project of the client any more and a row made here carries it: renamed on the phone. */
        data object Here : Next

        /** S5 lists no project of the client any more and it is a bench Customer with a live pair: the bench alone, judged on the bench's answer. */
        data class Bench(val docname: String) : Next

        /** Nothing is sent, and [reason] is why -- the route's own sentence. */
        data class Say(val reason: String) : Next
    }

    /**
     * THE ROUTE ASKED AGAIN AFTER THE LIST READ, as what the Save then does (the review of slice 5, fault 6). Every
     * route has its own answer: the dialog used to say "Offline" for any route that was neither S5 nor shut -- with
     * the list read a second before -- and the bench route was one of them.
     *
     * @param route the answer of [route], else of [Renames.route] for a client, with the list read
     * @param name  the client as the tree lists it
     */
    fun next(tx: Tx, route: RenameRoute, name: String): Next = when (route) {
        // an S5 route with nothing to write is no rename (neither rule gives one for a client; said, never sent)
        is RenameRoute.S5 -> if (route.writes.isEmpty()) Next.Say(Renames.nothingRenamed(tx, "client", name)) else Next.Send(route.writes) // text:value the wire kind
        RenameRoute.Local -> Next.Here
        is RenameRoute.Legacy -> Next.Bench(route.docname)
        is RenameRoute.Blocked -> Next.Say(route.reason)
    }

    /** S5's list could not be read for a Save: the sign-in line where this phone holds nothing to call S5 with, else the offline line. Nothing was sent. */
    fun notRead(tx: Tx, signedIn: Boolean): String = if (signedIn) tx.refuse_site_offline else tx.refuse_site_signed_out

    // ---- a client is never renamed onto another client's project of the same title ------------------------------

    /**
     * WHY [from] CANNOT BE RENAMED TO [to], asked before anything is sent and again on the list a Save read (the
     * review of slice 5, fault 2); null when it can.
     *
     *  - [to] is the "(no client)" bucket or the demo client: refused. Neither is a client's name -- the bucket
     *    cannot be renamed at all, so a client given its name could never be given another.
     *  - [to] ALREADY LISTS A PROJECT WITH THE TITLE OF ONE OF [from]'s: refused, naming it. The phone finds a
     *    project by its client and title (S5Auth.keyFor, the remembered keys, the capture rows), so two projects
     *    under one client with one title cannot be told apart again: the next capture of either goes to whichever
     *    is found first. The site move refuses its like ([Renames.moveRoute]'s `twin`).
     *  - A rename onto an existing client with NO title in common is a MERGE, and it is allowed: every project
     *    keeps its own key and title, so each row, capture and wish list is still found under the one name.
     *  - A change of case or spacing alone is the same client, never a clash with itself.
     *
     * @param rows  every project the tree lists, as client to title
     * @param begun the name THIS dialog has already begun giving the client (a Save that S5 took for some projects,
     *              or the bench took): the projects that moved are the client's own, so the Save that finishes the
     *              rename is not refused for them. Null when the dialog has sent nothing.
     */
    fun refusal(tx: Tx, from: String, to: String, rows: List<Pair<String, String>>, begun: String? = null): String? {
        val target = to.trim()
        if (target.isEmpty() || key(target) == key(from)) return null
        if (!named(target) || key(target) == key(TourDemo.CLIENT)) return tx.refuse_client_name_reserved
        if (begun != null && key(begun) == key(target)) return null
        val mine = rows.filter { key(it.first) == key(from) }.map { key(it.second) }.toSet()
        val twin = rows.firstOrNull { key(it.first) == key(target) && key(it.second) in mine } ?: return null
        return tx.refuse_client_has_project_x(client = twin.first.trim(), project = twin.second.trim())
    }

    // ---- whose the names are: staff's, and one login's ----------------------------------------------------------

    /**
     * THE LOGIN S5 IS CALLED WITH, AS A MARK kept beside the names (the review of slice 5, fault 3): the S5 session's
     * login while this phone is signed in to S5 ([s5Login], null or blank when it is not), else the legacy ERP key
     * S5 also takes ([erpKey], the key alone, never its secret); '' when the phone holds neither. A digest, so the
     * mark names nobody: it is only ever compared.
     */
    fun owner(s5Login: String?, erpKey: String?): String {
        val s = s5Login?.trim()?.lowercase().orEmpty()
        val k = erpKey?.trim().orEmpty()
        val who = if (s.isNotEmpty()) "s5:$s" else if (k.isNotEmpty()) "erp:$k" else return "" // text:value the two kinds of login, hashed
        val d = java.security.MessageDigest.getInstance("SHA-256").digest(who.toByteArray(Charsets.UTF_8)) // text:value the digest's name
        return d.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }.take(24)
    }

    /**
     * Whether names read by [heldBy] (the mark kept with them; null when none is) may be SHOWN TO, AND SENT FOR,
     * the login [now]: only to the login that read them. S5 gives a client's name to MCFT staff alone, so what one
     * login read is not the next person's to see -- nor a signed-out phone's ([now] '').
     */
    fun kept(heldBy: String?, now: String): Boolean = now.isNotEmpty() && heldBy == now

    /**
     * Whether everything held is FORGOTTEN before an answer of S5's is taken: the names are another login's
     * ([kept]), or this read of S5's list named [listed] projects and gave a client-name key on NONE of them
     * ([keyed] 0) -- the login is not staff, and S5 tells it no client's name. An empty list says nothing.
     */
    fun forgets(heldBy: String?, now: String, listed: Int, keyed: Int): Boolean =
        !kept(heldBy, now) || (listed > 0 && keyed == 0)

    /**
     * S5's name for one project as the login [now] is given it: [held] only where the names are that login's
     * ([kept]). So the tree shows none of them to anybody else, and the backfill ([seed], which sends only for a
     * project S5 gave a key for) sends nothing for a login S5 gave none.
     */
    fun shown(heldBy: String?, now: String, held: Renames.Held?): Renames.Held? = held.takeIf { kept(heldBy, now) }

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

    /**
     * [text] for a client that is ALSO a bench Customer (the review of slice 5, fault 1): [office] is the name the
     * bench took just before S5 was written, null when the bench was not told. S5 left a project behind, so the two
     * do not agree yet -- said first, in one line, then S5's own lines. Null when S5 took the name for every project.
     */
    fun text(tx: Tx, o: Outcome, titleOf: (String) -> String, office: String?): String? {
        val s5 = text(tx, o, titleOf) ?: return null
        return if (office == null) s5 else tx.refuse_client_office_only_x(office) + "\n" + s5
    }
}
