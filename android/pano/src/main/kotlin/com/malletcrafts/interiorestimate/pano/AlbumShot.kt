package com.malletcrafts.interiorestimate.pano

/**
 * THE PROGRESS SHOT'S FILE, the pure rules (port 61c slice K5, 9 Nov 2026; DESIGN-port61c.md §20, "THE SHOT'S FILE").
 * :app's AlbumShots writes, keeps and trims the files; everything it decides is here, so AlbumShotTest pins it.
 *
 * S5 takes a photo's JPEG once, `FF D8 FF` (else 415) and at most 8 MB (else 413) (S5 QUOTE 9a51733, asis.js: the album
 * photo's `…/img` PUT, MAX_IMG). The phone's camera app writes what it writes -- EXIF orientation rather than turned
 * pixels on most phones, 12 MP or more on the Mi A3 -- so the queued file is never the camera's: it is decoded at a
 * power-of-two sample that keeps the long side at least [LONG_SIDE], scaled to [LONG_SIDE] (never up), turned and
 * mirrored by its EXIF orientation ([turnOf], baked into the pixels: every reader draws it the right way up, and no
 * location or camera serial in the EXIF reaches S5 or the client), and encoded at the first of [QUALITIES] that fits
 * [MAX_BYTES]. At 3072 px the first always fits; the ladder is the guarantee.
 *
 * A shot is KEPT until it lands on S5 and goes with its op ([orphans]); the camera app's own file is a day's grace at
 * most ([rawStale]: the K5 critic, finding 12).
 */
object AlbumShot {

    /** The long side of a queued shot: VIEWER_TARGET, the largest the phone ever draws a photo (ZoomableImage.kt). */
    const val LONG_SIDE = 3072

    /** S5's MAX_IMG (asis.js): a larger JPEG is its 413, refused for good. */
    const val MAX_BYTES = 8 * 1024 * 1024

    /** The JPEG qualities tried in turn until the file is at most [MAX_BYTES]. */
    val QUALITIES: List<Int> = listOf(90, 75, 60)

    /** A shot no outbox holds is deleted once it is this old: a shot is written BEFORE its op is queued (ten minutes). */
    const val SHOT_GRACE_MS = 600_000L

    /**
     * The camera app's own file (`files/shots/album-<id>.jpg`) is deleted once it is a day old (the K5 critic, finding
     * 12): nothing pending can be that old -- a pending shot is filed when the camera app returns -- and one a failed
     * decode, a killed process or an app closed with the camera open left behind is megabytes.
     */
    const val RAW_GRACE_MS = 24L * 60 * 60 * 1000

    /** The camera app's file's name before the id (its folder is the FileProvider's `shots/`). */
    const val RAW_PREFIX = "album-" // text:value a file name's prefix, never shown

    /**
     * The `inSampleSize` a [w] x [h] picture is decoded at: the largest power of two that keeps its long side at least
     * [LONG_SIDE] (1 for a picture no larger), so the scale to [LONG_SIDE] after it never enlarges.
     */
    fun sampleFor(w: Int, h: Int): Int {
        val long = maxOf(w, h)
        var s = 1
        while (long / (s * 2) >= LONG_SIDE) s *= 2
        return s
    }

    /** The size a decoded [w] x [h] is drawn at: its long side [LONG_SIDE], the aspect kept, never larger than it came. */
    fun sizeFor(w: Int, h: Int): Pair<Int, Int> {
        val long = maxOf(w, h)
        if (w <= 0 || h <= 0 || long <= LONG_SIDE) return w to h
        val k = LONG_SIDE.toDouble() / long
        return if (w >= h) LONG_SIDE to maxOf(1, Math.round(h * k).toInt()) else maxOf(1, Math.round(w * k).toInt()) to LONG_SIDE
    }

    /** A turn of the pixels: rotate clockwise by [degrees] (0, 90, 180, 270), then [mirror] left to right. */
    data class Turn(val degrees: Int, val mirror: Boolean)

    /**
     * The EXIF orientation table (TIFF 6.0 / EXIF 2.3, tag 0x0112), as a rotation then a left-right mirror -- the order
     * Android's own readers apply it (a Matrix setRotate, then postScale(-1, 1)): 1 none; 2 mirrored; 3 180; 4 180 then
     * mirrored (a vertical flip); 5 90 then mirrored (transpose); 6 90; 7 270 then mirrored (transverse); 8 270. Anything
     * else (0, unknown, a value no camera writes) is no turn.
     */
    fun turnOf(orientation: Int): Turn = when (orientation) {
        2 -> Turn(0, true)
        3 -> Turn(180, false)
        4 -> Turn(180, true)
        5 -> Turn(90, true)
        6 -> Turn(90, false)
        7 -> Turn(270, true)
        8 -> Turn(270, false)
        else -> Turn(0, false)
    }

    /** Whether [head] starts as a JPEG does, `FF D8 FF` -- S5's own test (asis.js: `That is not a JPEG`). */
    fun isJpeg(head: ByteArray): Boolean =
        head.size >= 3 && head[0] == 0xFF.toByte() && head[1] == 0xD8.toByte() && head[2] == 0xFF.toByte()

    /** One file of a shots folder as the trims read it: its name less `.jpg` (a queued shot's: the photo's id) and its mtime. */
    data class ShotFile(val id: String, val mtime: Long)

    /**
     * The queued shots [files] that go at [now]: every one whose id is [held] by no login's queue or `dropped`
     * (AlbumQueue.heldPhotos) and that is older than [SHOT_GRACE_MS] -- a younger one may be a shot whose op is about to
     * be queued. NOTHING when [held] is null: the outbox could not be read, and an unreadable outbox is not an empty one
     * (the K5 critic, finding 5: read as empty it deleted every queued JPEG, and each op then met NO_FILE for good).
     */
    fun orphans(files: List<ShotFile>, held: Set<String>?, now: Long): List<String> {
        if (held == null) return emptyList()
        return files.filter { it.id !in held && now - it.mtime > SHOT_GRACE_MS }.map { it.id }
    }

    /** The camera app's files [files] (named whole) that go at [now]: an [RAW_PREFIX] file older than [RAW_GRACE_MS]. */
    fun rawStale(files: List<ShotFile>, now: Long): List<String> =
        files.filter { it.id.startsWith(RAW_PREFIX) && now - it.mtime > RAW_GRACE_MS }.map { it.id }
}
