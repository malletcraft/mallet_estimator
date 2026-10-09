package com.malletcrafts.sitephotos.pano

/**
 * Which rooms a site of a given type has, so a new site starts with its own
 * eight rooms instead of the master's twenty-nine (Amit, 2026-10-09: "flat type
 * selection is not good in apk. it still shows old 30 + rooms even for new
 * site."). The same rules as the Face Prep prototype's property picker, so
 * the phone and the prototype agree; names are the bench's room master.
 */
object FlatPresets {
    val TYPES = listOf("Flat", "Bungalow", "Row House", "Office", "Shop", "Other")
    private val BEDS = listOf("Master Bedroom", "Kids Bedroom", "Guest Bedroom", "Bedroom 4", "Bedroom 5")
    private val BATHS = listOf("Bathroom", "Toilet 1", "Toilet 2", "Toilet 3", "Toilet 4")

    fun hasBedrooms(type: String) = type in listOf("Flat", "Bungalow", "Row House")

    fun rooms(type: String, bhk: Int): List<String> {
        val n = bhk.coerceIn(1, 5)
        val beds = BEDS.take(n); val baths = BATHS.take(n)
        return when (type) {
            "Flat" -> listOf("Foyer", "Living Room", "Kitchen") + beds + baths + "Balcony" +
                (if (n >= 2) listOf("Utility") else emptyList()) + (if (n >= 3) listOf("Dining Room") else emptyList())
            "Bungalow", "Row House" -> listOf("Foyer", "Living Room", "Dining Room", "Kitchen", "Utility", "Pooja Room") +
                beds + baths + listOf("Passage", "Staircase", "Balcony", "Terrace") +
                (if (type == "Bungalow") listOf("Car Porch", "Garden") else listOf("Car Porch"))
            "Office" -> listOf("Foyer", "Passage", "Study", "Store", "Toilet 1", "Other")
            "Shop" -> listOf("Other", "Store", "Toilet 1")
            else -> listOf("Other")
        }
    }
}
