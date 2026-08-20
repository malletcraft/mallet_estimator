package com.malletcrafts.sitephotos

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.malletcrafts.sitephotos.pano.Annotation
import com.malletcrafts.sitephotos.pano.Handover
import kotlin.math.max
import kotlin.math.min

/**
 * Where MCFT's faces are found and measured. Locating: the faces live in
 * MediaStore exactly where FaceWriter put them (relative path from the
 * capture's client/project/room, filename from the capture id + face label),
 * so no extra bookkeeping exists to go stale.
 */
object FaceFiles {
    /** Uri of one face image, or null if it's gone from MediaStore. */
    fun findFace(context: Context, deviceId: String, face: String): Uri? {
        val name = Handover.filename(deviceId, face)
        val proj = arrayOf(MediaStore.Images.Media._ID)
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj,
            "${MediaStore.Images.Media.DISPLAY_NAME} = ?", arrayOf(name),
            null)?.use { c ->
            if (c.moveToFirst()) {
                return ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(0))
            }
        }
        return null
    }

    /** The four wall faces first — they're what gets measured; top/bottom
     *  follow for completeness. */
    val ORDER = listOf("front", "right", "back", "left", "up", "down")
}

private const val MAX_DECODE = 2400   // px, longest side, plenty for measuring

private fun loadBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts)
    }
    var sample = 1
    while (max(opts.outWidth, opts.outHeight) / sample > MAX_DECODE) sample *= 2
    val opts2 = BitmapFactory.Options().apply { inSampleSize = sample }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts2)
    }
}.getOrNull()

/**
 * The annotation editor for one face. Interactions, chosen for gloved
 * thumbs on site:
 *  - tap–tap places a measurement line (first tap anchors, second ends,
 *    then the dimension dialog opens);
 *  - dragging near an endpoint moves it, with a MAGNIFIER LOUPE above the
 *    finger so the endpoint lands exactly on the wall edge;
 *  - tapping a line's label re-opens its dimension;
 *  - long-press drops a note pin;
 *  - the m/ft chip toggles dual metric·imperial labels everywhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnotateScreen(
    deviceId: String,
    face: String,
    store: AnnotationStore,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uri = remember { FaceFiles.findFace(context, deviceId, face) }
    val bitmap: ImageBitmap? = remember(uri) {
        uri?.let { loadBitmap(context, it)?.asImageBitmap() }
    }
    var ann by remember { mutableStateOf(store.load(deviceId, face)) }
    var showImperial by remember {
        mutableStateOf(context.getSharedPreferences("capture", Context.MODE_PRIVATE)
            .getBoolean("imperial", false))
    }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }

    // In-flight gestures. pending = first tap of a new line waiting for its
    // second; dragging = (line index, endpoint 1|2); magnifier follows the
    // finger while dragging.
    var pending by remember { mutableStateOf<Offset?>(null) }
    var dragging by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var finger by remember { mutableStateOf<Offset?>(null) }
    var editingLine by remember { mutableStateOf<Int?>(null) }
    var notePos by remember { mutableStateOf<Offset?>(null) }

    fun persist(updated: Annotation.FaceAnnotations) {
        ann = updated
        store.save(deviceId, face, updated)
        SyncWorker.syncNow(context)
    }

    fun norm(o: Offset) = Pair(
        (o.x / canvasSize.width).toDouble().coerceIn(0.0, 1.0),
        (o.y / canvasSize.height).toDouble().coerceIn(0.0, 1.0))

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(Handover.FACE_LABELS[face] ?: face) },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            actions = {
                FilterChip(selected = showImperial, onClick = {
                    showImperial = !showImperial
                    context.getSharedPreferences("capture", Context.MODE_PRIVATE)
                        .edit().putBoolean("imperial", showImperial).apply()
                }, label = { Text("m · ft") })
            })
    }) { pad ->
        if (bitmap == null) {
            Text("This face's image is no longer on the phone.",
                Modifier.padding(pad).padding(16.dp))
            return@Scaffold
        }
        Column(Modifier.padding(pad)) {
            Text("Tap–tap: measure · drag ends to adjust · long-press: note",
                Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodySmall)
            Canvas(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { canvasSize = it }
                .pointerInput(ann) {
                    detectTapGestures(
                        onTap = { o ->
                            val (nx, ny) = norm(o)
                            // A tap on an existing label re-opens its value.
                            val hit = ann.lines.indexOfFirst { l ->
                                val mx = (l.x1 + l.x2) / 2; val my = (l.y1 + l.y2) / 2
                                Math.abs(mx - nx) < 0.06 && Math.abs(my - ny) < 0.05
                            }
                            if (hit >= 0) { editingLine = hit; return@detectTapGestures }
                            val p = pending
                            if (p == null) pending = o
                            else {
                                val (ax, ay) = norm(p)
                                val updated = ann.copy(lines = ann.lines +
                                    Annotation.Line(ax, ay, nx, ny, 0))
                                ann = updated
                                pending = null
                                editingLine = updated.lines.lastIndex
                            }
                        },
                        onLongPress = { o -> notePos = o })
                }
                .pointerInput(ann) {
                    detectDragGestures(
                        onDragStart = { o ->
                            val (nx, ny) = norm(o)
                            for ((i, l) in ann.lines.withIndex()) {
                                val end = Annotation.nearestEndpoint(l, nx, ny, 0.08)
                                if (end > 0) { dragging = i to end; break }
                            }
                        },
                        onDrag = { change, _ ->
                            finger = change.position
                            val d = dragging ?: return@detectDragGestures
                            val (nx, ny) = norm(change.position)
                            val l = ann.lines[d.first]
                            val nl = if (d.second == 1) l.copy(x1 = nx, y1 = ny)
                                     else l.copy(x2 = nx, y2 = ny)
                            ann = ann.copy(lines = ann.lines.toMutableList()
                                .also { it[d.first] = nl })
                        },
                        onDragEnd = {
                            if (dragging != null) persist(ann)
                            dragging = null; finger = null
                        })
                }
            ) {
                // Face image, letterboxed to fill.
                drawImage(bitmap, dstSize = IntSize(size.width.toInt(), size.height.toInt()))

                fun px(x: Double, y: Double) = Offset(
                    (x * size.width).toFloat(), (y * size.height).toFloat())

                for ((i, l) in ann.lines.withIndex()) {
                    val a = px(l.x1, l.y1); val b = px(l.x2, l.y2)
                    drawLine(Color(0xFFFF3D00), a, b, strokeWidth = 5f)
                    drawCircle(Color(0xFFFF3D00), 14f, a, style = Stroke(5f))
                    drawCircle(Color(0xFFFF3D00), 14f, b, style = Stroke(5f))
                    val mid = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
                    val text = if (l.mm > 0) Annotation.label(l.mm, showImperial) else "?"
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            textSize = 42f; color = android.graphics.Color.WHITE
                            setShadowLayer(6f, 0f, 0f, android.graphics.Color.BLACK)
                            isFakeBoldText = true
                        }
                        drawText(text, mid.x - paint.measureText(text) / 2,
                            mid.y - 14f, paint)
                    }
                }
                for (p in ann.pins) {
                    val at = px(p.x, p.y)
                    drawCircle(Color(0xFF2962FF), 16f, at)
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            textSize = 36f; color = android.graphics.Color.WHITE
                            setShadowLayer(6f, 0f, 0f, android.graphics.Color.BLACK)
                        }
                        drawText(p.text.take(24), at.x + 22f, at.y + 12f, paint)
                    }
                }
                pending?.let { drawCircle(Color(0xFFFF3D00), 18f, it, style = Stroke(6f)) }

                // The magnifier: a 3x loupe above the finger while dragging,
                // so the endpoint can be planted exactly on the wall line.
                finger?.let { f ->
                    val r = 120f
                    val center = Offset(f.x, max(r + 20f, f.y - 260f))
                    val path = Path().apply { addOval(
                        androidx.compose.ui.geometry.Rect(center - Offset(r, r),
                            center + Offset(r, r))) }
                    clipPath(path) {
                        val zoom = 3f
                        drawImage(bitmap,
                            dstSize = IntSize((size.width * zoom).toInt(),
                                (size.height * zoom).toInt()),
                            dstOffset = androidx.compose.ui.unit.IntOffset(
                                (center.x - f.x * zoom).toInt(),
                                (center.y - f.y * zoom).toInt()))
                        // crosshair on the exact point
                        drawLine(Color.Red, Offset(center.x - 20, center.y),
                            Offset(center.x + 20, center.y), 3f)
                        drawLine(Color.Red, Offset(center.x, center.y - 20),
                            Offset(center.x, center.y + 20), 3f)
                    }
                    drawCircle(Color.White, r, center, style = Stroke(6f))
                }
            }
        }
    }

    editingLine?.let { idx ->
        DimensionDialog(
            initialMm = ann.lines.getOrNull(idx)?.mm ?: 0,
            onDismiss = {
                // A brand-new line abandoned without a value is discarded —
                // an unmeasured line is noise, not data.
                if (ann.lines.getOrNull(idx)?.mm == 0)
                    persist(ann.copy(lines = ann.lines.filterIndexed { i, _ -> i != idx }))
                editingLine = null
            },
            onDelete = {
                persist(ann.copy(lines = ann.lines.filterIndexed { i, _ -> i != idx }))
                editingLine = null
            },
            onSave = { mm ->
                persist(ann.copy(lines = ann.lines.toMutableList()
                    .also { it[idx] = it[idx].copy(mm = mm) }))
                editingLine = null
            })
    }

    notePos?.let { o ->
        NoteDialog(
            onDismiss = { notePos = null },
            onSave = { text ->
                val (nx, ny) = norm(o)
                persist(ann.copy(pins = ann.pins + Annotation.Pin(nx, ny, text)))
                notePos = null
            })
    }
}

@Composable
private fun DimensionDialog(
    initialMm: Int,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var value by remember {
        mutableStateOf(if (initialMm > 0) initialMm.toString() else "")
    }
    var unit by remember { mutableStateOf(Annotation.Unit.MM) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Measurement") },
        text = {
            Column {
                OutlinedTextField(value = value, onValueChange = { value = it },
                    label = { Text("Value") }, singleLine = true)
                Row {
                    Annotation.Unit.entries.forEach { u ->
                        FilterChip(selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u.label) },
                            modifier = Modifier.padding(end = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                value.toDoubleOrNull()?.let { v ->
                    if (v > 0) onSave(Annotation.toMm(v, unit))
                }
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        })
}

@Composable
private fun NoteDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Note") },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it },
                label = { Text("What's here?") })
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onSave(text.trim()) }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

/** The six faces of one capture: tap to annotate. Wall faces first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacesScreen(
    capture: CaptureStore.Capture,
    annStore: AnnotationStore,
    onFace: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("${capture.projectTitle} — ${capture.room}") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } })
    }) { pad ->
        Column(Modifier.padding(pad).padding(12.dp)) {
            Text("Annotate every wall — measured faces are what the " +
                "SketchUp model gets built from.",
                style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            FaceFiles.ORDER.forEach { face ->
                val present = remember(face) {
                    FaceFiles.findFace(context, capture.deviceId, face) != null
                }
                val ann = annStore.load(capture.deviceId, face)
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    onClick = { if (present) onFace(face) }) {
                    Row(Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(Handover.FACE_LABELS[face] ?: face)
                        Text(when {
                            !present -> "image not on this phone"
                            ann.isEmpty -> "not annotated"
                            else -> "${ann.lines.size} lines · ${ann.pins.size} notes"
                        }, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
