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
import com.malletcrafts.sitephotos.pano.RoomBox
import com.malletcrafts.sitephotos.pano.WallCorners
import kotlin.math.hypot

/**
 * Fit the room box: drag its EDGES onto the room's edges, one wall at a time.
 *
 * Amit, 2026-10-08, after the eight corner dots: "setting up corners is still
 * difficult. how bout seting axex like this?" -- SketchUp's Match Photo, where
 * lines are laid on edges instead of dots on corners. Then: "Build straight
 * into APK".
 *
 * Each screen looks straight at one wall, with the box drawn over the photo:
 *  - the GREEN floor line: drag its middle to move the wall nearer or further;
 *    drag near an END and that end follows while the other end stays put,
 *    which turns the room until the line lies along the skirting;
 *  - the BLUE ceiling line: the ceiling;
 *  - the YELLOW verticals: the walls on either side.
 * A line is matched along its whole length, so a corner hidden by a wardrobe
 * no longer matters as long as some of the edge shows. The magnifier follows
 * the finger.
 *
 * Kept out of MainActivity: this module cannot be compiled in the cloud
 * container, and a small file with every import spelled out is cheap for CI.
 */
@Composable
internal fun RoomBoxDialog(
    session: FaceWriter.Session,
    start: RoomBox,
    isEdit: Boolean,
    onDone: (RoomBox) -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
) {
    var box by remember { mutableStateOf(start) }
    var step by remember { mutableStateOf(0) }
    var edge by remember { mutableStateOf<RoomBox.Edge?>(null) }
    var grabAt by remember { mutableStateOf(0.5) }
    var finger by remember { mutableStateOf<Offset?>(null) }
    val face = RoomBox.WALLS[step]
    val names = listOf("Front", "Right", "Back", "Left")

    // The view is fixed when the wall is entered, so it does not swing under
    // the thumb while the box changes.
    val view = remember(step) { box.view(face) }
    val basis = remember(step) { WallCorners.basis(view.first, 0.0, view.second) }
    val bmp = remember(step) {
        runCatching { FaceWriter.previewView(session, view.first, 0.0, view.second, 720) }.getOrNull()
    }
    val image = remember(bmp) { bmp?.asImageBitmap() }

    /** A 3D segment as screen points (0..1), null where it is behind the view. */
    fun trace(seg: Pair<DoubleArray, DoubleArray>, n: Int = SAMPLES): List<Pair<Double, Double>?> =
        (0..n).map { k ->
            val t = k.toDouble() / n
            WallCorners.pointOf(basis, WallCorners.unit(DoubleArray(3) {
                seg.first[it] * (1 - t) + seg.second[it] * t }))
        }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("Room box — ${names[step]} wall (${step + 1} of 4)") },
        text = {
            Column {
                Text("Lay the lines on this wall's edges. GREEN on the floor line: drag " +
                     "its middle to move it, an end to turn it. BLUE on the ceiling line. " +
                     "YELLOW on the two corners. Only part of an edge needs to show.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().aspectRatio(1f).pointerInput(step) {
                    detectDragGestures(
                        onDragStart = { o ->
                            val x = o.x / size.width.toDouble()
                            val y = o.y / size.height.toDouble()
                            var best: RoomBox.Edge? = null
                            var bestD = GRAB_RADIUS
                            var bestAt = 0.5
                            for ((e, seg) in box.edges(face)) {
                                trace(seg).forEachIndexed { k, p ->
                                    if (p != null) {
                                        val d = hypot(p.first - x, p.second - y)
                                        if (d < bestD) { bestD = d; best = e; bestAt = k.toDouble() / SAMPLES }
                                    }
                                }
                            }
                            edge = best; grabAt = bestAt
                            finger = if (best != null) o else null
                        },
                        onDragEnd = { edge = null; finger = null },
                        onDragCancel = { edge = null; finger = null },
                        onDrag = { change, _ ->
                            val e = edge
                            if (e != null) {
                                change.consume()
                                val x = (change.position.x / size.width.toDouble()).coerceIn(0.0, 1.0)
                                val y = (change.position.y / size.height.toDouble()).coerceIn(0.0, 1.0)
                                box = box.dragged(face, e, WallCorners.rayAt(basis, x, y), grabAt)
                                finger = change.position
                            }
                        })
                }) {
                    if (image != null) {
                        Image(bitmap = image, contentDescription = "${names[step]} wall",
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    }
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width; val h = size.height
                        fun drawSeg(seg: Pair<DoubleArray, DoubleArray>, colour: Color, width: Float) {
                            val pts = trace(seg)
                            for (k in 0 until pts.size - 1) {
                                val a = pts[k]; val b = pts[k + 1]
                                if (a != null && b != null) {
                                    val pa = Offset((a.first * w).toFloat(), (a.second * h).toFloat())
                                    val pb = Offset((b.first * w).toFloat(), (b.second * h).toFloat())
                                    drawLine(Color.Black, pa, pb, strokeWidth = width + 3f)
                                    drawLine(colour, pa, pb, strokeWidth = width)
                                }
                            }
                        }
                        // The rest of the box, thin, so the room reads as a box.
                        for (seg in box.allEdges()) drawSeg(seg, Color(0x99FFFFFF), 2f)
                        for ((e, seg) in box.edges(face)) {
                            val c = when (e) {
                                RoomBox.Edge.FLOOR -> Color(0xFF6CC08B)
                                RoomBox.Edge.CEILING -> Color(0xFF4FC3F7)
                                else -> Color(0xFFFFD23F)
                            }
                            drawSeg(seg, if (e == edge) Color.White else c, 5f)
                        }
                        val f = finger
                        if (f != null && image != null) {
                            val lens = 120.dp.toPx(); val zoom = 3f
                            val side = (lens / zoom * image.width / w).toInt().coerceAtLeast(8)
                            val sx = ((f.x / w * image.width).toInt() - side / 2)
                                .coerceIn(0, (image.width - side).coerceAtLeast(0))
                            val sy = ((f.y / h * image.height).toInt() - side / 2)
                                .coerceIn(0, (image.height - side).coerceAtLeast(0))
                            val left = (f.x - lens / 2).coerceIn(0f, maxOf(0f, w - lens))
                            val top = if (f.y > h / 2) 8f else h - lens - 8f
                            drawImage(image, srcOffset = IntOffset(sx, sy), srcSize = IntSize(side, side),
                                dstOffset = IntOffset(left.toInt(), top.toInt()),
                                dstSize = IntSize(lens.toInt(), lens.toInt()))
                            val yellow = Color(0xFFFFD23F)
                            drawRect(yellow, topLeft = Offset(left, top), size = Size(lens, lens), style = Stroke(4f))
                            drawLine(yellow, Offset(left + lens / 2, top), Offset(left + lens / 2, top + lens), strokeWidth = 2f)
                            drawLine(yellow, Offset(left, top + lens / 2), Offset(left + lens, top + lens / 2), strokeWidth = 2f)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(boxSummary(box, session.heightMm, null, null).first(),
                    style = MaterialTheme.typography.labelSmall)
                Row {
                    TextButton(enabled = step > 0, onClick = { step -= 1 }) { Text("◀ Previous") }
                    if (step < 3) TextButton(onClick = { step += 1 }) { Text("Next wall ▶") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDone(box) }) { Text("Use room box") }
        },
        dismissButton = {
            Column {
                if (isEdit) TextButton(onClick = onClear) { Text("Remove room box") }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        })
}

/** Points per edge when an edge is drawn or hit-tested. */
private const val SAMPLES = 24

/** How near (fraction of the view) a touch must be to grab an edge. */
private const val GRAB_RADIUS = 0.08

/**
 * What the fitted box says about the room. Every face is a rectangle by
 * construction, so the useful numbers are the room's own: its length and
 * width in mm from the typed height, the camera's height, and -- when a
 * length or width was typed -- how far the photo is from the tape.
 */
internal fun boxSummary(box: RoomBox, heightMm: Double?, lengthMm: Double?, widthMm: Double?): List<String> {
    if (heightMm == null) return listOf("Type the room height for sizes in mm.")
    val (l, w, cam) = box.sizeMm(heightMm)
    fun vs(photo: Double, tape: Double?) = tape?.let { t ->
        val pc = (photo - t) / t * 100
        " (tape ${Math.round(t)}, ${if (pc >= 0) "+" else "−"}${"%.1f".format(kotlin.math.abs(pc))}%)"
    } ?: ""
    return listOf(
        "Room ${Math.round(l)} × ${Math.round(w)} × ${Math.round(heightMm)} mm",
        "Length ${Math.round(l)} mm${vs(l, lengthMm)}",
        "Width ${Math.round(w)} mm${vs(w, widthMm)}",
        "Camera ${Math.round(cam)} mm above the floor")
}
