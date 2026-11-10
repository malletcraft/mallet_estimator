package com.malletcrafts.interiorestimate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malletcrafts.interiorestimate.pano.TourFlow
import com.malletcrafts.interiorestimate.pano.TourFlows

/**
 * THE HELP SCREEN (port 61c §31) — the prototype's helpScreen, built like the Laser test
 * screen: its own top bar with ←, the title and "Guided tours · on the demo flat" under it,
 * the intro paragraph, then one row per SHIPPED flow — its name, its one-line description and
 * Start. ◁ and ← close it. There is no first-run overlay, no auto-start and no seen-once
 * prefs flag anywhere in the prototype: this row in the ≡ drawer is the tour's only entry,
 * and every tour re-runs as often as wanted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit, onStart: (TourFlow) -> Unit) {
    val tx = LocalTx.current
    val flows = remember(tx) { TourFlows.shipped(tx) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tx.common_help)
                        Text(tx.help_subtitle, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { TextButton(onClick = onBack) { Text(tx.app_aria_back) } })
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize()) {
            item {
                Text(tx.help_each_tour_opens_demo,
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(flows.size) { i ->
                val f = flows[i]
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(f.name, style = MaterialTheme.typography.bodyLarge)
                            Text(f.line, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(2.dp))
                        Button(onClick = { onStart(f) }) { Text(tx.common_start) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
