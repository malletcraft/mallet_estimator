package com.malletcrafts.interiorestimate

import android.graphics.Bitmap
import android.graphics.Paint
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.malletcrafts.interiorestimate.pano.GestureClassifier
import com.malletcrafts.interiorestimate.pano.MarkLife
import com.malletcrafts.interiorestimate.pano.MarkSheet
import com.malletcrafts.interiorestimate.pano.MmBox
import com.malletcrafts.interiorestimate.pano.Pano360
import com.malletcrafts.interiorestimate.pano.Panorama
import com.malletcrafts.interiorestimate.pano.PhaseTools
import com.malletcrafts.interiorestimate.pano.PlanSave
import com.malletcrafts.interiorestimate.pano.SheetMarks
import com.malletcrafts.interiorestimate.pano.StepRoute
import com.malletcrafts.interiorestimate.pano.SurveyChain
import com.malletcrafts.interiorestimate.pano.SurveyExport
import com.malletcrafts.interiorestimate.pano.SurveyGuide
import com.malletcrafts.interiorestimate.pano.SurveyMeas
import com.malletcrafts.interiorestimate.pano.SurveyPlan
import com.malletcrafts.interiorestimate.pano.SurveyPrep
import com.malletcrafts.interiorestimate.pano.SurveyPrepEdit
import com.malletcrafts.interiorestimate.pano.SurveyPrepFile
import com.malletcrafts.interiorestimate.pano.SurveyPrepPalette
import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch
import com.malletcrafts.interiorestimate.pano.SurveyRef
import com.malletcrafts.interiorestimate.pano.UploadNote
import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Msg
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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
 *    photo (v52 slice 7, complete in slice 11): Select, Tape, Line, Chain,
 *    three types, More ▸; a type stays on and every tap on empty photo
 *    places one where the finger is; Chain taps a run of points along one
 *    axis, each segment a D2 reading (FK1.1 …) and its total (FK1.t).
 *  - 3 · List: the surface's checklist, the only part of the screen that
 *    scrolls (v52 slice 6).
 *  - The Mark sheet (port 62 slice L3, SpMarkSheet): a tap on a mark's code --
 *    on the picked row under the photo, or in a list -- opens it: the moves the
 *    project's quote stage offers the mark (→ Client wish ...), the four acts,
 *    its note, and the trade of an Other, a Line or an area. Every rule and
 *    every write is MarkSheet / MarkLife in :pano.
 *  - The ten new types, their kinds and the Note (port 62 slice L6): More ▸ is
 *    one row per trade; a type with kinds names the kind on its chip ("Placing
 *    Wardrobe ▾ · tap the photo") and a tap on those words lists them; the Mark
 *    sheet's KIND changes a placed mark's; a Note is a pin of words, whose sheet
 *    opens as it is placed. The rows, the chip, the kinds and the tool's memory
 *    are SurveyPrepPalette in :pano; the pin's tag is NotePin.
 *  - The phase (port 62 slice L4): the project's quote stage decides what is
 *    here. A band says which phase it is ("2 · Wish list · every mark you place
 *    is a client wish"); at Estimated and at Client approved there is no 2 ·
 *    Mark and no palette; a type tapped in the Wish list stage is BORN a Client
 *    wish (the chip says "· as Client wish"); and a mark of an earlier phase is
 *    read-only -- its controls are hidden, a drag on it says why. Every rule is
 *    PhaseTools in :pano.
 *  - 4 · 360 (v52 slice 17, SurveyPrep360): the same marks, tapes, Lines,
 *    chains and reference on the 360 itself, turned by the finger, and
 *    placed and edited there through the same functions -- one model, two
 *    views; the maths is Pano360 in :pano.
 *  - PLAN, the 8th tab (v52 slice 13): six drawings -- the floor plan, the
 *    reflected ceiling plan and the four wall elevations -- from one pure
 *    display list (SurveyPlan in :pano), painted by SpPlanPane.
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
    /** For the Plan tab's export (v52 slice 14a): the client's INITIALS -- never the name -- the project's code, and the
     *  capture's date. They name the file ("AP_LR_htu_front-elevation") and fill the sheet's title block; no client
     *  name, phone or address is ever handed to this screen for it. */
    clientInitials: String = "",
    projectCode: String = "",
    captureDate: String = "",
    /**
     * THE PROJECT'S QUOTE STAGE, AS SURVEY PREP IS GIVEN IT (port 62 slice L3; PhaseTools.stageOf, through
     * QuoteStageSync.prepStage at both call sites): 0-5 for a project S5 lists; 0, Survey's rules, for a project S5
     * does not list -- "The tools keep following the project's own phase, Survey for a project made on the phone"
     * (index61c2.html 4091-4093); null only while a listed project's stage is unread, when the Mark sheet offers every
     * move and says "Quote stage not read yet". It decides what the Mark sheet offers a mark (MarkSheet.view) and what
     * a Restore answers (deleteMark). NO DEFAULT: a caller that forgot it would open every room under one stage's rules.
     * Since slice L4 it also decides what survey prep itself shows (PhaseTools): the band, 2 · Mark and the palette,
     * what a type tool's tap places a mark as, and which marks may be changed.
     */
    quoteStage: Int?,
    /**
     * WHAT THE BAND SAYS AFTER "Client approved" (port 62 slice L4; QuoteStageSync.prepLine at both call sites): the
     * project's one derived line, "Execution · 1 in progress", or null in every other stage, where the band says the
     * phase's own words. NO DEFAULT, as the stage has none.
     */
    projectLine: String?,
    /** Save: the prep, and the room size it is squared to now (L, W, H mm -- the size line may have changed it). */
    onDone: (SurveyPrep.Prep, DoubleArray) -> Unit,
    /** Back without Save: the work is kept on the phone either way, and handed back as it stands, with its size. */
    onCancel: (SurveyPrep.Prep, DoubleArray) -> Unit,
    /** A NEW capture's way out: drop the 360 altogether. Null on a kept capture. */
    onDiscard: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    // THE LANGUAGE (port 61c slice M3a): every word of the chrome is a tx accessor or a Masters name; the surface names,
    // the mark types and the laser's axis letters stay the English identities they are everywhere else
    val tx = LocalTx.current
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
    val prepNote: Msg? = remember { SurveyPrepFile.openedNote(opened) }
    var prep by remember { mutableStateOf(loaded ?: SurveyPrep.fresh(roomSize[0], roomSize[1], roomSize[2])) }
    var rev by remember { mutableIntStateOf(0) }
    var tab by remember { mutableStateOf("front") }
    // THE STEP AS IT WAS LAST SET; `mode`, below, is the step survey prep is ON: this, moved on from one the project's
    // phase does not show (port 62 slice L4). Everything reads `mode`; only a step change writes this.
    var modeSet by remember { mutableStateOf("lines") }
    // 4 · 360 (v52 slice 17): where the 360 looks -- yaw, pitch and fov; the viewer sizes it to its box. A face tab, the
    // step and fit face the open face again (sp.js P3.face); a look round or a pinch moves it. Not stored with the prep:
    // the 360 is a VIEW of the same marks, and nothing is kept per view.
    var p3Look by remember { mutableStateOf(Pano360.faceView("front", 1.0, 1.0)) }
    // THE PLAN TAB (v52 slice 13): the drawing it shows -- kept while other tabs are open, so List and back keeps it
    // (ct52 DR) -- and a counter bumped each time the tab is opened, which fits the drawing again (sp.js showTab)
    var planV by remember { mutableStateOf("floor") }
    var planFit by remember { mutableIntStateOf(0) }
    // THE EXPORT BAR (v52 slice 14a): open or not, a save running, and the file last saved (what Share hands out)
    var planExp by remember { mutableStateOf(false) }
    var planBusy by remember { mutableStateOf(false) }
    var planSaved by remember { mutableStateOf<PlanExport.Saved?>(null) }
    val scope = rememberCoroutineScope()
    var show by remember { mutableStateOf("both") }
    var sel by remember { mutableStateOf<SpSel?>(null) }
    var active by remember { mutableStateOf<String?>(null) }
    val hist = remember { mutableStateListOf<SurveyPrep.Prep>() }
    // "N points re-centred" (v52 slice 4): said once, on the open that made older points 50 mm spots -- a Msg,
    // rendered with the open-time tx (say() keeps a String: M3a's rule)
    var note by remember { mutableStateOf(SurveyPrepFile.pointsNote(opened)?.invoke(tx)) }
    // a Msg, drawn in whatever language is on when the bar is drawn (design-marathi §2.4); the opened file's own
    // word (SurveyPrepFile.savedText) is a Msg too since port 61c slice M3b
    var savedMsg by remember { mutableStateOf<Msg>(SurveyPrepFile.savedText(opened)) }
    var dirty by remember { mutableIntStateOf(0) }
    var armed by remember { mutableStateOf(false) }
    var armedDiscard by remember { mutableStateOf(false) }
    // ⋮ (v52 slice 6): the status, the laser sizes, the web lines, Start over and Discard
    var menuOpen by remember { mutableStateOf(false) }
    // THE PALETTE (v52 slice 7): the tool on in 2 · Mark -- Select, or a type's letter, STICKY until another is
    // picked or the one on is tapped again -- and whether the More sheet is open
    // ... as it was last picked; `tool` is the one that is ON: Select wherever the phase has no palette (port 62 slice
    // L4; the prototype's spPre, index61c2.html 4210), so at Estimated and at Client approved nothing is placed or drawn
    var toolSet by remember { mutableStateOf(SurveyPrepPalette.SEL) }
    val tool = PhaseTools.toolFor(toolSet, quoteStage)
    var moreOpen by remember { mutableStateOf(false) }
    // LINE AND TAPE (v52 slice 9): a Tape or a Line being pulled (the Draft changes as the finger moves; rev follows
    // it), and the corners of a Line area tapped so far, on [areaS]
    var mdrag by remember { mutableStateOf<SurveyMeas.Draft?>(null) }
    var area by remember { mutableStateOf<List<DoubleArray>?>(null) }
    var areaS by remember { mutableStateOf<String?>(null) }
    // END POINTS THAT MOVE (v52 slice 10, item 8): while a Line's or a tape's end is dragged, the guide it shows --
    // through the fixed end, on the axis it snapped onto (SurveyGuide.endGuide); null when no end is being dragged
    var endG by remember { mutableStateOf<SurveyGuide.Guide?>(null) }
    // THE PLANE GRID BUTTON (v52 slice 10, in the fit column): the grid on the face whether or not anything is drawn
    var grid by remember { mutableStateOf(false) }
    // A CHAIN BEING DRAWN (v52 slice 11): its points so far, changed in place as the finger taps and drags (rev follows
    // it); null when none. A tool, a face or a step changed ENDS it -- plotted with two points or more (flushChain);
    // ✕ on the chip, a clear or Start over drops it (sp.js kFlush, d.cancel)
    var kdraft by remember { mutableStateOf<SurveyChain.Draft?>(null) }
    /** No Tape or Line half drawn and no end being dragged: a drag let go of, the area's corners forgotten (a tool, a
     *  face or a step changed -- sp.js gClear, ct52 FX11), so no guide is left on; a chain being drawn dropped (a
     *  caller that should keep it -- a tool, face or step change -- ends it first with flushChain).
     *  Declared here, above every caller -- a local function is visible only below its declaration. */
    fun dropDrafts() { mdrag = null; area = null; areaS = null; endG = null; kdraft = null }
    val views = remember { mutableStateMapOf<String, SpCut>() }
    // a finger is on the photo (SpPhoto, from its first down to its last up): a wider cut waits for it to lift
    var photoBusy by remember { mutableStateOf(false) }
    val rects = remember { mutableStateMapOf<String, SpRect>() }
    // THE SNACKBAR (v52 slice 8, sp.js snack): five seconds, with UNDO -- or, for a refusal, without; a new one
    // replaces the old and restarts the five seconds (the sequence number is the timer's key)
    var snackText by remember { mutableStateOf<String?>(null) }
    var snackUndo by remember { mutableStateOf(true) }
    var snackSeq by remember { mutableIntStateOf(0) }
    // THE LONG-PRESS MENU (v52 slice 8): the mark it is for, and where the press was (root px)
    var ctx by remember { mutableStateOf<SpCtx?>(null) }
    // ⋮'s clears, armed by a first tap (SurveyPrepEdit.SURF / ROOM), done by a second within 4 s
    var armedClear by remember { mutableStateOf<String?>(null) }
    // SAY (sp.js say): a note lasts 2.6 s from the LAST time it was said -- the same words said again restart the
    // timer (the sequence number is its key, as the snackbar's), so a second refusal is never cut short
    var noteSeq by remember { mutableIntStateOf(0) }
    fun say(t: String) { note = t; noteSeq++ }
    LaunchedEffect(noteSeq) { if (note != null) { delay(SurveyPrepEdit.SAY_MS); note = null } }
    LaunchedEffect(armed) { if (armed) { delay(SurveyPrepEdit.ARM_MS); armed = false } }
    LaunchedEffect(armedDiscard) { if (armedDiscard) { delay(SurveyPrepEdit.ARM_MS); armedDiscard = false } }
    LaunchedEffect(armedClear) { if (armedClear != null) { delay(SurveyPrepEdit.ARM_MS); armedClear = null } }
    LaunchedEffect(snackSeq) { if (snackSeq > 0) { delay(SurveyPrepEdit.SNACK_MS); snackText = null } }
    // hist's size when the snackbar was shown: its UNDO undoes only while nothing has been done since (with
    // snapshot() hiding the snackbar, as sp.js does, this is belt and braces)
    var snackHist by remember { mutableIntStateOf(-1) }
    fun snack(text: String, undo: Boolean = true) { snackText = text; snackUndo = undo; snackSeq++; snackHist = hist.size }
    fun hideSnack() { snackText = null }
    /** A locked mark's move, resize or delete refused: the no-UNDO snackbar, and no undo entry (sp.js refuse). */
    fun refuse(code: String) { ctx = null; snack(SurveyPrepEdit.refusal(tx, code), undo = false) }

    // ---- the size sheet (v51): L, W, H boxes, Cancel / Apply
    var lwhOpen by remember { mutableStateOf(false) }
    val lwhText = remember { mutableStateListOf(TextFieldValue(""), TextFieldValue(""), TextFieldValue("")) }
    var lwhBad by remember { mutableStateOf(emptyList<Int>()) }

    // ONE FRAME AND ONE SIZE (v52 slice 12): every face is squared, sized, listed, saved and uploaded through
    // SurveyPrep.frameOf / sizeOf -- from its reference rectangle when that is on, else its edges and the laser -- so a
    // mark is never placed in one frame and written in another. Nothing here squares a face any other way.
    fun ab(s: String) = SurveyPrep.sizeOf(s, prep, roomSize[0], roomSize[1], roomSize[2])
    fun frameOf(s: String) = SurveyPrep.frameOf(s, prep, roomSize[0], roomSize[1], roomSize[2])
    fun keyOf(s: String): String? = frameOf(s)?.key()

    // ---- THE PHASE (port 62 slice L4; the prototype's P54.spPre, index61c2.html 4204-4224). The project has ONE phase,
    // its S5 quote stage, and survey prep shows only that phase's tools -- hidden, never greyed. The step it is on is the
    // one last set, moved on from a step the phase does not show: 2 · Mark goes to 3 · List at Estimated and at Client
    // approved (PhaseTools.prepMode; 1 · Lines is slice L10's). Read on every recomposition, so a sync that moves the
    // stage while the screen is open takes the step and the tool with it, as the prototype's render does.
    val mode = PhaseTools.prepMode(modeSet, quoteStage) { StepRoute.isFace(tab) && frameOf(tab) != null }
    // the band: which phase this is, and with it why a tool is not there
    val band = PhaseTools.band(tx, quoteStage, projectLine)
    // "AS …" WHILE THE STAGE IS UNREAD (the prototype's asMem, 2853-2858): what this capture's chip was last cycled to.
    // Every stage that is read fixes what a mark is placed as, and PhaseTools.asFor reads this only when none is.
    var asPicked by remember { mutableStateOf(store.placeAs(id)) }
    fun cycleAs() {
        val next = PhaseTools.nextAs(quoteStage, asPicked) ?: return
        asPicked = next; store.setPlaceAs(id, next); rev++
    }
    /** An act the phase does not allow on an item (the prototype's spRefuse): said for 2.6 s, nothing changed, no undo
     *  entry. Its control is hidden, so this is only ever reached by a drag, or by a press already on its way. */
    fun phaseRefuse(text: String?) { ctx = null; if (text != null) say(text) }

    // Saved a moment after each change, then read back: the tick is only shown when what is on disk matches.
    val unsaved = remember { booleanArrayOf(false) }
    fun changed() { dirty++; rev++; unsaved[0] = true }
    LaunchedEffect(dirty) {
        if (dirty == 0) return@LaunchedEffect
        savedMsg = { _: Tx -> "…" } // text:value glyph
        delay(800)
        val ok = store.save(id, prep, roomSize[0], roomSize[1], roomSize[2])
        if (ok) unsaved[0] = false
        val at = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.ROOT))
        savedMsg = if (ok) { tx: Tx -> tx.sp_msg_saved + at } else { tx: Tx -> tx.sp_msg_not_saved_did_not }
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
    // S5's words for a re-file by work it refused (WorkRefile; FW2 review), read beside the state, or null
    var refileRefused by remember { mutableStateOf(AsIsUpload.refileRefused(context, id)) }
    // the pull's line and its colour KIND (Readings.NoteKind, port 61c slice M3b): the colour reads the kind
    var readingsMsg by remember { mutableStateOf<Pair<com.malletcrafts.interiorestimate.pano.Readings.NoteKind, Msg>?>(null) }
    // A pull of the web's readings is asked for by bumping this: on open, on resume, and when an upload lands.
    var pullTick by remember { mutableIntStateOf(1) }
    LaunchedEffect(Unit) {
        var wasDone = webState?.ok == true
        while (true) {
            webState = withContext(Dispatchers.IO) { AsIsUpload.status(context, id) }
            refileRefused = withContext(Dispatchers.IO) { AsIsUpload.refileRefused(context, id) }
            val isDone = webState?.ok == true
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

    // A NEW UNDO STEP HIDES THE SNACKBAR (sp.js snapshot: hideSnack() first), so its UNDO can never undo a later,
    // different change than the one it names; deleteMark, lockAllOf and clearTap show theirs after it
    fun snapshot() { hideSnack(); hist.add(prep.copy()); if (hist.size > 40) hist.removeAt(0) }
    // THE D2 INTO A REFERENCE BOX (v52 slice 12): the face and the box ("w" / "h") its D2 button armed and when; the next
    // reading sets that reference's W or H -- never prep.actual, never a reading key -- with an undo step, as a typed
    // figure. Where a reading goes is decided when it ARRIVES (SurveyRef.route, tested): a box left before its reading
    // came sets the reading aside, so it can never land as a mark's reading
    var refShot by remember { mutableStateOf<SurveyRef.Shot?>(null) }
    fun undo() {
        if (hist.isEmpty()) return
        val old = hist.removeAt(hist.size - 1)
        // Readings are not geometry: the ones taken since stay, and a reading a delete took away comes back with its
        // owner -- a mark, a Line's or an area's (v52 slice 9), a chain's FK1.1 … FK1.t (slice 11); and a code handed
        // out since is not handed out again: the web may hold readings under it (v52 slice 8). SurveyPrepEdit.undoTo, tested
        val back = SurveyPrepEdit.undoTo(prep, old)
        hideSnack(); ctx = null
        prep = back; sel = null
        if (active != null && back.ownerOf(active!!) == null) active = null
        // a reference box's D2 still out is LEFT (slice 12 review): its reading is set aside, never a mark's, and a
        // reference undone away never takes one (SurveyRef.route)
        refShot = SurveyRef.leave(refShot)
        changed()
    }

    /**
     * A LONG PRESS ON A MARK THE FINGER'S DRIFT HAD ALREADY MOVED (v52 slice 8 review): the press's own undo step is
     * taken back off and its geometry put back, so the menu opens on the mark where it was -- nothing moved, nothing
     * snapped, no undo entry, nothing to save (sp.js never opens its menu on a grab that moved). Readings are kept.
     */
    fun revertPress() {
        if (hist.isEmpty()) return
        val old = hist.removeAt(hist.size - 1)
        val back = SurveyPrep.Prep(old.surfaces, mutableMapOf(), old.extra, old.extraSurfaces)
        for ((k, v) in prep.actual) if (back.ownerOf(k) != null) back.actual[k] = v
        SurveyPrepEdit.keepSeq(prep, back)
        prep = back
        // SAVED AGAIN (v52 slice 10 review): the 800 ms save may have written the half-dragged geometry while the press
        // was still down; the reverted prep is marked unsaved so it is written back (one write, no undo entry)
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
        if (granted.values.all { it }) disto.start() else say(tx.sp_msg_bluetooth_refused)
    }
    fun reading(code: String, v: Double) {
        prep.actual[code] = v
        // the next one still missing on the same mark; a Line has one reading, so none after it (v52 slice 9)
        active = prep.nextAfter(code) { ab(it) }
        changed()
    }
    fun distoGo() {
        when (distoState) {
            DistoClient.State.OFF -> askBle.launch(blePerms)
            DistoClient.State.READY -> disto.measure()
            else -> say(tx.sp_msg_laser_still_connecting)
        }
    }
    fun fire() {
        // a measurement's D2: the reading is that measurement's, not a reference box's armed earlier
        refShot = null
        if (active == null) { say(tx.refuse_sp_tap_measurement_first); return }
        distoGo()
    }
    /** A REFERENCE'S W OR H SET (v52 slice 12, sp.js setRefDim) -- typed into its box or read by its own D2 -- on face
     *  [s]: the reference's figure, rounded to the mm, never a reading; one undo step, none when nothing changed.
     *  Declared here, above the D2's reading handler that calls it. */
    fun setRefDimS(k: String, v: Double?, s: String) {
        snapshot()
        if (!SurveyRef.setDim(prep.surface(s), k, v)) { hist.removeAt(hist.size - 1); return }
        changed()
    }
    // the laser's words rendered LIVE on the one keyed screen that says them (port 61c slice M6)
    disto.onState = { s, n -> distoState = s; if (n != null) say(n(tx)) }
    disto.onRefused = { why -> say(why(tx)) }
    disto.onReading = { r ->
        val route = SurveyRef.route(refShot, prep, r.mm, System.currentTimeMillis())
        refShot = null
        val a = active
        when (route) {
            is SurveyRef.Route.Ref -> setRefDimS(route.k, r.mm.toDouble(), route.s)
            is SurveyRef.Route.Drop -> say(SurveyRef.leftSaid(tx, route.mm))
            SurveyRef.Route.Normal ->
                if (a == null || prep.ownerOf(a) == null) say(tx.sp_msg_d2_x_tap_measurement(r.mm.toString()))
                else reading(a, r.mm.toDouble())
        }
    }

    // ---- site readings from the web (ReadingsSync): what changed there is taken, never an unsent edit here
    LaunchedEffect(pullTick) {
        if (pullTick == 0) return@LaunchedEffect
        val row = withContext(Dispatchers.IO) { CaptureStore(context).all().firstOrNull { it.deviceId == id } }
        // Off the main thread: a session past its renewal time is renewed first (S5Auth.fresh).
        val auth = withContext(Dispatchers.IO) { S5Auth.fresh(context) }
        // THIS capture's readings, by its device id -- and by its ERP name while S5 still holds it under that
        // (port 61c slice AS1, ReadingsSync.fetch): never the room's, which could be another 360's
        val fetched: Result<Map<String, com.malletcrafts.interiorestimate.pano.Readings.Remote>>? =
            if (row == null || row.project.isBlank() || auth == null) null
            else withContext(Dispatchers.IO) { runCatching { ReadingsSync.fetch(auth, row.project, row.room, row.deviceId, row.serverName) } }
        val remote = fetched?.getOrNull()
        // a conflict on a reading whose mark was deleted or cleared here is not put back as an orphan: the delete
        // stands and is sent again (Readings.orphans), so the upload is queued for it (v52 slice 8 review)
        val (applied, moved, orphans) = ReadingsSync.merge(context, id, prep.actual, remote) { k -> prep.ownerOf(k) != null }
        if (orphans.isNotEmpty() && AsIsUpload.status(context, id) != null) AsIsUpload.enqueue(context, id)
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
        // what had to be said -- a conflict applied, an orphan re-deleted -- rendered in the language on when ⋮ opens
        // (Readings.conflictText / orphanText, port 61c slice M3b)
        val clean = applied.isEmpty() && orphans.isEmpty()
        val head: (Tx) -> String = { t ->
            if (clean) "" else listOfNotNull(
                applied.takeIf { it.isNotEmpty() }?.let { com.malletcrafts.interiorestimate.pano.Readings.conflictText(t, it) },
                com.malletcrafts.interiorestimate.pano.Readings.orphanText(t, orphans)).joinToString("; ") + " · " // text:value separators
        }
        val e = fetched?.exceptionOrNull()
        // a Msg (port 61c slice M3a), WITH ITS KIND (M3b, critic note 3): the colour reads the kind, never the text --
        // and OK only when the pull succeeded AND nothing had to be said, so a conflict-carrying pull stays amber
        // (the M3b critic's finding 4; today's behaviour exactly). The time is the pull's.
        val at = ReadingsSync.now()
        val kind = com.malletcrafts.interiorestimate.pano.Readings.noteKind(
            ok = fetched != null && e == null, notYet = e != null && ReadingsSync.notAvailable(e), clean = clean)
        readingsMsg = when {
            fetched == null -> if (clean) readingsMsg else kind to { tx: Tx -> head(tx).removeSuffix(" · ") }
            e == null -> kind to { tx: Tx -> head(tx) + tx.sp_readings_web + at }
            ReadingsSync.notAvailable(e!!) -> kind to { tx: Tx -> head(tx) + tx.sp_web_readings_not_yet }
            // the fetch's own words as they come, or common.failed when it had none (the card, 28 Oct 2026)
            else -> kind to { tx: Tx -> head(tx) + tx.sp_readings_web_x((e?.message ?: tx.common_failed).take(160)) }
        }
    }

    // ---- pictures: the six views once, and each surface squared whenever its lines change
    // EVERY VIEW IS CUT WITH A 30 % BORDER of the same projection (sp.js loadPano / cutView, v41), so a corner at or past
    // the edge of the 960 x 800 view is still in the picture; the coordinates do not move -- (0, 0) is still the view's
    // corner and the picture is drawn ox, oy up and left of it
    LaunchedEffect(Unit) {
        for (s in listOf(tab) + SurveyPrep.SURFACES.filter { it != tab }) {
            val c = withContext(Dispatchers.Default) { SurveyPrep.cutView(previewPano, s, SurveyPrep.baseExt(s)) }
            val bmp = withContext(Dispatchers.Default) { FaceWriter.bitmapOf(c.img) }
            // a cut the wall's lines have already grown (below) is never put back to the border alone
            if (views[s] == null) views[s] = SpCut(bmp, c.ox, c.oy, c.ext)
        }
    }
    // A WALL'S VIEW REACHES ITS CORNERS (sp.js ensureCut, v43; Amit, 11 Oct 2026: "right side of wall is cropped. i cant
    // see both right side corners."): in 1 · Lines the wall on show is cut as wide as its lines need -- 15° beyond the
    // furthest of them, at least ±70°, at most ±84° (SurveyPrep.needExt) -- and cut again, wider, after a line is moved
    // or nudged towards the edge. Never during a drag (dirty moves only when a change is DONE: a drag let go, a nudge, an
    // undo), and never narrower than it was (SurveyPrep.growExt).
    // A cut that lands while a finger is on the photo WAITS for the finger to lift (sp.js 1527: ensureCut only when
    // !surfV.busy()), so a line held near the edge is never re-pictured, nor its fit moved, under the finger (review).
    // A burst of nudges makes ONE cut: the effect restarts on every change, waits 150 ms first, and a cut it no longer
    // wants stops at its next row (cutView's onRow) rather than running full width and being thrown away (review).
    val cutHere = views[tab]
    LaunchedEffect(tab, mode, dirty, cutHere != null) {
        if (mode != "lines" || !StepRoute.isFace(tab) || SurveyPrep.isPlan(tab)) return@LaunchedEffect
        val s = tab
        delay(150)
        val have = views[s] ?: return@LaunchedEffect
        val need = SurveyPrep.needExt(s, prep.surface(s).lines, frameOf(s)?.q)
        val grow = SurveyPrep.growExt(have.ext, need) ?: return@LaunchedEffect
        val c = withContext(Dispatchers.Default) { val cutting = this; SurveyPrep.cutView(previewPano, s, grow) { cutting.ensureActive() } }
        val bmp = withContext(Dispatchers.Default) { FaceWriter.bitmapOf(c.img) }
        snapshotFlow { photoBusy }.first { !it }
        val now = views[s]
        // a wider one made meanwhile is kept: a cut never shrinks
        if (now == null || SurveyPrep.growExt(now.ext, c.ext) != null) views[s] = SpCut(bmp, c.ox, c.oy, c.ext)
    }
    val need = when {
        tab == "list" -> SurveyPrep.SURFACES.filter { prep.surface(it).done() }
        !StepRoute.isFace(tab) -> emptyList()   // the Plan tab draws from the marks, not from a squared photo
        mode == "mark" || mode == "list" -> listOf(tab)
        else -> emptyList()
    }
    val wanted = need.mapNotNull { s -> keyOf(s)?.let { s to it } }
    LaunchedEffect(wanted.joinToString("#") { it.first + "=" + it.second }) {
        for ((s, k) in wanted) {
            if (rects[s]?.key == k) continue
            val f = frameOf(s) ?: continue
            val sq = SurveyPrep.Square.screen(f.a, f.b)
            // drawn as far as a wall can be cut, ±84° (sp.js rectify's lim, render's default), so a corner past the bare
            // view is in the squared face -- and in the saved elevations and the as-is upload, which square it the same way
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
        if (tab != s && tab != "list") { tab = s; if (mode != "360") modeSet = "mark"; dropDrafts() }
        rev++
    }
    /** A finger down on a line or a mark: it is selected, with the part grabbed; a newly selected mark's
     *  first unmeasured field becomes the active one. */
    fun grabbed(g: SpSel) {
        val fresh = g.id != null && sel?.id != g.id
        sel = g
        val gid = g.id
        if (fresh && gid != null) {
            val e = prep.markById(gid)
            if (e != null) active = ab(e.first).let { prep.nextOpen(e.first, e.second, null, it[0], it[1]) }
            // a chain's point pressed (v52 slice 11): the chain picked, its first open segment the one being read
            else prep.chainById(gid)?.let { (s, c) -> active = ab(s).let { prep.nextOpen(c, null, it[0], it[1]) } }
        }
        rev++
    }
    // ---- THE MARK SHEET (port 62 slice L3; the prototype's L53 sheet, index61c2.html 3012-3115): open on one item, or
    // null. Its state is MarkSheet.State in :pano, a value: every tap and keystroke REPLACES it through MarkSheet's pure
    // functions, and nothing reaches the prep until Done (sheetDone). Declared HERE, above the two placements, which open
    // a new Note's sheet -- a local function is visible only below its declaration.
    var markSheet by remember { mutableStateOf<MarkSheet.State?>(null) }
    /** A tap on a mark's code (or a Line's, an area's): its sheet opens. [focusMove]: the move whose Why box it opens on
     *  -- Dropped, when a tap on Drop opened it. A Tape, a chain and the reference have no sheet: nothing opens.
     *  [fresh] (slice L6): the sheet of a Note the tap before it placed -- it opens on the note field, and knows how many
     *  undo steps the screen holds now, the placement's the last. */
    fun openSheet(mid: String, focusMove: String? = null, fresh: Boolean = false) { ctx = null; markSheet = MarkSheet.open(prep, mid, focusMove, fresh, hist.size) }
    /** A mark just placed: A NOTE OPENS ITS SHEET at once, on its note field (L53.placed: `openSheet(e.id,{fresh:true})`). */
    fun notePlaced(e: SurveyPrep.Mark) { if (MarkLife.isNote(e)) openSheet(e.id, fresh = true) }
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
        if (!SurveyPrep.onFace(a[0], a[1], mx, my)) { say(SurveyPrep.placeMiss(tx, s)); return null }
        // BORN WHAT THE PHASE PLACES (port 62 slice L4; the prototype's L53.placed, index61c2.html 2959): a Client wish in
        // the Wish list stage, Vendor added at Vendor review and Revisions, a v52 mark at Survey -- and NOTHING, with no
        // undo step, where the phase has no palette. PhaseTools.place decides, stamps and takes the one undo step.
        // OF THE KIND THE TOOL REMEMBERS for this type on this kind of surface (port 62 slice L6; T53.newFields): its
        // first until one is picked on the chip or in a Mark sheet.
        val placed = PhaseTools.place(prep, s, t, mx, my, a[0], a[1], java.util.UUID.randomUUID().toString().take(8),
            quoteStage, asPicked, java.time.Instant.now(), SurveyPrepPalette.subFor(spKinds, t, s)) { snapshot() } ?: return null
        val e = placed.mark
        sel = SpSel(s, id = e.id); active = prep.nextOpen(s, e, null, a[0], a[1])
        if (placed.amber) say(MarkLife.afterEstimate(tx))
        changed()
        notePlaced(e)
        return e
    }
    /**
     * A TAP ON THE 360 WITH A TYPE ON (v52 slice 17, sp.js addElAt): type [t], picked on the open tab's palette, placed on
     * the surface the finger is on, [s], centred on ([mx], [my]) mm and kept inside it (SurveyPrep.placeAt) -- but only
     * when [s] has a type of that NAME (Pano360.placeRefusal; item 11: "A Switch plate cannot go on the floor", said, and
     * nothing placed). The new mark picked, its first measurement active, the tab following it; one undo step.
     */
    fun place360(s: String, t: String, mx: Double, my: Double): SurveyPrep.Mark? {
        if (mode != "360" || !StepRoute.isFace(tab) || s !in SurveyPrep.SURFACES) return null
        Pano360.placeRefusal(tx, tab, s, t)?.let { say(it); return null }
        val a = ab(s)
        // born what the phase places, or not placed at all, exactly as on 2 · Mark (PhaseTools.place; slice L4)
        // ... of the kind remembered for THE SURFACE IT LANDS ON, [s], never the open tab's (port 62 slice L6; the L5
        // review, finding 2): a Plumbing point picked on a wall and tapped onto the floor is the floor's kind
        val placed = PhaseTools.place(prep, s, t, mx, my, a[0], a[1], java.util.UUID.randomUUID().toString().take(8),
            quoteStage, asPicked, java.time.Instant.now(), SurveyPrepPalette.subFor(spKinds, t, s)) { snapshot() } ?: return null
        val e = placed.mark
        tab = s; sel = SpSel(s, id = e.id); active = prep.nextOpen(s, e, null, a[0], a[1])
        if (placed.amber) say(MarkLife.afterEstimate(tx))
        changed()
        notePlaced(e)
        return e
    }
    /**
     * A CHAIN ENDED (v52 slice 11, sp.js kFinish) -- a double tap, ✓ on the chip, or the tool, the face or the step
     * changed with two points or more: its points in order along its axis, plotted as FK1 … (the next K number, never
     * one handed out before), picked, its first segment the one being read; one ↶ step. Fewer than two different points:
     * nothing plotted, and it says why ("A chain needs two points or more").
     */
    fun finishChain(d: SurveyChain.Draft) {
        kdraft = null
        when (val f = SurveyChain.finish(d)) {
            is SurveyChain.Finish.Refused -> f.text?.let { say(it(tx)) }
            is SurveyChain.Finish.Done -> {
                snapshot()
                val c = prep.addChain(d.s, f.axis, f.nodes, f.snaps, java.util.UUID.randomUUID().toString().take(8))
                sel = SpSel(d.s, id = c.id)
                active = ab(d.s).let { prep.nextOpen(c, null, it[0], it[1]) }
                changed()
            }
        }
        rev++
    }
    /** A tool, a face or a step changed with a chain half drawn (sp.js kFlush): two points or more, it is plotted;
     *  fewer, it goes. */
    fun flushChain() {
        val d = kdraft ?: return
        kdraft = null
        if (d.nodes.size >= 2) finishChain(d)
    }
    /** A tap (or the start of a drag) with Chain on, at [mm] on the open face: a point added -- the first starts a chain,
     *  the second fixes its axis, each after it sits on that axis -- snapped within [tol] mm (SurveyChain.press). The new
     *  point's index, or null when refused (a chain is being drawn on another face: said). */
    fun chainPress(mm: DoubleArray, tol: Double): Int? {
        val s = tab; val a = ab(s)
        return when (val r = SurveyChain.press(kdraft, s, mm, a[0], a[1], prep.surface(s).marks, tol)) {
            is SurveyChain.Press.Busy -> { say(SurveyChain.busyText(tx, r.s)); null }
            is SurveyChain.Press.Added -> { kdraft = r.draft; rev++; r.idx }
        }
    }
    /** The point just added follows the finger (its axis re-decided while it is the second). */
    fun chainMove(idx: Int, mm: DoubleArray, tol: Double) {
        val d = kdraft ?: return
        val a = ab(d.s)
        if (SurveyChain.move(d, idx, mm, a[0], a[1], prep.surface(d.s).marks, tol)) rev++
    }
    /** A second finger: the point this press added is taken back (the draft goes when it is empty). */
    fun chainCancel(idx: Int) {
        val d = kdraft ?: return
        if (SurveyChain.cancel(d, idx)) kdraft = null
        rev++
    }
    /** A palette slot (or a More tile): that tool on, or Select when it was already on. A Line area half tapped is let go
     *  (sp.js setTool: st.mdraft=null); a chain half drawn is ENDED first -- plotted with two points or more (kFlush). */
    fun pickTool(t: String) {
        flushChain()
        toolSet = SurveyPrepPalette.next(tool, t); moreOpen = false; dropDrafts()
        if (t != SurveyPrepPalette.SEL) note = null
    }
    // THE PHASE'S MOVE IS MADE FOR GOOD (spPre writes st.mode and st.tool, 4208-4210): once a step or a tool has been
    // moved on, the one last set is forgotten, so a stage moved back later does not bring it back by itself. A chain
    // half drawn is ended as any tool change ends it; the More sheet shuts.
    LaunchedEffect(modeSet, toolSet, quoteStage) {
        if (modeSet != mode) modeSet = mode
        if (toolSet != tool) { flushChain(); toolSet = tool; moreOpen = false; dropDrafts() }
    }
    /**
     * A NEW NOTE THAT WENT AGAIN with no words (port 62 slice L6; the prototype's removeFresh, index61c2.html 3068-3070):
     * MarkSheet has taken it off its surface. The placement's own undo step is taken back with it -- when it is still
     * the last one (MarkSheet.takesBackUndo) -- so an empty Note leaves no undo entry; it is no longer picked; and the
     * screen says "Empty note removed", with no UNDO.
     */
    fun noteRemoved(st: MarkSheet.State) {
        if (MarkSheet.takesBackUndo(st, hist.size)) hist.removeAt(hist.size - 1)
        if (sel?.id == st.id) { sel = null; active = null }
        changed()
        snack(MarkSheet.emptyRemoved(tx), undo = false)
    }
    /**
     * DONE on the Mark sheet (the prototype's apply, 3071-3088): MarkSheet.apply writes through MarkLife, Trades and
     * MarkTypes -- the kind, the trade, the note, then the move or the act -- and takes ONE undo step through the
     * snapshot handed to it, just before its first write (none for a new Note's sheet: its placement took the step);
     * nothing changed closes the sheet with no undo step at all. A move the office is told of says "After the estimate
     * — tell the office". The sheet stays open while the move picked still needs its why. A kind that was changed is
     * remembered by the tool (slice L6); a new Note with no words goes again (noteRemoved).
     */
    fun sheetDone() {
        val st = markSheet ?: return
        val done = MarkSheet.apply(prep, st, quoteStage, java.time.Instant.now()) { snapshot() }
        markSheet = done.state
        done.kind?.let { k -> SurveyPrepPalette.rememberSub(spKinds, k.t, k.s, k.sub) }
        when (done.outcome) {
            MarkSheet.Outcome.DONE -> { changed(); if (done.amber) say(MarkLife.afterEstimate(tx)) }
            MarkSheet.Outcome.NEEDS_WORDS -> snack(MarkSheet.needsWords(tx), undo = false)
            MarkSheet.Outcome.REMOVED -> noteRemoved(st)
            MarkSheet.Outcome.GONE, MarkSheet.Outcome.UNCHANGED, MarkSheet.Outcome.STAYS -> Unit
        }
    }
    /** ✕, the scrim, ◁ and a swipe down on the Mark sheet (the prototype's cancel, 3065-3067): it closes and what was
     *  typed is lost -- and a Note just placed, still with no words, goes again (MarkSheet.cancel removes it). */
    fun sheetCancel() {
        val st = markSheet ?: return
        markSheet = null
        if (MarkSheet.cancel(prep, st)) noteRemoved(st)
    }
    /** DELETE (every path: the bar row, the floating trash, a list row, the long-press menu): '<code> deleted' with
     *  UNDO, which brings the mark back with its readings; a LOCKED mark is refused, with no undo entry. A Tape, a
     *  Line or an area (v52 slice 9) the same: F-M1 takes F-M1.len with it, and the readings sync deletes it on the web. */
    fun deleteMark(mid: String) {
        // THE REFERENCE IS NEVER DELETED (v52 slice 12; Amit, 14 Oct 2026 06:45 IST: "can not be deleted"): every delete
        // path that reaches it answers so, with no UNDO and no undo entry (sp.js refNoDelete)
        if (SurveyRef.isId(mid)) { ctx = null; snack(SurveyRef.noDelete(tx), undo = false); return }
        val it0 = prep.itemById(mid) ?: return
        val code = it0.code
        ctx = null
        // THE PHASE FIRST (port 62 slice L4; sp.js deleteMark's `!P54.spDelOk(e)`, before the lock and before Drop): an
        // item the phase holds read-only is not deleted, dropped or restored -- its control is hidden, and this says why
        if (!PhaseTools.delOk(prep, mid, quoteStage)) { phaseRefuse(PhaseTools.refuseText(tx, prep, mid, quoteStage)); return }
        if (it0.locked) { refuse(code); return }
        // A MARK PAST EXISTING IS NEVER DELETED (port 62 slice L2; the prototype's interceptDelete, index61c2.html
        // 2940-2946): its control reads Drop, or Restore on a Dropped mark (MarkLife.deleteLabel), and
        // MarkLife.deleteAct says what a tap does. RESTORE moves it back at once to what it was born -- one ↶ step,
        // "FW1 restored to Client wish" with UNDO -- or, where the stage offers no move, says why with no UNDO and
        // no undo entry. DROP needs its why, which is typed in the Mark sheet: the sheet opens HERE, on Dropped (slice
        // L3; interceptDelete's `openSheet(id,{focusMove:'drop'})`), with no undo step of its own -- its Done takes
        // the one step. Nothing is ever deleted (SurveyPrep.Prep.deleteMark refuses too).
        val lifeMark = prep.markById(mid)?.second
        if (lifeMark != null) when (MarkLife.deleteAct(lifeMark)) {
            MarkLife.DeleteAct.PLAIN -> Unit
            MarkLife.DeleteAct.RESTORE -> {
                // THE PHASE'S GATE ANSWERS BEFORE ANY UNDO STEP (the L2 review, finding 4; interceptDelete asks
                // `stage===0` and says the gate's line BEFORE H.snapshot()): a snapshot taken first and popped again
                // would drop the OLDEST of 40 held steps on every refused tap. The gate's own line, no UNDO.
                val gate = PhaseTools.gate(quoteStage)
                if (gate != null) { snack(PhaseTools.gateText(tx, gate), undo = false); return }
                snapshot()
                val moved = MarkLife.restore(lifeMark, quoteStage, java.time.Instant.now())
                if (!moved.ok) { hist.removeAt(hist.size - 1); snack(MarkLife.refusalText(tx, lifeMark, moved).orEmpty(), undo = false); return }
                snack(MarkLife.restoredText(tx, lifeMark)); changed()
                return
            }
            MarkLife.DeleteAct.DROP_SHEET -> { openSheet(mid, MarkLife.DROP); return }
        }
        snapshot(); prep.deleteMark(mid)
        if (sel?.id == mid) sel = null
        if (active?.startsWith("$code.") == true) active = null
        snack(SurveyPrepEdit.deleted(tx, code))
        changed()
    }
    fun nudgeMark(dx: Double, dy: Double) {
        val sl = sel ?: return; val mid = sl.id ?: return
        val e = prep.surface(sl.s).marks.firstOrNull { it.id == mid } ?: return
        // a mark of an earlier phase is not nudged (slice L4; sp.js nudgeMark's `!P54.spEdit(e)`, before the lock)
        if (!PhaseTools.editable(e, quoteStage)) { phaseRefuse(PhaseTools.refuseText(tx, e, quoteStage)); return }
        if (SurveyPrep.isLocked(e)) { refuse(e.code); return }
        val a = ab(sl.s); snapshot()
        SurveyPrep.moveMark(e, e.copy(), sl.part, dx, dy, a[0], a[1])     // buttons never snap
        changed()
    }
    /** Lock or unlock one mark (its padlock, the menu's Lock): one undo step, nothing said (sp.js setLock). */
    fun toggleLock(mid: String) {
        // ... or the reference rectangle's padlock (v52 slice 12): one undo step, nothing said, as a mark's
        if (prep.itemById(mid) == null && SurveyRef.byId(prep, mid) == null) return
        // an item the phase holds read-only is neither locked nor unlocked (slice L4; sp.js setLock's `!P54.spEdit`) --
        // never the reference's padlock, which is not the phase's (PhaseTools.editable answers true for it)
        if (!PhaseTools.editable(prep, mid, quoteStage)) { phaseRefuse(PhaseTools.refuseText(tx, prep, mid, quoteStage)); return }
        // A MARK IN SCOPE STAYS LOCKED (port 62 slice L2; the prototype's setLock, index61c2.html 5053): its padlock
        // and the menu's Unlock answer "FW1 is in Scope — take it Out of scope in its sheet", with no UNDO and no
        // undo entry (SurveyPrepEdit.setLocked refuses the unlock as well)
        val scopeNo = SurveyPrepEdit.unlockRefusal(tx, prep, mid)
        if (scopeNo != null) { ctx = null; snack(scopeNo, undo = false); return }
        ctx = null; snapshot(); SurveyPrepEdit.setLocked(prep, mid, null); changed()
    }
    /** 1 · Lines' padlock: the open face's four lines locked or unlocked, said for 2.6 s. */
    fun toggleLinesLock() {
        snapshot(); val v = SurveyPrepEdit.setLinesLocked(prep, tab, null)
        say(SurveyPrepEdit.linesSaid(tx, tab, v)); changed()
    }
    /**
     * 1 · LINES' SIZE FROM ROW (v52 slice 12, item 13; sp.js setRefMode): Reference ▭ ([on]) squares the open face from its
     * reference rectangle -- placed the first time at SurveyRef.refDefaultQuad -- and Edges · laser from its lines and the
     * laser; switching back and forth keeps the corners, the W × H and the eye. One undo step; none when nothing changes.
     */
    fun setRefMode(on: Boolean) {
        val s = tab
        if (s !in SurveyPrep.SURFACES) return
        snapshot()
        if (!SurveyRef.setMode(prep, s, on, roomSize[0], roomSize[1], roomSize[2])) { hist.removeAt(hist.size - 1); return }
        if (!on && SurveyRef.isId(sel?.id)) sel = null
        refShot = SurveyRef.leave(refShot)
        changed()
    }
    /** The eye, "Show on faces" (sp.js setRefShown): one undo step, and it says what it did. */
    fun toggleRefShown() {
        val s = tab
        if (s !in SurveyPrep.SURFACES) return
        snapshot()
        val v = SurveyRef.setShown(prep.surface(s), null)
        if (v == null) { hist.removeAt(hist.size - 1); return }
        say(SurveyRef.shownSaid(tx, s, v)); changed()
    }
    /** A reference box's own D2 (sp.js data-reffire): the next reading is that box's -- its W or H, never a reading. */
    fun fireRef(k: String) {
        val s = tab
        if (s !in SurveyPrep.SURFACES || SurveyRef.of(prep.surface(s))?.q == null) return
        refShot = SurveyRef.arm(s, k, System.currentTimeMillis()); rev++
        distoGo()
    }
    /** Lock all / Unlock all of type [t] on [s] -- or, with [kind], every tape or every Line mark (v52 slice 9): "Locked
     *  N items · front wall" with UNDO; no change, no undo entry. */
    fun lockAllOf(s: String, t: String?, on: Boolean, kind: String? = null) {
        // LOCK ALL IS A SURVEY TOOL (slice L4; sp.js lockAll's `!P54.spBulk()`): outside Survey -- where the menu still
        // offers it on a mark the phase lets be edited, as the prototype's does -- it says so and locks nothing
        if (!PhaseTools.bulk(quoteStage)) { phaseRefuse(PhaseTools.refuseBulk(tx, quoteStage)); return }
        ctx = null; snapshot()
        val n = SurveyPrepEdit.lockAll(prep, s, t, on, kind)
        // UNLOCK ALL SKIPS SCOPE MARKS (port 62 slice L2; the prototype's lockAll, index61c2.html 5055-5056): they stay
        // locked, and the snackbar says how many -- after what it unlocked, or alone and with no UNDO when it unlocked nothing
        val scopeKept = SurveyPrepEdit.scopeKept(prep, s, t, on, kind)
        if (n == 0) {
            hist.removeAt(hist.size - 1)
            if (scopeKept > 0) snack(MarkLife.scopeKeptText(tx, scopeKept), undo = false)
            return
        }
        snack(SurveyPrepEdit.lockedSnack(tx, on, n, s, scopeKept)); changed()
    }
    /** Duplicate: the copy 100 mm away with the next code, picked, its first measurement active (sp.js duplicateItem). */
    fun duplicateMark(mid: String) {
        val s = prep.itemById(mid)?.s ?: return
        // only what the phase lets be edited is copied (slice L4; sp.js duplicateItem's `!P54.spEdit(it)`). THE COPY IS
        // THE BOX ALONE, as the prototype's is -- never stamped "as …" (SurveyPrepEdit.duplicate; DESIGN-port62 Q-list 8)
        if (!PhaseTools.editable(prep, mid, quoteStage)) { phaseRefuse(PhaseTools.refuseText(tx, prep, mid, quoteStage)); return }
        // ... AND A MARK IS COPIED ONLY WHERE THAT UNSTAMPED BOX IS WHAT A TAP WOULD PLACE (the L4 review, finding 1;
        // PhaseTools.copyOk): at Survey, and while the stage is unread with the chip on "as Existing". Anywhere else the
        // copy of a mark born a wish would leave the office as a thing that is in the room. The row is not on the menu
        // there; a press that still arrives makes nothing, takes no undo step and says nothing -- the prototype has no
        // line for it (Q-list 8)
        if (!PhaseTools.copyOk(prep, mid, quoteStage, asPicked)) { phaseRefuse(null); return }
        val a = ab(s); ctx = null; snapshot()
        val nid = java.util.UUID.randomUUID().toString().take(8)
        if (prep.markById(mid) != null) {
            val c = SurveyPrepEdit.duplicate(prep, mid, a[0], a[1], nid)
            if (c == null) { hist.removeAt(hist.size - 1); return }
            sel = SpSel(s, id = c.id); active = prep.nextOpen(s, c, null, a[0], a[1])
        } else if (prep.chainById(mid) != null) {
            // a chain (v52 slice 11): 100 mm across its axis, the next K number, picked, its first segment active
            val c = SurveyPrepEdit.duplicateChain(prep, mid, a[0], a[1], nid)
            if (c == null) { hist.removeAt(hist.size - 1); return }
            sel = SpSel(s, id = c.id); active = prep.nextOpen(c, null, a[0], a[1])
        } else {
            // a Tape, a Line or an area (v52 slice 9): the next number of its own prefix, picked, its reading active
            val c = SurveyPrepEdit.duplicateMeas(prep, mid, a[0], a[1], nid)
            if (c == null) { hist.removeAt(hist.size - 1); return }
            sel = SpSel(s, id = c.id); active = SurveyMeas.key(c)
        }
        changed()
    }
    /** A menu row picked (SurveyPrepEdit.menuItems). */
    fun ctxPick(mid: String, k: String) {
        // a row the phase hides is not on the menu (SurveyPrepEdit.menuItemsOf with the stage); each act asks again
        when (k) {
            SurveyPrepEdit.DELETE -> deleteMark(mid)
            SurveyPrepEdit.DUPLICATE -> duplicateMark(mid)
            SurveyPrepEdit.LOCK -> toggleLock(mid)
            // a mark's type, or every tape, or every Line mark -- never the other (SurveyPrepEdit.lockGroupOf)
            SurveyPrepEdit.LOCK_ALL -> SurveyPrepEdit.lockGroupOf(prep, mid)?.let { (s, g) ->
                lockAllOf(s, g.t, !g.all, g.kind) } ?: run { ctx = null }
            else -> ctx = null
        }
    }
    /** A chain picked (sp.js kPick): selected; on a FRESH pick its first open reading becomes the one being taken (the
     *  laser's total skipped) -- or [measure], when a list row named it (v52 slice 11). */
    fun selectChain(s: String, cid: String, measure: String? = null) {
        val c = prep.chainById(cid)?.second ?: return
        val fresh = sel?.id != cid
        sel = SpSel(s, id = cid)
        if (measure != null) active = measure
        else if (fresh) active = ab(s).let { prep.nextOpen(c, null, it[0], it[1]) }
        if (tab != s && tab != "list") { tab = s; if (mode != "360") modeSet = "mark"; dropDrafts() }
        rev++
    }
    /** A Tape, a Line or an area picked (sp.js mPick): selected; a newly picked Line's reading becomes the one being
     *  taken -- or [measure], when a list row named it. A tape has none. */
    fun selectMeas(s: String, mid: String, measure: String? = null) {
        val m = prep.measById(mid)?.second ?: return
        val fresh = sel?.id != mid
        sel = SpSel(s, id = mid)
        active = SurveyMeas.pickActive(m, fresh, measure, active)
        if (tab != s && tab != "list") { tab = s; if (mode != "360") modeSet = "mark"; dropDrafts() }
        rev++
    }
    /**
     * A TAPE OR A LINE LET GO (v52 slice 9, sp.js measureDown.end): off the surface it is REFUSED -- "Only things on
     * this surface measure correctly.", nothing plotted, NO undo entry; under 20 mm nothing happens; otherwise it is
     * plotted with its next code (F-T1, F-M1 …), picked, and a Line's reading is the one being taken. One ↶ step.
     */
    fun endDrag(d: SurveyMeas.Draft) {
        mdrag = null
        when (SurveyMeas.end(d)) {
            SurveyMeas.End.REFUSED -> snack(SurveyMeas.offRefusal(tx), undo = false)
            SurveyMeas.End.SHORT -> Unit
            SurveyMeas.End.PLOTTED -> {
                snapshot()
                val m = prep.addMeas(d.s, d.kind, d.pts(), java.util.UUID.randomUUID().toString().take(8))
                sel = SpSel(d.s, id = m.id); active = SurveyMeas.key(m)
                changed()
            }
        }
        rev++
    }
    /**
     * A TAP WITH LINE ON BUILDS AN AREA (sp.js polyTap): the first corner starts it, each tap adds one, and a double tap
     * -- or a tap on the first corner ([nearFirst], within 18 dp) -- once it has three closes it as F-M2 …, picked, its
     * perimeter the reading to take. A corner off the surface is refused like a drag. [mm] where the finger went down.
     */
    fun areaTap(mm: DoubleArray, nearFirst: Boolean, double: Boolean) {
        val s = tab; val a = ab(s)
        val pts = if (areaS == s) area ?: emptyList() else emptyList()
        when (SurveyMeas.areaTap(pts, mm, a[0], a[1], double, nearFirst)) {
            SurveyMeas.Tap.REFUSED -> snack(SurveyMeas.offRefusal(tx), undo = false)
            SurveyMeas.Tap.STARTED -> { area = listOf(mm); areaS = s }
            SurveyMeas.Tap.ADDED -> area = pts + listOf(mm)
            SurveyMeas.Tap.IGNORED -> Unit
            SurveyMeas.Tap.CLOSED -> {
                snapshot()
                val m = prep.addMeas(s, SurveyMeas.AREA, pts, java.util.UUID.randomUUID().toString().take(8))
                dropDrafts()
                sel = SpSel(s, id = m.id); active = SurveyMeas.key(m)
                changed()
            }
        }
        rev++
    }
    /**
     * ⋮ CLEAR THIS SURFACE / CLEAR WHOLE ROOM (sp.js armClear / clearItems): the first tap arms ("Tap again to clear
     * N items"), a second within 4 s takes every UNLOCKED item and its readings, in one ↶; the snackbar says how
     * many, and how many locked ones were kept.
     */
    fun clearTap(scope: String) {
        // the clears are Survey tools (slice L4; sp.js clearItems' `!P54.spBulk()`): hidden outside Survey, and refused
        if (!PhaseTools.bulk(quoteStage)) { phaseRefuse(PhaseTools.refuseBulk(tx, quoteStage)); return }
        if (SurveyPrepEdit.clearCount(prep, scope, tab) == 0) return
        if (armedClear != scope) { armedClear = scope; return }
        armedClear = null
        snapshot()
        val r = SurveyPrepEdit.clearItems(prep, scope, tab)
        if (r.n == 0) { hist.removeAt(hist.size - 1); return }
        sel = null; active = null; menuOpen = false; ctx = null; dropDrafts()
        // a mark past Existing is kept, locked or not, and the snackbar names it (port 62 slice L2; SurveyPrepEdit.clearedSnack)
        snack(SurveyPrepEdit.clearedSnack(tx, r))
        changed()
    }
    fun nudgeLine(slot: String, mm: Double, deg: Double) {
        val s = tab; val sf = prep.surface(s)
        if (SurveyPrep.linesLocked(sf)) { say(SurveyPrepEdit.linesRefusal(tx)); return }
        sel = SpSel(s, line = slot)
        if (mm != 0.0) {
            val moved = SurveyPrep.nudgeLine(sf.lines.getValue(slot), slot, frameOf(s), mm)
            if (moved == null) { say(tx.refuse_sp_these_four_lines_do); return }
            snapshot(); sf.lines[slot] = moved
        } else {
            snapshot(); sf.lines[slot] = SurveyPrep.turnLine(sf.lines.getValue(slot), deg)
        }
        sf.set[slot] = true
        changed()
    }
    // a face or a step changed: back to Select, the More sheet shut, a chain half drawn ended first (sp.js showTab /
    // setMode: kFlush, then gClear)
    fun showTab(t: String) { flushChain(); refShot = SurveyRef.leave(refShot); if (StepRoute.isPlan(t)) planFit++ else planExp = false
        // on 4 · 360 a face tab turns the 360 to that face (sp.js showTab: P3.face)
        if (mode == "360" && StepRoute.isFace(t)) p3Look = Pano360.faceView(t, 1.0, 1.0)
        tab = t; sel = null; menuOpen = false; toolSet = SurveyPrepPalette.SEL; moreOpen = false; ctx = null; armedClear = null; dropDrafts(); rev++ }
    fun setMode(m: String) {
        // a step the phase does not show cannot be opened (slice L4): it is not on the strip, and a press routed to
        // where it was, or a tour's move, does nothing
        if (m !in PhaseTools.prepSteps(quoteStage)) return
        if (m == "mark" && frameOf(tab) == null) { say(tx.refuse_sp_these_four_lines_do_2); return }
        flushChain(); refShot = SurveyRef.leave(refShot)
        // 4 · 360 opens facing the open face (sp.js setMode: P3.face)
        if (m == "360") p3Look = Pano360.faceView(tab, 1.0, 1.0)
        modeSet = m; sel = null; toolSet = SurveyPrepPalette.SEL; moreOpen = false; ctx = null; dropDrafts(); rev++
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
        say(prep.outsideNote(tx, z[0], z[1], z[2])
            ?: tx.sp_room_size_l_x_2(z0 = z[0].roundToInt().toString(), z1 = z[1].roundToInt().toString(), z2 = z[2].roundToInt().toString()))
        changed()
    }

    val done = SurveyPrep.SURFACES.count { prep.surface(it).done() }
    // where the size line and the screen are, for placing the size sheet under the line
    var rootTop by remember { mutableStateOf(0f) }
    var rootH by remember { mutableStateOf(0) }
    // ... and for keeping the long-press menu on the screen: the root's left edge and width, the menu's own size (px)
    var rootLeft by remember { mutableStateOf(0f) }
    var rootW by remember { mutableStateOf(0) }
    var ctxSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
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
    // a FACE has lines, steps, marks and its own clear; the List and Plan tabs have none of them (slice 13)
    val face = StepRoute.isFace(tab)
    // 2 · Mark and 4 · 360 edit the same marks: the selection rows, the D2 strip, the palette and More ▸ serve both
    val marking = face && (mode == "mark" || mode == "360")
    val planTab = StepRoute.isPlan(tab)
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
            // ... of the steps the strip draws now: a step the phase hides keeps no touch area (slice L4)
            PhaseTools.prepSteps(quoteStage).mapNotNull { k ->
                stepBoxes[k]?.let { r -> StepRoute.Step(k, r.left.toDouble(), r.top.toDouble(), r.right.toDouble(), r.bottom.toDouble()) } },
            stripAt.y.toDouble(), dens.density.toDouble())
    // The long-lived pointer handlers below read these, so a handler started on an earlier frame acts on this one.
    val routeS by rememberUpdatedState(newValue = { x: Float, y: Float -> stepAt(x, y) })
    val goStepS by rememberUpdatedState(newValue = { k: String -> setMode(k) })

    // ---- 4 · 360 (v52 slice 17): what a press on the 360 does -- the SAME edits 2 · Mark makes, through the same
    // functions, into the same prep fields; only the surface comes from the press (the tab follows it, sp.js st.tab=s)
    val p3Actions = P3Actions(
        grab = { g -> grabbed(g) },
        focus = { s -> if (tab != s) { tab = s; rev++ } },
        before = { snapshot() },
        moved = { rev++ },
        changed = { changed() },
        revert = { revertPress() },
        deselect = { if (sel != null) { sel = null; rev++ } },
        place = { s, t, mx, my -> place360(s, t, mx, my) },
        pickChain = { s, cid -> selectChain(s, cid) },
        pickMeas = { s, mid -> selectMeas(s, mid) },
        chainPress = { s, mm, tol -> tab = s; chainPress(mm, tol) },
        chainMove = { i, mm, tol -> chainMove(i, mm, tol) },
        chainCancel = { i -> chainCancel(i) },
        chainFinish = { kdraft?.let { finishChain(it) } },
        dragStart = { d -> mdrag = d; rev++ },
        dragMove = { rev++ },
        dragEnd = { d -> endDrag(d) },
        dragCancel = { mdrag = null; rev++ },
        areaTap = { s, mm, near, dbl -> tab = s; areaTap(mm, near, dbl) },
        tapeTap = { say(SurveyMeas.tapeTap(tx)) },
        endGuide = { g -> endG = g; rev++ },
        say = { t -> say(t) },
        refuse = { code -> refuse(code) },
        delete = { mid -> deleteMark(mid) },
        longPress = { mid, at -> armedClear = null; ctx = SpCtx(mid, at.x, at.y) },
        look = { v -> p3Look = v },
        // ✕ on the chip: a chain half drawn is DROPPED, not plotted (sp.js d.cancel), then back to Select
        tool = { t -> kdraft = null; pickTool(t) },
        grid = { grid = !grid; rev++ },
        stepAt = { x, y -> routeS(x, y) },
        step = { k -> goStepS(k) },
    )

    val statusText = when {
        tab == "list" || planTab -> {
            val marks = SurveyPrep.SURFACES.sumOf { prep.surface(it).marks.size }
            val items = SurveyPrep.SURFACES.sumOf { s -> prep.surface(s).marks.sumOf { e -> SurveyPrep.measures(s, e, ab(s)[0], ab(s)[1]).size } }
            tx.sp_engineer_s_sheet_x(marks = marks, items = items, v = prep.actual.size.toString())
        }
        mode == "lines" -> {
            val left = SurveyPrep.SLOTS.filter { prep.surface(tab).set[it] != true }.map { SurveyPrep.lineName(tx, tab, it) }
            // the names are joined with ", " in both languages, as sp.js joins them
            if (left.isNotEmpty()) tx.sp_place_x(left.joinToString(", ")) else tx.sp_all_four_lines_placed
        }
        mode == "list" -> {
            val els = prep.surface(tab).marks
            val n = els.sumOf { e -> SurveyPrep.measures(tab, e, ab(tab)[0], ab(tab)[1]).size }
            // the prototype's wording wins (decision 5): it always writes "measurements"
            tx.sp_x_x_mark_x(T = SurveyPrep.NAME[tab]?.let { Masters.surface(tx, it) } ?: tab, length = els.size, v = n.toString())
        }
        // "surface" (the floor, the ceiling) or "wall" is an English word chosen in code: one whole key each
        else -> if (SurveyPrep.isPlan(tab)) tx.sp_squared_laser_add_surface else tx.sp_squared_laser_add_wall
    }
    val openShape = face && mode == "lines" && frameOf(tab) == null
    val boundedText = tx.sp_x_6_surfaces_bounded(done.toString())

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
            SpText("←", SP_FG, 20, Modifier.onGloballyPositioned { backBox = it.boundsInRoot() } // text:value glyph
                .clip(CircleShape).clickable { back() }
                .padding(horizontal = 8.dp, vertical = 2.dp))
            Column(Modifier.weight(1f)) {
                Text(tx.sp_x_survey_prep(title), color = SP_FG, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(savedMsg(tx), color = SP_OK, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            SpText("↶", if (hist.isEmpty()) SP_DIM else SP_FG, 20, // text:value glyph
                Modifier.clip(CircleShape).clickable(enabled = hist.isNotEmpty()) { undo() }.padding(horizontal = 6.dp, vertical = 2.dp))
            Text("⋮", color = SP_FG, fontSize = 20.sp, fontWeight = FontWeight.Bold, // text:value glyph
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (menuOpen) SP_LINE_BORDER else Color.Transparent)
                    .clickable { menuOpen = !menuOpen }
                    .semantics { contentDescription = tx.sp_aria_more_status_start_over }
                    .padding(horizontal = 8.dp, vertical = 2.dp))
            // ✓ SAVE: gold once all six surfaces are bounded, and only then does it do anything
            Box(Modifier.padding(start = 2.dp).onGloballyPositioned { saveBox = it.boundsInRoot() }
                // the guided tour's `save` anchor (port 61c §31): shown, never pressed (a look step)
                .tourAnchor("save", TourState.HOST_SURVEY)
                .height(if (land) 28.dp else 32.dp).clip(RoundedCornerShape(16.dp))
                .background(if (done == 6) SP_GOLD else SP_LINE_BORDER)
                .clickable(enabled = done == 6) { saveNow() }
                .semantics { contentDescription = tx.sp_save_aria }
                .padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                Text(tx.sp_save, color = if (done == 6) Color(0xFF1A1A12) else Color(0xFF7D8278), fontWeight = FontWeight.Bold,
                    fontSize = 13.sp, maxLines = 1)
            }
        }
    }

    // The line that STAYS on screen when the saved prep could not be read whole (slice 2): never behind ⋮.
    val openedLine: @Composable () -> Unit = {
        prepNote?.let { Text(it(tx), color = SP_AMB, fontSize = 11.sp, lineHeight = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 4.dp)) }
    }

    // ---- THE PARENT: the faces as text tabs, two rows, the open one white and bold over a 3 dp gold bar
    val tabsUi: @Composable () -> Unit = {
        rev    // read: a drag only bumps rev, and the ✓ on a face that just closed must follow it live
        val rowH = StepRoute.tabRowDp(land, listView, tx.mr).dp   // 27 in a landscape list under मराठी: SpTab's 17 sp name + a 9 sp Ref line
        Column(Modifier.fillMaxWidth()) {
            for (row in StepRoute.TAB_ROWS) {
                Row(Modifier.fillMaxWidth().height(rowH).drawBehind {
                    drawLine(SP_LINE_BORDER, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), strokeWidth = 1f)
                }) {
                    for (t in row) {
                        val tick = StepRoute.tabTick(t, StepRoute.isFace(t) && prep.surface(t).done())
                        // a face squared from its reference says so under its name, "Ref 905 × 2053" (v52 slice 12) --
                        // shown or hidden on the faces, since it squares the face either way
                        val refT = if (t in SurveyPrep.SURFACES) SurveyRef.tabText(tx, prep.surface(t)) else null
                        // the guided tour's `tab:<tab>` anchor (port 61c §31), in the dialog's own window
                        SpTab(StepRoute.tabName(tx, t), tab == t, tick,
                            Modifier.weight(1f).fillMaxHeight().tourAnchor("tab:$t", TourState.HOST_SURVEY),
                            ref = refT, land = land) { showTab(t) }
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
            Text(StepRoute.stripFace(tx, tab), color = SP_MUTED, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, softWrap = false,
                modifier = Modifier.padding(end = 6.dp))
            // ONLY THE STEPS THE PHASE SHOWS (port 62 slice L4; phase54.css 1272-1274 hides a step's circle and the join
            // after it): each keeps its own number, so with 2 · Mark gone the strip reads 1 · 3 · 4
            PhaseTools.prepSteps(quoteStage).forEachIndexed { n, k ->
                val i = StepRoute.stepIndex(k)
                if (n > 0) Box(Modifier.weight(1f).height(1.5.dp).background(SP_JOIN))
                val on = i == ci
                val past = ci >= 0 && i < ci
                val lab = StepRoute.label(tx, i)
                Row(Modifier.height(if (tight) 20.dp else if (land) 22.dp else 26.dp)
                    .onGloballyPositioned { stepBoxes[k] = it.boundsInRoot() }
                    // the guided tour's `mode:<step>` anchor (port 61c §31)
                    .tourAnchor("mode:$k", TourState.HOST_SURVEY)
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
                    if (on) Text(StepRoute.stepName(tx, k), color = SP_GOLD, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold,
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
            Text(tx.common_show, color = SP_MUTED, fontSize = 11.sp, lineHeight = 14.sp)
            for ((k, lab) in listOf("derived" to tx.common_photo, "actual" to tx.common_measured, "both" to tx.common_both)) {
                SpPill(lab, show == k, Modifier.weight(1f), outline = true, compact = land) { show = k; rev++ }
            }
        }
    }

    // the surface's title is a caption ABOVE the photo, never on it; it takes part in the strip's touch area. The lines'
    // padlock (slice 8) moved from its right end into 1 · Lines' Size from row with slice 12, beside the reference's own
    // padlock and its eye, where the prototype has it (sp.js 1512-1514, ct52 X4)
    val caption: @Composable () -> Unit = {
        rev    // read: the size follows the reference's W × H and corners
        val ab0 = ab(tab); val tags = SurveyPrep.sizeTag(tab)
        val faceName = SurveyPrep.NAME[tab]?.let { Masters.surface(tx, it) } ?: tab
        Row(Modifier.fillMaxWidth().onGloballyPositioned { capAt = it.positionInRoot() }
            .pointerInput(Unit) { routeToStep({ capAt }, { x, y -> routeS(x, y) }, { k -> goStepS(k) }) },
            verticalAlignment = Alignment.CenterVertically) {
            // 2 · Mark's caption is the face, the two axis letters and the size: no word of its own, in either language
            Text(if (mode == "lines") tx.sp_x_set_four_lines_2(faceName)
                 else if (mode == "360") Pano360.caption(tx, tab)
                 else "$faceName · ${tags.first} ${ab0[0].roundToInt()} × ${tags.second} ${ab0[1].roundToInt()}",
                color = SP_FG, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 6.dp, top = 3.dp, bottom = 3.dp))
        }
    }

    // THE NOTE AND THE SNACKBAR over the bottom of the photo (or the list), the note above the snackbar: neither
    // costs any height (v52 slice 8 keeps slice 7's dp budget). UNDO undoes the change the snackbar names.
    // THE BAND IN LANDSCAPE lies over the foot of the left column -- the photo, the list or the drawing -- so the
    // right-hand column loses no height (port 62 slice L4; phase54.css 1269-1270); the note and the snackbar are drawn
    // after it, over it, as the prototype stacks them. In portrait it is the screen's first line, over the bar.
    val bandOver: @Composable BoxScope.() -> Unit = {
        if (land) SpPhaseBand(band, true, Modifier.align(Alignment.BottomCenter))
    }
    val notesOver: @Composable BoxScope.() -> Unit = {
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            note?.let { SpNote(it, Modifier) }
            snackText?.let { t -> SpSnack(t, snackUndo, Modifier.fillMaxWidth()) { if (snackText != null && snackUndo && hist.size == snackHist) { hideSnack(); undo() } else hideSnack() } }
        }
    }

    // ---- THE PHOTO, with all the height the controls leave (1 · Lines and 2 · Mark)
    val photoPane: @Composable ColumnScope.() -> Unit = {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            // 1 · Lines: the wall's cut, drawn at its offset, fitted to the surface its lines enclose and 12° beyond its
            // corners (sp.js fitRectFor); 2 · Mark: the squared face, fitted whole
            val cut = if (mode == "lines") views[tab] else null
            val img: Bitmap? = if (mode == "lines") cut?.bmp else rectOf(tab)?.bmp
            val fitTo: DoubleArray? = cut?.let { c -> SurveyPrep.fitRectFor(tab, prep.surface(tab).lines, frameOf(tab)?.q, c.ext) }
            if (mode == "360") {
                // 4 · 360 (v52 slice 17): the same marks on the 360 itself, edited through the same functions as 2 · Mark
                Sp360(Modifier.fillMaxSize(), previewPano, p3Look, tab, rev, prep, roomSize, sel, active, tool,
                    mdrag, area, areaS, kdraft, SurveyGuide.now(mdrag, endG, areaS, area, tool, kdraft), grid, p3Actions,
                    stage = quoteStage, asPicked = asPicked, onAs = { cycleAs() })
            } else if (img == null) {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                SpPhoto(
                    modifier = Modifier.fillMaxSize(),
                    img = img, ox = (cut?.ox ?: 0).toDouble(), oy = (cut?.oy ?: 0).toDouble(), fitTo = fitTo,
                    tab = tab, mode = mode, rev = rev,
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
                    // ✕ on the chip: a chain half drawn is DROPPED, not plotted (sp.js d.cancel), then back to Select
                    onTool = { t -> kdraft = null; pickTool(t) },
                    onLongPress = { mid, at -> armedClear = null; ctx = SpCtx(mid, at.x, at.y) },
                    onRefuse = { code -> refuse(code) },
                    onLinesLocked = { say(SurveyPrepEdit.linesRefusal(tx)) },
                    onRevert = { revertPress() },
                    drag = mdrag, area = if (areaS == tab) area else null,
                    onDragStart = { d -> mdrag = d; rev++ },
                    onDragMove = { rev++ },
                    onDragEnd = { d -> endDrag(d) },
                    onDragCancel = { mdrag = null; rev++ },
                    onAreaTap = { mm, near, dbl -> areaTap(mm, near, dbl) },
                    onTapeTap = { say(SurveyMeas.tapeTap(tx)) },
                    onPickMeas = { mid -> selectMeas(tab, mid) },
                    // v52 slice 11: the chain being drawn, its taps, a second finger, its end (a double tap or ✓), a pick
                    chain = kdraft,
                    onChainPress = { mm, tol -> chainPress(mm, tol) },
                    onChainMove = { i, mm, tol -> chainMove(i, mm, tol) },
                    onChainCancel = { i -> chainCancel(i) },
                    onChainFinish = { kdraft?.let { finishChain(it) } },
                    onPickChain = { cid -> selectChain(tab, cid) },
                    guide = SurveyGuide.now(mdrag, endG, areaS, area, tool, kdraft),
                    grid = grid,
                    onGrid = { grid = !grid; rev++ },
                    onEndGuide = { g -> endG = g; rev++ },
                    onSay = { t -> say(t) },
                    onBusy = { b -> photoBusy = b },
                    // port 62 slice L4: the phase -- which marks may be changed, and the chip's "as …"
                    stage = quoteStage, asPicked = asPicked, onAs = { cycleAs() })
            }
            bandOver()
            notesOver()
        }
    }

    // one surface squared, with codes and dimensions, and its table: every surface on the room's List tab, the open
    // face alone on its 3 · List
    val surfaceBlock: @Composable (String, Boolean) -> Unit = { s, room ->
        val a = ab(s); val tg = SurveyPrep.sizeTag(s)
        val act = active
        val actHere = act != null && prep.ownerOf(act)?.s == s
        val faceName = Masters.surface(tx, SurveyPrep.NAME.getValue(s))
        Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp)).background(SP_SHEET).padding(6.dp)) {
            Row(Modifier.fillMaxWidth().padding(2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (room) faceName else tx.sp_x_its_list(faceName), color = SP_FG,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                // squared from its reference rectangle, its size is the reference's measure, not the laser's (v52 slice 12);
                // "Ref" (common.ref) is the card's row for the phone's "ref" (decision 5); "Laser" (app.laser) is Claude's extension of
                // it to the tag's other branch (decision 6) -- the card's only `· laser` row is the chain total's, keyed whole as sp.total_laser
                if (room || !actHere) Text("${tg.first} ${a[0].roundToInt()} × ${tg.second} ${a[1].roundToInt()} · ${if (frameOf(s)?.ref != null) tx.common_ref else tx.app_laser}",
                    color = SP_MUTED, fontSize = 12.sp)
                else SpD2(Modifier, "D2 → $act") { fire() } // text:value the laser (DISTO D2) and the reading key it is armed for
            }
            val r = if (room) { if (prep.surface(s).done()) rectOf(s) else null } else rectOf(s)
            when {
                // the prototype names the surface in both lines (decision 5: sp.x_set_four_lines_2, sp.x_set_four_lines)
                room && !prep.surface(s).done() -> Text(tx.sp_x_set_four_lines_2(faceName), color = SP_MUTED, fontSize = 12.sp, modifier = Modifier.padding(4.dp))
                !room && frameOf(s) == null -> Text(tx.sp_x_set_four_lines(faceName), color = SP_MUTED, fontSize = 12.sp,
                    modifier = Modifier.padding(4.dp))
                r == null -> CircularProgressIndicator(Modifier.padding(12.dp))
                else -> Canvas(Modifier.fillMaxWidth().aspectRatio(r.sq.width.toFloat() / r.sq.height).clip(RoundedCornerShape(8.dp))) {
                    rev; show; active
                    val sc = size.width / r.sq.width
                    drawIntoCanvas { cv ->
                        val nc = cv.nativeCanvas
                        nc.save(); nc.scale(sc, sc); nc.drawBitmap(r.bmp, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG)); nc.restore()
                        // the List's picture is the office's own screen: every mark, with its state (port 62 slice L2)
                        SurveyPrepDraw.drawMarks(nc, tx, SheetMarks.Audience.OFFICE, s, SurveyPrepDraw.Marks(prep, a[0], a[1], r.sq, "sheet", true, null, active, show,
                            size.width, size.height, null, density, frame = frameOf(s)),
                            { x, y -> floatArrayOf((x * sc).toFloat(), (y * sc).toFloat()) }, sc)
                    }
                }
            }
            if (!room || prep.surface(s).done()) {
                SpTable(s, prep, a, rev, sel, active, show, room, quoteStage,
                    onRow = { e, code -> selectMark(s, e, code) },
                    onMeasRow = { m, code -> selectMeas(s, m.id, code) },
                    onChainRow = { k, code -> selectChain(s, k.id, code) },
                    onSheet = { openSheet(it) },
                    onDelete = { deleteMark(it) },
                    onFocus = { active = it },
                    onValue = { code, v -> if (v != null) reading(code, v) else { prep.actual.remove(code); changed() } },
                    onFire = { code -> active = code; fire() },
                    emptyText = if (room) tx.sp_nothing_marked_surface else tx.sp_no_marks_yet_add)
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
            bandOver()
            notesOver()
        }
    }

    // EXPORT (v52 slice 14a, item 10): the drawing shown, on its own A4 sheet (SurveyExport: 1:50 or the first coarser
    // 1:10n that fits, the title block 'Drawing <DTITLE>', the two footnotes), saved to Downloads as SVG or PDF and
    // shared -- only ever because a button on the export bar was pressed, never on Save or sync. The file is named from
    // the client's initials, the room's token and the project's code (SurveyExport.exportName); the phone says what it
    // saved in the snackbar (the prototype's "On the phone this saves …" note is the prototype's alone).
    fun planName(): String = SurveyExport.exportName(clientInitials, SurveyExport.roomToken(title), projectCode, planV)
    /** The drawing shown, on its sheet, from the prep and the room size as they stand NOW (main thread; it is cheap).
     *  Of the prep's marks the sheet draws THE ROOM AS SURVEYED -- it is saved and shared -- which SurveyExport.sheet
     *  decides (SheetMarks.prep; port 62, the L2 review): this screen's Plan tab above draws the prep itself. */
    fun planSheet(): SurveyExport.Sheet =
        SurveyExport.sheet(planV, prep, roomSize[0], roomSize[1], roomSize[2],
            SurveyExport.Meta(clientInitials, projectCode, title, captureDate,
                SurveyExport.drawn(LocalDate.now(ZoneId.of("Asia/Kolkata")))), SurveyPlan.roomName(title))
    /**
     * SVG or PDF: saved to Downloads (and kept privately), the snackbar naming what Downloads called it. Share
     * ([thenShare]): the sheet as it stands now, written to the private copy ONLY -- never an older file, so a mark moved
     * or a figure keyed since the last save is in what is sent, and sharing never piles copies into Downloads.
     */
    /** A save or a share that failed: "Not shared: {why}" (sp.msg.not_shared_x), or the fragment "Not saved: " (sp.msg.not_saved)
     *  and the why -- a PlanSave.Refused rendered with the live tx (M3c), any other Throwable the system's own words,
     *  which stay the system's -- the prototype's fragment carries the colon (the card, 28 Oct 2026). */
    fun exportFailed(thenShare: Boolean, e: Throwable) {
        val why = if (e is PlanSave.Refused) PlanSave.text(tx, e.kind, e.file) else e.message ?: e.javaClass.simpleName
        snack(if (thenShare) tx.sp_msg_not_shared_x(why) else tx.sp_msg_not_saved + why, undo = false)
    }
    fun exportPlan(ext: String, thenShare: Boolean) {
        if (planBusy) return
        // the sheet is built here, on the main thread, from the prep as it stands; only the bytes and the files are not
        val sheet = runCatching { planSheet() }
            .getOrElse { e -> exportFailed(thenShare, e); return }
        planBusy = true
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = if (ext == PlanExport.SVG) PlanExport.svgBytes(sheet) else PlanExport.pdfBytes(sheet)
                    if (thenShare) PlanExport.keepOnly(context, id, sheet.name, ext, bytes)
                    else PlanExport.save(context, id, sheet.name, ext, bytes)
                }
            }
            planBusy = false
            r.onSuccess { sv ->
                if (thenShare) runCatching { PlanExport.share(context, sv, tx.export_share_x(sv.file.name)) }
                    .onFailure { e -> snack(tx.sp_msg_could_not_share_x_x(file = sv.shown, why = e.message ?: e.javaClass.simpleName), undo = false) }
                else { planSaved = sv; snack(tx.sp_msg_saved_x_downloads(sv.shown), undo = false) }
            }.onFailure { e ->
                // a failed save says nothing about what Share should send: forget the last one
                if (!thenShare) planSaved = null
                exportFailed(thenShare, e)
            }
        }
    }
    /** Share: the drawing as it stands now, in the format last saved for it, else as a PDF (SurveyExport.shareExt). */
    fun sharePlan() = exportPlan(SurveyExport.shareExt(planSaved?.file?.name, planName()), thenShare = true)
    // over the drawing: the export overlay when open (the whole pane, as v42.css .planx), and over it the note and the
    // snackbar as over a photo (notesOver), lifted clear of the overlay's buttons
    val planOver: @Composable BoxScope.() -> Unit = {
        bandOver()
        if (planExp) {
            val sheet = remember(planV, rev, prep, roomSize, title, clientInitials, projectCode, captureDate) { runCatching { planSheet() }.getOrNull() }
            SpPlanExport(sheet?.name ?: planName(), sheet, planBusy, Modifier.matchParentSize(),
                onSvg = { exportPlan(PlanExport.SVG, thenShare = false) },
                onPdf = { exportPlan(PlanExport.PDF, thenShare = false) },
                onShare = { sharePlan() },
                onClose = { planExp = false })
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = if (planExp) 56.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            note?.let { SpNote(it, Modifier) }
            snackText?.let { t -> SpSnack(t, snackUndo, Modifier.fillMaxWidth()) { if (snackText != null && snackUndo && hist.size == snackHist) { hideSnack(); undo() } else hideSnack() } }
        }
    }
    // ---- THE PLAN TAB (v52 slice 13, items 10 and 11): the six drawings in one box with all the height the controls
    // leave -- nothing scrolls -- and the line under it naming the drawing. No step strip, no Show row, no caption, and
    // no Clear this surface behind ⋮ (face is false here); the note and the snackbar sit over the drawing as over a photo.
    val planPane: @Composable ColumnScope.() -> Unit = {
        SpPlanPane(Modifier.weight(1f).fillMaxWidth(), prep, roomSize, SurveyPlan.roomName(title), rev, planV, planFit, land,
            onView = { v -> planV = v }, overlay = planOver)
    }
    val planFoot: @Composable () -> Unit = {
        rev    // read: the mark count follows every change
        SpPlanFoot(SurveyPlan.infoOf(tx, planV, prep, roomSize[0], roomSize[1], roomSize[2], SurveyPlan.roomName(title)),
            onExport = { planExp = true })
    }

    // ---- under the photo in 1 · Lines: EVERY LINE HAS ITS OWN CONTROLS, always there, in a compact panel (Amit,
    // 2026-10-10: "why not every line ... have its own control. currently i have to select it first ... lines are one
    // time settings"), then what is left to place
    val linesPanel: @Composable () -> Unit = {
        rev    // read: a line dragged into place updates 'Place: ...' and the bounded count while the finger is down
        val sfL = prep.surface(tab)
        val rf = SurveyRef.of(sfL)
        val refOnRow = rf?.on == true
        val boxH = if (land) 28.dp else 32.dp
        Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // THE SIZE FROM ROW (v52 slice 12, item 13; sp.js 1511-1514, ct52 X4): Edges · laser | Reference ▭, the lines'
            // padlock (icon only in portrait, and beside the eye in landscape, so both pills keep their whole label) and,
            // with the reference on and placed, its eye "Show on faces". "Size from" is dropped in landscape (v42.css).
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!land) Text(SurveyRef.sizeFrom(tx), color = SP_MUTED, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, softWrap = false,
                    modifier = Modifier.padding(start = 2.dp, end = 4.dp))
                SpPill(SurveyRef.edges(tx), !refOnRow, Modifier.weight(1f), square = true, compact = true,
                    padV = if (land) 5.dp else 7.dp) { setRefMode(false) }
                SpPill(SurveyRef.reference(tx), refOnRow, Modifier.weight(1f), square = true, compact = true,
                    padV = if (land) 5.dp else 7.dp) { setRefMode(true) }
                val eye = refOnRow && rf?.q != null
                if (sfL.done()) {
                    val lk = SurveyPrep.linesLocked(sfL)
                    SpLinesLock(lk, SurveyPrepEdit.linesState(tx, lk), SurveyPrepEdit.linesAction(tx, lk), iconOnly = !land || eye,
                        tall = if (land) 24.dp else 30.dp) { toggleLinesLock() }
                }
                if (eye && rf != null) SpRefEye(rf.shown, land) { toggleRefShown() }
            }
            // ITS W AND H (sp.js refrow2): each a box -- digits only, five at most -- with its own D2, whose reading is the
            // reference's figure and never a reading key; then its own padlock, cyan while unlocked
            if (refOnRow && rf != null) {
                // the face these boxes belong to, taken NOW (slice 12 review): a figure committed after a face change --
                // on focus loss, or as the box leaves -- goes to the face it was typed for, never the one now open
                val sRow = tab
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for ((k, v) in listOf("w" to rf.w, "h" to rf.h)) {
                        Text(k.uppercase(), color = SP_REF, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 14.sp,
                            textAlign = TextAlign.Center, modifier = Modifier.width(14.dp))
                        SpRefField("$sRow|$k", v, SurveyRef.armed(refShot, sRow, k), Modifier.weight(1f).height(boxH)) { t ->
                            SurveyRef.typed(v, t)?.let { c -> setRefDimS(k, c.value, sRow) } }
                        SpD2(Modifier.height(boxH), small = true) { fireRef(k) }
                    }
                    if (rf.q != null) SpPadlock(rf.locked, SurveyRef.lockLabel(tx, tab, rf.locked), Modifier.height(boxH), offColour = SP_REF) {
                        toggleLock(SurveyRef.id(tab)) }
                }
                // what the reference measures the face at, against the laser -- or how to place it
                val (rt, amb) = SurveyRef.refText(tx, tab, prep, roomSize[0], roomSize[1], roomSize[2])
                Text(rt, color = if (amb) SP_AMB else SP_OK, fontSize = if (land) 10.sp else 11.sp, lineHeight = if (land) 12.sp else 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 4.dp))
            }
            for (k in SurveyPrep.SLOTS) {
                val col = Color(SurveyPrepDraw.argb(SurveyPrepDraw.LINE_COLOUR.getValue(k)))
                val h = k == "bottom" || k == "top"
                // the guided tour's `lines:row` anchor (port 61c §31): the FIRST nudge row, as the
                // prototype's resolver takes the first `.lrow`
                Row(Modifier.tourAnchor(if (k == SurveyPrep.SLOTS.first()) "lines:row" else null, TourState.HOST_SURVEY),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(Modifier.width(74.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(col))
                        Spacer(Modifier.width(4.dp))
                        Text(SurveyPrep.lineName(tx, tab, k), color = col, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp, maxLines = 2)
                    }
                    val bs = if (h) listOf("▼5" to -5.0, "▼1" to -1.0, "▲1" to 1.0, "▲5" to 5.0) // text:value glyphs and mm
                             else listOf("◀5" to -5.0, "◀1" to -1.0, "1▶" to 1.0, "5▶" to 5.0) // text:value glyphs and mm
                    for ((lab, v) in bs) SpButton(lab, Modifier.weight(1f), SP_LINE_BORDER, compact = true) { nudgeLine(k, v, 0.0) }
                    SpButton("↺", Modifier.weight(1f), SP_LINE_BORDER, compact = true) { nudgeLine(k, 0.0, -0.1) } // text:value glyph
                    SpButton("↻", Modifier.weight(1f), SP_LINE_BORDER, compact = true) { nudgeLine(k, 0.0, 0.1) } // text:value glyph
                }
            }
            // in landscape the status lives in ⋮, so the lines and the reference keep their room (tools52.css
            // .spv.land.lnmode #status, v52 slice 12): the right column holds the Size from rows without growing
            if (!land) Text(statusText, color = SP_MUTED, fontSize = 11.sp, lineHeight = 13.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
            if (openShape) Text(tx.refuse_sp_these_four_lines_do_3, color = SP_AMB, fontSize = 11.sp, lineHeight = 13.sp,
                modifier = Modifier.padding(start = 4.dp))
            if (!land) Text(boundedText, color = SP_MUTED, fontSize = 10.sp, lineHeight = 12.sp, modifier = Modifier.padding(start = 4.dp))
        }
    }

    // ---- under the photo in 2 · Mark: the selected mark's buttons, the measurement being taken, then the palette
    val markPanel: @Composable () -> Unit = {
        rev    // read: a mark's edge dragged moves its live ≈ value while the finger is down
        val sm = sel?.takeIf { marking && it.s == tab && it.id != null }?.let { sl -> prep.surface(tab).marks.firstOrNull { it.id == sl.id } }
        if (sm != null) {
            val p = sel!!.part
            val edge = p != "body"
            val horiz = edge && (p.contains('t') || p.contains('b')) && !(p.contains('l') || p.contains('r'))
            val vert = edge && (p.contains('l') || p.contains('r')) && !(p.contains('t') || p.contains('b'))
            val what = when (p) {
                "body" -> tx.sp_move
                "l" -> tx.edge_left_edge; "r" -> tx.edge_right_edge; "t" -> tx.edge_top_edge; "b" -> tx.edge_bottom_edge
                else -> if (p.length > 1) tx.sp_corner else p
            }
            // A LOCKED MARK (v52 slice 8, sp.js 1500-1502): "locked" under its code, the nudges disabled, its padlock
            // lit; Delete stays, dimmed, and answers "FD1 is locked — unlock first"
            val lk = SurveyPrep.isLocked(sm)
            // ITS STATE (port 62 slice L2; the prototype's nlAttrs, index61c2.html 2930): a 3 dp bar on the label's left
            // edge in the state's colour -- none for an Existing mark, whose label is v52's
            val lifeBar = MarkLife.barColour(sm)
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = if (land) 2.dp else 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                // ... and A TAP ON IT OPENS THE MARK SHEET (slice L3; nlAttrs' role=button, "Open the Mark sheet for FW1")
                Text("${sm.code}\n${if (lk) tx.sp_locked else what}", color = SP_GOLD, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.width(52.dp).clip(RoundedCornerShape(3.dp)).drawBehind {
                        if (lifeBar != null) drawRect(Color(SurveyPrepDraw.argb(lifeBar)), size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height))
                    }.clickable(role = Role.Button) { openSheet(sm.id) }
                        .semantics { contentDescription = tx.life_aria_open_mark_sheet_x(sm.code) })
                val bs: List<Triple<String, Double, Double>> = when {
                    horiz -> listOf(Triple("▼5", 0.0, -5.0), Triple("▼1", 0.0, -1.0), Triple("▲1", 0.0, 1.0), Triple("▲5", 0.0, 5.0)) // text:value glyphs and mm
                    vert -> listOf(Triple("◀5", -5.0, 0.0), Triple("◀1", -1.0, 0.0), Triple("1▶", 1.0, 0.0), Triple("5▶", 5.0, 0.0)) // text:value glyphs and mm
                    else -> listOf(Triple("◀", -1.0, 0.0), Triple("▶", 1.0, 0.0), Triple("▼", 0.0, -1.0), Triple("▲", 0.0, 1.0)) // text:value glyphs
                }
                // A MARK OF AN EARLIER PHASE IS READ-ONLY (port 62 slice L4; the prototype's spPost, index61c2.html
                // 4257-4260): its nudges, its padlock and its delete are HIDDEN, never greyed -- the label stays, and
                // still opens its Mark sheet. Each of the three is its own rule in PhaseTools.
                if (PhaseTools.editable(sm, quoteStage))
                    for ((lab, dx, dy) in bs) SpButton(lab, Modifier.weight(1f), SP_GOLD, compact = land, enabled = !lk) { nudgeMark(dx, dy) }
                if (PhaseTools.lockOk(sm, quoteStage))
                    SpPadlock(lk, if (lk) tx.sp_aria_unlock_x(sm.code) else tx.sp_aria_lock_x(sm.code)) { toggleLock(sm.id) }
                // Delete, Drop on a mark past Existing, Restore on a Dropped one (port 62 slice L2; the prototype's delWord)
                if (PhaseTools.delOk(sm, quoteStage))
                    SpButton(MarkLife.deleteLabel(tx, sm), Modifier.weight(1.4f).semantics { contentDescription = SurveyPrepEdit.deleteLabel(tx, prep, sm.id) ?: MarkLife.deleteLabel(tx, sm) },
                        SP_GOLD, compact = land, dim = lk) { deleteMark(sm.id) }
            }
        }
        // A PICKED TAPE, LINE OR AREA (v52 slice 9, sp.js 1505-1507): its own row -- "F-T1 · Tape · 1234 mm", "F-M1 ·
        // 1234 mm" once read (else "≈1234 mm"), "F-M2 · 1.86 m²" -- with its padlock and Delete. A tape has NO D2 strip
        // under it (it has no reading); a Line's strip follows, as a mark's does.
        val sq = sel?.takeIf { marking && it.s == tab && it.id != null }?.let { sl -> prep.surface(tab).meas.firstOrNull { it.id == sl.id } }
        if (sq != null) {
            val lk = SurveyPrep.isLocked(sq)
            val tp = sq.kind == SurveyMeas.TAPE
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = if (land) 2.dp else 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // a Line's or an area's code opens its Mark sheet, for its TRADE and its NOTE (slice L3); a tape has none
                Text(sq.code, color = Color(SurveyPrepDraw.argb(if (tp) SurveyMeas.TAPE_COLOUR else SurveyMeas.LINE_COLOUR)),
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp, maxLines = 1,
                    modifier = if (!MarkSheet.opens(prep, sq.id)) Modifier else Modifier.clip(RoundedCornerShape(3.dp))
                        .clickable(role = Role.Button) { openSheet(sq.id) }
                        .semantics { contentDescription = tx.life_aria_open_mark_sheet_x(sq.code) })
                Text(SurveyMeas.rowValue(tx, sq, prep.actual), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    lineHeight = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                // a Tape, a Line and an area are the phase's own tools up to Estimation, where these two go (slice L4)
                if (PhaseTools.lockOk(sq, quoteStage))
                    SpPadlock(lk, if (lk) tx.sp_aria_unlock_x(sq.code) else tx.sp_aria_lock_x(sq.code)) { toggleLock(sq.id) }
                if (PhaseTools.delOk(sq, quoteStage))
                    SpButton(tx.common_delete, Modifier.width(72.dp).semantics { contentDescription = SurveyPrepEdit.deleteLabel(tx, prep, sq.id) ?: tx.common_delete },
                        SP_GOLD, compact = land, dim = lk) { deleteMark(sq.id) }
            }
        }
        // A PICKED CHAIN (v52 slice 11, sp.js 1503, ct52 K): its own row -- "FK1 ≈905 · ≈2405 · Σ 3310 laser", each
        // segment its reading, "2410 d" derived, or the photo's ≈ -- with its padlock and Delete; the D2 strip follows,
        // on its first open segment
        val sk = sel?.takeIf { marking && it.s == tab && it.id != null }?.let { sl -> prep.surface(tab).chains.firstOrNull { it.id == sl.id } }
        if (sk != null) {
            val lk = SurveyPrep.isLocked(sk)
            val kv = ab(tab).let { SurveyChain.vals(sk, prep.actual, it[0], it[1]) }
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = if (land) 2.dp else 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(sk.code, color = SP_CHAIN, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    lineHeight = 16.sp, maxLines = 1)
                Text(SurveyChain.rowValue(tx, kv), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    lineHeight = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                // ... and so is a chain (slice L4)
                if (PhaseTools.lockOk(sk, quoteStage))
                    SpPadlock(lk, if (lk) tx.sp_aria_unlock_x(sk.code) else tx.sp_aria_lock_x(sk.code)) { toggleLock(sk.id) }
                if (PhaseTools.delOk(sk, quoteStage))
                    SpButton(tx.common_delete, Modifier.width(72.dp).semantics { contentDescription = SurveyPrepEdit.deleteLabel(tx, prep, sk.id) ?: tx.common_delete },
                        SP_GOLD, compact = land, dim = lk) { deleteMark(sk.id) }
            }
        }
        // THE MEASUREMENT BEING TAKEN, right under the photo, so nothing need be opened while lasering: a mark's, or a
        // Line's length / an area's perimeter (v52 slice 9: the D2 and the keyboard land on F-M1.len, F-M2.per)
        val act = active
        val am = act?.let { a -> prep.readingOf(a) { ab(it) } }?.takeIf { marking && it.first == tab }
        val mm = am?.second
        if (am != null && mm != null && act != null) {
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp).background(Color(0xFF2A2410)).border(1.5.dp, SP_GOLD).padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("▶ ${act.substringBeforeLast('.')} ${mm.what(tx)}", color = Color.White, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, modifier = Modifier.weight(1f))
                Text(mm.est?.let { "≈" + it.roundToInt() } ?: tx.sp_site_only, color = Color(0xFFCFD3CB), fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp, lineHeight = 15.sp)
                SpMmField(act, prep.actual[act], true, Modifier.width(72.dp), onFocus = { active = act }) { v ->
                    if (v != null) reading(act, v) else { prep.actual.remove(act); changed() } }
                SpD2(Modifier) { fire() }
            }
        }
        // THE PALETTE (v52 slice 7) in place of the old toolbar: 6 + 50 dp (6 + 46 in landscape), where SpMarkToolbar
        // took 48 + 5 -- the photo gives up 3 dp in portrait and gains 1 in landscape
        // ... where the phase has one (port 62 slice L4): at Estimated and at Client approved there is no palette --
        // nothing new is marked there, and the band over the screen says so
        if (PhaseTools.can(PhaseTools.Tool.PALETTE, quoteStage))
            SpPalette(tab, tool, land, onTool = { t -> pickTool(t) }, onMore = { moreOpen = !moreOpen })
    }

    // The sizes half: the status, then the laser L/W/H with its caption. Every Text carries its own line height, or it
    // inherits the theme's 24 sp line (bodyLarge) and the menu outgrows a landscape screen.
    val menuSizes: @Composable () -> Unit = {
        // sp.this_360 upper-cased, as the prototype's CSS draws its header (.spv .spmenu .mh text-transform: uppercase): THIS 360 as
        // the phone always drew it; uppercase() is a no-op on Devanagari, so हा 360 is untouched (M3a review)
        Text(tx.sp_this_360.uppercase(), color = SP_MUTED, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 4.dp))
        // ---- the status
        Column(Modifier.padding(start = 6.dp, end = 6.dp, bottom = 6.dp)) {
            Text(statusText, color = SP_MUTED, fontSize = 12.sp, lineHeight = 15.sp)
            if (openShape) Text(tx.refuse_sp_these_four_lines_do_3, color = SP_AMB, fontSize = 12.sp, lineHeight = 15.sp)
            prep.outsideNote(tx, roomSize[0], roomSize[1], roomSize[2])?.let {
                Text(it, color = SP_OUT, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)) }
            Text(boundedText, color = SP_MUTED, fontSize = 11.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 2.dp))
        }
        // ---- the laser sizes: a tap opens the size sheet under them (v51)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { openLwh() }
            .onGloballyPositioned { c -> val b = c.boundsInRoot(); lwhRowBottom = b.bottom; lwhRowLeft = b.left }
            .padding(4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            for ((k, v, cap) in listOf(Triple("L", roomSize[0], tx.sp_front_back), Triple("W", roomSize[1], tx.sp_left_right), Triple("H", roomSize[2], tx.sp_floor_ceiling))) { // text:value the laser's axis letters
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
            Text("✎", color = SP_MUTED, fontSize = 14.sp, lineHeight = 18.sp) // text:value glyph
        }
        Text(tx.sp_laser_d2_readings_room, color = SP_MUTED, fontSize = 11.sp,
            lineHeight = 14.sp, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
    }
    // The actions half: the web lines (two lines at most each -- a list of web conflicts is cut, not grown), the
    // new-360 note, then Start over and Discard this 360, each still two taps.
    val menuActions: @Composable () -> Unit = {
        rev    // read: the clears' counts follow every change
        webState?.let { st ->
            // the line renders in the live language at draw time (UploadNote.statusLine, port 61c slice M2);
            // the colour reads the state and the ok flag, never the rendered string ("waiting" is the stored value)
            Text(UploadNote.statusLine(tx, st),
                color = if (st.ok) SP_OK else if (st.state == "waiting") SP_MUTED else SP_AMB, fontSize = 11.sp, lineHeight = 14.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
        }
        // a re-file by work S5 refused (FW2 review): the phone files the capture one way and S5 another until
        // the sweep's next ask lands -- said here, in S5's own words, not only in logcat
        refileRefused?.let { why ->
            Text(tx.app_not_refiled_web_x(why = why), color = SP_AMB, fontSize = 11.sp, lineHeight = 14.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
        }
        readingsMsg?.let { (kind, m) ->
            // THE COLOUR READS THE KIND, never the text (M3b, critic note 3): a string compare against a
            // now-bilingual line is the two-answers fault in miniature
            Text(m(tx), color = when (kind) {
                com.malletcrafts.interiorestimate.pano.Readings.NoteKind.OK -> SP_OK
                com.malletcrafts.interiorestimate.pano.Readings.NoteKind.NOT_YET -> SP_MUTED
                com.malletcrafts.interiorestimate.pano.Readings.NoteKind.FAILED -> SP_AMB
            }, fontSize = 11.sp, lineHeight = 14.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
        }
        if (onDiscard != null) {
            Text(tx.sp_new_360_keeps_room,
                color = SP_GOLD, fontSize = 11.sp, lineHeight = 14.sp, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
        }
        // ---- THE CLEARS (v52 slice 8, sp.js 1472-1477): this surface (not on the List tab) and the whole room, each two
        // taps, one ↶; what is locked is never counted and never taken
        val nS = SurveyPrepEdit.clearCount(prep, SurveyPrepEdit.SURF, tab); val kS = SurveyPrepEdit.lockedCount(prep, SurveyPrepEdit.SURF, tab)
        val nR = SurveyPrepEdit.clearCount(prep, SurveyPrepEdit.ROOM, tab); val kR = SurveyPrepEdit.lockedCount(prep, SurveyPrepEdit.ROOM, tab)
        // THE CLEARS AND START OVER ACT ON EVERY PHASE'S MARKS, so they are Survey's -- and an unread stage's (port 62
        // slice L4; the prototype's spBulk, index61c2.html 4239, 4264): in any other phase all three are HIDDEN
        val bulk = PhaseTools.bulk(quoteStage)
        if (bulk) {
            if (face) {
                SpTwoTap(SurveyPrepEdit.clearLabel(tx, SurveyPrepEdit.SURF, nS, kS, armedClear == SurveyPrepEdit.SURF),
                    armedClear == SurveyPrepEdit.SURF, nS > 0, land, SP_AMB) { clearTap(SurveyPrepEdit.SURF) }
                Spacer(Modifier.height(6.dp))
            }
            SpTwoTap(SurveyPrepEdit.clearLabel(tx, SurveyPrepEdit.ROOM, nR, kR, armedClear == SurveyPrepEdit.ROOM),
                armedClear == SurveyPrepEdit.ROOM, nR > 0, land, SP_AMB) { clearTap(SurveyPrepEdit.ROOM) }
            Spacer(Modifier.height(6.dp))
            // ---- start over, discard: each takes two taps. START OVER KEEPS WHAT IS LOCKED (v52 slice 8, sp.js 1683):
            // a locked surface's lines, every locked mark and its readings; everything else starts fresh
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, if (armed) SP_AMB else SP_LINE_BORDER, RoundedCornerShape(12.dp))
                .clickable {
                    if (!PhaseTools.bulk(quoteStage)) { armed = false; phaseRefuse(PhaseTools.refuseStartOver(tx)) }
                    else if (!armed) armed = true
                    else {
                        armed = false
                        val r = SurveyPrepEdit.startOver(prep, roomSize[0], roomSize[1], roomSize[2])
                        hist.clear(); sel = null; active = null; toolSet = SurveyPrepPalette.SEL; moreOpen = false
                        ctx = null; armedClear = null; menuOpen = false; hideSnack(); dropDrafts()
                        // ... and every mark past Existing, locked or not (port 62 slice L2): the note names those first
                        prep = r.prep; SurveyPrepEdit.startedOver(tx, r)?.let { say(it) }; changed()
                    }
                }.padding(if (land) 6.dp else 9.dp), contentAlignment = Alignment.Center) {
                Text(if (armed) SurveyPrepEdit.startOverArmed(tx) else SurveyPrepEdit.startOverLabel(tx),
                    color = if (armed) SP_AMB else SP_MUTED, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center)
            }
        }
        if (onDiscard != null) {
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .border(1.dp, if (armedDiscard) Color(0xFFFF9B7A) else SP_LINE_BORDER, RoundedCornerShape(12.dp))
                .clickable { if (!armedDiscard) armedDiscard = true else { armedDiscard = false; onDiscard() } }
                .padding(if (land) 6.dp else 9.dp), contentAlignment = Alignment.Center) {
                Text(if (armedDiscard) tx.sp_tap_again_discard_360 else tx.common_discard_360,
                    color = if (armedDiscard) Color(0xFFFF9B7A) else SP_MUTED, fontSize = 13.sp, lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
    }
    // ---- BEHIND ⋮ (sp.js 1412: "The status, the laser notes, Start over and Discard this 360 sit behind ⋮ in the top
    // bar"): what a non-scrolling screen cannot hold above the photo, one tap away. NOTHING IN IT SCROLLS: portrait is one
    // 300 dp column (~550 dp at its tallest with slice 8's two clears, of ~650 under the bar); landscape is two 264 dp
    // columns side by side, the sizes on the left (~258 dp at their tallest) and the web lines with the two clears, Start
    // over and Discard on the right (~260 dp with all armed: the clears' 68 dp paid for by the 20 dp drop and 6 dp
    // instead of 9 round each two-tap text), of the 286 dp under the bar -- so the two-tap actions are never below a fold.
    val menu: @Composable () -> Unit = {
        val shell = Modifier.clip(RoundedCornerShape(12.dp)).background(SP_CHIP)
            .border(1.dp, SP_LINE_BORDER, RoundedCornerShape(12.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }   // a tap inside never closes it
            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 10.dp)
        if (land) {
            Row(Modifier.widthIn(max = 560.dp).then(shell), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) { menuSizes() }
                // no 20 dp drop to line up with the sizes since slice 8: the two clears took that height
                Column(Modifier.weight(1f)) { menuActions() }
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
            if (ctx != null) ctx = null
            else if (lwhOpen) lwhOpen = false
            else if (menuOpen) menuOpen = false
            else if (moreOpen) moreOpen = false
            else onCancel(prep.copy(), roomSize.copyOf())
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize(), color = SP_BG) {
          Box(Modifier.fillMaxSize().onGloballyPositioned { val at = it.positionInRoot(); rootTop = at.y; rootLeft = at.x }
              .onSizeChanged { rootH = it.height; rootW = it.width }) {
            rev    // read, so every model change recomposes the screen
            if (!land) {
                // PORTRAIT, 360 x 780: bar 40 · tabs 64 + strip 28 = 92 · (Show) · caption · THE PHOTO · what works on it
                // ... and since port 62 slice L4 the phase band over them all, 20 dp: the screen's first line
                Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp)) {
                    SpPhaseBand(band, false)
                    bar()
                    openedLine()
                    tabsUi()
                    if (face) stripUi()
                    if (listView || (face && mode == "mark")) showRow()
                    if (planTab) { planPane(); planFoot() }
                    else if (listView) listPane()
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
                        } else if (planTab) {
                            planPane()
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
                            if (face && mode == "mark") showRow()
                        }
                        // the empty gap under the controls takes part in the strip's touch area (sp.js .spgap)
                        Box(Modifier.weight(1f).fillMaxWidth().onGloballyPositioned { gapAt = it.positionInRoot() }
                            .pointerInput(Unit) { routeToStep({ gapAt }, { x, y -> routeS(x, y) }, { k -> goStepS(k) }) })
                        // the Plan tab: the line naming the drawing, at the foot of the controls (sp.html #planfoot in .spfoot)
                        if (planTab) planFoot()
                        else if (!listView) {
                            if (mode == "lines") linesPanel() else markPanel()
                        }
                    }
                }
            }
            // ---- MORE ▸ (v52 slice 7; by trade since port 62 slice L6): "Every mark type · <Face>" over the bottom of
            // the screen, one row per trade -- the whole width in portrait; in landscape two columns, 700 dp at the
            // right (trades53.css .moresheet.ms53.two53), the whole width on a screen narrower than that; a pick or ✕
            // closes it
            if (moreOpen && marking && PhaseTools.can(PhaseTools.Tool.PALETTE, quoteStage)) {
                SpMoreSheet(tab, tool, land,
                    if (land) Modifier.align(Alignment.BottomEnd).widthIn(max = 700.dp).fillMaxWidth() else Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    onPick = { t -> pickTool(t) }, onClose = { moreOpen = false })
            }
            // ---- ⋮: a tap anywhere outside the menu closes it; one on ← or ✓ Save then goes on to Back or Save
            // (sp.js 1559: `if(!ev.target.closest('#save,#spBack')) return;`), anything else does nothing more
            if (menuOpen) {
                Box(Modifier.fillMaxSize().onGloballyPositioned { overlayAt = it.positionInRoot() }
                    .pointerInput(Unit) { detectTapGestures { off -> outsideTapS(off) } }
                    .semantics { onClick(label = tx.sp_aria_close_menu) { menuOpen = false; true } })
                // in portrait the band has moved the bar, and ⋮ with it, 20 dp down (phase54.css 1283)
                Box(Modifier.align(Alignment.TopEnd).padding(start = 10.dp, end = 10.dp, bottom = 10.dp, top = if (land) 40.dp else 68.dp)) {
                    menu()
                }
            }
            // ---- THE LONG-PRESS MENU (v52 slice 8): Delete · Duplicate · Lock/Unlock · Lock all <type> · Cancel, its
            // top-left 24 dp left of and 12 dp above the press, kept 4 dp inside the screen in either orientation
            // (sp.js 1471). Under it, the whole screen takes a press as Cancel, so nothing beneath moves while it is open.
            // ... on a mark, a Tape or a Line (v52 slice 9: "Lock all tapes", "Lock all Line marks")
            val cm = ctx?.let { c -> SurveyPrepEdit.menuHeaderOf(tx, prep, c.id)?.let { c to it } }
            if (cm != null) {
                val (c, head) = cm
                Box(Modifier.fillMaxSize()
                    .pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume(); ctx = null } }
                    .semantics { onClick(label = tx.sp_aria_close_menu) { ctx = null; true } })
                val edge = with(dens) { 4.dp.toPx() }
                val mw = if (ctxSize.width > 0) ctxSize.width.toFloat() else with(dens) { 180.dp.toPx() }
                val mh = if (ctxSize.height > 0) ctxSize.height.toFloat() else with(dens) { 232.dp.toPx() }
                val left = (c.x - rootLeft - with(dens) { 24.dp.toPx() }).coerceAtMost(rootW - mw - edge).coerceAtLeast(edge)
                val top = (c.y - rootTop - with(dens) { 12.dp.toPx() }).coerceAtMost(rootH - mh - edge).coerceAtLeast(edge)
                // ... less the rows the phase hides (port 62 slice L4): on a surveyed mark after Survey, its head and Cancel;
                // and no Duplicate on a mark where its copy would not be what a tap places (the L4 review; copyOk)
                SpCtxMenu(head, SurveyPrepEdit.menuItemsOf(tx, prep, c.id, quoteStage, asPicked),
                    Modifier.offset { IntOffset(left.roundToInt(), top.roundToInt()) }.onSizeChanged { ctxSize = it }) { k -> ctxPick(c.id, k) }
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
            // ---- THE MARK SHEET (port 62 slice L3): a bottom sheet over survey prep, in its own window -- ✕, the scrim
            // and ◁ cancel it before anything of this screen. What it draws is MarkSheet.view for the stage this
            // dialog was given; each tap and keystroke replaces the sheet's state, read as it stands when it lands.
            markSheet?.let { st ->
                val drawn = MarkSheet.view(tx, prep, st, quoteStage)
                if (drawn == null) LaunchedEffect(st.id) { markSheet = null }   // its item is gone: the sheet closes
                else SpMarkSheet(drawn, land, MarkSheetActions(
                    onKind = { sub -> markSheet = markSheet?.let { cur -> MarkSheet.pickSub(prep, cur, sub) } },
                    onMove = { to -> markSheet = markSheet?.let { cur -> MarkSheet.pickMove(prep, cur, quoteStage, to) } },
                    onWhy = { to, typed -> markSheet = markSheet?.let { cur -> MarkSheet.typeWhy(prep, cur, quoteStage, to, typed) } },
                    onAct = { act -> markSheet = markSheet?.let { cur -> MarkSheet.pickAct(prep, cur, quoteStage, act) } },
                    onNote = { typed -> markSheet = markSheet?.let { cur -> MarkSheet.typeNote(cur, typed) } },
                    onOffice = { office -> markSheet = markSheet?.let { cur -> MarkSheet.pickOffice(cur, office) } },
                    onTrade = { markSheet = markSheet?.let { cur -> MarkSheet.togglePicker(prep, cur) } },
                    onCat = { cat -> markSheet = markSheet?.let { cur -> MarkSheet.pickCat(cur, cat) } },
                    onBare = { markSheet = markSheet?.let { cur -> MarkSheet.closePicker(cur) } },
                    onDone = { sheetDone() },
                    onCancel = { sheetCancel() }))
            }
            // ---- THE GUIDED TOUR (port 61c §31): survey prep is a Dialog — its OWN window, which
            // the activity's overlay cannot cover — so it hosts the second TourOverlay instance,
            // and the bridge reports the open tab / step / mark count into the tour's world and
            // takes the host's `tab` / `mode` moves. Composed LAST, so the scrim draws over
            // everything, the sheets and menus included. Nothing composes when no tour runs.
            TourBindSurvey(tab, mode,
                marks = SurveyPrep.SURFACES.sumOf { prep.surface(it).marks.size },
                onTab = { showTab(it) }, onMode = { goStepS(it) })
            TourOverlay(TourState.HOST_SURVEY)
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
    val tx = LocalTx.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(SP_SHEET).border(1.5.dp, SP_GOLD, RoundedCornerShape(12.dp))
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
        .padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(tx.sp_lwh_title, color = SP_FG, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((i, k) in listOf("L", "W", "H").withIndex()) { // text:value the laser's axis letters
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
        Text(SurveyPrep.sizeHint(tx, bad.isNotEmpty()),
            color = if (bad.isNotEmpty()) SP_OUT else SP_MUTED, fontSize = 11.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpButton(tx.common_cancel, Modifier.weight(1f), Color(0xFF3A3E35)) { onCancel() }
            Box(Modifier.weight(1f).clip(RoundedCornerShape(7.dp)).background(SP_GOLD).clickable { onApply() }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center) {
                Text(tx.common_apply, color = Color(0xFF1A1A12), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

/** What is selected: a line (in Lines) or a mark and which part of it was grabbed (in Mark). */
internal data class SpSel(val s: String, val line: String? = null, val id: String? = null, val part: String = "body")

/** A surface squared to size, and the frame key it was made for. */
internal class SpRect(val key: String, val sq: SurveyPrep.Square, val bmp: Bitmap)

/** A surface's view as cut (SurveyPrep.Cut): [bmp] covers view px [ext] (x0, y0, x1, y1), so view (x, y) is bitmap
 *  (x + [ox], y + [oy]). */
internal class SpCut(val bmp: Bitmap, val ox: Int, val oy: Int, val ext: IntArray)

/** The long-press menu open on mark [id]; the press was at ([x], [y]) root px. */
internal data class SpCtx(val id: String, val x: Float, val y: Float)

/** A drag in progress: where it started on the picture, and what moving it does. [locked]: the mark under the finger
 *  is locked (v52 slice 8) -- it is picked, nothing moves, and a drag says why once: the mark's code. (Locked lines
 *  make no grab at all: they are refused at the press.) */
private class SpGrab(val x0: Double, val y0: Double, val sel: SpSel, val focusSlot: String?, val locked: String? = null,
                     /** v52 slice 10: [locked] is an END's (a Line's, a tape's, an area's corner): its refusal is said,
                      *  "F-M1 is locked — unlock to move it" (SurveyGuide.lockedEnd), not the mark's snackbar. */
                     val endLock: Boolean = false,
                     /** Where the magnifier looks while it is dragged, picture px: the moved end itself, after its snap. */
                     val focus: (() -> DoubleArray)? = null,
                     /** A second finger: what it does besides putting the moved geometry back (an end drag's guide off). */
                     val cancel: (() -> Unit)? = null,
                     /** v52 slice 11: it moves only once the finger has left the dead zone, as an end does (SurveyGuide.EndGate)
                      *  -- a chain's point, which snaps 25 mm, must not jump onto an edge on a tap that picks its chain. */
                     val gate: Boolean = false,
                     /** v52 slice 12: [locked] is said as it is, as a note -- a locked reference's corner: "Front wall
                      *  reference is locked — unlock first" (sp.js hitLines' `say(gr.locked)`). */
                     val said: Boolean = false,
                     val move: (Double, Double) -> Unit)

/** A press on the selected Line's or tape's end (or an area's corner), v52 slice 10: with Select on, [grab] moves it;
 *  with Tape or Line on ([grab] null), a new one starts from exactly that point, [mm] on the face. */
private class SpEnd(val grab: SpGrab?, val mm: DoubleArray)

/** The magnifier(s) while dragging: picture points to centre on, and the finger, canvas px. */
private class SpLoupe(val points: List<DoubleArray>, val finger: Offset)

/**
 * The editing photo: one zoomable picture with the lines (1) or the marks
 * (2) over it. Pinch to zoom (down to a third of fit), one finger off
 * anything pans at any zoom -- in 1 · Lines as far as the wall's cut, past
 * its corners -- "fit" resets; a magnifier follows a drag, and two watch the corners while
 * a line swings. In 2 · Mark a tap with Select picks, a tap with a type on
 * places one where the finger is, and the tool's chip sits top-left (v52 slice 7).
 */
@Composable
private fun SpPhoto(
    /** Its size: the height the controls leave (v52 slice 6), the whole picture letterboxed inside. */
    modifier: Modifier,
    img: Bitmap,
    /** Where view (0, 0) is in [img], px (sp.js c.ox / c.oy): a wall's cut reaches past its 960 x 800 view (v43), and
     *  everything over it -- lines, handles, marks, taps -- stays in VIEW coordinates. 0 for a squared face. */
    ox: Double, oy: Double,
    /** What zoom 1 fits, view px x0, y0, x1, y1 (sp.js fitRectFor); null: the whole picture. */
    fitTo: DoubleArray?,
    tab: String, mode: String, rev: Int,
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
    /** A long press on a mark with Select on (v52 slice 8): the mark's id, and the press in root px -- the menu opens there. */
    onLongPress: (String, Offset) -> Unit,
    /** A locked mark dragged, resized or nudged: its code (the refusal snackbar, once per gesture). */
    onRefuse: (String) -> Unit,
    /** A locked surface's lines pressed: said at the press, and nothing is picked (sp.js hitLines). */
    onLinesLocked: () -> Unit,
    /** A long press opened the menu on a grab the finger's drift had moved: put the press's change back. */
    onRevert: () -> Unit,
    /** v52 slice 9: the Tape or Line being pulled (drawn, and named live on the chip), and a Line area's corners so far. */
    drag: SurveyMeas.Draft?,
    area: List<DoubleArray>?,
    /** A drag with Tape or Line on has begun (from where the finger went down) / moved on / been let go / been cancelled. */
    onDragStart: (SurveyMeas.Draft) -> Unit,
    onDragMove: () -> Unit,
    onDragEnd: (SurveyMeas.Draft) -> Unit,
    onDragCancel: () -> Unit,
    /** A tap with Line on, at (x, y) mm: whether it is on the area's first corner (18 dp), and whether a double tap. */
    onAreaTap: (DoubleArray, Boolean, Boolean) -> Unit,
    /** A tap with Tape on and no drag. */
    onTapeTap: () -> Unit,
    /** With Select on, a press within 12 dp of a tape's band or a Line: it is picked (hit-order step 10, after the marks). */
    onPickMeas: (String) -> Unit,
    /** v52 slice 10: the perspective guide now (SurveyGuide.now) -- a Tape or Line pulled, an end dragged, a Line area
     *  tapped -- or null; drawn on its own surface in 2 · Mark, fading in and out. */
    guide: SurveyGuide.Guide?,
    /** The grid button is on: the plane grid on the face, in 1 · Lines and 2 · Mark, whether or not anything is drawn. */
    grid: Boolean,
    /** The grid button pressed. */
    onGrid: () -> Unit,
    /** An end dragged (its guide), or let go / cancelled (null). */
    onEndGuide: (SurveyGuide.Guide?) -> Unit,
    /** A note to say (sp.js say): a locked end's refusal. */
    onSay: (String) -> Unit,
    /** v52 slice 11: the chain being drawn (drawn dashed; its points counted on the chip), or null. */
    chain: SurveyChain.Draft?,
    /** A tap (or a drag's start) with Chain on at (x, y) mm, with the snapping distance in mm: the point's index, or null. */
    onChainPress: (DoubleArray, Double) -> Int?,
    /** That point follows the finger. */
    onChainMove: (Int, DoubleArray, Double) -> Unit,
    /** A second finger took it back. */
    onChainCancel: (Int) -> Unit,
    /** A double tap with Chain on, or ✓ on its chip: the chain ends. */
    onChainFinish: () -> Unit,
    /** A chain picked: a press on its point (any tool but a type) or, with Select on, within 12 dp of its line. */
    onPickChain: (String) -> Unit,
    /** A finger is on the photo (true at the first down, false when the gesture ends however it ends): a wider cut waits
     *  for it to lift (sp.js surfV.busy). */
    onBusy: (Boolean) -> Unit,
    /** Port 62 slice L4: the project's quote stage, as the dialog was given it. A mark the phase holds read-only
     *  (PhaseTools.editable) is picked by a press and does not move -- a drag says why, once -- and has no tabs and
     *  no trash; the chip's second half says what a tap places a mark as. */
    stage: Int?,
    /** What this capture's chip was last cycled to while the stage is unread, and a tap on that button. */
    asPicked: String?,
    onAs: () -> Unit,
) {
    val density = LocalDensity.current
    // the language (port 61c slice M3a): the chip, the fit and grid buttons; and, through a State, what the gesture says
    val tx = LocalTx.current
    val txS by rememberUpdatedState(tx)
    val iw = img.width.toDouble(); val ih = img.height.toDouble()
    // WHAT ZOOM 1 FITS (sp.js V.FR): the wall and 12° beyond its corners in 1 · Lines, the whole squared face in 2 · Mark.
    // It follows the lines while they move, so it is read through a State by the gesture, which outlives recompositions.
    val fr = fitTo ?: doubleArrayOf(-ox, -oy, iw - ox, ih - oy)
    val frS by rememberUpdatedState(fr)
    // THE PICTURE'S GEOMETRY, read through States too: in 1 · Lines the gesture is not restarted by a wider cut of the
    // same wall (its key has no picture in it), so it must see the cut on show now -- its offset and size -- not the one
    // it started with. Everything it holds is in view coordinates, which a wider cut does not move.
    val oxS by rememberUpdatedState(ox); val oyS by rememberUpdatedState(oy)
    val iwS by rememberUpdatedState(iw); val ihS by rememberUpdatedState(ih)
    val onBusyS by rememberUpdatedState(onBusy)
    val stageS by rememberUpdatedState(stage)
    // The gesture below outlives recompositions (it restarts only for a new surface, mode or -- off 1 · Lines --
    // squaring), so everything it reads that can change under it is read through these.
    val prepS by rememberUpdatedState(prep); val selS by rememberUpdatedState(sel)
    val rectS by rememberUpdatedState(rect); val abS by rememberUpdatedState(ab)
    val onSelectS by rememberUpdatedState(onSelect); val onGrabS by rememberUpdatedState(onGrab)
    val onBeforeChangeS by rememberUpdatedState(onBeforeChange); val onMovedS by rememberUpdatedState(onMoved)
    val onChangedS by rememberUpdatedState(onChanged); val onDeleteS by rememberUpdatedState(onDelete)
    val stepAtS by rememberUpdatedState(stepAt); val onStepS by rememberUpdatedState(onStep)
    val toolS by rememberUpdatedState(tool); val onPlaceS by rememberUpdatedState(onPlace)
    val onLongPressS by rememberUpdatedState(onLongPress); val onRefuseS by rememberUpdatedState(onRefuse)
    val onLinesLockedS by rememberUpdatedState(onLinesLocked); val onRevertS by rememberUpdatedState(onRevert)
    val areaS by rememberUpdatedState(area)
    val onDragStartS by rememberUpdatedState(onDragStart); val onDragMoveS by rememberUpdatedState(onDragMove)
    val onDragEndS by rememberUpdatedState(onDragEnd); val onDragCancelS by rememberUpdatedState(onDragCancel)
    val onAreaTapS by rememberUpdatedState(onAreaTap); val onTapeTapS by rememberUpdatedState(onTapeTap)
    val onPickMeasS by rememberUpdatedState(onPickMeas)
    val onEndGuideS by rememberUpdatedState(onEndGuide); val onSayS by rememberUpdatedState(onSay)
    val onChainPressS by rememberUpdatedState(onChainPress); val onChainMoveS by rememberUpdatedState(onChainMove)
    val onChainCancelS by rememberUpdatedState(onChainCancel); val onChainFinishS by rememberUpdatedState(onChainFinish)
    val onPickChainS by rememberUpdatedState(onPickChain); val chainS by rememberUpdatedState(chain)
    // THE VIEW (sp.js Viewer, v43), in view coordinates. A new surface or step starts FITTED; the same wall with a wider cut
    // keeps its zoom and centre, which mean the same place in view coordinates. A 2 · Mark face is a new picture whenever
    // it is squared again, as before. While fitted, zoom is 1 and the centre is the fit rect's -- following the lines as
    // they move; a pinch or a pan leaves it, and zoom then goes from a third of fit (ZMIN) to 8x.
    // The view itself -- fitted, zoom, centre and every rule over them -- is SurveyPrep.PhotoView in :pano, where it is
    // tested; the gesture and the drawing below both map through it, so the mapping exists once.
    val viewKey: Any = if (mode == "lines") "lines:$tab" else img
    var view by remember(viewKey) { mutableStateOf(SurveyPrep.PhotoView()) }
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
    // THE CHIP'S LIST OF KINDS IS OPEN (port 62 slice L6; sp.js st.subpick). It DIES with the face, the step and the tool
    // it was opened on: a NEW state for each (the L6 review -- setTool: st.subpick=false, index61c2.html 6641), so the
    // same face and tool come back to it SHUT. The first version kept the face and tool it was opened for and compared
    // them, and going away and back found the list open again, unasked. Inside the photo's box a press on the photo, on
    // the chip's plain words (the photo's), on fit or on grid shuts it, as on the page (6811). The photo's press does it
    // through a State, because the gesture outlives recompositions and would otherwise go on shutting the state it
    // started with. More ▸ and the chip's cycling "as …" button do NOT shut it -- nor do they on the page, where each
    // keeps its click to itself (6618, 3091): driven, the list stays open over the More sheet and while "as" cycles.
    val kindOpen = remember(tab, mode, tool) { mutableStateOf(false) }
    val kindOpenS by rememberUpdatedState(kindOpen)
    // With Tape or Line on (v52 slice 9) the chip says what the tool does, live while a drag is pulled, and the
    // caution under it; with a type on, "Placing Door · tap the photo"
    val measTool = SurveyPrepPalette.isMeasTool(tool)
    // With Chain on (v52 slice 11) the chip counts the points, with the caution, and from the second a ✓ ends it
    val chainTool = tool == SurveyPrepPalette.CHAIN
    val chainN = chain?.takeIf { it.s == tab }?.nodes?.size ?: 0
    // THE GUIDE'S FADE (v52 slice 10, sp.js guideState): while a Tape or a Line is drawn, or an end dragged, the grid
    // and the two rays come in at 45 % and reach full in 150 ms; when the drawing ends they go out in 300 ms (the grid
    // stays if its button is on). The last guide is kept for the fade out: what was being drawn has gone by then.
    val guideHere = guide?.takeIf { it.s == tab && mode == "mark" }
    val gAlpha = remember { Animatable(0f) }
    val lastGuide = remember { arrayOfNulls<SurveyGuide.Guide>(1) }
    if (guideHere != null) lastGuide[0] = guideHere
    LaunchedEffect(guideHere != null) {
        if (guideHere != null) {
            gAlpha.snapTo(SurveyGuide.FADE_FROM.toFloat())
            gAlpha.animateTo(1f, tween(SurveyGuide.FADE_IN_MS, easing = LinearEasing))
        } else if (gAlpha.value > 0f) {
            gAlpha.animateTo(0f, tween(SurveyGuide.fadeOutMs(gAlpha.value.toDouble()), easing = LinearEasing))
        }
    }
    // ... and after it what the tap places the mark AS: "· as Client wish" in the Wish list stage (port 62 slice L4;
    // SurveyPrepPalette.chip: none at Survey, plain words where the phase fixes it, the cycling button while unread)
    // ... and, for a type with sub-kinds on this face, the KIND a tap places in the type's place, as a button: "Placing
    // Wardrobe ▾ · tap the photo" (port 62 slice L6; the tool's remembered kind, SurveyPrepPalette.subFor)
    val kindNow = SurveyPrepPalette.subFor(spKinds, tool, tab)
    val typeChip = if (mode != "mark" || chainTool || measTool) null else SurveyPrepPalette.chip(tx, tab, tool, stage, asPicked, sub = kindNow)
    val kinds = if (typeChip == null || typeChip.kindFrom < 0) null else SurveyPrepPalette.kindList(tx, tab, tool, kindNow)
    val chipText = if (mode != "mark") null
                   else if (chainTool) SurveyChain.chipText(tx, chainN)
                   else if (measTool) SurveyMeas.chipText(tx, tool, drag?.takeIf { it.s == tab }, area?.size ?: 0)
                   else typeChip?.text

    // THE FIT RECT IN THE BOX at zoom 1, letterboxed (Amit, 2026-10-11: "does not show full foto for marking"; sp.js
    // V.k): the scale fits both sides of whatever box the screen gives it (v52 slice 6: all the height left). The
    // picture beyond the fit rect -- a wall's cut past its corners -- shows round it and is reached by panning.
    /** The box as it is now: its size, what zoom 1 fits, and the picture on show (SurveyPrep.PhotoBox). */
    fun box() = SurveyPrep.PhotoBox(boxW.toDouble(), boxH.toDouble(), frS, oxS, oyS, iwS, ihS)
    fun k() = view.k(box())
    fun toS(x: Double, y: Double): FloatArray = view.toS(box(), x, y).let { floatArrayOf(it[0].toFloat(), it[1].toFloat()) }
    fun fromS(sx: Float, sy: Float): DoubleArray = view.fromS(box(), sx.toDouble(), sy.toDouble())

    /**
     * WHAT A FINGER TAKES ON 1 · LINES (v52 slice 12, sp.js hitLines; the order is SurveyRef.linesHit, tested in :pano):
     * the reference rectangle's corner within 20 dp -- it moves, like a line end; a LOCKED reference's corner -- picked,
     * nothing moves, and a drag says "Front wall reference is locked — unlock first"; a line's end within 18 dp; a line
     * within 20 dp; else a press INSIDE the reference picks it ([picked] gets its id, so a long press opens its Lock ·
     * Cancel) and nothing is grabbed. LOCKED LINES (v52 slice 8, sp.js 947-950): a grip or a line under the finger says
     * so AT THE PRESS and nothing is picked -- no selection, no nudge row, no grab (a drag then pans).
     */
    fun hitLines(sx: Float, sy: Float, picked: (String) -> Unit = {}): SpGrab? {
        val sf = prepS.surface(tab)
        val r = SurveyRef.of(sf)?.takeIf { SurveyRef.placed(it) }
        fun scr(p: DoubleArray) = toS(p[0], p[1]).let { q -> doubleArrayOf(q[0].toDouble(), q[1].toDouble()) }
        val at = fromS(sx, sy)
        val rq = r?.q
        val h = SurveyRef.linesHit(rq?.map { scr(it) }, r?.locked == true,
            SurveyPrep.SLOTS.associateWith { slot -> sf.lines.getValue(slot).map { scr(it) } },
            { slot -> SurveyPrep.distToLine(at, sf.lines.getValue(slot)) * k() },
            rq != null && SurveyRef.inside(at[0], at[1], rq), SurveyPrep.linesLocked(sf),
            sx.toDouble(), sy.toDouble(), density.density.toDouble())
        val rid = SurveyRef.id(tab)
        return when (h) {
            null -> null
            is SurveyRef.LinesHit.Corner -> {
                val i = h.i; val p0 = rq!![i].copyOf()
                SpGrab(p0[0], p0[1], SpSel(tab, id = rid), null,
                    focus = { SurveyRef.of(sf)?.q?.getOrNull(i) ?: p0 }) { ix, iy -> SurveyRef.moveCorner(sf, i, ix, iy) }
            }
            is SurveyRef.LinesHit.RefLocked -> SpGrab(at[0], at[1], SpSel(tab, id = rid), null, SurveyRef.lockedSaid(txS, tab), said = true) { _, _ -> }
            is SurveyRef.LinesHit.End -> {
                val slot = h.slot; val j = h.j; val p0 = sf.lines.getValue(slot)[j]
                SpGrab(p0[0], p0[1], SpSel(tab, line = slot), slot) { ix, iy ->
                    sf.lines.getValue(slot)[j] = doubleArrayOf(ix, iy); sf.set[slot] = true }
            }
            is SurveyRef.LinesHit.Body -> {
                val slot = h.slot
                val start = sf.lines.getValue(slot).map { it.copyOf() }
                SpGrab(at[0], at[1], SpSel(tab, line = slot), slot) { ix, iy ->
                    sf.lines[slot] = Array(2) { i -> doubleArrayOf(start[i][0] + ix - at[0], start[i][1] + iy - at[1]) }; sf.set[slot] = true }
            }
            is SurveyRef.LinesHit.LinesLocked -> { onLinesLockedS(); null }
            is SurveyRef.LinesHit.Inside -> { onGrabS(SpSel(tab, id = rid)); picked(rid); null }
        }
    }

    /** Every chain on this face with its points on the canvas, px (v52 slice 11). */
    fun chainsOnScreen(): List<Pair<String, List<DoubleArray>>> {
        val r = rectS ?: return emptyList(); val sq = r.sq
        return prepS.surface(tab).chains.map { c ->
            c.id to c.nodes.map { p -> toS(sq.x(p[0]), sq.y(p[1])).let { q -> doubleArrayOf(q[0].toDouble(), q[1].toDouble()) } } }
    }

    /**
     * A CHAIN'S POINT PRESSED (v52 slice 11, sp.js kNodeGrab; items 7 and 14): the chain is picked (through the grab's
     * selection) and the point moves ALONG the chain, between its neighbours, snapping onto the face's and the marks'
     * edges within 25 mm or 14 dp (SurveyChain.nodeMove), one ↶ step, once the finger leaves the dead zone ([SpGrab.gate]).
     * A locked chain is picked and nothing moves: "FK1 is locked — unlock to move it". Null: no such point.
     */
    fun nodeGrab(cid: String, i: Int): SpGrab? {
        val r = rectS ?: return null; val sq = r.sq
        val c = prepS.surface(tab).chains.firstOrNull { it.id == cid } ?: return null
        val p0 = c.nodes.getOrNull(i) ?: return null
        val px = sq.x(p0[0]); val py = sq.y(p0[1])
        val pick = SpSel(tab, id = c.id)
        if (SurveyPrep.isLocked(c)) return SpGrab(px, py, pick, null, c.code, endLock = true, gate = true) { _, _ -> }
        // at Estimation a chain is read-only (port 62 slice L4, PhaseTools.editable): picked, nothing moves, and a drag
        // says "FK1 · Estimation changes nothing — …"
        if (!PhaseTools.editable(c, stageS)) return SpGrab(px, py, pick, null, PhaseTools.refuseText(txS, c, stageS), gate = true, said = true) { _, _ -> }
        // 14 dp in face mm at the scale the press was made at
        val tol = SurveyGuide.endTol(density.density.toDouble(), SurveyGuide.mmPerCanvasPx(sq.mpp, k()))
        val s = tab
        return SpGrab(px, py, pick, null, gate = true,
            focus = { c.nodes.getOrNull(i)?.let { q -> doubleArrayOf(sq.x(q[0]), sq.y(q[1])) } ?: doubleArrayOf(px, py) }) { ix, iy ->
            val ab = abS
            SurveyChain.nodeMove(s, c, i, doubleArrayOf(sq.mmX(ix), sq.mmY(iy)), ab[0], ab[1], prepS.surface(s).marks, tol)
        }
    }

    /** With Chain on: a press within 16 dp of a chain's point grabs that point (sp.js hit, before the Chain tool's new
     *  point); null: the press is a new point. */
    fun chainNodeAt(sx: Float, sy: Float): SpGrab? =
        SurveyChain.nodeAt(chainsOnScreen(), sx.toDouble(), sy.toDouble(), SurveyChain.NODE_HIT_DP * density.density)
            ?.let { (cid, i) -> nodeGrab(cid, i) }

    /** HIT-ORDER STEP 9 (v52 slice 11, sp.js kHitLine): with Select on and nothing else under the finger, the chain whose
     *  line is within 12 dp -- before a tape or a Line. Its id, or null. */
    fun hitChain(sx: Float, sy: Float): String? =
        SurveyChain.pick(chainsOnScreen(), sx.toDouble(), sy.toDouble(), SurveyChain.PICK_DP * density.density)

    /** Canvas px ([sx], [sy]) as mm on the squared face, with the chain's snapping distance there (25 mm or 14 dp). */
    fun chainAt(sx: Float, sy: Float): Pair<DoubleArray, Double>? {
        val r = rectS ?: return null; val at = fromS(sx, sy)
        return doubleArrayOf(r.sq.mmX(at[0]), r.sq.mmY(at[1])) to
            SurveyGuide.endTol(density.density.toDouble(), SurveyGuide.mmPerCanvasPx(r.sq.mpp, k()))
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
        // ... and so is a Note's pin (port 62 slice L6; sp.js 5791 and 5809: `ISPOINT(e.t)||L53.isNote(e)`)
        val drawn = els.map { e ->
            val p = toS(sq.x(e.x0), sq.y(e.y1)); val q = toS(sq.x(e.x1), sq.y(e.y0))
            SurveyPrepTouch.Drawn(e.id, p[0].toDouble(), q[0].toDouble(), p[1].toDouble(), q[1].toDouble(),
                spot = if (SurveyPrep.isSpot(e.t)) SurveyPrep.SPOT / 2 / sq.mpp * k() else null)
        }
        val tol = with(density) { 16.dp.toPx() }.toDouble()
        // a LOCKED picked mark has no tabs (v52 slice 8): it is tested as a mark like any other, so a press on it holds it
        // ... and so has a picked mark the phase holds read-only (port 62 slice L4): no tabs drawn, none to press
        val selId = selS?.id?.takeIf { sid -> els.firstOrNull { it.id == sid }?.let { !SurveyPrep.isLocked(it) && PhaseTools.editable(it, stageS) } == true }
        // A CHAIN'S POINTS (v52 slice 11), with Select on: AFTER the picked mark's tabs (ct52 X5: "a chain point on a
        // door's left tab resizes the door") and before every tag and mark -- 16 dp each (sp.js hit)
        val nodes = if (toolS == SurveyPrepPalette.SEL) chainsOnScreen().map { (id, pts) -> SurveyPrepTouch.Nodes(id, pts) } else emptyList()
        return when (val h = SurveyPrepTouch.hit(drawn, selId, tags.toList(), sx.toDouble(), sy.toDouble(), tol, density.density.toDouble(),
                nodes, SurveyChain.NODE_HIT_DP * density.density)) {
            null -> null
            is SurveyPrepTouch.Hit.Node -> nodeGrab(h.id, h.i)
            is SurveyPrepTouch.Hit.Tag -> {
                prepS.markOf(h.code)?.let { (s, e) -> onSelectS(s, e, h.code) }
                tagged()
                null
            }
            is SurveyPrepTouch.Hit.Mark -> {
                val e = els.firstOrNull { it.id == h.id }
                if (e == null) null
                else if (SurveyPrep.isLocked(e)) { val at = fromS(sx, sy); SpGrab(at[0], at[1], SpSel(tab, id = e.id), null, e.code) { _, _ -> } }
                // A MARK OF AN EARLIER PHASE IS READ-ONLY (port 62 slice L4; sp.js moveEl's `!P54.spEdit(e)`): the press
                // picks it -- its label opens its Mark sheet -- nothing moves, no undo entry, and once the finger has
                // really moved it says why: "FW1 was marked in Survey — move the phase back to change it"
                else if (!PhaseTools.editable(e, stageS)) { val at = fromS(sx, sy)
                    SpGrab(at[0], at[1], SpSel(tab, id = e.id), null, PhaseTools.refuseText(txS, e, stageS), said = true) { _, _ -> } }
                else {
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

    /** Canvas px ([sx], [sy]) as mm on the squared face; null without one. */
    fun mmAt(sx: Float, sy: Float): DoubleArray? {
        val r = rectS ?: return null; val at = fromS(sx, sy)
        return doubleArrayOf(r.sq.mmX(at[0]), r.sq.mmY(at[1]))
    }

    /** HIT-ORDER STEP 10 (v52 slice 9, sp.js mHit): with Select on and nothing else under the finger, the tape band or
     *  Line within 12 dp of it -- the nearest. Its id, or null. */
    fun hitMeas(sx: Float, sy: Float): String? {
        val r = rectS ?: return null; val sq = r.sq
        val ms = prepS.surface(tab).meas.filter { SurveyMeas.known(it.kind) }
        if (ms.isEmpty()) return null
        val ppm = k() / sq.mpp     // canvas px per face mm
        val dpx = density.density.toDouble()
        val shapes = ms.map { m -> m to m.pts.map { p -> toS(sq.x(p[0]), sq.y(p[1])).let { q -> doubleArrayOf(q[0].toDouble(), q[1].toDouble()) } } }
        return SurveyMeas.pick(shapes, sx.toDouble(), sy.toDouble(), SurveyMeas.PICK_DP * dpx) { m ->
            if (m.kind == SurveyMeas.TAPE) SurveyMeas.bandHalfPx(ppm, dpx) else 0.0 }
    }

    /**
     * END POINTS THAT MOVE (v52 slice 10, item 8; Amit, 14 Oct 2026 06:30 IST: "i can not move measure line once drawn.
     * i should be able to place it by moving its end points."; sp.js endHit / endGrab): a press within 14 dp of the
     * SELECTED Line's or tape's end, or an area's corner, on this face -- first in the hit order, before the marks.
     * Its grab moves that one point (SurveyGuide.endMove: kept on the face, 5° onto the other end's horizontal or
     * vertical, onto the face's and the marks' edges within 25 mm or 14 dp, 0.1 mm), the guide running through the
     * fixed end; a locked one is picked and does not move. Not while a Line area is being tapped (its taps are
     * corners). Null: no end under the finger.
     */
    fun endHit(sx: Float, sy: Float, tool: String): SpEnd? {
        val r = rectS ?: return null; val sq = r.sq
        val sid = selS?.takeIf { it.s == tab }?.id ?: return null
        val m = prepS.surface(tab).meas.firstOrNull { it.id == sid } ?: return null
        val scr = m.pts.map { p -> toS(sq.x(p[0]), sq.y(p[1])).let { q -> doubleArrayOf(q[0].toDouble(), q[1].toDouble()) } }
        // WHAT THE PRESS IS -- grab, locked, or a new one from the end -- is decided in :pano, where it is tested
        val ep = SurveyGuide.endPress(mode == "mark", tool, m, !areaS.isNullOrEmpty(), scr, sx.toDouble(), sy.toDouble(),
            SurveyGuide.END_HIT_DP * density.density) ?: return null
        val i = when (ep) {
            is SurveyGuide.EndPress.StartNew -> return SpEnd(null, ep.mm)
            is SurveyGuide.EndPress.Locked -> ep.i
            is SurveyGuide.EndPress.Grab -> ep.i
        }
        val p0 = m.pts[i].copyOf(); val px = sq.x(p0[0]); val py = sq.y(p0[1])
        val pick = SpSel(tab, id = m.id)
        if (ep is SurveyGuide.EndPress.Locked) return SpEnd(SpGrab(px, py, pick, null, ep.code, endLock = true) { _, _ -> }, p0)
        // at Estimation a Tape, a Line and an area are read-only (port 62 slice L4, PhaseTools.editable): the end is
        // picked and does not move, and a drag says "F-M1 · Estimation changes nothing — …"
        if (!PhaseTools.editable(m, stageS)) return SpEnd(SpGrab(px, py, pick, null, PhaseTools.refuseText(txS, m, stageS), said = true) { _, _ -> }, p0)
        // 14 dp in face mm at the scale the press was made at
        val tol = SurveyGuide.endTol(density.density.toDouble(), SurveyGuide.mmPerCanvasPx(sq.mpp, k()))
        val s = tab
        val g = SpGrab(px, py, pick, null,
            focus = { m.pts.getOrNull(i)?.let { q -> doubleArrayOf(sq.x(q[0]), sq.y(q[1])) } ?: doubleArrayOf(px, py) },
            cancel = { onEndGuideS(null) }) { ix, iy ->
            val ab = abS
            SurveyGuide.endMove(s, m, i, doubleArrayOf(sq.mmX(ix), sq.mmY(iy)), ab[0], ab[1], prepS.surface(s).marks, tol)?.let { e ->
                m.pts[i] = e.p
                onEndGuideS(SurveyGuide.endGuide(s, m, e))
            }
        }
        return SpEnd(g, p0)
    }

    fun loupeFor(g: SpGrab, ix: Double, iy: Double, finger: Offset): SpLoupe {
        val j = g.focusSlot?.let { SurveyPrep.junctions(tab, prepS.surface(tab).lines, it) }
        // an end being dragged: the magnifier shows the END where it has snapped to, not the finger's pixel (v52 slice 10)
        return SpLoupe(j ?: listOf(g.focus?.invoke() ?: doubleArrayOf(ix, iy)), finger)
    }

    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black)
        .onSizeChanged { boxW = it.width.toFloat(); boxH = it.height.toFloat() }
        .onGloballyPositioned { photoAt = it.positionInRoot() }
        // NOT KEYED ON THE PICTURE (review): in 1 · Lines a wider cut of the same wall is a new bitmap, and a restart
        // under a held finger dropped the drag; the gesture reads the picture through States instead. Off 1 · Lines a new
        // squaring is a new face (its rect), as before; in 1 · Lines the rect is not read, so it restarts nothing.
        .pointerInput(tab, mode, if (mode == "lines") null else rect) {
            // THE ONE GESTURE LOOP (v52 slice 7). What a finger is doing -- press, drag, tap, long press, double tap,
            // or cancelled by a second finger -- is decided by GestureClassifier in :pano, where it is tested; this
            // only feeds it what the system reports (and the clock, for the long press) and acts on its answers.
            // It outlives gestures, so a double tap is seen across two of them.
            val gc = GestureClassifier(viewConfiguration.touchSlop.toDouble(), GestureClassifier.DOUBLE_TAP_DP.dp.toPx().toDouble(),
                still = GestureClassifier.GRAB_STILL_DP.dp.toPx().toDouble())
            awaitEachGesture {
                val first = awaitFirstDown()
                first.consume()
                // a press anywhere on the photo shuts the chip's list of kinds, and goes on to do its own work (sp.js 6811)
                kindOpenS.value = false
                // THE FLOATING TRASH is on top of everything: a press on it deletes the picked mark when it lifts there.
                // Only with Select on (sp.js fTrashSurf): with a type on, the disc is not there, so a placing tap
                // beside the mark just placed can never land on it and delete that mark
                val tr = if (mode == "mark" && toolS == SurveyPrepPalette.SEL) trashAt[0] else null
                // ... except over the selected item's end handle: the handle is the end's, never the disc's (v52 slice
                // 10 review: a clamped disc could sit on a handle and delete the tape the press meant to stretch)
                if (tr != null && hypot(first.position.x - tr[0], first.position.y - tr[1]) <= tr[2] &&
                    endHit(first.position.x, first.position.y, SurveyPrepPalette.SEL) == null) {
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
                // a locked thing's refusal is said once per gesture; after a long press opened the menu the rest of
                // the gesture is the menu's (nothing drags, pans or places)
                var refused = false; var spent = false
                // the press picked a number tag (its measure is now the one being taken): no tap places under it
                var tagged = false
                var lastPinch = 0f; var lastMid = Offset.Zero
                val slideCx = view.centreX(box()); val slideCy = view.centreY(box())
                // the tool as the press began: a tap places the type that was on when the finger went down
                val tool0 = toolS
                val placing = mode == "mark" && SurveyPrepPalette.isType(tool0)
                // TAPE OR LINE ON (v52 slice 9): a drag pulls one from where the finger went down; a tap is a Line area's
                // corner (or, with Tape, the hint); nothing under the finger is picked or moved meanwhile (sp.js hit)
                val measOn = mode == "mark" && SurveyPrepPalette.isMeasTool(tool0)
                // CHAIN ON (v52 slice 11, sp.js hit / kGrab): a press on a chain's point grabs it; anything else is a point --
                // a tap adds it where the finger lifts, a drag adds it where the finger went down and carries it; a double
                // tap ends the chain. Nothing else under the finger is picked or moved meanwhile.
                val chainOn = mode == "mark" && tool0 == SurveyPrepPalette.CHAIN
                // the point this press added to the chain being drawn (its index), and whether the press tried to add one
                var chainIdx: Int? = null
                var chainTried = false
                var draft: SurveyMeas.Draft? = null
                var measDragged = false
                // a long still press with Tape or Line on is a slow tap: it acts when the finger lifts without a drag
                var heldTap = false
                var gone = false
                // with Select on, the Tape or Line a press picked (hit-order step 10): the long press opens its menu
                var pickedMeas: String? = null
                // v52 slice 10: with Tape or Line on, a press on the selected item's end starts the new one from that
                // very point (tapes laid end to end, ct52 FX13); with Select on, the press grabbed an end
                var startAt: DoubleArray? = null
                var endGrabbed = false
                // the grab moves only past the dead zone: an end's (slice 10) or a chain's point (slice 11, SpGrab.gate)
                var gated = false
                // v52 slice 10 review: an end moves only once the finger has left the touch slop (4 dp at least), so a
                // press that lifts as a tap never moved it; and a cancel puts it back only for a second finger
                val endGate = SurveyGuide.EndGate(max(SurveyGuide.END_DEAD_DP.dp.toPx().toDouble(), viewConfiguration.touchSlop.toDouble()))
                var secondFinger = false

                fun tapAt(x: Double, y: Double) {
                    if (grab == null && placing && !tagged) placeGrab(x.toFloat(), y.toFloat(), tool0)
                }
                /** A tap with Tape or Line on, at canvas ([x], [y]): the Tape's hint, or a Line area's corner. */
                fun measTap(x: Double, y: Double, dbl: Boolean) {
                    if (tool0 == SurveyPrepPalette.TAPE) { onTapeTapS(); return }
                    val mm = startAt ?: mmAt(x.toFloat(), y.toFloat()) ?: return
                    val a = areaS; val r = rectS
                    val near = a != null && a.isNotEmpty() && r != null &&
                        toS(r.sq.x(a[0][0]), r.sq.y(a[0][1])).let { q ->
                            SurveyMeas.nearFirst(doubleArrayOf(q[0].toDouble(), q[1].toDouble()), x, y, a.size, 1.dp.toPx().toDouble()) }
                    onAreaTapS(mm, near, dbl)
                }
                /** A tap with Chain on at canvas ([x], [y]): a new point there -- unless the press grabbed a chain's point. */
                fun chainTap(x: Double, y: Double) {
                    if (grab != null || chainTried) return
                    chainTried = true
                    chainAt(x.toFloat(), y.toFloat())?.let { (mm, tl) -> onChainPressS(mm, tl) }
                }
                fun act(evs: List<GestureClassifier.Event>) {
                    for (e in evs) if (!spent) when (e) {
                        // the press: what is under it is picked now, as sp.js picks on pointerdown
                        is GestureClassifier.Event.Down -> {
                            // FIRST, THE SELECTED ITEM'S ENDS (v52 slice 10, sp.js hit): with Select on the press grabs
                            // the end; with Tape or Line on it starts a NEW one there -- it never stretches the old one
                            val eh = endHit(e.x.toFloat(), e.y.toFloat(), tool0)
                            val eg = eh?.grab
                            if (eh != null && eg == null) startAt = eh.mm
                            else if (eg != null) { grab = eg; endGrabbed = true; gated = true; onGrabS(eg.sel); gc.grabbed() }
                            // CHAIN ON: a chain's point under the finger is grabbed (and its chain picked); else nothing yet
                            else if (chainOn) chainNodeAt(e.x.toFloat(), e.y.toFloat())?.let { g ->
                                grab = g; gated = true; onGrabS(g.sel); gc.grabbed() }
                            else if (!measOn) {
                                // 1 · Lines: a press inside the reference picks it (v52 slice 12), so its long press opens Lock · Cancel
                                val h = if (mode == "lines") hitLines(e.x.toFloat(), e.y.toFloat()) { rid -> pickedMeas = rid }
                                        else hitMarks(e.x.toFloat(), e.y.toFloat()) { tagged = true }
                                // a grab follows the finger from the first pixel (no slop dead zone), as sp.js moves it --
                                // except a chain's point, which waits for the dead zone as an end does (SpGrab.gate)
                                if (h != null) { grab = h; if (h.gate) gated = true; onGrabS(h.sel); gc.grabbed() }
                                // nothing grabbed and Select on: a chain within 12 dp of its line (v52 slice 11), else a Tape
                                // or a Line within 12 dp, is picked (it does not move)
                                else if (mode == "mark" && tool0 == SurveyPrepPalette.SEL && !tagged) {
                                    val cid = hitChain(e.x.toFloat(), e.y.toFloat())
                                    if (cid != null) { pickedMeas = cid; onPickChainS(cid) }
                                    else hitMeas(e.x.toFloat(), e.y.toFloat())?.let { mid -> pickedMeas = mid; onPickMeasS(mid) }
                                }
                            }
                        }
                        // A DRAG WITH TAPE OR LINE ON PULLS ONE (v52 slice 9): from the down point, snapped at 5° to the face's
                        // horizontal or vertical (SurveyMeas.Draft), the magnifier on the finger; let go, it is plotted or
                        // refused by the dialog. With a Line area started, a drag draws nothing (sp.js: taps only).
                        is GestureClassifier.Event.Drag -> if (measOn) {
                            if (draft == null && !(tool0 == SurveyPrepPalette.LINE && !areaS.isNullOrEmpty())) {
                                val m0 = startAt ?: mmAt((e.x - e.dx).toFloat(), (e.y - e.dy).toFloat())
                                val ab0 = abS
                                if (m0 != null) {
                                    val d = SurveyMeas.Draft(tab, if (tool0 == SurveyPrepPalette.TAPE) SurveyMeas.TAPE else SurveyMeas.LINE, m0, ab0[0], ab0[1])
                                    draft = d; measDragged = true; onDragStartS(d)
                                }
                            }
                            val d = draft
                            if (d != null) {
                                // FROM A SELECTED END (review): the end plus the finger's movement (SurveyGuide.dragFrom),
                                // so the finger's miss of the end never reaches the new one; else the finger itself
                                val fin = mmAt(e.x.toFloat(), e.y.toFloat())
                                val st = startAt
                                val dn = if (st != null) mmAt((e.x - e.dx).toFloat(), (e.y - e.dy).toFloat()) else null
                                val p = if (st != null && dn != null && fin != null) SurveyGuide.dragFrom(st, dn, fin) else fin
                                p?.let { d.move(it) }
                                val rq = rectS
                                val look = if (st != null && p != null && rq != null) doubleArrayOf(rq.sq.x(p[0]), rq.sq.y(p[1]))
                                           else fromS(e.x.toFloat(), e.y.toFloat())
                                loupe = SpLoupe(listOf(look), Offset(e.x.toFloat(), e.y.toFloat()))
                                onDragMoveS()
                            }
                        } else if (chainOn && grab == null) {
                            // A DRAG WITH CHAIN ON (v52 slice 11, sp.js kGrab): the point goes where the finger went down
                            // and follows it -- on the chain's axis once two points fix it -- the magnifier on the point
                            if (!chainTried) {
                                chainTried = true
                                chainAt((e.x - e.dx).toFloat(), (e.y - e.dy).toFloat())?.let { (mm, tl) -> chainIdx = onChainPressS(mm, tl) }
                            }
                            val ci = chainIdx
                            if (ci != null) chainAt(e.x.toFloat(), e.y.toFloat())?.let { (mm, tl) ->
                                onChainMoveS(ci, mm, tl)
                                val rq = rectS; val nd = chainS?.nodes?.getOrNull(ci)
                                val look = if (rq != null && nd != null) doubleArrayOf(rq.sq.x(nd[0]), rq.sq.y(nd[1]))
                                           else fromS(e.x.toFloat(), e.y.toFloat())
                                loupe = SpLoupe(listOf(look), Offset(e.x.toFloat(), e.y.toFloat()))
                            }
                        } else {
                            // a drag from empty photo with a type on: placed where the finger went down, then carried
                            if (e.first && grab == null && placing && !tagged) {
                                val sx0 = (e.x - e.dx).toFloat(); val sy0 = (e.y - e.dy).toFloat()
                                placeGrab(sx0, sy0, tool0)?.let { grab = it; snapped = true }   // placing was its one undo step
                            }
                            val g = grab
                            if (g != null && g.locked != null) {
                                // LOCKED (v52 slice 8): nothing moves, no undo entry; once the finger has really
                                // moved (4 dp, sp.js 935), the refusal -- once per gesture
                                if (!refused && hypot(e.dx, e.dy) > 4.dp.toPx()) {
                                    refused = true
                                    // a locked END says so as a note (sp.js say), a locked mark with its snackbar
                                    if (g.endLock) onSayS(SurveyGuide.lockedEnd(txS, g.locked))
                                    else if (g.said) onSayS(g.locked)
                                    else onRefuseS(g.locked)
                                }
                            } else if (g != null) {
                                // an END stays put (no move, no undo step) until the finger leaves the touch slop: a tap on its
                                // handle must not snap it onto an edge (review); a mark follows from the first pixel
                                if (!gated || endGate.move(e.dx, e.dy)) {
                                    if (!snapped) { onBeforeChangeS(); snapped = true }
                                    // kept on the picture, which in 1 · Lines reaches past the view (sp.js 936)
                                    val b = box(); val kk = view.k(b)
                                    val ix = b.keepX(g.x0 + e.dx / kk)
                                    val iy = b.keepY(g.y0 + e.dy / kk)
                                    g.move(ix, iy); moved = true
                                    loupe = loupeFor(g, ix, iy, Offset(e.x.toFloat(), e.y.toFloat()))
                                    onMovedS()
                                }
                            } else {
                                // A DRAG ON EMPTY PHOTO PANS, at any zoom (sp.js v43: "pan anywhere"), so the cut past a
                                // wall's corners can be brought into the box
                                view = view.pan(box(), slideCx, slideCy, e.dx, e.dy)
                                tick++
                            }
                        }
                        // A TAP: with Select on, the press already picked what was under it; with a type on and
                        // nothing under it, the type goes where the finger went down. A double tap acts as a tap
                        // here: the Line's area and the Chain give it a meaning of its own (slices 9 and 11).
                        // An end that moved and still lifted as a tap is put back, with its undo step (review).
                        is GestureClassifier.Event.Tap -> {
                            if (SurveyGuide.revertOnTap(gated, moved)) { onRevertS(); moved = false; snapped = false }
                            if (measOn) measTap(e.x, e.y, false) else if (chainOn) chainTap(e.x, e.y) else tapAt(e.x, e.y)
                        }
                        // a double tap closes a Line area of three corners or more (v52 slice 9), and ENDS a chain (slice 11,
                        // sp.js kDown: the second tap of the pair adds no point -- the first already did)
                        is GestureClassifier.Event.DoubleTap -> {
                            if (SurveyGuide.revertOnTap(gated, moved)) { onRevertS(); moved = false; snapped = false }
                            if (measOn) measTap(e.x, e.y, true)
                            else if (chainOn) { if (grab == null) onChainFinishS() }
                            else tapAt(e.x, e.y)
                        }
                        // A LONG PRESS WITH A TYPE ON IS A SLOW TAP: sp.js places on the press however long it is held
                        // (its long-press timer is armed for Select only, sp.js 929 / 1177), so the type goes where
                        // the finger went down and a slide after it carries the mark. With Select on it opens the
                        // menu of the mark under it (below).
                        // with Chain on a long still press is a slow tap: a point where the finger went down, at the lift
                        is GestureClassifier.Event.LongPress -> if (measOn) { heldTap = true } else if (chainOn) { if (grab == null) heldTap = true } else {
                            val gid = grab?.sel?.id ?: pickedMeas
                            if (grab == null && placing && !tagged)
                                placeGrab(e.x.toFloat(), e.y.toFloat(), tool0)?.let { grab = it; snapped = true; gc.grabbed() }
                            // THE LONG-PRESS MENU (v52 slice 8, sp.js openCtx): with Select on, held on a mark (locked
                            // or not), the menu opens at the press; on empty photo, nothing. A grab moved further than
                            // GRAB_STILL_DP gets no long press (GestureClassifier); one the drift moved less is put
                            // back first, with its undo step, so the menu never opens on a mark that has moved
                            // ... and on 1 · Lines, held on the reference rectangle (its corner or inside it): Lock · Cancel
                            // under "Front wall reference" (v52 slice 12, ct52 X4) -- a line's long press opens nothing
                            else if (!placing && gid != null && (mode == "mark" || SurveyRef.isId(gid))) {
                                if (moved) { onRevertS(); moved = false; snapped = false }
                                grab = null; loupe = null; spent = true
                                onLongPressS(gid, Offset(e.x.toFloat() + photoAt.x, e.y.toFloat() + photoAt.y))
                            }
                        }
                        is GestureClassifier.Event.DragEnd -> if (measOn) draft?.let { d -> draft = null; loupe = null; onDragEndS(d) }
                        // a second finger: the pinch takes over, and the grab lets go (what it moved stays); a Tape or Line
                        // being pulled is dropped, nothing plotted (sp.js: "a second finger ends the drag, nothing plotted")
                        is GestureClassifier.Event.Cancel -> {
                            val g = grab
                            grab = null; loupe = null; gone = true
                            if (draft != null) { draft = null; onDragCancelS() }
                            // a chain's point this press added goes again with a second finger (sp.js kDown's cancel): the
                            // pinch never leaves a stray point; a system cancel keeps it, as a lift does
                            chainIdx?.let { ci -> if (secondFinger) { chainIdx = null; onChainCancelS(ci) } }
                            // AN END BEING DRAGGED GOES BACK (v52 slice 10, ct52 FX3: "a second finger puts the end
                            // back"): the drag's own undo step is taken back off with its geometry, so the end is where
                            // it was and there is no undo entry; its guide goes off. A SYSTEM cancel (palm rejection)
                            // keeps the end where it was dragged, as a lift does (sp.js pointercancel -> up; review)
                            g?.cancel?.let { c ->
                                if (SurveyGuide.revertOnCancel(secondFinger, moved)) { onRevertS(); moved = false; snapped = false }
                                c()
                            }
                        }
                    }
                }

                // THE CLEAN-UP RUNS HOWEVER THE GESTURE ENDS (review): a pointerInput restarted by a step, mode or face
                // change mid-drag cancels this coroutine, and without the finally the end's guide stayed on and the
                // moved end was never saved
                onBusyS(true)
                try {
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
                            secondFinger = true
                            act(gc.move(down[0].position.x.toDouble(), down[0].position.y.toDouble(), t, down.size))
                            // PINCH AND PAN, as before: zoom about where the fingers started, follow their midpoint
                            val a = down[0].position; val b = down[1].position
                            val d = hypot(a.x - b.x, a.y - b.y).coerceAtLeast(1f); val mid = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
                            if (!pinch) { pinch = true; grab = null; loupe = null; lastPinch = d; lastMid = mid }
                            else {
                                view = view.pinch(box(), (d / lastPinch).toDouble(), lastMid.x.toDouble(), lastMid.y.toDouble(),
                                    mid.x.toDouble(), mid.y.toDouble())
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
                    // a slow tap with Tape or Line on: a corner (or the hint) where the finger went down
                    if (measOn && heldTap && !measDragged && !gone) measTap(first.position.x.toDouble(), first.position.y.toDouble(), false)
                    if (chainOn && heldTap && !gone) chainTap(first.position.x.toDouble(), first.position.y.toDouble())
                } finally {
                    // a drag the loop left (the gesture ended any other way): nothing plotted
                    if (draft != null) { draft = null; onDragCancelS() }
                    // an end let go (or cancelled, or its long press opened the menu): its guide fades out
                    if (endGrabbed) onEndGuideS(null)
                    loupe = null
                    if (moved) onChangedS() else tick++
                    onBusyS(false)
                }
            }
        }) {
        Canvas(Modifier.fillMaxSize()) {
            rev; tick; view; loupe; sel; active; show; photoAt; fitAt; chipAt; chipText; drag; area; grid; guide; chain
            val fw = fr[2] - fr[0]; val fh = fr[3] - fr[1]
            val u = min(size.width, (size.height * fw / fh).toFloat()) / 480f
            // THE GUIDE AS DRAWN THIS FRAME (v52 slice 10): the one on now, or the last one while it fades out; reading
            // the Animatable's value here redraws the photo on every step of the fade
            val ga = gAlpha.value
            val gNow = (guideHere ?: lastGuide[0])?.takeIf { it.s == tab && mode == "mark" && SurveyGuide.shown(ga.toDouble()) }
            // the plane grid: full with its button on, else with the guide's fade (sp.js `gridOn()?1:G?G.a:0`)
            val gridA = if (grid) 1f else if (gNow != null) ga else 0f
            // the same mapping the gesture uses (SurveyPrep.PhotoView), over the canvas as drawn
            val pb = SurveyPrep.PhotoBox(size.width.toDouble(), size.height.toDouble(), fr, ox, oy, iw, ih)
            val zz = view.zoom(); val vx = view.centreX(pb); val vy = view.centreY(pb)
            val kk = view.k(pb)
            drawIntoCanvas { cv ->
                val nc = cv.nativeCanvas
                nc.save(); nc.clipRect(0f, 0f, size.width, size.height)
                nc.translate((size.width / 2 - vx * kk).toFloat(), (size.height / 2 - vy * kk).toFloat()); nc.scale(kk.toFloat(), kk.toFloat())
                // the picture at its offset: view (0, 0) is bitmap (ox, oy) (sp.js drawImage(V.img, -V.ox, -V.oy))
                nc.drawBitmap(img, (-ox).toFloat(), (-oy).toFloat(), Paint().apply { isFilterBitmap = zz < 2.5 })
                nc.restore()
                val m: (Double, Double) -> FloatArray = { x, y -> floatArrayOf(((x - vx) * kk + size.width / 2).toFloat(), ((y - vy) * kk + size.height / 2).toFloat()) }
                fun overlay(c: android.graphics.Canvas, mm: (Double, Double) -> FloatArray, which: String) {
                    if (mode == "lines") {
                        // the plane grid in 1 · Lines (its button on), through the face's own mapping, so it leans with
                        // the photo's perspective and shows whether the four lines are set right (sp.js drawLines' grid)
                        if (which == "main" && grid && frame != null)
                            SurveyPrepDraw.drawGrid(c, frame.a, frame.b, { x, y ->
                                SurveyPrep.apply(frame.m2p, doubleArrayOf(x, y)).let { p -> mm(p[0], p[1]) } }, 1f, density.density)
                        SurveyPrepDraw.drawLines(c, tx, tab, prep.surface(tab), frame, mm, u, which == "loupe")
                        // the reference rectangle over the lines, shown or hidden -- Lines is where it is worked on (v52
                        // slice 12, sp.js `if(which!=='loupe') drawRef`)
                        if (which == "main") SurveyPrepDraw.drawRef(c, tx, tab, prep.surface(tab), mm, u)
                    }
                    else rect?.let { r ->
                        // the editing photo and its loupe are the office's own screen (port 62 slice L2)
                        SurveyPrepDraw.drawMarks(c, tx, SheetMarks.Audience.OFFICE, tab, SurveyPrepDraw.Marks(prep, ab[0], ab[1], r.sq, which, false,
                            sel?.takeIf { it.s == tab }?.id, active, show, size.width, size.height, if (which == "main") tags else null,
                            density.density, drag = drag?.takeIf { it.s == tab }, area = area,
                            gridAlpha = gridA, guide = gNow, guideAlpha = ga, ends = tool == SurveyPrepPalette.SEL,
                            chain = chain?.takeIf { it.s == tab }, frame = frame,
                            // no resize tabs on a picked mark the phase holds read-only (port 62 slice L4)
                            tabs = sel?.takeIf { it.s == tab }?.id?.let { sid -> PhaseTools.editable(prep, sid, stage) } ?: true), mm, u)
                    }
                }
                tags.clear()
                overlay(nc, m, "main")
                // ---- THE FLOATING TRASH (v52): a 36 dp red disc with a white ring beside the picked mark, placed
                // by SurveyPrepTouch.trashSpot so it never covers one of its figures -- a tap on a figure never deletes
                trashAt[0] = null
                // with a type on the disc is not drawn (sp.js 801: "with a drawing tool on, the trash stays in the bar")
                // ... nor where the phase hides the picked item's delete (port 62 slice L4; sp.js fTrashSurf's `!P54.spDelOk`)
                val pickedId = if (mode == "mark" && tool == SurveyPrepPalette.SEL) sel?.takeIf { it.s == tab }?.id?.takeIf { sid -> PhaseTools.delOk(prep, sid, stage) } else null
                val picked = pickedId?.let { sid -> prep.surface(tab).marks.firstOrNull { it.id == sid } }
                // ... or beside a picked Tape, Line or area (v52 slice 9): round the box of its points
                val pickedM = if (picked == null) pickedId?.let { sid -> prep.surface(tab).meas.firstOrNull { it.id == sid && it.pts.isNotEmpty() } } else null
                // ... or a picked chain (v52 slice 11): round the box of its points, clear of them
                val pickedK = if (picked == null && pickedM == null) pickedId?.let { sid -> prep.surface(tab).chains.firstOrNull { it.id == sid && it.nodes.isNotEmpty() } } else null
                val pickedPts: List<DoubleArray>? = pickedM?.pts ?: pickedK?.nodes
                val rr = rect
                if ((picked != null || pickedPts != null) && rr != null) {
                    val dpx = density.density
                    val bb = if (pickedPts != null) pickedPts.map { pq -> m(rr.sq.x(pq[0]), rr.sq.y(pq[1])) }.let { ps ->
                        SurveyPrepTouch.Box(ps.minOf { it[0] }.toDouble(), ps.minOf { it[1] }.toDouble(), ps.maxOf { it[0] }.toDouble(), ps.maxOf { it[1] }.toDouble()) }
                    else if (SurveyPrep.isSpot(picked!!.t)) SurveyPrepDraw.spotOf(picked, rr.sq, m, dpx).let { d ->
                        SurveyPrepTouch.Box((d[0] - d[2]).toDouble(), (d[1] - d[2]).toDouble(), (d[0] + d[2]).toDouble(), (d[1] + d[2]).toDouble()) }
                    else { val p0 = m(rr.sq.x(picked.x0), rr.sq.y(picked.y1)); val q0 = m(rr.sq.x(picked.x1), rr.sq.y(picked.y0))
                        SurveyPrepTouch.Box(p0[0].toDouble(), p0[1].toDouble(), q0[0].toDouble(), q0[1].toDouble()) }
                    // the selected Tape's, Line's or area's end handles: the disc keeps off them (v52 slice 10 review)
                    val handles = pickedPts?.map { pq -> m(rr.sq.x(pq[0]), rr.sq.y(pq[1])).let { q -> doubleArrayOf(q[0].toDouble(), q[1].toDouble()) } }.orEmpty()
                    SurveyPrepTouch.trashSpot(bb, tags.map { it.box }, size.width.toDouble(), size.height.toDouble(), dpx.toDouble(),
                        handles, SurveyGuide.HANDLE_CLEAR_DP * dpx)?.let { ts ->
                        val rad = 18f * dpx; val tcx = ts.left.toFloat() + rad; val tcy = ts.top.toFloat() + rad
                        val pt = Paint(Paint.ANTI_ALIAS_FLAG)
                        pt.style = Paint.Style.FILL; pt.color = android.graphics.Color.rgb(0xE5, 0x48, 0x4D); nc.drawCircle(tcx, tcy, rad, pt)
                        pt.style = Paint.Style.STROKE; pt.strokeWidth = 2f * dpx; pt.color = android.graphics.Color.WHITE
                        nc.drawCircle(tcx, tcy, rad - dpx, pt)
                        // a bin: lid, handle, body with two ribs
                        pt.strokeWidth = 1.8f * dpx; pt.strokeCap = Paint.Cap.ROUND
                        val g = 5.5f * dpx
                        nc.drawLine(tcx - 1.3f * g, tcy - g, tcx + 1.3f * g, tcy - g, pt)
                        nc.drawLine(tcx - .45f * g, tcy - 1.4f * g, tcx + .45f * g, tcy - 1.4f * g, pt)
                        nc.drawRect(tcx - g, tcy - .7f * g, tcx + g, tcy + 1.5f * g, pt)
                        nc.drawLine(tcx - .35f * g, tcy - .3f * g, tcx - .35f * g, tcy + 1.1f * g, pt)
                        nc.drawLine(tcx + .35f * g, tcy - .3f * g, tcx + .35f * g, tcy + 1.1f * g, pt)
                        trashAt[0] = floatArrayOf(tcx, tcy, rad)
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
                        nc.drawBitmap(img, (-ox).toFloat(), (-oy).toFloat(), Paint()); nc.restore()
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
                        // NOR OVER THE POINT THE DRAG IS MEASURED FROM (v52 slice 10, sp.js keep): a Tape's or a Line's
                        // start, or the FIXED end while an end is moved -- the guide's start, on screen
                        val anchors = guideHere?.let { g -> rect?.let { r -> m(r.sq.x(g.o[0]), r.sq.y(g.o[1])) } }
                            ?.let { listOf(doubleArrayOf(it[0].toDouble(), it[1].toDouble())) } ?: emptyList()
                        val sp = SurveyPrepTouch.loupePlace(lp.finger.x.toDouble(), lp.finger.y.toDouble(), size.width.toDouble(),
                            size.height.toDouble(), dia.toDouble(), gap.toDouble(), dd, obst, anchors)
                        one(lp.points[0], sp.left.toFloat(), sp.top.toFloat())
                    }
                }
            }
        }
        // THE TOOL CHIP (v52 slice 7): "Placing Door · tap the photo", top-left, as wide as the photo less 70 dp so
        // the fit button stays clear; ✕ goes back to Select. Hidden with Select on.
        if (chipText != null) {
            Column(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(start = 8.dp, top = 8.dp)) {
                Box(Modifier.fillMaxWidth().padding(end = 62.dp)) {
                    SpToolChip(chipText, SurveyPrepPalette.chipColour(tab, tool),
                        Modifier.onGloballyPositioned { chipAt = it.boundsInRoot() },
                        caution = if (measTool || chainTool) SurveyMeas.caution(tx) else null,
                        onDone = if (chainTool && SurveyChain.showDone(chainN)) ({ onChainFinish() }) else null,
                        asFrom = typeChip?.asFrom ?: -1, cycles = typeChip?.cycles == true, spoken = typeChip?.spoken,
                        onAs = onAs,
                        kindFrom = typeChip?.kindFrom ?: -1, kindTo = typeChip?.kindTo ?: -1, kindLabel = typeChip?.kindLabel,
                        onKind = { kindOpen.value = !kindOpen.value }) { onTool(SurveyPrepPalette.SEL) }
                }
                // THE LIST OF KINDS (port 62 slice L6; the prototype's t53subList): 4 dp under the chip, inside the photo,
                // 280 dp wide at most. A pick is remembered by the tool -- the next tap places that kind -- and shuts it.
                if (kinds != null && kindOpen.value) {
                    SpKindList(kinds, Modifier.padding(top = 4.dp, end = 8.dp).widthIn(max = 280.dp).fillMaxWidth()) { id ->
                        SurveyPrepPalette.rememberSub(spKinds, tool, tab, id); kindOpen.value = false }
                }
            }
        }
        Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) {
            Column(Modifier.onGloballyPositioned { fitAt = it.boundsInRoot() }, horizontalAlignment = Alignment.End) {
                Text(tx.sp_fit, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xAA000000)).clickable {
                        // sp.js V.fit: zoom 1 on the fit rect, and fitted again -- it follows the lines from here
                        // (and, a press outside the chip's list of kinds, it shuts that list: sp.js 6811)
                        kindOpen.value = false; view = view.fit(); tick++ }.padding(horizontal = 8.dp, vertical = 5.dp))
                // THE GRID BUTTON (v52 slice 10) joins the fit column, so it costs no height: the plane grid on the face
                // with it on (#22d3ee), and still by itself while a Tape or a Line is drawn
                Text(tx.sp_grid, color = if (grid) Color(0xFF06232A) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(6.dp))
                        .background(if (grid) Color(0xCC22D3EE) else Color(0xAA000000))
                        .semantics { contentDescription = tx.sp_aria_plane_grid; selected = grid }
                        .clickable(role = Role.Button) { kindOpen.value = false; onGrid() }.padding(horizontal = 8.dp, vertical = 5.dp))
                // the zoom, whenever the view is out of fit (sp.js zl: hidden while fitted; two places below 1)
                view.label()?.let { zl -> Text(zl, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xAA000000)).padding(horizontal = 6.dp, vertical = 3.dp)) }
            }
        }
    }
}

/**
 * One surface's checklist: Mark | Measure | ≈ Photo | Measured (+ D2) | Δ. Tapping a row selects its mark.
 * v52 slice 9 (item 6): every item's TYPE NAME stands on its own line under its code -- "Door", "Electric point",
 * "Marked length", "Marked area · 1.86 m²", and on the room's List tab ([room]) " · Front wall" after it; the column
 * title is "≈ Photo" in full; every Delete is labelled from its item ("Delete F-M1 marked length", "Delete FD1 Door").
 * After the marks come the Lines and areas, each with its one reading row (its length, its perimeter), and the tapes:
 * "Tape · 1234 mm · from the photo" with the note that a tape has no reading box -- and none.
 */
@Composable
private fun SpTable(
    s: String, prep: SurveyPrep.Prep, ab: DoubleArray, rev: Int,
    sel: SpSel?, active: String?, show: String, room: Boolean,
    /** Port 62 slice L4: the project's quote stage -- a row's Delete is hidden where the phase holds its item read-only. */
    stage: Int?,
    onRow: (SurveyPrep.Mark, String) -> Unit,
    onMeasRow: (SurveyPrep.Meas, String?) -> Unit,
    /** A chain's row tapped (v52 slice 11): the chain, and the reading key the row is (FK1.2, FK1.t). */
    onChainRow: (SurveyPrep.Chain, String?) -> Unit,
    /** A code tapped (port 62 slice L3): its Mark sheet opens -- a mark's, a Line's, an area's; the item's id. */
    onSheet: (String) -> Unit,
    onDelete: (String) -> Unit,
    onFocus: (String) -> Unit,
    onValue: (String, Double?) -> Unit,
    onFire: (String) -> Unit,
    emptyText: String,
) {
    rev
    val tx = LocalTx.current
    val els = prep.surface(s).marks
    val meas = prep.surface(s).meas.filter { SurveyMeas.known(it.kind) }
    val chains = prep.surface(s).chains
    if (els.isEmpty() && meas.isEmpty() && chains.isEmpty()) { Text(emptyText, color = SP_MUTED, fontSize = 12.sp, modifier = Modifier.padding(4.dp)); return }
    val where = if (room) " · " + Masters.surface(tx, SurveyPrep.NAME.getValue(s)) else "" // text:value the separator before the face's name
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(tx.common_mark, color = SP_MUTED, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(56.dp))
        // the checklist's column, not a tool: it stays "Measure" (item 6)
        Text(tx.common_measure, color = SP_MUTED, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(tx.common_photo, color = SP_MUTED, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false,
            modifier = Modifier.width(54.dp), textAlign = TextAlign.End)
        Text(tx.common_measured, color = SP_MUTED, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(112.dp), textAlign = TextAlign.Center)
        Text("Δ", color = SP_MUTED, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(36.dp), textAlign = TextAlign.End) // text:value glyph
    }
    /** The first column: the code, the type name on its own line under it, and Delete labelled from the item. The code
     *  of a mark, a Line and an area is a button: it opens the Mark sheet (slice L3; the prototype's markPanel, 6660). */
    @Composable
    fun head(id: String, code: String, colour: Color, name: String?) {
        Column(Modifier.width(56.dp).padding(3.dp)) {
            Text(code, color = colour, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = if (!MarkSheet.opens(prep, id)) Modifier else Modifier.clip(RoundedCornerShape(3.dp))
                    .clickable(role = Role.Button) { onSheet(id) }
                    .semantics { contentDescription = tx.life_aria_open_mark_sheet_x(code) })
            if (name != null) Text(name, color = SP_MUTED, fontSize = 10.sp, lineHeight = 11.sp)
            // a mark's own word -- Drop past Existing, Restore on a Dropped one (port 62 slice L2); Delete for the rest --
            // and none at all where the phase holds the item read-only (slice L4; the prototype's spPost on [data-del])
            if (PhaseTools.delOk(prep, id, stage)) Text(SurveyPrepEdit.deleteWord(tx, prep, id), color = Color(0xFFFF9B7A), fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(6.dp)).border(1.dp, Color(0x88FF9B7A), RoundedCornerShape(6.dp))
                    .clickable { onDelete(id) }.semantics { contentDescription = SurveyPrepEdit.deleteLabel(tx, prep, id) ?: tx.sp_aria_delete_x(code) }
                    .padding(horizontal = 6.dp, vertical = 3.dp))
        }
    }
    /** One reading row: what · ≈ photo · the box and D2 · Δ. */
    @Composable
    fun line(code: String, mm: SurveyPrep.Measure, pick: () -> Unit) {
        val a = prep.actual[code]; val act = active == code
        val d = if (a != null && mm.est != null) a - mm.est!! else null
        Row(Modifier.fillMaxWidth().background(if (act) Color(0xFF3D3410) else Color.Transparent)
            .clickable { pick() }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            // a reading row: one of the four boxes the prototype measured for Devanagari -- 17 sp under मराठी, or the vowel
            // signs below (उजव्या कोपऱ्यापासून) are cut (design-marathi §4; v61c.2 CSS .ms2 .wh)
            Text((if (act) "▶ " else "") + mm.what(tx), color = SP_FG, fontSize = 11.sp, lineHeight = if (tx.mr) 17.sp else 12.sp, modifier = Modifier.weight(1f))
            Text(if (show == "actual") "" else (mm.est?.let { "≈" + it.roundToInt() } ?: tx.sp_site_only), color = Color(0xFFCFD3CB),
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
    for (e in els) {
        val ms = SurveyPrep.measures(s, e, ab[0], ab[1])
        val selM = sel?.id == e.id
        Row(Modifier.fillMaxWidth().background(if (selM) Color(0xFF2A2614) else Color.Transparent)) {
            head(e.id, e.code, SP_GOLD, SurveyPrep.typeName(tx, s, e.t) + where)
            Column(Modifier.weight(1f)) {
                for (mm in ms) { val code = e.code + "." + mm.key; line(code, mm) { onRow(e, code) } }
            }
        }
        if (SurveyPrep.outOf(e, ab[0], ab[1]))
            Text(SurveyPrep.outNote(tx, e.code), color = SP_OUT, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2F28)))
    }
    // THE LINES AND AREAS, then THE TAPES (v52 slice 9)
    for (m in meas.sortedBy { if (it.kind == SurveyMeas.TAPE) 1 else 0 }) {
        val selM = sel?.id == m.id
        val tp = m.kind == SurveyMeas.TAPE
        Row(Modifier.fillMaxWidth().background(if (selM) Color(0xFF2A2614) else Color.Transparent)
            .let { if (tp) it.clickable { onMeasRow(m, null) } else it }) {
            head(m.id, m.code, Color(SurveyPrepDraw.argb(if (tp) SurveyMeas.TAPE_COLOUR else SurveyMeas.LINE_COLOUR)),
                // a tape's one header line is the right column's "Tape · 1234 mm · from the photo" (sp.js markPanel):
                // under its code, nothing -- or "Tape" would be said twice
                if (tp) null else SurveyMeas.listName(tx, s, m, room))
            Column(Modifier.weight(1f).padding(top = 3.dp)) {
                if (tp) {
                    // NO READING BOX: the tape's figure is the photo's
                    Text(SurveyMeas.listName(tx, s, m, room), color = SP_FG, fontSize = 11.sp, lineHeight = 13.sp)
                    Text(SurveyMeas.tapeNote(tx), color = SP_MUTED, fontSize = 10.sp, lineHeight = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 3.dp))
                } else for (mm in SurveyMeas.measures(m)) { val code = m.code + "." + mm.key; line(code, mm) { onMeasRow(m, code) } }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2F28)))
    }
    /**
     * One row of a chain (v52 slice 11, sp.js chainPanel): [label] (FK1.1, "FK1.2 · derived", "total · laser"), the
     * photo's ≈, the box with what the row IS -- its reading, else the derived figure in violet, the laser's total in
     * rose, read-only ([readOnly]: no D2 either) -- and Δ: a segment against its photo figure, the total its check.
     */
    @Composable
    fun chainLine(code: String, label: String, est: Double, shown: Double?, isReading: Boolean, tint: Color?, readOnly: Boolean, delta: Double?, pick: () -> Unit) {
        val act = active == code
        Row(Modifier.fillMaxWidth().background(if (act) Color(0xFF3D3410) else Color.Transparent)
            .clickable { pick() }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text((if (act) "▶ " else "") + label, color = SP_FG, fontSize = 11.sp, lineHeight = if (tx.mr) 17.sp else 12.sp, modifier = Modifier.weight(1f))
            Text(if (show == "actual") "" else "≈" + est.roundToInt(), color = Color(0xFFCFD3CB),
                fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.width(54.dp))
            Row(Modifier.width(112.dp).padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                SpMmField(code, shown, act, Modifier.weight(1f), onFocus = { if (!readOnly) onFocus(code) }, tint = tint, readOnly = readOnly,
                    isReading = isReading) { v ->
                    if (!readOnly) onValue(code, v) }
                if (!readOnly) { Spacer(Modifier.width(3.dp)); SpD2(Modifier, small = true) { onFire(code) } }
            }
            Text(if (show == "both" && delta != null) SurveyPrepDraw.sgn(delta) else "",
                color = if (delta != null && kotlin.math.abs(delta) <= SurveyPrep.TOL) SP_OK else SP_AMB,
                fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.End, modifier = Modifier.width(36.dp))
        }
    }
    // THE CHAINS (v52 slice 11): "Chain · horizontal · 2 parts" under the code, each segment a row, then the total --
    // the laser's when the chain runs edge to edge (read-only then), else its reading or the sum of the segments
    for (k in chains) {
        val selK = sel?.id == k.id
        val kv = SurveyChain.vals(k, prep.actual, ab[0], ab[1])
        Row(Modifier.fillMaxWidth().background(if (selK) Color(0xFF2A2614) else Color.Transparent)) {
            head(k.id, k.code, SP_CHAIN, SurveyChain.listName(tx, s, k, room))
            Column(Modifier.weight(1f)) {
                for ((i, mm) in SurveyChain.measures(k).withIndex()) {
                    val code = k.code + "." + mm.key
                    if (mm.key == SurveyChain.TOTAL) {
                        val ro = SurveyChain.totalReadOnly(kv)
                        // "total", then " · laser" or " · derived", then " · Σ N" were joined in code: one whole key per form
                        // (the card, 28 Oct 2026: sp.total_laser_x, sp.total_laser, sp.total_derived, sp.total_x; measure.total alone)
                        val sum = kv.sum?.takeIf { kv.src != SurveyChain.DERIVED }?.roundToInt()?.toString()
                        val total = when (kv.src) {
                            SurveyChain.LASER -> if (sum != null) tx.sp_total_laser_x(sum) else tx.sp_total_laser
                            SurveyChain.DERIVED -> tx.sp_total_derived
                            else -> if (sum != null) tx.sp_total_x(sum) else tx.measure_total
                        }
                        chainLine(code, total, kv.estT, kv.total, kv.src == SurveyChain.MEASURED,
                            when (kv.src) { SurveyChain.LASER -> SP_CHAIN; SurveyChain.DERIVED -> SP_DER; else -> null }, ro, kv.check) {
                            if (!ro) onChainRow(k, code) else onChainRow(k, null) }
                    } else {
                        val der = kv.der == i
                        val shown: Double? = kv.meas.getOrNull(i) ?: (if (der) kv.value.getOrNull(i) else null)
                        // a derived segment's tag: the separator and sp.derived, as the prototype joins them
                        chainLine(code, if (der) "$code · ${tx.sp_derived}" else code, mm.est ?: 0.0, shown, kv.meas.getOrNull(i) != null,
                            if (der) SP_DER else null, false,
                            shown?.let { it - (mm.est ?: 0.0) }) { onChainRow(k, code) }
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2F28)))
    }
}

/** A reading box: typed mm, kept on Done or when the box loses focus; cleared to remove a reading. [tint]: the figure's
 *  colour when it is not a reading (a chain's derived segment, the laser's total -- v52 slice 11); [readOnly]: shown,
 *  not typed into; [isReading]: [value] is a stored reading, not a derived figure. Only what was TYPED is written
 *  (MmBox.commit, sp.js `change`): focusing a box and leaving it writes nothing, so a derived 2404.5 shown "2405"
 *  never becomes a reading nobody took. */
@Composable
private fun SpMmField(code: String, value: Double?, active: Boolean, modifier: Modifier, onFocus: () -> Unit,
                      tint: Color? = null, readOnly: Boolean = false, isReading: Boolean = true, onCommit: (Double?) -> Unit) {
    val focus = LocalFocusManager.current
    var text by remember(code, value) { mutableStateOf(MmBox.shownText(value)) }
    var focused by remember { mutableStateOf(false) }
    fun commit() {
        if (readOnly) return
        val c = MmBox.commit(value, text, isReading) ?: return
        onCommit(c.value)
    }
    BasicTextField(
        value = text, onValueChange = { t -> text = t.filter { it.isDigit() }.take(6) }, singleLine = true, readOnly = readOnly,
        textStyle = TextStyle(color = tint ?: Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.End),
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
                if (text.isEmpty()) Text("mm", color = SP_DIM, fontSize = 12.sp) // text:value unit
                inner()
            }
        })
}

/**
 * A REFERENCE BOX, W or H (v52 slice 12, sp.js .refin): digits only, five at most (SurveyRef.boxText), committed on Done
 * or when it loses focus -- [onCommit] gets the text, and SurveyRef.typed decides whether anything changed. A cyan ring
 * (#22d3ee88), gold while its own D2 is armed ([armed]): the next reading lands here.
 *
 * A FIGURE TYPED AND NOT YET COMMITTED IS NEVER LOST (slice 12 review): tapping a face tab or a step does not take the
 * focus away, so when the box is re-keyed to another face or leaves the screen, what was typed here is committed with
 * THIS box's [onCommit] -- which the caller binds to the face it was typed for -- as the prototype's change-on-blur
 * fires before the tab's click. A figure that arrives meanwhile (its D2's reading) supersedes what was typed.
 */
@Composable
private fun SpRefField(key: String, value: Double?, armed: Boolean, modifier: Modifier, onCommit: (String) -> Unit) {
    val focus = LocalFocusManager.current
    // what was typed into THIS box and not yet committed; dropped when a new figure arrives
    val pending = remember(key) { arrayOfNulls<String>(1) }
    var text by remember(key, value) { pending[0] = null; mutableStateOf(value?.let { Math.round(it).toString() } ?: "") }
    var focused by remember { mutableStateOf(false) }
    fun commit() { val t = pending[0] ?: return; pending[0] = null; onCommit(t) }
    DisposableEffect(key, value) {
        val c = onCommit
        onDispose { val t = pending[0]; if (t != null) { pending[0] = null; c(t) } }
    }
    BasicTextField(
        value = text, onValueChange = { t -> text = SurveyRef.boxText(t); pending[0] = text }, singleLine = true,
        textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.End),
        cursorBrush = SolidColor(SP_REF),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit(); focus.clearFocus() }),
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(Color.Black)
            .border(1.5.dp, if (armed) SP_GOLD else SP_REF_RING, RoundedCornerShape(6.dp))
            .onFocusChanged { fs ->
                if (fs.isFocused) focused = true
                else if (focused) { focused = false; commit() }
            }.padding(horizontal = 5.dp),
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                if (text.isEmpty()) Text("mm", color = SP_DIM, fontSize = 12.sp) // text:value unit
                inner()
            }
        })
}

/** The laser button: a red dot and D2. */
@Composable
private fun SpD2(modifier: Modifier, label: String = "D2", small: Boolean = false, onClick: () -> Unit) { // text:value the laser, DISTO D2
    Row(modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF3A1512)).border(1.dp, Color(0xFFFF5A4A), RoundedCornerShape(8.dp))
        .clickable { onClick() }.padding(horizontal = if (small) 5.dp else 8.dp, vertical = if (small) 5.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFFF3B2F)))
        Spacer(Modifier.width(3.dp))
        Text(label, color = Color(0xFFFFB3A8), fontSize = if (small) 10.sp else 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

/** [padV]: its own top and bottom padding in place of compact's 4 / the full 8 dp (the Size from row's 28 / 24 dp pills). */
@Composable
private fun SpPill(text: String, on: Boolean, modifier: Modifier, square: Boolean = false, outline: Boolean = false,
                   compact: Boolean = false, padV: androidx.compose.ui.unit.Dp? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(if (square) 10.dp else 50.dp)
    val bg = if (on) (if (outline) Color(0xFF3A3E35) else SP_GOLD) else SP_CHIP
    val fg = if (on) (if (outline) Color.White else Color(0xFF1A1A12)) else SP_MUTED
    Box(modifier.clip(shape).background(bg).let { if (on && outline) it.border(1.5.dp, SP_GOLD, shape) else it }
        .clickable { onClick() }.padding(vertical = padV ?: if (compact) 4.dp else 8.dp), contentAlignment = Alignment.Center) {
        Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1,
            lineHeight = if (compact) 14.sp else TextUnit.Unspecified)   // compact: 4 + 14 + 4 = 22 dp, not the theme's 24 sp line
    }
}

/** [enabled] false: drawn dim and does nothing (a locked mark's nudges); [dim]: drawn dim, still answers (its Delete). */
@Composable
private fun SpButton(text: String, modifier: Modifier, border: Color, compact: Boolean = false, enabled: Boolean = true,
                     dim: Boolean = false, onClick: () -> Unit) {
    val faint = !enabled || dim
    Box(modifier.clip(RoundedCornerShape(7.dp)).background(Color.Black)
        .border(1.5.dp, if (faint) border.copy(alpha = 0.35f) else border, RoundedCornerShape(7.dp))
        .clickable(enabled = enabled) { onClick() }.padding(vertical = if (compact) 4.dp else 8.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (faint) SP_DIM else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
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
 * [ref]: a face squared from its reference rectangle carries "Ref 905 × 2053" under its name (v52 slice 12,
 * tools52.css .ftab .refl): 9 sp on a 10 sp line in #22d3ee, 8.5 on 9 in landscape -- inside the row's own height.
 */
@Composable
private fun SpTab(name: String, on: Boolean, tick: Boolean, modifier: Modifier, ref: String? = null, land: Boolean = false,
                  onClick: () -> Unit) {
    val tx = LocalTx.current
    Box(modifier.clickable(role = Role.Tab) { onClick() }.semantics { selected = on }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = if (ref != null && !land) 2.dp else 0.dp)) {
            Text(buildAnnotatedString {
                    append(name.uppercase())
                    if (tick) withStyle(SpanStyle(color = SP_OK, fontWeight = FontWeight.Bold)) { append(" ✓") } // text:value glyph
                },
                // a face tab: one of the four boxes the prototype measured for Devanagari -- 17 sp under मराठी, or the marks above
                // the line (the ॅ of प्लॅन) are cut (design-marathi §4; v61c.2 CSS .ftab .fl)
                color = if (on) Color.White else SP_TAB, fontSize = 11.sp, lineHeight = if (tx.mr) 17.sp else 14.sp, letterSpacing = 0.5.sp,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
                modifier = Modifier.padding(horizontal = 2.dp))
            if (ref != null) Text(ref, color = SP_REF, fontSize = if (land) 8.5.sp else 9.sp, lineHeight = if (land) 9.sp else 10.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp))
        }
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
/** The chain's rose (v52 slice 11, sp.js KC #fb7185), and a derived segment's violet in its box (sp.js .der). */
private val SP_CHAIN = Color(0xFFFB7185)
private val SP_DER = Color(0xFFC4B5FD)
/** The reference rectangle's cyan (v52 slice 12, sp.js RC #22d3ee), its boxes' ring (#22d3ee88). */
private val SP_REF = Color(0xFF22D3EE)
private val SP_REF_RING = Color(0x8822D3EE)
