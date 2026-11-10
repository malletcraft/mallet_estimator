package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.I18n
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * THE TOUR'S CHROME IS THE PAGE'S (port 61c §31, the tour review finding 1). The approved
 * prototype has exactly ONE tour style (the `#t55 …` rules in index61c2.html) — Material
 * light, drawn over survey prep's dark ground too — and the first build shipped an invented
 * dark-gold palette under a comment claiming it was the page's. :app does not compile here,
 * so this reads TourOverlay.kt the way the i18n tests read the sources: the page's values
 * must be present, the invented ones gone. A colour the page does not draw must not come
 * back without a §31 build note and Amit's approval on the prototype.
 */
class TourChromeTest {

    private val src by lazy {
        I18n.android.resolve("app/src/main/kotlin/com/malletcrafts/interiorestimate/TourOverlay.kt").readText()
    }

    @Test fun `a the overlay carries the page's t55 values`() {
        // bubble #F7F2FA, title #1D1B20, head/body #49454F, counter+buttons #6750A4, chip
        // #E8DEF8/#1D192B (look #ECE6F0), scrim rgba(0,0,0,.62), ring rgba(255,255,255,.92),
        // disabled rgba(29,27,32,.38) and the primary's .12
        val wanted = listOf(
            "0xFFF7F2FA", "0xFF1D1B20", "0xFF49454F", "0xFF6750A4", "0xFFE8DEF8",
            "0xFF1D192B", "0xFFECE6F0", "0x9E000000", "0xEBFFFFFF", "0x611D1B20", "0x1F1D1B20")
        val off = wanted.filter { it !in src }
        assertTrue(off.isEmpty(),
            "TourOverlay.kt must carry the page's t55 values (its one approved tour chrome); missing: $off")
    }

    @Test fun `b the invented dark-gold palette stays gone`() {
        // the first build's colours: card 0xF21E2219, edge 4A4F42, muted ABB1A2, gold D9B64A,
        // dim 6E7466, the primary label 14160F, and the 0.70 scrim
        val banned = listOf("1E2219", "D9B64A", "4A4F42", "ABB1A2", "6E7466", "14160F", "0xB3000000")
        val off = banned.filter { it in src }
        assertTrue(off.isEmpty(),
            "TourOverlay.kt carries $off — the first build's invented palette; the tour draws the page's t55 chrome only")
    }
}
