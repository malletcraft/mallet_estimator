package com.malletcrafts.interiorestimate.pano

/**
 * Which rooms a site of a given type has, so a new site starts with its own
 * eight rooms instead of the master's twenty-nine (Amit, 2026-10-09: "flat type
 * selection is not good in apk. it still shows old 30 + rooms even for new
 * site."). The same rules as the Face Prep prototype's property picker, so
 * the phone and the prototype agree; names are the bench's room master.
 */
object FlatPresets {
    // THE WIRE, Album.WORKS' idiom (K1b; port 61c slice M2): TYPES are the stored site-type values
    // (`localSiteType`, ERP's Site rows) and the room lists are the bench's room master. The SHOWN
    // words go through Masters.site (the type chips, a site row's sub) and Masters.room (the
    // Add-site room ticks) at the dialogs — one stored value, one shown word, no second answer.
    val TYPES = listOf("Flat", "Bungalow", "Row House", "Office", "Shop", "Other") // text:value stored site types, shown through Masters.site
    private val BEDS = listOf("Master Bedroom", "Kids Bedroom", "Guest Bedroom", "Bedroom 4", "Bedroom 5") // text:value the bench's room master, shown through Masters.room
    private val BATHS = listOf("Bathroom", "Toilet 1", "Toilet 2", "Toilet 3", "Toilet 4") // text:value the bench's room master, shown through Masters.room

    fun hasBedrooms(type: String) = type in listOf("Flat", "Bungalow", "Row House") // text:value stored site types, compared

    fun rooms(type: String, bhk: Int): List<String> {
        val n = bhk.coerceIn(1, 5)
        val beds = BEDS.take(n); val baths = BATHS.take(n)
        return when (type) {
            "Flat" -> listOf("Foyer", "Living Room", "Kitchen") + beds + baths + "Balcony" + "Dry Balcony" + // text:value the bench's room master, shown through Masters.room
                (if (n >= 2) listOf("Utility") else emptyList()) + (if (n >= 3) listOf("Dining Room") else emptyList()) // text:value the bench's room master, shown through Masters.room
            "Bungalow", "Row House" -> listOf("Foyer", "Living Room", "Dining Room", "Kitchen", "Utility", "Pooja Room") + // text:value the bench's room master, shown through Masters.room
                beds + baths + listOf("Passage", "Staircase", "Balcony", "Terrace") + // text:value the bench's room master, shown through Masters.room
                (if (type == "Bungalow") listOf("Car Porch", "Garden") else listOf("Car Porch")) // text:value the bench's room master, shown through Masters.room
            "Office" -> listOf("Foyer", "Passage", "Study", "Store", "Toilet 1", "Other") // text:value the bench's room master, shown through Masters.room
            "Shop" -> listOf("Other", "Store", "Toilet 1") // text:value the bench's room master, shown through Masters.room
            else -> listOf("Other") // text:value the bench's room master, shown through Masters.room
        }
    }
}
