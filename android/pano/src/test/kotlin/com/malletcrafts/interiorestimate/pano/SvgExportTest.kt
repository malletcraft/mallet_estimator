package com.malletcrafts.interiorestimate.pano

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.time.LocalDate
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * EACH DRAWING ON ITS OWN A4 SHEET, AS SVG (v52 slice 14a, item 10), against plan.js's #planexp and ct52 DR's export
 * assertions (1850-1861): A4 at 1:50 (`scale(0.02)`, sc 50) for the fixture room, the title block naming the drawing
 * ("Drawing" + "Front wall elevation"), "<DTITLE> from survey prep", the file name ending _front-elevation /
 * _ceiling-plan / _plan and never _plan_plan; and plan.js's own second footnote and its coarser-scale rule.
 *
 * The synthetic room of every slice, L 3319 / W 2678 / H 2677 (ct52's Guest Bedroom), with the marks SurveyPlanTest uses.
 * No client name anywhere: the title block and the name carry initials and a project code only.
 */
class SvgExportTest {

    private val L = 3319.0; private val W = 2678.0; private val H = 2677.0
    private var n = 0
    private fun put(p: SurveyPrep.Prep, s: String, t: String, code: String, x0: Double, x1: Double, y0: Double, y1: Double) {
        p.surface(s).marks.add(SurveyPrep.Mark("m${n++}", t, code, x0, x1, y0, y1))
    }

    private fun gb(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        put(p, "front", "W", "FW1", 782.0, 1953.0, 919.0, 2100.0)
        put(p, "front", "E", "FE1", 325.0, 375.0, 325.0, 375.0)
        put(p, "back", "D", "BD1", 1500.0, 2300.0, 0.0, 1900.0)
        put(p, "left", "N", "LN1", 1200.0, 2100.0, 750.0, 1950.0)
        put(p, "floor", "S", "GS1", 889.0, 1789.0, 150.0, 450.0)
        put(p, "ceil", "B", "CB1", 600.0, 830.0, 0.0, 3319.0)
        put(p, "ceil", "E", "CE1", 1725.0, 1875.0, 725.0, 875.0)
        put(p, "ceil", "F", "CF1", 1325.0, 1475.0, 2125.0, 2275.0)
        put(p, "ceil", "O", "CO1", 1700.0, 2300.0, 2400.0, 2900.0)
        put(p, "right", "L", "RL1", 0.0, 1800.0, 2077.0, 2677.0)
        put(p, "right", "B", "RB1", 2200.0, 3319.0, 2277.0, 2677.0)
        p.actual["FW1.s"] = 920.0; p.actual["FW1.h"] = 1180.0
        return p
    }

    private val meta = SurveyExport.Meta("AP", "htu", "LR · Living Room", "2026-10-14", SurveyExport.drawn(LocalDate.of(2026, 10, 19)))

    private fun parse(svg: String): Element {
        val f = DocumentBuilderFactory.newInstance()
        f.isNamespaceAware = true
        return f.newDocumentBuilder().parse(ByteArrayInputStream(svg.toByteArray(Charsets.UTF_8))).documentElement
    }

    private fun children(e: Element): List<Element> {
        val out = mutableListOf<Element>(); val nl = e.childNodes
        for (i in 0 until nl.length) (nl.item(i) as? Element)?.let { out.add(it) }
        return out
    }

    // ------------------------------------------------------------------ the scale

    @Test fun `the sheet scale is 1 to 50 when the drawing fits 277 x 162 mm at 1 to 50, else the first coarser 1 to 10n that fits`() {
        assertEquals(50, SurveyExport.sheetScale(4000.0, 4900.0))
        assertEquals(50, SurveyExport.sheetScale(277.0 * 50, 162.0 * 50))     // exactly the area
        // a large room: 12 780 mm high is 255.6 mm at 1:50; 12780 / 162 = 78.9 → 1:80
        assertEquals(80, SurveyExport.sheetScale(9000.0, 12780.0))
        // too wide rather than too tall: 16 000 / 277 = 57.8 → 1:60
        assertEquals(60, SurveyExport.sheetScale(16000.0, 3000.0))
        // whatever the scale, the drawing fits the area, and it is a multiple of 10
        for ((w, h) in listOf(20000.0 to 9000.0, 14000.0 to 30000.0, 8100.0 to 8100.0)) {
            val sc = SurveyExport.sheetScale(w, h)
            assertTrue(sc % 10 == 0 && sc >= 50 && w / sc <= 277.0 && h / sc <= 162.0, "$w × $h at 1:$sc")
            if (sc > 50) assertFalse(w / (sc - 10) <= 277.0 && h / (sc - 10) <= 162.0, "1:${sc - 10} would have fitted $w × $h")
        }
    }

    @Test fun `the fixture room goes on its sheet at 1 to 50 on all six drawings, a large room at a coarser scale, centred in the drawing area`() {
        val p = gb()
        for ((v, _) in SurveyPlan.VIEWS) {
            val s = SurveyExport.sheet(v, p, L, W, H, meta, "Living Room")
            assertEquals(50, s.scale, v)
            assertEquals(0.02, s.k, 1e-12)
            val b = s.drawing.box
            val x0 = s.tx + b[0] * s.k; val y0 = s.ty + b[1] * s.k
            assertEquals(10 + (277 - b[2] * s.k) / 2, x0, 1e-9, "$v centred across")
            assertEquals(10 + (162 - b[3] * s.k) / 2, y0, 1e-9, "$v centred down")
            assertTrue(x0 >= 10 - 1e-9 && x0 + b[2] * s.k <= 287 + 1e-9 && y0 >= 10 - 1e-9 && y0 + b[3] * s.k <= 172 + 1e-9, "$v inside the area")
            assertTrue(Regex("scale\\(0\\.02\\)").containsMatchIn(SvgWriter.write(s)), "$v scale(0.02)")
        }
        val big = SurveyPrep.fresh(9000.0, 6500.0, 2700.0)
        val s = SurveyExport.sheet("floor", big, 9000.0, 6500.0, 2700.0, meta)
        assertTrue(s.scale > 50, "a 6.5 × 9 m room is not at 1:50 (${s.scale})")
        assertEquals(SurveyExport.sheetScale(s.drawing.box[2], s.drawing.box[3]), s.scale)
        assertTrue(s.drawing.box[3] * s.k <= 162.0 && s.drawing.box[2] * s.k <= 277.0)
        assertTrue(SvgWriter.write(s).contains("1:${s.scale} on A4"))
    }

    @Test fun `the export draws at the export's own text unit -- the longer side over 30, plan_js`() {
        val s = SurveyExport.sheet("front", gb(), L, W, H, meta)
        val d = SurveyPrep.sizeOf("front", gb(), L, W, H)
        assertEquals(maxOf(d[0], d[1]) / 30.0, s.drawing.fs, 1e-9)
    }

    // ------------------------------------------------------------------ the names

    @Test fun `the six names for the fixture room -- initials, room token and project joined by underscores, then the drawing`() {
        val p = gb()
        val want = mapOf("floor" to "AP_LR_htu_plan", "ceil" to "AP_LR_htu_ceiling-plan", "front" to "AP_LR_htu_front-elevation",
            "right" to "AP_LR_htu_right-elevation", "back" to "AP_LR_htu_back-elevation", "left" to "AP_LR_htu_left-elevation")
        for ((v, name) in want) {
            assertEquals(name, SurveyExport.exportName("AP", "LR", "htu", v))
            assertEquals(name, SurveyExport.sheet(v, p, L, W, H, meta).name, "the sheet's own name for $v")
            assertFalse(name.endsWith("_plan_plan"))
        }
        assertEquals(6, want.values.toSet().size)
    }

    @Test fun `a name is sanitised -- ASCII letters, digits, dash and underscore only, spaces to underscores, never empty, never _plan_plan`() {
        assertEquals("AP_Living_Room_PROJ-0026_plan", SurveyExport.exportName(" AP ", "Living Room", "PROJ-0026", "floor"))
        assertEquals("AP_Mallet-s_Den_P-1_back-elevation", SurveyExport.exportName("AP", "Mallet's  Den", "P/1", "back"))
        assertEquals("room_plan", SurveyExport.exportName("", "  ", null, "floor"))
        assertEquals("room_ceiling-plan", SurveyExport.exportName(null, "स्वयंपाकघर", "§§", "ceil"))
        assertEquals("AP_LR_plan", SurveyExport.exportName("AP", "LR", "_plan", "floor"))
        assertEquals("AP_LR_x_plan", SurveyExport.exportName("AP", "LR", "x_plan", "floor"))
        assertEquals("AP_LR_front-elevation", SurveyExport.exportName("AP", "LR_front-elevation", null, "front"))
        assertFalse(SurveyExport.exportName("AP", "LR", "x_plan", "floor").endsWith("_plan_plan"))
        for ((v, _) in SurveyPlan.VIEWS) for (raw in listOf("Ünïcode room", "a&b<c>\"d\"", "\t\n", "x".repeat(300))) {
            val nm = SurveyExport.exportName("YS", raw, "PROJ 7", v)
            assertTrue(Regex("^[A-Za-z0-9_-]+$").matches(nm), nm)
            assertTrue(nm.endsWith(SurveyExport.SUFFIX.getValue(v)) && nm.length < 160, nm)
            assertFalse(nm.endsWith("_plan_plan"), nm)
        }
        assertEquals("LR", SurveyExport.roomToken("LR · Living Room"))
        assertEquals("Kitchen", SurveyExport.roomToken("Kitchen"))
    }

    @Test fun `the room part never drops out of a name -- a token that makes nothing safe is room, beside the initials and the project`() {
        // plan.js keeps the room part (split(' · ')[0]||'room'); a Marathi room or an unsynced site must not collapse
        // two different rooms or clients onto one shorter name
        assertEquals("AP_room_htu_plan", SurveyExport.exportName("AP", "स्वयंपाकघर", "htu", "floor"))
        assertEquals("AP_room_htu_front-elevation", SurveyExport.exportName("AP", "देवघर", "htu", "front"))
        assertEquals("AP_room_plan", SurveyExport.exportName("AP", "", "", "floor"))
        assertEquals("AP_LR_plan", SurveyExport.exportName("AP", "LR", "", "floor"), "no project code on an unsynced site: left out")
        assertEquals("room_htu_ceiling-plan", SurveyExport.exportName("", "  ", "htu", "ceil"))
        for ((v, _) in SurveyPlan.VIEWS) {
            val parts = SurveyExport.exportName("AP", "§", "htu", v).removeSuffix(SurveyExport.SUFFIX.getValue(v)).split('_')
            assertEquals(listOf("AP", "room", "htu"), parts, v)
        }
    }

    @Test fun `Share sends the format last saved for this drawing, else the PDF -- the format only, never an old file`() {
        assertEquals("svg", SurveyExport.shareExt("AP_LR_htu_plan.svg", "AP_LR_htu_plan"))
        assertEquals("pdf", SurveyExport.shareExt("AP_LR_htu_plan.pdf", "AP_LR_htu_plan"))
        assertEquals("pdf", SurveyExport.shareExt(null, "AP_LR_htu_plan"))
        // a file saved for another drawing says nothing about this one
        assertEquals("pdf", SurveyExport.shareExt("AP_LR_htu_plan.svg", "AP_LR_htu_front-elevation"))
        assertEquals("pdf", SurveyExport.shareExt("AP_LR_htu_plan (1).svg", "AP_LR_htu_plan"))
    }

    @Test fun `each capture keeps its copies in its own folder, named safely`() {
        assertEquals("cap-7f3a_01", SurveyExport.keepDir("cap-7f3a_01"))
        assertEquals("a-b", SurveyExport.keepDir("a/b"))
        assertEquals("capture", SurveyExport.keepDir(""))
        assertEquals("capture", SurveyExport.keepDir(null))
        assertEquals("capture", SurveyExport.keepDir(".."))
        assertTrue(Regex("^[A-Za-z0-9_-]+$").matches(SurveyExport.keepDir("../../etc")))
    }

    // ------------------------------------------------------------------ the title block and the footnotes

    @Test fun `the title block names the drawing -- Drawing then its full name -- and carries initials and the project code, never a name`() {
        val p = gb()
        for ((v, _) in SurveyPlan.VIEWS) {
            val s = SurveyExport.sheet(v, p, L, W, H, meta)
            val rows = s.rows()
            assertEquals(listOf("Drawing", "Client", "Project", "Room", "Captured", "Scale"), rows.map { it.first })
            assertEquals("Drawing" to SurveyPlan.DTITLE.getValue(v), rows[0])
            assertEquals("AP", rows[1].second); assertEquals("htu", rows[2].second); assertEquals("LR · Living Room", rows[3].second)
            assertEquals("2026-10-14", rows[4].second)
            assertEquals("1:50 on A4 · drawn 19 Oct 2026", rows[5].second)
            // the SVG's text, as ct52 reads it (textContent): "Drawing" then the title, side by side
            val txt = SvgWriter.write(s).let { parse(it).textContent }
            assertTrue(txt.contains("Drawing" + SurveyPlan.DTITLE.getValue(v)), v)
        }
        assertEquals("Front wall elevation", SurveyExport.sheet("front", p, L, W, H, meta).rows()[0].second)
        assertEquals("Reflected ceiling plan", SurveyExport.sheet("ceil", p, L, W, H, meta).rows()[0].second)
        assertEquals("Floor plan", SurveyExport.sheet("floor", p, L, W, H, meta).rows()[0].second)
        // nothing given: a dash, not an empty row
        val bare = SurveyExport.sheet("floor", p, L, W, H, SurveyExport.Meta("", "", "", "", "19 Oct 2026"))
        assertEquals(listOf("—", "—", "—", "—"), bare.rows().subList(1, 5).map { it.second })
        assertEquals("room_plan", bare.name)
    }

    @Test fun `both footnote lines, word for word -- DTITLE from survey prep, laser sizes -- and the estimate's grey note`() {
        val p = gb()
        for ((v, _) in SurveyPlan.VIEWS) {
            val s = SurveyExport.sheet(v, p, L, W, H, meta)
            assertEquals(listOf("${SurveyPlan.DTITLE.getValue(v)} from survey prep · laser sizes",
                "≈ = from the photo, not yet measured · MCFT S2"), s.footnotes())
            val svg = SvgWriter.write(s)
            assertTrue(svg.contains(">${SurveyPlan.DTITLE.getValue(v)} from survey prep · laser sizes</text>"), v)
            assertTrue(svg.contains(">≈ = from the photo, not yet measured · MCFT S2</text>"), v)
        }
        assertEquals("Front wall elevation from survey prep · laser sizes", SurveyExport.footSource("front"))
    }

    // ------------------------------------------------------------------ the SVG itself

    @Test fun `the SVG is well-formed, A4 landscape in mm, and holds every display-list item exactly once, in its ink`() {
        val p = gb()
        for ((v, _) in SurveyPlan.VIEWS) {
            val s = SurveyExport.sheet(v, p, L, W, H, meta)
            val root = parse(SvgWriter.write(s))
            assertEquals("svg", root.localName)
            assertEquals("http://www.w3.org/2000/svg", root.namespaceURI)
            assertEquals("0 0 297 210", root.getAttribute("viewBox"))
            assertEquals("297mm", root.getAttribute("width")); assertEquals("210mm", root.getAttribute("height"))
            val g = children(root).single { it.getAttribute("data-plan") == "1" }
            assertEquals(v, g.getAttribute("data-view"))
            val els = children(g); val items = s.drawing.items
            assertEquals(items.size, els.size, "$v: one element per item")
            // item by item, in order: the element its kind makes, its role and key, its colour
            items.zip(els).forEachIndexed { i, (it, e) ->
                val tag = when (it) {
                    is SurveyPlan.Line -> "line"; is SurveyPlan.Poly -> "path"; is SurveyPlan.Circle -> "circle"
                    is SurveyPlan.Ellipse -> "ellipse"; is SurveyPlan.Arc -> "path"; is SurveyPlan.Text -> "text"
                }
                assertEquals(tag, e.localName, "$v item $i")
                assertEquals(it.role, e.getAttribute("data-r"), "$v item $i role")
                assertEquals(it.key ?: "", e.getAttribute("data-k"), "$v item $i key")
                when (it) {
                    is SurveyPlan.Line -> assertEquals(SvgWriter.hex(it.colour), e.getAttribute("stroke"))
                    is SurveyPlan.Text -> { assertEquals(it.text, e.textContent); assertEquals(SvgWriter.hex(it.colour), e.getAttribute("fill")) }
                    is SurveyPlan.Poly -> { val f = it.fill
                        assertEquals(if (f != null && it.fillAlpha > 0) SvgWriter.hex(f) else "none", e.getAttribute("fill")) }
                    is SurveyPlan.Arc -> assertTrue(e.getAttribute("d").contains(" A"), "an arc is an SVG arc")
                    else -> {}
                }
            }
            // every kind the drawings use is there: lines, polygons, text -- and the elevation's floor hatch
            assertTrue(items.any { it is SurveyPlan.Line } && items.any { it is SurveyPlan.Poly } && items.any { it is SurveyPlan.Text })
            if (v in SurveyPlan.WALLS) assertTrue(els.count { it.getAttribute("data-r") == "hatch" } > 10, "$v hatched")
        }
        // the plans' wall band is filled even-odd (two rings), the door's swing an arc, the fan's blades ellipses
        val floor = parse(SvgWriter.write(SurveyExport.sheet("floor", gb(), L, W, H, meta)))
        val band = children(children(floor).single { it.getAttribute("data-plan") == "1" }).first { it.getAttribute("data-r") == "walls" }
        assertEquals("evenodd", band.getAttribute("fill-rule")); assertEquals(2, Regex("M").findAll(band.getAttribute("d")).count())
        val back = SurveyExport.sheet("back", gb(), L, W, H, meta)
        assertTrue(back.drawing.items.isNotEmpty())
        val ceil = SurveyExport.sheet("ceil", gb(), L, W, H, meta)
        assertEquals(3, ceil.drawing.items.count { it is SurveyPlan.Ellipse }); assertTrue(SvgWriter.write(ceil).contains("<ellipse"))
        assertTrue(SvgWriter.write(SurveyExport.sheet("floor", doorRoom(), L, W, H, meta)).contains(" A"), "the door swing")
    }

    @Test fun `the inks are the screen's -- a laser reading in ink, the photo's estimate grey with its approx sign`() {
        val s = SurveyExport.sheet("front", gb(), L, W, H, meta)
        val root = parse(SvgWriter.write(s))
        val g = children(root).single { it.getAttribute("data-plan") == "1" }
        val dims = children(g).filter { it.localName == "text" && it.getAttribute("data-r") == "dim" }
        val sill = dims.single { it.getAttribute("data-k") == "FW1.s" }
        assertEquals("920", sill.textContent); assertEquals("#1d1f1a", sill.getAttribute("fill")); assertEquals("700", sill.getAttribute("font-weight"))
        val w = dims.single { it.getAttribute("data-k") == "FW1.w" }
        assertTrue(w.textContent.startsWith("≈")); assertEquals("#8a8f86", w.getAttribute("fill"))
    }

    @Test fun `text is escaped and numbers never print an exponent or minus zero`() {
        assertEquals("a &amp; b &lt;c&gt; &quot;d&quot;", SvgWriter.esc("a & b <c> \"d\""))
        assertEquals("0", SvgWriter.f(-0.0001)); assertEquals("0", SvgWriter.f(0.0)); assertEquals("1.5", SvgWriter.f(1.5))
        assertEquals("-150", SvgWriter.f(-150.0)); assertEquals("0.01", SvgWriter.f(0.0123)); assertEquals("12345678.9", SvgWriter.f(12345678.9))
        assertEquals("#1d1f1a", SvgWriter.hex(SurveyPlan.INK)); assertEquals("#000000", SvgWriter.hex(0))
        val room = SurveyPrep.fresh(L, W, H)
        room.surface("front").marks.add(SurveyPrep.Mark("x", "X", "F<&>1", 100.0, 600.0, 100.0, 600.0))
        val svg = SvgWriter.write(SurveyExport.sheet("front", room, L, W, H, SurveyExport.Meta("A&B", "P<1>", "R \"1\"", "", "19 Oct 2026")))
        parse(svg)   // well-formed
        val g = children(parse(svg)).single { it.getAttribute("data-plan") == "1" }
        for (e in listOf(g) + children(g)) { val a = e.attributes
            for (i in 0 until a.length) assertFalse(Regex("\\d[eE][-+]?\\d|-0(?![.\\d])").containsMatchIn(a.item(i).nodeValue), "${e.localName} ${a.item(i).nodeName}=${a.item(i).nodeValue}") }
        assertTrue(svg.contains("A-B") && svg.contains("P&lt;1&gt;") && svg.contains("F&lt;&amp;&gt;1"))
    }

    @Test fun `the drawn date is the title block's day month year`() {
        assertEquals("19 Oct 2026", SurveyExport.drawn(LocalDate.of(2026, 10, 19)))
        assertEquals("05 Sep 2026", SurveyExport.drawn(LocalDate.of(2026, 9, 5)))
    }

    private fun doorRoom(): SurveyPrep.Prep {
        val p = gb(); put(p, "front", "D", "FD1", 0.0, 905.0, 0.0, 2053.0); return p
    }
}
