package com.malletcrafts.interiorestimate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malletcrafts.interiorestimate.pano.MarkDims
import com.malletcrafts.interiorestimate.pano.MeasureFlow
import com.malletcrafts.interiorestimate.pano.MeasurePad
import com.malletcrafts.interiorestimate.pano.StepRoute

/**
 * THE MEASURE BAR AND ITS PAD (port 63 slice M2, 16 Nov 2026; DESIGN-port63 §1.4). Amit, 15 Nov 2026, after his first
 * use of the app on a client site: "Having both measurements foto and measured and both is very confusing while using
 * app. The laundry list of measurements shown below screen is also frustrating. … imagemeter shows all dimensions on
 * the annotation there itself which is easy to use."
 *
 * So measuring happens on the photo, and under the photo there is ONE bar, the same in steps 2, 3 and 4:
 *
 *     label · ‹ · the measure's words over the progress · the figure box · D2 · ›
 *
 * and, only when asked for (a tap on the figure box, or on the plate that is already active), the app's own PAD of
 * four rows -- not the system keyboard, which covers far more of the screen, jumps at every field and has no Next.
 *
 * THIS FILE ONLY DRAWS. What the bar is and says is MeasureFlow.Bar, built in :pano (MeasureFlow.onFace for
 * 3 · Measure, MeasureFlow.onItem for 2 · Mark and 4 · 360) and tested there; what a key of the pad does to the text
 * is MeasurePad; the heights are StepRoute.measureBarDp / padRowDp, whose arithmetic of what fits a landscape column
 * is tested too. The widths are DESIGN-port63 §1.4's: label 52 · ‹ 36 · box 64 · D2 40 · › 36 dp, 4 dp apart -- which
 * leaves the two lines about 80 dp in portrait and 58 in landscape, so line 1 is cut with an ellipsis where it must.
 * Every word is a tx accessor (English first: the `sp.measure.*` keys' Marathi is pending, android/i18n/mr-pending.tsv);
 * the glyphs are the same in every language.
 */
@Composable
internal fun SpMeasureBar(
    bar: MeasureFlow.Bar,
    land: Boolean,
    /** The label cell's left edge: the picked mark's state colour (MarkLife.barColour, rgb), or null: none. */
    cellBar: Int?,
    /** The pad's uncommitted digits (MeasurePad.live): null, the box shows the dimension's own one figure; empty, its
     *  `mm` hint. */
    live: String?,
    /** The label cell: the picked item's Mark sheet opens. */
    onCell: () -> Unit,
    /** ‹ and ›. */
    onPrev: () -> Unit,
    onNext: () -> Unit,
    /** The figure box: the pad opens. */
    onBox: () -> Unit,
    /** D2: the laser fires. */
    onD2: () -> Unit,
    /** A finished face's button: the next face with something left. */
    onNextFace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tx = LocalTx.current
    val on = bar.kind == MeasureFlow.Kind.ACTIVE
    Row(modifier.fillMaxWidth().height(StepRoute.measureBarDp(land).dp)
        .background(if (on) MB_ON else MB_OFF).border(1.5.dp, if (on) MB_GOLD else MB_EDGE)
        .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        // THE LABEL CELL, 52 dp: the picked item's code over its type's short name (or `locked`), its state's 3 dp bar on
        // the left edge; a tap opens its Mark sheet -- at every stage, after Survey too, where the rest of the bar is not
        bar.cell?.let { cell ->
            Text(cell.text, color = MB_GOLD, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(52.dp).clip(RoundedCornerShape(3.dp)).drawBehind {
                    if (cellBar != null) drawRect(Color(SurveyPrepDraw.argb(cellBar)), size = Size(3.dp.toPx(), size.height))
                }.clickable(role = Role.Button) { onCell() }
                    .semantics { contentDescription = tx.life_aria_open_mark_sheet_x(cell.code) }
                    .padding(vertical = 4.dp))
        }
        if (bar.prev) MbArrow("‹", tx.sp_measure_aria_previous, onPrev) // text:value glyph
        // LINE 1, the measure's own words alone (12 sp, two lines at most), over LINE 2, the progress (10 sp, one line);
        // with nothing being measured the one line there is -- the progress, a finished face, nothing to measure, or
        // after Survey the phase's refusal -- at 11 sp, three lines at most
        Column(Modifier.weight(1f)) {
            bar.what?.let { what ->
                Text(what, color = Color.White, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (on) Text(bar.line, color = MB_MUTED, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            else Text(bar.line, color = if (bar.kind == MeasureFlow.Kind.READ_ONLY) MB_AMB else MB_FG, fontSize = 11.sp, lineHeight = 13.sp,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        // THE FIGURE BOX, 64 dp: the same ONE figure as the plate on the photo, in the same style -- the reading, else ≈
        // and the photo's estimate, else ? -- on the gold plate of the one being measured; from the first key of the pad
        // it shows the digits typed. A tap opens the pad.
        bar.figure?.let { fig ->
            val typed = live != null
            val st = MarkDims.style(if (typed) MarkDims.State.MEASURED else fig.state, true)
            Row(Modifier.width(64.dp).height(32.dp).clip(RoundedCornerShape(6.dp))
                .background(Color(SurveyPrepDraw.argb(st.fill, st.fillAlpha)))
                .border(1.5.dp, Color(SurveyPrepDraw.argb(st.outline ?: MarkDims.LINE_COLOUR)), RoundedCornerShape(6.dp))
                .clickable(role = Role.Button) { onBox() }
                .semantics { contentDescription = tx.sp_measure_aria_type }
                .padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                if (live != null && live.isEmpty()) Text("mm", color = MB_DIM, fontSize = 12.sp, maxLines = 1) // text:value unit
                else Text(live ?: fig.text, color = Color(SurveyPrepDraw.argb(st.ink)), fontFamily = FontFamily.Monospace,
                    fontWeight = if (st.bold) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp, maxLines = 1, softWrap = false)
                // the check's mark after a reading that is far from the photo: a mark, never a second number
                if (!typed && fig.check) Text(MarkDims.CHECK_GLYPH, color = Color(SurveyPrepDraw.argb(MarkDims.CHECK_COLOUR)),
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            }
        }
        // D2, 40 dp: the laser fires, as the meter's own button does -- a red dot and its name
        if (bar.d2) Row(Modifier.width(40.dp).height(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF3A1512))
            .border(1.dp, Color(0xFFFF5A4A), RoundedCornerShape(8.dp)).clickable(role = Role.Button) { onD2() },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFFF3B2F)))
            Spacer(Modifier.width(3.dp))
            Text("D2", color = Color(0xFFFFB3A8), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1) // text:value the laser, DISTO D2
        }
        if (bar.next) MbArrow("›", tx.sp_measure_aria_skip, onNext) // text:value glyph
        // A FINISHED FACE offers the next one with something left, in ›'s place: "Next: Right wall · 4 to measure ▸". The
        // screen never changes face by itself -- he has to turn round.
        bar.nextFaceText?.let { next ->
            Text(next, color = MB_INK, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                maxLines = 3, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(112.dp).clip(RoundedCornerShape(8.dp)).background(MB_GOLD)
                    .clickable(role = Role.Button) { onNextFace() }.padding(horizontal = 6.dp, vertical = 4.dp))
        }
    }
}

/** ‹ or ›: 36 dp wide and as tall as the bar; [label] is what a screen reader says. */
@Composable
private fun MbArrow(glyph: String, label: String, onClick: () -> Unit) {
    Box(Modifier.width(36.dp).fillMaxHeight().clickable(role = Role.Button) { onClick() }.semantics { contentDescription = label },
        contentAlignment = Alignment.Center) {
        Text(glyph, color = Color.White, fontSize = 24.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/**
 * THE PAD (DESIGN-port63 §1.4): four rows of StepRoute.padRowDp, under the measure bar, the photo giving up the height
 * so nothing is covered.
 *
 *      1   2   3   ⌫
 *      4   5   6   Clear
 *      7   8   9   Next ▸
 *      ⌄   [  0  ]   (Next ▸, two rows tall)
 *
 * It holds no text of its own: a digit, ⌫ and Clear are handed to [onKey] (MeasurePad.press decides what they do to
 * the text -- the first digit replaces what was there, six at most, a leading 0 ignored); Next ▸ commits and moves on
 * as › does ([onNext]); ⌄ commits and hides it ([onHide]). Digits only, whole mm.
 */
@Composable
internal fun SpMeasurePad(
    land: Boolean,
    onKey: (MeasurePad.Key) -> Unit,
    onNext: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tx = LocalTx.current
    Row(modifier.fillMaxWidth().height((StepRoute.PAD_ROWS * StepRoute.padRowDp(land)).dp)) {
        // the three columns of digits, and under them ⌄ and a 0 two columns wide
        Column(Modifier.weight(3f).fillMaxHeight()) {
            for (row in 0 until 3) {
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    for (col in 1..3) {
                        val n = row * 3 + col
                        MbKey(n.toString(), Modifier.weight(1f)) { onKey(MeasurePad.Key.Digit(n)) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().weight(1f)) {
                MbKey("⌄", Modifier.weight(1f), quiet = true) { onHide() } // text:value glyph
                MbKey(0.toString(), Modifier.weight(2f)) { onKey(MeasurePad.Key.Digit(0)) }
            }
        }
        // ⌫, Clear, and Next two rows tall
        Column(Modifier.weight(1f).fillMaxHeight()) {
            MbKey("⌫", Modifier.weight(1f), quiet = true) { onKey(MeasurePad.Key.Back) } // text:value glyph
            MbKey(tx.fp_clear, Modifier.weight(1f), quiet = true, word = true) { onKey(MeasurePad.Key.Clear) }
            MbKey(tx.common_next + " ▸", Modifier.weight(2f), gold = true, word = true) { onNext() } // text:value glyph
        }
    }
}

/** One key of the pad: it fills the cell its [modifier] weighs out, 1.5 dp clear of its neighbours. */
@Composable
private fun MbKey(label: String, modifier: Modifier, quiet: Boolean = false, gold: Boolean = false, word: Boolean = false, onClick: () -> Unit) {
    Box(modifier.fillMaxSize().padding(1.5.dp).clip(RoundedCornerShape(7.dp))
        .background(if (gold) MB_GOLD else if (quiet) MB_KEY_QUIET else MB_KEY)
        .clickable(role = Role.Button) { onClick() },
        contentAlignment = Alignment.Center) {
        Text(label, color = if (gold) MB_INK else Color.White, fontSize = if (word) 13.sp else 18.sp, fontWeight = FontWeight.Bold,
            fontFamily = if (word) FontFamily.Default else FontFamily.Monospace, maxLines = 1, softWrap = false)
    }
}

/** The bar's ground while a dimension is being measured, and otherwise; its gold; its edge when nothing is. */
private val MB_ON = Color(0xFF2A2410)
private val MB_OFF = Color(0xFF1A1C18)
private val MB_GOLD = Color(0xFFE0B040)
private val MB_EDGE = Color(0xFF3A3E35)
private val MB_FG = Color(0xFFECEEE8)
private val MB_MUTED = Color(0xFF9AA096)
private val MB_DIM = Color(0xFF555A50)
private val MB_AMB = Color(0xFFFFC46B)
/** Ink on gold. */
private val MB_INK = Color(0xFF1A1A12)
/** A digit's key, and the quieter ⌫, Clear and ⌄. */
private val MB_KEY = Color(0xFF2C2F28)
private val MB_KEY_QUIET = Color(0xFF23261F)
