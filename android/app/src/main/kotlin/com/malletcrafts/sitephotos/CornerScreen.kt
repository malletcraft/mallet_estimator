package com.malletcrafts.sitephotos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.malletcrafts.sitephotos.pano.CaptureGeometry
import com.malletcrafts.sitephotos.pano.RoomCorners
import com.malletcrafts.sitephotos.pano.WallCorners
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * The room's eight corners, placed once, four screens of two.
 *
 * Amit, 2026-10-08: "corner placement is very tedious on app". It was four
 * drags per face, twenty-four a room, each one a corner another face already
 * had. A room has four vertical corners, so this screen walks them -- FL, FR,
 * BR, BL -- with a view aimed at each, and asks for its ceiling end and its
 * floor end. All six elevations come from those eight.
 *
 * Two aids, both chosen by Amit the same day:
 *  - A GRAB TAB on each dot, a thumb's height away from it (below the
 *    ceiling dot, above the floor dot), so the thumb never covers the corner
 *    it is placing. The magnifier still shows the point itself.
 *  - SNAP on release: the dot moves to the nearest corner-like feature in the
 *    photo (RoomCorners.snap), or stays put when there is none. On low-
 *    contrast walls it helps less -- the person still judges.
 *
 * Kept out of MainActivity: this module cannot be compiled in the cloud
 * container, and a small file with every import spelled out is cheap for CI.
 */
@Composable
internal fun RoomCornerDialog(
    session: FaceWriter.Session,
    plan: CaptureGeometry.Plan?,
    start: RoomCorners?,
    onDone: (RoomCorners) -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
) {
    var rc by remember { mutableStateOf(start ?: RoomCorners.start(plan)) }
    var step by remember { mutableStateOf(0) }          // which vertical corner, 0..3
    var dragging by remember { mutableStateOf(-1) }     // 0 ceiling, 1 floor
    var grab by remember { mutableStateOf(Offset.Zero) }
    val names = listOf("Front-left", "Front-right", "Back-right", "Back-left")

    // The view is fixed when the step is entered, so it does not swing under
    // the thumb while a dot moves.
    val viewYaw = remember(step) {
        val mid = WallCorners.unit(DoubleArray(3) { rc.ceiling[step][it] + rc.floor[step][it] })
        Math.toDegrees(atan2(mid[0], mid[2]))
    }
    val basis = remember(step) { WallCorners.basis(viewYaw, 0.0, VIEW_FOV) }
    val bmp = remember(step) {
        runCatching { FaceWriter.previewView(session, viewYaw, 0.0, VIEW_FOV, 720) }.getOrNull()
    }
    val image = remember(bmp) { bmp?.asImageBitmap() }

    fun pointOf(i: Int): Pair<Double, Double> =
        WallCorners.pointOf(basis, if (i == 0) rc.ceiling[step] else rc.floor[step]) ?: Pair(0.5, 0.5)
    fun setPoint(i: Int, x: Double, y: Double) {
        val d = WallCorners.rayAt(basis, x.coerceIn(0.0, 1.0), y.coerceIn(0.0, 1.0))
        rc = rc.with(if (i == 0) step else step + 4, d)
    }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("Room corners — ${names[step]} (${step + 1} of 4)") },
        text = {
            Column {
                Text("Drag the TOP dot to where this corner meets the ceiling and the " +
                     "BOTTOM dot to where it meets the floor. Drag by the tab; the dot " +
                     "snaps to the corner when you let go. Hidden by furniture? Put it " +
                     "where the edges would meet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().aspectRatio(1f).pointerInput(step) {
                    val tab = TAB_DP.dp.toPx()
                    detectDragGestures(
                        onDragStart = { o ->
                            var best = -1
                            var bestD = 0.13
                            for (i in 0..1) {
                                val (px, py) = pointOf(i)
                                val ty = py + (if (i == 0) tab else -tab) / size.height
                                val x = o.x / size.width.toDouble()
                                val y = o.y / size.height.toDouble()
                                val d = minOf(hypot(px - x, py - y), hypot(px - x, ty - y))
                                if (d < bestD) { bestD = d; best = i }
                            }
                            dragging = best
                            if (best >= 0) {
                                val (px, py) = pointOf(best)
                                grab = Offset((px * size.width - o.x).toFloat(), (py * size.height - o.y).toFloat())
                            }
                        },
                        onDragEnd = {
                            val i = dragging
                            if (i >= 0) {
                                val k = if (i == 0) step else step + 4
                                rc = rc.with(k, RoomCorners.snap(session.previewPano, rc[k]))
                            }
                            dragging = -1
                        },
                        onDragCancel = { dragging = -1 },
                        onDrag = { change, _ ->
                            if (dragging >= 0) {
                                change.consume()
                                setPoint(dragging,
                                    (change.position.x + grab.x) / size.width.toDouble(),
                                    (change.position.y + grab.y) / size.height.toDouble())
                            }
                        })
                }) {
                    if (image != null) {
                        Image(bitmap = image, contentDescription = "${names[step]} corner",
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    }
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width; val h = size.height
                        val tab = TAB_DP.dp.toPx()
                        val yellow = Color(0xFFFFD23F)
                        val pts = (0..1).map { i ->
                            val (x, y) = pointOf(i); Offset((x * w).toFloat(), (y * h).toFloat())
                        }
                        drawLine(Color.Black, pts[0], pts[1], strokeWidth = 6f)
                        drawLine(yellow, pts[0], pts[1], strokeWidth = 3f)
                        pts.forEachIndexed { i, p ->
                            val t = Offset(p.x, p.y + if (i == 0) tab else -tab)
                            drawLine(yellow, p, t, strokeWidth = 3f)
                            drawCircle(Color.Black, radius = 14f, center = p, style = Stroke(6f))
                            drawCircle(if (i == dragging) Color.White else yellow,
                                radius = 14f, center = p, style = Stroke(3f))
                            drawCircle(yellow, radius = 22f, center = t)
                            drawCircle(Color.Black, radius = 22f, center = t, style = Stroke(3f))
                        }
                        val d = dragging
                        if (d >= 0 && image != null) {
                            val p = pts[d]
                            val lens = 120.dp.toPx(); val zoom = 3f
                            val side = (lens / zoom * image.width / w).toInt().coerceAtLeast(8)
                            val sx = ((p.x / w * image.width).toInt() - side / 2)
                                .coerceIn(0, (image.width - side).coerceAtLeast(0))
                            val sy = ((p.y / h * image.height).toInt() - side / 2)
                                .coerceIn(0, (image.height - side).coerceAtLeast(0))
                            // Put the lens on the side away from the thumb.
                            val left = (p.x - lens / 2).coerceIn(0f, maxOf(0f, w - lens))
                            val top = if (p.y > h / 2) 8f else h - lens - 8f
                            drawImage(image, srcOffset = IntOffset(sx, sy), srcSize = IntSize(side, side),
                                dstOffset = IntOffset(left.toInt(), top.toInt()),
                                dstSize = IntSize(lens.toInt(), lens.toInt()))
                            drawRect(yellow, topLeft = Offset(left, top), size = Size(lens, lens), style = Stroke(4f))
                            drawLine(yellow, Offset(left + lens / 2, top), Offset(left + lens / 2, top + lens), strokeWidth = 2f)
                            drawLine(yellow, Offset(left, top + lens / 2), Offset(left + lens, top + lens / 2), strokeWidth = 2f)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row {
                    TextButton(enabled = step > 0, onClick = { step -= 1 }) { Text("◀ Previous") }
                    if (step < 3) TextButton(onClick = { step += 1 }) { Text("Next corner ▶") }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = step == 3, onClick = { onDone(rc) }) { Text("Use 8 corners") }
        },
        dismissButton = {
            Column {
                if (start != null) TextButton(onClick = onClear) { Text("Remove corners") }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        })
}

/** Wide enough to hold a corner's ceiling and floor ends from mid-room. */
private const val VIEW_FOV = 110.0

/** How far the grab tab sits from its dot: a thumb's height. */
private const val TAB_DP = 56

/**
 * One line per face once the eight are placed: is it a rectangle, and what
 * the photo says it measures. The photo gives every PROPORTION; the typed
 * height turns a wall's into millimetres, and a typed length or width is
 * shown beside it as the check.
 */
internal fun roomSummary(
    rc: RoomCorners,
    heightMm: Double?,
    lengthMm: Double?,
    widthMm: Double?,
): List<String> {
    val out = mutableListOf<String>()
    for ((face, name) in listOf("front" to "Front", "right" to "Right", "back" to "Back", "left" to "Left")) {
        val m = WallCorners.measure(rc.forFace(face))
        if (!m.valid) { out += "$name: corners cross over — fix them"; continue }
        val sq = "square ${"%.1f".format(m.outOfSquareDeg)}°"
        if (heightMm == null) { out += "$name: proportion ${"%.2f".format(m.ratio)} · $sq (type the height for mm)"; continue }
        val photo = m.lengthFor(heightMm)
        val tape = if (face == "front" || face == "back") lengthMm else widthMm
        out += "$name: ${Math.round(photo)} mm long" +
            (tape?.let { t ->
                val pc = (photo - t) / t * 100
                " (tape ${Math.round(t)}, ${if (pc >= 0) "+" else "−"}${"%.1f".format(kotlin.math.abs(pc))}%)"
            } ?: "") + " · $sq"
    }
    for ((face, name) in listOf("up" to "Ceiling", "down" to "Floor")) {
        val m = WallCorners.measure(rc.forFace(face))
        out += if (m.valid) "$name: square ${"%.1f".format(m.outOfSquareDeg)}°"
               else "$name: corners cross over — fix them"
    }
    return out
}
