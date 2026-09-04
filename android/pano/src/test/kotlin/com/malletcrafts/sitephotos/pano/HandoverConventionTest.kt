package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

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
                "YS_KB — YS_1402_SKYI_INTERIOR/", p)
        // Path metacharacters must not become directories.
        assertTrue(!Handover.relativePath("A/B", "P:1", "R?").contains("A/B"))
    }

    @Test
    fun `an unknown client still yields a usable folder`() {
        val p = Handover.relativePath("", "P", "Master Bedroom")
        assertTrue(p.endsWith("/MB — P/"), p)
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

    @Test
    fun `two projects of one client with the same room get different albums`() {
        // Amit, 2026-08-22: "when a same client have multiple projects which
        // will have same room names under same site, what will happen."
        //
        // The FILES never collided — these are different directories. What
        // collided is what a person sees: Android names a gallery album after
        // its LEAF folder alone, so both showed in ImageMeter's picker as
        // "YS_MB", and picking the wrong one is silent.
        val a = Handover.relativePath("Yogesh Sahasrabudhe", "Kids bed and master bed",
                                      "Master Bedroom")
        val b = Handover.relativePath("Yogesh Sahasrabudhe", "Wardrobe refit",
                                      "Master Bedroom")
        val leafA = a.trimEnd('/').substringAfterLast('/')
        val leafB = b.trimEnd('/').substringAfterLast('/')
        assertTrue(leafA != leafB, "both projects still land in an album called $leafA")
        // and the leaf still SAYS which is which, in words, because the
        // import is done by hand
        assertTrue(leafA.contains("Kids bed"), leafA)
        assertTrue(leafB.contains("Wardrobe"), leafB)
        // the token prefix survives, so the album still matches the SKU code
        assertTrue(leafA.startsWith("YS_MB"), leafA)
    }

    @Test
    fun `a very long project name does not run away with the album name`() {
        val leaf = Handover.relativePath(
            "Yogesh Sahasrabudhe",
            "Complete interior fitout including wardrobes beds and lofts",
            "Master Bedroom").trimEnd('/').substringAfterLast('/')
        assertTrue(leaf.length <= "YS_MB — ".length + 29, "album name too long: $leaf")
        assertTrue(leaf.endsWith("…"), leaf)
    }

    // ---- The round trip. Every face this app writes must read back as the
    // same face, and for weeks two of them did not: filename() wrote "top"
    // and "bottom" while the reader looked for "up" and "down", so a capture
    // that split into six showed four. No error, no log line — the ceiling
    // and floor were simply parsed as "not a face" and dropped. A convention
    // with a writer and a reader in different files needs a test that closes
    // the loop, not two tests that each check one half.

    @Test
    fun `every face survives the trip through its own filename`() {
        for (face in Panorama.FACES.map { it.first }) {
            val name = Handover.filename("MCAP-0123456789ab", face)
            val token = name.substringBeforeLast('.').substringAfterLast('_')
            assertEquals(face, Handover.faceOfToken(token),
                "$face was written as '$name' and did not read back as $face")
        }
    }

    @Test
    fun `the two that were lost are named in the test that lost them`() {
        assertEquals("MCAP-0123456789ab_top.jpg",
                     Handover.filename("MCAP-0123456789ab", "up"))
        assertEquals("MCAP-0123456789ab_bottom.jpg",
                     Handover.filename("MCAP-0123456789ab", "down"))
        assertEquals("up", Handover.faceOfToken("top"))
        assertEquals("down", Handover.faceOfToken("bottom"))
    }

    @Test
    fun `a bare face name still reads, so nothing written the old way is orphaned`() {
        assertEquals("up", Handover.faceOfToken("up"))
        assertEquals("front", Handover.faceOfToken("front"))
    }

    @Test
    fun `hand-typed synonyms read back, matching the server's LABEL_TO_FACE`() {
        assertEquals("up", Handover.faceOfToken("ceiling"))
        assertEquals("down", Handover.faceOfToken("floor"))
        assertEquals("up", Handover.faceOfToken("  TOP "))
    }

    @Test
    fun `something that is not a face is still refused`() {
        assertNull(Handover.faceOfToken("photo"))
        assertNull(Handover.faceOfToken("sideways"))
        assertNull(Handover.faceOfToken(""))
        assertNull(Handover.faceOfToken(null))
    }
}
