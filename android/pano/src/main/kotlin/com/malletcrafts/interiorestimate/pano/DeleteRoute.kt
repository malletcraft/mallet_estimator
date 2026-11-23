package com.malletcrafts.interiorestimate.pano

/**
 * WHERE A DELETE GOES, decided before anything is removed anywhere.
 *
 * This exists because of one incident and one near-miss. On 2026-08-24 a site
 * was deleted from the phone, vanished from the tree, and was still on the
 * bench three days later with an untouched modified timestamp. The app said
 * "Removed from this phone" — true, and read as "deleted". The cause was a
 * blank server id: the tree inferred a site's docname from its projects, and
 * a site whose only project was filtered out came through with nothing to
 * call the server with.
 *
 * That specific cause is fixed (the id now comes from the bootstrap's own
 * sites array, keyed by client AND site so four "Main site" records stop
 * collapsing into one). But fixing a cause is not the same as removing a
 * FAILURE MODE. The shape that did the damage was still there: a blank id
 * routed silently to a local delete that reported success. Any future reason
 * for a blank id — a rename mid-session, a key that normalises differently, a
 * bootstrap that half-loaded — would lose data the same way and say the same
 * reassuring sentence.
 *
 * So the decision is made here, out of the UI, on two facts rather than one:
 * whether the office knows this row at all, and whether we hold its docname.
 * The pairing is what matters. Blank id AND unknown to the office is a row
 * this phone invented, which is genuinely a local delete. Blank id but KNOWN
 * to the office is not a local row — it is a bug in this app, and the only
 * honest response is to refuse and say so, because deleting locally would
 * once again destroy the phone's copy while leaving the real record standing.
 *
 * AND A THIRD FACT, SINCE S5 HOLDS PROJECTS THE BENCH HAS NEVER SEEN (port 63 slice F4b, 23 Nov 2026;
 * android/docs/port63/DESIGN-tree.md): whether S5 holds something of the row. A site made on a phone and uploaded
 * through S5 has no bench docname and was never in a bootstrap, so the pairing above read it as "a row this phone
 * invented": deleting it removed the phone's row, said "Removed from this phone — … had never reached the office
 * system", and the site stayed, listed by S5 -- the August sentence again, by a new road. And a project under a
 * phone-minted `S5-…` key carries an id, so it was sent to the bench, which has never held it. S5 has no delete for
 * a project, and nothing this phone can do removes one from it: a row S5 holds anything of is REFUSED, saying so and
 * that nothing was removed.
 *
 * AND WHAT S5 ONCE LISTED IS NOT LISTED FOR EVER (the review of F4, 23 Nov 2026; DESIGN-tree.md §10). The phone keeps
 * every project id S5's list has named and never drops one, so a project made on this phone that the office has since
 * taken off S5 read as "on S5" for good: its delete, and its site's, were refused for ever. The answer is asked of S5 at
 * the one moment it matters -- THIS delete -- and only where the phone holds nothing of the row that could still go up:
 *  - a capture of the row still on this phone keeps the refusal, and the sentence says to delete the photos first;
 *  - with none, S5's list is read now ([DeleteRoute.ReadFirst]) by a signed-in staff login, and the route asked again
 *    with what that read named ([Deletes.Tie.listRead], [Deletes.Tie.freshNames]);
 *  - S5 names none of the keys the refusal rested on -> [DeleteRoute.Gone]: the phone's row goes and exactly those keys
 *    are forgotten. S5 still names one, the read failed, nobody is signed in, or the login is not one S5 calls staff
 *    (its list would be short for another reason) -> the refusal stands.
 * Nothing is ever sent to S5 by a delete, and no other read of S5's list forgets anything.
 *
 * AND A BENCH DOCNAME WITH NO LIVE ERP PAIR IS THE OFFICE'S RECORD (the same review): it used to go to the bench with a
 * key that is dead or missing, and came back as "no server configured" beside a cascade button that could not work
 * either. It is refused at once, as the rename of such a row is, saying where it IS removed.
 */
import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Tx

sealed class DeleteRoute {

    /** Call the bench. `docname` is what delete_node is given. */
    data class Server(val docname: String) : DeleteRoute()

    /** Drop it from this phone. Nothing on the bench corresponds to it. */
    object LocalOnly : DeleteRoute()

    /** Do nothing, and show `reason`. A refusal beats a false success. */
    data class Blocked(val reason: String) : DeleteRoute()

    /**
     * S5 once listed something of the row and nothing of it is left on this phone: read S5's list NOW, and ask the
     * route again with what it named ([Deletes.Tie.listRead]). Nothing is removed on this route.
     */
    object ReadFirst : DeleteRoute()

    /**
     * S5's list, read for this delete by a staff login, names none of [keys]: the row is the phone's own again. Drop
     * it from this phone and forget exactly these keys -- S5 is sent nothing.
     */
    data class Gone(val keys: List<String>) : DeleteRoute()
}

object Deletes {

    /**
     * @param tx          the live language (port 61c slice M2): the refusals render WHOLE KEYS PER
     *                    KIND, as the card where-notes them — a gendered हा/ही cannot share one template
     * @param kind        "client", "site" or "project"
     * @param serverId    the ERP docname, blank if the app does not hold one
     * @param serverKnown whether this row came from the bench's bootstrap
     * @param onS5        whether S5 holds something of the row, or is being sent something of it (Renames.onS5,
     *                    Catalogue.onS5 in :app): asked for a site and for a project
     * @param erpLive     whether this phone holds a LIVE ERP pair ([TreeKeep.erpLive]): a bench docname is sent to the
     *                    bench only with one. The default is the route as it was before the pair was asked
     * @param tie         what the phone holds of the row's tie to S5, and of a read of S5's list made for this delete
     *                    ([Tie]). Null -- a caller that gathered none -- keeps the refusal exactly as it was
     */
    @JvmStatic
    fun route(
        tx: Tx, kind: String, serverId: String, serverKnown: Boolean, onS5: Boolean = false,
        erpLive: Boolean = true, tie: Tie? = null,
    ): DeleteRoute {
        // A client is an ERPNext Customer. It belongs to the office ledger,
        // it is referenced by quotations and invoices this app never sees,
        // and no phone removes one. The UI already offers no delete on a
        // client row; this is the same rule stated where it cannot be
        // forgotten by a future screen. The key is the rename dialog's note
        // (app.client_office_s_customer), so the two sentences cannot drift.
        if (kind == "client") return DeleteRoute.Blocked(tx.app_client_office_s_customer)

        val id = serverId.trim()
        val docname = id.isNotEmpty() && !Renames.minted(id)

        // THE OFFICE'S RECORD, AND NO LIVE PAIR TO REACH THE OFFICE WITH: refused at once, saying where it is removed --
        // before S5 is asked anything, as the rename of such a row is (the docname decides first: it is the reason
        // that names where the thing CAN be done). With a live pair the order is as it was: S5 first, then the bench.
        if (docname && !erpLive) return DeleteRoute.Blocked(officeRecord(tx, kind))

        // S5 HOLDS IT, AND THIS PHONE CANNOT DELETE IT THERE. Before the bench and before the local branch: a bench
        // delete would leave it listed by S5, and a local one would say it had never left the phone. A phone-minted
        // `S5-…` key is S5's by its own grammar and is never a docname to send the bench.
        if (onS5 || Renames.minted(id)) {
            val held = heldByS5(tx, kind)
            // a caller that gathered nothing about S5's side: the refusal as it always was
            if (tie == null) return DeleteRoute.Blocked(held)
            // SOMETHING OF THE ROW IS STILL ON THIS PHONE: it may be on its way to S5, so S5's list cannot say the row
            // is gone. The refusal stands, and says what he can do about it.
            if (tie.captures) return DeleteRoute.Blocked(photosFirst(tx, kind))
            // the keys the refusal rests on: what the phone gathered, and the row's own id where it is a minted key
            val keys = (tie.keys + (if (Renames.minted(id)) listOf(id) else emptyList()))
                .map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            if (keys.isEmpty()) return DeleteRoute.Blocked(held)
            // known on the phone, so said at once and nothing is read
            if (!tie.signedIn) return DeleteRoute.Blocked(tx.refuse_site_signed_out)
            return when (tie.listRead) {
                ListRead.NOT_READ -> DeleteRoute.ReadFirst
                ListRead.OFFLINE -> DeleteRoute.Blocked(tx.refuse_site_offline)
                // a list read by a login that is not staff proves nothing: it is short for another reason
                ListRead.UNPROVEN -> DeleteRoute.Blocked(held)
                ListRead.READ ->
                    // S5 names ONE of them: the row is S5's, as the refusal said
                    if (keys.any { it in tie.freshNames }) DeleteRoute.Blocked(held)
                    // S5 names none, and the row is also the bench's: the bench is asked, as for any docname
                    else if (docname) DeleteRoute.Server(id)
                    else DeleteRoute.Gone(keys)
            }
        }

        if (id.isNotEmpty()) return DeleteRoute.Server(id)

        // The whole point of this function. Known to the office but no id in
        // hand means the app failed to match a row it was sent — never that
        // the row is local.
        // Amit, 2026-08-25, on the first draft: "too long". Read standing in
        // a dusty flat, three lines is three lines nobody finishes. Cut to
        // one — but the consequence clause STAYS, because six words is what
        // separates this from the sentence that cost three days, and a
        // refusal nobody understands gets tapped past.
        // A client never reaches this branch: the first `if` returns.
        if (serverKnown) return DeleteRoute.Blocked(
            if (kind == "project") tx.app_cant_identify_project else tx.app_cant_identify_site)

        return DeleteRoute.LocalOnly
    }

    /** How a read of S5's list made for ONE delete came out ([Tie.listRead]). */
    enum class ListRead {
        /** No read has been made for this delete yet. */
        NOT_READ,
        /** S5 answered with its list, to a signed-in login S5 calls staff: [Tie.freshNames] is every project S5 holds. */
        READ,
        /** No answer of S5's: no signal, a session that could not be renewed, a page that is not S5's list. */
        OFFLINE,
        /** S5 answered, but the phone cannot tell that the login is staff: the list may be short, so it proves nothing. */
        UNPROVEN,
    }

    /**
     * What the phone holds of a row's tie to S5, gathered for a delete (Catalogue.s5Ties in :app), and the read of
     * S5's list made for it.
     *
     * @param keys       the S5 project ids the refusal rests on ([Renames.heldKeys]): what the row's projects and
     *                   captures carry and S5's list has named or the phone minted, and the keys of its phone-made rows
     *                   that S5's list has named
     * @param captures   a capture of the row is on this phone at all -- keyed or not, sent or not
     * @param signedIn   this phone holds an S5 sign-in (the legacy ERP key cannot show that its owner is staff)
     * @param listRead   how the read of S5's list made for this delete came out; [ListRead.NOT_READ] before it
     * @param freshNames every project id that read named; read only with [ListRead.READ]
     */
    data class Tie(
        val keys: List<String>, val captures: Boolean, val signedIn: Boolean,
        val listRead: ListRead = ListRead.NOT_READ, val freshNames: Set<String> = emptySet(),
    )

    /** Why a site or a project S5 holds something of is not removed: the phone cannot delete it there. A whole key per kind. */
    fun heldByS5(tx: Tx, kind: String): String =
        if (kind == "project") tx.app_project_s5_not_removed else tx.app_site_s5_not_removed

    /** The same refusal while photos of the row are still on this phone: what to do first, and that it may still be refused. */
    fun photosFirst(tx: Tx, kind: String): String =
        if (kind == "project") tx.app_project_s5_photos_first else tx.app_site_s5_photos_first

    /** Why a site or a project that carries a bench docname is not removed on a phone with no live ERP pair: it is the office's record. */
    fun officeRecord(tx: Tx, kind: String): String =
        if (kind == "project") tx.app_project_office_record_removed else tx.app_site_office_record_removed

    /** What the phone says after a row S5 no longer lists was dropped from it ([DeleteRoute.Gone]): removed here, and why that was allowed. */
    @JvmStatic
    fun goneMessage(tx: Tx, what: String): String =
        tx.app_msg_removed_phone_x_s5_gone(name = Masters.site_name(tx, what))

    /**
     * What the phone says after a local-only delete. Worth one function
     * because the sentence is load-bearing: "Removed" alone was read as
     * "deleted everywhere" by the person who owns the data, and that reading
     * is what made a silent local delete cost three days.
     */
    @JvmStatic
    fun localOnlyMessage(tx: Tx, what: String): String =
        // the fill through the site-name master: "Main site" reads मुख्य साइट, a typed name passes through
        tx.app_msg_removed_phone_x_never(name = Masters.site_name(tx, what))
}
