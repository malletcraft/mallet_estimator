package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * THE PLAN SAVE'S REFUSALS AS DATA (port 61c slice M3c). PlanExport (:app) builds the files other parties read, so
 * check (h) scans it whole and refuses every Tx token there — rightly — while the card names its four refusal
 * strings as phone texts to translate (export.msg.*). So the WORDS move here, where a tx can reach them and a
 * :pano test can pin them under both languages (the conflictText lesson, M3b finding 4: a words-home in :app
 * cannot be pinned where the suite runs); PlanExport throws the KIND as data and the screen renders it with the
 * live tx.
 *
 * [Refused]'s own message is the PINNED ENGLISH render, so a logcat line reads exactly as today's error() did —
 * the M3b salvage-log precedent: a log is a diagnostic other parties read (decision 4's register).
 */
object PlanSave {

    /** What refused the save: the private folder, the private copy, Downloads' insert, or the write into it. */
    enum class Kind { FOLDER, KEEP, INSERT, WRITE }

    /** The refusal in [tx]'s language; under `Tx.EN` byte for byte what PlanExport's error() calls said. */
    fun text(tx: Tx, kind: Kind, file: String): String = when (kind) {
        Kind.FOLDER -> tx.export_msg_could_not_make_folder_x(file)
        Kind.KEEP -> tx.export_msg_could_not_keep_x(file)
        Kind.INSERT -> tx.export_msg_downloads_refused_x(file)
        Kind.WRITE -> tx.export_msg_could_not_write_x(file)
    }

    /** A save PlanExport refused: the kind and the file travel as data; the screen renders them with the live tx. */
    class Refused(val kind: Kind, val file: String) : Exception(text(Tx.EN, kind, file))
}
