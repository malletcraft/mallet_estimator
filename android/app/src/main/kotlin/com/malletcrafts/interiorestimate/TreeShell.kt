package com.malletcrafts.interiorestimate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.malletcrafts.interiorestimate.pano.Album
import com.malletcrafts.interiorestimate.pano.CaptureGeometry
import com.malletcrafts.interiorestimate.pano.RoomToken
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
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
    val tx = LocalTx.current
    ModalDrawerSheet(Modifier.width(300.dp)) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(20.dp))
            Column(Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                // "Not signed in" has no prototype key yet: keyed once the v61c card carries it.
                Text(user.ifBlank { "Not signed in" },
                    style = MaterialTheme.typography.titleMedium)
                Text(server.ifBlank { tx.app_no_server_set },
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
    photos: Int,
    onDismiss: () -> Unit,
) {
    // v61c: no site stage outside the v53 lens (index61c2.html projectSheet drops At stage, Phase, Phases
    // and Stages), so the ERP site stage is neither shown nor moved on the phone (port 61c slice PL)
    val rows = buildList {
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
 * you came to look at. The levelling advice is read once ever, and the room
 * dimensions once per room, so both belong in a sheet you open when you are
 * about to shoot and never see when you are not.
 *
 * ITS FIRST ROW IS WORK, NOT A STAGE (v61c §4.3, the prototype's workRow61c;
 * port 61c slice FW2): where the next capture files. Before Client approved
 * it reads `Quote journey` over the stage the project is at and the lock, and
 * takes no tap; after approval it names the work picked for this project (or
 * the first In progress, or `Pick the work`) with its state and `tap to
 * change`, and the tap opens Which work? ([WorkSheet]). The thirty-nine-stage
 * picker that was FILED AT is gone with it: nothing on the phone picks a stage
 * any more. Every text is keyed; the camera's own note ([cameraNote]) is the
 * camera module's, shown as it says it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(
    /** The WORK row's two texts (Album.filingHead, Album.workSub). */
    workHead: String,
    workSub: String,
    /** Opens Which work?; null before approval, where the row is a plain line
     *  (the prototype's li without onClick: no tap, no chevron). */
    onWork: (() -> Unit)?,
    /** Approved, and no work to file at yet (Album.newCaptureWork null): Shoot,
     *  Pick, Camera and Gallery wait for a pick -- the album camera's rule. */
    noWork: Boolean,
    /** What he measured, as typed — kept as text so a half-entered "1" is
     *  not silently a 1 ft room while his thumb is still moving. */
    roomLength: String,
    roomWidth: String,
    roomHeight: String,
    /** Null on the public build, which has no SDK and no 360 camera. */
    hasCamera: Boolean,
    cameraConnected: Boolean,
    cameraNote: String?,
    busy: Boolean,
    onRoomLength: (String) -> Unit,
    onRoomWidth: (String) -> Unit,
    onRoomHeight: (String) -> Unit,
    onPick: () -> Unit,
    onCamera: () -> Unit,
    onShoot: () -> Unit,
    /** A flat photograph, straight off the phone camera. */
    onTakePhoto: () -> Unit,
    /** A flat photograph from the gallery. */
    onPickPhoto: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tx = LocalTx.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetTitle(tx.common_capture_360)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            ListItem(
                headlineContent = { Text(workHead) },
                overlineContent = { Text(tx.album_work_caps) },
                supportingContent = { Text(workSub) },
                modifier = if (onWork != null) Modifier.clickableRow(onWork) else Modifier)

            Text(tx.app_room_size_mm,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp))
            // Amit, 2026-09-21: "capture all 3 sides of a room in inches by
            // the user and decide correct FOV based on the room. that should
            // tell where camera should be in the room by all its axes x,y,z.
            // you can automatically adjust FOV for all 6 faces so that all 4
            // corners of each face is available by just showing 10% of the
            // adjacent walls to distinguish the face being measured."
            //
            // Three numbers, not two: HEIGHT is the term the old maths left
            // out, and it is the one that governs the floor. The floor face
            // needs 2*atan(half-diagonal / camera height), so a hardcoded
            // 9.5 ft ceiling made the same room behave differently on
            // different days. Inches because that is what the tape reads --
            // rounding a room to feet moves the answer by degrees.
            val plan = mmPlan(roomLength, roomWidth, roomHeight)
            // HEIGHT IS THE ONE THAT IS REQUIRED. Amit, 2026-10-08, choosing
            // "height only, L x W optional": the room's eight corners give
            // every proportion from the photo, and the height turns them into
            // millimetres -- it is the scale bar printed on every elevation
            // for ImageMeter's reference scale. Length and width, if typed,
            // are the CHECK on what the photo measured, and with all three the
            // faces are also sized to the room as before.
            // ALL THREE ARE REQUIRED since 2026-10-10 (Amit: "how room length
            // and width can be optional now? we need it to set bounding box").
            // The room is set from four floor corners, and the typed carpet
            // length and width are what give those corners their size.
            val heightOk = heightMmValid(roomHeight)
            val sizesOk = heightOk && mmOrNull(roomLength) != null && mmOrNull(roomWidth) != null
            OutlinedTextField(
                value = roomHeight, onValueChange = onRoomHeight,
                label = { Text(tx.app_height_floor_ceiling_required) }, singleLine = true,
                isError = roomHeight.isNotBlank() && !heightOk,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = roomLength, onValueChange = onRoomLength,
                    label = { Text(tx.app_length_front_back_carpet) }, singleLine = true,
                    isError = roomLength.isNotBlank() && mmOrNull(roomLength) == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = roomWidth, onValueChange = onRoomWidth,
                    label = { Text(tx.app_width_left_right_carpet) }, singleLine = true,
                    isError = roomWidth.isNotBlank() && mmOrNull(roomWidth) == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f))
            }
            // "Where to stand" is gone (Amit, 2026-10-08: "i could not
            // understand it"). It placed the tripod at the room's centre for
            // the old FOV maths; with the eight corners measuring the room,
            // where the camera stood no longer matters as long as every
            // corner is in view.
            // The height's range is the prototype's own words, `1829–6096`:
            // mm(CaptureGeometry.MIN_CEILING_IN) and mm(MAX_CEILING_IN), 72 in
            // and 240 in, which the key carries as written (the prototype's
            // English wins, decision 5).
            Text(
                when {
                    !heightOk && roomHeight.isBlank() -> tx.app_type_height_shoot_pick
                    !heightOk -> tx.app_height_mm_1829_6096
                    !sizesOk || plan == null -> tx.app_type_carpet_length_front
                    plan.fitted -> tx.app_walls_x_floor_ceiling(
                        wall = Math.round(plan.wallFovDeg).toString(),
                        floor = Math.round(plan.floorFovDeg).toString())
                    else -> tx.app_room_needs_x_x(
                        floor = Math.round(plan.floorFovDeg).toString(),
                        v = plan.unfittedFaces.joinToString("/"))
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (!sizesOk || (plan != null && !plan.fitted))
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

            if (hasCamera) {
                ListItem(
                    headlineContent = {
                        Text(if (cameraConnected) tx.app_x3_connected else tx.app_x3_not_connected)
                    },
                    supportingContent = {
                        Text(cameraNote ?: if (cameraConnected) tx.app_ready_shoot
                             else tx.app_join_camera_s_wi)
                    },
                    modifier = Modifier.clickableRow(onCamera))
                Button(
                    onClick = onShoot,
                    enabled = !busy && cameraConnected && sizesOk && !noWork,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                ) { Text(tx.app_shoot_360_x3) }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = onPick,
                enabled = !busy && sizesOk && !noWork,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            ) { Text(tx.app_pick_360_gallery) }

            // Not every capture is a 360. A repair is a close-up of one
            // hinge; a snag list is a dozen of them. These file a single
            // photograph under the same client, site, room and work — the
            // filing is the point, not the projection.
            Text(tx.app_single_photo,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 6.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onTakePhoto, enabled = !busy && !noWork,
                    modifier = Modifier.weight(1f)) { Text(tx.common_camera) }
                OutlinedButton(onClick = onPickPhoto, enabled = !busy && !noWork,
                    modifier = Modifier.weight(1f)) { Text(tx.common_gallery) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * WHICH WORK? (v61c §4.3, the prototype's workSheet61c; port 61c slice FW2):
 * the sixteen works with their states, the current one marked. It never
 * offers the Quote journey -- a photo taken or re-filed after approval
 * belongs to a work, the album camera's rule. Two callers: the capture
 * sheet's WORK row (a pick sets this project's work for new captures and
 * reopens the capture sheet; a dismiss, or ◁, reopens it unchanged) and the
 * photo's WORK line (a pick re-files that one capture). The states are the
 * project-state store's (QuoteStage.Cache.states); a work with none reads
 * Not started, and until K4 nobody can set one from the phone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkSheet(
    current: Int?,
    states: Map<Int, Album.WorkState>?,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
    /**
     * Open WHOLE, as the prototype draws every sheet (port 61c slice K5: the progress shot camera's Which work? passes
     * true, as the album's sheets open since the K4 review, finding 1); the Capture 360 sheet's and the WORK line's calls
     * keep material3's half-open default -- the app-wide item §19 recorded, not K5's.
     */
    skipPartiallyExpanded: Boolean = false,
) {
    val tx = LocalTx.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)) {
        SheetTitle(tx.album_which_work)
        Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
            Album.WORKS.forEach { w ->
                ListItem(
                    headlineContent = { Text(Album.workName(tx, w.id)) },
                    supportingContent = { Text(Album.stateText(tx, states?.get(w.id)?.state)) },
                    colors = if (w.id == current)
                        ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    else ListItemDefaults.colors(),
                    modifier = Modifier.clickableRow { onPick(w.id) })
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** The same grammar estimator.sku_code uses, shown before you commit. It is a
 *  PREVIEW: the bench is the authority, and it appends a suffix when this
 *  room already holds another of the same article. */
fun previewCode(client: String, room: String, articleCode: String): String =
    listOf(initials(client), RoomToken.of(room), articleCode)
        .filter { it.isNotBlank() }.joinToString("_")

/**
 * Correct a name.
 *
 * Amit, 2026-08-22: "need to have ability to update edit names like client /
 * project / site etc." A name is keyed once, in a hurry, standing in
 * somebody's flat — and it is then the label on every photograph of that job.
 * Until now the only way to fix one was the desk, which is not where the
 * person who typed it is standing.
 *
 * The dialog says whether the change is local or goes to the server, because
 * those have different consequences and the person deserves to know which
 * they are about to do: a local row is words on this phone, an ERP row is the
 * office's record and every other device's view of it.
 */
@Composable
fun RenameDialog(
    what: String,          // "client" | "site" | "project"
    current: String,
    onServer: Boolean,
    busy: Boolean = false,
    /** What stands in the way of deleting this, from the server's last
     *  refusal — "still has 3 capture(s)". Shown in place of the explanation,
     *  and turns Delete into the deliberate second answer. */
    blocker: String? = null,
    /** A refusal this dialog cannot retry its way out of. `blocker` alone
     *  means the SERVER said no and named what is in the way — "still has 3
     *  capture(s)" — so the button becomes the cascade and the next tap can
     *  succeed. A fatal refusal is the app's own: it does not hold the
     *  record's id, so there is nothing to send and no cascade to offer.
     *  Showing the cascade button there would advertise a one-tap action
     *  that cannot work, on the very screen whose last bug was an action
     *  that appeared to work and did nothing. */
    blockerFatal: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    /** Null for a node that cannot be removed here (a client, which is a
     *  Customer and belongs to the office). The flag is "yes, cascade". */
    onDelete: ((Boolean) -> Unit)? = null,
    /** ERP's own name for this row — PROJ-0005, MEST-SITE-0005. Shown before
     *  the delete button, because that is the ONLY thing the server acts on
     *  and it is the only way a person can tell two rows apart when four
     *  sites on this bench are all called "Main site". */
    serverId: String = "",
) {
    var text by remember(current) { mutableStateOf(current) }
    val changed = text.trim().isNotEmpty() && text.trim() != current
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        // The name is wrong HERE and the thing is unwanted HERE. Sending a
        // person somewhere else to delete what they are already looking at is
        // how a tree fills with the wreckage of every mistyped site.
        //
        // But a client offers no remove, and this title said "Rename or
        // remove client" over a dialog holding only Cancel and Save. Amit,
        // 2026-08-24, photographed exactly that while trying to delete the
        // probe customer. A heading that names a control the dialog does not
        // have reads as a broken button, not as a boundary — so it now says
        // what this particular dialog can actually do.
        title = { Text(if (onDelete != null) "Rename or remove $what" else "Rename $what") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    enabled = !busy,
                    label = { Text(what.replaceFirstChar { it.uppercase() } + " name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (onServer)
                        "This ${what} is on the server, so the new name reaches " +
                        "ERP and every other device. Photos already filed under " +
                        "it keep their place — only the label changes."
                    else
                        "This ${what} has not synced yet, so this is a change on " +
                        "this phone. It will go up under the corrected name.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Say WHY there is no delete here, rather than leaving its
                // absence to be read as a bug. A missing control with no
                // explanation is indistinguishable from a broken one, which is
                // what sent Amit hunting for it on 2026-08-24.
                if (onDelete == null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "A client is the office's customer record, so it is not " +
                        "removed from a phone. Delete the sites under it here; " +
                        "the customer itself is a desk job.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onDelete != null) {
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    // WHAT IS ABOUT TO GO, BY NAME. 2026-08-24: a delete
                    // aimed at "upvc door gap filling" ended with that project
                    // untouched and Amit's other site — "Main site", and the
                    // Balcony Renovation inside it — destroyed instead. The
                    // dialog that asked for that confirmation named neither.
                    // A label a person reads is not what the server acts on;
                    // the docname is, so the docname is on screen before the
                    // button that sends it, and it is the LAST thing read
                    // before the tap.
                    if (serverId.isNotBlank()) {
                        Text("About to delete", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(current, style = MaterialTheme.typography.titleSmall)
                        Text(serverId, style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                    }
                    // The server refused and said why. Repeating its words is
                    // the whole point: "still has 3 capture(s)" is a fact a
                    // person can act on, where "could not delete" is not.
                    Text(
                        blocker
                            ?: "Deleting removes it from ERP for everyone. " +
                               "Anything already quoted on an estimate is kept.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (blocker != null) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    // No button at all when the refusal is the app's own.
                    // Cancel is still there, and the reason above says what
                    // to do instead — which is a refresh, not another tap.
                    if (!blockerFatal) TextButton(
                        enabled = !busy,
                        onClick = { onDelete(blocker != null) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(when {
                            busy -> "Working…"
                            // Only after being told what is in the way. A
                            // cascade offered before the refusal would be a
                            // one-tap way to lose a job's whole history.
                            // Both carry the docname. A destructive button
                            // whose text would read the same for any row is a
                            // button nobody can check before pressing.
                            blocker != null ->
                                "Delete " + serverId.ifBlank { current } +
                                " and everything in it"
                            else ->
                                "Delete " + serverId.ifBlank { "this ${what}" }
                        })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = changed && !busy,
                onClick = { onSave(text.trim()) }) {
                Text(if (busy) "Saving…" else "Save")
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") }
        },
    )
}


/** Room sizes as typed, in MILLIMETRES, to the inch-based plan the geometry
 *  and the bench use. The one conversion point: mm on every screen, inches
 *  only inside CaptureGeometry and on the wire. */
internal const val MM_PER_IN = 25.4

// Typed sizes follow the room setup (2026-10-10): LENGTH runs front→back and
// WIDTH left→right, both the carpet maximum. CaptureGeometry's own "length" is
// the span of the front and back walls -- left→right -- so the two are handed
// over crossed, and its plan's lengthIn is therefore the typed WIDTH.
internal fun mmPlan(lengthMm: String, widthMm: String, heightMm: String) =
    CaptureGeometry.planForRoom(
        widthMm.trim().toDoubleOrNull()?.div(MM_PER_IN),
        lengthMm.trim().toDoubleOrNull()?.div(MM_PER_IN),
        heightMm.trim().toDoubleOrNull()?.div(MM_PER_IN))

/** Inches back to whole millimetres, for anything shown to a person. */
internal fun mm(inches: Double): Int = Math.round(inches * MM_PER_IN).toInt()

/** A typed millimetre value, or null when blank or not a number. */
internal fun mmOrNull(typed: String): Double? = typed.trim().toDoubleOrNull()?.takeIf { it > 0 }

/** A height the geometry accepts: a real ceiling, not a typo in feet. */
internal fun heightMmValid(typed: String): Boolean {
    val mmv = mmOrNull(typed) ?: return false
    return mmv / MM_PER_IN in CaptureGeometry.MIN_CEILING_IN..CaptureGeometry.MAX_CEILING_IN
}
