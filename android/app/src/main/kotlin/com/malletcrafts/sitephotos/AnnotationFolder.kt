package com.malletcrafts.sitephotos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

/**
 * ImageMeter's own folder, read directly, so a photo annotated on THIS phone
 * does not have to fly to Google Drive and back to be looked at.
 *
 * The round trip exists for a real case — a designer annotating at a desk on
 * another device, where the bench is the only thing both ends can see. It is
 * absurd for the common one. When the technician who took the photo annotates
 * it thirty seconds later, the annotated image is already on the device; the
 * only thing stopping the app reading it is that Android will not let one app
 * browse another's files without the person saying so.
 *
 * So the person says so, once: a SAF directory grant (ACTION_OPEN_DOCUMENT_TREE)
 * pointed at ImageMeter's data directory — Settings → Storage → Data directory
 * inside ImageMeter tells you where that is. The permission is persisted, so
 * it survives reboots and app updates.
 *
 * WHAT IT LOOKS FOR, and why it is written as a search rather than as a path.
 * ImageMeter keeps one directory per image, holding the unannotated original
 * plus its annotation data; exports land wherever the person sent them. Both
 * layouts have changed across ImageMeter versions and neither is a contract
 * anyone owes us. So this does not walk a known path: it indexes every image
 * under the granted tree and matches on the ONE thing we control — the name
 * FaceWriter gave the file, MCAP-<id>_<face>. When a directory holds our
 * original AND another image, that other image is the annotated render.
 *
 * Everything here degrades to null rather than throwing. A folder the person
 * has not granted, or has granted and then revoked, or that holds nothing we
 * recognise, means "no local copy" — and the bench path still answers.
 */
class AnnotationFolder(private val context: Context) {

    private val prefs =
        context.getSharedPreferences("capture", Context.MODE_PRIVATE)

    /** Bounded so a grant pointed at the whole of Downloads cannot hang the
     *  UI: this runs on a background thread but it is still somebody waiting. */
    private val maxDirs = 400

    val tree: Uri?
        get() = prefs.getString(KEY, null)?.let { runCatching { Uri.parse(it) }.getOrNull() }

    val linked: Boolean get() = tree != null

    /** The last path segment of the granted tree, for the drawer row. A raw
     *  content:// uri in a settings row tells a person nothing. */
    val label: String
        get() {
            val t = tree ?: return "not linked"
            val id = runCatching { DocumentsContract.getTreeDocumentId(t) }.getOrNull()
                ?: return "linked"
            return id.substringAfterLast(':').substringAfterLast('/')
                .ifBlank { "linked" }
        }

    /** Persist the grant. Without takePersistable the uri dies with the
     *  process, and a setting that silently unsets itself is worse than one
     *  that was never offered. */
    fun link(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        prefs.edit().putString(KEY, uri.toString()).apply()
    }

    fun unlink() {
        tree?.let { t ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    t, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        prefs.edit().remove(KEY).apply()
    }

    // ---- reading ---------------------------------------------------------

    private data class Doc(val id: String, val name: String, val mime: String) {
        val isDir get() = mime == DocumentsContract.Document.MIME_TYPE_DIR
        val isImage get() = mime.startsWith("image/")
    }

    private fun children(tree: Uri, parentId: String): List<Doc> {
        val uri = runCatching {
            DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        }.getOrNull() ?: return emptyList()
        val out = mutableListOf<Doc>()
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE),
                null, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    out.add(Doc(
                        c.getString(0) ?: continue,
                        c.getString(1) ?: "",
                        c.getString(2) ?: ""))
                }
            }
        }
        return out
    }

    /** What one walk of the tree found. Reported to the person, because a
     *  folder grant that quietly finds nothing is indistinguishable from one
     *  that was never made. */
    data class Report(val folders: Int, val images: Int, val ours: Int)

    private fun walk(onImage: (dirId: String, doc: Doc) -> Unit): Report {
        val t = tree ?: return Report(0, 0, 0)
        val root = runCatching { DocumentsContract.getTreeDocumentId(t) }.getOrNull()
            ?: return Report(0, 0, 0)
        var folders = 0
        var images = 0
        var ours = 0
        val queue = ArrayDeque(listOf(root))
        while (queue.isNotEmpty() && folders < maxDirs) {
            val dir = queue.removeFirst()
            folders++
            for (d in children(t, dir)) {
                if (d.isDir) queue.addLast(d.id)
                else if (d.isImage) {
                    images++
                    if (stemOf(d.name) != null) ours++
                    onImage(dir, d)
                }
            }
        }
        return Report(folders, images, ours)
    }

    fun scan(): Report = walk { _, _ -> }

    /**
     * Every annotated face of one capture, in ONE walk of the tree.
     *
     * Two shapes are accepted, because both happen. If a directory holds the
     * original we handed over AND another image, the other image is the
     * annotated one — that is ImageMeter's per-image bundle. If an image
     * anywhere under the tree carries our stem and is not the file we wrote,
     * that is an export that kept its name. Anything else is not a match: a
     * wrong annotated copy on a client's wall is worse than none.
     *
     * One walk, not one per face, because six walks of a granted directory
     * tree is six times the content-provider round trips for an answer that
     * was in the first one.
     */
    fun annotatedFor(deviceId: String): Map<String, Uri> {
        val t = tree ?: return emptyMap()
        val bundles = HashMap<String, MutableList<Doc>>()
        val oursByDir = HashMap<String, MutableList<Pair<String, Doc>>>()
        walk { dirId, doc ->
            bundles.getOrPut(dirId) { mutableListOf() }.add(doc)
            val face = faceOf(doc.name, deviceId)
            if (face != null) {
                oursByDir.getOrPut(dirId) { mutableListOf() }.add(face to doc)
            }
        }
        val out = HashMap<String, Uri>()
        for ((dir, ours) in oursByDir) {
            val here = bundles[dir].orEmpty()
            for ((face, original) in ours) {
                // The bundle case: exactly one other image beside our
                // original. More than one and we cannot tell which is the
                // annotation, so we decline rather than guess.
                val others = here.filter { it.id != original.id &&
                                           faceOf(it.name, deviceId) == null }
                val pick = others.singleOrNull() ?: continue
                runCatching { DocumentsContract.buildDocumentUriUsingTree(t, pick.id) }
                    .getOrNull()?.let { out[face] = it }
            }
        }
        return out
    }

    /** The face a filename names, for THIS capture. Null when the file is
     *  not one of ours. */
    private fun faceOf(name: String, deviceId: String): String? {
        val m = Regex("${Regex.escape(deviceId)}_([a-z]+)").find(name) ?: return null
        return m.groupValues[1]
    }

    /** MCAP-<id>_<face> out of a filename, or null. Same identity the whole
     *  handover turns on. */
    private fun stemOf(name: String): String? =
        Regex("(MCAP-[0-9a-f]{12})_([a-z]+)").find(name)?.value

    private companion object {
        const val KEY = "imagemeter_tree"
    }
}
