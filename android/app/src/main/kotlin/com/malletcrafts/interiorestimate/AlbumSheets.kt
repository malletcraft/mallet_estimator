package com.malletcrafts.interiorestimate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.malletcrafts.interiorestimate.pano.Album
import com.malletcrafts.interiorestimate.pano.AlbumPage
import com.malletcrafts.interiorestimate.pano.AlbumWrite

/**
 * THE PROJECT MANAGER'S THREE SHEETS (port 61c slice K4; DESIGN-port61c.md §19), drawn from AlbumPage's models -- every
 * text there, no rule here: the state sheet (index61c2.html stateSheet, 7543-7548), the raise sheet (crSheet,
 * 7520-7528) and the client's-answer sheet (crAnswerSheet, 7529-7539), in the prototype's sheet kit (7815-7829) as
 * FW2's WorkSheet draws Which work?: ModalBottomSheet, SheetTitle, one ListItem per row with the selected one on
 * secondaryContainer and the others on the sheet's own surfaceContainerLow (`.li`, `.li.sel`), OutlinedTextField with
 * its label floating (`field()`), the whole sheet one scrolling column (`PG`), 12 dp under the last element. No Cancel:
 * a scrim tap, a swipe or ◁ dismisses and loses what was typed, as `m.sheet=null` does; ◁ closes a sheet before
 * anything else (ModalBottomSheet's own back). A queued write is on the phone the moment it is tapped, so a sheet
 * closes at once and needs no busy state, unlike ScopeSheet. What is typed is the sheet's alone (`remember(work)`).
 * Each sheet opens WHOLE (`skipPartiallyExpanded = true`), as `.sheet3` is drawn: at its content's height up to the
 * frame less 24 px (index61c2.html 278, 381), never half-open (its handle has no drag code). material3's default opens
 * a sheet taller than half the window at half height, which hid the state sheet's foot, the answer sheet's button and,
 * at 780 x 360, every sheet below its first rows (the K4 review, finding 1). Taller than the screen, the column scrolls.
 */

/** `.alb60need`: the state sheet's need line. */
private val NEED = Color(0xFFC0392B)

/** `.alb60crm.warn`: the answer sheet's status line when the date is refused. */
private val WARN = Color(0xFFB0740E)

@Composable
private fun rowColors(selected: Boolean): ListItemColors =
    if (selected) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    else ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)

/**
 * The state sheet: the five states, the current one selected. [onPick] is AlbumWrite.state's outcome: the current
 * state (None) closes the sheet; leaving Done with a blank reason (Refused) draws the need line and stays open; any
 * other pick queues the state and the caller closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumStateSheet(s: AlbumPage.StateSheet, onPick: (String, String) -> AlbumWrite.Out, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    var reason by remember(s.work) { mutableStateOf("") }
    var need by remember(s.work) { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()) {
            SheetTitle(s.title)
            for (r in s.rows) {
                ListItem(
                    headlineContent = { Text(r.text) },
                    colors = rowColors(r.selected),
                    modifier = Modifier.clickableRow { if (onPick(r.state, reason) is AlbumWrite.Out.Refused) need = true })
            }
            val label = s.reasonLabel
            if (label != null) {
                // `.alb60reason` (4/12 dp), the need line inside it (`.alb60need`, 2/16 dp)
                Column(Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, end = 12.dp)) {
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text(label) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    if (need) Text(s.need, style = ty.bodySmall, color = NEED, modifier = Modifier.padding(start = 16.dp, top = 2.dp, end = 16.dp))
                }
            }
            Text(s.footer, style = ty.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** The raise sheet: the kinds the work allows, the first selected; the note the client reads; [onRaise] queues and the caller closes it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumRaiseSheet(s: AlbumPage.RaiseSheet, onRaise: (Album.CrKind, String) -> Unit, onDismiss: () -> Unit) {
    var kind by remember(s.work) { mutableStateOf(s.kinds.first().kind) }
    var note by remember(s.work) { mutableStateOf("") }
    // a kind the work no longer allows (the overlaid scope moved under the sheet) falls back to the first, as 7522 does
    val sel = if (s.kinds.any { it.kind == kind }) kind else s.kinds.first().kind
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()) {
            SheetTitle(s.title)
            for (k in s.kinds) {
                ListItem(
                    headlineContent = { Text(k.head) },
                    supportingContent = { Text(k.sub) },
                    colors = rowColors(k.kind == sel),
                    modifier = Modifier.clickableRow { kind = k.kind })
            }
            Column(Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, end = 12.dp)) {
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(s.noteLabel) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
            }
            Button(onClick = { onRaise(sel, note) }, modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp)) { Text(s.button) }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * The client's-answer sheet: the head line, the three outcomes (approved selected), the day the client answered (today,
 * IST), how, the help and the note. [onAnswer] is AlbumWrite.answer's outcome: a date that is not a day (Refused) draws
 * S5's words in the status line, marks the date field and stays open, and a keystroke in the date clears it (7534);
 * anything else queues or not and the caller closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumAnswerSheet(s: AlbumPage.AnswerSheet, onAnswer: (Album.CrOutcome, String, String) -> AlbumWrite.Out, onDismiss: () -> Unit) {
    val tx = LocalTx.current
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    var outcome by remember(s.work) { mutableStateOf(Album.CrOutcome.APPROVED) }
    var clientAt by remember(s.work) { mutableStateOf(s.today) }
    var how by remember(s.work) { mutableStateOf("") }
    var err by remember(s.work) { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()) {
            SheetTitle(s.title)
            // `.alb60crhd` (0/24/8 dp): the work · Change request · the kind · the note as typed
            Text(s.head, style = ty.bodySmall, modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp))
            for (o in s.outcomes) {
                ListItem(
                    headlineContent = { Text(o.text) },
                    colors = rowColors(o.outcome == outcome),
                    modifier = Modifier.clickableRow { outcome = o.outcome })
            }
            Column(Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, end = 12.dp)) {
                OutlinedTextField(value = clientAt, onValueChange = { clientAt = it; err = null }, label = { Text(s.dateLabel) },
                    isError = err != null, singleLine = true, modifier = Modifier.fillMaxWidth())
                // `.alb60crm` (2/4 dp, role=status): S5's words when the date is refused, amber
                Box(Modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp, end = 4.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
                    err?.let { Text(it, style = ty.bodySmall, color = WARN) }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = how, onValueChange = { how = it }, label = { Text(s.howLabel) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Text(s.help, style = ty.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, top = 2.dp, end = 4.dp))
            }
            Text(s.note, style = ty.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp))
            Button(
                onClick = { val out = onAnswer(outcome, clientAt, how); if (out is AlbumWrite.Out.Refused) err = out.why.text(tx) },
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp),
            ) { Text(s.button) }
            Spacer(Modifier.height(12.dp))
        }
    }
}
