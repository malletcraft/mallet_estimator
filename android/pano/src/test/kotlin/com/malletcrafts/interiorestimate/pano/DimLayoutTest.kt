package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch.Box
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHERE EVERY FIGURE GOES ON THE PHOTO (port 63 slice M1; DESIGN-port63 §1.6): the placing rule, on the design's own
 * example wall -- a door, a window, a switch plate and three electric points on a 2678 x 2677 front wall. Its 19
 * figures are 8 plates as 3 · Measure opens, and all 19 with `all` on.
 *
 * TWO CANVASES ([Rig]). The rule's own steps are worked on [fit], 340 px square at 1 px a dp and 1 px a unit, so
 * every figure in those tests is a round one. THE PHONE'S OWN NUMBERS are [phone]: the Mi A3's photo, about 340 dp =
 * 680 px square at 2 px a dp, the painter's unit 680 / 480 px = 0.71 dp (SurveyPrepScreen: `u = canvas / 480`). What
 * is claimed of the example wall -- how many plates, how many dots -- is pinned on BOTH, and the review of M1 added
 * the zoomed and panned canvas, where a mark can be wholly off it.
 */
class DimLayoutTest {

    private val W = 2678.0; private val H = 2677.0

    private fun mk(t: String, n: Int, x0: Double, x1: Double, y0: Double, y1: Double, extra: Map<String, Any?> = emptyMap()) =
        SurveyPrep.Mark("id-$t$n", t, "F$t$n", x0, x1, y0, y1, extra)

    private val door = mk("D", 1, 150.0, 1050.0, 0.0, 2100.0)
    private val window = mk("W", 1, 1350.0, 2550.0, 900.0, 2100.0)
    private val switch = mk("S", 1, 1100.0, 1300.0, 1100.0, 1190.0)
    private val points = listOf(mk("E", 1, 1475.0, 1525.0, 275.0, 325.0), mk("E", 2, 1975.0, 2025.0, 275.0, 325.0), mk("E", 3, 2475.0, 2525.0, 275.0, 325.0))
    private val wall = listOf(door, window, switch) + points

    private fun hit(a: Box, b: Box) = a.l < b.r && a.r > b.l && a.t < b.b && a.b > b.t
    private fun same(a: Box, b: Box) = abs(a.l - b.l) < 1e-9 && abs(a.t - b.t) < 1e-9 && abs(a.r - b.r) < 1e-9 && abs(a.b - b.b) < 1e-9
    private fun l(b: Box) = listOf(b.l, b.t, b.r, b.b)

    /** The boxes drawn that are not dots: each plate once, a compact plate whole. */
    private fun solid(out: DimLayout.Out): List<Box> = out.plates.filter { !it.dot }.map { it.whole ?: it.box }.distinctBy { l(it) }

    /**
     * ONE CANVAS: [size] px square, the painter's unit [u] px and [dp] px a dp; the wall filling it, or at [zoom]
     * with the face's ([cx], [cy]) mm at the canvas's centre -- the painter's own mapping through zoom and pan.
     */
    private inner class Rig(val size: Double, val u: Double, val dp: Double, val zoom: Double = 1.0, val cx: Double = W / 2, val cy: Double = H / 2) {
        /** Face mm to canvas px, y down. */
        fun toPx(x: Double, y: Double) = if (zoom == 1.0 && cx == W / 2 && cy == H / 2) doubleArrayOf(x * size / W, size - y * size / H)
            else doubleArrayOf(size / 2 + (x - cx) * size / W * zoom, size / 2 - (y - cy) * size / H * zoom)

        /** A monospace figure: 0.6 of its size a character (the painter measures the real text). */
        fun textW(text: String, fs: Double, bold: Boolean) = text.length * fs * 0.6
        val times: Double get() = textW(" × ", 10.0 * dp, false)

        /** The same canvas zoomed [z] times onto the face's ([x], [y]) mm. */
        fun at(z: Double, x: Double, y: Double) = Rig(size, u, dp, z, x, y)

        /** The example wall as the painter hands it over: [picked] the picked mark's id, [active] the key being measured. */
        fun marks(picked: String? = null, active: String? = null, actual: Map<String, Double> = emptyMap(),
                  tabs: Boolean = false, els: List<SurveyPrep.Mark> = wall, live: String? = null): List<DimLayout.Mark> =
            els.map { e -> DimLayout.mark(actual, "front", e, W, H, e.id == picked, tabs && e.id == picked && !SurveyPrep.isPoint(e.t) && !SurveyPrep.isLocked(e),
                active, ::toPx, u, dp, ::textW, live) }

        /** Where each mark's code is drawn (13 units high): inside a box's top-left corner, beside a point. */
        fun codes(els: List<SurveyPrep.Mark> = wall): List<Box> = els.map { e ->
            val p = toPx(e.x0, e.y1); val w = textW(e.code, 13.0 * u, true)
            if (SurveyPrep.isPoint(e.t)) Box(p[0] + 14 * u, p[1] - 18 * u, p[0] + 14 * u + w, p[1] - 2 * u) else Box(p[0] + 6 * u, p[1] + 3 * u, p[0] + 6 * u + w, p[1] + 19 * u)
        }

        /** The fit / grid / all column in the photo's top-right corner: the screen's own, so in dp. */
        val column = Box(size - 50 * dp, 6.0 * dp, size - 6 * dp, 84.0 * dp)
        fun keep(els: List<SurveyPrep.Mark> = wall) = codes(els) + column

        fun place(ms: List<DimLayout.Mark>, scope: OldPlace.Scope, keep: List<Box> = keep()) =
            OldPlace.place(ms, scope, size, size, keep, u, dp, times)

        /**
         * What must hold of every layout: every plate inside the photo, no plate on another, none on a kept box, taps
         * of 32 dp; every dot on the canvas unless its dimension is off it ([DimLayout.Plate.off]: it is never slid
         * in); and NO DOT UNDER A PLATE OR ANOTHER DOT.
         */
        fun sound(out: DimLayout.Out, keep: List<Box>, forced: Set<String> = emptySet(), what: String) {
            val edge = DimLayout.EDGE_U * u
            for (p in out.plates) {
                val b = p.whole ?: p.box
                val e = if (p.dot) 0.0 else edge
                if (!(p.dot && p.off))
                    assertTrue(b.l >= e - 1e-9 && b.t >= e - 1e-9 && b.r <= size - e + 1e-9 && b.b <= size - e + 1e-9, "$what: ${p.code} is off the photo: $b")
                assertTrue(p.tap.w >= 32 * dp - 1e-9 && p.tap.h >= 32 * dp - 1e-9, "$what: ${p.code}'s tap box is ${p.tap.w} x ${p.tap.h}")
                assertTrue(p.tap.l <= p.box.l + 1e-9 && p.tap.r >= p.box.r - 1e-9 && p.tap.t <= p.box.t + 1e-9 && p.tap.b >= p.box.b - 1e-9, "$what: ${p.code}'s tap box holds its plate")
                if (!p.dot && p.code !in forced) for (k in keep) assertFalse(hit(b, k), "$what: ${p.code} $b covers a kept box $k")
            }
            val s = solid(out)
            for (i in s.indices) for (j in i + 1 until s.size) assertFalse(hit(s[i], s[j]), "$what: two plates overlap: ${s[i]} and ${s[j]}")
            // a dot is seen: under no plate, and not on another dot
            val dots = out.plates.filter { it.dot }
            for (d in dots) {
                for (b in s) assertFalse(hit(d.box, b), "$what: the dot ${d.code} ${d.box} lies under a plate $b")
                for (o in dots) if (o !== d) assertFalse(hit(d.box, o.box), "$what: the dots ${d.code} and ${o.code} share a spot ${d.box}")
            }
            // every code once
            assertEquals(out.plates.size, out.plates.map { it.code }.toSet().size, what)
            assertEquals(out.plates.map { it.code }, out.tags.map { it.code }, what)
        }
    }

    /** The round canvas the rule's steps are worked on, and the phone's own. */
    private val fit = Rig(340.0, 1.0, 1.0)
    private val phone = Rig(680.0, 680.0 / 480, 2.0)

    private val SIZE = fit.size; private val U = fit.u; private val DP = fit.dp
    private val TIMES = fit.times
    private fun toPx(x: Double, y: Double) = fit.toPx(x, y)
    private fun textW(text: String, fs: Double, bold: Boolean) = fit.textW(text, fs, bold)
    private fun marks(picked: String? = null, active: String? = null, actual: Map<String, Double> = emptyMap(),
                      tabs: Boolean = false, els: List<SurveyPrep.Mark> = wall) = fit.marks(picked, active, actual, tabs, els)
    private fun keep(els: List<SurveyPrep.Mark> = wall) = fit.keep(els)
    private fun place(ms: List<DimLayout.Mark>, scope: OldPlace.Scope, keep: List<Box> = keep()) = fit.place(ms, scope, keep)
    private fun sound(out: DimLayout.Out, keep: List<Box>, forced: Set<String> = emptySet(), what: String) = fit.sound(out, keep, forced, what)

    // ------------------------------------------------------------------ the constants, the lines, the scopes

    @Test fun `a dimension's line runs where build 332 draws it`() {
        val fs = 12.0
        fun seg(e: SurveyPrep.Mark, key: String) = MarkDims.of("front", e, W, H).first { it.key == key }
            .let { DimLayout.line(it, SurveyPrep.isPoint(e.t), ::toPx, fs, U) }
        val p = toPx(window.x0, window.y1); val q = toPx(window.x1, window.y0)        // its top-left and bottom-right on the canvas
        val midX = (p[0] + q[0]) / 2; val midY = (p[1] + q[1]) / 2
        // w: 6 units above the top edge, corner to corner
        seg(window, "w")!!.let { assertEquals(listOf(p[0], p[1] - 6, q[0], p[1] - 6), listOf(it.x1, it.y1, it.x2, it.y2)); assertFalse(it.vertical) }
        // h: 6 units right of the right edge
        seg(window, "h")!!.let { assertEquals(listOf(q[0] + 6, q[1], q[0] + 6, p[1]), listOf(it.x1, it.y1, it.x2, it.y2)); assertTrue(it.vertical) }
        // the corner line 2.6 text sizes under mid-height, from the mark's right edge to the right corner
        seg(window, "r")!!.let { assertEquals(listOf(q[0], midY + 2.6 * fs, SIZE, midY + 2.6 * fs), listOf(it.x1, it.y1, it.x2, it.y2)) }
        // the floor line 1.6 text sizes left of centre, from the floor up to the sill
        seg(window, "s")!!.let { assertEquals(listOf(midX - 1.6 * fs, SIZE, midX - 1.6 * fs, q[1]), listOf(it.x1, it.y1, it.x2, it.y2)) }
        // a p has no line: it is anchored at the lower-right corner
        val pd = MarkDims.of("front", window, W, H).first { it.key == "p" }
        assertNull(DimLayout.line(pd, false, ::toPx, fs, U))
        assertEquals(listOf(q[0], q[1]), DimLayout.anchor(pd, ::toPx).toList())
        // a point's two lines run through its centre, unshifted
        val e = points[0]; val c = toPx(1500.0, 300.0)
        seg(e, "cr")!!.let { assertEquals(listOf(c[0], c[1], SIZE, c[1]), listOf(it.x1, it.y1, it.x2, it.y2)) }
        seg(e, "cs")!!.let { assertEquals(listOf(c[0], SIZE, c[0], c[1]), listOf(it.x1, it.y1, it.x2, it.y2)) }
        // a beam's drop: from its underside up to the ceiling, through its middle
        val beam = mk("B", 1, 0.0, W, H - 400, H)
        seg(beam, "d")!!.let { assertEquals(listOf(SIZE / 2, toPx(0.0, H - 400)[1], SIZE / 2, toPx(0.0, H)[1]), listOf(it.x1, it.y1, it.x2, it.y2)); assertTrue(it.vertical) }
        // the painter's constants, taken over from SurveyPrepDraw
        assertEquals(6.0, DimLayout.LINE_OFF_U); assertEquals(2.6, DimLayout.CORNER_UNDER_FS); assertEquals(1.6, DimLayout.FLOOR_LEFT_FS)
        assertEquals(1.0, DimLayout.LINE_U); assertEquals(4.0, DimLayout.ARROW_U)
        assertEquals(46.0, DimLayout.plateW(36.0, DP)); assertEquals(19.0, DimLayout.plateH(12.0, DP)); assertEquals(17.0, DimLayout.plateH(10.0, DP))
        assertEquals(12.0, DimLayout.textSize(true, DP)); assertEquals(10.0, DimLayout.textSize(false, DP)); assertEquals(24.0, DimLayout.textSize(true, 2.0))
    }

    @Test fun `which figures a scope shows, and the order they are placed in`() {
        val all = MarkDims.Role.values().toList()
        val size = setOf(MarkDims.Role.WIDTH, MarkDims.Role.HEIGHT)
        // ... and a four-corner mark's four sides and its diagonal ARE its size (port 63 slice Q1; DESIGN-quad §3.5): the
        // two roles join the two that were, and a rectangle's w and h show as they did
        val quad = setOf(MarkDims.Role.SIDE, MarkDims.Role.DIAG)
        // ... and ITEM, a Tape's, a Line's, an area's or a chain's figure placed with the marks' plates (port 63 slice I1c;
        // DESIGN-imagemeter §21, P5): never a mark's dimension, never its size
        assertEquals(setOf(MarkDims.Role.WIDTH, MarkDims.Role.HEIGHT, MarkDims.Role.OFF_H, MarkDims.Role.OFF_V, MarkDims.Role.DROP, MarkDims.Role.DEPTH) + quad + MarkDims.Role.ITEM, all.toSet())
        for (r in all) {
            // 2 · Mark: the picked mark, everything; nobody else
            assertTrue(OldPlace.shows(OldPlace.Scope.PICKED, true, r)); assertFalse(OldPlace.shows(OldPlace.Scope.PICKED, false, r))
            // 3 · Measure: every mark its size, the picked mark its position too
            assertTrue(OldPlace.shows(OldPlace.Scope.FACE, true, r)); assertEquals(r in size || r in quad, OldPlace.shows(OldPlace.Scope.FACE, false, r))
            assertEquals(r in size || r in quad, DimLayout.isSize(r))
            // all: every figure of every mark
            assertTrue(OldPlace.shows(OldPlace.Scope.ALL, true, r)); assertTrue(OldPlace.shows(OldPlace.Scope.ALL, false, r))
        }
        assertEquals(listOf("ACTIVE", "PICKED", "UNMEASURED", "MEASURED", "AWAY"), DimLayout.Rank.values().map { it.name })
        assertEquals(DimLayout.Rank.ACTIVE, DimLayout.rank(active = true, picked = true, away = true, measured = true))
        assertEquals(DimLayout.Rank.PICKED, DimLayout.rank(active = false, picked = true, away = true, measured = true))
        assertEquals(DimLayout.Rank.AWAY, DimLayout.rank(active = false, picked = false, away = true, measured = true))
        assertEquals(DimLayout.Rank.MEASURED, DimLayout.rank(active = false, picked = false, away = false, measured = true))
        assertEquals(DimLayout.Rank.UNMEASURED, DimLayout.rank(active = false, picked = false, away = false, measured = false))
    }

    @Test fun `the painter's input -- a figure's text, size and rank are decided here`() {
        val actual = mapOf("FD1.w" to 1400.0, "FD1.h" to 2104.0)
        val m = DimLayout.mark(actual, "front", door, W, H, picked = true, tabs = true, active = "FD1.l", toPx = ::toPx, u = U, dp = DP, textW = ::textW)
        assertEquals(listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p"), m.figs.map { it.code })
        assertTrue(m.picked && m.tabs); assertEquals("id-D1", m.id)
        val p = toPx(door.x0, door.y1); val q = toPx(door.x1, door.y0)
        assertEquals(listOf(p[0], p[1], q[0], q[1]), l(m.box))
        // the picked mark's figures at 12; "1400!" (the check fires: ≈900 on the photo), "2104", "▶≈150", "⊥ ?"
        assertEquals(listOf(12.0, 12.0, 12.0, 12.0), m.figs.map { it.fs })
        assertEquals(listOf("1400!", "2104", "▶≈150", "⊥ ?").map { DimLayout.plateW(textW(it, 12.0, false), DP) }, m.figs.map { it.w })
        assertEquals(listOf(DimLayout.Rank.PICKED, DimLayout.Rank.PICKED, DimLayout.Rank.ACTIVE, DimLayout.Rank.PICKED), m.figs.map { it.rank })
        assertEquals(listOf(true, true, false, false), m.figs.map { it.measured })
        // what the painter draws on each plate rides along: the text without its !, its state, the check, the active one
        assertEquals(listOf("1400", "2104", "▶≈150", "⊥ ?"), m.figs.map { it.text })
        assertEquals(listOf(MarkDims.State.MEASURED, MarkDims.State.MEASURED, MarkDims.State.ESTIMATE, MarkDims.State.UNKNOWN), m.figs.map { it.state })
        assertEquals(listOf(true, false, false, false), m.figs.map { it.check }); assertEquals(listOf(false, false, true, false), m.figs.map { it.active })
        val placed = OldPlace.place(listOf(m), OldPlace.Scope.FACE, SIZE, SIZE, emptyList(), U, DP, TIMES)
        assertEquals(m.figs.map { it.text }, placed.plates.map { it.fig.text }); assertEquals("id-D1", placed.plates[0].markId)
        // an upright plate's text starts 5 inside its box and sits 5 above its bottom, as the painter's tag did
        val wp = placed.of("FD1.w")!!
        assertEquals(wp.box.l + 5, DimLayout.textLeft(wp.box, DP), 1e-9); assertEquals(wp.box.b - 5, DimLayout.baseline(wp.box, DP), 1e-9)
        // a p has no line and an anchor; every other figure a line and none
        assertEquals(listOf(true, true, true, false), m.figs.map { it.line != null }); assertEquals(listOf(false, false, false, true), m.figs.map { it.at != null })
        // not picked: 10, and ranked by whether it is measured; a mark that is not in the room ranks last
        val other = DimLayout.mark(actual, "front", door, W, H, picked = false, tabs = false, active = null, toPx = ::toPx, u = U, dp = DP, textW = ::textW)
        assertEquals(listOf(10.0, 10.0, 10.0, 10.0), other.figs.map { it.fs })
        assertEquals(listOf(DimLayout.Rank.MEASURED, DimLayout.Rank.MEASURED, DimLayout.Rank.UNMEASURED, DimLayout.Rank.UNMEASURED), other.figs.map { it.rank })
        val wish = mk("D", 2, 150.0, 1050.0, 0.0, 2100.0, mapOf(MarkLife.KEY_BORN to MarkLife.WISH))
        val away = DimLayout.mark(mapOf("FD2.w" to 900.0), "front", wish, W, H, false, false, null, ::toPx, U, DP, ::textW)
        assertEquals(listOf(DimLayout.Rank.AWAY, DimLayout.Rank.AWAY, DimLayout.Rank.AWAY, DimLayout.Rank.AWAY), away.figs.map { it.rank })
        assertEquals(listOf(true, false, false, false), away.figs.map { it.measured })     // a reading it holds from an earlier build is still shown as one
        // a Note asks nothing: no figures
        assertEquals(0, DimLayout.mark(actual, "front", mk(MarkTypes.NOTE, 1, 300.0, 350.0, 900.0, 950.0), W, H, true, false, null, ::toPx, U, DP, ::textW).figs.size)
    }

    // ------------------------------------------------------------------ the rule, one step at a time

    private fun fig(code: String, x1: Double, y1: Double, x2: Double, y2: Double, rank: DimLayout.Rank, w: Double = 40.0,
                    role: MarkDims.Role = MarkDims.Role.OFF_H, measured: Boolean = false) =
        DimLayout.Fig(code, role, DimLayout.Seg(x1, y1, x2, y2), null, w, 10.0, rank, measured)
    private fun one(vararg figs: DimLayout.Fig) = listOf(DimLayout.Mark("m", Box(0.0, 0.0, 10.0, 10.0), figs.toList()))

    @Test fun `a plate sits mid-span on its line, and the h plate is turned to read bottom to top`() {
        val out = place(marks(), OldPlace.Scope.FACE)
        val w = assertNotNull(out.of("FW1.w")); val h = assertNotNull(out.of("FW1.h"))
        val wl = assertNotNull(w.line); val hl = assertNotNull(h.line)
        // the window's w: on its line, centred on its middle, upright, 17 high
        assertEquals(DimLayout.How.ON_LINE, w.how); assertFalse(w.turned); assertNull(w.leader); assertFalse(w.arrowsIn)
        assertEquals(wl.at(0.5).toList(), listOf((w.box.l + w.box.r) / 2, (w.box.t + w.box.b) / 2))
        assertEquals(DimLayout.plateW(textW("≈1200", 10.0, false), DP), w.box.w, 1e-9); assertEquals(17.0, w.box.h, 1e-9)
        // its h: turned -- the plate's WIDTH runs up the line -- and centred on the line's middle but for the photo's edge
        assertEquals(DimLayout.How.ON_LINE, h.how); assertTrue(h.turned)
        assertEquals(17.0, h.box.w, 1e-9); assertEquals(DimLayout.plateW(textW("≈1200", 10.0, false), DP), h.box.h, 1e-9)
        assertEquals(hl.at(0.5)[1], (h.box.t + h.box.b) / 2, 1e-9)
        assertTrue(abs((h.box.l + h.box.r) / 2 - hl.x1) <= 8.5)
        // the door's h, alone on the wall with room either side: exactly on the middle of its line
        val dh = assertNotNull(place(marks(els = listOf(door)), OldPlace.Scope.FACE, keep(listOf(door))).of("FD1.h"))
        assertEquals(assertNotNull(dh.line).at(0.5).toList(), listOf((dh.box.l + dh.box.r) / 2, (dh.box.t + dh.box.b) / 2))
        // ... and on the example wall, where the switch plate's code stands on that middle, a quarter along it instead
        val moved = assertNotNull(out.of("FD1.h"))
        assertEquals(DimLayout.How.ON_LINE, moved.how)
        assertEquals(assertNotNull(moved.line).at(0.25).toList(), listOf((moved.box.l + moved.box.r) / 2, (moved.box.t + moved.box.b) / 2))
    }

    @Test fun `a span too short for its plate -- the plate steps off by one plate height on a leader, arrowheads pointing in`() {
        // a 30 px span and a 40 px plate: it cannot sit on the line
        val out = place(one(fig("A.l", 100.0, 200.0, 130.0, 200.0, DimLayout.Rank.UNMEASURED)), OldPlace.Scope.ALL, emptyList())
        val a = assertNotNull(out.of("A.l"))
        assertEquals(DimLayout.How.OFF_LINE, a.how); assertTrue(a.arrowsIn)
        // above the line by one plate height, centre to line: the plate's bottom edge half a plate height over it
        assertEquals(listOf(115.0 - 20, 200.0 - 17 - 8.5, 115.0 + 20, 200.0 - 8.5), l(a.box))
        val ld = assertNotNull(a.leader)
        assertEquals(listOf(115.0, 200.0, 115.0, 200.0 - 8.5), listOf(ld.x1, ld.y1, ld.x2, ld.y2))
        // 8 dp of room more than the plate is exactly enough to sit on the line
        val fits = place(one(fig("A.l", 100.0, 200.0, 148.0, 200.0, DimLayout.Rank.UNMEASURED)), OldPlace.Scope.ALL, emptyList()).of("A.l")!!
        assertEquals(DimLayout.How.ON_LINE, fits.how); assertFalse(fits.arrowsIn)
        val short = place(one(fig("A.l", 100.0, 200.0, 147.9, 200.0, DimLayout.Rank.UNMEASURED)), OldPlace.Scope.ALL, emptyList()).of("A.l")!!
        assertEquals(DimLayout.How.OFF_LINE, short.how); assertTrue(short.arrowsIn)
        // an upright plate on a vertical line needs only its HEIGHT of span: a 30 px floor line holds it
        val v = place(one(fig("A.s", 200.0, 300.0, 200.0, 270.0, DimLayout.Rank.UNMEASURED, role = MarkDims.Role.OFF_V)), OldPlace.Scope.ALL, emptyList()).of("A.s")!!
        assertEquals(DimLayout.How.ON_LINE, v.how); assertFalse(v.turned)
        assertEquals(listOf(180.0, 285.0 - 8.5, 220.0, 285.0 + 8.5), l(v.box))
    }

    @Test fun `a place taken -- a quarter and three quarters along the line, then off it on a leader, then a dot`() {
        // five figures on lines 3 px apart, each wanting the same middle
        val figs = (0 until 5).map { i -> fig("A.$i", 20.0, 150.0 + 3 * i, 320.0, 150.0 + 3 * i, DimLayout.Rank.UNMEASURED, measured = i == 4) }
        val out = place(one(*figs.toTypedArray()), OldPlace.Scope.ALL, emptyList())
        sound(out, emptyList(), what = "the queue")
        val p = figs.indices.map { out.of("A.$it")!! }
        fun cx(b: Box) = (b.l + b.r) / 2
        // the first at mid-span, the second a quarter along, the third at three quarters: all on their lines
        assertEquals(listOf(DimLayout.How.ON_LINE, DimLayout.How.ON_LINE, DimLayout.How.ON_LINE), p.take(3).map { it.how })
        assertEquals(listOf(170.0, 95.0, 245.0), p.take(3).map { cx(it.box) })
        // the fourth steps off its line on a leader: above is the first plate, so below -- one plate height, centre to line
        assertEquals(DimLayout.How.OFF_LINE, p[3].how); assertFalse(p[3].arrowsIn)       // its span is long enough: the arrows stay as they are
        assertEquals(listOf(150.0, 159.0 + 8.5, 190.0, 159.0 + 8.5 + 17), l(p[3].box))
        assertEquals(listOf(170.0, 159.0, 170.0, 167.5), assertNotNull(p[3].leader).let { listOf(it.x1, it.y1, it.x2, it.y2) })
        // the fifth finds the middle taken above and below, and steps off a quarter along instead
        assertEquals(DimLayout.How.OFF_LINE, p[4].how)
        assertEquals(listOf(75.0, 162.0 + 8.5, 115.0, 162.0 + 8.5 + 17), l(p[4].box))
        // a sixth and a seventh still find room, one plate further out: nobody is a dot while there is room on the photo
        val more = place(one(*(0 until 7).map { i -> fig("A.$i", 20.0, 150.0 + 3 * i, 320.0, 150.0 + 3 * i, DimLayout.Rank.UNMEASURED) }.toTypedArray()), OldPlace.Scope.ALL, emptyList())
        sound(more, emptyList(), what = "seven on one place"); assertEquals(0, more.dots)
        // with everything round the line taken there is no room: a 6 dp dot on its line, with a 32 dp tap box
        val wallOff = listOf(Box(0.0, 0.0, SIZE, 149.0), Box(0.0, 151.0, SIZE, SIZE))
        val none = place(one(fig("A.0", 20.0, 150.0, 320.0, 150.0, DimLayout.Rank.UNMEASURED), fig("A.1", 20.0, 150.0, 320.0, 150.0, DimLayout.Rank.MEASURED, measured = true)),
            OldPlace.Scope.ALL, wallOff)
        // the first on its line at its middle; the second BESIDE it on the same line, the first spot clear of it (a
        // quarter dot at a time, towards the line's start), never on it: each dot is seen and takes its own tap
        for ((k, x) in listOf("A.0" to 170.0, "A.1" to 162.5)) {
            val d = none.of(k)!!
            assertTrue(d.dot); assertEquals(DimLayout.How.DOT, d.how)
            assertEquals(listOf(x - 3, 150.0 - 3, x + 3, 150.0 + 3), l(d.box))
            assertEquals(listOf(x - 16, 150.0 - 16, x + 16, 150.0 + 16), l(d.tap))
            assertNull(d.leader); assertNotNull(d.line); assertFalse(d.off)
        }
        assertFalse(hit(none.of("A.0")!!.box, none.of("A.1")!!.box))
        // hollow not measured, solid measured
        assertFalse(none.of("A.0")!!.measured); assertTrue(none.of("A.1")!!.measured)
        assertEquals(2, none.dots)
    }

    @Test fun `the active dimension is placed first and never shrinks`() {
        // listed LAST, and wanting the very place the picked mark's own figure wants: the active one gets it
        val figs = listOf(fig("A.w", 20.0, 150.0, 320.0, 150.0, DimLayout.Rank.MEASURED), fig("A.h", 20.0, 152.0, 320.0, 152.0, DimLayout.Rank.PICKED),
            fig("A.l", 20.0, 154.0, 320.0, 154.0, DimLayout.Rank.ACTIVE))
        val out = place(one(*figs.toTypedArray()), OldPlace.Scope.ALL, emptyList())
        fun cx(k: String) = out.of(k)!!.box.let { (it.l + it.r) / 2 }
        assertEquals(170.0, cx("A.l")); assertEquals(95.0, cx("A.h")); assertEquals(245.0, cx("A.w"))
        // the order in the answer is the order given, whatever order they were placed in
        assertEquals(listOf("A.w", "A.h", "A.l"), out.plates.map { it.code })
        // with no room at all the active figure is still a plate, at its first place; the other shrinks
        val full = listOf(Box(0.0, 0.0, SIZE, SIZE))
        val forced = place(one(fig("A.w", 20.0, 150.0, 320.0, 150.0, DimLayout.Rank.UNMEASURED), fig("A.l", 20.0, 154.0, 320.0, 154.0, DimLayout.Rank.ACTIVE)), OldPlace.Scope.ALL, full)
        assertEquals(DimLayout.How.ON_LINE, forced.of("A.l")!!.how); assertFalse(forced.of("A.l")!!.dot)
        assertEquals(listOf(150.0, 154.0 - 8.5, 190.0, 154.0 + 8.5), l(forced.of("A.l")!!.box))
        assertTrue(forced.of("A.w")!!.dot)
        // ... and an active p, and the active half of a compact plate, likewise
        val chip = DimLayout.Fig("A.p", MarkDims.Role.DEPTH, null, doubleArrayOf(200.0, 200.0), 30.0, 12.0, DimLayout.Rank.ACTIVE, false)
        assertEquals(DimLayout.How.CHIP, place(one(chip), OldPlace.Scope.ALL, full).of("A.p")!!.how)
        val small = DimLayout.Mark("s", Box(100.0, 100.0, 125.0, 111.0), listOf(
            DimLayout.Fig("S.w", MarkDims.Role.WIDTH, DimLayout.Seg(100.0, 94.0, 125.0, 94.0), null, 40.0, 10.0, DimLayout.Rank.UNMEASURED, false),
            DimLayout.Fig("S.h", MarkDims.Role.HEIGHT, DimLayout.Seg(131.0, 111.0, 131.0, 100.0), null, 34.0, 10.0, DimLayout.Rank.ACTIVE, false)))
        val sc = OldPlace.place(listOf(small), OldPlace.Scope.ALL, SIZE, SIZE, full, U, DP, TIMES)
        assertEquals(listOf(DimLayout.How.COMPACT, DimLayout.How.COMPACT), sc.plates.map { it.how })
        // not active, it gives way: both of its figures become dots on their own lines
        val gave = OldPlace.place(listOf(DimLayout.Mark("s", small.box, small.figs.map { DimLayout.Fig(it.code, it.role, it.line, null, it.w, it.fs, DimLayout.Rank.UNMEASURED, false) })),
            OldPlace.Scope.ALL, SIZE, SIZE, full, U, DP, TIMES)
        assertEquals(listOf(true, true), gave.plates.map { it.dot })
        assertEquals(listOf(112.5 - 3, 94.0 - 3, 112.5 + 3, 94.0 + 3), l(gave.of("S.w")!!.box))
        assertEquals(listOf(131.0 - 3, 105.5 - 3, 131.0 + 3, 105.5 + 3), l(gave.of("S.h")!!.box))
    }

    @Test fun `a small box gets one compact plate beside it, each figure its own tap half`() {
        val out = place(marks(), OldPlace.Scope.FACE)
        val w = assertNotNull(out.of("FS1.w")); val h = assertNotNull(out.of("FS1.h"))
        assertEquals(DimLayout.How.COMPACT, w.how); assertEquals(DimLayout.How.COMPACT, h.how)
        assertFalse(w.turned); assertFalse(h.turned)                // "≈200 × ≈90", upright
        val whole = assertNotNull(w.whole); assertTrue(same(whole, assertNotNull(h.whole)))
        // 25 x 11 dp on the photo: both spans shorter than their plates and 8 dp
        val p = toPx(switch.x0, switch.y1); val q = toPx(switch.x1, switch.y0)
        assertTrue(q[0] - p[0] < w.box.w + 8 && q[1] - p[1] < h.box.w + 8)
        // right of the box, clear of the h line (6) and 4 more, centred on its height
        assertEquals(q[0] + 10, whole.l, 1e-9); assertEquals((p[1] + q[1]) / 2, (whole.t + whole.b) / 2, 1e-9)
        val ww = DimLayout.plateW(textW("≈200", 10.0, false), DP); val hw = DimLayout.plateW(textW("≈90", 10.0, false), DP)
        assertEquals(ww + TIMES + hw, whole.w, 1e-9); assertEquals(17.0, whole.h, 1e-9)
        // the two halves: the width's on the left, the height's on the right, the × between them
        assertEquals(listOf(whole.l, whole.t, whole.l + ww, whole.b), l(w.box)); assertEquals(listOf(whole.r - hw, whole.t, whole.r, whole.b), l(h.box))
        // each its own tap half: they meet in the middle of the × and do not overlap, 32 dp at least each way
        assertEquals(w.tap.r, h.tap.l, 1e-9); assertEquals(whole.l + ww + TIMES / 2, w.tap.r, 1e-9)
        assertFalse(hit(w.tap, h.tap))
        for (t in listOf(w.tap, h.tap)) assertTrue(t.w >= 32 && t.h >= 32, "$t")
        // a larger box keeps its two plates on its two lines
        assertEquals(DimLayout.How.ON_LINE, out.of("FD1.w")!!.how); assertNull(out.of("FD1.w")!!.whole)
        // one of the two spans long enough: no compact plate
        val wide = mk("S", 2, 400.0, 1100.0, 1500.0, 1590.0)
        val o2 = place(marks(els = listOf(wide)), OldPlace.Scope.FACE, keep(listOf(wide)))
        assertEquals(DimLayout.How.ON_LINE, o2.of("FS2.w")!!.how); assertEquals(DimLayout.How.OFF_LINE, o2.of("FS2.h")!!.how)
        assertTrue(o2.of("FS2.h")!!.turned); assertTrue(o2.of("FS2.h")!!.arrowsIn)
        // ... the other way round too -- narrow and tall: its w steps off its line, its h sits on its own
        val tall = mk("X", 1, 600.0, 750.0, 300.0, 1800.0)
        val o3 = place(marks(els = listOf(tall)), OldPlace.Scope.FACE, keep(listOf(tall)))
        assertEquals(DimLayout.How.OFF_LINE, o3.of("FX1.w")!!.how); assertTrue(o3.of("FX1.w")!!.arrowsIn); assertNull(o3.of("FX1.w")!!.whole)
        assertEquals(DimLayout.How.ON_LINE, o3.of("FX1.h")!!.how); assertTrue(o3.of("FX1.h")!!.turned); assertFalse(o3.of("FX1.h")!!.arrowsIn)
    }

    @Test fun `a p is a chip at the mark's lower-right corner`() {
        // a niche alone in the middle of the wall, picked: w h l s and its depth into the wall
        val niche = mk("N", 1, 800.0, 1700.0, 600.0, 1800.0)
        val ms = marks(picked = niche.id, els = listOf(niche))
        val out = place(ms, OldPlace.Scope.FACE, keep(listOf(niche)))
        sound(out, keep(listOf(niche)), what = "a niche")
        assertEquals(listOf("FN1.w", "FN1.h", "FN1.l", "FN1.s", "FN1.p"), out.plates.map { it.code })
        val c = assertNotNull(out.of("FN1.p"))
        assertEquals(DimLayout.How.CHIP, c.how); assertNull(c.line); assertNull(c.leader); assertFalse(c.turned); assertFalse(c.arrowsIn)
        val q = toPx(niche.x1, niche.y0)
        // outside the corner: 4 right of it and 4 below, "⊥ ?" wide and one plate high
        assertEquals(listOf(q[0] + 4, q[1] + 4), listOf(c.box.l, c.box.t))
        assertEquals(DimLayout.plateW(textW("⊥ ?", 12.0, false), DP), c.box.w, 1e-9); assertEquals(19.0, c.box.h, 1e-9)
        // with the corner's outside taken it goes inside the corner; with that taken too, below it; then right of it
        fun chip(vararg taken: Box) = place(ms, OldPlace.Scope.FACE, keep(listOf(niche)) + taken).of("FN1.p")!!
        val below = Box(q[0] + 1, q[1] + 1, q[0] + 60, q[1] + 40); val inside = Box(q[0] - 30, q[1] - 20, q[0] - 1, q[1] - 1)
        val under = Box(q[0] - 30, q[1] + 1, q[0] - 1, q[1] + 40)
        assertEquals(listOf(q[0] - 4, q[1] - 4), chip(below).box.let { listOf(it.r, it.b) })
        assertEquals(listOf(q[0] - 4, q[1] + 4), chip(below, inside).box.let { listOf(it.r, it.t) })
        assertEquals(listOf(q[0] + 4, q[1] - 4), chip(below, inside, under).box.let { listOf(it.l, it.b) })
        for (ch in listOf(chip(below), chip(below, inside), chip(below, inside, under))) assertEquals(DimLayout.How.CHIP, ch.how)
        // no room at any of the four: a dot -- at the mark's lower-LEFT corner, since a kept box stands on the lower-right
        val d = chip(Box(q[0] - 60, q[1] - 40, q[0] + 60, q[1] + 40))
        val p = toPx(niche.x0, niche.y1)
        assertTrue(d.dot); assertEquals(listOf(p[0] - 3, q[1] - 3, p[0] + 3, q[1] + 3), l(d.box))
        // ... and with nothing clear anywhere, at the corner the chip is anchored at
        val d0 = chip(Box(0.0, 0.0, SIZE, SIZE))
        assertTrue(d0.dot); assertEquals(listOf(q[0] - 3, q[1] - 3, q[0] + 3, q[1] + 3), l(d0.box))
        // the door stands on the floor at the photo's bottom edge: its chip stays inside the photo
        val dc = place(marks(picked = door.id), OldPlace.Scope.FACE).of("FD1.p")!!
        assertEquals(DimLayout.How.CHIP, dc.how); assertEquals(SIZE - 2, dc.box.b, 1e-9)
    }

    // ------------------------------------------------------------------ the example wall

    @Test fun `as step 3 Measure opens the example wall shows 8 figures, none a dot`() {
        // the door picked: its four; the window's and the switch plate's size; the three points their spot alone
        val out = place(marks(picked = door.id, active = "FD1.w"), OldPlace.Scope.FACE)
        sound(out, keep(), what = "FACE, door picked")
        assertEquals(listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p", "FW1.w", "FW1.h", "FS1.w", "FS1.h"), out.plates.map { it.code })
        assertEquals(0, out.dots)
        assertEquals(listOf(DimLayout.How.ON_LINE, DimLayout.How.ON_LINE, DimLayout.How.OFF_LINE, DimLayout.How.CHIP, DimLayout.How.ON_LINE, DimLayout.How.ON_LINE,
            DimLayout.How.COMPACT, DimLayout.How.COMPACT), out.plates.map { it.how })
        // nothing picked: 6 -- every box mark its size, no position, no point
        val none = place(marks(), OldPlace.Scope.FACE)
        sound(none, keep(), what = "FACE, nothing picked")
        assertEquals(listOf("FD1.w", "FD1.h", "FW1.w", "FW1.h", "FS1.w", "FS1.h"), none.plates.map { it.code }); assertEquals(0, none.dots)
        // a point picked: its two lines to its centre join the six
        val pt = place(marks(picked = points[1].id), OldPlace.Scope.FACE)
        sound(pt, keep(), what = "FACE, a point picked")
        assertEquals(8, pt.plates.size); assertEquals(0, pt.dots)
        assertEquals(setOf("FE2.cr", "FE2.cs"), pt.plates.map { it.code }.filter { it.startsWith("FE") }.toSet())
        // whichever mark is picked, and whichever of its dimensions is active: 6 to 9 plates, sound, and no dot
        for (e in wall) for (d in listOf<String?>(null) + MarkDims.of("front", e, W, H).map { it.code }) {
            val o = place(marks(picked = e.id, active = d), OldPlace.Scope.FACE)
            sound(o, keep(), what = "FACE, ${e.code} picked, $d active")
            assertTrue(o.plates.size in 6..9, "${e.code}: ${o.plates.size} plates"); assertEquals(0, o.dots, "${e.code} picked, $d active")
        }
    }

    @Test fun `with all on the example wall places all 19, none on another`() {
        val out = place(marks(), OldPlace.Scope.ALL)
        sound(out, keep(), what = "ALL")
        assertEquals(19, out.plates.size)
        assertEquals(wall.flatMap { e -> MarkDims.of("front", e, W, H).map { it.code } }, out.plates.map { it.code })
        // at fit figures may give way to a dot: how many is pinned, so a change of the rule shows here -- on this round
        // canvas; the phone's own count is pinned in `the example wall at the phone's own numbers`
        assertEquals(ALL_DOTS, out.dots, out.plates.filter { it.dot }.joinToString { it.code })
        // with a mark picked and a dimension active, still all 19 and still sound -- and the active one is never the dot
        for (e in wall) for (d in MarkDims.of("front", e, W, H).map { it.code }) {
            val o = place(marks(picked = e.id, active = d), OldPlace.Scope.ALL)
            sound(o, keep(), setOf(d), "ALL, ${e.code} picked, $d active")
            assertEquals(19, o.plates.size); assertFalse(o.of(d)!!.dot, d)
        }
        // readings change the texts and the ranks, never the soundness
        val some = mapOf("FD1.w" to 905.0, "FW1.h" to 1198.0, "FE2.cs" to 300.0, "FS1.w" to 200.0, "FW1.p" to 230.0)
        sound(place(marks(actual = some), OldPlace.Scope.ALL), keep(), what = "ALL, some measured")
    }

    @Test fun `the same input gives the same output`() {
        fun dump(o: DimLayout.Out) = o.plates.map { listOf(it.code, it.how, it.turned, it.arrowsIn, l(it.box), l(it.tap), it.whole?.let(::l), it.leader?.toString()) }
        for (scope in OldPlace.Scope.values()) {
            val a = dump(place(marks(picked = window.id, active = "FW1.s"), scope))
            repeat(3) { assertEquals(a, dump(place(marks(picked = window.id, active = "FW1.s"), scope)), scope.name) }
        }
    }

    // ------------------------------------------------------------------ 2 · Mark: the picked mark keeps its tabs clear

    @Test fun `in step 2 Mark a picked door's w and h stand where widthTag and heightTag put them, clear of its eight tabs`() {
        // the wall without its switch plate, whose code stands just where the door's h tag goes (the last lines here)
        val els = listOf(door, window) + points
        val ms = marks(picked = door.id, active = "FD1.w", tabs = true, els = els)
        val out = place(ms, OldPlace.Scope.PICKED, keep(els))
        // only the picked mark shows figures in 2 · Mark, as today
        assertEquals(listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p"), out.plates.map { it.code })
        val b = ms[0].box; val fs = 12.0
        val w = out.of("FD1.w")!!; val h = out.of("FD1.h")!!
        assertEquals(DimLayout.How.HAND, w.how); assertEquals(DimLayout.How.HAND, h.how); assertFalse(h.turned)
        val wa = SurveyPrepTouch.tagShape(SurveyPrepTouch.widthTag(b.l, b.t, b.r, fs, U, DP, true), ms[0].figs[0].w, fs, U, SIZE, SIZE).drawn
        val ha = SurveyPrepTouch.tagShape(SurveyPrepTouch.heightTag(b.t, b.r, b.b, fs, U, DP, true), ms[0].figs[1].w, fs, U, SIZE, SIZE).drawn
        assertEquals(l(wa), l(w.box)); assertEquals(l(ha), l(h.box))
        // ... which is higher than the line's own middle, and right of the right edge's tabs
        assertTrue(w.box.b < b.t - 5 && h.box.l > b.r + 5)
        val tabs = DimLayout.tabBoxes(b, U)
        assertEquals(8, tabs.size)
        // the four corners and the four middles, 5 either side of each
        for (sq in listOf(listOf(b.l - 5, b.t - 5, b.l + 5, b.t + 5), listOf(b.r - 5, b.b - 5, b.r + 5, b.b + 5),
            listOf((b.l + b.r) / 2 - 5, b.t - 5, (b.l + b.r) / 2 + 5, b.t + 5), listOf(b.r - 5, (b.t + b.b) / 2 - 5, b.r + 5, (b.t + b.b) / 2 + 5)))
            assertTrue(sq in tabs.map(::l), "a tab square $sq")
        // no plate of the picked mark covers one of its eight tabs
        for (p in out.plates) for (t in tabs) assertFalse(hit(p.box, t), "${p.code} ${p.box} covers a tab $t")
        sound(out, keep(els) + tabs, what = "PICKED, door")
        // its p's chip stands off the corner by the same clearance, so the corner's tab stays free
        val chip = out.of("FD1.p")!!
        assertEquals(DimLayout.How.CHIP, chip.how); assertEquals(b.r + SurveyPrepTouch.clearance(U, DP), chip.box.l, 1e-9)
        // the same door in 3 · Measure shows no tabs: its plates are on its lines, mid-span
        val face = place(marks(picked = door.id, active = "FD1.w", els = els), OldPlace.Scope.FACE, keep(els))
        assertEquals(DimLayout.How.ON_LINE, face.of("FD1.w")!!.how); assertTrue(face.of("FD1.h")!!.turned)
        // ... and tabs asked for outside 2 · Mark move nothing: only PICKED anchors by hand
        assertEquals(DimLayout.How.ON_LINE, place(ms, OldPlace.Scope.FACE, keep(els)).of("FD1.w")!!.how)
        // on the whole example wall the switch plate's code stands where the h tag goes: the tag gives way as any
        // plate does -- upright, a quarter along its line -- and still covers no tab and no code
        val full = marks(picked = door.id, active = "FD1.w", tabs = true)
        val fo = place(full, OldPlace.Scope.PICKED)
        assertEquals(DimLayout.How.HAND, fo.of("FD1.w")!!.how)
        assertEquals(DimLayout.How.ON_LINE, fo.of("FD1.h")!!.how); assertFalse(fo.of("FD1.h")!!.turned)
        for (p in fo.plates) for (t in tabs) assertFalse(hit(p.box, t), "${p.code} ${p.box} covers a tab $t")
        sound(fo, keep() + tabs, what = "PICKED, door, the whole wall")
    }

    @Test fun `a locked mark and a point show no tabs, so their plates do not move`() {
        // a locked door: picked in 2 · Mark, no tabs -- its plates sit on its lines as they do in 3 · Measure
        val locked = mk("D", 1, 150.0, 1050.0, 0.0, 2100.0, mapOf(SurveyPrep.LOCKED to true))
        val els = listOf(locked, window, switch) + points
        val ms = marks(picked = locked.id, tabs = true, els = els)
        assertFalse(ms[0].tabs)
        val out = place(ms, OldPlace.Scope.PICKED)
        sound(out, keep(), what = "PICKED, locked door")
        val face = place(marks(picked = locked.id, els = els), OldPlace.Scope.FACE)
        for (k in listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p")) {
            assertEquals(l(face.of(k)!!.box), l(out.of(k)!!.box), k); assertEquals(face.of(k)!!.how, out.of(k)!!.how, k)
        }
        assertEquals(DimLayout.How.ON_LINE, out.of("FD1.w")!!.how); assertTrue(out.of("FD1.h")!!.turned)
        // a picked point: its two figures on its two lines, in either step
        val pm = marks(picked = points[0].id, tabs = true)
        assertFalse(pm[3].tabs)
        val po = place(pm, OldPlace.Scope.PICKED)
        assertEquals(listOf("FE1.cr", "FE1.cs"), po.plates.map { it.code })
        val pf = place(marks(picked = points[0].id), OldPlace.Scope.FACE)
        for (k in listOf("FE1.cr", "FE1.cs")) { assertEquals(l(pf.of(k)!!.box), l(po.of(k)!!.box), k); assertFalse(po.of(k)!!.how == DimLayout.How.HAND) }
        sound(po, keep(), what = "PICKED, a point")
    }

    @Test fun `a tap box is the plate and 6 units each way, and never under 32 dp`() {
        // 40 x 19: 6 each way is 52 x 31, and 31 is under 32 -- so 32 high, centred on the plate
        val t = DimLayout.tapOf(Box(100.0, 90.5, 140.0, 109.5), U, DP)
        assertEquals(listOf(94.0, 84.0, 146.0, 116.0), l(t))
        // a 6 dp dot: 32 square round it; at 2 px a dp, 64 px
        assertEquals(listOf(84.0, 84.0, 116.0, 116.0), l(DimLayout.tapOf(Box(97.0, 97.0, 103.0, 103.0), U, DP)))
        assertEquals(listOf(68.0, 68.0, 132.0, 132.0), l(DimLayout.tapOf(Box(94.0, 94.0, 106.0, 106.0), 2.0, 2.0)))
        // a wide plate keeps its 6 units
        assertEquals(listOf(94.0, 84.0, 206.0, 116.0), l(DimLayout.tapOf(Box(100.0, 90.5, 200.0, 109.5), U, DP)))
    }

    // ------------------------------------------------------------------ the review of M1 (16 Nov 2026)

    @Test fun `the example wall at the phone's own numbers`() {
        // 680 px at 2 px a dp, the unit 680 / 480 px: a figure is 12 dp on the picked mark and 10 dp on every other --
        // 24 and 20 px -- whatever the unit is; its plate 7 dp higher and 10 dp wider than its text
        assertEquals(2.0, phone.dp); assertEquals(680.0 / 480, phone.u); assertEquals(0.71, phone.u / phone.dp, 0.005)
        val out = phone.place(phone.marks(picked = door.id, active = "FD1.w"), OldPlace.Scope.FACE)
        phone.sound(out, phone.keep(), what = "phone FACE, door picked")
        assertEquals(listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p", "FW1.w", "FW1.h", "FS1.w", "FS1.h"), out.plates.map { it.code }); assertEquals(0, out.dots)
        assertEquals(listOf(24.0, 24.0, 24.0, 24.0, 20.0, 20.0, 20.0, 20.0), out.plates.map { it.fig.fs })
        val dw = out.of("FD1.w")!!; val ww = out.of("FW1.w")!!
        assertEquals(38.0, dw.box.h, 1e-9); assertEquals(phone.textW("▶≈900", 24.0, false) + 20, dw.box.w, 1e-9)
        assertEquals(34.0, ww.box.h, 1e-9); assertEquals(phone.textW("≈1200", 20.0, false) + 20, ww.box.w, 1e-9)
        // A PLATE KEEPS ITS SIZE ON SCREEN: the same dp on the round canvas, and on a canvas half as large at the same
        // density (landscape: the unit shrinks with the canvas, a plate does not)
        val round = place(marks(picked = door.id, active = "FD1.w"), OldPlace.Scope.FACE)
        val small = Rig(340.0, 340.0 / 480, 2.0)
        val land = small.place(small.marks(picked = door.id, active = "FD1.w", els = listOf(door)), OldPlace.Scope.FACE, emptyList())
        for (k in listOf("FD1.w", "FD1.l", "FD1.p")) {
            assertEquals(round.of(k)!!.box.w, out.of(k)!!.box.w / phone.dp, 1e-9, k); assertEquals(round.of(k)!!.box.h, out.of(k)!!.box.h / phone.dp, 1e-9, k)
            assertEquals(out.of(k)!!.box.w, land.of(k)!!.box.w, 1e-9, k); assertEquals(out.of(k)!!.box.h, land.of(k)!!.box.h, 1e-9, k)
        }
        // the text sits 5 dp inside its plate and 5 dp above its bottom
        assertEquals(dw.box.l + 10, DimLayout.textLeft(dw.box, phone.dp), 1e-9); assertEquals(dw.box.b - 10, DimLayout.baseline(dw.box, phone.dp), 1e-9)
        // THE LINES ARE IN UNITS, where build 332 draws them, and do not move when the mark is picked: the window's
        // corner line 2.6 x 12 units under its mid-height, its floor line 1.6 x 12 units left of its centre
        val p = phone.toPx(window.x0, window.y1); val q = phone.toPx(window.x1, window.y0)
        for (picked in listOf<String?>(null, window.id)) {
            val figs = phone.marks(picked = picked)[1].figs.associateBy { it.code }
            assertEquals((p[1] + q[1]) / 2 + 2.6 * 12 * phone.u, figs.getValue("FW1.r").line!!.y1, 1e-9)
            assertEquals((p[0] + q[0]) / 2 - 1.6 * 12 * phone.u, figs.getValue("FW1.s").line!!.x1, 1e-9)
            assertEquals(p[1] - 6 * phone.u, figs.getValue("FW1.w").line!!.y1, 1e-9)
        }
        // as 3 · Measure opens: whichever mark is picked, and whichever of its dimensions is active, 6 to 9 plates,
        // sound, and no dot -- at the phone's own numbers
        assertEquals(6, phone.place(phone.marks(), OldPlace.Scope.FACE).plates.size)
        for (e in wall) for (d in listOf<String?>(null) + MarkDims.of("front", e, W, H).map { it.code }) {
            val o = phone.place(phone.marks(picked = e.id, active = d), OldPlace.Scope.FACE)
            phone.sound(o, phone.keep(), what = "phone FACE, ${e.code} picked, $d active")
            assertTrue(o.plates.size in 6..9, "${e.code}: ${o.plates.size} plates"); assertEquals(0, o.dots, "${e.code} picked, $d active")
        }
        // with all on: all 19, the dots pinned, and with a mark picked and a dimension active still sound -- no dot
        // under a plate -- and the active one never the dot
        val all = phone.place(phone.marks(), OldPlace.Scope.ALL)
        phone.sound(all, phone.keep(), what = "phone ALL")
        assertEquals(19, all.plates.size); assertEquals(PHONE_ALL_DOTS, all.dots, all.plates.filter { it.dot }.joinToString { it.code })
        for (e in wall) for (d in MarkDims.of("front", e, W, H).map { it.code }) {
            val o = phone.place(phone.marks(picked = e.id, active = d), OldPlace.Scope.ALL)
            phone.sound(o, phone.keep(), setOf(d), "phone ALL, ${e.code} picked, $d active")
            assertEquals(19, o.plates.size); assertFalse(o.of(d)!!.dot, d)
        }
        // 2 · Mark at the phone's numbers: the picked door's w and h tags clear of its eight tabs, by the tags' own clearance
        val els = listOf(door, window) + points
        val ms = phone.marks(picked = door.id, active = "FD1.w", tabs = true, els = els)
        val hand = phone.place(ms, OldPlace.Scope.PICKED, phone.keep(els))
        val b = ms[0].box; val tabs = DimLayout.tabBoxes(b, phone.u)
        assertEquals(DimLayout.How.HAND, hand.of("FD1.w")!!.how); assertEquals(DimLayout.How.HAND, hand.of("FD1.h")!!.how)
        // the w tag's text sits 0.8 of its size and the clearance over the top edge; the h tag starts the clearance right of its line
        val gap = SurveyPrepTouch.clearance(phone.u, phone.dp)
        assertEquals(b.t - 24 * 0.8 - gap, DimLayout.baseline(hand.of("FD1.w")!!.box, phone.dp), 1e-9)
        assertEquals(b.r + 6 * phone.u + gap, hand.of("FD1.h")!!.box.l, 1e-9); assertEquals(38.0, hand.of("FD1.h")!!.box.h, 1e-9)
        for (pl in hand.plates) for (t in tabs) assertFalse(hit(pl.box, t), "${pl.code} ${pl.box} covers a tab $t")
        phone.sound(hand, phone.keep(els) + tabs, what = "phone PICKED, door")
    }

    @Test fun `a mark off the canvas has no plate, no dot and no tap box`() {
        for (r in listOf(fit, phone)) {
            // zoomed 2.8 times onto the door's upper half: the canvas shows the face's x 122 to 1078 and y 1172 to 2128 mm
            // -- the door's top, left and right edges, and nothing of the window, the switch plate or the three points
            val z = r.at(2.8, 600.0, 1650.0)
            for (scope in listOf(OldPlace.Scope.FACE, OldPlace.Scope.ALL)) {
                val out = z.place(z.marks(), scope)
                z.sound(out, z.keep(), what = "zoomed, $scope")
                // the door's size alone: its l runs under the canvas and its p's corner is on the floor, off it too
                assertEquals(listOf("FD1.w", "FD1.h"), out.plates.map { it.code }, "$scope"); assertEquals(0, out.dots)
                assertEquals(listOf("FD1.w", "FD1.h"), out.tags.map { it.code })
                assertTrue(out.plates.none { it.off }); assertFalse(out.activeOff); assertNull(out.active)
            }
            // THE PICKED MARK'S FIGURES STAY, slid into view as its tags are today, and say that they were
            val picked = z.place(z.marks(picked = door.id, active = "FD1.w"), OldPlace.Scope.FACE)
            z.sound(picked, z.keep(), what = "zoomed, door picked")
            assertEquals(listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p"), picked.plates.map { it.code })
            assertEquals(listOf(false, false, true, true), picked.plates.map { it.off })
            // the active figure is on the canvas: nothing to pan to
            assertEquals("FD1.w", picked.active!!.code); assertFalse(picked.activeOff)
            // ... the active one off the canvas: still a plate, inside the canvas, and the screen is told to pan
            val under = z.place(z.marks(picked = door.id, active = "FD1.l"), OldPlace.Scope.FACE)
            assertEquals("FD1.l", under.active!!.code); assertTrue(under.activeOff); assertFalse(under.active!!.dot)
            z.sound(under, z.keep(), what = "zoomed, the active figure off the canvas")
            // a picked mark that is wholly off the canvas keeps its own figures; no OTHER off-canvas mark gains one
            for (scope in listOf(OldPlace.Scope.FACE, OldPlace.Scope.ALL)) {
                val far = z.place(z.marks(picked = window.id, active = "FW1.s"), scope)
                z.sound(far, z.keep(), setOf("FW1.s"), "zoomed, the window picked, $scope")
                assertEquals(listOf("FD1.w", "FD1.h", "FW1.w", "FW1.h", "FW1.r", "FW1.s", "FW1.p"), far.plates.map { it.code }, "$scope")
                assertEquals(listOf(false, false, true, true, true, true, true), far.plates.map { it.off })
                assertTrue(far.activeOff)
            }
            // at fit nothing is off the canvas, and nothing is left out
            val whole = r.place(r.marks(picked = door.id, active = "FD1.l"), OldPlace.Scope.ALL)
            assertEquals(19, whole.plates.size); assertTrue(whole.plates.none { it.off }); assertFalse(whole.activeOff)
            // the review's own view -- 3 times, on the middle of the door, whose four edges are then off the canvas too:
            // with all on the one figure left is the switch plate's line from the left corner, which crosses the canvas
            val in3 = r.at(3.0, 600.0, 1050.0)
            val o3 = in3.place(in3.marks(), OldPlace.Scope.ALL)
            assertEquals(listOf("FS1.l"), o3.plates.map { it.code }); assertEquals(0, o3.dots)
            assertEquals(0, in3.place(in3.marks(), OldPlace.Scope.FACE).plates.size)
        }
    }

    @Test fun `zoom spreads the plates -- with all on there are no more dots zoomed than at fit`() {
        for (r in listOf(fit, phone)) {
            val atFit = r.place(r.marks(), OldPlace.Scope.ALL).dots
            for (zoom in listOf(1.5, 2.0, 3.0)) for (cx in listOf(450.0, 900.0, 1340.0, 1800.0, 2230.0)) for (cy in listOf(450.0, 900.0, 1340.0, 1800.0, 2230.0)) {
                val z = r.at(zoom, cx, cy)
                val out = z.place(z.marks(), OldPlace.Scope.ALL)
                z.sound(out, z.keep(), what = "ALL at $zoom on $cx, $cy")
                assertTrue(out.dots <= atFit, "ALL at $zoom on $cx, $cy: ${out.dots} dots, $atFit at fit")
                // nothing placed is off the canvas: every figure's own span crosses it
                for (p in out.plates) { assertFalse(p.off, p.code); assertTrue(DimLayout.onCanvas(p.fig, z.size, z.size), p.code) }
                // ... and everything whose span crosses the canvas is placed
                assertEquals(z.marks().flatMap { m -> m.figs.filter { DimLayout.onCanvas(it, z.size, z.size) }.map { it.code } }, out.plates.map { it.code })
            }
            // zoomed twice onto the three points, where the two dots are at fit: every figure there is a plate
            val z = r.at(2.0, 2008.0, 669.0)
            assertEquals(0, z.place(z.marks(), OldPlace.Scope.ALL).dots)
        }
    }

    @Test fun `a dot is never slid onto the canvas from outside it, and sits on the part of its line the canvas shows`() {
        val full = listOf(Box(0.0, 0.0, SIZE, SIZE))
        // the window picked, wholly off the canvas, and no room anywhere: its active figure is still a plate inside the
        // canvas; the others are dots where their lines are -- off the canvas, unseen -- not stacked in its corner
        val z = fit.at(2.8, 600.0, 1650.0)
        val out = z.place(z.marks(picked = window.id, active = "FW1.s"), OldPlace.Scope.FACE, full)
        val view = Box(0.0, 0.0, SIZE, SIZE)
        assertFalse(out.of("FW1.s")!!.dot); assertTrue(out.of("FW1.s")!!.off); assertTrue(out.activeOff)
        for (k in listOf("FW1.w", "FW1.h", "FW1.r")) {
            val d = out.of(k)!!; val mid = d.line!!.at(0.5)
            assertTrue(d.dot && d.off, k); assertFalse(hit(d.box, view), "$k's dot ${d.box} was slid onto the canvas")
            assertEquals(listOf(mid[0] - 3, mid[1] - 3, mid[0] + 3, mid[1] + 3), l(d.box), k)
        }
        // its p likewise: at the corner it is anchored at
        val c = out.of("FW1.p")!!; val q = z.toPx(window.x1, window.y0)
        assertTrue(c.dot && c.off); assertEquals(listOf(q[0] - 3, q[1] - 3, q[0] + 3, q[1] + 3), l(c.box))
        // a line that runs off the canvas's left edge: its dot is at the middle of the 100 px the canvas shows, not at
        // the line's own middle slid in to the edge
        val part = place(one(fig("A.l", -200.0, 150.0, 100.0, 150.0, DimLayout.Rank.UNMEASURED)), OldPlace.Scope.ALL, full).of("A.l")!!
        assertTrue(part.dot); assertFalse(part.off); assertEquals(listOf(50.0 - 3, 150.0 - 3, 50.0 + 3, 150.0 + 3), l(part.box))
        // a line along the canvas's very edge: half a dot inside it, no further
        val edge = place(one(fig("A.l", 20.0, 0.0, 320.0, 0.0, DimLayout.Rank.UNMEASURED)), OldPlace.Scope.ALL, full).of("A.l")!!
        assertEquals(listOf(170.0 - 3, 0.0, 170.0 + 3, 6.0), l(edge.box))
        // the canvas's own edges count as the canvas; a hair outside does not
        assertNotNull(DimLayout.clip(DimLayout.Seg(SIZE, 10.0, SIZE + 50, 10.0), SIZE, SIZE)); assertNull(DimLayout.clip(DimLayout.Seg(SIZE + 0.01, 10.0, SIZE + 50, 10.0), SIZE, SIZE))
        assertEquals(listOf(0.0, 150.0, 100.0, 150.0), DimLayout.clip(DimLayout.Seg(-200.0, 150.0, 100.0, 150.0), SIZE, SIZE)!!.let { listOf(it.x1, it.y1, it.x2, it.y2) })
        assertEquals(listOf(40.0, 0.0, 340.0, 300.0), DimLayout.clip(DimLayout.Seg(-60.0, -100.0, 440.0, 400.0), SIZE, SIZE)!!.let { listOf(it.x1, it.y1, it.x2, it.y2) })
        // ... and a slanted line past a corner is off it, though the box round it is not
        assertNull(DimLayout.clip(DimLayout.Seg(-50.0, 20.0, 20.0, -50.0), SIZE, SIZE))
    }

    @Test fun `a dot takes a clear spot on its line and keeps it -- under no plate, on no other dot`() {
        // the review's case: with all on at fit the corner point's floor line runs under another point's plate. Its dot
        // is on its own line (x), clear of every plate, and a plate placed AFTER a dot keeps off it
        val out = place(marks(), OldPlace.Scope.ALL)
        val d = out.of("FE3.cs")!!; val ln = d.line!!
        assertTrue(d.dot); assertEquals(ln.x1, (d.box.l + d.box.r) / 2, 1e-9)
        assertTrue(d.box.b <= maxOf(ln.y1, ln.y2) + 1e-9 && d.box.t >= minOf(ln.y1, ln.y2) - 3 - 1e-9)
        for (b in solid(out)) assertFalse(hit(d.box, b), "FE3.cs's dot ${d.box} lies under $b")
        // the middle of the line taken by a plate: the dot steps along the line to the first spot clear of it by a unit
        val taken = listOf(Box(0.0, 0.0, SIZE, 149.0), Box(0.0, 151.0, SIZE, SIZE))
        val two = place(one(fig("A.0", 100.0, 150.0, 240.0, 150.0, DimLayout.Rank.ACTIVE), fig("A.1", 20.0, 150.0, 320.0, 150.0, DimLayout.Rank.UNMEASURED)), OldPlace.Scope.ALL, taken)
        assertFalse(two.of("A.0")!!.dot); assertEquals(listOf(150.0, 141.5, 190.0, 158.5), l(two.of("A.0")!!.box))
        val beside = two.of("A.1")!!
        assertTrue(beside.dot); assertEquals(listOf(143.0, 147.0, 149.0, 153.0), l(beside.box))      // a unit clear of the plate's left edge at 150
        // TWO POINTS AT ONE HEIGHT share their corner line: the first one's plate lies along ALL the second's line shows.
        // Its dot stands beside the line, just clear of that plate -- not under it
        val along = place(one(fig("A.0", 250.0, 150.0, 340.0, 150.0, DimLayout.Rank.ACTIVE), fig("A.1", 300.0, 150.0, 340.0, 150.0, DimLayout.Rank.UNMEASURED)), OldPlace.Scope.ALL, taken)
        assertEquals(listOf(275.0, 141.5, 315.0, 158.5), l(along.of("A.0")!!.box))
        val off = along.of("A.1")!!
        assertTrue(off.dot); assertEquals(listOf(317.0, 147.0, 323.0, 153.0), l(off.box))             // on its line after all: right of the plate
        val under = place(one(fig("A.0", 250.0, 150.0, 340.0, 150.0, DimLayout.Rank.ACTIVE), fig("A.1", 280.0, 150.0, 310.0, 150.0, DimLayout.Rank.UNMEASURED)), OldPlace.Scope.ALL, taken)
        val by = under.of("A.1")!!
        assertTrue(by.dot); assertEquals(listOf(292.0, 133.5, 298.0, 139.5), l(by.box))               // above the plate, a unit clear of its top at 141.5
        assertFalse(hit(by.box, under.of("A.0")!!.box))
        // a dot is placed BEFORE a lower-ranked plate that wants its spot: the plate goes elsewhere
        val first = DimLayout.Mark("a", Box(60.0, 60.0, 100.0, 100.0), listOf(DimLayout.Fig("P.p", MarkDims.Role.DEPTH, null, doubleArrayOf(100.0, 100.0), 30.0, 10.0, DimLayout.Rank.UNMEASURED, false)))
        val later = DimLayout.Mark("b", Box(0.0, 0.0, 10.0, 10.0), listOf(fig("B.l", 0.0, 100.0, 200.0, 100.0, DimLayout.Rank.MEASURED, w = 20.0, measured = true)))
        // the chip's four places, each taken: 4 off the corner, 30 wide and 17 high
        val round = listOf(Box(104.0, 104.0, 134.0, 121.0), Box(66.0, 79.0, 96.0, 96.0), Box(66.0, 104.0, 96.0, 121.0), Box(104.0, 79.0, 134.0, 96.0))
        val o = place(listOf(first, later), OldPlace.Scope.ALL, round)
        assertTrue(o.of("P.p")!!.dot); assertEquals(listOf(97.0, 97.0, 103.0, 103.0), l(o.of("P.p")!!.box))
        // B.l's own middle is x 100, on the dot: it sits a quarter along its line instead
        assertEquals(DimLayout.How.ON_LINE, o.of("B.l")!!.how); assertEquals(listOf(40.0, 91.5, 60.0, 108.5), l(o.of("B.l")!!.box))
        sound(o, round, what = "a dot, then a plate")
    }

    @Test fun `a mark against the ceiling or the right wall keeps its figures -- its line is off the canvas, its edge is not`() {
        // a unit in the wall's top right corner: its w line runs 6 units ABOVE the canvas, its h line 6 units right of it
        val unit = mk("X", 1, W - 900, W, H - 800, H)
        val ms = marks(els = listOf(unit))
        val w = ms[0].figs.first { it.code == "FX1.w" }; val h = ms[0].figs.first { it.code == "FX1.h" }
        assertEquals(-6.0, w.line!!.y1, 1e-9); assertEquals(0.0, w.span!!.y1, 1e-9); assertEquals(SIZE + 6, h.line!!.x1, 1e-9); assertEquals(SIZE, h.span!!.x1, 1e-9)
        assertNull(DimLayout.clip(w.line!!, SIZE, SIZE)); assertNull(DimLayout.clip(h.line!!, SIZE, SIZE))
        assertTrue(DimLayout.onCanvas(w, SIZE, SIZE)); assertTrue(DimLayout.onCanvas(h, SIZE, SIZE))
        // (placed with nothing to keep clear of: the fit / grid / all column stands in this very corner)
        val out = place(ms, OldPlace.Scope.FACE, emptyList())
        sound(out, emptyList(), what = "a unit in the corner")
        assertEquals(listOf("FX1.w", "FX1.h"), out.plates.map { it.code }); assertTrue(out.plates.none { it.off || it.dot })
        // each plate slid the few units back inside the photo, still over its own edge
        assertEquals(2.0, out.of("FX1.w")!!.box.t, 1e-9); assertEquals(SIZE - 2, out.of("FX1.h")!!.box.r, 1e-9)
        // with no room its dots are on the canvas, on the unit's own edges
        val none = place(ms, OldPlace.Scope.FACE, listOf(Box(0.0, 0.0, SIZE, SIZE)))
        val mid = toPx(W - 450, H - 400)
        assertTrue(same(Box(mid[0] - 3, 0.0, mid[0] + 3, 6.0), none.of("FX1.w")!!.box), "${none.of("FX1.w")!!.box}"); assertFalse(none.of("FX1.w")!!.off)
        assertTrue(same(Box(SIZE - 6, mid[1] - 3, SIZE, mid[1] + 3), none.of("FX1.h")!!.box), "${none.of("FX1.h")!!.box}")
        // a `p`'s figure is on the canvas by its corner; a figure with neither a span nor a corner is taken as there
        assertTrue(DimLayout.onCanvas(DimLayout.Fig("A.p", MarkDims.Role.DEPTH, null, doubleArrayOf(SIZE, SIZE), 30.0, 12.0, DimLayout.Rank.UNMEASURED, false), SIZE, SIZE))
        assertFalse(DimLayout.onCanvas(DimLayout.Fig("A.p", MarkDims.Role.DEPTH, null, doubleArrayOf(SIZE + 1, 20.0), 30.0, 12.0, DimLayout.Rank.UNMEASURED, false), SIZE, SIZE))
    }

    @Test fun `the pad's live digits ride on the active plate`() {
        val stored = mapOf("FD1.w" to 1400.0)
        fun door(active: String?, live: String?) = DimLayout.mark(stored, "front", door, W, H, true, false, active, ::toPx, U, DP, ::textW, live)
        fun wide(t: String) = DimLayout.plateW(textW(t, 12.0, true), DP)
        // the pad untouched (MeasurePad.live is null while fresh): every figure is the stored one -- 1400, with its check
        val pad = MeasurePad.start(1400.0)
        assertNull(MeasurePad.live(pad))
        val still = door("FD1.w", MeasurePad.live(pad))
        assertEquals(listOf("▶1400", "≈2100", "≈150", "⊥ ?"), still.figs.map { it.text }); assertTrue(still.figs[0].check); assertTrue(still.figs.none { it.typing })
        assertEquals(wide("▶1400!"), still.figs[0].w)
        // from the first key press the digits show on the gold plate: the active glyph and what is typed, the plate
        // sized from THAT text, drawn as a reading, and no `!` -- nothing typed has been checked
        val nine = MeasurePad.press(pad, MeasurePad.Key.Digit(9))
        assertEquals("9", MeasurePad.live(nine))
        val typing = door("FD1.w", MeasurePad.live(nine))
        val f = typing.figs[0]
        assertEquals("▶9", f.text); assertTrue(f.typing); assertTrue(f.active); assertFalse(f.check)
        assertEquals(MarkDims.State.MEASURED, f.state); assertEquals(DimLayout.Rank.ACTIVE, f.rank); assertEquals(wide("▶9"), f.w)
        assertTrue(MarkDims.style(f.state, f.active).bold)
        // no other figure of the mark is touched
        assertEquals(still.figs.drop(1).map { listOf(it.text, it.w, it.state, it.check, it.typing) }, typing.figs.drop(1).map { listOf(it.text, it.w, it.state, it.check, it.typing) })
        // more digits, a wider plate; placed, the plate is as wide as its text asks
        val more = door("FD1.w", "905")
        assertEquals("▶905", more.figs[0].text); assertEquals(wide("▶905"), more.figs[0].w)
        val placed = OldPlace.place(listOf(more), OldPlace.Scope.FACE, SIZE, SIZE, emptyList(), U, DP, TIMES)
        assertEquals(wide("▶905"), placed.active!!.box.w, 1e-9); assertEquals("▶905", placed.active!!.fig.text)
        // over an ESTIMATE the digits are drawn as a reading too -- but the dimension is not measured until the commit
        val est = door("FD1.l", "64").figs[2]
        assertEquals("▶64", est.text); assertEquals(MarkDims.State.MEASURED, est.state); assertTrue(est.typing); assertFalse(est.measured)
        assertTrue(door("FD1.w", "9").figs[0].measured)
        // a `p` keeps its glyph before the digits
        assertEquals("▶⊥ 23", door("FD1.p", "23").figs[3].text)
        // AN EMPTIED PAD KEEPS THE FIGURE THE PLATE HAD until the commit: the reading, or the estimate
        val emptied = MeasurePad.press(nine, MeasurePad.Key.Back)
        assertEquals("", MeasurePad.live(emptied))
        door("FD1.w", MeasurePad.live(emptied)).figs[0].let { assertEquals("▶1400", it.text); assertFalse(it.typing); assertTrue(it.check); assertEquals(wide("▶1400!"), it.w) }
        door("FD1.l", "").figs[2].let { assertEquals("▶≈150", it.text); assertEquals(MarkDims.State.ESTIMATE, it.state); assertFalse(it.typing) }
        // digits with no active dimension, or on a mark that does not hold it, change nothing
        assertEquals(door(null, null).figs.map { it.text }, door(null, "55").figs.map { it.text })
        val other = DimLayout.mark(stored, "front", window, W, H, false, false, "FD1.w", ::toPx, U, DP, ::textW, "55")
        assertTrue(other.figs.none { it.typing || it.active })
    }


    // ------------------------------------------------------------------ a mark with four free corners (port 63 slice Q1)

    /**
     * THE DESIGN'S OWN EXAMPLE (DESIGN-quad §0, §2.5), made up: the front wall 3600 x 2700 mm, and on it window FW1 with
     * uprights 1550 (right) and 780 (left), a level top of 2370 and so a sloping bottom. AT THE PHONE'S OWN NUMBERS: the
     * Mi A3's photo 340 dp = 680 px wide at 2 px a dp, so 510 px high for this wall (one scale both ways, as the squared
     * picture has), the painter's unit 680 / 480 px.
     */
    private inner class QuadRig(val width: Double = 680.0, val u: Double = 680.0 / 480, val dp: Double = 2.0) {
        val a = 3600.0; val b = 2700.0; val height = width * b / a
        val window = SurveyPrep.Mark("id-W1", "W", "FW1", 600.0, 2970.0, 900.0, 2450.0).also {
            assertTrue(Quad.set(it, listOf(doubleArrayOf(600.0, 1670.0), doubleArrayOf(2970.0, 900.0), doubleArrayOf(2970.0, 2450.0), doubleArrayOf(600.0, 2450.0)))) }
        fun toPx(x: Double, y: Double) = doubleArrayOf(x * width / a, height - y * width / a)
        fun textW(text: String, fs: Double, bold: Boolean) = text.length * fs * 0.6
        val times: Double get() = textW(" × ", 10.0 * dp, false)
        fun mark(e: SurveyPrep.Mark, picked: Boolean = false, active: String? = null, actual: Map<String, Double> = emptyMap(), tabs: Boolean = false, free: Boolean = false) =
            DimLayout.mark(actual, "front", e, a, b, picked, tabs, active, ::toPx, u, dp, ::textW, null, free)
        /** Where its code is drawn: inside the bounding box's top-left corner, which a four-corner mark's code keeps. */
        fun code(e: SurveyPrep.Mark): Box { val p = toPx(e.x0, e.y1); return Box(p[0] + 6 * u, p[1] + 3 * u, p[0] + 6 * u + textW(e.code, 13.0 * u, true), p[1] + 19 * u) }
        val column = Box(width - 50 * dp, 6.0 * dp, width - 6 * dp, 84.0 * dp)
        fun place(ms: List<DimLayout.Mark>, scope: OldPlace.Scope, keep: List<Box>) = OldPlace.place(ms, scope, width, height, keep, u, dp, times)
        /** The shape on the canvas, q0 … q3. */
        fun corners(e: SurveyPrep.Mark) = Quad.corners(e).map { toPx(it[0], it[1]) }
        /** Which side of the shape on the canvas is nearest ([x], [y]), and how far. */
        fun nearest(e: SurveyPrep.Mark, x: Double, y: Double): Pair<Int, Double> = corners(e).let { c ->
            (0 until 4).map { i -> i to SurveyMeas.segDist(x, y, c[i], c[(i + 1) % 4]) }.minByOrNull { it.second }!! }
        fun sound(out: DimLayout.Out, keep: List<Box>, forced: Set<String>, what: String) {
            val plates = out.plates.filter { !it.dot }
            for (p in out.plates) {
                val e = if (p.dot) 0.0 else DimLayout.EDGE_U * u
                assertTrue(p.box.l >= e - 1e-9 && p.box.t >= e - 1e-9 && p.box.r <= width - e + 1e-9 && p.box.b <= height - e + 1e-9, "$what: ${p.code} is off the photo: ${p.box}")
                assertTrue(p.tap.w >= 32 * dp - 1e-9 && p.tap.h >= 32 * dp - 1e-9, "$what: ${p.code}'s tap box")
                if (!p.dot && p.code !in forced) for (k in keep) assertFalse(hit(p.box, k), "$what: ${p.code} ${p.box} covers a kept box $k")
                if (p.dot) for (o in plates) assertFalse(hit(p.box, o.box), "$what: the dot ${p.code} lies under ${o.code}")
            }
            for (i in plates.indices) for (j in i + 1 until plates.size) assertFalse(hit(plates[i].box, plates[j].box), "$what: ${plates[i].code} and ${plates[j].code} overlap")
            assertEquals(out.plates.size, out.plates.map { it.code }.toSet().size, what)
        }
    }

    @Test fun `the four-corner example at the phone's own numbers -- seven figures, each side's plate on its own side`() {
        val r = QuadRig(); val e = r.window; val keep = listOf(r.code(e), r.column)
        val SEVEN = listOf("FW1.bottom", "FW1.right", "FW1.top", "FW1.left", "FW1.l", "FW1.s", "FW1.p")
        assertEquals(510.0, r.height); assertEquals(0.71, r.u / r.dp, 0.005)
        val m = r.mark(e, picked = true, active = "FW1.bottom")
        assertEquals(SEVEN, m.figs.map { it.code }); assertFalse(m.tabs); assertNull(m.corners)
        assertEquals(listOf(MarkDims.Role.SIDE, MarkDims.Role.SIDE, MarkDims.Role.SIDE, MarkDims.Role.SIDE, MarkDims.Role.OFF_H, MarkDims.Role.OFF_V, MarkDims.Role.DEPTH), m.figs.map { it.role })
        assertEquals(listOf("▶≈2492", "≈1550", "≈2370", "≈780", "≈600", "≈900", "⊥ ?"), m.figs.map { it.text })
        // its box on the canvas is its BOUNDING box, which its code, its padlock and the trash keep
        val p = r.toPx(600.0, 2450.0); val q = r.toPx(2970.0, 900.0)
        assertEquals(listOf(p[0], p[1], q[0], q[1]), l(m.box))
        // EACH SIDE'S LINE runs parallel to its side, 6 units outside the shape; the true sloping side is not levelled
        val c = r.corners(e); val off = 6 * r.u
        for (i in 0 until 4) {
            val f = m.figs[i]; val ln = assertNotNull(f.line); val n = assertNotNull(f.out); val sp = assertNotNull(f.span)
            val a0 = c[i]; val a1 = c[(i + 1) % 4]
            assertEquals(listOf(a0[0], a0[1], a1[0], a1[1]), listOf(sp.x1, sp.y1, sp.x2, sp.y2), f.code)
            assertEquals(listOf(a0[0] + n[0] * off, a0[1] + n[1] * off, a1[0] + n[0] * off, a1[1] + n[1] * off), listOf(ln.x1, ln.y1, ln.x2, ln.y2), f.code)
            assertEquals(1.0, n[0] * n[0] + n[1] * n[1], 1e-9); assertEquals(0.0, n[0] * (a1[0] - a0[0]) + n[1] * (a1[1] - a0[1]), 1e-9, "${f.code} is square to its side")
            // out of the shape: away from its middle
            val mid = ln.at(0.5); val cx = c.map { it[0] }.average(); val cy = c.map { it[1] }.average()
            assertTrue((mid[0] - cx) * n[0] + (mid[1] - cy) * n[1] > 0, f.code)
            assertEquals(i, r.nearest(e, mid[0], mid[1]).first, "${f.code}'s line stands off its own side"); assertEquals(off, r.nearest(e, mid[0], mid[1]).second, 1e-9)
        }
        // on the canvas, y down: the right side's way out is right, the top's up, the left's left, the bottom's down and left
        assertEquals(listOf(1.0, 0.0), m.figs[1].out!!.map { Math.round(it * 1e9) / 1e9 + 0.0 }); assertEquals(listOf(0.0, -1.0), m.figs[2].out!!.map { Math.round(it * 1e9) / 1e9 + 0.0 })
        assertEquals(listOf(-1.0, 0.0), m.figs[3].out!!.map { Math.round(it * 1e9) / 1e9 + 0.0 }); assertTrue(m.figs[0].out!!.let { it[0] < 0 && it[1] > 0 })
        // ITS POSITION LINES run to the corner and are NOT shifted off-centre, as a point's are not: the corner line level
        // with the middle of its upright left side, the sill plumb under its low corner q1; its p hangs at q1
        val lf = m.figs[4].line!!; val sf = m.figs[5].line!!
        val yl = r.toPx(0.0, 2060.0)[1]
        assertEquals(0.0, lf.x1); assertEquals(p[0], lf.x2); assertEquals(yl, lf.y1, 1e-3); assertEquals(yl, lf.y2, 1e-3)
        assertEquals(listOf(q[0], r.height, q[0], q[1]), listOf(sf.x1, sf.y1, sf.x2, sf.y2))
        assertEquals(listOf(q[0], q[1]), m.figs[6].at!!.toList()); assertNull(m.figs[6].line); assertNull(m.figs[4].out)
        // AS 3 · MEASURE OPENS, nothing picked: its four sides -- they are its size -- and nothing of its position
        val open = r.place(listOf(r.mark(e)), OldPlace.Scope.FACE, keep)
        r.sound(open, keep, emptySet(), "FACE, nothing picked")
        assertEquals(SEVEN.take(4), open.plates.map { it.code }); assertEquals(QUAD_FACE_DOTS, open.dots)
        // PICKED, the bottom being measured: all seven placed
        val out = r.place(listOf(m), OldPlace.Scope.FACE, keep)
        r.sound(out, keep, setOf("FW1.bottom"), "FACE, picked")
        assertEquals(SEVEN, out.plates.map { it.code }); assertEquals(QUAD_PICKED_DOTS, out.dots, out.plates.filter { it.dot }.joinToString { it.code })
        // each side's plate on ITS OWN side: outside the shape, nearer its own side than any other
        for (i in 0 until 4) {
            val pl = out.plates[i]; if (pl.dot) continue
            val cx = (pl.box.l + pl.box.r) / 2; val cy = (pl.box.t + pl.box.b) / 2
            assertFalse(Pano360.inPoly(cx, cy, c), "${pl.code}'s plate is inside the shape")
            assertEquals(i, r.nearest(e, cx, cy).first, "${pl.code}'s plate ${pl.box} stands by another side")
            assertTrue(pl.how == DimLayout.How.ON_LINE || pl.how == DimLayout.How.OFF_LINE, "${pl.code}: ${pl.how}")
        }
        // upright on the top and on the sloping bottom (18 degrees off level); turned to read bottom to top on the two
        // uprights -- never turned to the slope
        assertEquals(listOf(false, true, false, true), out.plates.take(4).map { it.turned })
        // the active plate sits mid-span on the true sloping line, which it breaks
        val bp = out.of("FW1.bottom")!!; val bl = bp.line!!
        assertEquals(DimLayout.How.ON_LINE, bp.how); assertFalse(bp.dot)
        assertEquals(bl.at(0.5).toList().map { Math.round(it * 1e6) / 1e6 }, listOf((bp.box.l + bp.box.r) / 2, (bp.box.t + bp.box.b) / 2).map { Math.round(it * 1e6) / 1e6 })
        assertTrue(bl.y2 != bl.y1 && !bl.vertical)
        // no compact plate and no hand-anchored tag: it has no w and no h
        assertTrue(out.plates.none { it.how == DimLayout.How.COMPACT || it.how == DimLayout.How.HAND }); assertEquals(DimLayout.How.CHIP, out.of("FW1.p")!!.how)
        // WITH all ON: the same seven, whether or not it is picked
        for (pk in listOf(false, true)) {
            val all = r.place(listOf(r.mark(e, picked = pk)), OldPlace.Scope.ALL, keep)
            r.sound(all, keep, emptySet(), "ALL, picked $pk"); assertEquals(SEVEN, all.plates.map { it.code }); assertEquals(QUAD_ALL_DOTS, all.dots, "picked $pk")
        }
        // whichever of its seven is being measured, it is a plate and the layout is sound
        for (k in SEVEN) {
            val o = r.place(listOf(r.mark(e, picked = true, active = k)), OldPlace.Scope.FACE, keep)
            r.sound(o, keep, setOf(k), "FACE, $k active"); assertEquals(SEVEN, o.plates.map { it.code }); assertFalse(o.of(k)!!.dot, k)
        }
        // readings change the texts, never the soundness: the closing check's ! rides on the sloping side's plate alone
        val read = mapOf("FW1.bottom" to 2495.0, "FW1.right" to 1550.0, "FW1.top" to 2370.0, "FW1.left" to 870.0)
        val rd = r.mark(e, picked = true, active = "FW1.l", actual = read)
        assertEquals(listOf(true, false, false, false, false, false, false), rd.figs.map { it.check })
        assertEquals(listOf("2495", "1550", "2370", "870", "▶≈600", "≈900", "⊥ ?"), rd.figs.map { it.text })
        r.sound(r.place(listOf(rd), OldPlace.Scope.FACE, keep), keep, setOf("FW1.l"), "FACE, read")
        // the same input gives the same output
        fun dump(o: DimLayout.Out) = o.plates.map { listOf(it.code, it.how, it.turned, it.arrowsIn, l(it.box), l(it.tap), it.leader?.toString()) }
        assertEquals(dump(out), dump(r.place(listOf(r.mark(e, picked = true, active = "FW1.bottom")), OldPlace.Scope.FACE, keep)))
    }

    @Test fun `a picked mark whose corners are free keeps its four corner handles clear, not eight tabs`() {
        val r = QuadRig(); val e = r.window; val keep = listOf(r.code(e), r.column)
        // 2 · Mark, the mark picked, editable and unlocked (tabs) with its corners free: four corners, no tabs
        val m = r.mark(e, picked = true, active = "FW1.bottom", tabs = true, free = true)
        assertFalse(m.tabs); val cs = assertNotNull(m.corners)
        assertEquals(r.corners(e).map { it.toList() }, cs.map { it.toList() })
        // each handle a disc of 5.5 dp: the square its plates keep clear is 11 px either side at the phone's numbers
        val handles = DimLayout.cornerBoxes(cs, r.u, r.dp)
        assertEquals(4, handles.size); assertEquals(listOf(cs[0][0] - 11, cs[0][1] - 11, cs[0][0] + 11, cs[0][1] + 11), l(handles[0]))
        assertEquals(5.5, SurveyGuide.END_R_DP)
        val out = r.place(listOf(m), OldPlace.Scope.PICKED, keep)
        r.sound(out, keep + handles, setOf("FW1.bottom"), "PICKED, corners free")
        assertEquals(7, out.plates.size)
        for (p in out.plates) if (!p.dot) for (h in handles) assertFalse(hit(p.box, h), "${p.code} ${p.box} covers a corner handle $h")
        // its side plates still turn on the uprights: only a w or an h is anchored by hand, and it has neither
        assertTrue(out.plates.none { it.how == DimLayout.How.HAND }); assertEquals(listOf(false, true, false, true), out.plates.take(4).map { it.turned })
        // its p's chip stands off q1 by the tags' own clearance, so the corner's handle stays free
        val chip = out.of("FW1.p")!!
        if (chip.how == DimLayout.How.CHIP) assertTrue(handles.none { hit(chip.box, it) })
        // NOT free -- or shown no handles at all (locked, a later phase, 3 · Measure) -- it has no corners here
        assertNull(r.mark(e, picked = true, tabs = false, free = true).corners); assertFalse(r.mark(e, picked = true, tabs = false, free = true).tabs)
        r.mark(e, picked = true, tabs = true, free = false).let { assertTrue(it.tabs); assertNull(it.corners) }
        // corners asked for outside 2 · Mark keep nothing clear: only PICKED shows handles
        val face = r.place(listOf(m), OldPlace.Scope.FACE, keep)
        assertEquals(l(r.place(listOf(r.mark(e, picked = true, active = "FW1.bottom")), OldPlace.Scope.FACE, keep).of("FW1.top")!!.box), l(face.of("FW1.top")!!.box))
        // A RECTANGLE WHOSE CORNERS WERE FREED (the menu's Four corners, before any corner is dragged) shows its box's
        // four corners in the tabs' place -- and its w and h, which it still has
        val rect = SurveyPrep.Mark("id-D1", "D", "FD1", 600.0, 1500.0, 0.0, 2100.0)
        val fr = r.mark(rect, picked = true, tabs = true, free = true)
        assertFalse(fr.tabs); assertEquals(listOf(r.toPx(600.0, 0.0), r.toPx(1500.0, 0.0), r.toPx(1500.0, 2100.0), r.toPx(600.0, 2100.0)).map { it.toList() }, fr.corners!!.map { it.toList() })
        assertEquals(listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p"), fr.figs.map { it.code })
        val fo = r.place(listOf(fr), OldPlace.Scope.PICKED, listOf(r.code(rect), r.column))
        for (p in fo.plates) if (!p.dot) for (h in DimLayout.cornerBoxes(fr.corners!!, r.u, r.dp)) assertFalse(hit(p.box, h), "${p.code} covers a corner handle")
        // ... and the same rectangle, its corners not free, is the mark it always was: eight tabs, the default
        val plain = DimLayout.mark(emptyMap(), "front", rect, r.a, r.b, true, true, null, r::toPx, r.u, r.dp, r::textW)
        assertTrue(plain.tabs); assertNull(plain.corners); assertTrue(plain.figs.all { it.out == null })
        // a point is never free, whatever it is told
        val pt = SurveyPrep.Mark("id-E1", "E", "FE1", 1475.0, 1525.0, 275.0, 325.0)
        assertNull(r.mark(pt, picked = true, tabs = true, free = true).corners)
    }

    @Test fun `a very small four-corner mark has no compact plate -- its four sides fall to leaders and dots`() {
        val r = QuadRig()
        // a switch plate 200 x 90 mm, its top sloping: 38 x 17 px on the phone's photo
        val e = SurveyPrep.Mark("id-S1", "S", "FS1", 1100.0, 1300.0, 1100.0, 1190.0)
        assertTrue(Quad.set(e, listOf(doubleArrayOf(1100.0, 1100.0), doubleArrayOf(1300.0, 1100.0), doubleArrayOf(1300.0, 1160.0), doubleArrayOf(1100.0, 1190.0))))
        val keep = listOf(r.code(e), r.column)
        for (scope in listOf(OldPlace.Scope.FACE, OldPlace.Scope.ALL)) {
            val out = r.place(listOf(r.mark(e)), scope, keep)
            r.sound(out, keep, emptySet(), "small, $scope")
            assertEquals(listOf("FS1.bottom", "FS1.right", "FS1.top", "FS1.left"), out.plates.take(4).map { it.code })
            assertTrue(out.plates.none { it.how == DimLayout.How.COMPACT || it.how == DimLayout.How.HAND }, out.plates.joinToString { "${it.code} ${it.how}" })
            assertTrue(out.plates.take(4).none { it.how == DimLayout.How.ON_LINE }, out.plates.joinToString { "${it.code} ${it.how}" })
            // every side too short for its plate: off its line on a leader with the arrowheads pointing in, or a dot
            for (p in out.plates.take(4)) { assertTrue(p.arrowsIn, p.code); assertTrue(p.dot || p.leader != null, p.code) }
        }
    }

    // ------------------------------------------------------------------ the one step: no scope, the HELD mark's tabs (port 63 slice I1a)

    /** The example wall as [PhotoFigures.layout] hands it to [DimLayout.placeShown]: what is picked, held and gold. */
    private fun Rig.shown(pick: Pick.State, actual: Map<String, Double> = emptyMap(), els: List<SurveyPrep.Mark> = wall, live: String? = null,
                          ready: Boolean = true): List<DimLayout.Mark> =
        els.map { e -> DimLayout.markShown(com.malletcrafts.interiorestimate.pano.i18n.Tx.EN, actual, "front", e, W, H, pick.picked == e.id, pick.picked == e.id && pick.held, pick.gold, ::toPx, u, dp, ::textW, live, ready = ready) }

    private fun Rig.placeShown(ms: List<DimLayout.Mark>, keep: List<Box> = keep()) = DimLayout.placeShown(ms, size, size, keep, u, dp, times)

    @Test fun `no DRAWN plate of the held mark overlaps one of its tab squares - re-keyed from the scope to the mark that shows its tabs`() {
        // DESIGN-imagemeter §13 (the plan critic's finding C5): with OldPlace.Scope gone, what keeps the held mark's w
        // and h plates off its own tabs is the mark itself -- Mark.tabs, the HELD box mark
        val els = listOf(door, window) + points
        val read = MarkDims.of("front", door, W, H).associate { it.code to (it.est ?: 120.0) }
        for (r in listOf(fit, phone)) for ((what, actual, gold) in listOf(
            Triple("just placed, its width gold", emptyMap<String, Double>(), "FD1.w"), Triple("held, nothing gold", emptyMap<String, Double>(), null),
            Triple("held, every figure read", read, null), Triple("held, its height gold over a reading", read, "FD1.h"))) {
            val ms = r.shown(Pick.State(door.id, true, gold), actual, els)
            assertTrue(ms[0].tabs, what); assertTrue(ms.drop(1).none { it.tabs }, what)
            val out = r.placeShown(ms, r.keep(els))
            val tabs = DimLayout.tabBoxes(ms[0].box, r.u)
            assertEquals(8, tabs.size)
            // the w and h plates are anchored by hand, as the painter anchors them today, and NO plate covers a tab square
            assertEquals(DimLayout.How.HAND, out.of("FD1.w")!!.how, what); assertEquals(DimLayout.How.HAND, out.of("FD1.h")!!.how, what)
            for (pl in out.plates.filter { it.markId == door.id }) for (t in tabs) assertFalse(hit(pl.whole ?: pl.box, t), "$what: ${pl.code} ${pl.box} covers a tab $t")
            r.sound(out, r.keep(els) + tabs, setOfNotNull(gold), "held door, $what")
            // exactly where Scope.PICKED put them: the old path and the new one agree on the hand anchor -- the width level
            // as built; the height, since slice I1c (P5), TURNED UP from the same left edge and the same middle height,
            // so it runs along its line too
            val old = r.place(r.marks(picked = door.id, active = gold, actual = actual, tabs = true, els = els), OldPlace.Scope.PICKED, r.keep(els))
            old.of("FD1.w")!!.box.let { o -> val n = out.of("FD1.w")!!; assertEquals(o.t, n.box.t, 1e-9, what); assertEquals(o.b, n.box.b, 1e-9, what); assertEquals(0.0, n.angle) }
            old.of("FD1.h")!!.box.let { o -> val n = out.of("FD1.h")!!
                assertEquals(DimLayout.How.HAND, n.how); assertEquals(LabelAngle.UP, n.angle); assertTrue(n.turned, what)
                assertEquals(o.l, n.box.l, 1e-9, what); assertEquals((o.t + o.b) / 2, (n.box.t + n.box.b) / 2, 1e-9, what)
                // stood up: its box is the plate turned, ph wide and pw tall (its text is "?" or a reading now, no estimate)
                assertEquals(o.h, n.box.w, 1e-9, what); assertEquals(n.pw, n.box.h, 1e-9, what) }
        }
        // A MARK THE WALK REACHED is picked and shows NO tabs: its plates sit on their lines, and no tab square is kept clear
        for (r in listOf(fit, phone)) {
            val ms = r.shown(Pick.State(door.id, false, "FD1.w"), els = els)
            assertFalse(ms[0].tabs)
            val out = r.placeShown(ms, r.keep(els))
            assertTrue(out.plates.none { it.how == DimLayout.How.HAND }); assertEquals(DimLayout.How.ON_LINE, out.of("FD1.w")!!.how)
        }
        // a held POINT has no tabs to keep clear; a held LOCKED mark does not exist (Pick never holds one)
        assertFalse(fit.shown(Pick.State(points[0].id, true, null))[3].tabs)
    }

    @Test fun `placeShown places every figure it is handed and nothing else - an unpicked, unmeasured mark costs the layout nothing`() {
        for (r in listOf(fit, phone)) {
            // a wall nobody has measured, nothing picked: no plate, no dot, no tap box
            val none = r.placeShown(r.shown(Pick.NONE))
            assertTrue(none.plates.isEmpty()); assertTrue(none.tags.isEmpty())
            // the door's width read: ONE plate on the whole wall
            val one = r.placeShown(r.shown(Pick.NONE, mapOf("FD1.w" to 898.0)))
            assertEquals(listOf("FD1.w"), one.plates.map { it.code }); assertEquals("898", one.of("FD1.w")!!.fig.text)
            // the window picked by the walk, its height gold: all five of its figures, and still the door's one
            val w = r.placeShown(r.shown(Pick.State(window.id, false, "FW1.h"), mapOf("FD1.w" to 898.0, "FW1.w" to 912.0)))
            assertEquals(listOf("FD1.w", "FW1.w", "FW1.h", "FW1.r", "FW1.s", "FW1.p"), w.plates.map { it.code })
            assertEquals(listOf("898", "912", "▶?", "?", "?", "⊥ ?"), w.plates.map { it.fig.text })
            // the window's reveal depth gold: the one plate that carries its name, and it is still placed whole
            r.placeShown(r.shown(Pick.State(window.id, false, "FW1.p"))).let { o ->
                assertEquals("▶⊥ reveal depth ?", o.active!!.fig.text); assertFalse(o.active!!.dot); r.sound(o, r.keep(), setOf("FW1.p"), "the window's depth gold") }
            assertEquals("FW1.h", w.active!!.code); assertFalse(w.active!!.dot)
            r.sound(w, r.keep(), setOf("FW1.h"), "the window picked by the walk")
            // NO PLATE ANYWHERE READS AN ESTIMATE
            assertTrue(w.plates.none { "≈" in it.fig.text || it.fig.state == MarkDims.State.ESTIMATE })
        }
    }

    private companion object {
        /** How many of the example wall's 19 figures shrink to a dot with `all` on at fit, nothing picked: on the round
         *  canvas (340 px, 1 px a dp, 1 px a unit), and at the phone's own numbers (680 px, 2 px a dp, 680 / 480 px a unit). */
        const val ALL_DOTS = 2
        const val PHONE_ALL_DOTS = 1
        /** How many of the four-corner example's figures shrink to a dot at the phone's own numbers, pinned AS FOUND: as
         *  3 · Measure opens (its four sides), picked (its seven), and with `all` on (its seven). */
        const val QUAD_FACE_DOTS = 0
        const val QUAD_PICKED_DOTS = 0
        const val QUAD_ALL_DOTS = 0
    }
}
