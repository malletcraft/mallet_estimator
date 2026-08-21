package com.malletcrafts.sitephotos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.foundation.horizontalScroll
import com.malletcrafts.sitephotos.pano.CaptureGeometry
import com.malletcrafts.sitephotos.pano.RoomToken
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * The shell every folder level lives in: the three-line settings drawer, a
 * two-line app bar, and the breadcrumb rail under it.
 *
 * The drawer holds ONLY things you set once and forget — which server you
 * are signed into, Wi-Fi-only uploads, capture FOV, the ImageMeter folder,
 * the build version. Nothing you need mid-shoot is behind it, and that is
 * what frees the app bar: the build this replaces spent app-bar width on a
 * "Settings" text button and got a cramped one-line title in exchange.
 */

data class DrawerLine(
    val label: String,
    val value: String? = null,
    val toggled: Boolean? = null,
    /** Every row carries one. A drawer of bare text is a wall of words, and
     *  the icon is what you actually aim at when scanning it one-handed.
     *  A drawable res, not an ImageVector: these are the prototype's own
     *  icons (res/drawable/ic_mcft_*), because the core Material set had no
     *  honest stand-in for Wi-Fi-only, field of view, or keep-the-original. */
    @DrawableRes val icon: Int? = null,
    val onClick: (() -> Unit)? = null,
)

data class DrawerGroup(val title: String, val lines: List<DrawerLine>)

@Composable
fun SettingsDrawerContent(
    user: String,
    server: String,
    groups: List<DrawerGroup>,
) {
    ModalDrawerSheet(Modifier.width(300.dp)) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(20.dp))
            Column(Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                Text(user.ifBlank { "Not signed in" },
                    style = MaterialTheme.typography.titleMedium)
                Text(server.ifBlank { "no server set" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(Modifier.padding(top = 12.dp))
            groups.forEach { g ->
                Text(g.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 6.dp))
                g.lines.forEach { line ->
                    NavigationDrawerItem(
                        icon = line.icon?.let { res ->
                            { Icon(painterResource(res), contentDescription = null,
                                   modifier = Modifier.size(20.dp)) }
                        },
                        label = {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text(line.label, Modifier.weight(1f),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                when {
                                    line.toggled != null ->
                                        Switch(checked = line.toggled,
                                            onCheckedChange = { line.onClick?.invoke() })
                                    line.value != null ->
                                        Text(line.value,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        selected = false,
                        onClick = { line.onClick?.invoke() },
                        modifier = Modifier.padding(horizontal = 12.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * The app bar. Two lines on purpose: the title alone rarely says enough
 * ("Master Bedroom" — whose? which project?), and the subtitle costs nothing
 * because the bar is 64dp either way.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeTopBar(
    title: String,
    subtitle: String,
    /** Unused on a screen that passes [onBack] — there is no hamburger to
     *  press. Defaulted rather than required so those call sites do not have
     *  to hand over a lambda nothing can reach. */
    onMenu: () -> Unit = {},
    onSearch: () -> Unit,
    /** Set on screens that are a VIEW rather than a level — one capture, one
     *  face. They get a back arrow; the hamburger stays the settings drawer
     *  everywhere, which is the only way it means one thing. */
    onBack: (() -> Unit)? = null,
) {
    TopAppBar(
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_mcft_back),
                        contentDescription = "Back")
                }
            } else {
                IconButton(onClick = onMenu) {
                    Icon(painterResource(R.drawable.ic_mcft_menu),
                        contentDescription = "Settings")
                }
            }
        },
        actions = {
            IconButton(onClick = onSearch) {
                Icon(painterResource(R.drawable.ic_mcft_search),
                    contentDescription = "Search")
            }
        })
}

/** A one-line status strip, used for "no server yet" and "waiting for
 *  masters". Kept out of the list so it never scrolls away. */
@Composable
fun Banner(text: String, warn: Boolean = true) {
    Surface(
        color = if (warn) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (warn) MaterialTheme.colorScheme.onTertiaryContainer
                       else MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp))
    }
}

/**
 * The stage picker: thirty-nine rows, grouped by phase, current one marked.
 *
 * A chip row was never an option — thirty-nine chips is a wall, not a filter.
 * So the PHASE is what the capture list filters by, and the stage is chosen
 * here: one scroll, phase headings, the current stage highlighted so you can
 * see where the job is without reading every line.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StageSheet(
    stages: List<Catalogue.Stage>,
    /** Every stage in the master, whatever the job type. The picker offers
     *  the job type's slice by default and this on request — "I cannot see
     *  all the stages" has to have an answer ON the screen, not in a doc. */
    allStages: List<Catalogue.Stage> = stages,
    current: String,
    jobType: String,
    /** "Move the project to" or "Stage of this photo" — the same list, two
     *  quite different consequences, and the heading is the only warning. */
    heading: String = "Move the project to",
    onPick: (Catalogue.Stage) -> Unit,
    onDismiss: () -> Unit,
) {
    var showAll by remember { mutableStateOf(false) }
    val shown = if (showAll) allStages else stages
    val hidden = allStages.size - stages.size
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetTitle("$heading · $jobType · ${shown.size} stages")
        // Repair and Supply & install are a SLICE of the same sequence, not a
        // different one, so a picker narrowed to the job type is right — and
        // silently dropping fifteen rows is not. The count says what is
        // missing and the row unhides it.
        if (hidden > 0) {
            ListItem(
                headlineContent = {
                    Text(if (showAll) "Showing all ${allStages.size} stages"
                         else "$hidden more a $jobType job does not normally reach")
                },
                supportingContent = {
                    Text(if (showAll) "tap to go back to the $jobType stages"
                         else "tap to show every stage in the master")
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.clickableRow { showAll = !showAll })
        }
        // A plain scrolling Column, not a LazyColumn. Thirty-nine rows do not
        // need laziness, and a lazy list composes out of order — which breaks
        // any "print the heading when the phase changes" logic in a way that
        // only shows up as a missing heading halfway down.
        Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
            shown.groupBy { it.phase }.forEach { (phase, rows) ->
                Text(phase.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 2.dp))
                rows.forEach { s ->
                    val reachable = stages.any { it.name == s.name }
                    ListItem(
                        headlineContent = { Text(s.name) },
                        supportingContent = if (reachable) null else ({
                            Text("not a $jobType stage",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }),
                        colors = if (s.name == current)
                            ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        else ListItemDefaults.colors(),
                        modifier = Modifier.clickableRow { onPick(s) })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** The project's SKUs, so a photo can be filed beside the estimate line it
 *  belongs to. Read-only here; tagging happens on the capture itself. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkuSheet(
    skus: List<Catalogue.Sku>,
    onAdd: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetTitle(if (skus.isEmpty()) "No SKUs yet" else "${skus.size} SKUs on this project")
        if (skus.isEmpty()) {
            Text("The office adds these from the estimate. They arrive on the " +
                 "next sync.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        }
        skus.forEach { s ->
            ListItem(
                headlineContent = { Text(s.code) },
                supportingContent = {
                    Text(listOf(s.article, s.room).filter { it.isNotBlank() }
                        .joinToString(" · "))
                })
        }
        // The office usually adds these, but a technician standing in a
        // bathroom that turns out to need two doors should not have to phone
        // anybody. Offline, and it syncs like a new site does.
        onAdd?.let { add ->
            ListItem(
                headlineContent = {
                    Text("Add work", color = MaterialTheme.colorScheme.primary)
                },
                supportingContent = {
                    Text("room x article — the code writes itself")
                },
                modifier = Modifier.clickableRow(add))
        }
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Tag ONE photo to a SKU, or untag it.
 *
 * The SKUs of the room you are standing in come first, because that is what
 * you almost always want; the rest of the project is under them rather than
 * hidden, because a wardrobe photographed from the passage is a real thing.
 * And "none" is a first-class choice — a room shot filed against a wardrobe
 * is worse than one filed against nothing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSkuSheet(
    skus: List<Catalogue.Sku>,
    room: String,
    current: String,
    onPick: (String) -> Unit,
    onAdd: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val mine = skus.filter { it.room.equals(room, true) }
    val others = skus.filter { !it.room.equals(room, true) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetTitle("Tag this photo to a SKU")
        Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
            if (skus.isEmpty()) {
                Text("This project has no SKUs yet. The office adds them from " +
                     "the estimate, and they arrive on the next sync.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            }
            if (mine.isNotEmpty()) {
                SheetGroup("In ${RoomToken.of(room)}")
                mine.forEach { SkuLine(it, current, onPick) }
            }
            if (others.isNotEmpty()) {
                SheetGroup("Elsewhere on this project")
                others.forEach { SkuLine(it, current, onPick) }
            }
            onAdd?.let { add ->
                ListItem(
                    headlineContent = {
                        Text("Add work", color = MaterialTheme.colorScheme.primary)
                    },
                    supportingContent = { Text("not on the list yet? record it here") },
                    modifier = Modifier.clickableRow(add))
            }
            ListItem(
                headlineContent = { Text("— none —") },
                supportingContent = { Text("a room photo, not tied to one article") },
                colors = if (current.isBlank())
                    ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer)
                else ListItemDefaults.colors(),
                modifier = Modifier.clickableRow { onPick("") })
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SkuLine(s: Catalogue.Sku, current: String, onPick: (String) -> Unit) {
    ListItem(
        headlineContent = { Text(s.code) },
        supportingContent = { Text(s.article.ifBlank { s.room }) },
        trailingContent = { if (s.room.isNotBlank()) Pill(s.room, warn = false) },
        colors = if (s.code == current || s.name == current)
            ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer)
        else ListItemDefaults.colors(),
        modifier = Modifier.clickableRow { onPick(s.name.ifBlank { s.code }) })
}

@Composable
private fun SheetGroup(title: String) {
    Text(title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 2.dp))
}

/**
 * Everything the app knows about one project, on one sheet.
 *
 * The projects row and the room grid both had to choose two facts out of
 * ten. This is where the other eight live, so neither of those screens has
 * to grow a third line to carry them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSheet(
    project: Catalogue.Project,
    phase: String,
    phases: List<String>,
    stageCount: Int,
    photos: Int,
    onDismiss: () -> Unit,
) {
    val rows = buildList {
        add("At stage" to project.stage.ifBlank { "not set" })
        add("Phase" to listOfNotNull(
            phase.ifBlank { null },
            project.stageSince.ifBlank { null }?.let { "since ${Catalogue.day(it)}" },
        ).joinToString(" · ").ifBlank { "—" })
        add("Job type" to project.jobType)
        add("Status" to when {
            project.local -> "Not synced — still only on this phone"
            project.status.isBlank() -> "—"
            else -> project.status
        })
        add("ERP id" to project.serverId.ifBlank { "—" })
        add("Site" to project.site)
        add("Client" to project.client)
        add("Starts" to project.start.ifBlank { "—" })
        add("Ends" to project.end.ifBlank { "—" })
        add("Phases" to phases.joinToString(" · ").ifBlank { "—" })
        add("Stages" to "$stageCount in the master for this job type")
        add("Photos" to "$photos on this phone")
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetTitle(project.title)
        Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
            rows.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 5.dp)) {
                    Text(k, Modifier.width(96.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(v, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

/**
 * Work | Browse | Queue.
 *
 * Three destinations, which is where Material's bottom bar stops being a
 * menu and starts being a place: the filing cabinet, today's work, and the
 * honest answer to "did that actually go?". The Queue badge is the only
 * number in the app that must never be optimistic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomBar(current: String, queued: Int, onTab: (String) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = current == "work",
            onClick = { onTab("work") },
            icon = { Icon(painterResource(R.drawable.ic_mcft_home),
                          contentDescription = null) },
            label = { Text("Work") })
        NavigationBarItem(
            selected = current == "browse",
            onClick = { onTab("browse") },
            icon = { Icon(painterResource(R.drawable.ic_mcft_folder),
                          contentDescription = null) },
            label = { Text("Browse") })
        NavigationBarItem(
            selected = current == "queue",
            onClick = { onTab("queue") },
            icon = {
                // The cloud, not a refresh arrow: the queue answers "did that
                // actually go", which is about the server, not about reloading.
                val cloud = @Composable {
                    Icon(painterResource(R.drawable.ic_mcft_cloud),
                        contentDescription = null)
                }
                if (queued > 0) {
                    BadgedBox(badge = { Badge { Text("$queued") } }) { cloud() }
                } else cloud()
            },
            label = { Text("Queue") })
    }
}

/**
 * Everything the shutter needs, behind the FAB.
 *
 * It used to be a third of the room screen: two dropdowns, a paragraph of
 * levelling advice and two buttons, all sitting permanently above the photos
 * you came to look at. None of it is read twice — the room size is chosen
 * once a morning, the advice once ever — so it belongs in a sheet you open
 * when you are about to shoot and never see when you are not.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(
    stage: String,
    roomSize: Int,
    /** Null on the public build, which has no SDK and no 360 camera. */
    hasCamera: Boolean,
    cameraConnected: Boolean,
    cameraNote: String?,
    busy: Boolean,
    onStage: () -> Unit,
    onRoomSize: (Int) -> Unit,
    onPick: () -> Unit,
    onCamera: () -> Unit,
    onShoot: () -> Unit,
    /** A flat photograph, straight off the phone camera. */
    onTakePhoto: () -> Unit,
    /** A flat photograph from the gallery. */
    onPickPhoto: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetTitle("Capture 360")
        Column(Modifier.verticalScroll(rememberScrollState())) {
            ListItem(
                headlineContent = { Text(stage.ifBlank { "No stage set" }) },
                overlineContent = { Text("FILED AT") },
                supportingContent = { Text("tap to change — it can be corrected on the photo too") },
                modifier = Modifier.clickableRow(onStage))

            Text("ROOM SIZE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp))
            // Small rooms need wider faces or the split truncates the walls.
            // The geometry, and these presets, live in CaptureGeometry.
            Row(
                Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                CaptureGeometry.PRESETS.forEachIndexed { i, p ->
                    FilterChip(selected = roomSize == i, onClick = { onRoomSize(i) },
                        label = { Text("${p.label} ${p.fov.toInt()}°") })
                }
                FilterChip(
                    selected = roomSize >= CaptureGeometry.PRESETS.size,
                    onClick = { onRoomSize(CaptureGeometry.PRESETS.size) },
                    label = { Text("Server default") })
            }

            Text(
                "Shoot from the room centre, camera LEVEL at half ceiling height " +
                "(≈4 ft 9 in under a 9½ ft ceiling) — then every wall keeps all " +
                "four corners after the split.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp))

            if (hasCamera) {
                ListItem(
                    headlineContent = {
                        Text(if (cameraConnected) "X3 connected" else "X3 not connected")
                    },
                    supportingContent = {
                        Text(cameraNote ?: if (cameraConnected) "ready to shoot"
                             else "join the camera's Wi-Fi first, then tap")
                    },
                    modifier = Modifier.clickableRow(onCamera))
                Button(
                    onClick = onShoot,
                    enabled = !busy && cameraConnected,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                ) { Text("Shoot 360 on the X3") }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = onPick,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            ) { Text("Pick a 360 from the gallery") }

            // Not every capture is a 360. A repair is a close-up of one
            // hinge; a snag list is a dozen of them. These file a single
            // photograph under the same client, site, room and stage — the
            // filing is the point, not the projection.
            Text("A SINGLE PHOTO",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 6.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onTakePhoto, enabled = !busy,
                    modifier = Modifier.weight(1f)) { Text("Camera") }
                OutlinedButton(onClick = onPickPhoto, enabled = !busy,
                    modifier = Modifier.weight(1f)) { Text("Gallery") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Record a piece of WORK the site says is needed.
 *
 * Amit, 2026-08-21: "idea is to have a SKU / service discussed quickly with
 * client when measuring the site so that what high level required is
 * captured." So this is not the office transcribing an estimate — it is the
 * person standing in the room saying this wall needs POP, 120 sqft, while the
 * client is still next to them.
 *
 * ONE NUMBER, in the article's own unit. That is why electrical is three
 * articles rather than one carrying three figures: points, running feet and
 * fittings are quoted separately, and a phone in a dusty flat is the wrong
 * place for a spreadsheet. Built work also takes outer dimensions, because a
 * wardrobe has a shape and an area of POP does not.
 *
 * The CODE is shown before you commit, never typed. Two people describing the
 * same wall have to produce the same code or the grammar is decorative.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSkuSheet(
    articles: List<Catalogue.Article>,
    client: String,
    room: String,
    onAdd: (Catalogue.Article, Double?, Int?, Int?, Int?, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var picked by remember { mutableStateOf<Catalogue.Article?>(null) }
    var qty by remember { mutableStateOf("") }
    var w by remember { mutableStateOf("") }
    var h by remember { mutableStateOf("") }
    var d by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        val art = picked
        if (art == null) {
            SheetTitle("What work is needed in ${RoomToken.label(room)}?")
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                Catalogue.KIND_ORDER.forEach { kind ->
                    val rows = articles.filter { it.kind == kind }
                    if (rows.isEmpty()) return@forEach
                    Text(kindHeading(kind),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 2.dp))
                    rows.forEach { a ->
                        ListItem(
                            headlineContent = { Text(a.name) },
                            supportingContent = { Text("quoted by ${a.basis.lowercase()}") },
                            trailingContent = { Pill(a.code, warn = false) },
                            modifier = Modifier.clickableRow { picked = a })
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        } else {
            SheetTitle("${art.name} · ${RoomToken.label(room)}")
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)) {
                Text(previewCode(client, room, art.code),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
                Text("the code writes itself from customer, room and article",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))

                if (art.wantsQuantity) {
                    OutlinedTextField(
                        value = qty, onValueChange = { qty = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("How much? (${art.basis})") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                } else {
                    Text("Lumpsum — priced as a deal, so there is no quantity " +
                         "to take here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (art.wantsDimensions) {
                    Spacer(Modifier.height(10.dp))
                    Text("OUTER SIZE, mm — optional",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MmField("W", w, { w = it }, Modifier.weight(1f))
                        MmField("H", h, { h = it }, Modifier.weight(1f))
                        MmField("D", d, { d = it }, Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Note for the designer") },
                    modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picked = null },
                        modifier = Modifier.weight(1f)) { Text("Back") }
                    Button(
                        onClick = {
                            onAdd(art, qty.toDoubleOrNull(), w.toIntOrNull(),
                                h.toIntOrNull(), d.toIntOrNull(), note.trim())
                        },
                        // A quantity is the whole point of the line for work
                        // that is measured; a lumpsum deal has none to give.
                        enabled = !art.wantsQuantity || qty.toDoubleOrNull() != null,
                        modifier = Modifier.weight(1f),
                    ) { Text("Add") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MmField(label: String, value: String, onChange: (String) -> Unit,
                    modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() }) },
        label = { Text(label) }, singleLine = true, modifier = modifier)
}

private fun kindHeading(kind: String) = when (kind) {
    Catalogue.KIND_BUILD -> "We build it"
    Catalogue.KIND_INSTALL -> "We fit it"
    else -> "An agency does it"
}

/** The same grammar estimator.sku_code uses, shown before you commit. It is a
 *  PREVIEW: the bench is the authority, and it appends a suffix when this
 *  room already holds another of the same article. */
fun previewCode(client: String, room: String, articleCode: String): String =
    listOf(initials(client), RoomToken.of(room), articleCode)
        .filter { it.isNotBlank() }.joinToString("_")
