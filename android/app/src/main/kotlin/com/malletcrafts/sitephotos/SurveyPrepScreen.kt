package com.malletcrafts.sitephotos

import android.graphics.Bitmap
import android.graphics.Paint
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.malletcrafts.sitephotos.pano.GestureClassifier
import com.malletcrafts.sitephotos.pano.Panorama
import com.malletcrafts.sitephotos.pano.StepRoute
import com.malletcrafts.sitephotos.pano.SurveyPrep
import com.malletcrafts.sitephotos.pano.SurveyPrepFile
import com.malletcrafts.sitephotos.pano.SurveyPrepPalette
import com.malletcrafts.sitephotos.pano.SurveyPrepTouch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * SURVEY PREP -- bound each surface, square it to size, mark what is on it.
 * Mock-up v33, approved by Amit as the spec for this screen; it replaces the
 * four-floor-corners-and-a-ceiling-line room setup, from the same button.
 *
 *  - Six surfaces, each its own photo: Front / Right / Back / Left are level
 *    120-degree views, Floor and Ceiling straight down and straight up with
 *    the front wall at the top and the left wall on the left. Nothing on one
 *    surface moves anything on another.
 *  - 1 · Lines: four boundary lines per surface. Drag a line to move it, a
 *    diamond grip pivots it, and every line has its own permanent row of
 *    buttons under the photo (1 or 5 mm, 0.1 degree). No numbers on lines.
 *  - 2 · Mark: the surface squared to the laser size; boxes for what is on
 *    it, each with its checklist of measurements, the photo's estimate, and
 *    the site reading from the DISTO D2 or typed. The palette under the
 *    photo (v52 slice 7): Select, three types, More ▸; a type stays on and
 *    every tap on empty photo places one where the finger is.
 *  - 3 · List: the surface's checklist, the only part of the screen that
 *    scrolls (v52 slice 6).
 *  - The faces are tabs and the steps a strip under them; the photo takes
 *    every dp the controls leave; ✓ Save is in the bar, and the status, the
 *    laser sizes, the web lines, Start over and Discard are behind ⋮.
 *  - Lines, marks and readings save after each change and come back when
 *    the screen is opened again (SurveyPrepStore).
 *
 * The laser L, W and H come from the room capture (or, once saved, from the
 * prep itself); a tap on the size line changes them (v51). All maths is
 * SurveyPrep in :pano, where it is tested; drawing is SurveyPrepDraw.
 */
@Composable
internal fun SurveyPrepDialog(
    /** The capture the prep belongs to: its file is filesDir/surveyprep/<deviceId>.json. */
    deviceId: String,
    /** The 360 at preview size: the split's own copy, or a kept capture's file decoded again. */
    previewPano: Panorama.Image,
    title: String,
    lengthMm: Double,
    widthMm: Double,
    heightMm: Double,
    /** Save: the prep, and the room size it is squared to now (L, W, H mm -- the size line may have changed it). */
    onDone: (SurveyPrep.Prep, DoubleArray) -> Unit,
    /** Back without Save: the work is kept on the phone either way, and handed back as it stands, with its size. */
    onCancel: (SurveyPrep.Prep, DoubleArray) -> Unit,
    /** A NEW capture's way out: drop the 360 altogether. Null on a kept capture. */
    onDiscard: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val store = remember { SurveyPrepStore(context) }
    val id = deviceId

    // THE FILE, OPENED TOLERANTLY (v52 slice 2): what cannot be read whole is set aside before anything saves
    val opened = remember { store.open(id) }
    val decoded = when (opened) {
        is SurveyPrepFile.Opened.Restored -> opened.decoded
        is SurveyPrepFile.Opened.Salvaged -> opened.decoded
        is SurveyPrepFile.Opened.Missing, is SurveyPrepFile.Opened.Unreadable -> null
    }
    val loaded = decoded?.prep
    // THE ROOM SIZE IN USE (v51; Amit, 15 Oct 2026: "Yes, with slice 2"): the saved prep's own size, which its
    // lines were squared to, else the capture's; the size sheet changes it, and every face, save and caller follows
    var roomSize by remember { mutableStateOf(decoded?.size ?: doubleArrayOf(lengthMm, widthMm, heightMm)) }
    val prepNote = remember { SurveyPrepFile.openedNote(opened) }
    var prep by remember { mutableStateOf(loaded ?: SurveyPrep.fresh(roomSize[0], roomSize[1], roomSize[2])) }
    var rev by remember { mutableIntStateOf(0) }
    var tab by remember { mutableStateOf("front") }
    var mode by remember { mutableStateOf("lines") }
    var show by remember { mutableStateOf("both") }
    var sel by remember { mutableStateOf<SpSel?>(null) }
    var active by remember { mutableStateOf<String?>(null) }
    val hist = remember { mutableStateListOf<SurveyPrep.Prep>() }
    // "N points re-centred" (v52 slice 4): said once, on the open that made older points 50 mm spots
    var note by remember { mutableStateOf(SurveyPrepFile.pointsNote(opened)) }
    var savedMsg by remember { mutableStateOf(SurveyPrepFile.savedText(opened)) }
    var dirty by remember { mutableIntStateOf(0) }
    var armed by remember { mutableStateOf(false) }
    var armedDiscard by remember { mutableStateOf(false) }
    // ⋮ (v52 slice 6): the status, the laser sizes, the web lines, Start over and Discard
    var menuOpen by remember { mutableStateOf(false) }
    // THE PALETTE (v52 slice 7): the tool on in 2 · Mark -- Select, or a type's letter, STICKY until another is
    // picked or the one on is tapped again -- and whether the More sheet is open
    var tool by remember { mutableStateOf(SurveyPrepPalette.SEL) }
    var moreOpen by remember { mutableStateOf(false) }
    val views = remember { mutableStateMapOf<String, Bitmap>() }
    val rects = remember { mutableStateMapOf<String, SpRect>() }
    LaunchedEffect(note) { if (note != null) { delay(2600); note = null } }
    LaunchedEffect(armed) { if (armed) { delay(4000); armed = false } }
    LaunchedEffect(armedDiscard) { if (armedDiscard) { delay(4000); armedDiscard = false } }

    // ---- the size sheet (v51): L, W, H boxes, Cancel / Apply
    var lwhOpen by remember { mutableStateOf(false) }
    val lwhText = remember { mutableStateListOf(TextFieldValue(""), TextFieldValue(""), TextFieldValue("")) }
    var lwhBad by remember { mutableStateOf(emptyList<Int>()) }

    fun ab(s: String) = SurveyPrep.size(s, roomSize[0], roomSize[1], roomSize[2])
    fun frameOf(s: String) = ab(s).let { SurveyPrep.frame(prep.surface(s).lines, it[0], it[1]) }
    fun keyOf(s: String): String? = frameOf(s)?.let { f ->
        f.q.joinToString(";") { "%.3f,%.3f".format(it[0], it[1]) } + "|" + f.a + "x" + f.b }

    // Saved a moment after each change, then read back: the tick is only shown when what is on disk matches.
    val unsaved = remember { booleanArrayOf(false) }
    fun changed() { dirty++; rev++; unsaved[0] = true }
    LaunchedEffect(dirty) {
        if (dirty == 0) return@LaunchedEffect
        savedMsg = "…"
        delay(800)
        val ok = store.save(id, prep, roomSize[0], roomSize[1], roomSize[2])
        if (ok) unsaved[0] = false
        savedMsg = if (ok) "Saved ✓ " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) else "Not saved: did not read back"
    }
    // A prep opened for the first time is written at once, so the laser sizes it squares to are kept
    // with it even if nothing is changed (a kept capture reopens on exactly these). So is a prep read
    // in part whose original is SAFELY ASIDE, so the live file heals and the next open is Restored
    // rather than another copy aside. Never one that could not be read, nor a salvaged one whose copy
    // failed (SurveyPrepFile.saveOnOpen; the store refuses those saves anyway). And a prep whose older
    // points were just made spots (v52 slice 4): written once, so the next open finds nothing to convert;
    // for a room already sent to the web the upload is queued again, and its readings sync sends each
    // converted reading as a delete of the edge key and a set of the centre key, each on the `at` this
    // phone last saw (ReadingsSync.push) -- held by WorkManager until there is a network.
    LaunchedEffect(Unit) {
        if (SurveyPrepFile.saveOnOpen(opened)) {
            val ok = store.save(id, prep, roomSize[0], roomSize[1], roomSize[2])
            if (ok && SurveyPrepFile.convertedOf(opened) && AsIsUpload.status(context, id) != null) AsIsUpload.enqueue(context, id)
        }
    }
    // Closing the screen inside that moment must not lose the last change.
    DisposableEffect(Unit) { onDispose { if (unsaved[0]) store.save(id, prep, roomSize[0], roomSize[1], roomSize[2]) } }

    // ---- the as-is upload to MCFT Interior Estimate's web half: queued on Save, its state read back here
    var webState by remember { mutableStateOf(AsIsUpload.status(context, id)) }
    var readingsMsg by remember { mutableStateOf<String?>(null) }
    // A pull of the web's readings is asked for by bumping this: on open, on resume, and when an upload lands.
    var pullTick by remember { mutableIntStateOf(1) }
    LaunchedEffect(Unit) {
        var wasDone = webState?.second == true
        while (true) {
            webState = withContext(Dispatchers.IO) { AsIsUpload.status(context, id) }
            val isDone = webState?.second == true
            if (isDone && !wasDone) pullTick++
            wasDone = isDone
            delay(3000)
        }
    }
    val prepLifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(prepLifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) pullTick++
        }
        prepLifecycle.addObserver(obs)
        onDispose { prepLifecycle.removeObserver(obs) }
    }

    fun snapshot() { hist.add(prep.copy()); if (hist.size > 40) hist.removeAt(0) }
    fun undo() {
        if (hist.isEmpty()) return
        val old = hist.removeAt(hist.size - 1)
        // Readings are not geometry: the ones taken since stay, and a reading a delete took away comes back with its mark.
        val merged = old.actual.toMutableMap().apply { putAll(prep.actual) }
        val back = SurveyPrep.Prep(old.surfaces, mutableMapOf(), old.extra, old.extraSurfaces)
        for ((k, v) in merged) if (back.markOf(k) != null) back.actual[k] = v
        prep = back; sel = null
        if (active != null && back.markOf(active!!) == null) active = null
        changed()
    }

    // ---- the DISTO D2: a reading lands in the ACTIVE field, then the next unmeasured one of that mark is active
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
    fun reading(code: String, v: Double) {
        prep.actual[code] = v
        val mo = prep.markOf(code)
        active = mo?.let { (s, e) -> ab(s).let { prep.nextOpen(s, e, code, it[0], it[1]) } }
        changed()
    }
    fun fire() {
        if (active == null) { note = "Tap a measurement first"; return }
        when (distoState) {
            DistoClient.State.OFF -> askBle.launch(blePerms)
            DistoClient.State.READY -> disto.measure()
            else -> note = "The laser is still connecting…"
        }
    }
    disto.onState = { s, n -> distoState = s; if (n != null) note = n }
    disto.onRefused = { why -> note = why }
    disto.onReading = { r ->
        val a = active
        if (a == null || prep.markOf(a) == null) note = "D2 ${r.mm} mm — tap a measurement first, then read again"
        else reading(a, r.mm.toDouble())
    }

    // ---- site readings from the web (ReadingsSync): what changed there is taken, never an unsent edit here
    LaunchedEffect(pullTick) {
        if (pullTick == 0) return@LaunchedEffect
        val row = withContext(Dispatchers.IO) { CaptureStore(context).all().firstOrNull { it.deviceId == id } }
        val auth = FrappeClient.authorization(context)
        val fetched: Result<Map<String, com.malletcrafts.sitephotos.pano.Readings.Remote>>? =
            if (row == null || row.project.isBlank() || auth == null) null
            else withContext(Dispatchers.IO) { runCatching { ReadingsSync.fetch(auth, row.project, row.room) } }
        val remote = fetched?.getOrNull()
        val (applied, moved) = ReadingsSync.merge(context, id, prep.actual, remote)
        // AN EDGE READING THAT ARRIVED NOW (a web reading not seen before, or a conflict handing the server's
        // value back) is converted to the centre like the ones on opening -- or it would sit under l / s for
        // good, unasked by the checklist. normPoints converts only what still carries an edge key, so this is
        // nothing on every ordinary pull; when it does convert, the prep is written at once and the upload is
        // queued again so the delete of the edge key and the set of the centre key reach the web.
        val fix = SurveyPrep.normPoints(prep)
        if (fix.changed) {
            for (line in fix.log) android.util.Log.i("SurveyPrep", "$id: $line")
            prep = fix.prep
            val ok = store.save(id, prep, roomSize[0], roomSize[1], roomSize[2])
            if (ok && AsIsUpload.status(context, id) != null) AsIsUpload.enqueue(context, id)
        }
        if (moved || fix.changed) changed()
        val head = if (applied.isEmpty()) "" else ReadingsSync.conflictText(applied) + " · "
        val e = fetched?.exceptionOrNull()
        readingsMsg = when {
            fetched == null -> if (applied.isEmpty()) readingsMsg else head.removeSuffix(" · ")
            e == null -> head + "Readings from web ✓ " + ReadingsSync.now()
            ReadingsSync.notAvailable(e!!) -> head + ReadingsSync.NOT_YET
            else -> head + "Readings from web: " + (e?.message ?: "failed").take(160)
        }
    }

    // ---- pictures: the six views once, and each surface squared whenever its lines change
    LaunchedEffect(Unit) {
        for (s in listOf(tab) + SurveyPrep.SURFACES.filter { it != tab }) {
            val bmp = withContext(Dispatchers.Default) { FaceWriter.bitmapOf(SurveyPrep.cutView(previewPano, s)) }
            views[s] = bmp
        }
    }
    val need = when {
        tab == "list" -> SurveyPrep.SURFACES.filter { prep.surface(it).done() }
        mode == "mark" || mode == "list" -> listOf(tab)
        else -> emptyList()
    }
    val wanted = need.mapNotNull { s -> keyOf(s)?.let { s to it } }
    LaunchedEffect(wanted.joinToString("#") { it.first + "=" + it.second }) {
        for ((s, k) in wanted) {
            if (rects[s]?.key == k) continue
            val f = frameOf(s) ?: continue
            val sq = SurveyPrep.Square.screen(f.a, f.b)
            val bmp = withContext(Dispatchers.Default) { FaceWriter.bitmapOf(SurveyPrep.render(previewPano, s, f, sq)) }
            rects[s] = SpRect(k, sq, bmp)
        }
    }
    fun rectOf(s: String): SpRect? = rects[s]?.takeIf { it.key == keyOf(s) }

    // ---- selecting and editing
    fun selectMark(s: String, e: SurveyPrep.Mark, measure: String?) {
        val fresh = sel?.id != e.id
        sel = SpSel(s, id = e.id)
        if (measure != null) active = measure
        else if (fresh) active = ab(s).let { prep.nextOpen(s, e, null, it[0], it[1]) }
        if (tab != s && tab != "list") { tab = s; mode = "mark" }
        rev++
    }
    /** A finger down on a line or a mark: it is selected, with the part grabbed; a newly selected mark's
     *  first unmeasured field becomes the active one. */
    fun grabbed(g: SpSel) {
        val fresh = g.id != null && sel?.id != g.id
        sel = g
        if (fresh) prep.markById(g.id!!)?.let { (s, e) -> active = ab(s).let { prep.nextOpen(s, e, null, it[0], it[1]) } }
        rev++
    }
    /**
     * A TAP WITH A TYPE ON PLACES IT WHERE THE FINGER IS (v52 slice 7; Amit, 11 Oct 2026 20:35: "electric points
     * are spots . dont let them resize. have its da 50 mm." -- a spot centred where he taps): the type's default
     * size centred on ([mx], [my]) mm and kept inside the face (SurveyPrep.placeAt), the new mark selected with its
     * first measurement active, one undo step. Off the face it says "Tap inside the front wall to place it" and
     * places nothing. The tool stays on: the next tap places another. [t] is the type that was on when the
     * finger went DOWN (the photo's press captures it), not whatever the palette shows by the time it lifts.
     */
    fun placeMark(t: String, mx: Double, my: Double): SurveyPrep.Mark? {
        val s = tab; val a = ab(s)
        if (mode != "mark" || SurveyPrep.types(s).none { it.letter == t }) return null
        if (!SurveyPrep.onFace(a[0], a[1], mx, my)) { note = SurveyPrep.placeMiss(s); return null }
        snapshot()
        val e = prep.addMarkAt(s, t, mx, my, a[0], a[1], java.util.UUID.randomUUID().toString().take(8))
        sel = SpSel(s, id = e.id); active = prep.nextOpen(s, e, null, a[0], a[1])
        changed()
        return e
    }
    /** A palette slot (or a More tile, or ✕ on the chip): that tool on, or Select when it was already on. */
    fun pickTool(t: String) {
        tool = SurveyPrepPalette.next(tool, t); moreOpen = false
        if (t != SurveyPrepPalette.SEL) note = null
    }
    fun deleteMark(mid: String) {
        val mo = prep.markById(mid) ?: return
        snapshot(); prep.deleteMark(mid)
        if (sel?.id == mid) sel = null
        if (active?.startsWith(mo.second.code + ".") == true) active = null
        changed()
    }
    fun nudgeMark(dx: Double, dy: Double) {
        val sl = sel ?: return; val mid = sl.id ?: return
        val e = prep.surface(sl.s).marks.firstOrNull { it.id == mid } ?: return
        val a = ab(sl.s); snapshot()
        SurveyPrep.moveMark(e, e.copy(), sl.part, dx, dy, a[0], a[1])     // buttons never snap
        changed()
    }
    fun nudgeLine(slot: String, mm: Double, deg: Double) {
        val s = tab; val sf = prep.surface(s)
        sel = SpSel(s, line = slot)
        if (mm != 0.0) {
            val moved = SurveyPrep.nudgeLine(sf.lines.getValue(slot), slot, frameOf(s), mm)
            if (moved == null) { note = "These four lines do not make a closed shape yet — drag them first"; return }
            snapshot(); sf.lines[slot] = moved
        } else {
            snapshot(); sf.lines[slot] = SurveyPrep.turnLine(sf.lines.getValue(slot), deg)
        }
        sf.set[slot] = true
        changed()
    }
    // a face or a step changed: back to Select, the More sheet shut (sp.js showTab / setMode)
    fun showTab(t: String) { tab = t; sel = null; menuOpen = false; tool = SurveyPrepPalette.SEL; moreOpen = false; rev++ }
    fun setMode(m: String) {
        if (m == "mark" && frameOf(tab) == null) { note = "These four lines do not make a closed shape yet"; return }
        mode = m; sel = null; tool = SurveyPrepPalette.SEL; moreOpen = false; rev++
    }

    fun openLwh() {
        for (i in 0 until 3) { val t = roomSize[i].roundToInt().toString(); lwhText[i] = TextFieldValue(t, TextRange(0, t.length)) }
        lwhBad = emptyList(); lwhOpen = true
    }
    /** Apply: every face follows the new size; marks keep their mm, and one now past its face is flagged,
     *  not moved. A size change is not an undo step (↶ undoes lines, marks and readings only). */
    fun applyLwh() {
        val r = SurveyPrep.sizeEdit(lwhText.map { it.text }, roomSize)
        val z = r.size
        if (z == null) { lwhBad = r.bad; return }
        lwhOpen = false
        if (r.same) return
        roomSize = z
        note = prep.outsideNote(z[0], z[1], z[2])
            ?: "Room size L ${z[0].roundToInt()} · W ${z[1].roundToInt()} · H ${z[2].roundToInt()} mm"
        changed()
    }

    val done = SurveyPrep.SURFACES.count { prep.surface(it).done() }
    // where the size line and the screen are, for placing the size sheet under the line
    var rootTop by remember { mutableStateOf(0f) }
    var rootH by remember { mutableStateOf(0) }
    var lwhRowBottom by remember { mutableStateOf(0f) }
    var lwhRowLeft by remember { mutableStateOf(0f) }
    var sheetH by remember { mutableStateOf(0) }

    // ---- v52 slice 6 (item 9): THE FACES ARE TABS, THE STEPS A STRIP UNDER THEM, AND THE PHOTO TAKES THE HEIGHT LEFT.
    // Amit, 14 Oct 2026 06:23 IST: "all having same size confuses me. its actually parent child relation for face and
    // 1 2 3 4."; 20:45 IST: "Why all faces amd 1 2 3 4 have same look and feel. i had requested distinction in them."
    // NOTHING SCROLLS BUT A LIST: the photo has weight 1, so it gets every dp the controls leave, in both orientations;
    // the status, the laser sizes, the web lines, Start over and Discard sit behind ⋮ in the bar, with ✓ Save beside it.
    val conf = LocalConfiguration.current
    val land = conf.screenWidthDp > conf.screenHeightDp
    val listView = StepRoute.listView(tab, mode)
    val face = tab != "list"
    val dens = LocalDensity.current
    // Where each step of the strip is, and the strip's own top-left, in root px: a press within 48 dp under a step
    // (the caption, the top of the photo, the empty part of the Show row) is that step's -- StepRoute.stepFor, in :pano.
    // Only ever read by the pointer handlers, never by composition, so laying them out recomposes nothing.
    val stepBoxes = remember { mutableStateMapOf<String, Rect>() }
    var stripAt by remember { mutableStateOf(Offset.Zero) }
    var capAt by remember { mutableStateOf(Offset.Zero) }
    var showAt by remember { mutableStateOf(Offset.Zero) }
    var gapAt by remember { mutableStateOf(Offset.Zero) }
    // Where ← and ✓ Save are, in root px, and the ⋮ overlay's own top-left: with ⋮ open a tap outside the menu closes
    // it, and a tap that lands on ← or ✓ Save then goes on to Back or Save (sp.js 1559). Read only by the overlay's
    // tap handler, never by composition.
    var backBox by remember { mutableStateOf(Rect.Zero) }
    var saveBox by remember { mutableStateOf(Rect.Zero) }
    var overlayAt by remember { mutableStateOf(Offset.Zero) }
    fun stepAt(x: Float, y: Float): String? =
        if (!StepRoute.showStrip(tab)) null
        else StepRoute.stepFor(x.toDouble(), y.toDouble(),
            StepRoute.STEPS.mapNotNull { (k, _) ->
                stepBoxes[k]?.let { r -> StepRoute.Step(k, r.left.toDouble(), r.top.toDouble(), r.right.toDouble(), r.bottom.toDouble()) } },
            stripAt.y.toDouble(), dens.density.toDouble())
    // The long-lived pointer handlers below read these, so a handler started on an earlier frame acts on this one.
    val routeS by rememberUpdatedState(newValue = { x: Float, y: Float -> stepAt(x, y) })
    val goStepS by rememberUpdatedState(newValue = { k: String -> setMode(k) })

    val statusText = when {
        tab == "list" -> {
            val marks = SurveyPrep.SURFACES.sumOf { prep.surface(it).marks.size }
            val items = SurveyPrep.SURFACES.sumOf { s -> prep.surface(s).marks.sumOf { e -> SurveyPrep.measures(s, e, ab(s)[0], ab(s)[1]).size } }
            "The engineer's sheet: $marks mark${if (marks == 1) "" else "s"}, $items measurement${if (items == 1) "" else "s"}, ${prep.actual.size} read. "
        }
        mode == "lines" -> {
            val left = SurveyPrep.SLOTS.filter { prep.surface(tab).set[it] != true }.map { SurveyPrep.lineNames(tab).getValue(it) }
            (if (left.isNotEmpty()) "Place: ${left.joinToString(", ")}. " else "All four lines placed — go to 2 · Mark. ")
        }
        mode == "list" -> {
            val els = prep.surface(tab).marks
            val n = els.sumOf { e -> SurveyPrep.measures(tab, e, ab(tab)[0], ab(tab)[1]).size }
            "${SurveyPrep.NAME[tab]}: ${els.size} mark${if (els.size == 1) "" else "s"}, $n measurement${if (n == 1) "" else "s"}. "
        }
        else -> "Squared to the laser size. Add what is on this ${if (SurveyPrep.isPlan(tab)) "surface" else "wall"}; tap a mark to edit it. "
    }
    val openShape = face && mode == "lines" && frameOf(tab) == null
    val boundedText = "$done of 6 surfaces bounded. Save needs all six; marks and readings never block it."

    fun back() { menuOpen = false; onCancel(prep.copy(), roomSize.copyOf()) }
    /** ✓ Save: only with all six surfaces bounded. */
    fun saveNow() {
        menuOpen = false
        if (done != 6) return
        // The read-back save, then the as-is upload to the web (AsIsUpload); a save that did not
        // read back queues nothing, so the web never gets a prep this phone does not hold.
        if (store.save(id, prep, roomSize[0], roomSize[1], roomSize[2])) {
            unsaved[0] = false
            AsIsUpload.enqueue(context, id)
            webState = AsIsUpload.status(context, id)
        }
        onDone(prep.copy(), roomSize.copyOf())
    }
    // The ⋮ overlay's tap: closes the menu; on ← or ✓ Save it then does that too
    val outsideTapS by rememberUpdatedState<(Offset) -> Unit>(newValue = { off ->
        val pt = off + overlayAt
        menuOpen = false
        if (backBox.contains(pt)) back()
        else if (saveBox.contains(pt)) saveNow()
    })

    // ---- the bar: ← · title (and the save state under it) · ↶ · ⋮ · ✓ Save
    val bar: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().height(if (land) 32.dp else 40.dp), verticalAlignment = Alignment.CenterVertically) {
            SpText("←", SP_FG, 20, Modifier.onGloballyPositioned { backBox = it.boundsInRoot() }
                .clip(CircleShape).clickable { back() }
                .padding(horizontal = 8.dp, vertical = 2.dp))
            Column(Modifier.weight(1f)) {
                Text("$title · survey prep", color = SP_FG, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(savedMsg, color = SP_OK, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            SpText("↶", if (hist.isEmpty()) SP_DIM else SP_FG, 20,
                Modifier.clip(CircleShape).clickable(enabled = hist.isNotEmpty()) { undo() }.padding(horizontal = 6.dp, vertical = 2.dp))
            Text("⋮", color = SP_FG, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (menuOpen) SP_LINE_BORDER else Color.Transparent)
                    .clickable { menuOpen = !menuOpen }
                    .semantics { contentDescription = "More: status, Start over, Discard" }
                    .padding(horizontal = 8.dp, vertical = 2.dp))
            // ✓ SAVE: gold once all six surfaces are bounded, and only then does it do anything
            Box(Modifier.padding(start = 2.dp).onGloballyPositioned { saveBox = it.boundsInRoot() }
                .height(if (land) 28.dp else 32.dp).clip(RoundedCornerShape(16.dp))
                .background(if (done == 6) SP_GOLD else SP_LINE_BORDER)
                .clickable(enabled = done == 6) { saveNow() }
                .semantics { contentDescription = "Save survey prep" }
                .padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                Text("✓ Save", color = if (done == 6) Color(0xFF1A1A12) else Color(0xFF7D8278), fontWeight = FontWeight.Bold,
                    fontSize = 13.sp, maxLines = 1)
            }
        }
    }

    // The line that STAYS on screen when the saved prep could not be read whole (slice 2): never behind ⋮.
    val openedLine: @Composable () -> Unit = {
        prepNote?.let { Text(it, color = SP_AMB, fontSize = 11.sp, lineHeight = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 4.dp)) }
    }

    // ---- THE PARENT: the faces as text tabs, two rows, the open one white and bold over a 3 dp gold bar
    val tabsUi: @Composable () -> Unit = {
        rev    // read: a drag only bumps rev, and the ✓ on a face that just closed must follow it live
        val rowH = StepRoute.tabRowDp(land, listView).dp
        Column(Modifier.fillMaxWidth()) {
            for (row in StepRoute.TAB_ROWS) {
                Row(Modifier.fillMaxWidth().height(rowH).drawBehind {
                    drawLine(SP_LINE_BORDER, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), strokeWidth = 1f)
                }) {
                    for (t in row) {
                        val tick = StepRoute.tabTick(t, t != "list" && prep.surface(t).done())
                        SpTab(StepRoute.tabName(t), tab == t, tick, Modifier.weight(1f).fillMaxHeight()) { showTab(t) }
                    }
                }
            }
        }
    }

    // ---- THE CHILD: the open face's steps, "Front wall ·" then numbered 20 dp circles joined by thin lines. No step
    // has a tap of its own: every press on the strip (and under it, see routeToStep) goes through StepRoute.stepFor.
    val stripUi: @Composable () -> Unit = {
        val ci = StepRoute.stepIndex(mode)
        val tight = land && listView
        Row(Modifier.fillMaxWidth().height(StepRoute.stripDp(land, listView).dp)
            .onGloballyPositioned { stripAt = it.positionInRoot() }
            .pointerInput(Unit) { routeToStep({ stripAt }, { x, y -> routeS(x, y) }, { k -> goStepS(k) }) }
            .padding(start = 2.dp, end = 2.dp, top = if (tight) 0.dp else 2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(StepRoute.stripFace(tab), color = SP_MUTED, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, softWrap = false,
                modifier = Modifier.padding(end = 6.dp))
            StepRoute.STEPS.forEachIndexed { i, (k, n) ->
                if (i > 0) Box(Modifier.weight(1f).height(1.5.dp).background(SP_JOIN))
                val on = i == ci
                val past = ci >= 0 && i < ci
                val lab = StepRoute.label(i)
                Row(Modifier.height(if (tight) 20.dp else if (land) 22.dp else 26.dp)
                    .onGloballyPositioned { stepBoxes[k] = it.boundsInRoot() }
                    .semantics(mergeDescendants = true) {
                        contentDescription = lab
                        role = Role.Tab
                        selected = on
                        onClick(label = lab) { goStepS(k); true }
                    }
                    .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(20.dp).clip(CircleShape).background(if (on) SP_GOLD else Color.Transparent)
                        .border(1.5.dp, if (on) SP_GOLD else if (past) SP_PAST else SP_RING, CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text("${i + 1}", color = if (on) Color(0xFF14160F) else if (past) Color(0xFFD9CFA5) else SP_TAB,
                            fontSize = 11.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    if (on) Text(n, color = SP_GOLD, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, softWrap = false)
                }
            }
        }
    }

    // the photo-vs-measured toggle belongs to marks only (Amit, 2026-10-10: "lines should not show that"); its empty
    // part takes part in the strip's touch area, its chips keep their own taps (FX3)
    val showRow: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().onGloballyPositioned { showAt = it.positionInRoot() }
            .pointerInput(Unit) { routeToStep({ showAt }, { x, y -> routeS(x, y) }, { k -> goStepS(k) }) }
            .padding(top = 3.dp, bottom = if (land) 3.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Show", color = SP_MUTED, fontSize = 11.sp, lineHeight = 14.sp)
            for ((k, lab) in listOf("derived" to "≈ Photo", "actual" to "Measured", "both" to "Both")) {
                SpPill(lab, show == k, Modifier.weight(1f), outline = true, compact = land) { show = k; rev++ }
            }
        }
    }

    // the surface's title is a caption ABOVE the photo, never on it; it takes part in the strip's touch area
    val caption: @Composable () -> Unit = {
        val ab0 = ab(tab); val tags = SurveyPrep.sizeTag(tab)
        Text(if (mode == "lines") "${SurveyPrep.NAME[tab]} · set its four lines"
             else "${SurveyPrep.NAME[tab]} · ${tags.first} ${ab0[0].roundToInt()} × ${tags.second} ${ab0[1].roundToInt()}",
            color = SP_FG, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().onGloballyPositioned { capAt = it.positionInRoot() }
                .pointerInput(Unit) { routeToStep({ capAt }, { x, y -> routeS(x, y) }, { k -> goStepS(k) }) }
                .padding(start = 6.dp, top = 3.dp, bottom = 3.dp))
    }

    // ---- THE PHOTO, with all the height the controls leave (1 · Lines and 2 · Mark)
    val photoPane: @Composable ColumnScope.() -> Unit = {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val img: Bitmap? = if (mode == "lines") views[tab] else rectOf(tab)?.bmp
            if (img == null) {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                SpPhoto(
                    modifier = Modifier.fillMaxSize(),
                    img = img, tab = tab, mode = mode, rev = rev,
                    prep = prep, ab = ab(tab), frame = frameOf(tab), rect = rectOf(tab),
                    sel = sel, active = active, show = show,
                    onSelect = { s, e, measure -> selectMark(s, e, measure) },
                    onGrab = { g -> grabbed(g) },
                    onBeforeChange = { snapshot() },
                    onMoved = { rev++ },
                    onChanged = { changed() },
                    onDelete = { deleteMark(it) },
                    stepAt = { x, y -> routeS(x, y) },
                    onStep = { k -> goStepS(k) },
                    tool = tool,
                    onPlace = { t, mx, my -> placeMark(t, mx, my) },
                    onTool = { t -> pickTool(t) })
            }
            note?.let { SpNote(it, Modifier.align(Alignment.BottomCenter).padding(8.dp)) }
        }
    }

    // one surface squared, with codes and dimensions, and its table: every surface on the room's List tab, the open
    // face alone on its 3 · List
    val surfaceBlock: @Composable (String, Boolean) -> Unit = { s, room ->
        val a = ab(s); val tg = SurveyPrep.sizeTag(s)
        val act = active
        val actHere = act != null && prep.markOf(act)?.first == s
        Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp)).background(SP_SHEET).padding(6.dp)) {
            Row(Modifier.fillMaxWidth().padding(2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (room) SurveyPrep.NAME.getValue(s) else "${SurveyPrep.NAME[s]} · its list", color = SP_FG,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                if (room || !actHere) Text("${tg.first} ${a[0].roundToInt()} × ${tg.second} ${a[1].roundToInt()} · laser", color = SP_MUTED, fontSize = 12.sp)
                else SpD2(Modifier, "D2 → $act") { fire() }
            }
            val r = if (room) { if (prep.surface(s).done()) rectOf(s) else null } else rectOf(s)
            when {
                room && !prep.surface(s).done() -> Text("Set its four lines first.", color = SP_MUTED, fontSize = 12.sp, modifier = Modifier.padding(4.dp))
                !room && frameOf(s) == null -> Text("Set its four lines in 1 · Lines to see it squared.", color = SP_MUTED, fontSize = 12.sp,
                    modifier = Modifier.padding(4.dp))
                r == null -> CircularProgressIndicator(Modifier.padding(12.dp))
                else -> Canvas(Modifier.fillMaxWidth().aspectRatio(r.sq.width.toFloat() / r.sq.height).clip(RoundedCornerShape(8.dp))) {
                    rev; show; active
                    val sc = size.width / r.sq.width
                    drawIntoCanvas { cv ->
                        val nc = cv.nativeCanvas
                        nc.save(); nc.scale(sc, sc); nc.drawBitmap(r.bmp, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG)); nc.restore()
                        SurveyPrepDraw.drawMarks(nc, s, SurveyPrepDraw.Marks(prep, a[0], a[1], r.sq, "sheet", true, null, active, show,
                            size.width, size.height, null, density), { x, y -> floatArrayOf((x * sc).toFloat(), (y * sc).toFloat()) }, sc)
                    }
                }
            }
            if (!room || prep.surface(s).done()) {
                SpTable(s, prep, a, rev, sel, active, show,
                    onRow = { e, code -> selectMark(s, e, code) },
                    onDelete = { deleteMark(it) },
                    onFocus = { active = it },
                    onValue = { code, v -> if (v != null) reading(code, v) else { prep.actual.remove(code); changed() } },
                    onFire = { code -> active = code; fire() },
                    emptyText = if (room) "Nothing marked on this surface." else "No marks yet — add one in 2 · Mark")
            }
        }
    }

    // ---- THE LIST (3 · List, and the room's List tab): only the list scrolls
    val listPane: @Composable ColumnScope.() -> Unit = {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                if (tab == "list") for (s in SurveyPrep.SURFACES) surfaceBlock(s, true)
                else surfaceBlock(tab, false)
            }
            note?.let { SpNote(it, Modifier.align(Alignment.BottomCenter).padding(8.dp)) }
        }
    }

    // ---- under the photo in 1 · Lines: EVERY LINE HAS ITS OWN CONTROLS, always there, in a compact panel (Amit,
    // 2026-10-10: "why not every line ... have its own control. currently i have to select it first ... lines are one
    // time settings"), then what is left to place
    val linesPanel: @Composable () -> Unit = {
        rev    // read: a line dragged into place updates 'Place: ...' and the bounded count while the finger is down
        val names = SurveyPrep.lineNames(tab)
        Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            for (k in SurveyPrep.SLOTS) {
                val col = Color(SurveyPrepDraw.argb(SurveyPrepDraw.LINE_COLOUR.getValue(k)))
                val h = k == "bottom" || k == "top"
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(Modifier.width(74.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(col))
                        Spacer(Modifier.width(4.dp))
                        Text(names.getValue(k), color = col, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp, maxLines = 2)
                    }
                    val bs = if (h) listOf("▼5" to -5.0, "▼1" to -1.0, "▲1" to 1.0, "▲5" to 5.0)
                             else listOf("◀5" to -5.0, "◀1" to -1.0, "1▶" to 1.0, "5▶" to 5.0)
                    for ((lab, v) in bs) SpButton(lab, Modifier.weight(1f), SP_LINE_BORDER, compact = true) { nudgeLine(k, v, 0.0) }
                    SpButton("↺", Modifier.weight(1f), SP_LINE_BORDER, compact = true) { nudgeLine(k, 0.0, -0.1) }
                    SpButton("↻", Modifier.weight(1f), SP_LINE_BORDER, compact = true) { nudgeLine(k, 0.0, 0.1) }
                }
            }
            Text(statusText, color = SP_MUTED, fontSize = 11.sp, lineHeight = 13.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
            if (openShape) Text("These four lines do not make a closed shape.", color = SP_AMB, fontSize = 11.sp, lineHeight = 13.sp,
                modifier = Modifier.padding(start = 4.dp))
            Text(boundedText, color = SP_MUTED, fontSize = 10.sp, lineHeight = 12.sp, modifier = Modifier.padding(start = 4.dp))
        }
    }

    // ---- under the photo in 2 · Mark: the selected mark's buttons, the measurement being taken, then the palette
    val markPanel: @Composable () -> Unit = {
        rev    // read: a mark's edge dragged moves its live ≈ value while the finger is down
        val sm = sel?.takeIf { mode == "mark" && it.s == tab && it.id != null }?.let { sl -> prep.surface(tab).marks.firstOrNull { it.id == sl.id } }
        if (sm != null) {
            val p = sel!!.part
            val edge = p != "body"
            val horiz = edge && (p.contains('t') || p.contains('b')) && !(p.contains('l') || p.contains('r'))
            val vert = edge && (p.contains('l') || p.contains('r')) && !(p.contains('t') || p.contains('b'))
            val what = when { p == "body" -> "move"; p.length > 1 -> "corner"
                else -> mapOf("l" to "left edge", "r" to "right edge", "t" to "top edge", "b" to "bottom edge")[p] ?: p }
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = if (land) 2.dp else 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${sm.code}\n$what", color = SP_GOLD, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.width(52.dp))
                val bs: List<Triple<String, Double, Double>> = when {
                    horiz -> listOf(Triple("▼5", 0.0, -5.0), Triple("▼1", 0.0, -1.0), Triple("▲1", 0.0, 1.0), Triple("▲5", 0.0, 5.0))
                    vert -> listOf(Triple("◀5", -5.0, 0.0), Triple("◀1", -1.0, 0.0), Triple("1▶", 1.0, 0.0), Triple("5▶", 5.0, 0.0))
                    else -> listOf(Triple("◀", -1.0, 0.0), Triple("▶", 1.0, 0.0), Triple("▼", 0.0, -1.0), Triple("▲", 0.0, 1.0))
                }
                for ((lab, dx, dy) in bs) SpButton(lab, Modifier.weight(1f), SP_GOLD, compact = land) { nudgeMark(dx, dy) }
                SpButton("Delete", Modifier.weight(1.4f), SP_GOLD, compact = land) { deleteMark(sm.id) }
            }
        }
        // THE MEASUREMENT BEING TAKEN, right under the photo, so nothing need be opened while lasering
        val act = active
        val am = act?.let { prep.markOf(it) }?.takeIf { mode == "mark" && it.first == tab }
        val mm = am?.let { (s, e) -> SurveyPrep.measures(s, e, ab(s)[0], ab(s)[1]).firstOrNull { e.code + "." + it.key == act } }
        if (am != null && mm != null && act != null) {
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp).background(Color(0xFF2A2410)).border(1.5.dp, SP_GOLD).padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("▶ ${am.second.code} ${mm.what}", color = Color.White, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, modifier = Modifier.weight(1f))
                Text(mm.est?.let { "≈" + it.roundToInt() } ?: "site only", color = Color(0xFFCFD3CB), fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp, lineHeight = 15.sp)
                SpMmField(act, prep.actual[act], true, Modifier.width(72.dp), onFocus = { active = act }) { v ->
                    if (v != null) reading(act, v) else { prep.actual.remove(act); changed() } }
                SpD2(Modifier) { fire() }
            }
        }
        // THE PALETTE (v52 slice 7) in place of the old toolbar: 6 + 50 dp (6 + 46 in landscape), where SpMarkToolbar
        // took 48 + 5 -- the photo gives up 3 dp in portrait and gains 1 in landscape
        SpPalette(tab, tool, land, onTool = { t -> pickTool(t) }, onMore = { moreOpen = !moreOpen })
    }

    // The sizes half: the status, then the laser L/W/H with its caption. Every Text carries its own line height, or it
    // inherits the theme's 24 sp line (bodyLarge) and the menu outgrows a landscape screen.
    val menuSizes: @Composable () -> Unit = {
        Text("THIS 360", color = SP_MUTED, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 4.dp))
        // ---- the status
        Column(Modifier.padding(start = 6.dp, end = 6.dp, bottom = 6.dp)) {
            Text(statusText, color = SP_MUTED, fontSize = 12.sp, lineHeight = 15.sp)
            if (openShape) Text("These four lines do not make a closed shape.", color = SP_AMB, fontSize = 12.sp, lineHeight = 15.sp)
            prep.outsideNote(roomSize[0], roomSize[1], roomSize[2])?.let {
                Text(it, color = SP_OUT, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)) }
            Text(boundedText, color = SP_MUTED, fontSize = 11.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 2.dp))
        }
        // ---- the laser sizes: a tap opens the size sheet under them (v51)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { openLwh() }
            .onGloballyPositioned { c -> val b = c.boundsInRoot(); lwhRowBottom = b.bottom; lwhRowLeft = b.left }
            .padding(4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            for ((k, v, cap) in listOf(Triple("L", roomSize[0], "front→back"), Triple("W", roomSize[1], "left→right"), Triple("H", roomSize[2], "floor→ceiling"))) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(k, color = SP_FG, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(v.roundToInt().toString(), color = SP_FG, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp, lineHeight = 20.sp)
                    }
                    Text(cap, color = SP_MUTED, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1)
                }
            }
            Text("✎", color = SP_MUTED, fontSize = 14.sp, lineHeight = 18.sp)
        }
        Text("Laser D2 readings from the room capture, in mm. Tap the size line to change them.", color = SP_MUTED, fontSize = 11.sp,
            lineHeight = 14.sp, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
    }
    // The actions half: the web lines (two lines at most each -- a list of web conflicts is cut, not grown), the
    // new-360 note, then Start over and Discard this 360, each still two taps.
    val menuActions: @Composable () -> Unit = {
        webState?.let { (msg, good) ->
            Text(msg, color = if (good) SP_OK else if (msg.startsWith("Waiting")) SP_MUTED else SP_AMB, fontSize = 11.sp, lineHeight = 14.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
        }
        readingsMsg?.let { msg ->
            Text(msg, color = if (msg.contains("✓") && !msg.contains("changed on the web")) SP_OK
                              else if (msg == ReadingsSync.NOT_YET) SP_MUTED else SP_AMB, fontSize = 11.sp, lineHeight = 14.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
        }
        if (onDiscard != null) {
            Text("New 360 — ← keeps it in the room, and its lines and marks save as you go.",
                color = SP_GOLD, fontSize = 11.sp, lineHeight = 14.sp, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
        }
        // ---- start over, discard: each takes two taps
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, if (armed) SP_AMB else SP_LINE_BORDER, RoundedCornerShape(12.dp))
            .clickable {
                if (!armed) armed = true
                else {
                    armed = false; hist.clear(); sel = null; active = null; tool = SurveyPrepPalette.SEL; moreOpen = false
                    prep = SurveyPrep.fresh(roomSize[0], roomSize[1], roomSize[2]); changed()
                }
            }.padding(9.dp), contentAlignment = Alignment.Center) {
            Text(if (armed) "Tap again to clear every line, mark and reading" else "Start over",
                color = if (armed) SP_AMB else SP_MUTED, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center)
        }
        if (onDiscard != null) {
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .border(1.dp, if (armedDiscard) Color(0xFFFF9B7A) else SP_LINE_BORDER, RoundedCornerShape(12.dp))
                .clickable { if (!armedDiscard) armedDiscard = true else { armedDiscard = false; onDiscard() } }
                .padding(9.dp), contentAlignment = Alignment.Center) {
                Text(if (armedDiscard) "Tap again to discard this 360 — nothing is kept" else "Discard this 360",
                    color = if (armedDiscard) Color(0xFFFF9B7A) else SP_MUTED, fontSize = 13.sp, lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
    }
    // ---- BEHIND ⋮ (sp.js 1412: "The status, the laser notes, Start over and Discard this 360 sit behind ⋮ in the top
    // bar"): what a non-scrolling screen cannot hold above the photo, one tap away. NOTHING IN IT SCROLLS: portrait is one
    // 300 dp column (~470 dp at its tallest, of ~650 under the bar); landscape is two 264 dp columns side by side, the
    // sizes on the left (~258 dp at their tallest) and the web lines with Start over and Discard on the right (~224 dp
    // with both armed), of the 286 dp under the bar -- so the two-tap actions are never below a fold.
    val menu: @Composable () -> Unit = {
        val shell = Modifier.clip(RoundedCornerShape(12.dp)).background(SP_CHIP)
            .border(1.dp, SP_LINE_BORDER, RoundedCornerShape(12.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }   // a tap inside never closes it
            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 10.dp)
        if (land) {
            Row(Modifier.widthIn(max = 560.dp).then(shell), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) { menuSizes() }
                Column(Modifier.weight(1f).padding(top = 20.dp)) { menuActions() }
            }
        } else {
            Column(Modifier.widthIn(max = 300.dp).then(shell)) {
                menuSizes()
                menuActions()
            }
        }
    }

    // The phone's Back is the screen's own ←: nothing is lost, the work is already saved.
    // ... and with the size sheet open, Back closes the sheet only; with ⋮ open, the menu only.
    Dialog(onDismissRequest = {
            if (lwhOpen) lwhOpen = false
            else if (menuOpen) menuOpen = false
            else if (moreOpen) moreOpen = false
            else onCancel(prep.copy(), roomSize.copyOf())
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize(), color = SP_BG) {
          Box(Modifier.fillMaxSize().onGloballyPositioned { rootTop = it.positionInRoot().y }.onSizeChanged { rootH = it.height }) {
            rev    // read, so every model change recomposes the screen
            if (!land) {
                // PORTRAIT, 360 x 780: bar 40 · tabs 64 + strip 28 = 92 · (Show) · caption · THE PHOTO · what works on it
                Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp)) {
                    bar()
                    openedLine()
                    tabsUi()
                    if (face) stripUi()
                    if (listView || mode == "mark") showRow()
                    if (listView) listPane()
                    else {
                        caption()
                        photoPane()
                        if (mode == "lines") linesPanel() else markPanel()
                    }
                }
            } else {
                // LANDSCAPE, 780 x 360 (a ~336 dp window under the status bar): the photo on the left at full height;
                // on the right, 318 dp of controls -- bar 32 · tabs 56 + strip 24 = 80 · (Show) · gap · what works on it.
                // 3 · List and the List tab (v42.css .spv.land.lst): the tabs, the strip and Show go to the top of the
                // LEFT column, over the list, and only the bar stays on the right. The salvaged-prep line sits on the
                // left in landscape too, where it shortens the photo rather than clipping the panel under the controls.
                Row(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        openedLine()
                        if (listView) {
                            tabsUi()
                            if (face) stripUi()
                            showRow()
                            listPane()
                        } else {
                            caption()
                            photoPane()
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.width(318.dp).fillMaxHeight()) {
                        bar()
                        if (!listView) {
                            tabsUi()
                            if (face) stripUi()
                            if (mode == "mark") showRow()
                        }
                        // the empty gap under the controls takes part in the strip's touch area (sp.js .spgap)
                        Box(Modifier.weight(1f).fillMaxWidth().onGloballyPositioned { gapAt = it.positionInRoot() }
                            .pointerInput(Unit) { routeToStep({ gapAt }, { x, y -> routeS(x, y) }, { k -> goStepS(k) }) })
                        if (!listView) {
                            if (mode == "lines") linesPanel() else markPanel()
                        }
                    }
                }
            }
            // ---- MORE ▸ (v52 slice 7): "Every mark type · <Face>" over the bottom of the screen -- the whole width in
            // portrait, 340 dp at the right in landscape (v42.css .moresheet); a pick or ✕ closes it
            if (moreOpen && face && mode == "mark") {
                SpMoreSheet(tab, tool,
                    if (land) Modifier.align(Alignment.BottomEnd).width(340.dp) else Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    onPick = { t -> pickTool(t) }, onClose = { moreOpen = false })
            }
            // ---- ⋮: a tap anywhere outside the menu closes it; one on ← or ✓ Save then goes on to Back or Save
            // (sp.js 1559: `if(!ev.target.closest('#save,#spBack')) return;`), anything else does nothing more
            if (menuOpen) {
                Box(Modifier.fillMaxSize().onGloballyPositioned { overlayAt = it.positionInRoot() }
                    .pointerInput(Unit) { detectTapGestures { off -> outsideTapS(off) } }
                    .semantics { onClick(label = "Close the menu") { menuOpen = false; true } })
                Box(Modifier.align(Alignment.TopEnd).padding(start = 10.dp, end = 10.dp, bottom = 10.dp, top = if (land) 40.dp else 48.dp)) {
                    menu()
                }
            }
            // ---- the size sheet, under the size line, over the screen (nothing scrolls for it)
            if (lwhOpen) {
                Box(Modifier.fillMaxSize().background(Color(0x66000000))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { lwhOpen = false })
                val gap = with(dens) { 2.dp.toPx() }; val edge = with(dens) { 4.dp.toPx() }
                val left = max(with(dens) { 8.dp.toPx() }, lwhRowLeft).roundToInt()
                val top = min(lwhRowBottom - rootTop + gap, rootH - sheetH - edge).coerceAtLeast(edge).roundToInt()
                SpSizeSheet(
                    texts = lwhText, bad = lwhBad,
                    modifier = Modifier.offset { IntOffset(0, top) }.padding(start = with(dens) { left.toDp() }, end = 8.dp)
                        .widthIn(max = 344.dp)
                        .onSizeChanged { sheetH = it.height },
                    onText = { i, v ->
                        lwhText[i] = v.copy(text = SurveyPrep.sizeText(v.text))
                        if (i in lwhBad) lwhBad = lwhBad - i
                    },
                    onCancel = { lwhOpen = false },
                    onApply = { applyLwh() })
            }
          }
        }
    }
}

/**
 * THE ROOM SIZE SHEET (prototype v51): L, W and H in mm, prefilled, each a
 * whole number from 300 to 30000; a wrong one is outlined red and the caption
 * reads "300 to 30000 mm", and nothing changes. Done on the keyboard applies.
 */
@Composable
private fun SpSizeSheet(
    texts: List<TextFieldValue>, bad: List<Int>, modifier: Modifier,
    onText: (Int, TextFieldValue) -> Unit, onCancel: () -> Unit, onApply: () -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(SP_SHEET).border(1.5.dp, SP_GOLD, RoundedCornerShape(12.dp))
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
        .padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Room size · mm", color = SP_FG, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((i, k) in listOf("L", "W", "H").withIndex()) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(k, color = SP_GOLD, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(end = 4.dp))
                    BasicTextField(
                        value = texts[i], onValueChange = { v -> onText(i, v) }, singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                            fontSize = 14.sp, textAlign = TextAlign.End),
                        cursorBrush = SolidColor(SP_GOLD),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onApply() }),
                        modifier = Modifier.weight(1f).let { if (i == 0) it.focusRequester(first) else it }
                            .clip(RoundedCornerShape(6.dp)).background(Color.Black)
                            .border(1.5.dp, if (i in bad) SP_OUT else Color(0xFF3A3E35), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 7.dp))
                }
            }
        }
        Text(if (bad.isNotEmpty()) SurveyPrep.SIZE_HINT_BAD else SurveyPrep.SIZE_HINT,
            color = if (bad.isNotEmpty()) SP_OUT else SP_MUTED, fontSize = 11.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpButton("Cancel", Modifier.weight(1f), Color(0xFF3A3E35)) { onCancel() }
            Box(Modifier.weight(1f).clip(RoundedCornerShape(7.dp)).background(SP_GOLD).clickable { onApply() }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center) {
                Text("Apply", color = Color(0xFF1A1A12), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

/** What is selected: a line (in Lines) or a mark and which part of it was grabbed (in Mark). */
internal data class SpSel(val s: String, val line: String? = null, val id: String? = null, val part: String = "body")

/** A surface squared to size, and the frame key it was made for. */
internal class SpRect(val key: String, val sq: SurveyPrep.Square, val bmp: Bitmap)

/** A drag in progress: where it started on the picture, and what moving it does. */
private class SpGrab(val x0: Double, val y0: Double, val sel: SpSel, val focusSlot: String?, val move: (Double, Double) -> Unit)

/** The magnifier(s) while dragging: picture points to centre on, and the finger, canvas px. */
private class SpLoupe(val points: List<DoubleArray>, val finger: Offset)

/**
 * The editing photo: one zoomable picture with the lines (1) or the marks
 * (2) over it. Pinch to zoom, one finger off anything slides when zoomed,
 * "fit" resets; a magnifier follows a drag, and two watch the corners while
 * a line swings. In 2 · Mark a tap with Select picks, a tap with a type on
 * places one where the finger is, and the tool's chip sits top-left (v52 slice 7).
 */
@Composable
private fun SpPhoto(
    /** Its size: the height the controls leave (v52 slice 6), the whole picture letterboxed inside. */
    modifier: Modifier,
    img: Bitmap, tab: String, mode: String, rev: Int,
    prep: SurveyPrep.Prep, ab: DoubleArray, frame: SurveyPrep.Frame?, rect: SpRect?,
    sel: SpSel?, active: String?, show: String,
    onSelect: (String, SurveyPrep.Mark, String?) -> Unit,
    onGrab: (SpSel) -> Unit,
    onBeforeChange: () -> Unit,
    onMoved: () -> Unit,
    onChanged: () -> Unit,
    /** The floating trash pressed beside the picked mark: delete it. */
    onDelete: (String) -> Unit,
    /** Which step of the strip a press at this root position (px) belongs to (StepRoute.stepFor), or null. */
    stepAt: (Float, Float) -> String?,
    /** A press routed to a step: open it. */
    onStep: (String) -> Unit,
    /** The palette's tool (v52 slice 7): SurveyPrepPalette.SEL, or the letter of the type a tap places. */
    tool: String,
    /** Place type t (the letter that was on when the press began) centred at (x, y) mm on this face: the new
     *  mark, or null when refused (off the face, or no longer a type of this face). */
    onPlace: (String, Double, Double) -> SurveyPrep.Mark?,
    /** ✕ on the tool chip: a tool picked (SurveyPrepPalette.SEL). */
    onTool: (String) -> Unit,
) {
    val density = LocalDensity.current
    val iw = img.width.toDouble(); val ih = img.height.toDouble()
    // The gesture below outlives recompositions (it restarts only for a new picture, surface, mode or
    // squaring), so everything it reads that can change under it is read through these.
    val prepS by rememberUpdatedState(prep); val selS by rememberUpdatedState(sel)
    val rectS by rememberUpdatedState(rect); val abS by rememberUpdatedState(ab)
    val onSelectS by rememberUpdatedState(onSelect); val onGrabS by rememberUpdatedState(onGrab)
    val onBeforeChangeS by rememberUpdatedState(onBeforeChange); val onMovedS by rememberUpdatedState(onMoved)
    val onChangedS by rememberUpdatedState(onChanged); val onDeleteS by rememberUpdatedState(onDelete)
    val stepAtS by rememberUpdatedState(stepAt); val onStepS by rememberUpdatedState(onStep)
    val toolS by rememberUpdatedState(tool); val onPlaceS by rememberUpdatedState(onPlace)
    var z by remember(img) { mutableStateOf(1.0) }
    var cx by remember(img) { mutableStateOf(iw / 2) }
    var cy by remember(img) { mutableStateOf(ih / 2) }
    var loupe by remember { mutableStateOf<SpLoupe?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    val tags = remember { mutableListOf<SurveyPrepTouch.Tag>() }
    // The floating trash as last drawn: its centre x, y and radius, canvas px (null: none shown). Filled by the
    // drawing, like [tags], and read by the gesture, which takes a press on it before anything under it.
    val trashAt = remember { arrayOfNulls<FloatArray>(1) }
    var boxW by remember { mutableStateOf(1f) }
    var boxH by remember { mutableStateOf(1f) }
    // Where the photo and the fit button sit on screen, for keeping the magnifier off the button (v52 item 15)
    var photoAt by remember { mutableStateOf(Offset.Zero) }
    var fitAt by remember { mutableStateOf<Rect?>(null) }
    // ... and off the tool chip (v52 slice 7, ct52 FX14): read only while a chip is shown
    var chipAt by remember { mutableStateOf<Rect?>(null) }
    val chipText = if (mode == "mark") SurveyPrepPalette.chipText(tab, tool) else null

    // THE WHOLE PICTURE IN THE BOX at zoom 1, letterboxed (Amit, 2026-10-11: "does not show full foto
    // for marking"): the scale fits both sides of whatever box the screen gives it (v52 slice 6: all the height left).
    fun k() = SurveyPrep.fitScale(boxW.toDouble(), boxH.toDouble(), iw, ih) * z
    fun toS(x: Double, y: Double): FloatArray { val kk = k()
        return floatArrayOf(((x - cx) * kk + boxW / 2).toFloat(), ((y - cy) * kk + boxH / 2).toFloat()) }
    fun fromS(sx: Float, sy: Float): DoubleArray { val kk = k()
        return doubleArrayOf((sx - boxW / 2) / kk + cx, (sy - boxH / 2) / kk + cy) }
    fun clampView() {
        z = z.coerceIn(1.0, 8.0); val kk = k()
        cx = SurveyPrep.clampCentre(cx, iw, boxW / kk); cy = SurveyPrep.clampCentre(cy, ih, boxH / kk)
    }
    fun zoomAt(nz: Double, sx: Float, sy: Float) {
        val b = fromS(sx, sy); z = nz.coerceIn(1.0, 8.0); val a = fromS(sx, sy); cx += b[0] - a[0]; cy += b[1] - a[1]; clampView()
    }

    fun hitLines(sx: Float, sy: Float): SpGrab? {
        val sf = prepS.surface(tab); val grip = with(density) { 18.dp.toPx() }; val body = with(density) { 20.dp.toPx() }
        var best: Triple<String, Int, Float>? = null
        for (slot in SurveyPrep.SLOTS) sf.lines.getValue(slot).forEachIndexed { j, p ->
            val q = toS(p[0], p[1]); val d = hypot(q[0] - sx, q[1] - sy)
            if (d < grip && (best == null || d < best!!.third)) best = Triple(slot, j, d)
        }
        best?.let { (slot, j, _) ->
            val p0 = sf.lines.getValue(slot)[j]
            return SpGrab(p0[0], p0[1], SpSel(tab, line = slot), slot) { ix, iy ->
                sf.lines.getValue(slot)[j] = doubleArrayOf(ix, iy); sf.set[slot] = true }
        }
        val at = fromS(sx, sy); var near: Pair<String, Double>? = null
        for (slot in SurveyPrep.SLOTS) {
            val d = SurveyPrep.distToLine(at, sf.lines.getValue(slot)) * k()
            if (d < body && (near == null || d < near!!.second)) near = slot to d
        }
        val slot = near?.first ?: return null
        val start = sf.lines.getValue(slot).map { it.copyOf() }
        return SpGrab(at[0], at[1], SpSel(tab, line = slot), slot) { ix, iy ->
            sf.lines[slot] = Array(2) { i -> doubleArrayOf(start[i][0] + ix - at[0], start[i][1] + iy - at[1]) }; sf.set[slot] = true }
    }

    /**
     * THE HIT ORDER (v52 item 14; Amit, 13 Oct 2026 21:06 IST: "Check any mark tools handles , not every
     * handle works as expected like center handles"): the picked mark's eight tabs first, then a number tag
     * (TAP A NUMBER ON THE PHOTO TO MEASURE IT, Amit, 2026-10-10), then the marks topmost first -- MOVE
     * FREELY, THEN SET ITS BOUNDARIES: anywhere on a mark moves it, only the picked mark's tabs resize.
     * The order itself is SurveyPrepTouch.hit, tested in :pano; this only measures and acts.
     */
    fun hitMarks(sx: Float, sy: Float, tagged: () -> Unit = {}): SpGrab? {
        val r = rectS ?: return null; val sq = r.sq; val ab = abS
        val els = prepS.surface(tab).marks
        // a point (v52) is a spot: its 25 mm on screen goes with it, so it is grabbed round its centre and never resized
        val drawn = els.map { e ->
            val p = toS(sq.x(e.x0), sq.y(e.y1)); val q = toS(sq.x(e.x1), sq.y(e.y0))
            SurveyPrepTouch.Drawn(e.id, p[0].toDouble(), q[0].toDouble(), p[1].toDouble(), q[1].toDouble(),
                spot = if (SurveyPrep.isPoint(e.t)) SurveyPrep.SPOT / 2 / sq.mpp * k() else null)
        }
        val tol = with(density) { 16.dp.toPx() }.toDouble()
        return when (val h = SurveyPrepTouch.hit(drawn, selS?.id, tags.toList(), sx.toDouble(), sy.toDouble(), tol, density.density.toDouble())) {
            null -> null
            is SurveyPrepTouch.Hit.Tag -> {
                prepS.markOf(h.code)?.let { (s, e) -> onSelectS(s, e, h.code) }
                tagged()
                null
            }
            is SurveyPrepTouch.Hit.Mark -> {
                val e = els.firstOrNull { it.id == h.id }
                if (e == null) null else {
                    val part = h.part; val at = fromS(sx, sy)
                    val start = e.copy(); val mx0 = sq.mmX(at[0]); val my0 = sq.mmY(at[1])
                    SpGrab(at[0], at[1], SpSel(tab, id = e.id, part = part), null) { jx, jy ->
                        SurveyPrep.moveMark(e, start, part, sq.mmX(jx) - mx0, sq.mmY(jy) - my0, ab[0], ab[1])
                        SurveyPrep.snapMark(e, part, ab[0], ab[1])
                    }
                }
            }
        }
    }

    /**
     * A type placed under the finger at ([sx], [sy]) canvas px, and a grab that moves the new mark whole -- a drag
     * that starts on empty photo with a type on places it where the finger went down and carries it (sp.js
     * placeAt, called on the press). [t] is the type captured when the press began. Null when refused (off the
     * face: the note says so) or without a squared face.
     */
    fun placeGrab(sx: Float, sy: Float, t: String): SpGrab? {
        val r = rectS ?: return null; val sq = r.sq; val ab = abS
        val at = fromS(sx, sy); val mx = sq.mmX(at[0]); val my = sq.mmY(at[1])
        val e = onPlaceS(t, mx, my) ?: return null
        val start = e.copy()
        return SpGrab(at[0], at[1], SpSel(tab, id = e.id), null) { jx, jy ->
            SurveyPrep.moveMark(e, start, "body", sq.mmX(jx) - mx, sq.mmY(jy) - my, ab[0], ab[1])
            SurveyPrep.snapMark(e, "body", ab[0], ab[1])
        }
    }

    fun loupeFor(g: SpGrab, ix: Double, iy: Double, finger: Offset): SpLoupe {
        val j = g.focusSlot?.let { SurveyPrep.junctions(tab, prepS.surface(tab).lines, it) }
        return SpLoupe(j ?: listOf(doubleArrayOf(ix, iy)), finger)
    }

    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black)
        .onSizeChanged { boxW = it.width.toFloat(); boxH = it.height.toFloat() }
        .onGloballyPositioned { photoAt = it.positionInRoot() }
        .pointerInput(img, tab, mode, rect) {
            // THE ONE GESTURE LOOP (v52 slice 7). What a finger is doing -- press, drag, tap, long press, double tap,
            // or cancelled by a second finger -- is decided by GestureClassifier in :pano, where it is tested; this
            // only feeds it what the system reports (and the clock, for the long press) and acts on its answers.
            // It outlives gestures, so a double tap is seen across two of them.
            val gc = GestureClassifier(viewConfiguration.touchSlop.toDouble(), GestureClassifier.DOUBLE_TAP_DP.dp.toPx().toDouble())
            awaitEachGesture {
                val first = awaitFirstDown()
                first.consume()
                // THE FLOATING TRASH is on top of everything: a press on it deletes the picked mark when it lifts there.
                // Only with Select on (sp.js fTrashSurf): with a type on, the disc is not there, so a placing tap
                // beside the mark just placed can never land on it and delete that mark
                val tr = if (mode == "mark" && toolS == SurveyPrepPalette.SEL) trashAt[0] else null
                if (tr != null && hypot(first.position.x - tr[0], first.position.y - tr[1]) <= tr[2]) {
                    gc.reset()
                    var at = first.position
                    do {
                        val ev = awaitPointerEvent()
                        ev.changes.forEach { it.consume() }
                        ev.changes.firstOrNull { it.id == first.id }?.let { at = it.position }
                    } while (ev.changes.any { it.pressed })
                    val picked = selS?.id
                    if (picked != null && hypot(at.x - tr[0], at.y - tr[1]) <= tr[2]) onDeleteS(picked)
                    return@awaitEachGesture
                }
                // A PRESS JUST UNDER THE STEP STRIP IS THE STEP'S (v52 slice 6, ct52 X1 / FX3): within 48 dp below a
                // step's top it opens that step, on release, and is never a pan, a grab or a draw; 70 dp down it is the
                // photo's again. The trash above and the fit button (its own tap) keep theirs.
                val routed = stepAtS(first.position.x + photoAt.x, first.position.y + photoAt.y)
                if (routed != null) {
                    gc.reset()
                    do {
                        val ev = awaitPointerEvent()
                        ev.changes.forEach { it.consume() }
                    } while (ev.changes.any { it.pressed })
                    onStepS(routed)
                    return@awaitEachGesture
                }
                var grab: SpGrab? = null; var pinch = false; var moved = false; var snapped = false
                // the press picked a number tag (its measure is now the one being taken): no tap places under it
                var tagged = false
                var lastPinch = 0f; var lastMid = Offset.Zero
                val slideCx = cx; val slideCy = cy
                // the tool as the press began: a tap places the type that was on when the finger went down
                val tool0 = toolS
                val placing = mode == "mark" && tool0 != SurveyPrepPalette.SEL

                fun tapAt(x: Double, y: Double) {
                    if (grab == null && placing && !tagged) placeGrab(x.toFloat(), y.toFloat(), tool0)
                }
                fun act(evs: List<GestureClassifier.Event>) {
                    for (e in evs) when (e) {
                        // the press: what is under it is picked now, as sp.js picks on pointerdown
                        is GestureClassifier.Event.Down -> {
                            val h = if (mode == "lines") hitLines(e.x.toFloat(), e.y.toFloat())
                                    else hitMarks(e.x.toFloat(), e.y.toFloat()) { tagged = true }
                            // a grab follows the finger from the first pixel (no slop dead zone), as sp.js moves it
                            if (h != null) { grab = h; onGrabS(h.sel); gc.grabbed() }
                        }
                        is GestureClassifier.Event.Drag -> {
                            // a drag from empty photo with a type on: placed where the finger went down, then carried
                            if (e.first && grab == null && placing && !tagged) {
                                val sx0 = (e.x - e.dx).toFloat(); val sy0 = (e.y - e.dy).toFloat()
                                placeGrab(sx0, sy0, tool0)?.let { grab = it; snapped = true }   // placing was its one undo step
                            }
                            val g = grab
                            if (g != null) {
                                if (!snapped) { onBeforeChangeS(); snapped = true }
                                val kk = k()
                                val ix = (g.x0 + e.dx / kk).coerceIn(0.0, iw)
                                val iy = (g.y0 + e.dy / kk).coerceIn(0.0, ih)
                                g.move(ix, iy); moved = true
                                loupe = loupeFor(g, ix, iy, Offset(e.x.toFloat(), e.y.toFloat()))
                                onMovedS()
                            } else if (z > 1.001) {
                                cx = slideCx - e.dx / k(); cy = slideCy - e.dy / k(); clampView()
                                tick++
                            }
                        }
                        // A TAP: with Select on, the press already picked what was under it; with a type on and
                        // nothing under it, the type goes where the finger went down. A double tap acts as a tap
                        // here: the Line's area and the Chain give it a meaning of its own (slices 9 and 11).
                        is GestureClassifier.Event.Tap -> tapAt(e.x, e.y)
                        is GestureClassifier.Event.DoubleTap -> tapAt(e.x, e.y)
                        // A LONG PRESS WITH A TYPE ON IS A SLOW TAP: sp.js places on the press however long it is held
                        // (its long-press timer is armed for Select only, sp.js 929 / 1177), so the type goes where
                        // the finger went down and a slide after it carries the mark. With Select on it is
                        // classified and left alone: it opens the mark's menu in slice 8 (sp.js openCtx).
                        is GestureClassifier.Event.LongPress ->
                            if (grab == null && placing && !tagged)
                                placeGrab(e.x.toFloat(), e.y.toFloat(), tool0)?.let { grab = it; snapped = true; gc.grabbed() }
                        is GestureClassifier.Event.DragEnd -> Unit
                        // a second finger: the pinch takes over, and the grab lets go (what it moved stays)
                        is GestureClassifier.Event.Cancel -> { grab = null; loupe = null }
                    }
                }

                act(gc.down(first.position.x.toDouble(), first.position.y.toDouble(), first.uptimeMillis))
                while (true) {
                    // the long press needs the clock: wait for the next event no longer than its deadline
                    val dl = gc.deadline()
                    val ev = if (dl == null) awaitPointerEvent()
                             else withTimeoutOrNull(max(1L, dl - SystemClock.uptimeMillis())) { awaitPointerEvent() }
                    if (ev == null) { act(gc.timeout(dl!!)); continue }
                    val t = ev.changes.maxOf { it.uptimeMillis }
                    val down = ev.changes.filter { it.pressed }
                    if (down.size >= 2) {
                        act(gc.move(down[0].position.x.toDouble(), down[0].position.y.toDouble(), t, down.size))
                        // PINCH AND PAN, as before: zoom about where the fingers started, follow their midpoint
                        val a = down[0].position; val b = down[1].position
                        val d = hypot(a.x - b.x, a.y - b.y).coerceAtLeast(1f); val mid = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
                        if (!pinch) { pinch = true; grab = null; loupe = null; lastPinch = d; lastMid = mid }
                        else {
                            zoomAt(z * d / lastPinch, lastMid.x, lastMid.y)
                            cx -= (mid.x - lastMid.x) / k(); cy -= (mid.y - lastMid.y) / k(); clampView()
                            lastPinch = d; lastMid = mid
                        }
                        tick++
                        ev.changes.forEach { it.consume() }
                    } else if (down.size == 1) {
                        val ch = down[0]; ch.consume()
                        act(gc.move(ch.position.x.toDouble(), ch.position.y.toDouble(), t, 1))
                    } else {
                        // A SYSTEM CANCEL IS NOT A LIFT: Compose delivers ACTION_CANCEL (palm rejection, a system
                        // gesture, the window losing touch) as every pointer up AND already consumed -- the test its
                        // own waitForUpOrCancellation uses. Fed to up() it would be a Tap and place a mark.
                        val cancelled = ev.changes.all { it.isConsumed }
                        ev.changes.forEach { it.consume() }
                        val ch = ev.changes.firstOrNull { it.id == first.id } ?: ev.changes[0]
                        act(if (cancelled) gc.cancel() else gc.up(ch.position.x.toDouble(), ch.position.y.toDouble(), t))
                        break
                    }
                }
                loupe = null
                if (moved) onChangedS() else tick++
            }
        }) {
        Canvas(Modifier.fillMaxSize()) {
            rev; tick; z; cx; cy; loupe; sel; active; show; photoAt; fitAt; chipAt; chipText
            val u = min(size.width, (size.height * iw / ih).toFloat()) / 480f
            val kk = SurveyPrep.fitScale(size.width.toDouble(), size.height.toDouble(), iw, ih) * z
            drawIntoCanvas { cv ->
                val nc = cv.nativeCanvas
                nc.save(); nc.clipRect(0f, 0f, size.width, size.height)
                nc.translate((size.width / 2 - cx * kk).toFloat(), (size.height / 2 - cy * kk).toFloat()); nc.scale(kk.toFloat(), kk.toFloat())
                nc.drawBitmap(img, 0f, 0f, Paint().apply { isFilterBitmap = z < 2.5 })
                nc.restore()
                val m: (Double, Double) -> FloatArray = { x, y -> floatArrayOf(((x - cx) * kk + size.width / 2).toFloat(), ((y - cy) * kk + size.height / 2).toFloat()) }
                fun overlay(c: android.graphics.Canvas, mm: (Double, Double) -> FloatArray, which: String) {
                    if (mode == "lines") SurveyPrepDraw.drawLines(c, tab, prep.surface(tab), frame, mm, u, which == "loupe")
                    else rect?.let { r ->
                        SurveyPrepDraw.drawMarks(c, tab, SurveyPrepDraw.Marks(prep, ab[0], ab[1], r.sq, which, false,
                            sel?.takeIf { it.s == tab }?.id, active, show, size.width, size.height, if (which == "main") tags else null,
                            density.density), mm, u)
                    }
                }
                tags.clear()
                overlay(nc, m, "main")
                // ---- THE FLOATING TRASH (v52): a 36 dp red disc with a white ring beside the picked mark, placed
                // by SurveyPrepTouch.trashSpot so it never covers one of its figures -- a tap on a figure never deletes
                trashAt[0] = null
                // with a type on the disc is not drawn (sp.js 801: "with a drawing tool on, the trash stays in the bar")
                val picked = if (mode == "mark" && tool == SurveyPrepPalette.SEL) sel?.takeIf { it.s == tab }?.id?.let { sid -> prep.surface(tab).marks.firstOrNull { it.id == sid } } else null
                val rr = rect
                if (picked != null && rr != null) {
                    val dpx = density.density
                    val bb = if (SurveyPrep.isPoint(picked.t)) SurveyPrepDraw.spotOf(picked, rr.sq, m, dpx).let { d ->
                        SurveyPrepTouch.Box((d[0] - d[2]).toDouble(), (d[1] - d[2]).toDouble(), (d[0] + d[2]).toDouble(), (d[1] + d[2]).toDouble()) }
                    else { val p0 = m(rr.sq.x(picked.x0), rr.sq.y(picked.y1)); val q0 = m(rr.sq.x(picked.x1), rr.sq.y(picked.y0))
                        SurveyPrepTouch.Box(p0[0].toDouble(), p0[1].toDouble(), q0[0].toDouble(), q0[1].toDouble()) }
                    SurveyPrepTouch.trashSpot(bb, tags.map { it.box }, size.width.toDouble(), size.height.toDouble(), dpx.toDouble())?.let { ts ->
                        val rad = 18f * dpx; val tx = ts.left.toFloat() + rad; val ty = ts.top.toFloat() + rad
                        val pt = Paint(Paint.ANTI_ALIAS_FLAG)
                        pt.style = Paint.Style.FILL; pt.color = android.graphics.Color.rgb(0xE5, 0x48, 0x4D); nc.drawCircle(tx, ty, rad, pt)
                        pt.style = Paint.Style.STROKE; pt.strokeWidth = 2f * dpx; pt.color = android.graphics.Color.WHITE
                        nc.drawCircle(tx, ty, rad - dpx, pt)
                        // a bin: lid, handle, body with two ribs
                        pt.strokeWidth = 1.8f * dpx; pt.strokeCap = Paint.Cap.ROUND
                        val g = 5.5f * dpx
                        nc.drawLine(tx - 1.3f * g, ty - g, tx + 1.3f * g, ty - g, pt)
                        nc.drawLine(tx - .45f * g, ty - 1.4f * g, tx + .45f * g, ty - 1.4f * g, pt)
                        nc.drawRect(tx - g, ty - .7f * g, tx + g, ty + 1.5f * g, pt)
                        nc.drawLine(tx - .35f * g, ty - .3f * g, tx - .35f * g, ty + 1.1f * g, pt)
                        nc.drawLine(tx + .35f * g, ty - .3f * g, tx + .35f * g, ty + 1.1f * g, pt)
                        trashAt[0] = floatArrayOf(tx, ty, rad)
                    }
                }
                // ---- the magnifier(s): three times the photo as it is shown
                loupe?.let { lp ->
                    val dia = 120f * density.density; val sc = kk * 3
                    fun one(p: DoubleArray, left: Float, top: Float) {
                        val ccx = left + dia / 2; val ccy = top + dia / 2
                        nc.save()
                        nc.clipPath(android.graphics.Path().apply { addCircle(ccx, ccy, dia / 2, android.graphics.Path.Direction.CW) })
                        nc.drawColor(android.graphics.Color.BLACK)
                        nc.save(); nc.translate((ccx - p[0] * sc).toFloat(), (ccy - p[1] * sc).toFloat()); nc.scale(sc.toFloat(), sc.toFloat())
                        nc.drawBitmap(img, 0f, 0f, Paint()); nc.restore()
                        overlay(nc, { x, y -> floatArrayOf(((x - p[0]) * sc + ccx).toFloat(), ((y - p[1]) * sc + ccy).toFloat()) }, "loupe")
                        val cross = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = android.graphics.Color.BLACK }
                        nc.drawLine(ccx, top, ccx, top + dia, cross); nc.drawLine(left, ccy, left + dia, ccy, cross)
                        cross.strokeWidth = 1f; cross.color = android.graphics.Color.WHITE
                        nc.drawLine(ccx, top, ccx, top + dia, cross); nc.drawLine(left, ccy, left + dia, ccy, cross)
                        nc.restore()
                        nc.drawCircle(ccx, ccy, dia / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f * density.density; color = android.graphics.Color.WHITE })
                    }
                    // NEVER BETWEEN THE FINGER AND THE PHOTO (v52 item 15; Amit, 13 Oct 2026 23:50 IST: "Many times
                    // magnifier is getting in between fingers and foto place"): above the finger; with no room
                    // above, the top corner farthest from it -- never below it, never under it, never under the
                    // fit button. The rule is SurveyPrepTouch.loupePlace / loupePair, tested in :pano.
                    val gap = 8f * density.density
                    // the buttons on the photo: fit, and the tool chip while it is shown (v52 slice 7, ct52 FX14)
                    val obst = listOfNotNull(fitAt, if (chipText != null) chipAt else null).map { f ->
                        SurveyPrepTouch.Box((f.left - photoAt.x).toDouble(), (f.top - photoAt.y).toDouble(),
                            (f.right - photoAt.x).toDouble(), (f.bottom - photoAt.y).toDouble()) }
                    val dd = density.density.toDouble()
                    if (lp.points.size == 2) {
                        val pr = SurveyPrepTouch.loupePair(lp.finger.y.toDouble(), size.width.toDouble(), size.height.toDouble(),
                            dia.toDouble(), gap.toDouble(), dd, obst)
                        one(lp.points[0], pr.left1.toFloat(), pr.top.toFloat()); one(lp.points[1], pr.left2.toFloat(), pr.top.toFloat())
                    } else {
                        val sp = SurveyPrepTouch.loupePlace(lp.finger.x.toDouble(), lp.finger.y.toDouble(), size.width.toDouble(),
                            size.height.toDouble(), dia.toDouble(), gap.toDouble(), dd, obst)
                        one(lp.points[0], sp.left.toFloat(), sp.top.toFloat())
                    }
                }
            }
        }
        // THE TOOL CHIP (v52 slice 7): "Placing Door · tap the photo", top-left, as wide as the photo less 70 dp so
        // the fit button stays clear; ✕ goes back to Select. Hidden with Select on.
        if (chipText != null) {
            Box(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 62.dp)) {
                SpToolChip(chipText, SurveyPrep.typeOf(tab, tool).colour,
                    Modifier.onGloballyPositioned { chipAt = it.boundsInRoot() }) { onTool(SurveyPrepPalette.SEL) }
            }
        }
        Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) {
            Column(Modifier.onGloballyPositioned { fitAt = it.boundsInRoot() }, horizontalAlignment = Alignment.End) {
                Text("fit", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xAA000000)).clickable {
                        z = 1.0; cx = iw / 2; cy = ih / 2; tick++ }.padding(horizontal = 8.dp, vertical = 5.dp))
                if (z > 1.01) Text("%.1f×".format(z), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xAA000000)).padding(horizontal = 6.dp, vertical = 3.dp))
            }
        }
    }
}

/** One surface's checklist: Mark | Measure | ≈ Photo | Measured (+ D2) | Δ. Tapping a row selects its mark. */
@Composable
private fun SpTable(
    s: String, prep: SurveyPrep.Prep, ab: DoubleArray, rev: Int,
    sel: SpSel?, active: String?, show: String,
    onRow: (SurveyPrep.Mark, String) -> Unit,
    onDelete: (String) -> Unit,
    onFocus: (String) -> Unit,
    onValue: (String, Double?) -> Unit,
    onFire: (String) -> Unit,
    emptyText: String = "Nothing marked on this surface.",
) {
    rev
    val els = prep.surface(s).marks
    if (els.isEmpty()) { Text(emptyText, color = SP_MUTED, fontSize = 12.sp, modifier = Modifier.padding(4.dp)); return }
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("Mark", color = SP_MUTED, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(56.dp))
        Text("Measure", color = SP_MUTED, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("≈ Photo", color = SP_MUTED, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(54.dp), textAlign = TextAlign.End)
        Text("Measured", color = SP_MUTED, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(112.dp), textAlign = TextAlign.Center)
        Text("Δ", color = SP_MUTED, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
    }
    for (e in els) {
        val ms = SurveyPrep.measures(s, e, ab[0], ab[1])
        val selM = sel?.id == e.id
        Row(Modifier.fillMaxWidth().background(if (selM) Color(0xFF2A2614) else Color.Transparent)) {
            Column(Modifier.width(56.dp).padding(3.dp)) {
                Text(e.code, color = SP_GOLD, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(SurveyPrep.typeOf(s, e.t).name, color = SP_MUTED, fontSize = 10.sp, lineHeight = 11.sp)
                Text("Delete", color = Color(0xFFFF9B7A), fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(6.dp)).border(1.dp, Color(0x88FF9B7A), RoundedCornerShape(6.dp))
                        .clickable { onDelete(e.id) }.padding(horizontal = 6.dp, vertical = 3.dp))
            }
            Column(Modifier.weight(1f)) {
                for (mm in ms) {
                    val code = e.code + "." + mm.key; val a = prep.actual[code]; val act = active == code
                    val d = if (a != null && mm.est != null) a - mm.est!! else null
                    Row(Modifier.fillMaxWidth().background(if (act) Color(0xFF3D3410) else Color.Transparent)
                        .clickable { onRow(e, code) }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text((if (act) "▶ " else "") + mm.what, color = SP_FG, fontSize = 11.sp, lineHeight = 12.sp, modifier = Modifier.weight(1f))
                        Text(if (show == "actual") "" else (mm.est?.let { "≈" + it.roundToInt() } ?: "site only"), color = Color(0xFFCFD3CB),
                            fontSize = if (mm.est == null) 9.sp else 11.sp, textAlign = TextAlign.End, modifier = Modifier.width(54.dp))
                        Row(Modifier.width(112.dp).padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            SpMmField(code, a, act, Modifier.weight(1f), onFocus = { onFocus(code) }) { v -> onValue(code, v) }
                            Spacer(Modifier.width(3.dp))
                            SpD2(Modifier, small = true) { onFire(code) }
                        }
                        Text(if (show == "both" && d != null) SurveyPrepDraw.sgn(d) else "",
                            color = if (d != null && kotlin.math.abs(d) <= SurveyPrep.TOL) SP_OK else SP_AMB,
                            fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.End, modifier = Modifier.width(36.dp))
                    }
                }
            }
        }
        if (SurveyPrep.outOf(e, ab[0], ab[1]))
            Text(SurveyPrep.outNote(e.code), color = SP_OUT, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2F28)))
    }
}

/** A reading box: typed mm, kept on Done or when the box loses focus; cleared to remove a reading. */
@Composable
private fun SpMmField(code: String, value: Double?, active: Boolean, modifier: Modifier, onFocus: () -> Unit, onCommit: (Double?) -> Unit) {
    val focus = LocalFocusManager.current
    var text by remember(code, value) { mutableStateOf(value?.roundToInt()?.toString() ?: "") }
    var focused by remember { mutableStateOf(false) }
    fun commit() {
        val v = text.toDoubleOrNull()
        if (v != null && v > 0) { if (v != value) onCommit(v) }
        else if (text.isBlank() && value != null) onCommit(null)
    }
    BasicTextField(
        value = text, onValueChange = { t -> text = t.filter { it.isDigit() }.take(6) }, singleLine = true,
        textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.End),
        cursorBrush = SolidColor(SP_GOLD),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit(); focus.clearFocus() }),
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(Color.Black)
            .border(1.5.dp, if (active) SP_GOLD else Color(0xFF3A3E35), RoundedCornerShape(6.dp))
            .onFocusChanged { fs ->
                if (fs.isFocused) { focused = true; onFocus() }
                else if (focused) { focused = false; commit() }
            }.padding(horizontal = 5.dp, vertical = 5.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterEnd) {
                if (text.isEmpty()) Text("mm", color = SP_DIM, fontSize = 12.sp)
                inner()
            }
        })
}

/** The laser button: a red dot and D2. */
@Composable
private fun SpD2(modifier: Modifier, label: String = "D2", small: Boolean = false, onClick: () -> Unit) {
    Row(modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF3A1512)).border(1.dp, Color(0xFFFF5A4A), RoundedCornerShape(8.dp))
        .clickable { onClick() }.padding(horizontal = if (small) 5.dp else 8.dp, vertical = if (small) 5.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFFF3B2F)))
        Spacer(Modifier.width(3.dp))
        Text(label, color = Color(0xFFFFB3A8), fontSize = if (small) 10.sp else 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun SpPill(text: String, on: Boolean, modifier: Modifier, square: Boolean = false, outline: Boolean = false,
                   compact: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(if (square) 10.dp else 50.dp)
    val bg = if (on) (if (outline) Color(0xFF3A3E35) else SP_GOLD) else SP_CHIP
    val fg = if (on) (if (outline) Color.White else Color(0xFF1A1A12)) else SP_MUTED
    Box(modifier.clip(shape).background(bg).let { if (on && outline) it.border(1.5.dp, SP_GOLD, shape) else it }
        .clickable { onClick() }.padding(vertical = if (compact) 4.dp else 8.dp), contentAlignment = Alignment.Center) {
        Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1,
            lineHeight = if (compact) 14.sp else TextUnit.Unspecified)   // compact: 4 + 14 + 4 = 22 dp, not the theme's 24 sp line
    }
}

@Composable
private fun SpButton(text: String, modifier: Modifier, border: Color, compact: Boolean = false, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(7.dp)).background(Color.Black).border(1.5.dp, border, RoundedCornerShape(7.dp))
        .clickable { onClick() }.padding(vertical = if (compact) 4.dp else 8.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
            lineHeight = if (compact) 14.sp else TextUnit.Unspecified)   // compact: 4 + 14 + 4 = 22 dp, not the theme's 24 sp line
    }
}

@Composable
private fun SpText(text: String, color: Color, sizeSp: Int, modifier: Modifier) {
    Text(text, color = color, fontSize = sizeSp.sp, modifier = modifier)
}

/**
 * One face tab (v52 slice 6): plain upper-case text, no fill -- 11 sp, weight 500, letter spacing 0.5, #AEB3AA; the
 * open one white and bold over a 3 dp gold bar inset 12 % each side; a bounded face carries ' ✓' in green.
 * The 'Ref w × h' subtitle of a squared face arrives with the reference (slice 12).
 */
@Composable
private fun SpTab(name: String, on: Boolean, tick: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clickable(role = Role.Tab) { onClick() }.semantics { selected = on }, contentAlignment = Alignment.Center) {
        Text(buildAnnotatedString {
                append(name.uppercase())
                if (tick) withStyle(SpanStyle(color = SP_OK, fontWeight = FontWeight.Bold)) { append(" ✓") }
            },
            color = if (on) Color.White else SP_TAB, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp,
            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
            modifier = Modifier.padding(horizontal = 2.dp))
        if (on) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.76f).height(3.dp)
            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(SP_GOLD))
    }
}

/** A passing note ("Tap a measurement first", a refusal), over the bottom of the photo or the list for 2.6 s. */
@Composable
private fun SpNote(text: String, modifier: Modifier) {
    Text(text, color = SP_AMB, fontSize = 12.sp, lineHeight = 15.sp,
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xE6111311))
            .border(1.dp, SP_LINE_BORDER, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp))
}

/**
 * THE STEP STRIP'S TOUCH AREA (v52 slice 6): a press here that StepRoute gives to a step (within 48 dp under its top)
 * opens that step when it lifts, and is nobody else's; any other press is left alone. [at] is this element's top-left
 * in root px, read on each press. A press something under it has already taken -- a Show chip's own tap, which
 * consumes its down -- is never routed: awaitFirstDown passes over a consumed down (ct52 FX3).
 */
private suspend fun PointerInputScope.routeToStep(at: () -> Offset, route: (Float, Float) -> String?, go: (String) -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val o = at()
        val k = route(down.position.x + o.x, down.position.y + o.y) ?: return@awaitEachGesture
        down.consume()
        do {
            val ev = awaitPointerEvent()
            ev.changes.forEach { it.consume() }
        } while (ev.changes.any { it.pressed })
        go(k)
    }
}

private val SP_BG = Color(0xFF111311)
private val SP_FG = Color(0xFFECEEE8)
private val SP_MUTED = Color(0xFF9AA096)
private val SP_DIM = Color(0xFF555A50)
private val SP_CHIP = Color(0xFF23261F)
private val SP_SHEET = Color(0xFF1A1C18)
private val SP_GOLD = Color(0xFFE0B040)
private val SP_OK = Color(0xFF7DFF8A)
private val SP_AMB = Color(0xFFFFC46B)
private val SP_LINE_BORDER = Color(0xFF3A3E35)
/** The face tabs and the step strip (v52 slice 6): tab text, a step's ring, a past step, the joins. */
private val SP_TAB = Color(0xFFAEB3AA)
private val SP_RING = Color(0xFF7E847A)
private val SP_PAST = Color(0xFFB9A35A)
private val SP_JOIN = Color(0xFF4A4E45)
/** A mark past its face, and a refused size (v51). */
private val SP_OUT = Color(0xFFFF5C5C)
