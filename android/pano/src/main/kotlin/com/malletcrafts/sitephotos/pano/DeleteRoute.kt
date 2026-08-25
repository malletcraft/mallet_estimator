package com.malletcrafts.sitephotos.pano

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
 */
sealed class DeleteRoute {

    /** Call the bench. `docname` is what delete_node is given. */
    data class Server(val docname: String) : DeleteRoute()

    /** Drop it from this phone. Nothing on the bench corresponds to it. */
    object LocalOnly : DeleteRoute()

    /** Do nothing, and show `reason`. A refusal beats a false success. */
    data class Blocked(val reason: String) : DeleteRoute()
}

object Deletes {

    /**
     * @param kind        "client", "site" or "project"
     * @param serverId    the ERP docname, blank if the app does not hold one
     * @param serverKnown whether this row came from the bench's bootstrap
     */
    @JvmStatic
    fun route(kind: String, serverId: String, serverKnown: Boolean): DeleteRoute {
        // A client is an ERPNext Customer. It belongs to the office ledger,
        // it is referenced by quotations and invoices this app never sees,
        // and no phone removes one. The UI already offers no delete on a
        // client row; this is the same rule stated where it cannot be
        // forgotten by a future screen.
        if (kind == "client") return DeleteRoute.Blocked(
            "A client is the office's customer record. Delete the sites " +
            "under it here; the customer itself is a desk job.")

        val id = serverId.trim()
        if (id.isNotEmpty()) return DeleteRoute.Server(id)

        // The whole point of this function. Known to the office but no id in
        // hand means the app failed to match a row it was sent — never that
        // the row is local.
        // Amit, 2026-08-25, on the first draft: "too long". Read standing in
        // a dusty flat, three lines is three lines nobody finishes. Cut to
        // one — but the consequence clause STAYS, because six words is what
        // separates this from the sentence that cost three days, and a
        // refusal nobody understands gets tapped past.
        if (serverKnown) return DeleteRoute.Blocked(
            "Can't identify this $kind on the office system. Pull to " +
            "refresh — deleting now clears this phone only.")

        return DeleteRoute.LocalOnly
    }

    /**
     * What the phone says after a local-only delete. Worth one function
     * because the sentence is load-bearing: "Removed" alone was read as
     * "deleted everywhere" by the person who owns the data, and that reading
     * is what made a silent local delete cost three days.
     */
    @JvmStatic
    fun localOnlyMessage(what: String): String =
        "Removed from this phone — $what had never reached the office system."
}
