package com.malletcrafts.sitephotos

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.malletcrafts.sitephotos.pano.SurveyPrep
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
 *    the site reading from the DISTO D2 or typed.
 *  - Lines, marks and readings save after each change and come back when
 *    the screen is opened again (SurveyPrepStore).
 *
 * The laser L, W and H come from the room capture and are fixed here. All
 * maths is SurveyPrep in :pano, where it is tested; drawing is SurveyPrepDraw.
 */
@Composable
internal fun SurveyPrepDialog(
    session: FaceWriter.Session,
    title: String,
    lengthMm: Double,
    widthMm: Double,
    heightMm: Double,
    onDone: (SurveyPrep.Prep) -> Unit,
    /** Back without Save: the work is kept on the phone either way, and handed back as it stands. */
    onCancel: (SurveyPrep.Prep) -> Unit,
) {
    val context = LocalContext.current
    val store = remember { SurveyPrepStore(context) }
    val id = session.deviceId
    val lL = lengthMm; val lW = widthMm; val lH = heightMm

    val loaded = remember { store.load(id) }
    var prep by remember { mutableStateOf(loaded ?: SurveyPrep.fresh(lL, lW, lH)) }
    var rev by remember { mutableIntStateOf(0) }
    var tab by remember { mutableStateOf("front") }
    var mode by remember { mutableStateOf("lines") }
    var show by remember { mutableStateOf("both") }
    var sel by remember { mutableStateOf<SpSel?>(null) }
    var active by remember { mutableStateOf<String?>(null) }
    val hist = remember { mutableStateListOf<SurveyPrep.Prep>() }
    var note by remember { mutableStateOf<String?>(null) }
    var savedMsg by remember { mutableStateOf(if (loaded != null) "Restored ✓" else "Nothing saved yet") }
    var dirty by remember { mutableIntStateOf(0) }
    var armed by remember { mutableStateOf(false) }
    val views = remember { mutableStateMapOf<String, Bitmap>() }
    val rects = remember { mutableStateMapOf<String, SpRect>() }
    LaunchedEffect(note) { if (note != null) { delay(2600); note = null } }
    LaunchedEffect(armed) { if (armed) { delay(4000); armed = false } }

    fun ab(s: String) = SurveyPrep.size(s, lL, lW, lH)
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
        val ok = store.save(id, prep, lL, lW, lH)
        if (ok) unsaved[0] = false
        savedMsg = if (ok) "Saved ✓ " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) else "Not saved: did not read back"
    }
    // Closing the screen inside that moment must not lose the last change.
    DisposableEffect(Unit) { onDispose { if (unsaved[0]) store.save(id, prep, lL, lW, lH) } }

    fun snapshot() { hist.add(prep.copy()); if (hist.size > 40) hist.removeAt(0) }
    fun undo() {
        if (hist.isEmpty()) return
        val old = hist.removeAt(hist.size - 1)
        // Readings are not geometry: the ones taken since stay, and a reading a delete took away comes back with its mark.
        val merged = old.actual.toMutableMap().apply { putAll(prep.actual) }
        val back = SurveyPrep.Prep(old.surfaces, mutableMapOf())
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

    // ---- pictures: the six views once, and each surface squared whenever its lines change
    LaunchedEffect(Unit) {
        for (s in listOf(tab) + SurveyPrep.SURFACES.filter { it != tab }) {
            val bmp = withContext(Dispatchers.Default) { FaceWriter.bitmapOf(SurveyPrep.cutView(session.previewPano, s)) }
            views[s] = bmp
        }
    }
    val need = when {
        tab == "list" -> SurveyPrep.SURFACES.filter { prep.surface(it).done() }
        mode == "mark" -> listOf(tab)
        else -> emptyList()
    }
    val wanted = need.mapNotNull { s -> keyOf(s)?.let { s to it } }
    LaunchedEffect(wanted.joinToString("#") { it.first + "=" + it.second }) {
        for ((s, k) in wanted) {
            if (rects[s]?.key == k) continue
            val f = frameOf(s) ?: continue
            val sq = SurveyPrep.Square.screen(f.a, f.b)
            val bmp = withContext(Dispatchers.Default) { FaceWriter.bitmapOf(SurveyPrep.render(session.previewPano, s, f, sq)) }
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
    fun addMark(t: String) {
        val s = tab; val a = ab(s); snapshot()
        val e = prep.addMark(s, t, a[0], a[1], java.util.UUID.randomUUID().toString().take(8))
        sel = SpSel(s, id = e.id); active = prep.nextOpen(s, e, null, a[0], a[1])
        changed()
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
    fun showTab(t: String) { tab = t; sel = null; rev++ }
    fun setMode(m: String) {
        if (m == "mark" && frameOf(tab) == null) { note = "These four lines do not make a closed shape yet"; return }
        mode = m; sel = null; rev++
    }

    val done = SurveyPrep.SURFACES.count { prep.surface(it).done() }

    // The phone's Back is the screen's own ←: nothing is lost, the work is already saved.
    Dialog(onDismissRequest = { onCancel(prep.copy()) },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize(), color = SP_BG) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp)) {
                rev    // read, so every model change recomposes this column
                // ---- bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SpText("←", SP_FG, 20, Modifier.clip(CircleShape).clickable { onCancel(prep.copy()) }.padding(8.dp))
                    Text("$title · survey prep", Modifier.weight(1f), color = SP_FG, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(savedMsg, color = SP_OK, fontSize = 10.sp, textAlign = TextAlign.End, modifier = Modifier.width(96.dp))
                    SpText("↶", if (hist.isEmpty()) SP_DIM else SP_FG, 20,
                        Modifier.clip(CircleShape).clickable(enabled = hist.isNotEmpty()) { undo() }.padding(8.dp))
                }
                // ---- the laser sizes, fixed
                Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((k, v, cap) in listOf(Triple("L", lL, "front→back"), Triple("W", lW, "left→right"), Triple("H", lH, "floor→ceiling"))) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(k, color = SP_FG, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.width(4.dp))
                                Text(v.roundToInt().toString(), color = SP_FG, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                            }
                            Text(cap, color = SP_MUTED, fontSize = 10.sp)
                        }
                    }
                }
                Text("Laser D2 readings from the room capture, in mm. Fixed here.", color = SP_MUTED, fontSize = 11.sp,
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))

                // ---- tabs
                for (row in listOf(listOf("front", "right", "back", "left"), listOf("floor", "ceil", "list"))) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (t in row) {
                            val lab = if (t == "list") "List" else SurveyPrep.TAB.getValue(t) + if (prep.surface(t).done()) " ✓" else ""
                            SpPill(lab, tab == t, Modifier.weight(1f)) { showTab(t) }
                        }
                    }
                }
                if (tab != "list") {
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SpPill("1 · Lines", mode == "lines", Modifier.weight(1f), square = true) { setMode("lines") }
                        SpPill("2 · Mark", mode == "mark", Modifier.weight(1f), square = true) { setMode("mark") }
                    }
                }
                if (tab != "list" && mode == "mark") {
                    for (chunk in SurveyPrep.types(tab).chunked(3)) {
                        Row(Modifier.fillMaxWidth().padding(bottom = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            for (ty in chunk) {
                                Box(Modifier.weight(1f).clip(RoundedCornerShape(50)).border(1.5.dp, Color(SurveyPrepDraw.argb(ty.colour)), RoundedCornerShape(50))
                                    .clickable { addMark(ty.letter) }.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                    Text("+ ${ty.name}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                }
                            }
                            repeat(3 - chunk.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                // the photo-vs-measured toggle belongs to marks only (Amit, 2026-10-10: "lines should not show that")
                if (tab == "list" || mode == "mark") {
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Show", color = SP_MUTED, fontSize = 11.sp)
                        for ((k, lab) in listOf("derived" to "≈ Photo", "actual" to "Measured", "both" to "Both")) {
                            SpPill(lab, show == k, Modifier.weight(1f), outline = true) { show = k; rev++ }
                        }
                    }
                }

                if (tab != "list") {
                    val ab0 = ab(tab); val tags = SurveyPrep.sizeTag(tab)
                    // the surface's title is a caption ABOVE the photo, never on it
                    Text(if (mode == "lines") "${SurveyPrep.NAME[tab]} · set its four lines"
                         else "${SurveyPrep.NAME[tab]} · ${tags.first} ${ab0[0].roundToInt()} × ${tags.second} ${ab0[1].roundToInt()}",
                        color = SP_FG, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp, bottom = 6.dp))
                    val img: Bitmap? = if (mode == "lines") views[tab] else rectOf(tab)?.bmp
                    if (img == null) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1.2f).clip(RoundedCornerShape(12.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        SpPhoto(
                            img = img, tab = tab, mode = mode, rev = rev,
                            prep = prep, ab = ab0, frame = frameOf(tab), rect = rectOf(tab),
                            sel = sel, active = active, show = show,
                            onSelect = { s, e, measure -> selectMark(s, e, measure) },
                            onGrab = { g -> grabbed(g) },
                            onBeforeChange = { snapshot() },
                            onMoved = { rev++ },
                            onChanged = { changed() })
                    }

                    // ---- under the photo: the selected mark's buttons, then the measurement being taken
                    val sm = sel?.takeIf { mode == "mark" && it.s == tab && it.id != null }?.let { sl -> prep.surface(tab).marks.firstOrNull { it.id == sl.id } }
                    if (sm != null) {
                        val p = sel!!.part
                        val edge = p != "body"
                        val horiz = edge && (p.contains('t') || p.contains('b')) && !(p.contains('l') || p.contains('r'))
                        val vert = edge && (p.contains('l') || p.contains('r')) && !(p.contains('t') || p.contains('b'))
                        val what = when { p == "body" -> "move"; p.length > 1 -> "corner"
                            else -> mapOf("l" to "left edge", "r" to "right edge", "t" to "top edge", "b" to "bottom edge")[p] ?: p }
                        Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${sm.code}\n$what", color = SP_GOLD, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(52.dp))
                            val bs: List<Triple<String, Double, Double>> = when {
                                horiz -> listOf(Triple("▼5", 0.0, -5.0), Triple("▼1", 0.0, -1.0), Triple("▲1", 0.0, 1.0), Triple("▲5", 0.0, 5.0))
                                vert -> listOf(Triple("◀5", -5.0, 0.0), Triple("◀1", -1.0, 0.0), Triple("1▶", 1.0, 0.0), Triple("5▶", 5.0, 0.0))
                                else -> listOf(Triple("◀", -1.0, 0.0), Triple("▶", 1.0, 0.0), Triple("▼", 0.0, -1.0), Triple("▲", 0.0, 1.0))
                            }
                            for ((lab, dx, dy) in bs) SpButton(lab, Modifier.weight(1f), SP_GOLD) { nudgeMark(dx, dy) }
                            SpButton("Delete", Modifier.weight(1.4f), SP_GOLD) { deleteMark(sm.id) }
                        }
                    }
                    // THE MEASUREMENT BEING TAKEN, right under the photo, so the list need not be scrolled to while lasering
                    val act = active
                    val am = act?.let { prep.markOf(it) }?.takeIf { mode == "mark" && it.first == tab }
                    val mm = am?.let { (s, e) -> SurveyPrep.measures(s, e, ab(s)[0], ab(s)[1]).firstOrNull { e.code + "." + it.key == act } }
                    if (am != null && mm != null && act != null) {
                        Row(Modifier.fillMaxWidth().background(Color(0xFF2A2410)).border(1.5.dp, SP_GOLD).padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("▶ ${am.second.code} ${mm.what}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(mm.est?.let { "≈" + it.roundToInt() } ?: "site only", color = Color(0xFFCFD3CB), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            SpMmField(act, prep.actual[act], true, Modifier.width(72.dp), onFocus = { active = act }) { v ->
                                if (v != null) reading(act, v) else { prep.actual.remove(act); changed() } }
                            SpD2(Modifier) { fire() }
                        }
                    }

                    // ---- 1 · Lines: EVERY LINE HAS ITS OWN CONTROLS, always there (Amit, 2026-10-10: "why not every line ...
                    // have its own control. currently i have to select it first ... lines are one time settings")
                    if (mode == "lines") {
                        val names = SurveyPrep.lineNames(tab)
                        Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (k in SurveyPrep.SLOTS) {
                                val col = Color(SurveyPrepDraw.argb(SurveyPrepDraw.LINE_COLOUR.getValue(k)))
                                val h = k == "bottom" || k == "top"
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(Modifier.width(78.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(col))
                                        Spacer(Modifier.width(4.dp))
                                        Text(names.getValue(k), color = col, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 12.sp)
                                    }
                                    val bs = if (h) listOf("▼5" to -5.0, "▼1" to -1.0, "▲1" to 1.0, "▲5" to 5.0)
                                             else listOf("◀5" to -5.0, "◀1" to -1.0, "1▶" to 1.0, "5▶" to 5.0)
                                    for ((lab, v) in bs) SpButton(lab, Modifier.weight(1f), SP_LINE_BORDER) { nudgeLine(k, v, 0.0) }
                                    SpButton("↺", Modifier.weight(1f), SP_LINE_BORDER) { nudgeLine(k, 0.0, -0.1) }
                                    SpButton("↻", Modifier.weight(1f), SP_LINE_BORDER) { nudgeLine(k, 0.0, 0.1) }
                                }
                            }
                        }
                    } else {
                        // ---- this surface's own list
                        Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).background(SP_SHEET).padding(6.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("${SurveyPrep.NAME[tab]} · its list", color = SP_FG, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                if (am != null && act != null) SpD2(Modifier, "D2 → $act") { fire() }
                            }
                            SpTable(tab, prep, ab0, rev, sel, active, show,
                                onRow = { e, code -> selectMark(tab, e, code) },
                                onDelete = { deleteMark(it) },
                                onFocus = { active = it },
                                onValue = { code, v -> if (v != null) reading(code, v) else { prep.actual.remove(code); changed() } },
                                onFire = { code -> active = code; fire() })
                        }
                    }
                } else {
                    // ---- the room at once: every surface squared, with codes and dimensions, and its table
                    for (s in SurveyPrep.SURFACES) {
                        val a = ab(s); val tg = SurveyPrep.sizeTag(s)
                        Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp)).background(SP_SHEET).padding(6.dp)) {
                            Row(Modifier.fillMaxWidth().padding(2.dp)) {
                                Text(SurveyPrep.NAME.getValue(s), color = SP_FG, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Text("${tg.first} ${a[0].roundToInt()} × ${tg.second} ${a[1].roundToInt()} · laser", color = SP_MUTED, fontSize = 12.sp)
                            }
                            val r = if (prep.surface(s).done()) rectOf(s) else null
                            if (!prep.surface(s).done()) Text("Set its four lines first.", color = SP_MUTED, fontSize = 12.sp, modifier = Modifier.padding(4.dp))
                            else if (r == null) CircularProgressIndicator(Modifier.padding(12.dp))
                            else {
                                Canvas(Modifier.fillMaxWidth().aspectRatio(r.sq.width.toFloat() / r.sq.height).clip(RoundedCornerShape(8.dp))) {
                                    rev; show; active
                                    val sc = size.width / r.sq.width
                                    drawIntoCanvas { cv ->
                                        val nc = cv.nativeCanvas
                                        nc.save(); nc.scale(sc, sc); nc.drawBitmap(r.bmp, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG)); nc.restore()
                                        SurveyPrepDraw.drawMarks(nc, s, SurveyPrepDraw.Marks(prep, a[0], a[1], r.sq, "sheet", true, null, active, show,
                                            size.width, size.height, null), { x, y -> floatArrayOf((x * sc).toFloat(), (y * sc).toFloat()) }, sc)
                                    }
                                }
                                SpTable(s, prep, a, rev, sel, active, show,
                                    onRow = { e, code -> selectMark(s, e, code) },
                                    onDelete = { deleteMark(it) },
                                    onFocus = { active = it },
                                    onValue = { code, v -> if (v != null) reading(code, v) else { prep.actual.remove(code); changed() } },
                                    onFire = { code -> active = code; fire() })
                            }
                        }
                    }
                }

                // ---- status, save, start over
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
                    else -> "Squared to the laser size. Add what is on this ${if (SurveyPrep.isPlan(tab)) "surface" else "wall"}; tap a mark to edit it. "
                }
                Column(Modifier.padding(6.dp)) {
                    Text(statusText, color = SP_MUTED, fontSize = 12.sp)
                    if (tab != "list" && mode == "lines" && frameOf(tab) == null)
                        Text("These four lines do not make a closed shape.", color = SP_AMB, fontSize = 12.sp)
                    Text("$done of 6 surfaces bounded. Save needs all six; marks and readings never block it.", color = SP_MUTED, fontSize = 11.sp)
                    note?.let { Text(it, color = SP_AMB, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
                }
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (done == 6) SP_GOLD else Color(0xFF3A3E35))
                    .clickable(enabled = done == 6) { onDone(prep.copy()) }.padding(12.dp), contentAlignment = Alignment.Center) {
                    Text("Save survey prep", color = if (done == 6) Color(0xFF1A1A12) else Color(0xFF7D8278), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, if (armed) SP_AMB else Color(0xFF3A3E35), RoundedCornerShape(12.dp))
                    .clickable {
                        if (!armed) armed = true
                        else {
                            armed = false; hist.clear(); sel = null; active = null
                            prep = SurveyPrep.fresh(lL, lW, lH); changed()
                        }
                    }.padding(9.dp), contentAlignment = Alignment.Center) {
                    Text(if (armed) "Tap again to clear every line, mark and reading" else "Start over",
                        color = if (armed) SP_AMB else SP_MUTED, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(24.dp))
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
 * a line swings.
 */
@Composable
private fun SpPhoto(
    img: Bitmap, tab: String, mode: String, rev: Int,
    prep: SurveyPrep.Prep, ab: DoubleArray, frame: SurveyPrep.Frame?, rect: SpRect?,
    sel: SpSel?, active: String?, show: String,
    onSelect: (String, SurveyPrep.Mark, String?) -> Unit,
    onGrab: (SpSel) -> Unit,
    onBeforeChange: () -> Unit,
    onMoved: () -> Unit,
    onChanged: () -> Unit,
) {
    val density = LocalDensity.current
    val iw = img.width.toDouble(); val ih = img.height.toDouble()
    // The gesture below outlives recompositions (it restarts only for a new picture, surface, mode or
    // squaring), so everything it reads that can change under it is read through these.
    val prepS by rememberUpdatedState(prep); val selS by rememberUpdatedState(sel)
    val rectS by rememberUpdatedState(rect); val abS by rememberUpdatedState(ab)
    val onSelectS by rememberUpdatedState(onSelect); val onGrabS by rememberUpdatedState(onGrab)
    val onBeforeChangeS by rememberUpdatedState(onBeforeChange); val onMovedS by rememberUpdatedState(onMoved)
    val onChangedS by rememberUpdatedState(onChanged)
    var z by remember(img) { mutableStateOf(1.0) }
    var cx by remember(img) { mutableStateOf(iw / 2) }
    var cy by remember(img) { mutableStateOf(ih / 2) }
    var loupe by remember { mutableStateOf<SpLoupe?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    val tags = remember { mutableListOf<SurveyPrepDraw.Tag>() }
    var boxW by remember { mutableStateOf(1f) }

    fun k() = boxW / iw * z
    fun toS(x: Double, y: Double): FloatArray { val kk = k(); val bh = boxW * ih / iw
        return floatArrayOf(((x - cx) * kk + boxW / 2).toFloat(), ((y - cy) * kk + bh / 2).toFloat()) }
    fun fromS(sx: Float, sy: Float): DoubleArray { val kk = k(); val bh = boxW * ih / iw
        return doubleArrayOf((sx - boxW / 2) / kk + cx, (sy - bh / 2) / kk + cy) }
    fun clampView() {
        z = z.coerceIn(1.0, 8.0); val hw = iw / (2 * z); val hh = ih / (2 * z)
        cx = cx.coerceIn(hw, iw - hw); cy = cy.coerceIn(hh, ih - hh)
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

    fun hitMarks(sx: Float, sy: Float): SpGrab? {
        val r = rectS ?: return null; val sq = r.sq; val ab = abS
        // TAP A NUMBER ON THE PHOTO TO MEASURE IT (Amit, 2026-10-10)
        val th = tags.filter { it.contains(sx, sy) }.minByOrNull { it.dist(sx, sy) }
        if (th != null) {
            prepS.markOf(th.code)?.let { (s, e) -> onSelectS(s, e, th.code) }
            return null
        }
        val els = prepS.surface(tab).marks; val at = fromS(sx, sy); val tol = with(density) { 16.dp.toPx() } / k()
        for (i in els.indices.reversed()) {
            val e = els[i]; val x0 = sq.x(e.x0); val x1 = sq.x(e.x1); val yT = sq.y(e.y1); val yB = sq.y(e.y0)
            if (at[0] < x0 - tol || at[0] > x1 + tol || at[1] < yT - tol || at[1] > yB + tol) continue
            // MOVE FREELY, THEN SET ITS BOUNDARIES (Amit, 2026-10-10): anywhere on a mark moves it; only the
            // selected mark shows tabs, and only a tab resizes.
            var part = "body"
            if (selS?.id == e.id) {
                val xm = (x0 + x1) / 2; val ym = (yT + yB) / 2
                val tabsAt = listOf(Triple("l", x0, ym), Triple("r", x1, ym), Triple("t", xm, yT), Triple("b", xm, yB),
                    Triple("lt", x0, yT), Triple("rt", x1, yT), Triple("lb", x0, yB), Triple("rb", x1, yB))
                tabsAt.map { (pp, tx, ty) -> pp to hypot(at[0] - tx, at[1] - ty) }.filter { it.second < tol }.minByOrNull { it.second }?.let { part = it.first }
            } else if (at[0] < x0 || at[0] > x1 || at[1] < yT || at[1] > yB) continue
            val start = e.copy(); val mx0 = sq.mmX(at[0]); val my0 = sq.mmY(at[1])
            return SpGrab(at[0], at[1], SpSel(tab, id = e.id, part = part), null) { jx, jy ->
                SurveyPrep.moveMark(e, start, part, sq.mmX(jx) - mx0, sq.mmY(jy) - my0, ab[0], ab[1])
                SurveyPrep.snapMark(e, part, ab[0], ab[1])
            }
        }
        return null
    }

    fun loupeFor(g: SpGrab, ix: Double, iy: Double, finger: Offset): SpLoupe {
        val j = g.focusSlot?.let { SurveyPrep.junctions(tab, prepS.surface(tab).lines, it) }
        return SpLoupe(j ?: listOf(doubleArrayOf(ix, iy)), finger)
    }

    Box(Modifier.fillMaxWidth().aspectRatio((iw / ih).toFloat()).clip(RoundedCornerShape(12.dp)).background(Color.Black)
        .onSizeChanged { boxW = it.width.toFloat() }
        .pointerInput(img, tab, mode, rect) {
            awaitEachGesture {
                val first = awaitFirstDown()
                first.consume()
                var grab: SpGrab? = null; var pinch = false; var moved = false; var snapped = false
                val h = if (mode == "lines") hitLines(first.position.x, first.position.y) else hitMarks(first.position.x, first.position.y)
                if (h != null) { grab = h; onGrabS(h.sel) }
                var lastPinch = 0f; var lastMid = Offset.Zero; val sx0 = first.position.x; val sy0 = first.position.y
                val slideCx = cx; val slideCy = cy
                do {
                    val ev = awaitPointerEvent()
                    val down = ev.changes.filter { it.pressed }
                    if (down.size >= 2) {
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
                    } else if (down.size == 1 && !pinch) {
                        val ch = down[0]; ch.consume()
                        val g = grab
                        if (g != null) {
                            if (!snapped) { onBeforeChangeS(); snapped = true }
                            val kk = k()
                            val ix = (g.x0 + (ch.position.x - sx0) / kk).coerceIn(0.0, iw)
                            val iy = (g.y0 + (ch.position.y - sy0) / kk).coerceIn(0.0, ih)
                            g.move(ix, iy); moved = true
                            loupe = loupeFor(g, ix, iy, ch.position)
                            onMovedS()
                        } else if (z > 1.001) {
                            cx = slideCx - (ch.position.x - sx0) / k(); cy = slideCy - (ch.position.y - sy0) / k(); clampView()
                            tick++
                        }
                    }
                } while (ev.changes.any { it.pressed })
                loupe = null
                if (moved) onChangedS() else tick++
            }
        }) {
        Canvas(Modifier.fillMaxSize()) {
            rev; tick; z; cx; cy; loupe; sel; active; show
            val u = size.width / 480f
            val kk = size.width / iw * z
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
                            sel?.takeIf { it.s == tab }?.id, active, show, size.width, size.height, if (which == "main") tags else null), mm, u)
                    }
                }
                tags.clear()
                overlay(nc, m, "main")
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
                    val gap = 8f * density.density
                    if (lp.points.size == 2) {
                        val top = if (lp.finger.y < dia + 60f * density.density) size.height - dia - gap else gap
                        one(lp.points[0], gap, top); one(lp.points[1], size.width - dia - gap, top)
                    } else {
                        var top = lp.finger.y - dia - 40f * density.density
                        if (top < 4f) top = lp.finger.y + 40f * density.density
                        top = top.coerceIn(0f, max(0f, size.height - dia))
                        val left = min(size.width - dia - 4f, max(4f, lp.finger.x - dia / 2))
                        one(lp.points[0], left, top)
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) {
            Column(horizontalAlignment = Alignment.End) {
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
) {
    rev
    val els = prep.surface(s).marks
    if (els.isEmpty()) { Text("Nothing marked on this surface.", color = SP_MUTED, fontSize = 12.sp, modifier = Modifier.padding(4.dp)); return }
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
private fun SpPill(text: String, on: Boolean, modifier: Modifier, square: Boolean = false, outline: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(if (square) 10.dp else 50.dp)
    val bg = if (on) (if (outline) Color(0xFF3A3E35) else SP_GOLD) else SP_CHIP
    val fg = if (on) (if (outline) Color.White else Color(0xFF1A1A12)) else SP_MUTED
    Box(modifier.clip(shape).background(bg).let { if (on && outline) it.border(1.5.dp, SP_GOLD, shape) else it }
        .clickable { onClick() }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun SpButton(text: String, modifier: Modifier, border: Color, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(7.dp)).background(Color.Black).border(1.5.dp, border, RoundedCornerShape(7.dp))
        .clickable { onClick() }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun SpText(text: String, color: Color, sizeSp: Int, modifier: Modifier) {
    Text(text, color = color, fontSize = sizeSp.sp, modifier = modifier)
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
