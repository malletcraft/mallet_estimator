package com.malletcrafts.sitephotos

import android.content.Context
import android.net.Uri

/**
 * Annotated copies a PERSON pointed at, rather than ones the app found.
 *
 * The automatic route reads the app's own stamp back out of the gallery and
 * is right when it works. It depends, though, on two things outside this
 * app's control: that ImageMeter publishes a copy somewhere MediaStore can
 * see, and that the copy still carries a readable mark. When either is
 * untrue, the app can only report "found nothing" — which, from where a
 * person is standing holding the annotated photograph, is indistinguishable
 * from broken.
 *
 * So there is a second way in, and it has no identification step to get
 * wrong: the viewer is already showing ONE face of ONE capture, so a file
 * picked there belongs to it by construction. That is not the error-prone
 * manual filing Amit rejected — that was the office guessing which capture a
 * renamed file came from. This is the person who took the photograph, on its
 * own screen, pointing at the marked-up version of it.
 *
 * Kept beside the found ones and merged over them, so a deliberate choice
 * always beats a guess.
 */
object PickedAnnotations {

    private fun prefs(context: Context) =
        context.getSharedPreferences("pickedann", Context.MODE_PRIVATE)

    private fun key(captureId: String, face: String) = "$captureId|${face.lowercase()}"

    fun put(context: Context, captureId: String, face: String, uri: Uri) {
        // Persistable permission, or the picked file is unreadable the next
        // time the screen opens — a photo that vanishes on the second look is
        // worse than one that never appeared.
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        prefs(context).edit().putString(key(captureId, face), uri.toString()).apply()
    }

    fun of(context: Context, captureId: String): Map<String, Uri> {
        val out = HashMap<String, Uri>()
        val head = "$captureId|"
        for ((k, v) in prefs(context).all) {
            if (!k.startsWith(head)) continue
            val s = (v as? String).orEmpty()
            if (s.isBlank()) continue
            out[k.removePrefix(head)] = Uri.parse(s)
        }
        return out
    }

    fun forget(context: Context, captureId: String, face: String) {
        prefs(context).edit().remove(key(captureId, face)).apply()
    }
}
