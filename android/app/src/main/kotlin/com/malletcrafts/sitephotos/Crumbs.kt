package com.malletcrafts.sitephotos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * The breadcrumb rail.
 *
 * Client → Site → Project → Room is four levels, and every study of mobile
 * navigation says three is where people stop navigating and start guessing.
 * The rail is what buys the fourth back:
 *
 *  - ONE line, scrolled sideways, never wrapped. Wrapping breadcrumbs onto a
 *    second row is the single most-cited mobile breadcrumb mistake, and on a
 *    360dp screen it costs a whole row of content.
 *  - Past four crumbs the middle collapses to "…", which OPENS rather than
 *    truncating: the hidden levels are still reachable in one tap.
 *  - The current crumb is not a dead label the way it is on the web. It
 *    carries a chevron and opens the sibling switcher, so MB → KIT is one
 *    tap instead of four (up, up, down, down).
 *  - Every crumb is a 36dp pill inside a 48dp rail, so the touch target
 *    clears 44dp even where the text is two characters.
 */

data class Crumb(
    val label: String,
    /** Siblings of THIS level, for the switcher. Empty disables it. */
    val siblings: List<Pair<String, String>> = emptyList(),  // key to label
    val onUp: (() -> Unit)? = null,
    val onSibling: ((String) -> Unit)? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrumbRail(crumbs: List<Crumb>, modifier: Modifier = Modifier) {
    if (crumbs.isEmpty()) return
    var sheetFor by remember { mutableStateOf<Int?>(null) }
    var showHidden by remember { mutableStateOf(false) }

    // Four is the point at which the rail stops fitting a 360dp screen with
    // real names in it, so that is where the middle folds away.
    val collapse = crumbs.size > 4
    val hidden = if (collapse) crumbs.subList(1, crumbs.size - 2) else emptyList()
    val shown = if (collapse)
        listOf(0) + (crumbs.size - 2 until crumbs.size).toList()
    else crumbs.indices.toList()

    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        shown.forEachIndexed { pos, idx ->
            if (pos > 0) Separator()
            if (collapse && pos == 1 && hidden.isNotEmpty()) {
                CrumbPill("…", current = false) { showHidden = true }
                Separator()
            }
            val c = crumbs[idx]
            val isLast = idx == crumbs.size - 1
            CrumbPill(c.label, current = isLast) {
                if (isLast) {
                    if (c.siblings.size > 1) sheetFor = idx
                } else c.onUp?.invoke()
            }
        }
    }

    sheetFor?.let { idx ->
        val c = crumbs[idx]
        ModalBottomSheet(onDismissRequest = { sheetFor = null }) {
            SheetTitle("Switch")
            c.siblings.forEach { (key, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    colors = if (label == c.label)
                        ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    else ListItemDefaults.colors(),
                    modifier = Modifier.clickableRow {
                        sheetFor = null
                        c.onSibling?.invoke(key)
                    })
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showHidden && hidden.isNotEmpty()) {
        ModalBottomSheet(onDismissRequest = { showHidden = false }) {
            SheetTitle("Jump up to")
            hidden.forEach { c ->
                ListItem(
                    headlineContent = { Text(c.label) },
                    modifier = Modifier.clickableRow {
                        showHidden = false
                        c.onUp?.invoke()
                    })
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Separator() {
    Text("/", style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(horizontal = 2.dp))
}

@Composable
private fun CrumbPill(label: String, current: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (current) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        contentColor = if (current) MaterialTheme.colorScheme.onSecondaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.height(36.dp),
    ) {
        Row(Modifier.padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 180.dp))
            if (current) {
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Switch",
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun SheetTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp))
}

/** ListItem has no onClick of its own; this keeps the call sites readable. */
fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.fillMaxWidth().clickable(onClick = onClick)
