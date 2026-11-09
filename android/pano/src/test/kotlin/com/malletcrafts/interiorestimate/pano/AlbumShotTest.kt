package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The progress shot's file (port 61c slice K5; DESIGN-port61c.md §20, "THE SHOT'S FILE"): the sample and the size a
 * camera's picture is written at, the EXIF orientation table, S5's JPEG test and cap (S5 QUOTE 9a51733, asis.js: the album
 * photo's `…/img` PUT, MAX_IMG = 8 * 1024 * 1024), and which shots the trims delete. Every expected value is written out
 * by hand.
 */
class AlbumShotTest {

    @Test fun `the sample keeps the long side at least 3072 and the size never grows`() {
        for ((w, h) in listOf(4000 to 3000, 3000 to 4000, 3072 to 2304, 1600 to 1200, 12000 to 9000, 8000 to 6000)) {
            val s = AlbumShot.sampleFor(w, h)
            assertEquals(1, Integer.bitCount(s), "$w x $h: a power of two")
            val long = maxOf(w, h)
            if (long >= AlbumShot.LONG_SIDE) assertTrue(long / s >= AlbumShot.LONG_SIDE, "$w x $h: the decoded long side stays at least 3072")
            assertTrue(long / (s * 2) < AlbumShot.LONG_SIDE, "$w x $h: the largest such sample")
            val (sw, sh) = AlbumShot.sizeFor(w / s, h / s)
            assertTrue(sw <= w / s && sh <= h / s, "$w x $h: never up")
            assertEquals(minOf(AlbumShot.LONG_SIDE, maxOf(w / s, h / s)), maxOf(sw, sh), "$w x $h")
            // the aspect kept to the pixel
            assertTrue(Math.abs(sw.toDouble() / sh - w.toDouble() / h) * maxOf(sw, sh) < 1.5, "$w x $h -> $sw x $sh")
        }
        assertEquals(1, AlbumShot.sampleFor(4000, 3000))
        assertEquals(1, AlbumShot.sampleFor(3000, 4000))
        assertEquals(1, AlbumShot.sampleFor(1600, 1200))
        assertEquals(2, AlbumShot.sampleFor(12000, 9000))
        assertEquals(2, AlbumShot.sampleFor(8000, 6000))
        assertEquals(4, AlbumShot.sampleFor(16000, 12000))
        assertEquals(3072 to 2304, AlbumShot.sizeFor(4000, 3000))
        assertEquals(2304 to 3072, AlbumShot.sizeFor(3000, 4000))
        assertEquals(3072 to 2304, AlbumShot.sizeFor(3072, 2304))
        assertEquals(1600 to 1200, AlbumShot.sizeFor(1600, 1200))
        assertEquals(3072 to 2304, AlbumShot.sizeFor(6000, 4500))
        assertEquals(0 to 0, AlbumShot.sizeFor(0, 0), "nothing decoded is nothing scaled")
    }

    // A picture as rows of cells; the EXIF table is checked by turning a stored picture back into the one it shows.
    private fun rot90(a: List<List<Int>>): List<List<Int>> = List(a[0].size) { c -> List(a.size) { r -> a[a.size - 1 - r][c] } }
    private fun mirror(a: List<List<Int>>): List<List<Int>> = a.map { it.reversed() }
    private fun turned(a: List<List<Int>>, t: AlbumShot.Turn): List<List<Int>> {
        var x = a
        repeat(t.degrees / 90) { x = rot90(x) }
        return if (t.mirror) mirror(x) else x
    }

    @Test fun `every EXIF orientation turns and mirrors as the EXIF table says, anything else not at all`() {
        assertEquals(AlbumShot.Turn(0, false), AlbumShot.turnOf(1))
        assertEquals(AlbumShot.Turn(0, true), AlbumShot.turnOf(2))
        assertEquals(AlbumShot.Turn(180, false), AlbumShot.turnOf(3))
        assertEquals(AlbumShot.Turn(180, true), AlbumShot.turnOf(4))
        assertEquals(AlbumShot.Turn(90, true), AlbumShot.turnOf(5))
        assertEquals(AlbumShot.Turn(90, false), AlbumShot.turnOf(6))
        assertEquals(AlbumShot.Turn(270, true), AlbumShot.turnOf(7))
        assertEquals(AlbumShot.Turn(270, false), AlbumShot.turnOf(8))
        for (o in listOf(0, 9, -1, 255)) assertEquals(AlbumShot.Turn(0, false), AlbumShot.turnOf(o), "$o")
        // the picture as it is meant to be seen, 2 rows by 3, and how a camera stores it under each orientation
        // (TIFF 6.0: the 0th row and column of the stored picture are at the visual top-left (1), top-right (2),
        // bottom-right (3), bottom-left (4), left-top (5), right-top (6), right-bottom (7), left-bottom (8))
        val seen = listOf(listOf(1, 2, 3), listOf(4, 5, 6))
        val stored = mapOf(
            1 to seen,
            2 to listOf(listOf(3, 2, 1), listOf(6, 5, 4)),
            3 to listOf(listOf(6, 5, 4), listOf(3, 2, 1)),
            4 to listOf(listOf(4, 5, 6), listOf(1, 2, 3)),
            5 to listOf(listOf(1, 4), listOf(2, 5), listOf(3, 6)),
            6 to listOf(listOf(3, 6), listOf(2, 5), listOf(1, 4)),
            7 to listOf(listOf(6, 3), listOf(5, 2), listOf(4, 1)),
            8 to listOf(listOf(4, 1), listOf(5, 2), listOf(6, 3)),
        )
        for ((o, s) in stored) assertEquals(seen, turned(s, AlbumShot.turnOf(o)), "orientation $o")
    }

    @Test fun `a JPEG is FF D8 FF`() {
        assertTrue(AlbumShot.isJpeg(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())))
        assertTrue(AlbumShot.isJpeg(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())))
        assertFalse(AlbumShot.isJpeg(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)), "a PNG")
        assertFalse(AlbumShot.isJpeg(byteArrayOf(0xFF.toByte(), 0xD8.toByte())), "too short")
        assertFalse(AlbumShot.isJpeg(ByteArray(0)))
    }

    @Test fun `the qualities step down and 8 MB is S5's cap`() {
        assertEquals(listOf(90, 75, 60), AlbumShot.QUALITIES)
        assertEquals(AlbumShot.QUALITIES.sortedDescending(), AlbumShot.QUALITIES)
        assertEquals(8 * 1024 * 1024, AlbumShot.MAX_BYTES, "asis.js MAX_IMG")
        assertEquals(3072, AlbumShot.LONG_SIDE, "VIEWER_TARGET, the largest the phone draws a photo")
    }

    @Test fun `orphans - a shot no queue or dropped holds goes after ten minutes, a younger one and a held one stay`() {
        val now = 10_000_000_000L
        val files = listOf(
            AlbumShot.ShotFile("ALB-000000000001", now - 11 * 60_000),   // held: stays however old
            AlbumShot.ShotFile("ALB-000000000002", now - 11 * 60_000),   // not held, old: goes
            AlbumShot.ShotFile("ALB-000000000003", now - 2 * 60_000),    // not held, young: its op may be about to be queued
            AlbumShot.ShotFile("ALB-000000000004.jpg.part", now - 11 * 60_000), // a temp file a killed write left: goes
        )
        assertEquals(listOf("ALB-000000000002", "ALB-000000000004.jpg.part"), AlbumShot.orphans(files, setOf("ALB-000000000001"), now))
        assertEquals(10 * 60_000L, AlbumShot.SHOT_GRACE_MS)
        assertEquals(emptyList(), AlbumShot.orphans(emptyList(), emptySet(), now))
    }

    @Test fun `an outbox that could not be read deletes no shot - an unreadable outbox is not an empty one (the K5 critic, finding 5)`() {
        val now = 10_000_000_000L
        val files = listOf(AlbumShot.ShotFile("ALB-000000000002", now - 3_600_000))
        assertEquals(emptyList(), AlbumShot.orphans(files, null, now))
        assertEquals(listOf("ALB-000000000002"), AlbumShot.orphans(files, emptySet(), now), "an empty outbox holds nothing")
    }

    @Test fun `the camera app's own files go a day old, its others never (the K5 critic, finding 12)`() {
        val now = 10_000_000_000L
        val day = 24 * 3_600_000L
        val files = listOf(
            AlbumShot.ShotFile("album-ALB-000000000005.jpg", now - day - 1),  // a day old: goes
            AlbumShot.ShotFile("album-ALB-000000000006.jpg", now - day + 60_000), // younger: stays
            AlbumShot.ShotFile("shot-1730000000000.jpg", now - 30 * day),     // the Capture 360 sheet's: not K5's
        )
        assertEquals(listOf("album-ALB-000000000005.jpg"), AlbumShot.rawStale(files, now))
        assertEquals(day, AlbumShot.RAW_GRACE_MS)
    }
}
