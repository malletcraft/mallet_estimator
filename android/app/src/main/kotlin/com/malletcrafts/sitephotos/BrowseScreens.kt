package com.malletcrafts.sitephotos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The browser: client folder → project folder → room → the captures in it.
 *
 * Modelled on the folder tree Amit already navigates in ImageMeter, because
 * that is the shape of the work — you arrive at a client's flat, open the
 * project, and shoot room by room. Rooms come from the ERP master, so the
 * folder a photo lands in is the same string the estimate uses.
 *
 * Everything here reads from the local catalogue, so the whole tree works
 * with no signal. What cannot be done offline is invented: a site typed on
 * site is marked "not synced yet" rather than pretending it reached ERP.
 */

@Composable
private fun Crumb(text: String, onBack: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            TextButton(onClick = onBack) { Text("< Back") }
        }
        Text(text, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = if (onBack != null) 0.dp else 12.dp))
    }
}

@Composable
private fun FolderRow(
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit,
) {
    Card(onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (badge != null) {
                AssistChip(onClick = onClick, label = { Text(badge) })
            }
        }
    }
}

/** Level 1 — clients. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    clients: List<String>,
    projectCount: (String) -> Int,
    onOpen: (String) -> Unit,
    onNewSite: () -> Unit,
    onSettings: () -> Unit,
    banner: (@Composable () -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Clients") }, actions = {
                TextButton(onClick = onSettings) { Text("Settings") }
            })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNewSite,
                text = { Text("New site") }, icon = {})
        }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            banner?.invoke()
            if (clients.isEmpty()) {
                Text("No clients yet. Go online once to pull them from ERP, " +
                     "or use New site to start one here.",
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium)
            }
            LazyColumn(Modifier.weight(1f)) {
                items(clients) { c ->
                    val n = projectCount(c)
                    FolderRow(c, if (n == 1) "1 project" else "$n projects") { onOpen(c) }
                }
            }
        }
    }
}

/** Level 2 — that client's projects. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    client: String,
    projects: List<Catalogue.Project>,
    onOpen: (Catalogue.Project) -> Unit,
    onNewSite: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Crumb(client, onBack) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNewSite,
                text = { Text("New project") }, icon = {})
        }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize()) {
            items(projects) { p ->
                FolderRow(p.title,
                    if (p.synced) p.serverId else "not synced yet",
                    badge = if (p.synced) null else "offline") { onOpen(p) }
            }
        }
    }
}

/** Level 3 — rooms. The master list, with the ones already shot marked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsScreen(
    project: Catalogue.Project,
    rooms: List<String>,
    captureCount: (String) -> Int,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    var showAll by remember { mutableStateOf(false) }
    val shot = rooms.filter { captureCount(it) > 0 }
    val rest = rooms.filter { captureCount(it) == 0 }
    Scaffold(topBar = {
        TopAppBar(title = { Crumb("${project.client} · ${project.title}", onBack) })
    }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize()) {
            if (shot.isNotEmpty()) {
                item {
                    Text("Photographed", Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp),
                        style = MaterialTheme.typography.labelLarge)
                }
                items(shot) { r ->
                    val n = captureCount(r)
                    FolderRow(r, if (n == 1) "1 capture" else "$n captures",
                        badge = "$n") { onOpen(r) }
                }
            }
            item {
                Row(Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Other rooms", style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = { showAll = !showAll }) {
                        Text(if (showAll) "Hide" else "Show ${rest.size}")
                    }
                }
            }
            if (showAll) {
                items(rest) { r -> FolderRow(r, "no captures yet") { onOpen(r) } }
            }
        }
    }
}
