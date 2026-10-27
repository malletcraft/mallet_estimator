package com.malletcrafts.interiorestimate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malletcrafts.interiorestimate.pano.RoomToken

/**
 * Work and Queue — the two tabs beside Browse.
 *
 * Browse is the filing cabinet. Work is what you are doing today, and it is
 * what the app opens on for anyone who is mid-job: the room you were last in,
 * with the shutter one tap away. Queue is the honest answer to "did that
 * actually go?", which was previously buried under a room.
 */

data class RecentRoom(
    val client: String,
    val site: String,
    val projectKey: String,
    val projectTitle: String,
    val room: String,
    val count: Int,
    val panoPath: String,
    val lastDate: String,
)

data class QueueRow(
    val token: String,
    val title: String,
    val subtitle: String,
    /** SYNCED / SYNCING / ERROR / anything else = still on the phone. */
    val state: String,
)

data class SearchHit(
    val token: String,
    val label: String,
    val sub: String,
    val client: String,
    val site: String,
    val projectKey: String,
    val room: String,
)

@Composable
private fun Head(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp))
}

/** Work — carry on from where you stopped. */
@Composable
fun WorkScreen(
    resume: RecentRoom?,
    recents: List<RecentRoom>,
    synced: Boolean,
    queued: Int,
    onOpen: (RecentRoom) -> Unit,
) {
    val tx = LocalTx.current
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Surface(
                color = if (synced) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = if (synced) MaterialTheme.colorScheme.onSecondaryContainer
                               else MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(if (synced) R.drawable.ic_mcft_cloud
                                        else R.drawable.ic_mcft_wifi),
                        contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (synced) tx.app_everything_phone_has_been
                         else tx.app_x_waiting_upload(queued),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (resume == null) {
            item {
                Text(tx.app_nothing_captured_phone_yet,
                    Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            item { Head(tx.app_carry_on) }
            item {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(16f / 7f)
                        .clickable { onOpen(resume) },
                ) {
                    Thumb(ThumbSource.LocalFile(resume.panoPath), Modifier.fillMaxSize(),
                        target = 700, contentDescription = resume.room)
                    Box(Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = .22f),
                            .45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = .62f))))
                    Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                        Text("${RoomToken.of(resume.room)} · ${resume.room}",
                            color = Color.White, fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${resume.client} / ${resume.site} / ${resume.projectTitle}",
                            color = Color.White.copy(alpha = .92f), fontSize = 11.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (resume.lastDate.isNotBlank()) {
                        Text(tx.app_last_shot_x(resume.lastDate), color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = .55f))
                                .padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                }
            }
        }

        if (recents.isNotEmpty()) {
            item { Head(tx.app_recent_rooms) }
            items(recents) { r ->
                Column {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp)
                            .clickable { onOpen(r) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))) {
                            Thumb(ThumbSource.LocalFile(r.panoPath), Modifier.fillMaxSize(),
                                target = 160, contentDescription = r.room)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.room, style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${r.client} · ${r.site}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("${r.count}", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        item { Spacer(Modifier.height(90.dp)) }
    }
}

/** Queue — what is still on the phone, and why. */
@Composable
fun QueueScreen(waiting: List<QueueRow>, sent: List<QueueRow>, wifiOnly: Boolean) {
    val tx = LocalTx.current
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Surface(
                color = if (waiting.isEmpty()) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = if (waiting.isEmpty()) MaterialTheme.colorScheme.onSecondaryContainer
                               else MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (waiting.isEmpty()) tx.app_nothing_waiting
                    else (if (wifiOnly) tx.app_wi_fi_only else "") +
                         tx.app_x_waiting(tx.pl_item(waiting.size)),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp))
            }
        }
        if (waiting.isNotEmpty()) {
            item { Head(tx.app_waiting_2) }
            items(waiting) { QueueLine(it) }
        }
        if (sent.isNotEmpty()) {
            item { Head(tx.app_sent_2) }
            items(sent) { QueueLine(it) }
        }
        item { Spacer(Modifier.height(90.dp)) }
    }
}

@Composable
private fun QueueLine(q: QueueRow) {
    val tx = LocalTx.current
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 62.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(q.token, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(q.title, style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(q.subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Pill(
                when (q.state) {
                    "SYNCED" -> tx.app_sent
                    "SYNCING" -> tx.app_sending
                    "ERROR" -> tx.app_retrying
                    else -> tx.app_queued
                },
                warn = q.state != "SYNCED")
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * Search — across all four levels at once.
 *
 * Four levels is one more than people will walk, and this is the escape
 * hatch: type MB or a client's name and land there, from anywhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchOverlay(
    query: String,
    hits: List<SearchHit>,
    recents: List<String>,
    onQuery: (String) -> Unit,
    onPick: (SearchHit) -> Unit,
    onClose: () -> Unit,
) {
    val tx = LocalTx.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(6.dp, 8.dp, 12.dp, 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_mcft_back), contentDescription = tx.app_aria_close_search)
            }
            TextField(
                value = query,
                onValueChange = onQuery,
                placeholder = { Text(tx.app_client_site_project_room) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent),
                modifier = Modifier.weight(1f).focusRequester(focus))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (query.isBlank()) {
            Head(tx.app_try)
            LazyColumn {
                items(recents) { t ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp)
                            .clickable { onQuery(t) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(painterResource(R.drawable.ic_mcft_search), contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(14.dp))
                        Text(t, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        } else if (hits.isEmpty()) {
            Text(tx.app_nothing_matches_x(query), Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn {
                items(hits) { h ->
                    Column {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 62.dp)
                                .clickable { onPick(h) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(9.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(h.token.take(4),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(h.label, style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(h.sub, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}
