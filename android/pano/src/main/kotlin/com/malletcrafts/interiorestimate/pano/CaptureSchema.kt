package com.malletcrafts.interiorestimate.pano

/**
 * THE CAPTURE STORE'S UPGRADES, held where a test can pin them (port 61c slice D1, 28 Oct 2026).
 *
 * `captures.db` (:app CaptureStore) IS the offline queue, and the phones that most need an update
 * are the ones carrying unsent captures, so its table only ever grows: every change is one
 * `ALTER TABLE captures ADD COLUMN`, nullable or with a constant DEFAULT, never a rebuild, a rename
 * or a drop. CaptureStore's onUpgrade runs [upgrade] and nothing else, each statement in its own
 * runCatching, and its onCreate gives every column added here the same shape its ADD COLUMN does.
 * :app has no unit tests and cannot compile in the dev container, so the statements live in :pano:
 * CaptureStoreRevertTest pins them and reads CaptureStore.kt for the rest of the rule.
 *
 * Why every statement must survive running twice: versionCode is 100 + the CI run number, so a
 * revert of a schema change ships as a NEWER build with OLDER schema code. CaptureStore's no-op
 * onDowngrade opens the newer database and the helper writes the older version back; the next
 * newer build then runs its upgrade again over columns that already exist. The duplicate column is
 * the expected failure there, and runCatching swallows it.
 *
 * Version 6 adds `work`, the work a capture is filed under, with no DEFAULT. NULL means "filed by
 * its stage": every row already on a phone when this version arrives, and every capture taken
 * before the phone files by work (nothing writes the column in the build that adds it). What the
 * phone writes into it is 0 (the quote journey, [Album.QUOTE_WORK]) or one of the sixteen ids of
 * [Album.WORKS], never NULL, so NULL keeps meaning "from before".
 */
object CaptureSchema {

    /** One upgrade statement: it runs on a database whose version is below [since]. */
    data class Step(val since: Int, val sql: String)

    /** The helper's schema version: the newest step's. */
    const val VERSION = 6

    /**
     * Every upgrade statement since version 1, oldest first. A new version appends its steps and
     * raises [VERSION]; nothing here is ever edited or removed, because a phone that skipped a build
     * runs every step it has not had, in this order.
     */
    val STEPS: List<Step> = listOf(
        Step(2, "ALTER TABLE captures ADD COLUMN sku TEXT NOT NULL DEFAULT ''"), // text:value SQL
        Step(3, "ALTER TABLE captures ADD COLUMN kind TEXT NOT NULL DEFAULT '360'"), // text:value SQL
        Step(4, "ALTER TABLE captures ADD COLUMN fov REAL NOT NULL DEFAULT 0"), // text:value SQL
        // The measured room, in inches. Stored rather than the six face angles it implies, so there
        // is one source of truth and a capture cannot mean one thing on the phone and another on the
        // bench. 0 means unmeasured: every capture queued before version 5 keeps splitting at `fov`.
        Step(5, "ALTER TABLE captures ADD COLUMN room_l_in REAL NOT NULL DEFAULT 0"), // text:value SQL
        Step(5, "ALTER TABLE captures ADD COLUMN room_w_in REAL NOT NULL DEFAULT 0"), // text:value SQL
        Step(5, "ALTER TABLE captures ADD COLUMN room_h_in REAL NOT NULL DEFAULT 0"), // text:value SQL
        // The work a capture is filed under. No DEFAULT: NULL is "filed by its stage" (see above).
        Step(6, "ALTER TABLE captures ADD COLUMN work INTEGER"), // text:value SQL
    )

    /** The statements a database at version [old] runs to reach [VERSION], in order. */
    fun upgrade(old: Int): List<String> = STEPS.filter { old < it.since }.map { it.sql }
}
