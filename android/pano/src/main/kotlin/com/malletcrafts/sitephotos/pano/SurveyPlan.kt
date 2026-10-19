package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * SIX DRAWINGS ON THE PLAN TAB (v52 slice 13, items 10 and 11). Amit, 14 Oct 2026 06:32 IST: "like plan i need
 * elevation fo all walls and ceiling plan as well."; 20:45 IST: "where is other walls elevation diagram?" And for the
 * fan (11 Oct 20:36: "also add seprate mark type for fan"): on the ceiling plan a fan is three blades inside a dashed
 * 1200 mm sweep circle.
 *
 * Floor · Ceiling · Front · Right · Back · Left, each a PURE DISPLAY LIST in millimetres (lines, polygons, circles, arcs,
 * text, the floor hatch): built here from what survey prep already holds -- the laser L × W × H, every mark in its
 * surface's mm, and the readings -- and only painted by the screen. Nothing is guessed from the photo except what the
 * marks already say, and every figure says where it came from: a laser reading in ink ([INK], #1d1f1a), the photo's
 * estimate as ≈ in grey ([GREY], #8a8f86).
 *
 * PORTED FROM THE PROTOTYPE'S plan.js ONLY (v36build/plan.js, v52) plus its fan symbol (sp.js 1364-1369). NEVER from sp.js
 * 1235-1330, which is the v45 plan that drew the front at the bottom with the right wall still on the right -- the room's
 * MIRROR IMAGE: held with FRONT towards the front wall, every door on the right showed on the left.
 *
 * THE MAPPING (item 10), a TRUE TOP VIEW the way the APK's own floor and ceiling faces are drawn: page x runs left → right
 * (0 … W), page y front → back (0 … L), FRONT at the top and the right wall on the right. Each wall's own x (as Amit
 * faces it, from its left corner) lands on the plan at [planPoint]: front x → (x, 0); right x → (W, x); back x → (W − x,
 * L); left x → (0, L − x). The floor's and the ceiling's mm run x left → right and y back (0) → front (L), so their page
 * y is [planY] = L − y for BOTH -- the ceiling plan is the REFLECTED one, lying exactly over the floor plan. An elevation
 * is a wall face on: its x from its left corner, its height up from the floor, page y [elevY] = B − y.
 *
 * CONSTRAINT FOR S5 QUOTE (item 10: "S5's copy of the plan, when it is built, has to use the same mapping"): when S5
 * draws its own plan or elevations from the same as-is record, it MUST use exactly [planPoint], [planY] and [elevY] --
 * front at the top, the walls clockwise F → R → B → L, the ceiling reflected the same way round as the floor -- or the
 * phone and the web will show one room as two mirror images of each other.
 *
 * Sizes come from slice 12's ONE [SurveyPrep.sizeOf]: a wall squared from its reference rectangle draws (and is
 * dimensioned) at its reference size. The floor and ceiling plans are the room's laser W × L, as plan.js draws them.
 */
object SurveyPlan {

    /** A laser reading: ink. */
    const val INK = 0x1d1f1a
    /** The photo's estimate, shown with ≈: grey. */
    const val GREY = 0x8a8f86
    /** The wall band on the plans, the floor hatch. */
    const val WALL = 0x55594f
    /** FRONT, REFLECTED CEILING PLAN and a wall's name: the drawings' gold-brown. */
    const val LABEL = 0x6b5a1e
    const val WHITE = 0xffffff
    /** A niche's fill on the floor plan. */
    const val NICHE = 0xf4f1e8
    /** The drawing box's paper (v42.css .planv). */
    const val PAPER = 0xf7f5ee

    /** The wall band's thickness on the plans, mm (plan.js PLAN.T). */
    const val T = 150.0
    /** A drawing's text unit: its longer side over this (plan.js fs = max(dims) / 34). */
    const val FS_DIV = 34.0
    /** A ceiling fan's sweep circle: radius 600 mm, a 1200 mm circle. */
    const val SWEEP_R = 600.0
    /** The scale bar: 1 m. */
    const val BAR = 1000.0

    /** The six drawings, as the chips name them (plan.js DRAWV). */
    val VIEWS: List<Pair<String, String>> = listOf("floor" to "Floor", "ceil" to "Ceiling", "front" to "Front",
        "right" to "Right", "back" to "Back", "left" to "Left")

    /** What each drawing is called in full (plan.js DTITLE): the chips' read-out label, and the export's title block. */
    val DTITLE: Map<String, String> = mapOf("floor" to "Floor plan", "ceil" to "Reflected ceiling plan",
        "front" to "Front wall elevation", "right" to "Right wall elevation", "back" to "Back wall elevation",
        "left" to "Left wall elevation")

    /** Facing a wall, which wall meets it at its left corner and at its right (plan.js NEIGH). */
    val NEIGH: Map<String, Pair<String, String>> = mapOf("front" to ("Left wall" to "Right wall"),
        "right" to ("Front wall" to "Back wall"), "back" to ("Right wall" to "Left wall"), "left" to ("Back wall" to "Front wall"))

    val WALLS = listOf("front", "right", "back", "left")

    fun isView(v: String) = VIEWS.any { it.first == v }

    // ------------------------------------------------------------------ the mapping

    /** A wall's own x (from its left corner as Amit faces it) on the plan, page mm: front at the top (item 10). */
    fun planPoint(s: String, x: Double, w: Double, l: Double): DoubleArray = when (s) {
        "front" -> doubleArrayOf(x, 0.0)
        "right" -> doubleArrayOf(w, x)
        "back" -> doubleArrayOf(w - x, l)
        "left" -> doubleArrayOf(0.0, l - x)
        else -> throw IllegalArgumentException("not a wall: $s")
    }

    /** A wall's inward normal on the plan: into the room. */
    fun inward(s: String): DoubleArray = when (s) {
        "front" -> doubleArrayOf(0.0, 1.0)
        "right" -> doubleArrayOf(-1.0, 0.0)
        "back" -> doubleArrayOf(0.0, -1.0)
        "left" -> doubleArrayOf(1.0, 0.0)
        else -> throw IllegalArgumentException("not a wall: $s")
    }

    /** A floor's or a ceiling's mm y (back 0 → front L) on the page, front at the top -- the same for both. */
    fun planY(y: Double, l: Double): Double = l - y

    /** A wall's mm y (up from the floor) on an elevation's page (y down), the wall [b] high. */
    fun elevY(y: Double, b: Double): Double = b - y

    /** A drawing's text unit, mm: its longer side over [FS_DIV]. */
    fun fsOf(a: Double, b: Double): Double = max(a, b) / FS_DIV

    /** The size a drawing is of, mm: the room's W × L for the plans, else the wall's own [SurveyPrep.sizeOf]. */
    fun dims(v: String, prep: SurveyPrep.Prep, l: Double, w: Double, h: Double): DoubleArray =
        if (v == "floor" || v == "ceil") doubleArrayOf(w, l) else SurveyPrep.sizeOf(v, prep, l, w, h)

    // ------------------------------------------------------------------ the display list

    enum class Anchor { START, MIDDLE, END }

    /**
     * One thing to paint, in the drawing's mm (page y down). [role] says what it is (plan.js's data attributes and
     * classes: "op", "leaf", "swing", "mk", "el", "dim", "opl", "elc", "frontlab", …) and [key] whose it is (a mark's
     * code, or a dimension's reading key) -- for the tests, and for nothing on the screen.
     */
    sealed class Item(val role: String, val key: String?)

    class Line(val x1: Double, val y1: Double, val x2: Double, val y2: Double, val colour: Int, val width: Double,
               val dash: DoubleArray? = null, role: String = "", key: String? = null) : Item(role, key)

    /** A filled and / or stroked polygon; more than one ring is filled even-odd (the plans' wall band). */
    class Poly(val rings: List<List<DoubleArray>>, val fill: Int?, val fillAlpha: Double, val stroke: Int?, val width: Double,
               val dash: DoubleArray? = null, role: String = "", key: String? = null, val s: String? = null) : Item(role, key)

    class Circle(val cx: Double, val cy: Double, val r: Double, val fill: Int?, val stroke: Int?, val width: Double,
                 val dash: DoubleArray? = null, role: String = "", key: String? = null) : Item(role, key)

    /** A filled ellipse turned [rot] degrees about its centre (a fan's blade). */
    class Ellipse(val cx: Double, val cy: Double, val rx: Double, val ry: Double, val rot: Double, val fill: Int,
                  role: String = "", key: String? = null) : Item(role, key)

    /** An arc about ([cx], [cy]) from [start] degrees turning [sweep] degrees (page y down: clockwise is positive). */
    class Arc(val cx: Double, val cy: Double, val r: Double, val start: Double, val sweep: Double, val colour: Int,
              val width: Double, val dash: DoubleArray? = null, role: String = "", key: String? = null) : Item(role, key)

    /** Text centred vertically on [y] (dominant-baseline middle), [size] mm high, turned [rot] degrees about (x, y). */
    class Text(val x: Double, val y: Double, val text: String, val colour: Int, val size: Double, val anchor: Anchor,
               val bold: Boolean, val rot: Double, role: String = "", key: String? = null) : Item(role, key)

    /** A dimension as drawn: its reading key, the figure shown, and whether it is a laser reading. */
    class Dim(val key: String, val text: String, val read: Boolean)

    /**
     * One drawing: what to paint, the box it fits in ([box]: x, y, width, height, mm), its text unit [fs], the line under
     * it ([info]), its full name ([title]) and its dimensions ([dims], in the order drawn).
     */
    class Drawing(val view: String, val items: List<Item>, val box: DoubleArray, val fs: Double, val info: String,
                  val title: String, val dims: List<Dim>) {
        fun dim(key: String): Dim? = dims.firstOrNull { it.key == key }
        fun texts(role: String): List<Text> = items.filterIsInstance<Text>().filter { it.role == role }
        fun withKey(role: String, key: String): List<Item> = items.filter { it.role == role && it.key == key }
    }

    /** A figure for the drawing: the laser reading if there is one (ink), else the photo's estimate (≈, grey). */
    class Fig(val t: String, val read: Boolean)

    /** JS Math.round: half up. */
    private fun rnd(v: Double): Long = Math.round(v)

    fun figure(actual: Map<String, Double>, code: String, est: Double?): Fig {
        actual[code]?.let { return Fig(rnd(it).toString(), true) }
        if (est == null) return Fig("?", false)
        return Fig("≈" + rnd(est), false)
    }

    /** A reading used as a depth the way plan.js uses it (`actual[k] || d`): a missing, zero or NaN reading is [d]. */
    private fun orJs(v: Double?, d: Double): Double = if (v == null || v == 0.0 || v.isNaN()) d else v

    private fun colourOf(s: String, t: String) = SurveyPrep.typeOf(s, t).colour

    /** The drawing kit (plan.js dkit): text, lines, and dimensions with a filled arrow head at each end. */
    private class Kit(val fs: Double, val actual: Map<String, Double>) {
        val items = mutableListOf<Item>()
        val dims = mutableListOf<Dim>()

        fun txt(x: Double, y: Double, t: String, c: Int = INK, a: Anchor = Anchor.MIDDLE, sz: Double = 1.0, b: Boolean = false,
                rot: Double = 0.0, role: String = "", key: String? = null) {
            items.add(Text(x, y, t, c, fs * sz, a, b, rot, role, key))
        }

        fun ln(x1: Double, y1: Double, x2: Double, y2: Double, c: Int = INK, w: Double = 0.06, dash: DoubleArray? = null,
               role: String = "", key: String? = null) {
            items.add(Line(x1, y1, x2, y2, c, fs * w, dash, role, key))
        }

        private fun head(x: Double, y: Double, ux: Double, uy: Double, c: Int, role: String, key: String) {
            val hl = fs * 0.5; val hw = fs * 0.17; val bx = x - ux * hl; val by = y - uy * hl
            items.add(Poly(listOf(listOf(doubleArrayOf(x, y), doubleArrayOf(bx - uy * hw, by + ux * hw), doubleArrayOf(bx + uy * hw, by - ux * hw))),
                c, 1.0, null, 0.0, null, role, key))
        }

        /** A dimension from (x1, y1) to (x2, y2), its figure beside the middle: above a horizontal one, left of a
         *  vertical one reading upwards ([side] -1: the other side). Nothing under 1 mm. */
        fun dim(x1: Double, y1: Double, x2: Double, y2: Double, f: Fig, key: String, cls: String = "dim", side: Double = 1.0) {
            val d = hypot(x2 - x1, y2 - y1)
            if (d < 1) return
            val ux = (x2 - x1) / d; val uy = (y2 - y1) / d; val c = if (f.read) INK else GREY; val vert = abs(uy) > abs(ux)
            ln(x1, y1, x2, y2, c, 0.05, role = "$cls.line", key = key)
            if (d >= fs * 1.2) { head(x1, y1, -ux, -uy, c, "$cls.head", key); head(x2, y2, ux, uy, c, "$cls.head", key) }
            val mx = (x1 + x2) / 2; val my = (y1 + y2) / 2
            val tx = if (vert) mx - side * fs * 0.6 else mx; val ty = if (vert) my else my - side * fs * 0.6
            txt(tx, ty, f.t, c, sz = 0.8, b = f.read, rot = if (vert) -90.0 else 0.0, role = cls, key = key)
            dims.add(Dim(key, f.t, f.read))
        }

        /** The 1 m bar with its ticks and "1 m" (plan.js scaleBar). */
        fun scaleBar(x: Double, y: Double) {
            val tk = fs * 0.4
            ln(x, y, x + BAR, y, role = "scalebar"); ln(x, y - tk, x, y + tk, role = "scalebar")
            ln(x + BAR / 2, y - tk * 0.6, x + BAR / 2, y + tk * 0.6, role = "scalebar"); ln(x + BAR, y - tk, x + BAR, y + tk, role = "scalebar")
            txt(x + BAR + fs * 0.4, y, "1 m", a = Anchor.START, sz = 0.75, role = "scalebar")
        }

        fun fig(code: String, est: Double?) = figure(actual, code, est)

        /**
         * A point's symbol (plan.js pointSym, and its v52 fan wrapper): a white disc with a dot (Electric), a cross
         * (Bulb), or three blades and a hub (Fan) -- a CEILING fan inside its dashed 1200 mm sweep circle, a wall fan none.
         */
        fun pointSym(c: DoubleArray, t: String, code: String, s: String, role: String) {
            val r = fs * 0.42; val x = c[0]; val y = c[1]
            if (t == "F") {
                if (s == "ceil") items.add(Circle(x, y, SWEEP_R, null, INK, fs * 0.05, doubleArrayOf(fs * 0.35, fs * 0.25), "sweep", code))
                items.add(Circle(x, y, r, WHITE, INK, fs * 0.07, null, role, code))
                for (i in 0 until 3) {
                    val a = -90.0 + i * 120; val rad = Math.toRadians(a)
                    items.add(Ellipse(x + r * 0.5 * cos(rad), y + r * 0.5 * sin(rad), r * 0.48, r * 0.18, a, INK, "fan", code))
                }
                items.add(Circle(x, y, r * 0.14, INK, null, 0.0, null, "fan", code))
                return
            }
            items.add(Circle(x, y, r, WHITE, INK, fs * 0.07, null, role, code))
            if (t == "P") {
                val k = r * 0.7
                ln(x - k, y - k, x + k, y + k, w = 0.07, role = "sym", key = code)
                ln(x - k, y + k, x + k, y - k, w = 0.07, role = "sym", key = code)
            } else items.add(Circle(x, y, r * 0.3, INK, null, 0.0, null, "sym", code))
        }
    }

    private fun rect(x: Double, y: Double, w: Double, h: Double) =
        listOf(doubleArrayOf(x, y), doubleArrayOf(x + w, y), doubleArrayOf(x + w, y + h), doubleArrayOf(x, y + h))

    private fun moved(p: DoubleArray, n: DoubleArray, d: Double) = doubleArrayOf(p[0] + n[0] * d, p[1] + n[1] * d)

    /** The first of a mark's checklist lines across: l / r, or a point's cl / cr (plan.js hxOf). */
    private fun hxOf(ms: List<SurveyPrep.Measure>) = ms.firstOrNull { Regex("^c?[lr]$").matches(it.key) }
    private fun side(k: String) = k.last()

    /** Degrees of (dx, dy) on the page (y down). */
    private fun deg(dx: Double, dy: Double) = Math.toDegrees(atan2(dy, dx))

    // ------------------------------------------------------------------ the drawings

    /**
     * Drawing [view] (one of [VIEWS]) of [prep] at the room size [l] × [w] × [h], mm. [room]: the room's name for the
     * floor plan's line ("Living Room · 3310 × 5458"). [fs]: the text unit, by default [fsOf] its size.
     */
    fun drawing(view: String, prep: SurveyPrep.Prep, l: Double, w: Double, h: Double, room: String? = null, fs: Double? = null): Drawing =
        when (view) {
            "floor" -> floorPlan(prep, l, w, h, room, fs)
            "ceil" -> ceilingPlan(prep, l, w, h, fs)
            in WALLS -> elevation(view, prep, l, w, h, fs)
            else -> throw IllegalArgumentException("not a drawing: $view")
        }

    /** The line under a drawing (plan.js infoOf, v52 fix: short, never cut at 360 dp). */
    fun infoOf(view: String, prep: SurveyPrep.Prep, l: Double, w: Double, h: Double, room: String? = null): String {
        if (view == "floor") return "${room?.takeIf { it.isNotBlank() } ?: "Room"} · ${rnd(w)} × ${rnd(l)}"
        val d = dims(view, prep, l, w, h); val n = prep.surfaces[view]?.marks?.size ?: 0
        return "${SurveyPrep.NAME.getValue(view).removeSuffix(" wall")} · ${rnd(d[0])} × ${rnd(d[1])} · $n mark${if (n == 1) "" else "s"}"
    }

    /** The room's name in the screen's title ("LR · Living Room", RoomToken.label): what follows the token. */
    fun roomName(title: String): String = title.substringAfter(" · ", title).trim()

    // ---- the floor plan (plan.js floorSVG)
    private fun floorPlan(prep: SurveyPrep.Prep, l: Double, w: Double, h: Double, room: String?, fsIn: Double?): Drawing {
        val fs = fsIn ?: fsOf(w, l); val k = Kit(fs, prep.actual); val actual = prep.actual
        // the walls: a band T thick outside the room's inner rectangle
        k.items.add(Poly(listOf(rect(-T, -T, w + 2 * T, l + 2 * T), rect(0.0, 0.0, w, l)), WALL, 1.0, null, 0.0, null, "walls"))
        k.items.add(Poly(listOf(rect(0.0, 0.0, w, l)), null, 1.0, INK, fs * 0.12, null, "room"))
        for (s in WALLS) {
            val ab = SurveyPrep.sizeOf(s, prep, l, w, h); val a = ab[0]; val n = inward(s)
            fun at(x: Double) = planPoint(s, x, w, l)
            val rot = if (s == "left" || s == "right") -90.0 else 0.0
            for (e in prep.surfaces[s]?.marks ?: emptyList<SurveyPrep.Mark>()) {
                val t = e.t; val a0 = at(e.x0); val a1 = at(e.x1); val mid = at((e.x0 + e.x1) / 2)
                val ms = SurveyPrep.measures(s, e, a, ab[1])
                fun get(key: String) = ms.firstOrNull { it.key == key }
                if (t == "D" || t == "W" || t == "O") {
                    // a gap through the wall band
                    k.items.add(Poly(listOf(listOf(a0, a1, moved(a1, n, -T), moved(a0, n, -T))), WHITE, 1.0, INK, fs * 0.08, null, "op", e.code, s))
                    if (t == "W") for (d in listOf(T / 3, 2 * T / 3)) {
                        val p = moved(a0, n, -d); val q = moved(a1, n, -d); k.ln(p[0], p[1], q[0], q[1], w = 0.07, role = "glass", key = e.code)
                    }
                    if (t == "D") {
                        // the swing: the leaf into the room from the hinge on the nearer corner's side, and its arc to the other jamb
                        val dw = e.x1 - e.x0; val hingeLeft = (e.x0 + e.x1) / 2 <= a / 2
                        val hp = if (hingeLeft) a0 else a1; val jp = if (hingeLeft) a1 else a0; val leaf = moved(hp, n, dw)
                        k.ln(hp[0], hp[1], leaf[0], leaf[1], w = 0.1, role = "leaf", key = e.code)
                        val st = deg(leaf[0] - hp[0], leaf[1] - hp[1]); var sw = deg(jp[0] - hp[0], jp[1] - hp[1]) - st
                        while (sw > 180) sw -= 360
                        while (sw <= -180) sw += 360
                        k.items.add(Arc(hp[0], hp[1], dw, st, sw, INK, fs * 0.06, doubleArrayOf(fs * 0.3, fs * 0.2), "swing", e.code))
                    }
                    // its width and its offset from the nearer corner, outside the wall
                    val fw = k.fig(e.code + ".w", get("w")?.est)
                    val hx = SurveyPrep.measures0(s, e, a, ab[1]).firstOrNull { it.key == "l" || it.key == "r" }
                    // an opening at the corner still says so
                    val fo = hx?.let { m -> if (m.est != null && m.est < SurveyPrep.ZERO && actual[e.code + "." + m.key] == null) Fig("0", false)
                                            else k.fig(e.code + "." + m.key, m.est) }
                    val lab = moved(mid, n, -(T + fs * 1.1)); val lab2 = moved(mid, n, -(T + fs * 2.3))
                    k.txt(lab[0], lab[1], "${e.code} ${fw.t}", if (fw.read) INK else GREY, sz = 0.85, b = true, rot = rot, role = "opl", key = e.code)
                    if (fo != null && hx != null) {
                        val sideName = if (hx.key == "l") "left" else "right"
                        k.txt(lab2[0], lab2[1], if (fo.t == "0") "at the $sideName corner" else "${fo.t} from $sideName corner",
                            if (fo.read) INK else GREY, sz = 0.7, rot = rot, role = "opo", key = e.code)
                    }
                    continue
                }
                if (t == "N") {
                    val dp = min(T * 0.8, max(40.0, orJs(actual[e.code + ".p"], 100.0)))
                    k.items.add(Poly(listOf(listOf(a0, a1, moved(a1, n, -dp), moved(a0, n, -dp))), NICHE, 1.0, INK, fs * 0.07, null, "op", e.code, s))
                    val lab = moved(mid, n, -(T + fs * 1.1))
                    k.txt(lab[0], lab[1], "${e.code} ${k.fig(e.code + ".w", get("w")?.est).t}", GREY, sz = 0.8, rot = rot, role = "opl", key = e.code)
                    continue
                }
                if (t == "C" || t == "B" || t == "L") {
                    val dp = orJs(actual[e.code + ".p"], if (t == "L") 600.0 else 300.0)
                    val poly = listOf(listOf(a0, a1, moved(a1, n, dp), moved(a0, n, dp)))
                    k.items.add(if (t == "C") Poly(poly, INK, 1.0, null, 0.0, null, "mk", e.code, s)
                                else Poly(poly, null, 1.0, INK, fs * 0.07, doubleArrayOf(fs * 0.45, fs * 0.3), "mk", e.code, s))
                    val lab = moved(mid, n, dp + fs * 0.9)
                    k.txt(lab[0], lab[1], e.code + (if (t == "B") " beam over" else if (t == "L") " loft over" else ""), sz = 0.75, b = true, role = "mkl", key = e.code)
                    continue
                }
                if (SurveyPrep.isPoint(t)) {
                    val c = moved(mid, n, fs * 0.9)
                    k.pointSym(c, t, e.code, s, "mk"); k.txt(c[0], c[1] + fs * 1.1, e.code, sz = 0.65, role = "mkl", key = e.code)
                    continue
                }
                val c = moved(mid, n, fs * 0.7)
                k.items.add(Poly(listOf(rect(c[0] - fs * 0.35, c[1] - fs * 0.35, fs * 0.7, fs * 0.7)), null, 1.0, INK, fs * 0.06,
                    doubleArrayOf(fs * 0.15, fs * 0.15), "mk", e.code, s))
                k.txt(c[0], c[1] + fs, e.code, sz = 0.65, role = "mkl", key = e.code)
            }
        }
        // the floor and the ceiling: their marks at their own plan positions (ceiling ones dashed: they are overhead)
        for (s in listOf("floor", "ceil")) for (e in prep.surfaces[s]?.marks ?: emptyList<SurveyPrep.Mark>()) {
            val t = e.t; val x0 = e.x0; val x1 = e.x1; val y0 = planY(e.y1, l); val y1 = planY(e.y0, l)
            val cx = (x0 + x1) / 2; val cy = (y0 + y1) / 2
            if (SurveyPrep.isPoint(t)) { k.pointSym(doubleArrayOf(cx, cy), t, e.code, s, "mk"); k.txt(cx, cy + fs * 1.1, e.code, sz = 0.65, role = "mkl", key = e.code); continue }
            val over = s == "ceil"
            k.items.add(Poly(listOf(rect(x0, y0, x1 - x0, y1 - y0)), if (t == "C") INK else null, 1.0, INK, fs * 0.07,
                if (over && t != "C") doubleArrayOf(fs * 0.45, fs * 0.3) else null, "mk", e.code, s))
            if (t == "S") k.ln(x0, cy, x1, cy, w = 0.05, role = "step", key = e.code)
            k.txt(cx, if (t == "C") y1 + fs * 0.8 else cy, e.code + (if (over && t == "B") " beam over" else ""), sz = 0.7, b = true, role = "mkl", key = e.code)
        }
        roomDims(k, w, l)
        k.scaleBar(0.0, l + T + fs * 6)
        val box = doubleArrayOf(-T - fs * 3.2, -T - fs * 4.6, w + 2 * T + fs * 9.2, l + 2 * T + fs * 11.6)
        return Drawing("floor", k.items, box, fs, infoOf("floor", prep, l, w, h, room), DTITLE.getValue("floor"), k.dims)
    }

    /** The room's W along the bottom and L up the right, plain lines with end ticks, and FRONT over the front wall. */
    private fun roomDims(k: Kit, w: Double, l: Double) {
        val fs = k.fs; val yW = l + T + fs * 3.6; val xL = w + T + fs * 4.4; val tk = fs * 0.4
        k.ln(0.0, yW, w, yW, role = "dimWline"); k.ln(0.0, yW - tk, 0.0, yW + tk, role = "dimWline"); k.ln(w, yW - tk, w, yW + tk, role = "dimWline")
        k.txt(w / 2, yW + fs * 0.85, "W ${rnd(w)}", b = true, sz = 0.9, role = "dimW")
        k.ln(xL, 0.0, xL, l, role = "dimLline"); k.ln(xL - tk, 0.0, xL + tk, 0.0, role = "dimLline"); k.ln(xL - tk, l, xL + tk, l, role = "dimLline")
        k.txt(xL + fs * 0.9, l / 2, "L ${rnd(l)}", b = true, sz = 0.9, rot = -90.0, role = "dimL")
        k.txt(w / 2, -T - fs * 3.6, "FRONT", LABEL, b = true, sz = 0.8, role = "frontlab")
    }

    // ---- a wall, face on (plan.js elevSVG)
    private fun elevation(s: String, prep: SurveyPrep.Prep, l: Double, w: Double, h: Double, fsIn: Double?): Drawing {
        val ab = SurveyPrep.sizeOf(s, prep, l, w, h); val a = ab[0]; val b = ab[1]
        val fs = fsIn ?: fsOf(a, b); val k = Kit(fs, prep.actual); val actual = prep.actual; val hd = fs * 0.8
        fun yy(y: Double) = elevY(y, b)
        k.items.add(Poly(listOf(rect(0.0, 0.0, a, b)), WHITE, 1.0, INK, fs * 0.1, null, "wall", s, s))
        // the floor line with a thin hatch under it, and the two corner lines
        var x = 0.0
        while (x < a) { val x2 = min(a, x + hd); k.ln(x, b + (x2 - x), x2, b, WALL, 0.035, role = "hatch"); x += fs * 0.6 }
        k.ln(-fs * 0.6, b, a + fs * 0.6, b, w = 0.16, role = "floorline")
        k.ln(0.0, -fs * 0.6, 0.0, b + hd, w = 0.14, role = "corner"); k.ln(a, -fs * 0.6, a, b + hd, w = 0.14, role = "corner")
        // every mark on the wall: its outline where its mm put it, in its colour, with its code
        class Row(val lo: Double, val hi: Double, val seg: List<Pair<DoubleArray, Pair<Fig, String>>>, val x0: Double, val x1: Double) { var lane = 0 }
        val rows = mutableListOf<Row>()
        val vdims = mutableListOf<() -> Unit>()   // the dimensions up the wall, drawn after every mark, as plan.js does
        for (e in prep.surfaces[s]?.marks ?: emptyList<SurveyPrep.Mark>()) {
            val t = e.t; val col = colourOf(s, t); val x0 = e.x0; val x1 = e.x1; val y0 = e.y0; val y1 = e.y1
            val mw = x1 - x0; val hh = y1 - y0; val cx = (x0 + x1) / 2
            val ms = SurveyPrep.measures0(s, e, a, b)
            fun f(m: SurveyPrep.Measure) = k.fig(e.code + "." + m.key, m.est)
            if (SurveyPrep.isPoint(t)) {
                val cy = (y0 + y1) / 2; val c = doubleArrayOf(cx, yy(cy))
                k.pointSym(c, t, e.code, s, "el")
                k.txt(c[0] + fs * 0.6, c[1] - fs * 0.7, e.code, a = Anchor.START, sz = 0.8, b = true, role = "elc", key = e.code)
                // v52 fix: a spot is placed by its centre -- from the nearer corner (in the row under the floor) and from
                // the floor (or the ceiling, when nearer) up to its centre, beside it
                val hx = hxOf(ms); val vy = ms.firstOrNull { Regex("^c?[st]$").matches(it.key) }
                if (hx != null) {
                    val left = side(hx.key) == 'l'
                    rows.add(Row(if (left) 0.0 else cx, if (left) cx else a,
                        listOf((if (left) doubleArrayOf(0.0, cx) else doubleArrayOf(cx, a)) to (f(hx) to e.code + "." + hx.key)), cx, cx))
                }
                if (vy != null) {
                    val vx = if (cx + fs * 0.9 < a - fs * 0.4) cx + fs * 0.9 else cx - fs * 0.9
                    val fv = f(vy); val kk = e.code + "." + vy.key
                    vdims.add(if (side(vy.key) == 's') { { k.dim(vx, yy(0.0), vx, yy(cy), fv, kk) } } else { { k.dim(vx, yy(b), vx, yy(cy), fv, kk) } })
                }
                continue
            }
            k.items.add(Poly(listOf(rect(x0, yy(y1), mw, hh)), col, if (t == "C") 0.45 else 0.18, col, fs * 0.1,
                if (t == "B" || t == "L") doubleArrayOf(fs * 0.45, fs * 0.25) else null, "el", e.code, s))
            if (t == "D") k.ln(x0, yy(y1), x1, yy(y1), w = 0.1, role = "head", key = e.code)   // the door head
            if (t == "W") k.ln(x0, yy((y0 + y1) / 2), x1, yy((y0 + y1) / 2), col, 0.05, role = "transom", key = e.code)   // so it reads as glass
            val inTop = hh >= fs * 1.7; val ly = if (inTop) yy(y1) + fs * 0.75 else yy(y1) - fs * 0.55
            k.txt(cx, ly, e.code, sz = 0.85, b = true, role = "elc", key = e.code)
            fun get(key: String) = ms.firstOrNull { it.key == key }
            val hx = hxOf(ms)
            // along the bottom: the offset from the nearer corner and the width, one row of dimension lines under the floor
            val seg = mutableListOf<Pair<DoubleArray, Pair<Fig, String>>>()
            if (hx != null && ((hx.est ?: 0.0) >= SurveyPrep.ZERO || actual[e.code + "." + hx.key] != null))
                seg.add(if (hx.key == "l") doubleArrayOf(0.0, x0) to (k.fig(e.code + ".l", hx.est) to e.code + ".l")
                        else doubleArrayOf(x1, a) to (k.fig(e.code + ".r", hx.est) to e.code + ".r"))
            if (get("w") != null) seg.add(doubleArrayOf(x0, x1) to (k.fig(e.code + ".w", mw) to e.code + ".w"))
            if (seg.isNotEmpty()) rows.add(Row(seg.minOf { it.first[0] }, seg.maxOf { it.first[1] }, seg, x0, x1))
            // up the wall: sill and head for a window, height for a door / opening / niche / other, underside for a
            // loft, drop for a beam, height from the floor for a switch plate
            fun vx(dx: Double): Double { val v = x1 + dx; return if (v > a - fs * 0.4) x0 - dx else v }
            val inside = x0 + min(mw * 0.3, fs * 1.4)
            when (t) {
                "W" -> {
                    val sf = k.fig(e.code + ".s", y0)
                    val rs = actual[e.code + ".s"]; val rh = actual[e.code + ".h"]
                    val hf = if (rs != null && rh != null) Fig(rnd(rs + rh).toString(), true) else Fig("≈" + rnd(y1), false)
                    vdims.add { k.dim(inside, yy(0.0), inside, yy(y0), sf, e.code + ".s") }
                    vdims.add { k.dim(vx(fs * 0.8), yy(0.0), vx(fs * 0.8), yy(y1), hf, e.code + ".head") }
                }
                "D", "O", "N", "X" -> {
                    if (get("h") != null) {
                        val hx2 = if (t == "D") cx else inside; val fh = k.fig(e.code + ".h", hh)
                        vdims.add { k.dim(hx2, yy(y0), hx2, yy(y1), fh, e.code + ".h") }
                    }
                    val vy = get("s") ?: get("t")
                    if (vy != null && t != "D" && ((vy.est ?: 0.0) >= SurveyPrep.ZERO || actual[e.code + "." + vy.key] != null)) {
                        val fv = k.fig(e.code + "." + vy.key, vy.est)
                        vdims.add(if (vy.key == "s") { { k.dim(vx(fs * 0.8), yy(0.0), vx(fs * 0.8), yy(y0), fv, e.code + ".s") } }
                                  else { { k.dim(vx(fs * 0.8), yy(b), vx(fs * 0.8), yy(y1), fv, e.code + ".t") } })
                    }
                }
                "L" -> { val fv = k.fig(e.code + ".s", y0); vdims.add { k.dim(cx, yy(0.0), cx, yy(y0), fv, e.code + ".s") } }
                // v52 fix: a switch plate's height from the floor
                "S" -> { val fv = k.fig(e.code + ".s", y0); vdims.add { k.dim(vx(fs * 0.8), yy(0.0), vx(fs * 0.8), yy(y0), fv, e.code + ".s") } }
                "B" -> { val fv = k.fig(e.code + ".d", b - y0); vdims.add { k.dim(inside, yy(b), inside, yy(y0), fv, e.code + ".d") } }
            }
        }
        vdims.forEach { it() }
        // the rows under the floor: packed so lines that do not overlap share a row
        val lanes = mutableListOf<Double>()
        for (r in rows.sortedBy { it.lo }) {
            var i = lanes.indexOfFirst { it <= r.lo - fs * 0.5 }
            if (i < 0) { i = lanes.size; lanes.add(0.0) }
            lanes[i] = r.hi; r.lane = i
        }
        fun rowY(i: Int) = b + hd + fs * 1.7 + i * fs * 1.9
        for (r in rows) {
            val y = rowY(r.lane)
            k.ln(r.x0, b + hd * 0.3, r.x0, y + fs * 0.3, GREY, 0.03, role = "ext"); k.ln(r.x1, b + hd * 0.3, r.x1, y + fs * 0.3, GREY, 0.03, role = "ext")
            for ((ab2, fk) in r.seg) k.dim(ab2[0], y, ab2[1], y, fk.first, fk.second)
        }
        val yb = rowY(lanes.size) - fs * 0.6
        // the wall's own size outside the rectangle: its W (or L) along the top, its H up the right side
        val ta = SurveyPrep.sizeTag(s).first; val yA = -fs * 1.6; val xH = a + fs * 1.8; val tk = fs * 0.4
        k.ln(0.0, yA - tk, 0.0, yA + tk, role = "dimA.tick"); k.ln(a, yA - tk, a, yA + tk, role = "dimA.tick")
        k.dim(0.0, yA, a, yA, Fig("$ta ${rnd(a)}", true), "A", "dimA")
        k.ln(xH - tk, 0.0, xH + tk, 0.0, role = "dimH.tick"); k.ln(xH - tk, b, xH + tk, b, role = "dimH.tick")
        k.dim(xH, 0.0, xH, b, Fig("H ${rnd(b)}", true), "H", "dimH", side = -1.0)
        // which wall meets each corner, so the drawing says which way round it is
        val ng = NEIGH.getValue(s)
        k.txt(0.0, -fs * 3.4, "◂ " + ng.first, GREY, Anchor.START, 0.75, role = "neigh")
        k.txt(a, -fs * 3.4, ng.second + " ▸", GREY, Anchor.END, 0.75, role = "neigh")
        k.txt(a / 2, yb + fs * 1.4, SurveyPrep.NAME.getValue(s).uppercase(), LABEL, b = true, sz = 0.8, role = "walllab")
        k.scaleBar(0.0, yb + fs * 3)
        val box = doubleArrayOf(-fs * 2, -fs * 4.4, a + fs * 5.8, yb + fs * 4.2 + fs * 4.4)
        return Drawing(s, k.items, box, fs, infoOf(s, prep, l, w, h), DTITLE.getValue(s), k.dims)
    }

    // ---- the reflected ceiling plan (plan.js ceilSVG)
    private fun ceilingPlan(prep: SurveyPrep.Prep, l: Double, w: Double, h: Double, fsIn: Double?): Drawing {
        val fs = fsIn ?: fsOf(w, l); val k = Kit(fs, prep.actual); val actual = prep.actual
        k.items.add(Poly(listOf(rect(-T, -T, w + 2 * T, l + 2 * T), rect(0.0, 0.0, w, l)), WALL, 1.0, null, 0.0, null, "walls"))
        k.items.add(Poly(listOf(rect(0.0, 0.0, w, l)), WHITE, 1.0, INK, fs * 0.12, null, "room"))
        // what hangs from the walls is seen on the ceiling plan too: wall columns, beams and lofts, as on the floor plan
        for (s in WALLS) {
            val n = inward(s)
            fun at(x: Double) = planPoint(s, x, w, l)
            for (e in prep.surfaces[s]?.marks ?: emptyList<SurveyPrep.Mark>()) {
                val t = e.t; if (t != "C" && t != "B" && t != "L") continue
                val a0 = at(e.x0); val a1 = at(e.x1); val mid = at((e.x0 + e.x1) / 2)
                val dp = orJs(actual[e.code + ".p"], if (t == "L") 600.0 else 300.0); val col = colourOf(s, t)
                k.items.add(Poly(listOf(listOf(a0, a1, moved(a1, n, dp), moved(a0, n, dp))), if (t == "C") INK else col,
                    if (t == "C") 1.0 else 0.22, INK, fs * 0.07, null, "el", e.code, s))
                val lab = moved(mid, n, dp + fs * 0.9)
                k.txt(lab[0], lab[1], e.code + (if (t == "B") " beam" else if (t == "L") " loft" else ""), sz = 0.7, b = true, role = "elc", key = e.code)
            }
        }
        // the ceiling's own marks at their plan mm, each with its code and its distances from the nearer walls
        val abC = SurveyPrep.sizeOf("ceil", prep, l, w, h)
        for (e in prep.surfaces["ceil"]?.marks ?: emptyList<SurveyPrep.Mark>()) {
            val t = e.t; val x0 = e.x0; val x1 = e.x1; val y0 = planY(e.y1, l); val y1 = planY(e.y0, l)
            val cx = (x0 + x1) / 2; val cy = (y0 + y1) / 2; val col = colourOf("ceil", t); val pt = SurveyPrep.isPoint(t)
            if (pt) {
                k.pointSym(doubleArrayOf(cx, cy), t, e.code, "ceil", "el")
                k.txt(cx + fs * 0.6, cy - fs * 0.6, e.code, a = Anchor.START, sz = 0.8, b = true, role = "elc", key = e.code)
            } else {
                val fill = if (t == "C") INK else col; val op = if (t == "C") 1.0 else if (t == "O") 0.0 else 0.22
                k.items.add(Poly(listOf(rect(x0, y0, x1 - x0, y1 - y0)), fill, op, if (t == "C") INK else col, fs * 0.1,
                    if (t == "X") doubleArrayOf(fs * 0.3, fs * 0.3) else null, "el", e.code, "ceil"))
                // an opening in the ceiling: crossed through
                if (t == "O") { k.ln(x0, y0, x1, y1, col, 0.05, role = "cross", key = e.code); k.ln(x0, y1, x1, y0, col, 0.05, role = "cross", key = e.code) }
                k.txt(cx, if (t == "C") y1 + fs * 0.8 else cy, e.code + (if (t == "B") " beam" else ""), sz = 0.8, b = true,
                    rot = if (t == "B" && (y1 - y0) > (x1 - x0) * 2) -90.0 else 0.0, role = "elc", key = e.code)
            }
            val ms = SurveyPrep.measures0("ceil", e, abC[0], abC[1])
            val hx = hxOf(ms); val vy = ms.firstOrNull { Regex("^c?[fk]$").matches(it.key) }
            fun show(m: SurveyPrep.Measure?) = m != null && (pt || (m.est ?: 0.0) >= SurveyPrep.ZERO || actual[e.code + "." + m.key] != null)
            if (hx != null && show(hx)) {
                val xe = if (pt) cx else if (side(hx.key) == 'l') x0 else x1; val fh = k.fig(e.code + "." + hx.key, hx.est)
                if (side(hx.key) == 'l') k.dim(0.0, cy, xe, cy, fh, e.code + "." + hx.key) else k.dim(xe, cy, w, cy, fh, e.code + "." + hx.key)
            }
            // from the front wall (the top of the page) or from the back wall (its bottom)
            if (vy != null && show(vy)) {
                val ye = if (pt) cy else if (side(vy.key) == 'f') y0 else y1; val fv = k.fig(e.code + "." + vy.key, vy.est)
                if (side(vy.key) == 'f') k.dim(cx, 0.0, cx, ye, fv, e.code + "." + vy.key) else k.dim(cx, ye, cx, l, fv, e.code + "." + vy.key)
            }
        }
        // the room's W and L, FRONT over the front wall, the drawing's name under the back wall, the 1 m bar
        roomDims(k, w, l)
        k.txt(w / 2, l + T + fs * 5.6, "REFLECTED CEILING PLAN", LABEL, b = true, sz = 0.7, role = "rcplab")
        k.scaleBar(0.0, l + T + fs * 7.2)
        val box = doubleArrayOf(-T - fs * 3.2, -T - fs * 4.6, w + 2 * T + fs * 9.2, l + 2 * T + fs * 12.6)
        return Drawing("ceil", k.items, box, fs, infoOf("ceil", prep, l, w, h), DTITLE.getValue("ceil"), k.dims)
    }

    // ------------------------------------------------------------------ fit, pinch and pan (plan.js renderPlan)

    /**
     * What part of a drawing is on screen: [vb] (x, y, width, height, mm), shown whole and centred in a [cw] × [ch] px box
     * at [scale] px per mm. FITTED is the drawing's own box; zoomed in is a smaller vb. The same sums as plan.js's
     * viewBox with preserveAspectRatio="xMidYMid meet".
     */
    fun scale(vb: DoubleArray, cw: Double, ch: Double): Double = min(cw / vb[2], ch / vb[3])

    /** A drawing point (mm) on the screen (px). */
    fun toScreen(vb: DoubleArray, cw: Double, ch: Double, x: Double, y: Double): DoubleArray {
        val k = scale(vb, cw, ch)
        return doubleArrayOf((cw - vb[2] * k) / 2 + (x - vb[0]) * k, (ch - vb[3] * k) / 2 + (y - vb[1]) * k)
    }

    /** A screen point (px) on the drawing (mm). */
    fun fromScreen(vb: DoubleArray, cw: Double, ch: Double, sx: Double, sy: Double): DoubleArray {
        val k = scale(vb, cw, ch)
        return doubleArrayOf(vb[0] + (sx - (cw - vb[2] * k) / 2) / k, vb[1] + (sy - (ch - vb[3] * k) / 2) / k)
    }

    /** Whether [vb] shows the whole of [box]. */
    fun fitted(vb: DoubleArray, box: DoubleArray): Boolean = vb[2] >= box[2] - 1e-6

    /**
     * Zoomed by [f] (> 1 in) about the screen point ([sx], [sy]): the drawing point under it stays under it; never wider
     * than the drawing's [box] (then it is fitted: the box itself) nor narrower than an eighth of it.
     */
    fun zoom(vb: DoubleArray, box: DoubleArray, f: Double, sx: Double, sy: Double, cw: Double, ch: Double): DoubleArray {
        if (!(f > 0) || f.isInfinite()) return vb
        val u = fromScreen(vb, cw, ch, sx, sy)
        val nw = min(box[2], max(box[2] / 8, vb[2] / f)); val nh = vb[3] * nw / vb[2]
        val out = doubleArrayOf(u[0] - (u[0] - vb[0]) * nw / vb[2], u[1] - (u[1] - vb[1]) * nh / vb[3], nw, nh)
        return if (fitted(out, box)) box.copyOf() else out
    }

    /** A drag of ([dx], [dy]) px moves a zoomed drawing with the finger; a fitted one stays put. */
    fun pan(vb: DoubleArray, box: DoubleArray, dx: Double, dy: Double, cw: Double, ch: Double): DoubleArray {
        if (fitted(vb, box)) return vb
        val k = scale(vb, cw, ch)
        return doubleArrayOf(vb[0] - dx / k, vb[1] - dy / k, vb[2], vb[3])
    }

    /**
     * Where drawing point (0, 0) lands on the screen, px: with [scale] it is the whole mm-to-px mapping the Canvas paints
     * by -- `screen = origin + mm × k`, the same sums as [toScreen], so the painted picture and the zoom anchor
     * ([fromScreen]) can never disagree.
     */
    fun origin(vb: DoubleArray, cw: Double, ch: Double): DoubleArray = toScreen(vb, cw, ch, 0.0, 0.0)

    /**
     * One gesture event on the view [vb] (null = fitted, the drawing's [box]), plan.js's rule (pointermove): a PINCH only
     * zooms, about the fingers' centroid ([cx], [cy]), and never pans -- however far the centroid drifts; a one-finger
     * drag ([factor] == 1) only pans. The result is null when it is fitted, the box shown whole.
     */
    fun gesture(vb: DoubleArray?, box: DoubleArray, cx: Double, cy: Double, panX: Double, panY: Double, factor: Double,
                cw: Double, ch: Double): DoubleArray? {
        if (!(cw > 0) || !(ch > 0)) return vb
        val v0 = vb ?: box
        val v = if (factor != 1.0) zoom(v0, box, factor, cx, cy, cw, ch) else pan(v0, box, panX, panY, cw, ch)
        return if (fitted(v, box)) null else v
    }

    /** The fit button: "fit" when fitted, else the zoom on it, "fit · 150%" (v52 fix). */
    fun fitLabel(vb: DoubleArray, box: DoubleArray): String =
        if (fitted(vb, box)) "fit" else "fit · ${rnd(box[2] / vb[2] * 100)}%"
}
