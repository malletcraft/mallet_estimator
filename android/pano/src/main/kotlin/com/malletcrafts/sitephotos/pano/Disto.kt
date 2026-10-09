package com.malletcrafts.sitephotos.pano

/**
 * The Leica DISTO BLE protocol — the pure, testable half.
 *
 * Built ONLY from published sources (checked 2026-10-09), never from Leica's
 * own SDK, whose developer agreement we have not signed:
 *  - lz1asl/CaveSurvey `LeicaDistoBluetoothLEDevice.java` (MIT), an Android
 *    app tested on the D110 and D810: the UUIDs, the float32 little-endian
 *    METRES on the distance characteristic, indication rather than
 *    notification, and its rule of trusting only the metric display codes;
 *  - seichter/d2relay `doc/notes.md`: a GATT dump of a D2 with properties
 *    (distance and unit both `read, indicate`, command write-without-response);
 *  - FablabPPR/leica-disto-transfer: the ASCII trigger commands.
 * Eight or more independent clients agree on the service and distance UUIDs.
 *
 * What the sources do NOT agree on is the unit code table, so this file
 * does not pretend to know it. A reading is accepted only while the meter
 * shows one of the codes every source agrees are metric (0..3); anything
 * else is refused with a reason, never converted on a guess. The Laser test
 * screen shows the raw code so the D2's real table can be written down from
 * the meter itself rather than from somebody's notes.
 *
 * WRITTEN DOWN FROM AMIT'S D2 (v4.0, "DISTO 44517824"), 2026-10-09, Laser
 * test on 0.3.178 and 0.3.180: it advertises the DISTO service and is found
 * in ~3 s; the unit characteristic is 2 bytes and read [00 00] = code 0 on
 * every reading, INCLUDING two taken while the D2's screen showed ft (1997
 * and 2037 mm) -- so this meter sends metres whatever it displays, and the
 * unit arrives again just after each distance. 'g' fired it 10 of 10 times,
 * and its own button sends a distance with no 'g' before it. It dropped the
 * link once after ~64 s idle and the client reconnected in 9 s. The refusal
 * of other codes stays as the guard for any OTHER model that does convert.
 */
object Disto {

    private const val SUFFIX = "-f831-4395-b29d-570977d5bf94"

    /** The DISTO service. Not every model is known to advertise it, so a
     *  scan also accepts the name prefix below. */
    const val SERVICE = "3ab10100$SUFFIX"

    /** float32 little-endian, metres. read + indicate. */
    const val CH_DISTANCE = "3ab10101$SUFFIX"

    /** The meter's display unit, 2 bytes. read + indicate. */
    const val CH_DISTANCE_UNIT = "3ab10102$SUFFIX"

    /** Bare ASCII, write-without-response. */
    const val CH_COMMAND = "3ab10109$SUFFIX"

    /** Client Characteristic Configuration — the standard BLE descriptor. */
    const val CCCD = "00002902-0000-1000-8000-00805f9b34fb"

    /** Every published client matches on this prefix rather than a full name. */
    const val NAME_PREFIX = "DISTO"

    /** Remote trigger and laser on/off. Single public source — the Laser test
     *  screen is where they are proved on the D2 before anything relies on them. */
    const val CMD_MEASURE = "g"
    const val CMD_LASER_ON = "o"
    const val CMD_LASER_OFF = "p"

    /** The display codes every source agrees are metric (CaveSurvey: "4
     *  different decimal meter formats"). Not a full unit table on purpose. */
    fun isMetric(unitCode: Int): Boolean = unitCode in 0..3

    /** Metres on the wire → millimetres, the only length unit this house uses. */
    fun toMm(metres: Float): Int = Math.round(metres * 1000.0).toInt()

    /** A value that can be a room length at all: finite, positive, and inside
     *  what a hand-held meter reaches. */
    fun plausible(metres: Float): Boolean =
        metres.isFinite() && metres > 0.0f && metres < 500.0f

    /** float32 little-endian, as the characteristic delivers it. */
    fun readFloat32Le(bytes: ByteArray, offset: Int = 0): Float {
        if (bytes.size < offset + 4) return Float.NaN
        var bits = 0
        for (i in 3 downTo 0) {
            bits = (bits shl 8) or (bytes[offset + i].toInt() and 0xFF)
        }
        return Float.fromBits(bits)
    }

    /** uint16 little-endian; a one-byte payload is read as that byte. */
    fun readUint16Le(bytes: ByteArray, offset: Int = 0): Int {
        if (bytes.size < offset + 1) return -1
        val lo = bytes[offset].toInt() and 0xFF
        if (bytes.size < offset + 2) return lo
        return lo or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
    }

    fun hex(bytes: ByteArray): String = bytes.joinToString(" ") { "%02X".format(it) }

    /** What became of one distance indication. Exactly one of the two is set. */
    data class Outcome(val reading: Reading?, val refused: String?)

    data class Reading(val mm: Int, val unitCode: Int)

    /**
     * Joins the two characteristics into one reading.
     *
     * The unit is read once when the app subscribes and then whenever it
     * indicates a change, so a distance normally finds it already known. A
     * distance that arrives before any unit is HELD, not given a unit we were
     * never told, and is released by the unit when it comes.
     */
    class Pairing {
        private var pendingMetres: Float? = null
        var unit: Int = -1
            private set

        fun onUnit(unitCode: Int): Outcome? {
            unit = unitCode
            val m = pendingMetres ?: return null
            pendingMetres = null
            return judge(m)
        }

        fun onDistance(metres: Float): Outcome? {
            if (unit < 0) { pendingMetres = metres; return null }
            return judge(metres)
        }

        private fun judge(metres: Float): Outcome = when {
            !plausible(metres) -> Outcome(null, "Not a length (%.4f)".format(metres))
            !isMetric(unit) -> Outcome(null,
                "Meter is not showing metres/mm (unit code $unit) — set it to m or mm")
            else -> Outcome(Reading(toMm(metres), unit), null)
        }
    }
}
