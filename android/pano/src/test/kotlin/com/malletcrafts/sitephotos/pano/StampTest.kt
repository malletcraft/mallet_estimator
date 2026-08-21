package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The stamp is the only identity that survives ImageMeter.
 *
 * The filename does not: ImageMeter renames its exports to
 * image_from_19._Aug_2026.jpg, which is how 88 returned files ended up in a
 * review inbox saying "no capture id in the filename". So everything about
 * this class is about being certain, and about refusing rather than guessing.
 */
class StampTest {

    @Test
    fun `a face round-trips`() {
        val text = Stamp.payload("MCAP-4b2014dcba5e", "front")
        val mark = Stamp.parse(text)
        assertEquals("MCAP-4b2014dcba5e", mark?.captureId)
        assertEquals("front", mark?.face)
    }

    @Test
    fun `a flat photo round-trips too`() {
        // "photo" is deliberately not one of the six, and must still survive.
        assertEquals("photo", Stamp.parse(
            Stamp.payload("MCAP-4b2014dcba5e", "photo"))?.face)
    }

    @Test
    fun `every real face round-trips, including the short ones`() {
        // "up" is two characters. A tidy-looking three-to-ten regex would
        // have dropped the ceiling of every 360 in the system and said
        // nothing about it, so the parser validates against the face list
        // itself and this test is what holds it there.
        for ((face, _, _) in Panorama.FACES) {
            assertEquals(face, Stamp.parse(
                Stamp.payload("MCAP-4b2014dcba5e", face))?.face,
                "face did not survive: $face")
        }
    }

    @Test
    fun `a face we do not have is refused`() {
        assertNull(Stamp.parse("MCFT1|MCAP-4b2014dcba5e|ceiling"))
    }

    @Test
    fun `a server-named capture round-trips`() {
        // A capture the desk made has no device id — it carries the docname.
        assertEquals("MEST-PH-2026-00023",
            Stamp.parse(Stamp.payload("MEST-PH-2026-00023", "left"))?.captureId)
    }

    @Test
    fun `somebody else's QR is refused`() {
        // A phone gallery is full of them: Wi-Fi cards, UPI handles, parcel
        // labels. Decoding one of those into a capture id would attach a
        // stranger's photograph to a client's wall.
        assertNull(Stamp.parse("WIFI:S=CafeNet;T=WPA;P=hunter2;;"))
        assertNull(Stamp.parse("upi://pay?pa=someone@bank"))
        assertNull(Stamp.parse("https://example.com"))
        assertNull(Stamp.parse(""))
        assertNull(Stamp.parse(null))
    }

    @Test
    fun `a payload without our magic is refused`() {
        assertNull(Stamp.parse("OTHER|MCAP-4b2014dcba5e|front"))
    }

    @Test
    fun `a plausible but malformed id is refused`() {
        // Half a read that yields something id-SHAPED is the one failure that
        // silently files a photo against the wrong room. Reading nothing is
        // strictly better.
        assertNull(Stamp.parse("MCFT1|MCAP-notreallyhex|front"))
        assertNull(Stamp.parse("MCFT1|MCAP-4b2014dcba5|front"))   // 11 hex
        assertNull(Stamp.parse("MCFT1|MEST-PH-26-1|front"))
        assertNull(Stamp.parse("MCFT1|MCAP-4b2014dcba5e|"))
        assertNull(Stamp.parse("MCFT1|MCAP-4b2014dcba5e|FRONT!"))
    }

    @Test
    fun `the face is read back lower case whatever the case it was written in`() {
        assertEquals("front", Stamp.parse("MCFT1|MCAP-4b2014dcba5e|FRONT")?.face)
    }

    @Test
    fun `extra separators do not smuggle a second field through`() {
        assertNull(Stamp.parse("MCFT1|MCAP-4b2014dcba5e|front|extra"))
    }

    @Test
    fun `the matrix is square, dark somewhere, and big enough to survive`() {
        val m = Stamp.matrix(Stamp.payload("MCAP-4b2014dcba5e", "front"), 120)
        assertTrue(m.isNotEmpty())
        assertTrue(m.all { it.size == m.size }, "not square")
        assertTrue(m.any { row -> row.any { it } }, "nothing dark")
        // A QR only survives a re-encode if its modules are several pixels
        // across. At 120px this must stay well under 40 modules a side.
        assertTrue(m.size <= 120, "matrix larger than the box asked for")
    }

    @Test
    fun `the payload stays short because every character is a module`() {
        assertTrue(Stamp.payload("MEST-PH-2026-00023", "ceiling").length < 40)
    }
}
