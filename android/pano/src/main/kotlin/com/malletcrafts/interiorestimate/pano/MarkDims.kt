package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Msg
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

/**
 * A MARK'S DIMENSIONS ON THE PHOTO (port 63 slice M1, 16 Nov 2026; DESIGN-port63 §1.1, §1.2 and §4). Amit, 15 Nov 2026,
 * after his first use of the app on a client site: "Having both measurements foto and measured and both is very
 * confusing while using app. The laundry list of measurements shown below screen is also frustrating. … imagemeter
 * shows all dimensions on the annotation there itself which is easy to use." -- and: "But do not drop our industry
 * standard mark items like door window electrical plate etc."
 *
 * So every dimension a mark asks for is drawn ON the mark, with ONE figure, and this object says which dimensions
 * those are, where each one's line runs on the face, and what its one figure is. There is NO TABLE OF DRAWINGS PER
 * TYPE: [of] turns whatever [SurveyPrep.measures] returns for the mark into lines BY ITS KEY, so no type, sub-kind or
 * later addition can be left out and no key is invented or renamed -- the reading keys, S5's key grammar and a
 * point's c-keys are exactly what they were.
 *
 * Pure: nothing here draws, nothing is stored, and nothing is on screen yet (slice M2 draws from it). Millimetres on
 * the face, as everywhere in survey prep: x from its left edge, y UP from its bottom edge.
 */
object MarkDims {

    /** What a dimension IS on the mark, which decides how it is drawn ([DimLayout]). */
    enum class Role {
        /** `w`: across the mark, on its top edge. */
        WIDTH,
        /** `h`: up the mark, on its right edge. */
        HEIGHT,
        /** `l` / `r`, a point's `cl` / `cr`: from a corner of the wall (a plan: a side wall) to the mark. */
        OFF_H,
        /** `s` / `t`, a plan's `k` / `f`, a point's `cs` `ct` `ck` `cf`: from the floor or the ceiling (a plan: the back
         *  or the front wall) to the mark. */
        OFF_V,
        /** `d`: a Beam's drop, from its underside up to the ceiling. */
        DROP,
        /** `p`: what the photo cannot show -- reveal depth, projection, a drop below the slab. No line: a chip at the
         *  mark's lower-right corner. */
        DEPTH,
    }

    /**
     * ONE DIMENSION OF A MARK: its [key] (`w`) and reading key [code] (`FD1.w`), its [role], its name ([what], the
     * checklist's own [Msg]), the photo's estimate ([est]; null: site only) and its line on the face, mm, from
     * ([x1], [y1]) to ([x2], [y2]). A [Role.DEPTH] has no line ([line] false): both ends are its anchor, the mark's
     * lower-right corner. [known] false: a key this build has no drawing for, shown as a chip rather than dropped --
     * MarkDimsTest proves no key of today's types is one.
     */
    class Dim(val key: String, val code: String, val role: Role, val what: Msg, val est: Double?,
              val x1: Double, val y1: Double, val x2: Double, val y2: Double,
              val line: Boolean = true, val known: Boolean = true) {
        /** The line's length on the face, mm: for every key the photo can estimate, its [est]. */
        val length: Double get() = hypot(x2 - x1, y2 - y1)
    }

    /**
     * THE DIMENSIONS OF THE MARK [e] ON SURFACE [s] of [a] x [b] mm: one [Dim] per [SurveyPrep.measures], in the same
     * order. By key:
     *  - `w` along the top edge, corner to corner; `h` up the right edge;
     *  - `l` from the left corner (a plan: the left wall) to the mark's left edge, `r` from its right edge to the right
     *    corner, both at mid-height;
     *  - `s` / `k` from the floor (a plan: the back wall) up to its bottom edge, `t` / `f` from its top edge up to the
     *    ceiling (the front wall), both at mid-width;
     *  - `d` from its underside up to the ceiling;
     *  - `p` no line: anchored at its lower-right corner.
     * A POINT'S offsets run to its CENTRE, under its own keys (`cl` `cr` `cs` `ct` `cf` `ck`; a plan's Electric point
     * keeps `l` `r` `f` `k`: [SurveyPrep.ckey]) -- which key it is called decides nothing here, only that the mark is a
     * point. A Note asks nothing and has none.
     */
    fun of(s: String, e: SurveyPrep.Mark, a: Double, b: Double): List<Dim> {
        val pt = SurveyPrep.isPoint(e.t)
        val xm = (e.x0 + e.x1) / 2; val ym = (e.y0 + e.y1) / 2
        return SurveyPrep.measures(s, e, a, b).map { m ->
            val code = e.code + "." + m.key
            val k = if (pt && m.key.length == 2 && m.key[0] == 'c') m.key.substring(1) else m.key
            when (k) {
                "w" -> Dim(m.key, code, Role.WIDTH, m.what, m.est, e.x0, e.y1, e.x1, e.y1)
                "h" -> Dim(m.key, code, Role.HEIGHT, m.what, m.est, e.x1, e.y0, e.x1, e.y1)
                "l" -> Dim(m.key, code, Role.OFF_H, m.what, m.est, 0.0, ym, if (pt) xm else e.x0, ym)
                "r" -> Dim(m.key, code, Role.OFF_H, m.what, m.est, if (pt) xm else e.x1, ym, a, ym)
                "s", "k" -> Dim(m.key, code, Role.OFF_V, m.what, m.est, xm, 0.0, xm, if (pt) ym else e.y0)
                "t", "f" -> Dim(m.key, code, Role.OFF_V, m.what, m.est, xm, if (pt) ym else e.y1, xm, b)
                "d" -> Dim(m.key, code, Role.DROP, m.what, m.est, xm, e.y0, xm, b)
                "p" -> Dim(m.key, code, Role.DEPTH, m.what, m.est, e.x1, e.y0, e.x1, e.y0, line = false)
                else -> Dim(m.key, code, Role.DEPTH, m.what, m.est, e.x1, e.y0, e.x1, e.y0, line = false, known = false)
            }
        }
    }

    /**
     * ONLY A MARK THAT IS IN THE ROOM TAKES A READING ([SheetMarks.surveyed]): a mark born a Client wish or Vendor
     * added is not there to laser, and a Note is only words. Such a mark is never active, and it is not in the walk or
     * the counts ([MeasureWalk]); a reading it already holds from an earlier build is still shown, and never removed.
     */
    fun asked(e: SurveyPrep.Mark): Boolean = SheetMarks.surveyed(e)

    // ------------------------------------------------------------------ the one figure

    /** What a dimension's one figure is. */
    enum class State {
        /** A reading, by laser or typed: `1198`. */
        MEASURED,
        /** No reading, and the photo can estimate it: `≈1203`. */
        ESTIMATE,
        /** No reading, and the photo cannot show it (a `p`): `?`. */
        UNKNOWN,
    }

    /** A dimension's one figure: its [text] ([SurveyPlan.figure]'s, the rule the Plan tab draws by), its [state], and
     *  whether it carries the check mark ([check]). */
    class Value(val text: String, val state: State, val check: Boolean)

    /**
     * THE ONE FIGURE of the reading key [code] of the mark [e] on [s], whose photo estimate is [est]: the reading when
     * there is one, else `≈` and the estimate, else `?` -- from [SurveyPlan.figure], so the photo, the Plan tab and
     * (later) the saved pictures cannot disagree. Never the photo's figure BESIDE the reading.
     */
    fun value(actual: Map<String, Double>, s: String, e: SurveyPrep.Mark, code: String, est: Double?, a: Double, b: Double): Value {
        val read = actual[code]
        val state = if (read != null) State.MEASURED else if (est == null) State.UNKNOWN else State.ESTIMATE
        return Value(SurveyPlan.figure(actual, code, est).t, state, check(read, est, inPlane(s, e, a, b)))
    }

    // ------------------------------------------------------------------ the one check that stays

    /** A reading is checked against the photo only when it is this far from the estimate, mm ... */
    const val CHECK_MM = 150.0
    /** ... or this part of the estimate, whichever is LARGER. Deliberately wide: it should fire on a shot at the wrong
     *  target, not on an ordinary estimate. Claude's starting figures, to be tuned from Amit's first rooms. */
    const val CHECK_PART = 0.15

    /** The types whose `p` is a depth INTO the wall -- a wall's Door, Window, Opening and Niche: the thing still lies
     *  in the wall's plane, so its box on the squared photo is its size. */
    val INTO_WALL: Set<String> = setOf("D", "W", "O", "N")

    /**
     * The types whose `p` is a projection, a depth FROM the wall, a drop or a height, by kind of surface: the thing
     * STANDS OFF the squared plane. MarkDimsTest holds this list and [INTO_WALL] together to be exactly the types that
     * ask a `p`, so a type added later with a `p` is a red test until it is put in one of the two.
     */
    val STAND_OFF: Map<String, Set<String>> = mapOf(
        MarkTypes.WALL to setOf("C", "B", "L", "GRL", "AC", "CTR", "CRP", "SAN"),
        MarkTypes.FLOOR to setOf("S", "CRP", "SAN"),
        MarkTypes.CEIL to setOf("B", "AC", "CDP"))

    /**
     * WHETHER THE MARK LIES IN THE SQUARED PLANE. A face is squared to the wall's (the floor's, the ceiling's) own
     * plane, so a thing that stands off that plane is nearer the camera and its box on the squared photo is LARGER
     * than the thing: a 600 mm deep wardrobe on the wall of a 2.7 m room, seen from the middle, reads about 1.8 times
     * its width. In plane: a mark that asks no `p`, and a wall's `D` `W` `O` `N`, whose `p` goes into the wall.
     * Anything else that asks a `p` stands off it.
     */
    fun inPlane(s: String, e: SurveyPrep.Mark, a: Double, b: Double): Boolean {
        if (SurveyPrep.measures(s, e, a, b).none { it.key == "p" }) return true
        return !SurveyPrep.isPlan(s) && e.t in INTO_WALL
    }

    /**
     * THE CHECK: a reading that differs from the photo by more than the larger of [CHECK_MM] and [CHECK_PART] of the
     * estimate -- on a mark that lies in the squared plane ([inPlane]) ONLY. On a mark that stands off the plane it is
     * always false, for every one of its keys, its position figures included: a check there would flag the TRUE
     * reading of every wardrobe, loft and beam, and a warning that fires every time teaches the reader to skip it.
     * Computed from the reading and the box as it is now, so it also catches a mark moved after it was lasered;
     * nothing is stored.
     */
    fun check(read: Double?, est: Double?, inPlane: Boolean): Boolean {
        if (!inPlane || read == null || est == null) return false
        return abs(read - est) > max(CHECK_MM, CHECK_PART * est)
    }

    /**
     * EVERYTHING THE MARK ASKS IS MEASURED: the `✓` after its code. False for a mark that asks nothing (a Note) and for
     * one that is not in the room ([asked]), which is never asked for a reading.
     */
    fun done(prep: SurveyPrep.Prep, s: String, e: SurveyPrep.Mark, a: Double, b: Double): Boolean {
        if (!asked(e)) return false
        val keys = prep.keysOf(s, e, a, b)
        return keys.isNotEmpty() && keys.all { prep.actual[it] != null }
    }

    // ------------------------------------------------------------------ how a figure is drawn

    /** The glyph before the figure being measured now, the chip's before a `p`, and the check's after a reading. */
    const val ACTIVE_GLYPH = "▶" // text:value the active-reading glyph, the same in every language
    const val DEPTH_GLYPH = "⊥ " // text:value the site-only chip's glyph, the same in every language
    const val CHECK_GLYPH = "!" // text:value the check mark after a reading, the same in every language
    /** The tick after a mark's code once everything it asks is measured ([done]). */
    const val DONE_GLYPH = "✓" // text:value the measured tick, the same in every language

    /** The figure as its plate prints it: `▶` before the one being measured, `⊥ ` before a `p`'s. The `!` is drawn
     *  after it, in its own colour ([CHECK_COLOUR]), when [Value.check]. */
    fun shown(v: Value, role: Role, active: Boolean): String =
        (if (active) ACTIVE_GLYPH else "") + (if (role == Role.DEPTH) DEPTH_GLYPH else "") + v.text

    /** A dimension line: white at alpha 0xcc. */
    const val LINE_COLOUR = 0xffffff
    const val LINE_ALPHA = 0xcc
    /** The `!` after a reading that fails the check, and a `?` nobody has measured: amber. */
    const val CHECK_COLOUR = 0xffc46b

    /**
     * How a plate is painted: its text's colour [ink] and whether it is [bold]; its [fill] at [fillAlpha]; its
     * [outline] colour (null: none) and whether that outline is [dashed].
     */
    class Style(val ink: Int, val bold: Boolean, val fill: Int, val fillAlpha: Int, val outline: Int?, val dashed: Boolean)

    /**
     * A PLATE'S STYLE, by what its figure is (DESIGN-port63 §1.1): a reading white and bold on solid black; an
     * estimate grey on half-black inside a dashed grey outline; a `?` amber inside a dashed amber outline; and the one
     * being measured now ([active]) on the gold plate with the gold ring, its text in its state's own colour.
     */
    fun style(state: State, active: Boolean): Style {
        val ink = when (state) { State.MEASURED -> 0xffffff; State.ESTIMATE -> 0xcfd3cb; State.UNKNOWN -> CHECK_COLOUR }
        if (active) return Style(ink, state == State.MEASURED, 0x4a3d0c, 0xff, 0xe0b040, false)
        return when (state) {
            State.MEASURED -> Style(ink, true, 0x000000, 0xdd, null, false)
            State.ESTIMATE -> Style(ink, false, 0x000000, 0x88, ink, true)
            State.UNKNOWN -> Style(ink, false, 0x000000, 0x88, ink, true)
        }
    }
}
