package com.malletcrafts.interiorestimate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.malletcrafts.interiorestimate.pano.AlbumPage

/**
 * THE PROGRESS SHOT'S CAMERA (port 61c slice K5, 9 Nov 2026; DESIGN-port61c.md §20, "THE CONTROLS"): the prototype
 * v61c.2's camera (index61c2.html camHTML 7499-7510, its CSS 1504-1505, 1509, 1523-1542) drawn from
 * [AlbumPage.Camera], which holds every text and rule -- this file has none of its own. Over the whole album, as the
 * viewer is: #000, white; the viewfinder (the height the controls leave, at least 120 dp) over the controls column
 * (6/12/10 dp, 6 dp between): WORK, ROOM, the caption, and the bottom bar (84 dp: Cancel, the shutter, a 56 dp spacer).
 * In landscape the viewfinder is the left and the controls a 300 dp column on the right, its content at the bottom.
 *
 * THE VIEWFINDER IS BLACK WITH NOTHING IN IT: the shutter hands the shot to the phone's own camera app (the route the
 * Capture 360 sheet's Camera takes), so the phone has no preview here -- where the prototype paints a simulated scene
 * under its `phone camera · simulated` tag. A question for the prototype (Q1), not a state drawn here.
 *
 * The WORK row is read-only while the album's lock is up (`aria-disabled`: at .85, its second text on a line of its
 * own under the value), else a tap; ROOM is always a tap; the shutter takes a tap only with a filing to shoot into --
 * with `Pick the work` the same circle takes none (the prototype's `disabled` has no style of its own for
 * `.alb59shut`), and TalkBack says it is disabled. It takes every tap that is not its own, so nothing under it moves.
 */

private val CAM_INK = Color(0xFFEDEFEA)
private val CAM_MUTE = Color(0xFF9AA29E)
private val CAM_ROW = Color(0xFF1B1F1E)
private val CAM_OFF = Color(0xFF5E6663)
private val CAM_LIME = Color(0xFFB9F227)

@Composable
fun AlbumCamera(
    model: AlbumPage.Camera,
    landscape: Boolean,
    onWork: () -> Unit,
    onRoom: () -> Unit,
    onCaption: (String) -> Unit,
    onCancel: () -> Unit,
    onShutter: () -> Unit,
) {
    val root = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .pointerInput(Unit) { detectTapGestures { } }
        .semantics { paneTitle = model.pane }
        .imePadding()
    if (landscape) {
        Row(root) {
            Box(Modifier.weight(1f).fillMaxHeight())
            CameraControls(model, Modifier.width(300.dp).fillMaxHeight(), Arrangement.spacedBy(6.dp, Alignment.Bottom),
                onWork, onRoom, onCaption, onCancel, onShutter)
        }
    } else {
        Column(root) {
            Box(Modifier.weight(1f).heightIn(min = 120.dp).fillMaxWidth())
            CameraControls(model, Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp), onWork, onRoom, onCaption, onCancel, onShutter)
        }
    }
}

/** `.alb59cc`: the three rows and the bottom bar, 6/12/10 dp, 6 dp between. */
@Composable
private fun CameraControls(
    model: AlbumPage.Camera, modifier: Modifier, arrangement: Arrangement.Vertical,
    onWork: () -> Unit, onRoom: () -> Unit, onCaption: (String) -> Unit, onCancel: () -> Unit, onShutter: () -> Unit,
) {
    Column(modifier.padding(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 10.dp), verticalArrangement = arrangement) {
        CameraRow(model.work, onWork)
        CameraRow(model.room, onRoom)
        // `.alb59capf`: the field on #1B1F1E, 1 dp #5E6663 (2 dp #B9F227 focused), text #EDEFEA, label #9AA29E (#B9F227 focused)
        OutlinedTextField(
            value = model.caption,
            onValueChange = onCaption,
            label = { Text(model.captionLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = CAM_INK, unfocusedTextColor = CAM_INK,
                focusedContainerColor = CAM_ROW, unfocusedContainerColor = CAM_ROW,
                focusedBorderColor = CAM_LIME, unfocusedBorderColor = CAM_OFF,
                focusedLabelColor = CAM_LIME, unfocusedLabelColor = CAM_MUTE,
                cursorColor = CAM_LIME,
            ),
        )
        // `.alb59cb2`: 84 dp, Cancel, the shutter and a 56 dp spacer spaced between
        Row(Modifier.fillMaxWidth().height(84.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onCancel, modifier = Modifier.widthIn(min = 56.dp), colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                Text(model.cancel)
            }
            Shutter(model.shutter, model.shutterAria, onShutter)
            Spacer(Modifier.width(56.dp))
        }
    }
}

/**
 * One row (`.alb59row`): at least 44 dp, 12 dp sides, 10 dp round, #1B1F1E, #EDEFEA; the label labelSmall in 52 dp,
 * #9AA29E; 8 dp; the value bodyMedium, one line; its second text bodySmall #9AA29E after it -- or, read-only
 * (`aria-disabled`), the row at .85 with 6 dp above and below and the second text on its own line under the value.
 */
@Composable
private fun CameraRow(row: AlbumPage.CamRow, onTap: () -> Unit) {
    val ty = MaterialTheme.typography
    val shape = RoundedCornerShape(10.dp)
    val m = Modifier
        .fillMaxWidth()
        .heightIn(min = 44.dp)
        .clip(shape)
        .background(CAM_ROW)
        .then(if (row.tap) Modifier.clickable(role = Role.Button, onClick = onTap) else Modifier)
        .then(if (row.dim) Modifier.alpha(.85f) else Modifier)
        .padding(horizontal = 12.dp, vertical = if (row.dim) 6.dp else 0.dp)
    if (row.dim) {
        Column(m, verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(row.label, style = ty.labelSmall, color = CAM_MUTE, modifier = Modifier.width(52.dp))
                Text(row.value, style = ty.bodyMedium, color = CAM_INK, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
            row.sub?.let { Text(it, style = ty.bodySmall, color = CAM_MUTE, modifier = Modifier.padding(start = 60.dp)) }
        }
    } else {
        Row(m, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(row.label, style = ty.labelSmall, color = CAM_MUTE, modifier = Modifier.width(52.dp))
            Text(row.value, style = ty.bodyMedium, color = CAM_INK, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            row.sub?.let { Text(it, style = ty.bodySmall, color = CAM_MUTE, maxLines = 1) }
        }
    }
}

/** `.alb59shut`: a 68 dp white circle inside a 4 dp black ring and a 2 dp white one, 80 dp across; a tap only when [enabled]. */
@Composable
private fun Shutter(enabled: Boolean, aria: String, onShutter: () -> Unit) {
    Box(
        Modifier
            .size(80.dp)
            .drawBehind {
                drawCircle(Color.White, radius = 40.dp.toPx())
                drawCircle(Color.Black, radius = 38.dp.toPx())
                drawCircle(Color.White, radius = 34.dp.toPx())
            }
            .clip(CircleShape)
            .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onShutter) else Modifier)
            .clearAndSetSemantics {
                contentDescription = aria
                role = Role.Button
                if (enabled) onClick { onShutter(); true } else disabled()
            },
    )
}
