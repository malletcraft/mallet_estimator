package com.malletcrafts.interiorestimate.pano

/**
 * AN S5 PROJECT KEY IS REMEMBERED, NEVER RE-DERIVED (port 63, the review of F4; android/docs/port63/DESIGN-tree.md §10).
 *
 * A project made on a phone is known to S5 by a key the phone mints: "S5-" and a hash of client|site|title. Until this
 * the key was kept nowhere. It was worked out again from the names each time it was wanted -- and the names change:
 * a project is moved to another site, a site is renamed. While a capture of the project stayed on the phone the
 * masters still tied the project to its client, so the old key was found. Once its captures had left the phone S5's
 * list named the project with NO client (S5 keeps no client name for a project made on a phone), the phone filed it
 * under "(no client)", found no project of that client and title, and minted a NEW key from the names as they stood
 * now: a second S5 project for the same job, and the first one listed under no client at all.
 *
 * So the key is kept the moment it exists, with the names it is for, and read from there:
 *  - BY NAME: the folded client and the folded title -> the S5 project id. One key for a client's title, at whatever
 *    site: the masters are matched the same way, and the site is the very part a move or a site rename changes.
 *  - BY ID: the id -> the client and the title it is for, as typed. This is the only copy of a client's name for a
 *    project whose captures have left the phone, so S5's list can file the project under its client.
 *
 * Everything here is the rule; the phone's store (S5Keys in :app) only reads and writes what these answer.
 */
object KeyBook {

    /** The client and the project title an S5 project id is for, as they were typed. */
    data class Names(val client: String, val title: String)

    /** What the phone remembers: [byName] the folded client and title -> the id; [byId] the id -> its names. */
    data class Book(val byName: Map<String, String> = emptyMap(), val byId: Map<String, Names> = emptyMap())

    /** A client and a title as ONE key, each as the catalogue compares a name (trimmed, lower case, runs of space and underscore as one). */
    fun fold(client: String, title: String): String = Renames.key(client) + "|" + Renames.key(title)

    /** The key remembered for a client's project, or null when none is: only then is a key minted. */
    fun keyFor(book: Book, client: String, title: String): String? =
        book.byName[fold(client, title)]?.takeIf { it.isNotBlank() }

    /** The client and title [project] is for, or null when the phone has never held a capture of it. */
    fun namesOf(book: Book, project: String): Names? = if (project.isBlank()) null else book.byId[project]

    /**
     * WHICH OF TWO KEYS IS A CLIENT'S PROJECT, when two are claimed for one client and title (two captures of it carry
     * two ids: a key minted before this book existed and one minted after a move, or a bench docname and a minted
     * key). The one S5's list has named is S5's project; where both are named, or neither, the one remembered first
     * stays -- a remembered key is not replaced by a later claim.
     */
    fun wins(stored: String, comer: String, listed: (String) -> Boolean): String =
        if (stored.isBlank() || (!listed(stored) && listed(comer))) comer else stored

    /**
     * REMEMBER ONE KEY: [project] is the S5 project of [client]'s [title]. An id has ONE client and title at a time,
     * so a name the id was remembered under before is let go (the row was renamed and its capture now says the new
     * name); and where another id is already remembered for these names, [wins] says which stays. Blank parts are
     * no key: nothing is remembered.
     */
    fun take(book: Book, project: String, client: String, title: String, listed: (String) -> Boolean = { false }): Book {
        val id = project.trim()
        if (id.isEmpty() || client.isBlank() || title.isBlank()) return book
        val at = fold(client, title)
        val byName = LinkedHashMap(book.byName)
        val byId = LinkedHashMap(book.byId)
        val was = byId[id]
        if (was != null && fold(was.client, was.title) != at && byName[fold(was.client, was.title)] == id)
            byName.remove(fold(was.client, was.title))
        byId[id] = Names(client.trim(), title.trim())
        byName[at] = wins(byName[at].orEmpty(), id, listed)
        return if (byName == book.byName && byId == book.byId) book else Book(byName, byId)
    }

    /**
     * EVERY CAPTURE THAT CARRIES A PROJECT ID SAYS WHICH PROJECT ITS CLIENT AND TITLE ARE: the book as it stands with
     * each of [captures] taken ([take]), in the order given -- the caller gives them oldest first, so the key a
     * project was first shot under is the one remembered first. A capture with no id yet says nothing. This is how
     * the keys minted before the book existed are in it from the first read.
     */
    fun learn(book: Book, captures: List<TreeKeep.Cap>, listed: (String) -> Boolean): Book =
        captures.fold(book) { b, c -> take(b, c.project, c.client, c.title, listed) }

    /**
     * A ROW MADE ON THIS PHONE WAS RENAMED, and the keys remembered for it follow: a "client" rename gives [to] to
     * every entry of that client, a "project" rename to the entry of that client and title (names as the catalogue
     * compares them). The key itself does not change -- that is the point: the row's next capture is the SAME S5
     * project under its new name. An entry already standing under the new names stays ([wins]'s rule: remembered
     * first). A "site" rename changes nothing here: no key is kept by site.
     */
    fun renamed(book: Book, kind: String, client: String, title: String, to: String): Book {
        val n = to.trim()
        if (n.isEmpty() || (kind != "client" && kind != "project")) return book
        var out = book
        for ((id, names) in book.byId) {
            val hit = Renames.key(names.client) == Renames.key(client) &&
                (kind == "client" || Renames.key(names.title) == Renames.key(title))
            if (!hit) continue
            val now = if (kind == "client") Names(n, names.title) else Names(names.client, n)
            val byName = LinkedHashMap(out.byName)
            val byId = LinkedHashMap(out.byId)
            if (byName[fold(names.client, names.title)] == id) byName.remove(fold(names.client, names.title))
            byId[id] = now
            if (byName[fold(now.client, now.title)].isNullOrBlank()) byName[fold(now.client, now.title)] = id
            out = Book(byName, byId)
        }
        return out
    }

    /** FORGET ONE KEY, and only it: [project] and the name that points at it. For the one delete that proved S5 no longer lists it (Deletes.route). */
    fun forget(book: Book, project: String): Book {
        if (project.isBlank() || (project !in book.byId && project !in book.byName.values)) return book
        return Book(book.byName.filterValues { it != project }, book.byId.filterKeys { it != project })
    }
}
