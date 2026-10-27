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
 * THE भाषा · Language ROW STAYS HIDDEN until every screen is translated (Amit, 27 Oct 2026,
 * ~06:05 IST, decision 2: he chose the alternative). Slices 1b … 8 ship keyed English that looks
 * the same as today; the LAST slice flips this one constant, and nothing else may show the row or
 * pick मराठी -- one switch, never a second code path. I18nLiteralTest pins it false while
 * android/i18n/unkeyed.tsv has a row.
 */
object LangRow {
    const val VISIBLE: Boolean = false
}
