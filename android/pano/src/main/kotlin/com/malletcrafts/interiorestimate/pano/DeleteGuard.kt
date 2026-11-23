package com.malletcrafts.interiorestimate.pano

/**
 * DELETE CANNOT BE PRESSED BY ACCIDENT (port 63 slice F1, 16 Nov 2026). Amit, the same evening, with build 0.3.337 --
 * the first with port 63's measuring screen -- on his phone: "also navigation button and D2 aer very close where d2
 * get accidentally fired. same for delete button for any item."
 *
 * He asked for DISTANCE, so there is no confirm and no second tap: the one way an item of survey prep -- a mark, a
 * Tape, a Line, an area, a chain -- is deleted by one tap stands [CLEAR_DP] clear of whatever else takes a press, at
 * the END of what it is part of, and is drawn apart. A delete is one ↶ step and the screen says what went ("FD1
 * deleted", with UNDO), so a slip is seen and taken back at once.
 *
 * DELETE HAS ONE PLACE (port 63 slice I2b, 23 Nov 2026; DESIGN-imagemeter §6): THE PANEL THAT SLIDES UP, its LAST act
 * ([last], through [MarkSheet.acts]: line 2's last cell, [MarkSheet.DELETE_GAP_DP] from the act beside it and a whole
 * act row below Done), behind a deliberate tap on the picked item's code chip or a long press, in this look
 * ([OUTLINE], [INK]). Everything else that deleted by one tap is GONE, not hidden:
 *  - no row under the photo deletes (slice I1b took the picked item's row's Delete; slice I2b the row itself);
 *  - THE LONG-PRESS MENU, whose delete row F1 had moved to its foot, is gone: a long press opens the panel;
 *  - THE FLOATING TRASH beside the held item, on the photo and on the 360, is gone.
 * The panel deletes nothing else by one tap (a Drop is its why and Done), and ⋮'s clears and Start over take two taps:
 * none of them changed.
 *
 * Pure numbers and one ordering rule, so they are tested here; :app only draws from them.
 */
object DeleteGuard {

    /** How far a delete control stands from the nearest thing that takes a press, dp. */
    const val CLEAR_DP = 16

    /** The delete control's outline, 0xRRGGBB: a quiet red, and never [NEIGHBOUR] -- so it is not one more gold button. */
    const val OUTLINE = 0xC9504C

    /** Its word: the delete red the long-press menu had (v42.css .ctxm: #FF8A8A). */
    const val INK = 0xFF8A8A

    /** The gold of what stands by it (Done in the panel's head; SurveyPrepScreen's SP_GOLD). */
    const val NEIGHBOUR = 0xE0B040

    /** [rows] with its delete row(s) LAST, every other row in the order it had. */
    fun <T> last(rows: List<T>, isDelete: (T) -> Boolean): List<T> = rows.filterNot(isDelete) + rows.filter(isDelete)
}
