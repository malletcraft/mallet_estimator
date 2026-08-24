package com.malletcrafts.sitephotos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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
    /** The PHASE the photo is filed under — one of the ten, the word the
     *  capture doctype stores. The stage that produced it is [workStage]. */
    val stage: String,
    val panoPath: String,
    val state: String,
    /** How many of the six faces have come back annotated from ImageMeter. */
    val annotated: Int = 0,
    /** The Estimate SKU code this photo is filed against, or "". */
    val sku: String = "",
    /** The article that SKU names — "Wardrobe", "Study Table". The code alone
     *  identifies the work; the article says what it IS, and a designer
     *  reading a wall off a thumbnail needs the second one. */
    val article: String = "",
    /** The work stage the photo was filed at — one of the thirty-nine. Blank
     *  on a capture taken before the master existed, which still has a phase. */
    val workStage: String = "",
    /** "360" or "Photo". A 360 is the record of a whole room and splits into
     *  six faces; a Photo is one wall, floor or ceiling and has none. */
    val kind: String = "360",
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
        Icon(painterResource(R.drawable.ic_mcft_pen), contentDescription = "annotated",
            tint = INK, modifier = Modifier.size(12.dp))
        if (label != null) {
            Spacer(Modifier.width(3.dp))
            Text(label, color = INK, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Level 5 — the captures in one room, STAGE first and dates under it.
 *
 * A flat grid newest-first was the wrong shape for how a site actually runs.
 * Amit: "stage should come first and then its fotos on a particular date
 * because same stage can linger for many days but foto will keep
 * progressing." Exactly — a flat sits at Plaster & waterproofing for a week
 * and gets photographed every morning, and what you want to see is that
 * week's progress GROUPED under the thing that was happening, not eight
 * dates in a row with the same word stamped on each tile.
 *
 * Stages run newest-work-first (highest sequence at the top), which is where
 * the job is now; dates inside a stage run newest-first too. A capture whose
 * stage predates the master falls into its own group at the bottom rather
 * than being dropped.
 */
@Composable
fun CapturesScreen(
    captures: List<CaptureCard>,
    phases: List<String>,
    /** Trade order for a stage name — higher is later in the build. Unknown
     *  words sort to the bottom rather than to the top, where they would push
     *  today's work off the screen. */
    stageOrder: (String) -> Int,
    onOpen: (CaptureCard) -> Unit,
) {
    if (captures.isEmpty()) {
        Text("No captures in this room yet. Use Capture 360 below.",
            Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    var picked by remember { mutableStateOf<String?>(null) }
    // Only phases that actually have photos in THIS room. A chip that
    // filters to nothing is a chip that looks broken.
    val present = phases.filter { p -> captures.any { it.stage.equals(p, true) } }
    val shown = picked?.let { p -> captures.filter { it.stage.equals(p, true) } }
        ?: captures

    // stage -> date -> photos, both newest first.
    val grouped = remember(shown) {
        shown.groupBy { it.workStage.ifBlank { it.stage.ifBlank { "No stage" } } }
            .toList()
            .sortedByDescending { (stage, _) -> stageOrder(stage) }
            .map { (stage, rows) ->
                stage to rows.groupBy { it.date }.toList().sortedByDescending { it.first }
            }
    }

    Column(Modifier.fillMaxSize()) {
        if (present.size > 1) {
            Row(
                Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                FilterChip(selected = picked == null, onClick = { picked = null },
                    label = { Text("All ${captures.size}") })
                present.forEach { p ->
                    FilterChip(selected = picked == p, onClick = { picked = p },
                        label = { Text(p) })
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(2.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            grouped.forEach { (stage, byDate) ->
                val total = byDate.sumOf { it.second.size }
                item(key = "stage:$stage", span = { GridItemSpan(maxLineSpan) }) {
                    StageHeading(stage, total, byDate.size)
                }
                byDate.forEach { (date, rows) ->
                    // The date sub-head is dropped when the stage held only
                    // one day: a heading that never varies is furniture.
                    if (byDate.size > 1) {
                        item(key = "date:$stage:$date", span = { GridItemSpan(maxLineSpan) }) {
                            Text(date,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    start = 14.dp, top = 8.dp, bottom = 2.dp))
                        }
                    }
                    items(rows, key = { it.deviceId }) { c -> CaptureTile(c, onOpen) }
                }
            }
        }
    }
}

/** The stage a run of photographs belongs to, with how long it ran. */
@Composable
private fun StageHeading(stage: String, photos: Int, days: Int) {
    Column(Modifier.fillMaxWidth()
        .background(MaterialTheme.colorScheme.secondaryContainer)
        .padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text(stage.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(plural(photos, "photo") +
             (if (days > 1) " over ${plural(days, "day")}" else ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

/**
 * One capture: the photograph, and under it the work it is a photograph OF.
 *
 * Amit, 2026-08-23: "every capture should be able to show me sku related to it
 * just below it. currently its just showing as a tag on top which dose not
 * help ... this sku is for designer so that he will understand what is to be
 * designed on the wall for which this foto belongs."
 *
 * It used to be stamped on the image at 10sp against whatever the wall
 * happened to be, and it showed substringAfterLast('_') — the last fragment of
 * the code, not the work. Two failures in one: unreadable, and not the answer
 * anyway. The caption below is on solid ground, carries the whole code and the
 * article beside it, and says so plainly when a wall has no work on it yet —
 * an untagged photo is a question for somebody, and it cannot ask it while it
 * looks exactly like a tagged one.
 */
@Composable
private fun CaptureTile(c: CaptureCard, onOpen: (CaptureCard) -> Unit) {
    Column(Modifier.clickable { onOpen(c) }) {
        Box(
            Modifier
                .aspectRatio(4f / 3f),
        ) {
            Thumb(ThumbSource.LocalFile(c.panoPath), Modifier.fillMaxSize(),
                target = 400, contentDescription = "${c.date} ${c.stage}")
            Scrim()
            // The stage is the HEADING now, so the tile no longer repeats it —
            // it says what kind of capture this is instead, which is the thing
            // you cannot tell from a thumbnail.
            if (c.kind == "Photo") {
                Text("PHOTO", color = Color.White,
                    fontSize = 9.sp, fontWeight = FontWeight.Bold,
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
                // The SKU has moved below. What stays on the image is the one
                // thing that is about the FILE rather than about the work.
                Text(if (c.state == "SYNCED") "synced" else "on phone",
                    color = Color.White.copy(alpha = .85f), fontSize = 10.sp)
            }
        }
        Column(Modifier.fillMaxWidth().padding(7.dp, 6.dp, 7.dp, 8.dp)) {
            Text(
                c.sku.ifBlank { "No work tagged" },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (c.sku.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface)
            val under = c.article.ifBlank {
                if (c.sku.isBlank()) "tap to say what this wall needs" else ""
            }
            // Rendered only when it says something. An empty second line would
            // hold its height and make every tagged tile look like it was missing
            // a fact rather than not needing one.
            if (under.isNotBlank()) {
                Text(under,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** One tappable fact about a capture: a square token, a title, a reason. */
@Composable
private fun DetailRow(
    lead: String,
    title: String,
    subtitle: String,
    dim: Boolean = false,
    onClick: () -> Unit,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(lead.take(4), style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (dim) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(painterResource(R.drawable.ic_mcft_chev), contentDescription = null,
                tint = MaterialTheme.colorScheme.outline)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
    onPickStage: () -> Unit,
    onPickSku: () -> Unit,
    onDelete: (() -> Unit)? = null,
    /** ERP's name for this capture, blank while it is still only on the
     *  phone. The confirm dialog reads it out before destroying it. */
    serverId: String = "",
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // The two rows that make a photo findable a month later, and the two
        // that are most often wrong: the stage is inherited from whatever the
        // project was at when the shutter fired, and the SKU is usually not
        // set at all. Both are one tap from here, because a tag that can only
        // be fixed at a desk is a tag nobody fixes.
        // THE SKU COMES FIRST on a flat photo. Amit, 2026-08-22: "Sku is
        // function of a single foto. We must be able to clearly show on foto
        // screen what sku or service goes on that foto." A photograph of a
        // wall exists to say what work that wall needs; the stage is when,
        // which matters less than what. Below the picture it was something to
        // scroll past.
        if (capture.kind == "Photo") {
            DetailRow(
                lead = capture.sku.substringAfterLast('_').ifBlank { "SKU" },
                title = capture.sku.ifBlank { "Not tagged to a SKU" },
                subtitle = if (capture.sku.isNotBlank())
                               "the work expected on this wall · tap to change"
                           else "tap to say what work is expected here",
                dim = capture.sku.isBlank(),
                onClick = onPickSku)
        }
        DetailRow(
            lead = "STG",
            title = capture.workStage.ifBlank {
                capture.stage.ifBlank { "No stage set" } },
            // "recorded", not "tap to change": changing it is allowed, and
            // saying that the change is kept is what makes somebody choose
            // right at the shutter instead of fixing it later.
            subtitle = capture.stage.ifBlank { "no phase" } +
                " · tap to correct (recorded)",
            dim = capture.workStage.isBlank() && capture.stage.isBlank(),
            onClick = onPickStage)
        // ONLY on a flat photo. A 360 is the record of a whole ROOM, and a
        // room is not one article — you cannot say "this 360 is the
        // wardrobe". What CAN carry a SKU is a single wall, floor or
        // ceiling, which is what a flat photo is and what each of the six
        // faces is. Per-face tagging is the next batch; until it exists,
        // offering a capture-level SKU on a 360 would teach the wrong model.
        if (capture.kind == "Photo") {
            // Tapping opens the SAME viewer a face opens — which is where
            // Original/Annotated and "Annotate in ImageMeter" live. A photo
            // you cannot annotate is half a feature.
            Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f)
                .clickable { onOpenFace(0) }) {
                Thumb(ThumbSource.LocalFile(capture.panoPath), Modifier.fillMaxSize(),
                    target = 1200, contentDescription = "the photograph")
                if (annotatedFaces.isNotEmpty()) {
                    PenBadge(Modifier.align(Alignment.TopEnd).padding(8.dp), "marked")
                }
            }
            Text("Tap to open. To mark it up: add it to ImageMeter from your " +
                 "gallery, then Import from ImageMeter here.",
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("FILED AT", style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp))
            Text(folder, Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            onDelete?.let { DeleteRow(it, serverId) }
            Spacer(Modifier.height(28.dp))
            return@Column
        }

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
        onDelete?.let { DeleteRow(it, serverId) }
        Spacer(Modifier.height(28.dp))
    }
}

/** The only way to undo a shutter pressed by accident. Deliberately at the
 *  BOTTOM of the screen and behind a confirm: it is the one control here
 *  that destroys something. */
@Composable
private fun DeleteRow(onDelete: () -> Unit, serverId: String = "") {
    var confirm by remember { mutableStateOf(false) }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Delete this capture?") },
            text = {
                Column {
                    // This used to promise that "a capture that has already
                    // reached the server stays there". It has not been true
                    // since the phone learned to delete on the bench, and a
                    // reassurance that is no longer true is worse than none:
                    // it is the sentence a person reads just before losing
                    // the office's only copy.
                    Text(
                        if (serverId.isBlank())
                            "It goes from this phone — the queue row, the " +
                            "original, and the faces in the gallery. It never " +
                            "reached the server, so there is nothing there to " +
                            "remove."
                        else
                            "It goes from this phone AND from ERPNext, for " +
                            "everyone — the original, the faces, and the SKU " +
                            "if this was the only photograph holding it up.")
                    if (serverId.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(serverId,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { confirm = false; onDelete() }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Keep") }
            })
    }
    Spacer(Modifier.height(20.dp))
    OutlinedButton(
        onClick = { confirm = true },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) { Text("Delete this capture") }
}

/**
 * One face, full screen, with the Original ⇄ Annotated toggle.
 *
 * This is the whole point of pulling annotations back: ImageMeter is where
 * you DRAW, not where you look. The annotated copy comes from the server —
 * the Drive round trip already attaches it to the capture by face — and is
 * cached on disk so a second look costs nothing and works offline.
 *
 * ONE DIRECTION ONLY. There used to be a button here that handed the face to
 * ImageMeter, and it was wrong twice over: it created a fresh copy in
 * ImageMeter on every tap, so one wall accumulated duplicates, and coming
 * back it left the app showing nothing useful. Amit, 2026-08-22: "lets drop
 * any edit in imageter button in apk. and i am ok to import the split /
 * captured stamped (by apk) images in imageter myself."
 *
 * That is the better shape. The app writes stamped images into
 * Pictures/MCFT Site Photos, a person adds them to ImageMeter from the
 * gallery once, and the only traffic back this way is an IMPORT that matches
 * on the stamp. Re-annotating the same photograph later just produces a newer
 * export, which the next import picks up — no duplicates, and nothing to keep
 * in step.
 *
 * And it is where you LOOK, which is why the photo zooms. Amit, 2026-08-23:
 * "need pinch zoom on apk annotated fotos. hard to read it currently." What
 * comes back from ImageMeter is dimension text, sized for the photograph
 * rather than for a phone, and this screen was fitting the whole face into a
 * hand and offering no way in.
 */
@Composable
fun FaceViewer(
    title: String,
    subtitle: String,
    source: ThumbSource?,
    annotatedSource: ThumbSource?,
    showAnnotated: Boolean,
    onToggle: (Boolean) -> Unit,
    onImport: (() -> Unit)? = null,
    faceSku: String = "",
    onPickFaceSku: (() -> Unit)? = null,
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

        // THE SKU FOR THIS FACE, on the face. Amit, 2026-08-22: "why no sku
        // per foto?" — it was on the capture, and a capture is usually a 360,
        // which is a whole room and cannot be one article. This wall can.
        if (onPickFaceSku != null) {
            Row(Modifier.fillMaxWidth()
                .clickable(onClick = onPickFaceSku)
                .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_mcft_tag), contentDescription = null,
                    tint = if (faceSku.isBlank()) Color(0xFF6B7280) else Color(0xFFB9F227),
                    modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(9.dp))
                Text(
                    faceSku.ifBlank { "No work tagged on this face — tap to say what" },
                    color = if (faceSku.isBlank()) Color(0xFF9AA29E) else Color(0xFFEDEFEA),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            val shown = if (showAnnotated && hasAnnotated) annotatedSource else source
            // Zoomable, and the reset is keyed on the FACE rather than on which
            // of its two versions is showing: Original and Annotated are the
            // same wall from the same place, so a person flicking between them
            // to see what was marked keeps the region they were reading.
            ZoomableImage(shown, Modifier.fillMaxSize(),
                contentDescription = title, resetKey = current)
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
            // Where the "Edit in ImageMeter" button used to be, doing the
            // opposite. This is the screen a person is on when they come back
            // from marking something up, so it is the one place the import is
            // worth a tap rather than a trip through the drawer.
            if (onImport != null) {
                OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                    Icon(painterResource(R.drawable.ic_mcft_pen), contentDescription = null,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Import from ImageMeter")
                }
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
