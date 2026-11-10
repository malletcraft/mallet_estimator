package com.malletcrafts.interiorestimate.pano

/**
 * THE STEP MACHINE (port 61c §31) — the engine's run state with nothing drawn and nothing
 * Android: indices, busy, the counter, the auto-advance predicate over a host-fed [TourWorld],
 * and the log fields the tests read. The HOST (the app, MainActivity's tour host) executes a
 * step's setup moves and then calls [shown]; the overlay's poll calls [doneNow] every 150 ms
 * and advances on true. `Next` ALWAYS works and never writes data; `Back` is disabled on step
 * 1 and while a move runs; every step's setup is a POSITION, run on Next, Back and jumps
 * alike, so Back always lands right.
 *
 * THE TAP RULE (§31 critic finding 4, from the engine's own line
 * `if(d==='tap') return run.tapAt>run.shownAt&&Date.now()-run.tapAt>=320`): any tap inside
 * the spotlight AFTER the step showed qualifies, and the step advances once 320 ms have
 * passed SINCE THE TAP — a settle delay so the app's response to the tap is seen before the
 * bubble moves — never a demand that the tap itself come 320 ms after show.
 */
class TourMachine(val flow: TourFlow, startAt: Long) {

    /** Where the machine stands after a call that may move it. */
    sealed class Go {
        /** Show step [i]: the host runs its setup, then calls [shown]. */
        data class Show(val i: Int) : Go()
        /** The tour ended (`Done` on the last step, or Skip / back / Home). */
        object End : Go()
        /** Nothing to do (busy, already ended, or Back on step 1). */
        object None : Go()
    }

    var i: Int = 0
        private set

    /** True from [begin] until [shown]: a move is running, Next and Back take no tap. */
    var busy: Boolean = true
        private set

    /** How the tour ended — the engine's log `how`: "done" | "skip"; null while it runs. */
    var ended: String? = null
        private set

    var shownAt: Long = 0L
        private set
    var tapAt: Long = 0L
        private set

    /** The mark count when the step showed; `marks:+n` advances at marks0 + n. */
    var marks0: Int = 0
        private set

    /** Every step index shown, in order (the log's `steps` is its size). */
    val seen = mutableListOf<Int>()

    /** Restore-writes the host made at the end; a Next-only run records 0. */
    var wrote: Int = 0

    val t0: Long = startAt

    val step: TourStep get() = flow.steps[i]
    val n: Int get() = flow.steps.size
    val counter: String get() = "${i + 1}/$n"
    val last: Boolean get() = i == n - 1

    /** The host is about to run step [at]'s setup (start, Next, Back alike). */
    fun begin(at: Int) {
        if (ended != null) return
        i = at.coerceIn(0, n - 1)
        busy = true
    }

    /** The step is on screen: its setup ran and its anchor resolved (or timed out). */
    fun shown(markCount: Int, now: Long) {
        if (ended != null) return
        busy = false
        shownAt = now
        tapAt = 0L
        marks0 = markCount
        seen.add(i)
    }

    /** A tap landed inside the spotlight (the host feeds it when it can observe one). */
    fun tap(now: Long) {
        if (!busy && ended == null) tapAt = now
    }

    /** The engine's doneNow over the host-fed world; false while busy or ended. */
    fun doneNow(w: TourWorld, now: Long): Boolean {
        if (busy || ended != null) return false
        val d = step.done ?: return false
        return when (d) {
            is TourDone.Tap -> tapAt > shownAt && now - tapAt >= TAP_SETTLE_MS
            is TourDone.Exists -> d.anchor in w.anchors
            is TourDone.Gone -> d.anchor !in w.anchors
            is TourDone.Screen -> w.screen == d.screen
            is TourDone.Phase -> w.phase == d.n
            is TourDone.Tool -> w.spOpen && w.tool == d.tool
            is TourDone.Tab -> w.spOpen && (if (d.tab == "360") w.mode == "360" else w.tab == d.tab)
            is TourDone.Mode -> w.spOpen && w.mode == d.mode
            is TourDone.Marks -> w.spOpen && w.markCount >= marks0 + d.plus
        }
    }

    /** Next (the button, or an auto-advance): the next step, or the end on the last. */
    fun next(): Go {
        if (busy || ended != null) return Go.None
        if (last) { ended = "done"; return Go.End }
        begin(i + 1)
        return Go.Show(i)
    }

    /** Back: disabled on step 1 and while a move runs. */
    fun back(): Go {
        if (busy || ended != null || i == 0) return Go.None
        begin(i - 1)
        return Go.Show(i)
    }

    /** Skip tour, or a back / Home press while the tour runs. */
    fun skip() {
        if (ended == null) ended = "skip"
    }

    companion object {
        /** The settle delay after a qualifying tap (the engine's 320). */
        const val TAP_SETTLE_MS = 320L

        /** The engine's poll interval and anchor wait, for the hosts that drive this. */
        const val POLL_MS = 150L
        const val WAIT_MS = 4000L
    }
}

/**
 * Where the app is, as the host reports it every poll: the engine's `screen()`, survey
 * prep's tab / step / mark count, the demo's phase and the anchors resolvable right now.
 */
data class TourWorld(
    val screen: String? = null,
    val phase: Int? = null,
    val spOpen: Boolean = false,
    val tab: String? = null,
    val mode: String? = null,
    val tool: String? = null,
    val markCount: Int = 0,
    val anchors: Set<String> = emptySet(),
)
