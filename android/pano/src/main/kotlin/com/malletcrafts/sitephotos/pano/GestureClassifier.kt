package com.malletcrafts.sitephotos.pano

import kotlin.math.hypot

/**
 * WHAT ONE FINGER ON THE SURVEY-PREP PHOTO IS DOING (v52 slice 7). The photo's single gesture loop was
 * rewritten for the palette (Amit, 13 Oct 2026 17:22 IST, on the tools: "cannot go from one tool to other; not
 * a very useful design for tools"), and the loop is the riskiest code in the port because only the device runs
 * it. So the deciding is HERE, pure and tested, and SpPhoto's pointerInput only feeds it what the system
 * reports and acts on what it answers.
 *
 * Fed [down], [move], [up], [cancel] and [timeout] with timestamps (ms, one clock) and positions (px); answers
 * a list of [Event]s:
 *  - [Event.Down] on the first finger down -- where the hit test runs (a mark is picked on the press, as sp.js
 *    picks on pointerdown);
 *  - [Event.Drag] once the finger has moved more than [slop] from where it went down, then on every move
 *    ([Event.Drag.first] on the first), with the whole offset from the down point; [Event.DragEnd] on lift;
 *  - [Event.Tap] on a lift within [slop], before [longMs]; [Event.DoubleTap] instead when the press began within
 *    [doubleMs] of the last tap's lift and within [doubleDist] of it (a third tap is a Tap again);
 *  - [Event.LongPress] once a press has stayed within [slop] for [longMs] -- reported by [timeout], which the
 *    caller feeds at [deadline]; a press that lifts after it is not a tap; a finger that moves on after it
 *    drags (the caller decides what a long press then means: slice 8's menu);
 *  - [Event.Cancel] when a SECOND finger comes down (the pinch takes over) or the system cancels: nothing
 *    more is reported until every finger is up, so no tap or drag end follows a cancelled gesture.
 * A press that GRABBED something ([grabbed], told by the caller after [Event.Down]) drags from the first pixel,
 * as sp.js moves a grab on every pointermove: within the slop it still reports [Event.Drag] on every move, while
 * tap-versus-drag and the long press keep the slop -- a fine adjustment under the loupe is never a dead zone.
 * Timings as the prototype's: long press 500 ms (sp.js 929), double tap 450 ms within 24 dp (sp.js 531).
 */
class GestureClassifier(
    /** Movement under this, px, is still a press (the system's touch slop). */
    private val slop: Double,
    /** A second tap this close to the first, px (24 dp), can make a double tap. */
    private val doubleDist: Double,
    private val longMs: Long = LONG_PRESS_MS,
    private val doubleMs: Long = DOUBLE_TAP_MS,
) {
    companion object {
        const val LONG_PRESS_MS = 500L
        const val DOUBLE_TAP_MS = 450L
        /** The double tap's reach, dp. */
        const val DOUBLE_TAP_DP = 24.0
    }

    sealed class Event {
        data class Down(val x: Double, val y: Double) : Event()
        /** [x], [y] where the finger is; [dx], [dy] how far from where it went down. */
        data class Drag(val x: Double, val y: Double, val dx: Double, val dy: Double, val first: Boolean) : Event()
        data class DragEnd(val x: Double, val y: Double) : Event()
        /** At the DOWN point (sp.js places where the finger went down). */
        data class Tap(val x: Double, val y: Double) : Event()
        data class DoubleTap(val x: Double, val y: Double) : Event()
        data class LongPress(val x: Double, val y: Double) : Event()
        object Cancel : Event() { override fun toString() = "Cancel" }
    }

    private enum class State { IDLE, PRESSED, DRAGGING, SPENT }

    private var state = State.IDLE
    private var x0 = 0.0; private var y0 = 0.0; private var t0 = 0L
    private var long = false
    private var double = false
    /** The press grabbed a line, a mark or a tab: its moves are reported from the first pixel. */
    private var grab = false
    /** A Drag was already reported for this press (within the slop, for a grab). */
    private var dragged = false
    /** The last tap's point and lift time, while a double tap is still possible. */
    private var lastTap: DoubleArray? = null; private var lastTapT = 0L

    /** When [timeout] will report a long press if nothing else happens first; null when none is pending. */
    fun deadline(): Long? = if (state == State.PRESSED && !long) t0 + longMs else null

    /** A finger down; [pointers] how many are down now, this one included. */
    fun down(x: Double, y: Double, t: Long, pointers: Int = 1): List<Event> {
        if (pointers >= 2 || state != State.IDLE) return second()
        val lt = lastTap
        double = lt != null && t - lastTapT <= doubleMs && hypot(x - lt[0], y - lt[1]) < doubleDist
        if (!double) lastTap = null
        state = State.PRESSED; x0 = x; y0 = y; t0 = t; long = false; grab = false; dragged = false
        return listOf(Event.Down(x, y))
    }

    /** The press (still down) grabbed something: report its moves at once, without waiting for the slop. */
    fun grabbed() { if (state == State.PRESSED || state == State.DRAGGING) grab = true }

    /** The finger(s) moved; [pointers] how many are down. Two or more cancel. */
    fun move(x: Double, y: Double, t: Long, pointers: Int = 1): List<Event> {
        if (pointers >= 2) return second()
        return when (state) {
            State.PRESSED -> {
                if (hypot(x - x0, y - y0) > slop) {
                    state = State.DRAGGING; lastTap = null
                    val first = !dragged; dragged = true
                    listOf(Event.Drag(x, y, x - x0, y - y0, first))
                } else if (grab && (x != x0 || y != y0)) {
                    // a grab follows the finger inside the slop; the press is still a press (tap, long press)
                    val first = !dragged; dragged = true
                    listOf<Event>(Event.Drag(x, y, x - x0, y - y0, first)) + timeout(t)
                } else timeout(t)
            }
            State.DRAGGING -> listOf(Event.Drag(x, y, x - x0, y - y0, false))
            else -> emptyList()
        }
    }

    /** The clock reached [t] with nothing else happening. */
    fun timeout(t: Long): List<Event> {
        if (state != State.PRESSED || long || t - t0 < longMs) return emptyList()
        long = true; lastTap = null
        return listOf(Event.LongPress(x0, y0))
    }

    /** A finger lifted at ([x], [y]); [pointers] how many are still down. */
    fun up(x: Double, y: Double, t: Long, pointers: Int = 0): List<Event> {
        if (pointers > 0) return emptyList()
        val was = state; state = State.IDLE
        return when (was) {
            State.PRESSED -> {
                // a press that lifts past its long-press time is a long press, whether or not the clock was fed
                val held = timeoutOnLift(t)
                if (long) { lastTap = null; held }
                else if (double) { lastTap = null; listOf(Event.DoubleTap(x0, y0)) }
                else { lastTap = doubleArrayOf(x0, y0); lastTapT = t; listOf(Event.Tap(x0, y0)) }
            }
            State.DRAGGING -> listOf(Event.DragEnd(x, y))
            else -> emptyList()
        }
    }

    /** The system took the gesture away. */
    fun cancel(): List<Event> {
        val was = state; state = State.IDLE; lastTap = null
        return if (was == State.PRESSED || was == State.DRAGGING) listOf(Event.Cancel) else emptyList()
    }

    /** Forget everything: the press was handled elsewhere (the floating trash, a step of the strip). */
    fun reset() { state = State.IDLE; lastTap = null; long = false; double = false; grab = false; dragged = false }

    private fun timeoutOnLift(t: Long): List<Event> {
        if (long || t - t0 < longMs) return emptyList()
        long = true
        return listOf(Event.LongPress(x0, y0))
    }

    private fun second(): List<Event> {
        lastTap = null
        return when (state) {
            State.PRESSED, State.DRAGGING -> { state = State.SPENT; listOf(Event.Cancel) }
            else -> emptyList()
        }
    }
}
