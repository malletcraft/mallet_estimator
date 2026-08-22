@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.malletcrafts.sitephotos

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malletcrafts.sitephotos.pano.CaptureGeometry
import com.malletcrafts.sitephotos.pano.Handover
import com.malletcrafts.sitephotos.pano.Panorama
import com.malletcrafts.sitephotos.pano.RoomToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

@Composable
private fun AppScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { CaptureStore(context) }
    val scope = rememberCoroutineScope()

    var masters by remember { mutableStateOf(store.masters()) }
    var configured by remember { mutableStateOf(FrappeClient.configured(context)) }
    var showSettings by remember { mutableStateOf(!FrappeClient.configured(context)) }

    // Where we are in the folder tree: client > SITE > project > room. Null
    // means "not that deep yet", which is also what Back unwinds.
    val cat = remember { Catalogue(context) }
    var navClient by remember { mutableStateOf<String?>(null) }
    var navSite by remember { mutableStateOf<String?>(null) }
    var navProject by remember { mutableStateOf<Catalogue.Project?>(null) }
    var navRoom by remember { mutableStateOf<String?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var showNewSite by remember { mutableStateOf(false) }
    // what to rename: kind, the current name, the project row it came from
    // (which carries the ERP docnames), and whether it lives on the server.
    var renaming by remember {
        mutableStateOf<RenameTarget?>(null)
    }
    var renameBusy by remember { mutableStateOf(false) }
    var showStagePicker by remember { mutableStateOf(false) }
    // Work | Browse | Queue. Browse is the landing tab: Amit asked for the
    // app to open on Clients, ImageMeter-style.
    var tab by remember { mutableStateOf("browse") }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var prefsTick by remember { mutableStateOf(0) }
    // Bumped by every local create/rename/delete. The catalogue's locals live
    // in preferences, which Compose has no way to observe.
    var dataTick by remember { mutableStateOf(0) }
    // Capture and face sit BELOW the room, and are plain state rather than
    // crumb levels: six crumbs will not fit a phone, and a photo you are
    // looking at is a view, not a folder.
    var navCapture by remember { mutableStateOf<CaptureCard?>(null) }
    var navFace by remember { mutableStateOf<Int?>(null) }
    var faceMode by remember { mutableStateOf(true) }   // true = annotated
    var showSkus by remember { mutableStateOf(false) }
    var showDetail by remember { mutableStateOf(false) }
    // Work the site says is needed. Opened from the room's SKU list and from
    // a photo's tag row, because both are moments when somebody notices.
    var addSkuFor by remember { mutableStateOf<String?>(null) }
    // The two pickers that re-file ONE photo, as opposed to moving the whole
    // project. Same stage list, quite different consequence.
    var retagStage by remember { mutableStateOf(false) }
    var retagSku by remember { mutableStateOf(false) }
    var showCaptureSheet by remember { mutableStateOf(false) }
    var showFov by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf("") }
    // Which FOV the split uses. Index into CaptureGeometry.PRESETS, with one
    // extra "server default" entry at the end. Remembered across launches —
    // a photographer doing bathrooms all morning picks Small once.
    val capturePrefs = remember {
        context.getSharedPreferences("capture", android.content.Context.MODE_PRIVATE)
    }
    var roomSize by remember {
        mutableStateOf(capturePrefs.getInt("room_size", 1)
            .coerceIn(0, CaptureGeometry.PRESETS.size))
    }
    var updateJson by remember {
        mutableStateOf(capturePrefs.getString("update_available", null))
    }

    var busy by remember { mutableStateOf<String?>(null) }
    var lastResult by remember { mutableStateOf<String?>(null) }
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
    var annotating by remember { mutableStateOf<Pair<String, String>?>(null) }

    // EVERY branch below returns early, so each one needs its own
    // BackHandler — a branch without one falls through to Android's default,
    // which is "leave the app". That is how back came to close Site Photos
    // from a photograph instead of going up one level.
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
            }
            masters = store.masters()
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

    val rooms = cat.rooms(masters)

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

    // One ingest path for BOTH capture routes: gallery-pick and the direct
    // X3 connection hand the same equirect JPG to the same split + queue.
    fun ingest(uri: Uri, kind: String = "360") {
        val p = newSite?.let { ProjectRow("", it.second, it.first) } ?: project
        val r = room
        if (p == null || r == null) return
        // Nothing chosen means "whatever the project is at", which is right
        // nine times in ten and is exactly what the server would have filled
        // in anyway — writing it locally is what lets the phone SHOW it.
        val stageNow = stage.ifBlank { navProject?.stage.orEmpty() }
        busy = if (kind == "Photo") "Filing the photo…"
               else "Splitting the 360 into six faces…"
        lastResult = null
        scope.launch(Dispatchers.Default) {
            val outcome = runCatching {
                val id = Handover.mintDeviceId(
                    ByteArray(6).also { SecureRandom().nextBytes(it) })
                val today = LocalDate.now().toString()
                val fov = CaptureGeometry.PRESETS.getOrNull(roomSize)?.fov
                    ?: masters?.optDouble("default_fov", Panorama.DEFAULT_FOV)
                    ?: Panorama.DEFAULT_FOV
                val (result, pano) = if (kind == "Photo")
                    FaceWriter.single(
                        context = context, source = uri, deviceId = id,
                        customerName = p.customer, projectTitle = p.title,
                        room = r, captureDate = today, stage = stageNow,
                        panoDir = File(context.filesDir, "panos"))
                else
                    FaceWriter.split(
                        context = context, source = uri, deviceId = id,
                        customerName = p.customer, projectTitle = p.title,
                        room = r, captureDate = today, stage = stageNow, fov = fov,
                        panoDir = File(context.filesDir, "panos"))
                store.insert(CaptureStore.Capture(
                    deviceId = id, project = p.name, projectTitle = p.title,
                    customerName = p.customer, room = r, stage = stageNow,
                    captureDate = today, panoPath = pano.path,
                    createdAt = System.currentTimeMillis(), state = "LOCAL",
                    serverName = null, error = null, kind = kind))
                result
            }
            withContext(Dispatchers.Main) {
                busy = null
                lastResult = outcome.fold(
                    onSuccess = {
                        SyncWorker.syncNow(context)
                        if (kind == "Photo")
                            "Photo filed in ${it.relativePath}"
                        else
                            "${it.faceCount} faces saved to ${it.relativePath}\n" +
                                "Open ImageMeter → the room folder → add photos."
                    },
                    onFailure = { "Could not split: ${it.message}" })
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
        lastResult = if (granted) "Gallery access granted — looking for annotations now."
                     else "Without gallery access the app cannot see what " +
                          "ImageMeter wrote. Settings → Apps → MCFT Site " +
                          "Photos → Permissions → Photos."
        if (granted) SyncWorker.syncNow(context)
    }
    LaunchedEffect(configured) {
        // Asked once, on the way in, so the first annotation someone makes is
        // already findable rather than failing silently and needing a second
        // trip through the drawer.
        if (!StampScan.canRead(context)) askMedia.launch(StampScan.MEDIA_PERMISSION)
    }

    var shotUri by remember { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok: Boolean -> if (ok) shotUri?.let { ingest(it, kind = "Photo") } }

    addSkuFor?.let { skuRoom ->
        val proj = navProject
        AddSkuSheet(
            articles = cat.articles(masters, proj?.jobType),
            client = proj?.client.orEmpty(),
            room = skuRoom,
            onDismiss = { addSkuFor = null },
            onAdd = { art, qty, w, h, d, note ->
                // Minted here, at the tap, exactly as a capture id is: it is
                // what lets the queue retry after a dropped acknowledgement
                // without leaving the project carrying the same wardrobe
                // twice. Two REAL wardrobes get two taps and two ids.
                val id = "msku-" + Handover.mintDeviceId(
                    ByteArray(6).also { SecureRandom().nextBytes(it) })
                    .removePrefix("MCAP-")
                cat.addLocalSku(Catalogue.LocalSku(
                    deviceId = id,
                    client = proj?.client.orEmpty(),
                    projectTitle = proj?.title.orEmpty(),
                    projectId = proj?.serverId.orEmpty(),
                    room = skuRoom,
                    articleCode = art.code,
                    articleName = art.name,
                    basis = art.basis,
                    qty = qty, widthMm = w, heightMm = h, depthMm = d,
                    note = note))
                addSkuFor = null
                reload()
                lastResult = previewCode(proj?.client.orEmpty(), skuRoom, art.code) +
                    " recorded" + (qty?.let { " · $it ${art.basis}" } ?: "")
                SyncWorker.syncNow(context)
            })
    }

    if (showFov) {
        AlertDialog(
            onDismissRequest = { showFov = false },
            title = { Text("Field of view") },
            text = {
                Column {
                    Text("A small room needs a wider face, or the split cuts " +
                         "the walls off at the corners.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    (CaptureGeometry.PRESETS.map { "${it.label} — ${it.fov.toInt()}°" }
                        + "Server default").forEachIndexed { i, label ->
                        Row(Modifier.fillMaxWidth().clickableRow {
                                roomSize = i
                                capturePrefs.edit().putInt("room_size", i).apply()
                                prefsTick++
                                showFov = false
                            }.padding(vertical = 10.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(if (i == roomSize) "●  " else "○  ")
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFov = false }) { Text("Close") }
            })
    }

    if (showSettings) {
        SettingsDialog(
            initialUrl = FrappeClient.savedUrl(context),
            onDismiss = { showSettings = false },
            onSave = { url, key, secret ->
                FrappeClient.save(context, url, key, secret)
                configured = true
                showSettings = false
                SyncWorker.syncNow(context)
            })
    }

    renaming?.let { t ->
        BackHandler { if (!renameBusy) renaming = null }
        RenameDialog(
            what = t.kind, current = t.current, onServer = t.serverId.isNotBlank(),
            busy = renameBusy,
            onDismiss = { renaming = null },
            onSave = { newName ->
                // A local row is words in this phone's preferences, so it is
                // corrected here and now, with no signal. An ERP row is the
                // office's record and has to go to the server — and must not
                // be reported as done until the server says so.
                if (t.serverId.isBlank()) {
                    cat.renameLocal(t.kind, t.client, t.site, t.project, newName)
                    reload()
                    // Follow the rename: a person renaming the thing they are
                    // standing in should not be thrown back to the root.
                    when (t.kind) {
                        "client" -> { navClient = newName; navSite = null; navProject = null }
                        "site" -> { navSite = newName; navProject = null }
                        else -> navProject = null
                    }
                    lastResult = "Renamed to $newName"
                    renaming = null
                } else {
                    renameBusy = true
                    scope.launch(Dispatchers.IO) {
                        val res = runCatching {
                            FrappeClient.load(context)
                                ?.renameNode(t.kind, t.serverId, newName)
                                ?: error("no server configured")
                            // Pull the masters back immediately rather than
                            // waiting for the next sync: the rule is that a
                            // change is visible at once, and a rename that
                            // needs a sync to appear is the same bug again.
                            FrappeClient.load(context)?.bootstrap()?.let {
                                store.saveMasters(it)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            renameBusy = false
                            reload()
                            res.onSuccess {
                                when (t.kind) {
                                    "client" -> { navClient = newName; navSite = null; navProject = null }
                                    "site" -> { navSite = newName; navProject = null }
                                    else -> navProject = null
                                }
                                lastResult = "Renamed to $newName"
                                renaming = null
                            }.onFailure {
                                lastResult = "Could not rename: ${it.message}"
                            }
                        }
                    }
                }
            })
        return
    }

    // ---- the folder tree ------------------------------------------------
    if (showNewSite) {
        NewSiteDialog(
            client = navClient.orEmpty(),
            site = navSite.orEmpty(),
            jobTypes = cat.jobTypes(masters),
            onDismiss = { showNewSite = false },
            onSave = { client, site, proj, jobType ->
                cat.addLocal(client, site, proj, jobType = jobType)
                reload()          // the rule: it is on screen before this returns
                showNewSite = false
                navClient = client
                navSite = site
                navProject = Catalogue.Project(
                    client = client, site = site, siteId = "", title = proj,
                    serverId = "", jobType = jobType, stage = "", local = true)
            })
    }

    // One drawer, shared by every tab: the settings are the app's, not a
    // screen's, and building it twice would let the two drift.
    val drawerContent: @Composable () -> Unit = {
        SettingsDrawerContent(
            user = FrappeClient.savedUrl(context).ifBlank { "Site Photos" },
            server = FrappeClient.savedUrl(context),
            groups = drawerGroups(
                queued = unsent,
                lastSync = if (queue.isEmpty()) "nothing yet" else "all sent",
                fov = roomSizeLabel(roomSize),
                version = appVersion(context),
                cached = cacheSize(context),
                prefs = remember(prefsTick) { AppPrefs.read(capturePrefs) },
                server = FrappeClient.savedUrl(context),
                onPickFov = { showFov = true },
                onToggle = { key ->
                    if (key == "clear_cache") {
                        context.cacheDir.resolve("ann").deleteRecursively()
                        lastResult = "Cached annotated copies cleared"
                    } else {
                        AppPrefs.flip(capturePrefs, key)
                        // The Wi-Fi choice is a WorkManager constraint,
                        // so the worker has to be re-scheduled to pick
                        // it up — a toggle that only changes a boolean
                        // would be decoration.
                        if (key == "wifi_only") SyncWorker.schedule(context)
                    }
                    prefsTick++
                },
                onSignOut = {
                    FrappeClient.forget(context)
                    configured = false
                    showSettings = true
                },
                onImageMeterSync = {
                    busy = "Looking for annotated photos…"
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
                        val localLine = local?.let { (scan, sent, waiting) ->
                            when {
                                !scan.allowed ->
                                    "Android is hiding other apps' photos from " +
                                    "this app — grant Photos access and press " +
                                    "this again."
                                sent > 0 -> "Sent $sent annotated " +
                                    (if (sent == 1) "face" else "faces") + " from the gallery."
                                scan.stamped > 0 && waiting > 0 ->
                                    "${scan.stamped} annotated, but their captures " +
                                    "have not reached the server yet — sync first."
                                scan.stamped > 0 ->
                                    "${scan.stamped} annotated already sent, nothing new."
                                scan.looked == 0 ->
                                    "No photos in the gallery to look at."
                                else ->
                                    "Looked at ${scan.looked} gallery photos, none " +
                                    "carrying this app's stamp. Only faces captured " +
                                    "with 0.3.93 or later are stamped — re-shoot and " +
                                    "annotate that one."
                            }
                        } ?: "Could not read the gallery."
                        // Queue it, then WAIT and read what it did. "Queued"
                        // on its own says the button worked, not that
                        // anything came back — which is exactly what somebody
                        // sees when Drive is not wired: a cheerful message
                        // and no change on any photograph.
                        val out = runCatching {
                            val c = FrappeClient.load(context)
                            val q = c?.imagemeterSync()
                            // Terse on purpose — this is now a SUFFIX to the
                            // gallery line, not the answer. Drive is the slow
                            // secondary path and is expected to find nothing.
                            if (q?.optBoolean("queued") != true) {
                                q?.optString("skipped") ?: "no server configured"
                            } else {
                                Thread.sleep(6000)
                                val st = c.imagemeterStatus()
                                when {
                                    !st.optBoolean("configured") -> "no folder set"
                                    st.optInt("pulled") > 0 ->
                                        "${st.optInt("pulled")} pulled"
                                    st.optInt("unmatched") > 0 ->
                                        "${st.optInt("unmatched")} unmatched"
                                    else -> "nothing new"
                                }
                            }
                        }
                        withContext(Dispatchers.Main) {
                            busy = null
                            lastResult = localLine + "  •  Drive: " + out.getOrElse {
                                "sync failed: ${it.message}"
                            }
                        }
                    }
                },
                onSyncNow = { SyncWorker.syncNow(context) },
                onServer = { showSettings = true },
            ))
    }

    // ---- search: the escape hatch from four levels ----------------------
    if (showSearch) {
        BackHandler { showSearch = false }
        val hits = remember(searchQuery, masters, dataTick) {
            searchTree(cat, masters, searchQuery)
        }
        SearchOverlay(
            query = searchQuery,
            hits = hits,
            recents = listOf("MB", "KIT", "Kothrud"),
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
                        title = if (tab == "work") "Work" else "Sync queue",
                        subtitle = if (tab == "work") "Woodugift · site photos"
                                   else plural(unsent, "waiting"),
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
                            waiting = queue.filter { it.state != "SYNCED" }.map { queueRow(it) } +
                                cat.localSkus().map { skuQueueRow(it) },
                            sent = queue.filter { it.state == "SYNCED" }.take(12).map { queueRow(it) },
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

        val crumbs = buildList {
            add(Crumb(
                label = "Clients",
                onUp = { navClient = null; navSite = null; navProject = null }))
            if (client != null) add(Crumb(
                label = shortName(client),
                siblings = cs.clients.map { it to it },
                onUp = { navSite = null; navProject = null },
                onSibling = { openClient(it) }))
            if (client != null && site != null) add(Crumb(
                label = site,
                siblings = cs.sitesOf(client).map { it.name to it.name },
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
            proj != null -> { title = proj.title; subtitle = "${proj.jobType} · ${site.orEmpty()}" }
            site != null -> { title = site; subtitle = client.orEmpty() }
            client != null -> { title = client
                subtitle = plural(cs.siteCount(client), "site") }
            else -> { title = "Clients"
                subtitle = plural(cs.clients.size, "client") }
        }

        if (showStagePicker && proj != null) {
            StageSheet(
                stages = cat.stages(masters, proj.jobType),
                allStages = cat.stages(masters),
                current = proj.stage,
                jobType = proj.jobType,
                onDismiss = { showStagePicker = false },
                onPick = { st ->
                    showStagePicker = false
                    // Optimistic locally, authoritative on the server: the
                    // grid must repaint now, on a phone that may have no
                    // signal, and the sync is what makes it true.
                    navProject = proj.copy(stage = st.name)
                    if (proj.synced) scope.launch(Dispatchers.IO) {
                        runCatching {
                            FrappeClient.load(context)
                                ?.setProjectStage(proj.serverId, st.name)
                        }
                    }
                })
        }
        if (showSkus && proj != null) {
            SkuSheet(skus = remember(masters, dataTick, proj) {
                        cat.skusOf(masters, proj) },
                onAdd = {
                    showSkus = false
                    // From the project level there is no room in hand yet, so
                    // it opens on the first room that has been photographed —
                    // the one somebody is most likely standing in.
                    addSkuFor = rooms.firstOrNull { r ->
                        queue.any { it.room == r &&
                            it.projectTitle.equals(proj.title, true) }
                    } ?: rooms.firstOrNull().orEmpty()
                },
                onDismiss = { showSkus = false })
        }
        if (showDetail && proj != null) {
            val all = cat.stages(masters, proj.jobType)
            ProjectSheet(
                project = proj,
                phase = all.firstOrNull { it.name == proj.stage }?.phase.orEmpty(),
                phases = all.map { it.phase }.distinct(),
                stageCount = all.size,
                photos = queue.count { it.projectTitle.equals(proj.title, true) },
                onDismiss = { showDetail = false })
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
                    if (!configured) {
                        Banner("Set the server and API key in Settings to begin.")
                    } else if (masters == null) {
                        Banner("Waiting for the first master list — go online once.")
                    }
                    when {
                        proj != null -> RoomsScreen(
                            project = proj, rooms = rooms,
                            captureCount = { r ->
                                queue.count { it.room == r &&
                                    it.projectTitle.equals(proj.title, true) }
                            },
                            shotAtStage = { r ->
                                proj.stage.isBlank() || queue.any {
                                    it.room == r &&
                                    it.projectTitle.equals(proj.title, true) &&
                                    it.stage.equals(proj.stage, true)
                                }
                            },
                            // The newest 360 in that room, which is already on
                            // this phone — so the grid fills with no network.
                            lastPano = { r ->
                                queue.firstOrNull {
                                    it.room == r &&
                                    it.projectTitle.equals(proj.title, true)
                                }?.panoPath
                            },
                            skuCount = cat.skusOf(masters, proj).size,
                            onOpen = { navRoom = it },
                            onStage = { showStagePicker = true },
                            onSkus = { showSkus = true },
                            onDetail = { showDetail = true })
                        site != null && client != null -> ProjectsScreen(
                            onRename = { p -> renaming = RenameTarget(
                                "project", p.title, p.client, p.site, p.title,
                                p.serverId) },
                            projects = cs.projectsOf(client, site),
                            captureCount = { p ->
                                queue.count { it.projectTitle.equals(p.title, true) }
                            },
                            onOpen = { navProject = it },
                            onNew = { showNewSite = true })
                        client != null -> SitesScreen(
                            onRename = { st -> renaming = RenameTarget(
                                "site", st.name, client, st.name, "", st.serverId) },
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
                if (n > 0) lastResult = "Sent $n annotated " +
                    (if (n == 1) "face" else "faces") + " to $server"
            }.onFailure {
                lastResult = "Could not send the annotation: ${it.message}"
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

        val faceIdx = navFace
        if (faceIdx != null && faceIdx in faces.indices) {
            val f = faces[faceIdx]
            FaceViewer(
                title = f.name.replaceFirstChar { it.uppercase() },
                subtitle = (if (cap.sku.isNotBlank()) "${cap.sku} · " else "") +
                    "${cap.deviceId} · ${cap.date}" +
                    (if (cap.stage.isNotBlank()) " · ${cap.stage}" else ""),
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
                onEditInImageMeter = {
                    // Hand the face to whatever can annotate it. ImageMeter
                    // registers for image/*, so the chooser lands on it.
                    runCatching {
                        context.startActivity(android.content.Intent.createChooser(
                            android.content.Intent(android.content.Intent.ACTION_VIEW)
                                .setDataAndType(f.uri, "image/jpeg")
                                .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION),
                            "Annotate with"))
                    }.onFailure { lastResult = "No app to open the face: ${it.message}" }
                },
                faces = faces,
                current = faceIdx,
                onPickFace = { navFace = it },
                onClose = { navFace = null })
            return
        }

        Scaffold(topBar = {
            TreeTopBar(
                title = "${cap.date} · ${cap.stage.ifBlank { "no stage" }}",
                subtitle = RoomToken.label(navRoom ?: "") + " · " + cap.deviceId,
                // The magnifier used to open the API-KEY dialog from here.
                // That is the worst thing a button can do: a search icon that
                // shows somebody your server credentials. It searches, and
                // the lead icon is Back, because this screen is a view of one
                // photograph and not a level of the tree.
                onSearch = { showSearch = true; searchQuery = "" },
                onBack = { navCapture = null })
        }) { pad ->
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
                    onOpenFace = { navFace = it; faceMode = true },
                    // Allowed, and written down. A hard block produces the
                    // worse failure: a photo permanently mis-staged because
                    // the only person who noticed cannot fix it.
                    onPickStage = { retagStage = true },
                    onPickSku = { retagSku = true },
                    onDelete = {
                        // The row and the files, then back to the room. Files
                        // first would leave a queue row pointing at nothing if
                        // the delete were interrupted; the row first leaves an
                        // orphan file, which costs disk and nothing else.
                        val q = queue.firstOrNull { it.deviceId == cap.deviceId }
                        store.delete(cap.deviceId)
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
                        lastResult = if (q?.state == "SYNCED")
                            "Removed from this phone. The server copy stays — " +
                            "delete it in ERPNext if you meant that too."
                        else "Capture deleted"
                    })
            }
        }

        // Re-filing ONE photo. Both write locally first and to the bench
        // second: the correction has to survive a basement, and the row on
        // screen has to change the instant it is made or nobody trusts it.
        val serverName = queue.firstOrNull { it.deviceId == cap.deviceId }?.serverName
        fun retag(newStage: String?, newSku: String?) {
            store.setTags(cap.deviceId, stage = newStage, sku = newSku)
            refreshQueue()
            // The column holds the Estimate SKU DOCNAME; the card shows the
            // code. Resolving here keeps the two from drifting the moment a
            // tag is changed.
            val shownSku = newSku?.let { n ->
                if (n.isBlank()) "" else
                    cat.skusOf(masters, navProject)
                        .firstOrNull { it.name == n }?.code ?: n
            }
            navCapture = cap.copy(
                workStage = newStage ?: cap.workStage,
                stage = newStage?.let { cat.phaseOfStage(masters, it).ifBlank { it } }
                    ?: cap.stage,
                sku = shownSku ?: cap.sku)
            if (!serverName.isNullOrBlank()) scope.launch(Dispatchers.IO) {
                val out = runCatching {
                    FrappeClient.load(context)
                        ?.setCaptureTags(serverName, workStage = newStage, sku = newSku)
                }
                withContext(Dispatchers.Main) {
                    out.onFailure { lastResult = "Could not re-file on the bench: ${it.message}" }
                }
            }
        }
        if (retagStage) {
            val job = navProject?.jobType ?: Catalogue.JOB_NEW
            StageSheet(
                stages = cat.stages(masters, job),
                allStages = cat.stages(masters),
                current = cap.workStage,
                jobType = job,
                heading = "Stage of this photo",
                onDismiss = { retagStage = false },
                onPick = { st -> retagStage = false; retag(st.name, null) })
        }
        if (retagSku) {
            CaptureSkuSheet(
                skus = cat.skusOf(masters, navProject),
                room = navRoom.orEmpty(),
                current = cap.sku,
                onAdd = { retagSku = false; addSkuFor = navRoom.orEmpty() },
                onDismiss = { retagSku = false },
                onPick = { code -> retagSku = false; retag(null, code) })
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
    var camNote by remember { mutableStateOf<String?>(null) }
    val projSkus = cat.skusOf(masters, navProject)
    val roomCaptures = queue.filter {
        it.room == roomName &&
            it.projectTitle.equals(navProject?.title ?: "", true)
    }.map { c ->
        CaptureCard(
            deviceId = c.deviceId,
            date = c.captureDate,
            // The card wears the PHASE — ten words the chip row can filter on
            // — and remembers the stage behind it for the detail screen.
            stage = cat.phaseOfStage(masters, c.stage).ifBlank { c.stage },
            workStage = c.stage,
            panoPath = c.panoPath,
            state = c.state,
            kind = c.kind,
            sku = projSkus.firstOrNull { it.name == c.sku }?.code ?: c.sku)
    }

    BackHandler { navRoom = null }

    val roomCrumbs = buildList {
        add(Crumb(label = "Clients", onUp = {
            navClient = null; navSite = null; navProject = null; navRoom = null
        }))
        navClient?.let { c ->
            add(Crumb(label = shortName(c),
                onUp = { navSite = null; navProject = null; navRoom = null }))
        }
        navSite?.let { st ->
            add(Crumb(label = st, onUp = { navProject = null; navRoom = null }))
        }
        navProject?.let { p ->
            add(Crumb(label = p.title, onUp = { navRoom = null }))
        }
        add(Crumb(
            label = RoomToken.of(roomName),
            siblings = rooms.map { it to it },
            onUp = { },
            onSibling = { navRoom = it }))
    }

    if (showStagePicker && navProject != null) {
        val p = navProject!!
        StageSheet(
            stages = cat.stages(masters, p.jobType),
            allStages = cat.stages(masters),
            current = stage.ifBlank { p.stage },
            jobType = p.jobType,
            heading = "Stage for new photos",
            onDismiss = { showStagePicker = false },
            onPick = { st -> stage = st.name; showStagePicker = false })
    }

    if (showCaptureSheet) {
        CaptureSheet(
            stage = stage.ifBlank { navProject?.stage.orEmpty() },
            roomSize = roomSize,
            hasCamera = cam != null,
            cameraConnected = camConnected,
            cameraNote = camNote,
            busy = busy != null,
            onStage = { showCaptureSheet = false; showStagePicker = true },
            onRoomSize = { i ->
                roomSize = i
                capturePrefs.edit().putInt("room_size", i).apply()
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
                busy = "Shooting on the X3…"
                lastResult = null
                val out = File(context.filesDir, "x3-${System.currentTimeMillis()}.jpg")
                c.shootAndExport(out.path) { result ->
                    scopeRoom.launch(Dispatchers.Main) {
                        busy = null
                        result.fold(
                            onSuccess = { path -> ingest(Uri.fromFile(File(path))) },
                            onFailure = { lastResult = "X3 capture failed: ${it.message}" })
                    }
                }
                }
            },
            onTakePhoto = {
                showCaptureSheet = false
                val dir = File(context.filesDir, "shots").apply { mkdirs() }
                val f = File(dir, "shot-${System.currentTimeMillis()}.jpg")
                val u = androidx.core.content.FileProvider.getUriForFile(
                    context, "com.malletcrafts.sitephotos.fileprovider", f)
                shotUri = u
                runCatching { camera.launch(u) }.onFailure {
                    lastResult = "No camera app answered: ${it.message}"
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
                        title = RoomToken.label(roomName),
                        subtitle = listOfNotNull(
                            navProject?.title,
                            plural(roomCaptures.size, "capture"),
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
                    text = { Text("Capture 360") })
            },
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                busy?.let {
                    Row(Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.width(18.dp).height(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
                lastResult?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                            .clickableRow { lastResult = null },
                    ) {
                        Text(it, Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                updateJson?.let { uj ->
                    val info = org.json.JSONObject(uj)
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.fillMaxWidth().clickableRow {
                            busy = "Downloading update ${info.optString("version_name")}…"
                            scopeRoom.launch(Dispatchers.IO) {
                                val outcome = runCatching {
                                    val dest = File(context.filesDir, "updates/update.apk")
                                    FrappeClient.load(context)!!.downloadPrivate(
                                        info.getString("file_url"), dest)
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context, "com.malletcrafts.sitephotos.fileprovider", dest)
                                    context.startActivity(
                                        android.content.Intent(android.content.Intent.ACTION_VIEW)
                                            .setDataAndType(uri,
                                                "application/vnd.android.package-archive")
                                            .addFlags(
                                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                    or android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                                }
                                withContext(Dispatchers.Main) {
                                    busy = null
                                    outcome.onFailure {
                                        lastResult = "Update failed: ${it.message}"
                                    }
                                }
                            }
                        },
                    ) {
                        Text("Update available: ${info.optString("version_name")} — " +
                             "tap to download and install",
                            Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                CapturesScreen(
                    captures = roomCaptures,
                    phases = cat.phases(masters, navProject?.jobType),
                    stageOrder = { cat.stageOrder(masters, it) },
                ) { navCapture = it; navFace = null }
            }
        }
    }
}

@Composable
private fun SettingsDialog(initialUrl: String, onDismiss: () -> Unit,
                           onSave: (String, String, String) -> Unit) {
    var url by remember { mutableStateOf(initialUrl) }
    var key by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Server") },
        text = {
            Column {
                OutlinedTextField(url, { url = it }, label = { Text("Site URL") },
                    singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(key, { key = it }, label = { Text("API key") },
                    singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(secret, { secret = it },
                    label = { Text("API secret") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Text("Generate both on your User page in ERPNext " +
                    "(Settings → API Access). The secret is shown once.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(url, key, secret) },
                enabled = url.isNotBlank() && key.isNotBlank() && secret.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
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
)

@Composable
private fun NewSiteDialog(
    client: String,
    site: String,
    jobTypes: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
) {
    // Prefilled from wherever the button was pressed. Someone standing at a
    // known client's known flat should be typing ONE field, not four.
    var clientName by remember { mutableStateOf(client) }
    var siteName by remember { mutableStateOf(site) }
    var projectName by remember { mutableStateOf("") }
    var jobType by remember { mutableStateOf(jobTypes.firstOrNull() ?: Catalogue.JOB_NEW) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New site") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(clientName, { clientName = it },
                    label = { Text("Client") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(siteName, { siteName = it },
                    label = { Text("Site — the flat or bungalow") },
                    placeholder = { Text(Catalogue.DEFAULT_SITE) }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(projectName, { projectName = it },
                    label = { Text("Project") }, singleLine = true)
                Spacer(Modifier.height(10.dp))
                Text("Job type", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    jobTypes.forEach { j ->
                        FilterChip(selected = j == jobType, onClick = { jobType = j },
                            label = { Text(j, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Works offline. When the phone syncs, these become the " +
                    "real client, site and project in ERPNext — or match ones " +
                    "that already exist, however the names were spelled.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(clientName.trim(),
                        siteName.trim().ifEmpty { Catalogue.DEFAULT_SITE },
                        projectName.trim(), jobType)
                },
                enabled = clientName.isNotBlank() && projectName.isNotBlank(),
            ) { Text("Use this site") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
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
private fun appVersion(context: android.content.Context): String =
    runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    }.getOrDefault("?")

private fun roomSizeLabel(index: Int): String =
    CaptureGeometry.PRESETS.getOrNull(index)?.let { "${it.fov.toInt()}°" }
        ?: "server default"

/** The drawer holds only what you set once and forget. Anything needed
 *  mid-shoot belongs on the screen, not three lines away. */
private fun drawerGroups(
    queued: Int,
    lastSync: String,
    fov: String,
    version: String,
    cached: String,
    prefs: AppPrefs,
    server: String,
    onPickFov: () -> Unit,
    onSyncNow: () -> Unit,
    onImageMeterSync: () -> Unit,
    onToggle: (String) -> Unit,
    onServer: () -> Unit,
    onSignOut: () -> Unit,
): List<DrawerGroup> = listOf(
    DrawerGroup("Sync", listOf(
        DrawerLine("Sync now",
            value = if (queued == 0) lastSync else "$queued waiting",
            icon = R.drawable.ic_mcft_cloud, onClick = onSyncNow),
        // Real, not decorative: this is the WorkManager constraint the
        // uploader runs under. A 20 MB pano on a site's mobile data is
        // somebody's bill.
        DrawerLine("Upload on Wi-Fi only", toggled = prefs.wifiOnly,
            icon = R.drawable.ic_mcft_wifi, onClick = { onToggle("wifi_only") }),
    )),
    DrawerGroup("ImageMeter", listOf(
        // The "Exported-annotations folder" row lived here and is GONE. It
        // was a folder grant for the case where somebody exports an annotated
        // image to an ordinary folder by hand — which nobody does, because
        // ImageMeter's "Show images in gallery" covers the same ground with
        // one tick and no picker. A setting that takes two paragraphs to
        // explain and answers a question nobody asked is clutter, and it cost
        // Amit two questions before it earned its removal.
        // Named for what it does now. "Pull" described the Drive round trip,
        // which is the half that cannot identify anything; the gallery on this
        // phone is where an annotation is actually found.
        DrawerLine("Find annotated photos", icon = R.drawable.ic_mcft_cloud,
            onClick = onImageMeterSync),
        DrawerLine("Pull annotated copies", toggled = prefs.pullAnnotated,
            icon = R.drawable.ic_mcft_pen, onClick = { onToggle("pull_annotated") }),
    )),
    DrawerGroup("Capture", listOf(
        // Was a dead row for two builds: it displayed the setting and could
        // not change it, while the only real control was buried in the
        // capture sheet. A settings row that shows a value you cannot edit
        // reads as broken.
        DrawerLine("Field of view", value = fov, icon = R.drawable.ic_mcft_cam,
            onClick = onPickFov),
        // Off means the bench does the split instead. The projection contract
        // in CI is what makes the two agree, so this is a real choice rather
        // than a quality trade.
        DrawerLine("Split faces on device", toggled = prefs.splitOnDevice,
            icon = R.drawable.ic_mcft_folder, onClick = { onToggle("split_on_device") }),
        DrawerLine("Keep the original 360", toggled = prefs.keepOriginal,
            icon = R.drawable.ic_mcft_cube, onClick = { onToggle("keep_original") }),
    )),
    DrawerGroup("Display", listOf(
        DrawerLine("Units", value = if (prefs.imperial) "mm · ft-in" else "mm",
            icon = R.drawable.ic_mcft_ruler, onClick = { onToggle("imperial") }),
    )),
    DrawerGroup("Storage & app", listOf(
        DrawerLine("Cached photos", value = cached, icon = R.drawable.ic_mcft_disk,
            onClick = { onToggle("clear_cache") }),
        // The API-key dialog had NO row: onServer was handed to this function
        // and no line ever called it, so the only way in was the search
        // button, which is how a magnifier came to be showing people their
        // server credentials. One route, and it is a settings row.
        DrawerLine("Server", value = server.ifBlank { "not set" },
            icon = R.drawable.ic_mcft_link, onClick = onServer),
        DrawerLine("Version", value = version, icon = R.drawable.ic_mcft_info),
        DrawerLine("Sign out", icon = R.drawable.ic_mcft_out, onClick = onSignOut),
    )),
)

/**
 * The settings that are set once and then forgotten, and which actually do
 * something. Kept in the same prefs file the FOV picker already uses.
 */
data class AppPrefs(
    val wifiOnly: Boolean,
    val pullAnnotated: Boolean,
    val splitOnDevice: Boolean,
    val keepOriginal: Boolean,
    val imperial: Boolean,
) {
    companion object {
        fun read(p: android.content.SharedPreferences) = AppPrefs(
            wifiOnly = p.getBoolean("wifi_only", false),
            pullAnnotated = p.getBoolean("pull_annotated", true),
            splitOnDevice = p.getBoolean("split_on_device", true),
            keepOriginal = p.getBoolean("keep_original", true),
            imperial = p.getBoolean("imperial", true))

        fun flip(p: android.content.SharedPreferences, key: String) {
            val now = when (key) {
                "wifi_only" -> p.getBoolean(key, false)
                "pull_annotated", "split_on_device", "keep_original", "imperial" ->
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
    title = previewCode(k.client, k.room, k.articleCode) +
        (k.qty?.let { q ->
            " · " + (if (q % 1.0 == 0.0) q.toInt().toString() else q.toString()) +
            " ${k.basis}"
        } ?: ""),
    subtitle = listOf(k.articleName, k.projectTitle).filter { it.isNotBlank() }
        .joinToString(" / "),
    state = "LOCAL")

private fun queueRow(c: CaptureStore.Capture) = QueueRow(
    token = RoomToken.of(c.room),
    title = "${c.room} · ${c.captureDate}",
    subtitle = listOf(c.customerName, c.projectTitle).filter { it.isNotBlank() }
        .joinToString(" / ")
        .let { if (c.state == "ERROR" && !c.error.isNullOrBlank()) "$it — ${c.error}" else it },
    state = c.state)

/**
 * Search across every level at once, including SKU codes.
 *
 * Case-insensitive contains, except for the room token, which matches from
 * the START — typing "MB" should find Master Bedroom, not every room whose
 * name happens to contain those letters.
 */
private fun searchTree(
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
            out.add(SearchHit(initials(c), c, "Client", c, "", "", ""))
        }
    }
    projects.forEach { p ->
        if (p.site.lowercase().contains(q) &&
            out.none { it.label == p.site && it.client == p.client }) {
            out.add(SearchHit("SITE", p.site, "${p.client} · site",
                p.client, p.site, "", ""))
        }
        if (p.title.lowercase().contains(q)) {
            out.add(SearchHit("PRJ", p.title, "${p.client} / ${p.site} · ${p.jobType}",
                p.client, p.site, p.key, ""))
        }
        cat.skusOf(masters, p).forEach { sku ->
            if (sku.code.lowercase().contains(q) || sku.article.lowercase().contains(q)) {
                out.add(SearchHit(sku.article.take(3).uppercase().ifBlank { "SKU" },
                    sku.code, "${sku.article} · ${p.title}",
                    p.client, p.site, p.key, sku.room))
            }
        }
        cat.rooms(masters).forEach { r ->
            val tok = RoomToken.of(r).lowercase()
            if (tok.startsWith(q) || r.lowercase().contains(q)) {
                out.add(SearchHit(RoomToken.of(r), r, "${p.site} / ${p.title}",
                    p.client, p.site, p.key, r))
            }
        }
    }
    return out.take(30)
}

/** Roughly how much disk the cached annotated copies hold. */
private fun cacheSize(context: android.content.Context): String {
    val bytes = runCatching {
        context.cacheDir.resolve("ann").walkTopDown()
            .filter { it.isFile }.sumOf { it.length() }
    }.getOrDefault(0L)
    return when {
        bytes <= 0 -> "empty"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
    }
}
