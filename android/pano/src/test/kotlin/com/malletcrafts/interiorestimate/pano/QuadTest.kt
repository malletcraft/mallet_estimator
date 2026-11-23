package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * A MARK WITH FOUR FREE CORNERS, AS PURE MODELS (port 63 slice Q1; android/docs/port63/DESIGN-quad.md §1, §2, §3.1-3.3).
 * Amit, 16 Nov 2026: "while marking window, I realised that it can draw only rectangular windows. there are scenarios
 * when i need to draw trapezoidal windows as well like one just exported." -- and, on the selector, "Four corners".
 *
 * THE EXAMPLE, all through, is the design's own and made up (never a job's figures): the front wall, 3600 x 2700 mm, of
 * project PROJ-T1; window FW1 with uprights 1550 (right) and 780 (left), a level top of 2370, so a sloping bottom.
 * English first (Amit, 16 Nov 2026): the new texts are pinned in English only.
 */
class QuadTest {

    private val A = 3600.0; private val B = 2700.0; private val L = 5000.0
    private val en = Tx.EN

    /** FW1's four corners, mm: bottom-left, bottom-right, top-right, top-left. */
    private val MM = listOf(doubleArrayOf(600.0, 1670.0), doubleArrayOf(2970.0, 900.0), doubleArrayOf(2970.0, 2450.0), doubleArrayOf(600.0, 2450.0))
    /** ... and as the record keeps them: parts of its box, 600 … 2970 across and 900 … 2450 up. */
    private val PARTS = listOf(listOf(0.0, 0.496774), listOf(1.0, 0.0), listOf(1.0, 1.0), listOf(0.0, 1.0))
    private val BOX = listOf(600.0, 2970.0, 900.0, 2450.0)

    private fun mark(t: String = "W", code: String = "FW1", x0: Double = 600.0, x1: Double = 2970.0, y0: Double = 900.0, y1: Double = 2450.0,
                     extra: Map<String, Any?> = emptyMap()) = SurveyPrep.Mark("id-$code", t, code, x0, x1, y0, y1, extra)

    private fun window(): SurveyPrep.Mark = mark().also { assertTrue(Quad.set(it, MM)) }
    private fun box(e: SurveyPrep.Mark) = listOf(e.x0, e.x1, e.y0, e.y1)
    private fun r1(v: Double) = Math.round(v * 10) / 10.0
    /** A mark's corners, each to the 0.1 mm a corner is placed to. */
    private fun mm(e: SurveyPrep.Mark) = Quad.corners(e).map { listOf(r1(it[0]), r1(it[1])) }
    private fun pts(vararg p: Pair<Double, Double>) = p.map { doubleArrayOf(it.first, it.second) }
    private fun rectOf(x0: Double, x1: Double, y0: Double, y1: Double) = pts(x0 to y0, x1 to y0, x1 to y1, x0 to y1)

    // ------------------------------------------------------------------ the record

    @Test fun `the record -- one key, the four corners as parts of the box, stored only when the shape is not its box`() {
        assertEquals("quad", Quad.KEY); assertEquals(listOf("q0", "q1", "q2", "q3"), Quad.PARTS)
        assertEquals(listOf("bottom", "right", "top", "left"), Quad.SIDES); assertEquals("diag", Quad.DIAG)
        val e = mark(); val before = e.extra
        // a rectangle has no key, and its corners are its box's four, bottom-left first, counter-clockwise
        assertNull(Quad.of(e)); assertEquals(listOf(listOf(600.0, 900.0), listOf(2970.0, 900.0), listOf(2970.0, 2450.0), listOf(600.0, 2450.0)), mm(e))
        assertTrue(Quad.set(e, MM))
        // the design's own record, to the last digit: the box is the shape's bounding box, the corners parts of it
        assertEquals(BOX, box(e)); assertEquals(PARTS, e.extra[Quad.KEY]); assertEquals(setOf("quad"), e.extra.keys)
        assertEquals(PARTS, assertNotNull(Quad.of(e)).map { it.toList() })
        assertEquals(MM.map { it.toList() }, mm(e))
        // the map is REPLACED, never changed in place: an undo copy that shares the old one still holds a rectangle
        assertTrue(before.isEmpty()); assertTrue(e.extra !== before)
        // a corner on a box edge is exactly on it; the one off it under 0.01 mm from where it was put
        val c = Quad.corners(e)
        assertEquals(listOf(600.0, 2970.0, 2970.0, 600.0), c.map { it[0] }); assertEquals(listOf(900.0, 2450.0, 2450.0), c.drop(1).map { it[1] })
        assertTrue(abs(c[0][1] - 1670.0) < 0.01, "${c[0][1]}")
        // ... also where x0 + 1 x (x1 - x0) is not x1 in floating point
        val odd = mark(x0 = 0.1, x1 = 0.3, y0 = 0.1, y1 = 0.7, extra = mapOf(Quad.KEY to PARTS))
        assertEquals(listOf(0.1, 0.3, 0.3, 0.1), Quad.corners(odd).map { it[0] }); assertEquals(listOf(0.7, 0.7), Quad.corners(odd).drop(2).map { it[1] })
        // the shape put back to its box: the key is REMOVED, never written as [[0,0],[1,0],[1,1],[0,1]]
        val held = e.extra
        assertTrue(Quad.set(e, rectOf(600.0, 2970.0, 900.0, 2450.0)))
        assertFalse(Quad.KEY in e.extra); assertTrue(Quad.KEY in held); assertEquals(BOX, box(e))
        // a rectangle given its own four corners gains nothing
        val r = mark(extra = mapOf(SurveyPrep.LOCKED to false)); assertTrue(Quad.set(r, rectOf(600.0, 2970.0, 900.0, 2450.0)))
        assertEquals(mapOf<String, Any?>(SurveyPrep.LOCKED to false), r.extra)
        // corners are placed to 0.1 mm, as a Line's end is; the parts to 6 decimals
        val fine = mark(); assertTrue(Quad.set(fine, pts(600.04 to 1670.04, 2970.04 to 900.04, 2969.96 to 2450.04, 599.96 to 2449.96)))
        assertEquals(BOX, box(fine)); assertEquals(PARTS, fine.extra[Quad.KEY])
        // the other keys of the mark stay, each where it was, and the new one goes after them
        val kept = mark(extra = linkedMapOf("sub" to "bay", SurveyPrep.LOCKED to false)); assertTrue(Quad.set(kept, MM))
        assertEquals(listOf("sub", SurveyPrep.LOCKED, "quad"), kept.extra.keys.toList())
        // Rectangle: the key goes, in a new map; the box does not move
        val w = window(); val was = w.extra
        assertTrue(Quad.clear(w)); assertFalse(Quad.KEY in w.extra); assertTrue(Quad.KEY in was); assertEquals(BOX, box(w))
        assertFalse(Quad.clear(w), "nothing to remove on a rectangle"); assertFalse(Quad.clear(mark()))
    }

    @Test fun `the file -- a four-corner mark is written with its quad after its box and reads back the same, a rectangle with none`() {
        val p = SurveyPrep.fresh(L, A, B)
        p.surface("front").marks.add(window()); p.surface("front").marks.add(mark("D", "FD1", 100.0, 500.0, 0.0, 2100.0))
        p.actual["FW1.left"] = 780.0
        val o = SurveyPrepFile.encode(p, L, A, B)
        @Suppress("UNCHECKED_CAST") fun els(f: Map<String, Any?>) = ((f["w"] as Map<String, Any?>)["front"] as Map<String, Any?>)["els"] as List<Map<String, Any?>>
        // MARK_KEYS did not grow: the key is one of the kept keys, written after the seven the file has always had
        assertEquals(setOf("id", "t", "code", "x0", "x1", "y0", "y1"), SurveyPrepFile.MARK_KEYS)
        assertEquals(listOf("id", "t", "code", "x0", "x1", "y0", "y1", "quad"), els(o)[0].keys.toList()); assertEquals(PARTS, els(o)[0]["quad"])
        assertEquals(listOf("id", "t", "code", "x0", "x1", "y0", "y1"), els(o)[1].keys.toList())
        assertEquals(BOX, listOf("x0", "x1", "y0", "y1").map { els(o)[0][it] })
        val d = assertNotNull(SurveyPrepFile.decode(o))
        assertTrue(d.problems.isEmpty(), d.problems.map { it(en) }.toString())
        val back = d.prep.surface("front").marks
        assertEquals(PARTS, assertNotNull(Quad.of(back[0])).map { it.toList() }); assertNull(Quad.of(back[1]))
        assertEquals(o, SurveyPrepFile.encode(d.prep, L, A, B))
        // undo, Surface.copy and a prep copy carry it: they share the map, which is only ever replaced
        val copy = p.copy().surface("front").marks[0]
        assertSame(p.surface("front").marks[0].extra, copy.extra); assertEquals(mm(p.surface("front").marks[0]), mm(copy))
    }

    // ------------------------------------------------------------------ when a quad can be read

    private fun q(vararg p: Pair<Any?, Any?>): List<List<Any?>> = p.map { listOf(it.first, it.second) }

    @Test fun `when a quad can be read -- the five tests, one at a time, on the parts alone`() {
        // the example reads, as numbers of any JSON kind: whole numbers arrive as integers
        assertNotNull(Quad.read(PARTS))
        assertNotNull(Quad.read(q(0 to 0.496774, 1 to 0, 1L to 1L, 0 to 1)))
        // 1. exactly four items, each exactly two FINITE NUMBERS from 0 to 1 -- never text that looks like one
        for (bad in listOf<Any?>(null, "quad", 4, mapOf("q" to PARTS), PARTS.take(3), PARTS + listOf(listOf(0.5, 0.5)),
            listOf(listOf(0.0, 0.5), listOf(1.0, 0.0), listOf(1.0, 1.0), "0,1"), listOf(listOf(0.0, 0.5), listOf(1.0, 0.0), listOf(1.0, 1.0), listOf(0.0, 1.0, 0.0)),
            listOf(listOf(0.0, 0.5), listOf(1.0, 0.0), listOf(1.0, 1.0), listOf(0.0)),
            q("0" to "0.5", "1" to "0", "1" to "1", "0" to "1"), q(0 to "0.5", 1 to 0, 1 to 1, 0 to 1),
            q(0 to 0.5, 1 to 0, 1 to 1, 0 to null), q(0 to 0.5, 1 to 0, 1 to 1, 0 to true),
            q(0 to 0.5, 1 to 0, 1 to Double.NaN, 0 to 1), q(0 to 0.5, 1 to 0, 1 to Double.POSITIVE_INFINITY, 0 to 1),
            q(-0.1 to 0.5, 1 to 0, 1 to 1, 0 to 1), q(0 to 0.5, 1.1 to 0, 1 to 1, 0 to 1), q(0 to 0.5, 1 to 0, 7 to 1, 0 to 1)))
            assertNull(Quad.read(bad), "rule 1: $bad")
        // the numeric strings of the plan critic's finding 7, by name: the phone's numOf would have coerced them
        assertNull(Quad.read(q("0" to "0", "1" to "0", "1" to "1", "0" to "1"))); assertFalse(Quad.identity(q("0" to "0", "1" to "0", "1" to "1", "0" to "1")))
        assertNull(Quad.read(q("0" to "0.5", "1" to "0", "1" to "1", "0" to "1")))
        // 2. it spans its box: the least px 0 and the greatest 1, the least py 0 and the greatest 1
        for (bad in listOf(q(0.1 to 0.5, 1 to 0, 1 to 1, 0.1 to 1), q(0 to 0.5, 0.9 to 0, 0.9 to 1, 0 to 1), q(0 to 0.5, 1 to 0.1, 1 to 1, 0 to 1), q(0 to 0.5, 1 to 0, 1 to 0.9, 0 to 0.9)))
            assertNull(Quad.read(bad), "rule 2: $bad")
        // 3. each side keeps its direction: the bottom and the top run left to right, the left and the right upward
        for (bad in listOf(q(1 to 0, 0 to 0, 0 to 1, 1 to 1), q(0 to 1, 1 to 1, 1 to 0, 0 to 0), q(0 to 0, 0 to 1, 1 to 1, 1 to 0),
            q(0.5 to 0, 0.5 to 0.2, 1 to 1, 0 to 1), q(0 to 0, 1 to 0.5, 1 to 0.5, 0 to 1), q(0 to 0, 1 to 1, 1 to 0, 0 to 1)))
            assertNull(Quad.read(bad), "rule 3: $bad")
        // 4. convex and uncrossed: a corner turned inward, and three corners in one line, are not shapes -- though each
        // spans its box and keeps every side's direction
        for (bad in listOf(q(0 to 0, 1 to 0, 0.4 to 0.4, 0 to 1), q(0 to 0, 1 to 0, 0.5 to 0.5, 0 to 1), q(0.6 to 0.6, 1 to 0, 1 to 1, 0 to 1)))
            assertNull(Quad.read(bad), "rule 4: $bad")
        assertNotNull(Quad.read(q(0 to 0, 1 to 0, 0.6 to 0.6, 0 to 1)), "the same corner turned OUT is a shape")
        // the box's own four corners say what the box says: read as no key
        val same = q(0 to 0, 1 to 0, 1 to 1, 0 to 1)
        assertNull(Quad.read(same)); assertTrue(Quad.identity(same)); assertTrue(Quad.identity(q(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 1.0)))
        assertFalse(Quad.identity(PARTS)); assertFalse(Quad.identity(PARTS.take(3))); assertFalse(Quad.identity(null))
        // 5. the mark may have one: not a point, not a Note -- whatever its key says
        for (t in listOf("E", "P", "F", "PLB", "LV", MarkTypes.NOTE)) {
            val pt = mark(t, "F${t}1", 600.0, 650.0, 900.0, 950.0, mapOf(Quad.KEY to PARTS))
            assertFalse(Quad.mayHave(pt), t); assertNull(Quad.of(pt), t)
            assertEquals(listOf(listOf(600.0, 900.0), listOf(650.0, 900.0), listOf(650.0, 950.0), listOf(600.0, 950.0)), mm(pt), "$t is its box")
            assertFalse(Quad.set(pt, MM), t); assertNull(Quad.drag(pt, pt.copy(), 0, 10.0, 10.0, A, B, 25.0), t)
            assertFalse(Quad.free(pt, pt.id, "q0"), "$t is never free, whatever the three rules say")
        }
        // RULE 5 IS S5's, TO THE TOKEN (the review of Q2, finding 5). S5 knows a Note in any capitals and with white space
        // round its token (asis_view.js isNote: `e.t.trim().toUpperCase() === 'NOTE'`; its own test vector is " note "),
        // and reads such a mark's `quad` as no shape -- so the phone does, though it draws a Note only for the exact token
        for (t in listOf(" note ", "note", "Note", "NOTE ", "\tNoTe\n", "\ufeffnote", "\u00a0NOTE\u3000")) {
            val odd = mark(t, "FX1", extra = mapOf(Quad.KEY to PARTS))
            assertTrue(Quad.noteToken(t), "[$t]"); assertFalse(SurveyPrep.isSpot(t), "[$t] is no pin on the phone: only its shape is refused")
            assertFalse(Quad.mayHave(odd), "[$t]"); assertNull(Quad.of(odd), "[$t]"); assertEquals(BOX, box(odd), "[$t] is its box")
            assertFalse(Quad.set(odd, MM), "[$t]"); assertNull(Quad.drag(odd, odd.copy(), 0, 10.0, 10.0, A, B, 25.0), "[$t]")
            assertFalse(Quad.free(odd, odd.id, "q0"), "[$t]"); assertEquals(Quad.Handles.TABS, Quad.handles(odd, true, true), "[$t]")
        }
        // ... trimmed as JAVASCRIPT trims, exactly: its WhiteSpace and LineTerminator, of which Kotlin's own trim has
        // U+001C … U+001F too many and U+FEFF too few. Every one of the 65,536 code units is tried on both ends
        val jsSpace = "\u0009\u000a\u000b\u000c\u000d\u0020\u00a0\u1680\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a\u2028\u2029\u202f\u205f\u3000\ufeff"
        assertEquals(25, jsSpace.toSet().size)
        for (c in Char.MIN_VALUE..Char.MAX_VALUE) {
            assertEquals(c in jsSpace, Quad.noteToken(c + "note"), "U+%04X before".format(c.code))
            assertEquals(c in jsSpace, Quad.noteToken("Note$c"), "U+%04X after".format(c.code))
        }
        // and nothing else is a Note: a longer word, a space inside, no type at all -- each may have a shape, as before
        for (t in listOf("NOTES", "N OTE", "NOT", "", " ", "X", "\u001cNOTE", "NOTE\u200b")) {
            assertFalse(Quad.noteToken(t), "[$t]"); assertTrue(Quad.mayHave(t), "[$t]")
            assertNotNull(Quad.of(mark(t, "FX1", extra = mapOf(Quad.KEY to PARTS))), "[$t]")
        }
        // every other type of every kind of surface may: sixteen tokens, the seventeen box types (S is a wall's Switch
        // plate and the floor's Step)
        val may = SurveyPrep.SURFACES.flatMap { s -> SurveyPrep.types(s).map { it.letter } }.toSet().filter { Quad.mayHave(it) }.toSet()
        assertEquals(setOf("D", "W", "O", "N", "C", "B", "L", "S", "X", "GRL", "AC", "CDP", "CTR", "CRP", "SAN", "DEF"), may)
        // an unreadable value on a mark leaves it its box, everywhere
        val broken = mark(extra = mapOf(Quad.KEY to PARTS.take(3)))
        assertNull(Quad.of(broken)); assertEquals(BOX, box(broken)); assertEquals(listOf(true, true, true, true), Quad.straight(broken))
    }

    @Test fun `the parts are unchanged by a move, a nudge, an edge snap and a stretch -- the shape rides with its box`() {
        val e = window(); val parts = e.extra[Quad.KEY]; val o = e.copy()
        // a body move: the box moves, the key is the very same value, and every corner moved by the same amount
        assertTrue(SurveyPrep.moveMark(e, o, "body", 300.0, -200.0, A, B))
        assertSame(parts, e.extra[Quad.KEY]); assertEquals(listOf(900.0, 3270.0, 700.0, 2250.0), box(e))
        assertEquals(MM.map { listOf(it[0] + 300, it[1] - 200) }, mm(e))
        // dragged to the wall's edge it snaps as a rectangle does: the box, with the shape in it
        SurveyPrep.moveMark(e, o, "body", 620.0, 0.0, A, B); SurveyPrep.snapMark(e, "body", A, B)
        assertEquals(listOf(1230.0, 3600.0), box(e).take(2)); assertSame(parts, e.extra[Quad.KEY])
        // a stretch by the box's edges -- what an older build's resize does: the shape stretches with it, and upright
        // sides stay upright and level sides level
        val s = window(); val so = s.copy(); val own = s.extra[Quad.KEY]
        assertTrue(SurveyPrep.moveMark(s, so, "l", -200.0, 0.0, A, B)); assertTrue(SurveyPrep.moveMark(s, s.copy(), "t", 0.0, 100.0, A, B))
        assertSame(own, s.extra[Quad.KEY]); assertEquals(PARTS, own); assertEquals(listOf(400.0, 2970.0, 900.0, 2550.0), box(s))
        assertEquals(listOf(false, true, true, true), Quad.straight(s))
        assertEquals(listOf(listOf(400.0, 1719.7), listOf(2970.0, 900.0), listOf(2970.0, 2550.0), listOf(400.0, 2550.0)), mm(s))
        // whether it can be read does not depend on the box: squeezed to a sliver it still reads
        val thin = mark(x0 = 600.0, x1 = 600.5, extra = mapOf(Quad.KEY to PARTS)); assertNotNull(Quad.of(thin))
    }

    // ------------------------------------------------------------------ what stays impossible

    @Test fun `every refused edit leaves the mark where it last was`() {
        fun refused(e: SurveyPrep.Mark, what: String, edit: () -> Boolean) {
            val b = box(e); val x = e.extra
            assertFalse(edit(), what); assertEquals(b, box(e), what); assertSame(x, e.extra, what)
        }
        // a side that runs under 20 mm IN ITS OWN DIRECTION -- here the bottom, 10 mm across, though it is 300 long and
        // every corner stands well clear of its neighbours' line
        val e = mark()
        refused(e, "the bottom runs 10 mm") { Quad.set(e, pts(0.0 to 300.0, 10.0 to 0.0, 500.0 to 600.0, 300.0 to 900.0)) }
        assertTrue(Quad.set(mark(), pts(0.0 to 300.0, 20.0 to 0.0, 500.0 to 600.0, 300.0 to 900.0)), "20 mm is enough")
        refused(e, "the left runs 10 mm up") { Quad.set(e, pts(300.0 to 590.0, 900.0 to 0.0, 600.0 to 900.0, 0.0 to 600.0)) }
        // crossed sides: two corners swapped
        refused(e, "crossed") { Quad.set(e, pts(1000.0 to 0.0, 0.0 to 0.0, 1000.0 to 1000.0, 0.0 to 1000.0)) }
        // a corner turned inward, and a shape flattened to a triangle: one test, a corner under 1 mm (Quad.OUT_MM)
        // outside the line joining its two neighbours -- NOT the 20 mm of a side's run (the review of Q1, finding 1)
        refused(e, "turned inward") { Quad.set(e, pts(0.0 to 0.0, 1000.0 to 0.0, 400.0 to 400.0, 0.0 to 1000.0)) }
        refused(e, "a triangle") { Quad.set(e, pts(0.0 to 0.0, 1000.0 to 0.0, 500.0 to 500.0, 0.0 to 1000.0)) }
        refused(e, "0.7 mm outside") { Quad.set(e, pts(0.0 to 0.0, 1000.0 to 0.0, 500.5 to 500.5, 0.0 to 1000.0)) }
        assertTrue(Quad.set(mark(), pts(0.0 to 0.0, 1000.0 to 0.0, 501.0 to 501.0, 0.0 to 1000.0)), "1.4 mm outside is a shape")
        assertTrue(Quad.set(mark(), pts(0.0 to 0.0, 1000.0 to 0.0, 510.0 to 510.0, 0.0 to 1000.0)), "14 mm outside is a shape")
        // not four corners, a corner that is no number
        refused(e, "three corners") { Quad.set(e, MM.take(3)) }
        refused(e, "five corners") { Quad.set(e, MM + listOf(doubleArrayOf(1000.0, 1000.0))) }
        refused(e, "NaN") { Quad.set(e, pts(600.0 to Double.NaN, 2970.0 to 900.0, 2970.0 to 2450.0, 600.0 to 2450.0)) }
        // THE DRAG: the corner stops where the shape would break, and the mark is as the last good move left it
        val w = window(); val o = w.copy()
        assertNotNull(Quad.drag(w, o, 0, 400.0, 0.0, A, B, 0.0))            // q0 to x 1000: still a shape
        val good = box(w); val at = w.extra
        assertNull(Quad.drag(w, o, 0, 2360.0, 0.0, A, B, 0.0), "q0 10 mm left of q1: the bottom runs under 20 mm")
        assertEquals(good, box(w)); assertSame(at, w.extra)
        assertNull(Quad.drag(w, o, 0, 1000.0, 300.0, A, B, 0.0), "q0 pulled across the line from q3 to q1: turned inward")
        assertEquals(good, box(w)); assertSame(at, w.extra)
        // ... through moveMark too, which answers false and moves nothing
        assertFalse(SurveyPrep.moveMark(w, o, "q0", 2360.0, 0.0, A, B)); assertEquals(good, box(w))
        // A CORNER OFF THE FACE is not refused: it is kept on the face first
        val f = window()
        val d = assertNotNull(Quad.drag(f, f.copy(), 2, 5000.0, 5000.0, A, B, 0.0))
        assertEquals(listOf(A, B), d.p.toList()); assertEquals(listOf(600.0, A, 900.0, B), box(f))
        val g = window(); assertNotNull(Quad.drag(g, g.copy(), 1, 0.0, -5000.0, A, B, 0.0)); assertEquals(0.0, g.y0)
        // a locked mark's corners do not move, by a drag, a nudge or a snap
        val k = window().also { it.extra = SurveyPrep.withKey(it.extra, SurveyPrep.LOCKED, true) }
        val kb = box(k); val kx = k.extra
        assertNull(Quad.drag(k, k.copy(), 0, 0.0, -300.0, A, B, 150.0)); assertFalse(SurveyPrep.moveMark(k, k.copy(), "q0", 0.0, -300.0, A, B))
        SurveyPrep.snapMark(k, "q0", A, B)
        assertEquals(kb, box(k)); assertSame(kx, k.extra)
        // there is no fifth corner
        assertNull(Quad.drag(window(), window(), 4, 10.0, 10.0, A, B, 0.0)); assertNull(Quad.cornerOf("q4")); assertNull(Quad.cornerOf("body"))
        assertNull(Quad.cornerOf("lt")); assertNull(Quad.cornerOf(null)); assertEquals(listOf(0, 1, 2, 3), Quad.PARTS.map { Quad.cornerOf(it) })
        // no part's name holds l, r, t or b, which moveMark and snapMark test by letter
        assertTrue(Quad.PARTS.none { p -> "lrtb".any { it in p } })
    }

    /** Review of Q1, finding 1: the corner test stood at 20 mm, the resize floor itself, and a W x H rectangle's corner is
     *  W·H / √(W² + H²) outside its neighbours' line -- always UNDER its shorter side. So a mark squeezed to the floor a
     *  tab stops at refused every corner move, the ones that only enlarge it too, and a 30 mm square refused some. */
    @Test fun `every rectangle a tab can leave has corners that move -- the thinnest too`() {
        fun rect(w: Double, h: Double) = mark(x0 = 1000.0, x1 = 1000.0 + w, y0 = 500.0, y1 = 500.0 + h)
        // a mark squeezed by its top tab as far as it goes: 20 mm thick, the floor of SurveyPrep.MIN_SIZE
        val thin = rect(2000.0, 500.0)
        assertTrue(SurveyPrep.moveMark(thin, thin.copy(), "t", 0.0, -5000.0, A, B)); assertEquals(SurveyPrep.MIN_SIZE, thin.y1 - thin.y0)
        // ... its corners still move: outward, which only enlarges it,
        assertTrue(SurveyPrep.moveMark(thin, thin.copy(), "q2", 0.0, 5.0, A, B), "2000 x 20, q2 up 5")
        assertEquals(listOf(listOf(1000.0, 500.0), listOf(3000.0, 500.0), listOf(3000.0, 525.0), listOf(1000.0, 520.0)), mm(thin))
        assertTrue(SurveyPrep.moveMark(thin, thin.copy(), "q0", 0.0, -1.0, A, B), "then q0 down 1")
        assertEquals(listOf(1000.0, 3000.0, 499.0, 525.0), box(thin))
        // ... and along its own side, which keeps both its short sides at the floor
        val slid = rect(2000.0, 20.0); assertTrue(SurveyPrep.moveMark(slid, slid.copy(), "q0", 300.0, 0.0, A, B), "2000 x 20, q0 along the bottom")
        // every rectangle at the floor, long or square, each corner pulled 5 mm straight out of the mark
        val out = listOf(-5.0 to -5.0, 5.0 to -5.0, 5.0 to 5.0, -5.0 to 5.0)
        for ((w, h) in listOf(2000.0 to 20.0, 20.0 to 2000.0, 100.0 to 20.0, 20.0 to 20.0, 30.0 to 30.0, 21.0 to 2000.0)) for (i in 0..3) {
            val e = rect(w, h)
            assertTrue(SurveyPrep.moveMark(e, e.copy(), "q$i", out[i].first, out[i].second, A, B), "$w x $h, q$i pulled out")
            assertNotNull(Quad.of(e), "$w x $h, q$i: now a shape that is not its box")
        }
        // the review's own two: a 30 mm square's q2 up 5 (q3 then stands 19.5 mm outside the line from q2 to q0), and a
        // 20 mm square's q2 up 20 -- a sound little trapezoid, 20 wide with uprights of 20 and 40
        val s30 = rect(30.0, 30.0); assertTrue(SurveyPrep.moveMark(s30, s30.copy(), "q2", 0.0, 5.0, A, B), "30 x 30, q2 up 5")
        val s20 = rect(20.0, 20.0); assertTrue(SurveyPrep.moveMark(s20, s20.copy(), "q2", 0.0, 20.0, A, B), "20 x 20, q2 up 20")
        // WHAT THE FLOOR STILL STOPS: a side shortened under 20 mm in its own direction, on the same thin mark
        val t = rect(2000.0, 20.0); val tb = box(t); val te = t.extra
        assertFalse(SurveyPrep.moveMark(t, t.copy(), "q2", 0.0, -1.0, A, B), "the right side would run 19 mm"); assertEquals(tb, box(t)); assertSame(te, t.extra)
        assertFalse(SurveyPrep.moveMark(t, t.copy(), "q3", 1990.0, 0.0, A, B), "the top would run 10 mm"); assertEquals(tb, box(t)); assertSame(te, t.extra)
        // the two floors are two figures: a side's run is the resize floor; a corner's stand-off only keeps the shape a shape
        assertEquals(20.0, SurveyPrep.MIN_SIZE); assertEquals(1.0, Quad.OUT_MM)
        assertTrue(Quad.OUT_MM < SurveyPrep.MIN_SIZE / Math.sqrt(2.0), "the smallest square a tab can leave stands 14.1 mm clear at each corner")
    }

    /** Review of Q1, finding 2 -- a DECISION pinned, not a change: [SurveyGuide.snapCoord] reads x0 x1 y0 y1 alone, so a
     *  Line's, an area's or a chain's end catches a four-corner mark's BOX, and the tag a chain keeps ("FW1 bottom", with a
     *  space) names the box's edge -- the level of the shape's outermost corner, where FW1.s is measured to -- never the
     *  side FW1.bottom reads. Passes before the review as after it; fails the day somebody adds the corners as candidates
     *  without giving them a tag of their own. */
    @Test fun `a Line's or a chain's end snaps to a four-corner mark's box -- the snap tag names the box's edge, never a side`() {
        val w = window(); val box = mark()
        fun sn(e: SurveyPrep.Mark, v: Double, axis: Int) = SurveyGuide.snapCoord("front", A, B, listOf(e), v, axis, 25.0).let { it.v to it.tag }
        // the box's four edges, each with a corner of the shape on it: the lowest corner's level is "FW1 bottom"
        assertEquals(900.0 to "FW1 bottom", sn(w, 910.0, 1)); assertEquals(2450.0 to "FW1 top", sn(w, 2440.0, 1))
        assertEquals(600.0 to "FW1 left", sn(w, 610.0, 0)); assertEquals(2970.0 to "FW1 right", sn(w, 2960.0, 0))
        // the corner INSIDE the box -- q0, the short upright's foot at y 1670 -- is no candidate: the end stays where it was put
        assertEquals(listOf(600.0, 1670.0), mm(w)[0]); assertEquals(1675.0 to null, sn(w, 1675.0, 1)); assertEquals(1670.0 to null, sn(w, 1670.0, 1))
        // so on every level and every upright of the wall a four-corner mark snaps exactly as the rectangle of its box does
        var v = 0.0
        while (v <= A) { assertEquals(sn(box, v, 0), sn(w, v, 0), "x $v"); if (v <= B) assertEquals(sn(box, v, 1), sn(w, v, 1), "y $v"); v += 5.0 }
        // a snap tag is not a reading key: no side's key is ever a tag, and "FW1 bottom" is no key S5's grammar would take
        val tags = listOf("FW1 left", "FW1 right", "FW1 bottom", "FW1 top")
        assertEquals(listOf("FW1.bottom", "FW1.right", "FW1.top", "FW1.left"), Quad.sideCodes(w)); assertTrue(Quad.sideCodes(w).none { it in tags })
        assertTrue(tags.none { Regex("^[A-Za-z][A-Za-z0-9-]{0,23}\\.([A-Za-z]{1,8}|[0-9]{1,3})$").matches(it) })
    }

    // ------------------------------------------------------------------ dragging a corner, and the snap

    @Test fun `a dragged corner snaps straight within 5 degrees and within the snapping distance, and onto the face's edge on a free axis`() {
        fun q0(dy: Double, tol: Double, e: SurveyPrep.Mark = window()) = Quad.drag(e, e.copy(), 0, 0.0, dy, A, B, tol) to e
        // BY DISTANCE. The bottom-left corner pulled down to 50 mm over the level of its neighbour: 1.2 degrees off
        // level over 2370 mm. Within 150 mm (the wall at fit) it snaps level -- and the shape is its box, so the key goes
        val (fit, a) = q0(-720.0, 150.0)
        assertEquals(listOf(600.0, 900.0), fit!!.p.toList()); assertEquals("hv", fit.snap); assertFalse(Quad.KEY in a.extra); assertEquals(BOX, box(a))
        // ... zoomed in, where the distance is its 25 mm floor, the same 50 mm is drawn as a run-out
        val (zoomed, b) = q0(-720.0, 25.0)
        assertEquals(listOf(600.0, 950.0), zoomed!!.p.toList()); assertEquals("v", zoomed.snap); assertTrue(Quad.KEY in b.extra)
        // exactly at the distance it snaps; a hair past it, it does not
        assertEquals(900.0, q0(-745.0, 25.0).first!!.p[1]); assertEquals(925.1, q0(-744.9, 25.0).first!!.p[1])
        // ... and with no distance at all nothing snaps: a nudge
        assertEquals(listOf(600.0, 901.0), q0(-769.0, 0.0).first!!.p.toList())
        // BY ANGLE. On a narrow mark 30 mm off level over 200 mm is 8.5 degrees: inside the distance, and it stays sloped
        val n = mark("X", "FX1", 1000.0, 1200.0, 1000.0, 1600.0)
        val steep = assertNotNull(Quad.drag(n, n.copy(), 0, 0.0, 30.0, A, B, 150.0))
        assertEquals(listOf(1000.0, 1030.0), steep.p.toList()); assertEquals("v", steep.snap)
        // 17 mm over the same 200 is 4.9 degrees: it snaps level
        val m = mark("X", "FX2", 1000.0, 1200.0, 1000.0, 1600.0)
        assertEquals(1000.0, assertNotNull(Quad.drag(m, m.copy(), 0, 0.0, 17.0, A, B, 150.0)).p[1])
        assertEquals(5.0, SurveyMeas.SNAP_DEG)
        // THE UPRIGHT NEIGHBOUR, the same rule turned: the top-right corner 40 mm right of the corner under it, over 1550 mm
        val u = window(); val up = assertNotNull(Quad.drag(u, u.copy(), 2, 40.0, 0.0, A, B, 150.0))
        assertEquals(listOf(2970.0, 2450.0), up.p.toList()); assertEquals("hv", up.snap)
        val v = window(); val out = assertNotNull(Quad.drag(v, v.copy(), 2, 40.0, 0.0, A, B, 25.0))
        assertEquals(listOf(3010.0, 2450.0), out.p.toList()); assertEquals("h", out.snap)
        // neither side straight: "d"
        val x = window(); assertEquals("d", assertNotNull(Quad.drag(x, x.copy(), 2, 200.0, -200.0, A, B, 25.0)).snap)
        // ON AN AXIS NO SIDE FIXED, the face's edge within 25 mm: the bottom-right corner 10 mm short of the right corner
        val e = window(); val edge = assertNotNull(Quad.drag(e, e.copy(), 1, 620.0, 0.0, A, B, 25.0))
        assertEquals(listOf(A, 900.0), edge.p.toList()); assertEquals(A, e.x1)
        val far = window(); assertEquals(3570.0, assertNotNull(Quad.drag(far, far.copy(), 1, 600.0, 0.0, A, B, 25.0)).p[0], "30 mm short: no snap")
        val free = window(); assertEquals(3590.0, assertNotNull(Quad.drag(free, free.copy(), 1, 620.0, 0.0, A, B, 0.0)).p[0], "a nudge never snaps")
        assertEquals(25.0, SurveyPrep.SNAP)
        // A DRAG ON THE 360 goes through moveMark (no snap) and then snapMark, whose distance is the 25 mm floor
        val s = window(); val so = s.copy()
        assertTrue(SurveyPrep.moveMark(s, so, "q0", 0.0, -750.0, A, B)); assertEquals(listOf(600.0, 920.0), mm(s)[0])
        SurveyPrep.snapMark(s, "q0", A, B); assertEquals(listOf(600.0, 900.0), mm(s)[0]); assertFalse(Quad.KEY in s.extra)
        assertTrue(SurveyPrep.moveMark(s, so, "q0", 0.0, -740.0, A, B)); SurveyPrep.snapMark(s, "q0", A, B)
        assertEquals(listOf(600.0, 930.0), mm(s)[0], "30 mm off: past the floor, it stays"); assertTrue(Quad.KEY in s.extra)
        // the nudges of 1 mm move one corner and never snap
        val nd = window(); assertTrue(SurveyPrep.moveMark(nd, nd.copy(), "q3", 1.0, 0.0, A, B)); assertEquals(listOf(601.0, 2450.0), mm(nd)[3])
        assertEquals(listOf(false, true, true, false), Quad.straight(nd))
        // the guide: the two rays through the corner itself, bold along a side that is straight -- both on a square corner
        val gd = Quad.guide("front", fit)
        assertEquals("front", gd.s); assertEquals(listOf(600.0, 900.0), gd.o.toList()); assertEquals("hv", gd.snap)
        val both = SurveyGuide.guideRays(gd.o, A, B, gd.snap)
        assertTrue(both.bold("h") && both.bold("v")); assertEquals(SurveyGuide.RAY_SNAP_DP, both.widthDp("h")); assertEquals(SurveyGuide.RAY_SNAP_DP, both.widthDp("v"))
        val one = SurveyGuide.guideRays(zoomed.p, A, B, zoomed.snap)
        assertTrue(one.bold("v")); assertFalse(one.bold("h")); assertEquals(SurveyGuide.RAY_DP, one.widthDp("h"))
        val none = SurveyGuide.guideRays(doubleArrayOf(1.0, 1.0), A, B, "d"); assertFalse(none.bold("h") || none.bold("v"))
        // a Line's and a tape's guide is what it was: one axis, or none
        assertTrue(SurveyGuide.guideRays(doubleArrayOf(1.0, 1.0), A, B, "h").bold("h")); assertFalse(SurveyGuide.guideRays(doubleArrayOf(1.0, 1.0), A, B, null).bold("v"))
    }

    @Test fun `which handles a mark shows -- one rule, three ways in, and a drag through the box keeps its four corners`() {
        val rect = mark(); val quad = window()
        // 1. it has a quad; 2. its id is the freed one; 3. the picked part of THIS mark is a corner
        assertTrue(Quad.free(quad, null, null)); assertFalse(Quad.free(rect, null, null))
        assertTrue(Quad.free(rect, rect.id, null)); assertFalse(Quad.free(rect, "another", null))
        for (p in Quad.PARTS) assertTrue(Quad.free(rect, null, p), p)
        for (p in listOf("body", "l", "rt", "lb", null)) assertFalse(Quad.free(rect, null, p), "$p")
        // WHY THREE: a mark opened from the file (nothing freed) whose corner q0 is dragged back to its box loses its
        // key in the middle of the drag -- and must not turn into a rectangle with eight tabs under the finger
        val e = window(); val o = e.copy()
        assertNotNull(Quad.drag(e, o, 0, 0.0, -400.0, A, B, 150.0)); assertTrue(Quad.KEY in e.extra); assertTrue(Quad.free(e, null, "q0"))
        val through = assertNotNull(Quad.drag(e, o, 0, 0.0, -760.0, A, B, 150.0))
        assertEquals("hv", through.snap); assertFalse(Quad.KEY in e.extra, "the shape is its box: the key went")
        assertTrue(Quad.free(e, null, "q0"), "rule 3: the corner is still held"); assertFalse(Quad.free(e, null, null), "rules 1 and 2 alone would say eight tabs")
        // ... pulled out again, the key comes back, from the mark as it was when the drag began
        assertNotNull(Quad.drag(e, o, 0, 0.0, -300.0, A, B, 150.0))
        assertEquals(listOf(listOf(0.0, 0.303226), listOf(1.0, 0.0), listOf(1.0, 1.0), listOf(0.0, 1.0)), e.extra[Quad.KEY])
        assertTrue(Quad.free(e, null, "q0")); assertTrue(Quad.free(e, null, null))
        // a mark with NO key drags by a corner too: its corners are the box's four
        val r = mark(); val got = assertNotNull(Quad.drag(r, r.copy(), 0, 0.0, 770.0, A, B, 0.0))
        assertEquals(listOf(600.0, 1670.0), got.p.toList()); assertEquals(PARTS, r.extra[Quad.KEY]); assertEquals(BOX, box(r))
    }

    // ------------------------------------------------------------------ the figures

    private fun asks(s: String, e: SurveyPrep.Mark, a: Double = A, b: Double = B) = SurveyPrep.measures(s, e, a, b)

    @Test fun `the example, to the last figure -- four sides, the corner offset, the sill at its low corner, the reveal`() {
        val e = window(); val ms = asks("front", e)
        assertEquals(listOf("bottom", "right", "top", "left", "l", "s", "p"), ms.map { it.key })
        assertEquals(listOf("bottom side", "right side", "top side", "left side", "from left corner", "sill, from floor, to its nearest corner", "reveal depth"),
            ms.map { it.what(en) })
        // the photo's estimates: 2492 is the distance between the bottom's two corners, sqrt(2370^2 + 770^2) = 2491.9
        assertEquals(listOf<Long?>(2492, 1550, 2370, 780, 600, 900, null), ms.map { m -> m.est?.let { Math.round(it) } })
        assertEquals(2491.95, ms[0].est!!, 0.01)
        // left, right and top are straight; two corners are square; no diagonal is asked. Seven figures
        assertEquals(listOf(false, true, true, true), Quad.straight(e)); assertTrue(Quad.square(e)); assertEquals(7, ms.size)
        assertEquals("top", Quad.spanW(e)); assertEquals("right", Quad.spanH(e))
        assertEquals(listOf("FW1.bottom", "FW1.right", "FW1.top", "FW1.left"), Quad.sideCodes(e)); assertEquals(emptyList(), Quad.sideCodes(mark()))
        // its reading keys, in the walk's order
        val p = SurveyPrep.fresh(L, A, B); p.surface("front").marks.add(e)
        assertEquals(listOf("FW1.bottom", "FW1.right", "FW1.top", "FW1.left", "FW1.l", "FW1.s", "FW1.p"), p.keysOf("front", e, A, B))
        // w and h are NOT asked: they mean the box's overall size on every reader, and a trapezoid has neither to laser
        assertTrue(ms.none { it.key == "w" || it.key == "h" })
        // THE SAME MARK AS A RECTANGLE asks what a window always asked -- and Quad.asked answers that very list
        val r = mark(); val base = SurveyPrep.measures0("front", r, A, B)
        assertEquals(listOf("w", "h", "l", "s", "p"), base.map { it.key }); assertSame(base, Quad.asked("front", r, A, B, base))
        assertEquals(listOf("width", "height", "from left corner", "sill, from floor", "reveal depth"), base.map { it.what(en) })
        // where the shape touches each edge of its box, which is where its position lines end: its low corner is q1
        val t = Quad.touch(e)
        assertEquals(listOf(2060.0, 1675.0, 2970.0, 1785.0), listOf(r1(t.yLeft), t.yRight, t.xBottom, t.xTop))
        val tr = Quad.touch(r); assertEquals(listOf(1675.0, 1675.0, 1785.0, 1785.0), listOf(tr.yLeft, tr.yRight, tr.xBottom, tr.xTop))
        // Marathi: the nine words are English for now (the journey is verified in English first); the rest keeps its own
        assertEquals("bottom side", ms[0].what(Tx.MR)); assertEquals("सिल, जमिनीपासून, to its nearest corner", ms[5].what(Tx.MR))
    }

    @Test fun `what four sides fix -- the diagonal is asked exactly when the mark has no square corner`() {
        // (an Other, in the example's box: nearer the left corner and the ceiling, so its own two are l and t)
        fun shape(vararg p: Pair<Double, Double>) = mark("X", "FX1").also { assertTrue(Quad.set(it, pts(*p)), "a shape") }
        fun keys(e: SurveyPrep.Mark, s: String = "front") = asks(s, e).map { it.key }
        val sides = listOf("bottom", "right", "top", "left")
        // a square corner and three straight sides (the trapezoid): four sides, one to spare
        val trap = shape(600.0 to 1670.0, 2970.0 to 900.0, 2970.0 to 2450.0, 600.0 to 2450.0)
        assertEquals(3, Quad.straight(trap).count { it }); assertTrue(Quad.square(trap)); assertEquals(sides + listOf("l", "t"), keys(trap))
        // a square corner and two straight sides: four sides fix it exactly
        val two = shape(600.0 to 900.0, 2970.0 to 900.0, 2500.0 to 2000.0, 600.0 to 2450.0)
        assertEquals(listOf(true, false, false, true), Quad.straight(two)); assertTrue(Quad.square(two)); assertEquals(sides + listOf("l", "t"), keys(two))
        // two parallel straight sides and no square corner -- a level top and bottom, both uprights leaning: the diagonal
        val lean = shape(600.0 to 900.0, 2500.0 to 900.0, 2970.0 to 2450.0, 1000.0 to 2450.0)
        assertEquals(listOf(true, false, true, false), Quad.straight(lean)); assertFalse(Quad.square(lean)); assertEquals(sides + listOf("diag", "l", "t"), keys(lean))
        // ... and two uprights with neither end level
        val shear = shape(600.0 to 900.0, 2970.0 to 1200.0, 2970.0 to 2450.0, 600.0 to 2000.0)
        assertEquals(listOf(false, true, false, true), Quad.straight(shear)); assertFalse(Quad.square(shear)); assertEquals("diag", keys(shear)[4])
        // one straight side, or none: braced by the diagonal
        val one = shape(600.0 to 900.0, 2970.0 to 900.0, 2700.0 to 2450.0, 900.0 to 2000.0)
        assertEquals(1, Quad.straight(one).count { it }); assertEquals("diag", keys(one)[4])
        val none = shape(600.0 to 1200.0, 2600.0 to 900.0, 2970.0 to 2100.0, 1000.0 to 2450.0)
        assertEquals(0, Quad.straight(none).count { it }); assertEquals(sides + listOf("diag", "l", "t"), keys(none))
        // the diagonal runs from q0 to q2, and its estimate is that distance; its words say which way
        val dg = asks("front", none).first { it.key == "diag" }
        assertEquals(hypot(2970.0 - 600.0, 2100.0 - 1200.0), dg.est!!, 0.05); assertEquals("diagonal, bottom-left to top-right", dg.what(en))
        // each side's estimate is the distance between its two corners
        val c = Quad.corners(none)
        for (i in 0 until 4) assertEquals(hypot(c[(i + 1) % 4][0] - c[i][0], c[(i + 1) % 4][1] - c[i][1]), asks("front", none)[i].est!!, 1e-9)
        // a rectangle has all four straight, a square corner, no side spanning to report and nothing new to ask
        val r = mark(); assertEquals(listOf(true, true, true, true), Quad.straight(r)); assertTrue(Quad.square(r)); assertNull(Quad.spanW(r)); assertNull(Quad.spanH(r))
        // which side spans the box: a level one its width, an upright one its height; a leaning shape has neither
        assertEquals("bottom", Quad.spanW(two)); assertEquals("left", Quad.spanH(two)); assertNull(Quad.spanW(lean)); assertNull(Quad.spanH(lean))
        assertNull(Quad.spanW(shear)); assertEquals("left", Quad.spanH(shape(600.0 to 900.0, 2970.0 to 1200.0, 2970.0 to 2000.0, 600.0 to 2450.0)))
    }

    @Test fun `the position words gain to its nearest corner only where that side of the shape is not straight`() {
        fun shape(t: String, vararg p: Pair<Double, Double>) = mark(t, "F${t}1").also { assertTrue(Quad.set(it, pts(*p))) }
        fun words(e: SurveyPrep.Mark, s: String = "front", a: Double = A, b: Double = B) = asks(s, e, a, b).associate { it.key to it.what(en) }
        // left of the midline with a leaning left side and a sloping bottom: both offsets run to a corner
        val lean = shape("N", 600.0 to 900.0, 1500.0 to 600.0, 1700.0 to 1700.0, 800.0 to 1800.0)
        assertEquals("from left corner, to its nearest corner", words(lean).getValue("l")); assertEquals("from floor, to its nearest corner", words(lean).getValue("s"))
        // right of the midline and high: r and t, each to a corner
        val high = shape("X", 2200.0 to 2000.0, 3300.0 to 2100.0, 3400.0 to 2600.0, 2300.0 to 2500.0)
        assertEquals("from right corner, to its nearest corner", words(high).getValue("r")); assertEquals("from ceiling, to its nearest corner", words(high).getValue("t"))
        // an upright side and a level side there: no suffix, as on the example's corner offset
        val trap = shape("X", 2200.0 to 2000.0, 3400.0 to 2000.0, 3400.0 to 2600.0, 2500.0 to 2500.0)
        assertEquals("from right corner", words(trap).getValue("r")); assertEquals("from ceiling, to its nearest corner", words(trap).getValue("t"))
        // a Beam's drop gains nothing: its line shows which corner; its four sides are asked though a beam's rectangle asks neither w nor h
        val beam = shape("B", 0.0 to 2300.0, A to 2200.0, A to B, 0.0 to B)
        assertEquals(listOf("bottom", "right", "top", "left", "d", "p"), asks("front", beam).map { it.key })
        assertEquals("drop below ceiling", words(beam).getValue("d"))
        // a Column and a Curtain track, whose rectangles ask only a width: the four sides, then the rest
        val col = shape("C", 3000.0 to 0.0, 3300.0 to 0.0, 3300.0 to B, 3100.0 to B)
        assertEquals(listOf("bottom", "right", "top", "left", "r", "p"), asks("front", col).map { it.key })
        val track = shape("CTR", 600.0 to 2500.0, 2400.0 to 2450.0, 2400.0 to 2560.0, 600.0 to 2560.0)
        assertEquals(listOf("bottom", "right", "top", "left", "l", "t", "p"), asks("front", track).map { it.key })
        // ON THE FLOOR AND THE CEILING the bottom is the back side and the top the front side, and the diagonal says so
        val unit = mark("CRP", "GCRP1", 400.0, 2200.0, 500.0, 2500.0).also { assertTrue(Quad.set(it, pts(400.0 to 800.0, 2000.0 to 500.0, 2200.0 to 2300.0, 700.0 to 2500.0))) }
        val fl = asks("floor", unit, A, L)
        assertEquals(listOf("bottom", "right", "top", "left", "diag", "l", "k", "p"), fl.map { it.key })
        assertEquals(listOf("back side", "right side", "front side", "left side", "diagonal, back-left to front-right",
            "from left wall, to its nearest corner", "from back wall, to its nearest corner", "height"), fl.map { it.what(en) })
        val over = mark("CDP", "CCDP1", 400.0, 2200.0, 3000.0, 4600.0).also { assertTrue(Quad.set(it, pts(400.0 to 3000.0, 2200.0 to 3000.0, 2200.0 to 4600.0, 900.0 to 4400.0))) }
        val ce = asks("ceil", over, A, L).associate { it.key to it.what(en) }
        assertEquals("back side", ce.getValue("bottom")); assertEquals("front side", ce.getValue("top")); assertEquals("from front wall, to its nearest corner", ce.getValue("f"))
        assertEquals("from left wall, to its nearest corner", ce.getValue("l"))
        // a mark touching a boundary still asks nothing there: the rule is the box's, as before
        val onFloor = shape("X", 600.0 to 0.0, 1500.0 to 0.0, 1700.0 to 900.0, 600.0 to 1000.0)
        assertEquals(listOf("bottom", "right", "top", "left", "l"), asks("front", onFloor).map { it.key })
    }

    @Test fun `every box type on every kind of surface asks its four sides and then its rectangle's own, without w and h`() {
        var walked = 0
        for ((s, a, b) in listOf(Triple("front", A, B), Triple("floor", A, L), Triple("ceil", A, L))) for (type in SurveyPrep.types(s)) {
            val t = type.letter
            if (!Quad.mayHave(t)) continue
            val subs: List<String?> = MarkTypes.subsOf(t, s).map { it.id }.ifEmpty { listOf(null) }
            for (sub in subs) for ((x0, y0) in listOf(400.0 to 300.0, 2000.0 to 1500.0, 0.0 to 0.0)) {
                val w = 900.0; val h = 700.0
                val rect = SurveyPrep.Mark("r", t, SurveyPrep.CODE.getValue(s) + t + "1", x0, x0 + w, y0, y0 + h, MarkTypes.withSub(emptyMap(), t, s, sub))
                // a trapezoid (a square corner) and a shape with no straight side (the diagonal), each in the same box
                for ((name, mmc) in listOf("trapezoid" to pts(x0 to y0 + 300, x0 + w to y0, x0 + w to y0 + h, x0 to y0 + h),
                    "loose" to pts(x0 to y0 + 200, x0 + w - 100 to y0, x0 + w to y0 + h - 150, x0 + 200 to y0 + h))) {
                    val e = rect.copy(); assertTrue(Quad.set(e, mmc), "$t on $s"); walked++
                    assertEquals(listOf(rect.x0, rect.x1, rect.y0, rect.y1), box(e), "the same box")
                    val got = SurveyPrep.measures0(s, e, a, b).map { it.key }
                    val want = Quad.SIDES + (if (name == "loose") listOf(Quad.DIAG) else emptyList()) +
                        SurveyPrep.measures0(s, rect, a, b).map { it.key }.filter { it != "w" && it != "h" }
                    assertEquals(want, got, "$t ${sub ?: ""} $name on $s at $x0, $y0")
                    // the position estimates are the BOX's, as on every reader: its edge is the shape's outermost corner there
                    val base = SurveyPrep.measures0(s, rect, a, b).associate { it.key to it.est }
                    for (m in SurveyPrep.measures0(s, e, a, b)) if (m.key in base) assertEquals(base[m.key], m.est, "$t ${m.key}")
                }
            }
        }
        assertTrue(walked > 300, "walked $walked shapes")
    }

    // ------------------------------------------------------------------ the one new check

    @Test fun `the closing check -- three straight sides against the fourth, readings against readings`() {
        assertEquals(25.0, Quad.CLOSE_MM); assertEquals(0.01, Quad.CLOSE_PART)
        val e = window()
        val read = mutableMapOf("FW1.bottom" to 2495.0, "FW1.right" to 1550.0, "FW1.top" to 2370.0)
        assertNull(Quad.closing(read, e), "a side unread: nothing to close")
        // read as 2495, 1550, 2370, 780: the other three make the bottom 2491.9, off by 3. No `!`
        read["FW1.left"] = 780.0
        val ok = assertNotNull(Quad.closing(read, e))
        assertEquals(0, ok.side); assertEquals("FW1.bottom", ok.code); assertEquals(2491.95, ok.derived, 0.01); assertEquals(2495.0, ok.read); assertFalse(ok.fails)
        // the left upright typed 870 by mistake: sqrt(2370^2 + 680^2) = 2465.6, off by 29.4, over the 25 mm limit
        read["FW1.left"] = 870.0
        val bad = assertNotNull(Quad.closing(read, e))
        assertEquals(2465.6, bad.derived, 0.05); assertTrue(bad.fails); assertEquals("FW1.bottom", bad.code)
        // the limit is the LARGER of 25 mm and 1 % of the derived length: exactly at it the check holds, past it it fails
        read["FW1.left"] = 780.0
        read["FW1.bottom"] = 2491.95 + 24.9; assertFalse(Quad.closing(read, e)!!.fails)
        read["FW1.bottom"] = 2491.95 + 25.1; assertTrue(Quad.closing(read, e)!!.fails)
        read["FW1.bottom"] = 2491.95 - 25.1; assertTrue(Quad.closing(read, e)!!.fails)
        val long = mark("X", "FX1").also { assertTrue(Quad.set(it, pts(0.0 to 300.0, 3500.0 to 0.0, 3500.0 to 2000.0, 0.0 to 2000.0))) }
        val far = mutableMapOf("FX1.right" to 2000.0, "FX1.top" to 3500.0, "FX1.left" to 1700.0)       // the bottom is sqrt(3500^2 + 300^2) = 3512.8; 1 % is 35.1
        far["FX1.bottom"] = 3512.8 + 34.0; assertFalse(Quad.closing(far, long)!!.fails, "34 mm on 3513: inside 1 %")
        far["FX1.bottom"] = 3512.8 + 36.0; assertTrue(Quad.closing(far, long)!!.fails)
        // whichever side slopes: the joining side is the one opposite it, the parallel pair its two neighbours
        val top = mark("X", "FX2").also { assertTrue(Quad.set(it, pts(600.0 to 900.0, 2970.0 to 900.0, 2970.0 to 2450.0, 600.0 to 1680.0))) }
        assertEquals(listOf(true, true, false, true), Quad.straight(top))
        val tc = assertNotNull(Quad.closing(mapOf("FX2.bottom" to 2370.0, "FX2.right" to 1550.0, "FX2.top" to 2492.0, "FX2.left" to 780.0), top))
        assertEquals(2, tc.side); assertEquals("FX2.top", tc.code); assertEquals(2491.95, tc.derived, 0.01); assertFalse(tc.fails)
        val side = mark("X", "FX3").also { assertTrue(Quad.set(it, pts(600.0 to 900.0, 2000.0 to 900.0, 2970.0 to 2450.0, 600.0 to 2450.0))) }
        val sc = assertNotNull(Quad.closing(mapOf("FX3.bottom" to 1400.0, "FX3.right" to 1828.0, "FX3.top" to 2370.0, "FX3.left" to 1550.0), side))
        assertEquals(1, sc.side); assertEquals("FX3.right", sc.code); assertEquals(hypot(1550.0, 970.0), sc.derived, 1e-9); assertFalse(sc.fails)
        // ONLY with exactly three straight sides: a rectangle, a shape with two, and one with none have no closing check
        val all = mapOf("FW1.bottom" to 1.0, "FW1.right" to 1.0, "FW1.top" to 1.0, "FW1.left" to 1.0, "FX1.bottom" to 1.0, "FX1.right" to 1.0, "FX1.top" to 1.0, "FX1.left" to 1.0)
        assertNull(Quad.closing(all, mark()))
        assertNull(Quad.closing(all, mark("X", "FX1").also { Quad.set(it, pts(600.0 to 900.0, 2970.0 to 900.0, 2500.0 to 2000.0, 600.0 to 2450.0)) }))
        assertNull(Quad.closing(all, mark("X", "FX1").also { Quad.set(it, pts(600.0 to 1200.0, 2600.0 to 900.0, 2970.0 to 2100.0, 1000.0 to 2450.0)) }))
        // it compares readings with readings, so it also runs where the photo check cannot: a wardrobe stands off the wall
        val unit = mark("CRP", "FCRP1").also { assertTrue(Quad.set(it, MM)) }
        assertFalse(MarkDims.inPlane("front", unit, A, B))
        assertTrue(Quad.closing(mapOf("FCRP1.bottom" to 1400.0, "FCRP1.right" to 870.0, "FCRP1.top" to 1320.0, "FCRP1.left" to 430.0), unit)!!.fails.not())
        assertTrue(Quad.closing(mapOf("FCRP1.bottom" to 1500.0, "FCRP1.right" to 870.0, "FCRP1.top" to 1320.0, "FCRP1.left" to 430.0), unit)!!.fails)
    }
}
