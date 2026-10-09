package com.malletcrafts.sitephotos

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import com.malletcrafts.sitephotos.pano.Disto
import java.util.ArrayDeque
import java.util.UUID

/**
 * The Leica DISTO D2 over Bluetooth LE.
 *
 * The interaction is ImageMeter's, because it is the one that works with a
 * laser in one hand: SELECT a measure, then press the button on the meter,
 * and the number lands on the selection. No dialog, nothing to confirm.
 *
 * Built from the published clients listed in [Disto]. The ways this fails
 * silently if done wrong:
 *  - the distance characteristic INDICATES (CaveSurvey, d2relay's GATT
 *    dump), so the CCCD takes ENABLE_INDICATION_VALUE; subscribe with the
 *    notify value and no reading ever arrives.
 *  - no bonding: every published client connects to the D2 without pairing.
 *  - GATT operations go one at a time — Android drops a descriptor write
 *    issued while another operation is outstanding.
 *  - the unit only indicates when it CHANGES, so it is also read once at
 *    subscribe time.
 *
 * [onEvent] reports every raw step (scan hits, bytes received, commands
 * sent) for the Laser test screen, which exists to settle on the real D2 what
 * the published sources disagree about.
 */
@SuppressLint("MissingPermission")
class DistoClient(private val context: Context) {

    enum class State { OFF, SCANNING, CONNECTING, READY }

    var state: State = State.OFF
        private set

    /** Set by the screen that wants readings. */
    var onState: (State, String?) -> Unit = { _, _ -> }
    var onReading: (Disto.Reading) -> Unit = {}
    var onRefused: (String) -> Unit = {}
    var onEvent: (String) -> Unit = {}

    private val main = Handler(Looper.getMainLooper())
    private val pairing = Disto.Pairing()
    private var gatt: BluetoothGatt? = null
    private var scanning = false
    private var retries = 0
    private var scanToken = 0

    private val ops = ArrayDeque<(BluetoothGatt) -> Unit>()
    private var busy = false

    private fun uuid(s: String) = UUID.fromString(s)

    private fun adapter(): BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private fun moveTo(s: State, note: String? = null) {
        state = s
        main.post { onState(s, note) }
        event("state ${s.name}${note?.let { " — $it" } ?: ""}")
    }

    private fun event(line: String) = main.post { onEvent(line) }

    // ---- GATT queue: one operation in flight, next from its callback ----
    private fun enqueue(op: (BluetoothGatt) -> Unit) {
        ops.add(op)
        pump()
    }

    private fun pump() {
        if (busy) return
        val g = gatt ?: return
        val op = ops.poll() ?: return
        busy = true
        // Every GATT call can throw SecurityException if the person granted
        // SCAN but not CONNECT — they are separate permissions on Android
        // 12+, and these run on a binder callback thread where an escape
        // takes the whole app down. A dead laser is a nuisance; a crash in
        // the middle of measuring a room loses the visit.
        runCatching { op(g) }.onFailure {
            busy = false
            moveTo(State.OFF, "Bluetooth refused: ${it.message}")
        }
    }

    private fun done() {
        busy = false
        pump()
    }

    // ---- lifecycle ------------------------------------------------------

    fun start() {
        val ad = adapter()
        if (ad == null || !ad.isEnabled) {
            moveTo(State.OFF, "Turn Bluetooth on")
            return
        }
        val scanner = ad.bluetoothLeScanner
        if (scanner == null) {
            moveTo(State.OFF, "No BLE scanner")
            return
        }
        // No ScanFilter: it is not established that every DISTO advertises
        // the service UUID, and a filter that misses looks exactly like a
        // flat battery. Each hit is checked below for the service OR the
        // "DISTO" name prefix, and what was seen is reported either way.
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanning = true
        val token = ++scanToken
        moveTo(State.SCANNING)
        runCatching { scanner.startScan(null, settings, scanCallback) }
            .onFailure { moveTo(State.OFF, "Scan refused: ${it.message}") }
        // Android throttles scanning; give up after 20 s rather than draining
        // the phone while the meter is asleep.
        main.postDelayed({
            if (scanning && token == scanToken) { stopScan(); moveTo(State.OFF, "No DISTO found — press its Bluetooth button and try again") }
        }, 20_000)
    }

    fun stop() {
        stopScan()
        gatt?.let { runCatching { it.disconnect() }; runCatching { it.close() } }
        gatt = null
        ops.clear()
        busy = false
        moveTo(State.OFF)
    }

    private fun stopScan() {
        if (!scanning) return
        scanning = false
        runCatching { adapter()?.bluetoothLeScanner?.stopScan(scanCallback) }
    }

    /** Ask the meter to fire — the app-side equivalent of its own button. */
    fun measure() = write(Disto.CMD_MEASURE)

    fun laser(on: Boolean) = write(
        if (on) Disto.CMD_LASER_ON else Disto.CMD_LASER_OFF)

    private fun write(cmd: String) {
        val g = gatt ?: return
        val ch = g.getService(uuid(Disto.SERVICE))
            ?.getCharacteristic(uuid(Disto.CH_COMMAND))
        if (ch == null) { event("no command characteristic — cannot send '$cmd'"); return }
        enqueue {
            @Suppress("DEPRECATION")
            ch.value = cmd.toByteArray(Charsets.US_ASCII)
            ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            val ok = it.writeCharacteristic(ch)
            event("sent '$cmd' (${if (ok) "accepted" else "refused by Android"})")
            // Android still calls onCharacteristicWrite for a write-without-
            // response; only a refused write leaves the queue waiting.
            if (!ok) done()
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val dev = result.device ?: return
            val name = runCatching { result.scanRecord?.deviceName ?: dev.name }.getOrNull()
            val advertised = result.scanRecord?.serviceUuids
                ?.contains(ParcelUuid(uuid(Disto.SERVICE))) == true
            val byName = name?.startsWith(Disto.NAME_PREFIX) == true
            if (!advertised && !byName) return
            event("found '${name ?: "?"}' rssi ${result.rssi} — " +
                if (advertised) "advertises the DISTO service" else "matched by name only")
            stopScan()
            connect(dev)
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
            moveTo(State.OFF, "Scan failed ($errorCode)")
        }
    }

    private fun connect(device: BluetoothDevice) {
        moveTo(State.CONNECTING, runCatching { device.name }.getOrNull() ?: "DISTO")
        gatt = runCatching {
            device.connectGatt(context, false, gattCallback,
                BluetoothDevice.TRANSPORT_LE)
        }.getOrElse {
            // BLUETOOTH_CONNECT can be refused even when BLUETOOTH_SCAN was
            // granted, and this is the first call that needs it.
            moveTo(State.OFF, "Bluetooth connect permission refused")
            null
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                retries = 0
                // A short pause before discovery: discovering the instant the
                // link comes up is a common cause of an empty service list.
                main.postDelayed({ runCatching { g.discoverServices() } }, 600)
                return
            }
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runCatching { g.close() }
                gatt = null
                ops.clear(); busy = false
                // 133 is Android's notorious transient connect failure; the
                // remedy is close-and-retry. A DISTO also switches itself off
                // after a while, so a clean disconnect is normal, not an error.
                if (status != BluetoothGatt.GATT_SUCCESS && retries < 3) {
                    retries++
                    main.postDelayed({ start() }, 800L * retries)
                    moveTo(State.SCANNING, "Reconnecting ($status)…")
                } else {
                    moveTo(State.OFF, if (status == BluetoothGatt.GATT_SUCCESS)
                        "Meter disconnected" else "Disconnected ($status)")
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val svc = g.getService(uuid(Disto.SERVICE))
            if (svc == null) {
                moveTo(State.OFF, "Not a DISTO (no DISTO service)")
                return
            }
            event("characteristics: " + svc.characteristics.joinToString(", ") {
                it.uuid.toString().substring(0, 8) + "/" + props(it.properties)
            })
            val dist = svc.getCharacteristic(uuid(Disto.CH_DISTANCE))
            val unit = svc.getCharacteristic(uuid(Disto.CH_DISTANCE_UNIT))
            if (dist == null || unit == null) {
                moveTo(State.OFF, "Meter is missing the distance characteristic")
                return
            }
            subscribe(dist)
            subscribe(unit)
            // The unit only indicates when it CHANGES, so read it once now
            // rather than holding a default that may be wrong.
            enqueue { @Suppress("DEPRECATION") it.readCharacteristic(unit) }
            moveTo(State.READY)
        }

        private fun subscribe(ch: BluetoothGattCharacteristic) {
            enqueue { g ->
                g.setCharacteristicNotification(ch, true)
                val cccd = ch.getDescriptor(uuid(Disto.CCCD))
                if (cccd == null) { done(); return@enqueue }
                // Indication unless the characteristic says it notifies.
                val value = if (ch.properties and
                    BluetoothGattCharacteristic.PROPERTY_INDICATE != 0)
                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                cccd.value = value
                @Suppress("DEPRECATION")
                g.writeDescriptor(cccd)
            }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, s: Int) {
            event("subscribed ${d.characteristic.uuid.toString().substring(0, 8)} (status $s)")
            done()
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, s: Int) = done()

        override fun onCharacteristicRead(
            g: BluetoothGatt, c: BluetoothGattCharacteristic, s: Int,
        ) {
            @Suppress("DEPRECATION")
            handle(c, c.value)
            done()
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            g: BluetoothGatt, c: BluetoothGattCharacteristic,
        ) = handle(c, c.value)

        override fun onCharacteristicChanged(
            g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray,
        ) = handle(c, value)
    }

    private fun props(p: Int): String = buildString {
        if (p and BluetoothGattCharacteristic.PROPERTY_READ != 0) append('r')
        if (p and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) append('w')
        if (p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) append('W')
        if (p and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) append('n')
        if (p and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) append('i')
    }

    private fun handle(c: BluetoothGattCharacteristic, value: ByteArray?) {
        val bytes = value ?: return
        val outcome = when (c.uuid) {
            uuid(Disto.CH_DISTANCE) -> {
                val m = Disto.readFloat32Le(bytes)
                event("distance [${Disto.hex(bytes)}] = %.4f m".format(m))
                pairing.onDistance(m)
            }
            uuid(Disto.CH_DISTANCE_UNIT) -> {
                val u = Disto.readUint16Le(bytes)
                event("unit [${Disto.hex(bytes)}] = code $u")
                pairing.onUnit(u)
            }
            else -> {
                event("${c.uuid.toString().substring(0, 8)} [${Disto.hex(bytes)}]")
                null
            }
        } ?: return
        outcome.reading?.let { r -> main.post { onReading(r) } }
        outcome.refused?.let { why -> event("refused: $why"); main.post { onRefused(why) } }
    }
}
