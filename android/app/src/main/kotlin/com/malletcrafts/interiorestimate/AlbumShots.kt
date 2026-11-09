package com.malletcrafts.interiorestimate

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import com.malletcrafts.interiorestimate.pano.Album
import com.malletcrafts.interiorestimate.pano.AlbumPage
import com.malletcrafts.interiorestimate.pano.AlbumQueue
import com.malletcrafts.interiorestimate.pano.AlbumShot
import com.malletcrafts.interiorestimate.pano.AlbumWrite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

/**
 * THE PROGRESS SHOT'S FILE (port 61c slice K5, 9 Nov 2026; DESIGN-port61c.md §20, "THE SHOT'S FILE"; its rules are
 * AlbumShot's, in :pano). S5 takes a photo's JPEG once, at most 8 MB; the phone's camera app writes what it writes, so the
 * queued file is never the camera's: [write] decodes it at a power-of-two sample, scales it to a 3072 px long side, turns
 * and mirrors it by its EXIF orientation (the platform's android.media.ExifInterface: no new library; the turn baked
 * into the pixels and the EXIF left behind, so no location or camera serial reaches S5 or the client), encodes it at the
 * first quality that fits, into a temp file renamed onto `filesDir/album/shots/<id>.jpg` (AlbumQueue.shotPath) -- and
 * only then is the op queued, so no op is ever queued without its JPEG.
 *
 * KEPT UNTIL IT LANDS, then MOVED into the album's image cache by AlbumOutbox's commit (AlbumImages.adopt), never
 * copied; a shot no login's outbox holds goes ten minutes after it was written ([trim], AlbumShot.orphans), and NOTHING
 * goes when the outbox could not be read (the K5 critic, finding 5). The camera app's own file under `files/shots/`
 * (the FileProvider's `shots/`) goes as soon as the shot is written, and any left behind a day later ([trim],
 * AlbumShot.rawStale; the K5 critic, finding 12). A sign-out leaves the shots: the outbox survives it (AlbumSync.forgetAll
 * deletes the mirror files only).
 *
 * [file] runs the encode and the queueing on a scope of the PROCESS, not the composition's (the K5 critic, finding 2): a
 * rotation during the second the encode takes recreates the activity and cancels its scope, which left the shot written
 * and its op never queued. Never throws: every failure is false and one Log.w.
 */
object AlbumShots {

    private const val TAG = "AlbumShots"
    private const val RAW_DIR = "shots" // text:value the FileProvider's folder (res/xml/file_paths.xml), never shown

    /** The shot's encode and its queueing outlive the activity (finding 2): a scope of the process, on the IO pool. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** The file the phone's camera app writes the shot [id] into, through the FileProvider's `shots/`. */
    fun rawFile(context: Context, id: String): File =
        File(File(context.filesDir, RAW_DIR).apply { mkdirs() }, AlbumShot.RAW_PREFIX + id + ".jpg") // text:value a file name, never shown

    /** The queued shot of the album photo [id] (AlbumQueue.shotPath, under filesDir), there or not. */
    fun fileOf(context: Context, id: String): File = File(context.filesDir, AlbumQueue.shotPath(id))

    /**
     * The shot of [id] written once from [source] (the camera app's file, or a single photo's kept copy): decoded,
     * turned, scaled, encoded at the first quality that fits S5's 8 MB, into a temp file renamed into place. False, with
     * one Log.w and no file left, on any failure (a file that does not decode, a full disk, no memory). Blocking.
     */
    fun write(context: Context, source: File, id: String): Boolean {
        val dest = fileOf(context, id)
        val tmp = File(dest.parentFile, dest.name + AlbumPage.PART)
        var bmp: Bitmap? = null
        return try {
            dest.parentFile?.mkdirs()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IllegalStateException("not a picture") // text:value log line
            val opts = BitmapFactory.Options().apply { inSampleSize = AlbumShot.sampleFor(bounds.outWidth, bounds.outHeight) }
            var b = BitmapFactory.decodeFile(source.path, opts) ?: throw IllegalStateException("not decoded") // text:value log line
            bmp = b
            val turn = AlbumShot.turnOf(runCatching {
                ExifInterface(source.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL))
            val (w, h) = AlbumShot.sizeFor(b.width, b.height)
            val m = Matrix()
            if (w != b.width || h != b.height) m.postScale(w / b.width.toFloat(), h / b.height.toFloat())
            if (turn.degrees != 0) m.postRotate(turn.degrees.toFloat())
            if (turn.mirror) m.postScale(-1f, 1f)
            if (!m.isIdentity) {
                val t = Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
                if (t !== b) b.recycle()
                b = t
                bmp = t
            }
            var fits = false
            for (q in AlbumShot.QUALITIES) {
                tmp.outputStream().use { out -> if (!b.compress(Bitmap.CompressFormat.JPEG, q, out)) throw IllegalStateException("not encoded") } // text:value log line
                if (tmp.length() in 1..AlbumShot.MAX_BYTES.toLong()) { fits = true; break }
            }
            if (!fits) throw IllegalStateException("larger than S5 takes") // text:value log line
            if (!tmp.renameTo(dest)) throw IllegalStateException("not renamed into place") // text:value log line
            true
        } catch (e: Exception) {
            Log.w(TAG, "$id: shot not written: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
            runCatching { tmp.delete() }
            false
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "$id: shot not written: out of memory") // text:value log line
            runCatching { tmp.delete() }
            false
        } finally {
            bmp?.recycle()
        }
    }

    /**
     * THE CAMERA APP'S ✓ (port 61c slice K5): [pending]'s shot written from the camera app's file, then its op queued
     * under the login and the project the tap was drawn for (AlbumWrite.shotOp from the pending shot alone: no Model,
     * no screen; the lock is S5's 409 to decide) and the drain asked to run -- on the process's scope, so a recreated
     * activity does not lose it (finding 2). [done] runs on the main thread with whether the op was queued: the caller
     * opens the shot's chapter and sets the result line only then, so no frame draws an open chapter with no card
     * (finding 2). The camera app's file goes either way.
     */
    fun file(context: Context, pending: AlbumWrite.Pending, now: Instant, done: (Boolean) -> Unit) {
        val app = context.applicationContext
        scope.launch {
            val raw = rawFile(app, pending.shot.id)
            val written = write(app, raw, pending.shot.id)
            val queued = written && AlbumOutbox.add(app, pending.project, pending.login, AlbumWrite.shotOp(pending.shot, now), emptyList())
            runCatching { raw.delete() }
            if (queued) AlbumOutbox.enqueue(app, pending.project, replace = true)
            withContext(Dispatchers.Main) { done(queued) }
        }
    }

    /**
     * THE CAPTURE 360 SHEET'S SINGLE PHOTO after approval (Amit's answer 3 of 28 Oct 2026): [op]'s shot written from the
     * row's kept file [source] (`panos/<id>.jpg`, FaceWriter.single's copy of the original -- never the captioned
     * gallery copy) and the op queued for [project] under [login], on the process's scope as [file] is. Its kept file
     * stays: the row is the phone's item, and its picture.
     *
     * THE OP IS FILED FROM THE ROW AS STORED WHEN IT IS ADDED (AlbumOutbox.addSingle; the K5 review, finding 1), not as
     * ingest built it: a re-file on its WORK line during the encode is carried, and a capture deleted meanwhile queues
     * nothing and its shot goes. THEN the sync runs (the K5 review, finding 4), which ingest leaves to here for this
     * photo: run before the op was queued, (c2) marked the row "on this phone only", and the ERP leg could delete the
     * kept file under the encode.
     */
    fun single(context: Context, project: String, login: String, op: Album.Op, source: File) {
        val app = context.applicationContext
        scope.launch {
            if (write(app, source, op.photo)) {
                if (AlbumOutbox.addSingle(app, project, login, op)) AlbumOutbox.enqueue(app, project, replace = true)
                else delete(app, op.photo)
            }
            SyncWorker.syncNow(app)
        }
    }

    /** The shot of [id] goes (its capture deleted before it reached S5: AlbumOutbox.unqueuePhoto). Never throws. */
    fun delete(context: Context, id: String) {
        runCatching { fileOf(context, id).delete() }
    }

    /**
     * The shots no login's outbox holds any more, ten minutes after they were written, and the camera app's files a day
     * old (AlbumShot.orphans, AlbumShot.rawStale). [held] null -- an outbox that could not be read -- deletes no shot
     * (finding 5). Blocking; never throws.
     */
    fun trim(context: Context, held: Set<String>?) {
        val now = System.currentTimeMillis()
        runCatching {
            val d = File(context.filesDir, AlbumQueue.SHOTS)
            val files = d.listFiles()?.filter { it.isFile }.orEmpty()
            val gone = AlbumShot.orphans(files.map { AlbumShot.ShotFile(it.name.removeSuffix(".jpg"), it.lastModified()) }, held, now).toHashSet() // text:value a file suffix
            for (f in files) if (f.name.removeSuffix(".jpg") in gone) f.delete() // text:value a file suffix
        }
        runCatching {
            val d = File(context.filesDir, RAW_DIR)
            val files = d.listFiles()?.filter { it.isFile }.orEmpty()
            val gone = AlbumShot.rawStale(files.map { AlbumShot.ShotFile(it.name, it.lastModified()) }, now).toHashSet()
            for (f in files) if (f.name in gone) f.delete()
        }
    }
}
