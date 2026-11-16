package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.SurveyPrepTouch.Box
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.io.File
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A MARK WITHOUT `quad` IS WHAT IT WAS (port 63 slice Q1; DESIGN-quad §6: "its checklist is the same list, its Dims,
 * plates, polygon, tabs, plan items and file are unchanged. Q1 pins this on the existing example wall").
 *
 * Slice Q1 threads a mark's four free corners through the checklist, the dimensions on the photo, the plate layout, the
 * press, the 360, the six drawings and the file. Every one of those must answer a RECTANGLE -- every mark any phone has
 * today -- exactly as the build before it did. So this test DUMPS what each of them answers for marks that carry no
 * `quad` -- the measuring screen's example wall (a door, a window, a switch plate and three electric points on a
 * 2678 x 2677 front wall), every type and sub-kind on every kind of surface, and the six drawings of the Plan tab's
 * fixture room -- and holds each section to what the SAME dump printed on commit 7c49835 (port 63 M1 + M2), the commit
 * before Q1: the small sections line for line, every section by its line count and digest.
 *
 * The dump uses only what that commit already had (no `Quad`, no new parameter), which is how it was taken there.
 * Lengths are written in hundredths of a millimetre or of a pixel, as whole numbers, so no figure depends on how a JVM
 * prints a double. TO RE-PIN after a change that is MEANT to alter a rectangle: run with the environment variable
 * MCFT_RECTPIN_OUT set to a file, read the new sections there, and say in the commit why each moved.
 */
class RectanglePinTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0
    private val en = Tx.EN

    /** Hundredths, as a whole number: `1200.0` is `120000`. */
    private fun r(v: Double): String = Math.round(v * 100).toString()
    private fun r(v: Double?): String = if (v == null) "-" else r(v)
    private fun pts(l: List<DoubleArray>) = l.joinToString(";") { r(it[0]) + "," + r(it[1]) }

    private fun mk(t: String, n: Int, x0: Double, x1: Double, y0: Double, y1: Double, extra: Map<String, Any?> = emptyMap()) =
        SurveyPrep.Mark("id-$t$n", t, "F$t$n", x0, x1, y0, y1, extra)

    /** DimLayoutTest's example wall. */
    private fun wall(): List<SurveyPrep.Mark> = listOf(mk("D", 1, 150.0, 1050.0, 0.0, 2100.0), mk("W", 1, 1350.0, 2550.0, 900.0, 2100.0),
        mk("S", 1, 1100.0, 1300.0, 1100.0, 1190.0), mk("E", 1, 1475.0, 1525.0, 275.0, 325.0), mk("E", 2, 1975.0, 2025.0, 275.0, 325.0),
        mk("E", 3, 2475.0, 2525.0, 275.0, 325.0))

    /** SurveyPlanTest's fixture room, with a door on the front wall for its swing. */
    private fun room(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H); var n = 0
        fun put(s: String, t: String, code: String, x0: Double, x1: Double, y0: Double, y1: Double) { p.surface(s).marks.add(SurveyPrep.Mark("m${n++}", t, code, x0, x1, y0, y1)) }
        put("front", "W", "FW1", 782.0, 1953.0, 919.0, 2100.0); put("front", "E", "FE1", 325.0, 375.0, 325.0, 375.0); put("front", "D", "FD1", 0.0, 700.0, 0.0, 2053.0)
        put("front", "S", "FS1", 2100.0, 2300.0, 1100.0, 1190.0); put("front", "X", "FX1", 2200.0, 2600.0, 2000.0, 2400.0)
        put("back", "D", "BD1", 1500.0, 2300.0, 0.0, 1900.0); put("back", "O", "BO1", 200.0, 1100.0, 0.0, 2100.0); put("left", "N", "LN1", 1200.0, 2100.0, 750.0, 1950.0)
        put("left", "C", "LC1", 2869.0, 3169.0, 0.0, 2677.0); put("floor", "S", "GS1", 889.0, 1789.0, 150.0, 450.0); put("floor", "C", "GC1", 150.0, 450.0, 2869.0, 3169.0)
        put("floor", "X", "GX1", 1000.0, 1600.0, 1300.0, 1900.0); put("ceil", "B", "CB1", 600.0, 830.0, 0.0, 3319.0); put("ceil", "E", "CE1", 1725.0, 1875.0, 725.0, 875.0)
        put("ceil", "F", "CF1", 1325.0, 1475.0, 2125.0, 2275.0); put("ceil", "O", "CO1", 1700.0, 2300.0, 2400.0, 2900.0); put("ceil", "X", "CX1", 300.0, 500.0, 300.0, 500.0)
        put("right", "L", "RL1", 0.0, 1800.0, 2077.0, 2677.0); put("right", "B", "RB1", 2200.0, 3319.0, 2277.0, 2677.0)
        p.actual.putAll(mapOf("FW1.s" to 920.0, "FW1.h" to 1180.0, "FD1.w" to 905.0, "CO1.r" to 380.0, "LN1.p" to 110.0))
        return p
    }

    // ---- the phone's own canvas (DimLayoutTest's `phone`): 680 px square at 2 px a dp, the unit 680 / 480 px
    private val SIZE = 680.0; private val U = 680.0 / 480; private val DP = 2.0
    private fun toPx(x: Double, y: Double) = doubleArrayOf(x * SIZE / W, SIZE - y * SIZE / H)
    private fun textW(text: String, fs: Double, bold: Boolean) = text.length * fs * 0.6
    private fun codes(els: List<SurveyPrep.Mark>): List<Box> = els.map { e ->
        val p = toPx(e.x0, e.y1); val w = textW(e.code, 13.0 * U, true)
        if (SurveyPrep.isPoint(e.t)) Box(p[0] + 14 * U, p[1] - 18 * U, p[0] + 14 * U + w, p[1] - 2 * U) else Box(p[0] + 6 * U, p[1] + 3 * U, p[0] + 6 * U + w, p[1] + 19 * U)
    }
    private fun box(b: Box) = "${r(b.l)} ${r(b.t)} ${r(b.r)} ${r(b.b)}"
    private fun seg(s: DimLayout.Seg?) = if (s == null) "-" else "${r(s.x1)} ${r(s.y1)} ${r(s.x2)} ${r(s.y2)}"

    private fun plates(els: List<SurveyPrep.Mark>, scope: DimLayout.Scope, picked: String?, active: String?, tabs: Boolean, actual: Map<String, Double>): List<String> {
        val ms = els.map { e -> DimLayout.mark(actual, "front", e, W, H, e.id == picked, tabs && e.id == picked && !SurveyPrep.isPoint(e.t) && !SurveyPrep.isLocked(e),
            active, ::toPx, U, DP, ::textW, null) }
        val keep = codes(els) + Box(SIZE - 50 * DP, 6.0 * DP, SIZE - 6 * DP, 84.0 * DP)
        val out = DimLayout.place(ms, scope, SIZE, SIZE, keep, U, DP, textW(" × ", 10.0 * DP, false))
        return ms.flatMap { m -> m.figs.map { f -> "fig ${f.code} ${f.role} ${seg(f.line)} | ${f.at?.let { r(it[0]) + " " + r(it[1]) } ?: "-"} | ${r(f.w)} ${r(f.fs)} ${f.rank} ${f.measured} ${f.text} ${f.state} ${f.check} | ${seg(f.span)}" } } +
            ms.map { m -> "mark ${m.id} ${box(m.box)} ${m.picked} ${m.tabs}" } +
            out.plates.map { p -> "plate ${p.code} ${p.how} ${p.turned} ${p.arrowsIn} ${p.off} ${box(p.box)} | ${box(p.tap)} | ${seg(p.leader)} | ${p.whole?.let(::box) ?: "-"}" }
    }

    private fun item(it: SurveyPlan.Item): String = when (it) {
        is SurveyPlan.Line -> "line ${it.role} ${it.key} ${r(it.x1)} ${r(it.y1)} ${r(it.x2)} ${r(it.y2)} ${it.colour} ${r(it.width)} ${it.dash?.joinToString(",") { d -> r(d) }}"
        is SurveyPlan.Poly -> "poly ${it.role} ${it.key} ${it.s} ${it.rings.joinToString("/") { rg -> pts(rg) }} ${it.fill} ${r(it.fillAlpha)} ${it.stroke} ${r(it.width)} ${it.dash?.joinToString(",") { d -> r(d) }}"
        is SurveyPlan.Circle -> "circle ${it.role} ${it.key} ${r(it.cx)} ${r(it.cy)} ${r(it.r)} ${it.fill} ${it.stroke} ${r(it.width)}"
        is SurveyPlan.Ellipse -> "ellipse ${it.role} ${it.key} ${r(it.cx)} ${r(it.cy)} ${r(it.rx)} ${r(it.ry)} ${r(it.rot)} ${it.fill}"
        is SurveyPlan.Arc -> "arc ${it.role} ${it.key} ${r(it.cx)} ${r(it.cy)} ${r(it.r)} ${r(it.start)} ${r(it.sweep)} ${it.colour} ${r(it.width)}"
        is SurveyPlan.Text -> "text ${it.role} ${it.key} ${r(it.x)} ${r(it.y)} ${it.text} ${it.colour} ${r(it.size)} ${it.anchor} ${it.bold} ${r(it.rot)}"
    }

    private fun plain(v: Any?): String = when (v) {
        null -> "null"
        is Map<*, *> -> v.entries.joinToString(",", "{", "}") { (k, x) -> "$k:" + plain(x) }
        is List<*> -> v.joinToString(",", "[", "]") { plain(it) }
        is Double -> r(v)
        else -> v.toString()
    }

    /** Every section of the dump, by name, in order. */
    private fun sections(): LinkedHashMap<String, List<String>> {
        val out = LinkedHashMap<String, List<String>>()
        val ws = wall()
        // 1. the checklist of the example wall: key, words, the photo's estimate
        out["ask"] = ws.map { e -> e.code + " " + SurveyPrep.measures("front", e, W, H).joinToString(" | ") { "${it.key} ${it.what(en)} ${r(it.est)}" } }
        // 2. its dimensions on the photo
        out["dims"] = ws.flatMap { e -> MarkDims.of("front", e, W, H).map { d -> "${d.code} ${d.role} ${r(d.x1)} ${r(d.y1)} ${r(d.x2)} ${r(d.y2)} ${d.line} ${d.known} ${MarkDims.value(emptyMap(), "front", e, d.code, d.est, W, H).text}" } }
        // 3. where every plate goes, at the phone's own numbers
        val read = mapOf("FD1.w" to 1400.0, "FW1.h" to 1198.0, "FE2.cs" to 300.0, "FS1.w" to 200.0, "FW1.p" to 230.0)
        out["plates.face"] = plates(ws, DimLayout.Scope.FACE, "id-D1", "FD1.w", false, emptyMap())
        out["plates.all"] = plates(ws, DimLayout.Scope.ALL, null, null, false, read) + plates(ws, DimLayout.Scope.ALL, "id-W1", "FW1.s", false, read)
        out["plates.picked"] = plates(ws, DimLayout.Scope.PICKED, "id-D1", "FD1.w", true, read) + plates(ws, DimLayout.Scope.PICKED, "id-S1", null, true, emptyMap()) +
            plates(ws, DimLayout.Scope.PICKED, "id-E2", "FE2.cs", true, emptyMap())
        // 4. every type and sub-kind on every kind of surface: its checklist and its dimensions
        val every = ArrayList<String>()
        for ((s, a, b) in listOf(Triple("front", W, H), Triple("floor", W, L), Triple("ceil", W, L))) for (type in SurveyPrep.types(s)) {
            val t = type.letter; val subs: List<String?> = MarkTypes.subsOf(t, s).map { it.id }.ifEmpty { listOf(null) }
            val boxes = if (SurveyPrep.isSpot(t)) listOf(doubleArrayOf(40.0, 90.0, 30.0, 80.0), doubleArrayOf(a - 345, a - 295, b - 425, b - 375))
                        else listOf(doubleArrayOf(0.0, 900.0, 0.0, 700.0), doubleArrayOf(150.0, 350.0, 400.0, 490.0), doubleArrayOf(1700.0, 2600.0, 1900.0, 2600.0), doubleArrayOf(0.0, a, b - 400, b), doubleArrayOf(a - 300, a, 0.0, b))
            for (sub in subs) for (g in boxes) {
                val e = SurveyPrep.Mark("m", t, SurveyPrep.CODE.getValue(s) + t + "1", g[0], g[1], g[2], g[3], MarkTypes.withSub(emptyMap(), t, s, sub))
                every += "$s $t ${sub ?: "-"} ${pts(listOf(g))} : " + SurveyPrep.measures0(s, e, a, b).joinToString(" | ") { "${it.key} ${it.what(en)} ${r(it.est)}" } +
                    " :: " + SurveyPrep.measures(s, e, a, b).joinToString(",") { it.key } + " :: " + MarkDims.of(s, e, a, b).joinToString(" | ") { d -> "${d.key} ${d.role} ${r(d.x1)} ${r(d.y1)} ${r(d.x2)} ${r(d.y2)}" } +
                    " :: " + MarkDims.inPlane(s, e, a, b)
            }
        }
        out["every"] = every
        // 5. the press: every tab, the body, a miss, picked and not -- and what each drag then does to the box
        val touch = ArrayList<String>()
        val drawn = ws.map { e -> val p = toPx(e.x0, e.y1); val q = toPx(e.x1, e.y0)
            SurveyPrepTouch.Drawn(e.id, p[0], q[0], p[1], q[1], spot = if (SurveyPrep.isSpot(e.t)) SurveyPrep.SPOT / 2 * SIZE / W else null) }
        for (sel in listOf<String?>(null, "id-D1", "id-W1", "id-E2")) for (x in 0..680 step 20) for (y in 0..680 step 20) {
            val h = SurveyPrepTouch.hit(drawn, sel, emptyList(), x.toDouble(), y.toDouble(), 16 * DP, DP)
            if (h is SurveyPrepTouch.Hit.Mark) touch += "$sel $x $y ${h.id} ${h.part}"
        }
        out["touch"] = touch
        val moves = ArrayList<String>()
        for (part in listOf("body") + SurveyPrepTouch.TABS) for ((dx, dy) in listOf(37.0 to -61.0, -2000.0 to 9.0, 12.0 to 3000.0, -11.0 to -14.0)) {
            val e = mk("W", 1, 1350.0, 2550.0, 900.0, 2100.0); val o = e.copy()
            val ok = SurveyPrep.moveMark(e, o, part, dx, dy, W, H); val moved = "${r(e.x0)} ${r(e.x1)} ${r(e.y0)} ${r(e.y1)}"
            SurveyPrep.snapMark(e, part, W, H)
            moves += "$part $dx $dy $ok $moved -> ${r(e.x0)} ${r(e.x1)} ${r(e.y0)} ${r(e.y1)} ${e.extra}"
        }
        val lk = mk("D", 9, 150.0, 1050.0, 0.0, 2100.0, mapOf(SurveyPrep.LOCKED to true))
        moves += "locked ${SurveyPrep.moveMark(lk, lk.copy(), "r", 50.0, 0.0, W, H)} ${r(lk.x1)}"
        out["moves"] = moves
        // 6. the 360: its tabs and its outline
        val p360 = SurveyPrep.fresh(L, W, H).also { p -> for (s in SurveyPrep.SURFACES) for (k in SurveyPrep.SLOTS) p.surface(s).set[k] = true }
        val fr = Pano360.frames(p360, L, W, H).getValue("front"); val v = Pano360.faceView("front", 680.0, 840.0)
        out["p360"] = ws.flatMap { e ->
            listOf("tabs ${e.code} " + Pano360.tabsOf(e).joinToString(" ") { (part, mm) -> "$part ${r(mm[0])},${r(mm[1])}" },
                "poly ${e.code} " + (Pano360.markPoly(v, "front", fr, e, DP)?.let { pl -> pts(pl.pts) + " " + r(pl.area) + " " + (pl.spot?.let { s -> pts(listOf(s)) + "," + r(s[2]) } ?: "-") } ?: "off")) }
        // 7. the six drawings of the Plan tab, the saved sheet's marks, and the file
        val rm = room()
        for ((view, _) in SurveyPlan.VIEWS) {
            val d = SurveyPlan.drawing(en, view, rm, L, W, H)
            out["plan.$view"] = d.items.map(::item) + d.dims.map { "dim ${it.key} ${it.text} ${it.read}" } + listOf("box " + d.box.joinToString(" ") { r(it) } + " " + r(d.fs) + " " + d.info + " " + d.title)
        }
        out["sheet"] = SurveyPrep.SURFACES.flatMap { s -> SheetMarks.marks(rm.surface(s).marks, SheetMarks.Audience.OTHERS).map { e -> "$s ${e.code} ${r(e.x0)} ${r(e.x1)} ${r(e.y0)} ${r(e.y1)} ${e.extra}" } }
        out["file"] = listOf(plain(SurveyPrepFile.encode(rm, L, W, H)))
        // 8. the walk, the check's estimate, the menu and a copy
        val flow = ArrayList<String>()
        for (s in SurveyPrep.SURFACES) { val ab = SurveyPrep.size(s, L, W, H); flow += "stops $s " + MeasureWalk.stops(rm, s, ab[0], ab[1]).joinToString(",") }
        for (k in rm.actual.keys + listOf("FW1.w", "BD1.h")) flow += "check $k ${r(MeasureFlow.checkEst(rm, k) { SurveyPrep.size(it, L, W, H) })} ${MeasureFlow.value(rm, k) { SurveyPrep.size(it, L, W, H) }?.let { it.text + " " + it.state + " " + it.check }}"
        for (st in listOf<Int?>(0, 1, 3, 5, null)) flow += "menu $st " + SurveyPrepEdit.menuItemsOf(en, rm, "m0", st, null).joinToString("|") { it.first + "=" + it.second }
        flow += "menu3 " + SurveyPrepEdit.menuItemsOf(en, rm, "m0").joinToString("|") { it.second } + " / " + SurveyPrepEdit.menuItems(en, rm, "front", rm.markById("m0")!!.second).joinToString("|") { it.second }
        val c = SurveyPrepEdit.duplicate(rm, "m0", W, H, "copy")!!
        flow += "copy ${c.code} ${r(c.x0)} ${r(c.x1)} ${r(c.y0)} ${r(c.y1)} ${c.extra}"
        out["flow"] = flow
        return out
    }

    private fun digest(lines: List<String>): String =
        MessageDigest.getInstance("SHA-256").digest(lines.joinToString("\n").toByteArray(Charsets.UTF_8)).take(8).joinToString("") { "%02x".format(java.util.Locale.ROOT, it) }

    @Test fun `a mark without a quad is what it was on the commit before Q1 -- checklist, dimensions, plates, press, 360, drawings and file`() {
        val got = sections()
        System.getenv("MCFT_RECTPIN_OUT")?.takeIf { it.isNotBlank() }?.let { path ->
            File(path).writeText(got.entries.joinToString("\n") { (k, v) -> "## $k ${v.size} ${digest(v)}\n" + v.joinToString("\n") } + "\n")
        }
        // every section, by its line count and digest, as commit 7c49835 printed it
        assertEquals(PINNED.keys.toList(), got.keys.toList(), "the sections")
        val off = got.entries.filter { (k, v) -> PINNED.getValue(k) != "${v.size} ${digest(v)}" }.map { (k, v) -> "$k: ${v.size} ${digest(v)}, was ${PINNED.getValue(k)}" }
        assertTrue(off.isEmpty(), "a rectangle is no longer what it was before slice Q1 (set MCFT_RECTPIN_OUT to a file to read the dump):\n  " + off.joinToString("\n  "))
        // ... and the three a person can read, line for line
        assertEquals(ASK, got.getValue("ask")); assertEquals(DIMS, got.getValue("dims")); assertEquals(P360, got.getValue("p360"))
    }

    private companion object {
        /** Section -> "<lines> <digest>", from the dump taken on commit 7c49835. */
        val PINNED: Map<String, String> = linkedMapOf(
            "ask" to "6 098b908ecc4c33ce",
            "dims" to "19 51bf777fdbc63d03",
            "plates.face" to "33 6215baf01ce8198a",
            "plates.all" to "88 00b4494481632646",
            "plates.picked" to "85 ae87ebecee26208e",
            "every" to "433 c18e9208c66130cc",
            "touch" to "2511 1472be296f05e8b2",
            "moves" to "37 14dccbbd678167c3",
            "p360" to "12 de6cd7249974aaf7",
            "plan.floor" to "73 715589fb55bfb935",
            "plan.ceil" to "87 205d027ae8a6ef59",
            "plan.front" to "180 f5b5fc7ca4e695ae",
            "plan.right" to "105 b5e738f510e476a8",
            "plan.back" to "123 533031dd71cd290f",
            "plan.left" to "122 89c420da14e7300f",
            "sheet" to "19 8b6e0a86c8a2facd",
            "file" to "1 9ad60fbb7c4e0524",
            "flow" to "20 1bb9557a6b810aba")
        /** The example wall's checklist: key, words, the photo's estimate in hundredths of a mm. */
        val ASK: List<String> = listOf(
            "FD1 w width 90000 | h height 210000 | l from left corner 15000 | p reveal depth -",
            "FW1 w width 120000 | h height 120000 | r from right corner 12800 | s sill, from floor 90000 | p reveal depth -",
            "FS1 w width 20000 | h height 9000 | l from left corner 110000 | s from floor 110000",
            "FE1 cr from right corner, to centre 117800 | cs from floor, to centre 30000",
            "FE2 cr from right corner, to centre 67800 | cs from floor, to centre 30000",
            "FE3 cr from right corner, to centre 17800 | cs from floor, to centre 30000")
        /** Its dimensions: reading key, role, the line on the face (hundredths of a mm), whether it has one, whether this
         *  build draws it, and its figure with nothing read. */
        val DIMS: List<String> = listOf(
            "FD1.w WIDTH 15000 210000 105000 210000 true true ≈900",
            "FD1.h HEIGHT 105000 0 105000 210000 true true ≈2100",
            "FD1.l OFF_H 0 105000 15000 105000 true true ≈150",
            "FD1.p DEPTH 105000 0 105000 0 false true ?",
            "FW1.w WIDTH 135000 210000 255000 210000 true true ≈1200",
            "FW1.h HEIGHT 255000 90000 255000 210000 true true ≈1200",
            "FW1.r OFF_H 255000 150000 267800 150000 true true ≈128",
            "FW1.s OFF_V 195000 0 195000 90000 true true ≈900",
            "FW1.p DEPTH 255000 90000 255000 90000 false true ?",
            "FS1.w WIDTH 110000 119000 130000 119000 true true ≈200",
            "FS1.h HEIGHT 130000 110000 130000 119000 true true ≈90",
            "FS1.l OFF_H 0 114500 110000 114500 true true ≈1100",
            "FS1.s OFF_V 120000 0 120000 110000 true true ≈1100",
            "FE1.cr OFF_H 150000 30000 267800 30000 true true ≈1178",
            "FE1.cs OFF_V 150000 0 150000 30000 true true ≈300",
            "FE2.cr OFF_H 200000 30000 267800 30000 true true ≈678",
            "FE2.cs OFF_V 200000 0 200000 30000 true true ≈300",
            "FE3.cr OFF_H 250000 30000 267800 30000 true true ≈178",
            "FE3.cs OFF_V 250000 0 250000 30000 true true ≈300")
        /** On the 360: each mark's eight tabs (a point has them too, and the screen shows none), and its outline. */
        val P360: List<String> = listOf(
            "tabs FD1 l 15000,105000 r 105000,105000 t 60000,210000 b 60000,0 lt 15000,210000 rt 105000,210000 lb 15000,0 rb 105000,0",
            "poly FD1 9640,68635;28079,68635;28079,25610;9640,25610 7933505 -",
            "tabs FW1 l 135000,150000 r 255000,150000 t 195000,210000 b 195000,90000 lt 135000,210000 rt 255000,210000 lb 135000,90000 rb 255000,90000",
            "poly FW1 34225,50195;58811,50195;58811,25610;34225,25610 6044576 -",
            "tabs FS1 l 110000,114500 r 130000,114500 t 120000,119000 b 120000,110000 lt 110000,119000 rt 130000,119000 lb 110000,110000 rb 130000,110000",
            "poly FS1 29103,46098;33201,46098;33201,44254;29103,44254 75557 -",
            "tabs FE1 l 147500,30000 r 152500,30000 t 150000,32500 b 150000,27500 lt 147500,32500 rt 152500,32500 lb 147500,27500 rb 152500,27500",
            "poly FE1 40099,62488;39885,63560;39278,64468;38370,65075;37299,65288;36227,65075;35319,64468;34712,63560;34499,62488;34712,61417;35319,60508;36227,59901;37299,59688;38370,59901;39278,60508;39885,61417 246301 37299,62488,1200",
            "tabs FE2 l 197500,30000 r 202500,30000 t 200000,32500 b 200000,27500 lt 197500,32500 rt 202500,32500 lb 197500,27500 rb 202500,27500",
            "poly FE2 50343,62488;50129,63560;49523,64468;48614,65075;47543,65288;46471,65075;45563,64468;44956,63560;44743,62488;44956,61417;45563,60508;46471,59901;47543,59688;48614,59901;49523,60508;50129,61417 246301 47543,62488,1200",
            "tabs FE3 l 247500,30000 r 252500,30000 t 250000,32500 b 250000,27500 lt 247500,32500 rt 252500,32500 lb 247500,27500 rb 252500,27500",
            "poly FE3 60587,62488;60374,63560;59767,64468;58858,65075;57787,65288;56715,65075;55807,64468;55200,63560;54987,62488;55200,61417;55807,60508;56715,59901;57787,59688;58858,59901;59767,60508;60374,61417 246301 57787,62488,1200")
    }
}
