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

    /**
     * A note pinned to a spot on the face. It carries typed text, a voice
     * clip, or both — a person holding a laser in one hand often cannot
     * type, which is the whole reason audio exists.
     *
     * Two audio fields, deliberately: [audio] is the clip's file name on
     * THIS phone and [audioUrl] is where it lives on the server once it has
     * been uploaded. Keeping both means a note recorded with no signal is
     * still playable on the spot, and still uploads later; and a note
     * arriving from another device plays from the server without pretending
     * a local file exists.
     */
    data class Pin(
        val x: Double, val y: Double, val text: String,
        val audio: String = "",
        val audioUrl: String = "",
    ) {
        val hasAudio: Boolean get() = audio.isNotBlank() || audioUrl.isNotBlank()

        /** Uploaded already, or still owed to the server? */
        val audioPending: Boolean get() = audio.isNotBlank() && audioUrl.isBlank()
    }

    /** What a tagged rectangle can BE. Deliberately small and closed: the
     *  tag is what the model reads, so it has to stay machine-readable —
     *  anything that doesn't fit goes in the free-text note instead. */
    enum class Kind(val tag: String, val label: String) {
        WINDOW("window", "Window"),
        DOOR("door", "Door"),
        COLUMN("column", "Column"),
        BEAM("beam", "Beam"),
        OPENING("opening", "Opening");

        companion object {
            fun of(tag: String?): Kind =
                entries.firstOrNull { it.tag == tag } ?: OPENING
        }
    }

    /**
     * A tagged opening: FOUR corners, clockwise from top-left, plus what it
     * is. Four and not two, because a window photographed from anywhere but
     * dead centre is a quadrilateral on the face, never an axis-aligned box.
     * Keeping the marked corners keeps the perspective the projection
     * actually produced — which is what the ray maths wants, since each
     * corner becomes a ray meeting the wall plane at the opening's true
     * rectangle. Forcing a box would bake in an error.
     */
    data class Quad(
        val x1: Double, val y1: Double,
        val x2: Double, val y2: Double,
        val x3: Double, val y3: Double,
        val x4: Double, val y4: Double,
        val kind: String = Kind.OPENING.tag,
        val note: String = "",
    ) {
        fun corner(i: Int): Pair<Double, Double> = when (i) {
            1 -> x1 to y1
            2 -> x2 to y2
            3 -> x3 to y3
            else -> x4 to y4
        }

        fun withCorner(i: Int, x: Double, y: Double): Quad = when (i) {
            1 -> copy(x1 = x, y1 = y)
            2 -> copy(x2 = x, y2 = y)
            3 -> copy(x3 = x, y3 = y)
            else -> copy(x4 = x, y4 = y)
        }
    }

    data class FaceAnnotations(
        val lines: List<Line> = emptyList(),
        val pins: List<Pin> = emptyList(),
        val quads: List<Quad> = emptyList(),
    ) {
        val isEmpty: Boolean
            get() = lines.isEmpty() && pins.isEmpty() && quads.isEmpty()
    }

    /** A ready-made rectangle over the middle of the view, to be dragged
     *  onto the real corners — drop-then-adjust, so nobody has to trace an
     *  opening freehand with a thumb. */
    fun newQuad(
        kind: Kind,
        cx: Double = 0.5, cy: Double = 0.5,
        halfW: Double = 0.15, halfH: Double = 0.2,
    ) = Quad(
        cx - halfW, cy - halfH,
        cx + halfW, cy - halfH,
        cx + halfW, cy + halfH,
        cx - halfW, cy + halfH,
        kind.tag)

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
