@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.malletcrafts.sitephotos

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malletcrafts.sitephotos.pano.CaptureGeometry
import com.malletcrafts.sitephotos.pano.Handover
import com.malletcrafts.sitephotos.pano.Panorama
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SyncWorker.schedule(this)
        setContent { MaterialTheme { AppScreen() } }
    }
}

private data class ProjectRow(val name: String, val title: String, val customer: String)

private fun projects(masters: JSONObject?): List<ProjectRow> {
    val arr = masters?.optJSONArray("projects") ?: return emptyList()
    return (0 until arr.length()).map { i ->
        val p = arr.getJSONObject(i)
        ProjectRow(p.getString("project"), p.optString("title"),
            p.optString("customer_name"))
    }
}

private fun strings(masters: JSONObject?, key: String): List<String> {
    val arr = masters?.optJSONArray(key) ?: return emptyList()
    return (0 until arr.length()).map { arr.getString(it) }
}

@Composable
private fun AppScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { CaptureStore(context) }
    val scope = rememberCoroutineScope()

    var masters by remember { mutableStateOf(store.masters()) }
    var configured by remember { mutableStateOf(FrappeClient.configured(context)) }
    var showSettings by remember { mutableStateOf(!FrappeClient.configured(context)) }

    var project by remember { mutableStateOf<ProjectRow?>(null) }
    // A site with no project row yet: the typed (client, project) pair. The
    // capture files against the words; sync turns them into masters.
    var newSite by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showNewSite by remember { mutableStateOf(false) }
    var room by remember { mutableStateOf<String?>(null) }
    var stage by remember { mutableStateOf("") }
    // Which FOV the split uses. Index into CaptureGeometry.PRESETS, with one
    // extra "server default" entry at the end. Remembered across launches —
    // a photographer doing bathrooms all morning picks Small once.
    val capturePrefs = remember {
        context.getSharedPreferences("capture", android.content.Context.MODE_PRIVATE)
    }
    var roomSize by remember {
        mutableStateOf(capturePrefs.getInt("room_size", 1)
            .coerceIn(0, CaptureGeometry.PRESETS.size))
    }

    var busy by remember { mutableStateOf<String?>(null) }
    var lastResult by remember { mutableStateOf<String?>(null) }
    var queue by remember { mutableStateOf(store.all()) }

    fun refreshQueue() { queue = store.all() }

    // First composition with credentials: pull fresh masters in the
    // background so the pickers are not a week old.
    LaunchedEffect(configured) {
        if (configured) {
            withContext(Dispatchers.IO) {
                runCatching {
                    FrappeClient.load(context)?.bootstrap()?.let {
                        store.saveMasters(it)
                    }
                }
            }
            masters = store.masters()
        }
    }

    val projectRows = projects(masters)
    if (project == null && projectRows.isNotEmpty()) project = projectRows.first()
    val rooms = strings(masters, "rooms")
    if (room == null && rooms.isNotEmpty()) room = rooms.first()
    val stages = strings(masters, "stages")

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val p = newSite?.let { ProjectRow("", it.second, it.first) } ?: project
        val r = room
        if (uri == null || p == null || r == null) return@rememberLauncherForActivityResult
        val stageNow = stage
        busy = "Splitting the 360 into six faces…"
        lastResult = null
        scope.launch(Dispatchers.Default) {
            val outcome = runCatching {
                val id = Handover.mintDeviceId(
                    ByteArray(6).also { SecureRandom().nextBytes(it) })
                val today = LocalDate.now().toString()
                val fov = CaptureGeometry.PRESETS.getOrNull(roomSize)?.fov
                    ?: masters?.optDouble("default_fov", Panorama.DEFAULT_FOV)
                    ?: Panorama.DEFAULT_FOV
                val (result, pano) = FaceWriter.split(
                    context = context, source = uri, deviceId = id,
                    customerName = p.customer, projectTitle = p.title,
                    room = r, captureDate = today, stage = stageNow, fov = fov,
                    panoDir = File(context.filesDir, "panos"))
                store.insert(CaptureStore.Capture(
                    deviceId = id, project = p.name, projectTitle = p.title,
                    customerName = p.customer, room = r, stage = stageNow,
                    captureDate = today, panoPath = pano.path,
                    createdAt = System.currentTimeMillis(), state = "LOCAL",
                    serverName = null, error = null))
                result
            }
            withContext(Dispatchers.Main) {
                busy = null
                lastResult = outcome.fold(
                    onSuccess = {
                        SyncWorker.syncNow(context)
                        "${it.faceCount} faces saved to ${it.relativePath}\n" +
                            "Open ImageMeter → the room folder → add photos."
                    },
                    onFailure = { "Could not split: ${it.message}" })
                refreshQueue()
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("MCFT Site Photos") },
            actions = {
                TextButton(onClick = { showSettings = true }) { Text("Settings") }
            })
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize()) {
            if (!configured) {
                Text("Set the server and API key in Settings to begin.")
                Spacer(Modifier.height(12.dp))
            }
            if (masters == null && configured) {
                Text("Waiting for the first master list — go online once.")
                Spacer(Modifier.height(12.dp))
            }

            Picker("Project",
                projectRows.map { it.title } + "＋ New client / project…",
                newSite?.let { it.second + " (new)" } ?: project?.title ?: "—") { i ->
                if (i < projectRows.size) {
                    project = projectRows[i]; newSite = null
                } else {
                    showNewSite = true
                }
            }
            Spacer(Modifier.height(8.dp))
            Picker("Room", rooms, room ?: "—") { i -> room = rooms[i] }
            Spacer(Modifier.height(8.dp))
            Picker("Stage", listOf("(none)") + stages,
                stage.ifBlank { "(none)" }) { i ->
                stage = if (i == 0) "" else stages[i - 1]
            }
            Spacer(Modifier.height(8.dp))
            // Small rooms need wider faces or the split truncates the walls —
            // the geometry (and the presets) live in CaptureGeometry.
            val sizeOptions = CaptureGeometry.PRESETS.map {
                "${it.label} — ${it.fov.toInt()}°"
            } + "Server default"
            Picker("Room size", sizeOptions,
                sizeOptions[roomSize.coerceIn(0, sizeOptions.size - 1)]) { i ->
                roomSize = i
                capturePrefs.edit().putInt("room_size", i).apply()
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Shoot from the room centre, camera LEVEL at half ceiling " +
                    "height (≈4 ft 9 in under a 9½ ft ceiling) — " +
                    "then every wall keeps all four corners after the split.",
                style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    picker.launch(PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = busy == null && (project != null || newSite != null)
                    && room != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Pick 360 photo") }

            busy?.let {
                Spacer(Modifier.height(12.dp))
                Row {
                    CircularProgressIndicator(Modifier.width(20.dp).height(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(it)
                }
            }
            lastResult?.let {
                Spacer(Modifier.height(12.dp))
                Card { Text(it, Modifier.padding(12.dp)) }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Captures", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = {
                    SyncWorker.syncNow(context)
                    refreshQueue()
                }) { Text("Sync now") }
            }
            LazyColumn {
                items(queue, key = { it.deviceId }) { c ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            Text("${c.projectTitle} — ${c.room}"
                                + (if (c.stage.isNotBlank()) " · ${c.stage}" else ""))
                            Text(
                                when (c.state) {
                                    "SYNCED" -> "Synced as ${c.serverName}"
                                    "ERROR" -> "Waiting to retry: ${c.error}"
                                    "SYNCING" -> "Uploading…"
                                    else -> "On this phone, will upload when online"
                                },
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (showNewSite) {
        NewSiteDialog(
            onDismiss = { showNewSite = false },
            onSave = { client, proj ->
                newSite = client to proj
                showNewSite = false
            })
    }

    if (showSettings) {
        SettingsDialog(
            initialUrl = FrappeClient.savedUrl(context),
            onDismiss = { showSettings = false },
            onSave = { url, key, secret ->
                FrappeClient.save(context, url, key, secret)
                configured = true
                showSettings = false
                SyncWorker.syncNow(context)
            })
    }
}

@Composable
private fun Picker(label: String, options: List<String>, selected: String,
                   onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, Modifier.fillMaxWidth()) {
            Text("$label: $selected")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { i, opt ->
                DropdownMenuItem(text = { Text(opt) },
                    onClick = { onPick(i); open = false })
            }
        }
    }
}

@Composable
private fun SettingsDialog(initialUrl: String, onDismiss: () -> Unit,
                           onSave: (String, String, String) -> Unit) {
    var url by remember { mutableStateOf(initialUrl) }
    var key by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Server") },
        text = {
            Column {
                OutlinedTextField(url, { url = it }, label = { Text("Site URL") },
                    singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(key, { key = it }, label = { Text("API key") },
                    singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(secret, { secret = it },
                    label = { Text("API secret") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Text("Generate both on your User page in ERPNext " +
                    "(Settings → API Access). The secret is shown once.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(url, key, secret) },
                enabled = url.isNotBlank() && key.isNotBlank() && secret.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NewSiteDialog(onDismiss: () -> Unit,
                          onSave: (String, String) -> Unit) {
    var client by remember { mutableStateOf("") }
    var projectName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New site") },
        text = {
            Column {
                OutlinedTextField(client, { client = it },
                    label = { Text("Client name") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(projectName, { projectName = it },
                    label = { Text("Project name") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Text("Works offline. When the phone syncs, these become the " +
                    "real client and project in ERPNext — or match ones that " +
                    "already exist, however the names were spelled.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(client.trim(), projectName.trim()) },
                enabled = client.isNotBlank() && projectName.isNotBlank(),
            ) { Text("Use this site") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
