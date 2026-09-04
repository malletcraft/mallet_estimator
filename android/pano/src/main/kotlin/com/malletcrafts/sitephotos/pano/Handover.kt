package com.malletcrafts.sitephotos.pano

/**
 * Names and captions for faces handed to ImageMeter — the device side of the
 * convention in mallet_estimator/handover.py.
 *
 * The filename is the identity: it is the ONLY thing that survives the trip
 * through Google Photos → ImageMeter → Drive and comes home machine-readable
 * (proven on real data, 2026-08-17). The caption is for the human holding the
 * phone. Both must match the server's expectations exactly, so both live in
 * this pure module where a JVM test can hold them to it.
 */
object Handover {

    const val SEP = " · "

    /** Same vocabulary as the server and as the 42 hand-made files already on
     *  Amit's Drive: the vertical faces are called top/bottom, not up/down. */
    val FACE_LABELS = mapOf(
        "front" to "Front", "right" to "Right", "back" to "Back",
        "left" to "Left", "up" to "Top", "down" to "Bottom",
    )

    /**
     * The filename token back to the face name — the inverse of FACE_LABELS,
     * and the thing whose ABSENCE hid two faces from the app for weeks.
     *
     * filename() writes the LABEL: MCAP-…_top.jpg, MCAP-…_bottom.jpg. The
     * reader compared that token against Panorama.FACES, whose names are "up"
     * and "down". They never matched, so ceiling and floor were parsed as
     * "not a face" and silently dropped, and a capture that had written all
     * six showed exactly four. Nothing errored: the files were on the phone
     * the whole time, correct and captioned, simply invisible.
     *
     * That is why the map lives HERE, beside the function that writes the
     * name, rather than in the reader — one file owns both directions of the
     * convention, and a test can hold them to each other.
     *
     * Spoken synonyms are accepted too, mirroring LABEL_TO_FACE in
     * mallet_estimator/handover.py, so a file somebody named "ceiling" by
     * hand still reads back.
     */
    val LABEL_TO_FACE: Map<String, String> =
        FACE_LABELS.entries.associate { (face, label) -> label.lowercase() to face } +
        mapOf("ceiling" to "up", "floor" to "down")

    /** A filename token ("top") or a face name ("up") back to the face name.
     *  Both are accepted because both exist in the wild: the app writes
     *  labels, while older code and the odd export wrote face names. */
    fun faceOfToken(token: String?): String? {
        val t = token?.lowercase()?.trim().orEmpty()
        if (t.isEmpty()) return null
        return LABEL_TO_FACE[t] ?: if (FACE_LABELS.containsKey(t)) t else null
    }

    private val DEVICE_ID_RE = Regex("^MCAP-[0-9a-f]{12}$")

    /** MCAP-<12 hex>: the id a capture is born with, minted at the shutter,
     *  adopted by the server on sync. Lowercase hex on purpose — the server's
     *  pattern is case-sensitive and a well-meaning uppercase would orphan
     *  every annotation. */
    fun mintDeviceId(randomBytes: ByteArray): String {
        require(randomBytes.size >= 6) { "need at least 6 random bytes" }
        return "MCAP-" + randomBytes.take(6).joinToString("") { "%02x".format(it) }
    }

    fun isDeviceId(token: String?): Boolean =
        token != null && DEVICE_ID_RE.matches(token)

    /** MCAP-…_top.jpg — what the return matcher parses. */
    fun filename(captureId: String, face: String): String {
        val label = FACE_LABELS[face] ?: error("unknown face: $face")
        return "${captureId}_${label.lowercase()}.jpg"
    }

    /** The strip burned under the face: id first because the id is what a
     *  person reads out when a photo needs identifying by hand. */
    fun captionText(
        captureId: String,
        room: String,
        face: String,
        captureDate: String? = null,
        stage: String? = null,
    ): String {
        // PASSED THROUGH, not refused. filename() is strict because a bogus
        // token there would write a file nothing can ever match again; a
        // CAPTION is just words burned under a picture, and being strict here
        // bought nothing and cost everything — a flat photograph carries the
        // token "photo", which is deliberately not one of the six, and this
        // line threw on it. Both the Camera and Gallery paths died in the
        // same place, which is what a shared helper crashing looks like.
        // Passed through RAW, not prettified: the Python side has always
        // used FACE_LABELS.get(face, face), and this class exists to hold the
        // device's naming to the server's. A nicer-looking caption that
        // disagreed with the bench would defeat the whole point of the file.
        val label = FACE_LABELS[face] ?: face
        val parts = mutableListOf(captureId, room, label)
        val tail = listOfNotNull(
            captureDate?.takeIf { it.isNotBlank() },
            stage?.takeIf { it.isNotBlank() },
        ).joinToString(" ")
        if (tail.isNotEmpty()) parts.add(tail)
        return parts.joinToString(SEP)
    }

    /** Pictures/<app>/<client>/<project>/<initials>_<room> — <project>.
     *
     * The leaf carries the project because Android names device albums by
     * LEAF FOLDER ONLY, and one client can have two projects with the same
     * room in them. Until 2026-08-22 this KDoc claimed the leaf carried the
     * project while the code made it "YS_MB" — so two projects showed as two
     * identically named albums in the picker ImageMeter imports from, and
     * choosing the wrong one gave no sign. */
    fun relativePath(client: String, projectTitle: String, room: String): String {
        val c = safe(client.ifBlank { "Unfiled" })
        val p = safe(projectTitle)
        // YS_MB — client initials + room token. Exactly the folders Amit
        // already keeps in ImageMeter, and exactly the prefix the SKU codes
        // use (YS_MB_WAR), so the album, the Drive folder and the estimate
        // all say the same word about the same room.
        //
        // The client half is not decoration: Android names a gallery album
        // after the LEAF folder, so a bare "MB" would collide with every
        // other project's master bedroom in the picker ImageMeter imports
        // from. That is what the old "<project> — <room>" leaf was for.
        val r = safe(RoomToken.folder(client, room, projectTitle))
        return "Pictures/MCFT Site Photos/$c/$p/$r/"
    }

    private fun safe(part: String): String =
        part.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifEmpty { "_" }
}
