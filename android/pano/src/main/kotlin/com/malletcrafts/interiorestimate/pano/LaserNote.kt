package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Msg

/**
 * THE LASER'S WORDS, rendered live (port 61c slice M6). DistoClient hands its readers one of these
 * [Msg]s instead of an English sentence, so the state said on survey prep draws in the language on
 * when it is said (the M3b readingsMsg rule). Nothing STORES a laser note — it is a transient
 * callback — so the stored-English rule (M4, UploadNote) does not apply; what does is the
 * conflictText/PlanSave lesson: a words-home in :app cannot be pinned where the suite runs, so the
 * words live HERE and LaserNoteTest pins both languages. The {why}/{status}/{code} fills are the
 * system's own words and figures, data, shown as given. The laser LOG stays English (decision 4;
 * the page's own demo log line): DistoClient's event lines render these at `Tx.EN`.
 */
object LaserNote {

    /** `Turn Bluetooth on` — the adapter is off. */
    val turnBluetoothOn: Msg = { tx -> tx.sp_laser_turn_bluetooth_on }

    /** `No BLE scanner` — the adapter has no LE scanner. */
    val noBleScanner: Msg = { tx -> tx.sp_laser_no_ble_scanner }

    /** `Scan refused: {why}` — the system's own message as given. */
    fun scanRefused(why: String): Msg = { tx -> tx.sp_laser_scan_refused_x(why) }

    /** `No DISTO found — press its Bluetooth button and try again` — the 20 s scan ended empty. */
    val noDistoFound: Msg = { tx -> tx.sp_laser_no_disto_found }

    /** `Scan failed ({code})` — Android's scan error code, data. */
    fun scanFailed(code: Int): Msg = { tx -> tx.sp_laser_scan_failed_x(code.toString()) }

    /** `Bluetooth refused: {why}` — a GATT call threw (SCAN granted, CONNECT not). */
    fun bluetoothRefused(why: String): Msg = { tx -> tx.sp_laser_bluetooth_refused_x(why) }

    /** `Bluetooth connect permission refused` — connectGatt threw. */
    val connectPermissionRefused: Msg = { tx -> tx.sp_laser_connect_permission_refused }

    /** `Reconnecting ({status})…` — the 133-style transient, retried. */
    fun reconnecting(status: Int): Msg = { tx -> tx.sp_laser_reconnecting_x(status.toString()) }

    /** `Meter disconnected` — the clean disconnect (a DISTO switches itself off). */
    val meterDisconnected: Msg = { tx -> tx.app_meter_disconnected }

    /** `Disconnected ({status})` — a failed status with no retry left. */
    fun disconnected(status: Int): Msg = { tx -> tx.sp_laser_disconnected_x(status.toString()) }

    /** `Not a DISTO (no DISTO service)`. */
    val notADisto: Msg = { tx -> tx.sp_laser_not_a_disto }

    /** `Meter is missing the distance characteristic`. */
    val noDistanceCharacteristic: Msg = { tx -> tx.sp_laser_no_distance_characteristic }

    /** A refused reading's words, from [Disto.Refusal]'s kind and pre-formatted figure. */
    fun refusal(r: Disto.Refusal): Msg = when (r.kind) {
        Disto.Refusal.Kind.NOT_METRIC -> { tx -> tx.sp_laser_not_metric_x(r.arg) }
        Disto.Refusal.Kind.NOT_A_LENGTH -> { tx -> tx.sp_laser_not_a_length_x(r.arg) }
    }
}
