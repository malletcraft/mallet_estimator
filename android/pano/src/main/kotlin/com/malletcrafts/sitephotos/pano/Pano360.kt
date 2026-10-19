package com.malletcrafts.sitephotos.pano

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 4 · 360 -- MARK DIRECTLY ON THE 360 (prototype v44-v52, sp.js 1088-1233; v52 slice 17). Amit, 12 Oct 2026 18:43 IST:
 * "as you have 360 foto, can 360 foto be marked up directly as well?"; and on putting it in this port, 15 Oct 2026:
 * "Yes, last, after everything else."
 *
 * A flat (rectilinear) view of the whole 360 that the finger turns: a drag looks round (all the way round, straight up
 * and down), a pinch zooms between [FOV_MIN] and [FOV_MAX] degrees across. Over it, every BOUNDED surface's four lines
 * and every mark, tape, Line, chain and reference drawn in its true perspective. Marks are made and moved HERE in the
 * same millimetres as 2 · Mark: a press is a DIRECTION from the camera; that direction is put into each bounded
 * surface's own Lines view ([SurveyPrep.proj]) and through its ONE frame ([SurveyPrep.frameOf], p2m) to mm, and the
 * surface whose mm point falls inside its a x b -- most squarely inside, if two claim it -- is the one the finger is on
 * ([hitSurface]). So 2 · Mark, 3 · List, the List tab, the drawings and the S5 upload all see the same marks: ONE
 * MODEL, TWO VIEWS, and nothing is stored per view.
 *
 * Everything here is arithmetic -- the projection both ways, the near-plane clipping, which surface a direction is on,
 * where a mark's outline and tabs are on screen, the pick order, the placing check by type NAME, the tape's band, and
 * the pixels of the view itself -- so it is tested in this module; SurveyPrep360 in the app only dispatches and paints.
 *
 * Frame: the one Panorama and WallCorners use -- x right, y up, z forward (yaw 0, the front wall). Screen px: x right,
 * y DOWN, (0, 0) the view's top-left, [View.w] x [View.h].
 */
object Pano360 {

    // ------------------------------------------------------------------ the view (sp.js P3.V)

    /** The narrowest and widest view, degrees across (sp.js: `Math.min(110, Math.max(40, …))`). */
    const val FOV_MIN = 40.0
    const val FOV_MAX = 110.0
    /** A wall is faced level, 90° across; the floor and the ceiling 80° down or up, 110° across (sp.js V.face). */
    const val FOV_WALL = 90.0
    const val FOV_PLAN = 110.0
    const val PITCH_PLAN = 80.0
    /** Straight up and straight down are as far as a look goes. */
    const val PITCH_MAX = 90.0
    /** The near plane a polygon or a segment is clipped at, in the camera's unit depth (sp.js NEAR). */
    const val NEAR = 0.02
    /** A direction is on the view only past this depth (sp.js V.toS: `c[2]>1e-3`). */
    const val FRONT = 1e-3

    /** The view's pixels are drawn at this fraction of the screen's, then scaled up -- 340 x 400 px for the Mi A3's
     *  photo box -- since a 2048 px preview has only 512 px across 90° anyway (see [render]). */
    const val RENDER_SCALE = 0.5

    /** What the caption over the 360 says (sp.js render: capT). */
    fun caption(s: String) = "4 · 360 · drag to look round · ${SurveyPrep.NAME[s] ?: s}"

    /** The zoom label beside fit: "90°". */
    fun zoomText(fov: Double) = "${fov.roundToInt()}°"

    /**
     * ONE VIEW OF THE 360: looking at [yaw] degrees round from the front wall (clockwise seen from above, the right wall
     * at 90) and [pitch] degrees up, [fov] degrees across a [w] x [h] px screen. Its basis is WallCorners.basis -- the
     * one a face is cut with, so a point on a cut face and a ray through the 360 are the same thing -- and its screen is
     * the tile WallCorners.pointOf / rayAt work in, [w] across and as many px per unit vertically as horizontally.
     */
    class View(val yaw: Double, val pitch: Double, val fov: Double, val w: Double, val h: Double) {
        val basis: WallCorners.Basis = WallCorners.basis(yaw, pitch, fov, clamp = false)
        /** Screen px per unit of the image plane at depth 1 (sp.js V.f). */
        val f: Double = (w / 2) / basis.t

        /** The same look on a screen of another size (the render buffer). */
        fun sized(w2: Double, h2: Double) = View(yaw, pitch, fov, w2, h2)

        /** The unit direction through screen px ([sx], [sy]) (sp.js V.dirAt): WallCorners.rayAt on this view's tile. */
        fun dirAt(sx: Double, sy: Double): DoubleArray =
            WallCorners.rayAt(basis, sx / w, 0.5 + (sy - h / 2) / w)

        /** A direction (any length) in the camera's frame, normalised: right, up, depth (sp.js cam). */
        fun cam(d: DoubleArray): DoubleArray {
            val n = sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]).takeIf { it > 0 } ?: 1.0
            return doubleArrayOf(WallCorners.dot(d, basis.r) / n, WallCorners.dot(d, basis.u) / n, WallCorners.dot(d, basis.f) / n)
        }

        /** A point in the camera's frame on the screen (sp.js scr). */
        fun scr(c: DoubleArray): DoubleArray = doubleArrayOf(w / 2 + f * c[0] / c[2], h / 2 - f * c[1] / c[2])

        /** A direction on the screen, px; null when it is behind (or beside) the camera (sp.js V.toS). */
        fun toS(d: DoubleArray): DoubleArray? { val c = cam(d); return if (c[2] > FRONT) scr(c) else null }

        /**
         * THE PART OF A SEGMENT IN FRONT OF THE CAMERA, on the screen (sp.js seg): the chord between [d1] and [d2] cut
         * at the near plane, so a tape running from the wall ahead round past the shoulder still draws up to the edge of
         * the view and never as its mirror image. Null when both ends are behind.
         */
        fun seg(d1: DoubleArray, d2: DoubleArray): Pair<DoubleArray, DoubleArray>? {
            var a = cam(d1); var b = cam(d2)
            if (a[2] < NEAR && b[2] < NEAR) return null
            if (a[2] < NEAR) a = cut(a, b)
            if (b[2] < NEAR) b = cut(b, a)
            return scr(a) to scr(b)
        }

        /** A closed polygon of directions, clipped to what is in front of the camera, on the screen (sp.js clipPoly);
         *  fewer than three points when (nearly) none of it is. */
        fun clipPoly(ds: List<DoubleArray>): List<DoubleArray> {
            val cs = ds.map { cam(it) }; val out = mutableListOf<DoubleArray>()
            for (i in cs.indices) {
                val a = cs[i]; val b = cs[(i + 1) % cs.size]; val ia = a[2] >= NEAR; val ib = b[2] >= NEAR
                if (ia) out.add(a)
                if (ia != ib) out.add(if (ia) cut(b, a) else cut(a, b))
            }
            return out.map { scr(it) }
        }

        /** The point of the chord from [behind] to [front] at the near plane. */
        private fun cut(behind: DoubleArray, front: DoubleArray): DoubleArray {
            val t = (NEAR - behind[2]) / (front[2] - behind[2])
            return doubleArrayOf(behind[0] + (front[0] - behind[0]) * t, behind[1] + (front[1] - behind[1]) * t, NEAR)
        }
    }

    /** How the view opens on surface [s] (sp.js V.face): a wall level and 90° across, facing it; the floor 80° down and
     *  the ceiling 80° up, 110° across, with the front wall ahead. The fit button and a face tab come back to it. */
    fun faceView(s: String, w: Double, h: Double): View = when (s) {
        "floor" -> View(0.0, -PITCH_PLAN, FOV_PLAN, w, h)
        "ceil" -> View(0.0, PITCH_PLAN, FOV_PLAN, w, h)
        else -> View(SurveyPrep.YAW[s] ?: 0.0, 0.0, FOV_WALL, w, h)
    }

    /**
     * A ONE-FINGER DRAG LOOKS ROUND (sp.js pointermove, gs.look): the finger [dx] px right turns the view left by the
     * same angle the drag covers on the screen (fov / w degrees a px), all the way round; [dy] down tilts it up, no
     * further than straight up or down. [yaw0] and [pitch0] are the view as the finger went down.
     */
    fun look(v: View, yaw0: Double, pitch0: Double, dx: Double, dy: Double): View {
        val k = v.fov / v.w
        val yaw = ((yaw0 - dx * k) % 360 + 360) % 360
        val pitch = (pitch0 + dy * k).coerceIn(-PITCH_MAX, PITCH_MAX)
        return View(yaw, pitch, v.fov, v.w, v.h)
    }

    /** A PINCH ZOOMS (sp.js: `V.fov=Math.min(110,Math.max(40,gs.fov0*gs.d/d))`): fingers [d0] px apart at the start and
     *  [d] now; apart is narrower (zoomed in), together wider, within [FOV_MIN]..[FOV_MAX]. */
    fun pinchFov(fov0: Double, d0: Double, d: Double): Double =
        (fov0 * d0 / max(1e-6, d)).coerceIn(FOV_MIN, FOV_MAX)

    // ------------------------------------------------------------------ the surfaces' millimetres (sp.js mmOn, dirOfMM)

    /**
     * A DIRECTION TO MM ON SURFACE [s] (sp.js mmOn): through the surface's Lines view ([SurveyPrep.proj]) and its ONE
     * frame ([f], SurveyPrep.frameOf). Null with no frame, behind the wall's view, or -- for the floor and the ceiling --
     * in the WRONG HEMISPHERE: a direction up is never on the floor, nor one down on the ceiling (a plan's view would
     * otherwise mirror it through the camera onto the plane). The point may lie off the surface's a x b.
     */
    fun mmOn(s: String, f: SurveyPrep.Frame?, d: DoubleArray): DoubleArray? {
        if (f == null) return null
        if (SurveyPrep.isPlan(s) && (if (s == "ceil") d[1] <= 0 else d[1] >= 0)) return null
        val p = SurveyPrep.proj(s, d[0], d[1], d[2]) ?: return null
        val m = SurveyPrep.apply(f.p2m, p)
        return if (m[0].isFinite() && m[1].isFinite()) m else null
    }

    /** ([x], [y]) MM ON SURFACE [s] TO A DIRECTION (sp.js dirOfMM): through its frame to its Lines view, and that pixel's
     *  ray. Not normalised: [View.cam] is. */
    fun dirOfMm(s: String, f: SurveyPrep.Frame, x: Double, y: Double): DoubleArray {
        val p = SurveyPrep.apply(f.m2p, doubleArrayOf(x, y))
        return SurveyPrep.ray(s, p[0], p[1])
    }

    /** ([x], [y]) mm on [s] on the screen of [v], px; null when behind the camera. */
    fun toS(v: View, s: String, f: SurveyPrep.Frame, x: Double, y: Double): DoubleArray? = v.toS(dirOfMm(s, f, x, y))

    /** The part of the mm segment [a]-[b] on [s] in front of the camera, on the screen; null when none is. */
    fun seg(v: View, s: String, f: SurveyPrep.Frame, a: DoubleArray, b: DoubleArray): Pair<DoubleArray, DoubleArray>? =
        v.seg(dirOfMm(s, f, a[0], a[1]), dirOfMm(s, f, b[0], b[1]))

    /** A surface the 360 marks on is BOUNDED: its four lines all set and making a closed shape (sp.js bounded). */
    fun frames(prep: SurveyPrep.Prep, l: Double, w: Double, h: Double): Map<String, SurveyPrep.Frame> =
        SurveyPrep.SURFACES.mapNotNull { s ->
            if (prep.surfaces[s]?.done() != true) null else SurveyPrep.frameOf(s, prep, l, w, h)?.let { s to it }
        }.toMap()

    /** A press within [SLACK] of a surface's size past its edge still counts as on it (sp.js v46: "a tap right on a
     *  corner or edge still counts"). */
    const val SLACK = 0.01

    /** Where a direction lands: surface [s], at ([x], [y]) mm on it, [inside] how squarely (the least of its four
     *  fractions to an edge; negative within the slack). */
    class Hit(val s: String, val x: Double, val y: Double, val inside: Double)

    /**
     * WHICH SURFACE A DIRECTION IS ON (sp.js hitSurface): of the bounded ones ([frames], in SURFACES order), each whose mm
     * point falls within its a x b, give or take [SLACK] of each side; of two that claim it (an edge, a corner) the one it
     * is most squarely inside, the first on a tie. Null: on none -- a surface not bounded yet, or a gap the lines leave.
     */
    fun hitSurface(d: DoubleArray, frames: Map<String, SurveyPrep.Frame>): Hit? {
        var best: Hit? = null
        for (s in SurveyPrep.SURFACES) {
            val f = frames[s] ?: continue
            val m = mmOn(s, f, d) ?: continue
            val a = m[0] / f.a; val b = m[1] / f.b
            if (a < -SLACK || a > 1 + SLACK || b < -SLACK || b > 1 + SLACK) continue
            val inside = minOf(a, 1 - a, b, 1 - b)
            if (best == null || inside > best.inside) best = Hit(s, m[0], m[1], inside)
        }
        return best
    }

    /** What a press with Chain, Tape, Line or a type on says off every bounded surface (sp.js). */
    const val NO_LINES = "Set this surface's lines first"

    // ------------------------------------------------------------------ placing by what a type IS (item 11)

    /** Whether a type NAMED [typeName] may go on surface [to]: one of [to]'s types has that name. */
    fun canPlace(typeName: String, to: String): Boolean = SurveyPrep.types(to).any { it.name == typeName }

    /** The refusal: "A Switch plate cannot go on the floor", "A Step cannot go on the front wall" (sp.js, exactly). */
    fun cannotGo(typeName: String, to: String) = "A $typeName cannot go on the ${SurveyPrep.NAME.getValue(to).lowercase()}"

    /**
     * PLACING ON THE 360 COMPARES WHAT A TYPE IS, NOT ITS LETTER (v52 fix; Amit, 11 Oct 2026 20:36: "electric control
     * panels plate are rectangle. add mark type for it. also add seprate mark type for fan"): the palette's [t] was
     * picked on tab [from]; a press landing on [to] may place it only when [to] has a type of that NAME under the same
     * letter -- the wall's S is a Switch plate and the floor's S a Step, so neither crosses. Null: it may; else the
     * refusal, said as a note and nothing placed.
     */
    fun placeRefusal(from: String, to: String, t: String): String? {
        val name = SurveyPrep.typeOf(from, t).name
        val ok = canPlace(name, to) && SurveyPrep.types(to).any { it.letter == t && it.name == name }
        return if (ok) null else cannotGo(name, to)
    }

    // ------------------------------------------------------------------ marks on the screen

    /** A picked mark's tabs and the reach that grabs them, dp; their white squares are 5 dp either side (sp.js). */
    const val TAB_DP = 14.0
    const val TAB_HALF_DP = 5.0
    /** A chain's point is grabbed within 16 dp (SurveyChain.NODE_HIT_DP); a picked item's end within 14 dp. */
    const val NODE_DP = SurveyChain.NODE_HIT_DP
    const val END_DP = SurveyGuide.END_HIT_DP
    /** With Select on, a tap within 12 dp of a chain's or a Line's line picks it, on release (sp.js kHitLine, mHit). */
    const val LINE_PICK_DP = 12.0
    /** A spot is drawn at least 6 dp in radius, and is pressed anywhere within 14 dp of its centre at least. */
    const val SPOT_MIN_DP = 6.0
    const val SPOT_HIT_DP = 14.0
    /** An end's or a chain point's snapping reach is 25 mm, or what this many dp is on the surface at the finger. */
    const val SNAP_PROBE_DP = 14.0

    /**
     * A PICKED MARK'S EIGHT TABS, in mm on its surface, in sp.js's order -- each the part it drags: the left and right
     * edges at mid-height, the top and bottom at mid-width, then the four corners. So a tab dragged on the 360 moves
     * exactly its own edge(s) along the surface (ct52 X5), through SurveyPrep.moveMark as on 2 · Mark.
     */
    fun tabsOf(e: SurveyPrep.Mark): List<Pair<String, DoubleArray>> {
        val xm = (e.x0 + e.x1) / 2; val ym = (e.y0 + e.y1) / 2
        return listOf("l" to doubleArrayOf(e.x0, ym), "r" to doubleArrayOf(e.x1, ym), "t" to doubleArrayOf(xm, e.y1),
            "b" to doubleArrayOf(xm, e.y0), "lt" to doubleArrayOf(e.x0, e.y1), "rt" to doubleArrayOf(e.x1, e.y1),
            "lb" to doubleArrayOf(e.x0, e.y0), "rb" to doubleArrayOf(e.x1, e.y0))
    }

    /**
     * A MARK AS IT IS ON THE SCREEN: its surface and id, the outline a press inside picks it by, and that outline's area
     * (the smallest under the finger wins). A box's [pts] are its four corners through the perspective, clipped at the
     * near plane; a point's ([spot]: centre x, y and the radius it is drawn with -- its 50 mm, never under [SPOT_MIN_DP])
     * a 16-sided ring at least [SPOT_HIT_DP] round it.
     */
    class Poly(val s: String, val id: String, val pts: List<DoubleArray>, val area: Double, val spot: DoubleArray? = null)

    /** [e] on [s] in the view [v], [dpPx] px per dp; null when it is off the view (behind the camera). */
    fun markPoly(v: View, s: String, f: SurveyPrep.Frame, e: SurveyPrep.Mark, dpPx: Double): Poly? {
        if (SurveyPrep.isPoint(e.t)) {
            val cx = (e.x0 + e.x1) / 2; val cy = (e.y0 + e.y1) / 2
            val c = toS(v, s, f, cx, cy) ?: return null
            val c2 = toS(v, s, f, cx + SurveyPrep.SPOT / 2, cy)
            val r = max(SPOT_MIN_DP * dpPx, if (c2 != null) hypot(c2[0] - c[0], c2[1] - c[1]) else 0.0)
            val rh = max(r, SPOT_HIT_DP * dpPx)
            val ring = (0 until 16).map { i -> val a = i * PI / 8; doubleArrayOf(c[0] + rh * cos(a), c[1] + rh * sin(a)) }
            return Poly(s, e.id, ring, PI * rh * rh, doubleArrayOf(c[0], c[1], r))
        }
        val pts = v.clipPoly(listOf(doubleArrayOf(e.x0, e.y0), doubleArrayOf(e.x1, e.y0), doubleArrayOf(e.x1, e.y1),
            doubleArrayOf(e.x0, e.y1)).map { dirOfMm(s, f, it[0], it[1]) })
        if (pts.size < 3) return null
        return Poly(s, e.id, pts, polyArea(pts))
    }

    /** The area of a polygon on the screen, px² (the shoelace). */
    fun polyArea(p: List<DoubleArray>): Double {
        var a = 0.0
        for (i in p.indices) { val q = p[(i + 1) % p.size]; a += p[i][0] * q[1] - q[0] * p[i][1] }
        return abs(a) / 2
    }

    /** Whether ([x], [y]) is inside the polygon [p] (sp.js inPoly: the even-odd crossing rule). */
    fun inPoly(x: Double, y: Double, p: List<DoubleArray>): Boolean {
        var c = false; var j = p.size - 1
        for (i in p.indices) {
            val xi = p[i][0]; val yi = p[i][1]; val xj = p[j][0]; val yj = p[j][1]
            if (((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) c = !c
            j = i
        }
        return c
    }

    /**
     * THE SNAPPING REACH AT THE FINGER, mm (sp.js kTol): 25 mm, or what [probeDp] dp to the right of the finger at
     * ([sx], [sy]) is on [s] when that is more -- so it is 14 dp on the screen however far away the surface is.
     * [mm] the finger's own point on [s].
     */
    fun tolMm(v: View, s: String, f: SurveyPrep.Frame, sx: Double, sy: Double, mm: DoubleArray?, dpPx: Double,
              probeDp: Double = SNAP_PROBE_DP): Double {
        val m2 = mmOn(s, f, v.dirAt(sx + probeDp * dpPx, sy))
        return max(SurveyGuide.SNAP_MM, if (m2 != null && mm != null) hypot(m2[0] - mm[0], m2[1] - mm[1]) else SurveyGuide.SNAP_MM)
    }

    // ------------------------------------------------------------------ the tape's band (item 5: "narrows with distance")

    /**
     * THE TAPE'S HALF-WIDTH ON THE 360, mm (sp.js drawTapes): its 25 mm band, or [SurveyMeas.TAPE_MIN_HALF_DP] dp where
     * that is wider -- measured ACROSS the band at its middle, in the face's mm, so the band is still drawn in the face's
     * own perspective and NARROWS WITH DISTANCE as the wall does (Amit, 14 Oct 2026 11:48 IST: "add one more tool called
     * as Tape which act as a actual tape"). The ONE rule the drawing and the picking share with 2 · Mark
     * (SurveyMeas.bandHalfMm), with the scale taken where the tape is rather than once for the face.
     */
    fun tapeHalfMm(v: View, s: String, f: SurveyPrep.Frame, a: DoubleArray, b: DoubleArray, dpPx: Double): Double =
        SurveyMeas.bandHalfMm(tapeScale(v, s, f, a, b), dpPx)

    /** Screen px per face mm ACROSS the tape from [a] to [b], at its middle (sp.js drawTapes' ppm); 0 when that is off
     *  the view or the tape has no length. What the band's width, its ticks and its pick are scaled by. */
    fun tapeScale(v: View, s: String, f: SurveyPrep.Frame, a: DoubleArray, b: DoubleArray): Double {
        val len = hypot(b[0] - a[0], b[1] - a[1])
        if (!(len > 0)) return 0.0
        val nx = -(b[1] - a[1]) / len; val ny = (b[0] - a[0]) / len
        val mx = (a[0] + b[0]) / 2; val my = (a[1] + b[1]) / 2
        val m1 = toS(v, s, f, mx, my); val m2 = toS(v, s, f, mx + nx * 10, my + ny * 10)
        return if (m1 != null && m2 != null) hypot(m2[0] - m1[0], m2[1] - m1[1]) / 10 else 0.0
    }

    /** The tape's band, mm on its surface: its four corners, [hw] either side of the line from [a] to [b]. */
    fun band(a: DoubleArray, b: DoubleArray, hw: Double): List<DoubleArray> {
        val len = hypot(b[0] - a[0], b[1] - a[1]).takeIf { it > 0 } ?: 1.0
        val nx = -(b[1] - a[1]) / len; val ny = (b[0] - a[0]) / len
        return listOf(doubleArrayOf(a[0] + nx * hw, a[1] + ny * hw), doubleArrayOf(b[0] + nx * hw, b[1] + ny * hw),
            doubleArrayOf(b[0] - nx * hw, b[1] - ny * hw), doubleArrayOf(a[0] - nx * hw, a[1] - ny * hw))
    }

    // ------------------------------------------------------------------ lines on the screen, and picking them

    /** Where a point that is behind the camera is put when a list of points must keep its length (an end handle that
     *  cannot be pressed): far off every screen. */
    const val OFF = 1e9

    /** [pts] mm on [s] on the screen, each in its place; one behind the camera at ([OFF], [OFF]). */
    fun screenPts(v: View, s: String, f: SurveyPrep.Frame, pts: List<DoubleArray>): List<DoubleArray> =
        pts.map { p -> toS(v, s, f, p[0], p[1]) ?: doubleArrayOf(OFF, OFF) }

    /** An item drawn as straight pieces on the screen, clipped at the near plane: its [id], its [segs], and how much
     *  wider than its line a press may be ([widen], px: a tape's band). */
    class Line(val id: String, val segs: List<Pair<DoubleArray, DoubleArray>>, val widen: Double = 0.0)

    /** The pieces of the mm polyline [pts] on [s] in front of the camera; [closed] joins the last to the first. */
    fun segsOf(v: View, s: String, f: SurveyPrep.Frame, pts: List<DoubleArray>, closed: Boolean): List<Pair<DoubleArray, DoubleArray>> {
        val n = if (closed) pts.size else pts.size - 1
        return (0 until max(0, n)).mapNotNull { i -> seg(v, s, f, pts[i], pts[(i + 1) % pts.size]) }
    }

    /** A Tape, a Line or an area on the screen: a Line's and a tape's pieces open, an area's closed, a tape [widen]ed by
     *  its band's half-width there (SurveyMeas.bandHalfPx at its own scale, [tapeScale]). */
    fun measLine(v: View, s: String, f: SurveyPrep.Frame, m: SurveyPrep.Meas, dpPx: Double): Line =
        Line(m.id, segsOf(v, s, f, m.pts, m.kind == SurveyMeas.AREA),
            if (m.kind == SurveyMeas.TAPE && m.pts.size == 2) SurveyMeas.bandHalfPx(tapeScale(v, s, f, m.pts[0], m.pts[1]), dpPx) else 0.0)

    /**
     * WITH SELECT ON, A TAP ON NOTHING ELSE PICKS A LINE (sp.js kHitLine, then mHit): the item whose drawn line -- or
     * tape band -- is nearest the finger at ([x], [y]), within [r] px (12 dp). Null: none.
     */
    fun pickLine(lines: List<Line>, x: Double, y: Double, r: Double): String? {
        var best: String? = null; var bd = 0.0
        for (l in lines) for ((p, q) in l.segs) {
            val d = (SurveyMeas.segDist(x, y, p, q) - l.widen).coerceAtLeast(0.0)
            if (d < r && (best == null || d < bd)) { best = l.id; bd = d }
        }
        return best
    }

    // ------------------------------------------------------------------ THE 360 PICK ORDER (items 8 and 14)

    /** One step of the 360's pick order. */
    enum class Step {
        /** The selected Tape's, Line's or area's end within 14 dp: with Select on it moves; with Tape or Line on a new
         *  one starts from exactly there (tapes laid end to end). Not while a Line area or a chain is being tapped. */
        END,
        /** The picked mark's eight tabs within 14 dp -- BEFORE a chain's points (item 14, ct52 X5). */
        TABS,
        /** A chain's point within 16 dp: its chain picked, the point moved along it. */
        NODES,
        /** Chain on: a point on the surface under the finger. */
        CHAIN,
        /** Tape or Line on: a drag pulls one, a tap is a Line area's corner (or the Tape's hint). */
        MEAS,
        /** A mark's outline under the finger: the picked one first, then the smallest. */
        MARK,
        /** A type on: placed on the surface under the finger, centred and kept inside it. */
        PLACE,
        /** Nothing: the drag looks round; a tap picks a chain's or a Line's line within 12 dp, or lets go of the pick. */
        LOOK,
    }

    /**
     * WHAT A PRESS ON THE 360 TRIES, IN ORDER, with [tool] on (sp.js P3 pointerdown) -- [drafting]: a Line area or a
     * chain is being tapped, whose taps are corners and points, so no end is grabbed. The same order as 2 · Mark's,
     * with the tabs before the chain points (item 14) and the ends first (item 8); a drawing tool stops at its own step.
     */
    fun order(tool: String, drafting: Boolean): List<Step> {
        val out = mutableListOf<Step>()
        val meas = SurveyPrepPalette.isMeasTool(tool); val chain = tool == SurveyPrepPalette.CHAIN
        if (!drafting && (tool == SurveyPrepPalette.SEL || meas)) out.add(Step.END)
        if (!meas && !chain) out.add(Step.TABS)
        if (tool == SurveyPrepPalette.SEL || chain) out.add(Step.NODES)
        if (chain) { out.add(Step.CHAIN); return out }
        if (meas) { out.add(Step.MEAS); return out }
        out.add(Step.MARK)
        out.add(if (SurveyPrepPalette.isType(tool)) Step.PLACE else Step.LOOK)
        return out
    }

    /**
     * What is on the screen to be pressed: the selected Tape's / Line's / area's points ([ends], null when none is
     * selected; a point behind the camera far off the screen), the picked mark's tabs ([tabs]; empty for a locked mark
     * or a spot), every chain's points ([nodes]: id, points), and every mark's outline ([polys]), [selId] the picked one.
     */
    class Scene(val ends: List<DoubleArray>?, val tabs: List<Pair<String, DoubleArray>>,
                val nodes: List<Pair<String, List<DoubleArray>>>, val polys: List<Poly>, val selId: String?)

    /** What a press took. */
    sealed class Pick {
        data class End(val i: Int) : Pick()
        data class Tab(val part: String) : Pick()
        data class Node(val id: String, val i: Int) : Pick()
        object Chain : Pick() { override fun toString() = "Chain" }
        object Meas : Pick() { override fun toString() = "Meas" }
        data class Mark(val s: String, val id: String) : Pick()
        object Place : Pick() { override fun toString() = "Place" }
        object Look : Pick() { override fun toString() = "Look" }
    }

    /** THE PRESS at ([x], [y]) px through [order] over [sc], [dpPx] px per dp: the first step that takes it. */
    fun pick(order: List<Step>, sc: Scene, x: Double, y: Double, dpPx: Double): Pick {
        for (st in order) when (st) {
            Step.END -> sc.ends?.let { e -> SurveyGuide.endAt(e, x, y, END_DP * dpPx)?.let { return Pick.End(it) } }
            Step.TABS -> sc.tabs.map { (p, q) -> p to hypot(q[0] - x, q[1] - y) }.filter { it.second < TAB_DP * dpPx }
                .minByOrNull { it.second }?.let { return Pick.Tab(it.first) }
            Step.NODES -> SurveyChain.nodeAt(sc.nodes, x, y, NODE_DP * dpPx)?.let { (id, i) -> return Pick.Node(id, i) }
            Step.CHAIN -> return Pick.Chain
            Step.MEAS -> return Pick.Meas
            Step.MARK -> markAt(sc.polys, sc.selId, x, y)?.let { return Pick.Mark(it.s, it.id) }
            Step.PLACE -> return Pick.Place
            Step.LOOK -> return Pick.Look
        }
        return Pick.Look
    }

    /** The mark under ([x], [y]) (sp.js V.hit): the picked one first (it is drawn on top), then the smallest outline. */
    fun markAt(polys: List<Poly>, selId: String?, x: Double, y: Double): Poly? =
        polys.filter { inPoly(x, y, it.pts) }
            .sortedWith(compareBy<Poly>({ if (it.id == selId) 0 else 1 }, { it.area })).firstOrNull()

    // ------------------------------------------------------------------ the view's pixels

    /**
     * THE VIEW ITSELF (sp.js V.draw's pixel loop): every pixel of a [w] x [h] buffer a direction through [v] -- sized to
     * the buffer -- and the 360's pixel it falls on, NEAREST (as sp.js samples), into [out] as opaque ARGB. The 2048 px
     * preview the screen already holds is read in place: nothing is decoded or allocated here, so the screen keeps one
     * buffer per size and calls this once a frame, off the main thread.
     */
    fun render(pano: Panorama.Image, v: View, out: IntArray, w: Int, h: Int) {
        require(out.size >= w * h) { "buffer $w x $h" }
        val pw = pano.width; val ph = pano.height; val px = pano.pixels
        val b = v.basis; val fx = v.f
        val kx = pw / (2 * PI); val ky = ph / PI
        for (j in 0 until h) {
            val vv = -(j + 0.5 - h / 2.0) / fx
            val bx = b.f[0] + vv * b.u[0]; val by = b.f[1] + vv * b.u[1]; val bz = b.f[2] + vv * b.u[2]
            val row = j * w
            for (i in 0 until w) {
                val uu = (i + 0.5 - w / 2.0) / fx
                val x = bx + uu * b.r[0]; val y = by + uu * b.r[1]; val z = bz + uu * b.r[2]
                val lam = atan2(x, z); val phi = atan2(y, sqrt(x * x + z * z))
                var sx = floor(lam * kx + pw / 2.0).toInt()
                if (sx >= pw) sx -= pw else if (sx < 0) sx += pw
                val sy = floor(ph / 2.0 - phi * ky).toInt().coerceIn(0, ph - 1)
                out[row + i] = px[sy * pw + sx] or (0xFF shl 24)
            }
        }
    }

    /** The render buffer for a [w] x [h] px screen: [RENDER_SCALE] of it, at least 1 x 1. */
    fun bufferSize(w: Double, h: Double): IntArray =
        intArrayOf(max(1, (w * RENDER_SCALE).roundToInt()), max(1, (h * RENDER_SCALE).roundToInt()))
}
