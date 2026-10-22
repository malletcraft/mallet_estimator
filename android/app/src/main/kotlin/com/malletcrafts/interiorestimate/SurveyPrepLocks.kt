package com.malletcrafts.interiorestimate

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malletcrafts.interiorestimate.pano.SurveyPrepEdit

/**
 * SURVEY PREP'S LOCKS, ITS LONG-PRESS MENU AND ITS SNACKBAR (prototype v52; v52 slice 8). Amit, 14 Oct 2026
 * 02:39 IST: "Give locking lines for reference and outer box lines so it won't move accidentally. In fact each
 * mark type should also be locked." The rules are SurveyPrepEdit in :pano; these only draw them.
 *
 *  - [SpSnack]: v42.css .snack -- 48 dp at least, #ECEEE8 with #1A1A12 text at 14 sp and an UNDO in #8A3A00,
 *    five seconds; the no-UNDO variant (.snack.lkmsg: #FFE9A8 on #3D3410) for a refusal, which wraps rather
 *    than cuts ("FD1 is locked — unlock first" is whole at 360 dp).
 *  - [SpCtxMenu]: v42.css .ctxm -- #2A2D25, 10 dp corners, at least 156 dp wide, a gold monospace head, rows
 *    40 dp tall, Delete in #FF8A8A.
 *  - [SpPadlock] / [SpLinesLock]: sp.js LOCKSVG -- lit gold when locked; a tap toggles.
 *  - [SpTwoTap]: ⋮'s Clear this surface… / Clear whole room…, armed by the first tap, done by the second.
 */

private val SN_BG = Color(0xFFECEEE8)
private val SN_FG = Color(0xFF1A1A12)
private val SN_UNDO = Color(0xFF8A3A00)
private val SN_LK_BG = Color(0xFF3D3410)
private val SN_LK_FG = Color(0xFFFFE9A8)
private val CM_BG = Color(0xFF2A2D25)
private val CM_LINE = Color(0xFF3A3E35)
private val CM_FG = Color(0xFFECEEE8)
private val CM_DEL = Color(0xFFFF8A8A)
private val LK_GOLD = Color(0xFFE0B040)
private val LK_INK = Color(0xFF14160F)
private val LK_OFF = Color(0xFF7E847A)
private val TT_MUTED = Color(0xFF9AA096)
private val TT_DIM = Color(0xFF555A50)

/**
 * The snackbar over the bottom of the photo (or the list): [text], and UNDO when [undo]. It costs no height --
 * the caller overlays it. A refusal ([undo] false) wraps to a second line instead of being cut.
 */
@Composable
internal fun SpSnack(text: String, undo: Boolean, modifier: Modifier, onUndo: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(modifier.shadow(6.dp, shape).clip(shape).background(if (undo) SN_BG else SN_LK_BG).heightIn(min = 48.dp)
        .padding(start = 16.dp, end = if (undo) 6.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text, color = if (undo) SN_FG else SN_LK_FG, fontSize = 14.sp, lineHeight = 20.sp,
            maxLines = if (undo) 1 else 3, softWrap = !undo, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(vertical = if (undo) 0.dp else 6.dp))
        if (undo) {
            Box(Modifier.height(36.dp).clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button) { onUndo() }
                .padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                Text("UNDO", color = SN_UNDO, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp, maxLines = 1)
            }
        }
    }
}

/**
 * The long-press menu on a mark: [header] ("FD1 Door", "🔒 FD1 Door"), then [items] as (action, text) --
 * Delete · Duplicate · Lock · Lock all Door · Cancel (SurveyPrepEdit.menuItems). The caller places it at the press.
 */
@Composable
internal fun SpCtxMenu(header: String, items: List<Pair<String, String>>, modifier: Modifier, onPick: (String) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(modifier.width(IntrinsicSize.Max).widthIn(min = 156.dp).shadow(10.dp, shape).clip(shape).background(CM_BG)
        .border(1.dp, CM_LINE, shape).padding(vertical = 4.dp)) {
        Text(header, color = LK_GOLD, fontSize = 12.sp, lineHeight = 16.sp, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 220.dp).padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 2.dp))
        for ((k, t) in items) {
            Box(Modifier.fillMaxWidth().height(40.dp).clickable(role = Role.Button) { onPick(k) }.padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart) {
                Text(t, color = if (k == SurveyPrepEdit.DELETE) CM_DEL else CM_FG, fontSize = 14.sp, lineHeight = 18.sp, maxLines = 1)
            }
        }
    }
}

/** The padlock glyph on a 24 grid (sp.js LOCKSVG): shut and filled when [on], open and outlined when not. */
private fun DrawScope.drawPadlock(on: Boolean, c: Color) {
    val k = size.width / 24f
    val shackle = Path().apply {
        moveTo(8f * k, 10.5f * k); lineTo(8f * k, 7.5f * k)
        arcTo(Rect(8f * k, 3.5f * k, 16f * k, 11.5f * k), 180f, if (on) 180f else 154.7f, false)
        if (on) lineTo(16f * k, 10.5f * k)
    }
    drawPath(shackle, c, style = Stroke(width = 2.2f * k))
    if (on) drawRoundRect(c, Offset(5f * k, 10.5f * k), Size(14f * k, 10f * k), CornerRadius(2f * k, 2f * k))
    else drawRoundRect(c, Offset(5f * k, 10.5f * k), Size(14f * k, 10f * k), CornerRadius(2f * k, 2f * k), style = Stroke(width = 2f * k))
}

/** A mark's padlock (the selection row): 30 dp wide, lit gold when locked; [label] says what a tap does. [offColour]: the
 *  glyph unlocked -- the reference's own padlock is cyan (v42.css .refrow2 .lk2, v52 slice 12). */
@Composable
internal fun SpPadlock(on: Boolean, label: String, modifier: Modifier = Modifier, offColour: Color = LK_OFF, onClick: () -> Unit) {
    Box(modifier.width(30.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { onClick() }
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (on) LK_GOLD else Color.Transparent).padding(4.dp)) {
            Canvas(Modifier.size(16.dp)) { drawPadlock(on, if (on) LK_INK else offColour) }
        }
    }
}

/** The four lines' padlock in 1 · Lines' Size from row (v52 slice 12, sp.js .lkl): the glyph and its state ("Locked" /
 *  "Unlocked"), [tall] high; [iconOnly] the glyph alone, 32 dp wide -- in portrait, and in landscape beside the
 *  reference's eye -- so both Size from pills keep their whole label. [label] is what a tap does ("Lock lines"). */
@Composable
internal fun SpLinesLock(on: Boolean, state: String, label: String, modifier: Modifier = Modifier, iconOnly: Boolean = false,
                         tall: Dp = 20.dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(tall / 2)
    Row(modifier.height(tall).let { if (iconOnly) it.width(32.dp) else it }.clip(shape)
        .background(if (on) LK_GOLD else Color.Transparent).border(1.5.dp, if (on) LK_GOLD else CM_LINE, shape)
        .clickable(role = Role.Button) { onClick() }.semantics { contentDescription = label; role = Role.Button }
        .padding(horizontal = if (iconOnly) 0.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (iconOnly) Arrangement.Center else Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.size(14.dp)) { drawPadlock(on, if (on) LK_INK else LK_OFF) }
        if (!iconOnly) Text(state, color = if (on) LK_INK else TT_MUTED, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private val RF_CYAN = Color(0xFF22D3EE)
private val RF_RING = Color(0x8822D3EE)
private val RF_INK = Color(0xFF06232A)

/** The eye glyph on a 24 grid (sp.js EYESVG): an eye with its pupil when [on], the eye struck through when not. */
private fun DrawScope.drawEye(on: Boolean, c: Color) {
    val k = size.width / 24f
    val eye = Path().apply {
        moveTo(2f * k, 12f * k)
        // sp.js EYESVG's "M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12z", as four cubics
        cubicTo(5.33f * k, 7.67f * k, 8.67f * k, 5.5f * k, 12f * k, 5.5f * k)
        cubicTo(15.33f * k, 5.5f * k, 18.67f * k, 7.67f * k, 22f * k, 12f * k)
        cubicTo(18.67f * k, 16.33f * k, 15.33f * k, 18.5f * k, 12f * k, 18.5f * k)
        cubicTo(8.67f * k, 18.5f * k, 5.33f * k, 16.33f * k, 2f * k, 12f * k)
        close()
    }
    drawPath(eye, c, style = Stroke(width = 2f * k))
    if (on) drawCircle(c, 3.2f * k, Offset(12f * k, 12f * k))
    else drawLine(c, Offset(4f * k, 20f * k), Offset(20f * k, 4f * k), strokeWidth = 2.2f * k, cap = StrokeCap.Round)
}

/**
 * THE REFERENCE'S EYE, "Show on faces" (v52 slice 12, tools52.css .reye; Amit, 14 Oct 2026 06:45 IST: "it can be visible
 * or invisible but can not be deleted"): 32 x 30 dp (26 tall in landscape), round; on, filled #22d3ee with a dark eye;
 * off, a #22d3ee88 ring with the eye struck through in grey. Its state is said to a screen reader as well as drawn.
 */
@Composable
internal fun SpRefEye(on: Boolean, land: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(15.dp)
    Box(modifier.width(32.dp).height(if (land) 26.dp else 30.dp).clip(shape)
        .background(if (on) RF_CYAN else Color.Transparent)
        .let { if (on) it else it.border(1.5.dp, RF_RING, shape) }
        .clickable(role = Role.Button) { onClick() }
        .semantics { contentDescription = "Show on faces"; stateDescription = if (on) "On" else "Off" },
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(18.dp)) { drawEye(on, if (on) RF_INK else LK_OFF) }
    }
}

/**
 * A two-tap action behind ⋮ (Clear this surface…, Clear whole room…): its [text] already says which state it is
 * in (SurveyPrepEdit.clearLabel); [armed] outlines it in [armColour]; disabled ([enabled] false) it is dim and
 * does nothing. [compact] in landscape, where ⋮'s right column has the least height.
 */
@Composable
internal fun SpTwoTap(text: String, armed: Boolean, enabled: Boolean, compact: Boolean, armColour: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(Modifier.fillMaxWidth().clip(shape).border(1.dp, if (armed) armColour else CM_LINE, shape)
        .clickable(enabled = enabled) { onClick() }.padding(if (compact) 6.dp else 9.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (armed) armColour else if (enabled) TT_MUTED else TT_DIM, fontSize = 13.sp, lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}
