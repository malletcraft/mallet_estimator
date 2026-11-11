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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
                    letterSpacing = if (on) (-0.3).sp else if (land) (-0.3).sp else (-0.2).sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
            }
        }
    }
}

/**
 * MORE ▸: "Every mark type · <Face>", every type of the surface in a grid of five -- its icon, its letter in its
 * colour, its short name (sp.js 1455, v42.css .moresheet). The type on is tinted. A pick turns that type on (or,
 * already on, back to Select) and closes the sheet; ✕ closes it. Three rows at most (twelve wall types), about
 * 265 dp: it fits a portrait screen and a landscape one (340 dp wide, at the right) without scrolling.
 */
@Composable
internal fun SpMoreSheet(s: String, tool: String, modifier: Modifier, onPick: (String) -> Unit, onClose: () -> Unit) {
    val tx = LocalTx.current
    val all = SurveyPrepPalette.forSurface(s).all
    Column(modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(SP_SHEET_MORE)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }   // a tap inside never falls through
        .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(SurveyPrepPalette.sheetTitle(tx, s), color = Color(0xFFECEEE8), fontSize = 13.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).clickable { onClose() }
                .semantics { contentDescription = tx.common_close }, contentAlignment = Alignment.Center) {
                Text("✕", color = Color.White, fontSize = 15.sp, lineHeight = 18.sp) // text:value glyph
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (row in all.chunked(5)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (ty in row) {
                        val col = Color(SurveyPrepDraw.argb(ty.colour))
                        val on = ty.letter == tool
                        val shape = RoundedCornerShape(12.dp)
                        Column(Modifier.weight(1f).clip(shape)
                            .background(if (on) lerpColour(SP_TILE, col, 0.30f) else SP_TILE)
                            .border(1.5.dp, col, shape)
                            .clickable { onPick(ty.letter) }
                            .semantics { contentDescription = Masters.type(tx, ty.name); selected = on }
                            .padding(horizontal = 2.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Canvas(Modifier.size(24.dp)) { drawMarkIcon(ty.name, col) }
                            Text(ty.letter, color = col, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            // a More tile's name: one of the four boxes the prototype measured for Devanagari -- 14 sp under मराठी,
                            // or the feet of its marks are cut (design-marathi §4; v61c.2 CSS .mrows53 .mt span)
                            Text(SurveyPrepPalette.short(tx, ty.name), color = Color(0xFFECEEE8), fontSize = 10.sp,
                                lineHeight = if (tx.mr) 14.sp else 12.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
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
 * with nothing to pick; while the quote stage is unread ([cycles]) it is the chip's one other button, underlined, and
 * a press that lands on those words -- and only on them -- cycles it ([onAs]); every other press on the chip's words
 * is still the photo's. [spoken] is what a screen reader is told in place of [text] (the second half has its own label).
 */
@Composable
internal fun SpToolChip(text: String, colour: Int, modifier: Modifier, caution: String? = null, onDone: (() -> Unit)? = null,
                        asFrom: Int = -1, cycles: Boolean = false, spoken: String? = null, onAs: (() -> Unit)? = null,
                        onClose: () -> Unit) {
    val tx = LocalTx.current
    val col = Color(SurveyPrepDraw.argb(colour)); val shape = RoundedCornerShape(16.dp)
    val from = if (asFrom in 0..text.length) asFrom else -1
    val words: AnnotatedString = if (from < 0) AnnotatedString(text) else buildAnnotatedString {
        append(text.substring(0, from))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = if (cycles) TextDecoration.Underline else null)) {
            append(text.substring(from)) }
    }
    // the words as last laid out, for telling a press on the second half from one on the rest: written by the layout,
    // read only by the press, so it recomposes nothing
    val laid = remember { arrayOfNulls<TextLayoutResult>(1) }
    val onAsS by rememberUpdatedState(onAs)
    val button = cycles && from >= 0 && onAs != null
    Row(modifier.heightIn(min = 32.dp).clip(shape).background(Color(0xD0000000)).border(1.5.dp, col, shape).padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f, fill = false).padding(vertical = 4.dp)) {
            Text(words, color = Color.White, fontSize = 12.sp, lineHeight = 15.sp, maxLines = if (from < 0) 2 else 3, overflow = TextOverflow.Ellipsis,
                onTextLayout = { laid[0] = it },
                modifier = Modifier
                    .let { m -> if (spoken == null) m else m.clearAndSetSemantics {
                        contentDescription = spoken
                        if (button) onClick { onAsS?.invoke(); true }
                    } }
                    .let { m -> if (!button) m else m.pointerInput(from) {
                        // as the step strip's own touch area takes a press (routeToStep): a press that is not on the
                        // second half is left unconsumed, and the photo under the chip takes it
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val lay = laid[0] ?: return@awaitEachGesture
                            if (lay.getOffsetForPosition(down.position) < from) return@awaitEachGesture
                            down.consume()
                            do {
                                val ev = awaitPointerEvent()
                                ev.changes.forEach { it.consume() }
                            } while (ev.changes.any { it.pressed })
                            onAsS?.invoke()
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
        else -> drawCircle(c, w * .3f, Offset(w * .5f, h * .5f), style = line)
    }
}
