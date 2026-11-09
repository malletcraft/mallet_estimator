package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Msg
import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * THE X3'S WORDS (port 61c slice M6): the connect notes the capture sheet shows and the shoot
 * path's typed refusal. X3Bridge composes nothing in English any more — it hands back one of these
 * [Msg]s, or throws [Refused] — so the words have a :pano home the suite pins in both languages
 * (the conflictText/PlanSave lesson), and the capture sheet's note redraws in the language on when
 * it is drawn (the M3b readingsMsg rule). [Refused] is the PlanSave idiom byte for byte: a typed
 * refusal whose exception message IS the pinned English render, so a logcat line reads exactly as
 * it did before M6.
 */
object CameraNote {

    /** No ConnectivityManager on this device: a system identifier, never translated. */
    const val NO_CM = "no ConnectivityManager" // text:value a system class name, the page draws it Latin

    /** `{problem} — join the X3's Wi-Fi in Settings, then try again`, over one of this file's notes. */
    fun joinWifi(problem: Msg): Msg = { tx -> tx.app_x3_x_join_wifi(problem(tx)) }

    /** `phone is not on the camera's Wi-Fi` — the bind timed out. */
    val notOnCameraWifi: Msg = { tx -> tx.app_x3_phone_not_camera_wifi }

    /** `cannot watch Wi-Fi: {why}` — registerNetworkCallback threw; the system's message as given. */
    fun cannotWatchWifi(why: String): Msg = { tx -> tx.app_x3_cannot_watch_wifi_x(why) }

    /** `camera connect error {code} — check the X3 is on, …` — the SDK's code, data. */
    fun connectError(code: Int): Msg = { tx -> tx.app_x3_connect_error_x(code.toString()) }

    /** How a shoot-and-export failed. The SDK's {code} and {why} travel beside it, data. */
    enum class Kind { NO_FILE, CAPTURE_ERROR, STITCH_FAILED, STITCH_CANCELLED }

    /** One refusal's words in [tx]'s language. */
    fun text(tx: Tx, kind: Kind, code: Int = 0, why: String? = null): String = when (kind) {
        Kind.NO_FILE -> tx.app_x3_no_file
        Kind.CAPTURE_ERROR -> tx.app_x3_capture_error_x(code.toString())
        Kind.STITCH_FAILED -> tx.app_x3_stitch_failed_x(code = code.toString(), why = why.toString())
        Kind.STITCH_CANCELLED -> tx.app_x3_stitch_cancelled
    }

    /**
     * The shoot path's typed refusal (the PlanSave.Refused idiom): its exception [message] is the
     * PINNED English render, so `Result<String>` keeps its shape and a logcat line keeps today's
     * bytes; the screen renders it through [line] with the live language.
     */
    class Refused(val kind: Kind, val code: Int = 0, val why: String? = null) :
        Exception(text(Tx.EN, kind, code, why))

    /** A shoot failure's shown words: a [Refused] through [text]; any other Throwable as its own
     *  message (the M3c exportFailed idiom — an alien exception's words are data, drawn as given). */
    fun line(tx: Tx, e: Throwable): String =
        if (e is Refused) text(tx, e.kind, e.code, e.why) else e.message ?: e.javaClass.simpleName
}
