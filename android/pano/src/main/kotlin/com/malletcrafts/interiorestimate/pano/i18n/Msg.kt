package com.malletcrafts.interiorestimate.pano.i18n

/**
 * A text that is drawn later, in whatever language is on THEN (design-marathi §2.4).
 *
 * The transient messages (`lastResult`, `busy`, `note`, `say()`) hold one of these instead of a
 * String, so a message raised before the language changes redraws in the new language rather than
 * staying frozen in the old one. Hand-written beside the generated Tx; slice 1c is its first user.
 */
typealias Msg = (Tx) -> String

/**
 * THE भाषा · Language ROW IS SHOWN (Amit, 28 Oct 2026, ~13:10 IST: "Show it now, translate
 * alongside", reversing his decision 2 of 27 Oct, which kept it hidden until every screen was
 * translated). Under मराठी a screen not keyed yet stays English until its slice;
 * android/i18n/unkeyed.tsv is the list of those texts and only shrinks. English stays the default
 * on first run. This stays the ONE switch: nothing else may show the row or pick मराठी, so turning
 * Marathi off again is this line alone, never a second code path.
 */
object LangRow {
    const val VISIBLE: Boolean = true
}
