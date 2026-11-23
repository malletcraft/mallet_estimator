package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Msg
import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * THE MEASURING SCREEN'S OWN RULES (port 63 slice M2, 16 Nov 2026; DESIGN-port63 §1.3, §1.4, §1.5 and §1.8). Amit,
 * 15 Nov 2026, after his first use of the app on a client site: "Having both measurements foto and measured and both
 * is very confusing while using app. The laundry list of measurements shown below screen is also frustrating. …
 * imagemeter shows all dimensions on the annotation there itself which is easy to use."
 *
 * So measuring happens ON THE PHOTO, and since slice I1b on the same step as marking (DESIGN-imagemeter): what is
 * picked, held and gold is [Pick]'s, where a reading lands is [Landing]'s, and the rules both ask are HERE, pure and
 * tested -- the screen (:app) is compiled only by CI and only draws the answers:
 *  - [canRead] / [takes] / [activeFor]: readings follow the phase -- after Survey nothing turns gold -- and a mark that
 *    is not in the room (born a Client wish or Vendor added) is never asked for one;
 *  - [figure]: what a tap on a FIGURE is -- its owner, the reading it makes gold (on the gold one: the keypad opens),
 *    or why nothing turns gold ([Refused]);
 *  - [firstOpen]: an item's first unread figure, the one picking or placing it makes gold;
 *  - [stay]: after a reading, the next of the same item that is still LEFT -- else the gold walks on
 *    ([MeasureWalk.next]);
 *  - [tabLeft]: the face tab's amber count, the one visible count;
 *  - [flagged] / [guardMsg] / [guardText]: whether a reading keeps its `!`, and the sentence the message line says,
 *    with no figure -- the gross guard's, or a four-corner mark's four sides that do not agree;
 *  - [span] / [panTo]: the photo pans only if the gold figure is off screen.
 * The four-step route's measure bar, its walk between steps and its photo-estimate sentence went in slice I1b.
 *
 * Nothing here writes a reading: the screen's one `reading()` and [MmBox.commit] do. The reading keys, S5's key
 * grammar, a point's c-keys and the record are exactly what they were.
 */
object MeasureFlow {

    // ------------------------------------------------------------------ what may be measured

    /**
     * WHETHER A READING MAY BE KEYED AT [stage]: at Survey and while the stage is unread ([PhaseTools.PREP_HONOURS]
     * holds `readings` since this slice). After Survey a figure is a figure: nothing becomes active, there is no pad,
     * no D2, no walk and no count, and a tap on a figure says [PhaseTools.readingRefusal].
     */
    fun canRead(stage: Int?): Boolean = PhaseTools.prepShows(PhaseTools.Tool.READINGS, stage)

    /** The owner is a mark that is NOT IN THE ROOM (born a Client wish or Vendor added) or a Note: never asked. */
    private fun away(prep: SurveyPrep.Prep, o: SurveyPrep.Item): Boolean =
        o.kind == MARK && prep.markById(o.id)?.second?.let { !MarkDims.asked(it) } == true

    /** [SurveyPrep.Item.kind] of a mark. */
    private const val MARK = "mark" // text:value an item's stored kind, never shown

    /**
     * WHETHER THE READING [code] TAKES A READING NOW: the phase keys readings ([canRead]) and something asks for this
     * very key -- a mark that is in the room ([MarkDims.asked]) and whose checklist has it, a Line's length or an
     * area's perimeter, a chain's segment or its total (not the total the laser gives: [SurveyChain.openKeys]). A
     * tape, a Note, a mark born a wish and a key nothing owns take none. [ab] a face's size, across and up.
     */
    fun takes(prep: SurveyPrep.Prep, code: String, stage: Int?, ab: (String) -> DoubleArray): Boolean {
        if (!canRead(stage)) return false
        prep.markOf(code)?.let { (s, e) -> val z = ab(s); return MarkDims.asked(e) && code in prep.keysOf(s, e, z[0], z[1]) }
        prep.measOf(code)?.let { (_, m) -> return code in prep.keysOf(m) }
        prep.chainOf(code)?.let { (s, c) -> val z = ab(s); return code in SurveyChain.openKeys(c, z[0], z[1]) }
        return false
    }

    /** [code] when it [takes] a reading, else null: what the reading being taken is SET to, wherever the screen sets it. */
    fun activeFor(prep: SurveyPrep.Prep, code: String?, stage: Int?, ab: (String) -> DoubleArray): String? =
        code?.takeIf { takes(prep, it, stage, ab) }

    /**
     * WHAT PICKING OR PLACING THE ITEM [id] MAKES ACTIVE: its first reading still missing -- a mark's first
     * unmeasured dimension ([SurveyPrep.Prep.nextOpen]), a chain's first open segment, a Line's or an area's key while
     * unread. Null after Survey, for a mark that is not in the room, for a tape, and for an item with nothing left.
     */
    fun firstOpen(prep: SurveyPrep.Prep, id: String, stage: Int?, ab: (String) -> DoubleArray): String? {
        if (!canRead(stage)) return null
        prep.markById(id)?.let { (s, e) -> if (!MarkDims.asked(e)) return null; val z = ab(s); return prep.nextOpen(s, e, null, z[0], z[1]) }
        prep.measById(id)?.let { (_, m) -> return prep.nextOpen(m, null) }
        prep.chainById(id)?.let { (s, c) -> val z = ab(s); return prep.nextOpen(c, null, z[0], z[1]) }
        return null
    }

    // ------------------------------------------------------------------ the taps

    /** Why a tap on a figure made nothing active. */
    enum class Refused {
        /** Its mark is not in the room -- a Client wish and a Vendor-added mark alike: [MeasureText.notInRoom]. */
        NOT_IN_ROOM,
        /** Readings are Survey's: [PhaseTools.readingRefusal]. */
        READ_ONLY,
    }

    /**
     * WHAT A TAP DID. [pick]: the item that is picked -- null: the tap is nobody's and NOTHING changes, [active]
     * being what it was. [active]: the reading being taken after the tap (null: none). [pad]: the pad opens (the
     * figure tapped was already the active one). [refused]: why nothing became active -- the screen says it.
     */
    class Tap(val pick: SurveyPrep.Item?, val active: String?, val pad: Boolean = false, val refused: Refused? = null)

    /**
     * A TAP ON THE FIGURE WHOSE READING KEY IS [code] -- a mark's plate, a Line's or an area's tag, a chain's segment
     * figure or its total -- while [active] is the reading being taken. In 3 · Measure and in 2 · Mark alike:
     *  - its owner is picked ([Tap.pick]), so the owner's label can open its Mark sheet;
     *  - at Survey (or while the stage is unread) and for an owner that takes readings, [code] becomes active -- and on
     *    the figure that is ALREADY active the pad opens;
     *  - on a mark NOT IN THE ROOM it says so ([Refused.NOT_IN_ROOM]) -- asked first: such a mark is never measured, in
     *    any phase, so "move the phase back" would be a false promise there;
     *  - after Survey it says the phase's refusal ([Refused.READ_ONLY]).
     * In the last two the owner is picked and nothing is active. A key nothing owns: nobody's tap.
     */
    fun figure(prep: SurveyPrep.Prep, code: String, active: String?, stage: Int?, ab: (String) -> DoubleArray): Tap {
        val o = prep.ownerOf(code) ?: return Tap(null, active)
        if (away(prep, o)) return Tap(o, null, refused = Refused.NOT_IN_ROOM)
        if (!canRead(stage)) return Tap(o, null, refused = Refused.READ_ONLY)
        // a key its owner does not ask for (a total the laser gives): the owner is picked, and what was active on it stays
        if (!takes(prep, code, stage, ab)) return Tap(o, active?.takeIf { prep.ownerOf(it)?.id == o.id })
        return Tap(o, code, pad = code == active)
    }

    // ------------------------------------------------------------------ after a reading

    /**
     * AFTER A READING LANDS ON [code] IN 3 · MEASURE, on the face [s] ([a] x [b] mm): the next reading of the SAME
     * item still missing ([SurveyPrep.Prep.nextAfter], the mark's own order, wrapping) -- when it is LEFT
     * ([MeasureWalk.left]). Null: the item has nothing left and the walk moves on to the next unmeasured dimension of
     * the face ([MeasureWalk.next]), with no tap on the phone. A chain's DERIVED segment has no reading by design and
     * is not left, so the walk does not stop on it (in 2 · Mark and 4 · 360 the D2 flow still does, as before).
     */
    fun stay(prep: SurveyPrep.Prep, s: String, a: Double, b: Double, code: String, ab: (String) -> DoubleArray): String? =
        prep.nextAfter(code, ab)?.takeIf { MeasureWalk.left(prep, s, a, b, it) }

    /** What a face tab's amber count says: how many readings are still to take there -- none after Survey. */
    fun tabLeft(prep: SurveyPrep.Prep, s: String, stage: Int?, a: Double, b: Double): Int =
        if (!canRead(stage)) 0 else MeasureWalk.count(prep, s, a, b).left

    // ------------------------------------------------------------------ the check, with no figure (port 63 slice I1a)

    /**
     * WHETHER THE READING UNDER [code] KEEPS ITS `!` -- what its plate carries ([MarkDims.read]): a MARK's side whose
     * reading fails the gross guard ([MarkDims.guard]: more than twice, or less than half, the side as the mark is
     * drawn), or a four-corner mark's sloping side while its four readings do not close ([Quad.closing]). False for a
     * key with no reading, a Line's, an area's and a chain's, which are checked against nothing. This is what "a
     * reading that fails the check" means wherever the one step asks ([Pick.landed], [Landing.after]).
     */
    fun flagged(prep: SurveyPrep.Prep, code: String, ab: (String) -> DoubleArray): Boolean {
        val (s, e) = prep.markOf(code) ?: return false
        val z = ab(s)
        val d = MarkDims.of(s, e, z[0], z[1]).firstOrNull { it.code == code } ?: return false
        return MarkDims.read(prep.actual, s, e, d, z[0], z[1]).check
    }

    /**
     * WHAT THE MESSAGE LINE SAYS OF THE READING [code], WITH NO FIGURE (DESIGN-imagemeter §18 rule 5) -- when it lands
     * and when its plate turns gold. Null when there is nothing to say. TWO SENTENCES, and neither quotes a number: the
     * photo's estimate and the figure a four-corner mark's other sides make are figures nobody measured.
     *  - [MeasureText.checkReading], "This reading does not match the mark …": the reading's own plate carries `!` for
     *    the gross guard ([flagged]; [MarkDims.guard]) -- on any mark;
     *  - [MeasureText.sidesSay], "The four sides do not agree — see the ! on the bottom side …" (port 63 slice Q2a;
     *    DESIGN-quad §2.4, §11): a four-corner mark's three straight sides and its sloping side do not close
     *    ([Quad.closing]). SAID ON ANY OF THE MARK'S FOUR SIDE CODES while the check fails -- the reading that completes
     *    the four is usually not the sloping side's (the walk reads the sloping bottom first and the left upright last)
     *    -- and it NAMES the sloping side, the one whose plate carries the `!`.
     * WHICH, WHEN A CODE COULD SAY BOTH (DESIGN-quad §2.4, its photo sentence now the gross guard's): on the SLOPING
     * side's own code the closing sentence, also when that reading fails the gross guard too; on one of the other
     * three sides the guard's sentence when THAT reading fails its own guard -- it is about the figure just read --
     * else the closing sentence.
     */
    fun guardMsg(prep: SurveyPrep.Prep, code: String, ab: (String) -> DoubleArray): Msg? {
        val own = flagged(prep, code, ab)
        val open = prep.markOf(code)?.let { (s, e) -> Quad.closing(prep.actual, e)?.takeIf { it.fails && code in Quad.sideCodes(e) }?.let { s to it } }
        if (open != null && (code == open.second.code || !gross(prep, code, ab))) {
            val side = Quad.sideWhat(open.first, open.second.side)
            return { tx -> MeasureText.sidesSay(tx, side(tx)) }
        }
        return if (own) { tx -> MeasureText.checkReading(tx) } else null
    }

    /** [guardMsg] in the language [tx]: what the screen says when a plate turns gold. */
    fun guardText(tx: Tx, prep: SurveyPrep.Prep, code: String, ab: (String) -> DoubleArray): String? = guardMsg(prep, code, ab)?.invoke(tx)

    /** Whether the reading under [code] fails the GROSS GUARD by itself ([MarkDims.guard]) -- [flagged] without the
     *  closing check's share of the `!`. */
    private fun gross(prep: SurveyPrep.Prep, code: String, ab: (String) -> DoubleArray): Boolean {
        val (s, e) = prep.markOf(code) ?: return false
        val z = ab(s)
        val d = MarkDims.of(s, e, z[0], z[1]).firstOrNull { it.code == code } ?: return false
        return MarkDims.guard(prep.actual, s, e, d, z[0], z[1])
    }

    // ------------------------------------------------------------------ the photo pans only if it must

    /**
     * WHAT THE READING [code] MEASURES ON ITS FACE, mm (x from the left edge, y up from the bottom edge): x1, y1, x2,
     * y2 -- a mark's dimension as [MarkDims.of] runs it (a `p`: its corner, twice); a Line's two ends, an area's first
     * and last corner; a chain's segment between its two points, its total from its first point to its last. Null for
     * a key nothing asks for.
     */
    fun span(prep: SurveyPrep.Prep, code: String, ab: (String) -> DoubleArray): DoubleArray? {
        prep.markOf(code)?.let { (s, e) ->
            val z = ab(s)
            return MarkDims.of(s, e, z[0], z[1]).firstOrNull { it.code == code }?.let { doubleArrayOf(it.x1, it.y1, it.x2, it.y2) }
        }
        prep.measOf(code)?.let { (_, m) ->
            if (code !in prep.keysOf(m) || m.pts.isEmpty()) return null
            val p = m.pts.first(); val q = m.pts.last()
            return doubleArrayOf(p[0], p[1], q[0], q[1])
        }
        prep.chainOf(code)?.let { (_, c) ->
            val i = prep.keysOf(c).indexOf(code)
            if (i < 0 || c.nodes.size < 2) return null
            val p = if (i < c.nodes.size - 1) c.nodes[i] else c.nodes.first()
            val q = if (i < c.nodes.size - 1) c.nodes[i + 1] else c.nodes.last()
            return doubleArrayOf(p[0], p[1], q[0], q[1])
        }
        return null
    }

    /**
     * THE PHOTO PANS ONLY IF THE FIGURE BEING MEASURED IS OFF SCREEN (DESIGN-port63 §1.4): [view] over [box], and the
     * dimension's span from ([x1], [y1]) to ([x2], [y2]) in VIEW px. Null -- nothing moves -- while any of the span is
     * in the box; else the same zoom, out of fit, centred on the span's middle (kept over the picture:
     * [SurveyPrep.PhotoView.clamp]).
     */
    fun panTo(view: SurveyPrep.PhotoView, box: SurveyPrep.PhotoBox, x1: Double, y1: Double, x2: Double, y2: Double): SurveyPrep.PhotoView? {
        val p = view.toS(box, x1, y1); val q = view.toS(box, x2, y2)
        if (DimLayout.clip(DimLayout.Seg(p[0], p[1], q[0], q[1]), box.w, box.h) != null) return null
        return view.unfit(box).copy(cx = (x1 + x2) / 2, cy = (y1 + y2) / 2).clamp(box)
    }
}
