package com.malletcrafts.interiorestimate

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.malletcrafts.interiorestimate.pano.QuoteStage
import com.malletcrafts.interiorestimate.pano.WishList

/**
 * THE CLIENT'S WISH LIST (v52 item 16, slice 5b). Amit, 14 Oct 2026, 06:35 IST: "also as you
 * have services list , i should be able to quickly mark scope while talking with different
 * service vendor using this app ... this is kind of wish list client will have and vendor will
 * fulfil"; 06:36 IST: "but the wishlist should never get mixed with survey as this wishlist
 * comes after survey".
 *
 * A full screen reached from the PROJECT's chip row only (never a room, a capture or survey
 * prep), with no bottom bar -- a sub-screen, like a capture. The prototype (v52 wish.js) is the
 * spec: the title 'Wish list · <title>', the sub-line the quote stage, a fixed head (what the
 * list is, 'N of 12 wanted', where it is saved, the banners) and the twelve services under it.
 * THE LIST SCROLLS, as the prototype's does (wish.js puts the rows in its scroll pager, and
 * ct52 W asserts "it scrolls up and down"): twelve rows of at least 56 dp, one of them open,
 * do not fit 780 dp. The project screen keeps its no-scroll rule; this one is a list.
 *
 * The stage rules are WishList's: read-only at Survey and at Estimated (v61b: the prototype's
 * "Estimation — the wish list is read-only" banner), editable at every other stage and when the
 * stage is not read, and from Vendor quoted on an edit warns ([warned], held by the caller for
 * this visit) and still saves. Every edit goes straight to WishSync on this phone.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WishListScreen(
    project: Catalogue.Project,
    /** The rooms the project's grid shows, in its order: the APPLIES TO chips after Whole flat. */
    rooms: List<String>,
    /** The quote line as QuoteStageSync last read it: its title is this screen's sub-line. */
    quote: QuoteStage.Line,
    /** The quote stage's place (WishList.stageIndex), null when it is not read. */
    stageIdx: Int?,
    /** The project is not on S5 (phone-only, or S5 does not list it): no "not read yet" banner (WishList.notOnS5). */
    notOnS5: Boolean,
    /** Bumped by any write to the lists' preferences (a sync, a read on project open). */
    tick: Int,
    /** Changed after a vendor quoted, this visit: the warning shows. */
    warned: Boolean,
    onWarn: () -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
) {
    val context = LocalContext.current
    val key = WishList.keyOf(project.serverId, project.client, project.title)
    var list by remember(key) { mutableStateOf(WishSync.load(context, key)) }
    // A write from elsewhere -- the sync taking S5's copy, a 409, the read on open -- redraws.
    LaunchedEffect(key, tick) { list = WishSync.load(context, key) }
    val editable = WishList.editable(stageIdx)
    val rows = list?.rows ?: emptyMap()
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val ink = Color(0xFF0F5E57)

    fun change(f: (Map<String, WishList.Row>) -> Map<String, WishList.Row>) {
        if (!editable) return
        list = WishSync.edit(context, key, project.client, project.title, f)
        if (WishList.warnAfterQuote(stageIdx)) onWarn()
    }

    Scaffold(
        topBar = {
            TreeTopBar(
                title = "Wish list · ${project.title}",
                subtitle = quote.title,
                onSearch = onSearch,
                onBack = onBack)
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // The head stays put while the list scrolls (wish.css .whead: 8/16/8, a rule under it).
            Column(
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(WishList.HEAD, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(WishList.total(WishList.count(rows)),
                        style = MaterialTheme.typography.titleSmall, color = ink)
                    Text(WishList.savedText(list), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            list?.note?.takeIf { it.isNotBlank() }?.let { Banner(it) }
            WishList.banners(stageIdx, warned, notOnS5).forEach { Banner(it) }
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(WishList.SERVICES, key = { _, s -> s.id }) { i, s ->
                    WishRow(
                        n = i + 1,
                        service = s,
                        row = WishList.row(rows, s.id),
                        editable = editable,
                        rooms = rooms,
                        landscape = landscape,
                        onToggle = { change { WishList.toggle(it, s.id) } },
                        onWholeFlat = { change { WishList.wholeFlat(it, s.id) } },
                        onRoom = { r -> change { WishList.toggleRoom(it, s.id, r, rooms) } },
                        onNote = { v -> change { WishList.setNote(it, s.id, v) } },
                        onVendor = { v -> change { WishList.setVendor(it, s.id, v) } })
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

/**
 * One service (wish.css .wrow): at least 56 dp, #EEF7F5 when wanted. A tap on the head ticks
 * it unless the list is read-only; a wanted row opens to APPLIES TO (Whole flat, then the
 * project's rooms), a one-line Note and a Vendor field -- side by side in landscape.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WishRow(
    n: Int,
    service: WishList.Service,
    row: WishList.Row,
    editable: Boolean,
    rooms: List<String>,
    landscape: Boolean,
    onToggle: () -> Unit,
    onWholeFlat: () -> Unit,
    onRoom: (String) -> Unit,
    onNote: (String) -> Unit,
    onVendor: (String) -> Unit,
) {
    val bg = if (row.on) Color(0xFFEEF7F5) else MaterialTheme.colorScheme.surfaceContainerLow
    Column(Modifier.fillMaxWidth().background(bg)) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(enabled = editable, onClick = onToggle)
                .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("$n", modifier = Modifier.width(20.dp), textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            // The row is the target (as the prototype's whole head is), so the box itself takes no tap.
            Checkbox(checked = row.on, onCheckedChange = null, enabled = editable)
            Column(Modifier.weight(1f)) {
                Text(service.name, style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(WishList.rowLine(service, row), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (row.on && editable) {
            Column(Modifier.fillMaxWidth().padding(start = 32.dp, end = 16.dp, bottom = 10.dp)) {
                Text(WishList.APPLIES_TO, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(selected = row.rooms.isEmpty(), onClick = onWholeFlat,
                        label = { Text(WishList.WHOLE_FLAT) })
                    rooms.forEach { r ->
                        FilterChip(selected = r in row.rooms, onClick = { onRoom(r) },
                            label = { Text(r, maxLines = 1, overflow = TextOverflow.Ellipsis) })
                    }
                }
                if (landscape) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NoteField(row.note, onNote, Modifier.weight(1f))
                        VendorField(row.vendor, onVendor, Modifier.weight(1f))
                    }
                } else {
                    NoteField(row.note, onNote, Modifier.fillMaxWidth())
                    VendorField(row.vendor, onVendor, Modifier.fillMaxWidth())
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun NoteField(value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text("Note") },
        placeholder = { Text("one line — what exactly") },
        singleLine = true,
        modifier = modifier.padding(top = 12.dp))
}

@Composable
private fun VendorField(value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text("Vendor") },
        placeholder = { Text("filled in when a vendor is chosen") },
        singleLine = true,
        modifier = modifier.padding(top = 12.dp))
}
