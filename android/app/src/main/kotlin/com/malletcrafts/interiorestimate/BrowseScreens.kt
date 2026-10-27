package com.malletcrafts.interiorestimate

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malletcrafts.interiorestimate.pano.QuoteStage
import com.malletcrafts.interiorestimate.pano.RoomToken

/**
 * The browser: client → SITE → project → room → the captures in it.
 *
 * Three things separate this from the card list it replaces, and all three
 * are about the 360dp of screen a phone actually has:
 *
 *  - Rows go EDGE TO EDGE. A Material card with 12dp side margins, 16dp
 *    inner padding and an elevation shadow spends roughly a third of the
 *    width on gutters and shows eight rows where fourteen fit.
 *  - Rooms are a three-across TILE grid, not a list. Rooms are visual, the
 *    token is what you read, and a grid puts twelve on one screen.
 *  - The project's current stage leads the room grid, because that is the
 *    question being asked on site: what still needs shooting today.
 */

private val ROW_HEIGHT = 64.dp

@Composable
private fun Badge(text: String, muted: Boolean = false) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(
                if (muted) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.take(4), style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

// combinedClickable is still experimental in foundation 1.7; the opt-in is
// the whole cost of having a long-press at all.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderRow(
    badge: String,
    title: String,
    subtitle: String,
    count: Int? = null,
    pill: String? = null,
    warn: Boolean = true,
    muted: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = ROW_HEIGHT)
                // Hold to rename. A correction is not a primary action and
                // must not sit where a scrolling thumb can hit it, but it
                // also must not be somewhere else entirely — the name is
                // wrong HERE, and this is where a person is looking at it.
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Badge(badge, muted)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (pill != null) {
                Spacer(Modifier.width(8.dp))
                Pill(pill, warn)
            }
            if (count != null) {
                Spacer(Modifier.width(8.dp))
                Text("$count", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(painterResource(R.drawable.ic_mcft_chev), contentDescription = null,
                tint = MaterialTheme.colorScheme.outline)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun Pill(text: String, warn: Boolean = true) {
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = if (warn) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(text.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
            color = if (warn) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
    }
}

@Composable
private fun NewRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = ROW_HEIGHT)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_mcft_plus), contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Empty(text: String) {
    Text(text, Modifier.padding(24.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Level 1 — clients. */
@Composable
fun ClientsScreen(
    clients: List<String>,
    siteCount: (String) -> Int,
    projectCount: (String) -> Int,
    onOpen: (String) -> Unit,
    onRename: (String) -> Unit,
    onNew: () -> Unit,
) {
    val tx = LocalTx.current
    LazyColumn(Modifier.fillMaxSize()) {
        if (clients.isEmpty()) {
            item {
                Empty("No clients yet. Go online once to pull them from ERP, " +
                      "or use New site to start one here.")
            }
        }
        items(clients) { c ->
            val s = siteCount(c)
            val p = projectCount(c)
            FolderRow(
                badge = initials(c),
                title = c,
                subtitle = "${tx.pl_site(s)} · ${tx.pl_project(p)}",
                onClick = { onOpen(c) },
                onLongClick = { onRename(c) })
        }
        item { NewRow(tx.app_new_client, tx.app_works_no_signal, onNew) }
    }
}

/** Level 2 — that client's sites. */
@Composable
fun SitesScreen(
    sites: List<Catalogue.Site>,
    projectCount: (Catalogue.Site) -> Int,
    onOpen: (Catalogue.Site) -> Unit,
    onRename: (Catalogue.Site) -> Unit,
    onNew: () -> Unit,
) {
    val tx = LocalTx.current
    LazyColumn(Modifier.fillMaxSize()) {
        items(sites) { s ->
            FolderRow(
                badge = if (s.type.isNotBlank()) s.type.take(3).uppercase() else "SITE",
                title = s.name,
                subtitle = listOf(s.type, s.city).filter { it.isNotBlank() }
                    .joinToString(" · ").ifBlank { tx.pl_project(projectCount(s)) },
                count = projectCount(s),
                pill = if (s.local) tx.app_offline else null,
                muted = true,
                onClick = { onOpen(s) },
                onLongClick = { onRename(s) })
        }
        item { NewRow(tx.common_new_site, tx.app_flat_bungalow_office, onNew) }
    }
}

/** Level 3 — projects at that site. */
@Composable
fun ProjectsScreen(
    projects: List<Catalogue.Project>,
    captureCount: (Catalogue.Project) -> Int,
    onOpen: (Catalogue.Project) -> Unit,
    onRename: (Catalogue.Project) -> Unit,
    onNew: () -> Unit,
) {
    val tx = LocalTx.current
    LazyColumn(Modifier.fillMaxSize()) {
        items(projects) { p ->
            FolderRow(
                badge = "PRJ",
                title = p.title,
                // Job type, then the dates, then where it has got to. That is
                // the order the question is asked in — what kind of job, is it
                // running now, and how far along.
                subtitle = listOfNotNull(
                    p.jobType,
                    p.dateRange.ifBlank { null },
                    p.stage.ifBlank { null },
                ).joinToString(" · "),
                count = captureCount(p),
                pill = p.statusLabel.ifBlank { null },
                warn = p.statusWarn,
                muted = true,
                onClick = { onOpen(p) },
                onLongClick = { onRename(p) })
        }
        item { NewRow(tx.app_new_project, tx.app_job_type_dates_scope, onNew) }
    }
}

/**
 * Level 4 — the rooms. A tile grid, because rooms are visual and the token
 * is what you actually read, and because twelve tiles fit on one screen
 * where six rows would not.
 */
@Composable
fun RoomsScreen(
    project: Catalogue.Project,
    rooms: List<String>,
    captureCount: (String) -> Int,
    shotAtStage: (String) -> Boolean,
    /** The newest 360 in that room, on this phone. The tile wears it. */
    lastPano: (String) -> String?,
    onOpen: (String) -> Unit,
    onStage: () -> Unit,
    /** S5's QUOTE stage for this project, as QuoteStageSync last read it (v52 item 16):
     *  drawn under the work stage, read-only. MainActivity always passes one. */
    quote: QuoteStage.Line?,
    /** The client's wish list (v52 item 16, slice 5b): the chip after rooms shot reads
     *  [wishLabel] ("Wish list N/12") and opens the list. Null draws no chip. */
    onWish: (() -> Unit)?,
    wishLabel: String,
    /** The project sheet (job type, status, ERP id, phase). The dates chip is
     *  the only way into it, so that chip is drawn on every project. */
    onDetail: () -> Unit,
    /** Amit, 2026-09-21: "can i add new room under project? I can not see any
     *  option for that." Rooms are a master — the room is half of every SKU
     *  code — so this does not invent a local one: it asks the bench to
     *  resolve or create, the same way a new site does. Null while offline,
     *  because a room minted only on this phone would mint a second one on
     *  the next phone and the code prefix would fork. */
    onAddRoom: (() -> Unit)? = null,
    /** Amit, 2026-09-25: "once we visit the site, rooms are fixed and need to
     *  see other unnecessary rooms". Opens the tick-list of which rooms this
     *  site actually has. Null leaves the behaviour exactly as it was. */
    onChooseRooms: (() -> Unit)? = null,
    /** How many of the master's rooms this site is currently hiding. Shown so
     *  a narrowed list never looks like a list that lost something. */
    hiddenRooms: Int = 0,
) {
    val tx = LocalTx.current
    var showAll by remember { mutableStateOf(false) }
    val shot = rooms.filter { captureCount(it) > 0 }
    val rest = rooms.filter { captureCount(it) == 0 }
    val pending = shot.count { !shotAtStage(it) }

    Column(Modifier.fillMaxSize()) {
        StageBar(project, pending, onStage)
        quote?.let { QuoteLine(it) }
        // The facts about this job, each a tap to the thing behind it. The
        // dates chip is not decoration: "is this job even running this week"
        // is asked more often on site than anything else on the screen.
        // Amit, 15 Oct 2026: "Remove both" -- the N SKUs chip and the job-type
        // chip are gone, so the dates chip is the one way into the project
        // sheet and is drawn on every project, reading "No dates" when blank.
        Row(
            Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            AssistChip(onClick = { }, enabled = false,
                // "3 rooms shot", composed as the prototype composes it (v53 app.js:460): the count, then app.shot
                label = { Text(tx.pl_room(shot.size) + tx.app_shot) })
            // The wish list (v52 item 16; Amit, 14 Oct 06:36 IST: "the wishlist should never get
            // mixed with survey as this wishlist comes after survey"): reached from the PROJECT
            // only, never from a room, a capture or survey prep. Teal so it reads as the
            // quote line's, not the survey's (wish.css .chip.wq: a 1 dp #0F766E outline, #0F5E57
            // text, 12 dp side padding -- the chip's own 8 dp plus 4 dp round the label).
            onWish?.let {
                AssistChip(
                    onClick = it,
                    label = { Text(wishLabel, modifier = Modifier.padding(horizontal = 4.dp)) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF0F5E57)),
                    border = BorderStroke(1.dp, Color(0xFF0F766E)))
            }
            // The scope control sits with the other facts about this job, not
            // in a menu: choosing the rooms IS a fact about the site, and it
            // is the first thing worth doing on arriving at one.
            onChooseRooms?.let {
                AssistChip(onClick = it, label = {
                    Text(if (hiddenRooms > 0) tx.app_rooms_x_hidden(hiddenRooms.toString())
                         else tx.app_choose_rooms)
                })
            }
            AssistChip(onClick = onDetail,
                label = { Text(project.dateRange.ifBlank { tx.app_no_dates }) })
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(2.dp),
        ) {
            items(shot) { r ->
                RoomTile(r, captureCount(r), shotAtStage(r), lastPano(r)) { onOpen(r) }
            }
            item {
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { showAll = !showAll },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (showAll) tx.common_hide else "+${rest.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            if (showAll) {
                items(rest) { r -> RoomTile(r, 0, false, null) { onOpen(r) } }
                // Last, and only with the full list open. A room that is not
                // among the thirteen is a rare thing, and a create button
                // sitting above the rooms somebody actually shoots would be
                // pressed by accident far more often than on purpose.
                onAddRoom?.let { add ->
                    item {
                        Box(
                            Modifier
                                .aspectRatio(1f)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { add() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(tx.app_room,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StageBar(project: Catalogue.Project, pending: Int, onClick: () -> Unit) {
    val tx = LocalTx.current
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 11.dp)) {
            Text(tx.app_project_is_at, style = MaterialTheme.typography.labelSmall)
            Text(project.stage.ifBlank { tx.app_no_stage_set_tap },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    project.stageSince.ifBlank { null }
                        ?.let { tx.app_since_x(Catalogue.day(it)) },
                    if (pending > 0) tx.app_x_not_shot_stage(pending)
                    else tx.app_every_photographed_room_current,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * THE QUOTE STAGE under the work stage (v52 item 16; Amit, 14 Oct 2026, 06:53 IST: "survey,
 * client wish list, vendor quoted, client approved, estimated"). The COMMERCIAL state of the
 * job, which staff move on S5 QUOTE; the phone only shows it, so there is no tap. Outlined,
 * not filled -- a surface with a teal bar on the left -- so it never reads as the tappable
 * PROJECT IS AT card above it. Sizes are the prototype's (.qst, wish.css): padding 7/16/8/13
 * after a 3 dp bar, the title at 14/20 sp medium with .4 sp tracking in #0F5E57, six 12 x 4 dp
 * pips 3 dp apart, the second line at 12/16 sp: 7 + 20 + 16 + 8 = 51 dp in portrait. In
 * landscape it is one row, 5 + 20 + 5 = 30 dp, with the second line beside the pips.
 */
@Composable
private fun QuoteLine(line: QuoteStage.Line) {
    val teal = Color(0xFF0F766E)
    val ink = Color(0xFF0F5E57)
    val rule = MaterialTheme.colorScheme.outlineVariant
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val titleStyle = MaterialTheme.typography.titleSmall.copy(
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.4.sp, fontWeight = FontWeight.Medium)
    val secondStyle = MaterialTheme.typography.bodySmall.copy(
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp, fontWeight = FontWeight.Normal)
    val secondColor = MaterialTheme.colorScheme.onSurfaceVariant
    val frame = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .drawBehind {
            drawRect(teal, size = Size(3.dp.toPx(), size.height))
            val y = size.height - 0.5.dp.toPx()
            drawLine(rule, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
    if (landscape) {
        Row(
            frame.padding(start = 16.dp, top = 5.dp, end = 16.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(line.title, style = titleStyle, color = ink, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                QuotePips(line.lit, teal)
            }
            Text(line.second, style = secondStyle, color = secondColor, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
    } else {
        Column(frame.padding(start = 16.dp, top = 7.dp, end = 16.dp, bottom = 8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(line.title, style = titleStyle, color = ink, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                QuotePips(line.lit, teal)
            }
            // One line, so the block stays within 52 dp whatever it says.
            Text(line.second, style = secondStyle, color = secondColor, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The six quote stages (v61b) as 12 x 4 dp steps, lit up to and including the current one. */
@Composable
private fun QuotePips(lit: Int, on: Color) {
    val off = MaterialTheme.colorScheme.outlineVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (i in QuoteStage.STAGES.indices) {
            Box(
                Modifier
                    .size(width = 12.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i < lit) on else off))
        }
    }
}

/**
 * A room, wearing its most recent 360.
 *
 * A grid of coloured squares with three-letter tokens is a menu; a grid of
 * the actual rooms is recognition, and recognition is the whole reason the
 * rooms stopped being a list. The photo is already on the phone, so this
 * costs no network and works in a basement.
 */
@Composable
private fun RoomTile(
    room: String,
    count: Int,
    current: Boolean,
    pano: String?,
    onClick: () -> Unit,
) {
    val shot = count > 0
    val photo = shot && pano != null
    Box(
        Modifier
            .aspectRatio(1f)
            .background(
                if (shot) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
    ) {
        if (photo) {
            Thumb(ThumbSource.LocalFile(pano!!), Modifier.fillMaxSize(),
                target = 260, contentDescription = room)
            // Without the wash the white token vanishes into a bright wall.
            Box(Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = .34f),
                    .45f to Color.Transparent,
                    1f to Color.Black.copy(alpha = .50f))))
        }
        Text(RoomToken.of(room),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                photo -> Color.White
                shot -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
        if (shot) {
            // Green means this room has been shot at the stage the project is
            // AT; amber means it has photos but none from this stage. That is
            // the whole question on site: what still needs shooting today.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .size(9.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (current) Color(0xFF3F8F5B) else Color(0xFFB0740E)))
            Text("$count", style = MaterialTheme.typography.labelSmall,
                color = if (photo) Color.White
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp))
        }
        Text(room, style = MaterialTheme.typography.labelSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = when {
                photo -> Color.White
                shot -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.align(Alignment.BottomStart).padding(7.dp).fillMaxWidth(0.72f))
    }
}

// ---- small shared helpers ----------------------------------------------

fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "?"
    if (parts.size == 1) return parts[0].take(2).uppercase()
    return (parts.first().first().toString() + parts.last().first()).uppercase()
}
