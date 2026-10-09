package com.malletcrafts.sitephotos

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Laser test — proves the DISTO link on the real D2 before anything relies on it.
 *
 * Amit, 2026-10-09, chose this over checking by hand with nRF Connect: one
 * session with the meter answers what the published sources disagree about —
 * the unit codes the D2 actually sends, whether the app can fire it ('g'),
 * whether it advertises the DISTO service or is found by name, and how soon
 * it sleeps. So the screen shows the raw bytes as well as the millimetres,
 * and Share hands the whole log over as text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaserTestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val disto = remember { DistoClient(context) }
    var state by remember { mutableStateOf(DistoClient.State.OFF) }
    var note by remember { mutableStateOf<String?>(null) }
    var lastMm by remember { mutableStateOf<Int?>(null) }
    var laserOn by remember { mutableStateOf(false) }
    val log = remember { mutableStateListOf<String>() }
    val clock = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    fun add(line: String) {
        log.add("${clock.format(Date())}  $line")
        if (log.size > 400) log.removeAt(0)
    }

    DisposableEffect(Unit) { onDispose { disto.stop() } }
    disto.onState = { s, n -> state = s; note = n }
    disto.onEvent = { add(it) }
    disto.onReading = { r -> lastMm = r.mm; add("READING ${r.mm} mm (unit code ${r.unitCode})") }
    disto.onRefused = { why -> note = why }

    // Android 12+ gates a BLE scan behind runtime permission; without it the
    // scan fails silently, which would look exactly like a flat meter.
    val blePerms = remember {
        if (android.os.Build.VERSION.SDK_INT >= 31)
            arrayOf(android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val askBle = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) disto.start()
        else { note = "Bluetooth permission refused — the laser can't connect"; add("permission refused") }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(log.size) { if (log.isNotEmpty()) listState.animateScrollToItem(log.size - 1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laser test · DISTO D2") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    TextButton(enabled = log.isNotEmpty(), onClick = {
                        val text = "Site Photos ${appVersion(context)} — laser test\n" + log.joinToString("\n")
                        context.startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, text), "Share laser log"))
                    }) { Text("Share") }
                })
        },
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(lastMm?.let { "$it mm" } ?: "— mm",
                fontSize = 44.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(when (state) {
                DistoClient.State.OFF -> "Not connected"
                DistoClient.State.SCANNING -> "Looking for the meter…"
                DistoClient.State.CONNECTING -> "Connecting…"
                DistoClient.State.READY -> "Connected — press the measure button on the D2"
            } + (note?.let { "\n$it" } ?: ""), style = MaterialTheme.typography.bodyMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state == DistoClient.State.OFF)
                    Button(onClick = { add("connect pressed"); askBle.launch(blePerms) }) { Text("Connect") }
                else
                    OutlinedButton(onClick = { disto.stop() }) { Text("Disconnect") }
                // The remote commands are the least certain part of the protocol
                // (one public source), which is exactly why they are buttons here.
                Button(enabled = state == DistoClient.State.READY, onClick = { disto.measure() }) { Text("Fire") }
                OutlinedButton(enabled = state == DistoClient.State.READY, onClick = {
                    laserOn = !laserOn; disto.laser(laserOn)
                }) { Text(if (laserOn) "Laser off" else "Laser on") }
            }
            Text("Checks: 1) press measure on the D2 · 2) tap Fire · 3) set the D2 to mm, m, ft, in and measure once in each · " +
                "4) leave it idle and note when it disconnects. Then Share the log.",
                style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(log) { line ->
                    Text(line, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}
