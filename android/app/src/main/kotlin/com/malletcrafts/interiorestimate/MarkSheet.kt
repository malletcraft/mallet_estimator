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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
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
import com.malletcrafts.interiorestimate.pano.DeleteGuard
import com.malletcrafts.interiorestimate.pano.MarkSheet

/**
 * THE PANEL THAT SLIDES UP (port 63 slice I2b, 23 Nov 2026; DESIGN-imagemeter §6; imagemeter-pictures/mcft-new-5-panel.png,
 * the picture Amit said "Yes" to). Amit, 16 Nov 2026: "a slide up screen in imagemeter give more options. imagemeter is
 * gold standard when it comes to image annotation." It is THE MARK SHEET (port 62 slice L3; the approved prototype v61c.2,
 * index61c2.html: the sheet's CSS 1004-1068, its HTML in L53's paint, 3023-3060) with the acts that stood in rows under
 * the photo. Every text and every rule is [MarkSheet] in :pano -- MarkSheetTest pins the blocks against the prototype's
 * own painted sheet, MarkSheetPanelTest the head, the acts and where the panel rests; this file draws a
 * [MarkSheet.PanelView] and hands each tap and keystroke back ([MarkSheetActions]).
 *
 * TOP TO BOTTOM, portrait: a GRIP, 16 dp; the HEAD, 40 dp -- the code in gold mono, · the name, the trade's dot and
 * short name, and DONE AT ITS RIGHT END, where no fold can hide it (there is no ✕: Back, a swipe down and a tap on the
 * dimmed photo cancel, losing what was typed, as before); THE ACTS, two lines of 44 dp, each where [MarkSheet.slots]
 * puts it -- Lock · Duplicate · Fine move, then Lock all and DELETE, last, apart, in the delete control's own red
 * outline (DeleteGuard.OUTLINE / INK): the one place anything is deleted -- and its line stands 16 dp under the line
 * over it (MarkSheet.deleteDropDp), so Delete is DeleteGuard.CLEAR_DP clear of `Fine move`; a refused act's line; then the sheet's own
 * blocks as they always were -- KIND, TRADE, STATE, NOTE, each a 16 dp label over its controls, 4 dp apart, the blocks
 * 6 dp apart; chips 32 dp, a move 40 dp with its Why box 18 dp in under it, the note two lines in 48 dp, the audience
 * 32 dp. A Tape and a chain have the head and the acts and no block. Landscape: the head across (36 dp, no grip), the
 * acts on ONE line of 40 dp that ends 16 dp short of Done, then two columns -- KIND, TRADE and, at the foot, NOTE on the
 * left; STATE on the right, its label beside the now-line (the sheet's `.land` grid, 1048-1062).
 *
 * KIND (port 62 slice L6; `.l53kind`, 3030 and CSS 1019): the type's sub-kinds as 32 dp chips that wrap, the one the
 * mark is ringed gold; drawn off on a locked mark. The prototype cuts the chips at two rows (`max-height:68px;
 * overflow:hidden`); here the block grows with them, so a kind is never out of reach under मराठी's longer names.
 *
 * WHERE IT RESTS ([MarkSheet.opensWhole]). In PORTRAIT its half rest is ON: it opens at half the window when its rows
 * need more than that, else at its own height, and pulls up whole -- so the item stays in view above it (the photo is
 * lifted under it: SpPhoto, PhotoLift). That rest was switched off after the K4 review, for hiding the foot's Done;
 * Done is in the head now. In LANDSCAPE it opens whole. AND IT STANDS WHOLE WHILE A BOX HAS THE KEYBOARD
 * ([MarkSheet.standsWhole]): it opens whole where it opens on a box (a Note just placed, a Drop's why) and where it
 * opens with STATE in view (a long press on a mark past Existing), and a box that takes the keyboard later makes it
 * whole -- at the half rest the keyboard would cover the box being typed in.
 *
 * THE HALF REST IS THIS PANEL'S OWN HEIGHT, NOT THE LIBRARY'S ANCHOR (the review of I2, finding 1;
 * [MarkSheet.SKIPS_LIBRARY_HALF]). material3 1.3's sheet sends an expanded sheet back to its half anchor whenever its
 * size changes, and the keyboard coming up is such a change: `sheet.expand()` on a box's focus was undone the moment
 * the keys rose. So the library's sheet has NO half anchor here -- it is exactly as high as its content -- and the half
 * rest is a CAP on that content ([MarkSheet.contentCapDp]): the head and the acts stay, the blocks scroll under them.
 * The cap goes when the panel is whole ([MarkSheet.wholeAfter]): a box took the keyboard, or HE PULLED IT UP -- the
 * blocks dragged up, their first scroll. Once whole it stays whole until it closes. With no half anchor there is one
 * Back: it cancels.
 *
 * IT TAKES NO TAP FOR ITS FIRST 300 MS ([MarkSheet.answers], asked by the caller before every act, chip and keystroke,
 * and here before the sheet lets itself be closed): the long press and the code chip that open it are both pressed over
 * the photo, where the half-open panel then stands.
 *
 * WITH THE KEYBOARD UP the panel rises over it (`imePadding`, as the album's sheets) and, only when the blocks then
 * need more height than is left, they scroll under the head and the acts, which stay put.
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
/** DELETE's word and its outline (DeleteGuard.INK / OUTLINE): the one look the one delete control has. */
private val MS_DEL = Color(SurveyPrepDraw.argb(DeleteGuard.INK))
private val MS_DEL_LINE = Color(SurveyPrepDraw.argb(DeleteGuard.OUTLINE))

/** A control the sheet draws but does not offer (`:disabled`): 45 %. */
private const val MS_OFF = 0.45f

/**
 * What the panel's controls do. Each is a tap or a keystroke for the caller to hand to [MarkSheet]'s pure functions:
 * one of the ACTS tapped (its id: SurveyPrepEdit.LOCK / DUPLICATE / LOCK_ALL / DELETE, MarkSheet.FINE, and since port
 * 63 slice Q2 the shape row's SurveyPrepEdit.SHAPE_FREE / SHAPE_BOX -- `Four corners` / `Rectangle`),
 * a KIND chip tapped (the sub-kind's id; slice L6),
 * a move tapped (its target), a why typed (the move's target, the text), an act chip, the note typed, the audience
 * picked (true: Office only), the TRADE button, a trade picked in the picker, a bare part of the sheet tapped (the
 * picker shuts), Done, and cancel -- the dimmed photo, ◁ and a swipe down.
 * [answers]: whether the panel takes a tap yet (MarkSheet.answers) -- asked HERE before the sheet lets a swipe down or a
 * tap on the dimmed photo close it; ◁ always answers (DESIGN-imagemeter §23.2, decision 4), so [onCancel] is never
 * guarded; the caller asks it itself before every other callback. [onTop]: where the panel's top
 * edge is on the SCREEN, px, each time it moves, and null when the panel is gone -- the photo is lifted from it.
 */
internal class MarkSheetActions(
    val onPanelAct: (String) -> Unit,
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
    val answers: () -> Boolean,
    val onTop: (Float?) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpMarkSheet(v: MarkSheet.PanelView, land: Boolean, a: MarkSheetActions) {
    // read through States: the sheet's own state below is made once and outlives the lambdas it was made with
    val answersS by rememberUpdatedState(a.answers)
    val onTopS by rememberUpdatedState(a.onTop)
    // WHETHER IT IS WHOLE. How it OPENS is decided once, as it opens (MarkSheet.opensWhole): whole in landscape, where
    // it opens on a box and where it opens with STATE in view; else at its half rest. It BECOMES whole when a box takes
    // the keyboard or the blocks are pulled up, and then stays whole (MarkSheet.wholeAfter).
    var whole by remember(land) { mutableStateOf(MarkSheet.opensWhole(land, v)) }
    // THE FIRST 300 MS (MarkSheet.answers): a hurried second tap on the dimmed photo, or a finger that had not quite
    // left, must not close the panel it has just opened -- the sheet asks this before a swipe down or a tap on the
    // dimmed photo hides it. ◁ is the sheet's own back and does not ask: Back always answers (as ScopeSheet's note on the
    // same library says, a back press is not stopped here), and onCancel is not guarded -- a sheet hidden and then
    // refused its dismiss would sit off screen with its window up.
    // ONE lambda for the sheet's life: the sheet's state is keyed on it, and a new one would be a new sheet.
    val mayHide: (SheetValue) -> Boolean = remember { { to: SheetValue -> to != SheetValue.Hidden || answersS() } }
    // THE LIBRARY'S SHEET HAS NO HALF ANCHOR (MarkSheet.SKIPS_LIBRARY_HALF; the review of I2, finding 1): material3 1.3
    // sends an expanded sheet back to its half anchor at every resize -- the keyboard coming up, a refused act's line, a
    // Why box appearing -- so the half rest is not the sheet's: it is the CAP on the content below.
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = MarkSheet.SKIPS_LIBRARY_HALF, confirmValueChange = mayHide)
    // A BOX TOOK THE KEYBOARD: the panel stands whole (MarkSheet.standsWhole), or the keys would cover the box
    val boxFocused: () -> Unit = { whole = MarkSheet.wholeAfter(land, whole, typing = true) }
    // THE BLOCKS WERE DRAGGED UP: he pulled the panel up, and it is whole
    val pulled: () -> Unit = { whole = MarkSheet.wholeAfter(land, whole, pulled = true) }
    // THE HALF REST: how high the content under the grip may stand, dp -- half the window less the grip while the panel
    // is not whole, in portrait; null: no cap
    val cap = MarkSheet.contentCapDp(LocalConfiguration.current.screenHeightDp.toDouble(), land, whole)
    // a grip to pull by, in portrait only: in landscape the panel opens whole and there is nothing to pull
    val grip: (@Composable () -> Unit)? = if (MarkSheet.hasGrip(land)) { { MsGrip { y -> onTopS(y) } } } else null
    DisposableEffect(Unit) { onDispose { onTopS(null) } }
    ModalBottomSheet(
        onDismissRequest = a.onCancel,
        sheetState = sheet,
        sheetMaxWidth = Dp.Unspecified,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MS_BG,
        contentColor = MS_FG,
        scrimColor = Color(0x88000000),
        dragHandle = grip,
    ) {
        Box(Modifier.fillMaxWidth().semantics { paneTitle = v.pane }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { a.onBare() }
            .imePadding()) {
            if (land) MsLandscape(v, a, boxFocused) else MsPortrait(v, a, cap, boxFocused, pulled)
            val trade = v.body?.tradeBlock
            val chips = trade?.chips
            if (trade != null && chips != null) MsPicker(trade.title, chips, land, a.onCat)
        }
    }
}

/**
 * THE GRIP, 16 dp (MarkSheet.GRIP_DP): a small bar that says the panel can be pulled. THE PULL ITSELF IS THE BLOCKS
 * DRAGGED UP (MsPortrait: their first scroll makes the panel whole) -- the library's sheet has no anchor above the one
 * it stands at, so a drag up on the bar alone moves nothing, and a drag down from it closes the panel as anywhere on
 * the sheet. It is the panel's TOP EDGE, so it also says where that edge is on the SCREEN each time it moves ([onTop],
 * px) -- the panel is its own window, and the photo under it is lifted by what PhotoLift makes of this.
 */
@Composable
private fun MsGrip(onTop: (Float) -> Unit) {
    val window = LocalView.current
    Box(Modifier.fillMaxWidth().height(MarkSheet.GRIP_DP.dp)
        .onGloballyPositioned { c ->
            val at = IntArray(2)
            window.getLocationOnScreen(at)
            onTop(at[1] + c.positionInRoot().y)
        }, contentAlignment = Alignment.Center) {
        Box(Modifier.width(32.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MS_FIELD_LINE))
    }
}

/**
 * Portrait: the head with Done, the two lines of acts, then KIND, TRADE, STATE and NOTE in one column. [cap]: THE HALF
 * REST -- how high all of it may stand, dp (MarkSheet.contentCapDp); null: the panel is whole. Under the cap the head
 * and the acts stay and the blocks scroll; THE FIRST SCROLL OF THE BLOCKS IS HIM PULLING THE PANEL UP ([pulled]).
 */
@Composable
private fun MsPortrait(v: MarkSheet.PanelView, a: MarkSheetActions, cap: Double?, typing: () -> Unit, pulled: () -> Unit) {
    // the content's width and Done's, px: where each act stands is MarkSheet.slots' answer for them
    var wide by remember { mutableIntStateOf(0) }
    var doneWide by remember { mutableIntStateOf(0) }
    val scroll = rememberScrollState()
    val up by remember { derivedStateOf { scroll.value > 0 } }
    LaunchedEffect(up) { if (up) pulled() }
    val rest = if (cap != null) Modifier.heightIn(max = cap.toFloat().dp) else Modifier
    Column(rest.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
        MsHead(v.head, MarkSheet.HEAD_DP.dp, { wide = it }, { doneWide = it }, a.onDone)
        MsActs(v, false, wide, doneWide, a.onPanelAct)
        val body = v.body
        if (body != null) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val kind = body.kind
                if (kind != null) MsKind(kind, a.onKind)
                val trade = body.tradeBlock
                if (trade != null) MsTrade(trade, a.onTrade)
                val state = body.state
                if (state != null) MsState(state, body, false, a, typing)
                MsNote(body.note, body.focusNote, a, typing)
            }
        }
    }
}

/** Landscape: the head with Done across; the acts on ONE line, short of Done; KIND and TRADE at the top of the left
 *  column, NOTE at its foot; STATE on the right. */
@Composable
private fun MsLandscape(v: MarkSheet.PanelView, a: MarkSheetActions, typing: () -> Unit) {
    var wide by remember { mutableIntStateOf(0) }
    var doneWide by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 6.dp)) {
        MsHead(v.head, MarkSheet.HEAD_LAND_DP.dp, { wide = it }, { doneWide = it }, a.onDone)
        MsActs(v, true, wide, doneWide, a.onPanelAct)
        val body = v.body
        if (body != null) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                        // the grid's KIND and TRADE rows, each with its 4 dp gap: KIND sits 4 dp under the acts and TRADE 4 dp
                        // under it -- 8 dp under the acts where the type has no kinds -- and with neither the note starts
                        // 12 dp under them
                        Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                            val kind = body.kind
                            if (kind != null) MsKind(kind, a.onKind)
                            Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                val trade = body.tradeBlock
                                if (trade != null) MsTrade(trade, a.onTrade)
                            }
                        }
                        MsNote(body.note, body.focusNote, a, typing)
                    }
                    Column(Modifier.weight(1f)) {
                        val state = body.state
                        if (state != null) MsState(state, body, true, a, typing)
                    }
                }
            }
        }
    }
}

/**
 * The head: "FW1 · Window", the trade's dot and short name (a Tape and a chain take none), and DONE AT ITS RIGHT END --
 * gold; dim and deaf while the move picked still needs its why. Its row is the content's whole width, which it reports
 * ([onWide], px) with Done's own ([onDoneWide]): MarkSheet.slots places the acts from the two.
 */
@Composable
private fun MsHead(h: MarkSheet.Head, tall: Dp, onWide: (Int) -> Unit, onDoneWide: (Int) -> Unit, onDone: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(tall).onSizeChanged { onWide(it.width) },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(buildAnnotatedString {
                withStyle(SpanStyle(color = MS_GOLD, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)) { append(h.code) }
                append(MarkSheet.SEP); append(h.name)
            },
            color = MS_FG, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        val trade = h.trade
        if (trade != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MsDot(trade.colour)
                Text(trade.text, color = MS_SOFT, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, softWrap = false)
            }
        }
        Box(Modifier.onSizeChanged { onDoneWide(it.width) }.height(MarkSheet.DONE_DP.dp).alpha(if (h.doneEnabled) 1f else MS_OFF).clip(RoundedCornerShape(16.dp))
            .background(MS_GOLD).clickable(enabled = h.doneEnabled, role = Role.Button) { onDone() }.padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center) {
            Text(h.done, color = MS_INK, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
    }
}

/**
 * THE ACTS, each where MarkSheet.slots puts it across a content [wide] px wide whose Done is [doneWide] px ([land]: one
 * line that ends short of Done; else two, Delete in the last column of the second, apart): 36 dp cells in 44 dp rows
 * (32 in 40 in landscape), EVERY EDGE OF A CELL FROM ITS SLOT -- left, right, top and bottom -- and the height of the
 * whole from MarkSheet.actsDp: the line that holds Delete starts 16 dp under the cells of the line over it
 * (MarkSheet.deleteDropDp), not 8. DELETE is drawn in the delete control's own red outline and word; every other act as a quiet
 * chip. Under them, a refused act's words (MarkSheet.PanelView.said) on one line. Nothing is drawn where there is no
 * act -- and nothing until the head has been measured, in the height the acts will take, so nothing jumps.
 */
@Composable
private fun MsActs(v: MarkSheet.PanelView, land: Boolean, wide: Int, doneWide: Int, onAct: (String) -> Unit) {
    val tall = MarkSheet.actsDp(v.acts, land)
    val dens = LocalDensity.current.density
    if (tall > 0) {
        Box(Modifier.fillMaxWidth().height(tall.dp)) {
            if (wide > 0 && (!land || doneWide > 0)) {
                val w = (wide / dens).toDouble()
                val slots = MarkSheet.slots(v.acts, w, land, w - doneWide / dens)
                val shape = RoundedCornerShape(8.dp)
                for (s in slots) {
                    val c = s.cell
                    Box(Modifier.padding(start = s.l.toFloat().dp, top = s.t.toFloat().dp).width((s.r - s.l).toFloat().dp).height((s.b - s.t).toFloat().dp)
                        .clip(shape).background(if (c.delete) Color.Black else MS_CHIP)
                        .border(if (c.delete) 1.5.dp else 1.dp, if (c.delete) MS_DEL_LINE else MS_LINE, shape)
                        .clickable(role = Role.Button) { onAct(c.id) }
                        .semantics { contentDescription = c.aria }
                        .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center) {
                        Text(c.text, color = if (c.delete) MS_DEL else MS_FG, fontSize = 13.sp, lineHeight = 16.sp,
                            fontWeight = if (c.delete) FontWeight.Bold else FontWeight.Medium, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
    val said = v.said
    if (said != null) {
        Text(said, color = MS_AMBER, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().height(MarkSheet.SAID_DP.dp))
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
 * [typing]: a Why box took the keyboard -- the panel stands whole.
 */
@Composable
private fun MsState(s: MarkSheet.StateBlock, v: MarkSheet.View, land: Boolean, a: MarkSheetActions, typing: () -> Unit) {
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
                            .onFocusChanged { f -> if (f.isFocused) typing() }
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
 * [typing]: the box took the keyboard -- the panel stands whole.
 */
@Composable
private fun MsNote(n: MarkSheet.NoteBlock, focus: Boolean, a: MarkSheetActions, typing: () -> Unit) {
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
                .onFocusChanged { f -> if (f.isFocused) typing() }
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
