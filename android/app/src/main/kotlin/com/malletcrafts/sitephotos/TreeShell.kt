package com.malletcrafts.sitephotos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
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
     *  the icon is what you actually aim at when scanning it one-handed. */
    val icon: ImageVector? = null,
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
                        icon = line.icon?.let { iv ->
                            { Icon(iv, contentDescription = null) }
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
    onMenu: () -> Unit,
    onSearch: () -> Unit,
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
            IconButton(onClick = onMenu) {
                Icon(Icons.Filled.Menu, contentDescription = "Settings")
            }
        },
        actions = {
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
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
    current: String,
    jobType: String,
    onPick: (Catalogue.Stage) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetTitle("Project stage · $jobType · ${stages.size} stages")
        // A plain scrolling Column, not a LazyColumn. Thirty-nine rows do not
        // need laziness, and a lazy list composes out of order — which breaks
        // any "print the heading when the phase changes" logic in a way that
        // only shows up as a missing heading halfway down.
        Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
            stages.groupBy { it.phase }.forEach { (phase, rows) ->
                Text(phase.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 2.dp))
                rows.forEach { s ->
                    ListItem(
                        headlineContent = { Text(s.name) },
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
        Spacer(Modifier.height(16.dp))
    }
}
