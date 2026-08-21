package com.malletcrafts.sitephotos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Thumbnails, decoded on the device, from the files that are already there.
 *
 * A room full of captures is a visual thing and was being shown as a list of
 * dates — you had to open a capture to find out which wall it was. Every
 * image needed is already on the phone: the original 360 sits app-private for
 * the sync worker, and the six faces are in Pictures/MCFT Site Photos where
 * ImageMeter browses them. So this reads local bytes and never the network,
 * which also means the grid fills in a basement.
 *
 * No image library. Adding Coil for this would pull a dependency graph into a
 * build that has to stay small enough to sideload over a phone connection,
 * and what is needed here is one decode and one cache.
 */
object Thumbs {

    /** A quarter of the heap, in kilobytes. Bitmaps are the only thing in
     *  this app big enough to matter, and a grid of 360s will happily eat
     *  everything it is given. */
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 4).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    fun cached(key: String): Bitmap? = cache.get(key)

    /**
     * Decode at roughly [target] pixels on the long edge.
     *
     * inSampleSize only halves, so this asks for the smallest power of two
     * that still covers the target and lets the grid scale the rest. An 11K
     * pano decoded full-size is ~500 MB and takes the process with it.
     */
    private fun sample(w: Int, h: Int, target: Int): Int {
        var s = 1
        var big = maxOf(w, h)
        while (big / 2 >= target) { big /= 2; s *= 2 }
        return s
    }

    private fun decode(
        openBounds: () -> java.io.InputStream?,
        openFull: () -> java.io.InputStream?,
        target: Int,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openBounds()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample(bounds.outWidth, bounds.outHeight, target)
            inPreferredConfig = Bitmap.Config.RGB_565      // no alpha in a JPEG
        }
        return openFull()?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    suspend fun file(path: String, target: Int = 512): Bitmap? =
        withContext(Dispatchers.IO) {
            val key = "f:$path:$target"
            cache.get(key) ?: runCatching {
                val f = File(path)
                if (!f.exists()) return@runCatching null
                decode({ f.inputStream() }, { f.inputStream() }, target)
            }.getOrNull()?.also { cache.put(key, it) }
        }

    suspend fun uri(context: Context, uri: Uri, target: Int = 512): Bitmap? =
        withContext(Dispatchers.IO) {
            val key = "u:$uri:$target"
            cache.get(key) ?: runCatching {
                val r = context.contentResolver
                decode({ r.openInputStream(uri) }, { r.openInputStream(uri) }, target)
            }.getOrNull()?.also { cache.put(key, it) }
        }
}

/** What to draw: a local file, a MediaStore uri, or nothing yet. */
sealed interface ThumbSource {
    data class LocalFile(val path: String) : ThumbSource
    data class Content(val uri: Uri) : ThumbSource
}

/**
 * An image that decodes itself off the main thread and shows a plain block
 * while it works. Deliberately NOT a spinner per tile: a grid of twelve
 * spinners reads as an app in trouble, where twelve quiet placeholders read
 * as a grid still filling in.
 */
@Composable
fun Thumb(
    source: ThumbSource?,
    modifier: Modifier = Modifier,
    target: Int = 512,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val key = when (source) {
        is ThumbSource.LocalFile -> "f:${source.path}:$target"
        is ThumbSource.Content -> "u:${source.uri}:$target"
        null -> null
    }
    // Seeded from the cache so a tile that has been drawn before does not
    // flash a placeholder when it scrolls back into view.
    var bmp by remember(key) { mutableStateOf(key?.let { Thumbs.cached(it) }) }
    LaunchedEffect(key) {
        if (bmp != null || source == null) return@LaunchedEffect
        bmp = when (source) {
            is ThumbSource.LocalFile -> Thumbs.file(source.path, target)
            is ThumbSource.Content -> Thumbs.uri(context, source.uri, target)
        }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        bmp?.let {
            Image(it.asImageBitmap(), contentDescription,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}
