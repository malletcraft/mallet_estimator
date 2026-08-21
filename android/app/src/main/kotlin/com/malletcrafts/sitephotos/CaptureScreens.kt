package com.malletcrafts.sitephotos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A room's captures, as photographs.
 *
 * They were a list of dates you had to open one at a time to find out which
 * wall you were looking at — which is not how anyone looks for a photo. Every
 * image is already on the phone, so the grid fills with no network: the
 * original 360 is app-private for the sync worker, and the six faces sit in
 * Pictures/MCFT Site Photos where ImageMeter browses them.
 */

data class CaptureCard(
    val deviceId: String,
    val date: String,
    val stage: String,
    val panoPath: String,
    val state: String,
    /** How many of the six faces have come back annotated from ImageMeter. */
    val annotated: Int = 0,
)

private val YELLOW = Color(0xFFE9FF3A)
private val INK = Color(0xFF1B2000)

@Composable
private fun Scrim() {
    Box(Modifier.fillMaxSize().background(
        Brush.verticalGradient(
            0f to Color.Black.copy(alpha = .30f),
            .40f to Color.Transparent,
            1f to Color.Black.copy(alpha = .48f))))
}

@Composable
private fun PenBadge(modifier: Modifier = Modifier, label: String? = null) {
    Row(
        modifier
            .clip(RoundedCornerShape(11.dp))
            .background(YELLOW)
            .padding(horizontal = 5.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Edit, contentDescription = "annotated",
            tint = INK, modifier = Modifier.size(12.dp))
        if (label != null) {
            Spacer(Modifier.width(3.dp))
            Text(label, color = INK, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Level 5 — the captures in one room, newest first. */
@Composable
fun CapturesScreen(
    captures: List<CaptureCard>,
    onOpen: (CaptureCard) -> Unit,
) {
    if (captures.isEmpty()) {
        Text("No captures in this room yet. Use Capture 360 below.",
            Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(2.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(captures, key = { it.deviceId }) { c ->
            Box(
                Modifier
                    .aspectRatio(4f / 3f)
                    .clickable { onOpen(c) },
            ) {
                Thumb(ThumbSource.LocalFile(c.panoPath), Modifier.fillMaxSize(),
                    target = 400, contentDescription = "${c.date} ${c.stage}")
                Scrim()
                if (c.stage.isNotBlank()) {
                    Text(c.stage.uppercase(), color = Color.White,
                        fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.TopStart).padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = .55f))
                            .padding(horizontal = 5.dp, vertical = 3.dp))
                }
                if (c.annotated > 0) {
                    PenBadge(Modifier.align(Alignment.TopEnd).padding(6.dp),
                        "${c.annotated}/6")
                }
                Row(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth()
                        .padding(horizontal = 7.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(c.date, color = Color.White, fontSize = 10.sp,
                        fontWeight = FontWeight.Medium)
                    Text(if (c.state == "SYNCED") "synced" else "on phone",
                        color = Color.White.copy(alpha = .85f), fontSize = 10.sp)
                }
            }
        }
    }
}

/**
 * One capture: the 360 across the top, its six faces below.
 *
 * The 360 stays at this level on purpose — it is the record, and the faces
 * are what get handed to ImageMeter.
 */
@Composable
fun CaptureScreen(
    capture: CaptureCard,
    faces: List<LocalFaces.Face>,
    annotatedFaces: Set<String>,
    folder: String,
    onOpenFace: (Int) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScrollState()) {
        // Not clickable: the 360 is the record, and there is nothing useful to
        // open it into yet. A large image that does nothing when tapped reads
        // as a broken app, so it does not invite the tap.
        Box(Modifier.fillMaxWidth().height(140.dp)) {
            Thumb(ThumbSource.LocalFile(capture.panoPath), Modifier.fillMaxSize(),
                target = 900, contentDescription = "the original 360")
            Scrim()
            Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                Text("Original 360", color = Color.White,
                    style = MaterialTheme.typography.titleSmall)
                Text("${capture.deviceId}.jpg", color = Color.White.copy(alpha = .85f),
                    fontSize = 10.sp)
            }
        }

        Row(Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("SIX FACES", style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                when {
                    faces.isEmpty() -> "not split yet"
                    annotatedFaces.isEmpty() -> "none annotated yet"
                    else -> "${annotatedFaces.size} back from ImageMeter"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (faces.isEmpty()) {
            Text("The faces are written when the 360 is split. If this capture " +
                 "came from another phone, open it on the desk instead.",
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // A plain grid of rows rather than a nested LazyVerticalGrid: a lazy
        // grid inside a scrolling Column has no bounded height and crashes.
        faces.chunked(2).forEachIndexed { rowIdx, pair ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                pair.forEachIndexed { colIdx, f ->
                    val index = rowIdx * 2 + colIdx
                    Box(
                        Modifier.weight(1f).aspectRatio(4f / 3f)
                            .clickable { onOpenFace(index) },
                    ) {
                        Thumb(ThumbSource.Content(f.uri), Modifier.fillMaxSize(),
                            target = 400, contentDescription = f.name)
                        Scrim()
                        Text(f.name.uppercase(), color = Color.White,
                            fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = .55f))
                                .padding(horizontal = 5.dp, vertical = 3.dp))
                        if (f.name in annotatedFaces) {
                            PenBadge(Modifier.align(Alignment.TopEnd).padding(6.dp))
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Text("FILED AT", style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp))
        Text(folder, Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
    }
}

/** Column scroll, kept in one place so the import stays out of call sites. */
@Composable
private fun Modifier.verticalScrollState(): Modifier =
    this.then(androidx.compose.foundation.verticalScroll(
        androidx.compose.foundation.rememberScrollState()))

/**
 * One face, full screen, with the Original ⇄ Annotated toggle.
 *
 * This is the whole point of pulling annotations back: ImageMeter is where
 * you DRAW, not where you look. The annotated copy comes from the server —
 * the Drive round trip already attaches it to the capture by face — and is
 * cached on disk so a second look costs nothing and works offline.
 *
 * ImageMeter is one button, and it is only there for when something needs to
 * change.
 */
@Composable
fun FaceViewer(
    title: String,
    subtitle: String,
    source: ThumbSource?,
    annotatedSource: ThumbSource?,
    showAnnotated: Boolean,
    onToggle: (Boolean) -> Unit,
    onEditInImageMeter: () -> Unit,
    faces: List<LocalFaces.Face>,
    current: Int,
    onPickFace: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val hasAnnotated = annotatedSource != null
    Column(Modifier.fillMaxSize().background(Color(0xFF0B0D0D))) {
        Row(Modifier.fillMaxWidth().padding(4.dp, 6.dp, 8.dp, 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("Close", color = Color(0xFFEDEFEA)) }
            Column(Modifier.weight(1f)) {
                Text(title, color = Color(0xFFEDEFEA),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = Color(0xFF9AA29E), fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            val shown = if (showAnnotated && hasAnnotated) annotatedSource else source
            Thumb(shown, Modifier.fillMaxSize(), target = 1600,
                contentDescription = title)
        }

        Column(Modifier.padding(12.dp, 10.dp, 12.dp, 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))) {
                SegButton("Original", selected = !showAnnotated || !hasAnnotated,
                    enabled = true, modifier = Modifier.weight(1f)) { onToggle(false) }
                SegButton(if (hasAnnotated) "Annotated" else "Not annotated",
                    selected = showAnnotated && hasAnnotated,
                    enabled = hasAnnotated, modifier = Modifier.weight(1f)) {
                    onToggle(true)
                }
            }
            OutlinedButton(onClick = onEditInImageMeter, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Edit, contentDescription = null,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text(if (hasAnnotated) "Edit in ImageMeter" else "Annotate in ImageMeter")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                faces.forEachIndexed { i, f ->
                    Box(
                        Modifier.weight(1f).height(38.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .clickable { onPickFace(i) },
                    ) {
                        Thumb(ThumbSource.Content(f.uri), Modifier.fillMaxSize(),
                            target = 160, contentDescription = f.name)
                        if (i == current) {
                            Box(Modifier.fillMaxSize()
                                .background(YELLOW.copy(alpha = .28f)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .background(if (selected) YELLOW else Color(0xFF1B1F1E))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label,
            color = when {
                selected -> INK
                !enabled -> Color(0xFF5E6663)
                else -> Color(0xFFC7CCC8)
            },
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp)
    }
}
