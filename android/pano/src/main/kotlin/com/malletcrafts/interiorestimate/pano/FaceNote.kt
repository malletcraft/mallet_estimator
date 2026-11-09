package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * FACEWRITER'S REFUSALS AS DATA (port 61c slice M8) — the PlanSave idiom, a third time (M3c critic 2's
 * reason): FaceWriter (:app) writes the gallery files other parties read, so check (h) scans it whole and
 * refuses every Tx token there but the pinned literal `Tx.EN` of the survey-sheet render — while eight of
 * its refusal literals are SHOWN, as the {why} of MainActivity's four keyed heads ("Could not split: …"),
 * read on a banner, not in a file. So the WORDS live here, where a tx can reach them and a :pano test can
 * pin them under both languages; FaceWriter throws the KIND (and the not-360 bounds) as data, and the
 * catchers render it with the live tx through [line].
 *
 * [Refused]'s own message is the PINNED ENGLISH render, so a logcat line — and the note AsIsUpload stores
 * off a failed `fullPano` — reads exactly as the old require/error messages did (the stored-English rule;
 * a log is a diagnostic other parties read, decision 4's register).
 */
object FaceNote {

    /** What refused: opening the pick, its bytes, the 2:1 test, the two decodes, or the gallery's two steps. */
    enum class Kind { OPEN, NOT_IMAGE, NOT_360, DECODE_SELECTED, DECODE, GALLERY, WRITE }

    /** The refusal in [tx]'s language; under `Tx.EN` byte for byte what FaceWriter's require/error said.
     *  [w]/[h] fill only [Kind.NOT_360] (the decoded bounds, px). */
    fun text(tx: Tx, kind: Kind, w: Int = 0, h: Int = 0): String = when (kind) {
        Kind.OPEN -> tx.refuse_fw_could_not_open_photo
        Kind.NOT_IMAGE -> tx.refuse_fw_not_decodable
        Kind.NOT_360 -> tx.refuse_fw_not_360_x_x(w = w.toString(), h = h.toString())
        Kind.DECODE_SELECTED -> tx.refuse_fw_could_not_decode_selected
        Kind.DECODE -> tx.refuse_fw_could_not_decode_photo
        Kind.GALLERY -> tx.refuse_fw_gallery_refused
        Kind.WRITE -> tx.refuse_fw_could_not_write_gallery
    }

    /** A step FaceWriter refused: the kind and the not-360 bounds travel as data; every catcher is a
     *  `runCatching`, so no caller types the old IllegalArgument/IllegalState. */
    class Refused(val kind: Kind, val w: Int = 0, val h: Int = 0) : Exception(text(Tx.EN, kind, w, h))

    /** The `{why}` of a shown note: a [Refused] in [tx]'s language, and ANY OTHER Throwable as
     *  `e.message.orEmpty()` — NOT M3c's `?: simpleName`: the four catchers are MainActivity's shown-note
     *  sites, whose `orEmpty()` idiom the M6 review pinned, and line() must not change their null-message
     *  bytes. */
    fun line(tx: Tx, e: Throwable): String = if (e is Refused) text(tx, e.kind, e.w, e.h) else e.message.orEmpty()
}
