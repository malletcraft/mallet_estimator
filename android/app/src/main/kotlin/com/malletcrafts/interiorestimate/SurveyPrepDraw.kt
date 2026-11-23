package com.malletcrafts.interiorestimate

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.malletcrafts.interiorestimate.pano.DimLayout
import com.malletcrafts.interiorestimate.pano.MarkDims
import com.malletcrafts.interiorestimate.pano.MarkLife
import com.malletcrafts.interiorestimate.pano.NotePin
import com.malletcrafts.interiorestimate.pano.NudgePad
import com.malletcrafts.interiorestimate.pano.PhotoFigures
import com.malletcrafts.interiorestimate.pano.Pick
import com.malletcrafts.interiorestimate.pano.SheetMarks
import com.malletcrafts.interiorestimate.pano.SurveyChain
import com.malletcrafts.interiorestimate.pano.SurveyGuide
import com.malletcrafts.interiorestimate.pano.SurveyMeas
import com.malletcrafts.interiorestimate.pano.SurveyPrep
import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch
import com.malletcrafts.interiorestimate.pano.SurveyRef
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Drawing for survey prep, on an android.graphics.Canvas, so the screen and
 * the saved pictures draw marks the same way. A port of mock-up v33's canvas
 * code: every size there is in the mock-up's canvas pixels, and [u] scales
 * them (the screen passes its width / 480, a saved picture its width / 960).
 *
 * [m] maps picture pixels (a view's, or a squared surface's) to the canvas.
 *
 * THE LANGUAGE IS A PARAMETER of [drawMarks], [drawLines] and [drawRef], never a field on [Marks] (port 61c slice
 * M3b, critic note 5): the two callers deliberately differ -- the screen passes its live tx, FaceWriter's survey
 * sheets pass Tx.EN (decision 4: exports stay English) -- so every call site states its language, and check (i)
 * sees every site. The words drawn: the four boundary-line names, the reference's "Ref 905 × 2053" tag and the
 * chain total's " laser"; every other drawn string is a code, a figure or a glyph, byte-equal in every language.
 *
 * SO IS THE AUDIENCE (port 62 slice L2, 11 Nov 2026; DESIGN-port62 section 3.2, the plan critic's finding 3), for the
 * same reason: [drawMarks] takes a SheetMarks.Audience with no default, and is HANDED its marks by SheetMarks.marks.
 * The screen passes OFFICE and draws a mark's state -- its outline, a spot's ring, and after its code the act, the ✎
 * note badge, V+ and the state's pill, in the live language (MarkLife.stroke / tag; the prototype's outline, ring and
 * paintState, index61c2.html 2872-2911). FaceWriter's saved "survey marks" pictures pass OTHERS and are handed THE
 * ROOM AS SURVEYED: no mark born a wish or Vendor added, no Note, and no state on the marks that are there -- so this
 * painter cannot draw one into a document for anybody else, and a v52 prep's picture is what it always was.
 *
 * THE NOTE'S PIN (port 62 slice L6; the prototype's paintNote, index61c2.html 2918-2926): a Note is drawn as a 20 dp
 * white disc with ✎, and beside it "FNOTE1 ✎", a padlock when it is for the office only, and the first of its words
 * ([notePin]; what the tag says and where it is cut is NotePin.tag in :pano). ON THE SCREEN ONLY: the marks a picture
 * for others is handed hold no Note, so no branch here can write an office-only note into one. A Plumbing point and a
 * Low-voltage point are spots with their own symbols ([newSpotSym]).
 *
 * ONE FIGURE ON EVERY DIMENSION, ON THE PHOTO (port 63 slice M2, 16 Nov 2026; DESIGN-port63 §1.1, §1.2 and §1.6). Amit,
 * 15 Nov 2026, after his first use of the app on a client site: "Having both measurements foto and measured and both is
 * very confusing while using app. … imagemeter shows all dimensions on the annotation there itself which is easy to
 * use." So the EDITING PHOTO (mode "main") draws every dimension with ONE figure on its own line ([figures]): which
 * dimensions a mark has and what each one's figure is come from MarkDims, where every plate goes from DimLayout, both
 * in :pano -- this file measures the texts, asks, and draws what comes back. A Line's and an area's tag and a chain's
 * figures are tap targets too, under their reading keys. THE SAVED PICTURES ARE NOT CHANGED BY THIS (mode "sheet",
 * FaceWriter): they keep [dims] and [vtag], the photo's figure beside the reading, byte for byte, until Amit has ruled
 * on them (DESIGN-port63 question 1, slice M3) -- and those two functions have no other caller left.
 */
internal object SurveyPrepDraw {

    /** Bottom, top, left and right line colours. */
    val LINE_COLOUR = mapOf("bottom" to 0xff8a3d, "top" to 0xe0b040, "left" to 0x4dabf7, "right" to 0xda77f2)

    /** A mark past its face (the room made smaller under it): outlined in this red, dashed (v51). */
    const val OUT_COLOUR = 0xff5c5c

    fun argb(rgb: Int, alpha: Int = 255): Int = (alpha shl 24) or (rgb and 0xFFFFFF)

    private val MONO_BOLD: Typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

    fun label(c: Canvas, text: String, x: Float, y: Float, colour: Int, size: Float, u: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; isFakeBoldText = true }
        p.style = Paint.Style.STROKE; p.strokeWidth = 3f * u; p.color = argb(0x000000, 0xcc)
        c.drawText(text, x, y, p)
        p.style = Paint.Style.FILL; p.color = argb(colour)
        c.drawText(text, x, y, p)
    }

    private fun diamond(c: Canvas, x: Float, y: Float, colour: Int, r: Float, u: Float) {
        val path = Path().apply { moveTo(x, y - r); lineTo(x + r, y); lineTo(x, y + r); lineTo(x - r, y); close() }
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL; p.color = argb(colour, 0xee); c.drawPath(path, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.5f * u; p.color = argb(0xffffff); c.drawPath(path, p)
        p.style = Paint.Style.FILL; c.drawCircle(x, y, 3f * u, p)
    }

    private fun stroke(colour: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = width; color = colour
    }

    /** The padlock glyph, [sz] across, centred on ([x], [y]) (sp.js lockGlyph; v52 slice 8). */
    private fun lockGlyph(c: Canvas, x: Float, y: Float, sz: Float, colour: Int) {
        val bw = sz * .72f; val bh = sz * .48f; val r = sz * .22f
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colour; style = Paint.Style.STROKE; strokeWidth = max(1f, sz * .13f) }
        val path = Path().apply {
            moveTo(x - r, y + sz * .04f); lineTo(x - r, y - sz * .06f)
            arcTo(RectF(x - r, y - sz * .06f - r, x + r, y - sz * .06f + r), 180f, 180f, false)
            lineTo(x + r, y + sz * .04f)
        }
        c.drawPath(path, p)
        p.style = Paint.Style.FILL
        c.drawRect(x - bw / 2, y + sz * .02f, x + bw / 2, y + sz * .02f + bh, p)
    }

    /** A padlock on a dark pill, its top-right corner at ([x], [y]), in the mark's [colour] (sp.js padPill). */
    private fun padPill(c: Canvas, x: Float, y: Float, colour: Int, u: Float) {
        val sz = 12f * u; val pw = sz + 6f * u; val ph = sz + 5f * u; val x0 = x - pw
        c.drawRoundRect(RectF(x0, y, x, y + ph), ph / 2, ph / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(0x000000, 0xd9) })
        lockGlyph(c, x0 + pw / 2, y + ph / 2 - sz * .14f, sz, argb(colour))
    }

    // ------------------------------------------------------------ 1 · Lines

    /**
     * The four boundary lines of a surface, drawn long so they read as lines
     * and not segments; a solid line has been set by hand, a dashed one not
     * yet. No numbers -- only the boundary. [thin] is the magnifier's version.
     */
    fun drawLines(c: Canvas, tx: Tx, s: String, sf: SurveyPrep.Surface, frame: SurveyPrep.Frame?,
                  m: (Double, Double) -> FloatArray, u: Float, thin: Boolean) {
        val ext = 2.0 * SurveyPrep.VW
        val locked = SurveyPrep.linesLocked(sf)
        for (k in SurveyPrep.SLOTS) {
            val l = sf.lines.getValue(k); val set = sf.set[k] == true
            val dx = l[1][0] - l[0][0]; val dy = l[1][1] - l[0][1]; val n = hypot(dx, dy).takeIf { it > 0 } ?: 1.0
            val a = m(l[0][0] - dx / n * ext, l[0][1] - dy / n * ext); val b = m(l[0][0] + dx / n * ext, l[0][1] + dy / n * ext)
            val p = stroke(argb(LINE_COLOUR.getValue(k)), (if (thin) 1.5f else if (set) 3f else 2.2f) * u)
            if (!set) p.pathEffect = DashPathEffect(floatArrayOf(10f * u, 7f * u), 0f)
            c.drawLine(a[0], a[1], b[0], b[1], p)
        }
        if (thin) return
        frame?.q?.forEach { q -> val s0 = m(q[0], q[1]); c.drawCircle(s0[0], s0[1], 6f * u, stroke(argb(0xffffff), 2f * u)) }
        for (k in SurveyPrep.SLOTS) for (pt in sf.lines.getValue(k)) {
            val q = m(pt[0], pt[1]); diamond(c, q[0], q[1], LINE_COLOUR.getValue(k), 14f * u, u)
            // LOCKED LINES (v52 slice 8, sp.js 261): each grip carries a padlock on a disc of its colour
            if (locked) {
                c.drawCircle(q[0], q[1], 7f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(LINE_COLOUR.getValue(k)) })
                lockGlyph(c, q[0], q[1] - 1f * u, 11f * u, argb(0x14160F))
            }
        }
        // the four boundary-line names, in the language -- the first Devanagari this painter draws (port 61c slice M3b)
        for (k in SurveyPrep.SLOTS) {
            val p = sf.lines.getValue(k)[1]; val q = m(p[0], p[1])
            label(c, SurveyPrep.lineName(tx, s, k), q[0] + 16f * u, q[1] - 10f * u, 0xffffff, 12f * u, u)
        }
    }

    /**
     * THE REFERENCE RECTANGLE ON 1 · LINES (v52 slice 12, sp.js drawRef): while it is on and placed -- shown or hidden,
     * since Lines is where it is worked on -- its outline in cyan #22d3ee, dashed 8 / 5, filled #22d3ee22; its four
     * corners as diamonds dragged like line ends; "Ref 905 × 2053" by its topmost corner, with a padlock pill when
     * locked. Not in the magnifier.
     */
    fun drawRef(c: Canvas, tx: Tx, s: String, sf: SurveyPrep.Surface, m: (Double, Double) -> FloatArray, u: Float) {
        val r = SurveyRef.of(sf)?.takeIf { SurveyRef.placed(it) } ?: return
        val pts = r.q!!.map { m(it[0], it[1]) }
        val path = Path().apply { moveTo(pts[0][0], pts[0][1]); for (i in 1 until pts.size) lineTo(pts[i][0], pts[i][1]); close() }
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = argb(SurveyRef.COLOUR, 0x22) })
        c.drawPath(path, stroke(argb(SurveyRef.COLOUR), 2.5f * u).apply { pathEffect = DashPathEffect(floatArrayOf(8f * u, 5f * u), 0f) })
        for (p in pts) diamond(c, p[0], p[1], SurveyRef.COLOUR, 13f * u, u)
        val top = pts.minByOrNull { it[1] } ?: return
        label(c, SurveyRef.label(tx, r), top[0] + (if (r.locked) 30f else 14f) * u, top[1] - 8f * u, SurveyRef.COLOUR, 12f * u, u)
        if (r.locked) padPill(c, top[0] + 28f * u, top[1] - 24f * u, SurveyRef.COLOUR, u)
    }

    /**
     * THE REFERENCE ON A SQUARED FACE (v52 slice 12, sp.js drawRefSq): 2 · Mark, 3 · List, the List tab and the saved
     * pictures draw its outline through the face's ONE frame -- thin dashed cyan, 3 when picked -- and "Ref 905 × 2053"
     * by its top-left corner; never while its eye is off (SurveyRef.faceQuad), and it still squares the face then.
     */
    private fun drawRefSq(c: Canvas, tx: Tx, s: String, o: Marks, m: (Double, Double) -> FloatArray, u: Float) {
        val sf = o.prep.surface(s)
        val mm = SurveyRef.faceQuad(sf, o.frame) ?: return
        val r = SurveyRef.of(sf) ?: return
        val sq = o.sq
        val pts = mm.map { m(sq.x(it[0]), sq.y(it[1])) }
        val sel = o.selId == SurveyRef.id(s)
        val p = stroke(argb(SurveyRef.COLOUR), (if (sel) 3f else 1.5f) * u).apply { pathEffect = DashPathEffect(floatArrayOf(8f * u, 5f * u), 0f) }
        for (i in 0 until 4) { val a = pts[i]; val b = pts[(i + 1) % 4]; c.drawLine(a[0], a[1], b[0], b[1], p) }
        label(c, SurveyRef.label(tx, r), pts[3][0] + 4f * u, pts[3][1] - 6f * u, SurveyRef.COLOUR, (if (o.all) 22f else 11f) * u, u)
    }

    // ------------------------------------------------------------ 2 · Mark

    /** What the marks are drawn with: the room, the selection, the Show toggle. */
    class Marks(
        val prep: SurveyPrep.Prep,
        val a: Double, val b: Double,
        val sq: SurveyPrep.Square,
        /** "main" (the editing photo), "loupe" (thin), or "sheet" (a list picture / a saved one). */
        val mode: String,
        /** Every mark with its dimensions and big labels, as on the List tab. */
        val all: Boolean,
        val selId: String?,
        val active: String?,
        /** "derived", "actual" or "both": what a SAVED picture's value tags carry (mode "sheet", [vtag]) -- FaceWriter
         *  passes it; the screen has no such choice any more (port 63 slice M2) and names every later argument. */
        val show: String = "both",
        /** Canvas bounds, for keeping tags on it. */
        val width: Float, val height: Float,
        /** Filled with the number tags drawn, for taps (SurveyPrepTouch.hit). */
        val tags: MutableList<SurveyPrepTouch.Tag>?,
        /** Screen px per dp: the picked mark's tags stand at least 6 dp off its tabs; the Line and the Tape (v52
         *  slice 9) are sized in it. */
        val dp: Float = 1f,
        /** A Tape or a Line being pulled on this surface (v52 slice 9): drawn dashed (a Line) or translucent (a tape). */
        val drag: SurveyMeas.Draft? = null,
        /** The corners of a Line area being tapped on this surface: drawn open and dashed. */
        val area: List<DoubleArray>? = null,
        /** v52 slice 10: the plane grid's opacity on the editing photo (0: none) -- 1 with the grid button on, else the
         *  guide's fade. */
        val gridAlpha: Float = 0f,
        /** The perspective guide (SurveyGuide.now) and its opacity: two dashed rays and the start ring. */
        val guide: SurveyGuide.Guide? = null,
        val guideAlpha: Float = 0f,
        /** The picked Line's or tape's ends, and an area's corners, are handles (Select on). */
        val ends: Boolean = false,
        /** v52 slice 11: a chain being drawn on this surface -- dashed, its segments' photo figures. */
        val chain: SurveyChain.Draft? = null,
        /** v52 slice 12: the face's ONE frame (SurveyPrep.frameOf), through which its reference rectangle is drawn; null
         *  draws none. */
        val frame: SurveyPrep.Frame? = null,
        /** Port 62 slice L4: the picked mark's eight resize tabs are drawn (the editing photo only). Since port 63 slice
         *  I1b ONLY WHILE IT IS HELD (Pick.handles) -- a mark the walk reached, a locked one and one the phase holds
         *  read-only (Pick.mayHold) show none; a control that would change it is HIDDEN, never greyed. */
        val tabs: Boolean = true,
        /** The keypad's uncommitted digits (MeasurePad.live) for the gold plate; null: none. */
        val live: String? = null,
        /** Boxes no plate may cover, canvas px, besides the marks' own codes: the fit / grid column. */
        val keep: List<SurveyPrepTouch.Box> = emptyList(),
        /** Port 63 slice I1b, the editing photo: what is picked, held and gold (Pick.State), from which PhotoFigures decides
         *  which figures are drawn -- every READ figure of every mark, the picked one's unread ones as `?`, no photo
         *  estimate anywhere. Null: [selId] picked, [active] gold, nothing held. */
        val pick: Pick.State? = null,
        /** The laser chip is gold: the gold plate's `▶` is solid; hollow while it is not (LaserChip.ready). */
        val ready: Boolean = true,
        /** The reading key whose plate has just taken a reading: it rings gold for a second (Landing.RING_MS). */
        val ring: String? = null,
        /** The phase keys readings (MeasureFlow.canRead): false after Survey, where the picked item shows its READ figures
         *  only and no `?` -- nothing there can be read (the review of I1, finding 5; PhotoFigures.layout's `ask`). */
        val ask: Boolean = true,
        /** Port 63 slice I2b, the editing photo: THE PICKED ITEM'S CODE IS DRAWN AS A CHIP outside it
         *  (SurveyPrepTouch.codeChip), and not inside its box -- a tap on the chip opens the panel that slides up. */
        val chip: Boolean = false,
        /** Filled with what was drawn of the picked item -- its box and its chip -- for the press, the lift under the
         *  panel and Fine move's corner ([Shown]); null: nobody asks. */
        val shown: Shown? = null,
        /** FINE MOVE WHILE ITS PAD IS UP (the review of I2, finding 6; DESIGN-imagemeter §6: "the part last touched ...
         *  drawn solid"): the handle its arrows move is FILLED in the item's colour, ringed white -- one tab or end, or
         *  every handle of the item when they move it whole (NudgePad.solid). Null: no pad, every handle as it was. */
        val nudge: NudgePad.State? = null,
    )

    /**
     * WHAT THE EDITING PHOTO LAST DREW OF THE PICKED ITEM (port 63 slice I2b), canvas px: [id] the item; [item] its box
     * -- a mark's as drawn (a spot's own square), the box round a Tape's, a Line's, an area's or a chain's points;
     * [chip] its code chip, where SurveyPrepTouch.codeChip put it, or null: none could be placed, or none is drawn;
     * and the canvas's own size, [w] x [h]. Written by the drawing as [Marks.tags] is; read by the press (the chip's tap
     * box, SurveyPrepTouch.hit), by the lift under the panel (PhotoLift) and by Fine move (NudgePad.start's corner).
     * A plain holder: nothing composes from it.
     */
    class Shown {
        var id: String? = null
        var item: SurveyPrepTouch.Box? = null
        var chip: SurveyPrepTouch.Chip? = null
        var w: Double = 0.0
        var h: Double = 0.0
        /** A new frame of a [w] x [h] canvas: nothing is picked on it until the drawing says so. */
        fun clear(w: Double, h: Double) { id = null; item = null; chip = null; this.w = w; this.h = h }
    }

    /**
     * A point's spot on the canvas (v52): its centre x, y and its radius -- the 50 mm it is
     * across on the face, never under 6 dp, so never under 12 dp across ([dp]: px per dp).
     */
    fun spotOf(e: SurveyPrep.Mark, sq: SurveyPrep.Square, m: (Double, Double) -> FloatArray, dp: Float): FloatArray {
        val cx = (e.x0 + e.x1) / 2; val cy = (e.y0 + e.y1) / 2
        val c = m(sq.x(cx), sq.y(cy)); val c2 = m(sq.x(cx + SurveyPrep.SPOT / 2), sq.y(cy))
        return floatArrayOf(c[0], c[1], max(6f * dp, hypot(c2[0] - c[0], c2[1] - c[1])))
    }

    /** The symbols' ink on a spot. */
    private const val SPOT_INK = 0x14160F

    /**
     * THE TWO NEW POINTS' SYMBOLS inside a spot's dot (port 62 slice L6; the prototype's T53.spotSym, index61c2.html
     * 2669-2672), [k] = 0.62 of the dot's radius: a Plumbing point a DROP; a Low-voltage point a dot between two arcs,
     * above and below. [fill] and [line] are the caller's ink -- [line] a round-capped stroke max(1, 0.26 k) wide.
     * False, with nothing drawn, for any other type. One painter for the squared face and the 360.
     */
    fun newSpotSym(c: Canvas, x: Float, y: Float, k: Float, t: String, fill: Paint, line: Paint): Boolean {
        when (t) {
            "PLB" -> c.drawPath(Path().apply {
                moveTo(x, y - k * .85f); quadTo(x - k * .62f, y + k * .05f, x, y + k * .7f); quadTo(x + k * .62f, y + k * .05f, x, y - k * .85f); close() }, fill)
            "LV" -> {
                c.drawCircle(x, y, k * .22f, fill)
                val box = RectF(x - k * .62f, y - k * .62f, x + k * .62f, y + k * .62f)
                c.drawArc(box, -135f, 90f, false, line); c.drawArc(box, 45f, 90f, false, line)
            }
            else -> return false
        }
        return true
    }

    /** How wide the two new points' symbols are stroked, for a dot whose symbol scale is [k] (spotSym: max(1, 0.26 k)). */
    fun newSpotPen(k: Float): Float = max(1f, k * .26f)

    /**
     * A SPOT (Amit, 11 Oct 2026 20:35: "electric points are spots . dont let them resize. have
     * its da 50 mm."): a filled dot in the type's colour, a 1.6 dp dark rim (3 dp white when
     * picked), and its symbol -- Electric two dots, Bulb an X, Fan three blades round a hub; a
     * Plumbing point a drop, a Low-voltage point a dot between two arcs ([newSpotSym], slice L6).
     * No resize tabs, ever. As sp.js spot().
     */
    private fun spot(c: Canvas, x: Float, y: Float, r: Float, t: String, col: Int, sel: Boolean, thin: Boolean, dp: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL; p.color = argb(col); c.drawCircle(x, y, r, p)
        if (!thin) {
            p.style = Paint.Style.STROKE; p.strokeWidth = (if (sel) 3f else 1.6f) * dp
            p.color = if (sel) argb(0xffffff) else argb(0x000000, 0xaa); c.drawCircle(x, y, r, p)
        }
        val k = r * .62f
        p.color = argb(SPOT_INK); p.strokeWidth = max(1f, r * .16f); p.strokeCap = Paint.Cap.ROUND
        when (t) {
            "P" -> {
                p.style = Paint.Style.STROKE
                c.drawLine(x - k * .7f, y - k * .7f, x + k * .7f, y + k * .7f, p)
                c.drawLine(x - k * .7f, y + k * .7f, x + k * .7f, y - k * .7f, p)
            }
            "F" -> {
                p.style = Paint.Style.FILL
                for (i in 0 until 3) {
                    c.save(); c.translate(x, y); c.rotate(-90f + 120f * i)
                    c.drawOval(RectF(k * .05f, -k * .2f, k * 1.05f, k * .2f), p)
                    c.restore()
                }
                c.drawCircle(x, y, k * .18f, p)
            }
            "PLB", "LV" -> {
                p.style = Paint.Style.FILL
                newSpotSym(c, x, y, k, t, p, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; color = argb(SPOT_INK); strokeWidth = newSpotPen(k); strokeCap = Paint.Cap.ROUND })
            }
            else -> {
                p.style = Paint.Style.FILL
                for (i in listOf(-1f, 1f)) c.drawCircle(x + i * k * .38f, y, max(.8f, k * .17f), p)
            }
        }
    }

    // ------------------------------------------------------------ the Note's pin (port 62 slice L6)

    /**
     * A NOTE, at ([x], [y]) on the canvas (L53.paintNote, 2918-2926): a white disc 10 u in radius ringed 2 u in ink
     * -- and, picked ([sel]), in gold 3 u outside it -- with ✎ on it; then its tag from 4 u right of the disc, bold 11 u
     * on a dark halo: "FNOTE1 ✎" in white, a 10 u amber padlock when the note is for the office only, and its words in
     * #ECEEE8, cut as NotePin.tag cuts them to the tag's 120 u. ALWAYS AT FULL STRENGTH: the prototype sets the alpha to
     * 1 for a Note, so it does not dim while another mark is picked -- the caller draws it outside the fade's layer.
     * One painter for the squared face and the 360.
     */
    fun notePin(c: Canvas, e: SurveyPrep.Mark, x: Float, y: Float, u: Float, sel: Boolean) {
        val r = NotePin.R.toFloat() * u
        c.drawCircle(x, y, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(NotePin.DISC) })
        c.drawCircle(x, y, r, stroke(argb(NotePin.INK), NotePin.RING.toFloat() * u))
        if (sel) c.drawCircle(x, y, r + NotePin.SEL_GAP.toFloat() * u, stroke(argb(NotePin.PICKED), NotePin.RING.toFloat() * u))
        // ✎ centred on the disc, half a u under its centre (the page's textBaseline "middle")
        val glyph = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = NotePin.GLYPH.toFloat() * u; isFakeBoldText = true; textAlign = Paint.Align.CENTER; color = argb(NotePin.INK) }
        val fm = glyph.fontMetrics
        c.drawText(MarkLife.NOTE_BADGE, x, y + NotePin.GLYPH_DY.toFloat() * u - (fm.ascent + fm.descent) / 2, glyph)
        // the tag: measured with the paint it is drawn with
        val size = NotePin.TAG_SIZE.toFloat() * u
        val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; isFakeBoldText = true }
        val tag = NotePin.tag(e, u.toDouble()) { t -> pen.measureText(t).toDouble() }
        val tx0 = x + r + NotePin.TAG_DX.toFloat() * u; val ty = y + NotePin.TAG_DY.toFloat() * u
        label(c, tag.head, tx0, ty, NotePin.DISC, size, u)
        var xx = tx0 + pen.measureText(tag.head) + NotePin.HEAD_GAP.toFloat() * u
        if (tag.office) {
            lockGlyph(c, xx + NotePin.LOCK_DX.toFloat() * u, ty - NotePin.LOCK_UP.toFloat() * u, NotePin.LOCK.toFloat() * u, argb(NotePin.LOCK_COLOUR))
            xx += NotePin.LOCK_W.toFloat() * u
        }
        if (tag.text.isNotEmpty()) label(c, tag.text, xx + pen.measureText(" ") - NotePin.TEXT_BACK.toFloat() * u, ty, NotePin.TEXT_COLOUR, size, u)
    }

    // ------------------------------------------------------------ a mark's state (port 62 slice L2)

    /** A state's line on [p]: its dashes and gaps in u, round caps for a dotted one (MarkLife.Stroke; sp.js setLineDash, lineCap). */
    private fun lifeLine(p: Paint, st: MarkLife.Stroke, u: Float): Paint = p.apply {
        if (st.dash.isNotEmpty()) pathEffect = DashPathEffect(FloatArray(st.dash.size) { st.dash[it].toFloat() * u }, 0f)
        if (st.round) strokeCap = Paint.Cap.ROUND
    }

    /**
     * THE OUTLINE OF A MARK PAST EXISTING, in place of v52's (the prototype's L53.outline, index61c2.html 2872): the
     * box filled 0x22 and outlined in its state's line -- Client wish dashed 6 / 4, Vendor review 2 / 3, Vendor added
     * dotted, Vendor approved 1.5, Client approved and Scope 2.5, a Dropped mark grey at 40 % with a red diagonal from
     * its bottom-left to its top-right -- in the type's colour (a Dropped one's grey), as wide as
     * MarkLife.outlineWidth says for the selection and the loupe. False for an Existing mark, drawing nothing: the
     * caller draws v52's own.
     */
    private fun lifeOutline(c: Canvas, e: SurveyPrep.Mark, p: FloatArray, q: FloatArray, col: Int, u: Float, sel: Boolean, thin: Boolean): Boolean {
        val life = MarkLife.lifeOf(e)
        val st = MarkLife.stroke(life) ?: return false
        val ink = st.grey ?: col
        fun al(base: Int) = (base * st.alpha).roundToInt()
        c.drawRect(p[0], p[1], q[0], q[1], Paint().apply { color = argb(ink, al(MarkLife.FILL_ALPHA)) })
        c.drawRect(p[0], p[1], q[0], q[1], lifeLine(stroke(argb(ink, al(255)), MarkLife.outlineWidth(st, sel, thin).toFloat() * u), st, u))
        if (life == MarkLife.DROP) c.drawLine(p[0], q[1], q[0], p[1], stroke(argb(MarkLife.SLASH, al(255)), MarkLife.SLASH_W.toFloat() * u))
        return true
    }

    /**
     * A SPOT'S STATE (L53.ring, 2887): a ring 3 u outside its dot, in the state's line -- the dot itself stays v52's --
     * and a Dropped spot's red diagonal across it. Nothing for an Existing spot.
     */
    private fun lifeRing(c: Canvas, e: SurveyPrep.Mark, x: Float, y: Float, r: Float, col: Int, u: Float) {
        val life = MarkLife.lifeOf(e)
        val st = MarkLife.stroke(life) ?: return
        val a = (255 * st.alpha).roundToInt(); val rr = r + MarkLife.RING_GAP.toFloat() * u
        c.drawCircle(x, y, rr, lifeLine(stroke(argb(st.grey ?: col, a), MarkLife.ringWidth(st).toFloat() * u), st, u))
        if (life == MarkLife.DROP) c.drawLine(x - rr, y + rr, x + rr, y - rr, stroke(argb(MarkLife.SLASH, a), MarkLife.SLASH_W.toFloat() * u))
    }

    private const val TAG_INK = 0x1d1f1a
    private const val VPLUS_COLOUR = 0x6741d9

    /**
     * AFTER A MARK'S CODE, drawn at ([x0], [y]) (its baseline) in bold [fs] px (L53.paintState, 2898-2911): the act's
     * glyph, as the code is drawn (a 3 u dark halo under white); the ✎ note badge, a 10 u white disc ringed in ink; V+
     * on violet for a mark a vendor added; then the state's name on a 16 u pill of its colour, ringed white. All at the
     * tag's own alpha (.6 for a Dropped mark). [t] is MarkLife.tag's: this draws what it says and decides nothing.
     */
    private fun lifeTag(c: Canvas, code: String, t: MarkLife.Tag, x0: Float, y: Float, fs: Float, u: Float) {
        fun al(base: Int) = (base * t.alpha).roundToInt()
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = fs; isFakeBoldText = true }
        var x = x0 + p.measureText(code) + 3f * u
        val yc = y - fs * .36f
        t.act?.let { a ->
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f * u; p.color = argb(0x000000, al(0xcc)); c.drawText(a, x, y, p)
            p.style = Paint.Style.FILL; p.color = argb(0xffffff, al(255)); c.drawText(a, x, y, p)
            x += p.measureText(a) + 3f * u
        }
        if (t.note) {
            val r = 5f * u
            c.drawCircle(x + r, yc, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(0xffffff, al(255)) })
            c.drawCircle(x + r, yc, r, stroke(argb(TAG_INK, al(255)), 1f * u))
            c.drawText(MarkLife.NOTE_BADGE, x + r, yc + 2.5f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 7f * u; isFakeBoldText = true; textAlign = Paint.Align.CENTER; color = argb(TAG_INK, al(255)) })
            x += 2 * r + 3f * u
        }
        t.vplus?.let { v ->
            val vp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f * u; isFakeBoldText = true }
            val w = vp.measureText(v) + 6f * u; val h = 13f * u
            c.drawRoundRect(RectF(x, yc - h / 2, x + w, yc + h / 2), 3f * u, 3f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(VPLUS_COLOUR, al(255)) })
            vp.color = argb(0xffffff, al(255)); c.drawText(v, x + 3f * u, yc + 3.2f * u, vp)
            x += w + 3f * u
        }
        t.pill?.let { nm ->
            val pp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f * u; isFakeBoldText = true }
            val w = pp.measureText(nm) + 10f * u; val h = 16f * u
            val box = RectF(x, yc - h / 2, x + w, yc + h / 2)
            c.drawRoundRect(box, h / 2, h / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(t.colour, al(255)) })
            c.drawRoundRect(box, h / 2, h / 2, stroke(argb(0xffffff, al(255)), 1f * u))
            pp.color = argb(0xffffff, al(255)); c.drawText(nm, x + 5f * u, yc + 3.5f * u, pp)
        }
    }

    /** The alpha a mark that is not the picked one is drawn at while one is (sp.js 191 / 255). */
    private const val FADE = 191

    /**
     * [lifeTag] for the mark [e], when MarkLife.tag has one, after its [code] AS DRAWN (with its ✓, port 63). AT ITS OWN ALPHA, NEVER THE FADE'S: the prototype's
     * paintState SETS the alpha (1, or .6 for a Dropped mark) rather than multiplying it, so while another mark is picked
     * the rest dim to 75 % and their tags do not. A [faded] mark is being drawn into the fade's layer: that layer is
     * closed for the tag and opened again after it, so what follows -- the padlock -- still dims, and is still drawn
     * over the tag. Only a mark that HAS a tag pays for the second layer.
     */
    private fun lifeTagOf(c: Canvas, tx: Tx, e: SurveyPrep.Mark, code: String, x: Float, y: Float, fs: Float, u: Float, crowded: Boolean, faded: Boolean) {
        val t = MarkLife.tag(tx, e, crowded) ?: return
        if (faded) c.restore()
        lifeTag(c, code, t, x, y, fs, u)
        if (faded) c.saveLayerAlpha(null, FADE)
    }

    /**
     * THE GOLD FRAME ROUND THE MARK BEING MEASURED (MarkDims.ACTIVE_COLOUR): [MarkDims.ACTIVE_GAP_DP] outside its box --
     * a point: a ring that far outside its dot -- gold over a dark casing, so it reads on a bright photo and a dark
     * one alike. A Note asks nothing and is never measured.
     */
    private fun measuring(c: Canvas, e: SurveyPrep.Mark, sq: SurveyPrep.Square, m: (Double, Double) -> FloatArray, dp: Float) {
        if (MarkLife.isNote(e)) return
        val gap = MarkDims.ACTIVE_GAP_DP.toFloat() * dp
        val case = stroke(argb(MarkDims.ACTIVE_CASE, MarkDims.ACTIVE_CASE_ALPHA), MarkDims.ACTIVE_FRAME_CASE_DP.toFloat() * dp)
        val gold = stroke(argb(MarkDims.ACTIVE_COLOUR), MarkDims.ACTIVE_FRAME_DP.toFloat() * dp)
        if (SurveyPrep.isPoint(e.t)) {
            val d = spotOf(e, sq, m, dp); val r = d[2] + gap + MarkDims.ACTIVE_FRAME_DP.toFloat() * dp / 2f
            c.drawCircle(d[0], d[1], r, case); c.drawCircle(d[0], d[1], r, gold)
            return
        }
        val p = m(sq.x(e.x0), sq.y(e.y1)); val q = m(sq.x(e.x1), sq.y(e.y0))
        val l = min(p[0], q[0]) - gap; val t = min(p[1], q[1]) - gap; val r = max(p[0], q[0]) + gap; val b = max(p[1], q[1]) + gap
        c.drawRect(l, t, r, b, case); c.drawRect(l, t, r, b, gold)
    }

    /**
     * [audience] decides WHICH marks are drawn and what of them (SheetMarks.marks): the office's screen every mark with
     * its state; anyone else's picture the room as surveyed. It has no default -- every call site names who it draws for.
     */
    fun drawMarks(c: Canvas, tx: Tx, audience: SheetMarks.Audience, s: String, o: Marks, m: (Double, Double) -> FloatArray, u: Float) {
        val sq = o.sq; val thin = o.mode == "loupe"
        val els = SheetMarks.marks(o.prep.surface(s).marks, audience)
        val ta = m(sq.x(0.0), sq.y(o.b)); val tb = m(sq.x(o.a), sq.y(0.0))
        c.drawRect(ta[0], ta[1], tb[0], tb[1], stroke(argb(0xffffff, 0x99), 1.5f * u).apply {
            pathEffect = DashPathEffect(floatArrayOf(6f * u, 5f * u), 0f) })
        // DIM THE REST while one mark is being measured (Amit, 2026-10-10: "dim out rest of the foto when a
        // particular mark type is under selection for measurment") -- 20%, "so that rest foto can be seen".
        val sm = if (o.mode == "main" && o.selId != null) els.firstOrNull { it.id == o.selId } else null
        val dim = sm != null
        // where each mark's code is drawn on the editing photo: no figure's plate may cover one (DimLayout's `keep`)
        val codes = ArrayList<SurveyPrepTouch.Box>()
        val codePen = Paint().apply { isFakeBoldText = true }
        fun codeAt(text: String, x: Float, y: Float, size: Float) {
            codePen.textSize = size
            codes.add(SurveyPrepTouch.Box(x.toDouble(), (y - size).toDouble(), (x + codePen.measureText(text)).toDouble(), (y + size * .25f).toDouble()))
        }
        // THE PICKED ITEM'S CODE IS A CHIP (port 63 slice I2b; DESIGN-imagemeter §2.2; SurveyPrepTouch.codeChip): the code
        // and `▾`, outside the item -- above its top-left corner, below its bottom-left where it touches the photo's top
        // -- its tap box 16 dp clear of every tab, corner and end, so nothing else takes its press first. A HELD mark's
        // stands above its hand-anchored width plate (chipLift). Placed HERE, before the figures: its tap box is handed
        // to their layout as a box to keep clear, as a tab is. A Tape's, a Line's, an area's and a chain's is the one
        // place its code is shown (PhotoFigures.nameOf; P4). WHERE NO PLACE IS CLEAR THERE IS NO CHIP, and a mark's code
        // is then drawn inside its box as it always was: a long press still opens the panel.
        var chipOf: SurveyPrepTouch.Chip? = null
        var chipSays = ""
        var chipInk = 0
        val chipFs = DimLayout.textSize(true, o.dp.toDouble())
        if (o.mode == "main" && o.selId != null) {
            val cw = o.width.toDouble(); val ch = o.height.toDouble(); val dpx = o.dp.toDouble()
            val name = PhotoFigures.nameOf(o.prep, o.pick ?: Pick.State(o.selId, false, o.active))
            var itemBox: SurveyPrepTouch.Box? = null
            if (sm != null) {
                val spot = SurveyPrep.isSpot(sm.t)
                val bb = if (spot) spotOf(sm, sq, m, o.dp).let { d ->
                        SurveyPrepTouch.Box((d[0] - d[2]).toDouble(), (d[1] - d[2]).toDouble(), (d[0] + d[2]).toDouble(), (d[1] + d[2]).toDouble()) }
                    else { val p0 = m(sq.x(sm.x0), sq.y(sm.y1)); val q0 = m(sq.x(sm.x1), sq.y(sm.y0))
                        SurveyPrepTouch.Box(min(p0[0], q0[0]).toDouble(), min(p0[1], q0[1]).toDouble(), max(p0[0], q0[0]).toDouble(), max(p0[1], q0[1]).toDouble()) }
                itemBox = bb
                if (o.chip && name != null) {
                    // its code, and ✓ once everything it asks is measured -- as on its box
                    chipSays = SurveyPrepTouch.chipText(if (MarkDims.done(o.prep, s, sm, o.a, o.b)) name + MarkDims.DONE_GLYPH else name)
                    chipInk = SurveyPrep.typeOf(s, sm.t).colour
                    // it shows its tabs -- held, and no spot (a point and a Note's pin have none): the width plate is hand-anchored
                    val hand = o.tabs && !SurveyPrep.isLocked(sm) && !spot
                    chipOf = SurveyPrepTouch.codeChip(bb, SurveyPrepTouch.handlesOf(bb, spot), pen(chipFs, true).measureText(chipSays).toDouble(),
                        cw, ch, dpx, SurveyPrepTouch.chipLift(hand, chipFs, u.toDouble(), dpx), o.keep)
                }
            } else {
                val sf = o.prep.surface(s)
                val pm = sf.meas.firstOrNull { it.id == o.selId && it.pts.isNotEmpty() }
                val pk = if (pm == null) sf.chains.firstOrNull { it.id == o.selId && it.nodes.isNotEmpty() } else null
                val pts: List<DoubleArray>? = pm?.pts ?: pk?.nodes
                if (pts != null) {
                    val scr = pts.map { pq -> m(sq.x(pq[0]), sq.y(pq[1])).let { at -> doubleArrayOf(at[0].toDouble(), at[1].toDouble()) } }
                    itemBox = SurveyPrepTouch.Box(scr.minOf { it[0] }, scr.minOf { it[1] }, scr.maxOf { it[0] }, scr.maxOf { it[1] })
                    if (o.chip && name != null) {
                        chipSays = SurveyPrepTouch.chipText(name)
                        chipInk = if (pk != null) SurveyChain.COLOUR else if (pm?.kind == SurveyMeas.TAPE) TAPE_C else LINE_C
                        chipOf = SurveyPrepTouch.codeChipOfLine(scr, pen(chipFs, true).measureText(chipSays).toDouble(), cw, ch, dpx, o.keep)
                    }
                }
            }
            o.shown?.let { sh -> sh.id = o.selId; sh.item = itemBox; sh.chip = chipOf }
        }
        // the chip's tap box is kept clear by the figures' layout, with the codes
        chipOf?.let { k -> codes.add(k.tap) }
        if (sm != null && dim) {
            val pad = 6f * u
            // a picked spot -- a point, or a Note's pin (slice L6; sp.js 5082): the hole is round its dot, as drawn
            val hole = if (SurveyPrep.isSpot(sm.t)) spotOf(sm, sq, m, o.dp).let { d ->
                floatArrayOf(d[0] - d[2], d[1] - d[2], d[0] + d[2], d[1] + d[2]) }
            else { val p = m(sq.x(sm.x0), sq.y(sm.y1)); val q = m(sq.x(sm.x1), sq.y(sm.y0)); floatArrayOf(p[0], p[1], q[0], q[1]) }
            val path = Path().apply {
                fillType = Path.FillType.EVEN_ODD
                addRect(0f, 0f, o.width, o.height, Path.Direction.CW)
                addRect(hole[0] - pad, hole[1] - pad, hole[2] + pad, hole[3] + pad, Path.Direction.CW)
            }
            c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(0x000000, MarkDims.DIM_ALPHA) })
        }
        // THE MARK BEING MEASURED IS FRAMED IN GOLD (Amit, 16 Nov 2026: "i cant see which mark is being measured as its
        // no highlighted on foto"; MarkDims.ACTIVE_COLOUR) -- under the marks, so its own outline, code and tabs are
        // drawn over the frame, which stands just outside its box. The editing photo only: never a saved picture.
        if (o.mode == "main") o.active?.let { a -> o.prep.ownerOf(a)?.id }?.let { id -> els.firstOrNull { it.id == id } }
            ?.let { e -> measuring(c, e, sq, m, o.dp) }
        for (e in els.sortedBy { if (sm != null && it === sm) 1 else 0 }) {
            val col = SurveyPrep.typeOf(s, e.t).colour
            val p = m(sq.x(e.x0), sq.y(e.y1)); val q = m(sq.x(e.x1), sq.y(e.y0)); val sel = o.selId == e.id
            val faded = dim && !sel
            // ITS CODE, AND ✓ ONCE EVERYTHING IT ASKS IS MEASURED (port 63 slice M2; MarkDims.done) -- on the editing photo
            // only: a saved picture's code is the code alone, as it always was
            val code = if (o.mode == "main" && MarkDims.done(o.prep, s, e, o.a, o.b)) e.code + MarkDims.DONE_GLYPH else e.code
            // a locked mark (v52 slice 8): a padlock beside its code on the editing photo, and no resize tabs
            val locked = SurveyPrep.isLocked(e)
            // A NOTE IS ITS PIN (port 62 slice L6; sp.js 5087): no box, no figures, no tabs, and never dimmed -- the pin and
            // its tag are drawn at full strength, only a locked Note's padlock fades with the rest. In the magnifier, a dot.
            if (MarkLife.isNote(e)) {
                val at = m(sq.x((e.x0 + e.x1) / 2), sq.y((e.y0 + e.y1) / 2))
                if (thin) c.drawCircle(at[0], at[1], NotePin.LOUPE_R.toFloat() * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(NotePin.DISC) })
                else {
                    notePin(c, e, at[0], at[1], u, sel)
                    if (locked && o.mode == "main") {
                        if (faded) c.saveLayerAlpha(null, FADE)
                        padPill(c, at[0] + NotePin.PAD_DX.toFloat() * u, at[1] - NotePin.PAD_UP.toFloat() * u, NotePin.DISC, u)
                        if (faded) c.restore()
                    }
                }
                continue
            }
            if (faded) c.saveLayerAlpha(null, FADE)
            if (SurveyPrep.isPoint(e.t)) {
                // A SPOT: its dot and symbol, its code beside it, its two figures when picked -- no tabs
                val d = spotOf(e, sq, m, o.dp)
                spot(c, d[0], d[1], d[2], e.t, col, sel, thin, o.dp)
                if (!thin) {
                    // its state (port 62 slice L2): the ring round the dot, then the tag after its code
                    lifeRing(c, e, d[0], d[1], d[2], col, u)
                    // the picked item's code is its chip (slice I2b): not drawn here as well; its state's tag stays
                    val chipped = sel && chipOf != null
                    if (!chipped) {
                        label(c, code, d[0] + d[2] + 4f * u, d[1] - d[2] - 2f * u, col, (if (o.all) 26f else 13f) * u, u)
                        if (o.mode == "main") codeAt(code, d[0] + d[2] + 4f * u, d[1] - d[2] - 2f * u, 13f * u)
                    }
                    lifeTagOf(c, tx, e, if (chipped) "" else code, d[0] + d[2] + 4f * u, d[1] - d[2] - 2f * u, (if (o.all) 26f else 13f) * u, u, false, faded)
                    if (locked && o.mode == "main") padPill(c, d[0] + d[2] + 30f * u, d[1] - d[2] - 16f * u, col, u)
                    // a saved picture's own figures (mode "sheet"): the editing photo's are drawn once, after every mark ([figures])
                    if (o.mode == "sheet" && (sel || o.all)) dims(c, s, e, o, m, p, q, (if (o.all) 22f else 12f) * u, u)
                }
                if (faded) c.restore()
                if (!thin && SurveyPrep.outOf(e, o.a, o.b)) {
                    val pd = 4f * u
                    c.drawRect(d[0] - d[2] - pd, d[1] - d[2] - pd, d[0] + d[2] + pd, d[1] + d[2] + pd, stroke(argb(OUT_COLOUR), 2.5f * u).apply {
                        pathEffect = DashPathEffect(floatArrayOf(7f * u, 5f * u), 0f) })
                }
                continue
            }
            // A MARK PAST EXISTING is outlined in its state's line (port 62 slice L2), the loupe's thin copy too; an
            // Existing one exactly as v52 draws it -- and a sheet for anyone but the office is handed Existing marks alone
            if (!lifeOutline(c, e, p, q, col, u, sel, thin)) {
                c.drawRect(p[0], p[1], q[0], q[1], Paint().apply { color = argb(col, 0x22) })
                c.drawRect(p[0], p[1], q[0], q[1], stroke(argb(col), (if (thin) 1.5f else if (sel) 3.5f else 2.5f) * u))
            }
            if (!thin) {
                // the picked item's code is its chip, outside the box (slice I2b): not drawn inside it as well -- inside,
                // at the top-left, it lay in the `lt` tab's reach. Its state's tag stays where it was, from the corner.
                val chipped = sel && chipOf != null
                if (!chipped) {
                    label(c, code, p[0] + 6f * u, p[1] + (if (o.all) 30f else 16f) * u, col, (if (o.all) 26f else 13f) * u, u)
                    if (o.mode == "main") codeAt(code, p[0] + 6f * u, p[1] + 16f * u, 13f * u)
                }
                // ... and after its code its act, ✎, V+ and pill; the pill is its first word on a box under 90 u wide
                lifeTagOf(c, tx, e, if (chipped) "" else code, p[0] + 6f * u, p[1] + (if (o.all) 30f else 16f) * u, (if (o.all) 26f else 13f) * u, u, q[0] - p[0] < 90f * u, faded)
                if (locked && o.mode == "main") padPill(c, q[0] - 3f * u, p[1] + 3f * u, col, u)
                if (sel && o.mode == "main" && !locked && o.tabs) {
                    // its eight tabs, each where SurveyPrepTouch.tabAt puts it -- white, ringed in the mark's colour; THE ONE
                    // FINE MOVE NUDGES IS SOLID, the colours turned round, and all eight when its arrows move the whole
                    // mark (NudgePad.solid)
                    val h = 5f * u
                    for (k in SurveyPrepTouch.TABS) {
                        val at = SurveyPrepTouch.tabAt(k, p[0].toDouble(), q[0].toDouble(), p[1].toDouble(), q[1].toDouble())
                        val hx = at[0].toFloat(); val hy = at[1].toFloat()
                        val solid = NudgePad.solid(o.nudge, e.id, k)
                        c.drawRect(hx - h, hy - h, hx + h, hy + h, Paint().apply { color = argb(if (solid) col else 0xffffff) })
                        c.drawRect(hx - h, hy - h, hx + h, hy + h, stroke(argb(if (solid) 0xffffff else col), 2f * u))
                    }
                }
                if (o.mode == "sheet" && (sel || o.all)) dims(c, s, e, o, m, p, q, (if (o.all) 22f else 12f) * u, u)
            }
            if (faded) c.restore()
            // FLAGGED, NOT MOVED (v51): a mark the room size no longer holds keeps its millimetres and gets a
            // red dashed outline, until it is edited or deleted -- drawn OUTSIDE the faded layer, at full
            // strength even while another mark is picked (sp.js: g.globalAlpha=1 for this outline)
            if (!thin && SurveyPrep.outOf(e, o.a, o.b)) {
                val pd = 4f * u
                c.drawRect(p[0] - pd, p[1] - pd, q[0] + pd, q[1] + pd, stroke(argb(OUT_COLOUR), 2.5f * u).apply {
                    pathEffect = DashPathEffect(floatArrayOf(7f * u, 5f * u), 0f) })
            }
        }
        // THE LINES, AREAS AND TAPES (v52 slice 9) over the marks, as sp.js draws them after drawMarks -- and NOT in
        // the loupe (sp.js overlay: `if(which!=='loupe'){ ... drawMeas ... drawTapes }`), which shows the photo under
        // the finger: a tape being pulled ends at the loupe's centre, and its band would hide what is being aimed at
        // v52 slice 10: the plane grid over the marks and under the Lines and tapes, then the guide over everything --
        // as sp.js overlay draws them: drawGrid, drawMeas, drawTapes, drawGuide; none of it in the loupe
        fun face(x: Double, y: Double) = m(sq.x(x), sq.y(y))
        // EVERY FIGURE ON ITS ANNOTATION AND NOWHERE ELSE (port 63 slices I1a-I1c; DESIGN-imagemeter §18, §21): the editing
        // photo's plates -- every READ figure of every mark, the picked one's unread ones as `?`, no photo estimate, and
        // every Line's, area's, Tape's and chain's figure, the number alone -- PLACED TOGETHER, each turned along its own
        // line (P5), so no label lies on another, a code or the buttons. The marks' plates over every mark and under the
        // Lines, tapes and chains; theirs over those, which their plates break.
        val laid = if (o.mode == "main") figures(c, tx, s, o, m, u, codes) else null
        laid?.let { out -> drawPlates(c, out.plates.filter { it.role != MarkDims.Role.ITEM }, o, u, dim) }
        if (o.mode == "main" && o.gridAlpha > 0f) drawGrid(c, o.a, o.b, ::face, o.gridAlpha, o.dp)
        if (o.mode != "loupe") drawMeas(c, s, o, m, u)
        // v52 slice 11: the chains after the Lines and tapes, as sp.js overlay draws them (drawMeas, drawTapes, drawChains)
        // -- not in the loupe either
        if (o.mode != "loupe") drawChains(c, tx, s, o, m, u)
        laid?.let { out -> drawPlates(c, out.plates.filter { it.role == MarkDims.Role.ITEM }, o, u, false) }
        // v52 slice 12: the reference rectangle after the chains, as sp.js overlay draws it (drawRefSq) -- not in the loupe
        if (o.mode != "loupe") drawRefSq(c, tx, s, o, m, u)
        if (o.mode == "main") o.guide?.takeIf { it.s == s && o.guideAlpha > 0f }?.let { g -> drawGuide(c, g, o.a, o.b, ::face, o.guideAlpha, o.dp) }
        // THE CODE CHIP, over everything: it is a button
        chipOf?.let { k -> codeChip(c, k, chipSays, chipInk, chipFs, o.dp) }
    }

    /**
     * THE PICKED ITEM'S CODE CHIP, DRAWN (port 63 slice I2b; where it stands is SurveyPrepTouch.codeChip's): a pill the
     * height of its box, dark, outlined in the item's own colour -- its type's, a Line's blue, a tape's yellow, a
     * chain's -- its words ("FW1 ▾") white, bold and monospace at the picked figures' size, centred.
     */
    private fun codeChip(c: Canvas, k: SurveyPrepTouch.Chip, words: String, colour: Int, fs: Double, dp: Float) {
        val b = k.box
        val r = RectF(b.l.toFloat(), b.t.toFloat(), b.r.toFloat(), b.b.toFloat()); val rad = (b.b - b.t).toFloat() / 2f
        c.drawRoundRect(r, rad, rad, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(0x000000, 0xd9) })
        c.drawRoundRect(r, rad, rad, stroke(argb(colour), 1.5f * dp))
        c.drawText(words, ((b.l + b.r) / 2).toFloat(), ((b.t + b.b) / 2 + fs * 0.36).toFloat(),
            pen(fs, true).apply { color = argb(0xffffff); textAlign = Paint.Align.CENTER })
    }

    // ------------------------------------------------------------ v52 slice 10: the plane grid and the guide

    /**
     * THE PLANE GRID on an [a] x [b] face (sp.js drawGrid): a line every 100 mm once 100 mm is 12 dp on screen, else
     * every 500 mm (SurveyGuide.gridStep, measured at the face's centre), every 500 mm stronger (#ffffffaa at 1.4 dp,
     * the rest #ffffff4d at 0.8 dp), at [alpha]. [toS] maps face mm to the canvas -- the squared picture's scale in
     * 2 · Mark, the face's own perspective mapping in 1 · Lines, so there it leans with the photo.
     */
    fun drawGrid(c: Canvas, a: Double, b: Double, toS: (Double, Double) -> FloatArray, alpha: Float, dp: Float) {
        if (!(a > 0 && b > 0) || alpha <= 0f) return
        val c0 = toS(a / 2, b / 2); val c1 = toS(a / 2 + 100, b / 2)
        val step = SurveyGuide.gridStep(hypot(c1[0] - c0[0], c1[1] - c0[1]).toDouble(), dp.toDouble())
        val al = alpha.coerceIn(0f, 1f)
        val strong = stroke(argb(0xffffff, (SurveyGuide.GRID_STRONG_ALPHA * al).roundToInt()), SurveyGuide.GRID_STRONG_DP.toFloat() * dp)
        val weak = stroke(argb(0xffffff, (SurveyGuide.GRID_WEAK_ALPHA * al).roundToInt()), SurveyGuide.GRID_WEAK_DP.toFloat() * dp)
        for (gl in SurveyGuide.gridLines(a, b, step)) {
            val p = if (gl.axis == 0) toS(gl.at, 0.0) else toS(0.0, gl.at)
            val q = if (gl.axis == 0) toS(gl.at, b) else toS(a, gl.at)
            c.drawLine(p[0], p[1], q[0], q[1], if (gl.strong) strong else weak)
        }
    }

    /**
     * THE PERSPECTIVE GUIDE (sp.js drawGuide; Amit, 14 Oct 2026 20:45 IST: "i need perpective guidance reference shown
     * when i draw any of these two tools"): two rays in the tool's colour through the start, along the face's horizontal
     * and vertical and across the whole face (SurveyGuide.guideRays), dashed 9 / 6 dp, 1.6 dp wide and 2.8 on the axis
     * the drag has snapped onto; the start ringed, 5 dp. At [alpha] (the fade).
     */
    fun drawGuide(c: Canvas, g: SurveyGuide.Guide, a: Double, b: Double, toS: (Double, Double) -> FloatArray, alpha: Float, dp: Float) {
        val col = argb(SurveyGuide.colour(g.tool), (255 * alpha.coerceIn(0f, 1f)).roundToInt())
        val r = SurveyGuide.guideRays(g.o, a, b, g.snap)
        for ((sg, ax) in listOf(r.h to "h", r.v to "v")) {
            if (sg == null) continue
            val p = toS(sg.a[0], sg.a[1]); val q = toS(sg.b[0], sg.b[1])
            c.drawLine(p[0], p[1], q[0], q[1], stroke(col, r.widthDp(ax).toFloat() * dp).apply {
                pathEffect = DashPathEffect(floatArrayOf(SurveyGuide.DASH_DP.toFloat() * dp, SurveyGuide.GAP_DP.toFloat() * dp), 0f) })
        }
        val o = toS(g.o[0], g.o[1])
        c.drawCircle(o[0], o[1], SurveyGuide.RING_DP.toFloat() * dp, stroke(col, SurveyGuide.RING_W_DP.toFloat() * dp))
    }

    // ------------------------------------------------------------ v52 slice 9: Line and Tape

    /** A Line's light blue and the tape's yellow (sp.js MC, TC). */
    private val LINE_C = SurveyMeas.LINE_COLOUR
    private val TAPE_C = SurveyMeas.TAPE_COLOUR

    /** A filled arrow head at [tip], pointing away from [from]: 7 dp long, 3 dp either side (sp.js aHead). */
    private fun head(c: Canvas, tip: FloatArray, from: FloatArray, colour: Int, dp: Float) {
        val dx = tip[0] - from[0]; val dy = tip[1] - from[1]; val l = hypot(dx, dy); if (!(l > 0f)) return
        val ux = dx / l; val uy = dy / l
        val hl = SurveyMeas.HEAD_LEN_DP.toFloat() * dp; val hw = SurveyMeas.HEAD_W_DP.toFloat() * dp
        val path = Path().apply {
            moveTo(tip[0], tip[1])
            lineTo(tip[0] - ux * hl - uy * hw, tip[1] - uy * hl + ux * hw)
            lineTo(tip[0] - ux * hl + uy * hw, tip[1] - uy * hl - ux * hw)
            close()
        }
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = argb(colour) })
    }

    /**
     * THE LINES AND TAPES ON A SURFACE (sp.js drawMeas, drawTapes): the Lines and areas first, then the tapes, then
     * whatever is being drawn -- a Line pulled (dashed), an area's corners so far (dashed, open), a tape pulled (its
     * band translucent) -- in red where it has left the surface. Everything is sized in dp ([Marks.dp]).
     */
    private fun drawMeas(c: Canvas, s: String, o: Marks, m: (Double, Double) -> FloatArray, u: Float) {
        val sq = o.sq; val dp = o.dp
        fun toS(p: DoubleArray) = m(sq.x(p[0]), sq.y(p[1]))
        // canvas px per face mm: a squared face has one scale both ways
        val z0 = m(sq.x(0.0), sq.y(0.0)); val z1 = m(sq.x(100.0), sq.y(0.0))
        val ppm = hypot(z1[0] - z0[0], z1[1] - z0[1]) / 100f
        val sf = o.prep.surface(s)
        // the HELD item's ends are handles on the editing photo with no tool on (v52 slice 10; port 63 slice I1b)
        val hd = o.ends && o.mode == "main"
        // ON THE EDITING PHOTO NO TAG IS DRAWN HERE (port 63 slice I1b): a plotted Line's, area's and Tape's figure is the
        // number alone, placed along its line with every other figure (PhotoFigures.items, drawn by drawPlates); a Line
        // being pulled and an area being tapped show no figure (their length and m² are the photo's), a Tape being pulled
        // its length alone. A saved picture's tag is what it always was.
        val quiet = o.mode == "main"
        for (q in sf.meas) if (q.kind == SurveyMeas.LINE || q.kind == SurveyMeas.AREA)
            line(c, q.kind, q.pts, q.code, o.selId == q.id, false, false, SurveyPrep.isLocked(q), dp, ::toS, hd,
                figure = if (o.mode == "main") SurveyMeas.key(q) else null, o = o, u = u, quiet = quiet,
                solid = { i -> NudgePad.solid(o.nudge, q.id, NudgePad.pointPart(i)) })
        o.area?.takeIf { it.isNotEmpty() }?.let { line(c, "open", it, null, false, true, false, false, dp, ::toS, quiet = quiet) }
        o.drag?.takeIf { it.s == s && it.kind == SurveyMeas.LINE }?.let { d ->
            line(c, SurveyMeas.LINE, d.pts(), null, false, true, d.off, false, dp, ::toS, quiet = quiet) }
        for (q in sf.meas) if (q.kind == SurveyMeas.TAPE && q.pts.size == 2)
            tape(c, q.pts[0], q.pts[1], q.code, o.selId == q.id, false, false, SurveyPrep.isLocked(q), dp, ppm, o, ::toS, hd, quiet = quiet,
                solid = { i -> NudgePad.solid(o.nudge, q.id, NudgePad.pointPart(i)) })
        o.drag?.takeIf { it.s == s && it.kind == SurveyMeas.TAPE }?.let { d ->
            tape(c, d.from, d.to, null, false, true, d.off, false, dp, ppm, o, ::toS, quiet = quiet) }
    }

    /**
     * A LINE (sp.js drawMeas): 2.4 dp, 3.5 when picked, with an arrow head at each end (none on a line under three
     * heads, 21 dp), labelled "F-M1 1234 mm"; an AREA outlined and filled at 0x26 with its corners as dots and "F-M2
     * 1.86 m²"; a draft dashed 7 / 5 dp ("1234 mm" while pulled, "1.86 m²…" from an area's third corner).
     */
    private fun line(c: Canvas, kind: String, pts: List<DoubleArray>, code: String?, sel: Boolean, draft: Boolean, off: Boolean,
                     locked: Boolean, dp: Float, toS: (DoubleArray) -> FloatArray, handles: Boolean = false,
                     /** Port 63 slice M2, the editing photo only: the reading key of a plotted Line or area (F-M1.len,
                      *  F-M2.per) -- its tag then shows its ONE figure and takes a tap; null: the tag as it always was. */
                     figure: String? = null, o: Marks? = null, u: Float = dp,
                     /** Port 63 slice I1b, the editing photo: no tag -- the figure is placed with the others (drawPlates). */
                     quiet: Boolean = false,
                     /** Whether the handle at point i is drawn SOLID: Fine move nudges it (NudgePad.solid). */
                     solid: (Int) -> Boolean = { false }) {
        if (pts.isEmpty()) return
        val closed = kind == SurveyMeas.AREA; val isLine = kind == SurveyMeas.LINE
        val col = if (draft && off) OUT_COLOUR else LINE_C
        val scr = pts.map(toS)
        // THE LINE OR AREA BEING MEASURED is gold over a dark casing, as a mark's dimension is (MarkDims.ACTIVE_COLOUR)
        val meas = !draft && figure != null && o != null && figure == o.active
        val ink = if (meas) MarkDims.ACTIVE_COLOUR else col
        val case = if (meas) stroke(argb(MarkDims.ACTIVE_CASE, MarkDims.ACTIVE_CASE_ALPHA), 7f * dp).apply { strokeCap = Paint.Cap.ROUND } else null
        val p = stroke(argb(ink), (if (meas) 4f else if (sel) 3.5f else 2.4f) * dp).apply { strokeCap = Paint.Cap.ROUND }
        if (draft) p.pathEffect = DashPathEffect(floatArrayOf(7f * dp, 5f * dp), 0f)
        if (closed && !draft && scr.size >= 3) {
            val fill = Path().apply { moveTo(scr[0][0], scr[0][1]); for (i in 1 until scr.size) lineTo(scr[i][0], scr[i][1]); close() }
            c.drawPath(fill, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = argb(LINE_C, 0x26) })
        }
        val n = if (closed) scr.size else scr.size - 1
        for (i in 0 until n) {
            val a = scr[i]; val b = scr[(i + 1) % scr.size]
            if (case != null) c.drawLine(a[0], a[1], b[0], b[1], case)
            c.drawLine(a[0], a[1], b[0], b[1], p)
            if (isLine && hypot(b[0] - a[0], b[1] - a[1]) >= 3 * SurveyMeas.HEAD_LEN_DP.toFloat() * dp) {
                head(c, a, b, ink, dp); head(c, b, a, ink, dp)
            }
        }
        // an area's corners, and the corners tapped so far, as white dots ringed in the Line's blue; the picked Line's
        // ends and area's corners, with Select on, as HANDLES -- 5.5 dp, grabbed within 14 dp (v52 slice 10, sp.js drawMeas)
        val hd = sel && !draft && handles
        // -- and THE END FINE MOVE NUDGES SOLID, the colours turned round (every end when it moves the whole item)
        if (!isLine || hd) scr.forEachIndexed { i, q ->
            val rr = (if (hd) SurveyGuide.END_R_DP.toFloat() else 3.2f) * dp
            val full = hd && solid(i)
            c.drawCircle(q[0], q[1], rr, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(if (full) LINE_C else 0xffffff) })
            c.drawCircle(q[0], q[1], rr, stroke(argb(if (full) 0xffffff else LINE_C), 1.5f * dp))
        }
        val segs = if (n > 0) (0 until n).flatMap { listOf(scr[it], scr[(it + 1) % scr.size]) } else scr
        val cx = segs.map { it[0] }.average().toFloat(); val cy = segs.map { it[1] }.average().toFloat()
        // a saved picture's tag is what it always was: "F-M1 1234 mm", an area's m² and its perimeter once read
        if (!quiet) {
            val tag = SurveyMeas.lineTag(code, if (kind == "open") SurveyMeas.AREA else kind, pts)
            if (tag.isNotEmpty()) label(c, tag, cx + (if (locked) 30f else 6f) * dp, cy - 6f * dp, 0xe0f7ff, 12f * dp, dp)
        }
        if (locked && !draft) padPill(c, cx + 26f * dp, cy - 21f * dp, LINE_C, dp)
    }

    /**
     * A TAPE (sp.js drawTapes): a yellow band 25 mm wide on the face -- at least 3.5 dp either side of its middle, so
     * it can be seen and tapped -- ringed white when picked, graduated from its start (a tick every 100 mm, a longer
     * one with its figure every 500 mm: SurveyMeas.tapeTicks), and its tag at the far end: "1234 mm" while it is
     * pulled, "F-T1 1234 mm" once plotted, kept on the canvas -- moved to the side of the band when the end is at an edge.
     */
    private fun tape(c: Canvas, a: DoubleArray, b: DoubleArray, code: String?, sel: Boolean, draft: Boolean, off: Boolean,
                     locked: Boolean, dp: Float, ppm: Float, o: Marks, toS: (DoubleArray) -> FloatArray, handles: Boolean = false,
                     /** Port 63 slice I1b, the editing photo: a plotted Tape's figure is placed with the others (drawPlates),
                      *  the number alone along its band; one being pulled shows its length alone, `1234` (P4). */
                     quiet: Boolean = false,
                     /** Whether the handle at end i is drawn SOLID: Fine move nudges it (NudgePad.solid). */
                     solid: (Int) -> Boolean = { false }) {
        val len = hypot(b[0] - a[0], b[1] - a[1])
        val pa = toS(a); val pb = toS(b)
        val col = if (off) OUT_COLOUR else TAPE_C
        if (len > 0) {
            val d = doubleArrayOf((b[0] - a[0]) / len, (b[1] - a[1]) / len); val nv = doubleArrayOf(-d[1], d[0])
            fun at(k: Double, w: Double) = toS(doubleArrayOf(a[0] + d[0] * k + nv[0] * w, a[1] + d[1] * k + nv[1] * w))
            val hw = SurveyMeas.bandHalfMm(ppm.toDouble(), dp.toDouble())
            val cs = listOf(at(0.0, hw), at(len, hw), at(len, -hw), at(0.0, -hw))
            val band = Path().apply { moveTo(cs[0][0], cs[0][1]); for (i in 1 until 4) lineTo(cs[i][0], cs[i][1]); close() }
            if (sel) c.drawPath(band, stroke(argb(0xffffff), 5f * dp).apply { strokeJoin = Paint.Join.ROUND })
            c.drawPath(band, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = argb(col, if (draft) 0xc8 else 0xf0) })
            c.drawPath(band, stroke(argb(0x3b2f00), 1f * dp))
            for (tk in SurveyMeas.tapeTicks(len)) {
                // too close together to read: only every 500 mm
                if (!tk.major && ppm * 100 < 4f * dp) continue
                val p = at(tk.at, hw); val q = at(tk.at, hw - 2 * hw * (if (tk.major) .62 else .36))
                c.drawLine(p[0], p[1], q[0], q[1], stroke(argb(0x1d1a0a), (if (tk.major) 1.4f else .9f) * dp))
                if (tk.figure && ppm > 0f) {
                    val t = at(tk.at, -hw - 9f * dp / ppm)
                    val txt = tk.at.roundToInt().toString()
                    val tw = Paint().apply { textSize = 9f * dp; isFakeBoldText = true }.measureText(txt)
                    label(c, txt, t[0] - tw / 2, t[1] + 3f * dp, TAPE_C, 9f * dp, dp)
                }
            }
        }
        // the picked tape's two ends, with Select on: white handles, 5.5 dp, ringed dark (v52 slice 10, sp.js drawTapes)
        // -- and THE END FINE MOVE NUDGES SOLID, the colours turned round (both ends when it moves the whole tape)
        if (sel && !draft && handles) listOf(pa, pb).forEachIndexed { i, p ->
            val full = solid(i)
            c.drawCircle(p[0], p[1], SurveyGuide.END_R_DP.toFloat() * dp, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(if (full) 0x3b2f00 else 0xffffff) })
            c.drawCircle(p[0], p[1], SurveyGuide.END_R_DP.toFloat() * dp, stroke(argb(if (full) 0xffffff else 0x3b2f00), 2f * dp))
        }
        if (quiet && !draft) {
            // its figure is drawn by drawPlates; its padlock stays beside its far end, where the tag stood
            if (locked) padPill(c, pb[0] + 8f * dp, pb[1] - 13f * dp, TAPE_C, dp)
            return
        }
        val txt = if (quiet) SurveyMeas.tapeShown(len) else SurveyMeas.tapeTag(if (draft) null else code, len)
        val tw = Paint().apply { textSize = 12f * dp; isFakeBoldText = true }.measureText(txt)
        val pw = if (locked && !draft) 26f * dp else 0f
        // past the far end, kept on the canvas -- or beside it, above the band, when there is no room (SurveyMeas.tapeTagAt)
        val k = SurveyMeas.tapeTagAt(doubleArrayOf(pa[0].toDouble(), pa[1].toDouble()), doubleArrayOf(pb[0].toDouble(), pb[1].toDouble()),
            tw.toDouble(), pw.toDouble(), dp.toDouble(), o.width.toDouble(), o.height.toDouble()).let { floatArrayOf(it[0].toFloat(), it[1].toFloat()) }
        label(c, txt, k[0], k[1], if (off) 0xffd0d0 else 0xfff7c2, 12f * dp, dp)
        if (pw > 0f) padPill(c, k[0] + tw + pw, k[1] - 13f * dp, TAPE_C, dp)
    }

    // ------------------------------------------------------------ v52 slice 11: the chains

    /**
     * THE CHAINS ON A SURFACE (sp.js drawChains), then the one being drawn: see [chain]. Every figure comes from
     * SurveyChain.vals -- the readings, the laser's total, the derived segment -- so the photo says what the list says.
     */
    private fun drawChains(c: Canvas, tx: Tx, s: String, o: Marks, m: (Double, Double) -> FloatArray, u: Float) {
        val sq = o.sq
        fun toS(p: DoubleArray) = m(sq.x(p[0]), sq.y(p[1]))
        for (k in o.prep.surface(s).chains) if (k.nodes.isNotEmpty())
            chain(c, tx, k.nodes, k.axis, k.code, SurveyChain.vals(k, o.prep.actual, o.a, o.b), o.selId == k.id, false,
                SurveyPrep.isLocked(k), o, ::toS, quiet = o.mode == "main")
        o.chain?.takeIf { it.s == s && it.nodes.isNotEmpty() }?.let { d ->
            chain(c, tx, d.nodes, d.axis ?: "x", null, null, false, true, false, o, ::toS, quiet = o.mode == "main") }
    }

    /**
     * ONE CHAIN (sp.js drawChains; Amit, 13 Oct 2026 19:20 IST, the "divider"): its line in rose, 2.6 dp (3.6 picked),
     * an arrow head at both ends of every segment, a draft dashed 7 / 5 dp; a tick across it at every point (10 dp
     * either side) and the point a white dot ringed in rose -- the handle a finger finds within 16 dp; each segment's
     * figure 15 dp off the chain -- its reading white, a derived one violet with " d", else the photo's "≈905" grey; the
     * total under the last point, "FK1 Σ 3310 laser" (amber when the readings disagree with it), kept on the canvas; a
     * locked chain's padlock over its first point. Sized in dp ([Marks.dp]).
     */
    private fun chain(c: Canvas, tx: Tx, nodes: List<DoubleArray>, axis: String, code: String?, v: SurveyChain.Vals?, sel: Boolean,
                      draft: Boolean, locked: Boolean, o: Marks, toS: (DoubleArray) -> FloatArray,
                      /** Port 63 slice I1b, the editing photo: no figure here -- a chain's read segments and total are placed
                       *  with every other figure, the number alone (drawPlates), and one being drawn shows none. */
                      quiet: Boolean = false) {
        val dp = o.dp; val col = SurveyChain.COLOUR
        val ai = SurveyChain.axisIndex(axis)
        // across the chain, in the face's mm: up for a chain along it, right for one up it
        val perp = if (ai == 1) doubleArrayOf(1.0, 0.0) else doubleArrayOf(0.0, 1.0)
        val scr = nodes.map(toS)
        val ln = stroke(argb(col), ((if (sel) SurveyChain.LINE_SEL_DP else SurveyChain.LINE_DP) * dp).toFloat())
        if (draft) ln.pathEffect = DashPathEffect(floatArrayOf((SurveyChain.DASH_DP * dp).toFloat(), (SurveyChain.GAP_DP * dp).toFloat()), 0f)
        for (i in 1 until scr.size) {
            val a = scr[i - 1]; val b = scr[i]
            c.drawLine(a[0], a[1], b[0], b[1], ln)
            if (hypot(b[0] - a[0], b[1] - a[1]) >= 3 * SurveyMeas.HEAD_LEN_DP.toFloat() * dp) { head(c, a, b, col, dp); head(c, b, a, col, dp) }
        }
        if (locked && !draft) padPill(c, scr[0][0] + 8f * dp, scr[0][1] - 30f * dp, col, dp)
        val tick = stroke(argb(col), (SurveyChain.TICK_W_DP * dp).toFloat())
        val r = ((if (sel) SurveyChain.NODE_R_SEL_DP else SurveyChain.NODE_R_DP) * dp).toFloat()
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(0xffffff) }
        val ring = stroke(argb(col), (SurveyChain.NODE_RING_DP * dp).toFloat())
        for (j in nodes.indices) {
            val p = scr[j]; val n = nodes[j]
            val q = toS(doubleArrayOf(n[0] + perp[0] * 100, n[1] + perp[1] * 100))
            val dx = q[0] - p[0]; val dy = q[1] - p[1]; val l = hypot(dx, dy).takeIf { it > 0f } ?: 1f
            val kk = (SurveyChain.TICK_DP * dp).toFloat() / l
            c.drawLine(p[0] - dx * kk, p[1] - dy * kk, p[0] + dx * kk, p[1] + dy * kk, tick)
            c.drawCircle(p[0], p[1], r, dot); c.drawCircle(p[0], p[1], r, ring)
        }
        if (quiet) return
        val fs = 12f * dp
        val fp = Paint().apply { textSize = fs; isFakeBoldText = true }
        val off = (SurveyChain.FIGURE_OFF_DP * dp).toFloat()
        for (i in 1 until nodes.size) {
            val mid = doubleArrayOf((nodes[i - 1][0] + nodes[i][0]) / 2, (nodes[i - 1][1] + nodes[i][1]) / 2)
            val p = toS(mid); val q = toS(doubleArrayOf(mid[0] + perp[0] * 100, mid[1] + perp[1] * 100))
            var dx = q[0] - p[0]; var dy = q[1] - p[1]; val l = hypot(dx, dy)
            if (l > 0f) { dx /= l; dy /= l } else { dx = 0f; dy = -1f }
            val (t0, tc) = if (v != null) SurveyChain.segFigure(v, i - 1)
                          else ("≈" + abs(nodes[i][ai] - nodes[i - 1][ai]).roundToInt()) to SurveyChain.PHOTO_COLOUR // text:value the ≈ glyph, a figure
            val w = fp.measureText(t0)
            label(c, t0, p[0] + dx * off - w / 2, p[1] + dy * off + 4f * dp, tc, fs, dp)
        }
        if (v != null && code != null) {
            val p = scr[scr.size - 1]
            val t = SurveyChain.totalTag(tx, code, v)
            val ts = 11f * dp
            val tw = Paint().apply { textSize = ts; isFakeBoldText = true }.measureText(t)
            // kept inside the picture (sp.js: "kept inside the picture")
            val x = max(4f * dp, minOf(o.width - tw - 6f * dp, p[0] + 8f * dp))
            label(c, t, x, p[1] + 20f * dp, SurveyChain.totalColour(v), ts, dp)
        }
    }

    // ------------------------------------------------------------ one figure on every dimension (port 63 slice M2)

    /** A reading is written bold and an estimate regular -- both monospace, so a figure is as wide as its digits. */
    private val MONO: Typeface = Typeface.MONOSPACE

    /** Between a small box's two figures on its one compact plate. */
    private const val TIMES = " × " // text:value the multiplication glyph between two figures, the same in every language

    /** What is only the photo's is written in this grey: an estimate's dot, the compact plate's " × ". */
    private const val QUIET_INK = 0xcfd3cb

    /**
     * EVERY FIGURE ON THE EDITING PHOTO, PLACED (port 63 slices I1a-I1c; DESIGN-imagemeter §2.2, §18, §21). WHICH figures
     * are drawn and what each says is PhotoFigures.layout -- every READ figure of every mark, its size AND where it sits,
     * always (P3); the picked mark's unread ones as a quiet `?`; the gold one H1's plate, `▶?` until the reading lands
     * (`▷` while the laser is not ready); NO PHOTO ESTIMATE ANYWHERE (§18); and every Line's, area's, Tape's and chain's
     * figure, the number alone (P4). WHERE each plate goes is DimLayout.placeShown -- along its own line, turned to it
     * (P5), kept apart from every other by its turned rectangle, clear of the marks' [codes], the held mark's tabs and
     * [Marks.keep]. This only measures the texts with the paints it draws them with, asks, collects the tap targets
     * ([Marks.tags]) and hands the plates back to [drawPlates]. Nothing here decides a figure.
     */
    private fun figures(c: Canvas, tx: Tx, s: String, o: Marks, m: (Double, Double) -> FloatArray, u: Float,
                        codes: List<SurveyPrepTouch.Box>): DimLayout.Out {
        val sq = o.sq; val dp = o.dp.toDouble(); val du = u.toDouble()
        val toPx: (Double, Double) -> DoubleArray = { x, y -> m(sq.x(x), sq.y(y)).let { at -> doubleArrayOf(at[0].toDouble(), at[1].toDouble()) } }
        val textW: (String, Double, Boolean) -> Double = { t, fs, heavy -> pen(fs, heavy).measureText(t).toDouble() }
        val pk = o.pick ?: Pick.State(o.selId, false, o.active)
        val laid = PhotoFigures.layout(tx, o.prep, s, o.a, o.b, pk, toPx, du, dp, textW, o.live, o.ready, ask = o.ask)
        val times = textW(TIMES, DimLayout.textSize(false, dp), false)
        val out = DimLayout.placeShown(laid, o.width.toDouble(), o.height.toDouble(), codes + o.keep, du, dp, times)
        o.tags?.addAll(out.tags)
        return out
    }

    /** The two paints a plate's text is drawn and measured with: monospace, bold for a reading and the gold one. */
    private val PEN_REGULAR = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = MONO }
    private val PEN_BOLD = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = MONO_BOLD }
    private fun pen(fs: Double, heavy: Boolean): Paint = Paint(if (heavy) PEN_BOLD else PEN_REGULAR).apply { textSize = fs.toFloat() }

    /**
     * [plates] DRAWN: each mark figure's line and arrowheads first -- the gold one's over a dark casing, three times as
     * heavy (MarkDims.ACTIVE_COLOUR, H1), so which side the laser is asked for is seen at a glance -- then the plates over
     * them. A Line's, area's, Tape's or chain's figure (Role.ITEM) draws no dimension line -- the Line IS its line -- only
     * its leader when it stepped off. [fade]: one mark is picked and the others' figures are drawn at 75 % with them.
     */
    private fun drawPlates(c: Canvas, plates: List<DimLayout.Plate>, o: Marks, u: Float, fade: Boolean) {
        if (plates.isEmpty()) return
        val dp = o.dp.toDouble(); val du = u.toDouble()
        val times = pen(DimLayout.textSize(false, dp), false).measureText(TIMES).toDouble()
        val ln = stroke(argb(MarkDims.LINE_COLOUR, MarkDims.LINE_ALPHA), (DimLayout.LINE_U * du).toFloat())
        val tip = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = argb(MarkDims.LINE_COLOUR, MarkDims.LINE_ALPHA) }
        val lnCase = stroke(argb(MarkDims.ACTIVE_CASE, MarkDims.ACTIVE_CASE_ALPHA), (DimLayout.LINE_U * du * MarkDims.ACTIVE_LINE_CASE_X).toFloat())
        val tipCase = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = argb(MarkDims.ACTIVE_CASE, MarkDims.ACTIVE_CASE_ALPHA) }
        val lnOn = stroke(argb(MarkDims.ACTIVE_COLOUR), (DimLayout.LINE_U * du * MarkDims.ACTIVE_LINE_X).toFloat())
        val tipOn = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = argb(MarkDims.ACTIVE_COLOUR) }
        fun pass(ps: List<DimLayout.Plate>) {
            for (p in ps) {
                if (p.role == MarkDims.Role.ITEM) { p.leader?.let { g -> c.drawLine(g.x1.toFloat(), g.y1.toFloat(), g.x2.toFloat(), g.y2.toFloat(), ln) }; continue }
                if (p.fig.active) { dimLine(c, p, lnCase, tipCase, du); dimLine(c, p, lnOn, tipOn, du) } else dimLine(c, p, ln, tip, du)
            }
            for (p in ps) if (p.dot) dot(c, p, dp) else plate(c, p, dp, times, p.code == o.ring)
        }
        // the picked mark's figures last and at full strength; the others' with their marks, at 75 % while one is picked
        val (near, far) = plates.partition { it.markId == o.selId }
        if (far.isNotEmpty()) {
            if (fade) c.saveLayerAlpha(null, FADE)
            pass(far)
            if (fade) c.restore()
        }
        pass(near)
    }

    /**
     * A DIMENSION'S LINE: white at alpha 0xcc, 1 unit wide, an arrowhead at each end (the line itself is DimLayout's). The
     * plate that sits on it BREAKS it -- along the line, by the plate's own length, whatever the line's slope (P5: the
     * plate is turned along it); one that stepped off it hangs on its short leader; where the span is too short for its
     * plate the arrowheads stand outside, pointing in. A small box's two figures share one compact plate beside it and
     * draw no line; a `p` has none.
     */
    private fun dimLine(c: Canvas, p: DimLayout.Plate, ln: Paint, tip: Paint, u: Double) {
        val l = p.line ?: return
        if (p.how == DimLayout.How.COMPACT) return
        val x1 = l.x1.toFloat(); val y1 = l.y1.toFloat(); val x2 = l.x2.toFloat(); val y2 = l.y2.toFloat()
        val len = hypot(x2 - x1, y2 - y1)
        if (p.how == DimLayout.How.ON_LINE && len > 0f) {
            val ux = (x2 - x1) / len; val uy = (y2 - y1) / len
            val b = p.box; val cx = ((b.l + b.r) / 2).toFloat(); val cy = ((b.t + b.b) / 2).toFloat()
            val t = (cx - x1) * ux + (cy - y1) * uy; val half = (p.pw / 2).toFloat()
            if (t - half > 0f) c.drawLine(x1, y1, x1 + ux * (t - half), y1 + uy * (t - half), ln)
            if (t + half < len) c.drawLine(x1 + ux * (t + half), y1 + uy * (t + half), x2, y2, ln)
        } else c.drawLine(x1, y1, x2, y2, ln)
        p.leader?.let { g -> c.drawLine(g.x1.toFloat(), g.y1.toFloat(), g.x2.toFloat(), g.y2.toFloat(), ln) }
        val a = (DimLayout.ARROW_U * u).toFloat()
        arrowHead(c, x1, y1, x2, y2, a, p.arrowsIn, tip); arrowHead(c, x2, y2, x1, y1, a, p.arrowsIn, tip)
    }

    /** An arrowhead [len] long with its tip at ([ax], [ay]), on the line towards ([bx], [by]): inside the span, or --
     *  [outside] -- beyond its end, pointing in. */
    private fun arrowHead(c: Canvas, ax: Float, ay: Float, bx: Float, by: Float, len: Float, outside: Boolean, p: Paint) {
        val dx = bx - ax; val dy = by - ay; val l = hypot(dx, dy)
        if (!(l > 0f)) return
        val k = if (outside) -1f else 1f
        val ux = dx / l * k; val uy = dy / l * k; val w = len * .4f
        c.drawPath(Path().apply {
            moveTo(ax, ay)
            lineTo(ax + ux * len - uy * w, ay + uy * len + ux * w)
            lineTo(ax + ux * len + uy * w, ay + uy * len - ux * w)
            close()
        }, p)
    }

    /**
     * ONE FIGURE'S PLATE, in its look's style (MarkDims.plate; DESIGN-imagemeter §18): a reading white and bold on solid
     * black; not read, on the picked item, a quiet `?` grey inside a dashed grey outline; the one being measured the gold
     * plate. Its text is the figure as PhotoFigures prints it -- `▶` (or `▷`) before the gold one, `⊥ ` before a depth --
     * and after a reading that fails the check an amber `!`. THE PLATE IS TURNED TO ITS LINE (P5): drawn level, [pw] along
     * its text by [ph] across, about its own middle, then turned by [DimLayout.Plate.angle] -- never upside down
     * (LabelAngle). A small box's compact plate is its two halves, level, with " × " between them. [ringed]: it has just
     * taken a reading, and keeps a gold ring for a second.
     */
    private fun plate(c: Canvas, p: DimLayout.Plate, dp: Double, times: Double, ringed: Boolean) {
        val f = p.fig; val st = MarkDims.plate(f.state, f.active); val b = p.box
        val cx = ((b.l + b.r) / 2).toFloat(); val cy = ((b.t + b.b) / 2).toFloat()
        val turned = p.how != DimLayout.How.COMPACT && p.angle != 0.0
        val w = if (p.how == DimLayout.How.COMPACT) b.w else p.pw; val h = if (p.how == DimLayout.How.COMPACT) b.h else p.ph
        val up = SurveyPrepTouch.Box(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
        if (turned) { c.save(); c.rotate(p.angle.toFloat(), cx, cy) }
        val box = RectF(up.l.toFloat(), up.t.toFloat(), up.r.toFloat(), up.b.toFloat())
        val r = (DimLayout.RADIUS_DP * dp).toFloat()
        c.drawRoundRect(box, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(st.fill, st.fillAlpha) })
        st.outline?.let { ring ->
            c.drawRoundRect(box, r, r, stroke(argb(ring), ((if (f.active) 1.5 else 1.0) * dp).toFloat()).apply {
                if (st.dashed) pathEffect = DashPathEffect(floatArrayOf((3 * dp).toFloat(), (2 * dp).toFloat()), 0f) })
        }
        // THE PLATE THAT HAS JUST TAKEN A READING rings gold for a second (Landing.RING_MS), so the eye sees where it went
        if (ringed) {
            val g = (2 * dp).toFloat()
            c.drawRoundRect(RectF(box.left - g, box.top - g, box.right + g, box.bottom + g), r + g, r + g,
                stroke(argb(MarkDims.ACTIVE_COLOUR), (2 * dp).toFloat()))
        }
        val ink = pen(f.fs, st.bold).apply { color = argb(st.ink) }
        val x = DimLayout.textLeft(up, dp).toFloat(); val y = DimLayout.baseline(up, dp).toFloat()
        c.drawText(f.text, x, y, ink)
        if (f.check) {
            val past = ink.measureText(f.text)
            c.drawText(MarkDims.CHECK_GLYPH, x + past, y, ink.apply { color = argb(MarkDims.checkInk(f.active)) })
        }
        if (turned) c.restore()
        // the compact plate's " × ", once, after its first half
        if (p.how == DimLayout.How.COMPACT && p.role == MarkDims.Role.WIDTH) {
            c.drawRect(b.r.toFloat(), b.t.toFloat(), (b.r + times).toFloat(), b.b.toFloat(), Paint().apply { color = argb(0x000000, 0x88) })
            c.drawText(TIMES, b.r.toFloat(), y, pen(DimLayout.textSize(false, dp), false).apply { color = argb(QUIET_INK) })
        }
    }

    /** A figure with no room for its plate: a 6 dp dot on its line -- solid once measured, hollow until then. */
    private fun dot(c: Canvas, p: DimLayout.Plate, dp: Double) {
        val b = p.box; val cx = ((b.l + b.r) / 2).toFloat(); val cy = ((b.t + b.b) / 2).toFloat(); val r = (b.w / 2).toFloat()
        // the one being measured, should it ever have no room for its plate: a gold dot in a dark ring
        if (p.fig.active) {
            c.drawCircle(cx, cy, r + (1.5 * dp).toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(MarkDims.ACTIVE_CASE, MarkDims.ACTIVE_CASE_ALPHA) })
            c.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(MarkDims.ACTIVE_COLOUR) })
            return
        }
        if (p.measured) c.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(0xffffff) })
        else {
            c.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(0x000000, 0x88) })
            c.drawCircle(cx, cy, r, stroke(argb(QUIET_INK), (1.2 * dp).toFloat()))
        }
    }

    // ------------------------------------------------------------ a saved picture's figures (mode "sheet"), until slice M3

    /** Dimension lines for one mark, each with its estimate and / or reading -- A SAVED PICTURE'S ONLY (FaceWriter,
     *  mode "sheet") since port 63 slice M2: the editing photo draws [figures]. Untouched, so those pictures are byte
     *  for byte what they were until Amit has ruled on them (DESIGN-port63 question 1). */
    private fun dims(c: Canvas, s: String, e: SurveyPrep.Mark, o: Marks, m: (Double, Double) -> FloatArray,
                     p: FloatArray, q: FloatArray, fs: Float, u: Float) {
        val ms = SurveyPrep.measures(s, e, o.a, o.b).associateBy { it.key }
        fun act(k: String) = o.prep.actual[e.code + "." + k]
        val ln = stroke(argb(0xffffff, 0xcc), if (fs > 15f * u) 2.5f * u else 1f * u)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = c.drawLine(x1, y1, x2, y2, ln)
        fun tag(k: String, x: Float, y: Float, left: Boolean = false) = vtag(c, x, y, ms.getValue(k).est, act(k), fs, e.code + "." + k, o, u, left)
        val midY = (p[1] + q[1]) / 2; val midX = (p[0] + q[0]) / 2; val cE = SurveyPrep.isPoint(e.t)
        // v52: a point's figures run to its centre under its own keys (cl, cr, cs, ct, cf, ck; a plan's Electric
        // point keeps l, r, f, k) -- each line is drawn under whichever key the mark HAS, with that key's tag
        fun kOf(k: String): String? = if (k in ms) k else if ("c$k" in ms) "c$k" else null
        val xL = m(o.sq.x(0.0), 0.0)[0]; val xR = m(o.sq.x(o.a), 0.0)[0]
        val yT = m(0.0, o.sq.y(o.b))[1]; val yB = m(0.0, o.sq.y(0.0))[1]
        // v52 item 14: on the picked mark (the one showing its eight tabs) the width tag stands higher and the
        // height tag starts right of the edge, each clear of the tabs (SurveyPrepTouch.widthTag / heightTag)
        val hand = o.mode == "main" && o.selId == e.id && !cE && !SurveyPrep.isLocked(e)
        val du = u.toDouble(); val dfs = fs.toDouble(); val ddp = o.dp.toDouble()
        if ("w" in ms) {
            line(p[0], p[1] - 6f * u, q[0], p[1] - 6f * u)
            val a = SurveyPrepTouch.widthTag(p[0].toDouble(), p[1].toDouble(), q[0].toDouble(), dfs, du, ddp, hand)
            tag("w", a.x.toFloat(), a.y.toFloat(), a.left)
        }
        if ("h" in ms) {
            line(q[0] + 6f * u, p[1], q[0] + 6f * u, q[1])
            val a = SurveyPrepTouch.heightTag(p[1].toDouble(), q[0].toDouble(), q[1].toDouble(), dfs, du, ddp, hand)
            tag("h", a.x.toFloat(), a.y.toFloat(), a.left)
        }
        val hy = if (cE) midY else midY + fs * 2.6f
        kOf("l")?.let { k -> val x = if (cE) midX else p[0]; line(xL, hy, x, hy); tag(k, (xL + x) / 2, hy - 4f * u) }
        kOf("r")?.let { k -> val x = if (cE) midX else q[0]; line(x, hy, xR, hy); tag(k, (x + xR) / 2, hy - 4f * u) }
        val vx = if (cE) midX else midX - fs * 1.6f
        for (k0 in listOf("s", "k")) kOf(k0)?.let { k -> val y = if (cE) midY else q[1]; line(vx, y, vx, yB); tag(k, vx, (y + yB) / 2 + 4f * u) }
        for (k0 in listOf("t", "f")) kOf(k0)?.let { k -> val y = if (cE) midY else p[1]; line(vx, yT, vx, y); tag(k, vx, (yT + y) / 2 + 4f * u) }
        if ("d" in ms) { line(midX, yT, midX, q[1]); tag("d", midX, q[1] + fs * 1.4f) }
    }

    fun sgn(d: Double): String { val r = d.roundToInt(); return (if (r > 0) "+" else if (r < 0) "−" else "±") + abs(r) } // text:value sign glyphs on a figure

    /** A value tag: photo estimate and / or site reading, per the Show toggle; gold when it is the ACTIVE one.
     *  Centred on [x0], or starting there when [left]; [y0] is its baseline. */
    private fun vtag(c: Canvas, x0: Float, y0: Float, est: Double?, act: Double?, fs: Float, code: String, o: Marks, u: Float, left: Boolean = false) {
        val on = code == o.active
        val parts = mutableListOf<Pair<String, Int>>()
        if (o.show != "actual") parts.add((if (est != null) "≈" + est.roundToInt() else "—") to 0xcfd3cb) // text:value glyphs on a figure
        if (o.show != "derived") {
            if (o.show == "both") parts.add(" | " to 0x888888) // text:value the divider glyph
            parts.add((if (act != null) act.roundToInt().toString() else "?") to 0xffffff) // text:value the unknown glyph, a figure
            if (o.show == "both" && act != null && est != null) {
                val d = act - est; parts.add((" " + sgn(d)) to (if (abs(d) <= SurveyPrep.TOL) 0x7dff8a else 0xffc46b)) // text:value a space before the sign
            }
        }
        if (on && parts.isNotEmpty()) parts[0] = ("▶" + parts[0].first) to parts[0].second // text:value the active-reading glyph
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = fs; typeface = MONO_BOLD }
        val w = parts.sumOf { p.measureText(it.first).toDouble() }.toFloat() + 10f * u
        val sh = SurveyPrepTouch.tagShape(SurveyPrepTouch.Anchor(x0.toDouble(), y0.toDouble(), left), w.toDouble(), fs.toDouble(),
            u.toDouble(), o.width.toDouble(), o.height.toDouble())
        val x = sh.x.toFloat(); val y = sh.baseline.toFloat()
        val box = RectF(sh.drawn.l.toFloat(), sh.drawn.t.toFloat(), sh.drawn.r.toFloat(), sh.drawn.b.toFloat())
        c.drawRoundRect(box, 5f * u, 5f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (on) argb(0x4a3d0c) else argb(0x000000, 0xdd) })
        if (on) c.drawRoundRect(box, 5f * u, 5f * u, stroke(argb(0xe0b040), 2f * u))
        o.tags?.add(SurveyPrepTouch.Tag(code, sh.tap))
        var cx = x + 5f * u
        for ((t, col) in parts) { p.color = argb(col); c.drawText(t, cx, y, p); cx += p.measureText(t) }
    }
}
