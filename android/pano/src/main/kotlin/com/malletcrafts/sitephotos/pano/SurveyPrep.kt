package com.malletcrafts.sitephotos.pano

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * SURVEY PREP: bound each surface, square it to size, mark what is on it.
 * The maths of the room-setup screen as Amit approved it in mock-up v33
 * (2026-10-10), replacing the four-floor-corners-and-a-ceiling-line flow.
 *
 * SIX SURFACES, EACH ON ITS OWN (Amit, 2026-10-10: "keeping relation of each
 * foto ... shared among foto is making things complicated"). Front, right,
 * back and left are level 120-degree views along yaw 0 / 90 / 180 / 270; the
 * floor looks straight down and the ceiling straight up, both 150 degrees
 * square with the FRONT wall at the top and the LEFT wall on the left.
 * Nothing on one surface moves anything on another.
 *
 *  1 Lines -- four boundary lines per surface. Where they cross are the
 *    surface's corners, and a surface is a plane, so those four points and
 *    the laser size (walls: W or L across, H up; floor and ceiling: W by L)
 *    fix a homography between photo pixels and millimetres exactly.
 *  2 Mark -- the surface squared to size, and axis-aligned boxes on it in mm,
 *    each with its own short list of measurements for the site engineer.
 *
 * Millimetres on a surface: x from its left edge, y UP from its bottom edge
 * (walls: the floor line; floor and ceiling: the back edge). The laser L, W
 * and H come from the room capture, and can be changed on the screen (v51).
 *
 * Frame: Panorama's -- x right, y up, z forward (the front wall). Pure JVM,
 * so all of it is tested in this module; the screen only draws and listens.
 */
object SurveyPrep {

    val SURFACES = listOf("front", "right", "back", "left", "floor", "ceil")
    val NAME = mapOf("front" to "Front wall", "right" to "Right wall", "back" to "Back wall",
        "left" to "Left wall", "floor" to "Floor", "ceil" to "Ceiling")
    val TAB = mapOf("front" to "Front", "right" to "Right", "back" to "Back",
        "left" to "Left", "floor" to "Floor", "ceil" to "Ceiling")
    /** The first letter of every mark's code: G for the floor (ground). */
    val CODE = mapOf("front" to "F", "right" to "R", "back" to "B", "left" to "L", "floor" to "G", "ceil" to "C")

    fun isPlan(s: String) = s == "floor" || s == "ceil"

    /** Panorama's face name for a surface, which is what files are labelled by. */
    fun panoFace(s: String) = when (s) { "floor" -> "down"; "ceil" -> "up"; else -> s }

    /** The four boundary lines of every surface. */
    val SLOTS = listOf("bottom", "top", "left", "right")

    fun lineNames(s: String): Map<String, String> =
        if (isPlan(s)) mapOf("bottom" to "back edge", "top" to "front edge", "left" to "left edge", "right" to "right edge")
        else mapOf("bottom" to "floor line", "top" to "ceiling line", "left" to "left corner", "right" to "right corner")

    /** The two lines each line crosses: its corners are there. */
    val CROSSES = mapOf("bottom" to listOf("left", "right"), "top" to listOf("left", "right"),
        "left" to listOf("bottom", "top"), "right" to listOf("bottom", "top"))

    /** A site reading within this of the photo estimate, mm, is shown green. */
    const val TOL = 20.0
    /** Within this of a boundary, mm, a DRAGGED mark snaps onto it. */
    const val SNAP = 25.0
    /** The smallest a mark may be resized to, mm. */
    const val MIN_SIZE = 20.0
    /** An offset under this, mm, is a mark touching that side: nothing to measure. */
    const val ZERO = 1.0
    /** The keys of the position offsets, which drop out when a mark touches. */
    val OFFS = setOf("l", "r", "s", "t", "f", "k")

    // -------------------------------------------------------------- the views

    /** A wall view: 960 x 800, 120 degrees across, level. */
    const val VW = 960
    const val VH = 800
    val TH: Double = tan(PI / 3)
    val TV: Double = TH * VH / VW
    /** Floor and ceiling: 960 square, 150 degrees across. */
    const val PS = 960
    val PT: Double = tan(Math.toRadians(75.0))
    val YAW = mapOf("front" to 0.0, "right" to 90.0, "back" to 180.0, "left" to 270.0)

    /** Width and height of a surface's view, px. */
    fun dimOf(s: String): IntArray = if (isPlan(s)) intArrayOf(PS, PS) else intArrayOf(VW, VH)

    /** The direction through a view pixel (continuous px, 0..width). */
    fun ray(s: String, px: Double, py: Double): DoubleArray {
        if (isPlan(s)) {
            val a = (2 * px / PS - 1) * PT; val b = (1 - 2 * py / PS) * PT
            return doubleArrayOf(a, if (s == "ceil") 1.0 else -1.0, b)
        }
        val p = Math.toRadians(YAW.getValue(s))
        val fx = sin(p); val fz = cos(p); val rx = cos(p); val rz = -sin(p)
        val u = (2 * px / VW - 1) * TH; val v = (1 - 2 * py / VH) * TV
        return doubleArrayOf(fx + u * rx, v, fz + u * rz)
    }

    /** A point (any scale, camera at the origin) to its view pixel; null when behind. */
    fun proj(s: String, x: Double, y: Double, z: Double): DoubleArray? {
        if (isPlan(s)) {
            val k = abs(y); if (k < 1e-6) return null
            return doubleArrayOf((x / k / PT + 1) / 2 * PS, (1 - z / k / PT) / 2 * PS)
        }
        val p = Math.toRadians(YAW.getValue(s))
        val fx = sin(p); val fz = cos(p); val rx = cos(p); val rz = -sin(p)
        val d = x * fx + z * fz
        if (d < 1e-3) return null
        return doubleArrayOf(((x * rx + z * rz) / d / TH + 1) / 2 * VW, (1 - y / d / TV) / 2 * VH)
    }

    /** A surface's view, cut from the 360 at pixel centres. */
    fun cutView(pano: Panorama.Image, s: String): Panorama.Image {
        val (w, h) = dimOf(s).let { it[0] to it[1] }
        val out = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            out[y * w + x] = WallCorners.sample(pano, ray(s, x + 0.5, y + 0.5))
        }
        return Panorama.Image(w, h, out)
    }

    // ------------------------------------------------ the laser size of a surface

    /** The room sizes believed, mm: the size sheet's range (v51, Amit, 14 Oct
     *  2026: "i should be able to edit room outer dimensions details as well";
     *  15 Oct: "Yes, with slice 2"). It was 500 to 20000 before. */
    const val SIZE_MIN = 300.0
    const val SIZE_MAX = 30000.0

    /** A laser reading is believed between these, mm. */
    fun valid(v: Double?): Boolean = v != null && !v.isNaN() && v >= SIZE_MIN && v <= SIZE_MAX

    /** What the size sheet says when a figure is refused. */
    const val SIZE_HINT_BAD = "300 to 30000 mm"
    const val SIZE_HINT = "L front→back · W left→right · H floor→ceiling"

    /**
     * The size sheet's verdict on its three boxes (L, W, H as typed) against
     * the size in use. Each must be a WHOLE number from 300 to 30000 mm;
     * [bad] lists the boxes that are not (0 = L, 1 = W, 2 = H), and then
     * nothing changes. A box left at its prefill (the current figure rounded)
     * keeps the current figure unrounded, so a 3319.5 is not rounded by an
     * Apply that changed something else. [same]: nothing would change.
     */
    class SizeEdit(val size: DoubleArray?, val bad: List<Int>, val same: Boolean)

    fun sizeEdit(typed: List<String>, current: DoubleArray): SizeEdit {
        val out = DoubleArray(3); val bad = mutableListOf<Int>()
        for (i in 0 until 3) {
            val t = typed.getOrElse(i) { "" }.trim()
            val n = if (t.isNotEmpty() && t.all { it in '0'..'9' } && t.length <= 6) t.toInt().toDouble() else Double.NaN
            if (!(n >= SIZE_MIN && n <= SIZE_MAX)) { bad.add(i); continue }
            out[i] = if (n == current[i].roundToInt().toDouble()) current[i] else n
        }
        if (bad.isNotEmpty()) return SizeEdit(null, bad, false)
        return SizeEdit(out, bad, (0 until 3).all { out[it] == current[it] })
    }

    /** A size box's text as it is typed: digits only, at most five. */
    fun sizeText(t: String): String = t.filter { it in '0'..'9' }.take(5)

    /** mm to the inches the capture queue keeps (room_l_in, room_w_in, room_h_in). */
    fun inches(mm: DoubleArray): DoubleArray = DoubleArray(mm.size) { mm[it] / MM_PER_IN }

    /** A mark this far past its face's edge, mm, is outside it. */
    const val OUT_TOL = 0.5

    /** A mark reaching past its face (the room made smaller under it): flagged, never moved. */
    fun outOf(e: Mark, a: Double, b: Double): Boolean =
        e.x0 < -OUT_TOL || e.y0 < -OUT_TOL || e.x1 > a + OUT_TOL || e.y1 > b + OUT_TOL

    fun outNote(code: String) = "$code is outside the face: edit or delete it"

    /** The size a surface squares to: across (A) and up (B), mm. */
    fun size(s: String, l: Double, w: Double, h: Double): DoubleArray =
        if (isPlan(s)) doubleArrayOf(w, l)
        else doubleArrayOf(if (s == "front" || s == "back") w else l, h)

    /** Which laser figure each of A and B is. */
    fun sizeTag(s: String): Pair<String, String> =
        if (isPlan(s)) "W" to "L" else (if (s == "front" || s == "back") "W" else "L") to "H"

    // ---------------------------------------------- squaring a surface to size

    /** Where two lines (each two points) cross; null when parallel. */
    fun cross(l1: Array<DoubleArray>, l2: Array<DoubleArray>): DoubleArray? {
        val p = l1[0]; val q = l1[1]; val r = l2[0]; val t = l2[1]
        val d1x = q[0] - p[0]; val d1y = q[1] - p[1]; val d2x = t[0] - r[0]; val d2y = t[1] - r[1]
        val den = d1x * d2y - d1y * d2x
        if (abs(den) < 1e-9) return null
        val k = ((r[0] - p[0]) * d2y - (r[1] - p[1]) * d2x) / den
        return doubleArrayOf(p[0] + k * d1x, p[1] + k * d1y)
    }

    /** The homography taking four points onto four, as 9 numbers (h33 = 1); null when degenerate. */
    fun homography(src: List<DoubleArray>, dst: List<DoubleArray>): DoubleArray? {
        val a = Array(8) { DoubleArray(9) }
        for (i in 0 until 4) {
            val x = src[i][0]; val y = src[i][1]; val u = dst[i][0]; val v = dst[i][1]
            a[2 * i] = doubleArrayOf(x, y, 1.0, 0.0, 0.0, 0.0, -u * x, -u * y, u)
            a[2 * i + 1] = doubleArrayOf(0.0, 0.0, 0.0, x, y, 1.0, -v * x, -v * y, v)
        }
        for (c in 0 until 8) {
            var p = c
            for (r in c + 1 until 8) if (abs(a[r][c]) > abs(a[p][c])) p = r
            val tmp = a[c]; a[c] = a[p]; a[p] = tmp
            if (abs(a[c][c]) < 1e-12) return null
            for (r in 0 until 8) if (r != c) {
                val f = a[r][c] / a[c][c]
                for (k in c until 9) a[r][k] -= f * a[c][k]
            }
        }
        return DoubleArray(9) { i -> if (i == 8) 1.0 else a[i][8] / a[i][i] }
    }

    /** A homography applied to a point. */
    fun apply(hm: DoubleArray, p: DoubleArray): DoubleArray {
        val w = hm[6] * p[0] + hm[7] * p[1] + hm[8]
        return doubleArrayOf((hm[0] * p[0] + hm[1] * p[1] + hm[2]) / w, (hm[3] * p[0] + hm[4] * p[1] + hm[5]) / w)
    }

    /**
     * A surface's corners in its view (bottom-left, bottom-right, top-right,
     * top-left) and the two maps between view pixels and mm on the surface.
     */
    class Frame(val q: List<DoubleArray>, val a: Double, val b: Double, val p2m: DoubleArray, val m2p: DoubleArray)

    fun frame(lines: Map<String, Array<DoubleArray>>, a: Double, b: Double): Frame? {
        if (!valid(a) || !valid(b)) return null
        val q = listOf(cross(lines.getValue("bottom"), lines.getValue("left")), cross(lines.getValue("bottom"), lines.getValue("right")),
            cross(lines.getValue("top"), lines.getValue("right")), cross(lines.getValue("top"), lines.getValue("left")))
        if (q.any { it == null }) return null
        val qq = q.map { it!! }
        val mm = listOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(a, 0.0), doubleArrayOf(a, b), doubleArrayOf(0.0, b))
        val p2m = homography(qq, mm) ?: return null
        val m2p = homography(mm, qq) ?: return null
        return Frame(qq, a, b, p2m, m2p)
    }

    /**
     * A picture of a surface squared to size: [mX] / [mY] mm of margin beyond
     * its edges, [mpp] mm per pixel the same both ways, [width] x [height] px.
     * Pixel coordinates are continuous: pixel i's centre is at i + 0.5.
     */
    class Square(val a: Double, val b: Double, val mX: Double, val mY: Double, val mpp: Double, val width: Int, val height: Int) {
        fun x(mm: Double) = (mm + mX) / mpp
        fun y(mm: Double) = (b + mY - mm) / mpp
        fun mmX(px: Double) = px * mpp - mX
        fun mmY(py: Double) = b + mY - py * mpp

        companion object {
            /**
             * On screen: a margin of 4% of the longer side all round, and the
             * WHOLE surface in the picture -- 960 wide, or 1600 high when the
             * surface is so deep that 960 wide would need more. (The mock-up
             * kept 960 and cut the picture at 1600, which cut off the back of a
             * long room's floor: Amit, 2026-10-11, "does not show full foto".)
             */
            fun screen(a: Double, b: Double): Square {
                val m = 0.04 * max(a, b)
                val mpp = max((a + 2 * m) / SCREEN_W, (b + 2 * m) / SCREEN_H_MAX)
                return Square(a, b, m, m, mpp, ((a + 2 * m) / mpp).roundToInt().coerceAtLeast(1),
                    ((b + 2 * m) / mpp).roundToInt().coerceAtLeast(1))
            }

            const val SCREEN_W = 960
            const val SCREEN_H_MAX = 1600

            /** A saved elevation: [margin] of EACH side beyond it, as Face Prep reads one. */
            fun elevation(a: Double, b: Double, widthPx: Int, margin: Double): Square {
                val mx = margin * a; val my = margin * b; val mpp = (a + 2 * mx) / widthPx
                return Square(a, b, mx, my, mpp, widthPx, max(1, ((b + 2 * my) / mpp).roundToInt()))
            }
        }
    }

    /**
     * The surface squared to size, sampled straight from the 360 (so a saved
     * elevation is as sharp as the pano allows). Outside the surface's own
     * view is black. [flipY] turns it upside down -- the ceiling saved the
     * way the split's "up" face looks, back wall at the top.
     */
    fun render(pano: Panorama.Image, s: String, f: Frame, sq: Square, flipY: Boolean = false): Panorama.Image {
        val out = IntArray(sq.width * sq.height)
        val (sw, sh) = dimOf(s).let { it[0].toDouble() to it[1].toDouble() }
        val pt = DoubleArray(2)
        for (row in 0 until sq.height) {
            val my = sq.mmY((if (flipY) sq.height - 1 - row else row) + 0.5)
            for (col in 0 until sq.width) {
                pt[0] = sq.mmX(col + 0.5); pt[1] = my
                val p = apply(f.m2p, pt)
                if (p[0] >= 0 && p[1] >= 0 && p[0] < sw && p[1] < sh) out[row * sq.width + col] = WallCorners.sample(pano, ray(s, p[0], p[1]))
            }
        }
        return Panorama.Image(sq.width, sq.height, out)
    }

    // ------------------------------------------- the as-is picture for the web

    /** The longer side, px, of a surface as uploaded to MCFT Interior Estimate. */
    const val ASIS_LONG_PX = 2000

    /**
     * A surface squared to size with NO MARGIN, for the web (MCFT Interior
     * Estimate's as-is view, contract 2026-10-11): the picture IS the surface
     * rectangle. Pixel (0, 0) is mm (0, [b]) -- its top-left -- and the far
     * corner (width, height) is mm ([a], 0); the same mm per pixel both ways,
     * the longer side [longPx]. Floor and ceiling come out as on the phone:
     * front wall at the top, left wall on the left. The web draws the marks
     * (mm boxes) straight onto it, so nothing here may add a border.
     */
    fun asisSquare(a: Double, b: Double, longPx: Int = ASIS_LONG_PX): Square {
        val mpp = max(a, b) / longPx
        return Square(a, b, 0.0, 0.0, mpp, (a / mpp).roundToInt().coerceAtLeast(1),
            (b / mpp).roundToInt().coerceAtLeast(1))
    }

    /**
     * The direction to sample the 360 at for picture point ([px], [py]) of
     * [sq] (continuous: pixel i's centre is i + 0.5), through the surface's
     * frame; null where that point falls outside the surface's own view.
     * The same mapping [render] uses, one point at a time.
     */
    fun asisDirection(s: String, f: Frame, sq: Square, px: Double, py: Double): DoubleArray? {
        val p = apply(f.m2p, doubleArrayOf(sq.mmX(px), sq.mmY(py)))
        val d = dimOf(s)
        if (p[0].isNaN() || p[1].isNaN() || p[0] < 0 || p[1] < 0 || p[0] > d[0] || p[1] > d[1]) return null
        return ray(s, p[0], p[1])
    }

    /** A surface as uploaded: squared, no margin, sampled from the 360. */
    fun renderAsIs(pano: Panorama.Image, s: String, f: Frame): Panorama.Image =
        render(pano, s, f, asisSquare(f.a, f.b))

    // ------------------------------------------------------------- lines

    /** Distance, px, from a point to a line through two points. */
    fun distToLine(p: DoubleArray, l: Array<DoubleArray>): Double {
        val a = l[0]; val b = l[1]; val dx = b[0] - a[0]; val dy = b[1] - a[1]
        return abs((p[0] - a[0]) * dy - (p[1] - a[1]) * dx) / (hypot(dx, dy).takeIf { it > 0 } ?: 1.0)
    }

    /** Where a line meets the two it crosses, when both are inside its view; else null. */
    fun junctions(s: String, lines: Map<String, Array<DoubleArray>>, slot: String): List<DoubleArray>? {
        val (w, h) = dimOf(s).let { it[0] to it[1] }
        val f = CROSSES.getValue(slot).map { o ->
            cross(lines.getValue(slot), lines.getValue(o))?.takeIf { it[0] >= 0 && it[0] <= w && it[1] >= 0 && it[1] <= h }
        }
        return if (f.all { it != null }) f.map { it!! } else null
    }

    /** The unit direction across a line that a + nudge moves it: UP the
     *  picture for bottom/top lines, RIGHT for left/right. */
    fun normalOf(l: Array<DoubleArray>, slot: String): DoubleArray {
        val dx = l[1][0] - l[0][0]; val dy = l[1][1] - l[0][1]; val n = hypot(dx, dy).takeIf { it > 0 } ?: 1.0
        var nx = -dy / n; var ny = dx / n
        if (slot == "bottom" || slot == "top") { if (ny > 0) { nx = -nx; ny = -ny } }
        else if (nx < 0) { nx = -nx; ny = -ny }
        return doubleArrayOf(nx, ny)
    }

    /** mm on the surface per view pixel at [at], along [dir]. */
    fun mmPerPx(f: Frame, at: DoubleArray, dir: DoubleArray): Double {
        val p = apply(f.p2m, at); val q = apply(f.p2m, doubleArrayOf(at[0] + dir[0], at[1] + dir[1]))
        return hypot(p[0] - q[0], p[1] - q[1])
    }

    /** A line moved [mm] across itself (+ up or right), through the surface's own scale; null without a frame. */
    fun nudgeLine(l: Array<DoubleArray>, slot: String, f: Frame?, mm: Double): Array<DoubleArray>? {
        if (f == null) return null
        val mid = doubleArrayOf((l[0][0] + l[1][0]) / 2, (l[0][1] + l[1][1]) / 2)
        val n = normalOf(l, slot)
        val mpp = mmPerPx(f, mid, n)
        if (!(mpp > 0) || mpp.isNaN() || mpp.isInfinite()) return null
        val px = mm / mpp
        return Array(2) { i -> doubleArrayOf(l[i][0] + n[0] * px, l[i][1] + n[1] * px) }
    }

    /** A line turned [deg] about its middle (positive turns clockwise on the picture). */
    fun turnLine(l: Array<DoubleArray>, deg: Double): Array<DoubleArray> {
        val mx = (l[0][0] + l[1][0]) / 2; val my = (l[0][1] + l[1][1]) / 2
        val c = cos(Math.toRadians(deg)); val sn = sin(Math.toRadians(deg))
        return Array(2) { i ->
            val x = l[i][0] - mx; val y = l[i][1] - my
            doubleArrayOf(mx + x * c - y * sn, my + x * sn + y * c)
        }
    }

    // ------------------------------------------------------------- marks

    /** A mark type: its letter, its name, its colour (0xRRGGBB). */
    class MarkType(val letter: String, val name: String, val colour: Int)

    /**
     * The mark types of each KIND of surface (prototype v52, sp.js TYPES). Letters repeat
     * across kinds with different meanings -- a wall's S is a Switch plate, the floor's S a
     * Step -- so every lookup goes through [kind]. v52 (Amit, 11 Oct 2026 20:36: "electric
     * control panels plate are rectangle. add mark type for it. also add seprate mark type
     * for fan"): S Switch plate on walls only; F Fan on walls and the ceiling. P Bulb point
     * (v40) on every surface.
     */
    val TYPES: Map<String, List<MarkType>> = mapOf(
        "wall" to listOf(MarkType("W", "Window", 0x4dabf7), MarkType("D", "Door", 0xff8a3d), MarkType("C", "Column", 0xb197fc),
            MarkType("B", "Beam", 0xe0b040), MarkType("O", "Opening", 0x69db7c), MarkType("N", "Niche", 0x20c997),
            MarkType("L", "Loft", 0xff922b), MarkType("E", "Electric point", 0xffd43b), MarkType("P", "Bulb point", 0xffa8a8),
            MarkType("S", "Switch plate", 0xf783ac), MarkType("F", "Fan", 0x74c0fc), MarkType("X", "Other", 0xadb5bd)),
        "floor" to listOf(MarkType("C", "Column", 0xb197fc), MarkType("S", "Step", 0xff8a3d), MarkType("O", "Opening", 0x69db7c),
            MarkType("E", "Electric point", 0xffd43b), MarkType("P", "Bulb point", 0xffa8a8), MarkType("X", "Other", 0xadb5bd)),
        "ceil" to listOf(MarkType("B", "Beam", 0xe0b040), MarkType("C", "Column", 0xb197fc), MarkType("E", "Electric point", 0xffd43b),
            MarkType("P", "Bulb point", 0xffa8a8), MarkType("F", "Fan", 0x74c0fc), MarkType("O", "Opening", 0x69db7c),
            MarkType("X", "Other", 0xadb5bd)))

    fun kind(s: String) = if (isPlan(s)) s else "wall"
    fun types(s: String): List<MarkType> = TYPES.getValue(kind(s))
    fun typeOf(s: String, t: String): MarkType = types(s).firstOrNull { it.letter == t } ?: MarkType(t, t, 0xadb5bd)

    /**
     * PLACING A TYPE ON ANOTHER SURFACE compares what the type IS, not its letter (v52 fix):
     * a Switch plate dragged onto the floor would otherwise become a Step. Null when [t], as
     * it is on [from], may go on [to]; else the refusal the screen shows, as sp.js words it.
     * Its caller is the 4 · 360 step (slice 17), where a tap may land on another surface than
     * the tab the type was picked on; 2 · Mark adds only to the surface it shows.
     */
    fun placeRefusal(from: String, to: String, t: String): String? {
        val ok = types(to).any { it.letter == t } && typeOf(to, t).name == typeOf(from, t).name
        return if (ok) null else "A ${typeOf(from, t).name} cannot go on the ${NAME.getValue(to).lowercase()}"
    }

    fun canPlace(from: String, to: String, t: String): Boolean = placeRefusal(from, to, t) == null

    /**
     * POINTS ARE SPOTS (v52; Amit, 11 Oct 2026 20:35: "electric points are spots . dont let
     * them resize. have its da 50 mm."): an Electric point, a Bulb and a Fan are stored as a
     * [SPOT] x [SPOT] box centred where the point is, never resized, and measured to their
     * CENTRE on every surface.
     */
    fun isPoint(t: String) = t == "E" || t == "P" || t == "F"

    /** A spot's box, mm across and up. */
    const val SPOT = 50.0

    /**
     * The key a point's figure to its centre is stored under: its OWN key (cl, cr, cs, ct,
     * cf, ck) wherever APK 0.3.212 and S5 measured that type to its EDGE under l, r, s, t
     * -- the same name meaning edge on one device and centre on another would put a reading
     * on the wrong line. A plan's Electric point keeps l, r, f, k: it was always measured
     * to its centre. (sp.js ckey.)
     */
    fun ckey(plan: Boolean, t: String, k: String) = if (plan && t == "E") k else "c$k"

    /**
     * A box on a surface, mm: x0..x1 from its left edge, y0..y1 up from its
     * bottom edge. [extra]: what the prep file carried for it that this build
     * does not read (a later build's `locked`, say), written back as it came
     * (SurveyPrepFile). Never changed in place, so copies may share it: [normPoints] writes its
     * [WAS] and [CONV] into a NEW map on a new Mark.
     */
    class Mark(val id: String, val t: String, val code: String,
               var x0: Double, var x1: Double, var y0: Double, var y1: Double,
               val extra: Map<String, Any?> = emptyMap()) {
        fun copy() = Mark(id, t, code, x0, x1, y0, y1, extra)
    }

    /** Where a new mark of type [t] starts on a surface [a] x [b]: x0, x1, y0, y1. */
    fun defaults(s: String, t: String, a: Double, b: Double): DoubleArray {
        fun c(w: Double, h: Double, x: Double, y: Double) = doubleArrayOf(x, x + w, y, y + h)
        if (!isPlan(s)) return when (t) {
            "W" -> c(1200.0, 1200.0, (a - 1200) / 2, 900.0)
            "D" -> c(900.0, 2100.0, 300.0, 0.0)
            "C" -> c(300.0, b, a - 450, 0.0)
            "B" -> c(a, 400.0, 0.0, b - 400)
            "O" -> c(900.0, 2100.0, (a - 900) / 2, 0.0)
            "N" -> c(900.0, 1200.0, (a - 900) / 2, 750.0)
            "E" -> c(SPOT, SPOT, 350.0, 350.0)
            "P" -> c(SPOT, SPOT, 650.0, 350.0)
            "L" -> c(a, 600.0, 0.0, b - 600)
            // a Switch plate, 200 x 90, low on the left -- kept on a wall narrower or lower than that
            "S" -> c(200.0, 90.0, min(150.0, max(0.0, a - 200)), min(1100.0, max(0.0, b - 90)))
            // a Fan, centred across, its centre 450 mm under the ceiling
            "F" -> c(SPOT, SPOT, (a - SPOT) / 2, max(0.0, b - 450 - SPOT / 2))
            else -> c(600.0, 600.0, (a - 600) / 2, (b - 600) / 2)
        }
        return when (t) {
            "C" -> c(300.0, 300.0, 150.0, b - 450)
            "S" -> c(900.0, 300.0, (a - 900) / 2, 150.0)
            "B" -> c(230.0, b, (a - 230) / 2, 0.0)
            "E", "P", "F" -> c(SPOT, SPOT, (a - SPOT) / 2, (b - SPOT) / 2)
            else -> c(600.0, 600.0, (a - 600) / 2, (b - 600) / 2)
        }
    }

    /**
     * WHERE A TAPPED MARK GOES (v52 slice 7, sp.js addElAt; Amit, 11 Oct 2026 20:35: "electric points are spots
     * . dont let them resize. have its da 50 mm." -- "a 50 mm spot centred where Amit taps"): the type's
     * [defaults] size, no wider than the face ([a]) nor taller ([b]), CENTRED ON THE TAP at ([x], [y]) mm and
     * then slid back inside the face where it would reach past an edge. A point stays [SPOT] square, centred on
     * the tap unless that is within 25 mm of an edge. A Column (full height), a Beam or a Loft (full width) keeps
     * the side that spans the face. Returns x0, x1, y0, y1.
     */
    fun placeAt(t: String, s: String, x: Double, y: Double, a: Double, b: Double): DoubleArray {
        val d = defaults(s, t, a, b)
        val w = min(a, d[1] - d[0]); val h = min(b, d[3] - d[2])
        val x0 = (x - w / 2).coerceIn(0.0, max(0.0, a - w)); val y0 = (y - h / 2).coerceIn(0.0, max(0.0, b - h))
        return doubleArrayOf(x0, x0 + w, y0, y0 + h)
    }

    /**
     * Is ([x], [y]) mm on an [a] x [b] face? Within max(25 mm, 1 % of its longer side) past an edge still counts
     * (sp.js onFace): a finger on the boundary line itself is on the face, and [placeAt] slides the mark inside.
     */
    fun onFace(a: Double, b: Double, x: Double, y: Double): Boolean {
        if (x.isNaN() || y.isNaN()) return false
        val t = max(25.0, 0.01 * max(a, b))
        return x >= -t && y >= -t && x <= a + t && y <= b + t
    }

    /** What a tap off the face says (sp.js placeAt): "Tap inside the front wall to place it". */
    fun placeMiss(s: String) = "Tap inside the ${NAME.getValue(s).lowercase()} to place it"

    /** One line of a mark's checklist: its key, what it is, and the photo's estimate (null: site only). */
    class Measure(val key: String, val what: String, val est: Double?)

    /**
     * Everything a mark asks the engineer to measure, before dropping what is
     * zero. MEASURE FROM THE NEARER SIDE (Amit, 2026-10-10, choosing "measure
     * from the nearer corner"): a position is taken from whichever corner
     * (left / right) and whichever line (floor / ceiling; front / back wall on
     * a plan) is closer -- a shorter laser shot. A window always gives its
     * sill: it decides what fits under it. A POINT (E, P, F) is measured to its
     * CENTRE on every surface, under its own keys ([ckey]), and never asks for a
     * width or a height (v52). A Switch plate is measured like a window: width,
     * height, the nearer corner and its height FROM THE FLOOR, never from the
     * ceiling, wherever it sits. Line for line sp.js measures0.
     */
    fun measures0(s: String, e: Mark, a: Double, b: Double): List<Measure> {
        val w = e.x1 - e.x0; val h = e.y1 - e.y0; val plan = isPlan(s); val cE = isPoint(e.t)
        val cx = (e.x0 + e.x1) / 2; val cy = (e.y0 + e.y1) / 2; val to = if (cE) ", to centre" else ""
        val l = if (cE) cx else e.x0; val r = if (cE) a - cx else a - e.x1
        val bo = if (cE) cy else e.y0; val tp = if (cE) b - cy else b - e.y1
        val hx = if (l <= r) Measure("l", (if (plan) "from left wall" else "from left corner") + to, l)
                 else Measure("r", (if (plan) "from right wall" else "from right corner") + to, r)
        val vy = if (plan) (if (tp <= bo) Measure("f", "from front wall$to", tp) else Measure("k", "from back wall$to", bo))
                 else if (e.t == "W" || bo <= tp) Measure("s", (if (e.t == "W") "sill, from floor" else "from floor") + to, bo)
                 else Measure("t", "from ceiling$to", tp)
        if (cE) return listOf(hx, vy).map { Measure(ckey(plan, e.t, it.key), it.what, it.est) }
        if (!plan) return when (e.t) {
            "D" -> listOf(Measure("w", "width", w), Measure("h", "height", h), hx, Measure("p", "reveal depth", null))
            "C" -> listOf(Measure("w", "width", w), hx, Measure("p", "projection from wall", null))
            "B" -> listOf(Measure("d", "drop below ceiling", b - e.y0), hx, Measure("p", "projection from wall", null))
            "N" -> listOf(Measure("w", "width", w), Measure("h", "height", h), hx, vy, Measure("p", "depth into wall", null))
            // a loft hangs from the ceiling: what matters is how high its underside is
            "L" -> listOf(Measure("w", "width", w), Measure("h", "loft height", h), hx,
                Measure("s", "underside, from floor", e.y0), Measure("p", "loft depth", null))
            // a Switch plate (a WALL's S): from the floor, never from the ceiling, even in the upper half
            "S" -> listOf(Measure("w", "width", w), Measure("h", "height", h), hx, Measure("s", "from floor", bo))
            else -> listOf(Measure("w", "width", w), Measure("h", "height", h), hx, vy) +
                (if (e.t == "W" || e.t == "O") listOf(Measure("p", "reveal depth", null)) else emptyList())
        }
        val base = listOf(Measure("w", "width, left–right", w), Measure("h", "depth, front–back", h), hx, vy)
        return when (e.t) {
            // the FLOOR's S is a Step
            "S" -> base + Measure("p", "height", null)
            "B" -> base + Measure("p", "drop below ceiling", null)
            else -> base
        }
    }

    /**
     * The checklist: A MARK TOUCHING A BOUNDARY HAS NOTHING TO MEASURE THERE
     * (Amit, 2026-10-10: "any measurement when touching line or floor should
     * not have from floor or from left corner etc as it will be zero by
     * default"). A point keeps its offsets: they run to its centre.
     */
    fun measures(s: String, e: Mark, a: Double, b: Double): List<Measure> {
        val cE = isPoint(e.t)
        return measures0(s, e, a, b).filter { m -> !(m.key in OFFS && m.est != null && m.est < ZERO && !cE) }
    }

    /**
     * A mark dragged: [part] "body" moves it whole (kept on the surface);
     * otherwise any of l r t b resize that edge (never under [MIN_SIZE]).
     * [o] is the mark as it was when the drag started.
     */
    fun moveMark(e: Mark, o: Mark, part: String, dx: Double, dy: Double, a: Double, b: Double) {
        if (part == "body") {
            val w = o.x1 - o.x0; val h = o.y1 - o.y0
            e.x0 = max(0.0, min(a - w, o.x0 + dx)); e.x1 = e.x0 + w
            e.y0 = max(0.0, min(b - h, o.y0 + dy)); e.y1 = e.y0 + h
            return
        }
        if ('l' in part) e.x0 = max(0.0, min(o.x1 - MIN_SIZE, o.x0 + dx))
        if ('r' in part) e.x1 = min(a, max(o.x0 + MIN_SIZE, o.x1 + dx))
        if ('b' in part) e.y0 = max(0.0, min(o.y1 - MIN_SIZE, o.y0 + dy))
        if ('t' in part) e.y1 = min(b, max(o.y0 + MIN_SIZE, o.y1 + dy))
    }

    /** On a DRAG only (never a button nudge): within [SNAP] of a boundary, onto it. */
    fun snapMark(e: Mark, part: String, a: Double, b: Double) {
        val w = e.x1 - e.x0; val h = e.y1 - e.y0
        if (part == "body") {
            if (e.x0 < SNAP) { e.x0 = 0.0; e.x1 = w } else if (a - e.x1 < SNAP) { e.x1 = a; e.x0 = a - w }
            if (e.y0 < SNAP) { e.y0 = 0.0; e.y1 = h } else if (b - e.y1 < SNAP) { e.y1 = b; e.y0 = b - h }
            return
        }
        if ('l' in part && e.x0 < SNAP) e.x0 = 0.0
        if ('r' in part && a - e.x1 < SNAP) e.x1 = a
        if ('b' in part && e.y0 < SNAP) e.y0 = 0.0
        if ('t' in part && b - e.y1 < SNAP) e.y1 = b
    }

    // ------------------------------------------------------------- the whole room

    /** One surface: its four lines (two view-pixel points each), which were set by hand, and its marks.
     *  [extra]: the surface's keys this build does not read (a later build's `meas`, `chains`, `ref`,
     *  `lock`), kept and written back as they came. */
    class Surface(val lines: MutableMap<String, Array<DoubleArray>>, val set: MutableMap<String, Boolean>,
                  val marks: MutableList<Mark>, val extra: Map<String, Any?> = emptyMap()) {
        fun done() = SLOTS.all { set[it] == true }
        fun copy() = Surface(lines.mapValues { (_, l) -> Array(2) { l[it].copyOf() } }.toMutableMap(),
            set.toMutableMap(), marks.map { it.copy() }.toMutableList(), extra)
    }

    /** Every surface, and the site readings keyed "<code>.<measure key>", mm. [extra] and
     *  [extraSurfaces]: the file's top-level keys and its surfaces this build does not read,
     *  kept and written back as they came (SurveyPrepFile). */
    class Prep(val surfaces: MutableMap<String, Surface>, val actual: MutableMap<String, Double>,
               val extra: Map<String, Any?> = emptyMap(), val extraSurfaces: Map<String, Any?> = emptyMap()) {
        fun surface(s: String): Surface = surfaces.getValue(s)
        fun done(): Boolean = SURFACES.all { surfaces[it]?.done() == true }
        fun copy() = Prep(surfaces.mapValues { it.value.copy() }.toMutableMap(), actual.toMutableMap(), extra, extraSurfaces)

        /** Every mark now reaching past its face, at the room size [l] x [w] x [h], in surface order. */
        fun outside(l: Double, w: Double, h: Double): List<Pair<String, Mark>> = SURFACES.flatMap { s ->
            val ab = size(s, l, w, h)
            (surfaces[s]?.marks ?: emptyList<Mark>()).filter { outOf(it, ab[0], ab[1]) }.map { s to it }
        }

        /** The one line the screen shows for them: the first, and how many more. Null when none. */
        fun outsideNote(l: Double, w: Double, h: Double): String? {
            val o = outside(l, w, h); if (o.isEmpty()) return null
            return outNote(o[0].second.code) + if (o.size > 1) " (+${o.size - 1} more)" else ""
        }

        /** The surface and mark a reading key belongs to. */
        fun markOf(code: String): Pair<String, Mark>? {
            for (s in SURFACES) surfaces[s]?.marks?.firstOrNull { code.startsWith(it.code + ".") }?.let { return s to it }
            return null
        }

        fun markById(id: String): Pair<String, Mark>? {
            for (s in SURFACES) surfaces[s]?.marks?.firstOrNull { it.id == id }?.let { return s to it }
            return null
        }

        /**
         * A new mark on [s]: its type's default box, and the next code -- one
         * past the highest of that type on that surface, so a code is never
         * handed out twice while its mark (and its readings) still exist.
         */
        fun addMark(s: String, t: String, a: Double, b: Double, id: String): Mark {
            val d = defaults(s, t, a, b)
            return Mark(id, t, nextCode(s, t), d[0], d[1], d[2], d[3]).also { surface(s).marks.add(it) }
        }

        /** The next code of type [t] on [s]: one past the highest still there (FW1, FW2 …). */
        fun nextCode(s: String, t: String): String {
            val head = CODE.getValue(s) + t
            val n = (surface(s).marks.filter { it.t == t }.mapNotNull { it.code.removePrefix(head).toIntOrNull() }.maxOrNull() ?: 0) + 1
            return head + n
        }

        /** A new mark of type [t] on [s] where the finger is, ([x], [y]) mm: [placeAt], numbered as [addMark]. */
        fun addMarkAt(s: String, t: String, x: Double, y: Double, a: Double, b: Double, id: String): Mark {
            val d = placeAt(t, s, x, y, a, b)
            return Mark(id, t, nextCode(s, t), d[0], d[1], d[2], d[3]).also { surface(s).marks.add(it) }
        }

        /** A mark and its readings go together. */
        fun deleteMark(id: String) {
            for (s in SURFACES) {
                val sf = surfaces[s] ?: continue
                val m = sf.marks.firstOrNull { it.id == id } ?: continue
                sf.marks.remove(m)
                actual.keys.removeAll { it.startsWith(m.code + ".") }
            }
        }

        /** The reading keys of a mark, in checklist order. */
        fun keysOf(s: String, e: Mark, a: Double, b: Double): List<String> = measures(s, e, a, b).map { e.code + "." + it.key }

        /** The next of a mark's readings still missing after [after] (wrapping), or null when all are in. */
        fun nextOpen(s: String, e: Mark, after: String?, a: Double, b: Double): String? {
            val ks = keysOf(s, e, a, b); val i = if (after != null) ks.indexOf(after) else -1
            return ks.drop(i + 1).firstOrNull { actual[it] == null } ?: ks.firstOrNull { actual[it] == null }
        }
    }

    // ------------------------------------------------------ older points made spots

    /** What [normPoints] did: the prep converted (a copy), how many points it re-centred, one log line per
     *  change, and whether anything at all changed (a re-centred box, or a reading converted or removed). */
    class Recentred(val prep: Prep, val points: Int, val log: List<String>, val changed: Boolean = points > 0)

    /** The mark extra holding the size a point's edge readings are converted with (its measured, else its
     *  box's, width and height when it was re-centred), so an edge reading arriving LATER still converts. */
    const val WAS = "was"
    /** The mark extra holding the centre figures this phone itself made from an edge reading, so a newer
     *  edge reading for the same point replaces the phone's own conversion and nothing else. */
    const val CONV = "conv"

    private fun mmText(v: Double): String = if (v == Math.rint(v)) v.toLong().toString() else (Math.round(v * 10) / 10.0).toString()

    private fun numOf(v: Any?): Double? = when (v) {
        is Number -> v.toDouble().takeIf { it.isFinite() }
        is String -> v.toDoubleOrNull()?.takeIf { it.isFinite() }
        else -> null
    }

    /**
     * AN OLDER POINT BECOMES A SPOT, AND ITS READINGS GO WITH IT (v52, sp.js normPoints; Amit,
     * 11 Oct 2026 20:35: "electric points are spots . dont let them resize. have its da 50 mm.").
     * APK 0.3.212 stored a point as a 150 x 150 box (S5 may hold any size) and measured a wall
     * point to its EDGE under l, r, s, t, w, h. Every point whose box is not [SPOT] square is
     * made a [SPOT] square at its own centre, and the size its readings convert with is kept on
     * the mark as [WAS]. Then, FOR EVERY POINT THAT STILL CARRIES AN EDGE KEY -- whatever its box,
     * because an edge reading can arrive after the box was converted (a web reading pulled later,
     * a conflict handing the server's value back, a second phone still on 0.3.212):
     *  - on a wall, a reading to its edge becomes the figure to its centre under its own key:
     *    l / r plus half its MEASURED width (else [WAS], else the box's), s / t plus half its
     *    measured height (else [WAS], else the box's) -- l to cl, r to cr, s to cs, t to ct;
     *  - on the floor or ceiling a Bulb or a Fan was already measured to its centre, so its
     *    l, r, f, k move to cl, cr, cf, ck UNCHANGED; an Electric point keeps l, r, f, k;
     *  - its w and h, which a spot does not have, are taken out;
     *  - A FIGURE ALREADY UNDER THE CENTRE KEY IS NEVER OVERWRITTEN (staff on S5 typing to the
     *    centre while an old phone still sent the edge): the edge reading is then only removed --
     *    UNLESS that figure is still exactly the one this phone made from an earlier edge reading
     *    (recorded as [CONV]): the edge reading in hand is newer than that one, so it replaces it.
     * Pure, on a copy; the edge keys are consumed, so running it again changes nothing.
     */
    fun normPoints(p: Prep): Recentred {
        val out = p.copy(); val act = out.actual; val log = mutableListOf<String>(); var n = 0; var any = false
        for (s in SURFACES) {
            val sf = out.surfaces[s] ?: continue
            for (i in sf.marks.indices) {
                val e = sf.marks[i]
                if (!isPoint(e.t)) continue
                val bw = e.x1 - e.x0; val bh = e.y1 - e.y0
                val boxed = !(abs(bw - SPOT) < .01 && abs(bh - SPOT) < .01)
                val plan = isPlan(s)
                fun key(k: String) = e.code + "." + k
                val wasL = e.extra[WAS] as? List<*>
                val was = if (wasL != null && wasL.size == 2) { val a = numOf(wasL[0]); val b = numOf(wasL[1]); if (a != null && b != null) doubleArrayOf(a, b) else null } else null
                val conv = LinkedHashMap<String, Double>()
                (e.extra[CONV] as? Map<*, *>)?.forEach { (k, v) -> val d = numOf(v); if (k is String && d != null) conv[k] = d }
                val mw = act[key("w")] ?: (if (!boxed) was?.get(0) else null) ?: bw
                val mh = act[key("h")] ?: (if (!boxed) was?.get(1) else null) ?: bh
                val changes = mutableListOf<String>()
                if (e.code.isNotEmpty()) {
                    if (!(plan && e.t == "E")) for (k in if (plan) listOf("l", "r", "f", "k") else listOf("l", "r", "s", "t")) {
                        val v0 = act[key(k)] ?: continue
                        val v = Math.round((v0 + (if (plan) 0.0 else (if (k == "l" || k == "r") mw else mh) / 2)) * 10) / 10.0
                        val ck = ckey(plan, e.t, k); val to = key(ck)
                        val kept = act[to]
                        val own = kept != null && conv[ck]?.let { abs(it - kept) < 1e-6 } == true
                        if (kept == null || own) { act[to] = v; conv[ck] = v }
                        act.remove(key(k))
                        changes.add(when {
                            kept == null -> "${key(k)} ${mmText(v0)} -> $to ${mmText(v)}"
                            own -> "${key(k)} ${mmText(v0)} -> $to ${mmText(v)}, replacing this phone's earlier ${mmText(kept!!)}"
                            else -> "${key(k)} ${mmText(v0)} removed, $to ${mmText(kept!!)} kept"
                        })
                    }
                    for (k in listOf("w", "h")) act.remove(key(k))?.let { changes.add("${key(k)} ${mmText(it)} removed") }
                }
                if (!boxed && changes.isEmpty()) continue
                any = true
                val cx = (e.x0 + e.x1) / 2; val cy = (e.y0 + e.y1) / 2
                val extra = LinkedHashMap<String, Any?>(e.extra)
                if (boxed) extra[WAS] = listOf(mw, mh)
                if (conv.isNotEmpty()) extra[CONV] = LinkedHashMap<String, Any?>(conv)
                sf.marks[i] = if (boxed) Mark(e.id, e.t, e.code, cx - SPOT / 2, cx + SPOT / 2, cy - SPOT / 2, cy + SPOT / 2, extra)
                              else Mark(e.id, e.t, e.code, e.x0, e.x1, e.y0, e.y1, extra)
                if (boxed) {
                    n++
                    log.add("$s ${e.code}: ${bw.roundToInt()} x ${bh.roundToInt()} re-centred to a 50 mm spot at (${mmText(cx)}, ${mmText(cy)})" +
                        (if (changes.isEmpty()) "" else "; " + changes.joinToString("; ")))
                } else log.add("$s ${e.code}: a later edge reading converted; " + changes.joinToString("; "))
            }
        }
        return Recentred(out, n, log, any)
    }

    // ------------------------------------------------------ the photo box

    /**
     * Screen pixels per picture pixel at zoom 1: the WHOLE picture inside a
     * [bw] x [bh] box, letterboxed along whichever side has room to spare.
     */
    fun fitScale(bw: Double, bh: Double, iw: Double, ih: Double): Double =
        if (iw <= 0 || ih <= 0 || bw <= 0 || bh <= 0) 1.0 else min(bw / iw, bh / ih)

    /** A view's centre along one axis, picture px: kept so the view never
     *  leaves the picture, and the picture centred when the view is wider. */
    fun clampCentre(c: Double, extent: Double, view: Double): Double =
        if (view >= extent) extent / 2 else c.coerceIn(view / 2, extent - view / 2)

    // ------------------------------------------------- reopening a kept capture

    const val MM_PER_IN = 25.4

    /**
     * The laser L, W, H to reopen a KEPT capture's survey prep with, mm. The
     * sizes saved with the prep win: they are what its lines were squared to,
     * and reopening on anything else would move every mark's numbers. A
     * capture with no saved prep falls to the sizes stored on the capture
     * (inches, as the queue keeps them). Null when either gives no valid size.
     */
    fun reopenSize(saved: DoubleArray?, lengthIn: Double, widthIn: Double, heightIn: Double): DoubleArray? {
        if (saved != null && saved.size == 3 && saved.all { valid(it) }) return saved.copyOf()
        val mm = doubleArrayOf(lengthIn * MM_PER_IN, widthIn * MM_PER_IN, heightIn * MM_PER_IN)
        return if (mm.all { valid(it) }) mm else null
    }

    /**
     * Whether a capture's kept 360 (and its survey prep) may be removed from
     * the phone to free space: only once the bench has its own copy. Before
     * that, the local file is the ONLY copy and the upload still needs it.
     */
    fun localCopyRemovable(state: String): Boolean = state == "SYNCED"

    /**
     * A fresh room: every surface's four lines on a guess from the laser sizes
     * (camera 1300 mm up, in the middle), none of them yet set by hand, no marks.
     */
    fun fresh(l: Double, w: Double, h: Double): Prep {
        val x = (if (valid(w)) w else 2600.0) / 2 / 1300; val z = (if (valid(l)) l else 3000.0) / 2 / 1300
        val c = (if (valid(h)) h else 2600.0) / 1300 - 1
        fun lerp(p: DoubleArray, q: DoubleArray, t: Double) = doubleArrayOf(p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t)
        fun four(bl: DoubleArray, br: DoubleArray, tr: DoubleArray, tl: DoubleArray) = Surface(
            mutableMapOf("bottom" to arrayOf(lerp(bl, br, .25), lerp(bl, br, .75)), "top" to arrayOf(lerp(tl, tr, .25), lerp(tl, tr, .75)),
                "left" to arrayOf(lerp(bl, tl, .3), lerp(bl, tl, .7)), "right" to arrayOf(lerp(br, tr, .3), lerp(br, tr, .7))),
            SLOTS.associateWith { false }.toMutableMap(), mutableListOf())
        fun box(s: String): Surface {
            val (vw, vh) = dimOf(s).let { it[0].toDouble() to it[1].toDouble() }
            return four(doubleArrayOf(vw * .2, vh * .8), doubleArrayOf(vw * .8, vh * .8), doubleArrayOf(vw * .8, vh * .2), doubleArrayOf(vw * .2, vh * .2))
        }
        val fl = doubleArrayOf(-x, z); val fr = doubleArrayOf(x, z); val br = doubleArrayOf(x, -z); val bl = doubleArrayOf(-x, -z)
        val seen = mapOf("front" to (fl to fr), "right" to (fr to br), "back" to (br to bl), "left" to (bl to fl))
        val out = mutableMapOf<String, Surface>()
        for (s in SURFACES) {
            out[s] = if (!isPlan(s)) {
                val (pa, pb) = seen.getValue(s)
                val pts = listOf(proj(s, pa[0], -1.0, pa[1]), proj(s, pb[0], -1.0, pb[1]), proj(s, pb[0], c, pb[1]), proj(s, pa[0], c, pa[1]))
                if (pts.all { it != null }) four(pts[0]!!, pts[1]!!, pts[2]!!, pts[3]!!) else box(s)
            } else {
                val y = if (s == "ceil") c else -1.0
                // plan: bottom = back edge, top = front edge
                val pts = listOf(bl, br, fr, fl).map { proj(s, it[0], y, it[1]) }
                if (pts.all { it != null } && abs(y) > 1e-6) four(pts[0]!!, pts[1]!!, pts[2]!!, pts[3]!!) else box(s)
            }
        }
        return Prep(out, mutableMapOf())
    }
}
