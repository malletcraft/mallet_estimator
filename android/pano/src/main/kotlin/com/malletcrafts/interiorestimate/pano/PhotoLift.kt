package com.malletcrafts.interiorestimate.pano

import kotlin.math.max
import kotlin.math.min

/**
 * HOW FAR THE PHOTO LIFTS UNDER THE PANEL (port 63 slice I2a, 23 Nov 2026; DESIGN-imagemeter §2.3 F, §6, §13).
 * Amit, 16 Nov 2026: "a slide up screen in imagemeter give more options. imagemeter is gold standard when it comes to
 * image annotation." ImageMeter's sheet over a selected element stands over the lower part of the screen, the photo
 * above it dimmed "and the element stays in view" (the study's row 10). Ours opens half way in portrait -- over the
 * photo's lower 322 of 514 dp on the Mi A3 -- so a mark low on the wall, the one he has just long-pressed, would be
 * under it. The photo is MOVED UP, not rescaled, by just enough that the picked item is in view above the panel.
 *
 * PURE, in the window's own px, y down: :app is compiled only by CI, so the rule is here and :app only translates the
 * photo by what [lift] answers. Nothing is lifted in landscape, where the panel opens whole and beside nothing ([lifts]).
 */
object PhotoLift {

    /** The picked item keeps this much photo round it, dp: above its top (its code chip is higher still, and is
     *  handed in as part of the item) and between its foot and the panel. */
    const val MARGIN_DP = 12.0

    /** Whether the photo lifts at all: in portrait. In landscape the panel opens whole across the sheet and "nothing
     *  is lifted: nothing covers the photo but the panel" (§2.4). */
    fun lifts(land: Boolean): Boolean = !land

    /**
     * HOW FAR UP THE PHOTO MOVES, px, never negative. The photo's box on screen runs from [photoTop] to [photoBottom];
     * the panel's top edge is at [panelTop] (it moves as he pulls the panel, and the lift follows it); the picked item
     * -- its box WITH its code chip -- runs from [itemTop] to [itemBottom] where it is drawn before any lift; [margin]
     * is [MARGIN_DP] in px. In this order:
     *  1. the panel covers none of the photo, or the item's foot and [margin] are already above the panel: NOTHING
     *     moves -- a mark high on the wall is left where his eye is;
     *  2. else by what brings the item's foot [margin] above the panel's top;
     *  3. BUT NEVER SO FAR THAT ITS TOP LEAVES: its top stays [margin] under the photo's own top edge. An item taller
     *     than what is left of the photo shows its TOP -- its code chip and its top edge -- and its foot goes under
     *     the panel. One that already starts above that line is not lifted at all;
     *  4. and never by more than the panel covers: the photo's foot does not rise above the panel's top, so no empty
     *     band opens between them.
     * Zero in landscape ([land]).
     */
    fun lift(photoTop: Double, photoBottom: Double, panelTop: Double, itemTop: Double, itemBottom: Double, margin: Double,
             land: Boolean = false): Double {
        if (!lifts(land)) return 0.0
        val covered = photoBottom - panelTop
        val need = itemBottom + margin - panelTop
        if (covered <= 0 || need <= 0) return 0.0
        val headroom = itemTop - margin - photoTop
        return max(0.0, min(need, min(covered, headroom)))
    }

    /** Whether the whole item, with [margin] round it, is in view above the panel once the photo is lifted by [by]. */
    fun inView(photoTop: Double, panelTop: Double, itemTop: Double, itemBottom: Double, margin: Double, by: Double): Boolean =
        itemTop - by >= photoTop + margin - 1e-9 && itemBottom - by <= panelTop - margin + 1e-9

    /** A point of the photo drawn at [y] is ON SCREEN at this y while the photo is lifted by [by] ... */
    fun onScreen(y: Double, by: Double): Double = y - by

    /** ... and a touch at screen [y] is at this y OF THE PHOTO: every touch that reaches the photo while it is lifted
     *  maps through the lift (§16, risk 6), or a tap lands [by] px under the thing tapped. */
    fun onPhoto(y: Double, by: Double): Double = y + by
}
