package com.malletcrafts.sitephotos

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malletcrafts.sitephotos.pano.SurveyPlan
import com.malletcrafts.sitephotos.pano.SurveyPrep
import kotlin.math.roundToInt

/**
 * THE PLAN TAB (v52 slice 13, items 10 and 11): SIX DRAWINGS in one drawing area -- Floor · Ceiling · Front · Right ·
 * Back · Left. Amit, 14 Oct 2026 06:32 IST: "like plan i need elevation fo all walls and ceiling plan as well."; 20:45
 * IST: "where is other walls elevation diagram?"
 *
 * A row of six chips at the top of the drawing box picks the drawing (28 dp, radius 14, the open one gold; 24 dp in
 * landscape -- drawings.css .dvchip); the Canvas under it PAINTS SurveyPlan's display list and decides nothing: every
 * position, figure, colour and word comes from :pano, where it is tested against the prototype's suite. Pinch zooms about
 * the fingers, a drag moves a zoomed drawing, and "fit" (or "fit · 150%" while zoomed) fits it again; every pick fits
 * the drawing picked, and so does opening the tab. Nothing scrolls: the box takes the height the controls leave.
 */
@Composable
internal fun SpPlanPane(
    modifier: Modifier,
    prep: SurveyPrep.Prep,
    /** The room's L, W, H in use, mm. */
    roomSize: DoubleArray,
    /** The room's name, for the floor plan's line. */
    room: String,
    /** The screen's change counter: the drawing is rebuilt when it moves (a reading, a mark, an undo). */
    rev: Int,
    /** The drawing shown: one of SurveyPlan.VIEWS. */
    view: String,
    /** Bumped each time the Plan tab is opened: the drawing is fitted again. */
    fitKey: Int,
    land: Boolean,
    onView: (String) -> Unit,
    /** Over the bottom of the drawing: the screen's note and snackbar. */
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val l = roomSize[0]; val w = roomSize[1]; val h = roomSize[2]
    val d = remember(view, rev, prep, l, w, h, room) { SurveyPlan.drawing(view, prep, l, w, h, room) }
    // what part of the drawing is on screen, mm; null = fitted. A new drawing, or the tab opened again, fits it.
    var vb by remember(view, fitKey) { mutableStateOf<DoubleArray?>(null) }
    var cs by remember { mutableStateOf(IntSize.Zero) }
    // the long-lived gesture handler reads the CURRENT drawing, view and canvas size through this
    val gesture by rememberUpdatedState<(Offset, Offset, Float) -> Unit>(newValue = { c, pan, z ->
        val cw = cs.width.toDouble(); val ch = cs.height.toDouble()
        if (cw > 0 && ch > 0) {
            var v = vb ?: d.box
            if (z != 1f) v = SurveyPlan.zoom(v, d.box, z.toDouble(), c.x.toDouble(), c.y.toDouble(), cw, ch)
            v = SurveyPlan.pan(v, d.box, pan.x.toDouble(), pan.y.toDouble(), cw, ch)
            vb = if (SurveyPlan.fitted(v, d.box)) null else v
        }
    })
    val shown = vb ?: d.box
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF000000.toInt() or SurveyPlan.PAPER))) {
        Column(Modifier.fillMaxSize()) {
            // THE SIX CHIPS (drawings.css .dvchips): equal widths, 4 dp apart, 4 dp in from the box (3 dp at the top in landscape)
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = if (land) 3.dp else 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for ((v, lab) in SurveyPlan.VIEWS) {
                    val on = v == view
                    Box(Modifier.weight(1f).height(if (land) 24.dp else 28.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (on) PLAN_GOLD else PLAN_CHIP)
                        .clickable(role = Role.Tab) { vb = null; onView(v) }
                        .semantics { selected = on; contentDescription = SurveyPlan.DTITLE.getValue(v) },
                        contentAlignment = Alignment.Center) {
                        Text(lab, color = if (on) PLAN_ON_INK else PLAN_CHIP_INK, fontSize = 12.sp, lineHeight = 14.sp,
                            fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                    }
                }
            }
            // THE DRAWING: painted from the display list, fitted and centred (xMidYMid meet)
            Canvas(Modifier.weight(1f).fillMaxWidth().clipToBounds()
                .onSizeChanged { cs = it }
                .pointerInput(Unit) { detectTransformGestures { c, pan, z, _ -> gesture(c, pan, z) } }
                .semantics { contentDescription = d.title }) {
                drawIntoCanvas { c -> paintPlan(c.nativeCanvas, d, shown, size.width, size.height) }
            }
        }
        // fit: at the top right, under the chips (drawings.css .planv .fitcol top 38 / 32 dp); the zoom shows on it
        Text(SurveyPlan.fitLabel(shown, d.box), color = Color.White, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, softWrap = false,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = if (land) 32.dp else 38.dp, end = 6.dp)
                .clip(RoundedCornerShape(6.dp)).background(Color(0xAA000000)).clickable { vb = null }
                .padding(horizontal = 8.dp, vertical = 5.dp))
        overlay()
    }
}

/**
 * The line under the drawing (v42.css .planfoot, drawings.css #planinfo): which drawing it is -- "Front · 3310 × 2705 ·
 * 2 marks", "Living Room · 3310 × 5458" -- 11 sp on 14, never cut at 360 dp: one line, or two when it must.
 */
@Composable
internal fun SpPlanFoot(info: String) {
    Text(info, color = PLAN_MUTED, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, top = 6.dp, bottom = 2.dp))
}

/** A display list onto an [w] × [h] px canvas, [vb] (mm) shown whole and centred. Paints; decides nothing. */
private fun paintPlan(nc: android.graphics.Canvas, d: SurveyPlan.Drawing, vb: DoubleArray, w: Float, h: Float) {
    val k = SurveyPlan.scale(vb, w.toDouble(), h.toDouble())
    if (!(k > 0) || k.isInfinite()) return
    val ox = (w - vb[2] * k) / 2 - vb[0] * k; val oy = (h - vb[3] * k) / 2 - vb[1] * k
    fun px(v: Double) = (ox + v * k).toFloat()
    fun py(v: Double) = (oy + v * k).toFloat()
    fun len(v: Double) = (v * k).toFloat()
    fun argb(rgb: Int, a: Double = 1.0): Int = ((a.coerceIn(0.0, 1.0) * 255).roundToInt() shl 24) or (rgb and 0xFFFFFF)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val text = Paint(Paint.ANTI_ALIAS_FLAG)
    fun setStroke(c: Int, width: Double, dash: DoubleArray?) {
        stroke.color = argb(c); stroke.strokeWidth = len(width)
        stroke.pathEffect = dash?.takeIf { it.size >= 2 && it[0] > 0 && it[1] > 0 }?.let { DashPathEffect(floatArrayOf(len(it[0]), len(it[1])), 0f) }
    }
    for (item in d.items) when (item) {
        is SurveyPlan.Line -> { setStroke(item.colour, item.width, item.dash); nc.drawLine(px(item.x1), py(item.y1), px(item.x2), py(item.y2), stroke) }
        is SurveyPlan.Poly -> {
            val p = Path()
            if (item.rings.size > 1) p.fillType = Path.FillType.EVEN_ODD
            for (r in item.rings) {
                r.forEachIndexed { i, q -> if (i == 0) p.moveTo(px(q[0]), py(q[1])) else p.lineTo(px(q[0]), py(q[1])) }
                p.close()
            }
            val f = item.fill
            if (f != null && item.fillAlpha > 0) { fill.color = argb(f, item.fillAlpha); nc.drawPath(p, fill) }
            val s = item.stroke
            if (s != null && item.width > 0) { setStroke(s, item.width, item.dash); nc.drawPath(p, stroke) }
        }
        is SurveyPlan.Circle -> {
            val f = item.fill
            if (f != null) { fill.color = argb(f); nc.drawCircle(px(item.cx), py(item.cy), len(item.r), fill) }
            val s = item.stroke
            if (s != null && item.width > 0) { setStroke(s, item.width, item.dash); nc.drawCircle(px(item.cx), py(item.cy), len(item.r), stroke) }
        }
        is SurveyPlan.Ellipse -> {
            nc.save(); nc.rotate(item.rot.toFloat(), px(item.cx), py(item.cy))
            fill.color = argb(item.fill)
            nc.drawOval(RectF(px(item.cx - item.rx), py(item.cy - item.ry), px(item.cx + item.rx), py(item.cy + item.ry)), fill)
            nc.restore()
        }
        is SurveyPlan.Arc -> {
            setStroke(item.colour, item.width, item.dash)
            nc.drawArc(RectF(px(item.cx - item.r), py(item.cy - item.r), px(item.cx + item.r), py(item.cy + item.r)),
                item.start.toFloat(), item.sweep.toFloat(), false, stroke)
        }
        is SurveyPlan.Text -> {
            text.color = argb(item.colour); text.textSize = len(item.size)
            text.typeface = if (item.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            text.textAlign = when (item.anchor) {
                SurveyPlan.Anchor.START -> Paint.Align.LEFT
                SurveyPlan.Anchor.MIDDLE -> Paint.Align.CENTER
                SurveyPlan.Anchor.END -> Paint.Align.RIGHT
            }
            // dominant-baseline middle: the text's middle on y
            val fm = text.fontMetrics; val base = -(fm.ascent + fm.descent) / 2
            val x = px(item.x); val y = py(item.y)
            if (item.rot != 0.0) { nc.save(); nc.rotate(item.rot.toFloat(), x, y) }
            nc.drawText(item.text, x, y + base, text)
            if (item.rot != 0.0) nc.restore()
        }
    }
}

private val PLAN_GOLD = Color(0xFFE0B040)
/** A chip not open (drawings.css .dvchip: #E4E0D2 on #3F433A), and the open one's ink (#1A1A12). */
private val PLAN_CHIP = Color(0xFFE4E0D2)
private val PLAN_CHIP_INK = Color(0xFF3F433A)
private val PLAN_ON_INK = Color(0xFF1A1A12)
private val PLAN_MUTED = Color(0xFF9AA096)
