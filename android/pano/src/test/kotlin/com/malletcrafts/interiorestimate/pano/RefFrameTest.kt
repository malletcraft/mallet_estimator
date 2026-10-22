package com.malletcrafts.interiorestimate.pano

import java.io.File
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * THE REFERENCE RECTANGLE (v52 slice 12, item 13; Amit, 14 Oct 2026 06:45 IST: "Ref is currently treated as Mark Type
 * list. Its not like that. it should be controlled only from lines screen. also it can be visible or invisible but can
 * not be deleted."), and the ONE frame and ONE size every caller squares a face through (SurveyPrep.frameOf, sizeOf).
 * The synthetic room of every slice: L 3319 / W 2678 / H 2677, its lines where SurveyPrep.fresh puts them.
 */
class RefFrameTest {

    private val L = 3319.0; private val W = 2678.0; private val H = 2677.0

    private fun room(): SurveyPrep.Prep = SurveyPrep.fresh(L, W, H)

    /** A rectangle given in the face's mm, carried into the view through the face's laser frame: where a door
     *  905 × 2053 really is on the photo when the lines and the laser agree. */
    private fun refQuad(p: SurveyPrep.Prep, s: String, x0: Double, y0: Double, w: Double, h: Double): List<DoubleArray> {
        val f = assertNotNull(SurveyPrep.laserFrame(s, p, L, W, H))
        return listOf(doubleArrayOf(x0, y0), doubleArrayOf(x0 + w, y0), doubleArrayOf(x0 + w, y0 + h), doubleArrayOf(x0, y0 + h))
            .map { SurveyPrep.apply(f.m2p, it) }
    }

    private fun setRef(p: SurveyPrep.Prep, s: String, q: List<DoubleArray>?, w: Double?, h: Double?, on: Boolean = true,
                       extra: Map<String, Any?> = emptyMap()) {
        val m = linkedMapOf<String, Any?>("on" to on, "q" to q?.map { listOf(it[0], it[1]) }, "w" to w, "h" to h)
        m.putAll(extra)
        p.surface(s).extra = SurveyPrep.withKey(p.surface(s).extra, SurveyRef.KEY, m)
    }

    private fun near(e: Double, g: Double, tol: Double, what: String) = assertTrue(abs(e - g) <= tol, "$what: expected $e, got $g")

    @Test fun `a door 905 x 2053 reproduces the face's crossings within 1 mm, on every wall and on the floor`() {
        for (s in listOf("front", "right", "back", "left", "floor", "ceil")) {
            val p = room()
            val ab = SurveyPrep.size(s, L, W, H)
            setRef(p, s, refQuad(p, s, 300.0, 0.0, 905.0, 2053.0.coerceAtMost(ab[1] - 100)), 905.0, 2053.0.coerceAtMost(ab[1] - 100))
            val f = assertNotNull(SurveyPrep.frameOf(s, p, L, W, H), s)
            assertNotNull(f.ref, "$s: squared from its reference")
            val mm = assertNotNull(f.mm)
            val want = listOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(ab[0], 0.0), doubleArrayOf(ab[0], ab[1]), doubleArrayOf(0.0, ab[1]))
            for (i in 0 until 4) {
                near(want[i][0], mm[i][0], 1.0, "$s crossing $i x"); near(want[i][1], mm[i][1], 1.0, "$s crossing $i y")
            }
            near(ab[0], f.a, 1.0, "$s A"); near(ab[1], f.b, 1.0, "$s B")
            // m2p takes the mm crossings back onto the view crossings, and p2m the other way
            for (i in 0 until 4) {
                val v = SurveyPrep.apply(f.m2p, mm[i]); assertTrue(hypot(v[0] - f.q[i][0], v[1] - f.q[i][1]) < 1e-6, "$s m2p $i")
                val m = SurveyPrep.apply(f.p2m, f.q[i]); assertTrue(hypot(m[0] - mm[i][0], m[1] - mm[i][1]) < 1e-6, "$s p2m $i")
            }
            val z = SurveyPrep.sizeOf(s, p, L, W, H)
            assertEquals(f.a, z[0]); assertEquals(f.b, z[1])
        }
    }

    @Test fun `a reference typed 10 percent small measures the face 10 percent small -- the reference, not the laser, sizes it`() {
        val p = room()
        setRef(p, "front", refQuad(p, "front", 900.0, 400.0, 1000.0, 1200.0), 900.0, 1080.0)
        val z = SurveyPrep.sizeOf("front", p, L, W, H)
        near(W * 0.9, z[0], 1.0, "A"); near(H * 0.9, z[1], 1.0, "B")
        // and the laser frame is untouched by it
        val lf = assertNotNull(SurveyPrep.laserFrame("front", p, L, W, H))
        assertNull(lf.ref); assertEquals(W, lf.a); assertEquals(H, lf.b)
    }

    @Test fun `frameOf picks the reference only when it is on and valid -- otherwise the edges and the laser, as before`() {
        val laser = assertNotNull(SurveyPrep.frameOf("front", room(), L, W, H))
        assertNull(laser.ref)
        fun pick(q: List<DoubleArray>?, w: Double?, h: Double?, on: Boolean = true): SurveyPrep.Frame {
            val p = room(); setRef(p, "front", q, w, h, on); return assertNotNull(SurveyPrep.frameOf("front", p, L, W, H))
        }
        val p0 = room(); val q = refQuad(p0, "front", 300.0, 0.0, 905.0, 2053.0)
        assertNotNull(pick(q, 905.0, 2053.0).ref, "on, four corners, 905 x 2053")
        val q50 = refQuad(p0, "front", 600.0, 900.0, 50.0, 50.0)
        assertNotNull(pick(q50, 50.0, 50.0).ref, "50 x 50 is the least that squares")
        assertNull(pick(q, 50.0, 50.0).ref, "a door called 50 x 50 measures the wall under 100 mm: refused, the laser squares it")
        for ((why, f) in listOf(
            "off" to pick(q, 905.0, 2053.0, on = false),
            "w under 50" to pick(q, 49.0, 2053.0),
            "h under 50" to pick(q, 905.0, 49.0),
            "no w" to pick(q, null, 2053.0),
            "no corners" to pick(null, 905.0, 2053.0),
            "three corners" to pick(q.take(3), 905.0, 2053.0))) {
            assertNull(f.ref, why); assertEquals(W, f.a, why); assertEquals(H, f.b, why)
        }
        // and sizeOf follows it exactly
        val p = room(); setRef(p, "front", q, 905.0, 2053.0, on = false)
        assertEquals(listOf(W, H), SurveyPrep.sizeOf("front", p, L, W, H).toList())
        // a reference on one face squares that face only
        val p2 = room(); setRef(p2, "front", q, 905.0, 2053.0)
        assertNull(SurveyPrep.frameOf("right", p2, L, W, H)!!.ref)
        // the frame's key changes with the reference, so a squared picture is drawn again
        val k1 = pick(q, 905.0, 2053.0).key(); val k2 = pick(q, 905.0, 2054.0).key()
        assertTrue(k1 != k2 && k1 != laser.key())
    }

    @Test fun `refDefaultQuad -- 35 to 65 by 30 to 70 percent of the face through the laser frame, else 38 to 62 percent of the view`() {
        val p = room()
        val lf = assertNotNull(SurveyPrep.laserFrame("front", p, L, W, H))
        val q = SurveyRef.refDefaultQuad("front", lf)
        val want = listOf(.35 to .3, .65 to .3, .65 to .7, .35 to .7)
        for (i in 0 until 4) {
            val mm = SurveyPrep.apply(lf.p2m, q[i])
            near(W * want[i].first, mm[0], 1e-6, "corner $i x"); near(H * want[i].second, mm[1], 1e-6, "corner $i y")
        }
        val v = SurveyRef.refDefaultQuad("front", null)
        assertEquals(listOf(listOf(960 * .38, 800 * .62), listOf(960 * .62, 800 * .62), listOf(960 * .62, 800 * .38), listOf(960 * .38, 800 * .38)),
            v.map { it.toList() })
        val fl = SurveyRef.refDefaultQuad("floor", null)
        assertEquals(listOf(960 * .38, 960 * .62), fl[0].toList())
    }

    @Test fun `Reference first places the default, Edges keeps the corners and the eye, Reference brings them back`() {
        val p = room(); val sf = p.surface("front")
        val lf = SurveyPrep.laserFrame("front", p, L, W, H)
        assertFalse(SurveyRef.setMode(sf, "front", false, lf), "Edges on a face with none: nothing")
        assertNull(SurveyRef.of(sf))
        assertTrue(SurveyRef.setMode(sf, "front", true, lf))
        val r0 = assertNotNull(SurveyRef.of(sf))
        assertTrue(r0.on && r0.shown && !r0.locked); assertNull(r0.w)
        assertEquals(SurveyRef.refDefaultQuad("front", lf).map { it.toList() }, r0.q!!.map { it.toList() })
        assertFalse(SurveyRef.on(r0), "no W x H yet: it does not square")
        assertTrue(SurveyRef.setDim(sf, "w", 905.4)); assertTrue(SurveyRef.setDim(sf, "h", 2053.0))
        assertFalse(SurveyRef.setDim(sf, "h", 2053.0), "the same figure is no change")
        assertEquals(SurveyRef.setShown(sf, null), false)
        assertTrue(SurveyRef.moveCorner(sf, 2, 600.0, 200.0))
        val before = SurveyRef.of(sf)!!
        assertEquals(905.0, before.w)
        assertTrue(SurveyRef.setMode(sf, "front", false, lf))
        val off = SurveyRef.of(sf)!!
        assertFalse(off.on); assertEquals(before.q!!.map { it.toList() }, off.q!!.map { it.toList() }); assertFalse(off.shown)
        assertNull(SurveyPrep.frameOf("front", p, L, W, H)!!.ref, "off: the laser squares it")
        assertTrue(SurveyRef.setMode(sf, "front", true, lf))
        val back = SurveyRef.of(sf)!!
        assertTrue(back.on); assertEquals(before.q!!.map { it.toList() }, back.q!!.map { it.toList() })
        assertFalse(back.shown, "still hidden"); assertEquals(905.0, back.w); assertEquals(2053.0, back.h)
        assertFalse(SurveyRef.setMode(sf, "front", true, lf), "already on: nothing")
    }

    @Test fun `a figure into a reference box is the reference's, never a reading`() {
        val p = room(); val sf = p.surface("front")
        SurveyRef.setMode(sf, "front", true, null)
        p.actual["FD1.w"] = 900.0
        SurveyRef.setDim(sf, "w", 905.0); SurveyRef.setDim(sf, "h", 2053.0)
        assertEquals(mapOf("FD1.w" to 900.0), p.actual.toMap())
        assertTrue(SurveyRef.setDim(sf, "w", null)); assertNull(SurveyRef.of(sf)!!.w)
        assertFalse(SurveyRef.setDim(sf, "x", 1.0))
        // the box: digits only, five at most; a commit only when the figure changes
        assertEquals("12345", SurveyRef.boxText("1a2-3 456"))
        assertNull(SurveyRef.typed(905.0, "905")); assertEquals(906.0, SurveyRef.typed(905.0, "906")!!.value)
        assertNull(SurveyRef.typed(905.0, "")!!.value); assertNull(SurveyRef.typed(null, ""))
    }

    @Test fun `the eye hides it on the faces and it still squares the face`() {
        val p = room(); val sf = p.surface("front")
        val q = refQuad(p, "front", 300.0, 0.0, 905.0, 2053.0)
        setRef(p, "front", q, 905.0, 2053.0)
        val f = SurveyPrep.frameOf("front", p, L, W, H)
        assertNotNull(SurveyRef.faceQuad(sf, f))
        assertEquals(false, SurveyRef.setShown(sf, null))
        assertNull(SurveyRef.faceQuad(sf, SurveyPrep.frameOf("front", p, L, W, H)), "hidden: not drawn on the faces")
        assertNotNull(SurveyPrep.frameOf("front", p, L, W, H)!!.ref, "hidden: still squaring")
        assertEquals("Ref 905 × 2053", SurveyRef.tabText(sf), "the tab still says so")
        assertEquals("Reference hidden on the faces — it still squares the front wall", SurveyRef.shownSaid("front", false))
        assertEquals("Reference shown on 2 · Mark, 3 · List and 4 · 360", SurveyRef.shownSaid("front", true))
        assertEquals(true, SurveyRef.setShown(sf, null))
        assertNull(SurveyRef.tabText(p.surface("right")))
        setRef(p, "right", null, 905.0, 2053.0)
        assertNull(SurveyRef.tabText(p.surface("right")), "not placed: no subtitle")
    }

    @Test fun `it is never an item -- not counted, not cleared, not deleted, kept by Start over locked or not`() {
        for (locked in listOf(false, true)) for (shown in listOf(true, false)) {
            val p = room()
            setRef(p, "front", refQuad(p, "front", 300.0, 0.0, 905.0, 2053.0), 905.0, 2053.0,
                extra = mapOf("shown" to shown, "locked" to locked))
            val kept = p.surface("front").extra[SurveyRef.KEY]
            p.addMark("front", "D", W, H, "d1")
            assertEquals(1, SurveyPrepEdit.clearCount(p, SurveyPrepEdit.ROOM, "front"), "the reference is not counted")
            assertEquals(0, SurveyPrepEdit.lockedCount(p, SurveyPrepEdit.ROOM, "front"))
            assertTrue(p.itemsOf("front").none { SurveyRef.isId(it.id) })
            assertNull(p.itemById(SurveyRef.id("front")))
            assertFalse(p.deleteMark(SurveyRef.id("front")))
            val c = SurveyPrepEdit.clearItems(p, SurveyPrepEdit.SURF, "front")
            assertEquals(1, c.n)
            assertSame(kept, p.surface("front").extra[SurveyRef.KEY], "Clear this surface keeps it")
            SurveyPrepEdit.clearItems(p, SurveyPrepEdit.ROOM, "front")
            assertSame(kept, p.surface("front").extra[SurveyRef.KEY], "Clear whole room keeps it")
            p.addMark("front", "W", W, H, "w1")
            val r = SurveyPrepEdit.startOver(p, L, W, H)
            assertEquals(kept, r.prep.surface("front").extra[SurveyRef.KEY], "Start over keeps it (locked $locked, shown $shown)")
            assertEquals(if (locked) 1 else 0, r.kept, "a locked reference counts as kept")
            assertTrue(r.prep.surface("front").marks.isEmpty())
            val back = SurveyRef.of(r.prep.surface("front"))!!
            assertEquals(905.0, back.w); assertEquals(2053.0, back.h); assertEquals(shown, back.shown); assertEquals(locked, back.locked)
            assertNotNull(SurveyPrep.frameOf("front", r.prep, L, W, H)!!.ref, "and it still squares the wall")
        }
    }

    @Test fun `locked, picked and menued by its id -- Lock and Cancel only, under Front wall reference`() {
        val p = room()
        setRef(p, "front", refQuad(p, "front", 300.0, 0.0, 905.0, 2053.0), 905.0, 2053.0)
        val id = SurveyRef.id("front")
        assertEquals("ref:front", id); assertTrue(SurveyRef.isId(id)); assertFalse(SurveyRef.isId("ref:list")); assertFalse(SurveyRef.isId("FD1"))
        assertEquals("Front wall reference", SurveyPrepEdit.menuHeaderOf(p, id))
        assertEquals(listOf("Lock", "Cancel"), SurveyPrepEdit.menuItemsOf(p, id).map { it.second })
        assertEquals(true, SurveyPrepEdit.setLocked(p, id, null))
        assertEquals("🔒 Front wall reference", SurveyPrepEdit.menuHeaderOf(p, id))
        assertEquals(listOf("Unlock", "Cancel"), SurveyPrepEdit.menuItemsOf(p, id).map { it.second })
        assertFalse(SurveyRef.moveCorner(p.surface("front"), 0, 1.0, 1.0), "a locked reference does not move")
        assertEquals("Front wall reference is locked — unlock first", SurveyRef.lockedSaid("front"))
        assertEquals("The reference stays — hide it with the eye in 1 · Lines", SurveyRef.NO_DELETE)
        assertNull(SurveyPrepEdit.lockGroupOf(p, id))
        // turned off, it is no item to pick
        SurveyRef.setMode(p.surface("front"), "front", false, null)
        assertNull(SurveyRef.byId(p, id)); assertNull(SurveyPrepEdit.menuHeaderOf(p, id))
    }

    @Test fun `1 Lines hit order -- ref corner, locked ref, line end, line body, inside`() {
        val dp = 2.0
        val corners = listOf(doubleArrayOf(100.0, 300.0), doubleArrayOf(300.0, 300.0), doubleArrayOf(300.0, 100.0), doubleArrayOf(100.0, 100.0))
        val ends = mapOf("bottom" to listOf(doubleArrayOf(115.0, 305.0), doubleArrayOf(500.0, 310.0)),
            "top" to listOf(doubleArrayOf(0.0, 20.0), doubleArrayOf(500.0, 20.0)),
            "left" to listOf(doubleArrayOf(10.0, 0.0), doubleArrayOf(10.0, 500.0)),
            "right" to listOf(doubleArrayOf(600.0, 0.0), doubleArrayOf(600.0, 500.0)))
        fun body(x: Double, y: Double): (String) -> Double = { s -> when (s) { "bottom" -> abs(y - 310); "top" -> abs(y - 20); "left" -> abs(x - 10); else -> abs(x - 600) } }
        fun hit(x: Double, y: Double, locked: Boolean = false, lines: Boolean = false, c: List<DoubleArray>? = corners) =
            SurveyRef.linesHit(c, locked, ends, body(x, y), c != null && SurveyRef.inside(x, y, c), lines, x, y, dp)
        // a ref corner beats the line end 14 px away
        assertEquals(0, assertIs<SurveyRef.LinesHit.Corner>(hit(102.0, 302.0)).i)
        assertIs<SurveyRef.LinesHit.RefLocked>(hit(102.0, 302.0, locked = true))
        // the corner 44 px away is past 20 dp: the line end 28 px away takes it
        assertEquals("bottom", assertIs<SurveyRef.LinesHit.End>(hit(140.0, 318.0)).slot)
        assertIs<SurveyRef.LinesHit.Body>(hit(450.0, 300.0))
        assertIs<SurveyRef.LinesHit.Inside>(hit(200.0, 200.0))
        assertNull(hit(450.0, 200.0))
        // locked lines: on a line, refused; inside the reference, picked
        assertIs<SurveyRef.LinesHit.LinesLocked>(hit(450.0, 300.0, lines = true))
        assertIs<SurveyRef.LinesHit.Inside>(hit(200.0, 200.0, lines = true))
        // no reference on: nothing of it is hit
        assertNull(hit(200.0, 200.0, c = null))
    }

    @Test fun `a reference round-trips the prep file, and an older file without one is unchanged`() {
        val p = room()
        setRef(p, "front", refQuad(p, "front", 300.0, 0.0, 905.0, 2053.0), 905.0, 2053.0, extra = mapOf("shown" to false, "locked" to true, "later" to 7))
        val o = SurveyPrepFile.encode(p, L, W, H)
        val d = assertNotNull(SurveyPrepFile.decode(o))
        assertTrue(d.problems.isEmpty())
        val r = assertNotNull(SurveyRef.of(d.prep.surface("front")))
        assertTrue(r.on && !r.shown && r.locked); assertEquals(905.0, r.w); assertEquals(2053.0, r.h)
        assertEquals(SurveyRef.of(p.surface("front"))!!.q!!.map { it.toList() }, r.q!!.map { it.toList() })
        assertEquals(7, (d.prep.surface("front").extra[SurveyRef.KEY] as Map<*, *>)["later"], "its keys this build does not read are kept")
        assertEquals(SurveyPrepFile.encode(d.prep, L, W, H), o, "the second write is the first")
        val f1 = SurveyPrep.frameOf("front", p, L, W, H)!!; val f2 = SurveyPrep.frameOf("front", d.prep, L, W, H)!!
        assertEquals(f1.key(), f2.key())
        // no reference: no key written
        val plain = SurveyPrepFile.encode(room(), L, W, H)
        @Suppress("UNCHECKED_CAST")
        assertFalse(((plain["w"] as Map<String, Any?>)["front"] as Map<String, Any?>).containsKey(SurveyRef.KEY))
    }

    @Test fun `marks outside the face are judged in its own size -- the reference's when it squares it`() {
        val p = room()
        setRef(p, "front", refQuad(p, "front", 900.0, 400.0, 1000.0, 1200.0), 900.0, 1080.0)
        val e = p.addMark("front", "W", W, H, "w1")
        e.x0 = W * 0.9 + 50; e.x1 = e.x0 + 100
        assertEquals(listOf("front" to "FW1"), p.outside(L, W, H).map { it.first to it.second.code })
    }

    /** THE GUARD THE PLAN NAMES (slice 12: "mixed frames would misplace marks with no error at all"): no file of the app
     *  squares a face by itself -- the laser-only frame and size are reached only through SurveyPrep.frameOf / sizeOf. */
    @Test fun `the app squares a face only through the one frameOf and sizeOf`() {
        val dir = File("../app/src/main/kotlin/com/malletcrafts/interiorestimate")
        assertTrue(dir.isDirectory, "the app's sources are beside :pano (${dir.absolutePath})")
        val bad = dir.walk().filter { it.isFile && it.extension == "kt" }.flatMap { f ->
            f.readLines().withIndex().filter { (_, l) -> !l.trimStart().startsWith("//") && !l.trimStart().startsWith("*") &&
                (l.contains("SurveyPrep.frame(") || l.contains("SurveyPrep.size(") || l.contains("laserFrame(")) }
                .map { "${f.name}:${it.index + 1}" } }.toList()
        assertTrue(bad.isEmpty(), "a face squared outside SurveyPrep.frameOf / sizeOf: $bad")
        val uses = dir.walk().filter { it.isFile && it.extension == "kt" }.map { it.name to it.readText() }.toMap()
        for (f in listOf("SurveyPrepScreen.kt", "FaceWriter.kt", "AsIsUpload.kt"))
            assertTrue(uses.getValue(f).contains("SurveyPrep.frameOf("), "$f squares through SurveyPrep.frameOf")
        assertTrue(uses.getValue("AsIsUpload.kt").contains("SurveyPrep.sizeOf("), "the upload's a x b is SurveyPrep.sizeOf")
    }

    /** Slice 12 review: corners dragged out of order would square the face MIRRORED -- its crossings at negative y, every
     *  picture sampling the floor below the wall -- with ordinary figures and no error. Refused: the laser squares it. */
    @Test fun `a reference dragged upside down, mirrored or crossed is refused -- the edges and the laser square the face`() {
        val p0 = room(); val q = refQuad(p0, "front", 300.0, 0.0, 905.0, 2053.0)
        // the right way round squares it, crossings at y >= 0
        val ok = room(); setRef(ok, "front", q, 905.0, 2053.0)
        val fo = assertNotNull(SurveyPrep.frameOf("front", ok, L, W, H)); assertNotNull(fo.ref)
        assertTrue(assertNotNull(fo.mm).all { it[1] > -1.0 }, "crossings above the floor")
        assertTrue(SurveyRef.convex(q))
        for ((why, bad) in listOf(
            "upside down (top corners dragged onto the bottom)" to listOf(q[3], q[2], q[1], q[0]),
            "mirrored (left and right swapped)" to listOf(q[1], q[0], q[3], q[2]),
            "a bow-tie (bottom-right and top-right swapped)" to listOf(q[0], q[2], q[1], q[3]))) {
            val p = room(); setRef(p, "front", bad, 905.0, 2053.0)
            val f = assertNotNull(SurveyPrep.frameOf("front", p, L, W, H), why)
            assertNull(f.ref, why); assertEquals(W, f.a, why); assertEquals(H, f.b, why)
            assertEquals(listOf(W, H), SurveyPrep.sizeOf("front", p, L, W, H).toList(), why)
            assertNull(SurveyRef.frameFromReference(p.surface("front").lines, SurveyRef.of(p.surface("front"))), why)
            assertEquals(SurveyRef.PLACE_HINT, SurveyRef.refText("front", p, L, W, H).first, why)
        }
        assertFalse(SurveyRef.convex(listOf(q[0], q[2], q[1], q[3])), "a bow-tie is not convex")
        assertTrue(SurveyRef.area(q) * SurveyRef.area(listOf(q[3], q[2], q[1], q[0])) < 0, "a flip turns the winding")
    }

    /** Slice 12 review: who takes the next DISTO reading, decided when it ARRIVES. */
    @Test fun `a reference box's D2 -- its reading goes to that box, a left box sets it aside, nothing aimed at a box becomes a mark's`() {
        val p = room(); val q = refQuad(p, "front", 300.0, 0.0, 905.0, 2053.0); setRef(p, "front", q, null, null)
        val t0 = 1_000_000L
        // nothing armed: the active measurement's, or "tap a measurement first"
        assertSame(SurveyRef.Route.Normal, SurveyRef.route(null, p, 905, t0))
        // armed: into that box, never a reading key
        val shot = SurveyRef.arm("front", "w", t0)
        assertTrue(SurveyRef.armed(shot, "front", "w")); assertFalse(SurveyRef.armed(shot, "front", "h"))
        assertFalse(SurveyRef.armed(shot, "right", "w"))
        val r = assertIs<SurveyRef.Route.Ref>(SurveyRef.route(shot, p, 905, t0 + 800))
        assertEquals("front", r.s); assertEquals("w", r.k)
        assertTrue(SurveyRef.setDim(p.surface(r.s), r.k, 905.0)); assertTrue(p.actual.isEmpty(), "never a reading")
        // a tab change, a step change, Size from switched or an undo LEAVES the box: no longer armed, and a reading
        // already on its way is set aside and said -- never taken by the active measurement
        val left = assertNotNull(SurveyRef.leave(shot))
        assertFalse(SurveyRef.armed(left, "front", "w"))
        val d = assertIs<SurveyRef.Route.Drop>(SurveyRef.route(left, p, 2053, t0 + 1200))
        assertEquals("D2 2053 mm — not used: the reference box was left", d.note)
        // long after, a stale shot takes nothing: the reading is the active measurement's again
        assertSame(SurveyRef.Route.Normal, SurveyRef.route(left, p, 2053, t0 + SurveyRef.SHOT_MS + 1))
        assertNull(SurveyRef.leave(null))
        // armed, but its reference undone away (or switched off): falls through to "tap a measurement first"
        val gone = room()
        assertSame(SurveyRef.Route.Normal, SurveyRef.route(shot, gone, 905, t0 + 100))
        val off = room(); setRef(off, "front", q, null, null, on = false)
        assertSame(SurveyRef.Route.Normal, SurveyRef.route(shot, off, 905, t0 + 100))
    }

    @Test fun `Size from on the whole prep places the first reference through the laser frame`() {
        val p = room()
        assertTrue(SurveyRef.setMode(p, "front", true, L, W, H))
        val lf = assertNotNull(SurveyPrep.laserFrame("front", p, L, W, H))
        val want = SurveyRef.refDefaultQuad("front", lf)
        val got = assertNotNull(SurveyRef.of(p.surface("front"))?.q)
        for (i in 0 until 4) assertTrue(hypot(want[i][0] - got[i][0], want[i][1] - got[i][1]) < 1e-9, "corner $i")
        assertFalse(SurveyRef.setMode(p, "front", true, L, W, H), "already on: no change, no undo step")
    }

    @Test fun `the reference's words`() {
        assertEquals("Edges · laser", SurveyRef.EDGES); assertEquals("Reference ▭", SurveyRef.REFERENCE)
        assertEquals("Show on faces", SurveyRef.EYE_LABEL)
        val p = room(); val sf = p.surface("front")
        SurveyRef.setMode(sf, "front", true, null)
        assertEquals("Ref ? × ?", SurveyRef.label(SurveyRef.of(sf)!!))
        assertEquals(SurveyRef.PLACE_HINT to false, SurveyRef.refText("front", null, doubleArrayOf(W, H)))
        setRef(p, "front", refQuad(p, "front", 300.0, 0.0, 905.0, 2053.0), 905.0, 2053.0)
        val t = SurveyRef.refText("front", SurveyPrep.frameOf("front", p, L, W, H), doubleArrayOf(W, H))
        assertEquals("Measured W 2678 × H 2677 · laser 2678 × 2677 ✓", t.first); assertFalse(t.second)
        setRef(p, "front", refQuad(p, "front", 900.0, 400.0, 1000.0, 1200.0), 900.0, 1080.0)
        val u = SurveyRef.refText("front", SurveyPrep.frameOf("front", p, L, W, H), doubleArrayOf(W, H))
        assertTrue(u.second && u.first.endsWith(" · differs by −268 / −268 mm"), u.first)
    }
}
