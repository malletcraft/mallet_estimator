package com.malletcrafts.interiorestimate

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.malletcrafts.interiorestimate.pano.SurveyChain
import com.malletcrafts.interiorestimate.pano.SurveyGuide
import com.malletcrafts.interiorestimate.pano.SurveyMeas
import com.malletcrafts.interiorestimate.pano.SurveyPrep
import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch
import com.malletcrafts.interiorestimate.pano.SurveyRef
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Drawing for survey prep, on an android.graphics.Canvas, so the screen and
 * the saved pictures draw marks the same way. A port of mock-up v33's canvas
 * code: every size there is in the mock-up's canvas pixels, and [u] scales
 * them (the screen passes its width / 480, a saved picture its width / 960).
 *
 * [m] maps picture pixels (a view's, or a squared surface's) to the canvas.
 */
internal object SurveyPrepDraw {

    /** Bottom, top, left and right line colours. */
    val LINE_COLOUR = mapOf("bottom" to 0xff8a3d, "top" to 0xe0b040, "left" to 0x4dabf7, "right" to 0xda77f2)

    /** A mark past its face (the room made smaller under it): outlined in this red, dashed (v51). */
    const val OUT_COLOUR = 0xff5c5c

    fun argb(rgb: Int, alpha: Int = 255): Int = (alpha shl 24) or (rgb and 0xFFFFFF)

    private val MONO_BOLD: Typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

    fun label(c: Canvas, text: String, x: Float, y: Float, colour: Int, size: Float, u: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; isFakeBoldText = true }
        p.style = Paint.Style.STROKE; p.strokeWidth = 3f * u; p.color = argb(0x000000, 0xcc)
        c.drawText(text, x, y, p)
        p.style = Paint.Style.FILL; p.color = argb(colour)
        c.drawText(text, x, y, p)
    }

    private fun diamond(c: Canvas, x: Float, y: Float, colour: Int, r: Float, u: Float) {
        val path = Path().apply { moveTo(x, y - r); lineTo(x + r, y); lineTo(x, y + r); lineTo(x - r, y); close() }
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL; p.color = argb(colour, 0xee); c.drawPath(path, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.5f * u; p.color = argb(0xffffff); c.drawPath(path, p)
        p.style = Paint.Style.FILL; c.drawCircle(x, y, 3f * u, p)
    }

    private fun stroke(colour: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = width; color = colour
    }

    /** The padlock glyph, [sz] across, centred on ([x], [y]) (sp.js lockGlyph; v52 slice 8). */
    private fun lockGlyph(c: Canvas, x: Float, y: Float, sz: Float, colour: Int) {
        val bw = sz * .72f; val bh = sz * .48f; val r = sz * .22f
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colour; style = Paint.Style.STROKE; strokeWidth = max(1f, sz * .13f) }
        val path = Path().apply {
            moveTo(x - r, y + sz * .04f); lineTo(x - r, y - sz * .06f)
            arcTo(RectF(x - r, y - sz * .06f - r, x + r, y - sz * .06f + r), 180f, 180f, false)
            lineTo(x + r, y + sz * .04f)
        }
        c.drawPath(path, p)
        p.style = Paint.Style.FILL
        c.drawRect(x - bw / 2, y + sz * .02f, x + bw / 2, y + sz * .02f + bh, p)
    }

    /** A padlock on a dark pill, its top-right corner at ([x], [y]), in the mark's [colour] (sp.js padPill). */
    private fun padPill(c: Canvas, x: Float, y: Float, colour: Int, u: Float) {
        val sz = 12f * u; val pw = sz + 6f * u; val ph = sz + 5f * u; val x0 = x - pw
        c.drawRoundRect(RectF(x0, y, x, y + ph), ph / 2, ph / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(0x000000, 0xd9) })
        lockGlyph(c, x0 + pw / 2, y + ph / 2 - sz * .14f, sz, argb(colour))
    }

    // ------------------------------------------------------------ 1 · Lines

    /**
     * The four boundary lines of a surface, drawn long so they read as lines
     * and not segments; a solid line has been set by hand, a dashed one not
     * yet. No numbers -- only the boundary. [thin] is the magnifier's version.
     */
    fun drawLines(c: Canvas, s: String, sf: SurveyPrep.Surface, frame: SurveyPrep.Frame?,
                  m: (Double, Double) -> FloatArray, u: Float, thin: Boolean) {
        val ext = 2.0 * SurveyPrep.VW
        val locked = SurveyPrep.linesLocked(sf)
        for (k in SurveyPrep.SLOTS) {
            val l = sf.lines.getValue(k); val set = sf.set[k] == true
            val dx = l[1][0] - l[0][0]; val dy = l[1][1] - l[0][1]; val n = hypot(dx, dy).takeIf { it > 0 } ?: 1.0
            val a = m(l[0][0] - dx / n * ext, l[0][1] - dy / n * ext); val b = m(l[0][0] + dx / n * ext, l[0][1] + dy / n * ext)
            val p = stroke(argb(LINE_COLOUR.getValue(k)), (if (thin) 1.5f else if (set) 3f else 2.2f) * u)
            if (!set) p.pathEffect = DashPathEffect(floatArrayOf(10f * u, 7f * u), 0f)
            c.drawLine(a[0], a[1], b[0], b[1], p)
        }
        if (thin) return
        frame?.q?.forEach { q -> val s0 = m(q[0], q[1]); c.drawCircle(s0[0], s0[1], 6f * u, stroke(argb(0xffffff), 2f * u)) }
        for (k in SurveyPrep.SLOTS) for (pt in sf.lines.getValue(k)) {
            val q = m(pt[0], pt[1]); diamond(c, q[0], q[1], LINE_COLOUR.getValue(k), 14f * u, u)
            // LOCKED LINES (v52 slice 8, sp.js 261): each grip carries a padlock on a disc of its colour
            if (locked) {
                c.drawCircle(q[0], q[1], 7f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(LINE_COLOUR.getValue(k)) })
                lockGlyph(c, q[0], q[1] - 1f * u, 11f * u, argb(0x14160F))
            }
        }
        val names = SurveyPrep.lineNames(s)
        for (k in SurveyPrep.SLOTS) {
            val p = sf.lines.getValue(k)[1]; val q = m(p[0], p[1])
            label(c, names.getValue(k), q[0] + 16f * u, q[1] - 10f * u, 0xffffff, 12f * u, u)
        }
    }

    /**
     * THE REFERENCE RECTANGLE ON 1 · LINES (v52 slice 12, sp.js drawRef): while it is on and placed -- shown or hidden,
     * since Lines is where it is worked on -- its outline in cyan #22d3ee, dashed 8 / 5, filled #22d3ee22; its four
     * corners as diamonds dragged like line ends; "Ref 905 × 2053" by its topmost corner, with a padlock pill when
     * locked. Not in the magnifier.
     */
    fun drawRef(c: Canvas, s: String, sf: SurveyPrep.Surface, m: (Double, Double) -> FloatArray, u: Float) {
        val r = SurveyRef.of(sf)?.takeIf { SurveyRef.placed(it) } ?: return
        val pts = r.q!!.map { m(it[0], it[1]) }
        val path = Path().apply { moveTo(pts[0][0], pts[0][1]); for (i in 1 until pts.size) lineTo(pts[i][0], pts[i][1]); close() }
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = argb(SurveyRef.COLOUR, 0x22) })
        c.drawPath(path, stroke(argb(SurveyRef.COLOUR), 2.5f * u).apply { pathEffect = DashPathEffect(floatArrayOf(8f * u, 5f * u), 0f) })
        for (p in pts) diamond(c, p[0], p[1], SurveyRef.COLOUR, 13f * u, u)
        val top = pts.minByOrNull { it[1] } ?: return
        label(c, SurveyRef.label(r), top[0] + (if (r.locked) 30f else 14f) * u, top[1] - 8f * u, SurveyRef.COLOUR, 12f * u, u)
        if (r.locked) padPill(c, top[0] + 28f * u, top[1] - 24f * u, SurveyRef.COLOUR, u)
    }

    /**
     * THE REFERENCE ON A SQUARED FACE (v52 slice 12, sp.js drawRefSq): 2 · Mark, 3 · List, the List tab and the saved
     * pictures draw its outline through the face's ONE frame -- thin dashed cyan, 3 when picked -- and "Ref 905 × 2053"
     * by its top-left corner; never while its eye is off (SurveyRef.faceQuad), and it still squares the face then.
     */
    private fun drawRefSq(c: Canvas, s: String, o: Marks, m: (Double, Double) -> FloatArray, u: Float) {
        val sf = o.prep.surface(s)
        val mm = SurveyRef.faceQuad(sf, o.frame) ?: return
        val r = SurveyRef.of(sf) ?: return
        val sq = o.sq
        val pts = mm.map { m(sq.x(it[0]), sq.y(it[1])) }
        val sel = o.selId == SurveyRef.id(s)
        val p = stroke(argb(SurveyRef.COLOUR), (if (sel) 3f else 1.5f) * u).apply { pathEffect = DashPathEffect(floatArrayOf(8f * u, 5f * u), 0f) }
        for (i in 0 until 4) { val a = pts[i]; val b = pts[(i + 1) % 4]; c.drawLine(a[0], a[1], b[0], b[1], p) }
        label(c, SurveyRef.label(r), pts[3][0] + 4f * u, pts[3][1] - 6f * u, SurveyRef.COLOUR, (if (o.all) 22f else 11f) * u, u)
    }

    // ------------------------------------------------------------ 2 · Mark

    /** What the marks are drawn with: the room, the selection, the Show toggle. */
    class Marks(
        val prep: SurveyPrep.Prep,
        val a: Double, val b: Double,
        val sq: SurveyPrep.Square,
        /** "main" (the editing photo), "loupe" (thin), or "sheet" (a list picture / a saved one). */
        val mode: String,
        /** Every mark with its dimensions and big labels, as on the List tab. */
        val all: Boolean,
        val selId: String?,
        val active: String?,
        /** "derived", "actual" or "both". */
        val show: String,
        /** Canvas bounds, for keeping tags on it. */
        val width: Float, val height: Float,
        /** Filled with the number tags drawn, for taps (SurveyPrepTouch.hit). */
        val tags: MutableList<SurveyPrepTouch.Tag>?,
        /** Screen px per dp: the picked mark's tags stand at least 6 dp off its tabs; the Line and the Tape (v52
         *  slice 9) are sized in it. */
        val dp: Float = 1f,
        /** A Tape or a Line being pulled on this surface (v52 slice 9): drawn dashed (a Line) or translucent (a tape). */
        val drag: SurveyMeas.Draft? = null,
        /** The corners of a Line area being tapped on this surface: drawn open and dashed. */
        val area: List<DoubleArray>? = null,
        /** v52 slice 10: the plane grid's opacity on the editing photo (0: none) -- 1 with the grid button on, else the
         *  guide's fade. */
        val gridAlpha: Float = 0f,
        /** The perspective guide (SurveyGuide.now) and its opacity: two dashed rays and the start ring. */
        val guide: SurveyGuide.Guide? = null,
        val guideAlpha: Float = 0f,
        /** The picked Line's or tape's ends, and an area's corners, are handles (Select on). */
        val ends: Boolean = false,
        /** v52 slice 11: a chain being drawn on this surface -- dashed, its segments' photo figures. */
        val chain: SurveyChain.Draft? = null,
        /** v52 slice 12: the face's ONE frame (SurveyPrep.frameOf), through which its reference rectangle is drawn; null
         *  draws none. */
        val frame: SurveyPrep.Frame? = null,
    )

    /**
     * A point's spot on the canvas (v52): its centre x, y and its radius -- the 50 mm it is
     * across on the face, never under 6 dp, so never under 12 dp across ([dp]: px per dp).
     */
    fun spotOf(e: SurveyPrep.Mark, sq: SurveyPrep.Square, m: (Double, Double) -> FloatArray, dp: Float): FloatArray {
        val cx = (e.x0 + e.x1) / 2; val cy = (e.y0 + e.y1) / 2
        val c = m(sq.x(cx), sq.y(cy)); val c2 = m(sq.x(cx + SurveyPrep.SPOT / 2), sq.y(cy))
        return floatArrayOf(c[0], c[1], max(6f * dp, hypot(c2[0] - c[0], c2[1] - c[1])))
    }

    /** The symbols' ink on a spot. */
    private const val SPOT_INK = 0x14160F

    /**
     * A SPOT (Amit, 11 Oct 2026 20:35: "electric points are spots . dont let them resize. have
     * its da 50 mm."): a filled dot in the type's colour, a 1.6 dp dark rim (3 dp white when
     * picked), and its symbol -- Electric two dots, Bulb an X, Fan three blades round a hub.
     * No resize tabs, ever. As sp.js spot().
     */
    private fun spot(c: Canvas, x: Float, y: Float, r: Float, t: String, col: Int, sel: Boolean, thin: Boolean, dp: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL; p.color = argb(col); c.drawCircle(x, y, r, p)
        if (!thin) {
            p.style = Paint.Style.STROKE; p.strokeWidth = (if (sel) 3f else 1.6f) * dp
            p.color = if (sel) argb(0xffffff) else argb(0x000000, 0xaa); c.drawCircle(x, y, r, p)
        }
        val k = r * .62f
        p.color = argb(SPOT_INK); p.strokeWidth = max(1f, r * .16f); p.strokeCap = Paint.Cap.ROUND
        when (t) {
            "P" -> {
                p.style = Paint.Style.STROKE
                c.drawLine(x - k * .7f, y - k * .7f, x + k * .7f, y + k * .7f, p)
                c.drawLine(x - k * .7f, y + k * .7f, x + k * .7f, y - k * .7f, p)
            }
            "F" -> {
                p.style = Paint.Style.FILL
                for (i in 0 until 3) {
                    c.save(); c.translate(x, y); c.rotate(-90f + 120f * i)
                    c.drawOval(RectF(k * .05f, -k * .2f, k * 1.05f, k * .2f), p)
                    c.restore()
                }
                c.drawCircle(x, y, k * .18f, p)
            }
            else -> {
                p.style = Paint.Style.FILL
                for (i in listOf(-1f, 1f)) c.drawCircle(x + i * k * .38f, y, max(.8f, k * .17f), p)
            }
        }
    }

    fun drawMarks(c: Canvas, s: String, o: Marks, m: (Double, Double) -> FloatArray, u: Float) {
        val sq = o.sq; val thin = o.mode == "loupe"
        val els = o.prep.surface(s).marks
        val ta = m(sq.x(0.0), sq.y(o.b)); val tb = m(sq.x(o.a), sq.y(0.0))
        c.drawRect(ta[0], ta[1], tb[0], tb[1], stroke(argb(0xffffff, 0x99), 1.5f * u).apply {
            pathEffect = DashPathEffect(floatArrayOf(6f * u, 5f * u), 0f) })
        // DIM THE REST while one mark is being measured (Amit, 2026-10-10: "dim out rest of the foto when a
        // particular mark type is under selection for measurment") -- 20%, "so that rest foto can be seen".
        val sm = if (o.mode == "main" && o.selId != null) els.firstOrNull { it.id == o.selId } else null
        if (sm != null) {
            val pad = 6f * u
            // a picked spot: the hole is round its dot, as drawn
            val hole = if (SurveyPrep.isPoint(sm.t)) spotOf(sm, sq, m, o.dp).let { d ->
                floatArrayOf(d[0] - d[2], d[1] - d[2], d[0] + d[2], d[1] + d[2]) }
            else { val p = m(sq.x(sm.x0), sq.y(sm.y1)); val q = m(sq.x(sm.x1), sq.y(sm.y0)); floatArrayOf(p[0], p[1], q[0], q[1]) }
            val path = Path().apply {
                fillType = Path.FillType.EVEN_ODD
                addRect(0f, 0f, o.width, o.height, Path.Direction.CW)
                addRect(hole[0] - pad, hole[1] - pad, hole[2] + pad, hole[3] + pad, Path.Direction.CW)
            }
            c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(0x000000, 0x33) })
        }
        for (e in els.sortedBy { if (sm != null && it === sm) 1 else 0 }) {
            val col = SurveyPrep.typeOf(s, e.t).colour
            val p = m(sq.x(e.x0), sq.y(e.y1)); val q = m(sq.x(e.x1), sq.y(e.y0)); val sel = o.selId == e.id
            val faded = sm != null && !sel
            // a locked mark (v52 slice 8): a padlock beside its code on the editing photo, and no resize tabs
            val locked = SurveyPrep.isLocked(e)
            if (faded) c.saveLayerAlpha(null, 191)
            if (SurveyPrep.isPoint(e.t)) {
                // A SPOT: its dot and symbol, its code beside it, its two figures when picked -- no tabs
                val d = spotOf(e, sq, m, o.dp)
                spot(c, d[0], d[1], d[2], e.t, col, sel, thin, o.dp)
                if (!thin) {
                    label(c, e.code, d[0] + d[2] + 4f * u, d[1] - d[2] - 2f * u, col, (if (o.all) 26f else 13f) * u, u)
                    if (locked && o.mode == "main") padPill(c, d[0] + d[2] + 30f * u, d[1] - d[2] - 16f * u, col, u)
                    if (sel || o.all) dims(c, s, e, o, m, p, q, (if (o.all) 22f else 12f) * u, u)
                }
                if (faded) c.restore()
                if (!thin && SurveyPrep.outOf(e, o.a, o.b)) {
                    val pd = 4f * u
                    c.drawRect(d[0] - d[2] - pd, d[1] - d[2] - pd, d[0] + d[2] + pd, d[1] + d[2] + pd, stroke(argb(OUT_COLOUR), 2.5f * u).apply {
                        pathEffect = DashPathEffect(floatArrayOf(7f * u, 5f * u), 0f) })
                }
                continue
            }
            c.drawRect(p[0], p[1], q[0], q[1], Paint().apply { color = argb(col, 0x22) })
            c.drawRect(p[0], p[1], q[0], q[1], stroke(argb(col), (if (thin) 1.5f else if (sel) 3.5f else 2.5f) * u))
            if (!thin) {
                label(c, e.code, p[0] + 6f * u, p[1] + (if (o.all) 30f else 16f) * u, col, (if (o.all) 26f else 13f) * u, u)
                if (locked && o.mode == "main") padPill(c, q[0] - 3f * u, p[1] + 3f * u, col, u)
                if (sel && o.mode == "main" && !locked) {
                    val xm = (p[0] + q[0]) / 2; val ym = (p[1] + q[1]) / 2; val h = 5f * u
                    for ((tx, ty) in listOf(p[0] to p[1], q[0] to p[1], q[0] to q[1], p[0] to q[1], xm to p[1], xm to q[1], p[0] to ym, q[0] to ym)) {
                        c.drawRect(tx - h, ty - h, tx + h, ty + h, Paint().apply { color = argb(0xffffff) })
                        c.drawRect(tx - h, ty - h, tx + h, ty + h, stroke(argb(col), 2f * u))
                    }
                }
                if (sel || o.all) dims(c, s, e, o, m, p, q, (if (o.all) 22f else 12f) * u, u)
            }
            if (faded) c.restore()
            // FLAGGED, NOT MOVED (v51): a mark the room size no longer holds keeps its millimetres and gets a
            // red dashed outline, until it is edited or deleted -- drawn OUTSIDE the faded layer, at full
            // strength even while another mark is picked (sp.js: g.globalAlpha=1 for this outline)
            if (!thin && SurveyPrep.outOf(e, o.a, o.b)) {
                val pd = 4f * u
                c.drawRect(p[0] - pd, p[1] - pd, q[0] + pd, q[1] + pd, stroke(argb(OUT_COLOUR), 2.5f * u).apply {
                    pathEffect = DashPathEffect(floatArrayOf(7f * u, 5f * u), 0f) })
            }
        }
        // THE LINES, AREAS AND TAPES (v52 slice 9) over the marks, as sp.js draws them after drawMarks -- and NOT in
        // the loupe (sp.js overlay: `if(which!=='loupe'){ ... drawMeas ... drawTapes }`), which shows the photo under
        // the finger: a tape being pulled ends at the loupe's centre, and its band would hide what is being aimed at
        // v52 slice 10: the plane grid over the marks and under the Lines and tapes, then the guide over everything --
        // as sp.js overlay draws them: drawGrid, drawMeas, drawTapes, drawGuide; none of it in the loupe
        fun face(x: Double, y: Double) = m(sq.x(x), sq.y(y))
        if (o.mode == "main" && o.gridAlpha > 0f) drawGrid(c, o.a, o.b, ::face, o.gridAlpha, o.dp)
        if (o.mode != "loupe") drawMeas(c, s, o, m)
        // v52 slice 11: the chains after the Lines and tapes, as sp.js overlay draws them (drawMeas, drawTapes, drawChains)
        // -- not in the loupe either
        if (o.mode != "loupe") drawChains(c, s, o, m)
        // v52 slice 12: the reference rectangle after the chains, as sp.js overlay draws it (drawRefSq) -- not in the loupe
        if (o.mode != "loupe") drawRefSq(c, s, o, m, u)
        if (o.mode == "main") o.guide?.takeIf { it.s == s && o.guideAlpha > 0f }?.let { g -> drawGuide(c, g, o.a, o.b, ::face, o.guideAlpha, o.dp) }
    }

    // ------------------------------------------------------------ v52 slice 10: the plane grid and the guide

    /**
     * THE PLANE GRID on an [a] x [b] face (sp.js drawGrid): a line every 100 mm once 100 mm is 12 dp on screen, else
     * every 500 mm (SurveyGuide.gridStep, measured at the face's centre), every 500 mm stronger (#ffffffaa at 1.4 dp,
     * the rest #ffffff4d at 0.8 dp), at [alpha]. [toS] maps face mm to the canvas -- the squared picture's scale in
     * 2 · Mark, the face's own perspective mapping in 1 · Lines, so there it leans with the photo.
     */
    fun drawGrid(c: Canvas, a: Double, b: Double, toS: (Double, Double) -> FloatArray, alpha: Float, dp: Float) {
        if (!(a > 0 && b > 0) || alpha <= 0f) return
        val c0 = toS(a / 2, b / 2); val c1 = toS(a / 2 + 100, b / 2)
        val step = SurveyGuide.gridStep(hypot(c1[0] - c0[0], c1[1] - c0[1]).toDouble(), dp.toDouble())
        val al = alpha.coerceIn(0f, 1f)
        val strong = stroke(argb(0xffffff, (SurveyGuide.GRID_STRONG_ALPHA * al).roundToInt()), SurveyGuide.GRID_STRONG_DP.toFloat() * dp)
        val weak = stroke(argb(0xffffff, (SurveyGuide.GRID_WEAK_ALPHA * al).roundToInt()), SurveyGuide.GRID_WEAK_DP.toFloat() * dp)
        for (gl in SurveyGuide.gridLines(a, b, step)) {
            val p = if (gl.axis == 0) toS(gl.at, 0.0) else toS(0.0, gl.at)
            val q = if (gl.axis == 0) toS(gl.at, b) else toS(a, gl.at)
            c.drawLine(p[0], p[1], q[0], q[1], if (gl.strong) strong else weak)
        }
    }

    /**
     * THE PERSPECTIVE GUIDE (sp.js drawGuide; Amit, 14 Oct 2026 20:45 IST: "i need perpective guidance reference shown
     * when i draw any of these two tools"): two rays in the tool's colour through the start, along the face's horizontal
     * and vertical and across the whole face (SurveyGuide.guideRays), dashed 9 / 6 dp, 1.6 dp wide and 2.8 on the axis
     * the drag has snapped onto; the start ringed, 5 dp. At [alpha] (the fade).
     */
    fun drawGuide(c: Canvas, g: SurveyGuide.Guide, a: Double, b: Double, toS: (Double, Double) -> FloatArray, alpha: Float, dp: Float) {
        val col = argb(SurveyGuide.colour(g.tool), (255 * alpha.coerceIn(0f, 1f)).roundToInt())
        val r = SurveyGuide.guideRays(g.o, a, b, g.snap)
        for ((sg, ax) in listOf(r.h to "h", r.v to "v")) {
            if (sg == null) continue
            val p = toS(sg.a[0], sg.a[1]); val q = toS(sg.b[0], sg.b[1])
            c.drawLine(p[0], p[1], q[0], q[1], stroke(col, r.widthDp(ax).toFloat() * dp).apply {
                pathEffect = DashPathEffect(floatArrayOf(SurveyGuide.DASH_DP.toFloat() * dp, SurveyGuide.GAP_DP.toFloat() * dp), 0f) })
        }
        val o = toS(g.o[0], g.o[1])
        c.drawCircle(o[0], o[1], SurveyGuide.RING_DP.toFloat() * dp, stroke(col, SurveyGuide.RING_W_DP.toFloat() * dp))
    }

    // ------------------------------------------------------------ v52 slice 9: Line and Tape

    /** A Line's light blue and the tape's yellow (sp.js MC, TC). */
    private val LINE_C = SurveyMeas.LINE_COLOUR
    private val TAPE_C = SurveyMeas.TAPE_COLOUR

    /** A filled arrow head at [tip], pointing away from [from]: 7 dp long, 3 dp either side (sp.js aHead). */
    private fun head(c: Canvas, tip: FloatArray, from: FloatArray, colour: Int, dp: Float) {
        val dx = tip[0] - from[0]; val dy = tip[1] - from[1]; val l = hypot(dx, dy); if (!(l > 0f)) return
        val ux = dx / l; val uy = dy / l
        val hl = SurveyMeas.HEAD_LEN_DP.toFloat() * dp; val hw = SurveyMeas.HEAD_W_DP.toFloat() * dp
        val path = Path().apply {
            moveTo(tip[0], tip[1])
            lineTo(tip[0] - ux * hl - uy * hw, tip[1] - uy * hl + ux * hw)
            lineTo(tip[0] - ux * hl + uy * hw, tip[1] - uy * hl - ux * hw)
            close()
        }
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = argb(colour) })
    }

    /**
     * THE LINES AND TAPES ON A SURFACE (sp.js drawMeas, drawTapes): the Lines and areas first, then the tapes, then
     * whatever is being drawn -- a Line pulled (dashed), an area's corners so far (dashed, open), a tape pulled (its
     * band translucent) -- in red where it has left the surface. Everything is sized in dp ([Marks.dp]).
     */
    private fun drawMeas(c: Canvas, s: String, o: Marks, m: (Double, Double) -> FloatArray) {
        val sq = o.sq; val dp = o.dp
        fun toS(p: DoubleArray) = m(sq.x(p[0]), sq.y(p[1]))
        // canvas px per face mm: a squared face has one scale both ways
        val z0 = m(sq.x(0.0), sq.y(0.0)); val z1 = m(sq.x(100.0), sq.y(0.0))
        val ppm = hypot(z1[0] - z0[0], z1[1] - z0[1]) / 100f
        val sf = o.prep.surface(s)
        // the picked item's ends are handles on the editing photo with Select on (v52 slice 10)
        val hd = o.ends && o.mode == "main"
        for (q in sf.meas) if (q.kind == SurveyMeas.LINE || q.kind == SurveyMeas.AREA)
            line(c, q.kind, q.pts, q.code, o.selId == q.id, false, false, SurveyPrep.isLocked(q), dp, ::toS, hd)
        o.area?.takeIf { it.isNotEmpty() }?.let { line(c, "open", it, null, false, true, false, false, dp, ::toS) }
        o.drag?.takeIf { it.s == s && it.kind == SurveyMeas.LINE }?.let { d ->
            line(c, SurveyMeas.LINE, d.pts(), null, false, true, d.off, false, dp, ::toS) }
        for (q in sf.meas) if (q.kind == SurveyMeas.TAPE && q.pts.size == 2)
            tape(c, q.pts[0], q.pts[1], q.code, o.selId == q.id, false, false, SurveyPrep.isLocked(q), dp, ppm, o, ::toS, hd)
        o.drag?.takeIf { it.s == s && it.kind == SurveyMeas.TAPE }?.let { d ->
            tape(c, d.from, d.to, null, false, true, d.off, false, dp, ppm, o, ::toS) }
    }

    /**
     * A LINE (sp.js drawMeas): 2.4 dp, 3.5 when picked, with an arrow head at each end (none on a line under three
     * heads, 21 dp), labelled "F-M1 1234 mm"; an AREA outlined and filled at 0x26 with its corners as dots and "F-M2
     * 1.86 m²"; a draft dashed 7 / 5 dp ("1234 mm" while pulled, "1.86 m²…" from an area's third corner).
     */
    private fun line(c: Canvas, kind: String, pts: List<DoubleArray>, code: String?, sel: Boolean, draft: Boolean, off: Boolean,
                     locked: Boolean, dp: Float, toS: (DoubleArray) -> FloatArray, handles: Boolean = false) {
        if (pts.isEmpty()) return
        val closed = kind == SurveyMeas.AREA; val isLine = kind == SurveyMeas.LINE
        val col = if (draft && off) OUT_COLOUR else LINE_C
        val scr = pts.map(toS)
        val p = stroke(argb(col), (if (sel) 3.5f else 2.4f) * dp).apply { strokeCap = Paint.Cap.ROUND }
        if (draft) p.pathEffect = DashPathEffect(floatArrayOf(7f * dp, 5f * dp), 0f)
        if (closed && !draft && scr.size >= 3) {
            val fill = Path().apply { moveTo(scr[0][0], scr[0][1]); for (i in 1 until scr.size) lineTo(scr[i][0], scr[i][1]); close() }
            c.drawPath(fill, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = argb(LINE_C, 0x26) })
        }
        val n = if (closed) scr.size else scr.size - 1
        for (i in 0 until n) {
            val a = scr[i]; val b = scr[(i + 1) % scr.size]
            c.drawLine(a[0], a[1], b[0], b[1], p)
            if (isLine && hypot(b[0] - a[0], b[1] - a[1]) >= 3 * SurveyMeas.HEAD_LEN_DP.toFloat() * dp) {
                head(c, a, b, col, dp); head(c, b, a, col, dp)
            }
        }
        // an area's corners, and the corners tapped so far, as white dots ringed in the Line's blue; the picked Line's
        // ends and area's corners, with Select on, as HANDLES -- 5.5 dp, grabbed within 14 dp (v52 slice 10, sp.js drawMeas)
        val hd = sel && !draft && handles
        if (!isLine || hd) for (q in scr) {
            val rr = (if (hd) SurveyGuide.END_R_DP.toFloat() else 3.2f) * dp
            c.drawCircle(q[0], q[1], rr, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(0xffffff) })
            c.drawCircle(q[0], q[1], rr, stroke(argb(LINE_C), 1.5f * dp))
        }
        val segs = if (n > 0) (0 until n).flatMap { listOf(scr[it], scr[(it + 1) % scr.size]) } else scr
        val cx = segs.map { it[0] }.average().toFloat(); val cy = segs.map { it[1] }.average().toFloat()
        val t = SurveyMeas.lineTag(code, if (kind == "open") SurveyMeas.AREA else kind, pts)
        if (t.isNotEmpty()) label(c, t, cx + (if (locked) 30f else 6f) * dp, cy - 6f * dp, 0xe0f7ff, 12f * dp, dp)
        if (locked && !draft) padPill(c, cx + 26f * dp, cy - 21f * dp, LINE_C, dp)
    }

    /**
     * A TAPE (sp.js drawTapes): a yellow band 25 mm wide on the face -- at least 3.5 dp either side of its middle, so
     * it can be seen and tapped -- ringed white when picked, graduated from its start (a tick every 100 mm, a longer
     * one with its figure every 500 mm: SurveyMeas.tapeTicks), and its tag at the far end: "1234 mm" while it is
     * pulled, "F-T1 1234 mm" once plotted, kept on the canvas -- moved to the side of the band when the end is at an edge.
     */
    private fun tape(c: Canvas, a: DoubleArray, b: DoubleArray, code: String?, sel: Boolean, draft: Boolean, off: Boolean,
                     locked: Boolean, dp: Float, ppm: Float, o: Marks, toS: (DoubleArray) -> FloatArray, handles: Boolean = false) {
        val len = hypot(b[0] - a[0], b[1] - a[1])
        val pa = toS(a); val pb = toS(b)
        val col = if (off) OUT_COLOUR else TAPE_C
        if (len > 0) {
            val d = doubleArrayOf((b[0] - a[0]) / len, (b[1] - a[1]) / len); val nv = doubleArrayOf(-d[1], d[0])
            fun at(k: Double, w: Double) = toS(doubleArrayOf(a[0] + d[0] * k + nv[0] * w, a[1] + d[1] * k + nv[1] * w))
            val hw = SurveyMeas.bandHalfMm(ppm.toDouble(), dp.toDouble())
            val cs = listOf(at(0.0, hw), at(len, hw), at(len, -hw), at(0.0, -hw))
            val band = Path().apply { moveTo(cs[0][0], cs[0][1]); for (i in 1 until 4) lineTo(cs[i][0], cs[i][1]); close() }
            if (sel) c.drawPath(band, stroke(argb(0xffffff), 5f * dp).apply { strokeJoin = Paint.Join.ROUND })
            c.drawPath(band, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = argb(col, if (draft) 0xc8 else 0xf0) })
            c.drawPath(band, stroke(argb(0x3b2f00), 1f * dp))
            for (tk in SurveyMeas.tapeTicks(len)) {
                // too close together to read: only every 500 mm
                if (!tk.major && ppm * 100 < 4f * dp) continue
                val p = at(tk.at, hw); val q = at(tk.at, hw - 2 * hw * (if (tk.major) .62 else .36))
                c.drawLine(p[0], p[1], q[0], q[1], stroke(argb(0x1d1a0a), (if (tk.major) 1.4f else .9f) * dp))
                if (tk.figure && ppm > 0f) {
                    val t = at(tk.at, -hw - 9f * dp / ppm)
                    val txt = tk.at.roundToInt().toString()
                    val tw = Paint().apply { textSize = 9f * dp; isFakeBoldText = true }.measureText(txt)
                    label(c, txt, t[0] - tw / 2, t[1] + 3f * dp, TAPE_C, 9f * dp, dp)
                }
            }
        }
        // the picked tape's two ends, with Select on: white handles, 5.5 dp, ringed dark (v52 slice 10, sp.js drawTapes)
        if (sel && !draft && handles) for (p in listOf(pa, pb)) {
            c.drawCircle(p[0], p[1], SurveyGuide.END_R_DP.toFloat() * dp, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(0xffffff) })
            c.drawCircle(p[0], p[1], SurveyGuide.END_R_DP.toFloat() * dp, stroke(argb(0x3b2f00), 2f * dp))
        }
        val txt = SurveyMeas.tapeTag(if (draft) null else code, len)
        val tw = Paint().apply { textSize = 12f * dp; isFakeBoldText = true }.measureText(txt)
        val pw = if (locked && !draft) 26f * dp else 0f
        // past the far end, kept on the canvas -- or beside it, above the band, when there is no room (SurveyMeas.tapeTagAt)
        val k = SurveyMeas.tapeTagAt(doubleArrayOf(pa[0].toDouble(), pa[1].toDouble()), doubleArrayOf(pb[0].toDouble(), pb[1].toDouble()),
            tw.toDouble(), pw.toDouble(), dp.toDouble(), o.width.toDouble(), o.height.toDouble()).let { floatArrayOf(it[0].toFloat(), it[1].toFloat()) }
        label(c, txt, k[0], k[1], if (off) 0xffd0d0 else 0xfff7c2, 12f * dp, dp)
        if (pw > 0f) padPill(c, k[0] + tw + pw, k[1] - 13f * dp, TAPE_C, dp)
    }

    // ------------------------------------------------------------ v52 slice 11: the chains

    /**
     * THE CHAINS ON A SURFACE (sp.js drawChains), then the one being drawn: see [chain]. Every figure comes from
     * SurveyChain.vals -- the readings, the laser's total, the derived segment -- so the photo says what the list says.
     */
    private fun drawChains(c: Canvas, s: String, o: Marks, m: (Double, Double) -> FloatArray) {
        val sq = o.sq
        fun toS(p: DoubleArray) = m(sq.x(p[0]), sq.y(p[1]))
        for (k in o.prep.surface(s).chains) if (k.nodes.isNotEmpty())
            chain(c, k.nodes, k.axis, k.code, SurveyChain.vals(k, o.prep.actual, o.a, o.b), o.selId == k.id, false,
                SurveyPrep.isLocked(k), o, ::toS)
        o.chain?.takeIf { it.s == s && it.nodes.isNotEmpty() }?.let { d ->
            chain(c, d.nodes, d.axis ?: "x", null, null, false, true, false, o, ::toS) }
    }

    /**
     * ONE CHAIN (sp.js drawChains; Amit, 13 Oct 2026 19:20 IST, the "divider"): its line in rose, 2.6 dp (3.6 picked),
     * an arrow head at both ends of every segment, a draft dashed 7 / 5 dp; a tick across it at every point (10 dp
     * either side) and the point a white dot ringed in rose -- the handle a finger finds within 16 dp; each segment's
     * figure 15 dp off the chain -- its reading white, a derived one violet with " d", else the photo's "≈905" grey; the
     * total under the last point, "FK1 Σ 3310 laser" (amber when the readings disagree with it), kept on the canvas; a
     * locked chain's padlock over its first point. Sized in dp ([Marks.dp]).
     */
    private fun chain(c: Canvas, nodes: List<DoubleArray>, axis: String, code: String?, v: SurveyChain.Vals?, sel: Boolean,
                      draft: Boolean, locked: Boolean, o: Marks, toS: (DoubleArray) -> FloatArray) {
        val dp = o.dp; val col = SurveyChain.COLOUR
        val ai = SurveyChain.axisIndex(axis)
        // across the chain, in the face's mm: up for a chain along it, right for one up it
        val perp = if (ai == 1) doubleArrayOf(1.0, 0.0) else doubleArrayOf(0.0, 1.0)
        val scr = nodes.map(toS)
        val ln = stroke(argb(col), ((if (sel) SurveyChain.LINE_SEL_DP else SurveyChain.LINE_DP) * dp).toFloat())
        if (draft) ln.pathEffect = DashPathEffect(floatArrayOf((SurveyChain.DASH_DP * dp).toFloat(), (SurveyChain.GAP_DP * dp).toFloat()), 0f)
        for (i in 1 until scr.size) {
            val a = scr[i - 1]; val b = scr[i]
            c.drawLine(a[0], a[1], b[0], b[1], ln)
            if (hypot(b[0] - a[0], b[1] - a[1]) >= 3 * SurveyMeas.HEAD_LEN_DP.toFloat() * dp) { head(c, a, b, col, dp); head(c, b, a, col, dp) }
        }
        if (locked && !draft) padPill(c, scr[0][0] + 8f * dp, scr[0][1] - 30f * dp, col, dp)
        val tick = stroke(argb(col), (SurveyChain.TICK_W_DP * dp).toFloat())
        val r = ((if (sel) SurveyChain.NODE_R_SEL_DP else SurveyChain.NODE_R_DP) * dp).toFloat()
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(0xffffff) }
        val ring = stroke(argb(col), (SurveyChain.NODE_RING_DP * dp).toFloat())
        for (j in nodes.indices) {
            val p = scr[j]; val n = nodes[j]
            val q = toS(doubleArrayOf(n[0] + perp[0] * 100, n[1] + perp[1] * 100))
            val dx = q[0] - p[0]; val dy = q[1] - p[1]; val l = hypot(dx, dy).takeIf { it > 0f } ?: 1f
            val kk = (SurveyChain.TICK_DP * dp).toFloat() / l
            c.drawLine(p[0] - dx * kk, p[1] - dy * kk, p[0] + dx * kk, p[1] + dy * kk, tick)
            c.drawCircle(p[0], p[1], r, dot); c.drawCircle(p[0], p[1], r, ring)
        }
        val fs = 12f * dp
        val fp = Paint().apply { textSize = fs; isFakeBoldText = true }
        val off = (SurveyChain.FIGURE_OFF_DP * dp).toFloat()
        for (i in 1 until nodes.size) {
            val mid = doubleArrayOf((nodes[i - 1][0] + nodes[i][0]) / 2, (nodes[i - 1][1] + nodes[i][1]) / 2)
            val p = toS(mid); val q = toS(doubleArrayOf(mid[0] + perp[0] * 100, mid[1] + perp[1] * 100))
            var dx = q[0] - p[0]; var dy = q[1] - p[1]; val l = hypot(dx, dy)
            if (l > 0f) { dx /= l; dy /= l } else { dx = 0f; dy = -1f }
            val (t, tc) = if (v != null) SurveyChain.segFigure(v, i - 1)
                          else ("≈" + abs(nodes[i][ai] - nodes[i - 1][ai]).roundToInt()) to SurveyChain.PHOTO_COLOUR
            val w = fp.measureText(t)
            label(c, t, p[0] + dx * off - w / 2, p[1] + dy * off + 4f * dp, tc, fs, dp)
        }
        if (v != null && code != null) {
            val p = scr[scr.size - 1]
            val t = SurveyChain.totalTag(code, v)
            val ts = 11f * dp
            val tw = Paint().apply { textSize = ts; isFakeBoldText = true }.measureText(t)
            // kept inside the picture (sp.js: "kept inside the picture")
            val x = max(4f * dp, minOf(o.width - tw - 6f * dp, p[0] + 8f * dp))
            label(c, t, x, p[1] + 20f * dp, SurveyChain.totalColour(v), ts, dp)
        }
    }

    /** Dimension lines for one mark, each with its estimate and / or reading. */
    private fun dims(c: Canvas, s: String, e: SurveyPrep.Mark, o: Marks, m: (Double, Double) -> FloatArray,
                     p: FloatArray, q: FloatArray, fs: Float, u: Float) {
        val ms = SurveyPrep.measures(s, e, o.a, o.b).associateBy { it.key }
        fun act(k: String) = o.prep.actual[e.code + "." + k]
        val ln = stroke(argb(0xffffff, 0xcc), if (fs > 15f * u) 2.5f * u else 1f * u)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = c.drawLine(x1, y1, x2, y2, ln)
        fun tag(k: String, x: Float, y: Float, left: Boolean = false) = vtag(c, x, y, ms.getValue(k).est, act(k), fs, e.code + "." + k, o, u, left)
        val midY = (p[1] + q[1]) / 2; val midX = (p[0] + q[0]) / 2; val cE = SurveyPrep.isPoint(e.t)
        // v52: a point's figures run to its centre under its own keys (cl, cr, cs, ct, cf, ck; a plan's Electric
        // point keeps l, r, f, k) -- each line is drawn under whichever key the mark HAS, with that key's tag
        fun kOf(k: String): String? = if (k in ms) k else if ("c$k" in ms) "c$k" else null
        val xL = m(o.sq.x(0.0), 0.0)[0]; val xR = m(o.sq.x(o.a), 0.0)[0]
        val yT = m(0.0, o.sq.y(o.b))[1]; val yB = m(0.0, o.sq.y(0.0))[1]
        // v52 item 14: on the picked mark (the one showing its eight tabs) the width tag stands higher and the
        // height tag starts right of the edge, each clear of the tabs (SurveyPrepTouch.widthTag / heightTag)
        val hand = o.mode == "main" && o.selId == e.id && !cE && !SurveyPrep.isLocked(e)
        val du = u.toDouble(); val dfs = fs.toDouble(); val ddp = o.dp.toDouble()
        if ("w" in ms) {
            line(p[0], p[1] - 6f * u, q[0], p[1] - 6f * u)
            val a = SurveyPrepTouch.widthTag(p[0].toDouble(), p[1].toDouble(), q[0].toDouble(), dfs, du, ddp, hand)
            tag("w", a.x.toFloat(), a.y.toFloat(), a.left)
        }
        if ("h" in ms) {
            line(q[0] + 6f * u, p[1], q[0] + 6f * u, q[1])
            val a = SurveyPrepTouch.heightTag(p[1].toDouble(), q[0].toDouble(), q[1].toDouble(), dfs, du, ddp, hand)
            tag("h", a.x.toFloat(), a.y.toFloat(), a.left)
        }
        val hy = if (cE) midY else midY + fs * 2.6f
        kOf("l")?.let { k -> val x = if (cE) midX else p[0]; line(xL, hy, x, hy); tag(k, (xL + x) / 2, hy - 4f * u) }
        kOf("r")?.let { k -> val x = if (cE) midX else q[0]; line(x, hy, xR, hy); tag(k, (x + xR) / 2, hy - 4f * u) }
        val vx = if (cE) midX else midX - fs * 1.6f
        for (k0 in listOf("s", "k")) kOf(k0)?.let { k -> val y = if (cE) midY else q[1]; line(vx, y, vx, yB); tag(k, vx, (y + yB) / 2 + 4f * u) }
        for (k0 in listOf("t", "f")) kOf(k0)?.let { k -> val y = if (cE) midY else p[1]; line(vx, yT, vx, y); tag(k, vx, (yT + y) / 2 + 4f * u) }
        if ("d" in ms) { line(midX, yT, midX, q[1]); tag("d", midX, q[1] + fs * 1.4f) }
    }

    fun sgn(d: Double): String { val r = d.roundToInt(); return (if (r > 0) "+" else if (r < 0) "−" else "±") + abs(r) }

    /** A value tag: photo estimate and / or site reading, per the Show toggle; gold when it is the ACTIVE one.
     *  Centred on [x0], or starting there when [left]; [y0] is its baseline. */
    private fun vtag(c: Canvas, x0: Float, y0: Float, est: Double?, act: Double?, fs: Float, code: String, o: Marks, u: Float, left: Boolean = false) {
        val on = code == o.active
        val parts = mutableListOf<Pair<String, Int>>()
        if (o.show != "actual") parts.add((if (est != null) "≈" + est.roundToInt() else "—") to 0xcfd3cb)
        if (o.show != "derived") {
            if (o.show == "both") parts.add(" | " to 0x888888)
            parts.add((if (act != null) act.roundToInt().toString() else "?") to 0xffffff)
            if (o.show == "both" && act != null && est != null) {
                val d = act - est; parts.add((" " + sgn(d)) to (if (abs(d) <= SurveyPrep.TOL) 0x7dff8a else 0xffc46b))
            }
        }
        if (on && parts.isNotEmpty()) parts[0] = ("▶" + parts[0].first) to parts[0].second
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = fs; typeface = MONO_BOLD }
        val w = parts.sumOf { p.measureText(it.first).toDouble() }.toFloat() + 10f * u
        val sh = SurveyPrepTouch.tagShape(SurveyPrepTouch.Anchor(x0.toDouble(), y0.toDouble(), left), w.toDouble(), fs.toDouble(),
            u.toDouble(), o.width.toDouble(), o.height.toDouble())
        val x = sh.x.toFloat(); val y = sh.baseline.toFloat()
        val box = RectF(sh.drawn.l.toFloat(), sh.drawn.t.toFloat(), sh.drawn.r.toFloat(), sh.drawn.b.toFloat())
        c.drawRoundRect(box, 5f * u, 5f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (on) argb(0x4a3d0c) else argb(0x000000, 0xdd) })
        if (on) c.drawRoundRect(box, 5f * u, 5f * u, stroke(argb(0xe0b040), 2f * u))
        o.tags?.add(SurveyPrepTouch.Tag(code, sh.tap))
        var cx = x + 5f * u
        for ((t, col) in parts) { p.color = argb(col); c.drawText(t, cx, y, p); cx += p.measureText(t) }
    }
}
