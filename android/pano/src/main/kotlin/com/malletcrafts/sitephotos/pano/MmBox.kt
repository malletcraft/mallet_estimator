package com.malletcrafts.sitephotos.pano

import kotlin.math.roundToInt

/**
 * What a reading box writes when it loses focus or Done is pressed (v52 slice 11 review). The box shows a figure
 * rounded to whole mm -- a reading, or for a chain a DERIVED segment or total, which is not a reading at all. Before
 * this rule the box compared its rounded text with the unrounded figure, so tapping a derived 2404.5 (shown "2405")
 * and tapping away saved 2405 as a measured site reading nobody took, and ReadingsSync sent it to S5.
 *
 * sp.js commits only on the input's `change` event, i.e. only what the person TYPED. Same here: an unedited box
 * writes nothing, whatever it shows.
 */
object MmBox {

    /** What to write: [value] is the new reading, null to remove the reading. */
    class Commit(val value: Double?)

    /** The text a box shows for [value]: whole mm, empty when there is nothing. */
    fun shownText(value: Double?): String = value?.roundToInt()?.toString() ?: ""

    /**
     * The write for a box that showed [value] and now holds [text]; null writes nothing. [isReading]: [value] is a
     * stored reading (clearing the box removes it), not a derived or laser figure (clearing it has nothing to remove).
     */
    fun commit(value: Double?, text: String, isReading: Boolean = true): Commit? {
        if (text == shownText(value)) return null
        val v = text.toDoubleOrNull()
        if (v != null && v > 0) return Commit(v)
        if (text.isBlank() && value != null && isReading) return Commit(null)
        return null
    }
}
