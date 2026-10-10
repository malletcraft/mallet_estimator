package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlatPresetsTest {
    @Test
    fun `a 2 BHK flat is ten rooms, not twenty-nine`() {
        assertEquals(listOf("Foyer", "Living Room", "Kitchen", "Master Bedroom", "Kids Bedroom", "Bathroom", "Toilet 1", "Balcony", "Dry Balcony", "Utility"),
            FlatPresets.rooms("Flat", 2))
    }

    @Test
    fun `every preset room is a bench master room`() {
        val master = setOf("Master Bedroom", "Kids Bedroom", "Guest Bedroom", "Bedroom 4", "Bedroom 5", "Bathroom", "Toilet 1", "Toilet 2",
            "Toilet 3", "Toilet 4", "Toilet 5", "Living Room", "Dining Room", "Family Lounge", "Home Theatre", "Kitchen", "Study", "Utility",
            "Store", "Servant Room", "Foyer", "Passage", "Staircase", "Balcony", "Dry Balcony", "Terrace", "Car Porch", "Garden", "Pooja Room", "Other")
        for (t in FlatPresets.TYPES) for (n in 1..5) assertTrue(master.containsAll(FlatPresets.rooms(t, n)), "$t $n")
    }
}
