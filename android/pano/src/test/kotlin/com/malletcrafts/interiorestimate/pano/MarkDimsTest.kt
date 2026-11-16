package com.malletcrafts.interiorestimate.pano

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * A MARK'S DIMENSIONS ON THE PHOTO (port 63 slice M1; DESIGN-port63 §1.1, §1.2): which a mark has, where each one's
 * line runs, its one figure, and the one check that stays. The synthetic room of MeasTest: L 3319, W 2678, H 2677, so
 * the front wall is 2678 x 2677 and the floor and the ceiling 2678 x 3319.
 */
class MarkDimsTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0

    /** One surface of each kind, and its size across and up. */
    private val FACES = listOf(Triple(MarkTypes.WALL, "front", doubleArrayOf(W, H)),
        Triple(MarkTypes.FLOOR, "floor", doubleArrayOf(W, L)), Triple(MarkTypes.CEIL, "ceil", doubleArrayOf(W, L)))

    private fun mark(s: String, t: String, x0: Double, x1: Double, y0: Double, y1: Double, sub: String? = null,
                     extra: Map<String, Any?> = emptyMap(), n: Int = 1) =
        SurveyPrep.Mark("m$t$n", t, SurveyPrep.CODE.getValue(s) + t + n, x0, x1, y0, y1, MarkTypes.withSub(extra, t, s, sub))

    /** Boxes over the face: against each edge and clear of them, small and large, left and right of the midline, low and high. */
    private fun grid(t: String, a: Double, b: Double): List<DoubleArray> {
        val out = ArrayList<DoubleArray>()
        if (SurveyPrep.isPoint(t)) {
            for (cx in listOf(40.0, 700.0, a / 2, a - 320.0, a - 30.0)) for (cy in listOf(30.0, 900.0, b / 2, b - 400.0, b - 25.0))
                out += doubleArrayOf(cx - 25, cx + 25, cy - 25, cy + 25)
            return out
        }
        for (x0 in listOf(0.0, 150.0, 1700.0)) for (w in listOf(200.0, 900.0)) for (y0 in listOf(0.0, 400.0, 1900.0)) for (h in listOf(90.0, 700.0))
            out += doubleArrayOf(x0, x0 + w, y0, y0 + h)
        out += doubleArrayOf(0.0, a, b - 400, b)        // a beam, a loft, a bulkhead: the whole width under the top edge
        out += doubleArrayOf(a - 300, a, 0.0, b)        // a column: the whole height against the right edge
        return out
    }

    /** Every type of every kind of surface, with each of its sub-kinds (null where it has none), over the grid. */
    private fun everyMark(f: (kind: String, s: String, ab: DoubleArray, e: SurveyPrep.Mark) -> Unit) {
        for ((kind, s, ab) in FACES) for (type in SurveyPrep.TYPES.getValue(kind)) {
            val subs: List<String?> = MarkTypes.subsOf(type.letter, s).map { it.id }.ifEmpty { listOf(null) }
            for (sub in subs) for (g in grid(type.letter, ab[0], ab[1])) f(kind, s, ab, mark(s, type.letter, g[0], g[1], g[2], g[3], sub))
        }
    }

    @Test fun `every type and sub-kind on every kind of surface draws exactly what its checklist asks, by key`() {
        val prep = SurveyPrep.fresh(L, W, H)
        var marks = 0; var dims = 0
        everyMark { _, s, ab, e ->
            val ds = MarkDims.of(s, e, ab[0], ab[1]); marks++; dims += ds.size
            val at = "${e.code} ${e.t} on $s at ${e.x0},${e.y0} ${e.x1 - e.x0}x${e.y1 - e.y0}"
            // the same reading keys, in the same order, as the checklist: nothing left out, nothing invented
            assertEquals(prep.keysOf(s, e, ab[0], ab[1]), ds.map { it.code }, at)
            assertEquals(SurveyPrep.measures(s, e, ab[0], ab[1]).map { it.key }, ds.map { it.key }, at)
            val cx = (e.x0 + e.x1) / 2; val cy = (e.y0 + e.y1) / 2
            for (d in ds) {
                // no key of today's types is one this build has no drawing for
                assertTrue(d.known, "$at: ${d.key} has no drawing")
                // a p has no line and is the only thing that has none
                assertEquals(d.role != MarkDims.Role.DEPTH, d.line, "$at: ${d.key}")
                assertEquals(d.key == "p", d.role == MarkDims.Role.DEPTH, "$at: ${d.key}")
                // the line IS the estimate: as long as the figure it carries
                val est = d.est
                if (d.line) assertTrue(est != null && abs(d.length - est) < 1e-6, "$at: ${d.key} line ${d.length} est $est")
                else { assertNull(est, "$at: ${d.key}"); assertEquals(listOf(e.x1, e.y0, e.x1, e.y0), listOf(d.x1, d.y1, d.x2, d.y2), "$at: ${d.key}") }
                // every line is level or plumb on the face
                if (d.line) assertTrue(d.x1 == d.x2 || d.y1 == d.y2, "$at: ${d.key}")
                // a point's two lines end at its centre, one from a side and one from below or above
                if (SurveyPrep.isPoint(e.t)) {
                    assertTrue(d.role == MarkDims.Role.OFF_H || d.role == MarkDims.Role.OFF_V, "$at: ${d.key}")
                    val ends = listOf(listOf(d.x1, d.y1), listOf(d.x2, d.y2))
                    assertTrue(listOf(cx, cy) in ends, "$at: ${d.key} does not end at the centre: $ends")
                }
            }
            if (SurveyPrep.isPoint(e.t)) assertEquals(2, ds.size, at)
            if (MarkLife.isNote(e)) assertEquals(0, ds.size, at)
        }
        // the grid really walked the 21 + 11 + 13 types with their sub-kinds
        assertTrue(marks > 1500 && dims > 5000, "walked $marks marks and $dims dimensions")
    }

    @Test fun `each key's line runs where the design says -- a window, a beam and a plan's unit`() {
        // a window 1200 x 1200 at 300 from the left corner, its sill 900 up
        val w = mark("front", "W", 300.0, 1500.0, 900.0, 2100.0)
        val d = MarkDims.of("front", w, W, H).associateBy { it.key }
        assertEquals(listOf("w", "h", "l", "s", "p"), MarkDims.of("front", w, W, H).map { it.key })
        fun line(k: String) = d.getValue(k).let { listOf(it.x1, it.y1, it.x2, it.y2) }
        assertEquals(listOf(300.0, 2100.0, 1500.0, 2100.0), line("w"))      // along its top edge
        assertEquals(listOf(1500.0, 900.0, 1500.0, 2100.0), line("h"))      // up its right edge
        assertEquals(listOf(0.0, 1500.0, 300.0, 1500.0), line("l"))         // from the left corner, at mid-height
        assertEquals(listOf(900.0, 0.0, 900.0, 900.0), line("s"))           // from the floor up to the sill, at mid-width
        assertEquals(listOf(1500.0, 900.0, 1500.0, 900.0), line("p"))       // anchored at its lower-right corner
        assertEquals(listOf(MarkDims.Role.WIDTH, MarkDims.Role.HEIGHT, MarkDims.Role.OFF_H, MarkDims.Role.OFF_V, MarkDims.Role.DEPTH),
            MarkDims.of("front", w, W, H).map { it.role })
        assertEquals("FW1.s", d.getValue("s").code)
        assertEquals("sill, from floor", d.getValue("s").what(Tx.EN))
        // a beam across the wall, 400 deep: its drop from its underside up to the ceiling; it touches both corners
        val b = mark("front", "B", 0.0, W, H - 400, H)
        val bd = MarkDims.of("front", b, W, H)
        assertEquals(listOf("d", "p"), bd.map { it.key })
        assertEquals(listOf(W / 2, H - 400, W / 2, H), bd[0].let { listOf(it.x1, it.y1, it.x2, it.y2) })
        assertEquals(MarkDims.Role.DROP, bd[0].role)
        // a kitchen platform on the floor, nearer the right wall and the front wall: r from its right edge, f from its top edge
        val u = mark("floor", "CRP", 1500.0, 2500.0, 2600.0, 3200.0, "platform")
        val ud = MarkDims.of("floor", u, W, L).associateBy { it.key }
        assertEquals(setOf("w", "h", "r", "f", "p"), ud.keys)
        assertEquals(listOf(2500.0, 2900.0, W, 2900.0), ud.getValue("r").let { listOf(it.x1, it.y1, it.x2, it.y2) })
        assertEquals(listOf(2000.0, 3200.0, 2000.0, L), ud.getValue("f").let { listOf(it.x1, it.y1, it.x2, it.y2) })
        // a ceiling fan: its own centre keys, to its centre
        val f = mark("ceil", "F", 1000.0, 1050.0, 500.0, 550.0)
        val fd = MarkDims.of("ceil", f, W, L)
        assertEquals(listOf("cl", "ck"), fd.map { it.key })
        assertEquals(listOf(0.0, 525.0, 1025.0, 525.0), fd[0].let { listOf(it.x1, it.y1, it.x2, it.y2) })
        assertEquals(listOf(1025.0, 0.0, 1025.0, 525.0), fd[1].let { listOf(it.x1, it.y1, it.x2, it.y2) })
        // ... and a plan's Electric point keeps l r f k, still to its centre
        val e = mark("floor", "E", 2000.0, 2050.0, 3000.0, 3050.0)
        val ed = MarkDims.of("floor", e, W, L)
        assertEquals(listOf("r", "f"), ed.map { it.key })
        assertEquals(listOf(2025.0, 3025.0, W, 3025.0), ed[0].let { listOf(it.x1, it.y1, it.x2, it.y2) })
        assertEquals(listOf(2025.0, 3025.0, 2025.0, L), ed[1].let { listOf(it.x1, it.y1, it.x2, it.y2) })
    }

    @Test fun `a mark touching the floor has no floor line, and crossing the midline swaps the corner it is measured from`() {
        // geometry decides which measure exists, as it always did: nothing here adds or drops one
        val onFloor = mark("front", "X", 300.0, 900.0, 0.0, 600.0)
        assertEquals(listOf("w", "h", "l"), MarkDims.of("front", onFloor, W, H).map { it.key })
        val door = mark("front", "D", 300.0, 1200.0, 0.0, 2100.0)
        assertEquals(listOf("w", "h", "l", "p"), MarkDims.of("front", door, W, H).map { it.key })
        val left = mark("front", "W", 300.0, 1500.0, 900.0, 2100.0)
        val right = mark("front", "W", 1300.0, 2500.0, 900.0, 2100.0)
        assertEquals("l", MarkDims.of("front", left, W, H)[2].key)
        val r = MarkDims.of("front", right, W, H)[2]
        assertEquals("r", r.key)
        assertEquals(listOf(2500.0, 1500.0, W, 1500.0), listOf(r.x1, r.y1, r.x2, r.y2))
        assertEquals(178.0, r.est)
        // a switch plate is measured from the floor even high on the wall, never from the ceiling
        val sw = mark("front", "S", 150.0, 350.0, 2400.0, 2490.0)
        assertEquals(listOf("w", "h", "l", "s"), MarkDims.of("front", sw, W, H).map { it.key })
    }

    @Test fun `one figure per dimension, and it is the Plan tab's -- a reading, an estimate or a question mark`() {
        val w = mark("front", "W", 300.0, 1503.4, 900.0, 2100.0)
        val actual = mutableMapOf<String, Double>()
        fun v(key: String, est: Double?) = MarkDims.value(actual, "front", w, "FW1.$key", est, W, H)
        // not measured, and the photo can estimate it
        assertEquals("≈1203", v("w", 1203.4).text); assertEquals(MarkDims.State.ESTIMATE, v("w", 1203.4).state)
        // not measured, and the photo cannot show it
        assertEquals("?", v("p", null).text); assertEquals(MarkDims.State.UNKNOWN, v("p", null).state)
        // measured: the reading alone -- never the photo's figure beside it, never a difference
        actual["FW1.w"] = 1198.0; actual["FW1.p"] = 230.0
        assertEquals("1198", v("w", 1203.4).text); assertEquals(MarkDims.State.MEASURED, v("w", 1203.4).state)
        assertEquals("230", v("p", null).text); assertEquals(MarkDims.State.MEASURED, v("p", null).state)
        // ... and for every dimension of every mark it is SurveyPlan.figure's text for the same input, read or not
        everyMark { _, s, ab, e ->
            val some = mutableMapOf<String, Double>()
            val ds = MarkDims.of(s, e, ab[0], ab[1])
            ds.forEachIndexed { i, d -> if (i % 2 == 0) some[d.code] = 1234.0 + i }
            for (d in ds) {
                val got = MarkDims.value(some, s, e, d.code, d.est, ab[0], ab[1])
                val want = SurveyPlan.figure(some, d.code, d.est)
                assertEquals(want.t, got.text, d.code); assertEquals(want.read, got.state == MarkDims.State.MEASURED, d.code)
            }
        }
    }

    @Test fun `the plate prints the figure with its glyphs, in its state's style`() {
        val est = MarkDims.Value("≈640", MarkDims.State.ESTIMATE, false)
        val read = MarkDims.Value("1198", MarkDims.State.MEASURED, true)
        val unk = MarkDims.Value("?", MarkDims.State.UNKNOWN, false)
        assertEquals("≈640", MarkDims.shown(est, MarkDims.Role.OFF_H, false))
        assertEquals("▶≈640", MarkDims.shown(est, MarkDims.Role.OFF_H, true))
        assertEquals("⊥ ?", MarkDims.shown(unk, MarkDims.Role.DEPTH, false))
        assertEquals("▶⊥ ?", MarkDims.shown(unk, MarkDims.Role.DEPTH, true))
        assertEquals("⊥ 230", MarkDims.shown(MarkDims.Value("230", MarkDims.State.MEASURED, false), MarkDims.Role.DEPTH, false))
        assertEquals("1198", MarkDims.shown(read, MarkDims.Role.WIDTH, false))
        assertEquals("!", MarkDims.CHECK_GLYPH); assertEquals("✓", MarkDims.DONE_GLYPH)
        fun st(s: MarkDims.State, on: Boolean) = MarkDims.style(s, on).let { listOf(it.ink, it.bold, it.fill, it.fillAlpha, it.outline, it.dashed) }
        // measured: white bold on solid black; an estimate grey on half-black, dashed grey; a ? amber, dashed amber
        assertEquals(listOf(0xffffff, true, 0x000000, 0xdd, null, false), st(MarkDims.State.MEASURED, false))
        assertEquals(listOf(0xcfd3cb, false, 0x000000, 0x88, 0xcfd3cb, true), st(MarkDims.State.ESTIMATE, false))
        assertEquals(listOf(0xffc46b, false, 0x000000, 0x88, 0xffc46b, true), st(MarkDims.State.UNKNOWN, false))
        // the one being measured now: the gold plate with the gold ring, as 0.3.332 draws it
        assertEquals(listOf(0xcfd3cb, false, 0x4a3d0c, 0xff, 0xe0b040, false), st(MarkDims.State.ESTIMATE, true))
        assertEquals(listOf(0xffffff, true, 0x4a3d0c, 0xff, 0xe0b040, false), st(MarkDims.State.MEASURED, true))
        assertEquals(0xffffff, MarkDims.LINE_COLOUR); assertEquals(0xcc, MarkDims.LINE_ALPHA); assertEquals(0xffc46b, MarkDims.CHECK_COLOUR)
    }

    // ------------------------------------------------------------------ the one check that stays

    @Test fun `the check fires past the larger of 150 mm and 15 percent, and never without both figures`() {
        assertEquals(150.0, MarkDims.CHECK_MM); assertEquals(0.15, MarkDims.CHECK_PART)
        // a 900 span: 15 % is 135, so 150 mm decides
        assertFalse(MarkDims.check(900.0 + 149, 900.0, true)); assertTrue(MarkDims.check(900.0 + 151, 900.0, true))
        assertFalse(MarkDims.check(900.0 - 149, 900.0, true)); assertTrue(MarkDims.check(900.0 - 151, 900.0, true))
        assertFalse(MarkDims.check(900.0 + 150, 900.0, true))          // exactly the threshold is not past it
        // a 2678 span: 15 % is 401.7, which decides
        assertFalse(MarkDims.check(2678.0 + 401, 2678.0, true)); assertTrue(MarkDims.check(2678.0 + 402, 2678.0, true))
        assertFalse(MarkDims.check(2678.0 - 401, 2678.0, true)); assertTrue(MarkDims.check(2678.0 - 402, 2678.0, true))
        // never without a reading, never without an estimate
        assertFalse(MarkDims.check(null, 900.0, true)); assertFalse(MarkDims.check(5000.0, null, true)); assertFalse(MarkDims.check(null, null, true))
        // and never on a mark that stands off the plane, however far apart the two are
        assertFalse(MarkDims.check(5000.0, 900.0, false))
    }

    @Test fun `a window's reading far from the photo carries the check, on its size and on its position`() {
        val w = mark("front", "W", 300.0, 1500.0, 900.0, 2100.0)
        assertTrue(MarkDims.inPlane("front", w, W, H))
        val actual = mutableMapOf("FW1.w" to 1400.0, "FW1.h" to 1210.0, "FW1.l" to 640.0, "FW1.p" to 230.0)
        fun v(k: String) = MarkDims.of("front", w, W, H).first { it.key == k }.let { MarkDims.value(actual, "front", w, it.code, it.est, W, H) }
        assertTrue(v("w").check)        // 1400 against ≈1200: 200 > max(150, 180)
        assertFalse(v("h").check)       // 1210 against ≈1200
        assertTrue(v("l").check)        // 640 against ≈300: a shot at the wrong target
        assertFalse(v("s").check)       // not measured
        assertFalse(v("p").check)       // measured, and the photo has no figure to check it against
        // computed from the box as it is NOW: move the mark and the same reading is checked against the new estimate
        val moved = mark("front", "W", 640.0, 1840.0, 900.0, 2100.0)
        val lm = MarkDims.of("front", moved, W, H).first { it.key == "l" }
        assertFalse(MarkDims.value(actual, "front", moved, lm.code, lm.est, W, H).check)
    }

    @Test fun `every type that asks a p is classified, in the wall's plane or standing off it`() {
        val asksP = LinkedHashMap<String, MutableSet<String>>(); val noP = LinkedHashMap<String, MutableSet<String>>()
        everyMark { kind, s, ab, e ->
            val p = SurveyPrep.measures(s, e, ab[0], ab[1]).any { it.key == "p" }
            (if (p) asksP else noP).getOrPut(kind) { sortedSetOf() }.add(e.t)
            val want = !p || (kind == MarkTypes.WALL && e.t in setOf("D", "W", "O", "N"))
            assertEquals(want, MarkDims.inPlane(s, e, ab[0], ab[1]), "${e.t} on $kind")
        }
        // a type asks its p whatever its box or sub-kind: none is in both lists
        for (k in MarkTypes.KINDS) assertEquals(emptySet<String>(), asksP.getValue(k).intersect(noP.getValue(k)), k)
        // BY NAME: the four whose p goes INTO the wall, and the ones that stand off the squared plane. A type added
        // later with a p is in neither list, and this turns red until it is put in one.
        val into = setOf("D", "W", "O", "N")
        val off = mapOf(MarkTypes.WALL to setOf("C", "B", "L", "GRL", "AC", "CTR", "CRP", "SAN"),
            MarkTypes.FLOOR to setOf("S", "CRP", "SAN"), MarkTypes.CEIL to setOf("B", "AC", "CDP"))
        assertEquals(into + off.getValue(MarkTypes.WALL), asksP.getValue(MarkTypes.WALL).toSet())
        assertEquals(off.getValue(MarkTypes.FLOOR), asksP.getValue(MarkTypes.FLOOR).toSet())
        assertEquals(off.getValue(MarkTypes.CEIL), asksP.getValue(MarkTypes.CEIL).toSet())
        assertEquals(into, MarkDims.INTO_WALL); assertEquals(off, MarkDims.STAND_OFF)
        assertEquals(emptySet<String>(), MarkDims.INTO_WALL.intersect(MarkDims.STAND_OFF.getValue(MarkTypes.WALL)))
        // every type of every kind was walked: 21 on a wall, 11 on the floor, 13 on the ceiling
        for ((k, n) in listOf(MarkTypes.WALL to 21, MarkTypes.FLOOR to 11, MarkTypes.CEIL to 13))
            assertEquals(n, (asksP.getValue(k) + noP.getValue(k)).size, k)
    }

    @Test fun `a wardrobe that reads nearly twice its width on the photo carries no check, on any of its keys`() {
        // 600 deep on the wall of a 2.7 m room, seen from the middle: the squared photo shows it about 1.8 times as wide
        val u = mark("front", "CRP", 150.0, 150.0 + 2160.0, 0.0, 2100.0, "wardrobe")
        assertFalse(MarkDims.inPlane("front", u, W, H))
        val ds = MarkDims.of("front", u, W, H)
        assertEquals(listOf("w", "h", "l", "p"), ds.map { it.key })
        for (factor in listOf(1 / 1.8, 1.8, 0.2, 5.0)) {
            val actual = ds.associate { it.code to (it.est ?: 600.0) * factor }
            for (d in ds) assertFalse(MarkDims.value(actual, "front", u, d.code, d.est, W, H).check, "${d.key} x $factor")
        }
        // the same on the floor's bed and the ceiling's beam; and the same readings on a window DO carry it
        val bed = mark("floor", "CRP", 400.0, 2200.0, 500.0, 2500.0, "bed")
        val beam = mark("ceil", "B", 1200.0, 1430.0, 0.0, L)
        for ((s, e, ab) in listOf(Triple("floor", bed, doubleArrayOf(W, L)), Triple("ceil", beam, doubleArrayOf(W, L)))) {
            assertFalse(MarkDims.inPlane(s, e, ab[0], ab[1]), e.code)
            val es = MarkDims.of(s, e, ab[0], ab[1]); val actual = es.associate { it.code to (it.est ?: 450.0) * 0.5 }
            for (d in es) assertFalse(MarkDims.value(actual, s, e, d.code, d.est, ab[0], ab[1]).check, d.code)
        }
        val win = mark("front", "W", 300.0, 1500.0, 900.0, 2100.0)
        val ws = MarkDims.of("front", win, W, H); val half = ws.associate { it.code to (it.est ?: 450.0) * 0.5 }
        assertEquals(listOf(true, true, false, true, false), ws.map { MarkDims.value(half, "front", win, it.code, it.est, W, H).check })
    }

    // ------------------------------------------------------------------ a mark with four free corners (port 63 slice Q1)

    /** Three shapes in the box [g]: a trapezoid (a sloping bottom), a lean (level top and bottom, no square corner) and
     *  one with no straight side. Null where the box is too small to hold them. */
    private fun shapes(g: DoubleArray): List<List<DoubleArray>> {
        val x0 = g[0]; val x1 = g[1]; val y0 = g[2]; val y1 = g[3]; val w = x1 - x0; val h = y1 - y0
        fun p(x: Double, y: Double) = doubleArrayOf(x, y)
        return listOf(
            listOf(p(x0, y0 + h * 0.4), p(x1, y0), p(x1, y1), p(x0, y1)),
            listOf(p(x0, y0), p(x1 - w * 0.2, y0), p(x1, y1), p(x0 + w * 0.2, y1)),
            listOf(p(x0, y0 + h * 0.25), p(x1 - w * 0.15, y0), p(x1, y1 - h * 0.2), p(x0 + w * 0.2, y1)))
    }

    @Test fun `no key of a four-corner mark is undrawn -- every box type and sub-kind on every kind of surface, by key`() {
        val prep = SurveyPrep.fresh(L, W, H)
        var marks = 0; var sides = 0; var diags = 0
        everyMark { _, s, ab, r ->
            if (!Quad.mayHave(r)) { assertFalse(Quad.set(r.copy(), shapes(doubleArrayOf(r.x0, r.x1, r.y0, r.y1))[0]), "${r.t} is a spot"); return@everyMark }
            for (mm in shapes(doubleArrayOf(r.x0, r.x1, r.y0, r.y1))) {
                val e = r.copy()
                if (!Quad.set(e, mm)) continue          // the grid's 90 mm high boxes cannot hold a shape 20 mm clear of itself
                if (Quad.of(e) == null) continue
                marks++
                val at = "${e.code} ${e.t} on $s at ${e.x0},${e.y0} ${e.x1 - e.x0}x${e.y1 - e.y0}"
                val ds = MarkDims.of(s, e, ab[0], ab[1]); val c = Quad.corners(e)
                // the same reading keys, in the same order, as the checklist: nothing left out, nothing invented
                assertEquals(prep.keysOf(s, e, ab[0], ab[1]), ds.map { it.code }, at)
                assertEquals(listOf("bottom", "right", "top", "left"), ds.take(4).map { it.key }, at)
                assertTrue(ds.none { it.key == "w" || it.key == "h" }, at)
                val cx = c.map { it[0] }.average(); val cy = c.map { it[1] }.average()
                for (d in ds) {
                    assertTrue(d.known, "$at: ${d.key} has no drawing")
                    assertEquals(d.role != MarkDims.Role.DEPTH, d.line, "$at: ${d.key}")
                    assertEquals(d.key == "p", d.role == MarkDims.Role.DEPTH, "$at: ${d.key}")
                    val est = d.est
                    // the line IS the estimate, for a side, the diagonal and every position line alike
                    if (d.line) assertTrue(est != null && abs(d.length - est) < 1e-6, "$at: ${d.key} line ${d.length} est $est")
                    when (d.role) {
                        MarkDims.Role.SIDE -> {
                            sides++
                            val i = Quad.SIDES.indexOf(d.key)
                            // from corner i to corner i + 1, with a unit normal that points OUT of the shape
                            assertEquals(listOf(c[i][0], c[i][1], c[(i + 1) % 4][0], c[(i + 1) % 4][1]), listOf(d.x1, d.y1, d.x2, d.y2), "$at: ${d.key}")
                            assertEquals(1.0, d.nx * d.nx + d.ny * d.ny, 1e-9, "$at: ${d.key}")
                            assertEquals(0.0, d.nx * (d.x2 - d.x1) + d.ny * (d.y2 - d.y1), 1e-6, "$at: ${d.key} is square to its side")
                            val mx = (d.x1 + d.x2) / 2; val my = (d.y1 + d.y2) / 2
                            assertTrue((mx - cx) * d.nx + (my - cy) * d.ny > 0, "$at: ${d.key}'s normal points into the shape")
                        }
                        MarkDims.Role.DIAG -> { diags++; assertEquals(listOf(c[0][0], c[0][1], c[2][0], c[2][1]), listOf(d.x1, d.y1, d.x2, d.y2), at); assertEquals("diag", d.key) }
                        // a position line runs level or plumb from the wall, the floor or the ceiling TO THE BOX'S EDGE, where
                        // a corner of the mark is -- one corner, or the midpoint of the two on a straight side
                        MarkDims.Role.OFF_H, MarkDims.Role.OFF_V, MarkDims.Role.DROP -> {
                            assertTrue(d.x1 == d.x2 || d.y1 == d.y2, "$at: ${d.key}")
                            val end = if (d.key == "l" || d.key == "s" || d.key == "k") listOf(d.x2, d.y2) else listOf(d.x1, d.y1)
                            val on = c.map { it.toList() } + (0 until 4).map { i -> listOf((c[i][0] + c[(i + 1) % 4][0]) / 2, (c[i][1] + c[(i + 1) % 4][1]) / 2) }
                            assertTrue(on.any { abs(it[0] - end[0]) < 1e-9 && abs(it[1] - end[1]) < 1e-9 }, "$at: ${d.key} ends at $end, not at a corner of $on")
                            assertEquals(0.0, d.nx); assertEquals(0.0, d.ny)
                        }
                        // the p chip hangs at q1, the bottom-right corner
                        MarkDims.Role.DEPTH -> assertEquals(listOf(c[1][0], c[1][1], c[1][0], c[1][1]), listOf(d.x1, d.y1, d.x2, d.y2), "$at: p")
                        else -> assertTrue(false, "$at: a four-corner mark has no ${d.role}")
                    }
                }
                // one figure per dimension, by the Plan tab's rule, for the new keys as for the old
                for (d in ds) assertEquals(SurveyPlan.figure(emptyMap(), d.code, d.est).t, MarkDims.value(emptyMap(), s, e, d.code, d.est, ab[0], ab[1]).text, d.code)
            }
        }
        assertTrue(marks > 2000 && sides == 4 * marks && diags > 500, "walked $marks shapes, $sides sides and $diags diagonals")
    }

    @Test fun `the example's seven lines -- each side corner to corner, the offsets to the corner, the chip at q1`() {
        // the design's own: the front wall 3600 x 2700; FW1 with uprights 1550 and 780, a level top of 2370, a sloping bottom
        val a = 3600.0; val b = 2700.0
        val e = SurveyPrep.Mark("w", "W", "FW1", 600.0, 2970.0, 900.0, 2450.0)
        assertTrue(Quad.set(e, listOf(doubleArrayOf(600.0, 1670.0), doubleArrayOf(2970.0, 900.0), doubleArrayOf(2970.0, 2450.0), doubleArrayOf(600.0, 2450.0))))
        val ds = MarkDims.of("front", e, a, b); val d = ds.associateBy { it.key }
        assertEquals(listOf("bottom", "right", "top", "left", "l", "s", "p"), ds.map { it.key })
        assertEquals(listOf(MarkDims.Role.SIDE, MarkDims.Role.SIDE, MarkDims.Role.SIDE, MarkDims.Role.SIDE, MarkDims.Role.OFF_H, MarkDims.Role.OFF_V, MarkDims.Role.DEPTH), ds.map { it.role })
        fun line(k: String) = d.getValue(k).let { listOf(it.x1, it.y1, it.x2, it.y2).map { v -> Math.round(v * 10) / 10.0 } }
        assertEquals(listOf(600.0, 1670.0, 2970.0, 900.0), line("bottom"))       // the true sloping side
        assertEquals(listOf(2970.0, 900.0, 2970.0, 2450.0), line("right")); assertEquals(listOf(2970.0, 2450.0, 600.0, 2450.0), line("top"))
        assertEquals(listOf(600.0, 2450.0, 600.0, 1670.0), line("left"))
        // out of the shape: the right side's normal points right, the top's up, the left's left, the bottom's down and to the left
        assertEquals(listOf(1.0, 0.0), d.getValue("right").let { listOf(it.nx, it.ny) }); assertEquals(listOf(0.0, 1.0), d.getValue("top").let { listOf(it.nx, it.ny) })
        assertEquals(listOf(-1.0, 0.0), d.getValue("left").let { listOf(it.nx, it.ny) })
        d.getValue("bottom").let { assertEquals(-770.0 / 2491.95, it.nx, 1e-4); assertEquals(-2370.0 / 2491.95, it.ny, 1e-4) }
        // from the left corner to the left side, which is upright: at the midpoint of its two corners, as a rectangle's
        assertEquals(listOf(0.0, 2060.0, 600.0, 2060.0), line("l"))
        // the sill: from the floor up to its LOW corner, q1 -- plumb under it
        assertEquals(listOf(2970.0, 0.0, 2970.0, 900.0), line("s"))
        // the reveal's chip hangs at q1
        assertEquals(listOf(2970.0, 900.0, 2970.0, 900.0), line("p")); assertFalse(d.getValue("p").line)
        assertEquals(listOf("≈2492", "≈1550", "≈2370", "≈780", "≈600", "≈900", "?"), ds.map { MarkDims.value(emptyMap(), "front", e, it.code, it.est, a, b).text })
        // THE `!` OF THE CLOSING CHECK is on the sloping side's plate alone: 2495, 1550, 2370 and a left upright typed 870
        val read = mutableMapOf("FW1.bottom" to 2495.0, "FW1.right" to 1550.0, "FW1.top" to 2370.0, "FW1.left" to 780.0)
        fun checks() = ds.map { MarkDims.value(read, "front", e, it.code, it.est, a, b).check }
        assertEquals(List(7) { false }, checks())
        read["FW1.left"] = 870.0
        assertEquals(listOf(true, false, false, false, false, false, false), checks())
        // ... which the photo's own check does not raise: 2495 is 3 mm from the photo's 2492
        assertFalse(MarkDims.photoCheck(read, "front", e, "FW1.bottom", ds[0].est, a, b))
        // the photo check still runs per figure: a right upright read 1900 against the photo's 1550
        read["FW1.left"] = 780.0; read["FW1.right"] = 1900.0
        assertTrue(MarkDims.photoCheck(read, "front", e, "FW1.right", ds[1].est, a, b))
        assertEquals(listOf(true, true, false, false, false, false, false), checks(), "the bottom for the closing check, the right for the photo's")
        // done once all seven are read
        val prep = SurveyPrep.fresh(5000.0, a, b); prep.surface("front").marks.add(e)
        for (k in listOf("bottom", "right", "top", "left", "l", "s")) prep.actual["FW1.$k"] = 1000.0
        assertFalse(MarkDims.done(prep, "front", e, a, b)); prep.actual["FW1.p"] = 230.0; assertTrue(MarkDims.done(prep, "front", e, a, b))
        // a w or an h reading it kept from its time as a rectangle is not asked, shown or counted -- and is still there
        prep.actual["FW1.w"] = 2370.0
        assertTrue("FW1.w" !in prep.keysOf("front", e, a, b)); assertEquals(2370.0, prep.actual["FW1.w"])
    }

    // ------------------------------------------------------------------ who is asked, and when a mark is done

    @Test fun `only a mark in the room is asked -- not one born a wish, not a vendor's, not a Note`() {
        val door = mark("front", "D", 300.0, 1200.0, 0.0, 2100.0)
        assertTrue(MarkDims.asked(door))
        assertTrue(MarkDims.asked(mark("front", "D", 300.0, 1200.0, 0.0, 2100.0, extra = mapOf(MarkLife.KEY_BORN to MarkLife.EXIST))))
        assertFalse(MarkDims.asked(mark("front", "D", 300.0, 1200.0, 0.0, 2100.0, extra = mapOf(MarkLife.KEY_BORN to MarkLife.WISH))))
        assertFalse(MarkDims.asked(mark("front", "D", 300.0, 1200.0, 0.0, 2100.0, extra = mapOf(MarkLife.KEY_BORN to MarkLife.VADD))))
        assertFalse(MarkDims.asked(mark("front", MarkTypes.NOTE, 300.0, 350.0, 900.0, 950.0)))
        // a surveyed mark keeps being asked whatever its state: a window the client wants replaced is still there
        assertTrue(MarkDims.asked(mark("front", "W", 300.0, 1500.0, 900.0, 2100.0, extra = mapOf(MarkLife.KEY_LIFE to MarkLife.WISH))))
        // a locked mark is still measured: a lock stops moving, not reading
        assertTrue(MarkDims.asked(mark("front", "D", 300.0, 1200.0, 0.0, 2100.0, extra = mapOf(SurveyPrep.LOCKED to true))))
    }

    @Test fun `a mark is done once every key it asks has a reading`() {
        val prep = SurveyPrep.fresh(L, W, H)
        val door = mark("front", "D", 300.0, 1200.0, 0.0, 2100.0)
        assertFalse(MarkDims.done(prep, "front", door, W, H))
        for (k in listOf("FD1.w", "FD1.h", "FD1.l")) prep.actual[k] = 900.0
        assertFalse(MarkDims.done(prep, "front", door, W, H))       // its reveal depth is still to take
        prep.actual["FD1.p"] = 230.0
        assertTrue(MarkDims.done(prep, "front", door, W, H))
        // a Note asks nothing and is never ticked; a wish-born door is never asked, whatever an earlier build stored for it
        assertFalse(MarkDims.done(prep, "front", mark("front", MarkTypes.NOTE, 300.0, 350.0, 900.0, 950.0), W, H))
        assertFalse(MarkDims.done(prep, "front", mark("front", "D", 300.0, 1200.0, 0.0, 2100.0, extra = mapOf(MarkLife.KEY_BORN to MarkLife.WISH)), W, H))
    }
}
