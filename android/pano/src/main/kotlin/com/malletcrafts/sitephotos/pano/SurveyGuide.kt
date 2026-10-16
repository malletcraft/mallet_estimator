package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * THE PERSPECTIVE GUIDE, THE PLANE GRID AND END POINTS THAT MOVE (prototype v52, sp.js 448-457 and 576-626; v52
 * slice 10, items 7 and 8). Pure, so every ray, step, snap, fade and refusal is tested here; SurveyPrepScreen only
 * dispatches and SurveyPrepDraw only draws.
 *
 * Item 7 -- Amit, 14 Oct 2026 20:45 IST: "i need perpective guidance reference shown when i draw any of these two tools
 * so i will know i am drawing and measuring in correct reference"; 06:35: "currently measure line can be drawn in any
 * direction. does it give correct measures if drawn in any direction?". While a Tape or a Line is drawn (or a Line's or
 * a tape's end is dragged) the face's plane grid fades in, even with the grid button off, and two dashed rays in the
 * tool's colour run through the start point along the face's own horizontal and vertical -- constant y and constant x
 * in the face's mm, so the plane and the axis every figure is measured in can be seen. When the drawing ends the grid
 * fades out within 300 ms (unless the grid button is on).
 *
 * Item 8 -- Amit, 14 Oct 2026 06:30 IST: "i can not move measure line once drawn. i should be able to place it by
 * moving its end points." A selected Line or tape shows its two ends as white handles, an area its corners; a drag
 * moves that one point along the face: kept on the face, snapped onto the other end's horizontal or vertical within
 * 5° (a Line and a tape), and onto the face's edges and every mark's edges within 25 mm (14 dp when zoomed out). A
 * locked one is picked and says "F-M1 is locked — unlock to move it".
 */
object SurveyGuide {

    // ------------------------------------------------------------ the fade (sp.js guideState)

    /** The grid and the rays come in at this opacity ... */
    const val FADE_FROM = 0.45
    /** ... and reach full over this many ms. */
    const val FADE_IN_MS = 150
    /** They go out over this many ms once the drawing ends. */
    const val FADE_OUT_MS = 300
    /** Under this opacity nothing is drawn (sp.js `a>.02`). */
    const val SHOWN_MIN = 0.02

    /** The opacity [ms] after the guide came on: .45 → 1 over 150 ms (sp.js `.45+.55*t/150`). */
    fun fadeIn(ms: Double): Double = min(1.0, FADE_FROM + (1 - FADE_FROM) * max(0.0, ms) / FADE_IN_MS)

    /** The opacity [ms] after the drawing ended: 1 → 0 over 300 ms (sp.js `1-t/300`). */
    fun fadeOut(ms: Double): Double = max(0.0, 1 - max(0.0, ms) / FADE_OUT_MS)

    /** How long a fade out from [from] takes, ms: the same slope as from full, so a guide caught mid fade-in does not
     *  linger. Never under 1 ms. */
    fun fadeOutMs(from: Double): Int = (FADE_OUT_MS * from.coerceIn(0.0, 1.0)).roundToInt().coerceAtLeast(1)

    /** Whether a guide or a grid at [alpha] is drawn at all. */
    fun shown(alpha: Double) = alpha > SHOWN_MIN

    // ------------------------------------------------------------ the rays (sp.js drawGuide)

    /** The rays are dashed 9 / 6 dp ... */
    const val DASH_DP = 9.0
    const val GAP_DP = 6.0
    /** ... 1.6 dp wide, 2.8 dp on the axis the drag has snapped onto ... */
    const val RAY_DP = 1.6
    const val RAY_SNAP_DP = 2.8
    /** ... and the start point is ringed: radius 5 dp, 2 dp wide. */
    const val RING_DP = 5.0
    const val RING_W_DP = 2.0
    /** A draft off the face turns this red (sp.js OUTC). */
    const val OFF_COLOUR = 0xff5c5c

    /**
     * WHAT THE GUIDE IS DRAWN FROM: the surface [s], the start point [o] in its mm, the tool ([SurveyPrepPalette.TAPE]
     * or [SurveyPrepPalette.LINE]: the rays take its colour), the axis the drag has snapped onto ("h", "v", "d" or
     * null) and whether the drag is off the face.
     */
    class Guide(val s: String, val o: DoubleArray, val tool: String, val snap: String?, val off: Boolean = false)

    /** The rays' colour: the Tape's yellow, the Chain's rose (v52 slice 11) or the Line's blue (sp.js guideCol). */
    fun colour(tool: String): Int = when (tool) {
        SurveyPrepPalette.TAPE -> SurveyMeas.TAPE_COLOUR
        SurveyPrepPalette.CHAIN -> SurveyChain.COLOUR
        else -> SurveyMeas.LINE_COLOUR
    }

    /**
     * THE GUIDE NOW (sp.js guideNow), in its order: a Tape or a Line being pulled ([drag]: from where the finger went
     * down); an end being dragged ([end]: through the FIXED end, an area's corner through itself); a Line area being
     * tapped on [areaS] ([area]: through its last corner) -- but only with the Line tool on, so a draft left behind
     * under another tool never keeps a guide (ct52 FX11); then a chain being drawn ([chain], v52 slice 11: through its
     * first point, bold on its axis once two points fix it -- SurveyChain.guide). Null: no guide.
     */
    fun now(drag: SurveyMeas.Draft?, end: Guide?, areaS: String?, area: List<DoubleArray>?, tool: String,
            chain: SurveyChain.Draft? = null): Guide? = when {
        drag != null -> Guide(drag.s, drag.from.copyOf(), if (drag.kind == SurveyMeas.TAPE) SurveyPrepPalette.TAPE else SurveyPrepPalette.LINE,
            drag.snap, drag.off)
        end != null -> end
        areaS != null && !area.isNullOrEmpty() && tool == SurveyPrepPalette.LINE -> Guide(areaS, area.last().copyOf(), SurveyPrepPalette.LINE, null)
        else -> SurveyChain.guide(chain)
    }

    /** One straight piece on the face, mm. */
    class Seg(val a: DoubleArray, val b: DoubleArray)

    /**
     * The two rays: [h] along the face's horizontal (constant y) and [v] up its vertical (constant x), each through
     * the start and across the whole face; [snap] the axis the drag is on, whose ray is drawn bold ([widthDp]).
     */
    class Rays(val h: Seg?, val v: Seg?, val snap: String?) {
        /** Whether the ray on [axis] ("h" or "v") is the one snapped onto. */
        fun bold(axis: String) = snap == axis
        /** Its width, dp: 2.8 on the snapped axis, else 1.6. */
        fun widthDp(axis: String) = if (bold(axis)) RAY_SNAP_DP else RAY_DP
    }

    /**
     * THE RAYS THROUGH [o] ON AN [a] x [b] FACE (sp.js drawGuide: `[0,o[1]]-[F.A,o[1]]` and `[o[0],0]-[o[0],F.B]`),
     * clipped to the face: each runs from edge to edge, and a ray whose line lies off the face -- past the tolerance
     * that still counts as on it (SurveyPrep.onFace) -- is not drawn at all.
     */
    fun guideRays(o: DoubleArray, a: Double, b: Double, snap: String? = null): Rays {
        val t = max(25.0, 0.01 * max(a, b))
        val h = if (o[1] >= -t && o[1] <= b + t) Seg(doubleArrayOf(0.0, o[1]), doubleArrayOf(a, o[1])) else null
        val v = if (o[0] >= -t && o[0] <= a + t) Seg(doubleArrayOf(o[0], 0.0), doubleArrayOf(o[0], b)) else null
        return Rays(h, v, snap)
    }

    // ------------------------------------------------------------ the plane grid (sp.js drawGrid)

    /** Grid lines every 100 mm once 100 mm is at least 12 dp on screen, else every 500 mm. */
    const val GRID_FINE = 100
    const val GRID_COARSE = 500
    const val GRID_FINE_DP = 12.0
    /** Every 500 mm (and the far edges) drawn stronger: #ffffffaa at 1.4 dp, the rest #ffffff4d at 0.8 dp. */
    const val GRID_STRONG_ALPHA = 0xaa
    const val GRID_WEAK_ALPHA = 0x4d
    const val GRID_STRONG_DP = 1.4
    const val GRID_WEAK_DP = 0.8

    /** The grid's spacing, mm, at [pxPer100mm] canvas px for 100 mm of face (measured at its centre); [dpPx] px per dp. */
    fun gridStep(pxPer100mm: Double, dpPx: Double): Int = if (pxPer100mm >= GRID_FINE_DP * dpPx) GRID_FINE else GRID_COARSE

    /** One grid line: constant x ([axis] 0, a vertical line) or constant y ([axis] 1) at [at] mm; [strong] every 500. */
    class GridLine(val axis: Int, val at: Double, val strong: Boolean)

    /** The grid's lines on an [a] x [b] face at [step] mm: from 0 to each far edge, every 500 mm strong, and the far
     *  edge itself, strong, when the step does not land on it (sp.js `if(A%step>1)`). */
    fun gridLines(a: Double, b: Double, step: Int): List<GridLine> {
        val out = mutableListOf<GridLine>()
        for ((axis, lim) in listOf(0 to a, 1 to b)) {
            var k = 0
            while (k <= lim + 1e-6) { out.add(GridLine(axis, k.toDouble(), k % GRID_COARSE == 0)); k += step }
            if (lim % step > 1) out.add(GridLine(axis, lim, true))
        }
        return out
    }

    // ------------------------------------------------------------ snapping (sp.js kSnap)

    /** A coordinate after snapping, and what it snapped onto ("FD1 right", "left corner" …; null: nothing). */
    class Snapped(val v: Double, val tag: String?)

    /**
     * [v] mm on [axis] (0: x, 1: y) snapped onto the nearest within [tol] mm of: the face's two edges on that axis --
     * "left corner" / "right corner" and "floor" / "ceiling" on a wall; "left edge" / "right edge" and "back edge" /
     * "front edge" on the floor and the ceiling -- and every one of [marks]' two edges on it ("FD1 left", "FD1 right",
     * "FD1 bottom", "FD1 top"). The nearest wins; on a tie, the first in that order. Nothing within [tol]: [v] as it is.
     */
    fun snapCoord(s: String, a: Double, b: Double, marks: List<SurveyPrep.Mark>, v: Double, axis: Int, tol: Double): Snapped {
        val plan = SurveyPrep.isPlan(s)
        val cand = mutableListOf<Pair<Double, String>>()
        if (axis == 1) { cand.add(0.0 to (if (plan) "back edge" else "floor")); cand.add(b to (if (plan) "front edge" else "ceiling")) }
        else { cand.add(0.0 to (if (plan) "left edge" else "left corner")); cand.add(a to (if (plan) "right edge" else "right corner")) }
        for (e in marks) {
            if (axis == 1) { cand.add(e.y0 to e.code + " bottom"); cand.add(e.y1 to e.code + " top") }
            else { cand.add(e.x0 to e.code + " left"); cand.add(e.x1 to e.code + " right") }
        }
        var best: Pair<Double, String>? = null; var bd = 0.0
        for (c in cand) { val d = abs(v - c.first); if (d <= tol && (best == null || d < bd)) { best = c; bd = d } }
        return best?.let { Snapped(it.first, it.second) } ?: Snapped(v, null)
    }

    // ------------------------------------------------------------ end points (sp.js endHit, endMove, endGrab)

    /** An end's handle: radius 5.5 dp, drawn white on the selected item with Select on ... */
    const val END_R_DP = 5.5
    /** ... grabbed within 14 dp. */
    const val END_HIT_DP = 14.0
    /** An end snaps onto an edge within 25 mm, or 14 dp when that is more (zoomed out). */
    const val SNAP_MM = 25.0
    const val SNAP_DP = 14.0

    /** What a locked end says when it is dragged (sp.js endGrab): "F-M1 is locked — unlock to move it". */
    fun lockedEnd(code: String) = "$code is locked — unlock to move it"

    /** The snapping distance, mm: 25 mm, or 14 dp at the current scale ([mmPerPx] face mm per canvas px, [dpPx] px per
     *  dp) when that is more. */
    fun endTol(dpPx: Double, mmPerPx: Double): Double = max(SNAP_MM, SNAP_DP * dpPx * mmPerPx)

    /** Which of an item's points (on screen, canvas px, [scr]) a press at ([x], [y]) is on: the nearest within [r];
     *  null: none. */
    fun endAt(scr: List<DoubleArray>, x: Double, y: Double, r: Double): Int? {
        var best: Int? = null; var bd = 0.0
        scr.forEachIndexed { i, q -> val d = hypot(q[0] - x, q[1] - y); if (d < r && (best == null || d < bd)) { best = i; bd = d } }
        return best
    }

    /**
     * Where an end goes: the point [p] (mm, 0.1), the axis it snapped onto the other end along ([snap]: "h", "v", "d";
     * null for an area's corner, which has no other end), and the point the guide runs through, [o] -- the FIXED end of
     * a Line or a tape, an area's corner itself.
     */
    class EndMove(val p: DoubleArray, val snap: String?, val o: DoubleArray)

    /**
     * POINT [i] OF [m] DRAGGED TO [mm] ON SURFACE [s] (sp.js endMove), in this order: kept inside the [a] x [b] face;
     * for a Line or a tape, snapped onto the other end's horizontal or vertical within 5° (SurveyMeas.axisSnap); then,
     * on each axis the 5° snap did not fix, onto the face's edges and [marks]' edges within [tol] mm ([snapCoord]);
     * rounded to 0.1 mm. A locked [m] does not move: null.
     */
    fun endMove(s: String, m: SurveyPrep.Meas, i: Int, mm: DoubleArray, a: Double, b: Double,
                marks: List<SurveyPrep.Mark>, tol: Double): EndMove? {
        if (SurveyPrep.isLocked(m) || i !in m.pts.indices) return null
        var p = doubleArrayOf(mm[0].coerceIn(0.0, a), mm[1].coerceIn(0.0, b))
        var snap: String? = null
        val o = if (m.kind == SurveyMeas.AREA || m.pts.size != 2) null else m.pts[1 - i].copyOf()
        if (o != null) { val r = SurveyMeas.axisSnap(o, p); p = r.p; snap = r.snap }
        if (snap != "v") p[0] = snapCoord(s, a, b, marks, p[0], 0, tol).v
        if (snap != "h") p[1] = snapCoord(s, a, b, marks, p[1], 1, tol).v
        val q = doubleArrayOf(Math.round(p[0] * 10) / 10.0, Math.round(p[1] * 10) / 10.0)
        return EndMove(q, snap, o ?: q.copyOf())
    }

    /** The guide an end drag shows: through [EndMove.o], in the item's tool colour, on the axis it snapped onto. */
    fun endGuide(s: String, m: SurveyPrep.Meas, r: EndMove): Guide =
        Guide(s, r.o.copyOf(), if (m.kind == SurveyMeas.TAPE) SurveyPrepPalette.TAPE else SurveyPrepPalette.LINE, r.snap)

    // ------------------------------------------------------------ the press on an end, and what the drag does after (review)

    /** The snapping distance [endTol] needs, face mm per canvas px: the squared picture's mm per px ([sqMpp]) over its
     *  canvas px per squared px ([k], the zoom included). */
    fun mmPerCanvasPx(sqMpp: Double, k: Double): Double = sqMpp / k

    /** What a press on the selected item's end is (sp.js hit, endHit, endGrab). */
    sealed class EndPress {
        /** Select on: point [i] is grabbed and dragged. */
        class Grab(val i: Int) : EndPress()
        /** Select on, the item locked: point [i] is picked and does not move; a drag says [lockedEnd] of [code]. */
        class Locked(val i: Int, val code: String) : EndPress()
        /** Tape or Line on: a NEW one starts from exactly point [i], [mm] on the face -- whether or not the item is
         *  locked, since nothing of it moves (ct52 FX13). */
        class StartNew(val i: Int, val mm: DoubleArray) : EndPress()
    }

    /**
     * HIT-ORDER STEP 0 (v52 slice 10, sp.js hit): a press at ([x], [y]) canvas px within [rPx] of one of [sel]'s points
     * ([scr], on screen, in its order) -- [sel] being the item selected on this face, or null. Only in 2 · Marks
     * ([mark]) with Select, Tape or Line on ([tool]); never while a Line area is being tapped ([areaOpen]: its taps are
     * corners, sp.js `if(st.mdraft||st.kdraft) return null`); only a Tape, a Line or an area. Null: the press goes on
     * down the hit order.
     */
    fun endPress(mark: Boolean, tool: String, sel: SurveyPrep.Meas?, areaOpen: Boolean, scr: List<DoubleArray>,
                 x: Double, y: Double, rPx: Double): EndPress? {
        // Select, Tape or Line -- never the Chain, whose taps are its points (sp.js hit: `st.tool==='M'||st.tool==='T'`)
        if (!mark || !(tool == SurveyPrepPalette.SEL || SurveyPrepPalette.isMeasTool(tool))) return null
        if (areaOpen || sel == null || !SurveyMeas.known(sel.kind) || scr.size != sel.pts.size) return null
        val i = endAt(scr, x, y, rPx) ?: return null
        return when {
            tool != SurveyPrepPalette.SEL -> EndPress.StartNew(i, sel.pts[i].copyOf())
            SurveyPrep.isLocked(sel) -> EndPress.Locked(i, sel.code)
            else -> EndPress.Grab(i)
        }
    }

    /**
     * A NEW TAPE OR LINE FROM A SELECTED END follows the end PLUS the finger's movement, never the finger itself (sp.js
     * measureGrab: `x0:at`, moved by `(x - sx0)/k`): [start] the end, [down] where the finger went down and [finger]
     * where it is, all face mm. So the finger's miss of the end (up to 14 dp) never reaches the new one's length or
     * angle: pressed 10 dp under an end and pulled level, it is level at the end's height.
     */
    fun dragFrom(start: DoubleArray, down: DoubleArray, finger: DoubleArray): DoubleArray =
        doubleArrayOf(start[0] + finger[0] - down[0], start[1] + finger[1] - down[1])

    /** An end does not move until the finger has gone this far from its down point -- the locked refusal's 4 dp at
     *  least; the screen uses the touch slop when that is more, so a press that lifts as a tap never moved its end. */
    const val END_DEAD_DP = 4.0

    /**
     * A TAP ON AN END MOVES NOTHING: a grabbed press is reported from the first pixel (GestureClassifier), and one
     * pixel of jitter would snap the end up to 14 dp onto an edge. [move] is false until the finger has gone more than
     * [deadPx] from its down point, and true from then on, even back inside it.
     */
    class EndGate(private val deadPx: Double) {
        var live = false; private set
        fun move(dx: Double, dy: Double): Boolean { if (!live && hypot(dx, dy) > deadPx) live = true; return live }
    }

    /** A press on an end that ends as a tap (or a double tap) after it did move -- past the dead zone, inside the
     *  slop: put back, with its undo step, as a long press is. */
    fun revertOnTap(endGrab: Boolean, moved: Boolean) = endGrab && moved

    /** A cancel during an end drag puts the end back only for a SECOND FINGER (ct52 FX3); a system cancel (palm
     *  rejection) keeps what was dragged, as a lift does and as sp.js's pointercancel does (`up` -> `gr.end(moved)`). */
    fun revertOnCancel(secondFinger: Boolean, moved: Boolean) = secondFinger && moved

    /** The floating trash keeps its disc this far (dp) clear of the selected item's end handles: the handle's 5.5 dp
     *  and a 2 dp margin, so the disc never covers a handle -- and the press on a handle is the end's (hit order). */
    const val HANDLE_CLEAR_DP = END_R_DP + 2.0
}
