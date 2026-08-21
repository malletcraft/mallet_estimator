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
    val annFolder = remember { AnnotationFolder(context) }
    var navClient by remember { mutableStateOf<String?>(null) }
    var navSite by remember { mutableStateOf<String?>(null) }
    var navProject by remember { mutableStateOf<Catalogue.Project?>(null) }
    var navRoom by remember { mutableStateOf<String?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var showNewSite by remember { mutableStateOf(false) }
    var showStagePicker by remember { mutableStateOf(false) }
    // Work | Browse | Queue. Browse is the landing tab: Amit asked for the
    // app to open on Clients, ImageMeter-style.
    var tab by remember { mutableStateOf("browse") }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var prefsTick by remember { mutableStateOf(0) }
    // Capture and face sit BELOW the room, and are plain state rather than
    // crumb levels: six crumbs will not fit a phone, and a photo you are
    // looking at is a view, not a folder.
    var navCapture by remember { mutableStateOf<CaptureCard?>(null) }
    var navFace by remember { mutableStateOf<Int?>(null) }
    var faceMode by remember { mutableStateOf(true) }   // true = annotated
    var showSkus by remember { mutableStateOf(false) }
    var showDetail by remember { mutableStateOf(false) }
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

    // Annotation navigation: a capture opens its face list; a face opens
    // the editor. Plain state instead of a nav library — two levels deep.
    // Nothing sets facesFor today: the in-app annotator is parked behind the
    // ImageMeter round trip (task 33), and this is the hook it comes back
    // through. Kept deliberately rather than deleted and rewritten later.
    val annStore = remember { AnnotationStore(context) }
    var facesFor by remember { mutableStateOf<CaptureStore.Capture?>(null) }
    var annotating by remember { mutableStateOf<Pair<String, String>?>(null) }

    annotating?.let { (devId, face) ->
        AnnotateScreen(deviceId = devId, face = face, store = annStore,
            onBack = { annotating = null })
        return
    }
    facesFor?.let { cap ->
        FacesScreen(capture = cap, annStore = annStore,
            onFace = { face -> annotating = cap.deviceId to face },
            onBack = { facesFor = null })
        return
    }

    fun refreshQueue() {
        queue = store.all()
        updateJson = capturePrefs.getString("update_available", null)
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
            }
            masters = store.masters()
        }
    }

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

    var shotUri by remember { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok: Boolean -> if (ok) shotUri?.let { ingest(it, kind = "Photo") } }

    // The one-time grant that lets the app read ImageMeter's own folder, so a
    // photo annotated on THIS phone is visible immediately instead of after a
    // trip to Drive and back. Android will not let one app browse another's
    // files without the person saying so; this is them saying so.
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            annFolder.link(uri)
            prefsTick++
            scope.launch(Dispatchers.IO) {
                val r = annFolder.scan()
                withContext(Dispatchers.Main) {
                    // Counts, not a tick. A grant that found nothing looks
                    // exactly like one that was never made, and the person
                    // who just picked a folder is the only one who can tell
                    // us it was the wrong one.
                    lastResult = "ImageMeter folder linked: ${r.images} images in " +
                        "${r.folders} folders, ${r.ours} of them ours"
                }
            }
        }
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

    // ---- the folder tree ------------------------------------------------
    if (showNewSite) {
        NewSiteDialog(
            client = navClient.orEmpty(),
            site = navSite.orEmpty(),
            jobTypes = cat.jobTypes(masters),
            onDismiss = { showNewSite = false },
            onSave = { client, site, proj, jobType ->
                cat.addLocal(client, site, proj, jobType = jobType)
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
                queued = queue.count { it.state != "SYNCED" },
                lastSync = if (queue.isEmpty()) "nothing yet" else "all sent",
                fov = roomSizeLabel(roomSize),
                version = appVersion(context),
                cached = cacheSize(context),
                prefs = remember(prefsTick) { AppPrefs.read(capturePrefs) },
                server = FrappeClient.savedUrl(context),
                annotationFolder = remember(prefsTick) { annFolder.label },
                onPickFolder = { folderPicker.launch(null) },
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
                    busy = "Pulling annotations from ImageMeter…"
                    scope.launch(Dispatchers.IO) {
                        val out = runCatching {
                            FrappeClient.load(context)?.imagemeterSync()
                        }
                        withContext(Dispatchers.Main) {
                            busy = null
                            lastResult = out.fold(
                                { r ->
                                    val msg = r?.optJSONObject("message")
                                    when {
                                        msg == null -> "ImageMeter sync queued"
                                        msg.optBoolean("queued") ->
                                            "ImageMeter sync queued — annotations " +
                                            "appear when the job finishes"
                                        else -> "Not synced: " +
                                            msg.optString("skipped", "Drive not configured")
                                    }
                                },
                                { "ImageMeter sync failed: ${it.message}" })
                        }
                    }
                },
                onSyncNow = { SyncWorker.syncNow(context) },
                onServer = { showSettings = true },
            ))
    }

    // ---- search: the escape hatch from four levels ----------------------
    if (showSearch) {
        val hits = remember(searchQuery, masters) {
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
                navProject = cat.projects(masters).firstOrNull { it.key == h.projectKey }
                navRoom = h.room.ifBlank { null }
                navCapture = null; navFace = null
                showSearch = false
            },
            onClose = { showSearch = false })
        return
    }

    // ---- Work and Queue ---------------------------------------------------
    if (tab != "browse") {
        val scopeTab = rememberCoroutineScope()
        val recents = remember(queue) { recentRooms(cat, masters, queue) }
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = { drawerContent() },
        ) {
            Scaffold(
                topBar = {
                    TreeTopBar(
                        title = if (tab == "work") "Work" else "Sync queue",
                        subtitle = if (tab == "work") "Woodugift · site photos"
                                   else plural(queue.count { it.state != "SYNCED" }, "waiting"),
                        onMenu = { scopeTab.launch { drawerState.open() } },
                        onSearch = { showSearch = true; searchQuery = "" })
                },
                bottomBar = {
                    BottomBar(tab, queue.count { it.state != "SYNCED" }) { tab = it }
                },
            ) { pad ->
                Box(Modifier.padding(pad)) {
                    if (tab == "work") {
                        WorkScreen(
                            resume = recents.firstOrNull(),
                            recents = recents.drop(1).take(5),
                            synced = queue.none { it.state != "SYNCED" },
                            queued = queue.count { it.state != "SYNCED" },
                            onOpen = { r ->
                                tab = "browse"
                                navClient = r.client
                                navSite = r.site
                                navProject = cat.projects(masters)
                                    .firstOrNull { it.key == r.projectKey }
                                navRoom = r.room
                                navCapture = null; navFace = null
                            })
                    } else {
                        QueueScreen(
                            waiting = queue.filter { it.state != "SYNCED" }.map { queueRow(it) },
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
            val ss = cat.sitesOf(masters, c)
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
                    val siblings = cat.projectsOf(masters, client ?: "", site ?: "")
                    if (siblings.size == 1) {
                        if (cat.sitesOf(masters, client ?: "").size == 1) {
                            navClient = null; navSite = null
                        } else navSite = null
                        navProject = null
                    } else navProject = null
                }
                site != null ->
                    if (cat.sitesOf(masters, client ?: "").size == 1) {
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
                siblings = cat.clients(masters).map { it to it },
                onUp = { navSite = null; navProject = null },
                onSibling = { openClient(it) }))
            if (client != null && site != null) add(Crumb(
                label = site,
                siblings = cat.sitesOf(masters, client).map { it.name to it.name },
                onUp = { navProject = null },
                onSibling = { navSite = it; navProject = null }))
            if (proj != null) add(Crumb(
                label = proj.title,
                siblings = cat.projectsOf(masters, client ?: "", site ?: "")
                    .map { it.key to it.title },
                onUp = { navProject = null },
                onSibling = { k ->
                    navProject = cat.projectsOf(masters, client ?: "", site ?: "")
                        .firstOrNull { it.key == k }
                }))
        }

        val title: String
        val subtitle: String
        when {
            proj != null -> { title = proj.title; subtitle = "${proj.jobType} · ${site.orEmpty()}" }
            site != null -> { title = site; subtitle = client.orEmpty() }
            client != null -> { title = client
                subtitle = plural(cat.sitesOf(masters, client).size, "site") }
            else -> { title = "Clients"
                subtitle = plural(cat.clients(masters).size, "client") }
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
            SkuSheet(skus = cat.skusOf(masters, proj.serverId),
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
                    BottomBar(tab, queue.count { it.state != "SYNCED" }) { tab = it }
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
                            skuCount = cat.skusOf(masters, proj.serverId).size,
                            onOpen = { navRoom = it },
                            onStage = { showStagePicker = true },
                            onSkus = { showSkus = true },
                            onDetail = { showDetail = true })
                        site != null && client != null -> ProjectsScreen(
                            projects = cat.projectsOf(masters, client, site),
                            captureCount = { p ->
                                queue.count { it.projectTitle.equals(p.title, true) }
                            },
                            onOpen = { navProject = it },
                            onNew = { showNewSite = true })
                        client != null -> SitesScreen(
                            sites = cat.sitesOf(masters, client),
                            projectCount = { st ->
                                cat.projectsOf(masters, client, st.name).size
                            },
                            onOpen = { navSite = it.name },
                            onNew = { showNewSite = true })
                        else -> ClientsScreen(
                            clients = cat.clients(masters),
                            siteCount = { c -> cat.sitesOf(masters, c).size },
                            projectCount = { c ->
                                cat.projects(masters).count { it.client.equals(c, true) }
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
        val faces = remember(cap.deviceId) { LocalFaces.of(context, cap.deviceId) }
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
        LaunchedEffect(cap.deviceId, prefsTick, annTick) {
            runCatching {
                withContext(Dispatchers.IO) {
                    // The gallery first, because it needs no grant and it is
                    // the route that actually works: ImageMeter's data
                    // directory sits under /Android/data, which Android will
                    // not let any app be granted. Its "Show images in
                    // gallery" switch is what puts the annotated copy
                    // somewhere we can legally read it.
                    val gallery = LocalFaces.annotatedOf(context, cap.deviceId)
                    // A granted folder still helps for exports sent somewhere
                    // ordinary, so it fills whatever the gallery did not.
                    val folder = if (annFolder.linked)
                        annFolder.annotatedFor(cap.deviceId) else emptyMap()
                    folder + gallery
                }
            }.onSuccess { annotatedLocal = it }
        }
        LaunchedEffect(cap.deviceId) {
            val server = queue.firstOrNull { it.deviceId == cap.deviceId }?.serverName
            if (server.isNullOrBlank()) return@LaunchedEffect
            runCatching {
                withContext(Dispatchers.IO) {
                    val out = HashMap<String, String>()
                    val c = FrappeClient.load(context) ?: return@withContext out
                    val msg = c.captureDetail(server).optJSONObject("message")
                        ?: return@withContext out
                    val arr = msg.optJSONArray("annotations") ?: return@withContext out
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
                subtitle = "${cap.deviceId} · ${cap.date}" +
                    (if (cap.stage.isNotBlank()) " · ${cap.stage}" else ""),
                source = ThumbSource.Content(f.uri),
                // The local copy wins. It is the same annotation, it is
                // already here, and it is newer than anything the round trip
                // could have brought back.
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
                    cat.skusOf(masters, navProject?.serverId.orEmpty())
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
                skus = cat.skusOf(masters, navProject?.serverId.orEmpty()),
                room = navRoom.orEmpty(),
                current = cap.sku,
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
    val projSkus = cat.skusOf(masters, navProject?.serverId.orEmpty())
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
                BottomBar(tab, queue.count { it.state != "SYNCED" }) {
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
    annotationFolder: String,
    onPickFolder: () -> Unit,
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
        // Optional, and deliberately second. ImageMeter's OWN data directory
        // (/Android/data/de.dirkfarin.imagemeter/files/projects) cannot be
        // granted to anybody — Android 11 removed it from the directory
        // picker and Android 13 shut the last way round. This row is for a
        // folder you EXPORT to, which is an ordinary folder and can be. The
        // switch that matters is inside ImageMeter: Storage → Show images in
        // gallery.
        DrawerLine("Exported-annotations folder", value = annotationFolder,
            icon = R.drawable.ic_mcft_link, onClick = onPickFolder),
        DrawerLine("Pull annotations now", icon = R.drawable.ic_mcft_cloud,
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
        cat.skusOf(masters, p.serverId).forEach { sku ->
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
