package com.malletcrafts.sitephotos

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * The last leg: an annotated photograph the phone found in its own gallery,
 * sent home and filed against the face it belongs to.
 *
 * WHY THE PHONE AND NOT THE BENCH. The bench already tries — it scans the
 * Drive folder ImageMeter uploads into and attaches what it can match. It
 * cannot match anything, because ImageMeter renames its exports:
 * MCAP-4b2014dcba5e_front.jpg comes back as image_from_19._Aug_2026.jpg.
 * 88 files sat in a review inbox saying "no capture id in the filename". The
 * phone has the one thing the bench does not: the picture itself, with the
 * QR this app burned into its caption bar, which ImageMeter drew on top of
 * rather than under. So the identification happens here, where it is certain,
 * and only the answer travels.
 *
 * Amit, 2026-08-21: "i dont want to attach it manually . its error prone."
 * Nothing here asks a person anything.
 *
 * ONCE, AND ONLY ONCE. Every send is remembered by capture + face + the
 * MediaStore id of the exact image sent. Re-annotating the same face in
 * ImageMeter produces a NEW gallery row, so it gets a new id and goes up as
 * a second layer — which is right, the doctype holds several annotations per
 * face on purpose. Coming back to a screen you have already seen sends
 * nothing.
 */
object AnnotationPush {

    private fun sent(context: Context) =
        context.getSharedPreferences("annpush", Context.MODE_PRIVATE)

    private fun key(captureId: String, face: String, uri: Uri) =
        "$captureId|$face|${uri.lastPathSegment ?: uri.toString()}"

    /** True if this exact image has already been filed against this face. */
    fun alreadySent(context: Context, captureId: String, face: String,
                    uri: Uri): Boolean =
        sent(context).contains(key(captureId, face, uri))

    /**
     * Send every annotated face that has not gone up yet.
     *
     * Blocking — call it off the main thread. Returns how many were filed;
     * zero is the ordinary answer and is not a failure.
     */
    fun push(context: Context, docname: String, captureId: String,
             annotated: Map<String, Uri>): Int {
        if (docname.isBlank() || annotated.isEmpty()) return 0
        val client = FrappeClient.load(context) ?: return 0
        val store = sent(context)
        var filed = 0
        for ((face, uri) in annotated) {
            val k = key(captureId, face, uri)
            if (store.contains(k)) continue
            val tmp = File(context.cacheDir, "annpush/${captureId}_$face.jpg")
            runCatching {
                tmp.parentFile?.mkdirs()
                // Copied rather than streamed straight into the request: the
                // multipart body needs a length, and a content Uri will not
                // reliably give one.
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tmp.outputStream().use { out -> input.copyTo(out) }
                } ?: error("could not read the annotated image")
                client.uploadAnnotation(docname, face, tmp,
                    "Annotated in ImageMeter; matched by the app's stamp")
            }.onSuccess {
                // Marked sent only after the server has it. A crash between
                // the two costs one duplicate upload, which is recoverable;
                // marking first would lose the annotation silently, which is
                // not.
                store.edit().putBoolean(k, true).apply()
                filed += 1
            }
            tmp.delete()
        }
        return filed
    }

    /**
     * One face, chosen by a person, sent now.
     *
     * Not routed through push(): that one is keyed on "already sent this
     * exact image", which is right for a scan that runs on every resume and
     * wrong here. Picking a file is a deliberate act, and doing it again
     * means "no, THIS one" — so it always goes.
     */
    fun pushOne(context: Context, docname: String, captureId: String,
                face: String, uri: Uri): Boolean {
        val client = FrappeClient.load(context) ?: return false
        val tmp = File(context.cacheDir, "annpush/${captureId}_$face.jpg")
        return try {
            tmp.parentFile?.mkdirs()
            context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { out -> input.copyTo(out) }
            } ?: error("could not read the picked image")
            client.uploadAnnotation(docname, face, tmp,
                "Annotated in ImageMeter; attached by hand from the phone")
            sent(context).edit()
                .putBoolean(key(captureId, face, uri), true).apply()
            true
        } finally {
            tmp.delete()
        }
    }

    fun forget(context: Context) {
        sent(context).edit().clear().apply()
    }
}
