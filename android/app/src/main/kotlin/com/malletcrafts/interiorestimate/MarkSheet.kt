package com.malletcrafts.interiorestimate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.malletcrafts.interiorestimate.pano.MarkSheet

/**
 * THE MARK SHEET, DRAWN (port 62 slice L3, 11 Nov 2026; the approved prototype v61c.2, index61c2.html: the sheet's
 * CSS 1004-1068, its HTML in L53's paint, 3023-3060). Every text and every rule is [MarkSheet] in :pano, where
 * MarkSheetTest pins them against the prototype's own painted sheet; this file draws a [MarkSheet.View] and hands each
 * tap and keystroke back ([MarkSheetActions]).
 *
 * A bottom sheet in survey prep's own dark kit, every size the prototype's dp: 16 dp top corners on #1E211B, 12 dp at
 * the sides and 8 under; the head 40 dp (the code in gold mono, · the name, the trade's dot and short name, ✕); each
 * block a 16 dp label over its controls, 4 dp apart, the blocks 6 dp apart; chips 32 dp, a move 40 dp with its Why
 * box 18 dp in under it, the note two lines in 48 dp, the audience 32 dp, ONE Done of 48 dp. Portrait is one column:
 * KIND, TRADE, STATE, NOTE. Landscape is two: KIND and TRADE, and at the foot NOTE and Done, on the left; STATE on the
 * right, its label beside the now-line (the sheet's `.land` grid, 1048-1062).
 *
 * KIND (port 62 slice L6; `.l53kind`, 3030 and CSS 1019): the type's sub-kinds as 32 dp chips that wrap, the one the
 * mark is ringed gold; drawn off on a locked mark. The prototype cuts the chips at two rows (`max-height:68px;
 * overflow:hidden`); here the block grows with them, so a kind is never out of reach under मराठी's longer names.
 * A NOTE JUST PLACED opens with the keyboard on its note field (`View.focusNote`).
 *
 * IT OPENS WHOLE (`skipPartiallyExpanded = true`), as the album's sheets do since the K4 review: the prototype draws it
 * at its content's height, never half-open, and material3's default would open a sheet taller than half the window at
 * half height, with Done under the fold. No drag handle (the prototype draws none). ✕, the scrim and ◁ cancel, losing
 * what was typed; so does a swipe down, which the prototype has not and material3 gives every sheet.
 *
 * WITH THE KEYBOARD UP the sheet rises over it (`imePadding`, as the album's sheets) and, only when the blocks then
 * need more height than is left, they scroll between the head and Done, which stay put -- a state the prototype, a
 * page, never has. With the keyboard down nothing scrolls in either orientation at 360 x 780.
 *
 * THE TRADE PICKER is drawn OVER the sheet, 8 dp in from its sides and its foot (`.l53pop`, 1064-1068): a Popup, since
 * on a Line's or an area's short sheet it stands taller than the sheet itself. It takes no focus and no tap outside
 * it: a tap on the TRADE button shuts it again, a tap on any other control does that control's work, and a tap on a
 * bare part of the sheet shuts it -- the prototype's own three cases.
 */
private val MS_BG = Color(0xFF1E211B)
private val MS_FG = Color(0xFFECEEE8)
private val MS_GOLD = Color(0xFFE0B040)
private val MS_SOFT = Color(0xFFCFD3CB)
private val MS_MUTED = Color(0xFF9AA096)
private val MS_LINE = Color(0xFF3A3E35)
private val MS_CHIP = Color(0xFF2A2D25)
private val MS_MOVE = Color(0xFF23261F)
private val MS_MOVE_ON = Color(0xFF2F3A2C)
private val MS_FIELD = Color(0xFF111311)
private val MS_FIELD_LINE = Color(0xFF555A50)
private val MS_HINT = Color(0xFF7D8278)
private val MS_AMBER = Color(0xFFFFC46B)
private val MS_INK = Color(0xFF1A1A12)
private val MS_VPLUS = Color(0xFF6741D9)
private val MS_POP = Color(0xFF262923)

/** A control the sheet draws but does not offer (`:disabled`): 45 %. */
private const val MS_OFF = 0.45f

/**
 * What the sheet's controls do. Each is a tap or a keystroke for the caller to hand to [MarkSheet]'s pure functions:
 * a KIND chip tapped (the sub-kind's id; slice L6),
 * a move tapped (its target), a why typed (the move's target, the text), an act chip, the note typed, the audience
 * picked (true: Office only), the TRADE button, a trade picked in the picker, a bare part of the sheet tapped (the
 * picker shuts), Done, and cancel -- ✕, the scrim, ◁ and a swipe down.
 */
internal class MarkSheetActions(
    val onKind: (String) -> Unit,
    val onMove: (String) -> Unit,
    val onWhy: (String, String) -> Unit,
    val onAct: (String) -> Unit,
    val onNote: (String) -> Unit,
    val onOffice: (Boolean) -> Unit,
    val onTrade: () -> Unit,
    val onCat: (String) -> Unit,
    val onBare: () -> Unit,
    val onDone: () -> Unit,
    val onCancel: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpMarkSheet(v: MarkSheet.View, land: Boolean, a: MarkSheetActions) {
    ModalBottomSheet(
        onDismissRequest = a.onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetMaxWidth = Dp.Unspecified,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MS_BG,
        contentColor = MS_FG,
        scrimColor = Color(0x88000000),
        dragHandle = null,
    ) {
        Box(Modifier.fillMaxWidth().semantics { paneTitle = v.pane }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { a.onBare() }
            .imePadding()) {
            if (land) MsLandscape(v, a) else MsPortrait(v, a)
            val trade = v.tradeBlock
            val chips = trade?.chips
            if (trade != null && chips != null) MsPicker(trade.title, chips, land, a.onCat)
        }
    }
}

/** Portrait: the head, then KIND, TRADE, STATE and NOTE in one column, then Done. */
@Composable
private fun MsPortrait(v: MarkSheet.View, a: MarkSheetActions) {
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
        MsHead(v, 40.dp, a.onCancel)
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val kind = v.kind
            if (kind != null) MsKind(kind, a.onKind)
            val trade = v.tradeBlock
            if (trade != null) MsTrade(trade, a.onTrade)
            val state = v.state
            if (state != null) MsState(state, v, false, a)
            MsNote(v.note, v.focusNote, a)
        }
        MsDone(v, 8.dp, a.onDone)
    }
}

/** Landscape: the head across; KIND and TRADE at the top of the left column, NOTE and Done at its foot; STATE on the right. */
@Composable
private fun MsLandscape(v: MarkSheet.View, a: MarkSheetActions) {
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 6.dp)) {
        MsHead(v, 36.dp, a.onCancel)
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    // the grid's KIND and TRADE rows, each with its 4 dp gap: KIND sits 4 dp under the head and TRADE 4 dp
                    // under it -- 8 dp under the head where the type has no kinds -- and with neither the note starts
                    // 12 dp under the head
                    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                        val kind = v.kind
                        if (kind != null) MsKind(kind, a.onKind)
                        Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            val trade = v.tradeBlock
                            if (trade != null) MsTrade(trade, a.onTrade)
                        }
                    }
                    Column(Modifier.fillMaxWidth()) {
                        MsNote(v.note, v.focusNote, a)
                        MsDone(v, 6.dp, a.onDone)
                    }
                }
                Column(Modifier.weight(1f)) {
                    val state = v.state
                    if (state != null) MsState(state, v, true, a)
                }
            }
        }
    }
}

/** The head: "FW1 · Window", the trade's dot and short name, ✕. */
@Composable
private fun MsHead(v: MarkSheet.View, tall: Dp, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(tall), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(buildAnnotatedString {
                withStyle(SpanStyle(color = MS_GOLD, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)) { append(v.code) }
                append(MarkSheet.SEP); append(v.name)
            },
            color = MS_FG, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MsDot(v.trade.colour)
            Text(v.trade.text, color = MS_SOFT, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, softWrap = false)
        }
        Box(Modifier.size(tall).clip(CircleShape).clickable(role = Role.Button) { onClose() }
            .semantics { contentDescription = v.close }, contentAlignment = Alignment.Center) {
            Text("✕", color = MS_FG, fontSize = 16.sp, lineHeight = 20.sp) // text:value glyph
        }
    }
}

/** An 8 dp dot in a trade's or a state's colour (0xRRGGBB). */
@Composable
private fun MsDot(colour: Int) {
    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(SurveyPrepDraw.argb(colour))))
}

/** A block's label: STATE, TRADE, NOTE. */
@Composable
private fun MsLabel(label: String, modifier: Modifier = Modifier) {
    Text(label, color = MS_MUTED, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.Bold,
        maxLines = 1, softWrap = false, modifier = modifier)
}

/** A 32 dp chip: an act, the TRADE button, a trade in the picker. [on]: ringed gold; not [enabled]: dim, and takes no tap. */
@Composable
private fun MsChip(words: String, dot: Int?, on: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(modifier.height(32.dp).alpha(if (enabled) 1f else MS_OFF).clip(shape).background(if (on) MS_LINE else MS_CHIP)
        .border(if (on) 1.5.dp else 1.dp, if (on) MS_GOLD else MS_LINE, shape)
        .clickable(enabled = enabled, role = Role.Button) { onClick() }
        .semantics { selected = on }
        .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (dot != null) MsDot(dot)
        Text(words, color = if (on) Color.White else MS_FG, fontSize = 11.sp, lineHeight = 16.sp,
            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
    }
}

/** A grey line under the moves: the gate's, why the phase offers no move, the unread stage's, a locked mark's. */
@Composable
private fun MsLine(words: String) {
    Text(words, color = MS_MUTED, fontSize = 12.sp, lineHeight = 16.sp)
}

/** KIND: its label over the type's sub-kinds, as chips that wrap; the one the mark is ringed gold, all off on a locked mark. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MsKind(k: MarkSheet.KindBlock, onKind: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MsLabel(k.label)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (c in k.chips) MsChip(c.text, null, c.on, k.enabled, Modifier) { onKind(c.id) }
        }
    }
}

/** TRADE: its label over the button that opens the picker. */
@Composable
private fun MsTrade(t: MarkSheet.TradeBlock, onTrade: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MsLabel(t.label)
        MsChip(t.button.text, t.button.colour, false, t.enabled, Modifier) { onTrade() }
    }
}

/** The now-line: the state's dot, its name in bold, since when and who decided, and V+ on a mark a vendor added. */
@Composable
private fun MsNow(n: MarkSheet.Now, oneLine: Boolean, modifier: Modifier) {
    // the dot sits on the FIRST line's middle when the line wraps in portrait (8 dp in a 20 dp line)
    Row(modifier, verticalAlignment = if (oneLine) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(if (oneLine) 4.dp else 6.dp)) {
        Box(Modifier.padding(top = if (oneLine) 0.dp else 6.dp)) { MsDot(n.colour) }
        val badge = n.vplus
        Text(buildAnnotatedString {
                withStyle(SpanStyle(color = MS_FG, fontWeight = FontWeight.Bold)) { append(n.name) }
                append(n.rest)
                if (badge != null) {
                    append(MarkSheet.SEP)
                    withStyle(SpanStyle(color = Color.White, background = MS_VPLUS, fontSize = 10.sp, fontWeight = FontWeight.Bold)) { append(badge) }
                }
            },
            color = MS_SOFT, fontSize = 12.sp, lineHeight = 20.sp, maxLines = if (oneLine) 1 else 3, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f))
    }
}

/**
 * STATE: the label and the now-line (on one row in landscape), then what the phase offers the mark -- the act chips,
 * each move with its Why box under it -- or the line that says why it offers none; the amber line; the grey lines.
 */
@Composable
private fun MsState(s: MarkSheet.StateBlock, v: MarkSheet.View, land: Boolean, a: MarkSheetActions) {
    val focus = LocalFocusManager.current
    // Delete on a wish opened the sheet on Dropped: its Why box takes the keyboard, once, as the sheet opens
    val whyFocus = remember { FocusRequester() }
    val focusWhy = v.focusWhy
    LaunchedEffect(Unit) { if (focusWhy != null) runCatching { whyFocus.requestFocus() } }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (land) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MsLabel(s.label)
                MsNow(s.now, true, Modifier.weight(1f))
            }
        } else {
            MsLabel(s.label)
            MsNow(s.now, false, Modifier.fillMaxWidth())
        }
        val gate = s.gate
        if (gate != null) MsLine(gate)
        if (s.acts.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in s.acts) MsChip(c.text, null, c.on, c.enabled, Modifier.weight(1f, fill = false)) { a.onAct(c.id) }
            }
        }
        for (m in s.moves) {
            // a move and its Why box are one piece: the box sits 2 dp under its move, 18 dp in
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                MsMove(m) { a.onMove(m.to) }
                val why = m.why
                if (why != null) {
                    BasicTextField(
                        value = why.text, onValueChange = { t -> a.onWhy(m.to, t) }, singleLine = true, enabled = why.enabled,
                        textStyle = TextStyle(color = MS_FG, fontSize = 12.sp, lineHeight = 16.sp),
                        cursorBrush = SolidColor(MS_GOLD),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                        modifier = Modifier.padding(start = 18.dp).fillMaxWidth().height(40.dp)
                            .let { if (m.to == focusWhy) it.focusRequester(whyFocus) else it }
                            .alpha(if (why.enabled) 1f else MS_OFF)
                            .clip(RoundedCornerShape(8.dp)).background(MS_FIELD).border(1.dp, MS_FIELD_LINE, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp),
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                                if (why.text.isEmpty()) Text(v.whyHint, color = MS_HINT, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                inner()
                            }
                        })
                }
            }
        }
        val noMove = s.noMove
        if (noMove != null) MsLine(noMove)
        val amber = s.amber
        if (amber != null) {
            Text(amber, color = MS_INK, fontSize = 12.sp, lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(MS_AMBER).padding(horizontal = 8.dp, vertical = 4.dp))
        }
        for (f in s.foots) MsLine(f)
    }
}

/** One move, 40 dp: its target state's dot, "→ Client wish", its small hint. Picked: ringed gold on green. */
@Composable
private fun MsMove(m: MarkSheet.MoveRow, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(Modifier.fillMaxWidth().height(40.dp).alpha(if (m.enabled) 1f else MS_OFF).clip(shape).background(if (m.on) MS_MOVE_ON else MS_MOVE)
        .border(if (m.on) 2.dp else 1.dp, if (m.on) MS_GOLD else MS_LINE, shape)
        .clickable(enabled = m.enabled, role = Role.Button) { onClick() }
        .semantics { selected = m.on }
        .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MsDot(m.colour)
        Text(m.label, color = MS_FG, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
        val hint = m.hint
        if (hint != null) Text(hint, color = MS_MUTED, fontSize = 11.sp, lineHeight = 16.sp, maxLines = 1, softWrap = false,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

/**
 * NOTE: the label and "20/280", the two-line box, and who it is for: Client & vendors | Office only. [focus]: the box
 * takes the keyboard, once, as the sheet opens -- a Note just placed is only its words (slice L6; openSheet's `fresh`).
 */
@Composable
private fun MsNote(n: MarkSheet.NoteBlock, focus: Boolean, a: MarkSheetActions) {
    val noteFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (focus) runCatching { noteFocus.requestFocus() } }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MsLabel(n.label, Modifier.weight(1f))
            Text(n.count, color = MS_MUTED, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        BasicTextField(
            value = n.text, onValueChange = { t -> a.onNote(t) },
            textStyle = TextStyle(color = MS_FG, fontSize = 13.sp, lineHeight = 18.sp),
            cursorBrush = SolidColor(MS_GOLD),
            modifier = Modifier.fillMaxWidth().height(48.dp)
                .let { if (focus) it.focusRequester(noteFocus) else it }
                .clip(RoundedCornerShape(8.dp)).background(MS_FIELD)
                .border(1.dp, MS_FIELD_LINE, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.TopStart) {
                    if (n.text.isEmpty()) Text(n.placeholder, color = MS_HINT, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    inner()
                }
            })
        val shape = RoundedCornerShape(8.dp)
        Row(Modifier.fillMaxWidth().height(32.dp).clip(shape).border(1.dp, MS_LINE, shape)) {
            for ((words, office) in listOf(n.cv to false, n.officeOnly to true)) {
                val on = n.office == office
                Box(Modifier.weight(1f).fillMaxHeight().background(if (on) MS_LINE else Color.Transparent)
                    .let { if (on) it.border(1.5.dp, MS_GOLD, shape) else it }
                    .clickable(role = Role.RadioButton) { a.onOffice(office) }
                    .semantics { selected = on },
                    contentAlignment = Alignment.Center) {
                    Text(words, color = if (on) Color.White else MS_SOFT, fontSize = 12.sp, lineHeight = 16.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** The ONE Done, 48 dp, gold; dim and deaf while the move picked still needs its why. */
@Composable
private fun MsDone(v: MarkSheet.View, gap: Dp, onDone: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = gap).height(48.dp).alpha(if (v.doneEnabled) 1f else MS_OFF).clip(RoundedCornerShape(24.dp))
        .background(MS_GOLD).clickable(enabled = v.doneEnabled, role = Role.Button) { onDone() }, contentAlignment = Alignment.Center) {
        Text(v.done, color = MS_INK, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/**
 * THE TRADE PICKER, over the sheet: "Trade · FX1" and the 13 trades, two to a row (three in landscape), the one picked
 * ringed gold. 8 dp in from the sheet's sides and foot in portrait; in landscape 460 dp wide at its right. A tap on a
 * trade picks it and shuts the picker ([MarkSheet.pickCat]).
 */
@Composable
private fun MsPicker(title: String, chips: List<MarkSheet.TradeChip>, land: Boolean, onCat: (String) -> Unit) {
    val edge = with(LocalDensity.current) { 8.dp.roundToPx() }
    val across = LocalConfiguration.current.screenWidthDp - 16
    val wide = if (land) minOf(460, across) else across
    val cols = if (land) 3 else 2
    Popup(
        alignment = if (land) Alignment.BottomEnd else Alignment.BottomCenter,
        offset = IntOffset(if (land) -edge else 0, -edge),
        properties = PopupProperties(focusable = false, dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Column(Modifier.width(wide.dp).clip(RoundedCornerShape(12.dp)).background(MS_POP)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }   // a tap inside never falls through
            .padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MsLabel(title)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in chips.chunked(cols)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (c in row) MsChip(c.text, c.colour, c.on, true, Modifier.weight(1f)) { onCat(c.id) }
                        repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
