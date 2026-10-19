package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A WALL'S VIEW REACHES ITS CORNERS (sp.js 60-97: AMIN / AMAX / ABEYOND, xAt, aAt, lim, needExt, ensureCut, cutView,
 * fitRectFor). Amit, 11 Oct 2026: "right side of wall is cropped. i cant see both right side corners." / "right side
 * corners not visible in foto"; 19 Oct, the htu Living Room right face again. The bare 120-degree view stops 60 degrees
 * off the wall's centre, so a corner between 60 and 84 degrees was not in the phone's picture at all.
 */
class ExtCutTest {

    private fun close(exp: Double, got: Double, tol: Double, what: String) =
        assertTrue(abs(exp - got) <= tol, "$what: expected $exp, got $got")

    /** A small 360 whose every pixel differs, so two samples agree only when they look the same way. */
    private val pano = Panorama.Image(360, 180, IntArray(360 * 180) { i -> (0xFF shl 24) or ((i * 2654435761L).toInt() and 0xFFFFFF) })

    /** Lines whose crossings are exactly [q] (bottom-left, bottom-right, top-right, top-left), as a careful hand sets them. */
    private fun linesThrough(q: List<DoubleArray>): Map<String, Array<DoubleArray>> =
        mapOf("bottom" to arrayOf(q[0], q[1]), "top" to arrayOf(q[3], q[2]), "left" to arrayOf(q[0], q[3]), "right" to arrayOf(q[1], q[2]))

    /** Four line points at view x [xl] (left corner) and [xr] (right corner), 25 % and 75 % up the view. */
    private fun cornersAt(xl: Double, xr: Double) = linesThrough(listOf(
        doubleArrayOf(xl, 600.0), doubleArrayOf(xr, 600.0), doubleArrayOf(xr, 200.0), doubleArrayOf(xl, 200.0)))

    /**
     * THE htu RIGHT WALL'S SHAPE: the camera 800 mm from the right wall, its front end 1000 mm ahead and its back end
     * 2500 mm behind, floor 1300 mm down, ceiling 1400 mm up -- so its right (back) corner is atan(2500 / 800) = 72.3
     * degrees off the wall's centre, 12.3 degrees past the bare view.
     */
    private val htuRight: List<DoubleArray> = run {
        fun p(x: Double, y: Double, z: Double) = assertNotNull(SurveyPrep.proj("right", x, y, z))
        listOf(p(800.0, -1300.0, 1000.0), p(800.0, -1300.0, -2500.0), p(800.0, 1400.0, -2500.0), p(800.0, 1400.0, 1000.0))
    }

    // ------------------------------------------------------------ xAt / aAt

    @Test fun `xAt and aAt are each other's inverse, and the bare view is plus or minus 60 degrees`() {
        for (a in -84..84) close(a.toDouble(), SurveyPrep.aAt(SurveyPrep.xAt(a.toDouble())), 1e-9, "aAt(xAt($a))")
        for (x in listOf(-2000.0, -288.0, 0.0, 123.4, 480.0, 960.0, 1248.0, 3000.0))
            close(x, SurveyPrep.xAt(SurveyPrep.aAt(x)), 1e-7, "xAt(aAt($x))")
        close(480.0, SurveyPrep.xAt(0.0), 1e-9, "the centre")
        close(960.0, SurveyPrep.xAt(60.0), 1e-9, "the bare view's right edge")
        close(0.0, SurveyPrep.xAt(-60.0), 1e-9, "the bare view's left edge")
        // xAt agrees with the projection itself: a direction a degrees right of the front wall's centre
        val a = Math.toRadians(75.0)
        close(SurveyPrep.xAt(75.0), assertNotNull(SurveyPrep.proj("front", kotlin.math.sin(a), 0.0, kotlin.math.cos(a)))[0], 1e-7, "proj at 75°")
    }

    // ------------------------------------------------------------ lim

    @Test fun `lim is a wall's plus or minus 84 degrees across and a 30 percent border on a plan`() {
        val w = SurveyPrep.lim("right")
        close(SurveyPrep.xAt(-84.0), w[0], 1e-9, "wall x0"); close(-240.0, w[1], 1e-9, "wall y0")
        close(SurveyPrep.xAt(84.0), w[2], 1e-9, "wall x1"); close(1040.0, w[3], 1e-9, "wall y1")
        assertTrue(w[2] > 3000 && w[0] < -2000, "±84° is far past the 960 view: ${w.joinToString()}")
        val p = SurveyPrep.lim("floor")
        assertContentEquals(doubleArrayOf(-288.0, -288.0, 1248.0, 1248.0).toList(), p.map { (it * 1e6).roundToInt() / 1e6 }, "plan")
    }

    // ------------------------------------------------------------ needExt

    @Test fun `needExt is the 30 percent border with no lines, on a plan, or with lines near the centre`() {
        val base = intArrayOf(-288, -240, 1248, 1040)
        assertContentEquals(base, SurveyPrep.baseExt("front"))
        assertContentEquals(base, SurveyPrep.needExt("front", null, null), "no lines")
        assertContentEquals(intArrayOf(-288, -288, 1248, 1248), SurveyPrep.needExt("ceil", cornersAt(-500.0, 2000.0), null), "a plan")
        // corners 40° each side: 55° rounds to 55 and is raised to AMIN, whose 70° is inside the border already
        assertContentEquals(base, SurveyPrep.needExt("back", cornersAt(SurveyPrep.xAt(-40.0), SurveyPrep.xAt(40.0)), null), "near")
    }

    @Test fun `needExt clamps at AMIN for near corner lines and at AMAX for far ones`() {
        // a far right corner (75°): 75 + 15 = 90, capped at 84; a near left one (-20°): 35 -> 70, inside the border
        val e = SurveyPrep.needExt("right", cornersAt(SurveyPrep.xAt(-20.0), SurveyPrep.xAt(75.0)), null)
        assertEquals(ceil(SurveyPrep.xAt(84.0)).toInt(), e[2], "right capped at 84°")
        assertEquals(-288, e[0], "left at the border (AMIN's 70° is inside it)")
        assertEquals(-240, e[1]); assertEquals(1040, e[3])
        // a far left corner too, mirrored
        val f = SurveyPrep.needExt("left", cornersAt(SurveyPrep.xAt(-80.0), SurveyPrep.xAt(75.0)), null)
        assertEquals(floor(SurveyPrep.xAt(-84.0)).toInt(), f[0], "left capped at -84°")
    }

    @Test fun `needExt reaches ABEYOND past the outermost point, rounded out to 5 degrees`() {
        // 58 + 15 = 73 -> 75
        assertEquals(ceil(SurveyPrep.xAt(75.0)).toInt(), SurveyPrep.needExt("front", cornersAt(400.0, SurveyPrep.xAt(58.0)), null)[2], "58°")
        // 61 + 15 = 76 -> 80
        assertEquals(ceil(SurveyPrep.xAt(80.0)).toInt(), SurveyPrep.needExt("front", cornersAt(400.0, SurveyPrep.xAt(61.0)), null)[2], "61°")
        // exactly 60 + 15 = 75 stays 75
        assertEquals(ceil(SurveyPrep.xAt(75.0)).toInt(), SurveyPrep.needExt("front", cornersAt(400.0, SurveyPrep.xAt(60.0)), null)[2], "60°")
        // the frame's corners count as well as the line points (sp.js: F.q pushed onto xs)
        val q = listOf(doubleArrayOf(400.0, 600.0), doubleArrayOf(SurveyPrep.xAt(66.0), 600.0), doubleArrayOf(SurveyPrep.xAt(66.0), 200.0), doubleArrayOf(400.0, 200.0))
        assertEquals(ceil(SurveyPrep.xAt(84.0)).toInt(), SurveyPrep.needExt("front", cornersAt(400.0, 800.0), q)[2], "a frame corner at 66°: 81 -> 85, capped")
    }

    // ------------------------------------------------------------ the regression this is for

    @Test fun `a corner 75 degrees off centre is OUTSIDE the bare view and INSIDE the extended cut`() {
        val x75 = SurveyPrep.xAt(75.0); val y = 600.0
        val bare = SurveyPrep.cutView(pano, "right")
        assertTrue(x75 >= bare.width, "the bare 960 view stops short of it: x = $x75, width ${bare.width}")
        assertTrue(x75 >= SurveyPrep.baseExt("right")[2], "so does the 30 % border alone")
        val cut = SurveyPrep.cutView(pano, "right", SurveyPrep.needExt("right", cornersAt(SurveyPrep.xAt(-30.0), x75), null))
        val px = (x75 + cut.ox).toInt(); val py = (y + cut.oy).toInt()
        assertTrue(px in 0 until cut.img.width && py in 0 until cut.img.height, "in the cut: ($px, $py) of ${cut.img.width} x ${cut.img.height}")
        // and what is there is the 360 in that very direction
        assertEquals(WallCorners.sample(pano, SurveyPrep.ray("right", px - cut.ox + 0.5, py - cut.oy + 0.5)), cut.img.pixels[py * cut.img.width + px])
    }

    @Test fun `the htu Right wall - the cut reaches past 70 degrees and the fit shows 10 degrees beyond its right corner (ct52 F)`() {
        val lines = linesThrough(htuRight)
        val corner = SurveyPrep.aAt(htuRight[1][0])
        close(Math.toDegrees(kotlin.math.atan(2500.0 / 800)), corner, 1e-9, "the right corner's angle")
        assertTrue(htuRight[1][0] > SurveyPrep.VW, "outside the bare view")
        val e = SurveyPrep.needExt("right", lines, htuRight)
        val cutR = SurveyPrep.aAt(e[2].toDouble())
        assertTrue(cutR > 70, "the cut reaches $cutR°")
        val fr = SurveyPrep.fitRectFor("right", lines, htuRight, e)
        assertTrue(SurveyPrep.aAt(fr[2]) >= corner + 10, "fit reaches ${SurveyPrep.aAt(fr[2])}°, the corner is at $corner°")
        // both right corners, floor and ceiling, are in the fit and in the cut
        for (i in listOf(1, 2)) {
            val q = htuRight[i]
            assertTrue(q[0] >= fr[0] && q[0] <= fr[2] && q[1] >= fr[1] && q[1] <= fr[3], "corner $i in the fit")
            assertTrue(q[0] >= e[0] && q[0] < e[2] && q[1] >= e[1] && q[1] < e[3], "corner $i in the cut")
        }
    }

    @Test fun `a corner line moved to 72 degrees grows the cut, capped at 84 (ct52 F)`() {
        val before = SurveyPrep.needExt("right", cornersAt(SurveyPrep.xAt(-30.0), SurveyPrep.xAt(50.0)), null)
        val b = SurveyPrep.aAt(before[2].toDouble()).roundToInt()
        val need = SurveyPrep.needExt("right", cornersAt(SurveyPrep.xAt(-30.0), SurveyPrep.xAt(72.0)), null)
        val grown = assertNotNull(SurveyPrep.growExt(before, need), "it grows")
        val a = SurveyPrep.aAt(grown[2].toDouble()).roundToInt()
        assertTrue(a > b && a <= 84, "$b° -> $a°")
    }

    // ------------------------------------------------------------ ensureCut: grows, never shrinks

    @Test fun `growExt is null when the cut already holds what is needed, the union when it does not`() {
        val have = intArrayOf(-288, -240, 2000, 1040)
        assertNull(SurveyPrep.growExt(have, intArrayOf(-288, -240, 1500, 1040)), "narrower: kept, never shrunk")
        assertNull(SurveyPrep.growExt(have, intArrayOf(-288, -240, 2000, 1040)), "the same")
        assertContentEquals(intArrayOf(-900, -240, 2000, 1040), SurveyPrep.growExt(have, intArrayOf(-900, -240, 1248, 1040)), "wider on the left only: the right is kept")
        assertContentEquals(intArrayOf(-288, -240, 3000, 1040), SurveyPrep.growExt(have, intArrayOf(-288, -240, 3000, 1040)))
        assertContentEquals(intArrayOf(-288, -240, 1248, 1040), SurveyPrep.growExt(null, intArrayOf(-288, -240, 1248, 1040)), "none yet")
    }

    // ------------------------------------------------------------ cutView over an extent

    @Test fun `the extended cut is the bare view's projection, offset by ox and oy`() {
        val bare = SurveyPrep.cutView(pano, "left")
        val cut = SurveyPrep.cutView(pano, "left", SurveyPrep.baseExt("left"))
        assertEquals(288, cut.ox); assertEquals(240, cut.oy)
        assertEquals(1536, cut.img.width); assertEquals(1280, cut.img.height)
        for (y in 0 until bare.height step 37) for (x in 0 until bare.width step 41)
            assertEquals(bare.pixels[y * bare.width + x], cut.img.pixels[(y + cut.oy) * cut.img.width + x + cut.ox], "view ($x, $y)")
        // a plan too: the same 30 % border
        val fl = SurveyPrep.cutView(pano, "floor", SurveyPrep.baseExt("floor"))
        assertEquals(1536, fl.img.width); assertEquals(1536, fl.img.height); assertEquals(288, fl.ox)
    }

    // ------------------------------------------------------------ fitRectFor (sp.js 88-97)

    @Test fun `fit holds the view and the surface, 12 degrees beyond a wall's outermost points, never past the cut`() {
        // corners inside the view: the view itself
        val inner = cornersAt(300.0, 700.0)
        assertContentEquals(doubleArrayOf(0.0, 0.0, 960.0, 800.0).toList(), SurveyPrep.fitRectFor("front", inner, null, SurveyPrep.baseExt("front")).toList())
        // a right corner at 70°: 12° beyond is 82°, but a base cut stops it at the cut's edge
        val far = cornersAt(300.0, SurveyPrep.xAt(70.0))
        close(1248.0, SurveyPrep.fitRectFor("front", far, null, SurveyPrep.baseExt("front"))[2], 1e-9, "the base cut's edge")
        val wide = SurveyPrep.needExt("front", far, null)
        close(SurveyPrep.xAt(82.0), SurveyPrep.fitRectFor("front", far, null, wide)[2], 1e-7, "82° in a cut to 84°")
        // 78° + 12 = 90 is capped at AMAX
        val farther = cornersAt(300.0, SurveyPrep.xAt(78.0))
        close(SurveyPrep.xAt(84.0), SurveyPrep.fitRectFor("front", farther, null, intArrayOf(-288, -240, 5000, 1040))[2], 1e-7, "capped at 84°")
        // up and down: 4 % of the longer span beyond the points
        val tall = linesThrough(listOf(doubleArrayOf(100.0, 900.0), doubleArrayOf(800.0, 900.0), doubleArrayOf(800.0, -50.0), doubleArrayOf(100.0, -50.0)))
        val t = SurveyPrep.fitRectFor("front", tall, null, null)
        close(-50.0 - .04 * 950, t[1], 1e-9, "top"); close(900.0 + .04 * 950, t[3], 1e-9, "bottom")
        // the frame's corners, when there is a frame, rather than the line points
        val q = listOf(doubleArrayOf(100.0, 700.0), doubleArrayOf(SurveyPrep.xAt(65.0), 700.0), doubleArrayOf(SurveyPrep.xAt(65.0), 100.0), doubleArrayOf(100.0, 100.0))
        close(SurveyPrep.xAt(77.0), SurveyPrep.fitRectFor("front", inner, q, intArrayOf(-288, -240, 5000, 1040))[2], 1e-7, "frame corner 65° + 12")
        // a plan: 4 % across as well
        val pl = linesThrough(listOf(doubleArrayOf(-100.0, 1000.0), doubleArrayOf(1100.0, 1000.0), doubleArrayOf(1100.0, 0.0), doubleArrayOf(-100.0, 0.0)))
        val p = SurveyPrep.fitRectFor("floor", pl, null, SurveyPrep.baseExt("floor"))
        close(-100.0 - .04 * 1200, p[0], 1e-9, "plan left"); close(1100.0 + .04 * 1200, p[2], 1e-9, "plan right")
    }

    // ------------------------------------------------------------ the squared face and the junctions reach as far

    @Test fun `a junction past the bare view but inside lim counts, one past 84 degrees does not`() {
        val lines = cornersAt(300.0, SurveyPrep.xAt(75.0))
        val j = assertNotNull(SurveyPrep.junctions("right", lines, "right"), "the right corner's two junctions at 75°")
        assertTrue(j.all { it[0] > SurveyPrep.VW })
        assertNull(SurveyPrep.junctions("right", cornersAt(300.0, SurveyPrep.xAt(86.0)), "right"), "past ±84°")
    }

    /** A 360 lit everywhere, so a black pixel in a squared face can only be one outside the clip. */
    private val lit = Panorama.Image(64, 32, IntArray(64 * 32) { 0xFF808080.toInt() })
    private val bareClip = doubleArrayOf(0.0, 0.0, SurveyPrep.VW.toDouble(), SurveyPrep.VH.toDouble())

    @Test fun `the squared face is drawn as far as lim, where the bare view would leave it black`() {
        val f = assertNotNull(SurveyPrep.frame(linesThrough(htuRight), 2800.0, 2700.0))
        val sq = SurveyPrep.Square.screen(f.a, f.b)
        fun black(img: Panorama.Image) = img.pixels.count { it == 0 }
        val bare = SurveyPrep.render(lit, "right", f, sq, clip = bareClip)
        val wide = SurveyPrep.render(lit, "right", f, sq)
        assertContentEquals(wide.pixels, SurveyPrep.render(lit, "right", f, sq, clip = SurveyPrep.lim("right")).pixels, "lim is the default")
        assertTrue(black(wide) < black(bare), "lim draws more: ${black(wide)} black vs ${black(bare)}")
        // the face's right end (its right corner, 72.3°) is drawn: the pixel just inside the bottom-right corner
        val px = sq.x(f.a - 20).toInt(); val py = sq.y(20.0).toInt()
        assertEquals(0, bare.pixels[py * sq.width + px], "black in the bare view")
        assertTrue(wide.pixels[py * sq.width + px] != 0, "drawn within lim")
    }

    @Test fun `the saved elevation and the as-is upload show the htu right corner the screen shows - one squaring rule, as the prototype`() {
        val f = assertNotNull(SurveyPrep.frame(linesThrough(htuRight), 2800.0, 2700.0))
        // the S5 upload (AsIsUpload): squared, no margin -- not one black pixel inside the surface, both right corners in
        val asis = SurveyPrep.renderAsIs(lit, "right", f)
        assertEquals(0, asis.pixels.count { it == 0 }, "the as-is face is drawn edge to edge")
        val aq = SurveyPrep.asisSquare(f.a, f.b)
        for (mmY in listOf(20.0, f.b - 20)) {
            val px = aq.x(f.a - 20).toInt(); val py = aq.y(mmY).toInt()
            assertTrue(asis.pixels[py * aq.width + px] != 0, "as-is: just inside the right corner at $mmY mm up")
            assertNotNull(SurveyPrep.asisDirection("right", f, aq, px + 0.5, py + 0.5), "asisDirection agrees")
        }
        // the gallery elevation (FaceWriter.writeSurvey): the same call, the elevation square
        val eq = SurveyPrep.Square.elevation(f.a, f.b, 1600, 0.06)
        val el = SurveyPrep.render(lit, "right", f, eq)
        for (mmY in listOf(20.0, f.b - 20)) {
            val px = eq.x(f.a - 20).toInt(); val py = eq.y(mmY).toInt()
            assertTrue(el.pixels[py * eq.width + px] != 0, "elevation: just inside the right corner at $mmY mm up")
        }
    }

    // ------------------------------------------------------------ the photo box (sp.js V.clamp)

    @Test fun `zoomed or panned, the centre may go anywhere over the picture and no further`() {
        assertEquals(1.0 / 3, SurveyPrep.ZMIN); assertEquals(8.0, SurveyPrep.ZMAX)
        close(-288.0, SurveyPrep.clampOver(-1000.0, 288.0, 1536.0), 0.0, "left edge of the cut")
        close(1248.0, SurveyPrep.clampOver(5000.0, 288.0, 1536.0), 0.0, "right edge of the cut")
        close(-100.0, SurveyPrep.clampOver(-100.0, 288.0, 1536.0), 0.0, "free, in the border")
        close(0.0, SurveyPrep.clampOver(-5.0, 0.0, 960.0), 0.0, "a squared face has no offset")
    }

    // ------------------------------------------------------------ the photo box's view (sp.js Viewer; ct52 F)

    /** A portrait photo box on the Mi A3, px. */
    private val bw = 720.0; private val bh = 1000.0

    /** The htu Right wall as 1 · Lines shows it: its cut, and the box over it fitted to the surface. */
    private fun htuBox(): SurveyPrep.PhotoBox {
        val lines = linesThrough(htuRight)
        val e = SurveyPrep.needExt("right", lines, htuRight)
        val fr = SurveyPrep.fitRectFor("right", lines, htuRight, e)
        return SurveyPrep.PhotoBox(bw, bh, fr, -e[0].toDouble(), -e[1].toDouble(), (e[2] - e[0]).toDouble(), (e[3] - e[1]).toDouble())
    }

    @Test fun `fitted, the box shows the fit rect whole, centred, and 10 degrees beyond the htu right corner (ct52 F)`() {
        val b = htuBox(); val v = SurveyPrep.PhotoView()
        assertTrue(v.fitted); assertEquals(1.0, v.zoom()); assertNull(v.label(), "no zoom label while fitted")
        close((b.fr[0] + b.fr[2]) / 2, v.centreX(b), 1e-9, "centred on the fit rect")
        close(SurveyPrep.fitScale(bw, bh, b.fr[2] - b.fr[0], b.fr[3] - b.fr[1]), v.k(b), 1e-12, "k is the fit rect's scale")
        // every corner of the fit rect is inside the box
        for (x in listOf(b.fr[0], b.fr[2])) for (y in listOf(b.fr[1], b.fr[3])) {
            val s = v.toS(b, x, y); assertTrue(s[0] >= -1e-6 && s[0] <= bw + 1e-6 && s[1] >= -1e-6 && s[1] <= bh + 1e-6, "(${s[0]}, ${s[1]}) in the box")
        }
        val corner = SurveyPrep.aAt(htuRight[1][0])
        val edge = SurveyPrep.aAt(v.fromS(b, bw, bh / 2)[0])
        assertTrue(edge >= corner + 10, "the box's right edge is at $edge°, the corner at $corner°")
        // toS and fromS are each other's inverse
        val p = v.fromS(b, 123.0, 456.0); val q = v.toS(b, p[0], p[1])
        close(123.0, q[0], 1e-9, "x"); close(456.0, q[1], 1e-9, "y")
    }

    @Test fun `zoom out goes to a third of fit and no further, the level shown (ct52 F)`() {
        val b = htuBox(); val v0 = SurveyPrep.PhotoView()
        val v = v0.zoomAt(b, 0.2, bw / 2, bh / 2)
        assertTrue(!v.fitted); close(1.0 / 3, v.z, 1e-12, "clamped to ZMIN")
        assertTrue(v.k(b) < v0.k(b), "wider than fit")
        assertEquals("0.33×", v.label())
        assertEquals(SurveyPrep.ZMAX, v0.zoomAt(b, 50.0, 10.0, 10.0).z, "and in to 8x")
        assertEquals("8.0×", v0.zoomAt(b, 50.0, 10.0, 10.0).label())
        // the point under the finger stays under it
        val at = v0.fromS(b, 300.0, 400.0); val z2 = v0.zoomAt(b, 2.0, 300.0, 400.0)
        val s = z2.toS(b, at[0], at[1]); close(300.0, s[0], 1e-6, "x"); close(400.0, s[1], 1e-6, "y")
    }

    @Test fun `a drag on empty photo pans at any zoom, fitted or zoomed out, and fit brings back zoom 1 on the fit rect (ct52 F)`() {
        val b = htuBox()
        // fitted: the drag unfits and moves the centre against the finger
        val f0 = SurveyPrep.PhotoView()
        val c0x = f0.centreX(b); val c0y = f0.centreY(b)
        val p = f0.pan(b, c0x, c0y, -60.0, -30.0)
        assertTrue(!p.fitted); assertEquals(1.0, p.z)
        close(c0x + 60 / f0.k(b), p.cx, 1e-9, "x"); close(c0y + 30 / f0.k(b), p.cy, 1e-9, "y")
        // zoomed out (ct52: the drag from the bottom-right corner up and left moves the centre right and down)
        val zo = f0.zoomAt(b, 0.2, bw / 2, bh / 2)
        val zp = zo.pan(b, zo.cx, zo.cy, -60.0, -30.0)
        assertTrue(zp.cx > zo.cx && zp.cy > zo.cy, "${zo.cx},${zo.cy} -> ${zp.cx},${zp.cy}")
        // the centre stays over the picture, past the corner as far as the cut and no further
        val far = zo.pan(b, zo.cx, zo.cy, -1e6, 1e6)
        close(b.iw - b.ox, far.cx, 1e-9, "the cut's right edge"); close(-b.oy, far.cy, 1e-9, "the cut's top edge")
        // fit: zoom 1, fitted, centred on the fit rect, no label
        val fit = far.fit()
        assertTrue(fit.fitted); assertEquals(1.0, fit.zoom()); assertNull(fit.label())
        close((b.fr[0] + b.fr[2]) / 2, fit.centreX(b), 1e-9, "centre x"); close((b.fr[1] + b.fr[3]) / 2, fit.centreY(b), 1e-9, "centre y")
    }

    @Test fun `fitted, the view follows the fit rect as the lines move, and a pinch zooms about the fingers`() {
        val b = htuBox(); val v = SurveyPrep.PhotoView()
        val moved = SurveyPrep.PhotoBox(b.w, b.h, doubleArrayOf(b.fr[0] - 100, b.fr[1], b.fr[2] + 300, b.fr[3]), b.ox, b.oy, b.iw, b.ih)
        close((moved.fr[0] + moved.fr[2]) / 2, v.centreX(moved), 1e-9, "follows")
        assertTrue(v.k(moved) < v.k(b), "a wider fit rect, a smaller scale")
        // out of fit it stays where it was
        val u = v.unfit(b); close(v.centreX(b), u.centreX(moved), 1e-9, "unfitted stays")
        // two fingers 2x apart about (300, 400), not moving: that point stays put and the zoom doubles
        val at = v.fromS(b, 300.0, 400.0)
        val pz = v.pinch(b, 2.0, 300.0, 400.0, 300.0, 400.0)
        close(2.0, pz.z, 1e-12, "doubled"); val s = pz.toS(b, at[0], at[1]); close(300.0, s[0], 1e-6, "x"); close(400.0, s[1], 1e-6, "y")
        // the midpoint moving 50 px right carries the picture with it
        val pm = v.pinch(b, 1.0, 300.0, 400.0, 350.0, 400.0)
        close(350.0, pm.toS(b, at[0], at[1])[0], 1e-6, "follows the midpoint")
    }

    @Test fun `a dragged end is kept on the picture, past the 960 view as far as the cut`() {
        val b = htuBox()
        close(b.iw - b.ox, b.keepX(1e9), 0.0, "right"); close(-b.ox, b.keepX(-1e9), 0.0, "left")
        close(-b.oy, b.keepY(-1e9), 0.0, "top"); close(b.ih - b.oy, b.keepY(1e9), 0.0, "bottom")
        val x75 = SurveyPrep.xAt(75.0)
        assertTrue(x75 > SurveyPrep.VW); close(x75, b.keepX(x75), 0.0, "a corner at 75° stays where it is put")
    }

    // ------------------------------------------------------------ a cut nobody wants stops early

    @Test fun `cutView calls onRow before every row, and a cancelled cut stops there`() {
        var rows = 0
        val c = SurveyPrep.cutView(pano, "right", intArrayOf(-10, -5, 30, 15)) { rows++ }
        assertEquals(20, rows); assertEquals(40, c.img.width)
        class Stop : RuntimeException()
        var seen = 0
        val r = runCatching { SurveyPrep.cutView(pano, "right", SurveyPrep.baseExt("right")) { if (++seen > 3) throw Stop() } }
        assertTrue(r.exceptionOrNull() is Stop, "the cancellation propagates"); assertEquals(4, seen, "it stopped at the fourth row")
    }
}
