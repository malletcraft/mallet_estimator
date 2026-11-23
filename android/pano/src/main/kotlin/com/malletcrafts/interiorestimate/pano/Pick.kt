package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * PICKED, HELD, GOLD -- THE ONE VALUE 2 · MARK IS IN, AND WHAT EVERY EVENT DOES TO IT (port 63 slice I1a, 16 Nov 2026;
 * DESIGN-imagemeter §3 and §3.1, whose table of 28 rows is this object's specification, one test case per row in
 * PickTest). Amit, 16 Nov 2026, on build 0.3.337: "do we really need two screen now? which is mark and measure? as we
 * should be measure where marking is done." -- so marking and measuring are ONE step, and the guard that 3 · Measure
 * was ("nothing can be dragged") has to live somewhere else. It lives HERE, pure and tested, because :app is compiled
 * only by CI and nothing of the screen can be run in this module.
 *
 * Three things can be true of an item ([State]):
 *  - PICKED -- one item at most; it shows all its figures;
 *  - HELD -- picked by his own hand; it shows its handles and it can move. A locked item, one the phase holds
 *    read-only, and anything at Estimation or Client approved ([PhaseTools.holds]) is never held ([mayHold]);
 *  - GOLD -- one dimension at most (a reading key), at Survey only, on a face only ([goldOk]), and ALWAYS a
 *    dimension of the picked item. It is the figure the laser's next reading lands on.
 *
 * THE RULE THAT REPLACES "NOTHING CAN BE DRAGGED": only a held item moves ([drag]), and whenever a reading lands or
 * the gold moves by itself the handles go ([landed], [next]). A mark the walk has reached cannot be dragged by the
 * thumb of the hand that is not holding the laser; one tap on it gives the handles back ([body]).
 *
 * THREE RULES OF THE M2 SCREEN ARE RE-ANCHORED HERE, not dropped with their old tests:
 *  - the M2 review's guard -- "a thumb resting on the photo while the other hand holds the laser must change nothing"
 *    and "empty photo does nothing": nothing but a held item drags ([drag]), a landed reading drops the hold, and an
 *    empty tap leaves a gold figure alone unless he is holding the item ([empty], rows 10 and 11);
 *  - [MeasureFlow.Move.STAY] -- the open face's own tab and the open step are no change at all ([tab], [step], row 26);
 *  - [MeasureFlow.body]'s rule -- a second tap on a mark must not throw away the dimension he chose ([body], row 2).
 *
 * Every function is the whole of one event: it takes the state before and answers the state after ([Out.state]) with
 * what else the screen then does ([Out]). Nothing here writes a reading, moves a mark or shows a word but the
 * wall-finished note ([finishedText]); the screen writes, and asks.
 */
object Pick {

    /** What is picked, whether it is held, and which reading key is gold. */
    data class State(val picked: String? = null, val held: Boolean = false, val gold: String? = null)

    /** Nothing picked, nothing held, nothing gold. */
    val NONE = State()

    /** Where the events happen: the room's marks and readings, the project's quote stage ([PhaseTools.stageOf]), the
     *  open step ([StepRoute.ROUTE]) and a face's size, across and up. */
    class Scene(val prep: SurveyPrep.Prep, val stage: Int?, val step: String, val ab: (String) -> DoubleArray)

    /** What the event does to the keypad. */
    enum class Pad {
        /** Nothing: open stays open, shut stays shut. */
        KEEP,
        /** It opens (a tap on the gold figure). */
        OPEN,
        /** It shuts, and what was typed is stored first ([MeasurePad.commit]) -- every tap on the photo but one on the
         *  gold figure, and whatever else lets the gold figure go. */
        SHUT_STORE,
        /** It shuts and what was typed is dropped: the reading it was typed for has gone, or the laser has just
         *  written it. */
        SHUT_DROP,
    }

    /** Which wall-finished note is said ([finishedText]). */
    enum class Finished { FACE, ROOM }

    /**
     * WHAT AN EVENT DID. [state]: picked / held / gold after it. [pad]: what the keypad does. [refused]: why a tap on
     * a figure made nothing gold -- the screen says it ([MeasureText.notInRoom], [PhaseTools.readingRefusal]).
     * [options]: the picked item's options open -- the panel that slides up; after a long press, when the finger
     * lifts. [nudge]: the part of the held item that the fine nudges now move (a tab, a corner,
     * an end). [finished]: nothing is left on the wall -- the note says so. [stay]: NOTHING AT ALL CHANGES -- the
     * screen returns at once, storing nothing, ending no draft (row 26, row 28). [byItself]: the gold moved by itself
     * (a reading, `▸`), so its mark has no handles, and the photo pans if the new gold figure is off screen
     * ([MeasureFlow.panTo]). [waits]: a reading was written while a sheet was open or a finger was on a grab -- the
     * gold stays, and [released] is asked when the sheet closes or the finger lifts (row 20). [zoom]: the photo zooms
     * 2x about the point (row 13).
     */
    class Out(val state: State, val pad: Pad = Pad.KEEP, val refused: MeasureFlow.Refused? = null, val options: Boolean = false,
              val nudge: String? = null, val finished: Finished? = null, val stay: Boolean = false, val byItself: Boolean = false,
              val waits: Boolean = false, val zoom: Boolean = false)

    // ------------------------------------------------------------------ what may be held, what may be gold

    /**
     * WHETHER THE ITEM [id] MAY BE HELD: the phase holds anything at all ([PhaseTools.holds]: not at Estimation, not
     * at Client approved, where 2 · Mark is the view), the phase lets THIS item be edited ([PhaseTools.editable]) and
     * it is not locked. Where it may not, every "held: yes" of the table is "no". False for an id that is no item.
     */
    fun mayHold(sc: Scene, id: String): Boolean {
        val o = sc.prep.itemById(id) ?: return false
        return PhaseTools.holds(sc.stage) && !o.locked && PhaseTools.editable(sc.prep, id, sc.stage)
    }

    /** WHETHER ANYTHING MAY BE GOLD: in 2 · Mark, and where the phase keys readings ([MeasureFlow.canRead]: Survey, and
     *  while the stage is unread). NEVER on 3 · 360, never in 1 · Lines, never after Survey (row 27). */
    fun goldOk(sc: Scene): Boolean = sc.step == StepRoute.MARK && MeasureFlow.canRead(sc.stage)

    /** The item [id]'s first unread figure, where anything may be gold ([MeasureFlow.firstOpen]): none when it has
     *  none, is not in the room, is a tape, or after Survey. */
    private fun firstGold(sc: Scene, id: String): String? =
        if (goldOk(sc)) MeasureFlow.firstOpen(sc.prep, id, sc.stage, sc.ab) else null

    /** The gold figure while it is one of [id]'s and still takes a reading; else null. */
    private fun ownGold(sc: Scene, st: State, id: String): String? =
        st.gold?.takeIf { goldOk(sc) && sc.prep.ownerOf(it)?.id == id && MeasureFlow.takes(sc.prep, it, sc.stage, sc.ab) }

    /** A figure TURNED gold between [before] and [after]: the moment of measuring -- the laser's link is started then
     *  ([LaserChip.gold]), and the check's sentence is said if that plate carries `!` ([MeasureFlow.guardText]). */
    fun turnedGold(before: State, after: State): Boolean = after.gold != null && after.gold != before.gold

    /**
     * THE STATE AS THE PHASE, THE STEP AND THE ROOM STILL ALLOW IT -- asked whenever any of them moved under the
     * screen (a sync that moves the stage, a pull of readings): the pick goes with an item that is gone, the hold
     * with one that may no longer be held, the gold where nothing may be gold or its reading is no longer asked.
     */
    fun settle(sc: Scene, st: State): State {
        val id = st.picked?.takeIf { sc.prep.itemById(it) != null } ?: return NONE
        return State(id, st.held && mayHold(sc, id), ownGold(sc, st, id))
    }

    // ------------------------------------------------------------------ taps, with no tool armed (rows 1 - 8)

    /**
     * ROWS 1 AND 2 -- A TAP ON THE BODY OF [id] (a mark, a Tape's band, a Line, an area, a chain): it is picked and
     * HELD ([mayHold]). The gold: UNCHANGED while it is already one of this item's figures -- a second tap must not
     * throw away the dimension he chose -- else, at Survey, the item's first unread figure; none when it has none, is
     * not in the room, or after Survey. An open keypad stores what was typed and shuts. An id that is no item: nobody's
     * tap, nothing changes.
     */
    fun body(sc: Scene, st: State, id: String): Out {
        if (sc.prep.itemById(id) == null) return Out(st)
        return Out(State(id, mayHold(sc, id), ownGold(sc, st, id) ?: firstGold(sc, id)), pad = Pad.SHUT_STORE)
    }

    /** ROW 3 -- A LONG PRESS ON [id]: picked and held, the gold as [body] leaves it, and its options open when the
     *  finger LIFTS: the panel that slides up ([MarkSheet.openPanel]; the long-press menu went in slice I2b). */
    fun longPress(sc: Scene, st: State, id: String): Out {
        val o = body(sc, st, id)
        return if (o.state.picked != id) o else Out(o.state, pad = o.pad, options = true)
    }

    /**
     * ROWS 4, 5 AND 6 -- A TAP ON THE FIGURE WHOSE READING KEY IS [code] ([MeasureFlow.figure] decides what it is):
     *  - a figure that takes a reading (row 4): its owner is picked -- HELD ONLY IF IT ALREADY WAS -- and the figure
     *    turns gold; an open keypad stores and shuts;
     *  - THE GOLD FIGURE ITSELF (row 5): nothing changes, and the keypad opens;
     *  - after Survey, or a figure of a mark that is not in the room (row 6): its owner is picked, held only if it
     *    already was, NOTHING is gold, and the refusal is said ([Out.refused]);
     *  - a key its owner does not ask (a chain's total the laser gives): the owner is picked and what was gold on it stays.
     * A key nothing owns: nobody's tap. Where nothing may be gold ([goldOk]: 3 · 360) the owner is picked and that is all.
     */
    fun figure(sc: Scene, st: State, code: String): Out {
        val t = MeasureFlow.figure(sc.prep, code, st.gold, sc.stage, sc.ab)
        val o = t.pick ?: return Out(st)
        val held = st.held && st.picked == o.id && mayHold(sc, o.id)
        if (t.refused != null) return Out(State(o.id, held, null), pad = Pad.SHUT_STORE, refused = t.refused)
        if (!goldOk(sc)) return Out(State(o.id, held, null), pad = Pad.SHUT_STORE)
        if (t.pad && st.picked == o.id) return Out(st, pad = Pad.OPEN)
        return Out(State(o.id, held, t.active), pad = Pad.SHUT_STORE)
    }

    /** ROW 7 -- A TAP ON THE PICKED ITEM'S CODE CHIP, OR `⋯`: nothing changes; its options open (slice I2). With
     *  nothing picked there is nothing to open. */
    fun options(st: State): Out = Out(st, options = st.picked != null)

    /** ROW 8 -- A TAP ON A HANDLE OF THE HELD ITEM ([part]: a tab, a corner, an end): nothing moves and nothing
     *  changes; that part becomes the one the fine nudges move. Not held: no handle is there to tap. */
    fun handle(st: State, part: String): Out = Out(st, nudge = part.takeIf { st.held })

    /** THE ID [SurveyPrepTouch.hit] IS HANDED AS ITS `selId`: the held item's, else none -- so only a HELD item's tabs,
     *  corners and ends take a press, and a mark the walk reached is pressed only as any other mark is, by its body. */
    fun handleId(st: State): String? = st.picked?.takeIf { st.held }

    /** The part a chain's point [i] is, as the nudge target ([handle]). */
    fun nodePart(i: Int): String = "n$i" // text:value a part's key, never shown

    /**
     * A TAP ON THE PHOTO WITH NO TOOL ARMED, [hit] what [SurveyPrepTouch.hit] found under it (handed [handleId]) and
     * [line] the Tape, Line, area or chain whose line the finger is on when nothing else is hit (the screen's 12 dp
     * pick) -- the hit order's answer turned into the event it is:
     *  - a figure: [figure]; a handle of the held item -- a tab, a corner, a chain's point: [handle];
     *  - THE PICKED ITEM'S CODE CHIP ([SurveyPrepTouch.Hit.Code], slice I2a): [options] -- row 7, the panel opens;
     *  - an item's body -- a mark, the picked Tape's band, a chain's point of a chain that is not held, [line]: [body];
     *  - nothing: [empty].
     * With a tool armed the screen places instead and asks [placed].
     */
    fun tap(sc: Scene, st: State, hit: SurveyPrepTouch.Hit?, line: String? = null): Out = when (hit) {
        is SurveyPrepTouch.Hit.Code -> options(st)
        is SurveyPrepTouch.Hit.Tag -> figure(sc, st, hit.code)
        is SurveyPrepTouch.Hit.Mark -> if (hit.part != BODY && handles(st, hit.id)) handle(st, hit.part) else body(sc, st, hit.id)
        is SurveyPrepTouch.Hit.Node -> if (handles(st, hit.id)) handle(st, nodePart(hit.i)) else body(sc, st, hit.id)
        is SurveyPrepTouch.Hit.Body -> st.picked?.let { body(sc, st, it) } ?: empty(st)
        null -> line?.let { body(sc, st, it) } ?: empty(st)
    }

    /** [SurveyPrepTouch.Hit.Mark.part] of a mark pressed whole. */
    private const val BODY = "body" // text:value a part's key, never shown

    // ------------------------------------------------------------------ drags (row 9)

    /** What a drag that began on an item does. */
    enum class Drag {
        /** The held item moves: whole by its body (after [BODY_SLOP_DP]), or by the handle pressed. */
        MOVE,
        /** The photo pans: the press was on nothing, on an item that is not picked, or on one the walk reached. */
        PAN,
        /** The picked item is locked: nothing moves, and the lock's refusal is said once, in today's words. */
        LOCKED,
        /** The picked item is one the phase holds read-only: nothing moves, and the phase's refusal is said once. */
        READ_ONLY,
    }

    /** The held item's body moves it whole only after this many dp (it was from the first pixel): a press that lifts
     *  as a tap never nudged the mark it tapped. A handle keeps its own reach and dead zone. */
    const val BODY_SLOP_DP = 8.0

    /**
     * ONLY A HELD ITEM MOVES. A drag that began on the item [on] (null: on nothing): [Drag.MOVE] only when it is the
     * picked item AND it is held. A drag on the picked item that is not held moves nothing -- it says why when the
     * reason is a lock or the phase, and otherwise (the walk reached it, or the phase is the view) it pans, as a drag
     * on anything else does: an unpicked mark included, which a press no longer picks and drags.
     */
    fun drag(sc: Scene, st: State, on: String?): Drag = when {
        on == null || on != st.picked -> Drag.PAN
        st.held && mayHold(sc, on) -> Drag.MOVE
        sc.prep.itemById(on)?.locked == true -> Drag.LOCKED
        PhaseTools.holds(sc.stage) && !PhaseTools.editable(sc.prep, on, sc.stage) -> Drag.READ_ONLY
        else -> Drag.PAN
    }

    /** ROW 9 -- THE HELD ITEM'S BODY OR HANDLE DRAGGED: picked, held and gold are all unchanged WHILE THE FINGER IS
     *  DOWN. (What the drag does to the item is the screen's; a move changes no reading. It CAN change which figures the
     *  item asks -- [reshaped], asked when the drag ends.) */
    fun dragged(st: State): Out = Out(st)

    /**
     * THE PICKED ITEM WAS MOVED OR RESHAPED UNDER THE PICK (the review of Q2, finding 2; 23 Nov 2026) -- asked when a
     * drag ENDS, after a nudge of Fine move's pad that moved something, and after the panel's `Rectangle`. Picked and
     * held are unchanged. THE GOLD IS KEPT WHILE ITS FIGURE IS STILL ASKED; else it becomes the item's first unread
     * figure, as a tap on its body would make it ([body]); else nothing is gold.
     *
     * Why it exists: what a mark ASKS follows its shape and its place. A corner dragged off its box stores `quad`, and
     * the mark then asks its four sides where it asked `w` and `h` ([Quad.asked]); the panel's `Rectangle` goes the
     * other way; and a mark carried across the middle of its wall, or onto the wall's edge, asks the other one of
     * `l` / `r` or none ([SurveyPrep.measures]). Until this rule the gold stayed on the key the mark had stopped asking:
     * a gold frame round the mark, no gold figure on it, and the laser's next reading refused ("tap a measurement
     * first") -- straight after the drag that made the trapezoid.
     *
     * [Out.stay] when nothing changes -- every drag that leaves the asked figures as they were -- so the screen does
     * nothing at all for it. Where the gold moves OFF a figure, what was typed for that figure on the keypad is dropped
     * and the keypad shuts ([Pad.SHUT_DROP]): the reading it was typed for is no longer asked.
     */
    fun reshaped(sc: Scene, st: State): Out {
        val id = st.picked?.takeIf { sc.prep.itemById(it) != null } ?: return Out(st, stay = true)
        val gold = ownGold(sc, st, id) ?: firstGold(sc, id)
        if (gold == st.gold) return Out(st, stay = true)
        return Out(st.copy(gold = gold), pad = if (st.gold != null) Pad.SHUT_DROP else Pad.KEEP)
    }

    // ------------------------------------------------------------------ a tap on empty photo (rows 10 - 13)

    /**
     * ROWS 10, 11 AND 12 -- A TAP ON EMPTY PHOTO, no tool armed:
     *  - an item HELD (row 10): handles, pick and gold all clear;
     *  - a figure gold and NOTHING held (row 11): NOTHING -- "a stray thumb must not lose the dimension the laser is
     *    about to fill". To let it go: tap the mark (held), then empty photo;
     *  - an item picked, not held, nothing gold (row 12): the pick clears.
     * In every case an open keypad stores what was typed and shuts.
     */
    fun empty(st: State): Out = when {
        st.held -> Out(NONE, pad = Pad.SHUT_STORE)
        st.gold != null -> Out(st, pad = Pad.SHUT_STORE)
        else -> Out(NONE, pad = Pad.SHUT_STORE)
    }

    /** A double tap is two taps within this long ([GestureClassifier.DOUBLE_TAP_MS]). */
    const val DOUBLE_MS = GestureClassifier.DOUBLE_TAP_MS

    /** What a tap on empty photo cleared, and when: kept for the double-tap window ([emptied]). */
    class Emptied(val before: State, val atMs: Long)

    /** A tap on empty photo at [now], [before] it: what [doubleTap] puts back if a second tap follows in time. */
    fun emptied(before: State, now: Long): Emptied = Emptied(before, now)

    /**
     * ROW 13 -- A DOUBLE TAP ON EMPTY PHOTO (slice I4): the photo zooms 2x about the point and NOTHING ELSE changes.
     * The classifier reports a tap on the first lift, so the first tap of the double tap has already cleared a held
     * mark at the moment he zooms in to place its handle: the state that tap cleared ([first], within [DOUBLE_MS]) is
     * put back -- as far as the room still allows it ([settle]). A keypad the first tap shut stays shut, and what it
     * stored stays stored. With no first tap in time the state is left as it is.
     */
    fun doubleTap(sc: Scene, st: State, first: Emptied?, now: Long): Out {
        val back = first?.takeIf { now - it.atMs in 0..DOUBLE_MS }?.let { settle(sc, it.before) } ?: st
        return Out(back, zoom = true)
    }

    // ------------------------------------------------------------------ placing and the acts (rows 14 - 17, 24)

    /**
     * ROW 14 -- A TYPE PLACED; A TAPE, A LINE OR AN AREA PLOTTED ([id] the new item): it is picked and held, and at
     * Survey its first unread figure is gold (a Tape has none). The tool then goes off: the screen sets
     * [SurveyPrepPalette.afterPlaced].
     */
    fun placed(sc: Scene, id: String): Out = Out(State(id, mayHold(sc, id), firstGold(sc, id)), pad = Pad.SHUT_STORE)

    /** ROW 15 -- `Duplicate` made the copy [copy]: the copy is picked and held, its first unread figure gold. */
    fun duplicated(sc: Scene, copy: String): Out = placed(sc, copy)

    /** ROW 16 -- `Four corners`, `Rectangle`, `Fine move` on [id]: an act that edits shape or position HOLDS the item,
     *  so a mark the walk reached shows the handles the act needs. The gold is unchanged. */
    fun shaped(sc: Scene, st: State, id: String): Out {
        if (sc.prep.itemById(id) == null) return Out(st)
        return Out(State(id, mayHold(sc, id), ownGold(sc, st, id)))
    }

    /** ROW 17 -- `Lock` or `Unlock` on [id]: picked, NOT held -- a locked item never is, and an unlocked one is not
     *  until it is tapped. The gold is unchanged: a locked mark still takes readings. */
    fun locked(sc: Scene, st: State, id: String): Out {
        if (sc.prep.itemById(id) == null) return Out(st)
        return Out(State(id, false, ownGold(sc, st, id)))
    }

    /** ROW 24 -- THE ITEM [id] DELETED: if it was the picked one, nothing is picked, held or gold (the gold was its),
     *  and what was typed for it is dropped. Any other item's delete changes nothing here. */
    fun deleted(st: State, id: String): Out = if (st.picked == id) Out(NONE, pad = Pad.SHUT_DROP) else Out(st)

    // ------------------------------------------------------------------ a reading lands; the gold walks on (rows 18 - 22)

    /** The gold after the reading under [after] is in: the same item's next unread figure that is LEFT
     *  ([MeasureFlow.stay]), else the face's next unread one ([MeasureWalk.next]); null when the wall is finished. */
    private fun onward(sc: Scene, after: String): String? {
        val o = sc.prep.ownerOf(after) ?: return null
        val z = sc.ab(o.s)
        return MeasureFlow.stay(sc.prep, o.s, z[0], z[1], after, sc.ab) ?: MeasureWalk.next(sc.prep, o.s, z[0], z[1], after)
    }

    /** The gold is [gold] now, come by itself: its owner is picked WITHOUT handles. Null: nothing is left -- the pick
     *  is unchanged, nothing is gold, and the wall-finished note is said. */
    private fun walkedTo(sc: Scene, st: State, gold: String?, pad: Pad = Pad.KEEP): Out {
        val o = gold?.let { sc.prep.ownerOf(it) }
            ?: return Out(State(st.picked, false, null), pad = pad,
                finished = if (MeasureWalk.room(sc.prep, sc.ab).left == 0) Finished.ROOM else Finished.FACE)
        return Out(State(o.id, false, gold), pad = pad, byItself = true)
    }

    /**
     * ROWS 18, 19 AND 20 -- A READING HAS BEEN WRITTEN TO THE GOLD FIGURE [code] (the screen writes it first, then
     * asks), by the laser -- or [typed]: the keypad's Next ▸ stored a figure:
     *  - [busy] (row 20) -- the panel or the Add sheet is open, or a finger is down on a grab: picked, held and gold
     *    all STAY ([Out.waits]), so the picked item never changes under a sheet that holds a draft for it and a handle
     *    is never taken from under a finger; [released] is asked when the sheet closes or the finger lifts;
     *  - it FAILS THE CHECK (row 19; [MeasureFlow.flagged]) and came from the laser: the gold STAYS where it is,
     *    `1234!`, and the handles go. The next reading replaces it; `▸` or a tap on another figure accepts it. So a
     *    double press of the meter cannot quietly put a door's width into its height;
     *  - it passes, or it was TYPED (row 18; a typed figure is deliberate and keeps its `!`): the handles go and the
     *    gold moves on by itself -- to the same item's next unread figure, else the face's next unread one, whose
     *    mark becomes the picked one; with nothing left the pick is unchanged, nothing is gold, and the wall-finished
     *    note is said ([Out.finished]).
     */
    fun landed(sc: Scene, st: State, code: String, busy: Boolean, typed: Boolean = false): Out {
        if (busy) return Out(st, waits = true)
        if (!typed && MeasureFlow.flagged(sc.prep, code, sc.ab)) return Out(st.copy(held = false))
        return walkedTo(sc, st, onward(sc, code))
    }

    /** ROW 20, LATER -- the sheet closed or the finger lifted after a reading landed on [code] while it was open or
     *  down: row 18 or row 19 applies now. If the gold has since been moved off [code] by his own tap, nothing does. */
    fun released(sc: Scene, st: State, code: String): Out = if (st.gold != code) Out(st) else landed(sc, st, code, busy = false)

    /**
     * ROWS 21 AND 22 -- `▸`, on the open face [face] (and the keypad's Next ▸ when nothing was typed):
     *  - a figure gold (row 21): the face's next unread figure AFTER it, accepting a `!` -- the one skipped comes
     *    round again, and the only one left is its own next; nothing left: the gold is cleared and the wall-finished
     *    note is said;
     *  - nothing gold (row 22): the picked item's first unread figure, else the face's first unread one.
     * Its owner is picked WITHOUT handles. Where nothing may be gold, or with nothing to go to, nothing changes.
     */
    fun next(sc: Scene, st: State, face: String): Out {
        if (!goldOk(sc) || !StepRoute.isFace(face)) return Out(st)
        val z = sc.ab(face)
        val g = st.gold
        if (g != null) return walkedTo(sc, st, MeasureWalk.next(sc.prep, face, z[0], z[1], g))
        val to = st.picked?.let { firstGold(sc, it) } ?: MeasureWalk.next(sc.prep, face, z[0], z[1], null) ?: return Out(st)
        return walkedTo(sc, st, to)
    }

    /** The wall-finished note: "Front wall · all 9 measured ✓" for the face [face], or "Room measured · all 31 ✓" when
     *  it was the room's last ([MeasureText]). The face never changes by itself: the tabs' amber counts show where
     *  figures are left. */
    fun finishedText(tx: Tx, sc: Scene, face: String, f: Finished): String = when (f) {
        Finished.ROOM -> MeasureText.roomDone(tx, MeasureWalk.room(sc.prep, sc.ab).total)
        Finished.FACE -> sc.ab(face).let { z -> MeasureText.faceDone(tx, face, MeasureWalk.count(sc.prep, face, z[0], z[1]).total) }
    }

    // ------------------------------------------------------------------ undo; a face, a step, the Plan (rows 23, 25 - 28)

    /**
     * ROW 23 -- ↶ ([sc] holds the room as the undo left it): the gold is kept while its owner is still there and it
     * still takes a reading, and its owner is then the picked item; else nothing is picked. Never held. ↶ does not
     * undo a reading.
     */
    fun undone(sc: Scene, st: State): Out {
        val g = st.gold?.takeIf { goldOk(sc) && MeasureFlow.takes(sc.prep, it, sc.stage, sc.ab) }
        val o = g?.let { sc.prep.ownerOf(it) } ?: return Out(NONE, pad = Pad.SHUT_STORE)
        return Out(State(o.id, false, g))
    }

    /**
     * ROWS 25 AND 26 -- A FACE TAB (OR THE PLAN) PRESSED, the screen on tab [from]: ANOTHER tab clears everything --
     * nothing is picked, held or gold on entry: he may be about to mark; `▸` or a tap on a figure starts. THE OPEN
     * FACE'S OWN TAB IS NO CHANGE AT ALL ([Out.stay]): restarting there dropped the dimension he had chosen, and his
     * next laser shot landed on another one. (Only a face stays: the Plan's own tab is what it always was.)
     */
    fun tab(st: State, from: String, to: String): Out =
        if (from == to && StepRoute.isFace(to)) Out(st, stay = true) else Out(NONE, pad = Pad.SHUT_STORE)

    /**
     * ROWS 25, 26 AND 28 -- A STEP ASKED FOR ([to]; by its circle, or a thumb under the strip routed to it), the
     * screen on step [from] of the tab [face], and [ask] what the route answers ([PhaseTools.routeAsk]):
     *  - not [PhaseTools.Ask.OPEN] (row 28: 2 · Mark on a face whose four lines do not close, refused in every phase;
     *    or a step the phase hides): NO CHANGE AT ALL -- the screen says [PhaseTools.routeRefusal] for the first;
     *  - the open step itself (row 26): no change at all;
     *  - another step (row 25): nothing is picked, held or gold on entry.
     */
    fun step(st: State, from: String, to: String, face: String, ask: PhaseTools.Ask): Out = when {
        ask != PhaseTools.Ask.OPEN -> Out(st, stay = true)
        from == to && StepRoute.isFace(face) -> Out(st, stay = true)
        else -> Out(NONE, pad = Pad.SHUT_STORE)
    }

    // ------------------------------------------------------------------ what a held item shows

    /** The item [id] shows its handles -- eight tabs, four round corners, a Line's two ends: only while it is HELD. */
    fun handles(st: State, id: String): Boolean = st.held && st.picked == id

    /*
     * NOTHING ELSE HANGS ON THE PICK UNDER THE PHOTO (port 63 slice I2b). Slice I1 kept, for its one build, the floating
     * trash beside the held item (`trash`) and the picked item's row under the photo (`row`: its label, its nudges, its
     * padlock). Both are gone: the picked item's CODE CHIP ([SurveyPrepTouch.codeChip]) and a long press ([longPress])
     * open the panel that slides up ([MarkSheet.openPanel]) for ANY picked item, held or not, at every stage -- so a
     * mark the phase holds read-only, a locked one and a Line the walk reached each still reach their panel, which is
     * what the review of I1 (findings 1 and 6) asked of the row -- and Delete, the padlock and the nudges (Fine move)
     * are the panel's acts ([MarkSheet.acts]).
     */
}
