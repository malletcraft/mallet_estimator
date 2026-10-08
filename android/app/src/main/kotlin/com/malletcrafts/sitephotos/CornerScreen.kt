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
import com.malletcrafts.sitephotos.pano.RoomQuad
import com.malletcrafts.sitephotos.pano.WallCorners
import kotlin.math.hypot

/**
 * Set the room the SketchUp way: two diagonally opposite corners, each with
 * its two wall lines, then a check of each wall.
 *
 * Amit, 2026-10-08, after the room box: "get me exact setup like sketchup. i
 * will set only two diagonally opposite corners of a room" -- the box failing
 * him on "Room not square". Two corners alone fix only a rectangle, so each
 * corner carries its two wall lines as SketchUp's origin carries the red and
 * green axes, and each line turns on its own. The other two corners fall
 * where the lines cross.
 *
 * Screens:
 *  1. FRONT-LEFT corner, looked at straight: YELLOW dot on the corner at the
 *     floor, BLUE dot on the corner at the ceiling, RED line along the front
 *     wall's skirting, GREEN along the left wall's.
 *  2. BACK-RIGHT corner, the same: RED along the back wall, GREEN the right.
 *  3-6. Each wall square on: the floor line can still be turned (it pivots on
 *     its placed corner) and the ceiling line moved, where the far end of the
 *     wall shows better than it did from the corner.
 * Grab any part of a line; the magnifier follows the finger.
 *
 * Kept out of MainActivity: this module cannot be compiled in the cloud
 * container, and a small file with every import spelled out is cheap for CI.
 */
@Composable
internal fun RoomQuadDialog(
    session: FaceWriter.Session,
    start: RoomQuad,
    isEdit: Boolean,
    onDone: (RoomQuad) -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
) {
    var room by remember { mutableStateOf(start) }
    var step by remember { mutableStateOf(0) }
    // What the finger holds: a handle, or (on a wall screen) the ceiling line.
    var held by remember { mutableStateOf<RoomQuad.Handle?>(null) }
    var holdingCeiling by remember { mutableStateOf(false) }
    var finger by remember { mutableStateOf<Offset?>(null) }

    val corner = step < 2
    val face = if (corner) null else RoomQuad.WALLS[step - 2]
    val floorDot = if (step == 0) RoomQuad.Handle.FL_FLOOR else RoomQuad.Handle.BR_FLOOR
    val ceilDot = if (step == 0) RoomQuad.Handle.FL_CEILING else RoomQuad.Handle.BR_CEILING
    // Red runs along front and back, green along left and right -- SketchUp's
    // two axes, here allowed to be out of square.
    val lines = if (step == 0) listOf(RoomQuad.Handle.FRONT, RoomQuad.Handle.LEFT)
                else listOf(RoomQuad.Handle.BACK, RoomQuad.Handle.RIGHT)
    val title = when (step) {
        0 -> "Front-left corner"; 1 -> "Back-right corner"
        else -> "${face!!.replaceFirstChar { it.uppercase() }} wall — check"
    }

    // The view is fixed when the step is entered, so it does not swing under
    // the thumb while the room changes.
    val view = remember(step) { if (corner) room.cornerView(floorDot) else room.view(face!!) }
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

    fun screen(p: DoubleArray) = WallCorners.pointOf(basis, WallCorners.unit(p))

    /** The segments that can be grabbed on this step, with what each one moves. */
    fun grabbable(r: RoomQuad): List<Pair<RoomQuad.Handle?, Pair<DoubleArray, DoubleArray>>> =
        if (corner) lines.map { h -> h to r.ray3d(h, LINE_LENGTH) }
        else r.wallEdges(face!!).let { (fl, cl) -> listOf(r.lineOf(face!!) to fl, null to cl) }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("$title (${step + 1} of 6)") },
        text = {
            Column {
                Text(
                    if (corner)
                        "YELLOW dot on the corner where it meets the FLOOR, BLUE dot where " +
                        "it meets the CEILING. Then lay the RED and GREEN lines along the " +
                        "skirting of the two walls — grab any part of a line. Walls need " +
                        "not be square."
                    else
                        "Check the box sits on this wall. If the far end is off, drag the " +
                        "RED or GREEN floor line onto the skirting (it turns on its placed corner), " +
                        "and the BLUE line onto the ceiling.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().aspectRatio(1f).pointerInput(step) {
                    detectDragGestures(
                        onDragStart = { o ->
                            val x = o.x / size.width.toDouble()
                            val y = o.y / size.height.toDouble()
                            var best: RoomQuad.Handle? = null
                            var ceil = false
                            var bestD = GRAB_RADIUS
                            if (corner) {
                                for ((h, p) in listOf(floorDot to room.vertical(floorDot).first,
                                                      ceilDot to room.vertical(ceilDot).second)) {
                                    val s = screen(p) ?: continue
                                    val d = hypot(s.first - x, s.second - y)
                                    if (d < bestD) { bestD = d; best = h }
                                }
                            }
                            // A dot within reach wins over a line through it.
                            if (best == null) {
                                for ((h, seg) in grabbable(room)) {
                                    trace(seg).forEachIndexed { k, p ->
                                        // the first stretch of a corner's line is the corner
                                        if (p != null && !(corner && k < SAMPLES / 12)) {
                                            val d = hypot(p.first - x, p.second - y)
                                            if (d < bestD) { bestD = d; best = h; ceil = h == null }
                                        }
                                    }
                                }
                            }
                            held = best; holdingCeiling = ceil
                            finger = if (best != null || ceil) o else null
                        },
                        onDragEnd = { held = null; holdingCeiling = false; finger = null },
                        onDragCancel = { held = null; holdingCeiling = false; finger = null },
                        onDrag = { change, _ ->
                            val h = held
                            if (h != null || holdingCeiling) {
                                change.consume()
                                val x = (change.position.x / size.width.toDouble()).coerceIn(0.0, 1.0)
                                val y = (change.position.y / size.height.toDouble()).coerceIn(0.0, 1.0)
                                val ray = WallCorners.rayAt(basis, x, y)
                                room = if (h != null) room.dragged(h, ray) else room.ceilingFrom(face!!, ray)
                                finger = change.position
                            }
                        })
                }) {
                    if (image != null) {
                        Image(bitmap = image, contentDescription = title,
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
                        fun colourOf(hd: RoomQuad.Handle?) = when (hd) {
                            RoomQuad.Handle.FRONT, RoomQuad.Handle.BACK -> AXIS_RED
                            RoomQuad.Handle.LEFT, RoomQuad.Handle.RIGHT -> AXIS_GREEN
                            else -> AXIS_BLUE
                        }
                        // The whole room, thin, so it reads as a room.
                        for (seg in room.allEdges()) drawSeg(seg, Color(0x99FFFFFF), 2f)
                        if (corner) {
                            drawSeg(room.vertical(floorDot), AXIS_YELLOW, 3f)
                            for (hd in lines) {
                                drawSeg(room.ray3d(hd, LINE_LENGTH), if (hd == held) Color.White else colourOf(hd), 5f)
                            }
                            for ((hd, p, c) in listOf(Triple(floorDot, room.vertical(floorDot).first, AXIS_YELLOW),
                                                       Triple(ceilDot, room.vertical(ceilDot).second, AXIS_BLUE))) {
                                val s = screen(p) ?: continue
                                val o = Offset((s.first * w).toFloat(), (s.second * h).toFloat())
                                drawCircle(Color.Black, radius = 16f, center = o)
                                drawCircle(if (hd == held) Color.White else c, radius = 12f, center = o)
                            }
                        } else {
                            val (fl, cl) = room.wallEdges(face!!)
                            drawSeg(fl, if (held != null) Color.White else colourOf(room.lineOf(face)), 5f)
                            drawSeg(cl, if (holdingCeiling) Color.White else AXIS_BLUE, 5f)
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
                            drawRect(AXIS_YELLOW, topLeft = Offset(left, top), size = Size(lens, lens), style = Stroke(4f))
                            drawLine(AXIS_YELLOW, Offset(left + lens / 2, top), Offset(left + lens / 2, top + lens), strokeWidth = 2f)
                            drawLine(AXIS_YELLOW, Offset(left, top + lens / 2), Offset(left + lens, top + lens / 2), strokeWidth = 2f)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                for (line in roomSummary(room, session.heightMm, null, null).take(2)) {
                    Text(line, style = MaterialTheme.typography.labelSmall)
                }
                Row {
                    TextButton(enabled = step > 0, onClick = { step -= 1 }) { Text("◀ Back") }
                    if (step < 5) TextButton(onClick = { step += 1 }) { Text("Next ▶") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDone(room) }) { Text("Use room") }
        },
        dismissButton = {
            Column {
                if (isEdit) TextButton(onClick = onClear) { Text("Remove room") }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        })
}

private val AXIS_RED = Color(0xFFE5484D)
private val AXIS_GREEN = Color(0xFF6CC08B)
private val AXIS_BLUE = Color(0xFF4FC3F7)
private val AXIS_YELLOW = Color(0xFFFFD23F)

/** How far a corner's wall line is drawn, in camera heights: past the room. */
private const val LINE_LENGTH = 12.0

/** Points per edge when an edge is drawn or hit-tested. */
private const val SAMPLES = 24

/** How near (fraction of the view) a touch must be to grab an edge. */
private const val GRAB_RADIUS = 0.08

/**
 * What the room says, in mm: the four walls, how far each corner is from
 * square, and the camera height. The typed length is compared with the
 * front and back walls and the typed width with the sides.
 */
internal fun roomSummary(room: RoomQuad, heightMm: Double?, lengthMm: Double?, widthMm: Double?): List<String> {
    if (heightMm == null) return listOf("Type the room height for sizes in mm.")
    val (f, r, b, l) = room.wallsMm(heightMm)
    val angles = room.cornerAngles()
    fun vs(photo: Double, tape: Double?) = tape?.let { t ->
        val pc = (photo - t) / t * 100
        " (tape ${Math.round(t)}, ${if (pc >= 0) "+" else "−"}${"%.1f".format(kotlin.math.abs(pc))}%)"
    } ?: ""
    return listOf(
        "Walls F ${Math.round(f)} · R ${Math.round(r)} · B ${Math.round(b)} · L ${Math.round(l)} mm",
        "Corners FL ${"%.1f".format(angles[0])}° · FR ${"%.1f".format(angles[1])}° · " +
            "BR ${"%.1f".format(angles[2])}° · BL ${"%.1f".format(angles[3])}°",
        "Length (front/back) ${Math.round((f + b) / 2)} mm${vs((f + b) / 2, lengthMm)}",
        "Width (sides) ${Math.round((l + r) / 2)} mm${vs((l + r) / 2, widthMm)}",
        "Camera ${Math.round(room.mmPerUnit(heightMm))} mm above the floor")
}
