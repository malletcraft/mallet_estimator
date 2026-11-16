package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Msg
import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * THE LASER CHIP IN THE TOP BAR, AND WHEN THE LINK IS STARTED (port 63 slice I1a, 16 Nov 2026; DESIGN-imagemeter §4.2).
 * Amit, 16 Nov 2026, on build 0.3.337: "navigation button and D2 aer very close where d2 get accidentally fired." With
 * the measure bar gone no control in 2 · Mark fires the meter: its own button takes the reading. But until now the
 * Bluetooth link was started only by a press on that on-screen D2, so with the button gone something else has to
 * start it, and something has to SHOW it -- a failed link looks exactly like a meter not yet pressed.
 *
 *  - THE LINK IS STARTED BY ITSELF, with the Bluetooth permission already held, whenever a figure TURNS GOLD while
 *    the client is off, and when survey prep opens at Survey ([gold], [opened]) -- never more often than once in
 *    [RETRY_MS] (Android throttles scans, and each attempt scans for 20 s). A figure turning gold is the moment of
 *    measuring now; starting once at open would usually fail, the meter still being off while he marks.
 *  - AN ATTEMPT NOBODY ASKED FOR SAYS NOTHING WHEN IT FAILS -- no meter found, Bluetooth off, a scan refused
 *    ([state]). The chip alone shows it. ONLY A TAP ON THE CHIP SPEAKS, and only a tap on the chip asks for the
 *    permission ([tap], [permission]).
 *  - A link that WAS ready and drops says its line once, and the chip turns grey; the next figure that turns gold
 *    starts it again.
 *  - The chip never fires the meter. After Survey there is no chip and no link ([shown]).
 *
 * Pure: the screen (compiled only by CI) holds one [Mem], hands every event here with the client's state, and does
 * what the [Step] says -- start the link, ask for the permission, say a line.
 */
object LaserChip {

    /** The client's state, as DistoClient.State names it: the screen maps one to the other by name. */
    enum class Link { OFF, SCANNING, CONNECTING, READY }

    /** How the chip looks: grey = off, pulsing = connecting, gold = ready. */
    enum class Look { GREY, PULSING, GOLD }

    fun look(link: Link): Look = when (link) {
        Link.OFF -> Look.GREY
        Link.READY -> Look.GOLD
        else -> Look.PULSING
    }

    /** The chip is 44 dp wide, between the title and ↶ (DESIGN-imagemeter §2.1). */
    const val WIDTH_DP = 44

    /** The chip's two letters: the meter's name, the same in every language. */
    const val TEXT = "D2" // text:value the laser's name, the same in every language

    /** The chip is there only where readings are keyed: at Survey, and while the stage is unread ([MeasureFlow.canRead]). */
    fun shown(stage: Int?): Boolean = MeasureFlow.canRead(stage)

    /** The gold figure's glyph is drawn HOLLOW while the link is not ready ([MarkDims.WAIT_GLYPH]): the sign where his
     *  eyes are that a press of the meter will not arrive. The chip is the sign that stands. */
    fun ready(link: Link): Boolean = link == Link.READY

    /** What the chip is read out as: "Laser not connected · tap to connect", "The laser is still connecting…", or "D2
     *  connected · press the meter's own button to read". */
    fun label(tx: Tx, link: Link): String = when (look(link)) {
        Look.GREY -> tx.sp_laser_off
        Look.PULSING -> tx.sp_msg_laser_still_connecting
        Look.GOLD -> tx.sp_laser_ready
    }

    /** No start by itself within this long of the last start, ms. */
    const val RETRY_MS = 30_000L

    /**
     * What the chip remembers: when the link was last started ([startedAt], ms; null: never), whether the attempt in
     * flight was asked for BY A TAP ON THE CHIP ([asked]) and whether the link has been ready since it was last off
     * ([wasReady]).
     */
    data class Mem(val startedAt: Long? = null, val asked: Boolean = false, val wasReady: Boolean = false)

    /** What the screen does: keep [mem]; [start] the link (DistoClient.start); [askPermission] for Bluetooth; [say]
     *  a line (null: nothing is said). */
    class Step(val mem: Mem, val start: Boolean = false, val askPermission: Boolean = false, val say: Msg? = null)

    /** The link may be started by itself: the chip is there, the client is off, the permission is held, and the last
     *  start is at least [RETRY_MS] old. */
    private fun mayStart(mem: Mem, link: Link, permitted: Boolean, stage: Int?, now: Long): Boolean =
        shown(stage) && link == Link.OFF && permitted && (mem.startedAt == null || now - mem.startedAt >= RETRY_MS)

    private fun auto(mem: Mem, link: Link, permitted: Boolean, stage: Int?, now: Long): Step =
        if (mayStart(mem, link, permitted, stage, now)) Step(mem.copy(startedAt = now, asked = false), start = true) else Step(mem)

    /**
     * A FIGURE TURNED GOLD ([Pick.turnedGold]: a tap on a figure or a body, a placement, `▸`, the walk) at [now] ms:
     * the link is started if it may be ([mayStart]) -- nobody asked, so if it fails it says nothing. It NEVER asks for
     * the permission: [permitted] false, nothing happens, and the chip stays grey for him to tap.
     */
    fun gold(mem: Mem, link: Link, permitted: Boolean, stage: Int?, now: Long): Step = auto(mem, link, permitted, stage, now)

    /** SURVEY PREP OPENED: as [gold] -- a start at Survey with the permission held, silent if it fails. */
    fun opened(mem: Mem, link: Link, permitted: Boolean, stage: Int?, now: Long): Step = auto(mem, link, permitted, stage, now)

    /**
     * A TAP ON THE CHIP: grey -- it connects, asking for the permission first if it is not held, and from now on the
     * attempt SPEAKS; pulsing -- "The laser is still connecting…", and the attempt in flight speaks from now on too;
     * gold -- "D2 connected · press the meter's own button to read". It never fires the meter. A deliberate tap is
     * not held back by [RETRY_MS].
     */
    fun tap(mem: Mem, link: Link, permitted: Boolean, now: Long): Step = when (look(link)) {
        Look.GOLD -> Step(mem, say = { tx -> tx.sp_laser_ready })
        Look.PULSING -> Step(mem.copy(asked = true), say = { tx -> tx.sp_msg_laser_still_connecting })
        Look.GREY -> if (permitted) Step(mem.copy(startedAt = now, asked = true), start = true)
                     else Step(mem.copy(asked = true), askPermission = true)
    }

    /** THE PERMISSION WAS ANSWERED (it is asked only by a tap on the chip): granted, the link is started and speaks;
     *  refused, "Bluetooth permission refused — the laser can't connect". */
    fun permission(mem: Mem, granted: Boolean, now: Long): Step =
        if (granted) Step(mem.copy(startedAt = now, asked = true), start = true)
        else Step(mem.copy(asked = false), say = { tx -> tx.sp_msg_bluetooth_refused })

    /**
     * THE CLIENT MOVED TO [link], with its own [note] (DistoClient.onState; null: none) -- WHETHER THE NOTE IS SAID:
     *  - READY: nothing is said (the chip turns gold), and the link is remembered as having been ready;
     *  - OFF: its note -- "No DISTO found …", "Turn Bluetooth on", "Meter disconnected", "Disconnected (8)" -- is said
     *    ONLY if the attempt was asked for by a tap on the chip, or the link WAS READY and has dropped (said once: the
     *    memory is cleared, so the next figure that turns gold starts it again, silently);
     *  - on the way (scanning, connecting, "Reconnecting (133)…", the meter's name): said only for an attempt asked
     *    for by a tap. A link that was ready and is reconnecting says nothing until it ends one way or the other.
     */
    fun state(mem: Mem, link: Link, note: Msg?): Step = when (link) {
        Link.READY -> Step(mem.copy(asked = false, wasReady = true))
        Link.OFF -> Step(mem.copy(asked = false, wasReady = false), say = note?.takeIf { mem.asked || mem.wasReady })
        else -> Step(mem, say = note?.takeIf { mem.asked })
    }
}
