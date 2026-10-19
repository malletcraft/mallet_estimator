package com.malletcrafts.sitephotos.pano

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

/**
 * EACH OF THE SIX DRAWINGS ON ITS OWN A4 SHEET, SAVED AS SVG OR PDF (v52 slice 14a, item 10). Amit, 14 Oct 2026 06:32
 * IST: "like plan i need elevation fo all walls and ceiling plan as well." -- and item 10 says what the export is: "Export
 * puts the chosen drawing on its own A4 sheet at 1:50, and the title block names it ("Front wall elevation"). The Export
 * bar reads "Export · AP_LR_htu_front-elevation": the file name already says which drawing it is, and it fits 360 dp."
 *
 * PORTED FROM plan.js's #planexp handler (v36build/plan.js 221-234) and held to ct52 DR's export assertions: the sheet is
 * A4 landscape (297 × 210 mm) with a 277 × 162 mm drawing area; the drawing goes on it at 1:50, or at the first coarser
 * 1:10n that fits ([sheetScale]); the title block's first row is "Drawing" and the drawing's full name; the two lines
 * under the drawing are "<DTITLE> from survey prep · laser sizes" and "≈ = from the photo, not yet measured · MCFT S2";
 * the file is named by [exportName] and ends _plan, _ceiling-plan or _<face>-elevation, never _plan_plan.
 *
 * Everything on the sheet is decided HERE, so the SVG ([SvgWriter]) and the phone's PDF (painted from the same [Sheet])
 * cannot disagree, and every word on it is tested on the JVM.
 *
 * NO CLIENT NAME, PHONE OR ADDRESS EVER ENTERS A FILE NAME OR THE TITLE BLOCK (the v53 decision: initials, project code
 * and room names only). The caller hands over the client's INITIALS -- never the name -- and the project's code.
 */
object SurveyExport {

    /** A4 landscape, mm. */
    const val SHEET_W = 297.0
    const val SHEET_H = 210.0
    /** The drawing area inside the 10 mm margin: 277 × 162 mm (plan.js: 297 − 20, 210 − 48). */
    const val MARGIN = 10.0
    const val AREA_W = 277.0
    const val AREA_H = 162.0
    /** The scale every drawing goes on the sheet at when it fits: 1:50. */
    const val SCALE = 50
    /** The export's text unit: the drawing's longer side over this (plan.js: fs = max(drawDims) / 30, a little larger
     *  than the screen's / 34, so the figures read on paper). */
    const val FS_DIV = 30.0

    /** The sheet's border, and the title block's box (x, y, width, height, mm). */
    val FRAME = doubleArrayOf(5.0, 5.0, 287.0, 200.0)
    val TITLE_BOX = doubleArrayOf(166.0, 166.0, 126.0, 39.0)

    /** The title block's and the footnotes' grey (plan.js #6b6f66). */
    const val MUTED = 0x6b6f66

    /** The second footnote, word for word (plan.js, ct52 DR). */
    const val FOOT_ESTIMATE = "≈ = from the photo, not yet measured · MCFT S2"

    /** The first footnote: "<DTITLE> from survey prep · laser sizes". */
    fun footSource(view: String): String = "${SurveyPlan.DTITLE.getValue(view)} from survey prep · laser sizes"

    /** What each drawing's file name ends in (plan.js: floor → _plan, else '_' + DLAB with its space a '-'). */
    val SUFFIX: Map<String, String> = mapOf("floor" to "_plan", "ceil" to "_ceiling-plan", "front" to "_front-elevation",
        "right" to "_right-elevation", "back" to "_back-elevation", "left" to "_left-elevation")

    /**
     * The sheet scale, 1:n, for a drawing [w] × [h] mm (its box): 1:50 when it fits the 277 × 162 mm area at 1:50, else
     * the first coarser 1:10n that does (plan.js: ceil(max(w / 277, h / 162) / 10) × 10).
     */
    fun sheetScale(w: Double, h: Double): Int {
        if (w / SCALE <= AREA_W && h / SCALE <= AREA_H) return SCALE
        return (ceil(max(w / AREA_W, h / AREA_H) / 10.0) * 10).toInt()
    }

    private val SPACES = Regex("\\s+")
    private val NOT_SAFE = Regex("[^A-Za-z0-9_-]+")
    private val EDGES = Regex("^[-_]+|[-_]+$")
    /** A name part is kept short enough that the whole name stays well inside a file system's 255 bytes. */
    private const val PART_MAX = 40

    /** One part of a file name: spaces to '_', anything but ASCII letters, digits, '-' and '_' to '-', no '-' or '_' at
     *  either end. */
    fun safePart(s: String?): String =
        (s ?: "").trim().replace(SPACES, "_").replace(NOT_SAFE, "-").replace(EDGES, "").take(PART_MAX).replace(EDGES, "")

    /**
     * The file name of [drawing] (one of SurveyPlan.VIEWS), without its extension: the client's INITIALS, the room's
     * title (its token, "LR") and the project's code joined by '_', each part made safe ([safePart]), the empty ones
     * left out -- "room" when nothing is left -- and then the drawing's [SUFFIX]: "AP_LR_htu_front-elevation",
     * "AP_LR_htu_plan". Never empty, never ending _plan_plan (a suffix already at the end of the parts is not doubled).
     */
    fun exportName(clientInitials: String?, roomTitle: String?, project: String?, drawing: String): String {
        val suf = SUFFIX[drawing] ?: throw IllegalArgumentException("not a drawing: $drawing")
        var base = listOf(clientInitials, roomTitle, project).map { safePart(it) }.filter { it.isNotEmpty() }.joinToString("_")
        if (base.isEmpty()) base = "room"
        while (base.length > suf.length && base.endsWith(suf, ignoreCase = true)) base = base.dropLast(suf.length)
        return base + suf
    }

    /** The room's title for the file name: its token, what comes before " · " in the screen's title ("LR · Living Room"
     *  → "LR"; plan.js: spTitle.split(' · ')[0]). */
    fun roomToken(title: String): String = title.substringBefore(" · ").trim()

    private val DRAWN = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

    /** The day it was drawn, as the title block says it: "19 Oct 2026" (plan.js: en-GB, 2-digit day, short month). */
    fun drawn(date: LocalDate): String = DRAWN.format(date)

    /** What the title block says besides the drawing: initials, project code, the room's title, the capture's date, and
     *  the day it was drawn ([drawn]). */
    class Meta(val initials: String, val project: String, val room: String, val captured: String, val drawn: String)

    /** A word on the sheet itself (not the drawing), sheet mm: on its baseline at ([x], [y]), [size] mm high. */
    class Label(val x: Double, val y: Double, val text: String, val colour: Int, val size: Double, val bold: Boolean, val role: String)

    /**
     * One drawing on its A4 sheet. The drawing's own mm land on the sheet at `sheet = (tx, ty) + mm / scale`: its box
     * centred in the drawing area. [labels]: the title block and the two footnotes.
     */
    class Sheet(val drawing: SurveyPlan.Drawing, val view: String, val name: String, val scale: Int,
                val tx: Double, val ty: Double, val labels: List<Label>) {
        /** Sheet mm per drawing mm. */
        val k: Double get() = 1.0 / scale
        /** The title block as (label, value) rows, top to bottom. */
        fun rows(): List<Pair<String, String>> {
            val ls = labels.filter { it.role == "tb.k" || it.role == "tb.v" }
            return ls.filter { it.role == "tb.k" }.map { k -> k.text to (ls.firstOrNull { it.role == "tb.v" && it.y == k.y }?.text ?: "") }
        }
        fun footnotes(): List<String> = labels.filter { it.role == "foot" }.map { it.text }
    }

    /** A title-block value, kept inside its box (102 mm at 3.6 mm bold). */
    private fun fitValue(v: String): String = if (v.length <= 44) v else v.take(43).trimEnd() + "…"

    /**
     * Drawing [view] of [prep] at the room size [l] × [w] × [h] mm on its A4 sheet, named for [meta] (initials, project
     * code, room title) -- plan.js's #planexp: the drawing at the export's text unit, the sheet scale from its box, the box
     * centred in the drawing area, the title block and the two footnotes.
     */
    fun sheet(view: String, prep: SurveyPrep.Prep, l: Double, w: Double, h: Double, meta: Meta, roomName: String? = null): Sheet {
        val dd = SurveyPlan.dims(view, prep, l, w, h)
        val d = SurveyPlan.drawing(view, prep, l, w, h, roomName, max(dd[0], dd[1]) / FS_DIV)
        val b = d.box; val sc = sheetScale(b[2], b[3])
        val ox = MARGIN + (AREA_W - b[2] / sc) / 2; val oy = MARGIN + (AREA_H - b[3] / sc) / 2
        val ls = mutableListOf<Label>()
        fun row(k: String, v: String, y: Double) {
            ls.add(Label(170.0, y, k, MUTED, 3.2, false, "tb.k"))
            ls.add(Label(190.0, y, fitValue(v.ifBlank { "—" }), SurveyPlan.INK, 3.6, true, "tb.v"))
        }
        row("Drawing", SurveyPlan.DTITLE.getValue(view), 171.5)
        row("Client", meta.initials, 177.0)
        row("Project", meta.project, 182.5)
        row("Room", meta.room, 188.0)
        row("Captured", meta.captured, 193.5)
        row("Scale", "1:$sc on A4 · drawn ${meta.drawn}", 199.0)
        ls.add(Label(10.0, 196.0, footSource(view), MUTED, 3.0, false, "foot"))
        ls.add(Label(10.0, 201.0, FOOT_ESTIMATE, MUTED, 3.0, false, "foot"))
        return Sheet(d, view, exportName(meta.initials, roomToken(meta.room), meta.project, view), sc,
            ox - b[0] / sc, oy - b[1] / sc, ls)
    }
}

/**
 * A [SurveyExport.Sheet] as an SVG file: viewBox in mm (0 0 297 210, A4 landscape, width 297mm × height 210mm so a viewer
 * prints it at size), the drawing in a group translated and scaled to its sheet scale -- every display-list item ONE
 * element in it, in the inks it has on the screen -- then the title block and the footnotes. plan.js's own markup, as
 * far as a file needs it.
 */
object SvgWriter {

    fun write(s: SurveyExport.Sheet): String {
        val o = StringBuilder(16_384)
        o.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        o.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"297mm\" height=\"210mm\" viewBox=\"0 0 297 210\" data-sheet=\"A4\"")
            .append(" font-family=\"Roboto, Arial, sans-serif\">\n")
        o.append("<title>").append(esc(s.name)).append("</title>\n")
        o.append("<rect x=\"0\" y=\"0\" width=\"297\" height=\"210\" fill=\"#ffffff\"/>\n")
        rect(o, SurveyExport.FRAME, "none", 0.5)
        o.append("<g transform=\"translate(").append(f(s.tx)).append(' ').append(f(s.ty)).append(") scale(")
            .append(fine(s.k)).append(")\" data-plan=\"1\" data-view=\"").append(esc(s.view)).append("\">\n")
        for (it in s.drawing.items) { item(o, it); o.append('\n') }
        o.append("</g>\n")
        rect(o, SurveyExport.TITLE_BOX, "#ffffff", 0.4)
        for (l in s.labels) {
            o.append("<text x=\"").append(f(l.x)).append("\" y=\"").append(f(l.y)).append("\" font-size=\"").append(f(l.size))
                .append("\" fill=\"").append(hex(l.colour)).append('"')
            if (l.bold) o.append(" font-weight=\"700\"")
            o.append(" data-r=\"").append(l.role).append("\">").append(esc(l.text)).append("</text>")
        }
        // the labels run on without white space between them, as plan.js writes them, so the sheet's text reads
        // "DrawingFront wall elevation…" exactly as ct52 DR reads it
        o.append("\n</svg>\n")
        return o.toString()
    }

    private fun rect(o: StringBuilder, r: DoubleArray, fill: String, sw: Double) {
        o.append("<rect x=\"").append(f(r[0])).append("\" y=\"").append(f(r[1])).append("\" width=\"").append(f(r[2]))
            .append("\" height=\"").append(f(r[3])).append("\" fill=\"").append(fill).append("\" stroke=\"")
            .append(hex(SurveyPlan.INK)).append("\" stroke-width=\"").append(f(sw)).append("\"/>\n")
    }

    /** One display-list item as one SVG element. */
    private fun item(o: StringBuilder, it: SurveyPlan.Item) {
        when (it) {
            is SurveyPlan.Line -> {
                o.append("<line x1=\"").append(f(it.x1)).append("\" y1=\"").append(f(it.y1)).append("\" x2=\"").append(f(it.x2))
                    .append("\" y2=\"").append(f(it.y2)).append('"')
                stroke(o, it.colour, it.width, it.dash); tail(o, it)
            }
            is SurveyPlan.Poly -> {
                val d = it.rings.filter { r -> r.isNotEmpty() }.joinToString(" ") { r ->
                    r.withIndex().joinToString(" ") { (i, q) -> (if (i == 0) "M" else "L") + f(q[0]) + " " + f(q[1]) } + " Z"
                }
                o.append("<path d=\"").append(d).append('"')
                val fl = it.fill
                if (fl != null && it.fillAlpha > 0) {
                    o.append(" fill=\"").append(hex(fl)).append('"')
                    if (it.fillAlpha < 1) o.append(" fill-opacity=\"").append(f(it.fillAlpha)).append('"')
                    if (it.rings.size > 1) o.append(" fill-rule=\"evenodd\"")
                } else o.append(" fill=\"none\"")
                val st = it.stroke
                if (st != null && it.width > 0) stroke(o, st, it.width, it.dash)
                tail(o, it)
            }
            is SurveyPlan.Circle -> {
                o.append("<circle cx=\"").append(f(it.cx)).append("\" cy=\"").append(f(it.cy)).append("\" r=\"").append(f(it.r)).append('"')
                val fl = it.fill
                o.append(" fill=\"").append(if (fl != null) hex(fl) else "none").append('"')
                val st = it.stroke
                if (st != null && it.width > 0) stroke(o, st, it.width, it.dash)
                tail(o, it)
            }
            is SurveyPlan.Ellipse -> {
                o.append("<ellipse cx=\"").append(f(it.cx)).append("\" cy=\"").append(f(it.cy)).append("\" rx=\"").append(f(it.rx))
                    .append("\" ry=\"").append(f(it.ry)).append("\" fill=\"").append(hex(it.fill)).append('"')
                if (it.rot != 0.0) o.append(" transform=\"rotate(").append(f(it.rot)).append(' ').append(f(it.cx)).append(' ').append(f(it.cy)).append(")\"")
                tail(o, it)
            }
            is SurveyPlan.Arc -> {
                val a0 = Math.toRadians(it.start); val a1 = Math.toRadians(it.start + it.sweep)
                val sx = it.cx + it.r * Math.cos(a0); val sy = it.cy + it.r * Math.sin(a0)
                val ex = it.cx + it.r * Math.cos(a1); val ey = it.cy + it.r * Math.sin(a1)
                // page y down: a positive sweep turns clockwise on the page, which is SVG's sweep-flag 1
                o.append("<path d=\"M").append(f(sx)).append(' ').append(f(sy)).append(" A").append(f(it.r)).append(' ').append(f(it.r))
                    .append(" 0 ").append(if (Math.abs(it.sweep) > 180) 1 else 0).append(' ').append(if (it.sweep > 0) 1 else 0).append(' ')
                    .append(f(ex)).append(' ').append(f(ey)).append("\" fill=\"none\"")
                stroke(o, it.colour, it.width, it.dash); tail(o, it)
            }
            is SurveyPlan.Text -> {
                o.append("<text x=\"").append(f(it.x)).append("\" y=\"").append(f(it.y)).append("\" font-size=\"").append(f(it.size))
                    .append("\" fill=\"").append(hex(it.colour)).append("\" text-anchor=\"")
                    .append(when (it.anchor) { SurveyPlan.Anchor.START -> "start"; SurveyPlan.Anchor.MIDDLE -> "middle"; SurveyPlan.Anchor.END -> "end" })
                    .append("\" dominant-baseline=\"middle\"")
                if (it.bold) o.append(" font-weight=\"700\"")
                if (it.rot != 0.0) o.append(" transform=\"rotate(").append(f(it.rot)).append(' ').append(f(it.x)).append(' ').append(f(it.y)).append(")\"")
                attrs(o, it)
                o.append('>').append(esc(it.text)).append("</text>")
            }
        }
    }

    private fun stroke(o: StringBuilder, c: Int, w: Double, dash: DoubleArray?) {
        o.append(" stroke=\"").append(hex(c)).append("\" stroke-width=\"").append(f(w)).append('"')
        // as the screen paints it: a dash only when both its lengths are real
        if (dash != null && dash.size >= 2 && dash[0] > 0 && dash[1] > 0) o.append(" stroke-dasharray=\"").append(f(dash[0])).append(' ').append(f(dash[1])).append('"')
    }

    private fun attrs(o: StringBuilder, it: SurveyPlan.Item) {
        if (it.role.isNotEmpty()) o.append(" data-r=\"").append(esc(it.role)).append('"')
        it.key?.let { k -> o.append(" data-k=\"").append(esc(k)).append('"') }
    }

    private fun tail(o: StringBuilder, it: SurveyPlan.Item) { attrs(o, it); o.append("/>") }

    /** Text and attribute values escaped for XML. */
    fun esc(s: String): String {
        val b = StringBuilder(s.length + 8)
        for (ch in s) when (ch) {
            '&' -> b.append("&amp;"); '<' -> b.append("&lt;"); '>' -> b.append("&gt;"); '"' -> b.append("&quot;")
            else -> if (ch < ' ' && ch != '\t' && ch != '\n') b.append(' ') else b.append(ch)
        }
        return b.toString()
    }

    fun hex(c: Int): String = "#%06x".format(c and 0xffffff)

    /** A coordinate, mm: at most two decimals, never an exponent, never "-0". */
    fun f(v: Double): String = num(v, 100.0)

    /** The scale factor: six decimals (1:60 is 0.016667). */
    private fun fine(v: Double): String = num(v, 1_000_000.0)

    private fun num(v: Double, q: Double): String {
        if (!v.isFinite()) return "0"
        val r = Math.round(v * q)
        if (r == 0L) return "0"
        val neg = r < 0; val a = Math.abs(r); val qi = q.toLong()
        val whole = a / qi; val frac = (a % qi).toString().padStart(qi.toString().length - 1, '0').trimEnd('0')
        return (if (neg) "-" else "") + whole + (if (frac.isEmpty()) "" else ".$frac")
    }
}
