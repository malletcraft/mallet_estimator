package com.malletcrafts.interiorestimate

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.malletcrafts.interiorestimate.pano.QuoteView

/**
 * THE SCOPE SHEET (port 61c slice QV; SPEC61c §4.2 with §14.6's notes, index61c2.html scopeSheet): the app's bottom
 * sheet in the words of S5 /projects' scopeHtml, the one checklist of the sixteen works in three modes --
 * [QuoteView.Mode.APPROVE] (the Revisions panel's Approve the estimate ▸, port 61c slice QM2: pre-ticked from the
 * mirror's scope, its Move to Client approved is QuoteStageSync.approve, the guarded move carrying the ticks as the
 * baseline), [QuoteView.Mode.RECORD] (the APPROVED SCOPE row's Record the approved scope, for a project approved
 * before it had a baseline) and [QuoteView.Mode.VIEW] (the row's tap: ticked and disabled, read-only, Close alone).
 * Top to bottom: the title, the note, the count line (not in the view), the sixteen rows of 48 dp in WORKS order (two
 * columns in landscape), the status line (amber, `role=status`), and the foot, sticky under the rows' scroll: Cancel /
 * the filled button, or Close. Every rule is [QuoteView] in :pano, where QuoteViewTest pins it; this file draws.
 *
 * The sheet owns its ticks (QuoteView.open: the [stored] scope in the view, and again whenever the mirror's
 * changes; the stored scope once, as the sheet opens, for an approve; none for a record -- until a stale 409, [reset]
 * bumped by the caller, redraws them from the scope as stored, as the prototype's `st.ticks=scopeOf(p)` does on
 * scopestale and on nothing else: a final 409 leaves the person's ticks) WITH the scope_at they opened beside, one
 * value (QuoteView.Opened) that the submit hands back as the base it sends from -- never the mirror's stamp at submit
 * time, which a read landing meanwhile moves while the ticks stay (QM2 review) -- and
 * its own refusal (nothing ticked: S5's words, nothing called). A refusal S5 answered comes in as [status] and stays
 * until the next tap on a row clears it, as the prototype's `st.text=null` -- and the NEXT answer shows again even in
 * the same words ([answers] counts them: a status keyed on its text alone stayed cleared through a repeated
 * `Sign in first` or the same 403, QV review); the sheet stays open on every refusal. ◁ closes the sheet first: the
 * sheet's own back handling dismisses it before the tree's -- except while a submit is out ([busy]): then nothing
 * closes it, no swipe, scrim tap, ◁ or Cancel, until S5 answers, so the answer lands on the sheet that sent it, and
 * the strip under it waits with it (MainActivity: its busy is either PUT's; QM2 review).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScopeSheet(
    mode: QuoteView.Mode,
    /** The scope as the mirror stores it, none as empty: the view's ticks, and what a stale 409 redraws a record's to. */
    stored: List<Int>,
    /** The mirror's scope_at beside [stored], for the view's note and as the stamp the ticks open with (QuoteView.open); '' when S5 sent none. */
    scopeAt: String,
    /** S5's refusal of the last submit, in the reader's language (QuoteStageSync.scopeText); null when none. */
    status: String?,
    /** How many answers to a submit have landed since the sheet opened: each one shows its [status] afresh. */
    answers: Int,
    /** Bumped when an answer says the ticks are redrawn from [stored]: S5's stale 409. 0 when the sheet opens. */
    reset: Int,
    /** A submit is out: the filled button, Cancel and every dismiss wait for S5's answer. */
    busy: Boolean,
    /**
     * The ticked works, in WORKS order, never empty -- a record's, or an approve's scope (QuoteStageSync.approve) --
     * and the scope_at they were opened from (QuoteView.Opened.scopeAt): the base the submit sends, S5's scope_base
     * for an Approve and the record's base.
     */
    onSubmit: (List<Int>, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val tx = LocalTx.current
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // The view's ticks follow the stored scope; an approve's open pre-ticked from it and a record's empty
    // (QuoteView.open: the ticks WITH the scope_at they came from, one value -- the prototype's openScope), and from a
    // stale 409 on both follow it too (the mirror takes S5's stored scope as the answer lands, which can be after this
    // redraw). Until then an approve's ticks are the person's: a mirror a sync's read changes meanwhile does not redraw
    // them, only S5's answer to them does -- and the stamp the submit sends as its base is the one these ticks were
    // opened from, not the mirror's at submit time, so that read makes the Approve S5's scope 409 instead of a 200
    // over the amendment it landed (QM2 review).
    val follows = mode == QuoteView.Mode.VIEW || reset > 0
    val opened = remember(if (follows) stored else null, reset) { QuoteView.open(mode, stored, scopeAt, redrawn = follows) }
    var ticks by remember(opened, reset) { mutableStateOf(opened.ticks) }
    // the sheet's own refusal (nothing ticked), and whether S5's status line has been cleared by a tap since it came
    var local by remember { mutableStateOf<String?>(null) }
    var cleared by remember(answers) { mutableStateOf(false) }
    val shown = local ?: status?.takeIf { !cleared }
    val amber = Color(0xFFB0740E)
    val rows = QuoteView.rows(mode, ticks)
    fun tap(id: Int) {
        ticks = QuoteView.toggled(mode, ticks, id)
        local = null
        cleared = true
    }
    // A submit out: nothing closes the sheet until S5 answers -- no swipe or scrim tap (the state refuses Hidden), no ◁
    // (the dialog's back is off) and no Cancel -- so the answer lands on the sheet that sent it, and the strip under it
    // waits too (MainActivity: its busy is either PUT's). Each guard sits where its gesture is decided, none on
    // onDismissRequest: a sheet hidden and then refused its dismiss would sit off-screen with its window up (QM2 review).
    // The confirm lambda is one remembered instance: rememberModalBottomSheetState keys its state on it.
    val busyNow by rememberUpdatedState(busy)
    val confirm = remember<(SheetValue) -> Boolean> { { v -> v != SheetValue.Hidden || !busyNow } }
    val sheetState = rememberModalBottomSheetState(confirmValueChange = confirm)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = !busy),
    ) {
        Column(Modifier.fillMaxWidth()) {
            SheetTitle(QuoteView.title(tx, mode))
            val note = QuoteView.note(tx, mode, scopeAt)
            if (note.isNotEmpty()) {
                Text(note, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp))
            }
            QuoteView.countLine(tx, mode, ticks)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp))
            }
            // The rows scroll in their own area; the status line and the foot stay under them (the prototype's
            // sticky .sc61b). Two columns in landscape (.device.land .sc61g), one in portrait.
            Box(Modifier.weight(1f, fill = false)) {
                if (landscape) {
                    LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                        items(rows, key = { it.id }) { r -> ScopeRowItem(QuoteView.rowName(tx, r.id), r.on, r.disabled) { tap(r.id) } }
                    }
                } else {
                    LazyColumn {
                        items(rows, key = { it.id }) { r -> ScopeRowItem(QuoteView.rowName(tx, r.id), r.on, r.disabled) { tap(r.id) } }
                    }
                }
            }
            // role=status: the refusal is announced when it lands; blank keeps the foot in place
            Text(shown.orEmpty(), style = MaterialTheme.typography.bodySmall, color = amber,
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite })
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss, enabled = !busy) { Text(QuoteView.closeLabel(tx, mode)) }
                QuoteView.submitLabel(tx, mode)?.let { label ->
                    Button(enabled = !busy, onClick = {
                        val works = QuoteView.submit(ticks)
                        if (works == null) local = QuoteView.emptyRefusal(tx)
                        else { local = null; onSubmit(works, opened.scopeAt) }
                    }) { Text(label) }
                }
            }
        }
    }
}

/** One work's row: its name and its tick, 48 dp; the whole row toggles, and in the view it is disabled and takes no tap. */
@Composable
private fun ScopeRowItem(name: String, on: Boolean, disabled: Boolean, onTap: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = on, enabled = !disabled, role = Role.Checkbox, onValueChange = { onTap() })
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f),
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        // the tick is drawn by the row's toggleable; disabled it shows the stored scope, read-only (cb(on, true))
        Checkbox(checked = on, onCheckedChange = null, enabled = !disabled)
    }
}
