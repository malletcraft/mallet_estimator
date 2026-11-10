package com.malletcrafts.interiorestimate.pano

/**
 * THE DEMO FLAT (port 61c §31) — the one decision the prototype itself names for the port
 * (ch55, "Kotlin needs", approved with v55): "a built-in demo project, kept on the phone only
 * and never synced — the APK carries only real projects, and a tour's phase move there would
 * be a real stage move on S5." This object is the SINGLE is-this-the-demo rule: the fixed
 * client / site / project / room names and the fixed capture id, in its own `DEMO-` grammar,
 * outside `MCAP-`'s, so AS1's device-id rule can never mint a colliding real one.
 *
 * Every sync entry point refuses demo rows through it and nothing else: `AsIsUpload.enqueue`,
 * `SyncWorker` (whose prep sweep goes through that enqueue), `QuoteStageSync.move`,
 * `WishSync.push`, `AlbumOutbox.add` / [AlbumQueue.requestOf], `ReadingsSync`. A demo row
 * never reaches a queue, a PUT or the bench. The :app half (`TourSeed` in TourOverlay.kt)
 * does the disk: the capture row, the bundled pano asset, [prepJson] written as the demo's
 * survey prep. [TourDemoTest] pins the seed and the rule.
 */
object TourDemo {

    const val CLIENT = "Demo Client" // text:value the demo client's stored name (the card's demo.* rows are skipped and generate no accessor)
    const val SITE = "Demo Site, Pune" // text:value the demo site's stored name
    const val PROJECT = "Demo flat interiors" // text:value the demo project's stored name
    const val ROOM = "Guest Bedroom" // text:value the demo room's stored name (shown through Masters.room, as every stored room name is)

    /** The one demo capture: its own grammar, never `MCAP-`'s, so nothing real collides. */
    const val CAPTURE = "DEMO-guest-bedroom-360"
    private const val PREFIX = "DEMO-"

    /** The demo room's laser size, mm — the prototype's Guest Bedroom (L 3319 · W 2678 · H 2677). */
    val SIZE_MM = doubleArrayOf(3319.0, 2678.0, 2677.0)

    /** The prep's own savedAt (2026-10-11T09:51:05.389Z, prototype v35), as the row's created_at. */
    const val CAPTURED_AT_MS = 1791712265389L

    /** The demo's capture id grammar — every id the demo will ever mint, and nothing else. */
    fun isDemoCapture(id: String?): Boolean = id != null && id.startsWith(PREFIX)

    /** The demo project, by its fixed names (trimmed, ignoring case, as the catalogue compares). */
    fun isDemoProject(client: String?, title: String?): Boolean =
        client != null && title != null &&
            client.trim().equals(CLIENT, ignoreCase = true) &&
            title.trim().equals(PROJECT, ignoreCase = true)

    /** The demo's wish-list key (it has no ERP id, so WishList.keyOf gives its local key). */
    fun wishKey(): String = WishList.keyOf("", CLIENT, PROJECT)

    /**
     * A stored key that belongs to the demo: its wish-list key, a `DEMO-` capture id, or —
     * belt and braces for a caller handing a project string — the demo project's title.
     * The one rule every queue and sync guard calls.
     */
    fun isDemoKey(key: String?): Boolean =
        key != null && (key == wishKey() || isDemoCapture(key) || key.trim().equals(PROJECT, ignoreCase = true))

    /**
     * The demo room's survey prep, byte-identical on every phone: the prototype's
     * `prep/guest-bedroom` state (all six surfaces bounded; marks FW1 FN1 FN2 FE1 · RE1 ·
     * BD1 BE1 BL1 · LN1 · GS1 · CE1 and their readings) wrapped in the prep file's shape
     * (`v`, `L`, `W`, `H` from [SIZE_MM]). Written once by the seed, restored by every
     * tour's end if the practice changed it.
     */
    fun prepJson(): String =
        requireNotNull(TourDemo::class.java.getResourceAsStream("tour-demo-prep.json")) {
            "tour-demo-prep.json is not on the classpath" // text:value an impossible-state message for developers (the resource ships in the jar)
        }.readBytes().decodeToString()
}
