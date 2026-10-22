package com.malletcrafts.interiorestimate

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.malletcrafts.interiorestimate.pano.SurveyExport
import com.malletcrafts.interiorestimate.pano.SurveyPlan
import com.malletcrafts.interiorestimate.pano.SvgWriter
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * THE PLAN TAB'S EXPORT (v52 slice 14a, item 10): one drawing on its A4 sheet, saved to Downloads as SVG or PDF, and
 * shared. Amit, 14 Oct 2026: "like plan i need elevation fo all walls and ceiling plan as well." -- item 10: "Export puts
 * the chosen drawing on its own A4 sheet at 1:50, and the title block names it".
 *
 * Everything on the sheet -- the scale, where the drawing sits, the title block, the footnotes, the file name -- is
 * SurveyExport's in :pano, where it is tested; the SVG is SvgWriter's. This file only turns a sheet into bytes and puts
 * them where the phone keeps files:
 *  - the PDF is painted by android.graphics.pdf.PdfDocument from the SAME sheet, the drawing through paintItems -- the
 *    one painter the screen uses -- at the sheet's scale, in points (A4 landscape, 842 × 595 pt);
 *  - each file goes to Downloads through MediaStore (the Downloads collection; minSdk is 29, so it is always there and
 *    no storage permission is asked for). NOTHING IN DOWNLOADS IS EVER DELETED OR REPLACED (slice 14a review): two
 *    drawings can share a name -- "Master Bedroom" and "Master Bathroom" are both MB -- so a second save of a name is
 *    named beside the first by MediaStore ("… (1).pdf"), and the snackbar says the name it got;
 *  - a private copy under filesDir/drawings/<capture>/ is what Share hands out, through the app's FileProvider with a
 *    read grant (file_paths.xml "drawings") -- the same provider the update installer and the camera already use. It is
 *    written to a ".part" file and renamed only once whole, so a failed write never leaves a half file to be shared.
 * Nothing here runs on its own: the screen calls it when a button is pressed, never on save or sync.
 */
internal object PlanExport {

    private const val AUTHORITY = "com.malletcrafts.interiorestimate.fileprovider"
    /** PostScript points per mm: PdfDocument's page is measured in points. */
    private const val PT = 72.0 / 25.4
    /** A4 landscape, points. */
    private const val PAGE_W = 842
    private const val PAGE_H = 595

    const val SVG = "svg"
    const val PDF = "pdf"

    fun mimeOf(ext: String): String = if (ext == SVG) "image/svg+xml" else "application/pdf"

    /** A file saved: the private copy Share hands out, and the name Downloads gave it (MediaStore may add " (1)"). */
    class Saved(val file: File, val shown: String, val mime: String)

    /** The sheet as an SVG file, UTF-8. */
    fun svgBytes(s: SurveyExport.Sheet): ByteArray = SvgWriter.write(s).toByteArray(Charsets.UTF_8)

    /** The sheet as a one-page PDF: the border, the drawing at its sheet scale, the title block and the footnotes. */
    fun pdfBytes(s: SurveyExport.Sheet): ByteArray {
        val doc = PdfDocument()
        try {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
            paintSheet(page.canvas, s, PT, 0.0, 0.0)
            doc.finishPage(page)
            val out = ByteArrayOutputStream()
            doc.writeTo(out)
            return out.toByteArray()
        } finally {
            doc.close()
        }
    }

    /**
     * The whole sheet onto a canvas at `canvas = (ox, oy) + sheet mm × k`: the white A4, its border, the drawing at its
     * sheet scale (through paintItems, the screen's own painter), the title block and the footnotes. The PDF page (k =
     * points per mm) and the export overlay's preview of the sheet (k = px per mm) both paint through this ONE function,
     * so what the phone shows before a save is the file it saves.
     */
    fun paintSheet(c: android.graphics.Canvas, s: SurveyExport.Sheet, k: Double, ox: Double, oy: Double) {
        if (!(k > 0) || k.isInfinite()) return
        fun x(v: Double) = (ox + v * k).toFloat()
        fun y(v: Double) = (oy + v * k).toFloat()
        fun len(v: Double) = (v * k).toFloat()
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = argb(SurveyPlan.INK) }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = android.graphics.Color.WHITE }
        c.drawRect(x(0.0), y(0.0), x(SurveyExport.SHEET_W), y(SurveyExport.SHEET_H), fill)
        val fr = SurveyExport.FRAME
        stroke.strokeWidth = len(0.5)
        c.drawRect(x(fr[0]), y(fr[1]), x(fr[0] + fr[2]), y(fr[1] + fr[3]), stroke)
        // the drawing: its mm at (tx, ty) + mm / scale on the sheet
        paintItems(c, s.drawing.items, s.k * k, ox + s.tx * k, oy + s.ty * k)
        // the title block over it, white, as the SVG has it
        val tb = SurveyExport.TITLE_BOX
        c.drawRect(x(tb[0]), y(tb[1]), x(tb[0] + tb[2]), y(tb[1] + tb[3]), fill)
        stroke.strokeWidth = len(0.4)
        c.drawRect(x(tb[0]), y(tb[1]), x(tb[0] + tb[2]), y(tb[1] + tb[3]), stroke)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.LEFT }
        for (l in s.labels) {
            text.color = argb(l.colour); text.textSize = len(l.size)
            text.typeface = if (l.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            c.drawText(l.text, x(l.x), y(l.y), text)   // on its baseline, as the SVG's labels are
        }
    }

    private fun argb(rgb: Int): Int = (0xFF shl 24) or (rgb and 0xFFFFFF)

    /**
     * [bytes] saved as "<name>.<ext>": a private copy for Share (in capture [deviceId]'s own folder), and the file in
     * Downloads. Throws when either will not take it; the caller says so.
     */
    fun save(context: Context, deviceId: String, name: String, ext: String, bytes: ByteArray): Saved {
        val display = "$name.$ext"; val mime = mimeOf(ext)
        val file = keep(context, deviceId, display, bytes)
        return Saved(file, toDownloads(context, display, mime, bytes), mime)
    }

    /** [bytes] as the private copy only, for Share: nothing goes to Downloads, so sharing never piles up "(1)" files. */
    fun keepOnly(context: Context, deviceId: String, name: String, ext: String, bytes: ByteArray): Saved {
        val display = "$name.$ext"
        return Saved(keep(context, deviceId, display, bytes), display, mimeOf(ext))
    }

    /** The private copy: written whole to "<display>.part", then renamed over "<display>" -- a write that fails leaves
     *  the earlier copy (or none) and never a truncated one. */
    private fun keep(context: Context, deviceId: String, display: String, bytes: ByteArray): File {
        val dir = File(File(context.filesDir, "drawings"), SurveyExport.keepDir(deviceId))
        if (!dir.isDirectory && !dir.mkdirs()) error("could not make a folder for $display")
        val file = File(dir, display); val part = File(dir, "$display.part")
        try {
            part.writeBytes(bytes)
            if (!part.renameTo(file)) error("could not keep $display")
        } catch (e: Exception) {
            part.delete()
            throw e
        }
        return file
    }

    /** Into Downloads through MediaStore, never replacing anything there: a name already taken is named beside it by
     *  MediaStore ("… (1).pdf"). The name it was given. A write that fails removes only its own half-written entry. */
    private fun toDownloads(context: Context, display: String, mime: String, bytes: ByteArray): String {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val rel = Environment.DIRECTORY_DOWNLOADS
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, display)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, rel)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: error("Downloads refused $display")
        try {
            resolver.openOutputStream(uri).use { out ->
                requireNotNull(out) { "could not write $display to Downloads" }
                out.write(bytes)
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
        return runCatching {
            resolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: display
    }

    /** The share sheet for a saved file: ACTION_SEND with its FileProvider content URI and a read grant. */
    fun share(context: Context, saved: Saved) {
        val uri = FileProvider.getUriForFile(context, AUTHORITY, saved.file)
        val send = Intent(Intent.ACTION_SEND).setType(saved.mime)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, saved.file.name)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // the grant rides the ClipData through the chooser to whichever app is picked
        send.clipData = ClipData.newRawUri(saved.file.name, uri)
        context.startActivity(Intent.createChooser(send, "Share ${saved.file.name}")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }
}
