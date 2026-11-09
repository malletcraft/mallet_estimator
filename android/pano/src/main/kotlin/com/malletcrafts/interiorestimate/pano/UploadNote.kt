package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * THE AS-IS UPLOAD'S STORED NOTES, AND THE LINES THEY ARE DRAWN AS (port 61c slice M2).
 *
 * AsIsUpload keeps each capture's state in asisupload/<id>.json with an English `error` note, and
 * SyncWorker stamps CaptureStore rows the same way and COMPARES the stamp back by equality
 * (`c.error == notPhoto`) — and old files on the phone already hold these bytes. So the constants
 * cannot translate and cannot move a byte (the M4 REPLACED / NOT_SENT rule: stored in English, drawn
 * through a key). They live HERE, in :pano, because the conflictText and PlanSave lesson (M3b
 * finding 4, M3c critic 2) is that a words-home in :app cannot be pinned where the suite runs — and
 * TWO readers share the rendering (survey prep's web line and the queue's rows), so the mapper
 * exists once or the two drift.
 *
 * [line] renders ONE stored note in the live language; [statusLine] renders the whole web line from
 * a capture's [Status]. Any note neither an exact constant nor a known prefix is DRAWN AS STORED —
 * a bench exception's words, ServerMessage's output, S5's own sentences — never guessed at
 * (M4: "Any other stored note is drawn as stored").
 */
object UploadNote {

    // ---- the stored notes, byte for byte what 0.3.299 and every earlier build wrote.
    // A moved constant that changed a byte orphans every state file on the phone.

    const val NOT_SIGNED_IN = "not signed in - open the menu and sign in" // text:value a stored note, shown through tx.app_note_not_signed_in_open_menu
    const val NO_PREP = "no survey prep saved for this capture" // text:value a stored note, shown through tx.app_note_no_survey_prep_saved
    const val NO_MEMORY = "not enough memory to square the 360 — will try again" // text:value a stored note, shown through tx.app_note_not_enough_memory
    const val CAPTURE_GONE = "this capture is no longer on the phone" // text:value a stored note, shown through tx.app_note_capture_no_longer_phone
    const val WAITING_FILED = "waiting for the capture to be filed" // text:value a stored note, shown through tx.app_note_waiting_capture_filed
    const val WAITING_PROJECT = "waiting for the project to reach ERP" // text:value a stored note, shown through tx.app_note_waiting_project_erp
    const val WEB_REFUSED_PASSWORD = "the web refused the saved password" // text:value a stored note, shown through tx.app_note_web_refused_password
    const val SESSION_EXPIRED = "web session expired - renewing when the web answers" // text:value a stored note, shown through tx.app_note_web_session_expired
    const val SESSION_RENEWED = "web session renewed" // text:value a stored note, shown through tx.app_note_web_session_renewed
    const val SESSION_TO_RENEW = "web session to be renewed when the web answers" // text:value a stored note, shown through tx.app_note_web_session_to_renew
    const val SIGNIN_REFUSED_KEY = "web sign-in refused - sending with the ERP key" // text:value a stored note, shown through tx.app_note_web_signin_refused_key

    // SyncWorker's stamps (compared back by equality: the BYTES are the contract)
    const val PHONE_ONLY_SAVE_PREP = "on this phone only - save its survey prep to send it to the web" // text:value a stored note, shown through tx.app_note_phone_only_save_prep
    const val PHONE_ONLY_NOT_PHOTO = "on this phone only - the web takes a 360's survey prep, not a photo" // text:value a stored note, shown through tx.app_note_phone_only_not_photo
    const val PANO_MISSING = "pano file missing on device" // text:value a stored note, shown through tx.app_note_pano_file_missing

    // the three prefixes: the tail is run back through [line] (sign-in, readings) or kept as S5's own words (web answered)
    const val SIGN_AGAIN_PREFIX = "sign in again (menu → Server) - " // text:value a stored note's head, shown through tx.app_note_sign_again_x
    const val READINGS_NOT_SENT_PREFIX = "readings not sent: " // text:value a stored note's head, shown through tx.app_note_readings_not_sent_x
    const val WEB_ANSWERED_PREFIX = "web answered " // text:value a stored note's head, shown through tx.app_note_web_answered_x

    /**
     * One capture's upload state, read from asisupload/<id>.json (AsIsUpload.status): the raw
     * [state] ("waiting" / "done" / "error" / "refused" — stored values, compared, never shown),
     * the done time [hm] ("14:05", a figure), the stored English [note] (or null), whether any
     * photo went ([image]), and [ok] — the one flag the colours and the sync read, never a string.
     */
    class Status(val state: String, val hm: String, val note: String?, val image: Boolean, val ok: Boolean)

    /**
     * One stored note in the live language (the M4 noteLine idiom): an exact constant renders its
     * key; `sign in again (menu → Server) - {why}` and `readings not sent: {why}` match by prefix
     * and render their key with the TAIL run back through [line] (so a nested
     * `the web refused the saved password` or `web answered 401: …` translates too);
     * `web answered {code}` keeps S5's own `: {clean}` tail as given (data). ANY OTHER NOTE IS
     * DRAWN AS STORED.
     */
    fun line(tx: Tx, note: String): String = when (note) {
        NOT_SIGNED_IN -> tx.app_note_not_signed_in_open_menu
        NO_PREP -> tx.app_note_no_survey_prep_saved
        NO_MEMORY -> tx.app_note_not_enough_memory
        CAPTURE_GONE -> tx.app_note_capture_no_longer_phone
        WAITING_FILED -> tx.app_note_waiting_capture_filed
        WAITING_PROJECT -> tx.app_note_waiting_project_erp
        WEB_REFUSED_PASSWORD -> tx.app_note_web_refused_password
        SESSION_EXPIRED -> tx.app_note_web_session_expired
        SESSION_RENEWED -> tx.app_note_web_session_renewed
        SESSION_TO_RENEW -> tx.app_note_web_session_to_renew
        SIGNIN_REFUSED_KEY -> tx.app_note_web_signin_refused_key
        PHONE_ONLY_SAVE_PREP -> tx.app_note_phone_only_save_prep
        PHONE_ONLY_NOT_PHOTO -> tx.app_note_phone_only_not_photo
        PANO_MISSING -> tx.app_note_pano_file_missing
        else -> when {
            note.startsWith(SIGN_AGAIN_PREFIX) ->
                tx.app_note_sign_again_x(line(tx, note.substring(SIGN_AGAIN_PREFIX.length)))
            note.startsWith(READINGS_NOT_SENT_PREFIX) ->
                tx.app_note_readings_not_sent_x(line(tx, note.substring(READINGS_NOT_SENT_PREFIX.length)))
            note.startsWith(WEB_ANSWERED_PREFIX) -> webAnswered(tx, note) ?: note
            // the done state's tail is ReadingsSync.push's stored conflict note (conflictText(Tx.EN, …)):
            // its owner re-renders it, and a note that matches nothing comes back AS STORED
            else -> Readings.conflictNoteLine(tx, note)
        }
    }

    /** `web answered {code}` through its key, S5's `: {clean}` tail as given — or null when what
     *  follows the prefix is not a status code (drawn as stored, never guessed at). */
    private fun webAnswered(tx: Tx, note: String): String? {
        val rest = note.substring(WEB_ANSWERED_PREFIX.length)
        val code = rest.takeWhile { it.isDigit() }
        if (code.isEmpty()) return null
        val tail = rest.substring(code.length)
        if (tail.isNotEmpty() && !tail.startsWith(":")) return null
        return tx.app_note_web_answered_x(code) + tail
    }

    /**
     * The web line itself, rendered at DRAW time with the live tx (M3b's readingsMsg rule: a
     * long-lived message is drawn in the language on when it is drawn). Byte-equal under Tx.EN to
     * the strings AsIsUpload.status composed before M2. A blank or absent note on an error or
     * refused state renders `app.note.upload_failed` — the render default "upload failed", a
     * state whose file carries no note, not a stored note (the M2 critic's second round).
     */
    fun statusLine(tx: Tx, s: Status): String = when (s.state) {
        "done" -> tx.app_on_web_x(s.hm) +
            (if (s.image) "" else tx.app_no_photos_360_not_phone) +
            (s.note?.takeIf { it.isNotBlank() }?.let { " · " + line(tx, it) } ?: "") // text:value the " · " join
        "error", "refused" -> tx.app_not_on_web_x(
            s.note?.takeIf { it.isNotBlank() }?.let { line(tx, it) } ?: tx.app_note_upload_failed)
        else -> tx.common_waiting_upload +
            (s.note?.takeIf { it.isNotBlank() }?.let { " · " + line(tx, it) } ?: "") // text:value the " · " join
    }
}
