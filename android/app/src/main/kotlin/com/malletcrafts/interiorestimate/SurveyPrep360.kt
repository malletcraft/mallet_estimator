package com.malletcrafts.interiorestimate

import android.graphics.Bitmap
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malletcrafts.interiorestimate.pano.GestureClassifier
import com.malletcrafts.interiorestimate.pano.Pano360
import com.malletcrafts.interiorestimate.pano.Panorama
import com.malletcrafts.interiorestimate.pano.SurveyChain
import com.malletcrafts.interiorestimate.pano.SurveyGuide
import com.malletcrafts.interiorestimate.pano.SurveyMeas
import com.malletcrafts.interiorestimate.pano.SurveyPrep
import com.malletcrafts.interiorestimate.pano.SurveyPrepPalette
import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch
import com.malletcrafts.interiorestimate.pano.SurveyRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 4 · 360 (v52 slice 17; Amit, 12 Oct 2026 18:43 IST: "as you have 360 foto, can 360 foto be marked up directly as
 * well?"; 15 Oct 2026, on putting it in this port: "Yes, last, after everything else.").
 *
 * A flat view of the whole 360 that the finger turns -- a drag looks round, a pinch zooms 40°-110° across, fit faces
 * the open face again -- read from the 2048 px preview the screen already holds, never decoded again. Over it every
 * BOUNDED surface's four lines, and every mark, tape, Line, area, chain and (while its eye is on) reference in its true
 * perspective; the guide and the plane grid lean with the wall. Marks are placed, moved and resized, tapes and Lines
 * pulled, ends dragged and chains tapped HERE, in the same millimetres as 2 · Mark: ONE MODEL, TWO VIEWS -- every edit
 * goes through the dialog's own functions into the same prep fields, and nothing is stored per view.
 *
 * All the maths is Pano360 in :pano (tested there): the projection both ways, which surface a press is on, the pick
 * order, the placing check by type name, the tape's band. This file only dispatches, and paints with Paints and a Path
 * made once ([P3Ink]); the view's pixels are drawn off the main thread into buffers made once per size ([P3Buffers]).
 */

/** What 4 · 360 does with a press: the dialog's own edits, the same ones 2 · Mark makes (SurveyPrepDialog). */
internal class P3Actions(
    /** A thing pressed: picked, with the part grabbed (SpSel); a newly picked mark's first open reading active. */
    val grab: (SpSel) -> Unit,
    /** The tab follows the surface the finger is on (sp.js `st.tab=s`), in 4 · 360. */
    val focus: (String) -> Unit,
    /** The first move of a drag: its ONE undo step. */
    val before: () -> Unit,
    val moved: () -> Unit,
    val changed: () -> Unit,
    /** A drag put back (a second finger on an end, a long press on a moved grab): its undo step taken off. */
    val revert: () -> Unit,
    /** A tap on nothing with Select on: nothing picked. */
    val deselect: () -> Unit,
    /** Type t (on the tab's palette) at (x, y) mm on surface s: the new mark, or null when refused (said). */
    val place: (String, String, Double, Double) -> SurveyPrep.Mark?,
    val pickChain: (String, String) -> Unit,
    val pickMeas: (String, String) -> Unit,
    /** Chain on: a point at mm on surface s, with its snapping reach (mm); its index, or null when refused. */
    val chainPress: (String, DoubleArray, Double) -> Int?,
    val chainMove: (Int, DoubleArray, Double) -> Unit,
    val chainCancel: (Int) -> Unit,
    val chainFinish: () -> Unit,
    val dragStart: (SurveyMeas.Draft) -> Unit,
    val dragMove: () -> Unit,
    val dragEnd: (SurveyMeas.Draft) -> Unit,
    val dragCancel: () -> Unit,
    /** Line on: a tap at mm on surface s -- whether on the area's first corner, whether a double tap. */
    val areaTap: (String, DoubleArray, Boolean, Boolean) -> Unit,
    val tapeTap: () -> Unit,
    val endGuide: (SurveyGuide.Guide?) -> Unit,
    val say: (String) -> Unit,
    val refuse: (String) -> Unit,
    /** The floating trash tapped: the picked item deleted (as 2 · Mark's disc; the reference refuses). */
    val delete: (String) -> Unit,
    /** A long press with Select on, on a thing: its id and the press in root px -- the menu opens there. */
    val longPress: (String, Offset) -> Unit,
    /** The view turned or zoomed (or faced again by fit). */
    val look: (Pano360.View) -> Unit,
    /** ✕ on the tool chip: a tool picked (Select). */
    val tool: (String) -> Unit,
    val grid: () -> Unit,
    /** A press just under the step strip is that step's (StepRoute.stepFor), root px; and opening it. */
    val stepAt: (Float, Float) -> String?,
    val step: (String) -> Unit,
)

/** One drawn view: its pixels and the look they were drawn for (the overlay is drawn with the same look, so the marks
 *  stay on the photo while the next view is being drawn). */
private class P3Frame(val bmp: Bitmap, val yaw: Double, val pitch: Double, val fov: Double)

/**
 * THE VIEW'S PIXEL BUFFERS: one IntArray and three Bitmaps, made once per screen size and reused for every frame --
 * three, so the one being written is never the one on screen nor the one before it. [render] runs off the main thread,
 * one call at a time (the caller's flow is sequential).
 */
private class P3Buffers {
    private var bw = 0
    private var bh = 0
    private var px = IntArray(0)
    private val bmps = arrayOfNulls<Bitmap>(3)
    private var next = 0

    fun render(pano: Panorama.Image, yaw: Double, pitch: Double, fov: Double, w: Float, h: Float): P3Frame? {
        if (w < 2f || h < 2f) return null
        val sz = Pano360.bufferSize(w.toDouble(), h.toDouble())
        if (sz[0] != bw || sz[1] != bh) {
            bw = sz[0]; bh = sz[1]; px = IntArray(bw * bh)
            for (i in bmps.indices) bmps[i] = null
        }
        val i = next; next = (next + 1) % bmps.size
        val bmp = bmps[i] ?: Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888).also { bmps[i] = it }
        Pano360.render(pano, Pano360.View(yaw, pitch, fov, bw.toDouble(), bh.toDouble()), px, bw, bh)
        bmp.setPixels(px, 0, bw, 0, 0, bw, bh)
        return P3Frame(bmp, yaw, pitch, fov)
    }
}

/** What is on the 360 for a press to take, worked out from the model and the view at the press. */
private class P3Scene(
    val sc: Pano360.Scene,
    /** The picked Tape / Line / area, whose points [Pano360.Scene.ends] are. */
    val selMeas: Pair<String, SurveyPrep.Meas>?,
    /** The picked mark, whose tabs [Pano360.Scene.tabs] are. */
    val selMark: Pair<String, SurveyPrep.Mark>?,
    /** Every chain's line and every Tape's, Line's and area's, for a tap with Select on (sp.js kHitLine, mHit). */
    val chainLines: List<Pano360.Line>,
    val measLines: List<Pano360.Line>,
)

/** A drag in progress on the 360: the surface it is on, what it moves (the finger's mm on that surface's plane), and
 *  how it refuses. As 2 · Mark's SpGrab, in mm rather than picture px. */
private class P3Grab(val s: String, val id: String, val locked: String? = null, val endLock: Boolean = false,
                     val gate: Boolean = false, val cancel: (() -> Unit)? = null, val move: (DoubleArray) -> Unit)

@Composable
internal fun Sp360(
    /** Its size: the height the controls leave (the photo's box). */
    modifier: Modifier,
    /** The 360 at preview size, held by the screen (decoded once). */
    pano: Panorama.Image,
    /** Where it looks: yaw, pitch and fov (its w and h are the screen's, set here). */
    look: Pano360.View,
    tab: String, rev: Int,
    prep: SurveyPrep.Prep,
    /** L, W, H mm: every face's frame and size, as the dialog's. */
    roomSize: DoubleArray,
    sel: SpSel?, active: String?,
    tool: String,
    drag: SurveyMeas.Draft?,
    area: List<DoubleArray>?, areaS: String?,
    chain: SurveyChain.Draft?,
    guide: SurveyGuide.Guide?,
    grid: Boolean,
    act: P3Actions,
) {
    val dens = LocalDensity.current
    val dpx = dens.density
    // the gesture below outlives recompositions; everything it reads that can change under it is read through these
    val actS by rememberUpdatedState(act)
    val prepS by rememberUpdatedState(prep); val selS by rememberUpdatedState(sel)
    val lookS by rememberUpdatedState(look); val toolS by rememberUpdatedState(tool)
    val roomS by rememberUpdatedState(roomSize)
    val areaS0 by rememberUpdatedState(area); val areaSurfS by rememberUpdatedState(areaS)
    val chainS by rememberUpdatedState(chain)
    var boxW by remember { mutableStateOf(1f) }
    var boxH by remember { mutableStateOf(1f) }
    var boxAt by remember { mutableStateOf(Offset.Zero) }
    var tick by remember { mutableIntStateOf(0) }
    // THE FLOATING TRASH as last drawn (centre x, y and radius, box px), or null: written by the draw, read by a press
    val trashAt = remember { arrayOfNulls<FloatArray>(1) }

    fun frames(): Map<String, SurveyPrep.Frame> = roomS.let { r -> Pano360.frames(prepS, r[0], r[1], r[2]) }
    fun ab(s: String): DoubleArray = roomS.let { r -> SurveyPrep.sizeOf(s, prepS, r[0], r[1], r[2]) }
    fun viewNow(): Pano360.View = lookS.sized(boxW.toDouble(), boxH.toDouble())

    // ---- THE VIEW'S PIXELS: drawn off the main thread whenever the look or the size changes, the latest only
    // (conflated), into buffers made once per size; the screen shows the last one drawn
    val bufs = remember { P3Buffers() }
    var shown by remember { mutableStateOf<P3Frame?>(null) }
    LaunchedEffect(pano) {
        snapshotFlow { Triple(lookS, boxW, boxH) }
            .conflate()
            .collect { (lk, w, h) ->
                val f = withContext(Dispatchers.Default) { bufs.render(pano, lk.yaw, lk.pitch, lk.fov, w, h) }
                if (f != null) shown = f
            }
    }

    // THE GUIDE'S FADE, as 2 · Mark's (v52 slice 10): in at 45 % to full in 150 ms, out in 300 ms; the last guide kept
    // for the fade out. On the 360 it is drawn on its own surface, whichever tab is open.
    val gAlpha = remember { Animatable(0f) }
    val lastGuide = remember { arrayOfNulls<SurveyGuide.Guide>(1) }
    if (guide != null) lastGuide[0] = guide
    LaunchedEffect(guide != null) {
        if (guide != null) {
            gAlpha.snapTo(SurveyGuide.FADE_FROM.toFloat())
            gAlpha.animateTo(1f, tween(SurveyGuide.FADE_IN_MS, easing = LinearEasing))
        } else if (gAlpha.value > 0f) {
            gAlpha.animateTo(0f, tween(SurveyGuide.fadeOutMs(gAlpha.value.toDouble()), easing = LinearEasing))
        }
    }

    /** What a press can take, from the model as it stands and the view [v] (Pano360.Scene and the lines to pick). */
    fun sceneOf(v: Pano360.View, fr: Map<String, SurveyPrep.Frame>): P3Scene {
        val p = prepS; val sid = selS?.id; val d = dpx.toDouble()
        val sm = sid?.let { p.markById(it) }?.takeIf { it.first in fr }
        val sq = sid?.let { p.measById(it) }?.takeIf { it.first in fr && SurveyMeas.known(it.second.kind) }
        val polys = ArrayList<Pano360.Poly>()
        val chainL = ArrayList<Pano360.Line>(); val nodes = ArrayList<Pair<String, List<DoubleArray>>>()
        val measL = ArrayList<Pano360.Line>()
        for ((s, f) in fr) {
            val sf = p.surface(s)
            for (e in sf.marks) Pano360.markPoly(v, s, f, e, d)?.let { polys.add(it) }
            for (c in sf.chains) if (c.nodes.isNotEmpty()) {
                nodes.add(c.id to Pano360.screenPts(v, s, f, c.nodes))
                chainL.add(Pano360.Line(c.id, Pano360.segsOf(v, s, f, c.nodes, false)))
            }
            for (m in sf.meas) if (SurveyMeas.known(m.kind)) measL.add(Pano360.measLine(v, s, f, m, d))
        }
        // the picked mark's tabs -- none on a locked mark or a spot (it is grabbed whole)
        val tabs = sm?.let { (s, e) ->
            if (SurveyPrep.isLocked(e) || SurveyPrep.isPoint(e.t)) emptyList()
            else Pano360.tabsOf(e).mapNotNull { (part, mm) -> Pano360.toS(v, s, fr.getValue(s), mm[0], mm[1])?.let { part to it } }
        } ?: emptyList()
        val ends = sq?.let { (s, m) -> Pano360.screenPts(v, s, fr.getValue(s), m.pts) }
        return P3Scene(Pano360.Scene(ends, tabs, nodes, polys, sid), sq, sm, chainL, measL)
    }

    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black)
        .onSizeChanged { boxW = it.width.toFloat(); boxH = it.height.toFloat() }
        .onGloballyPositioned { boxAt = it.positionInRoot() }
        .pointerInput(pano) {
            // THE ONE GESTURE LOOP, as 2 · Mark's (v52 slice 7): GestureClassifier in :pano decides what a finger is
            // doing -- press, drag, tap, long press, double tap, or cancelled by a second finger -- and this acts on it.
            val gc = GestureClassifier(viewConfiguration.touchSlop.toDouble(), GestureClassifier.DOUBLE_TAP_DP.dp.toPx().toDouble(),
                still = GestureClassifier.GRAB_STILL_DP.dp.toPx().toDouble())
            awaitEachGesture {
                val first = awaitFirstDown()
                first.consume()
                // A PRESS JUST UNDER THE STEP STRIP IS THE STEP'S (v52 slice 6), as on the photo
                val routed = actS.stepAt(first.position.x + boxAt.x, first.position.y + boxAt.y)
                if (routed != null) {
                    gc.reset()
                    do {
                        val ev = awaitPointerEvent()
                        ev.changes.forEach { it.consume() }
                    } while (ev.changes.any { it.pressed })
                    actS.step(routed)
                    return@awaitEachGesture
                }
                val d = dpx.toDouble()
                // THE FLOATING TRASH is on top of everything (sp.js fTrashP3, as 2 · Mark's): a press on it deletes the
                // picked item when it lifts there. Only with Select on -- with a type on the disc is not drawn -- and
                // never over the picked Tape's, Line's or area's end handle, which is the end's
                val tr = if (toolS == SurveyPrepPalette.SEL) trashAt[0] else null
                if (tr != null && hypot(first.position.x - tr[0], first.position.y - tr[1]) <= tr[2] &&
                    sceneOf(viewNow(), frames()).sc.ends?.let { es ->
                        SurveyGuide.endAt(es, first.position.x.toDouble(), first.position.y.toDouble(), Pano360.END_DP * d) } == null) {
                    gc.reset()
                    var at = first.position
                    do {
                        val ev = awaitPointerEvent()
                        ev.changes.forEach { it.consume() }
                        ev.changes.firstOrNull { it.id == first.id }?.let { at = it.position }
                    } while (ev.changes.any { it.pressed })
                    val picked = selS?.id
                    if (picked != null && hypot(at.x - tr[0], at.y - tr[1]) <= tr[2]) actS.delete(picked)
                    return@awaitEachGesture
                }
                // the model and the view as the press began
                val v0 = viewNow()
                val fr = frames()
                val tool0 = toolS
                val drafting = !areaS0.isNullOrEmpty() || chainS != null
                val scene = sceneOf(v0, fr)
                val order = Pano360.order(tool0, drafting)
                var grab: P3Grab? = null
                var moved = false; var snapped = false; var refused = false; var spent = false
                var gated = false; var endGrabbed = false; var secondFinger = false; var gone = false
                val endGate = SurveyGuide.EndGate(max(SurveyGuide.END_DEAD_DP.dp.toPx().toDouble(), viewConfiguration.touchSlop.toDouble()))
                // what the press is for, when nothing was grabbed at it
                var measOn = false; var chainOn = false; var placing = false
                var looking = false; var lookMoved = false
                var kpick: Pair<String, String>? = null; var mpick: Pair<String, String>? = null
                // Tape or Line on, pressed on the picked item's end: the new one starts there ([startAt] on [startS])
                var startAt: DoubleArray? = null; var startS: String? = null
                var draft: SurveyMeas.Draft? = null; var measDragged = false; var heldTap = false
                var chainIdx: Int? = null; var chainTried = false; var chainSurf: String? = null; var chainTol = SurveyGuide.SNAP_MM
                var pinch = false; var pinchD0 = 1.0; var pinchFov0 = v0.fov

                /** The surface under screen ([x], [y]) and the mm there (sp.js hitSurface), or null: said. */
                fun hitAt(x: Double, y: Double): Pano360.Hit? =
                    Pano360.hitSurface(v0.dirAt(x, y), fr).also { if (it == null) actS.say(Pano360.NO_LINES) }

                /** The finger at ([x], [y]) on surface [s]'s plane, mm (null where it never meets it). */
                fun mmAt(s: String, x: Double, y: Double): DoubleArray? = Pano360.mmOn(s, fr[s], v0.dirAt(x, y))

                /** A type placed where the finger is, and a grab that carries the new mark (sp.js addElAt on the press). */
                fun placeAt(x: Double, y: Double): P3Grab? {
                    val h = hitAt(x, y) ?: return null
                    val e = actS.place(h.s, tool0, h.x, h.y) ?: return null
                    val start = e.copy(); val m0 = doubleArrayOf(h.x, h.y); val ab0 = ab(h.s)
                    return P3Grab(h.s, e.id) { mm ->
                        SurveyPrep.moveMark(e, start, "body", mm[0] - m0[0], mm[1] - m0[1], ab0[0], ab0[1])
                        SurveyPrep.snapMark(e, "body", ab0[0], ab0[1])
                    }
                }

                /** Tape or Line on, a tap: the Tape's hint, or a Line area's corner on the surface under the finger. */
                fun measTap(x: Double, y: Double, dbl: Boolean) {
                    // the Tape's hint only on a bounded surface (or a picked end): off every one it is "Set this
                    // surface's lines first", said by hitAt (sp.js P3 pointerdown runs hitSurface / mmOn first)
                    if (tool0 == SurveyPrepPalette.TAPE) { if (startS == null && hitAt(x, y) == null) return; actS.tapeTap(); return }
                    val open = areaSurfS?.takeIf { !areaS0.isNullOrEmpty() }
                    val s = startS ?: open ?: hitAt(x, y)?.s ?: return
                    val mm = startAt ?: mmAt(s, x, y) ?: run { actS.say(Pano360.NO_LINES); return }
                    val a = areaS0
                    val f = fr[s]
                    val near = a != null && a.isNotEmpty() && open == s && f != null &&
                        Pano360.toS(v0, s, f, a[0][0], a[0][1])?.let { q -> SurveyMeas.nearFirst(q, x, y, a.size, d) } == true
                    actS.areaTap(s, mm, near, dbl)
                }

                /** Chain on: a point where the finger is (or went down), on the chain's surface or the one under it. */
                fun chainAt(x: Double, y: Double): Int? {
                    val s = chainS?.s ?: hitAt(x, y)?.s ?: return null
                    val f = fr[s] ?: run { actS.say(Pano360.NO_LINES); return null }
                    val mm = mmAt(s, x, y) ?: run { actS.say(Pano360.NO_LINES); return null }
                    chainSurf = s; chainTol = Pano360.tolMm(v0, s, f, x, y, mm, d)
                    return actS.chainPress(s, mm, chainTol)
                }
                /** Select on, a tap (or a quick second tap, as on 2 · Mark) on nothing grabbed: a chain's line, else a
                 *  Tape's or a Line's, is picked; on nothing, the pick is let go (Pano360.lookTap). */
                fun lookTap(e: GestureClassifier.Event) {
                    val kp = kpick; val mp = mpick
                    when (Pano360.lookTap(e, kp != null, mp != null, lookMoved)) {
                        Pano360.LookTap.CHAIN -> if (kp != null) actS.pickChain(kp.first, kp.second)
                        Pano360.LookTap.MEAS -> if (mp != null) actS.pickMeas(mp.first, mp.second)
                        Pano360.LookTap.LET_GO -> actS.deselect()
                        null -> {}
                    }
                }
                fun chainTap(x: Double, y: Double) {
                    if (grab != null || chainTried) return
                    chainTried = true
                    chainAt(x, y)
                }

                fun act(evs: List<GestureClassifier.Event>) {
                    for (e in evs) if (!spent) when (e) {
                        // THE PRESS: what is under it is taken now, in the 360's pick order (Pano360.order / pick)
                        is GestureClassifier.Event.Down -> when (val pk = Pano360.pick(order, scene.sc, e.x, e.y, d)) {
                            is Pano360.Pick.End -> {
                                val (s, m) = scene.selMeas!!
                                val i = pk.i
                                if (SurveyPrepPalette.isMeasTool(tool0)) {
                                    // Tape or Line on: a NEW one from exactly this end (tapes laid end to end)
                                    measOn = true; startAt = m.pts[i].copyOf(); startS = s
                                } else {
                                    actS.grab(SpSel(s, id = m.id)); actS.focus(s); gc.grabbed()
                                    grab = if (SurveyPrep.isLocked(m)) P3Grab(s, m.id, m.code, endLock = true, gate = true) { }
                                    else {
                                        val f = fr.getValue(s)
                                        val tol = Pano360.tolMm(v0, s, f, e.x, e.y, mmAt(s, e.x, e.y), d)
                                        endGrabbed = true
                                        P3Grab(s, m.id, gate = true, cancel = { actS.endGuide(null) }) { mm ->
                                            val ab0 = ab(s)
                                            SurveyGuide.endMove(s, m, i, mm, ab0[0], ab0[1], prepS.surface(s).marks, tol)?.let { r ->
                                                m.pts[i] = r.p
                                                actS.endGuide(SurveyGuide.endGuide(s, m, r))
                                            }
                                        }
                                    }
                                    gated = true
                                }
                            }
                            is Pano360.Pick.Tab -> {
                                val (s, mk) = scene.selMark!!
                                actS.grab(SpSel(s, id = mk.id, part = pk.part)); actS.focus(s); gc.grabbed()
                                val m0 = mmAt(s, e.x, e.y)
                                val start = mk.copy(); val part = pk.part
                                grab = P3Grab(s, mk.id) { mm ->
                                    if (m0 != null) {
                                        val ab0 = ab(s)
                                        SurveyPrep.moveMark(mk, start, part, mm[0] - m0[0], mm[1] - m0[1], ab0[0], ab0[1])
                                        SurveyPrep.snapMark(mk, part, ab0[0], ab0[1])
                                    }
                                }
                            }
                            is Pano360.Pick.Node -> {
                                val sc = prepS.chainById(pk.id)
                                if (sc != null) {
                                    val (s, c) = sc; val i = pk.i
                                    actS.grab(SpSel(s, id = c.id)); actS.focus(s); gc.grabbed(); gated = true
                                    grab = if (SurveyPrep.isLocked(c)) P3Grab(s, c.id, c.code, endLock = true, gate = true) { }
                                    else {
                                        val tol = Pano360.tolMm(v0, s, fr.getValue(s), e.x, e.y, mmAt(s, e.x, e.y), d)
                                        P3Grab(s, c.id, gate = true) { mm ->
                                            val ab0 = ab(s)
                                            SurveyChain.nodeMove(s, c, i, mm, ab0[0], ab0[1], prepS.surface(s).marks, tol)
                                        }
                                    }
                                }
                            }
                            Pano360.Pick.Chain -> chainOn = true
                            Pano360.Pick.Meas -> measOn = true
                            is Pano360.Pick.Mark -> {
                                val mk = prepS.surface(pk.s).marks.firstOrNull { it.id == pk.id }
                                if (mk != null) {
                                    val s = pk.s
                                    actS.grab(SpSel(s, id = mk.id)); actS.focus(s); gc.grabbed()
                                    grab = if (SurveyPrep.isLocked(mk)) P3Grab(s, mk.id, mk.code) { }
                                    else {
                                        val m0 = mmAt(s, e.x, e.y); val start = mk.copy()
                                        P3Grab(s, mk.id) { mm ->
                                            if (m0 != null) {
                                                val ab0 = ab(s)
                                                SurveyPrep.moveMark(mk, start, "body", mm[0] - m0[0], mm[1] - m0[1], ab0[0], ab0[1])
                                                SurveyPrep.snapMark(mk, "body", ab0[0], ab0[1])
                                            }
                                        }
                                    }
                                }
                            }
                            Pano360.Pick.Place -> placing = true
                            Pano360.Pick.Look -> {
                                looking = true
                                // with Select on, a chain's line, else a Tape's or a Line's, within 12 dp: picked on the tap
                                if (tool0 == SurveyPrepPalette.SEL) {
                                    val r = Pano360.LINE_PICK_DP * d
                                    Pano360.pickLine(scene.chainLines, e.x, e.y, r)?.let { id -> prepS.chainById(id)?.let { kpick = it.first to id } }
                                    if (kpick == null) Pano360.pickLine(scene.measLines, e.x, e.y, r)?.let { id ->
                                        prepS.measById(id)?.let { mpick = it.first to id } }
                                }
                            }
                        }
                        // A DRAG: a Tape or a Line pulled, a chain's point carried, a type carried, a grab moved -- or
                        // the view turned
                        is GestureClassifier.Event.Drag -> if (measOn) {
                            if (draft == null && !(tool0 == SurveyPrepPalette.LINE && !areaS0.isNullOrEmpty())) {
                                val x0 = e.x - e.dx; val y0 = e.y - e.dy
                                val s = startS ?: hitAt(x0, y0)?.s
                                val m0 = if (s == null) null else startAt ?: mmAt(s, x0, y0)
                                if (s != null && m0 != null) {
                                    val ab0 = ab(s)
                                    val dr = SurveyMeas.Draft(s, if (tool0 == SurveyPrepPalette.TAPE) SurveyMeas.TAPE else SurveyMeas.LINE, m0, ab0[0], ab0[1])
                                    draft = dr; measDragged = true; actS.focus(s); actS.dragStart(dr)
                                } else measOn = false
                            }
                            val dr = draft
                            if (dr != null) {
                                val fin = mmAt(dr.s, e.x, e.y)
                                val st = startAt
                                val dn = if (st != null) mmAt(dr.s, e.x - e.dx, e.y - e.dy) else null
                                // FROM A SELECTED END: the end plus the finger's movement (SurveyGuide.dragFrom), as 2 · Mark
                                val p = if (fin == null) null else if (st != null && dn != null) SurveyGuide.dragFrom(st, dn, fin) else fin
                                if (p != null) dr.move(p) else dr.offPlane()
                                actS.dragMove()
                            }
                        } else if (chainOn && grab == null) {
                            if (!chainTried) { chainTried = true; chainIdx = chainAt(e.x - e.dx, e.y - e.dy) }
                            val ci = chainIdx; val cs = chainSurf
                            if (ci != null && cs != null) mmAt(cs, e.x, e.y)?.let { mm -> actS.chainMove(ci, mm, chainTol) }
                        } else {
                            if (e.first && grab == null && placing) {
                                placeAt(e.x - e.dx, e.y - e.dy)?.let { grab = it; snapped = true }   // placing was its one undo step
                            }
                            val g = grab
                            if (g != null && g.locked != null) {
                                // LOCKED: nothing moves, no undo entry; the refusal once the finger has really moved
                                if (!refused && hypot(e.dx, e.dy) > 4.dp.toPx()) {
                                    refused = true
                                    if (g.endLock) actS.say(SurveyGuide.lockedEnd(g.locked)) else actS.refuse(g.locked)
                                }
                            } else if (g != null) {
                                if (!gated || endGate.move(e.dx, e.dy)) {
                                    mmAt(g.s, e.x, e.y)?.let { mm ->
                                        if (!snapped) { actS.before(); snapped = true }
                                        g.move(mm); moved = true; actS.moved()
                                    }
                                }
                            } else if (looking) {
                                // A DRAG ON NOTHING LOOKS ROUND (sp.js gs.look): from the view as the finger went down
                                lookMoved = true
                                actS.look(Pano360.look(v0, v0.yaw, v0.pitch, e.dx, e.dy))
                            }
                        }
                        is GestureClassifier.Event.Tap -> {
                            if (SurveyGuide.revertOnTap(gated, moved)) { actS.revert(); moved = false; snapped = false }
                            if (measOn) measTap(e.x, e.y, false)
                            else if (chainOn) chainTap(e.x, e.y)
                            else if (placing && grab == null) placeAt(e.x, e.y)
                            else if (looking) lookTap(e)
                        }
                        is GestureClassifier.Event.DoubleTap -> {
                            if (SurveyGuide.revertOnTap(gated, moved)) { actS.revert(); moved = false; snapped = false }
                            if (measOn) measTap(e.x, e.y, true)
                            else if (chainOn) { if (grab == null) actS.chainFinish() }
                            else if (placing && grab == null) placeAt(e.x, e.y)
                            // with Select on, on nothing grabbed, a quick second tap picks or lets go as a tap does
                            else if (looking) lookTap(e)
                        }
                        // A LONG PRESS: with Tape, Line or Chain on, a slow tap; with a type on, it places and the slide
                        // carries; with Select on, held on a thing, the menu (Delete · Duplicate · Lock · Lock all · Cancel)
                        is GestureClassifier.Event.LongPress -> if (measOn) { heldTap = true } else if (chainOn) { if (grab == null) heldTap = true } else {
                            val gid = grab?.id ?: kpick?.second ?: mpick?.second
                            if (grab == null && placing)
                                placeAt(e.x, e.y)?.let { grab = it; snapped = true; gc.grabbed() }
                            else if (tool0 == SurveyPrepPalette.SEL && gid != null) {
                                if (moved) { actS.revert(); moved = false; snapped = false }
                                grab = null; spent = true
                                actS.longPress(gid, Offset(e.x.toFloat() + boxAt.x, e.y.toFloat() + boxAt.y))
                            }
                        }
                        is GestureClassifier.Event.DragEnd -> if (measOn) draft?.let { dr -> draft = null; actS.dragEnd(dr) }
                        // a second finger: the pinch takes over; a Tape or Line pulled is dropped (nothing plotted), a
                        // chain's point this press added goes, an end being dragged goes back with its undo step
                        is GestureClassifier.Event.Cancel -> {
                            val g = grab
                            grab = null; gone = true
                            if (draft != null) { draft = null; actS.dragCancel() }
                            chainIdx?.let { ci -> if (secondFinger) { chainIdx = null; actS.chainCancel(ci) } }
                            g?.cancel?.let { c ->
                                if (SurveyGuide.revertOnCancel(secondFinger, moved)) { actS.revert(); moved = false; snapped = false }
                                c()
                            }
                        }
                    }
                }

                try {
                    act(gc.down(first.position.x.toDouble(), first.position.y.toDouble(), first.uptimeMillis))
                    while (true) {
                        val dl = gc.deadline()
                        val ev = if (dl == null) awaitPointerEvent()
                                 else withTimeoutOrNull(max(1L, dl - SystemClock.uptimeMillis())) { awaitPointerEvent() }
                        if (ev == null) { act(gc.timeout(dl!!)); continue }
                        val t = ev.changes.maxOf { it.uptimeMillis }
                        val down = ev.changes.filter { it.pressed }
                        if (down.size >= 2) {
                            secondFinger = true
                            act(gc.move(down[0].position.x.toDouble(), down[0].position.y.toDouble(), t, down.size))
                            // A PINCH ZOOMS, 40°-110° (sp.js gs.pinch), about the view as it stands
                            val a = down[0].position; val b = down[1].position
                            val dd = hypot(a.x - b.x, a.y - b.y).toDouble().coerceAtLeast(1.0)
                            if (!pinch) { pinch = true; grab = null; pinchD0 = dd; pinchFov0 = lookS.fov }
                            else { val lk = lookS; actS.look(Pano360.View(lk.yaw, lk.pitch, Pano360.pinchFov(pinchFov0, pinchD0, dd), lk.w, lk.h)) }
                            ev.changes.forEach { it.consume() }
                        } else if (down.size == 1) {
                            val ch = down[0]; ch.consume()
                            if (!pinch) act(gc.move(ch.position.x.toDouble(), ch.position.y.toDouble(), t, 1))
                        } else {
                            // a system cancel is not a lift (palm rejection): every pointer up and already consumed
                            val cancelled = ev.changes.all { it.isConsumed }
                            ev.changes.forEach { it.consume() }
                            val ch = ev.changes.firstOrNull { it.id == first.id } ?: ev.changes[0]
                            act(if (cancelled) gc.cancel() else gc.up(ch.position.x.toDouble(), ch.position.y.toDouble(), t))
                            break
                        }
                    }
                    // a slow tap with Tape, Line or Chain on: where the finger went down
                    if (measOn && heldTap && !measDragged && !gone) measTap(first.position.x.toDouble(), first.position.y.toDouble(), false)
                    if (chainOn && heldTap && !gone) chainTap(first.position.x.toDouble(), first.position.y.toDouble())
                } finally {
                    if (draft != null) { draft = null; actS.dragCancel() }
                    if (endGrabbed) actS.endGuide(null)
                    if (moved) actS.changed() else tick++
                }
            }
        }) {
        val ink = remember(dpx) { P3Ink(dpx) }
        Canvas(Modifier.fillMaxSize()) {
            rev; tick; sel; active; drag; area; grid; guide; chain; tool; tab
            val fr0 = shown ?: return@Canvas
            val ga = gAlpha.value
            val gNow = (guide ?: lastGuide[0])?.takeIf { SurveyGuide.shown(ga.toDouble()) }
            drawIntoCanvas { cv ->
                val nc = cv.nativeCanvas
                ink.dst.set(0f, 0f, size.width, size.height)
                nc.drawBitmap(fr0.bmp, null, ink.dst, ink.bmp)
                // the overlay is drawn with the look the pixels were drawn with, so it never slides over them
                val v = Pano360.View(fr0.yaw, fr0.pitch, fr0.fov, size.width.toDouble(), size.height.toDouble())
                val r = roomSize
                val fr = Pano360.frames(prep, r[0], r[1], r[2])
                P3Draw.paint(nc, v, fr, prep, P3Draw.State(sel?.id, tab, tool == SurveyPrepPalette.SEL, drag,
                    area?.takeIf { !it.isEmpty() }, areaS, chain, gNow, ga, grid), ink, size.width, size.height)
                // THE FLOATING TRASH (sp.js fTrashP3): beside the picked mark, Tape, Line, area or chain, with Select on,
                // never for the reference; placed by SurveyPrepTouch.trashSpot as on 2 · Mark, clear of its end handles
                trashAt[0] = null
                Pano360.trashOf(v, fr, prep, tool, sel?.id)?.let { t ->
                    SurveyPrepTouch.trashSpot(t.box, emptyList(), size.width.toDouble(), size.height.toDouble(), dpx.toDouble(),
                        t.handles, SurveyGuide.HANDLE_CLEAR_DP * dpx)
                }?.let { ts ->
                    val rad = 18f * dpx; val tx = ts.left.toFloat() + rad; val ty = ts.top.toFloat() + rad
                    P3Draw.trash(nc, tx, ty, rad, ink)
                    trashAt[0] = floatArrayOf(tx, ty, rad)
                }
            }
        }
        // THE TOOL CHIP, as on 2 · Mark: "Placing Window · tap the 360"; the Tape's and Line's live words and caution;
        // the Chain's count and ✓; ✕ back to Select
        val chainN = chain?.nodes?.size ?: 0
        val chipText = if (tool == SurveyPrepPalette.CHAIN) SurveyChain.chipText(chainN)
                       else if (SurveyPrepPalette.isMeasTool(tool)) SurveyMeas.chipText(tool, drag, area?.size ?: 0)
                       else SurveyPrepPalette.chipText(tab, tool, "360")
        if (chipText != null) {
            Box(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 62.dp)) {
                SpToolChip(chipText, SurveyPrepPalette.chipColour(tab, tool), Modifier,
                    caution = if (SurveyPrepPalette.isMeasTool(tool) || tool == SurveyPrepPalette.CHAIN) SurveyMeas.CAUTION else null,
                    onDone = if (tool == SurveyPrepPalette.CHAIN && SurveyChain.showDone(chainN)) ({ act.chainFinish() }) else null) {
                    act.tool(SurveyPrepPalette.SEL) }
            }
        }
        // fit (faces the open face again), the grid, and the view's width in degrees
        Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) {
            Column(horizontalAlignment = Alignment.End) {
                Text("fit", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xAA000000)).clickable {
                        act.look(Pano360.faceView(tab, 1.0, 1.0)) }.padding(horizontal = 8.dp, vertical = 5.dp))
                Text("grid", color = if (grid) Color(0xFF06232A) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(6.dp))
                        .background(if (grid) Color(0xCC22D3EE) else Color(0xAA000000))
                        .semantics { contentDescription = "Plane grid"; selected = grid }
                        .clickable(role = Role.Button) { act.grid() }.padding(horizontal = 8.dp, vertical = 5.dp))
                Text(Pano360.zoomText(look.fov), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xAA000000))
                        .padding(horizontal = 6.dp, vertical = 3.dp))
            }
        }
    }
}

/** The Paints, Path and dashes the 360's overlay is drawn with: made once per density, never per frame. [a] scales every
 *  colour's alpha (the other marks at 75 % while one is picked). */
private class P3Ink(val dp: Float) {
    val bmp = Paint(Paint.FILTER_BITMAP_FLAG)
    val dst = RectF()
    val oval = RectF()
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFakeBoldText = true }
    val mono = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFakeBoldText = true }
    val path = Path()
    /** The floating trash's disc, ring and bin. */
    val trash = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    val dash75 = DashPathEffect(floatArrayOf(7f * dp, 5f * dp), 0f)
    val dash96 = DashPathEffect(floatArrayOf(SurveyGuide.DASH_DP.toFloat() * dp, SurveyGuide.GAP_DP.toFloat() * dp), 0f)
    val dash85 = DashPathEffect(floatArrayOf(8f * dp, 5f * dp), 0f)
    var a = 1f

    fun col(rgb: Int, alpha: Int = 255): Int = SurveyPrepDraw.argb(rgb, (alpha * a).roundToInt().coerceIn(0, 255))

    fun stroke(rgb: Int, w: Float, alpha: Int = 255, dash: DashPathEffect? = null): Paint =
        line.apply { color = col(rgb, alpha); strokeWidth = w; pathEffect = dash }

    fun paint(rgb: Int, alpha: Int = 255): Paint = fill.apply { color = col(rgb, alpha) }
}

/** THE OVERLAY ON THE 360 (sp.js P3.V.draw), in its order: every bounded surface's four lines; the marks (the picked one
 *  last, the rest at 75 %, the photo round it dimmed); the plane grid; each surface's Lines and areas, tapes, chains and
 *  reference; the guide; the picked mark's eight tabs. Everything through [Pano360] and clipped at the near plane. */
private object P3Draw {

    class State(val selId: String?, val tab: String, val ends: Boolean, val drag: SurveyMeas.Draft?,
                val area: List<DoubleArray>?, val areaS: String?, val chain: SurveyChain.Draft?,
                val guide: SurveyGuide.Guide?, val guideA: Float, val grid: Boolean)

    private const val OUT = SurveyPrepDraw.OUT_COLOUR

    /** THE FLOATING TRASH at ([tx], [ty]), radius [rad]: 2 · Mark's 36 dp red disc with a white ring and a bin. */
    fun trash(c: android.graphics.Canvas, tx: Float, ty: Float, rad: Float, ink: P3Ink) {
        val u = ink.dp; val pt = ink.trash
        pt.style = Paint.Style.FILL; pt.color = android.graphics.Color.rgb(0xE5, 0x48, 0x4D); c.drawCircle(tx, ty, rad, pt)
        pt.style = Paint.Style.STROKE; pt.strokeWidth = 2f * u; pt.color = android.graphics.Color.WHITE
        c.drawCircle(tx, ty, rad - u, pt)
        // a bin: lid, handle, body with two ribs
        pt.strokeWidth = 1.8f * u
        val g = 5.5f * u
        c.drawLine(tx - 1.3f * g, ty - g, tx + 1.3f * g, ty - g, pt)
        c.drawLine(tx - .45f * g, ty - 1.4f * g, tx + .45f * g, ty - 1.4f * g, pt)
        c.drawRect(tx - g, ty - .7f * g, tx + g, ty + 1.5f * g, pt)
        c.drawLine(tx - .35f * g, ty - .3f * g, tx - .35f * g, ty + 1.1f * g, pt)
        c.drawLine(tx + .35f * g, ty - .3f * g, tx + .35f * g, ty + 1.1f * g, pt)
    }
    private val LINE_C = SurveyMeas.LINE_COLOUR
    private val TAPE_C = SurveyMeas.TAPE_COLOUR

    fun paint(c: android.graphics.Canvas, v: Pano360.View, fr: Map<String, SurveyPrep.Frame>, prep: SurveyPrep.Prep,
              st: State, ink: P3Ink, w: Float, h: Float) {
        val u = ink.dp
        ink.a = 1f
        // every bounded surface's four lines: the edges of its frame, thin, in the line colours
        for ((s, f) in fr) {
            val dq = f.q.map { SurveyPrep.ray(s, it[0], it[1]) }
            for ((k, i, j) in listOf(Triple("bottom", 0, 1), Triple("right", 1, 2), Triple("top", 2, 3), Triple("left", 3, 0))) {
                val sg = v.seg(dq[i], dq[j]) ?: continue
                seg(c, sg, ink.stroke(SurveyPrepDraw.LINE_COLOUR.getValue(k), 1.5f * u, 217))
            }
        }
        marks(c, v, fr, prep, st.selId, ink, w, h)
        // the plane grid: on the open face with its button on, and on the guide's surface as the guide fades
        val gS = st.guide?.s?.takeIf { it in fr }
        for ((s, f) in fr) {
            val ga = if (st.grid && s == st.tab) 1f else if (s == gS) st.guideA else 0f
            if (ga > 0f) grid(c, v, s, f, ga, ink)
        }
        for ((s, f) in fr) {
            val sf = prep.surface(s)
            meas(c, v, s, f, sf, st, ink, w, h)
            chains(c, v, s, f, prep, sf, st, ink, w, h)
            ref(c, v, s, f, sf, st.selId, ink)
        }
        val g = st.guide
        if (g != null && gS != null) guide(c, v, g, fr.getValue(gS), st.guideA, ink)
        // the picked mark's eight tabs: white squares ringed in its colour, where a press resizes it
        val sm = st.selId?.let { prep.markById(it) }?.takeIf { it.first in fr }
        if (sm != null && !SurveyPrep.isLocked(sm.second) && !SurveyPrep.isPoint(sm.second.t)) {
            val (s, e) = sm; val f = fr.getValue(s); val hh = Pano360.TAB_HALF_DP.toFloat() * u
            val col = SurveyPrep.typeOf(s, e.t).colour
            for ((_, mm) in Pano360.tabsOf(e)) {
                val p = Pano360.toS(v, s, f, mm[0], mm[1]) ?: continue
                val x = p[0].toFloat(); val y = p[1].toFloat()
                c.drawRect(x - hh, y - hh, x + hh, y + hh, ink.paint(0xffffff))
                c.drawRect(x - hh, y - hh, x + hh, y + hh, ink.stroke(col, 2f * u))
            }
        }
        ink.a = 1f
    }

    private fun seg(c: android.graphics.Canvas, sg: Pair<DoubleArray, DoubleArray>, p: Paint) =
        c.drawLine(sg.first[0].toFloat(), sg.first[1].toFloat(), sg.second[0].toFloat(), sg.second[1].toFloat(), p)

    private fun pathOf(ink: P3Ink, pts: List<DoubleArray>): Path = ink.path.apply {
        reset(); moveTo(pts[0][0].toFloat(), pts[0][1].toFloat())
        for (i in 1 until pts.size) lineTo(pts[i][0].toFloat(), pts[i][1].toFloat())
        close()
    }

    /** Text with a dark rim, as SurveyPrepDraw.label, with the reused paint. */
    private fun label(c: android.graphics.Canvas, t: String, x: Float, y: Float, rgb: Int, size: Float, ink: P3Ink) {
        val p = ink.text
        p.textSize = size; p.style = Paint.Style.STROKE; p.strokeWidth = 3f * ink.dp; p.color = ink.col(0x000000, 0xcc)
        c.drawText(t, x, y, p)
        p.style = Paint.Style.FILL; p.color = ink.col(rgb)
        c.drawText(t, x, y, p)
    }

    private fun textW(t: String, size: Float, ink: P3Ink): Float { ink.mono.textSize = size; return ink.mono.measureText(t) }

    /** The padlock glyph and its dark pill, top-right corner at ([x], [y]) (sp.js padPill). */
    private fun padPill(c: android.graphics.Canvas, x: Float, y: Float, rgb: Int, ink: P3Ink) {
        val u = ink.dp; val sz = 12f * u; val pw = sz + 6f * u; val ph = sz + 5f * u; val x0 = x - pw
        ink.oval.set(x0, y, x, y + ph)
        c.drawRoundRect(ink.oval, ph / 2, ph / 2, ink.paint(0x000000, 0xd9))
        val gx = x0 + pw / 2; val gy = y + ph / 2 - sz * .14f
        val bw = sz * .72f; val bh = sz * .48f; val r = sz * .22f
        val p = ink.stroke(rgb, max(1f, sz * .13f))
        ink.path.reset()
        ink.path.moveTo(gx - r, gy + sz * .04f); ink.path.lineTo(gx - r, gy - sz * .06f)
        ink.oval.set(gx - r, gy - sz * .06f - r, gx + r, gy - sz * .06f + r)
        ink.path.arcTo(ink.oval, 180f, 180f, false)
        ink.path.lineTo(gx + r, gy + sz * .04f)
        c.drawPath(ink.path, p)
        c.drawRect(gx - bw / 2, gy + sz * .02f, gx + bw / 2, gy + sz * .02f + bh, ink.paint(rgb))
    }

    /** A Line's arrow head at [tip], pointing away from [from] (sp.js aHead): 7 dp long, 3 dp either side. */
    private fun head(c: android.graphics.Canvas, tip: DoubleArray, from: DoubleArray, rgb: Int, ink: P3Ink) {
        val dx = tip[0] - from[0]; val dy = tip[1] - from[1]; val l = hypot(dx, dy); if (!(l > 0)) return
        val ux = dx / l; val uy = dy / l
        val hl = SurveyMeas.HEAD_LEN_DP * ink.dp; val hw = SurveyMeas.HEAD_W_DP * ink.dp
        ink.path.reset()
        ink.path.moveTo(tip[0].toFloat(), tip[1].toFloat())
        ink.path.lineTo((tip[0] - ux * hl - uy * hw).toFloat(), (tip[1] - uy * hl + ux * hw).toFloat())
        ink.path.lineTo((tip[0] - ux * hl + uy * hw).toFloat(), (tip[1] - uy * hl - ux * hw).toFloat())
        ink.path.close()
        c.drawPath(ink.path, ink.paint(rgb))
    }

    /** A SPOT (an Electric point, a Bulb, a Fan): its dot, its rim, its symbol, as on 2 · Mark. */
    private fun spot(c: android.graphics.Canvas, x: Float, y: Float, r: Float, t: String, rgb: Int, sel: Boolean, ink: P3Ink) {
        val u = ink.dp
        c.drawCircle(x, y, r, ink.paint(rgb))
        c.drawCircle(x, y, r, if (sel) ink.stroke(0xffffff, 3f * u) else ink.stroke(0x000000, 1.6f * u, 0xaa))
        val k = r * .62f
        val sw = max(1f, r * .16f)
        when (t) {
            "P" -> {
                val p = ink.stroke(0x14160F, sw)
                c.drawLine(x - k * .7f, y - k * .7f, x + k * .7f, y + k * .7f, p)
                c.drawLine(x - k * .7f, y + k * .7f, x + k * .7f, y - k * .7f, p)
            }
            "F" -> {
                val p = ink.paint(0x14160F)
                for (i in 0 until 3) {
                    c.save(); c.translate(x, y); c.rotate(-90f + 120f * i)
                    ink.oval.set(k * .05f, -k * .2f, k * 1.05f, k * .2f); c.drawOval(ink.oval, p)
                    c.restore()
                }
                c.drawCircle(x, y, k * .18f, p)
            }
            else -> {
                val p = ink.paint(0x14160F)
                for (i in listOf(-1f, 1f)) c.drawCircle(x + i * k * .38f, y, max(.8f, k * .17f), p)
            }
        }
    }

    /** The marks: their true four-corner outlines on their surfaces, a point as its spot (sp.js 1141-1158). */
    private fun marks(c: android.graphics.Canvas, v: Pano360.View, fr: Map<String, SurveyPrep.Frame>, prep: SurveyPrep.Prep,
                      selId: String?, ink: P3Ink, w: Float, h: Float) {
        val u = ink.dp
        val drawn = ArrayList<Pair<Pano360.Poly, SurveyPrep.Mark>>()
        for ((s, f) in fr) for (e in prep.surface(s).marks) Pano360.markPoly(v, s, f, e, u.toDouble())?.let { drawn.add(it to e) }
        val sm = drawn.firstOrNull { it.first.id == selId }
        // THE PICKED MARK STANDS OUT: everything round it dimmed 20 % (Amit, 2026-10-10), its outline or spot left clear
        if (sm != null) {
            ink.a = 1f
            val p = ink.path; p.reset(); p.fillType = Path.FillType.EVEN_ODD
            p.addRect(0f, 0f, w, h, Path.Direction.CW)
            val sp = sm.first.spot
            if (sp != null) p.addCircle(sp[0].toFloat(), sp[1].toFloat(), sp[2].toFloat() + 6f * u, Path.Direction.CW)
            else { val q = sm.first.pts; p.moveTo(q[0][0].toFloat(), q[0][1].toFloat()); for (i in 1 until q.size) p.lineTo(q[i][0].toFloat(), q[i][1].toFloat()); p.close() }
            c.drawPath(p, ink.paint(0x000000, 0x33))
            p.fillType = Path.FillType.WINDING
        }
        for ((pl, e) in drawn.sortedBy { if (it.first.id == selId) 1 else 0 }) {
            val s = pl.s; val f = fr.getValue(s)
            val col = SurveyPrep.typeOf(s, e.t).colour; val sel = pl.id == selId
            ink.a = if (sm != null && !sel) 191f / 255f else 1f
            val locked = SurveyPrep.isLocked(e)
            val ab = doubleArrayOf(f.a, f.b)
            val sp = pl.spot
            if (sp != null) {
                val x = sp[0].toFloat(); val y = sp[1].toFloat(); val r = sp[2].toFloat()
                spot(c, x, y, r, e.t, col, sel, ink)
                label(c, e.code, min(w - 40f * u, x + r + 4f * u), max(14f * u, y - r - 2f * u), col, 13f * u, ink)
                if (locked) padPill(c, x + r + 30f * u, y - r - 16f * u, col, ink)
                ink.a = 1f
                if (SurveyPrep.outOf(e, ab[0], ab[1])) {
                    val pd = 4f * u
                    c.drawRect(x - r - pd, y - r - pd, x + r + pd, y + r + pd, ink.stroke(OUT, 2.5f * u, dash = ink.dash75))
                }
                continue
            }
            val path = pathOf(ink, pl.pts)
            c.drawPath(path, ink.paint(col, 0x22))
            c.drawPath(path, ink.stroke(col, (if (sel) 3.5f else 2.5f) * u))
            val tl = pl.pts.minByOrNull { it[1] } ?: pl.pts[0]
            label(c, e.code, min(w - 40f * u, max(4f * u, tl[0].toFloat() + 4f * u)), max(14f * u, tl[1].toFloat() + 15f * u), col, 13f * u, ink)
            if (locked) Pano360.toS(v, s, f, e.x1, e.y1)?.let { tr -> padPill(c, tr[0].toFloat() - 3f * u, tr[1].toFloat() + 3f * u, col, ink) }
            ink.a = 1f
            if (SurveyPrep.outOf(e, ab[0], ab[1])) c.drawPath(pathOf(ink, pl.pts), ink.stroke(OUT, 2.5f * u, dash = ink.dash75))
        }
        ink.a = 1f
    }

    /** THE PLANE GRID on [s] through its perspective (sp.js drawGrid with the 360's clip). */
    private fun grid(c: android.graphics.Canvas, v: Pano360.View, s: String, f: SurveyPrep.Frame, alpha: Float, ink: P3Ink) {
        val a = f.a; val b = f.b
        val c0 = Pano360.toS(v, s, f, a / 2, b / 2); val c1 = Pano360.toS(v, s, f, a / 2 + 100, b / 2)
        val step = SurveyGuide.gridStep(if (c0 != null && c1 != null) hypot(c1[0] - c0[0], c1[1] - c0[1]) else 0.0, ink.dp.toDouble())
        val al = alpha.coerceIn(0f, 1f)
        for (gl in SurveyGuide.gridLines(a, b, step)) {
            val p = if (gl.axis == 0) doubleArrayOf(gl.at, 0.0) else doubleArrayOf(0.0, gl.at)
            val q = if (gl.axis == 0) doubleArrayOf(gl.at, b) else doubleArrayOf(a, gl.at)
            val sg = Pano360.seg(v, s, f, p, q) ?: continue
            seg(c, sg, if (gl.strong) ink.stroke(0xffffff, SurveyGuide.GRID_STRONG_DP.toFloat() * ink.dp, (SurveyGuide.GRID_STRONG_ALPHA * al).roundToInt())
                       else ink.stroke(0xffffff, SurveyGuide.GRID_WEAK_DP.toFloat() * ink.dp, (SurveyGuide.GRID_WEAK_ALPHA * al).roundToInt()))
        }
    }

    /** THE PERSPECTIVE GUIDE (item 7): the two rays through its start, across the face, leaning with the wall. */
    private fun guide(c: android.graphics.Canvas, v: Pano360.View, g: SurveyGuide.Guide, f: SurveyPrep.Frame, alpha: Float, ink: P3Ink) {
        val al = (255 * alpha.coerceIn(0f, 1f)).roundToInt(); val col = SurveyGuide.colour(g.tool)
        val r = SurveyGuide.guideRays(g.o, f.a, f.b, g.snap)
        for ((sg, ax) in listOf(r.h to "h", r.v to "v")) {
            if (sg == null) continue
            val ss = Pano360.seg(v, g.s, f, sg.a, sg.b) ?: continue
            seg(c, ss, ink.stroke(col, r.widthDp(ax).toFloat() * ink.dp, al, ink.dash96))
        }
        Pano360.toS(v, g.s, f, g.o[0], g.o[1])?.let { o ->
            c.drawCircle(o[0].toFloat(), o[1].toFloat(), SurveyGuide.RING_DP.toFloat() * ink.dp, ink.stroke(col, SurveyGuide.RING_W_DP.toFloat() * ink.dp, al))
        }
    }

    /** THE LINES, AREAS AND TAPES on [s] (sp.js drawMeas, drawTapes), then what is being drawn there. */
    private fun meas(c: android.graphics.Canvas, v: Pano360.View, s: String, f: SurveyPrep.Frame, sf: SurveyPrep.Surface,
                     st: State, ink: P3Ink, w: Float, h: Float) {
        for (q in sf.meas) if (q.kind == SurveyMeas.LINE || q.kind == SurveyMeas.AREA)
            line(c, v, s, f, q.kind, q.pts, q.code, st.selId == q.id, false, false, SurveyPrep.isLocked(q), st.ends, ink)
        st.area?.takeIf { st.areaS == s }?.let { line(c, v, s, f, "open", it, null, false, true, false, false, false, ink) }
        st.drag?.takeIf { it.s == s && it.kind == SurveyMeas.LINE }?.let { d ->
            line(c, v, s, f, SurveyMeas.LINE, d.pts(), null, false, true, d.off, false, false, ink) }
        for (q in sf.meas) if (q.kind == SurveyMeas.TAPE && q.pts.size == 2)
            tape(c, v, s, f, q.pts[0], q.pts[1], q.code, st.selId == q.id, false, false, SurveyPrep.isLocked(q), st.ends, ink, w, h)
        st.drag?.takeIf { it.s == s && it.kind == SurveyMeas.TAPE }?.let { d ->
            tape(c, v, s, f, d.from, d.to, null, false, true, d.off, false, false, ink, w, h) }
    }

    private fun line(c: android.graphics.Canvas, v: Pano360.View, s: String, f: SurveyPrep.Frame, kind: String, pts: List<DoubleArray>,
                     code: String?, sel: Boolean, draft: Boolean, off: Boolean, locked: Boolean, handles: Boolean, ink: P3Ink) {
        if (pts.isEmpty()) return
        val u = ink.dp
        val closed = kind == SurveyMeas.AREA; val isLine = kind == SurveyMeas.LINE
        val col = if (draft && off) OUT else LINE_C
        val n = if (closed) pts.size else pts.size - 1
        val drawn = ArrayList<DoubleArray>()
        for (i in 0 until n) {
            val a = pts[i]; val b = pts[(i + 1) % pts.size]
            val sg = Pano360.seg(v, s, f, a, b) ?: continue
            seg(c, sg, ink.stroke(col, (if (sel) 3.5f else 2.4f) * u, dash = if (draft) ink.dash75 else null))
            drawn.add(sg.first); drawn.add(sg.second)
            if (isLine && hypot(sg.second[0] - sg.first[0], sg.second[1] - sg.first[1]) >= 3 * SurveyMeas.HEAD_LEN_DP * u) {
                if (Pano360.toS(v, s, f, a[0], a[1]) != null) head(c, sg.first, sg.second, col, ink)
                if (Pano360.toS(v, s, f, b[0], b[1]) != null) head(c, sg.second, sg.first, col, ink)
            }
        }
        if (closed && !draft && pts.size >= 3) {
            val scr = pts.map { Pano360.toS(v, s, f, it[0], it[1]) }
            if (scr.all { it != null }) c.drawPath(pathOf(ink, scr.map { it!! }), ink.paint(LINE_C, 0x26))
        }
        val hd = sel && !draft && handles
        if (!isLine || hd) for (p in pts) {
            val q = Pano360.toS(v, s, f, p[0], p[1]) ?: continue
            val rr = (if (hd) SurveyGuide.END_R_DP.toFloat() else 3.2f) * u
            c.drawCircle(q[0].toFloat(), q[1].toFloat(), rr, ink.paint(0xffffff))
            c.drawCircle(q[0].toFloat(), q[1].toFloat(), rr, ink.stroke(LINE_C, 1.5f * u))
        }
        if (drawn.isNotEmpty()) {
            val cx = drawn.map { it[0] }.average().toFloat(); val cy = drawn.map { it[1] }.average().toFloat()
            val t = SurveyMeas.lineTag(code, if (kind == "open") SurveyMeas.AREA else kind, pts)
            if (t.isNotEmpty()) label(c, t, cx + (if (locked) 30f else 6f) * u, cy - 6f * u, 0xe0f7ff, 12f * u, ink)
            if (locked && !draft) padPill(c, cx + 26f * u, cy - 21f * u, LINE_C, ink)
        }
    }

    /** A TAPE (item 5): its 25 mm band in the face's own perspective, NARROWING WITH DISTANCE as the wall does -- at least
     *  3.5 dp either side where the tape is (Pano360.tapeHalfMm) -- graduated from its start, its tag past the far end. */
    private fun tape(c: android.graphics.Canvas, v: Pano360.View, s: String, f: SurveyPrep.Frame, a: DoubleArray, b: DoubleArray,
                     code: String?, sel: Boolean, draft: Boolean, off: Boolean, locked: Boolean, handles: Boolean, ink: P3Ink,
                     w: Float, h: Float) {
        val u = ink.dp
        val len = hypot(b[0] - a[0], b[1] - a[1])
        val pa = Pano360.toS(v, s, f, a[0], a[1]); val pb = Pano360.toS(v, s, f, b[0], b[1])
        val col = if (off) OUT else TAPE_C
        if (len > 0) {
            val ppm = Pano360.tapeScale(v, s, f, a, b)
            val hw = SurveyMeas.bandHalfMm(ppm, u.toDouble())
            val cs = Pano360.band(a, b, hw).map { Pano360.toS(v, s, f, it[0], it[1]) }
            if (cs.all { it != null }) {
                val band = pathOf(ink, cs.map { it!! })
                if (sel) c.drawPath(band, ink.stroke(0xffffff, 5f * u))
                c.drawPath(band, ink.paint(col, if (draft) 0xc8 else 0xf0))
                c.drawPath(band, ink.stroke(0x3b2f00, 1f * u))
            } else Pano360.seg(v, s, f, a, b)?.let { sg -> seg(c, sg, ink.stroke(col, 7f * u)) }
            val dx = (b[0] - a[0]) / len; val dy = (b[1] - a[1]) / len; val nx = -dy; val ny = dx
            fun at(k: Double, o: Double) = Pano360.toS(v, s, f, a[0] + dx * k + nx * o, a[1] + dy * k + ny * o)
            for (tk in SurveyMeas.tapeTicks(len)) {
                if (!tk.major && ppm * 100 < 4.0 * u) continue
                val p = at(tk.at, hw) ?: continue
                val q = at(tk.at, hw - 2 * hw * (if (tk.major) .62 else .36)) ?: continue
                c.drawLine(p[0].toFloat(), p[1].toFloat(), q[0].toFloat(), q[1].toFloat(), ink.stroke(0x1d1a0a, (if (tk.major) 1.4f else .9f) * u))
                if (tk.figure && ppm > 0) at(tk.at, -hw - 9.0 * u / ppm)?.let { t ->
                    val txt = tk.at.roundToInt().toString()
                    label(c, txt, t[0].toFloat() - textW(txt, 9f * u, ink) / 2, t[1].toFloat() + 3f * u, TAPE_C, 9f * u, ink)
                }
            }
        }
        if (sel && !draft && handles) for (p in listOfNotNull(pa, pb)) {
            c.drawCircle(p[0].toFloat(), p[1].toFloat(), SurveyGuide.END_R_DP.toFloat() * u, ink.paint(0xffffff))
            c.drawCircle(p[0].toFloat(), p[1].toFloat(), SurveyGuide.END_R_DP.toFloat() * u, ink.stroke(0x3b2f00, 2f * u))
        }
        if (pb != null) {
            val txt = SurveyMeas.tapeTag(if (draft) null else code, len)
            val tw = textW(txt, 12f * u, ink)
            val pw = if (locked && !draft) 26f * u else 0f
            val k = SurveyMeas.tapeTagAt(pa ?: doubleArrayOf(pb[0] - 1, pb[1]), pb, tw.toDouble(), pw.toDouble(), u.toDouble(), w.toDouble(), h.toDouble())
            label(c, txt, k[0].toFloat(), k[1].toFloat(), if (off) 0xffd0d0 else 0xfff7c2, 12f * u, ink)
            if (pw > 0f) padPill(c, k[0].toFloat() + tw + pw, k[1].toFloat() - 13f * u, TAPE_C, ink)
        }
    }

    /** THE CHAINS on [s] (sp.js drawChains), then the one being drawn there. */
    private fun chains(c: android.graphics.Canvas, v: Pano360.View, s: String, f: SurveyPrep.Frame, prep: SurveyPrep.Prep,
                       sf: SurveyPrep.Surface, st: State, ink: P3Ink, w: Float, h: Float) {
        for (k in sf.chains) if (k.nodes.isNotEmpty())
            chain(c, v, s, f, k.nodes, k.axis, k.code, SurveyChain.vals(k, prep.actual, f.a, f.b), st.selId == k.id, false,
                SurveyPrep.isLocked(k), ink, w)
        st.chain?.takeIf { it.s == s && it.nodes.isNotEmpty() }?.let { d ->
            chain(c, v, s, f, d.nodes, d.axis ?: "x", null, null, false, true, false, ink, w) }
    }

    private fun chain(c: android.graphics.Canvas, v: Pano360.View, s: String, f: SurveyPrep.Frame, nodes: List<DoubleArray>,
                      axis: String, code: String?, vals: SurveyChain.Vals?, sel: Boolean, draft: Boolean, locked: Boolean,
                      ink: P3Ink, w: Float) {
        val u = ink.dp; val col = SurveyChain.COLOUR
        val ai = SurveyChain.axisIndex(axis)
        val perp = if (ai == 1) doubleArrayOf(1.0, 0.0) else doubleArrayOf(0.0, 1.0)
        val scr = nodes.map { Pano360.toS(v, s, f, it[0], it[1]) }
        for (i in 1 until nodes.size) {
            val sg = Pano360.seg(v, s, f, nodes[i - 1], nodes[i]) ?: continue
            seg(c, sg, ink.stroke(col, ((if (sel) SurveyChain.LINE_SEL_DP else SurveyChain.LINE_DP) * u).toFloat(), dash = if (draft) ink.dash75 else null))
            if (hypot(sg.second[0] - sg.first[0], sg.second[1] - sg.first[1]) >= 3 * SurveyMeas.HEAD_LEN_DP * u) {
                if (scr[i - 1] != null) head(c, sg.first, sg.second, col, ink)
                if (scr[i] != null) head(c, sg.second, sg.first, col, ink)
            }
        }
        if (locked && !draft) scr[0]?.let { p -> padPill(c, p[0].toFloat() + 8f * u, p[1].toFloat() - 30f * u, col, ink) }
        val r = ((if (sel) SurveyChain.NODE_R_SEL_DP else SurveyChain.NODE_R_DP) * u).toFloat()
        for (j in nodes.indices) {
            val p = scr[j] ?: continue; val n = nodes[j]
            val q = Pano360.toS(v, s, f, n[0] + perp[0] * 100, n[1] + perp[1] * 100)
            if (q != null) {
                val dx = q[0] - p[0]; val dy = q[1] - p[1]; val l = hypot(dx, dy).takeIf { it > 0 } ?: 1.0
                val kk = SurveyChain.TICK_DP * u / l
                c.drawLine((p[0] - dx * kk).toFloat(), (p[1] - dy * kk).toFloat(), (p[0] + dx * kk).toFloat(), (p[1] + dy * kk).toFloat(),
                    ink.stroke(col, (SurveyChain.TICK_W_DP * u).toFloat()))
            }
            c.drawCircle(p[0].toFloat(), p[1].toFloat(), r, ink.paint(0xffffff))
            c.drawCircle(p[0].toFloat(), p[1].toFloat(), r, ink.stroke(col, (SurveyChain.NODE_RING_DP * u).toFloat()))
        }
        val fs = 12f * u; val off = (SurveyChain.FIGURE_OFF_DP * u)
        for (i in 1 until nodes.size) {
            val mid = doubleArrayOf((nodes[i - 1][0] + nodes[i][0]) / 2, (nodes[i - 1][1] + nodes[i][1]) / 2)
            val p = Pano360.toS(v, s, f, mid[0], mid[1]) ?: continue
            val q = Pano360.toS(v, s, f, mid[0] + perp[0] * 100, mid[1] + perp[1] * 100)
            var dx = 0.0; var dy = -1.0
            if (q != null) { val l = hypot(q[0] - p[0], q[1] - p[1]); if (l > 0) { dx = (q[0] - p[0]) / l; dy = (q[1] - p[1]) / l } }
            val (t, tc) = if (vals != null) SurveyChain.segFigure(vals, i - 1)
                          else ("≈" + abs(nodes[i][ai] - nodes[i - 1][ai]).roundToInt()) to SurveyChain.PHOTO_COLOUR
            label(c, t, (p[0] + dx * off).toFloat() - textW(t, fs, ink) / 2, (p[1] + dy * off).toFloat() + 4f * u, tc, fs, ink)
        }
        if (vals != null && code != null) {
            val p = scr.last() ?: return
            val t = SurveyChain.totalTag(code, vals); val ts = 11f * u
            val x = max(4f * u, min(w - textW(t, ts, ink) - 6f * u, p[0].toFloat() + 8f * u))
            label(c, t, x, p[1].toFloat() + 20f * u, SurveyChain.totalColour(vals), ts, ink)
        }
    }

    /** THE REFERENCE ON THE 360 (item 13): its outline in dashed cyan through the face's frame, "Ref 905 × 2053" by its
     *  top-left corner -- never while its eye is off (SurveyRef.faceQuad), though it still squares the face then. */
    private fun ref(c: android.graphics.Canvas, v: Pano360.View, s: String, f: SurveyPrep.Frame, sf: SurveyPrep.Surface,
                    selId: String?, ink: P3Ink) {
        val mm = SurveyRef.faceQuad(sf, f) ?: return
        val r = SurveyRef.of(sf) ?: return
        val sel = selId == SurveyRef.id(s)
        val p = ink.stroke(SurveyRef.COLOUR, (if (sel) 3f else 1.5f) * ink.dp, dash = ink.dash85)
        for (i in 0 until 4) Pano360.seg(v, s, f, mm[i], mm[(i + 1) % 4])?.let { seg(c, it, p) }
        Pano360.toS(v, s, f, mm[3][0], mm[3][1])?.let { q ->
            label(c, SurveyRef.label(r), q[0].toFloat() + 4f * ink.dp, q[1].toFloat() - 6f * ink.dp, SurveyRef.COLOUR, 11f * ink.dp, ink) }
    }
}
