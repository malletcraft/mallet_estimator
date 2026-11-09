package com.malletcrafts.interiorestimate

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malletcrafts.interiorestimate.pano.Album
import com.malletcrafts.interiorestimate.pano.AlbumPage

/**
 * THE PROJECT ALBUM, READ-ONLY (port 61c slice K3, 9 Nov 2026; DESIGN-port61c.md §18): the prototype v61c.2's album
 * (index61c2.html window.__mcftAlbum60, its CSS 1446-1583) drawn from [AlbumPage.Page], which holds every text and rule
 * -- this file has none of its own. A sub-screen in place of the project's rooms, as the Wish list is: the top bar
 * `Album · <title>` over `N photos · survey to handover` with Back and Search, no bottom bar, no crumbs.
 *
 * The header (Viewing as, Open, To review, Change requests, the project line, the Work log) is fixed above the chapters
 * in portrait (`.alb59h` is flex: none) and a 248 dp column of its own that scrolls in landscape, with a 1 dp rule to
 * its right (`.alb59.land`). The chapters are ONE LazyColumn: the empty line, chapter 0 with its quote
 * groups and the staff's unfiled photos, the lock banner, the sixteen works, the end line, and 84 dp at the bottom where
 * K5's Progress shot FAB lands with nothing reflowing (`.alb59body`). A grid is rows of cards inside it (2 across, 3 in
 * landscape), since a lazy grid cannot nest in a lazy list.
 *
 * NOTHING HERE WRITES (the plan's table "THE CONTROLS THAT WRITE"): the state chip is the prototype's chip for someone it
 * offers no sheet to (no ▾, no tap); Accept / Not for client, the change-request buttons, the result line and the FAB
 * are absent in the slots K4 and K5 fill -- never drawn disabled, which would read as broken.
 *
 * The photo viewer is drawn over the whole screen ([view] is the item's key): dark, `Close`, the title over `Photo i
 * of N`, the photo FIT (no pinch: the prototype's viewer has none, and ZoomableImage carries an unkeyed hint), the
 * facts, ‹ ›. In landscape the photo and ‹ › take the left and the facts a 300 dp column on the right (the prototype's
 * later `.alb59v.land` rules, 1580-1583, override the 260 px of 1518 -- the K3 critic's finding 3, declined). Back
 * closes it first ([BackHandler] enabled only while it is open, so the album's own Back reaches the project after).
 */
@Composable
fun AlbumScreen(
    /** The S5 id the album is read under (WishSync.stageKey), "" for a project S5 does not list: S5's pictures are fetched under it. */
    project: String,
    page: AlbumPage.Page,
    /** The phone's own file of a capture it holds, by device id, when the file is there. */
    localPath: (String) -> String?,
    /** The key of the item open in the viewer, or null. */
    view: String?,
    /**
     * The chapters' scroll, the visit's: held by MainActivity above the search overlay, so Search then Back returns to
     * the album where it was (the K3 review, finding 5), and a new one for each visit.
     */
    listState: LazyListState,
    onView: (String?) -> Unit,
    /** A chapter's header tapped: its number, and whether it is now open. */
    onToggle: (Int, Boolean) -> Unit,
    onLog: () -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
) {
    val tx = LocalTx.current
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val shown = view?.let { AlbumPage.shown(tx, page, it) }
    // An item gone from the walk (a read moved it out of sight) closes the viewer, as the prototype's viewHTML does.
    LaunchedEffect(view, shown == null) { if (view != null && shown == null) onView(null) }
    BackHandler(enabled = view != null) { onView(null) }
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { TreeTopBar(title = page.title, subtitle = page.sub, onSearch = onSearch, onBack = onBack) },
        ) { pad ->
            if (landscape) {
                Row(Modifier.padding(pad).fillMaxSize()) {
                    Column(Modifier.width(248.dp).fillMaxHeight().background(cs.background).verticalScroll(rememberScrollState())) {
                        AlbumHeader(page.header, onLog, capLog = false)
                    }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(cs.outlineVariant))
                    AlbumBody(project, page, localPath, listState, cols = 3, onView = onView, onToggle = onToggle, modifier = Modifier.weight(1f))
                }
            } else {
                Column(Modifier.padding(pad).fillMaxSize()) {
                    Column(Modifier.fillMaxWidth().background(cs.background)) { AlbumHeader(page.header, onLog, capLog = true) }
                    AlbumBody(project, page, localPath, listState, cols = 2, onView = onView, onToggle = onToggle, modifier = Modifier.weight(1f))
                }
            }
        }
        if (shown != null) AlbumViewer(project, page, shown, localPath, landscape, onView)
    }
}

/** The Album tile's arguments for the rooms grid: what it says ([AlbumPage.tile]) and what a tap opens. */
class AlbumTileArgs(val tile: AlbumPage.Tile, val onOpen: () -> Unit)

/**
 * THE ALBUM TILE, the last child of the project's rooms grid (`.tile.alb59t`, index61c2.html 1448-1451, 7385-7387): the
 * tile's square in the tertiary container colour, the album glyph (30 x 24 dp) 8 dp from the top left, the count of the
 * photos this person may see bottom right and `Album` bottom left (labelSmall, 7 dp in, 72 % wide, one line). TalkBack
 * reads `Album of <title>: N photos, survey to handover`.
 */
@Composable
fun AlbumTile(a: AlbumTileArgs) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .aspectRatio(1f)
            .background(cs.tertiaryContainer)
            .clickable(role = Role.Button, onClick = a.onOpen)
            .clearAndSetSemantics { contentDescription = a.tile.aria; role = Role.Button; onClick { a.onOpen(); true } },
    ) {
        AlbumGlyph(Modifier.align(Alignment.TopStart).padding(8.dp).size(width = 30.dp, height = 24.dp), cs.onTertiaryContainer, cs.tertiaryContainer)
        Text(a.tile.count.toString(), style = MaterialTheme.typography.labelSmall, color = cs.onTertiaryContainer,
            modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp))
        Text(a.tile.label, style = MaterialTheme.typography.labelSmall, color = cs.onTertiaryContainer, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(7.dp).fillMaxWidth(0.72f))
    }
}

/** The prototype's GLYPH (7363): two offset frames, the front one filled, and a hill, in a 30 x 24 box, 1.6 strokes. */
@Composable
private fun AlbumGlyph(modifier: Modifier, ink: Color, fill: Color) {
    Box(modifier.drawBehind {
        val u = size.width / 30f
        val stroke = Stroke(width = 1.6f * u, join = StrokeJoin.Round)
        drawRoundRect(ink, topLeft = Offset(6f * u, 1f * u), size = Size(22f * u, 16f * u), cornerRadius = CornerRadius(2f * u), style = stroke)
        drawRoundRect(fill, topLeft = Offset(2f * u, 5f * u), size = Size(22f * u, 16f * u), cornerRadius = CornerRadius(2f * u))
        drawRoundRect(ink, topLeft = Offset(2f * u, 5f * u), size = Size(22f * u, 16f * u), cornerRadius = CornerRadius(2f * u), style = stroke)
        val hill = Path().apply {
            moveTo(5f * u, 18f * u); lineTo(10f * u, 12f * u); lineTo(14f * u, 16f * u); lineTo(17f * u, 13f * u); lineTo(22f * u, 18f * u)
        }
        drawPath(hill, ink, style = stroke)
    })
}

// ---- the header -------------------------------------------------------------------------------------------------

/**
 * The header (index61c2.html 7405-7423, CSS 1454-1468, 1544-1548, 1414): `Viewing as …` (min 40 dp, surfaceContainer,
 * labelSmall; no ▾ and no tap -- the role is S5's word, never switched), `Open: …` (primaryContainer, titleSmall, 10/16),
 * `To review: N` (#5C3A00 on #FFE3B0) and `Change requests: N` (#5C3A00 on #FDF0D5), 6/16; the project line (6/16,
 * onSurfaceVariant on surfaceContainer); the Work log toggle (min 40 dp, primary labelLarge, a 16 dp ▾ / ▴) and its rows,
 * at most 168 dp tall and scrolling in portrait ([capLog]), uncapped in landscape where the whole column scrolls.
 */
@Composable
private fun AlbumHeader(h: AlbumPage.Header, onLog: () -> Unit, capLog: Boolean) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    h.viewingAs?.let {
        Box(Modifier.fillMaxWidth().heightIn(min = 40.dp).background(cs.surfaceContainer).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart) {
            Text(it, style = ty.labelSmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    Box(Modifier.fillMaxWidth().background(cs.primaryContainer).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(h.open, style = ty.titleSmall, color = cs.onPrimaryContainer)
    }
    h.toReview?.let {
        Text(it, style = ty.bodySmall, color = AMBER_INK,
            modifier = Modifier.fillMaxWidth().background(AMBER).padding(horizontal = 16.dp, vertical = 6.dp))
    }
    h.changeRequests?.let {
        Text(it, style = ty.bodySmall, color = AMBER_INK,
            modifier = Modifier.fillMaxWidth().background(CR_BG).padding(horizontal = 16.dp, vertical = 6.dp))
    }
    Text(h.line, style = ty.bodySmall, color = cs.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().background(cs.surfaceContainer).padding(horizontal = 16.dp, vertical = 6.dp))
    val log = h.log
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clickable(role = Role.Button, onClick = onLog)
            .semantics { if (log.open) collapse { onLog(); true } else expand { onLog(); true } }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(log.toggle, style = ty.labelLarge, color = cs.primary, modifier = Modifier.weight(1f))
        Caret(log.open)
    }
    if (log.open) {
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (capLog) Modifier.heightIn(max = 168.dp).verticalScroll(rememberScrollState()) else Modifier)
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        ) {
            for (r in log.rows) LogRowView(r)
            log.empty?.let { Text(it, style = ty.bodySmall, color = cs.onSurfaceVariant) }
        }
    }
}

/** One Work log row (`.alb59le`): 6 dp above and below under a 1 dp outlineVariant rule. */
@Composable
private fun LogRowView(r: AlbumPage.LogRow) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val rule = cs.outlineVariant
    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind { drawLine(rule, Offset(0f, 0.5.dp.toPx()), Offset(size.width, 0.5.dp.toPx()), strokeWidth = 1.dp.toPx()) }
            .padding(vertical = 6.dp),
    ) {
        Text(r.head, style = ty.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (r.sub.isNotBlank()) Text(r.sub, style = ty.bodySmall, color = cs.onSurfaceVariant)
        if (r.remark.isNotBlank()) Text(r.remark, style = ty.bodySmall, color = cs.onSurface)
    }
}

// ---- the chapters -----------------------------------------------------------------------------------------------

/** The prototype's amber pair (#5C3A00 on #FFE3B0: To review, the banner, On hold, a vendor's chip) and the change request's paler ground. */
private val AMBER = Color(0xFFFFE3B0)
private val AMBER_INK = Color(0xFF5C3A00)
private val CR_BG = Color(0xFFFDF0D5)
private val AMBER_RING = Color(0xFFC98A1C)

@Composable
private fun AlbumBody(
    project: String, page: AlbumPage.Page, localPath: (String) -> String?, listState: LazyListState, cols: Int,
    onView: (String?) -> Unit, onToggle: (Int, Boolean) -> Unit, modifier: Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val card: @Composable (String) -> Unit = { k -> AlbumCard(project, page, k, localPath, onView) }
    LazyColumn(modifier.fillMaxWidth(), state = listState, contentPadding = PaddingValues(bottom = 84.dp)) {
        page.empty?.let { e ->
            item(key = "empty") { Text(e, style = ty.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
        }
        val q = page.quote
        item(key = "ch0") {
            Column(Modifier.fillMaxWidth()) {
                HorizontalDivider(thickness = 1.dp, color = cs.outlineVariant)
                QuoteHeader(q, onToggle)
            }
        }
        if (q.open) {
            item(key = "ch0sub") {
                Text(q.sub, style = ty.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 46.dp, end = 16.dp, bottom = 6.dp))
            }
            for (g in q.groups) {
                if (g.items.isEmpty()) {
                    item(key = "ge:" + g.stage) { EmptyGroup(g) }
                } else {
                    item(key = "gh:" + g.stage) { GroupHead(g.name, g.since, g.now, g.count) }
                    gridRows(g.items, cols, "g:" + g.stage, card)
                    item(key = "gp:" + g.stage) { Spacer(Modifier.height(8.dp)) }
                }
            }
            val uh = q.unfiledHead
            if (uh != null) {
                item(key = "uh") { GroupHead(uh, null, null, q.unfiledCount) }
                gridRows(q.unfiled, cols, "u", card)
                item(key = "up") { Spacer(Modifier.height(8.dp)) }
            }
        }
        page.banner?.let { b -> item(key = "banner") { LockBanner(b) } }
        for (w in page.works) {
            item(key = "w" + w.n) {
                Column(Modifier.fillMaxWidth()) {
                    HorizontalDivider(thickness = 1.dp, color = cs.outlineVariant)
                    WorkHeader(w, onToggle)
                }
            }
            if (w.open) {
                item(key = "wb" + w.n) { WorkBody(w) }
                val none = w.empty
                if (none != null) {
                    item(key = "wn" + w.n) {
                        Text(none, style = ty.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 46.dp, end = 12.dp, bottom = 8.dp))
                    }
                } else {
                    // nothing under a work's grid: `.alb59chb` has no rule and `.alb59grid` pads only its sides (1486),
                    // so the last card meets the next chapter's rule -- only a quote group (`.alb59sg`, 1479) keeps 8 dp
                    // (the K3 review, finding 1)
                    gridRows(w.items, cols, "w" + w.n, card)
                }
            }
        }
        item(key = "end") { EndLine(page.end) }
    }
}

/** A grid as rows of [cols] cards (`.alb59grid`: 6 dp gaps, 8 dp sides), each row one item of the list. */
private fun LazyListScope.gridRows(keys: List<String>, cols: Int, prefix: String, card: @Composable (String) -> Unit) {
    keys.chunked(cols).forEachIndexed { i, row ->
        item(key = prefix + "#" + i) {
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = if (i == 0) 0.dp else 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (k in row) Box(Modifier.weight(1f)) { card(k) }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** A chapter's number (`.alb59n`): a 24 dp circle, 12 sp medium, surfaceVariant -- primary when it is now. */
@Composable
private fun NumberCircle(n: Int, now: Boolean) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.size(24.dp).clip(CircleShape).background(if (now) cs.primary else cs.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(n.toString(), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, color = if (now) cs.onPrimary else cs.onSurfaceVariant)
    }
}

/** The ▾ / ▴ caret (`.alb59car`): 16 dp wide, onSurfaceVariant -- ▴ when open, ▾ otherwise, locked included (the K3 critic, finding 4). */
@Composable
private fun Caret(open: Boolean) {
    Text(if (open) "▴" else "▾", modifier = Modifier.width(16.dp), textAlign = TextAlign.Center, // text:value the prototype's caret glyphs
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A header row's TalkBack: the prototype's aria-label alone, a button that expands or collapses -- or, locked, disabled. */
private fun Modifier.chapterTap(aria: String, locked: Boolean, open: Boolean, toggle: (Boolean) -> Unit): Modifier =
    if (locked) clearAndSetSemantics { contentDescription = aria; disabled() }
    else clickable(role = Role.Button) { toggle(!open) }.clearAndSetSemantics {
        contentDescription = aria
        role = Role.Button
        onClick { toggle(!open); true }
        if (open) collapse { toggle(false); true } else expand { toggle(true); true }
    }

/** Chapter 0's header (`.alb59chh`): min 48 dp, 12 dp sides, 10 dp gaps -- the number, `Quote journey`, its count, the caret. */
@Composable
private fun QuoteHeader(q: AlbumPage.QuoteCh, onToggle: (Int, Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).chapterTap(q.aria, q.locked, q.open) { onToggle(0, it) }.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NumberCircle(0, q.now)
        Text(q.name, style = MaterialTheme.typography.titleSmall, color = if (q.locked) cs.onSurfaceVariant else cs.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(q.count, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        Caret(q.open)
    }
}

/** The `now` pill (`.alb59now`): 8 dp round, 6 dp sides, 11 sp medium, primary. */
@Composable
private fun NowPill(text: String) {
    val cs = MaterialTheme.colorScheme
    Text(text, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, color = cs.onPrimary,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(cs.primary).padding(horizontal = 6.dp))
}

/** A quote group's head with photos (`.alb59sgh`, 6/12/4/46 dp): the stage, `since …`, `now`, and the count at the right. */
@Composable
private fun GroupHead(name: String, since: String?, now: String?, count: String?) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    Row(
        Modifier.fillMaxWidth().padding(start = 46.dp, top = 6.dp, end = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name, style = ty.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            since?.let { Text(it, style = ty.bodySmall, color = cs.onSurfaceVariant, maxLines = 1) }
            now?.let { NowPill(it) }
        }
        count?.let { Text(it, style = ty.bodySmall, color = cs.onSurfaceVariant) }
    }
}

/** An empty quote group (`.alb59es`): min 28 dp from 46 dp -- the stage, `not reached yet` or `no photos`, and `now`. */
@Composable
private fun EmptyGroup(g: AlbumPage.Group) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    Row(
        Modifier.fillMaxWidth().heightIn(min = 28.dp).padding(start = 46.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(g.name, style = ty.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false))
        g.empty?.let { Text(it, style = ty.bodySmall, color = cs.onSurfaceVariant) }
        g.now?.let { NowPill(it) }
    }
}

/** The lock banner (`.alb60lockbanner`): 8/12 dp out, 10/12 dp in, 12 dp round, #5C3A00 on #FFE3B0, labelLarge. */
@Composable
private fun LockBanner(text: String) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).clip(RoundedCornerShape(12.dp)).background(AMBER)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = AMBER_INK)
    }
}

/**
 * A work's header (`.alb60chh`, 8 dp at the right, 4 dp gaps): the tap target (min 56 dp, 12 dp sides, 10 dp gaps -- the
 * number, the work over its line, the caret), then the live change request's chip and the state chip. The name is
 * onSurfaceVariant while locked, #B07A00 On hold, at .6 Abandoned.
 */
@Composable
private fun WorkHeader(w: AlbumPage.WorkCh, onToggle: (Int, Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val nameColor = when {
        w.locked -> cs.onSurfaceVariant
        w.state == Album.ON_HOLD -> Color(0xFFB07A00)
        else -> cs.onSurface
    }
    Row(
        Modifier.fillMaxWidth().padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            Modifier.weight(1f).heightIn(min = 56.dp).chapterTap(w.aria, w.locked, w.open) { onToggle(w.n, it) }.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NumberCircle(w.n, w.now)
            Column(Modifier.weight(1f)) {
                Text(w.name, style = ty.titleSmall, color = nameColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(if (!w.locked && w.state == Album.ABANDONED) .6f else 1f))
                Text(w.line, style = ty.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Caret(w.open)
        }
        w.crChip?.let { chip ->
            Box(
                Modifier.padding(end = 6.dp).height(28.dp).clip(RoundedCornerShape(14.dp)).background(CR_BG)
                    .clearAndSetSemantics { w.crAria?.let { contentDescription = it } }.padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(chip, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, color = AMBER_INK, maxLines = 1)
            }
        }
        StateChip(w)
    }
}

/**
 * The state chip (`.alb60st`), as the prototype draws it for a person it offers no sheet to (K3 offers it to nobody):
 * 28 dp, 14 dp round, 10 dp sides, 12 sp medium -- Not started a 1 dp outline ring, In progress
 * tertiaryContainer, Done primary, On hold #5C3A00 on #FFE3B0, Abandoned surfaceVariant struck through at .75.
 */
@Composable
private fun StateChip(w: AlbumPage.WorkCh) {
    val cs = MaterialTheme.colorScheme
    val bg: Color
    val fg: Color
    var ring = false
    when (w.state) {
        Album.IN_PROGRESS -> { bg = cs.tertiaryContainer; fg = cs.onTertiaryContainer }
        Album.DONE -> { bg = cs.primary; fg = cs.onPrimary }
        Album.ON_HOLD -> { bg = AMBER; fg = AMBER_INK }
        Album.ABANDONED -> { bg = cs.surfaceVariant; fg = cs.onSurfaceVariant }
        else -> { bg = Color.Transparent; fg = cs.onSurfaceVariant; ring = true }
    }
    val ab = w.state == Album.ABANDONED
    Box(
        Modifier
            .alpha(if (ab) .75f else 1f)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .then(if (ring) Modifier.border(1.dp, cs.outline, RoundedCornerShape(14.dp)) else Modifier)
            .clearAndSetSemantics { contentDescription = w.stateAria }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(w.stateText, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, color = fg, maxLines = 1,
            textDecoration = if (ab) TextDecoration.LineThrough else null)
    }
}

/**
 * An open work's lines before its grid (`.alb59chb`): Payment (28 dp, 8 dp round, surfaceContainer, 46 dp in, 12 dp
 * right, 6 dp below), the live change request's raise line (4/16 dp, #5C3A00), the decided requests (2/16 dp,
 * onSurfaceVariant). The buttons that record or raise a change request are K4's.
 */
@Composable
private fun WorkBody(w: AlbumPage.WorkCh) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier.padding(start = 46.dp, end = 12.dp, bottom = 6.dp).fillMaxWidth().height(28.dp).clip(RoundedCornerShape(8.dp))
                .background(cs.surfaceContainer).clearAndSetSemantics { contentDescription = w.paymentAria }.padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(w.payment, style = ty.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        w.crNote?.let { Text(it, style = ty.bodySmall, color = AMBER_INK, modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp)) }
        if (w.decided.isNotEmpty()) {
            Column(Modifier.padding(top = 6.dp)) {
                for (d in w.decided) Text(d, style = ty.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, top = 2.dp, end = 16.dp))
            }
        }
    }
}

/** The end line (`.alb59end`): min 48 dp, 8/12 dp out, a 2 dp outline rule above, centred; closed, the rule and text primary. */
@Composable
private fun EndLine(e: AlbumPage.End) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val rule = if (e.closed) cs.primary else cs.outline
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 8.dp, end = 12.dp)
            .heightIn(min = 48.dp)
            .drawBehind { drawLine(rule, Offset(0f, 1.dp.toPx()), Offset(size.width, 1.dp.toPx()), strokeWidth = 2.dp.toPx()) },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(e.text, style = ty.bodySmall, color = if (e.closed) cs.primary else cs.onSurfaceVariant, textAlign = TextAlign.Center)
        e.day?.let { Text(AlbumPage.SEP + it, style = ty.bodySmall, color = cs.onSurfaceVariant) }
    }
}

// ---- the card ---------------------------------------------------------------------------------------------------

/**
 * One card (`.alb59c`, 1486-1499, 1567-1571): 8 dp round on surfaceContainerLow with a 1 dp outlineVariant ring (2 dp
 * #C98A1C while a vendor's photo awaits review); the photo 4:3 on surfaceContainerHigh, CROP, the `360` tag 6 dp in; the
 * body 6/7/8 dp with 2 dp gaps: who, the badge, when, the filing, the room. A tap opens the viewer on it.
 */
@Composable
private fun AlbumCard(project: String, page: AlbumPage.Page, key: String, localPath: (String) -> String?, onView: (String?) -> Unit) {
    val line = page.cards[key] ?: return
    val p = page.items[key] ?: return
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val local = if (p.source == Album.Source.PHONE) localPath(p.id) else null
    val pic = AlbumPage.pic(p, local != null)
    val shape = RoundedCornerShape(8.dp)
    val ring = if (line.pending) BorderStroke(2.dp, AMBER_RING) else BorderStroke(1.dp, cs.outlineVariant)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(cs.surfaceContainerLow).border(ring, shape).clickable { onView(key) },
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
            AlbumImage(project, pic, local, fit = false, background = cs.surfaceContainerHigh, modifier = Modifier.fillMaxSize())
            if (line.tag360) Tag360(Modifier.align(Alignment.TopStart).padding(6.dp))
        }
        Column(Modifier.padding(start = 7.dp, top = 6.dp, end = 7.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            WhoChip(line)
            line.badge?.let { b ->
                Text(b, fontSize = 11.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium,
                    color = if (line.pending) AMBER_INK else cs.onSurfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (line.pending) AMBER else cs.surfaceVariant).padding(horizontal = 6.dp))
            }
            Text(line.whenText, style = ty.bodySmall, color = cs.onSurface)
            Text(line.work, style = ty.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(line.room, style = ty.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The `360` corner tag (`.alb59k`): 4 dp round, 5 dp sides, 10 sp bold, white on black at .6. */
@Composable
private fun Tag360(modifier: Modifier) {
    Text(Album.PHOTO_360, fontSize = 10.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
        modifier = modifier.clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = .6f)).padding(horizontal = 5.dp))
}

/**
 * Who (`.alb59who`): 22 dp, 11 dp round, 6/8 dp sides, 11 sp medium with a 12 dp glyph -- staff on surfaceVariant, a
 * vendor #5C3A00 on #FFE3B0 ringed 1 dp #C98A1C. The glyph says `Staff` / `Vendor` to TalkBack, as its title does.
 */
@Composable
private fun WhoChip(line: AlbumPage.CardLine) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(11.dp)
    val bg = if (line.vendor) AMBER else cs.surfaceVariant
    val fg = if (line.vendor) AMBER_INK else cs.onSurfaceVariant
    Row(
        Modifier.height(22.dp).clip(shape).background(bg)
            .then(if (line.vendor) Modifier.border(1.dp, AMBER_RING, shape) else Modifier)
            .padding(start = 6.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        WhoGlyph(line.vendor, fg, Modifier.size(12.dp).semantics { contentDescription = line.whoAria })
        Text(line.who, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The prototype's WHOG (7364-7365): a person, or a briefcase for a vendor, in a 12 x 12 box, 1.4 strokes. */
@Composable
private fun WhoGlyph(vendor: Boolean, ink: Color, modifier: Modifier) {
    Box(modifier.drawBehind {
        val u = size.width / 12f
        val stroke = Stroke(width = 1.4f * u, join = StrokeJoin.Round)
        if (vendor) {
            drawRoundRect(ink, topLeft = Offset(1f * u, 4.5f * u), size = Size(10f * u, 6f * u), cornerRadius = CornerRadius(1f * u), style = stroke)
            drawPath(Path().apply { moveTo(4f * u, 4.5f * u); lineTo(4f * u, 3f * u); lineTo(8f * u, 3f * u); lineTo(8f * u, 4.5f * u) }, ink, style = stroke)
        } else {
            drawCircle(ink, radius = 2.4f * u, center = Offset(6f * u, 4f * u), style = stroke)
            drawPath(Path().apply {
                moveTo(1.5f * u, 11f * u)
                cubicTo(2.1f * u, 8.4f * u, 3.9f * u, 7f * u, 6f * u, 7f * u)
                cubicTo(8.1f * u, 7f * u, 9.9f * u, 8.4f * u, 10.5f * u, 11f * u)
            }, ink, style = stroke)
        }
    })
}

// ---- the viewer -------------------------------------------------------------------------------------------------

private val V_BG = Color(0xFF0B0D0D)
private val V_INK = Color(0xFFEDEFEA)
private val V_MUTE = Color(0xFF9AA29E)
private val V_OFF = Color(0xFF5E6663)

/**
 * The viewer (`.alb59v`, 1504-1522, 1573, 1580-1585): over the album, #0B0D0D, text #EDEFEA. The top row 6/8/6/4 dp:
 * `Close` then the title (titleSmall) over `Photo i of N` (#9AA29E); the photo filling the rest, at least 120 dp, FIT; the
 * facts 8/14 dp with 3 dp gaps, #9AA29E but the caption #EDEFEA bodyMedium; ‹ and › at 22 sp, at least 56 dp, 4/8/10 dp,
 * #5E6663 when there is none. In landscape the photo with ‹ › under it on the left, the facts a 300 dp column on the right.
 * It takes every tap that is not its own, so nothing under it moves.
 */
@Composable
private fun AlbumViewer(
    project: String, page: AlbumPage.Page, s: AlbumPage.Shown, localPath: (String) -> String?, landscape: Boolean, onView: (String?) -> Unit,
) {
    val tx = LocalTx.current
    val p = page.items[s.key] ?: return
    val local = if (p.source == Album.Source.PHONE) localPath(p.id) else null
    val pic = AlbumPage.pic(p, local != null)
    Column(Modifier.fillMaxSize().background(V_BG).pointerInput(Unit) { detectTapGestures { } }) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp, end = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button) { onView(null) }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(tx.common_close, style = MaterialTheme.typography.labelLarge, color = V_INK)
            }
            Column(Modifier.weight(1f)) {
                Text(s.title, style = MaterialTheme.typography.titleSmall, color = V_INK, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(s.count, style = MaterialTheme.typography.bodySmall, color = V_MUTE)
            }
        }
        if (landscape) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    AlbumImage(project, pic, local, fit = true, background = V_BG, modifier = Modifier.weight(1f).fillMaxWidth())
                    ViewerNav(s, onView)
                }
                Column(Modifier.width(300.dp).fillMaxHeight().verticalScroll(rememberScrollState())) { ViewerFacts(s) }
            }
        } else {
            AlbumImage(project, pic, local, fit = true, background = V_BG, modifier = Modifier.weight(1f).heightIn(min = 120.dp).fillMaxWidth())
            ViewerFacts(s)
            ViewerNav(s, onView)
        }
    }
}

/** The viewer's facts (`.alb59vm`): who, when, the room, the chapter line, a live change request, Payment, the caption, the review. */
@Composable
private fun ViewerFacts(s: AlbumPage.Shown) {
    val ty = MaterialTheme.typography
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        WhoChip(s.card)
        Text(s.card.whenText, style = ty.bodySmall, color = V_MUTE)
        Text(s.card.room, style = ty.bodySmall, color = V_MUTE)
        Text(s.chapterLine, style = ty.bodySmall, color = V_MUTE)
        s.cr?.let { Text(it, style = ty.bodySmall, color = V_MUTE) }
        s.payment?.let { Text(it, style = ty.bodySmall, color = V_MUTE) }
        if (s.captionIsData) Text(s.caption, style = ty.bodyMedium, color = V_INK) else Text(s.caption, style = ty.bodySmall, color = V_MUTE)
        s.review?.let { Text(it, style = ty.bodySmall, color = V_MUTE) }
    }
}

/** ‹ and › (`.alb59vn`): 22 sp, at least 56 dp, 4/8/10 dp; #5E6663 and no tap where the walk ends. */
@Composable
private fun ViewerNav(s: AlbumPage.Shown, onView: (String?) -> Unit) {
    val tx = LocalTx.current
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        NavArrow("‹", s.prev, tx.album_aria_previous_photo, onView) // text:value the prototype's arrow glyph
        NavArrow("›", s.next, tx.album_aria_next_photo, onView) // text:value the prototype's arrow glyph
    }
}

@Composable
private fun NavArrow(glyph: String, to: String?, label: String, onView: (String?) -> Unit) {
    Box(
        Modifier
            .widthIn(min = 56.dp)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .then(if (to == null) Modifier else Modifier.clickable(role = Role.Button) { onView(to) })
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                if (to == null) disabled() else onClick { onView(to); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = 22.sp, color = if (to == null) V_OFF else V_INK)
    }
}
