package com.malletcrafts.sitephotos.pano

/**
 * The short room token — MB, KB, KIT — used in folder names and in the tree.
 *
 * This is NOT a new naming scheme. It is the same `room_abbr` grammar the
 * SKU code generator uses server-side (mallet_estimator/estimator.py), so a
 * photo folder and the estimate that prices what is in it carry the same
 * token: YS_MB the folder, YS_MB_WAR the wardrobe. Amit's ImageMeter folders
 * are already named this way.
 *
 * It lives here, in the pure module, for two reasons: a phone with no signal
 * still has to name the folder it is writing into, and a JVM test can hold
 * this implementation to the server's exact behaviour. If the two ever
 * disagree, folders and codes drift apart silently — which is the one
 * failure this file exists to prevent.
 */
object RoomToken {

    private val WORD_SPLIT = Regex("[\\s_\\-]+")
    private const val LETTERS_PER_WORD = 3

    /** "Master Bedroom" -> MB, "Kitchen" -> KIT, "Living Room" -> LR. */
    fun of(room: String?): String {
        val words = WORD_SPLIT.split((room ?: "").trim()).filter { it.isNotEmpty() }
        if (words.isEmpty()) return ""
        if (words.size == 1) {
            return words[0].take(LETTERS_PER_WORD).uppercase()
        }
        return words.joinToString("") { it.first().toString() }.uppercase()
    }

    /** "Yogesh Sahasrabudhe" -> YS. Same grammar as the server's
     *  customer_initials, which is what makes the folder name match the SKU
     *  code prefix. */
    fun initials(customerName: String?): String {
        val parts = (customerName ?: "").trim().split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
        if (parts.isEmpty()) return ""
        val first = parts.first().first()
        val last = if (parts.size > 1) parts.last().first().toString() else ""
        return "$first$last".uppercase()
    }

    /**
     * The album a phone gallery shows for one room: YS_MB.
     *
     * Customer initials + room token, exactly the folders Amit already keeps
     * in ImageMeter and exactly the prefix the SKU codes use (YS_MB_WAR). It
     * has to carry the client, not just the room: Android names the album
     * after the LEAF folder, so a bare "MB" would collide with every other
     * project's master bedroom in the picker ImageMeter imports from.
     */
    fun folder(customerName: String?, room: String): String {
        val i = initials(customerName)
        val t = of(room).ifBlank { room }
        return if (i.isBlank()) t else "${i}_$t"
    }

    /** What the tree shows: the token a person says, with the full name
     *  behind it so nobody has to memorise the mapping. */
    fun label(room: String): String {
        val t = of(room)
        return if (t.isBlank() || t.equals(room, true)) room else "$t · $room"
    }
}
