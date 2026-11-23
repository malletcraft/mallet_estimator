package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Msg

/**
 * A LASER READING ARRIVES (port 63 slice I1a, 16 Nov 2026; DESIGN-imagemeter §4.3 and §4.4). The meter's own button
 * takes the reading -- nothing in 2 · Mark fires it -- so the phone learns of a reading only when it lands, with his
 * eyes on the wall. Where it goes, whether the gold then moves, what the hand feels and what the note says are
 * decided HERE, pure and tested; the screen (compiled only by CI) writes what it is told and draws.
 *
 * TWO QUESTIONS, IN ORDER:
 *  1. [route] -- WHERE DOES IT GO? Into an armed reference box of 1 · Lines ([SurveyRef.route], unchanged); onto the
 *     gold figure; or NOWHERE -- refused, said, not stored: a reference box that was left, after Survey, on the 360,
 *     and with nothing gold. Nothing is held back for a later tap, and nothing lands on a figure he was not looking at.
 *  2. [after] -- the screen has written it to the gold figure (its one `reading()`): DOES THE GOLD MOVE? It moves on
 *     by itself when the reading passes the check ([Pick.landed], row 18); it STAYS when the reading fails it (row
 *     19), and while a sheet is open or a finger is on a grab (row 20).
 *
 * THREE HAPTICS, so each can be told by the hand ([Haptic]): one tick when a reading lands and passes; a double buzz
 * when it lands and fails the check; ONE LONG BUZZ WHEN IT IS THROWN AWAY -- "his eyes are on the wall when he shoots;
 * a reading thrown away must be felt" -- and that refusal's note STAYS until the next touch, where every other note
 * goes after 2.6 s ([Route.Refused.stays]).
 */
object Landing {

    /** What the hand feels. */
    enum class Haptic {
        /** The reading landed on the gold figure and passed: one tick, as today. */
        TICK,
        /** It landed and failed the check -- it is stored, with its `!`, and the gold has not moved: a double buzz. */
        DOUBLE,
        /** It was thrown away: one long buzz. */
        LONG,
    }

    /** One pulse of what the hand feels: when it starts, ms after the first ([atMs]), and whether it is the STRONG
     *  buzz a long press gives ([strong]) or the light tick a handle gives. */
    class Pulse(val atMs: Long, val strong: Boolean)

    /** Between the two ticks of a [Haptic.DOUBLE], ms: far enough apart to be felt as two. */
    const val DOUBLE_GAP_MS = 140L

    /**
     * WHAT EACH [Haptic] IS, PULSE BY PULSE (port 63 slice I1b): the screen plays these and decides nothing -- one light
     * tick ([Haptic.TICK]: landed and passed, as the only haptic has always been); two light ticks [DOUBLE_GAP_MS] apart
     * ([Haptic.DOUBLE]: landed and failed the check); one strong buzz ([Haptic.LONG]: thrown away). No two are alike,
     * so the hand tells them apart with the eyes on the wall.
     */
    fun pulses(h: Haptic): List<Pulse> = when (h) {
        Haptic.TICK -> listOf(Pulse(0L, false))
        Haptic.DOUBLE -> listOf(Pulse(0L, false), Pulse(DOUBLE_GAP_MS, false))
        Haptic.LONG -> listOf(Pulse(0L, true))
    }

    /** The plate that has just taken a reading keeps a gold ring this long while the gold moves on, so the eye sees
     *  where the reading went (DESIGN-imagemeter §2.2). */
    const val RING_MS = 1000L

    /** Where a reading goes. */
    sealed class Route {
        /** Into reference box [k] of face [s], its W or H: never a reading key ([SurveyRef.Route.Ref], unchanged). */
        class Ref(val s: String, val k: String) : Route()
        /** Onto the gold figure, the reading key [code]: the screen writes it, drops what was typed on the keypad,
         *  shuts the keypad, and asks [after]. */
        class Gold(val code: String) : Route()
        /** THROWN AWAY: not stored anywhere. The screen says [text], gives [haptic], and -- [stays] -- keeps the note
         *  up until the next touch. */
        class Refused(val text: Msg, val haptic: Haptic = Haptic.LONG, val stays: Boolean = true) : Route()
    }

    /**
     * WHERE A READING OF [mm] GOES, arriving at [now] ms with [shot] the reference box's D2 that is out (null: none)
     * and [st] what is picked, held and gold:
     *  - [SurveyRef.route] first, exactly as before: into the armed reference box; or, the box left within its five
     *    seconds, set aside with [SurveyRef.leftSaid];
     *  - after Survey: refused in the phase's own words ([PhaseTools.readingRefusal]);
     *  - on 3 · 360: refused with [MeasureText.on360] -- nothing is measured there;
     *  - NOTHING GOLD (or a gold whose reading is no longer asked): refused with the existing
     *    `sp.msg.d2_x_tap_measurement`, "D2 1203 mm — tap a measurement first, then read again";
     *  - else onto the gold figure. A locked mark's figure takes it: the lock holds its position, not its figures.
     * Every refusal is felt and its note stays ([Route.Refused]). The caller clears [shot] whatever comes back.
     */
    fun route(shot: SurveyRef.Shot?, sc: Pick.Scene, st: Pick.State, mm: Int, now: Long): Route {
        when (val r = SurveyRef.route(shot, sc.prep, mm, now)) {
            is SurveyRef.Route.Ref -> return Route.Ref(r.s, r.k)
            is SurveyRef.Route.Drop -> return Route.Refused({ tx -> SurveyRef.leftSaid(tx, r.mm) })
            SurveyRef.Route.Normal -> Unit
        }
        if (!MeasureFlow.canRead(sc.stage)) return Route.Refused({ tx -> PhaseTools.readingRefusal(tx, sc.stage).orEmpty() })
        if (sc.step == StepRoute.PANO) return Route.Refused({ tx -> MeasureText.on360(tx, mm) })
        val gold = st.gold?.takeIf { Pick.goldOk(sc) && MeasureFlow.takes(sc.prep, it, sc.stage, sc.ab) }
            ?: return Route.Refused({ tx -> tx.sp_msg_d2_x_tap_measurement(mm.toString()) })
        return Route.Gold(gold)
    }

    /**
     * WHAT FOLLOWS A READING THAT IS IN. [state]: picked, held and gold now. [pad]: what the keypad does. [haptic]:
     * what the hand feels (null: nothing). [note]: what the message line says (null: nothing) -- the check's sentence,
     * with no figure, or the wall-finished note. [ring]: the reading key whose plate rings gold for [RING_MS] (null:
     * none). [byItself]: the gold moved by itself -- the photo pans if the new gold figure is off screen
     * ([MeasureFlow.panTo]). [waits]: the gold was held back by a sheet or a finger -- the screen keeps [ring]'s key
     * and asks [released] when the sheet closes or the finger lifts.
     */
    class After(val state: Pick.State, val pad: Pick.Pad, val haptic: Haptic?, val note: Msg?, val ring: String?,
                val byItself: Boolean, val waits: Boolean)

    /** The note after a reading under [code]: the check's sentence first -- a reading with `!` says the gross guard's,
     *  and one that completes a four-corner mark whose sides do not close says THAT, naming the sloping side
     *  ([MeasureFlow.guardMsg]; port 63 slice Q2a) -- else, the wall finished, that note.
     *  A [Msg]: the screen renders it at once, with the language on then (its `say` keeps the words). */
    private fun noteOf(sc: Pick.Scene, code: String, out: Pick.Out): Msg? {
        MeasureFlow.guardMsg(sc.prep, code, sc.ab)?.let { return it }
        val f = out.finished ?: return null
        val face = sc.prep.ownerOf(code)?.s ?: return null
        return { tx -> Pick.finishedText(tx, sc, face, f) }
    }

    /**
     * THE LASER'S READING HAS BEEN WRITTEN TO THE GOLD FIGURE [code] -- what follows ([Pick.landed], rows 18 - 20):
     * typed digits are dropped and the keypad shuts ([Pick.Pad.SHUT_DROP]); one TICK when it passes the check, a
     * DOUBLE buzz when it fails it ([MeasureFlow.flagged]); the note says the check's sentence
     * ([MeasureText.checkReading]: no figure) or, the wall finished, the wall-finished note; the plate rings gold.
     * [busy]: the panel or the Add sheet is open, or a finger is down on a grab -- the reading is in and felt, and the
     * gold WAITS ([After.waits]).
     */
    fun after(sc: Pick.Scene, st: Pick.State, code: String, busy: Boolean): After {
        val out = Pick.landed(sc, st, code, busy)
        val haptic = if (MeasureFlow.flagged(sc.prep, code, sc.ab)) Haptic.DOUBLE else Haptic.TICK
        return After(out.state, Pick.Pad.SHUT_DROP, haptic, noteOf(sc, code, out), code, out.byItself, out.waits)
    }

    /**
     * THE SHEET CLOSED, OR THE FINGER LIFTED, after a reading landed on [code] while it was open or down
     * ([After.waits]): the gold moves on now, or stays on a reading that fails the check ([Pick.released]). Nothing
     * is felt again and no plate rings again; the wall-finished note is said if the wall is finished. The keypad is
     * left as it is.
     */
    fun released(sc: Pick.Scene, st: Pick.State, code: String): After {
        val out = Pick.released(sc, st, code)
        return After(out.state, Pick.Pad.KEEP, null, if (out.finished != null) noteOf(sc, code, out) else null, null, out.byItself, false)
    }

    /**
     * THE KEYPAD'S NEXT ▸ STORED A TYPED FIGURE under [code] (DESIGN-imagemeter §5): the gold moves as after a reading
     * that passes -- A TYPED FIGURE IS DELIBERATE, so one that fails the check keeps its `!`, the note says so, and
     * the gold still moves. The keypad stays up; nothing is felt; no plate rings. (Next ▸ with nothing typed writes
     * nothing: it is `▸`, [Pick.next].)
     */
    fun keyed(sc: Pick.Scene, st: Pick.State, code: String): After {
        val out = Pick.landed(sc, st, code, busy = false, typed = true)
        return After(out.state, Pick.Pad.KEEP, null, noteOf(sc, code, out), null, out.byItself, false)
    }
}
