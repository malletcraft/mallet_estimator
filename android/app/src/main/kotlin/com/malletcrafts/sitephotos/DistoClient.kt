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
import android.bluetooth.le.ScanFilter
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
 * Four things here are not guesses — they come from Leica's own SDK and
 * they are each a way this silently fails if you get them wrong:
 *  - the distance characteristic INDICATES, it does not notify, so the
 *    CCCD takes ENABLE_INDICATION_VALUE. Subscribe with the notify value
 *    and no reading ever arrives.
 *  - the D2 must NOT be bonded. Leica skips pairing when the DISTO service
 *    is advertised, and pairing a D2 breaks it.
 *  - GATT operations must be serialized — one outstanding at a time — or
 *    the descriptor writes are dropped.
 *  - readings arrive as a PAIR (distance, then unit), and the unit only
 *    indicates when it changes, so it is also read once at subscribe time.
 */
@SuppressLint("MissingPermission")
class DistoClient(private val context: Context) {

    enum class State { OFF, SCANNING, CONNECTING, READY }

    var state: State = State.OFF
        private set

    /** Set by the screen that wants readings. */
    var onState: (State, String?) -> Unit = { _, _ -> }
    var onReading: (Disto.Reading) -> Unit = {}

    private val main = Handler(Looper.getMainLooper())
    private val pairing = Disto.Pairing()
    private var gatt: BluetoothGatt? = null
    private var scanning = false
    private var retries = 0

    private val ops = ArrayDeque<(BluetoothGatt) -> Unit>()
    private var busy = false

    private fun uuid(s: String) = UUID.fromString(s)

    private fun adapter(): BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private fun moveTo(s: State, note: String? = null) {
        state = s
        main.post { onState(s, note) }
    }

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
        op(g)
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
        // Filter on the advertised service UUID. Filtering on the full name
        // would fail: the D2 advertises a SHORT name and the long
        // "DISTO D2 <serial>" other apps display is assembled client-side.
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(uuid(Disto.SERVICE))).build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanning = true
        moveTo(State.SCANNING)
        runCatching { scanner.startScan(listOf(filter), settings, scanCallback) }
            .onFailure { moveTo(State.OFF, "Scan refused: ${it.message}") }
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
            ?.getCharacteristic(uuid(Disto.CH_COMMAND)) ?: return
        enqueue {
            // Bare ASCII, no terminator, write-without-response.
            @Suppress("DEPRECATION")
            ch.value = cmd.toByteArray(Charsets.US_ASCII)
            ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            it.writeCharacteristic(ch)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val dev = result.device ?: return
            stopScan()
            connect(dev)
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
            moveTo(State.OFF, "Scan failed ($errorCode)")
        }
    }

    private fun connect(device: BluetoothDevice) {
        moveTo(State.CONNECTING, device.name ?: "DISTO")
        // No createBond(): the D2 is one of the models Leica connects to
        // WITHOUT pairing, and bonding it breaks the link.
        gatt = device.connectGatt(context, false, gattCallback,
            BluetoothDevice.TRANSPORT_LE)
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                retries = 0
                // Leica delays discovery after connecting; going straight in
                // is a known source of empty service lists.
                main.postDelayed({ runCatching { g.discoverServices() } }, 600)
                return
            }
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runCatching { g.close() }
                gatt = null
                ops.clear(); busy = false
                // 133 is the notorious transient connect failure; Leica's own
                // guidance is simply to retry. A DISTO also powers itself off
                // after a few minutes, so disconnects are normal, not errors.
                if (status != BluetoothGatt.GATT_SUCCESS && retries < 3) {
                    retries++
                    main.postDelayed({ start() }, 800L * retries)
                    moveTo(State.SCANNING, "Reconnecting…")
                } else {
                    moveTo(State.OFF, if (status == BluetoothGatt.GATT_SUCCESS)
                        "Meter disconnected" else "Disconnected ($status)")
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val svc = g.getService(uuid(Disto.SERVICE))
            if (svc == null) {
                moveTo(State.OFF, "Not a DISTO")
                return
            }
            val dist = svc.getCharacteristic(uuid(Disto.CH_DISTANCE))
            val unit = svc.getCharacteristic(uuid(Disto.CH_DISTANCE_UNIT))
            if (dist == null || unit == null) {
                moveTo(State.OFF, "Meter is missing the distance service")
                return
            }
            subscribe(dist)
            subscribe(unit)
            // The unit characteristic only indicates when it CHANGES, so read
            // it once now rather than holding a default that may be wrong.
            enqueue { @Suppress("DEPRECATION") it.readCharacteristic(unit) }
            moveTo(State.READY)
        }

        private fun subscribe(ch: BluetoothGattCharacteristic) {
            enqueue { g ->
                g.setCharacteristicNotification(ch, true)
                val cccd = ch.getDescriptor(uuid(Disto.CCCD))
                if (cccd == null) { done(); return@enqueue }
                // Indication, not notification — branch on what the
                // characteristic actually declares, the way Leica does.
                val value = if (ch.properties and
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0)
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                else BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                @Suppress("DEPRECATION")
                cccd.value = value
                @Suppress("DEPRECATION")
                g.writeDescriptor(cccd)
            }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, s: Int) = done()

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

    private fun handle(c: BluetoothGattCharacteristic, value: ByteArray?) {
        val bytes = value ?: return
        val reading = when (c.uuid) {
            uuid(Disto.CH_DISTANCE) ->
                pairing.onDistance(Disto.readFloat32Le(bytes))
            uuid(Disto.CH_DISTANCE_UNIT) ->
                pairing.onUnit(Disto.readUint16Le(bytes))
            else -> null
        } ?: return
        main.post { onReading(reading) }
    }
}
