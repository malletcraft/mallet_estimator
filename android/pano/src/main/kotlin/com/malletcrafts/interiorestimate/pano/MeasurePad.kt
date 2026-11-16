package com.malletcrafts.interiorestimate.pano

/**
 * THE MEASURE PAD'S TEXT (port 63 slice M1, 16 Nov 2026; DESIGN-port63 §1.4, and the plan critic's finding 2). The pad
 * is the app's own four rows of keys under the measure bar -- not the system keyboard, which covers far more of the
 * screen, jumps at every field and has no Next. This object is what its keys do to the text; the screen (slice M2)
 * only lays the keys out.
 *
 * WHAT IT STARTS WITH, AND WHY THAT IS A RULE: when a dimension becomes active the pad's text is the STORED reading
 * ([MmBox.shownText] of it), or empty -- never the photo's estimate, and never a chain's derived or laser figure, which
 * are not readings (the fault [MmBox]'s own header records: a derived 2404.5 shown as "2405" was once saved as a site
 * reading nobody took). It starts [State.fresh]: nothing has been typed.
 *
 * EVERY WRITE IS [MmBox.commit], UNCHANGED ([commit]). So an untouched pad writes nothing: walking with `‹`, `›` or
 * Next across a measured dimension never changes or removes it; typing the figure it already holds writes nothing; an
 * emptied pad over a stored reading removes the reading; an empty pad over no reading writes nothing.
 *
 * Pure. Digits only, whole mm, [MAX_DIGITS] at most.
 */
object MeasurePad {

    /** A figure is six digits at most: 999999 mm is a kilometre, far past any room. */
    const val MAX_DIGITS = 6

    /** The pad's four rows and four columns (the last two rows share Next; `0` spans the two middle columns). */
    const val ROWS = 4
    const val COLUMNS = 4

    /** The pad's text, and whether a key has been pressed since the dimension became active ([fresh]: none has). */
    data class State(val text: String, val fresh: Boolean)

    /** A key that changes the text. Next and the hide key change none: they commit ([commit]) and move or close. */
    sealed class Key {
        /** One of 0 … 9. */
        class Digit(val d: Int) : Key()
        /** `⌫`. */
        object Back : Key()
        /** Clear. */
        object Clear : Key()
    }

    /** The pad as a dimension becomes active: the [stored] reading as a reading box shows it, or empty; fresh. */
    fun start(stored: Double?): State = State(MmBox.shownText(stored), true)

    /**
     * ONE KEY PRESSED. While [State.fresh] the first digit REPLACES the text and `⌫` empties it; after that a digit
     * appends ([MAX_DIGITS] at most) and `⌫` removes one. Clear empties. A `0` that would LEAD the figure is ignored
     * and leaves the state exactly as it was -- so on a fresh pad it stays fresh, and `0` then Next cannot remove a
     * reading. A digit that is not 0 … 9 is ignored too.
     */
    fun press(state: State, key: Key): State = when (key) {
        is Key.Clear -> State("", false)
        is Key.Back -> if (state.fresh) State("", false) else State(state.text.dropLast(1), false)
        is Key.Digit -> {
            val base = if (state.fresh) "" else state.text
            if (key.d !in 0..9 || (key.d == 0 && base.isEmpty()) || base.length >= MAX_DIGITS) state
            else State(base + key.d, false)
        }
    }

    /**
     * WHAT THE BAR'S BOX AND THE GOLD PLATE SHOW IN PLACE OF THE ONE FIGURE: null while [State.fresh] (both draw the
     * dimension's own figure), else the digits typed so far. Empty once the pad was emptied: the box then shows its
     * `mm` hint, and the plate keeps the figure it had until the commit.
     */
    fun live(state: State): String? = if (state.fresh) null else state.text

    /**
     * THE WRITE, when the dimension is left -- Next, `‹`, `›`, the hide key, a tap elsewhere, a change of step, face or
     * tab, back and Save: [MmBox.commit] of the [stored] reading and the pad's text, unchanged. Null writes nothing; a
     * [MmBox.Commit] with a null value removes the reading. A laser reading that arrives while digits are uncommitted
     * discards them and is the reading: the caller then simply does not call this.
     */
    fun commit(stored: Double?, state: State): MmBox.Commit? = MmBox.commit(stored, state.text)
}
