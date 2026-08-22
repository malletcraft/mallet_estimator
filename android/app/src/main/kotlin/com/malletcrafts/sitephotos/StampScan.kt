package com.malletcrafts.sitephotos

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.malletcrafts.sitephotos.pano.Stamp

/**
 * Finding an annotated photograph again by the mark this app burned into it.
 *
 * THE PROBLEM THIS SOLVES. ImageMeter renames what it exports —
 * MCAP-4b2014dcba5e_front.jpg comes back as image_from_19._Aug_2026.jpg —
 * which is why 88 returned files sat in a review inbox saying "no capture id
 * in the filename", and why matching on the name can never work. The picture
 * itself, though, comes back intact: ImageMeter draws ON TOP, so the caption
 * bar and its QR survive.
 *
 * Amit, 2026-08-21: "site foto app written footer is the key to identify the
 * foto and replace it with annotated image from gallery." Exactly — and it
 * removes the human step, which was the alternative and which he rightly
 * called error-prone.
 *
 * HOW IT STAYS CHEAP. A gallery holds thousands of pictures and decoding all
 * of them on every screen would be absurd, so three things narrow it:
 *
 *   1. Only images NEWER than the capture, and never our own folder — an
 *      annotation cannot predate the photograph it is drawn on.
 *   2. Only the BOTTOM STRIP is decoded. The stamp is always in the caption
 *      bar, so a crop of the last fifth of the image is both faster to
 *      decode and far less likely to hit somebody else's QR.
 *   3. Every answer is CACHED by MediaStore id, including "nothing here".
 *      Re-scanning a picture that has already been read and rejected is the
 *      cost that would otherwise grow forever.
 */
object StampScan {

    /** How many recent gallery images to consider in one pass. Generous
     *  enough for a day's annotating, bounded so a five-year-old gallery
     *  cannot stall the screen. */
    private const val WINDOW = 240

    /** Decode target for the strip crop. Big enough that a 108px QR is still
     *  several pixels per module, small enough to decode instantly. */
    private const val DECODE_WIDTH = 1000

    private fun cache(context: Context) =
        context.getSharedPreferences("stampscan", Context.MODE_PRIVATE)

    /**
     * Every stamped image the gallery can show us, as capture → face → uri.
     *
     * ONE pass, not one per capture: the sync worker asks about every capture
     * on the phone, and re-walking the cursor for each of them would be the
     * same work multiplied by the queue. A face we wrote ourselves is never a
     * candidate — ours live under Pictures/MCFT Site Photos, and the original
     * is not an annotation of itself.
     */
    /**
     * What one pass saw. Counts, not just answers — a scan that reports only
     * its findings cannot tell "looked at 240 pictures, none carried a mark"
     * apart from "never looked", and those need completely different advice.
     */
    data class Scan(val looked: Int, val stamped: Int,
                    val marks: Map<String, Map<String, Uri>>)

    fun allMarks(context: Context, notBefore: Long = 0L): Map<String, Map<String, Uri>> =
        scan(context, notBefore).marks

    fun scan(context: Context, notBefore: Long = 0L): Scan {
        val out = HashMap<String, HashMap<String, Uri>>()
        val seen = cache(context)
        val edit = seen.edit()
        var decoded = 0
        var looked = 0
        var stamped = 0
        runCatching {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.RELATIVE_PATH,
                        MediaStore.Images.Media.DATE_ADDED),
                null, null,
                // No LIMIT in the sort order. MediaStore parses this string
                // as SQL and rejects the token outright from Android 11 on
                // ("Invalid token LIMIT"), which would have turned the whole
                // scan into a silent nothing on every phone that matters.
                // The window is enforced by counting rows instead.
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            )?.use { c ->
                while (c.moveToNext()) {
                    if (looked >= WINDOW) break
                    val id = c.getLong(0)
                    val path = c.getString(1) ?: ""
                    // Ours by definition, and never an annotation of itself.
                    if (path.contains(OUR_FOLDER, ignoreCase = true)) continue
                    if (notBefore > 0 && c.getLong(2) < notBefore) continue
                    looked += 1

                    val key = "m$id"
                    val known = seen.getString(key, null)
                    val mark = if (known != null) {
                        // "" is a remembered MISS. Re-decoding a picture that
                        // has already been read and rejected is the cost that
                        // would otherwise grow with the gallery.
                        if (known.isEmpty()) null else Stamp.parse(known)
                    } else {
                        val uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        val text = readStamp(context, uri)
                        edit.putString(key, text ?: "")
                        decoded += 1
                        Stamp.parse(text)
                    }
                    if (mark == null) continue
                    stamped += 1
                    // Newest first, so the first answer for a face is the
                    // latest drawing of it and later rows must not overwrite.
                    val faces = out.getOrPut(mark.captureId) { HashMap() }
                    if (mark.face !in faces) {
                        faces[mark.face] = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    }
                }
            }
        }
        if (decoded > 0) edit.apply()
        return Scan(looked, stamped, out)
    }

    /** Every annotated face of ONE capture. Face → uri. */
    fun annotatedFor(context: Context, captureId: String,
                     notBefore: Long = 0L): Map<String, Uri> =
        allMarks(context, notBefore)[captureId] ?: emptyMap()

    /** What one pass would have to look at — for the drawer, so "found
     *  nothing" can be told apart from "never looked". */
    fun cachedMarks(context: Context): Int =
        cache(context).all.count { (_, v) -> (v as? String).orEmpty().isNotEmpty() }

    fun forget(context: Context) {
        cache(context).edit().clear().apply()
    }

    // ---- decoding --------------------------------------------------------

    /**
     * Read the stamp out of one image, or null.
     *
     * Only the bottom fifth is decoded: the stamp is always in the caption
     * bar, so cropping there is faster AND safer — a gallery is full of other
     * people's QR codes, and the ones on a parcel label are rarely at the
     * foot of a photograph of a wall. The magic string in the payload is what
     * makes it certain; this just makes it quick.
     */
    private fun readStamp(context: Context, uri: Uri): String? =
        decode(stripOf(context, uri))
            // The strip crop is an optimisation, not the contract. It assumes
            // the caption bar is still at the bottom of the raw pixels, and an
            // exporter is free to break that — rotate via EXIF (which region
            // decoding ignores), crop, letterbox, pad. When the cheap read
            // misses, look at the whole picture before believing there is
            // nothing there. It costs a second decode only on images that have
            // already failed, and the answer is cached either way.
            ?: decode(wholeOf(context, uri))

    private fun decode(bmp: Bitmap?): String? {
        if (bmp == null) return null
        return try {
            val w = bmp.width
            val h = bmp.height
            val px = IntArray(w * h)
            bmp.getPixels(px, 0, w, 0, 0, w, h)
            val source = RGBLuminanceSource(w, h, px)
            val reader = MultiFormatReader()
            reader.decode(
                BinaryBitmap(HybridBinarizer(source)),
                mapOf(DecodeHintType.TRY_HARDER to true),
            ).text
        } catch (e: Throwable) {
            // NotFoundException is the ordinary case — most pictures have no
            // stamp — so it is not worth a log line, and an OOM on a huge
            // image must not take the scan down with it.
            null
        } finally {
            bmp.recycle()
        }
    }

    /** The whole image, downsampled — the fallback when the strip misses. */
    private fun wholeOf(context: Context, uri: Uri): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val w = bounds.outWidth
        if (w <= 0) return null
        var sample = 1
        // Wider than the strip pass: a QR that is a small part of a whole
        // frame needs more pixels per module to survive the downsample.
        while (w / sample > DECODE_WIDTH * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }.getOrNull()

    /** The bottom fifth of an image, downsampled. */
    private fun stripOf(context: Context, uri: Uri): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w <= 0 || h <= 0) return null
        var sample = 1
        while (w / sample > DECODE_WIDTH) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }

        context.contentResolver.openInputStream(uri)!!.use { stream ->
            // Platform type, and genuinely nullable: newInstance returns null
            // for anything it cannot region-decode. A gallery has plenty of
            // those, so this is an ordinary miss, not an error.
            val decoder = android.graphics.BitmapRegionDecoder.newInstance(
                stream, false) ?: return@use null
            try {
                val top = (h * 0.78).toInt().coerceIn(0, h - 1)
                decoder.decodeRegion(
                    android.graphics.Rect(0, top, w, h), opts)
            } finally {
                decoder.recycle()
            }
        }
    }.getOrNull()

    private const val OUR_FOLDER = "MCFT Site Photos"
}
