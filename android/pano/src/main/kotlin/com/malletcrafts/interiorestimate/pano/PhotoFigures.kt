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
 *  4. The Tape WAS the one exception -- its figure IS what the photo says -- "until he rules". He asked (23 Nov 2026,
 *     on 0.3.350: "does tape tool make any sense now? as we don't want any foto measured dimensions on fone."), and
 *     while [Features.TAPES] is off a Tape a room already has shows NO figure ([tape]; port 63 slice F3).
 *  5. THE 360's tags follow the same rule -- and nothing can be acted on there, so nothing shows `?` and nothing is
 *     gold ([line360], [chainSeg360], [chainTotal360]).
 *
 * The Plan, the six drawings, the saved pictures and the exports are NOT this object's: they follow in slice M3.
 *
 * AMENDED BY AMIT'S FIVE POINTS FROM 0.3.345 (port 63 slice I1c, 23 Nov 2026; DESIGN-imagemeter §21), over rule 2 above:
 *  - P3, "When a mark type window is selected, then only its different measurements are visible. its also annoying.
 *    i want to see all measurements in its units always visible": EVERY MARK SHOWS EVERY FIGURE THAT HAS BEEN READ --
 *    its size AND where it sits (from the corner, from the floor or the ceiling, a Beam's drop, the `⊥` depth) --
 *    ALWAYS, picked or not. §18's heart stands: a figure nobody has read shows NOTHING on a mark that is not picked,
 *    and the quiet `?` only on the picked mark; the one being measured stays H1's gold. A Line's, an area's and a
 *    chain's read figure likewise, always.
 *  - P4, "Measurements on any mark type alwyas show FM1 , 2 etc. it cosumes lot of space on line": A FIGURE'S LABEL IS
 *    THE NUMBER ALONE -- `1234`, not "F-M1 1234 mm"; every figure here is millimetres. An item's code is shown only
 *    where the item is NAMED ([nameOf]: the picked item's own label), never on its line. A mark's CODE on its box
 *    stays: it names the mark, it is no measurement.
 *  - P5, "no way to set alignment of measurements to line direction": every label is laid out ALONG its dimension,
 *    marks and Lines together, by [layout] and [DimLayout.placeShown].
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
     * THE FIGURES THE MARK [e] ON [s] SHOWS (rule 2 as P3 amends it), in its checklist's order ([MarkDims.of]):
     *  - not [picked]: EVERY figure that has been READ -- its size and its position alike, `w` `h` `l` `r` `s` `t` `d`
     *    and the `⊥` depth, a four-corner mark's sides and diagonal -- and NOTHING for one that has not;
     *  - [picked]: every dimension it asks -- [Look.READ] with its figure, [Look.ASK] with `?`;
     *  - the dimension whose reading key is [gold]: [Look.GOLD], `▶898` or `▶?` ([ready] false: `▷`); while digits
     *    are typed on the keypad ([live], [MeasurePad.live]; null or empty: none) it reads those digits, in the
     *    reading's own style and with no `!` -- nothing has been checked yet. A gold `⊥` chip carries its own name,
     *    in [tx]'s language, while it is gold: `▶⊥ reveal depth ?` ([MarkDims.figureText]);
     *  - a mark that is NOT IN THE ROOM ([MarkDims.asked] false: born a Client wish or Vendor added, or a Note): NONE,
     *    picked or not (§18 rule 6);
     *  - [ask] false -- the phase keys no readings ([MeasureFlow.canRead]: after Survey): the picked mark shows its READ
     *    figures only, and no `?` -- "not read is shown only where it can be acted on" (rule 2), and nothing can be read
     *    there (the review of I1, finding 5).
     */
    fun ofMark(tx: Tx, actual: Map<String, Double>, s: String, e: SurveyPrep.Mark, a: Double, b: Double, picked: Boolean, gold: String?,
               live: String? = null, ready: Boolean = true, ask: Boolean = true): List<Figure> {
        if (!MarkDims.asked(e)) return emptyList()
        return MarkDims.of(s, e, a, b).mapNotNull { d ->
            val stored = MarkDims.read(actual, s, e, d, a, b)
            val on = d.code == gold
            val measured = stored.state == MarkDims.State.MEASURED
            if (!((picked && ask) || on || measured)) return@mapNotNull null
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
               ready: Boolean = true, ask: Boolean = true): List<Pair<SurveyPrep.Mark, List<Figure>>> =
        (prep.surfaces[s]?.marks ?: emptyList<SurveyPrep.Mark>()).filter { !MarkLife.isNote(it) }
            .map { e -> e to ofMark(tx, prep.actual, s, e, a, b, pick.picked == e.id, pick.gold, live, ready, ask) }

    /**
     * THE FACE AS [DimLayout.placeShown] TAKES IT: every mark of [onFace] with ONLY the figures rule 2 shows, so an
     * unpicked, unmeasured mark costs the layout nothing. The HELD mark ([Pick.State.held]) shows its eight tabs --
     * or, where [free] says its corners are free ([Quad.free], the one rule the painter, the press and this ask), its
     * four corners -- and its `w` and `h` plates are anchored clear of them. [toPx] face mm to canvas px, [u] the
     * drawing's unit, [dp] px a dp, [textW] the painter's own measure of a text.
     *
     * THEN EVERY TAPE, LINE, AREA AND CHAIN OF THE FACE ([items], port 63 slice I1c, P5), after the marks: their figures
     * are placed with the marks' plates, so no label lies on another, each turned along its own line.
     *
     * [ask]: the phase keys readings ([MeasureFlow.canRead]) -- false after Survey, where the picked item shows its READ
     * figures only and no `?` anywhere (the review of I1, finding 5).
     *
     * [tapes]: [Features.TAPES], handed on to [items] -- off, a Tape's photo figure is not among them (port 63 slice F3):
     * an unpicked Tape's code is, in its place ([tapeLabel]).
     */
    fun layout(tx: Tx, prep: SurveyPrep.Prep, s: String, a: Double, b: Double, pick: Pick.State, toPx: (Double, Double) -> DoubleArray,
               u: Double, dp: Double, textW: (text: String, fs: Double, bold: Boolean) -> Double, live: String? = null,
               ready: Boolean = true, free: (SurveyPrep.Mark) -> Boolean = { false }, ask: Boolean = true,
               tapes: Boolean = Features.TAPES): List<DimLayout.Mark> =
        (prep.surfaces[s]?.marks ?: emptyList<SurveyPrep.Mark>()).filter { !MarkLife.isNote(it) }.map { e ->
            val on = pick.picked == e.id
            DimLayout.markShown(tx, prep.actual, s, e, a, b, on, on && pick.held, pick.gold, toPx, u, dp, textW, live, free(e), ready, ask)
        } + items(prep, s, a, b, pick, toPx, dp, textW, ready, ask, tapes)

    /**
     * EVERY FIGURE OF THE FACE [s] THAT IS NOT A MARK'S, as [DimLayout.placeShown] takes it ([DimLayout.item]; port 63
     * slice I1c, P4 and P5), in drawing order -- the Lines and areas, then the Tapes, then the chains, as the painter
     * draws them:
     *  - a LINE: its [line] figure ALONG it, from its first point to its last;
     *  - an AREA: its perimeter's figure at its middle (the mean of its corners), level -- it has no one line;
     *  - a TAPE: its [tapeLabel] along its band, centred on it; no tap target (it takes no reading) -- its [tape]
     *    figure while its tool is on; while [tapes] is off ([Features.TAPES], the default of the last parameter; port
     *    63 slice F3) NO FIGURE, its figure being the photo's: its CODE, the plain band's name, and for the picked
     *    one -- whose chip names it -- nothing;
     *  - a CHAIN: each segment's [chainSeg] figure along that segment, and its [chainTotal] at its last point, level.
     * A figure that shows nothing ("", or a chain segment with none) is not handed in: it costs the layout nothing.
     */
    fun items(prep: SurveyPrep.Prep, s: String, a: Double, b: Double, pick: Pick.State, toPx: (Double, Double) -> DoubleArray, dp: Double,
              textW: (text: String, fs: Double, bold: Boolean) -> Double, ready: Boolean = true, ask: Boolean = true,
              tapes: Boolean = Features.TAPES): List<DimLayout.Mark> {
        val sf = prep.surfaces[s] ?: return emptyList()
        val out = ArrayList<DimLayout.Mark>()
        fun px(p: DoubleArray) = toPx(p[0], p[1])
        for (m in sf.meas) if ((m.kind == SurveyMeas.LINE || m.kind == SurveyMeas.AREA) && m.pts.isNotEmpty()) {
            val t = line(m, prep.actual, pick, ready, ask)
            val key = SurveyMeas.key(m)
            val read = key != null && prep.actual[key] != null
            val (from, to) = if (m.kind == SurveyMeas.LINE && m.pts.size >= 2) px(m.pts.first()) to px(m.pts.last())
                             else px(doubleArrayOf(m.pts.map { it[0] }.average(), m.pts.map { it[1] }.average())) to null
            DimLayout.item(m.id, t.key, t.text, from, to, pick.picked == m.id, t.gold, read, dp, textW)?.let { out += it }
        }
        for (m in sf.meas) if (m.kind == SurveyMeas.TAPE && m.pts.size == 2)
            DimLayout.item(m.id, null, tapeLabel(m, pick.picked == m.id, tapes), px(m.pts[0]), px(m.pts[1]), pick.picked == m.id, false, false, dp, textW)?.let { out += it }
        for (c in sf.chains) if (c.nodes.size >= 2) {
            val v = SurveyChain.vals(c, prep.actual, a, b)
            for (i in 0 until c.nodes.size - 1) chainSeg(c, v, i, pick, ready, ask)?.let { t ->
                DimLayout.item(c.id, t.key, t.text, px(c.nodes[i]), px(c.nodes[i + 1]), pick.picked == c.id, t.gold, v.meas.getOrNull(i) != null, dp, textW)?.let { out += it } }
            chainTotal(c, v, pick, ready, ask).let { t ->
                DimLayout.item(c.id, t.key, t.text, px(c.nodes.last()), null, pick.picked == c.id, t.gold, v.src == SurveyChain.MEASURED || v.src == SurveyChain.LASER, dp, textW)?.let { out += it } }
        }
        return out
    }

    /**
     * WHAT THE PICKED ITEM IS NAMED BY -- its CODE, `FW1`, `F-M1`, `F-T1`, `FK1` -- the one place an item's code is shown
     * once its figures are the number alone (P4): the picked item's own code chip ([SurveyPrepTouch.chipText]). Null
     * when nothing is picked, or what is picked is gone. A mark's code on its box is drawn as it always was, picked or
     * not: it names the mark, it is no measurement.
     */
    fun nameOf(prep: SurveyPrep.Prep, pick: Pick.State): String? {
        val id = pick.picked ?: return null
        return prep.markById(id)?.second?.code ?: prep.measById(id)?.second?.code ?: prep.chainById(id)?.second?.code
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
     * A PLOTTED LINE'S OR AREA'S FIGURE ON ITS FACE, THE NUMBER ALONE ([SurveyMeas.shownTag]; P4): `1240` once read --
     * whether or not it is picked; NOTHING until then; `?` while it is the picked item; `▶?` while its reading is the
     * gold one. An area never shows its m². A TAPE answers an empty tag here: its own is [tape]. [asks] false (after
     * Survey): no `?`, the reading alone (the review of I1, finding 5).
     */
    fun line(m: SurveyPrep.Meas, actual: Map<String, Double>, pick: Pick.State, ready: Boolean = true, asks: Boolean = true): Tag {
        val key = SurveyMeas.key(m) ?: return Tag("", null, false)
        val read = actual[key]; val ask = asks && pick.picked == m.id; val on = pick.gold == key
        val t = SurveyMeas.shownTag(m.code, m.kind, read, ask || on)
        return Tag(glyph(on, ready) + t, key.takeIf { SurveyMeas.tagTaps(read, ask || on) }, on)
    }

    /** A Line being pulled and an area being tapped show NO figure: their length and their m² are the photo's (rule 1).
     *  What the painter draws for a draft's tag. */
    const val DRAFT_TAG = ""

    /** A TAPE'S FIGURE, the number alone ([SurveyMeas.tapeFigure]; P4): its length on the photo -- the one figure that
     *  is the photo's (§18 rule 4), placed along its band by [items], and no tap target. WHILE [tapes] IS OFF
     *  ([Features.TAPES], the default of the last parameter; port 63 slice F3) IT IS NOTHING: Amit, 23 Nov 2026, "we
     *  don't want any foto measured dimensions on fone" -- a Tape a room already has is its plain band. */
    fun tape(m: SurveyPrep.Meas, tapes: Boolean = Features.TAPES): String =
        if (m.kind == SurveyMeas.TAPE && m.pts.size == 2) SurveyMeas.tapeFigure(SurveyMeas.plen(m.pts), tapes) else ""

    /**
     * WHAT STANDS ALONG A TAPE'S BAND ON THE EDITING PHOTO ([SurveyMeas.tapeSays]; port 63 slice F3b): its [tape]
     * figure while its tool is on; while it is off, ITS CODE -- `F-T1`, the plain band's name (Amit's selector answer
     * of 23 Nov 2026, "Plain band, no figure": the band with its code, no ticks and no number) -- unless it is the
     * [picked] item, whose code chip names it. Placed by [items] with every other label, turned along the band, and no
     * tap target: a tap on it picks the Tape by its line, as a tap on the band does. "" for anything but a Tape.
     */
    fun tapeLabel(m: SurveyPrep.Meas, picked: Boolean, tapes: Boolean = Features.TAPES): String =
        if (m.kind == SurveyMeas.TAPE && m.pts.size == 2) SurveyMeas.tapeSays(m.code, SurveyMeas.plen(m.pts), picked, tapes) else ""

    /** The same Line or area ON THE 360: its reading once read, the number alone; else nothing. Never `?`, never gold. */
    fun line360(m: SurveyPrep.Meas, actual: Map<String, Double>): String =
        SurveyMeas.key(m)?.let { SurveyMeas.shownTag(m.code, m.kind, actual[it], ask = false) } ?: ""

    /**
     * SEGMENT [i] OF THE CHAIN [c] ON ITS FACE ([SurveyChain.segShown]): its reading, white; `?` while the chain is the
     * picked item; the gold glyph before it while it is the one being measured; NULL -- nothing drawn -- for an unread
     * segment of a chain that is not picked. No derived figure and no photo figure. [v] is [SurveyChain.vals]. [asks]
     * false (after Survey): no `?` (the review of I1, finding 5).
     */
    fun chainSeg(c: SurveyPrep.Chain, v: SurveyChain.Vals, i: Int, pick: Pick.State, ready: Boolean = true, asks: Boolean = true): Tag? {
        val key = c.code + "." + (i + 1)
        val on = pick.gold == key
        val (t, ink) = SurveyChain.segShown(v, i, (asks && pick.picked == c.id) || on) ?: return null
        return Tag(glyph(on, ready) + t, key, on, ink)
    }

    /**
     * THE CHAIN'S TOTAL UNDER ITS LAST POINT ([SurveyChain.totalShown]; P4): `Σ 3310` read or where the laser gives it,
     * else nothing -- `Σ ?` while the chain is picked. It takes a tap under its key unless the laser gives it
     * ([SurveyChain.totalReadOnly]) or it shows no figure and no `?`. [asks] false (after Survey): no `Σ ?`.
     */
    fun chainTotal(c: SurveyPrep.Chain, v: SurveyChain.Vals, pick: Pick.State, ready: Boolean = true, asks: Boolean = true): Tag {
        val key = c.code + "." + SurveyChain.TOTAL
        val on = pick.gold == key; val ask = (asks && pick.picked == c.id) || on
        val taps = !SurveyChain.totalReadOnly(v) && (v.src == SurveyChain.MEASURED || ask)
        val t = SurveyChain.totalShown(v, ask)
        return Tag(if (t.isEmpty()) "" else glyph(on, ready) + t, key.takeIf { taps }, on, SurveyChain.totalColour(v))
    }

    /** Segment [i] of a chain ON THE 360: its reading, or nothing. */
    fun chainSeg360(v: SurveyChain.Vals, i: Int): Pair<String, Int>? = SurveyChain.segShown(v, i, ask = false)

    /** A chain's total ON THE 360: read or the laser's, `Σ 3310`; else nothing. */
    fun chainTotal360(v: SurveyChain.Vals): String = SurveyChain.totalShown(v, ask = false)
}
