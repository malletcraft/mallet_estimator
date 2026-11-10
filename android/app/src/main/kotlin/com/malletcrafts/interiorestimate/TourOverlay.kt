package com.malletcrafts.interiorestimate

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malletcrafts.interiorestimate.pano.SurveyPrep
import com.malletcrafts.interiorestimate.pano.TourDemo
import com.malletcrafts.interiorestimate.pano.TourFlow
import com.malletcrafts.interiorestimate.pano.TourMachine
import com.malletcrafts.interiorestimate.pano.TourPlace
import com.malletcrafts.interiorestimate.pano.TourStep
import com.malletcrafts.interiorestimate.pano.TourWorld
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * THE GUIDED TOURS' OVERLAY AND STATE (port 61c §31) — the engine's run (window.T55) on the
 * phone. [TourState] is the one holder: the running tour, the anchor registry, the world the
 * machine's predicates read, and the two bridges (the survey window's movers, the app's end
 * hook). [TourOverlay] draws the scrim, the rounded spotlight and the bubble; it is composed
 * TWICE — the last child of AppScreen's root Box (the activity window) and the last child of
 * SurveyPrepDialog's root Box (survey prep is a Dialog, its OWN window, which no overlay in
 * the activity can cover and whose anchors live in its own coordinates) — and only the host
 * the step is in draws. NOTHING OF THE TOUR EXISTS WHEN NO TOUR RUNS: both hosts return at
 * once on run == null.
 *
 * Two narrowings against the prototype, recorded in §31's build notes: the reveal (scrolling
 * a target into view in its own list) is not in this build — a target off screen leaves the
 * bubble centred with no spotlight, the engine's own missing-anchor shape — and no host feeds
 * [TourMachine.tap] yet, because no SHIPPED step carries a `done: tap` rule (they are all in
 * blocked flows); the machine carries the rule, tested, for the flow that first needs it.
 */
object TourState {

    /** The activity window: Browse, the room screen, Work / Queue, the Help screen. */
    const val HOST_TREE = 0

    /** Survey prep's own Dialog window. */
    const val HOST_SURVEY = 1

    /** One running tour: the machine, and the demo snapshot its end restores. */
    class Run internal constructor(val flow: TourFlow, val machine: TourMachine, val snap: TourSeed.Snap) {
        /** Bumped on every step change, so the host effect runs the new step's setup. */
        var stepSeq by mutableStateOf(0)
            internal set
    }

    var run by mutableStateOf<Run?>(null)
        private set

    /** name -> (host, rect in that window's root coordinates). Last write wins; disposal removes. */
    val anchors = mutableStateMapOf<String, Pair<Int, Rect>>()

    // ---- the world, host-fed (the engine's screen() and survey-prep reads)
    var screen by mutableStateOf<String?>(null)

    /** The demo project's phase, PHONE-LOCAL (ch55's approved exception); a tour's `phase` move sets it. */
    var demoPhase by mutableStateOf(0)

    var spOpen by mutableStateOf(false)
        private set
    var spTab by mutableStateOf<String?>(null)
        private set
    var spMode by mutableStateOf<String?>(null)
        private set
    var spMarks by mutableStateOf(0)
        private set

    /** The survey window's mover, registered while the dialog is up: (tab?, mode?). */
    internal var surveyMove: ((String?, String?) -> Unit)? = null

    /** The app's end hook (restore the demo, return to Help), registered by AppScreenBody. */
    internal var onEnd: ((Run, String) -> Unit)? = null

    /** Which host draws the overlay now: the dialog covers the activity whenever it is up. */
    fun activeHost(): Int = if (spOpen) HOST_SURVEY else HOST_TREE

    fun world(): TourWorld = TourWorld(
        screen = if (spOpen) "survey" else screen,
        phase = demoPhase,
        spOpen = spOpen, tab = spTab, mode = spMode,
        markCount = spMarks,
        anchors = anchors.keys.toSet())

    fun anchorAt(host: Int, name: String?): Rect? =
        name?.let { anchors[it] }?.takeIf { it.first == host }?.second

    internal fun sp(open: Boolean, tab: String? = null, mode: String? = null, marks: Int = 0) {
        spOpen = open; spTab = tab; spMode = mode; spMarks = marks
    }

    /** Start a tour (the Help screen's Start). A tour already running is skipped first. */
    fun start(flow: TourFlow, snap: TourSeed.Snap) {
        if (run != null) skip()
        val r = Run(flow, TourMachine(flow, System.currentTimeMillis()), snap)
        r.machine.begin(0)
        run = r
    }

    /** Next — the button, or the poll's auto-advance. `Done` on the last step ends the tour. */
    fun next() {
        val r = run ?: return
        when (r.machine.next()) {
            is TourMachine.Go.Show -> r.stepSeq++
            TourMachine.Go.End -> end(r, "done")
            TourMachine.Go.None -> {}
        }
    }

    fun back() {
        val r = run ?: return
        if (r.machine.back() is TourMachine.Go.Show) r.stepSeq++
    }

    /** Skip tour, ◁ or Home while a tour runs: every end restores and returns to Help. */
    fun skip() {
        val r = run ?: return
        r.machine.skip()
        end(r, "skip")
    }

    private fun end(r: Run, how: String) {
        if (run !== r) return
        run = null
        onEnd?.invoke(r, how)
    }
}

/**
 * Tags one composable as a tour anchor, under the window that holds it. Rects are re-written
 * on every layout pass (an element kept from the last frame is a stale answer) and removed
 * when the composable leaves, so a gone target reads as a missing anchor, never a stale rect.
 */
@Suppress("ModifierFactoryUnreferencedReceiver")
fun Modifier.tourAnchor(name: String?, host: Int = TourState.HOST_TREE): Modifier =
    if (name == null) this
    else androidx.compose.ui.composed {
        DisposableEffect(name) { onDispose { TourState.anchors.remove(name) } }
        onGloballyPositioned { TourState.anchors[name] = host to it.boundsInRoot() }
    }

/**
 * Survey prep's half of the bridge, composed inside SurveyPrepDialog: reports the dialog's
 * tab / step / mark count into the world every frame it changes, and registers the mover the
 * tour host drives `tab` / `mode` setups through. Unregistered — and the world closed — the
 * moment the dialog leaves.
 */
@Composable
internal fun TourBindSurvey(tab: String, mode: String, marks: Int, onTab: (String) -> Unit, onMode: (String) -> Unit) {
    SideEffect { TourState.sp(open = true, tab = tab, mode = mode, marks = marks) }
    DisposableEffect(Unit) {
        TourState.surveyMove = { t, m ->
            if (t != null) onTab(t)
            if (m != null) onMode(m)
        }
        onDispose {
            TourState.surveyMove = null
            TourState.sp(open = false)
        }
    }
}

// the scrim and the card, the prototype's t55 palette on survey prep's dark ground
private val TOUR_SCRIM = Color(0xB3000000)
private val TOUR_CARD = Color(0xF21E2219)
private val TOUR_EDGE = Color(0xFF4A4F42)
private val TOUR_FG = Color.White
private val TOUR_MUTED = Color(0xFFABB1A2)
private val TOUR_GOLD = Color(0xFFD9B64A)
private val TOUR_DIM = Color(0xFF6E7466)

@Composable
fun TourOverlay(host: Int) {
    val r = TourState.run ?: return
    if (TourState.activeHost() != host) return
    val tx = LocalTx.current
    val m = r.machine
    val seq = r.stepSeq
    val step = m.step

    // ◁ while a tour runs = Skip tour, caught before the app sees it (this handler is
    // composed after the screen's own, so it wins while the overlay is up).
    BackHandler(true) { TourState.skip() }
    // Home or the task switcher = Skip: Android gives no Home intercept, and a tour must
    // never outlive the screen it lit.
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_STOP) TourState.skip()
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    // the engine's 150 ms poll: the step's done rule over the world, while the step shows
    LaunchedEffect(r, seq) {
        while (true) {
            delay(TourMachine.POLL_MS)
            if (TourState.run !== r) return@LaunchedEffect
            if (!m.busy && m.doneNow(TourState.world(), System.currentTimeMillis())) {
                TourState.next()
                return@LaunchedEffect
            }
        }
    }

    var myBounds by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current.density
    SubcomposeLayout(
        Modifier.fillMaxSize().onGloballyPositioned { myBounds = it.boundsInRoot() }
    ) { constraints ->
        val d = density
        val frame = TourPlace.R(0f, 0f, constraints.maxWidth / d, constraints.maxHeight / d)
        val land = frame.w > frame.h
        // the anchor, re-resolved every frame from the registry (never a kept rect)
        val at = TourState.anchorAt(host, step.anchor)
        val target = at
            ?.let { TourPlace.R((it.left - myBounds.left) / d, (it.top - myBounds.top) / d, it.width / d, it.height / d) }
            ?.takeIf { it.w > 0f && it.h > 0f }
        var hole = target?.let { TourPlace.hole(frame, it) }
        val bw = TourPlace.bubbleWidth(frame.w, land)
        // the bubble's text first: its height decides where it goes, and whether the hole is trimmed
        var slot = 0
        val need: (Float) -> Float = { w ->
            val px = max(1, (w * d).roundToInt())
            val p = subcompose("measure" + slot++) { TourBubble(tx, m, hole != null, step, scroll = false) }
                .first().measure(Constraints(minWidth = px, maxWidth = px))
            p.height / d
        }
        val place = TourPlace.place(frame, hole, bw, land, need)
        if (place.trim != null) hole = place.trim
        val bubbleW = max(1, (place.w * d).roundToInt())
        val bubbleH = max(1, (place.h * d).roundToInt())
        val bubble = subcompose("bubble") { TourBubble(tx, m, hole != null, step, scroll = place.clip) }
            .first().measure(Constraints(minWidth = bubbleW, maxWidth = bubbleW, maxHeight = bubbleH))
        val scrim = subcompose("scrim") { TourScrim(frame, hole, look = step.mode == "look") }
            .first().measure(Constraints.fixed(constraints.maxWidth, constraints.maxHeight))
        layout(constraints.maxWidth, constraints.maxHeight) {
            scrim.place(0, 0)
            bubble.place((place.x * d).roundToInt(), (place.y * d).roundToInt())
        }
    }
}

/** Eats every touch: nothing under a block reaches the app (the prototype's t55blk). */
private fun Modifier.tourConsume(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}

/**
 * The dark scrim with a rounded SPOTLIGHT over the target: one clear-blend hole for the eye,
 * four consuming blocks round it for the finger. On a `do` step taps inside the hole reach
 * the app; on a `look` step a fifth block covers the hole too (✓ Save, Capture 360 are
 * shown, never pressed). No hole blocks the whole frame.
 */
@Composable
private fun TourScrim(frame: TourPlace.R, hole: TourPlace.R?, look: Boolean) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawBehind {
                    drawRect(TOUR_SCRIM)
                    if (hole != null) {
                        drawRoundRect(
                            Color.Black,
                            topLeft = Offset(hole.x.dp.toPx(), hole.y.dp.toPx()),
                            size = Size(hole.w.dp.toPx(), hole.h.dp.toPx()),
                            cornerRadius = CornerRadius(10.dp.toPx()),
                            blendMode = BlendMode.Clear)
                    }
                })
        if (hole == null) {
            Box(Modifier.fillMaxSize().tourConsume())
        } else {
            val x1 = (frame.w - hole.x - hole.w).coerceAtLeast(0f)
            val y1 = (frame.h - hole.y - hole.h).coerceAtLeast(0f)
            Box(Modifier.fillMaxWidth().height(hole.y.coerceAtLeast(0f).dp).tourConsume())
            Box(Modifier.offset(0.dp, (hole.y + hole.h).dp).fillMaxWidth().height(y1.dp).tourConsume())
            Box(Modifier.offset(0.dp, hole.y.dp).width(hole.x.coerceAtLeast(0f).dp).height(hole.h.dp).tourConsume())
            Box(Modifier.offset((hole.x + hole.w).dp, hole.y.dp).width(x1.dp).height(hole.h.dp).tourConsume())
            if (look) Box(Modifier.offset(hole.x.dp, hole.y.dp).width(hole.w.dp).height(hole.h.dp).tourConsume())
        }
    }
}

/** A small text button of the bubble's own scale (material's minimum would double the card). */
@Composable
private fun TourBtn(label: String, enabled: Boolean, primary: Boolean = false, onClick: () -> Unit) {
    Text(
        label,
        color = if (!enabled) TOUR_DIM else if (primary) Color(0xFF14160F) else TOUR_FG,
        fontSize = 13.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(15.dp))
            .background(if (primary && enabled) TOUR_GOLD else Color.Transparent)
            .border(1.dp, if (primary && enabled) TOUR_GOLD else TOUR_EDGE, RoundedCornerShape(15.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null,
                enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp))
}

/**
 * The bubble: counter, the flow's name, the mode chip, the step's title and body, and
 * `Skip tour · Back · Next` (`Done` on the last step; Back disabled on step 1 and while a
 * move runs). Every text is a table row; the counter is digits.
 */
@Composable
private fun TourBubble(tx: Tx, m: TourMachine, hasHole: Boolean, step: TourStep, scroll: Boolean) {
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(TOUR_CARD)
            .border(1.dp, TOUR_EDGE, RoundedCornerShape(12.dp))
            .semantics { contentDescription = tx.help_tour_aria }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(m.counter, color = TOUR_GOLD, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(m.flow.name, color = TOUR_MUTED, fontSize = 11.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (hasHole) {
                Spacer(Modifier.width(8.dp))
                Text(if (step.mode == "look") tx.help_tour_mode_look else tx.help_tour_mode_do,
                    color = TOUR_GOLD, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp)).border(1.dp, TOUR_GOLD, RoundedCornerShape(9.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp))
            }
        }
        Text(step.title, color = TOUR_FG, fontSize = 14.sp, lineHeight = 18.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
        Text(step.body, color = Color(0xFFCFD4C8), fontSize = 12.sp, lineHeight = 16.sp,
            modifier = (if (scroll) Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()) else Modifier)
                .padding(top = 4.dp))
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            TourBtn(tx.help_tour_skip, enabled = true) { TourState.skip() }
            Spacer(Modifier.weight(1f))
            TourBtn(tx.app_aria_back, enabled = m.i > 0 && !m.busy) { TourState.back() }
            Spacer(Modifier.width(8.dp))
            TourBtn(if (m.last) tx.common_done else tx.common_next, enabled = !m.busy, primary = true) { TourState.next() }
        }
    }
}

/**
 * The demo flat on disk (port 61c §31): seeded idempotently at app start, snapshotted when a
 * tour starts and put back when it ends. [TourDemo] (:pano) is the identity and the seed
 * content; this is the Context half — the capture row, the bundled pano asset, the prep file.
 */
object TourSeed {

    /** What a tour's end restores: the prep file's bytes, the row's size, the local phase. */
    class Snap(val prep: String?, val sizeIn: DoubleArray, val phase: Int)

    private fun panoFile(context: Context): File =
        File(File(context.filesDir, "tourdemo").apply { mkdirs() }, "pano.jpg")

    /** The demo's prep file, where SurveyPrepStore keeps every prep. */
    private fun prepFile(context: Context): File =
        File(File(context.filesDir, "surveyprep").apply { mkdirs() }, TourDemo.CAPTURE + ".json")

    /** Seed the demo once: the pano asset onto disk, the capture row (SYNCED, so it adds
     *  nothing to "N waiting"), the prep. Idempotent; every piece alone, so a half seed heals. */
    fun ensure(context: Context, store: CaptureStore) {
        val pano = panoFile(context)
        if (!pano.exists()) runCatching {
            context.assets.open("tour_demo_pano.jpg").use { ins -> pano.outputStream().use { ins.copyTo(it) } }
        }
        if (store.all().none { it.deviceId == TourDemo.CAPTURE }) runCatching {
            val inch = SurveyPrep.inches(TourDemo.SIZE_MM)
            store.insert(CaptureStore.Capture(
                deviceId = TourDemo.CAPTURE, project = "", projectTitle = TourDemo.PROJECT,
                customerName = TourDemo.CLIENT, room = TourDemo.ROOM, stage = "",
                captureDate = "2026-10-11", // text:value the demo capture's fixed date (TourDemo.CAPTURED_AT_MS's day)
                panoPath = pano.path, createdAt = TourDemo.CAPTURED_AT_MS,
                state = "SYNCED", serverName = null, error = null,
                roomLengthIn = inch[0], roomWidthIn = inch[1], roomHeightIn = inch[2]))
        }
        val prep = prepFile(context)
        if (!prep.exists()) runCatching { prep.writeText(TourDemo.prepJson()) }
    }

    /** What the demo holds as the tour starts. */
    fun snapshot(context: Context, store: CaptureStore): Snap {
        val row = store.all().firstOrNull { it.deviceId == TourDemo.CAPTURE }
        return Snap(
            prep = runCatching { prepFile(context).takeIf { it.exists() }?.readText() }.getOrNull(),
            sizeIn = doubleArrayOf(row?.roomLengthIn ?: 0.0, row?.roomWidthIn ?: 0.0, row?.roomHeightIn ?: 0.0),
            phase = TourState.demoPhase)
    }

    /** Put the demo back; written only if the practice changed it. Returns the writes made. */
    fun restore(context: Context, store: CaptureStore, snap: Snap): Int {
        var wrote = 0
        runCatching {
            val f = prepFile(context)
            val now = if (f.exists()) f.readText() else null
            if (snap.prep != null && now != snap.prep) { f.writeText(snap.prep); wrote++ }
        }
        runCatching {
            val row = store.all().firstOrNull { it.deviceId == TourDemo.CAPTURE }
            if (row != null && snap.sizeIn.any { it > 0.0 } &&
                (row.roomLengthIn != snap.sizeIn[0] || row.roomWidthIn != snap.sizeIn[1] || row.roomHeightIn != snap.sizeIn[2])) {
                store.setRoomSize(TourDemo.CAPTURE, snap.sizeIn[0], snap.sizeIn[1], snap.sizeIn[2])
                wrote++
            }
        }
        return wrote
    }
}
