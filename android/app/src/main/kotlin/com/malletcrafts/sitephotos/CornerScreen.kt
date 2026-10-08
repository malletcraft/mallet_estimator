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
 * Set the room exactly as SketchUp's Match Photo is set, at two diagonally
 * opposite corners.
 *
 * Amit, 2026-10-08, on 0.3.174's one line per wall, "not useful": "i need set
 * origin and then vanishing perspective lines just as foto match of sketchup
 * using handles . setting ceiling and floor lines running from origing to
 * other direction is easiest way along with floor corner."
 *
 * Screens 1 and 2 look straight at a corner:
 *  - the YELLOW ORIGIN grip goes on the corner, at the floor;
 *  - a RED pair of bars goes on one wall -- one bar on the skirting, one on
 *    the cornice -- and a GREEN pair on the other wall. Every bar has a
 *    handle at each end and is laid by its handles, as in SketchUp. The bars
 *    need not touch the origin; put them wherever the edge shows clearly.
 * The pairs give each wall's vanishing direction and the ceiling bars the
 * ceiling, so the walls need not be square. Screens 3-6 show each wall
 * square on with the room drawn over it, to check the fit before using it.
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
    // The bars stay where they were laid, whatever the room does -- they are
    // what the person SAW, the room is what follows from it.
    var bars by remember {
        mutableStateOf(RoomQuad.Line.entries.associateWith { start.startBars(it) })
    }
    var origins by remember {
        mutableStateOf(RoomQuad.Origin.entries.associateWith { o ->
            start.vertical(o).first.let { WallCorners.unit(it) } })
    }
    // What the finger holds: the origin (-1), or handle 0..3 of a line's bars.
    var held by remember { mutableStateOf<Pair<RoomQuad.Line?, Int>?>(null) }
    var finger by remember { mutableStateOf<Offset?>(null) }

    val corner = step < 2
    val origin = if (step == 0) RoomQuad.Origin.FL else RoomQuad.Origin.BR
    val face = if (corner) null else RoomQuad.WALLS[step - 2]
    val title = when (step) {
        0 -> "Front-left corner"; 1 -> "Back-right corner"
        else -> "${face!!.replaceFirstChar { it.uppercase() }} wall — check"
    }
    // Red is the front/back direction, green the left/right -- SketchUp's two
    // axes, here allowed to be out of square.
    fun colourOf(l: RoomQuad.Line) =
        if (l == RoomQuad.Line.FRONT || l == RoomQuad.Line.BACK) AXIS_RED else AXIS_GREEN

    // The view is fixed when the step is entered, so it does not swing under
    // the thumb while the room changes.
    val view = remember(step) { if (corner) room.cornerView(origin) else room.view(face!!) }
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

    fun screen(ray: DoubleArray) = WallCorners.pointOf(basis, ray)

    /** Everything grabbable on this screen, as (what, ray). */
    fun grips(): List<Pair<Pair<RoomQuad.Line?, Int>, DoubleArray>> =
        listOf<Pair<Pair<RoomQuad.Line?, Int>, DoubleArray>>((null to -1) to origins.getValue(origin)) +
            room.lines(origin).flatMap { l ->
                bars.getValue(l).handles().mapIndexed { i, r -> (l to i) to r }
            }

    fun refit() { room = room.fitted(origin, origins.getValue(origin), bars) }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("$title (${step + 1} of 6)") },
        text = {
            Column {
                Text(
                    if (corner)
                        "YELLOW origin on the corner at the FLOOR. Then, as in SketchUp, lay " +
                        "each bar by its two handles: RED pair on one wall, GREEN on the " +
                        "other — one bar on the skirting, one on the ceiling edge. Bars " +
                        "can sit anywhere the edge shows; walls need not be square."
                    else
                        "Check the room lines sit on this wall's edges. If not, go back " +
                        "and move the bars on the corner screens.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().aspectRatio(1f).pointerInput(step) {
                    if (!corner) return@pointerInput
                    detectDragGestures(
                        onDragStart = { o ->
                            val x = o.x / size.width.toDouble()
                            val y = o.y / size.height.toDouble()
                            var best: Pair<RoomQuad.Line?, Int>? = null
                            var bestD = GRAB_RADIUS
                            for ((what, r) in grips()) {
                                val s = screen(r) ?: continue
                                val d = hypot(s.first - x, s.second - y)
                                if (d < bestD) { bestD = d; best = what }
                            }
                            held = best
                            finger = if (best != null) o else null
                        },
                        onDragEnd = { held = null; finger = null },
                        onDragCancel = { held = null; finger = null },
                        onDrag = { change, _ ->
                            val h = held
                            if (h != null) {
                                change.consume()
                                val x = (change.position.x / size.width.toDouble()).coerceIn(0.0, 1.0)
                                val y = (change.position.y / size.height.toDouble()).coerceIn(0.0, 1.0)
                                val ray = WallCorners.rayAt(basis, x, y)
                                val l = h.first
                                if (l == null) origins = origins + (origin to ray)
                                else bars = bars + (l to bars.getValue(l).with(h.second, ray))
                                refit()
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
                        fun at(p: Pair<Double, Double>) = Offset((p.first * w).toFloat(), (p.second * h).toFloat())
                        fun drawSeg(seg: Pair<DoubleArray, DoubleArray>, colour: Color, width: Float) {
                            val pts = trace(seg)
                            for (k in 0 until pts.size - 1) {
                                val a = pts[k]; val b = pts[k + 1]
                                if (a != null && b != null) {
                                    drawLine(Color.Black, at(a), at(b), strokeWidth = width + 3f)
                                    drawLine(colour, at(a), at(b), strokeWidth = width)
                                }
                            }
                        }
                        fun grip(r: DoubleArray, colour: Color, big: Boolean) {
                            val s = screen(r) ?: return
                            drawCircle(Color.Black, radius = if (big) 18f else 14f, center = at(s))
                            drawCircle(colour, radius = if (big) 14f else 10f, center = at(s))
                        }
                        // The room that follows from the bars, thin.
                        for (seg in room.allEdges()) drawSeg(seg, Color(0x99FFFFFF), 2f)
                        if (corner) {
                            drawSeg(room.vertical(origin), AXIS_YELLOW, 3f)
                            for (l in room.lines(origin)) {
                                val b = bars.getValue(l)
                                val c = colourOf(l)
                                // A bar is drawn straight between its handles on
                                // screen, as SketchUp draws it.
                                for ((p, q) in listOf(b.floorA to b.floorB, b.ceilA to b.ceilB)) {
                                    val sp = screen(p); val sq = screen(q)
                                    if (sp != null && sq != null) {
                                        drawLine(Color.Black, at(sp), at(sq), strokeWidth = 8f)
                                        drawLine(c, at(sp), at(sq), strokeWidth = 5f)
                                    }
                                }
                                b.handles().forEachIndexed { i, r ->
                                    grip(r, if (held == (l to i)) Color.White else c, false)
                                }
                            }
                            grip(origins.getValue(origin),
                                if (held == (null to -1)) Color.White else AXIS_YELLOW, true)
                        } else {
                            val (fl, cl) = room.wallEdges(face!!)
                            val l = when (face) {
                                "front" -> RoomQuad.Line.FRONT; "back" -> RoomQuad.Line.BACK
                                "left" -> RoomQuad.Line.LEFT; else -> RoomQuad.Line.RIGHT
                            }
                            drawSeg(fl, colourOf(l), 5f)
                            drawSeg(cl, colourOf(l), 5f)
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
private val AXIS_YELLOW = Color(0xFFFFD23F)

/** Points per edge when an edge is drawn or hit-tested. */
private const val SAMPLES = 24

/** How near (fraction of the view) a touch must be to grab a handle. */
private const val GRAB_RADIUS = 0.07

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
