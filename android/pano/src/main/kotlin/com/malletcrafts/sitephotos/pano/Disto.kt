package com.malletcrafts.sitephotos.pano

/**
 * The Leica DISTO BLE protocol — the pure, testable half.
 *
 * Everything here comes from Leica's own SDK (the decompiled
 * `ch.leica.sdk` `MeasurementConverter` / `ReceivedBleDataPacket` /
 * `commands.json`) cross-checked against two shipping implementations,
 * rather than from the community write-ups: the most-copied of those has
 * the unit table wrong (it claims 0=m, 1=ft, 2=in), which would silently
 * turn a 2.5 m wall into 2.5 ft.
 *
 * Two facts shape everything below:
 *  - the wire value is ALWAYS metres, whatever the meter's display says.
 *    So we never parse the device's formatting; we take the float and do
 *    our own. That also sidesteps a real rounding bug in Leica's own
 *    fraction reducer.
 *  - readings arrive as a PAIR — the distance on one characteristic, the
 *    display unit on another. Leica's own guidance is to wait for both.
 */
object Disto {

    private const val SUFFIX = "-f831-4395-b29d-570977d5bf94"

    /** Advertised by the meter, so a scan can filter on it directly. */
    const val SERVICE = "3ab10100$SUFFIX"

    /** float32 little-endian, metres. Indicate (NOT notify). */
    const val CH_DISTANCE = "3ab10101$SUFFIX"

    /** uint16 little-endian: which unit the meter is DISPLAYING. */
    const val CH_DISTANCE_UNIT = "3ab10102$SUFFIX"

    /** Bare ASCII, no terminator, write-without-response. */
    const val CH_COMMAND = "3ab10109$SUFFIX"

    /** Client Characteristic Configuration — the standard descriptor. */
    const val CCCD = "00002902-0000-1000-8000-00805f9b34fb"

    /** The D2 advertises a SHORT name; the long "DISTO D2 123456789" seen
     *  in other apps is assembled client-side from BLE service data, so a
     *  scan filter must match the prefix, never the full string. */
    const val NAME_PREFIX = "DISTO "

    /** Trigger a measurement from the app. 'o'/'p' turn the laser on/off. */
    const val CMD_MEASURE = "g"
    const val CMD_LASER_ON = "o"
    const val CMD_LASER_OFF = "p"

    /**
     * How the meter is displaying the number. We only care enough to (a)
     * refuse non-linear modes and (b) honour "use the device's unit".
     *
     * The modal offsets are the trap: the SAME characteristic carries an
     * AREA when the meter is in area mode (unit code + 100) and a VOLUME in
     * volume mode (+ 1000). A reading in either mode is not a length and
     * must never land on a measurement line.
     */
    fun isLinear(unitCode: Int): Boolean = unitCode in 0..14

    /** Whether the meter's own display is imperial — used when the app is
     *  set to follow the device rather than its own preference. */
    fun isImperial(unitCode: Int): Boolean = unitCode in 4..13

    /**
     * Metres on the wire → canonical millimetres. Rounded, because every
     * length in this house is an integer millimetre.
     */
    fun toMm(metres: Float): Int = Math.round(metres * 1000.0).toInt()

    /**
     * Is this a reading we can put on a measurement line? Guards the two
     * ways a DISTO hands over something that is not a wall length: a
     * non-linear mode, and a nonsense/failed value.
     */
    fun usable(metres: Float, unitCode: Int): Boolean =
        isLinear(unitCode) && metres.isFinite() &&
            metres > 0.0f && metres < 500.0f

    /** float32 little-endian, as the characteristic delivers it. */
    fun readFloat32Le(bytes: ByteArray, offset: Int = 0): Float {
        if (bytes.size < offset + 4) return Float.NaN
        var bits = 0
        for (i in 3 downTo 0) {
            bits = (bits shl 8) or (bytes[offset + i].toInt() and 0xFF)
        }
        return Float.fromBits(bits)
    }

    /** uint16 little-endian. */
    fun readUint16Le(bytes: ByteArray, offset: Int = 0): Int {
        if (bytes.size < offset + 2) return -1
        return (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8)
    }

    /**
     * Pairs the two indications into one reading.
     *
     * Either characteristic may indicate first, and the unit only indicates
     * when it CHANGES — so the unit is read once at subscribe time and then
     * remembered. A distance with no unit yet is held, not dropped.
     */
    class Pairing {
        private var pendingMm: Int? = null
        private var unit: Int = -1

        /** The unit read at subscribe time, or a later change. */
        fun onUnit(unitCode: Int): Reading? {
            unit = unitCode
            val mm = pendingMm ?: return null
            pendingMm = null
            return emit(mm)
        }

        fun onDistance(metres: Float): Reading? {
            val mm = toMm(metres)
            if (unit < 0) {           // unit not known yet — hold, don't guess
                pendingMm = mm
                return null
            }
            if (!usable(metres, unit)) return null
            return emit(mm)
        }

        private fun emit(mm: Int): Reading? {
            if (!isLinear(unit) || mm <= 0) return null
            return Reading(mm, unit, isImperial(unit))
        }
    }

    data class Reading(val mm: Int, val unitCode: Int, val deviceImperial: Boolean)
}
