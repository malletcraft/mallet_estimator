package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
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
        // the refusal travels TYPED (port 61c slice M6): the wire's parser carries no language, and
        // LaserNote renders it — pinned here under both Tx, the figure as the parser formatted it
        val r = assertNotNull(o?.refused)
        assertEquals(Disto.Refusal.Kind.NOT_METRIC, r.kind)
        assertEquals("Meter is not showing metres/mm (unit code 4) — set it to m or mm",
            LaserNote.refusal(r)(Tx.EN))
        assertEquals("मीटरवर मीटर/mm दिसत नाही (युनिट कोड 4) — तो m किंवा mm वर सेट करा", LaserNote.refusal(r)(Tx.MR))
        // Back to metres, and readings flow again.
        p.onUnit(0)
        assertEquals(2000, p.onDistance(2.0f)?.reading?.mm)
    }

    @Test
    fun `an implausible distance is refused as not a length, the figure pre-formatted pure`() {
        val p = Disto.Pairing()
        p.onUnit(0)
        val o = p.onDistance(0.0f)
        assertNull(o?.reading)
        val r = assertNotNull(o?.refused)
        assertEquals(Disto.Refusal.Kind.NOT_A_LENGTH, r.kind)
        assertEquals("0.0000", r.arg)
        assertEquals("Not a length (0.0000)", LaserNote.refusal(r)(Tx.EN))
        assertEquals("हे लांबीचे मोजमाप नाही (0.0000)", LaserNote.refusal(r)(Tx.MR))
    }
}
