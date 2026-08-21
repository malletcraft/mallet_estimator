package com.malletcrafts.sitephotos

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import com.malletcrafts.sitephotos.pano.Handover
import com.malletcrafts.sitephotos.pano.Panorama
import com.malletcrafts.sitephotos.pano.Stamp
import java.io.File

/**
 * From a picked 360 to six captioned faces in the gallery, entirely on the
 * device — a face that has to reach a server first is a face ImageMeter never
 * sees while anyone is still standing in the room.
 *
 * Faces go through MediaStore into Pictures/MCFT Site Photos/… because that
 * is what ImageMeter's importer browses (it picks from Google Photos, which
 * shows device folders as albums). The untouched original pano is kept
 * app-private for the sync worker to upload — the server re-splits it for the
 * ERPNext record, and the projection contract in CI is what guarantees those
 * faces agree with these.
 */
object FaceWriter {

    /** Decode target: enough source for a 1600px face at 110° (needs about
     *  face_px * 360/110 ≈ 5300px of pano width). An X3 shot passes through
     *  untouched; an 11K shot is halved instead of exhausting the heap. */
    private const val MAX_DECODE_WIDTH = 6500

    data class Result(val faceCount: Int, val relativePath: String)

    fun split(
        context: Context,
        source: Uri,
        deviceId: String,
        customerName: String,
        projectTitle: String,
        room: String,
        captureDate: String,
        stage: String,
        fov: Double,
        facePx: Int = Panorama.DEFAULT_FACE_PX,
        panoDir: File,
    ): Pair<Result, File> {
        val resolver = context.contentResolver

        // Keep the ORIGINAL bytes first: the server wants the real pano, not
        // our decode of it, and the picked Uri's read grant does not survive
        // process death — the sync worker runs later, maybe days later.
        panoDir.mkdirs()
        val panoFile = File(panoDir, "$deviceId.jpg")
        resolver.openInputStream(source).use { input ->
            requireNotNull(input) { "could not open the selected photo" }
            panoFile.outputStream().use { input.copyTo(it) }
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(panoFile.path, bounds)
        require(bounds.outWidth > 0) { "not a decodable image" }
        require(Panorama.looksEquirect(bounds.outWidth, bounds.outHeight)) {
            "not a 360 photo (${bounds.outWidth}×${bounds.outHeight} is not 2:1) — " +
                "export the 360 from the Insta360 app first"
        }

        var sample = 1
        while (bounds.outWidth / sample > MAX_DECODE_WIDTH) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeFile(panoFile.path, opts)
            ?: error("could not decode the selected photo")

        val pano = try {
            val px = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(px, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            for (i in px.indices) px[i] = px[i] and 0xFFFFFF
            Panorama.Image(bitmap.width, bitmap.height, px)
        } finally {
            bitmap.recycle()
        }

        val relPath = Handover.relativePath(customerName, projectTitle, room)
        var written = 0
        for ((face, yaw, pitch) in Panorama.FACES) {
            val img = Panorama.faceFromEquirect(pano, yaw, pitch, fov, facePx)
            val captioned = captioned(img, Handover.captionText(
                deviceId, room, face, captureDate, stage), deviceId, face)
            try {
                saveToGallery(context, captioned, relPath,
                    Handover.filename(deviceId, face))
                written += 1
            } finally {
                captioned.recycle()
            }
        }
        // The 360 itself belongs in the room's folder too, beside its faces.
        // Amit: "360 foto will be split in that folder itself retaining its
        // 360 foto at that folder level." The app-private copy above is what
        // the sync worker uploads; this one is for a person browsing the
        // folder, who should find the original next to what came out of it.
        runCatching {
            resolver.openInputStream(source).use { input ->
                requireNotNull(input)
                saveStreamToGallery(context, input, relPath, "$deviceId.jpg")
            }
        }
        return Result(written, relPath) to panoFile
    }

    /**
     * A FLAT photograph — one image, no split.
     *
     * A repair job is a close-up of a broken hinge, and a snag list is a
     * dozen of them. Putting those through the equirect splitter is nonsense,
     * and refusing to file them at all is why people fall back to the phone's
     * own camera app and lose the client, site, room and stage along with it.
     *
     * Everything else is identical to a face: the same gallery folder, the
     * same burned caption, the same MCAP-<id>_<face> naming — so ImageMeter
     * imports it the same way and the annotation comes home the same way.
     * The face token is "photo", which is not one of the six, which is
     * exactly what makes it recognisable as not-a-face downstream.
     */
    fun single(
        context: Context,
        source: Uri,
        deviceId: String,
        customerName: String,
        projectTitle: String,
        room: String,
        captureDate: String,
        stage: String,
        panoDir: File,
    ): Pair<Result, File> {
        val resolver = context.contentResolver
        // Kept app-private for the sync worker exactly as a pano is: the
        // picked Uri's read grant does not survive process death, and the
        // upload may be days away.
        panoDir.mkdirs()
        val file = File(panoDir, "$deviceId.jpg")
        resolver.openInputStream(source).use { input ->
            requireNotNull(input) { "could not open the selected photo" }
            file.outputStream().use { input.copyTo(it) }
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        require(bounds.outWidth > 0) { "not a decodable image" }
        var sample = 1
        while (bounds.outWidth / sample > MAX_DECODE_WIDTH) sample *= 2
        val bitmap = BitmapFactory.decodeFile(file.path,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }) ?: error("could not decode the photo")

        val relPath = Handover.relativePath(customerName, projectTitle, room)
        val captioned = captionedBitmap(bitmap, Handover.captionText(
            deviceId, room, PHOTO_FACE, captureDate, stage),
            deviceId, PHOTO_FACE)
        try {
            saveToGallery(context, captioned, relPath, "${deviceId}_$PHOTO_FACE.jpg")
        } finally {
            if (captioned !== bitmap) captioned.recycle()
            bitmap.recycle()
        }
        return Result(1, relPath) to file
    }

    /** The face token a flat photo carries. Deliberately not one of the six. */
    const val PHOTO_FACE = "photo"

    /** Copy bytes straight into the gallery folder without decoding them —
     *  an 11K pano must not be inflated into the heap just to be filed. */
    private fun saveStreamToGallery(
        context: Context,
        input: java.io.InputStream,
        relativePath: String,
        displayName: String,
    ): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        context.contentResolver.openOutputStream(uri)?.use { input.copyTo(it) }
        return uri
    }


    /**
     * The QR that lets a photograph name itself after ImageMeter has drawn on
     * it. Painted into the caption bar, at the right, on a white tile so it
     * reads against the dark strip.
     *
     * Sized off the strip and floored at MIN_STAMP_PX: a QR whose modules are
     * a pixel or two across does not survive a re-encode, and a stamp that
     * cannot be read is worse than no stamp because it looks like one.
     */
    private fun drawStamp(canvas: Canvas, captureId: String, face: String,
                          left: Float, top: Float, box: Int) {
        val m = runCatching {
            Stamp.matrix(Stamp.payload(captureId, face), box)
        }.getOrNull() ?: return
        val modules = m.size
        if (modules == 0) return
        val scale = box.toFloat() / modules
        val white = Paint().apply { color = Color.WHITE }
        canvas.drawRect(left, top, left + box, top + box, white)
        val dark = Paint().apply { color = Color.BLACK }
        for (y in 0 until modules) {
            for (x in 0 until modules) {
                if (!m[y][x]) continue
                canvas.drawRect(
                    left + x * scale, top + y * scale,
                    left + (x + 1) * scale, top + (y + 1) * scale, dark)
            }
        }
    }

    /** Below this a QR stops surviving JPEG, so the strip grows instead. */
    private const val MIN_STAMP_PX = 108

    /** The caption strip is ADDED BELOW the face, never painted over it —
     *  same rule as the server: annotation space is sacred. */
    private fun captioned(face: Panorama.Image, text: String,
                          captureId: String = "", faceName: String = ""): Bitmap {
        val stamped = captureId.isNotBlank() && faceName.isNotBlank()
        // The strip GROWS to fit a readable stamp rather than shrinking the
        // stamp to fit the strip. Six extra percent of one edge is cheap; an
        // unreadable QR costs the entire mechanism.
        val strip = (face.height * 0.052).toInt().coerceAtLeast(28)
            .let { if (stamped) maxOf(it, MIN_STAMP_PX + 12) else it }
        val out = Bitmap.createBitmap(face.width, face.height + strip,
            Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        val px = IntArray(face.pixels.size)
        for (i in px.indices) px[i] = -0x1000000 or face.pixels[i]   // opaque
        val faceBmp = Bitmap.createBitmap(px, face.width, face.height,
            Bitmap.Config.ARGB_8888)
        canvas.drawBitmap(faceBmp, 0f, 0f, null)
        faceBmp.recycle()

        val bar = Paint().apply { color = Color.rgb(17, 24, 31) }
        canvas.drawRect(0f, face.height.toFloat(), face.width.toFloat(),
            (face.height + strip).toFloat(), bar)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = strip * 0.62f
        }
        // Shrink to fit rather than clip: the id at the front is the part a
        // person needs, but a truncated room name helps nobody.
        while (paint.measureText(text) > face.width * 0.96f && paint.textSize > 8f) {
            paint.textSize -= 1f
        }
        val y = face.height + strip / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, face.width * 0.02f, y, paint)
        if (stamped) {
            val box = (strip * 0.82f).toInt().coerceAtLeast(MIN_STAMP_PX)
            drawStamp(canvas, captureId, faceName,
                face.width - box - strip * 0.09f,
                face.height + (strip - box) / 2f, box)
        }
        return out
    }

    /** The same strip, under a plain Bitmap rather than a projected face.
     *  Split from captioned() rather than shared through a common type: the
     *  face path owns a pixel array and this one owns a Bitmap, and the
     *  conversion between them would cost a full-size copy for nothing. */
    private fun captionedBitmap(src: Bitmap, text: String,
                                captureId: String = "", faceName: String = ""): Bitmap {
        val stamped = captureId.isNotBlank() && faceName.isNotBlank()
        val strip = (src.height * 0.052).toInt().coerceAtLeast(28)
            .let { if (stamped) maxOf(it, MIN_STAMP_PX + 12) else it }
        val out = Bitmap.createBitmap(src.width, src.height + strip,
            Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(src, 0f, 0f, null)

        val bar = Paint().apply { color = Color.rgb(17, 24, 31) }
        canvas.drawRect(0f, src.height.toFloat(), src.width.toFloat(),
            (src.height + strip).toFloat(), bar)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = strip * 0.62f
        }
        while (paint.measureText(text) > src.width * 0.96f && paint.textSize > 8f) {
            paint.textSize -= 1f
        }
        val y = src.height + strip / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, src.width * 0.02f, y, paint)
        if (stamped) {
            val box = (strip * 0.82f).toInt().coerceAtLeast(MIN_STAMP_PX)
            drawStamp(canvas, captureId, faceName,
                src.width - box - strip * 0.09f,
                src.height + (strip - box) / 2f, box)
        }
        return out
    }

    private fun saveToGallery(context: Context, bitmap: Bitmap,
                              relativePath: String, displayName: String) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath.trimEnd('/'))
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values)
            ?: error("gallery refused the file")
        try {
            resolver.openOutputStream(uri).use { out ->
                requireNotNull(out) { "could not write to the gallery" }
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }
}
