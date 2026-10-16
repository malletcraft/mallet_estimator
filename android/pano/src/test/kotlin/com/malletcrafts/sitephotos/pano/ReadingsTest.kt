package com.malletcrafts.sitephotos.pano

import com.malletcrafts.sitephotos.pano.Readings.Remote
import com.malletcrafts.sitephotos.pano.Readings.Seen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The phone's half of "Readings on the web": which keys go up, which server keys are taken. */
class ReadingsTest {

    private fun r(v: Double?, at: String, by: String = "web@x") = Remote(v, by, at)

    @Test fun `nothing changed sends nothing`() {
        val p = Readings.push(mapOf("FD1.w" to 900.0), mapOf("FD1.w" to Seen(900.0, "t1")))
        assertTrue(p.isEmpty())
    }

    @Test fun `a new, a changed and a deleted reading go up with their bases`() {
        val local = mapOf("FD1.w" to 905.0, "FW1.h" to 1200.0)
        val seen = mapOf("FD1.w" to Seen(900.0, "t1"), "FD1.h" to Seen(2100.0, "t2"))
        val p = Readings.push(local, seen)
        assertEquals(setOf("FD1.w", "FW1.h", "FD1.h"), p.set.keys)
        assertEquals(905.0, p.set["FD1.w"]); assertNull(p.set["FD1.h"]); assertEquals(1200.0, p.set["FW1.h"])
        assertEquals("t1", p.base["FD1.w"]); assertEquals("t2", p.base["FD1.h"])
        assertTrue(p.base.containsKey("FW1.h")); assertNull(p.base["FW1.h"])
    }

    @Test fun `pull takes a newer server value and a web delete`() {
        val local = mapOf("FD1.w" to 900.0, "FD1.h" to 2100.0)
        val seen = mapOf("FD1.w" to Seen(900.0, "t1"), "FD1.h" to Seen(2100.0, "t2"))
        val m = Readings.pull(local, seen, mapOf("FD1.w" to r(910.0, "t5"), "FW1.s" to r(850.0, "t6")))
        assertEquals(910.0, m.actual["FD1.w"]); assertEquals(850.0, m.actual["FW1.s"])
        assertNull(m.actual["FD1.h"], "deleted on the web")
        assertNull(m.seen["FD1.h"])
        assertEquals("t5", m.seen["FD1.w"]?.at)
        assertEquals(listOf("FD1.h", "FD1.w", "FW1.s"), m.taken)
        assertTrue(Readings.push(m.actual, m.seen).isEmpty(), "a pull leaves nothing to send")
    }

    @Test fun `pull never eats an unsent local edit`() {
        val local = mapOf("FD1.w" to 905.0)
        val seen = mapOf("FD1.w" to Seen(900.0, "t1"))
        val m = Readings.pull(local, seen, mapOf("FD1.w" to r(910.0, "t5")))
        assertEquals(905.0, m.actual["FD1.w"])
        assertEquals("t1", m.seen["FD1.w"]?.at, "still based on what it was edited from")
        assertTrue(m.taken.isEmpty())
        assertEquals("t1", Readings.push(m.actual, m.seen).base["FD1.w"])
    }

    @Test fun `first pull after the web seeded the same values just adopts them`() {
        val local = mapOf("FD1.w" to 900.0)
        val m = Readings.pull(local, emptyMap(), mapOf("FD1.w" to r(900.0, "t0")))
        assertEquals("t0", m.seen["FD1.w"]?.at)
        assertTrue(m.taken.isEmpty())
        assertTrue(Readings.push(m.actual, m.seen).isEmpty())
    }

    @Test fun `an unchanged server key is not taken`() {
        val m = Readings.pull(mapOf("FD1.w" to 900.0), mapOf("FD1.w" to Seen(900.0, "t1")), mapOf("FD1.w" to r(900.0, "t1")))
        assertTrue(m.taken.isEmpty())
    }

    @Test fun `a local edit to a key deleted on the web stays, as a new key`() {
        val m = Readings.pull(mapOf("FD1.w" to 905.0), mapOf("FD1.w" to Seen(900.0, "t1")), emptyMap())
        assertEquals(905.0, m.actual["FD1.w"])
        assertNull(Readings.push(m.actual, m.seen).base["FD1.w"])
    }

    @Test fun `after push written keys are seen and a conflict takes the server value`() {
        val local = mapOf("FD1.w" to 905.0, "FD1.h" to 2110.0)
        val seen = mapOf("FD1.w" to Seen(900.0, "t1"), "FD1.h" to Seen(2100.0, "t2"))
        val sent = Readings.push(local, seen)
        val conflict = mapOf("FD1.h" to r(2120.0, "t9", "medha@x"))
        val readings = mapOf("FD1.w" to r(905.0, "t10", "amit@x"), "FD1.h" to r(2120.0, "t9", "medha@x"))
        val m = Readings.afterPush(local, seen, sent, listOf("FD1.w"), conflict, readings)
        assertEquals(905.0, m.actual["FD1.w"]); assertEquals("t10", m.seen["FD1.w"]?.at)
        assertEquals(2120.0, m.actual["FD1.h"]); assertEquals("t9", m.seen["FD1.h"]?.at)
        assertEquals(setOf("FD1.h"), m.conflicts.keys)
        assertTrue(Readings.push(m.actual, m.seen).isEmpty())
    }

    @Test fun `a written delete leaves no baseline`() {
        val local = emptyMap<String, Double>()
        val seen = mapOf("FD1.w" to Seen(900.0, "t1"))
        val sent = Readings.push(local, seen)
        val m = Readings.afterPush(local, seen, sent, listOf("FD1.w"), emptyMap(), emptyMap())
        assertNull(m.actual["FD1.w"]); assertNull(m.seen["FD1.w"])
        assertTrue(m.taken.isEmpty())
    }

    // v52 slice 8 review: a conflict on a key whose mark is gone -- the phone's delete stands, sent again once
    @Test fun `a conflict on a key whose mark is gone is kept out and pushed again as a delete on the new at`() {
        val actual = mutableMapOf("FW2.w" to 900.0)
        val seen = mutableMapOf("FW2.w" to Seen(900.0, "t1"))
        val applied = mapOf("FW1.w" to r(612.0, "t9", "staff@x"), "FW2.w" to r(905.0, "t8"))
        // FW1 was cleared here; FW2 still has its mark: its conflict is applied as before, by merge
        val live = { k: String -> k.startsWith("FW2.") }
        assertEquals(listOf("FW1.w"), Readings.orphans(actual, seen, applied, live))
        assertNull(actual["FW1.w"])
        assertEquals(900.0, actual["FW2.w"])                      // a live key is not this function's
        val p = Readings.push(actual, seen)
        assertEquals(mapOf<String, Double?>("FW1.w" to null), p.set.filterKeys { it == "FW1.w" })
        assertEquals("t9", p.base["FW1.w"])                       // based on the server's newest at: it is written
        // the next pull of the same server state does not bring it back
        val m = Readings.pull(actual, seen, mapOf("FW1.w" to r(612.0, "t9", "staff@x")))
        assertNull(m.actual["FW1.w"])
        assertTrue(Readings.orphanText(listOf("FW1.w"))!!.startsWith("FW1.w changed on the web after its mark was deleted here"))
        assertNull(Readings.orphanText(emptyList()))
    }

    @Test fun `a conflict the server answered with a delete on a gone mark needs nothing`() {
        val actual = mutableMapOf<String, Double>()
        val seen = mutableMapOf("FW1.w" to Seen(600.0, "t1"))
        assertEquals(emptyList(), Readings.orphans(actual, seen, mapOf("FW1.w" to r(null, "t9")), { false }))
        assertTrue(Readings.push(actual, seen).isEmpty())
    }
}
