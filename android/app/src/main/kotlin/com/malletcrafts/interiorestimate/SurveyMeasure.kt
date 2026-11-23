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
import com.malletcrafts.interiorestimate.pano.MeasurePad
import com.malletcrafts.interiorestimate.pano.StepRoute
import com.malletcrafts.interiorestimate.pano.SurveyPrepPalette

/**
 * THE KEYPAD (DESIGN-imagemeter §5; port 63 slice I1b): four rows of StepRoute.padRowDp IN THE COLUMN, in place of the
 * tool row (StepRoute.underMark, Under.KEYPAD), the photo giving up the height and the whole face rescaled into what is
 * left, so nothing is covered and the gold plate is always in view. It has NO DISPLAY LINE: the digits typed appear on
 * the gold plate on the photo (MeasurePad.live). There is no measure bar any more -- nothing under the photo shows a
 * measurement -- and no key of it fires the laser. It opens only on a tap on the gold figure (Pick row 5).
 *
 *      1   2   3   ⌫
 *      4   5   6   Clear
 *      7   8   9   Next ▸
 *      ⌄   [  0  ]   (Next ▸, two rows tall)
 *
 * It holds no text of its own: a digit, ⌫ and Clear are handed to [onKey] (MeasurePad.press decides what they do to
 * the text -- the first digit replaces what was there, six at most, a leading 0 ignored); Next ▸ commits and moves on
 * as a reading that passes does ([onNext]: Landing.keyed, or `▸` when nothing was typed); ⌄ commits and hides it
 * ([onHide]). Digits only, whole mm. Its first row starts SurveyPrepPalette.TOP_DP under the photo, as the tool row
 * does (StepRoute.keypadDp).
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
    Column(modifier.fillMaxWidth()) {
        // the same 6 dp over its first row as the tool row has over its slots (StepRoute.keypadDp)
        Spacer(Modifier.height(SurveyPrepPalette.TOP_DP.dp))
        Row(Modifier.fillMaxWidth().height((StepRoute.PAD_ROWS * StepRoute.padRowDp(land)).dp)) {
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

/** The gold of Next ▸, ink on gold, a digit's key, and the quieter ⌫, Clear and ⌄. */
private val MB_GOLD = Color(0xFFE0B040)
private val MB_INK = Color(0xFF1A1A12)
private val MB_KEY = Color(0xFF2C2F28)
private val MB_KEY_QUIET = Color(0xFF23261F)
