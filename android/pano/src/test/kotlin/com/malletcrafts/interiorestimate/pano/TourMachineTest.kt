package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The step machine (port 61c §31): Next always advances; Back lands on a position (and is
 * disabled on step 1 and while a move runs); Skip and Done both end; a Next-only run records
 * wrote 0; and the tap rule — a tap inside the spotlight after the step showed, advancing
 * 320 ms AFTER THE TAP (the engine's `tapAt>shownAt && now-tapAt>=320`; §31 critic finding 4:
 * the delay runs from the tap, a settle so the app's response is seen, never a demand that
 * the tap come 320 ms after show).
 */
class TourMachineTest {

    private fun step(
        id: String, done: TourDone? = null, anchor: String? = "save",
        setup: List<TourMove> = emptyList(),
    ) = TourStep(id, "survey", anchor, if (done == null) "look" else "do", setup, done, "t $id", "b $id")

    private fun flow(vararg steps: TourStep) =
        TourFlow("t", "Test flow", "A test flow.", steps.toList(), ships = true)

    private fun shownMachine(f: TourFlow, at: Long = 1000L, marks: Int = 0): TourMachine {
        val m = TourMachine(f, at)
        m.begin(0)
        m.shown(marks, at)
        return m
    }

    @Test fun `next always advances, and Done on the last step ends the tour`() {
        val m = shownMachine(flow(step("t.1"), step("t.2"), step("t.3")))
        assertEquals("1/3", m.counter)
        assertTrue(m.next() is TourMachine.Go.Show)
        assertEquals(1, m.i)
        assertTrue(m.busy, "a move runs until the host calls shown")
        m.shown(0, 2000)
        m.next(); m.shown(0, 3000)
        assertTrue(m.last)
        assertEquals(TourMachine.Go.End, m.next())
        assertEquals("done", m.ended)
    }

    @Test fun `busy blocks next, back and doneNow - the buttons are disabled while a move runs`() {
        val m = TourMachine(flow(step("t.1"), step("t.2", TourDone.Screen("site"))), 1000)
        m.begin(0)
        assertTrue(m.busy)
        assertEquals(TourMachine.Go.None, m.next())
        assertEquals(TourMachine.Go.None, m.back())
        assertFalse(m.doneNow(TourWorld(screen = "site"), 5000))
    }

    @Test fun `back is disabled on step 1 and lands on the previous position otherwise`() {
        val m = shownMachine(flow(step("t.1"), step("t.2")))
        assertEquals(TourMachine.Go.None, m.back())
        m.next(); m.shown(0, 2000)
        assertEquals(TourMachine.Go.Show(0), m.back())
        assertEquals(0, m.i)
    }

    @Test fun `skip ends the tour, and nothing moves after an end`() {
        val m = shownMachine(flow(step("t.1"), step("t.2")))
        m.skip()
        assertEquals("skip", m.ended)
        assertEquals(TourMachine.Go.None, m.next())
        assertFalse(m.doneNow(TourWorld(screen = "survey"), 9000))
        m.skip()
        assertEquals("skip", m.ended, "a second end never rewrites how it ended")
    }

    @Test fun `a Next-only run sees every step and records wrote 0`() {
        val f = flow(step("t.1"), step("t.2"), step("t.3"), step("t.4"))
        val m = shownMachine(f)
        while (m.ended == null) {
            when (m.next()) {
                is TourMachine.Go.Show -> m.shown(0, 1000L + m.i)
                else -> {}
            }
        }
        assertEquals("done", m.ended)
        assertEquals(listOf(0, 1, 2, 3), m.seen)
        assertEquals(0, m.wrote)
    }

    // ---- the tap rule (§31 critic finding 4: the 320 ms runs FROM THE TAP) ----

    @Test fun `a tap after the step showed advances 320 ms after the tap`() {
        val m = shownMachine(flow(step("t.1", TourDone.Tap), step("t.2")))
        val w = TourWorld()
        assertFalse(m.doneNow(w, 99_000), "no tap yet, however long the step has shown")
        m.tap(1500)
        assertFalse(m.doneNow(w, 1819), "319 ms since the tap: not yet")
        assertTrue(m.doneNow(w, 1820), "320 ms since the tap: advance")
    }

    @Test fun `a prompt tap - right as the step shows - still advances, 320 ms later`() {
        val m = shownMachine(flow(step("t.1", TourDone.Tap), step("t.2")), at = 1000)
        m.tap(1001)
        assertTrue(m.doneNow(TourWorld(), 1321), "the tap need NOT come 320 ms after show")
    }

    @Test fun `shown resets the tap, so Back never inherits one`() {
        val m = shownMachine(flow(step("t.1", TourDone.Tap), step("t.2")))
        m.tap(1500)
        m.next(); m.shown(0, 2000)
        m.back(); m.shown(0, 3000)
        assertFalse(m.doneNow(TourWorld(), 9000), "the old tap died with the re-show")
    }

    // ---- the other predicates, over the host-fed world ----

    @Test fun `screen and phase advance without survey prep, the rest only with it open`() {
        val f = flow(
            step("t.1", TourDone.Screen("client")), step("t.2", TourDone.Phase(1)),
            step("t.3", TourDone.Mode("mark")), step("t.4", TourDone.Tool("E")),
            step("t.5", TourDone.Tab("front")), step("t.6"))
        val m = shownMachine(f)
        assertTrue(m.doneNow(TourWorld(screen = "client"), 2000))
        assertFalse(m.doneNow(TourWorld(screen = "site"), 2000))
        m.next(); m.shown(0, 2000)
        assertTrue(m.doneNow(TourWorld(phase = 1), 3000))
        m.next(); m.shown(0, 3000)
        assertFalse(m.doneNow(TourWorld(mode = "mark", spOpen = false), 4000), "mode needs survey prep open")
        assertTrue(m.doneNow(TourWorld(mode = "mark", spOpen = true), 4000))
        m.next(); m.shown(0, 4000)
        assertTrue(m.doneNow(TourWorld(tool = "E", spOpen = true), 5000))
        m.next(); m.shown(0, 5000)
        assertTrue(m.doneNow(TourWorld(tab = "front", spOpen = true), 6000))
    }

    @Test fun `tab 360 is the 360 mode, as the engine reads it`() {
        val m = shownMachine(flow(step("t.1", TourDone.Tab("360")), step("t.2")))
        assertTrue(m.doneNow(TourWorld(spOpen = true, tab = "front", mode = "360"), 2000))
        assertFalse(m.doneNow(TourWorld(spOpen = true, tab = "front", mode = "mark"), 2000))
    }

    @Test fun `marks advance from the count when the step showed, exists and gone from the anchors`() {
        val f = flow(
            step("t.1", TourDone.Marks(1)), step("t.2", TourDone.Exists("more")),
            step("t.3", TourDone.Gone("more")), step("t.4"))
        val m = shownMachine(f, marks = 5)
        assertFalse(m.doneNow(TourWorld(spOpen = true, markCount = 5), 2000))
        assertTrue(m.doneNow(TourWorld(spOpen = true, markCount = 6), 2000))
        m.next(); m.shown(5, 2000)
        assertTrue(m.doneNow(TourWorld(anchors = setOf("more")), 3000))
        assertFalse(m.doneNow(TourWorld(), 3000))
        m.next(); m.shown(5, 3000)
        assertTrue(m.doneNow(TourWorld(), 4000))
        assertFalse(m.doneNow(TourWorld(anchors = setOf("more")), 4000))
    }
}
