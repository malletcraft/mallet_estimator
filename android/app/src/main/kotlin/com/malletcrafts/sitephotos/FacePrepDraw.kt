package com.malletcrafts.sitephotos

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.malletcrafts.sitephotos.pano.FacePrep
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Drawing Face Prep marks, with ONE renderer for the screen and for the
 * ImageMeter photo — the prototype's rule, so what Amit lines up is exactly
 * what gets exported. [map] takes a point in face-photo pixels (the square
 * part, side [side]) and returns where to draw it on the target canvas.
 */
object FacePrepDraw {

    const val EDGE = 0xFFFFD23F.toInt()
    const val LINE = 0xFFFF7AD9.toInt()
    const val GUIDE = 0xFF35E0FF.toInt()
    const val GUIDE_ON = 0xFF7DFF8A.toInt()
    /** The SITE figure's green, the prototype's #7dff8a: a figure measured on site, beside the calculated one. */
    const val SITE = 0xFF7DFF8A.toInt()
    val STEP_COL = mapOf("column" to 0xFFFF8A65.toInt(), "beam" to 0xFFE0703E.toInt(), "step" to 0xFFC9C2B8.toInt())
    val DET_COL = mapOf("door" to 0xFF6FCF97.toInt(), "window" to 0xFF56CCF2.toInt(), "opening" to 0xFFBB86FC.toInt(),
        "electrical" to 0xFFF2C94C.toInt(), "loft" to 0xFFFFA45C.toInt())

    /** What is selected, so it can be drawn white. */
    data class Sel(val kind: String, val i: Int = -1, val j: Int = -1, val key: String = "")

    /**
     * A dimension as it was drawn on screen: where its label sits ([at], with
     * [w]×[h] px) and its arrow's ends — so a tap in Measure can find it.
     */
    class DrawnDim(val key: String, val at: FloatArray, val w: Float, val h: Float, val a: FloatArray?, val b: FloatArray?)

    class Opts(
        val handles: String? = null,          // "box", "steps", "dets", "lines" — which grips to draw
        val sel: Sel? = null,
        val grid: FacePrep.Grid? = null,      // screen only
        val guide: FacePrep.LineGuide? = null, val guideAt: DoubleArray? = null,
        val boxGuide: Pair<String, Double>? = null,   // side, t — a box divider being placed
        val dims: MutableList<DrawnDim>? = null,      // filled with every dimension drawn, in draw order
    )

    fun uvToImg(box: FacePrep.Box, side: Float, u: Double, v: Double) =
        floatArrayOf(((box.x0 + u * (box.x1 - box.x0)) * side).toFloat(), ((box.y0 + v * (box.y1 - box.y0)) * side).toFloat())

    fun draw(c: Canvas, room: FacePrep.Room, face: String, box: FacePrep.Box, side: Float,
             map: (Float, Float) -> FloatArray, sc: Float, o: Opts) {
        val f = room.face(face)
        val (w, h) = FacePrep.faceDims(room, face)
        fun P(u: Double, v: Double): FloatArray { val q = uvToImg(box, side, u, v); return map(q[0], q[1]) }
        val lw = max(2.4f, 2.6f * sc); val fs = max(20f, 22f * sc); val hs = 14f * sc; val off = fs * 1.4f
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        fun line(a: FloatArray, b: FloatArray, col: Int, width: Float, dash: Float = 0f) {
            stroke.color = col; stroke.strokeWidth = width
            stroke.pathEffect = if (dash > 0) DashPathEffect(floatArrayOf(dash, dash * 0.7f), 0f) else null
            c.drawLine(a[0], a[1], b[0], b[1], stroke); stroke.pathEffect = null
        }
        fun arrow(a: FloatArray, b: FloatArray, col: Int, width: Float, dash: Float = 0f, head: Float = hs) {
            line(a, b, col, width, dash)
            val ang = atan2(b[1] - a[1], b[0] - a[0])
            for ((p, dir) in listOf(b to ang, a to (ang + Math.PI.toFloat()))) {
                val path = Path(); path.moveTo(p[0], p[1])
                path.lineTo(p[0] - head * cos(dir - 0.45f), p[1] - head * sin(dir - 0.45f))
                path.lineTo(p[0] - head * cos(dir + 0.45f), p[1] - head * sin(dir + 0.45f)); path.close()
                fill.color = col; c.drawPath(path, fill)
            }
        }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = android.graphics.Typeface.MONOSPACE; isFakeBoldText = true }
        fun tag(t: String, at: FloatArray, col: Int, size: Float = fs) {
            text.textSize = size; text.color = col
            val tw = text.measureText(t); val th = size * 1.3f
            fill.color = 0xD90F0E0C.toInt()
            c.drawRoundRect(RectF(at[0] - tw / 2 - size * 0.35f, at[1] - th / 2, at[0] + tw / 2 + size * 0.35f, at[1] + th / 2), 4f, 4f, fill)
            c.drawText(t, at[0] - tw / 2, at[1] + size * 0.36f, text)
        }
        fun add(p: FloatArray, v: FloatArray, k: Float) = floatArrayOf(p[0] + v[0] * k, p[1] + v[1] * k)
        fun mid(a: FloatArray, b: FloatArray, t: Float = 0.5f) = floatArrayOf(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)
        fun unit(v: FloatArray): FloatArray { val l = hypot(v[0], v[1]).takeIf { it > 0 } ?: 1f; return floatArrayOf(v[0] / l, v[1] / l) }

        // Every dimension goes through dimTag: the CALCULATED figure in its own colour and, when one was
        // taken on site (laser or typed), the SITE figure in green right beside it. The site figure never
        // moves the drawing. The selected dimension's own arrow is drawn end to end, bold and white, with
        // end bars, so it is plain which two points the site figure is between (prototype v23).
        val strokeRect = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        fun dimTag(key: String, txt: String, at: FloatArray, col: Int, size: Float, a: FloatArray?, b: FloatArray?) {
            val isSel = o.sel?.kind == "dim" && o.sel?.key == key
            if (isSel && a != null && b != null) {
                val dv = unit(floatArrayOf(b[0] - a[0], b[1] - a[1])); val nb = floatArrayOf(-dv[1], dv[0]); val bar = 16f * sc
                arrow(a, b, 0xFFFFFFFF.toInt(), max(4f, lw * 1.8f), 0f, hs * 1.4f)
                for (e in listOf(a, b)) line(add(e, nb, -bar), add(e, nb, bar), 0xFFFFFFFF.toInt(), max(3f, lw * 1.3f))
            }
            tag(txt, at, col, size)
            text.textSize = size
            val tw = text.measureText(txt) + size * 0.7f; val th = size * 1.3f
            if (isSel) { strokeRect.color = 0xFFFFFFFF.toInt(); strokeRect.strokeWidth = 3f * sc
                c.drawRect(at[0] - tw / 2 - 3f, at[1] - th / 2 - 3f, at[0] + tw / 2 + 3f, at[1] + th / 2 + 3f, strokeRect) }
            val site = f.meas[key]
            if (site != null) {
                val t2 = "$site"; text.textSize = size; val w2 = text.measureText(t2) + size * 0.7f
                val qx = at[0] + tw / 2 + 4f * sc + w2 / 2; val qy = at[1]
                fill.color = 0xEB063012.toInt(); c.drawRect(qx - w2 / 2, qy - th / 2, qx + w2 / 2, qy + th / 2, fill)
                strokeRect.color = SITE; strokeRect.strokeWidth = 2f * sc
                c.drawRect(qx - w2 / 2, qy - th / 2, qx + w2 / 2, qy + th / 2, strokeRect)
                text.color = SITE; c.drawText(t2, qx - text.measureText(t2) / 2, qy + size * 0.36f, text)
            }
            o.dims?.add(DrawnDim(key, at, tw, th, a, b))
        }

        // reference grid, faint, under everything (screen only)
        o.grid?.let { g ->
            val (gu, gv) = FacePrep.gridLines(g, w, h); val a = g.alpha / 100f
            fun isMajor(m: Int) = g.major > 0 && m % g.major == 0
            for ((u, m) in gu) { val mj = isMajor(m); line(P(u, 0.0), P(u, 1.0), argb(if (mj) min(1f, a * 1.8f) else a), if (mj) 2f * sc else 1.2f * sc)
                if (g.labels && (mj || g.x >= 250)) tag("$m", P(u, if (g.fromBottom) 0.975 else 0.025), argb(min(1f, a * 2 + 0.2f)), fs * 0.75f) }
            for ((v, m) in gv) { val mj = isMajor(m); line(P(0.0, v), P(1.0, v), argb(if (mj) min(1f, a * 1.8f) else a), if (mj) 2f * sc else 1.2f * sc)
                if (g.labels && (mj || g.y >= 250)) tag("$m", P(if (g.fromLeft) 0.03 else 0.97, v), argb(min(1f, a * 2 + 0.2f)), fs * 0.75f) }
        }

        // the measured box, thin and dashed, so the steps read against it
        val corners = listOf(P(0.0, 0.0), P(1.0, 0.0), P(1.0, 1.0), P(0.0, 1.0))
        for (k in 0..3) line(corners[k], corners[(k + 1) % 4], 0xB3FFD23F.toInt(), max(1.5f, lw * 0.5f), 6f * sc)

        // columns, beams, other steps: shaded, named
        val sCount = HashMap<String, Int>()
        f.steps.forEachIndexed { i, s0 ->
            val s = s0.norm(); sCount[s.kind] = (sCount[s.kind] ?: 0) + 1
            val pts = listOf(P(s.u0, s.v0), P(s.u1, s.v0), P(s.u1, s.v1), P(s.u0, s.v1))
            val path = Path().apply { moveTo(pts[0][0], pts[0][1]); for (k in 1..3) lineTo(pts[k][0], pts[k][1]); close() }
            fill.color = 0x610A0A0A; c.drawPath(path, fill)
            val on = o.sel?.kind == "steps" && o.sel?.i == i
            val col = STEP_COL[s.kind] ?: EDGE
            for (k in 0..3) line(pts[k], pts[(k + 1) % 4], if (on) -1 else col, (if (on) 3f else 2f) * sc, 8f * sc)
            tag("${FacePrep.stepLabel(s.kind).split(" ")[0]} ${sCount[s.kind]}", P((s.u0 + s.u1) / 2, (s.v0 + s.v1) / 2), col)
            if (o.handles == "steps") for (p in pts) grip(c, fill, stroke, p, if (on) -1 else col, (if (on) 10f else 8f) * sc)
        }

        // the outline, solid, and a dimension on every piece, outside the face
        val pieces = FacePrep.outline(room, face, f)
        val edgeKeys = FacePrep.edgeKeys(pieces)
        val reach = HashMap<String, Float>(); var n = 0
        for (pc in pieces) {
            val a = P(pc.a[0].toDouble() / w, pc.a[1].toDouble() / h); val b = P(pc.b[0].toDouble() / w, pc.b[1].toDouble() / h)
            line(a, b, EDGE, lw)
            n++
            if (o.handles == "box") continue
            val mx = (pc.a[0] + pc.b[0]) / 2.0; val my = (pc.a[1] + pc.b[1]) / 2.0
            val inPt = P((mx + pc.inward[0] * w * 0.02) / w, (my + pc.inward[1] * h * 0.02) / h)
            val m = mid(a, b); val iv = unit(floatArrayOf(inPt[0] - m[0], inPt[1] - m[1]))
            val ov = if (pc.side != null) floatArrayOf(-iv[0], -iv[1]) else iv
            val p = add(a, ov, off); val q = add(b, ov, off)
            arrow(p, q, EDGE, max(1.6f, lw * 0.7f), 4f * sc)
            line(a, p, EDGE, max(1f, lw * 0.45f), 4f * sc); line(b, q, EDGE, max(1f, lw * 0.45f), 4f * sc)
            val labOff = fs * 0.9f
            dimTag(edgeKeys[n - 1], "E$n ${pc.len}", add(mid(p, q), ov, labOff), EDGE, fs, p, q)
            pc.side?.let { reach[it] = max(reach[it] ?: 0f, off + labOff * 2) }
        }
        // the measured totals, beyond the edge dimensions
        for (sd in FacePrep.SIDES) {
            val tot = if (sd == "top" || sd == "bottom") w else h
            val (a, b) = when (sd) { "top" -> P(0.0, 0.0) to P(1.0, 0.0); "bottom" -> P(0.0, 1.0) to P(1.0, 1.0)
                                     "left" -> P(0.0, 0.0) to P(0.0, 1.0); else -> P(1.0, 0.0) to P(1.0, 1.0) }
            val m = mid(a, b); val ctr = P(0.5, 0.5); val ov = unit(floatArrayOf(m[0] - ctr[0], m[1] - ctr[1]))
            dimTag("T:$sd", "$tot mm", add(m, ov, (reach[sd] ?: off) + fs), -1, fs, a, b)
        }
        // box dividers
        for ((sd, ts) in f.splits) ts.forEachIndexed { i, t ->
            val p = when (sd) { "top" -> P(t, 0.0); "bottom" -> P(t, 1.0); "left" -> P(0.0, t); else -> P(1.0, t) }
            val on = o.sel?.kind == "split" && o.sel?.key == sd && o.sel?.i == i
            fill.color = EDGE; c.drawCircle(p[0], p[1], (if (on) 11f else 7f) * sc, fill)
            if (on) { stroke.color = -1; stroke.strokeWidth = 3f * sc; c.drawCircle(p[0], p[1], 11f * sc, stroke) }
        }
        // the box's corner grips while lining it up
        if (o.handles == "box") {
            o.sel?.takeIf { it.kind == "bline" }?.let { s ->
                val (a, b) = when (s.key) { "x0" -> corners[0] to corners[3]; "x1" -> corners[1] to corners[2]; "y0" -> corners[0] to corners[1]; else -> corners[3] to corners[2] }
                line(a, b, -1, lw * 1.6f)
            }
            for (k in 0..3) { val p = corners[k]; val on = o.sel?.kind == "corner" && o.sel?.i == k
                fill.color = 0x59000000; c.drawCircle(p[0], p[1], 22f * sc, fill)
                stroke.color = if (on) -1 else GUIDE; stroke.strokeWidth = 3.5f * sc; c.drawCircle(p[0], p[1], 22f * sc, stroke)
                fill.color = GUIDE; c.drawCircle(p[0], p[1], 4f * sc, fill) }
        }
        // a wall's columns and beams carried onto this floor / ceiling
        for (g in FacePrep.plateGuides(room, face)) {
            val col = STEP_COL[g.kind] ?: EDGE
            line(P(g.p0[0], g.p0[1]), P(g.q0[0], g.q0[1]), col, 2.4f * sc, 10f * sc)
            line(P(g.p1[0], g.p1[1]), P(g.q1[0], g.q1[1]), col, 2.4f * sc, 10f * sc)
            line(P(g.q0[0], g.q0[1]), P(g.q1[0], g.q1[1]), col, 1.6f * sc, 10f * sc)
            tag(g.label, mid(P(g.q0[0], g.q0[1]), P(g.q1[0], g.q1[1])), col, fs * 0.85f)
        }
        // the floor / ceiling plan's steps, where this wall shows their face
        for (g in FacePrep.wallGuides(room, face)) {
            val col = STEP_COL[g.kind] ?: EDGE
            val vEnd = if (g.plate == "floor") 0.0 else 0.3; val vStart = if (g.plate == "floor") 1.0 else 0.0
            line(P(g.u0, vStart), P(g.u0, vEnd), col, 2.4f * sc, 10f * sc); line(P(g.u1, vStart), P(g.u1, vEnd), col, 2.4f * sc, 10f * sc)
            tag(g.label, P((g.u0 + g.u1) / 2, if (g.plate == "floor") 0.94 else 0.06), col, fs * 0.85f)
        }
        // details: a rectangle each, two size arrows, and where it sits
        f.dets.forEachIndexed { i, d ->
            val col = DET_COL[d.type] ?: EDGE; val on = o.sel?.kind == "dets" && o.sel?.i == i
            val pts = listOf(P(d.u0, d.v0), P(d.u1, d.v0), P(d.u1, d.v1), P(d.u0, d.v1))
            for (k in 0..3) line(pts[k], pts[(k + 1) % 4], if (on) -1 else col, (if (on) 3f else 1.8f) * sc)
            val ue = if (d.sx == 1) d.u1 else d.u0; val ve = if (d.sy == 1) d.v1 else d.v0
            val wOut = if (d.sy == 1) (if (d.v1 > 0.995) -1 else 1) else (if (d.v0 < 0.005) 1 else -1)
            val hOut = if (d.sx == 1) (if (d.u1 > 0.995) -1 else 1) else (if (d.u0 < 0.005) 1 else -1)
            val tw = (listOf(0.0) + d.sw.sorted() + listOf(1.0)); val th = (listOf(0.0) + d.sh.sorted() + listOf(1.0))
            for (k in 0 until tw.size - 1) {
                val ua = d.u0 + (d.u1 - d.u0) * tw[k]; val ub = d.u0 + (d.u1 - d.u0) * tw[k + 1]
                val a = P(ua, ve); val b = P(ub, ve); arrow(a, b, col, lw)
                val mm = FacePrep.mmU(w, ua, ub)
                dimTag(FacePrep.detKey(d, if (tw.size == 2) "w" else "w${k + 1}"), "$mm", add(mid(a, b), floatArrayOf(0f, 1f), wOut * fs * 1.2f), col, fs, a, b)
            }
            for (k in 0 until th.size - 1) {
                val va = d.v0 + (d.v1 - d.v0) * th[k]; val vb = d.v0 + (d.v1 - d.v0) * th[k + 1]
                val a = P(ue, va); val b = P(ue, vb); arrow(a, b, col, lw)
                val mm = FacePrep.mmV(h, va, vb)
                dimTag(FacePrep.detKey(d, if (th.size == 2) "h" else "h${k + 1}"), "$mm", add(mid(a, b), floatArrayOf(1f, 0f), hOut * fs * 2.8f), col, fs, a, b)
            }
            for (t in d.sw) { val p = P(d.u0 + (d.u1 - d.u0) * t, ve); fill.color = col; c.drawCircle(p[0], p[1], 6f * sc, fill) }
            for (t in d.sh) { val p = P(ue, d.v0 + (d.v1 - d.v0) * t); fill.color = col; c.drawCircle(p[0], p[1], 6f * sc, fill) }
            for (loc in FacePrep.locators(face, f, i)) {
                val a = P(loc.a[0], loc.a[1]); val b = P(loc.b[0], loc.b[1])
                arrow(a, b, col, max(1.4f, lw * 0.6f), 6f * sc)
                val mm = FacePrep.locatorMm(w, h, loc)
                dimTag(FacePrep.detKey(d, if (loc.horizontal) "lh" else "lv"), "$mm from ${loc.to}",
                    add(mid(a, b), if (loc.horizontal) floatArrayOf(0f, 1f) else floatArrayOf(1f, 0f),
                        if (loc.horizontal) wOut * fs * 1.2f else hOut * fs * 5f), col, fs * 0.9f, a, b)
            }
            val ctr = P((d.u0 + d.u1) / 2, (d.v0 + d.v1) / 2)
            val small = abs(pts[1][0] - pts[0][0]) < fs * 9 || abs(pts[3][1] - pts[0][1]) < fs * 4
            tag(FacePrep.detName(f, i), if (small) floatArrayOf(ctr[0], min(pts[0][1], pts[1][1]) - fs * 1.6f) else ctr, col)
            if (o.handles == "dets") for (p in pts) grip(c, fill, stroke, p, if (on) -1 else col, (if (on) 10f else 8f) * sc)
        }
        // free measure lines
        f.lines.forEachIndexed { i, l ->
            val on = (o.sel?.kind == "line" || o.sel?.kind == "lsplit") && o.sel?.i == i
            val A = P(l.a[0], l.a[1]); val B = P(l.b[0], l.b[1]); val dir = unit(floatArrayOf(B[0] - A[0], B[1] - A[1]))
            val nrm = floatArrayOf(-dir[1], dir[0])
            val ts = listOf(0.0) + l.ts.sorted() + listOf(1.0); val tot = FacePrep.lineMm(w, h, l.a, l.b)
            for (k in 0 until ts.size - 1) {
                val a = l.at(ts[k]); val b = l.at(ts[k + 1]); val pa = P(a[0], a[1]); val pb = P(b[0], b[1])
                arrow(pa, pb, if (on) -1 else LINE, lw)
                val seg = (tot * (ts[k + 1] - ts[k])).roundToInt()
                dimTag(if (ts.size == 2) FacePrep.lineKey(l) else FacePrep.lineKey(l, k + 1), "$seg", add(mid(pa, pb), nrm, fs * 1.3f), LINE, fs, pa, pb)
            }
            if (ts.size > 2) dimTag(FacePrep.lineKey(l), "L${i + 1} $tot", add(mid(A, B), nrm, -fs * 1.5f), LINE, fs * 0.95f, A, B)
            else tag("L${i + 1}", add(A, nrm, -fs * 1.3f), LINE, fs * 0.85f)
            l.ts.forEachIndexed { j, t -> val q = l.at(t); val p = P(q[0], q[1]); val onj = o.sel?.kind == "lsplit" && o.sel?.i == i && o.sel?.j == j
                fill.color = LINE; c.drawCircle(p[0], p[1], (if (onj) 11f else 7f) * sc, fill)
                if (onj) { stroke.color = -1; stroke.strokeWidth = 3f * sc; c.drawCircle(p[0], p[1], 11f * sc, stroke) } }
            if (o.handles == "lines") for (p in listOf(A, B)) grip(c, fill, stroke, p, if (on) -1 else LINE, (if (on) 10f else 8f) * sc)
        }
        // the guide straight across the face through a divider being placed
        o.guide?.let { g ->
            val col = if (g.onRef) GUIDE_ON else GUIDE
            g.upright?.let { u -> line(P(u, 0.0), P(u, 1.0), col, 2.4f * sc, 14f * sc) }
            g.level?.let { v -> line(P(0.0, v), P(1.0, v), col, 2.4f * sc, 14f * sc) }
            o.guideAt?.let { at -> tag("${g.left} | ${g.right} mm", add(P(at[0], at[1]), floatArrayOf(0f, 1f), -fs * 2.4f), col, fs * 1.1f) }
        }
        o.boxGuide?.let { (sd, t) ->
            val horiz = sd == "top" || sd == "bottom"; val len = if (horiz) w else h; val a = (t * len).roundToInt()
            val (p, q) = if (horiz) P(t, 0.0) to P(t, 1.0) else P(0.0, t) to P(1.0, t)
            line(p, q, GUIDE, 2.4f * sc, 14f * sc)
            tag("$a | ${len - a} mm", add(mid(p, q), floatArrayOf(0f, 1f), -fs * 2.4f), GUIDE, fs * 1.1f)
        }
    }

    private fun grip(c: Canvas, fill: Paint, stroke: Paint, p: FloatArray, col: Int, r: Float) {
        fill.color = col; c.drawCircle(p[0], p[1], r, fill)
        stroke.color = 0xB3000000.toInt(); stroke.strokeWidth = 2f; c.drawCircle(p[0], p[1], r, stroke)
    }
    private fun argb(a: Float): Int = ((a.coerceIn(0f, 1f) * 255).toInt() shl 24) or 0xFFFFFF
}
