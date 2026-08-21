package com.malletcrafts.sitephotos

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.malletcrafts.sitephotos.pano.Handover
import com.malletcrafts.sitephotos.pano.Panorama

/**
 * Finding a capture's six faces again, on the device.
 *
 * FaceWriter puts them in Pictures/MCFT Site Photos/<client>/<project>/<ROOM>/
 * as MCAP-<id>_<face>.jpg, because that is the folder ImageMeter's importer
 * browses. That filename is also the identity — the one thing that survives
 * the round trip — so it is what this looks them up by, rather than tracking
 * uris in a table that could disagree with the disk.
 *
 * Queried by DISPLAY_NAME rather than by path: a phone that moved the folder,
 * or a face re-saved by another app, still comes back.
 */
object LocalFaces {

    data class Face(val name: String, val uri: Uri, val displayName: String)

    /** The six faces of one capture, in projection order, missing ones
     *  dropped rather than faked. */
    fun of(context: Context, captureId: String): List<Face> {
        if (!Handover.isDeviceId(captureId)) return emptyList()
        val found = HashMap<String, Face>()
        val projection = arrayOf(MediaStore.Images.Media._ID,
                                 MediaStore.Images.Media.DISPLAY_NAME)
        runCatching {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?",
                arrayOf("$captureId%"),
                null,
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                while (c.moveToNext()) {
                    val display = c.getString(nameCol) ?: continue
                    val face = faceOf(display) ?: continue
                    // First match wins: a face re-exported by another app
                    // must not displace the one this phone produced.
                    if (face in found) continue
                    found[face] = Face(
                        face,
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            c.getLong(idCol)),
                        display)
                }
            }
        }
        return Panorama.FACES.map { it.first }.mapNotNull { found[it] }
    }

    /** MCAP-…_front.jpg -> "front". Tolerates whatever suffix an exporter
     *  bolted on, because the face token is what matters, not the extension. */
    fun faceOf(displayName: String): String? {
        val stem = displayName.substringBeforeLast('.')
        val token = stem.substringAfterLast('_', "").lowercase()
        return if (Panorama.FACES.any { it.first == token }) token else null
    }
}
