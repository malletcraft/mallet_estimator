package com.malletcrafts.sitephotos.pano

import com.malletcrafts.sitephotos.pano.GestureClassifier.Event
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The photo's gesture classifier (v52 slice 7), on the Mi A3 at density 2: touch slop 16 px (8 dp), the
 * double tap's reach 48 px (24 dp); long press 500 ms, double tap 450 ms, as sp.js.
 */
class GestureClassifierTest {

    private fun gc() = GestureClassifier(slop = 16.0, doubleDist = 48.0)

    private fun names(e: List<Event>) = e.joinToString(",") { it::class.simpleName ?: "?" }

    @Test fun `a quick press and lift is a tap at the down point`() {
        val g = gc()
        assertEquals(listOf<Event>(Event.Down(100.0, 200.0)), g.down(100.0, 200.0, 0))
        assertEquals(emptyList(), g.move(110.0, 205.0, 40))      // 11 px: under the slop
        assertEquals(listOf<Event>(Event.Tap(100.0, 200.0)), g.up(110.0, 205.0, 90))
    }

    @Test fun `the slop decides drag from tap, and a drag carries the offset from the down point`() {
        val g = gc()
        g.down(100.0, 100.0, 0)
        assertEquals(emptyList(), g.move(116.0, 100.0, 20))      // exactly the slop: still a press
        assertEquals(listOf<Event>(Event.Drag(117.0, 100.0, 17.0, 0.0, true)), g.move(117.0, 100.0, 30))
        assertEquals(listOf<Event>(Event.Drag(150.0, 80.0, 50.0, -20.0, false)), g.move(150.0, 80.0, 40))
        // back inside the slop it is still a drag
        assertEquals(listOf<Event>(Event.Drag(101.0, 100.0, 1.0, 0.0, false)), g.move(101.0, 100.0, 50))
        assertEquals(listOf<Event>(Event.DragEnd(101.0, 100.0)), g.up(101.0, 100.0, 60))
    }

    @Test fun `long press at 500 ms, then no tap`() {
        val g = gc()
        g.down(50.0, 50.0, 1000)
        assertEquals(1500L, g.deadline())
        assertEquals(emptyList(), g.timeout(1499))
        assertEquals(listOf<Event>(Event.LongPress(50.0, 50.0)), g.timeout(1500))
        assertNull(g.deadline())
        assertEquals(emptyList(), g.timeout(1700))               // once only
        assertEquals(emptyList(), g.up(52.0, 51.0, 1800))
    }

    @Test fun `a press lifted after 500 ms is a long press even when the clock was not fed`() {
        val g = gc()
        g.down(50.0, 50.0, 0)
        assertEquals(listOf<Event>(Event.LongPress(50.0, 50.0)), g.up(50.0, 50.0, 620))
    }

    @Test fun `moving off before 500 ms cancels the long press`() {
        val g = gc()
        g.down(50.0, 50.0, 0)
        assertEquals("Drag", names(g.move(90.0, 50.0, 200)))
        assertNull(g.deadline())
        assertEquals(emptyList(), g.timeout(600))
        assertEquals("DragEnd", names(g.up(90.0, 50.0, 700)))
    }

    @Test fun `a drag after a long press`() {
        val g = gc()
        g.down(50.0, 50.0, 0)
        assertEquals("LongPress", names(g.timeout(500)))
        assertEquals(listOf<Event>(Event.Drag(80.0, 50.0, 30.0, 0.0, true)), g.move(80.0, 50.0, 600))
        assertEquals("Drag", names(g.move(90.0, 50.0, 650)))
        assertEquals("DragEnd", names(g.up(90.0, 50.0, 700)))
    }

    @Test fun `double tap within 450 ms and 24 dp`() {
        val g = gc()
        g.down(100.0, 100.0, 0); assertEquals("Tap", names(g.up(100.0, 100.0, 80)))
        assertEquals("Down", names(g.down(130.0, 110.0, 400)))   // 320 ms after the lift, 32 px away
        assertEquals(listOf<Event>(Event.DoubleTap(130.0, 110.0)), g.up(130.0, 110.0, 460))
        // a third tap is a tap again
        g.down(130.0, 110.0, 600); assertEquals("Tap", names(g.up(130.0, 110.0, 650)))
    }

    @Test fun `too late or too far is two taps`() {
        val late = gc()
        late.down(100.0, 100.0, 0); late.up(100.0, 100.0, 80)
        late.down(100.0, 100.0, 531); assertEquals("Tap", names(late.up(100.0, 100.0, 560)))
        val far = gc()
        far.down(100.0, 100.0, 0); far.up(100.0, 100.0, 80)
        far.down(149.0, 100.0, 200); assertEquals("Tap", names(far.up(149.0, 100.0, 240)))
        // a second press that turns into a drag is no double tap, and leaves none pending
        val dr = gc()
        dr.down(100.0, 100.0, 0); dr.up(100.0, 100.0, 80)
        dr.down(100.0, 100.0, 200); dr.move(140.0, 100.0, 220); assertEquals("DragEnd", names(dr.up(140.0, 100.0, 240)))
        dr.down(140.0, 100.0, 300); assertEquals("Tap", names(dr.up(140.0, 100.0, 320)))
    }

    @Test fun `a second finger cancels, and nothing follows until every finger is up`() {
        val g = gc()
        g.down(100.0, 100.0, 0)
        assertEquals(listOf<Event>(Event.Cancel), g.down(300.0, 300.0, 50, pointers = 2))
        assertEquals(emptyList(), g.move(200.0, 100.0, 80, pointers = 2))
        assertEquals(emptyList(), g.move(200.0, 100.0, 90, pointers = 1))   // one finger left: still the pinch's
        assertEquals(emptyList(), g.timeout(700))
        assertEquals(emptyList(), g.up(200.0, 100.0, 800, pointers = 1))
        assertEquals(emptyList(), g.up(200.0, 100.0, 810))
        // the next gesture starts clean
        assertEquals("Down", names(g.down(10.0, 10.0, 900)))
        assertEquals("Tap", names(g.up(10.0, 10.0, 950)))
    }

    @Test fun `a tap after a cancelled drag never fires`() {
        val g = gc()
        g.down(100.0, 100.0, 0)
        assertEquals("Drag", names(g.move(140.0, 100.0, 30)))
        assertEquals(listOf<Event>(Event.Cancel), g.move(140.0, 100.0, 60, pointers = 2))
        // the fingers come off one by one, the last right where the first went down
        assertEquals(emptyList(), g.move(100.0, 100.0, 90, pointers = 1))
        assertEquals(emptyList(), g.up(100.0, 100.0, 120))
        // and a quick tap straight after is a tap, never a double tap with the one before the pinch
        g.down(100.0, 100.0, 200)
        assertEquals("Tap", names(g.up(100.0, 100.0, 230)))
    }

    @Test fun `a pinch between two taps breaks the double tap`() {
        val g = gc()
        g.down(100.0, 100.0, 0); g.up(100.0, 100.0, 40)
        g.down(100.0, 100.0, 100); g.down(200.0, 200.0, 120, pointers = 2); g.up(100.0, 100.0, 200)
        g.down(100.0, 100.0, 300)
        assertEquals("Tap", names(g.up(100.0, 100.0, 330)))
    }

    @Test fun `a system cancel and a reset`() {
        val g = gc()
        g.down(0.0, 0.0, 0)
        assertEquals(listOf<Event>(Event.Cancel), g.cancel())
        assertEquals(emptyList(), g.cancel())
        g.down(0.0, 0.0, 100); g.up(0.0, 0.0, 120)
        g.reset()
        g.down(0.0, 0.0, 200)
        assertTrue(g.up(0.0, 0.0, 220).single() is Event.Tap)
    }

    @Test fun `a system cancel after a still press is only a Cancel, and leaves no tap behind`() {
        val g = gc()
        g.down(100.0, 100.0, 0)
        assertEquals(listOf<Event>(Event.Cancel), g.cancel())
        // the phantom press recorded no tap: the next one, close and soon, is a Tap and not a DoubleTap
        g.down(102.0, 100.0, 100)
        assertEquals(listOf<Event>(Event.Tap(102.0, 100.0)), g.up(102.0, 100.0, 130))
    }

    @Test fun `a move inside the slop at 500 ms reports the long press without the clock being fed`() {
        val g = gc()
        g.down(50.0, 50.0, 0)
        assertEquals(emptyList(), g.move(52.0, 50.0, 300))
        assertEquals(listOf<Event>(Event.LongPress(50.0, 50.0)), g.move(53.0, 50.0, 520))
        assertNull(g.deadline())
        assertEquals(emptyList(), g.up(53.0, 50.0, 600))
    }

    @Test fun `a grabbed press drags from the first move, and is still a tap when it lifts inside the slop`() {
        val g = gc()
        g.down(100.0, 100.0, 0)
        g.grabbed()
        assertEquals(listOf<Event>(Event.Drag(105.0, 100.0, 5.0, 0.0, true)), g.move(105.0, 100.0, 20))
        assertEquals(listOf<Event>(Event.Drag(108.0, 101.0, 8.0, 1.0, false)), g.move(108.0, 101.0, 40))
        assertEquals(listOf<Event>(Event.Tap(100.0, 100.0)), g.up(108.0, 101.0, 80))
    }

    @Test fun `a grabbed press past the slop is one drag, its first reported once`() {
        val g = gc()
        g.down(100.0, 100.0, 0)
        g.grabbed()
        assertTrue((g.move(104.0, 100.0, 10).single() as Event.Drag).first)
        assertEquals(listOf<Event>(Event.Drag(130.0, 100.0, 30.0, 0.0, false)), g.move(130.0, 100.0, 20))
        assertEquals(listOf<Event>(Event.DragEnd(130.0, 100.0)), g.up(130.0, 100.0, 40))
        // the grab belongs to that press only: the next press waits for the slop again
        g.down(300.0, 300.0, 1000)
        assertEquals(emptyList(), g.move(305.0, 300.0, 1010))
    }

    @Test fun `a grabbed press still long-presses, and its moves inside the slop carry the clock`() {
        val g = gc()
        g.down(0.0, 0.0, 0)
        g.grabbed()
        assertEquals("Drag,LongPress", names(g.move(3.0, 0.0, 510)))
        assertEquals(emptyList(), g.up(3.0, 0.0, 600))
    }

    // ---- v52 slice 8 review: a grab that has moved is not a long press (sp.js gs.moved), drift excepted.
    // At density 2, GRAB_STILL_DP = 3 dp is 6 px.
    private fun gcStill() = GestureClassifier(slop = 16.0, doubleDist = 48.0, still = GestureClassifier.GRAB_STILL_DP * 2)

    @Test fun `a grab dragged 4 dp then held 600 ms opens no menu, and lifting it is no long press either`() {
        val g = gcStill()
        g.down(100.0, 100.0, 0); g.grabbed()
        assertEquals("Drag", names(g.move(108.0, 100.0, 100)))   // 8 px = 4 dp: past `still`, inside the slop
        assertNull(g.deadline())                                    // no clock to feed: the screen waits for events
        assertEquals(emptyList(), g.timeout(600))
        assertEquals("Drag", names(g.move(109.0, 100.0, 650)))   // still moving the mark, no menu
        // lifted inside the slop past 500 ms: a tap (Select acts on nothing for it), never a long press
        assertEquals(listOf<Event>(Event.Tap(100.0, 100.0)), g.up(109.0, 100.0, 700))
    }

    @Test fun `a grab that only drifted within 3 dp still long-presses (the screen puts the drift back)`() {
        val g = gcStill()
        g.down(100.0, 100.0, 0); g.grabbed()
        assertEquals("Drag", names(g.move(103.0, 101.0, 100)))   // ~3.2 px of drift
        assertEquals(500L, g.deadline())
        assertEquals("LongPress", names(g.timeout(500)))
    }

    @Test fun `a drift that comes back does not restore the long press, the furthest reach counts`() {
        val g = gcStill()
        g.down(100.0, 100.0, 0); g.grabbed()
        g.move(110.0, 100.0, 100); g.move(100.0, 100.0, 200)
        assertEquals(emptyList(), g.timeout(600))
    }

    @Test fun `a locked mark's refusal at 4 dp is past the still radius, so the menu never follows it`() {
        assertTrue(GestureClassifier.GRAB_STILL_DP < 4.0)
    }

    @Test fun `a press that grabbed nothing keeps the whole slop for its long press`() {
        val g = gcStill()
        g.down(100.0, 100.0, 0)
        assertEquals(emptyList(), g.move(112.0, 100.0, 100))       // 6 dp, no grab: still a press
        assertEquals("LongPress", names(g.timeout(500)))
    }

    @Test fun `the next press starts with no reach`() {
        val g = gcStill()
        g.down(100.0, 100.0, 0); g.grabbed(); g.move(130.0, 100.0, 50); g.up(130.0, 100.0, 80)
        g.down(300.0, 300.0, 1000); g.grabbed()
        assertEquals("LongPress", names(g.timeout(1500)))
    }
}
