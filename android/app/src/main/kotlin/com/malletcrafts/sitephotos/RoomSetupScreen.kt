package com.malletcrafts.sitephotos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.malletcrafts.sitephotos.pano.RoomQuad
import com.malletcrafts.sitephotos.pano.RoomSetup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Set the room: four floor corners on the straight-down photo, then one
 * ceiling line. Approved by Amit on 2026-10-10 ("verify yourself if my method
 * looks correct? and if so ... deploy it") after the method was checked
 * numerically; it replaces Match Photo and Claude's corner finder ("Remove it,
 * Amit marks corners") -- one way in, no fallback.
 *
 *  1 Floor -- the 360 is levelled by gravity, so the face looking straight
 *    down is a true parallel picture of the floor. Drag the four dots onto
 *    the floor corners; the typed carpet length and width give the scale and
 *    the camera height, and the status says how well the two agree.
 *  2 Front | Back and 3 Left | Right -- each wall squared on between its floor
 *    corners. The dashed floor line is fixed by step 1; drag only the level
 *    ceiling line. It is one height, so every wall follows.
 *
 * The pictures are only ever SCALED and SLID (- / + / reset, one finger off a
 * dot slides when zoomed) -- never turned. All maths is RoomSetup in :pano,
 * where it is tested; this file draws and listens.
 */
@Composable
internal fun RoomSetupDialog(
    session: FaceWriter.Session,
    title: String,
    lengthMm: Double,
    widthMm: Double,
    heightMm: Double,
    /** Dots from an earlier setting, or null to start from the carpet. */
    startDots: List<DoubleArray>?,
    onDone: (RsResult) -> Unit,
    onCancel: () -> Unit,
) {
    var dots by remember { mutableStateOf(startDots ?: RoomSetup.startPoints(lengthMm, widthMm)) }
    var moved by remember { mutableStateOf(List(4) { startDots != null }) }
    var ceilingMm by remember { mutableStateOf(heightMm.coerceIn(RoomSetup.H_MIN, RoomSetup.H_MAX)) }
    var step by remember { mutableStateOf(0) }
    var view by remember { mutableStateOf(RoomSetup.View()) }
    var history by remember { mutableStateOf(listOf<RsSnap>()) }
    var floorImg by remember { mutableStateOf<ImageBitmap?>(null) }
    // Bumped when a wall picture must be cut again: a new step, or a ceiling
    // line let go. During a drag only the line moves.
    var wallKey by remember { mutableStateOf(0) }
    var walls by remember { mutableStateOf<List<RsWall>>(emptyList()) }

    val plan = RoomSetup.plan(dots, lengthMm, widthMm)
    val placed = moved.count { it }
    val quad = if (plan != null) RoomSetup.roomQuad(plan, ceilingMm) else null
    val ok = placed == 4 && plan != null && plan.agrees && quad != null

    fun snapshot() {
        history = (history + RsSnap(dots, moved, ceilingMm)).takeLast(40)
    }

    LaunchedEffect(Unit) {
        floorImg = withContext(Dispatchers.Default) {
            FaceWriter.bitmapOf(RoomSetup.floorFace(session.previewPano, RS_FLOOR_PX)).asImageBitmap()
        }
    }
    LaunchedEffect(step, wallKey) {
        if (step == 0 || plan == null) { walls = emptyList(); return@LaunchedEffect }
        val names = if (step == 1) listOf("front", "back") else listOf("left", "right")
        val p = plan; val h = ceilingMm
        walls = withContext(Dispatchers.Default) {
            names.map { n ->
                val (_, li, ri) = RoomSetup.wall(n)
                val w = RoomSetup.wall(session.previewPano, p.corners[li], p.corners[ri], p.h, h, RS_WALL_PX)
                RsWall(n, li, ri, w, FaceWriter.bitmapOf(w.image!!).asImageBitmap())
            }
        }
    }

    Dialog(onDismissRequest = { }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = RS_BG) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onCancel) { Text("← Cancel", color = RS_FG) }
                    Text("$title · set the room", Modifier.weight(1f),
                        color = RS_FG, fontWeight = FontWeight.Bold)
                    TextButton(enabled = history.isNotEmpty(), onClick = {
                        val s = history.last(); history = history.dropLast(1)
                        dots = s.dots; moved = s.moved; ceilingMm = s.ceilingMm; wallKey += 1
                    }) { Text("↶ Undo", color = RS_FG) }
                }
                Text("Carpet ${lengthMm.roundToInt()} front→back × ${widthMm.roundToInt()} " +
                    "left→right mm · ceiling ${ceilingMm.roundToInt()} mm",
                    style = MaterialTheme.typography.labelSmall, color = RS_MUTED)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((i, label) in listOf("1 Floor", "2 Front | Back", "3 Left | Right").withIndex()) {
                        FilterChip(selected = step == i,
                            onClick = { if (step != i) { walls = emptyList(); step = i; wallKey += 1 } },
                            enabled = i == 0 || plan != null,
                            label = { Text(label) })
                    }
                }
                Spacer(Modifier.height(6.dp))

                if (step == 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Floor · straight down · front at top", Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall, color = RS_MUTED)
                        TextButton(onClick = { view = view.zoomAt(1 / RoomSetup.View.STEP, 0.5, 0.5) }) {
                            Text("−", color = RS_FG) }
                        TextButton(onClick = { view = view.zoomAt(RoomSetup.View.STEP, 0.5, 0.5) }) {
                            Text("+", color = RS_FG) }
                        TextButton(onClick = { view = RoomSetup.View() }) { Text("⟲", color = RS_FG) }
                    }
                    val img = floorImg
                    Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Color.Black)
                        .pointerInput(img) {
                            if (img == null) return@pointerInput
                            var held = -1
                            detectDragGestures(
                                onDragStart = { o ->
                                    val sw = size.width.toDouble(); val sh = size.height.toDouble()
                                    var best = -1; var bestD = RS_GRAB_DP.dp.toPx().toDouble()
                                    for (i in 0 until 4) {
                                        val s = rsFloorToScreen(dots[i], view, img)
                                        val d = hypot(s[0] * sw - o.x, s[1] * sh - o.y)
                                        if (d < bestD) { bestD = d; best = i }
                                    }
                                    held = best
                                    if (best >= 0) snapshot()
                                },
                                onDragEnd = { held = -1 },
                                onDragCancel = { held = -1 },
                                onDrag = { change, drag ->
                                    change.consume()
                                    val dx = drag.x / size.width.toDouble()
                                    val dy = drag.y / size.height.toDouble()
                                    val i = held
                                    if (i >= 0) {
                                        val f = RoomSetup.floorFrac(dots[i][0], dots[i][1], RS_FLOOR_PX)
                                        val u = (f[0] + dx / view.z).coerceIn(0.0, 1.0)
                                        val v = (f[1] + dy / view.z).coerceIn(0.0, 1.0)
                                        dots = dots.mapIndexed { k, p ->
                                            if (k == i) RoomSetup.floorPoint(u, v, RS_FLOOR_PX) else p }
                                        moved = moved.mapIndexed { k, m -> m || k == i }
                                    } else if (view.z > 1.0) {
                                        view = view.slid(dx, dy)
                                    }
                                })
                        }) {
                        if (img == null) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        } else {
                            Canvas(Modifier.fillMaxSize()) { rsDrawFloor(img, view, dots, moved, plan) }
                        }
                    }
                } else {
                    Text("Drag the solid ceiling line onto the cornice. The dashed floor line is from step 1.",
                        style = MaterialTheme.typography.labelSmall, color = RS_MUTED)
                    Spacer(Modifier.height(4.dp))
                    if (walls.isEmpty()) {
                        CircularProgressIndicator(Modifier.padding(16.dp))
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (rw in walls) {
                                val w = rw.wall
                                Column(Modifier.weight(1f)) {
                                    Text("${rw.name.replaceFirstChar { it.uppercase() }} · " +
                                        "${w.lengthMm.roundToInt()} × ${ceilingMm.roundToInt()} mm",
                                        style = MaterialTheme.typography.labelSmall, color = RS_FG)
                                    Box(Modifier.fillMaxWidth()
                                        .aspectRatio(w.widthPx.toFloat() / w.heightPx.toFloat())
                                        .background(Color.Black)
                                        .pointerInput(w) {
                                            var dragging = false
                                            detectDragGestures(
                                                onDragStart = { o ->
                                                    val y = w.fracOf(ceilingMm) * size.height
                                                    dragging = abs(o.y - y) < RS_GRAB_DP.dp.toPx() * 1.5
                                                    if (dragging) snapshot()
                                                },
                                                onDragEnd = { if (dragging) wallKey += 1; dragging = false },
                                                onDragCancel = { if (dragging) wallKey += 1; dragging = false },
                                                onDrag = { change, drag ->
                                                    if (dragging) {
                                                        change.consume()
                                                        val f = w.fracOf(ceilingMm) + drag.y / size.height.toDouble()
                                                        ceilingMm = w.heightAt(f).coerceIn(RoomSetup.H_MIN, RoomSetup.H_MAX)
                                                    }
                                                })
                                        }) {
                                        Canvas(Modifier.fillMaxSize()) { rsDrawWall(rw, ceilingMm) }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(rsStatus(placed, plan, ceilingMm, quad != null), style = MaterialTheme.typography.bodySmall,
                    color = when { placed < 4 -> RS_MUTED; ok -> RS_OK; else -> RS_BAD })
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCancel) { Text("Cancel") }
                    Button(enabled = ok, modifier = Modifier.weight(1f), onClick = {
                        val q = quad
                        if (q != null) onDone(RsResult(q, ceilingMm, dots))
                    }) { Text("Save room") }
                }
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}

/** What the screen hands back: the room, the ceiling-line height, the dots. */
internal class RsResult(val quad: RoomQuad, val heightMm: Double, val dots: List<DoubleArray>)

private class RsWall(val name: String, val left: Int, val right: Int,
                     val wall: RoomSetup.Wall, val img: ImageBitmap)

private class RsSnap(val dots: List<DoubleArray>, val moved: List<Boolean>, val ceilingMm: Double)

/** The floor face is rendered once at this size; 1024 keeps a quarter pixel
 *  of tap error at about 5 mm. */
private const val RS_FLOOR_PX = 1024
private const val RS_WALL_PX = 480
private const val RS_GRAB_DP = 28

private val RS_BG = Color(0xFF111311)
private val RS_FG = Color(0xFFECEEE8)
private val RS_MUTED = Color(0xFF9AA096)
private val RS_OK = Color(0xFF7DFF8A)
private val RS_BAD = Color(0xFFFF9B7A)
private val RS_LINE = Color(0xFFE0B040)
private val RS_DOTS = listOf(Color(0xFFFF6B6B), Color(0xFF4DABF7), Color(0xFF69DB7C), Color(0xFFDA77F2))

/** The integer source window the magnifier draws from: left, top, side px. */
private fun rsWindow(view: RoomSetup.View, img: ImageBitmap): IntArray {
    val win = view.window()
    val side = (win[2] * img.width).roundToInt().coerceIn(1, img.width)
    val left = (win[0] * img.width).roundToInt().coerceIn(0, img.width - side)
    val top = (win[1] * img.height).roundToInt().coerceIn(0, img.height - side)
    return intArrayOf(left, top, side)
}

/** A floor dot to fractions of the screen box, through the SAME integer
 *  window the picture is drawn with, so a dot sits on its pixel at any zoom. */
private fun rsFloorToScreen(p: DoubleArray, view: RoomSetup.View, img: ImageBitmap): DoubleArray {
    val f = RoomSetup.floorFrac(p[0], p[1], RS_FLOOR_PX)
    val w = rsWindow(view, img)
    return doubleArrayOf((f[0] * img.width - w[0]) / w[2], (f[1] * img.height - w[1]) / w[2])
}

private fun rsText(scope: DrawScope, text: String, x: Float, y: Float, colour: Int, size: Float = 34f) {
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        color = colour
        isFakeBoldText = true
        setShadowLayer(6f, 0f, 0f, android.graphics.Color.BLACK)
    }
    scope.drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}

private fun DrawScope.rsDrawFloor(
    img: ImageBitmap, view: RoomSetup.View, dots: List<DoubleArray>, moved: List<Boolean>,
    plan: RoomSetup.Plan?,
) {
    val w = rsWindow(view, img)
    drawImage(img, srcOffset = IntOffset(w[0], w[1]), srcSize = IntSize(w[2], w[2]),
        dstOffset = IntOffset(0, 0), dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
    val pts = dots.map { rsFloorToScreen(it, view, img).let { s ->
        Offset((s[0] * size.width).toFloat(), (s[1] * size.height).toFloat()) } }
    for (i in 0 until 4) {
        val a = pts[i]; val b = pts[(i + 1) % 4]
        drawLine(Color.Black, a, b, strokeWidth = 6f)
        drawLine(RS_LINE, a, b, strokeWidth = 3f)
        if (plan != null) {
            rsText(this, "${plan.sidesMm[i].roundToInt()} mm", (a.x + b.x) / 2 + 8f, (a.y + b.y) / 2 - 8f,
                android.graphics.Color.rgb(0xE0, 0xB0, 0x40))
        }
    }
    for (i in 0 until 4) {
        val c = if (moved[i]) RS_DOTS[i] else RS_DOTS[i].copy(alpha = 0.55f)
        drawCircle(c, radius = 22f, center = pts[i])
        drawCircle(Color.White, radius = 22f, center = pts[i], style = Stroke(4f))
        // A cross at the centre: the point that counts is under it, not the disc.
        drawLine(Color.White, pts[i] - Offset(8f, 0f), pts[i] + Offset(8f, 0f), strokeWidth = 2f)
        drawLine(Color.White, pts[i] - Offset(0f, 8f), pts[i] + Offset(0f, 8f), strokeWidth = 2f)
        rsText(this, RoomSetup.NAMES[i] + if (moved[i]) "" else " (drag)",
            pts[i].x + 28f, pts[i].y + 10f, android.graphics.Color.WHITE, 30f)
    }
}

private fun DrawScope.rsDrawWall(rw: RsWall, ceilingMm: Double) {
    val img = rw.img; val w = rw.wall
    drawImage(img, srcOffset = IntOffset(0, 0), srcSize = IntSize(img.width, img.height),
        dstOffset = IntOffset(0, 0), dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
    for ((s, idx) in listOf(0.0 to rw.left, 1.0 to rw.right)) {
        val x = (w.xFracOf(s) * size.width).toFloat()
        drawLine(RS_DOTS[idx], Offset(x, 0f), Offset(x, size.height), strokeWidth = 3f)
    }
    val yf = (w.fracOf(0.0) * size.height).toFloat()
    drawLine(Color.Black, Offset(0f, yf), Offset(size.width, yf), strokeWidth = 6f)
    drawLine(RS_LINE, Offset(0f, yf), Offset(size.width, yf), strokeWidth = 3f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f))
    val yc = (w.fracOf(ceilingMm) * size.height).toFloat()
    drawLine(Color.Black, Offset(0f, yc), Offset(size.width, yc), strokeWidth = 7f)
    drawLine(RS_LINE, Offset(0f, yc), Offset(size.width, yc), strokeWidth = 4f)
    rsText(this, "ceiling — drag", 8f, yc - 10f, android.graphics.Color.rgb(0xE0, 0xB0, 0x40), 26f)
    rsText(this, "floor", 8f, yf - 10f, android.graphics.Color.rgb(0xE0, 0xB0, 0x40), 26f)
}

private fun rsStatus(placed: Int, plan: RoomSetup.Plan?, ceilingMm: Double, closes: Boolean): String {
    if (plan == null) return "Move the dots apart — they have no extent yet."
    if (placed == 4 && plan.agrees && !closes)
        return "These corners do not make a room round the camera (or the ceiling is below it) — check each dot is on its own corner."
    val head = if (placed < 4) "$placed of 4 floor corners placed. "
        else "Corners and carpet sizes agree to ${"%.1f".format(plan.agreement * 100)}%" +
            (if (plan.agrees) ". " else " — must be 3% or better to save. ")
    return head + "Camera ${plan.h.roundToInt()} mm above the floor. From the corners: " +
        "width ${plan.widthMm.roundToInt()} (typed ${plan.typedWidth.roundToInt()}), " +
        "length ${plan.lengthMm.roundToInt()} (typed ${plan.typedLength.roundToInt()}). " +
        "Ceiling ${ceilingMm.roundToInt()} mm."
}
