package com.malletcrafts.interiorestimate.pano

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * THE TURN A FIGURE'S LABEL IS DRAWN AT (port 63 slice I1c, 23 Nov 2026; DESIGN-imagemeter §21, P5). Amit, after using
 * 0.3.345: "no way to set alignment of measurements to line direction. its always horizontal which overlaps on other
 * measurements." So EVERY FIGURE RUNS ALONG ITS DIMENSION, as ImageMeter draws a length: a Line's and a Tape's label is
 * turned to the line's own angle, centred on it and breaking it; a mark's width lies level on its width line and its
 * height turned up its height line; a four-corner mark's side along its slope; a position figure along its own line.
 *
 * The turn is in DEGREES, on the canvas (x right, y DOWN), clockwise -- the sense `Canvas.rotate` takes it in -- so a
 * level label is 0 and a label that reads bottom to top is -90, as the turned height plate has always been drawn.
 *
 * NEVER UPSIDE DOWN: a line has two directions and its label reads along the one that keeps the text the right way
 * up, so the turn is folded into ([UPRIGHT_LOW], [UPRIGHT_HIGH]] -- -95° to +85°. A line within [NEAR_UPRIGHT_DEG] of
 * upright reads BOTTOM TO TOP, as a turned height plate does, whichever way it leans: so a door's height and a
 * nearly-upright Line beside it read the same way, and the fold's seam lies where no label is near upright.
 *
 * Pure and in canvas px; the painter (slice I1b) only rotates by what comes back. Nothing here is on screen yet.
 */
object LabelAngle {

    /** A line this close to upright, degrees, reads bottom to top whichever way it leans. */
    const val NEAR_UPRIGHT_DEG = 5.0
    /** The fold: a turn is always above this ... */
    const val UPRIGHT_LOW = -90.0 - NEAR_UPRIGHT_DEG
    /** ... and at most this. */
    const val UPRIGHT_HIGH = 90.0 - NEAR_UPRIGHT_DEG

    /** A level label, and one that reads bottom to top. */
    const val LEVEL = 0.0
    const val UP = -90.0

    /**
     * THE TURN OF A LABEL ON THE LINE FROM ([x1], [y1]) TO ([x2], [y2]), canvas px: the line's own angle, clockwise from
     * level, folded so the text is never upside down ([UPRIGHT_LOW] < turn <= [UPRIGHT_HIGH]). The same for both
     * directions of the line. A line with no length is level. An exactly level line is exactly [LEVEL] and an exactly
     * upright one exactly [UP], so a mark's width and height plates turn by whole quarters, as they are drawn today.
     */
    fun of(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val dx = x2 - x1; val dy = y2 - y1
        if (dx == 0.0 && dy == 0.0) return LEVEL
        if (dy == 0.0) return LEVEL
        if (dx == 0.0) return UP
        return fold(Math.toDegrees(atan2(dy, dx)))
    }

    /** [of] for a segment. */
    fun of(s: DimLayout.Seg): Double = of(s.x1, s.y1, s.x2, s.y2)

    /** Any turn, degrees, folded into ([UPRIGHT_LOW], [UPRIGHT_HIGH]]: the same line read the other way is half a turn on. */
    fun fold(deg: Double): Double {
        var a = deg % 360.0
        if (a > 180.0) a -= 360.0
        if (a <= -180.0) a += 360.0
        if (a > UPRIGHT_HIGH) a -= 180.0
        if (a <= UPRIGHT_LOW) a += 180.0
        // -0.0 and a turn that is level within rounding are level
        return if (abs(a) < 1e-12) LEVEL else a
    }

    /** Whether text turned by [deg] reads the right way up: its "up" points up the canvas, or along it, never down. */
    fun upright(deg: Double): Boolean = deg > UPRIGHT_LOW && deg <= UPRIGHT_HIGH

    /** The unit vector ALONG a label turned by [deg] (the way its text runs) -- exact for a level and an upright one. */
    fun along(deg: Double): DoubleArray = when (deg) {
        LEVEL -> doubleArrayOf(1.0, 0.0)
        UP -> doubleArrayOf(0.0, -1.0)
        else -> Math.toRadians(deg).let { doubleArrayOf(cos(it), sin(it)) }
    }

    /** The unit vector ACROSS it, from the top of its text to the bottom: [along] turned a quarter clockwise. */
    fun across(deg: Double): DoubleArray = along(deg).let { doubleArrayOf(0.0 - it[1], it[0]) }
}
