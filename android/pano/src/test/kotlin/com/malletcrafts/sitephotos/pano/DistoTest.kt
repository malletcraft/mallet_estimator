package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DistoTest {

    @Test
    fun `the wire value is metres whatever the meter displays`() {
        // 2.5 m sent while the meter shows feet is still 2.5 m on the wire.
        // The most-copied community unit table gets this backwards and would
        // turn a 2.5 m wall into 2.5 ft.
        assertEquals(2500, Disto.toMm(2.5f))
        assertEquals(1219, Disto.toMm(1.2192f))
        assertEquals(0, Disto.toMm(0f))
    }

    @Test
    fun `little-endian decoding matches the characteristic layout`() {
        // 1.0f == 0x3F800000, little-endian on the wire.
        val one = byteArrayOf(0x00, 0x00, 0x80.toByte(), 0x3F)
        assertEquals(1.0f, Disto.readFloat32Le(one))
        assertEquals(3, Disto.readUint16Le(byteArrayOf(0x03, 0x00)))
        assertEquals(1000, Disto.readUint16Le(byteArrayOf(0xE8.toByte(), 0x03)))
        // Short buffers degrade rather than crash a BLE callback.
        assertTrue(Disto.readFloat32Le(byteArrayOf(0x00)).isNaN())
        assertEquals(-1, Disto.readUint16Le(byteArrayOf()))
    }

    @Test
    fun `area and volume modes are refused, not treated as lengths`() {
        // The SAME characteristic carries an area in area mode (unit + 100)
        // and a volume in volume mode (+ 1000). Neither is a wall length.
        assertTrue(Disto.isLinear(0))
        assertTrue(Disto.isLinear(3))
        assertTrue(Disto.isLinear(14))
        assertFalse(Disto.isLinear(100))    // m²
        assertFalse(Disto.isLinear(1000))   // m³
        assertFalse(Disto.usable(2.5f, 100))
        assertTrue(Disto.usable(2.5f, 0))
    }

    @Test
    fun `nonsense readings never reach a measurement line`() {
        assertFalse(Disto.usable(0f, 0))
        assertFalse(Disto.usable(-1f, 0))
        assertFalse(Disto.usable(Float.NaN, 0))
        assertFalse(Disto.usable(9999f, 0))
    }

    @Test
    fun `imperial display is recognised from the unit code`() {
        assertFalse(Disto.isImperial(0))    // m
        assertFalse(Disto.isImperial(3))    // mm
        assertTrue(Disto.isImperial(4))     // ft
        assertTrue(Disto.isImperial(8))     // ft-in-fraction
        assertTrue(Disto.isImperial(13))    // in-fraction
        assertFalse(Disto.isImperial(14))   // yd
    }

    @Test
    fun `a distance arriving before any unit is held, not guessed`() {
        val p = Disto.Pairing()
        // Leica's own note: data comes in pairs. Emitting on the distance
        // alone would mean inventing a unit we were never told.
        assertNull(p.onDistance(2.5f))
        val r = p.onUnit(0)
        assertEquals(2500, r?.mm)
        assertEquals(false, r?.deviceImperial)
    }

    @Test
    fun `once the unit is known later readings emit immediately`() {
        val p = Disto.Pairing()
        assertNull(p.onUnit(4))             // read once at subscribe time
        val r = p.onDistance(1.2192f)
        assertEquals(1219, r?.mm)
        assertEquals(true, r?.deviceImperial)
        // ...and a mode change to area silences it again.
        assertNull(p.onUnit(104))
        assertNull(p.onDistance(2.0f))
    }
}
