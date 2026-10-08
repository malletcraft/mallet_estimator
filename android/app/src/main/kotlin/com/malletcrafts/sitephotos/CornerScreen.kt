package com.malletcrafts.sitephotos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.malletcrafts.sitephotos.pano.CaptureGeometry
import com.malletcrafts.sitephotos.pano.Handover
import com.malletcrafts.sitephotos.pano.Panorama
import com.malletcrafts.sitephotos.pano.WallCorners
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Four handles on one wall face, and the elevation they produce.
 *
 * Amit, 2026-10-07: "use only foto and corners ... can i place 4 corners on
 * flat foto so that only that will be used to mesure?" -- after rejecting
 * laser guides as one more job on site. So this screen is the whole
 * correction: no level, no pitch or roll slider. The four corners define the
 * wall's plane, and WallCorners does the rest.
 *
 * THE MAGNIFIER IS NOT DECORATION. A thumb covers the corner it is placing,
 * and a corner placed a few pixels off is the one error this method cannot
 * correct for itself -- so a 3x view of what is under the finger is drawn
 * above it while it moves, the way the browser prototype had it.
 *
 * Kept out of MainActivity on purpose: this module cannot be compiled in the
 * cloud container, and a small file with every import spelled out is the
 * cheapest thing for CI to tell us about.
 */
@Composable
internal fun CornerDialog(
    session: FaceWriter.Session,
    face: String,
    fov: Double,
    turn: Double,
    plan: CaptureGeometry.Plan?,
    start: List<DoubleArray>?,
    onDone: (List<DoubleArray>) -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
) {
    val (_, nominalYaw, pitch) = Panorama.FACES.first { it.first == face }
    val basis = remember(face, fov, turn) { WallCorners.basis(nominalYaw + turn, pitch, fov) }
    val bmp = remember(face, fov, turn) {
        runCatching { FaceWriter.previewFace(session, face, fov, turn, 768) }.getOrNull()
    }
    val image = remember(bmp) { bmp?.asImageBitmap() }
    var pts by remember(face) {
        mutableStateOf(
            (start ?: WallCorners.startCorners(face, nominalYaw, fov, plan, pitch))
                .map { d -> WallCorners.pointOf(basis, d) ?: Pair(0.5, 0.5) })
    }
    var dragging by remember { mutableStateOf(-1) }
    var finger by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    val dirs = pts.map { (x, y) -> WallCorners.rayAt(basis, x, y) }
    val m = WallCorners.measure(dirs)
    // Rendered when the finger lifts, not on every move: a re-sample per
    // drag event would make the handle lag behind the thumb.
    val elev = remember(pts, dragging) {
        if (dragging < 0 && m.valid)
            runCatching { FaceWriter.previewElevation(session, dirs, 480) }.getOrNull()
        else null
    }
    val label = when (face) { "up" -> "Ceiling"; "down" -> "Floor"; else -> Handover.FACE_LABELS[face] ?: face }

    AlertDialog(
        onDismissRequest = { },
        title = { Text(if (face in WallCorners.PLATE_FACES) "$label — 4 corners" else "$label wall — 4 corners") },
        text = {
            Column {
                Text("Drag the four dots onto the corners: top-left, " +
                     "top-right, bottom-right, bottom-left. A corner behind " +
                     "furniture goes where the two visible edges would meet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().aspectRatio(1f).pointerInput(face, fov, turn) {
                    detectDragGestures(
                        onDragStart = { o ->
                            val x = o.x / size.width.toDouble()
                            val y = o.y / size.height.toDouble()
                            var best = -1
                            var bestD = 0.12
                            pts.forEachIndexed { i, p ->
                                val d = hypot(p.first - x, p.second - y)
                                if (d < bestD) { bestD = d; best = i }
                            }
                            dragging = best
                            if (best >= 0) finger = Pair(x, y)
                        },
                        onDragEnd = { dragging = -1; finger = null },
                        onDragCancel = { dragging = -1; finger = null },
                        onDrag = { change, _ ->
                            if (dragging >= 0) {
                                change.consume()
                                val x = (change.position.x / size.width.toDouble()).coerceIn(0.0, 1.0)
                                val y = (change.position.y / size.height.toDouble()).coerceIn(0.0, 1.0)
                                val i = dragging
                                pts = pts.mapIndexed { k, p -> if (k == i) Pair(x, y) else p }
                                finger = Pair(x, y)
                            }
                        })
                }) {
                    if (image != null) {
                        Image(bitmap = image, contentDescription = "$label face",
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    }
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val o = pts.map { (x, y) -> Offset((x * w).toFloat(), (y * h).toFloat()) }
                        val edge = if (m.square) Color(0xFF6CC08B) else Color(0xFFFFD23F)
                        for (i in 0 until 4) drawLine(edge, o[i], o[(i + 1) % 4], strokeWidth = 4f)
                        o.forEachIndexed { i, c ->
                            drawCircle(Color.Black, radius = 16f, center = c, style = Stroke(6f))
                            drawCircle(if (i == dragging) Color.White else Color(0xFFFFD23F),
                                radius = 16f, center = c, style = Stroke(4f))
                        }
                        val f = finger
                        if (f != null && image != null) {
                            val lens = 120.dp.toPx()
                            val zoom = 3f
                            val scale = image.width / w
                            val srcSide = (lens / zoom * scale).toInt().coerceAtLeast(8)
                            val sx = ((f.first * image.width).toInt() - srcSide / 2)
                                .coerceIn(0, (image.width - srcSide).coerceAtLeast(0))
                            val sy = ((f.second * image.height).toInt() - srcSide / 2)
                                .coerceIn(0, (image.height - srcSide).coerceAtLeast(0))
                            val cx = (f.first * w).toFloat()
                            val cy = (f.second * h).toFloat()
                            val left = (cx - lens / 2).coerceIn(0f, maxOf(0f, w - lens))
                            val top = (cy - lens * 1.25f).let { if (it < 0f) cy + lens * 0.25f else it }
                            drawImage(image, srcOffset = IntOffset(sx, sy), srcSize = IntSize(srcSide, srcSide),
                                dstOffset = IntOffset(left.toInt(), top.toInt()),
                                dstSize = IntSize(lens.toInt(), lens.toInt()))
                            drawRect(Color(0xFFFFD23F), topLeft = Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(lens, lens), style = Stroke(4f))
                            drawLine(Color(0xFFFFD23F), Offset(left + lens / 2, top), Offset(left + lens / 2, top + lens), strokeWidth = 2f)
                            drawLine(Color(0xFFFFD23F), Offset(left, top + lens / 2), Offset(left + lens, top + lens / 2), strokeWidth = 2f)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(measureLine(face, m, plan),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (m.square) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.error)
                if (elev != null) {
                    Spacer(Modifier.height(6.dp))
                    Text("Elevation from these 4 corners",
                        style = MaterialTheme.typography.labelSmall)
                    Image(bitmap = elev.asImageBitmap(), contentDescription = "$label elevation",
                        modifier = Modifier.fillMaxWidth()
                            .aspectRatio(elev.width.toFloat() / elev.height.toFloat()),
                        contentScale = ContentScale.FillBounds)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = m.valid, onClick = { onDone(dirs) }) { Text("Use corners") }
        },
        dismissButton = {
            Column {
                if (start != null) TextButton(onClick = onClear) { Text("Remove corners") }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        })
}

/** One line under the handles: is it a rectangle, and does the photo agree
 *  with the tape? The photo gives the wall's PROPORTION on its own; with the
 *  height from the tape, that is a length to compare. */
internal fun measureLine(face: String, m: WallCorners.Measure, plan: CaptureGeometry.Plan?): String {
    if (!m.valid) return "Corners cross over — put them back in order: " +
        "top-left, top-right, bottom-right, bottom-left."
    val sq = "out of square ${"%.1f".format(m.outOfSquareDeg)}°"
    if (plan == null) return "$sq · width:height ${"%.2f".format(m.ratio)} (room not measured)"
    // A wall is read against the room height; the ceiling and floor have no
    // height, so their WIDTH is the reference and the length is compared.
    val plate = face in WallCorners.PLATE_FACES
    val tape = if (face == "front" || face == "back" || plate) plan.lengthIn else plan.widthIn
    val photo = m.lengthFor(if (plate) plan.widthIn else plan.heightIn)
    val pct = (photo - tape) / tape * 100
    return "$sq · photo says ${mm(photo)} mm, tape ${mm(tape)} mm " +
        "(${if (pct >= 0) "+" else "−"}${"%.1f".format(kotlin.math.abs(pct))}%)"
}
