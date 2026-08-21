package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Holds the device's naming to the server's. The filename is the only
 * machine-readable identity that survives the ImageMeter round trip, so a
 * device that spells it differently orphans every annotation it produces.
 */
class HandoverConventionTest {

    @Test
    fun `filenames use top and bottom like the server and the existing drive`() {
        val id = "MCAP-a1b2c3d4e5f6"
        assertEquals("${id}_front.jpg", Handover.filename(id, "front"))
        assertEquals("${id}_top.jpg", Handover.filename(id, "up"))
        assertEquals("${id}_bottom.jpg", Handover.filename(id, "down"))
    }

    @Test
    fun `the caption reads like the servers caption`() {
        // The exact string the round-trip test file carried on 2026-08-17.
        assertEquals(
            "MCAP-a1b2c3d4e5f6 · Master Bedroom · Front · 2026-08-17 Carpentry",
            Handover.captionText("MCAP-a1b2c3d4e5f6", "Master Bedroom", "front",
                                 "2026-08-17", "Carpentry"),
        )
        // Stage is optional; the caption must not grow a trailing separator.
        assertEquals(
            "MCAP-a1b2c3d4e5f6 · Balcony · Top · 2026-08-17",
            Handover.captionText("MCAP-a1b2c3d4e5f6", "Balcony", "up", "2026-08-17", null),
        )
    }

    @Test
    fun `minted ids match the servers pattern exactly`() {
        val id = Handover.mintDeviceId(byteArrayOf(0xA1.toByte(), 0xB2.toByte(),
            0xC3.toByte(), 0xD4.toByte(), 0xE5.toByte(), 0xF6.toByte()))
        assertEquals("MCAP-a1b2c3d4e5f6", id)
        assertTrue(Handover.isDeviceId(id))
        // Uppercase would orphan annotations: the server pattern is
        // lowercase hex only.
        assertTrue(!Handover.isDeviceId("MCAP-A1B2C3D4E5F6"))
    }

    @Test
    fun `every face the projection makes has a name and a label`() {
        for ((face, _, _) in Panorama.FACES) {
            assertTrue(Handover.FACE_LABELS.containsKey(face), face)
            assertTrue(Handover.filename("MCAP-000000000000", face).endsWith(".jpg"))
        }
    }

    @Test
    fun `the folder leaf carries the client so albums stay unambiguous`() {
        // YS_KB: the same folder name ImageMeter already uses and the same
        // prefix the SKU codes use. The client half matters — Android names
        // the album after the LEAF, so a bare "KB" would collide with every
        // other project's kids bedroom in the picker ImageMeter imports from.
        val p = Handover.relativePath("Yogesh Sahasrabudhe", "YS_1402_SKYI_INTERIOR",
                                      "Kids Bedroom")
        assertEquals(
            "Pictures/MCFT Site Photos/Yogesh Sahasrabudhe/YS_1402_SKYI_INTERIOR/" +
                "YS_KB/", p)
        // Path metacharacters must not become directories.
        assertTrue(!Handover.relativePath("A/B", "P:1", "R?").contains("A/B"))
    }

    @Test
    fun `an unknown client still yields a usable folder`() {
        val p = Handover.relativePath("", "P", "Master Bedroom")
        assertTrue(p.endsWith("/MB/"), p)
    }

    @Test
    fun `a caption survives a token that is not one of the six faces`() {
        // A FLAT photograph carries "photo", which is deliberately not a
        // face. captionText used to throw on it, and because both the Camera
        // and the Gallery route go through this one helper, the whole
        // single-photo feature was dead on arrival — shipped and never run.
        val text = Handover.captionText(
            "MCAP-0123456789ab", "Master Bedroom", "photo", "2026-08-21",
            "Modular carpentry install")
        assertTrue(text.contains("MCAP-0123456789ab"))
        // Raw, exactly as handover.py writes it — this class holds the two
        // implementations to the same string.
        assertTrue(text.contains("photo"), "the token should read back: $text")
        assertTrue(text.contains("Master Bedroom"))
    }

    @Test
    fun `the six faces still read as their proper labels`() {
        // The passthrough must not have cost the mapping: "up" is TOP on this
        // Drive, and 42 hand-made files already say so.
        assertTrue(Handover.captionText("MCAP-0123456789ab", "Kitchen", "up")
            .contains("Top"))
        assertTrue(Handover.captionText("MCAP-0123456789ab", "Kitchen", "down")
            .contains("Bottom"))
    }

    @Test
    fun `a filename still refuses a token that is not a face`() {
        // Strict HERE on purpose, and the asymmetry is the point: a bogus
        // token in a FILENAME writes a file nothing can ever match again,
        // while a bogus token in a caption is just an odd word under a
        // picture. The single-photo path builds its own name for this reason.
        assertFailsWith<IllegalStateException> {
            Handover.filename("MCAP-0123456789ab", "photo")
        }
    }
}
