package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * WHICH FIGURES THE PHOTO DRAWS, WITH NO PHOTO ESTIMATE ANYWHERE (port 63 slice I1a, 16 Nov 2026; DESIGN-imagemeter
 * §18, which overrides every `≈` of the sections before it). Amit, shown the pictures of the one-step screen: "No foto
 * estimation at all. It confuses user" -- and, shown them redrawn without estimates and asked whether this is the
 * screen he wants: "Yes".
 *
 * THE RULES, as one place the painter asks (it is compiled only by CI, so what it draws is decided here and tested):
 *  1. THE PHONE NEVER SHOWS A FIGURE NOBODY MEASURED. A dimension has two looks only: READ -- its figure, white bold
 *     on black -- and NOT READ.
 *  2. NOT READ IS SHOWN ONLY WHERE IT CAN BE ACTED ON. A mark that is not picked shows only what has been READ of its
 *     size; nothing at all for an unread side. The PICKED mark shows every dimension it asks -- read ones as figures,
 *     unread ones as a quiet `?`, each a tap target -- and the one being measured is the gold plate, `▶?` until the
 *     reading lands or digits are typed ([ofMark], [Look]). A mark that is not in the room shows no figure at all.
 *  3. A LINE shows its reading once read; until then its code alone, and `?` after it while it is picked. AN AREA'S m²
 *     is computed from the photo, so it is not shown; its perimeter is, once read. A CHAIN shows read segments and a
 *     read total; no derived and no photo figure ([line], [chainSeg], [chainTotal]).
 *  4. The Tape is the one exception: its figure IS what the photo says, and it stays as built ([SurveyMeas.tapeTag]).
 *  5. THE 360's tags follow the same rule -- and nothing can be acted on there, so nothing shows `?` and nothing is
 *     gold ([line360], [chainSeg360], [chainTotal360]).
 *
 * The Plan, the six drawings, the saved pictures and the exports are NOT this object's: they follow in slice M3.
 */
object PhotoFigures {

    /** How one figure looks. */
    enum class Look {
        /** A reading: white and bold on solid black. */
        READ,
        /** Not read, on the picked item: the quiet `?`, grey inside a dashed grey outline. */
        ASK,
        /** The one being measured: solid gold, `▶` before it (`▷` while the laser is not ready). */
        GOLD,
    }

    /**
     * ONE FIGURE OF A MARK AS THE PHOTO DRAWS IT: the dimension [dim] it is, its [look], the [text] its plate prints
     * ([MarkDims.figureText]; the `!` is drawn after it when [check]), whether a reading is STORED under it
     * ([measured]) and whether its text is the keypad's uncommitted digits ([typing]).
     */
    class Figure(val dim: MarkDims.Dim, val look: Look, val text: String, val check: Boolean, val measured: Boolean, val typing: Boolean) {
        val code: String get() = dim.code
        val gold: Boolean get() = look == Look.GOLD
        /** What [MarkDims.plate] and [DimLayout.Fig] call it: a reading, or not read. Never an estimate. */
        val state: MarkDims.State get() = if (measured || typing) MarkDims.State.MEASURED else MarkDims.State.UNKNOWN
        /** How its plate is painted. */
        val style: MarkDims.Style get() = MarkDims.plate(state, gold)
    }

    /**
     * THE FIGURES THE MARK [e] ON [s] SHOWS (rule 2), in its checklist's order ([MarkDims.of]):
     *  - not [picked]: only its READ size figures ([DimLayout.isSize]: `w`, `h`, a four-corner mark's sides and
     *    diagonal) -- an unread side shows nothing, and so does every position figure, read or not;
     *  - [picked]: every dimension it asks -- [Look.READ] with its figure, [Look.ASK] with `?`;
     *  - the dimension whose reading key is [gold]: [Look.GOLD], `▶898` or `▶?` ([ready] false: `▷`); while digits
     *    are typed on the keypad ([live], [MeasurePad.live]; null or empty: none) it reads those digits, in the
     *    reading's own style and with no `!` -- nothing has been checked yet. A gold `⊥` chip carries its own name,
     *    in [tx]'s language, while it is gold: `▶⊥ reveal depth ?` ([MarkDims.figureText]);
     *  - a mark that is NOT IN THE ROOM ([MarkDims.asked] false: born a Client wish or Vendor added, or a Note): NONE,
     *    picked or not (§18 rule 6).
     */
    fun ofMark(tx: Tx, actual: Map<String, Double>, s: String, e: SurveyPrep.Mark, a: Double, b: Double, picked: Boolean, gold: String?,
               live: String? = null, ready: Boolean = true): List<Figure> {
        if (!MarkDims.asked(e)) return emptyList()
        return MarkDims.of(s, e, a, b).mapNotNull { d ->
            val stored = MarkDims.read(actual, s, e, d, a, b)
            val on = d.code == gold
            val measured = stored.state == MarkDims.State.MEASURED
            if (!(picked || on || (measured && DimLayout.isSize(d.role)))) return@mapNotNull null
            val typing = on && !live.isNullOrEmpty()
            val v = if (typing) MarkDims.Value(live!!, MarkDims.State.MEASURED, false) else stored
            Figure(d, if (on) Look.GOLD else if (measured) Look.READ else Look.ASK, MarkDims.figureText(v, d.role, on, ready, d.what(tx)), v.check, measured, typing)
        }
    }

    /**
     * EVERY MARK OF THE FACE [s] WITH THE FIGURES IT SHOWS, in drawing order -- a Note left out, as the painter leaves
     * it (it is its pin). [pick]: what is picked, held and gold ([Pick.State]); the picked mark is the one whose id it
     * names, on whichever face that is.
     */
    fun onFace(tx: Tx, prep: SurveyPrep.Prep, s: String, a: Double, b: Double, pick: Pick.State, live: String? = null,
               ready: Boolean = true): List<Pair<SurveyPrep.Mark, List<Figure>>> =
        (prep.surfaces[s]?.marks ?: emptyList<SurveyPrep.Mark>()).filter { !MarkLife.isNote(it) }
            .map { e -> e to ofMark(tx, prep.actual, s, e, a, b, pick.picked == e.id, pick.gold, live, ready) }

    /**
     * THE FACE AS [DimLayout.placeShown] TAKES IT: every mark of [onFace] with ONLY the figures rule 2 shows, so an
     * unpicked, unmeasured mark costs the layout nothing. The HELD mark ([Pick.State.held]) shows its eight tabs --
     * or, where [free] says its corners are free ([Quad.free], the one rule the painter, the press and this ask), its
     * four corners -- and its `w` and `h` plates are anchored clear of them. [toPx] face mm to canvas px, [u] the
     * drawing's unit, [dp] px a dp, [textW] the painter's own measure of a text.
     */
    fun layout(tx: Tx, prep: SurveyPrep.Prep, s: String, a: Double, b: Double, pick: Pick.State, toPx: (Double, Double) -> DoubleArray,
               u: Double, dp: Double, textW: (text: String, fs: Double, bold: Boolean) -> Double, live: String? = null,
               ready: Boolean = true, free: (SurveyPrep.Mark) -> Boolean = { false }): List<DimLayout.Mark> =
        (prep.surfaces[s]?.marks ?: emptyList<SurveyPrep.Mark>()).filter { !MarkLife.isNote(it) }.map { e ->
            val on = pick.picked == e.id
            DimLayout.markShown(tx, prep.actual, s, e, a, b, on, on && pick.held, pick.gold, toPx, u, dp, textW, live, free(e), ready)
        }

    // ------------------------------------------------------------------ rule 3: a Line, an area, a chain

    /**
     * A TAG THAT IS NOT A PLATE: its [text] as drawn ("" draws nothing), the reading key a tap on it takes ([key];
     * null: it takes no tap -- it shows no figure and no `?`), and whether it is the one being measured ([gold]: its
     * line is drawn gold, as a mark's dimension is). [colour]: a chain segment's own ink (0xRRGGBB); null: the
     * caller's usual.
     */
    class Tag(val text: String, val key: String?, val gold: Boolean, val colour: Int? = null)

    private fun glyph(gold: Boolean, ready: Boolean): String = if (!gold) "" else if (ready) MarkDims.ACTIVE_GLYPH else MarkDims.WAIT_GLYPH

    /**
     * A PLOTTED LINE'S OR AREA'S TAG ON ITS FACE ([SurveyMeas.shownTag]): "F-M1 1240 mm" once read; "F-M1" until then;
     * "F-M1 ?" while it is the picked item; "▶F-M1 ?" while its reading is the gold one. An area never shows its m².
     * A TAPE answers an empty tag here: its own is [SurveyMeas.tapeTag], as built.
     */
    fun line(m: SurveyPrep.Meas, actual: Map<String, Double>, pick: Pick.State, ready: Boolean = true): Tag {
        val key = SurveyMeas.key(m) ?: return Tag("", null, false)
        val read = actual[key]; val ask = pick.picked == m.id; val on = pick.gold == key
        val t = SurveyMeas.shownTag(m.code, m.kind, read, ask || on)
        return Tag(glyph(on, ready) + t, key.takeIf { SurveyMeas.tagTaps(read, ask || on) }, on)
    }

    /** A Line being pulled and an area being tapped show NO figure: their length and their m² are the photo's (rule 1).
     *  What the painter draws for a draft's tag. */
    const val DRAFT_TAG = ""

    /** The same Line or area ON THE 360: its reading once read, else its code alone. Never `?`, never gold. */
    fun line360(m: SurveyPrep.Meas, actual: Map<String, Double>): String =
        SurveyMeas.key(m)?.let { SurveyMeas.shownTag(m.code, m.kind, actual[it], ask = false) } ?: ""

    /**
     * SEGMENT [i] OF THE CHAIN [c] ON ITS FACE ([SurveyChain.segShown]): its reading, white; `?` while the chain is the
     * picked item; the gold glyph before it while it is the one being measured; NULL -- nothing drawn -- for an unread
     * segment of a chain that is not picked. No derived figure and no photo figure. [v] is [SurveyChain.vals].
     */
    fun chainSeg(c: SurveyPrep.Chain, v: SurveyChain.Vals, i: Int, pick: Pick.State, ready: Boolean = true): Tag? {
        val key = c.code + "." + (i + 1)
        val on = pick.gold == key
        val (t, ink) = SurveyChain.segShown(v, i, pick.picked == c.id || on) ?: return null
        return Tag(glyph(on, ready) + t, key, on, ink)
    }

    /**
     * THE CHAIN'S TOTAL UNDER ITS LAST POINT ([SurveyChain.totalShown]): "FK1 Σ 3310" read, "FK1 Σ 3310 laser" where
     * the laser gives it, else its code alone -- "FK1 Σ ?" while the chain is picked. It takes a tap under its key
     * unless the laser gives it ([SurveyChain.totalReadOnly]) or it shows no figure and no `?`.
     */
    fun chainTotal(tx: Tx, c: SurveyPrep.Chain, v: SurveyChain.Vals, pick: Pick.State, ready: Boolean = true): Tag {
        val key = c.code + "." + SurveyChain.TOTAL
        val on = pick.gold == key; val ask = pick.picked == c.id || on
        val taps = !SurveyChain.totalReadOnly(v) && (v.src == SurveyChain.MEASURED || ask)
        return Tag(glyph(on, ready) + SurveyChain.totalShown(tx, c.code, v, ask), key.takeIf { taps }, on, SurveyChain.totalColour(v))
    }

    /** Segment [i] of a chain ON THE 360: its reading, or nothing. */
    fun chainSeg360(v: SurveyChain.Vals, i: Int): Pair<String, Int>? = SurveyChain.segShown(v, i, ask = false)

    /** A chain's total ON THE 360: read or the laser's, else its code alone. */
    fun chainTotal360(tx: Tx, code: String, v: SurveyChain.Vals): String = SurveyChain.totalShown(tx, code, v, ask = false)
}
