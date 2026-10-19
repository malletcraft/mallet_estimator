package com.malletcrafts.sitephotos

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
import com.malletcrafts.sitephotos.pano.SurveyExport
import com.malletcrafts.sitephotos.pano.SurveyPlan
import com.malletcrafts.sitephotos.pano.SvgWriter
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
 *    no storage permission is asked for), replacing this app's earlier copy of the same name;
 *  - a private copy under filesDir/drawings/ is what Share hands out, through the app's FileProvider with a read grant
 *    (file_paths.xml "drawings") -- the same provider the update installer and the camera already use.
 * Nothing here runs on its own: the screen calls it when a button is pressed, never on save or sync.
 */
internal object PlanExport {

    private const val AUTHORITY = "com.malletcrafts.sitephotos.fileprovider"
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
            val c = page.canvas
            fun p(v: Double) = (v * PT).toFloat()
            c.drawColor(android.graphics.Color.WHITE)
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = argb(SurveyPlan.INK) }
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = android.graphics.Color.WHITE }
            val fr = SurveyExport.FRAME
            stroke.strokeWidth = p(0.5)
            c.drawRect(p(fr[0]), p(fr[1]), p(fr[0] + fr[2]), p(fr[1] + fr[3]), stroke)
            // the drawing: its mm at (tx, ty) + mm / scale on the sheet, in points
            paintItems(c, s.drawing.items, s.k * PT, s.tx * PT, s.ty * PT)
            // the title block over it, white, as the SVG has it
            val tb = SurveyExport.TITLE_BOX
            c.drawRect(p(tb[0]), p(tb[1]), p(tb[0] + tb[2]), p(tb[1] + tb[3]), fill)
            stroke.strokeWidth = p(0.4)
            c.drawRect(p(tb[0]), p(tb[1]), p(tb[0] + tb[2]), p(tb[1] + tb[3]), stroke)
            val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.LEFT }
            for (l in s.labels) {
                text.color = argb(l.colour); text.textSize = p(l.size)
                text.typeface = if (l.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                c.drawText(l.text, p(l.x), p(l.y), text)   // on its baseline, as the SVG's labels are
            }
            doc.finishPage(page)
            val out = ByteArrayOutputStream()
            doc.writeTo(out)
            return out.toByteArray()
        } finally {
            doc.close()
        }
    }

    private fun argb(rgb: Int): Int = (0xFF shl 24) or (rgb and 0xFFFFFF)

    /**
     * [bytes] saved as "<name>.<ext>": a private copy for Share, and the file in Downloads. Throws when Downloads will
     * not take it; the caller says so.
     */
    fun save(context: Context, name: String, ext: String, bytes: ByteArray): Saved {
        val display = "$name.$ext"; val mime = mimeOf(ext)
        val dir = File(context.filesDir, "drawings").apply { mkdirs() }
        val file = File(dir, display)
        file.writeBytes(bytes)
        return Saved(file, toDownloads(context, display, mime, bytes), mime)
    }

    /** Into Downloads through MediaStore: this app's earlier file of the same name removed first (one it cannot remove
     *  -- another install wrote it -- is left, and MediaStore names the new one beside it). The name it was given. */
    private fun toDownloads(context: Context, display: String, mime: String, bytes: ByteArray): String {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val rel = Environment.DIRECTORY_DOWNLOADS
        runCatching {
            resolver.delete(collection, "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
                arrayOf(display, "$rel/"))
        }
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
