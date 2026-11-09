package com.malletcrafts.interiorestimate

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
import com.malletcrafts.interiorestimate.pano.i18n.Msg
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
    val tx = LocalTx.current
    val disto = remember { DistoClient(context) }
    var state by remember { mutableStateOf(DistoClient.State.OFF) }
    var note by remember { mutableStateOf<Msg?>(null) }
    var lastMm by remember { mutableStateOf<Int?>(null) }
    var laserOn by remember { mutableStateOf(false) }
    val log = remember { mutableStateListOf<String>() }
    val clock = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    fun add(line: String) {
        // Also to logcat, so the whole log can be read over USB (adb logcat -s McftLaser)
        // without anyone copying it off the screen -- Amit, 2026-10-09: "get log yourself".
        android.util.Log.i("McftLaser", line)
        log.add("${clock.format(Date())}  $line") // text:value the laser log's clock join (decision 4)
        if (log.size > 400) log.removeAt(0)
    }

    DisposableEffect(Unit) { onDispose { disto.stop() } }
    // the note state is a Msg, drawn in the language on when it is drawn (port 61c slice M8)
    disto.onState = { s, n -> state = s; note = n }
    disto.onEvent = { add(it) }
    disto.onReading = { r -> lastMm = r.mm; add("READING ${r.mm} mm (unit code ${r.unitCode})") } // text:value the laser log (decision 4)
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
        else { note = { tx -> tx.sp_msg_bluetooth_refused }; add("permission refused") } // text:value the laser log (decision 4)
    }

    val listState = rememberLazyListState()
    LaunchedEffect(log.size) { if (log.isNotEmpty()) listState.animateScrollToItem(log.size - 1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tx.app_laser_test_disto_d2) },
                navigationIcon = { TextButton(onClick = onBack) { Text(tx.app_aria_back) } },
                actions = {
                    TextButton(enabled = log.isNotEmpty(), onClick = {
                        val text = "MCFT Interior Estimate ${appVersion(context)} — laser test\n" + log.joinToString("\n") // text:value the share payload's header, the log whole (decision 4)
                        context.startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, text), tx.app_share_laser_log))
                    }) { Text(tx.common_share) }
                })
        },
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(lastMm?.let { "$it mm" } ?: "— mm", // text:value the readout: a figure and the unit
                fontSize = 44.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(when (state) {
                DistoClient.State.OFF -> tx.app_not_connected
                DistoClient.State.SCANNING -> tx.app_looking_meter
                DistoClient.State.CONNECTING -> tx.app_connecting
                DistoClient.State.READY -> tx.app_connected_press_measure_button
            } + (note?.let { "\n" + it(tx) } ?: ""), style = MaterialTheme.typography.bodyMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state == DistoClient.State.OFF)
                    Button(onClick = { add("connect pressed"); askBle.launch(blePerms) }) { Text(tx.app_connect) } // text:value the laser log (decision 4)
                else
                    OutlinedButton(onClick = { disto.stop() }) { Text(tx.app_disconnect) }
                // The remote commands are the least certain part of the protocol
                // (one public source), which is exactly why they are buttons here.
                Button(enabled = state == DistoClient.State.READY, onClick = { disto.measure() }) { Text(tx.app_fire) }
                OutlinedButton(enabled = state == DistoClient.State.READY, onClick = {
                    laserOn = !laserOn; disto.laser(laserOn)
                }) { Text(if (laserOn) tx.app_laser_off else tx.app_laser_on) }
            }
            Text(tx.app_checks_1_press_measure, style = MaterialTheme.typography.bodySmall)
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
