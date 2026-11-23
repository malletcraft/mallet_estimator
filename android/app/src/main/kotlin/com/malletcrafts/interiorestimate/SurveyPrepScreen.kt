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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import com.malletcrafts.interiorestimate.pano.DeleteGuard
import com.malletcrafts.interiorestimate.pano.GestureClassifier
import com.malletcrafts.interiorestimate.pano.Landing
import com.malletcrafts.interiorestimate.pano.LaserChip
import com.malletcrafts.interiorestimate.pano.MarkLife
import com.malletcrafts.interiorestimate.pano.MarkSheet
import com.malletcrafts.interiorestimate.pano.MeasureFlow
import com.malletcrafts.interiorestimate.pano.MeasurePad
import com.malletcrafts.interiorestimate.pano.MeasureText
import com.malletcrafts.interiorestimate.pano.Pano360
import com.malletcrafts.interiorestimate.pano.Panorama
import com.malletcrafts.interiorestimate.pano.PhaseTools
import com.malletcrafts.interiorestimate.pano.PhotoFigures
import com.malletcrafts.interiorestimate.pano.Pick
import com.malletcrafts.interiorestimate.pano.PlanSave
import com.malletcrafts.interiorestimate.pano.SaveStay
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
 *    it, each with its checklist of measurements. ONE STEP TO MARK AND TO
 *    MEASURE (port 63 slice I1; DESIGN-imagemeter; Amit, 16 Nov 2026, on build
 *    0.3.337: "do we really need two screen now? which is mark and measure? as
 *    we should be measure where marking is done."): there is no 3 · Measure, no
 *    measure bar, no figure box and nothing under the photo that shows a
 *    measurement. A tap on a figure turns it GOLD; the meter's own button
 *    takes the reading, which lands on the gold figure, and the gold walks on
 *    by itself -- inside the mark, then along the wall; a tap on the gold
 *    figure brings the keypad up in the column, its digits on the gold plate.
 *    NO PHOTO ESTIMATE IS SHOWN (§18; Amit: "No foto estimation at all. It
 *    confuses user"): a mark that is not picked shows only what has been READ,
 *    the picked one what it still asks as a quiet `?`. What is picked, whether
 *    it is HELD (shows its handles, can move) and which figure is gold is ONE
 *    value, Pick.State, and every event that changes it is a function of Pick
 *    in :pano; where a reading lands is Landing; the laser's link and its chip
 *    in the top bar LaserChip; which figures the photo draws PhotoFigures.
 *    Nothing on this screen fires the laser. The row under the photo: Tape,
 *    Line, three types, More ▸ and `▸` (the next unread figure); a tool places
 *    ONE item and goes off, and with no tool on a touch selects.
 *    THE CHAIN TOOL IS OFF (port 63 slice F1; Features.CHAINS -- Amit, 16 Nov
 *    2026: "chain mark is rarely used. get rid of it."): no new chain can be
 *    drawn, and one a room already has is kept, drawn, measured (FK1.1 … and
 *    its total FK1.t) and can be deleted. A HELD Tape, Line or area moves
 *    whole by a drag on its body, as a mark does (slice F1; SurveyGuide).
 *  - The Mark sheet (port 62 slice L3, SpMarkSheet): a tap on a mark's code --
 *    on the held item's row under the photo -- opens it: the moves the
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
 *    is a client wish"); at Estimated and at Client approved 2 · Mark is the
 *    view -- no palette, nothing held; a type tapped in the Wish list stage is BORN a Client
 *    wish (the chip says "· as Client wish"); and a mark of an earlier phase is
 *    read-only -- its controls are hidden, a drag on it says why. Every rule is
 *    PhaseTools in :pano.
 *  - 3 · 360 (v52 slice 17, SurveyPrep360): the same marks, tapes, Lines,
 *    chains and reference on the 360 itself, turned by the finger, and
 *    placed and edited there through the same functions -- one model, two
 *    views; the maths is Pano360 in :pano.
 *  - PLAN, the 7th tab (v52 slice 13): six drawings -- the floor plan, the
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
    /**
     * WHETHER THE CAPTURE THIS PREP IS OF IS FILED WHEN SURVEY PREP OPENS (port 63 P2N): true for a kept capture, opened
     * from its room -- it has its row and its pictures; false for a NEW one, straight after the shutter, which its first
     * ✓ Save files (SaveStay.opened, SaveStay.flow). NO DEFAULT: a caller that forgot it would file nothing, or twice.
     */
    filed: Boolean,
    /**
     * ✓ SAVE (port 63 slice I1b, P2; P2N): the prep and the room size it is squared to now (L, W, H mm -- the size line
     * may have changed it), handed to the caller's keep -- which keeps the size on the capture's row and rewrites the
     * room folder's survey pictures, or, for a NEW capture nothing has filed yet, FILES it: its faces, its pictures and
     * its row, once (SaveStay.keep decides, in the caller, one press at a time). SURVEY PREP STAYS OPEN: nothing here
     * leaves. The caller answers through [report] once per call, when the pictures are written -- their count, or
     * (null, why) when they were not -- and the bar says it (SaveStay.answered).
     */
    onSaved: (SurveyPrep.Prep, DoubleArray, (Int?, String?) -> Unit) -> Unit,
    /** ←, the only way out: the work is kept on the phone either way, and handed back as it stands, with its size. */
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
    // IT OPENS ON 2 · MARK ONCE THE OPEN FACE'S LINES ARE SET (port 63 slice I1b, P1; Amit, 23 Nov 2026: "every time i open
    // a foto, it always start from 1 - lines. its annoying. once lines are set, I dont want this as default screen"):
    // PhaseTools.openedStep -- all four placed by hand and closing (PhaseTools.linesSet); else 1 · Lines, as always.
    var modeSet by remember {
        mutableStateOf(PhaseTools.openedStep(tab, quoteStage, PhaseTools.linesSet(prep, tab, roomSize[0], roomSize[1], roomSize[2])))
    }
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
    // ---- PICKED, HELD, GOLD (port 63 slice I1; DESIGN-imagemeter §3.1): what is picked, whether it is HELD -- picked by
    // his own hand, so it shows its handles and can move -- and which figure the laser's next reading lands on. ONE
    // value, and every event that changes it is a function of Pick in :pano (one test per row of the design's table of
    // 28): this screen asks, and does what the answer says (took, below). It holds no rule of its own.
    var pick by remember { mutableStateOf(Pick.NONE) }
    // 1 · LINES' OWN SELECTION: a boundary line's slot, or the reference rectangle -- never an item, which is `pick`'s
    var lineSel by remember { mutableStateOf<SpSel?>(null) }
    // the part of the held item its row's nudges move: the tab, corner or point last pressed (Pick.handle), else its body
    var part by remember { mutableStateOf("body") }
    /** What is selected NOW, as the painters, the row and the 360 take it: the picked item with the part its nudges
     *  move, else 1 · Lines' own selection. Read from state on every call. */
    fun selNow(): SpSel? = pick.picked?.let { pid -> prep.itemById(pid)?.let { o -> SpSel(o.s, id = pid, part = part) } } ?: lineSel
    // THE KEYPAD (DESIGN-imagemeter §5): open or hidden -- opened only by a tap on the gold figure (Pick.Pad.OPEN) -- and
    // the digits typed on it. They belong to ONE reading, [padKey], the one that was gold when the first key was
    // pressed: whatever leaves that reading commits them to IT (commitPad), never to the one gold by then.
    var padOpen by remember { mutableStateOf(false) }
    var padKey by remember { mutableStateOf<String?>(null) }
    var padText by remember { mutableStateOf(MeasurePad.start(null)) }
    // THE LASER'S LINK AS THE CHIP REMEMBERS IT (LaserChip): when it was last started, and whether a tap asked for it
    var laserMem by remember { mutableStateOf(LaserChip.Mem()) }
    // a reading that landed while a sheet was open or a finger was on a grab (Pick row 20): its key, until the release
    var waiting by remember { mutableStateOf<String?>(null) }
    // the plate that has just taken a reading keeps a gold ring for Landing.RING_MS, so the eye sees where it went
    var ring by remember { mutableStateOf<String?>(null) }
    var ringSeq by remember { mutableIntStateOf(0) }
    // THE GOLD MOVED BY ITSELF (a reading, `▸`): bumped, and the photo pans only if the new gold figure is off screen
    var walked by remember { mutableIntStateOf(0) }
    // THE MARK SHEET (port 62 slice L3): open on one item, or null -- declared with the state, because a reading that
    // lands while it is open waits for it to close (sheetOrFinger, below)
    var markSheet by remember { mutableStateOf<MarkSheet.State?>(null) }
    // what the hand feels when a laser reading lands, fails the guard or is thrown away (Landing.pulses) -- his eyes are
    // on the laser dot, not the phone. Read once, here.
    val haptic = LocalHapticFeedback.current
    val hist = remember { mutableStateListOf<SurveyPrep.Prep>() }
    // "N points re-centred" (v52 slice 4): said once, on the open that made older points 50 mm spots -- a Msg,
    // rendered with the open-time tx (say() keeps a String: M3a's rule)
    var note by remember { mutableStateOf(SurveyPrepFile.pointsNote(opened)?.invoke(tx)) }
    // a Msg, drawn in whatever language is on when the bar is drawn (design-marathi §2.4); the opened file's own
    // word (SurveyPrepFile.savedText) is a Msg too since port 61c slice M3b
    var savedMsg by remember { mutableStateOf<Msg>(SurveyPrepFile.savedText(opened)) }
    // ✓ SAVE SAVES AND STAYS (port 63 slice I1b, P2): what the last press saved, and where its pictures are -- the bar
    // says the save's own words until the next change (SaveStay.bar), and an older press's late answer is ignored
    // ... and whether the capture is FILED (port 63 P2N): a NEW capture opens not filed, and its first ✓ Save files it
    var saveMem by remember { mutableStateOf(SaveStay.opened(filed)) }
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
    // A NOTE THAT STAYS UNTIL THE NEXT TOUCH (Landing.Route.Refused.stays; DESIGN-imagemeter §4.4): a reading thrown
    // away is said for as long as it takes him to look down -- every other note goes after 2.6 s
    var noteStays by remember { mutableStateOf(false) }
    fun say(t: String) { note = t; noteStays = false; noteSeq++ }
    fun sayStay(t: String) { note = t; noteStays = true; noteSeq++ }
    /** The next touch -- on the photo, the 360 or a tool -- takes a note that stayed. */
    fun touched() { if (noteStays) { noteStays = false; note = null } }
    LaunchedEffect(noteSeq) { if (note != null && !noteStays) { delay(SurveyPrepEdit.SAY_MS); note = null } }
    LaunchedEffect(ringSeq) { if (ring != null) { delay(Landing.RING_MS); ring = null; rev++ } }
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
    /** The face [s]'s four lines are set -- placed by hand and closing (P1): the face opens on 2 · Mark. */
    fun linesSetOf(s: String): Boolean = PhaseTools.linesSet(prep, s, roomSize[0], roomSize[1], roomSize[2])

    // ---- THE PHASE (port 62 slice L4; the prototype's P54.spPre, index61c2.html 4204-4224). The project has ONE phase,
    // its S5 quote stage, and survey prep shows only that phase's tools -- hidden, never greyed. THE STEPS ARE THREE
    // (port 63 slice I1): 1 · Lines, 2 · Mark, 3 · 360, and 2 · Mark is in every phase -- what a phase takes away is what
    // can be DONE in it. The step it is on is the one last set, and 2 · Mark on a face whose four lines do not close
    // falls to 1 · Lines (PhaseTools.routeMode). Read on every recomposition, so a sync that moves the stage while the
    // screen is open takes the tool with it, as the prototype's render does.
    val mode = PhaseTools.routeMode(modeSet, quoteStage) { StepRoute.isFace(tab) && frameOf(tab) != null }
    /** Where Pick's events happen now: the room, the phase, the open step and a face's size. */
    fun scene() = Pick.Scene(prep, quoteStage, mode) { s -> ab(s) }
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

    // ---- THE DISTO D2 (port 63 slice I1; DESIGN-imagemeter §4). NOTHING ON THIS SCREEN'S 2 · MARK FIRES IT: the meter's
    // own button takes the reading, which lands on the GOLD figure (Landing). The link is started by itself when a
    // figure turns gold and when survey prep opens at Survey, and the chip in the top bar shows it -- every rule of
    // when it starts, when it asks for the permission and when it speaks is LaserChip in :pano.
    val disto = remember { DistoClient(context) }
    var distoState by remember { mutableStateOf(DistoClient.State.OFF) }
    DisposableEffect(Unit) { onDispose { disto.stop() } }
    val blePerms = remember {
        if (android.os.Build.VERSION.SDK_INT >= 31) arrayOf(android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    /** The client's state as LaserChip names it: the two enums carry the same four names. */
    fun link(): LaserChip.Link = LaserChip.Link.valueOf(distoState.name)
    fun blePermitted(): Boolean = blePerms.all { perm ->
        androidx.core.content.ContextCompat.checkSelfPermission(context, perm) == android.content.pm.PackageManager.PERMISSION_GRANTED }
    /** What LaserChip answered, done -- its memory kept, the link started, its line said. True: it asks for the
     *  Bluetooth permission, which is the caller's to launch (chipDo; the launcher is declared under this). */
    fun chipDid(step: LaserChip.Step): Boolean {
        laserMem = step.mem
        if (step.start) disto.start()
        step.say?.let { line -> say(line(tx)) }
        return step.askPermission
    }
    val askBle = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        chipDid(LaserChip.permission(laserMem, granted.values.all { it }, System.currentTimeMillis()))
    }
    fun chipDo(step: LaserChip.Step) { if (chipDid(step)) askBle.launch(blePerms) }
    // survey prep opens: the link is tried at once where readings are keyed, silently (LaserChip.opened)
    LaunchedEffect(Unit) { chipDo(LaserChip.opened(laserMem, link(), blePermitted(), quoteStage, System.currentTimeMillis())) }
    /** WHAT THE HAND FEELS (Landing.pulses): one tick for a reading that landed, a double buzz for one that fails the
     *  guard, a long one for a reading thrown away. The pattern is :pano's; this plays it. */
    fun buzz(h: Landing.Haptic?) {
        if (h == null) return
        scope.launch {
            var at = 0L
            for (p in Landing.pulses(h)) {
                if (p.atMs > at) { delay(p.atMs - at); at = p.atMs }
                haptic.performHapticFeedback(if (p.strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
            }
        }
    }
    /** A READING IS WRITTEN: the laser's, or a figure typed on the keypad -- THE ONE PLACE THIS SCREEN ASSIGNS
     *  prep.actual. What follows it -- whether the gold moves, what is felt and said -- is Landing's, asked by the caller. */
    fun reading(code: String, v: Double) { prep.actual[code] = v; changed() }
    /** The keypad's text NOW: the digits typed for the gold figure, else that reading as stored -- fresh
     *  (MeasurePad.start: the STORED reading or empty, never a figure nobody measured). */
    fun padNow(): MeasurePad.State =
        if (padKey != null && padKey == pick.gold) padText else MeasurePad.start(pick.gold?.let { g -> prep.actual[g] })
    /** A key of the keypad pressed: the digits are the gold figure's from here on. */
    fun padPress(key: MeasurePad.Key) {
        val code = pick.gold ?: return
        val now = padNow()
        padKey = code; padText = MeasurePad.press(now, key); rev++
    }
    /**
     * WHAT WAS TYPED IS WRITTEN, FOR THE READING IT WAS TYPED FOR -- through MmBox.commit alone (MeasurePad.commit), so
     * an untouched keypad writes nothing, the figure already stored writes nothing, and an emptied keypad over a stored
     * reading removes it: THE ONE PLACE THIS SCREEN REMOVES A READING. Called by whatever leaves the figure -- Next ▸,
     * ⌄, a tap on the photo (Pick.Pad.SHUT_STORE), ← and ✓ Save -- and, for every other way the gold changes, by the
     * effect below. Twice is once: the digits are forgotten as they are written. True: a figure was WRITTEN.
     */
    fun commitPad(): Boolean {
        val code = padKey ?: return false
        val typed = padText
        padKey = null
        if (prep.ownerOf(code) == null) return false
        val c = MeasurePad.commit(prep.actual[code], typed) ?: return false
        val v = c.value
        if (v != null) { reading(code, v); return true }
        prep.actual.remove(code); changed()
        return false
    }
    LaunchedEffect(pick.gold) { commitPad() }
    /** What an event does to the keypad (Pick.Pad): opened, shut with what was typed stored, or shut and dropped. */
    fun padDoes(pad: Pick.Pad) {
        when (pad) {
            Pick.Pad.KEEP -> Unit
            Pick.Pad.OPEN -> padOpen = true
            Pick.Pad.SHUT_STORE -> { commitPad(); padOpen = false }
            Pick.Pad.SHUT_DROP -> { padKey = null; padOpen = false }
        }
    }
    /** A figure TURNED gold (Pick.turnedGold): the moment of measuring -- the laser's link is started if it may be. */
    fun goldTurned(before: Pick.State) {
        if (Pick.turnedGold(before, pick)) chipDo(LaserChip.gold(laserMem, link(), blePermitted(), quoteStage, System.currentTimeMillis()))
    }
    /**
     * WHAT A Pick EVENT ANSWERED, DONE (port 63 slice I1; DESIGN-imagemeter §19.2): the keypad as the answer says --
     * AFTER Pick was asked, never before, or a tap on the gold plate would store half a figure -- then picked / held /
     * gold, the part the nudges move, and what the screen says: the refusal, the wall-finished note, or the check's
     * sentence when the plate that turned gold carries `!`. [face]: the face a wall-finished note names. An answer
     * that says "no change at all" (Pick.Out.stay) changes nothing here either.
     */
    fun took(o: Pick.Out, face: String = tab) {
        if (o.stay) return
        val before = pick
        padDoes(o.pad)
        pick = o.state
        part = o.nudge ?: "body"
        // the keypad is the gold figure's: with nothing gold it is not asked for any more
        if (o.state.gold == null) padOpen = false
        if (o.byItself) walked++
        val fin = o.finished
        when (o.refused) {
            MeasureFlow.Refused.NOT_IN_ROOM -> say(MeasureText.notInRoom(tx))
            MeasureFlow.Refused.READ_ONLY -> PhaseTools.readingRefusal(tx, quoteStage)?.let { no -> say(no) }
            null -> if (fin != null) say(Pick.finishedText(tx, scene(), face, fin))
                    else if (Pick.turnedGold(before, o.state)) o.state.gold?.let { g -> MeasureFlow.guardText(tx, prep, g) { s -> ab(s) } }?.let { t -> say(t) }
        }
        goldTurned(before)
        rev++
    }
    /** WHAT FOLLOWS A READING THAT IS IN (Landing.After), DONE: the keypad, picked / held / gold, what the hand feels,
     *  the note, the plate that rings, and the pan if the gold moved by itself. */
    fun landed(a: Landing.After) {
        val before = pick
        padDoes(a.pad)
        pick = a.state
        part = "body"
        if (a.state.gold == null) padOpen = false
        buzz(a.haptic)
        a.note?.let { line -> say(line(tx)) }
        a.ring?.let { r -> ring = r; ringSeq++ }
        if (a.byItself) walked++
        goldTurned(before)
        rev++
    }
    /** A sheet or the menu is open, or a finger is on the photo: a reading that lands now is written, and the gold
     *  WAITS for the release (Pick row 20). */
    fun sheetOrFinger(): Boolean = photoBusy || markSheet != null || moreOpen || ctx != null
    /** The sheet closed, or the finger lifted: a reading that landed meanwhile moves the gold on now (Landing.released). */
    fun released() {
        val code = waiting ?: return
        if (sheetOrFinger()) return
        waiting = null
        landed(Landing.released(scene(), pick, code))
    }
    fun undo() {
        if (hist.isEmpty()) return
        val old = hist.removeAt(hist.size - 1)
        // Readings are not geometry: the ones taken since stay, and a reading a delete took away comes back with its
        // owner -- a mark, a Line's or an area's (v52 slice 9), a chain's FK1.1 … FK1.t (slice 11); and a code handed
        // out since is not handed out again: the web may hold readings under it (v52 slice 8). SurveyPrepEdit.undoTo, tested
        val back = SurveyPrepEdit.undoTo(prep, old)
        hideSnack(); ctx = null
        prep = back; lineSel = null
        // ↶ (Pick row 23): the gold is kept while its owner is still there, and its owner is then the picked item; else
        // nothing is picked. Never held. What was typed for a reading let go here is written (Pick.Pad.SHUT_STORE).
        took(Pick.undone(scene(), pick))
        // a reference box's D2 still out is LEFT (slice 12 review): its reading is set aside, never a mark's, and a
        // reference undone away never takes one (SurveyRef.route)
        refShot = SurveyRef.leave(refShot)
        changed()
    }
    /** A REFERENCE'S W OR H SET (v52 slice 12, sp.js setRefDim) -- typed into its box or read by its own D2 -- on face
     *  [s]: the reference's figure, rounded to the mm, never a reading; one undo step, none when nothing changed.
     *  Declared here, above the D2's reading handler that calls it. */
    fun setRefDimS(k: String, v: Double?, s: String) {
        snapshot()
        if (!SurveyRef.setDim(prep.surface(s), k, v)) { hist.removeAt(hist.size - 1); return }
        changed()
    }
    // the laser's words rendered LIVE on the one keyed screen that says them (port 61c slice M6) -- and only when
    // LaserChip lets them be said: an attempt nobody asked for fails silently, the chip alone showing it
    disto.onState = { st, n -> distoState = st; chipDid(LaserChip.state(laserMem, LaserChip.Link.valueOf(st.name), n)) }
    // the METER refused a measurement (its own error code): he pressed its button, so it is always said
    disto.onRefused = { why -> say(why(tx)) }
    // A READING ARRIVES (Landing; DESIGN-imagemeter §4.3, §4.4): into an armed reference box of 1 · Lines; onto the gold
    // figure; or thrown away -- said, felt, and its note kept until the next touch. Nothing is held back for a later tap.
    disto.onReading = { r ->
        val route = Landing.route(refShot, scene(), pick, r.mm, System.currentTimeMillis())
        refShot = null
        when (route) {
            is Landing.Route.Ref -> setRefDimS(route.k, r.mm.toDouble(), route.s)
            is Landing.Route.Refused -> {
                buzz(route.haptic)
                if (route.stays) sayStay(route.text(tx)) else say(route.text(tx))
            }
            is Landing.Route.Gold -> {
                reading(route.code, r.mm.toDouble())
                val after = Landing.after(scene(), pick, route.code, sheetOrFinger())
                if (after.waits) waiting = route.code
                landed(after)
            }
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
        // readings came or went under the pick: what is gold must still be asked (Pick.settle)
        val kept = Pick.settle(scene(), pick)
        if (kept != pick) { pick = kept; if (kept.gold == null) padOpen = false; rev++ }
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
        if (mode != StepRoute.LINES || !StepRoute.isFace(tab) || SurveyPrep.isPlan(tab)) return@LaunchedEffect
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
        !StepRoute.isFace(tab) -> emptyList()   // the Plan tab draws from the marks, not from a squared photo
        mode == StepRoute.MARK -> listOf(tab)
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
    // WHAT A TAP, A PLACEMENT, A READING OR `▸` PICKS, HOLDS AND TURNS GOLD is Pick everywhere below (port 63 slice I1):
    // each handler asks the one function of its event and hands the answer to took().
    /** The room changed under the pick -- a clear, Start over, a sheet's Done, the stage moved: picked, held and gold as
     *  the room and the phase still allow them (Pick.settle). */
    fun settled() {
        val kept = Pick.settle(scene(), pick)
        if (kept != pick) { pick = kept; part = "body"; if (kept.gold == null) padOpen = false; rev++ }
    }
    /**
     * THE 360's PRESS (its gestures stay as built, DESIGN-imagemeter §11): a thing pressed there is picked and held as
     * a tap on its body is (Pick.body; nothing is ever gold on the 360), with the part grabbed -- the part its row's
     * nudges move. A press that is no item's (the reference) is 1 · Lines' kind of selection.
     */
    fun grabbed360(g: SpSel) {
        val gid = g.id
        if (gid == null || prep.itemById(gid) == null) { lineSel = g; rev++; return }
        took(Pick.body(scene(), pick, gid))
        part = g.part
    }
    /** A LONG PRESS ON AN ITEM (Pick row 3): picked and held, and its options open -- until slice I2 today's menu, at
     *  the press ([at], root px). On the reference rectangle: its own Lock · Cancel, as before. */
    fun longPressed(mid: String, at: Offset) {
        armedClear = null
        if (SurveyRef.isId(mid)) { ctx = SpCtx(mid, at.x, at.y); return }
        val o = Pick.longPress(scene(), pick, mid)
        took(o)
        if (o.options) ctx = SpCtx(mid, at.x, at.y)
    }
    // ---- THE MARK SHEET (port 62 slice L3; the prototype's L53 sheet, index61c2.html 3012-3115): open on one item, or
    // null. Its state is MarkSheet.State in :pano, a value: every tap and keystroke REPLACES it through MarkSheet's pure
    // functions, and nothing reaches the prep until Done (sheetDone). Its state is declared with the screen's; its
    // opening HERE, above the two placements, which open a new Note's sheet -- a local function is visible only below
    // its declaration.
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
     * size centred on ([mx], [my]) mm and kept inside the face (SurveyPrep.placeAt), the new mark picked and HELD, its
     * first unread figure gold at Survey (Pick.placed, row 14), one undo step. Off the face it says "Tap inside the
     * front wall to place it" and places nothing. THE TOOL PLACES ONE ITEM AND GOES OFF (port 63 slice I1;
     * SurveyPrepPalette.afterPlaced): he measures where he places, and a tool left on would place another under the
     * next tap. [t] is the type that was on when the finger went DOWN (the photo's press captures it).
     */
    fun placeMark(t: String, mx: Double, my: Double): SurveyPrep.Mark? {
        val s = tab; val a = ab(s)
        if (mode != StepRoute.MARK || SurveyPrep.types(s).none { it.letter == t }) return null
        if (!SurveyPrep.onFace(a[0], a[1], mx, my)) { say(SurveyPrep.placeMiss(tx, s)); return null }
        // BORN WHAT THE PHASE PLACES (port 62 slice L4; the prototype's L53.placed, index61c2.html 2959): a Client wish in
        // the Wish list stage, Vendor added at Vendor review and Revisions, a v52 mark at Survey -- and NOTHING, with no
        // undo step, where the phase has no palette. PhaseTools.place decides, stamps and takes the one undo step.
        // OF THE KIND THE TOOL REMEMBERS for this type on this kind of surface (port 62 slice L6; T53.newFields): its
        // first until one is picked on the chip or in a Mark sheet.
        val placed = PhaseTools.place(prep, s, t, mx, my, a[0], a[1], java.util.UUID.randomUUID().toString().take(8),
            quoteStage, asPicked, java.time.Instant.now(), SurveyPrepPalette.subFor(spKinds, t, s)) { snapshot() } ?: return null
        val e = placed.mark
        took(Pick.placed(scene(), e.id)); toolSet = SurveyPrepPalette.afterPlaced(tool)
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
        if (mode != StepRoute.PANO || !StepRoute.isFace(tab) || s !in SurveyPrep.SURFACES) return null
        Pano360.placeRefusal(tx, tab, s, t)?.let { say(it); return null }
        val a = ab(s)
        // born what the phase places, or not placed at all, exactly as on 2 · Mark (PhaseTools.place; slice L4)
        // ... of the kind remembered for THE SURFACE IT LANDS ON, [s], never the open tab's (port 62 slice L6; the L5
        // review, finding 2): a Plumbing point picked on a wall and tapped onto the floor is the floor's kind
        val placed = PhaseTools.place(prep, s, t, mx, my, a[0], a[1], java.util.UUID.randomUUID().toString().take(8),
            quoteStage, asPicked, java.time.Instant.now(), SurveyPrepPalette.subFor(spKinds, t, s)) { snapshot() } ?: return null
        val e = placed.mark
        tab = s; took(Pick.placed(scene(), e.id)); toolSet = SurveyPrepPalette.afterPlaced(tool)
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
                took(Pick.placed(scene(), c.id)); toolSet = SurveyPrepPalette.afterPlaced(tool)
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
        touched(); flushChain()
        toolSet = SurveyPrepPalette.next(tool, t); moreOpen = false; dropDrafts()
        if (t != SurveyPrepPalette.SEL) note = null
    }
    // THE STEP'S AND THE TOOL'S MOVE IS MADE FOR GOOD (spPre writes st.mode and st.tool, 4208-4210): once a step or a
    // tool has been moved on, the one last set is forgotten -- so a face whose lines did not close keeps 1 · Lines open
    // while its fourth line is dragged into place, and a stage moved back later does not bring a tool back by itself. A
    // chain half drawn is ended as any tool change ends it; the More sheet shuts.
    LaunchedEffect(modeSet, toolSet, quoteStage, mode) {
        if (modeSet != mode) modeSet = mode
        if (toolSet != tool) { flushChain(); toolSet = tool; moreOpen = false; dropDrafts() }
        // PICKED, HELD AND GOLD FOLLOW THE PHASE AND THE STEP (Pick.settle): a stage that moves past Survey while the
        // screen is open leaves nothing gold -- no keypad, no chip -- and nothing held where the phase holds nothing
        settled()
    }
    /**
     * A NEW NOTE THAT WENT AGAIN with no words (port 62 slice L6; the prototype's removeFresh, index61c2.html 3068-3070):
     * MarkSheet has taken it off its surface. The placement's own undo step is taken back with it -- when it is still
     * the last one (MarkSheet.takesBackUndo) -- so an empty Note leaves no undo entry; it is no longer picked; and the
     * screen says "Empty note removed", with no UNDO.
     */
    fun noteRemoved(st: MarkSheet.State) {
        if (MarkSheet.takesBackUndo(st, hist.size)) hist.removeAt(hist.size - 1)
        took(Pick.deleted(pick, st.id))
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
            MarkSheet.Outcome.DONE -> { changed(); settled(); if (done.amber) say(MarkLife.afterEstimate(tx)) }
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
    /** DELETE (the floating trash while the item is held, and the long-press menu -- no row under the photo deletes,
     *  port 63 slice I1): '<code> deleted' with
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
        // ONE ↶ STEP, AND THE SCREEN SAYS WHAT WENT (port 63 slice F1): every one-tap delete -- the floating trash and
        // the long-press menu, on the photo and on the 360 -- comes through here, takes exactly this one
        // snapshot and answers "FD1 deleted" with UNDO, so a slip is seen and taken back at once.
        // NO STEP IS TAKEN AND THEN TAKEN BACK (the F1 review, finding 4; the note in RESTORE above): the model refuses
        // a LOCKED item and a mark PAST EXISTING and nothing else (DeleteGuardTest holds it to exactly those), and both
        // were answered above, each in its own words, before any step -- so the one call below always deletes.
        snapshot(); prep.deleteMark(mid)
        // nothing is picked, held or gold when it was the picked one (Pick row 24); what was typed for it is dropped
        took(Pick.deleted(pick, mid))
        snack(SurveyPrepEdit.deleted(tx, code))
        changed()
    }
    fun nudgeMark(dx: Double, dy: Double) {
        val sl = selNow() ?: return; val mid = sl.id ?: return
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
        // Lock or Unlock (Pick row 17): picked, NOT held -- a locked item never is, an unlocked one not until it is tapped
        if (prep.itemById(mid) != null) took(Pick.locked(scene(), pick, mid))
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
        if (!on && SurveyRef.isId(lineSel?.id)) lineSel = null
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
        say(SurveyRef.shownOnRoute(tx, s, v)); changed()
    }
    /** A reference box's own D2 (sp.js data-reffire): the next reading is that box's -- its W or H, never a reading.
     *  THESE TWO BUTTONS STILL FIRE THE METER until slice I4 (DESIGN-imagemeter §4.1): linked, it measures; not linked,
     *  the link is asked for exactly as a tap on the laser chip asks (LaserChip.tap), and that attempt speaks. */
    fun fireRef(k: String) {
        val s = tab
        if (s !in SurveyPrep.SURFACES || SurveyRef.of(prep.surface(s))?.q == null) return
        refShot = SurveyRef.arm(s, k, System.currentTimeMillis()); rev++
        if (distoState == DistoClient.State.READY) disto.measure()
        else chipDo(LaserChip.tap(laserMem, link(), blePermitted(), System.currentTimeMillis()))
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
    /** Duplicate: the copy 100 mm away with the next code, picked and held, its first unread figure gold (Pick row 15). */
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
        if (tab != s) tab = s
        val nid = java.util.UUID.randomUUID().toString().take(8)
        if (prep.markById(mid) != null) {
            val c = SurveyPrepEdit.duplicate(prep, mid, a[0], a[1], nid)
            if (c == null) { hist.removeAt(hist.size - 1); return }
            took(Pick.duplicated(scene(), c.id))
        } else if (prep.chainById(mid) != null) {
            // a chain (v52 slice 11): 100 mm across its axis, the next K number, picked, its first segment active
            val c = SurveyPrepEdit.duplicateChain(prep, mid, a[0], a[1], nid)
            if (c == null) { hist.removeAt(hist.size - 1); return }
            took(Pick.duplicated(scene(), c.id))
        } else {
            // a Tape, a Line or an area (v52 slice 9): the next number of its own prefix, picked, its reading active
            val c = SurveyPrepEdit.duplicateMeas(prep, mid, a[0], a[1], nid)
            if (c == null) { hist.removeAt(hist.size - 1); return }
            took(Pick.duplicated(scene(), c.id))
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
    /**
     * `▸`, AND THE KEYPAD'S NEXT ▸ (Pick rows 21, 22; Landing.keyed): what was typed is stored first. A figure WRITTEN
     * by that -- Next ▸ on typed digits -- moves the gold as a reading that passes does (a typed figure is deliberate:
     * one that fails the guard keeps its `!`, the note says so, and the gold still moves); with nothing written it is
     * `▸`: the face's next unread figure, accepting a `!`. The owner is picked WITHOUT handles.
     */
    fun nextFigure() {
        val g = pick.gold
        val typed = padKey != null && padKey == g
        if (typed && g != null && commitPad()) landed(Landing.keyed(scene(), pick, g))
        else { commitPad(); took(Pick.next(scene(), pick, tab)) }
    }
    /**
     * A TAPE OR A LINE LET GO (v52 slice 9, sp.js measureDown.end): off the surface it is REFUSED -- "Only things on
     * this surface measure correctly.", nothing plotted, NO undo entry; under 20 mm nothing happens; otherwise it is
     * plotted with its next code (F-T1, F-M1 …), picked and held, a Line's reading gold (Pick.placed), and the tool
     * goes off. One ↶ step.
     */
    fun endDrag(d: SurveyMeas.Draft) {
        mdrag = null
        when (SurveyMeas.end(d)) {
            SurveyMeas.End.REFUSED -> snack(SurveyMeas.offRefusal(tx), undo = false)
            SurveyMeas.End.SHORT -> Unit
            SurveyMeas.End.PLOTTED -> {
                snapshot()
                val m = prep.addMeas(d.s, d.kind, d.pts(), java.util.UUID.randomUUID().toString().take(8))
                took(Pick.placed(scene(), m.id)); toolSet = SurveyPrepPalette.afterPlaced(tool)
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
                took(Pick.placed(scene(), m.id)); toolSet = SurveyPrepPalette.afterPlaced(tool)
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
        lineSel = null; settled(); menuOpen = false; ctx = null; dropDrafts()
        // a mark past Existing is kept, locked or not, and the snackbar names it (port 62 slice L2; SurveyPrepEdit.clearedSnack)
        snack(SurveyPrepEdit.clearedSnack(tx, r))
        changed()
    }
    fun nudgeLine(slot: String, mm: Double, deg: Double) {
        val s = tab; val sf = prep.surface(s)
        if (SurveyPrep.linesLocked(sf)) { say(SurveyPrepEdit.linesRefusal(tx)); return }
        lineSel = SpSel(s, line = slot)
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
    // a face or a step changed: no tool on, the More sheet shut, a chain half drawn ended first (sp.js showTab /
    // setMode: kFlush, then gClear) -- and NOTHING IS PICKED, HELD OR GOLD ON ENTRY (Pick row 25): he may be about to
    // mark; `▸` or a tap on a figure starts the measuring
    fun showTab(t: String) {
        // THE OPEN FACE'S OWN TAB IS NO CHANGE AT ALL (Pick row 26): the figure he chose, the picked mark and what he is
        // typing stay -- restarting there dropped the dimension the laser was about to fill
        val o = Pick.tab(pick, tab, t)
        if (o.stay) return
        flushChain(); refShot = SurveyRef.leave(refShot); if (StepRoute.isPlan(t)) planFit++ else planExp = false
        // on 3 · 360 a face tab turns the 360 to that face (sp.js showTab: P3.face)
        if (mode == StepRoute.PANO && StepRoute.isFace(t)) p3Look = Pano360.faceView(t, 1.0, 1.0)
        // what was typed is stored before the face is left (Pick.Pad.SHUT_STORE)
        took(o)
        // THE FACE OPENS ON 2 · MARK ONCE ITS LINES ARE SET (port 63 slice I1b, P1), on 1 · Lines while they are not; the
        // Plan keeps the step, and 3 · 360 stays 3 · 360 (PhaseTools.openStep). 1 · Lines is one tap away on the strip.
        modeSet = PhaseTools.openStep(t, mode, quoteStage, linesSetOf(t))
        tab = t; lineSel = null; menuOpen = false; toolSet = SurveyPrepPalette.SEL; moreOpen = false; ctx = null; armedClear = null; dropDrafts(); rev++
    }
    fun setMode(m: String) {
        // a step the phase does not show cannot be opened, and 2 · MARK ON A FACE WHOSE FOUR LINES DO NOT CLOSE is refused
        // in every phase, in the words it always was (Pick row 28; PhaseTools.routeAsk): nothing changes -- not the step,
        // the pick or the gold. The open step itself, or a thumb under the strip routed to it, is no change at all (row 26).
        val ask = PhaseTools.routeAsk(m, quoteStage) { StepRoute.isFace(tab) && frameOf(tab) != null }
        if (ask == PhaseTools.Ask.LINES_OPEN) say(PhaseTools.routeRefusal(tx))
        val o = Pick.step(pick, mode, m, tab, ask)
        if (o.stay) return
        flushChain(); refShot = SurveyRef.leave(refShot)
        // 3 · 360 opens facing the open face (sp.js setMode: P3.face)
        if (m == StepRoute.PANO) p3Look = Pano360.faceView(tab, 1.0, 1.0)
        took(o)
        modeSet = m; lineSel = null; toolSet = SurveyPrepPalette.SEL; moreOpen = false; ctx = null; dropDrafts(); rev++
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
    // NOTHING SCROLLS (the list, the one thing that did, went with port 63 slice M2): the photo has weight 1, so it gets
    // every dp the controls leave, in both orientations; the status, the laser sizes, the web lines, Start over and
    // Discard sit behind ⋮ in the bar, with ✓ Save beside it.
    val conf = LocalConfiguration.current
    val land = conf.screenWidthDp > conf.screenHeightDp
    // a FACE has lines, steps, marks and its own clear; the Plan tab has none of them (slice 13)
    val face = StepRoute.isFace(tab)
    // 2 · Mark and 3 · 360 edit the same marks: the held item's row, the tool row and More ▸ serve both
    val marking = face && (mode == StepRoute.MARK || mode == StepRoute.PANO)
    val planTab = StepRoute.isPlan(tab)
    val dens = LocalDensity.current
    // Where each step of the strip is, and the strip's own top-left, in root px: a press within 48 dp under a step
    // (the caption, the top of the photo) is that step's -- StepRoute.stepFor, in :pano.
    // Only ever read by the pointer handlers, never by composition, so laying them out recomposes nothing.
    val stepBoxes = remember { mutableStateMapOf<String, Rect>() }
    var stripAt by remember { mutableStateOf(Offset.Zero) }
    var capAt by remember { mutableStateOf(Offset.Zero) }
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
            PhaseTools.routeSteps(quoteStage).mapNotNull { k ->
                stepBoxes[k]?.let { r -> StepRoute.Step(k, r.left.toDouble(), r.top.toDouble(), r.right.toDouble(), r.bottom.toDouble()) } },
            stripAt.y.toDouble(), dens.density.toDouble())
    // The long-lived pointer handlers below read these, so a handler started on an earlier frame acts on this one.
    val routeS by rememberUpdatedState(newValue = { x: Float, y: Float -> stepAt(x, y) })
    val goStepS by rememberUpdatedState(newValue = { k: String -> setMode(k) })

    // ---- 3 · 360 (v52 slice 17): what a press on the 360 does -- the SAME edits 2 · Mark makes, through the same
    // functions, into the same prep fields; only the surface comes from the press (the tab follows it, sp.js st.tab=s)
    val p3Actions = P3Actions(
        grab = { g -> touched(); grabbed360(g) },
        focus = { s -> if (tab != s) { tab = s; rev++ } },
        before = { snapshot() },
        moved = { rev++ },
        changed = { changed() },
        revert = { revertPress() },
        // a tap on empty 360 lets the picked item go (Pick.empty: nothing is ever gold on the 360)
        deselect = { touched(); lineSel = null; took(Pick.empty(pick)) },
        place = { s, t, mx, my -> place360(s, t, mx, my) },
        pickChain = { s, cid -> if (tab != s) tab = s; took(Pick.body(scene(), pick, cid)) },
        pickMeas = { s, mid -> if (tab != s) tab = s; took(Pick.body(scene(), pick, mid)) },
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
        longPress = { mid, at -> longPressed(mid, at) },
        look = { v -> p3Look = v },
        // ✕ on the chip: a chain half drawn is DROPPED, not plotted (sp.js d.cancel), then back to Select
        tool = { t -> kdraft = null; pickTool(t) },
        grid = { grid = !grid; rev++ },
        stepAt = { x, y -> routeS(x, y) },
        step = { k -> goStepS(k) },
    )

    val statusText = when {
        planTab -> {
            val marks = SurveyPrep.SURFACES.sumOf { prep.surface(it).marks.size }
            val items = SurveyPrep.SURFACES.sumOf { s -> prep.surface(s).marks.sumOf { e -> SurveyPrep.measures(s, e, ab(s)[0], ab(s)[1]).size } }
            tx.sp_engineer_s_sheet_x(marks = marks, items = items, v = prep.actual.size.toString())
        }
        mode == StepRoute.LINES -> {
            val left = SurveyPrep.SLOTS.filter { prep.surface(tab).set[it] != true }.map { SurveyPrep.lineName(tx, tab, it) }
            // the names are joined with ", " in both languages, as sp.js joins them
            if (left.isNotEmpty()) tx.sp_place_x(left.joinToString(", ")) else tx.sp_all_four_lines_placed
        }
        // "surface" (the floor, the ceiling) or "wall" is an English word chosen in code: one whole key each
        else -> if (SurveyPrep.isPlan(tab)) tx.sp_squared_laser_add_surface else tx.sp_squared_laser_add_wall
    }
    val openShape = face && mode == StepRoute.LINES && frameOf(tab) == null
    val boundedText = tx.sp_x_6_surfaces_bounded(done.toString())

    /**
     * ← LEAVES, AND IS THE ONLY THING THAT DOES (port 63 slice I1b, P2): what was typed is written first, and what ← has
     * always done is done -- onCancel, whose keep files a NEW capture no ✓ Save has filed and never one that is
     * (SaveStay.keep, in the caller). Nothing of a save is run again (SaveStay.leave: no repeats); an upload a press
     * held back because the capture was not filed yet, and no answer has queued since, is queued ONCE here, after the
     * keep has been asked -- and SaveStay.left forgets it, so that press's answer, arriving after this screen has
     * closed, queues no second one.
     */
    fun back() {
        commitPad(); menuOpen = false
        val l = SaveStay.leave(saveMem, dirty)
        saveMem = SaveStay.left(saveMem)
        onCancel(prep.copy(), roomSize.copyOf())
        if (SaveStay.Effect.UPLOAD in l.then) AsIsUpload.enqueue(context, id)
    }
    /**
     * ✓ SAVE SAVES AND STAYS (port 63 slice I1b, P2; Amit, 23 Nov 2026: "Click on save always take me out to room foto.
     * its annoying."): only with all six surfaces bounded (SaveStay.may), and every effect of a press exactly once, in
     * SaveStay.effects' order -- the read-back save; the as-is upload when it read back (a save that did not read back
     * queues nothing, so the web never gets a prep this phone does not hold); the room size and the room folder's
     * survey pictures through onSaved, whose answer the bar then says. Survey prep STAYS OPEN on the same face, step and
     * zoom, nothing picked or gold changed; only ← leaves.
     * ON A NEW CAPTURE THE FIRST PRESS FILES IT, AND STAYS (port 63 P2N; SaveStay.flow -- asked at every press, because it
     * changes under this open screen, once): the press is handed to the caller's keep, which files the capture exactly
     * once however many presses are made meanwhile; no upload is queued at such a press (there is no row to upload
     * from) -- SaveStay.answered queues it at the answer that says the capture is filed, and from then on this is a kept
     * capture's screen and every press is a kept capture's.
     */
    fun saveNow() {
        menuOpen = false
        if (!SaveStay.may(done)) return
        commitPad()
        val ok = store.save(id, prep, roomSize[0], roomSize[1], roomSize[2])
        if (ok) unsaved[0] = false
        val fx = SaveStay.effects(done, ok, SaveStay.flow(saveMem))
        saveMem = SaveStay.saved(saveMem, dirty, ok)
        val seq = saveMem.seq
        if (SaveStay.Effect.UPLOAD in fx) { AsIsUpload.enqueue(context, id); webState = AsIsUpload.status(context, id) }
        if (SaveStay.toKeep(fx))
            onSaved(prep.copy(), roomSize.copyOf()) { n, why ->
                val a = SaveStay.answered(saveMem, seq, n, why)
                saveMem = a.mem
                if (SaveStay.Effect.UPLOAD in a.then) { AsIsUpload.enqueue(context, id); webState = AsIsUpload.status(context, id) }
            }
        rev++
    }
    // The ⋮ overlay's tap: closes the menu; on ← or ✓ Save it then does that too
    val outsideTapS by rememberUpdatedState<(Offset) -> Unit>(newValue = { off ->
        val pt = off + overlayAt
        menuOpen = false
        if (backBox.contains(pt)) back()
        else if (saveBox.contains(pt)) saveNow()
    })

    // ---- the bar: ← · title (and the save state under it) · the laser chip · ↶ · ⋮ · ✓ Save
    val bar: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().height(if (land) 32.dp else 40.dp), verticalAlignment = Alignment.CenterVertically) {
            SpText("←", SP_FG, 20, Modifier.onGloballyPositioned { backBox = it.boundsInRoot() } // text:value glyph
                .clip(CircleShape).clickable { back() }
                .padding(horizontal = 8.dp, vertical = 2.dp))
            Column(Modifier.weight(1f)) {
                Text(tx.sp_x_survey_prep(title), color = SP_FG, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                // the save's own words after ✓ Save, until the next change; the autosave's line otherwise (SaveStay.bar)
                Text(SaveStay.bar(tx, saveMem, dirty, savedMsg), color = SP_OK, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // THE LASER CHIP (port 63 slice I1; DESIGN-imagemeter §4.2): grey = off, pulsing = connecting, gold = ready;
            // only where readings are keyed. A tap connects and speaks; it NEVER fires the meter. Out of the thumb's
            // reach while measuring, and beside nothing he presses often. Every rule is LaserChip's.
            if (LaserChip.shown(quoteStage)) SpLaserChip(link(), land) {
                chipDo(LaserChip.tap(laserMem, link(), blePermitted(), System.currentTimeMillis())) }
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
        val rowH = StepRoute.tabRowDp(land).dp
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
                        // WHAT IS STILL TO MEASURE ON THE FACE (port 63 slice M2; DESIGN-imagemeter §9): a small amber count
                        // after its name, in every step -- the ONE visible count; none when nothing is left, nothing is
                        // asked, or after Survey (MeasureFlow.tabLeft). The ✓ keeps its one meaning: four lines set.
                        val left = if (StepRoute.isFace(t)) ab(t).let { z -> MeasureFlow.tabLeft(prep, t, quoteStage, z[0], z[1]) } else 0
                        // the guided tour's `tab:<tab>` anchor (port 61c §31), in the dialog's own window
                        SpTab(StepRoute.tabName(tx, t), tab == t, tick,
                            Modifier.weight(1f).fillMaxHeight().tourAnchor("tab:$t", TourState.HOST_SURVEY),
                            ref = refT, land = land, left = left) { showTab(t) }
                    }
                }
            }
        }
    }

    // ---- THE CHILD: the open face's steps, "Front wall ·" then numbered 20 dp circles joined by thin lines. No step
    // has a tap of its own: every press on the strip (and under it, see routeToStep) goes through StepRoute.stepFor.
    val stripUi: @Composable () -> Unit = {
        val ci = StepRoute.routeIndex(mode)
        Row(Modifier.fillMaxWidth().height(StepRoute.stripDp(land).dp)
            .onGloballyPositioned { stripAt = it.positionInRoot() }
            .pointerInput(Unit) { routeToStep({ stripAt }, { x, y -> routeS(x, y) }, { k -> goStepS(k) }) }
            .padding(start = 2.dp, end = 2.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(StepRoute.stripFace(tx, tab), color = SP_MUTED, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, softWrap = false,
                modifier = Modifier.padding(end = 6.dp))
            // THE THREE STEPS (port 63 slice I1): 1 · Lines, 2 · Mark, 3 · 360 -- those the phase shows, each keeping its
            // own number (PhaseTools.routeSteps; port 62 slice L4)
            PhaseTools.routeSteps(quoteStage).forEachIndexed { n, k ->
                val i = StepRoute.routeIndex(k)
                if (n > 0) Box(Modifier.weight(1f).height(1.5.dp).background(SP_JOIN))
                val on = i == ci
                val past = ci >= 0 && i < ci
                val lab = StepRoute.routeLabel(tx, i)
                Row(Modifier.height(if (land) 22.dp else 26.dp)
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
                    if (on) Text(StepRoute.routeName(tx, k), color = SP_GOLD, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, softWrap = false)
                }
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
            Text(if (mode == StepRoute.LINES) tx.sp_x_set_four_lines_2(faceName)
                 else if (mode == StepRoute.PANO) Pano360.caption(tx, tab)
                 else "$faceName · ${tags.first} ${ab0[0].roundToInt()} × ${tags.second} ${ab0[1].roundToInt()}",
                color = SP_FG, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 6.dp, top = 3.dp, bottom = 3.dp))
        }
    }

    // THE NOTE AND THE SNACKBAR over the bottom of the photo, the note above the snackbar: neither
    // costs any height (v52 slice 8 keeps slice 7's dp budget). UNDO undoes the change the snackbar names.
    // THE BAND IN LANDSCAPE lies over the foot of the left column -- the photo or the drawing -- so the
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

    // ---- THE PHOTO, with all the height the controls leave (1 · Lines and 2 · Mark; 3 · 360 is the 360 itself)
    val photoPane: @Composable ColumnScope.() -> Unit = {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            // 1 · Lines: the wall's cut, drawn at its offset, fitted to the surface its lines enclose and 12° beyond its
            // corners (sp.js fitRectFor); 2 · Mark: the squared face, fitted whole
            val cut = if (mode == StepRoute.LINES) views[tab] else null
            val img: Bitmap? = if (mode == StepRoute.LINES) cut?.bmp else rectOf(tab)?.bmp
            val fitTo: DoubleArray? = cut?.let { c -> SurveyPrep.fitRectFor(tab, prep.surface(tab).lines, frameOf(tab)?.q, c.ext) }
            if (mode == StepRoute.PANO) {
                // 3 · 360 (v52 slice 17): the same marks on the 360 itself, edited through the same functions as 2 · Mark.
                // Nothing is ever gold there (Pick.goldOk), so it is handed no reading being taken.
                Sp360(Modifier.fillMaxSize(), previewPano, p3Look, tab, rev, prep, roomSize, selNow(), pick.gold, tool,
                    mdrag, area, areaS, kdraft, SurveyGuide.now(mdrag, endG, areaS, area, tool, kdraft), grid, p3Actions,
                    stage = quoteStage, asPicked = asPicked, onAs = { cycleAs() })
            } else if (mode != StepRoute.LINES && frameOf(tab) == null) {
                // A FACE WHOSE FOUR LINES DO NOT CLOSE CANNOT BE SQUARED: it says so, where a picture that never comes kept
                // a spinner turning. (2 · Mark falls to 1 · Lines on such a face -- PhaseTools.routeMode -- so this is
                // only ever a frame's worth, while the fall is made.)
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                    Text(tx.sp_x_set_four_lines(SurveyPrep.NAME[tab]?.let { Masters.surface(tx, it) } ?: tab), color = SP_MUTED,
                        fontSize = 12.sp, lineHeight = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                }
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
                    sel = selNow(), pick = pick, scene = scene(),
                    // port 63 slice I1: what a TAP on the photo with nothing to place does is Pick.tap's -- a figure turns
                    // gold (the gold one opens the keypad), an item's body is picked and held, a handle of the held item
                    // becomes the part its nudges move, empty photo lets the pick go or leaves the gold alone
                    onTap = { hit, line -> took(Pick.tap(scene(), pick, hit, line)) },
                    // a drag began on a part of the held item: that part is the one the row's nudges move (Pick.handle)
                    onHandle = { pt -> took(Pick.handle(pick, pt)) },
                    // the keypad's uncommitted digits for the gold plate; the hollow glyph while the laser is not ready;
                    // the plate that has just taken a reading; and the bump that pans to a gold figure off screen
                    live = MeasurePad.live(padNow()),
                    laserReady = LaserChip.ready(link()), ring = ring, walked = walked,
                    onLineGrab = { g -> lineSel = g; rev++ },
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
                    onLongPress = { mid, at -> longPressed(mid, at) },
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
                    // v52 slice 11: the chain being drawn, its taps, a second finger, its end (a double tap or ✓), a pick
                    chain = kdraft,
                    onChainPress = { mm, tol -> chainPress(mm, tol) },
                    onChainMove = { i, mm, tol -> chainMove(i, mm, tol) },
                    onChainCancel = { i -> chainCancel(i) },
                    onChainFinish = { kdraft?.let { finishChain(it) } },
                    guide = SurveyGuide.now(mdrag, endG, areaS, area, tool, kdraft),
                    grid = grid,
                    onGrid = { grid = !grid; rev++ },
                    onEndGuide = { g -> endG = g; rev++ },
                    onSay = { t -> say(t) },
                    // a finger on the photo: the next touch takes a note that stayed; and when it lifts, a reading that
                    // landed under it moves the gold on (released, through the effect on sheetOrFinger below)
                    onBusy = { b -> photoBusy = b; if (b) touched() },
                    // port 62 slice L4: the phase -- which marks may be changed, and the chip's "as …"
                    stage = quoteStage, asPicked = asPicked, onAs = { cycleAs() })
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
    // leave -- nothing scrolls -- and the line under it naming the drawing. No step strip, no caption, and
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

    // ---- under the photo in 2 · Mark and 3 · 360 (port 63 slice I1; DESIGN-imagemeter §2.1, §2.3, §14 I1). NOTHING HERE
    // SHOWS A MEASUREMENT -- no bar, no figure box, no number, no word about a dimension -- NOTHING HERE DELETES, and
    // nothing here fires the laser. What stands here is StepRoute.underMark's answer: the KEYPAD while it is open, in the
    // tool row's place and the row's; else -- for this one build, slice I2 deletes it -- THE PICKED ITEM'S ROW, held or
    // not, reduced to its name and what moves it (Pick.row: its label, which opens its Mark sheet, its nudges while it is
    // held, its padlock), over the tool row; else the tool row alone; and where the phase has no tool row, that row alone.
    val markPanel: @Composable () -> Unit = {
        rev    // read: the held item, its lock and the gold follow every change
        val toolRow = PhaseTools.can(PhaseTools.Tool.PALETTE, quoteStage)
        val sl = selNow()?.takeIf { marking && it.s == tab && it.id != null }
        val row = if (sl != null) Pick.row(scene(), pick) else null
        val under = StepRoute.underMark(toolRow, row = row != null, keypad = padOpen && pick.gold != null)
        if (under == StepRoute.Under.KEYPAD) {
            // THE KEYPAD (DESIGN-imagemeter §5): no display line -- the digits are on the gold plate. Next ▸ stores and
            // moves the gold on as a reading does; ⌄ stores and hides it.
            SpMeasurePad(land, onKey = { k -> padPress(k) }, onNext = { nextFigure() }, onHide = { commitPad(); padOpen = false; rev++ })
        } else {
            // the PICKED item's row, held or not (the review of I1, findings 1 and 6): its label is the way to its Mark
            // sheet at every stage -- over the tool row, or alone where the phase has none (Under.ROW) -- and a Line's,
            // a Tape's or a chain's code is named here and nowhere else (P4)
            if (row != null && sl != null && (under == StepRoute.Under.HELD || under == StepRoute.Under.ROW)) {
                val sm = prep.surface(tab).marks.firstOrNull { it.id == sl.id }
                val sq = prep.surface(tab).meas.firstOrNull { it.id == sl.id }
                val sk = prep.surface(tab).chains.firstOrNull { it.id == sl.id }
                Row(Modifier.fillMaxWidth().height(StepRoute.heldRowDp(land).dp).padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (sm != null) {
                        val p = sl.part
                        val edge = p != "body"
                        val horiz = edge && (p.contains('t') || p.contains('b')) && !(p.contains('l') || p.contains('r'))
                        val vert = edge && (p.contains('l') || p.contains('r')) && !(p.contains('t') || p.contains('b'))
                        val what = when (p) {
                            "body" -> tx.sp_move
                            "l" -> tx.edge_left_edge; "r" -> tx.edge_right_edge; "t" -> tx.edge_top_edge; "b" -> tx.edge_bottom_edge
                            else -> if (p.length > 1) tx.sp_corner else p
                        }
                        val lk = SurveyPrep.isLocked(sm)
                        // ITS STATE (port 62 slice L2; the prototype's nlAttrs, index61c2.html 2930): a 3 dp bar on the
                        // label's left edge in the state's colour -- none for an Existing mark, whose label is v52's
                        val lifeBar = MarkLife.barColour(sm)
                        // ... and A TAP ON IT OPENS THE MARK SHEET (slice L3; nlAttrs' role=button, "Open the Mark sheet for FW1")
                        Text("${sm.code}\n${if (lk) tx.sp_locked else if (row.nudges) what else ""}", color = SP_GOLD, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.width(52.dp).clip(RoundedCornerShape(3.dp)).drawBehind {
                                if (lifeBar != null) drawRect(Color(SurveyPrepDraw.argb(lifeBar)), size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height))
                            }.clickable(enabled = row.sheet, role = Role.Button) { openSheet(sm.id) }
                                .semantics { contentDescription = tx.life_aria_open_mark_sheet_x(sm.code) })
                        val bs: List<Triple<String, Double, Double>> = when {
                            horiz -> listOf(Triple("▼5", 0.0, -5.0), Triple("▼1", 0.0, -1.0), Triple("▲1", 0.0, 1.0), Triple("▲5", 0.0, 5.0)) // text:value glyphs and mm
                            vert -> listOf(Triple("◀5", -5.0, 0.0), Triple("◀1", -1.0, 0.0), Triple("1▶", 1.0, 0.0), Triple("5▶", 5.0, 0.0)) // text:value glyphs and mm
                            else -> listOf(Triple("◀", -1.0, 0.0), Triple("▶", 1.0, 0.0), Triple("▼", 0.0, -1.0), Triple("▲", 0.0, 1.0)) // text:value glyphs
                        }
                        // THE NUDGES MOVE THE HELD MARK, a millimetre at a time and never snapping; a mark that is not
                        // held has none (Pick.row). NO DELETE: the trash and the menu delete for this build.
                        if (row.nudges) for ((lab, dx, dy) in bs) SpButton(lab, Modifier.weight(1f), SP_GOLD, compact = land) { nudgeMark(dx, dy) }
                        else Spacer(Modifier.weight(1f))
                        if (row.padlock) SpPadlock(lk, if (lk) tx.sp_aria_unlock_x(sm.code) else tx.sp_aria_lock_x(sm.code)) { toggleLock(sm.id) }
                    } else {
                        // A PICKED TAPE, LINE, AREA OR CHAIN, held or not: its code -- a Line's and an area's opens its Mark sheet, for its
                        // trade and its note (slice L3); a Tape and a chain have none -- and its padlock. NO VALUE: its
                        // figure is on the photo and nowhere else.
                        // its NAME is its code (P4: an item's code is shown only where it is named, never on its line)
                        val code = PhotoFigures.nameOf(prep, pick) ?: sq?.code ?: sk?.code
                        val oid = sq?.id ?: sk?.id
                        val lk = sq?.let { SurveyPrep.isLocked(it) } ?: sk?.let { SurveyPrep.isLocked(it) } ?: false
                        val ink = if (sk != null) SP_CHAIN
                                  else Color(SurveyPrepDraw.argb(if (sq?.kind == SurveyMeas.TAPE) SurveyMeas.TAPE_COLOUR else SurveyMeas.LINE_COLOUR))
                        if (code != null && oid != null) {
                            Text(code, color = ink, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp, maxLines = 1,
                                modifier = if (!row.sheet) Modifier else Modifier.clip(RoundedCornerShape(3.dp))
                                    .clickable(role = Role.Button) { openSheet(oid) }
                                    .semantics { contentDescription = tx.life_aria_open_mark_sheet_x(code) })
                            Spacer(Modifier.weight(1f))
                            if (row.padlock) SpPadlock(lk, if (lk) tx.sp_aria_unlock_x(code) else tx.sp_aria_lock_x(code)) { toggleLock(oid) }
                        }
                    }
                }
            }
            // THE TOOL ROW (v52 slice 7; port 63 slice I1): Tape, Line, three types, More ▸ and `▸` LAST -- no Select slot:
            // with no tool on, a touch selects. Where the phase has one (port 62 slice L4): at Estimated and at Client
            // approved nothing new is marked, and the band over the screen says so.
            if (toolRow) SpPalette(tab, tool, land,
                next = SurveyPrepPalette.nextSlot(tx, prep, tab, pick.gold, quoteStage, mode) { s -> ab(s) },
                onTool = { t -> pickTool(t) }, onMore = { moreOpen = !moreOpen }, onNext = { nextFigure() })
        }
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
        // A NEW CAPTURE'S NOTE AND ITS DISCARD, ONLY WHILE IT IS NOT FILED (port 63 P2N; SaveStay.mayDiscard): once its first
        // ✓ Save has filed it -- or is filing it -- it is a kept capture, whose 360 file its row points at
        if (onDiscard != null && SaveStay.mayDiscard(saveMem)) {
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
                        hist.clear(); lineSel = null; toolSet = SurveyPrepPalette.SEL; moreOpen = false
                        ctx = null; armedClear = null; menuOpen = false; hideSnack(); dropDrafts()
                        // ... and every mark past Existing, locked or not (port 62 slice L2): the note names those first
                        prep = r.prep; settled(); SurveyPrepEdit.startedOver(tx, r)?.let { say(it) }; changed()
                    }
                }.padding(if (land) 6.dp else 9.dp), contentAlignment = Alignment.Center) {
                Text(if (armed) SurveyPrepEdit.startOverArmed(tx) else SurveyPrepEdit.startOverLabel(tx),
                    color = if (armed) SP_AMB else SP_MUTED, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center)
            }
        }
        if (onDiscard != null && SaveStay.mayDiscard(saveMem)) {
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

    // A READING THAT LANDED UNDER A SHEET OR A FINGER MOVES THE GOLD ON AT THE RELEASE (Pick row 20; Landing.released):
    // asked whenever the last of them goes -- the Mark sheet, the More sheet, the menu, the finger on the photo
    val held0 = sheetOrFinger()
    LaunchedEffect(held0) { if (!held0) released() }

    // The phone's Back is the screen's own ←: nothing is lost, the work is already saved.
    // ... and with the size sheet open, Back closes the sheet only; with ⋮ open, the menu only.
    Dialog(onDismissRequest = {
            if (ctx != null) ctx = null
            else if (lwhOpen) lwhOpen = false
            else if (menuOpen) menuOpen = false
            else if (moreOpen) moreOpen = false
            else back()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize(), color = SP_BG) {
          Box(Modifier.fillMaxSize().onGloballyPositioned { val at = it.positionInRoot(); rootTop = at.y; rootLeft = at.x }
              .onSizeChanged { rootH = it.height; rootW = it.width }) {
            rev    // read, so every model change recomposes the screen
            if (!land) {
                // PORTRAIT, 360 x 780: bar 40 · tabs 64 + strip 28 = 92 · caption · THE PHOTO · what works on it
                // ... and since port 62 slice L4 the phase band over them all, 20 dp: the screen's first line
                Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp)) {
                    SpPhaseBand(band, false)
                    bar()
                    openedLine()
                    tabsUi()
                    if (face) stripUi()
                    if (planTab) { planPane(); planFoot() }
                    else {
                        caption()
                        photoPane()
                        if (mode == StepRoute.LINES) linesPanel() else markPanel()
                    }
                }
            } else {
                // LANDSCAPE, 780 x 360 (a ~336 dp window under the status bar): the photo on the left at full height;
                // on the right, 318 dp of controls -- bar 32 · tabs 56 + strip 24 = 80 · gap · what works on it (in
                // 2 · Mark the tool row, the held item's row over it, or the keypad: StepRoute.landFits holds that each fits).
                // The salvaged-prep line sits on the left in landscape too, where it shortens the photo rather than
                // clipping the panel under the controls.
                Row(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        openedLine()
                        if (planTab) {
                            planPane()
                        } else {
                            caption()
                            photoPane()
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.width(318.dp).fillMaxHeight()) {
                        bar()
                        tabsUi()
                        if (face) stripUi()
                        // the empty gap under the controls takes part in the strip's touch area (sp.js .spgap)
                        Box(Modifier.weight(1f).fillMaxWidth().onGloballyPositioned { gapAt = it.positionInRoot() }
                            .pointerInput(Unit) { routeToStep({ gapAt }, { x, y -> routeS(x, y) }, { k -> goStepS(k) }) })
                        // the Plan tab: the line naming the drawing, at the foot of the controls (sp.html #planfoot in .spfoot)
                        if (planTab) planFoot()
                        else if (mode == StepRoute.LINES) linesPanel() else markPanel()
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
            // ---- THE LONG-PRESS MENU (v52 slice 8): Duplicate · Lock/Unlock · Lock all <type> · Cancel, and Delete LAST
            // and apart (port 63 slice F1: it was the first row, right under the finger -- SurveyPrepEdit.menuItems), its
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
                val mh = if (ctxSize.height > 0) ctxSize.height.toFloat() else with(dens) { (232 + DeleteGuard.CLEAR_DP).dp.toPx() }
                val left = (c.x - rootLeft - with(dens) { 24.dp.toPx() }).coerceAtMost(rootW - mw - edge).coerceAtLeast(edge)
                // ... less the rows the phase hides (port 62 slice L4): on a surveyed mark after Survey, its head and Cancel;
                // and no Duplicate on a mark where its copy would not be what a tap places (the L4 review; copyOk)
                val rows = SurveyPrepEdit.menuItemsOf(tx, prep, c.id, quoteStage, asPicked)
                // 12 dp above the press, kept on the screen -- and NEVER WITH ITS DELETE ROW UNDER THE FINGER (port 63
                // slice F1; DeleteGuard.menuTop): low on the screen the menu is pushed up, and what lands under the
                // finger is its last row, which is Delete now; there it opens above the press instead
                val top = DeleteGuard.menuTop((c.y - rootTop).toDouble(), mh.toDouble(), rootH.toDouble(), dens.density.toDouble(),
                    rows.any { it.first == SurveyPrepEdit.DELETE }).toFloat()
                SpCtxMenu(head, rows,
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
                     /** Port 63 slice I1b (DESIGN-imagemeter §3, §8.1): the HELD item's body -- it moves whole only once the
                      *  finger is Pick.BODY_SLOP_DP from where it went down (it moved from the first pixel), so a press
                      *  that lifts as a tap never nudged the item it tapped. */
                     val slop: Boolean = false,
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
 * a line swings. In 2 · Mark a tap with a type on places one where the finger
 * is, and the tool's chip sits top-left (v52 slice 7).
 * ONE STEP TO MARK AND MEASURE (port 63 slice I1b; DESIGN-imagemeter §3): with no
 * tool on, A PRESS PICKS NOTHING -- it acts when it lifts as a tap, and what the
 * tap does is Pick.tap's (onTap): a figure turns gold, an item's body is picked
 * and held, a handle of the held item becomes the part its nudges move, empty
 * photo lets the pick go or leaves the gold alone. ONLY THE HELD ITEM MOVES
 * (Pick.drag): a drag on anything else pans, so a thumb resting on the photo while
 * the other hand holds the laser cannot drag a mark the walk has reached.
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
    /** What is selected as 1 · Lines takes it: a line's slot, or the reference rectangle; on 2 · Mark the picked item. */
    sel: SpSel?,
    /** Port 63 slice I1b: what is picked, whether it is HELD, and which figure is gold (Pick.State) -- and where Pick's
     *  events happen (the room, the phase, the step), so the gesture asks Pick.drag which press may move anything. */
    pick: Pick.State,
    scene: Pick.Scene,
    /** A TAP ON 2 · MARK WITH NO TOOL ON: what SurveyPrepTouch.hit found under it, and -- when it found nothing -- the
     *  Tape, Line, area or chain whose line the finger is on (12 dp). The dialog asks Pick.tap and does what it says. */
    onTap: (SurveyPrepTouch.Hit?, String?) -> Unit,
    /** A drag began on a handle of the held item -- a tab, a corner, a chain's point: that part is the one its row's
     *  nudges move (Pick.handle). */
    onHandle: (String) -> Unit,
    /** 1 · Lines: a press took a line, its end or the reference rectangle -- the lines' own selection. */
    onLineGrab: (SpSel) -> Unit,
    /** The keypad's uncommitted digits (MeasurePad.live), drawn on the gold plate; null: none. */
    live: String?,
    /** The laser chip is gold: the gold plate's `▶` is solid; while it is not, hollow (LaserChip.ready). */
    laserReady: Boolean,
    /** The reading key whose plate has just taken a reading: it rings gold for Landing.RING_MS. */
    ring: String?,
    /** Bumped whenever the gold moved BY ITSELF (a reading, `▸`): the photo pans if the new gold figure is off screen. */
    walked: Int,
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
    val prepS by rememberUpdatedState(prep)
    val rectS by rememberUpdatedState(rect); val abS by rememberUpdatedState(ab)
    val pickS by rememberUpdatedState(pick); val sceneS by rememberUpdatedState(scene)
    val onTapS by rememberUpdatedState(onTap); val onHandleS by rememberUpdatedState(onHandle)
    val onLineGrabS by rememberUpdatedState(onLineGrab)
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
    val onEndGuideS by rememberUpdatedState(onEndGuide); val onSayS by rememberUpdatedState(onSay)
    val onChainPressS by rememberUpdatedState(onChainPress); val onChainMoveS by rememberUpdatedState(onChainMove)
    val onChainCancelS by rememberUpdatedState(onChainCancel); val onChainFinishS by rememberUpdatedState(onChainFinish)
    val chainS by rememberUpdatedState(chain)
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
            is SurveyRef.LinesHit.Inside -> { onLineGrabS(SpSel(tab, id = rid)); picked(rid); null }
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
    /** The open face's marks as drawn on the canvas, for the hit test (SurveyPrepTouch.Drawn); none without a squared face. */
    fun drawnMarks(): List<SurveyPrepTouch.Drawn> {
        val r = rectS ?: return emptyList(); val sq = r.sq
        // a point (v52) is a spot: its 25 mm on screen goes with it, so it is grabbed round its centre and never resized
        // ... and so is a Note's pin (port 62 slice L6; sp.js 5791 and 5809: `ISPOINT(e.t)||L53.isNote(e)`)
        return prepS.surface(tab).marks.map { e ->
            val p = toS(sq.x(e.x0), sq.y(e.y1)); val q = toS(sq.x(e.x1), sq.y(e.y0))
            SurveyPrepTouch.Drawn(e.id, p[0].toDouble(), q[0].toDouble(), p[1].toDouble(), q[1].toDouble(),
                spot = if (SurveyPrep.isSpot(e.t)) SurveyPrep.SPOT / 2 / sq.mpp * k() else null)
        }
    }

    /**
     * A TAPE, A LINE AND AN AREA MOVE BY THEIR BODY (port 63 slice F1; Amit, 16 Nov 2026: "tape mark does not move unless
     * its end points are moved. difficult for moving it from one point to other."). A press on the PICKED one's body --
     * within 12 dp of its line, of a tape's band or of an area's outline, and outside its ends' 14 dp -- grabs it whole:
     * a drag moves every point by the same delta, stopped at the face's edge so its shape never changes
     * (SurveyGuide.bodyMove), one ↶ step, once the finger has left the dead zone (SpGrab.gate), so the tap that picks it
     * never nudges it -- since port 63 slice I1b only the HELD one's (Pick.handleId), and only once the finger is
     * Pick.BODY_SLOP_DP from where it went down. As a held mark's body: no tool on, in 2 · Mark; a LOCKED one does not move,
     * and a drag says "F-T1 is locked — unlock to move it"; one the phase holds read-only says the phase's line; a long
     * still press opens its menu. Its reading is not touched. WHETHER the press is on the body at all is
     * SurveyGuide.bodyPress, tested in :pano. Null: it is not -- the press goes on down the hit order.
     */
    fun bodyGrab(sx: Float, sy: Float): SpGrab? {
        val r = rectS ?: return null; val sq = r.sq
        val sid = Pick.handleId(pickS) ?: return null
        val m = prepS.surface(tab).meas.firstOrNull { it.id == sid } ?: return null
        val scr = m.pts.map { p -> toS(sq.x(p[0]), sq.y(p[1])).let { q -> doubleArrayOf(q[0].toDouble(), q[1].toDouble()) } }
        val dpx = density.density.toDouble()
        val bp = SurveyGuide.bodyPress(mode == "mark", toolS, m, !areaS.isNullOrEmpty(), scr, sx.toDouble(), sy.toDouble(),
            SurveyGuide.END_HIT_DP * dpx, SurveyMeas.PICK_DP * dpx,
            if (m.kind == SurveyMeas.TAPE) SurveyMeas.bandHalfPx(k() / sq.mpp, dpx) else 0.0) ?: return null
        val at = fromS(sx, sy); val pick = SpSel(tab, id = m.id)
        if (bp is SurveyGuide.BodyPress.Locked) return SpGrab(at[0], at[1], pick, null, bp.code, endLock = true, gate = true) { _, _ -> }
        // at Estimation a Tape, a Line and an area are read-only (PhaseTools.editable): picked, nothing moves, a drag says why
        if (!PhaseTools.editable(m, stageS)) return SpGrab(at[0], at[1], pick, null, PhaseTools.refuseText(txS, m, stageS), gate = true, said = true) { _, _ -> }
        val start = m.pts.map { it.copyOf() }; val mx0 = sq.mmX(at[0]); val my0 = sq.mmY(at[1])
        return SpGrab(at[0], at[1], pick, null, slop = true) { jx, jy ->
            val ab = abS
            SurveyGuide.bodyMove(m, start, sq.mmX(jx) - mx0, sq.mmY(jy) - my0, ab[0], ab[1])
        }
    }

    /**
     * WHAT IS UNDER A PRESS ON 2 · MARK WITH NO TOOL ON (port 63 slice I1b; DESIGN-imagemeter §3, "The hit order"): the
     * HELD item's handles first -- its tabs, a held chain's points -- then a figure, then the held Tape's, Line's or
     * area's body, then the marks topmost first. Only the held item's handles take a press (Pick.handleId): a mark the
     * walk reached is pressed as any other mark is, by its body. The order is SurveyPrepTouch.hit, tested in :pano.
     * Nothing here picks or moves: the press is the tap's when it lifts (onTap), and only Pick.drag lets a drag move.
     */
    fun markHit(sx: Float, sy: Float): SurveyPrepTouch.Hit? {
        if (rectS == null) return null
        val els = prepS.surface(tab).marks
        val tol = with(density) { 16.dp.toPx() }.toDouble()
        val hid = Pick.handleId(pickS)
        val selId = hid?.takeIf { sid -> els.any { it.id == sid } }
        val nodes = hid?.let { sid -> chainsOnScreen().filter { it.first == sid }.map { (cid, pts) -> SurveyPrepTouch.Nodes(cid, pts) } } ?: emptyList()
        return SurveyPrepTouch.hit(drawnMarks(), selId, tags.toList(), sx.toDouble(), sy.toDouble(), tol, density.density.toDouble(),
            nodes, SurveyChain.NODE_HIT_DP * density.density, body = bodyGrab(sx, sy) != null)
    }

    /** The item a press under [h] is on ([h] what [markHit] found; [line] the line within 12 dp when it found nothing):
     *  the long press's item, and the one Pick.drag is asked about. A figure's is its owner's. */
    fun pressId(h: SurveyPrepTouch.Hit?, line: String?): String? = when (h) {
        is SurveyPrepTouch.Hit.Mark -> h.id
        is SurveyPrepTouch.Hit.Node -> h.id
        is SurveyPrepTouch.Hit.Body -> pickS.picked
        is SurveyPrepTouch.Hit.Tag -> prepS.ownerOf(h.code)?.id
        null -> line
    }

    /**
     * THE GRAB A PRESS UNDER [h] MAKES -- ONLY THE HELD ITEM MOVES (Pick.drag; DESIGN-imagemeter §3, §8.1): a held mark by a
     * tab (it resizes, from the first pixel, as built) or by its body (it moves whole after Pick.BODY_SLOP_DP); the held
     * Tape's, Line's or area's body (bodyGrab); a held chain's point (nodeGrab). The PICKED item that is locked, or that
     * the phase holds read-only, makes a grab that moves nothing and says why once the finger has moved. Anything else
     * -- an unpicked mark, a mark the walk reached, a figure, nothing -- makes none: the drag pans.
     */
    fun markGrab(h: SurveyPrepTouch.Hit?, sx: Float, sy: Float): SpGrab? {
        val r = rectS ?: return null; val sq = r.sq; val ab = abS
        return when (h) {
            null, is SurveyPrepTouch.Hit.Tag -> null
            is SurveyPrepTouch.Hit.Body -> bodyGrab(sx, sy)
            is SurveyPrepTouch.Hit.Node -> if (Pick.handles(pickS, h.id)) nodeGrab(h.id, h.i) else null
            is SurveyPrepTouch.Hit.Mark -> {
                val e = prepS.surface(tab).marks.firstOrNull { it.id == h.id } ?: return null
                val at = fromS(sx, sy)
                when (Pick.drag(sceneS, pickS, e.id)) {
                    Pick.Drag.PAN -> null
                    Pick.Drag.LOCKED -> SpGrab(at[0], at[1], SpSel(tab, id = e.id), null, e.code) { _, _ -> }
                    // A MARK OF AN EARLIER PHASE IS READ-ONLY (port 62 slice L4; sp.js moveEl's `!P54.spEdit(e)`): nothing
                    // moves, no undo entry, and once the finger has really moved it says why
                    Pick.Drag.READ_ONLY -> SpGrab(at[0], at[1], SpSel(tab, id = e.id), null, PhaseTools.refuseText(txS, e, stageS), said = true) { _, _ -> }
                    Pick.Drag.MOVE -> {
                        val part = h.part
                        val start = e.copy(); val mx0 = sq.mmX(at[0]); val my0 = sq.mmY(at[1])
                        SpGrab(at[0], at[1], SpSel(tab, id = e.id, part = part), null, slop = part == "body") { jx, jy ->
                            SurveyPrep.moveMark(e, start, part, sq.mmX(jx) - mx0, sq.mmY(jy) - my0, ab[0], ab[1])
                            SurveyPrep.snapMark(e, part, ab[0], ab[1])
                        }
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
        // the HELD item's ends only (port 63 slice I1b, Pick.handleId): a Line the walk reached shows none
        val sid = Pick.handleId(pickS) ?: return null
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

    // THE PHOTO PANS ONLY IF THE GOLD FIGURE IS OFF SCREEN (port 63 slice M2; DESIGN-imagemeter §4.3 item 2): when the
    // gold moves BY ITSELF -- a reading, `▸`, the keypad's Next -- and none of what it measures is in the box (the photo
    // is zoomed elsewhere), the view moves, at its own zoom, to centre it. At fit nothing ever moves; a tap that makes a
    // figure gold never pans (he is looking at it). The rule is MeasureFlow.span / panTo, tested in :pano.
    LaunchedEffect(walked) {
        val code = pick.gold
        val r = rect
        if (walked > 0 && mode == StepRoute.MARK && code != null && r != null && boxW > 1f && boxH > 1f && prep.ownerOf(code)?.s == tab) {
            val at = MeasureFlow.span(prep, code) { _ -> ab }
            if (at != null) MeasureFlow.panTo(view, box(), r.sq.x(at[0]), r.sq.y(at[1]), r.sq.x(at[2]), r.sq.y(at[3]))?.let { v -> view = v; tick++ }
        }
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
                // THE FLOATING TRASH is on top of everything: a press on it deletes the HELD item when it lifts there.
                // Only while the item is held and no tool is on (Pick.trash; DESIGN-imagemeter §14 I1): with a type on, the
                // disc is not there, so a placing tap beside the mark just placed can never land on it and delete that
                // mark; and a mark the walk reached has no delete control anywhere near the laser's thumb
                val tr = if (mode == StepRoute.MARK && Pick.trash(sceneS, pickS, SurveyPrepPalette.armed(toolS))) trashAt[0] else null
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
                    val picked = pickS.picked?.takeIf { pickS.held }
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
                // 2 · MARK WITH NO TOOL ON (port 63 slice I1b): what was under the press -- the hit, and when nothing was hit
                // the Tape, Line, area or chain whose line the finger was on -- for the tap it lifts as (onTap)
                var pressHit: SurveyPrepTouch.Hit? = null
                var pressLine: String? = null
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
                // 2 · MARK WITH NO TOOL ON (port 63 slice I1b; DESIGN-imagemeter §3): A PRESS PICKS NOTHING when it goes
                // down -- a thumb resting on the photo while the other hand holds the laser must change nothing -- and acts
                // when it lifts as a tap (onTap). Only the HELD item's body or handle moves (Pick.drag); a drag on anything
                // else pans, and a pinch zooms.
                val selecting = mode == StepRoute.MARK && !SurveyPrepPalette.armed(tool0)
                // the finger slid the photo: a press that panned is not a slow tap
                var panned = false
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
                // ... and the HELD item's body only once the finger is Pick.BODY_SLOP_DP away (port 63 slice I1b, SpGrab.slop)
                val bodyGate = SurveyGuide.EndGate(max(Pick.BODY_SLOP_DP.dp.toPx().toDouble(), viewConfiguration.touchSlop.toDouble()))
                var secondFinger = false

                fun tapAt(x: Double, y: Double) {
                    if (grab == null && placing) placeGrab(x.toFloat(), y.toFloat(), tool0)
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
                        // the press. 1 · Lines: what is under it is taken now, as sp.js takes it on pointerdown. 2 · Mark with
                        // no tool on (port 63 slice I1b): NOTHING IS PICKED -- what is under it is kept for the tap it lifts as,
                        // and only a press on the HELD item's body or handle grabs it (markGrab, through Pick.drag)
                        is GestureClassifier.Event.Down -> {
                            // FIRST, THE HELD ITEM'S ENDS (v52 slice 10, sp.js hit): with no tool on the press grabs the end;
                            // with Tape or Line on it starts a NEW one there -- it never stretches the old one
                            val eh = endHit(e.x.toFloat(), e.y.toFloat(), tool0)
                            val eg = eh?.grab
                            if (eh != null && eg == null) startAt = eh.mm
                            else if (eg != null) { grab = eg; endGrabbed = true; gated = true; gc.grabbed() }
                            // CHAIN ON: a chain's point under the finger is grabbed (and its chain picked); else nothing yet
                            else if (chainOn) chainNodeAt(e.x.toFloat(), e.y.toFloat())?.let { g ->
                                grab = g; gated = true; gc.grabbed() }
                            else if (mode == StepRoute.LINES) {
                                // 1 · Lines: a press inside the reference picks it (v52 slice 12), so its long press opens Lock · Cancel
                                val h = hitLines(e.x.toFloat(), e.y.toFloat()) { rid -> pickedMeas = rid }
                                // a grab follows the finger from the first pixel (no slop dead zone), as sp.js moves it
                                if (h != null) { grab = h; if (h.gate) gated = true; onLineGrabS(h.sel); gc.grabbed() }
                            } else if (selecting) {
                                val h = markHit(e.x.toFloat(), e.y.toFloat())
                                pressHit = h
                                // nothing hit: a chain within 12 dp of its line (v52 slice 11), else a Tape or a Line within 12 dp
                                if (h == null) pressLine = hitChain(e.x.toFloat(), e.y.toFloat()) ?: hitMeas(e.x.toFloat(), e.y.toFloat())
                                val g = markGrab(h, e.x.toFloat(), e.y.toFloat())
                                if (g != null) {
                                    grab = g; if (g.gate) gated = true; gc.grabbed()
                                    // a drag on a handle of the held item makes that part the one its nudges move (Pick.handle)
                                    val pt = g.sel.part
                                    if (g.locked == null && pt != "body") onHandleS(pt)
                                    else if (h is SurveyPrepTouch.Hit.Node && g.locked == null) onHandleS(Pick.nodePart(h.i))
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
                            if (e.first && grab == null && placing) {
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
                                if (if (g.slop) bodyGate.move(e.dx, e.dy) else !gated || endGate.move(e.dx, e.dy)) {
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
                                // wall's corners can be brought into the box -- and in 3 · Measure every drag does
                                view = view.pan(box(), slideCx, slideCy, e.dx, e.dy)
                                panned = true
                                tick++
                            }
                        }
                        // A TAP: with Select on, the press already picked what was under it; with a type on and
                        // nothing under it, the type goes where the finger went down. A double tap acts as a tap
                        // here: the Line's area and the Chain give it a meaning of its own (slices 9 and 11).
                        // An end that moved and still lifted as a tap is put back, with its undo step (review).
                        is GestureClassifier.Event.Tap -> {
                            if (SurveyGuide.revertOnTap(gated, moved)) { onRevertS(); moved = false; snapped = false }
                            // 2 · Mark with no tool on: the press acts now, as Pick.tap says (port 63 slice I1b)
                            if (selecting) onTapS(pressHit, pressLine)
                            else if (measOn) measTap(e.x, e.y, false) else if (chainOn) chainTap(e.x, e.y) else tapAt(e.x, e.y)
                        }
                        // a double tap closes a Line area of three corners or more (v52 slice 9), and ENDS a chain (slice 11,
                        // sp.js kDown: the second tap of the pair adds no point -- the first already did)
                        is GestureClassifier.Event.DoubleTap -> {
                            if (SurveyGuide.revertOnTap(gated, moved)) { onRevertS(); moved = false; snapped = false }
                            // (the double tap's zoom is slice I4's; until then it is a second tap, as it always was)
                            if (selecting) onTapS(pressHit, pressLine)
                            else if (measOn) measTap(e.x, e.y, true)
                            else if (chainOn) { if (grab == null) onChainFinishS() }
                            else tapAt(e.x, e.y)
                        }
                        // A LONG PRESS WITH A TYPE ON IS A SLOW TAP: sp.js places on the press however long it is held
                        // (its long-press timer is armed for Select only, sp.js 929 / 1177), so the type goes where
                        // the finger went down and a slide after it carries the mark. With Select on it opens the
                        // menu of the mark under it (below).
                        // with Chain on a long still press is a slow tap: a point where the finger went down, at the lift
                        // 2 · Mark with no tool on (port 63 slice I1b): held on an item, its menu (Pick row 3); held on a
                        // figure, a slow tap -- it acts at the lift; on empty photo, nothing
                        is GestureClassifier.Event.LongPress -> if (measOn) { heldTap = true } else if (chainOn) { if (grab == null) heldTap = true }
                        else if (selecting && pressHit is SurveyPrepTouch.Hit.Tag) { heldTap = true } else {
                            val gid = if (selecting) pressId(pressHit, pressLine) else grab?.sel?.id ?: pickedMeas
                            if (grab == null && placing)
                                placeGrab(e.x.toFloat(), e.y.toFloat(), tool0)?.let { grab = it; snapped = true; gc.grabbed() }
                            // THE LONG-PRESS MENU (v52 slice 8, sp.js openCtx): with Select on, held on a mark (locked
                            // or not), the menu opens at the press; on empty photo, nothing. A grab moved further than
                            // GRAB_STILL_DP gets no long press (GestureClassifier); one the drift moved less is put
                            // back first, with its undo step, so the menu never opens on a mark that has moved
                            // ... and on 1 · Lines, held on the reference rectangle (its corner or inside it): Lock · Cancel
                            // under "Front wall reference" (v52 slice 12, ct52 X4) -- a line's long press opens nothing
                            else if (!placing && gid != null && (mode == StepRoute.MARK || SurveyRef.isId(gid))) {
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
                    // a slow tap on a figure: it acts at the lift, unless the finger slid on (port 63 slice I1b)
                    if (selecting && heldTap && !panned && !gone) onTapS(pressHit, pressLine)
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
            rev; tick; view; loupe; sel; pick; live; laserReady; ring; photoAt; fitAt; chipAt; chipText; drag; area; grid; guide; chain
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
                // WHICH FIGURES THE PHOTO DRAWS is PhotoFigures' (port 63 slices I1a-I1c): every READ figure of every mark,
                // always, and the picked mark's unread ones as a quiet `?` -- no photo estimate, anywhere -- and the fit /
                // grid column, which no plate may cover (canvas px). There is no `all`: every mark shows what it has.
                // The item the pick names, on THIS face (the 360 can leave it on another)
                val pickedHere = pick.picked?.takeIf { pid -> prep.itemById(pid)?.s == tab }
                val figKeep = listOfNotNull(fitAt).map { f ->
                    SurveyPrepTouch.Box((f.left - photoAt.x).toDouble(), (f.top - photoAt.y).toDouble(),
                        (f.right - photoAt.x).toDouble(), (f.bottom - photoAt.y).toDouble()) }
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
                            pickedHere, pick.gold, width = size.width, height = size.height,
                            tags = if (which == "main") tags else null,
                            dp = density.density, drag = drag?.takeIf { it.s == tab }, area = area,
                            gridAlpha = gridA, guide = gNow, guideAlpha = ga,
                            // the picked Line's ends are handles, and the picked mark shows its tabs, ONLY WHILE IT IS HELD
                            // (port 63 slice I1b; Pick.handles): a mark the walk reached, a locked one and one the phase
                            // holds read-only show none -- and none with a tool on
                            ends = mode == StepRoute.MARK && !SurveyPrepPalette.armed(tool) && pick.held,
                            chain = chain?.takeIf { it.s == tab }, frame = frame,
                            tabs = mode == StepRoute.MARK && pick.held,
                            live = live, keep = figKeep, pick = pick, ready = laserReady, ring = ring,
                            // after Survey nothing is read here, so the picked item asks nothing: no `?` (the review of I1, finding 5)
                            ask = MeasureFlow.canRead(scene.stage)), mm, u)
                    }
                }
                tags.clear()
                overlay(nc, m, "main")
                // ---- THE FLOATING TRASH (v52): a 36 dp red disc with a white ring beside the picked mark, placed
                // by SurveyPrepTouch.trashSpot so it never covers one of its figures -- a tap on a figure never deletes
                trashAt[0] = null
                // with a type on the disc is not drawn (sp.js 801: "with a drawing tool on, the trash stays in the bar")
                // ... nor where the phase hides the picked item's delete (port 62 slice L4; sp.js fTrashSurf's `!P54.spDelOk`)
                // ... and ONLY WHILE THE ITEM IS HELD (port 63 slice I1b; Pick.trash, for this one build -- I2 deletes it)
                val pickedId = if (mode == StepRoute.MARK && Pick.trash(scene, pick, SurveyPrepPalette.armed(tool))) pickedHere else null
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
                    // DELETE CANNOT BE PRESSED BY ACCIDENT (port 63 slice F1; DeleteGuard): the disc keeps 16 dp clear of the
                    // picked item's own handles -- a box mark's eight tabs (a spot's centre); a Tape's, a Line's, an area's
                    // or a chain's points and its line, a tape's as wide as its band -- and of every figure; where no place
                    // is that clear there is no disc (SurveyPrepTouch.trashSpot). It sat 2 dp off the corner tab until now.
                    val scrPts = pickedPts?.map { pq -> m(rr.sq.x(pq[0]), rr.sq.y(pq[1])).let { q -> doubleArrayOf(q[0].toDouble(), q[1].toDouble()) } }
                    val keep = if (scrPts != null) SurveyPrepTouch.keepOfLine(scrPts, pickedM?.kind == SurveyMeas.AREA,
                            if (pickedM?.kind == SurveyMeas.TAPE) SurveyMeas.bandHalfPx(kk / rr.sq.mpp, dpx.toDouble()) else 0.0)
                        else SurveyPrepTouch.keepOfMark(bb, picked?.let { SurveyPrep.isSpot(it.t) } == true)
                    SurveyPrepTouch.trashSpot(bb, tags.map { it.box }, size.width.toDouble(), size.height.toDouble(), dpx.toDouble(), keep)?.let { ts ->
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

/**
 * THE LASER CHIP (port 63 slice I1b; DESIGN-imagemeter §4.2): 44 dp in the top bar, between the title and ↶ -- out of the
 * thumb's reach while measuring, and beside nothing he presses often. Grey while the D2 is not linked, PULSING while it
 * connects, GOLD when it is ready (LaserChip.look). A tap connects -- asking for the Bluetooth permission if it is not
 * held -- or says the link's state; IT NEVER FIRES THE METER: the meter's own button takes the reading. Every rule of
 * when the link starts and when it speaks is LaserChip in :pano; this only draws it.
 */
@Composable
private fun SpLaserChip(link: LaserChip.Link, land: Boolean, onClick: () -> Unit) {
    val tx = LocalTx.current
    val look = LaserChip.look(link)
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(look) {
        if (look == LaserChip.Look.PULSING) while (true) { pulse.animateTo(0.35f, tween(600)); pulse.animateTo(1f, tween(600)) }
        else pulse.snapTo(1f)
    }
    val ink = if (look == LaserChip.Look.GOLD) SP_GOLD else SP_MUTED
    val shape = RoundedCornerShape(15.dp)
    Box(Modifier.padding(horizontal = 2.dp).width(LaserChip.WIDTH_DP.dp).height(if (land) 26.dp else 30.dp).clip(shape)
        .background(if (look == LaserChip.Look.GOLD) SP_CHIP_GOLD else Color.Transparent)
        .border(1.5.dp, ink.copy(alpha = pulse.value), shape)
        .clickable(role = Role.Button) { onClick() }
        .semantics { contentDescription = LaserChip.label(tx, link) },
        contentAlignment = Alignment.Center) {
        Text(LaserChip.TEXT, color = ink.copy(alpha = pulse.value), fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, maxLines = 1)
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

/** [enabled] false: drawn dim and does nothing (a locked mark's nudges); [dim]: drawn dim, still answers (its Delete).
 *  [ink]: its word's colour, white by default. */
@Composable
private fun SpButton(text: String, modifier: Modifier, border: Color, compact: Boolean = false, enabled: Boolean = true,
                     dim: Boolean = false, ink: Color = Color.White, onClick: () -> Unit) {
    val faint = !enabled || dim
    Box(modifier.clip(RoundedCornerShape(7.dp)).background(Color.Black)
        .border(1.5.dp, if (faint) border.copy(alpha = 0.35f) else border, RoundedCornerShape(7.dp))
        .clickable(enabled = enabled) { onClick() }.padding(vertical = if (compact) 4.dp else 8.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (faint) SP_DIM else ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
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
 * [left] (port 63 slice M2): how many readings are still to take on the face, a small amber count after its name
 * ("RIGHT ✓ 2") -- none at 0; read out as "2 to measure".
 */
@Composable
private fun SpTab(name: String, on: Boolean, tick: Boolean, modifier: Modifier, ref: String? = null, land: Boolean = false,
                  left: Int = 0, onClick: () -> Unit) {
    val tx = LocalTx.current
    Box(modifier.clickable(role = Role.Tab) { onClick() }
        .semantics { selected = on; if (left > 0) stateDescription = tx.sp_measure_x_to_measure(left.toString()) },
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = if (ref != null && !land) 2.dp else 0.dp)) {
            Text(buildAnnotatedString {
                    append(name.uppercase())
                    if (tick) withStyle(SpanStyle(color = SP_OK, fontWeight = FontWeight.Bold)) { append(" ✓") } // text:value glyph
                    if (left > 0) withStyle(SpanStyle(color = SP_AMB, fontWeight = FontWeight.Bold)) { append(" "); append(left.toString()) }
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
 * in root px, read on each press. A press something under it has already taken -- a control's own tap, which
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
/** The laser chip's ground while the D2 is ready. */
private val SP_CHIP_GOLD = Color(0xFF3A3012)
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
/** The chain's rose (v52 slice 11, sp.js KC #fb7185). */
private val SP_CHAIN = Color(0xFFFB7185)
/** The reference rectangle's cyan (v52 slice 12, sp.js RC #22d3ee), its boxes' ring (#22d3ee88). */
private val SP_REF = Color(0xFF22D3EE)
private val SP_REF_RING = Color(0x8822D3EE)
