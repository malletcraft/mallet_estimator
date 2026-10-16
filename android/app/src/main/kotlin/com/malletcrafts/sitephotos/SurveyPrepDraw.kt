package com.malletcrafts.sitephotos

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.malletcrafts.sitephotos.pano.SurveyPrep
import com.malletcrafts.sitephotos.pano.SurveyPrepTouch
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
        /** Screen px per dp: the picked mark's tags stand at least 6 dp off its tabs. */
        val dp: Float = 1f,
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
