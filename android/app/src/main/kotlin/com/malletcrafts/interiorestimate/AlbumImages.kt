package com.malletcrafts.interiorestimate

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.malletcrafts.interiorestimate.pano.AlbumPage
import com.malletcrafts.interiorestimate.pano.AsIsPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.Collections

/**
 * THE ALBUM'S JPEG CACHE (port 61c slice K3; DESIGN-port61c.md §18, "The JPEGs"). A card or the viewer that is actually
 * composed asks for its picture ON DEMAND -- an album photo's JPEG (`GET …/album/photos/<id>/img`) or the front face of a
 * capture S5 holds (`GET …/<room>/c/<capture>/img/front`) -- and the bytes are kept on disk, so the next time it is drawn
 * with no network. A capture the phone holds is drawn from the phone's own file and never comes here (AlbumPage.pic).
 *
 * WHERE. `cacheDir/album-img/`, never filesDir/album, where the mirrors live and where K5's queued shots will
 * (AlbumQueue.SHOTS = "album/shots"): Android may evict this folder and a fetch simply runs again. Each file is named by a
 * SHA-256 of what it is -- the project, and the photo's id, or the capture's room, name and S5's `at` (AlbumPage.Pic.Face's
 * version) -- so a room of any length or script is one short name (the K3 critic, finding 9: a percent-encoded Devanagari
 * room passes NAME_MAX, as S5's own backup found), and a face re-squared by a later record PUT is a new file (finding 2).
 * An album photo's bytes are written ONCE on S5 (asis.js), so its file never goes stale.
 *
 * HOW MUCH. The folder is held to AlbumPage.CACHE_CAP_BYTES: after every write the files least recently drawn (by mtime,
 * which a hit moves) go until it is under AlbumPage.CACHE_TRIM_TO, and a temp file older than a minute -- the orphan of a
 * process killed mid-download, or of a write that threw -- goes too (AlbumPage.trimmed, the K3 review, finding 6). A write
 * that throws deletes its own temp file. At most two downloads run at once ([inFlight]), one per file at a time (a lock
 * per name); each is CANCELLED with the card that asked for it (AsIsUpload.fetchBytes), so the cards on screen never wait
 * behind ones scrolled past. A 404 is remembered for the visit ([newVisit]): a LazyColumn composes a card again every
 * time it scrolls back, and asking S5 again each time would be a request per scroll for a picture that is not there.
 *
 * WHAT IS KEPT. Only S5's 200 with JPEG bytes (FF D8 FF) of at most 8 MB (S5's MAX_IMG), written to a temp file and
 * renamed. A 401 renews the S5 sign-in once, as AlbumSync does. Anything else -- no network, a 5xx, a refusal -- leaves
 * the plain block and is one Log.w; nothing here is a failure of anything. A sign-out forgets every file with the mirrors
 * ([forgetAll], from AlbumSync.forgetAll): the pictures are the album as THAT person's role saw it. The deletes run on a
 * thread of their own, since sign-out is a tap; a download that lands after them keeps bytes S5 serves to any staff
 * member alike (the phone signs in staff only), so no generation is counted.
 *
 * A PROGRESS SHOT'S BYTES (port 61c slice K5): an album photo still queued, or just landed, is on the phone as its shot
 * (AlbumShots, `filesDir/album/shots/<id>.jpg`), so [fetch] looks there FIRST, then in this cache, then asks S5 -- one rule
 * about where an album photo's bytes are, as there is one for a capture's -- and when the JPEG lands AlbumOutbox's commit
 * MOVES the shot into this cache under the photo's own name ([adopt]), so a card never asks S5 for the bytes it was just
 * sent. One of the two files is there at every instant of the move.
 */
object AlbumImages {

    private const val TAG = "AlbumImages"
    private const val DIR = "album-img"
    private const val MAX_IMG = 8 * 1024 * 1024

    private val inFlight = Semaphore(2)
    private val locks = HashMap<String, Mutex>()
    private val missing: MutableSet<String> = Collections.synchronizedSet(HashSet())

    private fun dir(context: Context) = File(context.cacheDir, DIR)

    private fun hash(parts: List<String>): String {
        val d = MessageDigest.getInstance("SHA-256").digest(parts.joinToString("\u0000").toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(d.size * 2)
        for (x in d) { val v = x.toInt() and 0xff; sb.append(HEX[v shr 4]).append(HEX[v and 15]) }
        return sb.toString()
    }

    private const val HEX = "0123456789abcdef"

    /** The cache file of [pic] in [project]'s album, or null for a picture that is not S5's (the phone's own, or none). */
    fun fileOf(context: Context, project: String, pic: AlbumPage.Pic): File? = when (pic) {
        is AlbumPage.Pic.AlbumJpeg -> File(dir(context), hash(listOf("photo", project, pic.id)) + ".jpg") // text:value a cache file's name parts, never shown
        is AlbumPage.Pic.Face -> File(dir(context), hash(listOf("face", project, pic.room, pic.capture, pic.version)) + ".jpg") // text:value a cache file's name parts, never shown
        else -> null
    }

    private fun urlOf(project: String, pic: AlbumPage.Pic): String? {
        val segments = when (pic) {
            is AlbumPage.Pic.AlbumJpeg -> AlbumPage.photoImage(project, pic.id)
            is AlbumPage.Pic.Face -> AlbumPage.faceImage(project, pic.room, pic.capture)
            else -> return null
        }
        return BuildConfig.WEB_URL.trimEnd('/') + AsIsPath.path(segments) { AsIsUpload.seg(it) }
    }

    /** A new album visit: the 404s remembered by the last one are asked again. */
    fun newVisit() = missing.clear()

    /**
     * The file holding [pic], fetched from S5 when it is not on disk yet; null when it cannot be had now (no network, a
     * 404, not a JPEG, no sign-in). Suspends; cancelled with the composition that asked. Never throws but cancellation.
     */
    suspend fun fetch(context: Context, project: String, pic: AlbumPage.Pic): File? {
        if (project.isBlank()) return null
        // a progress shot on the phone, queued or just landed, before the cache and before S5 (port 61c slice K5)
        if (pic is AlbumPage.Pic.AlbumJpeg) {
            val shot = AlbumShots.fileOf(context, pic.id)
            if (withContext(Dispatchers.IO) { shot.isFile && shot.length() > 0 }) return shot
        }
        val f = fileOf(context, project, pic) ?: return null
        if (withContext(Dispatchers.IO) { hit(f) }) return f
        if (f.name in missing) return null
        val lock = synchronized(locks) { locks.getOrPut(f.name) { Mutex() } }
        return lock.withLock {
            if (withContext(Dispatchers.IO) { hit(f) }) f
            else inFlight.withPermit { download(context, project, pic, f) }
        }
    }

    /** [f] is on disk: its mtime moves, so the trim keeps what is drawn. */
    private fun hit(f: File): Boolean {
        if (!f.isFile || f.length() <= 0) return false
        runCatching { f.setLastModified(System.currentTimeMillis()) }
        return true
    }

    private suspend fun download(context: Context, project: String, pic: AlbumPage.Pic, f: File): File? {
        val url = urlOf(project, pic) ?: return null
        return try {
            var auth = withContext(Dispatchers.IO) { S5Auth.fresh(context) } ?: return null
            var a = AsIsUpload.fetchBytes(url, auth, MAX_IMG)
            if (a.code == 401 && S5Session.signedIn(context)) {
                // S5 no longer holds the session: signed in again once, then asked again (AlbumSync's rule)
                if (!withContext(Dispatchers.IO) { S5Session.relogin(context) }) return null
                auth = S5Auth.current(context) ?: return null
                a = AsIsUpload.fetchBytes(url, auth, MAX_IMG)
            }
            if (a.code == 404) {
                missing += f.name
                return null
            }
            val b = a.bytes
            if (a.code != 200 || b == null || b.size < 4 || b[0] != 0xFF.toByte() || b[1] != 0xD8.toByte() || b[2] != 0xFF.toByte()) {
                Log.w(TAG, "album image not kept: web answered ${a.code}") // text:value log line
                return null
            }
            withContext(Dispatchers.IO) {
                val d = dir(context).apply { mkdirs() }
                val tmp = File(d, f.name + AlbumPage.PART)
                // a write that throws (a full disk) takes its temp file with it, as PlanExport.keep does
                val renamed = try { tmp.writeBytes(b); tmp.renameTo(f) } catch (e: Exception) { tmp.delete(); throw e }
                if (!renamed) { tmp.delete(); return@withContext null }
                trim(d)
                f
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Never the URL's credentials: the message is the exception's own, cut short.
            Log.w(TAG, "album image not fetched: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
            null
        }
    }

    /**
     * The shot of the album photo [id] of [project] MOVED into this cache under the photo's own name, once S5 has its
     * bytes (AlbumOutbox's commit, after the outbox is applied: the K5 critic, finding 3) -- never copied, so the card
     * never downloads what the phone just sent; a cache that already holds the file keeps its own and the shot goes (the
     * same bytes: S5 writes a photo's JPEG once). Blocking; never throws.
     */
    fun adopt(context: Context, project: String, id: String) {
        try {
            val shot = AlbumShots.fileOf(context, id)
            if (!shot.isFile) return
            val f = fileOf(context, project, AlbumPage.Pic.AlbumJpeg(id)) ?: return
            val d = dir(context).apply { mkdirs() }
            if (f.isFile && f.length() > 0) { shot.delete(); return }
            if (!shot.renameTo(f)) {
                // a rename that cannot cross the two folders' filesystems: a copy in through a temp file, then the shot goes
                val tmp = File(d, f.name + AlbumPage.PART)
                try { shot.copyTo(tmp, overwrite = true); if (tmp.renameTo(f)) shot.delete() else tmp.delete() } catch (e: Exception) { tmp.delete(); throw e }
            }
            // drawn NOW, as hit() marks a fetched file (the K5 review, finding 6): a rename keeps the mtime the shot was
            // ENCODED at -- hours or days before it landed, for a shot taken offline -- and the trim reads mtime as the last
            // draw, so on a full cache the picture just adopted went first and the card downloaded the bytes just sent
            runCatching { f.setLastModified(System.currentTimeMillis()) }
            trim(d)
        } catch (e: Exception) {
            Log.w(TAG, "$id: shot not moved into the cache: " + (e.message?.take(160) ?: e.javaClass.simpleName)) // text:value log line
        }
    }

    /** The folder trimmed by AlbumPage.trimmed's one rule: the cap by mtime, and the temp files a minute old. */
    private fun trim(d: File) {
        val files = d.listFiles()?.filter { it.isFile } ?: return
        val gone = AlbumPage.trimmed(files.map { AlbumPage.CacheFile(it.name, it.length(), it.lastModified()) }, System.currentTimeMillis()).toHashSet()
        for (x in files) if (x.name in gone) x.delete()
    }

    /**
     * Every cached picture goes (S5Session.signOut through AlbumSync.forgetAll). The 404s are forgotten at once; the
     * files are deleted OFF the main thread -- signOut is called from the UI, and the folder may hold 256 MB, hundreds
     * of unlinks (the K3 review, finding 6) -- on a thread of its own, as signOut sends its logout. Never throws.
     */
    fun forgetAll(context: Context) {
        missing.clear()
        val d = dir(context)
        Thread { runCatching { d.listFiles()?.forEach { it.delete() } } }.start()
    }
}

/**
 * One album picture, drawn: the phone's own file for [AlbumPage.Pic.OnPhone] ([localPath]), else S5's bytes through the
 * cache ([AlbumImages.fetch]), decoded off the main thread at [target] px (Thumbs.file: 512 for a card, VIEWER_TARGET for
 * the viewer) and drawn CROP on a card or FIT in the viewer ([fit]). The plain block until it is there, and for
 * [AlbumPage.Pic.None] -- no spinner, Thumb's own rule. Leaving composition cancels the fetch.
 */
@Composable
fun AlbumImage(
    project: String,
    pic: AlbumPage.Pic,
    localPath: String?,
    fit: Boolean,
    background: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val target = if (fit) VIEWER_TARGET else 512
    val path = remember(project, pic, localPath) {
        if (pic is AlbumPage.Pic.OnPhone) localPath else AlbumImages.fileOf(context, project, pic)?.path
    }
    // Seeded from the decode cache, so a card drawn before does not flash the block when it scrolls back.
    var bmp by remember(path, target) { mutableStateOf<Bitmap?>(path?.let { Thumbs.cached("f:$it:$target") }) }
    LaunchedEffect(project, pic, localPath, target) {
        if (bmp != null || path == null) return@LaunchedEffect
        val file = if (pic is AlbumPage.Pic.OnPhone) localPath else AlbumImages.fetch(context, project, pic)?.path
        if (file != null) bmp = Thumbs.file(file, target)
    }
    Box(modifier.background(background)) {
        bmp?.let {
            Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(),
                contentScale = if (fit) ContentScale.Fit else ContentScale.Crop)
        }
    }
}
