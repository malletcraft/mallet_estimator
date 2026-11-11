@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.malletcrafts.interiorestimate

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malletcrafts.interiorestimate.pano.Album
import com.malletcrafts.interiorestimate.pano.AlbumPage
import com.malletcrafts.interiorestimate.pano.AlbumWrite
import com.malletcrafts.interiorestimate.pano.AsIsPath
import com.malletcrafts.interiorestimate.pano.CameraNote
import com.malletcrafts.interiorestimate.pano.CaptureGeometry
import com.malletcrafts.interiorestimate.pano.DeleteRoute
import com.malletcrafts.interiorestimate.pano.Deletes
import com.malletcrafts.interiorestimate.pano.FaceNote
import com.malletcrafts.interiorestimate.pano.Handover
import com.malletcrafts.interiorestimate.pano.Panorama
import com.malletcrafts.interiorestimate.pano.Phases
import com.malletcrafts.interiorestimate.pano.QuoteStage
import com.malletcrafts.interiorestimate.pano.QuoteView
import com.malletcrafts.interiorestimate.pano.RenameRoute
import com.malletcrafts.interiorestimate.pano.Renames
import com.malletcrafts.interiorestimate.pano.RoomToken
import com.malletcrafts.interiorestimate.pano.SurveyPrep
import com.malletcrafts.interiorestimate.pano.TourDemo
import com.malletcrafts.interiorestimate.pano.TourFlows
import com.malletcrafts.interiorestimate.pano.TourMachine
import com.malletcrafts.interiorestimate.pano.UploadNote
import com.malletcrafts.interiorestimate.pano.WishList
import com.malletcrafts.interiorestimate.pano.i18n.LangRow
import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Msg
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SyncWorker.schedule(this)
        setContent { MaterialTheme { AppScreen() } }
    }
}

private data class ProjectRow(val name: String, val title: String, val customer: String)

/**
 * The scope sheet's one answer to a submit -- QV's Record the approved scope, QM2's Move to Client approved: whether S5
 * did what was asked ([done]: the sheet closes and the pending refusal goes), whether the sheet redraws its ticks from
 * the scope as stored ([stale], S5's scope 409), and the status line in a language ([text], a Msg's shape: null when
 * there is nothing to say -- done, or nothing to send, which closes the sheet as the prototype's does).
 */
private class SheetAnswer(val done: Boolean, val stale: Boolean, val text: (Tx) -> String?)

/**
 * The language, provided ONCE above every screen (design-marathi §2.4, slice 1b).
 *
 * Here, and not around the drawers alone: [AppScreenBody] returns early into the laser test,
 * Face Prep, the annotator, the sign-in sheets and the search overlay, and a later slice's
 * `LocalTx.current` on any of those would crash at launch outside the provider -- which CI,
 * compiling but never launching, could not see. Dialogs and sheets inherit it as well.
 *
 * The choice is read only under [LangRow.VISIBLE] (the row and the ability to pick मराठी switch
 * together, by that one constant). It is on since Amit's 28 Oct 2026 "Show it now, translate
 * alongside", so this is the stored pref: [Tx.EN] on first run whatever the phone's system language,
 * [Tx.MR] once मराठी is picked. The body's onToggle calls
 * `onLang` after flipping "lang", which re-reads it; the static local then
 * recomposes the whole tree and every `remember` survives, so the drawer stays open.
 */
@Composable
private fun AppScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("capture", android.content.Context.MODE_PRIVATE)
    }
    var langTick by remember { mutableStateOf(0) }
    val tx = remember(langTick) {
        Tx.of(if (LangRow.VISIBLE) AppPrefs.read(prefs).lang else null)
    }
    CompositionLocalProvider(LocalTx provides tx) {
        Box {
            AppScreenBody(onLang = { langTick++ })
            // THE TOUR OVERLAY, the activity window's instance (port 61c §31): drawn over
            // whatever AppScreenBody rendered, exactly as the prototype mounts #t55 as the
            // last child of #appwrap. Survey prep is a Dialog — its OWN window, which this
            // cannot cover — so SurveyPrepDialog hosts the second instance, and only the
            // host the running step is in draws anything. No tour, nothing composed.
            TourOverlay(TourState.HOST_TREE)
        }
    }
}

@Composable
private fun AppScreenBody(onLang: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tx = LocalTx.current
    val store = remember { CaptureStore(context) }
    val scope = rememberCoroutineScope()

    var masters by remember { mutableStateOf(store.masters()) }
    // CONFIGURED = AN S5 SIGN-IN OR A STORED ERP PAIR (Amit, 22 Oct 2026: "remove the erp key
    // pair dependency and let S5 be store for apk"). Either one is enough to file and send
    // captures; the pair stays honoured during the migration, and showSettings now opens the
    // S5 sign-in, with the old key dialog behind its "ERP key (legacy)" link.
    var configured by remember {
        mutableStateOf(FrappeClient.configured(context) || S5Session.signedIn(context))
    }
    var showSettings by remember {
        mutableStateOf(!(FrappeClient.configured(context) || S5Session.signedIn(context)))
    }
    var showLegacyKey by remember { mutableStateOf(false) }

    // Where we are in the folder tree: client > SITE > project > room. Null
    // means "not that deep yet", which is also what Back unwinds.
    val cat = remember { Catalogue(context) }
    var navClient by remember { mutableStateOf<String?>(null) }
    var navSite by remember { mutableStateOf<String?>(null) }
    var navProject by remember { mutableStateOf<Catalogue.Project?>(null) }
    var navRoom by remember { mutableStateOf<String?>(null) }
    // THE CLIENT'S WISH LIST (v52 item 16, slice 5b): the Project.key whose list is open, in
    // place of that project's rooms. A screen of the PROJECT, never of a room or survey prep
    // (Amit, 14 Oct 06:36 IST: "the wishlist should never get mixed with survey"), so it is
    // let go the moment a room, another project, another tab or survey prep opens. The search
    // overlay is not a leave: Search then Back returns to it, as in the prototype.
    var wishFor by remember { mutableStateOf<String?>(null) }
    // Changed after a vendor quoted, this visit: the warning shows until the list is left.
    var wishWarned by remember { mutableStateOf(false) }
    // The open list's key (WishList.keyOf), so the leave can find it whichever screen the
    // leave opened: S5's status line belongs to the visit (port 61c slice W, v61c.1 §16.4).
    var wishKey by remember { mutableStateOf<String?>(null) }
    // The visit ends: the warning and S5's status line go together, as the prototype's
    // onBack and leave() clear `warned` and `refused` together. The search overlay is not a leave.
    fun leaveWish() {
        wishKey?.let { WishSync.leave(context, it) }
        wishKey = null
        wishFor = null
        wishWarned = false
    }
    // THE PROJECT ALBUM (port 61c slice K3): the Project.key whose album is open, in place of that project's
    // rooms, as the wish list is; and this visit's taps -- the chapters opened or folded by hand (n -> open),
    // the Work log, and the item open in the viewer. The visit's alone (the prototype's memOf, 7351-7352): the
    // leave forgets them. The search overlay is not a leave: Search then Back returns to the album as it was.
    var albumFor by remember { mutableStateOf<String?>(null) }
    val albumOpen = remember { mutableStateMapOf<Int, Boolean>() }
    var albumLog by remember { mutableStateOf(false) }
    var albumView by remember { mutableStateOf<String?>(null) }
    // The chapters' scroll, the visit's as its taps are: a new one for each visit, and gone with the leave.
    var albumList by remember { mutableStateOf(LazyListState()) }
    // The opened project's album mirror and the kept files of its captures, held HERE, above the search overlay's early
    // return, each with the key it was read under (the K3 review, finding 5): read inside the folder level, they left
    // composition while Search was up, so Back drew the album with no mirror -- no Viewing as, the phone's captures
    // alone, chapters folding and unfolding -- until the file was read again. Another project's never draws: the key
    // is compared where they are read.
    var heldMirror by remember { mutableStateOf<Pair<String, Album.View?>?>(null) }
    var heldFiles by remember { mutableStateOf<Pair<String, Map<String, String>>?>(null) }
    // THE PROJECT MANAGER'S WRITES (port 61c slice K4): the signed-in login's queued album ops for the opened project,
    // read WITH the mirror as one snapshot -- the queue first, then the mirror file (the K4 critic, finding 1) -- and
    // held above the search overlay with its key, as the mirror is; and this visit's open sheet and last review tap
    // (the prototype's memOf `sheet` and `res`), gone with the leave.
    var heldQueue by remember { mutableStateOf<Pair<String, List<Album.Op>>?>(null) }
    var albumSheet by remember { mutableStateOf<AlbumPage.SheetFor?>(null) }
    var albumRes by remember { mutableStateOf<AlbumPage.Res?>(null) }
    // THE PROGRESS SHOT (port 61c slice K5): the visit's camera (AlbumPage.Cam: its work, room, caption and open sheet),
    // gone with the leave; and the shot pending in the phone's camera app (AlbumWrite.Pending, as strings) in SAVED
    // state, so a camera app that makes Android recreate the activity still files the shot when its result reaches the
    // new composition (the K5 critic, finding 2) -- the album itself reopens closed, the app-wide rotation loss.
    var albumCam by remember { mutableStateOf<AlbumPage.Cam?>(null) }
    var albumPending by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<ArrayList<String>?>(null) }
    fun leaveAlbum() {
        albumFor = null
        albumOpen.clear()
        albumLog = false
        albumView = null
        albumList = LazyListState()
        albumSheet = null
        albumRes = null
        albumCam = null
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var showNewSite by remember { mutableStateOf(false) }
    // what to rename: kind, the current name, the project row it came from
    // (which carries the ERP docnames), and whether it lives on the server.
    var renaming by remember {
        mutableStateOf<RenameTarget?>(null)
    }
    var renameBusy by remember { mutableStateOf(false) }
    // WHY THE LAST SAVE DID NOT RENAME (port 62 slice A2; the prototype's P0, approved 11 Nov 2026): drawn under the
    // name while the dialog stays open, set on failure, cleared on edit and on dismiss. A Msg, so it reads in the
    // language shown; the tick counts failures, so the dialog returns to its top for each one, the same words or not.
    var renameWhy by remember { mutableStateOf<Msg?>(null) }
    var renameWhyTick by remember { mutableStateOf(0) }
    // The S5 projects this dialog's site rename is sending to, fixed by its first Save. S5's answers move the names
    // the tree groups by at once (a 409's stored name, 1 of 2 renamed), so "the projects under the old name" is no
    // longer the same list on Save again -- these are, each with the base S5 last gave for it.
    var renameIds by remember { mutableStateOf<List<String>?>(null) }
    // The server's own words for why a delete was refused — "still has 3
    // capture(s)". Cleared whenever the dialog target changes, so a refusal
    // about one project never arms a cascade on the next.
    var deleteBlocker by remember { mutableStateOf<String?>(null) }
    // Set beside deleteBlocker when the refusal came from THIS APP rather
    // than the bench — no docname to send, so no cascade to offer.
    var deleteFatal by remember { mutableStateOf(false) }
    // Work | Browse | Queue. Browse is the landing tab: Amit asked for the
    // app to open on Clients, ImageMeter-style.
    var tab by remember { mutableStateOf("browse") }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var prefsTick by remember { mutableStateOf(0) }
    // Bumped by every local create/rename/delete. The catalogue's locals live
    // in preferences, which Compose has no way to observe.
    var dataTick by remember { mutableStateOf(0) }
    // A face tagged with the work expected on it. Held per (capture, face)
    // because a 360 is a whole room and only its faces are single walls.
    var faceSkuTick by remember { mutableStateOf(0) }
    var faceSkuFor by remember { mutableStateOf<Pair<String, String>?>(null) }
    // Capture and face sit BELOW the room, and are plain state rather than
    // crumb levels: six crumbs will not fit a phone, and a photo you are
    // looking at is a view, not a folder.
    var navCapture by remember { mutableStateOf<CaptureCard?>(null) }
    var navFace by remember { mutableStateOf<Int?>(null) }
    var faceMode by remember { mutableStateOf(true) }   // true = annotated
    var showDetail by remember { mutableStateOf(false) }
    // THE QUOTE VIEW (port 61c slice QV): the Project.key whose line is opened, this visit only -- never
    // persisted, so a reload starts folded, as the prototype's open56 does; the pending refusal line per
    // project key (the prototype's msg: a Record S5 refused, from QM1 a move), cleared by a success; the
    // scope sheet's mode while it is open, S5's status line for it, and whether a Record or an Approve (QM2) is out;
    // how many answers the open sheet has had (each shows its status afresh, even in the same words) and how many
    // were a stale 409 (each redraws its ticks from the scope as stored) -- both 0 when a sheet opens (QV review).
    var quoteOpen by remember { mutableStateOf<String?>(null) }
    // Held as a Msg (design-marathi §2.4), so a refusal raised before the language changes redraws in the new one (QM1).
    var quoteMsg by remember { mutableStateOf<Pair<String, Msg>?>(null) }
    var scopeSheet by remember { mutableStateOf<QuoteView.Mode?>(null) }
    var scopeStatus by remember { mutableStateOf<String?>(null) }
    var scopeBusy by remember { mutableStateOf(false) }
    var scopeAnswers by remember { mutableStateOf(0) }
    var scopeReset by remember { mutableStateOf(0) }
    // THE PHASE STRIP's move (port 61c slice QM1): one PUT …/stage out at a time; while it is, no segment takes a tap
    // -- and the sheet's Approve (scopeBusy, QM2) is a PUT …/stage too: the strip's busy is either (QM2 review).
    var phaseBusy by remember { mutableStateOf(false) }
    // The visit: another project, or none, folds the view and closes the sheet (the refusal stays with its
    // key). Keyed on the project alone, so a room opened and closed keeps the view as it was, as the
    // prototype's open56 does.
    LaunchedEffect(navProject?.key) { quoteOpen = null; scopeSheet = null; scopeStatus = null }
    // Adding a room is a BENCH act, not a local one: the room is half of
    // every SKU code, so one minted on this phone would fork the prefix the
    // moment a second phone spelled it differently. Null when closed.
    var addRoom by remember { mutableStateOf(false) }
    var chooseRooms by remember { mutableStateOf(false) }
    // A 360 that has been split but NOT yet accepted. Holds everything the
    // commit needs, so approving it writes the gallery and the queue without
    // re-deriving anything -- the bytes he approved are the bytes that land.
    var pending by remember { mutableStateOf<PendingSplit?>(null) }
    // The pickers that re-file ONE photo: its SKU, and -- after Client approved --
    // its WORK through Which work? (port 61c slice FW2; the stage picker went).
    var retagSku by remember { mutableStateOf(false) }
    var refileWork by remember { mutableStateOf(false) }
    var showCaptureSheet by remember { mutableStateOf(false) }
    // WHICH WORK? from the capture sheet's WORK row (v61c §4.3), and the work picked for
    // new captures PER PROJECT, held in memory for this visit only, as the prototype's
    // S.capWorkFor is (keyed as the wish list keys a project: its ERP id, else the local
    // key). Null for a project: the first work In progress files the next capture, else
    // nothing does until a pick (Album.newCaptureWork). Never persisted: a stage or a
    // state kept on the phone would be a second answer (Amit, 27 Oct 2026).
    var pickWork by remember { mutableStateOf(false) }
    val capWorkFor = remember { mutableStateMapOf<String, Int>() }
    // The room's measured floor dimensions, which is what sizes the faces.
    // Amit, 2026-09-04: he measures length and width anyway to find the
    // centre to stand at, so the app takes the numbers he already has rather
    // than a Small/Medium/Large rounding of them.
    //
    // Remembered across launches like the preset was, because a second 360
    // of the same room needs the same faces — and re-typing them is exactly
    // the friction that made the old picker get left on whatever it was.
    val capturePrefs = remember {
        context.getSharedPreferences("capture", android.content.Context.MODE_PRIVATE)
    }
    // MILLIMETRES, and new pref keys on purpose (Amit, 2026-10-08: "capture
    // or let user everything keyed in mm. thats oour default unit of capture
    // always."). The old keys held INCHES, and a "120" left over from that
    // build would read as 120 mm here -- a cupboard, refused as not-a-room
    // only because the keys changed. Reusing them would be the quiet kind of
    // wrong, the same reason the feet-to-inches move changed keys too.
    // The geometry and the bench still work in inches; mm is converted at
    // the one place a plan is built (CaptureSheet.mmPlan).
    var roomLength by remember {
        mutableStateOf(capturePrefs.getString("room_len_mm", "").orEmpty())
    }
    var roomWidth by remember {
        mutableStateOf(capturePrefs.getString("room_wid_mm", "").orEmpty())
    }
    var roomHeight by remember {
        mutableStateOf(capturePrefs.getString("room_hgt_mm", "").orEmpty())
    }
    var updateJson by remember {
        mutableStateOf(capturePrefs.getString("update_available", null))
    }

    // Msg, not String (design-marathi §2.4, slice 1c): drawn in whatever language is on WHEN
    // drawn, so a message raised before the language changes redraws in the new one. Every
    // assignment is a lambda with a NAMED parameter -- `{ tx -> … }` for a keyed text, `{ _ -> … }`
    // for one still English -- so an `it` inside means the enclosing lambda's, never the Tx.
    var busy by remember { mutableStateOf<Msg?>(null) }
    var lastResult by remember { mutableStateOf<Msg?>(null) }
    var queue by remember { mutableStateOf(store.all()) }
    // Captures AND recorded work. The Queue badge is the one number in the
    // app that must never be optimistic, and an SKU sitting unsent on a
    // phone is exactly as lost as a photograph would be.
    val unsent = queue.count { it.state != "SYNCED" } + cat.localSkus().size

    // Annotation navigation: a capture opens its face list; a face opens
    // the editor. Plain state instead of a nav library — two levels deep.
    // Nothing sets facesFor today: the in-app annotator is parked behind the
    // ImageMeter round trip (task 33), and this is the hook it comes back
    // through. Kept deliberately rather than deleted and rewritten later.
    val annStore = remember { AnnotationStore(context) }
    var facesFor by remember { mutableStateOf<CaptureStore.Capture?>(null) }
    var laserTest by remember { mutableStateOf(false) }
    // THE HELP SCREEN (port 61c §31): the guided tours' only entry — the drawer's first
    // group opens it; no first-run overlay, no auto-start, no seen-once flag (the prototype
    // has none). Its branch returns early like the laser test's, below the tour host.
    var helpOpen by remember { mutableStateOf(false) }
    // Face Prep on one capture's six faces (Amit, 2026-10-09: "Build and ship apk").
    var facePrep by remember { mutableStateOf<CaptureStore.Capture?>(null) }
    var facePrepStart by remember { mutableStateOf("floor") }
    // Survey prep reopened on a kept capture (Amit, 2026-10-10: "Keep a local copy").
    var surveyKept by remember { mutableStateOf<KeptSurvey?>(null) }
    var surveyLoading by remember { mutableStateOf(false) }
    var surveyTick by remember { mutableStateOf(0) }
    // A 360 whose file went with the upload (before 0.3.204): its id, for the warning.
    var surveyMissing by remember { mutableStateOf<String?>(null) }
    // A kept 360 with no laser sizes stored: they are asked for before it opens.
    var surveyAskDims by remember { mutableStateOf<CaptureStore.Capture?>(null) }
    var annotating by remember { mutableStateOf<Pair<String, String>?>(null) }

    // The wish list is let go when its project is left (the prototype's view(): another
    // project, a room, another tab, survey prep). Here, above every early return, so it runs
    // whichever screen the leave opened.
    LaunchedEffect(wishFor, navProject?.key, navRoom, tab, surveyKept, facePrep) {
        if (wishFor != null && (navProject?.key != wishFor || navRoom != null || tab != "browse" ||
                surveyKept != null || facePrep != null)) {
            leaveWish()
        }
    }
    // The album is let go the same way (the prototype's view(): another project, a room, another tab, survey
    // prep, Face Prep) -- here, above every early return, so it runs whichever screen the leave opened.
    LaunchedEffect(albumFor, navProject?.key, navRoom, tab, surveyKept, facePrep) {
        if (albumFor != null && (navProject?.key != albumFor || navRoom != null || tab != "browse" ||
                surveyKept != null || facePrep != null)) {
            leaveAlbum()
        }
    }

    // EVERY branch below returns early, so each one needs its own
    // BackHandler — a branch without one falls through to Android's default,
    // which is "leave the app". That is how back came to close Site Photos
    // from a photograph instead of going up one level.
    // The laser test is app-level, not part of the folder tree: it proves the
    // DISTO link on the real meter before any measuring screen relies on it.
    if (laserTest) {
        BackHandler { laserTest = false }
        LaserTestScreen(onBack = { laserTest = false })
        return
    }
    facePrep?.let { cap ->
        BackHandler { facePrep = null }
        // The room box starts from what was typed at capture time, in mm; the
        // last-typed sizes on this phone otherwise; a plain 2700 x 3000 x 3000.
        val sp = context.getSharedPreferences("capture", android.content.Context.MODE_PRIVATE)
        fun mm(inches: Double, key: String, dflt: Int) =
            if (inches > 0) Math.round(inches * 25.4).toInt() else sp.getString(key, null)?.toIntOrNull() ?: dflt
        FacePrepScreen(
            deviceId = cap.deviceId,
            roomName = cap.room.ifBlank { cap.deviceId },
            fovDeg = if (cap.fov > 0) cap.fov else com.malletcrafts.interiorestimate.pano.Panorama.DEFAULT_FOV,
            startDims = Triple(mm(cap.roomHeightIn, "room_hgt_mm", 2700), mm(cap.roomWidthIn, "room_wid_mm", 3000),
                               mm(cap.roomLengthIn, "room_len_mm", 3000)),
            onBack = { facePrep = null },
            startFace = facePrepStart)
        return
    }
    annotating?.let { (devId, face) ->
        BackHandler { annotating = null }
        AnnotateScreen(deviceId = devId, face = face, store = annStore,
            onBack = { annotating = null })
        return
    }
    facesFor?.let { cap ->
        BackHandler { facesFor = null }
        FacesScreen(capture = cap, annStore = annStore,
            onFace = { face -> annotating = cap.deviceId to face },
            onBack = { facesFor = null })
        return
    }

    /**
     * Re-read everything the screens draw from.
     *
     * THE RULE, Amit 2026-08-22: "anything CRUD at app level should effect
     * locally immediately." Compose cannot see a SharedPreferences write or a
     * background worker's database write, so that rule is not automatic — it
     * has to be made true, deliberately, at every point where data changes.
     *
     * This is what was missing. `masters` was read once, in a
     * LaunchedEffect(configured) that never ran again, while SyncWorker kept
     * writing fresh masters underneath it. The visible result was worse than
     * staleness: a new project would sync, the worker would drop its local
     * copy (correctly — ERP has it now), and the project then existed only in
     * data this screen was not re-reading. So it DISAPPEARED until the app
     * was restarted.
     *
     * dataTick covers what masters and queue cannot: the locals live in
     * preferences, which no state object observes, so a bump is the only way
     * to tell Compose that a typed-in client is now real.
     */
    fun reload() {
        masters = store.masters()
        queue = store.all()
        updateJson = capturePrefs.getString("update_available", null)
        dataTick += 1
    }

    fun refreshQueue() = reload()

    // SURVEY PREP ON ANY KEPT 360 (Amit, 2026-10-11: "I want to have any room
    // opened up for existing data to have all these feature. if this is old
    // fotos where you have not stored 360 fotos, just warn me and save that
    // project"). The room tile and the capture screen both come here. Nothing
    // is changed or removed when the 360 is gone: it warns, and that is all.
    fun loadSurvey(row: CaptureStore.Capture, size: DoubleArray) {
        if (surveyLoading) return
        surveyLoading = true
        val title = RoomToken.label(tx, row.room)
        scope.launch(Dispatchers.Default) {
            val r = runCatching { FaceWriter.previewPano(File(row.panoPath)) }
            withContext(Dispatchers.Main) {
                surveyLoading = false
                r.onSuccess { surveyKept = KeptSurvey(row, title, it, size) }
                    .onFailure { e -> lastResult = { tx -> tx.app_msg_could_not_open_360_x(why = FaceNote.line(tx, e)) } }
            }
        }
    }
    /** A kept capture's row takes the room size survey prep was left at, in inches (v51). The bench's copy of
     *  an already-synced capture keeps the size it was synced with: nothing re-sends it. */
    fun keepRoomSize(deviceId: String, mm: DoubleArray) {
        val inch = SurveyPrep.inches(mm)
        runCatching { store.setRoomSize(deviceId, inch[0], inch[1], inch[2]) }
        refreshQueue()
    }
    fun openSurveyFor(deviceId: String) {
        val row = queue.firstOrNull { it.deviceId == deviceId } ?: return
        if (row.kind != "360") return
        if (!File(row.panoPath).exists()) { surveyMissing = deviceId; return }
        val size = SurveyPrep.reopenSize(SurveyPrepStore(context).loadSize(deviceId),
            row.roomLengthIn, row.roomWidthIn, row.roomHeightIn)
        if (size == null) surveyAskDims = row else loadSurvey(row, size)
    }
    surveyKept?.let { k ->
        SurveyPrepDialog(
            deviceId = k.row.deviceId, previewPano = k.pano, title = k.title,
            lengthMm = k.size[0], widthMm = k.size[1], heightMm = k.size[2],
            // the Plan tab's export names its files and fills its title block from these: initials, never the name
            clientInitials = RoomToken.initials(k.row.customerName), projectCode = k.row.project,
            captureDate = k.row.captureDate,
            // THE PROJECT'S QUOTE STAGE (port 62 slice L3): the capture's own project's, as the one store holds it NOW
            // -- read on every recomposition, so a sync that moves the stage while survey prep is open reaches it, as
            // the prototype reads the stage on every render. Survey's rules for a project S5 does not list.
            quoteStage = QuoteStageSync.prepStage(context,
                WishSync.stageKey(context, k.row.project, k.row.customerName, k.row.projectTitle)),
            // ... and, for the band at Client approved, that project's one derived line (port 62 slice L4), read the same way
            projectLine = QuoteStageSync.prepLine(context,
                WishSync.stageKey(context, k.row.project, k.row.customerName, k.row.projectTitle), tx),
            onDone = { p, size ->
                surveyKept = null; surveyTick++
                // The room size as survey prep now has it (v51: its size line is editable) goes to the
                // capture's row, in inches; and the room folder's elevations follow the lines as they now
                // stand, squared to THAT size, REPLACING the earlier ones (FaceWriter.writeSurvey).
                val row = k.row
                keepRoomSize(row.deviceId, size)
                lastResult = { tx -> tx.app_msg_survey_prep_saved_writing }
                scope.launch(Dispatchers.Default) {
                    val n = runCatching {
                        FaceWriter.rewriteSurvey(context, File(row.panoPath), row.deviceId, row.room,
                            row.captureDate, row.stage,
                            Handover.relativePath(row.customerName, row.projectTitle, row.room), p, size)
                    }
                    withContext(Dispatchers.Main) {
                        n.onSuccess { c -> lastResult = { tx -> tx.app_msg_survey_prep_saved_x(c.toString()) } }
                            .onFailure { e -> lastResult = { tx -> tx.app_msg_survey_prep_saved_not_written_x(why = FaceNote.line(tx, e)) } }
                    }
                }
            },
            onCancel = { _, size -> surveyKept = null; keepRoomSize(k.row.deviceId, size); surveyTick++ })
    }
    surveyAskDims?.let { row ->
        SurveyDimsDialog(
            title = RoomToken.label(tx, row.room),
            start = listOf(row.roomLengthIn, row.roomWidthIn, row.roomHeightIn).map { it * MM_PER_IN },
            okLabel = tx.common_survey_prep, dismissLabel = tx.common_cancel,
            onOk = { size -> surveyAskDims = null; loadSurvey(row, size) },
            onDismiss = { surveyAskDims = null })
    }
    surveyMissing?.let {
        AlertDialog(
            onDismissRequest = { surveyMissing = null },
            title = { Text(tx.app_survey_prep_can_t) },
            text = {
                Text(tx.app_360_photo_capture_was)
            },
            confirmButton = { TextButton(onClick = { surveyMissing = null }) { Text(tx.app_ok) } })
    }

    // ---- THE GUIDED TOURS' HOST (port 61c §31) -------------------------------------------
    // The demo flat, seeded idempotently at start (ch55, approved with v55): the capture row,
    // the bundled pano, the prep. Always in Browse, as Demo Client always is on the prototype.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { runCatching { TourSeed.ensure(context, store) } }
        reload()
    }
    // Where the app is, for the machine's screen predicates (the engine's screen(); survey
    // prep's own world rides TourBindSurvey in its Dialog).
    SideEffect {
        TourState.screen = when {
            helpOpen -> "help"
            laserTest -> "laser"
            tab != "browse" -> tab
            navRoom != null -> if (navCapture != null) "capture" else "room"
            navProject != null -> if (albumFor != null) "album" else if (wishFor != null) "wish" else "project"
            navSite != null -> "site"
            navClient != null -> "client"
            else -> "clients"
        }
    }
    // EVERY end — Done, Skip, ◁, Home — puts the demo back (the prep IN PLACE, written only
    // if the practice changed it; the row's size; the local phase) and returns to Help.
    TourState.onEnd = { r, how ->
        surveyKept = null
        navClient = null; navSite = null; navProject = null; navRoom = null
        navCapture = null; navFace = null
        tab = "browse"; showSearch = false
        TourState.demoPhase = r.snap.phase
        helpOpen = true
        android.util.Log.i("McftTour", "${r.flow.id}: $how after ${r.machine.seen.size} steps") // text:value log line
        scope.launch {
            // The dialog's own dispose-save (SurveyPrepScreen: `if (unsaved[0]) store.save(…)`)
            // must land BEFORE the restore, or the practice the tour made would be written back
            // over the snapshot: wait for the survey window to really leave — its bridge closes
            // the world (spOpen false) in the same disposal pass, after which that save has run.
            withTimeoutOrNull(TourMachine.WAIT_MS) { while (TourState.spOpen) delay(20) }
            val w = withContext(Dispatchers.IO) { TourSeed.restore(context, store, r.snap) }
            r.machine.wrote += w
            reload()
        }
    }
    // One step's setup, run on start, Next, Back and jumps alike (every setup is a POSITION,
    // so Back always lands right); then the anchor is awaited and the step shows. The one
    // shipped flow uses close / go / phase / survey / tab / mode and nothing else
    // (TourFlowsTest pins it); a blocked flow cannot start, so no other move is hosted.
    val tourRun = TourState.run
    LaunchedEffect(tourRun, tourRun?.stepSeq) {
        val r = tourRun ?: return@LaunchedEffect
        val step = r.machine.step
        fun goTo(where: String) {
            if (surveyKept != null) surveyKept = null
            helpOpen = false; showSearch = false; showCaptureSheet = false
            showDetail = false; chooseRooms = false
            leaveWish(); leaveAlbum()
            navCapture = null; navFace = null
            tab = "browse"
            val depth = when (where) {
                "clients" -> 0; "client" -> 1; "site" -> 2; "project" -> 3; "room" -> 4
                else -> -1
            }
            if (depth < 0) return
            navClient = if (depth >= 1) TourDemo.CLIENT else null
            navSite = if (depth >= 2) TourDemo.SITE else null
            navProject = if (depth >= 3) cat.projects(masters)
                .firstOrNull { TourDemo.isDemoProject(it.client, it.title) } else null
            navRoom = if (depth >= 4) TourDemo.ROOM else null
        }
        for (mv in step.setup) {
            if (TourState.run !== r) return@LaunchedEffect
            when (mv.kind) {
                "close" -> {
                    runCatching { drawerState.close() }
                    showSearch = false; showCaptureSheet = false; chooseRooms = false; showDetail = false
                }
                "go" -> goTo(mv.arg.orEmpty())
                // the demo's phase is PHONE-LOCAL (ch55's approved exception): no PUT, ever
                "phase" -> TourState.demoPhase = mv.arg?.toIntOrNull() ?: 0
                "survey" -> if (!TourState.spOpen) {
                    goTo("room")
                    openSurveyFor(TourDemo.CAPTURE)
                    withTimeoutOrNull(TourMachine.WAIT_MS) { while (!TourState.spOpen) delay(50) }
                }
                "tab", "mode" -> {
                    withTimeoutOrNull(TourMachine.WAIT_MS) { while (TourState.surveyMove == null) delay(50) }
                    TourState.surveyMove?.invoke(
                        if (mv.kind == "tab") mv.arg else null,
                        if (mv.kind == "mode") mv.arg else null)
                    delay(30)
                }
                else -> {}
            }
        }
        step.anchor?.let { a ->
            withTimeoutOrNull(TourMachine.WAIT_MS) {
                while (TourState.run === r && TourState.anchors[a] == null) delay(50)
            }
        }
        if (TourState.run === r) r.machine.shown(TourState.spMarks, System.currentTimeMillis())
    }
    // The Help screen, an early-return branch like the laser test's; a Start snapshots the
    // demo off the main thread, then the tour runs. ◁ and ← close it; a tour's end reopens it.
    if (helpOpen) {
        BackHandler(enabled = TourState.run == null) { helpOpen = false }
        HelpScreen(
            onBack = { helpOpen = false },
            onStart = { flow ->
                scope.launch {
                    val snap = withContext(Dispatchers.IO) { TourSeed.snapshot(context, store) }
                    TourState.start(flow, snap)
                }
            })
        return
    }



    // First composition with credentials: pull fresh masters in the
    // background so the pickers are not a week old.
    LaunchedEffect(configured) {
        if (configured) {
            withContext(Dispatchers.IO) {
                runCatching {
                    FrappeClient.load(context)?.bootstrap()?.let {
                        store.saveMasters(it)
                    }
                }
                // A PAIR IS STORED (the A2 review, finding 1): the S5 read below is not made, and no other read of
                // S5's list runs until a sync or an opened project -- so on the update that brought S5Sites, the
                // rename dialog opened on names S5 had never been asked for. Whatever S5 takes from this phone (its
                // sign-in, or the stored key) reads the list ONCE here, for its site names alone: the masters stay
                // the bootstrap's.
                if (FrappeClient.load(context) != null && S5Auth.current(context) != null) SiteRename.hear(context)
                // No pair, an S5 sign-in: S5's project list is the masters' projects
                // (S5Masters merges; null keeps what is here), as the sync does.
                if (FrappeClient.load(context) == null && S5Session.signedIn(context))
                    S5Masters.fetch(context)?.let { store.saveMasters(it) }
            }
            // reload, not `masters =` alone: the tree also draws from S5Sites, which no state object observes
            reload()
        }
    }

    // Back from the camera, from ImageMeter, from anywhere: re-read. A phone
    // that was in another app while a sync landed must not come back to a
    // tree drawn from before it.
    // Same accessor the capture screen already uses — the lifecycle-compose
    // one is not on this module's compile classpath.
    val appLifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(appLifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) reload()
        }
        appLifecycle.addObserver(obs)
        onDispose { appLifecycle.removeObserver(obs) }
    }

    // …and the moment a sync finishes, without waiting to be resumed. This is
    // the transition that used to lose a project: the worker replaces the
    // local row with ERP's copy, and until this ran, the screen held neither.
    LaunchedEffect(Unit) {
        androidx.work.WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow("mcft-sync")
            .collect { infos -> if (infos.any { it.state.isFinished }) reload() }
    }

    // ONE parse per data change, not one per row per frame. Keyed on both
    // masters (what the server sent) and dataTick (what was typed here).
    val cs = remember(masters, dataTick) { cat.snapshot(masters) }

    // S5's QUOTE stage (v52 item 16), read ONCE per project open: ONE GET of the caller's
    // list, cached for every project this phone knows, and the line drawn from that cache.
    // Held here, above every early return, so going into a room, the search overlay or a
    // capture and back does not leave composition and send the read again -- only a change
    // of the opened project does. A project only on this phone never asks S5. A failed read
    // changes nothing -- the cached copy stays and QuoteStageSync logs it once.
    var quoteTick by remember { mutableStateOf(0) }
    var albumTick by remember { mutableStateOf(0) }
    LaunchedEffect(navProject?.serverId) {
        val id = navProject?.serverId
        if (id.isNullOrBlank()) return@LaunchedEffect
        val known = cs.projects.map { it.serverId } + id
        val read = withContext(Dispatchers.IO) { QuoteStageSync.refresh(context, known) }
        if (read) quoteTick += 1
        // THE ALBUM MIRROR (port 61c slice QV), beside the read of the list: at Client approved the quote view's
        // role, scope and stamp, and each work's state at its base, into the one store; at Revisions (QM2) S5's
        // role for this person, which decides the panel's Approve the estimate ▸, and the scope as stored -- the
        // sheet's pre-ticks and the Approve's scope_base. SINCE K3 FOR EVERY PROJECT THE STORE HOLDS AS LISTED, at
        // every stage: the album draws from the mirror from the survey on (chapter 0's captures and stage moves are
        // S5's from the first upload) and its tile counts S5's photos, so the project open is the one read it needs
        // -- one album GET per open; the hourly SyncWorker read stays at Phases.needsMirror. A project S5 does not
        // list keeps no mirror (404); a failed read keeps the one it had.
        if (QuoteStageSync.cached(context, id)?.listed == true) {
            val got = withContext(Dispatchers.IO) { AlbumSync.refresh(context, id) }
            if (got) albumTick += 1
        }
        // THE ALBUM OUTBOX (port 61c slice K4): a queue this login left for the project is drained now (REPLACE: a job
        // waiting out a backoff runs at once), so opening the project is the cure for writes that waited for signal.
        if (AlbumOutbox.queued(context, id).isNotEmpty()) AlbumOutbox.enqueue(context, id, replace = true)
    }
    // ANY write of a mirror's stamp redraws the quote view -- the hourly SyncWorker run's read, a
    // Record's answer -- held like the quote listener below.
    DisposableEffect(context) {
        val ap = AlbumSync.prefs(context)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            albumTick += 1
        }
        ap.registerOnSharedPreferenceChangeListener(listener)
        onDispose { ap.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    // ANY write to the album outbox -- a tap queued, a drain's answer taken -- redraws the line, the states and the
    // album (port 61c slice K4): quoteTick for every reader of the line and the states (QuoteStageSync.lineFor and
    // .states lay the queue over the store), outboxTick for the album's snapshot of the queue and the mirror.
    var outboxTick by remember { mutableStateOf(0) }
    DisposableEffect(context) {
        val op = AlbumOutbox.prefs(context)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            outboxTick += 1
            quoteTick += 1
            // the drain moved a capture row (port 61c slice K5: a single photo's album photo landed and its row reads
            // synced, or S5's filing went back on it after a refused re-file): the screens re-read the store, as on resume
            // (the K5 critic, finding 6)
            if (key == AlbumOutbox.KEY_ROWS) reload()
        }
        op.registerOnSharedPreferenceChangeListener(listener)
        onDispose { op.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    // ANY write to the quote cache redraws the line -- the hourly SyncWorker run included,
    // which no work observer here watches. SharedPreferences holds listeners weakly, so the
    // listener is kept by this effect until it leaves composition.
    DisposableEffect(context) {
        val qp = QuoteStageSync.prefs(context)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            quoteTick += 1
        }
        qp.registerOnSharedPreferenceChangeListener(listener)
        onDispose { qp.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    // THE WISH LISTS (slice 5b): any write to them -- an edit, the sync taking S5's copy or
    // a 409, the read on project open -- redraws the chip and the open list, held like the
    // quote listener above.
    var wishTick by remember { mutableStateOf(0) }
    DisposableEffect(context) {
        val wp = WishSync.prefs(context)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            wishTick += 1
        }
        wp.registerOnSharedPreferenceChangeListener(listener)
        onDispose { wp.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    // S5's copy of the opened project's wish list, read once per project open (when online):
    // a list with no unsent edit takes it, so staff edits on /projects show on the phone. A
    // project only on this phone never asks S5; a failed read is one Log.w in WishSync.
    LaunchedEffect(navProject?.serverId) {
        val p = navProject ?: return@LaunchedEffect
        if (!p.synced) return@LaunchedEffect
        withContext(Dispatchers.IO) { WishSync.pull(context, p) }
    }
    // The IST day, moved at IST midnight: a cached copy read yesterday shows its read time
    // (decision 1, 15 Oct), so the line is recomputed when the day turns even if nothing
    // else on the screen changes.
    var istDay by remember {
        mutableStateOf(LocalDate.now(com.malletcrafts.interiorestimate.pano.QuoteStage.IST))
    }
    LaunchedEffect(Unit) {
        val ist = com.malletcrafts.interiorestimate.pano.QuoteStage.IST
        while (true) {
            val next = LocalDate.now(ist).plusDays(1).atStartOfDay(ist).toInstant()
            val wait = java.time.Duration.between(java.time.Instant.now(), next).toMillis()
            kotlinx.coroutines.delay(wait.coerceAtLeast(1_000L))
            istDay = LocalDate.now(ist)
        }
    }

    val rooms = cat.rooms(masters)
    fun siteTypeOf(client: String, site: String) =
        cs.sitesOf(client).firstOrNull { it.name.equals(site, true) }?.type.orEmpty()

    // The tree drives the capture target; nothing is pre-selected, because
    // a photo filed against whatever happened to be first in a list is the
    // failure this whole screen exists to prevent.
    // A site with no project row yet keeps its typed words; sync turns them
    // into masters through ensure_site.
    val project: ProjectRow? = navProject?.takeIf { it.synced }
        ?.let { ProjectRow(it.serverId, it.title, it.client) }
    val newSite: Pair<String, String>? =
        navProject?.takeIf { !it.synced }?.let { it.client to it.title }
    val room: String? = navRoom

    // WHERE THE NEXT CAPTURE FILES (v61c §4.3, port 61c slice FW2; the prototype's newWork):
    // the opened project's quote stage as the one store holds it, read under the key its
    // stage is read under (WishSync.stageKey); before Client approved every new capture is
    // the Quote journey's (work 0); after it, this project's pick (capWorkFor), else the
    // first work In progress by the store's states, else nothing -- and then the capture
    // sheet says `Pick the work` and its buttons wait. Re-read on any write to the store
    // (quoteTick), a reload (dataTick) or a re-key of the lists (wishTick); the line, which
    // the sheet's WORK row reads the stage from before approval, also when the IST day
    // turns and the language changes (tx), as the project screen's own line is.
    val fileKey = remember(navProject?.serverId, navProject?.client, navProject?.title, dataTick, wishTick) {
        navProject?.let { WishSync.stageKey(context, it) }
    }
    val fileCache = remember(fileKey, dataTick, quoteTick) { fileKey?.let { QuoteStageSync.cached(context, it) } }
    val fileLine = remember(fileKey, dataTick, quoteTick, istDay, tx) { QuoteStageSync.lineFor(context, fileKey, tx) }
    val fileApproved = Album.approved(fileCache?.stage)
    // the works' states with this login's queued writes laid over them (port 61c slice K4): a state tapped on the
    // album is the default pick and the Which work? line at once
    val fileStates = remember(fileKey, dataTick, quoteTick) { QuoteStageSync.states(context, fileKey) }
    val pickKey = navProject?.let { WishList.keyOf(it.serverId, it.client, it.title) }
    val newWork = Album.newCaptureWork(fileApproved, pickKey?.let { capWorkFor[it] }, fileStates)

    // One ingest path for BOTH capture routes: gallery-pick and the direct
    // X3 connection hand the same equirect JPG to the same split + queue.
    fun ingest(uri: Uri, kind: String = "360") {
        val p = newSite?.let { ProjectRow("", it.second, it.first) } ?: project
        val r = room
        if (p == null || r == null) return
        // A NEW CAPTURE CARRIES ITS WORK AND NO STAGE (port 61c slice FW2): the Quote
        // journey (0) before Client approved, else the work picked for this project or
        // the first In progress (newWork, above). No stage: nothing on the phone picks
        // one any more, so the ERP leg sends a blank work_stage and S1 files the row
        // under the frozen mallet_stage (accepted, SPEC61c §13.3), and Handover's
        // optional caption stage is blank. On an approved project with no work to file
        // at the sheet's buttons are disabled; a route that still arrives here is refused
        // with the sheet's own words rather than filed to 0. Only the LAUNCHER routes can
        // (picker, photoPicker, camera: rememberLauncherForActivityResult hands the result
        // to the LATEST composition's ingest, whose newWork is re-read when a sync moves
        // the stage to Client approved while the gallery or the camera app is open; the X3
        // shoot calls the ingest of the composition that drew the enabled button). A
        // DELIBERATE DEPARTURE from the prototype's ingest (index61c2.html:8016-8019),
        // which files such a capture at null, "not filed to a work": on the phone NULL in
        // the store means a pre-FW2 row filed by its legacy stage (schema 6), so a new
        // unfiled row cannot be told from one, and the row would file at 0 through
        // Album.workOfCapture and go to S5 as the Quote journey. Refusing keeps the one
        // rule: a capture after approval is a work's, or it waits for the pick.
        val workNow = newWork
        if (fileApproved && workNow == null) { lastResult = { tx -> tx.album_pick_work }; return }
        busy = { tx -> if (kind == "Photo") tx.app_filing_photo else tx.app_splitting_360_into_six }
        lastResult = null
        scope.launch(Dispatchers.Default) {
            // a single photo handed to the album (AlbumShots.single) is synced by it once its op is queued (the K5
            // review, finding 4); every other capture here as before
            var toAlbum = false
            val outcome = runCatching {
                val id = Handover.mintDeviceId(
                    ByteArray(6).also { SecureRandom().nextBytes(it) })
                val today = LocalDate.now().toString()
                // Measured room first; the office default only when he has
                // not given one, so a blank pair degrades to what the bench
                // says rather than to a guess made here.
                // The measured room decides all six faces. They do not want
                // the same number: the floor is at the camera's height above
                // it while a wall is half the room away, so a 20x18 ft room
                // wants 106 degrees on its walls and 144 on its floor. One
                // number for all six is what cropped the floor corners.
                val plan = mmPlan(roomLength, roomWidth, roomHeight)
                val perFace = plan?.clampedByFace ?: emptyMap()
                // Still one number for the fallback, because an unmeasured
                // room has nothing better and the bench default is what it
                // would have had anyway.
                val fov = plan?.wallFovDeg
                    ?: masters?.optDouble("default_fov", Panorama.DEFAULT_FOV)
                    ?: Panorama.DEFAULT_FOV
                if (kind != "Photo") {
                    // RENDER ONLY. Nothing reaches device photos or the upload
                    // queue until he has seen the six faces and said yes --
                    // Amit, 2026-09-22: "save to device photos should happen
                    // only after he confirms that 6 faces are good otherwise
                    // phone will get cluttered." A shot from the wrong spot
                    // used to cost seven files in the gallery and a queue row,
                    // undoable only by hand.
                    val prev = FaceWriter.begin(
                        context = context, source = uri, deviceId = id,
                        customerName = p.customer, projectTitle = p.title,
                        room = r, captureDate = today, stage = "", fov = fov,
                        fovByFace = perFace,
                        panoDir = File(context.filesDir, "panos"))
                    withContext(Dispatchers.Main) {
                        prev.heightMm = mmOrNull(roomHeight)
                        pending = PendingSplit(
                            session = prev, projectName = p.name,
                            projectTitle = p.title, customerName = p.customer,
                            room = r, work = workNow, captureDate = today,
                            deviceId = id, fov = fov, plan = plan,
                            lengthMm = mmOrNull(roomLength), widthMm = mmOrNull(roomWidth))
                    }
                    null
                } else {
                    val (result, pano) = FaceWriter.single(
                        context = context, source = uri, deviceId = id,
                        customerName = p.customer, projectTitle = p.title,
                        room = r, captureDate = today, stage = "",
                        panoDir = File(context.filesDir, "panos"))
                    val shotAt = System.currentTimeMillis()
                    store.insert(CaptureStore.Capture(
                        deviceId = id, project = p.name, projectTitle = p.title,
                        customerName = p.customer, room = r, stage = "",
                        captureDate = today, panoPath = pano.path,
                        createdAt = shotAt, state = "LOCAL",
                        serverName = null, error = null, kind = kind,
                        // A flat Photo is not projected: no angle, no room.
                        fov = 0.0,
                        // Filed by WORK from the shutter (FW2): 0 the Quote journey, else the pick.
                        work = workNow))
                    // A SINGLE PHOTO AFTER CLIENT APPROVED GOES TO S5'S ALBUM TOO (port 61c slice K5; Amit's answer 3 of
                    // 28 Oct 2026: "as a staff photo of its work through the progress shot's route (record first, then the
                    // JPEG)"), under its capture's own album id (Album.photoIdOf), for a project S5 lists, signed in to S5:
                    // its shot written from the kept file and its op queued on the process's scope (AlbumShots.single).
                    // The row stays the phone's item. Before approval it stays the phone's alone: answer 3 is "after
                    // approval" (S5's record would take it at 0 -- the reason is the answer, not S5).
                    val albumKey = fileKey
                    val login = S5Session.loginKey(context)
                    // The op is filed from the row as stored when it is added, after the encode (AlbumOutbox.addSingle;
                    // the K5 review, finding 1), and this photo's sync runs after that add (finding 4).
                    if (fileApproved && albumKey != null && login.isNotEmpty() && S5Session.signedIn(context)) {
                        val op = AlbumWrite.single(id, workNow, r, shotAt)
                        if (op != null) {
                            AlbumShots.single(context, albumKey, login, op, pano)
                            toAlbum = true
                        }
                    }
                    result
                }
            }
            withContext(Dispatchers.Main) {
                busy = null
                // A null result means a 360 is waiting on his approval; the
                // preview sheet is the message, so there is nothing to say
                // in the banner.
                lastResult = null
                outcome.onSuccess { filed ->
                    if (filed != null) {
                        // a photo handed to the album: AlbumShots.single runs its sync after the add (finding 4)
                        if (!toAlbum) SyncWorker.syncNow(context)
                        val path = filed.relativePath
                        lastResult = { tx -> tx.app_msg_photo_filed_x(path) }
                    }
                }.onFailure { e -> lastResult = { tx -> tx.app_msg_could_not_split_x(why = FaceNote.line(tx, e)) } }
                refreshQueue()
            }
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> if (uri != null) ingest(uri) }

    // A FLAT photograph, from the gallery or straight off the phone camera.
    // A repair job is a close-up of a broken hinge and a snag list is a dozen
    // of them; forcing those through the 360 splitter is nonsense, and having
    // no route at all is why people fall back to the phone's camera app and
    // lose the client, site, room and stage along with the picture.
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> if (uri != null) ingest(uri, kind = "Photo") }

    // A gallery read this app is ALLOWED to do. Requested rather than
    // assumed: without it MediaStore hides every image another app wrote, so
    // the annotation scan searches a set that cannot contain the answer.
    //
    // No manual "pick the file yourself" fallback. Amit, 2026-08-22: "no
    // manual pick whtsover. you have ids local gallery access . if human can
    // see it, so you must." He is right — a picker would have been a way to
    // live with the bug instead of fixing it.
    var mediaTick by remember { mutableStateOf(0) }
    val askMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        mediaTick += 1
        lastResult = { tx -> if (granted) tx.app_msg_gallery_access_granted
                     else tx.app_msg_without_gallery_access }
        if (granted) SyncWorker.syncNow(context)
    }
    LaunchedEffect(configured) {
        // Asked once, on the way in, so the first annotation someone makes is
        // already findable rather than failing silently and needing a second
        // trip through the drawer.
        if (!StampScan.canRead(context)) askMedia.launch(StampScan.MEDIA_PERMISSION)
    }

    // ONE implementation, reachable from the drawer AND from the viewer a
    // person is looking at when they come back from ImageMeter. A feature
    // that only exists in a menu is a feature nobody finds twice.
    fun importFromImageMeter() {
                    busy = { tx -> tx.app_msg_importing_imagemeter }
                    scope.launch(Dispatchers.IO) {
                        // "Now" means now: drop the remembered misses so the
                        // gallery is read fresh. A picture scanned before it
                        // was annotated is cached as "nothing here", and the
                        // one button whose whole meaning is "go and look" is
                        // the right place to make that stop being true.
                        StampScan.forget(context)
                        if (!StampScan.canRead(context)) {
                            withContext(Dispatchers.Main) {
                                askMedia.launch(StampScan.MEDIA_PERMISSION)
                            }
                        }

                        // THE GALLERY FIRST, and reported in numbers.
                        //
                        // Amit, 2026-08-22: "thats not the intend to have it
                        // from google drive. i need to have local files
                        // annoated via imagemeter from gallery in my apk."
                        // Quite right — Drive was the mechanism that could
                        // never identify anything. This button now does the
                        // local route and only mentions Drive afterwards.
                        //
                        // The counts are the point. "Nothing new" is three
                        // different situations — no pictures looked at, none
                        // carrying our mark, or marks already sent — and they
                        // need three different answers.
                        val local = runCatching {
                            val scan = StampScan.scan(context)
                            var sent = 0
                            var waiting = 0
                            if (FrappeClient.load(context) != null) {
                                for (row in queue) {
                                    val found = scan.marks[row.deviceId] ?: continue
                                    val server = row.serverName
                                    if (server.isNullOrBlank()) { waiting += found.size; continue }
                                    sent += AnnotationPush.push(
                                        context, server, row.deviceId, found)
                                }
                            }
                            Triple(scan, sent, waiting)
                        }.getOrNull()
                        // A Msg, rendered at draw: the verdict sits in the banner until the next
                        // action, so it redraws in the language on THEN (the readingsMsg rule).
                        val localLine: Msg = local?.let { (scan, sent, waiting) ->
                            when {
                                !scan.allowed -> { tx: Tx -> tx.app_msg_android_hiding_photos }
                                sent > 0 -> { tx: Tx -> tx.app_msg_sent_x_annotated_gallery(sent) }
                                scan.stamped > 0 && waiting > 0 ->
                                    { tx: Tx -> tx.app_msg_x_annotated_not_reached("${scan.stamped}") }
                                scan.stamped > 0 ->
                                    { tx: Tx -> tx.app_msg_x_annotated_already_sent("${scan.stamped}") }
                                scan.looked == 0 -> { tx: Tx -> tx.app_msg_no_photos_gallery }
                                else -> { tx: Tx -> tx.app_msg_looked_x_gallery_photos("${scan.looked}") }
                            }
                        } ?: { tx: Tx -> tx.app_msg_could_not_read_gallery }
                        // THE DRIVE LEG IS GONE, and it is a removal rather
                        // than a regression. Amit, 2026-09-04: "everything
                        // works on local fone storage now like imagemeter and
                        // mcft-site-foto is on same fone."
                        //
                        // It could never identify anything anyway — ImageMeter
                        // renames its exports, so the filename that carried the
                        // capture id does not survive the trip, which is the
                        // whole reason the gallery route above exists. What it
                        // did reliably do was cost six seconds of sleep on
                        // every press and then append a second verdict to the
                        // first: "Sent 2 annotated faces • Drive: nothing new"
                        // reads like a partial failure when it is a complete
                        // success.
                        //
                        // The SERVER's sync is untouched and still runs on its
                        // schedule; what goes is the phone's ability to poke it
                        // and the sentence that confused the answer.
                        withContext(Dispatchers.Main) {
                            busy = null
                            lastResult = localLine
                        }
                    }
    }

    var shotUri by remember { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok: Boolean -> if (ok) shotUri?.let { ingest(it, kind = "Photo") } }

    // THE PROGRESS SHOT'S CAMERA APP (port 61c slice K5): TakePicture into files/shots/album-<id>.jpg through the
    // FileProvider's shots/, the route the Capture 360 sheet's Camera above takes. Its ✓ files the PENDING shot -- held in
    // saved state with the project and the login it was drawn for -- from that entry alone, on the process's scope
    // (AlbumShots.file; the K5 critic, finding 2); the camera closes at once, and its chapter opens and the result line is
    // set only once the op is queued, so no frame draws an open chapter with no card. Its back leaves the camera as it
    // was and deletes the camera app's empty file.
    val albumCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok: Boolean ->
        val pend = AlbumWrite.Pending.of(albumPending)
        albumPending = null
        if (pend != null && !ok) runCatching { AlbumShots.rawFile(context, pend.shot.id).delete() }
        if (pend != null && ok) {
            albumCam = null
            AlbumShots.file(context, pend, java.time.Instant.now()) { queued ->
                if (queued) {
                    Album.chapterOf(pend.shot.work)?.let { albumOpen[it] = true }
                    albumRes = AlbumPage.Res.Filed(pend.shot.work, pend.shot.id)
                }
            }
        }
    }

    if (showSettings) {
        S5SignInDialog(
            onDismiss = { showSettings = false },
            onSignedIn = {
                configured = true
                showSettings = false
                SyncWorker.syncNow(context)
            },
            onSignOut = {
                S5Session.signOut(context)
                configured = FrappeClient.configured(context) || S5Session.signedIn(context)
                // signOut drops the projects only S5 listed for that person: redraw without them.
                masters = store.masters()
            },
            onLegacy = { showSettings = false; showLegacyKey = true })
    }

    // The ERP key dialog, reachable ONLY through the sign-in's "ERP key (legacy)" link
    // during the migration (22 Oct 2026).
    if (showLegacyKey) {
        SettingsDialog(
            initialUrl = FrappeClient.savedUrl(context),
            onDismiss = { showLegacyKey = false },
            onSave = { url, key, secret ->
                FrappeClient.save(context, url, key, secret)
                configured = true
                showLegacyKey = false
                SyncWorker.syncNow(context)
            })
    }

    // ---- the rename dialog (port 62 slice A2) ------------------------------------------------
    fun closeRename() {
        renaming = null
        renameWhy = null
        renameIds = null
        deleteBlocker = null
        deleteFatal = false
    }
    // Every failure of a rename is said IN the dialog, which stays open (audit R2): never on a banner another screen draws.
    fun renameFailed(why: Msg) {
        renameBusy = false
        renameWhy = why
        renameWhyTick += 1
    }
    // WHERE A RENAME GOES IS DECIDED IN ONE PLACE, and it is tested (Renames.route in :pano, beside Deletes.route).
    // This used to be `if (t.serverId.isBlank())` inline: a blank id renamed "locally" without checking a row was
    // there, and any id went to the bench -- with a key that died on 23 Oct, for a site S5 now keeps the name of.
    fun renameRoute(t: RenameTarget, listRead: Boolean = false): RenameRoute {
        val isSite = t.kind == "site"
        val ids: List<String> = if (!isSite) emptyList() else (renameIds ?: cat.s5ProjectsOf(masters, t.client, t.site))
        // Until S5's list has been read on this phone, "S5Sites names no project of the site" is not S5's answer
        // (the A2 review, finding 1): a site that may be on S5 then reads the list first (Renames.route).
        val heard = !isSite || S5Sites.heard(context)
        return Renames.route(
            tx, t.kind, t.current, t.serverId,
            hasLocalRow = cat.hasLocal(t.kind, t.client, t.site, t.project),
            s5Projects = ids.mapNotNull { id -> S5Sites.held(context, id)?.let { RenameRoute.Write(id, it.site) } },
            // an S5 sign-in, or the legacy key S5 also takes: with neither there is nothing to call S5 with
            signedIn = S5Session.signedIn(context) || FrappeClient.authorization(context) != null,
            legacyPair = FrappeClient.load(context) != null,
            unlisted = isSite && cat.hasUnlistedCapture(masters, queue, t.client, t.site),
            listRead = listRead,
            heard = heard,
            mayHold = !heard && cat.mayBeOnS5(masters, queue, t.client, t.site))
    }
    // THE LOCAL RENAME -- the Local route's Save, and a site's Save whose route became Local once S5's list had been
    // read for it (the A2 review, finding 1). A local row is words in this phone's preferences, so it is corrected
    // here and now, with no signal -- and CHECKED: a rename that touched no row is a failure, never the success
    // that closed the dialog onto an empty folder (audit R3).
    fun renameHere(t: RenameTarget, newName: String) {
        renameBusy = false
        if (!cat.renameLocal(t.kind, t.client, t.site, t.project, newName)) {
            renameFailed { tx -> Renames.nothingRenamed(tx, t.kind, t.current) }
            return
        }
        if (t.kind == "site") {
            // The site's chosen rooms follow its name (R7), and so does every row of the stored
            // masters that names this client's site: nothing of the old name stays behind.
            cat.moveRoomScope(t.client, t.site, newName)
            store.masters()?.let { m ->
                if (cat.renameSiteRows(m, t.client, t.site, newName)) store.saveMasters(m)
            }
        }
        // The client's wish list (slice 5b) is kept under 'local:<client>|<title>'
        // for a phone-only project, so it follows the new name -- or the chip reads
        // 0/12 and the re-key never finds it. The keys still in use AFTER the rename
        // decide a move from a copy (the same title at another site keeps its list).
        WishSync.renameLocal(context, t.kind, t.client, t.project, newName,
            cat.projects(store.masters()).filter { !it.synced }
                .map { WishList.localKey(it.client, it.title) }.toSet())
        reload()
        // Follow the rename: a person renaming the thing they are
        // standing in should not be thrown back to the root.
        when (t.kind) {
            "client" -> { navClient = newName; navSite = null; navProject = null }
            "site" -> { navSite = newName; navProject = null }
            else -> navProject = null
        }
        lastResult = { tx -> tx.app_msg_renamed_x(newName) }
        closeRename()
    }

    renaming?.let { t ->
        BackHandler { if (!renameBusy) closeRename() }
        // The form the dialog opens in: which grey line, and whether the name can be edited at all (P0 b, c).
        val opened = remember(t, masters, queue, dataTick, renameIds, renameWhyTick, tx) { renameRoute(t) }
        RenameDialog(
            what = t.kind, current = t.current,
            line = Renames.line(opened),
            shut = if (opened is RenameRoute.Blocked && Renames.shut(opened)) opened.reason else "",
            why = renameWhy?.invoke(tx),
            whyTick = renameWhyTick,
            onEdit = { renameWhy = null },
            serverId = t.serverId,
            busy = renameBusy,
            onDismiss = { closeRename() },
            onSave = { newName ->
                when (val route = renameRoute(t)) {
                    // The phone's own row: renamed here and now, and checked (renameHere).
                    RenameRoute.Local -> renameHere(t, newName)
                    // A CLIENT or a PROJECT the bench holds, with a stored pair: the
                    // office's record, renamed on the bench and not reported as done
                    // until the bench says so. Never a site (Renames.route).
                    is RenameRoute.Legacy -> {
                        renameBusy = true
                        renameWhy = null
                        scope.launch(Dispatchers.IO) {
                            val res = runCatching {
                                FrappeClient.load(context)
                                    ?.renameNode(t.kind, route.docname, newName)
                                    ?: error(tx.app_no_server_configured)
                            }
                            // JUDGED ON THE RENAME ALONE (audit R6). The masters are pulled back at once so the
                            // change is visible without a sync -- but in a call of their own: on a weak signal the
                            // bench had the new name and a failed refresh was reported as a failed rename.
                            if (res.isSuccess) runCatching {
                                FrappeClient.load(context)?.bootstrap()?.let {
                                    store.saveMasters(it)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                reload()
                                res.onSuccess {
                                    when (t.kind) {
                                        "client" -> { navClient = newName; navSite = null; navProject = null }
                                        else -> navProject = null
                                    }
                                    lastResult = { tx -> tx.app_msg_renamed_x(newName) }
                                    renameBusy = false
                                    closeRename()
                                }.onFailure { e ->
                                    val said = e.message
                                    renameFailed { tx -> Renames.benchRefused(tx, said) }
                                }
                            }
                        }
                    }
                    // A SITE S5 HOLDS (Amit, 11 Nov 2026: "Yes — S5 keeps the site name"): one PUT …/site per
                    // project of it that S5 lists, each with its own base. Success is S5's answers ALONE.
                    is RenameRoute.S5 -> {
                        renameBusy = true
                        renameWhy = null
                        scope.launch {
                            var go: RenameRoute = route
                            if (route.readFirst) {
                                // AUDIT R9: a capture of this site is under an S5 key the masters do not list. S5's
                                // list is read FIRST -- the Save needs the signal anyway -- and the route asked
                                // again: listed now, its project is one of the writes; still not, the rename is
                                // refused until it is (a rename now would fork that project on its next capture).
                                // AND THE A2 REVIEW, FINDING 1: S5's list had not been read on this phone at all, so
                                // nothing said whether S5 holds the site. Read now, the route asked again is S5's
                                // own answer: its projects are the writes, or S5 does not hold it (below).
                                val fresh = withContext(Dispatchers.IO) {
                                    S5Masters.fetch(context)?.also { store.saveMasters(it) }
                                }
                                if (fresh == null) {
                                    renameFailed { tx -> tx.refuse_site_offline }
                                    return@launch
                                }
                                reload()
                                go = renameRoute(t, listRead = true)
                            }
                            if (go == RenameRoute.Local) {
                                // S5's list, read for this Save, names no project of the site, and a row made
                                // here carries it: the phone's own row, renamed as the Local route renames one.
                                renameHere(t, newName)
                                return@launch
                            }
                            val s5 = go as? RenameRoute.S5
                            if (s5 == null) {
                                val said = (go as? RenameRoute.Blocked)?.reason
                                renameFailed { tx -> said ?: tx.refuse_site_offline }
                                return@launch
                            }
                            renameIds = s5.writes.map { it.project }
                            val outcome = withContext(Dispatchers.IO) { SiteRename.rename(context, s5.writes, newName) }
                            if (outcome is Renames.Outcome.Done) {
                                // IN ONE STEP, once S5 has answered for every project: the phone's own rows under
                                // the old name are re-keyed (R8's phone half: the bench is deliberately not told),
                                // the room-scope key moves (R7), and every row of the stored masters that still
                                // says the old name -- the bootstrap's `sites` row above all -- says the new one,
                                // so the old name is not left behind as an empty site. S5's name for each project
                                // is already kept (SiteRename, S5Sites): the tree shows it without a sync (R4).
                                val name = outcome.site
                                withContext(Dispatchers.IO) {
                                    cat.renameLocal("site", t.client, t.site, "", name)
                                    cat.moveRoomScope(t.client, t.site, name)
                                    store.masters()?.let { m ->
                                        if (cat.renameSiteRows(m, t.client, t.site, name)) store.saveMasters(m)
                                    }
                                }
                                reload()
                                navSite = name
                                navProject = null
                                lastResult = { tx -> tx.app_msg_renamed_x(name) }
                                renameBusy = false
                                closeRename()
                            } else {
                                // Not every project took the name: the dialog stays open and says why. What S5 DID
                                // answer is already on the phone (a 409's stored name, the projects that were
                                // renamed), so the tree is redrawn from it and Save again sends against it.
                                reload()
                                renameFailed { tx -> Renames.text(tx, outcome).orEmpty() }
                            }
                        }
                    }
                    // Nothing to send and nothing to rename: said under the name, at once.
                    is RenameRoute.Blocked -> {
                        val said = route.reason
                        renameFailed { _ -> said }
                    }
                }
            },
            blocker = deleteBlocker,
            blockerFatal = deleteFatal,
            // A client is a Customer and belongs to the office, not to a
            // phone: it may be renamed here and must not be removed here.
            // A node that never reached the bench is this phone's own words
            // and is dropped locally.
            // Wrapped in parentheses deliberately: `else { x -> ... }` parses
            // as the else BLOCK of the if-expression, not as a lambda, and the
            // arrow inside it is then a syntax error. Parentheses force it to
            // be read as the lambda it is meant to be.
            onDelete = if (t.kind == "client") null else ({ cascade: Boolean ->
                // WHERE THIS GOES IS DECIDED IN ONE PLACE, and it is tested.
                // This used to be `if (t.serverId.isBlank())` inline, which
                // sent a row the bench knew about down the local branch and
                // reported success — the 2026-08-24 site that was still there
                // three days later. Deletes.route refuses that case instead.
                val route = Deletes.route(tx, t.kind, t.serverId, t.serverKnown)
                if (route is DeleteRoute.Blocked) {
                    deleteBlocker = route.reason
                    deleteFatal = true
                } else if (route is DeleteRoute.LocalOnly) {
                    cat.removeLocal(t.kind, t.client, t.site, t.project)
                    reload()
                    when (t.kind) {
                        "site" -> { navSite = null; navProject = null }
                        else -> navProject = null
                    }
                    lastResult = { tx -> Deletes.localOnlyMessage(tx, t.current) }
                    closeRename()
                } else {
                    renameBusy = true
                    scope.launch(Dispatchers.IO) {
                        val res = runCatching {
                            FrappeClient.load(context)
                                ?.deleteNode(t.kind, t.serverId, cascade)
                                ?: error(tx.app_no_server_configured)
                        }
                        // The tree is rebuilt from the bench either way: a
                        // partial cascade that removed three of five projects
                        // must show what actually survived, not what was
                        // asked for.
                        runCatching {
                            FrappeClient.load(context)?.bootstrap()?.let {
                                store.saveMasters(it)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            renameBusy = false
                            reload()
                            res.onSuccess {
                                cat.removeLocal(t.kind, t.client, t.site, t.project)
                                reload()
                                when (t.kind) {
                                    "site" -> { navSite = null; navProject = null }
                                    else -> navProject = null
                                }
                                lastResult = { tx -> tx.app_msg_deleted_x(t.current) }
                                closeRename()
                            }.onFailure { e ->
                                // Kept in the dialog rather than thrown at the
                                // snackbar: the refusal is the question, and
                                // the button that answers it is right here.
                                deleteBlocker = e.message
                                    ?: tx.app_server_refused_no_reason
                                // The BENCH refused, which is retryable — the
                                // cascade button is exactly the answer to it.
                                deleteFatal = false
                            }
                        }
                    }
                }
            }))
        return
    }

    // ---- the folder tree ------------------------------------------------
    if (showNewSite) {
        NewSiteDialog(
            client = navClient.orEmpty(),
            site = navSite.orEmpty(),
            jobTypes = cat.jobTypes(masters),
            siteTypes = cat.siteTypes(masters),
            onDismiss = { showNewSite = false },
            onSave = { client, site, proj, jobType, siteType, address ->
                cat.addLocal(client, site, proj, siteType = siteType,
                    jobType = jobType, address = address)
                reload()          // the rule: it is on screen before this returns
                showNewSite = false
                navClient = client
                navSite = site
                navProject = Catalogue.Project(
                    client = client, site = site, siteId = "", title = proj,
                    serverId = "", jobType = jobType, local = true)
            })
    }

    // The download-and-install itself, lifted out of the banner that used to
    // sit over the captures list. Same mechanism, one caller.
    val startUpdate: () -> Unit = {
        pendingUpdate(context)?.let { info ->
            val update = info.optString("version_name")
            busy = { tx -> tx.app_msg_downloading_update_x(update) }
            scope.launch(Dispatchers.IO) {
                val outcome = runCatching {
                    val dest = File(context.filesDir, "updates/update.apk")
                    FrappeClient.load(context)!!.downloadPrivate(
                        info.getString("file_url"), dest)
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context, "com.malletcrafts.interiorestimate.fileprovider", dest)
                    context.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW)
                            .setDataAndType(uri, "application/vnd.android.package-archive")
                            .addFlags(
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    or android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                withContext(Dispatchers.Main) {
                    busy = null
                    // the download's own refusal keyed from its code (FrappeClient.Download); any
                    // other failure's words are the system's, data — "अपडेट झाले नाही: डाउनलोड: HTTP 404"
                    outcome.onFailure { e ->
                        lastResult = { tx -> tx.app_msg_update_failed_x(why =
                            if (e is FrappeClient.Download) tx.app_download_http_x(e.code.toString())
                            else e.message.orEmpty()) }
                    }
                }
            }
        }
    }

    // One drawer, shared by every tab: the settings are the app's, not a
    // screen's, and building it twice would let the two drift.
    val drawerContent: @Composable () -> Unit = {
        // Signed in to S5: the drawer names the PERSON (S5's who), not a URL they never typed.
        val s5Name = S5Session.who(context)?.optString("full_name")?.ifBlank { null }
            ?.takeIf { S5Session.signedIn(context) }
        SettingsDrawerContent(
            user = s5Name ?: FrappeClient.savedUrl(context).ifBlank { "MCFT Interior Estimate" }, // text:value the product name
            server = FrappeClient.savedUrl(context),
            groups = drawerGroups(
                tx = tx,
                queued = unsent,
                lastSync = if (queue.isEmpty()) tx.app_nothing_yet else tx.app_all_sent,
                version = appVersion(context),
                // Re-read on every drawer open, and re-judged against the
                // running build, so an update already installed never shows.
                updateVersion = remember(prefsTick, updateJson) {
                    pendingUpdate(context)?.optString("version_name")
                },
                onUpdate = startUpdate,
                cached = cacheSize(context, tx),
                prefs = remember(prefsTick) { AppPrefs.read(capturePrefs) },
                // The "Server" row: who is signed in to S5, else the legacy pair's URL.
                // Its onClick (onServer) opens the S5 sign-in either way. When the stored
                // ERP pair is refused (SyncWorker's bootstrap probe, 23 Oct 2026) AND there
                // is no S5 session to fall back on, the row says so and the tap signs in —
                // the alternative was each queued row reading "HTTP 401" with no way out.
                server = s5Name
                    ?: if (FrappeClient.refused(context)) tx.app_erp_key_refused_sign_s5
                       else FrappeClient.savedUrl(context),
                onToggle = { key ->
                    if (key == "clear_cache") {
                        context.cacheDir.resolve("ann").deleteRecursively()
                        lastResult = { tx -> tx.app_msg_cached_annotated_copies_cleared }
                    } else {
                        AppPrefs.flip(capturePrefs, key)
                        // The Wi-Fi choice is a WorkManager constraint,
                        // so the worker has to be re-scheduled to pick
                        // it up — a toggle that only changes a boolean
                        // would be decoration.
                        if (key == "wifi_only") SyncWorker.schedule(context)
                        // The language is provided above this screen (AppScreen), which
                        // re-reads it: every screen redraws at once and the drawer stays open.
                        if (key == "lang") onLang()
                    }
                    prefsTick++
                },
                onSignOut = {
                    // Out of BOTH: the S5 session and the legacy pair (22 Oct 2026). Neither
                    // touches the capture queue, so a phone handed on keeps its unsent work.
                    S5Session.signOut(context)
                    FrappeClient.forget(context)
                    configured = FrappeClient.configured(context) || S5Session.signedIn(context)
                    masters = store.masters()
                    showSettings = true
                },
                onImageMeterSync = { importFromImageMeter() },
                onSyncNow = { SyncWorker.syncNow(context) },
                onServer = { showSettings = true },
                onLaserTest = { laserTest = true },
                tours = remember(tx) { TourFlows.shipped(tx).size },
                onHelp = { helpOpen = true },
            ))
    }

    // ---- search: the escape hatch from four levels ----------------------
    if (showSearch) {
        BackHandler { showSearch = false }
        val hits = remember(searchQuery, masters, dataTick, tx) {
            searchTree(tx, cat, masters, searchQuery)
        }
        SearchOverlay(
            query = searchQuery,
            hits = hits,
            recents = listOf("MB", "KIT", "Kothrud"), // text:value demo data (the card's demo.* rows are skipped and generate no accessor)
            onQuery = { searchQuery = it },
            onPick = { h ->
                tab = "browse"
                navClient = h.client.ifBlank { null }
                navSite = h.site.ifBlank { null }
                navProject = cs.projects.firstOrNull { it.key == h.projectKey }
                navRoom = h.room.ifBlank { null }
                navCapture = null; navFace = null
                showSearch = false
            },
            onClose = { showSearch = false })
        return
    }

    // ---- Work and Queue ---------------------------------------------------
    if (tab != "browse") {
        // Browse is the landing tab, so back from Work or Queue returns
        // there rather than leaving. Only Browse's own root exits.
        BackHandler { tab = "browse" }
        val scopeTab = rememberCoroutineScope()
        val recents = remember(queue, masters, dataTick) { recentRooms(cat, masters, queue) }
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = { drawerContent() },
        ) {
            Scaffold(
                topBar = {
                    TreeTopBar(
                        title = if (tab == "work") tx.common_work else tx.app_sync_queue, // text:value "work" is the tab id, wire
                        // "3 waitings": app.x_waiting_queue, the card's key for this site since L2 —
                        // its {pl} folds the "s" exactly as the prototype's plural(unsent(),'waiting')
                        // (v53 app.js:565) and the phone's own fold did.
                        subtitle = if (tab == "work") "MCFT Interior Estimate" // text:value "work" the tab id; the product name
                                   else tx.app_x_waiting_queue(n = unsent),
                        onMenu = { scopeTab.launch { drawerState.open() } },
                        onSearch = { showSearch = true; searchQuery = "" })
                },
                bottomBar = {
                    BottomBar(tab, unsent) { tab = it }
                },
            ) { pad ->
                Box(Modifier.padding(pad)) {
                    if (tab == "work") {
                        WorkScreen(
                            resume = recents.firstOrNull(),
                            recents = recents.drop(1).take(5),
                            synced = unsent == 0,
                            queued = unsent,
                            onOpen = { r ->
                                tab = "browse"
                                navClient = r.client
                                navSite = r.site
                                navProject = cs.projects
                                    .firstOrNull { it.key == r.projectKey }
                                navRoom = r.room
                                navCapture = null; navFace = null
                            })
                    } else {
                        QueueScreen(
                            waiting = queue.filter { it.state != "SYNCED" }.map { queueRow(tx, it) } +
                                cat.localSkus().map { skuQueueRow(it) },
                            sent = queue.filter { it.state == "SYNCED" }.take(12).map { queueRow(tx, it) },
                            wifiOnly = AppPrefs.read(capturePrefs).wifiOnly)
                    }
                }
            }
        }
        return
    }

    // ---- the four folder levels, one shell ------------------------------
    if (navRoom == null) {
        val proj = navProject
        val client = navClient
        val site = navSite
        val scopeDrawer = rememberCoroutineScope()

        // Descending through a single child is a level nobody chose from, so
        // it is filled in rather than presented — and Back skips it again on
        // the way out, or the two directions disagree.
        fun openClient(c: String) {
            navClient = c; navSite = null; navProject = null
            val ss = cs.sitesOf(c)
            if (ss.size == 1) openSiteInto(cat, masters, ss[0].name) { s2, p2 ->
                navSite = s2; navProject = p2
            }
        }

        // System back unwinds the tree, and skips exactly the levels the way
        // DOWN skipped. Without this, back exits the app from four levels
        // deep — which on a phone reads as the app crashing.
        BackHandler(enabled = client != null) {
            when {
                proj != null -> {
                    val siblings = cs.projectsOf(client ?: "", site ?: "")
                    if (siblings.size == 1) {
                        if (cs.siteCount(client ?: "") == 1) {
                            navClient = null; navSite = null
                        } else navSite = null
                        navProject = null
                    } else navProject = null
                }
                site != null ->
                    if (cs.siteCount(client ?: "") == 1) {
                        navClient = null; navSite = null
                    } else navSite = null
                else -> navClient = null
            }
        }

        // The project's rooms as its grid shows them: the APPLIES TO chips of the wish list
        // are the same list.
        fun gridRooms(p: Catalogue.Project): List<String> = cat.roomsInScope(
            rooms, p.client, p.site, siteTypeOf(p.client, p.site),
        ) { r -> queue.any { it.room == r && it.projectTitle.equals(p.title, true) } }

        // The ERP id the opened project's quote stage is read under (WishSync.stageKey): its own,
        // or -- a phone-only project held across the sync that re-keyed its list (navProject is
        // not re-read after a sync) -- the id that list moved to; null when it is only on this
        // phone. Re-read on any write to the lists (wishTick), as the re-key is one.
        val stageKey = remember(proj?.serverId, proj?.client, proj?.title, dataTick, wishTick) {
            proj?.let { WishSync.stageKey(context, it) }
        }
        // The opened project's quote stage as the phone last knew it -- QuoteStageSync's read of
        // S5's list, or a wish-list answer that named it since (QuoteStageSync.named) -- for the
        // wish list, its chip and the project line alike, so none reads the stage differently.
        val wishCache = remember(stageKey, dataTick, quoteTick) {
            stageKey?.let { QuoteStageSync.cached(context, it) }
        }
        // THE PROJECT LINE (v61c; Amit, 27 Oct 2026: "everything should be single truth"): the ONE
        // derived line the project screen and the Wish list's top bar draw, from the same store.
        // Re-read after any write to it (quoteTick: this screen's read or any sync), after a reload
        // (dataTick), when the IST day turns, since the read time shows only on a copy from before
        // today, and when the language changes (tx).
        val projectLine = remember(stageKey, dataTick, quoteTick, istDay, tx) {
            QuoteStageSync.lineFor(context, stageKey, tx)
        }
        // THE ALBUM MIRROR of the opened project (port 61c slice QV), under the id its stage is read under:
        // S5's role for this person, the PM, the approved scope and its stamp. Re-read after any write to it
        // (albumTick: the read on project open, a sync's, a Record's answer) and after a reload (dataTick) --
        // OFF the main thread, as the refresh is: the file holds the whole paged log and up to a thousand photos,
        // and a read in composition parsed all of it on every tick (QV review). None until the read lands, and
        // none again for another project (held under its stageKey), so no project's scope shows on another's row.
        // Held above the search overlay (heldMirror, the K3 review, finding 5), so Search then Back keeps it.
        val mirror = heldMirror?.takeIf { it.first == stageKey }?.second
        // Who is signed in names the phone's captures and this login's on S5 (S5Session.who: whoami's `user` and
        // `full_name`), re-read when the sign-in may have changed; and since K4 whether a write can be sent under it
        // (S5Session.signedIn: a refused renewal keeps `who`, the K4 critic, finding 8).
        val s5In = S5Session.signedIn(context)
        val albumMe = remember(configured, prefsTick, showSettings, masters, s5In) {
            val w = S5Session.who(context)
            AlbumPage.Me(w?.optString("user").orEmpty(), w?.optString("full_name").orEmpty(), s5In)
        }
        // This login's queued album ops for the project (AlbumOutbox, port 61c slice K4), held with the mirror.
        val albumQueue = heldQueue?.takeIf { it.first == stageKey }?.second.orEmpty()
        // ONE SNAPSHOT, THE QUEUE FIRST AND THEN THE MIRROR (the K4 critic, finding 1): the drain puts S5's row into the
        // mirror file BEFORE it takes the op out of the queue, so a queue read without the op is always paired with a
        // mirror read after S5's row landed -- never a frame in which the overlay has gone and S5's word has not come.
        // Re-read on any write to either (albumTick, outboxTick) and after a reload (dataTick); both assigned together,
        // with no suspension between, so no frame draws one without the other.
        LaunchedEffect(stageKey, albumTick, dataTick, outboxTick, albumMe.user) {
            val k = stageKey
            if (k == null) {
                heldQueue = null
                heldMirror = null
                return@LaunchedEffect
            }
            val read = withContext(Dispatchers.IO) {
                val q = AlbumOutbox.queued(context, k)
                val m = AlbumSync.mirror(context, k)
                q to m
            }
            heldQueue = k to read.first
            heldMirror = k to read.second
        }
        // THE PROJECT ALBUM (port 61c slice K3): its items -- S5's photos and captures from the mirror, and the
        // phone's own captures of this project (the rows the room screen reads: the project's title, ignoring
        // case) -- through the ONE filing (AlbumPage.compose: a capture S5 also holds is one item, filed by the
        // phone), who is signed in naming them (albumMe, above). Each capture's `captured_at` as
        // the record sends it (AsIsPath.capturedAt, the phone's own zone) is C3's key for a legacy-… row.
        val albumRows = remember(queue, proj?.title) {
            val t = proj?.title.orEmpty()
            queue.filter { it.projectTitle.equals(t, true) }
        }
        val albumLocal = remember(albumRows) {
            albumRows.map { c ->
                AlbumPage.Local(c.deviceId, c.serverName, c.work, c.stage, c.createdAt,
                    AsIsPath.capturedAt(c.createdAt, java.time.ZoneId.systemDefault()), c.room, c.kind)
            }
        }
        // this login's queue too (port 61c slice K5): a queued progress shot is an item at once, the tile counting it
        val albumItems = remember(mirror, albumLocal, albumMe, albumQueue) { AlbumPage.compose(mirror, albumLocal, albumMe, albumQueue) }
        // The kept file of each of the phone's captures that is still there (a removed local copy is not), by
        // device id -- read off the main thread, a disk check per row; held above the search overlay with its key (heldFiles).
        val albumFiles = heldFiles?.takeIf { it.first == proj?.key }?.second.orEmpty()
        LaunchedEffect(proj?.key, albumRows) {
            val rows = albumRows
            val k = proj?.key
            val files = withContext(Dispatchers.IO) {
                rows.filter { it.panoPath.isNotBlank() && File(it.panoPath).isFile }.associate { it.deviceId to it.panoPath }
            }
            heldFiles = if (k == null) null else k to files
        }
        // The tile, last in the rooms grid: the count of what this person may see (AlbumPage.tile). Opening it starts
        // a visit: nothing opened or folded by hand, the log folded, no photo open, the missing pictures asked again.
        val albumTile = proj?.let { p ->
            AlbumTileArgs(AlbumPage.tile(tx, p.title, mirror, projectLine, albumItems)) {
                AlbumImages.newVisit()
                albumOpen.clear()
                albumLog = false
                albumView = null
                albumList = LazyListState()
                albumSheet = null
                albumRes = null
                albumCam = null
                albumFor = p.key
            }
        }
        // THE QUOTE VIEW (SPEC61c §4.1 item 3): only at Client approved is the line a tap and the view drawn.
        // Opened for this visit alone (quoteOpen is the project's key; leaving the project folds it); the pending
        // refusal is this project's; the APPROVED SCOPE row reads the mirror (QuoteView.scopeRow).
        val quoteArgs = if (proj == null || stageKey == null || projectLine.kind != QuoteStage.LineKind.EXEC) null
            else QuoteViewArgs(
                open = quoteOpen == proj.key,
                onToggle = { quoteOpen = if (quoteOpen == proj.key) null else proj.key },
                refusal = quoteMsg?.takeIf { it.first == proj.key }?.let { it.second(tx) },
                // the APPROVED SCOPE with this login's queued approved adds and removes amended in (port 61c slice K4:
                // Album.scopeWithQueued, its since the tap's), as the prototype's quote view reads the amended scope
                row = (mirror?.let { Album.scopeWithQueued(it.scope, it.scopeAt, albumQueue) }).let { sc ->
                    QuoteView.scopeRow(tx, sc?.first, sc?.second.orEmpty(), mirror?.role)
                },
                onViewScope = { scopeStatus = null; scopeAnswers = 0; scopeReset = 0; scopeSheet = QuoteView.Mode.VIEW },
                onRecord = { scopeStatus = null; scopeAnswers = 0; scopeReset = 0; scopeSheet = QuoteView.Mode.RECORD })
        // THE PHASE STRIP through the quote journey (port 61c slice QM1; decision 2 of 27 Oct: the quote stage moves
        // from the phone as it does on /projects, one guarded value on S5): the five segments under the line, each tap
        // ONE online PUT …/stage (QuoteStageSync.move) whose answer goes into the one store -- its listener redraws the
        // line and the strip, so the current segment is always S5's word -- and whose refusal stands on the strip's
        // line (quoteMsg: the same pending refusal the quote view shows at Client approved, cleared by a move that
        // landed). SURVEY COVERAGE at Survey, over the rooms in scope and their captures (QuoteStage.coverage, the
        // rule the room dots use). At Revisions the panel's Approve the estimate ▸ for the PM -- S5's role from the
        // mirror (Phases.approve) -- opens the scope sheet's approve mode (QM2); the move is the sheet's, below. A
        // project S5 does not list draws the strip read-only (its line is LOCAL; the mover refuses NOT_ON_S5 before
        // any call, v61c.2). Not drawn at Client approved: the quote view is the strip there.
        val stripArgs = if (proj == null || projectLine.kind == QuoteStage.LineKind.EXEC) null else PhaseStripArgs(
            line = projectLine,
            refusal = quoteMsg?.takeIf { it.first == proj.key }?.let { it.second(tx) },
            coverage = if (Phases.phaseOf(projectLine) != Phases.SURVEY) null else gridRooms(proj).let { inScope ->
                QuoteStage.coverage(inScope, inScope.associateWith { r ->
                    queue.filter { it.room == r && it.projectTitle.equals(proj.title, true) }.map { QuoteStage.Shot(it.work, it.stage) }
                })
            },
            // either PUT …/stage out -- a segment's move or the sheet's Approve (scopeBusy): no segment, no Move back and no
            // Approve the estimate ▸ takes a tap until S5 answers, so a sheet closed with its Approve out cannot send a
            // second stage PUT from the strip, and no answer clears another's refusal (QM2 review)
            busy = phaseBusy || scopeBusy,
            role = mirror?.role,
            // Approve the estimate ▸ (QM2): opens the scope sheet's approve mode, pre-ticked from the mirror's scope; the
            // move is the sheet's Move to Client approved (its onSubmit below), never the button's
            onApprove = { scopeStatus = null; scopeAnswers = 0; scopeReset = 0; scopeSheet = QuoteView.Mode.APPROVE },
            onMove = { to ->
                val key = stageKey.orEmpty()
                phaseBusy = true
                scope.launch {
                    val out = withContext(Dispatchers.IO) {
                        val o = QuoteStageSync.move(context, key, to)
                        // An answer that leaves the project where the strip or the quote view reads the album
                        // (Phases.needsMirror) reads it now, as the project-open read does: a move to Revisions, so
                        // the panel draws S5's role for this person (Approve the estimate ▸, or the PM-only line;
                        // QM2); and one that reveals Client approved -- a stale or final 409 for a project approved
                        // meanwhile (critic G5) -- so the quote view's APPROVED SCOPE row draws S5's scope and role
                        // rather than `none recorded` from no mirror (QM1 review).
                        if (Phases.needsMirror(QuoteStageSync.cached(context, key)?.stage)) AlbumSync.refresh(context, key)
                        o
                    }
                    phaseBusy = false
                    // the mirror is re-read after every answer, as after a Record's: a 403 that names who approves
                    // (QM2) writes the role and PM into the mirror file and no stamp, so no listener fires (QM1 review)
                    albumTick += 1
                    val text = QuoteStageSync.moveText(tx, out)
                    when {
                        QuoteStageSync.moved(out) -> quoteMsg = null
                        text != null -> { val msg: Msg = { t -> QuoteStageSync.moveText(t, out).orEmpty() }; quoteMsg = proj.key to msg }
                    }
                }
            })
        // The project list's rows lead with each project's line (index61c2.html row61c), read from
        // the same store -- S5's one list GET carries every project's work_states, so a row needs no
        // album GET of its own. Each under the id its stage is read under (WishSync.stageKey), so a
        // re-keyed phone-only project's row and its screen agree (wishTick: the re-key is a write).
        val projectLines = remember(client, site, cs, dataTick, quoteTick, wishTick, istDay, tx) {
            if (client == null || site == null) emptyMap<String, String>()
            else cs.projectsOf(client, site).associate { p ->
                p.key to QuoteStageSync.lineFor(context, WishSync.stageKey(context, p), tx).text
            }
        }

        // THE PROJECT ALBUM in place of the rooms (port 61c slice K3), BEFORE the wish list's branch as the
        // prototype's main() puts the album first (index61c2.html 8271): a sub-screen with no bottom bar and no
        // crumbs. Its Back goes to the project; the viewer's own BackHandler, composed later inside AlbumScreen and
        // enabled only while a photo is open, closes the viewer first. The page is built once from the same mirror,
        // store copy and project line the project screen holds -- the stage and the states from the store, never
        // from the mirror file -- with this visit's taps, and this login's queue laid over it (port 61c slice K4).
        if (proj != null && albumFor != null && proj.key == albumFor) {
            // The page's data is built where the data changes, and a chapter or Work log tap only sets each chapter's
            // open and the log's rows on it (AlbumPage.Model; the K3 review, finding 4): a tap re-ran the whole page
            // -- the chapters, a card per item, the walk -- on the main thread. The queue and who is signed in are
            // data (K4): a tap queued redraws the page from the overlay.
            val albumModel = remember(tx, proj.title, albumItems, mirror, wishCache, projectLine, albumQueue, albumMe) {
                AlbumPage.model(tx, proj.title, albumItems, mirror, wishCache, projectLine, albumQueue, albumMe)
            }
            val toggled = albumOpen.toMap()
            val albumResult = AlbumPage.resultText(tx, albumModel, albumRes)
            val albumPage = remember(albumModel, toggled, albumLog, albumResult) { AlbumPage.page(albumModel, toggled, albumLog, albumResult) }
            // The open sheet's model; one whose subject has gone (the person no longer writes, the stage moved, the request
            // was answered) closes, as the prototype's ov() and its sheets close themselves (the K4 critic, finding 6).
            val sheetFor = albumSheet
            val sheetModel = sheetFor?.let { AlbumPage.sheet(tx, albumModel, it, java.time.Instant.now()) }
            LaunchedEffect(sheetFor, sheetModel == null) { if (sheetFor != null && sheetModel == null) albumSheet = null }
            // THE PROGRESS SHOT (port 61c slice K5): the camera's model -- null once the person cannot shoot, and the camera
            // then closes, as the prototype's ov() closes one whose role has gone (index61c2.html 7576) -- and Which room?'s
            // rows, the project's rooms in the grid's order.
            val camFor = albumCam
            val camModel = camFor?.let { AlbumPage.camera(tx, albumModel, it) }
            LaunchedEffect(camFor != null, camModel == null) { if (camFor != null && camModel == null) albumCam = null }
            val roomModel = camFor?.takeIf { it.sheet == AlbumPage.CamSheet.ROOM }?.let { AlbumPage.roomSheet(tx, it, gridRooms(proj)) }
            // A tap's op goes to the outbox off the main thread -- after the ops stored now, with a predecessor's base the
            // screen had not caught up with (AlbumOutbox.add) -- and its drain runs now (REPLACE); the listener redraws.
            fun queueAlbum(op: Album.Op) {
                val k = stageKey ?: return
                val seen = albumQueue
                val login = albumMe.user
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { AlbumOutbox.add(context, k, login, op, seen) }
                    if (ok) AlbumOutbox.enqueue(context, k, replace = true)
                }
            }
            BackHandler { leaveAlbum() }
            AlbumScreen(
                project = stageKey.orEmpty(),
                page = albumPage,
                localPath = { id -> albumFiles[id] },
                view = albumView,
                listState = albumList,
                onView = { albumView = it },
                onToggle = { n, open -> albumOpen[n] = open },
                onLog = { albumLog = !albumLog },
                onBack = { leaveAlbum() },
                onSearch = { showSearch = true; searchQuery = "" },
                sheet = sheetModel,
                onSheet = { albumSheet = it },
                onState = { work, to, remark ->
                    val out = AlbumWrite.state(albumModel, work, to, remark, java.time.Instant.now())
                    if (out is AlbumWrite.Out.Queue) queueAlbum(out.op)
                    if (out !is AlbumWrite.Out.Refused) albumSheet = null
                    out
                },
                onRaise = { work, kind, note ->
                    val out = AlbumWrite.raise(albumModel, work, kind, note, java.time.Instant.now())
                    if (out is AlbumWrite.Out.Queue) queueAlbum(out.op)
                    albumSheet = null
                },
                onAnswer = { work, outcome, clientAt, how ->
                    val out = AlbumWrite.answer(albumModel, work, outcome, clientAt, how, java.time.Instant.now())
                    if (out is AlbumWrite.Out.Queue) queueAlbum(out.op)
                    if (out !is AlbumWrite.Out.Refused) albumSheet = null
                    out
                },
                onReview = { key, state ->
                    val out = AlbumWrite.review(albumModel, key, state, java.time.Instant.now())
                    if (out is AlbumWrite.Out.Queue) {
                        queueAlbum(out.op)
                        albumRes = AlbumPage.Res.Review(state, out.op.photo)
                    }
                },
                camera = camModel,
                roomSheet = roomModel,
                camStates = AlbumPage.shootStates(albumModel),
                // the button opens the camera with the prototype's values (AlbumWrite.openCam) and closes the viewer (7491)
                onShot = { AlbumWrite.openCam(albumModel)?.let { albumView = null; albumCam = it } },
                onCamWork = { albumCam = albumCam?.copy(sheet = AlbumPage.CamSheet.WORK) },
                onCamRoom = { albumCam = albumCam?.copy(sheet = AlbumPage.CamSheet.ROOM) },
                onCaption = { t -> albumCam = albumCam?.copy(caption = t) },
                onCamCancel = { albumCam = null },
                // the camera's shutter: the pending shot minted and held (saved state), then the phone's camera app
                onShutter = {
                    val k = stageKey
                    val shot = albumCam?.let { AlbumWrite.shot(albumModel, it) }
                    if (k != null && shot != null) {
                        val u = androidx.core.content.FileProvider.getUriForFile(
                            context, "com.malletcrafts.interiorestimate.fileprovider", AlbumShots.rawFile(context, shot.id))
                        albumPending = AlbumWrite.Pending(k, albumMe.user, shot).saved()
                        runCatching { albumCamera.launch(u) }.onFailure {
                            // no camera app answered: nothing queued, nothing drawn (the prototype has no such state: Q1)
                            albumPending = null
                            android.util.Log.w("AlbumShots", "no camera app answered: " + (it.message?.take(160) ?: it.javaClass.simpleName)) // text:value log line
                        }
                    }
                },
                onPickWork = { w -> albumCam = albumCam?.copy(work = w, sheet = null) },
                onPickRoom = { r -> albumCam = albumCam?.copy(room = r, sheet = null) },
                onCamSheetGone = { albumCam = albumCam?.copy(sheet = null) })
            return
        }

        // THE WISH LIST in place of the rooms (slice 5b): its own Back (composed after the
        // tree's, so it wins) goes to the project; no bottom bar, as on a capture.
        if (proj != null && wishFor != null && proj.key == wishFor) {
            BackHandler { leaveWish() }
            WishListScreen(
                project = proj,
                rooms = gridRooms(proj),
                line = projectLine,
                stageIdx = WishList.stageIndex(wishCache),
                notOnS5 = WishList.notOnS5(stageKey != null, wishCache),
                tick = wishTick,
                warned = wishWarned,
                onWarn = { wishWarned = true },
                onBack = { leaveWish() },
                onSearch = { showSearch = true; searchQuery = "" })
            return
        }

        val crumbs = buildList {
            add(Crumb(
                label = tx.common_clients,
                onUp = { navClient = null; navSite = null; navProject = null }))
            if (client != null) add(Crumb(
                // the no-client bucket through its master; a typed name passes through, then shortens
                label = shortName(Masters.client_name(tx, client)),
                siblings = cs.clients.map { it to Masters.client_name(tx, it) },
                onUp = { navSite = null; navProject = null },
                onSibling = { openClient(it) }))
            if (client != null && site != null) add(Crumb(
                label = Masters.site_name(tx, site),
                siblings = cs.sitesOf(client).map { it.name to Masters.site_name(tx, it.name) },
                onUp = { navProject = null },
                onSibling = { navSite = it; navProject = null }))
            if (proj != null) add(Crumb(
                label = proj.title,
                siblings = cs.projectsOf(client ?: "", site ?: "")
                    .map { it.key to it.title },
                onUp = { navProject = null },
                onSibling = { k ->
                    navProject = cs.projectsOf(client ?: "", site ?: "")
                        .firstOrNull { it.key == k }
                }))
        }

        val title: String
        val subtitle: String
        when {
            // the job type as the masters name it in the language (Supply & install -> पुरवठा व बसवणे), the site as typed
            proj != null -> { title = proj.title; subtitle = "${Masters.job(tx, proj.jobType)} · ${site.orEmpty()}" }
            site != null -> { title = site; subtitle = client.orEmpty() }
            client != null -> { title = client
                subtitle = tx.pl_site(cs.siteCount(client)) }
            else -> { title = tx.common_clients
                subtitle = tx.pl_client(cs.clients.size) }
        }

        if (showDetail && proj != null) {
            ProjectSheet(
                project = proj,
                photos = queue.count { it.projectTitle.equals(proj.title, true) },
                onDismiss = { showDetail = false })
        }
        // THE SCOPE SHEET (port 61c slices QV and QM2; SPEC61c §4.2): read-only from the APPROVED SCOPE row's tap;
        // Record the approved scope for a project approved before it had one; and from the Revisions panel's Approve
        // the estimate ▸, the approve mode, pre-ticked from the mirror's scope. Each submit is ONE online PUT -- a
        // Record is QuoteStageSync.recordScope; an Approve is QuoteStageSync.approve, the guarded move to Client
        // approved carrying the ticks as the baseline and, as scope_base, the scope_at the sheet opened them from
        // (QuoteView.Opened -- never the mirror's at submit time, which a read landing meanwhile moves while the ticks
        // stay, and an amendment would then be a 200; QM2 review; a Record's base is the same stamp): done, the sheet closes
        // and the pending refusal goes (and after an Approve the store says Client approved, so the line turns to
        // Execution, the strip folds into the quote view and the album is read for its APPROVED SCOPE row); refused,
        // S5's words stand in the sheet's status line and on the strip, and the sheet stays open -- a 403 that names
        // who approves goes into the mirror, so the panel redraws who may (Phases.approve), and a scope 409 redraws
        // the ticks from the scope as stored. The sheet's own back handling closes it before the tree's (◁ closes
        // the sheet first) -- except while its submit is out: then nothing closes it until S5 answers, and the strip
        // under it is busy with it (QM2 review).
        val sheetMode = scopeSheet
        if (sheetMode != null && proj != null && stageKey != null) {
            val key = stageKey
            // the view shows the scope with this login's queued amendments (port 61c slice K4), as the row does; a record
            // and an approve open from S5's own scope and stamp, the base their PUT sends
            val viewed = if (sheetMode == QuoteView.Mode.VIEW) mirror?.let { Album.scopeWithQueued(it.scope, it.scopeAt, albumQueue) } else null
            ScopeSheet(
                mode = sheetMode,
                stored = (viewed?.first ?: mirror?.scope).orEmpty(),
                scopeAt = viewed?.second ?: mirror?.scopeAt.orEmpty(),
                status = scopeStatus,
                answers = scopeAnswers,
                reset = scopeReset,
                busy = scopeBusy,
                onSubmit = { works, scopeBase ->
                    scopeBusy = true
                    scope.launch {
                        val r = withContext(Dispatchers.IO) {
                            if (sheetMode == QuoteView.Mode.APPROVE) {
                                val o = QuoteStageSync.approve(context, key, works, scopeBase)
                                // read the album where the screen draws from it (Phases.needsMirror), as a strip move
                                // does: a 200 reveals Client approved (the quote view's row); a refusal leaves the project
                                // at Revisions, where the role S5 now gives this person redraws who approves
                                if (Phases.needsMirror(QuoteStageSync.cached(context, key)?.stage)) AlbumSync.refresh(context, key)
                                SheetAnswer(QuoteStageSync.moved(o), QuoteStageSync.moveScopeStale(o)) { t -> QuoteStageSync.moveText(t, o) }
                            } else {
                                val o = QuoteStageSync.recordScope(context, key, works, scopeBase)
                                SheetAnswer(QuoteStageSync.scopeRecorded(o), QuoteStageSync.scopeStale(o)) { t -> QuoteStageSync.scopeText(t, o) }
                            }
                        }
                        scopeBusy = false
                        albumTick += 1
                        scopeAnswers += 1
                        if (r.stale) scopeReset += 1
                        val text = r.text(tx)
                        when {
                            r.done -> { scopeSheet = null; scopeStatus = null; quoteMsg = null }
                            text == null -> { scopeSheet = null; scopeStatus = null }
                            else -> { scopeStatus = text; val msg: Msg = { t -> r.text(t).orEmpty() }; quoteMsg = proj.key to msg }
                        }
                    }
                },
                onDismiss = { scopeSheet = null; scopeStatus = null })
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = { drawerContent()             },
        ) {
            Scaffold(
                topBar = {
                    Column {
                        TreeTopBar(title, subtitle,
                            onMenu = { scopeDrawer.launch { drawerState.open() } },
                            onSearch = { showSearch = true; searchQuery = "" })
                        CrumbRail(crumbs)
                    }
                },
                bottomBar = {
                    BottomBar(tab, unsent) { tab = it }
                },
            ) { pad ->
                Column(Modifier.padding(pad).fillMaxSize()) {
                    // A renewal ERP refused signs the phone out of S5 in the background
                    // (S5Session.relogin): say so, and why, rather than go on looking signed in.
                    val s5Refused = S5Session.refusal(context)
                        ?.takeIf { !S5Session.signedIn(context) && !FrappeClient.configured(context) }
                    if (!configured) {
                        Banner(tx.app_sign_erp_email_password)
                    } else if (s5Refused != null) {
                        // the stored refusal through its one mapper: the constants render their keys,
                        // S5's own sentence stays as stored (UploadNote.line, port 61c slice M6)
                        Banner(tx.app_sign_again_menu_server_x(why = UploadNote.line(tx, s5Refused)))
                    } else if (masters == null) {
                        Banner(tx.app_waiting_first_master_list)
                    }
                    when {
                        proj != null -> RoomsScreen(
                            project = proj,
                            // Narrowed to the rooms this site actually has,
                            // plus anything already holding captures.
                            rooms = cat.roomsInScope(
                                rooms, proj.client, proj.site,
                                siteTypeOf(proj.client, proj.site),
                            ) { r ->
                                queue.any { it.room == r &&
                                    it.projectTitle.equals(proj.title, true) }
                            },
                            onChooseRooms = { chooseRooms = true },
                            hiddenRooms = (rooms.size - cat.roomsInScope(
                                rooms, proj.client, proj.site,
                                siteTypeOf(proj.client, proj.site),
                            ) { r ->
                                queue.any { it.room == r &&
                                    it.projectTitle.equals(proj.title, true) }
                            }.size).coerceAtLeast(0),
                            // Offered only when the bench is reachable. A
                            // room typed offline could not be resolved
                            // against the master, and a phone-only room is
                            // exactly the forked prefix this avoids.
                            // With an S5 sign-in and no pair (22 Oct 2026) the
                            // room is added on the phone: S5 settles it on upload.
                            onAddRoom = if (configured && masters != null)
                                ({ addRoom = true }) else null,
                            captureCount = { r ->
                                queue.count { it.room == r &&
                                    it.projectTitle.equals(proj.title, true) }
                            },
                            // The derived dot (QuoteStage.roomDot): before approval a survey
                            // capture, after it a capture filed to one of the sixteen works.
                            roomDot = { r ->
                                QuoteStage.roomDot(queue.filter {
                                    it.room == r && it.projectTitle.equals(proj.title, true)
                                }.map { QuoteStage.Shot(it.work, it.stage) }, projectLine.kind == QuoteStage.LineKind.EXEC)
                            },
                            // The newest 360 in that room, which is already on
                            // this phone — so the grid fills with no network.
                            lastPano = { r ->
                                queue.firstOrNull {
                                    it.room == r &&
                                    it.projectTitle.equals(proj.title, true)
                                }?.panoPath
                            },
                            onOpen = { navRoom = it },
                            line = projectLine,
                            quote = quoteArgs,
                            strip = stripArgs,
                            // The client's wish list (slice 5b), kept per project on this
                            // phone; the count follows any write to the lists (wishTick).
                            // No chip at Survey, as the prototype hides it there (WishList.
                            // chipShown); shown with the stage unread and at Client approved.
                            onWish = if (!WishList.chipShown(WishList.stageIndex(wishCache))) null else ({
                                wishWarned = false
                                val k = WishList.keyOf(proj.serverId, proj.client, proj.title)
                                // A visit begins: a status line an earlier visit drew and never
                                // left (a rotation, the process killed) goes (WishList.entered).
                                WishSync.enter(context, k)
                                wishKey = k
                                wishFor = proj.key
                            }),
                            wishLabel = remember(proj.key, proj.serverId, wishTick, dataTick, tx) {
                                WishList.chip(tx, WishSync.count(context,
                                    WishList.keyOf(proj.serverId, proj.client, proj.title)))
                            },
                            onDetail = { showDetail = true },
                            album = albumTile)
                        site != null && client != null -> ProjectsScreen(
                            onRename = { p -> renaming = RenameTarget(
                                "project", p.title, p.client, p.site, p.title,
                                p.serverId,
                                // Projects are built straight from the
                                // bootstrap array with local=false, so this
                                // one field is already the honest answer.
                                serverKnown = !p.local) },
                            projects = cs.projectsOf(client, site),
                            captureCount = { p ->
                                queue.count { it.projectTitle.equals(p.title, true) }
                            },
                            // Each row's ONE derived line, read from the store the list GET
                            // fills (S5-L's work_states: no album GET per project).
                            lineOf = { p -> projectLines[p.key].orEmpty() },
                            onOpen = { navProject = it },
                            onNew = { showNewSite = true })
                        client != null -> SitesScreen(
                            onRename = { st -> renaming = RenameTarget(
                                "site", st.name, client, st.name, "", st.serverId,
                                // NOT !st.local. That flag is deliberately
                                // pessimistic — it goes true when any project
                                // under the site is unsynced — so a server
                                // site holding one local project would have
                                // routed to a local delete. serverKnown asks
                                // the only question that matters here: did the
                                // bootstrap name this site.
                                serverKnown = st.serverKnown) },
                            sites = cs.sitesOf(client),
                            projectCount = { st ->
                                cs.projectsOf(client, st.name).size
                            },
                            onOpen = { navSite = it.name },
                            onNew = { showNewSite = true })
                        else -> ClientsScreen(
                            onRename = { c -> renaming = RenameTarget(
                                "client", c, c, "", "",
                                // A client's ERP identity lives on its
                                // projects; a client with none is local by
                                // definition and renames on the phone.
                                cs.projectsOf(c, "").firstOrNull()?.clientId
                                    ?: cs.projects.firstOrNull {
                                        it.client.equals(c, true) && it.clientId.isNotBlank()
                                    }?.clientId ?: "") },
                            clients = cs.clients,
                            siteCount = { c -> cs.siteCount(c) },
                            projectCount = { c ->
                                cs.projectCount(c)
                            },
                            onOpen = { openClient(it) },
                            onNew = { showNewSite = true })
                    }
                }
            }
        }
        return
    }

    // ---- one capture, and one face of it -------------------------------
    navCapture?.let { cap ->
        // Two levels in one branch: a face closes back to the capture, the
        // capture closes back to the room.
        BackHandler {
            if (navFace != null) navFace = null else navCapture = null
        }
        // THE ROW AS STORED (port 61c slice K5; the K5 critic, finding 6): the album drain can move this capture's row --
        // a single photo's album photo landed (synced), or S5's filing put back after a refused re-file -- and the screen's
        // copy (navCapture) is re-read from the store's rows whenever they are read again.
        LaunchedEffect(queue, cap.deviceId) {
            val row = queue.firstOrNull { it.deviceId == cap.deviceId } ?: return@LaunchedEffect
            val w = Album.workOfCapture(row.work, row.stage)
            if (w != cap.work || row.state != cap.state) navCapture = cap.copy(work = w, state = row.state)
        }
        // A 360 has six faces; a flat photo has ONE entry, itself. The
        // viewer, the Original/Annotated toggle and the "Annotate in
        // ImageMeter" button are all driven by this list, so returning an
        // empty one for a photo is exactly why a single photograph had no
        // route into ImageMeter at all.
        val faces = remember(cap.deviceId, cap.kind) {
            if (cap.kind == "Photo") LocalFaces.photoOf(context, cap.deviceId)
            else LocalFaces.of(context, cap.deviceId)
        }
        // Annotated copies come from the bench, where the Drive round trip
        // already attached them to this capture by face. Cached to a file so
        // a second look costs nothing and still works with no signal.
        var annotated by remember(cap.deviceId) { mutableStateOf<Map<String, String>>(emptyMap()) }
        // Local first, and on its own effect so it lands whether or not the
        // capture has ever reached the bench. A photo annotated in a basement
        // is visible in the basement.
        var annotatedLocal by remember(cap.deviceId) {
            mutableStateOf<Map<String, android.net.Uri>>(emptyMap())
        }
        // Re-scanned on every RESUME, not once: the whole point is that you
        // leave for ImageMeter, draw on the wall, and come back — and coming
        // back is the only moment the app can know something changed.
        var annTick by remember(cap.deviceId) { mutableStateOf(0) }
        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val obs = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) annTick++
            }
            lifecycleOwner.lifecycle.addObserver(obs)
            onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
        }
        LaunchedEffect(cap.deviceId, prefsTick, annTick, mediaTick) {
            runCatching {
                withContext(Dispatchers.IO) {
                    // The gallery, and only the gallery. ImageMeter's data
                    // directory sits under /Android/data, which Android will
                    // not let any app be granted — so its "Show images in
                    // gallery" switch is the one route that can work, and a
                    // second half-route was a setting to explain rather than
                    // a capability to use.
                    //
                    // The STAMP is what finds it. ImageMeter renames what it
                    // exports — MCAP-…_front.jpg comes back as
                    // image_from_19._Aug_2026.jpg — so the name cannot be the
                    // key, and 88 rows in the server inbox saying "no capture
                    // id in the filename" are the proof. Amit, 2026-08-21:
                    // "site foto app written footer is the key to identify the
                    // foto and replace it with annotated image from gallery."
                    // The QR we burn into the caption bar rides along inside
                    // the picture, and ImageMeter draws on top of it, so it
                    // survives. The name match stays as a second pass for the
                    // rare exporter that keeps our filename; the stamp wins
                    // where both answer.
                    val byName = LocalFaces.annotatedOf(context, cap.deviceId)
                    byName + StampScan.annotatedFor(context, cap.deviceId)
                }
            }.onSuccess { annotatedLocal = it }
        }
        // …and straight home. The office should not have to be told a wall
        // was marked up; the phone knows which face it is, which is exactly
        // what the bench could not work out from a renamed file. Silent when
        // there is nothing new, because there usually is nothing new.
        LaunchedEffect(annotatedLocal, cap.deviceId) {
            if (annotatedLocal.isEmpty()) return@LaunchedEffect
            val server = queue.firstOrNull { it.deviceId == cap.deviceId }?.serverName
            if (server.isNullOrBlank()) return@LaunchedEffect
            runCatching {
                withContext(Dispatchers.IO) {
                    AnnotationPush.push(context, server, cap.deviceId, annotatedLocal)
                }
            }.onSuccess { n ->
                if (n > 0) lastResult = { tx -> tx.app_msg_sent_x_annotated_to_x(n = n, server = server) }
            }.onFailure { e ->
                lastResult = { tx -> tx.app_msg_could_not_send_annotation_x(why = e.message.orEmpty()) }
            }
        }
        LaunchedEffect(cap.deviceId) {
            val server = queue.firstOrNull { it.deviceId == cap.deviceId }?.serverName
            if (server.isNullOrBlank()) return@LaunchedEffect
            runCatching {
                withContext(Dispatchers.IO) {
                    val out = HashMap<String, String>()
                    val c = FrappeClient.load(context) ?: return@withContext out
                    val arr = c.captureDetail(server)
                        .optJSONArray("annotations") ?: return@withContext out
                    for (i in 0 until arr.length()) {
                        val a = arr.optJSONObject(i) ?: continue
                        val face = a.optString("face")
                        val url = a.optString("image")
                        if (face.isBlank() || url.isBlank()) continue
                        val dest = File(context.cacheDir, "ann/${cap.deviceId}_$face.jpg")
                        if (!dest.exists()) {
                            dest.parentFile?.mkdirs()
                            c.downloadPrivate(url, dest)
                        }
                        out[face] = dest.absolutePath
                    }
                    out as Map<String, String>
                }
            }.onSuccess { annotated = it }
        }

        // Local first: the moment somebody decides what a wall needs is the
        // moment they are standing in front of it, which is rarely the moment
        // there is signal.
        val faceSkuMap = remember(cap.deviceId, faceSkuTick) {
            FaceSkus.of(context, cap.deviceId)
        }
        // A tag is STORED as the bench's docname so it means one thing, and
        // SHOWN as the code a person wrote — nobody tagged a wall MEST-SKU-00013.
        // The same mapping the capture-level SKU has always used; the face map
        // was displaying whatever it held, which read as a docname whenever the
        // desk had set it.
        val faceSkuCodes = remember(masters, navProject) { cat.skusOf(masters, navProject) }
        fun skuLabel(v: String) =
            faceSkuCodes.firstOrNull { it.name == v }?.code ?: v
        LaunchedEffect(cap.deviceId, prefsTick) {
            val server = queue.firstOrNull { it.deviceId == cap.deviceId }?.serverName
            if (server.isNullOrBlank()) return@LaunchedEffect
            runCatching {
                withContext(Dispatchers.IO) {
                    val remote = FrappeClient.load(context)
                        ?.captureDetail(server)?.optJSONObject("face_skus")
                    if (remote != null) FaceSkus.acceptFromServer(context, cap.deviceId, remote)
                }
            }.onSuccess { faceSkuTick += 1 }
        }

        val faceIdx = navFace
        if (faceIdx != null && faceIdx in faces.indices) {
            val f = faces[faceIdx]
            FaceViewer(
                title = f.name.replaceFirstChar { it.uppercase() },
                // The prototype's viewer sub-line (v61c §4.4): the SKU, the id
                // and the date; the stage is gone from it.
                subtitle = (if (cap.sku.isNotBlank()) "${cap.sku} · " else "") + // text:value the SKU code and the glyph, data
                    "${cap.deviceId} · ${cap.date}", // text:value an id and a date, data
                source = ThumbSource.Content(f.uri),
                // The local copy wins. It is the same annotation, it is
                // already here, and it is newer than anything the round trip
                // could have brought back.
                // What the stamp scan found on this phone, then what the
                // server sent back. The local copy wins: same annotation,
                // already here, and newer than anything the round trip could
                // have brought.
                annotatedSource = annotatedLocal[f.name]?.let { ThumbSource.Content(it) }
                    ?: annotated[f.name]?.let { ThumbSource.LocalFile(it) },
                showAnnotated = faceMode,
                onToggle = { faceMode = it },
                onImport = { importFromImageMeter() },
                faceSku = skuLabel(faceSkuMap[f.name].orEmpty()),
                onPickFaceSku = { faceSkuFor = cap.deviceId to f.name },
                faces = faces,
                current = faceIdx,
                onPickFace = { navFace = it },
                onClose = { navFace = null })
            return
        }

        // THE WORK LINE'S SECOND TEXT (v61c §4.4, P23, critic G2; port 61c slice FW2): the
        // project's stage as the one store holds it (under the key its stage is read under)
        // decides the lock, and for a Quote-journey capture the line names the quote stage
        // the capture was TAKEN in -- Album.quoteTakenAt: Album.quoteAt over the album
        // mirror's stage rows, with the store's current stage at its stamp as one more point
        // (K1b's G2b), at the row's own shutter instant -- and `not read yet` wherever the
        // phone cannot honestly say (FW2 review): the stage never read, NAMED without its
        // stamp (atUnknown, as the project line says for the same copy), or, with no mirror
        // held, a capture older than the current stamp. For a project only on this phone it
        // reads Survey, the prototype's photo line (Album.photoQuoteLead). The mirror is read
        // off the main thread, as the quote view reads it, and again after any write to it
        // (albumTick). Since K3 the project open reads the album of every project S5 lists, at every
        // stage (AlbumSync), so the mirror is there once the project has been opened online; before that
        // read lands the current stage at its stamp is the whole history the line has.
        val capKey = remember(navProject?.serverId, navProject?.client, navProject?.title, dataTick, wishTick) {
            navProject?.let { WishSync.stageKey(context, it) }
        }
        val capCache = remember(capKey, dataTick, quoteTick) { capKey?.let { QuoteStageSync.cached(context, it) } }
        val capLine = remember(capKey, dataTick, quoteTick, istDay, tx) { QuoteStageSync.lineFor(context, capKey, tx) }
        val capApproved = Album.approved(capCache?.stage)
        // the works' states with this login's queued writes laid over them (port 61c slice K4)
        val capStates = remember(capKey, dataTick, quoteTick) { QuoteStageSync.states(context, capKey) }
        var capMirror by remember(capKey) { mutableStateOf<Album.View?>(null) }
        LaunchedEffect(capKey, albumTick) {
            capMirror = capKey?.let { k -> withContext(Dispatchers.IO) { AlbumSync.mirror(context, k) } }
        }
        val capRow = queue.firstOrNull { it.deviceId == cap.deviceId }
        val quoteAt = if (capCache == null || capRow == null) null else Album.quoteTakenAt(
            capMirror?.stages, java.time.Instant.ofEpochMilli(capRow.createdAt).toString(),
            capCache.stage, capCache.at, capCache.atUnknown)
        val workSub = Album.workSub(tx, capApproved, cap.work, Album.photoQuoteLead(tx, capLine, quoteAt),
            cap.work?.let { w -> capStates?.get(w)?.state })

        Scaffold(topBar = {
            TreeTopBar(
                // `<date> · <where filed>` (v61c §4.4): the Quote journey, the
                // work, or not filed to a work. No stage name, for any capture.
                title = "${cap.date} · ${Album.workName(tx, cap.work)}",
                subtitle = RoomToken.label(tx, navRoom ?: "") + " · " + cap.deviceId,
                // The magnifier used to open the API-KEY dialog from here.
                // That is the worst thing a button can do: a search icon that
                // shows somebody your server credentials. It searches, and
                // the lead icon is Back, because this screen is a view of one
                // photograph and not a level of the tree.
                onSearch = { showSearch = true; searchQuery = "" },
                onBack = { navCapture = null })
        }) { pad ->
            // THE KEPT 360 (Amit, 2026-10-10: "Keep a local copy"): a 360 stays on
            // the phone after its upload so survey prep can be reopened for the
            // site readings. Read fresh on every recomposition -- a removed copy
            // must take its buttons with it.
            val keptRow = queue.firstOrNull { q -> q.deviceId == cap.deviceId }
            val keptFile = keptRow?.takeIf { it.kind == "360" }?.let { File(it.panoPath) }?.takeIf { it.exists() }
            val surveySaved = remember(cap.deviceId, surveyTick) { SurveyPrepStore(context).load(cap.deviceId) }
            val openSurvey: (() -> Unit)? = if (cap.kind == "360") ({ openSurveyFor(cap.deviceId) }) else null
            // Only once the bench has its own copy: before that the kept file is the upload.
            val removeLocal: (() -> Unit)? =
                if (keptRow != null && keptFile != null && SurveyPrep.localCopyRemovable(keptRow.state)) ({
                    runCatching { keptFile.delete() }
                    SurveyPrepStore(context).delete(cap.deviceId)
                    surveyTick++
                    refreshQueue()
                    lastResult = { tx -> tx.app_msg_local_copy_removed_capture }
                }) else null
            Box(Modifier.padding(pad)) {
                CaptureScreen(
                    capture = cap,
                    faces = faces,
                    annotatedFaces = annotated.keys + annotatedLocal.keys,
                    folder = Handover.relativePath(
                        queue.firstOrNull { q -> q.deviceId == cap.deviceId }
                            ?.customerName ?: "",
                        navProject?.title ?: "",
                        navRoom ?: "") + Handover.filename(cap.deviceId, "front"),
                    // A 360's face opens straight in Face Prep on that face
                    // (Amit, 2026-10-10: "clicking a foto should directly take
                    // me to our new menus developed. why i see separate menu
                    // for face prep?"). A plain photo still opens the viewer.
                    onOpenFace = { i ->
                        val row = queue.firstOrNull { q -> q.deviceId == cap.deviceId }
                        if (row != null && row.kind == "360" && i in faces.indices) {
                            facePrepStart = com.malletcrafts.interiorestimate.pano.FacePrep.prepFace(faces[i].name)
                            facePrep = row
                        } else { navFace = i; faceMode = true }
                    },
                    // The WORK line: read-only until Client approved, then a
                    // tap opens Which work? and re-files this one capture.
                    workSub = workSub,
                    onPickWork = if (capApproved) ({ refileWork = true }) else null,
                    onPickSku = { retagSku = true },
                    serverId = queue.firstOrNull { q -> q.deviceId == cap.deviceId }
                        ?.serverName.orEmpty(),
                    onDelete = {
                        // The row and the files, then back to the room. Files
                        // first would leave a queue row pointing at nothing if
                        // the delete were interrupted; the row first leaves an
                        // orphan file, which costs disk and nothing else.
                        val q = queue.firstOrNull { it.deviceId == cap.deviceId }
                        store.delete(cap.deviceId)
                        // a single photo's album op, not sent yet, goes with it from whichever login's outbox holds it,
                        // with its shot (port 61c slice K5); one already on S5 stays there (no delete on S5: Q4). AFTER
                        // the row's delete (the K5 review, finding 1): an op still being encoded is added only if its
                        // row is there under the outbox's lock (AlbumOutbox.addSingle), so it is either never queued or
                        // queued before this unqueue takes it out
                        if (q?.kind == "Photo") { // text:value the store's kind, a wire value
                            val photoId = Album.photoIdOf(cap.deviceId)
                            scope.launch(Dispatchers.IO) { AlbumOutbox.unqueuePhoto(context, photoId) }
                        }
                        FacePrepStore(context).delete(cap.deviceId)
                        SurveyPrepStore(context).delete(cap.deviceId)
                        runCatching { File(q?.panoPath ?: "").delete() }
                        runCatching {
                            context.contentResolver.delete(
                                android.provider.MediaStore.Images.Media
                                    .EXTERNAL_CONTENT_URI,
                                "${android.provider.MediaStore.Images.Media.DISPLAY_NAME} LIKE ?",
                                arrayOf("${cap.deviceId}%"))
                        }
                        navCapture = null; navFace = null
                        refreshQueue()
                        lastResult = { tx -> tx.app_msg_capture_deleted }

                        // And on the bench, if it ever got there. Amit,
                        // 2026-08-23: "when captures are deleted its sku is
                        // not getting deleted." It was not getting deleted
                        // because the CAPTURE was not either — this used to
                        // apologise instead ("the server copy stays, delete
                        // it in ERPNext"), which is not a feature.
                        //
                        // Local first and unconditionally, as everywhere
                        // else: the row has to leave the screen in a basement
                        // too. The bench call is a follow-up, and a failed one
                        // leaves a record the next delete will find and
                        // remove, because delete_node treats an already-gone
                        // capture as done rather than as an error.
                        val server = q?.serverName
                        if (!server.isNullOrBlank()) {
                            scope.launch(Dispatchers.IO) {
                                val res = runCatching {
                                    FrappeClient.load(context)
                                        ?.deleteNode("capture", server)
                                }
                                withContext(Dispatchers.Main) {
                                    res.onSuccess { j ->
                                        // post() has already unwrapped
                                        // "message", so "removed" is at the
                                        // top. Saying when a SKU went too is
                                        // worth a line: an article vanishing
                                        // from every picker should not be a
                                        // thing somebody discovers later.
                                        val gone = j?.optJSONArray("removed")
                                        val tookSku = (0 until (gone?.length() ?: 0))
                                            .any { gone!!.optString(it).startsWith("sku:") }
                                        lastResult = { tx -> if (tookSku)
                                            tx.app_msg_capture_deleted_sku
                                        else tx.app_msg_capture_deleted_here_bench }
                                    }.onFailure { e ->
                                        lastResult = { tx -> tx.app_msg_deleted_here_bench_not_x(why = e.message.orEmpty()) }
                                    }
                                }
                            }
                        }
                    },
                    onSurveyPrep = openSurvey,
                    surveyNote = when {
                        keptFile == null -> tx.app_360_no_longer_phone
                        surveyLoading -> tx.app_opening_360
                        surveySaved != null -> tx.app_x_6_surfaces_bounded(
                            done = SurveyPrep.SURFACES.count { surveySaved.surface(it).done() }.toString(),
                            marks = surveySaved.surfaces.values.sumOf { it.marks.size }.toString(),
                            read = surveySaved.actual.size.toString())
                        else -> tx.app_not_done_yet_tap
                    },
                    onRemoveLocal = removeLocal,
                    // the number alone: the button's key carries the unit (app.remove_local_copy_x)
                    localSize = keptFile?.let { "%.1f".format(java.util.Locale.ROOT, it.length() / 1024.0 / 1024.0) } ?: "")
            }
        }

        // Re-tagging ONE photo's SKU. Local first and the bench second: the
        // correction has to survive a basement, and the row on screen has to
        // change the instant it is made or nobody trusts it.
        val serverName = queue.firstOrNull { it.deviceId == cap.deviceId }?.serverName
        fun retag(newSku: String) {
            store.setTags(cap.deviceId, sku = newSku)
            refreshQueue()
            // The column holds the Estimate SKU DOCNAME; the card shows the
            // code. Resolving here keeps the two from drifting the moment a
            // tag is changed.
            val shownSku = if (newSku.isBlank()) "" else
                cat.skusOf(masters, navProject).firstOrNull { it.name == newSku }?.code ?: newSku
            navCapture = cap.copy(sku = shownSku)
            if (!serverName.isNullOrBlank()) scope.launch(Dispatchers.IO) {
                val out = runCatching {
                    FrappeClient.load(context)?.setCaptureTags(serverName, sku = newSku)
                }
                withContext(Dispatchers.Main) {
                    out.onFailure { e -> lastResult = { tx -> tx.app_msg_could_not_refile_x(why = e.message.orEmpty()) } }
                }
            }
        }
        // RE-FILING ONE CAPTURE BY WORK (v61c §4.4; port 61c slice FW2), after Client
        // approved only. The phone's own copy first (CaptureStore.setWork: the stored work
        // stands over the legacy stage from now on, Album.workOfCapture) and the screen
        // with it; then, for a capture S5 holds -- its upload reads On web -- the unique
        // job mcft-work-<id> tells S5 through S5-R by each name the capture has had
        // (WorkRefile). A capture not on S5 yet carries the work in its next record. The
        // bench is not told: ERP files by the stage it was created with, which nothing moves.
        fun refile(work: Int) {
            val from = cap.work
            store.setWork(cap.deviceId, work)
            refreshQueue()
            navCapture = cap.copy(work = work)
            if (WorkRefile.onWeb(context, cap.deviceId)) WorkRefile.enqueue(context, cap.deviceId)
            // A SINGLE PHOTO re-files on S5's album too (port 61c slice K5: S5-P, by its uploader), behind its photo op
            // in WHICHEVER login's outbox holds it, signed in or not (the K5 review, finding 5: that queue is sent only
            // under its own login, the uploader), else in this login's when the mirror holds the photo or its row reads
            // synced -- the base the OVERLAID filing, built under the outbox's lock in tap order (AlbumOutbox.refileSingle;
            // the K5 critic, finding 1); else the phone's filing moves alone (a photo taken before approval).
            val key = capKey
            val login = S5Session.loginKey(context)
            if (cap.kind == "Photo" && key != null) { // text:value the store's kind
                val landed = queue.firstOrNull { it.deviceId == cap.deviceId }?.state == "SYNCED" // text:value the store's state
                AlbumOutbox.refileSingle(context, key, login, cap.deviceId, from, work, landed)
            }
        }
        if (refileWork) {
            WorkSheet(
                current = cap.work,
                states = capStates,
                onPick = { id -> refileWork = false; refile(id) },
                onDismiss = { refileWork = false })
        }
        if (retagSku) {
            CaptureSkuSheet(
                skus = cat.skusOf(masters, navProject),
                room = navRoom.orEmpty(),
                current = cap.sku,
                onDismiss = { retagSku = false },
                onPick = { code -> retagSku = false; retag(code) })
        }
        // The SAME picker, for ONE face. A 360 is a whole room and cannot be
        // one article; each of its faces is a single wall, and that is what a
        // SKU describes.
        faceSkuFor?.let { (devId, face) ->
            CaptureSkuSheet(
                skus = cat.skusOf(masters, navProject),
                room = navRoom.orEmpty(),
                current = faceSkuMap[face].orEmpty(),
                onDismiss = { faceSkuFor = null },
                onPick = { code ->
                    faceSkuFor = null
                    // Written here FIRST so the row changes under the thumb,
                    // then pushed. The rule: a change made on the phone shows
                    // immediately, with or without signal.
                    FaceSkus.set(context, devId, face, code)
                    faceSkuTick += 1
                    val server = queue.firstOrNull { it.deviceId == devId }?.serverName
                    if (!server.isNullOrBlank()) {
                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                FrappeClient.load(context)?.setFaceSku(server, face, code)
                            }.onSuccess { FaceSkus.markSynced(context, devId, face) }
                        }
                    }
                    lastResult = { tx -> if (code.isBlank()) tx.app_cleared_work_face
                                 else tx.app_x_face(code) }
                })
        }
        return
    }

    // ---- level 5: the captures in one room ------------------------------
    //
    // The one screen the redesign had not reached. It kept a legacy app bar
    // with a "< Rooms" text button, two dropdowns, and — UNDER the grid of
    // photographs — the same captures again as a list of cards. Amit reported
    // the thumbnails missing; they were there, with the thing they replaced
    // still sitting beneath them, which is worse than either alone.
    //
    // Now it is the prototype: the same shell as every other level, the grid
    // filling the screen, phase chips over it, and one FAB. Everything the
    // shutter needs moved behind that FAB, because none of it is worth a
    // third of the screen when you are looking for a photo you already took.
    val roomName = navRoom.orEmpty()
    val scopeRoom = rememberCoroutineScope()
    val cam = CameraCapability.port
    var camConnected by remember { mutableStateOf(cam?.connected == true) }
    var camNote by remember { mutableStateOf<Msg?>(null) }
    val projSkus = cat.skusOf(masters, navProject)
    // Each 360's survey prep in one line, for its tile.
    val surveyLines = remember(queue, surveyTick, roomName, tx) {
        queue.filter { it.room == roomName && it.kind == "360" }.associate { it.deviceId to surveyLine(context, tx, it) }
    }
    val roomCaptures = queue.filter {
        it.room == roomName &&
            it.projectTitle.equals(navProject?.title ?: "", true)
    }.map { c ->
        CaptureCard(
            deviceId = c.deviceId,
            date = c.captureDate,
            // WHERE IT IS FILED, through the one filing rule (v61c): the
            // stored work when the phone filed it, else its legacy stage
            // through the 39-stage table. The room screen groups, titles and
            // labels by this and by nothing else.
            work = Album.workOfCapture(c.work, c.stage),
            panoPath = c.panoPath,
            state = c.state,
            kind = c.kind,
            survey = surveyLines[c.deviceId].orEmpty(),
            // Matched on the DOCNAME, which is what the column holds, with the
            // code as a fallback for a tag made before the bench had the row.
            sku = projSkus.firstOrNull { it.name == c.sku }?.code ?: c.sku,
            article = projSkus.firstOrNull {
                it.name == c.sku || (it.code.isNotBlank() && it.code == c.sku)
            }?.article.orEmpty())
    }

    BackHandler { navRoom = null }

    val roomCrumbs = buildList {
        add(Crumb(label = tx.common_clients, onUp = {
            navClient = null; navSite = null; navProject = null; navRoom = null
        }))
        navClient?.let { c ->
            add(Crumb(label = shortName(Masters.client_name(tx, c)),
                onUp = { navSite = null; navProject = null; navRoom = null }))
        }
        navSite?.let { st ->
            // the site through its master, as the tree crumb above draws it (an M2 leftover, threaded in M6)
            add(Crumb(label = Masters.site_name(tx, st), onUp = { navProject = null; navRoom = null }))
        }
        navProject?.let { p ->
            add(Crumb(label = p.title, onUp = { navRoom = null }))
        }
        add(Crumb(
            label = RoomToken.of(roomName),
            siblings = (navProject?.let { p ->
                cat.roomsInScope(rooms, p.client, p.site, siteTypeOf(p.client, p.site)) { r ->
                    queue.any { it.room == r && it.projectTitle.equals(p.title, true) } }
            } ?: rooms).map { it to it },
            onUp = { },
            onSibling = { navRoom = it }))
    }

    // WHICH WORK? for the capture sheet's WORK row (v61c §4.3, critic P25/P28): a pick
    // sets this project's work and reopens the capture sheet; a dismiss, or ◁, reopens it
    // unchanged. It exists only after approval (the row takes a tap only then).
    if (pickWork) {
        WorkSheet(
            current = newWork,
            states = fileStates,
            onPick = { id ->
                pickKey?.let { capWorkFor[it] = id }
                pickWork = false; showCaptureSheet = true
            },
            onDismiss = { pickWork = false; showCaptureSheet = true })
    }

    if (addRoom) {
        var typed by remember { mutableStateOf("") }
        var problem by remember { mutableStateOf<String?>(null) }
        var working by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!working) addRoom = false },
            title = { Text(tx.app_new_room) },
            text = {
                Column {
                    OutlinedTextField(typed, { typed = it; problem = null },
                        label = { Text(tx.app_room_name) },
                        placeholder = { Text(tx.room_home_theatre) },
                        singleLine = true, enabled = !working)
                    Spacer(Modifier.height(8.dp))
                    // The code prefix, shown BEFORE it is committed. The room
                    // is half of every SKU code and the abbreviation is
                    // derived from the words, so a person choosing the name
                    // is choosing the prefix whether or not they know it.
                    // Showing it is what makes "Powder Room" clashing with
                    // "Pooja Room" comprehensible rather than arbitrary.
                    val tok = RoomToken.of(typed)
                    Text(
                        if (tok.isBlank()) tx.app_sku_code_prefix_appears
                        else tx.app_sku_codes_will_read(t = tok),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    problem?.let {
                        Spacer(Modifier.height(8.dp))
                        // The BENCH's own words. It knows which existing room
                        // a name collides with; paraphrasing that here would
                        // be a second copy of a rule that lives there.
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = typed.isNotBlank() && !working,
                    onClick = {
                        working = true; problem = null
                        // Read on the main thread: the site's type comes from Compose state.
                        val forProject = navProject
                        val forSiteType = forProject?.let { siteTypeOf(it.client, it.site) }.orEmpty()
                        scope.launch(Dispatchers.IO) {
                            // <Unit>: the local branch returns early, and nothing reads the bench's answer.
                            val r = runCatching<Unit> {
                                // S5 IS THE STORE (no ERP pair, an S5 sign-in -- 22 Oct 2026):
                                // a room on S5 is settled by the upload itself and needs no
                                // bench, so it is added HERE -- to the room list in the
                                // masters (S5Masters keeps it across syncs) and to this
                                // site's rooms, so its tile appears now. The bench's prefix
                                // check does not run; with no bench there is no master to
                                // collide with.
                                if (FrappeClient.load(context) == null && S5Session.signedIn(context)) {
                                    val name = typed.trim()
                                    val m = store.masters()?.let { JSONObject(it.toString()) } ?: JSONObject()
                                    val have = cat.rooms(m)
                                    val room = have.firstOrNull { it.equals(name, true) } ?: name
                                    if (room == name && have.none { it.equals(name, true) }) {
                                        val arr = org.json.JSONArray()
                                        (have + name).forEach { arr.put(it) }
                                        m.put("rooms", arr)
                                        store.saveMasters(m)
                                    }
                                    if (forProject != null) {
                                        val chosen = cat.roomScope(forProject.client, forProject.site)
                                            .ifEmpty { cat.defaultScope(forSiteType) }
                                        cat.setRoomScope(forProject.client, forProject.site, chosen + room)
                                    }
                                    return@runCatching
                                }
                                // load(), not a constructor: the credentials
                                // live in prefs and the client is built from
                                // them. The tile is only drawn when the app
                                // is configured, so null here is close to
                                // impossible -- which is exactly why it gets
                                // a sentence rather than a crash.
                                val client = FrappeClient.load(context)
                                    ?: throw IllegalStateException(
                                        tx.app_sign_first_menu_server)
                                client.ensureRoom(typed.trim())
                            }
                            withContext(Dispatchers.Main) {
                                working = false
                                r.onSuccess {
                                    addRoom = false
                                    reload()
                                    // Re-read the masters so the new room is
                                    // in the grid immediately; a room you
                                    // just made and cannot see reads as a
                                    // failure.
                                    SyncWorker.syncNow(context)
                                    val made = typed.trim()
                                    lastResult = { tx -> tx.app_msg_room_x_ready(made) }
                                }.onFailure { e ->
                                    // ApiException already carries the
                                    // bench's own humanised message — which
                                    // is the one worth showing, because it
                                    // names the room the abbreviation
                                    // collides with. Nothing is re-worded
                                    // here; a second copy of that rule on
                                    // the phone would eventually disagree
                                    // with the one enforcing it.
                                    problem = e.message ?: tx.app_could_not_add_room
                                }
                            }
                        }
                    }) { Text(if (working) tx.app_adding else tx.common_add) }
            },
            dismissButton = {
                TextButton(enabled = !working,
                    onClick = { addRoom = false }) { Text(tx.common_cancel) }
            })
    }

    pending?.let { ps ->
        NewCaptureSurvey(
            pending = ps,
            onDiscard = {
                FaceWriter.discard(ps.session)
                SurveyPrepStore(context).delete(ps.session.deviceId)
                pending = null
                lastResult = { tx -> tx.app_msg_discarded_nothing_was_saved }
            },
            onKeep = {
                val keep = ps
                pending = null
                busy = { tx -> tx.app_msg_saving_six_faces }
                scope.launch(Dispatchers.Default) {
                    val outcome = runCatching {
                        val res = FaceWriter.commit(context, keep.session)
                        store.insert(CaptureStore.Capture(
                            deviceId = keep.deviceId, project = keep.projectName,
                            projectTitle = keep.projectTitle,
                            customerName = keep.customerName, room = keep.room,
                            stage = "", captureDate = keep.captureDate,
                            panoPath = keep.session.panoFile.path,
                            createdAt = System.currentTimeMillis(), state = "LOCAL",
                            serverName = null, error = null, kind = "360",
                            fov = keep.fov,
                            // Filed by WORK from the shutter (FW2), as the split carried it.
                            work = keep.work,
                            // The laser sizes survey prep squared to, when it ran;
                            // else CaptureGeometry's, whose lengthIn is the
                            // left→right span (see mmPlan) -- stored, length is
                            // front→back.
                            roomLengthIn = keep.session.surveySize?.let { it[0] / MM_PER_IN }
                                ?: keep.plan?.widthIn ?: 0.0,
                            roomWidthIn = keep.session.surveySize?.let { it[1] / MM_PER_IN }
                                ?: keep.plan?.lengthIn ?: 0.0,
                            roomHeightIn = keep.session.surveySize?.let { it[2] / MM_PER_IN }
                                ?: keep.plan?.heightIn
                                ?: (keep.session.heightMm?.div(MM_PER_IN) ?: 0.0)))
                        res
                    }
                    withContext(Dispatchers.Main) {
                        busy = null
                        outcome.onSuccess {
                            SyncWorker.syncNow(context)
                            // read into vals first: the banner is drawn later, from these
                            val count = it.faceCount.toString()
                            val path = it.relativePath
                            lastResult = { tx -> tx.app_msg_x_pictures_saved_x(v = count, v2 = path) }
                        }.onFailure { e -> lastResult = { tx -> tx.app_msg_could_not_save_x(why = FaceNote.line(tx, e)) } }
                        refreshQueue()
                    }
                }
            })
    }

    // A new site -- nothing chosen, nothing captured -- opens straight on the room
    // chooser, once, so it never starts as the master's twenty-nine rooms. On
    // picking the SITE, not the project (Amit, 2026-10-10: "site type should be
    // shown when i select site not project"): the rooms are the flat's, shared
    // by every project at it.
    LaunchedEffect(navClient, navSite) {
        val c = navClient ?: return@LaunchedEffect
        val s = navSite ?: return@LaunchedEffect
        val k = "scope_asked:$c|$s"
        val titles = cs.projectsOf(c, s).map { it.title.lowercase() }.toSet()
        val empty = cat.roomScope(c, s).isEmpty() &&
            queue.none { it.projectTitle.lowercase() in titles }
        if (empty && !capturePrefs.getBoolean(k, false)) {
            capturePrefs.edit().putBoolean(k, true).apply()
            chooseRooms = true
        }
    }

    if (chooseRooms) {
        val sc = navProject?.client ?: navClient
        val ss = navProject?.site ?: navSite
        if (sc == null || ss == null) { chooseRooms = false } else {
            // A fresh site starts from its own type -- the one chosen when the
            // site was made, or ERP's -- rather than from nothing or from all
            // twenty-nine (Amit, 2026-10-09: "flat type selection is not good in
            // apk. it still shows old 30 + rooms even for new site"); the type
            // and BHK chips re-tick the list in one tap. Rooms holding photos in
            // any project at the site stay ticked.
            val already = cat.roomScope(sc, ss)
            val titles = cs.projectsOf(sc, ss).map { it.title.lowercase() }.toSet()
            val withPhotos = rooms.filter { r ->
                queue.any { it.room == r && it.projectTitle.lowercase() in titles } }.toSet()
            val siteType = cs.sitesOf(sc).firstOrNull { it.name.equals(ss, true) }?.type.orEmpty()
            val presetType = com.malletcrafts.interiorestimate.pano.FlatPresets.TYPES
                .firstOrNull { it.equals(siteType, true) } ?: "Flat" // text:value a stored site type
            var pType by remember(sc, ss) { mutableStateOf(presetType) }
            var pBhk by remember(sc, ss) { mutableStateOf(2) }
            fun presetTicks() = com.malletcrafts.interiorestimate.pano.FlatPresets.rooms(pType, pBhk).filter { it in rooms }.toSet() + withPhotos
            val start = if (already.isNotEmpty()) already else presetTicks()
            var ticked by remember(sc, ss) { mutableStateOf(start) }
            AlertDialog(
                onDismissRequest = { chooseRooms = false },
                title = { Text(tx.app_rooms_x(ss = ss)) }, // the typed site name, as typed
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        // no type picked renders its key, never an unknown value through the master (M6)
                        Text(tx.app_site_type_x_pick(v = if (siteType.isBlank()) tx.common_not_set else Masters.site(tx, siteType)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (t in com.malletcrafts.interiorestimate.pano.FlatPresets.TYPES)
                                FilterChip(selected = pType == t,
                                    onClick = { pType = t; ticked = presetTicks() },
                                    label = { Text(Masters.site(tx, t)) })
                        }
                        if (com.malletcrafts.interiorestimate.pano.FlatPresets.hasBedrooms(pType))
                            Row(Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (n in 1..5)
                                    FilterChip(selected = pBhk == n,
                                        onClick = { pBhk = n; ticked = presetTicks() },
                                        label = { Text("$n BHK") }) // text:value digits and a Latin abbreviation, the page's own "2 BHK"
                            }
                        Text(tx.app_x_x_rooms(size = (ticked + withPhotos).size, length = rooms.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        for (r in rooms) {
                            val has = r in withPhotos
                            Row(Modifier.fillMaxWidth().clickableRow {
                                    ticked = if (r in ticked) ticked - r else ticked + r
                                },
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Checkbox(checked = r in ticked || has,
                                    enabled = !has,
                                    onCheckedChange = {
                                        ticked = if (r in ticked) ticked - r else ticked + r
                                    })
                                Text(if (has) tx.app_x_has_photos(r = Masters.room(tx, r)) else Masters.room(tx, r),
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        cat.setRoomScope(sc, ss, ticked)
                        chooseRooms = false
                        reload()
                    }) { Text(tx.app_use_these_rooms) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        // Every room of the master, written out: an EMPTY scope now
                        // means the site's 2 BHK preset, so "all" has to be explicit.
                        cat.setRoomScope(sc, ss, rooms.toSet())
                        chooseRooms = false
                        reload()
                    }) { Text(tx.common_show_all) }
                })
        }
    }

    if (showCaptureSheet) {
        CaptureSheet(
            // The WORK row (v61c §4.3): where the next capture files, and why it cannot be changed yet
            workHead = Album.filingHead(tx, newWork),
            workSub = Album.workSub(tx, fileApproved, newWork, Album.quoteLead(tx, fileLine, fileLine.quote),
                newWork?.let { w -> fileStates?.get(w)?.state }),
            onWork = if (fileApproved) ({ showCaptureSheet = false; pickWork = true }) else null,
            noWork = fileApproved && newWork == null,
            roomLength = roomLength,
            roomWidth = roomWidth,
            roomHeight = roomHeight,
            hasCamera = cam != null,
            cameraConnected = camConnected,
            cameraNote = camNote,
            busy = busy != null,
            onRoomLength = { v ->
                roomLength = v
                capturePrefs.edit().putString("room_len_mm", v).apply()
            },
            onRoomHeight = { v ->
                roomHeight = v
                capturePrefs.edit().putString("room_hgt_mm", v).apply()
            },
            onRoomWidth = { v ->
                roomWidth = v
                capturePrefs.edit().putString("room_wid_mm", v).apply()
            },
            onPick = {
                showCaptureSheet = false
                picker.launch(PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onCamera = {
                cam?.let { c ->
                    if (camConnected) {
                        c.disconnect(); camConnected = false; camNote = null
                    } else {
                        // The SDK calls back on whatever thread it likes;
                        // Compose state has to be written on the main one.
                        c.connect { ok, err ->
                            scopeRoom.launch(Dispatchers.Main) {
                                camConnected = ok; camNote = err
                            }
                        }
                    }
                }
            },
            onShoot = {
                cam?.let { c ->
                showCaptureSheet = false
                busy = { tx -> tx.app_msg_shooting_x3 }
                lastResult = null
                val out = File(context.filesDir, "x3-${System.currentTimeMillis()}.jpg")
                c.shootAndExport(out.path) { result ->
                    scopeRoom.launch(Dispatchers.Main) {
                        busy = null
                        result.fold(
                            onSuccess = { path -> ingest(Uri.fromFile(File(path))) },
                            // a typed refusal renders keyed; an alien exception's words stay as given
                            onFailure = { e -> lastResult = { tx ->
                                tx.app_msg_x3_capture_failed_x(CameraNote.line(tx, e)) } })
                    }
                }
                }
            },
            onTakePhoto = {
                showCaptureSheet = false
                val dir = File(context.filesDir, "shots").apply { mkdirs() }
                val f = File(dir, "shot-${System.currentTimeMillis()}.jpg")
                val u = androidx.core.content.FileProvider.getUriForFile(
                    context, "com.malletcrafts.interiorestimate.fileprovider", f)
                shotUri = u
                runCatching { camera.launch(u) }.onFailure { e ->
                    lastResult = { tx -> tx.app_msg_no_camera_app_x(why = e.message.orEmpty()) }
                }
            },
            onPickPhoto = {
                showCaptureSheet = false
                photoPicker.launch(PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onDismiss = { showCaptureSheet = false })
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { drawerContent() },
    ) {
        Scaffold(
            topBar = {
                Column {
                    TreeTopBar(
                        title = RoomToken.label(tx, roomName),
                        subtitle = listOfNotNull(
                            navProject?.title,
                            tx.pl_capture(roomCaptures.size),
                        ).joinToString(" · "),
                        onMenu = { scopeRoom.launch { drawerState.open() } },
                        onSearch = { showSearch = true; searchQuery = "" })
                    CrumbRail(roomCrumbs)
                }
            },
            bottomBar = {
                BottomBar(tab, unsent) {
                    tab = it; navRoom = null
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showCaptureSheet = true },
                    icon = { Icon(painterResource(R.drawable.ic_mcft_cam),
                                  contentDescription = null) },
                    text = { Text(tx.common_capture_360) },
                    // the survey tour's step 5 lights it, never presses it (port 61c §31)
                    modifier = Modifier.tourAnchor("capture360"))
            },
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                busy?.let {
                    Row(Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.width(18.dp).height(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(it(tx), style = MaterialTheme.typography.bodySmall)
                    }
                }
                lastResult?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                            .clickableRow { lastResult = null },
                    ) {
                        Text(it(tx), Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                // THE CHIP PICK IS THE ROOM'S -- the prototype's `S.capWork=null` on the room
                // crumb's onSibling (index61c2.html:8158). Keyed on the room, the screen and the
                // pick it remembers are rebuilt on a sibling switch. Unkeyed, a pick made in one
                // room filtered the next room's grid to nothing under a bar still counting its
                // captures, with no chip row to clear it when that room held one filing. Leaving
                // the room screen already discards the pick (the composable leaves with it).
                key(roomName) {
                    CapturesScreen(
                        captures = roomCaptures,
                        onDetails = { navCapture = it; navFace = null },
                    ) { c ->
                        // A 360 opens straight into survey prep, the one screen for all six
                        // surfaces (Amit, 2026-10-11); a flat photo opens as before.
                        if (c.kind == "360") openSurveyFor(c.deviceId)
                        else { navCapture = c; navFace = null }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsDialog(initialUrl: String, onDismiss: () -> Unit,
                           onSave: (String, String, String) -> Unit) {
    val tx = LocalTx.current
    var url by remember { mutableStateOf(initialUrl) }
    var key by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tx.app_server) },
        text = {
            Column {
                OutlinedTextField(url, { url = it }, label = { Text(tx.app_site_url) },
                    singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(key, { key = it }, label = { Text(tx.app_api_key) },
                    singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(secret, { secret = it },
                    label = { Text(tx.app_api_secret) }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                // ONE key for the page's whole sentence (ERPNext's own screen names stay as its
                // English screen shows them, the app.generate_both_your_user rule)
                Text(tx.app_generate_both_your_user,
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(url, key, secret) },
                enabled = url.isNotBlank() && key.isNotBlank() && secret.isNotBlank(),
            ) { Text(tx.common_save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tx.common_cancel) } },
    )
}

/**
 * THE SIGN-IN (Amit, 22 Oct 2026: "remove the erp key pair dependency and let S5 be store
 * for apk"): the ERP email and password, exchanged once by S5 for its own session
 * (S5Session). No API key and secret to generate and type any more; that dialog stays
 * behind the "ERP key (legacy)" link for the length of the migration.
 *
 * Signed in, it says who S5 thinks this is and offers Sign out. The sign-in runs off the
 * main thread and the dialog cannot be dismissed while it is out.
 */
@Composable
private fun S5SignInDialog(onDismiss: () -> Unit, onSignedIn: () -> Unit,
                           onSignOut: () -> Unit, onLegacy: () -> Unit) {
    val tx = LocalTx.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var signedIn by remember { mutableStateOf(S5Session.signedIn(context)) }
    var usr by remember { mutableStateOf(S5Session.who(context)?.optString("user").orEmpty()) }
    var pwd by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    // Opened after a refused renewal: why the phone was signed out (S5Session.refusal). ONE string,
    // two sources — a fresh login error or the STORED refusal — so ONE mapper (UploadNote.line)
    // renders it where it is shown; rendering keys directly here would be a second answer.
    var problem by remember { mutableStateOf(S5Session.refusal(context)) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(tx.app_sign_in) },
        text = {
            Column {
                if (signedIn) {
                    val who = S5Session.who(context)
                    val name = who?.optString("full_name")?.ifBlank { null }
                        ?: who?.optString("user").orEmpty()
                    val kind = who?.optString("kind").orEmpty()
                    // the " ({kind})" tail stays a value join: S5's own stored word, data — after
                    // the staff gate it is always "staff"
                    Text(tx.app_signed_in_as_x(name) + if (kind.isNotBlank()) " ($kind)" else "",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(tx.app_captures_go_x(BuildConfig.WEB_URL),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    OutlinedTextField(usr, { usr = it; problem = null },
                        label = { Text(tx.app_erp_email) }, singleLine = true, enabled = !busy,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(pwd, { pwd = it; problem = null },
                        label = { Text(tx.app_password) }, singleLine = true, enabled = !busy,
                        visualTransformation =
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password))
                    Spacer(Modifier.height(8.dp))
                    Text(tx.app_same_email_password_erpnext,
                        style = MaterialTheme.typography.bodySmall)
                    problem?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(UploadNote.line(tx, it), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onLegacy, enabled = !busy) {
                    Text(tx.app_erp_key_legacy, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (signedIn) {
                TextButton(onClick = {
                    onSignOut()
                    signedIn = S5Session.signedIn(context)
                    pwd = ""
                }) { Text(tx.app_sign_out) }
            } else {
                TextButton(
                    enabled = usr.isNotBlank() && pwd.isNotBlank() && !busy,
                    onClick = {
                        busy = true; problem = null
                        val u = usr; val w = pwd
                        scope.launch(Dispatchers.IO) {
                            val err = S5Session.login(context, u, w)
                            withContext(Dispatchers.Main) {
                                busy = false
                                if (err == null) {
                                    signedIn = true
                                    onSignedIn()
                                } else problem = err
                            }
                        }
                    }) { Text(if (busy) tx.app_signing_in else tx.app_sign_in) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(if (signedIn) tx.common_close else tx.common_cancel)
            }
        },
    )
}

/**
 * What a long-press is asking to rename.
 *
 * Carries BOTH identities on purpose: the names, which is how a local row is
 * found in preferences, and the ERP docname, which is how a synced one is
 * found on the server. Which of the two is used is decided by whether
 * serverId is blank — the same test the tree already uses to draw the
 * "offline" pill, so the dialog and the badge can never disagree.
 */
data class RenameTarget(
    val kind: String,          // "client" | "site" | "project"
    val current: String,
    val client: String,
    val site: String,
    val project: String,
    val serverId: String,
    /** Whether the bench's bootstrap named this row. Carried BESIDE the
     *  docname because the pair is what decides a delete: no id and unknown
     *  is a local row, no id but KNOWN is this app failing to match a record
     *  it was sent, and the two must never take the same branch. */
    val serverKnown: Boolean = false,
)

@Composable
private fun NewSiteDialog(
    client: String,
    site: String,
    jobTypes: List<String>,
    siteTypes: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit,
) {
    // Prefilled from wherever the button was pressed. Someone standing at a
    // known client's known flat should be typing ONE field, not four.
    var clientName by remember { mutableStateOf(client) }
    var siteName by remember { mutableStateOf(site) }
    var projectName by remember { mutableStateOf("") }
    var jobType by remember { mutableStateOf(jobTypes.firstOrNull() ?: Catalogue.JOB_NEW) }
    // Amit, 2026-08-22: "site should be selectable like flat, bunglow, shop
    // etc, address should be one more separate field where address of taht
    // site will be keyed in."
    var siteType by remember { mutableStateOf(siteTypes.firstOrNull() ?: "Flat") } // text:value a stored site type
    var address by remember { mutableStateOf("") }
    // Amit, 2026-09-25, from a client's flat: "too much information while
    // creating a client. it can be keyed in later. make this client input
    // form short. the form scrolls vertically too much and not very user
    // friendly."
    //
    // Three fields are all that is needed to START FILING PHOTOS, which is
    // the only thing this dialog is in the way of. Site type, address and job
    // type all have defaults that are right most of the time, none of them
    // changes where a photograph lands, and every one of them can be set
    // later from the desk with a keyboard instead of on a phone in somebody
    // else's flat. They are still here, one tap away, because "later" has to
    // mean "without starting again".
    var showMore by remember { mutableStateOf(false) }

    val tx = LocalTx.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tx.common_new_site) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(clientName, { clientName = it },
                    label = { Text(tx.common_client) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(siteName, { siteName = it },
                    label = { Text(tx.app_site_name) },
                    placeholder = { Text(Masters.site_name(tx, Catalogue.DEFAULT_SITE)) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(projectName, { projectName = it },
                    label = { Text(tx.common_project) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { showMore = !showMore }) {
                    Text(if (showMore) tx.app_fewer_details
                         else tx.app_more_details_type_address)
                }

                if (showMore) {
                    Text(tx.app_site_type, style = MaterialTheme.typography.labelMedium)
                    // Chunked into rows of three rather than a FlowRow: six
                    // types do not fit one line on a phone, a chip pushed off
                    // the edge is a type nobody can choose, and chunking needs
                    // no experimental layout opt-in to say so.
                    siteTypes.chunked(3).forEach { row ->
                        Row(Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { t ->
                                FilterChip(selected = t == siteType,
                                    onClick = { siteType = t },
                                    label = { Text(Masters.site(tx, t), style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(address, { address = it },
                        label = { Text(tx.app_address) },
                        placeholder = { Text(tx.app_flat_building_street_area) },
                        // maxLines only. minLines is Compose 1.4+ and is used
                        // nowhere else in this app; the field grows as it is
                        // typed into, which is enough for an address.
                        maxLines = 4, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    Text(tx.app_job_type, style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        jobTypes.forEach { j ->
                            FilterChip(selected = j == jobType, onClick = { jobType = j },
                                label = { Text(Masters.job(tx, j), style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(tx.app_works_offline_when_phone,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(clientName.trim(),
                        siteName.trim().ifEmpty { Catalogue.DEFAULT_SITE },
                        projectName.trim(), jobType, siteType, address.trim())
                },
                enabled = clientName.isNotBlank() && projectName.isNotBlank(),
            ) { Text(tx.app_use_site) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tx.common_cancel) } },
    )
}


/** A client name in a breadcrumb has ~180dp. "Yogesh S." fits; the full name
 *  does not, and an ellipsis in the middle of a crumb reads as a bug. */
private fun shortName(full: String): String {
    val parts = full.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.size < 2) return full
    return parts[0] + " " + parts.last().first() + "."
}

/** Descend into a site, and through it if that site has exactly one project.
 *  Skipping a level nobody chose from is what keeps four levels walkable. */
private fun openSiteInto(
    cat: Catalogue,
    masters: JSONObject?,
    site: String,
    apply: (String, Catalogue.Project?) -> Unit,
) {
    val client = cat.projects(masters).firstOrNull { it.site.equals(site, true) }?.client
    val ps = if (client == null) emptyList() else cat.projectsOf(masters, client, site)
    apply(site, if (ps.size == 1) ps[0] else null)
}

/** The installed versionName, read from the package rather than from
 *  BuildConfig — the app does not generate BuildConfig (only the compose
 *  build feature is on), and this is the same call SyncWorker already makes
 *  to stamp a capture with the build that took it. */
internal fun appVersion(context: android.content.Context): String =
    runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    }.getOrDefault("?")

/** The drawer holds only what you set once and forget. Anything needed
 *  mid-shoot belongs on the screen, not three lines away. */
/**
 * A pending app update, or null — decided against the RUNNING build every
 * time it is read.
 *
 * Amit, 2026-09-21: "why when i click a client, it shows update available
 * though i could see in version its 0.3.147."
 *
 * The pref was written by SyncWorker when 247 was newer than the 242 on the
 * phone, and only SyncWorker ever cleared it. So installing the update did
 * not remove the notice telling him to install the update: it survived until
 * the next sync happened to run, which on a phone with no signal on site is
 * not a bounded wait. The stored version_code is now compared with the one
 * actually running, here, so the banner disappears the moment the new build
 * starts — and the stale pref is deleted rather than left to be re-judged.
 */
private fun pendingUpdate(
    context: android.content.Context,
): org.json.JSONObject? {
    // The Play build has no installer permission and Play itself is its
    // updater: no drawer row, no install, whatever an old sync stored.
    if (!BuildConfig.SELF_UPDATE) return null
    // Fully qualified, like every other android.content reference in this
    // file: MainActivity.kt imports no android.content types, and the bare
    // name compiled nowhere.
    val prefs = context.getSharedPreferences(
        "capture", android.content.Context.MODE_PRIVATE)
    val raw = prefs.getString("update_available", null) ?: return null
    val info = runCatching { org.json.JSONObject(raw) }.getOrNull()
    val mine = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
    }.getOrDefault(Long.MAX_VALUE)
    if (info == null || info.optInt("version_code").toLong() <= mine) {
        prefs.edit().remove("update_available").apply()
        return null
    }
    return info
}

/**
 * The drawer's groups and lines (design-marathi slice 1b), every word keyed through [tx] with
 * the prototype's own keys (v53build/app.js:215-227). The English is byte-for-byte what the
 * drawer said before, so with the language row hidden nothing on screen changes. Product names
 * and units stay as written, marked `// text:value`.
 */
private fun drawerGroups(
    tx: Tx,
    queued: Int,
    lastSync: String,
    version: String,
    /** Set only when a NEWER build than the running one is waiting. */
    updateVersion: String?,
    onUpdate: () -> Unit,
    cached: String,
    prefs: AppPrefs,
    server: String,
    onSyncNow: () -> Unit,
    onImageMeterSync: () -> Unit,
    onToggle: (String) -> Unit,
    onServer: () -> Unit,
    onSignOut: () -> Unit,
    onLaserTest: () -> Unit,
    /** How many guided tours ship on this build (TourFlows.shipped), and the Help screen. */
    tours: Int,
    onHelp: () -> Unit,
): List<DrawerGroup> = listOf(
    // THE HELP GROUP, FIRST (port 61c §31; ch55: the drawer already scrolls, and a row at its
    // foot starts below the fold). One row, "Help · guided tours · N tours", opening the full
    // Help screen; the count is computed from the shipped flows, the prototype's own
    // `flows().length + ' tours'` — so it reads honestly while flows are still blocked.
    DrawerGroup(tx.common_help, listOf(
        DrawerLine(tx.help_row, value = "$tours${tx.app_tours}",
            icon = R.drawable.ic_mcft_info, onClick = onHelp),
    )),
    DrawerGroup(tx.app_sync, listOf(
        DrawerLine(tx.common_sync_now,
            value = if (queued == 0) lastSync else tx.app_x_waiting(queued.toString()),
            icon = R.drawable.ic_mcft_cloud, onClick = onSyncNow),
        // Real, not decorative: this is the WorkManager constraint the
        // uploader runs under. A 20 MB pano on a site's mobile data is
        // somebody's bill.
        DrawerLine(tx.app_upload_wi_fi_only, toggled = prefs.wifiOnly,
            icon = R.drawable.ic_mcft_wifi, onClick = { onToggle("wifi_only") }),
    )),
    DrawerGroup("ImageMeter", listOf( // text:value product name; the prototype has no key for it either
        // The "Exported-annotations folder" row lived here and is GONE. It
        // was a folder grant for the case where somebody exports an annotated
        // image to an ordinary folder by hand — which nobody does, because
        // ImageMeter's "Show images in gallery" covers the same ground with
        // one tick and no picker. A setting that takes two paragraphs to
        // explain and answers a question nobody asked is clutter, and it cost
        // Amit two questions before it earned its removal.
        // The ONE direction that exists now. The app writes stamped images
        // into Pictures/MCFT Interior Estimate; a person adds them to ImageMeter
        // from the gallery and marks them up; this reads them back by the
        // stamp and files each against the capture and face it names. Amit,
        // 2026-08-22: "a simple import from imagemeter button should simply
        // import the images back to apk as the stamp is genrated by apk and
        // it should know where to put what."
        DrawerLine(tx.app_import_imagemeter, icon = R.drawable.ic_mcft_cloud,
            onClick = onImageMeterSync),
        // "Pull annotated copies" stood here and did NOTHING. It was the
        // Drive pull's switch: read into the prefs object, drawn with a tick,
        // and consulted by no code at all. A toggle that shows a state it does
        // not control is a small lie told every time the drawer opens, and the
        // row directly above this one carries a comment about the last time
        // that happened. Removed with the Drive leg, 2026-09-04.
    )),
    DrawerGroup(tx.app_capture, listOf(
        // Was a dead row for two builds: it displayed the setting and could
        // not change it, while the only real control was buried in the
        // capture sheet. A settings row that shows a value you cannot edit
        // reads as broken.
        // "Field of view" stood here as a picker of three presets. Amit,
        // 2026-09-04: "No need of fov settings in settings of apk as its of
        // no use." Quite right, and for a reason worth keeping: the FOV is
        // not a preference, it is a consequence of the room. It now follows
        // the length and width typed on the capture sheet, where the person
        // is standing in the room and holding the tape.
        // Off means the bench does the split instead. The projection contract
        // in CI is what makes the two agree, so this is a real choice rather
        // than a quality trade.
        DrawerLine(tx.app_split_faces_device, toggled = prefs.splitOnDevice,
            icon = R.drawable.ic_mcft_folder, onClick = { onToggle("split_on_device") }),
        DrawerLine(tx.app_keep_original_360, toggled = prefs.keepOriginal,
            icon = R.drawable.ic_mcft_cube, onClick = { onToggle("keep_original") }),
    )),
    // Proving ground for the Leica DISTO D2 link (Amit, 2026-10-09: "Laser test
    // screen in APK"), until readings land on the Face Prep photo itself.
    DrawerGroup(tx.app_laser, listOf(
        DrawerLine(tx.app_laser_test, value = "DISTO D2", // text:value product name
            icon = R.drawable.ic_mcft_ruler, onClick = onLaserTest),
    )),
    DrawerGroup(tx.app_display, listOfNotNull(
        DrawerLine(tx.app_units, value = if (prefs.imperial) "mm · ft-in" else "mm", // text:value units
            icon = R.drawable.ic_mcft_ruler, onClick = { onToggle("imperial") }),
        // भाषा · Language, straight after Units, exactly as the prototype draws it (v57,
        // index61b.html:7571): the label in BOTH scripts so a person who reads only one of them
        // finds it, the value English / मराठी, no Switch, no sheet, no confirm. One tap flips it;
        // onToggle never closes the drawer, so it stays open showing the new value. SHOWN since
        // Amit's 28 Oct 2026 "Show it now, translate alongside": LangRow.VISIBLE is the one
        // switch, and AppScreen reads the stored choice only under it too.
        if (LangRow.VISIBLE) DrawerLine("भाषा · Language", value = if (prefs.lang == "mr") "मराठी" else "English", // text:value both scripts by design (prototype v57)
            icon = R.drawable.ic_mcft_info, onClick = { onToggle("lang") }) else null,
    )),
    DrawerGroup(tx.app_storage_app, listOfNotNull(
        updateVersion?.let {
            DrawerLine(tx.app_update_available, value = it,
                icon = R.drawable.ic_mcft_cloud, onClick = onUpdate)
        },
        DrawerLine(tx.app_cached_photos, value = cached, icon = R.drawable.ic_mcft_disk,
            onClick = { onToggle("clear_cache") }),
        // The API-key dialog had NO row: onServer was handed to this function
        // and no line ever called it, so the only way in was the search
        // button, which is how a magnifier came to be showing people their
        // server credentials. One route, and it is a settings row.
        DrawerLine(tx.app_server, value = server.ifBlank { tx.common_not_set },
            icon = R.drawable.ic_mcft_link, onClick = onServer),
        DrawerLine(tx.app_version, value = version, icon = R.drawable.ic_mcft_info),
        // THE UPDATE LIVES HERE, next to the version it replaces, and nowhere
        // else. Amit, 2026-09-21: "update app is not a client / project
        // thing. dont show these kind of banner at places where its not
        // relevant." It used to render in the room capture view, so opening a
        // client put an app-maintenance notice on top of his photographs --
        // the one screen whose whole job is the room in front of him. The
        // drawer already carries the version, the server and the cache, so it
        // is where an app-level fact belongs.
        DrawerLine(tx.app_sign_out, icon = R.drawable.ic_mcft_out, onClick = onSignOut),
    )),
)

/**
 * The settings that are set once and then forgotten, and which actually do
 * something. Kept in the same prefs file the FOV picker already uses.
 */
data class AppPrefs(
    val wifiOnly: Boolean,
    val splitOnDevice: Boolean,
    val keepOriginal: Boolean,
    val imperial: Boolean,
    /**
     * The भाषा · Language choice as stored: "mr" or "en", null until first tapped. Read with
     * Tx.of, so only exactly "mr" is Marathi and anything else is English -- the default on first
     * run, whatever the phone's system language (design-marathi §4). Per phone, like Units:
     * never written into a prep, never sent to S5 or ERP.
     */
    val lang: String?,
) {
    companion object {
        fun read(p: android.content.SharedPreferences) = AppPrefs(
            wifiOnly = p.getBoolean("wifi_only", false),
            splitOnDevice = p.getBoolean("split_on_device", true),
            keepOriginal = p.getBoolean("keep_original", true),
            imperial = p.getBoolean("imperial", true),
            lang = p.getString("lang", null))

        fun flip(p: android.content.SharedPreferences, key: String) {
            // The one String pref: Marathi when it was not, else English (the prototype's
            // L57.set(lang==='mr'?'en':'mr')).
            if (key == "lang") {
                p.edit().putString(key, if (p.getString(key, null) == "mr") "en" else "mr").apply()
                return
            }
            val now = when (key) {
                "wifi_only" -> p.getBoolean(key, false)
                "split_on_device", "keep_original", "imperial" ->
                    p.getBoolean(key, true)
                else -> return
            }
            p.edit().putBoolean(key, !now).apply()
        }
    }
}

// ---- what the Work, Queue and Search tabs read ---------------------------

/**
 * Rooms this phone has shot, newest first.
 *
 * Built from the capture QUEUE rather than from the server, because Work has
 * to answer "where was I" on a phone with no signal — which is the only time
 * anyone asks it.
 */
private fun recentRooms(
    cat: Catalogue,
    masters: JSONObject?,
    queue: List<CaptureStore.Capture>,
): List<RecentRoom> {
    val projects = cat.projects(masters)
    val seen = LinkedHashMap<String, RecentRoom>()
    for (c in queue) {                       // store returns newest first
        val key = "${c.projectTitle.lowercase()}|${c.room.lowercase()}"
        val p = projects.firstOrNull { it.title.equals(c.projectTitle, true) }
        val existing = seen[key]
        if (existing != null) {
            seen[key] = existing.copy(count = existing.count + 1)
            continue
        }
        seen[key] = RecentRoom(
            client = p?.client ?: c.customerName,
            site = p?.site ?: Catalogue.DEFAULT_SITE,
            projectKey = p?.key ?: c.projectTitle,
            projectTitle = c.projectTitle,
            room = c.room,
            count = 1,
            panoPath = c.panoPath,
            lastDate = c.captureDate)
    }
    return seen.values.toList()
}

/** Work recorded on site and not yet sent. It belongs in the queue for the
 *  same reason a capture does: the tab answers "did that actually go", and a
 *  thing missing from it is a thing nobody knows is missing. */
private fun skuQueueRow(k: Catalogue.LocalSku) = QueueRow(
    token = RoomToken.of(k.room),
    title = previewCode(k.client, k.room, k.articleCode) + // text:value the SKU code, data
        (k.qty?.let { q ->
            " · " + (if (q % 1.0 == 0.0) q.toInt().toString() else q.toString()) + // text:value the glyph and a figure
            " ${k.basis}" // text:value the wire's basis ("Nos")
        } ?: ""),
    subtitle = listOf(k.articleName, k.projectTitle).filter { it.isNotBlank() }
        .joinToString(" / "), // text:value names and a glyph
    state = "LOCAL")

private fun queueRow(tx: Tx, c: CaptureStore.Capture) = QueueRow(
    token = RoomToken.of(c.room),
    // the room as the masters name it beside the date ("हॉल · 2026-10-10", the served page's own row)
    title = "${Masters.room(tx, c.room)} · ${c.captureDate}", // text:value the glyph and a date
    subtitle = listOf(Masters.client_name(tx, c.customerName), c.projectTitle).filter { it.isNotBlank() }
        .joinToString(" / ") // text:value names and a glyph
        .let { base ->
            val err = c.error
            // the error tail through the stored notes' one mapper (UploadNote.line)
            if (c.state == "ERROR" && !err.isNullOrBlank()) "$base — ${UploadNote.line(tx, err)}" else base // text:value "ERROR" is CaptureStore's stored state, compared, never shown — the pill renders app.retrying
        },
    state = c.state)

/**
 * Search across every level at once, including SKU codes.
 *
 * Case-insensitive contains, except for the room token, which matches from
 * the START — typing "MB" should find Master Bedroom, not every room whose
 * name happens to contain those letters.
 */
private fun searchTree(
    tx: Tx,
    cat: Catalogue,
    masters: JSONObject?,
    query: String,
): List<SearchHit> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    val out = mutableListOf<SearchHit>()
    val projects = cat.projects(masters)

    cat.clients(masters).forEach { c ->
        if (c.lowercase().contains(q)) {
            // the badge stays initials of the stored value, a code; the label through its master
            out.add(SearchHit(initials(c), Masters.client_name(tx, c), tx.common_client, c, "", "", ""))
        }
    }
    projects.forEach { p ->
        if (p.site.lowercase().contains(q) &&
            out.none { it.label == Masters.site_name(tx, p.site) && it.client == p.client }) {
            // the hit type word after the glyph is the noun; the badge is the prototype's own raw literal (the PL ruling)
            out.add(SearchHit("SITE", Masters.site_name(tx, p.site), "${Masters.client_name(tx, p.client)} · ${tx.noun_site(1)}", // text:value the badge
                p.client, p.site, "", ""))
        }
        if (p.title.lowercase().contains(q)) {
            out.add(SearchHit("PRJ", p.title, "${Masters.client_name(tx, p.client)} / ${Masters.site_name(tx, p.site)} · ${Masters.job(tx, p.jobType)}", // text:value the badge and the glyphs
                p.client, p.site, p.key, ""))
        }
        cat.skusOf(masters, p).forEach { sku ->
            if (sku.code.lowercase().contains(q) || sku.article.lowercase().contains(q)) {
                out.add(SearchHit(sku.article.take(3).uppercase().ifBlank { "SKU" }, // text:value the badge, the prototype's own
                    sku.code, "${Masters.article(tx, sku.article)} · ${p.title}", // text:value the glyph and a name
                    p.client, p.site, p.key, sku.room))
            }
        }
        cat.rooms(masters).forEach { r ->
            val tok = RoomToken.of(r).lowercase()
            if (tok.startsWith(q) || r.lowercase().contains(q)) {
                out.add(SearchHit(RoomToken.of(r), Masters.room(tx, r), "${Masters.site_name(tx, p.site)} / ${p.title}", // text:value the glyph and a name
                    p.client, p.site, p.key, r))
            }
        }
    }
    return out.take(30)
}

/** Roughly how much disk the cached annotated copies hold. */
private fun cacheSize(context: android.content.Context, tx: Tx): String {
    val bytes = runCatching {
        context.cacheDir.resolve("ann").walkTopDown()
            .filter { it.isFile }.sumOf { it.length() }
    }.getOrDefault(0L)
    return when {
        bytes <= 0 -> tx.app_empty
        bytes < 1024 * 1024 -> "${bytes / 1024} KB" // text:value a figure and its unit
        else -> String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1024.0 / 1024.0) // text:value a figure and its unit
    }
}

/** A 360's survey prep in one line for its room tile. Never throws: an old
 *  capture with no 360 left on the phone says so instead. */
private fun surveyLine(context: android.content.Context, tx: Tx, c: CaptureStore.Capture): String {
    if (c.kind != "360") return ""
    if (!File(c.panoPath).exists()) return tx.app_360_not_phone_removed
    val p = SurveyPrepStore(context).load(c.deviceId) ?: return tx.app_not_started_tap_start
    val marks = p.surfaces.values.sumOf { it.marks.size }
    // ONE key replaces the two composed fragments: its {pl} folds "mark"/"marks"
    return tx.app_x_6_bounded_x(
        done = SurveyPrep.SURFACES.count { p.surface(it).done() }.toString(),
        marks = marks,
        read = p.actual.size.toString())
}

/** A kept capture's survey prep, opened: its queue row, room title, 360 at preview size, laser L W H in mm. */
private class KeptSurvey(val row: CaptureStore.Capture, val title: String, val pano: Panorama.Image, val size: DoubleArray)

/**
 * A split that has happened but not been accepted.
 *
 * Everything the commit needs travels with it, so approving writes the
 * gallery and the queue without re-deriving a thing — the bytes he looked at
 * are the bytes that land.
 */
data class PendingSplit(
    val session: FaceWriter.Session,
    val projectName: String,
    val projectTitle: String,
    val customerName: String,
    val room: String,
    /** Where it files (FW2): 0 the Quote journey, else the work picked when the shutter fired. */
    val work: Int?,
    val captureDate: String,
    val deviceId: String,
    val fov: Double,
    val plan: com.malletcrafts.interiorestimate.pano.CaptureGeometry.Plan?,
    /** Length and width as typed, mm -- optional, the check on the photo. */
    val lengthMm: Double? = null,
    val widthMm: Double? = null,
)

/**
 * A new 360 goes STRAIGHT to survey prep (Amit, 2026-10-11: "why do i need
 * sliders now for Zoom and turn? ... foto should directly split correctly
 * showing all four corner of each wall and then this 1.line and 2.mark UI
 * should appear"). The old preview with its per-pair zoom and turn sliders is
 * gone: survey prep's own fixed views are the check on the photo.
 *
 * The laser L, W and H are required -- every surface squares to them -- so
 * when the capture form did not hand all three over they are asked for first.
 * Save and Back both KEEP the capture (its faces, the 360, and an elevation
 * for every surface already bounded); only "Discard this 360" drops it.
 */
@Composable
private fun NewCaptureSurvey(
    pending: PendingSplit,
    onDiscard: () -> Unit,
    onKeep: () -> Unit,
) {
    val tx = LocalTx.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val session = pending.session
    val start = listOf(pending.lengthMm, pending.widthMm, session.heightMm)
    var dims by remember {
        mutableStateOf(if (start.all { SurveyPrep.valid(it) }) DoubleArray(3) { start[it]!! } else null)
    }
    val d = dims
    if (d == null) {
        SurveyDimsDialog(
            title = RoomToken.label(tx, pending.room), start = start,
            // the page's own shoot-and-keep labels (the card has held app.discard_360 since L1;
            // §24's "no key yet" claim was stale and §29 corrects it)
            okLabel = tx.common_survey_prep, dismissLabel = tx.app_discard_360,
            onOk = { dims = it }, onDismiss = onDiscard)
        return
    }
    // The size the prep was left at (its size line may have changed it, v51) is what the faces are written
    // at, and what the capture's row is filed with.
    fun keepWith(p: SurveyPrep.Prep, size: DoubleArray) {
        session.survey = p; session.surveySize = size; session.heightMm = size[2]
        onKeep()
    }
    SurveyPrepDialog(
        deviceId = session.deviceId,
        previewPano = session.previewPano,
        title = RoomToken.label(tx, pending.room),
        lengthMm = d[0], widthMm = d[1], heightMm = d[2],
        // the Plan tab's export names its files and fills its title block from these: initials, never the name
        clientInitials = RoomToken.initials(pending.customerName), projectCode = pending.projectName,
        captureDate = pending.captureDate,
        // THE PROJECT'S QUOTE STAGE (port 62 slice L3), by the same rule as a kept capture's: a new site's first survey
        // is a project S5 does not list -- Survey's rules -- and a new room of a listed project takes that project's
        quoteStage = QuoteStageSync.prepStage(context,
            WishSync.stageKey(context, pending.projectName, pending.customerName, pending.projectTitle)),
        // ... and, for the band at Client approved, that project's one derived line (port 62 slice L4)
        projectLine = QuoteStageSync.prepLine(context,
            WishSync.stageKey(context, pending.projectName, pending.customerName, pending.projectTitle), tx),
        onDone = { p, size -> keepWith(p, size) },
        onCancel = { p, size -> keepWith(p, size) },
        onDiscard = onDiscard)
}

/**
 * The laser L, W and H, asked for when a capture does not carry all three:
 * survey prep cannot square a surface without them.
 */
@Composable
private fun SurveyDimsDialog(
    title: String,
    /** What is already known, mm, front->back, left->right, floor->ceiling. */
    start: List<Double?>,
    okLabel: String,
    dismissLabel: String,
    onOk: (DoubleArray) -> Unit,
    onDismiss: () -> Unit,
) {
    val tx = LocalTx.current
    var texts by remember {
        mutableStateOf(start.map { v -> v?.takeIf { SurveyPrep.valid(it) }?.let { Math.round(it).toString() } ?: "" })
    }
    val values = texts.map { it.toDoubleOrNull() }
    val ok = values.all { SurveyPrep.valid(it) }
    AlertDialog(
        onDismissRequest = { },
        title = { Text(tx.app_x_laser_sizes(title = title)) },
        text = {
            Column {
                Text(tx.app_survey_prep_squares_every,
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                val labels = listOf(tx.app_length_front_back, tx.app_width_left_right, tx.app_height_floor_ceiling)
                for (i in 0 until 3) {
                    OutlinedTextField(texts[i], { t ->
                        texts = texts.toMutableList().also { it[i] = t.filter { c -> c.isDigit() }.take(5) } },
                        label = { Text(labels[i]) }, singleLine = true,
                        isError = texts[i].isNotBlank() && !SurveyPrep.valid(values[i]),
                        modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(enabled = ok, onClick = { onOk(DoubleArray(3) { values[it]!! }) }) { Text(okLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } })
}
