package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * THE MEASURE BAR'S TEXTS (port 63 slice M1, 16 Nov 2026; DESIGN-port63 §1.4, §1.5 and §3), every one through [Tx]. The
 * bar is `label · ‹ · two lines · the figure box · D2 · ›`, 328 dp wide at most in portrait and 306 in landscape, so
 * its two lines have about 80 dp (58 in landscape) beside the label cell -- which is why each text here is what it is:
 *  - LINE 1 is the measure's own words ALONE ([label]): "from left corner", "sill, from floor", "reveal depth"; a Line
 *    "length", an area "perimeter", a chain "segment 2" or "total". Nothing is composed: the code and the type are
 *    beside it, in the label cell ([cell]);
 *  - LINE 2 is progress ([progress]): "7 of 18 measured";
 *  - the check's sentence ([photoSays]) and the not-in-the-room sentence ([notInRoom]) are NOT in the bar: the screen's
 *    message line says them.
 *
 * ENGLISH FIRST (Amit, 16 Nov 2026: "Let amit verify it end to end in English then only start Marathi
 * transformation"): the six `sp.measure.*` keys read here are new, and their Marathi is their English for now, listed
 * in android/i18n/mr-pending.tsv. The measure names, the type short names and `sp.locked` are existing keys, with the
 * Marathi they had.
 */
object MeasureText {

    /**
     * LINE 1: what the reading [code] measures, in the checklist's own words -- the `what` of
     * [SurveyPrep.Prep.readingOf], a mark's, a Line's, an area's or a chain's. Null for a key nothing asks for.
     */
    fun label(tx: Tx, prep: SurveyPrep.Prep, code: String, ab: (String) -> DoubleArray): String? =
        prep.readingOf(code, ab)?.second?.what?.invoke(tx)

    /**
     * THE LABEL CELL, 52 dp wide: the picked item's [code] and, for a mark, the line [under] it. It opens the Mark
     * sheet, as the picked mark's own row does in 2 · Mark.
     */
    class Cell(val code: String, val under: String?) {
        /** The cell as one text: the code, and what is under it on a second line. */
        val text: String get() = if (under == null) code else code + "\n" + under
    }

    /**
     * The label cell of the picked item [id]: a MARK's code over its type's SHORT name ([SurveyPrepPalette.short]:
     * Window, Switch, Unit, Sanitary -- the full name does not fit 52 dp), or over "locked" when it is locked; a
     * LINE's or an AREA's code alone. Null for a tape and a chain, which open no sheet ([MarkSheet.opens]), and for an
     * id nothing has.
     */
    fun cell(tx: Tx, prep: SurveyPrep.Prep, id: String): Cell? {
        prep.markById(id)?.let { (s, e) ->
            return Cell(e.code, if (SurveyPrep.isLocked(e)) tx.sp_locked else SurveyPrepPalette.short(tx, SurveyPrep.typeOf(s, e.t).name))
        }
        if (!MarkSheet.opens(prep, id)) return null
        return prep.measById(id)?.let { (_, m) -> Cell(m.code, null) }
    }

    /** LINE 2: "7 of 18 measured" -- the open face's progress (in 2 · Mark and 4 · 360, the picked item's face's). */
    fun progress(tx: Tx, c: MeasureWalk.Count): String =
        tx.sp_measure_x_of_x(read = c.read.toString(), total = c.total.toString())

    /** A finished face: "Front wall · all 9 measured ✓". */
    fun faceDone(tx: Tx, s: String, total: Int): String =
        tx.sp_measure_face_done_x(face = face(tx, s), total = total.toString())

    /** The button a finished face offers: "Next: Right wall · 4 to measure ▸". */
    fun nextFace(tx: Tx, f: MeasureWalk.Face): String =
        tx.sp_measure_next_face_x(face = face(tx, f.s), n = f.left.toString())

    /** No face has anything left: "Room measured · all 31 ✓". */
    fun roomDone(tx: Tx, total: Int): String = tx.sp_measure_room_done_x(total.toString())

    /**
     * What the message line says when a reading that fails the check ([MarkDims.check]) lands, and whenever a plate
     * carrying `!` becomes active: "The photo says ≈640 mm. Check the mark, or shoot again." -- the one place the
     * phone still shows the photo's estimate once a dimension has a reading. [est] is rounded as the figure is.
     */
    fun photoSays(tx: Tx, est: Double): String = tx.sp_measure_photo_says_x(Math.round(est).toString())

    /**
     * What the message line says while a four-corner mark's three straight sides and its sloping side do not close
     * ([Quad.closing]; port 63 slice Q1, DESIGN-quad §2.4): "The other sides make the bottom side ≈2466 mm. Check the
     * readings, or the corners." [side] is the sloping side's own words ([Quad.sideWhat]: "bottom side", on a plan "back
     * side"), already in the language; [est] what the other three readings make it, rounded as a figure is. It names
     * the side because it is said when ANY of the four side readings lands ([MeasureFlow.checkText]).
     */
    fun sidesSay(tx: Tx, side: String, est: Double): String = tx.sp_measure_sides_say_x(side = side, est = Math.round(est).toString())

    /** What a tap on a figure of a mark that is not in the room says (a Client wish and a Vendor-added mark alike). */
    fun notInRoom(tx: Tx): String = tx.sp_measure_not_in_room

    /** A face's name in the language: "Front wall" (surface.*, through Masters). */
    private fun face(tx: Tx, s: String): String = SurveyPrep.NAME[s]?.let { Masters.surface(tx, it) } ?: s
}
