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

    /**
     * A FLAT photograph, as a one-entry face list.
     *
     * The viewer, the Original/Annotated toggle and the "Annotate in
     * ImageMeter" button are all driven by this list — so a Photo returning
     * an EMPTY list is exactly why a single photo had no way to reach
     * ImageMeter at all. It is not a face and must never appear among the
     * six, which is why faceOf() still rejects the token; it is looked up
     * explicitly instead.
     */
    fun photoOf(context: Context, captureId: String): List<Face> {
        if (!Handover.isDeviceId(captureId)) return emptyList()
        var found: Face? = null
        runCatching {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.RELATIVE_PATH),
                "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?",
                arrayOf("${captureId}_$PHOTO%"),
                "${MediaStore.Images.Media.DATE_ADDED} ASC",
            )?.use { c ->
                while (c.moveToNext() && found == null) {
                    // OURS, not a copy something else re-exported: the first
                    // one written, in our own folder.
                    if (!(c.getString(2) ?: "").contains(OUR_FOLDER, true)) continue
                    found = Face(PHOTO, ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        c.getLong(0)), c.getString(1) ?: "")
                }
            }
        }
        return listOfNotNull(found)
    }

    /** The token a flat photograph carries. Deliberately not one of the six. */
    const val PHOTO = "photo"

    /**
     * The ANNOTATED copy of each face, from the gallery, with no folder grant.
     *
     * ImageMeter's data directory is /Android/data/de.dirkfarin.imagemeter/…,
     * which no app can be granted access to: Android 11 removed
     * ACTION_OPEN_DOCUMENT_TREE for /Android/data and Android 13 closed the
     * subdirectory loophole. A directory grant simply cannot reach it, so
     * that is not the road.
     *
     * The road is ImageMeter's own "Show images in gallery" switch. With it
     * on, ImageMeter publishes to MediaStore — which we can already read,
     * with the permission the app already holds. Its copy carries the same
     * name we handed over, so it lands as a SECOND row for a face we wrote
     * once, and the one that is NOT in our own folder is the annotated one.
     *
     * of() takes the first match per face on purpose (ours); this takes the
     * others. Same query, opposite half.
     */
    fun annotatedOf(context: Context, captureId: String): Map<String, Uri> {
        if (!Handover.isDeviceId(captureId)) return emptyMap()
        val out = HashMap<String, Uri>()
        val projection = arrayOf(MediaStore.Images.Media._ID,
                                 MediaStore.Images.Media.DISPLAY_NAME,
                                 MediaStore.Images.Media.RELATIVE_PATH,
                                 MediaStore.Images.Media.DATE_ADDED)
        runCatching {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?",
                arrayOf("$captureId%"),
                // Newest first: if ImageMeter has published a face twice, the
                // later drawing is the one that counts.
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val display = c.getString(1) ?: continue
                    // A flat photo's annotated copy carries "photo", which
                    // faceOf() rejects by design — accepted explicitly here
                    // so the toggle lights up on a single photograph too.
                    val face = faceOf(display)
                        ?: PHOTO.takeIf { display.contains("_$PHOTO") }
                        ?: continue
                    if (face in out) continue
                    // Ours lives under Pictures/MCFT Site Photos/… — anything
                    // there is the original we wrote, never an annotation.
                    val path = c.getString(2) ?: ""
                    if (path.contains(OUR_FOLDER, ignoreCase = true)) continue
                    out[face] = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(0))
                }
            }
        }
        return out
    }

    /** The folder FaceWriter writes into. Matching on it is what separates
     *  our original from somebody else's copy of it. */
    private const val OUR_FOLDER = "MCFT Site Photos"

    /** MCAP-…_top.jpg -> "up". Tolerates whatever suffix an exporter bolted
     *  on, because the face token is what matters, not the extension.
     *
     *  THE TOKEN IS A LABEL, NOT A FACE NAME, and reading it as a face name is
     *  what hid the ceiling and the floor. FaceWriter writes all six and
     *  Handover.filename names the vertical two "top" and "bottom"; this
     *  compared those against Panorama.FACES ("up"/"down"), found no match,
     *  and returned null — so load() dropped them and the app showed four
     *  faces out of six with no error anywhere. The files were on the phone
     *  the whole time.
     *
     *  Handover.faceOfToken owns the mapping now, beside the function that
     *  writes the name, and accepts a bare face name too so anything already
     *  written the old way still reads. */
    fun faceOf(displayName: String): String? {
        val stem = displayName.substringBeforeLast('.')
        val token = stem.substringAfterLast('_', "")
        return Handover.faceOfToken(token)
    }
}
