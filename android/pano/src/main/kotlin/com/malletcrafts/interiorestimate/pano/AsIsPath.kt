package com.malletcrafts.interiorestimate.pano

import java.time.Instant
import java.time.ZoneId

/**
 * THE AS-IS UPLOAD'S ADDRESSES AND RECORD (port 61c slice AS1, 3 Nov 2026): every 360 is its own S5
 * capture, named by the phone's device id, on S5 QUOTE's explicit capture routes (mcft-interior-estimate
 * 9a51733, functions/_lib/asis.js, "ONE SERVER ROOM PER CAPTURE", migration 0006):
 *
 *  - PUT   /api/asis/<project>/<room>/c/<capture>                 the record ([record], [body]); answers `created`
 *  - PUT   /api/asis/<project>/<room>/c/<capture>/img/<surface>   a squared surface ([image]); 404 until the record
 *  - GET | PATCH /api/asis/<project>/<room>/c/<capture>/readings  its readings ([readings])
 *
 * in that order: the record first, then the photos, then the readings -- S5 stores no photo that no record
 * owns, so a photo's 404 now means its record is not on the web. The old room address (no capture: the room's
 * CURRENT capture, the `_pending` photo slot, the readings of the room's only capture) is 0.3.212's and goes
 * with AS2; nothing here builds it.
 *
 * NAMING. The capture is `deviceId`, stable from the shutter and needing no ERP: the record's `capture` (S5
 * requires it to equal the address), and `erp_capture` is the name ERP filed it under (MEST-PH-…), information
 * only, sent when there is one. `captured_at` is BYTE-IDENTICAL to what every build since 0.3.212 sent
 * ([capturedAt]: the same expression, on the phone's own zone), because S5's rule C3 renames a capture it holds
 * under an older name (MEST-PH-…, or MCAP-… from a phone that uploaded before ERP filed it) onto the id of a
 * record whose `captured_at` equals it: the row, its readings, its photos and the room's current pointer follow,
 * and nothing is lost on the first upload after this build. A changed byte would make S5 see a NEW capture and
 * leave the old one beside it.
 *
 * FILING. The record carries `work`: [Album.workOfCapture] of the row's `work` (0 the quote journey, else one
 * of the sixteen; null for a row from before schema 6) and its legacy `stage` -- the phone's own filing, through
 * the same 39-stage table S5 holds (STAGE_WORK, asis.js workOfRecord), so the two cannot disagree and a capture
 * re-filed on the phone (FW2) is never filed back by its stage. It is sent as JSON null when the table says
 * null ("not filed to a work"), never left out: S5 reads an absent `work` as "file it from `stage`", and the
 * record carries no `stage`.
 *
 * Pure JVM, so [AsIsPathTest] pins the three addresses, the single encoding of each segment, the body's keys
 * and S5 types, and the exact `captured_at` text. The percent-encoding is the caller's ([path]'s `enc`: :app's
 * `AsIsUpload.seg`, android.net.Uri.encode, which S5's `seg` decodes with decodeURIComponent), applied to each
 * segment exactly once and never to the fixed parts. No Tx anywhere: what is sent stays as S5 reads it
 * (MastersTest, the wire check).
 */
object AsIsPath {

    /** Where every as-is address starts, before the project. */
    const val PREFIX = "/api/asis/"

    /** The record's address: the segments after [PREFIX], each still to be encoded by [path]. */
    fun record(project: String, room: String, capture: String): List<String> = listOf(project, room, "c", capture)

    /** One squared surface's address (S5's SURFACES: ceiling, front, left, floor, right, back). */
    fun image(project: String, room: String, capture: String, surface: String): List<String> =
        listOf(project, room, "c", capture, "img", surface)

    /** The capture's readings: GET to pull, PATCH to push. */
    fun readings(project: String, room: String, capture: String): List<String> = listOf(project, room, "c", capture, "readings")

    /**
     * The URL path for [segments]: [PREFIX] and each segment through [enc] ONCE, joined by `/`. A project, a room
     * or a capture is one segment whatever it holds (a space, a slash, a `%`): the encoding is what keeps it one,
     * and encoding a segment twice would send `%2520` for a space, which S5 decodes to `%20` and never matches.
     */
    fun path(segments: List<String>, enc: (String) -> String): String = PREFIX + segments.joinToString("/") { enc(it) }

    /**
     * `captured_at`, byte for byte as every build since 0.3.212 wrote it: the shutter instant at the phone's own
     * [zone], as `OffsetDateTime.toString()` prints it -- `2023-11-15T03:43:20+05:30`, with the milliseconds when
     * there are any and WITHOUT the seconds when they are zero (java.time's own rule). S5's C3 matches it as a
     * string, so the expression is pinned by a test and must never be "tidied".
     */
    fun capturedAt(createdAt: Long, zone: ZoneId): String =
        Instant.ofEpochMilli(createdAt).atZone(zone).toOffsetDateTime().toString()

    /**
     * The record's body, in S5's order and S5's JSON types, as a map :app serialises whole (`JSONObject(map)`,
     * which writes a null as JSON null; `put(k, null)` would DROP the key). [prep] and [surfaces] are passed
     * through as the caller built them (org.json objects in :app). [work] and [stage] are the capture row's;
     * the body carries the ONE filing, [Album.workOfCapture] of the two. [erpCapture] is sent only when ERP has
     * named the capture. [capturedAt] is [AsIsPath.capturedAt]'s text, built by the caller on the phone's zone.
     */
    fun body(
        project: String, projectTitle: String, customer: String, room: String, capture: String, erpCapture: String?,
        capturedAt: String, appVersion: String, l: Double, w: Double, h: Double, prep: Any?, surfaces: Any?,
        work: Int?, stage: String?,
    ): LinkedHashMap<String, Any?> {
        val b = LinkedHashMap<String, Any?>()
        b["v"] = 1
        b["project"] = project
        b["project_title"] = projectTitle
        b["customer"] = customer
        b["room"] = room
        b["capture"] = capture
        if (erpCapture != null) b["erp_capture"] = erpCapture
        b["captured_at"] = capturedAt
        b["app_version"] = appVersion
        b["L"] = l
        b["W"] = w
        b["H"] = h
        b["prep"] = prep
        b["surfaces"] = surfaces
        b["work"] = Album.workOfCapture(work, stage)
        return b
    }
}
