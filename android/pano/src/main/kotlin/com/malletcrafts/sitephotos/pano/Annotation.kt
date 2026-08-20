package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Annotations are DATA, never pixels: a measurement line is two points in
 * normalized image space plus a millimetre value; a note pin is a point plus
 * text. Rendering happens at view time — app, ERPNext viewer, exports — so
 * the original face stays pristine and a re-measure edits a number.
 *
 * Coordinates are normalized (0..1 of image width/height) so the same JSON
 * is correct at every display and export resolution.
 */
object Annotation {

    /** Canonical unit is the millimetre — same as every OpenCutList number
     *  in the house. */
    data class Line(
        val x1: Double, val y1: Double,
        val x2: Double, val y2: Double,
        val mm: Int,
    )

    data class Pin(val x: Double, val y: Double, val text: String)

    data class FaceAnnotations(
        val lines: List<Line> = emptyList(),
        val pins: List<Pin> = emptyList(),
    ) {
        val isEmpty: Boolean get() = lines.isEmpty() && pins.isEmpty()
    }

    /** The units a site person keys a measurement in. Everything converts
     *  to mm at entry; display converts back out. */
    enum class Unit(val label: String, val toMm: Double) {
        MM("mm", 1.0),
        CM("cm", 10.0),
        M("m", 1000.0),
        IN("in", 25.4),
        FT("ft", 304.8),
    }

    fun toMm(value: Double, unit: Unit): Int = (value * unit.toMm).roundToInt()

    /** "1220 mm" — metres with two decimals once past a metre reads better
     *  on a label than five digits of mm. */
    fun metricLabel(mm: Int): String =
        if (mm >= 1000) {
            val m = mm / 1000.0
            "${(m * 100).roundToInt() / 100.0} m"
        } else "$mm mm"

    /** Feet-and-inches to the nearest 1/8" — the resolution a tape measure
     *  and a carpenter actually use. 1220 mm → 4' 0", 1250 mm → 4' 1 1/4". */
    fun imperialLabel(mm: Int): String {
        val totalEighths = (mm / 25.4 * 8).roundToInt()
        val feet = totalEighths / (12 * 8)
        var rem = totalEighths % (12 * 8)
        val inches = rem / 8
        val eighths = rem % 8
        val frac = when {
            eighths == 0 -> ""
            eighths % 4 == 0 -> " ${eighths / 4}/2"
            eighths % 2 == 0 -> " ${eighths / 2}/4"
            else -> " $eighths/8"
        }
        return "$feet' $inches$frac\""
    }

    /** What the label says: metric always; imperial beside it when asked —
     *  Amit's "showing both dimensions when enabled". */
    fun label(mm: Int, showImperial: Boolean): String =
        if (showImperial) "${metricLabel(mm)} · ${imperialLabel(mm)}"
        else metricLabel(mm)

    /** Distance from a point to a line's nearest endpoint, normalized space —
     *  which endpoint a drag should grab, if any. Returns -1 when neither is
     *  within [grabRadius]. */
    fun nearestEndpoint(
        line: Line, x: Double, y: Double, grabRadius: Double,
    ): Int {
        val d1 = abs(line.x1 - x) + abs(line.y1 - y)
        val d2 = abs(line.x2 - x) + abs(line.y2 - y)
        return when {
            d1 <= d2 && d1 <= grabRadius -> 1
            d2 < d1 && d2 <= grabRadius -> 2
            else -> -1
        }
    }
}
