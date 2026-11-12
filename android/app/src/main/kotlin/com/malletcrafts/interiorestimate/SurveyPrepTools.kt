package com.malletcrafts.interiorestimate

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malletcrafts.interiorestimate.pano.PhaseTools
import com.malletcrafts.interiorestimate.pano.Phases
import com.malletcrafts.interiorestimate.pano.SurveyChain
import com.malletcrafts.interiorestimate.pano.SurveyPrep
import com.malletcrafts.interiorestimate.pano.SurveyPrepPalette
import com.malletcrafts.interiorestimate.pano.i18n.Masters

/**
 * SURVEY PREP'S 2 · MARK PALETTE (prototype v52, sp.js 1356-1399 and v42.css .tb.pt; v52 slice 7). ONE row at
 * the bottom, every slot the same width, nothing scrolling sideways: Select, then the three types of the
 * surface, then More ▸ -- a sheet of every type with its name. Exactly one slot is lit: a 26 % tint of its
 * colour and a 2 dp ring. A type is STICKY (each tap on empty photo places one more); tapping the slot that is
 * on goes back to Select. A More type that is on lights the More slot in its colour and names it ("Loft ▾").
 *
 * Amit, 14 Oct 2026 11:48 IST (item 5): "i am getting confused between measure. add one more tool called as
 * Tape which act as a actual tape and can plot measurements like a real tape measure"; 20:45: "i had requested
 * two separate tools. one tape measure like anothr is actual marking simple line." -- v52's eight slots are
 * Select, Tape, Line, Chain, three types and More ▸. A SLOT APPEARS ONLY WHEN ITS TOOL WORKS: Tape and Line
 * joined in slice 9 and Chain (rose, #fb7185) in slice 11 through SurveyPrepPalette.TOOLS, which COMPLETES the eight;
 * the slot list is SurveyPrepPalette.slots, tested in :pano (PaletteTest: exactly 8). Amit, 11 Oct 2026 20:36 (item 11): "electric control panels plate are rectangle. add mark
 * type for it. also add seprate mark type for fan" -- the Switch plate and the Fan are in More ▸, with their
 * own icons. The old one-row SpMarkToolbar (a tap added a mark at its default place) is gone: there is no
 * second way to add a mark.
 */
/**
 * THE TOOL REMEMBERS THE LAST KIND PICKED (port 62 slice L6; T53's subMem, index61c2.html 2470-2473): type and kind of
 * surface -> the sub-kind a tap places, kept PER APP RUN as the prototype keeps it per page load -- never in the prep
 * file, and the type's first again after a restart. A state map, so the chip follows a pick at once. Read through
 * SurveyPrepPalette.subFor, written through SurveyPrepPalette.rememberSub: the rule is theirs.
 */
internal val spKinds = mutableStateMapOf<String, String>()

@Composable
internal fun SpPalette(s: String, tool: String, land: Boolean, onTool: (String) -> Unit, onMore: () -> Unit) {
    val tx = LocalTx.current
    val kind = SurveyPrep.kind(s)
    // .tools.pal: 6 dp over the row, no gap between slots; a slot 50 dp tall (46 in landscape)
    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        for (slot in SurveyPrepPalette.slots(kind)) {
            val on = SurveyPrepPalette.isOn(slot, kind, tool)
            val armed = if (slot.kind == SurveyPrepPalette.SlotKind.MORE) SurveyPrepPalette.moreArmed(kind, tool) else null
            val col = when (slot.kind) {
                SurveyPrepPalette.SlotKind.SELECT -> SP_SELECT
                SurveyPrepPalette.SlotKind.MORE -> armed?.let { Color(SurveyPrepDraw.argb(it.colour)) } ?: SP_MORE
                // Tape yellow, Line blue (v52 slice 9)
                SurveyPrepPalette.SlotKind.TOOL -> Color(SurveyPrepDraw.argb(SurveyPrepPalette.toolColour(slot.key)))
                SurveyPrepPalette.SlotKind.TYPE -> slot.type?.let { Color(SurveyPrepDraw.argb(it.colour)) } ?: SP_MORE
            }
            val name = when (slot.kind) {
                SurveyPrepPalette.SlotKind.SELECT -> tx.common_select
                SurveyPrepPalette.SlotKind.MORE -> SurveyPrepPalette.moreLabel(tx, kind, tool)
                SurveyPrepPalette.SlotKind.TOOL -> SurveyPrepPalette.toolName(tx, slot.key)
                SurveyPrepPalette.SlotKind.TYPE -> SurveyPrepPalette.short(tx, slot.type?.name ?: slot.key)
            }
            val aria = when (slot.kind) {
                SurveyPrepPalette.SlotKind.SELECT -> tx.common_select
                SurveyPrepPalette.SlotKind.MORE -> tx.common_more_2
                SurveyPrepPalette.SlotKind.TOOL -> SurveyPrepPalette.toolName(tx, slot.key)
                SurveyPrepPalette.SlotKind.TYPE -> Masters.type(tx, slot.type?.name ?: slot.key)
            }
            // an armed More type whose name is longer than "Opening ▾" is whole in its slot only drawn tight (CSS 993, long53)
            val tight = slot.kind == SurveyPrepPalette.SlotKind.MORE && SurveyPrepPalette.moreLong(tx, kind, tool)
            val shape = RoundedCornerShape(12.dp)
            Column(
                Modifier.weight(1f).height(if (land) 46.dp else 50.dp).clip(shape)
                    .background(if (on) col.copy(alpha = 0.26f) else Color.Transparent)
                    .let { if (on) it.border(2.dp, col, shape) else it }
                    .clickable { if (slot.kind == SurveyPrepPalette.SlotKind.MORE) onMore() else onTool(slot.key) }
                    .semantics { contentDescription = aria; role = Role.Button; selected = on }
                    .padding(top = 4.dp, bottom = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Canvas(Modifier.size(22.dp)) {
                    when (slot.kind) {
                        SurveyPrepPalette.SlotKind.SELECT -> drawSelectIcon(SP_SELECT)
                        // the More icon keeps its grey dots when a More type is on; only the tint and ring take its colour
                        SurveyPrepPalette.SlotKind.MORE -> drawMoreIcon(SP_MORE)
                        SurveyPrepPalette.SlotKind.TOOL -> when (slot.key) {
                            SurveyPrepPalette.TAPE -> drawTapeIcon(col)
                            SurveyPrepPalette.CHAIN -> drawChainIcon(col)
                            else -> drawLineIcon(col)
                        }
                        SurveyPrepPalette.SlotKind.TYPE -> drawMarkIcon(slot.type?.name ?: "", col)
                    }
                }
                Spacer(Modifier.height(2.dp))
                // 9.5 sp (9 in landscape), a 12 sp line, #AEB3AA; the one on white -- never cut, never two lines
                Text(name, color = if (on) Color.White else SP_SLOT, fontSize = if (land) 9.sp else 9.5.sp, lineHeight = 12.sp,
                    letterSpacing = if (tight) (-1).sp else if (on) (-0.3).sp else if (land) (-0.3).sp else (-0.2).sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
            }
        }
    }
}

/**
 * MORE ▸, ONE ROW PER TRADE (port 62 slice L6; the prototype's renderMore, index61c2.html 2688-2694, CSS 953-968). Amit,
 * 16 Oct 2026, 06:37 IST: "Make mark type categories better by service vendor point of view." -- "Every mark type ·
 * <Face>" over one 44 dp row for each trade that has a type on the surface, GENERAL FIRST (the Note is its first tile):
 * the trade's short name in its own colour, 80 dp wide, up to two lines, then its types as 48 x 40 dp tiles -- the icon,
 * the type's token in its colour, its short name. The type on is tinted. A pick turns that type on (or, already on,
 * back to Select) and closes the sheet; ✕ closes it. A wall has eleven rows: 32 + 11 x 44 + 16 = 532 dp, which a
 * portrait screen holds; in landscape ([land]) the rows are two columns, the first the longer, 700 dp wide at the right
 * (6 + 5 rows: 312 dp). Which rows, which tiles and which column are SurveyPrepPalette.moreRows / moreColumns, tested
 * in :pano; this only draws them.
 */
@Composable
internal fun SpMoreSheet(s: String, tool: String, land: Boolean, modifier: Modifier, onPick: (String) -> Unit, onClose: () -> Unit) {
    val tx = LocalTx.current
    val columns = SurveyPrepPalette.moreColumns(SurveyPrepPalette.moreRows(tx, s), land)
    Column(modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(SP_SHEET_MORE)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }   // a tap inside never falls through
        .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(SurveyPrepPalette.sheetTitle(tx, s), color = Color(0xFFECEEE8), fontSize = 13.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Box(Modifier.size(40.dp, 32.dp).clip(RoundedCornerShape(16.dp)).clickable { onClose() }
                .semantics { contentDescription = tx.common_close }, contentAlignment = Alignment.Center) {
                Text("✕", color = Color.White, fontSize = 15.sp, lineHeight = 18.sp) // text:value glyph
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (column in columns) {
                Column(Modifier.weight(1f)) {
                    for (row in column) SpMoreRow(row, tool, onPick)
                }
            }
        }
    }
}

/** One trade's row of More ▸: its short name in its colour, then its types' tiles. */
@Composable
private fun SpMoreRow(row: SurveyPrepPalette.MoreRow, tool: String, onPick: (String) -> Unit) {
    val tx = LocalTx.current
    Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        // the label: 11 sp on a 13 sp line, two lines at most (14 sp under मराठी, or the feet of its marks are cut)
        Text(row.label, color = Color(SurveyPrepDraw.argb(row.colour)), fontSize = 11.sp, lineHeight = if (tx.mr) 14.sp else 13.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Clip, modifier = Modifier.width(80.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (ty in row.tiles) {
                val col = Color(SurveyPrepDraw.argb(ty.colour))
                val on = ty.letter == tool
                val shape = RoundedCornerShape(10.dp)
                // 16 icon + 10 token + 11 name in 40 dp (16 + 10 + 14 under मराठी, with no padding above and below)
                Column(Modifier.size(48.dp, 40.dp).clip(shape)
                    .background(if (on) lerpColour(SP_TILE, col, 0.30f) else SP_TILE)
                    .border(1.5.dp, col, shape)
                    .clickable { onPick(ty.letter) }
                    .semantics { contentDescription = Masters.type(tx, ty.name); selected = on }
                    .padding(horizontal = 1.dp, vertical = if (tx.mr) 0.dp else 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Canvas(Modifier.size(16.dp)) { drawMarkIcon(ty.name, col) }
                    Text(ty.letter, color = col, fontSize = 9.sp, lineHeight = 10.sp, letterSpacing = (-0.2).sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, softWrap = false)
                    Text(SurveyPrepPalette.short(tx, ty.name), color = Color(0xFFECEEE8), fontSize = 10.sp,
                        lineHeight = if (tx.mr) 14.sp else 11.sp, letterSpacing = (-0.3).sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/**
 * THE LIST OF KINDS under the tool chip (port 62 slice L6; the prototype's t53subList, index61c2.html 6637-6640, CSS
 * 997-1002): "Carpentry unit · kind" over the type's sub-kinds on this surface, two to a row, each 36 dp -- eight at
 * most, four rows -- on black at 90 %, ringed in the type's colour; the kind a tap places is tinted, ringed and bold.
 * A pick is remembered by the tool and closes the list. Inside the photo, 280 dp wide at most. What it lists is
 * SurveyPrepPalette.kindList, tested in :pano.
 */
@Composable
internal fun SpKindList(list: SurveyPrepPalette.KindList, modifier: Modifier, onPick: (String) -> Unit) {
    val col = Color(SurveyPrepDraw.argb(list.colour)); val shape = RoundedCornerShape(12.dp)
    Column(modifier.clip(shape).background(Color(0xE6000000)).border(1.5.dp, col, shape)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }   // a tap inside never falls through to the photo
        .padding(6.dp)) {
        Text(list.title, color = SP_KIND_HEAD, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (pair in list.options.chunked(2)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (o in pair) {
                        val kindShape = RoundedCornerShape(8.dp)
                        Box(Modifier.weight(1f).height(36.dp).clip(kindShape)
                            .background(if (o.on) lerpColour(SP_TILE, col, 0.35f) else SP_TILE)
                            .let { if (o.on) it.border(1.5.dp, col, kindShape) else it }
                            .clickable(role = Role.Button) { onPick(o.id) }
                            .semantics { selected = o.on }
                            .padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                            Text(o.label, color = Color(0xFFECEEE8), fontSize = 12.sp, lineHeight = 14.sp,
                                fontWeight = if (o.on) FontWeight.Bold else FontWeight.Medium, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    repeat(2 - pair.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * THE TOOL CHIP over the top-left of the photo while a type is on (sp.js 1454, v42.css .toolchip): "Placing Door
 * · tap the photo" on black at 82 %, a 1.5 dp ring in the type's colour, and ✕ -- back to Select. Hidden with
 * Select on. It takes no touch of its own except ✕, so a tap under it still reaches the photo.
 * With Tape or Line on (v52 slice 9) it says what the tool does -- live while a drag is pulled ("↔ along the face ·
 * 1234 mm") -- with the amber [caution] under it: "Only things on this surface measure correctly." (sp.js .caut).
 * With Chain on (v52 slice 11) it counts the points ("Chain · 2 points · tap points along the surface, double-tap or ✓
 * to end"), and from the second point a green ✓ ([onDone], sp.html data-kdone "End the chain") ends the chain.
 *
 * ITS SECOND HALF, "· as Client wish" (port 62 slice L4; the prototype's asChipHTML, index61c2.html 2860, CSS 1071 and
 * 1280): with a type on, [text] ends in what a tap places the mark AS, from [asFrom] on (SurveyPrepPalette.Chip; -1:
 * none) -- drawn bold, in the same run of words, so it wraps with them. Where the phase fixes it, it is plain words
 * with nothing to pick; while the quote stage is unread ([cycles]) it is a button, underlined, and
 * a press that lands on those words -- and only on them -- cycles it ([onAs]); every other press on the chip's words
 * is still the photo's. [spoken] is what a screen reader is told in place of [text] (each button has its own label).
 *
 * ITS KIND BUTTON, "Wardrobe ▾" (port 62 slice L6; the prototype's t53subBtn, index61c2.html 6635-6636, CSS 996): for a
 * type that has sub-kinds on the surface the chip names the kind a tap places, in [text] from [kindFrom] up to [kindTo]
 * (SurveyPrepPalette.Chip; -1: none) -- bold and underlined, a button as the second half's is: a press on those words
 * opens or shuts the list of kinds ([onKind]). [kindLabel] is what a screen reader calls it ("Kind: Wardrobe — tap to
 * change").
 */
@Composable
internal fun SpToolChip(text: String, colour: Int, modifier: Modifier, caution: String? = null, onDone: (() -> Unit)? = null,
                        asFrom: Int = -1, cycles: Boolean = false, spoken: String? = null, onAs: (() -> Unit)? = null,
                        kindFrom: Int = -1, kindTo: Int = -1, kindLabel: String? = null, onKind: (() -> Unit)? = null,
                        onClose: () -> Unit) {
    val tx = LocalTx.current
    val col = Color(SurveyPrepDraw.argb(colour)); val shape = RoundedCornerShape(16.dp)
    val from = if (asFrom in 0..text.length) asFrom else -1
    // the kind button's words: inside the text, and before the second half
    val kindOk = kindFrom >= 0 && kindTo > kindFrom && kindTo <= text.length && (from < 0 || kindTo <= from)
    val k0 = if (kindOk) kindFrom else -1
    val k1 = if (kindOk) kindTo else -1
    val words: AnnotatedString = if (from < 0 && k0 < 0) AnnotatedString(text) else buildAnnotatedString {
        val head = if (k0 < 0) 0 else k1
        if (k0 >= 0) {
            append(text.substring(0, k0))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) { append(text.substring(k0, k1)) }
        }
        append(text.substring(head, if (from < 0) text.length else from))
        if (from >= 0) withStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = if (cycles) TextDecoration.Underline else null)) {
            append(text.substring(from)) }
    }
    // the words as last laid out, for telling a press on a button's words from one on the rest: written by the layout,
    // read only by the press, so it recomposes nothing
    val laid = remember { arrayOfNulls<TextLayoutResult>(1) }
    val onAsS by rememberUpdatedState(onAs)
    val onKindS by rememberUpdatedState(onKind)
    val asButton = cycles && from >= 0 && onAs != null
    val kindButton = k0 >= 0 && onKind != null
    Row(modifier.heightIn(min = 32.dp).clip(shape).background(Color(0xD0000000)).border(1.5.dp, col, shape).padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f, fill = false).padding(vertical = 4.dp)) {
            Text(words, color = Color.White, fontSize = 12.sp, lineHeight = 15.sp, maxLines = if (from < 0 && k0 < 0) 2 else 3, overflow = TextOverflow.Ellipsis,
                onTextLayout = { laid[0] = it },
                modifier = Modifier
                    .let { m -> if (spoken == null) m else m.clearAndSetSemantics {
                        contentDescription = spoken
                        // one press for a screen reader: the kind's list where the chip has a kind, else the cycling second half
                        if (kindButton) onClick(label = kindLabel) { onKindS?.invoke(); true }
                        else if (asButton) onClick { onAsS?.invoke(); true }
                    } }
                    .let { m -> if (!asButton && !kindButton) m else m.pointerInput(from, k0, k1, asButton, kindButton) {
                        // as the step strip's own touch area takes a press (routeToStep): a press that is not on a
                        // button's words is left unconsumed, and the photo under the chip takes it
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val lay = laid[0] ?: return@awaitEachGesture
                            val at = lay.getOffsetForPosition(down.position)
                            val onKindWords = kindButton && at >= k0 && at < k1
                            val onAsWords = asButton && at >= from
                            if (!onKindWords && !onAsWords) return@awaitEachGesture
                            down.consume()
                            do {
                                val ev = awaitPointerEvent()
                                ev.changes.forEach { it.consume() }
                            } while (ev.changes.any { it.pressed })
                            if (onKindWords) onKindS?.invoke() else onAsS?.invoke()
                        }
                    } })
            if (caution != null) Text(caution, color = SP_CAUT, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 2)
        }
        if (onDone != null) Box(Modifier.size(32.dp).clip(shape).clickable { onDone() }
            .semantics { contentDescription = SurveyChain.doneLabel(tx); role = Role.Button }, contentAlignment = Alignment.Center) {
            Text("✓", color = SP_DONE, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold) // text:value glyph
        }
        Box(Modifier.size(32.dp).clip(shape).clickable { onClose() }.semantics { contentDescription = tx.sp_aria_back_select },
            contentAlignment = Alignment.Center) {
            Text("✕", color = Color.White, fontSize = 15.sp, lineHeight = 18.sp) // text:value glyph
        }
    }
}

/**
 * THE PHASE BAND (port 62 slice L4; the prototype's P54 band, index61c2.html spPre 4218-4224 and CSS 1260-1270): one
 * 20 dp line that says which phase the open capture's project is in -- five 8 x 3 dp pips, lit up to the phase (all
 * five at Client approved, none while unread), the phase's number and name bold in gold ("2 · Wish list"), then its
 * words in the muted grey, cut with an ellipsis when the line is short ("· every mark you place is a client wish");
 * unread, the words alone in amber ("Phase not read yet — S5 decides when online"). It is why a tool is not there, so
 * it is always on screen; it is never a control -- the phase moves on the project screen -- and takes no touch.
 * In portrait it is survey prep's first line, over the bar (6 dp inset); in landscape ([land]) it lies over the foot of
 * the left column -- the photo, the list or the drawing -- on black at 72 %, its lower corners the photo's 12 dp, 10 dp
 * inset, so the right-hand column loses no height. What it says is PhaseTools.band, tested in :pano.
 */
@Composable
internal fun SpPhaseBand(band: PhaseTools.Band, land: Boolean, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(20.dp)
        .let { if (land) it.clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)).background(SP_BAND_BG) else it }
        .clearAndSetSemantics { contentDescription = band.label }
        .padding(horizontal = if (land) 10.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (i in 0 until Phases.N) Box(Modifier.size(8.dp, 3.dp).clip(RoundedCornerShape(2.dp))
                .background(if (i < band.lit) SP_SELECT else SP_BAND_DIM))
        }
        if (band.head.isNotEmpty()) Text(band.head, color = SP_SELECT, fontSize = 11.sp, lineHeight = 20.sp, letterSpacing = 0.3.sp,
            fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        if (band.rest.isNotEmpty()) Text(band.rest, color = if (band.unread) SP_CAUT else SP_BAND_FG, fontSize = 11.sp, lineHeight = 20.sp,
            letterSpacing = 0.3.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

private fun lerpColour(a: Color, b: Color, t: Float) =
    Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, 1f)

/** Select: an arrow pointer (sp.js selIcon, drawn on a 24 grid). */
private fun DrawScope.drawSelectIcon(c: Color) {
    val k = size.width / 24f
    val p = Path().apply {
        moveTo(6 * k, 3 * k); lineTo(6 * k, 19 * k); lineTo(10 * k, 15 * k); lineTo(13 * k, 21 * k)
        lineTo(15.5f * k, 20 * k); lineTo(12.6f * k, 14 * k); lineTo(18 * k, 14 * k); close()
    }
    drawPath(p, c)
}

/** Tape (v52 slice 9, sp.js tapeIcon): its case, the reel in it, and the tongue pulled out with its hook. */
private fun DrawScope.drawTapeIcon(c: Color) {
    val k = size.width / 24f; val sw = Stroke(width = 2f * k)
    drawRoundRect(c, Offset(2f * k, 5f * k), Size(13f * k, 14f * k), CornerRadius(3f * k, 3f * k), style = sw)
    drawCircle(c, 3f * k, Offset(8.5f * k, 12f * k), style = sw)
    drawRect(c, Offset(14f * k, 15f * k), Size(8f * k, 4f * k))
    drawRect(c, Offset(21f * k, 13f * k), Size(2f * k, 6f * k))
    for (x in listOf(16f, 18.5f)) drawRect(SP_INK, Offset(x * k, 15f * k), Size(1f * k, 2f * k))
}

/** Line (v52 slice 9, sp.js lineIcon): a diagonal with an arrow head at each end -- the v46 ruler is gone. */
private fun DrawScope.drawLineIcon(c: Color) {
    val k = size.width / 24f
    drawLine(c, Offset(5.5f * k, 18.5f * k), Offset(18.5f * k, 5.5f * k), 2f * k)
    fun hd(x: Float, y: Float, ux: Float, uy: Float) = drawPath(Path().apply {
        moveTo(x * k, y * k); lineTo((x - ux * 6 - uy * 3) * k, (y - uy * 6 + ux * 3) * k)
        lineTo((x - ux * 6 + uy * 3) * k, (y - uy * 6 - ux * 3) * k); close() }, c)
    val r = 0.70710677f
    hd(20f, 4f, r, -r); hd(4f, 20f, -r, r)
}

/** Chain (v52 slice 11, sp.js chainIcon): a line across with a tick at each end and one between -- a dimension chain. */
private fun DrawScope.drawChainIcon(c: Color) {
    val k = size.width / 24f; val w = 2f * k
    drawLine(c, Offset(2f * k, 12f * k), Offset(22f * k, 12f * k), w)
    for (x in listOf(2f, 10f, 22f)) drawLine(c, Offset(x * k, 7f * k), Offset(x * k, 17f * k), w)
}

/** More: nine dots (sp.js moreIcon). */
private fun DrawScope.drawMoreIcon(c: Color) {
    val k = size.width / 24f
    for (x in listOf(6f, 12f, 18f)) for (y in listOf(6f, 12f, 18f)) drawCircle(c, 1.8f * k, Offset(x * k, y * k))
}

private val SP_SELECT = Color(0xFFE0B040)
private val SP_MORE = Color(0xFFCFD3CB)
private val SP_SLOT = Color(0xFFAEB3AA)
private val SP_SHEET_MORE = Color(0xFF1E211B)
private val SP_TILE = Color(0xFF2A2D25)
/** The tape icon's ink, and the chip's amber caution line (sp.js .caut). */
private val SP_INK = Color(0xFF14160F)
private val SP_CAUT = Color(0xFFFFC46B)
/** The phase band (phase54.css .p54band): its words #9AA096, an unlit pip #555A50, and in landscape black at 72 % behind it. */
private val SP_BAND_FG = Color(0xFF9AA096)
private val SP_BAND_DIM = Color(0xFF555A50)
private val SP_BAND_BG = Color(0xB8000000)
/** The list of kinds' title (trades53.css .t53sh: #B9BCB2). */
private val SP_KIND_HEAD = Color(0xFFB9BCB2)
/** The chain chip's ✓ (v42.css .toolchip button[data-kdone]: #7dff8a, bold). */
private val SP_DONE = Color(0xFF7DFF8A)

/** One mark type's icon, by its NAME (letters repeat across surfaces: C is a column everywhere, B a beam). */
private fun DrawScope.drawMarkIcon(name: String, c: Color) {
    val w = size.width; val h = size.height; val sw = w * 0.09f
    val line = Stroke(width = sw)
    when (name) {
        "Window" -> {
            drawRect(c, Offset(w * .12f, h * .12f), Size(w * .76f, h * .76f), style = line)
            drawLine(c, Offset(w * .5f, h * .12f), Offset(w * .5f, h * .88f), sw)
            drawLine(c, Offset(w * .12f, h * .5f), Offset(w * .88f, h * .5f), sw)
        }
        "Door" -> {
            drawRect(c, Offset(w * .28f, h * .06f), Size(w * .44f, h * .88f), style = line)
            drawCircle(c, w * .06f, Offset(w * .62f, h * .52f))
        }
        "Niche" -> {
            drawArc(c, 180f, 180f, false, Offset(w * .2f, h * .1f), Size(w * .6f, h * .6f), style = line)
            drawLine(c, Offset(w * .2f, h * .4f), Offset(w * .2f, h * .92f), sw)
            drawLine(c, Offset(w * .8f, h * .4f), Offset(w * .8f, h * .92f), sw)
        }
        "Loft" -> {
            drawRect(c, Offset(w * .1f, h * .1f), Size(w * .8f, h * .8f), style = line)
            drawRect(c, Offset(w * .1f, h * .1f), Size(w * .8f, h * .28f))
        }
        "Column" -> drawRect(c, Offset(w * .36f, h * .05f), Size(w * .28f, h * .9f))
        "Beam" -> drawRect(c, Offset(w * .05f, h * .36f), Size(w * .9f, h * .28f))
        "Opening" -> drawRect(c, Offset(w * .14f, h * .1f), Size(w * .72f, h * .8f),
            style = Stroke(width = sw, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * .12f, w * .08f), 0f)))
        "Electric point" -> {
            drawCircle(c, w * .4f, Offset(w * .5f, h * .5f), style = line)
            drawCircle(c, w * .07f, Offset(w * .36f, h * .5f))
            drawCircle(c, w * .07f, Offset(w * .64f, h * .5f))
        }
        // v40: a bulb -- its glass, then the cap in three bands
        "Bulb point" -> {
            drawArc(c, 140.4f, 259.2f, false, Offset(w * .24f, h * .38f - w * .26f), Size(w * .52f, w * .52f), style = line)
            drawRect(c, Offset(w * .36f, h * .62f), Size(w * .28f, h * .1f))
            drawRect(c, Offset(w * .38f, h * .76f), Size(w * .24f, h * .08f))
            drawRect(c, Offset(w * .43f, h * .87f), Size(w * .14f, h * .06f))
        }
        // v52: a plate with two switches
        "Switch plate" -> {
            drawRect(c, Offset(w * .08f, h * .28f), Size(w * .84f, h * .44f), style = line)
            drawRect(c, Offset(w * .26f, h * .38f), Size(w * .14f, h * .24f))
            drawRect(c, Offset(w * .6f, h * .38f), Size(w * .14f, h * .24f))
        }
        // v52: three blades round a hub
        "Fan" -> {
            drawCircle(c, w * .08f, Offset(w * .5f, h * .5f))
            for (i in 0 until 3) rotate(-90f + 120f * i, Offset(w * .5f, h * .5f)) {
                drawOval(c, Offset(w * .5f + w * .24f - w * .2f, h * .5f - h * .08f), Size(w * .4f, h * .16f))
            }
        }
        "Step" -> {
            drawRect(c, Offset(w * .08f, h * .58f), Size(w * .84f, h * .32f))
            drawRect(c, Offset(w * .46f, h * .18f), Size(w * .46f, h * .4f))
        }
        "Other" -> for (i in 0 until 3) drawCircle(c, w * .08f, Offset(w * (.22f + .28f * i), h * .5f))
        else -> if (!drawNewIcon(name, c)) drawCircle(c, w * .3f, Offset(w * .5f, h * .5f), style = line)
    }
}

/**
 * THE TEN NEW TYPES' ICONS (port 62 slice L6; the prototype's T53.icon, index61c2.html 2649-2667), by the type's NAME
 * as the twelve above: each drawn on the prototype's 24 x 24 grid, scaled to the slot's or the tile's size, in the
 * type's colour, round caps and joins. False: [name] is not one of the ten. A curve the prototype draws as a quadratic
 * is drawn as the same curve by its cubic.
 */
private fun DrawScope.drawNewIcon(name: String, c: Color): Boolean {
    val k = size.width / 24f
    fun at(x: Float, y: Float) = Offset(x * k, y * k)
    fun pen(w: Float) = Stroke(width = w * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun seg(x1: Float, y1: Float, x2: Float, y2: Float, w: Float) = drawLine(c, at(x1, y1), at(x2, y2), strokeWidth = w * k, cap = StrokeCap.Round)
    /** A quadratic from the path's last point (x0, y0) through the control (cx, cy) to (x, y), as its cubic. */
    fun Path.quad(x0: Float, y0: Float, cx: Float, cy: Float, x: Float, y: Float) =
        cubicTo((x0 + 2f * (cx - x0) / 3f) * k, (y0 + 2f * (cy - y0) / 3f) * k, (x + 2f * (cx - x) / 3f) * k, (y + 2f * (cy - y) / 3f) * k, x * k, y * k)
    when (name) {
        // the pin: a white disc, an ink ring, a pencil across it
        "Note" -> { // text:value identity: the type's name, the key of its icon (shown through Masters.type)
            drawCircle(c, 9f * k, at(12f, 12f))
            drawCircle(SP_INK, 9f * k, at(12f, 12f), style = Stroke(width = 2f * k))
            rotate(-45f, at(12f, 12f)) {
                drawRect(SP_INK, at(7f, 10.4f), Size(8f * k, 3.2f * k))
                drawPath(Path().apply { moveTo(15f * k, 10.4f * k); lineTo(18f * k, 12f * k); lineTo(15f * k, 13.6f * k); close() }, SP_INK)
            }
        }
        // a grill: a frame, three bars and a rail
        "Grill" -> { // text:value identity: the type's name, the key of its icon (shown through Masters.type)
            drawRect(c, at(3f, 3f), Size(18f * k, 18f * k), style = pen(1.8f))
            for (x in listOf(7.5f, 12f, 16.5f)) seg(x, 3f, x, 21f, 1.8f)
            seg(3f, 12f, 21f, 12f, 1.8f)
        }
        // a split unit: its body, its louvre, three streams of air
        "AC unit" -> { // text:value identity: the type's name, the key of its icon (shown through Masters.type)
            drawRoundRect(c, at(2f, 5f), Size(20f * k, 9f * k), CornerRadius(2.5f * k, 2.5f * k), style = pen(2f))
            seg(5f, 11f, 19f, 11f, 2f)
            for (x in listOf(7f, 12f, 17f)) drawPath(Path().apply { moveTo(x * k, 16f * k); quad(x, 16f, x - 2f, 18.5f, x, 21f) }, c, style = pen(1.6f))
        }
        // a ceiling drop: the slab, two drops, the void between them dashed
        "Ceiling drop" -> { // text:value identity: the type's name, the key of its icon (shown through Masters.type)
            seg(2f, 4f, 22f, 4f, 2f)
            drawRect(c, at(2f, 4f), Size(7f * k, 9f * k))
            drawRect(c, at(15f, 4f), Size(7f * k, 9f * k))
            drawLine(c, at(9f, 9f), at(15f, 9f), strokeWidth = 1.4f * k, cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * k, 2f * k), 0f))
        }
        // a tap and its drop
        "Plumbing point" -> { // text:value identity: the type's name, the key of its icon (shown through Masters.type)
            seg(12f, 3f, 12f, 9f, 2.2f); seg(5f, 9f, 19f, 9f, 2.2f); seg(19f, 9f, 19f, 13f, 2.2f)
            drawPath(Path().apply { moveTo(12f * k, 14f * k); quad(12f, 14f, 8.5f, 18.5f, 12f, 21f); quad(12f, 21f, 15.5f, 18.5f, 12f, 14f); close() }, c)
        }
        // a track and four folds of curtain
        "Curtain track" -> { // text:value identity: the type's name, the key of its icon (shown through Masters.type)
            seg(2f, 4f, 22f, 4f, 2.2f)
            for (i in 0 until 4) {
                val x = 4f + i * 4.6f
                drawPath(Path().apply { moveTo(x * k, 4f * k); cubicTo((x + 2.4f) * k, 10f * k, (x - 1.4f) * k, 15f * k, (x + 1f) * k, 21f * k) }, c, style = pen(1.6f))
            }
        }
        // a two-door wardrobe
        "Carpentry unit" -> { // text:value identity: the type's name, the key of its icon (shown through Masters.type)
            drawRect(c, at(4f, 2f), Size(16f * k, 20f * k), style = pen(1.9f))
            seg(12f, 2f, 12f, 22f, 1.9f)
            drawRect(c, at(9f, 10f), Size(1.8f * k, 4f * k))
            drawRect(c, at(13.2f, 10f), Size(1.8f * k, 4f * k))
        }
        // a point and its signal: two arcs above, two below
        "Low-voltage point" -> { // text:value identity: the type's name, the key of its icon (shown through Masters.type)
            drawCircle(c, 3f * k, at(12f, 12f))
            for (r in listOf(6f, 9.5f)) {
                drawArc(c, -144f, 108f, false, at(12f - r, 12f - r), Size(2f * r * k, 2f * r * k), style = pen(2f))
                drawArc(c, 36f, 108f, false, at(12f - r, 12f - r), Size(2f * r * k, 2f * r * k), style = pen(2f))
            }
        }
        // a basin and its tap
        "Sanitary ware" -> { // text:value identity: the type's name, the key of its icon (shown through Masters.type)
            drawPath(Path().apply { moveTo(3f * k, 9f * k); lineTo(21f * k, 9f * k); quad(21f, 9f, 20f, 19f, 12f, 19f); quad(12f, 19f, 4f, 19f, 3f, 9f); close() }, c, style = pen(2f))
            seg(12f, 3f, 12f, 6.5f, 2f); seg(9f, 3.5f, 15f, 3.5f, 2f)
        }
        // a crack
        "Defect" -> { // text:value identity: the type's name, the key of its icon (shown through Masters.type)
            drawPath(Path().apply { moveTo(9f * k, 2f * k); lineTo(13f * k, 8f * k); lineTo(8f * k, 12f * k); lineTo(14f * k, 17f * k); lineTo(11f * k, 22f * k) }, c, style = pen(2f))
            seg(13f, 8f, 18f, 10f, 2f)
        }
        else -> return false
    }
    return true
}
