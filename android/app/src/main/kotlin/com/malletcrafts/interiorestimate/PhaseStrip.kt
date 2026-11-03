package com.malletcrafts.interiorestimate

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malletcrafts.interiorestimate.pano.Phases
import com.malletcrafts.interiorestimate.pano.QuoteStage
import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * THE PHASE STRIP through the quote journey (port 61c slice QM1; SPEC61c §4.1 item 3; index61c2.html stripHTML and
 * its .p54* CSS), drawn under the project line on the project screen: the five segments (the past ones ✓, the
 * current one filled), the does-line, the refusal line, the backward confirm, SURVEY COVERAGE at Survey and IN
 * REVISION at Revisions -- the LISTED SUBSET of the prototype's strip Amit chose (answer 1 of 28 Oct 2026): no VENDOR
 * PACKS, no GOES TO ESTIMATION, no hand-off counts (the v53/v54 port's), and Approve the estimate ▸ is QM2's. Every
 * rule is [Phases] in :pano, where PhasesTest pins it; this file draws.
 *
 * THE MOVER (decision 2 of 27 Oct 2026: the quote stage moves from the phone as it does on /projects, one guarded value
 * on S5). A tap on a later segment moves at once; an earlier one asks first ([Phases.ask], the inline confirm with
 * Keep / Move back); either way [PhaseStripArgs.onMove] sends ONE online `PUT …/stage` (QuoteStageSync.move) and S5's
 * answer goes into the one project-state store, whose listener redraws the line and this strip -- so the current
 * segment is always the store's stage, never one the phone set itself. A refusal ([PhaseStripArgs.refusal], the same
 * pending line the quote view shows at Client approved) stands amber under the does-line, `role=status`. While a
 * move is out ([PhaseStripArgs.busy]) no segment takes a tap.
 *
 * v61c.2 (Amit, 28 Oct 2026, "Read-only until S5 has it"): a project S5 does not list is drawn as an unread one --
 * no segment current, past or a tap, no phase row, no confirm -- and its does-line reads `not on S5 yet — this
 * project has not been sent` whole, on two lines if it needs them, in portrait and in landscape. An unread stage
 * offers no move either (critic): its does-line is the unread text.
 *
 * Sizes are the prototype's. Portrait (.p54): padded 2/4/4 over a 1 dp rule; a segment 48 dp tall, flex 1 of five,
 * with a 24 dp circle (1.5 dp border, #8F958B; past: teal border and ✓; current: filled teal, white number) over its
 * name at 10/16 sp (the current one semibold in #0F5E57), a 1.5 dp connector at 16 dp; the does-line centred at
 * 12/16 sp; the confirm a #FDF0D5 box with a 3 dp amber bar, 8 dp radius, its two 40 dp pill buttons right-aligned.
 * Landscape (.scaf.land .p54): one row, padded 2/8, of 40 dp pills (22 dp circles, the name beside; the current one
 * tinted #E3F1EF) with the does-line beside them, left-aligned; with the IN REVISION row the does-line gives way
 * and the other pills keep their circle alone; the confirm takes the whole row (the question at 13/17 sp, no aux
 * sentence); SURVEY COVERAGE is its own full-width line under the row, one line, hidden while the confirm shows.
 * The refusal line is drawn under the row in both orientations.
 */
class PhaseStripArgs(
    /** The one derived line (QuoteStageSync.lineFor): its kind and stage decide the strip's phase (Phases.phaseOf). */
    val line: QuoteStage.ProjectLine,
    /** The pending refusal for this project, in the reader's language (QuoteStageSync.moveText); null for none. */
    val refusal: String?,
    /** SURVEY COVERAGE (QuoteStage.coverage over the rooms in scope); drawn at Survey only, null when no room is in scope. */
    val coverage: QuoteStage.Coverage?,
    /** A move is out: no segment takes a tap until S5 answers. */
    val busy: Boolean,
    /** The tap's move: the quote stage to move to (Phases.stageOf), sent through QuoteStageSync.move. */
    val onMove: (String) -> Unit,
)

@Composable
fun PhaseStrip(s: PhaseStripArgs) {
    val tx = LocalTx.current
    val ph = Phases.phaseOf(s.line)
    // at Client approved the strip is the quote view's (QuoteStrip, slice QV): nothing to draw here
    if (ph == Phases.N) return
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val teal = Color(0xFF0F766E)
    val ink = Color(0xFF0F5E57)
    val amber = Color(0xFFB0740E)
    val rule = MaterialTheme.colorScheme.outlineVariant
    val small = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp)
    val segs = Phases.segments(tx, ph)
    val does = Phases.doesLine(tx, s.line)
    val wraps = Phases.wraps(s.line)
    val cover = if (ph == Phases.SURVEY) s.coverage?.let { Phases.coverageRow(tx, it) } else null
    val rev = if (ph == Phases.REVISIONS) Phases.revisionRow(tx) else null
    // The backward confirm, this strip's alone and keyed on the stage: a stage that moves under it -- a sync's read,
    // a move's answer -- drops it, and leaving the screen drops it with the strip (the prototype's finding 7: a
    // confirm whose strip is no longer on screen neither eats a ◁ nor comes back on the next visit).
    var ask by remember(s.line.kind, s.line.quote) { mutableStateOf<Int?>(null) }
    val askText = ask?.let { to -> ph?.let { Phases.ask(tx, it, to) } }
    fun tap(i: Int) {
        if (s.busy) return
        when (Phases.tap(ph, i)) {
            Phases.Tap.MOVE -> { ask = null; s.onMove(Phases.stageOf(i)) }
            Phases.Tap.ASK -> ask = i
            Phases.Tap.NONE -> {}
        }
    }
    val moveBack: () -> Unit = { ask?.let { to -> ask = null; s.onMove(Phases.stageOf(to)) } }
    val keep: () -> Unit = { ask = null }
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .drawBehind {
                val y = size.height - 0.5.dp.toPx()
                drawLine(rule, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            // role=group, aria-label "Phase of this project"
            .semantics { contentDescription = Phases.groupLabel(tx) }
            .padding(start = if (landscape) 8.dp else 4.dp, top = 2.dp, end = if (landscape) 8.dp else 4.dp, bottom = if (landscape) 3.dp else 5.dp),
    ) {
        if (landscape) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (askText != null) {
                    // the confirm takes the whole row: the segments, the does-line and the phase row give way
                    AskRow(askText, s.busy, keep, moveBack, small, amber)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // with a phase row beside them, only the current pill keeps its name (.p54:has(.p54x) .p54seg:not(.on) .p54l)
                        for ((i, seg) in segs.withIndex()) SegmentPill(seg, rev == null || seg.on, s.busy, teal, ink) { tap(i) }
                    }
                    if (rev != null) PanelBlock(rev, Modifier.weight(1f), true, ink, small)
                    else Text(does, style = small, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (wraps) 2 else 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }
            s.refusal?.let { RefusalLine(it, small, amber) }
            if (askText == null) cover?.let { c ->
                // its own full-width line, left, one line (.scaf.land .p56cov)
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp), color = ink, maxLines = 1)
                    Text(Phases.SEP + c.parts.joinToString(Phases.SEP), style = small, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }
        } else {
            val n = segs.size
            Row(
                Modifier.fillMaxWidth().drawBehind {
                    // the connector between the circles' centres, 16 dp down (4 dp padding + half the 24 dp circle)
                    val y = 16.dp.toPx()
                    val half = size.width / (2 * n)
                    drawLine(rule, Offset(half, y), Offset(size.width - half, y), strokeWidth = 1.5.dp.toPx())
                },
                verticalAlignment = Alignment.Top,
            ) {
                for ((i, seg) in segs.withIndex()) SegmentColumn(seg, s.busy, teal, ink, Modifier.weight(1f)) { tap(i) }
            }
            // the does-line (.p54does): centred, cut with an ellipsis -- but the never-sent words whole (data-s5none)
            Text(does, style = small, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                maxLines = if (wraps) 2 else 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 2.dp))
            s.refusal?.let { RefusalLine(it, small, amber) }
            askText?.let { AskBox(it, s.busy, keep, moveBack, small, amber) }
            cover?.let { c ->
                // .p56cov: the heading on its own line, the figures under it on one, both centred
                Column(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(c.label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp), color = ink, maxLines = 1)
                    Text(c.parts.joinToString(Phases.SEP), style = small, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            rev?.let { PanelBlock(it, Modifier.fillMaxWidth(), false, ink, small) }
        }
    }
}

/** The refusal line (.p54msg.warn): amber, `role=status`, announced when it lands. */
@Composable
private fun RefusalLine(text: String, small: androidx.compose.ui.text.TextStyle, amber: Color) {
    Text(text, style = small, color = amber,
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            .semantics { liveRegion = LiveRegionMode.Polite })
}

/**
 * A segment in portrait (.p54seg): the 24 dp circle over the name, 48 dp tall, one TalkBack node saying its label
 * (`Phase n of 5: name`, ` (now)` on the current one); a button only when it is a tap and no move is out.
 */
@Composable
private fun SegmentColumn(seg: Phases.Segment, busy: Boolean, teal: Color, ink: Color, modifier: Modifier, onTap: () -> Unit) {
    Column(
        modifier.heightIn(min = 48.dp)
            .then(if (seg.tap && !busy) Modifier.clickable(role = Role.Button, onClick = onTap) else Modifier)
            .padding(top = 4.dp, bottom = 2.dp)
            .semantics(mergeDescendants = true) { contentDescription = seg.label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SegmentCircle(24.dp, seg, teal)
        // .p54l: never cut -- the current one may run a few dp into its neighbours' free space
        Text(seg.name, fontSize = 10.sp, lineHeight = 16.sp, letterSpacing = (-0.2).sp, maxLines = 1, softWrap = false,
            overflow = TextOverflow.Visible, color = if (seg.on) ink else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (seg.on) FontWeight.SemiBold else FontWeight.Normal)
    }
}

/** A segment in landscape (.scaf.land .p54seg): a 40 dp pill, the 22 dp circle and -- when [named] -- the name beside it; the current one tinted. */
@Composable
private fun SegmentPill(seg: Phases.Segment, named: Boolean, busy: Boolean, teal: Color, ink: Color, onTap: () -> Unit) {
    Row(
        Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(20.dp))
            .background(if (seg.on) Color(0xFFE3F1EF) else Color.Transparent)
            .then(if (seg.tap && !busy) Modifier.clickable(role = Role.Button, onClick = onTap) else Modifier)
            .padding(horizontal = if (named) 8.dp else 9.dp)
            .semantics(mergeDescendants = true) { contentDescription = seg.label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SegmentCircle(22.dp, seg, teal)
        if (named) Text(seg.name, fontSize = 10.sp, lineHeight = 16.sp, letterSpacing = (-0.2).sp, maxLines = 1, softWrap = false,
            color = if (seg.on) ink else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (seg.on) FontWeight.SemiBold else FontWeight.Normal)
    }
}

/** The segment's circle (.p54n): [size] across; past, a teal border and a teal ✓; current, filled teal with a white number; else a grey border and its number. */
@Composable
private fun SegmentCircle(size: Dp, seg: Phases.Segment, teal: Color) {
    val grey = Color(0xFF8F958B)
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(if (seg.on) teal else MaterialTheme.colorScheme.surface)
            .then(if (seg.on) Modifier else Modifier.border(BorderStroke(1.5.dp, if (seg.past) teal else grey), CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (seg.past) "✓" else seg.n.toString(), fontSize = 12.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium, // text:value the prototype's .p54n glyph, in both languages
            color = if (seg.on) Color.White else if (seg.past) teal else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The two buttons of the confirm (.p54btns): `Keep <phase>` outlined, `Move back` filled teal, each a 40 dp pill. */
@Composable
private fun RowScope.AskButtons(a: Phases.Ask, busy: Boolean, keep: () -> Unit, moveBack: () -> Unit) {
    val brown = Color(0xFF3D2E05)
    OutlinedButton(
        onClick = keep,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF8F958B)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = brown),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        modifier = Modifier.heightIn(min = 40.dp),
    ) { Text(a.keep, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1) }
    Button(
        onClick = moveBack,
        enabled = !busy,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E), contentColor = Color.White),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        modifier = Modifier.heightIn(min = 40.dp),
    ) { Text(a.moveBack, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1) }
}

/** The confirm's box: #FDF0D5 with a 3 dp amber bar on the left and an 8 dp radius (.p54ask), `role=alertdialog` labelled `Move the phase back`. */
private fun Modifier.askBox(a: Phases.Ask, amber: Color): Modifier = this
    .clip(RoundedCornerShape(8.dp))
    .background(Color(0xFFFDF0D5))
    .drawBehind { drawRect(amber, size = Size(3.dp.toPx(), size.height)) }
    .semantics { contentDescription = a.label; liveRegion = LiveRegionMode.Polite }

/** The backward confirm in portrait: the question with its aux sentence, then the two buttons right-aligned under it. */
@Composable
private fun AskBox(a: Phases.Ask, busy: Boolean, keep: () -> Unit, moveBack: () -> Unit, small: androidx.compose.ui.text.TextStyle, amber: Color) {
    Column(
        Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 2.dp)
            .askBox(a, amber).padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(a.whole, style = small, color = Color(0xFF3D2E05))
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) { AskButtons(a, busy, keep, moveBack) }
    }
}

/** The backward confirm in landscape: one row, the question (13/17 sp, two lines at most, no aux sentence) beside the two buttons. */
@Composable
private fun RowScope.AskRow(a: Phases.Ask, busy: Boolean, keep: () -> Unit, moveBack: () -> Unit, small: androidx.compose.ui.text.TextStyle, amber: Color) {
    Row(
        Modifier.weight(1f).padding(vertical = 2.dp).askBox(a, amber).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(a.question, style = small.copy(fontSize = 13.sp, lineHeight = 17.sp), color = Color(0xFF3D2E05),
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        AskButtons(a, busy, keep, moveBack)
    }
}

/**
 * A phase's row (.p54x / .p54hot): the small-caps label over its text -- IN REVISION over `open for the client and
 * vendors`. Two lines at most in portrait (.p54hos), one in landscape ([oneLine]).
 */
@Composable
private fun PanelBlock(p: Phases.Panel, modifier: Modifier, oneLine: Boolean, ink: Color, small: androidx.compose.ui.text.TextStyle) {
    Column(modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
        Text(p.label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp), color = ink, maxLines = 1)
        Text(p.parts.joinToString(Phases.SEP), style = small, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (oneLine) 1 else 2, overflow = TextOverflow.Ellipsis)
    }
}
