package com.malletcrafts.interiorestimate

import androidx.compose.runtime.staticCompositionLocalOf
import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * The language every screen draws in (design-marathi §2.4, slice 1b).
 *
 * Provided ONCE, by `AppScreen` in MainActivity.kt, above every screen the app can show, so a
 * composable reads `val tx = LocalTx.current` and a plain function takes a `tx: Tx` parameter
 * (always named `tx`, so I18nLiteralTest (f) needs one pattern). STATIC on purpose: a change
 * recomposes the whole tree at once and every `remember` survives -- the drawer stays open, which
 * is the prototype's "the whole app switches at once" (v57).
 *
 * No default: a composable outside the provider is a bug to find, not an English to fall back on.
 * The choice itself is `AppPrefs.lang`, read by the provider only under `LangRow.VISIBLE` (Amit,
 * 27 Oct 2026, decision 2), so until the last slice every screen is `Tx.EN`.
 */
val LocalTx = staticCompositionLocalOf<Tx> { error("LocalTx not provided") } // text:value developer error, never shown
