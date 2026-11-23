package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * WHAT THE MESSAGE LINE SAYS WHILE MEASURING ON 2 · MARK (port 63 slices M1 and I1; DESIGN-imagemeter §4, §18), every
 * one through [Tx]. The measure bar these texts were first written for went in slice I1b -- nothing under the photo
 * shows a measurement -- and its label, progress, next-face and photo-estimate lines went with it. What is left is said
 * on the screen's message line: a wall finished ([faceDone]), the room finished ([roomDone]), a reading that does not
 * match the mark ([checkReading], with no figure), a reading on the 360 ([on360]) and a figure of a mark that is not in
 * the room ([notInRoom]).
 *
 * ENGLISH FIRST (Amit, 16 Nov 2026: "Let amit verify it end to end in English then only start Marathi
 * transformation"): the `sp.measure.*` keys read here are new, and their Marathi is their English for now, listed in
 * android/i18n/mr-pending.tsv.
 */
object MeasureText {

    /** A finished face: "Front wall · all 9 measured ✓". */
    fun faceDone(tx: Tx, s: String, total: Int): String =
        tx.sp_measure_face_done_x(face = face(tx, s), total = total.toString())

    /** No face has anything left: "Room measured · all 31 ✓". */
    fun roomDone(tx: Tx, total: Int): String = tx.sp_measure_room_done_x(total.toString())

    /**
     * THE CHECK'S SENTENCE WITH NO FIGURE (port 63 slice I1a; DESIGN-imagemeter §18 rule 5; Amit, 16 Nov 2026: "No foto
     * estimation at all. It confuses user"): "This reading does not match the mark. Shoot again, or ▸ to keep it." --
     * what the message line says when a reading that fails the gross guard ([MarkDims.guard]) lands, and when its
     * plate turns gold. Since slice I1b it is the only check sentence: the old one quoted the photo's estimate.
     * English first: its Marathi is pending (android/i18n/mr-pending.tsv).
     */
    fun checkReading(tx: Tx): String = tx.sp_measure_check_reading

    /** What a laser reading that arrives on 3 · 360 says (DESIGN-imagemeter §11): "D2 1203 mm — not used: readings are
     *  taken on the wall's own tab". Nothing is measured on the 360; the reading is thrown away. */
    fun on360(tx: Tx, mm: Int): String = tx.sp_laser_on_360_x(mm.toString())

    /** What a tap on a figure of a mark that is not in the room says (a Client wish and a Vendor-added mark alike). */
    fun notInRoom(tx: Tx): String = tx.sp_measure_not_in_room

    /** A face's name in the language: "Front wall" (surface.*, through Masters). */
    private fun face(tx: Tx, s: String): String = SurveyPrep.NAME[s]?.let { Masters.surface(tx, it) } ?: s
}
