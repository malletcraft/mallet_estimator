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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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

private fun projects(masters: JSONObject?): List<ProjectRow> {
    val arr = masters?.optJSONArray("projects") ?: return emptyList()
    return (0 until arr.length()).map { i ->
        val p = arr.getJSONObject(i)
        ProjectRow(p.getString("project"), p.optString("title"),
            p.optString("customer_name"))
    }
}

private fun strings(masters: JSONObject?, key: String): List<String> {
    val arr = masters?.optJSONArray(key) ?: return emptyList()
    return (0 until arr.length()).map { arr.getString(it) }
}

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

    val projectRows = projects(masters)
    val rooms = cat.rooms(masters)
    val stages = strings(masters, "stages")

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
    fun ingest(uri: Uri) {
        val p = newSite?.let { ProjectRow("", it.second, it.first) } ?: project
        val r = room
        if (p == null || r == null) return
        val stageNow = stage
        busy = "Splitting the 360 into six faces…"
        lastResult = null
        scope.launch(Dispatchers.Default) {
            val outcome = runCatching {
                val id = Handover.mintDeviceId(
                    ByteArray(6).also { SecureRandom().nextBytes(it) })
                val today = LocalDate.now().toString()
                val fov = CaptureGeometry.PRESETS.getOrNull(roomSize)?.fov
                    ?: masters?.optDouble("default_fov", Panorama.DEFAULT_FOV)
                    ?: Panorama.DEFAULT_FOV
                val (result, pano) = FaceWriter.split(
                    context = context, source = uri, deviceId = id,
                    customerName = p.customer, projectTitle = p.title,
                    room = r, captureDate = today, stage = stageNow, fov = fov,
                    panoDir = File(context.filesDir, "panos"))
                store.insert(CaptureStore.Capture(
                    deviceId = id, project = p.name, projectTitle = p.title,
                    customerName = p.customer, room = r, stage = stageNow,
                    captureDate = today, panoPath = pano.path,
                    createdAt = System.currentTimeMillis(), state = "LOCAL",
                    serverName = null, error = null))
                result
            }
            withContext(Dispatchers.Main) {
                busy = null
                lastResult = outcome.fold(
                    onSuccess = {
                        SyncWorker.syncNow(context)
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
                                { "ImageMeter sync done" },
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
                            onOpen = { navRoom = it },
                            onStage = { showStagePicker = true },
                            onSkus = { showSkus = true })
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
                annotatedSource = annotated[f.name]?.let { ThumbSource.LocalFile(it) },
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
                onMenu = { navCapture = null },
                onSearch = { showSettings = true })
        }) { pad ->
            Box(Modifier.padding(pad)) {
                CaptureScreen(
                    capture = cap,
                    faces = faces,
                    annotatedFaces = annotated.keys,
                    folder = Handover.relativePath(
                        queue.firstOrNull { q -> q.deviceId == cap.deviceId }
                            ?.customerName ?: "",
                        navProject?.title ?: "",
                        navRoom ?: "") + Handover.filename(cap.deviceId, "front"),
                    onOpenFace = { navFace = it; faceMode = true })
            }
        }
        return
    }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text("${navProject?.title ?: ""} · " +
                    RoomToken.label(navRoom ?: ""))
            },
            navigationIcon = {
                TextButton(onClick = { navRoom = null }) { Text("< Rooms") }
            },
            actions = {
                TextButton(onClick = { showSettings = true }) { Text("Settings") }
            })
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize()) {
            if (!configured) {
                Text("Set the server and API key in Settings to begin.")
                Spacer(Modifier.height(12.dp))
            }
            if (masters == null && configured) {
                Text("Waiting for the first master list — go online once.")
                Spacer(Modifier.height(12.dp))
            }

            // The photographs, as photographs. This room's captures were a
            // list of dates you had to open one at a time to find out which
            // wall you were looking at. Height-capped so the capture controls
            // below stay reachable without scrolling past a long history.
            val roomCaptures = queue.filter {
                it.room == navRoom &&
                    it.projectTitle.equals(navProject?.title ?: "", true)
            }.map {
                CaptureCard(deviceId = it.deviceId, date = it.captureDate,
                    stage = it.stage, panoPath = it.panoPath, state = it.state)
            }
            if (roomCaptures.isNotEmpty()) {
                Box(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                    CapturesScreen(roomCaptures) { navCapture = it; navFace = null }
                }
                Spacer(Modifier.height(12.dp))
            }

            updateJson?.let { uj ->
                val info = org.json.JSONObject(uj)
                Card(onClick = {
                    busy = "Downloading update ${info.optString("version_name")}…"
                    scope.launch(Dispatchers.IO) {
                        val outcome = runCatching {
                            val dest = File(context.filesDir, "updates/update.apk")
                            FrappeClient.load(context)!!.downloadPrivate(
                                info.getString("file_url"), dest)
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context, "com.malletcrafts.sitephotos.fileprovider", dest)
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW)
                                .setDataAndType(uri,
                                    "application/vnd.android.package-archive")
                                .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                        withContext(Dispatchers.Main) {
                            busy = null
                            outcome.onFailure {
                                lastResult = "Update failed: ${it.message}"
                            }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text("Update available: ${info.optString("version_name")} — " +
                        "tap to download & install",
                        Modifier.padding(12.dp))
                }
            }
            Picker("Stage", listOf("(none)") + stages,
                stage.ifBlank { "(none)" }) { i ->
                stage = if (i == 0) "" else stages[i - 1]
            }
            Spacer(Modifier.height(8.dp))
            // Small rooms need wider faces or the split truncates the walls —
            // the geometry (and the presets) live in CaptureGeometry.
            val sizeOptions = CaptureGeometry.PRESETS.map {
                "${it.label} — ${it.fov.toInt()}°"
            } + "Server default"
            Picker("Room size", sizeOptions,
                sizeOptions[roomSize.coerceIn(0, sizeOptions.size - 1)]) { i ->
                roomSize = i
                capturePrefs.edit().putInt("room_size", i).apply()
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Shoot from the room centre, camera LEVEL at half ceiling " +
                    "height (≈4 ft 9 in under a 9½ ft ceiling) — " +
                    "then every wall keeps all four corners after the split.",
                style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(16.dp))
            CameraCapability.port?.let { cam ->
                var x3Connected by remember { mutableStateOf(cam.connected) }
                var x3Note by remember { mutableStateOf<String?>(null) }
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text((if (x3Connected) "X3 connected" else "X3 not connected")
                        + (x3Note?.let { " — $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        if (x3Connected) { cam.disconnect(); x3Connected = false }
                        else cam.connect { ok, err ->
                            scope.launch(Dispatchers.Main) {
                                x3Connected = ok; x3Note = err
                            }
                        }
                    }) { Text(if (x3Connected) "Disconnect" else "Connect X3 (join its Wi-Fi first)") }
                }
                Button(
                    onClick = {
                        busy = "Shooting on the X3…"
                        lastResult = null
                        val out = File(context.filesDir,
                            "x3-${System.currentTimeMillis()}.jpg")
                        cam.shootAndExport(out.path) { result ->
                            scope.launch(Dispatchers.Main) {
                                busy = null
                                result.fold(
                                    onSuccess = { path -> ingest(Uri.fromFile(File(path))) },
                                    onFailure = { lastResult = "X3 capture failed: ${it.message}" })
                            }
                        }
                    },
                    enabled = busy == null && x3Connected
                        && (project != null || newSite != null) && room != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Shoot 360 on X3") }
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = {
                    picker.launch(PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = busy == null && (project != null || newSite != null)
                    && room != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Pick 360 photo") }

            busy?.let {
                Spacer(Modifier.height(12.dp))
                Row {
                    CircularProgressIndicator(Modifier.width(20.dp).height(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(it)
                }
            }
            lastResult?.let {
                Spacer(Modifier.height(12.dp))
                Card { Text(it, Modifier.padding(12.dp)) }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Captures", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = {
                    SyncWorker.syncNow(context)
                    refreshQueue()
                }) { Text("Sync now") }
            }
            LazyColumn {
                val here = queue.filter {
                    it.room == navRoom &&
                        it.projectTitle.equals(navProject?.title ?: "", true)
                }
                items(here, key = { it.deviceId }) { c ->
                    Card(onClick = { facesFor = c },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            Text((if (c.customerName.isNotBlank()) "${c.customerName} · " else "")
                                + "${c.projectTitle} — ${c.room}"
                                + (if (c.stage.isNotBlank()) " · ${c.stage}" else ""))
                            Text(
                                when (c.state) {
                                    "SYNCED" -> "Synced as ${c.serverName}"
                                    "ERROR" -> "Waiting to retry: ${c.error}"
                                    "SYNCING" -> "Uploading…"
                                    else -> "On this phone, will upload when online"
                                },
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }


}

@Composable
private fun Picker(label: String, options: List<String>, selected: String,
                   onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, Modifier.fillMaxWidth()) {
            Text("$label: $selected")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { i, opt ->
                DropdownMenuItem(text = { Text(opt) },
                    onClick = { onPick(i); open = false })
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
    onSyncNow: () -> Unit,
    onImageMeterSync: () -> Unit,
    onToggle: (String) -> Unit,
    onServer: () -> Unit,
    onSignOut: () -> Unit,
): List<DrawerGroup> = listOf(
    DrawerGroup("Sync", listOf(
        DrawerLine("Sync now",
            value = if (queued == 0) lastSync else "$queued waiting",
            icon = Icons.Filled.Refresh, onClick = onSyncNow),
        // Real, not decorative: this is the WorkManager constraint the
        // uploader runs under. A 20 MB pano on a site's mobile data is
        // somebody's bill.
        DrawerLine("Upload on Wi-Fi only", toggled = prefs.wifiOnly,
            icon = Icons.Filled.Warning, onClick = { onToggle("wifi_only") }),
    )),
    DrawerGroup("ImageMeter", listOf(
        DrawerLine("Pull annotations now", icon = Icons.Filled.Edit,
            onClick = onImageMeterSync),
        DrawerLine("Pull annotated copies", toggled = prefs.pullAnnotated,
            icon = Icons.Filled.Check, onClick = { onToggle("pull_annotated") }),
    )),
    DrawerGroup("Capture", listOf(
        DrawerLine("Field of view", value = fov, icon = Icons.Filled.Place),
        // Off means the bench does the split instead. The projection contract
        // in CI is what makes the two agree, so this is a real choice rather
        // than a quality trade.
        DrawerLine("Split faces on device", toggled = prefs.splitOnDevice,
            icon = Icons.Filled.Share, onClick = { onToggle("split_on_device") }),
        DrawerLine("Keep the original 360", toggled = prefs.keepOriginal,
            icon = Icons.Filled.Star, onClick = { onToggle("keep_original") }),
    )),
    DrawerGroup("Display", listOf(
        DrawerLine("Units", value = if (prefs.imperial) "mm · ft-in" else "mm",
            icon = Icons.Filled.Create, onClick = { onToggle("imperial") }),
    )),
    DrawerGroup("Storage & app", listOf(
        DrawerLine("Cached photos", value = cached, icon = Icons.Filled.Delete,
            onClick = { onToggle("clear_cache") }),
        DrawerLine("Version", value = version, icon = Icons.Filled.Info),
        DrawerLine("Sign out", icon = Icons.Filled.ExitToApp, onClick = onSignOut),
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
