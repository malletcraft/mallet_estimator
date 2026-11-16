package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.LaserChip.Link
import com.malletcrafts.interiorestimate.pano.LaserChip.Look
import com.malletcrafts.interiorestimate.pano.LaserChip.Mem
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * THE LASER CHIP, AND WHEN THE LINK IS STARTED (port 63 slice I1a; DESIGN-imagemeter §4.2). With the on-screen D2 gone
 * a failed link looks exactly like a meter not yet pressed, so the four things the design names are each a test: OFF
 * and a figure turns gold -> start; a second turn inside 30 s -> no start; an attempt nobody asked for that fails ->
 * no words; the chip tapped -> words. English first: the two new lines are pinned in English.
 *
 * (Test names are plain ASCII: MeasureWalkTest's header says why.)
 */
class LaserChipTest {

    private val SURVEY = PhaseTools.SURVEY
    private val noDisto = LaserNote.noDistoFound

    @Test fun `the chip is grey off, pulsing while it connects, gold when ready - and there only where readings are keyed`() {
        assertEquals(listOf(Look.GREY, Look.PULSING, Look.PULSING, Look.GOLD), Link.values().map { LaserChip.look(it) })
        // the client's four states, by the names DistoClient gives them: the screen maps one to the other by name
        assertEquals(listOf("OFF", "SCANNING", "CONNECTING", "READY"), Link.values().map { it.name })
        assertEquals(listOf(true, false, false, false, false, false, true), listOf(0, 1, 2, 3, 4, 5, null).map { LaserChip.shown(it) })
        assertEquals(44, LaserChip.WIDTH_DP); assertEquals("D2", LaserChip.TEXT)
        // the gold figure's glyph is hollow until the link is ready
        assertEquals(listOf(false, false, false, true), Link.values().map { LaserChip.ready(it) })
        assertEquals("Laser not connected · tap to connect", LaserChip.label(Tx.EN, Link.OFF))
        assertEquals("The laser is still connecting…", LaserChip.label(Tx.EN, Link.SCANNING)); assertEquals("The laser is still connecting…", LaserChip.label(Tx.EN, Link.CONNECTING))
        assertEquals("D2 connected · press the meter's own button to read", LaserChip.label(Tx.EN, Link.READY))
    }

    @Test fun `off and a figure turns gold - the link is started, with the permission already held`() {
        val s = LaserChip.gold(Mem(), Link.OFF, permitted = true, stage = SURVEY, now = 5_000L)
        assertTrue(s.start); assertFalse(s.askPermission); assertNull(s.say)
        assertEquals(Mem(startedAt = 5_000L, asked = false, wasReady = false), s.mem)
        // survey prep opening at Survey is the same start
        assertTrue(LaserChip.opened(Mem(), Link.OFF, true, SURVEY, 0L).start); assertTrue(LaserChip.opened(Mem(), Link.OFF, true, null, 0L).start)
        // it NEVER asks for the permission: without it nothing happens, and the chip stays grey for him to tap
        LaserChip.gold(Mem(), Link.OFF, permitted = false, stage = SURVEY, now = 0L).let { assertFalse(it.start); assertFalse(it.askPermission); assertNull(it.say); assertEquals(Mem(), it.mem) }
        // already on its way, or ready: nothing is started
        for (l in listOf(Link.SCANNING, Link.CONNECTING, Link.READY)) assertFalse(LaserChip.gold(Mem(), l, true, SURVEY, 0L).start, "$l")
        // after Survey there is no chip and no link
        for (stage in listOf(1, 2, 3, 4, 5)) { assertFalse(LaserChip.gold(Mem(), Link.OFF, true, stage, 0L).start); assertFalse(LaserChip.opened(Mem(), Link.OFF, true, stage, 0L).start) }
    }

    @Test fun `a second turn inside 30 s starts nothing - never more often than once in 30 s`() {
        val m = LaserChip.gold(Mem(), Link.OFF, true, SURVEY, 10_000L).mem
        for (t in listOf(10_001L, 25_000L, 39_999L)) LaserChip.gold(m, Link.OFF, true, SURVEY, t).let { assertFalse(it.start, "$t"); assertEquals(m, it.mem) }
        assertTrue(LaserChip.gold(m, Link.OFF, true, SURVEY, 40_000L).start); assertEquals(30_000L, LaserChip.RETRY_MS)
        // each placement while he marks with the meter off may ask: one scan in 30 s at most
        var mem = Mem(); var starts = 0
        for (t in 0L..120_000L step 2_000L) { val s = LaserChip.gold(mem, Link.OFF, true, SURVEY, t); mem = s.mem; if (s.start) starts++ }
        assertEquals(5, starts)   // at 0, 30, 60, 90 and 120 s
        // a start made by a tap on the chip counts toward the 30 s too
        val tapped = LaserChip.tap(Mem(), Link.OFF, true, 50_000L).mem
        assertFalse(LaserChip.gold(tapped, Link.OFF, true, SURVEY, 60_000L).start)
    }

    @Test fun `an attempt nobody asked for that fails says nothing - no meter found, Bluetooth off, a scan refused`() {
        val m = LaserChip.gold(Mem(), Link.OFF, true, SURVEY, 0L).mem
        for (note in listOf(noDisto, LaserNote.turnBluetoothOn, LaserNote.scanRefused("x"), LaserNote.scanFailed(6), LaserNote.noBleScanner))
            LaserChip.state(m, Link.OFF, note).let { assertNull(it.say); assertFalse(it.start) }
        // nor on the way: scanning, the meter's name while connecting, a reconnect
        assertNull(LaserChip.state(m, Link.SCANNING, null).say); assertNull(LaserChip.state(m, Link.CONNECTING) { "DISTO 1234567" }.say)
        assertNull(LaserChip.state(m, Link.SCANNING, LaserNote.reconnecting(133)).say)
        // ready says nothing either: the chip turns gold
        assertNull(LaserChip.state(m, Link.READY, null).say)
    }

    @Test fun `the chip tapped speaks - grey connects and its failure is said, pulsing and gold say what they are`() {
        // grey, permission held: it connects, and THIS attempt speaks when it fails
        val t = LaserChip.tap(Mem(), Link.OFF, permitted = true, now = 7_000L)
        assertTrue(t.start); assertFalse(t.askPermission); assertNull(t.say); assertEquals(Mem(7_000L, asked = true), t.mem)
        assertEquals("DISTO 1234567", LaserChip.state(t.mem, Link.CONNECTING) { "DISTO 1234567" }.say!!(Tx.EN))
        LaserChip.state(t.mem, Link.OFF, noDisto).let { s ->
            assertEquals("No DISTO found — press its Bluetooth button and try again", s.say!!(Tx.EN))
            // said once: the next silent attempt's failure is silent again
            assertFalse(s.mem.asked); assertNull(LaserChip.state(s.mem, Link.OFF, noDisto).say)
        }
        // a deliberate tap is not held back by the 30 s
        assertTrue(LaserChip.tap(Mem(startedAt = 6_500L), Link.OFF, true, 7_000L).start)
        // pulsing: it says so, and the attempt in flight speaks from now on
        LaserChip.tap(Mem(1_000L), Link.SCANNING, true, 2_000L).let { s ->
            assertFalse(s.start); assertEquals("The laser is still connecting…", s.say!!(Tx.EN)); assertTrue(s.mem.asked)
            assertEquals("No DISTO found — press its Bluetooth button and try again", LaserChip.state(s.mem, Link.OFF, noDisto).say!!(Tx.EN)) }
        // gold: "press the meter's own button" -- it never fires the meter
        LaserChip.tap(Mem(wasReady = true), Link.READY, true, 0L).let { s ->
            assertFalse(s.start); assertFalse(s.askPermission); assertEquals("D2 connected · press the meter's own button to read", s.say!!(Tx.EN)) }
    }

    @Test fun `only a tap on the chip asks for the permission - granted it connects and speaks, refused it says so`() {
        val t = LaserChip.tap(Mem(), Link.OFF, permitted = false, now = 0L)
        assertTrue(t.askPermission); assertFalse(t.start); assertNull(t.say)
        LaserChip.permission(t.mem, granted = true, now = 900L).let { assertTrue(it.start); assertEquals(Mem(900L, asked = true), it.mem) }
        LaserChip.permission(t.mem, granted = false, now = 900L).let {
            assertFalse(it.start); assertEquals("Bluetooth permission refused — the laser can't connect", it.say!!(Tx.EN)); assertFalse(it.mem.asked) }
    }

    @Test fun `a link that was ready and drops says its line once, and the next figure that turns gold starts it again`() {
        // started silently when a figure turned gold, and it came up
        var m = LaserChip.gold(Mem(), Link.OFF, true, SURVEY, 0L).mem
        m = LaserChip.state(m, Link.READY, null).mem
        assertTrue(m.wasReady)
        // the D2 switches itself off: its own line, once
        val drop = LaserChip.state(m, Link.OFF, LaserNote.meterDisconnected)
        assertEquals("Meter disconnected", drop.say!!(Tx.EN)); assertFalse(drop.mem.wasReady)
        assertNull(LaserChip.state(drop.mem, Link.OFF, LaserNote.meterDisconnected).say)
        // a failed status the same; a reconnect on the way says nothing until it ends one way or the other
        assertEquals("Disconnected (8)", LaserChip.state(m, Link.OFF, LaserNote.disconnected(8)).say!!(Tx.EN))
        LaserChip.state(m, Link.SCANNING, LaserNote.reconnecting(133)).let { assertNull(it.say); assertTrue(it.mem.wasReady)
            assertEquals("No DISTO found — press its Bluetooth button and try again", LaserChip.state(it.mem, Link.OFF, noDisto).say!!(Tx.EN)) }
        // the next figure that turns gold starts it again -- once the 30 s have gone
        assertFalse(LaserChip.gold(drop.mem, Link.OFF, true, SURVEY, 20_000L).start); assertTrue(LaserChip.gold(drop.mem, Link.OFF, true, SURVEY, 30_000L).start)
    }
}
