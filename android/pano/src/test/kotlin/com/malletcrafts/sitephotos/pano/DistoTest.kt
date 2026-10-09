package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DistoTest {

    @Test
    fun `metres on the wire become whole millimetres`() {
        assertEquals(2500, Disto.toMm(2.5f))
        assertEquals(3450, Disto.toMm(3.45f))
        assertEquals(1219, Disto.toMm(1.2192f))
    }

    @Test
    fun `little-endian decoding matches the characteristic layout`() {
        // 1.0f == 0x3F800000; 3.45f == 0x405CCCCD — both little-endian on the wire.
        assertEquals(1.0f, Disto.readFloat32Le(byteArrayOf(0x00, 0x00, 0x80.toByte(), 0x3F)))
        assertEquals(3.45f, Disto.readFloat32Le(
            byteArrayOf(0xCD.toByte(), 0xCC.toByte(), 0x5C, 0x40)))
        assertEquals(3, Disto.readUint16Le(byteArrayOf(0x03, 0x00)))
        assertEquals(1000, Disto.readUint16Le(byteArrayOf(0xE8.toByte(), 0x03)))
        // Sources differ on whether the unit is one byte or two: one byte reads as itself.
        assertEquals(2, Disto.readUint16Le(byteArrayOf(0x02)))
        // Short buffers degrade rather than crash a BLE callback.
        assertTrue(Disto.readFloat32Le(byteArrayOf(0x00)).isNaN())
        assertEquals(-1, Disto.readUint16Le(byteArrayOf()))
        assertEquals("CD CC 5C 40", Disto.hex(byteArrayOf(0xCD.toByte(), 0xCC.toByte(), 0x5C, 0x40)))
    }

    @Test
    fun `only the codes every source agrees are metric are trusted`() {
        for (c in 0..3) assertTrue(Disto.isMetric(c))
        assertFalse(Disto.isMetric(4))
        assertFalse(Disto.isMetric(104))
        assertFalse(Disto.isMetric(-1))
    }

    @Test
    fun `nonsense values are never a length`() {
        assertFalse(Disto.plausible(0f))
        assertFalse(Disto.plausible(-1f))
        assertFalse(Disto.plausible(Float.NaN))
        assertFalse(Disto.plausible(9999f))
        assertTrue(Disto.plausible(2.5f))
    }

    @Test
    fun `a distance arriving before any unit is held, not guessed`() {
        val p = Disto.Pairing()
        assertNull(p.onDistance(2.5f))
        assertEquals(2500, p.onUnit(0)?.reading?.mm)
    }

    @Test
    fun `once the unit is known later readings come straight through`() {
        val p = Disto.Pairing()
        assertNull(p.onUnit(3))             // read once at subscribe time
        assertEquals(1219, p.onDistance(1.2192f)?.reading?.mm)
    }

    @Test
    fun `a non-metric display is refused with a reason, not converted`() {
        val p = Disto.Pairing()
        p.onUnit(4)
        val o = p.onDistance(1.2192f)
        assertNull(o?.reading)
        assertNotNull(o?.refused)
        assertTrue(o!!.refused!!.contains("unit code 4"))
        // Back to metres, and readings flow again.
        p.onUnit(0)
        assertEquals(2000, p.onDistance(2.0f)?.reading?.mm)
    }
}
