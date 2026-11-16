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
 * example wall -- a door, a window, a switch plate and three electric points on a 2678 x 2677 front wall, drawn on a
 * photo 340 dp square (the Mi A3's, at 1 px a dp here so every figure below is in dp). Its 19 figures are 8 plates as
 * 3 · Measure opens, and all 19 with `all` on.
 */
class DimLayoutTest {

    private val W = 2678.0; private val H = 2677.0
    private val SIZE = 340.0
    private val U = 1.0; private val DP = 1.0

    /** Face mm to canvas px: the wall fills the photo, y down. */
    private fun toPx(x: Double, y: Double) = doubleArrayOf(x * SIZE / W, SIZE - y * SIZE / H)

    /** A monospace figure: 0.6 of its size a character (the painter measures the real text). */
    private fun textW(text: String, fs: Double, bold: Boolean) = text.length * fs * 0.6
    private val TIMES = textW(" × ", 10.0, false)

    private fun mk(t: String, n: Int, x0: Double, x1: Double, y0: Double, y1: Double, extra: Map<String, Any?> = emptyMap()) =
        SurveyPrep.Mark("id-$t$n", t, "F$t$n", x0, x1, y0, y1, extra)

    private val door = mk("D", 1, 150.0, 1050.0, 0.0, 2100.0)
    private val window = mk("W", 1, 1350.0, 2550.0, 900.0, 2100.0)
    private val switch = mk("S", 1, 1100.0, 1300.0, 1100.0, 1190.0)
    private val points = listOf(mk("E", 1, 1475.0, 1525.0, 275.0, 325.0), mk("E", 2, 1975.0, 2025.0, 275.0, 325.0), mk("E", 3, 2475.0, 2525.0, 275.0, 325.0))
    private val wall = listOf(door, window, switch) + points

    /** The example wall as the painter hands it over: [picked] the picked mark's id, [active] the key being measured. */
    private fun marks(picked: String? = null, active: String? = null, actual: Map<String, Double> = emptyMap(),
                      tabs: Boolean = false, els: List<SurveyPrep.Mark> = wall): List<DimLayout.Mark> =
        els.map { e -> DimLayout.mark(actual, "front", e, W, H, e.id == picked, tabs && e.id == picked && !SurveyPrep.isPoint(e.t) && !SurveyPrep.isLocked(e),
            active, ::toPx, U, ::textW) }

    /** Where each mark's code is drawn: inside a box's top-left corner, beside a point. */
    private fun codes(els: List<SurveyPrep.Mark> = wall): List<Box> = els.map { e ->
        val p = toPx(e.x0, e.y1); val w = textW(e.code, 13.0, true)
        if (SurveyPrep.isPoint(e.t)) Box(p[0] + 14, p[1] - 18, p[0] + 14 + w, p[1] - 2) else Box(p[0] + 6, p[1] + 3, p[0] + 6 + w, p[1] + 19)
    }

    /** The fit / grid / all column in the photo's top-right corner. */
    private val column = Box(SIZE - 50, 6.0, SIZE - 6, 84.0)
    private fun keep(els: List<SurveyPrep.Mark> = wall) = codes(els) + column

    private fun place(ms: List<DimLayout.Mark>, scope: DimLayout.Scope, keep: List<Box> = keep()) =
        DimLayout.place(ms, scope, SIZE, SIZE, keep, U, DP, TIMES)

    private fun hit(a: Box, b: Box) = a.l < b.r && a.r > b.l && a.t < b.b && a.b > b.t
    private fun same(a: Box, b: Box) = abs(a.l - b.l) < 1e-9 && abs(a.t - b.t) < 1e-9 && abs(a.r - b.r) < 1e-9 && abs(a.b - b.b) < 1e-9
    private fun l(b: Box) = listOf(b.l, b.t, b.r, b.b)

    /** The boxes drawn that are not dots: each plate once, a compact plate whole. */
    private fun solid(out: DimLayout.Out): List<Box> = out.plates.filter { !it.dot }.map { it.whole ?: it.box }.distinctBy { l(it) }

    /** What must hold of every layout: inside the photo, no plate on another, none on a kept box, taps of 32 dp. */
    private fun sound(out: DimLayout.Out, keep: List<Box>, forced: Set<String> = emptySet(), what: String) {
        val edge = DimLayout.EDGE_U * U
        for (p in out.plates) {
            val b = p.whole ?: p.box
            assertTrue(b.l >= (if (p.dot) 0.0 else edge) - 1e-9 && b.t >= (if (p.dot) 0.0 else edge) - 1e-9 &&
                b.r <= SIZE - (if (p.dot) 0.0 else edge) + 1e-9 && b.b <= SIZE - (if (p.dot) 0.0 else edge) + 1e-9, "$what: ${p.code} is off the photo: $b")
            assertTrue(p.tap.w >= 32 * DP - 1e-9 && p.tap.h >= 32 * DP - 1e-9, "$what: ${p.code}'s tap box is ${p.tap.w} x ${p.tap.h}")
            assertTrue(p.tap.l <= p.box.l + 1e-9 && p.tap.r >= p.box.r - 1e-9 && p.tap.t <= p.box.t + 1e-9 && p.tap.b >= p.box.b - 1e-9, "$what: ${p.code}'s tap box holds its plate")
            if (!p.dot && p.code !in forced) for (k in keep) assertFalse(hit(b, k), "$what: ${p.code} $b covers a kept box $k")
        }
        val s = solid(out)
        for (i in s.indices) for (j in i + 1 until s.size) assertFalse(hit(s[i], s[j]), "$what: two plates overlap: ${s[i]} and ${s[j]}")
        // every code once
        assertEquals(out.plates.size, out.plates.map { it.code }.toSet().size, what)
        assertEquals(out.plates.map { it.code }, out.tags.map { it.code }, what)
    }

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
        assertEquals(46.0, DimLayout.plateW(36.0, U)); assertEquals(19.0, DimLayout.plateH(12.0, U)); assertEquals(17.0, DimLayout.plateH(10.0, U))
        assertEquals(12.0, DimLayout.textSize(true, U)); assertEquals(10.0, DimLayout.textSize(false, U)); assertEquals(24.0, DimLayout.textSize(true, 2.0))
    }

    @Test fun `which figures a scope shows, and the order they are placed in`() {
        val all = MarkDims.Role.values().toList()
        val size = setOf(MarkDims.Role.WIDTH, MarkDims.Role.HEIGHT)
        for (r in all) {
            // 2 · Mark: the picked mark, everything; nobody else
            assertTrue(DimLayout.shows(DimLayout.Scope.PICKED, true, r)); assertFalse(DimLayout.shows(DimLayout.Scope.PICKED, false, r))
            // 3 · Measure: every mark its size, the picked mark its position too
            assertTrue(DimLayout.shows(DimLayout.Scope.FACE, true, r)); assertEquals(r in size, DimLayout.shows(DimLayout.Scope.FACE, false, r))
            // all: every figure of every mark
            assertTrue(DimLayout.shows(DimLayout.Scope.ALL, true, r)); assertTrue(DimLayout.shows(DimLayout.Scope.ALL, false, r))
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
        val m = DimLayout.mark(actual, "front", door, W, H, picked = true, tabs = true, active = "FD1.l", toPx = ::toPx, u = U, textW = ::textW)
        assertEquals(listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p"), m.figs.map { it.code })
        assertTrue(m.picked && m.tabs); assertEquals("id-D1", m.id)
        val p = toPx(door.x0, door.y1); val q = toPx(door.x1, door.y0)
        assertEquals(listOf(p[0], p[1], q[0], q[1]), l(m.box))
        // the picked mark's figures at 12; "1400!" (the check fires: ≈900 on the photo), "2104", "▶≈150", "⊥ ?"
        assertEquals(listOf(12.0, 12.0, 12.0, 12.0), m.figs.map { it.fs })
        assertEquals(listOf("1400!", "2104", "▶≈150", "⊥ ?").map { DimLayout.plateW(textW(it, 12.0, false), U) }, m.figs.map { it.w })
        assertEquals(listOf(DimLayout.Rank.PICKED, DimLayout.Rank.PICKED, DimLayout.Rank.ACTIVE, DimLayout.Rank.PICKED), m.figs.map { it.rank })
        assertEquals(listOf(true, true, false, false), m.figs.map { it.measured })
        // what the painter draws on each plate rides along: the text without its !, its state, the check, the active one
        assertEquals(listOf("1400", "2104", "▶≈150", "⊥ ?"), m.figs.map { it.text })
        assertEquals(listOf(MarkDims.State.MEASURED, MarkDims.State.MEASURED, MarkDims.State.ESTIMATE, MarkDims.State.UNKNOWN), m.figs.map { it.state })
        assertEquals(listOf(true, false, false, false), m.figs.map { it.check }); assertEquals(listOf(false, false, true, false), m.figs.map { it.active })
        val placed = DimLayout.place(listOf(m), DimLayout.Scope.FACE, SIZE, SIZE, emptyList(), U, DP, TIMES)
        assertEquals(m.figs.map { it.text }, placed.plates.map { it.fig.text }); assertEquals("id-D1", placed.plates[0].markId)
        // an upright plate's text starts 5 inside its box and sits 5 above its bottom, as the painter's tag did
        val wp = placed.of("FD1.w")!!
        assertEquals(wp.box.l + 5, DimLayout.textLeft(wp.box, U), 1e-9); assertEquals(wp.box.b - 5, DimLayout.baseline(wp.box, U), 1e-9)
        // a p has no line and an anchor; every other figure a line and none
        assertEquals(listOf(true, true, true, false), m.figs.map { it.line != null }); assertEquals(listOf(false, false, false, true), m.figs.map { it.at != null })
        // not picked: 10, and ranked by whether it is measured; a mark that is not in the room ranks last
        val other = DimLayout.mark(actual, "front", door, W, H, picked = false, tabs = false, active = null, toPx = ::toPx, u = U, textW = ::textW)
        assertEquals(listOf(10.0, 10.0, 10.0, 10.0), other.figs.map { it.fs })
        assertEquals(listOf(DimLayout.Rank.MEASURED, DimLayout.Rank.MEASURED, DimLayout.Rank.UNMEASURED, DimLayout.Rank.UNMEASURED), other.figs.map { it.rank })
        val wish = mk("D", 2, 150.0, 1050.0, 0.0, 2100.0, mapOf(MarkLife.KEY_BORN to MarkLife.WISH))
        val away = DimLayout.mark(mapOf("FD2.w" to 900.0), "front", wish, W, H, false, false, null, ::toPx, U, ::textW)
        assertEquals(listOf(DimLayout.Rank.AWAY, DimLayout.Rank.AWAY, DimLayout.Rank.AWAY, DimLayout.Rank.AWAY), away.figs.map { it.rank })
        assertEquals(listOf(true, false, false, false), away.figs.map { it.measured })     // a reading it holds from an earlier build is still shown as one
        // a Note asks nothing: no figures
        assertEquals(0, DimLayout.mark(actual, "front", mk(MarkTypes.NOTE, 1, 300.0, 350.0, 900.0, 950.0), W, H, true, false, null, ::toPx, U, ::textW).figs.size)
    }

    // ------------------------------------------------------------------ the rule, one step at a time

    private fun fig(code: String, x1: Double, y1: Double, x2: Double, y2: Double, rank: DimLayout.Rank, w: Double = 40.0,
                    role: MarkDims.Role = MarkDims.Role.OFF_H, measured: Boolean = false) =
        DimLayout.Fig(code, role, DimLayout.Seg(x1, y1, x2, y2), null, w, 10.0, rank, measured)
    private fun one(vararg figs: DimLayout.Fig) = listOf(DimLayout.Mark("m", Box(0.0, 0.0, 10.0, 10.0), figs.toList()))

    @Test fun `a plate sits mid-span on its line, and the h plate is turned to read bottom to top`() {
        val out = place(marks(), DimLayout.Scope.FACE)
        val w = assertNotNull(out.of("FW1.w")); val h = assertNotNull(out.of("FW1.h"))
        val wl = assertNotNull(w.line); val hl = assertNotNull(h.line)
        // the window's w: on its line, centred on its middle, upright, 17 high
        assertEquals(DimLayout.How.ON_LINE, w.how); assertFalse(w.turned); assertNull(w.leader); assertFalse(w.arrowsIn)
        assertEquals(wl.at(0.5).toList(), listOf((w.box.l + w.box.r) / 2, (w.box.t + w.box.b) / 2))
        assertEquals(DimLayout.plateW(textW("≈1200", 10.0, false), U), w.box.w, 1e-9); assertEquals(17.0, w.box.h, 1e-9)
        // its h: turned -- the plate's WIDTH runs up the line -- and centred on the line's middle but for the photo's edge
        assertEquals(DimLayout.How.ON_LINE, h.how); assertTrue(h.turned)
        assertEquals(17.0, h.box.w, 1e-9); assertEquals(DimLayout.plateW(textW("≈1200", 10.0, false), U), h.box.h, 1e-9)
        assertEquals(hl.at(0.5)[1], (h.box.t + h.box.b) / 2, 1e-9)
        assertTrue(abs((h.box.l + h.box.r) / 2 - hl.x1) <= 8.5)
        // the door's h, alone on the wall with room either side: exactly on the middle of its line
        val dh = assertNotNull(place(marks(els = listOf(door)), DimLayout.Scope.FACE, keep(listOf(door))).of("FD1.h"))
        assertEquals(assertNotNull(dh.line).at(0.5).toList(), listOf((dh.box.l + dh.box.r) / 2, (dh.box.t + dh.box.b) / 2))
        // ... and on the example wall, where the switch plate's code stands on that middle, a quarter along it instead
        val moved = assertNotNull(out.of("FD1.h"))
        assertEquals(DimLayout.How.ON_LINE, moved.how)
        assertEquals(assertNotNull(moved.line).at(0.25).toList(), listOf((moved.box.l + moved.box.r) / 2, (moved.box.t + moved.box.b) / 2))
    }

    @Test fun `a span too short for its plate -- the plate steps off by one plate height on a leader, arrowheads pointing in`() {
        // a 30 px span and a 40 px plate: it cannot sit on the line
        val out = place(one(fig("A.l", 100.0, 200.0, 130.0, 200.0, DimLayout.Rank.UNMEASURED)), DimLayout.Scope.ALL, emptyList())
        val a = assertNotNull(out.of("A.l"))
        assertEquals(DimLayout.How.OFF_LINE, a.how); assertTrue(a.arrowsIn)
        // above the line by one plate height, centre to line: the plate's bottom edge half a plate height over it
        assertEquals(listOf(115.0 - 20, 200.0 - 17 - 8.5, 115.0 + 20, 200.0 - 8.5), l(a.box))
        val ld = assertNotNull(a.leader)
        assertEquals(listOf(115.0, 200.0, 115.0, 200.0 - 8.5), listOf(ld.x1, ld.y1, ld.x2, ld.y2))
        // 8 dp of room more than the plate is exactly enough to sit on the line
        val fits = place(one(fig("A.l", 100.0, 200.0, 148.0, 200.0, DimLayout.Rank.UNMEASURED)), DimLayout.Scope.ALL, emptyList()).of("A.l")!!
        assertEquals(DimLayout.How.ON_LINE, fits.how); assertFalse(fits.arrowsIn)
        val short = place(one(fig("A.l", 100.0, 200.0, 147.9, 200.0, DimLayout.Rank.UNMEASURED)), DimLayout.Scope.ALL, emptyList()).of("A.l")!!
        assertEquals(DimLayout.How.OFF_LINE, short.how); assertTrue(short.arrowsIn)
        // an upright plate on a vertical line needs only its HEIGHT of span: a 30 px floor line holds it
        val v = place(one(fig("A.s", 200.0, 300.0, 200.0, 270.0, DimLayout.Rank.UNMEASURED, role = MarkDims.Role.OFF_V)), DimLayout.Scope.ALL, emptyList()).of("A.s")!!
        assertEquals(DimLayout.How.ON_LINE, v.how); assertFalse(v.turned)
        assertEquals(listOf(180.0, 285.0 - 8.5, 220.0, 285.0 + 8.5), l(v.box))
    }

    @Test fun `a place taken -- a quarter and three quarters along the line, then off it on a leader, then a dot`() {
        // five figures on lines 3 px apart, each wanting the same middle
        val figs = (0 until 5).map { i -> fig("A.$i", 20.0, 150.0 + 3 * i, 320.0, 150.0 + 3 * i, DimLayout.Rank.UNMEASURED, measured = i == 4) }
        val out = place(one(*figs.toTypedArray()), DimLayout.Scope.ALL, emptyList())
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
        val more = place(one(*(0 until 7).map { i -> fig("A.$i", 20.0, 150.0 + 3 * i, 320.0, 150.0 + 3 * i, DimLayout.Rank.UNMEASURED) }.toTypedArray()), DimLayout.Scope.ALL, emptyList())
        sound(more, emptyList(), what = "seven on one place"); assertEquals(0, more.dots)
        // with everything round the line taken there is no room: a 6 dp dot on its line, with a 32 dp tap box
        val wallOff = listOf(Box(0.0, 0.0, SIZE, 149.0), Box(0.0, 151.0, SIZE, SIZE))
        val none = place(one(fig("A.0", 20.0, 150.0, 320.0, 150.0, DimLayout.Rank.UNMEASURED), fig("A.1", 20.0, 150.0, 320.0, 150.0, DimLayout.Rank.MEASURED, measured = true)),
            DimLayout.Scope.ALL, wallOff)
        for (k in listOf("A.0", "A.1")) {
            val d = none.of(k)!!
            assertTrue(d.dot); assertEquals(DimLayout.How.DOT, d.how)
            assertEquals(listOf(170.0 - 3, 150.0 - 3, 170.0 + 3, 150.0 + 3), l(d.box))          // on its line, at its middle
            assertEquals(listOf(170.0 - 16, 150.0 - 16, 170.0 + 16, 150.0 + 16), l(d.tap))
            assertNull(d.leader); assertNotNull(d.line)
        }
        // hollow not measured, solid measured
        assertFalse(none.of("A.0")!!.measured); assertTrue(none.of("A.1")!!.measured)
        assertEquals(2, none.dots)
    }

    @Test fun `the active dimension is placed first and never shrinks`() {
        // listed LAST, and wanting the very place the picked mark's own figure wants: the active one gets it
        val figs = listOf(fig("A.w", 20.0, 150.0, 320.0, 150.0, DimLayout.Rank.MEASURED), fig("A.h", 20.0, 152.0, 320.0, 152.0, DimLayout.Rank.PICKED),
            fig("A.l", 20.0, 154.0, 320.0, 154.0, DimLayout.Rank.ACTIVE))
        val out = place(one(*figs.toTypedArray()), DimLayout.Scope.ALL, emptyList())
        fun cx(k: String) = out.of(k)!!.box.let { (it.l + it.r) / 2 }
        assertEquals(170.0, cx("A.l")); assertEquals(95.0, cx("A.h")); assertEquals(245.0, cx("A.w"))
        // the order in the answer is the order given, whatever order they were placed in
        assertEquals(listOf("A.w", "A.h", "A.l"), out.plates.map { it.code })
        // with no room at all the active figure is still a plate, at its first place; the other shrinks
        val full = listOf(Box(0.0, 0.0, SIZE, SIZE))
        val forced = place(one(fig("A.w", 20.0, 150.0, 320.0, 150.0, DimLayout.Rank.UNMEASURED), fig("A.l", 20.0, 154.0, 320.0, 154.0, DimLayout.Rank.ACTIVE)), DimLayout.Scope.ALL, full)
        assertEquals(DimLayout.How.ON_LINE, forced.of("A.l")!!.how); assertFalse(forced.of("A.l")!!.dot)
        assertEquals(listOf(150.0, 154.0 - 8.5, 190.0, 154.0 + 8.5), l(forced.of("A.l")!!.box))
        assertTrue(forced.of("A.w")!!.dot)
        // ... and an active p, and the active half of a compact plate, likewise
        val chip = DimLayout.Fig("A.p", MarkDims.Role.DEPTH, null, doubleArrayOf(200.0, 200.0), 30.0, 12.0, DimLayout.Rank.ACTIVE, false)
        assertEquals(DimLayout.How.CHIP, place(one(chip), DimLayout.Scope.ALL, full).of("A.p")!!.how)
        val small = DimLayout.Mark("s", Box(100.0, 100.0, 125.0, 111.0), listOf(
            DimLayout.Fig("S.w", MarkDims.Role.WIDTH, DimLayout.Seg(100.0, 94.0, 125.0, 94.0), null, 40.0, 10.0, DimLayout.Rank.UNMEASURED, false),
            DimLayout.Fig("S.h", MarkDims.Role.HEIGHT, DimLayout.Seg(131.0, 111.0, 131.0, 100.0), null, 34.0, 10.0, DimLayout.Rank.ACTIVE, false)))
        val sc = DimLayout.place(listOf(small), DimLayout.Scope.ALL, SIZE, SIZE, full, U, DP, TIMES)
        assertEquals(listOf(DimLayout.How.COMPACT, DimLayout.How.COMPACT), sc.plates.map { it.how })
        // not active, it gives way: both of its figures become dots on their own lines
        val gave = DimLayout.place(listOf(DimLayout.Mark("s", small.box, small.figs.map { DimLayout.Fig(it.code, it.role, it.line, null, it.w, it.fs, DimLayout.Rank.UNMEASURED, false) })),
            DimLayout.Scope.ALL, SIZE, SIZE, full, U, DP, TIMES)
        assertEquals(listOf(true, true), gave.plates.map { it.dot })
        assertEquals(listOf(112.5 - 3, 94.0 - 3, 112.5 + 3, 94.0 + 3), l(gave.of("S.w")!!.box))
        assertEquals(listOf(131.0 - 3, 105.5 - 3, 131.0 + 3, 105.5 + 3), l(gave.of("S.h")!!.box))
    }

    @Test fun `a small box gets one compact plate beside it, each figure its own tap half`() {
        val out = place(marks(), DimLayout.Scope.FACE)
        val w = assertNotNull(out.of("FS1.w")); val h = assertNotNull(out.of("FS1.h"))
        assertEquals(DimLayout.How.COMPACT, w.how); assertEquals(DimLayout.How.COMPACT, h.how)
        assertFalse(w.turned); assertFalse(h.turned)                // "≈200 × ≈90", upright
        val whole = assertNotNull(w.whole); assertTrue(same(whole, assertNotNull(h.whole)))
        // 25 x 11 dp on the photo: both spans shorter than their plates and 8 dp
        val p = toPx(switch.x0, switch.y1); val q = toPx(switch.x1, switch.y0)
        assertTrue(q[0] - p[0] < w.box.w + 8 && q[1] - p[1] < h.box.w + 8)
        // right of the box, clear of the h line (6) and 4 more, centred on its height
        assertEquals(q[0] + 10, whole.l, 1e-9); assertEquals((p[1] + q[1]) / 2, (whole.t + whole.b) / 2, 1e-9)
        val ww = DimLayout.plateW(textW("≈200", 10.0, false), U); val hw = DimLayout.plateW(textW("≈90", 10.0, false), U)
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
        val o2 = place(marks(els = listOf(wide)), DimLayout.Scope.FACE, keep(listOf(wide)))
        assertEquals(DimLayout.How.ON_LINE, o2.of("FS2.w")!!.how); assertEquals(DimLayout.How.OFF_LINE, o2.of("FS2.h")!!.how)
        assertTrue(o2.of("FS2.h")!!.turned); assertTrue(o2.of("FS2.h")!!.arrowsIn)
        // ... the other way round too -- narrow and tall: its w steps off its line, its h sits on its own
        val tall = mk("X", 1, 600.0, 750.0, 300.0, 1800.0)
        val o3 = place(marks(els = listOf(tall)), DimLayout.Scope.FACE, keep(listOf(tall)))
        assertEquals(DimLayout.How.OFF_LINE, o3.of("FX1.w")!!.how); assertTrue(o3.of("FX1.w")!!.arrowsIn); assertNull(o3.of("FX1.w")!!.whole)
        assertEquals(DimLayout.How.ON_LINE, o3.of("FX1.h")!!.how); assertTrue(o3.of("FX1.h")!!.turned); assertFalse(o3.of("FX1.h")!!.arrowsIn)
    }

    @Test fun `a p is a chip at the mark's lower-right corner`() {
        // a niche alone in the middle of the wall, picked: w h l s and its depth into the wall
        val niche = mk("N", 1, 800.0, 1700.0, 600.0, 1800.0)
        val ms = marks(picked = niche.id, els = listOf(niche))
        val out = place(ms, DimLayout.Scope.FACE, keep(listOf(niche)))
        sound(out, keep(listOf(niche)), what = "a niche")
        assertEquals(listOf("FN1.w", "FN1.h", "FN1.l", "FN1.s", "FN1.p"), out.plates.map { it.code })
        val c = assertNotNull(out.of("FN1.p"))
        assertEquals(DimLayout.How.CHIP, c.how); assertNull(c.line); assertNull(c.leader); assertFalse(c.turned); assertFalse(c.arrowsIn)
        val q = toPx(niche.x1, niche.y0)
        // outside the corner: 4 right of it and 4 below, "⊥ ?" wide and one plate high
        assertEquals(listOf(q[0] + 4, q[1] + 4), listOf(c.box.l, c.box.t))
        assertEquals(DimLayout.plateW(textW("⊥ ?", 12.0, false), U), c.box.w, 1e-9); assertEquals(19.0, c.box.h, 1e-9)
        // with the corner's outside taken it goes inside the corner; with that taken too, below it; then right of it
        fun chip(vararg taken: Box) = place(ms, DimLayout.Scope.FACE, keep(listOf(niche)) + taken).of("FN1.p")!!
        val below = Box(q[0] + 1, q[1] + 1, q[0] + 60, q[1] + 40); val inside = Box(q[0] - 30, q[1] - 20, q[0] - 1, q[1] - 1)
        val under = Box(q[0] - 30, q[1] + 1, q[0] - 1, q[1] + 40)
        assertEquals(listOf(q[0] - 4, q[1] - 4), chip(below).box.let { listOf(it.r, it.b) })
        assertEquals(listOf(q[0] - 4, q[1] + 4), chip(below, inside).box.let { listOf(it.r, it.t) })
        assertEquals(listOf(q[0] + 4, q[1] - 4), chip(below, inside, under).box.let { listOf(it.l, it.b) })
        for (ch in listOf(chip(below), chip(below, inside), chip(below, inside, under))) assertEquals(DimLayout.How.CHIP, ch.how)
        // no room at any of the four: a dot at the corner
        val d = chip(Box(q[0] - 60, q[1] - 40, q[0] + 60, q[1] + 40))
        assertTrue(d.dot); assertEquals(listOf(q[0] - 3, q[1] - 3, q[0] + 3, q[1] + 3), l(d.box))
        // the door stands on the floor at the photo's bottom edge: its chip stays inside the photo
        val dc = place(marks(picked = door.id), DimLayout.Scope.FACE).of("FD1.p")!!
        assertEquals(DimLayout.How.CHIP, dc.how); assertEquals(SIZE - 2, dc.box.b, 1e-9)
    }

    // ------------------------------------------------------------------ the example wall

    @Test fun `as step 3 Measure opens the example wall shows 8 figures, none a dot`() {
        // the door picked: its four; the window's and the switch plate's size; the three points their spot alone
        val out = place(marks(picked = door.id, active = "FD1.w"), DimLayout.Scope.FACE)
        sound(out, keep(), what = "FACE, door picked")
        assertEquals(listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p", "FW1.w", "FW1.h", "FS1.w", "FS1.h"), out.plates.map { it.code })
        assertEquals(0, out.dots)
        assertEquals(listOf(DimLayout.How.ON_LINE, DimLayout.How.ON_LINE, DimLayout.How.OFF_LINE, DimLayout.How.CHIP, DimLayout.How.ON_LINE, DimLayout.How.ON_LINE,
            DimLayout.How.COMPACT, DimLayout.How.COMPACT), out.plates.map { it.how })
        // nothing picked: 6 -- every box mark its size, no position, no point
        val none = place(marks(), DimLayout.Scope.FACE)
        sound(none, keep(), what = "FACE, nothing picked")
        assertEquals(listOf("FD1.w", "FD1.h", "FW1.w", "FW1.h", "FS1.w", "FS1.h"), none.plates.map { it.code }); assertEquals(0, none.dots)
        // a point picked: its two lines to its centre join the six
        val pt = place(marks(picked = points[1].id), DimLayout.Scope.FACE)
        sound(pt, keep(), what = "FACE, a point picked")
        assertEquals(8, pt.plates.size); assertEquals(0, pt.dots)
        assertEquals(setOf("FE2.cr", "FE2.cs"), pt.plates.map { it.code }.filter { it.startsWith("FE") }.toSet())
        // whichever mark is picked, and whichever of its dimensions is active: 6 to 9 plates, sound, and no dot
        for (e in wall) for (d in listOf<String?>(null) + MarkDims.of("front", e, W, H).map { it.code }) {
            val o = place(marks(picked = e.id, active = d), DimLayout.Scope.FACE)
            sound(o, keep(), what = "FACE, ${e.code} picked, $d active")
            assertTrue(o.plates.size in 6..9, "${e.code}: ${o.plates.size} plates"); assertEquals(0, o.dots, "${e.code} picked, $d active")
        }
    }

    @Test fun `with all on the example wall places all 19, none on another`() {
        val out = place(marks(), DimLayout.Scope.ALL)
        sound(out, keep(), what = "ALL")
        assertEquals(19, out.plates.size)
        assertEquals(wall.flatMap { e -> MarkDims.of("front", e, W, H).map { it.code } }, out.plates.map { it.code })
        // at fit, 340 dp across, figures may give way to a dot: how many is pinned, so a change of the rule shows here
        assertEquals(ALL_DOTS, out.dots, out.plates.filter { it.dot }.joinToString { it.code })
        // with a mark picked and a dimension active, still all 19 and still sound -- and the active one is never the dot
        for (e in wall) for (d in MarkDims.of("front", e, W, H).map { it.code }) {
            val o = place(marks(picked = e.id, active = d), DimLayout.Scope.ALL)
            sound(o, keep(), setOf(d), "ALL, ${e.code} picked, $d active")
            assertEquals(19, o.plates.size); assertFalse(o.of(d)!!.dot, d)
        }
        // readings change the texts and the ranks, never the soundness
        val some = mapOf("FD1.w" to 905.0, "FW1.h" to 1198.0, "FE2.cs" to 300.0, "FS1.w" to 200.0, "FW1.p" to 230.0)
        sound(place(marks(actual = some), DimLayout.Scope.ALL), keep(), what = "ALL, some measured")
    }

    @Test fun `the same input gives the same output`() {
        fun dump(o: DimLayout.Out) = o.plates.map { listOf(it.code, it.how, it.turned, it.arrowsIn, l(it.box), l(it.tap), it.whole?.let(::l), it.leader?.toString()) }
        for (scope in DimLayout.Scope.values()) {
            val a = dump(place(marks(picked = window.id, active = "FW1.s"), scope))
            repeat(3) { assertEquals(a, dump(place(marks(picked = window.id, active = "FW1.s"), scope)), scope.name) }
        }
    }

    // ------------------------------------------------------------------ 2 · Mark: the picked mark keeps its tabs clear

    @Test fun `in step 2 Mark a picked door's w and h stand where widthTag and heightTag put them, clear of its eight tabs`() {
        // the wall without its switch plate, whose code stands just where the door's h tag goes (the last lines here)
        val els = listOf(door, window) + points
        val ms = marks(picked = door.id, active = "FD1.w", tabs = true, els = els)
        val out = place(ms, DimLayout.Scope.PICKED, keep(els))
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
        val face = place(marks(picked = door.id, active = "FD1.w", els = els), DimLayout.Scope.FACE, keep(els))
        assertEquals(DimLayout.How.ON_LINE, face.of("FD1.w")!!.how); assertTrue(face.of("FD1.h")!!.turned)
        // ... and tabs asked for outside 2 · Mark move nothing: only PICKED anchors by hand
        assertEquals(DimLayout.How.ON_LINE, place(ms, DimLayout.Scope.FACE, keep(els)).of("FD1.w")!!.how)
        // on the whole example wall the switch plate's code stands where the h tag goes: the tag gives way as any
        // plate does -- upright, a quarter along its line -- and still covers no tab and no code
        val full = marks(picked = door.id, active = "FD1.w", tabs = true)
        val fo = place(full, DimLayout.Scope.PICKED)
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
        val out = place(ms, DimLayout.Scope.PICKED)
        sound(out, keep(), what = "PICKED, locked door")
        val face = place(marks(picked = locked.id, els = els), DimLayout.Scope.FACE)
        for (k in listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p")) {
            assertEquals(l(face.of(k)!!.box), l(out.of(k)!!.box), k); assertEquals(face.of(k)!!.how, out.of(k)!!.how, k)
        }
        assertEquals(DimLayout.How.ON_LINE, out.of("FD1.w")!!.how); assertTrue(out.of("FD1.h")!!.turned)
        // a picked point: its two figures on its two lines, in either step
        val pm = marks(picked = points[0].id, tabs = true)
        assertFalse(pm[3].tabs)
        val po = place(pm, DimLayout.Scope.PICKED)
        assertEquals(listOf("FE1.cr", "FE1.cs"), po.plates.map { it.code })
        val pf = place(marks(picked = points[0].id), DimLayout.Scope.FACE)
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

    private companion object {
        /** How many of the example wall's 19 figures shrink to a dot with `all` on at fit, nothing picked. */
        const val ALL_DOTS = 2
    }
}
