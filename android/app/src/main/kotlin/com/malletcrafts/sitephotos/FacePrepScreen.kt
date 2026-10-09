package com.malletcrafts.sitephotos

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.malletcrafts.sitephotos.pano.FacePrep
import com.malletcrafts.sitephotos.pano.Handover
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Face Prep — the browser prototype Amit approved on 2026-10-09 ("Build and
 * ship apk"), on the phone, on a capture's own six faces, with the DISTO D2
 * live. Line up the room box on each face, mark what sticks out (columns,
 * beams, steps), divide edges, place details and free measure lines, take
 * laser readings onto any of them, then save every face as an ImageMeter
 * photo next to the originals and share the measures as CSV.
 *
 * The geometry is all in [FacePrep] (pure, tested); drawing is [FacePrepDraw],
 * shared by the screen and the saved photos so they cannot disagree.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacePrepScreen(
    deviceId: String,
    roomName: String,
    fovDeg: Double,
    startDims: Triple<Int, Int, Int>,   // H, X (width), Z (length), mm
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { FacePrepStore(context) }
    val room = remember { store.load(deviceId) ?: FacePrep.Room(startDims.first, startDims.second, startDims.third) }
    var rev by remember { mutableIntStateOf(0) }          // bumped on every model change: redraws
    val undo = remember { mutableStateListOf<FacePrep.Room>() }
    fun touch() { rev++ }
    fun save() { runCatching { store.save(deviceId, room) }; rev++ }
    fun pushUndo() { undo.add(room.copy()); if (undo.size > 30) undo.removeAt(0) }
    fun restore(r: FacePrep.Room) {
        room.H = r.H; room.X = r.X; room.Z = r.Z; room.cx = r.cx; room.cz = r.cz; room.ch = r.ch
        room.faces.clear(); room.faces.putAll(r.faces); room.laser.clear(); room.laser.putAll(r.laser)
    }

    val prefs = remember { context.getSharedPreferences("faceprep", Context.MODE_PRIVATE) }
    var face by remember { mutableStateOf("floor") }
    var mode by remember { mutableStateOf("steps") }         // box, steps, edges, dets, lines -- the box comes from the split
    var stepKind by remember { mutableStateOf("column") }
    var detType by remember { mutableStateOf("door") }
    var sel by remember { mutableStateOf<FacePrepDraw.Sel?>(null) }
    var snapOn by remember { mutableStateOf(prefs.getBoolean("snap", false)) }
    var grid by remember { mutableStateOf(loadGrid(prefs)) }
    var note by remember { mutableStateOf<String?>(null) }
    var guide by remember { mutableStateOf<Pair<FacePrep.LineGuide, DoubleArray>?>(null) }
    var boxGuide by remember { mutableStateOf<Pair<String, Double>?>(null) }
    var showRoom by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf<String?>(null) }

    // ---- the photo of this face (the square part: the caption strip is below it)
    val src = remember(face) { faceSource(context, deviceId, face) }
    val uri = src?.uri
    var bitmap by remember(face) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) { bitmap = uri?.let { u -> withContext(Dispatchers.IO) { decodeFace(context, u, 2400)?.asImageBitmap() } } }
    val side = (bitmap?.width ?: 1).toFloat()

    // ---- the laser: a reading lands on the picked target, then the next unmeasured one is picked
    var pick by remember { mutableStateOf<String?>(null) }
    var typed by remember { mutableStateOf("") }
    val disto = remember { DistoClient(context) }
    var distoState by remember { mutableStateOf(DistoClient.State.OFF) }
    DisposableEffect(Unit) { onDispose { disto.stop() } }
    val blePerms = remember {
        if (android.os.Build.VERSION.SDK_INT >= 31) arrayOf(android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val askBle = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.all { it }) disto.start() else note = "Bluetooth permission refused — the laser can't connect"
    }
    fun targets(): List<FacePrep.Target> { rev; return FacePrep.laserTargets(room, face) }
    fun take(mm: Int) {
        val ts = FacePrep.laserTargets(room, face); val i = ts.indexOfFirst { it.key == pick }
        if (i < 0) { note = "Pick what to measure first"; return }
        pushUndo(); ts[i].set(mm); save()
        val ts2 = FacePrep.laserTargets(room, face)
        val next = (ts2.drop(i + 1) + ts2.take(i)).firstOrNull { it.got == null }
        note = "${ts[i].label}: $mm mm"
        if (next != null) pick = next.key
    }
    disto.onState = { s, n -> distoState = s; if (n != null) note = n }
    disto.onRefused = { why -> note = why }
    disto.onReading = { r -> take(r.mm) }

    // ---- viewport: fit, then zoom/pan
    var canvas by remember { mutableStateOf(IntSize(1, 1)) }
    var scale by remember { mutableStateOf(1f) }
    var off by remember { mutableStateOf(Offset.Zero) }
    fun fit(): Float { val b = bitmap ?: return 1f; return min(canvas.width / b.width.toFloat(), canvas.height / b.height.toFloat()) }
    fun base(): Offset { val b = bitmap ?: return Offset.Zero; val k = fit(); return Offset((canvas.width - b.width * k) / 2f, (canvas.height - b.height * k) / 2f) }
    fun imgToScreen(x: Float, y: Float): Offset { val k = fit(); val b0 = base(); return Offset((b0.x + x * k) * scale + off.x, (b0.y + y * k) * scale + off.y) }
    fun screenToImg(p: Offset): Offset { val k = fit(); val b0 = base(); return Offset(((p.x - off.x) / scale - b0.x) / k, ((p.y - off.y) / scale - b0.y) / k) }

    // The elevation the split made already has the room box in a known place, so it is used as is;
    // a hand-dragged box (Align box) wins, and only a face with no elevation falls to the camera guess.
    fun boxNow(): FacePrep.Box = room.face(face).box ?: src?.auto ?: (FacePrep.cameraGuess(room, face, fovDeg) ?: FacePrep.Box(0.2, 0.2, 0.8, 0.8))
    fun uvToScreen(u: Double, v: Double): Offset { val q = FacePrepDraw.uvToImg(boxNow(), side, u, v); return imgToScreen(q[0], q[1]) }
    fun screenToUv(p: Offset): DoubleArray {
        val b = boxNow(); val q = screenToImg(p)
        return doubleArrayOf((q.x / side - b.x0) / (b.x1 - b.x0), (q.y / side - b.y0) / (b.y1 - b.y0))
    }
    fun d(a: Offset, b: Offset) = hypot(a.x - b.x, a.y - b.y)
    val tol = 56f   // px on screen: a fingertip

    /** With Snap on, a point jumps to the nearest reference (box, steps, details, guides, dividers, grid) within a fingertip. */
    fun snapUv(uv: DoubleArray): DoubleArray {
        val out = doubleArrayOf(FacePrep.clamp01(uv[0]), FacePrep.clamp01(uv[1])); if (!snapOn) return out
        val (us, vs) = FacePrep.references(room, face, grid); val p = uvToScreen(out[0], out[1])
        var bu = tol * 0.6f; var bv = tol * 0.6f; var ru = out[0]; var rv = out[1]
        for (u in us) { val dd = abs(uvToScreen(u, out[1]).x - p.x); if (dd < bu) { bu = dd; ru = u } }
        for (v in vs) { val dd = abs(uvToScreen(out[0], v).y - p.y); if (dd < bv) { bv = dd; rv = v } }
        return doubleArrayOf(ru, rv)
    }

    fun lineHit(p: Offset, wide: Float): Pair<Int, Double>? {
        val f = room.face(face); val (w, h) = FacePrep.faceDims(room, face); val uv = screenToUv(p)
        f.lines.forEachIndexed { i, l -> val t = FacePrep.lineProj(w, h, l, uv)
            if (t > 0.02 && t < 0.98) { val q = l.at(t); if (d(uvToScreen(q[0], q[1]), p) < wide) return i to t } }
        return null
    }

    fun grabAt(p: Offset): FpGrab? {
        val f = room.face(face); val uv = screenToUv(p)
        when (mode) {
            "box" -> {
                val c = listOf(uvToScreen(0.0, 0.0), uvToScreen(1.0, 0.0), uvToScreen(1.0, 1.0), uvToScreen(0.0, 1.0))
                val k = c.indices.minByOrNull { d(c[it], p) }!!
                if (d(c[k], p) < tol * 1.3f) { sel = FacePrepDraw.Sel("corner", k); return GCorner(k) }
                val inV = uv[1] > -0.05 && uv[1] < 1.05; val inU = uv[0] > -0.05 && uv[0] < 1.05
                val cand = listOf("x0" to abs(p.x - c[0].x), "x1" to abs(p.x - c[1].x), "y0" to abs(p.y - c[0].y), "y1" to abs(p.y - c[3].y))
                    .filter { (k2, dd) -> dd < tol && (if (k2[0] == 'x') inV else inU) }.minByOrNull { it.second }
                if (cand != null) { sel = FacePrepDraw.Sel("bline", key = cand.first); return GBLine(cand.first) }
                sel = null; return null
            }
            "steps", "dets" -> {
                val list = mode
                fun rects(): List<DoubleArray> = if (list == "steps") f.steps.map { val n = it.norm(); doubleArrayOf(n.u0, n.v0, n.u1, n.v1) }
                    else f.dets.map { doubleArrayOf(it.u0, it.v0, it.u1, it.v1) }
                val rs = rects()
                val order = rs.indices.sortedBy { if (sel?.kind == list && sel?.i == it) 0 else 1 }
                for (i in order) { val r = rs[i]
                    for ((ku, kv) in listOf("u0" to "v0", "u1" to "v0", "u1" to "v1", "u0" to "v1")) {
                        val q = uvToScreen(if (ku == "u0") r[0] else r[2], if (kv == "v0") r[1] else r[3])
                        if (d(q, p) < tol) { sel = FacePrepDraw.Sel(list, i); return GRect(list, i, ku, kv) }
                    }
                }
                for (i in order) { val r = rs[i]
                    val a = uvToScreen(r[0], r[1]); val b = uvToScreen(r[2], r[3])
                    val inX = p.x > min(a.x, b.x) - tol / 2 && p.x < max(a.x, b.x) + tol / 2
                    val inY = p.y > min(a.y, b.y) - tol / 2 && p.y < max(a.y, b.y) + tol / 2
                    val hit = listOfNotNull(
                        ("u0" to abs(p.x - a.x)).takeIf { inY }, ("u1" to abs(p.x - b.x)).takeIf { inY },
                        ("v0" to abs(p.y - a.y)).takeIf { inX }, ("v1" to abs(p.y - b.y)).takeIf { inX },
                    ).filter { it.second < tol * 0.6f }.minByOrNull { it.second }
                    if (hit != null) { sel = FacePrepDraw.Sel(list, i)
                        return if (hit.first[0] == 'u') GRect(list, i, hit.first, null) else GRect(list, i, null, hit.first) }
                }
                for (i in order) { val r = rs[i]
                    if (uv[0] > r[0] && uv[0] < r[2] && uv[1] > r[1] && uv[1] < r[3]) { sel = FacePrepDraw.Sel(list, i)
                        return GMove(list, i, uv, r[0], r[1], r[2], r[3]) }
                }
                pushUndo(); val a = snapUv(uv)
                if (list == "steps") f.steps.add(FacePrep.Step(stepKind, a[0], a[1], a[0], a[1]))
                else f.dets.add(FacePrep.Detail(detType, a[0], a[1], a[0], a[1]))
                val i = if (list == "steps") f.steps.size - 1 else f.dets.size - 1
                sel = FacePrepDraw.Sel(list, i)
                return GRect(list, i, "u1", "v1", fresh = true)
            }
            "edges" -> {
                // a divider on a line, then on a detail's arrow, then on the box edge
                f.lines.forEachIndexed { i, l -> l.ts.forEachIndexed { j, t -> val q = l.at(t)
                    if (d(uvToScreen(q[0], q[1]), p) < tol * 0.8f) { sel = FacePrepDraw.Sel("lsplit", i, j); return GLSplit(i, j) } } }
                f.dets.forEachIndexed { i, dd ->
                    val ue = if (dd.sx == 1) dd.u1 else dd.u0; val ve = if (dd.sy == 1) dd.v1 else dd.v0
                    dd.sw.forEachIndexed { j, t -> if (d(uvToScreen(dd.u0 + (dd.u1 - dd.u0) * t, ve), p) < tol * 0.8f) { sel = FacePrepDraw.Sel("dsplit", i, j, "w"); return GDSplit(i, "w", j) } }
                    dd.sh.forEachIndexed { j, t -> if (d(uvToScreen(ue, dd.v0 + (dd.v1 - dd.v0) * t), p) < tol * 0.8f) { sel = FacePrepDraw.Sel("dsplit", i, j, "h"); return GDSplit(i, "h", j) } }
                }
                for ((sd, ts) in f.splits) ts.forEachIndexed { i, t ->
                    val q = when (sd) { "top" -> uvToScreen(t, 0.0); "bottom" -> uvToScreen(t, 1.0); "left" -> uvToScreen(0.0, t); else -> uvToScreen(1.0, t) }
                    if (d(q, p) < tol * 0.8f) { sel = FacePrepDraw.Sel("split", i, key = sd); return GSplit(sd, i) }
                }
                return null   // a tap decides what gets a new divider (on release)
            }
            "lines" -> {
                f.lines.forEachIndexed { i, l -> for (e in 0..1) { val q = if (e == 0) l.a else l.b
                    if (d(uvToScreen(q[0], q[1]), p) < tol) { sel = FacePrepDraw.Sel("line", i); return GLEnd(i, e) } } }
                f.lines.forEachIndexed { i, l -> l.ts.forEachIndexed { j, t -> val q = l.at(t)
                    if (d(uvToScreen(q[0], q[1]), p) < tol * 0.8f) { sel = FacePrepDraw.Sel("lsplit", i, j); return GLSplit(i, j) } } }
                lineHit(p, tol)?.let { (i, t) -> sel = FacePrepDraw.Sel("line", i)
                    return GLBody(i, t, uv, f.lines[i].a.copyOf(), f.lines[i].b.copyOf()) }
                pushUndo(); val a = snapUv(uv)
                f.lines.add(FacePrep.MLine(a.copyOf(), a.copyOf())); sel = FacePrepDraw.Sel("line", f.lines.size - 1)
                return GLEnd(f.lines.size - 1, 1, fresh = true)
            }
        }
        return null
    }

    fun dragTo(g: FpGrab, p: Offset) {
        val f = room.face(face); val raw = screenToUv(p); val (w, h) = FacePrep.faceDims(room, face)
        when (g) {
            is GCorner, is GBLine -> {
                val b = f.box ?: boxNow().copy().also { f.box = it }
                val q = screenToImg(p); val x = (q.x / side).toDouble(); val y = (q.y / side).toDouble()
                val keys = if (g is GCorner) listOf(listOf("x0", "y0"), listOf("x1", "y0"), listOf("x1", "y1"), listOf("x0", "y1"))[g.k] else listOf((g as GBLine).key)
                for (k in keys) when (k) {
                    "x0" -> b.x0 = min(x, b.x1 - 0.02); "x1" -> b.x1 = max(x, b.x0 + 0.02)
                    "y0" -> b.y0 = min(y, b.y1 - 0.02); "y1" -> b.y1 = max(y, b.y0 + 0.02)
                }
            }
            is GRect -> {
                val q = snapUv(raw)
                if (g.list == "steps") {
                    val s = f.steps[g.i]
                    when (g.ku) { "u0" -> s.u0 = q[0]; "u1" -> s.u1 = q[0] }
                    when (g.kv) { "v0" -> s.v0 = q[1]; "v1" -> s.v1 = q[1] }
                } else {
                    val dd = f.dets[g.i]
                    when (g.ku) { "u0" -> dd.u0 = q[0]; "u1" -> dd.u1 = q[0] }
                    when (g.kv) { "v0" -> dd.v0 = q[1]; "v1" -> dd.v1 = q[1] }
                }
            }
            is GMove -> {
                val wd = g.u1 - g.u0; val ht = g.v1 - g.v0
                val u0 = (g.u0 + raw[0] - g.start[0]).coerceIn(0.0, 1 - wd); val v0 = (g.v0 + raw[1] - g.start[1]).coerceIn(0.0, 1 - ht)
                val sn = snapUv(doubleArrayOf(u0, v0)); val su = if (sn[0] != u0) sn[0] else u0; val sv = if (sn[1] != v0) sn[1] else v0
                if (g.list == "steps") { val s = f.steps[g.i]; s.u0 = su; s.v0 = sv; s.u1 = su + wd; s.v1 = sv + ht }
                else { val dd = f.dets[g.i]; dd.u0 = su; dd.v0 = sv; dd.u1 = su + wd; dd.v1 = sv + ht }
            }
            is GSplit -> {
                val t = (if (g.sideName == "top" || g.sideName == "bottom") raw[0] else raw[1]).coerceIn(0.005, 0.995)
                f.splits.getValue(g.sideName)[g.i] = t; boxGuide = g.sideName to t
            }
            is GDSplit -> { val dd = f.dets[g.i]
                val t = (if (g.axis == "w") (raw[0] - dd.u0) / (dd.u1 - dd.u0) else (raw[1] - dd.v0) / (dd.v1 - dd.v0)).coerceIn(0.02, 0.98)
                (if (g.axis == "w") dd.sw else dd.sh)[g.j] = t }
            is GLEnd -> {
                val l = f.lines[g.i]; val other = if (g.end == 0) l.b else l.a
                val q = FacePrep.lineSnapAxis(w, h, other, snapUv(raw))
                when (g.end) { 0 -> l.a = q; else -> l.b = q }
            }
            is GLSplit -> { val l = f.lines[g.i]
                var t = FacePrep.lineProj(w, h, l, raw).coerceIn(0.02, 0.98)
                if (snapOn) { val (us, vs) = FacePrep.references(room, face, grid); val q = l.at(t); val here = uvToScreen(q[0], q[1]); var bd = tol * 0.6f
                    val du = l.b[0] - l.a[0]; val dv = l.b[1] - l.a[1]
                    for (u in us) if (abs(du) > 1e-6) { val tt = (u - l.a[0]) / du; if (tt in 0.02..0.98) { val qq = l.at(tt); val dd = d(uvToScreen(qq[0], qq[1]), here); if (dd < bd) { bd = dd; t = tt } } }
                    for (v in vs) if (abs(dv) > 1e-6) { val tt = (v - l.a[1]) / dv; if (tt in 0.02..0.98) { val qq = l.at(tt); val dd = d(uvToScreen(qq[0], qq[1]), here); if (dd < bd) { bd = dd; t = tt } } } }
                l.ts[g.j] = t; guide = FacePrep.lineGuide(room, face, grid, l, t) to l.at(t) }
            is GLBody -> { val l = f.lines[g.i]
                var du = raw[0] - g.start[0]; var dv = raw[1] - g.start[1]
                du = du.coerceIn(-min(g.a[0], g.b[0]), 1 - max(g.a[0], g.b[0])); dv = dv.coerceIn(-min(g.a[1], g.b[1]), 1 - max(g.a[1], g.b[1]))
                l.a = doubleArrayOf(g.a[0] + du, g.a[1] + dv); l.b = doubleArrayOf(g.b[0] + du, g.b[1] + dv) }
        }
        touch()
    }

    fun release(g: FpGrab?, moved: Boolean, at: Offset) {
        val f = room.face(face); val (w, h) = FacePrep.faceDims(room, face)
        guide = null; boxGuide = null
        if (g is GLBody && !moved) { pushUndo(); f.lines[g.i].ts.add(g.t); sel = FacePrepDraw.Sel("lsplit", g.i, f.lines[g.i].ts.size - 1) }
        if (g is GLEnd) { val l = f.lines[g.i]
            if (FacePrep.lineMm(w, h, l.a, l.b) < 30) { f.lines.removeAt(g.i); if (g.fresh) undo.removeLastOrNull(); sel = null } }
        if (g is GRect) {
            if (g.list == "steps") { val s = f.steps[g.i]; val n = s.norm()
                if ((n.u1 - n.u0) * w < 20 || (n.v1 - n.v0) * h < 20) { f.steps.removeAt(g.i); if (g.fresh) undo.removeLastOrNull(); sel = null }
                else { s.u0 = n.u0; s.v0 = n.v0; s.u1 = n.u1; s.v1 = n.v1 }
            } else { val dd = f.dets[g.i]
                // the corner the drag started from decides which two edges carry the size arrows
                if (g.fresh) { dd.sx = if (dd.u1 < dd.u0) 1 else 0; dd.sy = if (dd.v1 < dd.v0) 1 else 0 }
                val u0 = min(dd.u0, dd.u1); val u1 = max(dd.u0, dd.u1); val v0 = min(dd.v0, dd.v1); val v1 = max(dd.v0, dd.v1)
                if ((u1 - u0) * w < 20 || (v1 - v0) * h < 20) { f.dets.removeAt(g.i); if (g.fresh) undo.removeLastOrNull(); sel = null }
                else { dd.u0 = u0; dd.u1 = u1; dd.v0 = v0; dd.v1 = v1 }
            }
        }
        // Dividers: a tap that grabbed nothing divides whatever it lands on — a line first, then a detail's arrow, then the box edge
        if (g == null && !moved && mode == "edges") {
            val uv = screenToUv(at); val lh = lineHit(at, tol * 1.2f)
            if (lh != null) { pushUndo(); f.lines[lh.first].ts.add(lh.second); sel = FacePrepDraw.Sel("lsplit", lh.first, f.lines[lh.first].ts.size - 1) }
            else {
                var done = false
                f.dets.forEachIndexed { i, dd -> if (done) return@forEachIndexed
                    val ue = if (dd.sx == 1) dd.u1 else dd.u0; val ve = if (dd.sy == 1) dd.v1 else dd.v0
                    if (abs(uvToScreen(uv[0], ve).y - at.y) < tol * 0.6f && uv[0] > dd.u0 && uv[0] < dd.u1) { pushUndo(); dd.sw.add((uv[0] - dd.u0) / (dd.u1 - dd.u0)); done = true }
                    else if (abs(uvToScreen(ue, uv[1]).x - at.x) < tol * 0.6f && uv[1] > dd.v0 && uv[1] < dd.v1) { pushUndo(); dd.sh.add((uv[1] - dd.v0) / (dd.v1 - dd.v0)); done = true }
                }
                if (!done) {
                    val c = listOf("top" to abs(at.y - uvToScreen(uv[0], 0.0).y), "bottom" to abs(at.y - uvToScreen(uv[0], 1.0).y),
                        "left" to abs(at.x - uvToScreen(0.0, uv[1]).x), "right" to abs(at.x - uvToScreen(1.0, uv[1]).x))
                        .filter { it.second < tol * 0.8f }.minByOrNull { it.second }
                    if (c != null) { pushUndo(); val t = (if (c.first == "top" || c.first == "bottom") uv[0] else uv[1]).coerceIn(0.005, 0.995)
                        f.splits.getValue(c.first).add(t); sel = FacePrepDraw.Sel("split", f.splits.getValue(c.first).size - 1, key = c.first) }
                    else sel = null
                }
            }
        }
        // a tap on a detail points the laser at its first unmeasured dimension
        if (mode == "dets" && sel?.kind == "dets") {
            val mine = FacePrep.laserTargets(room, face).filter { it.key.startsWith("d${sel?.i}:") }
            (mine.firstOrNull { it.got == null } ?: mine.firstOrNull())?.let { pick = it.key }
        }
        if (mode == "lines" && sel?.kind == "line") pick = "l${sel?.i}"
        save()
    }

    fun deleteSel() {
        val s = sel ?: return; val f = room.face(face); pushUndo()
        when (s.kind) {
            "steps" -> if (s.i in f.steps.indices) f.steps.removeAt(s.i)
            "dets" -> if (s.i in f.dets.indices) f.dets.removeAt(s.i)
            "line" -> if (s.i in f.lines.indices) f.lines.removeAt(s.i)
            "lsplit" -> { val ts = f.lines.getOrNull(s.i)?.ts; if (ts != null && s.j in ts.indices) ts.removeAt(s.j) }
            "split" -> { val ts = f.splits[s.key]; if (ts != null && s.i in ts.indices) ts.removeAt(s.i) }
            "dsplit" -> { val dd = f.dets.getOrNull(s.i); val arr = if (s.key == "w") dd?.sw else dd?.sh; if (arr != null && s.j in arr.indices) arr.removeAt(s.j) }
        }
        sel = null; save()
    }

    // ---------------------------------------------------------------- UI
    Scaffold(topBar = {
        TopAppBar(
            title = { Column { Text("Face Prep · ${FacePrep.LABEL[face]}"); Text(roomName, style = MaterialTheme.typography.bodySmall) } },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            actions = {
                TextButton(onClick = { showRoom = true }) { Text("Room") }
                TextButton(enabled = busy == null, onClick = {
                    busy = "Saving photos…"
                    scope.launch {
                        val n = withContext(Dispatchers.IO) { exportAll(context, deviceId, roomName, room, fovDeg) }
                        busy = null
                        note = if (n > 0) "Saved $n ImageMeter photos next to the faces (…-faceprep.jpg)" else "No face photos found to save"
                    }
                }) { Text("Save photos") }
                TextButton(onClick = { shareCsv(context, roomName, deviceId, room) }) { Text("CSV") }
            })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // faces, in the order Amit asked for
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (fName in FacePrep.FACE_ORDER) {
                    val f = room.face(fName); rev
                    val k = f.steps.size + f.dets.size + f.lines.size + f.splits.values.sumOf { it.size }
                    FilterChip(selected = face == fName, onClick = { face = fName; sel = null; scale = 1f; off = Offset.Zero; pick = null },
                        label = { Text(FacePrep.LABEL.getValue(fName) + (if (k > 0) " · $k" else "") + (if (f.box != null) " ✓" else "")) })
                }
            }
            // tools
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((m, lab) in listOf("box" to "1 Align box", "steps" to "2 Columns & beams", "edges" to "3 Dividers", "dets" to "4 Details", "lines" to "5 Line"))
                    FilterChip(selected = mode == m, onClick = { mode = m; sel = null }, label = { Text(lab) })
            }
            if (mode == "steps" || mode == "dets") Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (mode == "steps") for ((k, lab) in FacePrep.STEP_KINDS) FilterChip(selected = stepKind == k, onClick = { stepKind = k }, label = { Text(lab) })
                else for ((k, lab) in FacePrep.DETAIL_TYPES) FilterChip(selected = detType == k, onClick = { detType = k }, label = { Text(lab) })
            }
            Text(busy ?: note ?: (if (mode == "box" && src?.auto != null && room.face(face).box == null)
                    "The box is already on the room's corners, from the split. Drag a line only if one is off." else HINT.getValue(mode)), Modifier.padding(horizontal = 12.dp, vertical = 2.dp), style = MaterialTheme.typography.bodySmall)

            Box(Modifier.fillMaxWidth().weight(1f)) {
                Canvas(Modifier.fillMaxSize()
                    .onSizeChanged { canvas = it }
                    .pointerInput(bitmap, face, mode) {
                        awaitEachGesture {
                            val first = awaitFirstDown(requireUnconsumed = false)
                            var g: FpGrab? = null; var moved = false; var transform = false; var undone = false
                            if (bitmap != null) {
                                g = grabAt(first.position)
                                // a new rectangle or line already saved an undo step when it was created
                                undone = (g is GLEnd && g.fresh) || (g is GRect && g.fresh)
                            }
                            do {
                                val ev = awaitPointerEvent()
                                val down = ev.changes.filter { it.pressed }
                                if (down.size >= 2) {
                                    transform = true
                                    val z = ev.calculateZoom(); val pan = ev.calculatePan()
                                    if (z != 1f) { val c = ev.calculateCentroid(useCurrent = true); val ns = (scale * z).coerceIn(1f, 10f); val k = ns / scale
                                        off = Offset(c.x - (c.x - off.x) * k, c.y - (c.y - off.y) * k); scale = ns }
                                    off += pan; ev.changes.forEach { it.consume() }
                                } else if (down.size == 1 && !transform) {
                                    val ch = down[0]
                                    // a finger wobbles: up to 18 px still counts as a tap (which divides a line), not a drag
                                    if (!moved && d(ch.position, first.position) > 18f) moved = true
                                    if (moved) {
                                        val gg = g
                                        if (gg != null) { if (!undone) { pushUndo(); undone = true }; dragTo(gg, ch.position) }
                                        else off += (ch.position - ch.previousPosition)
                                        ch.consume()
                                    }
                                }
                            } while (ev.changes.any { it.pressed })
                            if (bitmap != null && !transform) release(g, moved, first.position)
                        }
                    }) {
                    rev   // read so every model change redraws
                    val b = bitmap ?: return@Canvas
                    val k = fit(); val b0 = base()
                    drawIntoCanvas { cv ->
                        val nc = cv.nativeCanvas
                        nc.save(); nc.translate(off.x, off.y); nc.scale(scale, scale)
                        nc.drawBitmap(b.asAndroidBitmap(), null,
                            android.graphics.RectF(b0.x, b0.y, b0.x + b.width * k, b0.y + b.height * k), Paint(Paint.FILTER_BITMAP_FLAG))
                        nc.restore()
                        FacePrepDraw.draw(nc, room, face, boxNow(), side,
                            { x, y -> val q = imgToScreen(x, y); floatArrayOf(q.x, q.y) }, 1.2f,
                            FacePrepDraw.Opts(handles = when (mode) { "box" -> "box"; "steps" -> "steps"; "dets" -> "dets"; "lines" -> "lines"; else -> null },
                                sel = sel, grid = grid.takeIf { it.on }, guide = guide?.first, guideAt = guide?.second, boxGuide = boxGuide))
                    }
                }
                if (bitmap == null) Text(if (uri == null) "No ${FacePrep.LABEL[face]} photo for this capture on this phone" else "Loading…",
                    Modifier.align(Alignment.Center).padding(24.dp))
            }

            // toolbar
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(enabled = undo.isNotEmpty(), onClick = { undo.removeLastOrNull()?.let { restore(it) }; sel = null; save() }) { Text("Undo") }
                OutlinedButton(enabled = sel != null && sel?.kind != "corner" && sel?.kind != "bline", onClick = { deleteSel() }) { Text("Remove") }
                if (mode == "lines") OutlinedButton(enabled = sel?.kind == "line" || sel?.kind == "lsplit", onClick = {
                    val l = room.face(face).lines.getOrNull(sel?.i ?: -1) ?: return@OutlinedButton
                    val ts = listOf(0.0) + l.ts.sorted() + listOf(1.0); var kk = 0
                    for (j in 1 until ts.size - 1) if (ts[j + 1] - ts[j] > ts[kk + 1] - ts[kk]) kk = j
                    pushUndo(); l.ts.add((ts[kk] + ts[kk + 1]) / 2); save()
                }) { Text("Divide line") }
                if (mode == "box") OutlinedButton(onClick = { pushUndo(); room.face(face).box = null; save() }) { Text("Reset box") }
                FilterChip(selected = snapOn, onClick = { snapOn = !snapOn; prefs.edit().putBoolean("snap", snapOn).apply() }, label = { Text(if (snapOn) "Snap on" else "Snap off") })
                FilterChip(selected = grid.on, onClick = { grid = grid.copy(on = !grid.on); saveGrid(prefs, grid) }, label = { Text(if (grid.on) "Grid ${grid.x}×${grid.y}" else "Grid off") })
                TextButton(onClick = { showGrid = true }) { Text("Grid…") }
                TextButton(onClick = { scale = 1f; off = Offset.Zero }) { Text("Fit") }
            }
            // the laser
            val ts = targets()
            if (pick == null || ts.none { it.key == pick }) pick = (ts.firstOrNull { it.got == null } ?: ts.firstOrNull())?.key
            val cur = ts.firstOrNull { it.key == pick }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { val i = ts.indexOfFirst { it.key == pick }; if (ts.isNotEmpty()) pick = ts[(i - 1 + ts.size) % ts.size].key }) { Text("‹") }
                Column(Modifier.weight(1f)) {
                    Text(cur?.label ?: "—", style = MaterialTheme.typography.bodyMedium)
                    Text(cur?.let { (if (it.got == null) "photo ${it.now} mm" else if (it.agrees) "✓ laser ${it.got} mm" else "laser ${it.got} ≠ now ${it.now}") +
                        " · ${ts.count { t -> t.got != null }}/${ts.size} measured" } ?: "", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = { val i = ts.indexOfFirst { it.key == pick }; if (ts.isNotEmpty()) pick = ts[(i + 1) % ts.size].key }) { Text("›") }
                Button(onClick = { if (distoState == DistoClient.State.OFF) askBle.launch(blePerms) else if (distoState == DistoClient.State.READY) disto.measure() }) {
                    Text(when (distoState) { DistoClient.State.OFF -> "Laser"; DistoClient.State.SCANNING -> "Finding…"; DistoClient.State.CONNECTING -> "Linking…"; DistoClient.State.READY -> "Fire" })
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = typed, onValueChange = { typed = it.filter { c -> c.isDigit() }.take(6) }, singleLine = true,
                    label = { Text("Reading mm") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                Button(enabled = typed.isNotEmpty(), onClick = { typed.toIntOrNull()?.takeIf { it > 0 }?.let { take(it) }; typed = "" }) { Text("Set") }
                TextButton(enabled = cur?.got != null, onClick = { cur?.let { pushUndo(); it.forget(); save() } }) { Text("Forget") }
            }
        }
    }

    if (showRoom) {
        var hS by remember { mutableStateOf(room.H.toString()) }; var xS by remember { mutableStateOf(room.X.toString()) }
        var zS by remember { mutableStateOf(room.Z.toString()) }; var chS by remember { mutableStateOf(room.ch.toString()) }
        var cxS by remember { mutableStateOf(room.cx.toString()) }; var czS by remember { mutableStateOf(room.cz.toString()) }
        AlertDialog(onDismissRequest = { showRoom = false }, title = { Text("Room box, mm") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for ((lab, v, set) in listOf(Triple("Height", hS) { s: String -> hS = s }, Triple("Width ↔ (left to right)", xS) { s: String -> xS = s },
                    Triple("Length ↕ (front to back)", zS) { s: String -> zS = s }, Triple("Lens height", chS) { s: String -> chS = s },
                    Triple("Camera left ↔ right of centre", cxS) { s: String -> cxS = s }, Triple("Camera back ↕ front of centre", czS) { s: String -> czS = s }))
                    OutlinedTextField(v, { set(it.filter { c -> c.isDigit() || c == '-' }.take(6)) }, label = { Text(lab) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Text("Or measure them with the laser: Room height / width / length are the first three in the laser list.", style = MaterialTheme.typography.bodySmall)
            } },
            confirmButton = { TextButton(onClick = {
                pushUndo()
                hS.toIntOrNull()?.takeIf { it > 1000 }?.let { room.H = it }; xS.toIntOrNull()?.takeIf { it > 300 }?.let { room.X = it }
                zS.toIntOrNull()?.takeIf { it > 300 }?.let { room.Z = it }; chS.toIntOrNull()?.takeIf { it in 300 until room.H }?.let { room.ch = it }
                cxS.toIntOrNull()?.let { room.cx = it }; czS.toIntOrNull()?.let { room.cz = it }
                showRoom = false; save()
            }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showRoom = false }) { Text("Cancel") } })
    }
    if (showGrid) {
        var gx by remember { mutableStateOf(grid.x.toString()) }; var gy by remember { mutableStateOf(grid.y.toString()) }
        var gm by remember { mutableStateOf(grid.major.toString()) }; var gsx by remember { mutableStateOf(grid.sx.toString()) }
        var gsy by remember { mutableStateOf(grid.sy.toString()) }; var left by remember { mutableStateOf(grid.fromLeft) }
        var bottom by remember { mutableStateOf(grid.fromBottom) }; var labels by remember { mutableStateOf(grid.labels) }
        var alpha by remember { mutableStateOf(grid.alpha.toFloat()) }
        AlertDialog(onDismissRequest = { showGrid = false }, title = { Text("Reference grid") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(gx, { gx = it.filter(Char::isDigit).take(5) }, label = { Text("Across mm") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(gy, { gy = it.filter(Char::isDigit).take(5) }, label = { Text("Up mm") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                OutlinedTextField(gm, { gm = it.filter(Char::isDigit).take(5) }, label = { Text("Bold every mm (0 = none)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Count across from", Modifier.weight(1f))
                    FilterChip(selected = left, onClick = { left = true }, label = { Text("Left") }); Spacer(Modifier.width(4.dp))
                    FilterChip(selected = !left, onClick = { left = false }, label = { Text("Right") }) }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Count up from", Modifier.weight(1f))
                    FilterChip(selected = bottom, onClick = { bottom = true }, label = { Text("Floor") }); Spacer(Modifier.width(4.dp))
                    FilterChip(selected = !bottom, onClick = { bottom = false }, label = { Text("Ceiling") }) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(gsx, { gsx = it.filter { c -> c.isDigit() || c == '-' }.take(6) }, label = { Text("Shift across") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(gsy, { gsy = it.filter { c -> c.isDigit() || c == '-' }.take(6) }, label = { Text("Shift up") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Text("Strength", style = MaterialTheme.typography.bodySmall)
                Slider(value = alpha, onValueChange = { alpha = it }, valueRange = 10f..90f)
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(labels, { labels = it }); Text("mm labels") }
            } },
            confirmButton = { TextButton(onClick = {
                grid = FacePrep.Grid(on = true, x = (gx.toIntOrNull() ?: 500).coerceIn(10, 5000), y = (gy.toIntOrNull() ?: 500).coerceIn(10, 5000),
                    major = (gm.toIntOrNull() ?: 0).coerceIn(0, 10000), fromLeft = left, fromBottom = bottom,
                    sx = gsx.toIntOrNull() ?: 0, sy = gsy.toIntOrNull() ?: 0, alpha = alpha.toInt(), labels = labels)
                saveGrid(prefs, grid); showGrid = false
            }) { Text("OK") } },
            dismissButton = { Row {
                TextButton(onClick = { grid = FacePrep.Grid(on = true); saveGrid(prefs, grid); showGrid = false }) { Text("Reset") }
                TextButton(onClick = { showGrid = false }) { Text("Cancel") } } })
    }
}

// What a finger can hold on the photo
private sealed class FpGrab
private class GCorner(val k: Int) : FpGrab()
private class GBLine(val key: String) : FpGrab()
private class GRect(val list: String, val i: Int, val ku: String?, val kv: String?, val fresh: Boolean = false) : FpGrab()
private class GMove(val list: String, val i: Int, val start: DoubleArray, val u0: Double, val v0: Double, val u1: Double, val v1: Double) : FpGrab()
private class GSplit(val sideName: String, val i: Int) : FpGrab()
private class GDSplit(val i: Int, val axis: String, val j: Int) : FpGrab()
private class GLEnd(val i: Int, val end: Int, val fresh: Boolean = false) : FpGrab()
private class GLSplit(val i: Int, val j: Int) : FpGrab()
private class GLBody(val i: Int, val t: Double, val start: DoubleArray, val a: DoubleArray, val b: DoubleArray) : FpGrab()

private val HINT = mapOf(
    "box" to "Align box: drag a corner grip or a box line onto the room's corner lines. Two fingers zoom and move the photo.",
    "steps" to "Columns & beams: pick what sticks out, then drag a rectangle over the part of the face it takes away. Drag a side, a corner or the middle to adjust.",
    "edges" to "Dividers: tap a line, a door/window arrow or a box edge to divide it; drag a divider to slide it — the guide shows the mm either side.",
    "dets" to "Details: pick a type and drag a rectangle over it, starting at the corner its sizes are measured from. Tap one to point the laser at it.",
    "lines" to "Line: drag a straight line in any direction (snaps level or upright within 3°). Tap it to divide, drag an end to stretch, drag it to move.",
)

private fun loadGrid(p: android.content.SharedPreferences) = FacePrep.Grid(
    on = p.getBoolean("g_on", false), x = p.getInt("g_x", 500), y = p.getInt("g_y", 500), major = p.getInt("g_major", 1000),
    fromLeft = p.getBoolean("g_left", true), fromBottom = p.getBoolean("g_bottom", true), sx = p.getInt("g_sx", 0), sy = p.getInt("g_sy", 0),
    alpha = p.getInt("g_alpha", 35), labels = p.getBoolean("g_labels", true))

private fun saveGrid(p: android.content.SharedPreferences, g: FacePrep.Grid) {
    p.edit().putBoolean("g_on", g.on).putInt("g_x", g.x).putInt("g_y", g.y).putInt("g_major", g.major).putBoolean("g_left", g.fromLeft)
        .putBoolean("g_bottom", g.fromBottom).putInt("g_sx", g.sx).putInt("g_sy", g.sy).putInt("g_alpha", g.alpha).putBoolean("g_labels", g.labels).apply()
}

/** The picture Face Prep works on for one face, and the room box on it when that is already known. */
private class FaceSrc(val uri: Uri, val auto: FacePrep.Box?)

/**
 * The ELEVATION the split made from the corners set there, when it exists:
 * squared on, with the room's corners at a known place, so the box needs no
 * hand. Otherwise the plain face (a capture split before corners were set),
 * where the box has to be lined up by hand, and the screen says so.
 */
private fun faceSource(context: Context, deviceId: String, face: String): FaceSrc? {
    val pf = FacePrep.panoFace(face)
    findByName(context, FaceWriter.elevationFilename(deviceId, pf))?.let { u ->
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { context.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o) } }
        if (o.outWidth > 0 && o.outHeight > 0) return FaceSrc(u, FacePrep.elevationBox(o.outWidth, o.outHeight, FaceWriter.ELEV_MARGIN))
    }
    return FaceFiles.findFace(context, deviceId, pf)?.let { FaceSrc(it, null) }
}

private fun findByName(context: Context, name: String): Uri? = runCatching {
    context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media._ID),
        "${MediaStore.Images.Media.DISPLAY_NAME} = ?", arrayOf(name), null)?.use { c ->
        if (c.moveToFirst()) android.content.ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(0)) else null }
}.getOrNull()

/** A face photo, subsampled so its longest side is at most [maxPx]. */
internal fun decodeFace(context: Context, uri: Uri, maxPx: Int): Bitmap? = runCatching {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
    var s = 1; while (max(o.outWidth, o.outHeight) / s > maxPx) s *= 2
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s }) }
}.getOrNull()

/**
 * Every face as an ImageMeter photo: cropped round its box, marks drawn at
 * full resolution, a caption band — saved NEXT TO the original face, in the
 * folder ImageMeter's importer already browses, as MCAP-…_<face>-faceprep.jpg.
 * The "-faceprep" token keeps LocalFaces from ever taking it for the face
 * itself. Re-saving replaces the previous copy. Returns how many were saved.
 */
private fun exportAll(context: Context, deviceId: String, roomName: String, room: FacePrep.Room, fovDeg: Double): Int {
    var n = 0
    for (face in FacePrep.FACE_ORDER) {
        val fs = faceSource(context, deviceId, face) ?: continue
        val uri = fs.uri
        val src = decodeFace(context, uri, 4096) ?: continue
        val side = src.width.toFloat()
        val box = room.face(face).box ?: fs.auto ?: FacePrep.cameraGuess(room, face, fovDeg) ?: continue
        val xs = listOf(box.x0, box.x1).map { it * side }; val ys = listOf(box.y0, box.y1).map { it * side }
        val bw = xs[1] - xs[0]; val bh = ys[1] - ys[0]
        val x0 = max(0.0, xs[0] - bw * 0.2); val y0 = max(0.0, ys[0] - bh * 0.14)
        val x1 = min(side.toDouble(), xs[1] + bw * 0.2); val y1 = min(side.toDouble(), ys[1] + bh * 0.14)
        val s = max(1.0, 2400 / (x1 - x0)).toFloat()
        val out = Bitmap.createBitmap(((x1 - x0) * s).toInt().coerceAtLeast(1), ((y1 - y0) * s).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(out)
        c.drawBitmap(src, android.graphics.Rect(x0.toInt(), y0.toInt(), x1.toInt(), y1.toInt()),
            android.graphics.RectF(0f, 0f, out.width.toFloat(), out.height.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
        FacePrepDraw.draw(c, room, face, box, side, { x, y -> floatArrayOf(((x - x0) * s).toFloat(), ((y - y0) * s).toFloat()) },
            max(1.4f, out.width / 1200f), FacePrepDraw.Opts())
        val (w, h) = FacePrep.faceDims(room, face)
        val band = (out.width * 0.03f)
        c.drawRect(0f, out.height - band, out.width.toFloat(), out.height.toFloat(), Paint().apply { color = 0xCC0F0E0C.toInt() })
        c.drawText("$roomName · ${FacePrep.LABEL[face]} · face ${w}×${h} mm · mm scaled, Disto is the real figure", band * 0.5f, out.height - band * 0.32f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF1EDE6.toInt(); textSize = band * 0.5f; typeface = android.graphics.Typeface.MONOSPACE })
        if (saveNextTo(context, uri, Handover.filename(deviceId, FacePrep.panoFace(face)).removeSuffix(".jpg") + "-faceprep.jpg", out)) n++
        out.recycle(); src.recycle()
    }
    return n
}

private fun saveNextTo(context: Context, faceUri: Uri, name: String, bmp: Bitmap): Boolean = runCatching {
    val cr = context.contentResolver
    val path = cr.query(faceUri, arrayOf(MediaStore.Images.Media.RELATIVE_PATH), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        ?: return false
    cr.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} = ?", arrayOf(name, path))
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name); put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, path); put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    cr.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) } ?: return false
    cr.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
    true
}.getOrDefault(false)

/** The measures, every face, as CSV text handed to the share sheet (Drive, WhatsApp, mail). */
private fun shareCsv(context: Context, roomName: String, deviceId: String, room: FacePrep.Room) {
    fun cell(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
    val rows = mutableListOf("Room,Face,Measure,mm")
    for (face in FacePrep.FACE_ORDER) for ((m, mm) in FacePrep.measures(room, face))
        rows.add(listOf(roomName, FacePrep.LABEL.getValue(face), m, mm.toString()).joinToString(",") { cell(it) })
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, "$roomName measures ($deviceId)").putExtra(Intent.EXTRA_TEXT, rows.joinToString("\n")), "Share measures"))
}
