package com.malletcrafts.interiorestimate

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malletcrafts.interiorestimate.pano.QuoteStage
import com.malletcrafts.interiorestimate.pano.QuoteView
import com.malletcrafts.interiorestimate.pano.RoomToken
import com.malletcrafts.interiorestimate.pano.TourDemo
import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Tx

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
 *  - The project line leads the room grid (v61c; Amit, 27 Oct 2026: "everything
 *    should be single truth"): ONE line derived from S5 -- the quote stage before
 *    approval, the album's works after it -- never set by hand. PROJECT IS AT, the
 *    ERP site stage's card, left the phone in port 61c slice PL. At Client approved
 *    the line is the tap that opens THE QUOTE VIEW (port 61c slice QV): the five
 *    ticked steps, Approved, and APPROVED SCOPE.
 *
 * Every text here is keyed (port 61c slice PL); the two badges are the prototype's
 * own unkeyed literals, marked `// text:value`. The masters' identity values -- a room,
 * a job type, a site type -- are drawn through Masters, as the prototype's text layer
 * draws them (index61c2.html under मराठी: `हॉल`, `पुरवठा व बसवणे`, `फ्लॅट`); a typed one
 * as typed. The badge and the room token stay from the English (slice PL review).
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
    /** A project row's subtitle takes two lines, as the prototype's `.ell2` does (v61c: the line leads it). */
    subtitleLines: Int = 1,
    /** The guided tour's name for this row (`row:<stored name>`, port 61c §31) — the demo rows only. */
    anchor: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .tourAnchor(anchor)
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
                        maxLines = subtitleLines, overflow = TextOverflow.Ellipsis)
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
                Empty(tx.app_no_clients_yet_go)
            }
        }
        items(clients) { c ->
            val s = siteCount(c)
            val p = projectCount(c)
            FolderRow(
                // the badge stays initials of the stored value, a code; the title through its
                // master, so the no-client bucket reads "(ग्राहक नाही)" (port 61c slice M6)
                badge = initials(c),
                title = Masters.client_name(tx, c),
                subtitle = "${tx.pl_site(s)} · ${tx.pl_project(p)}",
                // the survey tour's step 1 (§31): the anchor NAME is the flow's own spelling
                anchor = if (c.trim().equals(TourDemo.CLIENT, ignoreCase = true)) "row:" + TourDemo.CLIENT else null,
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
                badge = if (s.type.isNotBlank()) s.type.take(3).uppercase() else "SITE", // text:value the prototype's badge, an unkeyed literal there too
                title = Masters.site_name(tx, s.name), // "Main site" through its master; a typed name as typed (port 61c slice M2)
                subtitle = listOf(Masters.site(tx, s.type), s.city).filter { it.isNotBlank() }
                    .joinToString(" · ").ifBlank { tx.pl_project(projectCount(s)) },
                count = projectCount(s),
                pill = if (s.local) tx.app_offline else null,
                muted = true,
                // the survey tour's step 2 (§31)
                anchor = if (s.name.trim().equals(TourDemo.SITE, ignoreCase = true)) "row:" + TourDemo.SITE else null,
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
    /** The project's ONE derived line (QuoteStageSync.lineFor's text), read from the store S5's list GET fills. */
    lineOf: (Catalogue.Project) -> String,
    onOpen: (Catalogue.Project) -> Unit,
    onRename: (Catalogue.Project) -> Unit,
    onNew: () -> Unit,
) {
    val tx = LocalTx.current
    LazyColumn(Modifier.fillMaxSize()) {
        items(projects) { p ->
            FolderRow(
                badge = "PRJ", // text:value the prototype's badge, an unkeyed literal there too
                title = p.title,
                // The project line first, then the job type, then the dates (v61c §14.1; index61c2.html row61c):
                // where the job stands is the one state, so it leads; two lines, as the prototype's .ell2.
                subtitle = listOfNotNull(
                    lineOf(p).ifBlank { null },
                    Masters.job(tx, p.jobType).ifBlank { null },
                    p.dateRange(tx).ifBlank { null },
                ).joinToString(" · "),
                subtitleLines = 2,
                count = captureCount(p),
                pill = p.statusLabel(tx).ifBlank { null },
                warn = p.statusWarn,
                muted = true,
                // the survey tour's step 3 (§31)
                anchor = if (TourDemo.isDemoProject(p.client, p.title)) "row:" + TourDemo.PROJECT else null,
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
    /** A shot room's dot: green when true, amber when false (QuoteStage.roomDot). */
    roomDot: (String) -> Boolean,
    /** The newest 360 in that room, on this phone. The tile wears it. */
    lastPano: (String) -> String?,
    onOpen: (String) -> Unit,
    /** THE PROJECT LINE (QuoteStageSync.lineFor): the one state, derived, never set by hand. */
    line: QuoteStage.ProjectLine,
    /** THE QUOTE VIEW at Client approved (QV): the line's tap and what it opens; null when the line is not at Client approved. */
    quote: QuoteViewArgs? = null,
    /** THE PHASE STRIP through the quote journey (QM1): the five segments, the mover; null at Client approved, where [quote] is the strip. */
    strip: PhaseStripArgs? = null,
    /** The client's wish list (v52 item 16, slice 5b): the chip after rooms shot reads
     *  [wishLabel] ("Wish list N/12") and opens the list. Null draws no chip. */
    onWish: (() -> Unit)?,
    wishLabel: String,
    /** The project sheet (job type, status, ERP id, dates). The dates chip is
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
    /** THE PROJECT ALBUM's tile (port 61c slice K3): the grid's LAST child, after the shot rooms, the `+N` / Hide
     *  tile and, with the full list open, the other rooms and + Room (index61c2.html 8137). Null draws none. */
    album: AlbumTileArgs? = null,
) {
    val tx = LocalTx.current
    var showAll by remember { mutableStateOf(false) }
    val shot = rooms.filter { captureCount(it) > 0 }
    val rest = rooms.filter { captureCount(it) == 0 }

    Column(Modifier.fillMaxSize()) {
        ProjectLineBox(line, quote)
        // the strip under the line (index61c2.html stripHTML): the quote view's at Client approved, the phase strip's before
        if (quote != null) QuoteStrip(quote) else if (strip != null) PhaseStrip(strip)
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
                // "3 rooms shot": the WHOLE template app.x_shot (v57), never pl_room + the fragment app.shot,
                // which glued "3 खोल्या फोटो" -- the form v57 rejected for "फोटो झाले: 3 खोल्या"
                label = { Text(tx.app_x_shot(shot.size)) })
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
                label = { Text(project.dateRange(tx).ifBlank { tx.app_no_dates }) })
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(2.dp),
        ) {
            items(shot) { r ->
                // the survey tour's step 4 (§31): the demo project's Guest Bedroom tile
                RoomTile(r, captureCount(r), roomDot(r), lastPano(r),
                    anchor = if (TourDemo.isDemoProject(project.client, project.title) &&
                        r.trim().equals(TourDemo.ROOM, ignoreCase = true)) "room:" + TourDemo.ROOM else null) { onOpen(r) }
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
            // The Album, last: on EVERY project, one only on this phone included (v61c.2 draws a never-sent
            // project's album; only the v53 lens hid it).
            album?.let { a -> item { AlbumTile(a) } }
        }
    }
}

/**
 * THE PROJECT LINE (v61c; index61c2.html lineHTML): ONE line, derived from S5 (QuoteStage.projectLine), never
 * set by hand -- it replaces both the PROJECT IS AT card and the QUOTE STAGE line under it. Outlined, not
 * filled: a surface with a teal bar on the left. Sizes are the prototype's (.qst): padding 7/16/8/13 after a
 * 3 dp bar, the line at 14/20 sp medium with .4 sp tracking in #0F5E57, the second line at 12/16 sp: 7 + 20 +
 * 16 + 8 + the 1 dp rule = 52 dp in portrait. In landscape it is one row, 5 + 20 + 5 + 1 = 31 dp, the second
 * line beside it. The rule is drawn BELOW the bottom padding, as .qst's border-bottom is (slice PL review: it
 * was painted over the padding's last dp, 51 and 30 dp).
 * - Through the quote journey (and unread, or not on S5): `Quote · <stage>` with the six 12 x 4 dp pips
 *   3 dp apart, over `since …`, `<stage> · not set on S5 yet` or why there is no stage.
 * - At Client approved (.qst.qx): an 18 dp teal ✓ 10 dp before `Execution · …`, over `Quote · Client
 *   approved`, at least 48 dp tall (36 dp in landscape) with the rule. Since port 61c slice QV THE WHOLE LINE IS
 *   THE TAP (role button, its expanded state announced as the expand / collapse action, as the prototype's
 *   aria-expanded): it opens the quote view under it for this visit, and while folded the second line ends in
 *   `tap to view` (D7: the span goes while open). With no [quote] (a line not at Client approved) there is no tap.
 */
@Composable
private fun ProjectLineBox(line: QuoteStage.ProjectLine, quote: QuoteViewArgs?) {
    val teal = Color(0xFF0F766E)
    val ink = Color(0xFF0F5E57)
    val rule = MaterialTheme.colorScheme.outlineVariant
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val exec = line.kind == QuoteStage.LineKind.EXEC
    val tap = if (exec) quote else null
    val titleStyle = MaterialTheme.typography.titleSmall.copy(
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.4.sp, fontWeight = FontWeight.Medium)
    val secondStyle = MaterialTheme.typography.bodySmall.copy(
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp, fontWeight = FontWeight.Normal)
    val secondColor = MaterialTheme.colorScheme.onSurfaceVariant
    val frame = Modifier
        .fillMaxWidth()
        // .qst.qx: min-height 48 px (36 px in landscape), padding included -- it binds only in landscape
        .heightIn(min = if (!exec) 0.dp else if (landscape) 36.dp else 48.dp)
        .background(MaterialTheme.colorScheme.surface)
        .drawBehind {
            drawRect(teal, size = Size(3.dp.toPx(), size.height))
            val y = size.height - 0.5.dp.toPx()
            drawLine(rule, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        // the tap (QV): the whole line, a button, folded or open as the expand / collapse action says
        .then(if (tap == null) Modifier else Modifier
            .clickable(role = Role.Button, onClick = tap.onToggle)
            .semantics { if (tap.open) collapse { tap.onToggle(); true } else expand { tap.onToggle(); true } })
    if (landscape) {
        Row(
            // the bottom 5 dp, then the 1 dp rule drawBehind paints below it
            frame.padding(start = 16.dp, top = 5.dp, end = 16.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LineHead(line, exec, titleStyle, ink, teal)
            }
            SecondLine(line.second, tap, secondStyle, secondColor, teal)
        }
    } else {
        Column(
            // the bottom 8 dp, then the 1 dp rule drawBehind paints below it
            frame.padding(start = 16.dp, top = 7.dp, end = 16.dp, bottom = 9.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LineHead(line, exec, titleStyle, ink, teal)
            }
            // One line, so the block stays within 52 dp whatever it says.
            SecondLine(line.second, tap, secondStyle, secondColor, teal)
        }
    }
}

/** The second line (.qsince): its text, and at Client approved while folded the teal `tap to view` 8 dp after it (.qv). */
@Composable
private fun SecondLine(second: String, tap: QuoteViewArgs?, style: androidx.compose.ui.text.TextStyle, color: Color, teal: Color) {
    val tx = LocalTx.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(second, style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false))
        if (tap != null && !tap.open) {
            Text(QuoteView.tapToView(tx), style = style, color = teal, maxLines = 1,
                modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/**
 * THE QUOTE VIEW at Client approved (port 61c slice QV; SPEC61c §4.1 item 3, index61c2.html stripHTML `op56`): what
 * the project line's tap opens, this visit only -- a reload starts folded. [open] draws the view under the line;
 * [onToggle] is the line's and the Approved chip's tap; [refusal] the pending refusal line for this project (critic
 * G5: a Record S5 refused, and from QM1 a move), amber under the steps when open and alone when folded, null for
 * none; [row] the APPROVED SCOPE row (QuoteView.scopeRow: the baseline's figure with its tap, or `none recorded`
 * with Record for the PM, the PM-only line for anyone else); [onViewScope] opens the scope sheet read-only;
 * [onRecord] opens it to record.
 */
class QuoteViewArgs(
    val open: Boolean,
    val onToggle: () -> Unit,
    val refusal: String?,
    val row: QuoteView.ScopeRow,
    val onViewScope: () -> Unit,
    val onRecord: () -> Unit,
)

/**
 * The strip under the line at Client approved (.p54 with `op56`): folded, nothing but a pending refusal line
 * (.p54msg.solo); open, the five ✓ steps and the Approved chip (a tap folds), the refusal line if any, then the
 * APPROVED SCOPE row. Sizes are the prototype's: the strip padded 2/4/4 over its 1 dp rule; a step 48 dp tall with a
 * 24 dp circle (1.5 dp teal border, a teal ✓) and its name at 10/16 sp; a 1.5 dp connector at 16 dp from the first
 * circle to the last; in landscape one 44 dp row of 40 dp pills (22 dp circles, the steps' names spoken only, as
 * .scaf.land .p54:has(.p54x) hides them) with the row beside them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuoteStrip(q: QuoteViewArgs) {
    val tx = LocalTx.current
    val teal = Color(0xFF0F766E)
    val ink = Color(0xFF0F5E57)
    val amber = Color(0xFFB0740E)
    val rule = MaterialTheme.colorScheme.outlineVariant
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val small = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp)
    if (!q.open) {
        q.refusal?.let {
            Text(it, style = small, color = amber,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite })
        }
        return
    }
    val segs = QuoteView.segments(tx)
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .drawBehind {
                val y = size.height - 0.5.dp.toPx()
                drawLine(rule, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            .padding(start = 4.dp, top = 2.dp, end = 4.dp, bottom = 5.dp),
    ) {
        if (landscape) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (s in segs) StepPill(s.label, teal)
                ApprovedPill(tx, q.onToggle, teal, ink)
                Spacer(Modifier.width(6.dp))
                ScopeRowBlock(q, Modifier.weight(1f), tx, ink, small)
            }
        } else {
            val n = segs.size + 1
            Row(
                Modifier.fillMaxWidth().drawBehind {
                    // the connector between the circles' centres, 16 dp down (4 dp padding + half the 24 dp circle)
                    val y = 16.dp.toPx()
                    val half = size.width / (2 * n)
                    drawLine(rule, Offset(half, y), Offset(size.width - half, y), strokeWidth = 1.5.dp.toPx())
                },
                verticalAlignment = Alignment.Top,
            ) {
                for (s in segs) Step(s.name, s.label, teal, Modifier.weight(1f))
                ApprovedStep(tx, q.onToggle, teal, ink, Modifier.weight(1f))
            }
        }
        q.refusal?.let {
            Text(it, style = small, color = amber,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp, end = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite })
        }
        if (!landscape) ScopeRowBlock(q, Modifier.fillMaxWidth(), tx, ink, small)
    }
}

/** A ticked step in portrait: the 24 dp circle with its teal ✓ over the name, one TalkBack node saying `Phase n of 5: name`. */
@Composable
private fun Step(name: String, label: String, teal: Color, modifier: Modifier) {
    Column(
        modifier.heightIn(min = 48.dp).padding(top = 4.dp, bottom = 2.dp)
            .semantics(mergeDescendants = true) { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Tick(24.dp, filled = false, teal)
        Text(name, fontSize = 10.sp, lineHeight = 16.sp, letterSpacing = (-0.2).sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The Approved chip in portrait (.p54appr.on): a filled teal circle with ✓ over `Approved` in bold ink; a tap folds the view. */
@Composable
private fun ApprovedStep(tx: Tx, onFold: () -> Unit, teal: Color, ink: Color, modifier: Modifier) {
    Column(
        modifier.heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onFold)
            .padding(top = 4.dp, bottom = 2.dp)
            .semantics(mergeDescendants = true) { contentDescription = QuoteView.approvedChipLabel(tx) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Tick(24.dp, filled = true, teal)
        Text(QuoteView.approvedChip(tx), fontSize = 10.sp, lineHeight = 16.sp, letterSpacing = (-0.2).sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = ink)
    }
}

/** A ticked step in landscape: a 40 dp pill holding the 22 dp circle alone, its name spoken (`Phase n of 5: name`). */
@Composable
private fun StepPill(label: String, teal: Color) {
    Box(
        Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(20.dp)).padding(horizontal = 9.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Tick(22.dp, filled = false, teal) }
}

/** The Approved chip in landscape (.scaf.land .p54appr): a tinted 40 dp pill, the filled circle and `Approved`; a tap folds. */
@Composable
private fun ApprovedPill(tx: Tx, onFold: () -> Unit, teal: Color, ink: Color) {
    Row(
        Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFFE3F1EF))
            .clickable(role = Role.Button, onClick = onFold).padding(horizontal = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = QuoteView.approvedChipLabel(tx) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Tick(22.dp, filled = true, teal)
        Text(QuoteView.approvedChip(tx), fontSize = 10.sp, lineHeight = 16.sp, letterSpacing = (-0.2).sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1, color = ink)
    }
}

/** The step's circle (.p54n): [size] across, a 1.5 dp teal border with a teal ✓, or filled teal with a white ✓. */
@Composable
private fun Tick(size: androidx.compose.ui.unit.Dp, filled: Boolean, teal: Color) {
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(if (filled) teal else MaterialTheme.colorScheme.surface)
            .then(if (filled) Modifier else Modifier.border(BorderStroke(1.5.dp, teal), CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        Text("✓", color = if (filled) Color.White else teal, fontSize = 12.sp, lineHeight = 12.sp) // text:value the prototype's .p54n glyph, in both languages
    }
}

/**
 * The APPROVED SCOPE row (.p54x.p61sc, wrapping): its label, then the baseline's figure as the tap that opens the sheet
 * read-only, or `none recorded` with the Record button (36 dp, filled teal, .p54pk.pri) for the PM, or the PM-only line.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScopeRowBlock(q: QuoteViewArgs, modifier: Modifier, tx: Tx, ink: Color, small: androidx.compose.ui.text.TextStyle) {
    val row = q.row
    FlowRow(
        modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(QuoteView.scopeLabel(tx), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp),
            color = ink, modifier = Modifier.align(Alignment.CenterVertically))
        if (row.tap) {
            Text(row.text, style = small, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterVertically).clip(RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button, onClick = q.onViewScope)
                    .heightIn(min = 36.dp).padding(horizontal = 4.dp, vertical = 8.dp))
        } else {
            Text(row.text, style = small, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterVertically))
            if (row.record) {
                Button(
                    onClick = q.onRecord,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E), contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.align(Alignment.CenterVertically).heightIn(min = 36.dp),
                ) { Text(tx.phase_scope_record, fontSize = 13.sp, maxLines = 1) }
            }
            row.pmOnly?.let {
                Text(it, style = small, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically))
            }
        }
    }
}

/** The line's first row: the ✓ and the line at Client approved, else the line and its six pips. */
@Composable
private fun RowScope.LineHead(
    line: QuoteStage.ProjectLine,
    exec: Boolean,
    style: androidx.compose.ui.text.TextStyle,
    ink: Color,
    teal: Color,
) {
    if (exec) ApprovedCheck(teal)
    Text(line.text, style = style, color = ink, maxLines = 1,
        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    if (!exec) QuotePips(line.lit, line.pips, teal)
}

/** The prototype's .qck: an 18 dp teal disc with a white ✓ at 11/18 sp, hidden from TalkBack as it is aria-hidden there. */
@Composable
private fun ApprovedCheck(teal: Color) {
    Box(
        Modifier.size(18.dp).clip(CircleShape).background(teal).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text("✓", color = Color.White, fontSize = 11.sp, lineHeight = 18.sp) // text:value the prototype's .qck glyph, in both languages
    }
}

/**
 * The six quote stages (v61b) as 12 x 4 dp steps, lit up to and including the current one. TalkBack reads
 * them as one node saying [label] (QuoteStage.ProjectLine.pips: `2 of 6`, `not read yet`, `not on S5 yet`),
 * the prototype's aria-label on .qpips (slice PL review: the place in the six was lost).
 */
@Composable
private fun QuotePips(lit: Int, label: String, on: Color) {
    val off = MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier.semantics(mergeDescendants = true) { contentDescription = label },
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
    /** The dot's colour: green when true, amber when false (QuoteStage.roomDot). */
    green: Boolean,
    pano: String?,
    /** The guided tour's name for this tile (`room:<stored name>`, port 61c §31) — the demo room only. */
    anchor: String? = null,
    onClick: () -> Unit,
) {
    val tx = LocalTx.current
    // The room as the masters name it in the language (Living Room -> हॉल under मराठी); the token stays the English's
    val shown = Masters.room(tx, room)
    val shot = count > 0
    val photo = shot && pano != null
    Box(
        Modifier
            .aspectRatio(1f)
            .tourAnchor(anchor)
            .background(
                if (shot) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
    ) {
        if (photo) {
            Thumb(ThumbSource.LocalFile(pano!!), Modifier.fillMaxSize(),
                target = 260, contentDescription = shown)
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
            // The derived dot (SPEC61c §3.2, QuoteStage.roomDot): before approval green
            // means the room has a survey capture, amber photos but none from the
            // survey; after it, green means a capture filed to one of the sixteen
            // works. That is the question on site: what still needs shooting.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .size(9.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (green) Color(0xFF3F8F5B) else Color(0xFFB0740E)))
            Text("$count", style = MaterialTheme.typography.labelSmall,
                color = if (photo) Color.White
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp))
        }
        Text(shown, style = MaterialTheme.typography.labelSmall,
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
