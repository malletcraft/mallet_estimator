package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Lang
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.time.Instant
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The ten new mark types and their sub-kinds (port 62 slice L5), against the approved prototype v61c.2 (index61c2.html,
 * T53: NEW 2397-2407, SUBS 2423-2466, displayName 2478, the code grammar 2534-2543, defaults53 2551-2565, measures53
 * 2569-2588, placeRefusal 2590).
 *
 * TWO KINDS OF PIN. (1) THE PAGE'S OWN ANSWERS, ROW BY ROW: `src/test/resources/marktypes/proto-type-rows.txt` is what
 * the page's T53 module prints (docs/port62/proto-tables.mjs, the rows between "L5 GOLDEN" and "L5 END") -- the ten,
 * the types of each kind of surface, every sub-kind with its label, what a mark stores and shows for each, the default
 * box of every type and sub-kind on seventeen faces, the measures of a mark in each corner and the middle of the room's
 * faces, where each type may go and what a refusal says, and the code grammar; [rows] prints the same rows from
 * MarkTypes, SurveyPrep and Trades, and the two are compared line for line. (2) BY HAND: the tables typed out, the
 * Marathi the page shows for every name and label, and what the phone does with them -- a new mark's box, its stored
 * `sub`, its code and readings, the checklist, the 360's refusal, an older box made a spot.
 */
class MarkTypesTest {

    private val KINDS = listOf("wall", "floor", "ceil")
    private val SURF = mapOf("wall" to "front", "floor" to "floor", "ceil" to "ceil")
    private val V52 = listOf("W", "D", "C", "B", "O", "N", "L", "E", "P", "S", "F", "X")
    private val V53 = listOf("NOTE", "GRL", "AC", "CDP", "PLB", "CTR", "CRP", "LV", "SAN", "DEF")
    private val ALLT = V52 + V53

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0

    // ------------------------------------------------------------------ (1) the page's own rows

    /** A figure as the script prints it: to 0.001 mm, an integer bare; `site` for none. */
    private fun n(v: Double?): String {
        if (v == null) return "site"
        val r = Math.round(v * 1000) / 1000.0
        return if (r == Math.rint(r)) r.toLong().toString() else r.toString()
    }
    private fun col(c: Int) = "#%06x".format(Locale.ROOT, c)
    private fun box(d: DoubleArray?) = d?.joinToString(",") { n(it) } ?: "v52"
    private fun nm(sub: String?) = sub ?: "(none)"
    /** No sub-kind, each of the type's on that kind, and one nobody knows. */
    private fun subsPlus(t: String, k: String): List<String?> = listOf<String?>(null) + MarkTypes.subsOf(t, k).map { it.id } + "zzz"

    private val FACES = mapOf(
        "wall" to listOf(2678 to 2677, 3319 to 2677, 6000 to 3000, 1000 to 1000, 400 to 2400, 180 to 80, 40 to 30),
        "floor" to listOf(2678 to 3319, 6000 to 4000, 500 to 5000, 180 to 80, 40 to 30),
        "ceil" to listOf(2678 to 3319, 6000 to 4000, 500 to 5000, 180 to 80, 40 to 30))
    private val ROOM = mapOf("wall" to (W to H), "floor" to (W to L), "ceil" to (W to L))
    private val CODES = listOf("FW1", "FP1", "FPLB1", "FPLB12.cl", "CCDP1", "GNOTE2", "LLV3", "LL1", "RS4.w", "BCRP10.p", "F-M1", "F-M2.per", "G-T3", "FK1",
        "FK1.t", "FK12.3", "FWx7", "FW01", "fw1", "XW1", "F1", "FW", "F-X1", "FPLB1.c l", "FPLB1.", "", "F W1")
    private val ELS = listOf("P" to "FP1", "PLB" to "FPLB1", "PLB" to "FPLB7", "P" to "FP2", "L" to "FL3", "LV" to "FLV9", "S" to "FS2", "SAN" to "FSAN5",
        "C" to "FC4", "CRP" to "FCRP6", "CTR" to "FCTR1", "NOTE" to "FNOTE2", "D" to "FD1", "DEF" to "FDEF8")

    /** The rows of proto-tables.mjs's "L5 GOLDEN", from the phone's own models, in the script's order and form. */
    private fun rows(): List<String> {
        val out = ArrayList<String>()
        for (x in MarkTypes.NEW) out += "NEW ${x.t} | ${x.name} | ${col(x.colour)} | ${x.kinds.joinToString(" ")} | ${x.short} | ${x.trade ?: "-"} | " +
            "${when (x.shape) { MarkTypes.Shape.PIN -> "pin"; MarkTypes.Shape.POINT -> "point"; MarkTypes.Shape.RECT -> "rect" }} | ${if (x.ink) 1 else 0}"
        for (k in KINDS) out += "TYPES53 $k | " + MarkTypes.typesOn(k).joinToString(" ; ") { "${it.letter}=${it.name}=${col(it.colour)}" }
        for (k in KINDS) out += "ON $k | " + Trades.ON.getValue(k).joinToString(" ")
        out += "POINT | ${ALLT.filter { MarkTypes.isPoint(it) }.joinToString(" ")} | PIN | ${ALLT.filter { MarkTypes.isPin(it) }.joinToString(" ")} | NEW | ${ALLT.filter { MarkTypes.isNew(it) }.joinToString(" ")}"
        for (t in ALLT) out += "CANGO $t | " + KINDS.joinToString(" ") { k -> "$k=" + when (MarkTypes.canGo(t, k)) { null -> "-"; true -> "1"; false -> "0" } } +
            " | typeName " + MarkTypes.typeName(Tx.EN, t)
        out += "typeName of a token nobody knows | " + MarkTypes.typeName(Tx.EN, "QQ")
        for (t in ALLT) for (k in KINDS) {
            val l = MarkTypes.subsOf(t, k)
            out += "SUBS $t $k | " + (l.joinToString(" ; ") { it.id + "=" + it.label + (it.service?.let { s -> " service:$s" } ?: "") + (it.trade?.let { tr -> " trade:$tr" } ?: "") }.ifEmpty { "-" }) +
                " | first " + (MarkTypes.firstSub(t, k) ?: "-") + " | stored " + subsPlus(t, k).joinToString(" ") { nm(it) + ">" + (MarkTypes.subToStore(t, k, it) ?: "-") }
            out += "NAME $t $k | " + subsPlus(t, k).joinToString(" ; ") { nm(it) + "=" + MarkTypes.displayName(Tx.EN, t, k, it) }
        }
        for (k in KINDS) for ((a, b) in FACES.getValue(k)) for (t in ALLT)
            out += "BOX ${SURF[k]} ${a}x$b $t | " + subsPlus(t, k).joinToString(" ; ") { nm(it) + "=" + box(MarkTypes.defaults(SURF.getValue(k), t, it, a.toDouble(), b.toDouble())) }
        for (k in KINDS) {
            val (a, b) = ROOM.getValue(k)
            for (t in ALLT) {
                val pt = MarkTypes.isPoint(t) || MarkTypes.isPin(t); val w = if (pt) 50.0 else 300.0; val h = if (pt) 50.0 else 200.0
                for ((x, y) in listOf(10.0 to 10.0, a - w - 10 to b - h - 10, (a - w) / 2 to (b - h) / 2, 10.0 to b - h - 10, a - w - 10 to 10.0, 0.0 to 0.0)) {
                    val e = SurveyPrep.Mark("x", t, "", x, x + w, y, y + h)
                    val m = MarkTypes.measures(SURF.getValue(k), e, a, b)
                    out += "MEAS ${SURF[k]} $t @${box(doubleArrayOf(e.x0, e.x1, e.y0, e.y1))} | " +
                        (if (m == null) "v52" else m.joinToString(" ; ") { "${it.key}:${it.what(Tx.EN)}:${n(it.est)}" }.ifEmpty { "(none)" })
                }
            }
        }
        for (t in ALLT) out += "REFUSE $t | " + KINDS.joinToString(" | ") { k -> "$k=" + (MarkTypes.placeRefusal(Tx.EN, t, k) ?: "ok") }
        for (c in CODES) {
            val p = MarkTypes.parseCode(c)
            out += "CODE \"$c\" | " + (p?.let { listOf(it.surface, it.surf, it.token ?: "-", it.kind, it.n, it.measure ?: "-").joinToString(" ") } ?: "null") +
                " | token " + (MarkTypes.codeToken(c) ?: "-")
        }
        val p = SurveyPrep.fresh(L, W, H)
        ELS.forEachIndexed { i, (t, code) -> p.surface("front").marks.add(SurveyPrep.Mark("m$i", t, code, 0.0, 50.0, 0.0, 50.0)) }
        out += "NEXT front | " + ALLT.joinToString(" ") { "$it>" + p.nextCode("front", it) }
        return out
    }

    @Test fun `the page's own rows -- the ten, their sub-kinds, names, boxes, measures, refusals and codes -- line for line`() {
        val golden = requireNotNull(javaClass.getResourceAsStream("/marktypes/proto-type-rows.txt")) { "the golden is missing" }
            .readBytes().decodeToString().lines().filter { it.isNotEmpty() }
        val mine = rows()
        assertEquals(992, golden.size, "the golden as proto-tables.mjs printed it")
        for (i in golden.indices) assertEquals(golden[i], mine.getOrNull(i), "row ${i + 1}")
        assertEquals(golden.size, mine.size)
        // and the rows really carry what they claim: every kind of row is there, in number
        fun count(head: String) = golden.count { it.startsWith("$head ") }
        assertEquals(listOf(10, 3, 3, 22, 66, 66, 374, 396, 22, 27), listOf("NEW", "TYPES53", "ON", "CANGO", "SUBS", "NAME", "BOX", "MEAS", "REFUSE", "CODE").map(::count))
    }

    // ------------------------------------------------------------------ (2) by hand: the ten

    @Test fun `the ten, row by row - token, name, colour, where it goes, short name, trade, and what it is on a face`() {
        fun row(x: MarkTypes.New) = listOf(x.t, x.name, col(x.colour), x.kinds.joinToString(" "), x.short, x.trade ?: "-", x.shape.name, if (x.ink) "ink" else "").joinToString(" | ")
        assertEquals(listOf(
            "NOTE | Note | #ffffff | wall floor ceil | Note | - | PIN | ",
            "GRL | Grill | #82c91e | wall | Grill | grills | RECT | ",
            "AC | AC unit | #99e9f2 | wall ceil | AC | hvac | RECT | ",
            "CDP | Ceiling drop | #96f2d7 | ceil | Drop | false-ceiling | RECT | ",
            "PLB | Plumbing point | #15aabf | wall floor | Plumbing | plumbing | POINT | ",
            "CTR | Curtain track | #c2255c | wall ceil | Track | curtains | RECT | ",
            "CRP | Carpentry unit | #a0522d | wall floor | Unit | carpentry | RECT | ",
            "LV | Low-voltage point | #7048e8 | wall ceil | LV point | lowvolt | POINT | ",
            "SAN | Sanitary ware | #d0ebff | wall floor | Sanitary | plumbing | RECT | ink",
            "DEF | Defect | #f03e3e | wall floor ceil | Defect | - | RECT | ",
        ), MarkTypes.NEW.map(::row))
        assertEquals(V53, MarkTypes.TOKENS)
        // v52's twelve letters are the wall's own first twelve rows; no new token is one of them, nor a letter of any surface's v52 list
        assertEquals(V52, SurveyPrep.TYPES.getValue("wall").map { it.letter }.take(12))
        assertEquals(10, MarkTypes.TOKENS.toSet().size); assertTrue((MarkTypes.TOKENS intersect V52.toSet()).isEmpty())
        for (x in MarkTypes.NEW) { assertEquals(x, MarkTypes.byToken(x.t)); assertTrue(MarkTypes.isNew(x.t)) }
        for (t in V52 + listOf("", "QQ", "note", "Note")) { assertNull(MarkTypes.byToken(t), t); assertFalse(MarkTypes.isNew(t), t); assertNull(MarkTypes.canGo(t, "wall"), t) }
        // no token is the chain's letter, a surface's code letter doubled is still a token of its own, and each is 2-4 capitals
        for (t in V53) { assertTrue(Regex("^[A-Z]{2,4}$").matches(t), t); assertFalse(t == SurveyChain.LETTER) }
        // both languages, as the page shows them (L57 over T53's names; AC and LV are abbreviations kept as written)
        assertEquals(listOf("Note", "Grill", "AC unit", "Ceiling drop", "Plumbing point", "Curtain track", "Carpentry unit", "Low-voltage point", "Sanitary ware", "Defect"),
            V53.map { MarkTypes.typeName(Tx.EN, it) })
        assertEquals(listOf("नोंद", "ग्रिल", "AC युनिट", "छताचा ड्रॉप", "प्लंबिंग पॉइंट", "पडद्याचा ट्रॅक", "सुतारकाम युनिट", "लो-व्होल्टेज पॉइंट", "सॅनिटरीवेअर", "दोष"),
            V53.map { MarkTypes.typeName(Tx.MR, it) })
        assertEquals(listOf("Note", "Grill", "AC", "Drop", "Plumbing", "Track", "Unit", "LV point", "Sanitary", "Defect"),
            MarkTypes.NEW.map { SurveyPrepPalette.short(Tx.EN, it.name) })
        assertEquals(listOf("नोंद", "ग्रिल", "AC", "ड्रॉप", "प्लंबिंग", "ट्रॅक", "युनिट", "LV पॉइंट", "सॅनिटरी", "दोष"),
            MarkTypes.NEW.map { SurveyPrepPalette.short(Tx.MR, it.name) })
        // the short names are the palette's own table now (index61c2.html 6608), beside v52's thirteen
        assertEquals(MarkTypes.NEW.associate { it.name to it.short }, MarkTypes.SHORT)
        for ((name, short) in MarkTypes.SHORT) assertEquals(short, SurveyPrepPalette.SHORT[name], name)
        assertEquals(23, SurveyPrepPalette.SHORT.size)
        // a v52 letter's name is the WALL's, a token nobody knows is shown as written
        assertEquals("Switch plate", MarkTypes.nameOf("S")); assertEquals("Window", MarkTypes.nameOf("W")); assertEquals("Ceiling drop", MarkTypes.nameOf("CDP"))
        assertEquals("QQ", MarkTypes.nameOf("QQ")); assertEquals("स्विच प्लेट", MarkTypes.typeName(Tx.MR, "S")); assertEquals("QQ", MarkTypes.typeName(Tx.MR, "QQ"))
    }

    @Test fun `the type table of each kind of surface - v52's rows, then the ten where each may go - 21, 11 and 13`() {
        fun row(k: String) = SurveyPrep.TYPES.getValue(k).joinToString(" ") { it.letter }
        assertEquals("W D C B O N L E P S F X NOTE GRL AC PLB CTR CRP LV SAN DEF", row("wall"))
        assertEquals("C S O E P X NOTE PLB CRP SAN DEF", row("floor"))
        assertEquals("B C E P F O X NOTE AC CDP CTR LV DEF", row("ceil"))
        assertEquals(listOf(21, 11, 13), KINDS.map { SurveyPrep.TYPES.getValue(it).size })
        assertEquals(listOf(9, 5, 6), KINDS.map { MarkTypes.typesOn(it).size })
        for (k in KINDS) {
            val all = SurveyPrep.TYPES.getValue(k); val new = MarkTypes.typesOn(k)
            // the tail IS typesOn, row for row; a type is on the list exactly where it may go
            assertEquals(new.map { Triple(it.letter, it.name, it.colour) }, all.takeLast(new.size).map { Triple(it.letter, it.name, it.colour) }, k)
            assertEquals(V53.filter { MarkTypes.canGo(it, k) == true }, new.map { it.letter }, k)
            assertEquals(all.size, all.map { it.letter }.toSet().size, "$k: a token names one type")
            assertEquals(Trades.ON.getValue(k), all.map { it.letter }, k)
            // looked up by its WHOLE token on every surface of that kind: name and colour, never the grey of an unknown type
            val s = SURF.getValue(k)
            for (x in MarkTypes.NEW) if (k in x.kinds) {
                assertEquals(x.name, SurveyPrep.typeOf(s, x.t).name, "${x.t} on $k"); assertEquals(x.colour, SurveyPrep.typeOf(s, x.t).colour, "${x.t} on $k")
                assertEquals(x.colour, SurveyPrepPalette.chipColour(s, x.t))
            } else assertEquals(x.t, SurveyPrep.typeOf(s, x.t).name, "${x.t} is not a type of the $k: the grey unknown")
        }
        for (s in listOf("front", "right", "back", "left")) assertEquals(SurveyPrep.TYPES.getValue("wall"), SurveyPrep.types(s))
        // where each of the ten goes, said once more in words: Note and Defect everywhere; Grill on walls only; Ceiling drop on the ceiling only
        assertEquals(mapOf("NOTE" to "wall floor ceil", "GRL" to "wall", "AC" to "wall ceil", "CDP" to "ceil", "PLB" to "wall floor", "CTR" to "wall ceil",
            "CRP" to "wall floor", "LV" to "wall ceil", "SAN" to "wall floor", "DEF" to "wall floor ceil"),
            V53.associateWith { t -> KINDS.filter { MarkTypes.canGo(t, it) == true }.joinToString(" ") })
        assertEquals(true, MarkTypes.canGo("GRL", "left")); assertEquals(false, MarkTypes.canGo("GRL", "floor"))
    }

    @Test fun `points, the pin and rectangles - a Plumbing point and a Low-voltage point join E, P and F, and a Note is none of them`() {
        assertEquals(listOf("E", "P", "F", "PLB", "LV"), ALLT.filter { SurveyPrep.isPoint(it) })
        assertEquals(listOf("PLB", "LV"), ALLT.filter { MarkTypes.isPoint(it) })
        assertEquals(listOf("NOTE"), ALLT.filter { MarkTypes.isPin(it) })
        assertFalse(SurveyPrep.isPoint("NOTE")); assertFalse(MarkTypes.isPoint("E"), "v52's points are SurveyPrep's own")
        assertEquals(MarkTypes.NOTE, MarkLife.NOTE_TYPE); assertEquals(MarkTypes.NOTE, Trades.NOTE)
        // a point's figure is to its centre under the c-keys on every surface (a plan's E alone keeps l r f k)
        for (t in listOf("PLB", "LV")) for (plan in listOf(false, true)) for (k in listOf("l", "r", "s", "t", "f", "k")) assertEquals("c$k", SurveyPrep.ckey(plan, t, k))
        // an older or resized box of a new point is made a 50 mm spot on opening and its EDGE readings go to its centre
        // keys, each plus half the box -- ON A PLAN AS ON A WALL (the L5 review; the page's normPoints over ISPOINT,
        // index61c2.html 4944-4955: `toC=plan&&e.t==='P'`, so only a plan Bulb moves unchanged there)
        val p = SurveyPrep.fresh(L, W, H)
        p.surface("front").marks.add(SurveyPrep.Mark("a", "PLB", "FPLB1", 300.0, 450.0, 800.0, 950.0))
        p.surface("ceil").marks.add(SurveyPrep.Mark("b", "LV", "CLV2", 1000.0, 1200.0, 2000.0, 2100.0))
        p.surface("front").marks.add(SurveyPrep.Mark("c", "NOTE", "FNOTE1", 100.0, 400.0, 100.0, 400.0))
        p.actual.putAll(mapOf("FPLB1.l" to 300.0, "FPLB1.s" to 800.0, "FPLB1.w" to 150.0, "CLV2.f" to 7.0))
        val r = SurveyPrep.normPoints(p)
        assertEquals(2, r.points)
        val a = r.prep.markById("a")!!.second; val b = r.prep.markById("b")!!.second; val c = r.prep.markById("c")!!.second
        assertEquals(listOf(350.0, 400.0, 850.0, 900.0), listOf(a.x0, a.x1, a.y0, a.y1)); assertEquals(listOf(1075.0, 1125.0, 2025.0, 2075.0), listOf(b.x0, b.x1, b.y0, b.y1))
        assertEquals(listOf(100.0, 400.0, 100.0, 400.0), listOf(c.x0, c.x1, c.y0, c.y1), "a Note is no point: its box is left as it came")
        assertEquals(mapOf("FPLB1.cl" to 375.0, "FPLB1.cs" to 875.0, "CLV2.cf" to 57.0), r.prep.actual, "the ceiling's LV: 7 + half its 100 mm box")
    }

    /**
     * THE L5 REVIEW, finding 1. What the PAGE'S OWN normPoints answers for an older box of a point with edge readings
     * (cut out of index61c2.html 4944-4955 and run in node: `proto-tables.mjs`, "L5 REVIEW normPoints"), typed out:
     * surface, type, code, the box, its readings -> the readings after, the spot. A plan Plumbing point's and a plan
     * Low-voltage point's l r f k are EDGE readings -- no build measured one to its centre under those keys -- and take
     * half the measured width (else the box's) or half the box's height, as on a wall. L5 as built moved them unchanged
     * (CLV2.cf 7, GPLB1.cl 40 / .ck 60), the phone's rule for a plan Bulb and Fan reaching two types it was never made for.
     */
    private val NORM_PAGE = listOf(
        "floor PLB GPLB1 100,250,200,350 l=40 k=60 -> GPLB1.ck=135 GPLB1.cl=115 @ 150,200,250,300",
        "floor PLB GPLB2 100,250,200,350 r=40 f=60 w=120 -> GPLB2.cf=135 GPLB2.cr=100 @ 150,200,250,300",
        "ceil LV CLV2 1000,1200,2000,2100 f=7 -> CLV2.cf=57 @ 1075,1125,2025,2075",
        "ceil LV CLV3 1000,1200,2000,2100 l=10 k=20 h=80 -> CLV3.ck=60 CLV3.cl=110 @ 1075,1125,2025,2075",
        "front PLB FPLB1 300,450,800,950 l=300 s=800 w=150 -> FPLB1.cl=375 FPLB1.cs=875 @ 350,400,850,900",
        "front LV FLV1 300,450,800,950 r=300 t=800 -> FLV1.cr=375 FLV1.ct=875 @ 350,400,850,900",
        // the two the page and the phone already agreed on: a plan Bulb unchanged, a plan Electric point keeping l r f k
        "ceil P CP9 500,650,800,950 r=900 f=700 -> CP9.cf=700 CP9.cr=900 @ 550,600,850,900",
        "floor P GP9 925,1075,1925,2075 l=1000 k=2000 -> GP9.ck=2000 GP9.cl=1000 @ 975,1025,1975,2025",
        "floor E GE1 0,150,0,150 l=75 k=75 -> GE1.k=75 GE1.l=75 @ 50,100,50,100",
    )

    private fun n1(v: Double) = if (v == Math.rint(v)) v.toLong().toString() else v.toString()

    @Test fun `an older box of a plan Plumbing or Low-voltage point - its edge readings take half the box, as the page's normPoints answers`() {
        val got = NORM_PAGE.map { row ->
            val (given, _) = row.split(" -> "); val f = given.split(" ")
            val (s, t, code) = f; val box = f[3].split(",").map { it.toDouble() }
            val p = SurveyPrep.fresh(L, W, H)
            p.surface(s).marks.add(SurveyPrep.Mark("a", t, code, box[0], box[1], box[2], box[3]))
            for (kv in f.drop(4)) kv.split("=").let { (k, v) -> p.actual["$code.$k"] = v.toDouble() }
            val r = SurveyPrep.normPoints(p); val e = r.prep.markById("a")!!.second
            assertFalse(SurveyPrep.normPoints(r.prep).changed, "$code: the edge keys are consumed, so a second pass changes nothing")
            given + " -> " + r.prep.actual.toSortedMap().map { (k, v) -> k + "=" + n1(v) }.joinToString(" ") + " @ " + listOf(e.x0, e.x1, e.y0, e.y1).joinToString(",") { n1(it) }
        }
        assertEquals(NORM_PAGE.joinToString("\n"), got.joinToString("\n"))
        // WHERE THE PHONE STILL DIFFERS, on purpose and not L5's: a plan FAN moves unchanged (v52 slice 4, SurveyPrepTest
        // "a plan Fan moves unchanged"), where the page adds half its box as for any plan point but a Bulb -- CF1.cl 1100
        // here, 1200 on the page. No build has written an l r f k reading for a ceiling Fan; left for the prototype.
        val fan = SurveyPrep.fresh(L, W, H)
        fan.surface("ceil").marks.add(SurveyPrep.Mark("f", "F", "CF1", 1000.0, 1200.0, 1000.0, 1200.0)); fan.actual["CF1.l"] = 1100.0
        assertEquals(mapOf("CF1.cl" to 1100.0), SurveyPrep.normPoints(fan).prep.actual)
        // and the phone's own extension (v52 slice 4), which the page does not have: an edge reading that arrives for a
        // point ALREADY a spot converts too -- on a plan as on a wall, by its 50 mm box where nothing else is known
        val late = SurveyPrep.fresh(L, W, H)
        late.surface("floor").marks.add(SurveyPrep.Mark("g", "PLB", "GPLB3", 500.0, 550.0, 500.0, 550.0)); late.actual.putAll(mapOf("GPLB3.l" to 40.0, "GPLB3.f" to 60.0))
        assertEquals(mapOf("GPLB3.cl" to 65.0, "GPLB3.cf" to 85.0), SurveyPrep.normPoints(late).prep.actual)
    }

    // ------------------------------------------------------------------ the sub-kinds

    /** SUBS (2423-2466), typed out: id=label, the first the default. */
    private val SUBS: Map<String, Map<String, String>> = mapOf(
        "S" to mapOf("wall" to "switch=Switch plate, db=DB / MCB board, meter=Meter board"),
        "GRL" to mapOf("wall" to "window=Window grill, balcony=Balcony grill, railing=Railing, door=Safety door grill"),
        "AC" to mapOf("wall" to "split=Split indoor unit, window-ac=Window AC, outdoor=Outdoor unit", "ceil" to "cassette=Cassette, duct=Ductable grille"),
        "CDP" to mapOf("ceil" to "bulkhead=Bulkhead, cove=Cove, full=Full false ceiling"),
        "PLB" to mapOf("wall" to "cold=Cold, hot=Hot, mixer=Mixer (hot + cold), wm=Washing machine, ro=RO / purifier, waste=Waste outlet",
            "floor" to "trap=Floor trap, waste=Waste outlet, cold=Cold, hot=Hot"),
        "CTR" to mapOf("wall" to "track=Curtain track, rod=Rod, roller=Roller blind, vertical=Vertical blind",
            "ceil" to "track=Curtain track, rod=Rod, roller=Roller blind, vertical=Vertical blind"),
        "CRP" to mapOf("wall" to "wardrobe=Wardrobe, kbase=Kitchen base, kwall=Kitchen wall unit, tv=TV unit, study=Study, shoe=Shoe rack, vanity=Vanity, other=Other unit",
            "floor" to "bed=Bed, platform=Kitchen platform, island=Island, other=Other unit"),
        "LV" to mapOf("wall" to "camera=Camera, intercom=Intercom / video door phone, data=Data / LAN, tv=TV point, wifi=Wi-Fi access point",
            "ceil" to "camera=Camera, wifi=Wi-Fi access point, smoke=Smoke detector"),
        "SAN" to mapOf("wall" to "basin=Wash basin, wc-wall=Wall-hung WC, shower=Shower, geyser=Geyser, sink=Kitchen sink, mirror=Mirror cabinet",
            "floor" to "wc=WC, shower-area=Shower area, tub=Bathtub"),
        "DEF" to KINDS.associateWith { "crack=Crack, damp=Damp / seepage, hollow=Hollow plaster, peeling=Peeling paint, tile=Broken / hollow tile" },
    )

    /** What the page shows for each sub-kind under मराठी (L57 over displayName), in the table's order. */
    private val SUBS_MR: Map<String, Map<String, List<String>>> = mapOf(
        "S" to mapOf("wall" to listOf("स्विच प्लेट", "DB / MCB बोर्ड", "मीटर बोर्ड")),
        "GRL" to mapOf("wall" to listOf("खिडकी ग्रिल", "बाल्कनी ग्रिल", "रेलिंग", "सेफ्टी डोअर ग्रिल")),
        "AC" to mapOf("wall" to listOf("स्प्लिट इनडोअर युनिट", "विंडो AC", "आउटडोअर युनिट"), "ceil" to listOf("कॅसेट", "डक्टेबल ग्रिल")),
        "CDP" to mapOf("ceil" to listOf("बल्कहेड", "कोव्ह", "पूर्ण फॉल्स सीलिंग")),
        "PLB" to mapOf("wall" to listOf("प्लंबिंग पॉइंट · थंड", "प्लंबिंग पॉइंट · गरम", "प्लंबिंग पॉइंट · मिक्सर (गरम + थंड)", "प्लंबिंग पॉइंट · वॉशिंग मशीन",
            "प्लंबिंग पॉइंट · RO / प्युरिफायर", "प्लंबिंग पॉइंट · वेस्ट आउटलेट"),
            "floor" to listOf("प्लंबिंग पॉइंट · फ्लोअर ट्रॅप", "प्लंबिंग पॉइंट · वेस्ट आउटलेट", "प्लंबिंग पॉइंट · थंड", "प्लंबिंग पॉइंट · गरम")),
        "CTR" to listOf("wall", "ceil").associateWith { listOf("पडद्याचा ट्रॅक", "रॉड", "रोलर ब्लाइंड", "व्हर्टिकल ब्लाइंड") },
        "CRP" to mapOf("wall" to listOf("वॉर्डरोब", "किचन खालचे युनिट", "किचन वरचे युनिट", "TV युनिट", "स्टडी", "शू रॅक", "व्हॅनिटी", "इतर युनिट"),
            "floor" to listOf("बेड", "किचन ओटा", "आयलंड", "इतर युनिट")),
        "LV" to mapOf("wall" to listOf("कॅमेरा", "इंटरकॉम / व्हिडिओ डोअर फोन", "डेटा / LAN", "TV पॉइंट", "Wi-Fi ॲक्सेस पॉइंट"),
            "ceil" to listOf("कॅमेरा", "Wi-Fi ॲक्सेस पॉइंट", "स्मोक डिटेक्टर")),
        "SAN" to mapOf("wall" to listOf("वॉश बेसिन", "वॉल-हँग WC", "शॉवर", "गीझर", "किचन सिंक", "आरसा कॅबिनेट"), "floor" to listOf("WC", "शॉवर एरिया", "बाथटब")),
        "DEF" to KINDS.associateWith { listOf("तडा", "ओल / पाझर", "पोकळ प्लास्टर", "निघालेला रंग", "तुटलेली / पोकळ टाइल") },
    )

    @Test fun `the sub-kinds, row by row - ids, labels in both languages, the first the default, and S's only on a wall`() {
        assertEquals(SUBS, MarkTypes.SUBS.mapValues { (_, by) -> by.mapValues { (_, l) -> l.joinToString(", ") { it.id + "=" + it.label } } })
        assertEquals(listOf("S", "GRL", "AC", "CDP", "PLB", "CTR", "CRP", "LV", "SAN", "DEF"), MarkTypes.SUBS.keys.toList())
        for ((t, by) in SUBS_MR) for ((k, mr) in by) assertEquals(mr, MarkTypes.subsOf(t, k).map { MarkTypes.displayName(Tx.MR, t, k, it.id) }, "$t on $k")
        assertEquals(SUBS.mapValues { it.value.keys }, SUBS_MR.mapValues { it.value.keys })
        // 77 rows, 57 labels: Waste outlet, Cold, Hot, Camera, Wi-Fi access point, Other unit, the four curtain kinds and the
        // five defects serve more than one kind of surface
        assertEquals(77, MarkTypes.SUBS.values.sumOf { it.values.sumOf { l -> l.size } })
        assertEquals(57, MarkTypes.SUBS.values.flatMap { it.values.flatten() }.map { it.label }.toSet().size)
        for (t in ALLT) for (k in KINDS) {
            val l = MarkTypes.subsOf(t, k)
            assertEquals(l.size, l.map { it.id }.toSet().size, "$t on $k: an id names one sub-kind")
            assertEquals(l.firstOrNull()?.id, MarkTypes.firstSub(t, k))
            // sub-kinds only where the type may go (S: a wall's Switch plate, never the floor's Step) -- and Note has none
            if (l.isNotEmpty()) assertTrue(t == "S" && k == "wall" || MarkTypes.canGo(t, k) == true, "$t on $k")
            if (t in V52 - "S" || t == "NOTE") assertTrue(l.isEmpty(), "$t on $k")
            for (s in l) { assertEquals(s, MarkTypes.subInfo(t, k, s.id)); assertTrue(Regex("^[a-z][a-z-]*$").matches(s.id), s.id) }
            // an absent or unknown sub-kind reads as the first
            for (bad in listOf(null, "", "zzz", "Wardrobe")) assertEquals(l.firstOrNull(), MarkTypes.subInfo(t, k, bad), "$t on $k: $bad")
        }
        // every new type but the Note has sub-kinds wherever it goes
        for (x in MarkTypes.NEW) for (k in x.kinds) assertEquals(x.t != "NOTE", MarkTypes.subsOf(x.t, k).isNotEmpty(), "${x.t} on $k")
        // a surface id reads as its kind
        assertEquals(MarkTypes.subsOf("CRP", "wall"), MarkTypes.subsOf("CRP", "back")); assertEquals("bed", MarkTypes.firstSub("CRP", "floor"))
        assertEquals(KINDS, MarkTypes.KINDS); assertEquals(listOf("wall", "wall", "floor", "ceil"), listOf("left", "wall", "floor", "ceil").map(MarkTypes::kindOf))
    }

    @Test fun `the name a mark shows - its sub-kind's label, a Plumbing point's after its type, the floor's S a Step`() {
        assertEquals("Wardrobe", MarkTypes.displayName(Tx.EN, "CRP", "front", null)); assertEquals("TV unit", MarkTypes.displayName(Tx.EN, "CRP", "front", "tv"))
        assertEquals("Bed", MarkTypes.displayName(Tx.EN, "CRP", "floor", "tv"), "tv is no floor unit: the floor's first")
        assertEquals("Full false ceiling", MarkTypes.displayName(Tx.EN, "CDP", "ceil", "full")); assertEquals("पूर्ण फॉल्स सीलिंग", MarkTypes.displayName(Tx.MR, "CDP", "ceil", "full"))
        assertEquals("Plumbing point · Mixer (hot + cold)", MarkTypes.displayName(Tx.EN, "PLB", "left", "mixer"))
        assertEquals("प्लंबिंग पॉइंट · मिक्सर (गरम + थंड)", MarkTypes.displayName(Tx.MR, "PLB", "left", "mixer"))
        assertEquals("Plumbing point", MarkTypes.displayName(Tx.EN, "PLB", "ceil", "mixer"), "where it has no sub-kinds, the type's name alone")
        assertEquals("Step", MarkTypes.displayName(Tx.EN, "S", "floor", null)); assertEquals("पायरी", MarkTypes.displayName(Tx.MR, "S", "floor", "db"))
        assertEquals("Switch plate", MarkTypes.displayName(Tx.EN, "S", "front", null)); assertEquals("DB / MCB board", MarkTypes.displayName(Tx.EN, "S", "front", "db"))
        assertEquals("मीटर बोर्ड", MarkTypes.displayName(Tx.MR, "S", "back", "meter"))
        assertEquals("Note", MarkTypes.displayName(Tx.EN, "NOTE", "ceil", null)); assertEquals("Window", MarkTypes.displayName(Tx.EN, "W", "front", "zzz"))
        assertEquals("WC", MarkTypes.displayName(Tx.EN, "SAN", "floor", null)); assertEquals("WC", MarkTypes.displayName(Tx.MR, "SAN", "floor", null))
        // of a mark: by what it stores
        val e = SurveyPrep.Mark("a", "SAN", "FSAN1", 0.0, 1.0, 0.0, 1.0, mapOf("sub" to "geyser"))
        assertEquals("Geyser", MarkTypes.displayName(Tx.EN, "front", e)); assertEquals("गीझर", MarkTypes.displayName(Tx.MR, "front", e))
        assertEquals("geyser", MarkTypes.subOf("front", e)!!.id); assertEquals("wc", MarkTypes.subOf("floor", e)!!.id, "geyser is no floor fitting: the floor's first")
        assertNull(MarkTypes.subOf("front", SurveyPrep.Mark("w", "W", "FW1", 0.0, 1.0, 0.0, 1.0)))
    }

    /**
     * Every name, short name and label is SHOWN, so each is read from the tables -- none is a literal in the code, however
     * the same it reads in both languages (the L1 review's rule; WC, AC and the article are the cases that tempt). Asked
     * of a table whose every cell is a marker, each must come back as cells, whole.
     */
    @Test fun `every name, short name, label and refusal is cells of the tables, never a literal in the code`() {
        val tab = Tx::class.java.getDeclaredField("tab").apply { isAccessible = true }
        val cells = (tab.get(Tx.EN) as Array<*>).size
        val probe = Tx::class.java.getDeclaredConstructor(Lang::class.java, Array<String>::class.java).apply { isAccessible = true }
            .newInstance(Lang.EN, Array(cells) { "⟦$it⟧" })
        val cell = Regex("^⟦\\d+⟧$")
        for (x in MarkTypes.NEW) {
            assertTrue(cell.matches(MarkTypes.typeName(probe, x.t)), "name of ${x.t}: ${MarkTypes.typeName(probe, x.t)}")
            assertTrue(cell.matches(SurveyPrepPalette.short(probe, x.name)), "short of ${x.t}: ${SurveyPrepPalette.short(probe, x.name)}")
        }
        val labels = MarkTypes.SUBS.values.flatMap { it.values.flatten() }
        for (s in labels) assertTrue(cell.matches(MarkTypes.subLabel(probe, s)), "label of ${s.id}: ${MarkTypes.subLabel(probe, s)}")
        assertEquals(57, labels.map { MarkTypes.subLabel(probe, it) }.toSet().size, "57 labels, 57 cells")
        // the ten names are ten cells; of the ten short names seven are their own, and Note, Grill and Defect the name's
        assertEquals(10, MarkTypes.NEW.map { MarkTypes.typeName(probe, it.t) }.toSet().size)
        assertEquals(listOf("NOTE", "GRL", "DEF"), MarkTypes.NEW.filter { MarkTypes.typeName(probe, it.t) == SurveyPrepPalette.short(probe, it.name) }.map { it.t })
        // a refusal: the article, the name and the place are each a cell (the probe's templates are cells too, so the whole is one)
        for (t in V53) for (k in KINDS) MarkTypes.placeRefusal(probe, t, k)?.let { assertTrue(cell.matches(it), "$t on $k: $it") }
        assertTrue(cell.matches(probe.t53_a) && cell.matches(probe.t53_an) && probe.t53_a != probe.t53_an)
        // a measure's words
        for (t in V53) for (k in KINDS) for (m in MarkTypes.measures(SURF.getValue(k), SurveyPrep.Mark("x", t, "", 100.0, 400.0, 100.0, 300.0), W, H).orEmpty())
            assertTrue(Regex("^(⟦\\d+⟧){1,2}$").matches(m.what(probe)), "$t on $k ${m.key}: ${m.what(probe)}")
    }

    @Test fun `a sub-kind is stored only when it is not the first - and a change replaces the map, never edits it`() {
        // what a mark stores (subToStore): the id, unless it is the first, unknown, or of another kind of surface
        assertEquals(listOf(null, null, "kbase", "other", null, null, null), listOf(null, "wardrobe", "kbase", "other", "bed", "zzz", "").map { MarkTypes.subToStore("CRP", "front", it) })
        assertEquals(listOf(null, "platform", null), listOf("bed", "platform", "kbase").map { MarkTypes.subToStore("CRP", "floor", it) })
        assertEquals(listOf(null, "db", "meter"), listOf("switch", "db", "meter").map { MarkTypes.subToStore("S", "right", it) })
        assertNull(MarkTypes.subToStore("S", "floor", "db"), "the floor's S is a Step: no sub-kind")
        assertNull(MarkTypes.subToStore("W", "front", "anything")); assertNull(MarkTypes.subToStore("NOTE", "front", "x"))
        // setSub on a mark: written, changed, and taken away again by the first, an unknown one and none
        val before = linkedMapOf<String, Any?>("locked" to true, "note" to "left of the door", "later" to listOf(1, null))
        val e = SurveyPrep.Mark("a", "CRP", "FCRP1", 150.0, 1350.0, 0.0, 2100.0, before)
        MarkTypes.setSub("front", e, "kbase")
        assertNotSame<Map<String, Any?>>(before, e.extra); assertEquals<Map<String, Any?>>(mapOf("locked" to true, "note" to "left of the door", "later" to listOf(1, null)), before, "the old map is as it was")
        assertEquals(listOf("locked", "note", "later", "sub"), e.extra.keys.toList()); assertEquals("kbase", Trades.subOf(e.extra))
        assertEquals(listOf(150.0, 1350.0, 0.0, 2100.0), listOf(e.x0, e.x1, e.y0, e.y1), "the box is the caller's: a kind changed keeps the box the mark has")
        MarkTypes.setSub("front", e, "tv"); assertEquals("tv", e.extra["sub"]); assertEquals(listOf("locked", "note", "later", "sub"), e.extra.keys.toList())
        for (gone in listOf("wardrobe", "zzz", null, "bed")) {
            MarkTypes.setSub("front", e, "tv"); MarkTypes.setSub("front", e, gone)
            assertEquals(before, e.extra, "$gone: the key is deleted, every other key whole"); assertEquals("wardrobe", MarkTypes.subOf("front", e)!!.id)
        }
        // withSub is the one writer; a map with no sub and a default asked stays equal, and is still a new map
        val m = mapOf<String, Any?>("x" to 1)
        assertEquals(m, MarkTypes.withSub(m, "CRP", "front", "wardrobe")); assertNotSame(m, MarkTypes.withSub(m, "CRP", "front", "wardrobe"))
        assertEquals(mapOf("x" to 1, "sub" to "duct"), MarkTypes.withSub(m, "AC", "ceil", "duct"))
        assertEquals(m, MarkTypes.withSub(m, "AC", "front", "duct"), "a ceiling's kind on a wall is no kind of a wall's AC unit")
        // the key is Trades' -- one name for the stored key, read by tradeOf and by the sheet a picture leaves in
        assertEquals("sub", Trades.KEY_SUB); assertTrue(Trades.KEY_SUB in MarkLife.KEYS); assertTrue(Trades.KEY_SUB in SheetMarks.KEPT)
    }

    // ------------------------------------------------------------------ trades and services

    @Test fun `a type's trade is Trades' - the table's own column, the Defect's by its sub-kind, a Low-voltage point's service by its kind`() {
        for (x in MarkTypes.NEW) {
            // the column equals Trades.FIXED; the two with none are the two tradeOf decides itself
            assertEquals(Trades.FIXED[x.t], x.trade, x.t)
            for (k in x.kinds) for (sub in listOf<String?>(null) + MarkTypes.subsOf(x.t, k).map { it.id }) {
                val want = when (x.t) { "NOTE" -> "general"; "DEF" -> if (sub == "tile") "tiling" else "painting"; else -> x.trade }
                assertEquals(want, Trades.tradeOf(x.t, k, sub), "${x.t} · $sub on $k")
            }
        }
        assertEquals(listOf("NOTE", "DEF"), MarkTypes.NEW.filter { it.trade == null }.map { it.t })
        // a sub-kind's own trade: only the Defect's Broken / hollow tile, Tiling's -- and tradeOf reads exactly that
        val own = MarkTypes.SUBS.flatMap { (t, by) -> by.flatMap { (k, l) -> l.filter { it.trade != null }.map { Triple(t, k, it) } } }
        assertEquals(listOf("DEF wall tile tiling", "DEF floor tile tiling", "DEF ceil tile tiling"), own.map { (t, k, s) -> "$t $k ${s.id} ${s.trade}" })
        for ((t, k, s) in own) assertEquals(s.trade, Trades.tradeOf(t, k, s.id))
        assertEquals(Trades.SUB_TILE, "tile")
        // a Low-voltage point's service: the camera, data, TV, Wi-Fi and smoke kinds are CCTV's, the intercom Intercom's -- Trades.SERVICE_TRADE's own lists
        val services = MarkTypes.SUBS.flatMap { (t, by) -> by.flatMap { (_, l) -> l.filter { it.service != null }.map { t to it } } }
        assertTrue(services.all { it.first == "LV" }); assertEquals(8, services.size)
        for ((_, s) in services) assertTrue(s.id in Trades.SERVICE_TRADE.getValue(s.service!!).subs!!, "${s.id} answers ${s.service}")
        assertEquals(Trades.SERVICE_TRADE.getValue("cctv").subs!!.toSet(), services.filter { it.second.service == "cctv" }.map { it.second.id }.toSet())
        assertEquals(Trades.SERVICE_TRADE.getValue("intercom").subs!!.toSet(), services.filter { it.second.service == "intercom" }.map { it.second.id }.toSet())
        for (l in listOf(MarkTypes.subsOf("LV", "wall"), MarkTypes.subsOf("LV", "ceil"))) assertTrue(l.all { it.service != null })
        // of a mark, by what it stores
        fun mark(t: String, vararg kv: Pair<String, Any?>) = SurveyPrep.Mark("a", t, "F${t}1", 0.0, 50.0, 0.0, 50.0, mapOf(*kv))
        assertEquals("tiling", Trades.of("floor", mark("DEF", "sub" to "tile"))); assertEquals("painting", Trades.of("floor", mark("DEF", "sub" to "damp")))
        assertEquals("carpentry", Trades.of("front", mark("CRP", "sub" to "kbase"))); assertEquals("plumbing", Trades.of("front", mark("NOTE", "cat" to "plumbing")))
        // More ▸'s rows carry every type of the surface once, the ten among them (slice L6 draws them)
        for (k in KINDS) assertEquals(SurveyPrep.TYPES.getValue(k).map { it.letter }.toSet(), Trades.moreRows(k).flatMap { it.types }.toSet(), k)
    }

    // ------------------------------------------------------------------ where a new mark starts

    @Test fun `SurveyPrep defaults asks the new table first - a new type's box by its sub-kind, v52's letters exactly where they were`() {
        // v52: untouched, with or without a sub-kind nobody gave them
        for (s in listOf("front", "floor", "ceil")) { val (a, b) = if (s == "front") W to H else W to L
            for (t in V52) if (!(t == "S" && s == "front")) {
                assertNull(MarkTypes.defaults(s, t, null, a, b), "$t on $s"); assertNull(MarkTypes.defaults(s, t, "db", a, b), "$t on $s")
                assertEquals(SurveyPrep.defaults(s, t, a, b).toList(), SurveyPrep.defaults(s, t, a, b, "db").toList(), "$t on $s") } }
        assertEquals(listOf((W - 1200) / 2, (W - 1200) / 2 + 1200, 900.0, 2100.0), SurveyPrep.defaults("front", "W", W, H).toList())
        // S: no sub-kind and `switch` are v52's plate; a DB board and a Meter board have their own boxes
        assertEquals(listOf(150.0, 350.0, 1100.0, 1190.0), SurveyPrep.defaults("front", "S", W, H).toList())
        assertEquals(SurveyPrep.defaults("front", "S", W, H).toList(), SurveyPrep.defaults("front", "S", W, H, "switch").toList())
        assertEquals(listOf(150.0, 450.0, 1500.0, 1900.0), SurveyPrep.defaults("front", "S", W, H, "db").toList())
        assertEquals(listOf(150.0, 600.0, 1500.0, 2100.0), SurveyPrep.defaults("front", "S", W, H, "meter").toList())
        assertEquals(listOf((W - 900) / 2, (W - 900) / 2 + 900, 150.0, 450.0), SurveyPrep.defaults("floor", "S", W, L, "db").toList(), "the floor's S is the Step, whatever it is given")
        // the ten: no sub-kind is the first; each named kind its own; all inside the face
        assertEquals(listOf(150.0, 1350.0, 0.0, 2100.0), SurveyPrep.defaults("front", "CRP", W, H).toList())                   // a Wardrobe, 1200 x 2100 on the floor
        assertEquals(SurveyPrep.defaults("front", "CRP", W, H).toList(), SurveyPrep.defaults("front", "CRP", W, H, "wardrobe").toList())
        assertEquals(listOf(150.0, 1950.0, 0.0, 850.0), SurveyPrep.defaults("front", "CRP", W, H, "kbase").toList())
        assertEquals(listOf(150.0, 1950.0, 1500.0, 2200.0), SurveyPrep.defaults("front", "CRP", W, H, "kwall").toList())
        assertEquals(listOf(150.0, 1950.0, 0.0, 2000.0), SurveyPrep.defaults("floor", "CRP", W, L).toList())                   // a Bed, against the back wall
        assertEquals(listOf((W - 1200) / 2, (W - 1200) / 2 + 1200, (L - 800) / 2, (L - 800) / 2 + 800), SurveyPrep.defaults("floor", "CRP", W, L, "island").toList())
        assertEquals(listOf(300.0, W - 300, 300.0, L - 300), SurveyPrep.defaults("ceil", "CDP", W, L, "full").toList())        // POP: the whole ceiling less a 300 margin
        assertEquals(listOf(0.0, W, L - 600, L), SurveyPrep.defaults("ceil", "CDP", W, L).toList())                            // a Bulkhead along the front wall
        assertEquals(listOf(300.0, 350.0, 875.0, 925.0), SurveyPrep.defaults("front", "PLB", W, H).toList())                   // Cold, a spot 325 in, 900 up
        assertEquals(listOf(300.0, 350.0, 375.0, 425.0), SurveyPrep.defaults("front", "PLB", W, H, "waste").toList())
        assertEquals(listOf(W - 350, W - 300, H - 350, H - 300), SurveyPrep.defaults("front", "LV", W, H).toList())            // a Camera, 325 under the ceiling
        assertEquals(listOf(W - 350, W - 300, 1375.0, 1425.0), SurveyPrep.defaults("front", "LV", W, H, "intercom").toList())
        assertEquals(listOf(W / 2 - 25, W / 2 + 25, H / 2 - 25, H / 2 + 25), SurveyPrep.defaults("front", "NOTE", W, H).toList())
        assertEquals(listOf((W - 1500) / 2, (W - 1500) / 2 + 1500, H - 250, H - 150), SurveyPrep.defaults("front", "CTR", W, H, "roller").toList())
        for (s in listOf("front", "floor", "ceil")) { val (a, b) = if (s == "front") W to H else W to L
            for (x in MarkTypes.NEW) for (sub in listOf<String?>(null, "zzz") + MarkTypes.subsOf(x.t, s).map { it.id }) {
                val d = SurveyPrep.defaults(s, x.t, a, b, sub)
                assertTrue(d[0] >= 0 && d[1] <= a && d[2] >= 0 && d[3] <= b && d[1] > d[0] && d[3] > d[2], "${x.t} · $sub on $s: ${d.toList()}")
                // a point and the pin are 50 mm spots wherever they may go (where a type may not, its box is the page's 600 mm square: the 360 refuses it first)
                if ((SurveyPrep.isPoint(x.t) || MarkTypes.isPin(x.t)) && MarkTypes.canGo(x.t, s) == true) assertEquals(listOf(50.0, 50.0), listOf(d[1] - d[0], d[3] - d[2]), "${x.t} on $s is a 50 mm spot")
                assertEquals(MarkTypes.defaults(s, x.t, sub, a, b)!!.toList(), d.toList())
            } }
    }

    @Test fun `a new mark - its sub-kind's box, sub stored only when not the first, its code by its whole token`() {
        val p = SurveyPrep.fresh(L, W, H)
        val wardrobe = p.addMark("front", "CRP", W, H, "a")
        assertEquals("FCRP1", wardrobe.code); assertEquals(emptyMap(), wardrobe.extra, "the first sub-kind stores nothing: a v52-shaped mark")
        assertEquals(listOf(150.0, 1350.0, 0.0, 2100.0), listOf(wardrobe.x0, wardrobe.x1, wardrobe.y0, wardrobe.y1))
        assertEquals(emptyMap(), p.addMark("front", "CRP", W, H, "a2", "wardrobe").extra)
        val base = p.addMark("front", "CRP", W, H, "b", "kbase")
        assertEquals("FCRP3", base.code); assertEquals(mapOf("sub" to "kbase"), base.extra); assertEquals(listOf(150.0, 1950.0, 0.0, 850.0), listOf(base.x0, base.x1, base.y0, base.y1))
        assertEquals(emptyMap(), p.addMark("front", "CRP", W, H, "b2", "bed").extra, "a floor's kind on a wall is not stored")
        val pop = p.addMark("ceil", "CDP", W, L, "c", "full")
        assertEquals("CCDP1", pop.code); assertEquals(mapOf("sub" to "full"), pop.extra); assertEquals("Full false ceiling", MarkTypes.displayName(Tx.EN, "ceil", pop))
        val note = p.addMark("floor", "NOTE", W, L, "n"); assertEquals("GNOTE1", note.code); assertEquals(emptyMap(), note.extra)
        // a v52 mark is made exactly as before, with or without the new argument
        val door = p.addMark("front", "D", W, H, "d"); assertEquals("FD1", door.code); assertEquals(emptyMap(), door.extra)
        assertEquals(listOf(300.0, 1200.0, 0.0, 2100.0), listOf(door.x0, door.x1, door.y0, door.y1))
        // S's two new kinds are stored, its plate is not
        assertEquals(mapOf("sub" to "db"), p.addMark("front", "S", W, H, "s1", "db").extra); assertEquals(emptyMap(), p.addMark("front", "S", W, H, "s2", "switch").extra)
        assertEquals(emptyMap(), p.addMark("floor", "S", W, L, "s3", "db").extra, "a Step has no kind")

        // a tap (addMarkAt): the sub-kind's size centred on the finger and slid inside; a Note a 50 mm pin where the finger is
        val tv = p.addMarkAt("front", "CRP", 1339.0, 600.0, W, H, "t", "tv")
        assertEquals(listOf(439.0, 2239.0, 375.0, 825.0), listOf(tv.x0, tv.x1, tv.y0, tv.y1)); assertEquals(mapOf("sub" to "tv"), tv.extra)
        val low = p.addMarkAt("front", "CRP", 100.0, 100.0, W, H, "t2")
        assertEquals(listOf(0.0, 1200.0, 0.0, 2100.0), listOf(low.x0, low.x1, low.y0, low.y1), "a Wardrobe tapped near the corner slides inside")
        val pin = p.addMarkAt("front", "NOTE", 1000.0, 2000.0, W, H, "n2")
        assertEquals(listOf(975.0, 1025.0, 1975.0, 2025.0), listOf(pin.x0, pin.x1, pin.y0, pin.y1)); assertEquals("FNOTE1", pin.code)
        val pt = p.addMarkAt("front", "PLB", 10.0, 500.0, W, H, "pl", "hot")
        assertEquals(listOf(0.0, 50.0, 475.0, 525.0), listOf(pt.x0, pt.x1, pt.y0, pt.y1)); assertEquals(mapOf("sub" to "hot"), pt.extra)
        assertEquals(SurveyPrep.placeAt("CRP", "front", 1339.0, 600.0, W, H, "tv").toList(), listOf(tv.x0, tv.x1, tv.y0, tv.y1))
        assertEquals(SurveyPrep.placeAt("W", "front", 1339.0, 600.0, W, H).toList(), SurveyPrep.placeAt("W", "front", 1339.0, 600.0, W, H, "tv").toList())

        // through the phase's placement (PhaseTools.place): the sub-kind beside what the phase stamps; none given, none stored
        val at = Instant.parse("2026-11-12T05:00:00Z"); var steps = 0
        val wish = PhaseTools.place(p, "back", "SAN", 1300.0, 900.0, W, H, "w", PhaseTools.WISH_LIST, null, at, "geyser") { steps++ }!!.mark
        assertEquals("BSAN1", wish.code); assertEquals("geyser", Trades.subOf(wish.extra)); assertEquals("wish", MarkLife.bornOf(wish)); assertEquals("wish", MarkLife.lifeOf(wish))
        assertEquals(listOf(1075.0, 1525.0, 600.0, 1200.0), listOf(wish.x0, wish.x1, wish.y0, wish.y1)); assertEquals(1, steps)
        val plain = PhaseTools.place(p, "back", "SAN", 1300.0, 900.0, W, H, "w2", PhaseTools.SURVEY, null, at) { steps++ }!!.mark
        assertEquals(emptyMap(), plain.extra); assertEquals(listOf(1025.0, 1575.0, 675.0, 1125.0), listOf(plain.x0, plain.x1, plain.y0, plain.y1))
        assertNull(PhaseTools.place(p, "back", "SAN", 1300.0, 900.0, W, H, "w3", PhaseTools.ESTIMATION, null, at, "geyser") { steps++ }); assertEquals(2, steps)
    }

    /**
     * THE L5 REVIEW, finding 2 -- the contract slice L6 builds on, pinned where it will be read. `place` and `addMarkAt`
     * are given ONE sub-kind and read it against the surface the mark LANDS on. The page never hands one over: addElAt
     * (index61c2.html 5922-5926) asks its memory -- kept per type AND kind of surface, `subMem[t+'@'+kind]` (2471-2473) --
     * with the landed surface, for the box and for the stored key. An id is not a kind's own, so the caller must do the
     * same: the sub-kind chosen on a wall tab, handed in for a tap on the 360 that lands on the floor, is stored THERE
     * wherever the floor has a sub-kind of that id -- and `cold`, the wall's default, is the floor's third.
     */
    @Test fun `the sub-kind handed to place is read against the surface the mark lands on - so the caller asks its memory with that surface, not the tab's`() {
        // the ids two kinds of surface share, and where each sits in its list: only at 0 is it the default, stored as nothing
        val shared = MarkTypes.SUBS.flatMap { (t, by) -> by.values.flatMap { l -> l.map { it.id } }.distinct()
            .map { id -> t to id to by.mapValues { (_, l) -> l.indexOfFirst { it.id == id } }.filterValues { it >= 0 } }.filter { it.second.size > 1 }
            .map { (ti, at) -> "${ti.first} ${ti.second} " + at.map { (k, i) -> "$k:$i" }.joinToString(" ") } }
        assertEquals(listOf("PLB cold wall:0 floor:2", "PLB hot wall:1 floor:3", "PLB waste wall:5 floor:1",
            "CTR track wall:0 ceil:0", "CTR rod wall:1 ceil:1", "CTR roller wall:2 ceil:2", "CTR vertical wall:3 ceil:3", "CRP other wall:7 floor:3",
            "LV camera wall:0 ceil:0", "LV wifi wall:4 ceil:1") + listOf("crack", "damp", "hollow", "peeling", "tile").mapIndexed { i, d -> "DEF $d wall:$i floor:$i ceil:$i" }, shared)

        val p = SurveyPrep.fresh(L, W, H); val at = Instant.parse("2026-11-12T05:00:00Z")
        fun put(s: String, t: String, sub: String?, id: String): SurveyPrep.Mark = PhaseTools.place(p, s, t, 1000.0, 1000.0, W, if (SurveyPrep.isPlan(s)) L else H, id, PhaseTools.SURVEY, null, at, sub) {}!!.mark
        fun shown(s: String, e: SurveyPrep.Mark) = MarkTypes.displayName(Tx.EN, s, e)
        // the wall's default, as a wall tab's tool holds it: on a wall nothing is stored ...
        val onWall = put("front", "PLB", "cold", "a")
        assertEquals(emptyMap(), onWall.extra); assertEquals("Plumbing point · Cold", shown("front", onWall))
        // ... and the SAME id for a tap that lands on the floor is stored: a Cold point, where the page -- its floor memory
        // untouched -- places the floor's default and stores nothing
        val wallsOnFloor = put("floor", "PLB", "cold", "b")
        assertEquals(mapOf("sub" to "cold"), wallsOnFloor.extra); assertEquals("Plumbing point · Cold", shown("floor", wallsOnFloor))
        // what the page's addElAt hands over there: the memory of the kind the mark lands on -- the floor's first until
        // the picker sets the floor's own (subFor(t, tkind(s)))
        val floors = put("floor", "PLB", MarkTypes.firstSub("PLB", "floor"), "c")
        assertEquals(emptyMap(), floors.extra); assertEquals("Plumbing point · Floor trap", shown("floor", floors))
        // an id the landed kind does not have reads as its first and is not stored: the wall's Mixer and Wardrobe on the floor
        assertEquals(emptyMap(), put("floor", "PLB", "mixer", "d").extra)
        val unit = put("floor", "CRP", "wardrobe", "e")
        assertEquals(emptyMap(), unit.extra); assertEquals("Bed", shown("floor", unit))
        assertEquals(SurveyPrep.placeAt("CRP", "floor", 1000.0, 1000.0, W, L).toList(), listOf(unit.x0, unit.x1, unit.y0, unit.y1))
        // and a shared id takes the LANDED kind's box: the wall's Other unit handed in for the floor is the floor's Other unit
        val other = put("floor", "CRP", "other", "f")
        assertEquals(mapOf("sub" to "other"), other.extra)
        assertEquals(SurveyPrep.placeAt("CRP", "floor", 1000.0, 1000.0, W, L, "other").toList(), listOf(other.x0, other.x1, other.y0, other.y1))
        assertEquals(mapOf("sub" to "wifi"), p.addMarkAt("ceil", "LV", 1000.0, 1000.0, W, L, "g", "wifi").extra)
    }

    // ------------------------------------------------------------------ codes and reading keys

    @Test fun `FP1 and FPLB1 never collide - a code is numbered by its whole token, and a reading belongs to its own mark`() {
        val p = SurveyPrep.fresh(L, W, H)
        fun add(t: String, s: String = "front") = p.addMark(s, t, W, H, "id${p.surface(s).marks.size}$s").code
        assertEquals(listOf("FP1", "FPLB1", "FP2", "FPLB2", "FL1", "FLV1", "FS1", "FSAN1", "FC1", "FCRP1", "FCTR1", "FN1", "FNOTE1", "FD1", "FDEF1", "FB1", "FAC1", "FGRL1", "FX1"),
            listOf("P", "PLB", "P", "PLB", "L", "LV", "S", "SAN", "C", "CRP", "CTR", "N", "NOTE", "D", "DEF", "B", "AC", "GRL", "X").map { add(it) })
        // the LEFT wall's letter is L, the ceiling's C, the back's B: its Loft is LL1 and its Low-voltage point LLV1; CC1 a column, CCDP1 a drop, CCTR1 a track
        assertEquals(listOf("LL1", "LLV1", "LLV2"), listOf("L", "LV", "LV").map { add(it, "left") })
        assertEquals(listOf("CC1", "CCDP1", "CCTR1", "CLV1"), listOf("C", "CDP", "CTR", "LV").map { add(it, "ceil") })
        assertEquals(listOf("BB1", "BS1", "BSAN1"), listOf("B", "S", "SAN").map { add(it, "back") })
        assertEquals(p.surfaces.values.sumOf { it.marks.size }, p.surfaces.values.flatMap { it.marks }.map { it.code }.toSet().size, "no code twice in the room")
        // every code reads back as its surface and its WHOLE token (the grammar), and names its own mark's type
        for (s in SurveyPrep.SURFACES) for (e in p.surface(s).marks) {
            val c = MarkTypes.parseCode(e.code)!!
            assertEquals(listOf(s, e.t, "mark"), listOf(c.surf, c.token, c.kind), e.code); assertEquals(SurveyPrep.CODE.getValue(s), c.surface); assertEquals(e.t, MarkTypes.codeToken(e.code))
        }
        // the surface's counter is keyed by the token
        @Suppress("UNCHECKED_CAST") val seq = p.surface("front").extra[SurveyPrep.SEQ] as Map<String, Any?>
        assertEquals(2, seq["P"]); assertEquals(2, seq["PLB"]); assertEquals(1, seq["L"]); assertEquals(1, seq["LV"]); assertEquals(1, seq["NOTE"]); assertEquals(1, seq["N"])
        // a reading is its own mark's: FP1.cl the Bulb's, FPLB1.cl the Plumbing point's; FN1.w the Niche's, never the Note's
        p.actual.putAll(mapOf("FP1.cl" to 1.0, "FPLB1.cl" to 2.0, "FPLB1.cs" to 3.0, "FP2.cs" to 4.0, "FL1.w" to 5.0, "FLV1.cl" to 6.0, "FN1.w" to 7.0, "FS1.w" to 8.0, "FSAN1.w" to 9.0, "FD1.w" to 10.0, "FDEF1.w" to 11.0))
        for ((key, t) in mapOf("FP1.cl" to "P", "FPLB1.cl" to "PLB", "FP2.cs" to "P", "FL1.w" to "L", "FLV1.cl" to "LV", "FN1.w" to "N", "FS1.w" to "S", "FSAN1.w" to "SAN", "FD1.w" to "D", "FDEF1.w" to "DEF")) {
            val (s, e) = p.markOf(key)!!; assertEquals("front" to t, s to e.t, key); assertEquals(key.substringBefore('.'), e.code); assertEquals(e.code, p.ownerOf(key)!!.code)
        }
        assertNull(p.markOf("FNOTE2.w")); assertNull(p.ownerOf("FPL1.cl"))
        // deleting the Bulb takes ITS readings and no other's; its number is never handed out again, nor the Plumbing point's disturbed
        assertTrue(p.deleteMark(p.markOf("FP1.cl")!!.second.id))
        assertEquals(setOf("FPLB1.cl", "FPLB1.cs", "FP2.cs", "FL1.w", "FLV1.cl", "FN1.w", "FS1.w", "FSAN1.w", "FD1.w", "FDEF1.w"), p.actual.keys)
        assertTrue(p.deleteMark(p.markOf("FD1.w")!!.second.id)); assertEquals(11.0, p.actual["FDEF1.w"]); assertNull(p.actual["FD1.w"])
        assertTrue(p.deleteMark(p.markOf("FPLB1.cl")!!.second.id)); assertEquals(4.0, p.actual["FP2.cs"]); assertNull(p.actual["FPLB1.cs"])
        assertEquals(listOf("FP3", "FPLB3", "FD2", "FDEF2"), listOf("P", "PLB", "D", "DEF").map { add(it) })
        assertEquals("FPLB4", p.nextCode("front", "PLB")); assertEquals("FP4", p.nextCode("front", "P")); assertEquals("GPLB1", p.nextCode("floor", "PLB"))
        // a clear records the tokens it takes, so the next codes still run on
        p.dropUnlocked("front"); assertTrue(p.surface("front").marks.isEmpty())
        assertEquals(listOf("FP4", "FPLB4", "FNOTE2", "FCRP2", "FL2", "FLV2"), listOf("P", "PLB", "NOTE", "CRP", "L", "LV").map { add(it) })
    }

    @Test fun `the code grammar - the whole letter run after the surface letter is the type, and what is no mark says so`() {
        fun row(c: String?) = MarkTypes.parseCode(c)?.let { "${it.surface} ${it.surf} ${it.token} ${it.kind} ${it.n} ${it.measure}" }
        assertEquals("F front PLB mark 1 cl", row("FPLB1.cl")); assertEquals("F front P mark 1 cl", row("FP1.cl"))
        assertEquals("C ceil CDP mark 1 null", row("CCDP1")); assertEquals("G floor NOTE mark 2 null", row("GNOTE2"))
        assertEquals("L left LV mark 3 null", row("LLV3")); assertEquals("L left L mark 1 null", row("LL1")); assertEquals("B back B mark 12 d", row("BB12.d"))
        // a Line and a Tape have no token; a chain's is the letter K
        assertEquals("F front null line 1 len", row("F-M1.len")); assertEquals("G floor null tape 3 null", row("G-T3")); assertEquals("R right K chain 2 t", row("RK2.t"))
        assertEquals(MarkTypes.KIND_CHAIN, MarkTypes.parseCode("FK1")!!.kind); assertEquals(SurveyChain.LETTER, MarkTypes.codeToken("FK1")); assertNull(MarkTypes.codeToken("F-M1"))
        // every surface letter is SurveyPrep.CODE's
        for ((s, letter) in SurveyPrep.CODE) assertEquals(s, MarkTypes.parseCode(letter + "W1")!!.surf)
        // what is no code: no surface letter, no number, a space, a lower-case surface, a number too long to be one
        for (bad in listOf(null, "", "W1", "XW1", "fw1", "F1", "FW", "F W1", "FW1 ", "FW1.", "FW1.c l", "F-X1", "F-M", "FW1\n", "FW99999999999999999999"))
            assertNull(MarkTypes.parseCode(bad), "$bad")
    }

    // ------------------------------------------------------------------ measures

    @Test fun `the checklist of a new mark - by its type, to a point's centre, nothing for a Note, and nothing where it touches`() {
        fun keys(s: String, e: SurveyPrep.Mark, a: Double, b: Double) = SurveyPrep.measures(s, e, a, b).joinToString(" ") { it.key }
        fun words(s: String, e: SurveyPrep.Mark, a: Double, b: Double, tx: Tx = Tx.EN) = SurveyPrep.measures0(s, e, a, b).joinToString(" | ") { it.key + " " + it.what(tx) }
        fun mark(t: String, x0: Double, x1: Double, y0: Double, y1: Double) = SurveyPrep.Mark("a", t, "F${t}1", x0, x1, y0, y1)
        // a wardrobe standing on the floor in the left corner: its underside and its left offset are zero and are not asked
        assertEquals("w width | h height | l from left corner | s underside, from floor | p depth from wall", words("front", mark("CRP", 0.0, 1200.0, 0.0, 2100.0), W, H))
        assertEquals("w h p", keys("front", mark("CRP", 0.0, 1200.0, 0.0, 2100.0), W, H))
        assertEquals("w h l s p", keys("front", mark("CRP", 150.0, 1950.0, 1500.0, 2200.0), W, H))
        assertEquals("w रुंदी | h उंची | l डाव्या कोपऱ्यापासून | s खालची बाजू, जमिनीपासून | p भिंतीपासून डेप्थ", words("front", mark("CRP", 150.0, 1950.0, 1500.0, 2200.0), W, H, Tx.MR))
        // a Grill from the floor always; an AC unit from whichever is nearer; a Curtain track by its length, from the ceiling
        assertEquals("w width | h height | r from right corner | s from floor | p projection from wall", words("front", mark("GRL", 1500.0, 2600.0, 1900.0, 2600.0), W, H))
        assertEquals("w width | h height | l from left corner | t from ceiling | p projection from wall", words("front", mark("AC", 839.0, 1839.0, 2227.0, 2527.0), W, H))
        assertEquals("w track length | l from left corner | t from ceiling | p projection from wall", words("front", mark("CTR", 589.0, 2089.0, 2427.0, 2527.0), W, H))
        assertEquals("w ट्रॅकची लांबी | l डाव्या कोपऱ्यापासून | t छतापासून | p भिंतीपासून पुढे आलेले", words("front", mark("CTR", 589.0, 2089.0, 2427.0, 2527.0), W, H, Tx.MR))
        assertEquals("w track length | l from left wall | f from front wall", words("ceil", mark("CTR", 589.0, 2089.0, 2919.0, 3019.0), W, L))
        assertEquals("w width | h height | l from left corner | s underside, from floor | p projection from wall", words("front", mark("SAN", 1064.0, 1614.0, 400.0, 850.0), W, H))
        // on a plan: left–right and front–back, the nearer walls, and the height or the drop the photo cannot show
        assertEquals("w width, left–right | h depth, front–back | l from left wall | k from back wall | p height", words("floor", mark("CRP", 150.0, 1950.0, 0.0, 2000.0), W, L))
        assertEquals("w h l p", keys("floor", mark("CRP", 150.0, 1950.0, 0.0, 2000.0), W, L), "a bed against the back wall: nothing to measure from it")
        assertEquals("w width, left–right | h depth, front–back | l from left wall | f from front wall | p drop below slab", words("ceil", mark("CDP", 300.0, 2378.0, 300.0, 3019.0), W, L))
        assertEquals("w रुंदी, डावी–उजवी | h डेप्थ, समोर–मागे | l डाव्या भिंतीपासून | f समोरच्या भिंतीपासून | p स्लॅबपासून खाली", words("ceil", mark("CDP", 300.0, 2378.0, 300.0, 3019.0), W, L, Tx.MR))
        assertEquals("w width, left–right | h depth, front–back | r from right wall | f from front wall | p drop below ceiling", words("ceil", mark("AC", 1900.0, 2500.0, 2600.0, 3200.0), W, L))
        assertEquals("w width | h height | l from left corner | s from floor", words("front", mark("DEF", 1189.0, 1489.0, 1188.0, 1488.0), W, H))
        // a Plumbing point and a Low-voltage point: two offsets to the centre, under the centre keys, kept even at the edge
        assertEquals("cl from left corner, to centre | cs from floor, to centre", words("front", mark("PLB", 300.0, 350.0, 875.0, 925.0), W, H))
        assertEquals("cr from right corner, to centre | ct from ceiling, to centre", words("front", mark("LV", 2328.0, 2378.0, 2327.0, 2377.0), W, H))
        assertEquals("cl डाव्या भिंतीपासून, मध्यापर्यंत | ck मागच्या भिंतीपासून, मध्यापर्यंत", words("floor", mark("PLB", 100.0, 150.0, 100.0, 150.0), W, L, Tx.MR))
        assertEquals("cl cs", keys("front", mark("PLB", 0.0, 50.0, 0.0, 50.0), W, H)); assertEquals(25.0, SurveyPrep.measures("front", mark("PLB", 0.0, 50.0, 0.0, 50.0), W, H)[0].est)
        // a Note asks nothing, anywhere
        for (s in listOf("front", "floor", "ceil")) { assertEquals("", keys(s, mark("NOTE", 100.0, 150.0, 100.0, 150.0), W, H)); assertEquals(emptyList(), MarkTypes.measures(s, mark("NOTE", 0.0, 50.0, 0.0, 50.0), W, H)) }
        // v52's own checklists are untouched: the new table answers null for them, and S measures as v52's S whatever its kind
        for (t in V52) assertNull(MarkTypes.measures("front", mark(t, 100.0, 400.0, 100.0, 300.0), W, H), t)
        val db = SurveyPrep.Mark("a", "S", "FS1", 150.0, 450.0, 1500.0, 1900.0, mapOf("sub" to "db"))
        assertEquals("w width | h height | l from left corner | s from floor", words("front", db, W, H))
        // the prep's own walk: keys, the next one open, a reading's line
        val p = SurveyPrep.fresh(L, W, H); val e = p.addMark("front", "CRP", W, H, "u", "kwall")
        assertEquals(listOf("FCRP1.w", "FCRP1.h", "FCRP1.l", "FCRP1.s", "FCRP1.p"), p.keysOf("front", e, W, H)); assertEquals("FCRP1.w", p.nextOpen("front", e, null, W, H))
        assertEquals("depth from wall", p.readingOf("FCRP1.p") { doubleArrayOf(W, H) }!!.second.what(Tx.EN))
        val n = p.addMark("front", "NOTE", W, H, "n"); assertEquals(emptyList(), p.keysOf("front", n, W, H)); assertNull(p.nextOpen("front", n, null, W, H))
    }

    // ------------------------------------------------------------------ where a type may go

    @Test fun `a new type goes only where it can - T53's words first, then v52's rule by what a type is`() {
        // the table's own words, English and Marathi (Marathi has no article: the keys' own note)
        assertEquals("A Grill cannot go on the ceiling", MarkTypes.placeRefusal(Tx.EN, "GRL", "ceil")); assertEquals("A Grill cannot go on the floor", MarkTypes.placeRefusal(Tx.EN, "GRL", "floor"))
        assertEquals("An AC unit cannot go on the floor", MarkTypes.placeRefusal(Tx.EN, "AC", "floor"))
        assertEquals("A Ceiling drop goes on the ceiling only", MarkTypes.placeRefusal(Tx.EN, "CDP", "front")); assertEquals("A Ceiling drop goes on the ceiling only", MarkTypes.placeRefusal(Tx.EN, "CDP", "floor"))
        assertEquals("A Sanitary ware cannot go on the ceiling", MarkTypes.placeRefusal(Tx.EN, "SAN", "ceil"))
        assertEquals("ग्रिल छतावर लावता येत नाही", MarkTypes.placeRefusal(Tx.MR, "GRL", "ceil")); assertEquals("AC युनिट जमिनीवर लावता येत नाही", MarkTypes.placeRefusal(Tx.MR, "AC", "floor"))
        assertEquals("छताचा ड्रॉप फक्त छतावरच लावता येईल", MarkTypes.placeRefusal(Tx.MR, "CDP", "back"))
        // the article is English's, each its own key, and Marathi has none
        assertEquals("A" to "An", Tx.EN.t53_a to Tx.EN.t53_an); assertEquals("" to "", Tx.MR.t53_a to Tx.MR.t53_an)
        assertEquals(listOf("AC"), V53.filter { t -> KINDS.any { MarkTypes.placeRefusal(Tx.EN, t, it)?.startsWith("An ") == true } }, "An before a vowel: the AC unit alone")
        // every type on every kind: refused exactly where it may not go; a v52 letter is never this table's to refuse
        for (t in ALLT) for (k in KINDS) for (tx in listOf(Tx.EN, Tx.MR)) {
            val said = MarkTypes.placeRefusal(tx, t, k)
            assertEquals(MarkTypes.canGo(t, k) == false, said != null, "$t on $k")
            if (said != null) { assertEquals(said.trim(), said); assertFalse(said.contains('{'), said); assertFalse(said.contains("  "), said) }
        }
        // on the 360 (Pano360.placeRefusal: the type was picked on `from`, the finger is on `to`): the new table's words for the ten ...
        assertEquals("A Ceiling drop goes on the ceiling only", Pano360.placeRefusal(Tx.EN, "ceil", "left", "CDP"))
        assertEquals("An AC unit cannot go on the floor", Pano360.placeRefusal(Tx.EN, "front", "floor", "AC")); assertNull(Pano360.placeRefusal(Tx.EN, "front", "ceil", "AC"))
        assertEquals("A Low-voltage point cannot go on the floor", Pano360.placeRefusal(Tx.EN, "ceil", "floor", "LV"))
        assertEquals("प्लंबिंग पॉइंट छतावर लावता येत नाही", Pano360.placeRefusal(Tx.MR, "floor", "ceil", "PLB"))
        assertNull(Pano360.placeRefusal(Tx.EN, "front", "back", "GRL")); assertNull(Pano360.placeRefusal(Tx.EN, "floor", "left", "CRP")); assertNull(Pano360.placeRefusal(Tx.EN, "ceil", "floor", "NOTE"))
        // ... and v52's own for a letter, unchanged
        assertEquals("A Switch plate cannot go on the floor", Pano360.placeRefusal(Tx.EN, "front", "floor", "S")); assertEquals("A Step cannot go on the front wall", Pano360.placeRefusal(Tx.EN, "floor", "front", "S"))
        assertEquals("A Window cannot go on the ceiling", Pano360.placeRefusal(Tx.EN, "front", "ceil", "W"))
        for (from in SurveyPrep.SURFACES) for (to in SurveyPrep.SURFACES) for (t in SurveyPrep.types(from)) {
            val ok = SurveyPrep.types(to).any { it.letter == t.letter && it.name == t.name }
            assertEquals(ok, Pano360.placeRefusal(Tx.EN, from, to, t.letter) == null, "${t.letter} $from -> $to"); assertEquals(ok, SurveyPrep.canPlace(from, to, t.letter))
            if (MarkTypes.isNew(t.letter)) assertEquals(MarkTypes.placeRefusal(Tx.EN, t.letter, to), Pano360.placeRefusal(Tx.EN, from, to, t.letter), "${t.letter} $from -> $to")
        }
    }
}
