package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The same cases the server's test_estimator.py asserts for room_abbr,
 * copied deliberately. The phone names folders offline, so if this drifts
 * from the server's grammar the folder and the SKU code stop agreeing and
 * nothing announces it.
 */
class RoomTokenTest {

    @Test
    fun `matches the servers room_abbr on its own test cases`() {
        assertEquals("KIT", RoomToken.of("Kitchen"))
        assertEquals("STU", RoomToken.of("Study"))
        assertEquals("BAL", RoomToken.of("Balcony"))
        assertEquals("MB", RoomToken.of("Master Bedroom"))
        assertEquals("LR", RoomToken.of("Living Room"))
        assertEquals("AR", RoomToken.of("All Rooms"))
    }

    @Test
    fun `splits on the same separators the server splits on`() {
        // _WORD_SPLIT is [\s_\-]+ server-side, so these are all two words.
        assertEquals("KB", RoomToken.of("Kids_Bedroom"))
        assertEquals("KB", RoomToken.of("Kids-Bedroom"))
        assertEquals("KB", RoomToken.of("Kids   Bedroom"))
        assertEquals("MB", RoomToken.of("  Master Bedroom  "))
    }

    @Test
    fun `degrades instead of throwing on nothing`() {
        assertEquals("", RoomToken.of(""))
        assertEquals("", RoomToken.of(null))
        assertEquals("", RoomToken.of("   "))
    }

    @Test
    fun `the tree label keeps the full name behind the token`() {
        assertEquals("MB · Master Bedroom", RoomToken.label("Master Bedroom"))
        // A room whose token is itself must not read "KIT · KIT".
        assertEquals("KIT", RoomToken.label("KIT"))
    }
}
