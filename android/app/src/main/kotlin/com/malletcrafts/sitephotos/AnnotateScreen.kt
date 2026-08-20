package com.malletcrafts.sitephotos

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.malletcrafts.sitephotos.pano.Annotation
import com.malletcrafts.sitephotos.pano.Handover
import kotlin.math.abs
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

/** What the toolbar acts on. Null means nothing is selected. */
private sealed interface Sel {
    data class L(val i: Int) : Sel
    data class P(val i: Int) : Sel
    data class Q(val i: Int) : Sel
}

/** Grab targets for a one-finger drag: a line endpoint, a pin, or one
 *  corner of a tagged opening. */
private sealed interface Grab {
    data class End(val i: Int, val end: Int) : Grab
    data class Pin(val i: Int) : Grab
    data class Corner(val i: Int, val corner: Int) : Grab
}

/** Colour per tag, so a wall reads at a glance: openings warm, structure
 *  cold. Structure is what a carpenter must work around, not through. */
private fun kindColor(kind: String): Color = when (Annotation.Kind.of(kind)) {
    Annotation.Kind.WINDOW -> Color(0xFF00B0FF)
    Annotation.Kind.DOOR -> Color(0xFF00E676)
    Annotation.Kind.COLUMN -> Color(0xFFAB47BC)
    Annotation.Kind.BEAM -> Color(0xFFFFA000)
    Annotation.Kind.OPENING -> Color(0xFFBDBDBD)
}

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 10f
private const val HANDLE_PX = 46f     // fingertip-sized grab radius, screen px
private const val SNAP_PX = 22f       // endpoints within this snap together

/**
 * The annotation editor for one face — rebuilt against the ImageMeter
 * benchmark (see mcft-erpnext-context execution/DESIGN.md §11).
 *
 * The three things that made the first version unusable, and what replaced
 * them:
 *  - the photo could not be ZOOMED, so precision was impossible on a 2400 px
 *    face squeezed into a phone width. Now: pinch to zoom (to 10x), one
 *    finger drags the photo, and every coordinate is stored in the image's
 *    own normalized space so zoom never changes what was measured.
 *  - the photo was STRETCHED to the canvas, so what the eye lined a
 *    measurement up against was a distorted room. Now: aspect-correct fit,
 *    letterboxed.
 *  - there was no SELECTION, so nothing could act on "the measure you mean".
 *    Now a tap selects; the toolbar (value, delete) acts on the selection —
 *    and this is the hook the Leica DISTO needs, because the laser's
 *    contract is "the value lands in the selected measure".
 *
 * Placement follows ImageMeter too: "+ Measure" DROPS a line into the middle
 * of what you are looking at and you drag its ends onto the wall, instead of
 * asking for two blind taps. The loupe sits in a fixed top corner rather than
 * riding above the finger, so it can never be pushed off-screen and the
 * other hand never covers it.
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
    val undo = remember { mutableStateListOf<Annotation.FaceAnnotations>() }
    var showImperial by remember {
        mutableStateOf(context.getSharedPreferences("capture", Context.MODE_PRIVATE)
            .getBoolean("imperial", false))
    }
    var canvas by remember { mutableStateOf(IntSize(1, 1)) }

    // Viewport: user zoom on top of the aspect-correct base fit.
    var scale by remember { mutableStateOf(1f) }
    var off by remember { mutableStateOf(Offset.Zero) }

    var selected by remember { mutableStateOf<Sel?>(null) }
    var finger by remember { mutableStateOf<Offset?>(null) }   // loupe target
    var editingLine by remember { mutableStateOf<Int?>(null) }
    var noteFor by remember { mutableStateOf<Int?>(null) }
    var tagFor by remember { mutableStateOf<Int?>(null) }
    var openingMenu by remember { mutableStateOf(false) }

    // The laser. Its whole contract is "the value lands in the SELECTED
    // measure" — which is why the selection model had to come first.
    val disto = remember { DistoClient(context) }
    var distoState by remember { mutableStateOf(DistoClient.State.OFF) }
    var distoNote by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) { onDispose { disto.stop() } }
    // Android 12+ gates a BLE scan behind runtime permission; without it the
    // scan fails silently, which would look exactly like a flat meter.
    val blePerms = remember {
        if (android.os.Build.VERSION.SDK_INT >= 31)
            arrayOf(android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val askBle = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) disto.start()
        else distoNote = "Bluetooth permission refused — the laser can't connect"
    }

    // --- viewport maths -------------------------------------------------
    val imgW = (bitmap?.width ?: 1).toFloat()
    val imgH = (bitmap?.height ?: 1).toFloat()
    val fit = min(canvas.width / imgW, canvas.height / imgH)
    val fitW = imgW * fit
    val fitH = imgH * fit
    val baseX = (canvas.width - fitW) / 2f
    val baseY = (canvas.height - fitH) / 2f

    fun toScreen(nx: Double, ny: Double) = Offset(
        (baseX + nx.toFloat() * fitW) * scale + off.x,
        (baseY + ny.toFloat() * fitH) * scale + off.y)

    fun toNorm(s: Offset): Pair<Double, Double> {
        val fx = (s.x - off.x) / scale
        val fy = (s.y - off.y) / scale
        return Pair(((fx - baseX) / fitW).toDouble().coerceIn(0.0, 1.0),
            ((fy - baseY) / fitH).toDouble().coerceIn(0.0, 1.0))
    }

    /** Keep the photo from being flung off-screen: at 1x it stays centred,
     *  zoomed in it may pan only as far as its own edges. */
    fun clamp() {
        val sw = fitW * scale
        val sh = fitH * scale
        val bx = baseX * scale
        val by = baseY * scale
        off = Offset(
            if (sw <= canvas.width) -bx + (canvas.width - sw) / 2f
            else off.x.coerceIn(canvas.width - sw - bx, -bx),
            if (sh <= canvas.height) -by + (canvas.height - sh) / 2f
            else off.y.coerceIn(canvas.height - sh - by, -by))
    }

    fun persist(updated: Annotation.FaceAnnotations) {
        undo.add(ann)
        if (undo.size > 20) undo.removeAt(0)
        ann = updated
        store.save(deviceId, face, updated)
        SyncWorker.syncNow(context)
    }

    // A laser reading lands on the selected measure — no dialog, nothing to
    // confirm, because the person is holding a meter and standing on site.
    // With nothing selected it is deliberately ignored rather than guessed
    // at: a number on the wrong wall is worse than no number.
    disto.onState = { s, note -> distoState = s; distoNote = note }
    disto.onReading = { r ->
        val s = selected
        if (s is Sel.L) {
            persist(ann.copy(lines = ann.lines.toMutableList().also {
                if (s.i in it.indices) it[s.i] = it[s.i].copy(mm = r.mm)
            }))
            // Honour the meter's own display when it is imperial: Amit's
            // "using the laser meter unit while keying in measurements".
            if (r.deviceImperial && !showImperial) {
                showImperial = true
                context.getSharedPreferences("capture", Context.MODE_PRIVATE)
                    .edit().putBoolean("imperial", true).apply()
            }
            distoNote = "Measured ${Annotation.label(r.mm, showImperial)}"
        } else {
            distoNote = "Select a measure first"
        }
    }

    /** Endpoint snapping: a wall corner shared by two measurements should be
     *  ONE point, not two a few pixels apart — the model is built from these. */
    fun snap(nx: Double, ny: Double, skipLine: Int, skipEnd: Int): Pair<Double, Double> {
        val tol = (SNAP_PX / (fitW * scale)).toDouble()
        for ((i, l) in ann.lines.withIndex()) {
            for (e in 1..2) {
                if (i == skipLine && e == skipEnd) continue
                val px = if (e == 1) l.x1 else l.x2
                val py = if (e == 1) l.y1 else l.y2
                if (abs(px - nx) < tol && abs(py - ny) < tol * (fitW / fitH))
                    return Pair(px, py)
            }
        }
        return Pair(nx, ny)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Handover.FACE_LABELS[face] ?: face) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    TextButton(onClick = {
                        if (distoState == DistoClient.State.OFF) askBle.launch(blePerms)
                        else disto.measure()
                    }) {
                        Text(when (distoState) {
                            DistoClient.State.OFF -> "Laser"
                            DistoClient.State.SCANNING -> "Finding…"
                            DistoClient.State.CONNECTING -> "Linking…"
                            DistoClient.State.READY -> "Shoot"
                        })
                    }
                    if (undo.isNotEmpty()) {
                        TextButton(onClick = {
                            val prev = undo.removeAt(undo.size - 1)
                            ann = prev
                            store.save(deviceId, face, prev)
                            selected = null
                            SyncWorker.syncNow(context)
                        }) { Text("Undo") }
                    }
                    FilterChip(selected = showImperial, onClick = {
                        showImperial = !showImperial
                        context.getSharedPreferences("capture", Context.MODE_PRIVATE)
                            .edit().putBoolean("imperial", showImperial).apply()
                    }, label = { Text("m · ft") })
                })
        },
        bottomBar = {
            BottomAppBar {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        // Drop a measure across the middle of what's on screen,
                        // then the ends get dragged onto the wall.
                        val a = toNorm(Offset(canvas.width * 0.3f, canvas.height * 0.5f))
                        val b = toNorm(Offset(canvas.width * 0.7f, canvas.height * 0.5f))
                        persist(ann.copy(lines = ann.lines +
                            Annotation.Line(a.first, a.second, b.first, b.second, 0)))
                        selected = Sel.L(ann.lines.lastIndex)
                    }) { Text("+ Measure") }
                    Box {
                        TextButton(onClick = { openingMenu = true }) {
                            Text("+ Opening")
                        }
                        DropdownMenu(expanded = openingMenu,
                            onDismissRequest = { openingMenu = false }) {
                            Annotation.Kind.entries.forEach { k ->
                                DropdownMenuItem(
                                    text = { Text(k.label) },
                                    onClick = {
                                        openingMenu = false
                                        // Dropped over the middle of what's on
                                        // screen, sized in view terms, so it
                                        // lands grabbable however far you're
                                        // zoomed in.
                                        val c = toNorm(Offset(canvas.width / 2f,
                                            canvas.height / 2f))
                                        val e = toNorm(Offset(canvas.width * 0.68f,
                                            canvas.height * 0.72f))
                                        persist(ann.copy(quads = ann.quads +
                                            Annotation.newQuad(k, c.first, c.second,
                                                kotlin.math.abs(e.first - c.first),
                                                kotlin.math.abs(e.second - c.second))))
                                        selected = Sel.Q(ann.quads.lastIndex)
                                    })
                            }
                        }
                    }
                    TextButton(onClick = {
                        val c = toNorm(Offset(canvas.width / 2f, canvas.height / 2f))
                        persist(ann.copy(pins = ann.pins +
                            Annotation.Pin(c.first, c.second, "")))
                        val i = ann.pins.lastIndex
                        selected = Sel.P(i)
                        noteFor = i
                    }) { Text("+ Note") }
                    val s = selected
                    TextButton(
                        enabled = s != null,
                        onClick = {
                            when (s) {
                                is Sel.L -> editingLine = s.i
                                is Sel.P -> noteFor = s.i
                                is Sel.Q -> tagFor = s.i
                                null -> {}
                            }
                        }) { Text(if (selected is Sel.Q) "Tag" else "Value") }
                    TextButton(
                        enabled = s != null,
                        onClick = {
                            when (s) {
                                is Sel.L -> persist(ann.copy(
                                    lines = ann.lines.filterIndexed { i, _ -> i != s.i }))
                                is Sel.P -> persist(ann.copy(
                                    pins = ann.pins.filterIndexed { i, _ -> i != s.i }))
                                is Sel.Q -> persist(ann.copy(
                                    quads = ann.quads.filterIndexed { i, _ -> i != s.i }))
                                null -> {}
                            }
                            selected = null
                        }) { Text("Delete") }
                }
            }
        }) { pad ->
        if (bitmap == null) {
            Text("This face's image is no longer on the phone.",
                Modifier.padding(pad).padding(16.dp))
            return@Scaffold
        }
        Column(Modifier.padding(pad)) {
            Text(distoNote ?: when (selected) {
                    null -> "Pinch to zoom · tap something to select it"
                    is Sel.Q -> "Drag the 4 corners onto the opening · Tag says what it is"
                    else -> "Drag the ends onto the wall · Value, or shoot the laser"
                },
                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall)
            Canvas(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { canvas = it; clamp() }
                .pointerInput(ann, selected, canvas, scale) {
                    awaitEachGesture {
                        val first = awaitFirstDown(requireUnconsumed = false)
                        var transform = false
                        var moved = false

                        // What did the finger land on? Endpoints of the
                        // selected line win — that is the adjust step.
                        var grab: Grab? = null
                        run {
                            val sl = selected
                            if (sl is Sel.L) {
                                ann.lines.getOrNull(sl.i)?.let { l ->
                                    val d1 = (toScreen(l.x1, l.y1) - first.position)
                                        .getDistance()
                                    val d2 = (toScreen(l.x2, l.y2) - first.position)
                                        .getDistance()
                                    if (d1 <= HANDLE_PX && d1 <= d2) grab = Grab.End(sl.i, 1)
                                    else if (d2 <= HANDLE_PX) grab = Grab.End(sl.i, 2)
                                }
                            }
                            if (grab == null && sl is Sel.P) {
                                ann.pins.getOrNull(sl.i)?.let { p ->
                                    if ((toScreen(p.x, p.y) - first.position)
                                            .getDistance() <= HANDLE_PX)
                                        grab = Grab.Pin(sl.i)
                                }
                            }
                            if (grab == null && sl is Sel.Q) {
                                ann.quads.getOrNull(sl.i)?.let { q ->
                                    var bestD = HANDLE_PX
                                    for (c in 1..4) {
                                        val (cx, cy) = q.corner(c)
                                        val d = (toScreen(cx, cy) - first.position)
                                            .getDistance()
                                        if (d <= bestD) {
                                            bestD = d
                                            grab = Grab.Corner(sl.i, c)
                                        }
                                    }
                                }
                            }
                        }
                        if (grab != null) finger = first.position

                        do {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.size >= 2) {
                                // Two fingers always mean zoom/pan, and they
                                // cancel any handle drag that had started.
                                transform = true
                                grab = null
                                finger = null
                                val z = event.calculateZoom()
                                val pan = event.calculatePan()
                                if (z != 1f) {
                                    val c = event.calculateCentroid(useCurrent = true)
                                    val ns = (scale * z).coerceIn(MIN_SCALE, MAX_SCALE)
                                    val k = ns / scale
                                    off = Offset(c.x - (c.x - off.x) * k,
                                        c.y - (c.y - off.y) * k)
                                    scale = ns
                                }
                                off += pan
                                clamp()
                                event.changes.forEach { it.consume() }
                            } else if (pressed.size == 1 && !transform) {
                                val ch = pressed[0]
                                val d = ch.positionChange()
                                if (d.getDistance() > 6f) moved = true
                                val g = grab
                                if (g != null) {
                                    finger = ch.position
                                    val (nx, ny) = toNorm(ch.position)
                                    ann = when (g) {
                                        is Grab.End -> {
                                            val (sx, sy) = snap(nx, ny, g.i, g.end)
                                            val l = ann.lines[g.i]
                                            val nl = if (g.end == 1)
                                                l.copy(x1 = sx, y1 = sy)
                                            else l.copy(x2 = sx, y2 = sy)
                                            ann.copy(lines = ann.lines.toMutableList()
                                                .also { it[g.i] = nl })
                                        }
                                        is Grab.Pin -> ann.copy(
                                            pins = ann.pins.toMutableList().also {
                                                it[g.i] = it[g.i].copy(x = nx, y = ny)
                                            })
                                        is Grab.Corner -> ann.copy(
                                            quads = ann.quads.toMutableList().also {
                                                it[g.i] = it[g.i]
                                                    .withCorner(g.corner, nx, ny)
                                            })
                                    }
                                    ch.consume()
                                } else if (moved) {
                                    off += d
                                    clamp()
                                    ch.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        if (grab != null) {
                            store.save(deviceId, face, ann)
                            SyncWorker.syncNow(context)
                            finger = null
                        } else if (!moved && !transform) {
                            // A tap: select whatever is under it, else clear.
                            val p = first.position
                            var hit: Sel? = null
                            var best = HANDLE_PX * 1.4f
                            for ((i, l) in ann.lines.withIndex()) {
                                val a = toScreen(l.x1, l.y1)
                                val b = toScreen(l.x2, l.y2)
                                val dd = distanceToSegment(p, a, b)
                                if (dd < best) { best = dd; hit = Sel.L(i) }
                            }
                            for ((i, pin) in ann.pins.withIndex()) {
                                val dd = (toScreen(pin.x, pin.y) - p).getDistance()
                                if (dd < best) { best = dd; hit = Sel.P(i) }
                            }
                            for ((i, q) in ann.quads.withIndex()) {
                                for (c in 1..4) {
                                    val (ax, ay) = q.corner(c)
                                    val (bx, by) = q.corner(if (c == 4) 1 else c + 1)
                                    val dd = distanceToSegment(p,
                                        toScreen(ax, ay), toScreen(bx, by))
                                    if (dd < best) { best = dd; hit = Sel.Q(i) }
                                }
                            }
                            selected = hit
                        }
                    }
                }
            ) {
                val topLeft = toScreen(0.0, 0.0)
                drawImage(bitmap,
                    dstOffset = IntOffset(topLeft.x.toInt(), topLeft.y.toInt()),
                    dstSize = IntSize((fitW * scale).toInt(), (fitH * scale).toInt()))

                val sel = selected

                // Tagged openings first, so measurement lines stay on top of
                // them — the number is what a person came to read.
                for ((i, q) in ann.quads.withIndex()) {
                    val on = sel is Sel.Q && sel.i == i
                    val col = kindColor(q.kind)
                    val pts = (1..4).map { c ->
                        val (cx, cy) = q.corner(c); toScreen(cx, cy)
                    }
                    for (c in 0..3) {
                        drawLine(col, pts[c], pts[(c + 1) % 4],
                            strokeWidth = if (on) 7f else 5f)
                    }
                    if (on) pts.forEach {
                        drawCircle(col, HANDLE_PX / 2f, it, style = Stroke(5f))
                    }
                    val label = Annotation.Kind.of(q.kind).label +
                        (if (q.note.isNotBlank()) " · ${q.note.take(18)}" else "")
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            textSize = 38f
                            color = android.graphics.Color.WHITE
                            setShadowLayer(6f, 0f, 0f, android.graphics.Color.BLACK)
                            isFakeBoldText = true
                        }
                        drawText(label, pts[0].x + 8f, pts[0].y - 12f, paint)
                    }
                }

                for ((i, l) in ann.lines.withIndex()) {
                    val a = toScreen(l.x1, l.y1)
                    val b = toScreen(l.x2, l.y2)
                    val on = sel is Sel.L && sel.i == i
                    val col = if (on) Color(0xFF00E5FF) else Color(0xFFFF3D00)
                    drawLine(col, a, b, strokeWidth = if (on) 7f else 5f)
                    // End ticks perpendicular to the run, so a dimension reads
                    // like a drawing rather than a scratch.
                    val dir = b - a
                    val len = max(dir.getDistance(), 0.001f)
                    val n = Offset(-dir.y / len, dir.x / len) * 14f
                    drawLine(col, a - n, a + n, strokeWidth = if (on) 6f else 4f)
                    drawLine(col, b - n, b + n, strokeWidth = if (on) 6f else 4f)
                    if (on) {
                        drawCircle(col, HANDLE_PX / 2f, a, style = Stroke(5f))
                        drawCircle(col, HANDLE_PX / 2f, b, style = Stroke(5f))
                    }
                    val mid = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
                    val text = if (l.mm > 0) Annotation.label(l.mm, showImperial)
                               else "tap Value"
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            textSize = 42f
                            color = if (l.mm > 0) android.graphics.Color.WHITE
                                    else android.graphics.Color.YELLOW
                            setShadowLayer(6f, 0f, 0f, android.graphics.Color.BLACK)
                            isFakeBoldText = true
                        }
                        drawText(text, mid.x - paint.measureText(text) / 2,
                            mid.y - 16f, paint)
                    }
                }
                for ((i, p) in ann.pins.withIndex()) {
                    val at = toScreen(p.x, p.y)
                    val on = sel is Sel.P && sel.i == i
                    val col = if (on) Color(0xFF00E5FF) else Color(0xFF2962FF)
                    drawCircle(col, if (on) 22f else 16f, at)
                    if (on) drawCircle(col, HANDLE_PX / 2f, at, style = Stroke(4f))
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            textSize = 36f; color = android.graphics.Color.WHITE
                            setShadowLayer(6f, 0f, 0f, android.graphics.Color.BLACK)
                        }
                        drawText(p.text.take(24), at.x + 26f, at.y + 12f, paint)
                    }
                }

                // The loupe: a fixed inset in a top corner (flipping to the
                // other side when the finger is under it), showing the exact
                // pixel being placed. Fixed beats finger-following — it can
                // never be pushed off-screen and the hand never covers it.
                finger?.let { f ->
                    val side = 300f
                    val m = 16f
                    val left = if (f.x < size.width / 2f) size.width - side - m else m
                    val rect = Rect(left, m, left + side, m + side)
                    val lz = 3f
                    clipRect(rect.left, rect.top, rect.right, rect.bottom) {
                        val c = rect.center
                        drawImage(bitmap,
                            dstOffset = IntOffset(
                                (c.x - (f.x - topLeft.x) * lz).toInt(),
                                (c.y - (f.y - topLeft.y) * lz).toInt()),
                            dstSize = IntSize((fitW * scale * lz).toInt(),
                                (fitH * scale * lz).toInt()))
                        drawLine(Color.Red, Offset(c.x - 26f, c.y),
                            Offset(c.x + 26f, c.y), 3f)
                        drawLine(Color.Red, Offset(c.x, c.y - 26f),
                            Offset(c.x, c.y + 26f), 3f)
                    }
                    drawRect(Color.White, topLeft = rect.topLeft,
                        size = rect.size, style = Stroke(5f))
                }
            }
        }
    }

    editingLine?.let { idx ->
        DimensionDialog(
            initialMm = ann.lines.getOrNull(idx)?.mm ?: 0,
            showImperial = showImperial,
            onDismiss = { editingLine = null },
            onSave = { mm ->
                persist(ann.copy(lines = ann.lines.toMutableList()
                    .also { it[idx] = it[idx].copy(mm = mm) }))
                editingLine = null
            })
    }

    tagFor?.let { idx ->
        ann.quads.getOrNull(idx)?.let { q ->
            TagDialog(
                initialKind = Annotation.Kind.of(q.kind),
                initialNote = q.note,
                onDismiss = { tagFor = null },
                onSave = { kind, note ->
                    persist(ann.copy(quads = ann.quads.toMutableList().also {
                        it[idx] = it[idx].copy(kind = kind.tag, note = note)
                    }))
                    tagFor = null
                })
        }
    }

    noteFor?.let { idx ->
        NoteDialog(
            initial = ann.pins.getOrNull(idx)?.text ?: "",
            onDismiss = {
                // A pin dropped and abandoned without words is noise.
                if (ann.pins.getOrNull(idx)?.text.isNullOrBlank()) {
                    persist(ann.copy(pins = ann.pins.filterIndexed { i, _ -> i != idx }))
                    selected = null
                }
                noteFor = null
            },
            onSave = { text ->
                persist(ann.copy(pins = ann.pins.toMutableList()
                    .also { it[idx] = it[idx].copy(text = text) }))
                noteFor = null
            })
    }
}

/** Shortest distance from p to segment a-b, in screen pixels. */
private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val len2 = ab.x * ab.x + ab.y * ab.y
    if (len2 < 0.0001f) return (p - a).getDistance()
    val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / len2).coerceIn(0f, 1f)
    return (p - (a + ab * t)).getDistance()
}

@Composable
private fun DimensionDialog(
    initialMm: Int,
    showImperial: Boolean,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var value by remember {
        mutableStateOf(if (initialMm > 0) initialMm.toString() else "")
    }
    var unit by remember { mutableStateOf(Annotation.Unit.MM) }
    val preview = value.toDoubleOrNull()?.let { v ->
        if (v > 0) Annotation.label(Annotation.toMm(v, unit), showImperial) else null
    }
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
                if (preview != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(preview, style = MaterialTheme.typography.titleMedium)
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

/** What an opening IS. The tag is the machine-readable part the model
 *  reads; the note takes whatever doesn't fit the vocabulary. */
@Composable
private fun TagDialog(
    initialKind: Annotation.Kind,
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (Annotation.Kind, String) -> Unit,
) {
    var kind by remember { mutableStateOf(initialKind) }
    var note by remember { mutableStateOf(initialNote) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What is this?") },
        text = {
            Column {
                Annotation.Kind.entries.chunked(3).forEach { row ->
                    Row {
                        row.forEach { k ->
                            FilterChip(selected = kind == k, onClick = { kind = k },
                                label = { Text(k.label) },
                                modifier = Modifier.padding(end = 4.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it },
                    label = { Text("Note (optional)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(kind, note.trim()) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun NoteDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
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
                Card(onClick = { if (present) onFace(face) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(Handover.FACE_LABELS[face] ?: face)
                        Text(when {
                            !present -> "image not on this phone"
                            ann.isEmpty -> "not annotated"
                            else -> "${ann.lines.size} lines · " +
                                "${ann.quads.size} openings · ${ann.pins.size} notes"
                        }, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
