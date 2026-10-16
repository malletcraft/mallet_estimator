package com.malletcrafts.sitephotos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malletcrafts.sitephotos.pano.SurveyPrep
import kotlinx.coroutines.delay

/**
 * Survey prep's 2 · Mark toolbar: ONE row of icon buttons, scrolling sideways,
 * in place of the three rows of "+ Window" text chips (Amit, 2026-10-11).
 * Each button is the mark type's little picture in its colour with its letter
 * under it; a tap adds a mark exactly as the chips did, a long press names it.
 * The icons are drawn here, so there are no image assets to ship.
 *
 * v52 slice 4 (interim until the palette of slice 7): the Bulb point, the
 * Switch plate and the Fan join the row with their own icons, as TYPES lists
 * them for the surface -- Switch plate on walls only, Fan on walls and the
 * ceiling.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SpMarkToolbar(types: List<SurveyPrep.MarkType>, onAdd: (String) -> Unit) {
    var hint by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(hint) { if (hint != null) { delay(1600); hint = null } }
    Column(Modifier.fillMaxWidth().padding(bottom = 5.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (ty in types) {
                val col = Color(SurveyPrepDraw.argb(ty.colour))
                Column(
                    Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, col, RoundedCornerShape(10.dp))
                        .combinedClickable(onClick = { onAdd(ty.letter) }, onLongClick = { hint = ty.name })
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(24.dp)) { drawMarkIcon(ty.name, col) }
                    Text(ty.letter, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        hint?.let { Text(it, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp, top = 3.dp)) }
    }
}

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
