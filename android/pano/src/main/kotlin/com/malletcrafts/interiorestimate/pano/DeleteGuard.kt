package com.malletcrafts.interiorestimate.pano

/**
 * DELETE CANNOT BE PRESSED BY ACCIDENT (port 63 slice F1, 16 Nov 2026). Amit, the same evening, with build 0.3.337 --
 * the first with port 63's measuring screen -- on his phone: "also navigation button and D2 aer very close where d2
 * get accidentally fired. same for delete button for any item."
 *
 * He asked for DISTANCE, so there is no confirm and no second tap: every way an item of survey prep -- a mark, a
 * Tape, a Line, an area, a chain -- is deleted by one tap stands [CLEAR_DP] clear of whatever else takes a press, at
 * the END of what it is part of, and is drawn apart. A delete is one ↶ step and the screen says what went ("FD1
 * deleted", with UNDO), so a slip is seen and taken back at once. The places, and what holds each:
 *  - NO ROW UNDER THE PHOTO DELETES (port 63 slice I1b; DESIGN-imagemeter §14 I1): the picked item's row lost its
 *    Delete, and with it the dead space before and under it that F1 gave it -- the measure bar's D2 that stood under
 *    it is gone too;
 *  - THE LONG-PRESS MENU: its Delete row is the last ([last]; it was the first, right under the finger that opened
 *    the menu), [CLEAR_DP] of dead menu above it, outlined the same;
 *  - THE FLOATING TRASH beside the HELD item (since slice I1b: only while it is held, [Pick.trash]), on the photo and
 *    on the 360: its disc keeps [CLEAR_DP] off the item's
 *    own handles -- a mark's eight tabs, a Tape's / Line's / area's ends and body, a chain's points and line -- and
 *    off every figure ([SurveyPrepTouch.trashSpot]); where no place is that clear there is NO disc, never one on a
 *    handle.
 * The Mark sheet deletes nothing by one tap (a Drop is its why and Done), and ⋮'s clears and Start over take two taps:
 * none of them changed. The panel that slides up (slice I2) will hold the one Delete, last, through [last].
 *
 * Pure numbers and one ordering rule, so they are tested here; :app only draws from them.
 */
object DeleteGuard {

    /** How far a delete control stands from the nearest thing that takes a press, dp. */
    const val CLEAR_DP = 16

    /** The delete control's outline, 0xRRGGBB: a quiet red, and never [NEIGHBOUR] -- so it is not one more gold button. */
    const val OUTLINE = 0xC9504C

    /** Its word: the long-press menu's own delete red (v42.css .ctxm: #FF8A8A). */
    const val INK = 0xFF8A8A

    /** The gold the buttons beside it are outlined in (the nudges; SurveyPrepScreen's SP_GOLD). */
    const val NEIGHBOUR = 0xE0B040

    /** [rows] with its delete row(s) LAST, every other row in the order it had. */
    fun <T> last(rows: List<T>, isDelete: (T) -> Boolean): List<T> = rows.filterNot(isDelete) + rows.filter(isDelete)

    // ------------------------------------------------------------------ the long-press menu, and the finger that opened it

    /** A row of the long-press menu, dp tall (v42.css .ctxm: 40); the menu's own padding above its first row and under
     *  its last, dp; how far above the press its top is put, and how far inside the screen it is kept, dp (sp.js 1471). */
    const val MENU_ROW_DP = 40
    const val MENU_FOOT_DP = 4
    const val MENU_ABOVE_PRESS_DP = 12
    const val MENU_EDGE_DP = 4

    /**
     * WHERE THE LONG-PRESS MENU'S TOP GOES, px down the screen: [pressY] where the finger is, [menuH] the menu's
     * height, [screenH] the screen's, [dp] px per dp. As before, [MENU_ABOVE_PRESS_DP] above the press -- the finger
     * is then on the menu's head, its first row below it -- and kept [MENU_EDGE_DP] inside the screen.
     *
     * BUT NEVER WITH THE DELETE ROW UNDER THE FINGER ([hasDelete]: the menu has one, its LAST row). Low on the screen
     * the menu does not fit under the press and is pushed up, and what was pushed under the finger is its foot --
     * since slice F1 the delete row: a second tap where the finger already is would delete. (In landscape that is
     * every mark in the lowest 46 dp of the photo.) Where the finger would be within [CLEAR_DP] of that row, the menu
     * opens ABOVE the press instead, its delete row [CLEAR_DP] over the finger -- as far up as the screen allows.
     */
    fun menuTop(pressY: Double, menuH: Double, screenH: Double, dp: Double, hasDelete: Boolean): Double {
        val edge = MENU_EDGE_DP * dp
        val top = minOf(pressY - MENU_ABOVE_PRESS_DP * dp, screenH - menuH - edge).coerceAtLeast(edge)
        if (!hasDelete) return top
        val clear = CLEAR_DP * dp
        val rowBottom = top + menuH - MENU_FOOT_DP * dp; val rowTop = rowBottom - MENU_ROW_DP * dp
        if (pressY < rowTop - clear || pressY > rowBottom + clear) return top
        return (pressY - clear - (menuH - MENU_FOOT_DP * dp)).coerceAtLeast(edge)
    }

    /** How far the finger at [pressY] is from the delete row of a menu whose top is at [top], px: 0 on the row. */
    fun menuClear(pressY: Double, top: Double, menuH: Double, dp: Double): Double {
        val rowBottom = top + menuH - MENU_FOOT_DP * dp; val rowTop = rowBottom - MENU_ROW_DP * dp
        return if (pressY < rowTop) rowTop - pressY else if (pressY > rowBottom) pressY - rowBottom else 0.0
    }
}
