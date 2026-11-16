package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * THE MEASURE PAD'S TEXT (port 63 slice M1; DESIGN-port63 §1.4, the plan critic's finding 2): what it starts with,
 * what each key does, and that every write is MmBox.commit's -- so walking past a reading never changes or removes it.
 */
class MeasurePadTest {

    private fun d(n: Int) = MeasurePad.Key.Digit(n)
    private val back = MeasurePad.Key.Back
    private val clear = MeasurePad.Key.Clear

    private fun type(s: MeasurePad.State, vararg keys: MeasurePad.Key) = keys.fold(s) { st, k -> MeasurePad.press(st, k) }
    private fun digits(s: MeasurePad.State, text: String) = type(s, *text.map { d(it - '0') }.toTypedArray())

    @Test fun `it starts with the stored reading, or empty -- and fresh`() {
        assertEquals(MeasurePad.State("905", true), MeasurePad.start(905.0))
        assertEquals(MeasurePad.State("", true), MeasurePad.start(null))
        // a fractional reading as a reading box shows it, whole mm
        assertEquals(MeasurePad.State("3315", true), MeasurePad.start(3314.7))
        // while fresh the bar's box and the gold plate draw the dimension's own one figure, not the pad's text
        assertNull(MeasurePad.live(MeasurePad.start(905.0))); assertNull(MeasurePad.live(MeasurePad.start(null)))
    }

    @Test fun `walking across measured dimensions with no key pressed leaves every reading as it was`() {
        // ‹, › and Next ▸ each commit the pad before they move: an untouched pad must write nothing, whatever it shows
        val actual = linkedMapOf("FD1.w" to 905.0, "FD1.h" to 2104.6, "FD1.l" to 300.25, "FK1.1" to 0.4)
        val before = LinkedHashMap(actual)
        repeat(3) {
            for (code in listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p", "FK1.1", "FK1.2")) {
                val pad = MeasurePad.start(actual[code])
                val c = MeasurePad.commit(actual[code], pad)
                assertNull(c, code)
                if (c != null) { if (c.value != null) actual[code] = c.value!! else actual.remove(code) }
            }
        }
        assertEquals(before, actual)
    }

    @Test fun `the first digit replaces what was there, then digits append -- six at most`() {
        val pad = MeasurePad.start(905.0)
        val one = MeasurePad.press(pad, d(1))
        assertEquals(MeasurePad.State("1", false), one)             // not "9051"
        assertEquals("1", MeasurePad.live(one))
        assertEquals(MeasurePad.State("1198", false), digits(pad, "1198"))
        assertEquals(1198.0, assertNotNull(MeasurePad.commit(905.0, digits(pad, "1198"))).value)
        // six digits at most: the seventh is ignored
        assertEquals(6, MeasurePad.MAX_DIGITS)
        assertEquals("123456", digits(MeasurePad.start(null), "1234567").text)
        assertEquals("123456", digits(MeasurePad.start(null), "12345678").text)
        // into an empty pad over no reading: the reading typed
        assertEquals(640.0, assertNotNull(MeasurePad.commit(null, digits(MeasurePad.start(null), "640"))).value)
        // only 0 … 9 are digits
        assertEquals(MeasurePad.State("12", false), type(digits(MeasurePad.start(null), "12"), d(10), d(-1)))
    }

    @Test fun `backspace on a fresh pad empties it, then removes one digit at a time`() {
        val pad = MeasurePad.start(905.0)
        assertEquals(MeasurePad.State("", false), MeasurePad.press(pad, back))
        assertEquals("", MeasurePad.live(MeasurePad.press(pad, back)))         // emptied: the box shows its mm hint
        assertEquals(MeasurePad.State("11", false), type(pad, d(1), d(1), d(9), back))
        assertEquals(MeasurePad.State("", false), type(pad, d(1), back))
        assertEquals(MeasurePad.State("", false), type(pad, d(1), back, back, back))   // nothing left to remove
        assertEquals(MeasurePad.State("7", false), type(pad, d(1), back, d(7)))
    }

    @Test fun `a leading zero is ignored and leaves the pad as it was`() {
        val pad = MeasurePad.start(905.0)
        // on a fresh pad it stays fresh: 0 then Next ▸ cannot remove or change a reading
        assertEquals(pad, MeasurePad.press(pad, d(0)))
        assertNull(MeasurePad.commit(905.0, type(pad, d(0), d(0))))
        assertEquals(MeasurePad.State("", true), MeasurePad.press(MeasurePad.start(null), d(0)))
        // on an emptied pad too; after a first digit a zero is a digit like any other
        assertEquals(MeasurePad.State("", false), type(pad, clear, d(0), d(0)))
        assertEquals(MeasurePad.State("1005", false), type(pad, d(1), d(0), d(0), d(5)))
        assertEquals(MeasurePad.State("50", false), type(pad, d(0), d(5), d(0)))
    }

    @Test fun `Clear then Next removes the reading, and an empty pad over no reading writes nothing`() {
        val cleared = MeasurePad.press(MeasurePad.start(905.0), clear)
        assertEquals(MeasurePad.State("", false), cleared)
        val c = assertNotNull(MeasurePad.commit(905.0, cleared))
        assertNull(c.value)                                                     // remove it: the estimate returns
        assertEquals(MeasurePad.State("", false), type(MeasurePad.start(905.0), d(4), d(2), clear))
        assertNull(MeasurePad.commit(null, MeasurePad.press(MeasurePad.start(null), clear)))
        assertNull(MeasurePad.commit(null, MeasurePad.press(MeasurePad.start(null), back)))
    }

    @Test fun `typing the figure it already holds writes nothing`() {
        assertNull(MeasurePad.commit(905.0, digits(MeasurePad.start(905.0), "905")))
        // a fractional reading retyped as it is shown is not rewritten as a whole number
        assertNull(MeasurePad.commit(3314.7, digits(MeasurePad.start(3314.7), "3315")))
        // ... and one digit different is a new reading
        assertEquals(906.0, assertNotNull(MeasurePad.commit(905.0, digits(MeasurePad.start(905.0), "906"))).value)
    }

    @Test fun `a derived chain segment starts empty, and leaving it writes nothing`() {
        // FK1.2 is derived 2404.5 from the total: it has no reading, so the pad is given none -- never the figure shown
        val pad = MeasurePad.start(null)
        assertEquals("", pad.text)
        assertNull(MeasurePad.commit(null, pad))
        // typing over it makes it a reading
        assertEquals(2410.0, assertNotNull(MeasurePad.commit(null, digits(pad, "2410"))).value)
    }

    @Test fun `the pad is four rows of four`() {
        assertEquals(4, MeasurePad.ROWS); assertEquals(4, MeasurePad.COLUMNS)
        assertTrue(MeasurePad.ROWS == StepRoute.PAD_ROWS)
    }
}
