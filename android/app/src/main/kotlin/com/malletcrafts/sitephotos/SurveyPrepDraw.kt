package com.malletcrafts.sitephotos

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.malletcrafts.sitephotos.pano.SurveyPrep
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
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

    /** A number tag on the photo, as a tap target: canvas px. */
    class Tag(val code: String, val x: Float, val y: Float, val w: Float, val h: Float) {
        fun contains(px: Float, py: Float) = px >= x && px <= x + w && py >= y && py <= y + h
        fun dist(px: Float, py: Float) = hypot(px - (x + w / 2), py - (y + h / 2))
    }

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

    // ------------------------------------------------------------ 1 · Lines

    /**
     * The four boundary lines of a surface, drawn long so they read as lines
     * and not segments; a solid line has been set by hand, a dashed one not
     * yet. No numbers -- only the boundary. [thin] is the magnifier's version.
     */
    fun drawLines(c: Canvas, s: String, sf: SurveyPrep.Surface, frame: SurveyPrep.Frame?,
                  m: (Double, Double) -> FloatArray, u: Float, thin: Boolean) {
        val ext = 2.0 * SurveyPrep.VW
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
        /** Filled with the number tags drawn, for taps. */
        val tags: MutableList<Tag>?,
    )

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
            val p = m(sq.x(sm.x0), sq.y(sm.y1)); val q = m(sq.x(sm.x1), sq.y(sm.y0)); val pad = 6f * u
            val path = Path().apply {
                fillType = Path.FillType.EVEN_ODD
                addRect(0f, 0f, o.width, o.height, Path.Direction.CW)
                addRect(p[0] - pad, p[1] - pad, q[0] + pad, q[1] + pad, Path.Direction.CW)
            }
            c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(0x000000, 0x33) })
        }
        for (e in els.sortedBy { if (sm != null && it === sm) 1 else 0 }) {
            val col = SurveyPrep.typeOf(s, e.t).colour
            val p = m(sq.x(e.x0), sq.y(e.y1)); val q = m(sq.x(e.x1), sq.y(e.y0)); val sel = o.selId == e.id
            val faded = sm != null && !sel
            if (faded) c.saveLayerAlpha(null, 191)
            c.drawRect(p[0], p[1], q[0], q[1], Paint().apply { color = argb(col, 0x22) })
            c.drawRect(p[0], p[1], q[0], q[1], stroke(argb(col), (if (thin) 1.5f else if (sel) 3.5f else 2.5f) * u))
            if (!thin) {
                label(c, e.code, p[0] + 6f * u, p[1] + (if (o.all) 30f else 16f) * u, col, (if (o.all) 26f else 13f) * u, u)
                if (sel && o.mode == "main") {
                    val xm = (p[0] + q[0]) / 2; val ym = (p[1] + q[1]) / 2; val h = 5f * u
                    for ((tx, ty) in listOf(p[0] to p[1], q[0] to p[1], q[0] to q[1], p[0] to q[1], xm to p[1], xm to q[1], p[0] to ym, q[0] to ym)) {
                        c.drawRect(tx - h, ty - h, tx + h, ty + h, Paint().apply { color = argb(0xffffff) })
                        c.drawRect(tx - h, ty - h, tx + h, ty + h, stroke(argb(col), 2f * u))
                    }
                }
                if (sel || o.all) dims(c, s, e, o, m, p, q, (if (o.all) 22f else 12f) * u, u)
            }
            if (faded) c.restore()
        }
    }

    /** Dimension lines for one mark, each with its estimate and / or reading. */
    private fun dims(c: Canvas, s: String, e: SurveyPrep.Mark, o: Marks, m: (Double, Double) -> FloatArray,
                     p: FloatArray, q: FloatArray, fs: Float, u: Float) {
        val ms = SurveyPrep.measures(s, e, o.a, o.b).associateBy { it.key }
        fun act(k: String) = o.prep.actual[e.code + "." + k]
        val ln = stroke(argb(0xffffff, 0xcc), if (fs > 15f * u) 2.5f * u else 1f * u)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = c.drawLine(x1, y1, x2, y2, ln)
        fun tag(k: String, x: Float, y: Float) = vtag(c, x, y, ms.getValue(k).est, act(k), fs, e.code + "." + k, o, u)
        val midY = (p[1] + q[1]) / 2; val midX = (p[0] + q[0]) / 2; val cE = SurveyPrep.isPlan(s) && e.t == "E"
        val xL = m(o.sq.x(0.0), 0.0)[0]; val xR = m(o.sq.x(o.a), 0.0)[0]
        val yT = m(0.0, o.sq.y(o.b))[1]; val yB = m(0.0, o.sq.y(0.0))[1]
        if ("w" in ms) { line(p[0], p[1] - 6f * u, q[0], p[1] - 6f * u); tag("w", midX, p[1] - fs * 0.8f) }
        if ("h" in ms) { line(q[0] + 6f * u, p[1], q[0] + 6f * u, q[1]); tag("h", q[0] + 6f * u + fs * 3.4f, midY + 4f * u) }
        val hy = if (cE) midY else midY + fs * 2.6f
        if ("l" in ms) { val x = if (cE) midX else p[0]; line(xL, hy, x, hy); tag("l", (xL + x) / 2, hy - 4f * u) }
        if ("r" in ms) { val x = if (cE) midX else q[0]; line(x, hy, xR, hy); tag("r", (x + xR) / 2, hy - 4f * u) }
        val vx = if (cE) midX else midX - fs * 1.6f
        for (k in listOf("s", "k")) if (k in ms) { val y = if (cE) midY else q[1]; line(vx, y, vx, yB); tag(k, vx, (y + yB) / 2 + 4f * u) }
        for (k in listOf("t", "f")) if (k in ms) { val y = if (cE) midY else p[1]; line(vx, yT, vx, y); tag(k, vx, (yT + y) / 2 + 4f * u) }
        if ("d" in ms) { line(midX, yT, midX, q[1]); tag("d", midX, q[1] + fs * 1.4f) }
    }

    fun sgn(d: Double): String { val r = d.roundToInt(); return (if (r > 0) "+" else if (r < 0) "−" else "±") + abs(r) }

    /** A value tag: photo estimate and / or site reading, per the Show toggle; gold when it is the ACTIVE one. */
    private fun vtag(c: Canvas, x0: Float, y0: Float, est: Double?, act: Double?, fs: Float, code: String, o: Marks, u: Float) {
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
        val x = max(2f * u, min(o.width - w - 2f * u, x0 - w / 2)); val y = max(fs + 4f * u, min(o.height - 4f * u, y0))
        val box = RectF(x, y - fs - 2f * u, x + w, y + 5f * u)
        c.drawRoundRect(box, 5f * u, 5f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (on) argb(0x4a3d0c) else argb(0x000000, 0xdd) })
        if (on) c.drawRoundRect(box, 5f * u, 5f * u, stroke(argb(0xe0b040), 2f * u))
        o.tags?.add(Tag(code, x - 6f * u, y - fs - 8f * u, w + 12f * u, fs + 19f * u))
        var cx = x + 5f * u
        for ((t, col) in parts) { p.color = argb(col); c.drawText(t, cx, y, p); cx += p.measureText(t) }
    }
}
