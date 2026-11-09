package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * THE LASER'S WORDS (port 61c slice M6). Under `Tx.EN` every builder is BYTE-EQUAL to the string
 * DistoClient or Disto composed at 80d3526, so nothing on screen or in the laser log moves under
 * English; under `Tx.MR` the pins are hand-typed against the card's values (the vis() answers in
 * §29's derivation). The fills stay as given — a system message's tail is data, Latin and all.
 */
class LaserNoteTest {

    @Test fun `every builder under Tx EN is byte-equal to the string the client composed before M6`() {
        assertEquals("Turn Bluetooth on", LaserNote.turnBluetoothOn(Tx.EN))
        assertEquals("No BLE scanner", LaserNote.noBleScanner(Tx.EN))
        assertEquals("Scan refused: application registration failed",
            LaserNote.scanRefused("application registration failed")(Tx.EN))
        assertEquals("No DISTO found — press its Bluetooth button and try again", LaserNote.noDistoFound(Tx.EN))
        assertEquals("Scan failed (6)", LaserNote.scanFailed(6)(Tx.EN))
        assertEquals("Bluetooth refused: need android.permission.BLUETOOTH_CONNECT",
            LaserNote.bluetoothRefused("need android.permission.BLUETOOTH_CONNECT")(Tx.EN))
        assertEquals("Bluetooth connect permission refused", LaserNote.connectPermissionRefused(Tx.EN))
        assertEquals("Reconnecting (133)…", LaserNote.reconnecting(133)(Tx.EN))
        assertEquals("Meter disconnected", LaserNote.meterDisconnected(Tx.EN))
        assertEquals("Disconnected (8)", LaserNote.disconnected(8)(Tx.EN))
        assertEquals("Not a DISTO (no DISTO service)", LaserNote.notADisto(Tx.EN))
        assertEquals("Meter is missing the distance characteristic", LaserNote.noDistanceCharacteristic(Tx.EN))
        assertEquals("Meter is not showing metres/mm (unit code 4) — set it to m or mm",
            LaserNote.refusal(Disto.Refusal(Disto.Refusal.Kind.NOT_METRIC, "4"))(Tx.EN))
        assertEquals("Not a length (0.0000)",
            LaserNote.refusal(Disto.Refusal(Disto.Refusal.Kind.NOT_A_LENGTH, "0.0000"))(Tx.EN))
    }

    @Test fun `every builder under Tx MR is the card's value, the fills as given`() {
        assertEquals("ब्लूटूथ सुरू करा", LaserNote.turnBluetoothOn(Tx.MR))
        assertEquals("ब्लूटूथ स्कॅनर उपलब्ध नाही", LaserNote.noBleScanner(Tx.MR))
        // the system's {why} as given — the tail stays Latin
        assertEquals("स्कॅन नाकारले: application registration failed",
            LaserNote.scanRefused("application registration failed")(Tx.MR))
        assertEquals("DISTO सापडला नाही — त्याचे ब्लूटूथ बटण दाबा आणि पुन्हा प्रयत्न करा", LaserNote.noDistoFound(Tx.MR))
        assertEquals("स्कॅन अयशस्वी (6)", LaserNote.scanFailed(6)(Tx.MR))
        assertEquals("ब्लूटूथने नकार दिला: boom", LaserNote.bluetoothRefused("boom")(Tx.MR))
        assertEquals("ब्लूटूथ जोडण्याची परवानगी नाकारली", LaserNote.connectPermissionRefused(Tx.MR))
        assertEquals("पुन्हा जोडत आहे (133)…", LaserNote.reconnecting(133)(Tx.MR))
        assertEquals("मीटर डिस्कनेक्ट झाला", LaserNote.meterDisconnected(Tx.MR))
        assertEquals("जोडणी तुटली (8)", LaserNote.disconnected(8)(Tx.MR))
        assertEquals("हा DISTO नाही (DISTO सेवा नाही)", LaserNote.notADisto(Tx.MR))
        assertEquals("मीटरमध्ये अंतर पाठवण्याची सोय नाही", LaserNote.noDistanceCharacteristic(Tx.MR))
        assertEquals("मीटरवर मीटर/mm दिसत नाही (युनिट कोड 4) — तो m किंवा mm वर सेट करा",
            LaserNote.refusal(Disto.Refusal(Disto.Refusal.Kind.NOT_METRIC, "4"))(Tx.MR))
        assertEquals("हे लांबीचे मोजमाप नाही (0.0000)",
            LaserNote.refusal(Disto.Refusal(Disto.Refusal.Kind.NOT_A_LENGTH, "0.0000"))(Tx.MR))
    }
}
