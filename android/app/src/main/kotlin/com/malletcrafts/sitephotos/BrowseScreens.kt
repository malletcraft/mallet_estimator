package com.malletcrafts.sitephotos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malletcrafts.sitephotos.pano.RoomToken

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

@Composable
private fun FolderRow(
    badge: String,
    title: String,
    subtitle: String,
    count: Int? = null,
    pill: String? = null,
    muted: Boolean = false,
    onClick: () -> Unit,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = ROW_HEIGHT)
                .clickable(onClick = onClick)
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
                Pill(pill)
            }
            if (count != null) {
                Spacer(Modifier.width(8.dp))
                Text("$count", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null,
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
            Icon(Icons.Filled.Add, contentDescription = null,
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
    onNew: () -> Unit,
) {
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
                subtitle = "${plural(s, "site")} · ${plural(p, "project")}",
                onClick = { onOpen(c) })
        }
        item { NewRow("New client", "works with no signal", onNew) }
    }
}

/** Level 2 — that client's sites. */
@Composable
fun SitesScreen(
    sites: List<Catalogue.Site>,
    projectCount: (Catalogue.Site) -> Int,
    onOpen: (Catalogue.Site) -> Unit,
    onNew: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(sites) { s ->
            FolderRow(
                badge = if (s.type.isNotBlank()) s.type.take(3).uppercase() else "SITE",
                title = s.name,
                subtitle = listOf(s.type, s.city).filter { it.isNotBlank() }
                    .joinToString(" · ").ifBlank { plural(projectCount(s), "project") },
                count = projectCount(s),
                pill = if (s.local) "offline" else null,
                muted = true,
                onClick = { onOpen(s) })
        }
        item { NewRow("New site", "a flat, bungalow or office", onNew) }
    }
}

/** Level 3 — projects at that site. */
@Composable
fun ProjectsScreen(
    projects: List<Catalogue.Project>,
    captureCount: (Catalogue.Project) -> Int,
    onOpen: (Catalogue.Project) -> Unit,
    onNew: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(projects) { p ->
            FolderRow(
                badge = "PRJ",
                title = p.title,
                subtitle = listOf(
                    p.jobType,
                    if (p.stage.isNotBlank()) p.stage else null,
                    if (p.synced) p.serverId else "not synced yet",
                ).filterNotNull().joinToString(" · "),
                count = captureCount(p),
                pill = if (p.local) "offline" else null,
                muted = true,
                onClick = { onOpen(p) })
        }
        item { NewRow("New project", "job type, dates, scope", onNew) }
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
    onOpen: (String) -> Unit,
    onStage: () -> Unit,
    onSkus: () -> Unit,
) {
    var showAll by remember { mutableStateOf(false) }
    val shot = rooms.filter { captureCount(it) > 0 }
    val rest = rooms.filter { captureCount(it) == 0 }
    val pending = shot.count { !shotAtStage(it) }

    Column(Modifier.fillMaxSize()) {
        StageBar(project, pending, onStage)
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = onSkus, label = { Text("SKUs") })
            AssistChip(onClick = onStage, label = { Text(project.jobType) })
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(2.dp),
        ) {
            items(shot) { r ->
                RoomTile(r, captureCount(r), shotAtStage(r)) { onOpen(r) }
            }
            item {
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { showAll = !showAll },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (showAll) "Hide" else "+${rest.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            if (showAll) {
                items(rest) { r -> RoomTile(r, 0, false) { onOpen(r) } }
            }
        }
    }
}

@Composable
private fun StageBar(project: Catalogue.Project, pending: Int, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 11.dp)) {
            Text("PROJECT IS AT", style = MaterialTheme.typography.labelSmall)
            Text(project.stage.ifBlank { "no stage set — tap to choose" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (pending > 0) "${plural(pending, "room")} not shot at this stage"
                else "every photographed room is current",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RoomTile(room: String, count: Int, current: Boolean, onClick: () -> Unit) {
    val shot = count > 0
    Box(
        Modifier
            .aspectRatio(1f)
            .background(
                if (shot) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
    ) {
        Text(RoomToken.of(room),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (shot) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
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
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp))
        }
        Text(room, style = MaterialTheme.typography.labelSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = if (shot) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomStart).padding(7.dp).fillMaxWidth(0.72f))
    }
}

// ---- small shared helpers ----------------------------------------------

fun plural(n: Int, word: String) = "$n $word" + if (n == 1) "" else "s"

fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "?"
    if (parts.size == 1) return parts[0].take(2).uppercase()
    return (parts.first().first().toString() + parts.last().first()).uppercase()
}
