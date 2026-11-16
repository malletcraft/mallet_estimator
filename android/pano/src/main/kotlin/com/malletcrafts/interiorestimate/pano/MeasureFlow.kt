package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * THE MEASURING SCREEN'S OWN RULES (port 63 slice M2, 16 Nov 2026; DESIGN-port63 §1.3, §1.4, §1.5 and §1.8). Amit,
 * 15 Nov 2026, after his first use of the app on a client site: "Having both measurements foto and measured and both
 * is very confusing while using app. The laundry list of measurements shown below screen is also frustrating. …
 * imagemeter shows all dimensions on the annotation there itself which is easy to use."
 *
 * So measuring happens ON THE PHOTO: a tap on a figure makes it the one being measured, the D2 or the app's own pad
 * fills it, and 3 · Measure walks on to the next unmeasured one by itself. The screen itself (:app) is compiled only
 * by CI and nothing of it can be run here, so what a tap DOES, what MAY be measured, which bar is drawn and what it
 * says are decided HERE, pure and tested, and the screen only draws the answers:
 *  - [canRead] / [takes] / [activeFor]: readings follow the phase -- after Survey nothing becomes the reading being
 *    taken -- and a mark that is not in the room (born a Client wish or Vendor added) is never asked for one;
 *  - [figure] / [body]: every tap 3 · Measure has. A tap on a FIGURE picks its owner and makes that reading active
 *    (on the one already active: the pad opens); a tap on a MARK picks it and makes its first unmeasured dimension
 *    active; after Survey and on a mark not in the room the same taps pick, say why ([Refused]) and make nothing
 *    active. 2 · Mark's tap on a figure is [figure] too;
 *  - [firstOpen]: what picking or placing an item makes active, in every step;
 *  - [stay]: after a reading in 3 · Measure, the next of the same item that is still LEFT -- else the walk moves on
 *    ([MeasureWalk.next]);
 *  - [onFace] / [onItem]: the measure bar as drawn ([Bar]) -- in 3 · Measure always there, in 2 · Mark and 4 · 360
 *    only while a dimension OF THE PICKED ITEM is active ([own]);
 *  - [move]: what a press on a step or a face tab does to the measuring -- the walk starts, the reading is let go,
 *    or (the step and the face already open in 3 · Measure) nothing at all;
 *  - [value] / [checkEst] / [checkText]: the one figure of any reading key, the estimate the photo's check names,
 *    and the whole sentence the message line says -- the photo's, or a four-corner mark's closing check's;
 *  - [span] / [panTo]: the photo pans only if the figure being measured is off screen.
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

    /**
     * A TAP ON THE MARK [id] ITSELF IN 3 · MEASURE, [picked] the item picked before it: the mark is picked, its
     * position figures are drawn, and -- at Survey and if it is in the room -- its first unmeasured dimension becomes
     * active ([firstOpen]; none for a fully measured mark, every plate of which then takes a tap: that is how a
     * reading is corrected). Tapped AGAIN while one of its own dimensions is active, that one stays: a second tap must
     * not throw away the dimension he chose. It says nothing: after Survey a tap on a mark is how its label is reached.
     */
    fun body(prep: SurveyPrep.Prep, id: String, picked: String?, active: String?, stage: Int?, ab: (String) -> DoubleArray): Tap {
        val o = prep.itemById(id) ?: return Tap(null, active)
        val own = active?.takeIf { prep.ownerOf(it)?.id == id && takes(prep, it, stage, ab) }
        if (picked == id && own != null) return Tap(o, own)
        return Tap(o, firstOpen(prep, id, stage, ab))
    }

    // ------------------------------------------------------------------ a step or a face pressed

    /**
     * NOTHING PICKED IS NOTHING BEING MEASURED, OUTSIDE 3 · MEASURE (M2 review, 16 Nov 2026): [active] while it is a
     * reading of the PICKED item [picked], else null. In 2 · Mark and 4 · 360 the bar is "the picked item's own keys"
     * (DESIGN-port63 §1.4): its line 1 is the measure's words alone, and the code that names it is on the picked
     * item's row (or, under the pad, in the bar's label cell, which is the picked item's too). So a reading whose
     * owner is not the picked item has no name on the screen -- `width · 3 of 9 measured`, nothing gold on the photo
     * -- and must not be the one a laser shot lands on. The screen asks this where a pick changes without choosing a
     * reading; [onItem] asks it before it draws.
     */
    fun own(prep: SurveyPrep.Prep, active: String?, picked: String?): String? =
        active?.takeIf { picked != null && prep.ownerOf(it)?.id == picked }

    /** What a press on a step or on a face tab does to the measuring ([move]). */
    enum class Move {
        /** The step AND the face that are already open, in 3 · Measure: not a change. The dimension he tapped or
         *  skipped to, the picked mark and what he is typing all stay -- the press does nothing. */
        STAY,
        /** 3 · Measure entered, or another face opened in it: the walk starts from that face's first unmeasured
         *  dimension ([MeasureWalk.next] from the top). */
        WALK,
        /** Every other step entered, and a face opened in one: nothing is picked there, so nothing is being measured. */
        DROP,
    }

    /**
     * A STEP OR A FACE TAB PRESSED: the screen is on step [fromStep] and tab [fromFace], and the press asks for
     * [toStep] and [toFace] (a step press keeps the tab, a tab press keeps the step). Every such press clears the
     * selection, so outside 3 · Measure the reading being taken goes with it ([Move.DROP]) -- 3 · Measure always
     * leaves one active, and 2 · Mark or 4 · 360 would open on a bar that names nothing. Into 3 · Measure, or onto
     * another face in it, the walk starts from the top ([Move.WALK]). But the open face's own tab, the open step
     * itself, or a thumb within 48 dp under the strip routed to it ([StepRoute.stepFor]) is no change at all
     * ([Move.STAY]): restarting the walk there dropped the dimension he had chosen, and his next laser shot landed
     * on another one. Only a FACE is walked: on the Plan tab a press is what it always was.
     */
    fun move(fromStep: String, toStep: String, fromFace: String, toFace: String): Move = when {
        toStep != StepRoute.MEASURE -> Move.DROP
        fromStep == toStep && fromFace == toFace && StepRoute.isFace(toFace) -> Move.STAY
        else -> Move.WALK
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

    // ------------------------------------------------------------------ the one figure of any reading

    /**
     * THE ONE FIGURE OF THE READING [code], whatever owns it -- what the bar's box shows, "the same one figure as the
     * plate, in the same style": a mark's through [MarkDims.value] (with its check); a Line's length, an area's
     * perimeter and a chain's segment or total through the same rule ([SurveyPlan.figure]: the reading, else `≈` and
     * the photo's figure). Null for a key nothing asks for.
     */
    fun value(prep: SurveyPrep.Prep, code: String, ab: (String) -> DoubleArray): MarkDims.Value? {
        prep.markOf(code)?.let { (s, e) ->
            val z = ab(s)
            val d = MarkDims.of(s, e, z[0], z[1]).firstOrNull { it.code == code } ?: return null
            return MarkDims.value(prep.actual, s, e, code, d.est, z[0], z[1])
        }
        val m = prep.readingOf(code, ab)?.second ?: return null
        val state = if (prep.actual[code] != null) MarkDims.State.MEASURED else if (m.est == null) MarkDims.State.UNKNOWN else MarkDims.State.ESTIMATE
        return MarkDims.Value(SurveyPlan.figure(prep.actual, code, m.est).t, state, false)
    }

    /**
     * THE CHECK'S SENTENCE (DESIGN-port63 §1.1): the photo's estimate to name -- "The photo says ≈640 mm. Check the
     * mark, or shoot again." ([MeasureText.photoSays]) -- when the reading under [code] fails [MarkDims.check]: a
     * MARK's reading, more than the larger of 150 mm and 15 % from the photo, on a mark that lies in the squared
     * plane. Null when there is nothing to say. Said when such a reading lands, and when its plate becomes active.
     */
    fun checkEst(prep: SurveyPrep.Prep, code: String, ab: (String) -> DoubleArray): Double? {
        val (s, e) = prep.markOf(code) ?: return null
        val z = ab(s)
        val d = MarkDims.of(s, e, z[0], z[1]).firstOrNull { it.code == code } ?: return null
        // the PHOTO's check alone: a four-corner mark's sloping side may carry its `!` for the closing check instead,
        // and then the photo has nothing to say of it ([checkText] says the right sentence)
        return d.est?.takeIf { MarkDims.photoCheck(prep.actual, s, e, code, it, z[0], z[1]) }
    }

    /**
     * WHAT THE MESSAGE LINE SAYS OF THE READING [code] when it lands and when its plate becomes active -- every check
     * there is, in one place (port 63 slice Q1; DESIGN-quad §2.4). For a rectangle, and for every mark with no `quad`,
     * it is the photo's sentence exactly where [checkEst] names an estimate, and nothing else. A MARK WITH FOUR FREE
     * CORNERS has one more check, its three straight sides against the fourth ([Quad.closing]), and its sentence is
     * said on ANY of the mark's four side codes while it fails -- the reading that completes the four is usually not
     * the sloping side's (the walk reads the sloping bottom first and the left upright last), and the screen speaks
     * only for the code that just landed or became active. So, in this order:
     *  - on the SLOPING side's own code, the closing sentence -- also when that plate fails the photo check too;
     *  - else the photo's sentence, when THIS reading fails its own photo check: it names the figure just read;
     *  - else, on another of the mark's four side codes, the closing sentence;
     *  - else nothing.
     * The closing sentence names the side, in its own words, because it is often said while another side is active:
     * "The other sides make the bottom side ≈2466 mm. Check the readings, or the corners." Nothing is stored.
     */
    fun checkText(tx: Tx, prep: SurveyPrep.Prep, code: String, ab: (String) -> DoubleArray): String? {
        val shut = prep.markOf(code)?.let { (s, e) -> Quad.closing(prep.actual, e)?.takeIf { it.fails }?.let { Triple(s, e, it) } }
        fun closing(): String? = shut?.let { (s, _, c) -> MeasureText.sidesSay(tx, Quad.sideWhat(s, c.side)(tx), c.derived) }
        if (shut != null && shut.third.code == code) return closing()
        checkEst(prep, code, ab)?.let { return MeasureText.photoSays(tx, it) }
        if (shut != null && code in Quad.sideCodes(shut.second)) return closing()
        return null
    }

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
     * and when its plate turns gold: [MeasureText.checkReading] while its own plate carries `!` ([flagged]), and also
     * on any other of a four-corner mark's four side codes while that mark's closing check fails ([checkText]'s own
     * reach: the reading that completes the four is usually not the sloping side's). Null when there is nothing to
     * say. It takes [checkText]'s place from slice I1b on: that one quotes the photo's estimate, and for a four-corner
     * mark the figure its other sides make -- neither is a figure anybody measured.
     */
    fun guardText(tx: Tx, prep: SurveyPrep.Prep, code: String, ab: (String) -> DoubleArray): String? {
        if (flagged(prep, code, ab)) return MeasureText.checkReading(tx)
        val open = prep.markOf(code)?.let { (_, e) -> Quad.closing(prep.actual, e)?.fails == true && code in Quad.sideCodes(e) } == true
        return if (open) MeasureText.checkReading(tx) else null
    }

    // ------------------------------------------------------------------ the measure bar

    /** What the measure bar is. */
    enum class Kind {
        /** A dimension is being measured: the whole bar -- ‹ · its words over the progress · the figure box · D2 · ›. */
        ACTIVE,
        /** 3 · Measure, nothing being measured and something left: ‹, the progress, and › (to the first unmeasured). */
        IDLE,
        /** 3 · Measure, the face finished: ‹, "Front wall · all 9 measured ✓" and the next face's button -- or, when no
         *  face has anything left, "Room measured · all 31 ✓". */
        DONE,
        /** 3 · Measure, nothing to measure on the face: "Nothing to measure on this face yet". */
        EMPTY,
        /** 3 · Measure after Survey: the phase's refusal, standing, so the screen never reads as broken. */
        READ_ONLY,
    }

    /**
     * THE MEASURE BAR AS DRAWN: `label · ‹ · two lines · the figure box · D2 · ›` (DESIGN-port63 §1.4). [cell]: the
     * label cell, which opens the Mark sheet (null: none is drawn). [what]: line 1, the measure's own words alone
     * (null: nothing is being measured). [line]: line 2, the progress -- or, alone, the finished face's line, the
     * empty face's or the phase's refusal. [figure]: the box's one figure (null: no box). [prev] / [next]: ‹ and ›
     * are drawn. [nextFace] and its words [nextFaceText]: the button a finished face offers in ›'s place.
     */
    class Bar(val kind: Kind, val cell: MeasureText.Cell?, val what: String?, val line: String, val figure: MarkDims.Value?,
              val prev: Boolean, val next: Boolean, val nextFace: MeasureWalk.Face? = null, val nextFaceText: String? = null) {
        /** The D2 button and the figure box belong to a dimension being measured. */
        val d2: Boolean get() = kind == Kind.ACTIVE
    }

    /**
     * THE BAR OF 3 · MEASURE on the open face [s], [picked] the picked item and [active] the reading being taken. It
     * is ALWAYS there. The label cell is the picked item's at every stage -- L4's rule: the label stays, controls are
     * hidden.
     *  - after Survey ([Kind.READ_ONLY]): the label and the phase's refusal beside it, nothing else;
     *  - a dimension of THIS face active ([Kind.ACTIVE]): the whole bar; ‹ goes to the stop before, read or not, and ›
     *    skips to the next unmeasured one;
     *  - nothing active and something left ([Kind.IDLE]): ‹, the progress and ›;
     *  - the face finished ([Kind.DONE]): ‹ (to its LAST stop, so a reading can still be reached) and the next face
     *    with something left -- the screen never changes face by itself: he has to turn round;
     *  - nothing to measure ([Kind.EMPTY]): the line saying so; ‹ only when the face has a stop at all (a chain whose
     *    one segment the laser's total already gives).
     */
    fun onFace(tx: Tx, prep: SurveyPrep.Prep, s: String, picked: String?, active: String?, stage: Int?, ab: (String) -> DoubleArray): Bar {
        val cell = picked?.let { MeasureText.cell(tx, prep, it) }
        if (!canRead(stage)) return Bar(Kind.READ_ONLY, cell, null, PhaseTools.readingRefusal(tx, stage).orEmpty(), null, prev = false, next = false)
        val z = ab(s)
        val stops = MeasureWalk.stops(prep, s, z[0], z[1])
        val count = MeasureWalk.count(prep, s, z[0], z[1])
        val on = active?.takeIf { it in stops && takes(prep, it, stage, ab) }
        if (on != null) return Bar(Kind.ACTIVE, cell, MeasureText.label(tx, prep, on, ab), MeasureText.progress(tx, count), value(prep, on, ab), prev = true, next = true)
        if (count.left > 0) return Bar(Kind.IDLE, cell, null, MeasureText.progress(tx, count), null, prev = true, next = true)
        if (count.done) {
            val nf = MeasureWalk.nextFace(prep, s, ab)
            return if (nf == null) Bar(Kind.DONE, cell, null, MeasureText.roomDone(tx, MeasureWalk.room(prep, ab).total), null, prev = true, next = false)
            else Bar(Kind.DONE, cell, null, MeasureText.faceDone(tx, s, count.total), null, prev = true, next = false, nextFace = nf, nextFaceText = MeasureText.nextFace(tx, nf))
        }
        return Bar(Kind.EMPTY, cell, null, tx.fp_nothing_measure_face_yet, null, prev = stops.isNotEmpty(), next = false)
    }

    /**
     * THE BAR OF 2 · MARK AND 4 · 360, where it shows ONLY WHILE A DIMENSION OF THE PICKED ITEM [picked] IS ACTIVE
     * (null: no bar -- nothing picked, a reading that is another item's ([own]: the bar would name nothing), after
     * Survey, and when the picked item has nothing left, as the reading strip went before). ‹ and › move within the
     * active reading's OWNER's own keys only ([MeasureWalk.stopsOf], [MeasureWalk.step]), read or not, wrapping, and
     * never change the selection; an item with one key (a Line) draws neither. Line 2 counts the owner's face -- 4 ·
     * 360 has no open face. [withCell]: the bar draws the label cell -- only while the pad is open, when the pad has
     * taken the place of the picked item's own row; otherwise that row carries the label, as it always did.
     */
    fun onItem(tx: Tx, prep: SurveyPrep.Prep, picked: String?, active: String?, stage: Int?, withCell: Boolean, ab: (String) -> DoubleArray): Bar? {
        val code = own(prep, active, picked)?.takeIf { takes(prep, it, stage, ab) } ?: return null
        val o = prep.ownerOf(code) ?: return null
        val z = ab(o.s)
        val many = MeasureWalk.stopsOf(prep, o.id, ab).size > 1
        return Bar(Kind.ACTIVE, if (withCell) MeasureText.cell(tx, prep, o.id) else null, MeasureText.label(tx, prep, code, ab),
            MeasureText.progress(tx, MeasureWalk.count(prep, o.s, z[0], z[1])), value(prep, code, ab), prev = many, next = many)
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
