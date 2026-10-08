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
import com.malletcrafts.sitephotos.pano.RoomCorners
import com.malletcrafts.sitephotos.pano.WallCorners
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

    /** The pano kept in memory while the sliders move. 2048 wide is about
     *  8 MB as ARGB and resamples into a 512px face fast enough to feel
     *  immediate; the full decode happens once, at commit. */
    private const val PREVIEW_PANO_WIDTH = 2048

    data class Result(val faceCount: Int, val relativePath: String)

    /** The three groups a room's six faces fall into, because those are the
     *  three numbers the geometry actually produces. Amit, 2026-09-22: "can i
     *  get a slider for 3 side like height, length and width or front, left
     *  and bottom". Front and back are one wall pair seen across the width,
     *  left and right the other seen across the length, and floor and ceiling
     *  share the half-diagonal — so three sliders cover all six without ever
     *  letting a pair disagree with itself. */
    enum class Group(val label: String, val faces: List<String>) {
        FRONT_BACK("Front / back walls", listOf("front", "back")),
        LEFT_RIGHT("Left / right walls", listOf("left", "right")),
        FLOOR_CEILING("Ceiling / floor", listOf("up", "down"));

        companion object {
            fun of(face: String): Group =
                entries.first { face in it.faces }
        }
    }

    /**
     * A decoded panorama held open so faces can be re-rendered as a slider
     * moves, plus everything a commit needs.
     *
     * Amit, 2026-09-22: "FOV calculation is not accurate and i am missing 4
     * corners at some foto out of 6. can i get a slider for 3 side ... to
     * manually verify if i am getting correct fotos and then only let me
     * save."
     *
     * The computed plan is a good starting point and is not always right --
     * a room is not a perfect box, nobody stands exactly at its centre, and
     * the camera is not exactly at half height. So the geometry now proposes
     * and the person disposes, which is the only arrangement that can be
     * correct in a room the maths does not know about.
     *
     * THE PANO IS KEPT DECODED AT PREVIEW RESOLUTION, deliberately. Re-reading
     * and re-decoding a 6500px equirect on every slider tick would make the
     * slider useless; resampling a 2048px one into a 512px face is quick
     * enough to feel live. The full-resolution faces are rendered ONCE, at
     * commit, from the angles he settled on.
     */
    class Session(
        val panoFile: File,
        val relativePath: String,
        val source: Uri,
        val deviceId: String,
        val room: String,
        val captureDate: String,
        val stage: String,
        /** Preview-resolution copy, kept in memory for the slider. */
        val previewPano: Panorama.Image,
        /** Where each group started, from CaptureGeometry. Shown so he can
         *  see how far he has moved from what the room implies. */
        val proposed: Map<Group, Double>,
        val facePx: Int,
    ) {
        /** What the FOV sliders currently say. Starts at `proposed`. */
        var chosen: Map<Group, Double> = proposed

        /**
         * Yaw offset per group, in degrees. Zero unless he turns it.
         *
         * Amit, 2026-09-22: "if the camera is placed not perfectly square to
         * the wall, 6 foto is slightly rotated, can we have a per set tilt as
         * well so that foto can be aligned correctly?"
         *
         * One error, two appearances. A camera turned a few degrees off the
         * wall leaves each WALL off-CENTRE in its frame -- the far corner
         * crops while the near one has room to spare -- and leaves the FLOOR
         * and CEILING visibly ROTATED, because a yaw change at the pole spins
         * the image in its own plane. Turning the sampling angle fixes both,
         * and it is cheaper than asking anybody to stand more squarely.
         *
         * PER GROUP rather than one global turn, which is what he asked for
         * and is right for a reason beyond preference: a room whose corners
         * are not true 90 degrees -- an old Pune flat, a boxed-in duct -- needs
         * its wall pairs turned independently, and a single number cannot
         * square both pairs at once.
         *
         * WHAT THIS DOES NOT FIX, said now rather than discovered: a camera
         * that was not LEVEL tilts the horizon itself, which is roll, a
         * different axis this does not touch. The X3's own levelling normally
         * removes it on export.
         */
        var turn: Map<Group, Double> = Group.entries.associateWith { 0.0 }

        /**
         * The room's eight corners -- ceiling and floor end of each vertical
         * corner -- as directions in the pano. Every face's elevation is
         * built from four of them at commit. Amit, 2026-10-08: "corner
         * placement is very tedious on app": 24 drags became 8, because a
         * room has four corners and every face shares them.
         */
        var roomCorners: RoomCorners? = null

        /** The room the corners came from -- two opposite corners and four
         *  wall lines -- kept so it can be opened again where it was left.
         *  Amit, 2026-10-08: "i will set only two diagonally opposite
         *  corners of a room", SketchUp-style, square or not. */
        var roomQuad: com.malletcrafts.sitephotos.pano.RoomQuad? = null

        /** Floor to ceiling, as typed, in mm. The one size an elevation
         *  needs for its scale bar: the photo gives every proportion, this
         *  gives the millimetres. Null when it was not typed. */
        var heightMm: Double? = null

        fun turnFor(face: String): Double = turn[Group.of(face)] ?: 0.0

        fun fovFor(face: String): Double = chosen[Group.of(face)] ?: Panorama.DEFAULT_FOV

        /** Every face at full size, for the commit. */
        fun fovByFace(): Map<String, Double> =
            Panorama.FACES.associate { (name, _, _) -> name to fovFor(name) }
    }

    /** Cheap, small, and thrown away: one face at preview size and the
     *  current angle, for the grid the sliders drive. */
    fun previewFace(
        session: Session,
        face: String,
        /** Passed EXPLICITLY rather than read off the session: the caller is
         *  Compose, and reading mutable non-state from inside a composition is
         *  how a tile ends up drawn from a value nobody observed. */
        fovDeg: Double,
        /** Yaw offset in degrees, likewise explicit. */
        turnDeg: Double,
        previewPx: Int = 512,
    ): Bitmap {
        val (_, yaw, pitch) = Panorama.FACES.first { it.first == face }
        val img = Panorama.faceFromEquirect(
            session.previewPano, yaw + turnDeg, pitch,
            Panorama.clampFov(fovDeg), previewPx)
        val out = Bitmap.createBitmap(img.width, img.height, Bitmap.Config.ARGB_8888)
        val px = IntArray(img.pixels.size)
        for (i in px.indices) px[i] = img.pixels[i] or (0xFF shl 24)
        out.setPixels(px, 0, img.width, 0, 0, img.width, img.height)
        return out
    }

    /** Any view of the pano, for the corner screen: aimed at one room corner
     *  rather than at one of the six faces. */
    fun previewView(session: Session, yawDeg: Double, pitchDeg: Double, fovDeg: Double, px: Int = 640): Bitmap =
        toBitmap(Panorama.faceFromEquirect(session.previewPano, yawDeg, pitchDeg, fovDeg, px))

    /** Border beyond the corners on every elevation, as a fraction of its
     *  side -- wide enough to carry the scale bar outside the wall. */
    const val ELEV_MARGIN = 0.06


    private fun toBitmap(img: Panorama.Image): Bitmap {
        val out = Bitmap.createBitmap(img.width, img.height, Bitmap.Config.ARGB_8888)
        val px = IntArray(img.pixels.size)
        for (i in px.indices) px[i] = img.pixels[i] or (0xFF shl 24)
        out.setPixels(px, 0, img.width, 0, 0, img.width, img.height)
        return out
    }

    /**
     * The bar printed on an elevation for ImageMeter's reference scale.
     *
     * Amit, 2026-10-08: ImageMeter's Reference Scale takes ONE known length on
     * a straight-on photo -- which an elevation is. The bar is that length,
     * drawn exactly from the wall's floor line to its ceiling line, so the
     * reference is laid on a printed mark instead of guessed at an edge.
     * Walls: vertical, the typed height. Ceiling and floor: horizontal along
     * the top edge, the room length the WALLS' photos give for that height --
     * labelled as from the photo, because it is measured, not typed.
     */
    class ScaleBar(val vertical: Boolean, val mm: Double, val fromPhoto: Boolean)

    fun scaleBarFor(face: String, rc: RoomCorners, heightMm: Double): ScaleBar? =
        if (face in WallCorners.WALL_FACES) ScaleBar(true, heightMm, false)
        else {
            val f = WallCorners.measure(rc.forFace("front"))
            val b = WallCorners.measure(rc.forFace("back"))
            if (f.valid && b.valid) ScaleBar(false, (f.ratio + b.ratio) / 2 * heightMm, true) else null
        }

    private fun drawScaleBar(bmp: Bitmap, bar: ScaleBar) {
        val c = Canvas(bmp)
        val w = bmp.width.toFloat(); val h = bmp.height.toFloat()
        val span = 1f + 2f * ELEV_MARGIN.toFloat()
        val m = ELEV_MARGIN.toFloat()
        val x0 = w * m / span; val x1 = w * (1f + m) / span
        val y0 = h * m / span; val y1 = h * (1f + m) / span
        val stroke = maxOf(3f, w / 400f)
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 210, 63); strokeWidth = stroke; style = Paint.Style.STROKE
        }
        val halo = Paint(ink).apply { color = Color.BLACK; strokeWidth = stroke * 2.5f }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 210, 63); textSize = maxOf(18f, minOf(x0, y0) * 0.45f)
            textAlign = Paint.Align.CENTER
        }
        val textHalo = Paint(text).apply {
            color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = text.textSize / 6f
        }
        val label = (if (bar.fromPhoto) "\u2248 " else "") + "${Math.round(bar.mm)} mm" +
            (if (bar.fromPhoto) " (from photo)" else "")
        val tick = stroke * 4f
        fun line(ax: Float, ay: Float, bx: Float, by: Float) {
            c.drawLine(ax, ay, bx, by, halo); c.drawLine(ax, ay, bx, by, ink)
        }
        if (bar.vertical) {
            val x = x0 * 0.55f
            line(x, y0, x, y1)
            line(x - tick, y0, x + tick, y0); line(x - tick, y1, x + tick, y1)
            var k = 500.0
            while (k < bar.mm) {           // a short tick every 500 mm up from the floor
                val y = y1 - ((k / bar.mm) * (y1 - y0)).toFloat()
                line(x - tick / 2, y, x + tick / 2, y); k += 500.0
            }
            c.save(); c.rotate(-90f, x0 * 0.2f, (y0 + y1) / 2)
            c.drawText(label, x0 * 0.2f, (y0 + y1) / 2 + text.textSize / 3, textHalo)
            c.drawText(label, x0 * 0.2f, (y0 + y1) / 2 + text.textSize / 3, text)
            c.restore()
        } else {
            val y = y0 * 0.6f
            line(x0, y, x1, y)
            line(x0, y - tick, x0, y + tick); line(x1, y - tick, x1, y + tick)
            c.drawText(label, (x0 + x1) / 2, y - tick * 1.2f, textHalo)
            c.drawText(label, (x0 + x1) / 2, y - tick * 1.2f, text)
        }
    }

    /** The filename an elevation is filed under, beside the six faces. NOT a
     *  face token: faceOfToken() does not know it, so nothing that reads faces
     *  back mistakes an elevation for the wall photo it was made from. */
    fun elevationFilename(captureId: String, face: String): String =
        "${captureId}_${Handover.FACE_LABELS[face] ?: face}-elevation.jpg"

    /**
     * Open a 360 for inspection. Copies the original, decodes it once at
     * preview resolution, and touches NEITHER the gallery nor the queue --
     * abandoning it costs nothing.
     */
    fun begin(
        context: Context,
        source: Uri,
        deviceId: String,
        customerName: String,
        projectTitle: String,
        room: String,
        captureDate: String,
        stage: String,
        fov: Double,
        fovByFace: Map<String, Double> = emptyMap(),
        facePx: Int = Panorama.DEFAULT_FACE_PX,
        panoDir: File,
    ): Session {
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
            "not a 360 photo (${bounds.outWidth}\u00d7${bounds.outHeight} is not 2:1) \u2014 " +
                "export the 360 from the Insta360 app first"
        }

        val previewPano = decodePano(panoFile, PREVIEW_PANO_WIDTH)
        val proposed = Group.entries.associateWith { g ->
            // The group's own proposal, or the single fallback when the room
            // was never measured.
            fovByFace[g.faces.first()] ?: fov
        }
        return Session(
            panoFile = panoFile,
            relativePath = Handover.relativePath(customerName, projectTitle, room),
            source = source, deviceId = deviceId, room = room,
            captureDate = captureDate, stage = stage,
            previewPano = previewPano, proposed = proposed, facePx = facePx,
        ).also { it.chosen = proposed }
    }

    /** Decode the pano down to at most `maxWidth`, as packed 0xRRGGBB. */
    private fun decodePano(file: File, maxWidth: Int): Panorama.Image {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxWidth) sample *= 2
        val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }) ?: error("could not decode the selected photo")
        return try {
            val px = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(px, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            for (i in px.indices) px[i] = px[i] and 0xFFFFFF
            Panorama.Image(bitmap.width, bitmap.height, px)
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Confirmed: render the six faces at FULL size from the angles he settled
     * on, and put them and the original 360 into device photos.
     *
     * The preview he approved was the same FRAMING at lower resolution -- the
     * thing being judged is whether the corners are inside the frame, and that
     * is identical. Rendering full size here rather than keeping six 1600px
     * bitmaps alive through a slider session is the trade, and it is the right
     * way round: the cheap thing happens 50 times, the expensive one once.
     */
    fun commit(context: Context, session: Session): Result {
        val full = decodePano(session.panoFile, MAX_DECODE_WIDTH)
        var written = 0
        for ((face, yaw, pitch) in Panorama.FACES) {
            val img = Panorama.faceFromEquirect(
                full, yaw + session.turnFor(face), pitch,
                Panorama.clampFov(session.fovFor(face)), session.facePx)
            val captioned = captioned(img, Handover.captionText(
                session.deviceId, session.room, face,
                session.captureDate, session.stage), session.deviceId, face)
            try {
                saveToGallery(context, captioned, session.relativePath,
                    Handover.filename(session.deviceId, face))
                written += 1
            } finally {
                captioned.recycle()
            }
        }
        // Elevations, all six, from the room's eight corners. Unstamped on
        // purpose: the stamp says "this is face X of capture Y", and an
        // elevation is not that face -- a scan that matched it to the face
        // would pair an annotation with the wrong picture.
        session.roomCorners?.let { rc ->
            for ((face, _, _) in Panorama.FACES) {
                val c = rc.forFace(face)
                if (!WallCorners.measure(c).valid) continue
                val bmp = toBitmap(WallCorners.elevation(full, c, session.facePx, ELEV_MARGIN))
                session.heightMm?.let { h -> scaleBarFor(face, rc, h)?.let { drawScaleBar(bmp, it) } }
                val captioned = captionedBitmap(bmp, Handover.captionText(
                    session.deviceId, session.room, "${Handover.FACE_LABELS[face] ?: face} elevation",
                    session.captureDate, session.stage))
                try {
                    saveToGallery(context, captioned, session.relativePath,
                        elevationFilename(session.deviceId, face))
                    written += 1
                } finally {
                    captioned.recycle()
                    bmp.recycle()
                }
            }
        }
        // The 360 itself belongs in the room's folder too, beside its faces.
        // Amit: "360 foto will be split in that folder itself retaining its
        // 360 foto at that folder level." The app-private copy is what the
        // sync worker uploads; this one is for a person browsing the folder,
        // who should find the original next to what came out of it.
        runCatching {
            context.contentResolver.openInputStream(session.source).use { input ->
                requireNotNull(input)
                saveStreamToGallery(context, input, session.relativePath,
                    session.panoFile.name)
            }
        }
        return Result(written, session.relativePath)
    }

    /**
     * Rejected: leave no trace. The gallery was never touched, so this is the
     * app-private pano -- and it matters, because a kept one with no queue row
     * is a file nothing will ever clean up.
     */
    fun discard(session: Session) {
        runCatching { session.panoFile.delete() }
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
        // The room left of the stamp, not the whole bar. The QR is drawn
        // last on an opaque tile, so a caption that ran under it would be
        // silently beheaded rather than shrunk.
        val textRoom = face.width * 0.96f -
            (if (stamped) (strip * 0.82f).coerceAtLeast(MIN_STAMP_PX.toFloat()) +
                strip * 0.18f else 0f)
        while (paint.measureText(text) > textRoom && paint.textSize > 8f) {
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
        // The room left of the stamp, not the whole bar. The QR is drawn
        // last on an opaque tile, so a caption that ran under it would be
        // silently beheaded rather than shrunk.
        val textRoom = src.width * 0.96f -
            (if (stamped) (strip * 0.82f).coerceAtLeast(MIN_STAMP_PX.toFloat()) +
                strip * 0.18f else 0f)
        while (paint.measureText(text) > textRoom && paint.textSize > 8f) {
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
